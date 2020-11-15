package spatial.codegen.pirgen

import argon._
import argon.node._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.types._
import spatial.lang._
import spatial.node._

trait PIRGenController extends PIRCodegen {

  def emitController(
    lhs:Lhs, 
    ctrler:Option[String]=None,
    cchain:Option[Sym[_]]=None, 
    iters:Seq[Seq[Bits[_]]]=Nil, 
    valids: Seq[Seq[Bits[_]]]=Nil, 
    resets: Seq[Seq[Bits[_]]]=Nil, 
    stopWhen:Option[Sym[_]]=None,
    ens:Set[Bit]=Set.empty
  )(blk: => Unit) = {
    var newCtrler = ctrler.getOrElse("UnitController()")
    val tp = newCtrler.trim.split("\\(")(0).split(" ").last
    newCtrler += cchain.ms(chain => src".cchain($chain)")
    if (ens.nonEmpty) {
      newCtrler += src".en($ens)"
    }
    stopWhen.foreach { stopWhen =>
      newCtrler += src".stopWhen(MemRead().setMem($stopWhen))"
    }
    lhs.sym.unrollBy.foreach { par =>
      newCtrler += src".par($par)"
    }
    state(lhs, tp=Some(tp))(
      src"""createCtrl(schedule=${lhs.sym.schedule})(${newCtrler})"""
    )
    def quoteIdx(sym:Bits[_]):String = {
      sym.counter.lanes.toString
    }
    val cchain_valid = cchain match {
      case Some(x) => true
      case _ => false
    }

    if (cchain_valid && cchain.get.isScanner) {
      iters.transpose.zipWithIndex.foreach { case (iters_x, j) =>
        iters_x.zipWithIndex.foreach { case (iter, i) =>
          state(iter)(src"CounterIter(${quoteIdx(iters_x.head)}).counter($lhs.cchain.T($i)).resetParent($lhs).tp(${iters_x.head.tp})")
        }
      }
      valids.transpose.zipWithIndex.foreach { case (valids_x, j) =>
        valids_x.zipWithIndex.foreach { case (valid, i) =>
          if (i == 0) {
            state(valid)(src"CounterValid(${quoteIdx(valids_x.head)}).counter($lhs.cchain.T(0)).resetParent($lhs /* $i, $j */).tp(${valids_x.head.tp})")
          } else {
            // state(valid)(src"/* $i, $j */ ${valids_x.head}")
            state(valid)(src"Const(true).tp(Bool)")
          }
        }
      }
      resets.transpose.zipWithIndex.foreach { case (resets_x, j) =>
        resets_x.zipWithIndex.foreach { case (reset, i) =>
          if (i == 0) {
            state(reset)(src"CounterReset(${quoteIdx(resets_x.head)}).counter($lhs.cchain.T(0)).resetParent($lhs).tp(${resets_x.head.tp})")
          } else {
            state(reset)(src"Const(true).tp(Bool)")
          }
        }
      }
    } else {
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
      resets.zipWithIndex.foreach { case (resets, i) =>
        resets.zipWithIndex.foreach { case (reset, j) =>
          state(reset)(src"CounterReset(${quoteIdx(reset)}).counter($lhs.cchain.T($i)).resetParent($lhs).tp(${reset.tp})")
        }
      }
    }
    blk
    emit(src"endState[Ctrl]")
  }

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case AccelScope(func) =>
      emitController(lhs) { ret(func) }

    case UnitPipe(ens, func, _) =>
      emitController(lhs, ens=ens) { ret(func) }

    case ParallelPipe(ens, func) =>
      emitController(lhs, ens=ens) { ret(func) }

    case UnrolledForeach(ens,cchain,func,iters,valids,resets,stopWhen) =>
      emitController(lhs, ctrler=Some("LoopController()"), cchain=Some(cchain), iters=iters, valids=valids, resets=resets, ens=ens, stopWhen=stopWhen) { ret(func) }

    case UnrolledReduce(ens,cchain,func,iters,valids,resets,stopWhen) =>
      emitController(lhs, ctrler=Some("LoopController()"), cchain=Some(cchain), iters=iters, valids=valids, resets=resets, ens=ens, stopWhen=stopWhen) { ret(func) }

    case op@Switch(selects, body) =>
      emitController(lhs) { ret(body) }
      val cases = body.stms.collect{case sym@Op(op:SwitchCase[_]) => sym }
      cases.zipWithIndex.foreach { case (c, i) =>
        emit(src"$c.en(${selects(i)})")
      }

    case SwitchCase(body) => // Controlled by Switch
      emitController(lhs) { ret(body) }

    case StateMachine(ens, start, notDone, action, nextState) =>
      emit(src"//TODO: ${qdef(lhs)}")

    //case IfThenElse(cond, thenp, elsep) =>
    
    case SplitterStart(addr) =>
      state(lhs, tp=Some("SplitController"))(
        src"""createCtrl(schedule=Pipelined)(SplitController().splitOn($addr))"""
      )

    case SplitterEnd(addr) =>
      emit(src"endState[Ctrl]")

    case _ => super.genAccel(lhs, rhs)
  }

}
