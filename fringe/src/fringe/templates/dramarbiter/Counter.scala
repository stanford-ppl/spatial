package fringe.templates.dramarbiter

import chisel3._
import chisel3.util._

class Counter(val w: Int) extends Module {
  val io = IO(new Bundle {
    val reset = Input(Bool())
    val enable = Input(Bool())
    val stride = Input(UInt(w.W))
    val out = Output(UInt(w.W))
    val next = Output(UInt(w.W))
  })

  val count = RegInit(0.U)

  val newCount = count + io.stride

  when(io.reset) {
    count := 0.U
  } .elsewhen(io.enable) {
    count := newCount
  }

  io.out := count
  io.next := newCount
}

