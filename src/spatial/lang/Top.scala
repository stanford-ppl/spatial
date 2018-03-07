package spatial.lang

import core._
import forge.tags._
import forge.VarLike
import utils.implicits.Readable._

import spatial.node.GenericToText

abstract class Top[A](implicit ev: A <:< Ref[Any,A]) extends Ref[Any,A] { self =>

  @rig protected def unrelated(that: Any): Unit = that match {
    case t: Ref[_,_] =>
      warn(this.ctx, r"Comparison between unrelated types ${this.tp} and ${t.tp}")
      warn(this.ctx)
    case t =>
      warn(this.ctx, r"Comparison between unrelated types ${this.tp} and ${t.getClass}")
      warn(this.ctx)
  }

  @api def !==(that: Any): Bit = this match {
    case v: VarLike[_] => v.__read match {
      case v: Top[_] => v !== that
      case _ => !v.equals(that)
    }
    case v => that match {
      case t: Top[_] if t.tp =:= this.tp => v.neql(t.asInstanceOf[A])
      case _ => unrelated(that); !this.equals(that)
    }
  }
  @api def ===(that: Any): Bit = this match {
    case v: VarLike[_] => v.__read match {
      case v: Top[_] => v === that
      case _         => v.equals(that)
    }
    case v => that match {
      case t: Top[_] if t.tp =:= this.tp => v.eql(t.asInstanceOf[A])
      case _ => unrelated(that); this.equals(that)
    }
  }

  @api def neql(that: A): Bit = !this.equals(that)
  @api def eql(that: A): Bit = this.equals(that)


  @api def ++(that: Any): Text = that match {
    case t: Text   => Text.concat(this.toText, t)
    case t: Top[_] => Text.concat(this.toText, t.toText)
    case t => Text.concat(this.toText, Text.c(t.toString))
  }

  @api def toText: Text = stage(GenericToText(me)(this.selfType))
}
