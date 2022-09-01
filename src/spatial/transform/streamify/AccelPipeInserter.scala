package spatial.transform.streamify

import argon._
import argon.transform._
import spatial.node._

case class AccelPipeInserter(IR: State) extends ForwardTransformer {
  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = (rhs match {
    case AccelScope(block) =>
      dbgs(s"Inserting unitpipe into Accel")
      stageWithFlow(AccelScope(stageBlock {
        stage(UnitPipe(Set.empty, stageBlock {
          inlineBlock(block)
        }, None))
      })) {
        accelNew => transferData(lhs, accelNew)
      }
    case _ => super.transform(lhs, rhs)
  }).asInstanceOf[Sym[A]]
}
