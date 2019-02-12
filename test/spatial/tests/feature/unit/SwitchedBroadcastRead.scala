package spatial.tests.feature.unit

import spatial.dsl._

@spatial class SwitchedBroadcastRead extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](32,32)
    val data = Matrix.tabulate(32,32){(i,j) => (i + j).to[Int] }

    setMem(dram, data)

    Accel {
      val sram1 = SRAM[Int](32, 32)
      val sram2 = SRAM[Int](32, 32)

      sram1 load dram

      Pipe {
        sram2(0,0) = 32
      }

      // We should properly CSE (or at least broadcast) the two reads to sram1 as one physical read
      Foreach(0 :: sram1.rows, 0 :: sram2.cols){ (d0, d1) =>
        if (sram1(d0, d1) != 0.0f) {
          sram2(d0,d1) = - sram1(d0, d1)
        } else {
          sram2(d0,d1) = sram1(d0, d1)
        }
      }

      dram store sram2
    }

    val golden = Matrix.tabulate(32,32){(i,j) =>
      if (i == 0 && j == 0) 32 else -(i + j)
    }

    val result = getMatrix(dram)
    printMatrix(result, "result")
    printMatrix(golden, "golden")
    assert(result == golden)
  }

}
