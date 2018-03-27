package spatial.lang

import argon._
import forge.tags._

@ref class Tup2[A:Type,B:Type] extends Struct[Tup2[A,B]] with Arith[Tup2[A,B]] with Bits[Tup2[A,B]] with Ref[Tuple2[Any,Any],Tup2[A,B]] {
  override val box = implicitly[Tup2[A,B] <:< (Bits[Tup2[A,B]] with Arith[Tup2[A,B]] with Struct[Tup2[A,B]])]
  val A: Type[A] = Type[A]
  val B: Type[B] = Type[B]
  @rig private def arith(op: String)(func: (Arith[A],Arith[B]) => Tup2[A,B]): Tup2[A,B] = (A.getView[Arith],B.getView[Arith]) match {
    case (Some(a),Some(b)) => func(a,b)
    case _ =>
      error(ctx, s"$op is not defined for ${this.tp}")
      error(ctx)
      err[Tup2[A,B]](s"$op is not defined for ${this.tp}")
  }
  @rig private def bits(op: String)(func: (Bits[A],Bits[B]) => Tup2[A,B]): Tup2[A,B] = (A.getView[Bits],B.getView[Bits]) match {
    case (Some(a),Some(b)) => func(a,b)
    case _ =>
      error(ctx, s"$op is not defined for ${this.tp}")
      error(ctx)
      err[Tup2[A,B]](s"$op is not defined for ${this.tp}")
  }

  def fields = Seq(
    "_1" -> Type[A],
    "_2" -> Type[B]
  )
  @api def _1: A = field[A]("_1")
  @api def _2: B = field[B]("_2")

  @api def unary_-(): Tup2[A,B] = arith("negate"){(a,b) => Tup2(a.neg(_1),b.neg(_2)) }
  @api def +(that: Tup2[A, B]): Tup2[A, B] = arith("+"){(a,b) => Tup2(a.add(this._1,that._1),b.add(this._2,that._2)) }
  @api def -(that: Tup2[A, B]): Tup2[A, B] = arith("-"){(a,b) => Tup2(a.sub(this._1,that._1),b.sub(this._2,that._2)) }
  @api def *(that: Tup2[A, B]): Tup2[A, B] = arith("*"){(a,b) => Tup2(a.mul(this._1,that._1),b.mul(this._2,that._2)) }
  @api def /(that: Tup2[A, B]): Tup2[A, B] = arith("/"){(a,b) => Tup2(a.div(this._1,that._1),b.div(this._2,that._2)) }
  @api def %(that: Tup2[A, B]): Tup2[A, B] = arith("%"){(a,b) => Tup2(a.mod(this._1,that._1),b.mod(this._2,that._2)) }

  @api def abs(a: Tup2[A, B]): Tup2[A, B] = arith("abs"){(a,b) => Tup2(a.abs(this._1),b.abs(this._2)) }
  @api def ceil(a: Tup2[A, B]): Tup2[A, B] = arith("ceil"){(a,b) => Tup2(a.ceil(this._1), b.ceil(this._2)) }
  @api def floor(a: Tup2[A, B]): Tup2[A, B] = arith("floor"){(a,b) => Tup2(a.floor(this._1), b.floor(this._2)) }

  override def nbits: Int = (A.getView[Bits],B.getView[Bits]) match {
    case (Some(a),Some(b)) => a.nbits + b.nbits
    case _ => fatal(s"${this.tp} is not a bit-based type.")
  }

  @api def zero: Tup2[A, B] = bits("zero"){(a,b) => Tup2(a.zero, b.zero) }
  @api def one: Tup2[A, B] = bits("one"){(a,b) => Tup2(a.one, b.one) }
  @api def random(max: Option[Tup2[A, B]]): Tup2[A, B] = bits("random"){(a,b) =>
    Tup2(a.random(max.map(_._1)), b.random(max.map(_._2)))
  }
}

object Tup2 {
  @api def apply[A:Type,B:Type](a: A, b: B): Tup2[A,B] = Struct[Tup2[A,B]]("_1" -> a, "_2" -> b)
}