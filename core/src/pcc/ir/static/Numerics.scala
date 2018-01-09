package pcc.ir.static

import forge._

trait Numerics {
  def num[T:Num]: Num[T] = implicitly[Num[T]]

  implicit class NumOps[A:Num](a: A) {
    @api def +(b: A): A = num[A].add(a, b)
    @api def -(b: A): A = num[A].sub(a, b)
    @api def *(b: A): A = num[A].mul(a, b)
    @api def /(b: A): A = num[A].div(a, b)
  }
}
