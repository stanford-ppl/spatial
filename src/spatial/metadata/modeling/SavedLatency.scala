package spatial.metadata.modeling

import argon._

case class SavedLatency(latency: Double) extends Data[SavedLatency](Transfer.Mirror)
