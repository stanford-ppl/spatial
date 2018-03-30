package spatial.codegen.cppgen

import argon._
import argon.codegen.Codegen
import spatial.lang._
import spatial.node._
import spatial.internal.{spatialConfig => cfg}


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
    case FixInv(x)   => emit(src"${lhs.tp} $lhs = ~$x;")
    case FixNeg(x)   => emit(src"${lhs.tp} $lhs = -$x;")
    case FixAdd(x,y) => emit(src"${lhs.tp} $lhs = $x + $y;")
    case FixSub(x,y) => emit(src"${lhs.tp} $lhs = $x - $y;")
    case FixMul(x,y) => emit(src"${lhs.tp} $lhs = $x * $y;")
    case FixDiv(x,y) => emit(src"${lhs.tp} $lhs = $x / $y;")
    case FixAnd(x,y) => emit(src"${lhs.tp} $lhs = $x & $y;")
    case FixOr(x,y)  => emit(src"${lhs.tp} $lhs = $x | $y;")
    case FixXor(x,y)  => emit(src"${lhs.tp} $lhs = $x ^ $y;")
    // case FixLt(x,y)  => emit(src"${lhs.tp} $lhs = $x < $y;")
    case FixLeq(x,y) => emit(src"${lhs.tp} $lhs = $x <= $y;")
    case FixNeq(x,y) => emit(src"${lhs.tp} $lhs = $x != $y;")
    case FixEql(x,y) => emit(src"${lhs.tp} $lhs = $x == $y;")
    case FixMod(x,y) => emit(src"${lhs.tp} $lhs = $x % $y;")
    case FixRandom(x) => emit(src"${lhs.tp} $lhs = rand() % ${x.getOrElse(100)};")
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
    case FltExp(x)  => x.tp match {
      case DoubleType() => emit(src"${lhs.tp} $lhs = exp($x);")
      case FloatType()  => emit(src"${lhs.tp} $lhs = exp($x);")
    }
    case FltSqrt(x) => x.tp match {
      case DoubleType() => emit(src"${lhs.tp} $lhs = sqrt($x);")
      case FloatType()  => emit(src"${lhs.tp} $lhs = sqrt($x);")
    }

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


    case Mux(sel, a, b) => 
      emit(src"${lhs.tp} $lhs;")
      emit(src"if ($sel){ $lhs = $a; } else { $lhs = $b; }")

    // // Assumes < and > are defined on runtime type...
    // case Min(a, b) => emit(src"${lhs.tp} $lhs = std::min($a,$b);")
    // case Max(a, b) => emit(src"${lhs.tp} $lhs = std::max($a,$b);")


      
    case _ => super.gen(lhs, rhs)
  }

}
