package fringe.templates.memory

import chisel3._
import chisel3.util._

import fringe._

class DRAMAllocator(appReqCount: Int) extends Module {
  val io = IO(new Bundle {
    val appReq = Vec(appReqCount, Flipped(Valid(new HeapReq)))

    val heapReq = Valid(new HeapReq)
    val heapResp = Flipped(Valid(new HeapResp))

    val alloc = Output(Bool())
    val size = Output(UInt(64.W))
    val addr = Output(UInt(64.W))
  })

  val reqIdx = PriorityEncoder(io.appReq.map { _.valid })
  val appReq = io.appReq(reqIdx)

  var alloc = RegInit(false.B)
  var size = RegInit(0.U)
  var addr = RegInit(0.U)

  when (io.heapResp.valid | appReq.valid) {
    alloc := Mux(io.heapResp.valid, io.heapResp.bits.allocDealloc, appReq.bits.allocDealloc)
  }
  when (io.heapResp.valid) {
    addr := io.heapResp.bits.sizeAddr
  }
  when (appReq.valid) {
    size := appReq.bits.sizeAddr
  }

  io.alloc := alloc
  io.size := size
  io.addr := addr

  io.heapReq.valid := appReq.valid
  io.heapReq.bits.allocDealloc := appReq.bits.allocDealloc
  io.heapReq.bits.sizeAddr := Mux(appReq.bits.allocDealloc, appReq.bits.sizeAddr, addr)
}
