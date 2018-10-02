package fringe.templates.retiming

import chisel3._
import chisel3.core.IntParam

// This wrapper is needed because we need to wire reset as an input,
// and reset is accessible only from within Modules in Chisel
class RetimeWrapper(val width: Int, val delay: Int, val init: Long) extends Module {
  val io = IO(new Bundle {
    val flow = Input(Bool())
    val in = Input(UInt(width.W))
    val out = Output(UInt(width.W))
  })

    val sr = Module(new RetimeShiftRegister(width, delay, init))
    sr.io.clock := clock
    sr.io.reset := reset.toBool
    sr.io.flow := io.flow
    sr.io.init := init.U
    sr.io.in := io.in
    io.out := sr.io.out
}

class RetimeWrapperWithReset(val width: Int, val delay: Int, val init: Long) extends Module {
  val io = IO(new Bundle {
    val flow = Input(Bool())
    val rst = Input(Bool())
    val in = Input(UInt(width.W))
    val out = Output(UInt(width.W))
  })

    if (delay > 0) {
      val sr = Module(new RetimeShiftRegister(width, delay, init))
      sr.io.clock := clock
      sr.io.reset := reset.toBool | io.rst
      sr.io.flow := io.flow
      sr.io.init := init.U
      sr.io.in := io.in
      io.out := sr.io.out      
    } else {
      io.out := io.in
    }
}

class RetimeShiftRegister(val width: Int, val delay: Int, val init: Long) extends BlackBox(
  Map(
    "WIDTH" -> IntParam(width),
    "STAGES" -> IntParam(delay)
    )
) {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())
    val flow = Input(Bool())
    val init = Input(UInt(width.W))
    val in = Input(UInt(width.W))
    val out = Output(UInt(width.W))
  })
}
