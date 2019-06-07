package spatial.metadata.access

import forge.tags._
import argon._
import utils.implicits.collections._
import utils.math._
import spatial.lang._
import spatial.metadata.control._
import spatial.metadata.memory._
import poly.{ConstraintMatrix, ISL, SparseMatrix, SparseVector}
import emul.ResidualGenerator._

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

  /** Determine pseudo lane of this matrix when the access is a VecAccess */
  def pseudoLane: Int = access.affineMatrices.indexOf(this)

  override def toString: String = {
    stm(access) + " {" + unroll.mkString(",") + "}\n" + matrix.toString
  }

  def short: String = s"$access {${unroll.mkString(",")}}"

  implicit class SeqMath(a: Seq[Int]) {
    def flatMul(b: SparseMatrix[Idx]): SparseVector[Idx] = {
      val vec: Map[Idx, Int] = b.keys.mapping{k => b.rows.zip(a).iterator.map{case (row_i,a_i) => row_i(k)*a_i }.sum }
      val c = b.rows.zip(a).iterator.map{case (row_i,a_i) => row_i.c*a_i}.sum
      val allIters = (Seq(Map[Idx,Seq[Idx]]()) ++ b.rows.map(_.allIters)).reduce(_++_)
      SparseVector[Idx](vec,c,allIters)
    }
    def hierarchicalMul(b: SparseMatrix[Idx]): SparseMatrix[Idx] = {
      val rows2 = b.rows.zip(a).map{case (r, alpha) => 
        val vec: Map[Idx, Int] = b.keys.mapping{k => r(k)*alpha}
        val c2 = r.c * alpha
        SparseVector[Idx](vec, c2, r.allIters)
      }
      SparseMatrix[Idx](rows2)
    }
  }

  /** True if the matrix always resolves to the same bank under banking scheme N, B, alpha */
  def isDirectlyBanked(N: Seq[Int], B: Seq[Int], alpha: Seq[Int]): Boolean = {
    bankMuxWidth(N, B, alpha) == 1
  }

  /** Computes how many banks this access can touch under the given banking scheme */
  def bankMuxWidth(N: Seq[Int], B: Seq[Int], alpha: Seq[Int]): Int = {
    if (N.size == 1 && alpha.size > 1) {
      val bank: SparseVector[Idx] = alpha flatMul matrix
      val span: ResidualGenerator = bank.span(N.head, B.head)
      span.expand(N.head).distinct.size
    } else {
      val banks: SparseMatrix[Idx] = alpha hierarchicalMul matrix
      val spans: Seq[ResidualGenerator] = banks.rows.zipWithIndex.map{case (r,i) => r.span(N(i), B(i))}
      spans.zipWithIndex.map{case (span, i) => span.expand(N(i)).distinct.size}.product
    }
  }

  /** Reports which arithmetic nodes are required for banking resolution.  Returns a list of node name, op1, op2 */
  def arithmeticNodes(N: Seq[Int], B: Seq[Int], alpha: Seq[Int]): Seq[(String, Option[Int], Option[Int])] = {
    if (isDirectlyBanked(N,B,alpha)) Seq()
    else {
      // Assume mul and div by pow2 are free
      if (N.size == 1 && alpha.size > 1) {
        // This count could be off if combining partition factor components happens to resolve in mod math, but it's just an estimate anyway
        val combinePattern: List[(String, Option[Int], Option[Int])] = matrix.rows.map{r => List.tabulate(r.cols.size - 1){q => ("FixAdd",None,None)} ++ {if (r.cols.size > 0) List(("FixAdd",None,Some(r.c))) else List()}}.flatten.toList
        val partitionFactorComponent: List[(String, Option[Int], Option[Int])] = List.tabulate(matrix.rows.size){i => i}.flatMap{i => 
          if (!isPow2(alpha(i)) && matrix.rows(i).cols.size == 0 && !isPow2(matrix.rows(i).c)) Some(("FixMul", Some(matrix.rows(i).c), Some(alpha(i)))) 
          else if (!isPow2(alpha(i)) && matrix.rows(i).cols.size > 0) Some(("FixMul", None, Some(alpha(i))))
          else None
        }
        val partitionFactorCombine: List[(String, Option[Int], Option[Int])] = List.tabulate(matrix.rows.size-1){i => ("FixAdd", None, None)}
        val divComponent: List[(String, Option[Int], Option[Int])] = if (B.head == 1 || isPow2(B.head)) List() else List(("FixDiv", None, Some(B.head)))
        val modComponent: List[(String, Option[Int], Option[Int])] = if (N.head == 1 || isPow2(N.head)) List() else List(("FixMod", None, Some(N.head)))
        combinePattern ++ partitionFactorCombine ++ partitionFactorComponent ++ divComponent ++ modComponent
      } else {
        // This count could be off if combining partition factor components happens to resolve in mod math, but it's just an estimate anyway
        val dimComponents = matrix.rows.zipWithIndex.map{case (r, i) => 
          val combinePattern: List[(String, Option[Int], Option[Int])] = List.tabulate(r.cols.size - 1){q => ("FixAdd",None,None)} ++ {if (r.cols.size > 0) List(("FixAdd",None,Some(r.c))) else List()}
          val partitionFactorComponent: List[(String, Option[Int], Option[Int])] = 
            if (r.cols.size == 1 && (r.cols.head._2 * alpha(i) % N(i) == 0)) List()
            else if (!isPow2(alpha(i)) && r.cols.size == 0 && !isPow2(r.c)) List(("FixMul", Some(r.c), Some(alpha(i)))) 
            else if (!isPow2(alpha(i)) && r.cols.size > 0) List(("FixMul", None, Some(alpha(i))))
            else List()
          val divComponent: List[(String, Option[Int], Option[Int])] = if (B(i) == 1 || isPow2(B(i))) List() else List(("FixDiv", None, Some(B(i))))
          val modComponent: List[(String, Option[Int], Option[Int])] = if (N(i) == 1 || isPow2(N(i))) List() else List(("FixMod", None, Some(N(i))))
          combinePattern ++ partitionFactorComponent ++ divComponent ++ modComponent
        }
        dimComponents.reduce(_++_).filterNot{x => x._2 == Some(0) || x._3 == Some(0)}
      }
    }
  }

  /** Get segments that each unroll belongs to */
  def segmentAssignments: Seq[Int] = access.segmentMapping.map(_._2).toSeq
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

