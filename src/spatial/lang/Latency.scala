package spatial.lang

import argon.withFlow
import forge.tags._
import spatial.metadata.retiming._

object ForcedLatency {
  @api def apply[R](latency: Double)(block: => R): R = {
    withFlow("ForcedLatency", x => x.forcedLatency = latency) { block }
  }
}
