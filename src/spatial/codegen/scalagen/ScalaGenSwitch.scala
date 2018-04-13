package spatial.codegen.scalagen

import argon._
import spatial.lang._
import spatial.node._
import spatial.util._

trait ScalaGenSwitch extends ScalaGenBits with ScalaGenMemories with ScalaGenSRAM with ScalaGenController {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@Switch(selects,_) =>
      val isBits = op.R.isBits

      emit(src"/** BEGIN SWITCH $lhs **/")
      open(src"val $lhs = {")
        selects.indices.foreach { i =>
          open(src"""${if (i == 0) "if" else "else if"} (${selects(i)}) {""")
            ret(op.cases(i).body)
          close("}")
        }
        if (isBits) emit(src"else { ${invalid(op.R)} }") else emit(src"()")
        emitControlDone(lhs)
      close("}")
      emit(src"/** END SWITCH $lhs **/")

    case SwitchCase(body) => // Controlled by Switch

    case _ => super.gen(lhs, rhs)
  }
}
