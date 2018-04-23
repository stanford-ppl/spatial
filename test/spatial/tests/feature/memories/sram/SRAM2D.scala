package spatial.tests.feature.memories.sram


import spatial.dsl._


@test class SRAM2D extends SpatialTest {
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
      val mem = SRAM[Int](64, 128)
      Sequential.Foreach(64 by 1, 128 by 1) { (i,j) =>
        mem(i,j) = x + (i.to[I32]*128+j.to[I32]).to[Int]
      }
      Pipe { y := mem(63,127) }
    }


    // Extract results from accelerator
    val result = getArg(y)

    // Create validation checks and debug code
    val gold = N+63*128+127
    println("expected: " + gold)
    println("result: " + result)
    assert(gold == result)

  }
}