package spatial.lang

import argon._
import forge.tags._
import forge.VarLike
import utils.implicits.Readable._

import spatial.node.GenericToText

abstract class Top[A](implicit ev: A <:< Ref[Any,A]) extends Ref[Any,A] { self =>

  @rig protected def unrelated(that: Any): Unit = that match {
    case t: Ref[_,_] =>
      warn(ctx, r"Comparison between unrelated types ${this.tp} and ${t.tp} - will always return false.")
      warn(ctx)
    case t =>
      warn(ctx, r"Comparison between unrelated types ${this.tp} and ${t.getClass} - will always return false.")
      warn(ctx)
  }

  @api def infix_!=(that: Any): Bit = this !== that
  @api def infix_==(that: Any): Bit = this === that

  @api def !==(that: Any): Bit = this match {
    case v: VarLike[_] => v.__read match {
      case v: Top[_] => v !== that
      case _ => !v.equals(that)
    }
    case v => that match {
      case t: Top[_] if t.tp =:= this.tp => v.neql(t.asInstanceOf[A])
      case _ =>
        // (Silently) Attempt to create a constant from the rhs
        // Fall back to unstaged comparison if this fails.
        if (this.tp.canConvertFrom(that)) {
          v.neql(this.tp.from(that))
        }
        else {
          unrelated(that)
          !this.equals(that)
        }
    }
  }
  @api def ===(that: Any): Bit = this match {
    case v: VarLike[_] => v.__read match {
      case v: Top[_] => v === that
      case _         => v.equals(that)
    }
    case v => that match {
      case t: Top[_] if t.tp =:= this.tp => v.eql(t.asInstanceOf[A])
      case _ =>
        // (Silently) Attempt to create a constant from the rhs
        // Fall back to unstaged comparison if this fails.
        if (this.tp.canConvertFrom(that)) {
          v.eql(this.tp.from(that))
        }
        else {
          unrelated(that)
          this.equals(that)
        }
    }
  }

  @api def neql(that: A): Bit = !this.equals(that)
  @api def eql(that: A): Bit = this.equals(that)


  @api def ++(that: Any): Text = that match {
    case t: Text   => Text.concat(this.toText, t)
    case t: Top[_] => Text.concat(this.toText, t.toText)
    case t => Text.concat(this.toText, Text(t.toString))
  }

  @api def toText: Text = stage(GenericToText(me)(this.selfType))
}
