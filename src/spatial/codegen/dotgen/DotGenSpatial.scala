package spatial.codegen.dotgen

import argon._
import spatial.lang._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.node._

trait DotGenSpatial extends DotCodegen {

  override def inputs(lhs:Sym[_]):Seq[Sym[_]] = lhs match {
    case Def(_:DRAMAddress[_,_]) => Nil
    case _ if lhs.isDRAM =>
      super.inputs(lhs) ++ 
      lhs.consumers.filter { c => 
        c.isTileStore || (c match { case Def(SetMem(_,_)) => true; case _ => false })
      }
    case _ if lhs.isStreamIn => super.inputs(lhs) ++ lhs.consumers.filter { _.isTileTransfer } ++ lhs.writers
    case _ if lhs.isMem => super.inputs(lhs) ++ lhs.writers
    case Writer(mem, _, _, _) => super.inputs(lhs).filterNot { _ == mem }
    case BankedWriter(mem, _, _, _, _) => super.inputs(lhs).filterNot { _ == mem }
    case Def(SetMem(dram, _)) => super.inputs(lhs).filterNot(_ == dram)
    case _ if lhs.isTileStore => super.inputs(lhs).filterNot { i => i.isDRAM || i.isStreamIn }
    case _ if lhs.isTileTransfer => super.inputs(lhs).filterNot { _.isStreamIn }
    case Def(_:ArrayNew[_]) => super.inputs(lhs) ++ lhs.consumers.filter { case Def(_:GetMem[_,_]) => true; case _ => false }
    case Def(GetMem(_, data)) => super.inputs(lhs).filterNot { _ == data }
    case _ => super.inputs(lhs)
  }

  override def nodeAttr(lhs:Sym[_]):Map[String,String] = super.nodeAttr(lhs) ++ (lhs match {
    case _:SRAM[_,_]    => "color" -> "forestgreen" :: "style" -> "filled" :: "shape" -> "box" :: Nil
    case _:LockSRAM[_,_]    => "color" -> "forestgreen" :: "style" -> "filled" :: "shape" -> "box" :: Nil
    case _:Lock[_]    => "color" -> "crimson" :: "style" -> "filled" :: "shape" -> "box" :: Nil
    case _:RegFile[_,_] => "color" -> "forestgreen" :: "style" -> "filled" :: "shape" -> "box" :: Nil
    case _:LUT[_,_] => "color" -> "forestgreen" :: "style" -> "filled" :: "shape" -> "box" :: Nil
    case _ if lhs.isReg => "color" -> "chartreuse2" :: "style" -> "filled" :: "shape" -> "box" :: Nil
    case _ if lhs.isFIFO | lhs.isFIFOReg | lhs.isStreamIn | lhs.isStreamOut =>
                              "color" -> "gold"        :: "style" -> "filled" :: "shape" -> "box" :: Nil
    case _:DRAM[_,_]    => "color" -> "blueviolet"  :: "style" -> "filled" :: "shape" -> "box" :: Nil
    case _ => Nil
  })

  override def label(lhs:Sym[_]): String = lhs match {
    case _ if lhs.isBound => src"${lhs.parent.s.map{ s => s"$s."}.getOrElse("")}${super.label(lhs)}"
    case _ if lhs.isMem => super.label(lhs) + src"\n${lhs.ctx}"
    case Def(UnrolledReduce(_, cchain, _, _, _, _)) =>
      super.label(lhs) + src"\npars=${cchain.pars}" + src"\n${lhs.ctx}"// + lhs.ctx.content.map{ c => s"\n$c" }.getOrElse("")
    case Def(UnrolledForeach(_, cchain, _, _, _, _)) =>
      super.label(lhs) + src"\npars=${cchain.pars}" + src"\n${lhs.ctx}"// + lhs.ctx.content.map{ c => s"\n$c" }.getOrElse("")
    case _ if lhs.isControl => super.label(lhs) + src"\n${lhs.ctx}"// + lhs.ctx.content.map{ c => s"\n$c" }.getOrElse("")
    case Def(CounterNew(_,_,_,par)) => super.label(lhs) + src"\npar=$par"
    case Def(DRAMAddress(dram)) => super.label(lhs) + src"\ndram=${label(dram)}"
    case _ => super.label(lhs)
  }

  override def inputGroups(lhs:Sym[_]):Map[String, Seq[Sym[_]]] = lhs match {
    // Accesses
    case Def(SRAMBankedWrite(_, data, bank, ofs, enss)) =>
      super.inputGroups(lhs) + ("data" -> data) + ("bank" -> bank.flatten) + ("ofs" -> ofs) + ("enss" -> enss.flatten)
    case Def(SRAMBankedRead(_, bank, ofs, enss)) =>
      super.inputGroups(lhs) + ("bank" -> bank.flatten) + ("ofs" -> ofs) + ("enss" -> enss.flatten)
    case Def(LUTBankedRead(_, bank, ofs, enss)) =>
      super.inputGroups(lhs) + ("bank" -> bank.flatten) + ("ofs" -> ofs) + ("enss" -> enss.flatten)
    case Def(StreamInBankedRead(_, enss)) =>
      super.inputGroups(lhs) + ("enss" -> enss.flatten)
    case Def(StreamOutBankedWrite(_, data, enss)) =>
      super.inputGroups(lhs) + ("data" -> data) + ("enss" -> enss.flatten)
    case Def(FIFOBankedDeq(_, enss)) =>
      super.inputGroups(lhs) + ("enss" -> enss.flatten)
    case Def(FIFOBankedEnq(_, data, enss)) =>
      super.inputGroups(lhs) + ("data" -> data) + ("enss" -> enss.flatten)
    case Def(FIFOIsEmpty(_, enss)) =>
      super.inputGroups(lhs) + ("enss" -> enss.toSeq)
    case Def(FIFOIsFull(_, enss)) =>
      super.inputGroups(lhs) + ("enss" -> enss.toSeq)
    case Def(FIFOIsAlmostEmpty(_, enss)) =>
      super.inputGroups(lhs) + ("enss" -> enss.toSeq)
    case Def(FIFOIsAlmostFull(_, enss)) =>
      super.inputGroups(lhs) + ("enss" -> enss.toSeq)
    case Def(FIFONumel(_, enss)) =>
      super.inputGroups(lhs) + ("enss" -> enss.toSeq)
    // Controller
    case Def(UnitPipe(ens, _,_)) =>
      super.inputGroups(lhs) + ("ens" -> ens.toSeq)
    case Def(ParallelPipe(ens, _)) =>
      super.inputGroups(lhs) + ("ens" -> ens.toSeq)
    case Def(UnrolledForeach(ens,_,_,_,_, _)) =>
      super.inputGroups(lhs) + ("ens" -> ens.toSeq)
    case Def(UnrolledReduce(ens,_,_,_,_, _)) =>
      super.inputGroups(lhs) + ("ens" -> ens.toSeq)
    case Def(Switch(selects, _)) =>
      super.inputGroups(lhs) + ("selects" -> selects)
    case Def(StateMachine(ens, _, _, _, _)) =>
      super.inputGroups(lhs) + ("ens" -> ens.toSeq)
    //case Def(IfThenElse(cond, thenp, elsep)) =>
    case _ => super.inputGroups(lhs)
  }

}

case class DotFlatGenSpatial(IR: State) extends DotFlatCodegen with DotGenSpatial
case class DotHierarchicalGenSpatial(IR: State) extends DotHierarchicalCodegen with DotGenSpatial
