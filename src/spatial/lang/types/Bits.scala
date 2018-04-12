package spatial.lang
package types

import argon._
import forge.tags._

import spatial.node._
import spatial.util._

trait Bits[A] extends Top[A] with Ref[Any,A] {
  def box: A <:< Bits[A]
  private implicit def evv: A <:< Bits[A] = box
  private implicit def A: Bits[A] = this.selfType

  // --- Infix Methods
  /**
    * Returns the given bit in this value.
    * 0 corresponds to the least significant bit (LSB).
    */
  @api def bit(i: Int): Bit = this.asBits.apply(i)

  /**
    * Returns a slice of the bits in this word as a VectorN.
    * The range must be statically determinable with a stride of 1.
    * The range is inclusive for both the start and end.
    * The range can be big endian (e.g. ``3::0``) or little endian (e.g. ``0::3``).
    * In both cases, element 0 is always the least significant element.
    *
    * For example, ``x.bits(3::0)`` returns a Vector of the 4 least significant bits of ``x``.
    */
  @api def bits(range: Series[I32]): Vec[Bit] = this.asBits.apply(range)

  /**
    * Gives a view of this value's bits as the given type, without conversion.
    * If B has fewer bits than this value's type, the most significant bits will be dropped.
    * If B has more bits than this value's type, the most significant bits will be zeros.
    */
  @api def as[B:Bits]: B = {
    Bits.checkMismatch(Bits[A], Bits[B], "asUnchecked")
    this.asUnchecked[B]
  }

  @rig def asType[B](tp: Bits[B]): B = {
    implicit val B: Bits[B] = tp
    this.as[B]
  }

  /**
    * Gives a view of this value's bits as the given type, without conversion.
    * Bit length mismatch warnings are suppressed.
    * If B has fewer bits than this value's type, the most significant bits will be dropped.
    * If B has more bits than this value's type, the most significant bits will be zeros.
    */
  @api def asUnchecked[B:Bits]: B = this.asBits.recastUnchecked[B]

  /**
    * Returns a value of the same type with this value's bits in reverse order.
    */
  @api def reverseBits: A = this.asBits.reverse.recastUnchecked[A]

  /**
    * Gives a view of this value as a vector of bits (without any conversion).
    */
  @api def asBits: Vec[Bit] = this match {
    case struct: Struct[_] =>
      if (!struct.fields.forall(_._2.isBits)) {
        error(ctx, s"asBits is not available for type ${this.tp}")
        error(ctx)
        implicit val vT: Vec[Bit] = Vec.bits[Bit](0)
        err[Vec[Bit]](s"asBits not available for type ${this.tp}")
      }
      else {
        val fields = struct.fieldMap.collect{case (_, sym: Bits[_]) => sym.asBits }
        Vec.concat(fields)
      }
    case _ =>
      implicit val tV: Vec[Bit] = Vec.bits[Bit](Bits[A].nbits)
      stage(DataAsBits(this))
  }

  // --- Typeclass Methods
  def nbits: Int
  @rig def zero: A
  @rig def one: A
  @rig def random(max: Option[A]): A

  /**
    * Returns an unimplemented error for the given op.
    */
  @rig def undefinedOp(op: String): A = {
    error(ctx, s"$op is not defined for inputs of type $this")
    error(ctx)
    bound[A]
  }
}

object Bits {
  def apply[A:Bits]: Bits[A] = {
    try { implicitly[Bits[A]] }
    catch {case t:Throwable =>
      fatal("Could not get bit evidence")
      throw t
    }
  }
  def m[A,B](n: Bits[A]): Bits[B] = n.asInstanceOf[Bits[B]]

  def unapply[A](x: ExpType[_,A]): Option[Bits[A]] = x match {
    case b: Bits[_] => Some(b.asInstanceOf[Bits[A]])
    case _ => None
  }

  @rig def checkMismatch[A,B](tA: Bits[A], tB: Bits[B], op: String): Unit = {
    val lenA = tA.nbits
    val lenB = tB.nbits
    if (lenA != lenB)
      warn(ctx, s"Bit length mismatch in conversion between $tA and $tB.")

    if (lenA < lenB) {
      warn(s"Most significant bits ($lenB::$lenA) will be set to zero in result.")
      warn(s"Use the $op method to suppress this warning.")
      warn(ctx)
    }
    else if (lenA > lenB) {
      warn(s"Most significant bits ($lenA::$lenB) will be dropped in result.")
      warn(s"Use the $op method to suppress this warning.")
      warn(ctx)
    }
  }
}
