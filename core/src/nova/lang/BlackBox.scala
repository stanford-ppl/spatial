package nova.lang

import forge.tags._
import nova.core._
import nova.node._

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
    k: I32, // common dimension length
    mt: I32,
    nt: I32
  ): Void = {
    val PP: I32 = 1 (1 -> 16)
    val ctrP = 1 until k par PP
    val cchain = CounterChain(ctrP)
    val iters = Seq(bound[I32])
    stage(GEMMBox(cchain,y,a,b,c,alpha,beta,i,j,mt,nt,iters))
  }

  @api def GEMV: Void = ???
  @api def CONV: Void = ???
  @api def SHIFT(validAfter: Int): Void = ???
}
