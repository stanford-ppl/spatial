package spatial.transform.stream

import argon._
import argon.transform._
import spatial.node._
import spatial.metadata.control._

/**
  * In the case where there is an outer Unitpipe which immediately contains a child outer Unitpipe with the same
  * schedule, the child can be 'dissolved' and all of its children dumped directly into the parent.
  */
case class PipeCollapse(IR: State) extends MutateTransformer {

  private def compatible(parentSchedule: Option[CtrlSchedule], childSchedule: Option[CtrlSchedule]): Boolean = (parentSchedule, childSchedule) match {
    case (Some(a), Some(b)) if Set(a, b) subsetOf Set(Pipelined, Sequenced) => true
    case (Some(a), Some(b)) if Set(a, b) subsetOf Set(ForkJoin, Streaming) => true
    case (a, b) => a == b
  }

  private def collapsible(schedule: Option[CtrlSchedule])(child: Sym[_], pipe: UnitPipe): Boolean = {
    if (child.isInnerControl) { return false }
    if (pipe.ens.nonEmpty || pipe.stopWhen.nonEmpty) { return false }
    compatible(schedule, child.getUserSchedule)
  }

  private def isTransformTarget(lhs: Sym[_], unitPipe: UnitPipe): Boolean = {
    if (!lhs.isOuterControl) { return false }
    val canCollapse = collapsible(lhs.getUserSchedule)(_, _)
    unitPipe.block.stms.exists {
      case child@Op(pipe:UnitPipe) =>
        canCollapse(child, pipe)
      case _ => false
    }
  }

  def transformUnitpipe(lhs: Sym[_], unitPipe: UnitPipe): Sym[_] = {
    val canCollapse = collapsible(lhs.getUserSchedule)(_, _)
    stageWithFlow(UnitPipe(f(unitPipe.ens), stageBlock {
      unitPipe.block.stms foreach {
        case child@Op(up: UnitPipe) if canCollapse(child, up) =>
          dbgs(s"Collapsing $lhs <- $child = $up")
          indent {
            up.block.stms foreach visit
          }
        case other => visit(other)
      }
      spatial.lang.void
    }, f(unitPipe.stopWhen))) {
      lhs2 => transferDataIfNew(lhs, lhs2)
    }
  }

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = (rhs match {
    case unitPipe: UnitPipe if isTransformTarget(lhs, unitPipe) =>
      dbgs(s"Analyzing: $lhs = $unitPipe")
      indent {
        transformUnitpipe(lhs, unitPipe)
      }
    case _ =>
      super.transform(lhs, rhs)
  }).asInstanceOf[Sym[A]]
}
