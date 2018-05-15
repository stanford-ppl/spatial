package spatial.data

import argon._

sealed abstract class AccumType {
  def |(that: AccumType): AccumType
  def >(that: AccumType): Boolean
  final def >=(that: AccumType): Boolean = this > that || this == that
}
object AccumType {
  case object Fold extends AccumType {
    def |(that: AccumType): AccumType = this
    def >(that: AccumType): Boolean = that != Fold
    override def toString = "Fold"
  }
  case object Buff extends AccumType {
    def |(that: AccumType): AccumType = that match {case Fold => Fold; case _ => Buff}
    def >(that: AccumType): Boolean = (that | Reduce) match {case Reduce => true; case _ => false }
    override def toString = "Buffer"
  }
  case object Reduce extends AccumType {
    def |(that: AccumType): AccumType = that match {case None => Reduce; case _ => that}
    def >(that: AccumType): Boolean = that match {case None => true; case _ => false }
    override def toString = "Reduce"
  }
  case object None extends AccumType {
    def |(that: AccumType): AccumType = that
    def >(that: AccumType): Boolean = false
  }
}
sealed trait ReduceFunction
case object FixPtSum extends ReduceFunction
case object FltPtSum extends ReduceFunction
case object FixPtMin extends ReduceFunction
case object FixPtMax extends ReduceFunction
case object OtherReduction extends ReduceFunction


/** Flags that this symbol is associated with an accumulator.
  * If this symbol is a memory, this memory is an accumulator.
  * If this symbol is a memory access, the access is a read or write to an accumulator.
  *
  * Getter:  sym.accumType
  * Setter:  sym.accumType = (AccumType)
  * Default: AccumType.None
  */
case class Accumulator(tp: AccumType) extends StableData[Accumulator]


/** TODO: Update description
  * Getter:  sym.reduceType : Option[ReduceFunction]
  * Setter:  sym.reduceType = (ReduceFunction | Option[ReduceFunction])
  * Default: None
  */
case class ReduceType(func: ReduceFunction) extends StableData[ReduceType]


trait AccumulatorData {

  implicit class AccumulatorOps(s: Sym[_]) {
    def accumType: AccumType = metadata[Accumulator](s).map(_.tp).getOrElse(AccumType.None)
    def accumType_=(tp: AccumType): Unit = metadata.add(s, Accumulator(tp))

    def reduceType: Option[ReduceFunction] = metadata[ReduceType](s).map(_.func)
    def reduceType_=(func: ReduceFunction): Unit = metadata.add(s, ReduceType(func))
    def reduceType_=(func: Option[ReduceFunction]): Unit = func.foreach{f => s.reduceType = f }
  }

}
