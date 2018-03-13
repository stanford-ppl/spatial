package spatial.data

import core._

sealed trait ReduceFunction
case object FixPtSum extends ReduceFunction
case object FltPtSum extends ReduceFunction
case object FixPtMin extends ReduceFunction
case object FixPtMax extends ReduceFunction
case object OtherReduction extends ReduceFunction

case class MReduceType(func: Option[ReduceFunction]) extends StableData[MReduceType]

object reduceType {
  def apply(e: Sym[_]): Option[ReduceFunction] = metadata[MReduceType](e).flatMap(_.func)
  def update(e: Sym[_], func: ReduceFunction): Unit = metadata.add(e, MReduceType(Some(func)))
  def update(e: Sym[_], func: Option[ReduceFunction]): Unit = metadata.add(e, MReduceType(func))
}