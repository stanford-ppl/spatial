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
      inAccel { 
        genInAccel(lhs, rhs)
      }

    case _ => super.genHost(lhs, rhs)
  }

  def emitIterValids(lhs:Sym[_], iters:Seq[Seq[Bits[_]]], valids:Seq[Seq[Bits[_]]]) = {
    def quoteIdx(sym:Bits[_]):String = {
      sym.counter.lanes.toString
    }
    iters.zipWithIndex.foreach { case (iters, i) =>
      iters.zipWithIndex.foreach { case (iter, j) =>
        state(iter)(src"CounterIter(${quoteIdx(iter)}).counter($lhs.cchain.T($i)).resetParent($lhs).tp(${iter.tp})")
      }
    }
    valids.zipWithIndex.foreach { case (valids, i) =>
      valids.zipWithIndex.foreach { case (valid, j) =>
        state(valid)(src"CounterValid(${quoteIdx(valid)}).counter($lhs.cchain.T($i)).resetParent($lhs).tp(${valid.tp})")
      }
    }
  }

  def emitController(
    lhs:Lhs, 
    ctrler:Option[String]=None,
    schedule:Option[Any]=None,
    cchain:Option[Sym[_]]=None, 
    iters:Seq[Seq[Bits[_]]]=Nil, 
    valids: Seq[Seq[Bits[_]]]=Nil, 
    ens:Set[Bit]=Set.empty
  )(blk: => Unit) = {
    val newCtrler = ctrler.getOrElse("UnitController()")
    val tp = newCtrler.trim.split("\\(")(0).split(" ").last
    state(lhs, tp=Some(tp))(
      src"""create(schedule="${schedule.getOrElse(lhs.sym.schedule)}")(${newCtrler})""" + 
      cchain.ms(chain => src".cchain($chain)") +
      (if (ens.isEmpty) "" else src".en($ens)")
    )
    emitIterValids(lhs.sym, iters, valids)
    blk
    emit(src"endState[Ctrl]")
  }

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case AccelScope(func) =>
      emitController(lhs) { ret(func) }

    case UnitPipe(ens, func) =>
      emitController(lhs, ens=ens) { ret(func) }

    case ParallelPipe(ens, func) =>
      emitController(lhs, ens=ens) { ret(func) }

    case UnrolledForeach(ens,cchain,func,iters,valids,_) =>
      emitController(lhs, ctrler=Some("LoopController()"), cchain=Some(cchain), iters=iters, valids=valids, ens=ens) { ret(func) }

    case UnrolledReduce(ens,cchain,func,iters,valids,_) =>
      emitController(lhs, ctrler=Some("LoopController()"), cchain=Some(cchain), iters=iters, valids=valids, ens=ens) { ret(func) }

    case op@Switch(selects, body) =>
      emitController(lhs) { ret(body) }
      val cases = body.stms.collect{case sym@Op(op:SwitchCase[_]) => sym }
      cases.zipWithIndex.foreach { case (c, i) =>
        emit(src"$c.en(${selects(i)})")
      }

    case SwitchCase(body) => // Controlled by Switch
      emitController(lhs) { ret(body) }

    case StateMachine(ens, start, notDone, action, nextState) =>
      emit(s"//TODO: ${qdef(lhs)}")

    //case IfThenElse(cond, thenp, elsep) =>

    case _ => super.genAccel(lhs, rhs)
  }

}
