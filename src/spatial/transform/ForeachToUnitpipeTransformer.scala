package spatial.transform

import argon._
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.metadata.control._
import spatial.node._
import spatial.traversal.AccelTraversal

/** Converts single-iteration Foreach controllers to Unitpipes
  */
case class ForeachToUnitpipeTransformer(IR: State) extends MutateTransformer with AccelTraversal {

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = {
    dbgs(s"Visiting: $lhs = $rhs")
    (rhs match {
      case AccelScope(_) => inAccel {
        super.transform(lhs, rhs)
      }

      case OpForeach(ens, cchain, block, iters, stopWhen) if inHw && cchain.isStatic && (cchain.approxIters == 1) =>
        dbgs(s"Transforming ${lhs} = ${rhs}")
        iters foreach {
          iter =>
            dbgs(s"Replacing: $iter -> ${iter.ctrStart}")
            register(iter -> iter.ctrStart)
        }
        stageWithFlow(UnitPipe(f(ens), super.visitBlock(block), f(stopWhen))) { lhs2 => transferData(lhs, lhs2) }

      case _ => super.transform(lhs, rhs)
    }).asInstanceOf[Sym[A]]
  }

}


