package spatial.transform

import argon._
import argon.transform.{ForwardTransformer, MutateTransformer}
import spatial.lang._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.node._

import scala.collection.mutable

case class RegWriteReadElimination(IR: State) extends MutateTransformer {
  // If a register is written to and then read in the same controller, we can directly connect the write to the read.

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case ctrl: Control[_] if lhs.isInnerControl =>
      indent {
        dbgs(s"Processing: $lhs = $rhs")
        routeThroughRegs(ctrl)
        super.transform(lhs, rhs)
      }

    case _ => super.transform(lhs, rhs)
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
              stmt =>
                stmt.op match {
                  case Some(RegNew(init)) if !stmt.keepUnused && isEliminatable(stmt) =>
                    dbgs(s"Registering $stmt <- $init")
                    regCache(stmt) = init
                  case Some(RegWrite(mem, data, ens)) if regCache contains mem =>
                    implicit lazy val bEV: Bits[data.R] = data
                    if (ens.isEmpty) {
                      regCache(mem) = data
                    } else {
                      regCache(mem) = stage(Mux(ens.reduce {
                        _ && _
                      }, data, regCache(mem).asInstanceOf[Bits[data.R]]))
                    }
                    dbgs(s"Updating $mem <- ${regCache(mem)}, ens: $ens")
                  case Some(RegRead(mem)) if regCache contains mem =>
                    dbgs(s"Reading: $mem")
                    subst += (stmt -> regCache(mem).asSym)

                  case Some(op) =>
                    val updated = update(stmt.asInstanceOf[Sym[stmt.R]], op.asInstanceOf[Op[stmt.R]])
                    subst += (stmt -> updated)
                  case None =>
                }
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
