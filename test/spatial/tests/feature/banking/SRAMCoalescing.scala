package spatial.tests.feature.banking

import spatial.dsl._

@test class SRAMCoalescing extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Unit = {
    val out1 = ArgOut[Int]
    val out2 = ArgOut[Int]

    Accel {
      val sram = SRAM[Int](32)
      Foreach(16 by 1){i =>
        Foreach(16 by 1){j => sram(j) = i*j + 1 }
        val sum = Reduce(0)(16 par 2){j => sram(j) }{_+_}
        val product = Reduce(1)(16 par 2){j => sram(j) }{_^_}
        out1 := sum
        out2 := product
      }
    }

    val data = Array.tabulate(16){j => j*15 + 1 }
    val goldSum = data.reduce{_+_}
    val goldProd = data.reduce{_^_}
    println(r"goldSum $goldSum =?= ${getArg(out1)}")
    println(r"goldProd $goldProd =?= ${getArg(out2)}")
    assert(getArg(out1) == goldSum)
    assert(getArg(out2) == goldProd)
  }
}
