package spatial.transform.streamify

import argon._
import argon.lang._
import spatial.metadata.access.TimeStamp

case class TimeTriplet(time: Num[_], isFirst: Bit, isLast: Bit)

// Iters is ordered outermost to innermost
case class TimeMap(iters: Seq[(Sym[_], TimeTriplet)]) extends TimeStamp {
  private lazy val iterMap = iters.toMap
  override def apply[T: Num](s: Sym[T]): T = iterMap(s).time.asInstanceOf[T]

  override def isFirst(s: Sym[_]): Bit = iterMap(s).isFirst

  override def isLast(s: Sym[_]): Bit = iterMap(s).isLast

  override def support: Set[Sym[Num[_]]] = iterMap.keySet.map(_.asInstanceOf[Sym[Num[_]]])

  @forge.tags.api def ++(other: TimeMap): TimeMap = {
    if (other.iters.isEmpty) return this

    // Since other is nested to the right, we need to update our iters
    val (_, TimeTriplet(_, headFirst, headLast)) = other.iters.head
    val newIters = iters.map {
      case (iter, TimeTriplet(time, isFirst, isLast)) =>
        iter -> TimeTriplet(time, isFirst & headFirst, isLast & headLast)
    }
    TimeMap(newIters ++ other.iters)
  }
}

object TimeMap {
  val empty: TimeMap = TimeMap(Seq.empty)
}
