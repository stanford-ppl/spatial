package spatial.codegen.chiselgen

import argon.compiler.Index
import argon.core._
import argon.nodes.{BuildHashMap, HashIndexApply, HashIndexType}

trait ChiselGenHashMap extends ChiselCodegen {

  override protected def remap(tp: Type[_]): String = tp match {
    case HashIndexType(mK) => src"scala.collection.mutable.HashMap[$mK,${typ[Index]}]"
    case _ => super.remap(tp)
  }

  override protected def emitNode(lhs: Sym[_], rhs: Op[_]) = rhs match {
    case HashIndexApply(index, key) => emit(src"val $lhs = $index.getOrElse($key, -1)")
    case _ => super.emitNode(lhs, rhs)
  }

  override protected def emitFat(lhs: Seq[Sym[_]], rhs: Def) = rhs match {
    case e @ BuildHashMap(in, apply, keyFunc, valFunc, reduce, rV, i) =>

    case _ => super.emitFat(lhs, rhs)
  }

}
