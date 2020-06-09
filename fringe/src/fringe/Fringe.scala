package fringe

import chisel3._
import chisel3.util._
import fringe.globals._
import fringe.targets.asic.ASIC
import fringe.targets.vcs.VCS
import fringe.targets.aws.AWS_Sim
import fringe.utils._
import fringe.templates.axi4._
import fringe.templates.dramarbiter.DRAMArbiter
import fringe.templates.heap.DRAMHeap
import fringe.templates.counters.FringeCounter
import fringe.templates.memory.RegFile

/** Top module for FPGA shell
  * @param blockingDRAMIssue TODO: What is this?
  * @param axiParams TODO: What is this?
  */
class Fringe(blockingDRAMIssue: Boolean, axiParams: AXI4BundleParameters) extends Module {
  val commandReg = 0  // TODO: These vals are used in test only, logic below does not use them.
  val statusReg = 1   //       Changing these values alone has no effect on the logic below.

  // Some constants (mostly MAG-related) that will later become module parameters
  val regWidth = 64 // Force 64-bit registers

  class StatusReg extends Bundle {
    val sizeAddr = UInt((regWidth - 5).W)
    val allocDealloc = UInt(3.W)
    val timeout = Bool()
    val done = Bool()
  }

  val axiLiteParams = new AXI4BundleParameters(64, 512, 1)

  val io = IO(new Bundle {
    // Host scalar interface
    val raddr = Input(UInt(ADDR_WIDTH.W))
    val wen  = Input(Bool())
    val waddr = Input(UInt(ADDR_WIDTH.W))
    val wdata = Input(Bits(regWidth.W))
    val rdata = Output(Bits(regWidth.W))

    // Accel Control IO
    val enable = Output(Bool())
    val done = Input(Bool())
    val reset = Output(Bool())

    // Accel Scalar IO
    val argIns          = Output(Vec(NUM_ARG_INS, UInt(regWidth.W)))
    val argOuts         = Vec(NUM_ARG_OUTS, Flipped(Decoupled(UInt(regWidth.W))))
    val argEchos         = Output(Vec(NUM_ARG_OUTS, UInt(regWidth.W)))

    // Accel memory IO
    val memStreams = new AppStreams(LOAD_STREAMS, STORE_STREAMS, GATHER_STREAMS, SCATTER_STREAMS)
    val dram = Vec(NUM_CHANNELS, new DRAMStream(DATA_WIDTH, WORDS_PER_STREAM))
    val heap = Vec(numAllocators, new HeapIO())

    // AXI Debuggers
    val TOP_AXI = new AXI4Probe(axiLiteParams)
    val DWIDTH_AXI = new AXI4Probe(axiLiteParams)
    val PROTOCOL_AXI = new AXI4Probe(axiLiteParams)
    val CLOCKCONVERT_AXI = new AXI4Probe(axiLiteParams)

    //Accel stream IO
//    val genericStreamsAccel = Flipped(new GenericStreams(streamInsInfo, streamOutsInfo))
//    val genericStreamOutTop = StreamOut(StreamParInfo(w, 1))
//    val genericStreamInTop = StreamIn(StreamParInfo(w, 1))

    // Debug
    val aws_top_enable = Input(Bool()) // For AWS, enable comes in as input to top module

    // val dbg = new DebugSignals
  })

  io.argEchos := DontCare
  io.argOuts <> DontCare

//  println(s"[Fringe] loadStreamInfo: $LOAD_STREAMS, storeStreamInfo: $STORE_STREAMS")
  val assignment: List[List[Int]] = channelAssignment.assignments
  val debugChannelID = 0
  val dramArbs = List.tabulate(NUM_CHANNELS){ i =>
    val channelAssignment = assignment(i)
    val (loads, absStores) = channelAssignment.partition(_ < NUM_LOAD_STREAMS)
    val stores = absStores.map{ _ - NUM_LOAD_STREAMS } // Compute indices into stores array

    println(s"[Fringe] Creating Stream Arbiter $i, assignment: $channelAssignment, Loads: $loads, Stores: $stores")
    val loadStreams = loads.map{ io.memStreams.loads(_) }
    val storeStreams = stores.map{ io.memStreams.stores(_) }

    val dramArb = Module(new DRAMArbiter(
      loadStreamInfo,
      storeStreamInfo,
      gatherStreamInfo,
      scatterStreamInfo,
      axiParams,
      debugChannelID == i
    ))
    dramArb.io <> DontCare
    dramArb.io.app.loads.zip(loadStreams).foreach{ case (l, ls) => l <> ls }
    dramArb.io.app.stores.zip(storeStreams).foreach{ case (s, ss) => s <> ss }
    dramArb.io.app.gathers.zip(io.memStreams.gathers).foreach{ case (g, gs) => g <> gs }
    dramArb.io.app.scatters.zip(io.memStreams.scatters).foreach{ case (s, ss) => s <> ss }
    dramArb
  }

  val heap = Module(new DRAMHeap(numAllocators))
  heap.io.accel <> io.heap
  val hostHeapReq = heap.io.host(0).req
  val hostHeapResp = heap.io.host(0).resp

  val numDebugs = dramArbs(debugChannelID).numDebugs
  val numRegs = NUM_ARGS + 2 - NUM_ARG_INS + numDebugs // (command, status registers)

  // Scalar, command, and status register file
  val regs = Module(new RegFile(regWidth, numRegs, numArgIns+2, numArgOuts+1+numDebugs, numArgIOs))
  regs.io <> DontCare
  regs.io.raddr := io.raddr
  regs.io.waddr := io.waddr
  regs.io.wen := io.wen
  regs.io.wdata := io.wdata
  // TODO: Fix this bug asap so that the axi42rf bridge verilog anticipates the 1 cycle delay of the data
  val bug239_hack = !(globals.target.isInstanceOf[AWS_Sim] || globals.target.isInstanceOf[VCS] || globals.target.isInstanceOf[ASIC])
  if (bug239_hack) {
    io.rdata := regs.io.rdata
  }
  else {
    io.rdata := chisel3.util.ShiftRegister(regs.io.rdata, 1)
  }
  

  val command = regs.io.argIns(0)
  val curStatus = regs.io.argIns(1).asTypeOf(new StatusReg)
  val localEnable = if (globals.perpetual) 1.U else command(0) === 1.U & !curStatus.done
  val localReset = command(1) === 1.U | reset.toBool
  io.enable := localEnable
  io.reset := localReset
  regs.io.reset := localReset
  regs.reset := reset.toBool

  // Hardware time out (for debugging)
  val timeoutCycles = 12000000000L
  val timeoutCtr = Module(new FringeCounter(40))
  timeoutCtr.io <> DontCare
  timeoutCtr.io.reset := 0.U 
  timeoutCtr.io.saturate := 1.U
  timeoutCtr.io.max := timeoutCycles.U
  timeoutCtr.io.stride := 1.U
  timeoutCtr.io.enable := localEnable

  io.argIns.zipWithIndex.foreach{case (p, i) => p := regs.io.argIns(i+2)}

  val depulser = Module(new Depulser())
  depulser.io <> DontCare
  depulser.io.in := io.done | timeoutCtr.io.done
  depulser.io.rst := ~command

  val status = Wire(Valid(new StatusReg))
  status.valid := depulser.io.out | pulse(hostHeapReq.valid)
  status.bits.done := Mux(depulser.io.out, command(0) & depulser.io.out.asUInt, curStatus.done)
  status.bits.timeout := Mux(depulser.io.out, command(0) & timeoutCtr.io.done, curStatus.timeout)
  status.bits.allocDealloc := Mux(hostHeapReq.valid, Mux(hostHeapReq.bits.allocDealloc, 1.U, 2.U), 0.U)
  status.bits.sizeAddr := Mux(hostHeapReq.valid, hostHeapReq.bits.sizeAddr, 0.U)

  regs.io.argOuts.zipWithIndex.foreach { case (argOutReg, i) =>
    // Manually assign bits and valid, because direct assignment with :=
    // is causing issues with chisel compilation due to the 'ready' signal
    // which we do not care about
    if (i == 0) { // statusReg: First argOut
      argOutReg.valid := status.valid
      argOutReg.bits := status.bits.asUInt
    }
    else if (i <= numArgOuts) {
      io.argEchos(i-1) := regs.io.argEchos(i)
      argOutReg.bits := io.argOuts(i-1).bits
      argOutReg.valid := io.argOuts(i-1).valid
    }
    else { // MAG debug regs
      argOutReg.bits := dramArbs(debugChannelID).io.debugSignals(i-numArgOuts-1)
      argOutReg.valid := 1.U
    }
  }


  // Memory address generator
  dramArbs.foreach { _.io.reset := localReset }
  dramArbs.foreach { _.reset := localReset }
  if (target.isInstanceOf[targets.aws.AWS_F1] || target.isInstanceOf[targets.aws.AWS_Sim]) {
    dramArbs.foreach { _.io.enable := io.aws_top_enable }
  }
  else {
    dramArbs.foreach { _.io.enable := localEnable }
  }

  dramArbs.zip(io.dram) foreach { case (dramarb, d) => dramarb.io.dram <> d }

  dramArbs(debugChannelID).io.TOP_AXI <> io.TOP_AXI
  dramArbs(debugChannelID).io.DWIDTH_AXI <> io.DWIDTH_AXI
  dramArbs(debugChannelID).io.PROTOCOL_AXI <> io.PROTOCOL_AXI
  dramArbs(debugChannelID).io.CLOCKCONVERT_AXI <> io.CLOCKCONVERT_AXI

  val alloc = curStatus.allocDealloc === 3.U
  val dealloc = curStatus.allocDealloc === 4.U
  hostHeapResp.valid := pulse(alloc | dealloc)
  hostHeapResp.bits.allocDealloc := alloc
  hostHeapResp.bits.sizeAddr := curStatus.sizeAddr

  // io.dbg <> mags(debugChannelID).io.dbg

  // In simulation, streams are just pass through
  // TODO: Make these connections with hashmap generated by codegen
//  if (io.genericStreamsAccel.outs.length == 1) {
//    io.genericStreamsAccel.ins(0) <> io.genericStreamInTop
//  }
//  if (io.genericStreamsAccel.ins.length == 1) {
//    io.genericStreamsAccel.outs(0) <> io.genericStreamOutTop
//  }
}
