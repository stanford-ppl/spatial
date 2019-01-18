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

@spatial class HostIOLoop extends SpatialTest {
  override def runtimeArgs: Args = "7"

  def main(args: Array[String]): Unit = {
    // Declare SW-HW interface vals
    val y = HostIO[Int]
    val N = args(0).to[Int]

    // Connect SW vals to HW vals
    setArg(y, N)

    // Create HW accelerator
    for (i <- 0 until 4) {
      Accel {
        y := y + 4
      }
    }

    val result = getArg(y)

    val gold = N + 4*4
    println("expected: " + gold)
    println("result: " + result)

    val cksum = gold == result
    println("PASS: " + cksum + " (InOutArg)")
    assert(cksum)
  }
}

@spatial class FixPtArgInOut extends SpatialTest {
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

@spatial class FltPtArgInOut extends SpatialTest {
  override def runtimeArgs: Args = "-1.5"
  type T = Float

  def main(args: Array[String]): Unit = {
    // Declare SW-HW interface vals
    val x = ArgIn[T]
    val y = ArgOut[T]
    val N = args(0).to[T]

    // Connect SW vals to HW vals
    setArg(x, N)

    // Create HW accelerator
    Accel {
      y := x + 1
    }


    // Extract results from accelerator
    val result = getArg(y)

    // Create validation checks and debug code
    val gold = N + 1
    println("expected: " + gold)
    println("result: " + result)
    assert(gold == result)
  }
}
