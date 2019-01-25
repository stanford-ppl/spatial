package fringe.templates.memory

import chisel3._

class SRFF(val strongReset: Boolean = false) extends Module {

  // Overload with null string input for testing
  def this(n: String) = this()

  val io = IO(new Bundle {
    val input = new Bundle {
      val set = Input(Bool()) // Set overrides reset.  Asyn_reset overrides both
      val reset = Input(Bool())
      val asyn_reset = Input(Bool())
    }
    val output = new Bundle {
      val data = Output(Bool())
    }
  })

  if (!strongReset) { // Set + reset = on
    val ff = RegInit(false.B)
    ff := Mux(io.input.asyn_reset, false.B, Mux(io.input.set, true.B, Mux(io.input.reset, false.B, ff)))
    io.output.data := Mux(io.input.asyn_reset, false.B, ff)
  } else { // Set + reset = off
    val ff = RegInit(false.B)
    ff := Mux(io.input.asyn_reset, false.B, Mux(io.input.reset, false.B, Mux(io.input.set, true.B, ff)))
    io.output.data := Mux(io.input.asyn_reset, false.B, ff)
  }
}