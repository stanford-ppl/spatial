package fringe.templates.heap

import chisel3._
import chisel3.util._

import fringe._
import fringe.utils._

class DRAMHeap (
  val numAlloc: Int
) extends Module {

  val io = IO(new Bundle {
    val accel = Vec(numAlloc, new HeapIO())
    val host = Flipped(Vec(1, new HeapIO()))
  })

  val reqIdx = PriorityEncoder(io.accel.map(_.req.valid))
  val req = io.accel(reqIdx).req

  io.host(0).req := req

  io.accel.zipWithIndex.foreach { case (p, i) =>
    p.resp.valid := (reqIdx === i.U) & io.host(0).resp.valid
    p.resp.bits := io.host(0).resp.bits
  }
}
