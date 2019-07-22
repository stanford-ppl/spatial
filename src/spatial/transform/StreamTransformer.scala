package spatial.transform

import argon._
import argon.node._
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.node._
import spatial.util.shouldMotionFromConditional
import spatial.traversal.AccelTraversal
import spatial.metadata.control._

/** Converts Stream Foreach controllers into Stream Unit controllers with the counterchain
  * duplicated and injected directly into children controllers. This removes the overhead of
  * waiting the latency of the child controller every time it tries to increment its copy
  * of the stream parent
  */
case class StreamTransformer(IR: State) extends MutateTransformer with AccelTraversal {

  private def injectCtrs(ctrs: Seq[Counter[_]], blk: Block[Void]): Block[Void] = {
    blk
  }

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case AccelScope(_) => inAccel{ super.transform(lhs,rhs) }

    case OpForeach(ens, cchain, blk, iters, stopWhen) if (inHw && stopWhen.isEmpty && lhs.isStreamControl) =>
      val blk2 = injectCtrs(cchain.counters, blk)
      stageWithFlow(UnitPipe(ens, blk2)){lhs2 => transferData(lhs, lhs2)}

    case _ => dbgs(s"visiting $lhs = $rhs");super.transform(lhs,rhs)
  }

}


