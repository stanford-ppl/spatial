package pcc.lang.static

import forge._
import pcc.lang.Math

trait Maths {
  @api def mux[A:Bits](s: Bit, a: A, b: A): A = Math.mux(s,a,b)
  @api def min[A:Num](a: A, b: A): A = Math.min(a,b)
  @api def max[A:Num](a: A, b: A): A = Math.max(a,b)
  @api def abs[A:Num](a: A): A = Math.abs(a)
  @api def ceil[A:Num](a: A): A = Math.ceil(a)
  @api def floor[A:Num](a: A): A = Math.floor(a)
  @api def pow[A:Num](b: A, e: A): A = Math.pow(b,e)
  @api def exp[A:Num](a: A): A = Math.exp(a)
  @api def ln[A:Num](a: A): A = Math.ln(a)
  @api def sqrt[A:Num](a: A): A = Math.sqrt(a)
  @api def sin[A:Num](a: A): A = Math.sin(a)
  @api def cos[A:Num](a: A): A = Math.cos(a)
  @api def tan[A:Num](a: A): A = Math.tan(a)
  @api def sinh[A:Num](a: A): A = Math.sinh(a)
  @api def cosh[A:Num](a: A): A = Math.cosh(a)
  @api def tanh[A:Num](a: A): A = Math.tanh(a)
  @api def asin[A:Num](a: A): A = Math.asin(a)
  @api def acos[A:Num](a: A): A = Math.acos(a)
  @api def atan[A:Num](a: A): A = Math.atan(a)
  @api def sigmoid[A:Num](a: A): A = Math.sigmoid(a)

  @api def sum[A:Num](xs: A*): A = Math.sum(xs:_*)
  @api def product[A:Num](xs: A*): A = Math.product(xs:_*)
}
