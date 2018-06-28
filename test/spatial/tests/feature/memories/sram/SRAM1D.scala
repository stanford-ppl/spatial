package spatial.tests.feature.memories.sram


import spatial.dsl._


@spatial class SRAM1D extends SpatialTest {
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
      val mem = SRAM[Int](384)
      Sequential.Foreach(384 by 1) { i =>
        mem(i) = x + i.to[Int]
      }
      Pipe { y := mem(383) }
    }


    // Extract results from accelerator
    val result = getArg(y)

    // Create validation checks and debug code
    val gold = N+383
    println("expected: " + gold)
    println("result: " + result)
    assert(gold == result)
  }
}
