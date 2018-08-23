package argon.static

import forge.tags.rig

import scala.annotation.implicitNotFound

trait Casting {
  @implicitNotFound(msg = "Casting from ${A} to ${B} is undefined.")
  type Cast[A,B] = Either[Cast2Way[B,A],CastFunc[A,B]]
  type Lifting[A,B] = Right[Nothing,Lifter[A,B]]

  object Lifting {
    def apply[A,B:Type]: Lifting[A,B] = Right(new Lifter[A,B])
  }

  implicit class CastOps[A,B](c: Cast[A,B]) {
    @rig def apply(a: A): B = c match {
      case Left(cast)  => cast.applyLeft(a)
      case Right(cast) => cast(a)
    }
    @rig def applyLeft(b: B): Option[A] = c match {
      case Left(cast)  => cast.get(b)
      case Right(cast) => cast.getLeft(b)
    }
    @rig def unchecked(a: A): B = c match {
      case Left(cast)  => cast.uncheckedLeft(a)
      case Right(cast) => cast.unchecked(a)
    }
    @rig def uncheckedLeft(b: B): Option[A] = c match {
      case Left(cast)  => cast.get(b)
      case Right(cast) => cast.uncheckedGetLeft(b)
    }

    def rightType: Type[B] = c match {
      case Left(cast)  => cast.tA
      case Right(cast) => cast.tB
    }
    def leftType: Option[Type[A]] = c match {
      case Left(cast)  => Some(cast.tB)
      case Right(cast) => None
    }
  }

  @rig def cast[A,B](a: A)(implicit cast: Cast[A,B]): B = cast.apply(a)

  implicit class SelfType[A<:Ref[_,_]](x: A) {
    def selfType: A = x.tp.asInstanceOf[A]
  }
}
