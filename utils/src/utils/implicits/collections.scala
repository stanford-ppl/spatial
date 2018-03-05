package utils.implicits

import scala.collection.mutable

/**
  * General helper methods for various data structures in Scala.
  */
object collections {

  implicit class SeqHelpers[A](x: Seq[A]) {
    def get(i: Int): Option[A] = if (i >= 0 && i < x.length) Some(x(i)) else None
    def indexOrElse(i: Int, els: => A): A = if (i >= 0 && i < x.length) x(i) else els

    /** Returns a new Map from elements in x to func(x)  **/
    def mapping[B](func: A => B): Map[A,B] = x.map{x => x -> func(x) }.toMap

    /** Returns true if the length of x is exactly len, false otherwise.
      * Equivalent to (but faster than) x.length == len
      */
    def lengthIs(len: Int): Boolean = x.lengthCompare(len) == 0

    /** Returns true if length of x is less than len, false otherwise.
      * Equivalent to (but faster than) x.length < len
      */
    def lengthLessThan(len: Int): Boolean = x.lengthCompare(len) < 0

    /** Returns true if length of x is more than len, false otherwise
      * Equivalent to (but faster than) x.length > len
      */
    def lengthMoreThan(len: Int): Boolean = x.lengthCompare(len) > 0
  }

  implicit class IterableHelpers[A](x: Iterable[A]) {
    /** Returns a new Map from elements in x to func(x)  **/
    def mapping[B](func: A => B): Map[A,B] = x.map{x => x -> func(x) }.toMap

    def maxByOrElse[B:Ordering](z: A)(f: A => B): A = if (x.isEmpty) z else x.maxBy(f)
    def minByOrElse[B:Ordering](z: A)(f: A => B): A = if (x.isEmpty) z else x.minBy(f)

    def minOrElse(z: A)(implicit o: Ordering[A]): A = if (x.isEmpty) z else x.min
    def maxOrElse(z: A)(implicit o: Ordering[A]): A = if (x.isEmpty) z else x.max

    def cross[B](y: Iterable[B]): Iterator[(A,B)] = {
      x.iterator.flatMap{a => y.iterator.map{b => (a,b) } }
    }

    /**
      * Returns true if the given function is true over all combinations of 2 elements
      * in this collection.
      */
    def forallPairs(func: (A,A) => Boolean): Boolean = x.pairs.forall{case (a,b) => func(a,b) }

    /**
      * Returns an iterator over all combinations of 2 from this iterable collection.
      * Assumes that either the collection has (functionally) distinct elements.
      */
    def pairs: Iterator[(A,A)] = x match {
      case m0 :: y => y.iterator.map{m1 => (m0,m1) } ++ y.pairs
      case _ => Iterator.empty
    }
  }

  implicit class HashMapOps[K,V](map: mutable.HashMap[K,V]) {
    def getOrElseAdd(key: K, value: () => V): V = map.get(key) match {
      case Some(v) => v
      case None =>
        val v = value()
        map += (key -> v)
        v
    }
  }

}
