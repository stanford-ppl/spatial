package spatial.tests.feature.retiming

import spatial.dsl._

@spatial class RandomInPlaceUpdate extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val dramA = DRAM[Int](32)
    val dramB = DRAM[Float](32)
    val dataA = (0::32){_ => random[Int](32) }
    val dataB = (0::32){_ => random[Float](32) }
    setMem(dramA, dataA)
    setMem(dramB, dataB)

    Accel {
      val sramA = SRAM[Int](32)
      val sramB = SRAM[Float](32)
      sramA load dramA
      sramB load dramB

      Foreach(0 until 32){i =>
        val x = sramA(i)
        sramB(x) = sramB(x) - 1
      }

      dramB store sramB
    }

    val result = getMem(dramB)
    val golden = Array.empty[Float](32)
    (0 until 32).foreach{i => golden(dataA(i)) = golden(dataA(i)) - 1 }

    printArray(result, "result")
    printArray(golden, "golden")
    assert(result == golden)
  }
}
