package argon.codegen.cppgen

import argon.core._
import argon.emul.FixedPoint
import argon.nodes._

trait CppGenFixPt extends CppCodegen {

  override protected def remap(tp: Type[_]): String = tp match {
    case IntType() => "int32_t"
    case LongType() => "int64_t"
    case FixPtType(s,d,f) => 
      val u = if (!s) "u" else ""
      if (f > 0) {"double"} else {
        if (d+f > 64) s"${u}int128_t"
        else if (d+f > 32) s"${u}int64_t"
        else if (d+f > 16) s"${u}int32_t"
        else if (d+f > 8) s"${u}int16_t"
        else if (d+f > 4) s"${u}int8_t"
        else if (d+f > 2) s"${u}int8_t"
        else if (d+f == 2) s"${u}int8_t"
        else "bool"
      }
    case _ => super.remap(tp)
  }

  override protected def quoteConst(c: Const[_]): String = (c.tp, c) match {
    case (IntType(), Const(c: FixedPoint)) => c.toInt.toString
    case (LongType(), Const(c: FixedPoint)) => c.toLong.toString + "L"
    case (FixPtType(s,d,f), Const(c: FixedPoint)) => c.toString
    case _ => super.quoteConst(c)
  }

  override protected def needsFPType(tp: Type[_]): Boolean = tp match {
      case IntType()  => false
      case LongType() => false
      case FixPtType(s,d,f) => if (f == 0) false else true
      case _ => super.needsFPType(tp)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case FixLsh(x,y) => lhs.tp match {
        case FixPtType(s,d,f) if (f > 0) => emit(src"${lhs.tp} $lhs = $x * pow(2.,$y);")
        case _ => emit(src"${lhs.tp} $lhs = $x << $y;")
      }
    case FixRsh(x,y) => lhs.tp match {
        case FixPtType(s,d,f) if (f > 0) => emit(src"${lhs.tp} $lhs = $x / pow(2.,$y);")
        case _ => emit(src"${lhs.tp} $lhs = $x >> $y;")
      }
    case FixURsh(x,y) => emit(src"${lhs.tp} $lhs = $x >>> $y; // Need to do this correctly for cpp")
    case FixInv(x)   => emit(src"${lhs.tp} $lhs = ~$x;")
    case FixNeg(x)   => emit(src"${lhs.tp} $lhs = -$x;")
    case FixAdd(x,y) => emit(src"${lhs.tp} $lhs = $x + $y;")
    case FixSub(x,y) => emit(src"${lhs.tp} $lhs = $x - $y;")
    case FixMul(x,y) => emit(src"${lhs.tp} $lhs = $x * $y;")
    case FixDiv(x,y) => emit(src"${lhs.tp} $lhs = $x / $y;")
    case FixAnd(x,y) => emit(src"${lhs.tp} $lhs = $x & $y;")
    case FixOr(x,y)  => emit(src"${lhs.tp} $lhs = $x | $y;")
    case FixXor(x,y)  => emit(src"${lhs.tp} $lhs = $x ^ $y;")
    case FixLt(x,y)  => emit(src"${lhs.tp} $lhs = $x < $y;")
    case FixLeq(x,y) => emit(src"${lhs.tp} $lhs = $x <= $y;")
    case FixNeq(x,y) => emit(src"${lhs.tp} $lhs = $x != $y;")
    case FixEql(x,y) => emit(src"${lhs.tp} $lhs = $x == $y;")
    case FixMod(x,y) => emit(src"${lhs.tp} $lhs = $x % $y;")
    case FixRandom(x) => lhs.tp match {
      case IntType()  => emit(src"${lhs.tp} $lhs = rand() % ${x.getOrElse(100)};")
      case LongType() => emit(src"${lhs.tp} $lhs = rand() % ${x.getOrElse(100)};")
      case _ => emit(src"${lhs.tp} $lhs = rand() % ${x.getOrElse(100)};")
    }
    case FixConvert(x) => lhs.tp match {
      case IntType()  => emit(src"${lhs.tp} $lhs = (${lhs.tp}) $x;")
      case LongType() => emit(src"${lhs.tp} $lhs = (${lhs.tp}) $x;")
      case FixPtType(s,d,f) => emit(src"${lhs.tp} $lhs = (${lhs.tp}) $x;  // should be fixpt ${lhs.tp}")
    }
    case FixPtToFltPt(x) => lhs.tp match {
      case DoubleType() => emit(src"${lhs.tp} $lhs = (double) $x;")
      case FloatType()  => emit(src"${lhs.tp} $lhs = (float) $x;")
      case HalfType()  => emit(src"${lhs.tp} $lhs = half_cast<half>($x);")
    }
    case StringToFixPt(x) => 
      lhs.tp match {
        case IntType()  => emit(src"int32_t $lhs = atoi(${x}.c_str());")
        case LongType() => emit(src"long $lhs = std::stol($x);")
        case FixPtType(s,d,f) => emit(src"float $lhs = std::stof($x);")
      }
      x match {
        case Def(ArrayApply(array, i)) => 
          array match {
            case Def(InputArguments()) => 
              val ii = i match {case c: Const[_] => c match {case Const(c: FixedPoint) => c.toInt; case _ => -1}; case _ => -1}
              if (cliArgs.contains(ii)) cliArgs += (ii -> s"${cliArgs(ii)} / ${lhs.name.getOrElse(s"${lhs.ctx}")}")
              else cliArgs += (ii -> lhs.name.getOrElse(s"${lhs.ctx}"))
            case _ =>
          }
        case _ =>          
      }

    case Char2Int(x) => 
      emit(src"${lhs.tp} $lhs = (${lhs.tp}) ${x}[0];")
    case Int2Char(x) => 
      emit(src"char ${lhs}[2]; // Declared as char but becomes string")
      emit(src"${lhs}[0] = $x;")
      emit(src"${lhs}[1] = '\0';")

    case _ => super.gen(lhs, rhs)
  }
}
