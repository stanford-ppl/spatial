package pcc.traversal.transform

import pcc.core._
import pcc.data._
import pcc.node._
import pcc.node.pir._
import pcc.lang._

import scala.collection.mutable.{HashMap,HashSet,ArrayBuffer}

case class GlobalAllocation(IR: State) extends MutateTransformer {
  override val name = "Global Allocation"

  val memoryPMUs = HashMap[Sym[_],PMU]()
  def getPMU(mem: Sym[_]): PMU = memoryPMUs.getOrElse(mem, throw new Exception(s"No PMU has been created for $mem"))

  val outerSyms = HashMap[Sym[_],ArrayBuffer[Sym[_]]]()
  def addOuter(s: Sym[_], ctrl: Sym[_]): Unit = {
    if (!outerSyms.contains(ctrl)) outerSyms += ctrl -> ArrayBuffer.empty
    outerSyms(ctrl) += s
  }

  protected def makePMU[A:Sym](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = {
    val mem = super.transform(lhs,rhs)
    val pmu = PMU(Seq(mem))
    memoryPMUs += mem -> pmu
    stage(pmu)
    mem
  }

  override def preprocess[S](block: Block[S]): Block[S] = {
    memoryPMUs.clear()
    outerSyms.clear()
    super.preprocess(block)
  }

  override def transform[A:Sym](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case SRAMNew(_) => makePMU(lhs,rhs)
    case FIFONew(_) => makePMU(lhs,rhs)
    case LIFONew(_) => makePMU(lhs,rhs)

    case UnitPipe(ens, block) if isInnerControl(lhs) => innerBlock(lhs,block,Nil)
    case OpForeach(ens,cchain,block,iters) if isInnerControl(lhs) => innerBlock(lhs,block,Seq(iters))

    case _ if isOuterControl(lhs) =>
      rhs.blocks.foreach{blk => register(blk -> outerBlock(blk)) }
      super.transform(lhs, rhs)

    case _ => super.transform(lhs,rhs)
  }

  protected def outerBlock[R](block: Block[R]): Block[R] = {
    val usedSyms = HashMap[Sym[_],Set[Sym[_]]]()
    def addUsed(x: Sym[_], using: Set[Sym[_]]): Unit = usedSyms(x) = usedSyms.getOrElse(x, Set.empty) ++ using

    block.stms.reverseIterator.foreach{
      case Memory(_)  =>
      case Control(s) => s.dataInputs.foreach{in => addUsed(in, Set(s)) }
      case s =>
        if (usedSyms.contains(s)) s.dataInputs.foreach{in => addUsed(in,usedSyms(s)) }
    }

    stageBlock {
      block.stms.foreach {
        case Memory(s) => visit(s)
        case Control(s) => visit(s)
        case s if usedSyms.contains(s) =>
          usedSyms(s).foreach {c => addOuter(s, c) }
        case s =>
          dbgs(s"Dropping ${stm(s)}")
      }
      implicit val ctx: SrcCtx = block.result.ctx
      (block.result match {case _:Void => Void.c; case s => f(s)}).asInstanceOf[Sym[R]]
    }
  }

  protected def innerBlock[A:Sym](lhs: Sym[A], block: Block[_], iters: Seq[Seq[I32]]): Sym[A] = {
    val outer = outerSyms.getOrElse(lhs, Nil)
    val scope = outer ++ block.stms

    val wrSyms = HashMap[Sym[_],Set[SRAM[_]]]()
    val rdSyms = HashMap[Sym[_],Set[SRAM[_]]]()
    val dataSyms = HashSet[Any]()
    val memAllocs = HashSet[Sym[_]]()

    def addWr(x: Sym[_], mem: Set[SRAM[_]]): Unit = wrSyms(x) = wrSyms.getOrElse(x, Set.empty) ++ mem
    def addRd(x: Sym[_], mem: Set[SRAM[_]]): Unit = rdSyms(x) = rdSyms.getOrElse(x, Set.empty) ++ mem
    def isWr(x: Sym[_], mem: SRAM[_]): Boolean = wrSyms.contains(x) && wrSyms(x).contains(mem)
    def isRd(x: Sym[_], mem: SRAM[_]): Boolean = rdSyms.contains(x) && rdSyms(x).contains(mem)
    def isDatapath(x: Sym[_]): Boolean = {
      dataSyms.contains(x) || (!wrSyms.contains(x) && !rdSyms.contains(x) && !memAllocs.contains(x))
    }

    def blk(use: Sym[_] => Boolean): Block[Void] = stageBlock {
      implicit val ctx: SrcCtx = SrcCtx.empty
      scope.foreach{ s => if (use(s)) visit(s) }
      Void.c
    }

    // TODO: Multi-config for multiple readers/writers
    scope.reverseIterator.foreach{
      case Stm(s, SRAMRead(sram,addr,_)) =>
        rdSyms += s -> Set(sram)
        addr.foreach{a => addRd(a, Set(sram)) }
        dataSyms += s // Keep the load to denote the data transfer

      case Stm(s, SRAMWrite(sram,data,addr,_)) =>
        wrSyms += s -> Set(sram)
        addr.foreach{a => addWr(a, Set(sram)) }
        dataSyms += data
        dataSyms += s // Keep the write to denote the data transfer

      case Memory(s) if !s.isReg => memAllocs += s

      case s =>
        if (wrSyms.contains(s)) s.dataInputs.foreach{in => addWr(in,wrSyms(s)) }
        if (rdSyms.contains(s)) s.dataInputs.foreach{in => addRd(in,rdSyms(s)) }
        if (dataSyms.contains(s)) dataSyms ++= s.dataInputs
    }
    val wrSRAMs: Seq[SRAM[_]] = wrSyms.values.flatten.toSeq.distinct
    val rdSRAMs: Seq[SRAM[_]] = rdSyms.values.flatten.toSeq.distinct

    val writes = wrSRAMs.map{sram => blk{s => isWr(s,sram) } }
    val reads  = rdSRAMs.map{sram => blk{s => isRd(s,sram) } }
    val datapath = blk{s => isDatapath(s) }

    val pcu = PU.compute(datapath,iters)
    memAllocs.foreach{mem => visit(mem) }
    wrSRAMs.zip(writes).foreach{case (mem,blk) => getPMU(f(mem)).writeAddr = Some(blk) }
    rdSRAMs.zip(reads).foreach{case (mem,blk) => getPMU(f(mem)).readAddr = Some(blk) }
    pcu.asInstanceOf[Sym[A]]
  }
}
