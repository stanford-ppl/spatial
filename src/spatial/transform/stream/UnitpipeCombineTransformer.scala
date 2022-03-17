package spatial.transform.stream

import argon._
import argon.node.Enabled
import argon.passes.RepeatableTraversal
import spatial.node._
import spatial.lang._
import argon.transform.MutateTransformer
import spatial.metadata.control._
import spatial.metadata.memory._

case class UnitpipeCombineTransformer(IR: argon.State) extends MutateTransformer with RepeatableTraversal {
  // When multiple unitpipes appear after each other within a block, we can fuse them into a single unitpipe

  def stagePipes(currentPipes: collection.mutable.ListBuffer[Sym[_]]): Unit = {
//    converged = false
    if (currentPipes.isEmpty) return
    if (currentPipes.size == 1) {
      super.visit(currentPipes.head)
      currentPipes.clear()
      return
    }
    dbgs(s"Staging Pipes: ${currentPipes.mkString(", ")}")
    val unitBlock = stageBlock {
      currentPipes foreach {
        case Op(UnitPipe(ens, block, _)) =>
          withEns(ens) {
            dbgs(s"Visiting: ${block.stms}")
            indent {
              block.stms.map(visit)
            }
          }
      }
    }
    val newPipe = stage(UnitPipe(Set.empty, unitBlock, None))
    dbgs(s"New Pipe: $newPipe = ${newPipe.op}")
    register(currentPipes.last -> newPipe)
    currentPipes.clear()
  }

  def fuseUnitpipes[T](block: Block[T]): Block[T] = {
    val currentPipes = collection.mutable.ListBuffer.empty[Sym[_]]

    stageBlock {
      block.stms foreach {
        case pipe@Op(UnitPipe(ens, block, breakWhen)) if breakWhen.isEmpty =>
          val canFuse = {
            val previousWrites = currentPipes.flatMap(_.effects.writes).toSet
            val conflicts = previousWrites intersect pipe.effects.reads
            val hasConflict = conflicts.isEmpty
            val writtenStreams = currentPipes.flatMap(_.nestedWrittenMems).filter(_.isFIFO)
            val readStreams = currentPipes.flatMap(_.nestedReadMems).filter(_.isFIFO)

            val mayLoopStreams = writtenStreams.nonEmpty && readStreams.nonEmpty

            hasConflict || mayLoopStreams
          }
          if (canFuse) {
            currentPipes.append(pipe)
          } else {
            // process pipe
            stagePipes(currentPipes)
            currentPipes.append(pipe)
          }
        // Currently consider everything to break the unitpipe.
        // TODO(stanfurd) Fuse Transients
        case sym =>
          stagePipes(currentPipes)
          super.visit(sym)
      }
      f(block.result)
    }
  }

  def visitForeach(lhs: Sym[_], foreach: OpForeach) = {
    dbgs(s"Visiting Foreach: $lhs = $foreach")
    val newBlock = indent { fuseUnitpipes(foreach.block) }
    stageWithFlow(OpForeach(f(foreach.ens), f(foreach.cchain), newBlock, f(foreach.iters), f(foreach.stopWhen))) {
      lhs2 =>
        dbgs(s"Replacing: $lhs -> $lhs2")
        transferData(lhs, lhs2)
    }
  }


  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = indent {
    (rhs match {
      case foreach: OpForeach =>
        val result = visitForeach(lhs, foreach)
        updateNode(result.op.get)
        result

      case _ => super.transform(lhs, rhs)
    }).asInstanceOf[Sym[A]]
  }
}