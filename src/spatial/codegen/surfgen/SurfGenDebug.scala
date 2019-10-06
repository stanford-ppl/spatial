package spatial.codegen.surfgen

import argon._
import argon.node._
import spatial.lang._
import spatial.node._

trait SurfGenDebug extends SurfGenCommon {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case FixToText(x, None) => emit(src"$lhs = str($x)")
    case FltToText(x, None) => emit(src"$lhs = str($x)")
    case FixToText(x, Some(format)) => emit(src"$lhs = str($x)")
    case FltToText(x, Some(format)) => emit(src"$lhs = str($x)")
    case CharArrayToText(array) => emit(src"$lhs = str($array)")
    case TextToFix(x, fmt) => emit(src"$lhs = int($x)")

    case TextToFlt(x, fmt) => emit(src"$lhs = float($x)")
    case TextLength(x) => emit(src"$lhs = len(${x})")
    case TextApply(x,el) => emit(src"$lhs = $x[$el]")
    case TextSlice(x,start,end) => emit(src"$lhs = $x[$start:$end]")
    case TextToBit(x) => emit(src"""$lhs = $x == 'true' or $x == 'True' or $x == '1'""")
    case TextEql(a,b) => emit(src"""$lhs = $a == $b;""")
    case GenericToText(x) => emit(src"""$lhs = str($x)""")

    case TextConcat(strings) =>
    	val paired = strings.map(quote).reduceRight{(r,c) => "(" + r + " + " + c} + ")" * (strings.length-1)
    	emit(src"$lhs = $paired;")
    case PrintIf(cond,x)   =>
    	if (cond.isEmpty) emit(src"""print($x)""")
    	else emit(src"""if ( ${cond.toList.mkString(" and ")} ) print($x)""")
    case BitToText(x) => emit(src"""$lhs = str($x)""")
    case DelayLine(_, data) => emit(src"""$lhs = $data;""")

    case VarNew(init)    => emit(src"$lhs = ${init.getOrElse(0)}")
    case VarRead(v)      => emit(src"$lhs = $v")
    case VarAssign(v, x) => emit(src"$v = $x")

    case _ => super.gen(lhs, rhs)
  }


}
