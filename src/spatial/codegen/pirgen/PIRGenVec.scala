package spatial.codegen.pirgen

import argon._
import argon.node._
import spatial.lang._

trait PIRGenVec extends PIRGenBits with PIRGenText {

  override protected def remap(tp: Type[_]): String = tp match {
    case tp: Vec[_] => src"Array[${tp.A}]"
    case _ => super.remap(tp)
  }

  override def invalid(tp: Type[_]): String = tp match {
    case tp: Vec[_] => src"""Array.fill(${tp.nbits}(${invalid(tp.A)})"""
    case _ => super.invalid(tp)
  }

  override def emitToString(lhs: Sym[_], x: Sym[_], tp: Type[_]): Unit = tp match {
    case v: Vec[_] =>
      v.A match {
        case _:Bit =>
          emit(src"""val $lhs = "0b" + $x.sliding(4,4).map{_.reverse.map{x => if (x) "1" else "0"}.mkString("")}.toList.reverse.mkString("_")""")
        case _ =>
          emit(src"""val $lhs = "Vector.ZeroFirst(" + $x.mkString(", ") + ")" """)
      }
    case _ => super.emitToString(lhs,x,tp)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case VecAlloc(elems)     => emit(src"val $lhs = Array($elems)")
    case VecApply(vector, i) => emit(src"val $lhs = $vector.apply($i)")
    case VecSlice(vector, end, start) =>
     emit(src"val $lhs = $vector.slice($start, $end+1)") // end is non-inclusive

    case VecConcat(vectors) =>
      // val v = concat(c(4::0), b(4::0), a(4::0))
      // v(12) // should give a(2)
      val concat = vectors.map(quote).mkString(" ++ ")
      emit(src"val $lhs = $concat")

    // Other cases (Structs, Vectors) are taken care of using rewrite rules


    case _ => super.gen(lhs, rhs)
  }

}
