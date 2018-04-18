package spatial.codegen.pirgen

import argon._
import spatial.lang._

trait PIRGenVoid extends PIRCodegen {

  override protected def quoteConst(tp: Type[_], c: Any): String = (tp,c) match {
    case (_:Void, _:Unit) => "()"
    case _ => super.quoteConst(tp,c)
  }

}
