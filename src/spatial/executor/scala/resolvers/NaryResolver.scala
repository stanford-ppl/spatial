package spatial.executor.scala.resolvers

import argon.node._
import argon._
import emul.FixedPoint
import spatial.executor.scala._

import scala.reflect.ClassTag

trait NaryResolver extends OpResolverBase {

  def hasInvalidInput(op: Op[_], execState: ExecutionState): Boolean = {
    op.inputs.foreach {
      input => execState(input) match {
        case ev: EmulVal[_] if !ev.valid =>
          return true
        case _ =>
      }
    }
    false
  }
  override def run[U, V](sym: Exp[U, V], op: Op[V], execState: ExecutionState): EmulResult = {
    implicit val ct: ClassTag[U] = sym.tp.tag
    implicit val state: argon.State = execState.IR
    op match {
      case _: Binary[_, _] | _: Unary[_, _] if hasInvalidInput(op, execState) =>
        dbgs(s"Evaluating ${stm(sym)}, but some arguments are poison: ${op.inputs.map(execState(_))}")
        EmulPoison(sym)

      case fb: Binary[U, _] =>
        val a = execState.getValue[U](fb.a)
        val b = execState.getValue[U](fb.b)
//        dbgs(s"Running Binary op: ${stm(sym)}, a -> $a, b -> $b")
        val result = fb.unstaged(a, b)
        SimpleEmulVal[U](result)

      case un: Unary[U, _] =>
        val a = execState.getValue[U](un.a)
        SimpleEmulVal[U](un.unstaged(a))

      case cmp: Comparison[U, _] =>
        val a = execState.getValue[U](cmp.a)
        val b = execState.getValue[U](cmp.b)
        SimpleEmulVal(cmp.unstaged(a, b))

      case _ => super.run(sym, op, execState)
    }
  }
}
