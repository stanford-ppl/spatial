package spatial.lang

import argon._
import forge.tags._
import spatial.node.CounterChainNew

/** Types */
@ref class CounterChain extends Top[CounterChain] with Ref[Array[Range],CounterChain] {
  override protected val __neverMutable: Boolean = false
}
object CounterChain {
  @api def apply(ctrs: Seq[Counter[_]]): CounterChain = stage(CounterChainNew(ctrs))
}


