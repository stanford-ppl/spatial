package fringe.templates.dramarbiter

import chisel3._
import chisel3.util._
import fringe.templates.memory._

class FIFOIO[T <: Data](t: T, depth: Int) extends Bundle {
  val in = Flipped(Decoupled(t.cloneType))
  val out = Decoupled(t.cloneType)

  class Bank[T <: Data] extends Bundle {
    val wdata = Flipped(Valid(t.cloneType))
    val rdata = Valid(t.cloneType)
    override def cloneType: this.type = new Bank().asInstanceOf[this.type]
  }

  val banks = Vec(depth, new Bank)

  override def cloneType: this.type = new FIFOIO(t, depth).asInstanceOf[this.type]
}

class FIFO[T <: Data](t: T, depth: Int, banked: Boolean = false) extends Module {
  assert(isPow2(depth))

  val addrWidth = log2Ceil(depth)

  val io = IO(new FIFOIO(t, depth))
  io <> DontCare

  val writeEn = io.in.valid & io.in.ready
  val readEn = io.out.valid & io.out.ready

  val counterW = log2Ceil(depth)
  val enqCounter = Module(new Counter(counterW))
  enqCounter.io <> DontCare
  enqCounter.io.enable := writeEn
  enqCounter.io.stride := 1.U
  val deqCounter = Module(new Counter(counterW))
  deqCounter.io <> DontCare
  deqCounter.io.enable := readEn
  deqCounter.io.stride := 1.U
  val maybeFull = RegInit(false.B)

  val ptrMatch = enqCounter.io.out === deqCounter.io.out
  val empty = ptrMatch && !maybeFull
  val full = ptrMatch && maybeFull

  if (t.getWidth == 1 || banked) {
    val m = Module(new FFRAM(t, depth))
    m.io <> DontCare

    m.io.raddr := deqCounter.io.out
    m.io.wen := writeEn
    m.io.waddr := enqCounter.io.out
    m.io.wdata := io.in.bits
    io.out.bits := m.io.rdata

    m.io.banks.zip(io.banks).foreach { case (a, b) =>
      a.wdata := b.wdata
      b.rdata.bits := a.rdata
    }

    List.tabulate(depth) { i =>
      val valid = RegInit(false.B)
      io.banks(i).rdata.valid := valid
      val wen = writeEn & (enqCounter.io.out === i.U)
      val ren = readEn & (deqCounter.io.out === i.U)
      when (wen | ren) {
        valid := wen & !ren
      }
    }
  } else {
    val m = Module(new SRAM(t, depth, "VIVADO_SELECT"))

    m.io.raddr := Mux(readEn, deqCounter.io.next, deqCounter.io.out)
    m.io.wen := writeEn
    m.io.waddr := enqCounter.io.out
    m.io.wdata := io.in.bits
    io.out.bits := m.io.rdata
    m.io.backpressure := true.B
  }

  when (writeEn =/= readEn) {
    maybeFull := writeEn
  }

  io.out.valid := !empty
  io.in.ready := !full
}

