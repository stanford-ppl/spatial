package spatial.codegen.pirgen

import argon._
import argon.node._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.types._
import spatial.lang._
import spatial.node._

trait PIRGenController extends PIRCodegen {

  override def emitAccelHeader = {
    super.emitAccelHeader
    emit("""
    def controller(schedule:String):Controller = {
      val tree = ControlTree(schedule)
      beginState(tree)
      new Controller()
    }
""")
  }

  override protected def genHost(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case AccelScope(func) => 
      emit("runAccel()")
      inAccel { 
        genInAccel(lhs, rhs)
      }

    case _ => super.genHost(lhs, rhs)
  }

  def emitIterValids(lhs:Sym[_], iters:Seq[Seq[Sym[_]]], valids:Seq[Seq[Sym[_]]]) = {
    iters.zipWithIndex.foreach { case (iters, i) =>
      iters.zipWithIndex.foreach { case (iter, j) =>
        state(iter, tp=Some("Output"))(src"$lhs.cchain.T($i).iters($j)")
      }
    }
    valids.zipWithIndex.foreach { case (valids, i) =>
      valids.zipWithIndex.foreach { case (valid, j) =>
        state(valid, tp=Some("Output"))(src"$lhs.cchain.T($i).valids($j)")
      }
    }
  }

  def emitController(lhs:Sym[_], iterValids:Option[(Sym[_], Seq[Seq[Sym[_]]], Seq[Seq[Sym[_]]])], ens:Set[Bit]) = {
    val cchain = iterValids.map { _._1 }
    state(lhs, tp=Some("Controller"))(
      src"""controller(schedule="${lhs.schedule}")""" + 
      cchain.ms(chain => src".cchain($chain)") +
      (if (ens.isEmpty) "" else src".en($ens)")
    )
    iterValids match {
      case Some((cchain, iters, valids)) => emitIterValids(lhs, iters, valids)
      case _ =>
    }
    lhs.op.get.blocks.foreach(ret)
    emit(src"endState[Ctrl]")
  }

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case AccelScope(func) =>
      emitController(lhs, None, Set())

    case UnitPipe(ens, func) =>
      emitController(lhs, None, ens)

    case ParallelPipe(ens, func) =>
      emitController(lhs, None, ens)

    case UnrolledForeach(ens,cchain,func,iters,valids) =>
      emitController(lhs, Some((cchain, iters, valids)), ens)

    case UnrolledReduce(ens,cchain,func,iters,valids) =>
      emitController(lhs, Some((cchain, iters, valids)), ens)

    case op@Switch(selects, body) =>
      emit(s"//TODO: ${qdef(lhs)}")

    case SwitchCase(body) => // Controlled by Switch
      emit(s"//TODO: ${qdef(lhs)}")

    case StateMachine(ens, start, notDone, action, nextState) =>
      emit(s"//TODO: ${qdef(lhs)}")

    case IfThenElse(cond, thenp, elsep) =>
      emit(s"//TODO: ${qdef(lhs)}")

    case _ => super.genAccel(lhs, rhs)
  }

}
