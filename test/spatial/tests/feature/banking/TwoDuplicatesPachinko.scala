package spatial.tests.feature.banking


import spatial.dsl._


// Nonsensical app, just to get structure there.
@test class TwoDuplicatesPachinko extends SpatialTest {
  override def runtimeArgs: Args = NoArgs


  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](512)
    val out = ArgOut[Int]

    Accel {
      Foreach(16 by 1){i =>

        val sram = SRAM[Int](16)

        sram load dram(i::i+16)

        Foreach(32 by 16 par 2){j =>
          val accum = Reg[Int]
          Reduce(accum)(16 par 16){k => sram(k) }{_+_}
          out := accum.value
        }
      }
    }
  }
}

