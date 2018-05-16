package pir.lang

import argon._
import forge.tags._
import spatial.lang._

@ref class Lanes extends Bits[Lanes] with Ref[Any,Lanes] {
  val box: Lanes <:< Bits[Lanes] = implicitly[Lanes <:< Bits[Lanes]]
  override val __neverMutable: Boolean = true

  def nWords: Int = 16 // TODO
  def wWidth: Int = 32 // TODO

  @rig def nbits: Int = nWords * wWidth
  @rig def zero: Lanes = const[Lanes](Array.fill(nWords)(0))
  @rig def one: Lanes = const[Lanes](Array.fill(nWords-1)(0) :+ 1)
  @rig def random(max: Option[Lanes]): Lanes = undefinedOp("random")
}

object Lanes {

}
