package spatial.executor.scala.memories

import argon.Sym
import spatial.executor.scala.{EmulMem, EmulVal}

import scala.reflect.ClassTag

class ScalaReg[TP](val sym: Sym[_], init: TP, var curVal: TP)(implicit ct: ClassTag[TP]) extends EmulMem[TP] {
  type ET = TP

  def reset(): Unit = {curVal = init}
  def write(data: TP, en: Boolean): Unit = if (en) {curVal = data}
  override lazy val tag: ClassTag[TP] = ct

  override def toString: String = {
    s"ScalaReg[${tag.toString}]($sym, init = $init, curVal = $curVal)"
  }
}
