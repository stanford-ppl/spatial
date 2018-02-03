package pcc.data

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
