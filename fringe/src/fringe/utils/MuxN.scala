package fringe.utils

import chisel3._
import chisel3.util._

import fringe.templates.memory.FringeFF

class MuxN[T<:Data](val t: T, val numInputs: Int) extends Module {
  val numSelectBits = log2Ceil(numInputs)
  val io = IO(new Bundle {
    val ins = Input(Vec(numInputs, t.cloneType))
    val sel = Input(Bits(numSelectBits.W))
    val out = Output(t.cloneType)
  })

  io.out := io.ins(io.sel)
}

class MuxPipe[T<:Data](val t: T, val numInputs: Int) extends Module {
  val logInputs = log2Ceil(numInputs)

  val io = IO(new Bundle {
    val in = Flipped(Decoupled(Vec(numInputs, t.cloneType)))
    val sel = Input(Bits(logInputs.W))
    val out = Decoupled((t.cloneType))
  })

  val enable = io.out.ready | !io.out.valid


  def splitMux(input: Vec[T], stages: Int): (T, Bits) = {
    def slice(v: Vec[T], start: Int, end: Int) = {
      Vec(for (i <- start to end) yield v(i))
    }

    stages match {
      case 0 =>
        (input(0), false.B)
      case 1 =>
        val m = Module(new MuxN(t, input.length))
        m.io.ins := input
        val c = log2Ceil(input.length)
        val sel = io.sel
        m.io.sel := sel(c - 1, 0)
        val s = if (sel.getWidth > c) sel(sel.getWidth - 1, c) else 0.U
        (m.io.out, s)
      case _ =>
        val inL = slice(input, 0, (input.length / 2) - 1)
        val inH = slice(input, input.length / 2, input.length - 1)
        val (muxL, selL) = splitMux(inL, stages - 1)
        val (muxH, selH) = splitMux(inH, stages - 1)

        val selFF = getFF(selL, enable)

        val m = Module(new MuxN(t, 2))
        m.io.ins := Vec(getFF(muxL, enable), getFF(muxH, enable))
        m.io.sel := selFF(0)
        val s = if (selFF.getWidth > 1) selFF(selFF.getWidth - 1, 1) else 0.U
        (m.io.out, s)
    }
  }

  def pad(v: Vec[T]): Vec[T] = {
    val c = scala.math.pow(2, log2Ceil(v.length)).toInt
    Vec(for (i <- 0 until c) yield if (i < v.length) v(i) else v(0))
  }

  val (m, s) = splitMux(pad(io.in.bits.data), logInputs)
  val delay = (logInputs - 1).max(0)
  io.out.valid := getRetimed(io.in.valid, delay, enable)
  io.in.ready := enable
  io.out.bits := m
}

class MuxNReg(val numInputs: Int, w: Int) extends Module {
  val numSelectBits = log2Ceil(numInputs)
  val io = IO(new Bundle {
    val ins = Input(Vec(numInputs, Bits(w.W)))
    val sel = Input(Bits(numSelectBits.W))
    val out = Output(Bits(w.W))
  })

  // Register the inputs
  val ffins = List.tabulate(numInputs) { i =>
    val ff = Module(new FringeFF(UInt(w.W)))
    ff.io.enable := true.B
    ff.io.in := io.ins(i)
    ff
  }

  val ffsel = Module(new FringeFF(UInt(numSelectBits.W)))
  ffsel.io.enable := true.B
  ffsel.io.in := io.sel
  val sel = ffsel.io.out

  val mux = Module(new MuxN(UInt(w.W), numInputs))
  mux.io.ins := Vec.tabulate(numInputs) { i => ffins(i).io.out }
  mux.io.sel := sel

  // Register the output
  val ff = Module(new FringeFF(UInt(w.W)))
  ff.io.enable := true.B
  ff.io.in := mux.io.out
  io.out := ff.io.out
}

