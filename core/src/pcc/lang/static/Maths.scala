package pcc.lang.static

import forge._
import pcc.lang.Math

trait Maths {
  @api def min[A:Num](a: A, b: A): A = Math.min(a,b)
  @api def max[A:Num](a: A, b: A): A = Math.max(a,b)
  @api def mux[A:Bits](s: Bit, a: A, b: A): A = Math.mux(s,a,b)

  @api def sigmoid[A:Num](a: A): A = Math.sigmoid(a)
  @api def exp[A:Num](a: A): A = Math.exp(a)
  @api def log[A:Num](a: A): A = Math.log(a)
  @api def sqrt[A:Num](a: A): A = Math.sqrt(a)
  @api def abs[A:Num](a: A): A = Math.abs(a)
}
