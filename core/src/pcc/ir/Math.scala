package pcc
package ir

import forge._

object Math {
  @api def min[A:Num](a: A, b: A): A = a match {
    case x: Fix[_] => fix_min(a,b)(x.asType[Fix[A]],ctx,state)
    case x: Flt[_] => flt_min(a,b)(x.asType[Flt[A]],ctx,state)
    case _ =>
      error(ctx, s"min is not defined for inputs of type ${num[A].typeName}")
      error(ctx)
      bound[A]
  }
  @api def max[A:Num](a: A, b: A): A = a match {
    case x: Fix[_] => fix_max(a,b)(x.asType[Fix[A]],ctx,state)
    case x: Flt[_] => flt_max(a,b)(x.asType[Flt[A]],ctx,state)
    case _ =>
      error(ctx, s"max is not defined for inputs of type ${num[A].typeName}")
      error(ctx)
      bound[A]
  }

  @api def sigmoid[A:Num](a: A): A = a match {
    case x: Fix[_] => fix_sig(a)(x.asType[Fix[A]],ctx,state)
    case x: Flt[_] => flt_sig(a)(x.asType[Flt[A]],ctx,state)
    case _ =>
      error(ctx, s"sigmoid not defined for inputs of type ${num[A].typeName}")
      error(ctx)
      bound[A]
  }
  @api def exp[A:Num](a: A): A = a match {
    case x: Fix[_] => fix_exp(a)(x.asType[Fix[A]],ctx,state)
    case x: Flt[_] => flt_exp(a)(x.asType[Flt[A]],ctx,state)
    case _ =>
      error(ctx, s"exp not defined for inputs of type ${num[A].typeName}")
      error(ctx)
      bound[A]
  }
  @api def log[A:Num](a: A): A = a match {
    case x: Fix[_] => fix_exp(a)(x.asType[Fix[A]],ctx,state)
    case x: Flt[_] => flt_exp(a)(x.asType[Flt[A]],ctx,state)
    case _ =>
      error(ctx, s"log not defined for inputs of type ${num[A].typeName}")
      error(ctx)
      bound[A]
  }
  @api def sqrt[A:Num](a: A): A = a match {
    case x: Fix[_] => fix_exp(a)(x.asType[Fix[A]],ctx,state)
    case x: Flt[_] => flt_exp(a)(x.asType[Flt[A]],ctx,state)
    case _ =>
      error(ctx, s"sqrt not defined for inputs of type ${num[A].typeName}")
      error(ctx)
      bound[A]
  }
  @api def abs[A:Num](a: A): A = a match {
    case x: Fix[_] => fix_exp(a)(x.asType[Fix[A]],ctx,state)
    case x: Flt[_] => flt_exp(a)(x.asType[Flt[A]],ctx,state)
    case _ =>
      error(ctx, s"abs not defined for inputs of type ${num[A].typeName}")
      error(ctx)
      bound[A]
  }

  @api def mux[A:Bits](s: Bit, a: A, b: A): A = stage(Mux(s,a,b))

  @api def fix_min[A:Fix](a: A, b: A): A = stage(FixMin(a,b))
  @api def fix_max[A:Fix](a: A, b: A): A = stage(FixMax(a,b))
  @api def fix_sig[A:Fix](a: A): A = stage(FixSig(a))
  @api def fix_exp[A:Fix](a: A): A = stage(FixExp(a))
  @api def fix_log[A:Fix](a: A): A = stage(FixLog(a))
  @api def fix_sqt[A:Fix](a: A): A = stage(FixSqt(a))
  @api def fix_abs[A:Fix](a: A): A = stage(FixAbs(a))

  @api def flt_min[A:Flt](a: A, b: A): A = stage(FltMin(a,b))
  @api def flt_max[A:Flt](a: A, b: A): A = stage(FltMax(a,b))
  @api def flt_sig[A:Flt](a: A): A = stage(FltSig(a))
  @api def flt_exp[A:Flt](a: A): A = stage(FltExp(a))
  @api def flt_log[A:Flt](a: A): A = stage(FltLog(a))
  @api def flt_sqt[A:Flt](a: A): A = stage(FltSqt(a))
  @api def flt_abs[A:Flt](a: A): A = stage(FltAbs(a))
}

case class Mux[T:Bits](s: Bit, a: T, b: T) extends Op[T] { def mirror(f:Tx) = Math.mux(f(s),f(a),f(b)) }

case class FixMin[T:Fix](a: T, b: T) extends Op[T] { def mirror(f:Tx) = Math.fix_min(f(a),f(b)) }
case class FixMax[T:Fix](a: T, b: T) extends Op[T] { def mirror(f:Tx) = Math.fix_max(f(a),f(b)) }
case class FixSig[T:Fix](a: T) extends Op[T] { def mirror(f:Tx) = Math.fix_sig(f(a)) }
case class FixExp[T:Fix](a: T) extends Op[T] { def mirror(f:Tx) = Math.fix_exp(f(a)) }
case class FixLog[T:Fix](a: T) extends Op[T] { def mirror(f:Tx) = Math.fix_log(f(a)) }
case class FixSqt[T:Fix](a: T) extends Op[T] { def mirror(f:Tx) = Math.fix_sqt(f(a)) }
case class FixAbs[T:Fix](a: T) extends Op[T] { def mirror(f:Tx) = Math.fix_abs(f(a)) }


case class FltMin[T:Flt](a: T, b: T) extends Op[T] { def mirror(f:Tx) = Math.flt_min(f(a),f(b)) }
case class FltMax[T:Flt](a: T, b: T) extends Op[T] { def mirror(f:Tx) = Math.flt_max(f(a),f(b)) }
case class FltSig[T:Flt](a: T) extends Op[T] { def mirror(f:Tx) = Math.flt_sig(f(a)) }
case class FltExp[T:Flt](a: T) extends Op[T] { def mirror(f:Tx) = Math.flt_exp(f(a)) }
case class FltLog[T:Flt](a: T) extends Op[T] { def mirror(f:Tx) = Math.flt_log(f(a)) }
case class FltSqt[T:Flt](a: T) extends Op[T] { def mirror(f:Tx) = Math.flt_sqt(f(a)) }
case class FltAbs[T:Flt](a: T) extends Op[T] { def mirror(f:Tx) = Math.flt_abs(f(a)) }
