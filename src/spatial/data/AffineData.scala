package spatial.data

import forge.tags._
import argon._
import spatial.lang._
import spatial.util._
import poly.{ConstraintMatrix, ISL, SparseMatrix}

/** A wrapper class representing a (optionally unrolled) access and its corresponding address space
  * as an affine, sparse matrix.
  *
  * Unrolling is tracked as the unroll (duplicate) index of each parallelized loop, ordered outermost
  * to innermost
  *
  * @param access The access being represented
  * @param matrix The sparse matrix representing the access's address space in terms of loop indices
  * @param unroll The unrolling index of the access
  */
case class AccessMatrix(
  access: Sym[_],
  matrix: SparseMatrix[Idx],
  unroll: Seq[Int]
) {
  def keys: Set[Idx] = matrix.keys
  @stateful def parent: Ctrl = access.parent

  /** True if there exists a reachable multi-dimensional index I such that a(I) = b(I).
    * True if all given dimensions may intersect.
    * Trivially true for random accesses with no constraints.
    */
  def overlapsAddress(b: AccessMatrix)(implicit isl: ISL): Boolean = isl.overlapsAddress(this.matrix, b.matrix)

  /** True if this space of addresses is statically known to include all of the addresses in b. */
  def isSuperset(b: AccessMatrix)(implicit isl: ISL): Boolean = isl.isSuperset(this.matrix, b.matrix)

  /** True if this space of addresses MAY have at least one element in common with b. */
  def intersects(b: AccessMatrix)(implicit isl: ISL): Boolean = isl.intersects(this.matrix, b.matrix)

  override def toString: String = {
    stm(access) + " {" + unroll.mkString(",") + "}\n" + matrix.toString
  }
}

/** The unrolled access patterns of an optionally parallelized memory access represented as affine matrices.
  * Random accesses are represented as affine by representing the random symbol as an 'iterator'.
  *
  * Option:  sym.getAffineMatrices
  * Getter:  sym.affineMatrices
  * Setter:  sym.affineMatrices = (Seq[AccessMatrix])
  * Default: Nil (no access matrices)
  */
case class AffineMatrices(matrices: Seq[AccessMatrix]) extends Data[AffineMatrices](Transfer.Remove) {
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
case class Domain(domain: ConstraintMatrix[Idx]) extends Data[Domain](Transfer.Remove)


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
