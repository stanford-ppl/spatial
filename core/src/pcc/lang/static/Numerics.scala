package pcc.lang.static

import forge._

trait Numerics {
  def tnum[A:Num]: Num[A] = implicitly[Num[A]]
  def mnum[A,B](num: Num[A]): Num[B] = num.asInstanceOf[Num[B]]

  def tfix[A:Fix]: Fix[A] = implicitly[Fix[A]]
  def mfix[A,B](fix: Fix[A]): Fix[B] = fix.asInstanceOf[Fix[B]]

  def tflt[A:Flt]: Flt[A] = implicitly[Flt[A]]
  def mflt[A,B](flt: Flt[A]): Flt[B] = flt.asInstanceOf[Flt[B]]

  @api def min[A:Num](a: A, b: A): A = tnum[A].min(a,b)
  @api def max[A:Num](a: A, b: A): A = tnum[A].max(a,b)
  @api def abs[A:Num](a: A): A = tnum[A].abs(a)
  @api def ceil[A:Num](a: A): A = tnum[A].ceil(a)
  @api def floor[A:Num](a: A): A = tnum[A].floor(a)
  @api def pow[A:Num](b: A, e: A): A = tnum[A].pow(b,e)
  @api def exp[A:Num](a: A): A = tnum[A].exp(a)
  @api def ln[A:Num](a: A): A = tnum[A].ln(a)
  @api def sqrt[A:Num](a: A): A = tnum[A].sqrt(a)
  @api def sin[A:Num](a: A): A = tnum[A].sin(a)
  @api def cos[A:Num](a: A): A = tnum[A].cos(a)
  @api def tan[A:Num](a: A): A = tnum[A].tan(a)
  @api def sinh[A:Num](a: A): A = tnum[A].sinh(a)
  @api def cosh[A:Num](a: A): A = tnum[A].cosh(a)
  @api def tanh[A:Num](a: A): A = tnum[A].tanh(a)
  @api def asin[A:Num](a: A): A = tnum[A].asin(a)
  @api def acos[A:Num](a: A): A = tnum[A].acos(a)
  @api def atan[A:Num](a: A): A = tnum[A].atan(a)
  @api def sigmoid[A:Num](a: A): A = tnum[A].sigmoid(a)

  implicit class NumOps[A:Num](a: A) {
    @api def unary_-(): A = tnum[A].neg(a)
    @api def +(b: A): A = tnum[A].add(a, b)
    @api def -(b: A): A = tnum[A].sub(a, b)
    @api def *(b: A): A = tnum[A].mul(a, b)
    @api def /(b: A): A = tnum[A].div(a, b)
  }
}
