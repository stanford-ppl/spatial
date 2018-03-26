package spatial.util

import argon._
import forge.tags._
import spatial.lang._
import spatial.data._

trait UtilsIRLowPriority {
  implicit class TypeUtils[A](x: Type[A]) {
    def isNum:  Boolean = x.isInstanceOf[Num[_]]
    def isBits: Boolean = x.isInstanceOf[Bits[_]]
    def isVoid: Boolean = x.isInstanceOf[Void]
  }
}

trait UtilsIR extends UtilsIRLowPriority {
  /** Returns the number of bits of data the given symbol represents. */
  def nbits(e: Sym[_]): Int = e.tp match {case Bits(bT) => bT.nbits; case _ => 0 }


  @stateful def canMotion(stms: Seq[Sym[_]]): Boolean = {
    stms.forall{s => !takesEnables(s) && s.effects.isIdempotent }
  }
  @stateful def shouldMotion(stms: Seq[Sym[_]], inHw: Boolean): Boolean = {
    canMotion(stms) && (inHw || stms.length == 1)
  }

  implicit class SymUtils[A](x: Sym[A]) {
    def isIdx:  Boolean = x.tp match {
      case FixPtType(_,_,0) => true
      case _ => false
    }
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

  implicit class ParamHelpers(x: Sym[_]) {
    def toInt: Int = x match {
      case Expect(c) => c
      case _ => throw new Exception(s"Cannot convert symbol $x to a constant Int")
    }
  }
}
