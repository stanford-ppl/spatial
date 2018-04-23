package spatial.tests.feature.banking


import spatial.dsl._


@test class SRAMCoalescing extends SpatialTest {
  override def runtimeArgs: Args = NoArgs


  def main(args: Array[String]): Unit = {
    Accel {
      val sram = SRAM[Int](32)
      val out1 = ArgOut[Int]
      val out2 = ArgOut[Int]
      Foreach(16 by 1){i =>
        Foreach(16 by 1){j => sram(j) = i*j }
        val sum = Reduce(0)(16 par 2){j => sram(j) }{_+_}
        val product = Reduce(0)(16 par 3){j => sram(j) }{_*_}
        out1 := sum
        out2 := product
      }
    }
  }
}
