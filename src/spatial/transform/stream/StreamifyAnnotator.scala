package spatial.transform.stream

import argon._
import spatial.node._
import spatial.traversal.AccelTraversal

import spatial.metadata.access._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.transform._

case class StreamifyAnnotator(IR: argon.State) extends AccelTraversal {


  private val allowableSchedules = Set[CtrlSchedule](Pipelined, Streaming, ForkJoin)

  private def canTransform[A: Type](lhs: Sym[A], rhs: Op[A]): (Boolean, String) = {
    // No point in converting inner controllers
    if (lhs.isInnerControl) {
      return (false, "Is Inner Control")
    }

    if (!allowableSchedules.contains(lhs.schedule)) {
      return (false, s"${lhs.schedule} is not allowed")
    }

    if (lhs.shouldConvertToStreamed.isDefined) {
      return (lhs.shouldConvertToStreamed.get, "Explicit Tag")
    }
    val hasForbidden = lhs.blocks.flatMap(_.nestedStms).filter {
      case Op(_:StreamOutWrite[_]) | Op(_:StreamInRead[_]) | Op(_:StreamInNew[_]) | Op(_:StreamOutNew[_]) =>
        true
      case _ => false
    }
    if (hasForbidden.nonEmpty) { return (false, s"Has forbidden nodes: $hasForbidden") }

    // can transform if all children are foreach loops
    (lhs.blocks.flatMap(_.stms).forall {
      case stmt@Op(foreach:OpForeach) => true
//        stmt.isOuterControl
      case s if s.isMem =>
        val result = (s.writers union s.readers) forall {
          case Op(_:StreamOutWrite[_]) | Op(_:StreamInRead[_]) =>
            false
          case _ => true
        }
        dbgs(s"$result: ${s} = ${s.op}")
        result
      case s if s.isCounterChain => true
      case Op(ctr:CounterNew[_]) =>
        // Allowed if all params are either static or defined outside parent.
        dbgs(s"Checking Counter params are either static or defined outside of parent ($lhs): ${ctr.inputs}")
        ctr.inputs forall { sym =>
          dbgs(s"$sym: Const: ${sym.isConst} = ${sym.op}")
          dbgs(s"$sym: HasImmediateParent: ${sym.hasAncestor(lhs.toCtrl)}")
          sym.isConst || !sym.hasAncestor(lhs.toCtrl)
        }
      case s =>
        dbgs(s"Disallowed: ${s} = ${s.op}")
        false
    }, "")
  }

  override def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = rhs match {
    case _:AccelScope =>
      inAccel { super.visit(lhs, rhs) }
    case foreach: OpForeach =>
      super.visit(lhs, rhs)
      // check if any of the children was marked as True
      if (!foreach.block.nestedStms.exists(_.streamify)) {
        implicit val ctx: SrcCtx = lhs.ctx
        implicit val typ: Type[A] = lhs.tp
        if (!inHw) {
          lhs.streamify = false
        } else if (foreach.cchain.isStatic && foreach.cchain.approxIters <= 1) {
          lhs.streamify = (false, s"$lhs is a Unitpipe in disguise")
        } else {
          lhs.streamify = canTransform(lhs, rhs)
        }
      }

    case _ => super.visit(lhs, rhs)
  }
}
