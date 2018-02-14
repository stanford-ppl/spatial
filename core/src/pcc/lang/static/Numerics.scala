package pcc.lang.static

import forge._

trait Numerics {
  def tnum[A:Num]: Num[A] = implicitly[Num[A]]
  def mnum[A,B](num: Num[A]): Num[B] = num.asInstanceOf[Num[B]]

  def tfix[A:Fix]: Fix[A] = implicitly[Fix[A]]
  def mfix[A,B](fix: Fix[A]): Fix[B] = fix.asInstanceOf[Fix[B]]

  def tflt[A:Flt]: Flt[A] = implicitly[Flt[A]]
  def mflt[A,B](flt: Flt[A]): Flt[B] = flt.asInstanceOf[Flt[B]]

  implicit class NumOps[A:Num](a: A) {
    @api def unary_-(): A = tnum[A].neg(a)
    @api def +(b: A): A = tnum[A].add(a, b)
    @api def -(b: A): A = tnum[A].sub(a, b)
    @api def *(b: A): A = tnum[A].mul(a, b)
    @api def /(b: A): A = tnum[A].div(a, b)
  }
}
