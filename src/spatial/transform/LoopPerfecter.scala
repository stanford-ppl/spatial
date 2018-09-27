package spatial.transform

import argon._
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.traversal.AccelTraversal

/** Converts inner pipes that contain switches into innerpipes with enabled accesses.
  * Also squashes outer unit pipes that contain only one child
  */
case class LoopPerfecter(IR: State) extends MutateTransformer with AccelTraversal {

  // private def transformCtrl[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {

  //   private var fusePipe = false
  //   case ctrl: Control[_] if (ctrl.isOuterControl && ctrl.children.forall(_.s.get.isInnerControl)) => 
  //     ctrl.bodies.foreach{body => 
  //       body.blocks.foreach{case (_,block) => 
  //           val block2 = f(block)
  //           register(block -> block2)
  //         }
  //       }


  //   case _ => super.transform(lhs,rhs)
  // }

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    // case _: AccelScope => inAccel{ transformCtrl(lhs,rhs)}

    // case ctrl: Control[_] if fusePipe && lhs.isInnerControl =>
    //   super.transform(lhs,rhs)
    // case ctrl: Control[_] if lhs.isUnitPipe && lhs.isInnerControl => 
    //   super.transform(lhs,rhs)

    case _ => super.transform(lhs,rhs)
  }

}


