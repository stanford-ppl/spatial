package fringe

import chisel3._
import chisel3.core.IntParam
import chisel3.util._


class SRAMVerilogIO[T <: Data](t: T, d: Int) extends Bundle {
    val addrWidth = log2Ceil(d)
    val clk = Input(Clock())
    val raddr = Input(UInt(addrWidth.W))
    val waddr = Input(UInt(addrWidth.W))
    val raddrEn = Input(Bool())
    val waddrEn = Input(Bool())
    val wen = Input(Bool())
    val flow = Input(Bool())
    val wdata = Input(UInt(t.getWidth.W))
    val rdata = Output(UInt(t.getWidth.W))
}

class SRAMVerilogSim[T <: Data](val t: T, val d: Int) extends BlackBox(
  Map("DWIDTH" -> IntParam(t.getWidth), "WORDS" -> IntParam(d), "AWIDTH" -> IntParam(log2Ceil(d))))
{
  val io = IO(new SRAMVerilogIO(t, d))
}

class SRAMVerilogAWS[T <: Data](val t: T, val d: Int) extends BlackBox(
  Map("DWIDTH" -> IntParam(t.getWidth), "WORDS" -> IntParam(d), "AWIDTH" -> IntParam(log2Ceil(d))))
{
  val io = IO(new SRAMVerilogIO(t, d))
}

class SRAMVerilogDE1SoC[T <: Data](val t: T, val d: Int) extends BlackBox(
  Map("DWIDTH" -> IntParam(t.getWidth), "WORDS" -> IntParam(d), "AWIDTH" -> IntParam(log2Ceil(d))))
{
  val io = IO(new SRAMVerilogIO(t, d))
}

class GenericRAMIO[T <: Data](t: T, d: Int) extends Bundle {
  val addrWidth = log2Ceil(d)
  val raddr = Input(UInt(addrWidth.W))
  val wen = Input(Bool())
  val waddr = Input(UInt(addrWidth.W))
  val wdata = Input(t)
  val rdata = Output(t)
  val flow = Input(Bool())

  override def cloneType(): this.type = {
    new GenericRAMIO(t, d).asInstanceOf[this.type]
  }
}

abstract class GenericRAM[T <: Data](val t: T, val d: Int) extends Module {
  val addrWidth = log2Ceil(d)
  val io = IO(new GenericRAMIO(t, d))
}

class FFRAM[T <: Data](override val t: T, override val d: Int) extends GenericRAM(t, d) {
  val rf = Module(new RegFilePure(t, d))
  rf.io.raddr := RegNext(io.raddr, 5.U)
  rf.io.wen := io.wen
  rf.io.waddr := io.waddr
  rf.io.wdata := io.wdata
  io.rdata := rf.io.rdata
}

class SRAM[T <: Data](override val t: T, override val d: Int) extends GenericRAM(t, d) {
  // Customize SRAM here
  FringeGlobals.target match {
    case "aws" | "zynq" | "zcu" | "arria10" =>
      val mem = Module(new SRAMVerilogAWS(t, d))
      mem.io.clk := clock
      mem.io.raddr := io.raddr
      mem.io.wen := io.wen
      mem.io.waddr := io.waddr
      mem.io.wdata := io.wdata.asUInt()
      mem.io.flow := io.flow
      mem.io.raddrEn := true.B
      mem.io.waddrEn := true.B

      // Implement WRITE_FIRST logic here
      // equality register
      val equalReg = RegNext(io.wen & (io.raddr === io.waddr), false.B)
      val wdataReg = RegNext(io.wdata.asUInt, 0.U)
      io.rdata := Mux(equalReg, wdataReg.asUInt, mem.io.rdata).asTypeOf(t)

    case "DE1" | "de1soc" =>
      val mem = Module(new SRAMVerilogDE1SoC(t, d))
      mem.io.clk := clock
      mem.io.raddr := io.raddr
      mem.io.wen := io.wen
      mem.io.waddr := io.waddr
      mem.io.wdata := io.wdata.asUInt()
      mem.io.flow := io.flow
      mem.io.raddrEn := true.B
      mem.io.waddrEn := true.B

    case _ =>
      val mem = Module(new SRAMVerilogSim(t, d))
      mem.io.clk := clock
      mem.io.raddr := io.raddr
      mem.io.wen := io.wen
      mem.io.waddr := io.waddr
      mem.io.wdata := io.wdata.asUInt()
      mem.io.flow := io.flow
      mem.io.raddrEn := true.B
      mem.io.waddrEn := true.B

      io.rdata := mem.io.rdata.asTypeOf(t)
  }
}

