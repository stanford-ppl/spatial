package spatial.tests.feature.streaming

import spatial.dsl._


// object MovingBox extends SpatialApp {

//   val Cmax = 320
//   val Rmax = 240

//   @struct case class Pixel16(b: UInt5, g: UInt6, r: UInt5)


//   def convolveVideoStream(): Unit = {
//     val imgOut = BufferedOut[Pixel16](DE1.VGA)
//     val dwell = ArgIn[Int]
//     val d = args(0).to[Int]
//     setArg(dwell, d)

//     Accel (*) {
//       Foreach(0 until 4) { i =>
//         Foreach(0 until dwell) { _ =>
//           Foreach(0 until Rmax, 0 until Cmax){ (r, c) =>
//             val pixel1 = mux(r > 60 && r < 120 && c > 60 && c < 120, Pixel16(0,63,0), Pixel16(0,0,0))
//             val pixel2 = mux(r > 120 && r < 180 && c > 60 && c < 120, Pixel16(31,0,0), Pixel16(0,0,0))
//             val pixel3 = mux(r > 120 && r < 180 && c > 120 && c < 180, Pixel16(0,0,31), Pixel16(0,0,0))
//             val pixel4 = mux(r > 60 && r < 120 && c > 120 && c < 180, Pixel16(31,0,0), Pixel16(0,0,0))
//             val pixel = mux(i == 0, pixel1, mux( i == 1, pixel2, mux( i == 2, pixel3, mux(i == 3, pixel4, Pixel16(0,0,0)))))
//             imgOut(r, c) = pixel
//           }
//         }
//       }
//     }
//   }


//   def main(args: Array[String]): Unit = {
//     val R = Rmax
//     val C = Cmax
//     convolveVideoStream()
//   }
// }
