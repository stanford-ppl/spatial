package spatial.transform

import argon._
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.traversal.AccelTraversal
import spatial.util.spatialConfig
import scala.collection.mutable.{Set,HashMap,ListBuffer}


import argon.node._
import argon.codegen.Codegen
import argon.node._
import spatial.lang._
import spatial.node._
import spatial.metadata.bounds._
import spatial.metadata.access._
import spatial.metadata.retiming._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.types._
import spatial.util.modeling.scrubNoise


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
    val prevWrMems: Set[Sym[_]] = Set()
    (lhs.children.head.s.get.nestedWrittenMems ++ lhs.children.head.s.get.nestedWrittenDRAMs ++ lhs.children.head.s.get.nestedReadMems ++ lhs.children.head.s.get.nestedReadDRAMs ++ lhs.children.head.s.get.nestedTransientReadMems).foreach(prevMems += _)
    (lhs.children.head.s.get.nestedWrittenMems ++ lhs.children.head.s.get.nestedWrittenDRAMs).foreach{x => prevWrMems += x}
    lhs.children.drop(1).zipWithIndex.foreach{case (cc,i) => 
      val c = cc.s.get
      val activeMems = c.nestedWrittenMems.toSet ++ c.nestedWrittenDRAMs.toSet ++ c.nestedReadMems.toSet ++ c.nestedReadDRAMs.toSet ++ c.nestedTransientReadMems
      val addressableMems = (activeMems ++ prevMems).filter(!_.isSingleton).filter(!_.isDRAM)
      val activeWrMems = c.nestedWrittenMems.toSet ++ c.nestedWrittenDRAMs.toSet
      val nextShouldNotBind = (Seq(c.toCtrl) ++ c.nestedChildren).exists(_.s.get.shouldNotBind) | (c.isSwitch && c.op.exists(_.R.isBits))
      val prevShouldNotBind = (Seq((lhs.children.apply(i))) ++ (lhs.children.apply(i)).nestedChildren).exists(_.s.get.shouldNotBind) | (lhs.children.apply(i).s.get.isSwitch && lhs.children.apply(i).s.get.op.exists(_.R.isBits))
      if (prevMems.intersect(activeMems).intersect(activeWrMems ++ prevWrMems ++ addressableMems).nonEmpty || nextShouldNotBind || prevShouldNotBind) {
        dbgs(s"Conflict between:")
        dbgs(s" - Prev rd/wr: $prevMems")
        dbgs(s" - Next rd/wr: $activeMems")
        dbgs(s" - Next wr + Prev wr + addressable: ${prevWrMems ++ activeWrMems ++ addressableMems}")
        dbgs(s" - (or someone should not bind next: $nextShouldNotBind prev: $prevShouldNotBind)!")
        dbgs(s"Placing $c in group ${bundling.toList.size}")
        prevMems.clear()
        prevWrMems.clear()
        bundling += (bundling.toList.size -> Seq(c))
      } else {
        dbgs(s"No dependencies detected between next child deps ($activeMems) prev bundle ($prevMems) deps.  Grouping $c in group ${bundling.toList.size-1}")
        bundling += ((bundling.toList.size - 1) -> (bundling(bundling.toList.size - 1) ++ Seq(c)))
      }
      activeMems.foreach{x => prevMems += x}
      activeWrMems.foreach{x => prevWrMems += x}
    }
    bundling.toList.sortBy(_._1).map{case (grp, children) => 
      dbgs(s"Bundled child $grp contains $children")
    }
    bundling
  }

  private def applyBundling(bundling: HashMap[Int, Seq[Sym[_]]], block: Block[Void]): Block[Void] = {
    var curGrp = 0
    val bundledStms: ListBuffer[Sym[_]] = ListBuffer()
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
    case ctrl@SwitchCase(blk) if (spatialConfig.enableParallelBinding && lhs.isOuterControl && lhs.children.length > 1) => 
      Type[A] match {
        case _:Void => 
          val bundling = precomputeBundling(lhs)
          if (bundling.toList.size < lhs.children.size) {
            stageWithFlow(SwitchCase(applyBundling(bundling,blk.asInstanceOf[Block[Void]]))){lhs2 => transferData(lhs, lhs2)}  
          } else super.transform(lhs,rhs)
        case _      => 
          super.transform(lhs,rhs)
      }

    case ctrl@AccelScope(func) => 
      val bundling = precomputeBundling(lhs)
      if (bundling.toList.size < lhs.children.size) {
        stageWithFlow(AccelScope(applyBundling(bundling, func))){lhs2 => transferData(lhs, lhs2)}  
      } else super.transform(lhs,rhs)

    case _ => 
      super.transform(lhs,rhs)
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
          val block2 = if (spatialConfig.enableParallelBinding && lhs.isOuterControl && (lhs.isPipeControl || lhs.isSeqControl) && lhs.children.length > 1) {
            val bundling = precomputeBundling(lhs)
            if (bundling.toList.size < lhs.children.size) applyBundling(bundling,block.asInstanceOf[Block[Void]]) else block
          } else block
          inlineBlock(block2)
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


