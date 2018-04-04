package spatial.lang.static

import argon._
import forge.tags._

import spatial.lang._

trait StaticTuples {

  /** Returns a staged Tuple2 from the given unstaged scala.Tuple2. */
  @api def pack[A:Type,B:Type](t: (A, B)): Tup2[A,B] = Tup2[A,B](t._1, t._2)

  /** Returns a staged Tup2 from the given pair of values. Shorthand for ``pack((a,b))``. */
  @api def pack[A:Type,B:Type](a: A, b: B): Tup2[A,B] = Tup2[A,B](a,b)

  /** Returns an unstaged scala.Tuple2 from this staged Tup2. Shorthand for ``(x._1, x._2)``. */
  @api def unpack[A:Type,B:Type](t: Tup2[A,B]): (A,B) = (t._1, t._2)

}
