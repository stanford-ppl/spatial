package spatial.codegen.chiselgen


import argon._
import argon.codegen.Codegen
import spatial.lang._
import spatial.node._
import spatial.metadata.bounds._
import spatial.metadata.access._
import spatial.metadata.retiming._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.types._
import spatial.util.modeling.scrubNoise
import spatial.util.spatialConfig


trait ChiselGenCounter extends ChiselGenCommon {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case CounterNew(start,end,step,par) => createCtrObject(lhs, start, end, step, par, false) //createCtr(lhs,start,end,step,par)
    case CounterChainNew(ctrs) => if (lhs.owner.isOuterStreamLoop) createStreamCChainObject(lhs,ctrs) else createCChainObject(lhs,ctrs)
    case ForeverNew() => 
      // createCtrObject(lhs) {
      //   emit(src"// Owner = ${lhs.owner}")
      //   emit(src"val par = 1")
      //   emit(src"val width = 32")
      //   emit(src"override val isForever = true")
      // }

	  case _ => super.gen(lhs, rhs)
  }


}