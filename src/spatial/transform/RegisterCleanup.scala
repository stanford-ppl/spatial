package spatial.transform

import argon._
import argon.transform.MutateTransformer
import spatial.data._
import spatial.lang._
import spatial.node._
import spatial.util._
import spatial.traversal.BlkTraversal

import utils.implicits.collections._

import scala.collection.mutable

case class RegisterCleanup(IR: State) extends MutateTransformer with BlkTraversal {
  // Substitutions per use location
  private var statelessSubstRules = Map[(Sym[_],Ctrl), Seq[(Sym[_], () => Sym[_])]]()

  private val completedMirrors = mutable.HashMap[(Sym[_],Ctrl), Sym[_]]()

  private def delayedMirror[T](lhs: Sym[T], rhs: Op[T], ctrl: Ctrl)(implicit ctx: SrcCtx): () => Sym[_] = () => {
    val key = (lhs, ctrl)
    completedMirrors.getOrElseAdd(key, () => {
      withCtrl(ctrl){ mirrorWithDuplication(lhs, rhs) }
    })
  }

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = (rhs match {
    case node: Primitive[_] if inHw && node.isEphemeral =>
      dbgs("")
      dbgs("[stateless]")
      dbgs(s"$lhs = $rhs")
      dbgs(s"users: ${usersOf(lhs)}")

      // For all uses within a single control node, create a single copy of this node
      // Then associate all uses within that control with that copy
      val users = usersOf(lhs).groupBy(_.blk)

      users.foreach{case (block, uses) =>
        val read = delayedMirror(lhs, rhs, block)

        dbgs(s"ctrl: $block")

        uses.foreach{ case User(use,_) =>
          val subs = (lhs -> read) +: statelessSubstRules.getOrElse((use,block), Nil)
          dbgs(s"  ($use, $block): $lhs -> $read")
          statelessSubstRules += (use,block) -> subs
        }
      }

      if (usersOf(lhs).isEmpty) dbgs(s"REMOVING stateless $lhs")
      Invalid

    case RegWrite(reg,value,en) =>
      dbgs("")
      dbgs("[reg write]")
      dbgs(s"$lhs = $rhs")
      if (readersOf(reg).isEmpty) {
        dbgs(s"REMOVING register write $lhs")
        Invalid
      }
      else mirrorWithDuplication(lhs, rhs)

    case RegNew(_) =>
      dbgs("")
      dbgs("[reg new]")
      dbgs(s"$lhs = $rhs")
      if (readersOf(lhs).isEmpty) {
        dbgs(s"REMOVING register $lhs")
        Invalid
      }
      else mirrorWithDuplication(lhs, rhs)

    case _ if isControl(lhs) => withCtrl(lhs){ mirrorWithDuplication(lhs, rhs) }
    case _ => mirrorWithDuplication(lhs, rhs)
  }).asInstanceOf[Sym[A]]

  private def mirrorWithDuplication[T](lhs: Sym[T], rhs: Op[T])(implicit ctx: SrcCtx): Sym[T] = {
    dbgs(s"${stm(lhs)} [$blk]")
    //statelessSubstRules.keys.foreach{k => dbgs(s"  $k") }
    if ( statelessSubstRules.contains((lhs,blk)) ) {
      dbgs("")
      dbgs(s"[external user, blk = $blk]")
      dbgs(s"$lhs = $rhs")
      // Activate / lookup duplication rules
      val rules = statelessSubstRules((lhs,blk)).map{case (s,s2) => s -> s2()}
      rules.foreach{case (s,s2) => dbgs(s"  $s -> ${stm(s2)}") }
      val lhs2 = isolateSubstWith(rules:_*){ mirror(lhs, rhs) }
      dbgs(s"${stm(lhs2)}")
      lhs2
    }
    else mirror(lhs, rhs)
  }

  /** Requires slight tweaks to make sure we transform block results properly, primarily for OpReduce **/
  override protected def inlineBlock[T](b: Block[T]): Sym[T] = inlineBlockWith(b){stms =>
    advanceBlock()
    stms.foreach(visit)
    val rules = blk match {
      case Host            => Nil
      case Controller(s,_) =>
        // Add substitutions for this node (ctrl.node, -1) and for the current block (ctrl)
        val node  = (s, Controller(s,-1))
        val block = (s, blk)
        dbgs(s"node: $node, block: $block")
        statelessSubstRules.getOrElse(node, Nil).map{case (s1, s2) => s1 -> s2() } ++
        statelessSubstRules.getOrElse(block, Nil).map{case (s1, s2) => s1 -> s2() }
    }
    if (rules.nonEmpty) rules.foreach{rule => dbgs(s"  ${rule._1} -> ${rule._2}") }
    isolateSubstWith(rules: _*){ f(b.result) }
  }

}
