package spatial.tests.feature.control


import spatial.dsl._


@test class ReduceTiny extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Unit = {
    Accel {
      val sram = SRAM[Int](1, 16)
      val sum = Reduce(0)(16 by 1) { i => sram(0, i) } { (a, b) => a + b }
      println(sum.value)
    }
  }
}


@test class ReduceAccumTiny extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Unit = {
    Accel {
      val product = Reg[Int](1)
      Reduce(product)(16 by 1){i => i } {_ * _}
      val sum2 = Reduce(0)(0 :: 1 :: 16 par 2) { i => i } {_ + _}
      println(product.value)
      println(sum2.value)
    }
  }
}