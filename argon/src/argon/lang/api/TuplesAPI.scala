package argon.lang.api

import argon._
import forge.tags._

trait TuplesAPI {

  /** Returns a staged Tuple2 from the given unstaged scala.Tuple2. */
  @api def pack[A:Type,B:Type](t: (A, B)): Tup2[A,B] = Tup2[A,B](t._1, t._2)

  /** Returns a staged Tup2 from the given pair of values. Shorthand for ``pack((a,b))``. */
  @api def pack[A:Type,B:Type](a: A, b: B): Tup2[A,B] = Tup2[A,B](a,b)

  /** Returns an unstaged scala.Tuple2 from this staged Tup2. Shorthand for ``(x._1, x._2)``. */
  @api def unpack[A:Type,B:Type](t: Tup2[A,B]): (A,B) = (t._1, t._2)

  /** Returns a staged Tuple3 from the given unstaged scala.Tuple3. */
  @api def pack[A:Type,B:Type,C:Type](t: (A, B, C)): Tup3[A,B,C] = Tup3[A,B,C](t._1, t._2, t._3)

  /** Returns a staged Tup3 from the given pair of values. Shorthand for ``pack((a,b))``. */
  @api def pack[A:Type,B:Type,C:Type](a: A, b: B, c: C): Tup3[A,B,C] = Tup3[A,B,C](a,b,c)

  /** Returns an unstaged scala.Tuple3 from this staged Tup3. Shorthand for ``(x._1, x._2)``. */
  @api def unpack[A:Type,B:Type,C:Type](t: Tup3[A,B,C]): (A,B,C) = (t._1, t._2, t._3)
}
