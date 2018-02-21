package spatial.lang

import forge.tags._
import core._
import spatial.node.CounterChainNew
import pir.node.CounterChainCopy

/** Types **/
case class CounterChain() extends Ref[CounterChain] {
  override type I = Array[Range]
  override def fresh: CounterChain = new CounterChain
  override def isPrimitive: Boolean = false
}
object CounterChain {
  implicit val tp: CounterChain = (new CounterChain).asType

  @api def apply(ctrs: Counter*): CounterChain = stage(CounterChainNew(ctrs))
  @rig def copy(ctrs: Counter*): CounterChain = stage(CounterChainCopy(ctrs))
}


