package pcc
package ir

import forge._

/** Types **/
case class Counter(eid: Int) extends Box[Counter](eid) {
  override type I = Range

  override def fresh(id: Int): Counter = Counter(id)
  override def stagedClass: Class[Counter] = classOf[Counter]
}
object Counter {
  implicit val ctr: Counter = Counter(-1)

  @api def apply(start: I32, end: I32, step: Option[I32] = None, par: Option[I32] = None): Counter = {
    Counter.staged(start, end, step.getOrElse(I32.c(1)), par.getOrElse(I32.p(1)))
  }
  @internal def staged(start: I32, end: I32, step: I32, par: I32): Counter = {
    stage(CounterAlloc(start, end, step, par))
  }
}

/** Nodes **/
case class CounterAlloc(start: I32, end: I32, step: I32, par: I32) extends BoxAlloc[Counter] {
  def mirror(f:Tx) = Counter.staged(f(start),f(end),f(step),f(par))
}