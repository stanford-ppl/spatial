import spatial.dsl._

@spatial object ArgInOutTest extends SpatialApp {
  def main(args: Array[String]): Unit = {
    val a = ArgIn[Int32]
    val b = ArgOut[Int32]
    setArg(a, 1.to[I32])
    Accel {
      b := a.value + 1
    }

    println(getArg(b))
  }
}
