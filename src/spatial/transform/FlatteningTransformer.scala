package spatial.transform

import argon._
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.node._
import spatial.data._
import spatial.util._
import spatial.traversal.AccelTraversal

/** Converts inner pipes that contain switches into innerpipes with enabled accesses.
  * Also squashes outer unit pipes that contain only one child
  */
case class FlatteningTransformer(IR: State) extends MutateTransformer with AccelTraversal {

  private var flattenSwitch: Boolean = false

  private def transformCtrl[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case ctrl: Control[_] =>
      ctrl.bodies.foreach{body =>
        // Pre-transform all blocks which correspond to inner stages in this controller
        if (lhs.isInnerControl || body.isInnerStage) {
          body.blocks.foreach{case (_,block) =>
            // Transform the block and register the block substitution
            val saveFlatten = flattenSwitch
            flattenSwitch = true
            val block2 = f(block)
            register(block -> block2)
            flattenSwitch = saveFlatten
          }
        }
      }
      // Mirror the controller symbol (with any block substitutions in place)
      super.transform(lhs,rhs)
    case _ => super.transform(lhs,rhs)
  }

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case op@Switch(F(sels),_) if flattenSwitch =>
      val vals = op.cases.map{cas => inlineBlock(cas.body).unbox }

      Type[A] match {
        case Bits(b) =>
          implicit val bA: Bits[A] = b
          if (sels.length == 2) mux[A](sels.head, vals.head, vals.last)
          else oneHotMux[A](sels, vals)

        case _:Void => void.asInstanceOf[Sym[A]]
        case _      => Switch.op_switch[A](sels,vals.map{v => () => v })
      }

    case _:Switch[_]  => super.transform(lhs,rhs)
    case _:AccelScope => inAccel{ transformCtrl(lhs,rhs) }
    case _:Control[_] => transformCtrl(lhs,rhs)

    case _ => super.transform(lhs,rhs)
  }

}


