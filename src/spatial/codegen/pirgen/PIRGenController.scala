package spatial.codegen.pirgen

import argon._
import argon.node._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.types._
import spatial.lang._
import spatial.node._

trait PIRGenController extends PIRGenControl with PIRGenStream with PIRGenMemories {

  // In PIR simulation, run a pipe until its read fifos and streamIns are empty
  def getReadStreamsAndFIFOs(ctrl: Ctrl): Set[Sym[_]] = {
    ctrl.children.flatMap(getReadStreamsAndFIFOs).toSet ++
    LocalMemories.all.filter{mem => mem.readers.exists{_.parent == ctrl }}
                     .filter{mem => mem.isStreamIn || mem.isFIFO }
                     .filter{case Op(StreamInNew(bus)) => !bus.isInstanceOf[DRAMBus[_]]; case _ => true}
  }

  def emitControlBlock(lhs: Sym[_], block: Block[_]): Unit = {
    if (lhs.isOuterStreamControl) {
      val children = lhs.children
      block.stms.foreach { stm =>
        val isChild = children.exists{child => child.s.contains(stm) }

        if (isChild) {
          val contents = block.nestedStms.toSet
          val inputs = getReadStreamsAndFIFOs(stm.toCtrl) diff contents  // Don't check things we declare inside
          if (inputs.nonEmpty) {
            // HACK: Run streaming children to completion (exhaust inputs) before moving on to the next
            // Note that this won't work for cases with feedback, but this is disallowed for now anyway
            emit(src"def hasItems_${lhs}_$stm: Boolean = " + inputs.map(quote).map(_ + ".nonEmpty").mkString(" || "))
            open(src"while (hasItems_${lhs}_$stm) {")
            visit(stm)
            close("}")
          }
          else {
            visit(stm)
          }
        }
        else visit(stm)
      }
    }
    else {
      gen(block)
    }
  }

  private def emitUnrolledLoop(
    lhs:    Sym[_],
    cchain: CounterChain,
    iters:  Seq[Seq[I32]],
    valids: Seq[Seq[Bit]]
  )(func: => Unit): Unit = {

    val ctrs = cchain.counters

    for (i <- iters.indices) {
      if (ctrs(i).isForever) {
        val inputs = getReadStreamsAndFIFOs(lhs.toCtrl)
        if (inputs.nonEmpty) {
          emit(src"def hasItems_$lhs: Boolean = " + inputs.map(quote).map(_ + ".nonEmpty").mkString(" || "))
        }
        else {
          emit(s"""print("No Stream inputs detected for loop at ${lhs.ctx}. Enter number of iterations: ")""")
          emit(src"val ${lhs}_iters_$i = Console.readLine.toInt")
          emit(src"var ${lhs}_ctr_$i = 0")
          emit(src"def hasItems_$lhs: Boolean = { val has = ${lhs}_ctr_$i < ${lhs}_iters_$i ; ${lhs}_ctr_$i += 1; has }")
        }

        open(src"while(hasItems_$lhs) {")
        iters(i).zipWithIndex.foreach { case (iter, j) => emit(src"val $iter = FixedPoint.fromInt(1)") }
        valids(i).zipWithIndex.foreach { case (valid, j) => emit(src"val $valid = Bool(true,true)") }
      }
      else {
        open(src"$cchain($i).foreach{case (is,vs) => ")
        iters(i).zipWithIndex.foreach { case (iter, j) => emit(src"val $iter = is($j)") }
        valids(i).zipWithIndex.foreach { case (valid, j) => emit(src"val $valid = vs($j)") }
      }
    }

    func
    iters.reverse.foreach{is =>
      emitControlIncrement(lhs, is)
      close("}")
    }
  }

  //inGen(kernel(lhs)){ // open another file
  //}

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case AccelScope(func) =>
      inGen(out, "AccelMain.scala") {
        define(lhs, "Controller(schedule=)")
        ret(func)
      }

    case UnitPipe(ens, func) =>
      ret(func)

    case ParallelPipe(ens, func) =>
      ret(func)

    case UnrolledForeach(ens,cchain,func,iters,valids) =>
      define(lhs, s"LoopController", "cchain" -> cchain, "ens"->ens)
      ret(func)

    case UnrolledReduce(ens,cchain,func,iters,valids) =>
      ret(func)

    case op@Switch(selects, body) =>
      ret(body)

    case SwitchCase(body) => // Controlled by Switch
      ret(body)

    case StateMachine(ens, start, notDone, action, nextState) =>
      ret(notDone)
      ret(action)
      ret(nextState)

    case IfThenElse(cond, thenp, elsep) =>
      ret(thenp)
      ret(elsep)

    case _ => super.gen(lhs, rhs)
  }
}
