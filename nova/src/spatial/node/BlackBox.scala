package spatial.node

import forge.tags._
import core._
import spatial.lang._

sealed abstract class BlackBox extends Control {
  override def effects: Effects = Effects.Simple
}
@op case class GEMMBox[T:Num](
  cchain: CounterChain,
  y:     SRAM[T],
  a:     SRAM[T],
  b:     SRAM[T],
  c:     T,
  alpha: T,
  beta:  T,
  i:     I32,
  j:     I32,
  mt:    I32,
  nt:    I32,
  iters: Seq[I32]
) extends BlackBox {
  override def cchains = Seq(cchain -> iters)
  override def bodies = Seq(iters -> Nil)
  override def effects: Effects = Effects.Writes(y)
}


//@op case class GEMVBox() extends BlackBox
//@op case class CONVBox() extends BlackBox
//@op case class SHIFTBox(validAfter: Int) extends BlackBox



