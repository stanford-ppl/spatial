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

import spatial.util.TransformUtils._

trait MemStrategy

// When a Token is needed and Buffers are needed
case object Buffer extends MemStrategy

// When the memory should be duplicated -- for registers.
case object Duplicate extends MemStrategy

// When the object needs ordering, but shouldn't be buffered or duplicated (FIFOs, LIFOs, etc.)
case object TokenOnly extends MemStrategy

object MemStrategy {
  def apply(mem: Sym[_]): MemStrategy = mem match {
    case _:Reg[_] => Duplicate
    case _:SRAM[_, _] => Buffer
    case _:FIFO[_] => TokenOnly
  }
}

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
  * @param isFirst: indicates whether this is the first iteration in general -- equivalent to iters map (_.isFirst).reduce {_&_}
  * @param isLast: indicates whether this is the last iteration in general -- equivalent to iters map (_.isLast).reduce {_&_}
  */
@struct case class PseudoIters[IterVec:Bits](iters: IterVec, isFirst: Bit, isLast: Bit)

case class FlattenToStream(IR: State) extends MutateTransformer with AccelTraversal with spatial.util.CounterIterUpdateMixin {

  private def visitInnerForeach(lhs: Sym[_], foreachOp: OpForeach): Sym[_] = {
    val ancestralChains = lhs.ancestors.flatMap(_.cchains)
    val allCounters = ancestralChains.flatMap(_.counters)
    val staticCounters = allCounters.reverse.takeWhile(_.isStatic).reverse
    val dynamicCounters = allCounters.reverse.dropWhile(_.isStatic).reverse

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
        val vecType = Vec.fromSeq(staticCounters.map(_ => PseudoIter(I32(0), Bit(true), Bit(true))))
        implicit def bitsEV: Bits[Vec[PseudoIter]] = vecType
        FIFO[PseudoIters[Vec[PseudoIter]]](I32(32))
      }

      def recurse(): Unit = {
        if (ancestorIterator.hasNext) {
          val (curCtrl, curChain) = ancestorIterator.next()
          // Static Counters go into the primary CChain
          val filteredCounters = curChain.counters filterNot (staticCounters.contains(_))
          // stage all the necessary value reads
          val allInputs = filteredCounters.map(_.inputs).toSet
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
          val withSignals = casted map (iter => (iter, isFirstIter(iter), isLastIter(iter)))
          val elements = withSignals map { case (iter, first, last) => PseudoIter(iter, first, last)}
          val data = Vec.fromSeq(elements)
          val isFirst = withSignals.map(_._2).reduceTree(_ & _)
          val isLast = withSignals.map(_._3).reduceTree(_ & _)
          implicit def bEV: Bits[Vec[PseudoIter]] = data
          indexFIFO.enq(PseudoIters[Vec[PseudoIter]](data, isFirst, isLast))
        }
      }
      isolateSubst() {
        recurse()
      }

      Some(indexFIFO)
    } else None

    val primaryCounters = if (dynamicCounters.nonEmpty) { Seq(stage(ForeverNew())) } else { Seq.empty } ++ staticCounters
    dbgs(s"Primary Chain: ${primaryCounters.map(_.op.get).mkString(", ")}")
    val primaryCChain = CounterChain(primaryCounters)
    stageWithFlow(OpForeach(f(foreachOp.ens), primaryCChain, stageBlock{

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

  val managerMap = cm.Map[Sym[_], MemoryManager]()
  private def visitForeach(lhs: Sym[_], foreachOp: OpForeach): Sym[_] = {
    if (lhs.isInnerControl) {

    } else {
      val definedMems = foreachOp.block.stms.filter(_.isMem)
      val managers = definedMems map {mem => new MemoryManager(lhs, mem, None)}
    }
  }

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = (rhs match {
    case _: AccelScope => inAccel {super.transform(lhs, rhs)}
    case foreachOp: OpForeach if inHw => visitForeach(lhs, foreachOp)
    case _ => super.transform(lhs, rhs)
  }).asInstanceOf[Sym[A]]
}

case class SendRecvFifos[T:Bits](send: FIFO[T], recv: FIFO[T])
class MemoryManager(val control: Sym[_], val mem: Sym[_], val initTokens: Option[Int])(implicit state: argon.State) {
  protected val bundle = state.getCurrentHandle()
  val strategy: MemStrategy = MemStrategy(mem)
}

