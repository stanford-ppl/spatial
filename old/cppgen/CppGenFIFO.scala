package spatial.codegen.cppgen

import argon.codegen.cppgen.CppCodegen
import argon.core._
import spatial.aliases._
import spatial.nodes._


trait CppGenFIFO extends CppCodegen {

  override protected def name(s: Dyn[_]): String = s match {
    case Def(_: FIFONew[_])            => s"""${s}_${s.name.getOrElse("fifo")}"""
    case Def(FIFOEnq(fifo:Sym[_],_,_)) => s"${s}_enqTo${fifo.id}"
    case Def(FIFODeq(fifo:Sym[_],_))   => s"${s}_deqFrom${fifo.id}"
    case _ => super.name(s)
  } 

  override protected def remap(tp: Type[_]): String = tp match {
    case tp: FIFOType[_] => src"cpp.collection.mutable.Queue[${tp.child}]"
    case _ => super.remap(tp)
  }

  override protected def emitNode(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case _ => super.emitNode(lhs, rhs)
  }
}
