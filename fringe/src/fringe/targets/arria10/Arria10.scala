package fringe.targets.arria10

import chisel3._
import fringe.{AbstractAccelTop, BigIP, TopInterface}
import fringe.targets.DeviceTarget

class Arria10 extends DeviceTarget {
  def makeBigIP: BigIP = new fringe.targets.arria10.BigIPArria10

  override def topInterface(reset: Reset, accel: AbstractAccelTop): TopInterface = {
    val io = IO(new Arria10Interface)

    val blockingDRAMIssue = false
    val fringe = Module(new FringeArria10(
      blockingDRAMIssue,
      io.axiLiteParams,
      io.axiParams
    ))

    // Fringe <-> Host Connections
    fringe.io.S_AVALON <> io.S_AVALON

    // Fringe <-> DRAM Connections
    io.M_AXI <> fringe.io.M_AXI

    // TODO: add memstream connections here
    if (accel.io.argIns.nonEmpty) {
      accel.io.argIns := fringe.io.argIns
    }

    if (accel.io.argOuts.nonEmpty) {
      fringe.io.argOuts.zip(accel.io.argOuts) foreach { case (fringeArgOut, accelArgOut) =>
        fringeArgOut.bits := accelArgOut.port.bits
        fringeArgOut.valid := accelArgOut.port.valid
      }
      fringe.io.argEchos.zip(accel.io.argOuts) foreach { case (fringeArgOut, accelArgOut) =>
        accelArgOut.echo := fringeArgOut
      }
    }

    // memStream connections
    fringe.io.externalEnable := false.B
    fringe.io.memStreams <> accel.io.memStreams
    fringe.io.heap <> accel.io.heap

    accel.io.enable := fringe.io.enable
    fringe.io.done := accel.io.done
    accel.io.reset := reset.toBool | fringe.io.reset

    io
  }
}
