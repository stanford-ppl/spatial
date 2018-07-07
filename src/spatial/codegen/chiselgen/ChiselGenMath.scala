package spatial.codegen.chiselgen

import argon._
import argon.node._
import argon.codegen.Codegen
import spatial.lang._
import spatial.node._
import spatial.metadata.control._

trait ChiselGenMath extends ChiselGenCommon {

  // TODO: Clean this and make it nice
  private def MathDL(lhs: Sym[_], rhs: Op[_], lat: String): Unit = {
    alphaconv_register(src"$lhs")
    emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})")

    if (controllerStack.nonEmpty) {
      val streamOuts = getAllReadyLogic(controllerStack.head.toCtrl).mkString(" && ")
      if (controllerStack.head.hasStreamAncestor & streamOuts.replace(" ","") != "" & lat != "None") {
        rhs match {
          case FixMul(a,b) => emitt(src"${lhs}.r := (${a}.*-*($b, ${lat}, ${streamOuts})).r")
          case UnbMul(a,b) => emitt(src"${lhs}.r := ($a.*-*($b, ${lat}, ${streamOuts}, rounding = Unbiased)).r")
          case SatMul(a,b) => emitt(src"${lhs}.r := ($a.*-*($b, ${lat}, ${streamOuts}, saturating = Saturation)).r")
          case UnbSatMul(a,b) => emitt(src"${lhs}.r := ($a.*-*($b, ${lat}, ${streamOuts}, saturating = Saturation, rounding = Unbiased)).r")
          case FixDiv(a,b) => emitt(src"${lhs}.r := ($a./-/($b, ${lat}, ${streamOuts})).r")
          case UnbDiv(a,b) => emitt(src"${lhs}.r := ($a./-/($b, ${lat}, ${streamOuts}, rounding = Unbiased)).r")
          case SatDiv(a,b) => emitt(src"${lhs}.r := ($a./-/($b, ${lat}, ${streamOuts}, saturating = Saturation)).r")
          case UnbSatDiv(a,b) => emitt(src"${lhs}.r := ($a./-/($b, ${lat}, ${streamOuts}, saturating = Saturation, rounding = Unbiased)).r")
          case FixMod(a,b) => emitt(src"${lhs}.r := ($a.%-%($b, ${lat}, ${streamOuts})).r")
          case FixRecip(a) => emitt(src"${lhs}.r := (${lhs}_one./-/($a, ${lat}, ${streamOuts})).r")
          case FixFMA(x,y,z) => emitt(src"Utils.FixFMA($x,$y,$z,${latencyOptionString("FixFMA", Some(bitWidth(lhs.tp)))}.getOrElse(0.0).toInt, ${streamOuts}).cast($lhs)")
        }
      } else {
        rhs match {
          case FixMul(a,b) => emitt(src"${lhs}.r := (${a}.*-*($b, ${lat}, true.B)).r")
          case UnbMul(a,b) => emitt(src"${lhs}.r := ($a.*-*($b, ${lat}, true.B, rounding = Unbiased)).r")
          case SatMul(a,b) => emitt(src"${lhs}.r := ($a.*-*($b, ${lat}, true.B, saturating = Saturation)).r")
          case UnbSatMul(a,b) => emitt(src"${lhs}.r := ($a.*-*($b, ${lat}, true.B, saturating = Saturation, rounding = Unbiased)).r")
          case FixDiv(a,b) => emitt(src"${lhs}.r := ($a./-/($b, ${lat}, true.B)).r")
          case UnbDiv(a,b) => emitt(src"${lhs}.r := ($a./-/($b, ${lat}, true.B, rounding = Unbiased)).r")
          case SatDiv(a,b) => emitt(src"${lhs}.r := ($a./-/($b, ${lat}, true.B, saturating = Saturation)).r")
          case UnbSatDiv(a,b) => emitt(src"${lhs}.r := ($a./-/($b, ${lat}, true.B, saturating = Saturation, rounding = Unbiased)).r")
          case FixMod(a,b) => emitt(src"${lhs}.r := ($a.%-%($b, ${lat}, true.B)).r")
          case FixRecip(a) => emitt(src"${lhs}.r := (${lhs}_one./-/($a, ${lat}, true.B)).r")
          case FixFMA(x,y,z) => emitt(src"Utils.FixFMA($x,$y,$z,${latencyOptionString("FixFMA", Some(bitWidth(lhs.tp)))}.getOrElse(0.0).toInt, true.B).cast($lhs)")
        }
      }
    } else {
      rhs match {
        case FixMul(a,b) => emitt(src"${lhs}.r := (${a}.*-*($b, ${lat}, true.B)).r")
        case UnbMul(a,b) => emitt(src"${lhs}.r := ($a.*-*($b, ${lat}, true.B, rounding = Unbiased)).r")
        case SatMul(a,b) => emitt(src"${lhs}.r := ($a.*-*($b, ${lat}, true.B, saturating = Saturation)).r")
        case UnbSatMul(a,b) => emitt(src"${lhs}.r := ($a.*-*($b, ${lat}, true.B, saturating = Saturation, rounding = Unbiased)).r")
        case FixDiv(a,b) => emitt(src"${lhs}.r := ($a./-/($b, ${lat}, true.B)).r")
        case UnbDiv(a,b) => emitt(src"${lhs}.r := ($a./-/($b, ${lat}, true.B, rounding = Unbiased)).r")
        case SatDiv(a,b) => emitt(src"${lhs}.r := ($a./-/($b, ${lat}, true.B, saturating = Saturation)).r")
        case UnbSatDiv(a,b) => emitt(src"${lhs}.r := ($a./-/($b, ${lat}, true.B, saturating = Saturation, rounding = Unbiased)).r")
        case FixMod(a,b) => emitt(src"${lhs}.r := ($a.%-%($b, ${lat}, true.B)).r")
        case FixRecip(a) => emitt(src"${lhs}.r := (${lhs}_one./-/($a, ${lat}, true.B)).r")
        case FixFMA(x,y,z) => emitt(src"Utils.FixFMA($x,$y,$z,${latencyOptionString("FixFMA", Some(bitWidth(lhs.tp)))}.getOrElse(0.0).toInt, true.B).cast($lhs)")
      }
    }
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case FixInv(x)   => emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emitt(src"${lhs}.r := (~$x).r")
    case FixNeg(x)   => emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emitt(src"${lhs}.r := (-$x).r")
    case FixAdd(x,y) => emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emitt(src"${lhs}.r := ($x + $y).r")
    case FixSub(x,y) => emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emitt(src"${lhs}.r := ($x - $y).r")
    case FixAnd(x,y)  => emitGlobalWire(src"val $lhs = Wire(${lhs.tp})");emitt(src"$lhs := $x & $y")
    case FixOr(x,y)   => emitGlobalWire(src"val $lhs = Wire(${lhs.tp})");emitt(src"$lhs := $x | $y")
    case FixXor(x,y)  => emitGlobalWire(src"val $lhs = Wire(${lhs.tp})");emitt(src"$lhs := $x ^ $y")
    case FixPow(x,y)  => throw new Exception(s"FixPow($x, $y) should have transformed to either a multiply tree (constant exp) or reduce structure (variable exp)")
    case VecApply(vector, i) => emitGlobalWireMap(src"""$lhs""", src"""Wire(${lhs.tp})"""); emitt(src"$lhs := $vector.apply($i)")

    case FixLst(x,y)  => alphaconv_register(src"$lhs"); emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emitt(src"$lhs := $x < $y")
    case FixLeq(x,y) => alphaconv_register(src"$lhs"); emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emitt(src"$lhs := $x <= $y")
    case FixNeq(x,y) => alphaconv_register(src"$lhs"); emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emitt(src"$lhs := $x =/= $y")
    case FixEql(x,y) => alphaconv_register(src"$lhs"); emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emitt(src"$lhs := $x === $y")
    case FltLst(x,y)  => alphaconv_register(src"$lhs"); emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emitt(src"$lhs := $x < $y")
    case FltLeq(x,y) => alphaconv_register(src"$lhs"); emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emitt(src"$lhs := $x <= $y")
    case FltNeq(x,y) => alphaconv_register(src"$lhs"); emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emitt(src"$lhs := $x =/= $y")
    case FltEql(x,y) => alphaconv_register(src"$lhs"); emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emitt(src"$lhs := $x === $y")
    case UnbMul(x,y) => MathDL(lhs, rhs, latencyOptionString("UnbMul", Some(bitWidth(lhs.tp)))) 
    case UnbDiv(x,y) => MathDL(lhs, rhs, latencyOptionString("UnbDiv", Some(bitWidth(lhs.tp)))) 
    case SatMul(x,y) => MathDL(lhs, rhs, latencyOptionString("SatMul", Some(bitWidth(lhs.tp)))) 
    case SatDiv(x,y) => MathDL(lhs, rhs, latencyOptionString("SatDiv", Some(bitWidth(lhs.tp)))) 
    case UnbSatMul(x,y) => MathDL(lhs, rhs, latencyOptionString("SatMul", Some(bitWidth(lhs.tp)))) 
    case UnbSatDiv(x,y) => MathDL(lhs, rhs, latencyOptionString("SatDiv", Some(bitWidth(lhs.tp)))) 
    case FixMul(x,y) => MathDL(lhs, rhs, latencyOptionString("FixMul", Some(bitWidth(lhs.tp))))
    case FixDiv(x,y) => MathDL(lhs, rhs, latencyOptionString("FixDiv", Some(bitWidth(lhs.tp))))
    case FixRecip(y) => 
      emitGlobalWireMap(src"${lhs}_one", src"Wire(${lhs.tp})")
      emit(src"1.U.cast(${lhs}_one)")
      MathDL(lhs, rhs, latencyOptionString("FixDiv", Some(bitWidth(lhs.tp)))) 
    case FixMod(x,y) => MathDL(lhs, rhs, latencyOptionString("FixMod", Some(bitWidth(lhs.tp)))) 
    case FixFMA(x,y,z) => 
      MathDL(lhs, rhs, latencyOptionString("FixFMA", Some(bitWidth(lhs.tp)))) 
      

    case SatAdd(x,y) => emitt(src"val $lhs = $x <+> $y")
    case SatSub(x,y) => emitt(src"val $lhs = $x <-> $y")
    case FixSLA(x,y) => 
      val shift = DLTrace(y).getOrElse(throw new Exception("Cannot shift by non-constant amount in accel")).replaceAll("\\.FP.*|\\.U.*|\\.S.*|L","")
      emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emitt(src"${lhs}.r := (${x}.r << $shift).r // TODO: cast to proper type (chisel expands bits)")
    case FixSRA(x,y) => 
      val shift = DLTrace(y).getOrElse(throw new Exception("Cannot shift by non-constant amount in accel")).replaceAll("\\.FP.*|\\.U.*|\\.S.*|L","")
      emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emitt(src"${lhs}.r := (${x} >> $shift).r")
    case FixSRU(x,y) => 
      val shift = DLTrace(y).getOrElse(throw new Exception("Cannot shift by non-constant amount in accel")).replaceAll("\\.FP.*|\\.U.*|\\.S.*|L","")
      emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emitt(src"${lhs}.r := (${x} >>> $shift).r")
    case BitRandom(None) => emitt(src"val ${lhs} = Utils.fixrand(${scala.math.random*scala.math.pow(2, bitWidth(lhs.tp))}.toInt, ${bitWidth(lhs.tp)}, ${swap(lhs.parent.s.get, DatapathEn)}) === 1.U")
    case FixRandom(None) => emitGlobalWire(src"val $lhs = Wire(${lhs.tp})");emitt(src"${lhs}.r := Utils.fixrand(${scala.math.random*scala.math.pow(2, bitWidth(lhs.tp))}.toInt, ${bitWidth(lhs.tp)}, ${swap(lhs.parent.s.get, DatapathEn)}).r")
    case FixRandom(x) =>
      val seed = (scala.math.random*1000).toInt
      val size = x match{
        case Some(Const(xx)) => s"$xx"
        case Some(_) => s"$x"
        case None => "4096"
      }
      emitt(s"val ${quote(lhs)}_bitsize = Utils.log2Up(${size}) max 1")
      emitGlobalModule(src"val ${lhs}_rng = Module(new PRNG($seed))")
      emitGlobalModule(src"${lhs}_rng.io.en := true.B")
      emitt(src"val ${lhs} = ${lhs}_rng.io.output(${lhs}_bitsize,0)")
    // case FixUnif() =>
    //   val bits = lhs.tp match {
    //     case FixPtType(s,d,f) => f
    //   }
    //   val seed = (random*1000).toInt
    //   emitGlobalModule(src"val ${lhs}_rng = Module(new PRNG($seed))")
    //   emitGlobalModule(src"${lhs}_rng.io.en := true.B")
    //   emitt(src"val ${lhs} = Wire(new FixedPoint(false, 0, $bits))")
    //   emitt(src"${lhs}.r := ${lhs}_rng.io.output(${bits},0)")
    // case FixConvert(x) => lhs.tp match {
    //   case IntType()  =>
    //     emitGlobalWireMap(src"$lhs", "Wire(new FixedPoint(true, 32, 0))")
    //     emitt(src"${x}.cast($lhs)")
    //   case LongType() =>
    //     // val pad = bitWidth(lhs.tp) - bitWidth(x.tp)
    //     emitGlobalWireMap(src"$lhs","Wire(new FixedPoint(true, 64, 0))")
    //     emitt(src"${x}.cast($lhs)")
    //     // if (pad > 0) {
    //     //   emitt(src"${lhs}.r := chisel3.util.Cat(0.U(${pad}.W), ${x}.r)")
    //     // } else {
    //     //   emitt(src"${lhs}.r := ${x}.r.apply(${bitWidth(lhs.tp)-1}, 0)")
    //     // }
    //   case FixPtType(s,d,f) =>
    //     emitt(src"val $lhs = Wire(new FixedPoint($s, $d, $f))")
    //     emitt(src"${x}.cast($lhs)")
    // }
    // case FixPtToFltPt(x) => 
    //    val FltPtType(m,e) = lhs.tp
    //    emitt(src"val $lhs = $x.toFloat($m,$e)")

    // case StringToFixPt(x) => lhs.tp match {
    //   case IntType()  => emitt(src"val $lhs = $x.toInt")
    //   case LongType() => emitt(src"val $lhs = $x.toLong")
    //   case _ => emitt(src"val $lhs = $x // No rule for this")
    // }

    case FixAbs(x) =>
      emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})")
      emitt(src"${lhs}.r := Mux(${x} < 0.U, -$x, $x).r")

    case FixSqrt(x) =>
      emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})")
      emitt(src"${lhs}.r := sqrt(${x}).r")

    case FltFMA(x,y,z) => 
      alphaconv_register(src"$lhs")
      emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})")
      emitt(src"${lhs}.r := ($x.FltFMA($y, $z).r)")

    case FltAdd(x,y) => 
      alphaconv_register(src"$lhs")
      emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})")
      emitt(src"${lhs}.r := ($x + $y).r")

    case FltSub(x,y) => 
      alphaconv_register(src"$lhs")
      emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})")
      emitt(src"${lhs}.r := ($x - $y).r")

    case FltMul(x,y) => 
      alphaconv_register(src"$lhs")
      emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})")
      emitt(src"${lhs}.r := ($x * $y).r")

    case FltDiv(x,y) => 
      alphaconv_register(src"$lhs")
      emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})")
      emitt(src"${lhs}.r := ($x / $y).r")

    // //case FltAbs(x)  =>
    // //  val FltPtType(g,e) = x.tp
    // //  val (e,g) = x.tp match {case FltPtType(g,e) => (e,g)}
    // //  emitt(src"val $lhs = Mux(${x} < 0.FlP($g,$e), -$x, $x)")
    // case FltAbs(x)  => x.tp match {
    //   case DoubleType() => throw new Exception("DoubleType not supported for FltAbs!")
    //   case HalfType() => emitt(src"val $lhs = Utils.fabs($x)")
    //   case FloatType()  => emitt(src"val $lhs = Utils.fabs($x)")
    // }
    // case FltLog(x)  => x.tp match {
    //   case DoubleType() => throw new Exception("DoubleType not supported for FltLog!")
    //   case HalfType() => emitt(src"val $lhs = Utils.flog($x)")
    //   case FloatType()  => emitt(src"val $lhs = Utils.flog($x)")
    // }
    // case FltExp(x)  => x.tp match {
    //   case DoubleType() => throw new Exception("DoubleType not supported for FltExp!")
    //   case HalfType() => emitt(src"val $lhs = Utils.fexp($x)")
    //   case FloatType()  => emitt(src"val $lhs = Utils.fexp($x)")
    // }
    case FltSqrt(x) => emitt(src"val $lhs = fsqrt($x)")

    // case FltPow(x,y) => if (emitEn) throw new Exception("Pow not implemented in hardware yet!")
    // case FixFloor(x) => emitt(src"val $lhs = Utils.floor($x)")
    // case FixCeil(x) => emitt(src"val $lhs = Utils.ceil($x)")

    // case FltSin(x)  => throw new spatial.TrigInAccelException(lhs)
    // case FltCos(x)  => throw new spatial.TrigInAccelException(lhs)
    // case FltTan(x)  => throw new spatial.TrigInAccelException(lhs)
    // case FltSinh(x) => throw new spatial.TrigInAccelException(lhs)
    // case FltCosh(x) => throw new spatial.TrigInAccelException(lhs)
    // case FltTanh(x) => emitt(src"val $lhs = Utils.tanh($x)")
    // case FltSigmoid(x) => emitt(src"val $lhs = Utils.sigmoid($x)")
    // case FltAsin(x) => throw new spatial.TrigInAccelException(lhs)
    // case FltAcos(x) => throw new spatial.TrigInAccelException(lhs)
    // case FltAtan(x) => throw new spatial.TrigInAccelException(lhs)

    case OneHotMux(sels, opts) => 
      emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})")
      emitt(src"${lhs}.r := Mux1H(List($sels), List(${opts.map{x => src"${x}.r"}}))")

    case Mux(sel, a, b) => 
      emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})")
      emitt(src"${lhs}.r := Mux(($sel), ${a}.r, ${b}.r)")

    // // Assumes < and > are defined on runtime type...
    case FixMin(a, b) => emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emitt(src"${lhs}.r := Mux(($a < $b), $a, $b).r")
    case FixMax(a, b) => emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emitt(src"${lhs}.r := Mux(($a > $b), $a, $b).r")
    case FixToFix(a, fmt) => emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emitt(src"${a}.cast($lhs)")
    case FixToFlt(a, fmt) => 
      val (s,d,f) = a.tp match {case FixPtType(s,d,f) => (s,d,f)}
      val (m,e) = lhs.tp match {case FltPtType(m,e) => (m,e)}
      emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emitt(src"fix2flt(${a}.r, $s,$d,$f, $m,$e)")
    case FltToFix(a, fmt) => 
      val (s,d,f) = lhs.tp match {case FixPtType(s,d,f) => (s,d,f)}
      val (m,e) = a.tp match {case FltPtType(m,e) => (m,e)}
      emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emitt(src"flt2fix(${a}.r, $s,$d,$f, $m,$e)")
    case FltRecip(x) => x.tp match {
      case DoubleType() => throw new Exception("DoubleType not supported for FltRecip") 
      case HalfType() => emitt(src"val $lhs = Utils.frec($x)") 
      case FloatType()  => emitt(src"val $lhs = Utils.frec($x)")
    }
    
    case And(a, b) => emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emitt(src"${lhs} := $a & $b")
    case Not(a) => emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emitt(src"${lhs} := ~$a")
    case Or(a, b) => emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emitt(src"${lhs} := $a | $b")
    case Xor(a, b) => emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emitt(src"${lhs} := $a ^ $b")
    case Xnor(a, b) => emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emitt(src"${lhs} := ~($a ^ $b)")

    case FixFloor(a) => emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emitt(src"${lhs}.r := Cat(${a}.raw_dec, 0.U(${fracBits(a)}.W))")
    case FixCeil(a) => emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emitt(src"${lhs}.r := Mux(${a}.raw_frac === 0.U, ${a}.r, Cat(${a}.raw_dec + 1.U, 0.U(${fracBits(a)}.W)))")
    case DataAsBits(data) => emitGlobalWireMap(src"${lhs}", src"Wire(${lhs.tp})");emitt(src"${lhs}.zipWithIndex.foreach{case (b, i) => b := ${data}(i)}")
    case BitsAsData(data, fmt) => emitGlobalWireMap(src"${lhs}", src"Wire(${lhs.tp})");emitt(src"${lhs}.r := chisel3.util.Cat(${data}.reverse)")
    // case FltInvSqrt(x) => x.tp match {
    //   case DoubleType() => throw new Exception("DoubleType not supported for FltInvSqrt") 
    //   case HalfType() =>  emitt(src"val $lhs = Utils.frsqrt($x)")
    //   case FloatType()  => emitt(src"val $lhs = Utils.frsqrt($x)")
    // }

//    case FltAccum(x) => x.tp match {
//      case DoubleType() => throw new Exception("DoubleType not supported for FltAccum") 
//      case HalfType() =>  emitt(src"val $lhs = Utils.faccum($x)")
//      case FloatType()  => emitt(src"val $lhs = Utils.faccum($x)")
//    }

	case _ => super.gen(lhs, rhs)
  }


}