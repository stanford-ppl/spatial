package fringe.templates.avalon

import chisel3._
import chisel3.util.{Cat, Decoupled, DecoupledIO, Queue}
import fringe.{DRAMCommand, DRAMStream, DRAMTag, globals}
import fringe.templates.axi4.{AvalonBundleParameters, AvalonMaster}

// TODO: Modify this queue so that it's a Tag queue.
class TagQueue(val p: AvalonBundleParameters)
    extends Module {
  val io = IO(new Bundle {
    val enqTag = Flipped(Decoupled(UInt(p.dramTagBits.W)))
    val deqTag: DecoupledIO[UInt] = Decoupled(UInt(p.dramTagBits.W))
    val enqIsWr = Flipped(Decoupled(Bool()))
    val deqIsWr: DecoupledIO[Bool] = Decoupled(Bool())
  })
  chisel3.core.dontTouch(io)
  io.deqTag <> Queue(io.enqTag, p.tagQueueSize)
  io.deqIsWr <> Queue(io.enqIsWr, p.tagQueueSize)
}

class MAGToAvalonBridge(val p: AvalonBundleParameters) extends Module {
  val io = IO(new Bundle {
    val in =
      Flipped(new DRAMStream(globals.DATA_WIDTH, globals.WORDS_PER_STREAM))
    scala.Console.println(
      s"MAGToAvalonBridge.in: data_width = ${globals.DATA_WIDTH}, word_per_stream = ${globals.WORDS_PER_STREAM}")
    val M_AVALON = new AvalonMaster(p)
  })

  // TODO: For now I'm connecting response to DontCare.
  //  Later we need to figure out how to properly use this signal to back-pressure Spatial,
  //  if the data is corrupted...
  io.M_AVALON.response <> DontCare
  io.M_AVALON.chipSelect <> DontCare
  private val cmd = io.in.cmd
  private val tag = io.in.cmd.bits.tag
  private val isWr = io.in.cmd.bits.isWr

  // This one handles returning back the correct tag.
  private val tagQueue = Module(new TagQueue(p))

  private val wData = io.in.wdata
  private val wResp = io.in.wresp
  private val rResp = io.in.rresp
  private val master = io.M_AVALON
//  private val slaveReady = (~master.waitRequest).toBool()
  private val slaveReady = (~master.waitRequest).toBool()
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

  master.writeData := wData.bits.wdata.reverse.reduce { Cat(_, _) }
  private val cmdCanIssue = slaveReady && cmd.valid
  master.write := cmdCanIssue && cmd.bits.isWr
  master.read := cmdCanIssue && (~cmd.bits.isWr).toBool()
  master.burstCount := cmd.bits.size
  master.address := cmd.bits.addr
  wData.ready := slaveReady

  cmd.ready := slaveReady

  private val canIssueCmd = cmd.valid && cmd.ready
  tagQueue.io.enqTag.valid := canIssueCmd
  tagQueue.io.enqIsWr.valid := canIssueCmd
  tagQueue.io.enqTag.bits := tag
  tagQueue.io.enqIsWr.bits := isWr

  private val canDeqTag = (wResp.ready && master.writeResponseValid) || (rResp.ready && master.readDataValid)
  tagQueue.io.deqTag.ready := canDeqTag
  tagQueue.io.deqIsWr.ready := canDeqTag

//  wResp.valid := canDeqTag && tagQueue.io.deqIsWr.bits
  wResp.valid := master.writeResponseValid
  wResp.bits.tag := tagQueue.io.deqTag.bits

  rResp.valid := canDeqTag && (~tagQueue.io.deqIsWr.bits).toBool
  rResp.bits.tag := tagQueue.io.deqTag.bits
  rResp.bits.rdata := readDataVector
}
