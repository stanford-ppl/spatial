package spatial.tests.feature.streaming

import spatial.SpatialApp
import spatial.dsl._


// object StreamTest extends SpatialApp {


//   def main(args: Array[String]): Unit = {
//     type T = Int

//     val frameRows = 16
//     val frameCols = 16
//     val onboardVideo = target.VideoCamera
//     val mem = DRAM[T](frameRows, frameCols)
//     val conduit = StreamIn[T](onboardVideo)
//     // val avalon = StreamOut()

//     // Raw Spatial streaming pipes
//     Accel {
//       Foreach(*, frameCols by 1) { (_,j) =>
//         val fifo1 = FIFO[T](frameCols)
//         val fifo2 = FIFO[T](frameCols)
//         Stream(frameCols by 1) { j =>
//           Pipe {
//             fifo1.enq(conduit)
//           }
//           Pipe {
//             val pop = fifo1.deq()
//             fifo2.enq(pop)
//           }
//           Pipe {
//             val pop = fifo2.deq()
//             // avalon.enq(pop)
//           }
//         }
//       }
//     }
//   }
// }

