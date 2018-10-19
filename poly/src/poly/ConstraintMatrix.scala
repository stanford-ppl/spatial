package poly

import utils.implicits.collections._

case class ConstraintMatrix[K](rows: Set[SparseConstraint[K]], hasDomain: Boolean = false) {
  def keys: Set[K] = rows.flatMap(_.keys)

  def ::(that: SparseConstraint[K]): ConstraintMatrix[K] = ConstraintMatrix[K](rows + that)
  def ::(that: ConstraintMatrix[K]): ConstraintMatrix[K] = ConstraintMatrix[K](this.rows ++ that.rows)

  def domain(implicit isl: ISL): ConstraintMatrix[K] = {
    keys.map{k => isl.domain(k) }.fold(ConstraintMatrix.empty){_::_}
  }

  def replaceKeys(keySwap: Map[K,K]): ConstraintMatrix[K] = {
    val rows2 = rows.map{r => 
      val cols2 = r.cols.map{case (k,v) => (keySwap.getOrElse(k,k) -> v)}
      SparseConstraint[K](cols2, r.c, r.tp)
    }
    ConstraintMatrix[K](rows2, hasDomain)
  }

  def andDomain(implicit isl: ISL): ConstraintMatrix[K] = {
    if (this.hasDomain) this
    else ConstraintMatrix(this.domain.rows ++ rows, hasDomain = true)
  }

  def toDenseString: String = {
    val keys = this.keys.toSeq
    val denseRows = rows.toSeq.map(_.toDenseString(keys))
    s"${denseRows.size} ${keys.size + 3}\n" + denseRows.mkString("\n")
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
    val rowStrs = rows.toSeq.map{row => row.tp.toString +: header.map{k => row(k).toString } :+ row.mod.toString :+ row.c.toString }
    val entries = (" " +: header.map(_.toString) :+ "mod" :+ "c") +: rowStrs
    val maxCol = entries.flatMap(_.map(_.length)).maxOrElse(0)
    entries.map{row => row.map{x => " "*(maxCol - x.length + 1) + x }.mkString(" ") }.mkString("\n")
  }
}
object ConstraintMatrix {
  def empty[K]: ConstraintMatrix[K] = ConstraintMatrix[K](Set.empty)
}
