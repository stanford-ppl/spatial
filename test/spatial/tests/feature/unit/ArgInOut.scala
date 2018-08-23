package spatial.tests.feature.unit

import spatial.dsl._

@spatial class ArgInOut extends SpatialTest {
  override def runtimeArgs: Args = "7"

  def main(args: Array[String]): Unit = {
    // Declare SW-HW interface vals
    val x = ArgIn[Int]
    val y = ArgOut[Int]
    val N = args(0).to[Int]

    // Connect SW vals to HW vals
    setArg(x, N)

    // Create HW accelerator
    Accel {
      y := x + 4
    }

    val result = getArg(y)

    val gold = N + 4
    println("expected: " + gold)
    println("result: " + result)

    val cksum = gold == result
    println("PASS: " + cksum + " (InOutArg)")
    assert(cksum)
  }
}
