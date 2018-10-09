package spatial.codegen.pirgen

import argon._
import argon.node._
import spatial.lang._
import spatial.node._
import utils.escapeString

trait PIRGenText extends PIRCodegen {

  override protected def remap(tp: Type[_]): String = tp match {
    case _:Text => "String"
    case _ => super.remap(tp)
  }

  override protected def quoteConst(tp: Type[_], c: Any): String = (tp, c) match {
    case (_:Text, c: String) => escapeString(c)
    case _ => super.quoteConst(tp,c)
  }

  def emitToString(lhs: Sym[_], x: Sym[_], tp: Type[_]): Unit = tp match {
    case _ => emit(src"val $lhs = $x.toString")
  }

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case e@GenericToText(x) => emitToString(lhs, x, e.A)
    case TextConcat(parts)  => emit(src"val $lhs = " + parts.map(quote).mkString(" + "))
    case TextEql(x,y)       => emit(src"val $lhs = $x == $y")
    case TextNeq(x,y)       => emit(src"val $lhs = $x != $y")
    case TextLength(x)      => emit(src"val $lhs = FixedPoint.fromInt($x.length)")
    case TextApply(x,i)     => emit(src"val $lhs = FixedPoint.fromChar($x.charAt($i))")

    case CharArrayToText(array) => emit(src"""val $lhs = $array.map(_.toChar).mkString("")""")

    //case StringSlice(x,start,end) => emit(src"val $lhs = $x.substring($start,$end);")
    //case StringLength(x) => emit(src"val $lhs = $x.length();")
    case _ => super.genAccel(lhs, rhs)
  }



}
