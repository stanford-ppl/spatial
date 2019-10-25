package spatial.codegen.roguegen

import argon._
import argon.node._
import spatial.lang._
import spatial.node._

trait RogueGenMath extends RogueGenCommon {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case FixInv(x)   => emit(src"$lhs = ~$x")
    case FixNeg(x)   => emit(src"$lhs = -$x;")
    case FixPow(x,y) => emit(src"$lhs = pow($x,$y);")
    case FltNeg(x)   => emit(src"$lhs = -$x;")
    case FltCeil(x)  => emit(src"$lhs = math.ceil($x);")
    case FltFloor(x)  => emit(src"$lhs = math.floor($x);")
    case FixAdd(x,y) => emit(src"$lhs = $x + $y;")
    case FixSub(x,y) => emit(src"$lhs = $x - $y;")
    case FixMul(x,y) => emit(src"$lhs = $x * $y;")
    case FixDiv(x,y) => emit(src"$lhs = $x / $y;")
    case FixMin(x,y) => emit(src"$lhs = $x if ($x < $y) else $y")
    case FixMax(x,y) => emit(src"$lhs = $x if ($x > $y) else $y")
    case FixRecip(y) => emit(src"$lhs = 1.0 / $y;")
    case FixRecipSqrt(y) => emit(src"$lhs = 1.0 / math.sqrt($y);")
    case FixFMA(a,b,c) => emit(src"$lhs = $a * $b + $c;")
    case FltIsNaN(a) => emit(src"$lhs = math.isnan($a);")
    case FltFMA(a,b,c) => emit(src"$lhs = $a * $b + $c;")
    case FltRecip(y) => emit(src"$lhs = 1.0 / $y;")
    case FltAdd(x,y) => emit(src"$lhs = $x + $y;")
    case FltSub(x,y) => emit(src"$lhs = $x - $y;")
    case FltMul(x,y) => emit(src"$lhs = $x * $y;")
    case FltDiv(x,y) => emit(src"$lhs = $x / $y;")
    case FixAnd(x,y) => emit(src"$lhs = $x & $y;")
    case FixOr(x,y)  => emit(src"$lhs = $x | $y;")
    case FixXor(x,y)  => emit(src"$lhs = $x ^ $y;")
    case FixLst(x,y)  => emit(src"$lhs = $x < $y;")
    case FixLeq(x,y) => emit(src"$lhs = $x <= $y;")
    case FixNeq(x,y) => emit(src"$lhs = $x != $y;")
    case FltMax(x,y) => emit(src"$lhs = $x if ($x > $y) else $y")
    case FltMin(x,y) => emit(src"$lhs = $x if ($x < $y) else $y")
    case FixEql(x,y) => emit(src"$lhs = $x == $y;")
    case FltLst(x,y)  => emit(src"$lhs = $x < $y;")
    case FltLeq(x,y) => emit(src"$lhs = $x <= $y;")
    case FltNeq(x,y) => emit(src"$lhs = $x != $y;")
    case FltEql(x,y) => emit(src"$lhs = $x == $y;")
    case FixMod(x,y) => emit(src"$lhs = $x % $y")
    case FixRandom(x) => emit(src"$lhs = random.randint(0,${x.getOrElse(100)})")
    case FltRandom(x) => emit(src"$lhs = random.random() * ${x.getOrElse(100)}")
    case BitRandom(x) => emit(src"$lhs = random.randint(0,1) == 0;")
    case And(x,y) => emit(src"$lhs = $x & $y;")
    case Or(x,y) => emit(src"$lhs = $x | $y;")
    case Xor(x,y) => emit(src"$lhs = $x ^ $y;")
    case Xnor(x,y) => emit(src"$lhs = ~($x ^ $y);")
    case Not(x) => emit(src"$lhs = ~$x;")

    case FixAbs(x)  => emit(src"$lhs = abs($x);")

    case FltAbs(x)  => emit(src"$lhs = abs($x);")
    // case FltLog(x)  => x.tp match {
    //   case DoubleType() => emit(src"$lhs = log($x);")
    //   case FloatType()  => emit(src"$lhs = log($x);")
    // }
    case FltExp(x)  => emit(src"$lhs = math.exp($x);")
    case FltSqrt(x) => emit(src"$lhs = math.sqrt($x);")
    case FixSqrt(x) => emit(src"$lhs = math.sqrt($x);")
    case FltSigmoid(x)  => emit(src"$lhs = 1.0 / (math.exp(-$x) + 1);")

    case FltPow(x,exp) => emit(src"$lhs = math.pow($x, $exp);")
    case FltSin(x)     => emit(src"$lhs = math.sin($x);")
    case FltCos(x)     => emit(src"$lhs = math.cos($x);")
    case FltTan(x)     => emit(src"$lhs = math.tan($x);")
    case FltSinh(x)    => emit(src"$lhs = math.sinh($x);")
    case FltCosh(x)    => emit(src"$lhs = math.cosh($x);")
    case FltTanh(x)    => emit(src"$lhs = math.tanh($x);")
    case FltAsin(x)    => emit(src"$lhs = math.asin($x);")
    case FltAcos(x)    => emit(src"$lhs = math.acos($x);")
    case FltAtan(x)    => emit(src"$lhs = math.atan($x);")
    case FixFloor(x)   => emit(src"$lhs = math.floor($x);")
    case FixCeil(x)    => emit(src"$lhs = math.ceil($x);")
    case FixToFix(a, fmt)   => emit(src"$lhs = $a;")
    case FixToFixSat(a, fmt)   =>
        val max = lhs.tp match {case x:Fix[_,_,_] => x.maxValue; case _ => throw new Exception("Error in Saturating Cast")}
        val min = lhs.tp match {case x:Fix[_,_,_] => x.minValue; case _ => throw new Exception("Error in Saturating Cast")}
        emit(src"if: ($a > ${max}) $lhs = ${max};")
        emit(src"elif: ($a < ${min}) $lhs = ${min};")
        emit(src"else: $lhs = $a;")
    case FixToFixUnb(a, fmt)   => emit(src"$lhs = $a;")
    case FixToFixUnbSat(a, fmt)   =>
        val max = lhs.tp match {case x:Fix[_,_,_] => x.maxValue; case _ => throw new Exception("Error in Saturating Cast")}
        val min = lhs.tp match {case x:Fix[_,_,_] => x.minValue; case _ => throw new Exception("Error in Saturating Cast")}
        emit(src"${lhs.tp} $lhs;")
        emit(src"if: ($a > ${max}) $lhs = ${max};")
        emit(src"elif: ($a < ${min}) $lhs = ${min};")
        emit(src"else: $lhs = $a;")
    case FixToFlt(a, fmt)   => emit(src"$lhs = $a;")
    case FltToFix(a, fmt)   => emit(src"$lhs = $a;")
    case FltToFlt(a, fmt)   => emit(src"$lhs = $a;")

    case Mux(sel, a, b) =>
      emit(src"$lhs = $a if ($sel) else $b")

    case FixSLA(x,y) => lhs.tp match {
        case FixPtType(s,d,f) if (f > 0) => emit(src"$lhs = $x * pow(2.,$y);")
        case _ => emit(src"$lhs = $x << $y;")
      }
    case FixSRA(x,y) => lhs.tp match {
        case FixPtType(s,d,f) if (f > 0) => emit(src"$lhs = $x / pow(2.,$y);")
        case _ => emit(src"$lhs = $x >> $y;")
      }
    case FixSRU(x,y) => emit(src"$lhs = $x >>> $y; // Need to do this correctly for cpp")

    case _ => super.gen(lhs, rhs)
  }

}
