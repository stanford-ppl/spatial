package spatial.tests.feature.streaming


import spatial.SpatialApp
import spatial.dsl._

object StreamInOut extends SpatialApp {
  import spatial.targets.DE1

   def main(args: Array[String]): Unit = {
    val in  = StreamIn[Int](DE1.GPInput1)
    val out = StreamOut[Int](DE1.GPOutput1)
    Accel(*) {
      out := in
    }
  }
}
