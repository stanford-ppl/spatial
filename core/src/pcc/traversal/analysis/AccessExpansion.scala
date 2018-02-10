package pcc.traversal
package analysis

import pcc.core._
import pcc.data._
import pcc.lang._
import pcc.util.multiLoop
import pcc.poly._

import scala.collection.mutable

trait AccessExpansion {
  implicit def __IR: State
  private val unrolls = mutable.HashMap[(I32,Seq[Int]),I32]()

  private def nextRand(): I32 = bound[I32]
  private def nextRand(x: I32): I32 = {
    val x2 = nextRand()
    domainOf.get(x).foreach{d => domainOf(x2) = d }
    x2
  }
  private def unrolled(x: I32, id: Seq[Int]): I32 = unrolls.getOrElseUpdate((x,id), nextRand(x))

  /**
    * If the access pattern is representable as an affine access for bound, returns
    * a min or max constraint for x based on this bound. Otherwise returns None.
    */
  def constraint(x: I32, bound: I32, isMin: Boolean): Option[SparseConstraint] = {
    val vec = accessPatternOf.get(bound).flatMap(_.head.getSparseVector)
    if (isMin) vec.map(_.asMinConstraint(x)) else vec.map(_.asMaxConstraint(x))
  }

  /**
    * Returns a SparseMatrix representing the minimum and maximum bounds of this symbol.
    * TODO: Account for bounds of random values
    */
  private def getOrAddDomain(x: I32): ConstraintMatrix = domainOf.getOrElseUpdate(x, {
    if (ctrOf.get(x).isDefined) {
      val min = constraint(x, x.ctrStart, isMin = true).toSet
      val max = constraint(x, x.ctrEnd, isMin = false)
      ConstraintMatrix(min ++ max)
    }
    else ConstraintMatrix.empty
  })

  def domain(x: I32): ConstraintMatrix = getOrAddDomain(x)


  def getAccessCompactMatrix(access: Sym[_], addr: Seq[I32], pattern: Seq[AddressPattern]): SparseMatrix = {
    val rows = pattern.zipWithIndex.map{case (ap,d) => ap.toSparseVector{() => addr.indexOrElse(d,nextRand())} }
    val matrix = SparseMatrix(rows)
    matrix.keys.foreach{x => getOrAddDomain(x) }
    matrix
  }

  def getUnrolledMatrices(
    mem:     Sym[_],
    access:  Sym[_],
    addr:    Seq[I32],
    pattern: Seq[AddressPattern],
    vecID:   Seq[Int] = Nil
  ): Seq[AccessMatrix] = {
    val is = accessIterators(access, mem)
    val ps = is.map(_.ctrParOr1)
    val iMap = is.zipWithIndex.toMap
    val matrix = getAccessCompactMatrix(access, addr, pattern)

    multiLoop(ps).map{uid =>
      val mat = matrix.map{vec =>
        val xsOrig: Seq[I32] = vec.cols.keys.toSeq
        val components: Seq[(Int,I32,Int,Option[I32])] = xsOrig.map{x =>
          val idx = iMap.getOrElse(x,-1)
          if (idx >= 0) {
            val i = Some(is(idx))
            val a = vec(x)*ps(idx)
            val b = vec(x)*uid(idx)
            (a,x,b,i)
          }
          else {
            val i = vec.lastIters(x)
            val xid = uid.dropRight( is.length - i.map(is.indexOf).getOrElse(-1) + 1)
            val a = vec(x)
            val b = 0
            val x2 = unrolled(x, xid)
            (a,x2,b,i)
          }
        }
        val as = components.map(_._1)
        val xs = components.map(_._2)
        val c  = components.map(_._3).sum + vec.c
        val lI = components.collect{case (a,x,b,i) if !i.contains(x) => x -> i }.toMap
        SparseVector(xs.zip(as).toMap, c, lI)
      }
      val amat = AccessMatrix(access, mat, uid ++ vecID)
      amat.keys.foreach{x => getOrAddDomain(x) }
      amat
    }.toSeq
  }

}
