package spatial.codegen.chiselgen

import argon._
import argon.node._
import emul.FixedPoint
import spatial.lang._
import spatial.node._
import spatial.metadata.bounds._
import spatial.metadata.retiming._
import spatial.metadata.blackbox._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.util.spatialConfig

import scala.collection.mutable.HashMap

trait ChiselGenCommon extends ChiselCodegen { 

  // // List of inputs for current controller, in order to know if quote(mem) is the mem itself or the interface
  // var scopeInputs = List[Sym[_]]()

  // Mapping for DRAMs, since DRAMs and their related Transfer nodes don't necessarily appear in consistent order
  val loadStreams: HashMap[Sym[_], (String, Int)] = HashMap()
  val storeStreams: HashMap[Sym[_], (String, Int)] = HashMap()
  val gatherStreams: HashMap[Sym[_], (String, Int)] = HashMap()
  val scatterStreams: HashMap[Sym[_], (String, Int)] = HashMap()

  // Statistics counters
  var ctrls: List[Sym[_]] = List()
  val widthStats = new scala.collection.mutable.ListBuffer[Int]
  val depthStats = new scala.collection.mutable.ListBuffer[Int]
  var appPropertyStats: Set[AppProperties] = Set()

  // Interface
  var argOuts = HashMap[Sym[_], Int]()
  var argIOs = HashMap[Sym[_], Int]()
  var argIns = HashMap[Sym[_], Int]()
  var argOutLoopbacks = HashMap[Int, Int]() // info about how to wire argouts back to argins in Fringe
  var accelDrams = HashMap[Sym[_], Int]()
  var hostDrams = HashMap[Sym[_], Int]()
  /* List of break or exit nodes */
  protected var earlyExits: List[Sym[_]] = List()
  /* List of instrumentation counters and their respective depth in the hierarchy*/
  protected var instrumentCounters: List[(Sym[_], Int)] = List()

  /* Mapping between FIFO/LIFO/FIFOReg accesses and the "activity" lane they occupy" */
  protected var activesMap = HashMap[Sym[_], Int]()

  protected def instrumentCounterIndex(s: Sym[_]): Int = {
    if (spatialConfig.enableInstrumentation && instrumentCounters.nonEmpty) {
      instrumentCounters.takeWhile(_._1 != s).map{x => 
        2 + {if (hasBackPressure(x._1.toCtrl) || hasForwardPressure(x._1.toCtrl)) 2 else 0}
      }.sum
    } else 0
  }
  protected def instrumentCounterArgs(): Int = {
    if (spatialConfig.enableInstrumentation && instrumentCounters.nonEmpty) {
      val last = instrumentCounters.last._1
      instrumentCounterIndex(last) + 2 + {if (hasBackPressure(last.toCtrl) || hasForwardPressure(last.toCtrl)) 2 else 0}
    } else 0
  }

  def latencyOption(op: String, b: Option[Int]): Double = {
    if (spatialConfig.enableRetiming) {
      if (b.isDefined) {spatialConfig.target.latencyModel.exactModel(op)("b" -> b.get)("LatencyOf")}
      else spatialConfig.target.latencyModel.exactModel(op)()("LatencyOf")
    } else {
      0.0
    }
  }

  final protected def enterCtrl(lhs: Sym[_]): Sym[_] = {
    if (inHw) ctrls = ctrls :+ lhs
    val parent = if (controllerStack.isEmpty) lhs else controllerStack.head
    controllerStack.push(lhs)
    ensigs = new scala.collection.mutable.ListBuffer[String]
    if (spatialConfig.enableInstrumentation && inHw) instrumentCounters = instrumentCounters :+ (lhs, controllerStack.length)
    val cchain = if (lhs.cchains.isEmpty) "" else s"${lhs.cchains.head}"
    if (lhs.isOuterControl)      { widthStats += lhs.children.filter(_.s.get != lhs).toList.length }
    else if (lhs.isInnerControl) { depthStats += controllerStack.length }
    parent
  }

  final protected def exitCtrl(lhs: Sym[_]): Unit = {
    // Tree stuff
    controllerStack.pop()
  }

  protected def getInputs(lhs: Sym[_], func: Block[_]*): Seq[Sym[_]] = {
    // Find everything that is used in this scope
    // Only use the non-block inputs to LHS since we already account for the block inputs in nestedInputs
    val used: Set[Sym[_]] = ({lhs.nonBlockInputs.toSet ++ func.flatMap{block => block.nestedInputs } ++ lhs.readMems} &~ lhs.cchains.toSet).filterNot(_.isBlackboxImpl)
    val usedStreamsInOut: Set[Sym[_]] = RemoteMemories.all.filter{x => x.consumers.exists(_.ancestors.map(_.s).contains(Some(lhs)))}
    val usedStreamMems: Set[Sym[_]] = if (lhs.hasStreamAncestor) {getReadStreams(lhs.toCtrl) ++ getWriteStreams(lhs.toCtrl)} else Set()
    val bufMapInputs: Set[Sym[_]] = bufMapping.getOrElse(lhs, List[BufMapping]()).map{_.mem}.toSet
    val allUsed = used ++ bufMapInputs ++ usedStreamsInOut ++ usedStreamMems

    val made: Set[Sym[_]] = lhs.op.map{d => d.binds }.getOrElse(Set.empty) &~ RemoteMemories.all
    dbgs(s"Inputs for $lhs are ($used ++ $bufMapInputs ++ $usedStreamsInOut ++ $usedStreamMems) diff $made ++ ${RemoteMemories.all}")
    (allUsed diff made).filterNot{s => s.trace.isValue}.toSeq
  }

  protected def groupInputs(inss: Seq[Sym[_]]): Map[Seq[Sym[_]], String] = {
    inss.groupBy{in => arg(in.tp, Some(in))}.map{case (name, ins) => if (ins.exists(cchainCopies.contains)) ins.map(List(_) -> name) else Seq(ins -> name)}.flatten.toMap
  }

  protected def iodot: String = if (spatialConfig.enableModular) "io." else ""
  protected def dotio: String = if (spatialConfig.enableModular) ".io" else ""
  protected def cchainOutput: String = if (spatialConfig.enableModular) "io.sigsIn.cchainOutputs.head" else "cchain.head.output"
  protected def cchainCopyOutput(ii: Int): String = if (spatialConfig.enableModular) s"io.sigsIn.cchainOutputs($ii)" else s"cchain($ii).output"
  protected def ifaceType(mem: Sym[_]): String = mem match {
        case _ if mem.isNBuffered => src".asInstanceOf[NBufInterface]"
        case Op(_: RegNew[_]) if mem.optimizedRegType.isDefined && mem.optimizedRegType.get == AccumFMA => src".asInstanceOf[FixFMAAccumBundle]"
        case Op(_: RegNew[_]) if mem.optimizedRegType.isDefined => src".asInstanceOf[FixOpAccumBundle]"
        case Op(_: RegNew[_]) => src".asInstanceOf[StandardInterface]"
        case Op(_: RegFileNew[_,_]) => src".asInstanceOf[ShiftRegFileInterface]"
        case Op(_: LUTNew[_,_]) => src".asInstanceOf[StandardInterface]"
        case Op(_: SRAMNew[_,_]) => src".asInstanceOf[StandardInterface]"
        case Op(_: FIFONew[_]) => src".asInstanceOf[FIFOInterface]"
        case Op(_: FIFORegNew[_]) => src".asInstanceOf[FIFOInterface]"
        case Op(_: LIFONew[_]) => src".asInstanceOf[FIFOInterface]"
        case _ => src""
      }
  protected def datapathEn: String = s"${iodot}sigsIn.datapathEn"
  protected def break: String = s"${iodot}sigsIn.break"
  protected def done: String = s"${iodot}sigsIn.done"
  protected def baseEn: String = s"${iodot}sigsIn.baseEn"
  protected def nextState: String = s"${iodot}sigsOut.smNextState"
  protected def initState: String = s"${iodot}sigsOut.smInitState"
  protected def doneCondition: String = s"${iodot}sigsOut.smDoneCondition"  
  protected def mask: String = s"${iodot}sigsIn.mask"
  protected def ctrDone: String = s"${iodot}sigsIn.ctrDone"
  protected def iiDone: String = s"${iodot}sigsIn.iiDone"
  protected def iiIssue: String = s"${iodot}sigsIn.iiIssue"
  protected def backpressure: String = s"${iodot}sigsIn.backpressure"
  protected def forwardpressure: String = s"${iodot}sigsIn.forwardpressure"

  var argHandleMap = scala.collection.mutable.HashMap[Sym[_], String]() // Map for tracking defs of nodes and if they get redeffed anywhere, we map it to a suffix
  def argHandle(d: Sym[_]): String = {
    if (argHandleMap.contains(d)) {
      argHandleMap(d)
    } else {
      val attempted_name = d.name.getOrElse(quote(d)).toUpperCase
      if (argHandleMap.values.toList.contains(attempted_name)) {
        val taken = argHandleMap.values.toList.filter(_.contains(attempted_name))
        val given_name = attempted_name + "_dup" + taken.length
        argHandleMap += (d -> given_name)
        given_name
      } else {
        argHandleMap += (d -> attempted_name)
        attempted_name
      }
    }
  }

  final protected def quoteAsScala(x: Sym[_]): String = {
    x.rhs match {
      case Def.Const(c) => quoteConst(x.tp, c).replaceAll("\\.F.*","").replaceAll("\\.U(.*)","").replaceAll("\\.S(.*)","").replaceAll("false.B","0").replaceAll("true.B","1")
      case Def.Node(id,_) => x match {
        case Op(SimpleStruct(fields)) => 
          var shift = 0
          fields.map{ f =>
            val x = src"(${quoteAsScala(f._2)} * ${scala.math.pow(2, shift)})"
            shift = shift + bitWidth(f._2.tp)
            x
          }.mkString(" + ")
        case Op(alloc@VecAlloc(elems)) =>
          var shift = 0
          elems.map{ f =>
            val x = src"(${quoteAsScala(f.asInstanceOf[Sym[_]])} * ${scala.math.pow(2, shift)})"
            shift = shift + bitWidth(alloc.tV.A)
            x
          }.mkString(" + ")
        case _ => throw new Exception(s"Cannot quote $x as a Scala type!") 
      }
      case _ => throw new Exception(s"Cannot quote $x as a Scala type!")
    }
  }

  protected def getField(tp: Type[_],field: String): (Int,Int) = tp match {
    case x: Struct[_] =>
      val idx = x.fields.indexWhere(_._1 == field)
      val width = bitWidth(x.fields(idx)._2)
      val prec = x.fields.take(idx)
      val precBits = prec.map{case (_,bt) => bitWidth(bt)}.sum
      (precBits+width-1, precBits)
    case _ => (-1, -1)
  }

  protected def createAndTieInstrs(lhs: Sym[_]): Unit = {
    if (spatialConfig.enableInstrumentation) {
      emit(src"""val cycles_$lhs = Module(new InstrumentationCounter())""")
      emit(src"""val iters_$lhs = Module(new InstrumentationCounter())""")
      emit(src"cycles_$lhs.io.enable := $baseEn")
      emit(src"iters_$lhs.io.enable := risingEdge($done)")
      if (hasBackPressure(lhs.toCtrl) || hasForwardPressure(lhs.toCtrl)) {
        emit(src"""val stalls_$lhs = Module(new InstrumentationCounter())""")
        emit(src"""val idles_$lhs = Module(new InstrumentationCounter())""")
        emit(src"stalls_$lhs.io.enable := $baseEn & ~(${getBackPressure(lhs.toCtrl)})")
        emit(src"idles_$lhs.io.enable := $baseEn & ~(${getForwardPressure(lhs.toCtrl)})")
        emit(src"Ledger.tieInstrCtr(instrctrs.toList, ${lhs.toString.toUpperCase}_instrctr, cycles_$lhs.io.count, iters_$lhs.io.count, stalls_$lhs.io.count, idles_$lhs.io.count)")
      } else {
        emit(src"Ledger.tieInstrCtr(instrctrs.toList, ${lhs.toString.toUpperCase}_instrctr, cycles_$lhs.io.count, iters_$lhs.io.count, 0.U, 0.U)")
      }
    }
  }

  protected def createWire(name: String, payload: String): String = {
    src"""val $name = Wire($payload).suggestName(""" + "\"\"\"" + src"$name" + "\"\"\"" + ")"
  }

  // Hack for gather/scatter/unaligned load/store, we want the controller to keep running
  //   if the input FIFO is empty but not trying to dequeue, and if the output FIFO is full but
  //   not trying to enqueue
  def FIFOForwardActive(sym: Ctrl, fifo: Sym[_]): String = {
    or(fifo.readers.filter(_.parent.s.get == sym.s.get).collect{
      case a@Op(x: FIFOBankedDeq[_]) => src"$fifo.active(${activesMap(a)}).out"
      case a@Op(x: FIFOBankedPriorityDeq[_]) => src"$fifo.active(${activesMap(a)}).out"
      case a@Op(x: FIFODeqInterface[_]) => src"$fifo.active(${activesMap(a)}).out"
      case a@Op(x: FIFORegDeq[_]) => src"$fifo.active(${activesMap(a)}).out"
      case a@Op(x@FieldDeq(struct, field, ens)) => src"""$struct.getActive("$field").out"""
    })
  }

  // TODO: Not sure if FIFOBackwardActive is actually needed
  def FIFOBackwardActive(sym: Ctrl, fifo: Sym[_]): String = {
    or(fifo.writers.filter(_.parent.s.get == sym.s.get).collect{
      case a@Op(x: FIFOBankedEnq[_]) => src"$fifo.active(${activesMap(a)}).out"
      case a@Op(x: FIFODeqInterface[_]) => src"$fifo.active(${activesMap(a)}).out"
      case a@Op(x: FIFORegEnq[_]) => src"$fifo.active(${activesMap(a)}).out"
      case a@Op(x@FieldDeq(struct, field, ens)) => src"""$struct.getActive("$field").out"""
    })
  }

  def getStreamForwardPressure(c: Sym[_]): String = { 
    if (c.hasStreamAncestor || c.isInBlackboxImpl) and(getReadStreams(c.toCtrl).collect {
      case fifo @ Op(StreamInNew(bus)) => src"$fifo.valid"
    }) else "true.B"
  }

  def getStreamBackPressure(c: Sym[_]): String = { 
    if (c.hasStreamAncestor || c.isInBlackboxImpl) and(getWriteStreams(c.toCtrl).collect {
      case fifo @ Op(StreamOutNew(bus)) => src"$fifo.ready"
    }) else "true.B"
  }

  def hasForwardPressure(sym: Ctrl): Boolean = (sym.hasStreamAncestor || sym.isInBlackboxImpl) && getReadStreams(sym).nonEmpty
  def hasBackPressure(sym: Ctrl): Boolean = (sym.hasStreamAncestor || sym.isInBlackboxImpl) && getWriteStreams(sym).nonEmpty
  def getForwardPressure(sym: Ctrl): String = {
    val regularInterfaces = if (sym.hasStreamAncestor || sym.isInBlackboxImpl) and(getReadStreams(sym).collect{
      case fifo@Op(StreamInNew(bus)) => src"$fifo.valid"
      case fifo@Op(FIFONew(_)) => src"(~$fifo.empty.D(${sym.s.get.II}-1) | ~(${FIFOForwardActive(sym, fifo)}))"
      case fifo@Op(FIFORegNew(_)) => src"(~$fifo.empty.D(${sym.s.get.II}-1) | ~(${FIFOForwardActive(sym, fifo)}))"
      case merge@Op(MergeBufferNew(_,_)) => src"~$merge.output.empty.D(${sym.s.get.II}-1)"
      case bbox@Op(_:CtrlBlackboxUse[_,_]) if getUsedFields(bbox, sym).nonEmpty => src"$bbox.getForwardPressures(${getUsedFields(bbox, sym).map{x => s""""$x""""}}).D(${sym.s.get.II}-1)"
      case b if b.isBound && b.isInstanceOf[StreamStruct[_]] && getUsedFields(b, sym).nonEmpty => src"$b.getForwardPressures(${getUsedFields(b, sym).map{x => s""""$x""""}}).D(${sym.s.get.II}-1)"
    }) else "true.B"
    val priorityInterfaces = if (getReadPriorityStreams(sym).nonEmpty && (sym.hasStreamAncestor || sym.isInBlackboxImpl)) {
      and(getReadPriorityStreams(sym).map { deqgrp =>
        "(" + or(deqgrp.collect{case fifo@Op(FIFONew(_)) => src"(~$fifo.empty.D(${sym.s.get.II}-1) | ~(${FIFOForwardActive(sym, fifo)}))"}) + ")"
      })
    } else "true.B"
    s"($regularInterfaces) && ($priorityInterfaces)"
  }
  def getBackPressure(sym: Ctrl): String = {
    if (sym.hasStreamAncestor || sym.isInBlackboxImpl) and(getWriteStreams(sym).collect{
      case fifo@Op(StreamOutNew(bus)) => src"$fifo.ready"
      // case fifo@Op(FIFONew(_)) if s"${fifo.tp}".contains("IssuedCmd") => src"~${fifo}.full"
      case fifo@Op(FIFONew(_)) => src"(~$fifo.full.D(${sym.s.get.II}-1) | ~(${FIFOBackwardActive(sym, fifo)}))"
      case fifo@Op(FIFORegNew(_)) => src"(~$fifo.full.D(${sym.s.get.II}-1) | ~(${FIFOBackwardActive(sym, fifo)}))"
      case merge@Op(MergeBufferNew(_,_)) =>
        merge.writers.filter{ c => c.parent.s == sym.s }.head match {
          case enq@Op(MergeBufferBankedEnq(_, way, _, _)) =>
            src"~$merge.output.full($way).D(${sym.s.get.II}-1)"
        }
//      case bbox@Op(_:CtrlBlackboxUse[_,_]) => src"$bbox.getBackPressures(${getUsedFields(bbox, sym).map{x => s""""$x""""}}).D(${sym.s.get.II}-1)"
    }) else "true.B"
  }

  def DLTrace(lhs: Sym[_]): Option[String] = lhs match {
    case Op(DelayLine(_, data)) => DLTrace(data)
    case Const(_) => Some(quote(lhs))
    case Param(_) => Some(quote(lhs))
    case _ => None
  }

  def DL[T](name: String, latency: T, isBit: Boolean = false): String = {
    val bpressure = if (controllerStack.nonEmpty) src"$backpressure" else "true.B"
    if (isBit) src"($name).DS($latency.toInt, rr, $bpressure)"
    else src"getRetimed($name, $latency.toInt, $bpressure)"
  }

  // DL for when we are visiting children but emiting DL on signals that belong to parent
  def DLo[T](name: String, latency: T, smname: String, isBit: Boolean = false): String = {
    val bpressure = src"$smname.sm.io.backpressure"
    if (isBit) src"($name).DS($latency.toInt, rr, $bpressure)"
    else src"getRetimed($name, $latency.toInt, $bpressure)"
  }

  protected def appendSuffix(ctrl: Sym[_], y: Sym[_]): String = {
    if (ctrl.parent.s.isDefined) {
      val madeEns = ctrl.parent.s.get.op.map{d => d.binds }.getOrElse(Set.empty).filterNot(_.isCounterChain).map(quote)
      y match {
        case x if x.isBound && getSlot(ctrl) > 0 && ctrl.parent.s.get.isOuterPipeLoop & madeEns.contains(quote(x)) => src"${x}_chain_read_${getSlot(ctrl)}"
        case x if x.isBound && ctrl.parent.s.get.isOuterStreamLoop & madeEns.contains(quote(x)) => src"${x}_copy$ctrl"
        case x => src"$x" 
      }
    } else src"$y"
  }

  protected def appendSuffix(ctrl: Sym[_], y: => String): String = {
    if (ctrl.parent.s.isDefined) {
      val madeEns = ctrl.parent.s.get.op.map{d => d.binds }.getOrElse(Set.empty).filterNot(_.isCounterChain).map(quote)
      y match {
        case x if x.startsWith("b") && getSlot(ctrl) > 0 && ctrl.parent.s.get.isOuterPipeLoop & madeEns.contains(x) => src"${x}_chain_read_${getSlot(ctrl)}"
        case x if x.startsWith("b") && ctrl.parent.s.get.isOuterStreamLoop & madeEns.contains(x) => src"${x}_copy$ctrl"
        case x => src"$x" 
      }
    } else src"$y"
  }

  protected def getSlot(lhs: Sym[_]): Int = {
    lhs.siblings.filter(_.s.get != lhs.parent.s.get).map(_.s.get).indexOf(lhs)
  }
  protected def parentAndSlot(lhs: Sym[_]): String = {
    if (lhs.parent.s.isDefined) {
      val sfx = if (lhs.parent.s.get.isBranch) "_obj" else ""
      src"Some(${lhs.parent.s.get}$sfx, ${getSlot(lhs)})"
    } else "None"
  }

  override protected def quote(s: Sym[_]): String = s.rhs match {
    case Def.Bound(id)  => super.quote(s)
    case Def.Node(_,_) => DLTrace(s).getOrElse(super.quote(s))
    case _ => super.quote(s)
  }

  def and(ens: Set[Bit]): String = and(ens.map(quote).toSeq)
  def or(ens: Set[Bit]): String = or(ens.map(quote).toSeq)
  def and(ens: => Set[String]): String = and(ens.toSeq)
  def or(ens: => Set[String]): String = or(ens.toSeq)
  def and(ens: Seq[String]): String = if (ens.isEmpty) "true.B" else ens.mkString(" & ")
  def or(ens: Seq[String]): String = if (ens.isEmpty) "false.B" else ens.mkString(" | ")

  protected def createMemObject(lhs: Sym[_])(contents: => Unit): Unit = {
    inGen(out, src"m_$lhs.scala"){
      emitHeader()
      open(src"class $lhs {")
        contents
        if (param(lhs).isDefined && spatialConfig.enableModular) {
          emit(src"""ModuleParams.addParams("${lhs}_p", ${param(lhs).get})""")
        }
      close("}")
    }
    emit(src"val $lhs = (new $lhs).m.io${ifaceType(lhs)}")
  }

  protected def createBusObject(lhs: Sym[_])(contents: => Unit): Unit = {
    inGen(out, src"bus_$lhs.scala"){
      emitHeader()
      open(src"class $lhs {")
        contents
        if (param(lhs).isDefined && spatialConfig.enableModular) {
          emit(src"""ModuleParams.addParams("${lhs}_p", ${param(lhs).get})""")
        }
      close("}")
    }
    emit(src"val $lhs = new $lhs")
  }

  protected def createCtrObject(lhs: Sym[_], start: Sym[_], stop: Sym[_], step: Sym[_], par: I32, forever: Boolean): Unit = {
    // Temporary hack to effectively implement issue #288 in hardware (still need to feed this info upstream so the IR is aware of bitwidth dieting)
    // This should really check the bound on start and stop, to be more general than checking if they are constants
    val w = (start, stop) match {
      case (Const(s: FixedPoint), Const(e: FixedPoint)) => scala.math.max(utils.math.log2Up(scala.math.abs(s.toInt)), utils.math.log2Up(scala.math.abs(e.toInt))) + 2
      case _ => bitWidth(lhs.tp.typeArgs.head)
    }

    val strt = start match {
                 case _ if forever => "Left(Some(0))"
                 case Final(s) => src"Left(Some($s))"
                 case Expect(s) => src"Left(Some($s))"
                 case _ => src"Right(${appendSuffix(lhs.owner, start)})"
                }
    val stp = stop match {
                 case _ if forever => "Left(Some(5))"
                 case Final(s) => src"Left(Some($s))"
                 case Expect(s) => src"Left(Some($s))"
                 case _ => src"Right(${appendSuffix(lhs.owner, stop)})"
                }
    val ste = step match {
                 case _ if forever => "Left(Some(0))"
                 case Final(s) => src"Left(Some($s))"
                 case Expect(s) => src"Left(Some($s))"
                 case _ => src"Right(${appendSuffix(lhs.owner, step)})"
                }
    val p = par match {case Final(s) => s"$s"; case Expect(s) => s"$s"; case _ => s"$par"}
    emit(src"val $lhs = new CtrObject($strt, $stp, $ste, $p, $w, $forever)")
  }
  protected def createStreamCChainObject(lhs: Sym[_], ctrs: Seq[Sym[_]]): Unit = {
    forEachChild(lhs.owner){case (c,i) => 
      createCChainObject(lhs, ctrs, src"_copy$c")
    }
  }

  protected def createCChainObject(lhs: Sym[_], ctrs: Seq[Sym[_]], suffix: String = ""): Unit = {
    var isForever = lhs.isForever
    emit(src"""val $lhs$suffix = (new CChainObject(List[CtrObject](${ctrs.map(quote).mkString(",")}), "$lhs$suffix")).cchain.io """)
    emit(src"""$lhs$suffix.setup.isStream := ${lhs.isOuterStreamLoop}.B""")
    if (spatialConfig.enableModular) emit(src"""ModuleParams.addParams("${lhs}_p", ${param(lhs).get})""")

  }

  protected def connectDRAMStreams(dram: Sym[_]): Unit = {
    dram.loadStreams.foreach{f =>
      forceEmit(src"val ${f.addrStream} = accelUnit.io.memStreams.loads(${loadStreams.size}).cmd // StreamOut")
      if (spatialConfig.enableModular) forceEmit(src"""ModuleParams.addParams("${f.addrStream}_p", ${param(f.addrStream).get})  """)
      forceEmit(src"val ${f.dataStream} = accelUnit.io.memStreams.loads(${loadStreams.size}).data // StreamIn")
      if (spatialConfig.enableModular) forceEmit(src"""ModuleParams.addParams("${f.dataStream}_p", ${param(f.dataStream).get})  """)
      RemoteMemories += f.addrStream; RemoteMemories += f.dataStream
      val par = f.dataStream.readers.head match { case Op(e@StreamInBankedRead(strm, ens)) => ens.length }
      loadStreams += (f -> (s"""StreamParInfo(${bitWidth(dram.tp.typeArgs.head)}, $par, 0)""", loadStreams.size))
    }
    dram.storeStreams.foreach{f =>
      forceEmit(src"val ${f.addrStream} = accelUnit.io.memStreams.stores(${storeStreams.size}).cmd // StreamOut")
      if (spatialConfig.enableModular) forceEmit(src"""ModuleParams.addParams("${f.addrStream}_p", ${param(f.addrStream).get})  """)
      forceEmit(src"val ${f.dataStream} = accelUnit.io.memStreams.stores(${storeStreams.size}).data // StreamOut")
      if (spatialConfig.enableModular) forceEmit(src"""ModuleParams.addParams("${f.dataStream}_p", ${param(f.dataStream).get})  """)
      forceEmit(src"val ${f.ackStream}  = accelUnit.io.memStreams.stores(${storeStreams.size}).wresp // StreamIn")
      RemoteMemories += f.addrStream; RemoteMemories += f.dataStream; RemoteMemories += f.ackStream
      val par = f.dataStream.writers.head match { case Op(e@StreamOutBankedWrite(_, _, ens)) => ens.length }
      storeStreams += (f -> (s"""StreamParInfo(${bitWidth(dram.tp.typeArgs.head)}, $par, 0)""", storeStreams.size))
    }
    dram.gatherStreams.foreach{f =>
      forceEmit(src"val ${f.addrStream} = accelUnit.io.memStreams.gathers(${gatherStreams.size}).cmd // StreamOut")
      if (spatialConfig.enableModular) forceEmit(src"""ModuleParams.addParams("${f.addrStream}_p", ${param(f.addrStream).get})  """)
      forceEmit(src"val ${f.dataStream} = accelUnit.io.memStreams.gathers(${gatherStreams.size}).data // StreamIn")
      // if (spatialConfig.enableModular) forceEmit(src"""ModuleParams.addParams("${f.dataStream}_p", ${param(f.dataStream).get})  """)
      RemoteMemories += f.addrStream; RemoteMemories += f.dataStream
      val par = f.dataStream.readers.head match { case Op(e@StreamInBankedRead(strm, ens)) => ens.length }
      gatherStreams += (f -> (s"""StreamParInfo(${bitWidth(dram.tp.typeArgs.head)}, $par, 0)""", gatherStreams.size))
    }
    dram.scatterStreams.foreach{f =>
      forceEmit(src"val ${f.addrStream} = accelUnit.io.memStreams.scatters(${scatterStreams.size}).cmd // StreamOut")
      if (spatialConfig.enableModular) forceEmit(src"""ModuleParams.addParams("${f.addrStream}_p", ${param(f.addrStream).get})  """)
      forceEmit(src"val ${f.ackStream} = accelUnit.io.memStreams.scatters(${scatterStreams.size}).wresp // StreamOut")
      RemoteMemories += f.addrStream; RemoteMemories += f.ackStream
      val par = f.addrStream.writers.head match { case Op(e@StreamOutBankedWrite(_, _, ens)) => ens.length }
      scatterStreams += (f -> (s"""StreamParInfo(${bitWidth(dram.tp.typeArgs.head)}, $par, 0)""", scatterStreams.size))
    }
  }
}




