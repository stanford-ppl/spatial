package templates

import ops._
import chisel3._
import types._
import scala.math.log
import chisel3.util.Mux1H

sealed abstract class Accum
object Accum {
  case object Add extends Accum
  case object Mul extends Accum
  case object Min extends Accum
  case object Max extends Accum
}


class FixFMAAccum(val numWriters: Int, val cycleLatency: Double, val fmaLatency: Double, val s: Boolean, val d: Int, val f: Int, init: Double) extends Module {
  def this(tup: (Int, Double, Double, Boolean, Int, Int, Double)) = this(tup._1, tup._2, tup._3, tup._4, tup._5, tup._6, tup._7)

  val cw = Utils.log2Up(cycleLatency) + 2
  val initBits = (init*scala.math.pow(2,f)).toLong.S((d+f).W).asUInt
  val io = IO(new Bundle{
    val input1 = Vec(numWriters, Input(UInt((d+f).W)))
    val input2 = Vec(numWriters, Input(UInt((d+f).W)))
    val enable = Vec(numWriters, Input(Bool()))
    val reset = Vec(numWriters, Input(Bool()))
    val last = Vec(numWriters, Input(Bool()))
    val first = Vec(numWriters, Input(Bool()))
    val output = Output(UInt((d+f).W))
  })

  val activeIn1 = Mux1H(io.enable, io.input1)
  val activeIn2 = Mux1H(io.enable, io.input2)
  val activeEn  = io.enable.reduce{_|_}
  val activeLast = Mux1H(io.enable, io.last)
  val activeReset = io.reset.reduce{_|_}
  val activeFirst = io.first.reduce{_|_}

  val fixin1 = Wire(new FixedPoint(s,d,f))
  fixin1.r := activeIn1
  val fixin2 = Wire(new FixedPoint(s,d,f))
  fixin2.r := activeIn2

  // Use log2Down to be consistent with latency model that truncates
  val drain_latency = (log(cycleLatency)/log(2)).toInt

  val laneCtr = Module(new SingleCounter(1, Some(0), Some(cycleLatency.toInt), Some(1), Some(0), cw))
  laneCtr.io.input.enable := activeEn
  laneCtr.io.input.reset := activeReset | activeLast.D(drain_latency + fmaLatency)
  laneCtr.io.input.saturate := false.B

  val firstRound = Module(new SRFF())
  firstRound.io.input.set := activeFirst & ~laneCtr.io.output.done
  firstRound.io.input.asyn_reset := false.B
  firstRound.io.input.reset := laneCtr.io.output.done | activeReset
  val isFirstRound = firstRound.io.output.data

  val drainState = Module(new SRFF())
  drainState.io.input.set := activeLast
  drainState.io.input.asyn_reset := false.B
  drainState.io.input.reset := activeLast.D(drain_latency + fmaLatency)
  val isDrainState = drainState.io.output.data

  val dispatchLane = laneCtr.io.output.count(0).asUInt
  val accums = Array.tabulate(cycleLatency.toInt){i => (Module(new FF(d+f)), i.U(cw.W))}
  accums.foreach{case (acc, lane) => 
    val fixadd = Wire(new FixedPoint(s,d,f))
    fixadd.r := Mux(isFirstRound, 0.U, acc.io.output.data(0))
    val result = Wire(new FixedPoint(s,d,f))
    Utils.FixFMA(fixin1, fixin2, fixadd, fmaLatency.toInt, true.B).cast(result)
    acc.io.xBarW(0).data := result.r
    acc.io.xBarW(0).en := Utils.getRetimed(activeEn & dispatchLane === lane, fmaLatency.toInt)
    acc.io.xBarW(0).reset := activeReset | activeLast.D(drain_latency + fmaLatency)
    acc.io.xBarW(0).init := initBits
  }

  io.output := Utils.getRetimed(accums.map(_._1.io.output.data(0)).reduce{_+_}, drain_latency, isDrainState).r // TODO: Please build tree and retime appropriately

  def connectXBarRPort(rBundle: R_XBar, bufferPort: Int, muxAddr: (Int, Int), vecId: Int): UInt = {connectXBarRPort(rBundle, bufferPort, muxAddr, vecId, true.B)}
  def connectXBarRPort(rBundle: R_XBar, bufferPort: Int, muxAddr: (Int, Int), vecId: Int, flow: Bool): UInt = {io.output}

}

class FixOpAccum(val t: Accum, val numWriters: Int, val cycleLatency: Double, val opLatency: Double, val s: Boolean, val d: Int, val f: Int, init: Double) extends Module {
  def this(tup: (Accum, Int, Double, Double, Boolean, Int, Int, Double)) = this(tup._1, tup._2, tup._3, tup._4, tup._5, tup._6, tup._7, tup._8)

  val cw = Utils.log2Up(cycleLatency) + 2
  val initBits = (init*scala.math.pow(2,f)).toLong.S((d+f).W).asUInt
  val io = IO(new Bundle{
    val input1 = Vec(numWriters, Input(UInt((d+f).W)))
    val enable = Vec(numWriters, Input(Bool()))
    val reset = Vec(numWriters, Input(Bool()))
    val last = Vec(numWriters, Input(Bool()))
    val first = Vec(numWriters, Input(Bool()))
    val output = Output(UInt((d+f).W))
  })

  val activeIn1 = Mux1H(io.enable, io.input1)
  val activeEn  = io.enable.reduce{_|_}
  val activeLast = Mux1H(io.enable, io.last)
  val activeReset = io.reset.reduce{_|_}
  val activeFirst = io.first.reduce{_|_}

  val fixin1 = Wire(new FixedPoint(s,d,f))
  fixin1.r := activeIn1

  // Use log2Down to be consistent with latency model that truncates
  val drain_latency = ((log(cycleLatency)/log(2)).toInt * opLatency).toInt

  val laneCtr = Module(new SingleCounter(1, Some(0), Some(cycleLatency.toInt), Some(1), Some(0), cw))
  laneCtr.io.input.enable := activeEn
  laneCtr.io.input.reset := activeReset | activeLast.D(drain_latency + opLatency)
  laneCtr.io.input.saturate := false.B

  val firstRound = Module(new SRFF())
  firstRound.io.input.set := activeFirst & ~laneCtr.io.output.done
  firstRound.io.input.asyn_reset := false.B
  firstRound.io.input.reset := laneCtr.io.output.done | activeReset
  val isFirstRound = firstRound.io.output.data

  val drainState = Module(new SRFF())
  drainState.io.input.set := activeLast
  drainState.io.input.asyn_reset := false.B
  drainState.io.input.reset := activeLast.D(drain_latency + opLatency)
  val isDrainState = drainState.io.output.data

  val dispatchLane = laneCtr.io.output.count(0).asUInt
  val accums = Array.tabulate(cycleLatency.toInt){i => (Module(new FF(d+f)), i.U(cw.W))}
  accums.foreach{case (acc, lane) => 
    val fixadd = Wire(new FixedPoint(s,d,f))
    fixadd.r := acc.io.output.data(0)
    val result = Wire(new FixedPoint(s,d,f))
    t match {
      case Accum.Add => result.r := Mux(isFirstRound.D(opLatency.toInt), Utils.getRetimed(fixin1.r, opLatency.toInt), (Utils.getRetimed(fixin1 + fixadd, opLatency.toInt)).r)
      case Accum.Mul => result.r := Mux(isFirstRound.D(opLatency.toInt), Utils.getRetimed(fixin1.r, opLatency.toInt), (fixin1.*-*(fixadd, Some(opLatency), true.B)).r)
      case Accum.Min => result.r := Mux(isFirstRound.D(opLatency.toInt), Utils.getRetimed(fixin1.r, opLatency.toInt), Utils.getRetimed(Mux(fixin1 < fixadd, fixin1, fixadd).r, opLatency.toInt))
      case Accum.Max => result.r := Mux(isFirstRound.D(opLatency.toInt), Utils.getRetimed(fixin1.r, opLatency.toInt), Utils.getRetimed(Mux(fixin1 > fixadd, fixin1, fixadd).r, opLatency.toInt))

    }
    acc.io.xBarW(0).data := result.r
    acc.io.xBarW(0).en := Utils.getRetimed(activeEn & dispatchLane === lane, opLatency.toInt)
    acc.io.xBarW(0).reset := activeReset | activeLast.D(drain_latency + opLatency)
    acc.io.xBarW(0).init := initBits
  }

    t match {
      case Accum.Add => io.output := Utils.getRetimed(accums.map(_._1.io.output.data(0)).reduce[UInt]{case (a:UInt,b:UInt) => 
        val t1 = Wire(new FixedPoint(s,d,f))
        val t2 = Wire(new FixedPoint(s,d,f))
        t1.r := a
        t2.r := b
        (t1+t2).r
        // Utils.getRetimed(t1 + t2, opLatency.toInt).r
      }, drain_latency, isDrainState)
      case Accum.Mul => io.output := Utils.getRetimed(accums.map(_._1.io.output.data(0)).foldRight[UInt](1.FP(s,d,f).r){case (a:UInt,b:UInt) => 
        val t1 = Wire(new FixedPoint(s,d,f))
        val t2 = Wire(new FixedPoint(s,d,f))
        t1.r := a
        t2.r := b
        (t1.*-*(t2, None, true.B)).r
      }, drain_latency, isDrainState)
      case Accum.Min => io.output := Utils.getRetimed(accums.map(_._1.io.output.data(0)).reduce[UInt]{case (a:UInt,b:UInt) => 
        val t1 = Wire(new FixedPoint(s,d,f))
        val t2 = Wire(new FixedPoint(s,d,f))
        t1.r := a
        t2.r := b
        Mux(t1 < t2, t1.r, t2.r)
        // Utils.getRetimed(Mux(t1 < t2, t1.r,t2.r), opLatency.toInt)
      }, drain_latency, isDrainState)
      case Accum.Max => io.output := Utils.getRetimed(accums.map(_._1.io.output.data(0)).reduce[UInt]{case (a:UInt,b:UInt) => 
        val t1 = Wire(new FixedPoint(s,d,f))
        val t2 = Wire(new FixedPoint(s,d,f))
        t1.r := a
        t2.r := b
        Mux(t1 > t2, t1.r,t2.r)
        // Utils.getRetimed(Mux(t1 > t2, t1.r,t2.r), opLatency.toInt)
      }, drain_latency, isDrainState)
    }


  def connectXBarRPort(rBundle: R_XBar, bufferPort: Int, muxAddr: (Int, Int), vecId: Int): UInt = {connectXBarRPort(rBundle, bufferPort, muxAddr, vecId, true.B)}
  def connectXBarRPort(rBundle: R_XBar, bufferPort: Int, muxAddr: (Int, Int), vecId: Int, flow: Bool): UInt = {io.output}

}
