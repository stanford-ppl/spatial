package spatial.transform

import argon._
import argon.transform.MutateTransformer
import spatial.node.{AccelScope, Control, CounterChainNew, CounterNew, OpForeach}
import spatial.traversal.AccelTraversal
import spatial.lang._
import spatial.metadata.control._
import spatial.metadata.memory._

case class AllocMotion(IR: State) extends MutateTransformer with AccelTraversal {
  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {

    case AccelScope(_) => inAccel { dbgs("In Accel"); super.transform(lhs, rhs) }

    case ctrl: Control[_] if inAccel && lhs.isOuterControl && (lhs.isForeach || lhs.isUnitPipe) =>
      dbgs(s"Processing: $lhs = $rhs")
      ctrl.blocks foreach {
        blk =>
          register(blk -> motionAllocs(blk))
      }
      super.transform(lhs, rhs)

    case _ =>
      super.transform(lhs, rhs)
  }

  def canMove(sym: Sym[_]): Boolean = sym.op match {
    case _ if sym.isMem => true
    case Some(ctr: CounterNew[_]) => ctr.inputs.forall(canMove)
    case Some(CounterChainNew(ctrs)) => ctrs.forall(canMove)
    case _ => false
  }

  // Moves allocs to the beginning of the block.
  def motionAllocs(block: Block[_]): Block[_] = {
    stageBlock({
      dbgs(s"Moving to front:")
      indent {
        block.stms.filter(canMove) foreach {
          stmt =>
            dbgs(s"$stmt = ${stmt.op}")
            register(stmt -> mirrorSym(stmt))
        }
      }

      dbgs("------------------------------")
      indent {
        block.stms.filterNot(canMove) foreach {
          case sym@Op(op) =>
            dbgs(s"Visiting: $sym = $op")
            super.visit(sym)
        }
      }
      f(block.result)
    })
  }
}
