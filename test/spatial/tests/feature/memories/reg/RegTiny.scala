package spatial.tests.feature.memories.reg

import spatial.dsl._

@spatial class RegTiny extends SpatialTest {
  override def runtimeArgs: Args = "32"

  def main(args: Array[String]): Unit = {
    val in = ArgIn[Int]
    val out = ArgOut[Int]
    setArg(in, args(0).to[Int])

    Accel {
      val reg = Reg[Int](0)
      val x = reg + in.value
      val y = in.value - reg
      println(x)
      println(y)
      out := y
    }

    assert(getArg(out) == args(0).to[Int])
  }
}