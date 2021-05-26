package spatial.transform

import argon.{Op, SrcCtx, State, Sym, Type, boundVar, stage}
import argon.transform.MutateTransformer
import spatial.lang.{Counter, I32}
import spatial.node.{AccelScope, CounterChainNew, CounterNew, OpForeach}
import spatial.traversal.AccelTraversal
import spatial.metadata.control._
import spatial.metadata.memory._

case class LoopCompaction(IR: State) extends MutateTransformer with AccelTraversal {

  private def isFusible(sym: Sym[_]) = sym match {
    case Op(_:OpForeach) => true
    case _ => false
  }

  // Can transform iff lhs consists of Memories, Transients, Counter info, and a single Foreach
  private def canTransform(lhs: Sym[_], foreach: OpForeach): Boolean = {
    if (foreach.block.stms.count(_.isControl) != 1) return false
    if (!foreach.block.stms.forall {x =>
      x.isPrimitive || x.isMem || x.isControl || x.isCounter || x.isCounterChain
    }) {
      return false
    }
    val ctrl = foreach.block.stms.find(_.isControl).get
    isFusible(ctrl)
  }

  private def mirrorSeq(seq: Seq[Sym[_]]) = {
    seq map {x =>
      val result = mirrorSym(x)
      register(x -> result)
      result
    }
  }

  private def transformForeach(lhs: Sym[_], foreach: OpForeach): Sym[_] = {
    val allStmts = foreach.block.stms
    val targetLoop = allStmts.find(isFusible).flatMap(_.op.map{ case op:OpForeach => op}).get
    val memories = allStmts.filter {_.isMem}
    val chains = allStmts.find(_.isCounterChain).flatMap(_.op.map {case ctr: CounterChainNew => ctr}).get
    val ens = foreach.ens ++ targetLoop.ens
    val oldCounters = foreach.cchain.counters ++ chains.counters
    val newCounters = mirrorSeq(oldCounters) map {_.asInstanceOf[Counter[_]]}
    val cchain = stage(CounterChainNew(newCounters))

    val newIters = cchain.counters.map { ctr =>
      val n = boundVar[I32]
      n.counter = IndexCounterInfo(ctr, Seq.tabulate(ctr.ctrParOr1) { i => i })
      n
    }

    (newIters zip (foreach.iters ++ targetLoop.iters)) foreach {
      case (n, o) => register(o -> n)
    }

    // TODO: If there are multiple stopwhens, what should we do?
    val stopWhen = {
      val tmp = foreach.stopWhen ++ targetLoop.stopWhen
      assert(tmp.size < 2)
      tmp.headOption
    }

    argon.stageWithFlow(OpForeach(ens, cchain, argon.stageBlock {
      mirrorSeq(targetLoop.block.stms)

      spatial.lang.void
    }, newIters, stopWhen)) {
      lhs2 => transferData(lhs, lhs2)
    }
  }

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = {
    rhs match {
      case foreach: OpForeach if inAccel && canTransform(lhs, foreach) => transformForeach(lhs, foreach).asInstanceOf[Sym[A]]

      case _:AccelScope => inAccel {super.transform(lhs, rhs)}
      case _ => super.transform(lhs, rhs)
    }
  }
}
