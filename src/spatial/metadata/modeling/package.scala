package spatial.metadata

import argon._
import forge.tags.stateful
import models._
import spatial.util.modeling._

package object modeling {

  implicit class ExpModelOps(s: Sym[_]) {
    @stateful def area: Area = metadata[SavedArea](s).map(_.area).getOrElse(NoArea)
    def area_=(a: Area): Unit = metadata.add(s, SavedArea(a))

    def latency: Double = metadata[SavedLatency](s).map(_.latency).getOrElse(0.0)
    def latency_=(l: Double): Unit = metadata.add(s, SavedLatency(l))
  }

}
