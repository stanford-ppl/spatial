package fringe

import chisel3._
import chisel3.util._
import templates._
import templates.Utils.log2Up
import axi4._

/**
 * Fringe: Top module for FPGA shell
 * @param w: Word width
 * @param numArgIns: Number of input scalar arguments
 * @param numArgOuts: Number of output scalar arguments
 */
class Fringe(
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
//  val numRegs = numArgIns + numArgOuts + 2 - numArgIOs // (command, status registers)
//  val addrWidth = log2Up(numRegs)
  val addrWidth = if (FringeGlobals.target == "zcu") 64 else 32

  val commandReg = 0  // TODO: These vals are used in test only, logic below does not use them.
  val statusReg = 1   //       Changing these values alone has no effect on the logic below.

  // Some constants (mostly MAG-related) that will later become module parameters
  val v = if (FringeGlobals.target == "vcs" || FringeGlobals.target == "asic") 64 else 16 // Number of words in the same stream
  val numOutstandingBursts = 1024  // Picked arbitrarily
  val burstSizeBytes = 64
  val d = 64        // FIFO depth: Controls FIFO sizes for address, size, and wdata
  val regWidth = 64 // Force 64-bit registers

  val axiLiteParams = new AXI4BundleParameters(64, 512, 1)

  val io = IO(new Bundle {
    // Host scalar interface
    val raddr = Input(UInt(addrWidth.W))
    val wen  = Input(Bool())
    val waddr = Input(UInt(addrWidth.W))
    val wdata = Input(Bits(regWidth.W))
    val rdata = Output(Bits(regWidth.W))

    // Accel Control IO
    val enable = Output(Bool())
    val done = Input(Bool())
    val reset = Output(Bool())

    // Accel Scalar IO
    val argIns = Output(Vec(numArgIns, UInt(regWidth.W)))
    val argOuts = Vec(numArgOuts, Flipped(Decoupled((UInt(regWidth.W)))))
    val argOutLoopbacks = Output(Vec(1 max argOutLoopbacksMap.toList.length, UInt(regWidth.W)))

    // Accel memory IO
    val memStreams = new AppStreams(loadStreamInfo, storeStreamInfo)
    val dram = Vec(numChannels, new DRAMStream(w, v))

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

  // Divide up channels within memStreams into 'numChannels' in round-robin fashion
  // The method below returns the IDs of load and store streams for each channel
  def getChannelAssignment(numStreams: Int, numChannels: Int) = {
    FringeGlobals.channelAssignment match {
      case AllToOne => List.tabulate(numChannels) { i => if (i == 0) (0 until numStreams).toList else List() }
      case BasicRoundRobin => List.tabulate(numChannels) { i => (i until numStreams by numChannels).toList }
      case ColoredRoundRobin => List.tabulate(numChannels) { i => loadStreamInfo.zipWithIndex.map{case (s, ii) => if (s.memChannel % numChannels == i) ii else -1}.filter{_ >= 0} ++ storeStreamInfo.zipWithIndex.map{case (s, ii) => if (s.memChannel % numChannels == i) ii + loadStreamInfo.size else -1}.filter{_ >= 0} }
      case AdvancedColored => List.tabulate(numChannels) { i => (i until numStreams by numChannels).toList } // TODO: Implement this!
    }
  }
      
  def getAppStreamIDs(assignment: List[Int]) = {
    val (loads, absStores) = if (assignment.indexWhere { _ >= loadStreamInfo.size } == -1) (assignment, List[Int]()) // No stores for this channel
                          else assignment.splitAt(assignment.indexWhere { _ >= loadStreamInfo.size })
    val stores = absStores.map { _ - loadStreamInfo.size } // Compute indices into stores array
    (loads, stores)
  }
      
  println(s"[Fringe] loadStreamInfo: $loadStreamInfo, storeStreamInfo: $storeStreamInfo")
  val numStreams = loadStreamInfo.size + storeStreamInfo.size
  val assignment = getChannelAssignment(numStreams, numChannels)
  val mags = List.tabulate(numChannels) { i =>
    val channelAssignment = assignment(i)
    val (loadStreamIDs, storeStreamIDs) = getAppStreamIDs(channelAssignment)

    println(s"[Fringe] Creating MAG $i, assignment: $channelAssignment, loadStreamIDs: $loadStreamIDs, storeStreamIDs: $storeStreamIDs")
    val linfo = if (loadStreamIDs.size == 0) List(StreamParInfo(w, 16, 0, false)) else loadStreamIDs.map { loadStreamInfo(_) }
    val sinfo = if (storeStreamIDs.size == 0) List(StreamParInfo(w, 16, 0, false)) else storeStreamIDs.map {storeStreamInfo(_) }
    val loadStreams = loadStreamIDs.map { io.memStreams.loads(_) }
    val storeStreams = storeStreamIDs.map { io.memStreams.stores(_) }

    val mag = Module(new MAGCore(w, d, v, linfo, sinfo, numOutstandingBursts, burstSizeBytes, axiParams, debugChannelID == i))
    mag.io.app.loads.zip(loadStreams) foreach { case (m, ls) =>
        // Instantiate FIFO for load.rdata, wire between MAG and interface
        val fifo = Module(new FIFOCore(m.rdata.bits.head.cloneType, 4*m.rdata.bits.size, m.rdata.bits.size, false, true))
        fifo.io.enq := m.rdata.bits
        fifo.io.enqVld := m.rdata.valid
        m.rdata.ready := ~fifo.io.full
        ls.rdata.bits := fifo.io.deq
        ls.rdata.valid := ~fifo.io.empty
        fifo.io.deqVld := ls.rdata.ready

        // Wire up load.cmd interface directly
        m.cmd <> ls.cmd
    }
    mag.io.app.stores.zip(storeStreams) foreach { case (s, ss) => s <> ss }
    mag
  }

  val debugChannelID = 0

  val numDebugs = mags(debugChannelID).numDebugs
  val numRegs = numArgIns + numArgOuts + 2 - numArgIOs + numDebugs // (command, status registers)

  // Scalar, command, and status register file
  val regs = Module(new RegFile(regWidth, numRegs, numArgIns+2, numArgOuts+1+numDebugs, numArgIOs, argOutLoopbacksMap))
  regs.io.raddr := io.raddr
  regs.io.waddr := io.waddr
  regs.io.wen := io.wen
  regs.io.wdata := io.wdata
  // TODO: Fix this bug asap so that the axi42rf bridge verilog anticipates the 1 cycle delay of the data
  val bug239_hack = false
  if (bug239_hack) {
    io.rdata := regs.io.rdata
  } else {
    io.rdata := chisel3.util.ShiftRegister(regs.io.rdata, 1)
  }
  

  val command = regs.io.argIns(0)   // commandReg = first argIn
  val curStatus = regs.io.argIns(1) // current status
  val localEnable = command(0) === 1.U & ~curStatus(0)          // enable = LSB of first argIn
  val localReset = command(1) === 1.U | reset.toBool               // reset = first argIn == 2
  io.enable := localEnable
  io.reset := localReset
  regs.io.reset := localReset
  regs.reset := reset.toBool



  // Hardware time out (for debugging)
  val timeoutCycles = 12000000000L
  val timeoutCtr = Module(new Counter(40))
  timeoutCtr.io.reset := 0.U 
  timeoutCtr.io.saturate := 1.U
  timeoutCtr.io.max := timeoutCycles.U
  timeoutCtr.io.stride := 1.U
  timeoutCtr.io.enable := localEnable

  io.argIns.zipWithIndex.foreach{case (p, i) => p := regs.io.argIns(i+2)}

  val depulser = Module(new Depulser())
  depulser.io.in := io.done | timeoutCtr.io.done
  depulser.io.rst := ~command
  val status = Wire(EnqIO(UInt(regWidth.W)))
  status.bits := Cat(command(0) & timeoutCtr.io.done, command(0) & depulser.io.out.asUInt)
  status.valid := depulser.io.out
  regs.io.argOuts.zipWithIndex.foreach { case (argOutReg, i) =>
    // Manually assign bits and valid, because direct assignment with :=
    // is causing issues with chisel compilation due to the 'ready' signal
    // which we do not care about
    if (i == 0) { // statusReg: First argOut
      argOutReg.bits := status.bits
      argOutReg.valid := status.valid
    } else if (i <= (numArgOuts - numArgInstrs)) {
      argOutReg.bits := io.argOuts(i-1).bits
      argOutReg.valid := io.argOuts(i-1).valid
    } else if (i <= numArgOuts ) {
      val ic = Module(new InstrumentationCounter()) 
      argOutReg.bits := ic.io.count 
      argOutReg.valid := 1.U // FIXME
      ic.io.enable := io.argOuts(i-1).valid
    } else { // MAG debug regs
      argOutReg.bits := mags(debugChannelID).io.debugSignals(i-numArgOuts-1)
      argOutReg.valid := 1.U
    }
  }

  io.argOutLoopbacks := regs.io.argOutLoopbacks


  // Memory address generator
  val magConfig = Wire(new MAGOpcode())
  magConfig.scatterGather := false.B
  mags.foreach { _.io.config := magConfig }
  mags.foreach { _.io.reset := localReset }
  if ((FringeGlobals.target == "aws") | (FringeGlobals.target == "aws-sim") | (FringeGlobals.target == "VCU1525")) {
    mags.foreach { _.io.enable := io.aws_top_enable }
  } else {
    mags.foreach { _.io.enable := localEnable }
  }

//  mag.io.app <> io.memStreams

  // Connect the MAGcores to flop based FIFO to improve timing results, FIFO outputs drive IO DRAM interface
  val cmdMagFifos    = List.fill(numChannels){ Module(new FIFOCore(io.dram(0).cmd.bits.cloneType(), 4, 1, false, true))}
  val wdataMagFifos  = List.fill(numChannels){ Module(new FIFOCore(io.dram(0).wdata.bits.cloneType(), 4, 1, false, true))}
  val rrespMagFifos  = List.fill(numChannels){ Module(new FIFOCore(io.dram(0).rresp.bits.cloneType(), 4, 1, false, true))}
  val wrespMagFifos  = List.fill(numChannels){ Module(new FIFOCore(io.dram(0).wresp.bits.cloneType(), 4, 1, false, true))}

  // Wire MAG cmd and wdata interfaces to write side of timing FIFOs
  mags.zip(cmdMagFifos) foreach { case (mag, magFifo) => 
      magFifo.io.enq.head := mag.io.dram.cmd.bits
      magFifo.io.enqVld := mag.io.dram.cmd.valid & ~magFifo.io.full
      mag.io.dram.cmd.ready := ~magFifo.io.full
  }
  mags.zip(wdataMagFifos) foreach { case (mag, magFifo) => 
      magFifo.io.enq.head := mag.io.dram.wdata.bits
      magFifo.io.enqVld := mag.io.dram.wdata.valid & ~magFifo.io.full
      mag.io.dram.wdata.ready := ~magFifo.io.full
  }
  
  // Wire MAG rresp and wresp interfaces to read side of timing FIFOs
  mags.zip(rrespMagFifos) foreach { case (mag, magFifo) =>
      mag.io.dram.rresp.bits := magFifo.io.deq.head
      magFifo.io.deqVld := mag.io.dram.rresp.ready & ~magFifo.io.empty
      mag.io.dram.rresp.valid := ~magFifo.io.empty
  }
  mags.zip(wrespMagFifos) foreach { case (mag, magFifo) => 
      mag.io.dram.wresp.bits := magFifo.io.deq.head
      magFifo.io.deqVld := mag.io.dram.wresp.ready & ~magFifo.io.empty
      mag.io.dram.wresp.valid := ~magFifo.io.empty
  }

  // Wire DRAM cmd and wdata interface to read side of timing FIFOs
  io.dram.zip(cmdMagFifos) foreach { case (dram, magFifo) => 
      dram.cmd.bits := magFifo.io.deq.head
      magFifo.io.deqVld := dram.cmd.ready & ~magFifo.io.empty
      dram.cmd.valid := ~magFifo.io.empty
  }
  io.dram.zip(wdataMagFifos) foreach { case (dram, magFifo) => 
      dram.wdata.bits := magFifo.io.deq.head
      magFifo.io.deqVld := dram.wdata.ready & ~magFifo.io.empty
      dram.wdata.valid := ~magFifo.io.empty
  }

  // Wire DRAM rresp and wresp interface to write side of timing FIFOs
  io.dram.zip(rrespMagFifos) foreach { case (dram, magFifo) => 
      magFifo.io.enq.head := dram.rresp.bits
      magFifo.io.enqVld := dram.rresp.valid & ~magFifo.io.full
      dram.rresp.ready := ~magFifo.io.full
  }
  io.dram.zip(wrespMagFifos) foreach { case (dram, magFifo) => 
      magFifo.io.enq.head := dram.wresp.bits
      magFifo.io.enqVld := dram.wresp.valid & ~magFifo.io.full
      dram.wresp.ready := ~magFifo.io.full
  }

  // Wire up debug interface
  mags(debugChannelID).io.TOP_AXI <> io.TOP_AXI
  mags(debugChannelID).io.DWIDTH_AXI <> io.DWIDTH_AXI
  mags(debugChannelID).io.PROTOCOL_AXI <> io.PROTOCOL_AXI
  mags(debugChannelID).io.CLOCKCONVERT_AXI <> io.CLOCKCONVERT_AXI

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
