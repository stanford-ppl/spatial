package spatial.codegen.chiselgen

import argon._
import argon.node._

trait ChiselGenVec extends ChiselGenCommon {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case VecAlloc(elems) => emit(src"val $lhs = Vec($elems)")

    case VecSlice(vec, start, stop) =>
      emit(createWire(quote(lhs),remap(lhs.tp)))
      emit(src"$lhs.zipWithIndex.foreach{case(w, i) => w := $vec(i+$stop)}")

    case VecConcat(list) => 
      emit(createWire(quote(lhs),remap(lhs.tp)))
      val raws = list.map{x => src"$x"}.mkString(" ++ ")
      emit(src"${lhs}.zip($raws).foreach{case (a,b) => a := b}")

    case VecApply(vec, id) =>
      emit(createWire(quote(lhs),remap(lhs.tp)))
      emit(src"$lhs.r := $vec($id).r")

    case _ => super.gen(lhs, rhs)
  }

}