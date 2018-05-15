package spatial.codegen.cppgen

import argon._
import argon.codegen.Codegen
import spatial.lang._
import spatial.node._
import spatial.internal.{spatialConfig => cfg}
import spatial.data._
import spatial.util._


trait CppGenMath extends CppGenCommon {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    // case FixLsh(x,y) => lhs.tp match {
    //     case FixPtType(s,d,f) if (f > 0) => emit(src"${lhs.tp} $lhs = $x * pow(2.,$y);")
    //     case _ => emit(src"${lhs.tp} $lhs = $x << $y;")
    //   }
    // case FixRsh(x,y) => lhs.tp match {
    //     case FixPtType(s,d,f) if (f > 0) => emit(src"${lhs.tp} $lhs = $x / pow(2.,$y);")
    //     case _ => emit(src"${lhs.tp} $lhs = $x >> $y;")
    //   }
    // case FixURsh(x,y) => emit(src"${lhs.tp} $lhs = $x >>> $y; // Need to do this correctly for cpp")
    case FixInv(x)   => emit(src"${lhs.tp} $lhs = (${lhs.tp}) ${toApproxFix(src"~(${toTrueFix(quote(x), x.tp)})", x.tp)};")
    case FixNeg(x)   => emit(src"${lhs.tp} $lhs = -$x;")
    case FixAdd(x,y) => emit(src"${lhs.tp} $lhs = $x + $y;")
    case FixSub(x,y) => emit(src"${lhs.tp} $lhs = $x - $y;")
    case FixMul(x,y) => emit(src"${lhs.tp} $lhs = $x * $y;")
    case FixDiv(x,y) => emit(src"${lhs.tp} $lhs = $x / $y;")
    case FixRecip(y) => emit(src"${lhs.tp} $lhs = 1.0 / $y;")
    case FltAdd(x,y) => emit(src"${lhs.tp} $lhs = $x + $y;")
    case FltSub(x,y) => emit(src"${lhs.tp} $lhs = $x - $y;")
    case FltMul(x,y) => emit(src"${lhs.tp} $lhs = $x * $y;")
    case FltDiv(x,y) => emit(src"${lhs.tp} $lhs = $x / $y;")
    case FixAnd(x,y) => emit(src"${lhs.tp} $lhs = $x & $y;")
    case FixOr(x,y)  => emit(src"${lhs.tp} $lhs = $x | $y;")
    case FixXor(x,y)  => emit(src"${lhs.tp} $lhs = $x ^ $y;")
    case FixLst(x,y)  => emit(src"${lhs.tp} $lhs = $x < $y;")
    case FixLeq(x,y) => emit(src"${lhs.tp} $lhs = $x <= $y;")
    case FixNeq(x,y) => emit(src"${lhs.tp} $lhs = $x != $y;")
    case FixEql(x,y) => emit(src"${lhs.tp} $lhs = $x == $y;")
    case FltLst(x,y)  => emit(src"${lhs.tp} $lhs = $x < $y;")
    case FltLeq(x,y) => emit(src"${lhs.tp} $lhs = $x <= $y;")
    case FltNeq(x,y) => emit(src"${lhs.tp} $lhs = $x != $y;")
    case FltEql(x,y) => emit(src"${lhs.tp} $lhs = $x == $y;")
    case FixMod(x,y) => emit(src"${lhs.tp} $lhs = (${lhs.tp}) ${toApproxFix(src"${toTrueFix(quote(x), x.tp)} % ${toTrueFix(quote(y), y.tp)}", lhs.tp)};")
    case FixRandom(x) => emit(src"${lhs.tp} $lhs = rand() % ${x.getOrElse(100)};")
    case FltRandom(x) => emit(src"${lhs.tp} $lhs = ((float) rand() / (float) RAND_MAX) * (float) ${x.getOrElse(100)};")
    case And(x,y) => emit(src"${lhs.tp} $lhs = $x & $y;")
    case Or(x,y) => emit(src"${lhs.tp} $lhs = $x | $y;")
    case Xor(x,y) => emit(src"${lhs.tp} $lhs = $x ^ $y;")
    case Not(x) => emit(src"${lhs.tp} $lhs = !$x;")
    // case FixConvert(x) => emit(src"${lhs.tp} $lhs = (${lhs.tp}) $x;  // should be fixpt ${lhs.tp}")
    // case FixPtToFltPt(x) => lhs.tp match {
    //   case DoubleType() => emit(src"${lhs.tp} $lhs = (double) $x;")
    //   case FloatType()  => emit(src"${lhs.tp} $lhs = (double) $x;")
    // }
    // case StringToFixPt(x) => 
    //   lhs.tp match {
    //     case IntType()  => emit(src"int32_t $lhs = atoi(${x}.c_str());")
    //     case LongType() => emit(src"long $lhs = std::stol($x);")
    //     case FixPtType(s,d,f) => emit(src"float $lhs = std::stof($x);")
    //   }
    //   x match {
    //     case Def(ArrayApply(array, i)) => 
    //       array match {
    //         case Def(InputArguments()) => 
    //           val ii = i match {case c: Const[_] => c match {case Const(c: FixedPoint) => c.toInt; case _ => -1}; case _ => -1}
    //           if (cliArgs.contains(ii)) cliArgs += (ii -> s"${cliArgs(ii)} / ${lhs.name.getOrElse(s"${lhs.ctx}")}")
    //           else cliArgs += (ii -> lhs.name.getOrElse(s"${lhs.ctx}"))
    //         case _ =>
    //       }
    //     case _ =>          
    //   }

    // case Char2Int(x) => 
    //   emit(src"${lhs.tp} $lhs = (${lhs.tp}) ${x}[0];")
    // case Int2Char(x) => 
    //   emit(src"char ${lhs}[2]; // Declared as char but becomes string")
    //   emit(src"${lhs}[0] = $x;")
    //   emit(src"${lhs}[1] = '\0';")

    case FixAbs(x)  => emit(src"${lhs.tp} $lhs = fabs($x);")

    case FltAbs(x)  => emit(src"${lhs.tp} $lhs = fabs($x);")
    // case FltLog(x)  => x.tp match {
    //   case DoubleType() => emit(src"${lhs.tp} $lhs = log($x);")
    //   case FloatType()  => emit(src"${lhs.tp} $lhs = log($x);")
    // }
    case FltExp(x)  => emit(src"${lhs.tp} $lhs = exp($x);")
    case FltSqrt(x) => emit(src"${lhs.tp} $lhs = sqrt($x);")
    case FixSqrt(x) => emit(src"${lhs.tp} $lhs = sqrt($x);")

    case FltPow(x,exp) => emit(src"${lhs.tp} $lhs = pow($x, $exp);")
    case FltSin(x)     => emit(src"${lhs.tp} $lhs = sin($x);")
    case FltCos(x)     => emit(src"${lhs.tp} $lhs = cos($x);")
    case FltTan(x)     => emit(src"${lhs.tp} $lhs = tan($x);")
    case FltSinh(x)    => emit(src"${lhs.tp} $lhs = sinh($x);")
    case FltCosh(x)    => emit(src"${lhs.tp} $lhs = cosh($x);")
    case FltTanh(x)    => emit(src"${lhs.tp} $lhs = tanh($x);")
    case FltAsin(x)    => emit(src"${lhs.tp} $lhs = asin($x);")
    case FltAcos(x)    => emit(src"${lhs.tp} $lhs = acos($x);")
    case FltAtan(x)    => emit(src"${lhs.tp} $lhs = atan($x);")
    case FixFloor(x)   => emit(src"${lhs.tp} $lhs = floor($x);")
    case FixCeil(x)    => emit(src"${lhs.tp} $lhs = ceil($x);")
    case FixToFix(a, fmt)   => emit(src"${lhs.tp} $lhs = (${lhs.tp}) $a;")
    case FixToFlt(a, fmt)   => emit(src"${lhs.tp} $lhs = (${lhs.tp}) $a;")
    case FltToFix(a, fmt)   => emit(src"${lhs.tp} $lhs = (${lhs.tp}) $a;")

    case Mux(sel, a, b) => 
      emit(src"${lhs.tp} $lhs;")
      emit(src"if ($sel){ $lhs = $a; } else { $lhs = $b; }")

    case FixSLA(x,y) => lhs.tp match {
        case FixPtType(s,d,f) if (f > 0) => emit(src"${lhs.tp} $lhs = $x * pow(2.,$y);")
        case _ => emit(src"${lhs.tp} $lhs = $x << $y;")
      }
    case FixSRA(x,y) => lhs.tp match {
        case FixPtType(s,d,f) if (f > 0) => emit(src"${lhs.tp} $lhs = $x / pow(2.,$y);")
        case _ => emit(src"${lhs.tp} $lhs = $x >> $y;")
      }
    case FixSRU(x,y) => emit(src"${lhs.tp} $lhs = $x >>> $y; // Need to do this correctly for cpp")


      
    case _ => super.gen(lhs, rhs)
  }

}
