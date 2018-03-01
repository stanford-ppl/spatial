package spatial.lang
package static

import core._
import forge.tags._
import nova.compiler.NovaVirtualization

import spatial.node.IfThenElse

trait SpatialVirtualization extends NovaVirtualization {

  //@rig def __ifThenElse[A](cond: Bit, thenBr: => Top[A], elseBr: => Top[A]): A = {

  @rig def __ifThenElse[A:Type](cond: Bit, thenBr: => Lift[A], elseBr: => Lift[A]): A = {
    ifThenElse(cond, () => thenBr.unbox, () => elseBr.unbox)
  }

  /*@rig def __ifThenElse[A:Type](cond: Bit, thenBr: => A, elseBr: => A): A = {
    ifThenElse(cond, () => thenBr, () => elseBr)
  }*/

  @rig private def ifThenElse[A:Type](cond: Bit, thenBr: () => A, elseBr: () => A): A = {
    val blkThen = stageBlock{ thenBr() }
    val blkElse = stageBlock{ elseBr() }
    stage(IfThenElse[A](cond,blkThen,blkElse))
  }

}
