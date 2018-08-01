package spatial.metadata.retiming

import argon._
import spatial.node.AccumMarker

/** For a given symbol, if the symbol is used in a reduction cycle, information about that cycle.
  *
  * Option:  sym.getReduceCycle
  * Getter:  sym.reduceCycle
  * Setter:  sym.reduceCycle = (Cycle)
  * Default: <undefined>
  */
abstract class Cycle extends Data[Cycle](Transfer.Remove) {
  def length: Double
  def symbols: Set[Sym[_]]
  def marker: AccumMarker
  def cycleID: Int
}

/** Write-after-read (WAR) cycle: Standard read-accumulate loop. */
case class WARCycle(
    reader: Sym[_],
    writer: Sym[_],
    memory: Sym[_],
    symbols: Set[Sym[_]],
    length: Double,
    marker: AccumMarker = AccumMarker.Unknown,
    cycleID: Int = -1)
  extends Cycle {
  override def key: Class[_] = classOf[Cycle]
}

/** Access-after-access (AAA) cycle: Time-multiplexed reads/writes. */
case class AAACycle(accesses: Set[Sym[_]], memory: Sym[_], length: Double) extends Cycle {
  def symbols: Set[Sym[_]] = accesses
  def marker: AccumMarker = AccumMarker.Unknown
  def cycleID: Int = -1
  override def key: Class[_] = classOf[Cycle]
}


/** The delay of the given symbol from the start of its parent controller.
  *
  * Getter:  sym.fullDelay
  * Setter:  sym.fullDelay = (Double)
  * Default: 0.0
  */
case class FullDelay(latency: Double) extends Data[FullDelay](Transfer.Mirror)

