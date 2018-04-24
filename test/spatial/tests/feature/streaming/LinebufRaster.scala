// package spatial.tests.feature.streaming

// import spatial.dsl._

// object LinebufRaster extends SpatialApp { // try arg = 100
//   override val target = DE1
//   val Cmax = 320
//   val Rmax = 240

//   @struct case class Pixel16(b: UInt5, g: UInt6, r: UInt5)


//   def convolveVideoStream(): Unit = {
//     val dwell = ArgIn[Int]
//     val d = args(0).to[Int]
//     setArg(dwell, d)
//     val imgOut = BufferedOut[Pixel16](target.VGA)
//     val dumbdelay = 20 // extra delay so fill and drain take different number of cycles

//     Accel (*) {
//       val lb = LineBuffer[Pixel16](1,Cmax)
//       Foreach(0 until Rmax) { i =>
//         Foreach(0 until dwell) { _ =>
//           Foreach(0 until Rmax){ r =>
//             Foreach(0 until Cmax) { c =>
//               val bluepixel = mux(abs(r - i) < 4, Pixel16(31.to[UInt5],0,0), Pixel16(0,0,0))
//               val greenpixel = mux(abs(r - i) < 4, Pixel16(0,31.to[UInt6],0), Pixel16(0,0,0))
//               val redpixel = mux(abs(r - i) < 4, Pixel16(0,0,31.to[UInt5]), Pixel16(0,0,0))
//               val pixel = mux(c < Cmax/3, bluepixel, mux(c < Cmax*2/3, greenpixel, redpixel))
//               lb.enq(pixel)
//             }
//             Foreach(0 until Cmax) { c =>
//               imgOut(r, c) = lb(0, c)
//             }

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
