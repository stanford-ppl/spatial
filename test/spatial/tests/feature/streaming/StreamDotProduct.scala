package spatial.tests.feature.streaming

import spatial.dsl._
import spatial.lang.FileBus

@spatial class StreamDotProduct extends SpatialTest {
  val batchSize = 4
  val featureSize = 16

  val bp = 2
  val fp = 16

   def main(args: Array[String]): Unit = {
    // 
    val one = ArgIn[Int]
    val in  = StreamIn[Tup2[Int,Bit]](FileBus[Tup2[Int,Bit]](""))
    val out = StreamOut[Tup2[Int,Bit]](FileBus[Tup2[Int,Bit]](""))
    setArg(one, 1)
    Accel(*){
      val valid = FIFO[Bit](10)
      Foreach(one by 1) { _ =>
        val sram = SRAM[Int](batchSize, featureSize)
        Foreach(0 until featureSize) { j =>
          Foreach(0 until batchSize par batchSize) { i =>
            val batch = in.value()
            sram(i,j) = batch._1
            valid.enq(batch._2)
          }
        }
        Foreach(0 until batchSize par bp) { j =>
          val dot = Reg[Int]
          Reduce(dot)(0 until featureSize par fp) { i =>
            sram(i,j)
          } { _ + _ }
          Pipe {
            out := Tup2(dot.value, valid.deq())
          }
        }
      }
    }
    assert(true)
  }
}
