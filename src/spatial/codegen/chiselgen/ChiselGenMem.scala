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

import utils.math._

trait ChiselGenMem extends ChiselGenCommon {

  private var memsWithReset: List[Sym[_]] = List()

  private def splitAndCreate(lhs: Sym[_], mem: Sym[_], port: String, tp: String, payload: Seq[String]): Unit = {
    val zipThreshold = 150 max payload.map(_.length).sorted.headOption.getOrElse(0) // Max number of characters before deciding to split line into many
    val totalPayload = payload.mkString(src"List[$tp](", ",", ")")
    if (totalPayload.length < zipThreshold) {
      emit(src"""val $port = $totalPayload""")
    }
    else {
      val groupSize = 1 max (payload.length / (totalPayload.length / zipThreshold))
      val groups = payload.grouped(groupSize).toList
      open(src"def create_$port(): List[$tp] = {")
      groups.zipWithIndex.foreach{case (group, i) => 
        val rdPayload = group.mkString(src"List[$tp](",",",")")
        emit(src"""val $port$i = $rdPayload""")
      }
      val all = List.tabulate(groups.size){i => src"$port$i"}.mkString(" ++ ")
      emit(s"$all")
      close("}")
      emit(src"""val $port = create_$port()""")
    }
  }

  private def implicitEnableRead(lhs: Sym[_], mem: Sym[_]): String = {
    if (mem.isFIFOReg && lhs.parent.s.get.isOuterControl) src"~$break && $done" // Don't know why this is the rule, but this is what works
    else               src"""~$break && ${DL(src"$datapathEn & $iiIssue", lhs.fullDelay, true)}"""
  }

  private def implicitEnableWrite(lhs: Sym[_]): String = {
    val flowEnable = src"~$break && $backpressure"
    src"""~$break && ${DL(src"$datapathEn & $iiIssue", lhs.fullDelay, true)} & $flowEnable"""
  }
  private def emitReset(lhs: Sym[_], mem: Sym[_], en: Set[Bit]): Unit = {
      if (memsWithReset.contains(mem)) throw new Exception(s"Currently only one resetter per Mem is supported ($mem ${mem.name} has more than 1)")
      else {
        memsWithReset = memsWithReset :+ mem
        val invisibleEnable = src"""${DL(src"$datapathEn & $iiIssue", lhs.fullDelay, true)}"""
        emit(src"$mem.connectReset($invisibleEnable & ${and(en)})")
      }
  }

  private def emitReadInterface(lhs: Sym[_], mem: Sym[_], ens: Seq[Set[Bit]]): Unit = {
    emit(createWire(quote(lhs), src"new FIFODeqInterface(${bitWidth(mem.tp.typeArgs.head)})"))
    val invisibleEnable = implicitEnableRead(lhs,mem)
    val commonEns = ens.head.collect{case e if ens.forall(_.contains(e)) && !e.isBroadcastAddr => e}
    val enslist = ens.map{e => and(e.filter(!commonEns.contains(_)).filter(!_.isBroadcastAddr))}.map(appendSuffix(lhs.parent.s.get, _))
    splitAndCreate(lhs, mem, src"${lhs}_en", "Bool", enslist)
    emit(src"""$lhs.r := $mem.connectRPort(${lhs.hashCode}, List[UInt](), List[UInt](), $backpressure, ${lhs}_en.map(_ && $lhs.ready && $invisibleEnable && ${and(commonEns.map(appendSuffix(lhs.parent.s.get, _)))}), ${!mem.broadcastsAnyRead}).head""")
    emit(src"$lhs.valid := ~$mem.empty")
    emit(src"$mem.connectAccessActivesIn(${activesMap(lhs)}, ${lhs}.activeIn /*(${or(ens.head.map(appendSuffix(lhs.parent.s.get, _)))})*/)")
    emit(src"$lhs.activeOut := $mem.active(${activesMap(lhs)}).out")
  }

  private def emitRead(lhs: Sym[_], mem: Sym[_], bank: Seq[Seq[Sym[_]]], ofs: Seq[Sym[_]], ens: Seq[Set[Bit]], enString: String = ""): Unit = {
    if (lhs.segmentMapping.values.exists(_>0)) appPropertyStats += HasAccumSegmentation
    
    lhs.tp match {
      case _: Vec[_] => emit(createWire(src"$lhs",src"Vec(${ens.length}, ${mem.tp.typeArgs.head})"))
      case _ => emit(createWire(quote(lhs), src"${mem.tp.typeArgs.head}"))
    }

    val banklist = bank.flatten.map{x => if (x.isBroadcastAddr) "0.U" else {quote(x) + ".r"}}
    splitAndCreate(lhs, mem, src"${lhs}_banks", "UInt", banklist)
    splitAndCreate(lhs, mem, src"${lhs}_ofs", "UInt", ofs.map{x => if (x.isBroadcastAddr) "0.U" else {quote(x) + ".r"}})
    val commonEns = ens.head.collect{case e if ens.forall(_.contains(e)) && !e.isBroadcastAddr => e}
    val enslist = ens.map{e => and(e.filter(!commonEns.contains(_)).filter(!_.isBroadcastAddr))}
    splitAndCreate(lhs, mem, src"${lhs}_en", "Bool", enslist)
    emit(src"""$lhs.toSeq.zip($mem.connectRPort(${lhs.hashCode}, ${lhs}_banks, ${lhs}_ofs, $backpressure, ${lhs}_en.map(_ && ${implicitEnableRead(lhs,mem)} && ${and(commonEns)} ${enString}), ${!mem.broadcastsAnyRead})).foreach{case (a,b) => a := b}""")
  }

  private def emitWrite(lhs: Sym[_], mem: Sym[_], data: Seq[Sym[_]], bank: Seq[Seq[Sym[_]]], ofs: Seq[Sym[_]], ens: Seq[Set[Bit]], shiftAxis: Option[Int] = None): Unit = {
    if (lhs.segmentMapping.values.exists(_>0)) appPropertyStats += HasAccumSegmentation

    val banklist = bank.flatten.map{x => if (x.isBroadcastAddr) "0.U" else {quote(x) + ".r"}}
    splitAndCreate(lhs, mem, src"${lhs}_banks", "UInt", banklist)
    splitAndCreate(lhs, mem, src"${lhs}_ofs", "UInt", ofs.map{x => if (x.isBroadcastAddr) "0.U" else {quote(x) + ".r"}})
    val commonEns = ens.head.collect{case e if ens.forall(_.contains(e)) && !e.isBroadcastAddr => e}
    val enslist = ens.map{e => and(e.filter(!commonEns.contains(_)).filter(!_.isBroadcastAddr))}
    splitAndCreate(lhs, mem, src"${lhs}_en", "Bool", enslist)
    splitAndCreate(lhs, mem, src"${lhs}_data", "UInt", data.map{x => if (x.isBroadcastAddr) "0.U" else {quote(x) + ".r"}})
    emit(src"""$mem.connectWPort(${lhs.hashCode}, ${lhs}_banks, ${lhs}_ofs, ${lhs}_data, ${lhs}_en.map(_ && ${implicitEnableWrite(lhs)} && ${and(commonEns)}))""")

    val enport = if (shiftAxis.isDefined) "shiftEn" else "en"
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
        i % pDims.drop(j).product / pDims.drop(j+1).product
      }
      if (coords.zip(dims).map{case(c:Int,d:Int) => c < d}.reduce{_&&_}) {
        val flatCoord = coords.zipWithIndex.map{ case (b,j) => 
          b * dims.drop(j+1).product
        }.sum
        src"(${quoteAsScala(inits(flatCoord))}).toDouble"
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

    val dualsfx = if (mem.isDualPortedRead) "DualRead" else ""
    val templateName = if (!mem.isNBuffered && name != "LineBuffer") s"$name$dualsfx("
                       else {
                         if (name == "SRAM") appPropertyStats += HasNBufSRAM
                         mem.swappers.zipWithIndex.foreach{case (node, port) => 
                           bufMapping += (node -> {bufMapping.getOrElse(node, List[BufMapping]()) ++ List(BufMapping(mem, port))})
                         }
                         s"NBufMem(${name}${dualsfx}Type, "
                       }
    if (mem.broadcastsAnyRead) appPropertyStats += HasBroadcastRead

    val depth = if (mem.isNBuffered) s"${inst.depth}," else ""

    def innerMap(t: String): String = s"${t}Map"

    val dimensions = paddedDims(mem, name).mkString("List[Int](", ",", ")") //dims.zip(padding).map{case (d,p) => s"$d+$p"}.mkString("List[Int](", ",", ")")
    val numBanks = {if (mem.isLUT | mem.isRegFile) dims else inst.nBanks}.map(_.toString).mkString("List[Int](", ",", ")")
    val blockCycs = {if (mem.isLUT | mem.isRegFile) List.fill(dims.size)(1) else inst.Bs}.map(_.toString).mkString("List[Int](",",",")")
    val neighborhood = {if (mem.isLUT | mem.isRegFile) dims else inst.Ps}.map(_.toString).mkString("List[Int](",",",")")
    val bankingMode = "BankedMemory" // TODO: Find correct one
    val ofsWidth =
      if (mem.isLineBuffer) utils.math.ofsWidth(paddedDims(mem,name).last, Seq(inst.nBanks.last))
      else utils.math.ofsWidth(utils.math.volume(inst.nBanks, inst.Bs, inst.Ps, paddedDims(mem,name)), inst.nBanks)
    val banksWidths = utils.math.banksWidths(inst.nBanks)

    val initStr = if (init.isDefined) expandInits(mem, init.get, name) else "None"
    createMemObject(mem) {
      mem.writers.zipWithIndex.foreach{ case (w, i) => 
        val resids = w.residualGenerators.map(_.map{x => s"$x"}.mkString("List(", ",", ")")).mkString("List(",",",")")
        emit(src"val w$i = Access(${w.hashCode}, ${w.port.muxPort}, ${w.port.muxOfs}, ${w.port.castgroup.mkString("List(",",",")")}, ${w.port.broadcast.mkString("List(",",",")")}, ${w.shiftAxis}, PortInfo(${w.port.bufferPort}, ${1 max w.accessWidth}, ${1 max ofsWidth}, ${banksWidths.map(1 max _).mkString("List(",",",")")}, ${bitWidth(mem.tp.typeArgs.head)}, $resids))")
      }
      if (mem.writers.isEmpty) {emit(src"val w0 = AccessHelper.singular(32)")}
      mem.readers.zipWithIndex.foreach{ case (r, i) =>
        val resids = r.residualGenerators.map(_.map{x => s"$x"}.mkString("List(", ",", ")")).mkString("List(",",",")")
        if (r.port.bufferPort.isEmpty && mem.isNBuffered && !mem.isLineBuffer) throw new Exception(src"Unsure how to handle broadcasted read @ ${r.ctx.content.getOrElse("<?:?:?>")} ($mem port $r)")
        emit(src"val r$i = Access(${r.hashCode}, ${r.port.muxPort}, ${r.port.muxOfs}, ${r.port.castgroup.mkString("List(",",",")")}, ${r.port.broadcast.mkString("List(",",",")")}, ${r.shiftAxis}, PortInfo(${r.port.bufferPort}, ${1 max r.accessWidth}, ${1 max ofsWidth}, ${banksWidths.map(1 max _).mkString("List(",",",")")}, ${bitWidth(mem.tp.typeArgs.head)}, $resids))")
      }
      if (mem.readers.isEmpty) {emit(src"val r0 = AccessHelper.singular(32)")}

      emit(src"""val m = Module(new $templateName 
    $dimensions,
    $depth ${bitWidth(mem.tp.typeArgs.head)}, 
    $numBanks,
    $blockCycs,
    $neighborhood,
    ${List.tabulate(1 max mem.writers.size){i => s"w$i"}.mkString("List(", ",", ")")},
    ${List.tabulate(1 max mem.readers.size){i => s"r$i"}.mkString("List(", ",", ")")},
    $bankingMode, 
    $initStr, 
    ${!spatialConfig.enableAsyncMem && spatialConfig.enableRetiming}, 
    ${fracBits(mem.tp.typeArgs.head)},
    ${1 max (mem.readers.size + mem.writers.size)}, 
    myName = "$mem"
  ))""")
     emit(src"m.io${ifaceType(mem)} <> DontCare")
      if (name == "FIFO" || name == "FIFOReg") {
        mem.writers.zipWithIndex.foreach{case (x,i) => activesMap += (x -> i)}
        mem.readers.zipWithIndex.foreach{case (x,i) => activesMap += (x -> {i + mem.writers.size})}
      }
      if (mem.resetters.isEmpty && !mem.isBreaker) emit(src"m.io.reset := false.B")
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
      emit(src"$reg.connectAccessActivesIn(${activesMap(lhs)}, ${and(ens)})")
    case FIFORegDeq(reg, ens) => 
      emitRead(lhs, reg, Seq(Seq()), Seq(), Seq(ens))
      emit(src"$reg.connectAccessActivesIn(${activesMap(lhs)}, ${and(ens)})")

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
            emit(src"val m = Module(new FixOpAccum(Accum.Add, $numWriters, $cycleLatency, $opLatency, $s,$d,$f, ${quoteAsScala(init)}))")
            if (lhs.resetters.isEmpty) emit(src"m.io.reset := false.B")
          }
        case Some(AccumMul) =>
          val FixPtType(s,d,f) = lhs.tp.typeArgs.head
          val opLatency = scala.math.max(1.0, latencyOption("FixMul", Some(d+f)))
          val cycleLatency = opLatency + latencyOption("RegRead", None) + latencyOption("RegWrite", None)
          val numWriters = lhs.writers.size
          createMemObject(lhs) {
            emit(src"val m = Module(new FixOpAccum(Accum.Mul, $numWriters, $cycleLatency, $opLatency, $s,$d,$f, ${quoteAsScala(init)}))")
            if (lhs.resetters.isEmpty) emit(src"m.io.reset := false.B")
          }
        case Some(AccumMin) =>
          val FixPtType(s,d,f) = lhs.tp.typeArgs.head
          val opLatency = scala.math.max(1.0, latencyOption("FixMin", Some(d+f)))
          val cycleLatency = opLatency + latencyOption("RegRead", None) + latencyOption("RegWrite", None)
          val numWriters = lhs.writers.size
          createMemObject(lhs) {
            emit(src"val m = Module(new FixOpAccum(Accum.Min, $numWriters, $cycleLatency, $opLatency, $s,$d,$f, ${quoteAsScala(init)}))")
            if (lhs.resetters.isEmpty) emit(src"m.io.reset := false.B")
          }
        case Some(AccumMax) =>
          val FixPtType(s,d,f) = lhs.tp.typeArgs.head
          val opLatency = scala.math.max(1.0, latencyOption("FixMax", Some(d+f)))
          val cycleLatency = opLatency + latencyOption("RegRead", None) + latencyOption("RegWrite", None)
          val numWriters = lhs.writers.size
          createMemObject(lhs) {
            emit(src"val m = Module(new FixOpAccum(Accum.Max, $numWriters, $cycleLatency, $opLatency, $s,$d,$f, ${quoteAsScala(init)}))")
            if (lhs.resetters.isEmpty) emit(src"m.io.reset := false.B")
          }
        case Some(AccumFMA) =>
          val FixPtType(s,d,f) = lhs.tp.typeArgs.head
          val opLatency = scala.math.max(1.0, latencyOption("FixFMA", Some(d+f)))
          val cycleLatency = opLatency + latencyOption("RegRead", None) + latencyOption("RegWrite", None)
          val numWriters = lhs.writers.size
          createMemObject(lhs) {
            emit(src"val m = Module(new FixFMAAccum($numWriters, $cycleLatency, $opLatency, $s,$d,$f, ${quoteAsScala(init)}))")
            if (lhs.resetters.isEmpty) emit(src"m.io.reset := false.B")
          }
        case Some(AccumUnk) => throw new Exception(s"Cannot emit Reg with specialized reduce of type Unk yet!")
      }
    case RegWrite(reg, data, ens) if !reg.isArgOut & !reg.isArgIn & !reg.isHostIO =>
      emitWrite(lhs, reg, Seq(data), Seq(Seq()), Seq(), Seq(ens))
    case RegRead(reg)  if !reg.isArgOut & !reg.isArgIn & !reg.isHostIO =>
      emitRead(lhs, reg, Seq(Seq()), Seq(), Seq(Set()))
    case RegAccumOp(reg, data, ens, t, first) => 
      val index = reg.writers.toList.indexOf(lhs)
      val invisibleEnable = implicitEnableRead(lhs,reg)
      emit(src"$reg.connectWPort($index, $data.r, ${and(ens)} && $invisibleEnable, ${DL(src"$ctrDone", lhs.fullDelay, true)}, $first)")
      emit(createWire(quote(lhs),remap(lhs.tp)))
      emit(src"$lhs.r := $reg.output")
    case RegAccumFMA(reg, data1, data2, ens, first) => 
      val index = reg.writers.toList.indexOf(lhs)
      val invisibleEnable = implicitEnableRead(lhs,reg)
      emit(src"$reg.connectWPort($index, $data1.r, $data2.r, ${and(ens)} && $invisibleEnable, ${DL(src"$ctrDone", lhs.fullDelay, true)}, $first)")
      emit(createWire(quote(lhs),remap(lhs.tp)))
      emit(src"$lhs.r := $reg.output")
    case RegReset(reg, en)    => emitReset(lhs, reg, en)

    // RegFiles
    case op@RegFileNew(_, inits) => emitMem(lhs, inits)
    case RegFileReset(rf, en)    => emitReset(lhs, rf, en)
    case RegFileShiftInVector(rf,data,addr,en,axis)  => emitWrite(lhs,rf,data.elems.map(_.asInstanceOf[Sym[_]]),Seq(addr),Seq(),Seq(en), Some(axis))
    case RegFileShiftIn(rf,data,addr,en,axis)        => emitWrite(lhs,rf,Seq(data),Seq(addr),Seq(),Seq(en), Some(axis))
    case RegFileBankedShiftIn(rf,data,addr,en,axis)  => emitWrite(lhs,rf,data,addr,Seq(),en, Some(axis))

    case RegFileVectorRead(rf,addr,ens)       => emitRead(lhs,rf,addr,addr.map{_ => I32(0) },ens)
    case RegFileVectorWrite(rf,data,addr,ens) => emitWrite(lhs,rf,data,addr,addr.map{_ => I32(0) },ens)

    // LineBuffers
    case LineBufferNew(rows, cols, stride) => emitMem(lhs, None)
    case LineBufferBankedEnq(lb, data, row, ens) => emitWrite(lhs,lb,data,Seq(row),Seq(),ens)
    case LineBufferBankedRead(lb, bank, ofs, ens) => emitRead(lhs,lb,bank,ofs,ens)
    
    // FIFOs
    case FIFONew(depths) => emitMem(lhs, None)
    case FIFOIsEmpty(fifo,_) => emit(src"val $lhs = $fifo.empty")
    case FIFOIsFull(fifo,_)  => emit(src"val $lhs = $fifo.full")
    case FIFOIsAlmostEmpty(fifo,_) => emit(src"val $lhs = $fifo.almostEmpty")
    case FIFOIsAlmostFull(fifo,_) => emit(src"val $lhs = $fifo.almostFull")
    case op@FIFOPeek(fifo,ens) => 
      emitRead(lhs, fifo, Seq(Seq()), Seq(), Seq(Set(Bit(false))))
      emit(src"$fifo.connectAccessActivesIn(${activesMap(lhs)}, false.B)")
      // emit(createWire(quote(lhs),remap(lhs.tp)));emit(src"$lhs.r := $fifo.rPort(0).output.head")
    case FIFONumel(fifo,_)   => emit(createWire(quote(lhs),remap(lhs.tp)));emit(src"$lhs.r := $fifo.numel")
    case op@FIFOBankedDeq(fifo, ens) => 
      emitRead(lhs, fifo, Seq.fill(ens.length)(Seq()), Seq(), ens)
      emit(src"$fifo.connectAccessActivesIn(${activesMap(lhs)}, (${or(ens.map{e => "(" + and(e) + ")"})}))")
    case op@FIFOBankedPriorityDeq(fifo, ens) =>
      emitRead(lhs, fifo, Seq.fill(ens.length)(Seq()), Seq(), ens, src"&& !$fifo.empty")
      emit(src"$fifo.connectAccessActivesIn(${activesMap(lhs)}, (${or(ens.map{e => "(" + and(e) + ")"})}))")
    case op@FIFODeqInterface(fifo, ens) =>
      emitReadInterface(lhs, fifo, Seq(ens))
    case FIFOBankedEnq(fifo, data, ens) =>
      emitWrite(lhs, fifo, data, Seq.fill(ens.length)(Seq()), Seq(), ens)
      emit(src"$fifo.connectAccessActivesIn(${activesMap(lhs)}, (${or(ens.map{e => "(" + and(e) + ")"})}))")

    // LIFOs
    case LIFONew(depths) => emitMem(lhs, None)
    case LIFOIsEmpty(fifo,_) => emit(src"val $lhs = $fifo.empty")
    case LIFOIsFull(fifo,_)  => emit(src"val $lhs = $fifo.full")
    case LIFOIsAlmostEmpty(fifo,_) => emit(src"val $lhs = $fifo.almostEmpty")
    case LIFOIsAlmostFull(fifo,_) => emit(src"val $lhs = $fifo.almostFull")
    case op@LIFOPeek(fifo,ens) => 
      emitRead(lhs, fifo, Seq(Seq()), Seq(), Seq(Set(Bit(false))))
    case LIFONumel(fifo,_)   => emit(createWire(quote(lhs),remap(lhs.tp)));emit(src"$lhs.r := $fifo.numel")
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
      val invEn = implicitEnableWrite(lhs)
      val en = ens.map{ and(_) + src"&& $invEn" }.mkString(src"List[Bool](", ",", ")")
      emit(src"""$merge.connectMergeEnq($way, $d, $en)""")
    case MergeBufferBankedDeq(merge, ens) => 
      val readerIdx = merge.readers.collect { case r@Op(MergeBufferBankedDeq(_, _)) => r }.toSeq.indexOf(lhs)
      emit(src"""val $lhs = Wire(Vec(${ens.length}, ${merge.tp.typeArgs.head}))""")
      val invEn = implicitEnableRead(lhs,merge)
      val en = ens.map{ and(_) + src"&& $invEn" }.mkString(src"List[Bool](", ",", ")")
      emit(src"$lhs.toSeq.zip($merge.connectMergeDeq($readerIdx, $en)).foreach{case (l,r) => l.r := r}")
    case MergeBufferBound(merge, way, data, ens) =>
      val invEn = implicitEnableWrite(lhs)
      emit(src"$merge.connectMergeBound($way, $data.r, ${and(ens)} & $invEn)")
    case MergeBufferInit(merge, data, ens) =>
      val invEn = implicitEnableWrite(lhs)
      emit(src"$merge.connectMergeInit($data.r, ${and(ens)} & $invEn)")

    case _ => super.gen(lhs, rhs)
  }


}
