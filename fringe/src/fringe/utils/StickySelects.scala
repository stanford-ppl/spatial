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
	  val outs = Array.tabulate(n){i => 
      val r = RegInit(false.B)
      val elseActive = io.ins.patch(i,Nil,1).reduce{_||_}
      val meActive = io.ins(i)
      val next = Mux(elseActive, false.B, meActive | r)
      r := next
      next
    }
	  io.outs.zip(outs).foreach{case(o,i) => o := i}
  }
}
