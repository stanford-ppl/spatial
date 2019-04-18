package spatial.codegen.pirgen

import argon._
import argon.node._
import spatial.lang._
import spatial.node._
import utils.escapeString

trait PIRGenText extends PIRCodegen {

  override protected def remap(tp: Type[_]): String = tp match {
    case _:Text => "Text"
    case _ => super.remap(tp)
  }

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case e@GenericToText(x) => genOp(lhs) // emitToString(lhs, x, e.A)
    case TextConcat(parts)  =>  // emit(src"val $lhs = " + parts.map(quote).mkString(" + "))
      state(lhs) {
        src"""OpDef(TextConcat).input(${parts}).tp(${lhs.sym.tp})"""
      }
    case TextEql(x,y)       => genOp(lhs) //emit(src"val $lhs = $x == $y")
    case TextNeq(x,y)       => genOp(lhs) //emit(src"val $lhs = $x != $y")
    case TextLength(x)      => genOp(lhs) //emit(src"val $lhs = FixedPoint.fromInt($x.length)")
    case TextApply(x,i)     => genOp(lhs) // emit(src"val $lhs = FixedPoint.fromChar($x.charAt($i))")

    case CharArrayToText(array) => genOp(lhs) //emit(src"""val $lhs = $array.map(_.toChar).mkString("")""")

    case _ => super.genAccel(lhs, rhs)
  }

}
