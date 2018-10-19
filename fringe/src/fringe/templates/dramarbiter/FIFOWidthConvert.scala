package fringe.templates.dramarbiter

import chisel3._
import chisel3.util._

// TODO: this is a mess and could be simplified
class FIFOWidthConvert(val win: Int, val vin: Int, val wout: Int, val vout: Int, val d: Int) extends Module {
  class DataStrobe(w: Int, v: Int, s: Int) extends Bundle {
    val data = Vec(v, Bits(w.W))
    val strobe = UInt(s.W)

    override def cloneType: this.type = new DataStrobe(w, v, s).asInstanceOf[this.type]
  }

  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new DataStrobe(win, vin, vin)))
    val out = Decoupled(new DataStrobe(wout, vout, wout*vout/8))
  })

  def convertVec(inVec: Vec[UInt], outw: Int, outv: Int): Vec[UInt] = {
    // 1. Cat everything
    val unifiedUInt = inVec.reverse.reduce{ Cat(_,_) }

    // 2. Split apart
    val out = Vec(List.tabulate(outv){ i =>
      unifiedUInt(i*outw+outw-1, i*outw)
    })

    out
  }

  def bytify(inVec: UInt, outbytes: Int, win: Int): UInt = {
    assert(win / 8 > 0)
    val boolVec = Vec(List.tabulate(outbytes){i => 
      val id = (i / (win/8)).toInt
      inVec(id)
    }.reverse)

    boolVec.reduce[UInt](Cat(_,_))
  }

  /**
   * Queue is full if no new element of 'inWidth' can be enqueued
   * Queue is empty if no element of 'outWidth' can be dequeued
   */
  val inWidth = win * vin
  val outWidth = wout * vout

  if ((inWidth < outWidth) || (inWidth == outWidth && wout < win)) {
    Predef.assert(outWidth % inWidth == 0, s"ERROR: Width conversion attempted between widths that are not multiples (in: $inWidth, out: $outWidth)")
    val v = outWidth / inWidth
    val fifo = Module(new FIFOVec(UInt(inWidth.W), d, v))
    val fifoStrb = Module(new FIFOVec(UInt(vin.W), d, v))
    fifo.io.chainEnq := true.B
    fifo.io.chainDeq := false.B
    fifoStrb.io.chainEnq := true.B
    fifoStrb.io.chainDeq := false.B
    fifo.io.in.bits(0) := io.in.bits.data.reverse.reduce { Cat(_,_) }
    fifo.io.in.valid := io.in.valid
    fifoStrb.io.in.bits(0) := io.in.bits.strobe
    fifoStrb.io.in.valid := io.in.valid
    io.in.ready := fifo.io.in.ready
    io.out.valid := fifo.io.out.valid
    io.out.bits.data := convertVec(fifo.io.out.bits, wout, vout)
    io.out.bits.strobe := bytify(fifoStrb.io.out.bits.reduce(Cat(_,_)), wout*vout/8, win)
    fifo.io.out.ready := io.out.ready
    fifoStrb.io.out.ready := io.out.ready
  }
  else if ((inWidth > outWidth) || (inWidth == outWidth && wout > win)) {
    Predef.assert(inWidth % outWidth == 0, s"ERROR: Width conversion attempted between widths that are not multiples (in: $inWidth, out: $outWidth)")
    val v = inWidth / outWidth
    val fifo = Module(new FIFOVec(UInt(outWidth.W), d, v))
    val fifoStrb = Module(new FIFOVec(UInt(vin.W), d, 1))
    fifo.io.chainEnq := false.B
    fifo.io.chainDeq := true.B
    fifoStrb.io.chainEnq := false.B
    fifoStrb.io.chainDeq := true.B
    io.in.ready := fifo.io.in.ready
    io.out.valid := fifo.io.out.valid

    fifo.io.in.bits := convertVec(io.in.bits.data, outWidth, v)
    fifo.io.in.valid := io.in.valid
    fifoStrb.io.in.bits(0) := io.in.bits.strobe
    fifoStrb.io.in.valid := io.in.valid

    io.out.bits.data := convertVec(Vec(fifo.io.out.bits(0)), wout, vout)
    io.out.bits.strobe := bytify(fifoStrb.io.out.bits(0), wout*vout/8, win)
    fifo.io.out.ready := io.out.ready
  } else {
    val fifo = Module(new FIFOVec(UInt(win.W), d, vin))
    val fifoStrb = Module(new FIFOVec(UInt(vin.W), d, 1))
    fifo.io.chainEnq := false.B
    fifo.io.chainDeq := false.B
    fifoStrb.io.chainEnq := false.B
    fifoStrb.io.chainDeq := false.B
    io.in.ready := fifo.io.in.ready
    io.out.valid := fifo.io.out.valid

    fifo.io.in.bits := io.in.bits.data
    fifo.io.in.valid := io.in.valid
    fifoStrb.io.in.bits(0) := io.in.bits.strobe
    fifoStrb.io.in.valid := io.in.valid

    io.out.bits.data := fifo.io.out.bits
    fifo.io.out.ready := io.out.ready
    io.out.bits.strobe := bytify(fifoStrb.io.out.bits.reduce[UInt](Cat(_,_)), wout*vout/8, win)
    fifoStrb.io.out.ready := io.out.ready
  }
}
