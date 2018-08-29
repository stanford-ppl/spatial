package spatial.codegen.dotgen

import argon._
import spatial.metadata.memory._
import spatial.lang._
import spatial.node._
import spatial.metadata._
import spatial.metadata.control._
import spatial.util.spatialConfig

trait DotGenSpatial extends DotCodegen {

  override def emitEdge(from:Sym[_], to:Sym[_]) = (from.op, to.op) match {
    case (Some(_:SRAMNew[_,_]), Some(_:SRAMBankedWrite[_,_])) => super.emitEdge(to, from)
    case (Some(_:StreamOutNew[_]), Some(_:StreamOutBankedWrite[_])) => super.emitEdge(to, from)
    case (Some(_:StreamInNew[_]), Some(_:FringeDenseLoad[_,_])) => super.emitEdge(to, from)
    case (Some(_:RegNew[_]), Some(_:RegWrite[_])) => super.emitEdge(to, from)
    case (Some(_:DRAMNew[_,_]), Some(_:SetMem[_,_])) => super.emitEdge(to, from)
    case (_, _) if from.isDRAM => 
    case (_, _) => super.emitEdge(from, to)
  }

  override def nodeAttr(lhs:Sym[_]):Map[String,String] = super.nodeAttr(lhs) ++ (lhs.op match {
    case Some(rhs:SRAMNew[_,_]) =>    "color" -> "forestgreen" :: "style" -> "filled" :: "shape" -> "box" :: Nil
    case Some(rhs:RegFileNew[_,_]) => "color" -> "forestgreen" :: "style" -> "filled" :: "shape" -> "box" :: Nil
    case Some(rhs) if lhs.isReg =>    "color" -> "chartreuse2" :: "style" -> "filled" :: "shape" -> "box" :: Nil
    case Some(rhs:FIFONew[_]) =>      "color" -> "gold"        :: "style" -> "filled" :: "shape" -> "box" :: Nil
    case Some(rhs:StreamInNew[_]) =>  "color" -> "gold"        :: "style" -> "filled" :: "shape" -> "box" :: Nil
    case Some(rhs:StreamOutNew[_]) => "color" -> "gold"        :: "style" -> "filled" :: "shape" -> "box" :: Nil
    case _ => Nil
  })

  override def label(lhs:Sym[_]) = lhs.op match {
    case Some(rhs) if lhs.isMem => super.label(lhs) + s"\n${lhs.ctx}"
    case Some(FringeDenseLoad(dram,_,_))   => super.label(lhs) + src"\ndram=${label(dram)}"
    case Some(FringeDenseStore(dram,_,_,_))  => super.label(lhs) + src"\ndram=${label(dram)}"
    case Some(FringeSparseLoad(dram,_,_))  => super.label(lhs) + src"\ndram=${label(dram)}"
    case Some(FringeSparseStore(dram,_,_)) => super.label(lhs) + src"\ndram=${label(dram)}"
    case Some(GetDRAMAddress(dram)) => super.label(lhs) + src"\ndram=${label(dram)}"
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

case class DotFlatGenSpatial(IR: State) extends DotFlatCodegen with DotGenSpatial
case class DotHierarchicalGenSpatial(IR: State) extends DotHierarchicalCodegen with DotGenSpatial
