package spatial.node

import core._
import forge.tags._

import spatial.lang._

sealed abstract class BlackBox extends Control[Void] {
  override def effects: Effects = Effects.Simple
}
@op case class GEMMBox[T:Num](
  cchain: CounterChain,
  y:     SRAM2[T],
  a:     SRAM2[T],
  b:     SRAM2[T],
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



