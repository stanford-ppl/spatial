package fringe.targets.zcu

import fringe.BigIP

class ZCU extends fringe.targets.zynq.ZynqLike {
  override def makeBigIP: BigIP = new fringe.targets.zynq.BigIPZynq
  override def regFileAddrWidth(n: Int): Int = 40
  override val magPipelineDepth: Int = 0
  override val addrWidth: Int = 40
  override val dataWidth: Int = 64
  override val external_w: Int = 64
  override val external_v: Int = 8
  override val wordsPerStream: Int = 8
  override val num_channels = 2
}