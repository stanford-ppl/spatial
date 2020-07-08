package fringe.targets.cxp

import chisel3._
import chisel3.util._
import fringe._
import fringe.globals._
import fringe.templates.euresys._
import fringe.templates.axi4._

/** Top module for CXP FPGA shell
  * @param blockingDRAMIssue TODO: What is this?
  * @param axiParams TODO: What is this?
  */
class FringeCXP(
  val blockingDRAMIssue: Boolean,
  val axiParams:         AXI4BundleParameters
) extends Module {

  val numRegs = NUM_ARG_INS + NUM_ARG_OUTS + NUM_ARG_IOS + 2  // (command, status registers)

  val commandReg = 0  // TODO: These vals are used in test only, logic below does not use them.
  val statusReg = 1   //       Changing these values alone has no effect on the logic below.

  // Some constants (mostly MAG-related) that will later become module parameters
  val v = 16 // Number of words in the same stream
  val numOutstandingBursts = 1024  // Picked arbitrarily
  val burstSizeBytes = 64
  // val d = 16 // FIFO depth: Controls FIFO sizes for address, size, and wdata. Rdata is not buffered

  val io = IO(new Bundle {
    // DRAM interface
    val M_AXI = Vec(NUM_CHANNELS, new AXI4Inlined(axiParams))

    // Accel Control IO
    val enable = Output(Bool())
    val done = Input(Bool())
    val reset = Output(Bool())

    // Accel Scalar IO
    val argIns          = Output(Vec(NUM_ARG_INS, UInt(TARGET_W.W)))
    val argOuts         = Vec(NUM_ARG_OUTS, Flipped(Decoupled(UInt(TARGET_W.W))))
    val argEchos         = Output(Vec(NUM_ARG_OUTS, UInt(TARGET_W.W)))

    // Control signals from outside world
    val ctrl_addr = Input(UInt(16.W))
    val ctrl_data_in_ce = Input(Bool())
    val ctrl_data_in = Input(UInt(32.W))
    val ctrl_data_out = Output(UInt(32.W))

    // Accel memory IO
    val memStreams = new AppStreams(LOAD_STREAMS, STORE_STREAMS, GATHER_STREAMS, SCATTER_STREAMS)

    val heap = Vec(numAllocators, new HeapIO())

  })

  io <> DontCare
  // Common Fringe
  val fringeCommon = Module(new Fringe(blockingDRAMIssue, axiParams))
  fringeCommon.io <> DontCare

  // AXI-lite bridge

  // fringeCommon.reset := ~reset.toBool
  fringeCommon.io.raddr := io.ctrl_addr
  fringeCommon.io.wen   := io.ctrl_data_in_ce
  fringeCommon.io.waddr := io.ctrl_addr
  fringeCommon.io.wdata := io.ctrl_data_in
  io.ctrl_data_out := fringeCommon.io.rdata

  io.enable := fringeCommon.io.enable
  fringeCommon.io.done := io.done
  fringeCommon.reset := reset.toBool
  io.reset := fringeCommon.io.reset

  io.argIns := fringeCommon.io.argIns
  fringeCommon.io.argOuts <> io.argOuts

  io.memStreams <> fringeCommon.io.memStreams
  io.heap <> fringeCommon.io.heap

  // AXI bridge
  io.M_AXI.zipWithIndex.foreach { case (maxi, i) =>
    val axiBridge = Module(new MAGToAXI4Bridge(axiParams))
    axiBridge.io.in <> fringeCommon.io.dram(i)
    maxi <> axiBridge.io.M_AXI
  }


}
