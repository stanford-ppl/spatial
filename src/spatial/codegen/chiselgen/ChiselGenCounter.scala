package spatial.codegen.chiselgen

import argon._
import spatial.lang._
import spatial.node._
import spatial.metadata.control._

trait ChiselGenCounter extends ChiselGenCommon {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case CounterNew(start,end,step,par) => createCtrObject(lhs, start, end, step, par, false) //createCtr(lhs,start,end,step,par)
    case CounterChainNew(ctrs) => if (lhs.owner.isOuterStreamLoop) createStreamCChainObject(lhs,ctrs) else createCChainObject(lhs,ctrs)
    case ForeverNew() => createCtrObject(lhs, lhs, lhs, lhs, 1, true)
      // createCtrObject(lhs) {
      //   emit(src"// Owner = ${lhs.owner}")
      //   emit(src"val par = 1")
      //   emit(src"val width = 32")
      //   emit(src"override val isForever = true")
      // }

	  case _ => super.gen(lhs, rhs)
  }


}