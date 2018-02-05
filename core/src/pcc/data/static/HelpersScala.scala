package pcc.data.static

/**
  * General helper methods for various data structures in Scala.
  */
trait HelpersScala {

  implicit class SeqHelpers[A](x: Seq[A]) {
    def get(i: Int): Option[A] = if (i >= 0 && i < x.length) Some(x(i)) else None
    def indexOrElse(i: Int, els: => A): A = if (i >= 0 && i < x.length) x(i) else els

    /**
      * Returns true if the length of x is exactly len, false otherwise.
      * Equivalent to (but faster than) x.length == len
      */
    def lengthIs(len: Int): Boolean = x.lengthCompare(len) == 0

    /**
      * Returns true if length of x is less than len, false otherwise.
      * Equivalent to (but faster than) x.length < len
      */
    def lengthLessThan(len: Int): Boolean = x.lengthCompare(len) < 0

    /**
      * Returns true if length of x is more than len, false otherwise
      * Equivalent to (but faster than) x.length > len
      */
    def lengthMoreThan(len: Int): Boolean = x.lengthCompare(len) > 0
  }

  implicit class IterableHelpers[A](x: Iterable[A]) {
    def maxFoldBy[B:Ordering](z: A)(f: A => B): A = if (x.isEmpty) z else x.maxBy(f)
    def minFoldBy[B:Ordering](z: A)(f: A => B): A = if (x.isEmpty) z else x.minBy(f)
  }

}
