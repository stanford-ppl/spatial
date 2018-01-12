package pcc.lang
package units

import forge._
import pcc.core._
import pcc.node._

object BlackBox {
  /**
    * Declares a black box for matrix multiplication with inputs a and b, output c.
    * Output is computed between [i,i+lenI), [j,j+lenJ)
    */
  @api def GEMM[T:Num](
    y: SRAM[T],
    a: SRAM[T],
    b: SRAM[T],
    c: T,
    alpha: T,
    beta: T,
    i: I32,
    j: I32,
    lenI: I32,
    lenJ: I32,
    p: I32
  ): Void = stage(GEMMBox(y,a,b,c,alpha,beta,i,j,lenI,lenJ,p))

  @api def GEMV: Void = ???
  @api def CONV: Void = ???
  @api def SHIFT(validAfter: Int): Void = ???
}
