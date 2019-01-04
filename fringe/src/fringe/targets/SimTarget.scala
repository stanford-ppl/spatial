package fringe.targets

import chisel3.Module
import fringe._
import fringe.targets.verilator.VerilatorInterface
import chisel3._

/** Any simulator */
abstract class SimTarget extends DeviceTarget {
  def makeBigIP: BigIP = new BigIPSim

  def topInterface(reset: Reset, accel: AbstractAccelTop): TopInterface = {
    val io: VerilatorInterface = IO(new VerilatorInterface)

    val blockingDRAMIssue = false
    val fringe = Module(new Fringe(blockingDRAMIssue, io.axiParams))
    fringe.io <> DontCare

    // Fringe <-> Host connections
    fringe.io.raddr := io.raddr
    fringe.io.wen   := io.wen
    fringe.io.waddr := io.waddr
    fringe.io.wdata := io.wdata
    io.rdata := fringe.io.rdata

    // Fringe <-> DRAM connections
    io.dram <> fringe.io.dram

    if (accel.io.argIns.length > 0) {
      accel.io.argIns := fringe.io.argIns
    }

    if (accel.io.argOutLoopbacks.length > 0) {
      accel.io.argOutLoopbacks := fringe.io.argOutLoopbacks
    }

    if (accel.io.argOuts.length > 0) {
      fringe.io.argOuts.zip(accel.io.argOuts) foreach { case (fringeArgOut, accelArgOut) =>
        fringeArgOut.bits := accelArgOut.bits
        fringeArgOut.valid := accelArgOut.valid
        accelArgOut.ready := true.B
      }
    }
    fringe.io.memStreams <> accel.io.memStreams
    fringe.io.heap <> accel.io.heap
    accel.io.enable := fringe.io.enable
    fringe.io.done := accel.io.done
    accel.io.reset := fringe.io.reset

    io
  }
}
