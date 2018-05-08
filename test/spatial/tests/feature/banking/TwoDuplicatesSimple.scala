package spatial.tests.feature.banking


import spatial.dsl._


@test class TwoDuplicatesSimple extends SpatialTest {
  override def runtimeArgs: Args = NoArgs


  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](32)
    val dram2 = DRAM[Int](32)

    Accel {
      val sram = SRAM[Int](32)

      Foreach(32 by 16){i =>
        Foreach(16 par 16){ii =>
          sram(ii) = i + ii
        }

        dram(i::i+16 par 16) store sram
        dram2(i::i+16 par 16) store sram
     
      }
    }

    val result = getMem(dram)
    val result2 = getMem(dram)
    val gold = Array.tabulate(32){i => i}

    printArray(result, "result")
    printArray(result2, "result2")
    printArray(gold, "gold")
    assert(result == gold)
    assert(result2 == gold)
  }
}