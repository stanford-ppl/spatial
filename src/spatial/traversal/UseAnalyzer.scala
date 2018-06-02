package spatial.traversal

import argon._
import spatial.data._
import spatial.node._
import spatial.util._

case class UseAnalyzer(IR: State) extends BlkTraversal {
  var boundSyms: Set[Sym[_]] = Set.empty

  override protected def preprocess[R](block: Block[R]): Block[R] = {
    pendingUses.reset()
    super.preprocess(block)
  }

  override protected def postprocess[R](block: Block[R]): Block[R] = {
    def isUnusedRead(read: Sym[_]): Boolean = {
      if (read.isEphemeral) read.users.isEmpty
      else read.consumers.isEmpty
    }

    localMems.all.foreach{mem =>
      if (mem.isReg && (mem.readers.isEmpty || mem.readers.forall(isUnusedRead))) {
        mem.isUnusedMemory = true
        if (mem.name.isDefined) {
          warn(mem.ctx, s"${mem.name.get} is defined here but never read. Unused writes will be dropped.")
          warn(mem.ctx)
        }
      }
      else if (mem.readers.isEmpty || mem.readers.forall(isUnusedRead)) {
        if (mem.name.isDefined) {
          warn(mem.ctx, s"${mem.name.get} is defined here but never read.")
          warn(mem.ctx)
        }
      }
    }
    super.postprocess(block)
  }

  override protected def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = {
    dbgs(s"$lhs = $rhs [ctrl: ${lhs.toCtrl}, inner: ${lhs.toCtrl.isInnerControl}]")

    metadata.clear[Users](lhs)

    def inspect(): Unit = {
      if (inHw) checkUses(lhs, rhs)
      if (lhs.isEphemeral) addPendingUse(lhs)
      super.visit(lhs, rhs)
    }

    if (lhs.isControl) inCtrl(lhs){ inspect() } else inspect()
  }

  override protected def visitBlock[R](block: Block[R]): Block[R] = {
    val saveBounds = boundSyms
    boundSyms ++= block.inputs

    advanceBlk()
    block.result.blk match {
      case Blk.Host =>
      case Blk.Node(ctrl,_) => addUse(ctrl, block.inputs.toSet, blk)
    }

    val result = super.visitBlock(block)
    block.result.blk match {
      case Blk.Host =>
      case Blk.Node(ctrl,_) => addUse(ctrl, pendingUses(block.result), blk)
    }

    boundSyms = saveBounds
    result
  }


  private def checkUses(lhs: Sym[_], rhs: Op[_]): Unit = {
    dbgs(s"  pending: ${pendingUses.all.mkString(", ")}")
    dbgs(s"  inputs: ${rhs.nonBlockExpInputs.mkString(", ")}")
    val pending = rhs.nonBlockExpInputs.flatMap{sym => pendingUses(sym) }
    dbgs(s"  uses: ${pending.mkString(", ")}")
    if (pending.nonEmpty) {
      // All nodes which could potentially use a reader outside of an inner control node
      // Add propagating use if outer or outside Accel
      if (lhs.isEphemeral && !lhs.toCtrl.isInnerControl) addPropagatingUse(lhs, pending.toSet)
      else addUse(lhs, pending.toSet, blk)
    }
  }

  /** Mark the given stateless symbols as being consumed by a user (sync).
    * @param user Consumer symbol
    * @param used Consumed symbol(s)
    * @param block The control block this use occurs in
    */
  private def addUse(user: Sym[_], used: Set[Sym[_]], block: Blk): Unit = {
    dbgs(s"  Uses [Block: $block]:")
    used.foreach{s => dbgs(s"  - ${stm(s)}")}

    // Bound symbols should always be the result of a block if they are defined elsewhere
    (used diff boundSyms).foreach{use =>
      use.users += User(user, block)

      // Also add stateless nodes that this node uses
      (pendingUses(use) - use).foreach{pend => pend.users += User(use, block) }
    }
  }

  private def addPropagatingUse(sym: Sym[_], pending: Set[Sym[_]]): Unit = {
    dbgs(s"  Node is propagating reader of:")
    pending.foreach{s => dbgs(s"  - ${stm(s)}")}
    pendingUses += sym -> (pending + sym)
  }

  private def addPendingUse(sym: Sym[_]): Unit = if (!pendingUses.all.contains(sym)) {
    dbgs(s"  Adding pending: $sym [ctrl: ${sym.toCtrl}, block: $blk]")
    pendingUses += sym -> Set(sym)
  }

}
