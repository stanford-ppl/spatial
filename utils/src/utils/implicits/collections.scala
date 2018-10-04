package utils.implicits

import scala.collection.mutable
import scala.reflect.ClassTag
import utils.math.ReduceTree

/**
  * General helper methods for various data structures in Scala.
  */
object collections {

  implicit class IntSeqHelpers(x: Seq[Int]) {
    /** Returns seq containing only elements who are unique in mod N. */
    def uniqueModN(N: Int): Seq[Int] = {
      val map = scala.collection.mutable.ListMap[Int,Int]()
      x.foreach{i => map.getOrElseUpdate(i % N, i) }
      map.values.toList.sorted
    }

  }

  implicit class SeqHelpers[A](x: Seq[A]) {
    def get(i: Int): Option[A] = if (i >= 0 && i < x.length) Some(x(i)) else None
    def indexOrElse(i: Int, els: => A): A = if (i >= 0 && i < x.length) x(i) else els

    private def inter(left: Boolean, x: Seq[A], y: Seq[A]): Seq[A] = (left,x,y) match {
      case (_, Nil, Nil)   => Nil
      case (true, Nil, _)  => y
      case (true, _, _)    => x.head +: inter(!left, x.tail, y)
      case (false, _, Nil) => x
      case (false, _, _)   => y.head +: inter(!left, x, y.tail)
    }

    def reduceTree(reduce: (A,A) => A): A = ReduceTree(x:_*)(reduce)

    def interleave(y: Seq[A])(implicit tag: ClassTag[A]): Seq[A] = inter(left=true,x,y)

    /** Returns a new Map from elements in x to func(x)  */
    def mapping[B](func: A => B): Map[A,B] = x.map{x => x -> func(x) }.toMap

    def collectAsMap[B,C](func: PartialFunction[A,(B,C)]): Map[B,C] = x.collect(func).toMap

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

    def mapFind[B](func: A => Option[B]): Option[B] = {
      var res: Option[B] = None
      val iter = x.iterator
      while (res.isEmpty && iter.hasNext) {
        res = func(iter.next())
      }
      res
    }
  }

  implicit class IterableHelpers[A](x: Iterable[A]) {
    /** Returns a new Map from elements in x to func(x)  */
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
    // abstract type pattern Coll is unchecked since it is eliminated by erasure?
    // def pairs: Iterator[(A,A)] = x match {
    //   case m0 +: y => y.iterator.map{m1 => (m0,m1) } ++ y.pairs
    //   case _ => Iterator.empty
    // }
    def pairs: Iterator[(A,A)] = {
      if (x.size >= 2) (x.tail.iterator.map{m1 => (x.head,m1) } ++ x.tail.pairs) else Iterator.empty
    }

    def mapFind[B](func: A => Option[B]): Option[B] = {
      var res: Option[B] = None
      val iter = x.iterator
      while (res.isEmpty && iter.hasNext) {
        res = func(iter.next())
      }
      res
    }
  }

  implicit class IteratorHelpers[A](x: Iterator[A]) {
    def mapFind[B](func: A => Option[B]): Option[B] = {
      var res: Option[B] = None
      val iter = x
      while (res.isEmpty && iter.hasNext) {
        res = func(iter.next())
      }
      res
    }
  }

  implicit class HashMapOps[K,V](map: mutable.Map[K,V]) {
    def getOrElseAdd(key: K, value: () => V): V = map.get(key) match {
      case Some(v) => v
      case None =>
        val v = value()
        map += (key -> v)
        v
    }
  }

}
