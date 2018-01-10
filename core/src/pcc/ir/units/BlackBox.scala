package pcc
package ir
package units

import forge._
import pcc.data.Effects
import pcc.ir.memories.SRAM

sealed abstract class BlackBox extends Control {
  override def effects: Effects = Effects.Simple
}
case class GEMMBox[T:Num](
  y:     SRAM[T],
  a:     SRAM[T],
  b:     SRAM[T],
  c:     T,
  alpha: T,
  beta:  T,
  i:     I32,
  j:     I32,
  lenI:  I32,
  lenJ:  I32,
  p:     I32
) extends BlackBox {
  override def effects: Effects = Effects.Writes(y)
  def mirror(f:Tx) = BlackBox.GEMM(f(y),f(a),f(b),f(c),f(alpha),f(beta),f(i),f(j),f(lenI),f(lenJ),f(p))
}


case class GEMVBox() extends BlackBox { def mirror(f:Tx) = stage(this) }
case class CONVBox() extends BlackBox { def mirror(f:Tx) = stage(this) }
case class SHIFTBox(validAfter: Int) extends BlackBox {
  def mirror(f:Tx) = stage(this)
}

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

  @api def GEMV: Void = stage(GEMVBox())
  @api def CONV: Void = stage(CONVBox())
  @api def SHIFT(validAfter: Int): Void = stage(SHIFTBox(validAfter))
}

