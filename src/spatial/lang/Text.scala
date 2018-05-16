package spatial.lang

import argon._
import forge.tags._

import spatial.node._

@ref class Text extends Top[Text] with Ref[String,Text] {
  // --- Infix methods
  @api override def neql(that: Text): Bit = stage(TextNeq(this,that))
  @api override def eql(that: Text): Bit = stage(TextEql(this,that))

  @api override def toText: Text = this
  @api def +(that: Any): Text = that match {
    case b: Top[_] => this ++ b.toText
    case b         => this ++ Text(b.toString)
  }

  @api def length: I32 = stage(TextLength(this))
  @api def apply(i: I32): U8 = stage(TextApply(this, i))
  @api def slice(start: I32, end: I32): Text = stage(TextSlice(this, start, end))

  @api def map[R:Type](f: U8 => R): Tensor1[R] = Tensor1.tabulate(this.length){i => f(this(i)) }

  @api def toCharArray: Tensor1[U8] = this.map{c => c}

  // --- Typeclass Methods
  override protected def value(c: Any): Option[(String, Boolean)] = c match {
    case s: String=> Some((s,true))
    case _ => super.value(c)
  }

  override protected val __neverMutable: Boolean = true
}
object Text {
  def apply(x: String): Text = uconst[Text](x)

  @rig def concat(a: Text, b: Text): Text = stage(TextConcat(Seq(a,b)))
}
