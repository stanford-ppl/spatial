package fringe.templates.memory

import chisel3._
import chisel3.util._

import fringe._

class DRAMAllocator(rank: Int, appReqCount: Int) extends Module {
  class AppReq(rank: Int) extends Bundle {
    val allocDealloc = Bool()
    val dims = Vec(rank, UInt(32.W))

    override def cloneType(): this.type = new AppReq(rank).asInstanceOf[this.type]
  }

  val appReq = Vec(appReqCount, Flipped(Valid(new AppReq(rank))))
  val heapResp = Flipped(Valid(new HeapResp))

  val output = new Bundle {
    val heapReq = Valid(new HeapReq)
    val isAlloc = Output(Bool())
    val size = Output(UInt(64.W))
    val dims = Output(Vec(rank, UInt(32.W)))
    val addr = Output(UInt(64.W))
  }

  def connectLedger(op: DRAMAllocatorIO)(implicit stack: List[KernelHash]): Unit = {
    if (stack.isEmpty) this <> op
    else {
      val cxn = Ledger.lookup(op.hashCode)
      cxn.allocDealloc.foreach{p => appReq(p) := op.appReq(p)}
      Ledger.substitute(op.hashCode, this.hashCode)
    }
  }

  def connectAlloc(lane: Int, dims: List[UInt], en: Bool)(implicit stack: List[KernelHash]): Unit = {
    appReq(lane).valid := en
    appReq(lane).bits.allocDealloc := true.B
    appReq(lane).bits.dims.zip(dims).foreach {case (l,r) => l := r}
    Ledger.connectAllocDealloc(this.hashCode, lane)
  }

  def connectDealloc(lane: Int, en: Bool)(implicit stack: List[KernelHash]): Unit = {
    appReq(lane).valid := en
    appReq(lane).bits.allocDealloc := false.B
    Ledger.connectAllocDealloc(this.hashCode, lane)
  }

  override def cloneType = (new DRAMAllocatorIO(rank, appReqCount)).asInstanceOf[this.type] // See chisel3 bug 358
}

    val heapReq = Valid(new HeapReq)
    val heapResp = Flipped(Valid(new HeapResp))

    val isAlloc = Output(Bool())
    val size = Output(UInt(64.W))
    val dims = Output(Vec(rank, UInt(32.W)))
    val addr = Output(UInt(64.W))
  })

  io <> DontCare

  val reqIdx = PriorityEncoder(io.appReq.map { _.valid })
  val appReq = io.appReq(reqIdx)

  val inSize = appReq.bits.dims.reduce { _*_ }

  var alloc = RegInit(false.B)
  var size = RegInit(0.U)
  var dims = RegInit(VecInit(Seq.fill(rank) { 0.U }))
  var addr = RegInit(0.U)

  when (io.heapResp.valid | appReq.valid) {
    alloc := Mux(io.heapResp.valid, io.heapResp.bits.allocDealloc, appReq.bits.allocDealloc)
  }
  when (io.heapResp.valid) {
    addr := io.heapResp.bits.sizeAddr
  }
  when (appReq.valid) {
    size := inSize
    dims := appReq.bits.dims
  }

  io.output.isAlloc := alloc
  io.output.size := size
  io.output.addr := addr

  io.output.heapReq.valid := appReq.valid
  io.output.heapReq.bits.allocDealloc := appReq.bits.allocDealloc
  io.output.heapReq.bits.sizeAddr := Mux(appReq.bits.allocDealloc, inSize, addr)
}
