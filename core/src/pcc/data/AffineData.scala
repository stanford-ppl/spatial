package pcc.data

import pcc.core._
import pcc.lang._

sealed abstract class ConstraintType
case object GEQ_ZERO extends ConstraintType
case object EQL_ZERO extends ConstraintType

case class SparseConstraint(cols: Map[String,Int], c: Int, tp: ConstraintType)

case class SparseVector(
  cols:      Map[String,Int],
  c:         Int,
  lastIters: Map[String,Option[I32]]
) {
  def apply(x: String): Int = if (x == "c") c else if (cols.contains(x)) cols(x) else 0
  def getOrElse(x: String, v: => Int): Int = if (x == "c") c else cols.getOrElse(x,v)

  def asMinConstraint(x: String) = SparseConstraint(cols.map{case (i,a) => i -> -a} ++ Map(x -> 1) , -c, GEQ_ZERO)
  def asMaxConstraint(x: String) = SparseConstraint(cols ++ Map(x -> -1), c, GEQ_ZERO)
}

case class SparseMatrix(rows: Seq[SparseVector]) {
  def printWithTab(print: String => Unit): Unit = {
    val header = rows.flatMap(_.cols.keys).distinct :+ "c"
    val entries = header +: rows.map{row => header.map{k => row(k).toString }}
    val maxCol = (0 +: entries.flatMap(_.map(_.length))).max
    entries.foreach{row =>
      print(row.map{x => " "*(maxCol - x.length + 1) + x }.mkString(" "))
    }
  }

  def map(f: SparseVector => SparseVector): SparseMatrix = SparseMatrix(rows.map(f))
}
object SparseMatrix {
  def empty = SparseMatrix(Nil)
}

case class AccessMatrix(
  access: Sym[_],
  matrix: SparseMatrix,
  domain: SparseMatrix,
  unroll: Seq[Int]
) {
  def printWithTab(print: String => Unit): Unit = {
    print(stm(access) + "[" + unroll.mkString(",") + "]")
    matrix.printWithTab(print)
  }
}
