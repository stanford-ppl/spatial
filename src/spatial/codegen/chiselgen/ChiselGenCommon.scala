package spatial.codegen.chiselgen

import argon._
import argon.codegen.Codegen
import spatial.lang._
import spatial.node._
import spatial.data._
import spatial.util._
import emul.FloatPoint
import emul.FixedPoint
import utils.escapeString
import spatial.internal.{spatialConfig => cfg}

trait ChiselGenCommon extends ChiselCodegen { 

  // Statistics counters
  // var itersMap = new scala.collection.mutable.HashMap[Bound[_], List[Sym[_]]]
  var controllerStack = scala.collection.mutable.Stack[Sym[_]]()
  var cchainPassMap = new scala.collection.mutable.HashMap[Sym[_], Sym[_]] // Map from a cchain to its ctrl node, for computing suffix on a cchain before we enter the ctrler
  var validPassMap = new scala.collection.mutable.HashMap[(Sym[_], String), Seq[Sym[_]]] // Map from a valid bound sym to its ctrl node, for computing suffix on a valid before we enter the ctrler
  var accumsWithIIDlay = new scala.collection.mutable.ListBuffer[Sym[_]]
  var widthStats = new scala.collection.mutable.ListBuffer[Int]
  var depthStats = new scala.collection.mutable.ListBuffer[Int]
  var appPropertyStats = Set[AppProperties]()

  // Interface
  var argOuts = scala.collection.mutable.HashMap[Sym[_], Int]()
  var argIOs = scala.collection.mutable.HashMap[Sym[_], Int]()
  var argIns = scala.collection.mutable.HashMap[Sym[_], Int]()
  var argOutLoopbacks = scala.collection.mutable.HashMap[Int, Int]() // info about how to wire argouts back to argins in Fringe
  var drams = scala.collection.mutable.HashMap[Sym[_], Int]()

  def getReadStreams(ctrl: Ctrl): Set[Sym[_]] = {
    // ctrl.children.flatMap(getReadStreams).toSet ++
    localMems.all.filter{mem => readersOf(mem).exists{_.parent == ctrl }}
                 .filter{mem => mem.isStreamIn || mem.isFIFO }
                 // .filter{case Op(StreamInNew(bus)) => !bus.isInstanceOf[DRAMBus[_]]; case _ => true}
  }

  def getWriteStreams(ctrl: Ctrl): Set[Sym[_]] = {
    // ctrl.children.flatMap(getWriteStreams).toSet ++
    localMems.all.filter{mem => writersOf(mem).exists{_.parent == ctrl }}
                 .filter{mem => mem.isStreamOut || mem.isFIFO }
                 // .filter{case Op(StreamInNew(bus)) => !bus.isInstanceOf[DRAMBus[_]]; case _ => true}
  }

  def latencyOption(op: String, b: Option[Int]): Double = {
    if (cfg.enableRetiming) {
      // if (b.isDefined) {cfg.target.latencyModel.model(op)("b" -> b.get)("LatencyOf")}
      // else cfg.target.latencyModel.model(op)()("LatencyOf") 
      0.0 // FIXME
    } else {
      0.0
    }
  }

  def latencyOptionString(op: String, b: Option[Int]): String = {
    if (cfg.enableRetiming) {
      val latency = latencyOption(op, b)
      if (b.isDefined) {
        s"""Some(${latency})"""
      } else {
        s"""Some(${latency})"""
      }
    } else {
      "None"      
    }
  }

  protected def isStreamChild(lhs: Sym[_]): Boolean = {
    var nextLevel: Option[Sym[_]] = Some(lhs)
    var result = false
    while (nextLevel.isDefined) {
      if (styleOf(nextLevel.get) == Sched.Stream) {
        result = true
        nextLevel = None
      } else {
        if (nextLevel.get.parent.s.isDefined) nextLevel = Some(nextLevel.get.parent.s.get)
        else nextLevel = None
      }
    }
    result
  }

  protected def beneathForever(lhs: Sym[_]):Boolean = { // TODO: Make a counterOf() method that will just grab me Some(counter) that I can check
    if (lhs.parent != Host) {
      if (lhs.parent.s.get.isForever) true
      else beneathForever(lhs.parent.s.get)
    } else {
      false
    }
  }

  protected def enableRetimeMatch(en: Sym[_], lhs: Sym[_]): Double = { // With partial retiming, the delay on standard signals needs to match the delay of the enabling input, not necessarily the symDelay(lhs) if en is delayed partially
    // val last_def_delay = en match {
    //   case Def(And(_,_)) => latencyOption("And", None)
    //   case Def(Or(_,_)) => latencyOption("Or", None)
    //   case Def(Not(_)) => latencyOption("Not", None)
    //   case Const(_) => 0.0
    //   case Def(DelayLine(size,_)) => size.toDouble // Undo subtraction
    //   case Def(RegRead(_)) => latencyOption("RegRead", None)
    //   case Def(FixEql(a,_)) => latencyOption("FixEql", Some(bitWidth(a.tp)))
    //   case b: Bound[_] => 0.0
    //   case _ => throw new Exception(s"Node enable $en not yet handled in partial retiming")
    // }
    // // if (spatialConfig.enableRetiming) symDelay(en) + last_def_delay else 0.0
    // if (spatialConfig.enableRetiming) symDelay(lhs) else 0.0
    0.0 // FIXME
  }

  def emitCounterChain(lhs: CounterChain, suffix: String = ""): Unit = {
    val ctrs = lhs.ctrs.toList
    var isForever = false
    // Temporarily shove ctrl node onto stack so the following is quoted properly
    if (cchainPassMap.contains(lhs)) {controllerStack.push(cchainPassMap(lhs))}
    val counter_data = ctrs.map{ ctr => ctr match {
      case Op(CounterNew(start, end, step, par)) => 
        val w = bitWidth(ctr.typeArgs.head)
        (start,end) match { 
          case (Final(s), Final(e)) => (src"${s}.FP(true, $w, 0)", src"${e}.FP(true, $w, 0)", src"$step", {src"$par"}.split('.').take(1)(0), src"$w")
          case _ => (src"$start", src"$end", src"$step", {src"$par"}.split('.').take(1)(0), src"$w")
        }
      case Op(ForeverNew()) => 
        isForever = true
        ("0.S", "999.S", "1.S", "1", "32") 
    }}
    // TODO: Combine the below with the above, just monkeypatched this to fix issue #233
    val counter_construction = ctrs.map{ ctr => ctr match {
      case Op(CounterNew(start, end, step, par)) => 
        val st = start match {
          case Final(s) => src"Some($s)"
          case _ => "None"
        }
        val en = end match {
          case Final(s) => src"Some($s)"
          case _ => "None"
        }
        val ste = step match {
          case Final(s) => src"Some($s)"
          case _ => "None"
        }
        (st, en, ste, "Some(0)")
      case Op(ForeverNew()) => 
        isForever = true
        ("Some(0)", "Some(999)", "Some(1)", "Some(0)") 
    }}
    if (cchainPassMap.contains(lhs)) {controllerStack.pop()}
    // disableSplit = true
    emitGlobalWireMap(src"""${lhs}${suffix}_done""", """Wire(Bool())""")
    emitGlobalWireMap(src"""${lhs}${suffix}_en""", """Wire(Bool())""") // Dangerous but whatever
    emitGlobalWireMap(src"""${lhs}${suffix}_resetter""", """Wire(Bool())""")
    emitGlobalModule(src"""val ${lhs}${suffix}_strides = List(${counter_data.map(_._3)}) // TODO: Safe to get rid of this and connect directly?""")
    emitGlobalModule(src"""val ${lhs}${suffix}_stops = List(${counter_data.map(_._2)}) // TODO: Safe to get rid of this and connect directly?""")
    emitGlobalModule(src"""val ${lhs}${suffix}_starts = List(${counter_data.map{_._1}}) """)
    emitGlobalModule(src"""val ${lhs}${suffix} = Module(new templates.Counter(List(${counter_data.map(_._4)}), 
  List(${counter_construction.map(_._1)}), List(${counter_construction.map(_._2)}), List(${counter_construction.map(_._3)}), List(${counter_construction.map(_._4)}), List(${counter_data.map(_._5)}))) // Par of 0 creates forever counter""")

    emit(src"""${lhs}${suffix}.io.input.stops.zip(${lhs}${suffix}_stops).foreach { case (port,stop) => port := stop.r.asSInt }""")
    emit(src"""${lhs}${suffix}.io.input.strides.zip(${lhs}${suffix}_strides).foreach { case (port,stride) => port := stride.r.asSInt }""")
    emit(src"""${lhs}${suffix}.io.input.starts.zip(${lhs}${suffix}_starts).foreach { case (port,start) => port := start.r.asSInt }""")
    emit(src"""${lhs}${suffix}.io.input.gaps.foreach { gap => gap := 0.S }""")
    emit(src"""${lhs}${suffix}.io.input.saturate := false.B""")
    emit(src"""${lhs}${suffix}.io.input.enable := ${swap(src"${lhs}${suffix}", En)}""")
    emit(src"""${swap(src"${lhs}${suffix}", Done)} := ${lhs}${suffix}.io.output.done""")
    emit(src"""${lhs}${suffix}.io.input.reset := ${swap(src"${lhs}${suffix}", Resetter)}""")
    if (suffix != "") {
      emit(src"""${lhs}${suffix}.io.input.isStream := true.B""")
    } else {
      emit(src"""${lhs}${suffix}.io.input.isStream := false.B""")      
    }
    emit(src"""val ${lhs}${suffix}_maxed = ${lhs}${suffix}.io.output.saturated""")
    ctrs.zipWithIndex.foreach { case (c, i) =>
      val x = c match {
        case Op(CounterNew(_,_,_,Literal(p))) => p
        case Op(ForeverNew()) => 1
      }
      if (suffix == "") {emitGlobalWireMap(s"""${quote(c)}""", src"""Wire(Vec($x, SInt(${counter_data(i)._5}.W)))""")}
      else {emitGlobalWire(s"""val ${quote(c)}${suffix} = (0 until $x).map{ j => Wire(SInt(${counter_data(i)._5}.W)) }""")}
      emit(s"""(0 until $x).map{ j => ${quote(c)}${suffix}(j) := ${quote(lhs)}${suffix}.io.output.counts($i + j) }""")
    }

    // disableSplit = false
  }

  protected def emitInhibitor(lhs: Sym[_], fsm: Option[Sym[_]] = None, switch: Option[Sym[_]]): Unit = {
    if (cfg.enableRetiming) {
      emitGlobalWireMap(src"${lhs}_inhibitor", "Wire(Bool())") // Used to be global module?
      if (fsm.isDefined) {
          emitGlobalModuleMap(src"${lhs}_inhibit", "Module(new SRFF())")
          emit(src"${swap(lhs, Inhibit)}.io.input.set := Utils.risingEdge(~${fsm.get})")  
          emit(src"${swap(lhs, Inhibit)}.io.input.reset := ${DL(swap(lhs, Done), src"1 + ${swap(lhs, Retime)}", true)}")
          /* or'ed  back in because of BasicCondFSM!! */
          emit(src"${swap(lhs, Inhibitor)} := ${swap(lhs, Inhibit)}.io.output.data /*| ${fsm.get}*/ // Really want inhibit to turn on at last enabled cycle")        
          emit(src"${swap(lhs, Inhibit)}.io.input.asyn_reset := reset")
      } else if (switch.isDefined) {
        emit(src"${swap(lhs, Inhibitor)} := ${swap(switch.get, Inhibitor)}")
      } else {
        if (!lhs.cchains.isEmpty) {
          emitGlobalModuleMap(src"${lhs}_inhibit", "Module(new SRFF())")
          emit(src"${swap(lhs, Inhibit)}.io.input.set := ${lhs.cchains.head}.io.output.done")  
          emit(src"${swap(lhs, Inhibitor)} := ${swap(lhs, Inhibit)}.io.output.data /*| ${lhs.cchains.head}.io.output.done*/ // Correction not needed because _done should mask dp anyway")
          emit(src"${swap(lhs, Inhibit)}.io.input.reset := ${swap(lhs, Done)}")
          emit(src"${swap(lhs, Inhibit)}.io.input.asyn_reset := reset")
        } else {
          emitGlobalModuleMap(src"${lhs}_inhibit", "Module(new SRFF())")
          emit(src"${swap(lhs, Inhibit)}.io.input.set := Utils.risingEdge(${swap(lhs, Done)} /*${lhs}_sm.io.output.ctr_inc*/)")
          val rster = if (levelOf(lhs) == InnerControl & !getReadStreams(lhs.toCtrl).isEmpty) {src"${DL(src"Utils.risingEdge(${swap(lhs, Done)})", src"1 + ${swap(lhs, Retime)}", true)} // Ugly hack, do not try at home"} else src"${DL(swap(lhs, Done), 1, true)}"
          emit(src"${swap(lhs, Inhibit)}.io.input.reset := $rster")
          emit(src"${swap(lhs, Inhibitor)} := ${swap(lhs, Inhibit)}.io.output.data")
          emit(src"${swap(lhs, Inhibit)}.io.input.asyn_reset := reset")
        }        
      }
    } else {
      emitGlobalWireMap(src"${lhs}_inhibitor", "Wire(Bool())");emit(src"${swap(lhs, Inhibitor)} := false.B // Maybe connect to ${swap(lhs, Done)}?  ")
    }
  }

  def remappedEns(node: Sym[_], ens: List[Sym[_]]): String = {
    var previousLevel: Sym[_] = node
    var nextLevel: Option[Sym[_]] = Some(node.parent.s.get)
    var result = ens.map(quote)
    while (nextLevel.isDefined) {
      if (styleOf(nextLevel.get) == Sched.Stream) {
        nextLevel.get match {
          case Op(UnrolledForeach(_,_,_,_,e)) => 
            ens.foreach{ my_en_exact =>
              val my_en = my_en_exact match { case Op(DelayLine(_,node)) => node; case _ => my_en_exact}
              e.foreach{ their_en =>
                if (src"${my_en}" == src"${their_en}" & !src"${my_en}".contains("true")) {
                  // Hacky way to avoid double-suffixing
                  if (!src"$my_en".contains(src"_copy${previousLevel}") && !src"$my_en".contains("(") /* hack for remapping */) {  
                    result = result.filter{a => !src"$a".contains(src"$my_en")} :+ swap(src"${my_en}_copy${previousLevel}", Blank)
                  }
                }
              }
            }
          case _ => // do nothing
        }
        nextLevel = None
      } else {
        previousLevel = nextLevel.get
        if (nextLevel.get.parent.s.isDefined) nextLevel = Some(nextLevel.get.parent.s.get)
        else nextLevel = None
      }
    }
    result.mkString("&")

  }

  def getStreamEnablers(c: Sym[_]): String = {
      // If we are inside a stream pipe, the following may be set
      // Add 1 to latency of fifo checks because SM takes one cycle to get into the done state

      // TODO: Assumes only one stream access to the fifo (i.e. readersOf(pt).head)
      val lat = 0 //bodyLatency.sum(c) // FIXME
      val readiers = getReadStreams(c.toCtrl).map{ pt => pt match {
        case fifo @ Op(FIFONew(size)) => // In case of unaligned load, a full fifo should not necessarily halt the stream
          readersOf(pt).head match {
            case Op(FIFODeq(_,ens)) => src"(${DL(src"~$fifo.io.empty", lat+1, true)} | ~${remappedEns(readersOf(pt).head,ens.toList)})"
          }
        // case fifo @ Op(FILONew(size)) => src"${DL(src"~$fifo.io.empty", lat + 1, true)}"
        case fifo @ Op(StreamInNew(bus)) => src"${swap(fifo, Valid)}"
        case fifo => src"${fifo}_en" // parent node
      }}.filter(_ != "").mkString(" & ")
      val holders = getWriteStreams(c.toCtrl).map { pt => pt match {
        case fifo @ Op(FIFONew(size)) => // In case of unaligned load, a full fifo should not necessarily halt the stream
          writersOf(pt).head match {
            case Op(FIFOEnq(_,_,ens)) => src"(${DL(src"~$fifo.io.full", /*lat + 1*/1, true)} | ~${remappedEns(writersOf(pt).head,ens.toList)})"
          }
        case fifo @ Op(StreamOutNew(bus)) => src"${swap(fifo,Ready)}"
        // case fifo @ Op(BufferedOutNew(_, bus)) => src"" //src"~${fifo}_waitrequest"        
      }}.filter(_ != "").mkString(" & ")

      val hasHolders = if (holders != "") "&" else ""
      val hasReadiers = if (readiers != "") "&" else ""

      src"${hasHolders} ${holders} ${hasReadiers} ${readiers}"

  }

  def getNowValidLogic(c: Sym[_]): String = { // Because of retiming, the _ready for streamins and _valid for streamins needs to get factored into datapath_en
      // If we are inside a stream pipe, the following may be set
      val readiers = getReadStreams(c.toCtrl).map {
        case fifo @ Op(StreamInNew(bus)) => src"${swap(fifo, NowValid)}" //& ${fifo}_ready"
        case _ => ""
      }.mkString(" & ")
      val hasReadiers = if (readiers != "") "&" else ""
      if (cfg.enableRetiming) src"${hasReadiers} ${readiers}" else " "
  }

  def getReadyLogic(c: Sym[_]): String = { // Because of retiming, the _ready for streamins and _valid for streamins needs to get factored into datapath_en
      // If we are inside a stream pipe, the following may be set
      val readiers = getWriteStreams(c.toCtrl).map {
        case fifo @ Op(StreamInNew(bus)) => src"${swap(fifo, Ready)}"
        case _ => ""
      }.mkString(" & ")
      val hasReadiers = if (readiers != "") "&" else ""
      if (cfg.enableRetiming) src"${hasReadiers} ${readiers}" else " "
  }

  def getStreamInfoReady(sym: Ctrl): List[String] = {
    getWriteStreams(sym).map{ pt => pt match {
        case fifo @ Op(StreamOutNew(bus)) => src"${swap(fifo, Ready)}"
        case fifo @ Op(FIFONew(_)) if s"${fifo.tp}".contains("IssuedCmd") => src"~${fifo}.io.full"
        case _ => ""
    }}.toList.filter(_ != "")
  }


  def DL[T](name: String, latency: T, isBit: Boolean = false): String = {
    val streamOuts = if (!controllerStack.isEmpty) {getStreamInfoReady(controllerStack.head.toCtrl).mkString(" && ")} else { "" }
    latency match {
      case lat: Int => 
        if (!controllerStack.isEmpty) {
          if (isStreamChild(controllerStack.head) & streamOuts != "") {
            if (isBit) src"(${name}).DS(${latency}.toInt, rr, ${streamOuts})"
            else src"Utils.getRetimed($name, $latency, ${streamOuts})"
          } else {
            if (isBit) src"(${name}).D(${latency}.toInt, rr)"
            else src"Utils.getRetimed($name, $latency)"          
          }
        } else {
          if (isBit) src"(${name}).D(${latency}.toInt, rr)"
          else src"Utils.getRetimed($name, $latency)"                    
        }
      case lat: Double => 
        if (!controllerStack.isEmpty) {
          if (isStreamChild(controllerStack.head) & streamOuts != "") {
            if (isBit) src"(${name}).DS(${latency}.toInt, rr, ${streamOuts})"
            else src"Utils.getRetimed($name, $latency, ${streamOuts})"
          } else {
            if (isBit) src"(${name}).D(${latency}.toInt, rr)"
            else src"Utils.getRetimed($name, $latency)"
          }
        } else {
          if (isBit) src"(${name}).D(${latency}.toInt, rr)"
          else src"Utils.getRetimed($name, $latency)"
        }
      case lat: String => 
        if (!controllerStack.isEmpty) {
          if (isStreamChild(controllerStack.head) & streamOuts != "") {
            if (isBit) src"(${name}).DS(${latency}.toInt, rr, ${streamOuts})"
            else src"Utils.getRetimed($name, $latency, ${streamOuts})"
          } else {
            if (isBit) src"(${name}).D(${latency}.toInt, rr)"
            else src"Utils.getRetimed($name, $latency)"
          }
        } else {
          if (isBit) src"(${name}).D(${latency}.toInt, rr)"
          else src"Utils.getRetimed($name, $latency)"
        }
    }
  }

  // Stream helpers
  // def getStreamEnablers(c: Sym[Any]): String = {
  //     // If we are inside a stream pipe, the following may be set
  //     // Add 1 to latency of fifo checks because SM takes one cycle to get into the done state
  //     val lat = bodyLatency.sum(c)
  //     val readiers = listensTo(c).distinct.map{ pt => pt.memory match {
  //       case fifo @ Def(FIFONew(size)) => // In case of unaligned load, a full fifo should not necessarily halt the stream
  //         pt.access match {
  //           case Def(FIFODeq(_,en)) => src"(${DL(src"~$fifo.io.empty", lat+1, true)} | ~${remappedEns(pt.access,List(en))})"
  //           case Def(ParFIFODeq(_,ens)) => src"""(${DL(src"~$fifo.io.empty", lat+1, true)} | ~(${remappedEns(pt.access, ens.toList)}))"""
  //         }
  //       case fifo @ Def(FILONew(size)) => src"${DL(src"~$fifo.io.empty", lat + 1, true)}"
  //       case fifo @ Def(StreamInNew(bus)) => bus match {
  //         case SliderSwitch => ""
  //         case _ => src"${swap(fifo, Valid)}"
  //       }
  //       case fifo => src"${fifo}_en" // parent node
  //     }}.filter(_ != "").mkString(" & ")
  //     val holders = pushesTo(c).distinct.map { pt => pt.memory match {
  //       case fifo @ Def(FIFONew(size)) => // In case of unaligned load, a full fifo should not necessarily halt the stream
  //         pt.access match {
  //           case Def(FIFOEnq(_,_,en)) => src"(${DL(src"~$fifo.io.full", /*lat + 1*/1, true)} | ~${remappedEns(pt.access,List(en))})"
  //           case Def(ParFIFOEnq(_,_,ens)) => src"""(${DL(src"~$fifo.io.full", /*lat + 1*/1, true)} | ~(${remappedEns(pt.access, ens.toList)}))"""
  //         }
  //       case fifo @ Def(FILONew(size)) => // In case of unaligned load, a full fifo should not necessarily halt the stream
  //         pt.access match {
  //           case Def(FILOPush(_,_,en)) => src"(${DL(src"~$fifo.io.full", /*lat + 1*/1, true)} | ~${remappedEns(pt.access,List(en))})"
  //           case Def(ParFILOPush(_,_,ens)) => src"""(${DL(src"~$fifo.io.full", /*lat + 1*/1, true)} | ~(${remappedEns(pt.access, ens.toList)}))"""
  //         }
  //       case fifo @ Def(StreamOutNew(bus)) => src"${swap(fifo,Ready)}"
  //       case fifo @ Def(BufferedOutNew(_, bus)) => src"" //src"~${fifo}_waitrequest"        
  //     }}.filter(_ != "").mkString(" & ")

  //     val hasHolders = if (holders != "") "&" else ""
  //     val hasReadiers = if (readiers != "") "&" else ""

  //     src"${hasHolders} ${holders} ${hasReadiers} ${readiers}"

  // }

  protected def bitWidth(tp: Type[_]) = tp match {case FixPtType(s,d,f) => d+f; case FltPtType(g,e) => g+e; case _ => 32}

  final protected def wireMap(x: String): String = { 
    if (cfg.compressWires == 1 | cfg.compressWires == 2) {
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
    } else if (rhs.contains(" templates.FF(")) {
      val extractor = ".*FF\\([ ]*([0-9]+)[ ]*,[ ]*([0-9]+)[ ]*\\).*".r
      val extractor(d,w) = rhs
      s"${vec}ff${d}_${w}"  
    } else if (rhs.contains(" multidimR(")) {
      val extractor = ".*multidimR\\([ ]*([0-9]+)[ ]*,[ ]*List\\(([0-9,]+)\\)[ ]*,[ ]*([0-9]+)[ ]*\\).*".r
      val extractor(n,dims,w) = rhs
      val d = dims.replace(" ", "").replace(",","_")
      s"${vec}mdr${n}_${d}_${w}"  
    } else if (rhs.contains(" multidimW(")) {
      val extractor = ".*multidimW\\([ ]*([0-9]+)[ ]*,[ ]*List\\(([0-9,]+)\\)[ ]*,[ ]*([0-9]+)[ ]*\\).*".r
      val extractor(n,dims,w) = rhs
      val d = dims.replace(" ", "").replace(",","_")
      s"${vec}mdw${n}_${d}_${w}"  
    } else if (rhs.contains(" multidimRegW(")) {
      val extractor = ".*multidimRegW\\([ ]*([0-9]+)[ ]*,[ ]*List\\(([0-9, ]+)\\)[ ]*,[ ]*([0-9]+)[ ]*\\).*".r
      val extractor(n,dims,w) = rhs
      val d = dims.replace(" ", "").replace(",","_")
      s"${vec}mdrw${n}_${d}_${w}"  
    } else if (rhs.contains(" Seqpipe(")) {
      val extractor = ".*Seqpipe\\([ ]*([0-9]+)[ ]*,[ ]*isFSM[ ]*=[ ]*([falsetrue]+)[ ]*,[ ]*ctrDepth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*stateWidth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*staticNiter[ ]*=[ ]*([falsetrue]+),[ ]*isReduce[ ]*=[ ]*([falsetrue]+)\\).*".r
      val extractor(stages,fsm,ctrd,stw,static,isRed) = rhs
      val f = fsm.replace("false", "f").replace("true", "t")
      val s = static.replace("false", "f").replace("true", "t")
      val ir = isRed.replace("false", "f").replace("true", "t")
      s"${vec}seq${stages}_${f}_${ctrd}_${stw}_${s}_${ir}"
    } else if (rhs.contains(" Metapipe(")) {
      val extractor = ".*Metapipe\\([ ]*([0-9]+)[ ]*,[ ]*isFSM[ ]*=[ ]*([falsetrue]+)[ ]*,[ ]*ctrDepth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*stateWidth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*staticNiter[ ]*=[ ]*([falsetrue]+),[ ]*isReduce[ ]*=[ ]*([falsetrue]+)\\).*".r
      val extractor(stages,fsm,ctrd,stw,static,isRed) = rhs
      val f = fsm.replace("false", "f").replace("true", "t")
      val s = static.replace("false", "f").replace("true", "t")
      val ir = isRed.replace("false", "f").replace("true", "t")
      s"${vec}meta${stages}_${f}_${ctrd}_${stw}_${s}_${ir}"
    } else if (rhs.contains(" Innerpipe(")) {
      val extractor = ".*Innerpipe\\([ ]*([falsetrue]+)[ ]*,[ ]*ctrDepth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*stateWidth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*staticNiter[ ]*=[ ]*([falsetrue]+),[ ]*isReduce[ ]*=[ ]*([falsetrue]+)\\).*".r
      val extractor(strm,ctrd,stw,static,isRed) = rhs
      val st = strm.replace("false", "f").replace("true", "t")
      val s = static.replace("false", "f").replace("true", "t")
      val ir = isRed.replace("false", "f").replace("true", "t")
      s"${vec}inner${st}_${ctrd}_${stw}_${s}_${ir}"
    } else if (rhs.contains(" Streaminner(")) {
      val extractor = ".*Streaminner\\([ ]*([falsetrue]+)[ ]*,[ ]*ctrDepth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*stateWidth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*staticNiter[ ]*=[ ]*([falsetrue]+),[ ]*isReduce[ ]*=[ ]*([falsetrue]+)\\).*".r
      val extractor(strm,ctrd,stw,static,isRed) = rhs
      val st = strm.replace("false", "f").replace("true", "t")
      val s = static.replace("false", "f").replace("true", "t")
      val ir = isRed.replace("false", "f").replace("true", "t")
      s"${vec}strinner${st}_${ctrd}_${stw}_${s}_${ir}"
    } else if (rhs.contains(" Parallel(")) {
      val extractor = ".*Parallel\\([ ]*([0-9]+)[ ]*,[ ]*isFSM[ ]*=[ ]*([falsetrue]+)[ ]*,[ ]*ctrDepth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*stateWidth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*staticNiter[ ]*=[ ]*([falsetrue]+),[ ]*isReduce[ ]*=[ ]*([falsetrue]+)\\).*".r
      val extractor(stages,fsm,ctrd,stw,static,isRed) = rhs
      val f = fsm.replace("false", "f").replace("true", "t")
      val s = static.replace("false", "f").replace("true", "t")
      val ir = isRed.replace("false", "f").replace("true", "t")
      s"${vec}parallel${stages}_${f}_${ctrd}_${stw}_${s}_${ir}"
    } else if (rhs.contains(" Streampipe(")) {
      val extractor = ".*Streampipe\\([ ]*([0-9]+)[ ]*,[ ]*isFSM[ ]*=[ ]*([falsetrue]+)[ ]*,[ ]*ctrDepth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*stateWidth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*staticNiter[ ]*=[ ]*([falsetrue]+),[ ]*isReduce[ ]*=[ ]*([falsetrue]+)\\).*".r
      val extractor(stages,fsm,ctrd,stw,static,isRed) = rhs
      val f = fsm.replace("false", "f").replace("true", "t")
      val s = static.replace("false", "f").replace("true", "t")
      val ir = isRed.replace("false", "f").replace("true", "t")
      s"${vec}strmpp${stages}_${f}_${ctrd}_${stw}_${s}_${ir}"
    } else if (rhs.contains("_retime")) {
      "rt"
    } else {
      throw new Exception(s"Cannot compress ${rhs}!")
    }
  }

  def swap(tup: (Sym[_], RemapSignal)): String = { swap(tup._1, tup._2) }

  def swap(lhs: Sym[_], s: RemapSignal): String = { swap(src"$lhs", s) }
  def swap(lhs: Ctrl, s: RemapSignal): String = { swap(src"${lhs.s.get}", s) }

  def swap(lhs: => String, s: RemapSignal): String = {
    s match {
      case En => wireMap(src"${lhs}_en")
      case Done => wireMap(src"${lhs}_done")
      case BaseEn => wireMap(src"${lhs}_base_en")
      case Mask => wireMap(src"${lhs}_mask")
      case Resetter => wireMap(src"${lhs}_resetter")
      case DatapathEn => wireMap(src"${lhs}_datapath_en")
      case CtrTrivial => wireMap(src"${lhs}_ctr_trivial")
      case IIDone => wireMap(src"${lhs}_II_done")
      case RstEn => wireMap(src"${lhs}_rst_en")
      case CtrEn => wireMap(src"${lhs}_ctr_en")
      case Ready => wireMap(src"${lhs}_ready")
      case Valid => wireMap(src"${lhs}_valid")
      case NowValid => wireMap(src"${lhs}_now_valid")
      case Inhibitor => wireMap(src"${lhs}_inhibitor")
      case Wren => wireMap(src"${lhs}_wren")
      case Chain => wireMap(src"${lhs}_chain")
      case Blank => wireMap(src"${lhs}")
      case DataOptions => wireMap(src"${lhs}_data_options")
      case ValidOptions => wireMap(src"${lhs}_valid_options")
      case ReadyOptions => wireMap(src"${lhs}_ready_options")
      case EnOptions => wireMap(src"${lhs}_en_options")
      case RVec => wireMap(src"${lhs}_rVec")
      case WVec => wireMap(src"${lhs}_wVec")
      case Retime => wireMap(src"${lhs}_retime")
      case SM => wireMap(src"${lhs}_sm")
      case Inhibit => wireMap(src"${lhs}_inhibit")
    }
  }

}

sealed trait RemapSignal
// "Standard" Signals
object En extends RemapSignal
object Done extends RemapSignal
object BaseEn extends RemapSignal
object Mask extends RemapSignal
object Resetter extends RemapSignal
object DatapathEn extends RemapSignal
object CtrTrivial extends RemapSignal
// A few non-canonical signals
object IIDone extends RemapSignal
object RstEn extends RemapSignal
object CtrEn extends RemapSignal
object Ready extends RemapSignal
object Valid extends RemapSignal
object NowValid extends RemapSignal
object Inhibitor extends RemapSignal
object Wren extends RemapSignal
object Chain extends RemapSignal
object Blank extends RemapSignal
object DataOptions extends RemapSignal
object ValidOptions extends RemapSignal
object ReadyOptions extends RemapSignal
object EnOptions extends RemapSignal
object RVec extends RemapSignal
object WVec extends RemapSignal
object Retime extends RemapSignal
object SM extends RemapSignal
object Inhibit extends RemapSignal

sealed trait AppProperties
object HasLineBuffer extends AppProperties
object HasNBufSRAM extends AppProperties
object HasNBufRegFile extends AppProperties
object HasGeneralFifo extends AppProperties
object HasTileStore extends AppProperties
object HasTileLoad extends AppProperties
object HasGather extends AppProperties
object HasScatter extends AppProperties
object HasLUT extends AppProperties
object HasBreakpoint extends AppProperties
object HasAlignedLoad extends AppProperties
object HasAlignedStore extends AppProperties
object HasUnalignedLoad extends AppProperties
object HasUnalignedStore extends AppProperties
object HasStaticCtr extends AppProperties
object HasVariableCtrBounds extends AppProperties
object HasVariableCtrStride extends AppProperties
object HasFloats extends AppProperties
object HasVariableCtrSyms extends AppProperties
