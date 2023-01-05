package spatial.traversal

import argon._
import argon.lang.Idx
import argon.passes.Traversal
import spatial.lang.Counter
import spatial.metadata.control._

case class CounterIterSynchronization(IR: State) extends Traversal {
  override protected def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = lhs match {
    case ctr: Counter[_] =>
      val iter = ctr.iter.get.asInstanceOf[Sym[Idx]].unbox
      iter.counter = IndexCounterInfo(ctr.asInstanceOf[Counter[ctr.CT]], Seq.tabulate(ctr.ctrParOr1){i => i})
      dbgs(s"Updating counter and iter info: $ctr -> $iter")
    case _ => super.visit(lhs, rhs)
  }
}
