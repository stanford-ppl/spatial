package spatial.codegen.chiselgen

import argon.core._
import argon.emul.FixedPoint
import argon.nodes._

import scala.math.random

trait ChiselGenFixPt extends ChiselCodegen {

  override protected def remap(tp: Type[_]): String = tp match {
    case IntType() => "Int"
    case LongType() => "Long"
    case _ => super.remap(tp)
  }

  override protected def bitWidth(tp: Type[_]): Int = tp match {
      case IntType()  => 32
      case LongType() => 32 // or 64?
      case FixPtType(s,d,f) => d+f
      case _ => super.bitWidth(tp)
  }

  override protected def needsFPType(tp: Type[_]): Boolean = tp match {
      case FixPtType(s,d,f) => if (s) true else if (f == 0) false else true
      case IntType()  => false
      case LongType() => false
      case _ => super.needsFPType(tp)
  }

  protected def newWireFix(tp: Type[_]): String = tp match {
    case FixPtType(s,d,f) => src"new FixedPoint($s, $d, $f)"
    case IntType() => "UInt(32.W)"
    case LongType() => "UInt(32.W)"
    case FltPtType(m,e) => src"new FloatingPoint($m,$e)"
    case BooleanType => "Bool()"
    // case tp: VectorType[_] => src"Vec(${tp.width}, ${newWireFix(tp.typeArguments.head)})"
    case tp: StructType[_] => src"UInt(${bitWidth(tp)}.W)"
    // case tp: IssuedCmd => src"UInt(${bitWidth(tp)}.W)"
    case tp: ArrayType[_] => src"Wire(Vec(999, ${newWireFix(tp.typeArguments.head)}"
    case _ => throw new argon.NoWireConstructorException(s"$tp")
  }

  override protected def quoteConst(c: Const[_]): String = (c.tp, c) match {
    case (FixPtType(s,d,f), Const(cc)) =>
      if (d > 32 | (!s & d == 32)) cc.toString + src"L.FP($s, $d, $f)"
      else cc.toString + src".FP($s, $d, $f)"
    case (IntType(), Const(cc: FixedPoint)) =>
      if (cc >= 0) {
        cc.toString + ".toInt.U(32.W)"
      } else {
        cc.toString + ".toInt.S(32.W).asUInt"
      }

    case (LongType(), Const(cc: FixedPoint)) => cc.toLong.toString + ".L"
    case (FixPtType(s,d,f), Const(cc: FixedPoint)) =>
      if (needsFPType(c.tp)) {s"Utils.FixedPoint($s,$d,$f,$cc)"} else {
        if (cc >= 0) cc.toInt.toString + ".U(32.W)" else cc.toInt.toString + ".S(32.W).asUInt"
      }
    case _ => super.quoteConst(c)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case FixInv(x)   => emitGlobalWireMap(src"$lhs", src"Wire(${newWireFix(lhs.tp)})");emit(src"${lhs}.r := (~$x).r")
    case FixNeg(x)   => emitGlobalWireMap(src"$lhs", src"Wire(${newWireFix(lhs.tp)})");emit(src"${lhs}.r := (-$x).r")
    case FixAdd(x,y) => emitGlobalWireMap(src"$lhs", src"Wire(${newWireFix(lhs.tp)})");emit(src"${lhs}.r := ($x + $y).r")
    case FixSub(x,y) => emitGlobalWireMap(src"$lhs", src"Wire(${newWireFix(lhs.tp)})");emit(src"${lhs}.r := ($x - $y).r")
    case FixAnd(x,y)  => emitGlobalWire(src"val $lhs = Wire(${newWireFix(lhs.tp)})");emit(src"$lhs := $x & $y")
    case FixOr(x,y)   => emitGlobalWire(src"val $lhs = Wire(${newWireFix(lhs.tp)})");emit(src"$lhs := $x | $y")
    case FixXor(x,y)  => emitGlobalWire(src"val $lhs = Wire(${newWireFix(lhs.tp)})");emit(src"$lhs := $x ^ $y")
    case FixLt(x,y)  => alphaconv_register(src"$lhs"); emitGlobalWireMap(src"$lhs", src"Wire(${newWireFix(lhs.tp)})");emit(src"$lhs := $x < $y")
    case FixLeq(x,y) => alphaconv_register(src"$lhs"); emitGlobalWireMap(src"$lhs", src"Wire(${newWireFix(lhs.tp)})");emit(src"$lhs := $x <= $y")
    case FixNeq(x,y) => alphaconv_register(src"$lhs"); emitGlobalWireMap(src"$lhs", src"Wire(${newWireFix(lhs.tp)})");emit(src"$lhs := $x =/= $y")
    case FixEql(x,y) => alphaconv_register(src"$lhs"); emitGlobalWireMap(src"$lhs", src"Wire(${newWireFix(lhs.tp)})");emit(src"$lhs := $x === $y")
    case UnbMul(x,y) => emit(src"val $lhs = $x *& $y")
    case UnbDiv(x,y) => emit(src"val $lhs = $x /& $y")
    case SatAdd(x,y) => emit(src"val $lhs = $x <+> $y")
    case SatSub(x,y) => emit(src"val $lhs = $x <-> $y")
    case SatMul(x,y) => emit(src"val $lhs = $x <*> $y")
    case SatDiv(x,y) => emit(src"val $lhs = $x </> $y")
    case FixLsh(x,Const(yy))  => emitGlobalWireMap(src"$lhs", src"Wire(${newWireFix(lhs.tp)})");emit(src"${lhs}.r := ($x << $yy).r // TODO: cast to proper type (chisel expands bits)")
    case FixRsh(x,Const(yy))  => emitGlobalWireMap(src"$lhs", src"Wire(${newWireFix(lhs.tp)})");emit(src"${lhs}.r := ($x >> $yy).r")
    case FixURsh(x,Const(yy)) => emitGlobalWireMap(src"$lhs", src"Wire(${newWireFix(lhs.tp)})");emit(src"${lhs}.r := ($x >>> $yy).r")
    case UnbSatMul(x,y) => emit(src"val $lhs = $x <*&> $y")
    case UnbSatDiv(x,y) => emit(src"val $lhs = $x </&> $y")
    case FixRandom(x) =>
      val seed = (random*1000).toInt
      val size = x match{
        case Some(Const(xx)) => s"$xx"
        case Some(_) => s"$x"
        case None => "4096"
      }
      emit(s"val ${quote(lhs)}_bitsize = Utils.log2Up(${size}) max 1")
      emitGlobalModule(src"val ${lhs}_rng = Module(new PRNG($seed))")
      emitGlobalModule(src"${lhs}_rng.io.en := true.B")
      emit(src"val ${lhs} = ${lhs}_rng.io.output(${lhs}_bitsize,0)")
    case FixUnif() =>
      val bits = lhs.tp match {
        case FixPtType(s,d,f) => f
      }
      val seed = (random*1000).toInt
      emitGlobalModule(src"val ${lhs}_rng = Module(new PRNG($seed))")
      emitGlobalModule(src"${lhs}_rng.io.en := true.B")
      emit(src"val ${lhs} = Wire(new FixedPoint(false, 0, $bits))")
      emit(src"${lhs}.r := ${lhs}_rng.io.output(${bits},0)")
    case FixConvert(x) => lhs.tp match {
      case IntType()  =>
        emitGlobalWireMap(src"$lhs", "Wire(new FixedPoint(true, 32, 0))")
        emit(src"${x}.cast($lhs)")
      case LongType() =>
        // val pad = bitWidth(lhs.tp) - bitWidth(x.tp)
        emitGlobalWireMap(src"$lhs","Wire(new FixedPoint(true, 64, 0))")
        emit(src"${x}.cast($lhs)")
        // if (pad > 0) {
        //   emit(src"${lhs}.r := chisel3.util.Cat(0.U(${pad}.W), ${x}.r)")
        // } else {
        //   emit(src"${lhs}.r := ${x}.r.apply(${bitWidth(lhs.tp)-1}, 0)")
        // }
      case FixPtType(s,d,f) =>
        emit(src"val $lhs = Wire(new FixedPoint($s, $d, $f))")
        emit(src"${x}.cast($lhs)")
    }
    case FixPtToFltPt(x) => 
       val FltPtType(m,e) = lhs.tp
       emit(src"val $lhs = $x.toFloat($m,$e)")

    case StringToFixPt(x) => lhs.tp match {
      case IntType()  => emit(src"val $lhs = $x.toInt")
      case LongType() => emit(src"val $lhs = $x.toLong")
      case _ => emit(src"val $lhs = $x // No rule for this")
    }
    case _ => super.gen(lhs, rhs)
  }
}
