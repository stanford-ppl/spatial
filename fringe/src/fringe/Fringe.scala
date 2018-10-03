package fringe

import chisel3._
import chisel3.util._
import fringe.globals._
import fringe.targets.asic.ASIC
import fringe.targets.vcs.VCS
import fringe.utils._
import fringe.templates.axi4._
import fringe.templates.mag.{MAGCore, DRAMHeap}
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
  val v = WORDS_PER_STREAM
  val numOutstandingBursts = 1024  // Picked arbitrarily
  val burstSizeBytes = 64
  val d = 64        // FIFO depth: Controls FIFO sizes for address, size, and wdata
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
    val argOutLoopbacks = Output(Vec(NUM_ARG_LOOPS, UInt(regWidth.W)))

    // Accel memory IO
    val memStreams = new AppStreams(LOAD_STREAMS, STORE_STREAMS)
    val dram = Vec(NUM_CHANNELS, new DRAMStream(DATA_WIDTH, WORDS_PER_STREAM))
    val heap = new HeapIO(numAllocators)

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

  println(s"[Fringe] loadStreamInfo: $LOAD_STREAMS, storeStreamInfo: $STORE_STREAMS")
  val assignment: List[List[Int]] = channelAssignment.assignments
  val debugChannelID = 0
  val mags = List.tabulate(NUM_CHANNELS){ i =>
    val channelAssignment = assignment(i)
    val (loads, absStores) = channelAssignment.partition(_ < NUM_LOAD_STREAMS)
    val stores = absStores.map{ _ - NUM_LOAD_STREAMS } // Compute indices into stores array

    println(s"[Fringe] Creating MAG $i, assignment: $channelAssignment, Loads: $loads, Stores: $stores")
    val loadStreams = loads.map{ io.memStreams.loads(_) }
    val storeStreams = stores.map{ io.memStreams.stores(_) }

    val mag = Module(new MAGCore(
      w = DATA_WIDTH,
      d = d,
      v = WORDS_PER_STREAM,
      loadStreamInfo  = LOAD_STREAMS,
      storeStreamInfo = STORE_STREAMS,
      numOutstandingBursts,
      burstSizeBytes,
      axiParams,
      debugChannelID == i
    ))
    mag.io.app.loads.zip(loadStreams).foreach{ case (l, ls) => l <> ls }
    mag.io.app.stores.zip(storeStreams).foreach{ case (s, ss) => s <> ss }
    mag
  }

  val heap = Module(new DRAMHeap(numAllocators))
  heap.io.accel <> io.heap
  val hostHeapReq = heap.io.host.req(0)
  val hostHeapResp = heap.io.host.resp(0)

  val numDebugs = mags(debugChannelID).numDebugs
  val numRegs = NUM_ARGS + 2 - NUM_ARG_INS + numDebugs // (command, status registers)

  // Scalar, command, and status register file
  val regs = Module(new RegFile(regWidth, numRegs, numArgIns+2, numArgOuts+1+numDebugs, numArgIOs, argOutLoopbacksMap))
  regs.io.raddr := io.raddr
  regs.io.waddr := io.waddr
  regs.io.wen := io.wen
  regs.io.wdata := io.wdata
  // TODO: Fix this bug asap so that the axi42rf bridge verilog anticipates the 1 cycle delay of the data
  val bug239_hack = !(globals.target.isInstanceOf[VCS] || globals.target.isInstanceOf[ASIC])
  if (bug239_hack) {
    io.rdata := regs.io.rdata
  }
  else {
    io.rdata := chisel3.util.ShiftRegister(regs.io.rdata, 1)
  }
  

  val command = regs.io.argIns(0)
  val curStatus = regs.io.argIns(1).asTypeOf(new StatusReg)
  val localEnable = command(0) === 1.U & !curStatus.done
  val localReset = command(1) === 1.U | reset.toBool
  io.enable := localEnable
  io.reset := localReset
  regs.io.reset := localReset
  regs.reset := reset.toBool

  // Hardware time out (for debugging)
  val timeoutCycles = 12000000000L
  val timeoutCtr = Module(new FringeCounter(40))
  timeoutCtr.io.reset := 0.U 
  timeoutCtr.io.saturate := 1.U
  timeoutCtr.io.max := timeoutCycles.U
  timeoutCtr.io.stride := 1.U
  timeoutCtr.io.enable := localEnable

  io.argIns.zipWithIndex.foreach{case (p, i) => p := regs.io.argIns(i+2)}

  val depulser = Module(new Depulser())
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
      argOutReg.bits := io.argOuts(i-1).bits
      argOutReg.valid := io.argOuts(i-1).valid
    }
    else { // MAG debug regs
      argOutReg.bits := mags(debugChannelID).io.debugSignals(i-numArgOuts-1)
      argOutReg.valid := 1.U
    }
  }

  io.argOutLoopbacks := regs.io.argOutLoopbacks

  // Memory address generator
  val magConfig = Wire(MAGOpcode())
  magConfig.scatterGather := false.B
  mags.foreach { _.io.config := magConfig }
  mags.foreach { _.io.reset := localReset }
  mags.foreach { _.reset := localReset }
  if (target.isInstanceOf[targets.aws.AWS_F1]) {
    mags.foreach { _.io.enable := io.aws_top_enable }
  }
  else {
    mags.foreach { _.io.enable := localEnable }
  }

//  mag.io.app <> io.memStreams

  mags.zip(io.dram) foreach { case (mag, d) => mag.io.dram <> d }

  mags(debugChannelID).io.TOP_AXI <> io.TOP_AXI
  mags(debugChannelID).io.DWIDTH_AXI <> io.DWIDTH_AXI
  mags(debugChannelID).io.PROTOCOL_AXI <> io.PROTOCOL_AXI
  mags(debugChannelID).io.CLOCKCONVERT_AXI <> io.CLOCKCONVERT_AXI

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
