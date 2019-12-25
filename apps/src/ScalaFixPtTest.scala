import spatial.dsl._

@spatial object ScalaFixPtTest extends SpatialApp {
  def main(args: Array[String]): Unit = {
    val a0 = ArgIn[Int8]
    val a1 = ArgIn[Int8]
    setArg(a0, 1)
    setArg(a1, 5)
    val a2 = ArgOut[Int8]
    Accel {
      a2 := a0.value / a1.value
    }

    println(getArg(a2))
  }
}
