package spatial.codegen.chiselgen

import argon._
import argon.codegen.FileDependencies
import emul.{Bool, FloatPoint, FixedPoint}
import spatial.codegen.naming._
import spatial.lang._
import spatial.metadata.memory._
import spatial.node._
import spatial.util.spatialConfig
import spatial.traversal.AccelTraversal

import scala.collection.mutable


trait ChiselCodegen extends NamedCodegen with FileDependencies with AccelTraversal {
  override val lang: String = "chisel"
  override val ext: String = "scala"
  final val CODE_WINDOW: Int = 75

  protected val scoped: mutable.Map[Sym[_],String] = new mutable.HashMap[Sym[_],String]()
  private var globalBlockID: Int = 0

  override def named(s: Sym[_], id: Int): String = {
    val name = s.op match {
      case Some(rhs) => rhs match {
        case _: AccelScope       => s"RootController"
        case DelayLine(size, data) => data match {
          case Const(_) => src"$data"
          case _ => wireMap(src"${data}_D$size" + alphaconv.getOrElse(src"${data}_D$size", ""))
        }
        case _ => wireMap(super.named(s, id))
      }
      case _ => wireMap(super.named(s, id))
    }

    scoped.getOrElse(s, name)
  }

  override def emitHeader(): Unit = {
    emit("""package accel""")
    emit("import fringe.templates._")
    emit("import fringe.utils._")
    emit("import fringe.utils.implicits._")
    emit("import fringe.templates.math._")
    emit("import fringe.templates.counters._")
    emit("import fringe.templates.memory._")
    emit("import fringe.templates.retiming._")
    emit("import api._")
    emit("import chisel3._")
    emit("import chisel3.util._")
    emit("import Args._")
    emit("import scala.collection.immutable._")
    emit("")
    super.emitHeader()
  }


  override protected def gen(b: Block[_], withReturn: Boolean = false): Unit = {
    if (b.stms.length < CODE_WINDOW) {
      visitBlock(b)
    }
    else {
      globalBlockID += 1
      // TODO: More hierarchy? What if the block is > CODE_WINDOW * CODE_WINDOW size?
      val blockID: Int = globalBlockID
      var chunkID: Int = 0
      var chunk: Seq[Sym[_]] = Nil
      var remain: Seq[Sym[_]] = b.stms
      // TODO: Other ways to speed this up?
      // Memories are always global in scala generation right now
      def isLive(s: Sym[_]): Boolean = !s.isMem && (b.result == s || remain.exists(_.nestedInputs.contains(s)))
      while (remain.nonEmpty) {
        chunk = remain.take(CODE_WINDOW)
        remain = remain.drop(CODE_WINDOW)
        open(src"object block${blockID}Chunker$chunkID {")
          open(src"def gen(): Map[String, Any] = {")
          chunk.foreach{s => visit(s) }
          val live = chunk.filter(isLive)
          emit("Map[String,Any](" + live.map{s => src""""$s" -> $s""" }.mkString(", ") + ")")
          scoped ++= live.map{s => s -> src"""block${blockID}chunk$chunkID("$s").asInstanceOf[${s.tp}]"""}
          close("}")
        close("}")
        emit(src"val block${blockID}chunk$chunkID: Map[String, Any] = block${blockID}Chunker$chunkID.gen()")
        chunkID += 1
      }
    }
    if (withReturn) emit(src"${b.result}")
  }

  def emitPreMain(): Unit = {
    inGen(out, s"IOModule.$ext") {
      emit ("package accel")
      emit ("import chisel3._")
      emit ("import chisel3.util._")
      emit ("import fringe._")
      emit ("import utils.implicits._")
      emit ("import fringe.templates.math._")
      emit ("import fringe.templates.counters._")
      emit ("import fringe.templates.memory._")
      emit ("import fringe.templates.retiming._")

      emit ("class CustomAccelInterface(")
      emit ("  io_w: Int, ")
      emit ("  io_v: Int, ")
      emit ("  io_loadStreamInfo: List[StreamParInfo], ")
      emit ("  io_storeStreamInfo: List[StreamParInfo], ")
      emit ("  io_numAllocators: Int, ")
      emit ("  io_numArgIns: Int, ")
      emit ("  io_numArgOuts: Int, ")
      emit ("  io_numArgOutLoopbacks: Int")
      emit (") extends AccelInterface{")
      emit ("  // Control IO")
      emit ("  val enable = Input(Bool())")
      emit ("  val done = Output(Bool())")
      emit ("  val reset = Input(Bool())")
      emit ("  ")
      emit ("  // DRAM IO")
      emit ("  val memStreams = Flipped(new AppStreams(io_loadStreamInfo, io_storeStreamInfo))")
      emit ("  ")
      emit ("  // HEAP IO")
      emit ("  val heap = Flipped(new HeapIO(io_numAllocators))")
      emit ("  ")
      emit ("  // Scalar IO")
      emit ("  val argIns = Input(Vec(io_numArgIns, UInt(64.W)))")
      emit ("  val argOuts = Vec(io_numArgOuts, Decoupled((UInt(64.W))))")
      emit ("  val argOutLoopbacks = Input(Vec(io_numArgOutLoopbacks, UInt(64.W)))")
      emit ("}")
      
      open("trait IOModule extends Module {")
     emit (s"""val io_w = if ("${spatialConfig.target.name}" == "VCS" || "${spatialConfig.target.name}" == "ASIC") 8 else 32 // TODO: How to generate these properly?""")
     emit (s"""val io_v = if ("${spatialConfig.target.name}" == "VCS" || "${spatialConfig.target.name}" == "ASIC") 64 else 16 // TODO: How to generate these properly?""")
    }

    inGen(out, "Instantiator.scala") {
      emit("package top")
      emit("")
      emit("import accel._")
      emit("import fringe._")
      emit("import chisel3.core.Module")
      emit("import chisel3._")
      emit("import chisel3.util._")
      emit("import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}")
      emit("")
      emit("import scala.collection.mutable.ListBuffer")

      emit("/**")
      emit(" * Top test harness")
      emit(" */")
      open("class TopUnitTester(c: Top)(implicit args: Array[String]) extends ArgsTester(c) {")
      close("}")
      emit("")
      open("object Instantiator extends CommonMain {")
        emit("type DUTType = Top")
        emit("")
        open("def dut = () => {")

    }

    inGen(out, "Controllers.scala"){
      emitHeader()

      open(s"abstract trait SMObject {")
        emit("val en = Wire(Bool())")
        emit("val done = Wire(Bool())")
        emit("val baseEn = Wire(Bool())")
        emit("val iiDone = Wire(Bool())")
        emit("val flow = Wire(Bool())")
        emit("val mask = Wire(Bool())")
        emit("val resetMe = Wire(Bool())")
        emit("val resetChildren = Wire(Bool())")
        emit("val datapathEn = Wire(Bool())")
        emit("val doneCondition = Wire(Bool())")
        emit("val sm: GeneralControl")
        emit("val children = List[SMObject]()")
        emit("val parent: Option[(SMObject, Int)]")
        emit("")
        open("def configure(): Unit = {")
          emit("sm.io.flow := flow")
          emit("sm.io.rst := resetMe")
          emit("done := sm.io.done")
          emit("sm.io.enable := en")
          emit("resetChildren := sm.io.ctrRst")
          emit("children.zipWithIndex.foreach{case (c, i) => c.baseEn := sm.io.enableOut(i); c.sm.io.parentAck := sm.io.childAck(i)}")
          emit("parent.foreach{case(p, idx) => p.sm.io.doneIn(idx) := done; p.sm.io.maskIn(idx) := mask}")
        close("}")

      close("}")
    }

    inGen(out, "ArgInterface.scala"){
      emit("""package accel""")
      emit("import fringe.templates._")
      emit("import fringe.utils._")
      emit("import fringe.utils.implicits._")
      emit("import fringe.templates.math._")
      emit("import fringe.templates.counters._")
      emit("import fringe.templates.memory._")
      emit("import fringe.templates.retiming._")
      emit("import api._")
      emit("import chisel3._")
      emit("import chisel3.util._")
      emit("import Args._")
      emit("import scala.collection.immutable._")
      open("object Args {")
    }

    inGen(out, "CounterChains.scala"){
      emitHeader()

      open(s"abstract trait CChainObject {")
        emit("val done = Wire(Bool())")
        emit("val en = Wire(Bool())")
        emit("val reset = Wire(Bool())")
        emit("val strides: List[FixedPoint]")
        emit("val stops: List[FixedPoint]")
        emit("val starts: List[FixedPoint]")
        emit("val cchain: CounterChain")
        emit("")
        open("def configure(): Unit = {")
          emit("cchain.io.input.stops.zip(stops).foreach{case (port,stop) => port := stop.r.asSInt}")
          emit("cchain.io.input.strides.zip(strides).foreach{case (port,stride) => port := stride.r.asSInt}")
          emit("cchain.io.input.starts.zip(starts).foreach{case (port,start) => port := start.r.asSInt}")
          emit("cchain.io.input.saturate := true.B")
          emit("cchain.io.input.enable := en")
          emit("done := cchain.io.output.done")
          emit("cchain.io.input.reset := reset")
        close("}")
      close("}")
    }

  }
  def emitPostMain(): Unit = {

    inGen(out, s"IOModule.$ext") {
      emit ("// Combine values")
      emit ("val io_numArgIns = scala.math.max(1, io_numArgIns_reg + io_numArgIns_mem + io_numArgIOs_reg)")
      emit ("val io_numArgOuts = scala.math.max(1, io_numArgOuts_reg + io_numArgIOs_reg + io_numArgOuts_instr + io_numArgOuts_breakpts)")
      emit ("val io_numArgOutLoopbacks = scala.math.max(1, io_argOutLoopbacksMap.toList.length)")
      emit ("val io_numArgIOs = io_numArgIOs_reg")
      emit ("val io_numArgInstrs = io_numArgOuts_instr")
      emit ("val io_numArgBreakpts = io_numArgOuts_breakpts")
      emit ("globals.numArgIns = io_numArgIns")
      emit ("globals.numArgOuts = io_numArgOuts")
      emit ("globals.numArgIOs = io_numArgIOs")
      emit ("globals.numArgInstrs = io_numArgInstrs")
      emit ("globals.argOutLoopbacksMap = io_argOutLoopbacksMap")
      emit ("globals.loadStreamInfo = io_loadStreamInfo")
      emit ("globals.storeStreamInfo = io_storeStreamInfo")
      emit ("globals.streamInsInfo = io_streamInsInfo")
      emit ("globals.streamOutsInfo = io_streamOutsInfo")
      emit ("globals.numAllocators = io_numAllocators")

      open("val io = IO(new CustomAccelInterface(io_w, io_v, globals.LOAD_STREAMS, globals.STORE_STREAMS, globals.numAllocators, io_numArgIns, io_numArgOuts, io_numArgOutLoopbacks))")
      emit ("var outStreamMuxMap: scala.collection.mutable.Map[String, Int] = scala.collection.mutable.Map[String,Int]()")
      open("def getStreamOutLane(id: String): Int = {")
        emit ("val lane = outStreamMuxMap.getOrElse(id, 0)")
        emit ("outStreamMuxMap += (id -> {lane + 1})")
        emit ("lane")
      close("}")
      emit ("var outBuffMuxMap: scala.collection.mutable.Map[String, Int] = scala.collection.mutable.Map[String,Int]()")
      open("def getBuffOutLane(id: String): Int = {")
        emit ("val lane = outBuffMuxMap.getOrElse(id, 0)")
        emit ("outBuffMuxMap += (id -> {lane + 1})")
        emit ("lane")
      close("}")
      emit ("var inStreamMuxMap: scala.collection.mutable.Map[String, Int] = scala.collection.mutable.Map[String,Int]()")
      open("def getStreamInLane(id: String): Int = {")
        emit ("val lane = inStreamMuxMap.getOrElse(id, 0)")
        emit ("inStreamMuxMap += (id -> {lane + 1})")
        emit ("lane")
      close("}")
      close("}")
    }
    inGen(out, "Instantiator.scala") {
      emit (s"""val w = if ("${spatialConfig.target.name}" == "zcu") 32 else if ("${spatialConfig.target.name}" == "VCS" || "${spatialConfig.target.name}" == "ASIC") 8 else 32""")
      emit ("val numArgIns = numArgIns_mem  + numArgIns_reg + numArgIOs_reg")
      emit ("val numArgOuts = numArgOuts_reg + numArgIOs_reg + numArgOuts_instr + numArgOuts_breakpts")
      emit ("val numArgIOs = numArgIOs_reg")
      emit ("val numArgInstrs = numArgOuts_instr")
      emit ("val numArgBreakpts = numArgOuts_breakpts")
      emit (s"""new Top("${spatialConfig.target.name}", () => Module(new AccelTop(w, numArgIns, numArgOuts, numArgIOs, numArgOuts_instr + numArgBreakpts, io_argOutLoopbacksMap, numAllocators, loadStreamInfo, storeStreamInfo, streamInsInfo, streamOutsInfo)))""")
      // emit ("new Top(w, numArgIns, numArgOuts, numArgIOs, numArgOuts_instr + numArgBreakpts, io_argOutLoopbacksMap, loadStreamInfo, storeStreamInfo, streamInsInfo, streamOutsInfo, globals.target)")
      close("}")
      emit ("def tester = { c: DUTType => new TopUnitTester(c) }")
      close("}")
    }
    inGen(out, "AccelTop.scala") {
      emit(s"""package accel""")
      emit("import chisel3._")
      emit("import chisel3.util._")
      emit("import fringe._")
      emit("import utils.implicits._")
      emit("import fringe.templates.math._")
      emit("import fringe.templates.counters._")
      emit("import fringe.templates.memory._")
      emit("import fringe.templates.retiming._")
      open("class AccelTop(")
        emit("val top_w: Int,")
        emit("val numArgIns: Int,")
        emit("val numArgOuts: Int,")
        emit("val numArgIOs: Int,")
        emit("val numArgInstrs: Int,")
        emit("val argOutLoopbacksMap: scala.collection.immutable.Map[Int,Int],")
        emit("val numAllocators: Int,")
        emit("val loadStreamInfo: List[StreamParInfo],")
        emit("val storeStreamInfo: List[StreamParInfo],")
        emit("val streamInsInfo: List[StreamParInfo],")
        emit("val streamOutsInfo: List[StreamParInfo]")
      closeopen(s") extends AbstractAccelTop with IOModule { ")
        emit("val retime_released_reg = RegInit(false.B)")
        emit("val retime_released = Wire(Bool())")
        emit("val rr = retime_released // Shorthand")
        emit("val accelReset = reset.toBool | io.reset")
        emit("Main.main(this)")
      close("}")
    }

    inGen(out, "ArgInterface.scala"){
      close("}")
    }

    inGen(out, "Controllers.scala"){
      emitFooter()
    }

  }

  override protected def emitEntry(block: Block[_]): Unit = {
    open(src"object Main {")
      open(src"def main(top: AccelTop): Unit = {")
        emitPreMain()
        outsideAccel{gen(block)}
        emitPostMain()
      close(src"}")
    close(src"}")
  }


  protected def forceEmit(x: String): Unit = {
    val on = config.enGen
    config.enGen = true
    emit(x)
    config.enGen = on
  }




















  var streamLines = collection.mutable.Map[String, Int]() // Map from filename number of lines it has
  var streamExtensions = collection.mutable.Map[String, Int]() // Map from filename to number of extensions it has
  val tabWidth: Int = 2
  val maxLinesPerFile = if (spatialConfig.compressWires == 0) 300 else 150
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

  // override protected def gen(b: Block[_], withReturn: Boolean = false): Unit = {
  //   inGenn(out, "RootController", ext, forceful=true) {
  //     outsideAccel{
  //       visitBlock(b)
  //     }
  //   }
  //   // if (withReturn) emit(src"${b.result}")
  // }

  override protected def quoteConst(tp: Type[_], c: Any): String = (tp,c) match {
    case (FixPtType(s,d,f), _) => c.toString + {if (d+f >= 32 && f == 0) "L" else ""} + s".FP($s, $d, $f)"
    case (FltPtType(g,e), _) => c.toString + s".FlP($g, $e)"
    case (_:Bit, c:Bool) => s"${c.value}.B"
    case (_:Text, c: String) => s"${c}"
    case _ => super.quoteConst(tp,c)
  }

  override protected def remap(tp: Type[_]): String = tp match {
    case FixPtType(s,d,f) => s"new FixedPoint($s, $d, $f)"
    // case FixPtType(s,d,f) => s"new FixedPoint($s, $d, $f)"
    case FltPtType(m,e) => s"new FloatingPoint($m, $e)"
    case BitType() => "Bool()"
    case tp: Vec[_] => src"Vec(${tp.width}, ${tp.typeArgs.head})"
    // case tp: StructType[_] => src"UInt(${bitWidth(tp)}.W)"
    case _ => super.remap(tp)
  }

  protected def arg(tp: Type[_]): String = tp match {
    case FixPtType(s,d,f) => s"FixedPoint"
    // case FixPtType(s,d,f) => s"new FixedPoint($s, $d, $f)"
    case FltPtType(m,e) => s"FloatingPoint"
    case BitType() => "Bool"
    case tp: Vec[_] => src"Vec[${arg(tp.typeArgs.head)}]"
    // case tp: StructType[_] => src"UInt(${bitWidth(tp)}.W)"
    case _ => super.remap(tp)
  }

  final protected def wireMap(x: String): String = { 
    if (spatialConfig.compressWires == 1 | spatialConfig.compressWires == 2) {
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
    } else if (rhs.contains(" FF(")) {
      val extractor = ".*FF\\([ ]*([0-9]+)[ ]*,[ ]*([0-9]+)[ ]*\\).*".r
      val extractor(d,w) = rhs
      s"${vec}ff${d}_${w}"
    } else if (rhs.contains(" W_Direct(")) {
      val extractor = ".*W_Direct\\([ ]*([0-9]+)[ ]*,[ ]*([0-9]+)[ ]*,[ ]*List\\(([0-9,]+)\\).grouped\\([0-9]+\\).toList[ ]*,[ ]*([0-9]+)[ ]*\\).*".r
      val extractor(pw,ofsW,bankWs,dW) = rhs
      val bWs = bankWs.replace(" ", "").replace(",","_")
      s"${vec}wdbar${pw}_${ofsW}_${bWs}_${dW}"
    } else if (rhs.contains(" R_Direct(")) {
      val extractor = ".*R_Direct\\([ ]*([0-9]+)[ ]*,[ ]*([0-9]+)[ ]*,[ ]*List\\(([0-9,]+)\\).grouped\\([0-9]+\\).toList[ ]*\\).*".r
      val extractor(pw,ofsW,bankWs) = rhs
      val bWs = bankWs.replace(" ", "").replace(",","_")
      s"${vec}rdbar${pw}_${ofsW}_${bWs}"
    } else if (rhs.contains(" W_XBar(")) {
      val extractor = ".*W_XBar\\([ ]*([0-9]+)[ ]*,[ ]*([0-9]+)[ ]*,[ ]*List\\(([0-9,]+)\\)[ ]*,[ ]*([0-9]+)[ ]*\\).*".r
      val extractor(pw,ofsW,bankWs,dW) = rhs
      val bWs = bankWs.replace(" ", "").replace(",","_")
      s"${vec}wxbar${pw}_${ofsW}_${bWs}_${dW}"
    } else if (rhs.contains(" R_XBar(")) {
      val extractor = ".*R_XBar\\([ ]*([0-9]+)[ ]*,[ ]*([0-9]+)[ ]*,[ ]*List\\(([0-9,]+)\\)[ ]*\\).*".r
      val extractor(pw,ofsW,bankWs) = rhs
      val bWs = bankWs.replace(" ", "").replace(",","_")
      s"${vec}rxbar${pw}_${ofsW}_${bWs}"
    } else if (rhs.contains(" RegChainPass(")) {
      val extractor = ".*RegChainPass\\([ ]*([0-9]+)[ ]*,[ ]*([0-9,]+)[ ]*\\).*".r
      val extractor(depth,width) = rhs
      s"${vec}rcp${depth}_${width}"
    } else if (rhs.contains(" InnerControl(")) {
      val extractor = ".*InnerControl\\([ ]*([a-zA-Z]+)[ ]*,[ ]*([falsetrue]+)[ ]*.*\\).*".r
      val extractor(style,fsm) = rhs
      val st = style.take(3)
      val f = fsm.replace("false", "f").replace("true", "t")
      s"${vec}inr${st}_${f}"
    } else if (rhs.contains(" OuterControl(")) {
      val extractor = ".*OuterControl\\([ ]*([a-zA-Z]+)[ ]*,[ ]*([0-9]+)[ ]*,[ ]*isFSM[ ]*=[ ]*([falsetrue]+)[ ]*.*\\).*".r
      val extractor(style,children,fsm) = rhs
      val f = fsm.replace("false", "f").replace("true", "t")
      val st = style.take(3)
      s"${vec}outr${st}_${children}_${f}"
    } else if (rhs.contains("_latency")) {
      "lat"
    } else {
      throw new Exception(s"Cannot compress ${rhs}!")
    }
  }

  // override def named(s: Sym[_], id: Int): String = s.op match {
  //   case Some(rhs) => rhs match {
  //     case _: AccelScope       => s"RootController"
  //     case DelayLine(size, data) => data match {
  //       case Const(_) => src"$data"
  //       case _ => wireMap(src"${data}_D$size" + alphaconv.getOrElse(src"${data}_D$size", ""))
  //     }
  //     case _ => wireMap(super.named(s, id))
  //   }
  //   case _ => wireMap(super.named(s, id))
  // }

  final protected def startFile(): Unit = {
      emit("""package accel""")
      emit("import fringe.templates._")
      emit("import fringe.utils._")
      emit("import fringe.utils.implicits._")
      emit("import fringe.templates.math._")
      emit("import fringe.templates.counters._")
      emit("import fringe.templates.memory._")
      emit("import fringe.templates.retiming._")
      emit("import api._")
      emit("import chisel3._")
      emit("import chisel3.util._")
      emit("import scala.collection.immutable._")
  }

  final protected def inSubGen[A](name: String, parent: String)(body: => A): Unit = { // Places body inside its own trait file and includes it at the end
    val prnts = if (inHw) List.tabulate(streamExtensions(parent)){i => src"${parent}_${i+1}"}.mkString(" with ") else ""
    emit(src"// Creating sub kernel ${name}_1")
    inGenn(out, name, ext) {
      startFile()
      open(src"""trait ${name}_1 extends ${prnts} {""")
      if (spatialConfig.compressWires == 2) { emit(src"""def method_${name}_1() {""") }
      try { body }
      finally {
        inGennAll(out, name, ext){
          emit(s"} ${if (spatialConfig.compressWires == 2) "}" else ""}")
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


  // protected def emit(x: String, forceful: Boolean = false): Unit = {
  //   val curStream = state.streamName.split("/").last
  //   val curStreamNoExt = curStream.split("\\.").dropRight(1).last
  //   val base = curStream.split("_").dropRight(1).mkString("_")
  //   val lineCount = streamLines(curStream)
  //   val on = config.enGen
  //   if (forceful) {config.enGen = true}
  //   if (config.enGen) {
  //     streamLines(curStream) += 1
  //     if (lineCount > maxLinesPerFile) {
  //       val newExt = streamExtensions(base) + 1
  //       streamExtensions += base -> newExt
  //       val newStreamString = (curStream.split("_").dropRight(1) :+ s"${newExt}").mkString("_")
  //       val newStream = getOrCreateStream(out, newStreamString + "." + ext)
  //       streamLines += {newStreamString + "." + ext} -> 0
  //       state.gen = newStream
  //       startFile()
  //       open(src"""trait ${newStreamString} extends ${curStreamNoExt} {""")
  //       if (spatialConfig.compressWires == 2) { emit(src"""def method_${newStreamString}() {""") }
  //     }
  //   }
  //   emit(x)
  //   config.enGen = on
  // }

  final protected def emitGlobalWire(x: String, forceful: Boolean = false): Unit = {
    inGenn(out, "GlobalWires", ext) {
      val on = config.enGen
      if (forceful) {config.enGen = true}
      emit(x)
      config.enGen = on
    }
  }

  final protected def emitGlobalWireMap(lhs: String, rhs: String, forceful: Boolean = false): Unit = {
    val module_type = rhs.replace("new ", "newnbsp").replace(" ", "").replace("nbsp", " ")
    if (spatialConfig.compressWires == 1 | spatialConfig.compressWires == 2) {
      if (!compressorMap.contains(lhs)) {
        val id = compressorMap.values.map(_._1).filter(_ == module_type).size
        compressorMap += (lhs -> (module_type, id))
      }
    } else {
      if (compressorMap.contains(lhs)) {
        emitGlobalWire(src"// val $lhs = $rhs already emited", forceful)
      } else {
        compressorMap += (lhs -> (module_type, 0))
        emitGlobalWire(src"val $lhs = $rhs", forceful)
      }
    }
  }

  final protected def emitGlobalRetimeMap(lhs: String, rhs: String, forceful: Boolean = false): Unit = {
    val module_type = rhs.replace(" ", "")
    if (spatialConfig.compressWires == 1 | spatialConfig.compressWires == 2) {
      // Assume _latency values only emited once
      val id = compressorMap.values.map(_._1).filter(_ == "_latency").size
      compressorMap += (lhs -> ("_latency", id))
      retimeList += rhs
    } else {
      emitGlobalWire(src"val $lhs = $rhs", forceful)
    }
  }

  final protected def emitGlobalModuleMap(lhs: String, rhs: String, forceful: Boolean = false): Unit = {
    val module_type_white = rhs.replace("new ", "newnbsp").replace(" ", "").replace("nbsp", " ")
    var rtid = "na"
    if (spatialConfig.compressWires == 1 | spatialConfig.compressWires == 2) {
      val module_type = if (module_type_white.contains("latency=")) {
        val extract = ".*,latency=lat\\(([0-9]+)\\).*".r
        val extract(x) = module_type_white
        rtid = x
        module_type_white.replace(s",latency=lat(${rtid})","")
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
        emitGlobalModule(src"// val $lhs = $rhs already emited", forceful)
      } else {
        compressorMap += (lhs -> (module_type, 0))
        emitGlobalModule(src"val $lhs = $rhs", forceful)
      }
    }
  }

  final protected def emitInstrumentation(x: String, forceful: Boolean = false): Unit = {
    inGenn(out, "Instrumentation", ext) {
      emit(x, forceful)
    }
  }

  final protected def emitGlobalModule(x: String, forceful: Boolean = false): Unit = {
    inGenn(out, "GlobalModules", ext) {
      emit(x, forceful)
    }
  }

  final protected def emitGlobalRetiming(x: String, forceful: Boolean = false): Unit = {
    inGenn(out, "GlobalRetiming", ext) {
      emit(x, forceful)
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

    // if (spatialConfig.enableDebugResources) {
    //   dependencies ::= DirDep("fringe/src", "fringe")
    // }

    super.copyDependencies(out)
  }


}
