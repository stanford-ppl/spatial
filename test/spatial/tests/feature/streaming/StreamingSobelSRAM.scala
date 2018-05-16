// package spatial.tests.feature.streaming

// import spatial.dsl._

// object StreamingSobelSRAM extends SpatialApp {
//   override val target = DE1

//   val Kh = 3
//   val Kw = 3
//   val Rmax = 240
//   val Cmax = 320

//   type Int16 = FixPt[TRUE,_16,_0]
//   type UInt8 = FixPt[FALSE,_8,_0]
//   type UInt5 = FixPt[FALSE,_5,_0]
//   type UInt6 = FixPt[FALSE,_6,_0]
//   @struct class sw3(forward: Bit, backward: Bit, unused: UInt8)
//   @struct case class Pixel16(b: UInt5, g: UInt6, r: UInt5)


//   def convolveVideoStream(rows: Int, cols: Int): Unit = {
//     val imgIn  = StreamIn[Pixel16](target.VideoCamera)
//     val imgOut = BufferedOut[Pixel16](target.VGA)

//     Accel(*) {
//       val kv = LUT[Int16](3, 3)(
//         1,  2,  1,
//         0,  0,  0,
//         -1, -2, -1
//       )
//       val kh = LUT[Int16](3, 3)(
//         1,  0, -1,
//         2,  0, -2,
//         1,  0, -1
//       )

//       val sr = RegFile[Int16](Kh, Kw)
//       val frame = SRAM[Int16](Rmax, Cmax)
//       val lb = LineBuffer[Int16](Kh, Cmax)

//       Foreach(0 until Rmax, 0 until Cmax) { (r,c) =>
//         val pixel = imgIn.value()
//         val grayPixel = (pixel.b.to[Int16] + pixel.g.to[Int16] + pixel.r.to[Int16]) / 3
//         frame(r,c) = grayPixel
//       }

//       Foreach(0 until Rmax) { r =>
//         Foreach(0 until Cmax) { c =>
//           lb.enq(frame(r,c))
//         }
//         val horz = Reg[Int16](0)
//         val vert = Reg[Int16](0)
//         Foreach(0 until Cmax) { c =>
//           Foreach(0 until Kh par Kh) { i => sr(i, *) <<= lb(i, c) }

//           Reduce(horz)(Kh by 1, Kw by 1) { (i, j) =>
//             val number = mux(r < Kh-1 || c < Kw-1, 0.to[Int16], sr(i, j))
//             number * kh(i, j)
//           }{_+_}

//           Reduce(vert)(Kh by 1, Kw by 1) { (i, j) =>
//             val number = mux(r < Kh-1 || c < Kw-1, 0.to[Int16], sr(i, j))
//             number * kv(i, j)
//           }{_+_}

//           val result = abs(horz.value) + abs(vert.value)
//           imgOut(r,c) = Pixel16(result(5::1).as[UInt5], result(5::0).as[UInt6], result(5::1).as[UInt5]) // Technically should be sqrt(horz**2 + vert**2)
//         }
//       }
//       ()
//     }
//   }


//   def main(args: Array[String]): Unit = {
//     val R = Rmax
//     val C = Cmax
//     convolveVideoStream(R, C)
//   }
// }
