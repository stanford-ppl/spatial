package spatial.transform

import argon._
import argon.transform.MutateTransformer
import spatial.metadata.control._
import spatial.lang._
import spatial.node._

case class RegReadCSE(IR: State) extends MutateTransformer {
  private var inInnerCtrl: Boolean = false

  private def inCtrl[A](isInner: Boolean)(x: => A): A = {
    val prev = inInnerCtrl
    inInnerCtrl = isInner
    val result = x
    inInnerCtrl = prev
    result
  }

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case op@RegRead(reg) if inInnerCtrl =>
      implicit val A: Bits[A] = op.A
      // Note that we're not staging the op yet, just creating it
      val op2 = RegRead(f(reg))
      val effects = computeEffects(op2)

      // Effects includes anti-dependencies, so this checks if there's an intermediate write
      // between the two reads we're attempting to CSE.
      state.cache.get(op2).filter(state.scope.contains).filter(_.effects == effects) match {
        case Some(lhs2) => lhs2.asInstanceOf[Sym[A]]
        case None =>
          val lhs2 = super.transform(lhs,rhs)
          state.cache += op2 -> lhs2
          lhs2
      }

    case ctrl: Control[_] =>
      ctrl.bodies.zipWithIndex.foreach{case (body, id) =>
        val stage = Ctrl.Node(lhs, id)
        val isInner = !stage.mayBeOuterBlock || lhs.isInnerControl
        body.blocks.foreach{case (_, block) => register(block -> inCtrl(isInner){ f(block) }) }
      }
      super.transform(lhs,rhs)

    case _ => super.transform(lhs,rhs)
  }

}
