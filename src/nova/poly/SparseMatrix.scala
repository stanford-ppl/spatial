package nova.poly

import forge.implicits.collections._

case class SparseMatrix(rows: Seq[SparseVector]) {
  def keys: Set[Idx] = rows.map(_.cols.keySet).fold(Set.empty)(_++_)

  def sliceDims(dims: Seq[Int]): SparseMatrix = SparseMatrix(dims.map{i => rows(i) })

  def map(f: SparseVector => SparseVector): SparseMatrix = SparseMatrix(rows.map(f))
  def zip(that: SparseMatrix)(func: (Int,Int) => Int): SparseMatrix = {
    val rows2 = this.rows.zip(that.rows).map{case (v1,v2) => v1.zip(v2)(func) }
    SparseMatrix(rows2)
  }
  def unary_-(): SparseMatrix = this.map{row => -row}
  def +(that: SparseMatrix): SparseMatrix = this.zip(that){_+_}
  def -(that: SparseMatrix): SparseMatrix = this.zip(that){_-_}
  def asConstraintEqlZero = ConstraintMatrix(rows.map(_.asConstraintEqlZero).toSet)
  def asConstraintGeqZero = ConstraintMatrix(rows.map(_.asConstraintGeqZero).toSet)

  override def toString: String = {
    val header = this.keys.toSeq
    val rowStrs = rows.map{row => header.map{k => row(k).toString } :+ row.c.toString }
    val entries = (header.map(_.toString) :+ "c") +: rowStrs
    val maxCol = entries.flatMap(_.map(_.length)).maxOrElse(0)
    entries.map{row => row.map{x => " "*(maxCol - x.length + 1) + x }.mkString(" ") }.mkString("\n")
  }
}
object SparseMatrix {
  def empty: SparseMatrix = SparseMatrix(Nil)
}