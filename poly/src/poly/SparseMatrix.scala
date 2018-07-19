package poly

import utils.implicits.collections._

case class SparseMatrix[K](rows: Seq[SparseVector[K]]) {
  def keys: Set[K] = rows.map(_.cols.keySet).fold(Set.empty)(_++_)

  def sliceDims(dims: Seq[Int]): SparseMatrix[K] = SparseMatrix[K](dims.map{i => rows(i) })

  def map(f: SparseVector[K] => SparseVector[K]): SparseMatrix[K] = SparseMatrix[K](rows.map(f))
  def zip(that: SparseMatrix[K])(func: (Int,Int) => Int): SparseMatrix[K] = {
    val rows2 = this.rows.zip(that.rows).map{case (v1,v2) => v1.zip(v2)(func) }
    SparseMatrix[K](rows2)
  }
  def unary_-(): SparseMatrix[K] = this.map{row => -row}
  def +(that: SparseMatrix[K]): SparseMatrix[K] = this.zip(that){_+_}
  def -(that: SparseMatrix[K]): SparseMatrix[K] = this.zip(that){_-_}
  def asConstraintEqlZero = ConstraintMatrix(rows.map(_.asConstraintEqlZero).toSet)
  def asConstraintGeqZero = ConstraintMatrix(rows.map(_.asConstraintGeqZero).toSet)

  override def toString: String = {
    val header = this.keys.toSeq
    val rowStrs = rows.map{row => header.map{k => row(k).toString } :+ row.c.toString :+ row.modulus.toString}
    val entries = (header.map(_.toString) :+ "c" :+ "mod") +: rowStrs
    val maxCol = entries.flatMap(_.map(_.length)).maxOrElse(0)
    entries.map{row => row.map{x => " "*(maxCol - x.length + 1) + x }.mkString(" ") }.mkString("\n")
  }
}
object SparseMatrix {
  def empty[K]: SparseMatrix[K] = SparseMatrix[K](Nil)
}