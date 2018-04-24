package spatial.tests.feature.streaming


import spatial.dsl._

// object ColoredLines extends SpatialApp { // try arg = 100
//   val Cmax = 320
//   val Rmax = 240

//   @struct case class Pixel16(b: UInt5, g: UInt6, r: UInt5)


//   def convolveVideoStream(): Unit = {
//     val dwell = ArgIn[Int]
//     val d = args(0).to[Int]
//     setArg(dwell, d)
//     val imgOut = BufferedOut[Pixel16](target.VGA)

//     Accel (*) {
//       Foreach(0 until Rmax) { i =>
//         Foreach(0 until dwell) { _ =>
//           Foreach(0 until Rmax, 0 until Cmax){ (r, c) =>
//             val bluepixel = mux(r == i, Pixel16((r%32).as[UInt5],0,0), Pixel16(0,0,0))
//             val greenpixel = mux(r == i, Pixel16(0,(r%32).as[UInt6],0), Pixel16(0,0,0))
//             val redpixel = mux(r == i, Pixel16(0,0,(r%32).as[UInt5]), Pixel16(0,0,0))
//             val pixel = mux(r < (Rmax/3), bluepixel, mux(r < (2*Rmax/3), greenpixel, redpixel))
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

