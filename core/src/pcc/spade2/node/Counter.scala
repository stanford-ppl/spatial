package pcc.spade2.node

import pcc._
import pcc.ir.{I32,Bit}

/*case class Counter(eid: Int) extends Mod[Counter](eid) {
  override type I = this.type

  override def fresh(id: Int): Counter = Counter(id)
  override def stagedClass: Class[Counter] = classOf[Counter]
}
object Counter {
  implicit val ctr: Counter = Counter(-1)
}

case class CounterModule()(implicit state: State) extends Module[Counter] {
  var en: Bit = Bit.bit
  var min: I32 = I32.i32
  var max: I32 = I32.i32
  var step: I32 = I32.i32
  var out: Out[I32] = Out[I32]
  var done: Out[Bit] = Out[Bit]
}*/

