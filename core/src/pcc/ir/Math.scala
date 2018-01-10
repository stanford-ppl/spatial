package pcc
package ir

import forge._

object Math {
  def sigmoid[A:Num](a: A): A = (a match {
    case x: Fix[a] => fix_sig[A](a)
    case x: Flt[_] => flt_sig[A](a)
    case _ => throw new Exception(s"No implementation for type ${num[A].typeName}")
  }).asInstanceOf[A]

  @api def fix_sig[A:Fix](a: A): A = stage(FixSig(a))
  @api def fix_exp[A:Fix](a: A): A = stage(FixExp(a))
  @api def fix_log[A:Fix](a: A): A = stage(FixLog(a))
  @api def fix_sqt[A:Fix](a: A): A = stage(FixSqt(a))
  @api def fix_abs[A:Fix](a: A): A = stage(FixAbs(a))

  @api def flt_sig[A:Flt](a: A): A = stage(FltSig(a))
  @api def flt_exp[A:Flt](a: A): A = stage(FltExp(a))
  @api def flt_log[A:Flt](a: A): A = stage(FltLog(a))
  @api def flt_sqt[A:Flt](a: A): A = stage(FltSqt(a))
  @api def flt_abs[A:Flt](a: A): A = stage(FltAbs(a))
}


case class FixSig[T:Fix](a: T) extends Op[T] { def mirror(f:Tx) = Math.fix_sig(f(a)) }
case class FixExp[T:Fix](a: T) extends Op[T] { def mirror(f:Tx) = Math.fix_exp(f(a)) }
case class FixLog[T:Fix](a: T) extends Op[T] { def mirror(f:Tx) = Math.fix_log(f(a)) }
case class FixSqt[T:Fix](a: T) extends Op[T] { def mirror(f:Tx) = Math.fix_sqt(f(a)) }
case class FixAbs[T:Fix](a: T) extends Op[T] { def mirror(f:Tx) = Math.fix_abs(f(a)) }


case class FltSig[T:Flt](a: T) extends Op[T] { def mirror(f:Tx) = Math.flt_sig(f(a)) }
case class FltExp[T:Flt](a: T) extends Op[T] { def mirror(f:Tx) = Math.flt_exp(f(a)) }
case class FltLog[T:Flt](a: T) extends Op[T] { def mirror(f:Tx) = Math.flt_log(f(a)) }
case class FltSqt[T:Flt](a: T) extends Op[T] { def mirror(f:Tx) = Math.flt_sqt(f(a)) }
case class FltAbs[T:Flt](a: T) extends Op[T] { def mirror(f:Tx) = Math.flt_abs(f(a)) }
