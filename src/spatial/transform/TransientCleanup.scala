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
import spatial.traversal.ScopeTraversal

import utils.implicits.collections._

import scala.collection.mutable

case class TransientCleanup(IR: State) extends MutateTransformer with ScopeTraversal {
  // Substitutions per use location
  private var statelessSubstRules = Map[(Sym[_],Scope), Seq[(Sym[_], () => Sym[_])]]()

  private val completedMirrors = mutable.HashMap[(Sym[_],Scope), Sym[_]]()

  override protected def preprocess[R](block: Block[R]): Block[R] = {
    blk = Blk.Host
    scp = Scope.Host
    super.preprocess(block)
  }

  private def delayedMirror[T](lhs: Sym[T], rhs: Op[T], scope: Scope)(implicit ctx: SrcCtx): () => Sym[_] = () => {
    val key = (lhs, scope)
    if (completedMirrors.contains(key)) {
      dbgs(s"Using mirror ${completedMirrors(key)} for $lhs in $scope")
      completedMirrors(key)
    } else {
      val newMirror = inScope(scope){ inCopyMode(copy = true){ updateWithContext(lhs, rhs) } }
      completedMirrors += ((key -> newMirror))
      dbgs(s"Created new mirror ${newMirror} for $lhs in $scope")
      newMirror
    }
  }

  def requiresMoveOrDuplication[A](lhs: Sym[A], rhs: Op[A]): Boolean = rhs match {
    case node:Primitive[_] =>
      // Duplicate stateless nodes when they have users across control or not the current block
      val scopes = lhs.users.map(_.sym match {case s if s.isControl => s.toScope; case s => s.scope})
      node.isTransient && (scopes.size > 1 || scopes.exists(_ != scp))

    case _ => false
  }

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = (rhs match {
    case node: Primitive[_] if requiresMoveOrDuplication(lhs, rhs) =>
      dbgs("")
      dbgs(s"$lhs = $rhs")
      dbgs(s" - users: ${lhs.users} [stateless]")

      // For all uses within a single control node, create a single copy of this node
      // Then associate all uses within that control with that copy
      val users = lhs.users.groupBy(_.sym match {case s if s.isControl => s.toScope; case s => s.scope})

      users.foreach{case (scope, uses) =>
        val read = delayedMirror(lhs, rhs, scope)

        dbgs(s" - ctrl: $scope")

        uses.foreach{ case User(use,_) =>
          val subs = (lhs -> read) +: statelessSubstRules.getOrElse((use,scope), Nil)
          dbgs(s"    - ($use, $scope): $lhs -> $read")
          statelessSubstRules += (use,scope) -> subs
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
      dbgs(s"$lhs = $rhs [stateless]")
      dbgs(s" - users: ${lhs.users}")
      dbgs(s" - ctrl:  $scp")
      if (lhs.users.isEmpty) {
        dbgs(s"REMOVING stateless $lhs")
        Invalid
      }
      else {
        updateWithContext(lhs, rhs)
      }

    // Remove unused counters and counterchains
    case _:CounterNew[_]     => if (lhs.getOwner.isEmpty) Invalid else inCtrl(lhs.owner){ updateWithContext(lhs, rhs) }
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
    dbgs(s"${stm(lhs)} [$scp]")
    //statelessSubstRules.keys.foreach{k => dbgs(s"  $k") }
    if ( statelessSubstRules.contains((lhs,scp)) ) {
      dbgs("")
      dbgs(s"$lhs = $rhs [external user, scp = $scp]")
      // Activate / lookup duplication rules
      val rules = statelessSubstRules((lhs,scp)).map{case (s,s2) => s -> s2()}
      rules.foreach{case (s,s2) => dbgs(s"  $s -> ${stm(s2)}") }
      val lhs2 = isolateSubstWith(escape=Nil, rules:_*){ update(lhs,rhs) }
      dbgs(s"${stm(lhs2)}")
      lhs2
    }
    else update(lhs, rhs)
  }

  def withBlockSubsts[A](escape: Sym[_]*)(block: => A): A = {
    val rules = scp match {
      case Scope.Host      => Nil
      case Scope.Node(s,_,_) =>
        // Add substitutions for this node (ctrl.node, -1) and for the current block (ctrl)
        val node  = (s, Scope.Node(s,-1,-1))
        val scope = (s, scp)
        dbgs(s"node: $node, scope: $scope")
        statelessSubstRules.getOrElse(node, Nil).map{case (s1, s2) => s1 -> s2() } ++
          statelessSubstRules.getOrElse(scope, Nil).map{case (s1, s2) => s1 -> s2() }
    }
    if (rules.nonEmpty) rules.foreach{rule => dbgs(s"  ${rule._1} -> ${rule._2}") }
    isolateSubstWith(escape, rules: _*){ block }
  }

  /** Requires slight tweaks to make sure we transform block results properly, primarily for OpReduce **/
  override protected def inlineBlock[T](b: Block[T]): Sym[T] = {
    advanceScope() // Advance block counter before transforming inputs

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
