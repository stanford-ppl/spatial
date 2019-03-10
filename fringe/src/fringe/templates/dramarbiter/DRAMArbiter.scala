package fringe.templates.dramarbiter

import chisel3._
import chisel3.util._

import fringe._
import fringe.globals._
import fringe.templates.axi4._

import java.io.{File, PrintWriter}
import scala.collection.mutable.ListBuffer

class DRAMArbiter(
  val loadStreamInfo: List[StreamParInfo],
  val storeStreamInfo: List[StreamParInfo],
  val gatherStreamInfo: List[StreamParInfo],
  val scatterStreamInfo: List[StreamParInfo],
  val axiParams: AXI4BundleParameters,
  val isDebugChannel: Boolean = false
) extends Module {

  val numDebugs = 500
  val axiLiteParams = new AXI4BundleParameters(64, 512, 1)

  val io = IO(new Bundle {
    val enable = Input(Bool())
    val reset = Input(Bool())
    val app = new AppStreams(LOAD_STREAMS, STORE_STREAMS, GATHER_STREAMS, SCATTER_STREAMS)
    val dram = new DRAMStream(EXTERNAL_W, EXTERNAL_V)

    val debugSignals = Output(Vec(numDebugs, UInt(32.W)))
    val TOP_AXI = new AXI4Probe(axiLiteParams)
    val DWIDTH_AXI = new AXI4Probe(axiLiteParams)
    val PROTOCOL_AXI = new AXI4Probe(axiLiteParams)
    val CLOCKCONVERT_AXI = new AXI4Probe(axiLiteParams)
  })

  io <> DontCare

  val numStreams = loadStreamInfo.size + storeStreamInfo.size +
                   gatherStreamInfo.size + scatterStreamInfo.size

  // Print all debugging signals into a header file
  val debugFileName = "cpp/generated_debugRegs.h"
  
  val debugPW = if (isDebugChannel) {
    val debugPW = new PrintWriter(new File(debugFileName))
    debugPW.println(s"""#ifndef __DEBUG_REGS_H__""")
    debugPW.println(s"""#define __DEBUG_REGS_H__""")
    Some(debugPW)
  } else None

  val signalLabels = if (numStreams > 0) {
    val streamTagWidth = log2Ceil(numStreams)
    assert(streamTagWidth <= (new DRAMTag(64)).streamID.getWidth)


    val loadControllers = loadStreamInfo.zipWithIndex.map { case (s, i) =>
      val load = io.app.loads(i)
      val m = Module(new StreamControllerLoad(s, io.dram, load))
      m.io.load <> load
      m
    }

    val storeControllers = storeStreamInfo.zipWithIndex.map { case (s, i) =>
      val store = io.app.stores(i)
      val m = Module(new StreamControllerStore(s, io.dram, store))
      m.io.store <> store
      m
    }

    val gatherControllers = gatherStreamInfo.zipWithIndex.map { case (s, i) =>
      val gather = io.app.gathers(i)
      val m = Module(new StreamControllerGather(s, io.dram, gather))
      m.io.gather <> gather
      m
    }

    val scatterControllers = scatterStreamInfo.zipWithIndex.map { case (s, i) =>
      val scatter = io.app.scatters(i)
      val m = Module(new StreamControllerScatter(s, io.dram, scatter))
      m.io.scatter <> scatter
      m
    }

    val streamArb = Module(new StreamArbiter(io.dram, numStreams))
    streamArb.io.app <> VecInit(
      loadControllers.map { _.io.dram } ++
      gatherControllers.map { _.io.dram } ++
      storeControllers.map { _.io.dram } ++
      scatterControllers.map { _.io.dram }
    )

    val axiSplit = Module(new AXICmdSplit(io.dram))
    axiSplit.io.in <> streamArb.io.dram

    val axiIssue = Module(new AXICmdIssue(io.dram))
    axiIssue.io.in <> axiSplit.io.out
    io.dram <> axiIssue.io.out
    io.dram.cmd.valid := io.enable & axiIssue.io.out.cmd.valid
    io.dram.wdata.valid := io.enable & axiIssue.io.out.wdata.valid

    // debug
    if (isDebugChannel) {
      var dbgCount = 0
      val signalLabels = ListBuffer[String]()
      def connectDbgSig(sig: UInt, label: String): Unit = {
        if (globals.enableDebugRegs) {
          if (isDebugChannel) {
            io.debugSignals(dbgCount) := sig
            val padded_label = if (label.length < 55) {label + "."*(55-label.length)} else label
            signalLabels.append(padded_label)
            dbgCount += 1
          }
        }
      }

      def debugCounter(en: Bool) = {
        val c = RegInit(0.U(64.W))
        when (en) {
          c := c + 1.U
        }
        c
      }

      def debugFF[T<:Data](sig: T, en: Bool) = {
        val ff = RegInit(sig)
        when (en) {
          ff := sig
        }
        ff
      }

      val cycleCount = debugCounter(io.enable)
      connectDbgSig(cycleCount, "Cycles")

      val rdataEnqCount = debugCounter(io.dram.rresp.valid & io.dram.rresp.ready)
      val wdataCount = debugCounter(io.dram.wdata.valid & io.dram.wdata.ready)
      val appWdataCount = debugCounter(io.app.stores.head.data.valid & io.app.stores.head.data.ready)
      connectDbgSig(wdataCount, "[FRINGE] num wdata transferred (wvalid & wready)")
      connectDbgSig(rdataEnqCount, "[FRINGE] num rdata enqueued (rresp.valid & rresp.ready")

      connectDbgSig(debugCounter(io.dram.cmd.ready & io.dram.cmd.valid), "[FRINGE] # DRAM Commands Issued")
      connectDbgSig(debugCounter(io.dram.cmd.ready & io.dram.cmd.valid & !io.dram.cmd.bits.isWr), "[FRINGE] # Read Commands Sent")
      connectDbgSig(debugCounter(io.dram.cmd.ready),                                              "[FRINGE]      # io.dram.cmd.ready")
      connectDbgSig(debugCounter(io.dram.cmd.valid),                                              "[FRINGE]      # io.dram.cmd.valid")
      connectDbgSig(debugCounter(!io.dram.cmd.bits.isWr),                                         "[FRINGE]      # !io.dram.cmd.bits.isWr")
      connectDbgSig(debugCounter(io.dram.cmd.ready & io.dram.cmd.valid & io.dram.cmd.bits.isWr), "[FRINGE] # Write Commands Sent")

      // Count number of load commands issued from accel per stream
      io.app.loads.zipWithIndex.foreach { case (load, i) =>
        val loadCounterHandshake = debugCounter(io.enable & load.cmd.valid & load.cmd.ready)
        connectDbgSig(loadCounterHandshake, s"[ACCEL] # from load stream $i")
        val signal = s"[FRINGE] # from load stream $i"
      }
      // Count number of store commands issued from accel per stream
      io.app.stores.zipWithIndex.foreach { case (store, i) =>
        val storeCounterHandshake = debugCounter(io.enable & store.cmd.valid & store.cmd.ready)
        connectDbgSig(storeCounterHandshake, s"[ACCEL] # from store stream $i")
      }

      connectDbgSig(debugCounter(io.dram.rresp.valid & io.dram.rresp.ready), "[FRINGE] # Read Responses Acknowledged")
      connectDbgSig(debugCounter(io.dram.rresp.valid & !io.dram.rresp.ready), "[FRINGE] # RResp rejected by ready")
      connectDbgSig(debugCounter(!io.dram.rresp.valid & io.dram.rresp.ready), "[FRINGE] Cycles RResp ready and idle (~valid)")
      loadStreamInfo.indices.foreach{i =>
        val signal = s"[FRINGE]  # from load stream $i"
        connectDbgSig(debugCounter(io.dram.rresp.valid & io.dram.rresp.ready & (io.dram.rresp.bits.getTag.streamID === i.U)), signal)
      }
      connectDbgSig(debugCounter(io.dram.wresp.valid & io.dram.wresp.ready), "[FRINGE] # Write Responses Acknowledged")
      connectDbgSig(debugCounter(io.dram.wresp.valid & !io.dram.wresp.ready), "[FRINGE] # WResp rejected by ready")
      connectDbgSig(debugCounter(!io.dram.wresp.valid & io.dram.wresp.ready), "[FRINGE] Cycles WResp ready and idle (~valid)")
      storeStreamInfo.indices.foreach{i =>
        val signal = s"[FRINGE]  # from store stream $i"
        connectDbgSig(debugCounter(io.dram.wresp.valid & io.dram.wresp.ready & (io.dram.wresp.bits.getTag.streamID === (i+loadStreamInfo.length).U)), signal)
      }

      // Connect fringe io.dram interface debuggers
      connectDbgSig(debugFF(io.dram.cmd.bits.addr, io.dram.cmd.valid && io.dram.cmd.ready), "Last io.dram cmd (leaving fringe)")
      connectDbgSig(debugFF(io.dram.cmd.bits.size, io.dram.cmd.valid && io.dram.cmd.ready), "Last io.dram size (leaving fringe)")
      connectDbgSig(debugFF(io.dram.wdata.bits.wdata.head, io.dram.wdata.valid & io.dram.wdata.ready), "Last io.dram wdata.head (leaving fringe)")
      connectDbgSig(debugFF(io.dram.wdata.bits.wstrb.head, io.dram.wdata.valid & io.dram.wdata.ready), "Last io.dram wstrb.head (leaving fringe)")
      connectDbgSig(debugFF(io.dram.wdata.bits.wdata.head, io.dram.wdata.valid & io.dram.wdata.ready & wdataCount === 0.U), "First io.dram wdata.head (leaving fringe)")
      connectDbgSig(debugFF(io.dram.wdata.bits.wstrb.head, io.dram.wdata.valid & io.dram.wdata.ready & wdataCount === 0.U), "First io.dram wstrb.head (leaving fringe)")
      connectDbgSig(debugFF(io.dram.wdata.bits.wdata.head, io.dram.wdata.valid & io.dram.wdata.ready & wdataCount === 1.U), "Second io.dram wdata.head (leaving fringe)")
      connectDbgSig(debugFF(io.dram.wdata.bits.wstrb.head, io.dram.wdata.valid & io.dram.wdata.ready & wdataCount === 1.U), "Second io.dram wstrb.head (leaving fringe)")
      connectDbgSig(debugFF(io.app.stores.head.cmd.bits.addr, io.app.stores.head.cmd.valid && io.app.stores.head.cmd.ready), "Last store cmd (entering fringe)")
      connectDbgSig(debugFF(io.app.stores.head.cmd.bits.size, io.app.stores.head.cmd.valid && io.app.stores.head.cmd.ready), "Last store size (entering fringe)")
      connectDbgSig(debugFF(io.app.stores.head.data.bits.wdata.head, io.app.stores.head.data.valid & io.app.stores.head.data.ready), "Last store wdata.head (entering fringe)")
      connectDbgSig(debugFF(io.app.stores.head.data.bits.wstrb, io.app.stores.head.data.valid & io.app.stores.head.data.ready), "Last store wstrb.head (entering fringe)")
      connectDbgSig(debugFF(io.app.stores.head.data.bits.wdata.head, io.app.stores.head.data.valid & io.app.stores.head.data.ready & appWdataCount === 0.U), "First store wdata.head (entering fringe)")
      connectDbgSig(debugFF(io.app.stores.head.data.bits.wstrb, io.app.stores.head.data.valid & io.app.stores.head.data.ready & appWdataCount === 0.U), "First store wstrb.head (entering fringe)")
      connectDbgSig(debugFF(io.app.stores.head.data.bits.wdata.head, io.app.stores.head.data.valid & io.app.stores.head.data.ready & appWdataCount === 1.U), "Second store wdata.head (entering fringe)")
      connectDbgSig(debugFF(io.app.stores.head.data.bits.wstrb, io.app.stores.head.data.valid & io.app.stores.head.data.ready & appWdataCount === 1.U), "Second store wstrb.head (entering fringe)")

      // Connect AXI loopback debuggers
      // TOP
      connectDbgSig(debugCounter(io.TOP_AXI.ARVALID), "# cycles TOP ARVALID ")
      connectDbgSig(debugCounter(io.TOP_AXI.ARREADY), "# cycles TOP ARREADY")
      connectDbgSig(debugCounter(io.TOP_AXI.ARREADY & io.TOP_AXI.ARVALID), "# cycles TOP ARREADY & ARVALID ")
      connectDbgSig(debugCounter(io.TOP_AXI.AWVALID), "# cycles TOP AWVALID ")
      connectDbgSig(debugCounter(io.TOP_AXI.AWREADY & io.TOP_AXI.AWVALID), "# cycles TOP AWREADY & AWVALID ")
      connectDbgSig(debugCounter(io.TOP_AXI.RVALID), "# cycles TOP RVALID ")
      connectDbgSig(debugCounter(io.TOP_AXI.RREADY & io.TOP_AXI.RVALID), "# cycles TOP RREADY & RVALID ")
      connectDbgSig(debugCounter(io.TOP_AXI.WVALID), "# cycles TOP WVALID ")
      connectDbgSig(debugCounter(io.TOP_AXI.WREADY & io.TOP_AXI.WVALID), "# cycles TOP WREADY & WVALID ")
      connectDbgSig(debugCounter(!io.TOP_AXI.WREADY & io.TOP_AXI.WVALID), "# cycles TOP ~WREADY & WVALID (forced)" )
      connectDbgSig(debugCounter(!io.TOP_AXI.WREADY), "# cycles TOP ~WREADY" )
      connectDbgSig(debugCounter(io.TOP_AXI.BVALID), "# cycles TOP BVALID ")
      connectDbgSig(debugCounter(io.TOP_AXI.BREADY & io.TOP_AXI.BVALID), "# cycles TOP BREADY & BVALID ")
      connectDbgSig(debugFF(io.TOP_AXI.ARADDR, io.TOP_AXI.ARVALID & io.TOP_AXI.ARREADY), "Last TOP ARADDR")
      connectDbgSig(debugFF(io.TOP_AXI.ARLEN, io.TOP_AXI.ARVALID & io.TOP_AXI.ARREADY), "Last TOP ARLEN")
      connectDbgSig(debugFF(io.TOP_AXI.ARSIZE, io.TOP_AXI.ARVALID & io.TOP_AXI.ARREADY), "Last TOP ARSIZE")
      connectDbgSig(debugFF(io.TOP_AXI.ARID, io.TOP_AXI.ARVALID & io.TOP_AXI.ARREADY), "Last TOP ARID")
      connectDbgSig(debugFF(io.TOP_AXI.ARBURST, io.TOP_AXI.ARVALID & io.TOP_AXI.ARREADY), "Last TOP ARBURST")
      connectDbgSig(debugFF(io.TOP_AXI.AWADDR, io.TOP_AXI.AWVALID & io.TOP_AXI.AWREADY), "Last TOP AWADDR")
      connectDbgSig(debugFF(io.TOP_AXI.AWLEN, io.TOP_AXI.AWVALID & io.TOP_AXI.AWREADY), "Last TOP AWLEN")
      connectDbgSig(debugFF(io.TOP_AXI.WDATA, io.TOP_AXI.WVALID & io.TOP_AXI.WREADY), "Last TOP WDATA")
      connectDbgSig(debugFF(io.TOP_AXI.WSTRB, io.TOP_AXI.WVALID & io.TOP_AXI.WREADY), "Last TOP WSTRB")
      connectDbgSig(debugFF(io.TOP_AXI.WDATA, io.TOP_AXI.WVALID & io.TOP_AXI.WREADY & wdataCount === 0.U), "First TOP WDATA")
      connectDbgSig(debugFF(io.TOP_AXI.WSTRB, io.TOP_AXI.WVALID & io.TOP_AXI.WREADY & wdataCount === 0.U), "First TOP WSTRB")
      connectDbgSig(debugFF(io.TOP_AXI.WDATA, io.TOP_AXI.WVALID & io.TOP_AXI.WREADY & wdataCount === 1.U), "Second TOP WDATA")
      connectDbgSig(debugFF(io.TOP_AXI.WSTRB, io.TOP_AXI.WVALID & io.TOP_AXI.WREADY & wdataCount === 1.U), "Second TOP WSTRB")

      // // // DWIDTH
      // connectDbgSig(debugCounter(io.DWIDTH_AXI.ARVALID), "# cycles DWIDTH ARVALID ")
      // connectDbgSig(debugCounter(io.DWIDTH_AXI.ARREADY), "# cycles DWIDTH ARREADY ")
      // connectDbgSig(debugCounter(io.DWIDTH_AXI.ARREADY & io.DWIDTH_AXI.ARVALID), "# cycles DWIDTH ARREADY & ARVALID ")
      // connectDbgSig(debugCounter(io.DWIDTH_AXI.AWVALID), "# cycles DWIDTH AWVALID ")
      // connectDbgSig(debugCounter(io.DWIDTH_AXI.AWREADY & io.DWIDTH_AXI.AWVALID), "# cycles DWIDTH ARREADY & AWVALID ")
      // connectDbgSig(debugCounter(io.DWIDTH_AXI.RVALID), "# cycles DWIDTH RVALID ")
      // connectDbgSig(debugCounter(io.DWIDTH_AXI.RREADY & io.DWIDTH_AXI.RVALID), "# cycles DWIDTH RREADY & RVALID ")
      // connectDbgSig(debugCounter(io.DWIDTH_AXI.WVALID), "# cycles DWIDTH WVALID ")
      // connectDbgSig(debugCounter(io.DWIDTH_AXI.WREADY & io.DWIDTH_AXI.WVALID), "# cycles DWIDTH WREADY & WVALID ")
      // connectDbgSig(debugCounter(!io.DWIDTH_AXI.WREADY & io.DWIDTH_AXI.WVALID), "# cycles DWIDTH ~WREADY & WVALID (forced)" )
      // connectDbgSig(debugCounter(!io.DWIDTH_AXI.WREADY), "# cycles DWIDTH ~WREADY" )
      // connectDbgSig(debugCounter(io.DWIDTH_AXI.BVALID), "# cycles DWIDTH BVALID ")
      // connectDbgSig(debugCounter(io.DWIDTH_AXI.BREADY & io.DWIDTH_AXI.BVALID), "# cycles DWIDTH BREADY & BVALID ")
      // connectDbgSig(debugFF(io.DWIDTH_AXI.ARADDR, io.DWIDTH_AXI.ARVALID & io.DWIDTH_AXI.ARREADY), "Last DWIDTH ARADDR")
      // connectDbgSig(debugFF(io.DWIDTH_AXI.ARLEN, io.DWIDTH_AXI.ARVALID & io.DWIDTH_AXI.ARREADY), "Last DWIDTH ARLEN")
      // connectDbgSig(debugFF(io.DWIDTH_AXI.ARSIZE, io.DWIDTH_AXI.ARVALID & io.DWIDTH_AXI.ARREADY), "Last DWIDTH ARSIZE")
      // // connectDbgSig(debugFF(io.DWIDTH_AXI.ARID, io.DWIDTH_AXI.ARVALID & io.DWIDTH_AXI.ARREADY), "Last DWIDTH ARID")
      // connectDbgSig(debugFF(io.DWIDTH_AXI.ARBURST, io.DWIDTH_AXI.ARVALID & io.DWIDTH_AXI.ARREADY), "Last DWIDTH ARBURST")
      // // connectDbgSig(debugCounter(io.DWIDTH_AXI.ARLOCK), "# cycles DWIDTH ARLOCK ")
      // connectDbgSig(debugFF(io.DWIDTH_AXI.AWADDR, io.DWIDTH_AXI.AWVALID & io.DWIDTH_AXI.AWREADY), "Last DWIDTH AWADDR")
      // connectDbgSig(debugFF(io.DWIDTH_AXI.AWLEN, io.DWIDTH_AXI.AWVALID & io.DWIDTH_AXI.AWREADY), "Last DWIDTH AWLEN")
      // connectDbgSig(debugFF(io.DWIDTH_AXI.WDATA, io.DWIDTH_AXI.WVALID & io.DWIDTH_AXI.WREADY), "Last DWIDTH WDATA")
      // connectDbgSig(debugFF(io.DWIDTH_AXI.WSTRB, io.DWIDTH_AXI.WVALID & io.DWIDTH_AXI.WREADY), "Last DWIDTH WSTRB")
      // connectDbgSig(debugFF(io.DWIDTH_AXI.WDATA, io.DWIDTH_AXI.WVALID & io.DWIDTH_AXI.WREADY & wdataCount === 0.U), "First DWIDTH WDATA")
      // connectDbgSig(debugFF(io.DWIDTH_AXI.WSTRB, io.DWIDTH_AXI.WVALID & io.DWIDTH_AXI.WREADY & wdataCount === 0.U), "First DWIDTH WSTRB")
      // connectDbgSig(debugFF(io.DWIDTH_AXI.WDATA, io.DWIDTH_AXI.WVALID & io.DWIDTH_AXI.WREADY & wdataCount === 1.U), "Second DWIDTH WDATA")
      // connectDbgSig(debugFF(io.DWIDTH_AXI.WSTRB, io.DWIDTH_AXI.WVALID & io.DWIDTH_AXI.WREADY & wdataCount === 1.U), "Second DWIDTH WSTRB")

      signalLabels
    } else ListBuffer[String]()
  } else ListBuffer[String]()

  if (isDebugChannel) {
    debugPW.get.println(s"""#define NUM_DEBUG_SIGNALS ${signalLabels.size}""")
    debugPW.get.println(s"""const char *signalLabels[] = {""")

    debugPW.get.println(signalLabels.map { l => s"""\"${l}\"""" }.mkString(", "))
    debugPW.get.println("};")

    debugPW.get.println("#endif // __DEBUG_REGS_H__")
    debugPW.get.close()
  }

}

