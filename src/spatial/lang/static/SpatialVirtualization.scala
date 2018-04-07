package spatial.lang
package static

import argon._
import forge.tags._
import utils.Overloads._
import spatial.node.IfThenElse

import language.experimental.macros
import scala.reflect.macros.whitebox

trait LowPriorityVirtualization {

  implicit class EqualsOps(x: Any) {
    def !==(y: Any): Boolean = x != y
    def ===(y: Any): Boolean = x == y
  }

}

trait SpatialVirtualization extends ArgonVirtualization with forge.EmbeddedControls {
  import SpatialVirtualization._

  @rig def __ifThenElse[A:Type](cond: Bit, thenBr: => Lift[A], elseBr: => Lift[A])(implicit ov0: Overload0): A = {
    ifThenElse(cond, () => thenBr.unbox, () => elseBr.unbox)
  }
  @rig def __ifThenElse[A:Type,B](cond: Bit, thenBr: => Sym[A], elseBr: => Lift[B])(implicit ov1: Overload1): A = {
    ifThenElse(cond, () => thenBr, () => Type[A].from(elseBr.orig))
  }
  @rig def __ifThenElse[A,B:Type](cond: Bit, thenBr: => Lift[A], elseBr: => Sym[B])(implicit ov2: Overload2): B = {
    ifThenElse(cond, () => Type[B].from(thenBr.orig), () => elseBr)
  }
  @rig def __ifThenElse[A](cond: Bit, thenBr: => Sym[A], elseBr: => Sym[A])(implicit ov3: Overload3): A = {
    ifThenElse(cond, () => thenBr, () => elseBr)
  }

  @rig def ifThenElse[A](cond: Bit, thenBr: () => Sym[A], elseBr: () => Sym[A]): A = {
    val blkThen = stageBlock{ thenBr() }
    val blkElse = stageBlock{ elseBr() }
    implicit val A: Type[A] = blkThen.tp
    stage(IfThenElse[A](cond,blkThen,blkElse))
  }

  @rig def infix_+[A](x1: String, x2: A): Text = x2 match {
    case t: Top[_] => Text(x1) ++ t.toText
    case _ => Text(x1 + x2)
  }

  @rig def infix_!=[A:Type,B](a: Sym[A], b: Lift[B]): Bit = a.view[Top] !== Type[A].from(b.orig)
  @rig def infix_!=[A,B:Type](a: Lift[A], b: Sym[B]): Bit = Type[B].from(a.orig) !== b
  @rig def infix_!=[A:Type,B:Type](a: Sym[A], b: Sym[B]): Bit = a.view[Top] !== b

  @rig def infix_==[A:Type,B](a: Sym[A], b: Lift[B]): Bit = a.view[Top] === Type[A].from(b.orig)
  @rig def infix_==[A,B:Type](a: Lift[A], b: Sym[A]): Bit = Type[B].from(a.orig) === b
  @rig def infix_==[A:Type,B:Type](a: Sym[A], b: Sym[B]): Bit = a.view[Top] === b
}

private object SpatialVirtualization {
//  def eqlImpl[T](c: whitebox.Context)(x1: c.Expr[Any], x2: c.Expr[Any]): c.Expr[T] = {
//    import c.universe._
//    c.Expr(q"$x1 === $x2")
//  }
//
//  def eqlLiftRightImpl[T](c: whitebox.Context)(x1: c.Expr[Any], x2: c.Expr[Any])(l: c.Tree): c.Expr[T] = {
//    import c.universe._
//    c.Expr(q"$x1 === lift($x2)")
//  }
//
//  def eqlLiftLeftImpl[T](c: whitebox.Context)(x1: c.Expr[Any], x2: c.Expr[Any])(l: c.Tree): c.Expr[T] = {
//    import c.universe._
//    c.Expr(q"lift($x1) === $x2")
//  }
//
//  def neqImpl(c: whitebox.Context)(a: c.Expr[Any], b: c.Expr[Any]): c.Expr[Bit] = {
//    import c.universe._
//    c.Expr(q"$a !== $b")
//  }
//
//  def neqLiftRightImpl(c: whitebox.Context)(x1: c.Expr[Any], x2: c.Expr[Any])(l: c.Tree): c.Expr[Bit] = {
//    import c.universe._
//    c.Expr(q"$x1 !== lift($x2)")
//  }
//
//  def neqLiftLeftImpl[T](c: whitebox.Context)(x1: c.Expr[Any], x2: c.Expr[Any])(l: c.Tree): c.Expr[T] = {
//    import c.universe._
//    c.Expr(q"lift($x1) =!= $x2")
//  }
}