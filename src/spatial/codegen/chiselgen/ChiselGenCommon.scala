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


  // Mapping for DRAMs, since DRAMs and their related Transfer nodes don't necessarily appear in consistent order
  var loadStreams = scala.collection.mutable.HashMap[Sym[_], (String, Int)]()
  var storeStreams = scala.collection.mutable.HashMap[Sym[_], (String, Int)]()
  var gatherStreams = scala.collection.mutable.HashMap[Sym[_], (String, Int)]()
  var scatterStreams = scala.collection.mutable.HashMap[Sym[_], (String, Int)]()

  // Statistics counters
  var controllerStack = scala.collection.mutable.Stack[Sym[_]]()
  var widthStats = new scala.collection.mutable.ListBuffer[Int]
  var depthStats = new scala.collection.mutable.ListBuffer[Int]
  var appPropertyStats = Set[AppProperties]()

  // Buffer mappings from LCA to list of memories controlled by it
  case class BufMapping(val mem: Sym[_], val lane: Int)
  var bufMapping = scala.collection.mutable.HashMap[Sym[_], List[BufMapping]]()
  var regchainsMapping =  scala.collection.mutable.HashMap[Sym[_], List[BufMapping]]()

  /** Map between cchain and list of controllers it is copied for, due to stream controller logic */
  var cchainCopies = scala.collection.mutable.HashMap[Sym[_], List[Sym[_]]]()

  // Interface
  var argOuts = scala.collection.mutable.HashMap[Sym[_], Int]()
  var argIOs = scala.collection.mutable.HashMap[Sym[_], Int]()
  var argIns = scala.collection.mutable.HashMap[Sym[_], Int]()
  var argOutLoopbacks = scala.collection.mutable.HashMap[Int, Int]() // info about how to wire argouts back to argins in Fringe
  var accelDrams = scala.collection.mutable.HashMap[Sym[_], Int]()
  var hostDrams = scala.collection.mutable.HashMap[Sym[_], Int]()
  /* List of break or exit nodes */
  protected var earlyExits: List[Sym[_]] = List()
  /* List of instrumentation counters and their respective depth in the hierarchy*/
  protected var instrumentCounters: List[(Sym[_], Int)] = List()

  /* Mapping between FIFO/LIFO/FIFOReg accesses and the "activity" lane they occupy" */
  protected var activesMap = scala.collection.mutable.HashMap[Sym[_], Int]()

  protected def instrumentCounterIndex(s: Sym[_]): Int = {
    if (spatialConfig.enableInstrumentation) {
      instrumentCounters.takeWhile(_._1 != s).map{x => 
        2 + {if (hasBackPressure(x._1.toCtrl) || hasForwardPressure(x._1.toCtrl)) 2 else 0}
      }.sum
    } else 0
  }
  protected def instrumentCounterArgs(): Int = {
    if (spatialConfig.enableInstrumentation) {
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

  protected def createWire(name: String, payload: String): String = {
    src"""val $name = Wire($payload).suggestName(""" + "\"\"\"" + src"$name" + "\"\"\"" + ")"
  }

  // Hack for gather/scatter/unaligned load/store, we want the controller to keep running
  //   if the input FIFO is empty but not trying to dequeue, and if the output FIFO is full but
  //   not trying to enqueue
  def FIFOForwardActive(sym: Ctrl, fifo: Sym[_]): String = {
    or((fifo.readers.filter(_.parent.s.get == sym.s.get)).collect{
      case a@Op(x: FIFOBankedDeq[_]) => src"${fifo}.io.asInstanceOf[FIFOInterface].accessActivesOut(${activesMap(a)})"
      case a@Op(x: FIFORegDeq[_]) => src"${fifo}.io.asInstanceOf[FIFOInterface].accessActivesOut(${activesMap(a)})"
    })
  }

  def FIFOBackwardActive(sym: Ctrl, fifo: Sym[_]): String = {
    or((fifo.writers.filter(_.parent.s.get == sym.s.get)).collect{
      case a@Op(x: FIFOBankedEnq[_]) => src"${fifo}.io.asInstanceOf[FIFOInterface].accessActivesOut(${activesMap(a)})"
      case a@Op(x: FIFORegEnq[_]) => src"${fifo}.io.asInstanceOf[FIFOInterface].accessActivesOut(${activesMap(a)})"
    })
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

  def hasForwardPressure(sym: Ctrl): Boolean = sym.hasStreamAncestor && getReadStreams(sym).nonEmpty
  def hasBackPressure(sym: Ctrl): Boolean = sym.hasStreamAncestor && getWriteStreams(sym).nonEmpty
  def getForwardPressure(sym: Ctrl): String = {
    if (sym.hasStreamAncestor) and(getReadStreams(sym).collect{
      case fifo@Op(StreamInNew(bus)) => src"${fifo}.valid"
      case fifo@Op(FIFONew(_)) => src"(~${fifo}.io.asInstanceOf[FIFOInterface].empty | ~(${FIFOForwardActive(sym, fifo)}))"
      case fifo@Op(FIFORegNew(_)) => src"(~${fifo}.io.asInstanceOf[FIFOInterface].empty | ~(${FIFOForwardActive(sym, fifo)}))"
      case merge@Op(MergeBufferNew(_,_)) => src"~${merge}.io.empty"
    }) else "true.B"
  }
  def getBackPressure(sym: Ctrl): String = {
    if (sym.hasStreamAncestor) and(getWriteStreams(sym).collect{
      case fifo@Op(StreamOutNew(bus)) => src"${fifo}.ready"
      // case fifo@Op(FIFONew(_)) if s"${fifo.tp}".contains("IssuedCmd") => src"~${fifo}.io.asInstanceOf[FIFOInterface].full"
      case fifo@Op(FIFONew(_)) => src"(~${fifo}.io.asInstanceOf[FIFOInterface].full | ~(${FIFOBackwardActive(sym, fifo)}))"
      case fifo@Op(FIFORegNew(_)) => src"(~${fifo}.io.asInstanceOf[FIFOInterface].full | ~(${FIFOBackwardActive(sym, fifo)}))"
      case merge@Op(MergeBufferNew(_,_)) =>
        merge.writers.filter{ c => c.parent.s == sym.s }.head match {
          case enq@Op(MergeBufferBankedEnq(_, way, _, _)) =>
            src"~${merge}.io.full($way)"
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
    val backpressure = if (controllerStack.nonEmpty) src"sm.io.backpressure" else "true.B"
    if (isBit) src"(${name}).DS(${latency}.toInt, rr, $backpressure)"
    else src"getRetimed($name, ${latency}.toInt, $backpressure)"
  }

  // DL for when we are visiting children but emiting DL on signals that belong to parent
  def DLo[T](name: String, latency: T, isBit: Boolean = false): String = {
    val backpressure = "true.B"
    if (isBit) src"(${name}).DS(${latency}.toInt, rr, $backpressure)"
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

  protected def appendSuffix(ctrl: Sym[_], y: Sym[_]): String = {
    if (ctrl.parent.s.isDefined) {
      val madeEns = ctrl.parent.s.get.op.map{d => d.binds }.getOrElse(Set.empty).filterNot(_.isCounterChain).map(quote)
      y match {
        case x if (x.isBound && getSlot(ctrl) > 0 && ctrl.parent.s.get.isOuterPipeLoop & madeEns.contains(quote(x))) => src"${x}_chain_read_${getSlot(ctrl)}"
        case x if (x.isBound && ctrl.parent.s.get.isOuterStreamLoop & madeEns.contains(quote(x))) => src"${x}_copy$ctrl"
        // case x if (x.isBranch) => src"${x}.data"
        case x => src"$x" 
      }
    } else src"$y"
  }

  protected def appendSuffix(ctrl: Sym[_], y: => String): String = {
    if (ctrl.parent.s.isDefined) {
      val madeEns = ctrl.parent.s.get.op.map{d => d.binds }.getOrElse(Set.empty).filterNot(_.isCounterChain).map(quote)
      y match {
        case x if (x.startsWith("b") && getSlot(ctrl) > 0 && ctrl.parent.s.get.isOuterPipeLoop & madeEns.contains(x)) => src"${x}_chain_read_${getSlot(ctrl)}"
        case x if (x.startsWith("b") && ctrl.parent.s.get.isOuterStreamLoop & madeEns.contains(x)) => src"${x}_copy$ctrl"
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
    inGen(out, src"m_${lhs}.scala"){
      emitHeader()
      open(src"class $lhs {")
        contents
      close("}")
    }
    emit(src"val ${lhs} = (new $lhs).m")
  }

  protected def createBusObject(lhs: Sym[_])(contents: => Unit): Unit = {
    inGen(out, src"bus_${lhs}.scala"){
      emitHeader()
      open(src"class $lhs {")
        contents
      close("}")
    }
    emit(src"val ${lhs} = new $lhs")
  }

  protected def createCtrObject(lhs: Sym[_], start: Sym[_], stop: Sym[_], step: Sym[_], par: I32, forever: Boolean): Unit = {
    val w = bitWidth(lhs.tp.typeArgs.head)
    val strt = start match {case Final(s) => src"Left(Some($s))"
                 case Expect(s) => src"Left(Some($s))"
                 case _ => src"Right(${appendSuffix(lhs.owner, start)})"
                }
    val stp = stop match {case Final(s) => src"Left(Some($s))"
                 case Expect(s) => src"Left(Some($s))"
                 case _ => src"Right(${appendSuffix(lhs.owner, stop)})"
                }
    val ste = step match {case Final(s) => src"Left(Some($s))"
                 case Expect(s) => src"Left(Some($s))"
                 case _ => src"Right(${appendSuffix(lhs.owner, step)})"
                }
    val p = par match {case Final(s) => s"$s"; case Expect(s) => s"$s"; case _ => s"$par"}
    emit(src"val $lhs = new CtrObject($strt, $stp, $ste, $p, $w, $forever)")
  }
  protected def createStreamCChainObject(lhs: Sym[_], ctrs: Seq[Sym[_]]): Unit = {
    forEachChild(lhs.owner){case (c,i) => 
      cchainCopies += (lhs -> {cchainCopies.getOrElse(lhs, List()) ++ List(c)})
      createCChainObject(lhs, ctrs, src"_copy${c}")
    }
  }

  protected def createCChainObject(lhs: Sym[_], ctrs: Seq[Sym[_]], suffix: String = ""): Unit = {
    var isForever = lhs.isForever
    emit(src"""val ${lhs}${suffix}_components = List[CtrObject](${ctrs.map(quote).mkString(",")})""")
    emit(src"""val $lhs$suffix = (new CChainObject(List[CtrObject](${ctrs.map(quote).mkString(",")}), "$lhs$suffix")).cchain """)
    emit(src"""$lhs$suffix.io.input.isStream := ${lhs.isOuterStreamLoop}.B""")

  }

}




