package fringe.targets.vcs

import fringe.targets.SimTarget

class VCS extends SimTarget {
  override val wordsPerStream: Int = 64
  override val external_w: Int = 8
  override val external_v: Int = 64
  override val dataWidth: Int = 8
}
