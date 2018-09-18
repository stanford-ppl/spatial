package spatial.codegen.chiselgen

import argon.core._
import argon.nodes._
import spatial.aliases._
import spatial.metadata._
import spatial.nodes._

trait ChiselGenMath extends ChiselGenSRAM {

  override protected def name(s: Dyn[_]): String = s match {
    case Def(FixRandom(x)) => s"${s}_fixrnd"
    case Def(FixNeg(x:Exp[_]))  => s"""${s}_${s.name.getOrElse(s"neg${quoteOperand(x)}")}"""
    case Def(FixAdd(x:Exp[_],y:Exp[_]))  => s"""${s}_${s.name.getOrElse("sum")}"""
    case Def(FixSub(x:Exp[_],y:Exp[_]))  => s"""${s}_${s.name.getOrElse("sub")}"""
    case Def(FixDiv(x:Exp[_],y:Exp[_]))  => s"""${s}_${s.name.getOrElse("div")}"""
    case Def(FixMul(x:Exp[_],y:Exp[_]))  => s"""${s}_${s.name.getOrElse("mul")}"""
    case _ => super.name(s)
  } 

  def quoteOperand(s: Exp[_]): String = s match {
    case ss:Sym[_] => s"x${ss.id}"
    case Const(xx:Exp[_]) => s"${boundOf(xx).toInt}"
    case _ => "unk"
  }

  def emitt(lhs: Sym[_], rhs: String): Unit = {
    emitGlobalWireMap(src"$lhs", src"Wire(${newWire(lhs.tp)})")
    emit(src"${lhs}.r := ($rhs.r)")
  }

  override protected def emitNode(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case FixMul(x,y) =>
      alphaconv_register(src"$lhs")
      emitGlobalWireMap(src"$lhs", src"Wire(${newWire(lhs.tp)})")
      emit(src"${lhs}.r := ($x.*-*($y, ${latencyOptionString("FixMul", Some(bitWidth(lhs.tp)))}).r)")

    case FixDiv(x,y) =>
      emitGlobalWireMap(src"$lhs", src"Wire(${newWire(lhs.tp)})")
      emit(src"${lhs}.r := ($x./-/($y, ${latencyOptionString("FixDiv", Some(bitWidth(lhs.tp)))}).r)")

    case FixMod(x,y) =>
      emitGlobalWireMap(src"$lhs",src"Wire(${newWire(lhs.tp)})")
      emit(src"$lhs := $x.%-%($y, ${latencyOptionString("FixMod", Some(bitWidth(lhs.tp)))})")

    case FixAbs(x) =>
      emitGlobalWireMap(src"$lhs", src"Wire(${newWire(lhs.tp)})")
      emit(src"${lhs}.r := Mux(${x} < 0.U, -$x, $x).r")

    case FixFMA(x,y,z) => 
      alphaconv_register(src"$lhs")
      emitGlobalWireMap(src"$lhs", src"Wire(${newWire(lhs.tp)})")
      emit(src"${lhs}.r := ($x.*-*($y, ${latencyOptionString("FixMul", Some(bitWidth(lhs.tp)))})+$z).r")

    case FltFMA(x,y,z) => 
      alphaconv_register(src"$lhs")
      emitGlobalWireMap(src"$lhs", src"Wire(${newWire(lhs.tp)})")
      emit(src"${lhs}.r := ($x.FltFMA($y, $z).r)")

    case FltAbs(x)  => x.tp match {
      case DoubleType() => throw new Exception("DoubleType not supported for FltAbs!")
      case HalfType() => emitt(lhs, src"Utils.fabs($x)")
      case FloatType()  => emitt(lhs, src"Utils.fabs($x)")
    }
    case FltLog(x)  => x.tp match {
      case DoubleType() => throw new Exception("DoubleType not supported for FltLog!")
      case HalfType() => emitt(lhs, src"Utils.flog($x)")
      case FloatType()  => emitt(lhs, src"Utils.flog($x)")
    }
    case FltExp(x)  => x.tp match {
      case DoubleType() => throw new Exception("DoubleType not supported for FltExp!")
      case HalfType() => emitt(lhs, src"Utils.fexp($x)")
      case FloatType()  => emitt(lhs, src"Utils.fexp($x)")
    }
    case FltSqrt(x) => x.tp match {
      case DoubleType() => throw new Exception("DoubleType not supported for FltSqrt!")
      case HalfType() => emitt(lhs, src"Utils.fsqrt($x)")
      case FloatType()  => emitt(lhs, src"Utils.fsqrt($x)")
    }
    case FixSqrt(x)    => emitt(lhs, src"Utils.fixsqrt($x)")
    case FixInvSqrt(x) => emitt(lhs, src"Utils.fixrsqrt($x)")
    case FixExp(x)     => emitt(lhs, src"Utils.fixexp($x)")
    case FixLog(x)     => emitt(lhs, src"Utils.fixlog($x)")
    case FixSigmoid(x) => emitt(lhs, src"Utils.fixsigmoid($x)")
    case FixTanh(x)    => emitt(lhs, src"Utils.fixtanh($x)")

    case FltPow(x,y) => if (emitEn) throw new Exception("Pow not implemented in hardware yet!")
    case FixFloor(x) => emitt(lhs, src"Utils.floor($x)")
    case FixCeil(x)  => emitt(lhs, src"Utils.ceil($x)")

    case FltSin(x)  => throw new spatial.TrigInAccelException(lhs)
    case FltCos(x)  => throw new spatial.TrigInAccelException(lhs)
    case FltTan(x)  => throw new spatial.TrigInAccelException(lhs)
    case FltSinh(x) => throw new spatial.TrigInAccelException(lhs)
    case FltCosh(x) => throw new spatial.TrigInAccelException(lhs)
    case FltTanh(x)    => emitt(lhs, src"Utils.tanh($x)")
    case FltSigmoid(x) => emitt(lhs, src"Utils.sigmoid($x)")
    case FltAsin(x) => throw new spatial.TrigInAccelException(lhs)
    case FltAcos(x) => throw new spatial.TrigInAccelException(lhs)
    case FltAtan(x) => throw new spatial.TrigInAccelException(lhs)

    case Mux(sel, a, b) => 
      emitGlobalWireMap(src"$lhs", src"Wire(${newWire(lhs.tp)})")
      // lhs.tp match { 
      //   case FixPtType(s,d,f) => 
      //     emitGlobalWire(s"""val ${quote(lhs)} = Wire(new FixedPoint($s,$d,$f))""")
      //   case _ =>
      //     emitGlobalWire(s"""val ${quote(lhs)} = Wire(UInt(${bitWidth(lhs.tp)}.W))""")
      // }
      emit(src"${lhs}.r := Mux(($sel), ${a}.r, ${b}.r)")

    // Assumes < and > are defined on runtime type...
    case Min(a, b) => a.tp match {
      case FloatType() => 
        emitGlobalWireMap(src"$lhs", src"Wire(${newWire(lhs.tp)})")
        emit(src"""${lhs}.r := Mux(($a < $b), ${DL(src"$a",2)}, ${DL(src"$b",2)}).r""")
      case FixPtType(_,_,_) =>   
        emitGlobalWireMap(src"$lhs", src"Wire(${newWire(lhs.tp)})")
        emit(src"""${lhs}.r := Mux(($a < $b), $a, $b).r""")
      case _ => throw new Exception( a.tp + "not supported for Min")

    }
    case Max(a, b) => a.tp match {
      case FloatType() => 
        emitGlobalWireMap(src"$lhs", src"Wire(${newWire(lhs.tp)})")
        emit(src"""${lhs}.r := Mux(($a > $b), ${DL(src"$a",2)}, ${DL(src"$b",2)}).r""")
      case FixPtType(_,_,_) =>   
        emitGlobalWireMap(src"$lhs", src"Wire(${newWire(lhs.tp)})")
        emit(src"""${lhs}.r := Mux(($a > $b), $a, $b).r""")
      case _ => throw new Exception( a.tp + "not supported for Max")
    }


    case FltRecip(x) => x.tp match {
      case DoubleType() => throw new Exception("DoubleType not supported for FltRecip") 
      case HalfType() => emit(src"val $lhs = Utils.frec($x)") 
      case FloatType()  => emit(src"val $lhs = Utils.frec($x)")
    }
    
    case FltInvSqrt(x) => x.tp match {
      case DoubleType() => throw new Exception("DoubleType not supported for FltInvSqrt") 
      case HalfType() =>  emit(src"val $lhs = Utils.frsqrt($x)")
      case FloatType()  => emit(src"val $lhs = Utils.frsqrt($x)")
    }

//    case FltAccum(x) => x.tp match {
//      case DoubleType() => throw new Exception("DoubleType not supported for FltAccum") 
//      case HalfType() =>  emit(src"val $lhs = Utils.faccum($x)")
//      case FloatType()  => emit(src"val $lhs = Utils.faccum($x)")
//    }

    case _ => super.emitNode(lhs, rhs)
  }

}
