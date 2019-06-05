package spatial.transform

import argon._
import argon.transform.MutateTransformer
import emul.{FALSE, TRUE}
import spatial.node._
import spatial.lang._
import spatial.metadata.control._
import spatial.metadata.access._
import spatial.metadata.memory._
import spatial.traversal.AccelTraversal
import spatial.util.shouldMotionFromConditional

case class SwitchOptimizer(IR: State) extends MutateTransformer with AccelTraversal {

  // Hotswap is defined as when the firstBlock writes to a Reg and that Reg is read and used to set up the condition in a Switch in the secondBlock
  private def markHotSwaps(prevHotSwaps: Seq[(Sym[_], Sym[_])], firstBlock: Block[_], secondBlock: Block[_]): Unit = {
    val hotSwapRegs:Seq[(Sym[_], Sym[_])] = firstBlock.nestedStms.collect{case x if (x.isWriter && x.writtenMem.isDefined && x.writtenMem.get.isReg) => (x, x.writtenMem.get)}
    val nestedSwitches: Option[(Seq[Sym[_]], Block[_])] = secondBlock.nestedStms.collectFirst{case Op(op@Switch(sels, bod)) => (sels,bod)}
    if (nestedSwitches.isDefined) {
      val hotSwapReaders = secondBlock.nestedStms.collect{case x if (x.isReader && x.readMem.isDefined && (prevHotSwaps ++ hotSwapRegs).map(_._2).contains(x.readMem.get)) => x}
      hotSwapReaders.foreach{x => 
        val relationships: Map[Sym[_], Set[Sym[_]]] = x.readMem.get.hotSwapPairings
        val conflicts: Set[Sym[_]] = hotSwapRegs.collect{ case (w,r) if (r == x.readMem.get) => w}.toSet
        x.readMem.get.hotSwapPairings = relationships.filter(_._1 != x).toMap ++ Map((x -> (conflicts ++ relationships.getOrElse(x,Set()))))
      }
      val bodies = nestedSwitches.get._2.nestedStms.collect{case Op(op@SwitchCase(bod)) => bod}
      markHotSwaps(prevHotSwaps ++ hotSwapRegs, bodies(0), bodies(1))
    }
  }

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case AccelScope(_) => inAccel{ super.transform(lhs,rhs) }

    case SwitchCase(_) =>
      dbgs(s"$lhs = $rhs")
      super.transform(lhs,rhs)

    case Switch(selects,body) =>
      // Mark hotSwapWriters
      val bodies = body.nestedStms.collect{case Op(op@SwitchCase(bod)) => bod}
      markHotSwaps(Seq(), bodies(0), bodies(1))

      val scs = f(selects).zip(body.stms).filter{case (Const(FALSE),_) => false; case _ => true }
      val sels = scs.map(_._1)
      val stms = scs.map(_._2).map(_.asInstanceOf[A])
      val cases = stms.collect{case Op(op:SwitchCase[_]) => op.asInstanceOf[SwitchCase[A]]  }
      val trueConds = sels.zipWithIndex.collect{case (Const(TRUE),i) => i }

      dbgs(s"Optimizing Switch with selects/cases: ")
      scs.zipWithIndex.foreach{case ((sel,cond),i) =>
        dbgs(s"  #$i [sel]: ${stm(sel)}")
        dbgs(s"  #$i [cas]: ${stm(cond)}")
      }

      if (trueConds.length == 1) {
        val cas = cases.apply(trueConds.head).body
        inlineBlock(cas)
      }
      else if (cases.length == 1) {
        inlineBlock(cases.head.body)
      }
      else {
        if (trueConds.length > 1 || sels.isEmpty) {
          if (trueConds.isEmpty) warn(ctx, "Switch has no enabled cases")
          else  warn(ctx, "Switch has multiple enabled cases")
          warn(ctx)
        }
        val canMotion = cases.forall{c => shouldMotionFromConditional(c.body.stms,inHw) }

        Type[A] match {
          case Bits(b) if canMotion =>
            implicit val bA: Bits[A] = b
            val vals = cases.map{cas => inlineBlock(cas.body).unbox }

            if (sels.length == 2) mux[A](sels.head, vals.head, vals.last)
            else oneHotMux[A](sels, vals)

          case _ =>
            Switch.op_switch[A](sels, stms.map{s => () => { visit(s); f(s) } })
        }
      }


    case _ => super.transform(lhs,rhs)
  }

}
