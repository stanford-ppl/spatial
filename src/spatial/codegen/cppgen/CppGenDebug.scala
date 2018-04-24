package spatial.codegen.cppgen

import argon._
import argon.codegen.Codegen
import spatial.lang._
import spatial.node._


trait CppGenDebug extends CppGenCommon {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case FixToText(x) => emit(src"${lhs.tp} $lhs = std::to_string($x);")
    case TextToFix(x, fmt) => emit(src"${lhs.tp} $lhs = std::stof($x);")

    case TextConcat(strings) => 
    	val paired = strings.map(quote).reduceRight{(r,c) => "string_plus(" + r + "," + c} + ")" * (strings.length-1)
    	emit(src"${lhs.tp} $lhs = $paired;")
    case PrintIf(cond,x)   => 
    	if (cond.isEmpty) emit(src"""std::cout << $x;""")
    	else emit(src"""if ( ${cond.toList.mkString(" & ")} ) std::cout << $x;""")
    case BitToText(x) => emit(src"""${lhs.tp} $lhs = $x ? string("true") : string("false");""")
    case DelayLine(_, data) => emit(src"""${lhs.tp} $lhs = $data;""")
    case _ => super.gen(lhs, rhs)
  }


}
