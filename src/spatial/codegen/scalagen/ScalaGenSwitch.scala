package spatial.codegen.scalagen

import argon._
import spatial.lang._
import spatial.node._
import spatial.metadata.types._

trait ScalaGenSwitch extends ScalaGenBits with ScalaGenMemories with ScalaGenSRAM with ScalaGenController {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@Switch(selects,_) =>
      emit(src"/** BEGIN SWITCH $lhs **/")
      open(src"val $lhs = {")
        selects.indices.foreach { i =>
          open(src"""${if (i == 0) "if" else "else if"} (${selects(i)}) {""")
            ret(op.cases(i).body)
          close("}")
        }
        if (op.R.isBits)      emit(src"else { ${invalid(op.R)} }")
        else if (op.R.isVoid) emit(src"else ()")
        else                  emit(src"else { null.asInstanceOf[${op.R}]")

        emitControlDone(lhs)
      close("}")
      emit(src"/** END SWITCH $lhs **/")

    case SwitchCase(body) => // Controlled by Switch

    case _ => super.gen(lhs, rhs)
  }

}
