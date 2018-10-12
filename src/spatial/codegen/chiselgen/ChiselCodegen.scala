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

  protected val scoped: mutable.Map[Sym[_],String] = new mutable.HashMap[Sym[_],String]()
  private var globalBlockID: Int = 0
  protected var chunking: Boolean = false
  protected var ensigs = new scala.collection.mutable.ListBuffer[String]
  protected var noscope = false

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

    // If we are emitting for an object file (Counter, Controller, Mem, etc.), do not look up val in "scoped"
    if (!noscope) scoped.getOrElse(s, name) else name
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

  /** Returns list of stms that are not in a broadcast path, and the "weight" of the stm */
  protected def printableStms(stms: Seq[Sym[_]]): Seq[(Sym[_], Int)] = stms.filter(!_.isBroadcastAddr).map{x => (x, x.parOrElse1)}
  protected def willChunk(b: Block[_]*): Boolean = printableStms(b.toSeq.flatMap(_.stms)).map(_._2).sum >= CODE_WINDOW
  protected def weightedWindow(l: List[Int], limit: Int): Int = {
    @annotation.tailrec
    def take0(list: List[Int], accList: List[Int], accSum: Int) : List[Int] =
      list match {
        case h :: t if accSum + h < limit =>  
          take0(t, h :: accList, h + accSum)
        case _ => accList
      }
    take0(l, Nil, 0).size
  }

  /** Because chisel gen emits all counters when it visits the CounterChainNew, we need to 
    * forward the nestedInputs of the CounterNews through to the CounterChainNew.  
    * TODO: Generate CounterNew and CounterChainNew properly so this isn't a thing
    */
  protected def nestedInputsCtr(s: Sym[_]): Set[Sym[_]] = s match {
    case Op(CounterChainNew(ctrs)) => s.nestedInputs ++ ctrs.flatMap(_.nestedInputs).toSet
    case _ => s.nestedInputs
  }

  override protected def gen(b: Block[_], withReturn: Boolean = false): Unit = {
    if (!willChunk(b)) {
      visitBlock(b)
    }
    else {
      val stmsToEmit = printableStms(b.stms)
      globalBlockID += 1
      // TODO: More hierarchy? What if the block is > CODE_WINDOW * CODE_WINDOW size?
      val blockID: Int = globalBlockID
      var chunkID: Int = 0
      var chunk: Seq[(Sym[_], Int)] = Nil
      var remain: Seq[(Sym[_], Int)] = stmsToEmit
      // TODO: Other ways to speed this up?
      // Memories are always global in scala generation right now
      def isLive(s: Sym[_]): Boolean = !s.isMem && !s.isCounterChain && !s.isCounter && (b.result == s || remain.exists{r => nestedInputsCtr(r._1).contains(s)})
      while (remain.nonEmpty) {
        ensigs = new scala.collection.mutable.ListBuffer[String]
        val num_stm = weightedWindow(remain.map(_._2).toList, CODE_WINDOW)
        chunk = remain.take(num_stm)
        remain = remain.drop(num_stm)
        open(src"object block${blockID}Chunker$chunkID { // ${chunk.size} nodes, ${chunk.map(_._2).sum} weight")
          open(src"def gen(): Map[String, Any] = {")
          chunk.foreach{s => visit(s._1) }
          val live = chunk.map(_._1).filter(isLive)
          emit("Map[String,Any](" + live.map{case s if (s.isBranch) => src""""$s" -> $s.data"""; case s => src""""$s" -> $s""" }.mkString(", ") + ")")
          scoped ++= live.map{s => s -> src"""block${blockID}chunk$chunkID("$s").asInstanceOf[${arg(s.tp)}]"""}
          close("}")
        close("}")
        emit(src"val block${blockID}chunk$chunkID: Map[String, Any] = block${blockID}Chunker$chunkID.gen()")
        chunkID += 1
      }
      // open("def findBool(x: String): Bool = {")
      //   Seq.tabulate(chunkID){i => 
      //     if (i == 0)           emit(src"""if (block${blockID}chunk${i}.contains(x)) block${blockID}chunk${i}(x).asInstanceOf[Bool]""")
      //     else if (i < chunkID) emit(src"""else if (block${blockID}chunk${i}.contains(x)) block${blockID}chunk${i}(x).asInstanceOf[Bool]""")
      //     else                  emit(src"""else block${blockID}chunk${i}(x).asInstanceOf[Bool]""")
      //   }
      //   emit(src"")
      // close("}")
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
        emit("val cchains = List[CChainObject]()")
        if (spatialConfig.enableInstrumentation) {
          emit(src"""val cycles = Module(new InstrumentationCounter())""")
          emit(src"""val iters = Module(new InstrumentationCounter())""")          
        }
        emit("")
        open("def configure(): Unit = {")
          emit("sm.io.flow := flow")
          emit("sm.io.rst := resetMe")
          emit("done := sm.io.done")
          emit("sm.io.enable := en")
          emit("resetChildren := sm.io.ctrRst")
          if (spatialConfig.enableInstrumentation) {
            emit(src"cycles.io.enable := en")
            emit(src"iters.io.enable := risingEdge(done)")
          }
          emit("children.zipWithIndex.foreach{case (c, i) => c.baseEn := sm.io.enableOut(i).D(1) && ~c.done.D(1); c.sm.io.parentAck := sm.io.childAck(i)}")
          emit("parent.foreach{case(p, idx) => p.sm.io.doneIn(idx) := done; p.sm.io.maskIn(idx) := mask}")
          emit("if (sm.p.sched == Streaming && cchains.nonEmpty) cchains.zipWithIndex.foreach{case (cc, i) => sm.io.ctrCopyDone(i) := cc.done; cc.reset := sm.io.ctrRst.D(1)}")
          emit("else if (sm.p.sched == Streaming) children.zipWithIndex.foreach{case (c, i) => sm.io.ctrCopyDone(i) := c.done}")
          emit("if (parent.exists{pa => pa._1.sm.p.sched == Streaming && pa._1.cchains.size > 0}) {parent.get._1.cchains(parent.get._2).en := done}")
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

  protected def arg(tp: Type[_]): String = tp match {
    case FixPtType(s,d,f) => s"FixedPoint"
    case _: Var[_] => "String"
    case FltPtType(m,e) => s"FloatingPoint"
    case BitType() => "Bool"
    case tp: Vec[_] => src"Vec[${arg(tp.typeArgs.head)}]"
    case _: Struct[_] => s"UInt"
    // case tp: StructType[_] => src"UInt(${bitWidth(tp)}.W)"
    case _ => super.remap(tp)
  }

  override def copyDependencies(out: String): Unit = {

    // if (spatialConfig.enableDebugResources) {
    //   dependencies ::= DirDep("fringe/src", "fringe")
    // }

    super.copyDependencies(out)
  }


}
