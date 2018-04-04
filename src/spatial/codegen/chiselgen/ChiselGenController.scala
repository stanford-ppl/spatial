package spatial.codegen.chiselgen

import argon._
import argon.codegen.Codegen
import spatial.lang._
import spatial.node._
import spatial.internal.{spatialConfig => cfg}
import spatial.data._
import spatial.util._

trait ChiselGenController extends ChiselGenCommon {

  var hwblock: Option[Sym[_]] = None
  // var outMuxMap: Map[Sym[Reg[_]], Int] = Map()
  private var nbufs: List[(Sym[Reg[_]], Int)]  = List()

  /* Set of control nodes which already have their enable signal emitted */
  var enDeclaredSet = Set.empty[Sym[_]]

  /* Set of control nodes which already have their done signal emitted */
  var doneDeclaredSet = Set.empty[Sym[_]]

  var instrumentCounters: List[(Sym[_], Int)] = List()

  /* For every iter we generate, we track the children it may be used in.
     Given that we are quoting one of these, look up if it has a map entry,
     and keep getting parents of the currentController until we find a match or 
     get to the very top
  */

  /* List of break or exit nodes */
  var earlyExits: List[Sym[_]] = List()

  def createBreakpoint(lhs: Sym[_], id: Int): Unit = {
    // emitInstrumentation(src"io.argOuts(io_numArgOuts_reg + io_numArgIOs_reg + io_numArgOuts_instr + $id).bits := 1.U")
    // emitInstrumentation(src"io.argOuts(io_numArgOuts_reg + io_numArgIOs_reg + io_numArgOuts_instr + $id).valid := breakpoints($id)")
  }

  def createInstrumentation(lhs: Sym[_]): Unit = {
    // if (cfg.enableInstrumentation) {
    //   val ctx = s"${lhs.ctx}"
    //   emitInstrumentation(src"""// Instrumenting $lhs, context: ${ctx}, depth: ${controllerStack.length}""")
    //   val id = instrumentCounters.length
    //   if (config.multifile == 5 || config.multifile == 6) {
    //     emitInstrumentation(src"ic(${id*2}).io.enable := ${swap(lhs,En)}; ic(${id*2}).reset := accelReset")
    //     emitInstrumentation(src"ic(${id*2+1}).io.enable := Utils.risingEdge(${swap(lhs, Done)}); ic(${id*2+1}).reset := accelReset")
    //     emitInstrumentation(src"""io.argOuts(io_numArgOuts_reg + io_numArgIOs_reg + 2 * ${id}).bits := ic(${id*2}).io.count""")
    //     emitInstrumentation(src"""io.argOuts(io_numArgOuts_reg + io_numArgIOs_reg + 2 * ${id}).valid := ${swap(hwblock_sym.head, En)}//${swap(hwblock_sym.head, Done)}""")
    //     emitInstrumentation(src"""io.argOuts(io_numArgOuts_reg + io_numArgIOs_reg + 2 * ${id} + 1).bits := ic(${id*2+1}).io.count""")
    //     emitInstrumentation(src"""io.argOuts(io_numArgOuts_reg + io_numArgIOs_reg + 2 * ${id} + 1).valid := ${swap(hwblock_sym.head, En)}//${swap(hwblock_sym.head, Done)}""")        
    //   } else {
    //     emitInstrumentation(src"""val ${lhs}_cycles = Module(new InstrumentationCounter())""")
    //     emitInstrumentation(src"${lhs}_cycles.io.enable := ${swap(lhs,En)}; ${lhs}_cycles.reset := accelReset")
    //     emitInstrumentation(src"""val ${lhs}_iters = Module(new InstrumentationCounter())""")
    //     emitInstrumentation(src"${lhs}_iters.io.enable := Utils.risingEdge(${swap(lhs, Done)}); ${lhs}_iters.reset := accelReset")
    //     emitInstrumentation(src"""io.argOuts(io_numArgOuts_reg + io_numArgIOs_reg + 2 * ${id}).bits := ${lhs}_cycles.io.count""")
    //     emitInstrumentation(src"""io.argOuts(io_numArgOuts_reg + io_numArgIOs_reg + 2 * ${id}).valid := ${swap(hwblock_sym.head, En)}//${swap(hwblock_sym.head, Done)}""")
    //     emitInstrumentation(src"""io.argOuts(io_numArgOuts_reg + io_numArgIOs_reg + 2 * ${id} + 1).bits := ${lhs}_iters.io.count""")
    //     emitInstrumentation(src"""io.argOuts(io_numArgOuts_reg + io_numArgIOs_reg + 2 * ${id} + 1).valid := ${swap(hwblock_sym.head, En)}//${swap(hwblock_sym.head, Done)}""")        
    //   }
    //   instrumentCounters = instrumentCounters :+ (lhs, controllerStack.length)
    // }
  }

  protected def emitControlSignals(lhs: Sym[_]): Unit = {
    emitGlobalWireMap(src"""${swap(lhs, Done)}""", """Wire(Bool())""")
    emitGlobalWireMap(src"""${swap(lhs, En)}""", """Wire(Bool())""")
    emitGlobalWireMap(src"""${swap(lhs, BaseEn)}""", """Wire(Bool())""")
    emitGlobalWireMap(src"""${lhs}_mask""", """Wire(Bool())""")
    emitGlobalWireMap(src"""${lhs}_resetter""", """Wire(Bool())""")
    emitGlobalWireMap(src"""${lhs}_datapath_en""", """Wire(Bool())""")
    emitGlobalWireMap(src"""${lhs}_ctr_trivial""", """Wire(Bool())""")
  }

  def emitRegChains(lhs: Sym[_]) = {
    val stages = lhs.children.toList.map(_.s.get)
    val counters = lhs.cchains.head.ctrs
    val par = lhs.cchains.head.pars
    // val ctrMapping = par.indices.map{i => par.dropRight(par.length - i).sum}
    // ctrlIters(lhs).toList.zipWithIndex.foreach { case (idx,index) =>
    //   val ctr = ctrMapping.filter(_ <= index).length - 1
    //   val w = bitWidth(counters(ctr).typeArgs.head)
    //   inGenn(out, "BufferControlCxns", ext) {
    //     stages.zipWithIndex.foreach{ case (s, i) =>
    //       emitGlobalWireMap(src"${s}_done", "Wire(Bool())")
    //       emitGlobalWireMap(src"${s}_en", "Wire(Bool())")
    //       emit(src"""${swap(idx, Chain)}.connectStageCtrl(${DL(swap(s, Done), 0, true)}, ${swap(s, En)}, List($i)) // Used to be delay of 1 on Nov 26, 2017 but not sure why""")
    //     }
    //   }
    //   emit(src"""${swap(idx, Chain)}.chain_pass(${idx}, ${swap(controller, SM)}.io.output.ctr_inc)""")
    //   // Associate bound sym with both ctrl node and that ctrl node's cchain
    // }
  }

  protected def ctrlInfo(sym: Sym[_]): (Boolean, Boolean, String) = {    
    val hasStreamIns = !getReadStreams(sym.toCtrl).isEmpty

    val isInner = levelOf(sym) == InnerControl

    val smStr = if (isInner) {
      if (isStreamChild(sym) & hasStreamIns ) {
        "Streaminner"
      } else {
        "Innerpipe"
      }
    } else {
      styleOf(sym) match {
        case Sched.Pipe => s"Metapipe"
        case Sched.Stream => "Streampipe"
        case Sched.Seq => s"Seqpipe"
        case Sched.ForkJoin => s"Parallel"
        case Sched.Fork => s"Sched.Fork"
      }
    }
    (hasStreamIns, isInner, smStr)
  }

  protected def emitCopiedCChain(self: Sym[_]): Unit = {
    val parent = self.parent.s.get
    if (parent != Host) {
      if (levelOf(parent) != InnerControl && styleOf(parent) == Sched.Stream) {
        emitCounterChain(self.cchains.head, src"_copy${self}")
      }
    }

  }

  protected def emitChildrenCxns(sym:Sym[_], isFSM: Boolean = false): Unit = {
    val (hasStreamIns, isInner, smStr) = ctrlInfo(sym)

    /* Control Signals to Children Controllers */
    if (!isInner) {
      emit(src"""// ---- Begin $smStr ${sym} Children Signals ----""")
      sym.children.toList.zipWithIndex.foreach { case (c, idx) =>
        if (smStr == "Streampipe" & !sym.cchains.isEmpty) {
          emit(src"""${swap(sym, SM)}.io.input.stageDone(${idx}) := ${swap(src"${sym.cchains.head}_copy${c}", Done)};""")
        } else {
          emit(src"""${swap(sym, SM)}.io.input.stageDone(${idx}) := ${swap(c, Done)};""")
        }

        emit(src"""${swap(sym, SM)}.io.input.stageMask(${idx}) := ${swap(c, Mask)};""")

        val streamAddition = getStreamEnablers(c.s.get)

        val base_delay = if (cfg.enableTightControl) 0 else 1
        emit(src"""${swap(c, BaseEn)} := ${DL(src"${swap(sym, SM)}.io.output.stageEnable(${idx})", base_delay, true)} & ${DL(src"~${swap(c, Done)}", 1, true)}""")  
        emit(src"""${swap(c, En)} := ${swap(c, BaseEn)} ${streamAddition}""")  

        // If this is a stream controller, need to set up counter copy for children
        if (smStr == "Streampipe" & !sym.cchains.isEmpty) {
          emitGlobalWireMap(src"""${swap(src"${sym.cchains.head}_copy${c}", En)}""", """Wire(Bool())""") 
          val unitKid = c match {case Op(UnitPipe(_,_)) => true; case _ => false}
          val snooping = getNowValidLogic(c.s.get).replace(" ", "") != ""
          val innerKid = levelOf(c.s.get) == InnerControl
          val signalHandle = if (unitKid & innerKid & snooping) { // If this is a unit pipe that listens, we just need to snoop the now_valid & _ready overlap
            src"true.B ${getReadyLogic(c.s.get)} ${getNowValidLogic(c.s.get)}"
          } else if (innerKid) { // Otherwise, use the done & ~inhibit
            src"${swap(c, Done)} /* & ~${swap(c, Inhibitor)} */"
          } else {
            src"${swap(c, Done)}"
          }
          // emit copied cchain is now responsibility of child
          // emitCounterChain(sym.cchains.head, ctrs, src"_copy$c")
          emit(src"""${swap(src"${sym.cchains.head}_copy${c}", En)} := ${signalHandle}""")
          emit(src"""${swap(src"${sym.cchains.head}_copy${c}", Resetter)} := ${DL(src"${swap(sym, SM)}.io.output.rst_en", 1, true)}""")
        }
        if (c match { case Op(_: StateMachine[_]) => true; case _ => false}) { // If this is an fsm, we want it to reset with each iteration, not with the reset of the parent
          emit(src"""${swap(c, Resetter)} := ${DL(src"${swap(sym, SM)}.io.output.rst_en", 1, true)} | ${DL(swap(c, Done), 1, true)}""") //changed on 12/13
        } else {
          emit(src"""${swap(c, Resetter)} := ${DL(src"${swap(sym, SM)}.io.output.rst_en", 1, true)}""")   //changed on 12/13
        }
        
      }
    }
    /* Emit reg chains */
    if (!ctrlIters(sym.toCtrl).isEmpty) {
      if (smStr == "Metapipe" & sym.children.toList.length > 1) {
        emitRegChains(sym)
      }
    }

  }

  def emitController(sym:Sym[_], isFSM: Boolean = false): Unit = {

    val (hasStreamIns, isInner, smStr) = ctrlInfo(sym)
    // TODO: We should really just check for unspecialized reduce, not any reduce
    val isReduce = if (isInner) {
      sym match {
        case Op(UnrolledReduce(_,_,_,_,_)) => true
        case _ => false
      }
    } else false

    emitt(src"""//  ---- ${if (isInner) {"INNER: "} else {"OUTER: "}}Begin ${smStr} $sym Controller ----""")

    /* State Machine Instatiation */
    // IO
    var hasForever = false
    // disableSplit = true
    val numIter = if (!sym.cchains.isEmpty) {
      val counters = sym.cchains.head.ctrs
      counters.zipWithIndex.map {case (ctr,i) =>
        if (ctr.isForever) {
          hasForever = true
          emitGlobalWireMap(src"${sym}_level${i}_iters", src"Wire(UInt(32.W))")
          emitt(src"""${swap(src"${sym}_level${i}_iters", Blank)} := 0.U // Count forever""")
          src"${swap(src"${sym}_level${i}_iters", Blank)}"
        } else {
          val w = bitWidth(ctr.typeArgs.head)
          (ctr.start, ctr.end, ctr.step, ctr.ctrPar) match {
            /*
                (e - s) / (st * p) + Mux( (e - s) % (st * p) === 0, 0, 1)
                   1          1              1          1    
                        1                         1
                        .                                     1
                        .            1
                                   1
                Issue # 199           
            */
            case (Final(s), Final(e), Final(st), Final(p)) => 
              appPropertyStats += HasStaticCtr
              emitt(src"val ${sym}${i}_range = ${e} - ${s}")
              emitt(src"val ${sym}${i}_jump = ${st} * ${p}")
              emitt(src"val ${sym}${i}_hops = ${sym}${i}_range / ${sym}${i}_jump")
              emitt(src"val ${sym}${i}_leftover = ${sym}${i}_range % ${sym}${i}_jump")
              emitt(src"val ${sym}${i}_evenfit = ${sym}${i}_leftover == 0")
              emitt(src"val ${sym}${i}_adjustment = if (${sym}${i}_evenfit) 0 else 1")
              emitGlobalWireMap(src"${sym}_level${i}_iters", src"Wire(UInt(${w}.W))")
              emitt(src"""${swap(src"${sym}_level${i}_iters", Blank)} := (${sym}${i}_hops + ${sym}${i}_adjustment).U(${w}.W)""")
            case (Final(s), Final(e), _, Final(p)) => 
              appPropertyStats += HasVariableCtrStride
              emitt("// TODO: Figure out how to make this one cheaper!")
              emitt(src"val ${sym}${i}_range = ${e} - ${s}")
              emitt(src"val ${sym}${i}_jump = ${ctr.step} *-* ${p}.S(${w}.W)")
              emitt(src"val ${sym}${i}_hops = (${sym}${i}_range.S(${w}.W) /-/ ${sym}${i}_jump).asUInt")
              emitt(src"val ${sym}${i}_leftover = ${sym}${i}_range.S(${w}.W) %-% ${sym}${i}_jump")
              emitt(src"val ${sym}${i}_evenfit = ${DL(src"${sym}${i}_leftover.asUInt === 0.U", "Utils.fixeql_latency")}")
              emitt(src"val ${sym}${i}_adjustment = ${DL(src"Mux(${sym}${i}_evenfit, 0.U, 1.U)", "Utils.mux_latency")}")
              emitGlobalWireMap(src"${sym}_level${i}_iters", src"Wire(UInt(${w}.W))")
              emitt(src"""${swap(src"${sym}_level${i}_iters", Blank)} := ${DL(src"${sym}${i}_hops + ${sym}${i}_adjustment", src"(Utils.fixadd_latency*${sym}${i}_hops.getWidth).toInt")}.r""")
            case (_, _, Final(st), Final(p)) => 
              appPropertyStats += HasVariableCtrSyms
              emitt(src"val ${sym}${i}_range =  ${DL(src"${ctr.end} - ${ctr.start}", src"(Utils.fixsub_latency*${ctr.end}.getWidth).toInt")}")
              emitt(src"val ${sym}${i}_jump = ${st} * ${p}")
              emitt(src"val ${sym}${i}_hops = ${sym}${i}_range /-/ ${sym}${i}_jump.FP(true, 32, 0)")
              emitt(src"val ${sym}${i}_leftover = ${sym}${i}_range %-% ${sym}${i}_jump.FP(true, 32, 0)")
              emitt(src"val ${sym}${i}_evenfit = ${DL(src"${sym}${i}_leftover === 0.U", "Utils.fixeql_latency")}")
              emitt(src"val ${sym}${i}_adjustment = ${DL(src"Mux(${sym}${i}_evenfit, 0.U, 1.U)", "Utils.mux_latency")}")
              emitGlobalWireMap(src"${sym}_level${i}_iters", src"Wire(UInt(32.W))")
              emitt(src"""${swap(src"${sym}_level${i}_iters", Blank)} := ${DL(src"${sym}${i}_hops + ${sym}${i}_adjustment", src"(Utils.fixadd_latency*${sym}${i}_hops.getWidth).toInt")}.r""")
            case _ => 
              appPropertyStats += HasVariableCtrSyms // TODO: Possible variable stride too, should probably match against this
              emitt(src"val ${sym}${i}_range = ${DL(src"${ctr.end} - ${ctr.start}", src"(Utils.fixsub_latency*${ctr.end}.getWidth).toInt")}")
              emitt(src"val ${sym}${i}_jump = ${ctr.step} *-* ${ctr.ctrPar}")
              emitt(src"val ${sym}${i}_hops = ${sym}${i}_range /-/ ${sym}${i}_jump")
              emitt(src"val ${sym}${i}_leftover = ${sym}${i}_range %-% ${sym}${i}_jump")
              emitt(src"val ${sym}${i}_evenfit = ${DL(src"${sym}${i}_leftover === 0.U", "Utils.fixeql_latency")}")
              emitt(src"val ${sym}${i}_adjustment = ${DL(src"Mux(${sym}${i}_evenfit, 0.U, 1.U)", "Utils.mux_latency")}")
              emitGlobalWireMap(src"${sym}_level${i}_iters", src"Wire(UInt(32.W))")
              emitt(src"""${swap(src"${sym}_level${i}_iters", Blank)} := ${DL(src"${sym}${i}_hops + ${sym}${i}_adjustment", src"(Utils.fixadd_latency*${sym}${i}_hops.getWidth).toInt")}.r""")
          }
          // emitt(src"""${swap(src"${sym}_level${i}_iters", Blank)} := ${DL(src"${sym}${i}_hops + ${sym}${i}_adjustment", 1)}.r""")
          src"${swap(src"${sym}_level${i}_iters", Blank)}"
        }
      }
    } else { 
      List("1.U") // Unit pipe:
    }
    // disableSplit = false

    val constrArg = if (isInner) {s"${isFSM}"} else {s"${sym.children.length}, isFSM = ${isFSM}"}

    val lat = 0// bodyLatency.sum(sym) // FIXME
    emitControlSignals(sym)
    createInstrumentation(sym)

    // Pass done signal upward and grab your own en signals if this is a switchcase child
    sym.parent match {
      case Op(SwitchCase(_)) => 
        emitt(src"""${swap(sym.parent, Done)} := ${swap(sym, Done)}""")
        val streamAddition = ""//getStreamEnablers(sym) // FIXME
        emitt(src"""${swap(sym, En)} := ${swap(sym.parent, En)} ${streamAddition}""")  
        emitt(src"""${swap(sym, Resetter)} := ${swap(sym.parent, Resetter)}""")

      case _ =>
        // no sniffing to be done
    }

    val stw = sym match{case Op(x: StateMachine[_]) => bitWidth(sym.tp.typeArgs.head); case _ if (sym.children.length <= 1) => 3; case _ => (scala.math.log(sym.children.length) / scala.math.log(2)).toInt + 2}
    val ctrdepth = if (sym.cchains.isEmpty) 1 else sym.cchains.head.ctrs.length
    val static = if (sym.cchains.isEmpty) true else sym.cchains.head.isStatic
    emitGlobalRetimeMap(src"""${sym}_retime""", s"${lat}.toInt")
    emitt(s"""// This is now global: val ${quote(sym)}_retime = ${lat}.toInt // Inner loop? ${isInner}, II = ${iiOf(sym)}""")
    emitGlobalModuleMap(src"${sym}_sm", src"Module(new ${smStr}(${constrArg.mkString}, ctrDepth = $ctrdepth, stateWidth = ${stw}, retime = ${swap(sym, Retime)}, staticNiter = $static, isReduce = $isReduce))")
    emitt(src"// This is now global: val ${swap(sym, SM)} = Module(new ${smStr}(${constrArg.mkString}, ctrDepth = $ctrdepth, stateWidth = ${stw}, retime = ${swap(sym, Retime)}, staticNiter = $static))")
    emitt(src"""${swap(sym, SM)}.io.input.enable := ${swap(sym, En)} & retime_released""")
    if (isFSM) {
      emitGlobalWireMap(src"${sym}_inhibitor", "Wire(Bool())") // hacky but oh well
      emitt(src"""${swap(sym, Done)} := ${DL(src"${swap(sym, SM)}.io.output.done & ${DL(src"~${swap(sym, Inhibitor)}", 2, true)}", swap(sym, Retime), true)}""")      
    } else if (isStreamChild(sym)) {
      val streamOuts = if (getStreamInfoReady(sym.toCtrl).mkString(" && ").replace(" ","") != "") getStreamInfoReady(sym.toCtrl).mkString(" && ") else { "true.B" }
      emitt(src"""${swap(sym, Done)} := Utils.streamCatchDone(${swap(sym, SM)}.io.output.done, $streamOuts, ${swap(sym, Retime)}, rr, accelReset) // Directly connecting *_done.D* creates a hazard on stream pipes if ~*_ready turns off for that exact cycle, since the retime reg will reject it""")
    } else {
      emitt(src"""${swap(sym, Done)} := Utils.risingEdge(${DL(src"${swap(sym, SM)}.io.output.done", swap(sym, Retime), true)}) // Rising edge necessary in case stall happens at same time as done comes through""")
    }
    emitGlobalWireMap(src"""${swap(sym, RstEn)}""", """Wire(Bool())""") 
    emitt(src"""${swap(sym, RstEn)} := ${swap(sym, SM)}.io.output.rst_en // Generally used in inner pipes""")
    emitt(src"""${swap(sym, SM)}.io.input.numIter := (${numIter.mkString(" *-* ")}).raw.asUInt // Unused for inner and parallel""")
    if (cfg.target.latencyModel.model("FixMul")("b" -> 32)("LatencyOf").toInt * numIter.length > maxretime) maxretime = cfg.target.latencyModel.model("FixMul")("b" -> 32)("LatencyOf").toInt * numIter.length
    emitt(src"""${swap(sym, SM)}.io.input.rst := ${swap(sym, Resetter)} // generally set by parent""")

    if (isStreamChild(sym) & hasStreamIns & beneathForever(sym)) {
      emitt(src"""${swap(sym, DatapathEn)} := ${swap(sym, En)} & ~${swap(sym, CtrTrivial)} // Immediate parent has forever counter, so never mask out datapath_en""")    
    } else if ((isStreamChild(sym) & hasStreamIns & !sym.cchains.isEmpty)) { // for FSM or hasStreamIns, tie en directly to datapath_en
      emitt(src"""${swap(sym, DatapathEn)} := ${swap(sym, En)} & ~${swap(sym, Done)} & ~${swap(sym, CtrTrivial)} ${getNowValidLogic(sym)}""")  
    } else if (isFSM) { // for FSM or hasStreamIns, tie en directly to datapath_en
      emitt(src"""${swap(sym, DatapathEn)} := ${swap(sym, En)} & ~${swap(sym, Done)} & ~${swap(sym, CtrTrivial)} & ~${swap(sym, SM)}.io.output.done ${getNowValidLogic(sym)}""")  
    } else if ((isStreamChild(sym) & hasStreamIns)) { // _done used to be commented out but I'm not sure why
      emitt(src"""${swap(sym, DatapathEn)} := ${swap(sym, En)} & ~${swap(sym, Done)} & ~${swap(sym, CtrTrivial)} ${getNowValidLogic(sym)} """)  
    } else {
      emitt(src"""${swap(sym, DatapathEn)} := ${swap(sym, SM)}.io.output.ctr_inc & ~${swap(sym, Done)} & ~${swap(sym, CtrTrivial)} ${getReadyLogic(sym)}""")
    }
    
    /* Counter Signals for controller (used for determining done) */
    if (smStr != "Parallel" & smStr != "Streampipe") {
      if (!sym.cchains.isEmpty) {
        if (!sym.cchains.head.isForever) {
          emitGlobalWireMap(src"""${swap(sym.cchains.head, En)}""", """Wire(Bool())""") 
          sym match { 
            case Op(n: UnrolledReduce) => // These have II
              emitt(src"""${swap(sym.cchains.head, En)} := ${swap(sym, SM)}.io.output.ctr_inc & ${swap(sym, IIDone)}""")
            case Op(n: UnrolledForeach) => 
              if (isStreamChild(sym) & hasStreamIns) {
                emitt(src"${swap(sym.cchains.head, En)} := ${swap(sym, DatapathEn)} & ${swap(sym, IIDone)} & ~${swap(sym, Inhibitor)} ${getNowValidLogic(sym)}") 
              } else {
                emitt(src"${swap(sym.cchains.head, En)} := ${swap(sym, SM)}.io.output.ctr_inc & ${swap(sym, IIDone)}// Should probably also add inhibitor")
              }             
            case _ => // If parent is stream, use the fine-grain enable, otherwise use ctr_inc from sm
              if (isStreamChild(sym) & hasStreamIns) {
                emitt(src"${swap(sym.cchains.head, En)} := ${swap(sym, DatapathEn)} & ~${swap(sym, Inhibitor)} ${getNowValidLogic(sym)}") 
              } else {
                emitt(src"${swap(sym.cchains.head, En)} := ${swap(sym, SM)}.io.output.ctr_inc // Should probably also add inhibitor")
              } 
          }
          emitt(src"""// ---- Counter Connections for $smStr ${sym} (${sym.cchains.head}) ----""")
          val ctr = sym.cchains.head
          if (isStreamChild(sym) & hasStreamIns) {
            emitt(src"""${swap(ctr, Resetter)} := ${DL(swap(sym, Done), 1, true)} // Do not use rst_en for stream kiddo""")
          } else {
            emitt(src"""${swap(ctr, Resetter)} := ${DL(swap(sym, RstEn), 0, true)} // changed on 9/19""")
          }
          if (isInner) { 
            // val dlay = if (cfg.enableRetiming || cfg.enablePIRSim) {src"1 + ${swap(sym, Retime)}"} else "1"
            emitt(src"""${swap(sym, SM)}.io.input.ctr_done := ${DL(swap(ctr, Done), 1, true)}""")
          }

        }
      } else {
        emitt(src"""// ---- Single Iteration for $smStr ${sym} ----""")
        if (isInner) { 
          emitGlobalWireMap(src"${sym}_ctr_en", "Wire(Bool())")
          if (isStreamChild(sym) & hasStreamIns) {
            emitt(src"""${swap(sym, SM)}.io.input.ctr_done := ${DL(src"${swap(sym, En)} & ~${swap(sym, Done)}", 1, true)} // Disallow consecutive dones from stream inner""")
            emitt(src"""${swap(sym, CtrEn)} := ${swap(sym, Done)} // stream kiddo""")
          } else {
            emitt(src"""${swap(sym, SM)}.io.input.ctr_done := ${DL(src"Utils.risingEdge(${swap(sym, SM)}.io.output.ctr_inc)", 1, true)}""")
            emitt(src"""${swap(sym, CtrEn)} := ${swap(sym, SM)}.io.output.ctr_inc""")            
          }
        } else {
          emitt(s"// TODO: How to properly emitt for non-innerpipe unit counter?  Probably doesn't matter")
        }
      }
    }

    val hsi = if (isInner & hasStreamIns) "true.B" else "false.B"
    val hf = if (hasForever) "true.B" else "false.B"
    emitt(src"${swap(sym, SM)}.io.input.hasStreamIns := $hsi")
    emitt(src"${swap(sym, SM)}.io.input.forever := $hf")

    // emitChildrenCxns(sym, cchain, iters, isFSM)
    /* Create reg chain mapping */
    if (!ctrlIters(sym.toCtrl).isEmpty) {
      if (smStr == "Metapipe" & sym.children.length > 1) {
        sym.children.foreach{ 
          case stage @ Op(s:UnrolledForeach) => cchainPassMap += (s.cchain -> stage)
          case stage @ Op(s:UnrolledReduce) => cchainPassMap += (s.cchain -> stage)
          case _ =>
        }
        ctrlIters(sym.toCtrl).foreach{ idx => 
          itersMap += (idx -> sym.children.toList.map(_.s.get))
        }

      }
    }
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {

    case AccelScope(func) =>
      hwblock = Some(lhs)
      controllerStack.push(lhs)
      if (levelOf(lhs) == OuterControl) {widthStats += lhs.toCtrl.children.length}
      else if (levelOf(lhs) == InnerControl) {depthStats += controllerStack.length}
      enterAccel()
      val streamAddition = getStreamEnablers(lhs)
      emitController(lhs)
      emitGlobalWire(src"val accelReset = reset.toBool | io.reset")
      emitt(s"""${swap(lhs, En)} := io.enable & !io.done ${streamAddition}""")
      emitt(s"""${swap(lhs, Resetter)} := Utils.getRetimed(accelReset, 1)""")
      emitt(src"""${swap(lhs, CtrTrivial)} := false.B""")
      emitGlobalWireMap(src"""${lhs}_II_done""", """Wire(Bool())""")
      if (iiOf(lhs) <= 1) {
        emitt(src"""${swap(lhs, IIDone)} := true.B""")
      } else {
        emitt(src"""val ${lhs}_IICtr = Module(new RedxnCtr(2 + Utils.log2Up(${swap(lhs, Retime)})));""")
        emitt(src"""${swap(lhs, IIDone)} := ${lhs}_IICtr.io.output.done | ${swap(lhs, CtrTrivial)}""")
        emitt(src"""${lhs}_IICtr.io.input.enable := ${swap(lhs,En)}""")
        emitt(src"""${lhs}_IICtr.io.input.stop := ${iiOf(lhs)}.toInt.S // ${swap(lhs, Retime)}.S""")
        emitt(src"""${lhs}_IICtr.io.input.reset := accelReset | ${DL(swap(lhs, IIDone), 1, true)}""")
        emitt(src"""${lhs}_IICtr.io.input.saturate := false.B""")       
      }
      emitt(src"""val retime_counter = Module(new SingleCounter(1, Some(0), Some(max_retime), Some(1), Some(0))) // Counter for masking out the noise that comes out of ShiftRegister in the first few cycles of the app""")
      // emitt(src"""retime_counter.io.input.start := 0.S; retime_counter.io.input.stop := (max_retime.S); retime_counter.io.input.stride := 1.S; retime_counter.io.input.gap := 0.S""")
      emitt(src"""retime_counter.io.input.saturate := true.B; retime_counter.io.input.reset := accelReset; retime_counter.io.input.enable := true.B;""")
      emitGlobalWire(src"""val retime_released_reg = RegInit(false.B)""")
      emitGlobalWire(src"""val retime_released = ${DL("retime_released_reg", 1)}""")
      emitGlobalWire(src"""val rr = retime_released // Shorthand""")
      emitt(src"""retime_released := ${DL("retime_counter.io.output.done",1)} // break up critical path by delaying this """)
      if (levelOf(lhs) == InnerControl) emitInhibitor(lhs, None, None)

      visitBlock(func)
      emitChildrenCxns(lhs)
      // emitCopiedCChain(lhs)

      emitt(s"""val done_latch = Module(new SRFF())""")
      if (earlyExits.length > 0) {
        appPropertyStats += HasBreakpoint
        emitGlobalWire(s"""val breakpoints = Wire(Vec(${earlyExits.length}, Bool()))""")
        emitt(s"""done_latch.io.input.set := ${swap(lhs, Done)} | breakpoints.reduce{_|_}""")        
      } else {
        emitt(s"""done_latch.io.input.set := ${swap(lhs, Done)}""")                
      }
      emitt(s"""done_latch.io.input.reset := ${swap(lhs, Resetter)}""")
      emitt(s"""done_latch.io.input.asyn_reset := ${swap(lhs, Resetter)}""")
      emitt(s"""io.done := done_latch.io.output.data""")
      exitAccel()
      controllerStack.pop()

    case UnitPipe(ens,func) =>
      emitGlobalWireMap(src"${lhs}_II_done", "Wire(Bool())")
      val parent_kernel = controllerStack.head 
      controllerStack.push(lhs)
      if (levelOf(lhs) == OuterControl) {widthStats += lhs.children.toList.length}
      else if (levelOf(lhs) == InnerControl) {depthStats += controllerStack.length}
      emitGlobalWireMap(src"${lhs}_inhibitor", "Wire(Bool())") // hack
      emitController(lhs)
      emit(src"""${swap(lhs, CtrTrivial)} := ${DL(swap(controllerStack.tail.head, CtrTrivial), 1, true)} | false.B""")
      emitGlobalWire(src"""${swap(lhs, IIDone)} := true.B""")
      if (levelOf(lhs) == InnerControl) emitInhibitor(lhs, None, None)
      inSubGen(src"${lhs}", src"${parent_kernel}") {
        emit(s"// Controller Stack: ${controllerStack.tail}")
        visitBlock(func)
      }
      emitChildrenCxns(lhs)
      emitCopiedCChain(lhs)
      val en = if (ens.isEmpty) "true.B" else ens.map(quote).mkString(" && ")
      emit(src"${swap(lhs, Mask)} := $en")
      controllerStack.pop()


    case _ => super.gen(lhs, rhs)
  }

  override def emitFooter(): Unit = {
    // if (config.multifile == 5 | config.multifile == 6) {
    //   withStream(getStream("GlobalModules")) {
    //     emitt(src"val ic = List.fill(${instrumentCounters.length*2}){Module(new InstrumentationCounter())}")
    //   }
    // }

    emitGlobalWire(s"val max_retime = $maxretime")

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
      emitt(src"// Root controller for app: ${config.name}")
      // emitt(src"// Complete config: ${config.printer()}")
      // emitt(src"// Complete cfg: ${cfg.printer()}")
      emitt("")
      emitt(src"// Widths: ${widthStats.sorted}")
      emitt(src"//   Widest Outer Controller: ${if (widthStats.length == 0) 0 else widthStats.max}")
      emitt(src"// Depths: ${depthStats.sorted}")
      emitt(src"//   Deepest Inner Controller: ${if (depthStats.length == 0) 0 else depthStats.max}")
      emitt(s"// App Characteristics: ${appPropertyStats.toList.map(_.getClass.getName.split("\\$").last.split("\\.").last).mkString(",")}")
      emitt("// Instrumentation")
      emitt(s"val io_numArgOuts_instr = ${instrumentCounters.length*2}")
      emitt(s"val io_numArgOuts_breakpts = ${earlyExits.length}")

    }

    super.emitFooter()
  }

}