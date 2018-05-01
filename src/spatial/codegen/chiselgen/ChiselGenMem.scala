package spatial.codegen.chiselgen

import argon._
import argon.codegen.Codegen
import spatial.lang._
import spatial.node._
import spatial.internal.{spatialConfig => cfg}
import spatial.data._
import spatial.util._

trait ChiselGenMem extends ChiselGenCommon {

  private var nbufs: List[Sym[_]] = List()

  def emitRead(lhs: Sym[_], mem: Sym[_], bank: Seq[Seq[Sym[_]]], ofs: Seq[Sym[_]], ens: Seq[Set[Bit]]): Unit = {
    val rPar = accessWidth(lhs)
    val width = bitWidth(mem.tp.typeArgs.head)
    val parent = lhs.parent.s.get //readersOf(mem).find{_.node == lhs}.get.ctrlNode
    val invisibleEnable = src"""${swap(parent, DatapathEn)} & ~${swap(parent, Inhibitor)}"""
    val ofsWidth = 1 max (Math.ceil(scala.math.log((constDimsOf(mem).product/mem.instance.nBanks.product))/scala.math.log(2))).toInt
    val banksWidths = mem.instance.nBanks.map{x => Math.ceil(scala.math.log(x)/scala.math.log(2)).toInt}
    val bufferPort = lhs.ports.values.head.bufferPort.getOrElse(0)
    val muxPort = lhs.ports.values.head.muxPort

    lhs.tp match {
      case _: Vec[_] => emitGlobalWireMap(src"""${lhs}""", src"""Wire(Vec(${ens.length}, ${mem.tp.typeArgs.head}))""") 
      case _ => emitGlobalWireMap(src"""${lhs}""", src"""Wire(${mem.tp.typeArgs.head})""") 
    }

    ens.zipWithIndex.foreach{case (e, i) => 
      if (ens(i).isEmpty) emit(src"""${swap(src"${lhs}_$i", Blank)}.en := ${invisibleEnable}""")
      else emit(src"""${swap(src"${lhs}_$i", Blank)}.en := ${DL(invisibleEnable, enableRetimeMatch(e.head, lhs), true)} & ${e.map(quote).mkString(" & ")}""")
      if (!ofs.isEmpty) emit(src"""${swap(src"${lhs}_$i", Blank)}.ofs := ${ofs(i)}.r""")
      if (lhs.isDirectlyBanked) {
        emitGlobalWireMap(src"""${lhs}_$i""", s"Wire(new R_Direct($ofsWidth, ${bank(i).map(_.toInt)}))") 
        emit(src"""${lhs}($i).r := ${mem}.connectDirectRPort(${swap(src"${lhs}_$i", Blank)}, $bufferPort, $muxPort, $i)""")
      } else {
        emitGlobalWireMap(src"""${lhs}_$i""", s"Wire(new R_XBar($ofsWidth, ${banksWidths.mkString("List(",",",")")}))") 
        bank(i).zipWithIndex.foreach{case (b,j) => emit(src"""${swap(src"${lhs}_$i", Blank)}.banks($j) := ${b}.r""")}
        lhs.tp match {
          case _: Vec[_] => emit(src"""${lhs}($i).r := ${mem}.connectXBarRPort(${swap(src"${lhs}_$i", Blank)}, $bufferPort, $muxPort, $i)""")
          case _ => emit(src"""${lhs}.r := ${mem}.connectXBarRPort(${swap(src"${lhs}_$i", Blank)}, $bufferPort, $muxPort, $i)""")
        }
      }
    }
    
  }

  def emitWrite(lhs: Sym[_], mem: Sym[_], data: Seq[Sym[_]], bank: Seq[Seq[Sym[_]]], ofs: Seq[Sym[_]], ens: Seq[Set[Bit]]): Unit = {
    val wPar = ens.length
    val width = bitWidth(mem.tp.typeArgs.head)
    val parent = lhs.parent.s.get
    val invisibleEnable = src"""${swap(parent, DatapathEn)} & ~${swap(parent, Inhibitor)}"""
    val ofsWidth = 1 max (Math.ceil(scala.math.log((constDimsOf(mem).product/mem.instance.nBanks.product))/scala.math.log(2))).toInt
    val banksWidths = mem.instance.nBanks.map{x => Math.ceil(scala.math.log(x)/scala.math.log(2)).toInt}
    val isBroadcast = !lhs.ports.values.head.bufferPort.isDefined & mem.instance.depth > 1
    val bufferPort = lhs.ports.values.head.bufferPort.getOrElse(0)
    val muxPort = lhs.ports.values.head.muxPort

    data.zipWithIndex.foreach{case (d, i) => 
      if (ens(i).isEmpty) emit(src"""${swap(src"${lhs}_$i", Blank)}.en := ${invisibleEnable}""")
      else emit(src"""${swap(src"${lhs}_$i", Blank)}.en := ${DL(invisibleEnable, enableRetimeMatch(ens(i).head, lhs), true)} & ${ens(i).map(quote).mkString(" & ")}""")
      if (ofs.nonEmpty) emit(src"""${swap(src"${lhs}_$i", Blank)}.ofs := ${ofs(i)}.r""")
      emit(src"""${swap(src"${lhs}_$i", Blank)}.data := ${d}.r""")
      if (lhs.isDirectlyBanked && !isBroadcast) {
        emitGlobalWireMap(src"""${lhs}_$i""", s"Wire(new W_Direct($ofsWidth, ${bank(i).map(_.toInt)}, $width))") 
        emit(src"""${mem}.connectDirectWPort(${swap(src"${lhs}_$i", Blank)}, $bufferPort, $muxPort, $i)""")
      } else if (isBroadcast & mem.instance.depth > 1) {
        emitGlobalWireMap(src"""${lhs}_$i""", s"Wire(new W_XBar($ofsWidth, ${banksWidths.mkString("List(",",",")")}, $width))") 
        bank(i).zipWithIndex.foreach{case (b,j) => emit(src"""${swap(src"${lhs}_$i", Blank)}.banks($j) := ${b}.r""")}
        emit(src"""${mem}.connectBroadcastPort(${swap(src"${lhs}_$i", Blank)}, $muxPort, $i)""")        
      } else {
        emitGlobalWireMap(src"""${lhs}_$i""", s"Wire(new W_XBar($ofsWidth, ${banksWidths.mkString("List(",",",")")}, $width))") 
        bank(i).zipWithIndex.foreach{case (b,j) => emit(src"""${swap(src"${lhs}_$i", Blank)}.banks($j) := ${b}.r""")}
        emit(src"""${mem}.connectXBarWPort(${swap(src"${lhs}_$i", Blank)}, $bufferPort, $muxPort, $i)""")
      }
    }
  }

  def emitMem(mem: Sym[_], name: String, init: Option[Seq[Sym[_]]]): Unit = {
    val inst = mem.instance
    val dims = constDimsOf(mem)
    val broadcasts = mem.writers.filter{w => w.ports.values.head.bufferPort.isEmpty & inst.depth > 1}.zipWithIndex.map{case (a,i) => src"$i -> ${accessWidth(a)}"}.toList

    val templateName = if (inst.depth == 1) s"${name}("
                       else {appPropertyStats += HasNBufSRAM; nbufs = nbufs :+ mem; s"NBufMem(${name}Type, "}

    val depth = if (inst.depth > 1) s"${inst.depth}," else ""
    val nbuf = if (inst.depth > 1) "NBuf" else ""
    def outerMap(t: String): String = if (inst.depth > 1) s"NBuf${t}Map" else if (false) s"Shift${t}Map" else s"${t}Map"
    def innerMap(t: String): String = if (false) s"Shift${t}Map" else s"${t}Map"
    // Create mapping for (bufferPort -> (muxPort -> width)) for XBar accesses
    val XBarW = s"${outerMap("X")}(" + writersOf(mem).filter(portsOf(_).values.head.bufferPort.isDefined | inst.depth == 1) // Filter out broadcasters
                              .filter(!_.isDirectlyBanked)              // Filter out statically banked
                              .groupBy(_.ports.values.head.bufferPort.getOrElse(0))      // Group by port
                              .map{case(bufp, writes) => 
                                if (inst.depth > 1) src"$bufp -> ${innerMap("X")}(" + writes.map{w => src"${portsOf(w).values.head.muxPort} -> ${accessWidth(w)}"}.mkString(",") + ")"
                                else writes.map{w => src"${portsOf(w).values.head.muxPort} -> ${accessWidth(w)}"}.mkString(",")
                              }.mkString(",") + ")"
    val XBarR = s"${outerMap("X")}(" + readersOf(mem)
                              .filter(!_.isDirectlyBanked)              // Filter out statically banked
                              .groupBy(_.ports.values.head.bufferPort.getOrElse(0))      // Group by port
                              .map{case(bufp, reads) => 
                                if (inst.depth > 1) src"$bufp -> ${innerMap("X")}(" + reads.map{r => src"${portsOf(r).values.head.muxPort} -> ${accessWidth(r)}"}.mkString(",") + ")"
                                else reads.map{r => src"${portsOf(r).values.head.muxPort} -> ${accessWidth(r)}"}.mkString(",")
                              }.mkString(",") + ")"
    val DirectW = s"${outerMap("D")}(" + writersOf(mem).filter(portsOf(_).values.head.bufferPort.isDefined | inst.depth == 1) // Filter out broadcasters
                              .filter(_.isDirectlyBanked)              // Filter out dynamically banked
                              .groupBy(_.ports.values.head.bufferPort.getOrElse(0))      // Group by port
                              .map{case(bufp, writes) => 
                                if (inst.depth > 1) src"$bufp -> ${innerMap("D")}(" + writes.map{w => src"${portsOf(w).values.head.muxPort} -> " + s"${w.banks.map(_.map(_.toInt))}".replace("Vector","Banks")}.mkString(",") + ")"
                                else writes.map{w => src"${portsOf(w).values.head.muxPort} -> " + s"${w.banks.map(_.map(_.toInt))}".replace("Vector","Banks")}.mkString(",")
                              }.mkString(",") + ")"
    val DirectR = s"${outerMap("D")}(" + readersOf(mem)
                              .filter(_.isDirectlyBanked)              // Filter out dynamically banked
                              .groupBy(_.ports.values.head.bufferPort.getOrElse(0))      // Group by port
                              .map{case(bufp, reads) => 
                                if (inst.depth > 1) src"$bufp -> ${innerMap("D")}(" + reads.map{w => src"${portsOf(w).values.head.muxPort} -> " + s"${w.banks.map(_.map(_.toInt))}".replace("Vector","Banks")}.mkString(",") + ")"
                                else reads.map{w => src"${portsOf(w).values.head.muxPort} -> " + s"${w.banks.map(_.map(_.toInt))}".replace("Vector","Banks")}.mkString(",")
                              }.mkString(",") + ")"
    val bPar = if (inst.depth > 1) s"${innerMap("X")}(" + broadcasts.mkString(",") + ")," else ""

    val dimensions = dims.map(_.toString).mkString("List[Int](", ",", ")")
    val numBanks = inst.nBanks.map(_.toString).mkString("List[Int](", ",", ")")
    val strides = numBanks // TODO: What to do with strides
    val bankingMode = "BankedMemory" // TODO: Find correct one

    emitGlobalModule(src"""val $mem = Module(new $templateName $dimensions, $depth ${bitWidth(mem.tp.typeArgs.head)}, $numBanks, $strides, $XBarW, $XBarR, $DirectW, $DirectR, $bPar $bankingMode, $init, ${!cfg.enableAsyncMem && cfg.enableRetiming}, ${fracBits(mem.tp.typeArgs.head)}))""")
  }



  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {

    // SRAMs
    case op: SRAMNew[_,_] => emitMem(lhs, "SRAM", None)
    case op@SRAMBankedRead(sram,bank,ofs,ens) => emitRead(lhs, sram, bank, ofs, ens)
    case op@SRAMBankedWrite(sram,data,bank,ofs,ens) => emitWrite(lhs, sram, data, bank, ofs, ens)

    // Registers
    case RegNew(init) => emitMem(lhs, "FF", None)
    case RegWrite(reg, data, ens) if (!reg.isArgOut & !reg.isArgIn) => 
      emitWrite(lhs, reg, Seq(data), Seq(Seq()), Seq(), Seq(ens))
    case RegRead(reg)  if (!reg.isArgOut & !reg.isArgIn) => 
      emitRead(lhs, reg, Seq(Seq()), Seq(), Seq(Set())) 

    // FIFOs
    case FIFONew(depths) => emitMem(lhs, "FIFO", None)
    case FIFOIsEmpty(fifo,_) => emit(src"val $lhs = $fifo.io.empty")
    case FIFOIsFull(fifo,_)  => emit(src"val $lhs = $fifo.io.full")
    case FIFOIsAlmostEmpty(fifo,_) => emit(src"val $lhs = $fifo.io.almostEmpty")
    case FIFOIsAlmostFull(fifo,_) => emit(src"val $lhs = $fifo.io.almostFull")
    case op@FIFOPeek(fifo,_) => emit(src"val $lhs = $fifo.io.output.data(0)")
    case FIFONumel(fifo,_)   => emit(src"val $lhs = $fifo.io.numel")
    case op@FIFOBankedDeq(fifo, ens) => emitRead(lhs, fifo, Seq.fill(ens.length)(Seq()), Seq(), ens)
    case FIFOBankedEnq(fifo, data, ens) => emitWrite(lhs, fifo, data, Seq.fill(ens.length)(Seq()), Seq(), ens)

    case _ => super.gen(lhs, rhs)
  }

  protected def bufferControlInfo(mem: Sym[_]): List[Sym[_]] = {
    val accesses = mem.accesses
    val ports = accesses.filter{a => a.ports.values.head.bufferPort.isDefined}.map{a => a.ports.values.head.bufferPort.get}.toList

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

    if (!specialLB) {
      val siblings = accesses.reduce(LCA(_,_).s.get).children.toList
      val portMatchup = accesses.map{a => siblings.indexOf(siblings.filter{ s => a.ancestors.contains(s) }.head)}
      val basePort = portMatchup.min
      val numPorts = ports.max

      val info = (0 to numPorts).map { port => siblings(basePort + port).s.get }
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
    enterAccel()

    inGenn(out, "BufferControlCxns", ext) {
      nbufs.foreach{ mem => 
        val info = bufferControlInfo(mem)
        info.zipWithIndex.foreach{ case (node, port) => 
          emitt(src"""${mem}.connectStageCtrl(${DL(swap(quote(node), Done), 1, true)}, ${swap(quote(node), BaseEn)}, ${port})""")
        }
      }
    }

    exitAccel()
    super.emitFooter()
  }

}