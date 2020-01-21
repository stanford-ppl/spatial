import spatial.dsl._

@spatial object ArgInOutTestVCS extends SpatialApp {
  def main(args: Array[String]): Unit = {
    val a = ArgIn[Int32]
    val b = ArgOut[Int32]
    setArg(a, 8.to[I32])
    Accel {
      b := a.value + 24
    }

    println(getArg(b))
  }
}
