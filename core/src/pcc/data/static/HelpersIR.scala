package pcc.data.static

import pcc.core._
import pcc.lang._

trait HelpersIR {
  implicit class SymUtils(x: Sym[_]) {
    def isNum:  Boolean = x.isInstanceOf[Num[_]]
    def isBits: Boolean = x.isInstanceOf[Bits[_]]
    def isVoid: Boolean = x.isInstanceOf[Void]

    def nestedDataInputs: Set[Sym[_]] = {
      x.dataInputs.toSet ++ x.op.map{o =>
        val outs = o.blocks.flatMap(_.nestedStms)
        val used = outs.flatMap{s => s.nestedDataInputs }
        used diff outs
      }.getOrElse(Set.empty)
    }
  }

  implicit class BlockUtils(x: Block[_]) {
    def nestedStms: Set[Sym[_]] = x.stms.flatMap{s =>
      s.op.map{o => o.blocks.flatMap(_.nestedStms) }.getOrElse(Nil)
    }.toSet
  }

  implicit class ParamHelpers(x: I32) {
    def toInt: Int = x.c.getOrElse{throw new Exception(s"Cannot convert symbol $x to a constant")}
  }
}
