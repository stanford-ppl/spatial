package fringe

import chisel3._
import chisel3.util._
import templates.Utils.log2Up


/**
 * FIFO config register format
 */
case class FIFOOpcode(val d: Int, val v: Int) extends Bundle {
  def roundUpDivide(num: Int, divisor: Int) = (num + divisor - 1) / divisor

  var chainWrite = Bool()
  var chainRead = Bool()

  override def cloneType(): this.type = {
    new FIFOOpcode(d, v).asInstanceOf[this.type]
  }
}

class FIFOBaseIO[T <: Data](t: T, d: Int, v: Int, banked: Boolean = false) extends Bundle {
  class BankInterface extends Bundle {
    val wen = Input(Bool())
    val rdata = Output(t)
    val wdata = Input(t)
    val valid = Output(Bool())

    override def cloneType(): this.type = {
      new BankInterface().asInstanceOf[this.type]
    }
  }

  val enq = Input(Vec(v, t))
  val enqVld = Input(Bool())
  val deq = Output(Vec(v, t))
  val deqVld = Input(Bool())
  val full = Output(Bool())
  val empty = Output(Bool())
  val almostFull = Output(Bool())
  val almostEmpty = Output(Bool())
  val config = Input(new FIFOOpcode(d, v))
  val fifoSize = Output(UInt(32.W))
  val banks = if (banked) Some(Vec(d/v, Vec(v, new BankInterface))) else None

  override def cloneType(): this.type = {
    new FIFOBaseIO(t, d, v, banked).asInstanceOf[this.type]
  }
}

abstract class FIFOBase[T <: Data](val t: T, val d: Int, val v: Int, val banked: Boolean = false) extends Module {
  val w = t.getWidth
  val addrWidth = log2Up(d/v)
  val bankSize = d/v
  val bankCount = if (banked) bankSize else 1
  val depth = if (banked) 1 else bankSize

  val io = IO(new FIFOBaseIO(t, d, v, banked))

  // Check for sizes and v
  Predef.assert(d%v == 0, s"Unsupported FIFO size ($d)/banking($v) combination; $d must be a multiple of $v")
  Predef.assert(isPow2(v), s"Unsupported banking number $v; must be a power-of-2")
  Predef.assert(isPow2(d), s"Unsupported FIFO size $d; must be a power-of-2")

  // Create size register
  val sizeUDC = Module(new UpDownCtr(log2Up(d+1)))
  val size = sizeUDC.io.out
  io.fifoSize := size
  val remainingSlots = d.U - size
  val nextRemainingSlots = d.U - sizeUDC.io.nextInc

  val strideInc = Mux(io.config.chainWrite, 1.U, v.U)
  val strideDec = Mux(io.config.chainRead, 1.U, v.U)

  // FIFO is full if #rem elements (d - size) < strideInc
  // FIFO is emty if elements (size) < strideDec
  // FIFO is almostFull if the next enqVld will make it full
  val empty = size < strideDec
  val almostEmpty = sizeUDC.io.nextDec < strideDec
  val full = remainingSlots < strideInc
  val almostFull = nextRemainingSlots < strideInc

  sizeUDC.io.initval := 0.U
  sizeUDC.io.max := d.U
  sizeUDC.io.init := 0.U
  sizeUDC.io.strideInc := strideInc
  sizeUDC.io.strideDec := strideDec
  sizeUDC.io.init := 0.U

  val writeEn = io.enqVld & ~full
  val readEn = io.deqVld & ~empty
  sizeUDC.io.inc := writeEn
  sizeUDC.io.dec := readEn

  io.empty := empty
  io.almostEmpty := almostEmpty
  io.full := full
  io.almostFull := almostFull
}

class FIFOCounter(override val d: Int, override val v: Int) extends FIFOBase(UInt(1.W), d, v) {
  io.deq.foreach { d => d := ~empty }
}

class FIFOCore[T<:Data](override val t: T, override val d: Int, override val v: Int, override val banked: Boolean=false) extends FIFOBase(t, d, v, banked) {

  // Create wptr (tail) counter chain
  val wptrConfig = Wire(new CounterChainOpcode(log2Up(scala.math.max(bankSize,v)+1), 2, 0, 0))
  wptrConfig.chain(0) := io.config.chainWrite
  (0 until 2) foreach { i => i match {
      case 1 => // Localaddr: max = bankSize, stride = 1
        val cfg = wptrConfig.counterOpcode(i)
        cfg.max := bankSize.U
        cfg.stride := 1.U
        cfg.maxConst := true.B
        cfg.strideConst := true.B
      case 0 => // Bankaddr: max = v, stride = 1
        val cfg = wptrConfig.counterOpcode(i)
        cfg.max := v.U
        cfg.stride := 1.U
        cfg.maxConst := true.B
        cfg.strideConst := true.B
  }}
  val wptr = Module(new CounterChainCore(log2Up(scala.math.max(bankSize,v)+1), 2, 0, 0))
  wptr.io.enable(0) := writeEn & io.config.chainWrite
  wptr.io.enable(1) := writeEn
  wptr.io.config := wptrConfig
  val tailLocalAddr = wptr.io.out(1)
  val tailBankAddr = wptr.io.out(0)


  // Create rptr (head) counter chain
  val rptrConfig = Wire(new CounterChainOpcode(log2Up(scala.math.max(bankSize,v)+1), 2, 0, 0))
  rptrConfig.chain(0) := io.config.chainRead
  (0 until 2) foreach { i => i match {
      case 1 => // Localaddr: max = bankSize, stride = 1
        val cfg = rptrConfig.counterOpcode(i)
        cfg.max := bankSize.U
        cfg.stride := 1.U
        cfg.maxConst := true.B
        cfg.strideConst := true.B
      case 0 => // Bankaddr: max = v, stride = 1
        val cfg = rptrConfig.counterOpcode(i)
        cfg.max := v.U
        cfg.stride := 1.U
        cfg.maxConst := true.B
        cfg.strideConst := true.B
    }}
  val rptr = Module(new CounterChainCore(log2Up(scala.math.max(bankSize,v)+1), 2, 0, 0))
  rptr.io.enable(0) := readEn & io.config.chainRead
  rptr.io.enable(1) := readEn
  rptr.io.config := rptrConfig
  val headLocalAddr = rptr.io.out(1)
  val nextHeadLocalAddr = Mux(io.config.chainRead, Mux(rptr.io.done(0), rptr.io.next(1), rptr.io.out(1)), rptr.io.next(1))
  val headBankAddr = rptr.io.out(0)
  val nextHeadBankAddr = rptr.io.next(0)

  // Backing SRAM
  val mems = List.fill(bankCount) {
    List.fill(v) {
      if (w == 1 || banked || depth == 1) Module(new FFRAM(t, depth)) else {
        val sram = Module(new SRAM(t, depth))
        sram.io.flow := true.B
        sram
      }
    }
  }

  val rdata = if (banked) {
    val m = Module(new MuxN(Vec(v, t), bankCount))
    m.io.ins := Vec(mems.map { case banks => Vec(banks.map { _.io.rdata }) })
    m.io.sel := headLocalAddr
    m.io.out
  } else {
    mems(0).map { _.io.rdata }
  }

  val wdata = Vec(List.tabulate(v) { i => if (i == 0) io.enq(i) else Mux(io.config.chainWrite, io.enq(0), io.enq(i)) })

  mems.zipWithIndex.foreach { case (m, i) =>
    m.zipWithIndex.foreach { case (bank, j) =>
      io.banks match {
        case Some(b: Vec[_]) =>
          bank.io.raddr := 0.U
          bank.io.waddr := 0.U

          val enqWen = Mux(io.config.chainWrite, writeEn & tailBankAddr === j.U, writeEn) & tailLocalAddr === i.U
          val deqWen = Mux(io.config.chainRead, readEn & headBankAddr === j.U, readEn) & headLocalAddr === i.U
          val ff = Module(new FF(Bool()))
          ff.io.init := false.B
          ff.io.enable := enqWen | deqWen
          ff.io.in := enqWen
          b(i)(j).valid := ff.io.out

          bank.io.wdata := Mux(b(i)(j).wen, b(i)(j).wdata, wdata(j))
          bank.io.wen := Mux(enqWen, true.B, b(i)(j).wen)
          b(i)(j).rdata := bank.io.rdata
        case None =>
          bank.io.raddr := Mux(readEn, nextHeadLocalAddr, headLocalAddr)
          bank.io.waddr := tailLocalAddr

          bank.io.wdata := wdata(j)
          bank.io.wen := Mux(io.config.chainWrite, writeEn & tailBankAddr === j.U, writeEn)
      }

      i match {
        case 0 =>
          // Read data output
          val deqData = j match {
            case 0 =>
              val rdata0Mux = Module(new MuxN(t, v))
              val addrFF = Module(new FF(UInt(log2Ceil(v).W)))
              addrFF.io.in := Mux(readEn, nextHeadBankAddr, headBankAddr)
              addrFF.io.enable := true.B

              rdata0Mux.io.ins := rdata
              rdata0Mux.io.sel := Mux(io.config.chainRead, addrFF.io.out, 0.U)
              rdata0Mux.io.out
            case _ =>
              rdata(j)
          }
          io.deq(j) := deqData
        case _ =>
      }
    }
  }
}

