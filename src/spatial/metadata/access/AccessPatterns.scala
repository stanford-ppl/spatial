package spatial.metadata.access

import argon._
import forge.tags._
import poly.SparseVector
import spatial.lang._
import spatial.metadata.bounds.Expect

import utils.implicits.collections._

case class InvalidDivisionException(x: Any, c: Any)
  extends Exception(s"Cannot statically divide $x by $c")

case class Prod(xs: Seq[Idx], m: Int = 1) {
  def syms: Seq[Idx] = xs
  def isConst: Boolean = xs.isEmpty
  def isSymWithMultiplier: Boolean = xs.lengthIs(1)

  def unary_-(): Prod = Prod(xs, -m)
  def *(p: Prod): Prod = Prod(this.xs ++ p.xs, this.m * p.m)
  def +(p: Prod): Sum = Sum(Seq(this,p))
  def -(p: Prod): Sum = Sum(Seq(this,-p))

  def *(s: Sum): Sum = s * this
  def +(s: Sum): Sum = s + this
  def -(s: Sum): Sum = -s + this

  def *(c: Int): Option[Prod] = if (c != 0) Some(Prod(xs, m * c)) else None
  def +(c: Int): Sum  = Sum(Seq(this), c)
  def -(c: Int): Sum  = Sum(Seq(this), c)

  def /(c: Int): Prod = {
    if (canBeDividedBy(c)) Prod(xs, m / c)
    else throw InvalidDivisionException(this, c)
  }
  def /(s: Sum): Prod = {
    if (canBeDividedBy(s) && s.isConst) Prod(xs, m / s.b)
    else if (canBeDividedBy(s)) this / s.ps.head
    else throw InvalidDivisionException(this, s)
  }
  def /(p: Prod): Prod = {
    if (canBeDividedBy(p)) Prod(xs diff p.xs, m / p.m)
    else throw InvalidDivisionException(this, p)
  }

  def canBeDividedBy(c: Int): Boolean = m % c == 0
  def canBeDividedBy(s: Sum): Boolean = {
    (s.isConst && m % s.b == 0) ||
      (s.b == 0 && s.ps.size == 1 && this.canBeDividedBy(s.ps.head))
  }
  def canBeDividedBy(p: Prod): Boolean = {
    // All symbols in p also exist in this, and the constant multiplier divides this multiplier
    p.xs.diff(xs).isEmpty && m % p.m == 0
  }

  def partialEval(f: PartialFunction[Idx,Int]): Prod = {
    val (cs,ys) = xs.partition(f.isDefinedAt)
    val c = m * cs.map(f).product
    Prod(ys,c)
  }
  override def toString: String = if (isConst) m.toString else {
    (if (m != 1) s"$m*" else "") + xs.mkString("*")
  }
}
object Prod {
  def single(c: Int): Prod = Prod(Nil, c)
  def single(x: Idx): Prod = x match {
    case Expect(c) => Prod.single(c)
    case _ => Prod(Seq(x))
  }
}

case class Sum(ps: Seq[Prod], b: Int = 0) {

  implicit class SumOfProdsOps(ps: Seq[Prod]) {
    def unary_-(): Seq[Prod] = ps.map{p => -p}                 // -(p1 + p2)

    def canBeDividedBy(c: Int): Boolean = ps.forall(_.canBeDividedBy(c))
    def canBeDividedBy(s: Sum): Boolean = ps.forall(_.canBeDividedBy(s))
    def canBeDividedBy(p: Prod): Boolean = ps.forall(_.canBeDividedBy(p))

    def *(p2: Seq[Prod]): Seq[Prod] = p2.flatMap{p => ps * p } // (p1 + p2) * (p3 + p4)
    def *(p: Prod): Seq[Prod] = ps.map(_ * p)                  // (p1 + p2) * p
    def *(s: Sum): Seq[Prod] = ps * s.ps + ps * s.b            // (p1 + p2) * (p3 + p4 + c)
    def *(c: Int): Seq[Prod] = ps.flatMap(_ * c)               // (p1 + p2) * c

    def +(p2: Seq[Prod]): Seq[Prod] = ps ++ p2                 // (p1 + p2) + (p3 + p4)
    def +(p: Option[Prod]): Seq[Prod] = ps ++ p                // (p1 + p2) + (p|0)
    def +(p: Prod): Seq[Prod] = p +: ps                        // (p1 + p2) + p
    def +(c: Int): Sum = Sum(ps, c)                            // (p1 + p2) + c

    def -(p: Prod): Seq[Prod] = -p +: ps                       // (p1 + p2) - p
    def -(c: Int): Sum = Sum(ps, -c)                           // (p1 + p2) - c

    def /(c: Int): Seq[Prod] = ps.map(_ / c)
    def /(s: Sum): Seq[Prod] = ps.map(_ / s)
    def /(p: Prod): Seq[Prod] = ps.map(_ / p)
  }


  def syms: Seq[Idx] = ps.flatMap(_.syms)
  def isConst: Boolean = ps.isEmpty
  def unary_-(): Sum = Sum(ps.map{p => -p }, -b)

  def *(p: Prod): Sum = ps*p + p*b + 0
  def *(s: Sum): Sum = Sum(ps.flatMap{p => s.ps.map{p2 => p * p2 }} ++ s.ps.flatMap{_*b} ++ ps.flatMap{_*s.b}, b * s.b)
  def *(c: Int): Sum = Sum(ps*c, b*c)

  def +(p: Prod): Sum = ps + p + b
  def +(s: Sum): Sum = ps + s.ps + b + s.b
  def +(c: Int): Sum = Sum(ps, b + c)

  def -(s: Sum): Sum = -s + this
  def -(p: Prod): Sum = Sum(ps - p, b)
  def -(c: Int): Sum = Sum(ps, b - c)

  def /(c: Int): Sum = {
    if (canBeDividedBy(c)) Sum(ps / c, b / c)
    else throw InvalidDivisionException(this, c)
  }
  def /(s: Sum): Sum = {
    if (s.isConst) this / s.b
    else if (b != 0) throw InvalidDivisionException(this, s)
    else Sum(ps / s, 0)
  }
  def /(p: Prod): Sum = {
    if (p.isConst) this / p.m
    else if (b != 0) throw InvalidDivisionException(this, p)
    else Sum(ps / p, 0)
  }

  def canBeDividedBy(c: Int): Boolean = b % c == 0 && ps.canBeDividedBy(c)
  def canBeDividedBy(s: Sum): Boolean = {
    // Can only statically divide a sum by another sum if
    // b is zero or the sum is a constant value which divides b
    // and the sum divides all the product components.
    (b == 0 || (s.isConst && b % s.b == 0)) && ps.canBeDividedBy(s)
  }
  def canBeDividedBy(p: Prod): Boolean = {
    (b == 0 || (p.isConst && b % p.m == 0)) && ps.canBeDividedBy(p)
  }

  def partialEval(f: PartialFunction[Idx,Int]): Sum = {
    val (cs,ys) = ps.map(_.partialEval(f)).partition(_.isConst)
    val c = b + cs.map(_.m).sum
    Sum(ys, c)
  }

  override def toString: String = if (isConst) b.toString else {
    ps.mkString(" + ") + (if (b != 0) s" + $b" else "")
  }
}
object Sum {
  def single(c: Int): Sum = Sum(Nil, c)
  def single(x: Idx): Sum = x match {
    case Expect(c) => Sum.single(c)
    case _ => Sum(Seq(Prod.single(x)))
  }
}

case class Modulus(m: Int, set: Boolean = true) {
  def +(b: Modulus): Modulus = if (b.set && set) {Modulus(m + b.m, true)}
                               else if (b.set && !set) {Modulus(b.m, true)}
                               else if (!b.set && set) {Modulus(m, true)}
                               else {Modulus(0, false)}
  def %(b: Modulus): Modulus = if (b.set && set) {Modulus(m min b.m, true)}
                               else if (b.set && !set) {Modulus(b.m, true)}
                               else if (!b.set && set) {Modulus(m, true)}
                               else {Modulus(0, false)}
  def toInt: Int = if (set) m else 0
  override def toString: String = if (set) s"mod $m" else ""
}
object Modulus {
  def notset: Modulus = Modulus(0, false)
}


case class AffineComponent(a: Prod, i: Idx) {
  def syms: Seq[Idx] = i +: a.syms
  def unary_-(): AffineComponent = AffineComponent(-a, i)
  def *(s: Sum): Seq[AffineComponent] = (s * a).ps.map{p => AffineComponent(p,i) }
  def /(s: Sum): AffineComponent = AffineComponent(a / s, i)

  def canBeDividedBy(s: Sum): Boolean = a.canBeDividedBy(s)

  override def toString: String = s"$a*$i"
}

case class AffineProduct(a: Sum, i: Idx) {
  def syms: Seq[Idx] = i +: a.syms
  override def toString: String = if (a.isConst || (a.b == 0 && a.ps.size == 1)) s"$a*$i" else s"($a)*$i"
}


/** Represents a generalized symbolic affine address function.
  * Each component of the affine function is represented as a sum of products, where each sum is
  * a multiplier for a distinct loop index.
  *
  * The offset is represented as a separate sum of products, where each sum is a multiplier for some
  * symbol.
  *
  * e.g. for x(32*i + m*i + 15*j + 2*x + 13), this is represented as:
  *  comps = AffineProduct(Sum(Prod(32), Prod(m)), i), AffineProduct(Sum(Prod(15)), j)
  *  ofs   = Sum(Prod(2,x),Prod(13))
  *
  * @param comps A list of sum of products multipliers for distinct loop indices
  * @param ofs   A sum of products representing the symbolic offset
  * @param lastIters Mapping from symbol to the innermost iterator it varies with, None if it is entirely loop invariant
  */
case class AddressPattern(comps: Seq[AffineProduct], ofs: Sum, lastIters: Map[Idx,Option[Idx]], last: Option[Idx], iterStarts: Map[Idx,Ind[_]], modulus: Int) {

  /** Convert this to a sparse vector representation if it is representable as an affine equation with
    * constant multipliers. Returns None otherwise.
    */
  @stateful def getSparseVector: Option[SparseVector[Idx]] = {
    val is = comps.map(_.i)
    val starts = is.collect{case x if (iterStarts.contains(x)) => iterStarts(x)}.filter(!_.isConst)
    val as = comps.map{_.a.partialEval{case Expect(c) => c}}
    val bx = ofs.partialEval{case Expect(c) => c}
    if (as.forall(_.isConst) && (bx.isConst || bx.ps.forall(_.isSymWithMultiplier)) ) {
      val randComponents = bx.ps.map{p => (p.m, p.xs.head) }
      val rs: Seq[(Idx,Int)] = randComponents.groupBy(_._2).mapValues{as => as.map(_._1).sum}.toSeq

      val xs_all: Seq[Idx] = is ++ rs.map(_._1) ++ starts
      val as_all: Seq[Int] = as.map(_.b) ++ rs.map(_._2) ++ starts.indices.map{_ => 1}
      Some( SparseVector(xs_all.zip(as_all).toMap, bx.b, lastIters, modulus) )
    }
    else None
  }

  /**
    * Convert this to a sparse vector representation if it is representable as an affine equation with
    * constant multipliers. Falls back to a representation of 1*x + 0 otherwise.
    */
  @stateful def toSparseVector(x: () => (Idx, Int)): SparseVector[Idx] = {
    getSparseVector.getOrElse{
      val info = x()
      val y = info._1
      val m = info._2
      SparseVector(Map(y -> 1), 0, Map(y -> last), m)
    }
  }

  override def toString: String = comps.mkString(" + ") + (if (comps.isEmpty) "" else " + ") + ofs + {if (modulus != 0) s" mod $modulus" else ""}
}

/** Access pattern metadata for memory accesses.
  *
  * Option:  sym.getAccessPattern
  * Getter:  sym.accessPattern
  * Setter:  sym.accessPattern = (Set[AddressPattern])
  * Default: undefined
  */
case class AccessPattern(pattern: Seq[AddressPattern]) extends Data[AccessPattern](Transfer.Remove)
