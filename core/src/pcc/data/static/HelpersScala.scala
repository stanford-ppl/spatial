package pcc.data.static

/**
  * General helper methods for various data structures in Scala.
  */
trait HelpersScala {

  implicit class SeqHelpers[A](x: Seq[A]) {
    def get(i: Int): Option[A] = if (i >= 0 && i < x.length) Some(x(i)) else None
    def indexOrElse(i: Int, els: => A): A = if (i >= 0 && i < x.length) x(i) else els
  }

  implicit class IterableHelpers[A](x: Iterable[A]) {
    def maxFoldBy[B:Ordering](z: A)(f: A => B): A = if (x.isEmpty) z else x.maxBy(f)
    def minFoldBy[B:Ordering](z: A)(f: A => B): A = if (x.isEmpty) z else x.minBy(f)
  }

}
