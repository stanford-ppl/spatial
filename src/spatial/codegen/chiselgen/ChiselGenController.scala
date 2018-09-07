package spatial.codegen.chiselgen

import argon._
import argon.codegen.Codegen
import spatial.lang._
import spatial.node._
import spatial.metadata.bounds._
import spatial.metadata.access._
import spatial.metadata.retiming._
import spatial.metadata.control._
import spatial.metadata.types._
import spatial.util.modeling.scrubNoise
import spatial.util.spatialConfig

trait ChiselGenController extends ChiselGenCommon {

  var hwblock: Option[Sym[_]] = None
  // var outMuxMap: Map[Sym[Reg[_]], Int] = Map()
  private var nbufs: List[(Sym[Reg[_]], Int)]  = List()
  private var memsWithReset: List[Sym[_]] = List()

  var instrumentCounters: List[(Sym[_], Int)] = List()

  /* For every iter we generate, we track the children it may be used in.
     Given that we are quoting one of these, look up if it has a map entry,
     and keep getting parents of the currentController until we find a match or 
     get to the very top
  */


  def createBreakpoint(lhs: Sym[_], id: Int): Unit = {
    emitInstrumentation(src"io.argOuts(io_numArgOuts_reg + io_numArgIOs_reg + io_numArgOuts_instr + $id).bits := 1.U")
    emitInstrumentation(src"io.argOuts(io_numArgOuts_reg + io_numArgIOs_reg + io_numArgOuts_instr + $id).valid := breakpoints($id)")
  }

  def createInstrumentation(lhs: Sym[_]): Unit = {
    if (spatialConfig.enableInstrumentation) {
      val ctx = s"${lhs.ctx}"
      emitInstrumentation(src"""// Instrumenting $lhs, context: ${ctx}, depth: ${controllerStack.length}""")
      val id = instrumentCounters.length
      if (spatialConfig.compressWires == 1 || spatialConfig.compressWires == 2) {
        emitInstrumentation(src"ic(${id*2}).io.enable := ${swap(lhs,En)}; ic(${id*2}).reset := accelReset")
        emitInstrumentation(src"ic(${id*2+1}).io.enable := Utils.risingEdge(${swap(lhs, Done)}); ic(${id*2+1}).reset := accelReset")
        emitInstrumentation(src"""io.argOuts(io_numArgOuts_reg + io_numArgIOs_reg + 2 * ${id}).bits := ic(${id*2}).io.count""")
        emitInstrumentation(src"""io.argOuts(io_numArgOuts_reg + io_numArgIOs_reg + 2 * ${id}).valid := ${swap(hwblock.get, En)}//${swap(hwblock.get, Done)}""")
        emitInstrumentation(src"""io.argOuts(io_numArgOuts_reg + io_numArgIOs_reg + 2 * ${id} + 1).bits := ic(${id*2+1}).io.count""")
        emitInstrumentation(src"""io.argOuts(io_numArgOuts_reg + io_numArgIOs_reg + 2 * ${id} + 1).valid := ${swap(hwblock.get, En)}//${swap(hwblock.get, Done)}""")        
      } else {
        emitInstrumentation(src"""val ${lhs}_cycles = Module(new InstrumentationCounter())""")
        emitInstrumentation(src"${lhs}_cycles.io.enable := ${swap(lhs,En)}; ${lhs}_cycles.reset := accelReset")
        emitInstrumentation(src"""val ${lhs}_iters = Module(new InstrumentationCounter())""")
        emitInstrumentation(src"${lhs}_iters.io.enable := Utils.risingEdge(${swap(lhs, Done)}); ${lhs}_iters.reset := accelReset")
        emitInstrumentation(src"""io.argOuts(io_numArgOuts_reg + io_numArgIOs_reg + 2 * ${id}).bits := ${lhs}_cycles.io.count""")
        emitInstrumentation(src"""io.argOuts(io_numArgOuts_reg + io_numArgIOs_reg + 2 * ${id}).valid := ${swap(hwblock.get, En)}//${swap(hwblock.get, Done)}""")
        emitInstrumentation(src"""io.argOuts(io_numArgOuts_reg + io_numArgIOs_reg + 2 * ${id} + 1).bits := ${lhs}_iters.io.count""")
        emitInstrumentation(src"""io.argOuts(io_numArgOuts_reg + io_numArgIOs_reg + 2 * ${id} + 1).valid := ${swap(hwblock.get, En)}//${swap(hwblock.get, Done)}""")        
      }
      instrumentCounters = instrumentCounters :+ (lhs, controllerStack.length)
    }
  }



  protected def forEachChild(lhs: Sym[_])(body: (Sym[_],Int) => Unit): Unit = {
    lhs.children.filter(_.s.get != lhs).zipWithIndex.foreach { case (cc, idx) =>
      val c = cc.s.get
      controllerStack.push(c)
      body(c,idx)
      controllerStack.pop()
    }
  }

  final private def enterCtrl(lhs: Sym[_]): Sym[_] = {
    val parent = if (controllerStack.isEmpty) lhs else controllerStack.head
    controllerStack.push(lhs)
    val cchain = if (lhs.cchains.isEmpty) "" else s"${lhs.cchains.head}"
    if (lhs.isOuterControl)      { widthStats += lhs.children.filter(_.s.get != lhs).toList.length }
    else if (lhs.isInnerControl) { depthStats += controllerStack.length }

    parent
  }

  final private def exitCtrl(lhs: Sym[_]): Unit = {
    // Tree stuff
    controllerStack.pop()
  }

  final private def allocateValids(lhs: Sym[_], cchain: Sym[CounterChain], iters: Seq[Seq[Sym[_]]], valids: Seq[Seq[Sym[_]]]): Unit = {
    // Need to recompute ctr data because of multifile 5
    valids.zip(iters).zipWithIndex.foreach{ case ((layer,count), i) =>
      layer.zip(count).foreach{ case (v, c) =>
        // emitGlobalWire(s"//${validPassMap}")
        emitGlobalModuleMap(src"${v}","Wire(Bool())")  
        if (lhs.isOuterPipeControl) {
          lhs.children.filter(_.s.get != lhs).indices.drop(1).foreach{i => emitGlobalModuleMap(src"""${swap(src"$v", Blank)}_chain_read_$i""", "Wire(Bool())")}
        }
      }
    }
  }

  final private def allocateRegChains(lhs: Sym[_], inds:Seq[Sym[_]], cchain:Sym[CounterChain]): Unit = {
    if (lhs.isOuterPipeControl) {
      val stages = lhs.children.filter(_.s.get != lhs).map(_.s.get)
      val Op(CounterChainNew(counters)) = cchain
      val par = counters.map(_.ctrPar)
      val ctrMapping = par.indices.map{i => par.dropRight(par.length - i).map(_.toInt).sum}
      inds.zipWithIndex.foreach { case (idx,index) =>
        val this_counter = ctrMapping.count(_ <= index) - 1
        val this_width = bitWidth(counters(this_counter).typeArgs.head)
        emitGlobalModuleMap(src"""${idx}_chain""", src"""Module(new RegChainPass(${stages.size}, ${this_width}))""")
        stages.indices.foreach{i => emitGlobalModuleMap(src"""${idx}_chain_read_$i""", src"Wire(new FixedPoint(true,${this_width},0))"); emitGlobalModule(src"""${swap(src"${idx}_chain_read_$i", Blank)} := ${swap(idx, Chain)}.read(${i})""")}
      }
    }
  }

  private final def emitRegChains(lhs: Sym[_]) = {
    if (lhs.isOuterPipeControl) {
      val stages = lhs.children.filter(_.s.get != lhs).toList.map(_.s.get)
      val counters = lhs.cchains.head.counters
      val par = lhs.cchains.head.pars
      val ctrMapping = par.indices.map{i => par.dropRight(par.length - i).map(_.toInt).sum}
      lhs.toScope.iters.toList.zipWithIndex.foreach { case (idx,index) =>
        val ctr = ctrMapping.count(_ <= index) - 1
        val w = bitWidth(counters(ctr).typeArgs.head)
        inGenn(out, "BufferControlCxns", ext) {
          stages.zipWithIndex.foreach{ case (s, i) =>
            emitGlobalWireMap(src"${s}_done", "Wire(Bool())")
            emitGlobalWireMap(src"${s}_en", "Wire(Bool())")
            emitt(src"""${swap(idx, Chain)}.connectStageCtrl(${swap(s, Done)}, ${swap(s, En)}, $i)""")
          }
        }
        emitt(src"""${swap(idx, Chain)}.chain_pass(${idx}, ${swap(lhs, SM)}.io.doneIn.head)""")
        // Associate bound sym with both ctrl node and that ctrl node's cchain
      }
    }
  }

  final private def emitValids(lhs: Sym[_], cchain: Sym[CounterChain], iters: Seq[Seq[Sym[_]]], valids: Seq[Seq[Sym[_]]]): Unit = {
    val Op(CounterChainNew(counters)) = cchain
    valids.zipWithIndex.foreach{ case (vs,id) =>
      val base = counters.take(id).map(_.ctrPar.toInt).sum
      vs.zipWithIndex.foreach{ case(v,i) =>
        emitt(src"${swap(src"${v}", Blank)} := ~${cchain}.io.output.oobs(${base} + $i)")
        if (lhs.isOuterPipeControl) {
          emitGlobalModuleMap(src"""${swap(src"${swap(src"${v}", Blank)}", Chain)}""",src"""Module(new RegChainPass(${lhs.children.filter(_.s.get != lhs).size}, 1))""")
          lhs.children.filter(_.s.get != lhs).indices.drop(1).foreach{i => emitGlobalModule(src"""${swap(src"${swap(src"${v}", Blank)}_chain_read_$i", Blank)} := ${swap(src"${swap(src"${v}", Blank)}", Chain)}.read(${i}) === 1.U(1.W)""")}
          inGenn(out, "BufferControlCxns", ext) {
            lhs.children.filter(_.s.get != lhs).zipWithIndex.foreach{ case (ss, i) =>
              val s = ss.s.get
              emitGlobalWireMap(src"${s}_done", "Wire(Bool())")
              emitGlobalWireMap(src"${s}_en", "Wire(Bool())")
              emitt(src"""${swap(src"${swap(src"${v}", Blank)}", Chain)}.connectStageCtrl(${DL(swap(s, Done), 1, true)}, ${swap(s,En)}, $i)""")
            }
          }
          emitt(src"""${swap(src"${swap(src"${v}", Blank)}", Chain)}.chain_pass(${swap(src"${v}", Blank)}, ${swap(lhs, SM)}.io.doneIn.head)""")
        }
      }
    }
  }

  protected def connectMask(ctr: Option[Sym[_]]): Unit = {
    if (ctr.isDefined) {
      val ctrl = ctr.get.owner
      emitt(src"""${swap(ctrl, Mask)} := ~${ctr.get}.io.output.noop & ${and(controllerStack.head.enables)}""")
    } else {
      emitt(src"""${swap(controllerStack.head, Mask)} := ${and(controllerStack.head.enables)}""")     
    }
  }


  final private def emitValidsDummy(iters: Seq[Seq[Sym[_]]], valids: Seq[Seq[Sym[_]]]): Unit = {
    valids.zip(iters).zipWithIndex.foreach{ case ((layer,count), i) =>
      layer.zip(count).foreach{ case (v, c) =>
        emitt(src"val ${v} = true.B")
      }
    }
  }

  final private def emitIICounter(lhs: Sym[_]): Unit = {
    if (lhs.II <= 1 | !spatialConfig.enableRetiming | lhs.isOuterControl) {
      emitt(src"""${swap(lhs, IIDone)} := true.B""")
    } else {
      emitt(src"""val ${lhs}_IICtr = Module(new IICounter(${swap(lhs, II)}, 2 + Utils.log2Up(${swap(lhs, II)})));""")
      emitt(src"""${swap(lhs, IIDone)} := ${lhs}_IICtr.io.output.done | ~${swap(lhs, Mask)}""")
      emitt(src"""${lhs}_IICtr.io.input.enable := ${swap(lhs,DatapathEn)}""")
      emitt(src"""${lhs}_IICtr.io.input.reset := accelReset | ${swap(lhs, SM)}.io.parentAck""")
    }
  }

  final private def allocateIters(iters: Seq[Seq[Sym[_]]], cchain: Sym[CounterChain]) = {
    val Op(CounterChainNew(counters)) = cchain
    iters.zipWithIndex.foreach{ case (is, i) =>
      val w = bitWidth(counters(i).typeArgs.head)
      is.zipWithIndex.foreach{ case (iter, j) =>
        emitGlobalWireMap(src"${iter}", src"Wire(new FixedPoint(true,${w},0))")
      }
    }
  }


  final private def emitIters(iters: Seq[Seq[Sym[_]]], cchain: Sym[CounterChain]) = {
    val Op(CounterChainNew(counters)) = cchain
    iters.zipWithIndex.foreach{ case (is, id) =>
      val base = counters.take(id).map(_.ctrPar.toInt).sum
      is.zipWithIndex.foreach{ case (iter, j) =>
        emitt(src"${swap(src"${iter}", Blank)}.r := ${cchain}.io.output.counts($base + $j).r")
      }
    }
  }

  protected def emitChildrenCxns(sym: Sym[_], isFSM: Boolean = false): Unit = {
    val isInner = sym.isInnerControl

    forEachChild(sym){case (c, idx) => 
      sym match {
        case Op(op@Switch(_,_)) => 
          emitt(src"""${swap(c, BaseEn)} := ${swap(sym, SM)}.io.selectsOut($idx)""")
          emitt(src"""${swap(c, En)} := ${swap(sym, SM)}.io.selectsOut($idx)""")
          emitt(src"""${swap(sym, SM)}.io.doneIn($idx) := ${swap(c, Done)}""")
        case Op(op@SwitchCase(_)) => 
          emitt(src"""${swap(c, BaseEn)} := ${swap(sym,DatapathEn)}""")
          emitt(src"""${swap(c, En)} := ${swap(sym,DatapathEn)}""")
        case _ if (isInner) => // Happens when a controller (FSM, Foreach, etc) contains a switch with inner style cases
          emitt(src"""${swap(c, En)} := ${swap(sym,DatapathEn)}""")  
          emitt(src"""${swap(sym, SM)}.io.doneIn($idx) := ${swap(c,Done)}""")  
        case _ => 
      }
      if (!isInner) emitt(src"""${swap(sym, SM)}.io.maskIn(${idx}) := ${swap(c, Mask)}""")
      emitt(src"""${swap(c, SM)}.io.parentAck := ${swap(sym, SM)}.io.childAck(${idx})""")
    }

    /* Control Signals to Children Controllers */
    if (!isInner) {
      emitt(src"""// ---- Begin ${sym.rawSchedule.toString} $sym Children Signals ----""")

      forEachChild(sym) { case (c, idx) =>

        val base_delay = if (spatialConfig.enableTightControl) 0 else 1
        emitt(src"""${swap(c, BaseEn)} := ${DLo(src"${swap(sym, SM)}.io.enableOut(${idx})", base_delay, true)} & ${DLo(src"~${swap(c, Done)}", 1, true)} & ${and(c.enables)}""")  
        emitt(src"""${swap(c, En)} := ${swap(c, BaseEn)} & ${getForwardPressure(c.toCtrl)}""")  

        // If this is a stream controller, need to set up counter copy for children
        if (sym.isOuterStreamLoop) {
          emitt(src"""// ${swap(sym, SM)}.io.parentAck := ${swap(src"${sym.cchains.head}", Done)} // Not sure why this used to be connected, since it can cause an extra command issued""")
          emitGlobalWireMap(src"""${swap(src"${sym.cchains.head}", En)}""", """Wire(Bool())""") 
          emitt(src"""${swap(src"${sym.cchains.head}", En)} := ${swap(c,Done)}""")
          emitt(src"""${swap(src"${sym.cchains.head}", Resetter)} := ${DLo(src"${swap(sym, SM)}.io.ctrRst", 1, true)}""")
          emitt(src"""${swap(sym, SM)}.io.ctrCopyDone(${idx}) := ${swap(src"${sym.cchains.head}",Done)}""")
        } else if (sym.isOuterStreamControl) {
          emitt(src"""${swap(sym, SM)}.io.ctrCopyDone(${idx}) := ${swap(c, Done)}""")
        }
        emitt(src"""${swap(sym, SM)}.io.doneIn(${idx}) := ${swap(c, Done)};""")
        if (c match { case Op(_: StateMachine[_]) => true; case _ => false}) emitt(src"""${swap(c, Resetter)} := ${DLo(src"${swap(sym, SM)}.io.ctrRst", 1, true)} | ${DLo(swap(c, Done), 1, true)}""") //changed on 12/13 // If this is an fsm, we want it to reset with each iteration, not with the reset of the parent
        else emitt(src"""${swap(c, Resetter)} := ${DLo(src"${swap(sym, SM)}.io.ctrRst", 1, true)}""")   //changed on 12/13
        
      }
    }
    /* Emit reg chains */
    emitRegChains(sym)

  }

  def emitController(sym:Sym[_], isFSM: Boolean = false): Unit = {
    val isInner = sym.isInnerControl
    val lat = if (spatialConfig.enableRetiming & sym.isInnerControl) scrubNoise(sym.bodyLatency.sum) else 0.0
    val ii = scrubNoise(sym.II)

    // Construct controller args
    emitt(src"""//  ---- ${sym.level.toString}: Begin ${sym.rawSchedule.toString} $sym Controller ----""")
    val constrArg = if (sym.isInnerControl) {s"$isFSM"} else {s"${sym.children.filter(_.s.get != sym).length}, isFSM = ${isFSM}"}
    val isPassthrough = sym match{
      case Op(_: Switch[_]) if isInner && sym.parent.s.isDefined && sym.parent.s.get.isInnerControl => ",isPassthrough = true";
      case Op(_:SwitchCase[_]) if isInner && sym.parent.s.get.parent.s.isDefined && sym.parent.s.get.parent.s.get.isInnerControl => ",isPassthrough = true";
      case _ => ""
    }
    val stw = sym match{case Op(StateMachine(_,_,notDone,_,_)) => s",stateWidth = ${bitWidth(notDone.input.tp)}"; case _ => ""}
    val ncases = sym match{
      case Op(x: Switch[_]) => s",cases = ${x.cases.length}"
      case Op(x: SwitchCase[_]) if isInner & sym.children.filter(_.s.get != sym).nonEmpty => s",cases = ${sym.children.filter(_.s.get != sym).length}"
      case Op(_: StateMachine[_]) if isInner & sym.children.filter(_.s.get != sym).nonEmpty => s", cases=${sym.children.filter(_.s.get != sym).length}"
      case _ => ""
    }

    // Generate standard control signals for all types
    emitGlobalRetimeMap(src"""${sym}_latency""", s"$lat.toInt")
    emitGlobalRetimeMap(src"""${sym}_ii""", s"$ii.toInt")

    // Emit control signals for children up front because of blk/parent distinction, a regread in an outer pipe may use control signals of a still-to-be-visited controller
    forEachChild(sym){case (c, idx) => emitControlSignals(c)}

    createInstrumentation(sym)

    // Create controller
    emitGlobalModuleMap(src"${sym}_sm", src"Module(new ${sym.level.toString}(templates.${sym.rawSchedule.toString}, ${constrArg.mkString} $stw $isPassthrough $ncases, latency = ${swap(sym, Latency)}))")

    // Connect enable and rst in (rst)
    emitt(src"""${swap(sym, SM)}.io.enable := ${swap(sym, En)} & retime_released & ${getForwardPressure(sym.toCtrl)} """)
    emitt(src"""${swap(sym, SM)}.io.rst := ${swap(sym, Resetter)} // generally set by parent""")

    //  Capture rst out (ctrRst)
    emitGlobalWireMap(src"""${swap(sym, RstEn)}""", """Wire(Bool())""") 
    emitt(src"""${swap(sym, RstEn)} := ${swap(sym, SM)}.io.ctrRst // Generally used in inner pipes""")

    // Capture sm done
    emitt(src"""${swap(sym, Done)} := ${swap(sym, SM)}.io.done // Used to delay in cg, now delay in templates""")
    emitt(src"""${swap(sym, SM)}.io.flow := ${getBackPressure(sym.toCtrl)}""")

    // Capture datapath_en
    if (sym.cchains.isEmpty) emitt(src"""${swap(sym, DatapathEn)} := ${swap(sym, SM)}.io.datapathEn & ${swap(sym, Mask)} // Used to have many variations""")
    else emitt(src"""${swap(sym, DatapathEn)} := ${swap(sym, SM)}.io.datapathEn & ${swap(sym, Mask)} & ~${swap(sym, SM)}.io.ctrDone // Used to have many variations""")

    // Update bound sym watchlists
    (sym.toScope.iters ++ sym.toScope.valids).foreach{ item =>
      if (sym.isPipeControl) pipeChainPassMap += (item -> sym.children.filter(_.s.get != sym).toList.map(_.s.get))
      else if (sym.isStreamControl) {streamCopyWatchlist = streamCopyWatchlist :+ item}
    }

    // Emit counterchain(s)
    if (sym.isOuterStreamLoop) {
      streamCopyWatchlist = streamCopyWatchlist :+ sym.cchains.head
      sym.cchains.head.counters.foreach{c => streamCopyWatchlist = streamCopyWatchlist :+ c}
      forEachChild(sym){case (c, idx) =>
        emitCounterChain(sym)
        emitt(src"""${swap(sym.cchains.head, En)} := ${swap(sym, SM)}.io.ctrInc & ${swap(sym, IIDone)} & ${getForwardPressure(sym.toCtrl)}""")
      }
    } 
    else {
      emitCounterChain(sym)  
      // Hook up ctr done
      if (sym.cchains.nonEmpty && !sym.cchains.head.isForever) {
        val ctr = sym.cchains.head
        emitt(src"""${swap(ctr, En)} := ${swap(sym, SM)}.io.ctrInc & ${swap(sym, IIDone)} & ${getForwardPressure(sym.toCtrl)}""")
        if (getReadStreams(sym.toCtrl).nonEmpty) emitt(src"""${swap(ctr, Resetter)} := ${DL(swap(sym, Done), 1, true)} // Do not use rst_en for stream kiddo""")
        emitt(src"""${swap(ctr, Resetter)} := ${swap(sym, RstEn)}""")
        emitt(src"""${swap(sym, SM)}.io.ctrDone := ${DL(swap(ctr, Done), 1, true)}""")
      } else if (sym.isInnerControl & sym.children.filter(_.s.get != sym).nonEmpty & (sym match {case Op(SwitchCase(_)) => true; case _ => false})) { // non terminal switch case
        emitt(src"""${swap(sym, SM)}.io.ctrDone := ${swap(src"${sym.children.filter(_.s.get != sym).head.s.get}", Done)}""")
      } else if (sym match {case Op(Switch(_,_)) => true; case _ => false}) { // switch, ctrDone is replaced with doneIn(#)
      } else if (sym match {case Op(_:StateMachine[_]) if (isInner && sym.children.filter(_.s.get != sym).length > 0) => true; case _ => false }) {
        emitt(src"""${swap(sym, SM)}.io.ctrDone := ${swap(sym.children.filter(_.s.get != sym).head.s.get, Done)}""")
      } else if (sym match {case Op(x:StateMachine[_]) if (isInner && sym.children.filter(_.s.get != sym).length == 0) => true; case _ => false }) {
        val x = sym match {case Op(_@StateMachine(_,_,_,_,nextState)) => nextState.result; case _ => throw new Exception("Unreachable SM Logic")}
        emitt(src"""${swap(sym, SM)}.io.ctrDone := ${swap(sym, IIDone)}.D(${x.fullDelay})""")
      } else {
        emitt(src"""${swap(sym, SM)}.io.ctrDone := Utils.risingEdge(${swap(sym, SM)}.io.ctrInc)""")
      }
    }

  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case AccelScope(func) =>
      inAccel{
        hwblock = Some(enterCtrl(lhs))
        emitControlSignals(lhs)
        emitController(lhs)
        emitGlobalWire(src"val accelReset = reset.toBool | io.reset")
        emitt(s"""${swap(lhs, BaseEn)} := io.enable""")
        emitt(s"""${swap(lhs, En)} := ${swap(lhs, BaseEn)} & !io.done & ${getForwardPressure(lhs.toCtrl)}""")
        emitt(s"""${swap(lhs, Resetter)} := Utils.getRetimed(accelReset, 1)""")
        emitt(src"""${swap(lhs, Mask)} := true.B""")
        emitGlobalWireMap(src"""${lhs}_II_done""", """Wire(Bool())""")
        emitIICounter(lhs)
        emitt(src"""val retime_counter = Module(new SingleCounter(1, Some(0), Some(max_latency), Some(1), Some(0))) // Counter for masking out the noise that comes out of ShiftRegister in the first few cycles of the app""")
        // emitt(src"""retime_counter.io.input.start := 0.S; retime_counter.io.input.stop := (max_latency.S); retime_counter.io.input.stride := 1.S; retime_counter.io.input.gap := 0.S""")
        emitt(src"""retime_counter.io.input.saturate := true.B; retime_counter.io.input.reset := accelReset; retime_counter.io.input.enable := true.B;""")
        emitGlobalWire(src"""val retime_released_reg = RegInit(false.B)""")
        emitGlobalWire(src"""val retime_released = Wire(Bool())""")
        emitGlobalWire(src"""val rr = retime_released // Shorthand""")
        emitt(src"""retime_released := ${DL("retime_counter.io.output.done",1)} // break up critical path by delaying this """)
        // if (lhs.isInnerControl) emitInhibitor(lhs, None, None)

        emitt(src"""${swap(lhs, SM)}.io.parentAck := io.done""")
        visitBlock(func)
        emitChildrenCxns(lhs)

        emitt(s"""val done_latch = Module(new SRFF())""")
        if (earlyExits.nonEmpty) {
          appPropertyStats += HasBreakpoint
          emitGlobalWire(s"""val breakpoints = Wire(Vec(${earlyExits.length}, Bool()))""")
          emitt(s"""done_latch.io.input.set := ${swap(lhs, Done)} | breakpoints.reduce{_|_}""")        
        } else {
          emitt(s"""done_latch.io.input.set := ${swap(lhs, Done)}""")                
        }
        emitt(s"""done_latch.io.input.reset := ${swap(lhs, Resetter)}""")
        emitt(s"""done_latch.io.input.asyn_reset := ${swap(lhs, Resetter)}""")
        emitt(s"""io.done := done_latch.io.output.data""")
        exitCtrl(lhs)
      }

    case UnitPipe(ens,func) =>
      // emitGlobalWireMap(src"${lhs}_II_done", "Wire(Bool())")
      // // emitGlobalWireMap(src"${lhs}_inhibitor", "Wire(Bool())") // hack
      val parent_kernel = enterCtrl(lhs)
      emitController(lhs)
      connectMask(None)
      emitGlobalWire(src"""${swap(lhs, IIDone)} := true.B""")
      // if (lhs.isInnerControl) emitInhibitor(lhs, None, None)
      inSubGen(src"${lhs}", src"${parent_kernel}") {
        emitt(s"// Controller Stack: ${controllerStack.tail}")
        visitBlock(func)
        emitChildrenCxns(lhs)
      }
      exitCtrl(lhs)

    case ParallelPipe(ens,func) =>
      val parent_kernel = enterCtrl(lhs)
      emitController(lhs)
      connectMask(None)
      emitGlobalWire(src"""${swap(lhs, IIDone)} := true.B""")
      inSubGen(src"${lhs}", src"${parent_kernel}") {
        emitt(s"// Controller Stack: ${controllerStack.tail}")
        visitBlock(func)
        emitChildrenCxns(lhs)
      } 
      exitCtrl(lhs)

    case UnrolledForeach(ens,cchain,func,iters,valids) if (inHw) =>
      val parent_kernel = enterCtrl(lhs)
      emitController(lhs)
      emitIICounter(lhs)
      if (lhs.isStreamControl) forEachChild(lhs){case (c,i) => allocateIters(iters, cchain)}
      else allocateIters(iters, cchain)
      allocateRegChains(lhs, iters.flatten, cchain)
      if (lhs.isPipeControl | lhs.isSeqControl) {
        inSubGen(src"${lhs}", src"${parent_kernel}") {
          emitt(s"// Controller Stack: ${controllerStack.tail}")
          emitIters(iters, cchain)
          allocateValids(lhs, cchain, iters, valids)
          visitBlock(func)
          emitChildrenCxns(lhs)
        }
        emitValids(lhs, cchain, iters, valids)
      }
      else if (lhs.isStreamControl) {
        inSubGen(src"${lhs}", src"${parent_kernel}") {
          emitt(s"// Controller Stack: ${controllerStack.tail}")
          forEachChild(lhs){case (_,_) =>
            emitIters(iters, cchain)
            allocateValids(lhs, cchain, iters, valids) // Must have visited func before we can properly run this method
          }
          visitBlock(func)
          emitChildrenCxns(lhs)
        }
        forEachChild(lhs){case (_,_) =>
          emitValids(lhs, cchain, iters, valids) // Must have visited func before we can properly run this method
        }
      }
      if (lhs.isOuterStreamControl) { controllerStack.push(lhs.children.head.s.get) } // If stream controller with children, just quote the counter for its first child for ctr trivial check
      connectMask(Some(cchain))
      if (lhs.isOuterStreamControl) { controllerStack.pop() }
      exitCtrl(lhs)

    case UnrolledReduce(ens,cchain,func,iters,valids) if (inHw) =>
      val parent_kernel = enterCtrl(lhs)
      emitController(lhs) // If this is a stream, then each child has its own ctr copy
      // if (lhs.isInnerControl) emitInhibitor(lhs, None, None)
      emitIICounter(lhs)
      if (lhs.isStreamControl) forEachChild(lhs){case (c,i) => controllerStack.push(c); allocateIters(iters, cchain); controllerStack.pop()}
      else allocateIters(iters, cchain)
      allocateRegChains(lhs, iters.flatten, cchain)
      if (lhs.isPipeControl | lhs.isSeqControl) {
        inSubGen(src"${lhs}", src"${parent_kernel}") {
          emitt(s"// Controller Stack: ${controllerStack.tail}")
          emitIters(iters, cchain)
          allocateValids(lhs, cchain, iters, valids)
          visitBlock(func)
          emitChildrenCxns(lhs)
        }
        emitValids(lhs, cchain, iters, valids)
      }
      else if (lhs.isStreamControl) {
        inSubGen(src"${lhs}", src"${parent_kernel}") {
          emitt(s"// Controller Stack: ${controllerStack.tail}")
          forEachChild(lhs){case (_,_) =>
            emitIters(iters, cchain)
            allocateValids(lhs, cchain, iters, valids) // Must have visited func before we can properly run this method
          }
          visitBlock(func)
          emitChildrenCxns(lhs)
        }
        forEachChild(lhs){case (_,_) =>
          emitValids(lhs, cchain, iters, valids) // Must have visited func before we can properly run this method
        }
      }
      if (lhs.isOuterStreamControl) { controllerStack.push(lhs.children.head.s.get) } // If stream controller with children, just quote the counter for its first child for ctr trivial check
      connectMask(Some(cchain))
      if (lhs.isOuterStreamControl) { controllerStack.pop() }
      exitCtrl(lhs)

    case StateMachine(ens,start,notDone,action,nextState) =>
      appPropertyStats += HasFSM
      val parent_kernel = enterCtrl(lhs)
      emitController(lhs, true) // If this is a stream, then each child has its own ctr copy
      val state = notDone.input
      alphaconv_register(src"$state")

      emitt("// Emitting notDone")
      visitBlock(notDone)

      connectMask(None)
      emitIICounter(lhs)

      emitt("// Emitting action")
      inSubGen(src"${lhs}", src"${parent_kernel}") {
        emitt(s"// Controller Stack: ${controllerStack.tail}")
        visitBlock(action)
      }
      emitt("// Emitting nextState")
      visitBlock(nextState)
      emitt(src"${swap(lhs, SM)}.io.enable := ${swap(lhs, En)} ")
      emitt(src"${swap(lhs, SM)}.io.nextState := ${nextState.result}.r.asSInt //Mux(${DL(swap(lhs, IIDone), src"1 max ${if (spatialConfig.enableRetiming) nextState.result.fullDelay else 0}", true)}, ${nextState.result}.r.asSInt, ${swap(lhs, SM)}.io.state.r.asSInt) // Assume always int")
      emitt(src"${swap(lhs, SM)}.io.initState := ${start}.r.asSInt")
      emitGlobalWireMap(src"$state", src"Wire(${state.tp})")
      emitt(src"${state}.r := ${swap(lhs, SM)}.io.state.r")
      emitGlobalWireMap(src"${lhs}_doneCondition", "Wire(Bool())")
      emitt(src"${swap(lhs, Blank)}_doneCondition := ~${notDone.result}")
      emitt(src"${swap(lhs, SM)}.io.doneCondition := ${swap(lhs, Blank)}_doneCondition")
      emitChildrenCxns(lhs, true)
      exitCtrl(lhs)

    case op@Switch(selects, body) => 
      val parent_kernel = enterCtrl(lhs)
      emitController(lhs) // If this is a stream, then each child has its own ctr copy
      emitIICounter(lhs)
      val cases = lhs.children.filter(_.s.get != lhs).map(_.s.get)
      
      // Route through signals
      // emitGlobalWireMap(src"""${lhs}_II_done""", """Wire(Bool())"""); emitt(src"""${swap(lhs, IIDone)} := ${swap(parent_kernel, IIDone)}""")
      emitt(src"""${swap(lhs, DatapathEn)} := ${swap(parent_kernel, DatapathEn)} // Not really used probably""")
      connectMask(None)
      // emitt(src"""${swap(lhs, Mask)} := true.B // No enable associated with switch, never mask it""")


      if (lhs.isInnerControl) { // If inner, don't worry about condition mutation
        selects.zipWithIndex.foreach{case (s,i) => emitt(src"""${swap(lhs, SM)}.io.selectsIn($i) := $s""")}
      } else { // If outer, latch in selects in case the body mutates the condition
        selects.indices.foreach{i => 
          emitGlobalWire(src"""val ${cases(i)}_switch_sel_reg = RegInit(false.B)""")
          emitt(src"""${cases(i)}_switch_sel_reg := Mux(Utils.risingEdge(${swap(lhs, En)}), ${selects(i)}, ${cases(i)}_switch_sel_reg)""")
          emitt(src"""${swap(lhs, SM)}.io.selectsIn($i) := ${selects(i)}""")
        }
      }

      inSubGen(src"${lhs}", src"${parent_kernel}") {
        emitt(s"// Controller Stack: ${controllerStack.tail}")
        if (op.R.isBits) {
          emitt(src"val ${lhs}_onehot_selects = Wire(Vec(${selects.length}, Bool()))");emitt(src"val ${lhs}_data_options = Wire(Vec(${selects.length}, ${lhs.tp}))")
          selects.indices.foreach { i => emitt(src"${lhs}_onehot_selects($i) := ${selects(i)}");emitt(src"${lhs}_data_options($i) := ${cases(i)}") }
          emitGlobalWire(src"val $lhs = Wire(${lhs.tp})"); emitt(src"$lhs := Mux1H(${lhs}_onehot_selects, ${lhs}_data_options).r")
        }
        visitBlock(body)
      }
      emitChildrenCxns(lhs, false)
      exitCtrl(lhs)


    case op@SwitchCase(body) =>
      val parent_kernel = enterCtrl(lhs)
      emitController(lhs) // If this is a stream, then each child has its own ctr copy
      emitIICounter(lhs)
      // emitInhibitor(lhs, None, Some(lhs.parent.s.get))
      connectMask(None)

      inSubGen(src"${lhs}", src"${parent_kernel}") {
        emitt(s"// Controller Stack: ${controllerStack.tail}")
        // if (blockContents(body).length > 0) {
        // if (childrenOf(lhs).count(isControlNode) == 1) { // This is an outer pipe
        visitBlock(body)
        if (op.R.isBits) {
          emitGlobalWire(src"val $lhs = Wire(${lhs.tp})")
          emitt(src"$lhs.r := ${body.result}.r")
        }
      }
      emitChildrenCxns(lhs, false)
      exitCtrl(lhs)


    case _ => super.gen(lhs, rhs)
  }

  override def emitFooter(): Unit = {
    inAccel{
      if (spatialConfig.compressWires >= 1) {
        inGenn(out, "GlobalModules", ext) {
          emitt(src"val ic = List.fill(${instrumentCounters.length*2}){Module(new InstrumentationCounter())}")
        }
        inGenn(out, "Mapping", ext) {
          emit("// Found the following wires:")
          compressorMap.values.map(_._1).toSet.toList.foreach{wire: String => 
            emit(s"    // $wire (${listHandle(wire)})")
          }
          compressorMap.values.map(_._1).toSet.toList.foreach{wire: String => 
            val handle = listHandle(wire)
            emit("")
            emit(s"// ${wire}")
            emit("// ##################")
            compressorMap.filter(_._2._1 == wire).foreach{entry => 
              emit(s"      // ${handle}(${entry._2._2}) = ${entry._1}")
            }
          }
        }
      }


      emitGlobalWire(s"val max_latency = $maxretime")

      inGenn(out, "GlobalModules", ext) {
        emitt(src"val breakpt_activators = List.fill(${earlyExits.length}){Wire(Bool())}")
      }

      inGen(out, "Instantiator.scala") {
        emit ("")
        emit ("// Instrumentation")
        emit (s"val numArgOuts_instr = ${instrumentCounters.length*2}")
        instrumentCounters.zipWithIndex.foreach { case(p,i) =>
          val depth = " "*p._2
          emit (src"""// ${depth}${quote(p._1)}""")
        }
        emit (s"val numArgOuts_breakpts = ${earlyExits.length}")
        emit ("""/* Breakpoint Contexts:""")
        earlyExits.zipWithIndex.foreach {case (p,i) => 
          createBreakpoint(p, i)
          emit (s"breakpoint ${i}: ${p.ctx}")
        }
        emit ("""*/""")
      }

      inGenn(out, "IOModule", ext) {
        emit (src"// Root controller for app: ${config.name}")
        // emit (src"// Complete config: ${config.printer()}")
        // emit (src"// Complete cfg: ${spatialConfig.printer()}")
        emit ("")
        emit (src"// Widths: ${widthStats.sorted}")
        emit (src"//   Widest Outer Controller: ${if (widthStats.length == 0) 0 else widthStats.max}")
        emit (src"// Depths: ${depthStats.sorted}")
        emit (src"//   Deepest Inner Controller: ${if (depthStats.length == 0) 0 else depthStats.max}")
        emit (s"// App Characteristics: ${appPropertyStats.toList.map(_.getClass.getName.split("\\$").last.split("\\.").last).mkString(",")}")
        emit ("// Instrumentation")
        emit (s"val io_numArgOuts_instr = ${instrumentCounters.length*2}")
        emit (s"val io_numArgOuts_breakpts = ${earlyExits.length}")

        emit ("""// Set Build Info""")
        val trgt = s"${spatialConfig.target.name}".replace("DE1", "de1soc")
        if (spatialConfig.compressWires >= 1) {
          pipeRtMap.groupBy(_._1._1).map{x => 
            val listBuilder = x._2.toList.sortBy(_._1._2).map(_._2)
            emit (src"val ${listHandle(x._1)}_latmap = List(${listBuilder.mkString(",")})")
          }
          // TODO: Make the things below more efficient
          compressorMap.values.map(_._1).toSet.toList.foreach{wire: String => 
            if (wire == "_latency") {
              emit (src"val ${listHandle(wire)} = List[Int](${retimeList.mkString(",")})")  
            }
          }
          compressorMap.values.map(_._1).toSet.toList.foreach{wire: String => 
            if (wire == "_latency") {
            } else if (wire.contains("InnerControl(") || wire.contains("OuterControl(")) {
              val numel = compressorMap.filter(_._2._1 == wire).size
              emit (src"val ${listHandle(wire)} = List.tabulate(${numel}){i => ${wire.replace("))", src",latency=${listHandle("_latency")}(${listHandle(wire)}_latmap(i))))")}}")
            } else {
              val numel = compressorMap.filter(_._2._1 == wire).size
              emit (src"val ${listHandle(wire)} = List.fill(${numel}){${wire}}")            
            }
          }
        }

        emit (s"Utils.fixmul_latency = ${latencyOption("FixMul", Some(1))}")
        emit (s"Utils.fixdiv_latency = ${latencyOption("FixDiv", Some(1))}")
        emit (s"Utils.fixadd_latency = ${latencyOption("FixAdd", Some(1))}")
        emit (s"Utils.fixsub_latency = ${latencyOption("FixSub", Some(1))}")
        emit (s"Utils.fixmod_latency = ${latencyOption("FixMod", Some(1))}")
        emit (s"Utils.fixeql_latency = ${latencyOption("FixEql", None)}.toInt")
        // emit (s"Utils.tight_control   = ${spatialConfig.enableTightControl}")
        emit (s"Utils.mux_latency    = ${latencyOption("Mux", None)}.toInt")
        emit (s"Utils.sramload_latency    = ${latencyOption("SRAMBankedRead", None)}.toInt")
        emit (s"Utils.sramstore_latency    = ${latencyOption("SRAMBankedWrite", None)}.toInt")
        emit (s"Utils.SramThreshold = 4")
        emit (s"""Utils.target = ${trgt}""")
        emit (s"""Utils.retime = ${spatialConfig.enableRetiming}""")

      }

    }
    super.emitFooter()
  }

}
