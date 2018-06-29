package fringe

import chisel3._
import chisel3.util._
import templates._

class MuxN[T<:Data](val t: T, val numInputs: Int) extends Module {
  val numSelectBits = log2Ceil(numInputs)
  val io = IO(new Bundle {
    val ins = Input(Vec(numInputs, t.cloneType))
    val sel = Input(Bits(numSelectBits.W))
    val out = Output(t.cloneType)
  })

  io.out := io.ins(io.sel)
}

class MuxNPipe[T<:Data](val t: T, val numInputs: Int, val stages: Int) extends Module {
  val logInputs = log2Ceil(numInputs)
  val pow2Inputs = scala.math.pow(2, logInputs).toInt
  assert(stages >= 0 && stages <= logInputs)

  val io = IO(new Bundle {
    val ins = Input(Vec(numInputs, t.cloneType))
    val sel = Input(Bits(logInputs.W))
    val en = Input(Bool())
    val out = Output(t.cloneType)
  })

  def splitMux(input: Vec[T], stages: Int): (T, Bits) = {
    def slice(v: Vec[T], start: Int, end: Int) = {
      Vec(for (i <- start to end) yield v(i))
    }

    stages match {
      case 0 => {
        val m = Module(new MuxN(t, input.length))
        m.io.ins := input
        val c = log2Ceil(input.length)
        m.io.sel := io.sel(c - 1, 0)
        val s = if (io.sel.getWidth > c) io.sel(io.sel.getWidth - 1, c) else 0.U
        (m.io.out, s)
      }
      case _ => {
        val inL = slice(input, 0, (input.length / 2) - 1)
        val inH = slice(input, input.length / 2, input.length - 1)
        val (muxL, selL) = splitMux(inL, stages - 1)
        val (muxH, selH) = splitMux(inH, stages - 1)

        val selFF = Utils.getFF(selL, io.en)

        val m = Module(new MuxN(t, 2));
        m.io.ins := Vec(Utils.getFF(muxL, io.en), Utils.getFF(muxH, io.en))
        m.io.sel := selFF(0)
        val s = if (selFF.getWidth > 1) selFF(selFF.getWidth - 1, 1) else 0.U
        (m.io.out, s)
      }
    }
  }

  def pad(v: Vec[T]) = {
    val c = scala.math.pow(2, log2Ceil(v.length)).toInt
    Vec(for (i <- 0 until c) yield if (i < v.length) v(i) else v(0))
  }

  val (m, s) = splitMux(pad(io.ins), stages)
  io.out := m
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

