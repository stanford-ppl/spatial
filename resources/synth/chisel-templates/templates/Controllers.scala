package templates

import chisel3._
import chisel3.util._
import chisel3.internal.sourceinfo._
import types._
import fringe._
import emul._
import Utils._

import scala.math._

sealed trait Sched
// Easier to just let codegen use their toString and catch those names here
object Sequential extends Sched // Seq extends Sched { override def toString = "Sequential" }
object Pipeline extends Sched // Pipe extends Sched { override def toString = "Pipeline" }
object Stream extends Sched // Stream extends Sched { override def toString = "Stream" }
object Fork extends Sched // Fork extends Sched { override def toString = "Fork" }
object ForkJoin extends Sched // ForkJoin extends Sched { override def toString = "ForkJoin" }

class OuterController(val sched: Sched, val depth: Int, val isFSM: Boolean = false, val stateWidth: Int = 32) extends Module {
  // Overloaded construters
  // Tuple unpacker
  def this(tuple: (Sched, Int, Boolean)) = this(tuple._1,tuple._2,tuple._3)

  val io = IO( new Bundle {
    // Controller signals
    val enable = Input(Bool())
    val done = Output(Bool())
    val rst = Input(Bool())
    val ctrDone = Input(Bool())
    val ctrInc = Output(Bool())
    val ctrRst = Output(Bool())

    // Signals from children
    val doneIn = Vec(depth, Input(Bool()))
    val maskIn = Vec(depth, Input(Bool()))

    // Signals to children
    val enableOut = Vec(depth, Output(Bool()))
  })

  // Create SRFF arrays for stages' actives and dones
  val active = List.tabulate(depth){i => Module(new SRFF())}
  val done = List.tabulate(depth){i => Module(new SRFF())}

  // Collect when all stages are done with all iters
  val allDone = done.map(_.io.output.data).reduce{_&&_} // TODO: Retime tree

  // Tie down the asyn_resets
  active.foreach(_.io.input.asyn_reset := false.B)
  done.foreach(_.io.input.asyn_reset := false.B)
  done.foreach(_.io.input.reset := io.rst | allDone)

  // Create SRFFs that synchronize children on each iter
  val synchronize = Wire(Bool())
  val iterDone = List.tabulate(depth){i => Module(new SRFF())} 
  iterDone.foreach(_.io.input.asyn_reset := false.B)
  iterDone.foreach(_.io.input.reset := synchronize | io.rst)

  // Wire up stage communication
  sched match {
    case Pipeline => 
      // Define rule for when ctr increments
      io.ctrInc := iterDone(0).io.output.data & synchronize

      // Configure synchronization
      synchronize := active.zip(iterDone).map{case (a, id) => a.io.output.data === id.io.output.data}.reduce{_&&_} // TODO: Retime tree

      // Define logic for first stage
      active(0).io.input.set := Mux(!done(0).io.output.data & io.enable, true.B, false.B)
      active(0).io.input.reset := io.ctrDone
      iterDone(0).io.input.set := io.doneIn(0)
      done(0).io.input.set := io.ctrDone & ~io.rst

      // Define logic for the rest of the stages
      for (i <- 1 until depth) {
        // Start when previous stage receives its first done, stop when previous stage turns off and current stage is done
        active(i).io.input.set := synchronize & iterDone(i-1).io.output.data & io.enable
        active(i).io.input.reset := done(i-1).io.output.data & synchronize
        iterDone(i).io.input.set := io.doneIn(i)
        done(i).io.input.set := done(i-1).io.output.data & synchronize & ~io.rst
      }
    
    case Sequential => 
      // Define rule for when ctr increments
      io.ctrInc := io.doneIn.last

      // Configure synchronization
      synchronize := io.doneIn.last
      
      // Define logic for first stage
      active(0).io.input.set := Mux(!done(0).io.output.data & ~io.ctrDone & io.enable, true.B, false.B)
      active(0).io.input.reset := io.doneIn(0) | io.rst
      iterDone(0).io.input.set := io.doneIn(0) & ~synchronize
      done(0).io.input.set := io.ctrDone & ~io.rst

      // Define logic for the rest of the stages
      for (i <- 1 until depth) {
        active(i).io.input.set := io.doneIn(i-1)
        active(i).io.input.reset := io.doneIn(i) | io.rst
        iterDone(i).io.input.set := io.doneIn(i) & ~synchronize
        done(i).io.input.set := io.ctrDone & ~io.rst
      }

      // Connect output stage enables
      io.enableOut.zip(active).zip(io.maskIn).foreach{case ((eo,a),m) => eo := a.io.output.data & m}

    case ForkJoin | Stream => 
      // Define rule for when ctr increments
      io.ctrInc := synchronize

      // Configure synchronization
      synchronize := iterDone.map(_.io.output.data).reduce{_&_}

      // Define logic for all stages
      for (i <- 0 until depth) {
        active(i).io.input.set := Mux(~iterDone(i).io.output.data & ~io.doneIn(i) & !done(i).io.output.data & ~io.ctrDone & io.enable, true.B, false.B)
        active(i).io.input.reset := io.doneIn(i) | io.rst
        iterDone(i).io.input.set := io.doneIn(i)
        done(i).io.input.set := io.ctrDone & ~io.rst
      }


  }

  // Connect output signals
  io.enableOut.zipWithIndex.foreach{case (eo,i) => eo := active(i).io.output.data & ~iterDone(i).io.output.data & io.maskIn(i)}
  io.done := Utils.risingEdge(allDone)
  io.ctrRst := Utils.getRetimed(Utils.risingEdge(allDone), 1)

}



class InnerController(val sched: Sched, val isFSM: Boolean = false, val stateWidth: Int = 32) extends Module {

  // Overloaded construters
  // Tuple unpacker
  def this(tuple: (Sched, Boolean, Int)) = this(tuple._1,tuple._2,tuple._3)

  // States
  val pipeInit = 0
  val pipeReset = 1
  val pipeRun = 2
  val pipeDone = 3
  val pipeSpinWait = 4

  // Module IO
  val io = IO(new Bundle {
    // Controller signals
    val enable = Input(Bool())
    val done = Output(Bool())
    val rst = Input(Bool())
    val ctrDone = Input(Bool())
    val ctrInc = Output(Bool())
    val ctrRst = Output(Bool())

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
    active.io.input.reset := io.ctrDone | io.rst
    active.io.input.asyn_reset := false.B
    done.io.input.set := io.ctrDone
    done.io.input.reset := io.rst
    done.io.input.asyn_reset := false.B

    // Set outputs
    io.ctrRst := !active.io.output.data | io.rst 
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
