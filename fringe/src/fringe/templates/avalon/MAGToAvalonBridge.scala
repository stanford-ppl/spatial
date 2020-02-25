package fringe.templates.avalon

import chisel3._
import chisel3.util.{Cat, Decoupled, DecoupledIO, Queue}
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
  private val tag = cmd.bits.tag.asUInt()
  // TODO: Not sure if this would help...
  private val tagQueueW = Module(new Queue(UInt(32.W), p.tagQueueSize))
  private val tagQueueR = Module(new Queue(UInt(32.W), p.tagQueueSize))

  private val wData = io.in.wdata
  private val wResp = io.in.wresp
  private val rResp = io.in.rresp
  private val size = io.in.cmd.bits.size
  private val master = io.M_AVALON
  private val slaveReady = ~master.waitRequest
  private val isWr = cmd.bits.isWr
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
  master.read := ~isWr
  rResp.bits.rdata := readDataVector
  master.write := isWr
  master.writeData := wData.bits.wdata.reverse.reduce { Cat(_, _) }
  wData.ready := slaveReady
  // In avalon, we don't need to decouple cmd and transfer.
  // I'm setting cmd ready to always high for now.

  // Tag management
  private val (tagWEnqIO, tagWDeqIO, tagREnqIO, tagRDeqIO) = (
    tagQueueW.io.enq,
    tagQueueW.io.deq,
    tagQueueR.io.enq,
    tagQueueR.io.deq
  )

  // Master to slave
  tagWEnqIO.valid := cmd.valid && isWr
  tagREnqIO.valid := cmd.valid && (~isWr).toBool()
  tagWEnqIO.bits := tag
  tagREnqIO.bits := tag

  // Slave to master
  tagWDeqIO.ready := master.writeResponseValid
  tagRDeqIO.ready := master.readDataValid

  rResp.valid := tagRDeqIO.valid
  wResp.valid := tagWDeqIO.valid

  rResp.bits.tag := tagRDeqIO.bits
  wResp.bits.tag := tagWDeqIO.bits
}
