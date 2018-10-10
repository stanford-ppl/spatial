package spatial.codegen.chiselgen

import argon._
import argon.node._
import spatial.lang._
import spatial.node._
import spatial.metadata.bounds._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.retiming._
import spatial.metadata.types._
import spatial.util.spatialConfig

trait ChiselGenCommon extends ChiselCodegen { 

  /* Set of controllers that we've already emited control signals for.  Sometimes a RegRead
   * can sit in an outer controller and be used by a controller that is a descendent of the 
   * other controllers in this outer controller 
  **/
  private var initializedControllers = Set.empty[Sym[_]]


  // Statistics counters
  var pipeChainPassMap = new scala.collection.mutable.HashMap[Sym[_], List[Sym[_]]]
  var pipeChainPassMapBug41Hack = new scala.collection.mutable.HashMap[Sym[_], Sym[_]]
  var streamCopyWatchlist = List[Sym[_]]()
  var controllerStack = scala.collection.mutable.Stack[Sym[_]]()
  var validPassMap = new scala.collection.mutable.HashMap[Sym[_], Seq[Sym[_]]] // Map from a valid bound sym to its ctrl node, for computing suffix on a valid before we enter the ctrler
  var accumsWithIIDlay = new scala.collection.mutable.ListBuffer[Sym[_]]
  var widthStats = new scala.collection.mutable.ListBuffer[Int]
  var depthStats = new scala.collection.mutable.ListBuffer[Int]
  var appPropertyStats = Set[AppProperties]()

  // Interface
  var argOuts = scala.collection.mutable.HashMap[Sym[_], Int]()
  var argIOs = scala.collection.mutable.HashMap[Sym[_], Int]()
  var argIns = scala.collection.mutable.HashMap[Sym[_], Int]()
  var argOutLoopbacks = scala.collection.mutable.HashMap[Int, Int]() // info about how to wire argouts back to argins in Fringe
  var accelDrams = scala.collection.mutable.HashMap[Sym[_], Int]()
  var hostDrams = scala.collection.mutable.HashMap[Sym[_], Int]()
  /* List of break or exit nodes */
  var earlyExits: List[Sym[_]] = List()

  def latencyOption(op: String, b: Option[Int]): Double = {
    if (spatialConfig.enableRetiming) {
      if (b.isDefined) {spatialConfig.target.latencyModel.exactModel(op)("b" -> b.get)("LatencyOf")}
      else spatialConfig.target.latencyModel.exactModel(op)()("LatencyOf")
    } else {
      0.0
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
          fields.map{f => // Used to be .reversed but not sure now
            val x = src"(${quoteAsScala(f._2)} << $shift).toDouble"
            shift = shift + bitWidth(f._2.tp)
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

  protected def enableRetimeMatch(en: Sym[_], lhs: Sym[_]): Double = { 
    if (spatialConfig.enableRetiming) lhs.fullDelay else 0.0
  }


  def remappedEns(node: Sym[_], ens: List[Sym[_]]): String = {
    var previousLevel: Sym[_] = node
    var nextLevel: Option[Sym[_]] = Some(node.parent.s.get)
    var result = ens.map(quote)
    while (nextLevel.isDefined) {
      if (nextLevel.get.isStreamControl) {
        nextLevel.get match {
          case Op(op: UnrolledForeach) =>
            ens.foreach{ my_en_exact =>
              val my_en = my_en_exact match { case Op(DelayLine(_,node)) => node; case _ => my_en_exact}
              op.ens.foreach{ their_en =>
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

  // Hack for gather/scatter/unaligned load/store, we want the controller to keep running
  //   if the input FIFO is empty but not trying to dequeue, and if the output FIFO is full but
  //   not trying to enqueue
  def FIFOForwardActive(sym: Ctrl, fifo: Sym[_]): String = {
    or((fifo.readers.filter(_.parent.s.get == sym.s.get)).collect{case Op(x: FIFOBankedDeq[_]) => x.enss.map{y => y.filter(!_.trace.isConst).map{z => emit(src"$z = Wire(Bool())")}}; "(" + or(x.enss.map("(" + and(_) + ")")) + ")"})
  }

  def FIFOBackwardActive(sym: Ctrl, fifo: Sym[_]): String = {
    or((fifo.writers.filter(_.parent.s.get == sym.s.get)).collect{case Op(x: FIFOBankedEnq[_]) => x.enss.map{y => y.filter(!_.trace.isConst).map{z => emit(src"$z = Wire(Bool())")}}; "(" + or(x.enss.map("(" + and(_) + ")")) + ")"})
  }

  def getStreamForwardPressure(c: Sym[_]): String = { 
    if (c.hasStreamAncestor) and(getReadStreams(c.toCtrl).collect {
      case fifo @ Op(StreamInNew(bus)) => src"${fifo}.valid"
    }) else "true.B"
  }

  def getStreamBackPressure(c: Sym[_]): String = { 
    if (c.hasStreamAncestor) and(getWriteStreams(c.toCtrl).collect {
      case fifo @ Op(StreamOutNew(bus)) => src"${fifo}.ready"
    }) else "true.B"
  }

  def getForwardPressure(sym: Ctrl): String = {
    if (sym.hasStreamAncestor) and(getReadStreams(sym).collect{
      case fifo@Op(StreamInNew(bus)) => src"${fifo}.valid"
      case fifo@Op(FIFONew(_)) => src"(~${fifo}.m.io.asInstanceOf[FIFOInterface].empty | ~(${FIFOForwardActive(sym, fifo)}))"
      case fifo@Op(FIFORegNew(_)) => src"~${fifo}.m.io.asInstanceOf[FIFOInterface].empty"
      case merge@Op(MergeBufferNew(_,_)) => src"~${merge}.m.io.empty"
    }) else "true.B"
  }
  def getBackPressure(sym: Ctrl): String = {
    if (sym.hasStreamAncestor) and(getWriteStreams(sym).collect{
      case fifo@Op(StreamOutNew(bus)) => src"${fifo}.ready"
      // case fifo@Op(FIFONew(_)) if s"${fifo.tp}".contains("IssuedCmd") => src"~${fifo}.io.asInstanceOf[FIFOInterface].full"
      case fifo@Op(FIFONew(_)) => src"(~${fifo}.m.io.asInstanceOf[FIFOInterface].full | (~${FIFOBackwardActive(sym, fifo)}))"
      case fifo@Op(FIFORegNew(_)) => src"~${fifo}.m.io.asInstanceOf[FIFOInterface].full"
      case merge@Op(MergeBufferNew(_,_)) =>
        merge.writers.filter{ c => c.parent.s == sym.s }.head match {
          case enq@Op(MergeBufferBankedEnq(_, way, _, _)) =>
            src"~${merge}.m.io.full($way)"
        }
    }) else "true.B"
  }

  def DLTrace(lhs: Sym[_]): Option[String] = lhs match {
    case Op(DelayLine(_, data)) => DLTrace(data)
    case Const(_) => Some(quote(lhs))
    case Param(_) => Some(quote(lhs))
    case _ => None
  }

  def DL[T](name: String, latency: T, isBit: Boolean = false): String = {
    val backpressure = if (controllerStack.nonEmpty) src"${controllerStack.head}.sm.io.flow" else "true.B"
    if (isBit) src"(${name}).DS(${latency}.toInt, top.rr, $backpressure)"
    else src"getRetimed($name, ${latency}.toInt, $backpressure)"
  }

  // DL for when we are visiting children but emiting DL on signals that belong to parent
  def DLo[T](name: String, latency: T, isBit: Boolean = false): String = {
    val backpressure = "true.B"
    if (isBit) src"(${name}).DS(${latency}.toInt, top.rr, $backpressure)"
    else src"getRetimed($name, ${latency}.toInt, $backpressure)"
  }


  protected def bitWidth(tp: Type[_]): Int = tp match {
    case Bits(bT) => bT.nbits
    case _ => -1
  }
  protected def fracBits(tp: Type[_]): Int = tp match {
    case FixPtType(s,d,f) => f
    case _ => 0
  }

  final protected def appendChainPass(s: Sym[_], rawname: String): (String, Boolean) = {
    var result = rawname
    var modified = false
    if (pipeChainPassMap.contains(s)) {
      val siblings = pipeChainPassMap(s)
      var nextLevel: Option[Sym[_]] = Some(controllerStack.head)
      while (nextLevel.isDefined) {
        if (siblings.contains(nextLevel.get)) {
          if (siblings.indexOf(nextLevel.get) > 0) {result = result + s"_chain_read_${siblings.indexOf(nextLevel.get)}"}
          modified = true
          nextLevel = None
        }
        else if (pipeChainPassMapBug41Hack.contains(s) && pipeChainPassMapBug41Hack(s) == nextLevel.get) {
          result = result + s"_chain_read_${siblings.length-1}"
          modified = true
          nextLevel = None
        } else {
          nextLevel = nextLevel.get.parent.s
        }
      }
    }
    (result, modified)
  }

  protected def appendSuffix(ctrl: Sym[_], y: Sym[_]): String = {
    y match {
      case x if (x.isBound && getSlot(ctrl) > 0 && ctrl.parent.s.get.isOuterPipeLoop) => src"${x}_chain_read_${getSlot(ctrl)}"
      case x if (x.isBound && ctrl.parent.s.get.isOuterStreamLoop) => src"${x}_copy$ctrl"
      case x => src"$x" 
    }
  }

  protected def appendSuffix(ctrl: Sym[_], y: => String): String = {
    y match {
      case x if (x.startsWith("b") && getSlot(ctrl) > 0 && ctrl.parent.s.get.isOuterPipeLoop) => src"${x}_chain_read_${getSlot(ctrl)}"
      case x if (x.startsWith("b") && ctrl.parent.s.get.isOuterStreamLoop) => src"${x}_copy$ctrl"
      case x => src"$x" 
    }
  }

  protected def getSlot(lhs: Sym[_]): Int = {
    lhs.parent.s.get.children.map(_.s.get).indexOf(lhs)
  }
  protected def parentAndSlot(lhs: Sym[_]): String = {
    if (lhs.parent.s.isDefined) {
      src"Some(${lhs.parent.s.get}, ${getSlot(lhs)})"
    } else "None"
  }

  final protected def getCtrSuffix(ctrl: Sym[_]): String = {
    if (ctrl.parent != Ctrl.Host) {
      if (ctrl.parent.isStreamControl) {src"_copy${ctrl}"} else {getCtrSuffix(ctrl.parent.s.get)}
    } else {
      throw new Exception(s"Could not find LCA stream schedule for a bound sym that is definitely in a stream controller.  This error should be impossibru!")
    }
  }

  final protected def appendStreamSuffix(s: Sym[_], rawname: String): (String, Boolean) = {
    if (streamCopyWatchlist.contains(s)) (rawname + getCtrSuffix(controllerStack.head), true)
    else                           (rawname,                                      false)
  }

  override protected def quote(s: Sym[_]): String = s.rhs match {
    case Def.Bound(id)  => 
      val base = wireMap(super.quote(s) + alphaconv.getOrElse(super.quote(s), ""))
      // val (pipelineRemap, pmod) = appendChainPass(s, base)
      // val (streamRemap, smod) = appendStreamSuffix(s, base)
      // if (pmod & smod) throw new Exception(s"ERROR: Seemingly impossible bound sym that is both part of a pipeline and a stream pipe!")
      // if (smod) wireMap(streamRemap)
      // else if (pmod) wireMap(pipelineRemap)
      // else base
      base
    case Def.Node(_,_) => // Specifically places suffix on ctrchains
      val base = wireMap(super.quote(s) + alphaconv.getOrElse(super.quote(s), ""))
      val (streamRemap, smod) = appendStreamSuffix(s, base)
      if (smod) wireMap(streamRemap)
      else DLTrace(s).getOrElse(base)
    case _ => super.quote(s)
  }

  def and(ens: Set[Bit]): String = and(ens.map(quote).toSeq)
  def or(ens: Set[Bit]): String = or(ens.map(quote).toSeq)
  def and(ens: => Set[String]): String = and(ens.toSeq)
  def or(ens: => Set[String]): String = or(ens.toSeq)
  def and(ens: Seq[String]): String = if (ens.isEmpty) "true.B" else ens.mkString(" & ")
  def or(ens: Seq[String]): String = if (ens.isEmpty) "false.B" else ens.mkString(" | ")
    
  def swap(tup: (Sym[_], RemapSignal)): String = swap(tup._1, tup._2)
  def swap(lhs: Sym[_], s: RemapSignal): String = swap(src"$lhs", s)
  def swap(lhs: Ctrl, s: RemapSignal): String = swap(src"${lhs.s.get}", s)

  def swap(lhs: => String, s: RemapSignal): String = s match {
    case En           => wireMap(src"${lhs}_en")
    case Done         => wireMap(src"${lhs}_done")
    case DoneCondition => wireMap(src"${lhs}_doneCondition")
    case BaseEn       => wireMap(src"${lhs}_base_en")
    case Mask         => wireMap(src"${lhs}_mask")
    case Resetter     => wireMap(src"${lhs}_resetter")
    case DatapathEn   => wireMap(src"${lhs}_datapath_en")
    case CtrTrivial   => wireMap(src"${lhs}_ctr_trivial")
    case IIDone       => wireMap(src"${lhs}_II_done")
    case RstEn        => wireMap(src"${lhs}_rst_en")
    case CtrEn        => wireMap(src"${lhs}_ctr_en")
    case Ready        => wireMap(src"${lhs}_ready")
    case Valid        => wireMap(src"${lhs}_valid")
    case NowValid     => wireMap(src"${lhs}_now_valid")
    case Inhibitor    => wireMap(src"${lhs}_inhibitor")
    case Wren         => wireMap(src"${lhs}_wren")
    case Chain        => wireMap(src"${lhs}_chain")
    case Blank        => wireMap(src"$lhs")
    case DataOptions  => wireMap(src"${lhs}_data_options")
    case ValidOptions => wireMap(src"${lhs}_valid_options")
    case ReadyOptions => wireMap(src"${lhs}_ready_options")
    case EnOptions    => wireMap(src"${lhs}_en_options")
    case RVec         => wireMap(src"${lhs}_rVec")
    case WVec         => wireMap(src"${lhs}_wVec")
    case Latency      => wireMap(src"${lhs}_latency")
    case II           => wireMap(src"${lhs}_ii")
    case SM           => wireMap(src"${lhs}_sm")
    case Inhibit      => wireMap(src"${lhs}_inhibit")
    case Flow      => wireMap(src"${lhs}_flow")
  }

}




