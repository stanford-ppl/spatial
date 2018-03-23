package spatial.codegen.cppgen

import argon.codegen.FileDependencies
import argon.codegen.cppgen.CppCodegen
import argon.core._
import spatial.aliases._
import spatial.nodes._

trait CppGenCounter extends CppCodegen with FileDependencies {
  // dependencies ::= AlwaysDep("cppgen", "Counter.cpp")

  override protected def name(s: Dyn[_]): String = s match {
    case Def(_:CounterNew)      => s"${s}_ctr"
    case Def(_:CounterChainNew) => s"${s}_ctrchain"
    case _ => super.name(s)
  }

  override protected def remap(tp: Type[_]): String = tp match {
    case CounterType      => src"Counter"
    case CounterChainType => src"Array[Counter]"
    case _ => super.remap(tp)
  }

  override protected def emitNode(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case CounterNew(start,end,step,par) =>
    case CounterChainNew(ctrs) =>
    case Forever() => emit(s"// ${quote(lhs)} = Forever")
    case _ => super.emitNode(lhs, rhs)
  }

}
