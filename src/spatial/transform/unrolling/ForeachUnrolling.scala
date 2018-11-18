package spatial.transform.unrolling

import argon._
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import utils.tags.instrument

trait ForeachUnrolling extends UnrollingBase {

  override def unrollCtrl[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[_] = rhs match {
    case OpForeach(ens, cchain, func, iters, stopWhen) =>
      val stopWhen2 = if (stopWhen.isDefined) Some(memories((stopWhen.get,0)).asInstanceOf[Reg[Bit]]) else stopWhen
      if (cchain.willFullyUnroll) fullyUnrollForeach(lhs, f(ens), f(cchain), func, iters, stopWhen2)
      else partiallyUnrollForeach(lhs, f(ens), f(cchain), func, iters, stopWhen2)

    case _ => super.unrollCtrl(lhs,rhs)
  }

  def fullyUnrollForeach(
    lhs:    Sym[_],
    ens:    Set[Bit],
    cchain: CounterChain,
    func:   Block[Void],
    iters:  Seq[I32],
    stopWhen: Option[Reg[Bit]]
  )(implicit ctx: SrcCtx): Void = {
    dbgs(s"Fully unrolling foreach $lhs")
    val lanes = FullUnroller(s"$lhs", cchain, iters, lhs.isInnerControl)
    val blk   = inLanes(lanes){ substituteBlock(func) }
    val lhs2  = stageWithFlow(UnitPipe(enables ++ ens, blk)){lhs2 => transferData(lhs,lhs2) }
    dbgs(s"Created unit pipe ${stm(lhs2)}")
    lhs2
  }

  def partiallyUnrollForeach (
    lhs:    Sym[_],
    ens:    Set[Bit],
    cchain: CounterChain,
    func:   Block[Void],
    iters:  Seq[I32],
    stopWhen: Option[Reg[Bit]]
  )(implicit ctx: SrcCtx): Sym[_] = {
    dbgs(s"Unrolling foreach $lhs")
    val lanes = PartialUnroller(s"$lhs", cchain, iters, lhs.isInnerControl)
    val is    = lanes.indices
    val vs    = lanes.indexValids
    val blk   = inLanes(lanes){ substituteBlock(func) }
    val lhs2  = stageWithFlow(UnrolledForeach(enables ++ ens, cchain, blk, is, vs, stopWhen)){lhs2 => transferData(lhs,lhs2) }
    dbgs(s"Created foreach ${stm(lhs2)}")
    lhs2
  }

}
