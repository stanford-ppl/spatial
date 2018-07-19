package spatial.codegen.cppgen

import argon._
import argon.node._
import spatial.lang._
import spatial.node._

trait CppGenDebug extends CppGenCommon {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case FixToText(x) => emit(src"${lhs.tp} $lhs = std::to_string($x);")
    case FltToText(x) => emit(src"${lhs.tp} $lhs = std::to_string($x);")
    case CharArrayToText(array) => 
        emit(src"""${lhs.tp} $lhs;""")
        open(src"""for (int ${lhs}_i = 0; ${lhs}_i < (*${array}).size(); ${lhs}_i ++){""")
            emit(src"""${lhs} += (*${array})[${lhs}_i];""")
        close("}")
    case TextToFix(x, fmt) => emit(src"${lhs.tp} $lhs = std::stof($x);")
      // lhs.tp match {
      //   case IntType()  => emit(src"int32_t $lhs = atoi(${x}.c_str());")
      //   case LongType() => emit(src"long $lhs = std::stol($x);")
      //   // case FixPtType(s,d,f) => emit(src"float $lhs = std::stof($x);")
      //   case FixPtType(s,d,f) => 
      //     if (f > 0 || f+d <= 8) emit(src"float $lhs = std::stof($x);") 
      //     else {
      //       emit(src"${lhs.tp} $lhs;")
      //       emit(src"std::istringstream iss$lhs($x);")
      //       emit(src"iss$lhs >> $lhs;")
      //     }
      // }

    case TextToFlt(x, fmt) => emit(src"${lhs.tp} $lhs = std::stof($x);")
    case TextLength(x) => emit(src"${lhs.tp} $lhs = ${x}.length();")
    case TextApply(x,el) => emit(src"${lhs.tp} $lhs = $x.at($el);")
    case TextSlice(x,start,end) => emit(src"${lhs.tp} $lhs = $x.substr($start,${end}-${start});")
    case TextToBit(x) => emit(src"""${lhs.tp} $lhs = $x != "false" & $x != "False" | $x != "0";""")
    case TextEql(a,b) => emit(src"""${lhs.tp} $lhs = $a == $b;""")
    case GenericToText(x) => emit(src"""${lhs.tp} $lhs = $x.toString();""")

    case TextConcat(strings) => 
    	val paired = strings.map(quote).reduceRight{(r,c) => "string_plus(" + r + "," + c} + ")" * (strings.length-1)
    	emit(src"${lhs.tp} $lhs = $paired;")
    case PrintIf(cond,x)   => 
    	if (cond.isEmpty) emit(src"""std::cout << $x;""")
    	else emit(src"""if ( ${cond.toList.mkString(" & ")} ) std::cout << $x;""")
    case BitToText(x) => emit(src"""${lhs.tp} $lhs = $x ? string("true") : string("false");""")
    case DelayLine(_, data) => emit(src"""${lhs.tp} $lhs = $data;""")

    case VarNew(init)    => emit(src"${lhs.tp.typeArgs.head} $lhs = ${init.getOrElse(0)};")
    case VarRead(v)      => emit(src"${lhs.tp} $lhs = $v;")
    case VarAssign(v, x) => emit(src"$v = $x;")

    case _ => super.gen(lhs, rhs)
  }


}
