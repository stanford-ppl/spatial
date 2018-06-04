package spatial.tests.feature.memories.lut



import spatial.dsl._

@test class Breakpoint extends SpatialTest { // Regression (Unit) // Args: 7
  override def runtimeArgs: Args = NoArgs
  def main(args: Array[String]): Void = {

    // Declare SW-HW interface vals
    val y = ArgOut[Int]
    val z = HostIO[Int]

    // Create HW accelerator
    Accel {
      Sequential.Foreach(16 by 1) {i =>
        sleep(100)
        Pipe{y := i}
        if (i == 8) { Sequential{
          Pipe{exit()}
          sleep(100)
        }} // breakpoint() also works
        Pipe{z := i}
      }
    }


    // Extract results from accelerator
    val Y = getArg(y)
    val Z = getArg(z)

    println("Y = " + Y + ", Z = " + Z)

    val cksum = Y == 8 && Z == 7
    println("PASS: " + cksum + " (Breakpoint)")
    assert(cksum)
  }
}
