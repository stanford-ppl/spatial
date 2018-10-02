package fringe.templates.counters

import chisel3._
import fringe.templates.memory.FringeFF

/** A 1-dimensional counter. Counts up to 'max', each time incrementing
  * by 'stride', beginning at zero.
  * @param w: Word width
  */
class UpDownCtr(val w: Int) extends Module {
  val io = IO(new Bundle {
    val initval  = Input(UInt(w.W))
    val max      = Input(UInt(w.W))
    val strideInc    = Input(UInt(w.W))
    val strideDec    = Input(UInt(w.W))
    val initAtConfig = Input(Bool())
    val init     = Input(Bool())
    val inc      = Input(Bool())
    val dec      = Input(Bool())
    val gtz      = Output(Bool())  // greater-than-zero
    val isMax    = Output(Bool())
    val out      = Output(UInt(w.W))
    val nextInc  = Output(UInt(w.W))
    val nextDec  = Output(UInt(w.W))
  })

  val reg = Module(new FringeFF(UInt(w.W)))
  val configInit = Mux(io.initAtConfig, io.initval, 0.U(w.W))
  reg.io.init := configInit

  // If inc and dec go high at the same time, the counter
  // should change value based on strideInc and strideDec
  reg.io.enable := io.inc | io.dec | io.init

  val incval = Mux(io.inc, io.strideInc, 0.U)
  val decval = Mux(io.dec, io.strideDec, 0.U)
  val incr = incval - decval

  val newval = reg.io.out + incr
  io.isMax := newval > io.max
  reg.io.in := Mux (io.init, io.initval, newval)
  io.gtz := (reg.io.out > 0.U)
  io.out := reg.io.out
  io.nextInc := reg.io.out + io.strideInc
  io.nextDec := reg.io.out - io.strideDec
}


