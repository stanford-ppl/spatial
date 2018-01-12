package pcc.node

import forge._
import pcc.data._
import pcc.lang._

sealed abstract class BlackBox extends Control {
  override def effects: Effects = Effects.Simple
}
@op case class GEMMBox[T:Num](
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
}


@op case class GEMVBox() extends BlackBox
@op case class CONVBox() extends BlackBox
@op case class SHIFTBox(validAfter: Int) extends BlackBox



