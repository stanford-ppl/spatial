package spatial.codegen.dotgen

import argon._
import spatial.metadata.memory._
import spatial.metadata.access._
import spatial.lang._
import spatial.node._
import spatial.metadata._
import spatial.metadata.control._
import spatial.util.spatialConfig

trait DotGenSpatial extends DotCodegen {

  override def emitEdge(from:Sym[_], to:Sym[_], fromAlias:String, toAlias:String):Unit = (from.op, to.op) match {
    case (_, _) if from.isMem && to.isWriter && !to.isReader => super.emitEdge(from, to, toAlias, fromAlias)
    case (_, _) if from.isStreamIn && to.isTileTransfer => super.emitEdge(from, to, toAlias, fromAlias)
    case (_, _) if from.isDRAM && to.isTileStore => super.emitEdge(from, to, toAlias, fromAlias)
    case (_, Some(_:SetMem[_,_])) if from.isDRAM => super.emitEdge(from, to, toAlias, fromAlias)
    case (_, _) => super.emitEdge(from, to, fromAlias, toAlias)
  }

  override def nodeAttr(lhs:Sym[_]):Map[String,String] = super.nodeAttr(lhs) ++ (lhs match {
    case lhs:SRAM[_,_]    => "color" -> "forestgreen" :: "style" -> "filled" :: "shape" -> "box" :: Nil
    case lhs:RegFile[_,_] => "color" -> "forestgreen" :: "style" -> "filled" :: "shape" -> "box" :: Nil
    case lhs if lhs.isReg => "color" -> "chartreuse2" :: "style" -> "filled" :: "shape" -> "box" :: Nil
    case lhs:FIFO[_]      => "color" -> "gold"        :: "style" -> "filled" :: "shape" -> "box" :: Nil
    case lhs:StreamIn[_]  => "color" -> "gold"        :: "style" -> "filled" :: "shape" -> "box" :: Nil
    case lhs:StreamOut[_] => "color" -> "gold"        :: "style" -> "filled" :: "shape" -> "box" :: Nil
    case lhs:DRAM[_,_]    => "color" -> "blueviolet"  :: "style" -> "filled" :: "shape" -> "box" :: Nil
    case _ => Nil
  })

  override def label(lhs:Sym[_]) = lhs.op match {
    case None if lhs.isBound => src"${lhs.parent.s.map{ s => s"$s."}.getOrElse("")}${super.label(lhs)}"
    case Some(rhs) if lhs.isMem => super.label(lhs) + src"\n${lhs.ctx}"
    case Some(rhs) if lhs.isControl => super.label(lhs) + src"\n${lhs.ctx}"
    case _ => super.label(lhs)
  }

  override def inputGroups(lhs:Sym[_]):Map[String, Seq[Sym[_]]] = lhs.op match {
    case Some(SRAMBankedWrite(mem, data, bank, ofs, enss)) => 
      super.inputGroups(lhs) + ("data" -> data) + ("bank" -> bank.flatten) + ("ofs" -> ofs) + ("enss" -> enss.flatten)
    case Some(SRAMBankedRead(mem, bank, ofs, enss)) => 
      super.inputGroups(lhs) + ("bank" -> bank.flatten) + ("ofs" -> ofs) + ("enss" -> enss.flatten)
    case _ => super.inputGroups(lhs)
  }

}

case class DotFlatGenSpatial(IR: State) extends DotFlatCodegen with DotGenSpatial {
  override def label(lhs:Sym[_]) = lhs.op match {
    case Some(FringeDenseLoad(dram,_,_))   => super.label(lhs) + src"\ndram=${label(dram)}"
    case Some(FringeDenseStore(dram,_,_,_))  => super.label(lhs) + src"\ndram=${label(dram)}"
    case Some(FringeSparseLoad(dram,_,_))  => super.label(lhs) + src"\ndram=${label(dram)}"
    case Some(FringeSparseStore(dram,_,_)) => super.label(lhs) + src"\ndram=${label(dram)}"
    case Some(GetDRAMAddress(dram)) => super.label(lhs) + src"\ndram=${label(dram)}"
    case _ => super.label(lhs)
  }
  override def emitEdge(from:Sym[_], to:Sym[_], fromAlias:String, toAlias:String):Unit = (from.op, to.op) match {
    case (_, Some(GetDRAMAddress(dram))) if from.isDRAM => 
    case (_, _) => super.emitEdge(from, to, fromAlias, toAlias)
  }
}
case class DotHierarchicalGenSpatial(IR: State) extends DotHierarchicalCodegen with DotGenSpatial {
}
