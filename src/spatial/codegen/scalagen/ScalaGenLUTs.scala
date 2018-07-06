package spatial.codegen.scalagen

import argon._
import spatial.lang._
import spatial.node._

trait ScalaGenLUTs extends ScalaGenMemories {

  override protected def remap(tp: Type[_]): String = tp match {
    case tp: LUT[_,_] => src"Array[${tp.A}]"
    case _ => super.remap(tp)
  }
  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@LUTNew(_,elems) => emitBankedInitMem(lhs,Some(elems),op.A)
    case op@LUTBankedRead(lut,bank,ofs,ens) => emitBankedLoad(lhs,lut,bank,ofs,ens)(op.A)
    case _ => super.gen(lhs, rhs)
  }

}
