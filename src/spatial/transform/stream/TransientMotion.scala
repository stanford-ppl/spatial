package spatial.transform.stream

import argon._
import spatial.node._

import argon.transform.MutateTransformer
import spatial.traversal.AccelTraversal

import spatial.metadata.control._
import spatial.metadata.memory._


case class TransientMotion(IR: State) extends MutateTransformer with AccelTraversal {

  def motionBlock[A](blk: Block[A]): Block[A] = {
    // All inputs of symbols in the block that are transient
    val allTransients = blk.stms.flatMap(_.inputs).filter(_.isTransient)

    // exclude transients that are defined in the block
    val nonlocalTransients = allTransients.filterNot(blk.stms.contains)
    dbgs(s"Found Transients: $nonlocalTransients")
    stageBlock {
      // Restage all the transients in here, but we don't want to keep them
      excludeSubst(nonlocalTransients:_*) {
        subst = subst.filterNot { case (s, _) => nonlocalTransients.contains(s) }
        nonlocalTransients foreach {
          sym =>
            register(sym -> mirrorSym(sym))
            dbgs(s"Registering: $sym = ${sym.op.get} -> ${f(sym)} = ${f(sym).op.get}")
        }
        // mirror the block, with the transient replacements
        inlineBlock(blk)
      }
    }

  }

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case _: AccelScope => inAccel { super.transform(lhs, rhs) }
    case foreach: OpForeach if inHw =>
      dbgs(s"Processing: $lhs = $rhs")
      register(foreach.block -> motionBlock(foreach.block))
      mirrorSym(lhs)
    case _ => super.transform(lhs, rhs)
  }
}
