package argon

import forge.tags._

import scala.annotation.implicitNotFound
import scala.reflect.{ClassTag, classTag}

/** Describes conversion from type A to staged type B.
  *
  * val a: A
  * val b: B
  * val cast: Cast[A,B]
  *
  * val b' = cast.apply(a)
  * val a' = cast.getLeft(b) // May be None
  */
abstract class CastFunc[A,B:Type] {
  def tB: Type[B] = Type[B]
  @rig def apply(a: A): B
  @rig def get(a: A): Option[B] = Some(apply(a))
  @rig def getLeft(b: B): Option[A] = None
}

/** Describes conversion from type A to staged type B and vice versa.
  *
  * val a: A
  * val b: B
  * val cast: Cast2Way[A,B]
  *
  * val b' = cast.apply(a)
  * val a' = cast.applyLeft(b)
  */
abstract class Cast2Way[A,B:Type] extends CastFunc[A,B] {
  @rig def apply(a: A): B
  @rig def applyLeft(b: B): A
  @rig override def getLeft(b: B): Option[A] = Some(applyLeft(b))
}


/**
  * Describes conversion from unstaged type A to staged type B.
  */
class Lifter[A,B:Type] extends CastFunc[A,B] {
  @rig def apply(a: A): B = tB.from(a, checked = false)
}

/**
  * Used when no other evidence exists for how to lift an unstaged type.
  * Unstaged types should have at most one implicit Lift typeclass instance.
  *
  * E.g. staged statement:
  *   if (c) 1 else 0
  *
  *   Needs implicit evidence for what type scala.Int should be lifted to.
  */
class Lift[B:Type](orig: Any, b: B) {
  def unbox: B = b
  def B: Type[B] = Type[B]
}

