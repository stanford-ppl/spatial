package spatial.tests.feature.banking

import spatial.dsl._

@test class LinearWriteRandomRead extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Unit = {
    val out1 = ArgOut[Int]

    Accel {
      val sram = SRAM[Int](16)
      val addr = SRAM[Int](16)
      Foreach(16 by 1){i =>
        Foreach(16 by 1 par 2){j =>
          sram(j) = i*j
          addr(j) = 16 - j
        }
        val sum = Reduce(0)(16 par 5){j => sram(addr(j)) }{_+_}
        out1 := sum
      }
    }

    val data = Array.tabulate(16){j => 15*j }
    val addr = Array.tabulate(16){j => 16 - j }
    val gold = Array.tabulate(16){j => data(addr(j)) }.reduce{_+_}
    assert(getArg(out1) == gold)
  }
}
