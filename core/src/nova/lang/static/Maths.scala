package nova.lang.static

import forge.tags._
import nova.lang.Math

trait Maths { this: Numerics =>
  @api def mux[A:Bits](s: Bit, a: A, b: A): A = Math.mux(s,a,b)

  @api def sum[A:Num](xs: A*): A = Math.sum(xs:_*)
  @api def product[A:Num](xs: A*): A = Math.product(xs:_*)
}
