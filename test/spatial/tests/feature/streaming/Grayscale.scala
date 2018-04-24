// package spatial.tests.feature.streaming

// import spatial.dsl._

// object Grayscale extends SpatialApp {
//   override val target = DE1

//   val Kh = 3
//   val Kw = 3
//   val Rmax = 240
//   val Cmax = 320

//   //  @struct case class Pixel24(b: UInt8, g: UInt8, r: UInt8)
//   @struct case class Pixel16(b: UInt5, g: UInt6, r: UInt5)


//   def convolveVideoStream(rows: Int, cols: Int): Unit = {
//     val imgOut = BufferedOut[Pixel16](target.VGA)
//     val imgIn  = StreamIn[Pixel16](target.VideoCamera)

//     Accel(*) {


//       val fifoIn = FIFO[Int16](Cmax*2)
//       Foreach(0 until Rmax) { i =>
//         Foreach(0 until Cmax) { j =>
//           val pixel = imgIn.value()
//           val grayPixel = (pixel.b.to[Int16] + pixel.g.to[Int16] + pixel.r.to[Int16]) / 3
//           fifoIn.enq(grayPixel)
//         }
//         Foreach(0 until Cmax) { j =>
//           val pixel = fifoIn.deq()
//           imgOut(i,j) = Pixel16(pixel(5::1).as[UInt5], pixel(5::0).as[UInt6], pixel(5::1).as[UInt5])
//         }
//       }
//     }
//   }


//   def main(args: Array[String]): Unit = {
//     val R = Rmax
//     val C = Cmax
//     convolveVideoStream(R, C)
//   }
// }


