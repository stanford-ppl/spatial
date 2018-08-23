package spatial.codegen.chiselgen

import argon._
import spatial.node._


trait ChiselGenCounter extends ChiselGenCommon {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case CounterNew(start,end,step,par) => 
      emit(s"// $lhs = ($start to $end by $step par $par")
    case CounterChainNew(ctrs) => 
      emit(s"// $lhs = cchain of $ctrs")
      // val user = usersOf(lhs).head._1
      // if (styleOf(user) != StreamPipe) emitCounterChain(lhs)
    case ForeverNew() => 
      emit("// $lhs = Forever")

	  case _ => super.gen(lhs, rhs)
  }


}