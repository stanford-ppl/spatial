package pcc.node.pir

import forge._
import pcc.core._
import pcc.lang._

case class Word(eid: Int) extends Bits[Word](eid) {
  override type I = Any
  def nWords: Int = 16 // TODO
  def wWidth: Int = 32 // TODO

  override def fresh(id: Int): Word = Word(id)
  override def stagedClass: Class[Word] = classOf[Word]

  override def bits: Int = nWords * wWidth

  @api def zero: Word = Word.c(0)
  @api def one: Word = Word.c(1)
}

object Word {
  implicit val tp: Word = Word(-1)
  @api def c(x: Any): Word = const[Word](x)
}
