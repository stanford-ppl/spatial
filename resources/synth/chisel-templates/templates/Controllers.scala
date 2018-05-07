package templates

import templates.ops._
import chisel3._
import chisel3.util._
import chisel3.internal.sourceinfo._
import types._
import fringe._
// import emul._
import Utils._

import scala.math._

sealed trait Sched
// Easier to just let codegen use their toString and catch those names here
object Sequential extends Sched // Seq extends Sched { override def toString = "Sequential" }
object Pipeline extends Sched // Pipe extends Sched { override def toString = "Pipeline" }
object Stream extends Sched // Stream extends Sched { override def toString = "Stream" }
object Fork extends Sched // Fork extends Sched { override def toString = "Fork" }
object ForkJoin extends Sched // ForkJoin extends Sched { override def toString = "ForkJoin" }

class OuterControl(val sched: Sched, val depth: Int, val isFSM: Boolean = false, val stateWidth: Int = 32, cases: Int = 1) extends Module {
  // Overloaded construters
  // Tuple unpacker
  def this(tuple: (Sched, Int, Boolean)) = this(tuple._1,tuple._2,tuple._3)

  val io = IO( new Bundle {
    // Control signals
    val enable = Input(Bool())
    val done = Output(Bool())
    val rst = Input(Bool())
    val ctrDone = Input(Bool())
    val datapathEn = Output(Bool())
    val ctrInc = Output(Bool())
    val ctrRst = Output(Bool())
    val parentAck = Input(Bool())

    // Signals from children
    val doneIn = Vec(depth, Input(Bool()))
    val maskIn = Vec(depth, Input(Bool()))

    // Signals to children
    val enableOut = Vec(depth, Output(Bool()))
    val childAck = Vec(depth, Output(Bool()))

    // Switch signals
    val selectsIn = Vec(cases, Input(Bool()))
    val selectsOut = Vec(cases, Output(Bool()))

    // FSM signals
    val nextState = Input(SInt(stateWidth.W))
    val initState = Input(SInt(stateWidth.W))
    val doneCondition = Input(Bool())
    val state = Output(SInt(stateWidth.W))

    // Signals for Stream
    val ctrCopyDone = Vec(depth, Input(Bool()))
  })

  // Create SRFF arrays for stages' actives and dones
  val active = List.tabulate(depth){i => Module(new SRFF())}
  val done = List.tabulate(depth){i => Module(new SRFF())}

  // Collect when all stages are done with all iters
  val allDone = done.map(_.io.output.data).reduce{_&&_} // TODO: Retime tree

  // Tie down the asyn_resets
  active.foreach(_.io.input.asyn_reset := false.B)
  done.foreach(_.io.input.asyn_reset := false.B)
  done.foreach(_.io.input.reset := io.rst | allDone | io.parentAck)

  // Create SRFFs that synchronize children on each iter
  val synchronize = Wire(Bool())
  val iterDone = List.tabulate(depth){i => Module(new SRFF())} 
  iterDone.foreach(_.io.input.asyn_reset := false.B)
  iterDone.foreach(_.io.input.reset := synchronize | io.rst | io.parentAck)

  // Wire up stage communication
  sched match {
    case Pipeline => 
      // Define rule for when ctr increments
      io.ctrInc := iterDone(0).io.output.data & synchronize

      // Configure synchronization
      synchronize := active.zip(iterDone).map{case (a, id) => a.io.output.data === id.io.output.data}.reduce{_&&_} // TODO: Retime tree

      // Define logic for first stage
      active(0).io.input.set := Mux(!done(0).io.output.data & ~io.ctrDone & io.enable, true.B, false.B)
      active(0).io.input.reset := io.ctrDone | io.parentAck
      iterDone(0).io.input.set := (io.doneIn(0)) | (~io.maskIn(0) & io.enable)
      done(0).io.input.set := io.ctrDone & ~io.rst

      // Define logic for the rest of the stages
      for (i <- 1 until depth) {
        // Start when previous stage receives its first done, stop when previous stage turns off and current stage is done
        active(i).io.input.set := ((synchronize & iterDone(i-1).io.output.data) | ~io.maskIn(i-1)) & io.enable
        active(i).io.input.reset := done(i-1).io.output.data & synchronize | io.parentAck
        iterDone(i).io.input.set := (io.doneIn(i)) | (iterDone(i-1).io.output.data & ~io.maskIn(i) & io.enable)
        done(i).io.input.set := done(i-1).io.output.data & synchronize & ~io.rst
      }
    
    case Sequential => 
      if (!isFSM) {
        // Define rule for when ctr increments
        io.ctrInc := io.doneIn.last

        // Configure synchronization
        synchronize := io.doneIn.last.D(1)
        
        // Define logic for first stage
        active(0).io.input.set := Mux(!done(0).io.output.data & ~io.ctrDone & io.enable & ~io.doneIn(0), true.B, false.B)
        active(0).io.input.reset := io.doneIn(0) | io.rst | io.parentAck | allDone
        iterDone(0).io.input.set := (io.doneIn(0) & ~synchronize) | (~io.maskIn(0) & io.enable)
        done(0).io.input.set := io.ctrDone & ~io.rst

        // Define logic for the rest of the stages
        for (i <- 1 until depth) {
          active(i).io.input.set := io.doneIn(i-1) | (~io.maskIn(i-1) & ~io.doneIn(i) & io.enable)
          active(i).io.input.reset := io.doneIn(i) | io.rst | io.parentAck
          iterDone(i).io.input.set := (io.doneIn(i) & ~synchronize) | (iterDone(i-1).io.output.data & ~io.maskIn(i) & io.enable)
          done(i).io.input.set := io.ctrDone & ~io.rst
        }
      } else {
        val stateFSM = Module(new FF(stateWidth))
        val doneReg = Module(new SRFF())

        stateFSM.io.input(0).data := io.nextState.asUInt
        stateFSM.io.input(0).init := io.initState.asUInt
        stateFSM.io.input(0).en := io.enable
        stateFSM.io.input(0).reset := reset.toBool | ~io.enable
        io.state := stateFSM.io.output.data.asSInt

        doneReg.io.input.set := io.doneCondition & io.enable
        doneReg.io.input.reset := ~io.enable
        doneReg.io.input.asyn_reset := false.B
        io.done := doneReg.io.output.data | (io.doneCondition & io.enable)
      }

    case ForkJoin => 
      // Define rule for when ctr increments
      io.ctrInc := synchronize

      // Configure synchronization
      synchronize := iterDone.map(_.io.output.data).reduce{_&_}

      // Define logic for all stages
      for (i <- 0 until depth) {
        active(i).io.input.set := Mux(~iterDone(i).io.output.data & ~io.doneIn(i) & !done(i).io.output.data & ~io.ctrDone & io.enable, true.B, false.B)
        active(i).io.input.reset := io.doneIn(i) | io.rst | io.parentAck
        iterDone(i).io.input.set := io.doneIn(i) | (~io.maskIn(i) & io.enable)
        done(i).io.input.set := io.ctrDone & ~io.rst
      }

    case Stream => 
      // Define rule for when ctr increments
      io.ctrInc := synchronize // Don't care, each child has its own copy

      // Configure synchronization
      synchronize := iterDone.map(_.io.output.data).reduce{_&_}

      // Define logic for all stages
      for (i <- 0 until depth) {
        active(i).io.input.set := Mux(~iterDone(i).io.output.data & ~io.doneIn(i) & !done(i).io.output.data & ~io.ctrDone & io.enable, true.B, false.B)
        active(i).io.input.reset := io.ctrCopyDone(i) | io.rst | io.parentAck
        iterDone(i).io.input.set := io.doneIn(i) | ~io.maskIn(i)
        iterDone(i).io.input.reset := io.doneIn(i).D(1) // Override iterDone reset
        done(i).io.input.set := (io.ctrCopyDone(i) & ~io.rst) | (~io.maskIn(i) & io.enable)
      }

    case Fork => 
      // Define rule for when ctr increments
      io.ctrInc := synchronize

      // Configure synchronization
      synchronize := io.doneIn.reduce{_|_}

      // Define logic for all stages
      for (i <- 0 until depth) {
        active(i).io.input.set := io.selectsIn(i)
        active(i).io.input.reset := synchronize
        iterDone(i).io.input.set := io.doneIn(i)
        iterDone(i).io.input.reset := synchronize
        done(i).io.input.set := synchronize
      }


  }

  // Connect output signals
  iterDone.zip(io.childAck).foreach{ case (id, ca) => ca := id.io.output.data }
  io.datapathEn := io.enable & ~io.done
  io.enableOut.zipWithIndex.foreach{case (eo,i) => eo := io.enable & active(i).io.output.data & ~iterDone(i).io.output.data & io.maskIn(i) & ~allDone & {if (i == 0) ~io.ctrDone else true.B}}
  io.done := Utils.risingEdge(allDone)
  io.ctrRst := Utils.getRetimed(Utils.risingEdge(allDone), 1)

}



class InnerControl(val sched: Sched, val isFSM: Boolean = false, val stateWidth: Int = 32, val cases: Int = 1) extends Module {

  // Overloaded construters
  // Tuple unpacker
  def this(tuple: (Sched, Boolean, Int)) = this(tuple._1,tuple._2,tuple._3)

  // Module IO
  val io = IO(new Bundle {
    // Control signals
    val enable = Input(Bool())
    val done = Output(Bool())
    val rst = Input(Bool())
    val datapathEn = Output(Bool())
    val ctrDone = Input(Bool())
    val ctrInc = Output(Bool())
    val ctrRst = Output(Bool())
    val parentAck = Input(Bool())

    // Switch signals
    val selectsIn = Vec(cases, Input(Bool()))
    val selectsOut = Vec(cases, Output(Bool()))
    val doneIn = Vec(cases, Input(Bool()))

    // FSM signals
    val nextState = Input(SInt(stateWidth.W))
    val initState = Input(SInt(stateWidth.W))
    val doneCondition = Input(Bool())
    val state = Output(SInt(stateWidth.W))
  })

  // Create state SRFFs
  val active = Module(new SRFF())
  val done = Module(new SRFF())

  if (!isFSM) {
    active.io.input.set := io.enable & !io.rst & ~io.ctrDone & ~done.io.output.data
    active.io.input.reset := io.ctrDone | io.rst | io.parentAck
    active.io.input.asyn_reset := false.B
    sched match { case Fork => done.io.input.set := io.doneIn.reduce{_|_}; case _ => done.io.input.set := io.ctrDone }
    done.io.input.reset := io.rst | io.parentAck
    done.io.input.asyn_reset := false.B

    // Set outputs
    io.selectsIn.zip(io.selectsOut).foreach{case(a,b)=>b:=a & io.enable}
    io.ctrRst := !active.io.output.data | io.rst 
    io.datapathEn := active.io.output.data & ~io.ctrDone
    io.ctrInc := active.io.output.data
    io.done := Utils.risingEdge(done.io.output.data)

  } else { // FSM inner
    val stateFSM = Module(new FF(stateWidth))
    val doneReg = Module(new SRFF())

    stateFSM.io.input(0).data := io.nextState.asUInt
    stateFSM.io.input(0).init := io.initState.asUInt
    stateFSM.io.input(0).en := io.enable
    stateFSM.io.input(0).reset := reset.toBool | ~io.enable
    io.state := stateFSM.io.output.data.asSInt

    doneReg.io.input.set := io.doneCondition & io.enable
    doneReg.io.input.reset := ~io.enable
    doneReg.io.input.asyn_reset := false.B
    io.done := doneReg.io.output.data | (io.doneCondition & io.enable)

  }
}
