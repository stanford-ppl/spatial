package fringe.targets.de1

import chisel3._
import chisel3.util.{Decoupled, DecoupledIO}
import fringe.{AppStreams, Fringe, HeapIO}
import fringe.globals._
import fringe.templates.axi4._

class FringeDE1(blockingDRAMIssue: Boolean,
                avalonLiteParams: AvalonBundleParameters,
                avalonParams: AvalonBundleParameters)
    extends Module {

  val numRegs: Int = NUM_ARG_INS + NUM_ARG_OUTS + NUM_ARG_IOS + 2
  val commandReg = 0
  val statusReg = 1

  val v = 16 // Number of words in the same stream
  val numOutstandingBursts = 1024 // Picked arbitrarily
  val burstSizeBytes = 64

  val io = IO(new Bundle {
    val S_AVALON = new AvalonSlave(avalonLiteParams)

    // TODO: Add M_AXI Later...
    val M_AXI: Vec[AXI4Inlined] =
      Vec(NUM_CHANNELS, new AXI4Inlined(avalonParams))

    // TODO: Add Avalon probes for board debugging.
    //  For now just add AXI to stop firrtl panicing

    val TOP_AXI = new AXI4Probe(avalonLiteParams)
    val DWIDTH_AXI = new AXI4Probe(avalonLiteParams)
    val PROTOCOL_AXI = new AXI4Probe(avalonLiteParams)
    val CLOCKCONVERT_AXI = new AXI4Probe(avalonLiteParams)

    val enable = Output(Bool())
    val done = Input(Bool())
    val reset = Output(Bool())

    val argIns = Output(Vec(NUM_ARG_INS, UInt(TARGET_W.W)))
    val argOuts: Vec[DecoupledIO[UInt]] =
      Vec(NUM_ARG_OUTS, Flipped(Decoupled(UInt(TARGET_W.W))))
    val argEchos = Output(Vec(NUM_ARG_OUTS, UInt(TARGET_W.W)))

    // TODO: Accel memory IO
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
  val fringeCommon = Module(new Fringe(blockingDRAMIssue, avalonParams))
  fringeCommon.io.raddr := io.S_AVALON.address
  fringeCommon.io.wen := io.S_AVALON.write
  fringeCommon.io.waddr := io.S_AVALON.address
  fringeCommon.io.wdata := io.S_AVALON.writeData
  io.S_AVALON.readData := fringeCommon.io.rdata

  // TODO: Update M_AXI
  io.M_AXI := DontCare
  fringeCommon.io.dram.foreach(m => m := DontCare)
  fringeCommon.io.aws_top_enable := DontCare

  // TODO: Add debug probes
  fringeCommon.io.TOP_AXI <> io.TOP_AXI
  fringeCommon.io.DWIDTH_AXI <> io.DWIDTH_AXI
  fringeCommon.io.PROTOCOL_AXI <> io.PROTOCOL_AXI
  fringeCommon.io.CLOCKCONVERT_AXI <> io.CLOCKCONVERT_AXI

  // Fringe connections
  io.enable := fringeCommon.io.enable
  fringeCommon.io.done := io.done

  // TODO: This might be an inverted signal...
  fringeCommon.reset := reset.toBool
  io.reset := fringeCommon.io.reset

  io.argIns := fringeCommon.io.argIns
  fringeCommon.io.argOuts <> io.argOuts

  // TODO: Do memStream management
  io.memStreams <> fringeCommon.io.memStreams
  io.heap <> fringeCommon.io.heap
}
