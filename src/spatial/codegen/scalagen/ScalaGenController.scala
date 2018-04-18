package spatial.codegen.scalagen

import argon._
import spatial.data._
import spatial.lang._
import spatial.node._
import spatial.util._

trait ScalaGenController extends ScalaGenControl with ScalaGenStream with ScalaGenMemories {

  def isStreaming(ctrl: Sym[_]): Boolean = ctrl.isStreamPipe

  // In Scala simulation, run a pipe until its read fifos and streamIns are empty
  def getReadStreamsAndFIFOs(ctrl: Ctrl): Set[Sym[_]] = {
    ctrl.children.flatMap(getReadStreamsAndFIFOs).toSet ++
    localMems.all.filter{mem => readersOf(mem).exists{_.parent == ctrl }}
                 .filter{mem => mem.isStreamIn || mem.isFIFO }
                 .filter{case Op(StreamInNew(bus)) => !bus.isInstanceOf[DRAMBus[_]]; case _ => true}
  }

  def emitControlBlock(lhs: Sym[_], block: Block[_]): Unit = {
    if (isOuterControl(lhs) && isStreaming(lhs)) {
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

    val ctrs = cchain.ctrs

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

  private def emitControlObject(lhs: Sym[_], ens: Set[Bit], func: Block[_])(contents: => Unit): Unit = {
    val ins    = func.nestedInputs
    val binds  = lhs.op.map{d => d.binds ++ d.blocks.map(_.result) }.getOrElse(Nil)
    val inputs = (lhs.op.map{_.inputs}.getOrElse(Nil) ++ ins).distinct.filterNot(_.isMem) diff binds
    val en = if (ens.isEmpty) "true" else ens.map(quote).mkString(" && ")

    dbgs(s"${stm(lhs)}")
    inputs.foreach{in => dbgs(s" - ${stm(in)}") }

    inGen(kernel(lhs)){
      emitHeader()
      open(src"object $lhs {")
      open(src"def run(")
      inputs.zipWithIndex.foreach{case (in,i) => emit(src"$in: ${in.tp}" + (if (i == inputs.length-1) "" else ",")) }
      closeopen("): Unit = {")
      open(src"if ($en) {")
      contents
      close("}")
      close("}")
      close("}")
      emitFooter()
    }
    emit(src"$lhs.run($inputs)")
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case AccelScope(func) =>
      emit(src"/** BEGIN HARDWARE BLOCK $lhs **/")
      globalMems = true
      if (!lhs.willRunForever) {
        open(src"def accel(): Unit = {")
        open(src"val $lhs = try {")
        visitBlock(func)
        close("}")
        open("catch {")
        emit(src"""case x: Exception if x.getMessage == "exit" =>  """)
        emit(src"""case t: Throwable => throw t""")
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
        if (!bus.isInstanceOf[DRAMBus[_]]) emit(src"$x.dump()") // HACK: Print out streams after block finishes running
      }
      emitControlDone(lhs)
      bufferedOuts.foreach{buff => emit(src"$buff.close()") }
      globalMems = false
      emit(src"/** END HARDWARE BLOCK $lhs **/")


    case UnitPipe(ens, func) =>
      emitControlObject(lhs, ens, func) {
        emit(src"/** BEGIN UNIT PIPE $lhs **/")
        emitControlBlock(lhs, func)
        emitControlDone(lhs)
        emit(src"/** END UNIT PIPE $lhs **/")
      }


    case ParallelPipe(ens, func) =>
      emitControlObject(lhs, ens, func){
        emit(src"/** BEGIN PARALLEL PIPE $lhs **/")
        visitBlock(func)
        emitControlDone(lhs)
        emit(src"/** END PARALLEL PIPE $lhs **/")
      }

    case UnrolledForeach(ens,cchain,func,iters,valids) =>
      emitControlObject(lhs, ens, func) {
        emit(src"/** BEGIN UNROLLED FOREACH $lhs **/")
        emitUnrolledLoop(lhs, cchain, iters, valids) {
          emitControlBlock(lhs, func)
        }
        emitControlDone(lhs)
        emit(src"/** END UNROLLED FOREACH $lhs **/")
      }

    case UnrolledReduce(ens,cchain,func,iters,valids) =>
      emitControlObject(lhs, ens, func) {
        emit(src"/** BEGIN UNROLLED REDUCE $lhs **/")
        emitUnrolledLoop(lhs, cchain, iters, valids) {
          emitControlBlock(lhs, func)
        }
        emitControlDone(lhs)
        emit(src"/** END UNROLLED REDUCE $lhs **/")
      }


    case _ => super.gen(lhs, rhs)
  }
}
