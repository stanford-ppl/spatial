package pcc.lang

import forge._
import pcc.core._
import pcc.node._
import pcc.emul.Number

case class Vec[A:Bits](private val len: Int)(implicit val tV: Bits[Vec[A]]) extends Bits[Vec[A]] {
  val tA: Bits[A] = tbits[A]
  type AI = tA.I
  override type I = Array[AI]

  override def fresh: Vec[A] = new Vec[A](0)

  def length: Int = tp.extract.len

  override def nBits: Int = tA.nBits * length

  @rig def zero: Vec[A] = Vec.LeastLast(Seq.fill(length){ tA.zero }:_*)
  @rig def one: Vec[A] = Vec.LeastLast(Seq.fill(length-1){ tA.zero} :+ tA.one :_*)
  @rig def random(max: Option[Vec[A]]): Vec[A] = {
    if (max.isDefined && max.get.length != length) {
      error(ctx, s"Vector length mismatch. Expected $length ${plural(length,"word")}, got ${max.get.length}")
      error(ctx)
    }
    val elems = Seq.tabulate(length){i => tA.random(max.map{vec => vec(i)}) }
    Vec.LeastLast(elems:_*)
  }

  /**
    * Returns the word at index i in this vector.
    * Index 0 is always the least significant word.
    */
  @api def apply(i: Int): A = stage(VecApply(this,i))

  /**
    * Returns a new vector by slicing this vector in the given range.
    * The range must be statically known, and must have a stride of 1.
    */
  @api def apply(s: Series): Vec[A] = (s.start, s.end, s.step) match {
    case (Lit(x1),Lit(x2),Lit(c)) =>
      if (c !== 1) {
        error(ctx, "Strides for vector slice are currently unsupported.")
        error(ctx)
        Vec.empty[A]
      }
      else {
        val msb = Number.max(x1, x2).toInt
        val lsb = Number.min(x1, x2).toInt
        if (msb - lsb == 0) {
          warn(ctx, "Empty vector slice.")
          warn(ctx)
        }
        Vec.slice(this, msb, lsb)
      }
    case _ =>
      error(ctx, "Apply range for bit slicing must be statically known.")
      error(ctx)
      Vec.empty[A]
  }

  /**
    * Returns a new vector formed by the concatenation of this and that.
    */
  @api def ++(that: Vec[A]): Vec[A] = Vec.concat(Seq(this,that))

  /**
    * Returns a new vector with this vector's elements in reverse order.
    */
  @api def reverse: Vec[A] = stage(VecReverse(this)(tA, this.tp.extract))
}

object Vec {
  def tp[A:Bits](len: Int): Vec[A] = (new Vec(len)(tbits[A],null)).asType

  /**
    * Creates a little-endian vector from the given N elements
    * The first element is the most significant word (vector index N-1).
    * The last element is the least significant word (vector index of 0).
    **/
  @api def LittleEndian[A:Bits](elems: A*): Vec[A] = fromSeq(elems.reverse)

  /**
    * (Alias for LittleEndian)
    * Creates a little-endian vector from the given N elements
    * The first element is the most significant word (vector index N-1).
    * The last element is the least significant word (vector index of 0).
    **/
  @api def LeastLast[A:Bits](elems: A*): Vec[A] = fromSeq(elems.reverse)

  /**
    * Creates a big-endian vector from the given N elements.
    * The first element is the least significant word (vector index 0).
    * The last element is the most significant word (vector index of N-1).
    **/
  @api def BigEndian[A:Bits](elems: A*): Vec[A] = fromSeq(elems)

  /**
    * (Alias for BigEndian)
    * Creates a big-endian vector from the given N elements.
    * The first element is the least significant word (vector index 0).
    * The last element is the most significant word (vector index of N-1).
    **/
  @api def LeastFirst[A:Bits](elems: A*): Vec[A] = fromSeq(elems)

  /**
    * Creates an empty vector (of the given type).
    */
  @rig def empty[A:Bits]: Vec[A] = Vec.fromSeq[A](Nil)

  /**
    * Creates a vector from the concatenation of the given elements.
    */
  @rig def fromSeq[A:Bits](elems: Seq[A]): Vec[A] = {
    implicit val tV: Vec[A] = Vec.tp[A](elems.length)
    stage(VecAlloc(elems))
  }

  /**
    * Creates an element slice of the vector from [lsb,msb]
    */
  @rig def slice[A:Bits](vec: Vec[A], msw: Int, lsw: Int): Vec[A] = {
    implicit val tV: Vec[A] = Vec.tp[A](java.lang.Math.max(msw - lsw + 1, 0))
    stage(VecSlice(vec,msw,lsw))
  }

  /**
    * Creates a new vector which is the concatenation of all given vectors.
    */
  @rig def concat[A:Bits](vecs: Seq[Vec[A]]): Vec[A] = {
    implicit val tV: Vec[A] = Vec.tp[A](vecs.map(_.length).sum)
    stage(VecConcat(vecs))
  }
}
