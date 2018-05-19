package spatial.codegen.scalagen

import argon._
import spatial.lang._
import spatial.node._

trait ScalaGenCounter extends ScalaCodegen {

  override protected def remap(tp: Type[_]): String = tp match {
    case _:Counter[_]   => src"Counterlike"
    case _:CounterChain => src"Array[Counterlike]"
    case _ => super.remap(tp)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case CounterNew(start,end,step,par) => emit(src"val $lhs = Counter($start, $end, $step, $par)")
    case CounterChainNew(ctrs)          => emit(src"val $lhs = Array[Counterlike]($ctrs)")
    case ForeverNew()                   => emit(src"val $lhs = Forever()")
    case _ => super.gen(lhs, rhs)
  }

}
