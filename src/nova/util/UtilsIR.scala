package nova.util

import core._
import spatial.lang._

trait UtilsIR {
  implicit class SymUtils[A](x: Sym[A]) {
    def isNum:  Boolean = x.isInstanceOf[Num[_]]
    def isBits: Boolean = x.isInstanceOf[Bits[_]]
    def isVoid: Boolean = x.isInstanceOf[Void]

    def nestedInputs: Set[Sym[_]] = {
      x.inputs.toSet ++ x.op.map{o =>
        val outs = o.blocks.flatMap(_.nestedStms)
        val used = outs.flatMap{s => s.nestedInputs }
        used diff outs
      }.getOrElse(Set.empty)
    }
  }

  implicit class ParamHelpers(x: I32) {
    def toInt: Int = x.c.map(_.toInt).getOrElse{throw new Exception(s"Cannot convert symbol $x to a constant")}
  }
}
