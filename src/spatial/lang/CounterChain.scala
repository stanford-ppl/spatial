package spatial.lang

import forge.tags._
import core._
import spatial.node.CounterChainNew
import pir.node.CounterChainCopy

/** Types **/
@ref class CounterChain extends Top[CounterChain] with Ref[Array[Range],CounterChain] {
  override def isPrimitive: Boolean = false
}
object CounterChain {
  implicit val tp: CounterChain = (new CounterChain).asType

  @api def apply(ctrs: Counter[_]*): CounterChain = stage(CounterChainNew(ctrs))
  @rig def copy(ctrs: Counter[_]*): CounterChain = stage(CounterChainCopy(ctrs))
}


