package fringe.targets.zedboard

import chisel3._
import fringe.{AbstractAccelTop, BigIP, TopInterface}
import fringe.targets.DeviceTarget
import fringe.targets.zynq._

class ZedBoard extends Zynq {
  override def makeBigIP: BigIP = new fringe.targets.zedboard.BigIPZedBoard
}
