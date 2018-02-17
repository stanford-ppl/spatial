package nova.poly

case class DenseMatrix(nRows: Int, nCols: Int, rows: Seq[Seq[Int]]) {
  override def toString: String = s"$nRows $nCols\n" + rows.map(_.mkString(" ")).mkString("\n")
}
