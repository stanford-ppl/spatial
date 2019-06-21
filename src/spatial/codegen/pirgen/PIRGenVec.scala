package spatial.codegen.pirgen

import argon._
import argon.node._
import spatial.lang._

trait PIRGenVec extends PIRCodegen {

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case VecApply(vector, i) => 
      if (i!=0) {
        bug(s"VecApply of i != 0 for plasticine")
        IR.logBug()
      }
      stateStruct(lhs, lhs.tp) { field =>
        Lhs(vector,field.map{_._1})
      }

    case VecSlice(vector, end, start) =>
     emit(src"val $lhs = $vector.slice($start, $end+1)") // end is non-inclusive

    case VecAlloc(elems) => //TODO: Can be from user input asBits, which will not necessary have one element
      if (elems.size != 1) {
        bug(s"number of elements in $elems is not 1 for Plasticine $lhs (${lhs.ctx})")
        IR.logBug()
      }
      stateStruct(lhs, lhs.tp) { field =>
        Lhs(elems.head.asInstanceOf[Sym[_]],field.map{_._1})
      }
    //case VecConcat(vectors) =>
      // val v = concat(c(4::0), b(4::0), a(4::0))
      // v(12) // should give a(2)
      //val concat = vectors.map(quote).mkString(" ++ ")
      //emit(src"val $lhs = $concat")

    // Other cases (Structs, Vectors) are taken care of using rewrite rules


    case _ => super.genAccel(lhs, rhs)
  }

}
