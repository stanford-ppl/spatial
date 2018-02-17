package pcc.lang.pir

import forge._
import pcc.core._
import pcc.lang._

case class Lanes() extends Bits[Lanes] {
  override type I = Array[Any]
  override def fresh: Lanes = new Lanes
  override def nBits: Int = nWords * wWidth

  def nWords: Int = 16 // TODO
  def wWidth: Int = 32 // TODO

  @rig def zero: Lanes = Lanes.c(Array.fill(nWords)(0))
  @rig def one: Lanes = Lanes.c(Array.fill(nWords-1)(0) :+ 1)
  @rig def random(max: Option[Lanes]): Lanes = undefinedOp("random")
}

object Lanes {
  implicit val tp: Lanes = (new Lanes).asType
  def c(values: Array[Any]): Lanes = const[Lanes](values)
}
