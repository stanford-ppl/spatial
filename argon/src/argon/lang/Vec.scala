package argon.lang

import argon._
import emul.{FixedPoint, Number}
import forge.tags._
import utils.math.ReduceTree
import utils.plural

import argon.node._

@ref class Vec[A:Bits](val width: Int) extends Top[Vec[A]] with Ref[Array[Any],Vec[A]] with Arith[Vec[A]] with Bits[Vec[A]] {
  val A: Bits[A] = Bits[A]
  val aA: Option[Arith[A]] = A.getView[Arith]
  override val box: Vec[A] <:< (Arith[Vec[A]] with Bits[Vec[A]]) = implicitly[Vec[A] <:< (Arith[Vec[A]] with Bits[Vec[A]])]
  private implicit val evv: A <:< Bits[A] = A.box

  // TODO[4]: These are all quite expensive for large vectors
  @api def elems: List[A] = List.tabulate(width){i => this.apply(i) }
  @api def map[B:Bits](func: A => B): Vec[B] = Vec.ZeroFirst(elems.map(func):_*)
  @api def zip[B:Bits,R:Bits](that: Vec[B])(func: (A,B) => R): Vec[R] = {
    if (that.width != this.width) {
      implicit val tV: Vec[R] = Vec.bits[R](width)
      error(ctx,s"Mismatched vector lengths. Expected length $width, got ${that.width}.")
      error(ctx)
      err[Vec[R]]("Mismatched vector")
    }
    else {
      Vec.ZeroFirst(this.elems.zip(that.elems).map{case (a,b) => func(a,b) }:_*)
    }
  }
  @api def reduce(func: (A,A) => A): A = ReduceTree(elems:_*)(func)

  @rig def arith(name: String)(func: Arith[A] => Vec[A]): Vec[A] = aA.map(func).getOrElse{
    implicit val tV: Vec[A] = Vec.bits[A](width)
    error(ctx, s"Arithmetic $name is not defined for ${this.tp}")
    error(ctx)
    err[Vec[A]]("Undefined arithmetic vector operation")
  }

  // TODO[5]: This is a bit hacky - is there a better way to define these?
  @api def unary_-(): Vec[A] = arith("negation"){a => this.map(a.neg) }
  @api def +(b: Vec[A]): Vec[A] = arith("addition"){a => this.zip(b)(a.add) }
  @api def -(b: Vec[A]): Vec[A] = arith("subtraction"){a => this.zip(b)(a.sub) }
  @api def *(b: Vec[A]): Vec[A] = arith("multiplication"){a => this.zip(b)(a.mul) }
  @api def /(b: Vec[A]): Vec[A] = arith("division"){a => this.zip(b)(a.div) }
  @api def %(b: Vec[A]): Vec[A] = arith("modulus"){a => this.zip(b)(a.mod) }

  @api override def asBits: Vec[Bit] = {
    val elems = Seq.tabulate(width){i => this(i).asBits }
    Vec.concat(elems)
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
  @api def apply(s: Series[I32]): Vec[A] = (s.start, s.end, s.step) match {
    case (Const(x1),Const(x2),Const(c)) =>
      if (c !== 1) {
        error(ctx, "Strides for vector slice are currently unsupported.")
        error(ctx)
        Vec.empty[A]
      }
      else {
        val msb = Number.max(x1, x2).toInt
        val lsb = Number.min(x1, x2).toInt
        if (lsb > width) {
          warn(ctx, "Slice is entirely outside word width. Will use zeros instead.")
          warn(ctx)
        }
        else if (msb > width) {
          warn(ctx, "Slice includes words outside vector width.")
          warn(ctx, "Zeros will be inserted in the MSBs.", noWarning = true)
          warn(ctx)
        }
        sliceUnchecked(msb, lsb)
      }
    case _ =>
      error(this.ctx, "Apply range for bit slicing must be statically known.")
      error(this.ctx)
      Vec.empty[A]
  }

  @rig def sliceUnchecked(msb: Int, lsb: Int): Vec[A] = {
    if (msb <= width)     Vec.slice(this, msb, lsb)
    else if (lsb > width) Vec.bits[A](msb - lsb + 1).zero
    else {
      val zeros = Vec.bits[A](msb - width).zero
      Vec.slice(this, width, lsb) ++ zeros
    }
  }

  /**
    * Returns a new vector formed by the concatenation of this and that.
    */
  @api def ++(that: Vec[A]): Vec[A] = Vec.concat(Seq(this,that))

  /**
    * Returns a new vector with this vector's elements in reverse order.
    */
  @api def reverse: Vec[A] = {
    implicit val tV: Vec[A] = this.selfType
    stage(VecReverse(this))
  }

  @api override def neql(that: Vec[A]): Bit = this.zip(that){(a,b) => a.neql(b) }.reduce{_||_}
  @api override def eql(that: Vec[A]): Bit = this.zip(that){(a,b) => a.eql(b) }.reduce{_&&_}

  // --- Typeclass Methods
  override protected val __neverMutable: Boolean = false

  @rig def nbits: Int = A.nbits * width
  @rig def zero: Vec[A] = Vec.ZeroLast(Seq.fill(width){ A.zero }:_*)
  @rig def one: Vec[A] = Vec.ZeroLast(Seq.fill(width-1){ A.zero} :+ A.one :_*)
  @rig def random(max: Option[Vec[A]]): Vec[A] = {
    if (max.isDefined && max.get.width != width) {
      error(ctx, s"Vector length mismatch. Expected $width ${plural(width,"word")}, got ${max.get.width}")
      error(ctx)
    }
    val elems = Seq.tabulate(width){i => A.random(max.map{vec => vec(i)}) }
    Vec.ZeroLast(elems:_*)
  }

  @rig def abs(a: Vec[A]): Vec[A] = arith("abs"){a => this.map(a.abs) }
  @rig def ceil(a: Vec[A]): Vec[A] = arith("ceil"){a => this.map(a.ceil) }
  @rig def floor(a: Vec[A]): Vec[A] = arith("floor"){a => this.map(a.floor) }
}



object Vec {
  def bits[A:Bits](length: Int): Vec[A] = proto(new Vec[A](length))
  def arith[A](length: Int)(implicit ev: (Arith[A] with Bits[A])): Vec[A] = proto(new Vec[A](length))

  /**
    * Creates a little-endian vector from the given N elements
    * The first element is the most significant word (vector index N-1).
    * The last element is the least significant word (vector index of 0).
    */
  @api def LittleEndian[A:Bits](elems: A*): Vec[A] = fromSeq(elems.reverse)

  /**
    * (Alias for LittleEndian)
    * Creates a little-endian vector from the given N elements
    * The first element is the most significant word (vector index N-1).
    * The last element is the least significant word (vector index of 0).
    */
  @api def ZeroLast[A:Bits](elems: A*): Vec[A] = fromSeq(elems.reverse)

  /**
    * Creates a big-endian vector from the given N elements.
    * The first element is the least significant word (vector index 0).
    * The last element is the most significant word (vector index of N-1).
    */
  @api def BigEndian[A:Bits](elems: A*): Vec[A] = fromSeq(elems)

  /**
    * (Alias for BigEndian)
    * Creates a big-endian vector from the given N elements.
    * The first element is the least significant word (vector index 0).
    * The last element is the most significant word (vector index of N-1).
    */
  @api def ZeroFirst[A:Bits](elems: A*): Vec[A] = fromSeq(elems)

  /**
    * Creates an empty vector (of the given type).
    */
  @rig def empty[A:Bits]: Vec[A] = Vec.fromSeq[A](Nil)

  /**
    * Creates a vector from the concatenation of the given elements.
    */
  @rig def fromSeq[A:Bits](elems: Seq[A]): Vec[A] = {
    implicit val tV: Vec[A] = Vec.bits[A](elems.length)
    stage(VecAlloc(elems))
  }

  /**
    * Creates an element slice of the vector from [lsb,msb]
    */
  @rig def slice[A:Bits](vec: Vec[A], msw: Int, lsw: Int): Vec[A] = {
    implicit val tV: Vec[A] = Vec.bits[A](Math.max(msw - lsw + 1, 0))
    stage(VecSlice(vec,msw,lsw))
  }

  /**
    * Creates a new vector which is the concatenation of all given vectors.
    */
  @rig def concat[A:Bits](vecs: Seq[Vec[A]]): Vec[A] = {
    implicit val tV: Vec[A] = Vec.bits[A](vecs.map(_.width).sum)
    stage(VecConcat(vecs))
  }

  @rig def fromBits[A](bits: Vec[Bit], width: Int, A: Bits[A]): Vec[A] = A match {
    case _:Bit => bits.asInstanceOf[Vec[A]]
    case _ =>
      implicit val bA: Bits[A] = A
      val elems = Seq.tabulate(width) { i =>
        val offset = i * A.nbits
        val length = A.nbits
        bits.sliceUnchecked(msb = offset + length, lsb = offset).as[A]
      }
      Vec.fromSeq(elems)
  }
}
