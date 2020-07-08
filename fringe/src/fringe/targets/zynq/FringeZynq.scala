package fringe.targets.zynq

import chisel3._
import chisel3.util.Decoupled
import fringe._
import fringe.globals._
import fringe.templates.axi4._

/** Top module for Zynq FPGA shell
  * @param blockingDRAMIssue TODO: What is this?
  * @param axiLiteParams TODO: What is this?
  * @param axiParams TODO: What is this?
  */
class FringeZynq(
  val blockingDRAMIssue: Boolean,
  val axiLiteParams:     AXI4BundleParameters,
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
    // Host scalar interface
    val axil_s_clk = Input(Bool())
    val S_AXI = Flipped(new AXI4Lite(axiLiteParams))

    // DRAM interface
    val M_AXI = Vec(NUM_CHANNELS, new AXI4Inlined(axiParams))

    // AXI Debuggers
    val TOP_AXI = new AXI4Probe(axiLiteParams)
    val DWIDTH_AXI = new AXI4Probe(axiLiteParams)
    val PROTOCOL_AXI = new AXI4Probe(axiLiteParams)
    val CLOCKCONVERT_AXI = new AXI4Probe(axiLiteParams)

    // Accel Control IO
    val enable = Output(Bool())
    val done = Input(Bool())
    val reset = Output(Bool())

    // Accel Scalar IO
    val argIns          = Output(Vec(NUM_ARG_INS, UInt(TARGET_W.W)))
    val argOuts         = Vec(NUM_ARG_OUTS, Flipped(Decoupled(UInt(TARGET_W.W))))
    val argEchos         = Output(Vec(NUM_ARG_OUTS, UInt(TARGET_W.W)))


    // Accel memory IO
    val memStreams = new AppStreams(LOAD_STREAMS, STORE_STREAMS, GATHER_STREAMS, SCATTER_STREAMS)
    val heap = Vec(numAllocators, new HeapIO())

    // External enable
    val externalEnable = Input(Bool()) // For AWS, enable comes in as input to top module

    // Accel stream IO
//    val genericStreams = new GenericStreams(streamInsInfo, streamOutsInfo)
  })

  io <> DontCare

  // Common Fringe
  val fringeCommon = Module(new Fringe(blockingDRAMIssue, axiParams))
  fringeCommon.io <> DontCare

//  fringeCommon.io.TOP_AXI <> io.TOP_AXI
//  fringeCommon.io.DWIDTH_AXI <> io.DWIDTH_AXI
//  fringeCommon.io.PROTOCOL_AXI <> io.PROTOCOL_AXI
//  fringeCommon.io.CLOCKCONVERT_AXI <> io.CLOCKCONVERT_AXI

  // AXI-lite bridge
  if (target.isInstanceOf[targets.kcu1500.KCU1500]) {
    val axiLiteBridge = Module(new AXI4LiteToRFBridgeKCU1500(ADDR_WIDTH, DATA_WIDTH))
    axiLiteBridge.io.S_AXI <> io.S_AXI
    axiLiteBridge.clock := io.axil_s_clk.asClock()

    fringeCommon.reset := reset.toBool
    fringeCommon.io.raddr := axiLiteBridge.io.raddr
    fringeCommon.io.wen   := axiLiteBridge.io.wen
    fringeCommon.io.waddr := axiLiteBridge.io.waddr
    fringeCommon.io.wdata := axiLiteBridge.io.wdata
    axiLiteBridge.io.rdata := fringeCommon.io.rdata
  }
  else if (target.isInstanceOf[targets.zynq.Zynq] || target.isInstanceOf[targets.zedboard.ZedBoard]) {
    val axiLiteBridge = Module(new AXI4LiteToRFBridge(ADDR_WIDTH, DATA_WIDTH))
    axiLiteBridge.io.S_AXI <> io.S_AXI
    axiLiteBridge.clock := io.axil_s_clk.asClock()

    // fringeCommon.reset := ~reset.toBool
    fringeCommon.io.raddr := axiLiteBridge.io.raddr
    fringeCommon.io.wen   := axiLiteBridge.io.wen
    fringeCommon.io.waddr := axiLiteBridge.io.waddr
    fringeCommon.io.wdata := axiLiteBridge.io.wdata
    axiLiteBridge.io.rdata := fringeCommon.io.rdata
  }
  else if (target.isInstanceOf[targets.zcu.ZCU]) {
    val axiLiteBridge = Module(new AXI4LiteToRFBridgeZCU(ADDR_WIDTH, DATA_WIDTH))
    axiLiteBridge.io.S_AXI <> io.S_AXI
    axiLiteBridge.clock := io.axil_s_clk.asClock()

    // fringeCommon.reset := ~reset.toBool
    fringeCommon.io.raddr := axiLiteBridge.io.raddr
    fringeCommon.io.wen   := axiLiteBridge.io.wen
    fringeCommon.io.waddr := axiLiteBridge.io.waddr
    fringeCommon.io.wdata := axiLiteBridge.io.wdata
    axiLiteBridge.io.rdata := fringeCommon.io.rdata
  
  }

  fringeCommon.io.aws_top_enable := io.externalEnable

  io.enable := fringeCommon.io.enable
  fringeCommon.io.done := io.done
  fringeCommon.reset := reset.toBool
  // fringeCommon.reset := reset.toBool
  io.reset := fringeCommon.io.reset

  io.argIns := fringeCommon.io.argIns
  fringeCommon.io.argOuts <> io.argOuts
  // io.argIOIns := fringeCommon.io.argIOIns
  // fringeCommon.io.argIOOuts <> io.argIOOuts

  io.memStreams <> fringeCommon.io.memStreams
  io.heap <> fringeCommon.io.heap

  // AXI bridge
  io.M_AXI.zipWithIndex.foreach { case (maxi, i) =>
    val axiBridge = Module(new MAGToAXI4Bridge(axiParams))
    axiBridge.io.in <> fringeCommon.io.dram(i)
    maxi <> axiBridge.io.M_AXI
  }
  // TODO: This was an attempt to resolve issue #276 but it didn't work
  if (globals.loadStreamInfo.size == 0 && globals.storeStreamInfo.size == 0) {
    io.M_AXI.foreach(_.AWVALID := false.B)
    io.M_AXI.foreach(_.ARVALID := false.B)
  }
}
