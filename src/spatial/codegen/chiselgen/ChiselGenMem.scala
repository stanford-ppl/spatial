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
  private var memsWithReset: List[Sym[_]] = List()

  val zipThreshold = 100 // Max number of characters before deciding to split line into many
  private def zipAndConnect(lhs: Sym[_], mem: Sym[_], port: String, tp: String, payload: Seq[String], suffix: String): Unit = {
    val totalPayload = payload.mkString(src"List[$tp](", ",", ")")
    if (totalPayload.length < zipThreshold) {
      val rdPayload = totalPayload + suffix
      emitt(src"""${swap(src"${lhs}_port", Blank)}.$port.zip($rdPayload).foreach{case (left, right) => left.r := right}""")
    }
    else {
      val groupSize = (payload.length / (totalPayload.length / zipThreshold)).toInt
      val groups = payload.grouped(groupSize).toList
      groups.zipWithIndex.foreach{case (group, i) => 
        val rdPayload = group.mkString(src"List[$tp](",",",")") + suffix
        emitt(src"""${swap(src"${lhs}_port", Blank)}.$port.drop(${i*groupSize}).take(${group.size}).zip($rdPayload).foreach{case (left, right) => left.r := right}""")
      }
    }
  }

  private def emitRead(lhs: Sym[_], mem: Sym[_], bank: Seq[Seq[Sym[_]]], ofs: Seq[Sym[_]], ens: Seq[Set[Bit]]): Unit = {
    if (lhs.segmentMapping.values.exists(_>0)) appPropertyStats += HasAccumSegmentation
    val name = if (mem.isInstanceOf[RegNew[_]]) "FF" else ""
    val rPar = lhs.accessWidth
    val width = bitWidth(mem.tp.typeArgs.head)
    val parent = lhs.parent.s.get 
    emitControlSignals(parent) // Hack for compressWires > 0, when RegRead in outer control is used deeper in the hierarchy
    val invisibleEnable = src"""${DL(src"${swap(parent, DatapathEn)} & ${swap(parent, IIDone)}", lhs.fullDelay, true)}"""
    val flowEnable = src",${swap(parent,SM)}.io.flow"
    val ofsWidth = if (!mem.isLineBuffer) Math.max(1, Math.ceil(scala.math.log(paddedDims(mem,name).product/mem.instance.nBanks.product)/scala.math.log(2)).toInt)
                     else Math.max(1, Math.ceil(scala.math.log(paddedDims(mem,name).last/mem.instance.nBanks.last)/scala.math.log(2)).toInt)
    val banksWidths = if (mem.isRegFile || mem.isLUT) paddedDims(mem,name).map{x => Math.ceil(scala.math.log(x)/scala.math.log(2)).toInt}
                      else mem.instance.nBanks.map{x => Math.ceil(scala.math.log(x)/scala.math.log(2)).toInt}

    val isBroadcast = lhs.port.bufferPort.isEmpty & mem.instance.depth > 1
    val bufferPort = lhs.port.bufferPort.getOrElse(-1)
    val muxPort = lhs.port.muxPort
    val muxOfs = lhs.port.muxOfs

    lhs.tp match {
      case _: Vec[_] => emitGlobalWireMap(src"""${lhs}""", src"""Wire(Vec(${ens.length}, ${mem.tp.typeArgs.head}))""") 
      case _ => emitGlobalWireMap(src"""${lhs}""", src"""Wire(${mem.tp.typeArgs.head})""") 
    }

    val ignoreCastInfo = !mem.broadcastsAnyRead
    val castgrps     = lhs.port.castgroup.mkString("List(",",",")")
    val broadcastids = lhs.port.broadcast.mkString("List(",",",")")
    if (lhs.isDirectlyBanked & !isBroadcast) {
      emitGlobalWireMap(src"""${lhs}_port""", s"Wire(new R_Direct(${ens.length}, $ofsWidth, ${bank.flatten.map(_.trace.toInt).mkString("List(",",",")")}.grouped(${bank.head.length}).toList))") 
      emitt(src"""${lhs}.toSeq.zip(${mem}.connectDirectRPort(${swap(src"${lhs}_port", Blank)}, $bufferPort, ($muxPort, $muxOfs),$castgrps, $broadcastids, $ignoreCastInfo $flowEnable)).foreach{case (left, right) => left.r := right}""")
    } else if (isBroadcast) {
      val banklist = bank.flatten.filter(!_.isBroadcastAddr).map(quote(_) + ".r")
      emitGlobalWireMap(src"""${lhs}_port""", s"Wire(new R_XBar(${ens.length}, $ofsWidth, ${banksWidths.mkString("List(",",",")")}))") 
      zipAndConnect(lhs, mem, "banks", "UInt", banklist, ".map(_.rd)")
      emitt(src"""${lhs}.toSeq.zip(${mem}.connectBroadcastRPort(${swap(src"${lhs}_port", Blank)}, ($muxPort, $muxOfs),$castgrps, $broadcastids, $ignoreCastInfo $flowEnable)).foreach{case (left, right) => left.r := right}""")
    } else {
      val banklist = bank.flatten.filter(!_.isBroadcastAddr).map(quote(_) + ".r")
      emitGlobalWireMap(src"""${lhs}_port""", s"Wire(new R_XBar(${ens.length}, $ofsWidth, ${banksWidths.mkString("List(",",",")")}))") 
      zipAndConnect(lhs, mem, "banks", "UInt", banklist, ".map(_.rd)")
      emitt(src"""${lhs}.toSeq.zip(${mem}.connectXBarRPort(${swap(src"${lhs}_port", Blank)}, $bufferPort, ($muxPort, $muxOfs),$castgrps, $broadcastids, $ignoreCastInfo $flowEnable)).foreach{case (left, right) => left.r := right}""")
    }
    val commonEns = ens.head.collect{case e if (ens.forall(_.contains(e)) && !e.isBroadcastAddr) => e}
    val enslist = ens.map{e => and(e.filter(!commonEns.contains(_)).filter(!_.isBroadcastAddr))}
    zipAndConnect(lhs, mem, "en", "Bool", enslist, src".toSeq.map(_ && ${invisibleEnable} && ${and(commonEns)})")
    if (ofs.nonEmpty) zipAndConnect(lhs, mem, "ofs", "UInt", ofs.filter(!_.isBroadcastAddr).map(quote(_) + ".r"), ".toSeq.map(_.rd)")
  }

  private def emitWrite(lhs: Sym[_], mem: Sym[_], data: Seq[Sym[_]], bank: Seq[Seq[Sym[_]]], ofs: Seq[Sym[_]], ens: Seq[Set[Bit]], shiftAxis: Option[Int] = None): Unit = {
    if (lhs.segmentMapping.values.exists(_>0)) appPropertyStats += HasAccumSegmentation
    val name = if (mem.isInstanceOf[RegNew[_]]) "FF" else ""
    val wPar = ens.length
    val width = bitWidth(mem.tp.typeArgs.head)
    val parent = lhs.parent.s.get
    val flowEnable = src"${swap(parent,SM)}.io.flow"
    val invisibleEnable = src"""${DL(src"${swap(parent, DatapathEn)} & ${swap(parent, IIDone)}", lhs.fullDelay, true)} & $flowEnable"""
    val ofsWidth = if (!mem.isLineBuffer) 1 max Math.ceil(scala.math.log(paddedDims(mem,name).product / mem.instance.nBanks.product) / scala.math.log(2)).toInt
                   else 1 max Math.ceil(scala.math.log(paddedDims(mem,name).last / mem.instance.nBanks.last) / scala.math.log(2)).toInt
    val banksWidths = if (mem match {case Op(_:RegFileNew[_,_]) => true; case Op(_:LUTNew[_,_]) => true; case _ => false}) paddedDims(mem,name).map{x => Math.ceil(scala.math.log(x)/scala.math.log(2)).toInt}
                      else mem.instance.nBanks.map{x => Math.ceil(scala.math.log(x)/scala.math.log(2)).toInt}

    val isBroadcast = lhs.port.bufferPort.isEmpty & mem.instance.depth > 1
    val bufferPort = lhs.port.bufferPort.getOrElse(-1)
    val muxPort = lhs.port.muxPort
    val muxOfs = lhs.port.muxOfs

    val enport = if (shiftAxis.isDefined) "shiftEn" else "en"
    if (lhs.isDirectlyBanked && !isBroadcast) {
      emitGlobalWireMap(src"""${lhs}_port""", s"Wire(new W_Direct(${data.length}, $ofsWidth, ${bank.flatten.map(_.trace.toInt).mkString("List(",",",")")}.grouped(${bank.head.length}).toList, $width))") 
      emitt(src"""${mem}.connectDirectWPort(${swap(src"${lhs}_port", Blank)}, $bufferPort, (${muxPort}, $muxOfs))""")
    } else if (isBroadcast) {
      val banklist = bank.flatten.map(quote(_) + ".r")
      emitGlobalWireMap(src"""${lhs}_port""", s"Wire(new W_XBar(${data.length}, $ofsWidth, ${banksWidths.mkString("List(",",",")")}, $width))") 
      zipAndConnect(lhs, mem, "banks", "UInt", banklist, ".map(_.rd)")
      emitt(src"""${mem}.connectBroadcastWPort(${swap(src"${lhs}_port", Blank)}, ($muxPort, $muxOfs))""")        
    } else {
      val banklist = bank.flatten.map(quote(_) + ".r")
      emitGlobalWireMap(src"""${lhs}_port""", s"Wire(new W_XBar(${data.length}, $ofsWidth, ${banksWidths.mkString("List(",",",")")}, $width))") 
      zipAndConnect(lhs, mem, "banks", "UInt", banklist, ".map(_.rd)")
      emitt(src"""${mem}.connectXBarWPort(${swap(src"${lhs}_port", Blank)}, $bufferPort, (${muxPort}, $muxOfs))""")
    }
    val commonEns = ens.head.collect{case e if ens.forall(_.contains(e)) => e}
    val enslist = ens.map{e => and(e.filter(!commonEns.contains(_)))}
    zipAndConnect(lhs, mem, enport, "Bool", enslist, src".toSeq.map(_ && ${invisibleEnable} && ${and(commonEns)})")
    if (ofs.nonEmpty) zipAndConnect(lhs, mem, "ofs", "UInt", ofs.map(quote(_) + ".r"), ".toSeq.map(_.rd)")
    zipAndConnect(lhs, mem, "data", "UInt", data.map(quote(_) + ".r"), "")
  }

  private def paddedDims(mem: Sym[_], name: String): Seq[Int] = {
    val dims = if (name == "FF") List(1) else mem.constDims
    val padding = if (name == "FF") List(0) else mem.getPadding.getOrElse(Seq.fill(dims.length)(0))
    dims.zip(padding).map{case (d:Int,p:Int) => d+p}
  }

  private def emitMem(mem: Sym[_], name: String, init: Option[Seq[Sym[_]]]): Unit = {
    if (mem.dephasedAccesses.nonEmpty) appPropertyStats += HasDephasedAccess
    val inst = mem.instance
    val dims = if (name == "FF") List(1) else mem.constDims
    // val padding = if (name == "FF") List(0) else mem.getPadding.getOrElse(Seq.fill(dims.length)(0))
    val broadcastWrites = mem.writers.filter{w => w.port.bufferPort.isEmpty & inst.depth > 1}
                                     .map{w => src"(${w.port.muxPort},${w.port.muxOfs},0) -> (${w.accessWidth}, ${w.shiftAxis})"}.toList
    val broadcastReads = mem.readers.filter{w => w.port.bufferPort.isEmpty & inst.depth > 1}
                                    .map{w => src"(${w.port.muxPort},${w.port.muxOfs},0) -> (${w.accessWidth}, ${w.shiftAxis})"}.toList

    if (inst.depth == 1 && name == "LineBuffer") throw new Exception(s"Cannot create non-buffered line buffer!  Make sure $mem has readers and writers bound by a pipelined LCA, or else turn it into an SRAM")
    val templateName = if (inst.depth == 1 && name != "LineBuffer") s"${name}("
                       else {{if (name == "SRAM") appPropertyStats += HasNBufSRAM}; nbufs = nbufs :+ mem; s"NBufMem(${name}Type, "}
    if (mem.broadcastsAnyRead) appPropertyStats += HasBroadcastRead

    val depth = if (inst.depth > 1) s"${inst.depth}," else ""
    val nbuf = if (inst.depth > 1) "NBuf" else ""
    def outerMap(t: String): String = if (inst.depth > 1) s"NBuf${t}Map" else s"${t}Map"
    def innerMap(t: String): String = s"${t}Map"
    // Strip the lanes if necessary and return ports for constructor
    def recomputePortInfo(muxOfs: Int, castgroup: Seq[Int], broadcast: Seq[Int]): Seq[(Int,Int,Int)] = {
      castgroup.zip(broadcast).zipWithIndex.collect{case ((cg,bid),i) if (bid == 0) => 
        (muxOfs+i,cg,castgroup.filter(_==cg).size)
      }.toSeq
    }
    // Create mapping for (bufferPort -> (muxPort -> width)) for XBar accesses
    val XBarW = s"${outerMap("X")}(" + mem.writers.filter(_.port.bufferPort.isDefined | inst.depth == 1) // Filter out broadcasters
                      .filterNot(_.isDirectlyBanked)              // Filter out statically banked
                      .filter(_.accessWidth > 0)
                      .groupBy(_.port.bufferPort.getOrElse(-1))      // Group by port
                      .map{case(bufp, writes) => 
                        if (inst.depth > 1) src"$bufp -> ${innerMap("X")}(" + writes.map{w => src"(${w.port.muxPort},${w.port.muxOfs},0) -> (${w.accessWidth}, ${w.shiftAxis})"}.mkString(",") + ")"
                        else writes.map{w => src"(${w.port.muxPort},${w.port.muxOfs},0) -> (${w.accessWidth}, ${w.shiftAxis})"}.mkString(",")
                      }.mkString(",") + ")"
    val XBarR = s"${outerMap("X")}(" + mem.readers.filter(_.port.bufferPort.isDefined | inst.depth == 1) // Filter out broadcasters
                      .filterNot(_.isDirectlyBanked)              // Filter out statically banked
                      .filter(_.accessWidth > 0)
                      .groupBy(_.port.bufferPort.getOrElse(-1))      // Group by port
                      .map{case(bufp, reads) => 
                        if (mem.broadcastsAnyRead) { // Make generated code more readable if this memory doesn't actually broadcast anywhere
                          if (inst.depth > 1) src"$bufp -> ${innerMap("X")}(" + reads.map{r => 
                            recomputePortInfo(r.port.muxOfs, r.port.castgroup, r.port.broadcast).map{case (o,c,w) => 
                              src"(${r.port.muxPort},$o,$c) -> (${w}, ${r.shiftAxis})"
                            }.mkString(",")
                          }.filter(_.nonEmpty).mkString(",") + ")"
                          else reads.map{r =>
                            recomputePortInfo(r.port.muxOfs, r.port.castgroup, r.port.broadcast).map{case (o,c,w) => 
                              src"(${r.port.muxPort},$o,$c) -> (${w}, ${r.shiftAxis})"
                            }.mkString(",")
                          }.filter(_.nonEmpty).mkString(",")                                  
                        } else {
                          if (inst.depth > 1) src"$bufp -> ${innerMap("X")}(" + reads.map{r => src"(${r.port.muxPort},${r.port.muxOfs},0) -> (${r.accessWidth}, ${r.shiftAxis})"}.mkString(",") + ")"
                          else reads.map{r => src"(${r.port.muxPort},${r.port.muxOfs},0) -> (${r.accessWidth}, ${r.shiftAxis})"}.mkString(",")
                        }
                      }.mkString(",") + ")"
    val DirectW = s"${outerMap("D")}(" + mem.writers.filter(_.port.bufferPort.isDefined | inst.depth == 1) // Filter out broadcasters
                      .filter(_.isDirectlyBanked)              // Filter out dynamically banked
                      .filter(_.accessWidth > 0)
                      .groupBy(_.port.bufferPort.getOrElse(-1))      // Group by port
                      .map{case(bufp, writes) => 
                        if (inst.depth > 1) src"$bufp -> ${innerMap("D")}(" + writes.map{w => src"(${w.port.muxPort},${w.port.muxOfs},0) -> " + s"(${w.banks.map(_.map(_.trace.toInt).mkString("Banks(",",",")"))}, ${w.shiftAxis})".replace("Vector","List")}.mkString(",") + ")"
                        else writes.map{w => src"(${w.port.muxPort},${w.port.muxOfs},0) -> " + s"(${w.banks.map(_.map(_.trace.toInt).mkString("Banks(",",",")"))}, ${w.shiftAxis})".replace("Vector","List")}.mkString(",")
                      }.mkString(",") + ")"
    val DirectR = s"${outerMap("D")}(" + mem.readers.filter(_.port.bufferPort.isDefined | inst.depth == 1) // Filter out broadcasters
                      .filter(_.isDirectlyBanked)              // Filter out dynamically banked
                      .filter(_.accessWidth > 0)
                      .groupBy(_.port.bufferPort.getOrElse(-1))      // Group by port
                      .map{case(bufp, reads) => 
                        if (mem.broadcastsAnyRead) { // Make generated code more readable if this memory doesn't actually broadcast anywhere
                          if (inst.depth > 1) src"$bufp -> ${innerMap("D")}(" + reads.map{r => 
                            recomputePortInfo(r.port.muxOfs, r.port.castgroup, r.port.broadcast).map{case (o,c,_) => 
                              src"(${r.port.muxPort},$o,$c) -> " + s"(${r.banks.zip(r.port.castgroup).filter(_._2 == c).map(_._1).map(_.map(_.trace.toInt).mkString("Banks(",",",")"))}, ${r.shiftAxis})".replace("Vector","List")
                            }.mkString(",")
                          }.filter(_.nonEmpty).mkString(",") + ")"
                          else reads.map{r => 
                            recomputePortInfo(r.port.muxOfs, r.port.castgroup, r.port.broadcast).map{case (o,c,_) => 
                              src"(${r.port.muxPort},$o,$c) -> " + s"(${r.banks.zip(r.port.castgroup).filter(_._2 == c).map(_._1).map(_.map(_.trace.toInt).mkString("Banks(",",",")"))}, ${r.shiftAxis})".replace("Vector","List")
                            }.mkString(",")
                          }.filter(_.nonEmpty).mkString(",")
                        } else {
                          if (inst.depth > 1) src"$bufp -> ${innerMap("D")}(" + reads.map{r => src"(${r.port.muxPort},${r.port.muxOfs},0) -> " + s"(${r.banks.map(_.map(_.trace.toInt).mkString("Banks(",",",")"))}, ${r.shiftAxis})".replace("Vector","List")}.mkString(",") + ")"
                          else reads.map{r => src"(${r.port.muxPort},${r.port.muxOfs},0) -> " + s"(${r.banks.map(_.map(_.trace.toInt).mkString("Banks(",",",")"))}, ${r.shiftAxis})".replace("Vector","List")}.mkString(",")
                        }
                      }.mkString(",") + ")"
    val BXBarW = if (inst.depth > 1) s"${innerMap("X")}(" + broadcastWrites.mkString(",") + ")," else ""
    val BXBarR = if (inst.depth > 1) s"${innerMap("X")}(" + broadcastReads.mkString(",") + ")," else ""

    val dimensions = paddedDims(mem, name).mkString("List[Int](", ",", ")") //dims.zip(padding).map{case (d,p) => s"$d+$p"}.mkString("List[Int](", ",", ")")
    val numBanks = {if (mem.isLUT | mem.isRegFile) dims else inst.nBanks}.map(_.toString).mkString("List[Int](", ",", ")")
    val strides = inst.Ps.map(_.toString).mkString("List[Int](",",",")")
    val bankingMode = "BankedMemory" // TODO: Find correct one

    val initStr = if (init.isDefined) init.get.map(quoteAsScala).map(x => src"${x}.toDouble").mkString("Some(List(",",","))")
      else "None"
    emitGlobalModule(src"""val $mem = Module(new $templateName $dimensions, $depth ${bitWidth(mem.tp.typeArgs.head)}, $numBanks, $strides, $XBarW, $XBarR, $DirectW, $DirectR, $BXBarW $BXBarR $bankingMode, $initStr, ${!spatialConfig.enableAsyncMem && spatialConfig.enableRetiming}, ${fracBits(mem.tp.typeArgs.head)}))""")
  }

  private def ifaceType(mem: Sym[_]): String = {
    mem match {
      case Op(_:FIFONew[_]) => if (mem.instance.depth > 1) "" else ".asInstanceOf[FIFOInterface]"
      case Op(_:LIFONew[_]) => if (mem.instance.depth > 1) "" else ".asInstanceOf[FIFOInterface]"
      case _ => ""
    }
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {

    // SRAMs
    case op: SRAMNew[_,_] => emitMem(lhs, "SRAM", None)
    case op@SRAMBankedRead(sram,bank,ofs,ens) => emitRead(lhs, sram, bank, ofs, ens)
    case op@SRAMBankedWrite(sram,data,bank,ofs,ens) => emitWrite(lhs, sram, data, bank, ofs, ens)

    // Registers
    case RegNew(init) => 
      lhs.optimizedRegType match {
        case None            => emitMem(lhs, "FF", Some(List(init)))
        case Some(AccumAdd) =>
          val FixPtType(s,d,f) = lhs.tp.typeArgs.head
          val opLatency = scala.math.max(1.0, latencyOption("FixAdd", Some(d+f)))
          val cycleLatency = opLatency + latencyOption("RegRead", None) + latencyOption("RegWrite", None)
          val numWriters = lhs.writers.size
          emitGlobalModuleMap(src"${lhs}", src"Module(new FixOpAccum(Accum.Add, $numWriters, ${cycleLatency}, ${opLatency}, $s,$d,$f, ${quoteAsScala(init)}))")
        case Some(AccumMul) =>
          val FixPtType(s,d,f) = lhs.tp.typeArgs.head
          val opLatency = scala.math.max(1.0, latencyOption("FixMul", Some(d+f)))
          val cycleLatency = opLatency + latencyOption("RegRead", None) + latencyOption("RegWrite", None)
          val numWriters = lhs.writers.size
          emitGlobalModuleMap(src"${lhs}", src"Module(new FixOpAccum(Accum.Mul, $numWriters, ${cycleLatency}, ${opLatency}, $s,$d,$f, ${quoteAsScala(init)}))")
        case Some(AccumMin) =>
          val FixPtType(s,d,f) = lhs.tp.typeArgs.head
          val opLatency = scala.math.max(1.0, latencyOption("FixMin", Some(d+f)))
          val cycleLatency = opLatency + latencyOption("RegRead", None) + latencyOption("RegWrite", None)
          val numWriters = lhs.writers.size
          emitGlobalModuleMap(src"${lhs}", src"Module(new FixOpAccum(Accum.Min, $numWriters, ${cycleLatency}, ${opLatency}, $s,$d,$f, ${quoteAsScala(init)}))")
        case Some(AccumMax) =>
          val FixPtType(s,d,f) = lhs.tp.typeArgs.head
          val opLatency = scala.math.max(1.0, latencyOption("FixMax", Some(d+f)))
          val cycleLatency = opLatency + latencyOption("RegRead", None) + latencyOption("RegWrite", None)
          val numWriters = lhs.writers.size
          emitGlobalModuleMap(src"${lhs}", src"Module(new FixOpAccum(Accum.Max, $numWriters, ${cycleLatency}, ${opLatency}, $s,$d,$f, ${quoteAsScala(init)}))")
        case Some(AccumFMA) =>
          val FixPtType(s,d,f) = lhs.tp.typeArgs.head
          val opLatency = scala.math.max(1.0, latencyOption("FixFMA", Some(d+f)))
          val cycleLatency = opLatency + latencyOption("RegRead", None) + latencyOption("RegWrite", None)
          val numWriters = lhs.writers.size
          emitGlobalModuleMap(src"${lhs}", src"Module(new FixFMAAccum($numWriters, ${cycleLatency}, ${opLatency}, $s,$d,$f, ${quoteAsScala(init)}))")
        case Some(AccumUnk) => throw new Exception(s"Cannot emit Reg with specialized reduce of type Unk yet!")
      }
    case RegWrite(reg, data, ens) if (!reg.isArgOut & !reg.isArgIn & !reg.isHostIO) => 
      emitWrite(lhs, reg, Seq(data), Seq(Seq()), Seq(), Seq(ens))
    case RegRead(reg)  if (!reg.isArgOut & !reg.isArgIn & !reg.isHostIO) => 
      emitRead(lhs, reg, Seq(Seq()), Seq(), Seq(Set()))
    case RegAccumOp(reg, data, ens, t, first) => 
      val index = reg.writers.toList.indexOf(lhs)
      val parent = lhs.parent.s.get
      val invisibleEnable = src"""${DL(src"${swap(parent, DatapathEn)} & ${swap(parent, IIDone)}", lhs.fullDelay, true)}"""
      emitt(src"${reg}.io.input1($index) := $data.r")
      emitt(src"${reg}.io.enable($index) := ${and(ens)} && $invisibleEnable")
      emitt(src"${reg}.io.last($index)   := ${DL(src"${swap(parent, SM)}.io.ctrDone", lhs.fullDelay, true)}")
      emitt(src"${reg}.io.reset($index) := false.B//${swap(parent, Resetter)}")
      emitt(src"${reg}.io.first($index) := ${first}")
      emitGlobalWireMap(src"${lhs}", src"Wire(${lhs.tp})")
      emitt(src"${lhs}.r := ${reg}.io.output")
    case RegAccumFMA(reg, data1, data2, ens, first) => 
      val index = reg.writers.toList.indexOf(lhs)
      val parent = lhs.parent.s.get
      val invisibleEnable = src"""${DL(src"${swap(parent, DatapathEn)} & ${swap(parent, IIDone)}", lhs.fullDelay, true)}"""
      emitt(src"${reg}.io.input1($index) := $data1.r")
      emitt(src"${reg}.io.input2($index) := $data2.r")
      emitt(src"${reg}.io.enable($index) := ${and(ens)} && $invisibleEnable")
      emitt(src"${reg}.io.last($index)   := ${DL(src"${swap(parent, SM)}.io.ctrDone", lhs.fullDelay, true)}")
      emitt(src"${reg}.io.reset($index) := false.B//${swap(parent, Resetter)}")
      emitt(src"${reg}.io.first($index) := ${first}")
      emitGlobalWireMap(src"${lhs}", src"Wire(${lhs.tp})")
      emitt(src"${lhs}.r := ${reg}.io.output")
    // Specialized FMA Register

    // RegFiles
    case op@RegFileNew(_, inits) => emitMem(lhs, "ShiftRegFile", inits)
    case RegFileReset(rf, en)    => 
      val parent = lhs.parent
      if (memsWithReset.contains(rf)) throw new Exception(s"Currently only one resetter per RegFile is supported ($rf ${rf.name} has more than 1)")
      else {
        memsWithReset = memsWithReset :+ rf
        val flowEnable = src",${swap(parent,SM)}.io.flow"
        val invisibleEnable = src"""${DL(src"${swap(parent, DatapathEn)} & ${swap(parent, IIDone)}", lhs.fullDelay, true)} & $flowEnable"""
        emitt(src"${rf}.io.reset := ${invisibleEnable} & ${and(en)}")

      }

      // val parent = lhs.parent.s.get
      // val id = resettersOf(rf).map{_._1}.indexOf(lhs)
      // duplicatesOf(rf).indices.foreach{i => emitt(src"${rf}_${i}_manual_reset_$id := $en & ${DL(swap(parent, DatapathEn), enableRetimeMatch(en, lhs), true)} ")}

    case RegFileShiftInVector(rf,data,addr,en,axis)  => emitWrite(lhs,rf,data.elems.map(_.asInstanceOf[Sym[_]]).toSeq,Seq(addr),Seq(),Seq(en), Some(axis))
    case RegFileShiftIn(rf,data,addr,en,axis)        => emitWrite(lhs,rf,Seq(data),Seq(addr),Seq(),Seq(en), Some(axis))
    case RegFileBankedShiftIn(rf,data,addr,en,axis)  => emitWrite(lhs,rf,data,addr,Seq(),en, Some(axis))

    // TODO: Matt are these correct?
    case RegFileVectorRead(rf,addr,ens)       => emitRead(lhs,rf,addr,addr.map{_ => I32(0) },ens)
    case RegFileVectorWrite(rf,data,addr,ens) => emitWrite(lhs,rf,data,addr,addr.map{_ => I32(0) },ens)

    // LineBuffers
    case LineBufferNew(rows, cols) => emitMem(lhs, "LineBuffer", None)
    case LineBufferBankedEnq(lb, data, ens) => emitWrite(lhs,lb,data,Seq(),Seq(),ens)
    case LineBufferBankedRead(lb, bank, ofs, ens) => emitRead(lhs,lb,bank,ofs,ens)
    
    // FIFOs
    case FIFONew(depths) => emitMem(lhs, "FIFO", None)
    case FIFOIsEmpty(fifo,_) => emitt(src"val $lhs = $fifo.io${ifaceType(fifo)}.empty")
    case FIFOIsFull(fifo,_)  => emitt(src"val $lhs = $fifo.io${ifaceType(fifo)}.full")
    case FIFOIsAlmostEmpty(fifo,_) => emitt(src"val $lhs = $fifo.io${ifaceType(fifo)}.almostEmpty")
    case FIFOIsAlmostFull(fifo,_) => emitt(src"val $lhs = $fifo.io${ifaceType(fifo)}.almostFull")
    case op@FIFOPeek(fifo,_) => emitt(src"val $lhs = $fifo.io.output.data(0)")
    case FIFONumel(fifo,_)   => emitt(src"val $lhs = $fifo.io${ifaceType(fifo)}.numel")
    case op@FIFOBankedDeq(fifo, ens) => emitRead(lhs, fifo, Seq.fill(ens.length)(Seq()), Seq(), ens)
    case FIFOBankedEnq(fifo, data, ens) => emitWrite(lhs, fifo, data, Seq.fill(ens.length)(Seq()), Seq(), ens)

    // LIFOs
    case LIFONew(depths) => emitMem(lhs, "LIFO", None)
    case LIFOIsEmpty(fifo,_) => emitt(src"val $lhs = $fifo.io${ifaceType(fifo)}.empty")
    case LIFOIsFull(fifo,_)  => emitt(src"val $lhs = $fifo.io${ifaceType(fifo)}.full")
    case LIFOIsAlmostEmpty(fifo,_) => emitt(src"val $lhs = $fifo.io${ifaceType(fifo)}.almostEmpty")
    case LIFOIsAlmostFull(fifo,_) => emitt(src"val $lhs = $fifo.io${ifaceType(fifo)}.almostFull")
    case op@LIFOPeek(fifo,_) => emitt(src"val $lhs = $fifo.io.output.data(0)")
    case LIFONumel(fifo,_)   => emitt(src"val $lhs = $fifo.io${ifaceType(fifo)}.numel")
    case op@LIFOBankedPop(fifo, ens) => emitRead(lhs, fifo, Seq.fill(ens.length)(Seq()), Seq(), ens)
    case LIFOBankedPush(fifo, data, ens) => emitWrite(lhs, fifo, data, Seq.fill(ens.length)(Seq()), Seq(), ens)
    
    // LUTs
    case op@LUTNew(dims, init) => emitMem(lhs, "LUT", Some(init))
    case op@LUTBankedRead(lut,bank,ofs,ens) => emitRead(lhs,lut,bank,ofs,ens)

    case _ => super.gen(lhs, rhs)
  }

  protected def bufferControlInfo(mem: Sym[_]): List[Sym[_]] = {
    val accesses = mem.accesses.filter(_.port.bufferPort.isDefined)
    if (accesses.nonEmpty) {
      val lca = if (accesses.size == 1) accesses.head.parent else LCA(accesses)
      if (lca.isParallel){ // Assume memory analysis chose buffering based on lockstep of different bodies within this parallel, and just use one
        val releventAccesses = accesses.toList.filter(_.ancestors.contains(lca.children.head)).toSet
        val logickingLca = LCA(releventAccesses)
        val (basePort, numPorts) = if (logickingLca.s.get.isInnerControl) (0,0) else LCAPortMatchup(releventAccesses.toList, logickingLca)
        val info = if (logickingLca.s.get.isInnerControl) List[Sym[_]]() else (basePort to {basePort+numPorts}).map { port => logickingLca.children.toList(port).s.get }
        info.toList        
      } else {
        val (basePort, numPorts) = if (lca.s.get.isInnerControl) (0,0) else LCAPortMatchup(accesses.toList, lca)
        val info = if (lca.s.get.isInnerControl) List[Sym[_]]() else (basePort to {basePort+numPorts}).map { port => lca.children.toList(port).s.get }
        info.toList        
      }
    } else {
      throw new Exception(s"Cannot create a buffer on $mem, which has no accesses")
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