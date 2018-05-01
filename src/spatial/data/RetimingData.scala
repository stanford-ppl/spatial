package spatial.data

import argon._

/** For a given symbol, if the symbol is used in a reduction cycle, a list of symbols in that cycle.
  * Otherwise, Nil.
  *
  * Getter:  sym.reduceCycle
  * Setter:  sym.reduceCycle = (Seq[ Sym[_] ])
  * Default: Nil
  */
case class ReduceCycle(x: Seq[Sym[_]]) extends FlowData[ReduceCycle]


/** The delay of the given symbol from the start of its parent controller.
  *
  * Getter:  sym.fullDelay
  * Setter:  sym.fullDelay = (Double)
  * Default: 0.0
  */
case class FullDelay(latency: Double) extends StableData[FullDelay]


trait RetimingData {

  implicit class RetimingOps(s: Sym[_]) {
    def reduceCycle: Seq[Sym[_]] = metadata[ReduceCycle](s).map(_.x).getOrElse(Nil)
    def reduceCycle_=(cycle: Seq[Sym[_]]): Unit = metadata.add(s, ReduceCycle(cycle))

    def fullDelay: Double = metadata[FullDelay](s).map(_.latency).getOrElse(0.0)
    def fullDelay(d: Double): Unit = metadata.add(s, FullDelay(d))
  }

}
