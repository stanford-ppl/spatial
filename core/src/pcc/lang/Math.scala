package pcc.lang

import forge._
import pcc.core._
import pcc.node._

object Math {
  @api def min[A:Num](a: A, b: A): A = a match {
    case x: Fix[_] => stage(FixMin(a,b)(x.asType[Fix[A]]))
    case x: Flt[_] => stage(FltMin(a,b)(x.asType[Flt[A]]))
    case _ =>
      error(ctx, s"min is not defined for inputs of type ${num[A].typeName}")
      error(ctx)
      bound[A]
  }
  @api def max[A:Num](a: A, b: A): A = a match {
    case x: Fix[_] => stage(FixMax(a,b)(x.asType[Fix[A]]))
    case x: Flt[_] => stage(FltMax(a,b)(x.asType[Flt[A]]))
    case _ =>
      error(ctx, s"max is not defined for inputs of type ${num[A].typeName}")
      error(ctx)
      bound[A]
  }

  @api def sigmoid[A:Num](a: A): A = a match {
    case x: Fix[_] => stage(FixSig(a)(x.asType[Fix[A]]))
    case x: Flt[_] => stage(FltSig(a)(x.asType[Flt[A]]))
    case _ =>
      error(ctx, s"sigmoid not defined for inputs of type ${num[A].typeName}")
      error(ctx)
      bound[A]
  }
  @api def exp[A:Num](a: A): A = a match {
    case x: Fix[_] => stage(FixExp(a)(x.asType[Fix[A]]))
    case x: Flt[_] => stage(FltExp(a)(x.asType[Flt[A]]))
    case _ =>
      error(ctx, s"exp not defined for inputs of type ${num[A].typeName}")
      error(ctx)
      bound[A]
  }
  @api def log[A:Num](a: A): A = a match {
    case x: Fix[_] => stage(FixLog(a)(x.asType[Fix[A]]))
    case x: Flt[_] => stage(FltLog(a)(x.asType[Flt[A]]))
    case _ =>
      error(ctx, s"log not defined for inputs of type ${num[A].typeName}")
      error(ctx)
      bound[A]
  }
  @api def sqrt[A:Num](a: A): A = a match {
    case x: Fix[_] => stage(FixSqt(a)(x.asType[Fix[A]]))
    case x: Flt[_] => stage(FltSqt(a)(x.asType[Flt[A]]))
    case _ =>
      error(ctx, s"sqrt not defined for inputs of type ${num[A].typeName}")
      error(ctx)
      bound[A]
  }
  @api def abs[A:Num](a: A): A = a match {
    case x: Fix[_] => stage(FixAbs(a)(x.asType[Fix[A]]))
    case x: Flt[_] => stage(FltAbs(a)(x.asType[Flt[A]]))
    case _ =>
      error(ctx, s"abs not defined for inputs of type ${num[A].typeName}")
      error(ctx)
      bound[A]
  }

  @api def mux[A:Bits](s: Bit, a: A, b: A): A = stage(Mux(s,a,b))
}
