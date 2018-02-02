package pcc.data

import pcc.core._

case class SparseVector(
  values:   Map[String,Int],
  c:        Int,
  lastIter: Option[String]
) {
  def apply(x: String): Int = if (x == "c") c else if (values.contains(x)) values(x) else 0
  def getOrElse(x: String, v: => Int): Int = if (x == "c") c else values.getOrElse(x,v)
}

case class SparseMatrix(rows: Seq[SparseVector], vecID: Seq[Int] = Nil) {
  def printWithTab(print: String => Unit): Unit = {
    val header = rows.flatMap(_.values.keys).distinct :+ "c"
    val entries = header +: rows.map{row => header.map{k => row(k).toString }}
    val maxCol = (0 +: entries.flatMap(_.map(_.length))).max
  }
}

case class AccessMatrix(
  access: Sym[_],
  matrix: SparseMatrix,
  domain: SparseMatrix,
  unroll: Seq[Int]
) {
  def printWithTab(print: String => Unit): Unit = {

  }
}
