package fringe.templates.memory

import chisel3._
import chisel3.util.Cat

class FIFOWidthConvertIO(val win: Int, val vin: Int, val wout: Int, val vout: Int) extends Bundle {
  val enq = Input(Vec(vin, Bits(win.W)))
  val enqStrb = Input(UInt(vin.W))
  val enqVld = Input(Bool())
  val deq = Output(Vec(vout, Bits(wout.W)))
  val deqStrb = Output(UInt((wout*vout/8).W))
  val deqVld = Input(Bool())
  val full = Output(Bool())
  val empty = Output(Bool())
  val almostEmpty = Output(Bool())
  val almostFull = Output(Bool())
  val fifoSize = Output(UInt(32.W))

  override def cloneType(): this.type = {
    new FIFOWidthConvertIO(win, vin, wout, vout).asInstanceOf[this.type]
  }
}

/**
 * WidthConverterFIFO: Convert a stream of width w1 bits to
 * a stream of width w2 bits
 * @param win: Input word width (bits)
 * @param vin: Input vector length
 * @param wout: Output word width
 * @param vout: Out vector length
 * @param d: FIFO depth
 */
class FIFOWidthConvert(val win: Int, val vin: Int, val wout: Int, val vout: Int, val d: Int) extends Module {
  val io = IO(new FIFOWidthConvertIO(win, vin, wout, vout))

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
    val fifo = Module(new FIFOCore(UInt(inWidth.W), d, v))
    val fifoStrb = Module(new FIFOCore(UInt(vin.W), d, v))
    val fifoConfig = Wire(new FIFOOpcode(d, v))
    fifoConfig.chainWrite := 1.U
    fifoConfig.chainRead := 0.U
    fifo.io.config := fifoConfig
    fifo.io.enq(0) := io.enq.reverse.reduce { Cat(_,_) }
    fifo.io.enqVld := io.enqVld
    fifoStrb.io.config := fifoConfig
    fifoStrb.io.enq(0) := io.enqStrb
    fifoStrb.io.enqVld := io.enqVld
    io.full := fifo.io.full
    io.empty := fifo.io.empty
    io.almostEmpty := fifo.io.almostEmpty
    io.almostFull := fifo.io.almostFull
    io.deq := convertVec(fifo.io.deq, wout, vout)
    io.deqStrb := bytify(fifoStrb.io.deq.reduce(Cat(_,_)), wout*vout/8, win)
    io.fifoSize := fifo.io.fifoSize
    fifo.io.deqVld := io.deqVld
    fifoStrb.io.deqVld := io.deqVld
  }
  else if ((inWidth > outWidth) || (inWidth == outWidth && wout > win)) {
    Predef.assert(inWidth % outWidth == 0, s"ERROR: Width conversion attempted between widths that are not multiples (in: $inWidth, out: $outWidth)")
    val v = inWidth / outWidth
    val fifo = Module(new FIFOCore(UInt(outWidth.W), d, v))
    val fifoStrb = Module(new FIFOCore(UInt(vin.W), d, 1))
    val fifoConfig = Wire(new FIFOOpcode(d, v))
    fifoConfig.chainWrite := 0.U
    fifoConfig.chainRead := 1.U
    fifo.io.config := fifoConfig
    fifoStrb.io.config := fifoConfig
    io.full := fifo.io.full
    io.empty := fifo.io.empty
    io.almostEmpty := fifo.io.almostEmpty
    io.almostFull := fifo.io.almostFull
    io.fifoSize := fifo.io.fifoSize

    fifo.io.enq := convertVec(io.enq, outWidth, v)
    fifo.io.enqVld := io.enqVld
    fifoStrb.io.enq(0) := io.enqStrb
    fifoStrb.io.enqVld := io.enqVld


    io.deq := convertVec(Vec(fifo.io.deq(0)), wout, vout)
    io.deqStrb := bytify(fifoStrb.io.deq(0), wout*vout/8, win)
    fifo.io.deqVld := io.deqVld
  } else {
    val fifo = Module(new FIFOCore(UInt(win.W), d, vin))
    val fifoStrb = Module(new FIFOCore(UInt(vin.W), d, 1))
    val fifoConfig = Wire(new FIFOOpcode(d, vin))
    fifoConfig.chainWrite := 0.U
    fifoConfig.chainRead := 0.U
    fifo.io.config := fifoConfig
    fifoStrb.io.config := fifoConfig
    io.full := fifo.io.full
    io.empty := fifo.io.empty
    io.almostEmpty := fifo.io.almostEmpty
    io.almostFull := fifo.io.almostFull
    io.fifoSize := fifo.io.fifoSize

    fifo.io.enq := io.enq
    fifo.io.enqVld := io.enqVld
    fifoStrb.io.enq(0) := io.enqStrb
    fifoStrb.io.enqVld := io.enqVld

    io.deq := fifo.io.deq
    fifo.io.deqVld := io.deqVld
    io.deqStrb := bytify(fifoStrb.io.deq.reduce[UInt](Cat(_,_)), wout*vout/8, win)
    fifoStrb.io.deqVld := io.deqVld
  }
}
