package spatial.traversal

import argon._
import spatial.lang._
import spatial.node._
import spatial.util.spatialConfig
import spatial.metadata.control._
import spatial.metadata.bounds._
import spatial.issues.ControlPrimitiveMix

/** Used to automatically detect invalid changes that occurred during transformations. */
case class CompilerSanityChecks(IR: State, enable: Boolean) extends AbstractSanityChecks {
  var visitedStms: Map[Sym[_],Option[Sym[_]]] = Map.empty
  var nestedScope: Set[Sym[_]] = Set.empty
  var parent: Option[Sym[_]] = None
  override def shouldRun: Boolean = enable

  override def postprocess[R](block: Block[R]): Block[R] = {
    visitedStms = Map.empty
    nestedScope = Set.empty
    super.postprocess(block)
  }

  override def visitBlock[R](block: Block[R]): Block[R] = {
    val saveScope = nestedScope
    nestedScope ++= block.inputs
    val result = super.visitBlock(block)
    nestedScope = saveScope
    result
  }

  def check[A](lhs: Sym[A], rhs: Op[A]): Unit = {
    val inputs = rhs.nonBlockSymInputs
    val unknownInputs = (inputs.toSet diff nestedScope).filterNot { _.vecConst.nonEmpty }
    if (unknownInputs.nonEmpty) {
      bug(lhs.ctx, s"Statement $lhs = $rhs used one or more undefined values: ")
      unknownInputs.foreach { in =>
        bug("  " + stm(in))
      }
    }
    if (visitedStms.contains(lhs)) {
      bug(lhs.ctx, s"Statement $lhs = $rhs appears to be defined twice: ")
      bug(s"First here: ${visitedStms(lhs).map(stm).getOrElse("<Host>")}")
      bug(s"Then here: ${parent.map(stm).getOrElse("<Host>")}")
    }

    nestedScope ++= (rhs.binds + lhs)
    visitedStms += (lhs -> parent)

    rhs match {
      case AccelScope(block) =>
        val (stms, inputs) = block.nestedStmsAndInputs
        val illegalUsed = disallowedInputs(stms, inputs.iterator, allowArgInference = true)

        if (illegalUsed.nonEmpty) {
          bug("One or more values were defined on the host but used in Accel without explicit transfer.")
          illegalUsed.foreach { case (in, use) =>
            bug(in.ctx, stm(in))
            bug(use.ctx, s"First use in Accel occurs here.", noBug = true)
            bug(stm(use))
            bug(use.ctx)
          }
        }

      case _ => // Nothing
    }

    if (!spatialConfig.allowPrimitivesInOuterControl) rhs match {
      case ctrl:Control[_] if lhs.isOuterControl =>
        val blocks = ctrl.blocks
        blocks.foreach{block =>
          val stms = block.stms
          val ctrl = stms.filter{s => s.isControl && !(s.isBranch && s.isInnerControl) }
          val prim = stms.filter{s => s.isPrimitive && !s.isTransient }
          if (ctrl.nonEmpty && prim.nonEmpty) {
            raiseIssue(ControlPrimitiveMix(lhs,ctrl,prim))
          }
        }
      case _ =>
    }
  }

  override def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = {
    val saveParent = parent
    parent = Some(lhs)
    rhs match {
      case AccelScope(_) => inAccel {check(lhs, rhs); super.visit(lhs, rhs) }
      case _ =>
        check(lhs, rhs)
        super.visit(lhs, rhs)
    }
    parent = saveParent
  }

}
