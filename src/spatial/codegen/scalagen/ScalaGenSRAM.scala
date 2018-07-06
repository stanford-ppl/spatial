package spatial.codegen.scalagen

import argon._
import spatial.lang._
import spatial.node._

trait ScalaGenSRAM extends ScalaGenMemories {

  override protected def remap(tp: Type[_]): String = tp match {
    case tp: SRAM[_,_] => src"Array[${tp.A}]"
    case _ => super.remap(tp)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op: SRAMNew[_,_] => emitBankedInitMem(lhs, None, op.A)
    case op@SRAMBankedRead(sram,bank,ofs,ens)       => emitBankedLoad(lhs,sram,bank,ofs,ens)(op.A)
    case op@SRAMBankedWrite(sram,data,bank,ofs,ens) => emitBankedStore(lhs,sram,data,bank,ofs,ens)(op.A)
    case _ => super.gen(lhs, rhs)
  }
}
