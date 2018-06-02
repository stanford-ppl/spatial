package spatial.transform

import argon._
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.node._
import spatial.internal._
import spatial.util.shouldMotion
import spatial.traversal.AccelTraversal

/** Converts IfThenElse nodes into nested Switch/SwitchCase nodes within Accel scopes.
  *
  * A sequence of if-then-else statements will be nested in the IR as follows:
  *   if (x) { blkA }
  *   else if (y) { blkB }
  *   else if (z) { blkC }
  *   else { blkD }
  *
  * // Logic to compute x
  * IfThenElse(x, blkA, blkX)
  * blkX:
  *   // Logic to compute y
  *   IfThenElse(y, blkB, blkY)
  *   blkY:
  *     // Logic to compute z
  *     IfThenElse(z, blkC, blkD)
  *
  * This means, except in trivial cases, IfThenElses will often not be perfectly nested, as the
  * block containing an IfThenElse will usually also contain the corresponding condition logic.
  *
  * We can pull this logic out of the outer condition if they meet normal code motion rules
  */
case class SwitchTransformer(IR: State) extends MutateTransformer with AccelTraversal {
  private var enables: Set[Bit] = Set.empty

  private def withEn[T](en: Bit)(blk: => T): T = {
    val saveEnables = enables
    enables += en
    val result = blk
    enables = saveEnables
    result
  }

  private def createCase[T:Type](cond: Bit, body: Block[T])(implicit ctx: SrcCtx) = () => {
    dbgs(s"Creating SwitchCase from cond $cond and body $body")
    val c = withEn(cond){ op_case(f(body) )}
    dbgs(s"  ${stm(c)}")
    c
  }


  private def extractSwitches[T:Type](
    block:    Block[T],
    prevCond: Bit,
    selects:  Seq[Bit],
    cases:    Seq[() => T]
  )(implicit ctx: SrcCtx): (Seq[Bit], Seq[() => T]) = block.result match {
    // Flatten switch only if there are no enabled primitives or controllers
    case Op(IfThenElse(cond,thenBlk,elseBlk)) if shouldMotion(block.stms,inHw) =>
      // Move all statements except the result out of the case
      withEn(prevCond){ block.stms.dropRight(1).foreach(visit) }
      val cond2 = f(cond)
      val thenCond = cond2 & prevCond
      val elseCond = !cond2 & prevCond
      val thenCase = createCase(thenCond, thenBlk)
      extractSwitches(elseBlk, elseCond, selects :+ thenCond, cases :+ thenCase)

    case _ =>
      val lastCase = createCase(prevCond, block)
      (selects :+ prevCond, cases :+ lastCase)
  }

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case AccelScope(_) => inAccel{ super.transform(lhs,rhs) }

    case IfThenElse(cond,thenBlk,elseBlk) if inHw =>
      dbgs(s"Transforming $lhs ($cond ? $thenBlk : $elseBlk")
      val cond2 = f(cond)
      val elseCond = !cond2
      val thenCase = createCase(cond2, thenBlk)
      val (selects, cases) = extractSwitches(elseBlk, elseCond, Seq(cond2), Seq(thenCase))

      val switch = op_switch(selects, cases)
      switch

    case _ => super.transform(lhs,rhs)
  }

  override def updateNode[A](node: Op[A]): Unit = node match {
    case e:Enabled[_] => e.updateEn(f, enables)
    case _ => super.updateNode(node)
  }
}


