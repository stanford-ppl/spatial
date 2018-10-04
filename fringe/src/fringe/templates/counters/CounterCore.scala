package fringe.templates.counters

import chisel3._
import fringe.utils.Depulser

/** COunter configuration format. */
case class CounterOpcode(w: Int, startDelayWidth: Int, endDelayWidth: Int) extends Bundle {
  val max = UInt(w.W)
  val stride = UInt(w.W)
  val maxConst = Bool()
  val strideConst = Bool()
  val startDelay = {
    val delayWidth = math.max(startDelayWidth, 1)
    UInt(delayWidth.W)
  }
  var endDelay = {
    val delayWidth = math.max(startDelayWidth, 1)
    UInt(delayWidth.W)
  }

  val onlyDelay = Bool()

  override def cloneType(): this.type = CounterOpcode(w, startDelayWidth, endDelayWidth).asInstanceOf[this.type]
}

/**
 * CounterCore: Counter with optional start and end delays
 * @param w: Word width
 * @param startDelayWidth: Width of start delay counter
 * @param endDelayWidth: Width of end delay counter
 */
class CounterCore(val w: Int, val startDelayWidth: Int = 0, val endDelayWidth: Int = 0) extends Module {
  val io = IO(new Bundle {
    val max      = Input(UInt(w.W))
    val stride   = Input(UInt(w.W))
    val configuredMax = Output(UInt(w.W))
    val out      = Output(UInt(w.W))
    val next     = Output(UInt(w.W))
    val enable    = Input(Bool())
    val enableWithDelay = Output(Bool())
    val waitIn    = Input(Bool())
    val waitOut    = Output(Bool())
    val done   = Output(Bool())
    val isMax  = Output(Bool())
    val config = Input(CounterOpcode(w, startDelayWidth, endDelayWidth))
  })

  // Actual counter
  val counter = Module(new FringeCounter(w))
  counter.io.max := Mux(io.config.maxConst, io.config.max, io.max)
  counter.io.stride := Mux(io.config.strideConst, io.config.stride, io.stride)
  io.out := counter.io.out
  io.next := counter.io.next
  io.configuredMax := io.config.max
  val depulser = Module(new Depulser())
  depulser.io.in := counter.io.done

  // Assign counter enable signal
  if (startDelayWidth > 0) {
    // Start delay counter
    val startDelayCounter = Module(new FringeCounter(startDelayWidth))
    startDelayCounter.io.max := io.config.startDelay
    startDelayCounter.io.stride := 1.U(startDelayWidth.W)
    startDelayCounter.io.saturate := true.B

    val localEnable = io.enable & !io.waitIn
    startDelayCounter.io.enable := localEnable
    startDelayCounter.io.reset := Mux(io.config.onlyDelay, !io.enable, counter.io.done)
    counter.io.enable := startDelayCounter.io.done & !depulser.io.out
    io.enableWithDelay := startDelayCounter.io.done
  }
  else {
    counter.io.enable := io.enable
    io.enableWithDelay := io.enable
  }

  if (endDelayWidth > 0) {
    // End delay counter
    val endDelayCounter = Module(new FringeCounter(endDelayWidth))
    endDelayCounter.io.max := io.config.endDelay
    endDelayCounter.io.stride := 1.U(endDelayWidth.W)
    counter.io.reset := false.B
    endDelayCounter.io.saturate := false.B

    endDelayCounter.io.enable := depulser.io.out | counter.io.done
    depulser.io.rst := endDelayCounter.io.done
    counter.io.saturate := endDelayCounter.io.enable
    counter.io.reset := endDelayCounter.io.done
    io.done := endDelayCounter.io.done
  }
  else {
    counter.io.saturate := false.B
    counter.io.reset := false.B
    io.done := counter.io.done
  }

  // Control signal wiring up
  io.waitOut := io.waitIn | depulser.io.out
}
