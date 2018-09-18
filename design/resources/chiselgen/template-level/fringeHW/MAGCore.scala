package fringe

import util._
import chisel3._
import chisel3.util._
import templates.SRFF
import templates.Utils.log2Up
import templates.Utils
import scala.language.reflectiveCalls
import axi4._


import scala.collection.mutable.ListBuffer
import java.io.{File, PrintWriter}

class BurstAddr(addrWidth: Int, w: Int, burstSizeBytes: Int) extends Bundle {
  val bits = UInt(addrWidth.W)
  def burstTag = bits(bits.getWidth - 1, log2Up(burstSizeBytes))
  def burstOffset = bits(log2Up(burstSizeBytes) - 1, 0)
  def burstAddr = Cat(burstTag, 0.U(log2Up(burstSizeBytes).W))
  def wordOffset = bits(log2Up(burstSizeBytes) - 1, if (w == 8) 0 else log2Up(w/8))

  override def cloneType(): this.type = {
    new BurstAddr(addrWidth, w, burstSizeBytes).asInstanceOf[this.type]
  }
}

class MAGCore(
  val w: Int,
  val d: Int,
  val v: Int,
  val loadStreamInfo: List[StreamParInfo],
  val storeStreamInfo: List[StreamParInfo],
  val numOutstandingBursts: Int,
  val burstSizeBytes: Int,
  val axiParams: AXI4BundleParameters,
  val isDebugChannel: Boolean = false
) extends Module {

  val numRdataDebug = 0
  val numRdataWordsDebug = 16
  val numWdataDebug = 0
  val numWdataWordsDebug = 16
  val numDebugs = 500

  val scatterGatherD = d

  val numStreams = loadStreamInfo.size + storeStreamInfo.size
  val streamTagWidth = log2Up(numStreams)
  assert(streamTagWidth <= (new DRAMCommandTag).streamId.getWidth)

  val sparseLoads = loadStreamInfo.zipWithIndex.filter { case (s, i) => s.isSparse }
  val denseLoads = loadStreamInfo.zipWithIndex.filterNot { case (s, i) => s.isSparse }
  val sparseStores = storeStreamInfo.zipWithIndex.filter { case (s, i) => s.isSparse }
  val denseStores = storeStreamInfo.zipWithIndex.filterNot { case (s, i) => s.isSparse }

  def storeStreamIndex(id: UInt) = id - loadStreamInfo.size.U
  def storeStreamId(index: Int) = index + loadStreamInfo.size


  val axiLiteParams = new AXI4BundleParameters(64, 512, 1)

  val io = IO(new Bundle {
    val enable = Input(Bool())
    val reset = Input(Bool())
    val app = new AppStreams(loadStreamInfo, storeStreamInfo)
    val dram = new DRAMStream(w, v)
    val config = Input(new MAGOpcode())
    val debugSignals = Output(Vec(numDebugs, UInt(w.W)))

    // AXI Debuggers
    val TOP_AXI = new AXI4Probe(axiLiteParams)
    val DWIDTH_AXI = new AXI4Probe(axiLiteParams)
    val PROTOCOL_AXI = new AXI4Probe(axiLiteParams)
    val CLOCKCONVERT_AXI = new AXI4Probe(axiLiteParams)

  })

  val external_w = if (FringeGlobals.target == "vcs" || FringeGlobals.target == "asic") 8 else 32
  val external_v = if (FringeGlobals.target == "vcs" || FringeGlobals.target == "asic") 64 else 16
  // debug registers
  def debugCounter(en: Bool) = {
    val c = Module(new Counter(w))
    c.io.reset := io.reset
    c.io.saturate := false.B
    c.io.max := ~(0.U(w.W))
    c.io.stride := 1.U
    c.io.enable := en
    c
  }

  def debugFF[T<:Data](sig: T, en: UInt) = {
    val in = sig match {
      case v: Vec[_] => v.asInstanceOf[Vec[UInt]].reverse.reduce { Cat(_,_) }
      case u: UInt => u
    }

    val ff = Module(new FF(UInt(sig.getWidth.W)))
    ff.io.init := Cat("hBADF".U, dbgCount.U)
    ff.io.in := in
    ff.io.enable := en
    ff
  }

  var dbgCount = 0
  val signalLabels = ListBuffer[String]()
  def connectDbgSig(sig: UInt, label: String) {
    if (FringeGlobals.enableDebugRegs) {
      if (isDebugChannel) {
        io.debugSignals(dbgCount) := sig
        val padded_label = if (label.length < 55) {label + "."*(55-label.length)} else label
        signalLabels.append(padded_label)
        dbgCount += 1
      }
    }
  }

  val addrWidth = io.app.loads(0).cmd.bits.addrWidth
  val sizeWidth = io.app.loads(0).cmd.bits.sizeWidth

  val cmd = new Command(addrWidth, sizeWidth, 0)
  val cmdArbiter = Module(new FIFOArbiter(cmd, d, 1, numStreams))
  val cmdFifos = List.fill(numStreams) { Module(new FIFOCore(cmd, d, 1)) }
  cmdArbiter.io.fifo.zip(cmdFifos).foreach { case (io, f) => io <> f.io }

  val cmdFifoConfig = Wire(new FIFOOpcode(d, 1))
  cmdFifoConfig.chainRead := true.B
  cmdFifoConfig.chainWrite := true.B
  cmdArbiter.io.config := cmdFifoConfig
  cmdArbiter.io.forceTag.valid := false.B

  connectDbgSig(debugFF(chisel3.util.Cat(0x7F.U(32.W), io.app.loads.head.cmd.bits.addr(31,0)), io.app.loads.head.cmd.valid ).io.out, "Last load addr issued from app")  

  val cmds = io.app.loads.map { _.cmd } ++ io.app.stores.map {_.cmd }
  cmdArbiter.io.enq.zip(cmds) foreach {case (enq, cmd) => 
    // enq(0).addr := chisel3.util.Cat(0x7f.U(32.W), cmd.bits.addr(31,0))
    // enq(0).isWr := cmd.bits.isWr
    // enq(0).isSparse := cmd.bits.isSparse
    // enq(0).size := cmd.bits.size
    enq(0) := cmd.bits 
  }
  cmdArbiter.io.enqVld.zip(cmds) foreach {case (enqVld, cmd) => enqVld := cmd.valid }
  cmdArbiter.io.full.zip(cmds) foreach { case (full, cmd) => cmd.ready := ~full }

  val cmdHead = cmdArbiter.io.deq(0)

  val cmdAddr = Wire(new BurstAddr(addrWidth, w, burstSizeBytes))
  cmdAddr.bits := cmdHead.addr

  val cmdRead = io.enable & cmdArbiter.io.deqReady & ~cmdHead.isWr
  val cmdWrite = io.enable & cmdArbiter.io.deqReady & cmdHead.isWr

  val rrespTag = io.dram.rresp.bits.tag
  val wrespTag = io.dram.wresp.bits.tag

  val wdataReady = io.dram.wdata.ready
  val burstCounter = Module(new Counter(w))
  val burstTagCounter = Module(new Counter(log2Up(numOutstandingBursts)))
  val dramReadySeen = Wire(Bool())

  val rrespReadyMux = Module(new MuxN(Bool(), loadStreamInfo.size))
  rrespReadyMux.io.sel := rrespTag.streamId
  io.dram.rresp.ready := rrespReadyMux.io.out

  val wdataMux = Module(new MuxN(Valid(io.dram.wdata.bits), storeStreamInfo.size))
  wdataMux.io.sel := storeStreamIndex(cmdArbiter.io.tag)
  wdataMux.io.ins.foreach { case i =>
    i.bits.wlast := i.valid & (burstCounter.io.out === (io.dram.cmd.bits.size - 1.U))
  }
  val wdataValid = wdataMux.io.out.valid

  val cmdDeqValidMux = Module(new MuxN(Bool(), numStreams))
  cmdDeqValidMux.io.sel := cmdArbiter.io.tag

  val dramCmdMux = Module(new MuxN(Valid(io.dram.cmd.bits), numStreams))
  dramCmdMux.io.sel := cmdArbiter.io.tag
  dramCmdMux.io.ins.zipWithIndex.foreach { case (i, id) =>
    i.bits.addr := cmdAddr.burstAddr
    i.bits.rawAddr := cmdAddr.bits
    val tag = Wire(new DRAMCommandTag)
    tag.streamId := cmdArbiter.io.tag
    i.bits.tag := tag
    val size = Wire(new BurstAddr(cmdHead.size.getWidth, w, burstSizeBytes))
    size.bits := cmdHead.size
    i.bits.size := size.burstTag + (size.burstOffset != 0.U)
    i.bits.isWr := cmdHead.isWr
    if (id < loadStreamInfo.length && id == 0) {
      connectDbgSig(debugFF(dramCmdMux.io.out.bits.tag.streamId, dramCmdMux.io.out.valid & ~dramCmdMux.io.out.bits.isWr ).io.out, "Last load streamId (tag) sent")
      connectDbgSig(debugFF(dramCmdMux.io.out.bits.addr, dramCmdMux.io.out.valid & ~dramCmdMux.io.out.bits.isWr ).io.out, "Last load addr sent")
      connectDbgSig(debugFF(dramCmdMux.io.out.bits.size, dramCmdMux.io.out.valid & ~dramCmdMux.io.out.bits.isWr ).io.out, "Last load size sent")
    } else if (id == loadStreamInfo.length) {
      connectDbgSig(debugFF(cmdArbiter.io.tag, cmdWrite ).io.out, "Last store streamId (tag) sent")
      connectDbgSig(debugFF(cmdHead.size, cmdWrite ).io.out, "Last store size sent")
    }

  }

  val wrespReadyMux = Module(new MuxN(Bool(), storeStreamInfo.size))
  wrespReadyMux.io.sel := wrespTag.streamId
  io.dram.wresp.ready := wrespReadyMux.io.out

  val dramReady = io.dram.cmd.ready


  val gatherLoadIssueMux = Module(new MuxN(Bool(), numStreams))
  gatherLoadIssueMux.io.ins.foreach { _ := false.B }
  gatherLoadIssueMux.io.sel := cmdArbiter.io.tag
  val gatherLoadIssue = debugCounter(gatherLoadIssueMux.io.out)

  val gatherLoadSkipMux = Module(new MuxN(Bool(), numStreams))
  gatherLoadSkipMux.io.ins.foreach { _ := false.B }
  gatherLoadSkipMux.io.sel := cmdArbiter.io.tag

  val gatherLoadSkip = debugCounter(gatherLoadSkipMux.io.out)
  if (sparseLoads.size > 0) {
    connectDbgSig(gatherLoadIssue.io.out, "Gather load issue")
    connectDbgSig(gatherLoadSkip.io.out, "Gather load skip")
  }

  val scatterLoadIssueMux = Module(new MuxN(Bool(), numStreams))
  scatterLoadIssueMux.io.ins.foreach { _ := false.B }
  scatterLoadIssueMux.io.sel := cmdArbiter.io.tag
  val scatterLoadIssue = debugCounter(scatterLoadIssueMux.io.out)

  val scatterLoadSkipMux = Module(new MuxN(Bool(), numStreams))
  scatterLoadSkipMux.io.ins.foreach { _ := false.B }
  scatterLoadSkipMux.io.sel := cmdArbiter.io.tag
  val scatterLoadSkip = debugCounter(scatterLoadSkipMux.io.out)

  val scatterStoreIssueMux = Module(new MuxN(Bool(), numStreams))
  scatterStoreIssueMux.io.ins.foreach { _ := false.B }
  scatterStoreIssueMux.io.sel := cmdArbiter.io.tag
  val scatterStoreIssue = debugCounter(scatterStoreIssueMux.io.out)

  val scatterStoreSkipMux = Module(new MuxN(Bool(), numStreams))
  scatterStoreSkipMux.io.ins.foreach { _ := false.B }
  scatterStoreSkipMux.io.sel := cmdArbiter.io.tag
  val scatterStoreSkip = debugCounter(scatterStoreSkipMux.io.out)

  if (sparseStores.size > 0) {
    connectDbgSig(scatterLoadSkip.io.out, "Scatter load skip")
    connectDbgSig(scatterStoreIssue.io.out, "Scatter store issue")
    connectDbgSig(scatterLoadIssue.io.out, "Scatter load issue")
    connectDbgSig(scatterStoreSkip.io.out, "Scatter store skip")
  }

  val gatherBuffers = sparseLoads.map { case (s, i) =>
    val m = Module(new GatherBuffer(s.w, scatterGatherD, s.v, burstSizeBytes, addrWidth, cmdHead, io.dram.rresp.bits))
    m.io.rresp.valid := io.dram.rresp.valid & (rrespTag.streamId === i.U)
    m.io.rresp.bits := io.dram.rresp.bits
    m.io.cmd.valid := cmdRead & cmdArbiter.io.tag === i.U & dramReady
    m.io.cmd.bits := cmdHead

    gatherLoadIssueMux.io.ins(i) := ~cmdArbiter.io.empty & cmdDeqValidMux.io.ins(i) & dramCmdMux.io.ins(i).valid
    gatherLoadSkipMux.io.ins(i) := ~cmdArbiter.io.empty & cmdDeqValidMux.io.ins(i) & ~dramCmdMux.io.ins(i).valid

    rrespReadyMux.io.ins(i) := true.B
    cmdDeqValidMux.io.ins(i) := ~m.io.fifo.full & dramReady
    dramCmdMux.io.ins(i).valid := cmdRead & ~m.io.fifo.full & ~m.io.hit
    dramCmdMux.io.ins(i).bits.tag.uid := cmdAddr.burstTag

    val stream = io.app.loads(i)
    stream.rdata.bits := m.io.fifo.deq
    stream.rdata.valid := m.io.complete
    m.io.fifo.deqVld := stream.rdata.ready
    m
  }

  // TODO: this currently assumes the memory controller hands back data in the order it was requested,
  // but according to the AXI spec the ARID field should be constant to enforce ordering?
  val denseLoadBuffers = denseLoads.map { case (s, i) =>
    val m = Module(new FIFOWidthConvert(external_w, io.dram.rresp.bits.rdata.size, s.w, s.v, d))
    m.io.enq := io.dram.rresp.bits.rdata
    m.io.enqVld := io.dram.rresp.valid & (rrespTag.streamId === i.U)

    rrespReadyMux.io.ins(i) := ~m.io.full
    cmdDeqValidMux.io.ins(i) := dramReady

    dramCmdMux.io.ins(i).valid := cmdRead
    dramCmdMux.io.ins(i).bits.tag.uid := burstTagCounter.io.out

    val stream = io.app.loads(i)
    stream.rdata.bits := m.io.deq
    stream.rdata.valid := ~m.io.empty
    m.io.deqVld := stream.rdata.ready

    connectDbgSig(debugCounter(m.io.enqVld).io.out, s"rdataFifo $i # enqs")
    connectDbgSig(debugCounter(~m.io.empty).io.out, s"rdataFifo $i # cycles ~empty (= data valid)")
    connectDbgSig(debugCounter(stream.rdata.ready).io.out, s"load stream $i # cycles ready")
    connectDbgSig(debugCounter(~m.io.empty && stream.rdata.ready).io.out, s"load stream $i # handshakes")

    connectDbgSig(debugCounter(m.io.empty & m.io.deqVld).io.out, s"number of bad elements (IF =!= 0, LOOK HERE FOR BUGS)")

    val sDeq_latch = Module(new SRFF())
    sDeq_latch.io.input.set := m.io.deqVld
    sDeq_latch.io.input.reset := reset.toBool | io.reset
    sDeq_latch.io.input.asyn_reset := reset.toBool | io.reset

    val sEnq_latch = Module(new SRFF())
    sEnq_latch.io.input.set := m.io.enqVld
    sEnq_latch.io.input.reset := reset.toBool | io.reset
    sEnq_latch.io.input.asyn_reset := reset.toBool | io.reset


    connectDbgSig(debugFF(m.io.deq, ~sDeq_latch.io.output.data & Utils.risingEdge(m.io.deqVld)).io.out, s"m.io.deq")
    connectDbgSig(debugFF(m.io.enq, ~sEnq_latch.io.output.data & Utils.risingEdge(m.io.enqVld)).io.out, s"m.io.enq")

    m
  }


  val scatterBuffers = sparseStores.map { case (s, i) =>
    val j = storeStreamId(i)

    val m = Module(new ScatterBuffer(s.w, scatterGatherD, s.v, burstSizeBytes, addrWidth, sizeWidth, io.dram.rresp.bits))
    val wdata = Module(new FIFOCore(UInt(s.w.W), d, s.v))
    val stream = io.app.stores(i)

    val write = cmdWrite & (cmdArbiter.io.tag === j.U)
    val issueWrite = m.io.complete & (cmdArbiter.io.tag === j.U)
    val issueRead = ~m.io.complete & write & ~m.io.fifo.full & ~wdata.io.empty & ~m.io.hit
    val skipRead = write & m.io.hit & ~wdata.io.empty

    val deqCmd = skipRead | (issueRead & dramReady)
    wdata.io.config.chainRead := true.B
    wdata.io.config.chainWrite := true.B
    wdata.io.enqVld := stream.wdata.valid
    wdata.io.enq := stream.wdata.bits
    wdata.io.deqVld := deqCmd
    stream.wdata.ready := ~wdata.io.full

    cmdDeqValidMux.io.ins(j) := deqCmd

    val dramCmd = dramCmdMux.io.ins(j)
    val addr = Wire(new BurstAddr(addrWidth, s.w, burstSizeBytes))
    addr.bits := Mux(issueRead, cmdHead.addr, m.io.fifo.deq(0).cmd.addr)
    dramCmd.bits.addr := addr.burstAddr
    dramCmd.bits.rawAddr := addr.bits
    dramCmd.bits.tag.uid := Mux(issueRead, addr.burstTag, m.io.fifo.deq(0).count)
    dramCmd.bits.isWr := issueWrite
    val size = Wire(new BurstAddr(cmdHead.size.getWidth, s.w, burstSizeBytes))
    size.bits := Mux(issueRead, cmdHead.size, m.io.fifo.deq(0).cmd.size)
    dramCmd.bits.size := size.burstTag + (size.burstOffset != 0.U)
    dramCmd.valid := issueRead | (issueWrite & wdataValid & ~dramReadySeen)

    scatterLoadIssueMux.io.ins(j) := dramCmd.valid & deqCmd & ~cmdArbiter.io.empty
    scatterLoadSkipMux.io.ins(j) := ~dramCmd.valid & deqCmd & ~cmdArbiter.io.empty
    scatterStoreIssueMux.io.ins(j) := m.io.complete & m.io.fifo.deqVld
    scatterStoreSkipMux.io.ins(j) := deqCmd & ~cmdArbiter.io.empty

    m.io.rresp.valid := io.dram.rresp.valid & (rrespTag.streamId === j.U)
    m.io.rresp.bits := io.dram.rresp.bits
    m.io.fifo.enqVld := deqCmd
    m.io.fifo.enq(0).data.foreach { _ := wdata.io.deq(0) }
    m.io.fifo.enq(0).cmd := cmdHead
    m.io.fifo.deqVld := burstCounter.io.done

    wdataMux.io.ins(i).valid := issueWrite
    wdataMux.io.ins(i).bits.wdata := Utils.vecWidthConvert(m.io.fifo.deq(0).data, w)
    wdataMux.io.ins(i).bits.wstrb.zipWithIndex.foreach{case (st, i) => st := true.B}

    val wrespFIFO = Module(new FIFOCore(UInt(32.W), d, 1))
    wrespFIFO.io.enq(0) := io.dram.wresp.bits.tag.uid
    wrespFIFO.io.enqVld := io.dram.wresp.valid & (wrespTag.streamId === j.U)
    wrespReadyMux.io.ins(i) := ~wrespFIFO.io.full

    val count = Module(new UpDownCtr(32))
    count.io.max := ~(0.U(32.W))
    // send a response after at least 16 sparse writes have completed
    // why does spatial always expect 16 regardless of parallelization factor?
    val sendResp = count.io.out >= 16.U
    val deqRespCount = ~wrespFIFO.io.empty & ~sendResp
    count.io.strideInc := wrespFIFO.io.deq(0)
    count.io.strideDec := 16.U(32.W)
    count.io.inc := deqRespCount
    count.io.dec := sendResp & stream.wresp.ready

    stream.wresp.bits := sendResp
    stream.wresp.valid := sendResp
    wrespFIFO.io.deqVld := deqRespCount

    m
  }
  // force command arbiter to service scatter buffers when data is waiting to be written back to memory
  sparseStores.headOption match {
    case Some((s, i)) =>
      val scatterReadys = scatterBuffers.map { ~_.io.fifo.empty }
      cmdArbiter.io.forceTag.valid := scatterReadys.reduce { _|_ }
      cmdArbiter.io.forceTag.bits := PriorityEncoder(scatterReadys) + storeStreamId(i).U
    case None =>
  }

  val denseStoreBuffers = denseStores.map { case (s, i) =>
    val j = storeStreamId(i)
    val m = Module(new FIFOWidthConvert(s.w, s.v, external_w, external_v, d))
    val stream = io.app.stores(i)

    cmdDeqValidMux.io.ins(j) := burstCounter.io.done

    dramCmdMux.io.ins(j).valid := cmdWrite & wdataValid & ~dramReadySeen
    dramCmdMux.io.ins(j).bits.tag.uid := burstTagCounter.io.out

    m.io.enqVld := stream.wdata.valid
    m.io.enq := stream.wdata.bits
    m.io.enqStrb := stream.wstrb.bits
    m.io.deqVld := cmdWrite & ~m.io.empty & io.dram.wdata.ready & (cmdArbiter.io.tag === j.U)

    wdataMux.io.ins(i).valid := cmdWrite & ~m.io.empty
    wdataMux.io.ins(i).bits.wdata := m.io.deq
    wdataMux.io.ins(i).bits.wstrb.zipWithIndex.foreach{case (st, i) => st := m.io.deqStrb(i)}
    stream.wdata.ready := ~m.io.full

    // connectDbgSig(debugCounter(m.io.enqVld).io.out, s"wdataFifo $i # enqs")
    // connectDbgSig(debugCounter(~m.io.full).io.out, s"wdataFifo $i # cycles ~full (= ready)")
    // connectDbgSig(debugCounter(stream.wdata.valid).io.out, s"store stream $i # cycles valid")
    // connectDbgSig(debugCounter(~m.io.full && stream.wdata.valid).io.out, s"store stream $i # handshakes")

    val wrespFIFO = Module(new FIFOCounter(d, 1))
    wrespFIFO.io.enq(0) := io.dram.wresp.valid
    wrespFIFO.io.enqVld := io.dram.wresp.valid & (wrespTag.streamId === j.U)
    wrespReadyMux.io.ins(i) := ~wrespFIFO.io.full
    stream.wresp.bits  := wrespFIFO.io.deq(0)
    stream.wresp.valid := ~wrespFIFO.io.empty
    wrespFIFO.io.deqVld := stream.wresp.ready

    connectDbgSig(debugCounter(wrespFIFO.io.enqVld).io.out, s"wrespFifo $i enq")
    connectDbgSig(debugCounter(~wrespFIFO.io.empty).io.out, s"wrespFifo $i ~empty (stream.wresp.valid)")
    connectDbgSig(debugCounter(stream.wresp.ready).io.out, s"wrespFifo $i deq (stream.wresp.ready)")

    m
  }

  val dramValid = io.dram.cmd.valid
  burstCounter.io.max := Mux(io.dram.cmd.bits.isWr, io.dram.cmd.bits.size, 1.U)
  burstCounter.io.stride := 1.U
  burstCounter.io.reset := io.reset
  burstCounter.io.enable := Mux(io.dram.cmd.bits.isWr, wdataValid & wdataReady, dramValid  & dramReady)
  burstCounter.io.saturate := false.B

  // strictly speaking this isn't necessary, but the DRAM part of the test bench expects unique tags
  // and sometimes apps make requests to the same address so tagging with the address isn't enough to guarantee uniqueness
  burstTagCounter.io.max := (numOutstandingBursts - 1).U
  burstTagCounter.io.stride := 1.U
  burstTagCounter.io.reset := io.reset
  burstTagCounter.io.enable := dramValid  & dramReady
  burstTagCounter.io.saturate := false.B


  val dramReadyFF = Module(new FF(Bool()))
  dramReadyFF.io.init := 0.U
  dramReadyFF.io.enable := burstCounter.io.done | (dramValid  & io.dram.cmd.bits.isWr)
  dramReadyFF.io.in := Mux(burstCounter.io.done, 0.U, dramReady | dramReadySeen)
  dramReadySeen := dramReadyFF.io.out
  cmdArbiter.io.deqVld := cmdDeqValidMux.io.out

  io.dram.wdata.bits.wdata := wdataMux.io.out.bits.wdata
  io.dram.wdata.bits.wstrb := wdataMux.io.out.bits.wstrb.reverse // .foreach(_ := 1.U)
  io.dram.wdata.bits.wlast := wdataMux.io.out.bits.wlast
  io.dram.wdata.valid := wdataMux.io.out.valid

  io.dram.cmd.bits := dramCmdMux.io.out.bits
  // io.dram.cmd.bits.addr := chisel3.util.Cat(0x7F.U(32.W), dramCmdMux.io.out.bits.addr(31,0))
  // io.dram.cmd.bits.size := dramCmdMux.io.out.bits.size
  // io.dram.cmd.bits.rawAddr := chisel3.util.Cat(0x7F.U(32.W), dramCmdMux.io.out.bits.rawAddr(31,0))
  // io.dram.cmd.bits.isWr := dramCmdMux.io.out.bits.isWr
  // io.dram.cmd.bits.tag := dramCmdMux.io.out.bits.tag
  // io.dram.cmd.bits.dramReadySeen := dramCmdMux.io.out.bits.dramReadySeen

  io.dram.cmd.valid := dramCmdMux.io.out.valid

  val cycleCount = debugCounter(io.enable)
  connectDbgSig(cycleCount.io.out, "Cycles")

  val rdataEnqCount = debugCounter(io.dram.rresp.valid & io.dram.rresp.ready)
  val wdataCount = debugCounter(io.dram.wdata.valid & io.dram.wdata.ready & io.enable)

  // rdata enq values
  for (i <- 0 until numRdataDebug) {
    connectDbgSig(debugFF(io.dram.cmd.bits.addr, io.dram.rresp.ready & io.dram.rresp.valid & (rdataEnqCount.io.out === (i+42).U) ).io.out, s"raddr_from_dram${(i+42)}")  
    connectDbgSig(debugFF(io.dram.cmd.bits.size, io.dram.rresp.ready & io.dram.rresp.valid & (rdataEnqCount.io.out === (i+42).U) ).io.out, s"raddr_from_dram${(i+42)}")  
    for (j <- 0 until numRdataWordsDebug) {
      connectDbgSig(debugFF(io.dram.rresp.bits.rdata(j), io.dram.rresp.ready & io.dram.rresp.valid & (rdataEnqCount.io.out === (i+42).U)).io.out, s"""rdata_from_dram${(i+42)}_$j""")
    }
  }


  if (io.app.stores.size > 0) {
    // wdata enq values
    for (i <- 0 until numWdataDebug) {
      connectDbgSig(debugFF(io.dram.wdata.bits.wstrb, io.dram.wdata.ready & io.dram.wdata.valid & (wdataCount.io.out === (i).U)).io.out, s"""wstrb_from_dram${(i)}""")
      for (j <- 0 until numWdataWordsDebug) {
        connectDbgSig(debugFF(io.dram.wdata.bits.wdata(j), io.dram.wdata.ready & io.dram.wdata.valid & (wdataCount.io.out === (i).U)).io.out, s"""wdata_from_dram${(i)}_$j""")
      }
      // connectDbgSig(debugFF(wdataMux.io.out.bits.wdata, io.dram.wdata.ready & io.dram.wdata.valid & (wdataCount.io.out === i.U)).io.out, s"""Actual values on wdata.bits""")
    }
  }

  connectDbgSig(debugCounter(io.enable & dramReady & io.dram.cmd.valid).io.out, "# DRAM Commands Issued")
  connectDbgSig(debugCounter(io.enable & ~cmdArbiter.io.empty & ~(dramReady & io.dram.cmd.valid)).io.out, "Total cycles w/ 1+ cmds queued up")
  connectDbgSig(debugCounter(io.enable & dramReady & io.dram.cmd.valid & ~cmdHead.isWr).io.out, "# Read Commands Sent")
  // Count number of load commands issued from accel per stream
  io.app.loads.zipWithIndex.foreach { case (load, i) =>
    val loadCounter = debugCounter(io.enable & load.cmd.valid)
    val loadCounterHandshake = debugCounter(io.enable & load.cmd.valid & load.cmd.ready)
    connectDbgSig(loadCounterHandshake.io.out, s" # from Accel load stream $i")
    val signal = s" # from Fringe load stream $i"
    connectDbgSig(debugCounter(io.dram.cmd.valid & dramReady & (cmdArbiter.io.tag === i.U)).io.out, signal)
    connectDbgSig(loadCounter.io.out, s" # attempted from Accel load stream (cycles valid) $i")
  }
  connectDbgSig(debugCounter(io.enable & dramReady & io.dram.cmd.valid & cmdHead.isWr).io.out, "# Write Commands Sent")
  // Count number of store commands issued from accel per stream
  io.app.stores.zipWithIndex.foreach { case (store, i) =>
    val storeCounter = debugCounter(io.enable & store.cmd.valid)
    val storeCounterHandshake = debugCounter(io.enable & store.cmd.valid & store.cmd.ready)
    connectDbgSig(storeCounterHandshake.io.out, s" # from Accel store stream $i")
    val signal = s" # from Fringe store stream ${i}"
    connectDbgSig(debugCounter(io.dram.cmd.valid & dramReady & (cmdArbiter.io.tag === (i+loadStreamInfo.length).U)).io.out, signal)
    connectDbgSig(storeCounter.io.out, s" # attempted from Accel store stream (cycles valid) $i")
  }

  connectDbgSig(debugCounter((io.dram.rresp.valid & io.dram.rresp.ready)).io.out, "# Read Responses Acknowledged")
  connectDbgSig(debugCounter(io.enable & io.dram.rresp.valid & ~io.dram.rresp.ready).io.out, "# RResp rejected by ready")
  connectDbgSig(debugCounter(io.enable & ~io.dram.rresp.valid & io.dram.rresp.ready).io.out, "Cycles RResp ready and idle (~valid)")
  (0 until loadStreamInfo.size).map{i =>
    val signal = s" # from load stream $i"
    connectDbgSig(debugCounter(io.dram.rresp.valid & io.dram.rresp.ready & (rrespTag.streamId === i.U)).io.out, signal)
  }
  connectDbgSig(debugCounter((io.dram.wresp.valid & io.dram.wresp.ready)).io.out, "# Write Responses Acknowledged")
  connectDbgSig(debugCounter(io.enable & io.dram.wresp.valid & ~io.dram.wresp.ready).io.out, "# WResp rejected by ready")
  connectDbgSig(debugCounter(io.enable & ~io.dram.wresp.valid & io.dram.wresp.ready).io.out, "Cycles WResp ready and idle (~valid)")
  (0 until storeStreamInfo.size).map{i =>
    val signal = s" # from store stream $i"
    connectDbgSig(debugCounter(io.dram.wresp.valid & io.dram.wresp.ready & (wrespTag.streamId === (i+loadStreamInfo.length).U)).io.out, signal)
  }

  denseLoadBuffers.zipWithIndex foreach { case (b,i) =>
    connectDbgSig(debugCounter(b.io.full).io.out, "(load) fifo converter " + i + " # cycles full")
    connectDbgSig(debugCounter(b.io.almostFull).io.out, "(load) fifo converter " + i + " # cycles almostFull")
    connectDbgSig(debugCounter(b.io.empty).io.out, "(load) fifo converter " + i + " # cycles empty")
    connectDbgSig(debugCounter(b.io.almostEmpty).io.out, "(load) fifo converter " + i + " # cycles almostEmpty")
    connectDbgSig(debugCounter(b.io.enqVld).io.out, "(load) fifo converter " + i + " # cycles enqVld")
    connectDbgSig(debugCounter(rrespTag.streamId === i.U).io.out, "(load) fifo converter " + i + " # cycles streamId == " + i)
    connectDbgSig(debugCounter(io.dram.rresp.valid).io.out, "(load) # cycles rresp == valid")
  }
  connectDbgSig(debugCounter(rrespTag.streamId >= denseLoadBuffers.length.U).io.out, "(load) # cycles streamId >= last")
  connectDbgSig(debugFF(rrespTag.streamId, io.dram.rresp.valid).io.out, "(load) last streamId")
  
  denseStoreBuffers.zipWithIndex foreach { case (b,i) => 
    connectDbgSig(debugCounter(b.io.enqVld).io.out, "(store) fifo converter " + i + " # enq")
    connectDbgSig(debugCounter(~b.io.full).io.out, "(store) fifo converter " + i + " # cycles ~full (= ready)")
    connectDbgSig(debugCounter(~b.io.full && b.io.enqVld).io.out, s"(store) fifo converter $i # enq while not full")
    connectDbgSig(debugCounter(b.io.full && b.io.enqVld).io.out, s"(store) fifo converter $i # enq while full")
    connectDbgSig(debugCounter(b.io.full).io.out, "(store) fifo converter " + i + " # cycles full")
    connectDbgSig(debugCounter(b.io.almostFull).io.out, "(store) fifo converter " + i + " # cycles almostFull")
    connectDbgSig(debugCounter(b.io.empty).io.out, "(store) fifo converter " + i + " # cycles empty")
    connectDbgSig(debugCounter(b.io.almostEmpty).io.out, "(store) fifo converter " + i + " # cycles almostEmpty")
  }

  connectDbgSig(debugCounter(io.dram.rresp.valid & denseLoadBuffers.map {_.io.enqVld}.reduce{_|_}).io.out, "RResp valid enqueued somewhere")
  connectDbgSig(debugCounter(io.dram.rresp.valid & io.dram.rresp.ready).io.out, "Rresp valid and ready")
  connectDbgSig(debugCounter(io.dram.rresp.valid & io.dram.rresp.ready & denseLoadBuffers.map {_.io.enqVld}.reduce{_|_}).io.out, "Resp valid and ready and enqueued somewhere")
  connectDbgSig(debugCounter(io.dram.rresp.valid & ~io.dram.rresp.ready).io.out, "Resp valid and not ready")

  connectDbgSig(wdataCount.io.out, "num wdata transferred (wvalid & wready)")

  // Connect AXI loopback debuggers
  // TOP
  connectDbgSig(debugCounter(io.TOP_AXI.ARVALID).io.out, "# cycles TOP ARVALID ")
  connectDbgSig(debugCounter(io.TOP_AXI.ARREADY).io.out, "# cycles TOP ARREADY")
  connectDbgSig(debugCounter(io.TOP_AXI.ARREADY & io.TOP_AXI.ARVALID).io.out, "# cycles TOP ARREADY & ARVALID ")
  connectDbgSig(debugCounter(io.TOP_AXI.AWVALID).io.out, "# cycles TOP AWVALID ")
  connectDbgSig(debugCounter(io.TOP_AXI.AWREADY & io.TOP_AXI.AWVALID).io.out, "# cycles TOP AWREADY & AWVALID ")
  connectDbgSig(debugCounter(io.TOP_AXI.RVALID).io.out, "# cycles TOP RVALID ")
  connectDbgSig(debugCounter(io.TOP_AXI.RREADY & io.TOP_AXI.RVALID).io.out, "# cycles TOP RREADY & RVALID ")
  connectDbgSig(debugCounter(io.TOP_AXI.WVALID).io.out, "# cycles TOP WVALID ")
  connectDbgSig(debugCounter(io.TOP_AXI.WREADY & io.TOP_AXI.WVALID).io.out, "# cycles TOP WREADY & WVALID ")
  connectDbgSig(debugCounter(~io.TOP_AXI.WREADY & io.TOP_AXI.WVALID).io.out, "# cycles TOP ~WREADY & WVALID (forced)" )
  connectDbgSig(debugCounter(~io.TOP_AXI.WREADY).io.out, "# cycles TOP ~WREADY" )
  connectDbgSig(debugCounter(io.TOP_AXI.BVALID).io.out, "# cycles TOP BVALID ")
  connectDbgSig(debugCounter(io.TOP_AXI.BREADY & io.TOP_AXI.BVALID).io.out, "# cycles TOP BREADY & BVALID ")
  connectDbgSig(debugFF(io.TOP_AXI.ARADDR, io.TOP_AXI.ARVALID & io.TOP_AXI.ARREADY).io.out, "Last TOP ARADDR")
  connectDbgSig(debugFF(io.TOP_AXI.ARLEN, io.TOP_AXI.ARVALID & io.TOP_AXI.ARREADY).io.out, "Last TOP ARLEN")
  connectDbgSig(debugFF(io.TOP_AXI.ARSIZE, io.TOP_AXI.ARVALID & io.TOP_AXI.ARREADY).io.out, "Last TOP ARSIZE")
  connectDbgSig(debugFF(io.TOP_AXI.ARID, io.TOP_AXI.ARVALID & io.TOP_AXI.ARREADY).io.out, "Last TOP ARID")
  connectDbgSig(debugFF(io.TOP_AXI.ARBURST, io.TOP_AXI.ARVALID & io.TOP_AXI.ARREADY).io.out, "Last TOP ARBURST")
  // connectDbgSig(debugCounter(io.TOP_AXI.ARLOCK).io.out, "# cycles TOP ARLOCK ")
  connectDbgSig(debugFF(io.TOP_AXI.AWADDR, io.TOP_AXI.AWVALID & io.TOP_AXI.AWREADY).io.out, "Last TOP AWADDR")
  connectDbgSig(debugFF(io.TOP_AXI.AWLEN, io.TOP_AXI.AWVALID & io.TOP_AXI.AWREADY).io.out, "Last TOP AWLEN")
  connectDbgSig(debugFF(io.TOP_AXI.WDATA, io.TOP_AXI.WVALID & io.TOP_AXI.WREADY).io.out, "Last TOP WDATA")
  connectDbgSig(debugFF(io.TOP_AXI.WSTRB, io.TOP_AXI.WVALID & io.TOP_AXI.WREADY).io.out, "Last TOP WSTRB")
  connectDbgSig(debugFF(io.TOP_AXI.WDATA, io.TOP_AXI.WVALID & io.TOP_AXI.WREADY & wdataCount.io.out === 0.U).io.out, "First TOP WDATA")
  connectDbgSig(debugFF(io.TOP_AXI.WSTRB, io.TOP_AXI.WVALID & io.TOP_AXI.WREADY & wdataCount.io.out === 0.U).io.out, "First TOP WSTRB")
  connectDbgSig(debugFF(io.TOP_AXI.WDATA, io.TOP_AXI.WVALID & io.TOP_AXI.WREADY & wdataCount.io.out === 1.U).io.out, "Second TOP WDATA")
  connectDbgSig(debugFF(io.TOP_AXI.WSTRB, io.TOP_AXI.WVALID & io.TOP_AXI.WREADY & wdataCount.io.out === 1.U).io.out, "Second TOP WSTRB")

  // // DWIDTH
  connectDbgSig(debugCounter(io.DWIDTH_AXI.ARVALID).io.out, "# cycles DWIDTH ARVALID ")
  connectDbgSig(debugCounter(io.DWIDTH_AXI.ARREADY).io.out, "# cycles DWIDTH ARREADY ")
  connectDbgSig(debugCounter(io.DWIDTH_AXI.ARREADY & io.DWIDTH_AXI.ARVALID).io.out, "# cycles DWIDTH ARREADY & ARVALID ")
  connectDbgSig(debugCounter(io.DWIDTH_AXI.AWVALID).io.out, "# cycles DWIDTH AWVALID ")
  connectDbgSig(debugCounter(io.DWIDTH_AXI.AWREADY & io.DWIDTH_AXI.AWVALID).io.out, "# cycles DWIDTH ARREADY & AWVALID ")
  connectDbgSig(debugCounter(io.DWIDTH_AXI.RVALID).io.out, "# cycles DWIDTH RVALID ")
  connectDbgSig(debugCounter(io.DWIDTH_AXI.RREADY & io.DWIDTH_AXI.RVALID).io.out, "# cycles DWIDTH RREADY & RVALID ")
  connectDbgSig(debugCounter(io.DWIDTH_AXI.WVALID).io.out, "# cycles DWIDTH WVALID ")
  connectDbgSig(debugCounter(io.DWIDTH_AXI.WREADY & io.DWIDTH_AXI.WVALID).io.out, "# cycles DWIDTH WREADY & WVALID ")
  connectDbgSig(debugCounter(~io.DWIDTH_AXI.WREADY & io.DWIDTH_AXI.WVALID).io.out, "# cycles DWIDTH ~WREADY & WVALID (forced)" )
  connectDbgSig(debugCounter(~io.DWIDTH_AXI.WREADY).io.out, "# cycles DWIDTH ~WREADY" )
  connectDbgSig(debugCounter(io.DWIDTH_AXI.BVALID).io.out, "# cycles DWIDTH BVALID ")
  connectDbgSig(debugCounter(io.DWIDTH_AXI.BREADY & io.DWIDTH_AXI.BVALID).io.out, "# cycles DWIDTH BREADY & BVALID ")
  connectDbgSig(debugFF(io.DWIDTH_AXI.ARADDR, io.DWIDTH_AXI.ARVALID & io.DWIDTH_AXI.ARREADY).io.out, "Last DWIDTH ARADDR")
  connectDbgSig(debugFF(io.DWIDTH_AXI.ARLEN, io.DWIDTH_AXI.ARVALID & io.DWIDTH_AXI.ARREADY).io.out, "Last DWIDTH ARLEN")
  connectDbgSig(debugFF(io.DWIDTH_AXI.ARSIZE, io.DWIDTH_AXI.ARVALID & io.DWIDTH_AXI.ARREADY).io.out, "Last DWIDTH ARSIZE")
  // connectDbgSig(debugFF(io.DWIDTH_AXI.ARID, io.DWIDTH_AXI.ARVALID & io.DWIDTH_AXI.ARREADY).io.out, "Last DWIDTH ARID")
  connectDbgSig(debugFF(io.DWIDTH_AXI.ARBURST, io.DWIDTH_AXI.ARVALID & io.DWIDTH_AXI.ARREADY).io.out, "Last DWIDTH ARBURST")
  // connectDbgSig(debugCounter(io.DWIDTH_AXI.ARLOCK).io.out, "# cycles DWIDTH ARLOCK ")
  connectDbgSig(debugFF(io.DWIDTH_AXI.AWADDR, io.DWIDTH_AXI.AWVALID & io.DWIDTH_AXI.AWREADY).io.out, "Last DWIDTH AWADDR")
  connectDbgSig(debugFF(io.DWIDTH_AXI.AWLEN, io.DWIDTH_AXI.AWVALID & io.DWIDTH_AXI.AWREADY).io.out, "Last DWIDTH AWLEN")
  connectDbgSig(debugFF(io.DWIDTH_AXI.WDATA, io.DWIDTH_AXI.WVALID & io.DWIDTH_AXI.WREADY).io.out, "Last DWIDTH WDATA")
  connectDbgSig(debugFF(io.DWIDTH_AXI.WSTRB, io.DWIDTH_AXI.WVALID & io.DWIDTH_AXI.WREADY).io.out, "Last DWIDTH WSTRB")
  connectDbgSig(debugFF(io.DWIDTH_AXI.WDATA, io.DWIDTH_AXI.WVALID & io.DWIDTH_AXI.WREADY & wdataCount.io.out === 0.U).io.out, "First DWIDTH WDATA")
  connectDbgSig(debugFF(io.DWIDTH_AXI.WSTRB, io.DWIDTH_AXI.WVALID & io.DWIDTH_AXI.WREADY & wdataCount.io.out === 0.U).io.out, "First DWIDTH WSTRB")
  connectDbgSig(debugFF(io.DWIDTH_AXI.WDATA, io.DWIDTH_AXI.WVALID & io.DWIDTH_AXI.WREADY & wdataCount.io.out === 1.U).io.out, "Second DWIDTH WDATA")
  connectDbgSig(debugFF(io.DWIDTH_AXI.WSTRB, io.DWIDTH_AXI.WVALID & io.DWIDTH_AXI.WREADY & wdataCount.io.out === 1.U).io.out, "Second DWIDTH WSTRB")

  // // PROTOCOL
  // connectDbgSig(debugCounter(io.PROTOCOL_AXI.ARVALID).io.out, "# cycles PROTOCOL ARVALID ")
  // connectDbgSig(debugCounter(io.PROTOCOL_AXI.ARREADY).io.out, "# cycles PROTOCOL ARREADY ")
  // connectDbgSig(debugCounter(io.PROTOCOL_AXI.ARREADY & io.PROTOCOL_AXI.ARVALID).io.out, "# cycles PROTOCOL ARREADY & ARVALID ")
  // connectDbgSig(debugCounter(io.PROTOCOL_AXI.AWVALID).io.out, "# cycles PROTOCOL AWVALID ")
  // connectDbgSig(debugCounter(io.PROTOCOL_AXI.AWREADY & io.PROTOCOL_AXI.AWVALID).io.out, "# cycles PROTOCOL ARREADY & AWVALID ")
  // connectDbgSig(debugCounter(io.PROTOCOL_AXI.RVALID).io.out, "# cycles PROTOCOL RVALID ")
  // connectDbgSig(debugCounter(io.PROTOCOL_AXI.RREADY & io.PROTOCOL_AXI.RVALID).io.out, "# cycles PROTOCOL RREADY & RVALID ")
  // connectDbgSig(debugCounter(io.PROTOCOL_AXI.WVALID).io.out, "# cycles PROTOCOL WVALID ")
  // connectDbgSig(debugCounter(io.PROTOCOL_AXI.WREADY & io.PROTOCOL_AXI.WVALID).io.out, "# cycles PROTOCOL WREADY & WVALID ")
  // connectDbgSig(debugCounter(io.PROTOCOL_AXI.BVALID).io.out, "# cycles PROTOCOL BVALID ")
  // connectDbgSig(debugCounter(io.PROTOCOL_AXI.BREADY & io.PROTOCOL_AXI.BVALID).io.out, "# cycles PROTOCOL BREADY & BVALID ")
  // connectDbgSig(debugFF(io.PROTOCOL_AXI.ARADDR, io.PROTOCOL_AXI.ARVALID & io.PROTOCOL_AXI.ARREADY).io.out, "Last PROTOCOL ARADDR")
  // connectDbgSig(debugFF(io.PROTOCOL_AXI.ARLEN, io.PROTOCOL_AXI.ARVALID & io.PROTOCOL_AXI.ARREADY).io.out, "Last PROTOCOL ARLEN")
  // connectDbgSig(debugFF(io.PROTOCOL_AXI.ARSIZE, io.PROTOCOL_AXI.ARVALID & io.PROTOCOL_AXI.ARREADY).io.out, "Last PROTOCOL ARSIZE")
  // // connectDbgSig(debugFF(io.PROTOCOL_AXI.ARID, io.PROTOCOL_AXI.ARVALID & io.PROTOCOL_AXI.ARREADY).io.out, "Last PROTOCOL ARID")
  // connectDbgSig(debugFF(io.PROTOCOL_AXI.ARBURST, io.PROTOCOL_AXI.ARVALID & io.PROTOCOL_AXI.ARREADY).io.out, "Last PROTOCOL ARBURST")
  // // connectDbgSig(debugCounter(io.PROTOCOL_AXI.ARLOCK).io.out, "# cycles PROTOCOL ARLOCK ")
  // connectDbgSig(debugFF(io.PROTOCOL_AXI.AWADDR, io.PROTOCOL_AXI.AWVALID & io.PROTOCOL_AXI.AWREADY).io.out, "Last PROTOCOL AWADDR")

  // // Clock converter
  // connectDbgSig(debugCounter(io.CLOCKCONVERT_AXI.ARVALID).io.out, "# cycles CLOCKCONVERT ARVALID ")
  // connectDbgSig(debugCounter(io.CLOCKCONVERT_AXI.ARREADY).io.out, "# cycles CLOCKCONVERT ARREADY ")
  // connectDbgSig(debugCounter(io.CLOCKCONVERT_AXI.ARREADY & io.CLOCKCONVERT_AXI.ARVALID).io.out, "# cycles CLOCKCONVERT ARREADY & ARVALID ")
  // connectDbgSig(debugCounter(io.CLOCKCONVERT_AXI.AWVALID).io.out, "# cycles CLOCKCONVERT AWVALID ")
  // connectDbgSig(debugCounter(io.CLOCKCONVERT_AXI.AWREADY & io.CLOCKCONVERT_AXI.AWVALID).io.out, "# cycles CLOCKCONVERT ARREADY & AWVALID ")
  // connectDbgSig(debugCounter(io.CLOCKCONVERT_AXI.RVALID).io.out, "# cycles CLOCKCONVERT RVALID ")
  // connectDbgSig(debugCounter(io.CLOCKCONVERT_AXI.RREADY & io.CLOCKCONVERT_AXI.RVALID).io.out, "# cycles CLOCKCONVERT RREADY & RVALID ")
  // connectDbgSig(debugCounter(io.CLOCKCONVERT_AXI.WVALID).io.out, "# cycles CLOCKCONVERT WVALID ")
  // connectDbgSig(debugCounter(io.CLOCKCONVERT_AXI.WREADY & io.CLOCKCONVERT_AXI.WVALID).io.out, "# cycles CLOCKCONVERT WREADY & WVALID ")
  // connectDbgSig(debugCounter(io.CLOCKCONVERT_AXI.BVALID).io.out, "# cycles CLOCKCONVERT BVALID ")
  // connectDbgSig(debugCounter(io.CLOCKCONVERT_AXI.BREADY & io.CLOCKCONVERT_AXI.BVALID).io.out, "# cycles CLOCKCONVERT BREADY & BVALID ")
  // connectDbgSig(debugFF(io.CLOCKCONVERT_AXI.ARADDR, io.CLOCKCONVERT_AXI.ARVALID & io.CLOCKCONVERT_AXI.ARREADY).io.out, "Last CLOCKCONVERT ARADDR")
  // connectDbgSig(debugFF(io.CLOCKCONVERT_AXI.ARLEN, io.CLOCKCONVERT_AXI.ARVALID & io.CLOCKCONVERT_AXI.ARREADY).io.out, "Last CLOCKCONVERT ARLEN")
  // connectDbgSig(debugFF(io.CLOCKCONVERT_AXI.ARSIZE, io.CLOCKCONVERT_AXI.ARVALID & io.CLOCKCONVERT_AXI.ARREADY).io.out, "Last CLOCKCONVERT ARSIZE")
  // // connectDbgSig(debugFF(io.CLOCKCONVERT_AXI.ARID, io.CLOCKCONVERT_AXI.ARVALID & io.CLOCKCONVERT_AXI.ARREADY).io.out, "Last CLOCKCONVERT ARID")
  // connectDbgSig(debugFF(io.CLOCKCONVERT_AXI.ARBURST, io.CLOCKCONVERT_AXI.ARVALID & io.CLOCKCONVERT_AXI.ARREADY).io.out, "Last CLOCKCONVERT ARBURST")
  // // connectDbgSig(debugCounter(io.CLOCKCONVERT_AXI.ARLOCK).io.out, "# cycles CLOCKCONVERT ARLOCK ")
  // connectDbgSig(debugFF(io.CLOCKCONVERT_AXI.AWADDR, io.CLOCKCONVERT_AXI.AWVALID & io.CLOCKCONVERT_AXI.AWREADY).io.out, "Last CLOCKCONVERT AWADDR")

  val debugFileName = "cpp/generated_debugRegs.h"
  val debugPW = new PrintWriter(new File(debugFileName))
  debugPW.println(s"""
                     |  #ifndef __DEBUG_REGS_H__
                     |  #define __DEBUG_REGS_H__
   """.stripMargin
  )

  if (isDebugChannel) {
    // Print all debugging signals into a header file
    debugPW.println(s"""

  #define NUM_DEBUG_SIGNALS ${signalLabels.size}

  const char *signalLabels[] = {
  """)

    debugPW.println(signalLabels.map{ l => s"""\"${l}\"""" }.mkString(", "))
    debugPW.println("};")
  }
  debugPW.println("#endif // __DEBUG_REGS_H__")
  debugPW.close
}
