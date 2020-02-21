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
  type T = FixPt[TRUE, _10, _22]
  val M = 32
  val N = 32

  override def main(args: Array[String]): Unit = {

    val stride = DRAM[Int](M,N)
    val slide = DRAM[Int](M,N)

    Accel {
      // Strided
      {
        val A = SRAM[Int](M, N).dualportedread
        val B = SRAM[Int](M, N)
        Foreach(M by 1, N by 1 par 2) { (i, j) => A(i, j) = i * N + j }
        Foreach(M by 1 par 4, N by 1) { (i, j) => B(i, j) = A(i, j) }
        stride store B
      }

      // Sliding window
      {
        val A = SRAM[Int](M, N).dualportedread
        val B = SRAM[Int](M, N)
        Foreach(M-1 by 1, N by 1) { (i, j) =>
          A(i, j) = i * N + j
          A(i, j + 1) = i * N + j + 1
        }
        Foreach(M-3 by 1, N by 1) { (i, j) =>
          B(i, j) = A(i, j)
          B(i+1, j) = A(i+1, j)
          B(i+2, j) = A(i+2, j)
          B(i+3, j) = A(i+3, j)
        }
        slide store B
      }
    }

    val gold = (0::M, 0::N){(i,j) => i * N + j}
    printMatrix(gold, "want")
    val result1 = getMatrix(stride)
    printMatrix(result1, "gotstride")
    val result2 = getMatrix(slide)
    printMatrix(result2, "gotslide")
  }
}