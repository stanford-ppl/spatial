package spatial.transform.unrolling

import argon._
import spatial.lang._
import spatial.node._
import spatial.metadata.control._

trait SwitchUnrolling extends UnrollingBase {

  override def unroll[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): List[Sym[_]] = rhs match {
    case _:Switch[_] if lhs.isInnerControl => lanes.duplicate(lhs, rhs)
    case _ => super.unroll(lhs, rhs)
  }

}
