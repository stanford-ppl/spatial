package spatial.transform

import argon._
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.node._
import spatial.traversal.AccelTraversal

/** Converts Metapipelined controllers into streamed controllers.
  */
case class UnitPipeToForeachTransformer(IR: State) extends MutateTransformer with AccelTraversal {

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = (rhs match {
    case AccelScope(_) => inAccel{ super.transform(lhs,rhs) }

    case pipe@UnitPipe(ens, block, stopWhen) if inHw =>
      dbgs(s"Transforming ${lhs} = ${rhs}")
      val ctr = stage(CounterNew(I32(0), I32(1), I32(1), I32(1)))
      val ccnew = stage(CounterChainNew(Seq(ctr)))
      val iter = boundVar[I32]
      iter.counter = IndexCounterInfo(ctr, Seq(0))

      stageWithFlow(OpForeach(
        ens, ccnew, stageBlock {
          block.stms.foreach(visit)
        }, Seq(iter), stopWhen
      )){ lhs2 =>
        transferData(lhs, lhs2)
        lhs2.userSchedule = Pipelined
      }

    case _ => super.transform(lhs,rhs)
  }).asInstanceOf[Sym[A]]

}


