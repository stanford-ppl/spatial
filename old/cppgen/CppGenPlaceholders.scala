package spatial.codegen.cppgen

import argon.core._
import argon.nodes._
import argon.codegen.cppgen.CppCodegen

import spatial.nodes._

import scala.collection.mutable.ListBuffer

trait CppGenPlaceholders extends CppCodegen {
  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case NumpyArray(str) => emit(s"assert(0); // NumpyArray($str)")
    case NumpyMatrix(str) => emit(s"assert(0); // NumpyMatrix($str)")
    case _ => super.gen(lhs, rhs)
  }
}
