package spatial.metadata

import argon._
import forge.tags._
import spatial.lang._
import spatial.metadata.bounds.Expect

trait UtilsIRLowPriority {
  implicit class TypeUtils[A](x: Type[A]) {
    def isNum:  Boolean = x.isInstanceOf[Num[_]]
    def isBits: Boolean = x.isInstanceOf[Bits[_]]
    def isVoid: Boolean = x.isInstanceOf[Void]
    def isString: Boolean = x.typeArgs.headOption.exists(_.isInstanceOf[Text])
  }
}

object types extends UtilsIRLowPriority {
  /** Returns the number of bits of data the given symbol represents. */
  @rig def nbits(e: Sym[_]): Int = e.tp match {case Bits(bT) => bT.nbits; case _ => 0 }

  implicit class SymUtils[A](x: Sym[A]) {
    def isIdx:  Boolean = x.tp match {
      case FixPtType(_,_,0) => true
      case _ => false
    }
    def isNum:  Boolean = x.isInstanceOf[Num[_]]
    def isBits: Boolean = x.isInstanceOf[Bits[_]]
    def isVoid: Boolean = x.isInstanceOf[Void]
    def isString: Boolean = x.tp.isString

    def bitInputs: Seq[Sym[_]] = x.op.map(_.bitInputs).getOrElse(Nil)
  }

  implicit class OpUtils(op: Op[_]) {
    def bitInputs: Seq[Sym[_]] = op.expInputs.filter(_.isBits)
  }

  implicit class ParamHelpers(x: Sym[_]) {
    def toInt: Int = x match {
      case Expect(c) => c
      case _ => throw new Exception(s"Cannot convert symbol $x to a constant Int")
    }
  }
}
