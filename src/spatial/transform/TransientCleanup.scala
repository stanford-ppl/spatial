package spatial.transform

import argon._
import argon.node._
import argon.transform.MutateTransformer
import spatial.metadata.access._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.types._
import spatial.lang._
import spatial.node._
import spatial.traversal.BlkTraversal

import utils.implicits.collections._

import scala.collection.mutable

case class TransientCleanup(IR: State) extends MutateTransformer with BlkTraversal {
  // Substitutions per use location
  private var statelessSubstRules = Map[(Sym[_],Blk), Seq[(Sym[_], () => Sym[_])]]()

  private val completedMirrors = mutable.HashMap[(Sym[_],Blk), Sym[_]]()

  override protected def preprocess[R](block: Block[R]): Block[R] = {
    blk = Blk.Host
    super.preprocess(block)
  }

  private def delayedMirror[T](lhs: Sym[T], rhs: Op[T], blk: Blk)(implicit ctx: SrcCtx): () => Sym[_] = () => {
    val key = (lhs, blk)
    if (completedMirrors.contains(key)) {
      dbgs(s"Using mirror ${completedMirrors(key)} for $lhs in $blk")
      completedMirrors(key)
    } else {
      val newMirror = inBlk(blk){ inCopyMode(copy = true){ updateWithContext(lhs, rhs) } }
      completedMirrors += (key -> newMirror)
      dbgs(s"Created new mirror ${newMirror} for $lhs in $blk")
      newMirror
    }
  }

  /* Gets rid of cases where the user has already been visited,
     such as an OpForeach's block 0 may return a value who is considered
     to be used by the OpForeach's block -1
  */
  private def priorUser(x: User): Boolean = { 
    x.sym match {
      case s if s.isControl => blk.s.contains(s) && (blk.block > -1)
      case s if s.isCounter && s.getOwner.isDefined => blk.s.contains(s.owner) && (blk.block > -1)
      case _ => (blk.s == x.blk.s) && (blk.block > x.blk.block)
    }
  }
  def requiresMoveOrDuplication[A](lhs: Sym[A], rhs: Op[A]): Boolean = rhs match {
    case node:Primitive[_] =>
      // Duplicate stateless nodes when they have users across control or not the current block
      val blks = lhs.users.filterNot(priorUser).map(_.blk)
      node.isTransient && (blks.size > 1 || blks.exists(_ != blk))

    case _ => false
  }

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = (rhs match {
    case node: Primitive[_] if requiresMoveOrDuplication(lhs, rhs) =>
      dbgs("")
      dbgs(s"Transforming node that is primitive and requires move or dup")
      dbgs(s"$lhs = $rhs")
      dbgs(s" - users: ${lhs.users} [stateless]")

      // For all uses within a single control node, create a single copy of this node
      // Then associate all uses within that control with that copy
      val users = lhs.users.filterNot(priorUser).groupBy(_.blk)

      users.foreach{case (block, uses) =>
        val read = delayedMirror(lhs, rhs, block)

        dbgs(s" - ctrl: $block")
        lhs.readMem.foreach{m => block.s.get.transientReadMems = block.s.get.transientReadMems ++ Set(m)}

        uses.foreach{ case User(use,_) =>
          val subs = (lhs -> read) +: statelessSubstRules.getOrElse((use,block), Nil)
          dbgs(s"    - ($use, $block): $lhs -> $read")
          statelessSubstRules += (use,block) -> subs
        }
      }

      if (inHw) {
        if (lhs.users.isEmpty) dbgs(s"REMOVING stateless $lhs")
        Invalid // In hardware, must know context to use this symbol
      }
      else {
        // Nodes in host, used only in host won't have users
        updateWithContext(lhs, rhs)
      }

    case node: Primitive[_] if inHw && node.isTransient =>
      dbgs("")
      dbgs(s"Transforming node that is primitive, is inHw, and is transient")
      dbgs(s"$lhs = $rhs [stateless]")
      dbgs(s" - users: ${lhs.users}")
      dbgs(s" - ctrl:  $blk")
      if (lhs.users.isEmpty) {
        dbgs(s"REMOVING stateless $lhs")
        Invalid
      }
      else {
        updateWithContext(lhs, rhs)
      }

    // Remove unused counters and counterchains
    case _:(CounterNew[_])   => if (lhs.getOwner.isEmpty) Invalid else inCtrl(lhs.owner){ updateWithContext(lhs, rhs) }
    case _:(ScannerNew)   => if (lhs.getOwner.isEmpty) Invalid else inCtrl(lhs.owner){ updateWithContext(lhs, rhs) }
    case _:CounterChainNew   => if (lhs.getOwner.isEmpty) Invalid else inCtrl(lhs.owner){ updateWithContext(lhs, rhs) }

    case RegWrite(reg,value,en) =>
      dbgs("")
      dbgs(s"$lhs = $rhs [reg write]")
      if (reg.isUnusedMemory) {
        dbgs(s"REMOVING register write $lhs")
        Invalid
      }
      else updateWithContext(lhs, rhs)

    case RegNew(_) =>
      dbgs("")
      dbgs(s"$lhs = $rhs [reg new]")
      if (lhs.isUnusedMemory) {
        dbgs(s"REMOVING register $lhs")
        Invalid
      }
      else updateWithContext(lhs, rhs)

    case _ if lhs.isControl => inCtrl(lhs){ updateWithContext(lhs, rhs) }
    case _ => updateWithContext(lhs, rhs)
  }).asInstanceOf[Sym[A]]

  private def updateWithContext[T](lhs: Sym[T], rhs: Op[T])(implicit ctx: SrcCtx): Sym[T] = {
    dbgs(s"${stm(lhs)} [$blk]")
    statelessSubstRules.keys.foreach{k => dbgs(s"  $k") }
    if ( statelessSubstRules.contains((lhs,blk)) ) {
      dbgs("")
      dbgs(s"$lhs = $rhs [external user, blk = $blk]: Subst Rule ${statelessSubstRules((lhs,blk))}")
      // Activate / lookup duplication rules
      val rules = statelessSubstRules((lhs,blk)).map{case (s,s2) => s -> s2()}
      rules.foreach{case (s,s2) => dbgs(s"  $s -> ${stm(s2)}") }
      val lhs2 = isolateSubstWith(escape=Nil, rules:_*){ update(lhs,rhs) }
      dbgs(s"${stm(lhs2)}")
      lhs2
    }
    else {
      dbgs(s"Updating $lhs since there are no subst rules for it")
      update(lhs, rhs)
    }
  }

  def withBlockSubsts[A](escape: Sym[_]*)(block: => A): A = {
    val rules = blk match {
      case Blk.Host      => Nil
      case Blk.Node(s,_) =>
        // Add substitutions for this node (ctrl.node, -1) and for the current block (ctrl)
        val node  = (s, Blk.Node(s,-1))
        val block = (s, blk)
        dbgs(s"node: $node, block: $block")
        statelessSubstRules.getOrElse(node, Nil).map{case (s1, s2) => s1 -> s2() } ++
          statelessSubstRules.getOrElse(block, Nil).map{case (s1, s2) => s1 -> s2() }
    }
    if (rules.nonEmpty) rules.foreach{rule => dbgs(s"  ${rule._1} -> ${rule._2}") }
    isolateSubstWith(escape, rules: _*){ block }
  }

  /** Requires slight tweaks to make sure we transform block results properly, primarily for OpReduce **/
  override protected def inlineBlock[T](b: Block[T]): Sym[T] = {
    dbgs(s"Advancing blk")
    dbgs(s"   From: $blk")
    advanceBlk() // Advance block counter before transforming inputs
    dbgs(s"   To: $blk")

    withBlockSubsts(b.result) {
      inlineWith(b){stms =>
        stms.foreach(visit)
        // Note: This assumes that non-void return types in blocks are always used
        if (b.result.isVoid) void.asInstanceOf[Sym[T]]
        else {
          // Have to call withBlockSubsts again in case subst was added inside block in order
          // to get updated substitution for block result
          withBlockSubsts(b.result){ f(b.result) }
        }
      }
    }
  }
}
