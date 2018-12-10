package fringe.utils

import chisel3._

/** Converts a rising edge to a 1-cycle pulse. */
class StickySelects(n: Int) extends Module {
  val io = IO(new Bundle {
    val ins = Vec(n, Input(Bool()))
    val outs = Vec(n, Output(Bool()))
  })

  if (n == 1) io.outs(0) := io.ins(0)
  else {
	  val regs = Array.tabulate(n){i => RegInit(false.B)}
	  regs.zipWithIndex.foreach{case(r,i) => 
	  	r := Mux(io.ins(i), true.B, Mux(io.outs.patch(i,Nil,1).reduce{_||_}, false.B, r))
	  }
	  io.outs.zipWithIndex.foreach{case(o,i) => o := Mux(io.ins(i), true.B, regs(i))}
  }
}
