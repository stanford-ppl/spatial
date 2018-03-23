package spatial.codegen.cppgen

import argon.codegen.cppgen.CppCodegen
import argon.core._
import spatial.aliases._
import spatial.nodes._

trait CppGenAlteraVideo extends CppCodegen {

  override protected def emitNode(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case AxiMSNew() => emit(src"""// axi_master_slave""")
    case _ => super.emitNode(lhs, rhs)
  }

}
