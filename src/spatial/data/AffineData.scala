package spatial.data

import forge.tags._
import core._
import spatial.lang._
import spatial.util._
import poly.{ConstraintMatrix, ISL, SparseMatrix}

case class AccessMatrix(
  access: Sym[_],
  matrix: SparseMatrix[Idx],
  unroll: Seq[Int]
) {
  def keys: Set[Idx] = matrix.keys
  @stateful def parent: Ctrl = access.parent

  def overlaps(b: AccessMatrix)(implicit isl: ISL): Boolean = isl.overlaps(this.matrix, b.matrix)
  def isSuperset(b: AccessMatrix)(implicit isl: ISL): Boolean = isl.isSuperset(this.matrix, b.matrix)
  def intersects(b: AccessMatrix)(implicit isl: ISL): Boolean = isl.intersects(this.matrix, b.matrix)

  override def toString: String = {
    stm(access) + " {" + unroll.mkString(",") + "}\n" + matrix.toString
  }
}


case class AffineMatrices(matrices: Seq[AccessMatrix]) extends AnalysisData[AffineMatrices] {
  override def toString: String = s"AffineMatrices(${matrices.length} matrices)"
}
@data object affineMatricesOf {
  def get(access: Sym[_]): Option[Seq[AccessMatrix]] = metadata[AffineMatrices](access).map(_.matrices)
  def apply(access: Sym[_]): Seq[AccessMatrix] = affineMatricesOf.get(access).getOrElse{throw new Exception(s"No affine matrices defined for $access")}
  def update(access: Sym[_], matrices: Seq[AccessMatrix]): Unit = metadata.add(access, AffineMatrices(matrices))
}


case class Domain(domain: ConstraintMatrix[Idx]) extends AnalysisData[Domain]
@data object domainOf {
  def get(x: Sym[_]): Option[ConstraintMatrix[Idx]] = metadata[Domain](x).map(_.domain)
  def apply(x: Sym[_]): ConstraintMatrix[Idx] = domainOf.get(x).getOrElse{ ConstraintMatrix.empty }
  def update(x: Sym[_], domain: ConstraintMatrix[Idx]): Unit = metadata.add(x, Domain(domain))

  def getOrElseUpdate(x: Sym[_], els: => ConstraintMatrix[Idx]): ConstraintMatrix[Idx] = domainOf.get(x) match {
    case Some(domain) => domain
    case None =>
      val domain = els
      domainOf(x) = domain
      domain
  }
}