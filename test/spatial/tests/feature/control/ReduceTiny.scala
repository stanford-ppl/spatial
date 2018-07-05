package spatial.tests.feature.control

import spatial.dsl._

@spatial class ReduceTiny extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val sumOut = ArgOut[Int]
    Accel {
      val sram = SRAM[Int](1, 16)
      Foreach(0 until 16){i => sram(0, i) = i }
      val sum = Reduce(0)(16 by 1) { i => sram(0, i) } { (a, b) => a + b }
      sumOut := sum
    }
    assert(getArg(sumOut) == Array.tabulate(16){i => i}.reduce{_+_})
  }

}


@spatial class ReduceAccumTiny extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val prodOut = ArgOut[Int]
    val sumOut = ArgOut[Int]

    Accel {
      val product = Reg[Int](1)
      Reduce(product)(16 by 1){i => i } {_ * _}
      val sum2 = Reduce(0)(0 :: 1 :: 16 par 2) { i => i } {_ + _}

      prodOut := product
      sumOut := sum2
    }

    assert(getArg(prodOut) == Array.tabulate(16){i => i}.reduce{_*_})
    assert(getArg(sumOut) == Array.tabulate(16){i => i}.reduce{_+_})
  }

}