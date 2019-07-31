package spatial.codegen.chiselgen

import argon._
import argon.node._
import spatial.lang._
import spatial.node._
import spatial.metadata.access._
import spatial.metadata.retiming._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.types._
import spatial.util.modeling.scrubNoise
import spatial.util.spatialConfig

trait ChiselGenController extends ChiselGenCommon {

  var hwblock: Option[Sym[_]] = None
  // var outMuxMap: Map[Sym[Reg[_]], Int] = Map()
  private var memsWithReset: List[Sym[_]] = List()

  final private def enterCtrl(lhs: Sym[_]): Sym[_] = {
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

  final private def exitCtrl(lhs: Sym[_]): Unit = {
    // Tree stuff
    controllerStack.pop()
  }

  final private def connectItersAndValids(lhs: Sym[_]) = {
    val cchain = lhs.cchains.head
    val iters = lhs.toScope.iters
    val valids = lhs.toScope.valids
    val Op(CounterChainNew(counters)) = cchain
    iters.zipWithIndex.foreach{ case (iter, id) =>
      val i = cchain.constPars.zipWithIndex.map{case(_,j) => cchain.constPars.take(j+1).sum}.indexWhere(id < _)
      val w = bitWidth(counters(i).typeArgs.head)
    }
    valids.zipWithIndex.foreach{ case (v,id) => 
    }
  }

  final private def connectBufs(lhs: Sym[_]): Unit = {
    bufMapping.getOrElse(lhs, List()).foreach{case BufMapping(mem, port) => 
      emit(src"""$mem.connectStageCtrl(${DL(src"$done", 1, isBit = true)}, $baseEn, $port)""")
    }
  }
  final private def connectChains(lhs: Sym[_]): Unit = {
    regchainsMapping.getOrElse(lhs, List()).foreach{case BufMapping(mem, port) => 
      val swobj = if (lhs.isBranch) "_obj" else ""
      emit(src"""${mem}_chain.connectStageCtrl(${DLo(src"$lhs$swobj.done", 1, src"$lhs" + swobj, isBit = true)}, $lhs$swobj.baseEn, $port)""")
    }
  }

  final private def emitItersAndValids(lhs: Sym[_]) = {
    val cchain = lhs.cchains.head
    val iters = lhs.toScope.iters
    val valids = lhs.toScope.valids
    val Op(CounterChainNew(counters)) = cchain
    iters.zipWithIndex.foreach{ case (iter, id) =>
      val i = cchain.constPars.zipWithIndex.map{case(_,j) => cchain.constPars.take(j+1).sum}.indexWhere(id < _)
      val w = bitWidth(counters(i).typeArgs.head)
      emit(src"""val $iter = $cchainOutput.counts($id).FP(true, $w, 0); $iter.suggestName("$iter")""")
      if (lhs.isOuterPipeLoop && lhs.children.count(_.s.get != lhs) > 1) {
        lhs.children.filter(_.s.get != lhs).zipWithIndex.foreach{case (st, port) => 
          regchainsMapping += (st.s.get -> {regchainsMapping.getOrElse(st.s.get, List[BufMapping]()) ++ List(BufMapping(iter, port))})
        }
        emit(src"""val ${iter}_chain = Module(new RegChainPass(${lhs.children.count(_.s.get != lhs)}, $w, myName = "${iter}_chain")); ${iter}_chain.io <> DontCare""")
        emit(src"""${iter}_chain.chain_pass($iter, ${iodot}sigsOut.smDoneIn.head)""")
        forEachChild(lhs){case (c, ii) =>
          val swobj = if (c.isBranch) "_obj" else ""
          if (ii > 0) emit(src"""val ${iter}_chain_read_$ii = ${iter}_chain.read($ii).FP(true,$w,0)""")
        }
      }
    }
    valids.zipWithIndex.foreach{ case (v,id) => 
      emit(src"""val $v = ~$cchainOutput.oobs($id); $v.suggestName("$v")""")
      if (lhs.isOuterPipeLoop && lhs.children.count(_.s.get != lhs) > 1) {
        emit(src"""val ${v}_chain = Module(new RegChainPass(${lhs.children.count(_.s.get != lhs)}, 1, myName = "${v}_chain")); ${v}_chain.io <> DontCare""")
        emit(src"""${v}_chain.chain_pass($v, ${iodot}sigsOut.smDoneIn.head)""")
        lhs.children.filter(_.s.get != lhs).zipWithIndex.foreach{case (st, port) => 
          regchainsMapping += (st.s.get -> {regchainsMapping.getOrElse(st.s.get, List[BufMapping]()) ++ List(BufMapping(v, port))})
        }
        forEachChild(lhs){case (c, i) => 
          val swobj = if (c.isBranch) "_obj" else ""
          if (i > 0) emit(src"""val ${v}_chain_read_$i: Bool = ${v}_chain.read($i).apply(0)""")
        }
      }
    }
  }

  final private def emitItersAndValidsStream(lhs: Sym[_]) = {
    val cchain = lhs.cchains.head
    val iters = lhs.toScope.iters
    val valids = lhs.toScope.valids
    val Op(CounterChainNew(counters)) = cchain
    forEachChild(lhs){case (c, ii) => 
      iters.zipWithIndex.foreach{ case (iter, id) =>
        val i = cchain.constPars.zipWithIndex.map{case(_,j) => cchain.constPars.take(j+1).sum}.indexWhere(id < _)
        val w = bitWidth(counters(i).typeArgs.head)
        emit(src"val ${iter}_copy$c = ${cchainCopyOutput(ii)}.counts($id).FP(true, $w, 0)")
      }
      valids.zipWithIndex.foreach{ case (v,id) => 
        emit(src"val ${v}_copy$c = ~${cchainCopyOutput(ii)}.oobs($id)")
      }

    }
  }

  private def getInputs(lhs: Sym[_], func: Block[_]*): Seq[Sym[_]] = {
    // Find everything that is used in this scope
    // Only use the non-block inputs to LHS since we already account for the block inputs in nestedInputs
    val used: Set[Sym[_]] = {lhs.nonBlockInputs.toSet ++ func.flatMap{block => block.nestedInputs } ++ lhs.readMems} &~ lhs.cchains.toSet
    val usedStreamsInOut: Set[Sym[_]] = RemoteMemories.all.filter{x => x.consumers.exists(_.ancestors.map(_.s).contains(Some(lhs)))}
    val usedStreamMems: Set[Sym[_]] = if (lhs.hasStreamAncestor) {getReadStreams(lhs.toCtrl) ++ getWriteStreams(lhs.toCtrl)} else Set()
    val bufMapInputs: Set[Sym[_]] = bufMapping.getOrElse(lhs, List[BufMapping]()).map{_.mem}.toSet
    val allUsed = used ++ bufMapInputs ++ usedStreamsInOut ++ usedStreamMems

    val made: Set[Sym[_]] = lhs.op.map{d => d.binds }.getOrElse(Set.empty) &~ RemoteMemories.all
    dbgs(s"Inputs for $lhs are ($used ++ $bufMapInputs ++ $usedStreamsInOut ++ $usedStreamMems) diff $made ++ ${RemoteMemories.all}")
    (allUsed diff made).filterNot{s => s.trace.isValue}.toSeq    
  }

  private def groupInputs(inss: Seq[Sym[_]]): Map[Seq[Sym[_]], String] = {
    inss.groupBy{in => arg(in.tp, Some(in))}.map{case (name, ins) => if (ins.exists(cchainCopies.contains)) ins.map(List(_) -> name) else Seq(ins -> name)}.flatten.toMap
  }

  private def writeKernelClass(lhs: Sym[_], ens: Set[Bit], func: Block[_]*)(contents: => Unit): Unit = {
    val inputs: Seq[Sym[_]] = getInputs(lhs, func:_*)
    // val oldInputs = scopeInputs
    // scopeInputs = inputs.toList

    val isInner = lhs.isInnerControl
    val swobj = if (lhs.isBranch) "_obj" else ""

    dbgs(s"${stm(lhs)}")
    val chainPassedInputs = inputs.map{x => appendSuffix(lhs, x)}
    inputs.foreach{in => dbgs(s" - ${stm(in)}") }
    chainPassedInputs.foreach{in => dbgs(s" - $in") }

    val useMap = inputs.flatMap{s => scoped.get(s).map{v => s -> v}}
    scoped --= useMap.map(_._1)

    inGen(out, src"sm_$lhs.scala"){
      emitHeader()

      val ret = if (lhs.op.exists(_.R.isBits)) src"${arg(lhs.op.get.R.tp, Some(lhs))}" else "Unit"
      emit(src"/** Hierarchy: ${controllerStack.mkString(" -> ")} **/")
      emit(src"/** BEGIN ${lhs.name} $lhs **/")
      val groupedInputs = groupInputs(inputs)
      open(src"class ${lhs}_kernel(")
        groupedInputs.foreach{case (ins, typ) => 
          if (cchainCopies.contains(ins.head)) cchainCopies(ins.head).foreach{c => emit(src"${ins.head}_copy$c: ${arg(ins.head.tp, Some(ins.head))},")}
          else emit(src"list_${ins.head}: List[$typ],")
        }
        emit(s"parent: Option[Kernel], cchain: List[CounterChainInterface], childId: Int, nMyChildren: Int, ctrcopies: Int, ctrPars: List[Int], ctrWidths: List[Int], breakpoints: Vec[Bool], ${if (spatialConfig.enableInstrumentation) "instrctrs: List[InstrCtr], " else ""}rr: Bool")
        // emit(src"parent: ${if (controllerStack.size == 1) "AccelTop" else "SMObject"}")
        // emit("rr: ")
      closeopen(") extends Kernel(parent, cchain, childId, nMyChildren, ctrcopies, ctrPars, ctrWidths) {")

      createSMObject(lhs)

      if (spatialConfig.enableModular) {
        open(src"abstract class ${lhs}_module(depth: Int)(implicit stack: List[KernelHash]) extends Module {")
          open("val io = IO(new Bundle {")
            inputs.filter(!_.isString).zipWithIndex.foreach{case(in,i) => 
              if (cchainCopies.contains(in)) cchainCopies(in).foreach{c => emit(src"val in_${in}_copy$c = ${port(in.tp, Some(in))}")}
              else emit(src"val in_$in = ${port(in.tp, Some(in))}")
            }
            if (spatialConfig.enableInstrumentation) emit("val in_instrctrs = Vec(api.numCtrls, Output(new InstrCtr()))")
            val nMyChildren = lhs.children.count(_.s.get != lhs) max 1
            val ctrPars = if (lhs.cchains.nonEmpty) src"List(${lhs.cchains.head.parsOr1})" else "List(1)"
            val ctrWidths = if (lhs.cchains.nonEmpty) src"List(${lhs.cchains.head.widths})" else "List(32)"
            val ctrcopies = if (lhs.isOuterStreamControl) nMyChildren else 1
            emit(s"val in_breakpoints = Vec(api.numArgOuts_breakpts, Output(Bool()))")
            emit(s"val sigsIn = Input(new InputKernelSignals($nMyChildren, $ctrcopies, $ctrPars, $ctrWidths))")
            emit(s"val sigsOut = Output(new OutputKernelSignals($nMyChildren, $ctrcopies))")
            emit("val rr = Input(Bool())")
            if (lhs.op.exists(_.R.isBits)) emit(src"val ret = Output(${remap(lhs.op.get.R.tp)})")
          close("})")
          inputs.filter(!_.isString).zipWithIndex.foreach{case(in,i) => 
            if (cchainCopies.contains(in)) cchainCopies(in).foreach{c => emit(src"def ${in}_copy$c = {io.in_${in}_copy$c}; io.in_${in}_copy$c := DontCare")}
            else emit(src"def $in = {io.in_$in} ${if (ledgerized(in) | in.isCounterChain) src"; io.in_$in := DontCare" else ""}")
          }

        close("}")

        val numgrps = math.ceil(inputs.count(!_.isString).toDouble / 100.0)
        inputs.filter(!_.isString).grouped(100).zipWithIndex.foreach{case (inpgrp, grpid)  => 
          open(src"def connectWires$grpid(module: ${lhs}_module)(implicit stack: List[KernelHash]): Unit = {")
            inpgrp.foreach{ in => 
            if (ledgerized(in)) {
              emit(src"$in.connectLedger(module.io.in_$in)")
              if (in.isArgOut || in.isHostIO) emit(src"module.io.in_$in.port.zip($in.port).foreach{case (l,r) => l.ready := r.ready}")
              else if (in.isMergeBuffer || in.isDRAMAccel) emit(src"module.io.in_$in.output <> $in.output")
              else if (in.isBreaker) emit(src"module.io.in_$in.rPort <> $in.rPort")
            } 
            else if (cchainCopies.contains(in)) cchainCopies(in).foreach{c => emit(src"module.io.in_${in}_copy$c.input <> ${in}_copy$c.input; module.io.in_${in}_copy$c.output <> ${in}_copy$c.output")}
            else if (in.isCounterChain) emit(src"module.io.in_$in.input <> $in.input; module.io.in_$in.output <> $in.output")
            else emit(src"module.io.in_$in <> $in")}
          close("}")
        }
      }

      groupedInputs.collect{case (ins, typ) if !cchainCopies.contains(ins.head) => 
        ins.zipWithIndex.foreach{case (in, i) => emit(src"val $in = list_${ins.head}($i)")}
      }

      open(src"def kernel(): $ret = {")
        emit(src"""Ledger.enter(this.hashCode, "$lhs$swobj")""")
        emit("implicit val stack = ControllerStack.stack.toList")
        if (spatialConfig.enableModular) {
          open(src"class ${lhs}_concrete(depth: Int)(implicit stack: List[KernelHash]) extends ${lhs}_module(depth) {")
            emit("io.sigsOut := DontCare")
            emit("val breakpoints = io.in_breakpoints; breakpoints := DontCare")
            if (spatialConfig.enableInstrumentation) emit("val instrctrs = io.in_instrctrs; instrctrs := DontCare")
            emit("val rr = io.rr")
        }
        if (lhs.op.exists(_.R.isBits) && !spatialConfig.enableModular) emit(src"val ret = Wire(${remap(lhs.op.get.R.tp)})")
        if (spatialConfig.enableInstrumentation) {
          emit("""val cycles = Module(new InstrumentationCounter())""")
          emit("""val iters = Module(new InstrumentationCounter())""")          
          emit(src"cycles.io.enable := $baseEn")
          emit(src"iters.io.enable := risingEdge($done)")
          if (hasBackPressure(lhs.toCtrl) || hasForwardPressure(lhs.toCtrl)) {
            emit("""val stalls = Module(new InstrumentationCounter())""")
            emit("""val idles = Module(new InstrumentationCounter())""")          
            emit(src"stalls.io.enable := $baseEn & ~(${getBackPressure(lhs.toCtrl)})")
            emit(src"idles.io.enable := $baseEn & ~(${getForwardPressure(lhs.toCtrl)})")
            emit(src"Ledger.tieInstrCtr(instrctrs.toList, ${lhs.toString.toUpperCase}_instrctr, cycles.io.count, iters.io.count, stalls.io.count, idles.io.count)")
          } else {
            emit(src"Ledger.tieInstrCtr(instrctrs.toList, ${lhs.toString.toUpperCase}_instrctr, cycles.io.count, iters.io.count, 0.U, 0.U)")
          }
        }

        // Set up reg chains
        if (!lhs.isOuterStreamControl) {
          if (lhs.cchains.nonEmpty) {
            emitItersAndValids(lhs)
          }
        }
        else {
          if (lhs.isOuterStreamLoop) emitItersAndValidsStream(lhs)
        }

        
        // Emit body
        contents

        // Connect reg chains and buffered mems
        if (!lhs.isOuterStreamControl) {
          if (lhs.cchains.nonEmpty) {
            connectItersAndValids(lhs)
          }
        }

        connectBufs(lhs)

        if (spatialConfig.enableModular) {
          close("}")
          emit(src"val module = Module(new ${lhs}_concrete(sm.p.depth)); module.io := DontCare")
          val numgrps = math.ceil(inputs.count(!_.isString).toDouble / 100.0).toInt
          List.tabulate(numgrps){i => emit(src"connectWires$i(module)")}
          if (spatialConfig.enableInstrumentation) emit("Ledger.connectInstrCtrs(instrctrs, module.io.in_instrctrs)")
          emit(src"Ledger.connectBreakpoints(breakpoints, module.io.in_breakpoints)")
          emit("module.io.rr := rr")
          emit("module.io.sigsIn := me.sigsIn")
          emit("me.sigsOut := module.io.sigsOut")

          if (lhs.op.exists(_.R.isBits)) emit("val ret = module.io.ret")
        }
        emit("""Ledger.exit()""")
        if (lhs.op.exists(_.R.isBits)) emit("ret")
        close("}")
      close("}")
      emit(src"/** END ${lhs.op.get.name} $lhs **/")
      emitFooter()
    }
    // scopeInputs = oldInputs
    scoped ++= useMap
  }

  private def quoteCChainCopy(cchain: Sym[_], copy: Sym[_]): String = {
    if (scoped.contains(cchain)) scoped(cchain).assemble(src"_copy$copy")
    else src"${cchain}_copy$copy"
  }

  private def instantiateKernel(lhs: Sym[_], ens: Set[Bit], func: Block[_]*)(modifications: => Unit): Unit = {
    val inputs: Seq[Sym[_]] = getInputs(lhs, func:_*)

    val isInner = lhs.isInnerControl
    val swobj = if (lhs.isBranch) "_obj" else ""
    val groupedInputs = groupInputs(inputs)

    val chainPassedInputs = groupedInputs.flatMap{case (ins, typ) =>
      if (cchainCopies.contains(ins.head)) cchainCopies(ins.head).map{c => quoteCChainCopy(ins.head, c)}
      else List(ins.map{x => appendSuffix(lhs, x)}.mkString("List(", ",", ")"))
    }
    
    val parent = if (controllerStack.size == 1) "None" else "Some(me)"
    val cchain = if (lhs.cchains.nonEmpty) {
        if (lhs.isOuterStreamControl) {
          val ccs = lhs.children.filter(_.s.get != lhs).map{c => quoteCChainCopy(lhs.cchains.head, c.s.get)}
          src"List(${ccs.mkString(",")})"
        } else src"List(${lhs.cchains.head})"
      }
      else "List()"
    val childId = if (controllerStack.size == 1) -1 else s"${lhs.parent.s.get.children.filter(_.s.get != lhs.parent.s.get).map(_.s.get).indexOf(lhs)}"
    val nMyChildren = lhs.children.count(_.s.get != lhs) max 1
    val ctrcopies = if (lhs.isOuterStreamControl) nMyChildren else 1
    val ctrPars = if (lhs.cchains.nonEmpty) src"List(${lhs.cchains.head.parsOr1})" else "List(1)"
    val ctrWidths = if (lhs.cchains.nonEmpty && !lhs.cchains.head.isForever) src"List(${lhs.cchains.head.widths})" else "List(32)"
    emit(src"val $lhs$swobj = new ${lhs}_kernel($chainPassedInputs ${if (inputs.nonEmpty) "," else ""} $parent, $cchain, $childId, $nMyChildren, $ctrcopies, $ctrPars, $ctrWidths, breakpoints, ${if (spatialConfig.enableInstrumentation) "instrctrs.toList, " else ""}rr)")
    modifications
    // Wire signals to SM object
    if (!lhs.isOuterStreamControl) {
      if (lhs.cchains.nonEmpty) {
        val ctr = lhs.cchains.head
        // if (spatialConfig.enableInstrumentation && (hasBackPressure(lhs.toCtrl) || hasForwardPressure(lhs.toCtrl))) {
        //   emit(src"${lhs}$swobj.stalled.io.enable := ${lhs}$swobj.baseEn & ~(${getBackPressure(lhs.toCtrl)})")
        //   emit(src"${lhs}$swobj.idle.io.enable := ${lhs}$swobj.baseEn & ~(${getForwardPressure(lhs.toCtrl)})")
        // }
        emit(src"""$lhs$swobj.sm.io.ctrDone := ${DL(src"$lhs$swobj.cchain.head.output.done", 1, isBit = true)}""")
      } else if (lhs.isInnerControl & lhs.children.exists(_.s.get != lhs) & (lhs match {case Op(SwitchCase(_)) => true; case _ => false})) { // non terminal switch case
        val headchild = lhs.children.filter(_.s.get != lhs).head.s.get
        emit(src"""$lhs$swobj.sm.io.ctrDone := ${if (headchild.isBranch) quote(headchild) + "_obj" else quote(headchild)}.done""")
      } else if (lhs.isSwitch) { // switch, ctrDone is replaced with doneIn(#)
      } else if (lhs match {case Op(_:StateMachine[_]) if isInner && lhs.children.exists(_.s.get != lhs) => true; case _ => false }) {
        val headchild = lhs.children.filter(_.s.get != lhs).head.s.get
        emit(src"""$lhs$swobj.sm.io.ctrDone := ${if (headchild.isBranch) quote(headchild) + "_obj" else quote(headchild)}.done""")
      } else if (lhs match {case Op(_:StateMachine[_]) if isInner && !lhs.children.exists(_.s.get != lhs) => true; case _ => false }) {
        val x = lhs match {case Op(_@StateMachine(_,_,_,_,nextState)) => nextState.result; case _ => throw new Exception("Unreachable SM Logic")}
        emit(src"""$lhs$swobj.sm.io.ctrDone := $lhs$swobj.iiDone.D(${x.fullDelay})""")
      } else {
        emit(src"""$lhs$swobj.sm.io.ctrDone := risingEdge($lhs$swobj.sm.io.ctrInc)""")
      }
    }

    connectChains(lhs)

    // if (spatialConfig.enableInstrumentation && (hasBackPressure(lhs.toCtrl) || hasForwardPressure(lhs.toCtrl))) { // TBD
    //   emit(src"${lhs}$swobj.stalled.io.enable := ${lhs}$swobj.baseEn & ~(${getBackPressure(lhs.toCtrl)})")
    //   emit(src"${lhs}$swobj.idle.io.enable := ${lhs}$swobj.baseEn & ~(${getForwardPressure(lhs.toCtrl)})")
    // }
    emit(src"$lhs$swobj.backpressure := ${getBackPressure(lhs.toCtrl)} | $lhs$swobj.sm.io.doneLatch")
    emit(src"$lhs$swobj.forwardpressure := ${getForwardPressure(lhs.toCtrl)} | $lhs$swobj.sm.io.doneLatch")
    emit(src"$lhs$swobj.sm.io.enableOut.zip($lhs$swobj.smEnableOuts).foreach{case (l,r) => r := l}")

    // StreamTransformer means that parent breakWhen should also break/reset immediate child, since it becomes host of cchain
    // val parentBreak = if (lhs.parent.s.isDefined && lhs.parent.s.get.stopWhen.isDefined) src"${lhs.parent.s.get.stopWhen.get}.rPort(0).output.head.apply(0)" else "false.B"
    val myBreak = if (lhs.stopWhen.isDefined) src"${lhs.stopWhen.get}.rPort(0).output.head.apply(0); ${lhs.stopWhen.get}.connectReset($done)" else "false.B"
    emit(src"$lhs$swobj.sm.io.break := $myBreak")

    if (lhs.op.exists(_.R.isBits)) emit(createWire(quote(lhs), remap(lhs.op.head.R)))
    val suffix = if (lhs.isOuterStreamLoop) src"_copy${lhs.children.filter(_.s.get != lhs).head.s.get}" else ""
    val noop = if (lhs.cchains.nonEmpty) src"~$lhs.cchain.head.output.noop" else "true.B"
    val parentMask = and(controllerStack.head.enables.map{x => appendSuffix(lhs, x)})
    emit(src"$lhs$swobj.mask := $noop & $parentMask")

    val sigsIn = if (controllerStack.size == 1) "None" else s"Some(${iodot}sigsIn)"
    val sigsOut = if (controllerStack.size == 1) "None" else s"Some(${iodot}sigsOut)"
    emit(src"""$lhs$swobj.configure("$lhs$swobj", $sigsIn, $sigsOut, isSwitchCase = ${lhs.isSwitchCase && lhs.parent.s.isDefined && lhs.parent.s.get.isInnerControl})""")
      
    if (lhs.op.exists(_.R.isBits)) emit(src"$lhs.r := $lhs$swobj.kernel().r")
    else emit(src"$lhs$swobj.kernel()")

  }


  private def createSMObject(lhs:Sym[_]): Unit = {

    val swobj = if (lhs.isBranch) "_obj" else ""
    val isInner = lhs.isInnerControl
    val lat = if (spatialConfig.enableRetiming & lhs.isInnerControl) scrubNoise(lhs.bodyLatency.sum) else 0.0
    val ii = if (lhs.II <= 1 | !spatialConfig.enableRetiming | lhs.isOuterControl) 1.0 else scrubNoise(lhs.II)

    // Construct controller args
    val constrArg = if (lhs.isInnerControl) {s"${lhs.isFSM}"} else {s"${lhs.children.count(_.s.get != lhs)}, isFSM = ${lhs.isFSM}"}
    val isPassthrough = lhs match{
      case Op(_: Switch[_]) if isInner && lhs.parent.s.isDefined && lhs.parent.s.get.isInnerControl => ",isPassthrough = true";
      case Op(_:SwitchCase[_]) if isInner && lhs.parent.s.get.parent.s.isDefined && lhs.parent.s.get.parent.s.get.isInnerControl => ",isPassthrough = true";
      case _ => ""
    }
    val stw = lhs match{case Op(StateMachine(_,_,notDone,_,_)) => s",stateWidth = ${bitWidth(notDone.input.tp)}"; case _ => ""}
    val ncases = lhs match{
      case Op(x: Switch[_]) => s",cases = ${x.cases.length}"
      case Op(x: SwitchCase[_]) if isInner & lhs.children.exists(_.s.get != lhs) => s",cases = ${lhs.children.count(_.s.get != lhs)}"
      case Op(_: StateMachine[_]) if isInner & lhs.children.exists(_.s.get != lhs) => s", cases=${lhs.children.count(_.s.get != lhs)}"
      case _ => ""
    }

    emit("")
    emit("val me = this")
    emit(src"""val sm = Module(new ${lhs.level.toString}(${lhs.rawSchedule.toString}, ${constrArg.mkString} $stw $isPassthrough $ncases, latency = $lat.toInt, myName = "${lhs}_sm")); sm.io <> DontCare""")
    emit(src"""val iiCtr = Module(new IICounter($ii.toInt, 2 + fringe.utils.log2Up($ii.toInt), "${lhs}_iiCtr"))""")
    emit("")
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case AccelScope(func) =>
      RemoteMemories.all.collect{case x if x.isDRAMAccel =>
        val id = accelDrams.size
        accelDrams += (x -> id)
        val Op(DRAMAccelNew(dim)) = x
        val reqCount = x.consumers.collect {
          case w@Op(_: DRAMAlloc[_,_] | _: DRAMDealloc[_,_]) => w
        }.size
        connectDRAMStreams(x)
        forceEmit(src"""val $x = Module(new DRAMAllocator($dim, $reqCount)).io; $x <> DontCare""")
        forceEmit(src"top.io.heap($id).req := $x.output.heapReq")
        if (spatialConfig.enableModular) forceEmit(src"""ModuleParams.addParams("${x}_p", ${param(x).get})  """)
        forceEmit(src"$x.heapResp := top.io.heap($id).resp")
      }
      inAccel{
        emit(src"""val retime_counter = Module(new SingleCounter(1, Some(0), Some(top.max_latency), Some(1), false)); retime_counter.io <> DontCare // Counter for masking out the noise that comes out of ShiftRegister in the first few cycles of the app""")
        emit(src"""retime_counter.io.setup.saturate := true.B; retime_counter.io.input.reset := top.reset.toBool; retime_counter.io.input.enable := true.B;""")
        emit(src"""val rr = getRetimed(retime_counter.io.output.done, 1, true.B) // break up critical path by delaying this """)
        emit(src"""val breakpoints = Wire(Vec(top.io_numArgOuts_breakpts max 1, Bool())); breakpoints.zipWithIndex.foreach{case(b,i) => b.suggestName(s"breakpoint" + i)}; breakpoints := DontCare""")
        if (spatialConfig.enableInstrumentation) emit(src"""val instrctrs = List.fill[InstrCtr](api.numCtrls)(Wire(new InstrCtr()))""")
        emit(src"""val done_latch = Module(new SRFF())""")
        hwblock = Some(enterCtrl(lhs))
        instantiateKernel(lhs, Set(), func){
          emit(src"""$lhs.baseEn := top.io.enable && rr && ~done_latch.io.output""")
          emit(src"""$lhs.resetMe := getRetimed(top.accelReset, 1)""")
          emit(src"""$lhs.mask := true.B""")
          emit(src"""$lhs.sm.io.parentAck := top.io.done""")
          emit(src"""$lhs.sm.io.enable := $lhs.baseEn & !top.io.done & ${getForwardPressure(lhs.toCtrl)}""")
          emit(src"""done_latch.io.input.reset := $lhs.resetMe""")
          emit(src"""done_latch.io.input.asyn_reset := $lhs.resetMe""")
          emit(src"""top.io.done := done_latch.io.output""")
        }
        writeKernelClass(lhs, Set(), func){
          gen(func)
        }
        if (earlyExits.nonEmpty) {
          appPropertyStats += HasBreakpoint
          earlyExits.zipWithIndex.foreach{case (e, i) => 
            emit(src"top.io.argOuts(api.${quote(e).toUpperCase}_exit_arg).port.bits := 1.U")
            emit(src"top.io.argOuts(api.${quote(e).toUpperCase}_exit_arg).port.valid := breakpoints($i)")
          }
          emit(src"""done_latch.io.input.set := $lhs.done | breakpoints.reduce{_|_}""")
        } else {
          emit(src"""done_latch.io.input.set := $lhs.done""")
        }

        if (spatialConfig.enableInstrumentation) emit(src"Instrument.connect(top, instrctrs)")

        emit("Ledger.finish()")
        exitCtrl(lhs)
      }

    case ctrl: EnControl[_] if !lhs.isFSM => 
      enterCtrl(lhs)
      instantiateKernel(lhs, ctrl.ens, ctrl.bodies.flatMap{_.blocks.map(_._2)}:_*){}
      writeKernelClass(lhs, ctrl.ens, ctrl.bodies.flatMap{_.blocks.map(_._2)}:_*) {
        ctrl.bodies.flatMap{_.blocks.map(_._2)}.foreach{b => gen(b); ()}
      }
      exitCtrl(lhs)


    case op@Switch(selects, body) => 
      enterCtrl(lhs)
      val cases = lhs.children.filter(_.s.get != lhs).map(_.s.get)
      instantiateKernel(lhs, Set(), body){

        if (lhs.isInnerControl) { // If inner, don't worry about condition mutation
          selects.zipWithIndex.foreach{case (s,i) => emit(src"""${lhs}_obj.sm.io.selectsIn($i) := $s""")}
        } else { // If outer, latch in selects in case the body mutates the condition
          selects.indices.foreach{i => 
            emit(src"""val ${cases(i)}_switch_sel_reg = RegInit(false.B)""")
            emit(src"""${cases(i)}_switch_sel_reg := Mux(risingEdge(${lhs}_obj.en), ${selects(i)}, ${cases(i)}_switch_sel_reg)""")
            emit(src"""${lhs}_obj.sm.io.selectsIn($i) := ${selects(i)}""")
          }
        }
      }

      writeKernelClass(lhs, Set(), body) {
        gen(body)
        if (op.R.isBits) {
          emit(createWire(src"${lhs}_onehot_selects", src"Vec(${selects.length}, Bool())"))
          emit(createWire(src"${lhs}_data_options", src"Vec(${selects.length}, ${lhs.tp})"))
          selects.indices.foreach { i => emit(src"${lhs}_onehot_selects($i) := ${selects(i)}");emit(src"${lhs}_data_options($i) := ${cases(i)}") }
          emit(src"${iodot}ret.r := Mux1H(${lhs}_onehot_selects, ${lhs}_data_options).r")
        }
      }
      exitCtrl(lhs)


    case op@SwitchCase(body) =>
      enterCtrl(lhs)
      instantiateKernel(lhs, Set(), body){
        if (lhs.isInnerControl) {
          emit(src"""${lhs}_obj.baseEn := ${iodot}sigsIn.smSelectsOut(${lhs}_obj.childId)""")
        }
      }

      writeKernelClass(lhs, Set(), body) {
        emit(s"// Controller Stack: ${controllerStack.tail}")
        gen(body)
        if (op.R.isBits) {
          emit(src"${iodot}ret.r := ${body.result}.r")
        }
      }
      exitCtrl(lhs)

    case StateMachine(ens,start,notDone,action,nState) =>
      appPropertyStats += HasFSM
      enterCtrl(lhs)
      instantiateKernel(lhs, ens, notDone, action, nState){}
      writeKernelClass(lhs, ens, notDone, action, nState) {
        val state = notDone.input
        emit(createWire(src"$state", src"${state.tp}"))
        emit(src"$state.r := ${iodot}sigsIn.smState.r")

        gen(notDone)
        gen(action)
        gen(nState)

        emit(src"$nextState := ${nState.result}.r.asSInt ")
        emit(src"$initState := $start.r.asSInt")
        emit(src"$doneCondition := ~${notDone.result}")
      }
      exitCtrl(lhs)

    case SeriesForeach(_,_,_,blk) => 
      gen(blk)


    case _ => super.gen(lhs, rhs)
  }

  override def emitPostMain(): Unit = {

    inGen(out, "Instrument.scala"){
      emitHeader()
      open("object Instrument {")
        open("def connect(top: AccelTop, instrctrs: List[InstrCtr]): Unit = {")
          val printableLines: Seq[StmWithWeight[String]] = instrumentCounters.zipWithIndex.flatMap{case ((s,d), i) => 
            val swobj = if (s.isBranch) "_obj" else ""
            Seq(
              StmWithWeight(src"""top.io.argOuts(api.${s.toString.toUpperCase}_cycles_arg).port.bits := instrctrs(${s.toString.toUpperCase}_instrctr).cycs""",1,Seq[String]()),
              StmWithWeight(src"""top.io.argOuts(api.${s.toString.toUpperCase}_cycles_arg).port.valid := top.io.enable""",1,Seq[String]()),
              StmWithWeight(src"""top.io.argOuts(api.${s.toString.toUpperCase}_iters_arg).port.bits := instrctrs(${s.toString.toUpperCase}_instrctr).iters""",1,Seq[String]()),
              StmWithWeight(src"""top.io.argOuts(api.${s.toString.toUpperCase}_iters_arg).port.valid := top.io.enable""",1,Seq[String]())
            ) ++ {if (hasBackPressure(s.toCtrl) || hasForwardPressure(s.toCtrl)) { Seq(
                StmWithWeight(src"""top.io.argOuts(api.${s.toString.toUpperCase}_stalled_arg).port.bits := instrctrs(${s.toString.toUpperCase}_instrctr).stalls""",1,Seq[String]()),
                StmWithWeight(src"""top.io.argOuts(api.${s.toString.toUpperCase}_stalled_arg).port.valid := top.io.enable""",1,Seq[String]()),
                StmWithWeight(src"""top.io.argOuts(api.${s.toString.toUpperCase}_idle_arg).port.bits := instrctrs(${s.toString.toUpperCase}_instrctr).idles""",1,Seq[String]()),
                StmWithWeight(src"""top.io.argOuts(api.${s.toString.toUpperCase}_idle_arg).port.valid := top.io.enable""",1,Seq[String]())
              )} else Nil}

          }
          def isLive(s: String, remaining: Seq[String]): Boolean = false
          def branchswobj(s: String, n: Option[String] = None): String = src""""${n.getOrElse(quote(s))}" -> $s"""
          def initChunkState(): Unit = {}

          val hierarchyDepth = (scala.math.log(printableLines.size) / scala.math.log(CODE_WINDOW)).toInt
          globalBlockID = javaStyleChunk[String](
            printableLines, 
            CODE_WINDOW, 
            hierarchyDepth, 
            globalBlockID, 
            isLive, 
            branchswobj, 
            arg, 
            () => initChunkState()
          )(emit(_) )

          emit (s"val numArgOuts_breakpts = ${earlyExits.length}")
        close("}")
      close("}")
    }

    inGen(out, "Instantiator.scala") {
      emit ("")
      emit ("// Instrumentation")
      emit (s"val numArgOuts_instr = $instrumentCounterArgs")
      emit (s"val numCtrls = ${ctrls.size}")
      emit (s"val numArgOuts_breakpts = ${earlyExits.length}")
      emit ("""/* Breakpoint Contexts:""")
      earlyExits.zipWithIndex.foreach {case (p,i) => 
        emit (s"breakpoint $i: ${p.ctx}")
      }
      emit ("""*/""")
    }

    inGen(out, s"IOModule.$ext") {
      emit (src"// Root controller for app: ${config.name}")
      emit ("")
      emit (src"// Widths: ${widthStats.sorted}")
      emit (src"//   Widest Outer Controller: ${if (widthStats.isEmpty) 0 else widthStats.max}")
      emit (src"// Depths: ${depthStats.sorted}")
      emit (src"//   Deepest Inner Controller: ${if (depthStats.isEmpty) 0 else depthStats.max}")
      emit (s"// App Characteristics: ${appPropertyStats.toList.map(_.getClass.getName.split("\\$").last.split("\\.").last).mkString(",")}")
      emit ("// Instrumentation")
      emit (s"val io_numArgOuts_instr = $instrumentCounterArgs")
      emit (s"val io_numArgCtrls = ${ctrls.size}")
      emit (s"val io_numArgOuts_breakpts = ${earlyExits.length}")

      emit ("""// Set Build Info""")
      emit(s"val max_latency = $maxretime")

      emit (s"globals.target.fixmul_latency = ${latencyOption("FixMul", Some(1))}")
      emit (s"globals.target.fixdiv_latency = ${latencyOption("FixDiv", Some(1))}")
      emit (s"globals.target.fixadd_latency = ${latencyOption("FixAdd", Some(1))}")
      emit (s"globals.target.fixsub_latency = ${latencyOption("FixSub", Some(1))}")
      emit (s"globals.target.fixmod_latency = ${latencyOption("FixMod", Some(1))}")
      emit (s"globals.target.fixeql_latency = ${latencyOption("FixEql", None)}.toInt")
      // emit (s"tight_control   = ${spatialConfig.enableTightControl}")
      emit (s"globals.target.mux_latency    = ${latencyOption("Mux", None)}.toInt")
      emit (s"globals.target.sramload_latency    = ${latencyOption("SRAMBankedRead", None)}.toInt")
      emit (s"globals.target.sramstore_latency    = ${latencyOption("SRAMBankedWrite", None)}.toInt")
      emit (s"globals.target.SramThreshold = ${spatialConfig.sramThreshold}")
      emit (s"""globals.retime = ${spatialConfig.enableRetiming}""")
      emit (s"""globals.enableModular = ${spatialConfig.enableModular}""")

    }

    super.emitPostMain()
  }

}
