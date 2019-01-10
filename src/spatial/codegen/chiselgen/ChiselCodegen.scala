package spatial.codegen.chiselgen

import argon._
import argon.codegen.FileDependencies
import emul.{Bool, FloatPoint, FixedPoint}
import spatial.codegen.naming._
import spatial.lang._
import spatial.node._
import spatial.metadata.memory._
import spatial.metadata.control._
import spatial.metadata.access._
import spatial.util.spatialConfig
import spatial.traversal.AccelTraversal

import scala.collection.mutable


trait ChiselCodegen extends NamedCodegen with FileDependencies with AccelTraversal {
  override val lang: String = "chisel"
  override val ext: String = "scala"
  backend = "accel"
  final val CODE_WINDOW: Int = 50

  protected var globalBlockID: Int = 0
  protected var ensigs = new scala.collection.mutable.ListBuffer[String]
  protected var boreMe = new scala.collection.mutable.ListBuffer[(String, String)]

  override def named(s: Sym[_], id: Int): String = {
    val name = s.op match {
      case Some(rhs) => rhs match {
        case _: AccelScope       => s"RootController"
        case DelayLine(size, data) => data match {
          case Const(_) => src"$data"
          case _ => super.named(s, id)
        }
        case _ => super.named(s, id)
      }
      case _ => super.named(s, id)
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
    emit("import fringe.templates.vector._")
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
    /** Returns list of stms that are not in a broadcast path, and the "weight" of the stm */
    def printableStms(stms: Seq[Sym[_]]): Seq[(Sym[_], Int)] = stms.collect{case x if !x.isBroadcastAddr => (x, x.parOrElse1)}
    def isLive(s: Sym[_], remaining: Seq[Sym[_]]): Boolean = !s.isMem && !s.isCounterChain && !s.isCounter && (b.result == s || remaining.exists(_.nestedInputs.contains(s)))
    def branchSfx(s: Sym[_], n: Option[String] = None): String = {if (s.isBranch) src""""${n.getOrElse(quote(s))}" -> $s.data""" else src""""${n.getOrElse(quote(s))}" -> $s"""}
    def initChunkState(): Unit = {ensigs = new scala.collection.mutable.ListBuffer[String]}

    val hierarchyDepth = (scala.math.log(printableStms(b.stms).map(_._2).sum) / scala.math.log(CODE_WINDOW)).toInt
    globalBlockID = javaStyleChunk[Sym[_]](
      printableStms(b.stms), 
      CODE_WINDOW, 
      hierarchyDepth, 
      globalBlockID, 
      isLive, 
      branchSfx, 
      arg, 
      () => initChunkState
    )(visit _ )
    
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
      emit("import fringe.templates.vector._")
      emit ("import fringe.templates.memory._")
      emit ("import fringe.templates.retiming._")

      emit ("class CustomAccelInterface(")
      emit ("  val io_w: Int, ")
      emit ("  val io_v: Int, ")
      emit ("  val io_loadStreamInfo: List[StreamParInfo], ")
      emit ("  val io_storeStreamInfo: List[StreamParInfo], ")
      emit ("  val io_gatherStreamInfo: List[StreamParInfo], ")
      emit ("  val io_scatterStreamInfo: List[StreamParInfo], ")
      emit ("  val io_numAllocators: Int, ")
      emit ("  val io_numArgIns: Int, ")
      emit ("  val io_numArgOuts: Int, ")
      emit ("  val io_numArgOutLoopbacks: Int")
      emit (") extends AccelInterface{")
      emit ("  // Control IO")
      emit ("  val enable = Input(Bool())")
      emit ("  val done = Output(Bool())")
      emit ("  val reset = Input(Bool())")
      emit ("  ")
      emit ("  // DRAM IO")
      emit ("  val memStreams = Flipped(new AppStreams(io_loadStreamInfo, io_storeStreamInfo, io_gatherStreamInfo, io_scatterStreamInfo))")
      emit ("  ")
      emit ("  // HEAP IO")
      emit ("  val heap = Flipped(new HeapIO(io_numAllocators))")
      emit ("  ")
      emit ("  // Scalar IO")
      emit ("  val argIns = Input(Vec(io_numArgIns, UInt(64.W)))")
      emit ("  val argOuts = Vec(io_numArgOuts, Decoupled((UInt(64.W))))")
      emit ("  val argOutLoopbacks = Input(Vec(io_numArgOutLoopbacks, UInt(64.W)))")
      emit ("")
      emit ("  override def cloneType = (new CustomAccelInterface(io_w, io_v, io_loadStreamInfo, io_storeStreamInfo, io_gatherStreamInfo, io_scatterStreamInfo, io_numAllocators, io_numArgIns, io_numArgOuts, io_numArgOutLoopbacks)).asInstanceOf[this.type] // See chisel3 bug 358")
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
      emit("""abstract class Kernel(val parent: Option[Kernel], val cchain: Option[CounterChain], val childId: Int) {""")
      emit("""  val resetChildren = Wire(Bool()); resetChildren := DontCare""")
      emit("""  val done = Wire(Bool()); done := DontCare""")
      emit("""  val baseEn = Wire(Bool()); baseEn := DontCare""")
      emit("""  val en = Wire(Bool()); en := DontCare""")
      emit("""  val mask = Wire(Bool()); mask := DontCare""")
      emit("""  val resetMe = Wire(Bool()); resetMe := DontCare""")
      emit("""  val iiDone = Wire(Bool()); iiDone := DontCare""")
      emit("""  val backpressure = Wire(Bool()); backpressure := DontCare""")
      emit("""  val forwardpressure = Wire(Bool()); forwardpressure := DontCare""")
      emit("""  val datapathEn = Wire(Bool()); datapathEn := DontCare""")
      emit("""  val doneCondition = Wire(Bool()); doneCondition := DontCare""")
      emit("""  val break = Wire(Bool()); break := DontCare""")
      emit("""  val parentAck = Wire(Bool()); parentAck := DontCare""")
      emit("""  val sm: GeneralControl""")
      emit("""  val iiCtr: IICounter""")

      emit(""" """)
      emit("""  def configure(n: String): Unit = {""")
      emit("""    sm.io.backpressure := backpressure | sm.io.doneLatch""")
      emit("""    sm.io.rst := resetMe""")
      emit("""    done := sm.io.done""")
      emit("""    break := sm.io.break""")
      emit("""    en := baseEn & forwardpressure""")
      emit("""    parent.foreach{p => baseEn := p.sm.io.enableOut(childId).D(1) && ~done.D(1)}""")
      emit("""    parentAck := {if (parent.isDefined) parent.get.sm.io.childAck(childId) else false.B}""")
      emit("""    sm.io.enable := en""")
      emit("""    resetChildren := sm.io.ctrRst""")
      emit("""    sm.io.parentAck := parentAck""")
      emit("""    en.suggestName(n + "_en")""")
      emit("""    done.suggestName(n + "_done")""")
      emit("""    baseEn.suggestName(n + "_baseEn")""")
      emit("""    iiDone.suggestName(n + "_iiDone")""")
      emit("""    backpressure.suggestName(n + "_flow")""")
      emit("""    mask.suggestName(n + "_mask")""")
      emit("""    resetMe.suggestName(n + "_resetMe")""")
      emit("""    resetChildren.suggestName(n + "_resetChildren")""")
      emit("""    datapathEn.suggestName(n + "_datapathEn")""")
      emit("""    doneCondition.suggestName(n + "_doneCondition")""")
      emit("""    parent.foreach{p => p.sm.io.doneIn(childId) := done; p.sm.io.maskIn(childId) := mask}""")
      emit("""    datapathEn := sm.io.datapathEn & mask & {if (cchain.isEmpty) true.B else ~sm.io.ctrDone} """)
      emit("""    iiCtr.io.input.enable := datapathEn; iiCtr.io.input.reset := sm.io.parentAck; iiDone := iiCtr.io.output.done | ~mask""")
      emit("""    cchain.foreach{case c => c.io.input.enable := sm.io.ctrInc & iiDone & forwardpressure; c.io.input.reset := resetChildren}""")
      emit("""    // if (sm.p.sched == Streaming && cchains.nonEmpty) cchains.zipWithIndex.foreach{case (cc, i) => sm.io.ctrCopyDone(i) := cc.done; cc.reset := sm.io.ctrRst.D(1)}""")
      emit("""    // else if (sm.p.sched == Streaming) children.zipWithIndex.foreach{case (c, i) => sm.io.ctrCopyDone(i) := c.done}""")
      emit("""    // if (parent.exists{pa => pa._1.sm.p.sched == Streaming && pa._1.cchains.size > 0}) {parent.get._1.cchains(parent.get._2).en := done}""")
      emit("""  }""")
      emit("""""")
      emit("""}""")
    }

    inGen(out, "ArgInterface.scala"){
      emit("""package accel""")
      emit("import fringe.templates._")
      emit("import fringe.utils._")
      emit("import fringe.utils.implicits._")
      emit("import fringe.templates.math._")
      emit("import fringe.templates.counters._")
      emit("import fringe.templates.vector._")
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

      open("class CtrObject(")
        emit("val start: Either[Option[Int], FixedPoint],")
        emit("val stop: Either[Option[Int], FixedPoint],")
        emit("val step: Either[Option[Int], FixedPoint],")
        emit("val par: Int,")
        emit("val width: Int,")
        emit("val isForever: Boolean")
      closeopen("){")
        emit("def fixedStart: Option[Int] = start match {case Left(x) => x; case Right(x) => None}")
        emit("def fixedStop: Option[Int] = stop match {case Left(x) => x; case Right(x) => None}")
        emit("def fixedStep: Option[Int] = step match {case Left(x) => x; case Right(x) => None}")
      close("}")

      open("class CChainObject(")
        emit("val ctrs: List[CtrObject],")
        emit("val name: String")
      closeopen("){")
        emit("""val cchain = Module(new CounterChain(ctrs.map(_.par), ctrs.map(_.fixedStart), ctrs.map(_.fixedStop), ctrs.map(_.fixedStep), """ + 
                     """ctrs.map(_.isForever), ctrs.map(_.width), myName = name))""")
        emit("cchain.io <> DontCare")
        emit("cchain.io.input.stops.zip(ctrs.map(_.stop)).foreach{case (port,Right(stop)) => port := stop.r.asSInt; case (_,_) => }")
        emit("cchain.io.input.strides.zip(ctrs.map(_.step)).foreach{case (port,Right(stride)) => port := stride.r.asSInt; case (_,_) => }")
        emit("cchain.io.input.starts.zip(ctrs.map(_.start)).foreach{case (port,Right(start)) => port := start.r.asSInt; case (_,_) => }")
        emit("cchain.io.input.saturate := true.B")
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
      emit ("globals.gatherStreamInfo = io_gatherStreamInfo")
      emit ("globals.scatterStreamInfo = io_scatterStreamInfo")
      emit ("globals.streamInsInfo = io_streamInsInfo")
      emit ("globals.streamOutsInfo = io_streamOutsInfo")
      emit ("globals.numAllocators = io_numAllocators")

      open("val io = IO(new CustomAccelInterface(io_w, io_v, globals.LOAD_STREAMS, globals.STORE_STREAMS, globals.GATHER_STREAMS, globals.SCATTER_STREAMS, globals.numAllocators, io_numArgIns, io_numArgOuts, io_numArgOutLoopbacks))")
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
      emit (s"""val w = if (this.target == "zcu") 32 else if (this.target == "VCS" || this.target == "ASIC") 8 else 32""")
      emit ("val numArgIns = numArgIns_mem  + numArgIns_reg + numArgIOs_reg")
      emit ("val numArgOuts = numArgOuts_reg + numArgIOs_reg + numArgOuts_instr + numArgOuts_breakpts")
      emit ("val numArgIOs = numArgIOs_reg")
      emit ("val numArgInstrs = numArgOuts_instr")
      emit ("val numArgBreakpts = numArgOuts_breakpts")
      emit (s"""new Top(this.target, () => Module(new AccelTop(w, numArgIns, numArgOuts, numArgIOs, numArgOuts_instr + numArgBreakpts, io_argOutLoopbacksMap, numAllocators, loadStreamInfo, storeStreamInfo, gatherStreamInfo, scatterStreamInfo, streamInsInfo, streamOutsInfo)))""")
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
      emit("import fringe.templates.vector._")
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
        emit("val gatherStreamInfo: List[StreamParInfo],")
        emit("val scatterStreamInfo: List[StreamParInfo],")
        emit("val streamInsInfo: List[StreamParInfo],")
        emit("val streamOutsInfo: List[StreamParInfo]")
      closeopen(s") extends AbstractAccelTop with IOModule { ")
        emit("val retime_released_reg = RegInit(false.B)")
        emit("val accelReset = reset.toBool | io.reset")
        emit("Main.main(this)")
        emit("Instrument.connect(this)")
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
        emit("top.io <> DontCare")
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


  var maxretime = 0

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

  // protected def arg(x: Sym[_]): String = arg(x.tp)

  protected def arg(tp: Type[_], node: Option[Sym[_]] = None): String = tp match {
    case FixPtType(s,d,f) => s"FixedPoint"
    case _: Var[_] => "String"
    case FltPtType(m,e) => s"FloatingPoint"
    case BitType() => "Bool"
    case tp: Vec[_] => src"Vec[${arg(tp.typeArgs.head)}]"
    case _: Struct[_] => s"UInt"
    // case tp: StructType[_] => src"UInt(${bitWidth(tp)}.W)"
    case _ => node match {
      case Some(x) if x.isNBuffered => "NBufMem"
      case Some(Op(_: ArgInNew[_])) => "UInt"
      case Some(Op(x: RegNew[_])) if (node.get.optimizedRegType.isDefined && node.get.optimizedRegType.get == AccumFMA) => "FixFMAAccum"
      case Some(Op(x: RegNew[_])) if (node.get.optimizedRegType.isDefined) => "FixOpAccum"
      case Some(Op(_: RegNew[_])) => "FF"
      case Some(Op(_: SRAMNew[_,_])) => "BankedSRAM"
      case Some(Op(_: FIFONew[_])) => "FIFO"
      case Some(Op(_: LIFONew[_])) => "LIFO"
      case Some(Op(_: DRAMHostNew[_,_])) => "DRAM"
      case Some(Op(_: DRAMAccelNew[_,_])) => "DRAM"
      case Some(Op(_: StreamOutNew[_])) => "StreamOut"
      case Some(Op(_: StreamInNew[_])) => "StreamIn"
      case _ => super.remap(tp)
    }
  }

  override def copyDependencies(out: String): Unit = {

    // if (spatialConfig.enableDebugResources) {
    //   dependencies ::= DirDep("fringe/src", "fringe")
    // }

    super.copyDependencies(out)
  }


}
