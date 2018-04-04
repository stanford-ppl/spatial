package fringe

import chisel3._
import chisel3.util._
import axi4._
import templates.Utils.log2Up

/**
 * FringeZynq: Top module for FPGA shell
 * @param w: Word width
 * @param numArgIns: Number of input scalar arguments
 * @param numArgOuts: Number of output scalar arguments
 */
class FringeArria10 (
  val w: Int,
  val numArgIns: Int,
  val numArgOuts: Int,
  val numArgIOs: Int,
  val numChannels: Int,
  val numArgInstrs: Int,
  val argOutLoopbacksMap: scala.collection.immutable.Map[Int, Int],
  val loadStreamInfo: List[StreamParInfo],
  val storeStreamInfo: List[StreamParInfo],
  val streamInsInfo: List[StreamParInfo],
  val streamOutsInfo: List[StreamParInfo],
  val blockingDRAMIssue: Boolean,
  val axiLiteParams: AXI4BundleParameters,
  val axiParams: AXI4BundleParameters
) extends Module {
  val numRegs = numArgIns + numArgOuts + numArgIOs + 2  // (command, status registers)
  // val addrWidth = log2Up(numRegs)

  val commandReg = 0  // TODO: These vals are used in test only, logic below does not use them.
  val statusReg = 1   //       Changing these values alone has no effect on the logic below.

  // Some constants (mostly MAG-related) that will later become module parameters
  val v = 16 // Number of words in the same stream
  val numOutstandingBursts = 1024  // Picked arbitrarily
  val burstSizeBytes = 64
  val d = 16 // FIFO depth: Controls FIFO sizes for address, size, and wdata. Rdata is not buffered

  // val axiLiteParams = new AXI4BundleParameters(10, w, 1)
  val io = IO(new Bundle {
    // Host scalar interface
    val S_AVALON = new AvalonSlave(axiLiteParams)

    // DRAM interface
    val M_AXI = Vec(numChannels, new AXI4Inlined(axiParams))

    // Accel Control IO
    val enable = Output(Bool())
    val done = Input(Bool())
    val reset = Output(Bool())

    // Accel Scalar IO
    val argIns = Output(Vec(numArgIns, UInt(w.W)))
    val argOuts = Vec(numArgOuts, Flipped(Decoupled((UInt(w.W)))))
    val argOutLoopbacks = Output(Vec(1 max argOutLoopbacksMap.toList.length, UInt(w.W)))

    // Accel memory IO
    val memStreams = new AppStreams(loadStreamInfo, storeStreamInfo)
    // TODO: need to add memory stream support

    // External enable
    val externalEnable = Input(Bool()) // For AWS, enable comes in as input to top module

    // Accel stream IO
//    val genericStreams = new GenericStreams(streamInsInfo, streamOutsInfo)
  })

  // Common Fringe
  val fringeCommon = Module(new Fringe(w, numArgIns, numArgOuts, numArgIOs,
                                        numChannels, numArgInstrs, argOutLoopbacksMap, loadStreamInfo,
                                        storeStreamInfo, streamInsInfo, streamOutsInfo,
                                        blockingDRAMIssue, axiParams))

  // Connect to Avalon Slave
  fringeCommon.reset := reset
  fringeCommon.io.raddr := io.S_AVALON.address
  fringeCommon.io.wen := io.S_AVALON.write
  fringeCommon.io.waddr := io.S_AVALON.address
  fringeCommon.io.wdata := io.S_AVALON.writedata
  io.S_AVALON.readdata := fringeCommon.io.rdata

  io.enable := fringeCommon.io.enable
  fringeCommon.io.done := io.done

  if (io.argIns.length > 0) {
    io.argIns := fringeCommon.io.argIns
  }

  if (io.argOutLoopbacks.length > 0) {
    io.argOutLoopbacks := fringeCommon.io.argOutLoopbacks
  }

  if (io.argOuts.length > 0) {
    fringeCommon.io.argOuts.zip(io.argOuts) foreach { case (fringeArgOut, accelArgOut) =>
      fringeArgOut.bits := accelArgOut.bits
      fringeArgOut.valid := accelArgOut.valid
    }
  }

  // Memory interface
  io.memStreams <> fringeCommon.io.memStreams

  // AXI bridge
  io.M_AXI.zipWithIndex.foreach { case (maxi, i) =>
    val axiBridge = Module(new MAGToAXI4Bridge(axiParams, fringeCommon.mags(i).streamTagWidth))
    axiBridge.io.in <> fringeCommon.io.dram(i)
    maxi <> axiBridge.io.M_AXI
  }
}
