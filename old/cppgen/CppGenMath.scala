package spatial.codegen.cppgen

import argon.nodes._
import argon.codegen.cppgen.CppCodegen
import argon.core._
import spatial.aliases._
import spatial.metadata._
import spatial.nodes._


trait CppGenMath extends CppCodegen {

  override protected def name(s: Dyn[_]): String = s match {
    case Def(FixRandom(x))              => s"${s}_fixrnd"
    case Def(FixNeg(x:Exp[_]))          => s"${s}_neg${quoteOperand(x)}"
    case Def(FixAdd(x:Exp[_],y:Exp[_])) => s"${s}_sum${quoteOperand(x)}_${quoteOperand(y)}"
    case _ => super.name(s)
  } 

  def quoteOperand(s: Exp[_]): String = s match {
    case ss: Sym[_] => s"x${ss.id}"
    case Const(xx: Exp[_]) => s"${boundOf(xx).toInt}"  // FIXME: This should never match
    case _ => "unk"
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case FixFMA(x, y, z)  => emit(src"${lhs.tp} $lhs = ($x * $y) + $z; // FixFMA")
    case FltFMA(x, y, z)  => emit(src"${lhs.tp} $lhs = ($x * $y) + $z; // FltFMA")
    case FixAbs(x)  => emit(src"${lhs.tp} $lhs = fabs($x);")

    case FltAbs(x)  => emit(src"${lhs.tp} $lhs = fabs($x);")
    case FltLog(x)  => x.tp match {
      case DoubleType() => emit(src"${lhs.tp} $lhs = log($x);")
      case FloatType()  => emit(src"${lhs.tp} $lhs = log($x);")
      case HalfType()  => emit(src"${lhs.tp} $lhs = log($x);")
    }
    case FltExp(x)  => x.tp match {
      case DoubleType() => emit(src"${lhs.tp} $lhs = exp($x);")
      case FloatType()  => emit(src"${lhs.tp} $lhs = exp($x);")
      case HalfType()  => emit(src"${lhs.tp} $lhs = exp($x);")
    }
    case FltSqrt(x) => x.tp match {
      case DoubleType() => emit(src"${lhs.tp} $lhs = sqrt($x);")
      case FloatType()  => emit(src"${lhs.tp} $lhs = sqrt($x);")
      case HalfType()  => emit(src"${lhs.tp} $lhs = sqrt($x);")
    }
    case FltRecip(x) => x.tp match {
      case DoubleType() => emit(src"${lhs.tp} $lhs = 1.0/$x;")
      case FloatType()  => emit(src"${lhs.tp} $lhs = 1.0/$x;")
      case HalfType()  => emit(src"${lhs.tp} $lhs = half_cast<half>(1.0/$x);")
    }
    
    case FltInvSqrt(x) => x.tp match {
      case DoubleType() => emit(src"${lhs.tp} $lhs = 1.0/sqrt($x);")
      case FloatType()  => emit(src"${lhs.tp} $lhs = 1.0/sqrt($x);")
      case HalfType()  => emit(src"${lhs.tp} $lhs = half_cast<half>(1.0/sqrt($x));")
    }

    case FltSigmoid(x)  => x.tp match {
      case DoubleType()  => emit(src"${lhs.tp} $lhs = 1.0 / (1 + exp(-$x));")
      case FloatType()  => emit(src"${lhs.tp} $lhs = 1.0 / (1 + exp(-$x));")
      case HalfType()  => emit(src"${lhs.tp} $lhs =  half_cast<half>(1.0 / (1 + exp(-$x)));")
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

    // Assumes < and > are defined on runtime type...
    case Min(a, b) => emit(src"${lhs.tp} $lhs = std::min($a,$b);")
    case Max(a, b) => emit(src"${lhs.tp} $lhs = std::max($a,$b);")

    case _ => super.gen(lhs, rhs)
  }

}
