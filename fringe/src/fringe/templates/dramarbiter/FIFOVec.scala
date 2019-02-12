package fringe.templates.dramarbiter

import chisel3._
import chisel3.util._
import fringe.templates.memory._
import fringe.utils.log2Up

class FIFOVec[T <: Data](t: T, depth: Int, v: Int) extends Module {
  assert(isPow2(depth))
  assert(isPow2(v))
  assert(depth % v == 0, s"$depth, $v")

  val io = IO(new Bundle {
    val in = Flipped(Decoupled(Vec(v, t.cloneType)))
    val out = Decoupled(Vec(v, t.cloneType))
    val chainEnq = Input(Bool())
    val chainDeq = Input(Bool())
  })

  val readEn = io.out.valid & io.out.ready
  val writeEn = io.in.valid & io.in.ready

  val counterW = log2Ceil(v)
  val enqCounter = Module(new Counter(counterW))
  enqCounter.io <> DontCare
  enqCounter.io.enable := writeEn & io.chainEnq
  enqCounter.io.stride := 1.U
  val deqCounter = Module(new Counter(counterW))
  deqCounter.io <> DontCare
  deqCounter.io.enable :=  readEn & io.chainDeq
  deqCounter.io.stride := 1.U

  val enqDecoder = UIntToOH(enqCounter.io.out)
  val deqDecoder = UIntToOH(deqCounter.io.out)

  val rAddr = deqCounter.io.out

  val d = depth / v
  val fifos = List.tabulate(v) { i =>
    val m = Module(new FIFO(t, d))
    m.io <> DontCare
    m.io.in.valid := Mux(io.chainEnq, enqDecoder(i), true.B) & writeEn
    m.io.in.bits := Mux(io.chainEnq, io.in.bits(0), io.in.bits(i))
    m.io.out.ready := Mux(io.chainDeq, deqDecoder(i), true.B) & readEn
    m
  }

  val inReady = fifos.map { _.io.in.ready }
  io.in.ready := Mux(io.chainEnq, VecInit(inReady)(enqCounter.io.out), inReady.reduce { _&_ })
  val outValid = fifos.map { _.io.out.valid }
  io.out.valid := Mux(io.chainDeq, VecInit(outValid)(deqCounter.io.out), outValid.reduce { _&_ })
  val outBits = fifos.map { _.io.out.bits }
  io.out.bits := Mux(io.chainDeq, VecInit(List.fill(v) { VecInit(outBits)(rAddr) }), VecInit(outBits))
}
