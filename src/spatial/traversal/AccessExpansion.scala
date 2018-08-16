package spatial.traversal

import argon._
import utils.implicits.collections._
import utils.multiLoop
import poly._
import spatial.lang._
import spatial.metadata.access._
import spatial.metadata.control._

import scala.collection.mutable

trait AccessExpansion {
  implicit def __IR: State
  private val unrolls = mutable.HashMap[(Idx,Seq[Int]),Idx]()

  private def nextRand(): Idx = boundVar[I32]
  private def nextRand(x: Idx): Idx = {
    val x2 = nextRand()
    x.getDomain.foreach{d => x2.domain = d }
    x2
  }
  private def unrolled(x: Idx, id: Seq[Int]): Idx = unrolls.getOrElseUpdate((x,id), nextRand(x))

  /** If the access pattern is representable as an affine access for bound, returns
    * a min or max constraint for x based on this bound. Otherwise returns None.
    */
  def constraint(x: Idx, bound: Idx, isMin: Boolean): Option[SparseConstraint[Idx]] = {
    val vec = bound.getAccessPattern.flatMap(_.head.getSparseVector)
    if (isMin) vec.map(_.asMinConstraint(x)) else vec.map(_.asMaxConstraint(x))
  }

  /** Returns a SparseMatrix representing the minimum and maximum bounds of this symbol. */
  private def getOrAddDomain(x: Idx): ConstraintMatrix[Idx] = x.getOrElseUpdateDomain{
    if (x.getCounter.isDefined) {
      val min = constraint(x, x.ctrStart, isMin = true).toSet
      val max = constraint(x, x.ctrEnd, isMin = false)
      ConstraintMatrix[Idx](min ++ max)
    }
    else {
      // TODO[5]: Account for bounds of random values
      ConstraintMatrix.empty[Idx]
    }
  }

  def domain(x: Idx): ConstraintMatrix[Idx] = getOrAddDomain(x)


  def getAccessCompactMatrix(access: Sym[_], addr: Seq[Idx], pattern: Seq[AddressPattern]): SparseMatrix[Idx] = {
    val rows = pattern.zipWithIndex.map{case (ap,d) =>
      ap.toSparseVector{() => addr.indexOrElse(d, nextRand()) }
    }
    val matrix = SparseMatrix[Idx](rows)
    matrix.keys.foreach{x => getOrAddDomain(x) }
    matrix
  }

  def getUnrolledMatrices(
    mem:     Sym[_],
    access:  Sym[_],
    addr:    Seq[Idx],
    pattern: Seq[AddressPattern],
    vecID:   Seq[Int] = Nil
  ): Seq[AccessMatrix] = {
    val is = accessIterators(access, mem)
    val ps = is.map(_.ctrParOr1)
    val starts = is.map(_.ctrStart)

    dbgs("  Iterators: " + is.indices.map{i => s"${is(i)} (par: ${ps(i)}, start: ${starts(i)})"}.mkString(", "))

    val iMap = is.zipWithIndex.toMap
    val matrix = getAccessCompactMatrix(access, addr, pattern)

    multiLoop(ps).map{uid =>
      val mat = matrix.map{vec =>
        val xsOrig: Seq[Idx] = vec.cols.keys.toSeq
        dbgs(s"    xs: ${xsOrig.mkString(", ")}")
        val components: Seq[(Int,Idx,Int,Option[Idx])] = xsOrig.map{x =>
          val idx = iMap.getOrElse(x,-1)
          if (idx >= 0) {
            dbgs(s"  Iterator: $x")
            val i = Some(is(idx))
            val a = vec(x)*ps(idx)
            val b = vec(x)*uid(idx)
            (a,x,b,i)
          }
          else {
            val i = vec.lastIters(x)
            val xid = uid.dropRight(is.length - i.map(is.indexOf).getOrElse(-1) - 1)
            val a = vec(x)
            val b = 0
            val x2 = unrolled(x, xid)
            dbgs(s"  Other: $x")
            dbgs(s"    Iterators: ${is.mkString(",")}")
            dbgs(s"    Last variant iterator: $i")
            dbgs(s"    Full UID: {${uid.mkString(",")}}")
            dbgs(s"    Unrolling $x {${xid.mkString(",")}} -> $x2")
            (a,x2,b,i)
          }
        }
        val as  = components.map(_._1)
        val xs  = components.map(_._2)
        val c   = components.map(_._3).sum + vec.c
        val lI = components.collectAsMap{case (_,x,_,i) if !i.contains(x) => (x, i): (Idx,Option[Idx]) }  // Scala really doesn't like Option with wildcards
        SparseVector[Idx](xs.zip(as).toMap, c, lI)
      }
      val amat = AccessMatrix(access, mat, uid ++ vecID)
      amat.keys.foreach{x => getOrAddDomain(x) }
      amat
    }.toSeq
  }

}
