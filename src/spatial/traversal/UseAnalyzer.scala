package spatial.traversal

import argon._
import spatial.data._
import spatial.node._
import spatial.util._

case class UseAnalyzer(IR: State) extends BlkTraversal {

  override protected def preprocess[R](block: Block[R]): Block[R] = {
    pendingUses.reset()
    super.preprocess(block)
  }

  override protected def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = {
    dbgs(s"$lhs = $rhs [ctrl: ${lhs.toCtrl}, inner: ${isInnerControl(lhs.toCtrl)}]")

    metadata.clear[MUsers](lhs)

    if (inHw) checkUses(lhs, rhs)
    if (isEphemeral(lhs)) addPendingUse(lhs)

    if (isControl(lhs)) withCtrl(lhs){ super.visit(lhs,rhs) }
    else super.visit(lhs, rhs)
  }

  override protected def visitBlock[R](block: Block[R]): Block[R] = {
    advanceBlock()
    val result = super.visitBlock(block)
    block.result.blk match {
      case Host =>
      case Controller(ctrl,_) => addUse(ctrl, pendingUses(block.result), blk)
    }
    result
  }


  private def checkUses(lhs: Sym[_], rhs: Op[_]): Unit = {
    dbgs(s"  pending: ${pendingUses.all.mkString(", ")}")
    dbgs(s"  inputs: ${rhs.nonBlockInputs.mkString(", ")}")
    val pending = rhs.nonBlockInputs.flatMap{sym => pendingUses(sym) }
    dbgs(s"  uses: ${pending.mkString(", ")}")
    if (pending.nonEmpty) {
      // All nodes which could potentially use a reader outside of an inner control node
      // Add propagating use if outer or outside Accel
      if (isEphemeral(lhs) && !isInnerControl(lhs.toCtrl)) addPropagatingUse(lhs, pending)
      else addUse(lhs, pending.toSet, blk)
    }
  }

  private def addUse(user: Sym[_], used: Set[Sym[_]], block: Ctrl): Unit = {
    dbgs(s"  Uses:")
    used.foreach{s => dbgs(s"  - ${stm(s)}")}

    used.foreach{node =>
      usersOf(node) = usersOf(node) + User(user, block)

      // Also add stateless nodes that this node uses
      pendingUses(node).filter(_ != node).foreach{used =>
        usersOf(used) = usersOf(used) + User(node,block)
      }
    }
  }

  private def addPropagatingUse(sym: Sym[_], pending: Seq[Sym[_]]): Unit = {
    dbgs(s"  Node is propagating reader of:")
    pending.foreach{s => dbgs(s"  - ${stm(s)}")}
    pendingUses += sym -> (pending.toSet + sym)
  }

  private def addPendingUse(sym: Sym[_]): Unit = if (!pendingUses.all.contains(sym)) {
    dbgs(s"  Adding pending: $sym [ctrl: ${sym.toCtrl}, block: $blk]")
    pendingUses += sym -> Set(sym)
  }

}
