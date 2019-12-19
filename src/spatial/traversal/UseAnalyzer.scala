package spatial.traversal

import argon._
import spatial.node._
import spatial.metadata.PendingUses
import spatial.metadata.access._
import spatial.metadata.control._
import spatial.metadata.memory._

case class UseAnalyzer(IR: State) extends BlkTraversal {
  var boundSyms: Set[Sym[_]] = Set.empty

  override protected def preprocess[R](block: Block[R]): Block[R] = {
    PendingUses.reset()
    super.preprocess(block)
  }

  override protected def postprocess[R](block: Block[R]): Block[R] = {
    def isUnusedRead(read: Sym[_]): Boolean = {
      if (read.isTransient) read.users.isEmpty
      else read.consumers.isEmpty
    }

    LocalMemories.all.foreach{mem =>
      if (mem.isReg && ((mem.readers.isEmpty || mem.readers.forall(isUnusedRead)) && !mem.isBreaker)) {
        mem.isUnusedMemory = true
        if (mem.name.isDefined) {
          warn(mem.ctx, s"${mem.name.get} is defined here but never read. Unused writes will be dropped.")
          warn(mem.ctx)
        }
      }
      else if (!mem.isStreamOut && (mem.readers.isEmpty || mem.readers.forall(isUnusedRead)) && !mem.isBreaker) {
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
      if (lhs.isTransient | lhs.isCounter) addPendingUse(lhs)
      super.visit(lhs, rhs)
    }

    if (lhs.isControl) {
      lhs.transientReadMems = Set()
      lhs match {
        case Op(OpForeach(_,_,_,_,Some(breakWhen))) => breakWhen.isBreaker = true
        case Op(OpReduce(_,_,_,_,_,_,_,_,_,_,Some(breakWhen))) => breakWhen.isBreaker = true
        case Op(OpMemReduce(_,_,_,_,_,_,_,_,_,_,_,_,_,Some(breakWhen))) => breakWhen.isBreaker = true 
        case Op(UnrolledForeach(_,_,_,_,_,Some(breakWhen))) => breakWhen.isBreaker = true
        case Op(UnrolledReduce(_,_,_,_,_,Some(breakWhen))) => breakWhen.isBreaker = true
        case _ => 
      }
      inCtrl(lhs){ inspect() } 
    } else inspect()
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
      case Blk.Node(ctrl,_) => addUse(ctrl, PendingUses(block.result), blk)
    }

    boundSyms = saveBounds
    result
  }

  private def blkOfUser(x: Sym[_], block: Blk): Blk = {
    x match {
      case s if s.isControl => Blk.Node(s,-1)
      case s if (s.isCounter || s.isCounterChain) && s.getOwner.isDefined => Blk.Node(s.owner,-1)
      case _ => block
    }
  }

  private def checkUses(lhs: Sym[_], rhs: Op[_]): Unit = {
    dbgs(s"  Pending: ${PendingUses.all.mkString(", ")}")
    dbgs(s"  Inputs:  ${rhs.nonBlockExpInputs.mkString(", ")}")
    val pending = rhs.nonBlockExpInputs.flatMap{sym => PendingUses(sym) }
    dbgs(s"  Uses:    ${pending.mkString(", ")}")
    dbgs(s"  Transient: ${lhs.isTransient}")
    // We care about whether the IR scope is outer, not whether the owning controller is outer
    val isOuter = lhs.isControl || blk.toScope.toCtrl.isOuterControl || (blk.toScope.toCtrl.isInnerControl && blk.toScope.children.nonEmpty)
    dbgs(s"  Outer: $isOuter")
    if (pending.nonEmpty) {
      // All nodes which could potentially use a reader outside of an inner control node
      // Add propagating use if outer or outside Accel
      if ((lhs.isTransient | lhs.isCounter) && isOuter) addPropagatingUse(lhs, pending.toSet)
      else addUse(lhs, pending.toSet, blk)
    }
  }

  /** Mark the given transient symbols as being consumed by a user.
    * @param consumer Consumer symbol
    * @param used Consumed symbol(s)
    * @param block The control block this use occurs in
    */
  private def addUse(consumer: Sym[_], used: Set[Sym[_]], block: Blk): Unit = {
    dbgs(s"  Uses [Block: $block]:")
    dbgs(s"    consumer $consumer")
    dbgs(s"    used $used")
    dbgs(s"    ")
    used.foreach{s => dbgs(s"  - ${stm(s)}")}

    // Bound symbols should always be the result of a block if they are defined elsewhere
    (used diff boundSyms).foreach{use =>
      use.users += User(consumer, blkOfUser(consumer, block))
      dbgs(s"  Adding direct ($consumer ${blkOfUser(consumer, block)}) to uses for $use")

      // Also add stateless nodes that this node uses
      (PendingUses(use) - use).foreach{pend =>
        dbgs(s"  Adding pending ($use ${blkOfUser(consumer, block)}) to uses for $pend")
        pend.users += User(use, blkOfUser(consumer, block))
      }
    }
  }

  private def addPropagatingUse(sym: Sym[_], pending: Set[Sym[_]]): Unit = {
    dbgs(s"  Node is propagating reader of:")
    pending.foreach{s => dbgs(s"  - ${stm(s)}")}
    PendingUses += sym -> (pending + sym)
  }

  private def addPendingUse(sym: Sym[_]): Unit = if (!PendingUses.all.contains(sym)) {
    dbgs(s"  Adding pending: $sym [ctrl: ${sym.toCtrl}, block: $blk]")
    PendingUses += sym -> Set(sym)
  }

}
