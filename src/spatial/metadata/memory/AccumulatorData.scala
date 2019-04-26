package spatial.metadata.memory

import argon._

/** The accumulation type for a local memory. */
sealed abstract class AccumType {
  def |(that: AccumType): AccumType
  def >(that: AccumType): Boolean
  final def >=(that: AccumType): Boolean = this > that || this == that
}
object AccumType {
  /** A fold accumulator. Always reads previous value, even before accumulation starts. */
  case object Fold extends AccumType {
    def |(that: AccumType): AccumType = this
    def &(that: AccumType): AccumType = that match {case Unknown => Fold; case _ => that }
    def >(that: AccumType): Boolean = that != Fold
    override def toString = "Fold"
  }
  /** A buffer-only accumulator. */
  case object Buff extends AccumType {
    def |(that: AccumType): AccumType = that match {case Fold => that; case _ => this }
    def &(that: AccumType): AccumType = that match {case Unknown => this; case Fold => this; case _ => that }
    def >(that: AccumType): Boolean = (that | Reduce) match {case Reduce => true; case _ => false }
    override def toString = "Buffer"
  }
  /** A reduce accumulator. Reads previous value only after first accumulation. */
  case object Reduce extends AccumType {
    def |(that: AccumType): AccumType = that match {case Unknown => this; case None => this; case _ => that }
    def &(that: AccumType): AccumType = that match {case None => that; case _ => this }
    def >(that: AccumType): Boolean = that match {case Unknown => true; case None => true; case _ => false }
    override def toString = "Reduce"
  }
  /** Memory is not an accumulator. */
  case object None extends AccumType {
    def |(that: AccumType): AccumType = that match {case Unknown => this; case _ => that }
    def &(that: AccumType): AccumType = this
    def >(that: AccumType): Boolean = that match {case Unknown => true; case _ => false }
  }

  /** Unknown **/
  case object Unknown extends AccumType {
    def |(that: AccumType): AccumType = that
    def &(that: AccumType): AccumType = that
    def >(that: AccumType): Boolean = false
  }
}


/** Flags that this symbol is associated with an accumulator.
  * If this symbol is a memory, this memory is an accumulator.
  * If this symbol is a memory access, the access is a read or write to an accumulator.
  *
  * Getter:  sym.accumType
  * Setter:  sym.accumType = (AccumType)
  * Default: AccumType.None
  */
case class AccumulatorType(tp: AccumType) extends Data[AccumulatorType](Transfer.Mirror)

// TODO: Need to refactor this
sealed trait ReduceFunction
case object FixPtSum extends ReduceFunction
case object FltPtSum extends ReduceFunction
case object FixPtMin extends ReduceFunction
case object FixPtMax extends ReduceFunction
case object FixPtFMA extends ReduceFunction
case object OtherReduction extends ReduceFunction

/** TODO: Update description
  * Getter:  sym.reduceType : Option[ReduceFunction]
  * Setter:  sym.reduceType = (ReduceFunction | Option[ReduceFunction])
  * Default: None
  */
case class ReduceType(func: ReduceFunction) extends Data[ReduceType](SetBy.Analysis.Self)

/** TODO: Update description
  * Getter:  sym.fmaReduceInfo : Option[(data, mul1, mul2, fma, latency)]
  * Setter:  sym.fmaReduceInfo = ((data, mul1, mul2, fma, latency) | Option[(data, mul1, mul2, fma, latency)])
  * Default: None
  */
case class FMAReduce(info: (Sym[_], Sym[_], Sym[_], Sym[_], Double)) extends Data[FMAReduce](SetBy.Analysis.Self)

/** TODO: Update description
  * Getter:  sym.iterDiff : Int
  * Setter:  sym.iterDiff = Int
  * Default: 1
  */
case class IterDiff(diff: Int) extends Data[IterDiff](SetBy.Analysis.Self)

/** Mapping from lane to which segment of an unrolled accumulation it should be placed in.  Segmentation
    occurs when later lanes of an unrolled read depend on earlier lanes of the unrolled write

  */
case class SegmentMapping(mapping: Map[Int,Int]) extends Data[SegmentMapping](SetBy.Analysis.Self)

/*
 * A marker for inner loop accumulator. Used by pir
 * */

case class InnerAccum(isInnerAccum:Boolean) extends Data[InnerAccum](SetBy.Analysis.Self)
