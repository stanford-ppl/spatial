import spatial.dsl._

@spatial object NormedMul extends SpatialApp {
  def main(args: Array[String]): Unit = {
    type F = Float
    val len: I32 = I32(128)
    val aVec: Array[Float] = Array.tabulate[F](len)(_ => random(2.to[F]))
    val bVec: Array[Float] = Array.tabulate[F](len)(_ => random(2.to[F]))
    Accel {
    }
  }
}
