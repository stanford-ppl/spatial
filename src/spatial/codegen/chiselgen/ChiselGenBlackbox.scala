package spatial.codegen.chiselgen

import argon._
import spatial.lang._
import spatial.metadata.blackbox._
import spatial.node._

trait ChiselGenBlackbox extends ChiselGenCommon {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case VerilogBlackbox(inputs) =>
      val inports: Struct[_] = inputs.tp.asInstanceOf[Struct[_]]
      val outports: Struct[_] = lhs.tp.asInstanceOf[Struct[_]]
      val BlackboxConfig(file, latency, pipelined, params) = lhs.bboxInfo
      val bbName: String = file.split("/").last.split('.').head
      inGen(out, src"bb_$lhs.scala") {
        emit(s"""package accel""")
        emit("import chisel3._")
        emit("import chisel3.experimental._")
        emit("import chisel3.util._")
        emit("import java.nio.file.{Files, Paths, StandardCopyOption}")
        open(src"class $bbName() extends BlackBox(")
        val paramString = params.map{case (param, value) => s""""$param" -> $value"""}.mkString("Map(",",",")")
        emit(src"$paramString")
        closeopen(") {")
          open(src"val io = IO(new Bundle {")
            emit("val clock = Input(Clock())")
            emit("val reset = Input(Bool())")
            inports.fields.foreach{case(name, typ) => emit(src"val $name = Input(UInt(${bitWidth(typ)}.W))")}
            outports.fields.foreach{case(name, typ) => emit(src"val $name = Output(UInt(${bitWidth(typ)}.W))")}
          close("})")
          emit("val path = Files.copy(")
          emit(s"""    Paths.get("$file"),""")
          emit(s"""    Paths.get(System.getProperty("user.dir") + "/${file.split("/").last}"),""")
          emit("""    StandardCopyOption.REPLACE_EXISTING""")
          emit(")")
          emit("// TODO: Make sure file copies properly")

        close("}")
      }
      emit(src"val ${lhs}_bbox = Module(new $bbName())")
      emit(src"${lhs}_bbox.io.clock := clock")
      emit(src"${lhs}_bbox.io.reset := reset.toBool")
      inports.fields.foreach{case (field, _) =>
        val (start, end) = getField(inports, field)
        emit(src"${lhs}_bbox.io.$field := $inputs($start,$end).r")
      }
      val outbits = outports.fields.map{case (field, _) => src"${lhs}_bbox.io.$field"}.reverse.mkString(",")
      emit(src"val $lhs = Cat($outbits)")

     case _ => super.gen(lhs, rhs)
  }


}
