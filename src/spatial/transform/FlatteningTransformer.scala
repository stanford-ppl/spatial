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
case class FlatteningTransformer(IR: State) extends MutateTransformer with AccelTraversal {

  private var flattenSwitch: Boolean = false
  private var deleteChild: Boolean = false

  // private def liftBody[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {

  // }

  private def transformCtrl[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case ctrl: Control[_] if (lhs.isInnerControl || ctrl.bodies.exists(_.isInnerStage)) =>
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

    case ctrl: Control[_] if (lhs.isOuterControl && lhs.children.length == 1) => 
      val child = lhs.children.head
      // If child is UnitPipe, inline its contents with parent
      val childInputs = child.s.get.op.get.inputs
      val parentNeeded = lhs.op.get.blocks.exists{block => block.stms.exists(childInputs.contains)}
      if (child.isUnitPipe & !parentNeeded && !lhs.isStreamControl && !child.isStreamControl){
        ctrl.bodies.foreach{body => 
          body.blocks.foreach{case (_,block) => 
            val saveMerge = deleteChild
            deleteChild = true
            val block2 = f(block)
            register(block -> block2)
            deleteChild = saveMerge
          }
        }
        super.transform(lhs,rhs)
      } 
      // If parent is UnitPipe, delete it
      else if (lhs.isUnitPipe & !parentNeeded && !lhs.isStreamControl && !child.isStreamControl) {
        ctrl.bodies.foreach{body => 
          body.blocks.foreach{case (_,block) => 
            val block2 = f(block)
            register(block -> block2)
          }
        }
        dbgs(s"Deleting $lhs and inlining its child directly")
        void.asInstanceOf[Sym[A]]
      }
      else super.transform(lhs,rhs)

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

    case ctrl: Control[_] if deleteChild =>
      dbgs(s"Deleting $lhs and inlining body with parent")
      if (!(lhs.children.length == 1 && lhs.children.head.isUnitPipe && !lhs.isStreamControl)) deleteChild = false
      ctrl.bodies.foreach{body => 
        body.blocks.foreach{case (_,block) => 
          inlineBlock(block)
        }
      }
      if (!(lhs.children.length == 1 && lhs.children.head.isUnitPipe && !lhs.isStreamControl)) deleteChild = true
      void.asInstanceOf[Sym[A]]

    case _:Switch[_]  => super.transform(lhs,rhs)
    case _:AccelScope => inAccel{ transformCtrl(lhs,rhs) }
    case _:Control[_] => transformCtrl(lhs,rhs)

    case _ => super.transform(lhs,rhs)
  }

}


