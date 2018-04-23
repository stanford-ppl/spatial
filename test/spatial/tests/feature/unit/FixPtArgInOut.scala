package spatial.tests.feature.unit

import spatial.dsl._

@test class FixPtArgInOut extends SpatialTest {
  override def runtimeArgs: Args = "-1.5"
  type T = FixPt[TRUE,_28,_4]

  def main(args: Array[String]): Unit = {
    // Declare SW-HW interface vals
    val x = ArgIn[T]
    val y = ArgOut[T]
    val N = args(0).to[T]

    // Connect SW vals to HW vals
    setArg(x, N)

    // Create HW accelerator
    Accel {
      y := ((x * 9)-10)/ -1 + 7
    }


    // Extract results from accelerator
    val result = getArg(y)

    // Create validation checks and debug code
    val gold = ((N * 9)-10)/ -1 + 7
    println("expected: " + gold)
    println("result: " + result)
    assert(gold == result)
  }
}