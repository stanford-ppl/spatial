package spatial.executor.scala

import argon.{Const, Exp, Sym, stm}
import spatial.executor.scala.resolvers.OpResolver

import scala.reflect.ClassTag

class ExecutionState(var values: Map[Sym[_], EmulResult] = Map.empty, val log: String => Any) {
  def apply[U, V](s: Exp[U, V]): EmulResult = s match {
    case Const(result) =>
      implicit def ct: ClassTag[U] = s.tp.tag
      SimpleEmulVal(s, result)
    case _ => values(s.asInstanceOf[Sym[_]])
  }

  def getValue[T](s: Exp[_, _]): T = this(s) match {
    case ev: EmulVal[T] => ev.value
  }

  def register(v: EmulResult): Unit = {
    values += (v.sym -> v)
  }

  def copy(): ExecutionState = new ExecutionState(values, log)

  def runAndRegister[U, V](s: Exp[U, V]): EmulResult = {
    val result = OpResolver.run(s, this)
    register(result)
    result
  }

  override def toString: String = {
    s"ExecutionState($values)"
  }
}
