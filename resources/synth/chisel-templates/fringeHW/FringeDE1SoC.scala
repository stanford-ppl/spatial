package fringe

import chisel3._
import chisel3.util._
import axi4._
import templates.Utils.log2Up

/**
 * FringeDE1SoC: Top module for FPGA shell
 * @param w: Word width
 * @param numArgIns: Number of input scalar arguments
 * @param numArgOuts: Number of output scalar arguments
 * @param numArgIOs: Number of Arg IOs
 */
class FringeDE1SoC(
  val w: Int,
  val numArgIns: Int,
  val numArgOuts: Int,
  val numArgIOs: Int,
  val numChannels: Int,
  val numArgInstrs: Int,
  val argOutLoopbacksMap: scala.collection.immutable.Map[Int,Int],
  val loadStreamInfo: List[StreamParInfo],
  val storeStreamInfo: List[StreamParInfo],
  val streamInsInfo: List[StreamParInfo],
  val streamOutsInfo: List[StreamParInfo],
  val blockingDRAMIssue: Boolean = false,
  val axiParams: AXI4BundleParameters
) extends Module {
  // Some constants (mostly MAG-related) that will later become module parameters
  val axiLiteParams = new AXI4BundleParameters(16, w, 1)
  val io = IO(new Bundle {
    // Host scalar interface
    val S_AVALON = new AvalonSlave(axiLiteParams)

    // Accel Control IO
    val enable = Output(Bool())
    val done = Input(Bool())
    val reset = Output(Bool())

    // Accel Scalar IO
    val argIns = Output(Vec(numArgIns, UInt(w.W)))
    val argOuts = Vec(numArgOuts, Flipped(Decoupled((UInt(w.W)))))
  })
  // Common Fringe
  val fringeCommon = Module(new Fringe(w, numArgIns, numArgOuts, numArgIOs, numChannels, numArgInstrs, argOutLoopbacksMap, loadStreamInfo, storeStreamInfo, streamInsInfo, streamOutsInfo, blockingDRAMIssue, axiParams))

  // Connect to Avalon Slave
  // Avalon is using reset and write_n
  fringeCommon.reset := reset
  fringeCommon.io.raddr := io.S_AVALON.address
  // TODO: This signal might be reconsidered since Arria10 takes a different control signal
  fringeCommon.io.wen   := ~io.S_AVALON.write & io.S_AVALON.chipselect

  fringeCommon.io.waddr := io.S_AVALON.address
  fringeCommon.io.wdata := io.S_AVALON.writedata
  io.S_AVALON.readdata := fringeCommon.io.rdata

  io.enable := fringeCommon.io.enable
  fringeCommon.io.done := io.done

  if (io.argIns.length > 0) {
    io.argIns := fringeCommon.io.argIns
  }

  if (io.argOuts.length > 0) {      
    fringeCommon.io.argOuts.zip(io.argOuts) foreach { case (fringeArgOut, accelArgOut) =>
      fringeArgOut.bits := accelArgOut.bits
      fringeArgOut.valid := accelArgOut.valid
    }
  }
}
