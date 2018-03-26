package spatial.lang
package static

import argon.{ArgonVirtualization, _}
import forge.tags._
import utils.Overloads._
import spatial.node.IfThenElse

trait SpatialVirtualization extends ArgonVirtualization {

  @rig def __ifThenElse[A:Type](cond: Bit, thenBr: => Lift[A], elseBr: => Lift[A])(implicit ov0: Overload0): A = {
    ifThenElse(cond, () => thenBr.unbox, () => elseBr.unbox)
  }
  @rig def __ifThenElse[A:Type,B](cond: Bit, thenBr: => Sym[A], elseBr: => Lift[B])(implicit ov1: Overload1): A = {
    ifThenElse(cond, () => thenBr, () => Type[A].from(elseBr.orig))
  }
  @rig def __ifThenElse[A,B:Type](cond: Bit, thenBr: => Lift[A], elseBr: => Sym[B])(implicit ov2: Overload2): B = {
    ifThenElse(cond, () => Type[B].from(thenBr.orig), () => elseBr)
  }
  @rig def __ifThenElse[A:Type](cond: Bit, thenBr: => Sym[A], elseBr: => Sym[A])(implicit ov3: Overload3): A = {
    ifThenElse(cond, () => thenBr, () => elseBr)
  }

  @rig private def ifThenElse[A:Type](cond: Bit, thenBr: () => Sym[A], elseBr: () => Sym[A]): A = {
    val blkThen = stageBlock{ thenBr() }
    val blkElse = stageBlock{ elseBr() }
    stage(IfThenElse[A](cond,blkThen,blkElse))
  }


  @rig def infix_+[A](x1: String, x2: A): Text = x2 match {
    case t: Top[_] => Text(x1) ++ t.toText
    case _ => Text(x1 + x2)
  }
}
