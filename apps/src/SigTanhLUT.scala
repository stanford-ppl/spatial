import spatial.dsl._
import scala.math._

@spatial
object SigTanhLUT extends SpatialApp {
  def main(args: Array[String]): Unit = {
    type T = FixPt[TRUE, _10, _22]
    type F = FltPt[_24, _8]
    val LUTLen: scala.Int = 256

    val tanhHostFn: T => F = x => {
      tanh(x).to[F]
    }

    val sigHostFn: T => F = x => {

    }

    val tanhRange: scala.Int = 4
    val sigRange: scala.Int = 4

    Accel {
      val tanhAccelFn: T => F = x => {
        val lut = LUT[F](LUTLen)(Seq.tabulate[F](LUTLen)(i => math.tanh().to[F]): _*)
      }
    }
  }
}
