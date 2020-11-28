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
  * Kernel class is a container for a state machine (SM) module, a concrete kernel signalsFromParent and signalsToParent IO ports (not the Module itself),
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
  val sigsIn = Wire(new InputKernelSignals(nMyChildren, ctrcopies, ctrPars, ctrWidths)); val mySignalsIn = sigsIn; mySignalsIn := DontCare
  val sigsOut = Wire(new OutputKernelSignals(nMyChildren, ctrcopies)); val mySignalsOut = sigsOut; mySignalsOut := DontCare
  def done = mySignalsIn.done
  def smEnableOuts = mySignalsIn.smEnableOuts
  def smEnableOut(i: Int) = mySignalsIn.smEnableOuts(i)
  def mask = mySignalsIn.mask
  def baseEn = mySignalsIn.baseEn
  def iiDone = mySignalsIn.iiDone
  def iiIssue = mySignalsIn.iiIssue
  def backpressure = mySignalsIn.backpressure
  def forwardpressure = mySignalsIn.forwardpressure
  def datapathEn = mySignalsIn.datapathEn
  val resetChildren = Wire(Bool()); resetChildren := DontCare
  val en = Wire(Bool()); en := DontCare
  val resetMe = Wire(Bool()); resetMe := DontCare
  val parentAck = Wire(Bool()); parentAck := DontCare
  val sm: GeneralControl
  val iiCtr: IICounter

  // Configure relative to parent Kernel (parent = signalsFromParent/Out, me = io.mySignalsIn/Out)
  def configure(n: String, signalsFromParent: Option[InputKernelSignals], signalsToParent: Option[OutputKernelSignals], isSwitchCase: Boolean = false): Unit = {
    // Feed my counter values to my kernel
    cchain.zip(mySignalsIn.cchainOutputs).foreach{case (cc, sc) => sc := cc.output}
    // Feed my one-hot select vector to my kernel (for Switch controllers)
    mySignalsIn.smSelectsOut.zip(sm.io.selectsOut).foreach{case (si, sm) => si := sm}

    mySignalsIn.ctrDone := sm.io.ctrDone
    mySignalsIn.smState := sm.io.state
    sm.io.nextState := mySignalsOut.smNextState
    sm.io.initState := mySignalsOut.smInitState
    sm.io.doneCondition := mySignalsOut.smDoneCondition
    mySignalsIn.smEnableOuts.zip(sm.io.enableOut).foreach{case (l,r) => l := r}
    mySignalsIn.smChildAcks.zip(sm.io.childAck).foreach{case (l,r) => l := r}
    sm.io.doneIn.zip(mySignalsOut.smDoneIn).foreach{case (sm, i) => sm := i}
    sm.io.maskIn.zip(mySignalsOut.smMaskIn).foreach{case (sm, i) => sm := i}
    sm.io.backpressure := backpressure
    sm.io.rst := resetMe
    done := sm.io.done
    mySignalsIn.break := sm.io.break
    en := baseEn & forwardpressure
    if (!isSwitchCase) signalsFromParent.foreach{si => baseEn := si.smEnableOuts(childId).D(1) && ~done.D(1)}
    parentAck := {if (signalsFromParent.isDefined) signalsFromParent.get.smChildAcks(childId) else false.B}
    sm.io.enable := en
    resetChildren := sm.io.ctrRst
    sm.io.parentAck := parentAck
    mySignalsIn.suggestName(n + "_mySignalsIn")
    mySignalsOut.suggestName(n + "_mySignalsOut")
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
    signalsToParent.foreach{so => so.smDoneIn(childId) := done; so.smMaskIn(childId) := mask}
    datapathEn := sm.io.datapathEn & mask & {if (cchain.isEmpty) true.B else ~sm.io.ctrDone} 
    iiCtr.io.input.enable := datapathEn; iiCtr.io.input.reset := sm.io.rst | sm.io.parentAck; iiDone := iiCtr.io.output.done | ~mask; iiIssue := iiCtr.io.output.issue | ~mask
    cchain.foreach { c => c.input.enable := sm.io.ctrInc & iiDone & forwardpressure; c.input.reset := resetChildren }
    if (sm.p.sched == Streaming && sm.isInstanceOf[fringe.templates.OuterControl]) {
      // If we are a streaming outer controller, do the following sleight of hand
      sm.io.ctrCopyDone.zip(mySignalsOut.smCtrCopyDone).foreach{case (mySM, parentSM) => mySM := parentSM}
      if (cchain.nonEmpty) {
        mySignalsOut.smCtrCopyDone.zip(cchain).foreach{case (ccd, cc) => ccd := cc.output.done}
        cchain.zip(mySignalsOut.cchainEnable).foreach{case (cc, ce) => cc.input.enable := ce }
      }
    }
    // Enable parent cchain when I am all done
    if      (parent.isDefined && parent.get.sm.p.sched == Streaming && parent.get.cchain.nonEmpty) {signalsToParent.foreach{toParent => toParent.cchainEnable(childId) := done}}
    // Otherwise inform parent I am totally done when I am done
    else if (parent.isDefined && parent.get.sm.p.sched == Streaming) {{signalsToParent.foreach{toParent => toParent.smCtrCopyDone(childId) := done }}}
  }
}