package spatial.tests.feature.streaming

import spatial.dsl._

// object ColorSelect extends SpatialApp { // try arg = 100

//   val Cmax = 320
//   val Rmax = 240

//   type UINT3 = FixPt[FALSE,_3,_0]
//   type UINT4 = FixPt[FALSE,_4,_0]

//   @struct class sw3(b1: UINT3, b2: UINT4, b3: UINT3)
//   @struct case class Pixel16(b: UInt5, g: UInt6, r: UInt5)


//   def convolveVideoStream(): Unit = {
//     val dwell = ArgIn[Int]
//     val d = args(0).to[Int]
//     setArg(dwell, d)
//     val switch = target.SliderSwitch
//     val swInput = StreamIn[sw3](switch)

//     val imgOut = StreamOut[Pixel16](target.VGA)

//     Accel (*) {
//       Foreach(0 until Rmax) { i =>
//         Foreach(0 until dwell) { _ =>
//           Foreach(0 until Rmax, 0 until Cmax){ (r, c) =>
//             val swBits = swInput.value()
//             val bluepixel = mux(r == i, Pixel16(31.to[UInt5],0,0), Pixel16(0,0,0))
//             val greenpixel = mux(r == i, Pixel16(0,63.to[UInt6],0), Pixel16(0,0,0))
//             val redpixel = mux(r == i, Pixel16(0,0,31.to[UInt5]), Pixel16(0,0,0))
//             val pixel = mux(swBits.b1 == 0.to[UINT3], bluepixel, mux(swBits.b1 == 1.to[UINT3], greenpixel, redpixel))
//             imgOut := pixel
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