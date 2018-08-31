package spatial.tests.feature.retiming

import spatial.dsl._

@spatial class RandomInPlaceUpdate2 extends SpatialTest {

  def main(args: Array[String]): Unit = {

    val dram1 = DRAM[Float](16)
    val dram2 = DRAM[Float](1, 16)

    val array1 = Array.fill(16){ 5.to[Float] }
    setMem(dram1, array1)

    Accel {
      val sram1 = SRAM[Float](1)
      val sram2 = SRAM[Float](1, 16)

      Sequential.Foreach(16 by 1) { i =>
        sram1 load dram1(i :: i + 1)
        /* tensor.zero_sram */
        Foreach(0 :: sram2.rows, 0 :: sram2.cols) {(d0, d1) => sram2(d0, d1) = 0.to[Float] }
        Foreach(0 :: 1) { d0 =>
          val d1 = sram1(d0).to[Int]
          sram2(d0, d1) = sram2(d0, d1) - 1.to[Float]  // (0,5) should now be -1
        }
      }
      dram2 store sram2
    }

    val golden = Matrix.tabulate[Float](1, 16){(i,j) => if (j == 5) -1.0f else 0.0f  }
    val result = getMatrix(dram2)

    printMatrix(golden, "golden")
    printMatrix(result, "result")
    assert(golden == result)
  }
}
