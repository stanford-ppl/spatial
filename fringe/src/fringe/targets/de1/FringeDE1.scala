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

  // TODO: Later we need to override some of FringeZynq's defs.
  val io = IO(new Bundle {
    val S_AVALON = new AvalonSlave(avalonLiteParams)
    val M_AXI: Vec[AXI4Inlined] =
      Vec(NUM_CHANNELS, new AXI4Inlined(avalonParams))

    // TODO: Add Avalon probes for board debugging.
    val enable = Output(Bool())
    val done = Input(Bool())
    val reset = Output(Bool())

    val argIns = Output(Vec(NUM_ARG_INS, UInt(TARGET_W.W)))
    val argOuts: Vec[DecoupledIO[UInt]] =
      Vec(NUM_ARG_OUTS, Flipped(Decoupled(UInt(TARGET_W.W))))
    val argEchos = Output(Vec(NUM_ARG_OUTS, UInt(TARGET_W.W)))

    // Accel memory IO
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
  fringeCommon.io <> DontCare

  // TODO: Add debug probes

  // Fringe connections
  io.enable := fringeCommon.io.enable
  fringeCommon.io.done := io.done
  fringeCommon.reset := reset.toBool // TODO: This might be an inverted signal...
  io.reset := fringeCommon.io.reset

  io.argIns := fringeCommon.io.argIns
  fringeCommon.io.argOuts <> io.argOuts

  // TODO: Do memStream management
//  io.memStreams <> fringeCommon.io.memStreams
//  io.heap <> fringeCommon.io.heap

}