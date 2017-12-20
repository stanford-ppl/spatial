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
    val stride = step.getOrElse(const[I32](1))
    val parfac = par.getOrElse(param[I32](1))
    stage(CounterAlloc(start, end, stride, parfac))
  }
}

/** Nodes **/
case class CounterAlloc(start: I32, end: I32, step: I32, par: I32) extends BoxAlloc[Counter]