package nova.traversal.transform

import core._
import core.transform.MutateTransformer
import nova.data._
import nova.util._

import spatial.lang._
import spatial.node._
import pir.lang._
import pir.node._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

case class GlobalAllocation(IR: State) extends MutateTransformer {
  private val memoryPMUs = mutable.HashMap[Sym[_],VPMU]()
  private val controlData = mutable.HashMap[Sym[_],CtrlData]()

  def getPMU(mem: Sym[_]): VPMU = memoryPMUs.getOrElse(mem, throw new Exception(s"No PMU has been created for $mem"))

  case class CtrlData(var syms: ArrayBuffer[Sym[_]], var iters: ArrayBuffer[Seq[I32]])
  object CtrlData {
    def empty = CtrlData(ArrayBuffer.empty, ArrayBuffer.empty)
  }

  def addOuter(s: Sym[_], ctrl: Sym[_]): Unit = {
    if (!controlData.contains(ctrl)) controlData += ctrl -> CtrlData.empty
    controlData(ctrl).syms += s
  }
  def prependOuter(data: CtrlData, ctrl: Sym[_]): Unit = {
    if (!controlData.contains(ctrl)) controlData += ctrl -> CtrlData.empty
    controlData(ctrl).syms = data.syms ++ controlData(ctrl).syms
    controlData(ctrl).iters ++= data.iters
  }

  protected def makePMU[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = {
    val mem = super.transform(lhs,rhs)
    val pmu = VPMU(Seq(mem))
    memoryPMUs += mem -> pmu
    stage(pmu)
    mem
  }

  override def preprocess[S](block: Block[S]): Block[S] = {
    memoryPMUs.clear()
    controlData.clear()
    super.preprocess(block)
  }

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case SRAMNew(_) => makePMU(lhs,rhs)
    case FIFONew(_) => makePMU(lhs,rhs)
    case LIFONew(_) => makePMU(lhs,rhs)

    case c: BlackBox => super.transform(lhs,rhs)
    case c: VPCU => super.transform(lhs,rhs)
    case c: VPMU => super.transform(lhs,rhs)

    case c: Control[_] if isInnerControl(lhs) => innerBlock(lhs,c.blocks.head,c.iters)
    case c: Control[_] if isOuterControl(lhs) => outerBlock(lhs,c.blocks.head,c.iters)

    case _ => super.transform(lhs,rhs)
  }

  protected def outerBlock[A:Type](lhs: Sym[A], block: Block[_], iters: Seq[I32]): Sym[A] = {
    val usedSyms = mutable.HashMap[Sym[_],Set[Sym[_]]]()
    def addUsed(x: Sym[_], using: Set[Sym[_]]): Unit = usedSyms(x) = usedSyms.getOrElse(x, Set.empty) ++ using
    var children = mutable.HashSet[Sym[_]]()

    block.stms.reverseIterator.foreach{
      case MemAlloc(_)  =>
      case Bus(_)     =>
      case Control(s) =>
        children += s
        s.inputs.foreach{in => addUsed(in, Set(s)) }

      case s =>
        if (usedSyms.contains(s)) s.inputs.foreach{in => addUsed(in,usedSyms(s)) }
    }

    // Stage the inner block, preserving only memory allocations and other controllers
    val blk: Block[Void] = stageBlock {
      block.stms.foreach {
        case MemAlloc(s)  => visit(s)
        case Control(s) => visit(s)
        case Bus(s)     => visit(s)
        case s if usedSyms.contains(s) => usedSyms(s).foreach{c => addOuter(s, c) }
        case s => dbgs(s"Dropping ${stm(s)}")
      }
      implicit val ctx: SrcCtx = block.result.src
      void
    }
    val psu = VSwitch(blk, Seq(iters))
    stage(psu).asInstanceOf[Sym[A]]
  }

  protected def innerBlock[A:Type](lhs: Sym[A], block: Block[_], iters: Seq[I32]): Sym[A] = {
    val parents = symParents(lhs)
    val edata = parents.map{p => controlData.getOrElse(p, CtrlData.empty) }
    val cdata = controlData.getOrElse(lhs, CtrlData.empty)
    val external = edata.flatMap(_.syms)
    val outer = cdata.syms
    val scope = block.stms
    val allIters = edata.flatMap(_.iters) ++ cdata.iters :+ iters

    val wrSyms = mutable.HashMap[Sym[_],Set[Sym[_]]]()
    val rdSyms = mutable.HashMap[Sym[_],Set[Sym[_]]]()
    val dataSyms = mutable.HashSet[Any]()
    val memAllocs = mutable.HashSet[Sym[_]]()

    val outputs = mutable.HashSet[Sym[_]]()

    def addWr(x: Sym[_], mem: Set[Sym[_]]): Unit = wrSyms(x) = wrSyms.getOrElse(x, Set.empty) ++ mem
    def addRd(x: Sym[_], mem: Set[Sym[_]]): Unit = rdSyms(x) = rdSyms.getOrElse(x, Set.empty) ++ mem
    def isWr(x: Sym[_], mem: Sym[_]): Boolean = wrSyms.contains(x) && wrSyms(x).contains(mem)
    def isRd(x: Sym[_], mem: Sym[_]): Boolean = rdSyms.contains(x) && rdSyms(x).contains(mem)
    def isDatapath(x: Sym[_]): Boolean = {
      dataSyms.contains(x) || (!wrSyms.contains(x) && !rdSyms.contains(x) && !memAllocs.contains(x))
    }
    def isRemoteMemory(mem: Sym[_]): Boolean = !mem.isReg || {
      accessesOf(mem).exists{access => parentOf(access).sym != lhs }
    }
    scope.reverseIterator.foreach{
      case s @ Reader(mem,addr,_) =>
        dataSyms += s
        if (isRemoteMemory(mem)) {
          rdSyms += s -> Set(mem)
          // Push read address computation to PMU if this is the only reader
          if (readersOf(mem).size == 1) addr.foreach{a => addRd(a, Set(mem)) }
        }

      case s @ Writer(mem,data,addr,_) =>
        dataSyms += s
        dataSyms += data
        if (isRemoteMemory(mem)) {
          wrSyms += s -> Set(mem)
          // Push write address to PMU if this is the only writer
          if (writersOf(mem).size == 1) addr.foreach{a => addWr(a, Set(mem)) }
        }

      case MemAlloc(s) if !s.isReg => memAllocs += s

      case s =>
        if (wrSyms.contains(s)) s.inputs.foreach{in => addWr(in,wrSyms(s)) }
        if (rdSyms.contains(s)) s.inputs.foreach{in => addRd(in,rdSyms(s)) }
        if (dataSyms.contains(s)) dataSyms ++= s.inputs
    }
    val wrMems: Seq[Sym[_]] = wrSyms.values.flatten.toSeq.distinct
    val rdMems: Seq[Sym[_]] = rdSyms.values.flatten.toSeq.distinct

    // Add statements to a PCU
    // For inner statements, this is always a PCU
    def update(x: Sym[_]): Unit = x match {
      case Reader(mem,addr,_) => visit(x)
      case  _ => visit(x)
    }

    // Copy statements from another block
    // For inner statements, this is always a PMU
    def clone(x: Sym[_]): Unit = x match {
      case Op(CounterChainNew(ctrs)) => stage(CounterChainCopy(f(ctrs)))
      case _ => mirrorSym(x)
    }

    def blk(copy: Sym[_] => Unit)(use: Sym[_] => Boolean): Block[Void] = stageBlock {
      // Copy parent controllers' dependencies into the controller
      external.foreach{s => clone(s) }
      // Push this controller's direct dependencies into the controller
      outer.foreach{s => copy(s) }
      // Add statements to the data/address path in this block
      scope.foreach{s => if (use(s)) copy(s) }
      void
    }

    dbgs(s"${stm(lhs)}")
    dbgs(s"Iters: $allIters")
    dbgs(s"Outer: ")
    outer.foreach{s => dbgs(s"  ${stm(s)}")}
    dbgs(s"Scope: ")
    scope.foreach{s => dbgs(s"  ${stm(s)} [datapath:${isDatapath(s)}]")}

    val wrs = wrMems.map{sram => blk(clone){s => isWr(s,sram)} }
    val rds = rdMems.map{sram => blk(clone){s => isRd(s,sram)} }
    val datapath = blk(update){s => isDatapath(s) }

    val pcu = VPCU(datapath,allIters)
    memAllocs.foreach{mem => visit(mem) }
    wrMems.zip(wrs).foreach{case (mem,blk) => getPMU(f(mem)).setWr(blk,allIters) }
    rdMems.zip(rds).foreach{case (mem,blk) => getPMU(f(mem)).setRd(blk,allIters) }

    stage(pcu).asInstanceOf[Sym[A]]
  }


}
