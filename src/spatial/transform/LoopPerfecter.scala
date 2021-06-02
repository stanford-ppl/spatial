package spatial.transform

import argon._
import argon.node.Enabled
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.traversal.AccelTraversal

/** Performs loop perfection as mentioned by Pu. et al in the Halide on FPGA paper.
  * Loop perfection takes a sequence of instructions, where there is at most one controller. All others must be
  * unitpipes.
  */
case class LoopPerfecter(IR: State) extends MutateTransformer with AccelTraversal {

  val enStack = scala.collection.mutable.Stack[scala.collection.Set[Bit]]()

  private def shouldTransform(sequence: Seq[Sym[_]]): Boolean = {
    // should transform IFF sequence is promote-able to a single looped controller.
    // This means that there is exactly 1 loop controller, and an arbitrary number of unitpipes.
    if (sequence.count(_.isLoopControl) != 1) {
      return false
    }
    val looped = sequence.find(_.isLoopControl).get.asInstanceOf[Sym[Void]]
    if (looped.hasStreamAncestor) {
      return false
    }
    looped match {
      case Op(_:OpForeach) => true
      case _ => false
    }
  }

  override def mirrorNode[A](rhs: Op[A]): Op[A] = {
    rhs match {
      case en: Enabled[A] =>
        en.mirrorEn(f, enStack.flatten.toSet)
      case _ =>
        super.mirrorNode(rhs)
    }
  }

  private def withEns[T](ens: Set[Bit])(v: => T) = {
    enStack.push(ens)
    val r = v
    enStack.pop()
    r
  }

  private def mirrorSeq(seq: Seq[Sym[_]]) = {
    seq map {x =>
      val result = mirrorSym(x)
      register(x -> result)
      result
    }
  }

  private def destructBlocks(seq: Seq[Sym[_]]) = {
    seq flatMap {
      sym =>
        if (sym.isTransient || sym.isMem) { Seq(sym) } else sym.blocks flatMap {_.stms}
    }
  }

  private def transformSequence(preTarget: Seq[Sym[_]], targetLoop: Sym[_], postTarget: Seq[Sym[_]]) = {
    targetLoop match {
      case Op(OpForeach(ens, cchain, block, iters, stopWhen)) if ens.isEmpty =>
        // Mirror the cchain
        val newCChain = spatial.util.TransformUtils.expandCounterPars(cchain)
        val newiters = newCChain.counters.map { ctr =>
          val n  = boundVar[I32]
          n.counter = IndexCounterInfo(ctr, Seq.tabulate(ctr.ctrParOr1) { i => i })
          n
        }

        val ctrPars = cchain.counters map {_.ctrParOr1}
        val replicas = spatial.util.computeShifts(ctrPars)

        stage(OpForeach(ens, newCChain, stageBlock {
          val isFirst = spatial.util.TransformUtils.isFirstIter(newiters, newCChain)

          withEns(isFirst.toSet) {
            mirrorSeq(preTarget)
          }

          // Need to unroll this to maintain correctness
          replicas foreach {
            replica =>
              isolateSubst() {
                val shifts = (cchain.counters zip replica) map {
                  case (ctr, shift) =>
                    implicit def numEV: Num[ctr.CT] = ctr.CTeV.asInstanceOf[Num[ctr.CT]]
                    implicit def castEV: Cast[ctr.CT, I32] = argon.lang.implicits.numericCast[ctr.CT, I32]
                    ctr.step.asInstanceOf[ctr.CT].to[I32] * I32(shift)
                }
                (iters zip newiters zip shifts) foreach {
                  case ((olditer, newiter), shift) =>
                    register(olditer -> (newiter + shift))
                }
                mirrorSeq(block.stms)
              }
          }

          val isLastIteration = spatial.util.TransformUtils.isLastIter(newiters, newCChain)
          withEns(isLastIteration.toSet) {
            mirrorSeq(postTarget)
          }
          spatial.lang.void
        }, newiters, f(stopWhen)))
    }
  }

  private def transformForeach(foreach: OpForeach): Sym[_] = {
    val sequence = foreach.block.stms.toIndexedSeq
    val targetIndex = sequence.indexWhere(_.isLoopControl)
    val targetLoop = sequence(targetIndex)
    val preTarget = sequence.take(targetIndex)
    val postTarget = sequence.drop(targetIndex + 1)
    dbgs(s"Pre: $preTarget, target: $targetLoop, post: $postTarget")
    // Extract all counter-related things
    val (chains, actual) = preTarget.partition {x => x.isCounter || x.isCounterChain}
    val preDestructed = destructBlocks(actual)
    stage(OpForeach(
      foreach.ens, foreach.cchain, stageBlock {
        mirrorSeq(chains)
        dbgs(s"Chains: $chains -> ${f(chains)}")
        transformSequence(preDestructed, targetLoop, destructBlocks(postTarget))
      }, foreach.iters, foreach.stopWhen
    ))
  }

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
     case _: AccelScope => inAccel{ super.transform(lhs, rhs) }

     case foreach:OpForeach if lhs.isOuterControl && shouldTransform(foreach.block.stms) =>
       dbgs(s"Transforming: $lhs = $rhs")
       val transformed = transformForeach(foreach).asInstanceOf[Sym[A]]
       super.transform(transformed, transformed.op.get)

    case _ =>
      dbgs(s"Skipping: $lhs = $rhs")
      super.transform(lhs,rhs)
  }

}


