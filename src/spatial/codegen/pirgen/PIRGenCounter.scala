package spatial.codegen.pirgen

import argon._
import spatial.lang._
import spatial.node._

trait PIRGenCounter extends PIRCodegen {

  override protected def remap(tp: Type[_]): String = tp match {
    case _:Counter[_]   => src"Counterlike"
    case _:CounterChain => src"Array[Counterlike]"
    case _ => super.remap(tp)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case CounterNew(start,end,step,par) => 
      emitc(src"val $lhs = Counter(min=$start, max=$end, step=$step, par=$par)", rhs)
    case CounterChainNew(ctrs)          => 
      emitc(lhs, src"CounterChain(List(${ctrs.map(quote).mkString(",")}))", rhs)
    case ForeverNew()                   => emit(src"val $lhs = Forever()") //TODO
    case _ => super.gen(lhs, rhs)
  }

}
