// See LICENSE.txt for license details.
package templates

import util._
import chisel3._
import chisel3.util._
import ops._


/**
 * NBufCtr: 1-dimensional counter. Basically a cheap, wrapping counter because  
             chisel is retarted and optimizes away a Vec(1) to a single val,
             but still forces you to index the thing and hence only gets the
             first bit
 */
class NBufCtr(val stride: Int = 1, val start: Option[Int], val stop: Option[Int], val init: Int = 0,
             val width: Int = 32) extends Module {
  val io = IO(new Bundle {
    val input = new Bundle {
      val start = Input(UInt(width.W))
      val stop = Input(UInt(width.W))
      val countUp  = Input(Bool())
      val enable = Input(Bool())
    }
    val output = new Bundle {
      val count      = Output(UInt(width.W))
    }
  })

  if (start.isDefined && stop.isDefined) {
    val cnt = Wire(UInt(width.W))

    val effectiveCnt = Mux(cnt + start.get.U(width.W) >= stop.get.U(width.W), (cnt.asSInt + (start.get-stop.get).S(width.W)).asUInt, cnt + start.get.U(width.W))

    val nextCntDown = Mux(io.input.enable, Mux(cnt === 0.U(width.W), (stop.get-stride).U(width.W), cnt-stride.U(width.W)), cnt) // TODO: This could be an issue if strided counter is used in reverse
    val nextCntUp = Mux(io.input.enable, Mux(cnt + stride.U(width.W) >= stop.get.U(width.W), 0.U(width.W) + (cnt.asSInt + (stride - stop.get).S(width.W)).asUInt, cnt+stride.U(width.W)), cnt)
    cnt := Utils.getRetimed(Mux(reset.toBool, init.U, Mux(io.input.countUp, nextCntUp, nextCntDown)), 1, init = init.toLong)

    io.output.count := effectiveCnt    
  } else if (stop.isDefined) {
    val cnt = Wire(UInt(width.W))

    val effectiveCnt = Mux(cnt + io.input.start >= stop.get.U(width.W), cnt + io.input.start - stop.get.U(width.W), cnt + io.input.start)

    val nextCntDown = Mux(io.input.enable, Mux(cnt === 0.U(width.W), (stop.get-stride).U(width.W), cnt-stride.U(width.W)), cnt) // TODO: This could be an issue if strided counter is used in reverse
    val nextCntUp = Mux(io.input.enable, Mux(cnt + stride.U(width.W) >= stop.get.U(width.W), 0.U(width.W) + cnt+stride.U(width.W) - stop.get.U(width.W), cnt+stride.U(width.W)), cnt)
    cnt := Utils.getRetimed(Mux(reset.toBool, init.U(width.W), Mux(io.input.countUp, nextCntUp, nextCntDown)), 1, init = init.toLong)

    io.output.count := effectiveCnt
  } else {
    val cnt = Wire(UInt(width.W))

    val effectiveCnt = Mux(cnt + io.input.start >= io.input.stop, cnt + io.input.start - io.input.stop, cnt + io.input.start)

    val nextCntDown = Mux(io.input.enable, Mux(cnt === 0.U(width.W), io.input.stop-stride.U(width.W), cnt-stride.U(width.W)), cnt) // TODO: This could be an issue if strided counter is used in reverse
    val nextCntUp = Mux(io.input.enable, Mux(cnt + stride.U(width.W) >= io.input.stop, 0.U(width.W) + cnt+stride.U(width.W) - io.input.stop, cnt+stride.U(width.W)), cnt)
    cnt := Utils.getRetimed(Mux(reset.toBool, init.U(width.W), Mux(io.input.countUp, nextCntUp, nextCntDown)), 1, init = init.toLong)

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
      val overread      = Output(Bool())
      val overwrite      = Output(Bool())
      val empty         = Output(Bool())
      val full          = Output(Bool())
      val almostEmpty         = Output(Bool())
      val almostFull          = Output(Bool())
      val numel         = Output(SInt((width+1).W))
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
class IICounter(val ii: Int, val width: Int = 32) extends Module {
  val io = IO(new Bundle {
    val input = new Bundle {
      val enable = Input(Bool())
      val reset = Input(Bool())
    }
    val output = new Bundle {
      val done      = Output(Bool())
    }
  })

  val cnt = RegInit((ii-1).S(width.W))
  val isDone = (cnt === (ii-1).S(width.W)) & io.input.enable

  val nextLive = Mux(cnt === 0.S(width.W), (ii-1).S(width.W), cnt-1.S(width.W))
  val next = Mux(io.input.enable, nextLive, cnt)
  cnt := Mux(io.input.reset, (ii-1).S(width.W), next)

  io.output.done := isDone
}

class CompactingIncDincCtr(inc: Int, dinc: Int, stop: Int, width: Int = 32) extends Module {
  val io = IO(new Bundle {
    val input = new Bundle {
      val inc_en     = Vec(inc, Input(Bool()))
      val dinc_en    = Vec(dinc, Input(Bool()))
    }
    val output = new Bundle {
      val overread      = Output(Bool())
      val overwrite      = Output(Bool())
      val empty         = Output(Bool())
      val full          = Output(Bool())
      val almostEmpty         = Output(Bool())
      val almostFull          = Output(Bool())
      val numel         = Output(SInt((width+1).W))
    }
  })

  val cnt = RegInit(0.S(32.W))

  val numPushed = io.input.inc_en.map{e => Mux(e, 1.S(width.W), 0.S(width.W))}.reduce{_+_}
  val numPopped = io.input.dinc_en.map{e => Mux(e, 1.S(width.W), 0.S(width.W))}.reduce{_+_}
  cnt := cnt + numPushed - numPopped

  io.output.overread := cnt < 0.S((width+1).W)
  io.output.overwrite := cnt > stop.S((width+1).W)
  io.output.empty := cnt === 0.S((width+1).W)
  io.output.almostEmpty := cnt - dinc.S((width+1).W) === 0.S((width+1).W)
  io.output.full := cnt === stop.S((width+1).W)
  io.output.almostFull := cnt + inc.S((width+1).W) === stop.S((width+1).W)
  io.output.numel := cnt
}

class CompactingCounter(val lanes: Int, val depth: Int, val width: Int) extends Module {
  def this(tuple: (Int, Int, Int)) = this(tuple._1, tuple._2, tuple._3)
  val io = IO(new Bundle {
    val input = new Bundle {
      val dir = Input(Bool())
      val reset  = Input(Bool())
      val enables = Vec(lanes, Input(Bool()))
    }
    val output = new Bundle {
      val done   = Output(Bool())
      val count  = Output(SInt(width.W))
    }
  })

  val base = Module(new FF((width)))
  base.io.xBarW(0).init.head := 0.U(width.W)
  base.io.xBarW(0).reset.head := io.input.reset
  base.io.xBarW(0).en.head := io.input.enables.reduce{_|_}

  val count = base.io.output.data(0).asSInt
  val num_enabled = io.input.enables.map{e => Mux(e, 1.S(width.W), 0.S(width.W))}.reduce{_+_}
  val newval = count + Mux(io.input.dir, num_enabled, -num_enabled)
  val isMax = Mux(io.input.dir, newval >= depth.S, newval <= 0.S)
  val next = Mux(isMax, newval - depth.S(width.W), newval)
  base.io.xBarW(0).data.head := Mux(io.input.reset, 0.asUInt, next.asUInt)

  io.output.count := base.io.output.data(0).asSInt
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
                    val stride: Option[Int], val gap: Option[Int], val width: Int = 32) extends Module {
  def this(tuple: (Int, Option[Int], Option[Int], Option[Int], Option[Int], Int)) = this(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6)

  val io = IO(new Bundle {
    val input = new Bundle {
      val start    = Input(SInt((width).W)) // TODO: Currently resets to "start" but wraps to 0, is this normal behavior?
      val stop      = Input(SInt((width).W))
      val stride   = Input(SInt((width).W))
      val gap      = Input(SInt((width).W))
      // val wrap     = BoolInput(()) // TODO: This should let 
      //                                   user specify (8 by 3) ctr to go
      //                                   0,3,6 (wrap) 1,4,7 (wrap) 2,5...
      //                                   instead of default
      //                                   0,3,6 (wrap) 0,3,6 (wrap) 0,3...
      val reset  = Input(Bool())
      val enable = Input(Bool())
      val saturate = Input(Bool())
    }
    val output = new Bundle {
      val count      = Vec(1 max par, Output(SInt((width).W)))
      val oobs        = Vec(1 max par, Output(Bool()))
      val noop   = Output(Bool())
      val done   = Output(Bool())
      val saturated = Output(Bool())
    }
  })

  if (par > 0) {
    val lock = Module(new SRFF())
    lock.io.input.set := io.input.enable  & ~io.input.reset
    lock.io.input.reset := io.input.reset || io.output.done
    lock.io.input.asyn_reset := false.B
    val locked = lock.io.output.data// | io.input.enable
    val bases = List.tabulate(par){i => Module(new FF((width)))}
    val inits = List.tabulate(par){i => 
      Utils.getRetimed(
        if (start.isDefined & stride.isDefined) {(start.get + i*stride.get).S(width.W)} 
        else if (start.isDefined) {start.get.S(width.W) + i.S(width.W) * io.input.stride}
        else if (stride.isDefined) {io.input.start + (i*stride.get).S(width.W)}
        else {io.input.start + i.S(width.W) * io.input.stride}
        ,0) // Maybe delay by 1 cycle just in case this is a critical path.  Init should never change during execution
    }
    bases.zipWithIndex.foreach{ case (b,i) => 
      b.io.xBarW(0).init.head := inits(i).asUInt
      b.io.xBarW(0).reset.head := io.input.reset | 
                                  {if (stride.isDefined) false.B else {Utils.getRetimed(io.input.stride,1) =/= io.input.stride}} | 
                                  {if (start.isDefined) false.B else {!(locked | io.input.enable) && (Utils.getRetimed(io.input.start,1) =/= io.input.start)}}
      b.io.xBarW(0).en.head := io.input.enable
    }

    val counts = bases.map(_.io.output.data(0).asSInt)
    val delta = if (stride.isDefined & gap.isDefined) { (stride.get*par+gap.get).S((width.W)) }
                else if (stride.isDefined) { (stride.get*par).S((width.W)) + io.input.gap}
                else if (gap.isDefined) { io.input.stride * par.S((width.W)) + gap.get.S(width.W)}
                else { io.input.stride * par.S((width.W)) + io.input.gap}
    val newvals = counts.map( _ + delta)
    val isMax = Mux(io.input.stride >= 0.S((width).W), 
      newvals(0) >= {if (stop.isDefined) stop.get.S(width.W) else io.input.stop}, 
      newvals(0) <= {if (stop.isDefined) stop.get.S(width.W) else io.input.stop}
    )
    val wasMax = RegNext(isMax, false.B)
    val wasEnabled = RegNext(io.input.enable, false.B)
    bases.zipWithIndex.foreach {case (b,i) => 
      b.io.xBarW(0).data.head := Mux(io.input.reset, inits(i).asUInt, Mux(isMax, Mux(io.input.saturate, counts(i).asUInt, inits(i).asUInt), newvals(i).asUInt))
    }

    if(stride.isDefined) {
      (0 until par).foreach { i => 
        io.output.count(i) := counts(i)
      }
    } else {
      (0 until par).foreach { i => 
        io.output.count(i) := counts(i)
      }      
    }

    // Connect oobies (Out Of Bound-ies)
    val defs = {if (start.isDefined) 0x4 else 0x0} | {if (stop.isDefined) 0x2 else 0x0} | {if (stride.isDefined) 0x1 else 0x0}
    (0 until par).foreach{ i => 
      // Changing start and resetting counts(i) takes a cycle, so forward the init value if required
      val c = defs match {
        case 0x7 | 0x6 | 0x5 | 0x4 => counts(i)
        case 0x3 | 0x2 | 0x1 | 0x0 => Mux(!locked && (Utils.getRetimed(io.input.start,1) =/= io.input.start), inits(i), counts(i))
      }
      // Connections are a mouthful but it is historically unsafe to trust that chisel will optimize constants properly
      defs match {
        case 0x7 => if (stride.get >= 0) io.output.oobs(i) :=                                      c < start.get.S(width.W) || c >= stop.get.S(width.W) else io.output.oobs(i) := c > start.get.S(width.W) || c <= stop.get.S(width.W)   
        case 0x6 =>                      io.output.oobs(i) := Mux(io.input.stride >= 0.S(width.W), c < start.get.S(width.W) || c >= stop.get.S(width.W),                          c > start.get.S(width.W) || c <= stop.get.S(width.W))
        case 0x5 => if (stride.get >= 0) io.output.oobs(i) :=                                      c < start.get.S(width.W) || c >= io.input.stop       else io.output.oobs(i) := c > start.get.S(width.W) || c <= io.input.stop   
        case 0x4 =>                      io.output.oobs(i) := Mux(io.input.stride >= 0.S(width.W), c < start.get.S(width.W) || c >= io.input.stop,                                c > start.get.S(width.W) || c <= io.input.stop)
        case 0x3 => if (stride.get >= 0) io.output.oobs(i) :=                                      c < io.input.start       || c >= stop.get.S(width.W) else io.output.oobs(i) := c > io.input.start       || c <= stop.get.S(width.W)
        case 0x2 =>                      io.output.oobs(i) := Mux(io.input.stride >= 0.S(width.W), c < io.input.start       || c >= stop.get.S(width.W),                          c > io.input.start       || c <= stop.get.S(width.W))
        case 0x1 => if (stride.get >= 0) io.output.oobs(i) :=                                      c < io.input.start       || c >= io.input.stop       else io.output.oobs(i) := c > io.input.start       || c <= io.input.stop
        case 0x0 =>                      io.output.oobs(i) := Mux(io.input.stride >= 0.S(width.W), c < io.input.start       || c >= io.input.stop,                                c > io.input.start       || c <= io.input.stop)
      }
    }

    io.output.done := io.input.enable & isMax
    defs match {
      case 0x7 | 0x6 => io.output.noop := (start.get            == stop.get).B
      case 0x5 | 0x4 => io.output.noop := start.get.S(width.W) === io.input.stop
      case 0x3 | 0x2 => io.output.noop := io.input.start       === stop.get.S(width.W)
      case 0x1 | 0x0 => io.output.noop := io.input.start       === io.input.stop
    }
    io.output.saturated := io.input.saturate & isMax
  } else { // Forever counter
    io.output.count(0) := 0.S(width.W)
    io.output.saturated := false.B
    io.output.noop := false.B
    io.output.done := false.B

  }
}

class SingleSCounter(val par: Int, val width: Int = 32) extends Module { // Signed counter, used in FILO
  def this(tuple: (Int, Int)) = this(tuple._1, tuple._2)

  val io = IO(new Bundle {
    val input = new Bundle {
      val start    = Input(SInt((width).W)) // TODO: Currently resets to "start" but wraps to 0, is this normal behavior?
      val stop      = Input(SInt((width).W))
      val stride   = Input(SInt((width).W))
      val gap      = Input(SInt((width).W))
      // val wrap     = BoolInput(()) // TODO: This should let 
      //                                   user specify (8 by 3) ctr to go
      //                                   0,3,6 (wrap) 1,4,7 (wrap) 2,5...
      //                                   instead of default
      //                                   0,3,6 (wrap) 0,3,6 (wrap) 0,3...
      val reset  = Input(Bool())
      val enable = Input(Bool())
      val saturate = Input(Bool())
    }
    val output = new Bundle { 
      val count      = Vec(par, Output(SInt((width).W)))
      val done   = Output(Bool())
      val saturated = Output(Bool())
    }
  })

  if (par > 0) {
    val base = Module(new FF((width)))
    val init = io.input.start
    base.io.xBarW(0).init.head := init.asUInt
    base.io.xBarW(0).reset.head := io.input.reset
    base.io.xBarW(0).en.head := io.input.reset | io.input.enable

    val count = base.io.output.data(0).asSInt
    val newval = count + (io.input.stride * par.S((width).W)) + io.input.gap // TODO: If I use *-* here, BigIPSim doesn't see par.S as a constant (but it sees par.U as one... -_-)
    val isMax = newval >= io.input.stop
    val wasMax = RegNext(isMax, false.B)
    val isMin = newval < 0.S((width).W)
    val wasMin = RegNext(isMin, false.B)
    val wasEnabled = RegNext(io.input.enable, false.B)
    val next = Mux(isMax, Mux(io.input.saturate, count, init), Mux(isMin, io.input.stop + io.input.stride, newval))
    base.io.xBarW(0).data.head := Mux(io.input.reset, init.asUInt, next.asUInt)

    (0 until par).foreach { i => io.output.count(i) := count + i.S((width).W)*io.input.stride } // TODO: If I use *-* here, BigIPSim doesn't see par.S as a constant (but it sees par.U as one... -_-)
    io.output.done := io.input.enable & (isMax | isMin)
    io.output.saturated := io.input.saturate & ( isMax | isMin )
  } else { // Forever
    io.output.saturated := false.B
    io.output.done := false.B
  }
}

// SingleSCounter that is cheaper, if bounds and stride are known
class SingleSCounterCheap(val par: Int, val start: Int, val stop: Int, val strideUp: Int, val strideDown: Int, 
            val gap: Int, val width: Int = 32) extends Module { // Signed counter, used in FILO

  val io = IO(new Bundle {
    val input = new Bundle {
      val dir = Input(Bool())
      // val wrap     = BoolInput(()) // TODO: This should let 
      //                                   user specify (8 by 3) ctr to go
      //                                   0,3,6 (wrap) 1,4,7 (wrap) 2,5...
      //                                   instead of default
      //                                   0,3,6 (wrap) 0,3,6 (wrap) 0,3...
      val reset  = Input(Bool())
      val enable = Input(Bool())
      val saturate = Input(Bool())
    }
    val output = new Bundle { 
      val count      = Vec(par, Output(SInt((width).W)))
      val done   = Output(Bool())
      val saturated = Output(Bool())
    }
  })

  if (par > 0) {
    val base = Module(new FF((width)))
    val init = start.asSInt
    base.io.xBarW(0).init.head := init.asUInt
    base.io.xBarW(0).reset.head := io.input.reset
    base.io.xBarW(0).en.head := io.input.enable

    val count = base.io.output.data(0).asSInt
    val newval_up = count + ((strideUp * par + gap).S((width).W))
    val newval_down = count + ((strideDown * par + gap).S((width).W))
    val isMax = newval_up >= stop.asSInt
    val wasMax = RegNext(isMax, false.B)
    val isMin = newval_down < 0.S((width).W)
    val wasMin = RegNext(isMin, false.B)
    val wasEnabled = RegNext(io.input.enable, false.B)
    // TODO: stop + strideDown in line below.. correct?
    val next = Mux(isMax & io.input.dir, Mux(io.input.saturate, count, init), Mux(isMin & ~io.input.dir & ~io.input.reset , (stop + strideDown).asSInt, Mux(io.input.dir, newval_up, newval_down)))
    base.io.xBarW(0).data.head := Mux(io.input.reset, init.asUInt, next.asUInt)

    (0 until par).foreach { i => io.output.count(i) := Mux(io.input.dir, count + (i*strideUp).S((width).W), count + (i*strideDown).S((width).W)) }
    io.output.done := io.input.enable & ((isMax & io.input.dir) | (isMin & ~io.input.dir))
    io.output.saturated := io.input.saturate & ( (isMax & io.input.dir) | (isMin & ~io.input.dir) )
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

/**
 * Counter: n-depth counter. Counts up to each stop. Lists go from
            outermost (slowest) to innermost (fastest) counter.
 * @param w: Word width
 */
class Counter(val par: List[Int], val starts: List[Option[Int]], val stops: List[Option[Int]], 
              val strides: List[Option[Int]], val gaps: List[Option[Int]], val widths: List[Int]) extends Module {
  def this(par: List[Int], sts: List[Option[Int]], stps: List[Option[Int]], strs: List[Option[Int]], gps: List[Option[Int]]) = this(par, sts, stps, strs, gps, List.fill(par.length){32})
  def this(tuple: (List[Int], List[Option[Int]], List[Option[Int]], List[Option[Int]], List[Option[Int]], List[Int])) = this(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6)

  val depth = par.length
  val numWires = par.reduce{_+_}
  val ctrMapping = par.indices.map{i => par.dropRight(par.length - i).sum}

  val io = IO(new Bundle {
    val input = new Bundle {
      val starts    = HVec.tabulate(depth){i => Input(SInt((widths(i)).W))}
      val stops      = HVec.tabulate(depth){i => Input(SInt((widths(i)).W))}
      val strides   = HVec.tabulate(depth){i => Input(SInt((widths(i)).W))}
      val gaps      = HVec.tabulate(depth){i => Input(SInt((widths(i)).W))}
      val reset  = Input(Bool())
      val enable = Input(Bool())
      val saturate = Input(Bool())
      val isStream = Input(Bool()) // If a stream counter, do not need enable on to report done
    }
    val output = new Bundle {
      val counts      = HVec.tabulate(numWires){i => Output(SInt((widths(ctrMapping.filter(_ <= i).length - 1)).W))}
      val oobs        = Vec(numWires, Output(Bool()))
      val noop   = Output(Bool())
      val done   = Output(Bool())
      val saturated = Output(Bool())
    }
  })

  // Create counters
  val ctrs = (0 until depth).map{ i => Module(new SingleCounter(par(i), starts(i), stops(i), strides(i), gaps(i), widths(i))) }

  // Wire up the easy inputs from IO
  ctrs.zipWithIndex.foreach { case (ctr, i) =>
    ctr.io.input.start := io.input.starts(i)
    ctr.io.input.stop := io.input.stops(i)
    ctr.io.input.stride := io.input.strides(i)
    ctr.io.input.gap := io.input.gaps(i)
    ctr.io.input.reset := io.input.reset
    ctr.io.input.gap := 0.S
  }

  // Wire up the enables between ctrs
  ctrs(depth-1).io.input.enable := io.input.enable
  (0 until depth-1).foreach { i =>
    ctrs(i).io.input.enable := ctrs(i+1).io.output.done & io.input.enable
  }

  // Wire up the saturates between ctrs
  ctrs(0).io.input.saturate := io.input.saturate
  (1 until depth).foreach { i =>
    ctrs(i).io.input.saturate := io.input.saturate & ctrs.take(i).map{ ctr => ctr.io.output.saturated }.reduce{_&_}
  }

  // Wire up the outputs
  par.zipWithIndex.foreach { case (p, i) => 
    val addr = par.take(i+1).reduce{_+_} - par(i) // i+1 to avoid reducing empty list
    (0 until p).foreach { k => 
      io.output.counts(addr+k) := ctrs(i).io.output.count(k) 
      io.output.oobs(addr+k) := ctrs(i).io.output.oobs(k)
    }
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
  io.output.done := Mux(io.input.isStream, true.B, io.input.enable) & isDone & ~wasDone
  io.output.saturated := io.input.saturate & isSaturated


}


