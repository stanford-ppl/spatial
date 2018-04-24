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
    val ofsWidth = 1 max (Math.ceil(scala.math.log((constDimsOf(mem).product/memInfo(mem).nBanks.product))/scala.math.log(2))).toInt
    val banksWidths = memInfo(mem).nBanks.map{x => Math.ceil(scala.math.log(x)/scala.math.log(2)).toInt}

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
        emit(src"""${lhs}($i).r := ${mem}.connectDirectRPort(${swap(src"${lhs}_$i", Blank)}, ${portsOf(lhs).values.head.muxPort}, $i)""")
      } else {
        emitGlobalWireMap(src"""${lhs}_$i""", s"Wire(new R_XBar($ofsWidth, ${banksWidths.mkString("List(",",",")")}))") 
        bank(i).zipWithIndex.foreach{case (b,j) => emit(src"""${swap(src"${lhs}_$i", Blank)}.banks($j) := ${b}.r""")}
        lhs.tp match {
          case _: Vec[_] => emit(src"""${lhs}($i).r := ${mem}.connectXBarRPort(${swap(src"${lhs}_$i", Blank)}, ${portsOf(lhs).values.head.muxPort}, $i)""")
          case _ => emit(src"""${lhs}.r := ${mem}.connectXBarRPort(${swap(src"${lhs}_$i", Blank)}, ${portsOf(lhs).values.head.muxPort}, $i)""")
        }
      }
    }
    
  }

  def emitWrite(lhs: Sym[_], mem: Sym[_], data: Seq[Sym[_]], bank: Seq[Seq[Sym[_]]], ofs: Seq[Sym[_]], ens: Seq[Set[Bit]]): Unit = {
    val wPar = ens.length
    val width = bitWidth(mem.tp.typeArgs.head)
    val parent = lhs.parent.s.get
    val invisibleEnable = src"""${swap(parent, DatapathEn)} & ~${swap(parent, Inhibitor)}"""
    val ofsWidth = 1 max (Math.ceil(scala.math.log((constDimsOf(mem).product/memInfo(mem).nBanks.product))/scala.math.log(2))).toInt
    val banksWidths = memInfo(mem).nBanks.map{x => Math.ceil(scala.math.log(x)/scala.math.log(2)).toInt}
    val isBroadcast = !portsOf(lhs).values.head.bufferPort.isDefined

    data.zipWithIndex.foreach{case (d, i) => 
      if (ens(i).isEmpty) emit(src"""${swap(src"${lhs}_$i", Blank)}.en := ${invisibleEnable}""")
      else emit(src"""${swap(src"${lhs}_$i", Blank)}.en := ${DL(invisibleEnable, enableRetimeMatch(ens(i).head, lhs), true)} & ${ens(i).map(quote).mkString(" & ")}""")
      if (!ofs.isEmpty) emit(src"""${swap(src"${lhs}_$i", Blank)}.ofs := ${ofs(i)}.r""")
      emit(src"""${swap(src"${lhs}_$i", Blank)}.data := ${d}.r""")
      if (lhs.isDirectlyBanked && !isBroadcast) {
        emitGlobalWireMap(src"""${lhs}_$i""", s"Wire(new W_Direct($ofsWidth, ${bank(i).map(_.toInt)}, $width))") 
        emit(src"""${mem}.connectDirectWPort(${swap(src"${lhs}_$i", Blank)}, ${portsOf(lhs).values.head.muxPort}, $i)""")
      } else if (isBroadcast & memInfo(mem).depth > 1) {
        emitGlobalWireMap(src"""${lhs}_$i""", s"Wire(new W_XBar($ofsWidth, ${banksWidths.mkString("List(",",",")")}, $width))") 
        bank(i).zipWithIndex.foreach{case (b,j) => emit(src"""${swap(src"${lhs}_$i", Blank)}.banks($j) := ${b}.r""")}
        emit(src"""${mem}.connectBroadcastPort(${swap(src"${lhs}_$i", Blank)}, ${portsOf(lhs).values.head.muxPort}, $i)""")        
      } else {
        emitGlobalWireMap(src"""${lhs}_$i""", s"Wire(new W_XBar($ofsWidth, ${banksWidths.mkString("List(",",",")")}, $width))") 
        bank(i).zipWithIndex.foreach{case (b,j) => emit(src"""${swap(src"${lhs}_$i", Blank)}.banks($j) := ${b}.r""")}
        emit(src"""${mem}.connectXBarWPort(${swap(src"${lhs}_$i", Blank)}, ${portsOf(lhs).values.head.muxPort}, $i)""")
      }
    }
  }

  def emitMem(mem: Sym[_], name: String, init: Option[Seq[Sym[_]]]): Unit = {
    val inst = memInfo(mem)
    val dims = constDimsOf(mem)
    val broadcasts = writersOf(mem).filter{w => !portsOf(w).values.head.bufferPort.isDefined & inst.depth > 1}.zipWithIndex.map{case (a,i) => src"$i -> ${accessWidth(a)}"}.toList

    val templateName = if (inst.depth == 1) s"${name}("
                       else if (broadcasts.length > 0) {appPropertyStats += HasNBufSRAM; nbufs = nbufs :+ mem; s"NBufMem(${name}Type, "}

    val depth = if (inst.depth > 1) s"${inst.depth}," else ""
    // Create mapping for (bufferPort -> (muxPort -> width)) for XBar accesses
    val XBarW = "HashMap[Int, " + {if (inst.depth > 1) "HashMap[Int, Int]](" else "Int]("} + writersOf(mem).filter(portsOf(_).values.head.bufferPort.isDefined | inst.depth == 1) // Filter out broadcasters
                              .filter(!_.isDirectlyBanked)              // Filter out statically banked
                              .groupBy(portsOf(_).values.head.bufferPort.getOrElse(0))      // Group by port
                              .map{case(bufp, writes) => 
                                if (inst.depth > 1) src"$bufp -> HashMap(" + writes.map{w => src"${portsOf(w).values.head.muxPort} -> ${accessWidth(w)}"}.mkString(",") + ")"
                                else writes.map{w => src"${portsOf(w).values.head.muxPort} -> ${accessWidth(w)}"}.mkString(",")
                              }.mkString(",") + ")"
    val XBarR = "HashMap[Int, " + {if (inst.depth > 1) "HashMap[Int, Int]](" else "Int]("} + readersOf(mem).filter(portsOf(_).values.head.bufferPort.isDefined | inst.depth == 1) // Filter out broadcasters
                              .filter(!_.isDirectlyBanked)              // Filter out statically banked
                              .groupBy(portsOf(_).values.head.bufferPort.getOrElse(0))      // Group by port
                              .map{case(bufp, reads) => 
                                if (inst.depth > 1) src"$bufp -> HashMap(" + reads.map{r => src"${portsOf(r).values.head.muxPort} -> ${accessWidth(r)}"}.mkString(",") + ")"
                                else reads.map{r => src"${portsOf(r).values.head.muxPort} -> ${accessWidth(r)}"}.mkString(",")
                              }.mkString(",") + ")"
    val DirectW = "HashMap[Int, " + {if (inst.depth > 1) "HashMap[Int, List[List[Int]]]](" else "List[List[Int]]]("} + writersOf(mem).filter(portsOf(_).values.head.bufferPort.isDefined | inst.depth == 1) // Filter out broadcasters
                              .filter(_.isDirectlyBanked)              // Filter out dynamically banked
                              .groupBy(portsOf(_).values.head.bufferPort.getOrElse(0))      // Group by port
                              .map{case(bufp, writes) => 
                                if (inst.depth > 1) src"$bufp -> HashMap(" + writes.map{w => src"${portsOf(w).values.head.muxPort} -> " + s"${w.banks.map(_.map(_.toInt))}".replace("Vector","List")}.mkString(",") + ")"
                                else writes.map{w => src"${portsOf(w).values.head.muxPort} -> " + s"${w.banks.map(_.map(_.toInt))}".replace("Vector","List")}.mkString(",")
                              }.mkString(",") + ")"
    val DirectR = "HashMap[Int, " + {if (inst.depth > 1) "HashMap[Int, List[List[Int]]]](" else "List[List[Int]]]("} + readersOf(mem).filter(portsOf(_).values.head.bufferPort.isDefined | inst.depth == 1) // Filter out broadcasters
                              .filter(_.isDirectlyBanked)              // Filter out dynamically banked
                              .groupBy(portsOf(_).values.head.bufferPort.getOrElse(0))      // Group by port
                              .map{case(bufp, writes) => 
                                if (inst.depth > 1) src"$bufp -> HashMap(" + writes.map{w => src"${portsOf(w).values.head.muxPort} -> " + s"${w.banks.map(_.map(_.toInt))}".replace("Vector","List")}.mkString(",") + ")"
                                else writes.map{w => src"${portsOf(w).values.head.muxPort} -> " + s"${w.banks.map(_.map(_.toInt))}".replace("Vector","List")}.mkString(",")
                              }.mkString(",") + ")"
    val bPar = if (broadcasts.length > 0) "HashMap[Int, Int](" + broadcasts.mkString(",") + ")," else ""

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
    case RegWrite(reg, data, ens) => emitWrite(lhs, reg, Seq(data), Seq(Seq()), Seq(), Seq(ens))
    case RegRead(reg) => emitRead(lhs, reg, Seq(Seq()), Seq(), Seq(Set())) 

    // FIFOs
    case FIFONew(depths) => emitMem(lhs, "FIFO", None)
    case FIFOIsEmpty(fifo,_) => emit(src"val $lhs = $fifo.io.empty")
    case FIFOIsFull(fifo,_)  => emit(src"val $lhs = $fifo.io.full")
    case FIFOIsAlmostEmpty(fifo,_) => emit(src"val $lhs = $fifo.io.almostEmpty")
    case FIFOIsAlmostFull(fifo,_) => emit(src"val $lhs = $fifo.io.almostFull")
    case op@FIFOPeek(fifo,_) => emit(src"val $lhs = $fifo.io.output.data(0)")
    case FIFONumel(fifo,_)   => emit(src"val $lhs = $fifo.io.numel")
    case op@FIFOBankedDeq(fifo, ens) => emitRead(lhs, fifo, Seq(Seq()), Seq(), ens)
    case FIFOBankedEnq(fifo, data, ens) => emitWrite(lhs, fifo, data, Seq(Seq()), Seq(), ens)

    case _ => super.gen(lhs, rhs)
  }

  override def emitFooter(): Unit = {

    super.emitFooter()
  }

}