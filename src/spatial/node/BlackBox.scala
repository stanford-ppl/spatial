package spatial.node

import argon._
import forge.tags._

import spatial.lang._

abstract class BlackBox[R:Type] extends Control[R]

/** Black box which must be expanded early in compiler (after initial analyses). */
abstract class EarlyBlackBox[R:Type] extends BlackBox[R] {
  override def cchains = Nil
  override def iters = Nil
  override def bodies = Nil
  @rig def lower(): R
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
) extends BlackBox[Void] {
  override def cchains = Seq(cchain -> iters)
  override def bodies = Seq(iters -> Nil)
  override def effects: Effects = Effects.Writes(y)
}


//@op case class GEMVBox() extends BlackBox
//@op case class CONVBox() extends BlackBox
//@op case class SHIFTBox(validAfter: Int) extends BlackBox



