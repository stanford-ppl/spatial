package spatial.transform

import argon._
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.types._
import spatial.node._
import spatial.traversal.AccelTraversal
import spatial.util.spatialConfig

import scala.collection.mutable
import scala.collection.mutable.{HashMap, ListBuffer, Set}


/** Binds consecutive controller stages with no hazardous dependencies
  */
case class BindingTransformer(IR: State) extends MutateTransformer with AccelTraversal {

  private def precomputeBundling(lhs: Sym[_]): mutable.HashMap[Int, Seq[Sym[_]]] = {
    // Predetermine bundling
    // TODO: Read - Read dependencies are actually ok
    dbgs(s"Attempt to bundle children of $lhs (${lhs.children.map(_.s.get)})")
    val bundling: mutable.HashMap[Int,Seq[Sym[_]]] = mutable.HashMap(0 -> Seq(lhs.children.head.s.get))
    val prevMems: mutable.Set[Sym[_]] = mutable.Set()
    val prevWrMems: mutable.Set[Sym[_]] = mutable.Set()
    (lhs.children.head.s.get.nestedWrittenMems ++ lhs.children.head.s.get.nestedWrittenDRAMs ++ lhs.children.head.s.get.nestedReadMems ++ lhs.children.head.s.get.nestedReadDRAMs ++ lhs.children.head.s.get.nestedTransientReadMems).foreach(prevMems += _)
    (lhs.children.head.s.get.nestedWrittenMems ++ lhs.children.head.s.get.nestedWrittenDRAMs).foreach{x => prevWrMems += x}
    lhs.children.drop(1).zipWithIndex.foreach{case (cc,i) =>
      val c = cc.s.get
      val activeMems = c.nestedWrittenMems ++ c.nestedWrittenDRAMs ++ c.nestedReadMems ++ c.nestedReadDRAMs ++ c.nestedTransientReadMems
      val addressableMems = (activeMems ++ prevMems).filter(!_.isSingleton).filter(!_.isDRAM)
      val activeWrMems = c.nestedWrittenMems ++ c.nestedWrittenDRAMs
      val nextShouldNotBind = (Seq(c.toCtrl) ++ c.nestedChildren).exists(_.s.get.shouldNotBind) | (c.isSwitch && c.op.exists(_.R.isBits))
      val prevShouldNotBind = (Seq(lhs.children.apply(i)) ++ lhs.children.apply(i).nestedChildren).exists(_.s.get.shouldNotBind) | (lhs.children.apply(i).s.get.isSwitch && lhs.children.apply(i).s.get.op.exists(_.R.isBits))
      val prevStreamShouldNotBind = if (i == 0)
          lhs.hasStreamAncestor && (lhs.children.apply(i).nestedWrittenMems ++ lhs.children.apply(i).nestedReadMems).exists{mem => mem.isStreamOut || mem.isFIFO || mem.isMergeBuffer || mem.isFIFOReg || mem.isStreamIn}
        else false
      val streamShouldNotBind = lhs.hasStreamAncestor && activeMems.exists{mem => mem.isStreamOut || mem.isFIFO || mem.isMergeBuffer || mem.isFIFOReg || mem.isStreamIn}
      val breakWhenShouldNotBind = activeMems.exists{m => (Seq(lhs.toCtrl) ++ lhs.ancestors).map(_.breaker).contains(Some(m))}
      if (prevMems.intersect(activeMems).intersect(activeWrMems ++ prevWrMems ++ addressableMems).nonEmpty || nextShouldNotBind || prevStreamShouldNotBind || prevShouldNotBind || streamShouldNotBind || breakWhenShouldNotBind) {
        dbgs(s"Conflict between:")
        dbgs(s" - Prev rd/wr: $prevMems")
        dbgs(s" - Next rd/wr: $activeMems")
        dbgs(s" - Next wr + Prev wr + addressable: ${prevWrMems ++ activeWrMems ++ addressableMems}")
        dbgs(s" - Stream dependency: $streamShouldNotBind")
        dbgs(s" - Prev Stream dependency (child #1 only): $prevStreamShouldNotBind")
        dbgs(s" - BreakWhen dependency: $breakWhenShouldNotBind")
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
    bundling.toList.sortBy(_._1).foreach{case (grp, children) =>
      dbgs(s"""Bundled child $grp contains $children""")
    }
    bundling
  }

  private def applyBundling(bundling: mutable.HashMap[Int, Seq[Sym[_]]], block: Block[Void]): Block[Void] = {
    var curGrp = 0
    val bundledStms: ListBuffer[Sym[_]] = ListBuffer()
    stageBlock{
      block.stms.foreach{ stm => 
        // Add stm to roster if it is a ctrl to be bundled
        if (bundling(curGrp).contains(stm)) bundledStms += stm
        else {
          visit(stm)
        }
        // If we are at final controller in bundle, visit each sym in roster (need to transfer metadata, so use visit instead of visit)
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
    case UnrolledForeach(ens, cchain, blk, is, vs, stopWhen) if spatialConfig.enableParallelBinding && lhs.isOuterControl && (lhs.isPipeControl || lhs.isSeqControl) && lhs.children.length > 1 =>
      val bundling = precomputeBundling(lhs)
      if (bundling.toList.size < lhs.children.size) {
        stageWithFlow(UnrolledForeach(ens, cchain, applyBundling(bundling, blk), is, vs, stopWhen)){lhs2 => transferData(lhs, lhs2)}  
      } else super.transform(lhs,rhs)
    case UnrolledReduce(ens, cchain, blk, is, vs, stopWhen) if spatialConfig.enableParallelBinding && lhs.isOuterControl && (lhs.isPipeControl || lhs.isSeqControl) && lhs.children.length > 1 =>
      val bundling = precomputeBundling(lhs)
      if (bundling.toList.size < lhs.children.size) {
        stageWithFlow(UnrolledReduce(ens, cchain, applyBundling(bundling, blk), is, vs, stopWhen)){lhs2 => transferData(lhs, lhs2)}  
      } else super.transform(lhs,rhs)
    case SwitchCase(blk) if spatialConfig.enableParallelBinding && lhs.isOuterControl && lhs.children.length > 1 =>
      Type[A] match {
        case _:Void => 
          val bundling = precomputeBundling(lhs)
          if (bundling.toList.size < lhs.children.size) {
            stageWithFlow(SwitchCase(applyBundling(bundling,blk.asInstanceOf[Block[Void]]))){lhs2 => transferData(lhs, lhs2)}  
          } else super.transform(lhs,rhs)
        case _      => 
          super.transform(lhs,rhs)
      }

    case AccelScope(func) if lhs.isOuterControl =>
      val bundling = precomputeBundling(lhs)
      if (bundling.toList.size < lhs.children.size) {
        stageWithFlow(AccelScope(applyBundling(bundling, func))){lhs2 => transferData(lhs, lhs2)}  
      } else super.transform(lhs,rhs)

    case _ => 
      super.transform(lhs,rhs)
  }

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case _:Switch[_]  => super.transform(lhs,rhs)
    case _:AccelScope => inAccel{ transformCtrl(lhs,rhs) }
    case _:BlackboxImpl[_,_,_] => inBox{ transformCtrl(lhs,rhs) }
    case _:Control[_] => transformCtrl(lhs,rhs)

    case _ => super.transform(lhs,rhs)
  }

}


