package spatial.tests.feature.banking

import spatial.dsl._

// Nonsensical app, just to get structure there.
@spatial class TwoDuplicatesPachinko extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](512)
    val out = ArgOut[Int]

    val data = Array.tabulate(512){i => random[Int] }
    setMem(dram, data)

    Accel {
      Foreach(512 by 32){i =>

        val sram = SRAM[Int](16)

        sram load dram(i::i+16)

        Reduce(out)(32 by 16 par 2){j =>
          val accum = Reg[Int]
          Reduce(accum)(16 par 16){k => sram(k) }{_+_}
        }{_+_}
      }
    }

    val last32 = Array.tabulate(32){i => data(511 - i) }.reduce{_+_}
    assert(getArg(out) == last32, r"${getArg(out)} != $last32")
  }
}

