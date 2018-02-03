package pcc.data

import forge._
import pcc.core._
import pcc.lang._

case class Prod(xs: Seq[I32], m: Int = 1) {
  def syms: Seq[I32] = xs
  def isConst: Boolean = xs.isEmpty
  def isSymWithMultiplier: Boolean = xs.length == 1

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

  def partialEval(f: PartialFunction[I32,Int]): Prod = {
    val (cs,ys) = xs.partition(f.isDefinedAt)
    val c = m * cs.map(f).product
    Prod(ys,c)
  }
  override def toString: String = if (isConst) m.toString else {
    (if (m != 1) s"$m" else "") + xs.mkString("*")
  }
}
object Prod {
  def single(c: Int): Prod = Prod(Nil, c)
  def single(x: I32): Prod = Prod(Seq(x))
}

case class Sum(ps: Seq[Prod], b: Int = 0) {
  def syms: Seq[I32] = ps.flatMap(_.syms)
  def isConst: Boolean = ps.isEmpty
  def unary_-(): Sum = Sum(ps.map{p => -p }, -b)
  def *(s: Sum): Sum = Sum(ps.flatMap{p => s.ps.map{p2 => p * p2 }} ++ s.ps.flatMap{_*b} ++ ps.flatMap{_*s.b}, b * s.b)
  def +(s: Sum): Sum = Sum(ps ++ s.ps, b + s.b)
  def -(s: Sum): Sum = -s + this

  def *(p: Prod): Sum = Sum(ps.map{_ * p} ++ p*b, 0)
  def +(p: Prod): Sum = Sum(p +: ps, b)
  def -(p: Prod): Sum = Sum(-p +: ps, b)

  def *(c: Int): Sum = Sum(ps.flatMap(_ * c), b*c)
  def +(c: Int): Sum = Sum(ps, b + c)
  def -(c: Int): Sum = Sum(ps, b - c)

  def partialEval(f: PartialFunction[I32,Int]): Sum = {
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
  def single(x: I32): Sum = Sum(Seq(Prod.single(x)))
}

case class AffineComponent(a: Prod, i: I32) {
  def unary_-(): AffineComponent = AffineComponent(-a, i)
  def *(s: Sum): Seq[AffineComponent] = (s * a).ps.map{p => AffineComponent(p,i) }
  override def toString: String = s"$a*$i"
}

case class AffineProduct(a: Sum, i: I32) {
  override def toString: String = s"($a)$i"
}


case class AddressPattern(comps: Seq[AffineProduct], ofs: Sum, lastIters: Map[I32,Option[I32]], last: Option[I32]) {

  /**
    * Convert this to a sparse vector representation if it is representable as an affine equation with
    * constant multipliers. Returns None otherwise.
    */
  @stateful def getSparseVector: Option[SparseVector] = {
    val is = comps.map(_.i)
    val as = comps.map{_.a.partialEval{case Expect(c) => c}}
    val bx = ofs.partialEval{case Expect(c) => c}
    if (as.forall(_.isConst) && (bx.isConst || bx.ps.forall(_.isSymWithMultiplier)) ) {
      val randComponents = bx.ps.map{p => (p.m, p.xs.head) }
      val rs: Seq[(I32,Int)] = randComponents.groupBy(_._2).mapValues{as => as.map(_._1).sum}.toSeq

      val xs_all: Seq[I32] = is ++ rs.map(_._1)
      val as_all: Seq[Int] = as.map(_.b) ++ rs.map(_._2)
      val lastIs = lastIters.map{case (k,v) => k -> v }
      Some( SparseVector(xs_all.zip(as_all).toMap, bx.b, lastIs) )
    }
    else None
  }

  /**
    * Convert this to a sparse vector representation if it is representable as an affine equation with
    * constant multipliers. Falls back to a representation of 1*x + 0 otherwise.
    */
  @stateful def toSparseVector(x: () => I32): SparseVector = {
    getSparseVector.getOrElse{
      val y = x()
      SparseVector(Map(y -> 1), 0, Map(y -> last))
    }
  }

  override def toString: String = comps.mkString(" + ") + (if (comps.isEmpty) "" else " + ") + ofs
}

case class AccessPattern(pattern: Seq[AddressPattern]) extends SimpleData[AccessPattern]
@data object accessPatternOf {
  def get(x: Sym[_]): Option[Seq[AddressPattern]] = metadata[AccessPattern](x).map(_.pattern)
  def apply(x: Sym[_]): Seq[AddressPattern] = accessPatternOf.get(x).getOrElse{throw new Exception(s"No access pattern defined for $x")}
  def update(x: Sym[_], pattern: Seq[AddressPattern]): Unit = metadata.add(x, AccessPattern(pattern))
}
