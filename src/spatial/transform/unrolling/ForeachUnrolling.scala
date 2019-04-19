package spatial.transform.unrolling

import argon._
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.util.spatialConfig
import utils.tags.instrument

trait ForeachUnrolling extends UnrollingBase {

  override def unrollCtrl[A:Type](lhs: Sym[A], rhs: Op[A], mop: Boolean)(implicit ctx: SrcCtx): Sym[_] = rhs match {
    case OpForeach(ens, cchain, func, iters, stopWhen) =>
      val stopWhen2 = if (stopWhen.isDefined) Some(memories((stopWhen.get,0)).asInstanceOf[Reg[Bit]]) else stopWhen
      if (cchain.willFullyUnroll) fullyUnrollForeach(lhs, f(ens), f(cchain), func, iters, stopWhen2, mop)
      else partiallyUnrollForeach(lhs, f(ens), f(cchain), func, iters, stopWhen2, mop)

    case _ => super.unrollCtrl(lhs,rhs,mop)
  }

  def fullyUnrollForeach(
    lhs:    Sym[_],
    ens:    Set[Bit],
    cchain: CounterChain,
    func:   Block[Void],
    iters:  Seq[I32],
    stopWhen: Option[Reg[Bit]],
    mop: Boolean
  )(implicit ctx: SrcCtx): Void = {
    dbgs(s"Fully unrolling foreach $lhs for $cchain $iters $mop")
    val unrLanes = FullUnroller(s"$lhs", cchain, iters, lhs.isInnerControl, mop)
    val newEns = enables ++ ens
    if (mop || unrLanes.size == 1 || lhs.isInnerControl) {
      val blk   = inLanes(unrLanes){ substituteBlock(func) }
      val lhs2  = stageWithFlow(UnitPipe(newEns, blk)){lhs2 => transferData(lhs,lhs2) }
      dbgs(s"Created unit pipe ${stm(lhs2)}")
      lhs2    
    } else {
      val blocks = stageBlock {
        unrLanes.foreach{ case List(p) => 
          dbgs(s"Staging lane $p (addr ${unrLanes.parAddr(p)}) of $unrLanes")
          val cchainPart = crossSection(cchain, unrLanes.parAddr(p))
          val blk   = inLanes(unrLanes,p){ substituteBlock(func) }
          val lhs2  = stageWithFlow(UnitPipe(newEns, blk)){lhs2 => transferData(lhs,lhs2) }
        }
      }
      val lhs3 = stage(ParallelPipe(newEns, blocks))
      lhs3       
    }
  }

  def partiallyUnrollForeach (
    lhs:    Sym[_],
    ens:    Set[Bit],
    cchain: CounterChain,
    func:   Block[Void],
    iters:  Seq[I32],
    stopWhen: Option[Reg[Bit]],
    mop: Boolean
  )(implicit ctx: SrcCtx): Sym[_] = {
    dbgs(s"Unrolling foreach $lhs")
    val unrLanes = PartialUnroller(s"$lhs", cchain, iters, lhs.isInnerControl, mop) 
    val is    = unrLanes.indices
    val vs    = unrLanes.indexValids
    val newEns = enables ++ ens
    if (mop || unrLanes.size == 1 || lhs.isInnerControl) {
      val blk   = inLanes(unrLanes){ substituteBlock(func) }
      val lhs2  = stageWithFlow(UnrolledForeach(newEns, cchain, blk, is, vs, stopWhen)){lhs2 => transferData(lhs,lhs2) }
      dbgs(s"Created foreach ${stm(lhs2)}")
      lhs2      
    } else {
      val blocks = stageBlock { 
        unrLanes.foreach{ case List(p) => 
          val i = is(p).map(List(_))
          val v = vs(p).map(List(_))
          dbgs(s"Staging lane $p (addr ${unrLanes.parAddr(p)}) of $unrLanes (is = $i, vs = $v)")
          val cchainPart = crossSection(cchain, unrLanes.parAddr(p))
          val blk   = inLanes(unrLanes,p){ substituteBlock(func) }
          val lhs2  = stageWithFlow(UnrolledForeach(newEns, cchainPart, blk, i, v, stopWhen)){lhs2 => transferData(lhs,lhs2) }
        }
      }
      val lhs3 = stage(ParallelPipe(newEns, blocks))
      lhs3       
    }
  }

}
