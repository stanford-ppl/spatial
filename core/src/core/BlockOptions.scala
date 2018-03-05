package core

import core.schedule.Scheduler

case class BlockOptions(
  temp: Freq.Freq = Freq.Normal,
  sched: Option[Scheduler] = None
)
object BlockOptions {
  lazy val Normal = BlockOptions(Freq.Normal, None)
  lazy val Sealed = BlockOptions(Freq.Cold,   None)
}
