import spatial.dsl._

@spatial object BasicLUTTest extends SpatialApp {
  def main(args: Array[String]): Unit = {
    val inVal = 4
    val len = 16
    val inArg = ArgIn[Int]
    val outArg = ArgOut[Int]

    Accel {
      val lut = LUT[Int](len)(Seq.tabulate[Int](len)(i => i.to[Int]): _*)
      outArg := lut(inArg.value)
    }

    println(outArg.value - inArg.value)
  }
}
