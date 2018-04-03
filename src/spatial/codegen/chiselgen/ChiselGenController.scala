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

  def emitController(sym:Sym[_], isFSM: Boolean = false): Unit = {

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
        case _ => s"Seqpipe"
      }
    }

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
      var maxw = 32 min counters.map{c => bitWidth(c.typeArgs.head)}.reduce{_*_}
      counters.zipWithIndex.map {case (ctr,i) =>
        if (ctr.isForever) {
          hasForever = true
          emitGlobalWireMap(src"${sym}_level${i}_iters", src"Wire(UInt(32.W))")
          emit(src"""${swap(src"${sym}_level${i}_iters", Blank)} := 0.U // Count forever""")
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
              emit(src"val ${sym}${i}_range = ${e} - ${s}")
              emit(src"val ${sym}${i}_jump = ${st} * ${p}")
              emit(src"val ${sym}${i}_hops = ${sym}${i}_range / ${sym}${i}_jump")
              emit(src"val ${sym}${i}_leftover = ${sym}${i}_range % ${sym}${i}_jump")
              emit(src"val ${sym}${i}_evenfit = ${sym}${i}_leftover == 0")
              emit(src"val ${sym}${i}_adjustment = if (${sym}${i}_evenfit) 0 else 1")
              emitGlobalWireMap(src"${sym}_level${i}_iters", src"Wire(UInt(${w}.W))")
              emit(src"""${swap(src"${sym}_level${i}_iters", Blank)} := (${sym}${i}_hops + ${sym}${i}_adjustment).U(${w}.W)""")
            case (Final(s), Final(e), _, Final(p)) => 
              appPropertyStats += HasVariableCtrStride
              emit("// TODO: Figure out how to make this one cheaper!")
              emit(src"val ${sym}${i}_range = ${e} - ${s}")
              emit(src"val ${sym}${i}_jump = ${ctr.step} *-* ${p}.S(${w}.W)")
              emit(src"val ${sym}${i}_hops = (${sym}${i}_range.S(${w}.W) /-/ ${sym}${i}_jump).asUInt")
              emit(src"val ${sym}${i}_leftover = ${sym}${i}_range.S(${w}.W) %-% ${sym}${i}_jump")
              emit(src"val ${sym}${i}_evenfit = ${DL(src"${sym}${i}_leftover.asUInt === 0.U", "Utils.fixeql_latency")}")
              emit(src"val ${sym}${i}_adjustment = ${DL(src"Mux(${sym}${i}_evenfit, 0.U, 1.U)", "Utils.mux_latency")}")
              emitGlobalWireMap(src"${sym}_level${i}_iters", src"Wire(UInt(${w}.W))")
              emit(src"""${swap(src"${sym}_level${i}_iters", Blank)} := ${DL(src"${sym}${i}_hops + ${sym}${i}_adjustment", src"(Utils.fixadd_latency*${sym}${i}_hops.getWidth).toInt")}.r""")
            case (_, _, Final(st), Final(p)) => 
              appPropertyStats += HasVariableCtrSyms
              emit(src"val ${sym}${i}_range =  ${DL(src"${ctr.end} - ${ctr.start}", src"(Utils.fixsub_latency*${ctr.end}.getWidth).toInt")}")
              emit(src"val ${sym}${i}_jump = ${st} * ${p}")
              emit(src"val ${sym}${i}_hops = ${sym}${i}_range /-/ ${sym}${i}_jump.FP(true, 32, 0)")
              emit(src"val ${sym}${i}_leftover = ${sym}${i}_range %-% ${sym}${i}_jump.FP(true, 32, 0)")
              emit(src"val ${sym}${i}_evenfit = ${DL(src"${sym}${i}_leftover === 0.U", "Utils.fixeql_latency")}")
              emit(src"val ${sym}${i}_adjustment = ${DL(src"Mux(${sym}${i}_evenfit, 0.U, 1.U)", "Utils.mux_latency")}")
              emitGlobalWireMap(src"${sym}_level${i}_iters", src"Wire(UInt(32.W))")
              emit(src"""${swap(src"${sym}_level${i}_iters", Blank)} := ${DL(src"${sym}${i}_hops + ${sym}${i}_adjustment", src"(Utils.fixadd_latency*${sym}${i}_hops.getWidth).toInt")}.r""")
            case _ => 
              appPropertyStats += HasVariableCtrSyms // TODO: Possible variable stride too, should probably match against this
              emit(src"val ${sym}${i}_range = ${DL(src"${ctr.end} - ${ctr.start}", src"(Utils.fixsub_latency*${ctr.end}.getWidth).toInt")}")
              emit(src"val ${sym}${i}_jump = ${ctr.step} *-* ${ctr.ctrPar}")
              emit(src"val ${sym}${i}_hops = ${sym}${i}_range /-/ ${sym}${i}_jump")
              emit(src"val ${sym}${i}_leftover = ${sym}${i}_range %-% ${sym}${i}_jump")
              emit(src"val ${sym}${i}_evenfit = ${DL(src"${sym}${i}_leftover === 0.U", "Utils.fixeql_latency")}")
              emit(src"val ${sym}${i}_adjustment = ${DL(src"Mux(${sym}${i}_evenfit, 0.U, 1.U)", "Utils.mux_latency")}")
              emitGlobalWireMap(src"${sym}_level${i}_iters", src"Wire(UInt(32.W))")
              emit(src"""${swap(src"${sym}_level${i}_iters", Blank)} := ${DL(src"${sym}${i}_hops + ${sym}${i}_adjustment", src"(Utils.fixadd_latency*${sym}${i}_hops.getWidth).toInt")}.r""")
          }
          // emit(src"""${swap(src"${sym}_level${i}_iters", Blank)} := ${DL(src"${sym}${i}_hops + ${sym}${i}_adjustment", 1)}.r""")
          src"${swap(src"${sym}_level${i}_iters", Blank)}"
        }
      }
    } else { 
      List("1.U") // Unit pipe:
    }
    // disableSplit = false
    // // Special match if this is HWblock, there is no forever cchain, just the forever flag
    // sym match {
    //   case Def(Hwblock(_,isFrvr)) => if (isFrvr) hasForever = true
    //   case _ =>;
    // }

    // val constrArg = if (isInner) {s"${isFSM}"} else {s"${childrenOf(sym).length}, isFSM = ${isFSM}"}

    // val lat = bodyLatency.sum(sym)
    // emitStandardSignals(sym)
    // createInstrumentation(sym)

    // // Pass done signal upward and grab your own en signals if this is a switchcase child
    // if (parentOf(sym).isDefined) {
    //   parentOf(sym).get match {
    //     case Def(SwitchCase(_)) => 
    //       emit(src"""${swap(parentOf(sym).get, Done)} := ${swap(sym, Done)}""")
    //       val streamAddition = getStreamEnablers(sym)
    //       emit(src"""${swap(sym, En)} := ${swap(parentOf(sym).get, En)} ${streamAddition}""")  
    //       emit(src"""${swap(sym, Resetter)} := ${swap(parentOf(sym).get, Resetter)}""")

    //     case _ =>
    //       // no sniffing to be done
    //   }
    // }

    // val stw = sym match{case Def(StateMachine(_,_,_,_,_,s)) => bitWidth(s.tp); case _ if (childrenOf(sym).length <= 1) => 3; case _ => (scala.math.log(childrenOf(sym).length) / scala.math.log(2)).toInt + 2}
    // val ctrdepth = if (cchain.isDefined) {cchain.get match {case Def(CounterChainNew(ctrs)) => ctrs.length; case _ => 0}} else 0
    // val static = if (cchain.isDefined) {
    //   cchain.get match {
    //     case Def(CounterChainNew(ctrs)) => 
    //       ctrs.map{c => c match {
    //         case Def(CounterNew(s, e, str, p)) => 
    //           val s_static = s match {
    //             case Def(RegRead(reg)) => reg match {case Def(ArgInNew(_)) => true; case _ => false}
    //             case b: Sym[_] if b.isBound => false
    //             case _ => isGlobal(s)
    //           }
    //           val e_static = e match {
    //             case Def(RegRead(reg)) => reg match {case Def(ArgInNew(_)) => true; case _ => false}
    //             case b: Sym[_] if b.isBound => false
    //             case _ => isGlobal(s)
    //           }
    //           val str_static = str match {
    //             case Def(RegRead(reg)) => reg match {case Def(ArgInNew(_)) => true; case _ => false}
    //             case b: Sym[_] if b.isBound => false
    //             case _ => isGlobal(s)
    //           }
    //           s_static && e_static && str_static
    //       }}.reduce{_&&_}
    //   }
    // } else true
    // emitGlobalRetimeMap(src"""${sym}_retime""", s"${lat}.toInt")
    // emit(s"""// This is now global: val ${quote(sym)}_retime = ${lat}.toInt // Inner loop? ${isInner}, II = ${iiOf(sym)}""")
    // emitGlobalModuleMap(src"${sym}_sm", src"Module(new ${smStr}(${constrArg.mkString}, ctrDepth = $ctrdepth, stateWidth = ${stw}, retime = ${swap(sym, Retime)}, staticNiter = $static, isReduce = $isReduce))")
    // emit(src"// This is now global: val ${swap(sym, SM)} = Module(new ${smStr}(${constrArg.mkString}, ctrDepth = $ctrdepth, stateWidth = ${stw}, retime = ${swap(sym, Retime)}, staticNiter = $static))")
    // emit(src"""${swap(sym, SM)}.io.input.enable := ${swap(sym, En)} & retime_released""")
    // if (isFSM) {
    //   emitGlobalWireMap(src"${sym}_inhibitor", "Wire(Bool())") // hacky but oh well
    //   emit(src"""${swap(sym, Done)} := ${DL(src"${swap(sym, SM)}.io.output.done & ${DL(src"~${swap(sym, Inhibitor)}", 2, true)}", swap(sym, Retime), true)}""")      
    // } else if (isStreamChild(sym)) {
    //   val streamOuts = if (getStreamInfoReady(sym).mkString(" && ").replace(" ","") != "") getStreamInfoReady(sym).mkString(" && ") else { "true.B" }
    //   emit(src"""${swap(sym, Done)} := Utils.streamCatchDone(${swap(sym, SM)}.io.output.done, $streamOuts, ${swap(sym, Retime)}, rr, accelReset) // Directly connecting *_done.D* creates a hazard on stream pipes if ~*_ready turns off for that exact cycle, since the retime reg will reject it""")
    // } else {
    //   emit(src"""${swap(sym, Done)} := Utils.risingEdge(${DL(src"${swap(sym, SM)}.io.output.done", swap(sym, Retime), true)}) // Rising edge necessary in case stall happens at same time as done comes through""")
    // }
    // emitGlobalWireMap(src"""${swap(sym, RstEn)}""", """Wire(Bool())""") // TODO: Is this legal?
    // emit(src"""${swap(sym, RstEn)} := ${swap(sym, SM)}.io.output.rst_en // Generally used in inner pipes""")
    // emit(src"""${swap(sym, SM)}.io.input.numIter := (${numIter.mkString(" *-* ")}).raw.asUInt // Unused for inner and parallel""")
    // if (spatialConfig.target.latencyModel.model("FixMul")("b" -> 32)("LatencyOf").toInt * numIter.length > maxretime) maxretime = spatialConfig.target.latencyModel.model("FixMul")("b" -> 32)("LatencyOf").toInt * numIter.length
    // emit(src"""${swap(sym, SM)}.io.input.rst := ${swap(sym, Resetter)} // generally set by parent""")

    // if (isStreamChild(sym) & hasStreamIns & beneathForever(sym)) {
    //   emit(src"""${swap(sym, DatapathEn)} := ${swap(sym, En)} & ~${swap(sym, CtrTrivial)} // Immediate parent has forever counter, so never mask out datapath_en""")    
    // } else if ((isStreamChild(sym) & hasStreamIns & cchain.isDefined)) { // for FSM or hasStreamIns, tie en directly to datapath_en
    //   emit(src"""${swap(sym, DatapathEn)} := ${swap(sym, En)} & ~${swap(sym, Done)} & ~${swap(sym, CtrTrivial)} ${getNowValidLogic(sym)}""")  
    // } else if (isFSM) { // for FSM or hasStreamIns, tie en directly to datapath_en
    //   emit(src"""${swap(sym, DatapathEn)} := ${swap(sym, En)} & ~${swap(sym, Done)} & ~${swap(sym, CtrTrivial)} & ~${swap(sym, SM)}.io.output.done ${getNowValidLogic(sym)}""")  
    // } else if ((isStreamChild(sym) & hasStreamIns)) { // _done used to be commented out but I'm not sure why
    //   emit(src"""${swap(sym, DatapathEn)} := ${swap(sym, En)} & ~${swap(sym, Done)} & ~${swap(sym, CtrTrivial)} ${getNowValidLogic(sym)} """)  
    // } else {
    //   emit(src"""${swap(sym, DatapathEn)} := ${swap(sym, SM)}.io.output.ctr_inc & ~${swap(sym, Done)} & ~${swap(sym, CtrTrivial)} ${getReadyLogic(sym)}""")
    // }
    
    // /* Counter Signals for controller (used for determining done) */
    // if (smStr != "Parallel" & smStr != "Streampipe") {
    //   if (cchain.isDefined) {
    //     if (!isForever(cchain.get)) {
    //       emitGlobalWireMap(src"""${swap(cchain.get, En)}""", """Wire(Bool())""") 
    //       sym match { 
    //         case Def(n: UnrolledReduce[_,_]) => // These have II
    //           emit(src"""${swap(cchain.get, En)} := ${swap(sym, SM)}.io.output.ctr_inc & ${swap(sym, IIDone)}""")
    //         case Def(n: UnrolledForeach) => 
    //           if (isStreamChild(sym) & hasStreamIns) {
    //             emit(src"${swap(cchain.get, En)} := ${swap(sym, DatapathEn)} & ${swap(sym, IIDone)} & ~${swap(sym, Inhibitor)} ${getNowValidLogic(sym)}") 
    //           } else {
    //             emit(src"${swap(cchain.get, En)} := ${swap(sym, SM)}.io.output.ctr_inc & ${swap(sym, IIDone)}// Should probably also add inhibitor")
    //           }             
    //         case _ => // If parent is stream, use the fine-grain enable, otherwise use ctr_inc from sm
    //           if (isStreamChild(sym) & hasStreamIns) {
    //             emit(src"${swap(cchain.get, En)} := ${swap(sym, DatapathEn)} & ~${swap(sym, Inhibitor)} ${getNowValidLogic(sym)}") 
    //           } else {
    //             emit(src"${swap(cchain.get, En)} := ${swap(sym, SM)}.io.output.ctr_inc // Should probably also add inhibitor")
    //           } 
    //       }
    //       emit(src"""// ---- Counter Connections for $smStr ${sym} (${cchain.get}) ----""")
    //       val ctr = cchain.get
    //       if (isStreamChild(sym) & hasStreamIns) {
    //         emit(src"""${swap(ctr, Resetter)} := ${DL(swap(sym, Done), 1, true)} // Do not use rst_en for stream kiddo""")
    //       } else {
    //         emit(src"""${swap(ctr, Resetter)} := ${DL(swap(sym, RstEn), 0, true)} // changed on 9/19""")
    //       }
    //       if (isInner) { 
    //         // val dlay = if (spatialConfig.enableRetiming || spatialConfig.enablePIRSim) {src"1 + ${swap(sym, Retime)}"} else "1"
    //         emit(src"""${swap(sym, SM)}.io.input.ctr_done := ${DL(swap(ctr, Done), 1, true)}""")
    //       }

    //     }
    //   } else {
    //     emit(src"""// ---- Single Iteration for $smStr ${sym} ----""")
    //     if (isInner) { 
    //       emitGlobalWireMap(src"${sym}_ctr_en", "Wire(Bool())")
    //       if (isStreamChild(sym) & hasStreamIns) {
    //         emit(src"""${swap(sym, SM)}.io.input.ctr_done := ${DL(src"${swap(sym, En)} & ~${swap(sym, Done)}", 1, true)} // Disallow consecutive dones from stream inner""")
    //         emit(src"""${swap(sym, CtrEn)} := ${swap(sym, Done)} // stream kiddo""")
    //       } else {
    //         emit(src"""${swap(sym, SM)}.io.input.ctr_done := ${DL(src"Utils.risingEdge(${swap(sym, SM)}.io.output.ctr_inc)", 1, true)}""")
    //         emit(src"""${swap(sym, CtrEn)} := ${swap(sym, SM)}.io.output.ctr_inc""")            
    //       }
    //     } else {
    //       emit(s"// TODO: How to properly emit for non-innerpipe unit counter?  Probably doesn't matter")
    //     }
    //   }
    // }

    // val hsi = if (isInner & hasStreamIns) "true.B" else "false.B"
    // val hf = if (hasForever) "true.B" else "false.B"
    // emit(src"${swap(sym, SM)}.io.input.hasStreamIns := $hsi")
    // emit(src"${swap(sym, SM)}.io.input.forever := $hf")

    // // emitChildrenCxns(sym, cchain, iters, isFSM)
    // /* Create reg chain mapping */
    // if (iters.isDefined) {
    //   if (smStr == "Metapipe" & childrenOf(sym).length > 1) {
    //     childrenOf(sym).foreach{ 
    //       case stage @ Def(s:UnrolledForeach) => cchainPassMap += (s.cchain -> stage)
    //       case stage @ Def(s:UnrolledReduce[_,_]) => cchainPassMap += (s.cchain -> stage)
    //       case _ =>
    //     }
    //     iters.get.foreach{ idx => 
    //       itersMap += (idx -> childrenOf(sym).toList)
    //     }

    //   }
    // }
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {

    case AccelScope(func) =>
      hwblock = Some(lhs)
      controllerStack.push(lhs)
      if (levelOf(lhs) == OuterControl) {widthStats += lhs.toCtrl.children.length}
      else if (levelOf(lhs) == InnerControl) {depthStats += controllerStack.length}
      enterAccel()
      val valids = getReadStreams(lhs.toCtrl)
      val readies = getWriteStreams(lhs.toCtrl)
      emitController(lhs)
      // emitGlobalWire(src"val accelReset = reset.toBool | io.reset")
      // emitt(s"""${swap(lhs, En)} := io.enable & !io.done ${streamAddition}""")
      // emitt(s"""${swap(lhs, Resetter)} := Utils.getRetimed(accelReset, 1)""")
      // emitt(src"""${swap(lhs, CtrTrivial)} := false.B""")
      // emitGlobalWireMap(src"""${lhs}_II_done""", """Wire(Bool())""")
      // if (iiOf(lhs) <= 1) {
      //   emitt(src"""${swap(lhs, IIDone)} := true.B""")
      // } else {
      //   emitt(src"""val ${lhs}_IICtr = Module(new RedxnCtr(2 + Utils.log2Up(${swap(lhs, Retime)})));""")
      //   emitt(src"""${swap(lhs, IIDone)} := ${lhs}_IICtr.io.output.done | ${swap(lhs, CtrTrivial)}""")
      //   emitt(src"""${lhs}_IICtr.io.input.enable := ${swap(lhs,En)}""")
      //   emitt(src"""${lhs}_IICtr.io.input.stop := ${iiOf(lhs)}.toInt.S // ${swap(lhs, Retime)}.S""")
      //   emitt(src"""${lhs}_IICtr.io.input.reset := accelReset | ${DL(swap(lhs, IIDone), 1, true)}""")
      //   emitt(src"""${lhs}_IICtr.io.input.saturate := false.B""")       
      // }
      // emitt(src"""val retime_counter = Module(new SingleCounter(1, Some(0), Some(max_retime), Some(1), Some(0))) // Counter for masking out the noise that comes out of ShiftRegister in the first few cycles of the app""")
      // // emitt(src"""retime_counter.io.input.start := 0.S; retime_counter.io.input.stop := (max_retime.S); retime_counter.io.input.stride := 1.S; retime_counter.io.input.gap := 0.S""")
      // emitt(src"""retime_counter.io.input.saturate := true.B; retime_counter.io.input.reset := accelReset; retime_counter.io.input.enable := true.B;""")
      // emitGlobalWire(src"""val retime_released_reg = RegInit(false.B)""")
      // emitGlobalWire(src"""val retime_released = ${DL("retime_released_reg", 1)}""")
      // emitGlobalWire(src"""val rr = retime_released // Shorthand""")
      // emitt(src"""retime_released := ${DL("retime_counter.io.output.done",1)} // break up critical path by delaying this """)
      // topLayerTraits = lhs.children.map { c => src"$c" }
      // if (levelOf(lhs) == InnerControl) emitInhibitor(lhs, None, None, None)

      // emitBlock(func)
      // emitChildrenCxns(lhs, None, None)
      // emitCopiedCChain(lhs)

      // emitt(s"""val done_latch = Module(new SRFF())""")
      // if (earlyExits.length > 0) {
      //   appPropertyStats += HasBreakpoint
      //   emitGlobalWire(s"""val breakpoints = Wire(Vec(${earlyExits.length}, Bool()))""")
      //   emitt(s"""done_latch.io.input.set := ${swap(lhs, Done)} | breakpoints.reduce{_|_}""")        
      // } else {
      //   emitt(s"""done_latch.io.input.set := ${swap(lhs, Done)}""")                
      // }
      // emitt(s"""done_latch.io.input.reset := ${swap(lhs, Resetter)}""")
      // emitt(s"""done_latch.io.input.asyn_reset := ${swap(lhs, Resetter)}""")
      // emitt(s"""io.done := done_latch.io.output.data""")
      exitAccel()
      controllerStack.pop()

    case _ => super.gen(lhs, rhs)
  }

  override def emitFooter(): Unit = {
    // if (config.multifile == 5 | config.multifile == 6) {
    //   withStream(getStream("GlobalModules")) {
    //     emit(src"val ic = List.fill(${instrumentCounters.length*2}){Module(new InstrumentationCounter())}")
    //   }
    // }

    inGenn(out, "GlobalModules", ext) {
      emitt(src"val breakpt_activators = List.fill(${earlyExits.length}){Wire(Bool())}")
    }

    inGen(out, "Instantiator.scala") {
      emit("")
      emit("// Instrumentation")
      emit(s"val numArgOuts_instr = ${instrumentCounters.length*2}")
      instrumentCounters.zipWithIndex.foreach { case(p,i) =>
        val depth = " "*p._2
        emit(src"""// ${depth}${quote(p._1)}""")
      }
      emit(s"val numArgOuts_breakpts = ${earlyExits.length}")
      emit("""/* Breakpoint Contexts:""")
      earlyExits.zipWithIndex.foreach {case (p,i) => 
        createBreakpoint(p, i)
        emit(s"breakpoint ${i}: ${p.ctx}")
      }
      emit("""*/""")
    }

    inGenn(out, "IOModule", ext) {
      emitt(src"// Root controller for app: ${config.name}")
      // emitt(src"// Complete config: ${config.printer()}")
      // emitt(src"// Complete spatialConfig: ${spatialConfig.printer()}")
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