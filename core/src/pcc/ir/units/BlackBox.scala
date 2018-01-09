package pcc
package ir
package units

import forge._
import pcc.data.Effects
import pcc.ir.memories.SRAM

sealed abstract class BlackBox extends Op[Void] {
  override def effects: Effects = Effects.Simple
}
case class GEMMBox[T](
  a: SRAM[T],
  b: SRAM[T],
  c: SRAM[T],
  i: I32,
  j: I32,
  p: I32
) extends BlackBox


case class GEMVBox() extends BlackBox
case class CONVBox() extends BlackBox
case class SHIFTBox(validAfter: Int) extends BlackBox // Shift out registers, valid after X cycles

object BlackBox {
  @api def GEMM[T:Num](
    a: SRAM[T],
    b: SRAM[T],
    c: SRAM[T],
    i: I32,
    j: I32,
    p: I32
  ): Void = stage(GEMMBox(a,b,c,i,j,p))

  @api def GEMV: Void = stage(GEMVBox())
  @api def CONV: Void = stage(CONVBox())
  @api def SHIFT(validAfter: Int): Void = stage(SHIFTBox(validAfter))
}

