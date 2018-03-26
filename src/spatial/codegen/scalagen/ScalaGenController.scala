package spatial.codegen.scalagen

import argon._
import spatial.data._
import spatial.lang._
import spatial.node._
import spatial.util._

trait ScalaGenController extends ScalaGenControl with ScalaGenStream with ScalaGenMemories {
  // In Scala simulation, run a pipe until its read fifos and streamIns are empty
  def getReadStreamsAndFIFOs(ctrl: Ctrl): List[Sym[_]] = {
    val read = localMems.filter{mem => readersOf(mem).exists(_.parent == ctrl) }
                        .filter{mem => mem.isStreamIn || mem.isFIFO } ++ ctrl.children.flatMap(getReadStreamsAndFIFOs)

    read.filter{case Op(StreamInNew(bus)) => !bus.isInstanceOf[DRAMBus[_]]; case _ => true}
    //read //diff getWrittenStreamsAndFIFOs(ctrl) // Don't also wait for things we're writing to
  }

  def isStreaming(ctrl: Sym[_]): Boolean = ctrl.isStreamPipe

  def emitControlBlock(lhs: Sym[_], block: Block[_]): Unit = {
    if (isOuterControl(lhs) && isStreaming(lhs)) {
      val children = lhs.children
      lazy val contents = block.nestedStms

      block.stms.foreach{sym =>
        if (children.contains(sym)) {
          val inputs = getReadStreamsAndFIFOs(sym.toCtrl) diff contents  // Don't check things we declare inside
          if (inputs.nonEmpty) {
            // HACK: Run streaming children to completion (exhaust inputs) before moving on to the next
            // Note that this won't work for cases with feedback, but this is disallowed for now anyway
            emit(src"def hasItems_${lhs}_$sym: Boolean = " + inputs.map(quote).map(_ + ".nonEmpty").mkString(" || "))
            open(src"while (hasItems_${lhs}_$sym) {")
              visit(sym)
            close("}")
          }
          else {
            visit(sym)
          }
        }
        else visit(sym)
      }
    }
    else {
      gen(block)
    }
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case AccelScope(func) =>
      localMems.filterNot(_.isRemoteMem).foreach{lhs => emit(src"var $lhs: ${lhs.tp} = null") }
      localMems.filter(_.isInternalStream).foreach{lhs => emit(src"var $lhs: ${lhs.tp} = new ${lhs.tp}") }

      emit(src"/** BEGIN HARDWARE BLOCK $lhs **/")
      globalMems = true
      if (!lhs.willRunForever) {
        open(src"def accel(): Unit = {")
          open(src"val $lhs = {")
            ret(func)
          close("}")
        close("}")
        emit("accel()")
      }
      else {
        if (streamIns.nonEmpty) {
          emit(src"def hasItems = " + streamIns.map(quote).map(_ + ".nonEmpty").mkString(" || "))
        }
        else {
          emit(s"""print("No Stream inputs detected for loop at ${lhs.ctx}. Enter number of iterations: ")""")
          emit(src"val ${lhs}_iters = Console.readLine.toInt")
          emit(src"var ${lhs}_ctr = 0")
          emit(src"def hasItems: Boolean = { val has = ${lhs}_ctr < ${lhs}_iters ; ${lhs}_ctr += 1; has }")
        }
        open(src"while(hasItems) {")
          emitControlBlock(lhs, func)
        close("}")
        emit(src"val $lhs = ()")
      }
      streamOuts.foreach{case x@Op(StreamOutNew(bus)) =>
        if (!bus.isInstanceOf[DRAMBus[_]]) emit(src"print_$x()") // HACK: Print out streams after block finishes running
      }
      emitControlDone(lhs)
      bufferedOuts.foreach{buff => emit(src"close_$buff()") }
      globalMems = false
      emit(src"/** END HARDWARE BLOCK $lhs **/")


    case UnitPipe(ens, func) =>
      emit(src"/** BEGIN UNIT PIPE $lhs **/")
      val en = if (ens.isEmpty) "true" else ens.map(quote).mkString(" && ")
      open(src"val $lhs = if ($en) {")
        emitControlBlock(lhs, func)
        emitControlDone(lhs)
      close("}")
      emit(src"/** END UNIT PIPE $lhs **/")

    case ParallelPipe(ens, func) =>
      emit(src"/** BEGIN PARALLEL PIPE $lhs **/")
      val en = if (ens.isEmpty) "true" else ens.map(quote).mkString(" && ")
      open(src"val $lhs = if ($en) {")
        emitBlock(func)
        emitControlDone(lhs)
      close("}")
      emit(src"/** END PARALLEL PIPE $lhs **/")


    // These should no longer exist in the IR at codegen time
    /*
    case _:OpForeach =>
    case _:OpReduce[_] =>
    case _:OpMemReduce[_,_] =>
    */

    case _ => super.gen(lhs, rhs)
  }
}
