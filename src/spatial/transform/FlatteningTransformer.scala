package spatial.transform

import argon._
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.traversal.AccelTraversal
import spatial.util.spatialConfig
import scala.collection.mutable.{Set,HashMap}

/** Converts inner pipes that contain switches into innerpipes with enabled accesses.
  * Also squashes outer unit pipes that contain only one child
  */
case class FlatteningTransformer(IR: State) extends MutateTransformer with AccelTraversal {

  private var flattenSwitch: Boolean = false
  private var deleteChild: Boolean = false

  // private def liftBody[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {

  // }

  private def precomputeBundling(lhs: Sym[_]): HashMap[Int, Seq[Sym[_]]] = {
    // Predetermine bundling
    // TODO: Read - Read dependencies are actually ok
    dbgs(s"Attempt to bundle children of $lhs (${lhs.children.map(_.s.get)})")
    val bundling: HashMap[Int,Seq[Sym[_]]] = HashMap((0 -> Seq(lhs.children.head.s.get)))
    val prevMems: Set[Sym[_]] = Set()
    (lhs.children.head.s.get.nestedWrittenMems.toSet ++ lhs.children.head.s.get.nestedReadMems.toSet ++ lhs.children.head.s.get.nestedTransientReadMems.toSet).foreach(prevMems += _)
    lhs.children.drop(1).foreach{case cc => 
      val c = cc.s.get
      val activeMems = c.nestedWrittenMems.toSet ++ c.nestedReadMems.toSet ++ c.nestedTransientReadMems
      if (prevMems.intersect(activeMems).nonEmpty) {
        dbgs(s"Conflict between $prevMems and $activeMems! Placing $c in group ${bundling.toList.size}")
        prevMems.clear()
        bundling += (bundling.toList.size -> Seq(c))
      } else {
        dbgs(s"No dependencies detected between next child deps (${activeMems}) and prev bundle deps (${prevMems}).  Grouping $c in group ${bundling.toList.size-1}")
        bundling += ((bundling.toList.size - 1) -> (bundling(bundling.toList.size - 1) ++ Seq(c)))
      }
      activeMems.foreach{x => prevMems += x}
    }
    bundling.toList.sortBy(_._1).map{case (grp, children) => 
      dbgs(s"Bundled child $grp contains $children")
    }
    bundling
  }

  private def applyBundling(bundling: HashMap[Int, Seq[Sym[_]]], block: Block[Void]): Block[Void] = {
    var curGrp = 0
    val bundledStms: Set[Sym[_]] = Set()
    stageBlock{
      block.stms.foreach{ stm => 
        // Add stm to roster if it is a ctrl to be bundled
        if (bundling(curGrp).contains(stm)) bundledStms += stm
        else {
          visit(stm)
        }
        // If we are at final controller in bundle, visit each sym in roster
        if (stm == bundling(curGrp).last) {
          // Wrap in Parallel if bundled, otherwise just plain visit
          if (bundling(curGrp).size > 1) {
            stage(ParallelPipe(scala.collection.immutable.Set[Bit](), stageBlock(bundledStms.foreach(visit))))
          } else bundledStms.foreach(visit)
          // Increment to next group and clear bundleStms
          bundledStms.clear()
          curGrp += 1
        }
      }
    }

  }

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
            block.stms.foreach(visit)
            register(block -> block2)
          }
        }
        dbgs(s"Deleting $lhs and inlining its child directly")
        void.asInstanceOf[Sym[A]]
      }
      else super.transform(lhs,rhs)

    case ctrl@UnrolledForeach(ens, cchain, blk, is, vs, stopWhen) if (spatialConfig.enableParallelBinding && lhs.isOuterControl && (lhs.isPipeControl || lhs.isSeqControl) && lhs.children.length > 1) => 
      val bundling = precomputeBundling(lhs)
      if (bundling.toList.size < lhs.children.size) {
        stageWithFlow(UnrolledForeach(ens, cchain, applyBundling(bundling, blk), is, vs, stopWhen)){lhs2 => transferData(lhs, lhs2)}  
      } else super.transform(lhs,rhs)
    case ctrl@UnrolledReduce(ens, cchain, blk, is, vs, stopWhen) if (spatialConfig.enableParallelBinding && lhs.isOuterControl && (lhs.isPipeControl || lhs.isSeqControl) && lhs.children.length > 1) => 
      val bundling = precomputeBundling(lhs)
      if (bundling.toList.size < lhs.children.size) {
        stageWithFlow(UnrolledReduce(ens, cchain, applyBundling(bundling, blk), is, vs, stopWhen)){lhs2 => transferData(lhs, lhs2)}  
      } else super.transform(lhs,rhs)

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
      if (!(lhs.children.length == 1 && lhs.children.head.isUnitPipe && !lhs.children.head.isStreamControl && !lhs.isStreamControl)) deleteChild = false
      ctrl.bodies.foreach{body => 
        body.blocks.foreach{case (_,block) => 
          inlineBlock(block)
        }
      }
      if (!(lhs.children.length == 1 && lhs.children.head.isUnitPipe && !lhs.children.head.isStreamControl && !lhs.isStreamControl)) deleteChild = true
      void.asInstanceOf[Sym[A]]

    case _:Switch[_]  => super.transform(lhs,rhs)
    case _:AccelScope => inAccel{ transformCtrl(lhs,rhs) }
    case _:Control[_] => transformCtrl(lhs,rhs)

    case _ => super.transform(lhs,rhs)
  }

}


