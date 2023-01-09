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

case class EmulPoison(sym: Sym[_]) extends EmulVal[Nothing] {
  override def value: Nothing = throw new Exception(s"Attempting to access invalid value defined by $sym")
  override val valid: Boolean = false
}

abstract class EmulMem extends EmulResult

