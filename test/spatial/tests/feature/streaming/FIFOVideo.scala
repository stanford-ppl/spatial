package spatial.tests.feature.streaming

import spatial.dsl._


// object FIFOVideo extends SpatialApp {
//   override val target = DE1

//   val Kh = 3
//   val Kw = 3
//   val Rmax = 240
//   val Cmax = 320

//   @struct class sw3(forward: Bit, backward: Bit, unused: UInt8)

//   @struct case class Pixel16(b: UInt5, g: UInt6, r: UInt5)


//   def convolveVideoStream(rows: Int, cols: Int): Unit = {
//     val imgOut = BufferedOut[Pixel16](target.VGA)
//     val imgIn  = StreamIn[Pixel16](target.VideoCamera)

//     Accel(*) {

//       val fifoIn = FIFO[Pixel16](Cmax*2)
//       Foreach(0 until Rmax) { i =>
//         Foreach(0 until Cmax ) { _ =>
//           fifoIn.enq(imgIn.value())
//         }

//         Foreach(0 until Cmax) { j =>
//           val pixel = fifoIn.deq()
//           imgOut(i,j) = Pixel16(pixel.b, pixel.g, pixel.r)
//         }
//       }
//       // ()
//     }
//   }


//   def main(args: Array[String]): Unit = {
//     val R = Rmax
//     val C = Cmax
//     convolveVideoStream(R, C)
//   }
// }

