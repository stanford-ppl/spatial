package spatial.tests.compiler

import spatial.dsl._

@test class InlineSwitch extends SpatialTest {
  override def runtimeArgs: Args = "3" and "7" and "5" and "32"


  def main(args: Array[String]): Unit = {
    val in = args(0).to[Int]
    val y = ArgIn[Int]
    val x = ArgOut[Int]
    setArg(y, in)

    Accel {
      val z = if (y == 3.to[Int]) 0 else if (y == 5.to[Int]) 1 else if (y == 7.to[Int]) 2 else 3
      x := z
    }

    println(getArg(x))
    val gold = if (in == 3) 0 else if (in == 5) 1 else if (in == 7) 2 else 3
    assert(getArg(x) == gold)
  }
}
