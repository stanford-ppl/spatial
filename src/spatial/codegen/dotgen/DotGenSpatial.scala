package spatial.codegen.dotgen

import argon._
import argon.node._
import spatial.metadata.memory._
import spatial.metadata.access._
import spatial.lang._
import spatial.node._
import spatial.metadata._
import spatial.metadata.control._
import spatial.util.spatialConfig

trait DotGenSpatial extends DotCodegen {

  override def inputs(lhs:Sym[_]):Seq[Sym[_]] = lhs match {
    case Def(_:GetDRAMAddress[_,_]) => Nil
    case lhs if lhs.isDRAM => 
      super.inputs(lhs) ++ 
      lhs.consumers.filter { c => 
        c.isTileStore || (c match { case Def(SetMem(_,_)) => true; case _ => false })
      }
    case lhs if lhs.isStreamIn => super.inputs(lhs) ++ lhs.consumers.filter { _.isTileTransfer } ++ lhs.writers
    case lhs if lhs.isMem => super.inputs(lhs) ++ lhs.writers
    case Writer(mem, data, addr, ens) => super.inputs(lhs).filterNot { _ == mem }
    case BankedWriter(mem, data, bank, ofs, ens) => super.inputs(lhs).filterNot { _ == mem }
    case Def(SetMem(dram, _)) => super.inputs(lhs).filterNot(_ == dram)
    case lhs if lhs.isTileStore => super.inputs(lhs).filterNot { i => i.isDRAM || i.isStreamIn }
    case lhs if lhs.isTileTransfer => super.inputs(lhs).filterNot { _.isStreamIn }
    case Def(_:ArrayNew[_]) => super.inputs(lhs) ++ lhs.consumers.filter { case Def(_:GetMem[_,_]) => true; case _ => false }
    case Def(GetMem(dram, data)) => super.inputs(lhs).filterNot { _ == data }
    case _ => super.inputs(lhs)
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

  override def label(lhs:Sym[_]) = lhs match {
    case lhs if lhs.isBound => src"${lhs.parent.s.map{ s => s"$s."}.getOrElse("")}${super.label(lhs)}"
    case lhs if lhs.isMem => super.label(lhs) + src"\n${lhs.ctx}"
    case Def(UnrolledReduce(ens, cchain, func, iters, valids)) =>
      super.label(lhs) + src"\npars=${cchain.pars}" + src"\n${lhs.ctx}"// + lhs.ctx.content.map{ c => s"\n$c" }.getOrElse("")
    case Def(UnrolledForeach(ens, cchain, func, iters, valids)) =>
      super.label(lhs) + src"\npars=${cchain.pars}" + src"\n${lhs.ctx}"// + lhs.ctx.content.map{ c => s"\n$c" }.getOrElse("")
    case lhs if lhs.isControl => super.label(lhs) + src"\n${lhs.ctx}"// + lhs.ctx.content.map{ c => s"\n$c" }.getOrElse("")
    case Def(CounterNew(_,_,_,par)) => super.label(lhs) + src"\npar=${par}"
    case Def(GetDRAMAddress(dram)) => super.label(lhs) + src"\ndram=${label(dram)}"
    case _ => super.label(lhs)
  }

  override def inputGroups(lhs:Sym[_]):Map[String, Seq[Sym[_]]] = lhs match {
    // Accesses
    case Def(SRAMBankedWrite(mem, data, bank, ofs, enss)) => 
      super.inputGroups(lhs) + ("data" -> data) + ("bank" -> bank.flatten) + ("ofs" -> ofs) + ("enss" -> enss.flatten)
    case Def(SRAMBankedRead(mem, bank, ofs, enss)) => 
      super.inputGroups(lhs) + ("bank" -> bank.flatten) + ("ofs" -> ofs) + ("enss" -> enss.flatten)
    case Def(StreamInBankedRead(mem, enss)) =>
      super.inputGroups(lhs) + ("enss" -> enss.flatten)
    case Def(StreamOutBankedWrite(mem, data, enss)) => 
      super.inputGroups(lhs) + ("data" -> data) + ("enss" -> enss.flatten)
    case Def(FIFOBankedDeq(mem, enss)) =>
      super.inputGroups(lhs) + ("enss" -> enss.flatten)
    case Def(FIFOBankedEnq(mem, data, enss)) => 
      super.inputGroups(lhs) + ("data" -> data) + ("enss" -> enss.flatten)
    case Def(FIFOIsEmpty(mem, enss)) => 
      super.inputGroups(lhs) + ("enss" -> enss.toSeq)
    case Def(FIFOIsFull(mem, enss)) => 
      super.inputGroups(lhs) + ("enss" -> enss.toSeq)
    case Def(FIFOIsAlmostEmpty(mem, enss)) => 
      super.inputGroups(lhs) + ("enss" -> enss.toSeq)
    case Def(FIFOIsAlmostFull(mem, enss)) => 
      super.inputGroups(lhs) + ("enss" -> enss.toSeq)
    case Def(FIFONumel(mem, enss)) => 
      super.inputGroups(lhs) + ("enss" -> enss.toSeq)
    // Controller
    case Def(UnitPipe(ens, func)) =>
      super.inputGroups(lhs) + ("ens" -> ens.toSeq)
    case Def(ParallelPipe(ens, func)) =>
      super.inputGroups(lhs) + ("ens" -> ens.toSeq)
    case Def(UnrolledForeach(ens,cchain,func,iters,valids)) =>
      super.inputGroups(lhs) + ("ens" -> ens.toSeq)
    case Def(UnrolledReduce(ens,cchain,func,iters,valids)) =>
      super.inputGroups(lhs) + ("ens" -> ens.toSeq)
    case Def(Switch(selects, body)) =>
      super.inputGroups(lhs) + ("selects" -> selects.toSeq)
    case Def(StateMachine(ens, start, notDone, action, nextState)) =>
      super.inputGroups(lhs) + ("ens" -> ens.toSeq)
    //case Def(IfThenElse(cond, thenp, elsep)) =>
    case _ => super.inputGroups(lhs)
  }

}

case class DotFlatGenSpatial(IR: State) extends DotFlatCodegen with DotGenSpatial
case class DotHierarchicalGenSpatial(IR: State) extends DotHierarchicalCodegen with DotGenSpatial
