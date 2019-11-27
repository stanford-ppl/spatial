package fringe.templates.memory

import chisel3._
import chisel3.core.{IntParam, Param}
import chisel3.util._
import fringe.globals
import fringe.targets.arria10.Arria10
import fringe.targets.aws.AWS_F1
import fringe.targets.kcu1500.KCU1500
import fringe.targets.zcu.ZCU
import fringe.targets.zynq.Zynq

class SRAMVerilogIO[T<:Data](t: T, d: Int) extends Bundle {
    val clk = Input(Clock())
    val raddr = Input(UInt({1 max log2Ceil(d)}.W))
    val waddr = Input(UInt({1 max log2Ceil(d)}.W))
    val raddrEn = Input(Bool())
    val waddrEn = Input(Bool())
    val wen = Input(Bool())
    val backpressure = Input(Bool())
    val wdata = Input(UInt(t.getWidth.W))
    val rdata = Output(UInt(t.getWidth.W))

    override def cloneType = (new SRAMVerilogIO(t, d)).asInstanceOf[this.type] // See chisel3 bug 358

}

abstract class SRAMBlackBox[T<:Data](params: Map[String,Param]) extends BlackBox(params) {
  val io: SRAMVerilogIO[T]
}



class SRAMVerilogSim[T<:Data](val t: T, val d: Int) extends BlackBox(
  Map("DWIDTH" -> IntParam(t.getWidth), "WORDS" -> IntParam(d), "AWIDTH" -> IntParam({1 max log2Ceil(d)})))
{
  override val io = IO(new SRAMVerilogIO(t, d))
}

class SRAMVerilogAWS[T<:Data](val t: T, val d: Int) extends SRAMBlackBox[T](
  Map("DWIDTH" -> IntParam(t.getWidth), "WORDS" -> IntParam(d), "AWIDTH" -> IntParam({1 max log2Ceil(d)})))
{
  override val io = IO(new SRAMVerilogIO(t, d))
}

class SRAMVerilogAWS_URAM[T<:Data](val t: T, val d: Int) extends SRAMBlackBox[T](
  Map("DWIDTH" -> IntParam(t.getWidth), "WORDS" -> IntParam(d), "AWIDTH" -> IntParam({1 max log2Ceil(d)})))
{
  override val io = IO(new SRAMVerilogIO(t, d))
}

class SRAMVerilogAWS_BRAM[T<:Data](val t: T, val d: Int) extends SRAMBlackBox[T](
  Map("DWIDTH" -> IntParam(t.getWidth), "WORDS" -> IntParam(d), "AWIDTH" -> IntParam({1 max log2Ceil(d)})))
{
  override val io = IO(new SRAMVerilogIO(t, d))
}

class SRAMVerilogDE1SoC[T<:Data](val t: T, val d: Int) extends SRAMBlackBox[T](
  Map("DWIDTH" -> IntParam(t.getWidth), "WORDS" -> IntParam(d), "AWIDTH" -> IntParam({1 max log2Ceil(d)})))
{
  override val io = IO(new SRAMVerilogIO(t, d))
}

class GenericRAMIO[T<:Data](t: T, d: Int) extends Bundle {
  val addrWidth = {1 max log2Ceil(d)}
  val raddr = Input(UInt(addrWidth.W))
  val wen = Input(Bool())
  val waddr = Input(UInt(addrWidth.W))
  val wdata = Input(t.cloneType)
  val rdata = Output(t.cloneType)
  val backpressure = Input(Bool())

  override def cloneType: this.type = {
    new GenericRAMIO(t, d).asInstanceOf[this.type]
  }
}

abstract class GenericRAM[T<:Data](val t: T, val d: Int) extends Module {
  val addrWidth = {1 max log2Ceil(d)}
  val io: GenericRAMIO[T]
}

class FFRAM[T<:Data](override val t: T, override val d: Int) extends GenericRAM(t, d) {
  class FFRAMIO[T<:Data](t: T, d: Int) extends GenericRAMIO(t, d) {
    class Bank[T<:Data](t: T, d: Int) extends Bundle {
      val wdata = Flipped(Valid(t.cloneType))
      val rdata = Output(t.cloneType)

      override def cloneType: this.type = new Bank(t, d).asInstanceOf[this.type]
    }

    val banks = Vec(d, new Bank(t.cloneType, d))

    override def cloneType: this.type = new FFRAMIO(t, d).asInstanceOf[this.type]
  }

  val io = IO(new FFRAMIO(t, d))

  val regs = List.tabulate(d) { i =>
    val r = RegInit((0.U).asTypeOf(t))
    val bank = io.banks(i)
    val wen = bank.wdata.valid
    when (wen | (io.wen & (io.waddr === i.U))) {
      r := Mux(wen, bank.wdata.bits, io.wdata)
    }
    bank.rdata := r
    r
  }

  io.rdata := VecInit(regs)(io.raddr)
}

class SRAM[T<:Data](override val t: T, override val d: Int, val resourceType: String) extends GenericRAM(t, d) {
  val io = IO(new GenericRAMIO(t, d))

  // Customize SRAM here
  // TODO: Still needs some cleanup
  globals.target match {
    case _:AWS_F1 | _:Zynq | _:ZCU | _:Arria10 | _:KCU1500 =>

      val mem = resourceType match {
        case "URAM" => Module(new SRAMVerilogAWS_URAM(t, d))
        case "BRAM" => Module(new SRAMVerilogAWS_BRAM(t, d))
        case _      => Module(new SRAMVerilogAWS(t, d))
      }
      mem.io.clk := clock
      mem.io.raddr := io.raddr
      mem.io.wen := io.wen
      mem.io.waddr := io.waddr
      mem.io.wdata := io.wdata.asUInt()
      mem.io.backpressure := io.backpressure
      mem.io.raddrEn := true.B
      mem.io.waddrEn := true.B

      // Implement WRITE_FIRST logic here
      // equality register
      val equalReg = RegNext(io.wen & (io.raddr === io.waddr), false.B)
      val wdataReg = RegNext(io.wdata.asUInt, 0.U)
      io.rdata := Mux(equalReg, wdataReg.asUInt, mem.io.rdata).asTypeOf(t)

    case _ =>
      val mem = Module(new SRAMVerilogSim(t, d))
      mem.io.clk := clock
      mem.io.raddr := io.raddr
      mem.io.wen := io.wen
      mem.io.waddr := io.waddr
      mem.io.wdata := io.wdata.asUInt()
      mem.io.backpressure := io.backpressure
      mem.io.raddrEn := true.B
      mem.io.waddrEn := true.B

      io.rdata := mem.io.rdata.asTypeOf(t)
  }
}

