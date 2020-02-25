package spatial.tests.feature.memories.sram

import spatial.dsl._

@spatial class SRAM2D extends SpatialTest {
  override def runtimeArgs: Args = "7"

  def main(args: Array[String]): Unit = {
    val x = ArgIn[Int]
    val y = ArgOut[Int]
    val N = args(0).to[Int]

    setArg(x, N)

    Accel {
      val mem = SRAM[Int](64, 128)
      Sequential.Foreach(64 by 1, 128 by 1) { (i,j) =>
        mem(i,j) = x + (i.to[I32]*128+j.to[I32]).to[Int]
      }
      Pipe { y := mem(63,127) }
    }

    val result = getArg(y)

    val gold = N+63*128+127
    println("expected: " + gold)
    println("result: " + result)
    assert(gold == result)
  }
}


@spatial class DualPortedReads extends SpatialTest {

  // Feel free to tweak this. You're under no obligation whatsoever to keep precisions anywhere as long as your final
  // result satisfies the requirements in GEMM.md
  val M = 16
  val N = 16
  val P = 8

  override def main(args: Array[String]): Unit = {

    val stride = DRAM[Int](M,N)
    val sliding = DRAM[Int](M,N)
    val hier_sliding = DRAM[Int](M,N)
    val hier_stride = DRAM[Int](P,M,N)

    Accel {
      // Strided (and buffered)
      Foreach(4 by 1) { _ =>
        val A_strided = SRAM[Int](M, N).dualportedread.nofission // should have 4 banks
        val B_strided = SRAM[Int](M, N)
        Foreach(M by 1, N by 1 par 2) { (i, j) => A_strided(i, j) = i * N + j }
        Foreach(M by 1 par 8, N by 1) { (i, j) => B_strided(i, j) = A_strided(i, j) }
        stride store B_strided
      }

      // Sliding window
      val A_sliding = SRAM[Int](M, N).dualportedread.nofission // should have 4 banks
      val B_sliding = SRAM[Int](M, N)
      Foreach(M by 1, N by 1 par 2) { (i, j) => A_sliding(i, j) = i * N + j }
      Foreach(M-7 by 1, N by 1) { (i, j) =>
        List.tabulate(8){ii => B_sliding(i + ii, j) = A_sliding(i + ii, j)}
      }
      sliding store B_sliding

      // Hierarchical sliding
      val A_hier_sliding = SRAM[Int](M, N).dualportedread.hierarchical.nofission // should have 8 banks
      val B_hier_sliding = SRAM[Int](M, N)
      Foreach(M by 1 par 2, N by 1 par 2) { (i, j) => A_hier_sliding(i, j) = i * N + j }
      Foreach(M-3 by 1, N-3 by 1) { (i, j) =>
        List.tabulate(4,4){(ii,jj) => B_hier_sliding(i+ii, j+jj) = A_hier_sliding(i+ii, j+jj)}
      }
      hier_sliding store B_hier_sliding

      // Hierarchical strided
      val A_hier_stride = SRAM[Int](P, M, N).dualportedread.hierarchical.nofission.effort(0) // should have between 32 banks (effort 0 to make it choose N=2 for dim0 and not N=3)
      val B_hier_stride = SRAM[Int](P, M, N)
      Foreach(P by 1 par 2, M by 1 par 2, N by 1 par 2) { (k, i, j) => A_hier_stride(k, i, j) = k * M * N + i * N + j }
      Foreach(P by 1 par 3, M by 1 par 4, N by 1 par 4) { (k, i, j) => B_hier_stride(k, i, j) = A_hier_stride(k, i, j) }
      hier_stride store B_hier_stride
    }

    val gold = (0::M, 0::N){(i,j) => i * N + j}
    printMatrix(gold, "want")
    val result1 = getMatrix(stride)
    printMatrix(result1, "got stride")
    val result2 = getMatrix(sliding)
    printMatrix(result2, "got slide")
    val result3 = getMatrix(hier_sliding)
    printMatrix(result3, "got hier slide")
    val gold4 = (0::P, 0::M, 0::N){(k,i,j) => k * M * N + i * N + j}
    val result4 = getTensor3(hier_stride)
    printTensor3(result4, "got hier stride")

    println(r"Passing? ${gold == result1} ${gold == result2} ${gold == result3} ${gold4 == result4}")
    assert(result1 == gold && result2 == gold && gold == result3 && gold4 == result4)
  }
}