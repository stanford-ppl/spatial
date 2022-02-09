package spatial.transform.stream

import argon._
import argon.node.Enabled
import argon.passes.RepeatableTraversal
import spatial.node._
import spatial.lang._
import argon.transform.MutateTransformer
import spatial.metadata.control._

case class UnitpipeCombineTransformer(IR: argon.State) extends MutateTransformer with RepeatableTraversal {
  // When multiple unitpipes appear after each other within a block, we can fuse them into a single unitpipe

  var enStack = Set.empty[Bit]

  def withEns[T](ens: Set[Bit])(thunk: => T): T = {
    val oldStack = enStack
    enStack ++= ens
    val result = thunk
    enStack = oldStack
    result
  }

  def stagePipes(currentPipes: collection.mutable.ListBuffer[Sym[_]]): Unit = {
//    converged = false
    if (currentPipes.isEmpty) return
    if (currentPipes.size == 1) {
      super.visit(currentPipes.head)
      currentPipes.clear()
      return
    }
    dbgs(s"Staging Pipes: ${currentPipes.mkString(", ")}")
    dbgs(s"EnStack: $enStack")
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
            conflicts.isEmpty
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
      lhs2 => transferData(lhs, lhs2)
    }
  }

  def updateEnables(en: Sym[_]): Unit = {
    en.op.get.asInstanceOf[Enabled[_]].updateEn(f, f(enStack))
  }

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = indent {
    (rhs match {
      case foreach: OpForeach =>
        val result = visitForeach(lhs, foreach)
        updateEnables(result)
        result

      case en: Enabled[_] =>
        val enNew = super.transform(lhs, rhs)
        updateEnables(enNew)
        dbgs(s"Updating Enables: $lhs = $rhs -> $enNew = ${enNew.op}")
        enNew
      case _ => super.transform(lhs, rhs)
    }).asInstanceOf[Sym[A]]
  }
}
