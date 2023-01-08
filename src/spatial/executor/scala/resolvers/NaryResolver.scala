package spatial.executor.scala.resolvers

import argon.node._
import argon.{Binary, Exp, Op, Unary, dbgs, stm}
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
        dbgs(s"Running Binary op: ${stm(sym)}, a -> $a, b -> $b")
        val result = fb.unstaged(a, b)
        SimpleEmulVal[U](result)

      case Op(un: Unary[U, _]) =>
        val a = execState.getValue[U](un.a)
        SimpleEmulVal[U](un.unstaged(a))

      case Op(lst: FixLst[_, _, _]) =>
        val a = execState.getValue[FixedPoint](lst.a)
        val b = execState.getValue[FixedPoint](lst.b)
        SimpleEmulVal((a < b))

      case Op(eq: FixEql[_, _, _]) =>
        val a = execState.getValue[FixedPoint](eq.a)
        val b = execState.getValue[FixedPoint](eq.b)
        SimpleEmulVal((a === b))

      case Op(op: FixSLA[_, _, _]) =>
        val a = execState.getValue[FixedPoint](op.a)
        val b = execState.getValue[FixedPoint](op.b)
        if (b >= 0) SimpleEmulVal(a << b) else SimpleEmulVal(a >> -b)

      case Op(op: FixToFix[_, _, _, _, _, _]) =>
        val a = execState.getValue[FixedPoint](op.a)
        SimpleEmulVal(a.toFixedPoint(op.fmt.toEmul))

      case _ => super.run(sym, execState)
    }
  }
}
