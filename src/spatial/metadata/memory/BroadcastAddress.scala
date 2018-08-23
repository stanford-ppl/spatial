package spatial.metadata.memory

import argon._

case class BroadcastAddress(flag: Boolean) extends Data[BroadcastAddress](Transfer.Mirror)
