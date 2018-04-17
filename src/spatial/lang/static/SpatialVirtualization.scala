package spatial.lang
package static

import argon._
import forge.tags._
import utils.Overloads._
import spatial.node.IfThenElse

import language.experimental.macros
import scala.reflect.macros.whitebox

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

  def infix_+(x1: String, x2: Any): Text = macro text_plus

  @rig def infix_toString(x: Any): Text = x match {
    case x: Top[_] => x.toText
    case _ => Text(x.toString)
  }
}

private object SpatialVirtualization {
  def text_plus(c: whitebox.Context)(x1: c.Expr[String], x2: c.Expr[Any]): c.Expr[Text] = {
    import c.universe._
    c.Expr(q"Text($x1) + $x2")
  }

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