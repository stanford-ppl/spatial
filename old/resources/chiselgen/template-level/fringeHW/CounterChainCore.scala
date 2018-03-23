package fringe

import chisel3._

import scala.collection.mutable.HashMap

/**
 * CounterChain config register format
 */
case class CounterChainOpcode(val w: Int, val numCounters: Int, val startDelayWidth: Int, val endDelayWidth: Int) extends Bundle {
  val chain = Vec(numCounters-1, Bool())
  val counterOpcode = Vec(numCounters, CounterOpcode(w, startDelayWidth, endDelayWidth))

  override def cloneType(): this.type = {
    new CounterChainOpcode(w, numCounters, startDelayWidth, endDelayWidth).asInstanceOf[this.type]
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

///**
// * CounterChain test harness
// */
//class CounterChainTests(c: CounterChain) extends PlasticineTester(c) {
//  val saturationVal = (1 << c.w) - 1  // Based on counter width
//
//  val max = 10 % saturationVal
//  val stride = 2
//  val saturate = 0
//
//  (0 until c.numCounters) map { i =>
//    poke(c.io.data(i).max, max)
//    poke(c.io.data(i).stride, stride)
//    poke(c.io.control(i).enable, 1)
//  }
//
//  val counts = peek(c.io.data)
//  val done = peek(c.io.control)
//  for (i <- 0 until 50) {
//    step(1)
//    c.io.data foreach { d => peek(d.out) }
//    val done = peek(c.io.control)
//  }
//}
//
//object CounterChainTest {
//
//  def main(args: Array[String]): Unit = {
//    val (appArgs, chiselArgs) = args.splitAt(args.indexOf("end"))
//
//    if (appArgs.size != 1) {
//      println("Usage: bin/sadl CounterChainTest <pisa config>")
//      sys.exit(-1)
//    }
//
//    val pisaFile = appArgs(0)
//    val configObj = Parser(pisaFile).asInstanceOf[CounterChainConfig]
//    val bitwidth = 8
//    val numCounters = 8
//    val startDelayWidth = 4
//    val endDelayWidth = 4
//
//    chiselMainTest(args, () => Module(new CounterChain(bitwidth, startDelayWidth, endDelayWidth, numCounters, configObj))) {
//      c => new CounterChainTests(c)
//    }
//  }
//}
