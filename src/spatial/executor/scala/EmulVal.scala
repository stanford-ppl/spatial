package spatial.executor.scala

import argon.Sym

import scala.reflect.ClassTag

trait EmulResult {
  def sym: Sym[_]
}

abstract class EmulVal[VT] extends EmulResult {
  def value: VT
  def tag: ClassTag[VT]
}

case class SimpleEmulVal[VT](sym: Sym[_], value: VT) extends EmulVal[VT] {
  override lazy val tag: ClassTag[VT] = sym.tp.tag.asInstanceOf[ClassTag[VT]]
}

case class EmulUnit(sym: Sym[_]) extends EmulVal[Unit] {
  override def value: Unit = Unit
  override def tag: ClassTag[Unit] = implicitly[ClassTag[Unit]]
}

abstract class EmulMem[ET] extends EmulResult {
  def tag: ClassTag[ET]
}

