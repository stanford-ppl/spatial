package spatial.codegen.chiselgen

import argon._
import argon.codegen.{Codegen, FileDependencies}
import spatial.internal.{spatialConfig => cfg}
import emul.FloatPoint
import emul.FixedPoint
import spatial.lang._
import spatial.node._

trait ChiselCodegen extends FileDependencies  {
  override val lang: String = "chisel"
  override val ext: String = "scala"
  override def entryFile: String = s"RootController_1.$ext"

  var streamLines = collection.mutable.Map[String, Int]() // Map from filename number of lines it has
  var streamExtensions = collection.mutable.Map[String, Int]() // Map from filename to number of extensions it has
  val tabWidth: Int = 2
  val maxLinesPerFile = 200
  var compressorMap = collection.mutable.HashMap[String, (String,Int)]()
  var retimeList = collection.mutable.ListBuffer[String]()
  val pipeRtMap = collection.mutable.HashMap[(String,Int), String]()
  var maxretime = 0
  var itersMap = new scala.collection.mutable.HashMap[Sym[_], List[Sym[_]]]
  /** Map for tracking defs of nodes. If they get redeffed anywhere, we map it to a suffix */
  var alphaconv = collection.mutable.HashMap[String, String]()

  final def alphaconv_register(xx: String): Unit = {
    val x = "_reuse[0-9]+".r.replaceAllIn(xx, "")
    if (alphaconv.contains(x)) {
      val suf = alphaconv(x).replace("_reuse","")
      if (suf == "") {
        alphaconv += (x -> "_reuse1") // If already used, increment suffix
      } else {
        val newsuf = suf.toInt + 1
        alphaconv += (x -> s"_reuse$newsuf")
      }
    } else {
      alphaconv += (x -> "") // Otherwise don't suffix it
    }
  }

  override protected def gen(b: Block[_], withReturn: Boolean = false): Unit = {
    inGenn(out, "RootController", ext) {
      exitAccel()
      visitBlock(b)
      enterAccel()
    }
    // if (withReturn) emitt(src"${b.result}")
  }

  override protected def quoteConst(tp: Type[_], c: Any): String = (tp,c) match {
    case (FixPtType(s,d,f), _) => c.toString + {if (f == 0 && !s) s".U($d.W)" else s".FP($s, $d, $f)"}
    case (FltPtType(g,e), _) => c.toString + s".FlP($g, $e)"
    case _ => super.quoteConst(tp,c)
  }

  override protected def remap(tp: Type[_]): String = tp match {
    case FixPtType(s,d,f) => if (f == 0 && !s) s"UInt($d.W)" else s"new FixedPoint($s, $d, $f)"
    case FltPtType(g,e) => s"new FloatingPoint($e, $g)"
    case BitType() => "Bool()"
    // case tp: VectorType[_] => src"Vec(${tp.width}, ${tp.typeArguments.head})"
    // case tp: StructType[_] => src"UInt(${bitWidth(tp)}.W)"
    case _ => super.remap(tp)
  }

  override def named(s: Sym[_], id: Int): String = s.op match {
    case Some(rhs) => rhs match {
      case _: AccelScope       => s"RootController"
      case _ => super.named(s, id)
    }
    case _ => super.named(s, id)
  }

  final protected def inSubGen[A](name: String, parent: String)(body: => A): Unit = { // Places body inside its own trait file and includes it at the end
    val prnts = List.tabulate(streamExtensions(parent)){i => src"${parent}_${i+1}"}
    emit(src"// Creating sub kernel ${name}_1")
    inGenn(out, name, ext) {
      emit("""package accel""")
      emit("import templates._")
      emit("import templates.ops._")
      emit("import types._")
      emit("import chisel3._")
      emit("import chisel3.util._")
      open(src"""trait ${name}_1 extends ${prnts} {""")
      if (cfg.compressWires == 2) {
        emit(src"""def method_${name}_1() {""")
      }
      try { body }
      finally {
        inGennAll(out, name, ext){
          emit(s"} ${if (cfg.compressWires == 2) "}" else ""}")
        }
      }
    }
  }

  final protected def inGennAll[T](out: String, base: String, ext: String)(blk: => T): Unit = {
    List.tabulate(streamExtensions(base)) {i => 
      inGen(out, base + "_" + {i+1} + "." + ext)(blk)
    }
    ()
  }

  final protected def inGenn[T](out: String, base: String, ext: String)(blk: => T): Unit = {
    // Lookup current split extension and number of lines
    if (!streamExtensions.contains(base)) streamExtensions += base -> 1
    val currentOverflow = s"_${streamExtensions(base)}"
    if (!streamLines.contains(base + currentOverflow + "." + ext)) streamLines += {base + currentOverflow + "." + ext} -> 0
    inGen(out, base + currentOverflow + "." + ext)(blk)
  }

  protected def emitt(x: String, forceful: Boolean = false): Unit = {
    val lineCount = streamLines(state.streamName.split("/").last)
    streamLines(state.streamName.split("/").last) += 1
    if (lineCount > maxLinesPerFile) Console.println("exceeded!")
    val on = config.enGen
    if (forceful) {config.enGen = true}
    emit(x)
    config.enGen = on
  }

  final protected def emitGlobalWire(x: String, forceful: Boolean = false): Unit = {
    inGenn(out, "GlobalWires", ext) {
      val on = config.enGen
      if (forceful) {config.enGen = true}
      emitt(x)
      config.enGen = on
    }
  }

  final protected def emitGlobalWireMap(lhs: String, rhs: String, forceful: Boolean = false): Unit = {
    val module_type = rhs.replace("new ", "newnbsp").replace(" ", "").replace("nbsp", " ")
    if (cfg.compressWires == 1 | cfg.compressWires == 2) {
      if (!compressorMap.contains(lhs)) {
        val id = compressorMap.values.map(_._1).filter(_ == module_type).size
        compressorMap += (lhs -> (module_type, id))
      }
    } else {
      if (compressorMap.contains(lhs)) {
        emitGlobalWire(src"// val $lhs = $rhs already emitted", forceful)
      } else {
        compressorMap += (lhs -> (module_type, 0))
        emitGlobalWire(src"val $lhs = $rhs", forceful)
      }
    }
  }

  final protected def emitGlobalRetimeMap(lhs: String, rhs: String, forceful: Boolean = false): Unit = {
    val module_type = rhs.replace(" ", "")
    if (cfg.compressWires == 1 | cfg.compressWires == 2) {
      // Assume _retime values only emitted once
      val id = compressorMap.values.map(_._1).filter(_ == "_retime").size
      compressorMap += (lhs -> ("_retime", id))
      retimeList += rhs
    } else {
      emitGlobalWire(src"val $lhs = $rhs", forceful)
    }
  }

  final protected def emitGlobalModuleMap(lhs: String, rhs: String, forceful: Boolean = false): Unit = {
    val module_type_white = rhs.replace("new ", "newnbsp").replace(" ", "").replace("nbsp", " ")
    var rtid = "na"
    if (cfg.compressWires == 1 | cfg.compressWires == 2) {
      val module_type = if (module_type_white.contains("retime=")) {
        val extract = ".*retime=rt\\(([0-9]+)\\),.*".r
        val extract(x) = module_type_white
        rtid = x
        module_type_white.replace(s"retime=rt(${rtid}),","")
      } else {
        module_type_white
      }
      if (!compressorMap.contains(lhs)) {
        val id = compressorMap.values.map(_._1).filter(_ == module_type).size
        compressorMap += (lhs -> (module_type, id))
        if (rtid != "na") {
          pipeRtMap += ((module_type, id) -> rtid)
        }
      }
    } else {
      val module_type = module_type_white
      if (compressorMap.contains(lhs)) {
        emitGlobalModule(src"// val $lhs = $rhs already emitted", forceful)
      } else {
        compressorMap += (lhs -> (module_type, 0))
        emitGlobalModule(src"val $lhs = $rhs", forceful)
      }
    }
  }

  final protected def emitInstrumentation(x: String, forceful: Boolean = false): Unit = {
    inGenn(out, "Instrumentation", ext) {
      emitt(x, forceful)
    }
  }

  final protected def emitGlobalModule(x: String, forceful: Boolean = false): Unit = {
    inGenn(out, "GlobalModules", ext) {
      emitt(x, forceful)
    }
  }

  final protected def emitGlobalRetiming(x: String, forceful: Boolean = false): Unit = {
    inGenn(out, "GlobalRetiming", ext) {
      emitt(x, forceful)
    }
  }

  final protected def openGlobalWire(x: String, forceful: Boolean = false): Unit = {
    inGenn(out, "GlobalWires", ext) {
      open(x)
    }
  }

  final protected def openInstrumentation(x: String, forceful: Boolean = false): Unit = {
    inGenn(out, "Instrumentation", ext) {
      open(x)
    }
  }

  final protected def openGlobalModule(x: String, forceful: Boolean = false): Unit = {
    inGenn(out, "GlobalModules", ext) {
      open(x)
    }
  }

  final protected def openGlobalRetiming(x: String, forceful: Boolean = false): Unit = {
    inGenn(out, "GlobalRetiming", ext) {
      open(x)
    }
  }

  final protected def closeGlobalWire(x: String, forceful: Boolean = false): Unit = {
    inGenn(out, "GlobalWires", ext) {
      close(x)
    }
  }

  final protected def closeInstrumentation(x: String, forceful: Boolean = false): Unit = {
    inGenn(out, "Instrumentation", ext) {
      close(x)
    }
  }

  final protected def closeGlobalModule(x: String, forceful: Boolean = false): Unit = {
    inGenn(out, "GlobalModules", ext) {
      close(x)
    }
  }

  final protected def closeGlobalRetiming(x: String, forceful: Boolean = false): Unit = {
    inGenn(out, "GlobalRetiming", ext) {
      close(x)
    }
  }


  override def copyDependencies(out: String): Unit = {
    val resourcesPath = "synth/chisel-templates"

    dependencies ::= DirDep(resourcesPath, "templates", relPath = "template-level/")
    dependencies ::= DirDep(resourcesPath, "hardfloat", relPath = "template-level/templates/")
    dependencies ::= DirDep(resourcesPath, "fringeHW", relPath = "template-level/")
    dependencies ::= DirDep(resourcesPath, "fringeZynq", relPath = "template-level/")
    dependencies ::= DirDep(resourcesPath, "fringeASIC", relPath = "template-level/")
    // dependencies ::= DirDep(resourcesPath, "fringeDE1SoC", relPath = "template-level/")
    dependencies ::= DirDep(resourcesPath, "fringeVCS", relPath = "template-level/")
    dependencies ::= DirDep(resourcesPath, "fringeXSIM", relPath = "template-level/")
    dependencies ::= DirDep(resourcesPath, "fringeAWS", relPath = "template-level/")
    dependencies ::= DirDep(resourcesPath, "fringeArria10", relPath = "template-level/")
    dependencies ::= DirDep(resourcesPath, "fringeASIC", relPath = "template-level/")
    dependencies ::= DirDep(resourcesPath, "scripts", "../", Some("scripts/"))

    dependencies ::= FileDep(resourcesPath, "Top.scala", outputPath = Some("Top.scala"))

    super.copyDependencies(out)
  }


}
