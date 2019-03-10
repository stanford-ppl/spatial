package fringe.targets.arria10

import chisel3._
import chisel3.util._
import fringe.globals._
import fringe.templates.axi4._
import fringe.{Fringe, StreamParInfo, AppStreams, HeapIO}
import fringe.utils.log2Up

/** Top module for Arria 10 FPGA shell
  * @param blockingDRAMIssue TODO: What is this?
  * @param axiLiteParams TODO: What is this?
  * @param axiParams: TODO: What is this?
  */
class FringeArria10 (
  val blockingDRAMIssue: Boolean,
  val axiLiteParams:     AXI4BundleParameters,
  val axiParams:         AXI4BundleParameters
) extends Module {
  private val w = DATA_WIDTH
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
//    val M_AXI = Vec(NUM_CHANNELS, new AXI4Inlined(axiParams))
    val M_AVALON = Vec(NUM_CHANNELS, new AvalonMaster(axiParams))

    // AXI Debuggers
    val TOP_AXI = new AXI4Probe(axiLiteParams)
    val DWIDTH_AXI = new AXI4Probe(axiLiteParams)
    val PROTOCOL_AXI = new AXI4Probe(axiLiteParams)
    val CLOCKCONVERT_AXI = new AXI4Probe(axiLiteParams)

    // Accel Control IO
    val enable = Output(Bool())
    val done   = Input(Bool())
    val reset  = Output(Bool())

    // Accel Scalar IO
    val argIns          = Output(Vec(NUM_ARG_INS, UInt(w.W)))
    val argOuts         = Vec(NUM_ARG_OUTS, Flipped(Decoupled(UInt(w.W))))
    val argEchos         = Output(Vec(NUM_ARG_OUTS, UInt(w.W)))

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

  fringeCommon.io.TOP_AXI <> io.TOP_AXI
  fringeCommon.io.DWIDTH_AXI <> io.DWIDTH_AXI
  fringeCommon.io.PROTOCOL_AXI <> io.PROTOCOL_AXI
  fringeCommon.io.CLOCKCONVERT_AXI <> io.CLOCKCONVERT_AXI

  // Connect to Avalon Slave

  // Avalon-slave bridge
  fringeCommon.io.raddr := io.S_AVALON.address
  fringeCommon.io.wen := io.S_AVALON.write
  fringeCommon.io.waddr := io.S_AVALON.address
  fringeCommon.io.wdata := io.S_AVALON.writedata
  io.S_AVALON.readdata := fringeCommon.io.rdata

  fringeCommon.io.aws_top_enable := io.externalEnable

  io.enable := fringeCommon.io.enable
  fringeCommon.io.done := io.done
  fringeCommon.reset := reset.toBool
  io.reset := fringeCommon.io.reset

  io.argIns := fringeCommon.io.argIns
  fringeCommon.io.argOuts <> io.argOuts

//  if (io.argOuts.length > 0) {
//    fringeCommon.io.argOuts.zip(io.argOuts) foreach { case (fringeArgOut, accelArgOut) =>
//      fringeArgOut.bits := accelArgOut.bits
//      fringeArgOut.valid := accelArgOut.valid
//    }
//  }

  // Memory interface
  io.memStreams <> fringeCommon.io.memStreams
  io.heap <> fringeCommon.io.heap

  // AXI bridge
//  io.M_AXI.zipWithIndex.foreach { case (maxi, i) =>
//    val axiBridge = Module(new MAGToAXI4Bridge(axiParams))
//    axiBridge.io.in <> fringeCommon.io.dram(i)
//    maxi <> axiBridge.io.M_AXI
//  }

  // AVALON Master bridge
  io.M_AVALON.zipWithIndex.foreach { case (maxi, i) =>
    val avalonBridge = Module(new MAGToAvalonBridge(axiParams))
    avalonBridge.io.in <> fringeCommon.io.dram(i)
    maxi <> avalonBridge.io.M_AVALON
  }
}
