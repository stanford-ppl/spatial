package spatial.executor.scala

import argon.{Const, Exp, Param, Sym, Value, stm}
import spatial.executor.scala.memories.ScalaTensor
import spatial.executor.scala.resolvers.OpResolver

import scala.reflect.ClassTag

class ExecutionState(var values: Map[Exp[_, _], EmulResult], val log: String => Any, val error: String => Any) {
  def apply[U, V](s: Exp[U, V]): EmulResult = s match {
    case Value(result) =>
      implicit def ct: ClassTag[U] = s.tp.tag
      SimpleEmulVal(result)
    case _ => values(s)
  }

  def getValue[T](s: Exp[_, _]): T = {
    val recv = this(s)
    log(s"Reading: $s => $recv")
    recv match {
      case ev: EmulVal[T] => ev.value
    }
  }

  def getTensor[T](s: Exp[_, _]): ScalaTensor[T] = {
    this(s) match {
      case st: ScalaTensor[T] => st
    }
  }

  def register(sym: Sym[_], v: EmulResult): Unit = {
    values += (sym -> v)
  }

  def copy(): ExecutionState = new ExecutionState(values, log, error)

  def runAndRegister[U, V](s: Exp[U, V]): EmulResult = {
    val result = OpResolver.run(s, this)
    register(s, result)
    result
  }

  override def toString: String = {
    s"ExecutionState($values)"
  }
}
