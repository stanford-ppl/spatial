package pcc.lang

import forge._
import pcc.core._
import pcc.node._

object Math {
  @api def min[A:Num](a: A, b: A): A = typ[A] match {
    case _:Fix[_] => stage(FixMin(a,b)(a.mtyp))
    case _:Flt[_] => stage(FltMin(a,b)(a.mtyp))
    case _ =>
      error(ctx, s"min is not defined for inputs of type ${num[A].typeName}")
      error(ctx)
      bound[A]
  }
  @api def max[A:Num](a: A, b: A): A = typ[A] match {
    case _:Fix[_] => stage(FixMax(a,b)(a.mtyp))
    case _:Flt[_] => stage(FltMax(a,b)(a.mtyp))
    case _ =>
      error(ctx, s"max is not defined for inputs of type ${num[A].typeName}")
      error(ctx)
      bound[A]
  }

  @api def sigmoid[A:Num](a: A): A = a match {
    case _:Fix[_] => stage(FixSig(a)(a.mtyp))
    case _:Flt[_] => stage(FltSig(a)(a.mtyp))
    case _ =>
      error(ctx, s"sigmoid not defined for inputs of type ${num[A].typeName}")
      error(ctx)
      bound[A]
  }
  @api def exp[A:Num](a: A): A = a match {
    case _:Fix[_] => stage(FixExp(a)(a.mtyp))
    case _:Flt[_] => stage(FltExp(a)(a.mtyp))
    case _ =>
      error(ctx, s"exp not defined for inputs of type ${num[A].typeName}")
      error(ctx)
      bound[A]
  }
  @api def log[A:Num](a: A): A = a match {
    case _:Fix[_] => stage(FixLog(a)(a.mtyp))
    case _:Flt[_] => stage(FltLog(a)(a.mtyp))
    case _ =>
      error(ctx, s"log not defined for inputs of type ${num[A].typeName}")
      error(ctx)
      bound[A]
  }
  @api def sqrt[A:Num](a: A): A = a match {
    case _:Fix[_] => stage(FixSqt(a)(a.mtyp))
    case _:Flt[_] => stage(FltSqt(a)(a.mtyp))
    case _ =>
      error(ctx, s"sqrt not defined for inputs of type ${num[A].typeName}")
      error(ctx)
      bound[A]
  }
  @api def abs[A:Num](a: A): A = a match {
    case _:Fix[_] => stage(FixAbs(a)(a.mtyp))
    case _:Flt[_] => stage(FltAbs(a)(a.mtyp))
    case _ =>
      error(ctx, s"abs not defined for inputs of type ${num[A].typeName}")
      error(ctx)
      bound[A]
  }

  @api def mux[A:Bits](s: Bit, a: A, b: A): A = stage(Mux(s,a,b))
}
