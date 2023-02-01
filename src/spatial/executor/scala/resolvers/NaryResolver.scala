package spatial.executor.scala.resolvers

import argon.node._
import argon._
import emul.FixedPoint
import spatial.executor.scala.{EmulResult, ExecutionState, SimpleEmulVal}

import scala.reflect.ClassTag

trait NaryResolver extends OpResolverBase {

  override def run[U, V](sym: Exp[U, V], execState: ExecutionState): EmulResult = {
    implicit val ct: ClassTag[U] = sym.tp.tag
    implicit val state: argon.State = execState.IR
    sym match {
      case Op(fb: Binary[U, _]) =>
        val a = execState.getValue[U](fb.a)
        val b = execState.getValue[U](fb.b)
        val result = fb.unstaged(a, b)
        SimpleEmulVal[U](result)

      case Op(un: Unary[U, _]) =>
        val a = execState.getValue[U](un.a)
        SimpleEmulVal[U](un.unstaged(a))

      case Op(cmp: Comparison[U, _]) =>
        val a = execState.getValue[U](cmp.a)
        val b = execState.getValue[U](cmp.b)
        SimpleEmulVal(cmp.unstaged(a, b))

      case _ => super.run(sym, execState)
    }
  }
}
