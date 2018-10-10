package fringe.templates

import chisel3._

import fringe.templates.memory.{SRFF,FF}
import fringe.utils.{getRetimed, risingEdge}
import fringe.utils.implicits._

class ControlInterface(p: ControlParams) extends Bundle {
  // Control signals
  val enable = Input(Bool())
  val done = Output(Bool())
  val rst = Input(Bool())
  val ctrDone = Input(Bool())
  val datapathEn = Output(Bool())
  val ctrInc = Output(Bool())
  val ctrRst = Output(Bool())
  val parentAck = Input(Bool())
  val flow = Input(Bool())

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

  // Create SRFF arrays for stages' actives and dones
  val active = List.tabulate(p.depth){i => Module(new SRFF())}
  val done = List.tabulate(p.depth){i => Module(new SRFF())}

  // Collect when all stages are done with all iters
  val allDone = done.map(_.io.output.data).reduce{_&&_} // TODO: Retime tree

  // Tie down the asyn_resets
  active.foreach(_.io.input.asyn_reset := false.B)
  done.foreach(_.io.input.asyn_reset := false.B)
  done.foreach(_.io.input.reset := io.rst | allDone | io.parentAck)

  // Create SRFFs that synchronize children on each iter
  val synchronize = Wire(Bool())
  val iterDone = List.tabulate(p.depth){i => Module(new SRFF())} 
  iterDone.foreach(_.io.input.asyn_reset := false.B)
  iterDone.foreach(_.io.input.reset := synchronize | io.rst | io.parentAck)

  // Wire up stage communication
  p.sched match {
    case Pipelined if (!p.isFSM) => 
      // Define rule for when ctr increments
      io.ctrInc := iterDone(0).io.output.data & synchronize & io.flow

      // Configure synchronization
      val anydone = iterDone.map(_.io.output.data).reduce(_|_)
      synchronize := (active,iterDone,io.maskIn).zipped.map{case (a, id, mask) => a.io.output.data === id.io.output.data | (anydone & (a.io.output.data === ~mask.D(1)))}.reduce{_&&_} // TODO: Retime tree

      // Define logic for first stage
      active(0).io.input.set := !done(0).io.output.data & !io.ctrDone & io.enable & io.flow
      active(0).io.input.reset := io.ctrDone | io.parentAck
      iterDone(0).io.input.set := io.doneIn(0) | (!io.maskIn(0).D(1) & io.enable & io.flow)
      done(0).io.input.set := io.ctrDone & !io.rst

      // Define logic for the rest of the stages
      for (i <- 1 until p.depth) {
        val extension = if (p.latency == 0) (synchronize & iterDone(i-1).io.output.data).D(1) else false.B // Hack for when retiming is turned off, in case mask turns on at the same time as the next iter should begin
        // Start when previous stage receives its first done, stop when previous stage turns off and current stage is done
        active(i).io.input.set := ((synchronize & active(i-1).io.output.data)) & io.enable & io.flow
        active(i).io.input.reset := done(i-1).io.output.data & synchronize | io.parentAck
        iterDone(i).io.input.set := (io.doneIn(i))
        done(i).io.input.set := done(i-1).io.output.data & synchronize & !io.rst
      }
    
    case Sequenced => 
      // Define rule for when ctr increments
      io.ctrInc := io.doneIn.last | (!io.maskIn.last & iterDone.last.io.output.data & io.enable & io.flow)

      // Configure synchronization
      synchronize := io.doneIn.last.D(1) | (!io.maskIn.last & iterDone.last.io.output.data & io.enable & io.flow)
      
      // Define logic for first stage
      active(0).io.input.set := !done(0).io.output.data & !io.ctrDone & io.enable & io.flow & ~iterDone(0).io.output.data & !io.doneIn(0)
      active(0).io.input.reset := io.doneIn(0) | io.rst | io.parentAck | allDone
      iterDone(0).io.input.set := (io.doneIn(0) & !synchronize) | (!io.maskIn(0) & io.enable & io.flow)
      done(0).io.input.set := io.ctrDone & !io.rst

      // Define logic for the rest of the stages
      for (i <- 1 until p.depth) {
        active(i).io.input.set := (io.doneIn(i-1) | (iterDone(i-1).io.output.data & ~iterDone(i).io.output.data & !io.doneIn(i) & io.enable & io.flow)) & !synchronize
        active(i).io.input.reset := io.doneIn(i) | io.rst | io.parentAck
        iterDone(i).io.input.set := (io.doneIn(i) | (iterDone(i-1).io.output.data & !io.maskIn(i) & io.enable & io.flow)) & !synchronize
        done(i).io.input.set := io.ctrDone & !io.rst
      }

    case ForkJoin => 
      // Define rule for when ctr increments
      io.ctrInc := synchronize & io.flow

      // Configure synchronization
      synchronize := iterDone.map(_.io.output.data).reduce{_&_}

      // Define logic for all stages
      for (i <- 0 until p.depth) {
        active(i).io.input.set := ~iterDone(i).io.output.data & !io.doneIn(i) & !done(i).io.output.data & !io.ctrDone & io.enable & io.flow
        active(i).io.input.reset := io.doneIn(i) | io.rst | io.parentAck
        iterDone(i).io.input.set := io.doneIn(i) | (!io.maskIn(i) & io.enable & io.flow)
        done(i).io.input.set := io.ctrDone & !io.rst
      }

    case Streaming => 
      // Define rule for when ctr increments
      io.ctrInc := false.B // Don't care, each child has its own copy

      // Configure synchronization
      synchronize := false.B // iterDone.map(_.io.output.data).reduce{_&_}

      // Define logic for all stages
      for (i <- 0 until p.depth) {
        active(i).io.input.set := ~iterDone(i).io.output.data & !io.doneIn(i) & !done(i).io.output.data & !io.ctrDone & io.enable & io.flow & !io.ctrCopyDone(i)
        active(i).io.input.reset := io.ctrCopyDone(i) | io.rst | io.parentAck
        iterDone(i).io.input.set := (io.doneIn(i) | !io.maskIn(i).D(1)) & io.enable & io.flow
        iterDone(i).io.input.reset := io.doneIn(i).D(1) | io.parentAck // Override iterDone reset
        done(i).io.input.set := (io.ctrCopyDone(i) & !io.rst) | (!io.maskIn(i).D(1) & io.enable & io.flow)
        done(i).io.input.reset := io.parentAck // Override done reset
      }

    case Fork => 
      // Define rule for when ctr increments
      io.ctrInc := synchronize & io.flow

      // Configure synchronization
      synchronize := io.doneIn.reduce{_|_}

      // Define logic for all stages
      for (i <- 0 until p.depth) {
        active(i).io.input.set := ~iterDone(i).io.output.data & !io.doneIn(i) & !done(i).io.output.data & !io.ctrDone & io.enable & io.flow & io.selectsIn(i) & !io.done
        active(i).io.input.reset := io.doneIn(i) | io.rst
        iterDone(i).io.input.set := io.doneIn(i)
        iterDone(i).io.input.reset := done(i).io.output.data
        done(i).io.input.set := synchronize
      }

    case _ => // FSM, do sequential
      // Define rule for when ctr increments
      io.ctrInc := io.doneIn.last | (~io.maskIn.last.D(1) & iterDone.last.io.output.data & io.enable & io.flow)

      // Configure synchronization
      synchronize := io.doneIn.last.D(1) | (~io.maskIn.last.D(1) & iterDone.last.io.output.data & io.enable & io.flow)
      
      // Define logic for first stage
      active(0).io.input.set := !done(0).io.output.data & ~io.ctrDone & io.enable & io.flow & ~iterDone(0).io.output.data & ~io.doneIn(0)
      active(0).io.input.reset := io.doneIn(0) | io.rst | io.parentAck | allDone
      iterDone(0).io.input.set := (io.doneIn(0) & !synchronize) | (~io.maskIn(0).D(1) & io.enable & io.flow)
      done(0).io.input.set := io.ctrDone & ~io.rst

      // Define logic for the rest of the stages
      for (i <- 1 until p.depth) {
        active(i).io.input.set := (io.doneIn(i-1) | (iterDone(i-1).io.output.data & ~iterDone(i).io.output.data & ~io.doneIn(i) & io.enable & io.flow)) & ~synchronize
        active(i).io.input.reset := io.doneIn(i) | io.rst | io.parentAck
        iterDone(i).io.input.set := (io.doneIn(i) | (iterDone(i-1).io.output.data & ~io.maskIn(i).D(1) & io.enable & io.flow)) & ~synchronize
        done(i).io.input.set := io.ctrDone & ~io.rst
      }



  }


  iterDone.zip(io.childAck).foreach{ case (id, ca) => ca := id.io.output.data }
  io.enableOut.zipWithIndex.foreach{case (eo,i) => eo := io.enable & active(i).io.output.data & ~iterDone(i).io.output.data & io.maskIn(i) & ~allDone & {if (i == 0) ~io.ctrDone else true.B}}
  io.ctrRst := getRetimed(risingEdge(allDone), 1)
  
  // Connect output signals
  if (p.isFSM) {
    val stateFSM = Module(new FF(p.stateWidth))
    val doneReg = Module(new SRFF())

    stateFSM.io.xBarW(0).data.head := io.nextState.asUInt
    stateFSM.io.xBarW(0).init.head := io.initState.asUInt
    stateFSM.io.xBarW(0).en.head := io.enable & iterDone.last.io.output.data
    stateFSM.io.xBarW(0).reset.head := reset.toBool | ~io.enable
    io.state := stateFSM.io.output.data(0).asSInt

    doneReg.io.input.set := io.doneCondition & io.enable & iterDone.last.io.output.data.D(1)
    doneReg.io.input.reset := ~io.enable
    doneReg.io.input.asyn_reset := false.B
    active.zip(io.doneIn).foreach{case (a,di) => a.io.input.reset := di | io.rst | io.parentAck | doneReg.io.output.data}
    io.datapathEn := io.enable & ~doneReg.io.output.data & ~io.doneCondition
    io.done := doneReg.io.output.data & io.enable
  }
  else {
    io.datapathEn := io.enable & ~allDone
    io.done := getRetimed(risingEdge(allDone), p.latency + 1, io.enable)
  }


}



class InnerControl(p: ControlParams) extends GeneralControl(p) {
  def this(sched: Sched, isFSM: Boolean = false, isPassthrough: Boolean = false, stateWidth: Int = 32, cases: Int = 1, latency: Int = 0, myName: String = "InnerControl") = this(ControlParams(sched, cases, isFSM, isPassthrough, stateWidth, cases, latency, myName))
  def this(tup: (Sched, Boolean, Boolean, Int, Int, Int)) = this(tup._1, tup._2, tup._3, tup._4, tup._5, tup._6)

  // Create state SRFFs
  val active = Module(new SRFF())
  val done = Module(new SRFF())

  if (!p.isFSM) {
    active.io.input.set := io.enable & !io.rst & ~io.ctrDone & ~done.io.output.data
    active.io.input.reset := io.ctrDone | io.rst | io.parentAck
    active.io.input.asyn_reset := false.B
    p.sched match { case Fork => done.io.input.set := io.doneIn.reduce{_|_}; case _ => done.io.input.set := risingEdge(io.ctrDone)}
    done.io.input.reset := io.rst | io.parentAck
    done.io.input.asyn_reset := false.B

    // Set outputs
    io.selectsIn.zip(io.selectsOut).foreach{case(a,b)=>b:=a & io.enable}
    io.ctrRst := !active.io.output.data | io.rst 
    if (p.isPassthrough) { // pass through signals
      io.datapathEn := io.enable  & io.flow// & ~io.done & ~io.parentAck
      io.ctrInc := io.enable & io.flow
    }
    else {
      io.datapathEn := active.io.output.data & ~done.io.output.data & io.enable & io.flow
      io.ctrInc := active.io.output.data & io.enable & io.flow
    }
    val doneLag = if (p.cases > 1) 0 else p.latency
    io.done := risingEdge(getRetimed(done.io.output.data, doneLag, true.B))
    io.childAck.zip(io.doneIn).foreach{case (a,b) => a := b.D(1) | io.ctrDone.D(1)}

  } else { // FSM inner
    val stateFSM = Module(new FF(p.stateWidth))
    val doneReg = Module(new SRFF())

    // // With retime turned off (i.e p.latency == 0), this ensures mutations in the fsm body will be considered when jumping to next state
    // val depulser = RegInit(true.B) 
    // if (p.latency == 0) depulser := Mux(io.enable, ~depulser, depulser)
    // else depulser := true.B

    stateFSM.io.xBarW(0).data.head := io.nextState.asUInt
    stateFSM.io.xBarW(0).init.head := io.initState.asUInt
    stateFSM.io.xBarW(0).en.head := io.enable & io.ctrDone
    // if (p.latency == 0) stateFSM.io.xBarW(0).en := io.enable & ~depulser
    // else stateFSM.io.xBarW(0).en := io.enable
    stateFSM.io.xBarW(0).reset.head := reset.toBool | ~io.enable
    io.state := stateFSM.io.output.data(0).asSInt

    // Screwiness with "switch" signals until we have better fsm test cases
    io.childAck.last := io.doneIn.last

    doneReg.io.input.set := io.doneCondition & io.enable
    doneReg.io.input.reset := ~io.enable
    doneReg.io.input.asyn_reset := false.B
    io.ctrInc := io.enable & ~doneReg.io.output.data & ~io.doneCondition & ~io.ctrDone & io.flow
    io.datapathEn := io.enable & ~doneReg.io.output.data & ~io.doneCondition & io.flow
    io.done := risingEdge(getRetimed(doneReg.io.output.data | (io.doneCondition & io.enable & io.flow), p.latency + 1, true.B))

  }
}
