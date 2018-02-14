package pcc.lang

import forge._
import pcc.core._
import pcc.node._

object Math {
  @api def mux[A:Bits](s: Bit, a: A, b: A): A = stage(Mux(s,a,b))

  @api def min[A:Num](a: A, b: A): A = num[A] match {
    case t:Fix[_] => stage(FixMin(a,b)(mfix(t)))
    case t:Flt[_] => stage(FltMin(a,b)(mflt(t)))
    case _ => undefinedNumeric[A]("min")
  }
  @api def max[A:Num](a: A, b: A): A = num[A] match {
    case t:Fix[_] => stage(FixMax(a,b)(mfix(t)))
    case t:Flt[_] => stage(FltMax(a,b)(mflt(t)))
    case _ => undefinedNumeric[A]("max")
  }
  @api def abs[A:Num](a: A): A = num[A] match {
    case t:Fix[_] => stage(FixAbs(a)(mfix(t)))
    case t:Flt[_] => stage(FltAbs(a)(mflt(t)))
    case _ => undefinedNumeric[A]("abs")
  }
  @api def ceil[A:Num](a: A): A = num[A] match {
    case t:Fix[_] => stage(FixCeil(a)(mfix(t)))
    case t:Flt[_] => stage(FltCeil(a)(mflt(t)))
    case _ => undefinedNumeric[A]("ceil")
  }
  @api def floor[A:Num](a: A): A = num[A] match {
    case t:Fix[_] => stage(FixFloor(a)(mfix(t)))
    case t:Flt[_] => stage(FltFloor(a)(mflt(t)))
    case _ => undefinedNumeric[A]("floor")
  }
  @api def pow[A:Num](b: A, e: A): A = num[A] match {
    case t:Fix[_] => stage(FixPow(b,e)(mfix(t)))
    case t:Flt[_] => stage(FltPow(b,e)(mflt(t)))
    case _ => undefinedNumeric[A]("exp")
  }
  @api def exp[A:Num](a: A): A = num[A] match {
    case t:Fix[_] => stage(FixExp(a)(mfix(t)))
    case t:Flt[_] => stage(FltExp(a)(mflt(t)))
    case _ => undefinedNumeric[A]("exp")
  }
  @api def ln[A:Num](a: A): A = num[A] match {
    case t:Fix[_] => stage(FixLn(a)(mfix(t)))
    case t:Flt[_] => stage(FltLn(a)(mflt(t)))
    case _ => undefinedNumeric[A]("log")
  }
  @api def sqrt[A:Num](a: A): A = num[A] match {
    case t:Fix[_] => stage(FixSqrt(a)(mfix(t)))
    case t:Flt[_] => stage(FltSqrt(a)(mflt(t)))
    case _ => undefinedNumeric[A]("sqrt")
  }
  @api def sin[A:Num](a: A): A = num[A] match {
    case t:Fix[_] => stage(FixSin(a)(mfix(t)))
    case t:Flt[_] => stage(FltSin(a)(mflt(t)))
    case _ => undefinedNumeric[A]("sin")
  }
  @api def cos[A:Num](a: A): A = num[A] match {
    case t:Fix[_] => stage(FixCos(a)(mfix(t)))
    case t:Flt[_] => stage(FltCos(a)(mflt(t)))
    case _ => undefinedNumeric[A]("cos")
  }
  @api def tan[A:Num](a: A): A = num[A] match {
    case t:Fix[_] => stage(FixTan(a)(mfix(t)))
    case t:Flt[_] => stage(FltTan(a)(mflt(t)))
    case _ => undefinedNumeric[A]("tan")
  }
  @api def sinh[A:Num](a: A): A = num[A] match {
    case t:Fix[_] => stage(FixSinh(a)(mfix(t)))
    case t:Flt[_] => stage(FltSinh(a)(mflt(t)))
    case _ => undefinedNumeric[A]("sinh")
  }
  @api def cosh[A:Num](a: A): A = num[A] match {
    case t:Fix[_] => stage(FixCosh(a)(mfix(t)))
    case t:Flt[_] => stage(FltCosh(a)(mflt(t)))
    case _ => undefinedNumeric[A]("cosh")
  }
  @api def tanh[A:Num](a: A): A = num[A] match {
    case t:Fix[_] => stage(FixTanh(a)(mfix(t)))
    case t:Flt[_] => stage(FltTanh(a)(mflt(t)))
    case _ => undefinedNumeric[A]("tanh")
  }
  @api def asin[A:Num](a: A): A = num[A] match {
    case t:Fix[_] => stage(FixAsin(a)(mfix(t)))
    case t:Flt[_] => stage(FltAsin(a)(mflt(t)))
    case _ => undefinedNumeric[A]("asin")
  }
  @api def acos[A:Num](a: A): A = num[A] match {
    case t:Fix[_] => stage(FixAcos(a)(mfix(t)))
    case t:Flt[_] => stage(FltAcos(a)(mflt(t)))
    case _ => undefinedNumeric[A]("acos")
  }
  @api def atan[A:Num](a: A): A = num[A] match {
    case t:Fix[_] => stage(FixAtan(a)(mfix(t)))
    case t:Flt[_] => stage(FltAtan(a)(mflt(t)))
    case _ => undefinedNumeric[A]("atan")
  }
  @api def sigmoid[A:Num](a: A): A = num[A] match {
    case t:Fix[_] => stage(FixSigmoid(a)(mfix(t)))
    case t:Flt[_] => stage(FltSigmoid(a)(mflt(t)))
    case _ => undefinedNumeric[A]("sigmoid")
  }

  @api def sum[T:Num](xs: T*): T = if (xs.isEmpty) num[T].zero else reduce(xs:_*){_+_}
  @api def product[T:Num](xs: T*): T = if (xs.isEmpty) num[T].one else reduce(xs:_*){_*_}

  @api def reduce[T](xs: T*)(reduce: (T,T) => T): T = reduceTreeLevel(xs, reduce).head

  @rig private def reduceTreeLevel[T](xs: Seq[T], reduce: (T,T) => T): Seq[T] = xs.length match {
    case 0 => throw new Exception("Empty reduction level")
    case 1 => xs
    case len if len % 2 == 0 => reduceTreeLevel(List.tabulate(len/2){i => reduce( xs(2*i), xs(2*i+1)) }, reduce)
    case len => reduceTreeLevel(List.tabulate(len/2){i => reduce( xs(2*i), xs(2*i+1)) } :+ xs.last, reduce)
  }

  @rig def undefinedNumeric[A:Num](op: String): A = {
    error(ctx, s"$op is not defined for inputs of type ${num[A].typeName}")
    error(ctx)
    bound[A]
  }
}
