package fringe.templates.counters

import chisel3._

/** CounterChain config register format */
case class CounterChainOpcode(w: Int, numCounters: Int, startDelayWidth: Int, endDelayWidth: Int) extends Bundle {
  val chain = Vec(numCounters-1, Bool())
  val counterOpcode = Vec(numCounters, CounterOpcode(w, startDelayWidth, endDelayWidth))

  override def cloneType(): this.type = {
    CounterChainOpcode(w, numCounters, startDelayWidth, endDelayWidth).asInstanceOf[this.type]
  }
}

class CounterChainCore(
  val w: Int,
  val numCounters: Int,
  val startDelayWidth: Int = 0,
  val endDelayWidth: Int = 0) extends Module {
  val io = IO(new Bundle {
    val max      = Input(Vec(numCounters, UInt(w.W)))
    val stride   = Input(Vec(numCounters, UInt(w.W)))
    val configuredMax = Output(Vec(numCounters, UInt(w.W)))
    val out      = Output(Vec(numCounters, UInt(w.W)))
    val next     = Output(Vec(numCounters, UInt(w.W)))

    val enable = Input(Vec(numCounters, Bool()))
    val enableWithDelay = Output(Vec(numCounters, Bool()))
    val done   = Output(Vec(numCounters, Bool()))
    val config = Input(CounterChainOpcode(w, numCounters, startDelayWidth, endDelayWidth))
  })

  val counters = (0 until numCounters) map { i =>
    val c = Module(new CounterCore(w, startDelayWidth, endDelayWidth))
    c.io.max := io.max(i)
    io.configuredMax(i) := c.io.configuredMax
    c.io.stride := io.stride(i)
    c.io.config := io.config.counterOpcode(i)
    io.out(i) := c.io.out
    io.next(i) := c.io.next
    c
  }

  // Create chain reconfiguration logic
  (0 until numCounters) foreach { i: Int =>
    // Enable-done chain
    if (i == 0) {
      counters(i).io.enable := io.enable(i)
    } else {
      counters(i).io.enable := Mux(io.config.chain(i-1),
        counters(i-1).io.done,
        io.enable(i))
    }

    // waitIn - waitOut chain
    if (i == numCounters-1) {
      counters(i).io.waitIn := false.B
    } else {
      counters(i).io.waitIn := Mux(io.config.chain(i),
                                      counters(i+1).io.waitOut,
                                      false.B)
    }
    io.done(i) := counters(i).io.done
    io.enableWithDelay(i) := counters(i).io.enableWithDelay
  }
}
