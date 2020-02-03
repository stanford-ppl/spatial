package spatial.transform

import argon._
import argon.node._
import argon.transform.MutateTransformer

import spatial.lang._
import spatial.node._
import spatial.util.shouldMotionFromConditional
import spatial.traversal.AccelTraversal
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.blackbox._

import utils.math.log2Up

import emul.FixedPoint

case class CounterBitwidthTransformer(IR: State) extends MutateTransformer
  with AccelTraversal {

  /** Calculate the least bitwidth required for an integer. */
  private def getBitwidth(x: Int): Int = log2Up(x.abs)

  /** Extract the content from Const and cast it to Int. */
  private def constToInt(x: Sym[_]): Int = 
    if (x.isConst)
      x.c.get.asInstanceOf[FixedPoint].toInt
    else
      throw new Exception(s"$x is not a Const.")

  /** Create a new CounterNew object with compact bitwidth. */
  private def getOptimizedCounterNew(ctr: CounterNew[_]): CounterNew[_] = ctr match {
    case CounterNew(start, stop, step, par) =>
      // we take the largest magnitude of start and stop to decide the boundary of bitwidth
      val begin = constToInt(start)
      val end = constToInt(stop)
      val bitwidth = math.max(getBitwidth(begin), getBitwidth(end))

      // TODO: Find a better way that can map bitwidth to the exact Fix type
      if (bitwidth <= 7) {
        type T = Fix[TRUE,_8,_0]
        CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
      } else if (bitwidth <= 15) {
        type T = Fix[TRUE,_16,_0]
        CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
      } else {
        type T = Fix[TRUE,_32,_0]
        CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
      }
    case _ => ctr
  }

  /** Optimize a list of Counter. */
  private def getOptimizeCounters(ctrs: Seq[Counter[_]]): Seq[Counter[_]] = {
    ctrs.map { 
      case Op(ctr: CounterNew[_]) => stage(getOptimizeCounterNew(ctr))
    }
  }

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case AccelScope(_) => 
      inAccel { super.transform(lhs, rhs) }

    case OpForeach(ens, cchain, blk, iters, stopWhen) if inHw =>
      val newctrs = getOptimizedCounters(cchain.counters)
      val newcchain = stageWithFlow(CounterChainNew(newctrs)){ lhs2 => transferData(lhs, lhs2)}

      stageWithFlow(
        OpForeach(
          ens,
          newcchain,
          stageBlock{blk.stms.foreach(visit)},
          iters,
          stopWhen)
      ){lhs2 => transferData(lhs, lhs2)}

    case _ => 
      dbgs(s"visiting $lhs = $rhs");
      super.transform(lhs, rhs)
  }
}
