package fringe.targets.asic

import fringe.BigIP
import fringe.targets.SimTarget

class ASIC extends SimTarget {
  override def makeBigIP: BigIP = new BigIPASIC
  override val wordsPerStream: Int = 64
  override val external_w: Int = 8
  override val external_v: Int = 64
  override val dataWidth: Int = 8
}
