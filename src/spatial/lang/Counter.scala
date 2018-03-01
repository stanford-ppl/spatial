package spatial.lang

import forge.tags._
import core._
import spatial.node._

/** Types **/
@ref class Counter extends Top[Counter] with Ref[Range,Counter] {
  override def isPrimitive: Boolean = false

  @rig def from(c: Any, checked: Boolean): Option[Counter] = c match {
    //case s: Series[_] => Counter.from(series)
    case r: Range  => Some(Counter(r.start,r.end,r.step,I32.c(1)))
    case _ => None
  }
}
object Counter {
  @api def apply(start: I32, end: I32, step: I32 = I32.c(1), par: I32 = I32.c(1)): Counter = {
    stage(CounterNew(start, end, step, par))
  }

  @rig def from(series: Series[I32]): Counter = {
    val Series(start,end,step,par,_) = series
    Counter(start,end,step,par)
  }
}
