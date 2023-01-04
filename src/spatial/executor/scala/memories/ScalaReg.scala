package spatial.executor.scala.memories

import argon.Sym
import spatial.executor.scala.{EmulMem, EmulVal}

import scala.reflect.ClassTag

class ScalaReg[TP](val sym: Sym[_], init: EmulVal[TP], var curVal: TP) extends EmulMem[TP] {
  type ET = TP

  def reset(): Unit = {curVal = init.value}
  def write(data: EmulVal[TP], en: Boolean): Unit = if (en) {curVal = data.value}
  override lazy val tag: ClassTag[TP] = init.tag
}
