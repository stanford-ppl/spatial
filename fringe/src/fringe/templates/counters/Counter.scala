// See LICENSE.txt for license details.
package fringe.templates.counters

import chisel3._
import fringe.utils.getRetimed
import fringe.utils.HVec
import fringe.templates.memory.{SRFF, FF}

class CChainOutput(par: List[Int], widths: List[Int]) extends Bundle {
  // val ctrMapping = par.indices.map{i => par.dropRight(par.length - i).sum}
  val counts    = HVec.tabulate(par.sum){i => SInt(widths(par.indices.map{i => par.dropRight(par.length - i).sum}.count(_ <= i) - 1).W) }
  val oobs      = Vec(par.sum, Bool())
  val noop      = Bool()
  val done      = Bool()
  val saturated = Bool()

  override def cloneType(): this.type = new CChainOutput(par, widths).asInstanceOf[this.type]
}

/** 1-dimensional counter. Basically a cheap, wrapping counter because
  * Chisel optimizes away a Vec(1) to a single val,
  * but still forces you to index the thing and hence only gets the first bit.
  */
class NBufCtr(
  val stride: Int = 1,
  val start: Option[Int],
  val stop: Option[Int],
  val init: Int = 0,
  val width: Int = 32
) extends Module {

  val io = IO(new Bundle {
    val setup = new Bundle {
      val start   = Input(UInt(width.W))
      val stop    = Input(UInt(width.W))
    }
    val input = new Bundle {
      val countUp = Input(Bool())
      val enable  = Input(Bool())
    }
    val output = new Bundle {
      val count   = Output(UInt(width.W))
    }
  })

  if (start.isDefined && stop.isDefined) {
    val cnt = Wire(UInt(width.W))

    val effectiveCnt = Mux(cnt + start.get.U(width.W) >= stop.get.U(width.W), (cnt.asSInt + (start.get-stop.get).S(width.W)).asUInt, cnt + start.get.U(width.W))

    val nextCntDown = Mux(io.input.enable, Mux(cnt === 0.U(width.W), (stop.get-stride).U(width.W), cnt-stride.U(width.W)), cnt) // TODO: This could be an issue if strided counter is used in reverse
    val nextCntUp = Mux(io.input.enable, Mux(cnt + stride.U(width.W) >= stop.get.U(width.W), 0.U(width.W) + (cnt.asSInt + (stride - stop.get).S(width.W)).asUInt, cnt+stride.U(width.W)), cnt)
    cnt := getRetimed(Mux(reset.toBool, init.U, Mux(io.input.countUp, nextCntUp, nextCntDown)), 1, init = init.toLong)

    io.output.count := effectiveCnt
  } else if (stop.isDefined) {
    val cnt = Wire(UInt(width.W))

    val effectiveCnt = Mux(cnt + io.setup.start >= stop.get.U(width.W), cnt + io.setup.start - stop.get.U(width.W), cnt + io.setup.start)

    val nextCntDown = Mux(io.input.enable, Mux(cnt === 0.U(width.W), (stop.get-stride).U(width.W), cnt-stride.U(width.W)), cnt) // TODO: This could be an issue if strided counter is used in reverse
    val nextCntUp = Mux(io.input.enable, Mux(cnt + stride.U(width.W) >= stop.get.U(width.W), 0.U(width.W) + cnt+stride.U(width.W) - stop.get.U(width.W), cnt+stride.U(width.W)), cnt)
    cnt := getRetimed(Mux(reset.toBool, init.U(width.W), Mux(io.input.countUp, nextCntUp, nextCntDown)), 1, init = init.toLong)

    io.output.count := effectiveCnt
  } else {
    val cnt = Wire(UInt(width.W))

    val effectiveCnt = Mux(cnt + io.setup.start >= io.setup.stop, cnt + io.setup.start - io.setup.stop, cnt + io.setup.start)

    val nextCntDown = Mux(io.input.enable, Mux(cnt === 0.U(width.W), io.setup.stop-stride.U(width.W), cnt-stride.U(width.W)), cnt) // TODO: This could be an issue if strided counter is used in reverse
    val nextCntUp = Mux(io.input.enable, Mux(cnt + stride.U(width.W) >= io.setup.stop, 0.U(width.W) + cnt+stride.U(width.W) - io.setup.stop, cnt+stride.U(width.W)), cnt)
    cnt := getRetimed(Mux(reset.toBool, init.U(width.W), Mux(io.input.countUp, nextCntUp, nextCntDown)), 1, init = init.toLong)

    io.output.count := effectiveCnt
  }
}


/**
  * IncDincCtr: 1-dimensional counter, used in tracking number of elements when you push and pop
               from a fifo
  */
class IncDincCtr(inc: Int, dinc: Int, stop: Int, width: Int = 32) extends Module {
  val io = IO(new Bundle {
    val input = new Bundle {
      val inc_en     = Input(Bool())
      val dinc_en    = Input(Bool())
    }
    val output = new Bundle {
      val overread    = Output(Bool())
      val overwrite   = Output(Bool())
      val empty       = Output(Bool())
      val full        = Output(Bool())
      val almostEmpty = Output(Bool())
      val almostFull  = Output(Bool())
      val numel       = Output(SInt((width+1).W))
    }
  })

  val cnt = RegInit(0.S(32.W))

  val numPushed = Mux(io.input.inc_en, inc.S, 0.S((width+1).W))
  val numPopped = Mux(io.input.dinc_en, dinc.S, 0.S((width+1).W))
  cnt := cnt + numPushed - numPopped

  io.output.overread := cnt < 0.S((width+1).W)
  io.output.overwrite := cnt > stop.S((width+1).W)
  io.output.empty := cnt === 0.S((width+1).W)
  io.output.almostEmpty := cnt - dinc.S((width+1).W) === 0.S((width+1).W)
  io.output.full := cnt === stop.S((width+1).W)
  io.output.almostFull := cnt + inc.S((width+1).W) === stop.S((width+1).W)
  io.output.numel := cnt
}



/**
  * IICounter: 1-dimensional counter. Basically a cheap, wrapping for reductions
  */
class IICounter(val ii: Int, val width: Int = 32, val myName: String = "iiCtr") extends Module {
  val io = IO(new Bundle {
    val input = new Bundle {
      val enable = Input(Bool())
      val reset  = Input(Bool())
    }
    val output = new Bundle {
      val issue = Output(Bool())
      val done   = Output(Bool())
    }
  })

  override def desiredName: String = myName
  if (ii > 1) {
    val cnt = RegInit((ii-1).S(width.W))
    // TODO: iiCtr used to issue done on the very first cycle, which is not correct but it's unclear how much other logic relies on this.
    //       Switching it to cnt == 0 rather than cnt == ii-1 for now and seeing what breaks
    val isIssue = (cnt === (ii-1).S(width.W)) & io.input.enable
    val isDone = (cnt === 0.S(width.W)) & io.input.enable

    val nextLive = Mux(cnt === 0.S(width.W), (ii-1).S(width.W), cnt-1.S(width.W))
    val next = Mux(io.input.enable, nextLive, cnt)
    cnt := Mux(io.input.reset, (ii-1).S(width.W), next)

    io.output.done := isDone
    io.output.issue := isIssue
  } else {
    io.output.done := true.B
    io.output.issue := true.B
  }
}

class CompactingIncDincCtr(inc: Int, dinc: Int, widest_inc: Int, widest_dinc: Int, stop: Int, width: Int = 32) extends Module {
  val io = IO(new Bundle {
    val input = new Bundle {
      val inc_en     = Vec(1 max inc, Input(Bool()))
      val dinc_en    = Vec(1 max dinc, Input(Bool()))
    }
    val output = new Bundle {
      val overread    = Output(Bool())
      val overwrite   = Output(Bool())
      val empty       = Output(Bool())
      val full        = Output(Bool())
      val almostEmpty = Output(Bool())
      val almostFull  = Output(Bool())
      val numel       = Output(SInt((width+1).W))
    }
  })

  val cnt = RegInit(0.S(32.W))

  val numPushed = io.input.inc_en.map{e => Mux(e, 1.S(width.W), 0.S(width.W))}.reduce{_+_}
  val numPopped = io.input.dinc_en.map{e => Mux(e, 1.S(width.W), 0.S(width.W))}.reduce{_+_}
  cnt := cnt + numPushed - numPopped

  io.output.overread := cnt < 0.S((width+1).W)
  io.output.overwrite := cnt > stop.S((width+1).W)
  io.output.empty := cnt === 0.S((width+1).W)
  io.output.almostEmpty := cnt - widest_dinc.S((width+1).W) === 0.S((width+1).W)
  io.output.full := cnt > (stop-widest_inc).S((width+1).W)
  io.output.almostFull := cnt + widest_inc.S((width+1).W) === stop.S((width+1).W)
  io.output.numel := cnt
}

class CompactingCounter(val lanes: Int, val depth: Int, val width: Int) extends Module {
  def this(tuple: (Int, Int, Int)) = this(tuple._1, tuple._2, tuple._3)
  val io = IO(new Bundle {
    val input = new Bundle {
      val dir     = Input(Bool())
      val reset   = Input(Bool())
      val enables = Vec(1 max lanes, Input(Bool()))
    }
    val output = new Bundle {
      val done   = Output(Bool())
      val count  = Output(SInt(width.W))
    }
  })

  val base = Module(new FF(width))
  base.io <> DontCare
  base.io.wPort(0).init := 0.U(width.W)
  base.io.wPort(0).reset := io.input.reset
  base.io.wPort(0).en.head := io.input.enables.reduce{_|_}

  val count = base.io.rPort(0).output(0).asSInt
  val num_enabled: SInt = io.input.enables.map{e => Mux(e, 1.S(width.W), 0.S(width.W))}.reduce{_+_}
  val newval = count + Mux(io.input.dir, num_enabled, -num_enabled)
  val isMax = Mux(io.input.dir, newval >= depth.S, newval <= 0.S)
  val next = Mux(isMax, newval - depth.S(width.W), newval)
  base.io.wPort(0).data.head := Mux(io.input.reset, 0.asUInt, next.asUInt)

  io.output.count := base.io.rPort(0).output(0).asSInt
  io.output.done := io.input.enables.reduce{_|_} & isMax
}

class InstrumentationCounter(val width: Int = 64) extends Module {
  val io = IO(new Bundle {
    val enable = Input(Bool())
    val count = Output(UInt(width.W))
  })

  val ff = RegInit(0.U(width.W))
  ff := Mux(io.enable, ff + 1.U(width.W), ff)
  io.count := ff
}
/**
  * SingleCounter: 1-dimensional counter. Counts upto 'stop', each time incrementing
  * by 'stride', beginning at zero.
  * @param w: Word width
  */
class SingleCounter(val par: Int, val start: Option[Int], val stop: Option[Int],
                    val stride: Option[Int], val forever: Boolean, val width: Int = 32) extends Module {
  def this(tuple: (Int, Option[Int], Option[Int], Option[Int], Boolean, Int)) = this(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6)

  val io = IO(new Bundle {
    val setup = new Bundle {
      val start  = Input(SInt(width.W)) // TODO: Currently resets to "start" but wraps to 0, is this normal behavior?
      val stop   = Input(SInt(width.W))
      val stride = Input(SInt(width.W))
      val saturate = Input(Bool())
    }
    val input = new Bundle {
      // val wrap     = BoolInput(()) // TODO: This should let
      //                                   user specify (8 by 3) ctr to go
      //                                   0,3,6 (wrap) 1,4,7 (wrap) 2,5...
      //                                   instead of default
      //                                   0,3,6 (wrap) 0,3,6 (wrap) 0,3...
      val reset  = Input(Bool())
      val enable = Input(Bool())
    }
    val output = new Bundle {
      val count      = Vec(1 max par, Output(SInt(width.W)))
      val oobs        = Vec(1 max par, Output(Bool()))
      val noop   = Output(Bool())
      val done   = Output(Bool())
      val saturated = Output(Bool())
    }
  })

  io <> DontCare
  val bases = List.tabulate(par){i => val x = Module(new FF(width)); x.io <> DontCare; x}
  if (par > 0 && !forever) {
    val lock = Module(new SRFF())
    lock.io.input.set := io.input.enable  & !io.input.reset
    lock.io.input.reset := io.input.reset || io.output.done
    lock.io.input.asyn_reset := false.B
    val locked = lock.io.output// | io.input.enable
    val inits = List.tabulate(par){i =>
      getRetimed(
        if (start.isDefined & stride.isDefined) {(start.get + i*stride.get).S(width.W)}
        else if (start.isDefined) {start.get.S(width.W) + i.S(width.W) * io.setup.stride}
        else if (stride.isDefined) {io.setup.start + (i*stride.get).S(width.W)}
        else {io.setup.start + i.S(width.W) * io.setup.stride}
        ,0) // Maybe delay by 1 cycle just in case this is a critical path.  Init should never change during execution
    }
    bases.zipWithIndex.foreach{ case (b,i) =>
      b.io.rPort(0) <> DontCare
      b.io.wPort(0)   <> DontCare
      b.io.reset := false.B
      b.io.wPort(0).init := inits(i).asUInt
      b.io.wPort(0).reset := io.input.reset |
        {if (stride.isDefined) false.B else {getRetimed(io.setup.stride,1) =/= io.setup.stride}} |
        {if (start.isDefined) false.B else {!(locked | io.input.enable) && (getRetimed(io.setup.start,1) =/= io.setup.start)}}
      b.io.wPort(0).en.head := io.input.enable
    }

    val counts = bases.map(_.io.rPort(0).output(0).asSInt)
    val delta = if (stride.isDefined) { (stride.get*par).S(width.W) }
    else if (stride.isDefined) { (stride.get*par).S(width.W)}
    else { io.setup.stride * par.S(width.W)}
    val newvals = counts.map( _ + delta)
    val isMax = Mux({if (stride.isDefined) stride.get.S(width.W) else io.setup.stride} >= 0.S(width.W),
      newvals(0) >= {if (stop.isDefined) stop.get.S(width.W) else io.setup.stop},
      newvals(0) <= {if (stop.isDefined) stop.get.S(width.W) else io.setup.stop}
    )
    val wasMax = RegNext(isMax, false.B)
    val wasEnabled = RegNext(io.input.enable, false.B)
    bases.zipWithIndex.foreach {case (b,i) =>
      b.io.wPort(0).data.head := Mux(io.input.reset, inits(i).asUInt, Mux(isMax, Mux(io.setup.saturate, counts(i).asUInt, inits(i).asUInt), newvals(i).asUInt))
    }

    if(stride.isDefined) {
      (0 until {1 max par}).foreach { i =>
        io.output.count(i) := counts(i)
      }
    } else {
      (0 until {1 max par}).foreach { i =>
        io.output.count(i) := counts(i)
      }
    }

    // Connect oobies (Out Of Bound-ies)
    val defs = {if (start.isDefined) 0x4 else 0x0} | {if (stop.isDefined) 0x2 else 0x0} | {if (stride.isDefined) 0x1 else 0x0}
    (0 until {1 max par}).foreach{ i =>
      // Changing start and resetting counts(i) takes a cycle, so forward the init value if required
      val c = defs match {
        case 0x7 | 0x6 | 0x5 | 0x4 => counts(i)
        case 0x3 | 0x2 | 0x1 | 0x0 => Mux(!locked && (getRetimed(io.setup.start,1) =/= io.setup.start), inits(i), counts(i))
      }
      // Connections are a mouthful but it is historically unsafe to trust that chisel will optimize constants properly
      defs match {
        case 0x7 => if (stride.get >= 0) io.output.oobs(i) :=                                      c < start.get.S(width.W) || c >= stop.get.S(width.W) else io.output.oobs(i) := c > start.get.S(width.W) || c <= stop.get.S(width.W)
        case 0x6 =>                      io.output.oobs(i) := Mux(io.setup.stride >= 0.S(width.W), c < start.get.S(width.W) || c >= stop.get.S(width.W),                          c > start.get.S(width.W) || c <= stop.get.S(width.W))
        case 0x5 => if (stride.get >= 0) io.output.oobs(i) :=                                      c < start.get.S(width.W) || c >= io.setup.stop       else io.output.oobs(i) := c > start.get.S(width.W) || c <= io.setup.stop
        case 0x4 =>                      io.output.oobs(i) := Mux(io.setup.stride >= 0.S(width.W), c < start.get.S(width.W) || c >= io.setup.stop,                                c > start.get.S(width.W) || c <= io.setup.stop)
        case 0x3 => if (stride.get >= 0) io.output.oobs(i) :=                                      c < io.setup.start       || c >= stop.get.S(width.W) else io.output.oobs(i) := c > io.setup.start       || c <= stop.get.S(width.W)
        case 0x2 =>                      io.output.oobs(i) := Mux(io.setup.stride >= 0.S(width.W), c < io.setup.start       || c >= stop.get.S(width.W),                          c > io.setup.start       || c <= stop.get.S(width.W))
        case 0x1 => if (stride.get >= 0) io.output.oobs(i) :=                                      c < io.setup.start       || c >= io.setup.stop       else io.output.oobs(i) := c > io.setup.start       || c <= io.setup.stop
        case _   =>                      io.output.oobs(i) := Mux(io.setup.stride >= 0.S(width.W), c < io.setup.start       || c >= io.setup.stop,                                c > io.setup.start       || c <= io.setup.stop)
      }
    }

    io.output.done := io.input.enable & isMax
    defs match {
      case 0x7 | 0x6 => io.output.noop := (start.get == stop.get).B
      case 0x5 | 0x4 => io.output.noop := start.get.S(width.W) === io.setup.stop
      case 0x3 | 0x2 => io.output.noop := io.setup.start       === stop.get.S(width.W)
      case 0x1 | 0x0 => io.output.noop := io.setup.start       === io.setup.stop
    }
    io.output.saturated := io.setup.saturate & isMax
  }
  else { // Forever counter
    bases.foreach{ b =>
      b.io.wPort(0).init := 0.U
      b.io.wPort(0).reset := io.input.reset
      b.io.wPort(0).en.head := io.input.enable
      b.io.wPort(0).data.head := (b.io.rPort(0).output.head.asSInt + 1.S).asUInt
    }

    io.output.count(0) := bases.head.io.rPort(0).output.head.asSInt
    io.output.saturated := false.B
    io.output.noop := false.B
    io.output.done := false.B

  }
}

class SingleSCounter(val par: Int, val width: Int = 32) extends Module { // Signed counter, used in FILO
  def this(tuple: (Int, Int)) = this(tuple._1, tuple._2)

  val io = IO(new Bundle {
    val setup = new Bundle {
      val start    = Input(SInt(width.W)) // TODO: Currently resets to "start" but wraps to 0, is this normal behavior?
      val stop      = Input(SInt(width.W))
      val stride   = Input(SInt(width.W))
      val saturate = Input(Bool())
    }
    val input = new Bundle {
      // val wrap     = BoolInput(()) // TODO: This should let
      //                                   user specify (8 by 3) ctr to go
      //                                   0,3,6 (wrap) 1,4,7 (wrap) 2,5...
      //                                   instead of default
      //                                   0,3,6 (wrap) 0,3,6 (wrap) 0,3...
      val reset  = Input(Bool())
      val enable = Input(Bool())
    }
    val output = new Bundle {
      val count      = Vec(par, Output(SInt(width.W)))
      val done   = Output(Bool())
      val saturated = Output(Bool())
    }
  })

  if (par > 0) {
    val base = Module(new FF(width))
    val init = io.setup.start
    base.io.wPort(0).init := init.asUInt
    base.io.wPort(0).reset := io.input.reset
    base.io.wPort(0).en.head := io.input.reset | io.input.enable

    val count = base.io.rPort(0).output(0).asSInt
    val newval = count + (io.setup.stride * par.S(width.W))
    val isMax = newval >= io.setup.stop
    val wasMax = RegNext(isMax, false.B)
    val isMin = newval < 0.S(width.W)
    val wasMin = RegNext(isMin, false.B)
    val wasEnabled = RegNext(io.input.enable, false.B)
    val next = Mux(isMax, Mux(io.setup.saturate, count, init), Mux(isMin, io.setup.stop + io.setup.stride, newval))
    base.io.wPort(0).data.head := Mux(io.input.reset, init.asUInt, next.asUInt)

    (0 until par).foreach { i => io.output.count(i) := count + i.S(width.W)*io.setup.stride } // TODO: If I use *-* here, BigIPSim doesn't see par.S as a constant (but it sees par.U as one... -_-)
    io.output.done := io.input.enable & (isMax | isMin)
    io.output.saturated := io.setup.saturate & ( isMax | isMin )
  } else { // Forever
    io.output.saturated := false.B
    io.output.done := false.B
  }
}

// SingleSCounter that is cheaper, if bounds and stride are known
class SingleSCounterCheap(val par: Int, val start: Int, val stop: Int, val strideUp: Int, val strideDown: Int,
                          val width: Int = 32) extends Module { // Signed counter, used in FILO

  val io = IO(new Bundle {
    val setup = new Bundle {
      val saturate = Input(Bool())
    }
    val input = new Bundle {
      val dir = Input(Bool())
      // val wrap     = BoolInput(()) // TODO: This should let
      //                                   user specify (8 by 3) ctr to go
      //                                   0,3,6 (wrap) 1,4,7 (wrap) 2,5...
      //                                   instead of default
      //                                   0,3,6 (wrap) 0,3,6 (wrap) 0,3...
      val reset  = Input(Bool())
      val enable = Input(Bool())
    }
    val output = new Bundle {
      val count      = Vec(par, Output(SInt(width.W)))
      val done   = Output(Bool())
      val saturated = Output(Bool())
    }
  })

  if (par > 0) {
    val base = Module(new FF(width))
    base.io <> DontCare
    val init = start.asSInt
    base.io.wPort(0).init := init.asUInt
    base.io.wPort(0).reset := io.input.reset
    base.io.wPort(0).en.head := io.input.enable

    val count = base.io.rPort(0).output(0).asSInt
    val newval_up = count + ((strideUp * par).S(width.W))
    val newval_down = count + ((strideDown * par).S(width.W))
    val isMax = newval_up >= stop.asSInt
    val wasMax = RegNext(isMax, false.B)
    val isMin = newval_down < 0.S((width).W)
    val wasMin = RegNext(isMin, false.B)
    val wasEnabled = RegNext(io.input.enable, false.B)
    // TODO: stop + strideDown in line below.. correct?
    val next = Mux(isMax & io.input.dir, Mux(io.setup.saturate, count, init), Mux(isMin & !io.input.dir & !io.input.reset , (stop + strideDown).asSInt, Mux(io.input.dir, newval_up, newval_down)))
    base.io.wPort(0).data.head := Mux(io.input.reset, init.asUInt, next.asUInt)

    (0 until par).foreach { i => io.output.count(i) := Mux(io.input.dir, count + (i*strideUp).S(width.W), count + (i*strideDown).S(width.W)) }
    io.output.done := io.input.enable & ((isMax & io.input.dir) | (isMin & !io.input.dir))
    io.output.saturated := io.setup.saturate & ( (isMax & io.input.dir) | (isMin & !io.input.dir) )
  } else { // Forever
    io.output.saturated := false.B
    io.output.done := false.B
  }
}


/*
     outermost    middle   innermost
      |     |    |     |    |     |
      |     |    |     |    |     |
      |_____|    |_____|    |_____|
      _| | |_    __| |    _____|
     |   |   |  |    |   |
count(0) 1   2  3    4   5

*/

class CounterChainInterface(val par: List[Int], val widths: List[Int]) extends Bundle {
  def this(tup: (List[Int], List[Int])) = this(tup._1, tup._2)
  val setup = new Bundle {
    val starts   = HVec.tabulate(par.length){i => Input(SInt(widths(i).W))}
    val stops    = HVec.tabulate(par.length){i => Input(SInt(widths(i).W))}
    val strides  = HVec.tabulate(par.length){i => Input(SInt(widths(i).W))}
    val saturate = Input(Bool())
    val isStream = Input(Bool()) // If a stream counter, do not need enable on to report done
  }
  val input = new Bundle {
    val reset    = Input(Bool())
    val enable   = Input(Bool())
  }
  val output = Output(new CChainOutput(par, widths))

  override def cloneType = (new CounterChainInterface(par, widths)).asInstanceOf[this.type] // See chisel3 bug 358
}

/**
  * Counter: n-depth counter. Counts up to each stop. Lists go from
            outermost (slowest) to innermost (fastest) counter.
  * @param w: Word width
  */
class CounterChain(val par: List[Int], val starts: List[Option[Int]], val stops: List[Option[Int]],
              val strides: List[Option[Int]], val forevers: List[Boolean], val widths: List[Int], val myName: String = "CChain") extends Module {
  def this(par: List[Int], sts: List[Option[Int]], stps: List[Option[Int]], strs: List[Option[Int]], frvrs: List[Boolean]) = this(par, sts, stps, strs, frvrs, List.fill(par.length){32})
  def this(tuple: (List[Int], List[Option[Int]], List[Option[Int]], List[Option[Int]], List[Boolean], List[Int])) = this(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6)

  override def desiredName = myName
  
  val depth = par.length
  val numWires = par.reduce{_+_}
  val ctrMapping = par.indices.map{i => par.dropRight(par.length - i).sum}

  val io = IO(new CounterChainInterface(par, widths))

  // Create counters
  val ctrs = (0 until depth).map{ i => Module(new SingleCounter(par(i), starts(i), stops(i), strides(i), forevers(i), widths(i))) }

  // Wire up the easy inputs from IO
  ctrs.zipWithIndex.foreach { case (ctr, i) =>
    ctr.io.setup.start := io.setup.starts(i)
    ctr.io.setup.stop := io.setup.stops(i)
    ctr.io.setup.stride := io.setup.strides(i)
    ctr.io.input.reset := io.input.reset
  }

  // Wire up the enables between ctrs
  ctrs(depth-1).io.input.enable := io.input.enable
  (0 until depth-1).foreach { i =>
    ctrs(i).io.input.enable := ctrs(i+1).io.output.done & io.input.enable
  }

  // Wire up the saturates between ctrs
  ctrs(0).io.setup.saturate := io.setup.saturate
  (1 until depth).foreach { i =>
    ctrs(i).io.setup.saturate := io.setup.saturate & ctrs.take(i).map{ ctr => ctr.io.output.saturated }.reduce{_&_}
  }

  // // Wire up countBases for easy debugging
  // ctrs.zipWithIndex.map { case (ctr,i) =>
  //   io.output.countBases(i) := ctr.io.output.count(0)
  // }

  // Wire up the done, saturated, and extendedDone signals
  val isDone = ctrs.map{_.io.output.done}.reduce{_&_}
  val wasDone = RegNext(isDone, false.B)
  val isSaturated = ctrs.map{_.io.output.saturated}.reduce{_&_}
  val wasWasDone = RegNext(wasDone, false.B)
  io.output.noop := ctrs.map(_.io.output.noop).reduce{_&&_}
  io.output.done := Mux(io.setup.isStream, true.B, io.input.enable) & isDone & !wasDone
  io.output.saturated := io.setup.saturate & isSaturated

  // Done latch
  val doneLatch = RegInit(false.B)
  doneLatch := Mux(io.input.reset, false.B, Mux(isDone, true.B, doneLatch))
    
  // Wire up the outputs
  par.zipWithIndex.foreach { case (p, i) =>
    val addr = par.take(i+1).sum - par(i) // i+1 to avoid reducing empty list
    (0 until {1 max p}).foreach { k =>
      io.output.counts(addr+k) := ctrs(i).io.output.count(k)
      io.output.oobs(addr+k) := ctrs(i).io.output.oobs(k) | doneLatch
    }
  }

}

