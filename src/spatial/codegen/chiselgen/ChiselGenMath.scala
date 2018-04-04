package spatial.codegen.chiselgen

import argon._
import argon.codegen.Codegen
import spatial.lang._
import spatial.node._
import spatial.internal.{spatialConfig => cfg}


trait ChiselGenMath extends ChiselGenCommon {


  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case FixInv(x)   => emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emit(src"${lhs}.r := (~$x).r")
    case FixNeg(x)   => emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emit(src"${lhs}.r := (-$x).r")
    case FixAdd(x,y) => emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emit(src"${lhs}.r := ($x + $y).r")
    case FixSub(x,y) => emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emit(src"${lhs}.r := ($x - $y).r")
    case FixAnd(x,y)  => emitGlobalWire(src"val $lhs = Wire(${lhs.tp})");emit(src"$lhs := $x & $y")
    case FixOr(x,y)   => emitGlobalWire(src"val $lhs = Wire(${lhs.tp})");emit(src"$lhs := $x | $y")
    case FixXor(x,y)  => emitGlobalWire(src"val $lhs = Wire(${lhs.tp})");emit(src"$lhs := $x ^ $y")
    // case FixLt(x,y)  => alphaconv_register(src"$lhs"); emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emit(src"$lhs := $x < $y")
    // case FixLeq(x,y) => alphaconv_register(src"$lhs"); emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emit(src"$lhs := $x <= $y")
    // case FixNeq(x,y) => alphaconv_register(src"$lhs"); emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emit(src"$lhs := $x =/= $y")
    // case FixEql(x,y) => alphaconv_register(src"$lhs"); emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emit(src"$lhs := $x === $y")
    // case UnbMul(x,y) => emit(src"val $lhs = $x *& $y")
    // case UnbDiv(x,y) => emit(src"val $lhs = $x /& $y")
    // case SatAdd(x,y) => emit(src"val $lhs = $x <+> $y")
    // case SatSub(x,y) => emit(src"val $lhs = $x <-> $y")
    // case SatMul(x,y) => emit(src"val $lhs = $x <*> $y")
    // case SatDiv(x,y) => emit(src"val $lhs = $x </> $y")
    // case FixLsh(x,Const(yy))  => emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emit(src"${lhs}.r := ($x << $yy).r // TODO: cast to proper type (chisel expands bits)")
    // case FixRsh(x,Const(yy))  => emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emit(src"${lhs}.r := ($x >> $yy).r")
    // case FixURsh(x,Const(yy)) => emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emit(src"${lhs}.r := ($x >>> $yy).r")
    // case UnbSatMul(x,y) => emit(src"val $lhs = $x <*&> $y")
    // case UnbSatDiv(x,y) => emit(src"val $lhs = $x </&> $y")
    // case FixRandom(x) =>
    //   val seed = (random*1000).toInt
    //   val size = x match{
    //     case Some(Const(xx)) => s"$xx"
    //     case Some(_) => s"$x"
    //     case None => "4096"
    //   }
    //   emit(s"val ${quote(lhs)}_bitsize = Utils.log2Up(${size}) max 1")
    //   emitGlobalModule(src"val ${lhs}_rng = Module(new PRNG($seed))")
    //   emitGlobalModule(src"${lhs}_rng.io.en := true.B")
    //   emit(src"val ${lhs} = ${lhs}_rng.io.output(${lhs}_bitsize,0)")
    // case FixUnif() =>
    //   val bits = lhs.tp match {
    //     case FixPtType(s,d,f) => f
    //   }
    //   val seed = (random*1000).toInt
    //   emitGlobalModule(src"val ${lhs}_rng = Module(new PRNG($seed))")
    //   emitGlobalModule(src"${lhs}_rng.io.en := true.B")
    //   emit(src"val ${lhs} = Wire(new FixedPoint(false, 0, $bits))")
    //   emit(src"${lhs}.r := ${lhs}_rng.io.output(${bits},0)")
    // case FixConvert(x) => lhs.tp match {
    //   case IntType()  =>
    //     emitGlobalWireMap(src"$lhs", "Wire(new FixedPoint(true, 32, 0))")
    //     emit(src"${x}.cast($lhs)")
    //   case LongType() =>
    //     // val pad = bitWidth(lhs.tp) - bitWidth(x.tp)
    //     emitGlobalWireMap(src"$lhs","Wire(new FixedPoint(true, 64, 0))")
    //     emit(src"${x}.cast($lhs)")
    //     // if (pad > 0) {
    //     //   emit(src"${lhs}.r := chisel3.util.Cat(0.U(${pad}.W), ${x}.r)")
    //     // } else {
    //     //   emit(src"${lhs}.r := ${x}.r.apply(${bitWidth(lhs.tp)-1}, 0)")
    //     // }
    //   case FixPtType(s,d,f) =>
    //     emit(src"val $lhs = Wire(new FixedPoint($s, $d, $f))")
    //     emit(src"${x}.cast($lhs)")
    // }
    // case FixPtToFltPt(x) => 
    //    val FltPtType(m,e) = lhs.tp
    //    emit(src"val $lhs = $x.toFloat($m,$e)")

    // case StringToFixPt(x) => lhs.tp match {
    //   case IntType()  => emit(src"val $lhs = $x.toInt")
    //   case LongType() => emit(src"val $lhs = $x.toLong")
    //   case _ => emit(src"val $lhs = $x // No rule for this")
    // }

     case FixMul(x,y) =>
      alphaconv_register(src"$lhs")
      emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})")
      emit(src"${lhs}.r := ($x.*-*($y, ${latencyOptionString("FixMul", Some(bitWidth(lhs.tp)))}).r)")

    case FixDiv(x,y) =>
      emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})")
      emit(src"${lhs}.r := ($x./-/($y, ${latencyOptionString("FixDiv", Some(bitWidth(lhs.tp)))}).r)")

    case FixMod(x,y) =>
      emitGlobalWireMap(src"$lhs",src"Wire(${lhs.tp})")
      emit(src"$lhs := $x.%-%($y, ${latencyOptionString("FixMod", Some(bitWidth(lhs.tp)))})")

    case FixAbs(x) =>
      emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})")
      emit(src"${lhs}.r := Mux(${x} < 0.U, -$x, $x).r")

    case FixFMA(x,y,z) => 
      alphaconv_register(src"$lhs")
      emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})")
      emit(src"${lhs}.r := ($x.*-*($y, ${latencyOptionString("FixMul", Some(bitWidth(lhs.tp)))})+$z).r")

    case FltFMA(x,y,z) => 
      alphaconv_register(src"$lhs")
      emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})")
      emit(src"${lhs}.r := ($x.FltFMA($y, $z).r)")

    // //case FltAbs(x)  =>
    // //  val FltPtType(g,e) = x.tp
    // //  val (e,g) = x.tp match {case FltPtType(g,e) => (e,g)}
    // //  emit(src"val $lhs = Mux(${x} < 0.FlP($g,$e), -$x, $x)")
    // case FltAbs(x)  => x.tp match {
    //   case DoubleType() => throw new Exception("DoubleType not supported for FltAbs!")
    //   case HalfType() => emit(src"val $lhs = Utils.fabs($x)")
    //   case FloatType()  => emit(src"val $lhs = Utils.fabs($x)")
    // }
    // case FltLog(x)  => x.tp match {
    //   case DoubleType() => throw new Exception("DoubleType not supported for FltLog!")
    //   case HalfType() => emit(src"val $lhs = Utils.flog($x)")
    //   case FloatType()  => emit(src"val $lhs = Utils.flog($x)")
    // }
    // case FltExp(x)  => x.tp match {
    //   case DoubleType() => throw new Exception("DoubleType not supported for FltExp!")
    //   case HalfType() => emit(src"val $lhs = Utils.fexp($x)")
    //   case FloatType()  => emit(src"val $lhs = Utils.fexp($x)")
    // }
    // case FltSqrt(x) => x.tp match {
    //   case DoubleType() => throw new Exception("DoubleType not supported for FltSqrt!")
    //   case HalfType() => emit(src"val $lhs = Utils.fsqrt($x)")
    //   case FloatType()  => emit(src"val $lhs = Utils.fsqrt($x)")
    // }

    // case FltPow(x,y) => if (emitEn) throw new Exception("Pow not implemented in hardware yet!")
    // case FixFloor(x) => emit(src"val $lhs = Utils.floor($x)")
    // case FixCeil(x) => emit(src"val $lhs = Utils.ceil($x)")

    // case FltSin(x)  => throw new spatial.TrigInAccelException(lhs)
    // case FltCos(x)  => throw new spatial.TrigInAccelException(lhs)
    // case FltTan(x)  => throw new spatial.TrigInAccelException(lhs)
    // case FltSinh(x) => throw new spatial.TrigInAccelException(lhs)
    // case FltCosh(x) => throw new spatial.TrigInAccelException(lhs)
    // case FltTanh(x) => emit(src"val $lhs = Utils.tanh($x)")
    // case FltSigmoid(x) => emit(src"val $lhs = Utils.sigmoid($x)")
    // case FltAsin(x) => throw new spatial.TrigInAccelException(lhs)
    // case FltAcos(x) => throw new spatial.TrigInAccelException(lhs)
    // case FltAtan(x) => throw new spatial.TrigInAccelException(lhs)

    case Mux(sel, a, b) => 
      emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})")
      // lhs.tp match { 
      //   case FixPtType(s,d,f) => 
      //     emitGlobalWire(s"""val ${quote(lhs)} = Wire(new FixedPoint($s,$d,$f))""")
      //   case _ =>
      //     emitGlobalWire(s"""val ${quote(lhs)} = Wire(UInt(${bitWidth(lhs.tp)}.W))""")
      // }
      emit(src"${lhs}.r := Mux(($sel), ${a}.r, ${b}.r)")

    // // Assumes < and > are defined on runtime type...
    // case Min(a, b) => emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emit(src"${lhs}.r := Mux(($a < $b), $a, $b).r")
    // case Max(a, b) => emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})");emit(src"${lhs}.r := Mux(($a > $b), $a, $b).r")

    case FltRecip(x) => x.tp match {
      case DoubleType() => throw new Exception("DoubleType not supported for FltRecip") 
      case HalfType() => emit(src"val $lhs = Utils.frec($x)") 
      case FloatType()  => emit(src"val $lhs = Utils.frec($x)")
    }
    
    // case FltInvSqrt(x) => x.tp match {
    //   case DoubleType() => throw new Exception("DoubleType not supported for FltInvSqrt") 
    //   case HalfType() =>  emit(src"val $lhs = Utils.frsqrt($x)")
    //   case FloatType()  => emit(src"val $lhs = Utils.frsqrt($x)")
    // }

//    case FltAccum(x) => x.tp match {
//      case DoubleType() => throw new Exception("DoubleType not supported for FltAccum") 
//      case HalfType() =>  emit(src"val $lhs = Utils.faccum($x)")
//      case FloatType()  => emit(src"val $lhs = Utils.faccum($x)")
//    }

	case _ => super.gen(lhs, rhs)
  }


}