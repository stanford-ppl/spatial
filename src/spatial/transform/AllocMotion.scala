package spatial.transform

import argon._
import argon.transform.MutateTransformer
import spatial.node.{AccelScope, Control, OpForeach}
import spatial.traversal.AccelTraversal
import spatial.lang._
import spatial.metadata.control._
import spatial.metadata.memory._

case class AllocMotion(IR: State) extends MutateTransformer with AccelTraversal {
  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {

    case AccelScope(_) => inAccel { dbgs("In Accel"); super.transform(lhs, rhs) }

    case ctrl: OpForeach if inAccel =>
      dbgs(s"Processing: $lhs = $rhs")
      ctrl.blocks foreach {
        blk =>
          register(blk -> motionAllocs(blk))
      }
      mirrorSym(lhs)

    case _ =>
      super.transform(lhs, rhs)
  }

  // Moves allocs to the beginning of the block.
  def motionAllocs(block: Block[_]): Block[_] = {
    stageBlock({
      block.internalMems.foreach {
        mem =>
          dbgs(s"Mirroring Memory: $mem = ${mem.op}")
          register(mem -> mirrorSym(mem))
      }

      block.stms.filterNot(_.isMem).foreach {
        case sym@Op(op) =>
          implicit def tpEV: Type[sym.R] = op.R.asInstanceOf[Type[sym.R]]
          register(sym -> transform(sym.asInstanceOf[Sym[sym.R]], op.asInstanceOf[Op[sym.R]]))
      }
      f(block.stms.last)
    })
  }
}
