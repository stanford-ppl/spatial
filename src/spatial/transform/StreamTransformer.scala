package spatial.transform

import argon._
import argon.node._
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.node._
import spatial.util.shouldMotionFromConditional
import spatial.traversal.AccelTraversal
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.blackbox._

/** Converts Stream Foreach controllers into Stream Unit controllers with the counterchain
  * duplicated and injected directly into children controllers. This removes the overhead of
  * waiting the latency of the child controller every time it tries to increment its copy
  * of the stream parent
  */
case class StreamTransformer(IR: State) extends MutateTransformer with AccelTraversal {

  private def injectCtrs(block: Block[Void], ctrs: Seq[Sym[_]], is: Seq[I32]): Block[Void] = {
    stageBlock{
      block.stms.foreach{
        case x@Op(CounterChainNew(ctrs2)) => 
          val newctrs = ctrs.map{
            case Op(CounterNew(start, stop, step, par)) =>
              stage(CounterNew[I32](start.asInstanceOf[I32], stop.asInstanceOf[I32], step.asInstanceOf[I32], par))
            case Op(ForeverNew()) =>
              stage(ForeverNew())
          }
          val newcchain = stageWithFlow(CounterChainNew(newctrs ++ ctrs2)){lhs2 => transferData(x, lhs2)}
          subst += (x -> newcchain)
          newcchain
        case x@Op(OpForeach(ens, cchain, blk, iters, stopWhen)) if x.isStreamControl && stopWhen.isEmpty && x.isOuterControl =>
          stageWithFlow(UnitPipe(ens, injectCtrs(blk, cchain.counters ++ ctrs, is ++ iters), stopWhen)){lhs2 => transferData(x, lhs2)}
        case x@Op(OpForeach(ens, cchain, blk, iters, stopWhen)) =>
          val newctrs = subst(cchain).asInstanceOf[CounterChain].counters
          val newiters = is.zip(newctrs).map{case (i,ctr) => 
            val n = boundVar[I32]
            subst += (i -> n)
            n.name = i.name
            n.counter = IndexCounterInfo(ctr, Seq.tabulate(ctr.ctrParOr1){i => i})
            n
          }
          stageWithFlow(OpForeach(ens, subst(cchain).asInstanceOf[CounterChain], stageBlock{blk.stms.foreach(visit)}, newiters ++ iters, stopWhen)){lhs2 => transferData(x, lhs2)}
        case x@Op(UnitPipe(ens, blk, stopWhen)) =>
          val newctrs = ctrs.map{
            case Op(CounterNew(start, stop, step, par)) =>
              stage(CounterNew[I32](start.asInstanceOf[I32], stop.asInstanceOf[I32], step.asInstanceOf[I32], par))
            case Op(ForeverNew()) =>
              stage(ForeverNew())
          }
          val newcchain = stageWithFlow(CounterChainNew(newctrs)){lhs2 => transferData(x, lhs2)}
          val newiters = is.zip(newctrs).map{case (i,ctr) => 
            val n = boundVar[I32]
            subst += (i -> n)
            n.name = i.name
            n.counter = IndexCounterInfo(ctr, Seq.tabulate(ctr.ctrParOr1){i => i})
            n
          }
          val lhs3 = stageWithFlow(OpForeach(ens, newcchain, stageBlock{blk.stms.foreach(visit)}, newiters, stopWhen)){lhs2 => transferData(x, lhs2)}
          if (lhs3.isOuterControl) lhs3.userSchedule = Sequenced
        case x: Control[_] => throw new Exception(s"Cannot transform Stream controller with $x (${x.rhs}) child!  Please compile with --leaveStreamCounters flag to skip this transformer.")

        case x => visit(x)
      }
    }
  }

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case AccelScope(_) => inAccel{ super.transform(lhs,rhs) }

    case OpForeach(ens, cchain, block, iters, stopWhen) if inHw && lhs.isStreamControl && lhs.isOuterControl && !stopWhen.isDefined =>
      if (lhs.children.exists{x => x.s.get.isCtrlBlackbox || x.s.get.isBlackboxUse}) {
        warn(s"Optimization for folding the counter chain of a Stream controller into its child counter chains is not supported on VerilogCtrlBlackBoxes!  Optimization ignored")
        super.transform(lhs,rhs)
      } else {
        stageWithFlow(UnitPipe(ens, injectCtrs(block, cchain.counters, iters), stopWhen)){lhs2 => transferData(lhs, lhs2)}
      }

    case _ => dbgs(s"visiting $lhs = $rhs");super.transform(lhs,rhs)
  }

}


