package spatial.codegen.chiselgen

import argon._
import spatial.lang._
import spatial.metadata.blackbox._
import spatial.metadata.control._
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

    case VerilogCtrlBlackbox(inputs) =>
      enterCtrl(lhs)
      val inports: StreamStruct[_] = inputs.tp.asInstanceOf[StreamStruct[_]]
      val outports: StreamStruct[_] = lhs.tp.asInstanceOf[StreamStruct[_]]
      val BlackboxConfig(file, _, _, params) = lhs.bboxInfo
      val bbName: String = file.split("/").last.split('.').head
      inGen(out, src"bb_$lhs.scala") {
        emit(s"""package accel""")
        emit("import chisel3._")
        emit("import chisel3.experimental._")
        emit("import chisel3.util._")
        emit("import fringe.templates.memory._")
        emit("import fringe.templates._")
        emit("import java.nio.file.{Files, Paths, StandardCopyOption}")
        open(src"class ${bbName}_wrapper() extends Module() {")
        val inportString = inports.fields.map{case(name, typ) => src""" ("$name" -> ${bitWidth(typ)}) """}.mkString("Map(", ",", ")")
        val outportString = outports.fields.map{case(name, typ) => src""" ("$name" -> ${bitWidth(typ)}) """}.mkString("Map(", ",", ")")
          open(src"val io = IO(new Bundle {")
            emit("val clock = Input(Clock())")
            emit("val reset = Input(Bool())")
            emit("val enable = Input(Bool())")
            emit("val done = Output(Bool())")
            emit(src"""val in = Flipped(new StreamStructInterface($inportString))""")
            emit(src"""val out = new StreamStructInterface($outportString)""")
          close("})")
          emit(src"val vbox = Module(new ${bbName}())")
          emit("vbox.io.clock := io.clock")
          emit("vbox.io.reset := io.reset")
          emit("vbox.io.enable := io.enable")
          emit("io.done := vbox.io.done")
          inports.fields.foreach{case(name, typ) =>
             emit(src"""vbox.io.$name := io.in.get("$name").bits""")
             emit(src"""vbox.io.${name}_valid := io.in.get("$name").valid""")
             emit(src"""io.in.get("$name").ready := vbox.io.${name}_ready""")
           }
           outports.fields.foreach{case(name, typ) =>
             emit(src"""io.out.get("$name").bits := vbox.io.$name""")
             emit(src"""io.out.get("$name").valid := vbox.io.${name}_valid""")
             emit(src"""vbox.io.${name}_ready := io.out.get("$name").ready""")
           }
        close("}")
        open(src"class $bbName() extends BlackBox(")
        val paramString = params.map{case (param, value) => s""""$param" -> $value"""}.mkString("Map(",",",")")
        emit(src"$paramString")
        closeopen(") {")
          open("val io = IO(new Bundle{")
            emit("val clock = Input(Clock())")
            emit("val reset = Input(Bool())")
            emit("val enable = Input(Bool())")
            emit("val done = Output(Bool())")
            inports.fields.foreach{case(name, typ) =>
               emit(src"val $name = Input(UInt(${bitWidth(typ)}.W))")
               emit(src"val ${name}_valid = Input(Bool())")
               emit(src"val ${name}_ready = Output(Bool())")
             }
             outports.fields.foreach{case(name, typ) =>
               emit(src"val $name = Output(UInt(${bitWidth(typ)}.W))")
               emit(src"val ${name}_valid = Output(Bool())")
               emit(src"val ${name}_ready = Input(Bool())")
             }
          close("})")
          emit("val path = Files.copy(")
          emit(s"""    Paths.get("$file"),""")
          emit(s"""    Paths.get(System.getProperty("user.dir") + "/${file.split("/").last}"),""")
          emit("""    StandardCopyOption.REPLACE_EXISTING""")
          emit(")")
          emit("// TODO: Make sure file copies properly")
        close("}")
      }
      val idx = lhs.parent.s.get.children.indexWhere{x => x.s.get == lhs}
      emit(src"val ${lhs}_bbox = Module(new ${bbName}_wrapper())")
      emit(src"${lhs}_bbox.io.clock := clock")
      emit(src"${lhs}_bbox.io.reset := reset.toBool")
      emit(src"${lhs}_bbox.io.enable := io.sigsIn.smEnableOuts($idx)")
      if (lhs.parent.s.get.isLoopControl) {
        emit(src"io.sigsOut.cchainEnable($idx) := ${lhs}_bbox.io.done")
      } else {
        emit(src"io.sigsOut.smCtrCopyDone($idx) := ${lhs}_bbox.io.done")
      }
      val parentMask = and(lhs.parent.s.get.toScope.valids.map { x => appendSuffix(lhs, x) })
      emit(src"io.sigsOut.smDoneIn($idx) := ${lhs}_bbox.io.done")
      emit(src"io.sigsOut.smMaskIn($idx) := $parentMask")
      inputs match {
        case Op(SimpleStreamStruct(elems)) => elems.foreach { case (field, s) =>
          emit(src"""${lhs}_bbox.io.in.get("$field").bits := $s.r""")
          emit(src"""${lhs}_bbox.io.in.get("$field").valid := ${s}_valid""")
          emit(src"""${s}_ready := ${lhs}_bbox.io.in.get("$field").ready""")
        }
      }

      emit(src"val $lhs = ${lhs}_bbox.io.out")
      exitCtrl(lhs.parent.s.get)

     case _ => super.gen(lhs, rhs)
  }


}
