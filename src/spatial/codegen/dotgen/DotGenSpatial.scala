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

  override def emitEdge(from:Sym[_], to:Sym[_], fromAlias:String, toAlias:String):Unit = (from, to) match {
    case (from, to) if from.isMem && to.isWriter && !to.isReader => super.emitEdge(from, to, toAlias, fromAlias)
    case (from, to) if from.isStreamIn && to.isStreamLoad => super.emitEdge(from, to, toAlias, fromAlias)
    case (_, _) if from.isDRAM => 
    case (_, _) => super.emitEdge(from, to, fromAlias, toAlias)
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
    case None if lhs.isBound => src"${lhs.parent.s.map{ s => s"$s."}.getOrElse("")}${super.label(lhs)}"
    case Some(rhs) if lhs.isMem => super.label(lhs) + src"\n${lhs.ctx}"
    case Some(rhs) if lhs.isControl => super.label(lhs) + src"\n${lhs.ctx}"
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
