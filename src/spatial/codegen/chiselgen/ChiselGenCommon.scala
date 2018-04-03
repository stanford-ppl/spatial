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

  protected def isStreamChild(lhs: Sym[_]): Boolean = {
    var nextLevel: Option[Ctrl] = Some(lhs.toCtrl)
    var result = false
    while (nextLevel.isDefined) {
      if (styleOf(nextLevel.get.asInstanceOf[Sym[_]]) == Sched.Stream) {
        result = true
        nextLevel = None
      } else {
        nextLevel = Some(nextLevel.get.parent)
      }
    }
    result
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
