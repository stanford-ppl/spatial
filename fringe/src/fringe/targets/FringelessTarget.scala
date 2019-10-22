package fringe.targets

import chisel3._
import fringe._
import fringe.targets.verilator.FringelessInterface

/** Sim target for experiments where you only want the accel block and not fringe */
class FringelessTarget extends DeviceTarget {
  def makeBigIP: BigIP = new BigIPSim

  def addFringeAndCreateIP(reset: Reset, accel: AbstractAccelUnit): SpatialIPInterface = {
    val io: FringelessInterface = IO(new FringelessInterface)
    io <> DontCare
    io
  }
}
