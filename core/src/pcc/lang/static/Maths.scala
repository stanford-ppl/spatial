package pcc.lang.static

import forge._
import pcc.lang.Math

trait Maths { this: Numerics =>
  @api def mux[A:Bits](s: Bit, a: A, b: A): A = Math.mux(s,a,b)

  @api def sum[A:Num](xs: A*): A = Math.sum(xs:_*)
  @api def product[A:Num](xs: A*): A = Math.product(xs:_*)
}
