package spatial.codegen.pirgen

import argon._
import spatial.lang._
import spatial.node._

trait PIRGenCounter extends PIRCodegen {

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case CounterNew(start,end,step,par) => 
      state(lhs)(src"Counter(start=$start, end=$end, step=$step, par=$par)")
    case CounterChainNew(ctrs)          => 
      state(lhs, tp=Some("List[Counter]"))(src"$ctrs")
    case ForeverNew()                   => 
      state(lhs)(src"ForeverNew")
    case _ => super.genAccel(lhs, rhs)
  }

}
