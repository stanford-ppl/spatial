package fringe.SpatialBlocks

import fringe._
import fringe.templates.memory._
import fringe.templates._
import fringe.Ledger._
import fringe.utils._
import fringe.utils.implicits._
import fringe.templates.math._
import fringe.templates.counters._
import fringe.templates.vector._
import fringe.templates.memory._
import fringe.templates.memory.implicits._
import fringe.templates.retiming._
import emul.ResidualGenerator._
import chisel3._
import chisel3.util._
import scala.collection.immutable._

class CtrObject(
  val start: Either[Option[Int], FixedPoint],
  val stop: Either[Option[Int], FixedPoint],
  val step: Either[Option[Int], FixedPoint],
  val par: Int,
  val width: Int,
  val isForever: Boolean
){
  def fixedStart: Option[Int] = start match {case Left(x) => x; case Right(x) => None}
  def fixedStop: Option[Int] = stop match {case Left(x) => x; case Right(x) => None}
  def fixedStep: Option[Int] = step match {case Left(x) => x; case Right(x) => None}
}

class CChainObject(
  val ctrs: List[CtrObject],
  val name: String
){
  val cchain = Module(new CounterChain(ctrs.map(_.par), ctrs.map(_.fixedStart), ctrs.map(_.fixedStop), ctrs.map(_.fixedStep), 
                     ctrs.map(_.isForever), ctrs.map(_.width), myName = name))
  cchain.io <> DontCare
  cchain.io.setup.stops.zip(ctrs.map(_.stop)).foreach{case (port,Right(stop)) => port := stop.r.asSInt; case (_,_) => }
  cchain.io.setup.strides.zip(ctrs.map(_.step)).foreach{case (port,Right(stride)) => port := stride.r.asSInt; case (_,_) => }
  cchain.io.setup.starts.zip(ctrs.map(_.start)).foreach{case (port,Right(start)) => port := start.r.asSInt; case (_,_) => }
  cchain.io.setup.saturate := true.B
}


class InputKernelSignals(val depth: Int, val ctrcopies: Int, val ctrPars: List[Int], val ctrWidths: List[Int]) extends Bundle{ // From outside to inside kernel module
  val done = Bool()              // my sm -> parent sm + insides
  val mask = Bool()              // my cchain -> parent sm + insides
  val iiDone = Bool()            // my iiCtr -> my cchain + insides
  val iiIssue = Bool()
  val ctrDone = Bool()           // my sm -> my cchain + insides
  val backpressure = Bool()      // parent kernel -> my insides
  val forwardpressure = Bool()   // parent kernel -> my insides
  val datapathEn = Bool()        // my sm -> insides
  val baseEn = Bool()
  val break = Bool()        
  val smState = SInt(32.W)        
  val smEnableOuts = Vec(depth, Bool())
  val smSelectsOut = Vec(depth, Bool())
  val smChildAcks = Vec(depth, Bool())
  val cchainOutputs = Vec(ctrcopies, new CChainOutput(ctrPars, ctrWidths))
}
class OutputKernelSignals(val depth: Int, val ctrcopies: Int) extends Bundle{ // From inside to outside kernel module
  val smDoneIn = Vec(depth, Bool())
  val smMaskIn = Vec(depth, Bool())
  val smNextState = SInt(32.W)
  val smInitState = SInt(32.W)
  val smDoneCondition = Bool()
  val cchainEnable = Vec(ctrcopies, Bool())
  val smCtrCopyDone = Vec(ctrcopies, Bool())
}

/**
  * Kernel class is a container for a state machine (SM) module, a concrete kernel ifaceSigsIn and ifaceSigsOut IO ports (not the Module itself),
  * and optional counter chains (cchain).  If there are 0 cchains, this is a Unit Pipe.  If there are as many cchains as children (>=2),
  * this is a Stream controller with counter duplication for each child.  Otherwise, there is just a single cchain for the majority of cases.
  *
  * The configure() method is called after constructing a Kernel and wires up all of the control signals between the SM, concrete kernel,
  * cchains, and parent/child relationships.
  *                ____            _________________
  *               | SM |  <---->  |                 |
  *                ````           | Concrete Kernel |
  *                ________       |                 |
  *               | CCHAIN | <--> |_________________|
  *               `````````
  *
  * @param parent
  * @param cchain
  * @param childId
  * @param nMyChildren
  * @param ctrcopies
  * @param ctrPars
  * @param ctrWidths
  */
abstract class Kernel(val parent: Option[Kernel], val cchain: List[CounterChainInterface], val childId: Int, val nMyChildren: Int, ctrcopies: Int, ctrPars: List[Int], ctrWidths: List[Int]) {
  val sigsIn = Wire(new InputKernelSignals(nMyChildren, ctrcopies, ctrPars, ctrWidths)); sigsIn := DontCare
  val sigsOut = Wire(new OutputKernelSignals(nMyChildren, ctrcopies)); sigsOut := DontCare
  def done = sigsIn.done
  def smEnableOuts = sigsIn.smEnableOuts
  def smEnableOut(i: Int) = sigsIn.smEnableOuts(i)
  def mask = sigsIn.mask
  def baseEn = sigsIn.baseEn
  def iiDone = sigsIn.iiDone
  def iiIssue = sigsIn.iiIssue
  def backpressure = sigsIn.backpressure
  def forwardpressure = sigsIn.forwardpressure
  def datapathEn = sigsIn.datapathEn
  val resetChildren = Wire(Bool()); resetChildren := DontCare
  val en = Wire(Bool()); en := DontCare
  val resetMe = Wire(Bool()); resetMe := DontCare
  val parentAck = Wire(Bool()); parentAck := DontCare
  val sm: GeneralControl
  val iiCtr: IICounter

  // Configure relative to parent Kernel (parent = ifaceSigsIn/Out, me = io.sigsIn/Out)
  def configure(n: String, ifaceSigsIn: Option[InputKernelSignals], ifaceSigsOut: Option[OutputKernelSignals], isSwitchCase: Boolean = false): Unit = {
    // Feed my counter values to my kernel
    cchain.zip(sigsIn.cchainOutputs).foreach{case (cc, sc) => sc := cc.output}
    // Feed my one-hot select vector to my kernel (for Switch controllers)
    sigsIn.smSelectsOut.zip(sm.io.selectsOut).foreach{case (si, sm) => si := sm}

    sigsIn.ctrDone := sm.io.ctrDone
    sigsIn.smState := sm.io.state
    sm.io.nextState := sigsOut.smNextState
    sm.io.initState := sigsOut.smInitState
    sm.io.doneCondition := sigsOut.smDoneCondition
    sigsIn.smEnableOuts.zip(sm.io.enableOut).foreach{case (l,r) => l := r}
    sigsIn.smChildAcks.zip(sm.io.childAck).foreach{case (l,r) => l := r}
    sm.io.doneIn.zip(sigsOut.smDoneIn).foreach{case (sm, i) => sm := i}
    sm.io.maskIn.zip(sigsOut.smMaskIn).foreach{case (sm, i) => sm := i}
    cchain.zip(sigsOut.cchainEnable).foreach{case (c,e) => c.input.enable := e}
    sm.io.backpressure := backpressure
    sm.io.forwardpressure := forwardpressure
    sm.io.rst := resetMe
    done := sm.io.done
    sigsIn.break := sm.io.break
    en := baseEn & forwardpressure
    if (!isSwitchCase) ifaceSigsIn.foreach{si => baseEn := si.smEnableOuts(childId).D(1) && ~done.D(1)}
    parentAck := {if (ifaceSigsIn.isDefined) ifaceSigsIn.get.smChildAcks(childId) else false.B}
    sm.io.enable := en
    resetChildren := sm.io.ctrRst
    sm.io.parentAck := parentAck
    sigsIn.suggestName(n + "_sigsIn")
    sigsOut.suggestName(n + "_sigsOut")
    en.suggestName(n + "_en")
    done.suggestName(n + "_done")
    baseEn.suggestName(n + "_baseEn")
    iiDone.suggestName(n + "_iiDone")
    iiIssue.suggestName(n + "_iiIssue")
    backpressure.suggestName(n + "_flow")
    forwardpressure.suggestName(n + "_flow")
    mask.suggestName(n + "_mask")
    resetMe.suggestName(n + "_resetMe")
    resetChildren.suggestName(n + "_resetChildren")
    datapathEn.suggestName(n + "_datapathEn")
    ifaceSigsOut.foreach{so => so.smDoneIn(childId) := done; so.smMaskIn(childId) := mask}
    datapathEn := sm.io.datapathEn & mask & {if (cchain.isEmpty) true.B else ~sm.io.ctrDone} 
    iiCtr.io.input.enable := datapathEn; iiCtr.io.input.reset := sm.io.rst | sm.io.parentAck; iiDone := iiCtr.io.output.done | ~mask; iiIssue := iiCtr.io.output.issue | ~mask
    cchain.foreach { c => c.input.enable := sm.io.ctrInc & iiDone & forwardpressure; c.input.reset := resetChildren }
    if (sm.p.sched == Streaming) {
      sm.io.ctrCopyDone.zip(sigsOut.smCtrCopyDone).foreach{case (mySM, parentSM) => mySM := parentSM}
      if (cchain.nonEmpty) {
        sigsOut.smCtrCopyDone.zip(cchain).foreach{case (ccd, cc) => ccd := cc.output.done}
        cchain.zip(sigsOut.cchainEnable).foreach{case (cc, ce) => cc.input.enable := ce }
      }
    }
    if (parent.isDefined && parent.get.sm.p.sched == Streaming && parent.get.cchain.nonEmpty) {ifaceSigsOut.foreach{so => so.cchainEnable(childId) := done}}
    else if (parent.isDefined && parent.get.sm.p.sched == Streaming) {{ifaceSigsOut.foreach{so => so.smCtrCopyDone(childId) := done}}}
  }
}