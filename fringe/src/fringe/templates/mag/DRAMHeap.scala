package fringe.templates.mag

import chisel3._
import chisel3.util._

import fringe._
import fringe.utils._

class DRAMHeap (
  val numAlloc: Int
) extends Module {

  val io = IO(new Bundle {
    val accel = new HeapIO(numAlloc)
    val host = Flipped(new HeapIO(1))
  })

  val reqIdx = PriorityEncoder(io.accel.req.map { _.valid })
  val req = io.accel.req(reqIdx)

  io.host.req(0) := req

  io.accel.resp.zipWithIndex.foreach { case (resp, i) =>
    resp.valid := (reqIdx === i.U) & io.host.resp(0).valid
    resp.bits := io.host.resp(0).bits
  }
}
