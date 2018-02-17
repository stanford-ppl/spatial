package nova.poly

import forge.tags._
import forge.implicits.collections._
import nova.data._
import nova.lang.I32

case class ConstraintMatrix(rows: Set[SparseConstraint], hasDomain: Boolean = false) {
  def keys: Set[I32] = rows.flatMap(_.keys)

  def ::(that: SparseConstraint): ConstraintMatrix = ConstraintMatrix(rows + that)
  def ::(that: ConstraintMatrix): ConstraintMatrix = ConstraintMatrix(this.rows ++ that.rows)

  @stateful def domain: ConstraintMatrix = keys.map{x => domainOf(x) }.fold(ConstraintMatrix.empty){_::_}

  @stateful def andDomain: ConstraintMatrix = {
    if (this.hasDomain) this
    else ConstraintMatrix(this.domain.rows ++ rows, hasDomain = true)
  }

  def toDenseString: String = {
    val keys = this.keys.toSeq
    val denseRows = rows.toSeq.map(_.toDenseString(keys))
    s"${denseRows.size} ${keys.size + 2}\n" + denseRows.mkString("\n")
  }
  def toDenseMatrix: DenseMatrix = {
    val keys = this.keys.toSeq
    val denseRows = rows.toSeq.map(_.toDenseVector(keys))
    DenseMatrix(denseRows.length, keys.length, denseRows)
  }

  def isEmpty(implicit isl: ISL): Boolean = isl.isEmpty(this)
  def nonEmpty(implicit isl: ISL): Boolean = isl.nonEmpty(this)

  override def toString: String = {
    val header = this.keys.toSeq
    val rowStrs = rows.toSeq.map{row => row.tp.toString +: header.map{k => row(k).toString } :+ row.c.toString }
    val entries = (" " +: header.map(_.toString) :+ "c") +: rowStrs
    val maxCol = entries.flatMap(_.map(_.length)).maxOrElse(0)
    entries.map{row => row.map{x => " "*(maxCol - x.length + 1) + x }.mkString(" ") }.mkString("\n")
  }
}
object ConstraintMatrix {
  def empty: ConstraintMatrix = ConstraintMatrix(Set.empty)
}
