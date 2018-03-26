package spatial.codegen.chiselgen

import argon.core._

trait ChiselGenPrint extends ChiselCodegen {

  override protected def gen(lhs: Sym[_], rhs: Op[_]) = rhs match {
    // case Print(x)   => emit(src"val $lhs = System.out.print($x)")
    // case Println(x) => emit(src"val $lhs = System.out.println($x)")
    case _ => super.gen(lhs, rhs)
  }
}
