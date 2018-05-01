package spatial.data

import forge.tags._
import argon._
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

/** The unrolled access patterns of a (optionally parallelized) memory access represented as affine matrices.
  * Random accesses are represented as affine by representing the random symbol as a 'iterator'.
  *
  * Option:  sym.getAffineMatrices
  * Getter:  sym.affineMatrices
  * Setter:  sym.affineMatrices = (Seq[AccessMatrix])
  * Default: Nil (no access matrices)
  */
case class AffineMatrices(matrices: Seq[AccessMatrix]) extends AnalysisData[AffineMatrices] {
  override def toString: String = s"AffineMatrices(${matrices.length} matrices)"
}

/** Symbolic domain restrictions of an integer symbol, represented as a symbolic affine constraint matrix
  *
  * Option:  sym.getDomain
  * Getter:  sym.domain
  * Setter:  sym.domain = (ConstraintMatrix[Idx])
  * Get/Set: sym.getOrElseUpdateDomain( => ConstraintMatrix[Idx])
  * Default: Empty constraint matrix (no constraints)
  */
case class Domain(domain: ConstraintMatrix[Idx]) extends AnalysisData[Domain]


trait AffineData {

  implicit class AffineDataOps(s: Sym[_]) {
    def getAffineMatrices: Option[Seq[AccessMatrix]] = metadata[AffineMatrices](s).map(_.matrices)
    def affineMatrices: Seq[AccessMatrix] = getAffineMatrices.getOrElse(Nil)
    def affineMatrices_=(matrices: Seq[AccessMatrix]): Unit = metadata.add(s, AffineMatrices(matrices))

    def getDomain: Option[ConstraintMatrix[Idx]] = metadata[Domain](s).map(_.domain)
    def domain: ConstraintMatrix[Idx] = getDomain.getOrElse{ ConstraintMatrix.empty }
    def domain_=(d: ConstraintMatrix[Idx]): Unit = metadata.add(s, Domain(d))

    def getOrElseUpdateDomain(els: => ConstraintMatrix[Idx]): ConstraintMatrix[Idx] = getDomain match {
      case Some(domain) => domain
      case None =>
        s.domain = els
        s.domain
    }
  }
}
