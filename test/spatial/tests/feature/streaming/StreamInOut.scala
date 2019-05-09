 package spatial.tests.feature.streaming

 import spatial.dsl._

@spatial class StreamInOut extends SpatialTest {

    def main(args: Array[String]): Unit = {
     val in  = StreamIn[Tup2[Int,Bit]](FileBus[Tup2[Int,Bit]])
     val out = StreamOut[Tup2[Int,Bit]](FileBus[Tup2[Int,Bit]])
     Accel {
       Stream {
         out := in
       }
     }
   }
 }
