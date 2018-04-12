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
    emitGlobalWireMap(src"""${swap(lhs, IIDone)}""", """Wire(Bool())""")
    emitGlobalWireMap(src"""${swap(lhs, Inhibitor)}""", """Wire(Bool())""")
    emitGlobalWireMap(src"""${lhs}_mask""", """Wire(Bool())""")
    emitGlobalWireMap(src"""${lhs}_resetter""", """Wire(Bool())""")
    emitGlobalWireMap(src"""${lhs}_datapath_en""", """Wire(Bool())""")
    emitGlobalWireMap(src"""${lhs}_ctr_trivial""", """Wire(Bool())""")
  }

  final private def enterCtrl(lhs: Sym[_]): Sym[_] = {
      val parent = if (controllerStack.isEmpty) lhs else controllerStack.head 
      controllerStack.push(lhs)
      if (levelOf(lhs) == OuterControl) {widthStats += lhs.children.toList.length}
      else if (levelOf(lhs) == InnerControl) {depthStats += controllerStack.length}

      // Tree stuff

      parent
  }

  final private def exitCtrl(lhs: Sym[_]): Unit = {
    // Tree stuff

    controllerStack.pop()
  }

  final private def allocateValids(lhs: Sym[_], cchain: Sym[CounterChain], iters: Seq[Seq[Sym[_]]], valids: Seq[Seq[Sym[_]]], suffix: String = ""): Unit = {
    // Need to recompute ctr data because of multifile 5
    valids.zip(iters).zipWithIndex.foreach{ case ((layer,count), i) =>
      layer.zip(count).foreach{ case (v, c) =>
        // emitGlobalWire(s"//${validPassMap}")
        emitGlobalModuleMap(src"${v}${suffix}","Wire(Bool())")  
        if (styleOf(lhs) == Sched.Pipe & lhs.children.length > 1) {
          lhs.children.indices.drop(1).foreach{i => emitGlobalModuleMap(src"""${v}${suffix}_chain_read_$i""", "Wire(Bool())")}
        }
      }
    }
    // Console.println(s"map is $validPassMap")
  }

  final private def allocateRegChains(lhs: Sym[_], inds:Seq[Sym[_]], cchain:Sym[CounterChain]): Unit = {
    val stages = lhs.children.map(_.s.get)
    val Op(CounterChainNew(counters)) = cchain
    val par = counters.map(_.ctrPar)
    val ctrMapping = par.indices.map{i => par.dropRight(par.length - i).map(_.toInt).sum}
    inds.zipWithIndex.foreach { case (idx,index) =>
      val this_counter = ctrMapping.filter(_ <= index).length - 1
      val this_width = bitWidth(counters(this_counter).typeArgs.head)
      emitGlobalModuleMap(src"""${idx}_chain""", src"""Module(new NBufFF(${stages.size}, ${this_width}))""")
      stages.indices.foreach{i => emitGlobalModuleMap(src"""${idx}_chain_read_$i""", src"Wire(UInt(${this_width}.W))"); emitGlobalModule(src"""${swap(src"${idx}_chain_read_$i", Blank)} := ${swap(idx, Chain)}.read(${i})""")}
    }
  }

  private final def emitRegChains(lhs: Sym[_]) = {
    val stages = lhs.children.toList.map(_.s.get)
    val Op(CounterChainNew(counters)) = lhs.cchains.head
    val par = lhs.cchains.head.pars
    val ctrMapping = par.indices.map{i => par.dropRight(par.length - i).map(_.toInt).sum}
    ctrlIters(lhs.toCtrl).toList.zipWithIndex.foreach { case (idx,index) =>
      val ctr = ctrMapping.filter(_ <= index).length - 1
      val w = bitWidth(counters(ctr).typeArgs.head)
      inGenn(out, "BufferControlCxns", ext) {
        stages.zipWithIndex.foreach{ case (s, i) =>
          emitGlobalWireMap(src"${s}_done", "Wire(Bool())")
          emitGlobalWireMap(src"${s}_en", "Wire(Bool())")
          emitt(src"""${swap(idx, Chain)}.connectStageCtrl(${DL(swap(s, Done), 0, true)}, ${swap(s, En)}, List($i)) // Used to be delay of 1 on Nov 26, 2017 but not sure why""")
        }
      }
      emitt(src"""${swap(idx, Chain)}.chain_pass(${idx}, ${swap(lhs, SM)}.io.output.ctr_inc)""")
      // Associate bound sym with both ctrl node and that ctrl node's cchain
    }
  }

  final private def emitValids(lhs: Sym[_], cchain: Sym[CounterChain], iters: Seq[Seq[Sym[_]]], valids: Seq[Seq[Sym[_]]], suffix: String = ""): Unit = {
    // Need to recompute ctr data because of multifile 5
    val Op(CounterChainNew(ctrs)) = cchain
    val counter_data = ctrs.map{ ctr => ctr match {
      case Op(CounterNew(start, end, step, par)) => 
        val w = bitWidth(ctr.typeArgs.head)
        (start,end) match { 
          case (Final(s), Final(e)) => (src"${s}.FP(true, $w, 0)", src"${e}.FP(true, $w, 0)", src"$step", {src"$par"}.split('.').take(1)(0), src"$w")
          case _ => (src"$start", src"$end", src"$step", {src"$par"}.split('.').take(1)(0), src"$w")
        }
      case Op(ForeverNew()) => 
        ("0.S", "999.S", "1.S", "1", "32") 
    }}

    valids.zip(iters).zipWithIndex.foreach{ case ((layer,count), i) =>
      layer.zip(count).foreach{ case (v, c) =>
        // // Handled by allocatevalids
        // if (suffix == "") {
        //   emitGlobalModuleMap(src"${v}","Wire(Bool())")  
        // } else {
        //   emitGlobalModule(src"val ${v}${suffix} = Wire(Bool())")
        // }
        emitt(src"${swap(src"${v}${suffix}", Blank)} := Mux(${counter_data(i)._3} >= 0.S, ${swap(src"${c}${suffix}", Blank)} < ${counter_data(i)._2}, ${swap(src"${c}${suffix}", Blank)} > ${counter_data(i)._2}) // TODO: Generate these inside counter")
        if (styleOf(lhs) == Sched.Pipe & lhs.children.length > 1) {
          emitGlobalModuleMap(src"""${swap(src"${swap(src"${v}${suffix}", Blank)}", Chain)}""",src"""Module(new NBufFF(${lhs.children.size}, 1))""")
          lhs.children.indices.drop(1).foreach{i => emitGlobalModule(src"""${swap(src"${swap(src"${v}${suffix}", Blank)}_chain_read_$i", Blank)} := ${swap(src"${swap(src"${v}${suffix}", Blank)}", Chain)}.read(${i}) === 1.U(1.W)""")}
          inGenn(out, "BufferControlCxns", ext) {
            lhs.children.zipWithIndex.foreach{ case (s, i) =>
              emitGlobalWireMap(src"${s}_done", "Wire(Bool())")
              emitGlobalWireMap(src"${s}_en", "Wire(Bool())")
              emitt(src"""${swap(src"${swap(src"${v}${suffix}", Blank)}", Chain)}.connectStageCtrl(${DL(swap(s, Done), 1, true)}, ${swap(s,En)}, List($i))""")
            }
          }
          emitt(src"""${swap(src"${swap(src"${v}${suffix}", Blank)}", Chain)}.chain_pass(${swap(src"${v}${suffix}", Blank)}, ${swap(lhs, SM)}.io.output.ctr_inc)""")
        }
      }
    }
    // Console.println(s"map is $validPassMap")
  }

  protected def connectCtrTrivial(lhs: Sym[_], suffix: String = ""): Unit = {
    val ctrl = ctrlNodeOf(lhs)
    if (suffix != "") { // emitting for a copied ctr
      emit(src"// this trivial signal will be assigned multiple times but each should be the same")
      emit(src"""${swap(ctrl, CtrTrivial)} := ${DL(swap(controllerStack.tail.head, CtrTrivial), 1, true)} | ${lhs}${suffix}_stops.zip(${lhs}${suffix}_starts).map{case (stop,start) => (stop === start)}.reduce{_||_}""")
    } else {
      emit(src"""${swap(ctrl, CtrTrivial)} := ${DL(swap(controllerStack.tail.head, CtrTrivial), 1, true)} | ${lhs}${suffix}_stops.zip(${lhs}${suffix}_starts).map{case (stop,start) => (stop === start)}.reduce{_||_}""")
    }
  }


  final private def createValidsPassMap(lhs: Sym[_], cchain: Sym[CounterChain], iters: Seq[Seq[Sym[_]]], valids: Seq[Seq[Sym[_]]], suffix: String = ""): Unit = {
    if (levelOf(lhs) != InnerControl) {
      valids.zip(iters).zipWithIndex.foreach{ case ((layer,count), i) =>
        layer.zip(count).foreach{ case (v, c) =>
          validPassMap += ((v, suffix) -> lhs.children.map(_.s.get))
        }
      }
    }
  }

  final private def emitValidsDummy(iters: Seq[Seq[Sym[_]]], valids: Seq[Seq[Sym[_]]], suffix: String = ""): Unit = {
    valids.zip(iters).zipWithIndex.foreach{ case ((layer,count), i) =>
      layer.zip(count).foreach{ case (v, c) =>
        emitt(src"val ${v}${suffix} = true.B")
      }
    }
  }

  final private def emitParallelizedLoop(iters: Seq[Seq[Sym[_]]], cchain: Sym[CounterChain], suffix: String = "") = {
    val Op(CounterChainNew(counters)) = cchain

    iters.zipWithIndex.foreach{ case (is, i) =>
      val w = bitWidth(counters(i).typeArgs.head)
      if (is.size == 1) { // This level is not parallelized, so assign the iter as-is  
        emitGlobalWireMap(src"${is(0)}${suffix}", src"Wire(new FixedPoint(true,$w,0))")
        // if (suffix == "") emitGlobalWireMap(src"${is(0)}", src"Wire(new FixedPoint(true,$w,0))") else emitGlobalWire(src"val ${is(0)}${suffix} = Wire(new FixedPoint(true,$w,0))")
        emitt(src"${swap(src"${is(0)}${suffix}", Blank)}.raw := ${counters(i)}${suffix}(0).r")
      } else { // This level IS parallelized, index into the counters correctly
        is.zipWithIndex.foreach{ case (iter, j) =>
          emitGlobalWireMap(src"${iter}${suffix}", src"Wire(new FixedPoint(true,$w,0))")
          // if (suffix == "") emitGlobalWireMap(src"${iter}", src"Wire(new FixedPoint(true,$w,0))") else emitGlobalWire(src"val ${iter}${suffix} = Wire(new FixedPoint(true,$w,0))")
          emitt(src"${swap(src"${iter}${suffix}", Blank)}.raw := ${counters(i)}${suffix}($j).r")
        }
      }
    }
  }


  protected def emitCopiedCChain(self: Sym[_]): Unit = {
    val parent = self.parent.s.get
    if (parent != Host) {
      if (levelOf(parent) != InnerControl && styleOf(parent) == Sched.Stream) {
        emitCounterChain(self, src"_copy${self}")
      }
    }

  }

  protected def emitChildrenCxns(sym:Sym[_], isFSM: Boolean = false): Unit = {
    val hasStreamIns = !getReadStreams(sym.toCtrl).isEmpty
    val isInner = levelOf(sym) == InnerControl

    /* Control Signals to Children Controllers */
    if (!isInner) {
      emitt(src"""// ---- Begin ${styleOf(sym)} ${sym} Children Signals ----""")
      sym.children.toList.zipWithIndex.foreach { case (c, idx) =>
        if (styleOf(sym) == Sched.Stream & !sym.cchains.isEmpty) {
          emitt(src"""${swap(sym, SM)}.io.input.stageDone(${idx}) := ${swap(src"${sym.cchains.head}_copy${c}", Done)};""")
        } else {
          emitt(src"""${swap(sym, SM)}.io.input.stageDone(${idx}) := ${swap(c, Done)};""")
        }

        emitt(src"""${swap(sym, SM)}.io.input.stageMask(${idx}) := ${swap(c, Mask)};""")

        val streamAddition = getStreamEnablers(c.s.get)

        val base_delay = if (cfg.enableTightControl) 0 else 1
        emitt(src"""${swap(c, BaseEn)} := ${DL(src"${swap(sym, SM)}.io.output.stageEnable(${idx})", base_delay, true)} & ${DL(src"~${swap(c, Done)}", 1, true)}""")  
        emitt(src"""${swap(c, En)} := ${swap(c, BaseEn)} ${streamAddition}""")  

        // If this is a stream controller, need to set up counter copy for children
        if (styleOf(sym) == Sched.Stream & !sym.cchains.isEmpty) {
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
          emitt(src"""${swap(src"${sym.cchains.head}_copy${c}", En)} := ${signalHandle}""")
          emitt(src"""${swap(src"${sym.cchains.head}_copy${c}", Resetter)} := ${DL(src"${swap(sym, SM)}.io.output.rst_en", 1, true)}""")
        }
        if (c match { case Op(_: StateMachine[_]) => true; case _ => false}) { // If this is an fsm, we want it to reset with each iteration, not with the reset of the parent
          emitt(src"""${swap(c, Resetter)} := ${DL(src"${swap(sym, SM)}.io.output.rst_en", 1, true)} | ${DL(swap(c, Done), 1, true)}""") //changed on 12/13
        } else {
          emitt(src"""${swap(c, Resetter)} := ${DL(src"${swap(sym, SM)}.io.output.rst_en", 1, true)}""")   //changed on 12/13
        }
        
      }
    }
    /* Emit reg chains */
    if (!ctrlIters(sym.toCtrl).isEmpty) {
      if (styleOf(sym) == Sched.Pipe & sym.children.toList.length > 1) {
        emitRegChains(sym)
      }
    }

  }

  def emitController(sym:Sym[_], isFSM: Boolean = false): Unit = {
    val isInner = levelOf(sym) == InnerControl

    emitt(src"""//  ---- ${levelOf(sym)}: Begin ${styleOf(sym)} $sym Controller ----""")
    val constrArg = if (levelOf(sym) == InnerControl) {s"${isFSM}"} else {s"${sym.children.length}, isFSM = ${isFSM}"}

    val lat = 0// bodyLatency.sum(sym) // FIXME
    emitControlSignals(sym)
    createInstrumentation(sym)

    val stw = sym match{case Op(x: StateMachine[_]) => bitWidth(sym.tp.typeArgs.head); case _ if (sym.children.length <= 1) => 3; case _ => (scala.math.log(sym.children.length) / scala.math.log(2)).toInt + 2}
    emitGlobalRetimeMap(src"""${sym}_retime""", s"${lat}.toInt")
    emitGlobalModuleMap(src"${sym}_sm", src"Module(new ${levelOf(sym)}(${styleOf(sym)}, ${constrArg.mkString}, stateWidth = ${stw}))")
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
    emitt(src"""${swap(sym, SM)}.io.input.rst := ${swap(sym, Resetter)} // generally set by parent""")

    val hasStreamIns = !getReadStreams(sym.toCtrl).isEmpty
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
    if (styleOf(sym) != Sched.ForkJoin & styleOf(sym) != Sched.Stream) {
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
          emitt(src"""// ---- Counter Connections for ${styleOf(sym)} ${sym} (${sym.cchains.head}) ----""")
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
        emitt(src"""// ---- Single Iteration for ${styleOf(sym)} ${sym} ----""")
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

    // emitChildrenCxns(sym, cchain, iters, isFSM)
    /* Create reg chain mapping */
    if (!ctrlIters(sym.toCtrl).isEmpty) {
      if (styleOf(sym) == Sched.Pipe & sym.children.length > 1) {
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
    emitCounterChain(sym)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {

    case AccelScope(func) =>
      hwblock = Some(enterCtrl(lhs))
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
      exitCtrl(lhs)

    case UnitPipe(ens,func) =>
      // emitGlobalWireMap(src"${lhs}_II_done", "Wire(Bool())")
      // emitGlobalWireMap(src"${lhs}_inhibitor", "Wire(Bool())") // hack
      val parent_kernel = enterCtrl(lhs)
      emitController(lhs)
      emitt(src"""${swap(lhs, CtrTrivial)} := ${DL(swap(parent_kernel, CtrTrivial), 1, true)} | false.B""")
      emitGlobalWire(src"""${swap(lhs, IIDone)} := true.B""")
      if (levelOf(lhs) == InnerControl) emitInhibitor(lhs, None, None)
      inSubGen(src"${lhs}", src"${parent_kernel}") {
        emitt(s"// Controller Stack: ${controllerStack.tail}")
        visitBlock(func)
      }
      emitChildrenCxns(lhs)
      emitCopiedCChain(lhs)
      val en = if (ens.isEmpty) "true.B" else ens.map(quote).mkString(" && ")
      emitt(src"${swap(lhs, Mask)} := $en")
      exitCtrl(lhs)

    case UnrolledForeach(ens,cchain,func,iters,valids) =>
      val parent_kernel = enterCtrl(lhs)
      emitController(lhs) // If this is a stream, then each child has its own ctr copy
      if (levelOf(lhs) == InnerControl) emitInhibitor(lhs, None, None)
      if (iiOf(lhs) <= 1) {
        emitt(src"""${swap(lhs, IIDone)} := true.B""")
      } else {
        emitGlobalModule(src"""val ${lhs}_IICtr = Module(new RedxnCtr(2 + Utils.log2Up(${swap(lhs, Retime)})));""")
        emitt(src"""${swap(lhs, IIDone)} := ${lhs}_IICtr.io.output.done | ${swap(lhs, CtrTrivial)}""")
        emitt(src"""${lhs}_IICtr.io.input.enable := ${swap(lhs, DatapathEn)}""")
        emitt(src"""${lhs}_IICtr.io.input.stop := ${swap(lhs, Retime)}.S //${iiOf(lhs)}.S""")
        emitt(src"""${lhs}_IICtr.io.input.reset := accelReset | ${DL(swap(lhs, IIDone), 1, true)}""")
        emitt(src"""${lhs}_IICtr.io.input.saturate := false.B""")       
      }
      if (styleOf(lhs) == Sched.Pipe | styleOf(lhs) == Sched.Seq) {
        if (styleOf(lhs) == Sched.Pipe) createValidsPassMap(lhs, cchain, iters, valids)
        inSubGen(src"${lhs}", src"${parent_kernel}") {
          emitt(s"// Controller Stack: ${controllerStack.tail}")
          emitParallelizedLoop(iters, cchain)
          allocateValids(lhs, cchain, iters, valids)
          if (styleOf(lhs) == Sched.Pipe & lhs.children.length > 1) allocateRegChains(lhs, iters.flatten, cchain) // Needed to generate these global wires before visiting children who may use them
          visitBlock(func)
        }
        emitValids(lhs, cchain, iters, valids)
      } else if (styleOf(lhs) == Sched.Stream) {
        // Indicate that the valids and iters for this UnrForeach must be suffix-ized for children
        valids.flatten.foreach{ v => streamCtrCopy = streamCtrCopy :+ v }
        iters.flatten.foreach{ iter => streamCtrCopy = streamCtrCopy :+ iter }

        inSubGen(src"${lhs}", src"${parent_kernel}") {
          emitt(s"// Controller Stack: ${controllerStack.tail}")
          lhs.children.zipWithIndex.foreach { case (c, idx) =>
            emitParallelizedLoop(iters, cchain, src"_copy$c")
          }
          if (lhs.children.length > 0) {
            lhs.children.zipWithIndex.foreach { case (c, idx) =>
              allocateValids(lhs, cchain, iters, valids, src"_copy$c") // Must have visited func before we can properly run this method
            }          
          } else {
            emitValidsDummy(iters, valids, src"_copy$lhs") // FIXME: Weird situation with nested stream ctrlrs, hacked quickly for tian so needs to be fixed
          }
          // Register the remapping for bound syms in children
          visitBlock(func)
        }
        if (lhs.children.length > 0) {
          lhs.children.zipWithIndex.foreach { case (c, idx) =>
            emitValids(lhs, cchain, iters, valids, src"_copy$c") // Must have visited func before we can properly run this method
          }          
        }
      }
      emitChildrenCxns(lhs)
      emitCopiedCChain(lhs)
      if (!(styleOf(lhs) == Sched.Stream && lhs.children.length > 0)) {
        connectCtrTrivial(cchain)
      }
      val en = if (ens.isEmpty) "true.B" else ens.map(quote).mkString(" && ")
      emitt(src"${swap(lhs, Mask)} := $en")
      exitCtrl(lhs)


    case _ => super.gen(lhs, rhs)
  }

  override def emitFooter(): Unit = {
    if (cfg.compressWires >= 1) {
      inGenn(out, "GlobalModules", ext) {
        emitt(src"val ic = List.fill(${instrumentCounters.length*2}){Module(new InstrumentationCounter())}")
      }
    }

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