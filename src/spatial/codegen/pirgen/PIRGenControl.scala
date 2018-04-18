package spatial.codegen.pirgen

import argon._
import spatial.lang._

trait PIRGenControl extends PIRCodegen {
  protected def emitControlDone(ctrl: Sym[_]): Unit = { }

  protected def emitControlIncrement(ctrl: Sym[_], iter: Seq[Idx]): Unit = { }
}
