package spatial.codegen.chiselgen

import argon._
import argon.codegen.{Codegen, FileDependencies}
import spatial.codegen.naming._
import spatial.internal.{spatialConfig => cfg}
import emul.FloatPoint
import emul.FixedPoint
import spatial.lang._
import spatial.node._
import emul.Bool
import spatial.traversal.AccelTraversal

trait ChiselCodegen extends NamedCodegen with FileDependencies with AccelTraversal {
  override val lang: String = "chisel"
  override val ext: String = "scala"
  override def entryFile: String = s"RootController_1.$ext"

  var streamLines = collection.mutable.Map[String, Int]() // Map from filename number of lines it has
  var streamExtensions = collection.mutable.Map[String, Int]() // Map from filename to number of extensions it has
  val tabWidth: Int = 2
  val maxLinesPerFile = 400
  var compressorMap = collection.mutable.HashMap[String, (String,Int)]()
  var retimeList = collection.mutable.ListBuffer[String]()
  val pipeRtMap = collection.mutable.HashMap[(String,Int), String]()
  var maxretime = 0
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
    inGenn(out, "RootController", ext, forceful=true) {
      outsideAccel{
        visitBlock(b)
      }
    }
    // if (withReturn) emitt(src"${b.result}")
  }

  override protected def quoteConst(tp: Type[_], c: Any): String = (tp,c) match {
    case (FixPtType(s,d,f), _) => c.toString + {if (d+f >= 32 && f == 0) "L" else ""} + {if (f == 0 && !s) s".U($d.W)" else s".FP($s, $d, $f)"}
    case (FltPtType(g,e), _) => c.toString + s".FlP($g, $e)"
    case (_:Bit, c:Bool) => s"${c.value}.B"
    case _ => super.quoteConst(tp,c)
  }

  override protected def remap(tp: Type[_]): String = tp match {
    case FixPtType(s,d,f) => if (f == 0 && !s) s"UInt($d.W)" else s"new FixedPoint($s, $d, $f)"
    case FltPtType(g,e) => s"new FloatingPoint($e, $g)"
    case BitType() => "Bool()"
    case tp: Vec[_] => src"Vec(${tp.width}, ${tp.typeArgs.head})"
    // case tp: StructType[_] => src"UInt(${bitWidth(tp)}.W)"
    case _ => super.remap(tp)
  }

  final protected def wireMap(x: String): String = { 
    if (cfg.compressWires == 1 | cfg.compressWires == 2) {
      if (compressorMap.contains(x)) {
        src"${listHandle(compressorMap(x)._1)}(${compressorMap(x)._2})"
      } else {
        x
      }
    } else {
      x
    }
  }

  final protected def listHandle(rhs: String): String = {
    val vec = if (rhs.contains("Vec")) {
      val width_extractor = "Wire\\([ ]*Vec\\(([0-9]+)[ ]*,.*".r
      val width_extractor(vw) = rhs
      s"vec${vw}_"
    } else {""}
      if (rhs.contains("Bool()")) {
      s"${vec}b"
    } else if (rhs.contains("SRFF()")) {
      s"${vec}srff"
    } else if (rhs.contains("UInt(")) {
      val extractor = ".*UInt\\(([0-9]+).W\\).*".r
      val extractor(width) = rhs
      s"${vec}u${width}"
    } else if (rhs.contains("SInt(")) {
      val extractor = ".*SInt\\(([0-9]+).W\\).*".r
      val extractor(width) = rhs
      s"${vec}s${width}"
    } else if (rhs.contains(" FixedPoint(")) {
      val extractor = ".*FixedPoint\\([ ]*(.*)[ ]*,[ ]*([0-9]+)[ ]*,[ ]*([0-9]+)[ ]*\\).*".r
      val extractor(s,i,f) = rhs
      val ss = if (s.contains("rue")) "s" else "u"
      s"${vec}fp${ss}${i}_${f}"
    } else if (rhs.contains(" FloatingPoint(")) {
      val extractor = ".*FloatingPoint\\([ ]*([0-9]+)[ ]*,[ ]*([0-9]+)[ ]*\\).*".r
      val extractor(m,e) = rhs
      s"${vec}flt${m}_${e}"
    } else if (rhs.contains(" NBufFF(") && !rhs.contains("numWriters")) {
      val extractor = ".*NBufFF\\([ ]*([0-9]+)[ ]*,[ ]*([0-9]+)[ ]*\\).*".r
      val extractor(d,w) = rhs
      s"${vec}nbufff${d}_${w}"
    } else if (rhs.contains(" NBufFF(") && rhs.contains("numWriters")) {
      val extractor = ".*FF\\([ ]*([0-9]+)[ ]*,[ ]*([0-9]+)[ ]*,[ ]*numWriters[ ]*=[ ]*([0-9]+)[ ]*\\).*".r
      val extractor(d,w,n) = rhs
      s"${vec}ff${d}_${w}_${n}wr"
    } else if (rhs.contains(" templates.FF(")) {
      val extractor = ".*FF\\([ ]*([0-9]+)[ ]*,[ ]*([0-9]+)[ ]*\\).*".r
      val extractor(d,w) = rhs
      s"${vec}ff${d}_${w}"
    } else if (rhs.contains(" R_Info(")) {
      val extractor = ".*R_Info\\([ ]*([0-9]+)[ ]*,[ ]*List\\(([0-9,]+)\\)[ ]*\\).*".r
      val extractor(n,dims) = rhs
      val d = dims.replace(" ", "").replace(",","_")
      s"${vec}mdr${n}_${d}"
    } else if (rhs.contains(" W_Info(")) {
      val extractor = ".*W_Info\\([ ]*([0-9]+)[ ]*,[ ]*List\\(([0-9,]+)\\)[ ]*,[ ]*([0-9]+)[ ]*\\).*".r
      val extractor(n,dims,w) = rhs
      val d = dims.replace(" ", "").replace(",","_")
      s"${vec}mdw${n}_${d}_${w}"
    } else if (rhs.contains(" RegR_Info(")) {
      val extractor = ".*RegR_Info\\([ ]*([0-9]+)[ ]*,[ ]*List\\(([0-9,]+)\\)[ ]*\\).*".r
      val extractor(n,dims) = rhs
      val d = dims.replace(" ", "").replace(",","_")
      s"${vec}ri${n}_${d}"
    } else if (rhs.contains(" RegW_Info(")) {
      val extractor = ".*RegW_Info\\([ ]*([0-9]+)[ ]*,[ ]*List\\(([0-9,]+)\\)[ ]*,[ ]*([0-9]+)[ ]*\\).*".r
      val extractor(n,dims,w) = rhs
      val d = dims.replace(" ", "").replace(",","_")
      s"${vec}wi${n}_${d}_${w}"
    } else if (rhs.contains(" multidimRegW(")) {
      val extractor = ".*multidimRegW\\([ ]*([0-9]+)[ ]*,[ ]*List\\(([0-9, ]+)\\)[ ]*,[ ]*([0-9]+)[ ]*\\).*".r
      val extractor(n,dims,w) = rhs
      val d = dims.replace(" ", "").replace(",","_")
      s"${vec}mdrw${n}_${d}_${w}"
    } else if (rhs.contains(" Seqpipe(")) {
      val extractor = ".*Seqpipe\\([ ]*([0-9]+)[ ]*,[ ]*isFSM[ ]*=[ ]*([falsetrue]+)[ ]*,[ ]*ctrDepth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*stateWidth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*staticNiter[ ]*=[ ]*([falsetrue]+),[ ]*isReduce[ ]*=[ ]*([falsetrue]+)\\).*".r
      val extractor(stages,fsm,ctrd,stw,static,isRed) = rhs
      val f = fsm.replace("false", "f").replace("true", "t")
      val s = static.replace("false", "f").replace("true", "t")
      val ir = isRed.replace("false", "f").replace("true", "t")
      s"${vec}seq${stages}_${f}_${ctrd}_${stw}_${s}_${ir}"
    } else if (rhs.contains(" Metapipe(")) {
      val extractor = ".*Metapipe\\([ ]*([0-9]+)[ ]*,[ ]*isFSM[ ]*=[ ]*([falsetrue]+)[ ]*,[ ]*ctrDepth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*stateWidth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*staticNiter[ ]*=[ ]*([falsetrue]+),[ ]*isReduce[ ]*=[ ]*([falsetrue]+)\\).*".r
      val extractor(stages,fsm,ctrd,stw,static,isRed) = rhs
      val f = fsm.replace("false", "f").replace("true", "t")
      val s = static.replace("false", "f").replace("true", "t")
      val ir = isRed.replace("false", "f").replace("true", "t")
      s"${vec}meta${stages}_${f}_${ctrd}_${stw}_${s}_${ir}"
    } else if (rhs.contains(" Innerpipe(")) {
      val extractor = ".*Innerpipe\\([ ]*([falsetrue]+)[ ]*,[ ]*ctrDepth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*stateWidth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*staticNiter[ ]*=[ ]*([falsetrue]+),[ ]*isReduce[ ]*=[ ]*([falsetrue]+)\\).*".r
      val extractor(strm,ctrd,stw,static,isRed) = rhs
      val st = strm.replace("false", "f").replace("true", "t")
      val s = static.replace("false", "f").replace("true", "t")
      val ir = isRed.replace("false", "f").replace("true", "t")
      s"${vec}inner${st}_${ctrd}_${stw}_${s}_${ir}"
    } else if (rhs.contains(" Streaminner(")) {
      val extractor = ".*Streaminner\\([ ]*([falsetrue]+)[ ]*,[ ]*ctrDepth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*stateWidth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*staticNiter[ ]*=[ ]*([falsetrue]+),[ ]*isReduce[ ]*=[ ]*([falsetrue]+)\\).*".r
      val extractor(strm,ctrd,stw,static,isRed) = rhs
      val st = strm.replace("false", "f").replace("true", "t")
      val s = static.replace("false", "f").replace("true", "t")
      val ir = isRed.replace("false", "f").replace("true", "t")
      s"${vec}strinner${st}_${ctrd}_${stw}_${s}_${ir}"
    } else if (rhs.contains(" Parallel(")) {
      val extractor = ".*Parallel\\([ ]*([0-9]+)[ ]*,[ ]*isFSM[ ]*=[ ]*([falsetrue]+)[ ]*,[ ]*ctrDepth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*stateWidth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*staticNiter[ ]*=[ ]*([falsetrue]+),[ ]*isReduce[ ]*=[ ]*([falsetrue]+)\\).*".r
      val extractor(stages,fsm,ctrd,stw,static,isRed) = rhs
      val f = fsm.replace("false", "f").replace("true", "t")
      val s = static.replace("false", "f").replace("true", "t")
      val ir = isRed.replace("false", "f").replace("true", "t")
      s"${vec}parallel${stages}_${f}_${ctrd}_${stw}_${s}_${ir}"
    } else if (rhs.contains(" Streampipe(")) {
      val extractor = ".*Streampipe\\([ ]*([0-9]+)[ ]*,[ ]*isFSM[ ]*=[ ]*([falsetrue]+)[ ]*,[ ]*ctrDepth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*stateWidth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*staticNiter[ ]*=[ ]*([falsetrue]+),[ ]*isReduce[ ]*=[ ]*([falsetrue]+)\\).*".r
      val extractor(stages,fsm,ctrd,stw,static,isRed) = rhs
      val f = fsm.replace("false", "f").replace("true", "t")
      val s = static.replace("false", "f").replace("true", "t")
      val ir = isRed.replace("false", "f").replace("true", "t")
      s"${vec}strmpp${stages}_${f}_${ctrd}_${stw}_${s}_${ir}"
    } else if (rhs.contains("_retime")) {
      "rt"
    } else {
      throw new Exception(s"Cannot compress ${rhs}!")
    }
  }

  override def named(s: Sym[_], id: Int): String = s.op match {
    case Some(rhs) => rhs match {
      case _: AccelScope       => s"RootController"
      case DelayLine(size, data) => data match {
        case Const(_) => src"$data"
        case _ => wireMap(src"${data}_D$size" + alphaconv.getOrElse(src"${data}_D$size", ""))
      }
      case _ => super.named(s, id)
    }
    case _ => super.named(s, id)
  }

  final protected def startFile(): Unit = {
      emit("""package accel""")
      emit("import templates._")
      emit("import templates.ops._")
      emit("import types._")
      emit("import api._")
      emit("import chisel3._")
      emit("import chisel3.util._")
      emit("import Utils._")    
      emit("import scala.collection.immutable._")
  }

  final protected def inSubGen[A](name: String, parent: String)(body: => A): Unit = { // Places body inside its own trait file and includes it at the end
    val prnts = if (inHw) List.tabulate(streamExtensions(parent)){i => src"${parent}_${i+1}"}.mkString(" with ") else ""
    emitt(src"// Creating sub kernel ${name}_1")
    inGenn(out, name, ext) {
      startFile()
      open(src"""trait ${name}_1 extends ${prnts} {""")
      if (cfg.compressWires == 2) { emit(src"""def method_${name}_1() {""") }
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

  final protected def inGenn[T](out: String, base: String, ext: String, forceful: Boolean = false)(blk: => T): Unit = {
    // Lookup current split extension and number of lines
    if (inHw | forceful) {
      if (!streamExtensions.contains(base)) streamExtensions += base -> 1
      val currentOverflow = s"_${streamExtensions(base)}"
      if (!streamLines.contains(base + currentOverflow + "." + ext)) streamLines += {base + currentOverflow + "." + ext} -> 0
      inGen(out, base + currentOverflow + "." + ext)(blk)
    }
  }

  protected def emitt(x: String, forceful: Boolean = false): Unit = {
    val curStream = state.streamName.split("/").last
    val curStreamNoExt = curStream.split("\\.").dropRight(1).last
    val base = curStream.split("_").dropRight(1).mkString("_")
    val lineCount = streamLines(curStream)
    val on = config.enGen
    if (forceful) {config.enGen = true}
    if (config.enGen) {
      streamLines(curStream) += 1
      if (lineCount > maxLinesPerFile) {
        val newExt = streamExtensions(base) + 1
        streamExtensions += base -> newExt
        val newStreamString = (curStream.split("_").dropRight(1) :+ s"${newExt}").mkString("_")
        val newStream = getOrCreateStream(out, newStreamString + "." + ext)
        streamLines += {newStreamString + "." + ext} -> 0
        state.gen = newStream
        startFile()
        open(src"""trait ${newStreamString} extends ${curStreamNoExt} {""")
        if (cfg.compressWires == 2) { emit(src"""def method_${newStreamString}() {""") }
      }
    }
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
    dependencies ::= DirDep(resourcesPath, "emul", relPath = "template-level/")
    dependencies ::= DirDep(resourcesPath, "hardfloat", relPath = "template-level/templates/")
    dependencies ::= DirDep(resourcesPath, "fringeHW", relPath = "template-level/")
    dependencies ::= DirDep(resourcesPath, "fringeZynq", relPath = "template-level/")
    // dependencies ::= DirDep(resourcesPath, "fringeASIC", relPath = "template-level/")
    // dependencies ::= DirDep(resourcesPath, "fringeDE1SoC", relPath = "template-level/")
    dependencies ::= DirDep(resourcesPath, "fringeVCS", relPath = "template-level/")
    // dependencies ::= DirDep(resourcesPath, "fringeXSIM", relPath = "template-level/")
    dependencies ::= DirDep(resourcesPath, "fringeAWS", relPath = "template-level/")
    // dependencies ::= DirDep(resourcesPath, "fringeArria10", relPath = "template-level/")
    dependencies ::= DirDep(resourcesPath, "scripts", "../", Some("scripts/"))

    dependencies ::= FileDep(resourcesPath, "Top.scala", outputPath = Some("Top.scala"))

    super.copyDependencies(out)
  }


}
