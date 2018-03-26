package spatial.codegen.scalagen

import argon._
import spatial.lang._
import spatial.node._
import spatial.util._

trait ScalaGenSRAM extends ScalaGenMemories {

  override protected def remap(tp: Type[_]): String = tp match {
    case tp: SRAM[_,_] => src"Array[${tp.A}]"
    case _ => super.remap(tp)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@SRAMNew(dims) => emitMem(lhs, src"""$lhs = Array.fill(${dims.map(quote).mkString("*")})(${invalid(op.A)})""")

    case op@SRAMBankedRead(sram,inds,ens) =>
      val dims = stagedDimsOf(sram)
      open(src"val $lhs = {")
      inds.indices.foreach{i =>
        open(src"val a$i = {")
        oobApply(op.mT,sram,lhs,inds(i)){ emit(src"""if (${ens(i)}) $sram.apply(${flattenAddress(dims, inds(i))}) else ${invalid(op.mT)}""") }
        close("}")
      }
      emit(src"Array[${op.mT}](" + inds.indices.map{i => src"a$i"}.mkString(", ") + ")")
      close("}")

    case op@ParSRAMStore(sram,inds,data,ens) =>
      val dims = stagedDimsOf(sram)
      open(src"val $lhs = {")
      inds.indices.foreach{i =>
        oobUpdate(op.mT, sram, lhs,inds(i)){ emit(src"if (${ens(i)}) $sram.update(${flattenAddress(dims, inds(i))}, ${data(i)})") }
      }
      close("}")

    case _ => super.gen(lhs, rhs)
  }
}
