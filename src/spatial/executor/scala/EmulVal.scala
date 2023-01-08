package spatial.executor.scala

import argon.Sym

trait EmulResult

abstract class EmulVal[+VT] extends EmulResult {
  def value: VT
  val valid: Boolean = true
}

case class SimpleEmulVal[+VT](value: VT, override val valid: Boolean = true) extends EmulVal[VT]

case class EmulUnit(sym: Sym[_]) extends EmulVal[Unit] {
  override def value: Unit = Unit
}

abstract class EmulMem extends EmulResult

