package spatial.transform

import argon._
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.metadata.retiming._
import spatial.traversal.AccelTraversal

case class AccumTransformer(IR: State) extends MutateTransformer with AccelTraversal {

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case AccelScope(_) => inHw{ transformControl(lhs,rhs) }
    case _ => transformControl(lhs,rhs)
  }

  def transformControl[A:Type](lhs: Sym[A], rhs: Op[A]): Sym[A] = rhs match {
    case ctrl: Control[_] if !(lhs.isSwitch && lhs.isInnerControl) =>
      ctrl.bodies.foreach{body =>
        if (body.isInnerStage || lhs.isInnerControl) {
          body.blocks.foreach{case (_, block) =>
            register(block -> optimizeAccumulators(block))
          }
        }
      }
      super.transform(lhs,rhs)
    case _ => super.transform(lhs,rhs)
  }


  def optimizeAccumulators[R](block: Block[R]): Block[R] = {
    val stms = block.stms
    val cycleStms = stms.filter(_.isInCycle).filter(_.reduceCycle.shouldSpecialize).toSet



  }

}
