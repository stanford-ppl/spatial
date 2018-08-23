package spatial.tests.feature.dense

import spatial.dsl._
import spatial.lib._

@spatial trait GEMMLibBase extends SpatialTest {
  def common[T:Num](KP: scala.Int): Unit = {

    // Populate input matrices
    val inputA = Matrix.tabulate[T](16, 8){(i, j) => i.to[T]}
    val inputB = Matrix.tabulate[T](16, 8){(i, j) => j.to[T]}

    // Print input matrices
    print("\n*** Input A ***\n")
    printMatrix(inputA)
    print("\n*** Input B ***\n")
    printMatrix(inputB)

    // Allocate DRAM
    val dramA = DRAM[T](16, 8)
    val dramB = DRAM[T](16, 8)
    val dramY = DRAM[T](8,8)

    // Transfer input matrices to DRAM
    setMem(dramA, inputA)
    setMem(dramB, inputB)


    Accel {
      val sramA = SRAM[T](16, 8)
      val sramB = SRAM[T](16, 8)
      val sramY = SRAM[T](8, 8)

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
        beta = 1.0.to[T],
        mp = 1, np = 1,
        kp = KP)

      dramY store sramY
    }

    val outputY = getMatrix(dramY)
    print("\n*** Output Y ***\n")
    printMatrix(outputY)

    val goldY = Matrix.tabulate[T](8, 8) { (i,j) =>
      Array.tabulate(16){ k => inputA.t.apply(i,k)*inputB(k,j)}.reduce{_+_}
    }.t

    print("\n*** Golden Y ***\n")
    printMatrix(goldY)

    val cksum = goldY.zip(outputY){(a,b) => abs(a-b) <= .001.to[T]}.reduce{_&&_}
    assert(cksum)
  }
}

@spatial class GEMMLibFloatTest extends GEMMLibBase {
  def main(args: Array[String]): Unit = common[Float](1)
}

@spatial class GEMMLibIntTest extends GEMMLibBase {
  def main(args: Array[String]): Unit = common[Int](1)
}

@spatial class GEMMLibHalfTest extends GEMMLibBase {
  def main(args: Array[String]): Unit = common[Half](1)
}

@spatial class GEMMLibFloatParTest extends GEMMLibBase {
  def main(args: Array[String]): Unit = common[Float](2)
}

@spatial class GEMMLibIntParTest extends GEMMLibBase {
  def main(args: Array[String]): Unit = common[Int](2)
}

@spatial class GEMMLibHalfParTest extends GEMMLibBase {
  def main(args: Array[String]): Unit = common[Half](2)
}
