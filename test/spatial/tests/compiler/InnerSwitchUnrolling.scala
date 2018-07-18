package spatial.tests.compiler

import spatial.dsl._

@spatial class InnerSwitchUnrolling extends SpatialTest {

  def main(args: Array[String]): Unit = {

    val data = Array.tabulate(32){i => i }
    val dram = DRAM[Int](32)
    setMem(dram, data)

    Accel {
      val sram1 = SRAM[Int](32)
      val sram2 = SRAM[Int](32)

      sram1 load dram

      Foreach(0 until 32 par 8){i =>
        val x = if (i % 2 == 0) 16 else 8
        sram2(i) = sram1(x)
      }

      dram store sram2
    }

    val result = getMem(dram)
    val golden = Array.tabulate(32){i => if (i%2==0) 16 else 8 }
    printArray(result, "result")
    printArray(golden, "golden")
    assert(result == golden)
  }

}
