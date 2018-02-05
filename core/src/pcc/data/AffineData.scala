package pcc.data

import forge._
import pcc.core._
import pcc.lang._

sealed abstract class ConstraintType
case object GEQ_ZERO extends ConstraintType
case object EQL_ZERO extends ConstraintType

case class SparseConstraint(cols: Map[I32,Int], c: Int, tp: ConstraintType)

case class SparseVector(
  cols:      Map[I32,Int],
  c:         Int,
  lastIters: Map[I32,Option[I32]]
) {
  def apply(x: I32): Int = if (cols.contains(x)) cols(x) else 0
  def getOrElse(x: I32, v: => Int): Int = cols.getOrElse(x,v)

  def asMinConstraint(x: I32) = SparseConstraint(cols.map{case (i,a) => i -> -a} ++ Map(x -> 1) , -c, GEQ_ZERO)
  def asMaxConstraint(x: I32) = SparseConstraint(cols ++ Map(x -> -1), c, GEQ_ZERO)
}


case class SparseMatrix(rows: Seq[SparseVector]) {
  def printWithTab(print: String => Unit): Unit = {
    val header = rows.flatMap(_.cols.keys).distinct
    val entries = (header.map(_.toString) :+ "c") +: rows.map{row => header.map{k => row(k).toString } :+ row.c.toString }
    val maxCol = (0 +: entries.flatMap(_.map(_.length))).max
    entries.foreach{row =>
      print(row.map{x => " "*(maxCol - x.length + 1) + x }.mkString(" "))
    }
  }

  def map(f: SparseVector => SparseVector): SparseMatrix = SparseMatrix(rows.map(f))
  def keys: Set[I32] = rows.map(_.cols.keySet).fold(Set.empty)(_++_)
}
object SparseMatrix {
  def empty = SparseMatrix(Nil)
}


case class AccessMatrix(
  access: Sym[_],
  matrix: SparseMatrix,
  unroll: Seq[Int]
) {
  def printWithTab(print: String => Unit): Unit = {
    print(stm(access) + "[" + unroll.mkString(",") + "]")
    matrix.printWithTab(print)
  }
  def keys: Set[I32] = matrix.keys
}


case class AffineMatrices(matrices: Seq[AccessMatrix]) extends SimpleData[AffineMatrices]
@data object affineMatricesOf {
  def get(access: Sym[_]): Option[Seq[AccessMatrix]] = metadata[AffineMatrices](access).map(_.matrices)
  def apply(access: Sym[_]): Seq[AccessMatrix] = affineMatricesOf.get(access).getOrElse{throw new Exception(s"No affine matrices defined for $access")}
  def update(access: Sym[_], matrices: Seq[AccessMatrix]): Unit = metadata.add(access, AffineMatrices(matrices))
}

case class Domain(domain: Seq[SparseConstraint]) extends SimpleData[Domain]
@data object domainOf {
  def get(x: Sym[_]): Option[Seq[SparseConstraint]] = metadata[Domain](x).map(_.domain)
  def apply(x: Sym[_]): Seq[SparseConstraint] = domainOf.get(x).getOrElse{throw new Exception(s"No domain defined for $x")}
  def update(x: Sym[_], domain: Seq[SparseConstraint]): Unit = metadata.add(x, Domain(domain))

  def getOrElseUpdate(x: Sym[_], els: => Seq[SparseConstraint]): Seq[SparseConstraint] = domainOf.get(x) match {
    case Some(domain) => domain
    case None =>
      val domain = els
      domainOf(x) = domain
      domain
  }
}