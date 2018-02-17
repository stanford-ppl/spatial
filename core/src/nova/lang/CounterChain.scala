package nova.lang

import forge.tags._
import nova.core._
import nova.node._
import nova.node.pir.CounterChainCopy

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


