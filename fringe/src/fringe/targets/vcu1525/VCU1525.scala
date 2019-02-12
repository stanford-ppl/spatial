package fringe.targets.vcu1525

import fringe.{AbstractAccelTop, BigIP, TopInterface}
import fringe.targets.{BigIPSim, DeviceTarget}
import fringe.targets.zynq.ZynqInterface

class VCU1525 extends DeviceTarget {
  def makeBigIP: BigIP = new BigIPSim // TODO
  override val num_channels: Int = 4
  override def topInterface(reset: Reset, accel: AbstractAccelTop): TopInterface = {
    val io = IO(new ZynqInterface) // TODO
    throw new Exception("Top Interface is unimplemented for VCU1525")
    // io
  }
}
