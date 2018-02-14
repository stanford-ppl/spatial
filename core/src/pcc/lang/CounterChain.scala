package pcc.lang

import forge._
import pcc.core._
import pcc.node._
import pcc.node.pir.CounterChainCopy

/** Types **/
case class CounterChain() extends Top[CounterChain] {
  override type I = Array[Range]
  override def fresh: CounterChain = new CounterChain
  override def isPrimitive: Boolean = false
}
object CounterChain {
  implicit val tp: CounterChain = (new CounterChain).asType

  @api def apply(ctrs: Counter*): CounterChain = stage(CounterChainNew(ctrs))
  @rig def copy(ctrs: Counter*): CounterChain = stage(CounterChainCopy(ctrs))
}


