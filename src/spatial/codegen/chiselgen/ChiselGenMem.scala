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

  def emitBankedLoad(lhs: Sym[_], mem: Sym[_], bank: Seq[Seq[Sym[_]]], ofs: Seq[Sym[_]], ens: Seq[Set[Bit]]): Unit = {
    val rPar = accessWidth(lhs)
    val width = bitWidth(mem.tp.typeArgs.head)
    val parent = lhs.parent.s.get //readersOf(mem).find{_.node == lhs}.get.ctrlNode
    val invisibleEnable = src"""${swap(parent, DatapathEn)} & ~${swap(parent, Inhibitor)}"""
    val ofsWidth = 1 max (Math.ceil(scala.math.log((constDimsOf(mem).product/memInfo(mem).nBanks.product))/scala.math.log(2))).toInt
    val banksWidths = memInfo(mem).nBanks.map{x => Math.ceil(scala.math.log(x)/scala.math.log(2)).toInt}

    emitGlobalWireMap(src"""${lhs}""", src"""Wire(Vec(${ofs.length}, ${lhs.tp.typeArgs.head}))""") 
    ofs.zipWithIndex.foreach{case (o, i) => 
      if (ens(i).isEmpty) emit(src"""${swap(src"${lhs}_$i", Blank)}.en := ${invisibleEnable}""")
      else emit(src"""${swap(src"${lhs}_$i", Blank)}.en := ${DL(invisibleEnable, enableRetimeMatch(ens(i).head, lhs), true)} & ${ens(i).mkString(" & ")}""")
      emit(src"""${swap(src"${lhs}_$i", Blank)}.ofs := ${o}.r""")
      if (lhs.isDirectlyBanked) {
        emitGlobalWireMap(src"""${lhs}_$i""", s"Wire(new R_Direct($ofsWidth, ${bank(i).map(_.toInt)}))") 
        emit(src"""${lhs}($i).r := ${mem}.connectDirectRPort(${swap(src"${lhs}_$i", Blank)}, ${portsOf(lhs).values.head.muxPort}, $i)""")
      } else {
        emitGlobalWireMap(src"""${lhs}_$i""", s"Wire(new R_XBar($ofsWidth, ${banksWidths.mkString("List(",",",")")}))") 
        bank(i).zipWithIndex.foreach{case (b,j) => emit(src"""${swap(src"${lhs}_$i", Blank)}.banks($j) := ${b}.r""")}
        emit(src"""${lhs}($i).r := ${mem}.connectXBarRPort(${swap(src"${lhs}_$i", Blank)}, ${portsOf(lhs).values.head.muxPort}, $i)""")
      }
    }
    
  }

  def emitBankedStore(lhs: Sym[_], mem: Sym[_], data: Seq[Sym[_]], bank: Seq[Seq[Sym[_]]], ofs: Seq[Sym[_]], ens: Seq[Set[Bit]]): Unit = {
    val wPar = ens.length
    val width = bitWidth(mem.tp.typeArgs.head)
    val parent = lhs.parent.s.get
    val invisibleEnable = src"""${swap(parent, DatapathEn)} & ~${swap(parent, Inhibitor)}"""
    val ofsWidth = 1 max (Math.ceil(scala.math.log((constDimsOf(mem).product/memInfo(mem).nBanks.product))/scala.math.log(2))).toInt
    val banksWidths = memInfo(mem).nBanks.map{x => Math.ceil(scala.math.log(x)/scala.math.log(2)).toInt}

    ofs.zipWithIndex.foreach{case (o, i) => 
      if (ens(i).isEmpty) emit(src"""${swap(src"${lhs}_$i", Blank)}.en := ${invisibleEnable}""")
      else emit(src"""${swap(src"${lhs}_$i", Blank)}.en := ${DL(invisibleEnable, enableRetimeMatch(ens(i).head, lhs), true)} & ${ens(i).mkString(" & ")}""")
      emit(src"""${swap(src"${lhs}_$i", Blank)}.ofs := ${o}.r""")
      emit(src"""${swap(src"${lhs}_$i", Blank)}.data := ${data}.r""")
      if (lhs.isDirectlyBanked) {
        emitGlobalWireMap(src"""${lhs}_$i""", s"Wire(new W_Direct($ofsWidth, ${bank(i).map(_.toInt)}, $width))") 
        emit(src"""${mem}.connectDirectWPort(${swap(src"${lhs}_$i", Blank)}, ${portsOf(lhs).values.head.muxPort}, $i)""")
      } else {
        emitGlobalWireMap(src"""${lhs}_$i""", s"Wire(new W_XBar($ofsWidth, ${banksWidths.mkString("List(",",",")")}, $width))") 
        bank(i).zipWithIndex.foreach{case (b,j) => emit(src"""${swap(src"${lhs}_$i", Blank)}.banks($j) := ${b}.r""")}
        emit(src"""${mem}.connectXBarWPort(${swap(src"${lhs}_$i", Blank)}, ${portsOf(lhs).values.head.muxPort}, $i)""")
      }
    }
    // emit(s"""// Assemble W_Info vector""")
    // emitGlobalWireMap(src"""${lhs}_wVec""", s"Wire(Vec(${wPar}, new W_Info(32, ${List.fill(bank.head.length)(32)}, $width)))")
    // ofs.zipWithIndex.foreach{case (o,i) => 
    //   emit(src"""${swap(lhs, WVec)}($i).en := ${DL(invisibleEnable, enableRetimeMatch(ens(i), lhs), true)} & ${ens(i)}""")
    //   emit(src"""${swap(lhs, WVec)}($i).ofs := ${o}.r""")
    //   emit(src"""${swap(lhs, WVec)}($i).data := ${data(i)}.r""")
    //   bank(i).zipWithIndex.foreach{case (b,j) => 
    //     emit(src"""${swap(lhs, WVec)}($i).banks($j) := ${b}.r""")
    //   }
    // }
    // emit(src"""${mem}.connectWPort(${swap(lhs, WVec)}, ${portsOf(lhs, mem).head._2.mkString("List(", ",", ")")})""")
  }

  def emitBankedInitMem(mem: Sym[_], init: Option[Seq[Sym[_]]]): Unit = {
    val inst = memInfo(mem)
    val dims = constDimsOf(mem)
    val broadcasts = writersOf(mem).filter{w => !portsOf(w).values.head.bufferPort.isDefined}.map(accessWidth(_)).toList

    val templateName = if (inst.depth == 1) "SRAM"
                       else if (broadcasts.length > 0) {appPropertyStats += HasNBufSRAM; nbufs = nbufs :+ mem; "NBufSRAM"}
                       else {appPropertyStats += HasNBufSRAM; nbufs = nbufs :+ mem;"NBufSRAMnoBcast"}

    val depth = if (inst.depth > 1) s"${inst.depth}," else ""
    // Create mapping for (bufferPort -> (muxPort -> width)) for XBar accesses
    val XBarWBuilder = writersOf(mem).filter(portsOf(_).values.head.bufferPort.isDefined) // Filter out broadcasters
                              .filter(!_.isDirectlyBanked)              // Filter out statically banked
                              .groupBy(portsOf(_).values.head.bufferPort.get)      // Group by port
                              .map{case(bufp, writes) => 
                                if (inst.depth > 1) src"$bufp -> " else "" + 
                                "scala.collection.mutable.HashMap(" + writes.map{w => src"${portsOf(w).values.head.muxPort} -> ${accessWidth(w)}"}.mkString(",") + ")"
                              }
    val XBarRBuilder = readersOf(mem).filter(portsOf(_).values.head.bufferPort.isDefined) // Filter out broadcasters
                              .filter(!_.isDirectlyBanked)              // Filter out statically banked
                              .groupBy(portsOf(_).values.head.bufferPort.get)      // Group by port
                              .map{case(bufp, reads) => 
                                if (inst.depth > 1) src"$bufp -> " else "" + 
                                "scala.collection.mutable.HashMap(" + reads.map{r => src"${portsOf(r).values.head.muxPort} -> ${accessWidth(r)}"}.mkString(",") + ")"
                              }
    val DirectWBuilder = writersOf(mem).filter(portsOf(_).values.head.bufferPort.isDefined) // Filter out broadcasters
                              .filter(_.isDirectlyBanked)              // Filter out dynamically banked
                              .groupBy(portsOf(_).values.head.bufferPort.get)      // Group by port
                              .map{case(bufp, writes) => 
                                if (inst.depth > 1) src"$bufp -> " else "" + 
                                "scala.collection.mutable.HashMap(" + writes.map{w => src"${portsOf(w).values.head.muxPort} -> " + s"${w.banks.map(_.map(_.toInt))}".replace("Vector","List")}.mkString(",") + ")"
                              }
    val DirectRBuilder = readersOf(mem).filter(portsOf(_).values.head.bufferPort.isDefined) // Filter out broadcasters
                              .filter(_.isDirectlyBanked)              // Filter out dynamically banked
                              .groupBy(portsOf(_).values.head.bufferPort.get)      // Group by port
                              .map{case(bufp, writes) => 
                                if (inst.depth > 1) src"$bufp -> " else "" + 
                                "scala.collection.mutable.HashMap(" + writes.map{w => src"${portsOf(w).values.head.muxPort} -> " + s"${w.banks.map(_.map(_.toInt))}".replace("Vector","List")}.mkString(",") + ")"
                              }
    val bPar = if (broadcasts.length > 0) {broadcasts.mkString("List(",",",")") + ","} else ""

    val dimensions = dims.map(_.toString).mkString("List(", ",", ")")
    val numBanks = inst.nBanks.map(_.toString).mkString("List(", ",", ")")
    val strides = numBanks // TODO: What to do with strides
    val bankingMode = "BankedMemory" // TODO: Find correct one

    val XBarW = if (XBarWBuilder.isEmpty) {if (inst.depth == 1) "scala.collection.mutable.HashMap[Int,Int]()" else "scala.collection.mutable.HashMap[Int,scala.collection.mutable.HashMap[Int,Int]]()"} else XBarWBuilder
    val XBarR = if (XBarRBuilder.isEmpty) {if (inst.depth == 1) "scala.collection.mutable.HashMap[Int,Int]()" else "scala.collection.mutable.HashMap[Int,scala.collection.mutable.HashMap[Int,Int]]()"} else XBarRBuilder
    val DirectW = if (DirectWBuilder.isEmpty) {if (inst.depth == 1) "scala.collection.mutable.HashMap[Int,List[List[Int]]]()" else "scala.collection.mutable.HashMap[Int,scala.collection.mutable.HashMap[Int,List[List[Int]]]]()"} else DirectWBuilder
    val DirectR = if (DirectRBuilder.isEmpty) {if (inst.depth == 1) "scala.collection.mutable.HashMap[Int,List[List[Int]]]()" else "scala.collection.mutable.HashMap[Int,scala.collection.mutable.HashMap[Int,List[List[Int]]]]()"} else DirectRBuilder

    emitGlobalModule(src"""val $mem = Module(new $templateName($dimensions, $depth ${bitWidth(mem.tp.typeArgs.head)}, $numBanks, $strides, $XBarW, $XBarR, $DirectW, $DirectR, $bPar $bankingMode, ${!cfg.enableAsyncMem && cfg.enableRetiming}))""")
  }



  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {


    case op: SRAMNew[_,_] => emitBankedInitMem(lhs, None)

    case op@SRAMBankedRead(sram,bank,ofs,ens) => emitBankedLoad(lhs, sram, bank, ofs, ens)

    case op@SRAMBankedWrite(sram,data,bank,ofs,ens) => emitBankedStore(lhs, sram, data, bank, ofs, ens)

    case _ => super.gen(lhs, rhs)
  }

  override def emitFooter(): Unit = {

    super.emitFooter()
  }

}