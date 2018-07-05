package spatial.metadata.retiming

import argon._

/** For a given symbol, if the symbol is used in a reduction cycle, a list of symbols in that cycle.
  * Otherwise, Nil.
  *
  * Getter:  sym.reduceCycle
  * Setter:  sym.reduceCycle = (Seq[ Sym[_] ])
  * Default: Nil
  */
case class ReduceCycle(x: Seq[Sym[_]]) extends Data[ReduceCycle](Transfer.Remove)


/** The delay of the given symbol from the start of its parent controller.
  *
  * Getter:  sym.fullDelay
  * Setter:  sym.fullDelay = (Double)
  * Default: 0.0
  */
case class FullDelay(latency: Double) extends Data[FullDelay](Transfer.Mirror)

