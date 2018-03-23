package spatial.codegen.cppgen

import argon.core._
import spatial.aliases._
import spatial.nodes._


trait CppGenUnrolled extends CppGenController {

  private def emitUnrolledLoop(
    cchain: Exp[CounterChain],
    iters:  Seq[Seq[Bound[Index]]],
    valids: Seq[Seq[Bound[Bit]]]
  )(func: => Unit): Unit = {

    for (i <- iters.indices) {
      open(src"$cchain($i).foreach{case (is,vs) => ")
      iters(i).zipWithIndex.foreach{case (iter,j) => emit(src"val $iter = is($j)") }
      valids(i).zipWithIndex.foreach{case (valid,j) => emit(src"val $valid = vs($j)") }
    }
    func
    iters.indices.foreach{_ => close("}") }
  }

  override protected def name(s: Dyn[_]): String = s match {
    case Def(_: UnrolledForeach) => s"${s}_unrForeach"
    case Def(_: UnrolledReduce[_,_]) => s"${s}_unrRed"
    case Def(_: ParSRAMLoad[_]) => s"${s}_parLd"
    case Def(_: ParSRAMStore[_]) => s"${s}_parSt"
    case Def(_: ParFIFODeq[_]) => s"${s}_parDeq"
    case Def(_: ParFIFOEnq[_]) => s"${s}_parEnq"
    case _ => super.name(s)
  } 

  private def flattenAddress(dims: Seq[Exp[Index]], indices: Seq[Exp[Index]]): String = {
    val strides = List.tabulate(dims.length){i => (dims.drop(i+1).map(quote) :+ "1").mkString("*") }
    indices.zip(strides).map{case (i,s) => src"$i*$s"}.mkString(" + ")
  }

  override protected def emitNode(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case UnrolledForeach(en, cchain,func,iters,valids) =>
      controllerStack.push(lhs)
      instrumentCounters = instrumentCounters :+ (lhs, controllerStack.length)
      emitUnrolledLoop(cchain, iters, valids){ emitBlock(func) }
      controllerStack.pop()      

    case UnrolledReduce(en, cchain,_,func,iters,valids) =>
      controllerStack.push(lhs)
      instrumentCounters = instrumentCounters :+ (lhs, controllerStack.length)
      emitBlock(func)
      controllerStack.pop()

    case ParSRAMLoad(sram,inds,ens) =>

    case ParSRAMStore(sram,inds,data,ens) =>

    case ParFIFODeq(fifo, ens) =>

    case ParFIFOEnq(fifo, data, ens) =>

    case _ => super.emitNode(lhs, rhs)
  }
}
