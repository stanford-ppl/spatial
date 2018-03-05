package spatial.lang
package static

import core._
import forge.tags._
import utils.Overloads._
import nova.compiler.NovaVirtualization

import spatial.node.IfThenElse

trait SpatialVirtualization extends NovaVirtualization {

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



  /*@rig def __ifThenElse[A:Type](cond: Bit, thenBr: => A, elseBr: => A): A = {
    ifThenElse(cond, () => thenBr, () => elseBr)
  }*/

  @rig private def ifThenElse[A:Type](cond: Bit, thenBr: () => Sym[A], elseBr: () => Sym[A]): A = {
    val blkThen = stageBlock{ thenBr() }
    val blkElse = stageBlock{ elseBr() }
    stage(IfThenElse[A](cond,blkThen,blkElse))
  }

}
