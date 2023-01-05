package spatial.transform

import argon._
import argon.transform.MutateTransformer
import spatial.traversal.AccelTraversal
import spatial.node._
import spatial.metadata.control._

case class DuplicateRetimeStripper(IR: State) extends MutateTransformer with AccelTraversal {
  def stripDuplicateRetimes(block: Block[_]): Block[_] = {
    stageScope(f(block.inputs), block.options) {
      var previousWasRetime = false
      block.stms.foreach {
        case rt@Op(RetimeGate()) =>
          if (!previousWasRetime) {
            super.visit(rt)
          }
          previousWasRetime = true
        case other =>
          super.visit(other)
          previousWasRetime = false
      }
      f(block.result)
    }
  }

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case ctrl: Control[_] if lhs.isInnerControl =>
      ctrl.blocks foreach {
        block => register(block -> stripDuplicateRetimes(block))
      }
      super.transform(lhs, rhs)

    case _ => super.transform(lhs, rhs)
  }
}
