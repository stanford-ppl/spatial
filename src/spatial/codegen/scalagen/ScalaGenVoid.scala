package spatial.codegen.scalagen

import argon._
import spatial.lang._

trait ScalaGenVoid extends ScalaCodegen {
  override protected def quoteConst(tp: Type[_], c: Any): String = (tp,c) match {
    case (_:Void, _) => "()"
    case _ => super.quoteConst(tp,c)
  }
}
