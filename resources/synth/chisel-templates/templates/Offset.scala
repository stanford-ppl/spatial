import Chisel._

class Offset(latency : Int) extends Module {
    val io = new Bundle {
        val signal_in = Bool(INPUT)
        val signal_out = Bool(OUTPUT)
    }
   var pipe = Module(new Pipe[Bool](Bool(), latency = latency))
   pipe.io.enq.valid := io.signal_in
   io.signal_out := pipe.io.deq.valid
}
