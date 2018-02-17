package nova.implicits

import forge.implicits.readable._
import nova.core._

object views {
  implicit class ViewOps[A](x: A) {
    def view[B[_]<:Type[_]](implicit tp: B[A]): B[A] = x match {
      case t: Top[_] if t.tp <:< tp && !t.isType => x.asInstanceOf[B[A]]
      case t: Top[_] if t.isType => throw new Exception(s"Cannot view $x as a $tp ($x is a Type)")
      case t: Top[_] => throw new Exception(s"Cannot view $x as a $tp ($x has type ${t.tp})")
      case _ => throw new Exception(r"Cannot view $x as a $tp ($x is a ${x.getClass}, not a symbol)")
    }
    def viewSym(implicit tp: Type[A]): Sym[A] = x match {
      case t: Top[_] if t.tp <:< tp && !t.isType => tp.viewAsSym(x)
      case t: Top[_] if t.isType => throw new Exception(s"Cannot view $x as a Sym[$tp] ($x is a Type)")
      case t: Top[_] => throw new Exception(s"Cannot view $x as a Sym[$tp] ($x has type ${t.tp})")
      case _ => throw new Exception(r"Cannot view $x as a Sym[$tp] ($x is a ${x.getClass}, not a symbol)")
    }
    def viewTop(implicit tp: Type[A]): Top[A] = x match {
      case t: Top[_] if t.tp <:< tp && !t.isType => tp.viewAsTop(x)
      case t: Top[_] if t.isType => throw new Exception(s"Cannot view $x as a Top[$tp] ($x is a Type)")
      case t: Top[_] => throw new Exception(s"Cannot view $x as a Top[$tp] ($x has type ${t.tp})")
      case _ => throw new Exception(s"Cannot view $x as a Top ($x is a ${x.getClass}, not a Top)")
    }
  }

}
