package spatial.data

import argon._
import forge.tags._
import spatial.internal.spatialConfig

/** The delay of the given symbol from the start of its parent controller */
case class MDelay(latency: Double) extends StableData[MDelay]
object symDelay {
  def apply(e: Sym[_]): Double = metadata[MDelay](e).map(_.latency).getOrElse(0.0)
  def update(e: Sym[_], delay: Double): Unit = metadata.add(e, MDelay(delay))
}


/** Latency of a given inner pipe body - used for control signal generation. */
case class MBodyLatency(latency: Seq[Double]) extends StableData[MBodyLatency]
object bodyLatency {
  def apply(e: Sym[_]): Seq[Double] = metadata[MBodyLatency](e).map(_.latency).getOrElse(Nil)
  def update(e: Sym[_], latency: Seq[Double]): Unit = metadata.add(e, MBodyLatency(latency))
  def update(e: Sym[_], latency: Double): Unit = metadata.add(e, MBodyLatency(Seq(latency)))

  @stateful def sum(e: Sym[_]): Double = if (spatialConfig.enableRetiming) bodyLatency(e).sum else 0.0
}


/** Initiation interval of a given controller - used for control signal generation. */
case class InitiationInterval(interval: Double) extends StableData[InitiationInterval]
object iiOf {
  def apply(e: Sym[_]): Double = metadata[InitiationInterval](e).map(_.interval).getOrElse(1)
  def update(e: Sym[_], interval: Double): Unit = metadata.add(e, InitiationInterval(interval))
}


/** User-defined intiation interval of a given controller. */
case class UserII(interval: Double) extends StableData[UserII]
object userIIOf {
  def apply(e: Sym[_]): Option[Double] = metadata[UserII](e).map(_.interval)
  def update(e: Sym[_], interval: Option[Double]): Unit = interval.foreach{ii => metadata.add(e, UserII(ii)) }
}

