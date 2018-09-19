package templates

import chisel3._
import chisel3.util._
import fringe._

import Utils._

class UpDownCounter(val w: Int) extends Module {
  val io = IO(new Bundle {
    val reset = Input(Bool())
    val init = Input(UInt(w.W))
    val incDec = Input(Bool())
    val saturate = Input(Bool())
    val stride = Input(UInt((w + 1).W))
    val en = Input(Bool())
    val out = Output(UInt(w.W))
    val done = Output(Bool())
  })

  val en = io.en | io.reset

  val counter = Module(new fringe.FringeFF(UInt(w.W)))
  counter.io.enable := en

  val count = Mux(io.incDec, counter.io.out + io.stride, counter.io.out - io.stride)(w - 1,0)
  val overflow = Mux(io.incDec, count < counter.io.out, count > counter.io.out) | io.stride(w)
  val newCount = Mux(overflow & io.saturate, counter.io.out, count)
  counter.io.in := Mux(io.reset, io.init, newCount)
  io.out := counter.io.out

  io.done := overflow & en
}

class BarrelShifter[T<:Data](val t: T, val v: Int) extends Module {
  assert(isPow2(v))
  val shiftW = log2Ceil(v + 1)

  val io = IO(new Bundle {
    val in = Input(Vec(v, t.cloneType))
    val shiftIn = Input(Vec(v, t.cloneType))
    val shiftSel = Input(Bits(shiftW.W))
    val out = Output(Vec(v, t.cloneType))
  })

  def barrelShifter(in: Vec[T], shiftSel: Bits, shift: Int): Vec[T] = {
    val out = Vec(List.tabulate(in.length) { i =>
      val shiftIndex = i + shift
      val a = if (shiftIndex >= in.length) in(in.length - 1) else in(shiftIndex)
      val s = shiftSel(0)
      Mux(s, a, in(i))
    })
    if (log2Ceil(shift + 1) == shiftW) out else barrelShifter(out, shiftSel(shiftSel.getWidth - 1, 1), shift * 2)
  }

  val out = barrelShifter(Utils.vecJoin(io.in, io.shiftIn), io.shiftSel, 1)
  io.out := Utils.vecSlice(out, 0, v - 1)
}

class FIFOPeek[T<:Data](val t: T) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(t.cloneType))
    val peek = Valid(t.cloneType)
    val out = Decoupled(t.cloneType)
  })

  val fifo = Module(new fringe.FIFOCore(t.cloneType, 128, 1))

  val ff = Module(new fringe.FringeFF(Valid(t.cloneType)))

  val deq = ~ff.io.out.valid | io.out.ready
  ff.io.enable := deq
  ff.io.in.valid := ~fifo.io.empty
  ff.io.in.bits := fifo.io.deq(0)

  io.in.ready := ~fifo.io.full

  fifo.io.enqVld := io.in.valid
  fifo.io.deqVld := deq
  fifo.io.enq(0) := io.in.bits

  io.peek.valid := ~fifo.io.empty
  io.peek.bits := fifo.io.deq(0)

  io.out.valid := ff.io.out.valid
  io.out.bits := ff.io.out.bits
}

class SortPipe(val w: Int, val v: Int) extends Module {
  assert(isPow2(v))

  val io = IO(new Bundle {
    val in = Flipped(Decoupled(Vec(v, UInt(w.W))))
    val out = Decoupled(Vec(v, UInt(w.W)))
  })

  def sortNetworkMap(width: Int) = {
    def bitonicSort(in: List[Int]): List[List[Tuple2[Int, Int]]] = {
      if (in.length == 2) List(List((in(0), in(1)))) else {
        val (l, h) = in.splitAt(in.length / 2)
        val s = bitonicSort(l).zip(bitonicSort(h)).map { case (l, h) => l ::: h }
        l.zip(h) :: s
      }
    }

    def bitonicMerge(in: List[Int]): List[List[Tuple2[Int, Int]]] = {
      if (in.length == 2) bitonicSort(in) else {
        val (l, h) = in.splitAt(in.length / 2)
        val s = bitonicSort(l).zip(bitonicSort(h)).map { case (l, h) => l ::: h }
        l.zip(h.reverse) :: s
      }
    }

    def sort(in: List[Int]): List[List[Tuple2[Int, Int]]] = {
      if (in.length == 2) bitonicMerge(in) else {
        val (l, h) = in.splitAt(in.length / 2)
        val s = sort(l).zip(sort(h)).map { case (l, h) => l ::: h }
        s ::: bitonicMerge(in)
      }
    }

    sort(List.range(0, width))
  }

  val sortMap = sortNetworkMap(v)

  val en = io.out.ready | ~io.out.valid

  val stages = List.fill(sortMap.length) { Wire(Vec(v, UInt(w.W))) }
  sortMap.zipWithIndex.foreach { case (s, i) =>
    val stageIn = if (i == 0) io.in.bits else stages(i - 1)
    val stageOut = Wire(Vec(v, UInt(w.W)))
    s.foreach { case (a, b) =>
      val inA = stageIn(a)
      val inB = stageIn(b)
      stageOut(a) := Mux(inA < inB, inA, inB)
      stageOut(b) := Mux(inA < inB, inB, inA)
    }
    stages(i) := Utils.getRetimed(stageOut, 1, en)
  }

  io.out.valid := Utils.getRetimed(io.in.valid, stages.length, en)
  io.in.ready := en
  io.out.bits := stages.last
}

class MergeBufferIO(ways: Int, w: Int, v: Int) extends Bundle {
  val in = Vec(ways, Flipped(Decoupled(Vec(v, UInt(w.W)))))
  val initMerge = Flipped(Valid(Bool()))
  val inBound = Vec(ways, Flipped(Valid(UInt(w.W))))
  val out = Decoupled(Vec(v, UInt(w.W)))
  val outBound = Valid(UInt(w.W))
}

class MergeBufferTwoWay(w: Int, v: Int) extends Module {

  val io = IO(new MergeBufferIO(2, w, v))

  val sortPipe = Module(new SortPipe(w, v))
  sortPipe.io.out.ready := io.out.ready

  val countEn = sortPipe.io.in.valid & sortPipe.io.in.ready

  val counterW = log2Ceil(v)
  val headCounter = List.fill(2) { Module(new UpDownCounter(counterW)) }
  headCounter.zipWithIndex.foreach { case (hc, i) =>
    hc.io.init := 0.U
    hc.io.incDec := true.B
    hc.io.en := countEn
  }

  val initMerge = Module(new fringe.FringeFF(Bool()))
  initMerge.io.enable := io.initMerge.valid
  initMerge.io.in := io.initMerge.bits

  val buffers = List.fill(2) { Module(new FIFOPeek(Vec(v, UInt(w.W)))) }
  buffers.zipWithIndex.foreach { case (b, i) =>
    b.io.in.valid := io.in(i).valid
    b.io.in.bits := io.in(i).bits

    io.in(i).ready := b.io.in.ready
    b.io.in.valid := io.in(i).valid
    b.io.out.ready := headCounter(i).io.done
  }

  val streamCounter = List.fill(2) { Module(new UpDownCounter(w)) }
  streamCounter.zipWithIndex.foreach { case (sc, i) =>
    sc.io.init := io.inBound(i).bits
    sc.io.incDec := false.B
    sc.io.saturate := true.B
    sc.io.reset := io.inBound(i).valid
    sc.io.en := countEn
  }

  val shifters = List.fill(2) { Module(new BarrelShifter(Valid(UInt(w.W)), v)) }
  shifters.zipWithIndex.foreach { case (s, i) =>
    s.io.in.zipWithIndex.foreach { case (v, j) =>
      v.valid := buffers(i).io.out.valid
      v.bits := buffers(i).io.out.bits(j)
    }
    s.io.shiftIn.zipWithIndex.foreach { case (v, j) =>
      v.valid := buffers(i).io.peek.valid
      v.bits := buffers(i).io.peek.bits(j)
    }
    s.io.shiftSel := headCounter(i).io.out
  }

  val (rBits, lBits, outBits) = List.tabulate(v) { i =>
    val r = shifters(0).io.out(i)
    val l = shifters(1).io.out(v - i - 1)
    val compare = r.bits < l.bits | initMerge.io.out
    val rBit = Mux(r.valid & l.valid, compare, r.valid)
    val lBit = Mux(r.valid & l.valid, ~compare, l.valid)
    val out = Mux(r.valid & l.valid, Mux(compare, r.bits, l.bits), Mux(r.valid, r.bits, l.bits))
    (rBit, lBit, out)
  }.unzip3

  val rValid = Vec(rBits).asUInt
  val lValid = Vec(lBits).asUInt
  headCounter(0).io.stride := PopCount(rValid)
  headCounter(1).io.stride := PopCount(lValid)
  streamCounter(0).io.stride := PopCount(rValid)
  streamCounter(1).io.stride := PopCount(lValid)

  val oneSided = ((rValid | lValid).andR & streamCounter.map { _.io.out < v.U }.reduce { _|_ })
  sortPipe.io.in.valid := oneSided | shifters.map { s => s.io.out.map { _.valid }.reduce { _&_ } }.reduce { _&_ }

  sortPipe.io.in.bits := Vec(outBits)

  io.outBound.valid := io.inBound.map { _.valid }.reduce{ _&_ }
  io.outBound.bits := io.inBound.map { _.bits }.reduce{ _+_ }

  io.out.valid := sortPipe.io.out.valid
  io.out.bits := sortPipe.io.out.bits
}

class MergeBufferNWay(ways: Int, w: Int, v: Int) extends Module {
  assert(isPow2(ways))

  val io = IO(new MergeBufferIO(ways, w, v))

  ways match {
    case 2 => {
      val m = Module(new MergeBufferTwoWay(w, v))
      m.io.in <> io.in
      m.io.initMerge := io.initMerge
      m.io.inBound := io.inBound
      io.out <> m.io.out
      io.outBound <> m.io.outBound
    }
    case _ => {
      val twoWay = Module(new MergeBufferTwoWay(w, v))
      val mergeBuffers = List.tabulate(2) { i =>
        val n = ways / 2
        val nWay = Module(new MergeBufferNWay(n, w, v))
        nWay.io.in.zipWithIndex.foreach { case (in, j) =>
          in <> io.in(i * n + j)
        }

        nWay.io.initMerge := io.initMerge

        nWay.io.inBound.zipWithIndex.foreach { case (inBound, j) =>
          inBound := io.inBound(i * n + j)
        }
        twoWay.io.in(i) <> nWay.io.out
        twoWay.io.inBound(i) := nWay.io.outBound
      }
      io.out <> twoWay.io.out
      io.outBound := twoWay.io.outBound
    }
  }
}

class MergeBuffer(ways: Int, par: Int, bitWidth: Int, readers: Int) extends Module {
  val io = IO(new Bundle {
    val in_wen = Input(Vec(ways, Vec(par, Bool())))
    val in_data = Input(Vec(ways, Vec(par, UInt(bitWidth.W))))
    val initMerge_wen = Input(Bool())
    val initMerge_data = Input(Bool())
    val inBound_wen = Input(Vec(ways, UInt(bitWidth.W)))
    val inBound_data = Input(Vec(ways, UInt(bitWidth.W)))
    val out_ren = Input(Vec(readers, Vec(par, Bool())))
    val out_data = Output(Vec(par, UInt(bitWidth.W)))

    val empty = Output(Bool())
    val full = Output(Vec(ways, Bool()))
  })

  val mergeBuf = Module(new MergeBufferNWay(ways, bitWidth, par))

  mergeBuf.io.out.ready := io.out_ren.asUInt.orR

  mergeBuf.io.in.zipWithIndex.foreach { case (in, i) =>
    in.valid := io.in_wen(i).reduce{_|_}
    in.bits := io.in_data(i)
  }

  mergeBuf.io.inBound.zipWithIndex.foreach { case (bound, i) =>
    bound.valid := io.inBound_wen(i)
    bound.bits := io.inBound_data(i)
  }

  mergeBuf.io.initMerge.valid := io.initMerge_wen
  mergeBuf.io.initMerge.bits := io.initMerge_data

  io.out_data := mergeBuf.io.out.bits

  io.empty := ~mergeBuf.io.out.valid
  io.full.zipWithIndex.foreach { case (f, i) =>
    f := ~mergeBuf.io.in(i).ready
  }
}

