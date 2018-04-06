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

  /*@rig def __ifThenElse[A:Type](cond: Bit, thenBr: => Lift[A], elseBr: => Lift[A])(implicit ov0: Overload0): A = {
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
  }*/

  def __ifThenElse(cond: Bit, thenBr: I64, elseBr: I64): I64 = macro ifThenElseI64

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
}

private object SpatialVirtualization {
  def ifThenElseI64(c: whitebox.Context)(cond: c.Expr[Bit], thenBr: c.Expr[I64], elseBr: c.Expr[I64]): c.Expr[I64] = {
    import c.universe._
    c.Expr(q"ifThenElse($cond, () => $thenBr, () => $elseBr)")
  }
}