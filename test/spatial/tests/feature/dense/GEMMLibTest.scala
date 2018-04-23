package spatial.tests.feature.dense

import spatial.dsl._
import spatial.lib._

@test class GEMMLibTest extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  type T = Float

  def main(args: Array[String]): Unit = {

    // Populate input matrices
    val inputA = Matrix.tabulate[T](3, 2){(i, j) => i.to[T]+j.to[T]}
    val inputB = Matrix.tabulate[T](3, 1){(i, j) => i.to[T]+j.to[T]}

    // Print input matrices
    print("\n*** Input A ***\n")
    printMatrix(inputA)
    print("\n*** Input B ***\n")
    printMatrix(inputB)

    // Allocate DRAM
    val dramA = DRAM[T](3, 2)
    val dramB = DRAM[T](3, 1)
    val dramY = DRAM[T](1,2)

    // Transfer input matrices to DRAM
    setMem(dramA, inputA)
    setMem(dramB, inputB)


    Accel {
      val sramA = SRAM[T](3, 2)
      val sramB = SRAM[T](3, 1)
      val sramY = SRAM[T](1, 2)

      sramA load dramA
      sramB load dramB

      Foreach(0 :: sramY.rows, 0 :: sramY.cols) { (d0, d1) =>
        sramY(d0, d1) = 0.to[T]
      }

      gemm(Y = sramY,
        A = sramA,
        B = sramB,
        C = 0.to[T],
        transA = true,
        transB = false,
        transY = true,
        sumY = true,
        alpha = 1.0.to[T],
        beta = 1.0.to[T])

      dramY store sramY
    }

    val outputY = getMatrix(dramY)
    print("\n*** Output Y ***\n")
    printMatrix(outputY)

    val goldY = Matrix.tabulate[T](2, 1) { (i,j) =>
      Array.tabulate(3){ k => inputA.t.apply(i,k)*inputB(k,j)}.reduce{_+_}
    }.t

    print("\n*** Golden Y ***\n")
    printMatrix(goldY)

    val cksum = goldY.zip(outputY){(a,b) => abs(a-b) < .001.to[T]}.reduce{_&&_}
    assert(cksum)
  }
}
