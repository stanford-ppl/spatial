package templates

import ops._
import chisel3._
import types._

class FixFMAAccum(val cycleLatency: Double, val fmaLatency: Double, val s: Boolean, val d: Int, val f: Int, init: Double) extends Module {
  def this(tup: (Double, Double, Boolean, Int, Int, Double)) = this(tup._1, tup._2, tup._3, tup._4, tup._5, tup._6)

  val cw = Utils.log2Up(cycleLatency) + 2
  val initBits = (init*scala.math.pow(2,f)).toLong.S((d+f).W).asUInt
  val io = IO(new Bundle{
    val input1 = Input(UInt((d+f).W))
    val input2 = Input(UInt((d+f).W))
    val enable = Input(Bool())
    val reset = Input(Bool())
    val output = Output(UInt((d+f).W))
  })

  val fixin1 = Wire(new FixedPoint(s,d,f))
  fixin1.r := io.input1
  val fixin2 = Wire(new FixedPoint(s,d,f))
  fixin2.r := io.input2

  val laneCtr = Module(new SingleCounter(1, Some(0), Some(cycleLatency.toInt), Some(1), Some(0), cw))
  laneCtr.io.input.enable := io.enable
  laneCtr.io.input.reset := io.reset
  laneCtr.io.input.saturate := false.B

  val dispatchLane = laneCtr.io.output.count(0).asUInt
  val accums = Array.tabulate(cycleLatency.toInt){i => (Module(new FF(d+f)), i.U(cw.W))}
  accums.foreach{case (acc, lane) => 
    val fixadd = Wire(new FixedPoint(s,d,f))
    fixadd.r := acc.io.output.data(0)
    val result = Wire(new FixedPoint(s,d,f))
    Utils.FixFMA(fixin1, fixin2, fixadd, fmaLatency.toInt, true.B).cast(result)
    acc.io.xBarW(0).data.head := result.r
    acc.io.xBarW(0).en.head := Utils.getRetimed(io.enable & dispatchLane === lane, fmaLatency.toInt)
    acc.io.xBarW(0).reset.head := io.reset
    acc.io.xBarW(0).init.head := initBits
  }

  io.output := Utils.getRetimed(accums.map(_._1.io.output.data(0)).reduce{_+_}, (Utils.log2Up(cycleLatency).toDouble).toInt).r // TODO: Please build tree and retime appropriately

}


class FixAddAccum(val cycleLatency: Double, val addLatency: Double, val s: Boolean, val d: Int, val f: Int, init: Double) extends Module {
  def this(tup: (Double, Double, Boolean, Int, Int, Double)) = this(tup._1, tup._2, tup._3, tup._4, tup._5, tup._6)

  val cw = Utils.log2Up(cycleLatency) + 2
  val initBits = (init*scala.math.pow(2,f)).toLong.S((d+f).W).asUInt
  val io = IO(new Bundle{
    val input1 = Input(UInt((d+f).W))
    val enable = Input(Bool())
    val reset = Input(Bool())
    val output = Output(UInt((d+f).W))
  })

  val fixin1 = Wire(new FixedPoint(s,d,f))
  fixin1.r := io.input1

  val laneCtr = Module(new SingleCounter(1, Some(0), Some(cycleLatency.toInt), Some(1), Some(0), cw))
  laneCtr.io.input.enable := io.enable
  laneCtr.io.input.reset := io.reset
  laneCtr.io.input.saturate := false.B

  val dispatchLane = laneCtr.io.output.count(0).asUInt
  val accums = Array.tabulate(cycleLatency.toInt){i => (Module(new FF(d+f)), i.U(cw.W))}
  accums.foreach{case (acc, lane) => 
    val fixadd = Wire(new FixedPoint(s,d,f))
    fixadd.r := acc.io.output.data(0)
    val result = Wire(new FixedPoint(s,d,f))
    acc.io.xBarW(0).data.head := (fixadd + fixin1).r
    acc.io.xBarW(0).en.head := Utils.getRetimed(io.enable & dispatchLane === lane, addLatency.toInt)
    acc.io.xBarW(0).reset.head := io.reset
    acc.io.xBarW(0).init.head := initBits
  }

  io.output := Utils.getRetimed(accums.map(_._1.io.output.data(0)).reduce{_+_}, (Utils.log2Up(cycleLatency).toDouble).toInt).r // TODO: Please build tree and retime appropriately

}
