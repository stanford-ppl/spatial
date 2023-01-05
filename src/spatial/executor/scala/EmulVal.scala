package spatial.executor.scala

import argon.Sym

import scala.reflect.ClassTag

trait EmulResult
case class SymWithResult(sym: Sym[_], value: EmulResult)

abstract class EmulVal[VT] extends EmulResult {
  def value: VT
  def tag: ClassTag[VT]
  val valid: Boolean = true
}

case class SimpleEmulVal[VT: ClassTag](value: VT, override val valid: Boolean = true) extends EmulVal[VT] {
  override lazy val tag: ClassTag[VT] = implicitly[ClassTag[VT]]
}

case class EmulUnit(sym: Sym[_]) extends EmulVal[Unit] {
  override def value: Unit = Unit
  override def tag: ClassTag[Unit] = implicitly[ClassTag[Unit]]
}

abstract class EmulMem[ET] extends EmulResult {
  def tag: ClassTag[ET]
}

