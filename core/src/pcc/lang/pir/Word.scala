package pcc.lang.pir

import forge._
import pcc.core._
import pcc.lang._

case class Word() extends Bits[Word] {
  override type I = Any
  override def fresh: Word = new Word
  override def nBits: Int = nWords * wWidth

  def nWords: Int = 16 // TODO
  def wWidth: Int = 32 // TODO

  @rig def zero: Word = Word.c(0)
  @rig def one: Word = Word.c(1)
  @rig def random(max: Option[Word]): Word = undefinedOp("random")
}

object Word {
  implicit val tp: Word = (new Word).asType
  def c(x: Any): Word = const[Word](x)
}
