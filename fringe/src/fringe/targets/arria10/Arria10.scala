package fringe.targets.arria10

import chisel3._
import fringe.{AbstractAccelTop, BigIP, TopInterface}
import fringe.targets.DeviceTarget

class Arria10 extends DeviceTarget {
  def makeBigIP: BigIP = new fringe.targets.arria10.BigIPArria10

  override def topInterface(reset: Reset, accel: AbstractAccelTop): TopInterface = {
    val io = IO(new Arria10Interface)

    val blockingDRAMIssue = false
    val fringe = Module(new FringeArria10(blockingDRAMIssue, io.axiLiteParams, io.axiParams))

    // Fringe <-> Host Connections
    fringe.io.S_AVALON <> io.S_AVALON

    // Fringe <-> DRAM Connections
//    io.M_AXI <> fringe.io.M_AXI
    io.M_AVALON <> fringe.io.M_AVALON

    io.TOP_AXI <> fringe.io.TOP_AXI
    io.DWIDTH_AXI <> fringe.io.DWIDTH_AXI
    io.PROTOCOL_AXI <> fringe.io.PROTOCOL_AXI
    io.CLOCKCONVERT_AXI <> fringe.io.CLOCKCONVERT_AXI

    io.rdata := DontCare

    accel.io.argIns := fringe.io.argIns

    fringe.io.argOuts.zip(accel.io.argOuts) foreach { case (fringeArgOut, accelArgOut) =>
      fringeArgOut.bits := accelArgOut.port.bits
      fringeArgOut.valid := accelArgOut.port.valid
    }
    fringe.io.argEchos.zip(accel.io.argOuts) foreach { case (fringeArgOut, accelArgOut) =>
      accelArgOut.echo := fringeArgOut
    }
    // memStream connections
    fringe.io.externalEnable := false.B
    fringe.io.memStreams <> accel.io.memStreams
    fringe.io.heap <> accel.io.heap
    accel.io.enable := fringe.io.enable
    fringe.io.done := accel.io.done
    fringe.reset := reset.toBool
    accel.reset := fringe.io.reset
//    accel.io.reset := reset.toBool | fringe.io.reset

    io
  }
}
