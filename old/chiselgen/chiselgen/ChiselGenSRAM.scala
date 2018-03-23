package spatial.codegen.chiselgen

import scala.math._
import argon.core._
import argon.nodes._
import spatial.targets.DE1._
import spatial.aliases._
import spatial.metadata._
import spatial.nodes._
import spatial.utils._

trait ChiselGenSRAM extends ChiselCodegen {
  private var nbufs: List[(Sym[SRAM[_]], Int)] = Nil

  var itersMap = new scala.collection.mutable.HashMap[Bound[_], List[Exp[_]]]
  var cchainPassMap = new scala.collection.mutable.HashMap[Exp[_], Exp[_]] // Map from a cchain to its ctrl node, for computing suffix on a cchain before we enter the ctrler
  var validPassMap = new scala.collection.mutable.HashMap[(Exp[_], String), Seq[Exp[_]]] // Map from a valid bound sym to its ctrl node, for computing suffix on a valid before we enter the ctrler
  var accumsWithIIDlay = new scala.collection.mutable.ListBuffer[Exp[_]]
  var widthStats = new scala.collection.mutable.ListBuffer[Int]
  var depthStats = new scala.collection.mutable.ListBuffer[Int]
  var appPropertyStats = Set[AppProperties]()

  // Helper for getting the BigDecimals inside of Const exps for things like dims, when we know that we need the numbers quoted and not chisel types
  protected def getConstValues(all: Seq[Exp[_]]): Seq[Any] = all.map{i => getConstValue(i) }
  protected def getConstValue(one: Exp[_]): Any = one match {case Const(c) => c }

  // TODO: Should this be deprecated?
  protected def enableRetimeMatch(en: Exp[_], lhs: Exp[_]): Double = { // With partial retiming, the delay on standard signals needs to match the delay of the enabling input, not necessarily the symDelay(lhs) if en is delayed partially
    val last_def_delay = en match {
      case Def(And(_,_)) => latencyOption("And", None)
      case Def(Or(_,_)) => latencyOption("Or", None)
      case Def(Not(_)) => latencyOption("Not", None)
      case Const(_) => 0.0
      case Def(DelayLine(size,_)) => size.toDouble // Undo subtraction
      case Def(RegRead(_)) => latencyOption("RegRead", None)
      case Def(FixEql(a,_)) => latencyOption("FixEql", Some(bitWidth(a.tp)))
      case b: Bound[_] => 0.0
      case _ => throw new Exception(s"Node enable $en not yet handled in partial retiming")
    }
    // if (spatialConfig.enableRetiming) symDelay(en) + last_def_delay else 0.0
    if (spatialConfig.enableRetiming) symDelay(lhs) else 0.0
  }

  protected def computeSuffix(s: Bound[_]): String = {
    var result = if (config.enableNaming) super.quote(s) else wireMap(super.quote(s)) // TODO: Playing with fire here.  Probably just make the quote and name of bound in Codegen.scala do the wireMap themselves instead of doing it here!
    if (itersMap.contains(s)) {
      val siblings = itersMap(s)
      var nextLevel: Option[Exp[_]] = Some(controllerStack.head)
      while (nextLevel.isDefined) {
        if (siblings.contains(nextLevel.get)) {
          if (siblings.indexOf(nextLevel.get) > 0) {result = result + s"_chain_read_${siblings.indexOf(nextLevel.get)}"}
          nextLevel = None
        } else {
          nextLevel = parentOf(nextLevel.get)
        }
      }
    } 
    result
  }

  def latencyOption(op: String, b: Option[Int]): Double = {
    if (spatialConfig.enableRetiming) {
      if (b.isDefined) {spatialConfig.target.latencyModel.model(op)("b" -> b.get)("LatencyOf")}
      else spatialConfig.target.latencyModel.model(op)()("LatencyOf") 
    } else {
      0.0
    }
  }
  def latencyOptionString(op: String, b: Option[Int]): String = {
    if (spatialConfig.enableRetiming) {
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

  protected def isStreamChild(lhs: Exp[_]): Boolean = {
    var nextLevel: Option[Exp[_]] = Some(lhs)
    var result = false
    while (nextLevel.isDefined) {
      if (styleOf(nextLevel.get) == StreamPipe) {
        result = true
        nextLevel = None
      } else {
        nextLevel = parentOf(nextLevel.get)
      }
    }
    result
  }

  def getStreamInfoReady(sym: Exp[_]): List[String] = {
    pushesTo(sym).distinct.map{ pt => pt.memory match {
        case fifo @ Def(StreamOutNew(bus)) => src"${swap(fifo, Ready)}"
        case fifo @ Def(FIFONew(_)) if s"${fifo.tp}".contains("IssuedCmd") => src"~${fifo}.io.full"
        case _ => ""
    }}.filter(_ != "")
  }
  // Method for deciding if we should use the always-enabled delay line or the stream delay line (DS)
  def DL[T](name: String, latency: T, isBit: Boolean = false): String = {
    val streamOuts = if (!controllerStack.isEmpty) {getStreamInfoReady(controllerStack.head).mkString(" && ")} else { "" }
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

  // Method for deciding if we should use the always-enabled delay line or the stream delay line (DS), specifically for signals like inhibitor resets that must acknowledeg a done signal that can strobe while stalled
  def DLI[T](name: String, latency: T, isBit: Boolean = false): String = {
    val streamOuts = if (!controllerStack.isEmpty) {getStreamInfoReady(controllerStack.head).mkString(" && ")} else { "" }

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

  override protected def remap(tp: Type[_]): String = tp match {
    case tp: SRAMType[_] => src"Array[${tp.child}]"
    case _ => super.remap(tp)
  }

  //def cchainWidth(ctr: Exp[Counter]): Int = {
    //ctr match {
      //case Def(CounterNew(Exact(s), Exact(e), _, _)) => 
        //val sbits = if (s > 0) {BigInt(1) max ceil(scala.math.log((BigInt(1) max s).toDouble)/scala.math.log(2)).toInt} 
                    //else {BigInt(1) max ceil(scala.math.log((BigInt(1) max (s.abs+BigInt(1))).toDouble)/scala.math.log(2)).toInt}
        //val ebits = if (e > 0) {BigInt(1) max ceil(scala.math.log((BigInt(1) max e).toDouble)/scala.math.log(2)).toInt} 
                    //else {BigInt(1) max ceil(scala.math.log((BigInt(1) max (e.abs+BigInt(1))).toDouble)/scala.math.log(2)).toInt}
        //({ebits max sbits} + 2).toInt
      //case Def(CounterNew(start, stop, _, _)) => 
        //val sbits = bitWidth(start.tp)
        //val ebits = bitWidth(stop.tp)
        //({ebits max sbits} + 0).toInt
      //case _ => 32
    //}
  //}

  def isSpecializedReduce(accum: Exp[_]): Boolean = {
    reduceType(accum) match {
      case Some(fps: ReduceFunction) => // is an accumulator
        fps match {
          case FixPtSum => true
          case _ => false
        }
      case _ => false
    }
  }
  def cchainWidth(ctr: Exp[Counter]): Int = ctr match {
    case Def(CounterNew(Exact(s), Exact(e), _, _)) =>
      val sbits = if (s > 0) {BigInt(2) + ceil(scala.math.log((BigInt(1) max s).toDouble)/scala.math.log(2)).toInt}
                  else {BigInt(2) + ceil(scala.math.log((BigInt(1) max (s.abs+BigInt(1))).toDouble)/scala.math.log(2)).toInt}
      val ebits = if (e > 0) {BigInt(2) + ceil(scala.math.log((BigInt(1) max e).toDouble)/scala.math.log(2)).toInt}
                  else {BigInt(2) + ceil(scala.math.log((BigInt(1) max (e.abs+BigInt(1))).toDouble)/scala.math.log(2)).toInt}
      ({ebits max sbits} + 2).toInt
    case Def(CounterNew(start, stop, _, _)) =>
      val sbits = bitWidth(start.tp)
      val ebits = bitWidth(stop.tp)
      ({ebits max sbits} + 0).toInt
    case _ => 32
  }

  def getWriteAddition(c: Exp[Any]): String = {
    // If we are inside a stream pipe, the following may be set
    // Add 1 to latency of fifo checks because SM takes one cycle to get into the done state
    val lat = bodyLatency.sum(c)
    val readiers = listensTo(c).distinct.map{_.memory}.map {
      case fifo @ Def(StreamInNew(bus)) => src"${swap(fifo, Valid)}"
      case _ => ""
    }.filter(_ != "").mkString(" & ")

    val hasReadiers = if (readiers != "") "&" else ""

    src" ${hasReadiers} ${readiers}"
  }

  def getNowValidLogic(c: Exp[Any]): String = { // Because of retiming, the _ready for streamins and _valid for streamins needs to get factored into datapath_en
      // If we are inside a stream pipe, the following may be set
      val readiers = listensTo(c).distinct.map{_.memory}.map {
        case fifo @ Def(StreamInNew(bus)) => src"${swap(fifo, NowValid)}" //& ${fifo}_ready"
        case _ => ""
      }.mkString(" & ")
      val hasReadiers = if (readiers != "") "&" else ""
      if (spatialConfig.enableRetiming) src"${hasReadiers} ${readiers}" else " "
  }
  def getReadyLogic(c: Exp[Any]): String = { // Because of retiming, the _ready for streamins and _valid for streamins needs to get factored into datapath_en
      // If we are inside a stream pipe, the following may be set
      val readiers = listensTo(c).distinct.map{_.memory}.map {
        case fifo @ Def(StreamInNew(bus)) => src"${swap(fifo, Ready)}"
        case _ => ""
      }.mkString(" & ")
      val hasReadiers = if (readiers != "") "&" else ""
      if (spatialConfig.enableRetiming) src"${hasReadiers} ${readiers}" else " "
  }


  protected def bufferControlInfo(mem: Exp[_], i: Int = 0): List[(Exp[_], String)] = {
    val readers = readersOf(mem)
    val writers = writersOf(mem)
    val readPorts = readers.filter{reader => dispatchOf(reader, mem).contains(i) }.groupBy{a => portsOf(a, mem, i) }.toList
    val writePorts = writers.filter{writer => dispatchOf(writer, mem).contains(i) }.groupBy{a => portsOf(a, mem, i) }.toList
    // Console.println(s"working on $mem $i $readers $readPorts $writers $writePorts")
    // Console.println(s"${readPorts.map{case (_, readers) => readers}}")
    // Console.println(s"innermost ${readPorts.map{case (_, readers) => readers.flatMap{a => topControllerOf(a,mem,i)}.head}.head.node}")
    // Console.println(s"middle ${parentOf(readPorts.map{case (_, readers) => readers.flatMap{a => topControllerOf(a,mem,i)}.head}.head.node).get}")
    // Console.println(s"outermost ${childrenOf(parentOf(readPorts.map{case (_, readers) => readers.flatMap{a => topControllerOf(a,mem,i)}.head}.head.node).get)}")
    var specialLB = false
    val readCtrls = readPorts.map{case (port, readers) =>
      val readTops = readers.flatMap{a => topControllerOf(a, mem, i) }
      mem match {
        case Def(_:LineBufferNew[_]) => // Allow empty lca, meaning we use a sequential pipe for rotations
          if (readTops.nonEmpty) {
            readTops.headOption.get.node
          } else {
            warn(u"Memory $mem, instance $i, port $port had no read top controllers.  Consider wrapping this linebuffer in a metapipe to get better speedup")
            specialLB = true
            // readTops.headOption.getOrElse{throw new Exception(u"Memory $mem, instance $i, port $port had no read top controllers") }    
            readers.head.node
          }
        case _ =>
          readTops.headOption.getOrElse{throw new Exception(u"Memory $mem, instance $i, port $port had no read top controllers") }.node    
      }
      
    }
    if (readCtrls.isEmpty) throw new Exception(u"Memory $mem, instance $i had no readers?")

    // childrenOf(parentOf(readPorts.map{case (_, readers) => readers.flatMap{a => topControllerOf(a,mem,i)}.head}.head.node).get)

    if (!specialLB) {
      val allSiblings = childrenOf(parentOf(readCtrls.head).get)
      val readSiblings = readPorts.map{case (_,r) => r.flatMap{ a => topControllerOf(a, mem, i)}}.filter{_.nonEmpty}.map{all => all.head.node}
      val writeSiblings = writePorts.map{case (_,w) => w.flatMap{ a => topControllerOf(a, mem, i)}}.filter{_.nonEmpty}.map{all => all.head.node}
      val writePortsNumbers = writeSiblings.map{ sw => allSiblings.indexOf(sw) }
      val readPortsNumbers = readSiblings.map{ sr => allSiblings.indexOf(sr) }
      val firstActivePort = math.min( readPortsNumbers.min, writePortsNumbers.min )
      val lastActivePort = math.max( readPortsNumbers.max, writePortsNumbers.max )
      val numStagesInbetween = lastActivePort - firstActivePort

      val info = (0 to numStagesInbetween).map { port =>
        val ctrlId = port + firstActivePort
        val node = allSiblings(ctrlId)
        val rd = if (readPortsNumbers.toList.contains(ctrlId)) {"read"} else {
          // emit(src"""${mem}_${i}.readTieDown(${port})""")
          ""
        }
        val wr = if (writePortsNumbers.toList.contains(ctrlId)) {"write"} else {""}
        val empty = if (rd == "" & wr == "") "empty" else ""
        (node, src"/*$rd $wr $empty*/")
      }
      info.toList
    } else {
      // Assume write comes before read and there is only one write
      val writer = writers.head.ctrl._1
      val reader = readers.head.ctrl._1
      val lca = leastCommonAncestorWithPaths[Exp[_]](reader, writer, {node => parentOf(node)})._1.get
      val allSiblings = childrenOf(lca)
      var writeSibling: Option[Exp[Any]] = None
      var candidate = writer
      while (!writeSibling.isDefined) {
        if (allSiblings.contains(candidate)) {
          writeSibling = Some(candidate)
        } else {
          candidate = parentOf(candidate).get
        }
      }
      // Get LCA of read and write
      List((writeSibling.get, src"/*seq write*/"))
    }

  }

  // Emit an SRFF that will block a counter from incrementing after the counter reaches the max
  //  rather than spinning even when there is retiming and the surrounding loop has a delayed
  //  view of the counter done signal
  protected def emitInhibitor(lhs: Exp[_], cchain: Option[Exp[_]], fsm: Option[Exp[_]] = None, switch: Option[Exp[_]]): Unit = {
    if (spatialConfig.enableRetiming || spatialConfig.enablePIRSim) {
      emitGlobalWireMap(src"${lhs}_inhibitor", "Wire(Bool())") // Used to be global module?
      if (fsm.isDefined) {
          emitGlobalModuleMap(src"${lhs}_inhibit", "Module(new SRFF())")
          emit(src"${swap(lhs, Inhibit)}.io.input.set := Utils.risingEdge(~${fsm.get})")  
          emit(src"${swap(lhs, Inhibit)}.io.input.reset := ${DLI(swap(lhs, Done), src"1 + ${swap(lhs, Retime)}", true)}")
          /* or'ed  back in because of BasicCondFSM!! */
          emit(src"${swap(lhs, Inhibitor)} := ${swap(lhs, Inhibit)}.io.output.data /*| ${fsm.get}*/ // Really want inhibit to turn on at last enabled cycle")        
          emit(src"${swap(lhs, Inhibit)}.io.input.asyn_reset := reset")
      } else if (switch.isDefined) {
        emit(src"${swap(lhs, Inhibitor)} := ${swap(switch.get, Inhibitor)}")
      } else {
        if (cchain.isDefined) {
          emitGlobalModuleMap(src"${lhs}_inhibit", "Module(new SRFF())")
          emit(src"${swap(lhs, Inhibit)}.io.input.set := ${cchain.get}.io.output.done")  
          emit(src"${swap(lhs, Inhibitor)} := ${swap(lhs, Inhibit)}.io.output.data /*| ${cchain.get}.io.output.done*/ // Correction not needed because _done should mask dp anyway")
          emit(src"${swap(lhs, Inhibit)}.io.input.reset := ${swap(lhs, Done)}")
          emit(src"${swap(lhs, Inhibit)}.io.input.asyn_reset := reset")
        } else {
          emitGlobalModuleMap(src"${lhs}_inhibit", "Module(new SRFF())")
          emit(src"${swap(lhs, Inhibit)}.io.input.set := Utils.risingEdge(${swap(lhs, Done)} /*${lhs}_sm.io.output.ctr_inc*/)")
          val rster = if (levelOf(lhs) == InnerControl & listensTo(lhs).distinct.length > 0) {src"${DLI(src"Utils.risingEdge(${swap(lhs, Done)})", src"1 + ${swap(lhs, Retime)}", true)} // Ugly hack, do not try at home"} else src"${DLI(swap(lhs, Done), 1, true)}"
          emit(src"${swap(lhs, Inhibit)}.io.input.reset := $rster")
          emit(src"${swap(lhs, Inhibitor)} := ${swap(lhs, Inhibit)}.io.output.data")
          emit(src"${swap(lhs, Inhibit)}.io.input.asyn_reset := reset")
        }        
      }
    } else {
      emitGlobalWireMap(src"${lhs}_inhibitor", "Wire(Bool())");emit(src"${swap(lhs, Inhibitor)} := false.B // Maybe connect to ${swap(lhs, Done)}?  ")
    }
  }

  def logRetime(lhs: String, data: String, delay: Int, isVec: Boolean = false, vecWidth: Int = 1, wire: String = "", isBool: Boolean = false): Unit = {
    if (delay > maxretime) maxretime = delay
    if (isVec) {
      emitGlobalWireMap(src"$lhs", src"Wire(${wire})")
      emit(src"(0 until ${vecWidth}).foreach{i => ${lhs}(i).r := ${DL(src"${data}(i).r", delay)}}")
    } else {
      if (isBool) {
        emitGlobalWireMap(src"""$lhs""", src"""Wire(Bool())""");emit(src"""${lhs} := ${DL(data, delay, true)}""")
      } else {
        emitGlobalWireMap(src"""$lhs""", src"""Wire(${wire})""");emit(src"""${lhs}.r := ${DL(src"${data}.r", delay)}""")
      }
    }
  }

  def logRetime(lhs: => Sym[_], data: String, delay: Int, isVec: Boolean, vecWidth: Int, wire: String, isBool: Boolean): Unit = {
    if (delay > maxretime) maxretime = delay
    if (isVec) {
      emitGlobalWireMap(src"$lhs", src"Wire(${wire})")
      emit(src"(0 until ${vecWidth}).foreach{i => ${lhs}(i).r := ${DL(src"${data}(i).r", delay)}}")
    } else {
      if (isBool) {
        emitGlobalWireMap(src"""$lhs""", src"""Wire(Bool())""");emit(src"""${lhs} := ${DL(data, delay, true)}""")
      } else {
        emitGlobalWireMap(src"""$lhs""", src"""Wire(${wire})""");emit(src"""${lhs}.r := ${DL(src"${data}.r", delay)}""")
      }
    }
  }

  override protected def spatialNeedsFPType(tp: Type[_]): Boolean = tp match { // FIXME: Why doesn't overriding needsFPType work here?!?!
    case FixPtType(s,d,f) => if (s) true else if (f == 0) false else true
    case IntType()  => false
    case LongType() => false
    case HalfType() => true
    case FloatType() => true
    case DoubleType() => true
    case _ => super.needsFPType(tp)
  }

  override protected def name(s: Dyn[_]): String = s match {
    case Def(SRAMNew(_)) => s"""${s}_${s.name.getOrElse("sram").replace("$","")}"""
    case _ => super.name(s)
  }

  override protected def quote(e: Exp[_]): String = e match {
    // FIXME: Unclear precedence with the quote rule for Bound in ChiselGenCounter
    case b: Bound[_] => 
      swap(computeSuffix(b), Blank)
    case _ => super.quote(e)
  } 

  def flattenAddress(dims: Seq[Exp[Index]], indices: Seq[Exp[Index]], ofs: Option[Exp[Index]]): String = {
    val strides = List.tabulate(dims.length){i => (dims.drop(i+1).map(quote) :+ "1").mkString("*") }
    indices.zip(strides).map{case (i,s) => src"$i*$s" }.mkString(" + ") + ofs.map{o => src" + $o"}.getOrElse("")
  }

  override protected def emitNode(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@SRAMNew(_) =>
      val dimensions = constDimsOf(lhs)
      duplicatesOf(lhs).zipWithIndex.foreach{ case (mem, i) => 
        val rParZip = readersOf(lhs).toList
          .filter{read => dispatchOf(read, lhs) contains i}
          .map { r => 
            val par = r.node match {
              case Def(_: SRAMLoad[_]) => 1
              case Def(a@ParSRAMLoad(_,inds,ens)) => inds.length
            }
            val port = portsOf(r, lhs, i).toList.head
            (par, port)
          }
        val rPar = if (rParZip.isEmpty) "1" else rParZip.map{_._1}.mkString(",")
        val rBundling = if (rParZip.isEmpty) "0" else rParZip.map{_._2}.mkString(",")
        val wParZip = writersOf(lhs).toList
          .filter{write => dispatchOf(write, lhs) contains i}
          .filter{w => portsOf(w, lhs, i).toList.length == 1}
          .map { w => 
            val par = w.node match {
              case Def(_: SRAMStore[_]) => 1
              case Def(a@ParSRAMStore(_,_,_,ens)) => ens match {
                case Op(ListVector(elems)) => elems.length // Was this deprecated?
                case _ => ens.length
              }
            }
            val port = portsOf(w, lhs, i).toList.head
            (par, port)
          }
        val wPar = if (wParZip.isEmpty) "1" else wParZip.map{_._1}.mkString(",")
        val wBundling = if (wParZip.isEmpty) "0" else wParZip.map{_._2}.mkString(",")
        val broadcasts = writersOf(lhs).toList
          .filter{w => portsOf(w, lhs, i).toList.length > 1}.map { w =>
          w.node match {
            case Def(_: SRAMStore[_]) => 1
            case Def(a@ParSRAMStore(_,_,_,ens)) => ens match {
              case Op(ListVector(elems)) => elems.length // Was this deprecated?
              case _ => ens.length
            }
          }
        }
        val bPar = if (broadcasts.nonEmpty) broadcasts.mkString(",") else "0"
        val width = bitWidth(lhs.tp.typeArguments.head)

        mem match {
          case instance @ BankedMemory(dims, depth, isAccum) =>
            val resource_type = instance.resource.name
            val strides = src"""List(${dims.map(_.banks)})"""
            if (depth == 1) {
              emitGlobalModule(src"""val ${lhs}_$i = Module(new SRAM(List($dimensions), $width, 
    List(${dims.map(_.banks)}), $strides,
    List($wPar), List($rPar), BankedMemory, ${spatialConfig.enableSyncMem}, "${resource_type}"
  ))""")
            } else {
              appPropertyStats += HasNBufSRAM
              nbufs = nbufs :+ (lhs.asInstanceOf[Sym[SRAM[_]]], i)
              val memname = if (bPar == "0") "NBufSRAMnoBcast" else "NBufSRAM"
              emitGlobalModule(src"""val ${lhs}_$i = Module(new ${memname}(List($dimensions), $depth, $width,
    List(${dims.map(_.banks)}), $strides,
    List($wPar), List($rPar), 
    List($wBundling), List($rBundling), List($bPar), BankedMemory, ${spatialConfig.enableSyncMem}, "${resource_type}"
  ))""")
            }
          case instance @ DiagonalMemory(strides, banks, depth, isAccum) =>
            val resource_type = instance.resource.name
            if (depth == 1) {
              emitGlobalModule(src"""val ${lhs}_$i = Module(new SRAM(List($dimensions), $width, 
    List(${(0 until dimensions.length).map{_ => s"$banks"}}), List($strides),
    List($wPar), List($rPar), DiagonalMemory, ${spatialConfig.enableSyncMem}, "${resource_type}"
  ))""")
            } else {
              appPropertyStats += HasNBufSRAM
              nbufs = nbufs :+ (lhs.asInstanceOf[Sym[SRAM[_]]], i)
              val memname = if (bPar == "0") "NBufSRAMnoBcast" else "NBufSRAM"
              emitGlobalModule(src"""val ${lhs}_$i = Module(new ${memname}(List($dimensions), $depth, $width,
    List(${(0 until dimensions.length).map{_ => s"$banks"}}), List($strides),
    List($wPar), List($rPar), 
    List($wBundling), List($rBundling), List($bPar), DiagonalMemory, ${spatialConfig.enableSyncMem}, "${resource_type}"
  ))""")
            }
          }
        }
    
    case SRAMLoad(sram, dims, is, ofs, en) =>
      val dispatch = dispatchOf(lhs, sram)
      val rPar = 1 // Because this is SRAMLoad node    
      val width = bitWidth(sram.tp.typeArguments.head)

      // Check if we need to expose a _ready signal to the read port
      val streamOuts = pushesTo(controllerStack.head).distinct.map{ pt => pt.memory match {
          case fifo @ Def(StreamOutNew(bus)) => src"${swap(fifo, Ready)}"
          case _ => ""
        }}.filter(_ != "").mkString(" & ")

      emit(s"""// Assemble multidimR vector""")
      dispatch.foreach{ i =>  // TODO: Shouldn't dispatch only have one element?
        val parent = readersOf(sram).find{_.node == lhs}.get.ctrlNode
        val enable = src"""${swap(parent, DatapathEn)} & ~${swap(parent, Inhibitor)}"""
        emitGlobalWireMap(src"""${lhs}_rVec""", src"""Wire(Vec(${rPar}, new multidimR(${dims.length}, List(${constDimsOf(sram)}), ${width})))""")
        emit(src"""${swap(lhs, RVec)}(0).en := ${DL(enable, src"${enableRetimeMatch(en, lhs)}.toInt", true)} & $en""")
        is.zipWithIndex.foreach{ case(ind,j) => 
          emit(src"""${swap(lhs, RVec)}(0).addr($j) := ${ind}.raw // Assume always an int""")
        }
        val p = portsOf(lhs, sram, i).head
        val basequote = src"${lhs}_base" // get string before we create the map
        if (isStreamChild(controllerStack.head) & streamOuts != "") {
          emit(src"""val ${basequote} = ${sram}_$i.connectRPort(Vec(${swap(lhs, RVec)}.toArray), $p, $streamOuts)""")
        } else {
          emit(src"""val ${basequote} = ${sram}_$i.connectRPort(Vec(${swap(lhs, RVec)}.toArray), $p)""")
        }
        emitGlobalWireMap(src"""${lhs}""", src"""Wire(${newWire(lhs.tp)})""") 
        emit(src"""${lhs}.r := ${sram}_$i.io.output.data(${basequote})""")
      }

    case SRAMStore(sram, dims, is, ofs, v, en) =>
      val width = bitWidth(sram.tp.typeArguments.head)
      val parent = writersOf(sram).find{_.node == lhs}.get.ctrlNode
      val enable = src"""${swap(parent, DatapathEn)} & ~${swap(parent, Inhibitor)}"""
      emit(s"""// Assemble multidimW vector""")
      emitGlobalWireMap(src"""${lhs}_wVec""", src"""Wire(Vec(1, new multidimW(${dims.length}, List(${constDimsOf(sram)}), $width))) """)
      emit(src"""${swap(lhs, WVec)}(0).data := $v.raw""")
      emit(src"""${swap(lhs, WVec)}(0).en := $en & ${DL(src"${enable} & ${swap(parent, IIDone)}", src"${enableRetimeMatch(en, lhs)}.toInt")}""")
      is.zipWithIndex.foreach{ case(ind,j) => 
        emit(src"""${swap(lhs, WVec)}(0).addr($j) := ${ind}.raw // Assume always an int""")
      }
      duplicatesOf(sram).zipWithIndex.foreach{ case (mem, i) =>
        emit(src"""${sram}_$i.connectWPort(${swap(lhs, WVec)}, List(${portsOf(lhs, sram, i)})) """)
      }


    case ParSRAMLoad(sram,inds,ens) =>
      val dispatch = dispatchOf(lhs, sram)
      val width = bitWidth(sram.tp.typeArguments.head)
      val rPar = inds.length
      val dims = stagedDimsOf(sram)

      // Check if we need to expose a _ready signal to the read port
      val streamOuts = pushesTo(controllerStack.head).distinct.map{ pt => pt.memory match {
          case fifo @ Def(StreamOutNew(bus)) => src"${swap(fifo, Ready)}"
          case _ => ""
        }}.filter(_ != "").mkString(" & ")

      disableSplit = true
      emit(s"""// Assemble multidimR vector""")
      emitGlobalWireMap(src"""${lhs}_rVec""", src"""Wire(Vec(${rPar}, new multidimR(${dims.length}, List(${constDimsOf(sram)}), ${width})))""")
      if (dispatch.toList.length == 1) {
        val k = dispatch.toList.head 
        val parent = readersOf(sram).find{_.node == lhs}.get.ctrlNode
        inds.zipWithIndex.foreach{ case (ind, i) =>
          emit(src"${swap(lhs, RVec)}($i).en := ${DL(swap(parent, En), src"${enableRetimeMatch(ens(i), lhs)}.toInt", true)} & ${ens(i)}")
          ind.zipWithIndex.foreach{ case (a, j) =>
            emit(src"""${swap(lhs, RVec)}($i).addr($j) := ${a}.raw """)
          }
        }
        val p = portsOf(lhs, sram, k).head
        if (isStreamChild(controllerStack.head) & streamOuts != "") {
          emit(src"""val ${lhs}_base = ${sram}_$k.connectRPort(Vec(${swap(lhs, RVec)}.toArray), $p, $streamOuts)""")
        } else {
          emit(src"""val ${lhs}_base = ${sram}_$k.connectRPort(Vec(${swap(lhs, RVec)}.toArray), $p)""")
        }
        // sram.tp.typeArguments.head match { 
        //   case FixPtType(s,d,f) => if (spatialNeedsFPType(sram.tp.typeArguments.head)) {
              emit(src"""val ${lhs} = Wire(${newWire(lhs.tp)})""") 
              emit(s"""(0 until ${rPar}).foreach{i => ${quote(lhs)}(i).r := ${quote(sram)}_$k.io.output.data(${quote(lhs)}_base+i) }""")
          //   } else {
          //     emit(src"""val $lhs = (0 until ${rPar}).map{i => ${sram}_$k.io.output.data(${lhs}_base+i) }""")
          //   }
          // case _ => emit(src"""val $lhs = (0 until ${rPar}).map{i => ${sram}_$k.io.output.data(${lhs}_base+i) }""")
        // }
      } else {
        emit(src"""val ${lhs} = Wire(${newWire(lhs.tp)})""")
        dispatch.zipWithIndex.foreach{ case (k,id) => 
          val parent = readersOf(sram).find{_.node == lhs}.get.ctrlNode
          emit(src"${swap(lhs, RVec)}($id).en := ${DL(swap(parent, En), swap(parent, Retime), true)} & ${ens(id)}")
          inds(id).zipWithIndex.foreach{ case (a, j) =>
            emit(src"""${swap(lhs, RVec)}($id).addr($j) := ${a}.raw """)
          }
          val p = portsOf(lhs, sram, k).head
          if (isStreamChild(controllerStack.head) & streamOuts != "") {
            emit(src"""val ${lhs}_base_$k = ${sram}_$k.connectRPort(Vec(${swap(lhs, RVec)}($id)), $p, $streamOuts) // TODO: No need to connect all rVec lanes to SRAM even though only one is needed""")
          } else {
            emit(src"""val ${lhs}_base_$k = ${sram}_$k.connectRPort(Vec(${swap(lhs, RVec)}($id)), $p) // TODO: No need to connect all rVec lanes to SRAM even though only one is needed""")
          }
          // sram.tp.typeArguments.head match { 
          //   case FixPtType(s,d,f) => if (spatialNeedsFPType(sram.tp.typeArguments.head)) {
                emit(s"""${quote(lhs)}($id).r := ${quote(sram)}_$k.io.output.data(${quote(lhs)}_base_$k)""")
            //   } else {
            //     emit(src"""${lhs}($id) := ${sram}_$k.io.output.data(${lhs}_base_$k)""")
            //   }
            // case _ => emit(src"""${lhs}($id) := ${sram}_$k.io.output.data(${lhs}_base_$k)""")
          // }
        }
      }
      disableSplit = false

    case ParSRAMStore(sram,inds,data,ens) =>
      val dims = stagedDimsOf(sram)
      val width = bitWidth(sram.tp.typeArguments.head)
      val parent = writersOf(sram).find{_.node == lhs}.get.ctrlNode
      val enable = src"${swap(parent, DatapathEn)}"
      emit(s"""// Assemble multidimW vector""")
      emitGlobalWireMap(src"""${lhs}_wVec""", src"""Wire(Vec(${inds.indices.length}, new multidimW(${dims.length}, List(${constDimsOf(sram)}), ${width})))""")
      val datacsv = data.map{d => src"${d}.r"}.mkString(",")
      data.zipWithIndex.foreach { case (d, i) =>
        emit(src"""${swap(lhs, WVec)}($i).data := ${d}.r""")
      }
      inds.zipWithIndex.foreach{ case (ind, i) =>
        emit(src"${swap(lhs, WVec)}($i).en := ${ens(i)} & ${DL(src"$enable & ~${swap(parent, Inhibitor)} & ${swap(parent, IIDone)}", src"${enableRetimeMatch(ens(i), lhs)}.toInt")}")
        ind.zipWithIndex.foreach{ case (a, j) =>
          emit(src"""${swap(lhs, WVec)}($i).addr($j) := ${a}.r """)
        }
      }
      duplicatesOf(sram).zipWithIndex.foreach{ case (mem, i) =>
        emit(src"""${sram}_$i.connectWPort(${swap(lhs, WVec)}, List(${portsOf(lhs, sram, i)}))""")
      }

    case _ => super.emitNode(lhs, rhs)
  }


  override protected def emitFileFooter() {
    if (config.multifile == 5 || config.multifile == 6) {
      withStream(getStream("Mapping")) {
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
    withStream(getStream("IOModule")) {
      emit("""// Set Build Info""")
      val trgt = s"${spatialConfig.target.name}".replace("DE1", "de1soc")
      if (config.multifile == 5 || config.multifile == 6) {
        pipeRtMap.groupBy(_._1._1).map{x => 
          val listBuilder = x._2.toList.sortBy(_._1._2).map(_._2)
          emit(src"val ${listHandle(x._1)}_rtmap = List(${listBuilder.mkString(",")})")
        }
        // TODO: Make the things below more efficient
        compressorMap.values.map(_._1).toSet.toList.foreach{wire: String => 
          if (wire == "_retime") {
            emit(src"val ${listHandle(wire)} = List[Int](${retimeList.mkString(",")})")  
          }
        }
        compressorMap.values.map(_._1).toSet.toList.foreach{wire: String => 
          if (wire == "_retime") {
          } else if (wire.contains("pipe(") || wire.contains("inner(")) {
            val numel = compressorMap.filter(_._2._1 == wire).size
            emit(src"val ${listHandle(wire)} = List.tabulate(${numel}){i => ${wire.replace("))", src",retime=${listHandle("_retime")}(${listHandle(wire)}_rtmap(i))))")}}")
          } else {
            val numel = compressorMap.filter(_._2._1 == wire).size
            emit(src"val ${listHandle(wire)} = List.fill(${numel}){${wire}}")            
          }
        }
      }

      emit(s"Utils.fixmul_latency = ${latencyOption("FixMul", Some(1))}")
      emit(s"Utils.fixdiv_latency = ${latencyOption("FixDiv", Some(1))}")
      emit(s"Utils.fixadd_latency = ${latencyOption("FixAdd", Some(1))}")
      emit(s"Utils.fixsub_latency = ${latencyOption("FixSub", Some(1))}")
      emit(s"Utils.fixmod_latency = ${latencyOption("FixMod", Some(1))}")
      emit(s"Utils.fixeql_latency = ${latencyOption("FixEql", None)}.toInt")
      emit(s"Utils.tight_control   = ${spatialConfig.enableTightControl}")
      emit(s"Utils.mux_latency    = ${latencyOption("Mux", None)}.toInt")
      emit(s"Utils.sramload_latency    = ${latencyOption("SRAMLoad", None)}.toInt")
      emit(s"Utils.sramstore_latency    = ${latencyOption("SRAMStore", None)}.toInt")
      emit(s"Utils.SramThreshold = 0")
      emit(s"""Utils.target = ${trgt}""")
      emit(s"""Utils.retime = ${spatialConfig.enableRetiming}""")
    }
    withStream(getStream("BufferControlCxns")) {
      nbufs.foreach{ case (mem, i) => 
        val info = bufferControlInfo(mem, i)
        info.zipWithIndex.foreach{ case (inf, port) => 
          emit(src"""${mem}_${i}.connectStageCtrl(${DL(swap(quote(inf._1), Done), 1, true)}, ${swap(quote(inf._1), BaseEn)}, List(${port})) ${inf._2}""")
        }
      }
    }

    super.emitFileFooter()
  }
    
} 
