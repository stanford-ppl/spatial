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

    case ctrl: Control[_] if inAccel && (lhs.isUnitPipe || lhs.isForeach) =>
      dbgs(s"Processing: $lhs = $rhs")
      routeThroughRegs(ctrl)
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
        val regCache = mutable.Map[Sym[_], Bits[_]]().withDefault({case Op(RegNew(init)) => init})

        val eliminationCache = (block.stms flatMap {
          case Op(RegWrite(mem, _, _)) => Some(mem.asSym)
          case Op(RegRead(mem)) => Some(mem.asSym)
          case Op(_) => None
        }).toSet.map({
          mem: Sym[_] => mem -> isEliminatable(mem)
        }).toMap[Sym[_], Boolean].withDefaultValue(false)

        dbgs(s"$eliminationCache")

        val copied = stageBlock({
          block.stms foreach {
              case stmt@Op(RegWrite(mem, data, ens)) if eliminationCache(mem) =>
                implicit lazy val bEV: Bits[data.R] = data
                if (ens.isEmpty) {
                  regCache(mem) = f(data)
                } else {
                  regCache(mem) = stage(Mux(ens.reduce {
                    _ && _
                  }, f(data), regCache(mem).asInstanceOf[Bits[data.R]]))
                }
                dbgs(s"Updating $mem <- ${regCache(mem)}, ens: $ens, eliminated $stmt")
                subst += (stmt -> Invalid)
              case stmt@Op(RegRead(mem)) if eliminationCache(mem) =>
                dbgs(s"Reading: $mem, eliminated $stmt")
                subst += (stmt -> regCache(mem).asSym)

              case stmt if eliminationCache(stmt) =>
                dbgs(s"Eliminating: ${stmt}")
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
    // All users of the register are in the same controller
    if (s.resetters.nonEmpty) return false
    if (s.isNonBuffer) return false
    val guaranteedWrite = s.writers forall {
        case Writer((_, _, _, ens)) => ens.isEmpty || ens.forall {
          case Const(c) => c.value
          case _ => false
        }
    }
    dbgs(s"$s : ${s.accesses}")
    dbgs(s"${metadata.all(s).toList}")
    val ancestors = (s.accesses map {
      sym => sym.parent
    })
    val hasSingleAncestor = ancestors.size == 1

    dbgs(s"isEliminatable($s) = $guaranteedWrite && $hasSingleAncestor")
    indent {
      dbgs(s"Accesses: ${s.accesses}, Ancestors: $ancestors")
    }

    guaranteedWrite && hasSingleAncestor
  }
}
