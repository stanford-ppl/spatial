// package spatial.tests.feature.streaming

// import spatial.dsl._
// import spatial.lang.CXPPixelBus

// @spatial class HelloCXP extends SpatialTest {
//   override def backends = DISABLED

//   def main(args: Array[String]): Unit = {
//     // Declare SW-HW interface vals
//     val in = StreamIn[U256](CXPPixelBus)
//     val out = StreamOut[U256](CXPPixelBus)

//     // Create HW accelerator
//     Accel {
//       Stream.Foreach(*){_ => 
//       	out := in.value
//       }
//     }

//   }
// }