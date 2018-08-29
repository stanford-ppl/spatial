package poly

import utils.implicits.collections._

case class SparseMatrix[K](rows: Seq[SparseVector[K]]) {
  def keys: Set[K] = rows.map(_.cols.keySet).fold(Set.empty)(_++_)
  def replaceKeys(keySwap: Map[K,K]): SparseMatrix[K] = {
    val rows2 = rows.map{r => 
      val cols2 = r.cols.map{case (k,v) => (keySwap.getOrElse(k,k) -> v)}
      SparseVector[K](cols2, r.c, r.lastIters)
    }
    SparseMatrix[K](rows2)
  }

  def sliceDims(dims: Seq[Int]): SparseMatrix[K] = SparseMatrix[K](dims.map{i => rows(i) })

  def map(f: SparseVector[K] => SparseVector[K]): SparseMatrix[K] = SparseMatrix[K](rows.map(f))
  def zip(that: SparseMatrix[K])(func: (Int,Int) => Int): SparseMatrix[K] = {
    val rows2 = this.rows.zip(that.rows).map{case (v1,v2) => v1.zip(v2)(func) }
    SparseMatrix[K](rows2)
  }
  def unary_-(): SparseMatrix[K] = this.map{row => -row}
  def +(that: SparseMatrix[K]): SparseMatrix[K] = this.zip(that){_+_}
  def -(that: SparseMatrix[K]): SparseMatrix[K] = this.zip(that){_-_}
  def increment(key: K, value: Int): SparseMatrix[K] = {
    val rows2 = this.rows.map{r => 
      val cols2 = r.cols.map{case (k,v) => k -> (if (k == key) {v + v*value} else v) }
      SparseVector[K](cols2, r.c, r.lastIters)
    }
    SparseMatrix[K](rows2)
  }
  def incrementConst(value: Int): SparseMatrix[K] = {
    val rows2 = this.rows.map{r => 
      SparseVector[K](r.cols, r.c + value, r.lastIters)
    }
    SparseMatrix[K](rows2)
  }
  private def combs(lol: List[List[SparseVector[K]]]): List[List[SparseVector[K]]] = lol match {
    case Nil => List(Nil)
    case l::rs => for(x <- l;cs <- combs(rs)) yield x::cs
  }
  private def allLoops(maxes: Seq[Int], steps: Seq[Int], iterators: Seq[Int]): Seq[Int] = maxes match {
    case Nil => Nil
    case h::tail if tail.nonEmpty => (0 until h).flatMap{i => allLoops(tail, steps.tail, iterators ++ Seq(i*steps.head))}
    case h::tail if tail.isEmpty => (0 until h).map{i => i*steps.head + iterators.sum}
  }
  private def gcd(a: Int,b: Int): Int = if(b ==0) a else gcd(b, a%b)
  def expand: Seq[SparseMatrix[K]] = {
    val rowOptions = rows.map{row => 
      if (row.mod != 0) {
        val a = row.cols.values.filter(_ != 0)
        val p = a.map{x => row.mod/gcd(row.mod,x)}
        val possible = if (p.toSeq.contains(row.mod)) Seq.tabulate(row.mod){i => i} else allLoops(p.toSeq,a.toSeq,Nil).map(_%row.mod).sorted.distinct
        if (possible.isEmpty) List(SparseVector[K](row.cols,row.c,row.lastIters))
        else possible.map{i => row.empty(i) }.toList
      }
      else {
        List(row)
      }
    }

    combs(rowOptions.toList).map{sm => SparseMatrix[K](sm)}
  }
  def asConstraintEqlZero = ConstraintMatrix(rows.map(_.asConstraintEqlZero).toSet)
  def asConstraintGeqZero = ConstraintMatrix(rows.map(_.asConstraintGeqZero).toSet)
  def collapse: Seq[Int] = rows.map{r => r.cols.values.sum + r.c}

  def >==(b: Int): ConstraintMatrix[K] = ConstraintMatrix(rows.map(_ >== b).toSet)
  def ===(b: Int): ConstraintMatrix[K] = ConstraintMatrix(rows.map(_ === b).toSet)

  override def toString: String = {
    val header = this.keys.toSeq
    val rowStrs = rows.map{row => header.map{k => row(k).toString } :+ row.c.toString :+ row.mod.toString}
    val entries = (header.map(_.toString) :+ "c" :+ "mod") +: rowStrs
    val maxCol = entries.flatMap(_.map(_.length)).maxOrElse(0)
    entries.map{row => row.map{x => " "*(maxCol - x.length + 1) + x }.mkString(" ") }.mkString("\n")
  }
}
object SparseMatrix {
  def empty[K]: SparseMatrix[K] = SparseMatrix[K](Nil)
}