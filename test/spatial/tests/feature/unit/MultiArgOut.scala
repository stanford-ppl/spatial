package spatial.tests.feature.unit


import spatial.dsl._


@test class MultiArgOut extends SpatialTest {
  override def runtimeArgs: Args = "32 16"
  type T = Int


  def main(args: Array[String]): Unit = {
    // Declare SW-HW interface vals
    val a = ArgIn[T]
    val b = ArgIn[T]
    val x = ArgOut[T]
    val y = ArgOut[T]
    val i = args(0).to[T]
    val j = args(1).to[T]
    setArg(a, i)
    setArg(b, j)

    Accel {
      x := a
      y := b
    }

    // Extract results from accelerator
    val xx = getArg(x)
    val yy = getArg(y)

    println("xx = " + xx + ", yy = " + yy)
    val cksum = (xx == i) && (yy == j)
    assert(cksum)
  }
}
