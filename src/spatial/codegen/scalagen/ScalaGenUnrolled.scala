package spatial.codegen.scalagen

import argon._
import spatial.lang._
import spatial.node._
import spatial.metadata.control._

trait ScalaGenUnrolled extends ScalaGenMemories with ScalaGenSRAM with ScalaGenController {

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
        iters(i).zipWithIndex.foreach { case (iter, j) => emit(src"val $iter = FixedPoint(1)") }
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

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case UnrolledForeach(ens,cchain,func,iters,valids) =>
      emit(src"/** BEGIN UNROLLED FOREACH $lhs **/")
      val en = if (ens.isEmpty) "true" else ens.map(quote).mkString(" && ")
      open(src"val $lhs = if ($en) {")
        emitUnrolledLoop(lhs, cchain, iters, valids){
          emitControlBlock(lhs, func)
        }
        emitControlDone(lhs)
      close("}")
      emit(src"/** END UNROLLED FOREACH $lhs **/")

    case UnrolledReduce(ens,cchain,func,iters,valids) =>
      emit(src"/** BEGIN UNROLLED REDUCE $lhs **/")
      val en = if (ens.isEmpty) "true" else ens.map(quote).mkString(" && ")
      open(src"val $lhs = if ($en) {")
        emitUnrolledLoop(lhs, cchain, iters, valids){
          emitControlBlock(lhs, func)
        }
        emitControlDone(lhs)
      close("}")
      emit(src"/** END UNROLLED REDUCE $lhs **/")

    case _ => super.gen(lhs, rhs)
  }
}
