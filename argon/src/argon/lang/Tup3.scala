package argon.lang

import argon._
import forge.tags._

@ref class Tup3[A:Type,B:Type,C:Type] extends Struct[Tup3[A,B,C]] with Arith[Tup3[A,B,C]] with Bits[Tup3[A,B,C]] with Ref[Tuple3[Any,Any,Any],Tup3[A,B,C]] {
  override val box = implicitly[Tup3[A,B,C] <:< (Bits[Tup3[A,B,C]] with Arith[Tup3[A,B,C]] with Struct[Tup3[A,B,C]])]
  val A: Type[A] = Type[A]
  val B: Type[B] = Type[B]
  val C: Type[C] = Type[C]
  @rig private def arith(op: String)(func: (Arith[A],Arith[B],Arith[C]) => Tup3[A,B,C]): Tup3[A,B,C] = (A.getView[Arith],B.getView[Arith],C.getView[Arith]) match {
    case (Some(a),Some(b),Some(c)) => func(a,b,c)
    case _ =>
      error(ctx, s"$op is not defined for ${this.tp}")
      error(ctx)
      err[Tup3[A,B,C]](s"$op is not defined for ${this.tp}")
  }
  @rig private def bits(op: String)(func: (Bits[A],Bits[B],Bits[C]) => Tup3[A,B,C]): Tup3[A,B,C] = (A.getView[Bits],B.getView[Bits],C.getView[Bits]) match {
    case (Some(a),Some(b),Some(c)) => func(a,b,c)
    case _ =>
      error(ctx, s"$op is not defined for ${this.tp}")
      error(ctx)
      err[Tup3[A,B,C]](s"$op is not defined for ${this.tp}")
  }

  def fields = Seq(
    "_1" -> Type[A],
    "_2" -> Type[B],
    "_3" -> Type[C]
  )
  @api def _1: A = field[A]("_1")
  @api def _2: B = field[B]("_2")
  @api def _3: C = field[C]("_3")

  @api def unary_-(): Tup3[A,B,C] = arith("negate"){(a,b,c) => Tup3(a.neg(_1),b.neg(_2),c.neg(_3)) }
  @api def +(that: Tup3[A,B,C]): Tup3[A,B,C] = arith("+"){(a,b,c) => Tup3(a.add(this._1,that._1),b.add(this._2,that._2),c.add(this._3,that._3)) }
  @api def -(that: Tup3[A,B,C]): Tup3[A,B,C] = arith("-"){(a,b,c) => Tup3(a.sub(this._1,that._1),b.sub(this._2,that._2),c.sub(this._3,that._3)) }
  @api def *(that: Tup3[A,B,C]): Tup3[A,B,C] = arith("*"){(a,b,c) => Tup3(a.mul(this._1,that._1),b.mul(this._2,that._2),c.mul(this._3,that._3)) }
  @api def /(that: Tup3[A,B,C]): Tup3[A,B,C] = arith("/"){(a,b,c) => Tup3(a.div(this._1,that._1),b.div(this._2,that._2),c.div(this._3,that._3)) }
  @api def %(that: Tup3[A,B,C]): Tup3[A,B,C] = arith("%"){(a,b,c) => Tup3(a.mod(this._1,that._1),b.mod(this._2,that._2),c.mod(this._3,that._3)) }

  @api def abs(a:   Tup3[A,B,C]): Tup3[A,B,C] = arith("abs"){(a,b,c)   => Tup3(a.abs(this._1),   b.abs(this._2),   c.abs(this._3))   }
  @api def ceil(a:  Tup3[A,B,C]): Tup3[A,B,C] = arith("ceil"){(a,b,c)  => Tup3(a.ceil(this._1),  b.ceil(this._2),  c.ceil(this._3))  }
  @api def floor(a: Tup3[A,B,C]): Tup3[A,B,C] = arith("floor"){(a,b,c) => Tup3(a.floor(this._1), b.floor(this._2), c.floor(this._3)) }

  @rig def nbits: Int = (A.getView[Bits],B.getView[Bits],C.getView[Bits]) match {
    case (Some(a),Some(b),Some(c)) => a.nbits + b.nbits + c.nbits
    case _ =>
      error(this.ctx, s"${this.tp} is not a bit-based type.")
      error(this.ctx)
      0
  }
  @api def zero: Tup3[A, B, C] = bits("zero"){(a,b,c) => Tup3(a.zero, b.zero, c.zero) }
  @api def one: Tup3[A, B, C] = bits("one"){(a,b,c) => Tup3(a.one, b.one, c.one) }
  @api def random(max: Option[Tup3[A, B, C]]): Tup3[A, B, C] = bits("random"){(a,b,c) =>
    Tup3(a.random(max.map(_._1)), b.random(max.map(_._2)), c.random(max.map(_._3)))
  }
}

object Tup3 {
  import argon.lang.implicits._

  @api def apply[A:Type,B:Type,C:Type](a: A, b: B, c: C): Tup3[A,B,C] = Struct[Tup3[A,B,C]]("_1" -> a, "_2" -> b, "_3" -> c)
}
