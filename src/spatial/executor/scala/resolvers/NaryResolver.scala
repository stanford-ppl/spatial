package spatial.executor.scala.resolvers

import argon.node.{FixEql, FixLst}
import argon.{Binary, Exp, Op, Unary}
import emul.FixedPoint
import spatial.executor.scala.{EmulResult, ExecutionState, SimpleEmulVal}

trait NaryResolver extends OpResolverBase {
  override def run[U, V](sym: Exp[U, V], execState: ExecutionState): EmulResult = {
    sym match {
      case Op(fb: Binary[U, _]) =>
        val a = execState.getValue[U](fb.a)
        val b = execState.getValue[U](fb.b)
        val result = fb.unstaged(a, b)
        SimpleEmulVal[U](sym, result)

      case Op(un: Unary[U, _]) =>
        val a = execState.getValue[U](un.a)
        SimpleEmulVal[U](sym, un.unstaged(a))

      case Op(lst: FixLst[_, _, _]) =>
        val a = execState.getValue[FixedPoint](lst.a)
        val b = execState.getValue[FixedPoint](lst.b)
        SimpleEmulVal(sym, (a < b).value)

      case Op(eq: FixEql[_, _, _]) =>
        val a = execState.getValue[FixedPoint](eq.a)
        val b = execState.getValue[FixedPoint](eq.b)
        SimpleEmulVal(sym, (a === b).value)

      case _ => super.run(sym, execState)
    }
  }
}
