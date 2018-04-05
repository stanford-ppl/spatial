package fringe

import chisel3._

//import plasticine.pisa.parser.Parser
//import plasticine.pisa.ir._
//
import scala.collection.mutable.HashMap

/**
 * Counter configuration format
 */
case class CounterOpcode(val w: Int, val startDelayWidth: Int, val endDelayWidth: Int) extends Bundle {
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

  override def cloneType(): this.type = {
    new CounterOpcode(w, startDelayWidth, endDelayWidth).asInstanceOf[this.type]
  }
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
  val counter = Module(new Counter(w))
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
    val startDelayCounter = Module(new Counter(startDelayWidth))
    startDelayCounter.io.max := io.config.startDelay
    startDelayCounter.io.stride := 1.U(startDelayWidth.W)
    startDelayCounter.io.saturate := true.B

    val localEnable = io.enable & ~io.waitIn
    startDelayCounter.io.enable := localEnable
    startDelayCounter.io.reset := Mux(io.config.onlyDelay, ~io.enable, counter.io.done)
    counter.io.enable := startDelayCounter.io.done & ~depulser.io.out
    io.enableWithDelay := startDelayCounter.io.done
  } else {
    counter.io.enable := io.enable
    io.enableWithDelay := io.enable
  }

  if (endDelayWidth > 0) {
    // End delay counter
    val endDelayCounter = Module(new Counter(endDelayWidth))
    endDelayCounter.io.max := io.config.endDelay
    endDelayCounter.io.stride := 1.U(endDelayWidth.W)
    counter.io.reset := false.B
    endDelayCounter.io.saturate := false.B

    endDelayCounter.io.enable := depulser.io.out | counter.io.done
    depulser.io.rst := endDelayCounter.io.done
    counter.io.saturate := endDelayCounter.io.enable
    counter.io.reset := endDelayCounter.io.done
    io.done := endDelayCounter.io.done
  } else {
    counter.io.saturate := false.B
    counter.io.reset := false.B
    io.done := counter.io.done
  }



  // Control signal wiring up
//  io.waitOut := depulser.io.out | counter.io.done | endDelayCounter.io.enable
  io.waitOut := io.waitIn | depulser.io.out
}

//class CounterCoreReg(val w: Int, val startDelayWidth: Int, val endDelayWidth: Int, inst: CounterCoreConfig) extends Module {
//  val io = new ConfigInterface {
//    val config_enable = Bool(INPUT)
//    val data = new Bundle {
//      val max      = UInt(INPUT,  w)
//      val stride   = UInt(INPUT,  w)
//      val out      = UInt(OUTPUT, w)
//    }
//    val control = new Bundle {
//      val enable    = Bool(INPUT)
//      val waitIn    = Bool(INPUT)
//      val waitOut    = Bool(OUTPUT)
//      val done   = Bool(OUTPUT)
//    }
//  }
//
//  // Register the inputs
//  val maxReg = Module(new FF(w))
//  maxReg.io.enable := true.B
//  maxReg.io.in := io.max
//  val max = maxReg.io.out
//
//  val strideReg = Module(new FF(w))
//  strideReg.io.enable := true.B
//  strideReg.io.in := io.stride
//  val stride = strideReg.io.out
//
//  val enableReg = Module(new FF(1))
//  enableReg.io.enable := true.B
//  enableReg.io.in := io.enable
//  val enable = enableReg.io.out
//
//  val waitInReg = Module(new FF(1))
//  waitInReg.io.enable := true.B
//  waitInReg.io.in := io.waitIn
//  val waitIn = waitInReg.io.out
//
//  // Instantiate counter
//  val counter = Module(new CounterCore(w, startDelayWidth, endDelayWidth, inst))
//  counter.io.config_enable := io.config_enable
//  counter.io.config_data := io.config_data
//  counter.io.max := max
//  counter.io.stride := stride
//  counter.io.enable := enable
//  counter.io.waitIn := waitIn
//
//  // Register outputs
//  val outReg = Module(new FF(w))
//  outReg.io.enable := true.B
//  outReg.io.in := counter.io.out
//  io.out := outReg.io.out
//  val waitOutReg = Module(new FF(1))
//  waitOutReg.io.enable := true.B
//  waitOutReg.io.in := counter.io.waitOut
//  io.waitOut := waitOutReg.io.out
//  val doneReg = Module(new FF(1))
//  doneReg.io.enable := true.B
//  doneReg.io.in := counter.io.done
//  io.done := doneReg.io.out
//}
//
///**
// * CounterCore test harness
// */
//class CounterCoreTests(c: CounterCore) extends PlasticineTester(c) {
//  val saturationVal = (1 << c.w) - 1  // Based on counter width
//
//  val max = 10 % saturationVal
//  val stride = 2
//
//  poke(c.io.max, max)
//  poke(c.io.stride, stride)
//
//  poke(c.io.enable, 1)
//  val count = peek(c.io.out)
//  val done = peek(c.io.done)
//  for (i <- 0 until 50) {
//    step(1)
//    val count = peek(c.io.out)
//    val done = peek(c.io.done)
//  }
//}
//
//class CounterCoreCharTests(c: CounterCoreReg) extends Tester(c)
//
//object CounterCoreTest {
//
//  def main(args: Array[String]): Unit = {
//    val (appArgs, chiselArgs) = args.splitAt(args.indexOf("end"))
//
//    if (appArgs.size != 1) {
//      println("Usage: bin/sadl CounterCoreTest <pisa config>")
//      sys.exit(-1)
//    }
//
//    val pisaFile = appArgs(0)
//    val configObj = Parser(pisaFile).asInstanceOf[CounterCoreConfig]
//    val bitwidth = 7
//    val startDelayWidth = 4
//    val endDelayWidth = 4
//
//    // Configuration passed to design as register initial values
//    // When the design is reset, config is set
//    println(s"parsed configObj: $configObj")
//
//    chiselMainTest(chiselArgs, () => Module(new CounterCore(bitwidth, startDelayWidth, endDelayWidth, configObj))) {
//      c => new CounterCoreTests(c)
//    }
//  }
//}
//
//object CounterCoreChar {
//  def main(args: Array[String]): Unit = {
//    val appArgs = args.take(args.indexOf("end"))
//    if (appArgs.size < 1) {
//      println("Usage: CounterCoreChar <w>")
//      sys.exit(-1)
//    }
//    val w = appArgs(0).toInt
//    val startDelayWidth = 4
//    val endDelayWidth = 4
//    val config = CounterCoreConfig.getRandom
//    chiselMainTest(args, () => Module(new CounterCoreReg(w, startDelayWidth, endDelayWidth, config))) {
//      c => new CounterCoreCharTests(c)
//    }
//  }
//}
//
