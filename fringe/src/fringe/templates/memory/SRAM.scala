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
import fringe.targets.cxp.CXP

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

class SRAMVerilogDualReadIO[T<:Data](t: T, d: Int) extends Bundle {
    val clk = Input(Clock())
    val raddr0 = Input(UInt({1 max log2Ceil(d)}.W))
    val raddr1 = Input(UInt({1 max log2Ceil(d)}.W))
    val waddr = Input(UInt({1 max log2Ceil(d)}.W))
    val raddrEn0 = Input(Bool())
    val raddrEn1 = Input(Bool())
    val waddrEn = Input(Bool())
    val wen = Input(Bool())
    val backpressure0 = Input(Bool())
    val backpressure1 = Input(Bool())
    val wdata = Input(UInt(t.getWidth.W))
    val rdata0 = Output(UInt(t.getWidth.W))
    val rdata1 = Output(UInt(t.getWidth.W))

    override def cloneType = (new SRAMVerilogDualReadIO(t, d)).asInstanceOf[this.type] // See chisel3 bug 358

}

abstract class SRAMBlackBox[T<:Data](params: Map[String,Param]) extends BlackBox(params) {
  val io: SRAMVerilogIO[T]
}


class SRAMVerilogDualRead[T<:Data](val t: T, val d: Int) extends BlackBox(
  Map("DWIDTH" -> IntParam(t.getWidth), "WORDS" -> IntParam(d), "AWIDTH" -> IntParam({1 max log2Ceil(d)})))
{
  override val io = IO(new SRAMVerilogDualReadIO(t, d))
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

class GenericRAMDualReadIO[T<:Data](t: T, d: Int) extends Bundle {
  val addrWidth = {1 max log2Ceil(d)}
  val raddr0 = Input(UInt(addrWidth.W))
  val raddr1 = Input(UInt(addrWidth.W))
  val wen = Input(Bool())
  val waddr = Input(UInt(addrWidth.W))
  val wdata = Input(t.cloneType)
  val rdata0 = Output(t.cloneType)
  val rdata1 = Output(t.cloneType)
  val backpressure0 = Input(Bool())
  val backpressure1 = Input(Bool())

  override def cloneType: this.type = {
    new GenericRAMDualReadIO(t, d).asInstanceOf[this.type]
  }
}

abstract class GenericRAM[T<:Data](val t: T, val d: Int) extends Module {
  val addrWidth = {1 max log2Ceil(d)}
  val io: GenericRAMIO[T]
}
abstract class GenericRAMDualRead[T<:Data](val t: T, val d: Int) extends Module {
  val addrWidth = {1 max log2Ceil(d)}
  val io: GenericRAMDualReadIO[T]
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
    case _:AWS_F1 | _:Zynq | _:ZCU | _:Arria10 | _:KCU1500 | _:CXP =>

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


class SRAMDualRead[T<:Data](override val t: T, override val d: Int, val resourceType: String) extends GenericRAMDualRead(t, d) {
  val io = IO(new GenericRAMDualReadIO(t, d))

  // Customize SRAM here
  // TODO: Still needs some cleanup
  globals.target match {
//    case _:AWS_F1 | _:Zynq | _:ZCU | _:Arria10 | _:KCU1500 | _:CXP  =>
    case _ =>
      val mem = Module(new SRAMVerilogDualRead(t, d))

      mem.io.clk := clock
      mem.io.raddr0 := io.raddr0
      mem.io.raddr1 := io.raddr1
      mem.io.wen := io.wen
      mem.io.waddr := io.waddr
      mem.io.wdata := io.wdata.asUInt()
      mem.io.backpressure0 := io.backpressure0
      mem.io.backpressure1 := io.backpressure1
      mem.io.raddrEn0 := true.B
      mem.io.raddrEn1 := true.B
      mem.io.waddrEn := true.B

      // Implement WRITE_FIRST logic here
      // equality register
      val equalReg0 = RegNext(io.wen & (io.raddr0 === io.waddr), false.B)
      val equalReg1 = RegNext(io.wen & (io.raddr1 === io.waddr), false.B)
      val wdataReg = RegNext(io.wdata.asUInt, 0.U)
      io.rdata0 := Mux(equalReg0, wdataReg.asUInt, mem.io.rdata0).asTypeOf(t)
      io.rdata1 := Mux(equalReg1, wdataReg.asUInt, mem.io.rdata1).asTypeOf(t)

  }
}

