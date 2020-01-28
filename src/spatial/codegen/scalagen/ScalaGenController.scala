package spatial.codegen.scalagen

import argon._
import argon.node._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.types._
import spatial.lang._
import spatial.node._
import spatial.util.spatialConfig

trait ScalaGenController extends ScalaGenControl with ScalaGenStream with ScalaGenMemories {

  // In Scala simulation, run a pipe until its read fifos and streamIns are empty
  def getReadStreamsAndFIFOs(ctrl: Ctrl): Set[Sym[_]] = {
    ctrl.children.flatMap(getReadStreamsAndFIFOs).toSet ++
    LocalMemories.all.filter{mem => mem.readers.exists{_.parent == ctrl }}
                     .filter{mem => mem.isStreamIn || mem.isFIFO || mem.isMergeBuffer }
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

        lhs match {
          case Op(UnrolledForeach(_,_,_,_,_,stopWhen)) if stopWhen.isDefined => 
            warn("breakWhen detected!  Note scala break occurs at the end of the loop, while --synth break occurs immediately")
            open(src"while(hasItems_$lhs && !${stopWhen.get}.value) {")
          case Op(UnrolledReduce(_,_,_,_,_,stopWhen)) if stopWhen.isDefined => 
            warn("breakWhen detected!  Note scala break occurs at the end of the loop, while --synth break occurs immediately")
            open(src"while(hasItems_$lhs && !${stopWhen.get}.value) {")
          case _ => open(src"while(hasItems_$lhs) {")
        }        
        iters(i).foreach { iter => emit(src"val $iter = FixedPoint.fromInt(1)") }
        valids(i).foreach { valid => emit(src"val $valid = Bool(true,true)") }
      }
      else {
        lhs match {
          case Op(UnrolledForeach(_,_,_,_,_,stopWhen)) if stopWhen.isDefined => 
            warn("breakWhen detected!  Note scala break occurs at the end of the loop, while --synth break occurs immediately")
            open(src"$cchain($i).takeWhile(!${stopWhen.get}.value){case (is,vs) => ")
          case Op(UnrolledReduce(_,_,_,_,_,stopWhen)) if stopWhen.isDefined => 
            warn("breakWhen detected!  Note scala break occurs at the end of the loop, while --synth break occurs immediately")
            open(src"$cchain($i).takeWhile(!${stopWhen.get}.value){case (is,vs) => ")
          case _ => open(src"$cchain($i).foreach{case (is,vs) => ")
        }        
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

  private def emitControlObject(lhs: Sym[_], ens: Set[Bit], func: Block[_]*)(contents: => Unit): Unit = {
    // Find everything that is used in this scope
    // Only use the non-block inputs to LHS since we already account for the block inputs in nestedInputs
    val used: Set[Sym[_]] = lhs.nonBlockInputs.toSet ++ func.flatMap{block => block.nestedInputs }
    val made: Set[Sym[_]] = lhs.op.map{d => d.binds }.getOrElse(Set.empty)
    val inputs: Seq[Sym[_]] = (used diff made).filterNot{s => s.isMem || s.isValue }.toSeq

    dbgs(s"${stm(lhs)}")
    inputs.foreach{in => dbgs(s" - ${stm(in)}") }
    val gate = if (ens.nonEmpty) src"if (${and(ens)})" else ""
    val els  = if (ens.nonEmpty) src"else null.asInstanceOf[${lhs.tp}]" else ""

    val useMap = inputs.flatMap{s => scoped.get(s).map{v => s -> v}}
    scoped --= useMap.map(_._1)

    inGen(kernel(lhs)){
      emitHeader()
      emit(src"/** BEGIN ${lhs.op.get.name} $lhs **/")
      open(src"object ${lhs}_kernel {")
        open(src"def run(")
          inputs.zipWithIndex.foreach{case (in,i) => emit(src"$in: ${in.tp}" + (if (i == inputs.size-1) "" else ",")) }
        closeopen(src"): ${lhs.tp} = $gate{")
          contents
          lineBufSwappers.getOrElse(lhs, Set()).foreach{x => emit(src"$x.swap()")}
        close(s"} $els")
      close("}")
      emit(src"/** END ${lhs.op.get.name} $lhs **/")
      emitFooter()
    }
    scoped ++= useMap
    emit(src"val $lhs = ${lhs}_kernel.run($inputs)")
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case AccelScope(func) =>
      emitControlObject(lhs, Set.empty, func){
        open("try {")
        if (spatialConfig.enableResourceReporter) emit("StatTracker.pushState(true)")
        globalMems = true
        if (!lhs.willRunForever) {
          gen(func)
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
        }
        // HACK: Print out streams after block finishes running
        streamOuts.foreach{case x@Op(StreamOutNew(bus)) =>
          if (!bus.isInstanceOf[DRAMBus[_]]) emit(src"$x.dump()")
        }
        emitControlDone(lhs)
        bufferedOuts.foreach{buff => emit(src"$buff.close()") }
        globalMems = false
        if (spatialConfig.enableResourceReporter) emit("StatTracker.popState()")
        close("}")
        open("catch {")
          emit(src"""case x: Exception if x.getMessage == "exit" =>  """)
          emit(src"""case t: Throwable => throw t""")
        close("}")
      }

    case UnitPipe(ens, func, _) =>
      emitControlObject(lhs, ens, func) {
        emitControlBlock(lhs, func)
        emitControlDone(lhs)
      }


    case ParallelPipe(ens, func) =>
      emitControlObject(lhs, ens, func){
        gen(func)
        emitControlDone(lhs)
      }

    case UnrolledForeach(ens,cchain,func,iters,valids,_) =>
      emitControlObject(lhs, ens, func) {
        emitUnrolledLoop(lhs, cchain, iters, valids) {
          emitControlBlock(lhs, func)
        }
        emitControlDone(lhs)
      }

    case UnrolledReduce(ens,cchain,func,iters,valids,_) =>
      emitControlObject(lhs, ens, func) {
        emitUnrolledLoop(lhs, cchain, iters, valids) {
          emitControlBlock(lhs, func)
        }
        emitControlDone(lhs)
      }

    case op@Switch(selects, body) =>
      emitControlObject(lhs, Set.empty, body){
        selects.indices.foreach { i =>
          open(src"""${if (i == 0) "if" else "else if"} (${selects(i)}) {""")
          ret(op.cases(i).body)
          close("}")
        }
        if (op.R.isBits)      emit(src"else { ${invalid(op.R)} }")
        else if (op.R.isVoid) emit(src"else ()")
        else                  emit(src"else { null.asInstanceOf[${op.R}]")
        emitControlDone(lhs)
      }

    case SwitchCase(_) => // Controlled by Switch

    case StateMachine(ens, start, notDone, action, nextState) =>
      emitControlObject(lhs, ens, notDone, action, nextState){
        val stat = notDone.input
        emit(src"var $stat: ${stat.tp} = $start")
        open(src"def notDone() = {")
          ret(notDone)
        close("}")
        open(src"while( notDone() ){")
          gen(action)
          gen(nextState)
          emit(src"$stat = ${nextState.result}")
        close("}")
        emitControlDone(lhs)
      }

    case IfThenElse(cond, thenp, elsep) =>
      emitControlObject(lhs, Set.empty, thenp, elsep){
        open(src"if ($cond) { ")
          ret(thenp)
        close("}")
        open("else {")
          ret(elsep)
        close("}")
      }

    case _ => super.gen(lhs, rhs)
  }
}
