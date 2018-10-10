package fringe.targets.zynq

import chisel3._
import fringe.{AbstractAccelTop, BigIP, TopInterface}
import fringe.targets.DeviceTarget

abstract class ZynqLike extends DeviceTarget {
  def makeBigIP: BigIP = new fringe.targets.zynq.BigIPZynq

  override val magPipelineDepth: Int = 0
  override def regFileAddrWidth(n: Int): Int = 32

  override def topInterface(reset: Reset, accel: AbstractAccelTop): TopInterface = {
    val io = IO(new ZynqInterface)

    // Zynq Fringe
    val blockingDRAMIssue = false // Allow only one in-flight request, block until response comes back
    val fringe = Module(new FringeZynq(blockingDRAMIssue, io.axiLiteParams, io.axiParams))

    // Fringe <-> Host connections
    fringe.io.S_AXI <> io.S_AXI

    // Fringe <-> DRAM connections
    io.M_AXI <> fringe.io.M_AXI

    io.TOP_AXI <> fringe.io.TOP_AXI
    io.DWIDTH_AXI <> fringe.io.DWIDTH_AXI
    io.PROTOCOL_AXI <> fringe.io.PROTOCOL_AXI
    io.CLOCKCONVERT_AXI <> fringe.io.CLOCKCONVERT_AXI

    accel.io.argIns := fringe.io.argIns
    accel.io.argOutLoopbacks := fringe.io.argOutLoopbacks
    fringe.io.argOuts.zip(accel.io.argOuts) foreach { case (fringeArgOut, accelArgOut) =>
      fringeArgOut.bits := accelArgOut.bits
      fringeArgOut.valid := accelArgOut.valid
    }
    // accel.io.argIOIns := fringe.io.argIOIns
    // fringe.io.argIOOuts.zip(accel.io.argIOOuts) foreach { case (fringeArgOut, accelArgOut) =>
    //     fringeArgOut.bits := accelArgOut.bits
    //     fringeArgOut.valid := 1.U
    // }
    fringe.io.externalEnable := false.B
    fringe.io.memStreams <> accel.io.memStreams
    fringe.io.heap <> accel.io.heap
    accel.io.enable := fringe.io.enable
    fringe.io.done := accel.io.done
    fringe.reset := !reset.toBool
    accel.reset := fringe.io.reset
    // accel.reset := ~reset.toBool
    // io.is_enabled := ~accel.io.enable

    io
  }
}

class Zynq extends ZynqLike {
  override def makeBigIP: BigIP = new fringe.targets.zynq.BigIPZynq
  override def regFileAddrWidth(n: Int): Int = 32
  override val magPipelineDepth: Int = 0
  override val addrWidth: Int = 32
  override val dataWidth: Int = 32
  override val wordsPerStream: Int = 16
  override val num_channels = 4
}
