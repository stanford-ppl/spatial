package spatial.codegen.scalagen

import argon._
import spatial.lang._

trait ScalaGenControl extends ScalaCodegen {
  def localMems: List[Sym[_]]

  protected def emitControlDone(ctrl: Sym[_]): Unit = { }

  protected def emitControlIncrement(ctrl: Sym[_], iter: Seq[Idx]): Unit = { }
}
