package spatial.tests.feature.control

import spatial.dsl._

@spatial class Breakpoint extends SpatialTest {

  def main(args: Array[String]): Void = {
    val y = ArgOut[Int]
    val z = HostIO[Int]

    Accel {
      Sequential.Foreach(16 by 1) {i =>
        sleep(100)
        Pipe{y := i}
        if (i == 8) { Sequential{
          Pipe{ exit() }
          sleep(100)
        }} // breakpoint() also works
        Pipe{z := i}
      }
    }

    val Y = getArg(y)
    val Z = getArg(z)

    println("Y = " + Y + ", Z = " + Z)

    val cksum = Y == 8 && Z == 7
    println("PASS: " + cksum + " (Breakpoint)")
    assert(cksum)
  }
}
