package spatial.codegen.pirgen

import argon._
import argon.node._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.types._
import spatial.lang._
import spatial.node._

trait PIRGenController extends PIRCodegen {

  override protected def genHost(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case AccelScope(func) => 
      emit("runAccel()")
      openAccel {
        inAccel { 
          gen(lhs, rhs)
        }
      }

    case _ => super.genHost(lhs, rhs)
  }

  def emitIterValids(lhs:Sym[_], iters:Seq[Seq[Sym[_]]], valids:Seq[Seq[Sym[_]]]) = {
    iters.zipWithIndex.foreach { case (iters, i) =>
      iters.zipWithIndex.foreach { case (iter, j) =>
        state(iter, tp=Some("IterDef"))(src"$lhs.iters($i)($j)")
      }
    }
    valids.zipWithIndex.foreach { case (valids, i) =>
      valids.zipWithIndex.foreach { case (valid, j) =>
        state(valid, tp=Some("ValidDef"))(src"$lhs.valids($i)($j)")
      }
    }
  }
  def emitController(lhs:Sym[_], cchain:Option[Sym[_]], ens:Set[Bit]) = {
    val ctrs = cchain match {
      case Some(cchain) => cchain
      case _ => Nil
    }
    state(lhs, tp=Some("Controller"))(src"""createController(cchain=$ctrs, ens=${ens}, schedule="${lhs.schedule}")""")
  }

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case AccelScope(func) =>
      emitController(lhs, None, Set())
      ret(func)

    case UnitPipe(ens, func) =>
      emitController(lhs, None, ens)
      ret(func)

    case ParallelPipe(ens, func) =>
      emitController(lhs, None, ens)
      ret(func)

    case UnrolledForeach(ens,cchain,func,iters,valids) =>
      emitController(lhs, Some(cchain), ens)
      emitIterValids(lhs, iters, valids)
      ret(func)

    case UnrolledReduce(ens,cchain,func,iters,valids) =>
      emitController(lhs, Some(cchain), ens)
      emitIterValids(lhs, iters, valids)
      ret(func)

    case op@Switch(selects, body) =>
      emit(s"//TODO: ${qdef(lhs)}")
      ret(body)

    case SwitchCase(body) => // Controlled by Switch
      emit(s"//TODO: ${qdef(lhs)}")
      ret(body)

    case StateMachine(ens, start, notDone, action, nextState) =>
      emit(s"//TODO: ${qdef(lhs)}")
      ret(notDone)
      ret(action)
      ret(nextState)

    case IfThenElse(cond, thenp, elsep) =>
      emit(s"//TODO: ${qdef(lhs)}")
      ret(thenp)
      ret(elsep)

    case _ => super.genAccel(lhs, rhs)
  }

}
