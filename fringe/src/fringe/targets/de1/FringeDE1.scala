package fringe.targets.de1

import chisel3._
import chisel3.util.{Decoupled, DecoupledIO}
import fringe.globals._
import fringe.templates.avalon.MAGToAvalonBridge
import fringe.templates.axi4._
import fringe.{AppStreams, Fringe, HeapIO}

class FringeDE1(blockingDRAMIssue: Boolean,
                avalonLiteParams: AvalonBundleParameters,
                avalonParams: AvalonBundleParameters)
    extends Module {

  val numRegs: Int = NUM_ARG_INS + NUM_ARG_OUTS + NUM_ARG_IOS + 2
  val commandReg = 0
  val statusReg = 1

  val v = 16 // Number of words in the same stream
  val numOutstandingBursts = 1024 // Picked arbitrarily

  val io = IO(new Bundle {
    val S_AVALON = new AvalonSlave(avalonLiteParams)

    // TODO: Add M_AVALON Later...
    scala.Console.println("Getting M_AVALON tested...")
    val M_AVALON: Vec[AvalonMaster] =
      Vec(NUM_CHANNELS, new AvalonMaster(avalonParams))

    val TOP_M_AVALON = new AvalonProbe(avalonParams)

    val enable = Output(Bool())
    val done = Input(Bool())
    val reset = Output(Bool())

    val argIns = Output(Vec(NUM_ARG_INS, UInt(TARGET_W.W)))
    val argOuts: Vec[DecoupledIO[UInt]] =
      Vec(NUM_ARG_OUTS, Flipped(Decoupled(UInt(TARGET_W.W))))
    val argEchos = Output(Vec(NUM_ARG_OUTS, UInt(TARGET_W.W)))

    // TODO: Accel Avalon memory IO
    val memStreams = new AppStreams(LOAD_STREAMS,
                                    STORE_STREAMS,
                                    GATHER_STREAMS,
                                    SCATTER_STREAMS)
    val heap: Vec[HeapIO] = Vec(numAllocators, new HeapIO())

    // External enable
    val externalEnable = Input(Bool())
  })

  io <> DontCare

  // Common Fringe
  val fringeCommon = Module(new Fringe(blockingDRAMIssue, avalonParams, isAvalon = true))
  fringeCommon.io.raddr := io.S_AVALON.address
  fringeCommon.io.wen := io.S_AVALON.write
  fringeCommon.io.waddr := io.S_AVALON.address
  fringeCommon.io.wdata := io.S_AVALON.writeData
  io.S_AVALON.readData := fringeCommon.io.rdata

  io.M_AVALON := DontCare
  fringeCommon.io.dram.foreach(m => m := DontCare)
  fringeCommon.io.aws_top_enable := DontCare

  // TODO: Factor out the case for avalon debugging...
  fringeCommon.io.TOP_M_AVALON <> io.TOP_M_AVALON
  fringeCommon.io.TOP_AXI <> DontCare
  fringeCommon.io.DWIDTH_AXI <> DontCare
  fringeCommon.io.PROTOCOL_AXI <> DontCare
  fringeCommon.io.CLOCKCONVERT_AXI <> DontCare

  // Fringe connections
  io.enable := fringeCommon.io.enable
  fringeCommon.io.done := io.done
  fringeCommon.reset := reset.toBool
  io.reset := fringeCommon.io.reset

  io.argIns := fringeCommon.io.argIns
  fringeCommon.io.argOuts <> io.argOuts

  // TODO: Do memStream management
  io.memStreams <> fringeCommon.io.memStreams
  io.heap <> fringeCommon.io.heap

  // TODO: This might work better with an Avalon stream...
  // AXI bridge
  io.M_AVALON.zipWithIndex.foreach {
    case (m, i) =>
      val avalonBridge = Module(new MAGToAvalonBridge(avalonParams))
      avalonBridge.io.in <> fringeCommon.io.dram(i)
      m <> avalonBridge.io.M_AVALON
      m.chipSelect := io.enable
  }
  // TODO: Seems that this one is not helping much?
//  if (globals.loadStreamInfo.size == 0 && globals.storeStreamInfo.size == 0) {
//    io.M_AXI.foreach(_.AWVALID := false.B)
//    io.M_AXI.foreach(_.ARVALID := false.B)
//  }
}
