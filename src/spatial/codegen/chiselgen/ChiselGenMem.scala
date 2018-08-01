package spatial.codegen.chiselgen

import argon._
import spatial.lang._
import spatial.node._
import spatial.metadata.access._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.retiming._
import spatial.metadata.types._
import spatial.util.spatialConfig

trait ChiselGenMem extends ChiselGenCommon {

  private var nbufs: List[Sym[_]] = List()

  private def emitRead(lhs: Sym[_], mem: Sym[_], bank: Seq[Seq[Sym[_]]], ofs: Seq[Sym[_]], ens: Seq[Set[Bit]]): Unit = {
    val rPar = lhs.accessWidth
    val width = bitWidth(mem.tp.typeArgs.head)
    val parent = lhs.parent.s.get 
    emitControlSignals(parent) // Hack for compressWires > 0, when RegRead in outer control is used deeper in the hierarchy
    val invisibleEnable = src"""${DL(src"${swap(parent, DatapathEn)} & ${swap(parent, IIDone)}", lhs.fullDelay, true)}"""
    val flowEnable = if (getAllReadyLogic(parent.toCtrl).nonEmpty) src""",${getAllReadyLogic(parent.toCtrl).mkString(" & ")}""" else ""
    val ofsWidth = Math.max(1, Math.ceil(scala.math.log(mem.constDims.product/mem.instance.nBanks.product)/scala.math.log(2)).toInt)
    val banksWidths = if (mem.isRegFile || mem.isLUT) mem.constDims.map{x => Math.ceil(scala.math.log(x)/scala.math.log(2)).toInt}
                      else mem.instance.nBanks.map{x => Math.ceil(scala.math.log(x)/scala.math.log(2)).toInt}
    val isBroadcast = lhs.ports(0).values.head.bufferPort.isEmpty & mem.instance.depth > 1
    val bufferPort = lhs.ports(0).values.head.bufferPort.getOrElse(-1)
    val muxPort = lhs.ports(0).values.head.muxPort
    val muxOfs = lhs.ports(0).values.head.muxOfs

    lhs.tp match {
      case _: Vec[_] => emitGlobalWireMap(src"""${lhs}""", src"""Wire(Vec(${ens.length}, ${mem.tp.typeArgs.head}))""") 
      case _ => emitGlobalWireMap(src"""${lhs}""", src"""Wire(${mem.tp.typeArgs.head})""") 
    }

    if (lhs.isDirectlyBanked & !isBroadcast) {
      emitGlobalWireMap(src"""${lhs}_port""", s"Wire(new R_Direct(${ens.length}, $ofsWidth, ${bank.flatten.map(_.trace.toInt).mkString("List(",",",")")}.grouped(${bank.head.length}).toList))") 
      emitt(src"""${lhs}.toSeq.zip(${mem}.connectDirectRPort(${swap(src"${lhs}_port", Blank)}, $bufferPort, ($muxPort, $muxOfs) $flowEnable)).foreach{case (left, right) => left.r := right}""")
    } else if (isBroadcast) {
      val bankString = bank.flatten.map(quote(_) + ".r").mkString("List[UInt](", ",", ")")
      emitGlobalWireMap(src"""${lhs}_port""", s"Wire(new R_XBar(${ens.length}, $ofsWidth, ${banksWidths.mkString("List(",",",")")}))") 
      emitt(src"""${swap(src"${lhs}_port", Blank)}.banks.zip($bankString.map(_.rd)).foreach{case (left, right) => left.r := right}""")
      emitt(src"""${lhs}.toSeq.zip(${mem}.connectBroadcastRPort(${swap(src"${lhs}_port", Blank)}, ($muxPort, $muxOfs) $flowEnable)).foreach{case (left, right) => left.r := right}""")
    } else {
      val bankString = bank.flatten.map(quote(_) + ".r").mkString("List[UInt](", ",", ")")
      emitGlobalWireMap(src"""${lhs}_port""", s"Wire(new R_XBar(${ens.length}, $ofsWidth, ${banksWidths.mkString("List(",",",")")}))") 
      emitt(src"""${swap(src"${lhs}_port", Blank)}.banks.zip($bankString.map(_.rd)).foreach{case (left, right) => left.r := right}""")
      emitt(src"""${lhs}.toSeq.zip(${mem}.connectXBarRPort(${swap(src"${lhs}_port", Blank)}, $bufferPort, ($muxPort, $muxOfs) $flowEnable)).foreach{case (left, right) => left.r := right}""")
    }
    val ensString = ens.map{e => and(e)}.mkString("List(",",",")")
    emitt(src"""${swap(src"${lhs}_port", Blank)}.en.zip(${ensString}.toSeq.map(_ && ${invisibleEnable})).foreach{case (left, right) => left := right}""")
    if (ofs.nonEmpty) emitt(src"""${swap(src"${lhs}_port", Blank)}.ofs.zip(${ofs.map(quote(_) + ".r").mkString("List[UInt](",",",")")}.toSeq.map(_.rd)).foreach{case (left, right) => left.r := right}""")
    
  }

  private def emitWrite(lhs: Sym[_], mem: Sym[_], data: Seq[Sym[_]], bank: Seq[Seq[Sym[_]]], ofs: Seq[Sym[_]], ens: Seq[Set[Bit]], shiftAxis: Option[Int] = None): Unit = {
    val wPar = ens.length
    val width = bitWidth(mem.tp.typeArgs.head)
    val parent = lhs.parent.s.get //switchCaseLookaheadHack(lhs.parent.s.get)
    val invisibleEnable = src"""${DL(src"${swap(parent, DatapathEn)} & ${swap(parent, IIDone)}", lhs.fullDelay, true)}"""
    val ofsWidth = 1 max Math.ceil(scala.math.log(mem.constDims.product / mem.instance.nBanks.product) / scala.math.log(2)).toInt
    val banksWidths = if (mem match {case Op(_:RegFileNew[_,_]) => true; case Op(_:LUTNew[_,_]) => true; case _ => false}) mem.constDims.map{x => Math.ceil(scala.math.log(x)/scala.math.log(2)).toInt}
                      else mem.instance.nBanks.map{x => Math.ceil(scala.math.log(x)/scala.math.log(2)).toInt}
    val isBroadcast = lhs.ports(0).values.head.bufferPort.isEmpty & mem.instance.depth > 1
    val bufferPort = lhs.ports(0).values.head.bufferPort.getOrElse(-1)
    val muxPort = lhs.ports(0).values.head.muxPort
    val muxOfs = lhs.ports(0).values.head.muxOfs

    val enport = if (shiftAxis.isDefined) "shiftEn" else "en"
    if (lhs.isDirectlyBanked && !isBroadcast) {
      emitGlobalWireMap(src"""${lhs}_port""", s"Wire(new W_Direct(${data.length}, $ofsWidth, ${bank.flatten.map(_.trace.toInt).mkString("List(",",",")")}.grouped(${bank.head.length}).toList, $width))") 
      emitt(src"""${mem}.connectDirectWPort(${swap(src"${lhs}_port", Blank)}, $bufferPort, (${muxPort}, $muxOfs))""")
    } else if (isBroadcast) {
      val bankString = bank.flatten.map(quote(_) + ".r").mkString("List[UInt](", ",", ")")
      emitGlobalWireMap(src"""${lhs}_port""", s"Wire(new W_XBar(${data.length}, $ofsWidth, ${banksWidths.mkString("List(",",",")")}, $width))") 
      emitt(src"""${swap(src"${lhs}_port", Blank)}.banks.zip($bankString.map(_.rd)).foreach{case (left, right) => left.r := right}""")
      emitt(src"""${mem}.connectBroadcastWPort(${swap(src"${lhs}_port", Blank)}, ($muxPort, $muxOfs))""")        
    } else {
      val bankString = bank.flatten.map(quote(_) + ".r").mkString("List[UInt](", ",", ")")
      emitGlobalWireMap(src"""${lhs}_port""", s"Wire(new W_XBar(${data.length}, $ofsWidth, ${banksWidths.mkString("List(",",",")")}, $width))") 
      emitt(src"""${swap(src"${lhs}_port", Blank)}.banks.zip($bankString.map(_.rd)).foreach{case (left, right) => left.r := right}""")
      emitt(src"""${mem}.connectXBarWPort(${swap(src"${lhs}_port", Blank)}, $bufferPort, (${muxPort}, $muxOfs))""")
    }
    val ensString = ens.map{e => and(e)}.mkString("List(",",",")")
    emitt(src"""${swap(src"${lhs}_port", Blank)}.$enport.zip(${ensString}.toSeq.map(_ && ${invisibleEnable})).foreach{case (left, right) => left := right}""")
    if (ofs.nonEmpty) emitt(src"""${swap(src"${lhs}_port", Blank)}.ofs.zip(${ofs.map(quote(_) + ".r").mkString("List[UInt](",",",")")}.toSeq.map(_.rd)).foreach{case (left, right) => left.r := right}""")
    emitt(src"""${swap(src"${lhs}_port", Blank)}.data.zip(${data.map(quote(_) + ".r").mkString("List[UInt](",",",")")}).foreach{case (left, right) => left.r := right}""")
  }

  private def emitMem(mem: Sym[_], name: String, init: Option[Seq[Sym[_]]]): Unit = {
    val inst = mem.instance
    val dims = if (name == "FF") List(1) else mem.constDims
    val padding = if (name == "FF") List(0) else mem.getPadding.getOrElse(Seq.fill(dims.length)(0))
    val broadcastWrites = mem.writers.filter{w => w.ports(0).values.head.bufferPort.isEmpty & inst.depth > 1}.zipWithIndex.map{case (a,i) => src"($i,0) -> (${a.accessWidth}, ${a.shiftAxis})"}.toList
    val broadcastReads = mem.readers.filter{w => w.ports(0).values.head.bufferPort.isEmpty & inst.depth > 1}.zipWithIndex.map{case (a,i) => src"($i,0) -> (${a.accessWidth}, ${a.shiftAxis})"}.toList

    val templateName = if (inst.depth == 1) s"${name}("
                       else {appPropertyStats += HasNBufSRAM; nbufs = nbufs :+ mem; s"NBufMem(${name}Type, "}

    val depth = if (inst.depth > 1) s"${inst.depth}," else ""
    val nbuf = if (inst.depth > 1) "NBuf" else ""
    def outerMap(t: String): String = if (inst.depth > 1) s"NBuf${t}Map" else s"${t}Map"
    def innerMap(t: String): String = s"${t}Map"
    // Create mapping for (bufferPort -> (muxPort -> width)) for XBar accesses
    val XBarW = s"${outerMap("X")}(" + mem.writers.filter(_.ports(0).values.head.bufferPort.isDefined | inst.depth == 1) // Filter out broadcasters
                              .filterNot(_.isDirectlyBanked)              // Filter out statically banked
                              .filter(_.accessWidth > 0)
                              .groupBy(_.ports(0).values.head.bufferPort.getOrElse(-1))      // Group by port
                              .map{case(bufp, writes) => 
                                if (inst.depth > 1) src"$bufp -> ${innerMap("X")}(" + writes.map{w => src"(${w.ports(0).values.head.muxPort},${w.ports(0).values.head.muxOfs}) -> (${w.accessWidth}, ${w.shiftAxis})"}.mkString(",") + ")"
                                else writes.map{w => src"(${w.ports(0).values.head.muxPort},${w.ports(0).values.head.muxOfs}) -> (${w.accessWidth}, ${w.shiftAxis})"}.mkString(",")
                              }.mkString(",") + ")"
    val XBarR = s"${outerMap("X")}(" + mem.readers.filter(_.ports(0).values.head.bufferPort.isDefined | inst.depth == 1) // Filter out broadcasters
                              .filterNot(_.isDirectlyBanked)              // Filter out statically banked
                              .filter(_.accessWidth > 0)
                              .groupBy(_.ports(0).values.head.bufferPort.getOrElse(-1))      // Group by port
                              .map{case(bufp, reads) => 
                                if (inst.depth > 1) src"$bufp -> ${innerMap("X")}(" + reads.map{r => src"(${r.ports(0).values.head.muxPort},${r.ports(0).values.head.muxOfs}) -> (${r.accessWidth}, ${r.shiftAxis})"}.mkString(",") + ")"
                                else reads.map{r => src"(${r.ports(0).values.head.muxPort},${r.ports(0).values.head.muxOfs}) -> (${r.accessWidth}, ${r.shiftAxis})"}.mkString(",")
                              }.mkString(",") + ")"
    val DirectW = s"${outerMap("D")}(" + mem.writers.filter(_.ports(0).values.head.bufferPort.isDefined | inst.depth == 1) // Filter out broadcasters
                              .filter(_.isDirectlyBanked)              // Filter out dynamically banked
                              .filter(_.accessWidth > 0)
                              .groupBy(_.ports(0).values.head.bufferPort.getOrElse(-1))      // Group by port
                              .map{case(bufp, writes) => 
                                if (inst.depth > 1) src"$bufp -> ${innerMap("D")}(" + writes.map{w => src"(${w.ports(0).values.head.muxPort},${w.ports(0).values.head.muxOfs}) -> " + s"(${w.banks.map(_.map(_.trace.toInt).mkString("Banks(",",",")"))}, ${w.shiftAxis})".replace("Vector","List")}.mkString(",") + ")"
                                else writes.map{w => src"(${w.ports(0).values.head.muxPort},${w.ports(0).values.head.muxOfs}) -> " + s"(${w.banks.map(_.map(_.trace.toInt).mkString("Banks(",",",")"))}, ${w.shiftAxis})".replace("Vector","List")}.mkString(",")
                              }.mkString(",") + ")"
    val DirectR = s"${outerMap("D")}(" + mem.readers.filter(_.ports(0).values.head.bufferPort.isDefined | inst.depth == 1) // Filter out broadcasters
                              .filter(_.isDirectlyBanked)              // Filter out dynamically banked
                              .filter(_.accessWidth > 0)
                              .groupBy(_.ports(0).values.head.bufferPort.getOrElse(-1))      // Group by port
                              .map{case(bufp, reads) => 
                                if (inst.depth > 1) src"$bufp -> ${innerMap("D")}(" + reads.map{w => src"(${w.ports(0).values.head.muxPort},${w.ports(0).values.head.muxOfs}) -> " + s"(${w.banks.map(_.map(_.trace.toInt).mkString("Banks(",",",")"))}, ${w.shiftAxis})".replace("Vector","List")}.mkString(",") + ")"
                                else reads.map{w => src"(${w.ports(0).values.head.muxPort},${w.ports(0).values.head.muxOfs}) -> " + s"(${w.banks.map(_.map(_.trace.toInt).mkString("Banks(",",",")"))}, ${w.shiftAxis})".replace("Vector","List")}.mkString(",")
                              }.mkString(",") + ")"
    val BXBarW = if (inst.depth > 1) s"${innerMap("X")}(" + broadcastWrites.mkString(",") + ")," else ""
    val BXBarR = if (inst.depth > 1) s"${innerMap("X")}(" + broadcastReads.mkString(",") + ")," else ""

    val dimensions = dims.zip(padding).map{case (d,p) => s"$d+$p"}.mkString("List[Int](", ",", ")")
    val numBanks = {if (mem.isLUT | mem.isRegFile) dims else inst.nBanks}.map(_.toString).mkString("List[Int](", ",", ")")
    val strides = numBanks // TODO: What to do with strides
    val bankingMode = "BankedMemory" // TODO: Find correct one

    val initStr = if (init.isDefined) init.get.map(quoteAsScala).map(x => src"${x}.toDouble").mkString("Some(List(",",","))")
      else "None"
    emitGlobalModule(src"""val $mem = Module(new $templateName $dimensions, $depth ${bitWidth(mem.tp.typeArgs.head)}, $numBanks, $strides, $XBarW, $XBarR, $DirectW, $DirectR, $BXBarW $BXBarR $bankingMode, $initStr, ${!spatialConfig.enableAsyncMem && spatialConfig.enableRetiming}, ${fracBits(mem.tp.typeArgs.head)}))""")
  }



  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {

    // SRAMs
    case op: SRAMNew[_,_] => emitMem(lhs, "SRAM", None)
    case op@SRAMBankedRead(sram,bank,ofs,ens) => emitRead(lhs, sram, bank, ofs, ens)
    case op@SRAMBankedWrite(sram,data,bank,ofs,ens) => emitWrite(lhs, sram, data, bank, ofs, ens)

    // Registers
    case RegNew(init) => emitMem(lhs, "FF", Some(List(init)))
    case RegWrite(reg, data, ens) if (!reg.isArgOut & !reg.isArgIn & !reg.isHostIO & (!spatialConfig.enableOptimizedReduce || (lhs.fmaReduceInfo.isEmpty))) => 
      emitWrite(lhs, reg, Seq(data), Seq(Seq()), Seq(), Seq(ens))
    case RegRead(reg)  if (!reg.isArgOut & !reg.isArgIn & !reg.isHostIO & (!spatialConfig.enableOptimizedReduce || (lhs.fmaReduceInfo.isEmpty))) => 
      emitRead(lhs, reg, Seq(Seq()), Seq(), Seq(Set()))
    // Specialized FMA Register
    case RegWrite(reg, data, ens) if (spatialConfig.enableOptimizedReduce && (lhs.fmaReduceInfo.isDefined)) => 
      
    case RegRead(reg)  if (spatialConfig.enableOptimizedReduce && (lhs.fmaReduceInfo.isDefined)) => 
      val info = lhs.fmaReduceInfo.get
      val latency = latencyOption("FixFMA", Some(bitWidth(lhs.tp)))
      val FixPtType(s,d,f) = lhs.tp
      val Op(RegNew(init)) = reg
      val treeLatency = scala.math.ceil(scala.math.log(info._5)/scala.math.log(2))
      emitGlobalModule(src"val ${reg}_accum = Module(new FixFMAAccum(${info._5}, ${latency}, $s,$d,$f, ${quoteAsScala(init)}))")
      emitt(src"${reg}_accum.io.input1 := ${info._2}.r")
      emitt(src"${reg}_accum.io.input2 := ${info._3}.r")
      emitt(src"""${reg}_accum.io.enable := ${DL(src"${swap(lhs.parent.s.get, DatapathEn)} & ${swap(lhs.parent.s.get, IIDone)}", info._4.fullDelay, true)}""")
      emitt(src"""${reg}_accum.io.reset := ${DL(src"${swap(lhs.parent.s.get, Done)}", info._4.fullDelay + treeLatency + 1, true)}""")
      emitGlobalWireMap(src"${info._1}", src"Wire(${info._1.tp})")
      emitt(src"""${info._1}.r := ${reg}_accum.io.output""")

    // RegFiles
    case op@RegFileNew(_, inits) => emitMem(lhs, "ShiftRegFile", inits)
    case RegFileReset(rf, en)    => 
      // val parent = lhs.parent.s.get
      // val id = resettersOf(rf).map{_._1}.indexOf(lhs)
      // duplicatesOf(rf).indices.foreach{i => emitt(src"${rf}_${i}_manual_reset_$id := $en & ${DL(swap(parent, DatapathEn), enableRetimeMatch(en, lhs), true)} ")}

    case RegFileShiftInVector(rf,data,addr,en,axis)  => emitWrite(lhs,rf,data.elems.map(_.asInstanceOf[Sym[_]]).toSeq,Seq(addr),Seq(),Seq(en), Some(axis))
    case RegFileShiftIn(rf,data,addr,en,axis)        => emitWrite(lhs,rf,Seq(data),Seq(addr),Seq(),Seq(en), Some(axis))
    case RegFileBankedShiftIn(rf,data,addr,en,axis)  => emitWrite(lhs,rf,data,addr,Seq(),en, Some(axis))

    // TODO: Matt are these correct?
    case RegFileVectorRead(rf,addr,ens)       => emitRead(lhs,rf,addr,addr.map{_ => I32(0) },ens)
    case RegFileVectorWrite(rf,data,addr,ens) => emitWrite(lhs,rf,data,addr,addr.map{_ => I32(0) },ens)

    // FIFOs
    case FIFONew(depths) => emitMem(lhs, "FIFO", None)
    case FIFOIsEmpty(fifo,_) => emitt(src"val $lhs = $fifo.io.asInstanceOf[FIFOInterface].empty")
    case FIFOIsFull(fifo,_)  => emitt(src"val $lhs = $fifo.io.asInstanceOf[FIFOInterface].full")
    case FIFOIsAlmostEmpty(fifo,_) => emitt(src"val $lhs = $fifo.io.asInstanceOf[FIFOInterface].almostEmpty")
    case FIFOIsAlmostFull(fifo,_) => emitt(src"val $lhs = $fifo.io.asInstanceOf[FIFOInterface].almostFull")
    case op@FIFOPeek(fifo,_) => emitt(src"val $lhs = $fifo.io.output.data(0)")
    case FIFONumel(fifo,_)   => emitt(src"val $lhs = $fifo.io.asInstanceOf[FIFOInterface].numel")
    case op@FIFOBankedDeq(fifo, ens) => emitRead(lhs, fifo, Seq.fill(ens.length)(Seq()), Seq(), ens)
    case FIFOBankedEnq(fifo, data, ens) => emitWrite(lhs, fifo, data, Seq.fill(ens.length)(Seq()), Seq(), ens)

    // LIFOs
    case LIFONew(depths) => emitMem(lhs, "LIFO", None)
    case LIFOIsEmpty(fifo,_) => emitt(src"val $lhs = $fifo.io.asInstanceOf[FIFOInterface].empty")
    case LIFOIsFull(fifo,_)  => emitt(src"val $lhs = $fifo.io.asInstanceOf[FIFOInterface].full")
    case LIFOIsAlmostEmpty(fifo,_) => emitt(src"val $lhs = $fifo.io.asInstanceOf[FIFOInterface].almostEmpty")
    case LIFOIsAlmostFull(fifo,_) => emitt(src"val $lhs = $fifo.io.asInstanceOf[FIFOInterface].almostFull")
    case op@LIFOPeek(fifo,_) => emitt(src"val $lhs = $fifo.io.output.data(0)")
    case LIFONumel(fifo,_)   => emitt(src"val $lhs = $fifo.io.asInstanceOf[FIFOInterface].numel")
    case op@LIFOBankedPop(fifo, ens) => emitRead(lhs, fifo, Seq.fill(ens.length)(Seq()), Seq(), ens)
    case LIFOBankedPush(fifo, data, ens) => emitWrite(lhs, fifo, data, Seq.fill(ens.length)(Seq()), Seq(), ens)
    
    // LUTs
    case op@LUTNew(dims, init) => emitMem(lhs, "LUT", Some(init))
    case op@LUTBankedRead(lut,bank,ofs,ens) => emitRead(lhs,lut,bank,ofs,ens)

    case _ => super.gen(lhs, rhs)
  }

  protected def bufferControlInfo(mem: Sym[_]): List[Sym[_]] = {
    val accesses = mem.accesses.filter(_.ports(0).values.head.bufferPort.isDefined)

    var specialLB = false
    // val readCtrls = readPorts.map{case (port, readers) =>
    //   val readTops = readers.flatMap{a => topControllerOf(a, mem, i) }
    //   mem match {
    //     case Def(_:LineBufferNew[_]) => // Allow empty lca, meaning we use a sequential pipe for rotations
    //       if (readTops.nonEmpty) {
    //         readTops.headOption.get.node
    //       } else {
    //         warn(u"Memory $mem, instance $i, port $port had no read top controllers.  Consider wrapping this linebuffer in a metapipe to get better speedup")
    //         specialLB = true
    //         // readTops.headOption.getOrElse{throw new Exception(u"Memory $mem, instance $i, port $port had no read top controllers") }    
    //         readers.head.node
    //       }
    //     case _ =>
    //       readTops.headOption.getOrElse{throw new Exception(u"Memory $mem, instance $i, port $port had no read top controllers") }.node    
    //   }
      
    // }
    // if (readCtrls.isEmpty) throw new Exception(u"Memory $mem, instance $i had no readers?")

    // childrenOf(parentOf(readPorts.map{case (_, readers) => readers.flatMap{a => topControllerOf(a,mem,i)}.head}.head.node).get)
    if (!specialLB && accesses.nonEmpty) {
      val lca = if (accesses.size == 1) accesses.head.parent else LCA(accesses)
      val (basePort, numPorts) = if (lca.s.get.isInnerControl) (0,0) else LCAPortMatchup(accesses.toList, lca)
      val info = if (lca.s.get.isInnerControl) List[Sym[_]]() else (basePort to {basePort+numPorts}).map { port => lca.children.toList(port).s.get }
      info.toList
    } else {
      throw new Exception("Implement LB with transient buffering")
      // // Assume write comes before read and there is only one write
      // val writer = writers.head.ctrl._1
      // val reader = readers.head.ctrl._1
      // val lca = leastCommonAncestorWithPaths[Exp[_]](reader, writer, {node => parentOf(node)})._1.get
      // val allSiblings = childrenOf(lca)
      // var writeSibling: Option[Exp[Any]] = None
      // var candidate = writer
      // while (!writeSibling.isDefined) {
      //   if (allSiblings.contains(candidate)) {
      //     writeSibling = Some(candidate)
      //   } else {
      //     candidate = parentOf(candidate).get
      //   }
      // }
      // // Get LCA of read and write
      // List((writeSibling.get, src"/*seq write*/"))
    }

  }

  override def emitFooter(): Unit = {
    inAccel{

      inGenn(out, "BufferControlCxns", ext) {
        nbufs.foreach{ mem => 
          val info = bufferControlInfo(mem)
          info.zipWithIndex.foreach{ case (node, port) => 
            emitt(src"""${mem}.connectStageCtrl(${DL(swap(quote(node), Done), 1, true)}, ${swap(quote(node), BaseEn)}, ${port})""")
          }
        }
      }

    }
    super.emitFooter()
  }

}