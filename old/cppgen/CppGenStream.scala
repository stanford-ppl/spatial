package spatial.codegen.cppgen

import argon.codegen.cppgen.CppCodegen
import argon.core._
import spatial.aliases._
import spatial.nodes._

trait CppGenStream extends CppCodegen {

  override protected def emitNode(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
//    case StreamInNew(bus) => emit(s"$lhs = $bus // TODO: No idea what to connect this bus to, should expose periphal pins to something...")
//    case StreamOutNew(bus) =>
//      s"$bus" match {
//        case "BurstCmdBus" => 
//        case _ =>
//          emit(src"// New stream out $lhs")
//      }
    case BufferedOutNew(_, bus) => emit(s"// ${quote(lhs)} = $bus // TODO: No idea what to connect this bus to, should expose periphal pins to something...")
    case StreamInNew(bus) => emit(s"// ${quote(lhs)} = $bus // TODO: No idea what to connect this bus to, should expose periphal pins to something...")
    case StreamOutNew(bus) => emit(s"// ${quote(lhs)} = $bus // TODO: No idea what to connect this bus to, should expose periphal pins to something...")
    case _ => super.emitNode(lhs, rhs)
  }

  

  override protected def emitFileFooter() {
    super.emitFileFooter()
  }
}
