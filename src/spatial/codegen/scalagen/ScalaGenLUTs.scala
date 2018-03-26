package spatial.codegen.scalagen

import argon._
import spatial.lang._
import spatial.node._
import spatial.util._

trait ScalaGenLUTs extends ScalaGenMemories {

  override protected def remap(tp: Type[_]): String = tp match {
    case tp: LUT[_,_] => src"Array[${tp.A}]"
    case _ => super.remap(tp)
  }
  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@LUTNew(dims,elems) => emitMem(lhs, src"""$lhs = Array[${op.A}]($elems)""")

    case _ => super.gen(lhs, rhs)
  }

}
