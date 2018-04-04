package argon.codegen.cppgen

import argon.core._
import argon.nodes._

trait CppGenUnit extends CppCodegen {

  override protected def quoteConst(c: Const[_]): String = c match {
    case Const(()) => "()"
    case _ => super.quoteConst(c)
  }
}
