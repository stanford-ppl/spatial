package fringe.templates.memory

import chisel3._

/** TFF: Flip-flop with the ability to set enable and init
  * value as IO
  */
class TFF() extends Module {

  // Overload with null string input for testing
  def this(n: String) = this()

  val io = IO(new Bundle {
    val input = new Bundle {
      val enable = Input(Bool())
    }
    val output = new Bundle {
      val data = Output(Bool())      
    }
  })

  val ff = RegInit(false.B)
  ff := Mux(io.input.enable, ~ff, ff)
  io.output.data := ff
}
