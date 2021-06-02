package spatial.transform

import argon._
import argon.transform.{ForwardTransformer, MutateTransformer}
import spatial.lang._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.node._
import spatial.traversal.AccelTraversal

import scala.collection.mutable

case class RegWriteReadElimination(IR: State) extends MutateTransformer with AccelTraversal {
  // If a register is written to and then read in the same controller, we can directly connect the write to the read.
  // We can then drop the register.

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {

    case AccelScope(_) => inAccel { dbgs("In Accel"); super.transform(lhs, rhs) }

    case ctrl: Control[_] if inAccel && (lhs.isUnitPipe || lhs.isForeach) && lhs.isInnerControl =>
      dbgs(s"Processing: $lhs = $rhs")
//      routeThroughRegs(ctrl)
      val mirrored = mirrorSym(lhs)
      register(lhs -> mirrored)
      mirrored

    case _ =>
      super.transform(lhs, rhs)
  }

  private def routeThroughRegs(ctrl: Control[_]) = {
    ctrl.bodies flatMap {
      _.blocks
    } foreach {
      case (_, block) =>
        // Reg -> current value
        val regCache = mutable.Map[Sym[_], Bits[_]]()
        val copied = stageBlock({
          block.stms foreach {
              case stmt@Op(RegNew(init)) if !stmt.keepUnused && isEliminatable(stmt) =>
                dbgs(s"Registering $stmt <- $init")
                regCache(stmt) = init
              case stmt@Op(RegWrite(mem, data, ens)) if regCache contains mem =>
                implicit lazy val bEV: Bits[data.R] = data
                if (ens.isEmpty) {
                  regCache(mem) = data
                } else {
                  regCache(mem) = stage(Mux(ens.reduce {
                    _ && _
                  }, data, regCache(mem).asInstanceOf[Bits[data.R]]))
                }
                dbgs(s"Updating $mem <- ${regCache(mem)}, ens: $ens, eliminated $stmt")
                subst += (stmt -> Invalid)
              case stmt@Op(RegRead(mem)) if regCache contains mem =>
                dbgs(s"Reading: $mem, eliminated $stmt")
                subst += (stmt -> regCache(mem).asSym)

              case stmt@Op(op) =>
                dbgs(s"Updating: $stmt = $op")
                subst += (stmt -> mirrorSym(stmt))
          }
        }, block.options)
        register(block -> copied)
    }
  }

  private def isEliminatable(s: Sym[_]): Boolean = {
    // A register is eliminatable if:
    // all readers happen after a guaranteed write
    if (s.resetters.nonEmpty) return false
    s.writers forall {
        case Writer((_, _, _, ens)) => ens.isEmpty || ens.forall {
          case Const(c) => c.value
          case _ => false
        }
    }
  }
}
