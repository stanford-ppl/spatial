package pir.lang

import argon._
import forge.tags._

import spatial.lang._

@ref class Word extends Top[Word] with Bits[Word] with Ref[Any,Word] {
  override protected val __neverMutable: Boolean = true
  val box: Word <:< Bits[Word] = implicitly[Word <:< Bits[Word]]

  def nWords: Int = 16 // TODO
  def wWidth: Int = 32 // TODO

  @rig def nbits: Int = nWords * wWidth
  @rig def zero: Word = Word.c(0)
  @rig def one: Word = Word.c(1)
  @rig def random(max: Option[Word]): Word = undefinedOp("random")
}

object Word {
  def c(x: Any): Word = uconst[Word](x)
}
