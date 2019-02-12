package spatial.codegen.pirgen

import argon._
import argon.node._
import spatial.lang._

trait PIRGenVec extends PIRCodegen {

  override protected def genHost(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case VecAlloc(elems)     => emit(src"val $lhs = Array($elems)")
    case _ => super.genHost(lhs, rhs)
  }

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case VecApply(vector, i) => 
      assert(i==0, s"VecApply of i != 0 for plasticine")
      stateStruct(lhs, lhs.tp) { name =>
        Lhs(vector,name)
      }

    case VecSlice(vector, end, start) =>
     emit(src"val $lhs = $vector.slice($start, $end+1)") // end is non-inclusive

    //case VecConcat(vectors) =>
      // val v = concat(c(4::0), b(4::0), a(4::0))
      // v(12) // should give a(2)
      //val concat = vectors.map(quote).mkString(" ++ ")
      //emit(src"val $lhs = $concat")

    // Other cases (Structs, Vectors) are taken care of using rewrite rules


    case _ => super.genAccel(lhs, rhs)
  }

}
