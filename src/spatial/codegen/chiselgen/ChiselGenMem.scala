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

  private var memsWithReset: List[Sym[_]] = List()

  // def and(owner: Sym[_], ens: Set[Bit]): String = {
  //   and(ens.map{x => if (controllerStack.head.isOuterControl) appendSuffix(owner,x) else quote(x)})
  // }
  private def zipAndConnect(lhs: Sym[_], mem: Sym[_], port: String, tp: String, payload: Seq[String], suffix: String): Unit = {
    val zipThreshold = 150 max payload.map(_.size).sorted.headOption.getOrElse(0) // Max number of characters before deciding to split line into many
    val totalPayload = payload.mkString(src"List[$tp](", ",", ")")
    if (totalPayload.length < zipThreshold) {
      val rdPayload = totalPayload + suffix
      emit(src"""${local(lhs)}_port.$port.zip($rdPayload).foreach{case (left, right) => left.r := right}""")
    }
    else {
      val groupSize = 1 max (payload.length / (totalPayload.length / zipThreshold)).toInt
      val groups = payload.grouped(groupSize).toList
      groups.zipWithIndex.foreach{case (group, i) => 
        val rdPayload = group.mkString(src"List[$tp](",",",")") + suffix
        emit(src"""${local(lhs)}_port.$port.drop(${i*groupSize}).take(${group.size}).zip($rdPayload).foreach{case (left, right) => left.r := right}""")
      }
    }
  }

  private def invisibleEnableRead(lhs: Sym[_], mem: Sym[_]): String = {
    if (mem.isFIFOReg) src"~$break && $done"
    else               src"""~$break && ${DL(src"$datapathEn & $iiDone", lhs.fullDelay, true)}"""
  }

  private def invisibleEnableWrite(lhs: Sym[_]): String = {
    val flowEnable = src"~$break && $backpressure"
    src"""~$break && ${DL(src"$datapathEn & $iiDone", lhs.fullDelay, true)} & $flowEnable"""
  }
  private def emitReset(lhs: Sym[_], mem: Sym[_], en: Set[Bit]): Unit = {
      if (memsWithReset.contains(mem)) throw new Exception(s"Currently only one resetter per Mem is supported ($mem ${mem.name} has more than 1)")
      else {
        memsWithReset = memsWithReset :+ mem
        val invisibleEnable = src"""${DL(src"$datapathEn & $iiDone", lhs.fullDelay, true)}"""
        emit(src"${memIO(mem)}.reset := ${invisibleEnable} & ${and(en)}")
      }
  }

  private def emitRead(lhs: Sym[_], mem: Sym[_], bank: Seq[Seq[Sym[_]]], ofs: Seq[Sym[_]], ens: Seq[Set[Bit]]): Unit = {
    if (lhs.segmentMapping.values.exists(_>0)) appPropertyStats += HasAccumSegmentation
    val name = if (mem.isInstanceOf[RegNew[_]]) "FF" else ""
    val rPar = lhs.accessWidth
    val width = bitWidth(mem.tp.typeArgs.head)
    // if (lhs.parent.stage == -1) emitControlSignals(parent)
    val invisibleEnable = invisibleEnableRead(lhs,mem)
    val flowEnable = src",$backpressure"
    val ofsWidth = if (!mem.isLineBuffer) Math.max(1, Math.ceil(scala.math.log(paddedDims(mem,name).product/mem.instance.nBanks.product)/scala.math.log(2)).toInt)
                     else Math.max(1, Math.ceil(scala.math.log(paddedDims(mem,name).last/mem.instance.nBanks.last)/scala.math.log(2)).toInt)
    val banksWidths = if (mem.isRegFile || mem.isLUT) paddedDims(mem,name).map{x => Math.ceil(scala.math.log(x)/scala.math.log(2)).toInt}
                      else mem.instance.nBanks.map{x => Math.ceil(scala.math.log(x)/scala.math.log(2)).toInt}

    val isBroadcast = lhs.port.bufferPort.isEmpty & mem.instance.depth > 1
    val bufferPort = lhs.port.bufferPort.getOrElse(-1)
    val muxPort = lhs.port.muxPort
    val muxOfs = lhs.port.muxOfs

    lhs.tp match {
      case _: Vec[_] => emit(createWire(src"${lhs}",src"Vec(${ens.length}, ${mem.tp.typeArgs.head})")) 
      case _ => emit(createWire(quote(lhs), src"${mem.tp.typeArgs.head}"))
    }

    val ignoreCastInfo = !mem.broadcastsAnyRead
    val castgrps     = lhs.port.castgroup.mkString("List(",",",")")
    val broadcastids = lhs.port.broadcast.mkString("List(",",",")")
    if (lhs.isDirectlyBanked & !isBroadcast) {
      emit(createWire(src"""${local(lhs)}_port""", src"""new R_Direct(${ens.length}, $ofsWidth, ${bank.flatten.map(_.trace.toInt).mkString("List(",",",")")}.grouped(${bank.head.length}).toList)"""))
      emit(src"${local(lhs)}_port := DontCare")
      emit(src"""${lhs}.toSeq.zip(${mem}.connectDirectRPort(${local(lhs)}_port, $bufferPort, ($muxPort, $muxOfs),$castgrps, $broadcastids, $ignoreCastInfo $flowEnable)).foreach{case (left, right) => left.r := right}""")
    } else if (isBroadcast) {
      val banklist = bank.flatten.filter(!_.isBroadcastAddr).map(quote(_) + ".r")
      emit(createWire(src"""${local(lhs)}_port""", src"""new R_XBar(${ens.length}, $ofsWidth, ${banksWidths.mkString("List(",",",")")})"""))
      emit(src"${local(lhs)}_port := DontCare")
      zipAndConnect(lhs, mem, "banks", "UInt", banklist, ".map(_.rd)")
      emit(src"""${lhs}.toSeq.zip(${mem}.connectBroadcastRPort(${local(lhs)}_port, ($muxPort, $muxOfs),$castgrps, $broadcastids, $ignoreCastInfo $flowEnable)).foreach{case (left, right) => left.r := right}""")
    } else {
      val banklist = bank.flatten.filter(!_.isBroadcastAddr).map(quote(_) + ".r")
      emit(createWire(src"""${local(lhs)}_port""", src"""new R_XBar(${ens.length}, $ofsWidth, ${banksWidths.mkString("List(",",",")")})"""))
      emit(src"${local(lhs)}_port := DontCare")
      zipAndConnect(lhs, mem, "banks", "UInt", banklist, ".map(_.rd)")
      emit(src"""${lhs}.toSeq.zip(${mem}.connectXBarRPort(${local(lhs)}_port, $bufferPort, ($muxPort, $muxOfs),$castgrps, $broadcastids, $ignoreCastInfo $flowEnable)).foreach{case (left, right) => left.r := right}""")
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
    // if (lhs.parent.stage == -1) emitControlSignals(parent)
    val flowEnable = src"backpressure"
    val invisibleEnable = invisibleEnableWrite(lhs)
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
      emit(createWire(src"""${local(lhs)}_port""", src"""new W_Direct(${data.length}, $ofsWidth, ${bank.flatten.map(_.trace.toInt).mkString("List(",",",")")}.grouped(${bank.head.length}).toList, $width)"""))
      emit(src"${local(lhs)}_port := DontCare")
      emit(src"""${mem}.connectDirectWPort(${local(lhs)}_port, $bufferPort, (${muxPort}, $muxOfs))""")
    } else if (isBroadcast) {
      val banklist = bank.flatten.map(quote(_) + ".r")
      emit(createWire(src"""${local(lhs)}_port""", src"""new W_XBar(${data.length}, $ofsWidth, ${banksWidths.mkString("List(",",",")")}, $width)"""))
      emit(src"${local(lhs)}_port := DontCare")
      zipAndConnect(lhs, mem, "banks", "UInt", banklist, ".map(_.rd)")
      emit(src"""${mem}.connectBroadcastWPort(${local(lhs)}_port, ($muxPort, $muxOfs))""")        
    } else {
      val banklist = bank.flatten.map(quote(_) + ".r")
      emit(createWire(src"""${local(lhs)}_port""", src"""new W_XBar(${data.length}, $ofsWidth, ${banksWidths.mkString("List(",",",")")}, $width)"""))
      emit(src"${local(lhs)}_port := DontCare")
      zipAndConnect(lhs, mem, "banks", "UInt", banklist, ".map(_.rd)")
      emit(src"""${mem}.connectXBarWPort(${local(lhs)}_port, $bufferPort, (${muxPort}, $muxOfs))""")
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

  private def expandInits(mem: Sym[_], inits: Seq[Sym[_]], name: String): String = {
    val dims = if (name == "FF" | name == "FIFOReg") List(1) else mem.constDims
    val padding = if (name == "FF" | name == "FIFOReg") List(0) else mem.getPadding.getOrElse(Seq.fill(dims.length)(0))
    val pDims = dims.zip(padding).map{case (d:Int,p:Int) => d+p}
    val paddedInits = Seq.tabulate(pDims.product){i => 
      val coords = pDims.zipWithIndex.map{ case (b,j) =>
        i % (pDims.drop(j).product) / pDims.drop(j+1).product
      }
      if (coords.zip(dims).map{case(c:Int,d:Int) => c < d}.reduce{_&&_}) {
        val flatCoord = coords.zipWithIndex.map{ case (b,j) => 
          b * dims.drop(j+1).product
        }.sum
        src"${quoteAsScala(inits(flatCoord))}.toDouble"
      }
      else 
        "0.toDouble"
    }
    paddedInits.mkString("Some(List(",",","))")
  }

  private def emitMem(mem: Sym[_], init: Option[Seq[Sym[_]]]): Unit = {
    val name = mem.memName
    if (mem.dephasedAccesses.nonEmpty) appPropertyStats += HasDephasedAccess
    val inst = mem.instance
    val dims = if (name == "FF") List(1) else mem.constDims
    // val padding = if (name == "FF") List(0) else mem.getPadding.getOrElse(Seq.fill(dims.length)(0))
    val broadcastWrites = mem.writers.filter{w => w.port.bufferPort.isEmpty & mem.isNBuffered}
                                     .map{w => src"(${w.port.muxPort},${w.port.muxOfs},0) -> (${w.accessWidth}, ${w.shiftAxis})"}.toList
    val broadcastReads = mem.readers.filter{w => w.port.bufferPort.isEmpty & mem.isNBuffered}
                                    .map{w => src"(${w.port.muxPort},${w.port.muxOfs},0) -> (${w.accessWidth}, ${w.shiftAxis})"}.toList

    if (!mem.isNBuffered && name == "LineBuffer") throw new Exception(s"Cannot create non-buffered line buffer!  Make sure $mem has readers and writers bound by a pipelined LCA, or else turn it into an SRAM")

    val templateName = if (!mem.isNBuffered && name != "LineBuffer") s"${name}("
                       else {
                         if (name == "SRAM") appPropertyStats += HasNBufSRAM
                         mem.swappers.zipWithIndex.foreach{case (node, port) => 
                           bufMapping += (node -> {bufMapping.getOrElse(node, List[BufMapping]()) ++ List(BufMapping(mem, port))})
                         }
                         s"NBufMem(${name}Type, "
                       }
    if (mem.broadcastsAnyRead) appPropertyStats += HasBroadcastRead

    val depth = if (mem.isNBuffered) s"${inst.depth}," else ""
    val nbuf = if (mem.isNBuffered) "NBuf" else ""
    def outerMap(t: String): String = if (mem.isNBuffered) s"NBuf${t}Map" else s"${t}Map"
    def innerMap(t: String): String = s"${t}Map"
    // Strip the lanes if necessary and return ports for constructor
    def recomputePortInfo(muxOfs: Int, castgroup: Seq[Int], broadcast: Seq[Int]): Seq[(Int,Int,Int)] = {
      castgroup.zip(broadcast).zipWithIndex.collect{case ((cg,bid),i) if (bid == 0) => 
        (muxOfs+i,cg,castgroup.filter(_==cg).size)
      }.toSeq
    }
    // Create mapping for (bufferPort -> (muxPort -> width)) for XBar accesses
    val XBarW = s"${outerMap("X")}(" + mem.writers.filter(_.port.bufferPort.isDefined | !mem.isNBuffered) // Filter out broadcasters
                      .filterNot(_.isDirectlyBanked)              // Filter out statically banked
                      .filter(_.accessWidth > 0)
                      .groupBy(_.port.bufferPort.getOrElse(-1))      // Group by port
                      .map{case(bufp, writes) => 
                        if (mem.isNBuffered) src"$bufp -> ${innerMap("X")}(" + writes.map{w => src"(${w.port.muxPort},${w.port.muxOfs},0) -> (${w.accessWidth}, ${w.shiftAxis})"}.mkString(",") + ")"
                        else writes.map{w => src"(${w.port.muxPort},${w.port.muxOfs},0) -> (${w.accessWidth}, ${w.shiftAxis})"}.mkString(",")
                      }.mkString(",") + ")"
    val XBarR = s"${outerMap("X")}(" + mem.readers.filter(_.port.bufferPort.isDefined | !mem.isNBuffered) // Filter out broadcasters
                      .filterNot(_.isDirectlyBanked)              // Filter out statically banked
                      .filter(_.accessWidth > 0)
                      .groupBy(_.port.bufferPort.getOrElse(-1))      // Group by port
                      .map{case(bufp, reads) => 
                        if (mem.broadcastsAnyRead) { // Make generated code more readable if this memory doesn't actually broadcast anywhere
                          if (mem.isNBuffered) src"$bufp -> ${innerMap("X")}(" + reads.map{r => 
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
                          if (mem.isNBuffered) src"$bufp -> ${innerMap("X")}(" + reads.map{r => src"(${r.port.muxPort},${r.port.muxOfs},0) -> (${r.accessWidth}, ${r.shiftAxis})"}.mkString(",") + ")"
                          else reads.map{r => src"(${r.port.muxPort},${r.port.muxOfs},0) -> (${r.accessWidth}, ${r.shiftAxis})"}.mkString(",")
                        }
                      }.mkString(",") + ")"
    val DirectW = s"${outerMap("D")}(" + mem.writers.filter(_.port.bufferPort.isDefined | !mem.isNBuffered) // Filter out broadcasters
                      .filter(_.isDirectlyBanked)              // Filter out dynamically banked
                      .filter(_.accessWidth > 0)
                      .groupBy(_.port.bufferPort.getOrElse(-1))      // Group by port
                      .map{case(bufp, writes) => 
                        if (mem.isNBuffered) src"$bufp -> ${innerMap("D")}(" + writes.map{w => src"(${w.port.muxPort},${w.port.muxOfs},0) -> " + s"(${w.banks.map(_.map(_.trace.toInt).mkString("Banks(",",",")"))}, ${w.shiftAxis})".replace("Vector","List")}.mkString(",") + ")"
                        else writes.map{w => src"(${w.port.muxPort},${w.port.muxOfs},0) -> " + s"(${w.banks.map(_.map(_.trace.toInt).mkString("Banks(",",",")"))}, ${w.shiftAxis})".replace("Vector","List")}.mkString(",")
                      }.mkString(",") + ")"
    val DirectR = s"${outerMap("D")}(" + mem.readers.filter(_.port.bufferPort.isDefined | !mem.isNBuffered) // Filter out broadcasters
                      .filter(_.isDirectlyBanked)              // Filter out dynamically banked
                      .filter(_.accessWidth > 0)
                      .groupBy(_.port.bufferPort.getOrElse(-1))      // Group by port
                      .map{case(bufp, reads) => 
                        if (mem.broadcastsAnyRead) { // Make generated code more readable if this memory doesn't actually broadcast anywhere
                          if (mem.isNBuffered) src"$bufp -> ${innerMap("D")}(" + reads.map{r => 
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
                          if (mem.isNBuffered) src"$bufp -> ${innerMap("D")}(" + reads.map{r => src"(${r.port.muxPort},${r.port.muxOfs},0) -> " + s"(${r.banks.map(_.map(_.trace.toInt).mkString("Banks(",",",")"))}, ${r.shiftAxis})".replace("Vector","List")}.mkString(",") + ")"
                          else reads.map{r => src"(${r.port.muxPort},${r.port.muxOfs},0) -> " + s"(${r.banks.map(_.map(_.trace.toInt).mkString("Banks(",",",")"))}, ${r.shiftAxis})".replace("Vector","List")}.mkString(",")
                        }
                      }.mkString(",") + ")"
    val BXBarW = if (mem.isNBuffered) s"${innerMap("X")}(" + broadcastWrites.mkString(",") + ")," else ""
    val BXBarR = if (mem.isNBuffered) s"${innerMap("X")}(" + broadcastReads.mkString(",") + ")," else ""

    val dimensions = paddedDims(mem, name).mkString("List[Int](", ",", ")") //dims.zip(padding).map{case (d,p) => s"$d+$p"}.mkString("List[Int](", ",", ")")
    val numBanks = {if (mem.isLUT | mem.isRegFile) dims else inst.nBanks}.map(_.toString).mkString("List[Int](", ",", ")")
    val strides = inst.Ps.map(_.toString).mkString("List[Int](",",",")")
    val bankingMode = "BankedMemory" // TODO: Find correct one

    val initStr = if (init.isDefined) expandInits(mem, init.get, name) else "None"
    createMemObject(mem) {
      emit(src"""val m = Module(new $templateName 
    $dimensions, 
    $depth ${bitWidth(mem.tp.typeArgs.head)}, 
    $numBanks, 
    $strides, 
    $XBarW, 
    $XBarR, 
    $DirectW, 
    $DirectR, 
    $BXBarW 
    $BXBarR 
    $bankingMode, 
    $initStr, 
    ${!spatialConfig.enableAsyncMem && spatialConfig.enableRetiming}, 
    ${fracBits(mem.tp.typeArgs.head)},
    ${1 max (mem.readers.size + mem.writers.size)}, 
    myName = "$mem"
  ))""")
     emit(src"m.io${ifaceType(mem)} <> DontCare")
      if (name == "FIFO" || name == "FIFOReg") {
        mem.writers.zipWithIndex.foreach{case (x,i) => activesMap += (x -> i); emit(src"// enqActive_$x = ${activesMap(x)}")}
        mem.readers.zipWithIndex.foreach{case (x,i) => activesMap += (x -> {i + mem.writers.size}); emit(src"// deqActive_$x = ${activesMap(x)}")}
      }
      if (mem.resetters.isEmpty && !mem.isBreaker) emit(src"m.io.reset := false.B")
    }
  }

  private def ifaceType(mem: Sym[_]): String = {
    mem match {
      case Op(_:FIFONew[_]) => if (mem.instance.depth > 1) "" else ".asInstanceOf[FIFOInterface]"
      case Op(_:FIFORegNew[_]) => if (mem.instance.depth > 1) "" else ".asInstanceOf[FIFOInterface]"
      case Op(_:LIFONew[_]) => if (mem.instance.depth > 1) "" else ".asInstanceOf[FIFOInterface]"
      case _ => ""
    }
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {

    // SRAMs
    case op: SRAMNew[_,_] => emitMem(lhs, None)
    case op@SRAMBankedRead(sram,bank,ofs,ens) => emitRead(lhs, sram, bank, ofs, ens)
    case op@SRAMBankedWrite(sram,data,bank,ofs,ens) => emitWrite(lhs, sram, data, bank, ofs, ens)

    // FIFORegs
    case FIFORegNew(init) => emitMem(lhs, Some(List(init)))
    case FIFORegEnq(reg, data, ens) => 
      emitWrite(lhs, reg, Seq(data), Seq(Seq()), Seq(), Seq(ens))
      emit(src"${memIO(reg)}.accessActivesIn(${activesMap(lhs)}) := ${and(ens)}") 
    case FIFORegDeq(reg, ens) => 
      emitRead(lhs, reg, Seq(Seq()), Seq(), Seq(ens))
      emit(src"${memIO(reg)}.accessActivesIn(${activesMap(lhs)}) := ${and(ens)}") 

    // Registers
    case RegNew(init) => 
      lhs.optimizedRegType match {
        case None            => emitMem(lhs, Some(List(init)))
        case Some(AccumAdd) =>
          val FixPtType(s,d,f) = lhs.tp.typeArgs.head
          val opLatency = scala.math.max(1.0, latencyOption("FixAdd", Some(d+f)))
          val cycleLatency = opLatency + latencyOption("RegRead", None) + latencyOption("RegWrite", None)
          val numWriters = lhs.writers.size
          createMemObject(lhs) {
            emit(src"val m = Module(new FixOpAccum(Accum.Add, $numWriters, ${cycleLatency}, ${opLatency}, $s,$d,$f, ${quoteAsScala(init)}))")  
            if (lhs.resetters.isEmpty) emit(src"m.io.input.foreach(_.reset := false.B)")
          }
        case Some(AccumMul) =>
          val FixPtType(s,d,f) = lhs.tp.typeArgs.head
          val opLatency = scala.math.max(1.0, latencyOption("FixMul", Some(d+f)))
          val cycleLatency = opLatency + latencyOption("RegRead", None) + latencyOption("RegWrite", None)
          val numWriters = lhs.writers.size
          createMemObject(lhs) {
            emit(src"val m = Module(new FixOpAccum(Accum.Mul, $numWriters, ${cycleLatency}, ${opLatency}, $s,$d,$f, ${quoteAsScala(init)}))")  
            if (lhs.resetters.isEmpty) emit(src"m.io.input.foreach(_.reset := false.B)")
          }
        case Some(AccumMin) =>
          val FixPtType(s,d,f) = lhs.tp.typeArgs.head
          val opLatency = scala.math.max(1.0, latencyOption("FixMin", Some(d+f)))
          val cycleLatency = opLatency + latencyOption("RegRead", None) + latencyOption("RegWrite", None)
          val numWriters = lhs.writers.size
          createMemObject(lhs) {
            emit(src"val m = Module(new FixOpAccum(Accum.Min, $numWriters, ${cycleLatency}, ${opLatency}, $s,$d,$f, ${quoteAsScala(init)}))")  
            if (lhs.resetters.isEmpty) emit(src"m.io.input.foreach(_.reset := false.B)")
          }
        case Some(AccumMax) =>
          val FixPtType(s,d,f) = lhs.tp.typeArgs.head
          val opLatency = scala.math.max(1.0, latencyOption("FixMax", Some(d+f)))
          val cycleLatency = opLatency + latencyOption("RegRead", None) + latencyOption("RegWrite", None)
          val numWriters = lhs.writers.size
          createMemObject(lhs) {
            emit(src"val m = Module(new FixOpAccum(Accum.Max, $numWriters, ${cycleLatency}, ${opLatency}, $s,$d,$f, ${quoteAsScala(init)}))")  
            if (lhs.resetters.isEmpty) emit(src"m.io.input.foreach(_.reset := false.B)")
          }
        case Some(AccumFMA) =>
          val FixPtType(s,d,f) = lhs.tp.typeArgs.head
          val opLatency = scala.math.max(1.0, latencyOption("FixFMA", Some(d+f)))
          val cycleLatency = opLatency + latencyOption("RegRead", None) + latencyOption("RegWrite", None)
          val numWriters = lhs.writers.size
          createMemObject(lhs) {
            emit(src"val m = Module(new FixFMAAccum($numWriters, ${cycleLatency}, ${opLatency}, $s,$d,$f, ${quoteAsScala(init)}))")  
            if (lhs.resetters.isEmpty) emit(src"m.io.input.foreach(_.reset := false.B)")
          }
        case Some(AccumUnk) => throw new Exception(s"Cannot emit Reg with specialized reduce of type Unk yet!")
      }
    case RegWrite(reg, data, ens) if (!reg.isArgOut & !reg.isArgIn & !reg.isHostIO) => 
      emitWrite(lhs, reg, Seq(data), Seq(Seq()), Seq(), Seq(ens))
    case RegRead(reg)  if (!reg.isArgOut & !reg.isArgIn & !reg.isHostIO) => 
      emitRead(lhs, reg, Seq(Seq()), Seq(), Seq(Set()))
    case RegAccumOp(reg, data, ens, t, first) => 
      val index = reg.writers.toList.indexOf(lhs)
      val invisibleEnable = invisibleEnableRead(lhs,reg)
      emit(src"${memIO(reg)}.connectXBarWPort($index, $data.r, ${and(ens)} && $invisibleEnable, ${DL(src"$ctrDone", lhs.fullDelay, true)}, ${first})")
      emit(createWire(quote(lhs),remap(lhs.tp)))
      emit(src"${lhs}.r := ${memIO(reg)}.output")
    case RegAccumFMA(reg, data1, data2, ens, first) => 
      val index = reg.writers.toList.indexOf(lhs)
      val invisibleEnable = invisibleEnableRead(lhs,reg)
      emit(src"${memIO(reg)}.connectXBarWPort($index, $data1.r, $data2.r, ${and(ens)} && $invisibleEnable, ${DL(src"$ctrDone", lhs.fullDelay, true)}, ${first})")
      emit(createWire(quote(lhs),remap(lhs.tp)))
      emit(src"${lhs}.r := ${memIO(reg)}.output")
    case RegReset(reg, en)    => emitReset(lhs, reg, en)

    // RegFiles
    case op@RegFileNew(_, inits) => emitMem(lhs, inits)
    case RegFileReset(rf, en)    => emitReset(lhs, rf, en)
    case RegFileShiftInVector(rf,data,addr,en,axis)  => emitWrite(lhs,rf,data.elems.map(_.asInstanceOf[Sym[_]]).toSeq,Seq(addr),Seq(),Seq(en), Some(axis))
    case RegFileShiftIn(rf,data,addr,en,axis)        => emitWrite(lhs,rf,Seq(data),Seq(addr),Seq(),Seq(en), Some(axis))
    case RegFileBankedShiftIn(rf,data,addr,en,axis)  => emitWrite(lhs,rf,data,addr,Seq(),en, Some(axis))

    // TODO: Matt are these correct?
    case RegFileVectorRead(rf,addr,ens)       => emitRead(lhs,rf,addr,addr.map{_ => I32(0) },ens)
    case RegFileVectorWrite(rf,data,addr,ens) => emitWrite(lhs,rf,data,addr,addr.map{_ => I32(0) },ens)

    // LineBuffers
    case LineBufferNew(rows, cols, stride) => emitMem(lhs, None)
    case LineBufferBankedEnq(lb, data, row, ens) => emitWrite(lhs,lb,data,Seq(row),Seq(),ens)
    case LineBufferBankedRead(lb, bank, ofs, ens) => emitRead(lhs,lb,bank,ofs,ens)
    
    // FIFOs
    case FIFONew(depths) => emitMem(lhs, None)
    case FIFOIsEmpty(fifo,_) => emit(src"val $lhs = ${memIO(fifo)}.empty")
    case FIFOIsFull(fifo,_)  => emit(src"val $lhs = ${memIO(fifo)}.full")
    case FIFOIsAlmostEmpty(fifo,_) => emit(src"val $lhs = ${memIO(fifo)}.almostEmpty")
    case FIFOIsAlmostFull(fifo,_) => emit(src"val $lhs = ${memIO(fifo)}.almostFull")
    case op@FIFOPeek(fifo,_) => 
      emit(src"${memIO(fifo)}.accessActivesIn(${activesMap(lhs)}) := false.B") 
      emit(createWire(quote(lhs),remap(lhs.tp)));emit(src"$lhs.r := ${memIO(fifo)}.output.data(0)")
    case FIFONumel(fifo,_)   => emit(createWire(quote(lhs),remap(lhs.tp)));emit(src"$lhs.r := ${memIO(fifo)}.numel")
    case op@FIFOBankedDeq(fifo, ens) => 
      emitRead(lhs, fifo, Seq.fill(ens.length)(Seq()), Seq(), ens)
      emit(src"${memIO(fifo)}.accessActivesIn(${activesMap(lhs)}) := (${or(ens.map{e => "(" + and(e) + ")"})})") 
    case FIFOBankedEnq(fifo, data, ens) => 
      emitWrite(lhs, fifo, data, Seq.fill(ens.length)(Seq()), Seq(), ens)
      emit(src"${memIO(fifo)}.accessActivesIn(${activesMap(lhs)}) := (${or(ens.map{e => "(" + and(e) + ")"})})") 

    // LIFOs
    case LIFONew(depths) => emitMem(lhs, None)
    case LIFOIsEmpty(fifo,_) => emit(src"val $lhs = ${memIO(fifo)}.empty")
    case LIFOIsFull(fifo,_)  => emit(src"val $lhs = ${memIO(fifo)}.full")
    case LIFOIsAlmostEmpty(fifo,_) => emit(src"val $lhs = ${memIO(fifo)}.almostEmpty")
    case LIFOIsAlmostFull(fifo,_) => emit(src"val $lhs = ${memIO(fifo)}.almostFull")
    case op@LIFOPeek(fifo,_) => emit(createWire(quote(lhs),remap(lhs.tp)));emit(src"$lhs.r := ${memIO(fifo)}.output.data(0)")
    case LIFONumel(fifo,_)   => emit(createWire(quote(lhs),remap(lhs.tp)));emit(src"$lhs.r := ${memIO(fifo)}.numel")
    case op@LIFOBankedPop(fifo, ens) => emitRead(lhs, fifo, Seq.fill(ens.length)(Seq()), Seq(), ens)
    case LIFOBankedPush(fifo, data, ens) => emitWrite(lhs, fifo, data, Seq.fill(ens.length)(Seq()), Seq(), ens)
    
    // LUTs
    case op@LUTNew(dims, init) => emitMem(lhs, Some(init))
    case op@LUTBankedRead(lut,bank,ofs,ens) => emitRead(lhs,lut,bank,ofs,ens)

    // MergeBuffer
    case MergeBufferNew(ways, par) =>
      val readers = lhs.readers.collect { case r@Op(MergeBufferBankedDeq(_, _)) => r }.size
      createMemObject(lhs){
        emit(src"""val m = Module(new MergeBuffer(${ways.toInt}, ${par.toInt}, ${bitWidth(lhs.tp.typeArgs.head)}, $readers)); m.io <> DontCare""")
      }
    case MergeBufferBankedEnq(merge, way, data, ens) =>
      val d = data.map{ quote(_) + ".r" }.mkString(src"List[UInt](", ",", ")")
      emit(src"""$merge.io.in_data($way).zip($d).foreach{case (l, r) => l := r }""")
      val invEn = invisibleEnableWrite(lhs)
      val en = ens.map{ and(_) + src"&& $invEn" }.mkString(src"List[UInt](", ",", ")")
      emit(src"""$merge.io.in_wen($way).zip($en).foreach{case (l, r) => l := r }""")
    case MergeBufferBankedDeq(merge, ens) => 
      val readerIdx = merge.readers.collect { case r@Op(MergeBufferBankedDeq(_, _)) => r }.toSeq.indexOf(lhs)
      emit(src"""val $lhs = Wire(Vec(${ens.length}, ${merge.tp.typeArgs.head}))""")
      emit(src"""$lhs.toSeq.zip($merge.io.out_data).foreach{case (l, r) => l.r := r }""")
      val invEn = invisibleEnableRead(lhs,merge)
      val en = ens.map{ and(_) + src"&& $invEn" }.mkString(src"List[UInt](", ",", ")")
      emit(src"""$merge.io.out_ren($readerIdx).zip($en).foreach{case (l, r) => l := r }""")
    case MergeBufferBound(merge, way, data, ens) =>
      val invEn = invisibleEnableWrite(lhs)
      emit(src"$merge.io.inBound_wen($way) := ${and(ens)} & $invEn")
      emit(src"$merge.io.inBound_data($way) := ${data}.r")
    case MergeBufferInit(merge, data, ens) =>
      val invEn = invisibleEnableWrite(lhs)
      emit(src"$merge.io.initMerge_wen := ${and(ens)} & $invEn")
      emit(src"$merge.io.initMerge_data := ${data}.r")

    case _ => super.gen(lhs, rhs)
  }


}
