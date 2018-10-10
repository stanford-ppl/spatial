package fringe.targets.aws

import chisel3._
import fringe.{AbstractAccelTop, BigIP, TopInterface}
import fringe.targets.DeviceTarget
import fringe.utils.getFF
import fringe.targets.zynq.FringeZynq

class AWS_F1 extends DeviceTarget {
  def makeBigIP: BigIP = new fringe.targets.aws.BigIPAWS

  override val addrWidth: Int = 64
  override val num_channels: Int = 4

  override def topInterface(reset: Reset, accel: AbstractAccelTop): TopInterface = {
    val io = IO(new AWSInterface)

    val blockingDRAMIssue = false  // Allow only one in-flight request, block until response comes back
    val fringe = Module(new FringeZynq(blockingDRAMIssue, io.axiLiteParams, io.axiParams))

    // Fringe <-> DRAM connections
    //      topIO.dram <> fringe.io.dram
    io.M_AXI <> fringe.io.M_AXI
    fringe.io.memStreams <> accel.io.memStreams
    fringe.io.heap <> accel.io.heap

    // Accel: Scalar and control connections
    accel.io.argIns := io.scalarIns
    io.scalarOuts.zip(accel.io.argOuts).foreach{case (ioOut, accelOut) => ioOut := getFF(accelOut.bits, accelOut.valid) }
    accel.io.enable := io.enable
    io.done := accel.io.done
    accel.io.reset := fringe.io.reset

    fringe.io.externalEnable := io.enable
    //      topIO.dbg <> fringe.io.dbg

    io
  }
}


