package pcc.node.pir

import forge._
import pcc.core._
import pcc.lang._

case class Lanes(eid: Int) extends Bits[Lanes](eid) {
  override type I = Array[Any]
  def nWords: Int = 16 // TODO
  def wWidth: Int = 32 // TODO

  override def fresh(id: Int): Lanes = Lanes(id)
  override def stagedClass: Class[Lanes] = classOf[Lanes]

  override def bits: Int = nWords * wWidth

  @api def zero: Lanes = Lanes.c(Array.fill(nWords)(0))
  @api def one: Lanes = Lanes.c(Array.fill(nWords-1)(0) :+ 1)
}

object Lanes {
  implicit val tp: Lanes = Lanes(-1)
  @api def c(values: Array[Any]): Lanes = const[Lanes](values)
}
