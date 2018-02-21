package spatial.lang

import core._
import forge.tags._

abstract class Ref[A](implicit ev: A <:< Ref[A]) extends Top[A] {
  @rig private def unrelated(that: Any): Unit = that match {
    case t: Top[_] =>
      warn(ctx, s"Comparison between unrelated types ${this.tp} and ${t.tp}")
      warn(ctx)
    case t =>
      warn(ctx, s"Comparison between unrelated types ${this.tp} and ${t.getClass}")
      warn(ctx)
  }

  @rig def nEql(that: Any): Bit = that match {
    case t: Top[_] if t.tp <:> this.tp => this !== that.asInstanceOf[A]
    case _ => unrelated(that); true
  }
  @rig def isEql(that: Any): Bit = that match {
    case t: Top[_] if t.tp <:> this.tp => this === that.asInstanceOf[A]
    case _ => unrelated(that); false
  }

  @api def !==(that: A): Bit = !this.equals(that)  // FIXME: Correct default?
  @api def ===(that: A): Bit = this.equals(that)   // FIXME: Correct default?

  @api def ++(that: Any): Text = that match {
    case t: Text   => Text.concat(this.toText, t)
    case t: Ref[_] => Text.concat(this.toText, t.toText)
    case t => Text.concat(this.toText, Text.c(t.toString))
  }
  @api def toText: Text = Text.textify(me)(this.tp,ctx,state)
}
