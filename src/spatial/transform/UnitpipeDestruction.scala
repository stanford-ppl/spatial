package spatial.transform

import argon.node.Enabled
import argon._
import argon.transform.MutateTransformer
import spatial.dsl.retimeGate
import spatial.node.{AccelScope, OpForeach, UnitPipe, UnrolledForeach}
import spatial.traversal.AccelTraversal
import spatial.metadata.control._
import spatial.metadata.memory._

case class UnitpipeDestruction(IR: argon.State) extends MutateTransformer with AccelTraversal {

  private def transformGeneric(s: Sym[_], op: Op[_])(implicit ctx: argon.SrcCtx) = {
    type T = s.R
    implicit def tp: Type[T] = s.tp.asInstanceOf[Type[T]]
    transform[T](s.asInstanceOf[Sym[T]], op.asInstanceOf[Op[T]])
  }

  private def restageUnitpipe(unitpipe: UnitPipe) = {
    isolateSubst() {
      unitpipe.block.stms foreach {
        case s@Op(op: Enabled[_]) =>
          implicit val tA: Type[s.R] = op.R.asInstanceOf[Type[s.R]]
          implicit val ctx: SrcCtx = s.ctx
          val copy = stageWithFlow(op.mirrorEn(f, unitpipe.ens)) { lhs2 => transferDataIfNew(s, lhs2) }
          register(s -> copy)
        case s@Op(op) =>
          dbgs(s"Mirroring Inside Unitpipe: $s = $op")
          register(s -> mirrorSym(s))
      }
    }
  }

  private def shouldTransform(s:Sym[_], up: UnitPipe) = {
    up.ens.isEmpty && up.stopWhen.isEmpty && !s.isStreamControl && s.isInnerControl
  }

  private def transformHelper(stmts: Seq[Sym[_]]) = {
    // collect all unitpipe allocs
    val allocs = stmts filter(_.isLocalMem)

    dbgs(s"Restaging Allocs: $allocs")
    allocs.foreach {
      alloc =>
        register(alloc -> mirrorSym(alloc))
    }

    stmts foreach {
      case up@Op(pipe@UnitPipe(_, _, _)) if shouldTransform(up, pipe) =>
        dbgs(s"Restaging Unitpipe: $up, $pipe")
        indent {
          restageUnitpipe(pipe)
        }
      case s if s.isLocalMem =>
      // Already handled
      case s@Op(op) =>
        dbgs(s"Mirroring: $s")
        register(s -> transformGeneric(s, op))
    }
  }

  private def transformForeach(sym: Sym[_], foreach: OpForeach) = {
    stageWithFlow(OpForeach(foreach.ens, foreach.cchain, stageBlock {
      transformHelper(foreach.block.stms)
      spatial.lang.void
    }, foreach.iters, foreach.stopWhen)) {lhs2 => transferData(sym, lhs2)}
  }

  private def transformUnrolledForeach(sym: Sym[_], foreach: UnrolledForeach) = {
    stageWithFlow(UnrolledForeach(foreach.ens, foreach.cchain, stageBlock {
      transformHelper(foreach.func.stms)
      spatial.lang.void
    }, foreach.iterss, foreach.validss, foreach.stopWhen)) {lhs2 => transferData(sym, lhs2)}
  }

//  private def shouldTransformParent(sym: Sym[_]): Boolean = {
//    sym.children forall {
//      child => (!child.isUnitPipe) || child.sym.isInnerControl
//    }
//  }

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit srcCtx: argon.SrcCtx): Sym[A] = rhs match {
    case _: AccelScope => inAccel{ super.transform(lhs, rhs) }

    case foreach:OpForeach if lhs.isOuterControl && foreach.block.stms.exists(_.isUnitPipe) && !lhs.isStreamControl =>
      dbgs(s"Transforming: $lhs = $rhs")
      indent {
        transformForeach(lhs, foreach).asInstanceOf[Sym[A]]
      }

//    case foreach:UnrolledForeach if lhs.isOuterControl && foreach.func.stms.exists(_.isUnitPipe) && !lhs.isStreamControl =>
//      dbgs(s"Transforming: $lhs = $rhs")
//      transformUnrolledForeach(lhs, foreach).asInstanceOf[Sym[A]]

    case _ =>
      super.transform(lhs,rhs)
  }
}
