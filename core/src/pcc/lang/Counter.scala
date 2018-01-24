package pcc.lang

import forge._
import pcc.core._
import pcc.node._

/** Types **/
case class Counter(eid: Int) extends Sym[Counter](eid) {
  override type I = Range

  override def fresh(id: Int): Counter = Counter(id)
  override def stagedClass: Class[Counter] = classOf[Counter]
  override def isPrimitive: Boolean = false
}
object Counter {
  implicit val tp: Counter = Counter(-1)

  @api def fromSeries(series: Series): Counter = Counter(series.start,series.end,series.step,series.par)

  @api def apply(start: I32, end: I32, step: Option[I32] = None, par: Option[I32] = None): Counter = {
    stage(CounterNew(start, end, step.getOrElse(I32.c(1)), par.getOrElse(I32.p(1)) ))
  }
}
