package spatial.codegen.chiselgen

import argon._
import spatial.lang._
import spatial.metadata.blackbox._
import spatial.metadata.control._
import spatial.metadata.access._
import spatial.metadata.memory.LocalMemories
import spatial.node._
import spatial.util.spatialConfig
import spatial.util.modeling.scrubNoise

import scala.collection.mutable.ArrayBuffer

trait ChiselGenBlackbox extends ChiselGenCommon {

  val createdBoxes: ArrayBuffer[String] = ArrayBuffer[String]()

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case VerilogBlackbox(inputs) =>
      val inports: Struct[_] = inputs.tp.asInstanceOf[Struct[_]]
      val outports: Struct[_] = lhs.tp.asInstanceOf[Struct[_]]
      val BlackboxConfig(file, moduleName, _, _, params) = lhs.bboxInfo
      val bbName: String = moduleName.getOrElse(file.split("/").last.split('.').head)
      inGen(out, src"bb_$lhs.scala") {
        emit(s"""package accel""")
        emit("import chisel3._")
        emit("import chisel3.experimental._")
        emit("import chisel3.util._")
        emit("import java.nio.file.{Files, Paths, StandardCopyOption}")
        open(src"class ${bbName}_${lhs}_wrapper() extends Module {")
        open(src"val io = IO(new Bundle {")
        emit("val clock = Input(Clock())")
        emit("val reset = Input(Bool())")
        inports.fields.foreach { case (name, typ) => emit(src"val $name = Input(UInt(${bitWidth(typ)}.W))") }
        outports.fields.foreach { case (name, typ) => emit(src"val $name = Output(UInt(${bitWidth(typ)}.W))") }
        close("})")
        val paramString = params.map { case (param, value) => s""""$param" -> IntParam($value)""" }.mkString("Map(", ",", ")")
        emit(src"val vbox = Module(new $bbName($paramString))")
        emit("vbox.io.clock := io.clock")
        emit("vbox.io.reset := io.reset")
        inports.fields.foreach { case (name, _) => emit(src"""vbox.io.$name := io.$name""") }
        outports.fields.foreach { case (name, _) => emit(src"""io.$name := vbox.io.$name""") }
        close("}")

        if (!createdBoxes.contains(bbName)) {
          createdBoxes += bbName
          open(src"class $bbName(params: Map[String, chisel3.core.Param]) extends BlackBox(params) {")
          open("val io = IO(new Bundle{")
          emit("val clock = Input(Clock())")
          emit("val reset = Input(Bool())")
          inports.fields.foreach { case (name, typ) => emit(src"val $name = Input(UInt(${bitWidth(typ)}.W))") }
          outports.fields.foreach { case (name, typ) => emit(src"val $name = Output(UInt(${bitWidth(typ)}.W))") }
          close("})")
          emit("val path = Files.copy(")
          emit(s"""    Paths.get("$file"),""")
          emit(s"""    Paths.get(System.getProperty("user.dir") + "/${file.split("/").last}"),""")
          emit("""    StandardCopyOption.REPLACE_EXISTING""")
          emit(")")
          emit("// TODO: Make sure file copies properly")
          close("}")
        }
      }
      emit(src"val ${lhs}_bbox = Module(new ${bbName}_${lhs}_wrapper())")
      emit(src"${lhs}_bbox.io.clock := clock")
      emit(src"${lhs}_bbox.io.reset := reset.toBool")
      inports.fields.foreach { case (field, _) =>
        val (start, end) = getField(inports, field)
        emit(src"${lhs}_bbox.io.$field := $inputs($start,$end).r")
      }
      val outbits = outports.fields.map { case (field, _) => src"${lhs}_bbox.io.$field" }.reverse.mkString(",")
      emit(src"val $lhs = Cat($outbits)")

    case VerilogCtrlBlackbox(ens, inputs) =>
      enterCtrl(lhs)
      val inports: StreamStruct[_] = inputs.tp.asInstanceOf[StreamStruct[_]]
      val outports: StreamStruct[_] = lhs.tp.asInstanceOf[StreamStruct[_]]
      val BlackboxConfig(file, moduleName, _, _, params) = lhs.bboxInfo
      val bbName: String = moduleName.getOrElse(file.split("/").last.split('.').head)
      inGen(out, src"bb_$lhs.scala") {
        emit(s"""package accel""")
        emit("import chisel3._")
        emit("import chisel3.experimental._")
        emit("import chisel3.util._")
        emit("import fringe.templates.memory._")
        emit("import fringe.templates._")
        emit("import java.nio.file.{Files, Paths, StandardCopyOption}")
        open(src"class ${bbName}_${lhs}_wrapper() extends Module() {")
        val inportString = inports.fields.map { case (name, typ) => src""" ("$name" -> ${bitWidth(typ)}) """ }.mkString("Map(", ",", ")")
        val outportString = outports.fields.map { case (name, typ) => src""" ("$name" -> ${bitWidth(typ)}) """ }.mkString("Map(", ",", ")")
        open(src"val io = IO(new Bundle {")
        emit("val clock = Input(Clock())")
        emit("val reset = Input(Bool())")
        emit("val enable = Input(Bool())")
        emit("val done = Output(Bool())")
        emit(src"""val in = Flipped(new StreamStructInterface($inportString))""")
        emit(src"""val out = new StreamStructInterface($outportString)""")
        close("})")
        val paramString = params.map { case (param, value) => s""""$param" -> IntParam($value)""" }.mkString("Map(", ",", ")")
        emit(src"val vbox = Module(new $bbName($paramString))")
        emit("vbox.io.clock := io.clock")
        emit("vbox.io.reset := io.reset")
        emit("vbox.io.enable := io.enable")
        emit("io.done := vbox.io.done")
        inports.fields.foreach { case (name, typ) =>
          emit(src"""vbox.io.$name := io.in.get("$name").bits""")
          emit(src"""vbox.io.${name}_valid := io.in.get("$name").valid""")
          emit(src"""io.in.getActive("$name").in := vbox.io.${name}_ready // Feed the ready (i.e. read inside blackbox) to external world""")
          emit(src"""io.in.get("$name").ready := vbox.io.${name}_ready""")
        }
        outports.fields.foreach { case (name, typ) =>
          emit(src"""io.out.get("$name").bits := vbox.io.$name""")
          emit(src"""io.out.get("$name").valid := vbox.io.${name}_valid""")
          emit(src"""io.out.getActive("$name").out := io.out.getActive("$name").in // Loopback the enable of some external reader """)
          emit(src"""vbox.io.${name}_ready := io.out.get("$name").ready""")
        }
        close("}")

        if (!createdBoxes.contains(bbName)) {
          createdBoxes += bbName
          open(src"class $bbName(params: Map[String, chisel3.core.Param]) extends BlackBox(params){")
          open("val io = IO(new Bundle{")
          emit("val clock = Input(Clock())")
          emit("val reset = Input(Bool())")
          emit("val enable = Input(Bool())")
          emit("val done = Output(Bool())")
          inports.fields.foreach { case (name, typ) =>
            emit(src"val $name = Input(UInt(${bitWidth(typ)}.W))")
            emit(src"val ${name}_valid = Input(Bool())")
            emit(src"val ${name}_ready = Output(Bool())")
          }
          outports.fields.foreach { case (name, typ) =>
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
      }
      val idx = lhs.parent.s.get.children.indexWhere { x => x.s.get == lhs }
      emit(src"val ${lhs}_bbox = Module(new ${bbName}_${lhs}_wrapper())")
      emit(src"${lhs}_bbox.io.clock := clock")
      emit(src"${lhs}_bbox.io.reset := reset.toBool")
      emit(src"${lhs}_bbox.io.enable := io.sigsIn.smEnableOuts($idx) & ${and(ens.map{x => appendSuffix(lhs, x)})}")
      createAndTieInstrs(lhs)
      if (lhs.parent.s.get.isLoopControl) {
        emit(src"io.sigsOut.cchainEnable($idx) := ${lhs}_bbox.io.done")
      } else {
        emit(src"io.sigsOut.smCtrCopyDone($idx) := ${lhs}_bbox.io.done")
      }
      val parentMask = and(lhs.parent.s.get.toScope.valids.map { x => appendSuffix(lhs, x) })
      emit(src"io.sigsOut.smDoneIn($idx) := ${lhs}_bbox.io.done")
      emit(src"io.sigsOut.smMaskIn($idx) := $parentMask")
      emit(src"${lhs}_bbox.io.in <> $inputs")

      emit(src"val $lhs = ${lhs}_bbox.io.out")
      exitCtrl(lhs.parent.s.get)

    case SpatialBlackboxImpl(func) =>
      val inports: Struct[_] = func.input.tp.asInstanceOf[Struct[_]]
      val outports: Struct[_] = lhs.tp.typeArgs.last.asInstanceOf[Struct[_]]
      inBox {
        inGen(out, src"bb_$lhs.scala") {
          emit(s"""package accel""")
          emit("import chisel3._")
          emit("import chisel3.util._")
          emit("import fringe.templates.memory._")
          emit("import fringe.templates._")
          emit("import fringe._")
          emit("import fringe.Ledger._")
          emit("import fringe.utils._")
          emit("import fringe.utils.implicits._")
          emit("import fringe.templates.math._")
          emit("import fringe.templates.counters._")
          emit("import fringe.templates.vector._")
          emit("import fringe.templates.axi4._")
          emit("import fringe.SpatialBlocks._")
          emit("import fringe.templates.memory._")
          emit("import fringe.templates.memory.implicits._")
          emit("import fringe.templates.retiming._")
          emit("import emul.ResidualGenerator._")
          emit("import fringe.templates.euresys._")
          emit("import api._")
          open(src"class $lhs()(implicit stack: List[KernelHash]) extends Module() {")
          open(src"val io = IO(new Bundle {")
          emit(s"val sigsIn = Input(new InputKernelSignals(1,1,List(1),List(32)))")
          emit("val rr = Input(Bool())")
          emit(s"val in_breakpoints = Vec(api.numArgOuts_breakpts, Output(Bool()))")
          inports.fields.foreach { case (name, typ) => emit(src"val $name = Input(UInt(${bitWidth(typ)}.W))") }
          outports.fields.foreach { case (name, typ) => emit(src"val $name = Output(UInt(${bitWidth(typ)}.W))") }
          close("})")
          emit("val rr = io.rr")
          emit("val breakpoints = io.in_breakpoints; breakpoints := DontCare")
          emit(s"val ${func.input} = Cat(${inports.fields.reverse.map{case (name,_) => s"io.$name"}.mkString(",")})")
          emit("// Emit blackbox function")
          gen(func)
          emit("// Connect function result to module outputs")
          outports.fields.foreach { case (field, typ) =>
            val (start, end) = getField(outports, field)
            emit(src"io.$field := ${func.result}($start,$end).r")
          }
          close("}")
        }
      }

    case SpatialBlackboxUse(bbox, inputs) =>
      val inports: Struct[_] = inputs.tp.asInstanceOf[Struct[_]]
      val outports: Struct[_] = lhs.tp.asInstanceOf[Struct[_]]
      emit(src"val ${lhs}_bbox = Module(new $bbox())")
      inports.fields.foreach { case (field, _) =>
        val (start, end) = getField(inports, field)
        emit(src"${lhs}_bbox.io.$field := $inputs($start,$end).r")
      }
      emit(src"${lhs}_bbox.io.sigsIn := io.sigsIn")
      emit(src"${lhs}_bbox.io.rr := rr")
      emit(src"${lhs}_bbox.io.in_breakpoints <> io.in_breakpoints")
      val outbits = outports.fields.map { case (field, _) => src"${lhs}_bbox.io.$field" }.reverse.mkString(",")
      emit(src"val $lhs = Cat($outbits)")

    case SpatialCtrlBlackboxImpl(func) =>
      LocalMemories += func.input // Push the input onto the local memory stack as an honarary memory for the sake of codegen here
      if (!spatialConfig.enableModular) throw new Exception("Cannot generate a Ctrl blackbox without modular codegen enabled!")
      val inports: StreamStruct[_] = func.input.tp.asInstanceOf[StreamStruct[_]]
      val outports: StreamStruct[_] = lhs.tp.typeArgs.last.asInstanceOf[StreamStruct[_]]
      inBox {
        enterCtrl(lhs)
        inGen(out, src"bb_$lhs.scala") {
          emitHeader()
          open(src"class ${lhs}_kernel(")
          emit(s"in: ${arg(func.input.tp)},")
          emit(s"parent: Option[Kernel], cchain: List[CounterChainInterface], childId: Int, nMyChildren: Int, ctrcopies: Int, ctrPars: List[Int], ctrWidths: List[Int], breakpoints: Vec[Bool], ${if (spatialConfig.enableInstrumentation) "instrctrs: List[InstrCtr], " else ""}rr: Bool")
          closeopen(") extends Kernel(parent, cchain, childId, nMyChildren, ctrcopies, ctrPars, ctrWidths) {")

          // Poor man's createSMObject
          val lat = if (spatialConfig.enableRetiming & lhs.isInnerControl) scrubNoise(lhs.bodyLatency.sum) else 0.0
          val ii = if (lhs.II <= 1 | !spatialConfig.enableRetiming | lhs.isOuterControl) 1.0 else scrubNoise(lhs.II)
          emit("")
          emit("val me = this")
          emit(src"""val sm = Module(new OuterControl(Sequenced, ${lhs.children.size}, isFSM = false, latency = $lat.toInt, myName = "${lhs}_sm")); sm.io <> DontCare""")
          emit(src"""val iiCtr = Module(new IICounter($ii.toInt, 2 + _root_.utils.math.log2Up($ii.toInt), "${lhs}_iiCtr"))""")
          emit("")
          open(src"abstract class ${lhs}_module(depth: Int)(implicit stack: List[KernelHash]) extends Module {")
            open("val io = IO(new Bundle {")
            emit(src"val in = ${port(func.input.tp, Some(func.input))}")
            if (spatialConfig.enableInstrumentation) emit("val in_instrctrs = Vec(api.numCtrls, Output(new InstrCtr()))")
            val nMyChildren = lhs.children.count(_.s.get != lhs) max 1
            emit(s"val in_breakpoints = Vec(api.numArgOuts_breakpts, Output(Bool()))")
            emit(s"val sigsIn = Input(new InputKernelSignals($nMyChildren, 1, List(1), List(32)))")
            emit(s"val sigsOut = Output(new OutputKernelSignals($nMyChildren, 1))")
            emit("val rr = Input(Bool())")
            val outportString = outports.fields.map { case (name, typ) => src""" ("$name" -> ${bitWidth(typ)}) """ }.mkString("Map(", ",", ")")
            emit(src"""val out = new StreamStructInterface($outportString)""")
            close("})")
            emit(src"def ${func.input} = io.in")
            close("}")


            open(src"def kernel(): ${arg(func.result.tp)} = {")
            emit(src"""Ledger.enter(this.hashCode, "$lhs")""")
            emit("implicit val stack = ControllerStack.stack.toList")
            open(src"class ${lhs}_concrete(depth: Int)(implicit stack: List[KernelHash]) extends ${lhs}_module(depth) {")
            emit("io.sigsOut := DontCare")
            emit("val breakpoints = io.in_breakpoints; breakpoints := DontCare")
            if (spatialConfig.enableInstrumentation) emit("val instrctrs = io.in_instrctrs; instrctrs := DontCare")
            emit("val rr = io.rr")
            createAndTieInstrs(lhs)

            emit("// Emit blackbox function")
            gen(func)

            emit("// Wire up the output ports")
            outports.fields.foreach {case (name, _) => emit(src"""io.out.getActive("$name").out := io.out.getActive("$name").in // Loopback the enable of some external reader """)}

            emit(src"io.out <> ${func.result}")

            close("}")
            emit(src"val module = Module(new ${lhs}_concrete(sm.p.depth)); module.io := DontCare")
            emit("// Connect ports on this kernel to its parent")
            emit(src"in.connectLedger(module.io.in)")
            emit(src"module.io.in <> in")
            if (spatialConfig.enableInstrumentation) emit("Ledger.connectInstrCtrs(instrctrs, module.io.in_instrctrs)")
            emit(src"Ledger.connectBreakpoints(breakpoints, module.io.in_breakpoints)")
            emit("module.io.rr := rr")
            emit("module.io.sigsIn := me.sigsIn")
            emit("me.sigsOut := module.io.sigsOut")
            emit("val out = module.io.out")
            emit("""Ledger.exit()""")
            emit("out")
            close("}")
            close("}")
          }
        }
        exitCtrl(lhs)
        LocalMemories -= func.input

    case SpatialCtrlBlackboxUse(ens, bbox, inputs) =>
      val inports: StreamStruct[_] = inputs.tp.asInstanceOf[StreamStruct[_]]
      val outports: StreamStruct[_] = lhs.tp.asInstanceOf[StreamStruct[_]]
      enterCtrl(lhs)
      val idx = lhs.parent.s.get.children.indexWhere { x => x.s.get == lhs }
      val inportString = inports.fields.map { case (name, typ) => src""" ("$name" -> ${bitWidth(typ)}) """ }.mkString("Map(", ",", ")")
      val outportString = outports.fields.map { case (name, typ) => src""" ("$name" -> ${bitWidth(typ)}) """ }.mkString("Map(", ",", ")")
      emit(src"val ${lhs}_bbox = new ${bbox}_kernel($inputs, Some(me), List(), 1, 1, 1, List(1), List(32), breakpoints, rr)")
      emit(src"""${lhs}_bbox.sm.io.ctrDone := risingEdge(${lhs}_bbox.sm.io.ctrInc)""")
      emit(src"${lhs}_bbox.backpressure := ${getBackPressure(lhs.toCtrl)} | ${lhs}_bbox.sm.io.doneLatch")
      emit(src"${lhs}_bbox.forwardpressure := true.B | ${lhs}_bbox.sm.io.doneLatch // Always has forward pressure because it is an outer?")
      emit(src"${lhs}_bbox.sm.io.enableOut.zip(${lhs}_bbox.smEnableOuts).foreach{case (l,r) => r := l}")
      emit(src"${lhs}_bbox.sm.io.break := false.B // TODO: What if bbox can raise break signal?!")
      emit(src"${lhs}_bbox.mask := ${and(ens.map{x => appendSuffix(lhs, x)})}")
      emit(src"""${lhs}_bbox.configure("$lhs", Some(io.sigsIn), Some(io.sigsOut), false)""")
      emit(src"val $lhs = ${lhs}_bbox.kernel()")

    case _ => super.gen(lhs, rhs)
  }


}
