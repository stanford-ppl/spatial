package fringe.targets.zedboard

import chisel3._
import fringe.{AbstractAccelUnit, BigIP, SpatialIPInterface}
import fringe.targets.DeviceTarget
import fringe.targets.zynq._

class ZedBoard extends Zynq {
  override def makeBigIP: BigIP = new fringe.targets.zedboard.BigIPZedBoard
}
