import spatial.dsl._

@spatial object Flt2FixTest extends SpatialApp {
  def main(args: Array[String]): Unit = {
    type T = FixPt[TRUE, _16, _16]
    type F = Float
    val inArg = ArgIn[Float]
    val outArg = ArgOut[T]
    setArg(inArg, args(0).to[F])
    Accel {
      outArg := inArg.value.to[T]
    }

    println("outArg = " + getArg(outArg))
  }
}
