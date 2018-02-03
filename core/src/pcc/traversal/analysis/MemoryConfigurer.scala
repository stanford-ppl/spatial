package pcc.traversal
package analysis

import pcc.core._
import pcc.data._
import pcc.lang._
import pcc.util.multiLoop

import scala.collection.mutable

case class MemoryConfigurer[C[_]](mem: Mem[_,C], strategy: BankingStrategy)(implicit state: State) {
  private val rank: Int = rankOf(mem)
  private var randCtr: Int = -1
  private val domains = mutable.HashMap[String,Seq[SparseConstraint]]()
  private val unrolls = mutable.HashMap[(String,Seq[Int]),String]()

  private def nextRand(): String = { randCtr += 1; s"r$randCtr" }
  private def nextRand(x: String): String = {
    randCtr += 1
    val x2 = s"${x}_$randCtr"
    domains.get(x).foreach{d => domains += (x2 -> d) }
    x2
  }

  /**
    * If the access pattern is representable as an affine access for bound, returns
    * a min or max constraint for x based on this bound.
    * Otherwise returns None.
    */
  private def constraint(x: I32, bound: I32, isMin: Boolean): Option[SparseConstraint] = {
    val vec = accessPatternOf.get(bound).flatMap(_.head.getSparseVector.map(_._1))
    if (isMin) vec.map(_.asMinConstraint(x.toString)) else vec.map(_.asMaxConstraint(x.toString))
  }

  /**
    * Returns a SparseMatrix representing the minimum and maximum bounds of this symbol.
    * TODO: Account for bounds of random values
    */
  private def getOrAddDomain(x: I32): Seq[SparseConstraint] = domains.getOrElseUpdate(x.toString, {
    if (ctrOf.get(x).isDefined) {
      val min = constraint(x, x.ctrStart, isMin = true)
      val max = constraint(x, x.ctrEnd, isMin = false)
      min.toSeq ++ max
    }
    else Nil
  })

  private def unrolled(x: String, id: Seq[Int]): String = unrolls.getOrElseUpdate((x,id), nextRand(x))

  def getAccessCompactMatrix(access: Sym[_], addr: Seq[I32]): SparseMatrix = {
    def idx(d: Int): String = addr.get(d) match {
      case Some(x) =>
        getOrAddDomain(x)
        x.toString
      case None => nextRand()
    }

    val aps = accessPatternOf(access)
    val rows = aps.zipWithIndex.map { case (ap, d) => ap.getSparseVector match {
      case Some((vec, xs)) =>
        xs.foreach(getOrAddDomain)
        vec
      case None =>
        val x = idx(d)
        SparseVector(Map(x -> 1), 0, Map(x -> ap.last))
    }}
    SparseMatrix(rows)
  }

  def getUnrolledMatrix(access: Sym[_], addr: Seq[I32], vecID: Seq[Int] = Nil): Seq[AccessMatrix] = {
    val is = accessIterators(access, mem)
    val ps = is.map(_.ctrParOr1)
    val is_str = is.map(_.toString)
    val iMap = is_str.zipWithIndex.toMap
    val matrix = getAccessCompactMatrix(access, addr)

    multiLoop(ps).map{uid =>
      val mat = matrix.map{vec =>
        val xsOrig: Seq[String] = vec.cols.keys.toSeq
        val components: Seq[(Int,String,Int,Option[I32])] = xsOrig.map{x =>
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
        val lI = components.collect{case (a,x,b,i) if !i.exists(_.toString == x) => x -> i }.toMap
        SparseVector(xs.zip(as).toMap, c, lI)
      }
      AccessMatrix(access, mat, SparseMatrix.empty, uid ++ vecID)
    }.toSeq
  }

}
