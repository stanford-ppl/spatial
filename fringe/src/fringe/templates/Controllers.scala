package fringe.templates

import chisel3._

import fringe.templates.memory.{SRFF,FF}
import fringe.utils.{getRetimed, risingEdge}
import fringe.utils.implicits._

class ControlInterface(val p: ControlParams) extends Bundle {
  // Control signals
  val enable = Input(Bool())
  val done = Output(Bool())
  val doneLatch = Output(Bool())
  val rst = Input(Bool())
  val ctrDone = Input(Bool())
  val datapathEn = Output(Bool())
  val ctrInc = Output(Bool())
  val ctrRst = Output(Bool())
  val parentAck = Input(Bool())
  val backpressure = Input(Bool())
  val break = Input(Bool())

  // Signals from children
  val doneIn = Vec(p.depth, Input(Bool()))
  val maskIn = Vec(p.depth, Input(Bool()))

  // Signals to children
  val enableOut = Vec(p.depth, Output(Bool())) // Only for outer
  val childAck = Vec(p.depth, Output(Bool()))

  // Switch signals
  val selectsIn = Vec(p.cases, Input(Bool()))
  val selectsOut = Vec(p.cases, Output(Bool()))

  // FSM signals
  val nextState = Input(SInt(p.stateWidth.W))
  val initState = Input(SInt(p.stateWidth.W))
  val doneCondition = Input(Bool())
  val state = Output(SInt(p.stateWidth.W))

  // Signals for Streaming
  val ctrCopyDone = Vec(p.depth, Input(Bool()))
}

case class ControlParams(
  val sched: Sched, 
  val depth: Int, 
  val isFSM: Boolean = false, 
  val isPassthrough: Boolean = false,
  val stateWidth: Int = 32, 
  val cases: Int = 1, 
  val latency: Int = 0,
  val myName: String = "Controller"
){}

abstract class GeneralControl(val p: ControlParams) extends Module {
  val io = IO(new ControlInterface(p))

  override def desiredName = p.myName
}

sealed trait Sched
object Sequenced extends Sched // Seq extends Sched { override def toString = "Sequenced" }
object Pipelined extends Sched // Pipe extends Sched { override def toString = "Pipelined" }
object Streaming extends Sched // Streaming extends Sched { override def toString = "Streaming" }
object Fork extends Sched // Fork extends Sched { override def toString = "Fork" }
object ForkJoin extends Sched // ForkJoin extends Sched { override def toString = "ForkJoin" }

class OuterControl(p: ControlParams) extends GeneralControl(p) {
  def this(sched: Sched, depth: Int, isFSM: Boolean = false, stateWidth: Int = 32, cases: Int = 1, latency: Int = 0, myName: String = "OuterControl") = this(ControlParams(sched, depth, isFSM, false, stateWidth, cases, latency, myName))
  def this(tup: (Sched, Int, Boolean, Int, Int, Int)) = this(tup._1, tup._2, tup._3, tup._4, tup._5, tup._6)

  io.selectsOut <> DontCare
  
  // Create SRFF arrays for stages' actives and dones
  val active = List.tabulate(p.depth){i => Module(new SRFF())}
  val done = List.tabulate(p.depth){i => Module(new SRFF())}

  // Collect when all stages are done with all iters
  val allDone = done.map(_.io.output).reduce{_&&_} // TODO: Retime tree
  val finished = allDone || io.done || {if (p.depth > 1) done.last.io.input.set else false.B}

  // Tie down the asyn_resets
  active.foreach(_.io.input.asyn_reset := false.B)
  done.foreach(_.io.input.asyn_reset := false.B)
  done.foreach(_.io.input.reset := io.rst | allDone | io.parentAck | risingEdge(~io.break))

  // Create SRFFs that synchronize children on each iter
  val synchronize = Wire(Bool())
  val iterDone = List.tabulate(p.depth){i => Module(new SRFF())} 
  iterDone.foreach(_.io.input.asyn_reset := false.B)
  iterDone.foreach(_.io.input.reset := synchronize | io.rst | io.parentAck | risingEdge(~io.break))

  // Wire up stage communication
  p.sched match {
    case Pipelined if (!p.isFSM) => 
      // Define rule for when ctr increments
      io.ctrInc := iterDone(0).io.output & synchronize & io.backpressure

      // Configure synchronization
      val anydone = iterDone.map(_.io.output).reduce(_|_)
      synchronize := (active,iterDone,io.maskIn).zipped.map{case (a, id, mask) => a.io.output === id.io.output | (anydone & (a.io.output === ~mask.D(1)))}.reduce{_&&_} // TODO: Retime tree

      // Define logic for first stage
      active(0).io.input.set := !done(0).io.output & !io.ctrDone & io.enable & io.backpressure
      active(0).io.input.reset := io.ctrDone | io.parentAck | io.break
      iterDone(0).io.input.set := io.doneIn(0) | (!synchronize && !done(0).io.output && !io.maskIn(0).D(1) & io.enable & io.backpressure) | io.break
      done(0).io.input.set := (io.ctrDone & !io.rst) | io.break

      // Define logic for the rest of the stages
      for (i <- 1 until p.depth) {
        val extension = if (p.latency == 0) (synchronize & iterDone(i-1).io.output).D(1) else false.B // Hack for when retiming is turned off, in case mask turns on at the same time as the next iter should begin
        // Start when previous stage receives its first done, stop when previous stage turns off and current stage is done
        active(i).io.input.set := ((synchronize & active(i-1).io.output)) & io.enable & io.backpressure
        active(i).io.input.reset := done(i-1).io.output & synchronize | io.parentAck | io.break
        iterDone(i).io.input.set := (io.doneIn(i)) | io.break
        done(i).io.input.set := (done(i-1).io.output & synchronize & !io.rst) | io.break
      }
    
    case Sequenced => 
      // Define rule for when ctr increments
      io.ctrInc := io.doneIn.last | (!io.maskIn.last & iterDone.last.io.output & io.enable & io.backpressure)

      // Configure synchronization
      synchronize := io.doneIn.last.D(1) | (!io.maskIn.last & iterDone.last.io.output & io.enable & io.backpressure)
      
      // Define logic for first stage
      active(0).io.input.set := !done(0).io.output & !io.ctrDone & io.enable & io.backpressure & ~iterDone(0).io.output & !io.doneIn(0)
      active(0).io.input.reset := io.doneIn(0) | io.rst | io.parentAck | allDone | io.break
      iterDone(0).io.input.set := ((io.doneIn(0) & !synchronize) | (!io.maskIn(0) & io.enable & io.backpressure) | io.break) && !finished
      done(0).io.input.set := (io.ctrDone & !io.rst) | io.break

      // Define logic for the rest of the stages
      for (i <- 1 until p.depth) {
        active(i).io.input.set := (io.doneIn(i-1) | (iterDone(i-1).io.output & ~iterDone(i).io.output & !io.doneIn(i) & io.enable & io.backpressure)) & !synchronize
        active(i).io.input.reset := io.doneIn(i) | io.rst | io.parentAck | io.break
        iterDone(i).io.input.set := ((io.doneIn(i) | (iterDone(i-1).io.output & !io.maskIn(i) & io.enable & io.backpressure)) & !synchronize) | io.break
        done(i).io.input.set := (io.ctrDone & !io.rst) | io.break
      }

    case ForkJoin => 
      // Define rule for when ctr increments
      io.ctrInc := synchronize & io.backpressure

      // Configure synchronization
      synchronize := iterDone.map(_.io.output).reduce{_&_}

      // Define logic for all stages
      for (i <- 0 until p.depth) {
        active(i).io.input.set := ~iterDone(i).io.output & !io.doneIn(i) & !done(i).io.output & !io.ctrDone & io.enable & io.backpressure
        active(i).io.input.reset := io.doneIn(i) | io.rst | io.parentAck | io.break
        iterDone(i).io.input.set := (io.doneIn(i) | (!io.maskIn(i) & io.enable & io.backpressure)) | io.break
        done(i).io.input.set := (io.ctrDone & !io.rst) | io.break
      }

    case Streaming => 
      // Define rule for when ctr increments
      io.ctrInc := false.B // Don't care, each child has its own copy

      // Configure synchronization
      synchronize := false.B // iterDone.map(_.io.output).reduce{_&_}

      // Define logic for all stages
      for (i <- 0 until p.depth) {
        active(i).io.input.set := ~iterDone(i).io.output & !io.doneIn(i) & !done(i).io.output & !io.ctrDone & io.enable & io.backpressure & !io.ctrCopyDone(i)
        active(i).io.input.reset := io.ctrCopyDone(i) | io.rst | io.parentAck | io.break
        iterDone(i).io.input.set := ((io.doneIn(i) | !io.maskIn(i).D(1)) & io.enable & io.backpressure) | io.break
        iterDone(i).io.input.reset := io.doneIn(i).D(1) | io.parentAck | risingEdge(~io.break) // Override iterDone reset
        done(i).io.input.set := ((io.ctrCopyDone(i) & !io.rst) | (!io.maskIn(i).D(1) & io.enable & io.backpressure)) | io.break
        done(i).io.input.reset := io.parentAck | risingEdge(~io.break) // Override done reset
      }

    case Fork => 
      // Define rule for when ctr increments
      io.ctrInc := synchronize & io.backpressure

      // Configure synchronization
      synchronize := io.doneIn.reduce{_|_}

      // Define logic for all stages
      for (i <- 0 until p.depth) {
        active(i).io.input.set := ~iterDone(i).io.output & !io.doneIn(i) & !done(i).io.output & !io.ctrDone & io.enable & io.backpressure & io.selectsIn(i) & !io.done
        active(i).io.input.reset := io.doneIn(i) | io.rst
        iterDone(i).io.input.set := io.doneIn(i)
        iterDone(i).io.input.reset := done(i).io.output
        done(i).io.input.set := synchronize
      }

    case _ => // FSM, do sequential
      // Define rule for when ctr increments
      io.ctrInc := io.doneIn.last | (~io.maskIn.last.D(1) & iterDone.last.io.output & io.enable & io.backpressure)

      // Configure synchronization
      synchronize := io.doneIn.last.D(1) | (~io.maskIn.last.D(1) & iterDone.last.io.output & io.enable & io.backpressure)
      
      // Define logic for first stage
      active(0).io.input.set := !done(0).io.output & ~io.ctrDone & io.enable & io.backpressure & ~iterDone(0).io.output & ~io.doneIn(0)
      active(0).io.input.reset := io.doneIn(0) | io.rst | io.parentAck | allDone
      iterDone(0).io.input.set := (io.doneIn(0) & !synchronize) | (~io.maskIn(0).D(1) & io.enable & io.backpressure)
      done(0).io.input.set := io.ctrDone & ~io.rst

      // Define logic for the rest of the stages
      for (i <- 1 until p.depth) {
        active(i).io.input.set := (io.doneIn(i-1) | (iterDone(i-1).io.output & ~iterDone(i).io.output & ~io.doneIn(i) & io.enable & io.backpressure)) & ~synchronize
        active(i).io.input.reset := io.doneIn(i) | io.rst | io.parentAck
        iterDone(i).io.input.set := (io.doneIn(i) | (iterDone(i-1).io.output & ~io.maskIn(i).D(1) & io.enable & io.backpressure)) & ~synchronize
        done(i).io.input.set := io.ctrDone & ~io.rst
      }

  }

  iterDone.zip(io.childAck).foreach{ case (id, ca) => ca := id.io.output || io.break }
  io.enableOut.zipWithIndex.foreach{case (eo,i) => eo := io.enable & active(i).io.output & ~iterDone(i).io.output & io.maskIn(i) & ~allDone & {if (i == 0) ~io.ctrDone else true.B}}
  io.enableOut.foreach(chisel3.core.dontTouch(_))
  io.ctrRst := getRetimed(risingEdge(allDone) || io.parentAck, 1)

  // Done latch
  val doneLatchReg = RegInit(false.B)

  // Connect output signals
  if (p.isFSM) {
    val stateFSM = Module(new FF(p.stateWidth))
    stateFSM.io <> DontCare
    val doneReg = Module(new SRFF())
    doneReg.io <> DontCare

    stateFSM.io.wPort(0).data.head := io.nextState.asUInt
    stateFSM.io.wPort(0).init := io.initState.asUInt
    stateFSM.io.wPort(0).en.head := io.enable & iterDone.last.io.output
    stateFSM.io.wPort(0).reset := reset.toBool | ~io.enable
    io.state := stateFSM.io.rPort(0).output(0).asSInt

    doneReg.io.input.set := io.doneCondition & io.enable & iterDone.last.io.output.D(1)
    doneReg.io.input.reset := ~io.enable
    doneReg.io.input.asyn_reset := false.B
    active.zip(io.doneIn).foreach{case (a,di) => a.io.input.reset := di | io.rst | io.parentAck | doneReg.io.output}
    io.datapathEn := io.enable & ~doneReg.io.output & ~io.doneCondition
    io.done := doneReg.io.output & io.enable
    doneLatchReg := Mux(io.rst || io.parentAck, false.B, Mux(doneReg.io.output & io.enable, true.B, doneLatchReg))
    io.doneLatch := doneLatchReg
  }
  else {
    io.state := DontCare
    io.datapathEn := io.enable & ~allDone
    io.done := getRetimed(risingEdge(allDone), p.latency + 1, io.enable)

    doneLatchReg := Mux(io.rst || io.parentAck, false.B, Mux(getRetimed(risingEdge(allDone), p.latency, io.enable), true.B, doneLatchReg))
    io.doneLatch := doneLatchReg
  }


}



class InnerControl(p: ControlParams) extends GeneralControl(p) {
  def this(sched: Sched, isFSM: Boolean = false, isPassthrough: Boolean = false, stateWidth: Int = 32, cases: Int = 1, latency: Int = 0, myName: String = "InnerControl") = this(ControlParams(sched, cases, isFSM, isPassthrough, stateWidth, cases, latency, myName))
  def this(tup: (Sched, Boolean, Boolean, Int, Int, Int)) = this(tup._1, tup._2, tup._3, tup._4, tup._5, tup._6)

  // Create state SRFFs
  val active = Module(new SRFF())
  val done = Module(new SRFF())

  active.io.input.set := io.enable & !io.rst & ~io.ctrDone & ~done.io.output & io.backpressure
  active.io.input.reset := io.ctrDone | io.rst | io.parentAck | io.break
  active.io.input.asyn_reset := false.B
  done.io.input.reset := io.rst | io.parentAck | risingEdge(~io.break)
  done.io.input.asyn_reset := false.B
  p.sched match { case Fork => done.io.input.set := io.doneIn.reduce{_|_}; case _ => done.io.input.set := risingEdge(io.ctrDone) | io.break}

  io.selectsIn.zip(io.selectsOut).foreach{case(a,b)=>b:=a & io.enable}
  io.enableOut <> DontCare
  val doneLag = if (p.cases > 1) 0 else p.latency
  io.ctrRst := risingEdge(getRetimed(done.io.output, doneLag, io.backpressure)) | io.rst | io.parentAck

  if (!p.isFSM) {
    // Set outputs
    if (p.isPassthrough) { // pass through signals
      io.datapathEn := io.enable  & io.backpressure// & ~io.done & ~io.parentAck
      io.ctrInc := io.enable & io.backpressure
    }
    else {
      io.datapathEn := active.io.output & ~done.io.output & io.enable & io.backpressure
      io.ctrInc := active.io.output & io.enable & io.backpressure
    }

    io.done := risingEdge(getRetimed(done.io.output, doneLag, io.backpressure))
    io.childAck.zip(io.doneIn).foreach{case (a,b) => a := b.D(1) | io.ctrDone.D(1)}

    // Done latch
    val doneLatchReg = RegInit(false.B)
    doneLatchReg := Mux(io.rst || io.parentAck, false.B, Mux(getRetimed(risingEdge(done.io.output), 0 max {doneLag-1}, io.backpressure), true.B, doneLatchReg))
    io.doneLatch := doneLatchReg

    io.state := DontCare

  } else { // FSM inner
    val stateFSM = Module(new FF(p.stateWidth))
    stateFSM.io <> DontCare
    val doneReg = Module(new SRFF())
    doneReg.io <> DontCare

    // // With retime turned off (i.e p.latency == 0), this ensures mutations in the fsm body will be considered when jumping to next state
    // val depulser = RegInit(true.B) 
    // if (p.latency == 0) depulser := Mux(io.enable, ~depulser, depulser)
    // else depulser := true.B

    stateFSM.io.wPort(0).data.head := io.nextState.asUInt
    stateFSM.io.wPort(0).init := io.initState.asUInt
    stateFSM.io.wPort(0).en.head := io.enable & io.ctrDone
    // if (p.latency == 0) stateFSM.io.wPort(0).en := io.enable & ~depulser
    // else stateFSM.io.wPort(0).en := io.enable
    stateFSM.io.wPort(0).reset := reset.toBool | ~io.enable
    io.state := stateFSM.io.rPort(0).output.head.asSInt

    // Screwiness with "switch" signals until we have better fsm test cases
    io.childAck.last := io.doneIn.last || io.parentAck

    doneReg.io.input.set := io.doneCondition & io.enable
    doneReg.io.input.reset := ~io.enable
    doneReg.io.input.asyn_reset := false.B
    io.ctrInc := io.enable & ~doneReg.io.output & ~io.doneCondition & ~io.ctrDone & io.backpressure
    io.datapathEn := io.enable & ~doneReg.io.output & ~io.doneCondition & io.backpressure
    io.done := risingEdge(getRetimed(doneReg.io.output | (io.doneCondition & io.enable & io.backpressure), p.latency + 1, true.B))

    // Done latch
    val doneLatchReg = RegInit(false.B)
    doneLatchReg := Mux(io.rst || io.parentAck, false.B, Mux(risingEdge(getRetimed(doneReg.io.output | (io.doneCondition & io.enable & io.backpressure), p.latency, true.B)), true.B, doneLatchReg))
    io.doneLatch := doneLatchReg

  }
}
