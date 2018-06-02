package spatial.tests.feature.buffering

import spatial.dsl._

@test class MultiplexedSRAMAccum extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Unit = {
    val data = Array.fill(32){ random[Int](max = 10) }
    val dram = DRAM[Int](32)
    setMem(dram, data)

    Accel {
      val a = SRAM[Int](32)
      val b = SRAM[Int](32)
      b load dram

      printSRAM1(a, "LOAD")

      'A.MemReduce(a)(0 until 32){i => b }{_+_}

      printSRAM1(a, "Accum #1")

      'B.MemFold(a)(0 until 32){i => b }{_+_}

      printSRAM1(a, "Accum #2")

      dram store a
    }

    val result = getMem(dram)
    val golden = data.map{e => e * 64 }
    printArray(result, "result")
    printArray(golden, "golden")
    assert(result == golden)
  }
}

