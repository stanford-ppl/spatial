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

case class CounterBitwidthTransformer(IR: State) extends MutateTransformer
  with AccelTraversal {

  private def optimizeCtrs(ctrs: Seq[Sym[_]]): Seq[Sym[_]] = {
    ctrs.map { 
      case Op(CounterNew(start, stop, step, par)) =>
        println(start, stop, step, par)
    }
    ctrs
  }

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case AccelScope(_) => 
      inAccel { super.transform(lhs, rhs) }

    case OpForeach(ens, cchain, block, iters, stopWhen) if inHw =>
      println(ens, cchain, block, iters, stopWhen) 
      println(cchain.node)
      optimizeCtrs(cchain.counters)
      super.transform(lhs, rhs)

    case _ => 
      // println(lhs, rhs)
      dbgs(s"visiting $lhs = $rhs");
      super.transform(lhs, rhs)
  }
}
