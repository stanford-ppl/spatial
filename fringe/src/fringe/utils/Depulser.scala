package fringe.utils

import chisel3._
import fringe.templates.memory.FringeFF

/** Depulser: 1-cycle pulse to a steady high signal. */
class Depulser() extends Module {
  val io = IO(new Bundle {
    val in = Input(Bool())
    val rst = Input(Bool())
    val out = Output(Bool())
  })

  val r = Module(new FringeFF(Bool()))
  r.io.in := Mux(io.rst, 0.U, io.in)
  r.io.init := 0.U
  r.io.enable := io.in | io.rst
  r.io.reset := io.rst
  io.out := r.io.out
}
