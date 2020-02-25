package fringe.templates.avalon

import chisel3._
import chisel3.util.{Cat, Decoupled, Queue}
import fringe.globals
import fringe.DRAMStream
import fringe.templates.axi4.{AvalonBundleParameters, AvalonMaster}

class MAGToAvalonBridge(val p: AvalonBundleParameters) extends Module {
  val io = IO(new Bundle {
    val in =
      Flipped(new DRAMStream(globals.DATA_WIDTH, globals.WORDS_PER_STREAM))
    scala.Console.println(
      s"MAGToAvalonBridge.in: data_width = ${globals.DATA_WIDTH}, word_per_stream = ${globals.WORDS_PER_STREAM}")
    val M_AVALON = new AvalonMaster(p)
  })

  io <> DontCare
  // TODO: How is dram cmd ID handled?
  val numPipelinedLevels: Int = globals.magPipelineDepth
  private val cmd = io.in.cmd
  private val tag = cmd.bits.tag
  private val tagQueue = Queue(Flipped(Decoupled(tag)), p.tagQueueSize)
  private val wData = io.in.wdata
  private val wResp = io.in.wresp
  private val rResp = io.in.rresp
  private val size = io.in.cmd.bits.size
  private val master = io.M_AVALON
  private val slaveReady = ~master.waitRequest
  private val readDataVector = VecInit(
    List
      .tabulate(globals.EXTERNAL_V) { i =>
        // Original formulation:
        // io.M_AXI.RDATA((
        //  globals.EXTERNAL_W*globals.EXTERNAL_V) - 1 - i*globals.EXTERNAL_W,
        //  (globals.EXTERNAL_W*globals.EXTERNAL_V) - 1 - i*globals.EXTERNAL_W -
        //    (globals.EXTERNAL_W-1)
        //  )
        val start = (globals.EXTERNAL_W * globals.EXTERNAL_V) - 1 - i * globals.EXTERNAL_W
        val end = start - (globals.EXTERNAL_W - 1)
        master.readData(start, end)
      }
      .reverse
  )

  master.burstCount := size
  master.address := cmd.bits.addr
  master.read := ~cmd.bits.isWr
  rResp.bits.rdata := readDataVector
  master.write := cmd.bits.isWr
  master.writeData := wData.bits.wdata.reverse.reduce { Cat(_, _) }
  wData.ready := slaveReady
  // In avalon, we don't need to decouple cmd and transfer.
  // I'm setting cmd ready to always high for now.
  cmd.ready := true.B
  rResp.valid := master.readDataValid
  wResp.valid := master.writeResponseValid
  when(cmd.ready && cmd.valid) {
    tagQueue.enq(tag)
  }.elsewhen(master.readDataValid && rResp.ready) {
      rResp.bits.tag := tagQueue.deq
    }
    .elsewhen(master.writeResponseValid && wResp.ready) {
      wResp.bits.tag := tagQueue.deq
    }

  // TODO: How do we deal with tags?
  //  Qsys interconnect ensures in-order transfers.
}
