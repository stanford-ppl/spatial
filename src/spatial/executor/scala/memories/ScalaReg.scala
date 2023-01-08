package spatial.executor.scala.memories

import argon.Sym
import spatial.executor.scala.{EmulMem, EmulResult, EmulVal}

import scala.reflect.ClassTag

class ScalaReg[TP <: EmulResult](init: TP, var curVal: TP) extends EmulMem {
  type ET = TP

  def reset(): Unit = {curVal = init}
  def write(data: TP, en: Boolean): Unit = if (en) {curVal = data}

  override def toString: String = {
    s"ScalaReg(init = $init, curVal = $curVal)"
  }
}
