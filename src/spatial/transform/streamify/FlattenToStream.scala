package spatial.transform.streamify

import argon.transform.MutateTransformer
import argon._
import argon.tags.struct
import spatial.lang._
import spatial.node._
import spatial.traversal.AccelTraversal

import scala.collection.{mutable => cm}
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.util.TransformUtils
import spatial.util.TransformUtils._

trait MemStrategy

// When a Token is needed and Buffers are needed
case object Buffered extends MemStrategy

// When the memory should be duplicated -- for registers.
case object Duplicate extends MemStrategy

// When the object needs ordering, but shouldn't be buffered or duplicated (FIFOs, LIFOs, etc.)
case object TokenOnly extends MemStrategy

/**
  * A PseudoIter represents an iteration variable that is sent along a FIFO in order to handle variable bounds
  * @param i: the value of the iterator
  * @param isFirst: whether this is the first iteration of this particular iterator
  * @param isLast: whether this is the last iteration of this particular iterator
  */
@struct case class PseudoIter(i: I32, isFirst: Bit, isLast: Bit)

/**
  * A bundle of pseudoIters, representing the leading iterators of a variable-iteration controller
  * @param iters: a vector of PseudoIter
  */
@struct case class PseudoIters[IterVec:Bits](iters: IterVec)

case class FlattenToStream(IR: State) extends MutateTransformer with AccelTraversal with spatial.util.CounterIterUpdateMixin {


  /**
    * Captures when a token should be acquired/released for the memory
    * @param mem
    * @param isFirst
    * @param isLast
    */
  case class MemEnableData(mem: Sym[_], isFirst: Bit, isLast: Bit)

//  private def processBlock[T](block: Block[T], enableData: Map[Sym[_], MemEnableData]): Block[T] = {
//    stageScope(f(block.inputs), block.options) {
//      // Get all registers for remapping
//      enableData.filter(_._1.isReg) foreach {
//        case (reg, MemEnableData(_, enq, deq)) =>
//
//      }
//      block.stms foreach {
//
//      }
//    }
//  }

  private def visitInnerForeach(lhs: Sym[_], foreachOp: OpForeach): Sym[_] = {
    dbgs(s"Visiting Inner Foreach: $lhs = $foreachOp")
    val ancestralChains = lhs.ancestors.flatMap(_.cchains)
    val allCounters = ancestralChains.flatMap(_.counters)
    val staticCounters = allCounters.reverse.takeWhile(_.isStatic).reverse
    val dynamicCounters = allCounters.reverse.dropWhile(_.isStatic).reverse

    dbgs(s"Static Counters: $staticCounters")
    dbgs(s"Dynamic Counters: $dynamicCounters")

    val indexFifo = if (dynamicCounters.nonEmpty) {
      // Stage the feeder controllers
      val ancestorIterator = lhs.ancestors.flatMap({
        ctrl =>
          ctrl.cchains map (cc => (ctrl, cc))
      }).toIterator

      // Create the index FIFO
      // TODO: Do we need to calculate the fifo depth? The IndexFIFO should be backpressured the entire time.
      val indexFIFO = {
        // create a dummy vector to provide the requisite evidence
        val vecType = Vec.fromSeq(dynamicCounters.map({x => PseudoIter(I32(0), Bit(true), Bit(true))}))
        implicit def bitsEV: Bits[Vec[PseudoIter]] = vecType
        FIFO[PseudoIters[Vec[PseudoIter]]](I32(32))
      }

      def recurse(): Unit = {
        if (ancestorIterator.hasNext) {
          val (curCtrl, curChain) = ancestorIterator.next()
          // Static Counters go into the primary CChain
          val filteredCounters = curChain.counters filterNot (staticCounters.contains(_))
          // stage all the necessary value reads
          val allInputs = filteredCounters.flatMap(_.inputs).toSet
          val newChain = inCopyMode(true) {
            // TODO: support other transients like FixToFix
            allInputs.foreach { case read@Op(RegRead(_)) => visit(read) }
            filteredCounters.foreach(visit(_))
            mirrorSym(curChain)
          }
          val newEnables = curCtrl.s match {
            case Some(enControl: EnControl[_]) => enControl.ens
            case _ => Set.empty[Bit]
          }
          stage(OpForeach(f(newEnables), newChain.unbox, stageBlock {
            recurse()
            spatial.lang.void
          }, f(filteredCounters.map(_.iter.get)).asInstanceOf[Seq[I32]], None))
        } else {
          // We're on the innermost loop
          val iters = f(dynamicCounters).flatMap(_.iter)
          dbgs(s"Updated Iters: ${iters.mkString(", ")}")
          val casted = iters.asInstanceOf[Seq[Sym[I32]]].map(_.unbox)
          val isFirsts = isFirstIters(casted:_*)
          val isLasts = isLastIters(casted:_*)
          val elements = (casted.zip(isFirsts).zip(isLasts)) map {
            case ((i, f), l) => PseudoIter(i, f, l)
          }
          val data = Vec.fromSeq(elements)
          implicit def bEV: Bits[Vec[PseudoIter]] = data
          indexFIFO.enq(PseudoIters[Vec[PseudoIter]](data))
        }
      }
      isolateSubst() {
        recurse()
      }

      Some(indexFIFO)
    } else None

    val newStaticCounters = staticCounters.map(mirrorSym(_).unbox)
    val primaryCounters = if (dynamicCounters.nonEmpty) { Seq(stage(ForeverNew())) } else { Seq.empty } ++ newStaticCounters
    dbgs(s"Primary Chain: ${primaryCounters.map(_.op.get).mkString(", ")}")
    val primaryCChain = CounterChain(primaryCounters)
    stageWithFlow(OpForeach(f(foreachOp.ens), primaryCChain, stageBlock {
      val staticIters = newStaticCounters.map(_.iter.get.unbox).asInstanceOf[Seq[I32]]
      (staticIters zip staticCounters.map(_.iter.get)) foreach {
        case (newIter, oldIter) =>
          register(oldIter.unbox, newIter.asSym)
      }
      dbgs(s"Static Iters: $staticIters")
      val isFirsts = cm.Map((staticIters zip isFirstIters(staticIters:_*)):_*)
      val isLasts = cm.Map((staticIters zip isLastIters(staticIters:_*)):_*)
      indexFifo match {
        case None =>
        case Some(fifo) =>
          dbgs(s"Handling Dynamic Iters")
          val staticIsFirst = if (staticIters.nonEmpty) { isFirsts(staticIters.head) } else Bit(true)
          val staticIsLast = if (staticIters.nonEmpty) { isLasts(staticIters.head) } else Bit(true)
          val pseudoIters = fifo.deq(staticIsFirst)
          val streamedIters = pseudoIters.iters
          dbgs(s"StreamedIters: ${streamedIters.size}")
          dbgs(s"Dynamic Counters: $dynamicCounters")
          assert(streamedIters.size == dynamicCounters.size, s"Expected StreamedIters and DynamicCounters to be of the same size!")
          (streamedIters.elems zip dynamicCounters) foreach {
            case (dynIter, dynCtr) =>
              dbgs(s"Registering Dynamic Counter: ${dynCtr.iter.get} -> ${dynIter.i.asSym}")
              register(dynCtr.iter.get -> dynIter.i.asSym)
              isFirsts(dynCtr.iter.get.unbox.asInstanceOf[I32]) = dynIter.isFirst & staticIsFirst
              isLasts(dynCtr.iter.get.unbox.asInstanceOf[I32]) = dynIter.isLast & staticIsLast
          }
      }

      dbgs(s"IsFirsts: $isFirsts")
      dbgs(s"IsLasts: $isLasts")
      // need to get appropriate tokens
      val enableData = ((lhs.readMems ++ lhs.writtenMems) map {
        mem =>
          dbgs(s"Wiring up: $mem = ${mem.op}")
          indent {
            // We a acquire a token at the LDA between us and the previous producer
            // and release it at the LDA between us and the next consumer

            // Compute LDA between us and previous producer

            val (_, pathToAncestor, _) = LCAWithPaths(lhs.toCtrl, mem.parent)
            dbgs(s"Path to ancestor: ${pathToAncestor.mkString(" -> ")}")
            // Now we need to get the counters involved.
            // On each fresh iteration of the ancestor, we get a new token
            // What we want here is the outermost CChain
            val outermostCChain = pathToAncestor.drop(1).find(_.cchains.nonEmpty).get.cchains.head
            dbgs(s"Outermost CChain: $outermostCChain")
            val outermostCounter = outermostCChain.counters.head
            dbgs(s"Outermost counter: $outermostCounter")

            val outermostIter = outermostCounter.iter.get
            val isFirst = isFirsts(outermostIter.unbox.asInstanceOf[I32])
            val isLast = isLasts(outermostIter.unbox.asInstanceOf[I32])
            dbgs(s"Outermost Iter: $outermostIter ($isFirst, $isLast)")
            mem -> MemEnableData(mem, isFirst, isLast)
          }
      }).toMap

      foreachOp.block.stms foreach {
        case sym if !sym.isMem =>
          super.visit(sym)
        case reg@Op(_:RegNew[_]) =>

      }

    }, primaryCounters.flatMap(_.iter).asInstanceOf[Seq[I32]], f(foreachOp.stopWhen))) {lhs2 => transferData(lhs, lhs2)}
  }
  /**
    * Counterchain mapping:
    *   Let D denote a dynamic chain, and S denote a static chain
    *
    *   If the chain is purely static: CChain(S+)
    *     We can keep the chain as is, as we do not need to worry about data dependencies.
    *
    *   If the chain has dynamic elements, then we need to read those at the appropriate time points.
    *     For a counterchain CChain((D|S)*, D, S*), we can create a new chain, which can be called the "execution" controller:
    *     CChain(Forever, S*)
    *
    *     This necessitates creating an additional "feeder" controller to provide the missing indices, and performs reads
    *     at appropriate times.
    *     This feeder can be phrased as a set of nested Foreaches that are later collapsed.
    **/

  private def visitForeach(lhs: Sym[_], foreachOp: OpForeach): Sym[_] = {
    // TODO: Compute initTokens
    val linearizedUses = computeLinearizedUses(lhs, foreachOp.block)
    lhs
  }

  /**
    * Captures a writer and all readers which see that value. We consider writers to kill the previous writer
    *   even if it is a conditional write. Here, we consider only inner pipelines as readers/writers
    *   instead of the actual Writer/Reader node.
    * @param writer -- The Inner Controller which writes to Mem
    * @param readers -- The Inner Controllers which read that value of Mem
    */
  case class LinearizedUse(writer: Option[Sym[_]], readers: cm.ArrayBuffer[Sym[_]])
  private def computeLinearizedUses(parent: Sym[_], block: Block[_]) = {
    val mems = block.internalMems
    val memUseMap = cm.Map[Sym[_], cm.ArrayBuffer[LinearizedUse]](
      (mems map {mem => (mem -> cm.ArrayBuffer[LinearizedUse]())}):_*
    )
    parent.nestedChildren.filter({x => x.isInnerControl || x.isCounter}).foreach {
      ctrl =>
        val allReads = ctrl.readMems ++ ctrl.writtenMems
        val allWrites = ctrl.writtenMems

        allReads.intersect(mems.toSet) foreach {
          mem =>
            val useMap = memUseMap(mem)
            useMap.lastOption match {
              case Some(uses) => uses.readers.append(ctrl.sym)
              case None => useMap.append(LinearizedUse(None, cm.ArrayBuffer(ctrl.sym)))
            }
        }

        allWrites.intersect(mems.toSet) foreach {
          mem =>
            memUseMap(mem).append(LinearizedUse(Some(ctrl.sym), cm.ArrayBuffer.empty))
        }
    }
    dbgs(s"Linearized Uses: $memUseMap")
    memUseMap
  }

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = (rhs match {
    case accelScope: AccelScope => inAccel {
      computeLinearizedUses(lhs, accelScope.block)
      super.transform(lhs, rhs)
    }
    case foreachOp: OpForeach if inHw && lhs.isOuterControl =>
      visitForeach(lhs, foreachOp)
      super.transform(lhs, rhs)

    case foreachOp: OpForeach if inHw && lhs.isInnerControl =>
      visitInnerForeach(lhs, foreachOp)

    case _ => super.transform(lhs, rhs)
  }).asInstanceOf[Sym[A]]
}


