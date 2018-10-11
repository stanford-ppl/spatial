package spatial.metadata.access

import forge.tags._
import argon._
import utils.implicits.collections._
import spatial.lang._
import spatial.metadata.control._
import poly.{ConstraintMatrix, ISL, SparseMatrix, SparseVector}

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
  def randomizeKeys(keySwap: Map[Idx,Idx]): AccessMatrix = {
    keySwap.foreach{case (old, swp) => swp.domain = old.domain.replaceKeys(keySwap)}
    val matrix2 = matrix.replaceKeys(keySwap) 
    AccessMatrix(access, matrix2, unroll)
  }

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

  def short: String = s"$access {${unroll.mkString(",")}}"

  implicit class SeqMath(a: Seq[Int]) {
    def *(b: SparseMatrix[Idx]): SparseVector[Idx] = {
      val vec = b.keys.mapping{k => b.rows.zip(a).iterator.map{case (row_i,a_i) => row_i(k)*a_i }.sum }
      val c = b.rows.zip(a).iterator.map{case (row_i,a_i) => row_i.c*a_i}.sum
      SparseVector[Idx](vec,c,Map.empty)
    }
  }

  /** True if the matrix always resolves to the same bank under banking scheme N, B, alpha */
  def isDirectlyBanked(N: Seq[Int], B: Seq[Int], alpha: Seq[Int]): Boolean = {
    val bank = alpha*matrix 
    bank.cols.forall{case (k,v) => (v/B.head /*?*/) % N.head /*?*/ == 0}
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

