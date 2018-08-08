package spatial.tests.feature.retiming

import spatial.dsl._

@spatial class RetimeRandomTest extends SpatialTest {

  def main(args: Array[String]): Void = {
    val x = ArgOut[Bit]
    val nx = ArgOut[Bit]
    Accel {
      val y = random[Bit]
      x := !y
      nx := y
    }
    println(r"bit: $x")
    assert(getArg(x) != getArg(nx))
  }
}