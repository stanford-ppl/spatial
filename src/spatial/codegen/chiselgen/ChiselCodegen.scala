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

  /** Map between cchain and list of controllers it is copied for, due to stream controller logic */
  var cchainCopies = scala.collection.mutable.HashMap[Sym[_], List[Sym[_]]]()

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

  protected def bitWidth(tp: Type[_]): Int = tp match {
    case Bits(bT) => bT.nbits
    case _ => -1
  }
  protected def fracBits(tp: Type[_]): Int = tp match {
    case FixPtType(s,d,f) => f
    case _ => 0
  }

  override def emitHeader(): Unit = {
    emit("""package accel""")
    emit("import fringe._")
    emit("import fringe.templates._")
    emit("import fringe.Ledger._")
    emit("import fringe.utils._")
    emit("import fringe.utils.implicits._")
    emit("import fringe.templates.math._")
    emit("import fringe.templates.counters._")
    emit("import fringe.templates.vector._")
    emit("import fringe.templates.memory._")
    emit("import fringe.templates.memory.implicits._")
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
    def printableStms(stms: Seq[Sym[_]]): Seq[(Sym[_], Int)] = stms.collect{case x if (x.parent == Ctrl.Host) => (x, 0); case x if !x.isBroadcastAddr => (x, x.parOrElse1)}
    def isLive(s: Sym[_], remaining: Seq[Sym[_]]): Boolean = (b.result == s || remaining.exists(_.nestedInputs.contains(s)))
    def branchSfx(s: Sym[_], n: Option[String] = None): String = {if (s.isBranch) src""""${n.getOrElse(quote(s))}" -> $s.data""" else src""""${n.getOrElse(quote(s))}" -> $s"""}
    def initChunkState(): Unit = {ensigs = new scala.collection.mutable.ListBuffer[String]}
    val hierarchyDepth = (scala.math.log(1 max printableStms(b.stms).map(_._2).sum) / scala.math.log(CODE_WINDOW)).toInt
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
      emit ("  val heap = Flipped(Vec(io_numAllocators, new HeapIO()))")
      emit ("  ")
      emit ("  // Scalar IO")
      emit ("  val argIns = Input(Vec(io_numArgIns, UInt(64.W)))")
      emit ("  val argOuts = Vec(io_numArgOuts, new ArgOut())")
      emit ("")
      emit ("  override def cloneType = (new CustomAccelInterface(io_w, io_v, io_loadStreamInfo, io_storeStreamInfo, io_gatherStreamInfo, io_scatterStreamInfo, io_numAllocators, io_numArgIns, io_numArgOuts)).asInstanceOf[this.type] // See chisel3 bug 358")
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
      emit("""class InputKernelSignals(val depth: Int, val ctrcopies: Int, val ctrPars: List[Int], val ctrWidths: List[Int]) extends Bundle{ // From outside to inside kernel module""")
      emit("""  val done = Bool()              // my sm -> parent sm + insides""")
      emit("""  val mask = Bool()              // my cchain -> parent sm + insides""")
      emit("""  val iiDone = Bool()            // my iiCtr -> my cchain + insides""")
      emit("""  val ctrDone = Bool()           // my sm -> my cchain + insides""")
      emit("""  val backpressure = Bool()      // parent kernel -> my insides""")
      emit("""  val forwardpressure = Bool()   // parent kernel -> my insides""")
      emit("""  val datapathEn = Bool()        // my sm -> insides""")
      emit("""  val baseEn = Bool()""")
      emit("""  val break = Bool()        """)
      emit("""  val smState = SInt(32.W)        """)
      emit("""  val smEnableOuts = Vec(depth, Bool())""")
      emit("""  val smSelectsOut = Vec(depth, Bool())""")
      emit("""  val smChildAcks = Vec(depth, Bool())""")
      emit("""  val cchainOutputs = Vec(ctrcopies, new CChainOutput(ctrPars, ctrWidths))""")
      emit("""}""")
      emit("""class OutputKernelSignals(val depth: Int, val ctrcopies: Int) extends Bundle{ // From inside to outside kernel module""")
      emit("""  val smDoneIn = Vec(depth, Bool())""")
      emit("""  val smMaskIn = Vec(depth, Bool())""")
      emit("""  val smNextState = SInt(32.W)""")
      emit("""  val smInitState = SInt(32.W)""")
      emit("""  val smDoneCondition = Bool()""")      
      emit("""  val cchainEnable = Vec(ctrcopies, Bool())""")
      emit("""  val smCtrCopyDone = Vec(ctrcopies, Bool())""")
      emit("""}""")
      emit("""abstract class Kernel(val parent: Option[Kernel], val cchain: List[CounterChainInterface], val childId: Int, val nMyChildren: Int, ctrcopies: Int, ctrPars: List[Int], ctrWidths: List[Int]) {""")
      emit("""  val sigsIn = Wire(new InputKernelSignals(nMyChildren, ctrcopies, ctrPars, ctrWidths)); sigsIn := DontCare""")
      emit("""  val sigsOut = Wire(new OutputKernelSignals(nMyChildren, ctrcopies)); sigsOut := DontCare""")
      emit("""  def done = sigsIn.done""")
      emit("""  def smEnableOuts = sigsIn.smEnableOuts""")
      emit("""  def smEnableOut(i: Int) = sigsIn.smEnableOuts(i)""")
      emit("""  def mask = sigsIn.mask""")
      emit("""  def baseEn = sigsIn.baseEn""")
      emit("""  def iiDone = sigsIn.iiDone""")
      emit("""  def backpressure = sigsIn.backpressure""")
      emit("""  def forwardpressure = sigsIn.forwardpressure""")
      emit("""  def datapathEn = sigsIn.datapathEn""")
      emit("""  val resetChildren = Wire(Bool()); resetChildren := DontCare""")
      emit("""  val en = Wire(Bool()); en := DontCare""")
      emit("""  val resetMe = Wire(Bool()); resetMe := DontCare""")
      emit("""  val parentAck = Wire(Bool()); parentAck := DontCare""")
      emit("""  val sm: GeneralControl""")
      emit("""  val iiCtr: IICounter""")
      emit(""" """)
      emit("""class InstrBundle(isStream: Boolean) {""")
      emit(""" """)
      emit(""" """)
      emit("""""")
      emit("""}""")
      emit("""  def configure(n: String, ifaceSigsIn: Option[InputKernelSignals], ifaceSigsOut: Option[OutputKernelSignals], isSwitchCase: Boolean = false): Unit = {""")
      emit("""    cchain.zip(sigsIn.cchainOutputs).foreach{case (cc, sc) => sc := cc.output}""")
      emit("""    sigsIn.smSelectsOut.zip(sm.io.selectsOut).foreach{case (si, sm) => si := sm}""")
      emit("""    sigsIn.ctrDone := sm.io.ctrDone""")
      emit("""    sigsIn.smState := sm.io.state""")
      emit("""    sm.io.nextState := sigsOut.smNextState""")
      emit("""    sm.io.initState := sigsOut.smInitState""")
      emit("""    sm.io.doneCondition := sigsOut.smDoneCondition""")
      emit("""    sigsIn.smEnableOuts.zip(sm.io.enableOut).foreach{case (l,r) => l := r}""")
      emit("""    sigsIn.smChildAcks.zip(sm.io.childAck).foreach{case (l,r) => l := r}""")
      emit("""    sm.io.doneIn.zip(sigsOut.smDoneIn).foreach{case (sm, i) => sm := i}""")
      emit("""    sm.io.maskIn.zip(sigsOut.smMaskIn).foreach{case (sm, i) => sm := i}""")
      emit("""    cchain.zip(sigsOut.cchainEnable).foreach{case (c,e) => c.input.enable := e}""")
      emit("""    sm.io.backpressure := backpressure""")
      emit("""    sm.io.backpressure := backpressure""")
      emit("""    sm.io.rst := resetMe""")
      emit("""    done := sm.io.done""")
      emit("""    sigsIn.break := sm.io.break""")
      emit("""    en := baseEn & forwardpressure""")
      emit("""    if (!isSwitchCase) ifaceSigsIn.foreach{si => baseEn := si.smEnableOuts(childId).D(1) && ~done.D(1)}""")
      emit("""    parentAck := {if (ifaceSigsIn.isDefined) ifaceSigsIn.get.smChildAcks(childId) else false.B}""")
      emit("""    sm.io.enable := en""")
      emit("""    resetChildren := sm.io.ctrRst""")
      emit("""    sm.io.parentAck := parentAck""")
      emit("""    sigsIn.suggestName(n + "_sigsIn")""")
      emit("""    sigsOut.suggestName(n + "_sigsOut")""")
      emit("""    en.suggestName(n + "_en")""")
      emit("""    done.suggestName(n + "_done")""")
      emit("""    baseEn.suggestName(n + "_baseEn")""")
      emit("""    iiDone.suggestName(n + "_iiDone")""")
      emit("""    backpressure.suggestName(n + "_flow")""")
      emit("""    forwardpressure.suggestName(n + "_flow")""")
      emit("""    mask.suggestName(n + "_mask")""")
      emit("""    resetMe.suggestName(n + "_resetMe")""")
      emit("""    resetChildren.suggestName(n + "_resetChildren")""")
      emit("""    datapathEn.suggestName(n + "_datapathEn")""")
      emit("""    ifaceSigsOut.foreach{so => so.smDoneIn(childId) := done; so.smMaskIn(childId) := mask}""")
      emit("""    datapathEn := sm.io.datapathEn & mask & {if (cchain.isEmpty) true.B else ~sm.io.ctrDone} """)
      emit("""    iiCtr.io.input.enable := datapathEn; iiCtr.io.input.reset := sm.io.rst | sm.io.parentAck; iiDone := iiCtr.io.output.done | ~mask""")
      emit("""    cchain.foreach{case c => c.input.enable := sm.io.ctrInc & iiDone & forwardpressure; c.input.reset := resetChildren}""")
      emit("""    if (sm.p.sched == Streaming) {""")
      emit("""      sm.io.ctrCopyDone.zip(sigsOut.smCtrCopyDone).foreach{case (sm, so) => sm := so}""")
      emit("""      if (cchain.nonEmpty) {""")
      emit("""        sigsOut.smCtrCopyDone.zip(cchain).foreach{case (ccd, cc) => ccd := cc.output.done}""")
      emit("""        cchain.zip(sigsOut.cchainEnable).foreach{case (cc, ce) => cc.input.enable := ce }""")
      emit("""      }""")
      emit("""    }""")
      emit("""    if (parent.isDefined && parent.get.sm.p.sched == Streaming && parent.get.cchain.nonEmpty) {ifaceSigsOut.foreach{so => so.cchainEnable(childId) := done}}""")
      emit("""    else if (parent.isDefined && parent.get.sm.p.sched == Streaming) {{ifaceSigsOut.foreach{so => so.smCtrCopyDone(childId) := done}}}""")
      emit("""  }""")
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
      emit ("val io_numArgIOs = io_numArgIOs_reg")
      emit ("val io_numArgInstrs = io_numArgOuts_instr")
      emit ("val io_numArgBreakpts = io_numArgOuts_breakpts")
      emit ("globals.numArgIns = io_numArgIns")
      emit ("globals.numArgOuts = io_numArgOuts")
      emit ("globals.numArgIOs = io_numArgIOs")
      emit ("globals.numArgInstrs = io_numArgInstrs")
      emit ("globals.loadStreamInfo = io_loadStreamInfo")
      emit ("globals.storeStreamInfo = io_storeStreamInfo")
      emit ("globals.gatherStreamInfo = io_gatherStreamInfo")
      emit ("globals.scatterStreamInfo = io_scatterStreamInfo")
      emit ("globals.streamInsInfo = io_streamInsInfo")
      emit ("globals.streamOutsInfo = io_streamOutsInfo")
      emit ("globals.numAllocators = io_numAllocators")

      open("val io = IO(new CustomAccelInterface(io_w, io_v, globals.LOAD_STREAMS, globals.STORE_STREAMS, globals.GATHER_STREAMS, globals.SCATTER_STREAMS, globals.numAllocators, io_numArgIns, io_numArgOuts))")
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
      emit (s"""new Top(this.target, () => Module(new AccelTop(w, numArgIns, numArgOuts, numArgIOs, numArgOuts_instr + numArgBreakpts, numAllocators, loadStreamInfo, storeStreamInfo, gatherStreamInfo, scatterStreamInfo, streamInsInfo, streamOutsInfo)))""")
      // emit ("new Top(w, numArgIns, numArgOuts, numArgIOs, numArgOuts_instr + numArgBreakpts, loadStreamInfo, storeStreamInfo, streamInsInfo, streamOutsInfo, globals.target)")
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

  protected def arg(tp: Type[_], node: Option[Sym[_]] = None): String = tp match {
    case FixPtType(s,d,f) => s"FixedPoint"
    case _: Var[_] => "String"
    case FltPtType(m,e) => s"FloatingPoint"
    case BitType() => "Bool"
    case tp: Vec[_] => src"Vec[${arg(tp.typeArgs.head)}]"
    case _: Struct[_] => s"UInt"
    // case tp: StructType[_] => src"UInt(${bitWidth(tp)}.W)"
    case _ => node match {
      case Some(x) if x.isNBuffered => "NBufInterface"
      case Some(Op(_: ArgInNew[_])) => "UInt"
      case Some(Op(_: ArgOutNew[_])) => "MultiArgOut"
      case Some(Op(_: HostIONew[_])) => "MultiArgOut"
      case Some(Op(_: CounterNew[_])) => "CtrObject"
      case Some(Op(_: CounterChainNew)) => "CounterChainInterface"
      case Some(Op(x: RegNew[_])) if (node.get.optimizedRegType.isDefined && node.get.optimizedRegType.get == AccumFMA) => "FixFMAAccumBundle"
      case Some(Op(x: RegNew[_])) if (node.get.optimizedRegType.isDefined) => "FixOpAccumBundle"
      case Some(Op(_: RegNew[_])) => "StandardInterface"
      case Some(Op(_: RegFileNew[_,_])) => "ShiftRegFileInterface"
      case Some(Op(_: LUTNew[_,_])) => "StandardInterface"
      case Some(Op(_: SRAMNew[_,_])) => "StandardInterface"
      case Some(Op(_: FIFONew[_])) => "FIFOInterface"
      case Some(Op(_: FIFORegNew[_])) => "FIFOInterface"
      case Some(Op(_: MergeBufferNew[_])) => "MergeBufferFullIO"
      case Some(Op(_: LIFONew[_])) => "FIFOInterface"
      case Some(Op(_: DRAMHostNew[_,_])) => "FixedPoint"
      case Some(Op(_: DRAMAccelNew[_,_])) => "DRAMAllocator"
      case Some(Op(_@StreamInNew(bus))) => 
        bus match {
          case _: BurstDataBus[_] => "DecoupledIO[AppLoadData]"
          case BurstAckBus => "DecoupledIO[Bool]"
          case _: GatherDataBus[_] => "DecoupledIO[Vec[UInt]]"
          case ScatterAckBus => "DecoupledIO[Bool]"
          case _ => super.remap(tp)
        }
      case Some(x@Op(_@StreamOutNew(bus))) => 
        bus match {
          case BurstCmdBus => "DecoupledIO[AppCommandDense]"
          case _: BurstFullDataBus[_] => "DecoupledIO[AppStoreData]"
          case GatherAddrBus => "DecoupledIO[AppCommandSparse]"
          case _: ScatterCmdBus[_] => "DecoupledIO[ScatterCmdStream]"
          case _ => super.remap(tp)
        }

      case _ => super.remap(tp)
    }
  }

  protected def port(tp: Type[_], node: Option[Sym[_]] = None): String = tp match {
    case FixPtType(s,d,f) => s"Input(${remap(tp)})"
    case _: Var[_] => "String"
    case FltPtType(m,e) => s"Input(${remap(tp)})"
    case BitType() => "Input(Bool())"
    case tp: Vec[_] => src"Vec(${tp.width},${port(tp.typeArgs.head)})"
    case _: Struct[_] => s"Input(UInt(${bitWidth(tp)}))"
    // case tp: StructType[_] => src"UInt(${bitWidth(tp)}.W)"
    case _ => node match {
      case Some(x) if x.isNBuffered => src"Flipped(new NBufInterface(${x}_p))"
      case Some(Op(_: ArgInNew[_])) => "Input(UInt(64.W))"
      case Some(x@Op(_: ArgOutNew[_])) => s"new MultiArgOut(${scala.math.max(1,x.writers.filter(_.parent != Ctrl.Host).size)})"
      case Some(x@Op(_: HostIONew[_])) => s"new MultiArgOut(${scala.math.max(1,x.writers.filter(_.parent != Ctrl.Host).size)})"
      case Some(Op(_: CounterNew[_])) => "CtrObject"
      case Some(x@Op(_: CounterChainNew)) => src"Flipped(new CounterChainInterface(${x}_p))"
      case Some(x@Op(_: RegNew[_])) if (node.get.optimizedRegType.isDefined && node.get.optimizedRegType.get == AccumFMA) => 
        val FixPtType(s,d,f) = x.tp.typeArgs.head
        src"Flipped(new FixFMAAccumBundle(${x.writers.size}, $d, $f))"
      case Some(x@Op(_: RegNew[_])) if (node.get.optimizedRegType.isDefined) => 
        val FixPtType(s,d,f) = x.tp.typeArgs.head
        src"Flipped(new FixOpAccumBundle(${x.writers.size}, $d, $f))"
      case Some(x@Op(_: RegNew[_])) => src"Flipped(new StandardInterface(${x}_p))"
      case Some(x@Op(_: RegFileNew[_,_])) => src"Flipped(new ShiftRegFileInterface(${x}_p))"
      case Some(x@Op(_: LUTNew[_,_])) => src"Flipped(new StandardInterface(${x}_p))"
      case Some(x@Op(_: SRAMNew[_,_])) => src"Flipped(new StandardInterface(${x}_p))"
      case Some(x@Op(_: FIFONew[_])) => src"Flipped(new FIFOInterface(${x}_p))"
      case Some(x@Op(_: FIFORegNew[_])) => src"Flipped(new FIFOInterface(${x}_p))"
      case Some(x@Op(_: MergeBufferNew[_])) => s"Flipped(new MergeBufferFullIO(${x}_p)"
      case Some(x@Op(_: LIFONew[_])) => src"Flipped(new FIFOInterface(${x}_p))"
      case Some(x@Op(_: DRAMHostNew[_,_])) => "Input(new FixedPoint(true, 64, 0))"
      case Some(x@Op(_: DRAMAccelNew[_,_])) => "DRAMAllocator"
      case Some(x@Op(_@StreamInNew(bus))) => 
        bus match {
          case _: BurstDataBus[_] => s"Flipped(Decoupled(new AppLoadData(${x}_p)))"
          case BurstAckBus => "Flipped(Decoupled(Bool()))"
          case _: GatherDataBus[_] => "Flipped(Decoupled(Vec(0,UInt)))"
          case ScatterAckBus => "Flipped(Decoupled(Bool()))"
          case _ => super.remap(tp)
        }
      case Some(x@Op(_@StreamOutNew(bus))) => 
        bus match {
          case BurstCmdBus => s"Decoupled(new AppCommandDense(${x}_p))"
          case _: BurstFullDataBus[_] => s"Decoupled(new AppStoreData(${x}_p))"
          case GatherAddrBus => s"Decoupled(new AppCommandSparse(${x}_p))"
          case _: ScatterCmdBus[_] => "Decoupled(new ScatterCmdStream)"
          case _ => super.remap(tp)
        }

      case _ => super.remap(tp)
    }
  }

  protected def param(node: Sym[_]): Option[String] = node match {
    case x if x.isNBuffered => Some(src"$x.p")
    case Op(_: MergeBufferNew[_]) => Some(src"($node.ways, $node.par, $node.bitWidth, $node.readers)")
    case x if x.isMemPrimitive => Some(src"$x.p")
    case x if x.isCounterChain => 
      val sfx = if (cchainCopies.contains(x)) src"_copy${cchainCopies(x).head}" else ""
      Some(src"(${x}${sfx}.par, ${x}${sfx}.widths)")
    case x@Op(_@StreamInNew(bus)) => 
      bus match {
        case _: BurstDataBus[_] => Some(src"($x.bits.v, $x.bits.w)")
        case _ => None
      }
    case x@Op(_@StreamOutNew(bus)) => 
      bus match {
        case BurstCmdBus => Some(src"($x.bits.addrWidth, $x.bits.sizeWidth)")
        case _: BurstFullDataBus[_] => Some(src"($x.bits.v, $x.bits.w)")
        case GatherAddrBus => Some(src"($x.bits.v, $x.bits.scatterWidth)")
        case _: ScatterCmdBus[_] => Some(src"$x.bits.p")
        case _ => None
      }

    case _ => None
  }
  // Check if this node has an io that will be partially connected in each controller
  protected def subset(node: Sym[_]): Boolean = node match {
    case _ if node.isMem & !node.isArgIn & !node.isDRAM & !node.isStreamIn & !node.isStreamOut => true
    case _ => false
  }


}
