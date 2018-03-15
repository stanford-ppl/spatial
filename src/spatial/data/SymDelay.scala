package spatial.data

import core._

/** The delay of the given symbol from the start of its parent controller */
case class MDelay(latency: Double) extends StableData[MDelay]
object symDelay {
  def apply(e: Sym[_]): Double = metadata[MDelay](e).map(_.latency).getOrElse(0.0)
  def update(e: Sym[_], delay: Double): Unit = metadata.add(e, MDelay(delay))
}
