package pir.lang

import core._
import forge.tags._
import spatial.lang._

@ref class Lanes extends Bits[Lanes] with Ref[Any,Lanes] {
  val box: Lanes <:< Bits[Lanes] = implicitly[Lanes <:< Bits[Lanes]]
  override def isPrimitive: Boolean = true
  override def nbits: Int = nWords * wWidth

  def nWords: Int = 16 // TODO
  def wWidth: Int = 32 // TODO

  @rig def zero: Lanes = Lanes.c(Array.fill(nWords)(0))
  @rig def one: Lanes = Lanes.c(Array.fill(nWords-1)(0) :+ 1)
  @rig def random(max: Option[Lanes]): Lanes = undefinedOp("random")
}

object Lanes {
  def c(values: Array[Any]): Lanes = const[Lanes](values)
}
