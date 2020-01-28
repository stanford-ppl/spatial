package fringe.targets.cxp

import chisel3._
import fringe.{AbstractAccelUnit, BigIP, SpatialIPInterface}
import fringe.targets.DeviceTarget

class CXP extends DeviceTarget {

  override val magPipelineDepth: Int = 0
  override val external_w: Int = 256
  override val external_v: Int = 1
  override val dataWidth = 256
  override val wordsPerStream = 1

  override def regFileAddrWidth(n: Int): Int = 32

  override def addFringeAndCreateIP(reset: Reset, accel: AbstractAccelUnit): SpatialIPInterface = {
    val io = IO(new CXPInterface)
    io <> DontCare

    // CXP Fringe
    val blockingDRAMIssue = false // Allow only one in-flight request, block until response comes back
    val fringe = Module(new FringeCXP(blockingDRAMIssue, io.axiParams))
    fringe.io <> DontCare

    // Fringe <-> DRAM connections
    io.M_AXI <> fringe.io.M_AXI

    // SpatialIP <-> AXIStream connections (note they go directly from accel to spatial IP)
    io.AXIS_IN <> accel.io.axiStreamsIn.head
    io.AXIS_OUT <> accel.io.axiStreamsOut.head

    // Fringe <-> Host connections
    fringe.io.ctrl_addr := io.CUSTOMLOGIC_CTRL_ADDR
    fringe.io.ctrl_data_in_ce := io.CUSTOMLOGIC_CTRL_DATA_IN_CE
    fringe.io.ctrl_data_in := io.CUSTOMLOGIC_CTRL_DATA_IN
    io.CUSTOMLOGIC_CTRL_DATA_OUT := fringe.io.ctrl_data_out

    // io.rdata handled by bridge inside FringeCXP
    io.rdata := DontCare

    accel.io.argIns := fringe.io.argIns
    fringe.io.argOuts.zip(accel.io.argOuts) foreach { case (fringeArgOut, accelArgOut) =>
      fringeArgOut.bits := accelArgOut.port.bits
      fringeArgOut.valid := accelArgOut.port.valid
    }
    fringe.io.argEchos.zip(accel.io.argOuts) foreach { case (fringeArgOut, accelArgOut) =>
      accelArgOut.echo := fringeArgOut
    }

    fringe.io.memStreams <> accel.io.memStreams
    fringe.io.heap <> accel.io.heap
    accel.io.enable := fringe.io.enable
    fringe.io.done := accel.io.done
    fringe.reset := !reset.toBool
    accel.reset := fringe.io.reset


    io
  }

  def makeBigIP: BigIP = new fringe.targets.cxp.BigIPCXP

}
