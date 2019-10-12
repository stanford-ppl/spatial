package spatial.tests.feature.streaming

import spatial.dsl._

@spatial class HelloCXP extends SpatialTest {
  override def backends = DISABLED

  def main(args: Array[String]): Unit = {
    // Declare SW-HW interface vals
    val in = StreamIn[U256](AxiStream256Bus)
    val out = StreamOut[U256](AxiStream256Bus)

    // Create HW accelerator
    Accel {
      Stream.Foreach(*){_ => 
        val in_queue = List.fill(256/8)(FIFO[U8](8))
        val out_queue = List.fill(256/8)(FIFO[U8](8))
        // Collect
        Pipe{
          val raw = in.value
          val pxls = List.tabulate(256/8){i => raw.bits(i*8+7::i*8).as[U8]}
          in_queue.zip(pxls).foreach{case (f, p) => f.enq(p)}
        }
        // Modify
        Pipe{
          out_queue.zip(in_queue).foreach{case (o,i) => 
            val p = i.deq()
            val edit = mux(p < 128, 0, p)
            o.enq(edit)
          }
        }
        // Send
        Pipe{
          out := cat(out_queue.map(_.deq().asBits):_*).as[U256]
        }
      }
    }

  }
}
