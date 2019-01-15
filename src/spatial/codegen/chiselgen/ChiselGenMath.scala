package spatial.codegen.chiselgen

import argon._
import argon.node._
import argon.codegen.Codegen
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.retiming._
import spatial.util.spatialConfig

trait ChiselGenMath extends ChiselGenCommon {

  // Cut back on code size by replacing long strings
  private def newEnsig(code: String): String = {
    emit(s"val ensig${ensigs.size} = Wire(Bool())")
    emit(s"ensig${ensigs.size} := $code")
    ensigs += code
    s"ensig${ensigs.size-1}"
  }
  // TODO: Clean this and make it nice
  private def MathDL(lhs: Sym[_], rhs: Op[_], nodelat: Double): Unit = {
    emit(createWire(quote(lhs),remap(lhs.tp)))

    val backpressure_raw = if (controllerStack.nonEmpty) getBackPressure(controllerStack.head.toCtrl) else "true.B"
    val backpressure = if (ensigs.contains(backpressure_raw) && backpressure_raw != "true.B") s"ensig${ensigs.indexOf(backpressure_raw)}"
                       else if (backpressure_raw == "true.B") "true.B"
                       else {newEnsig(backpressure_raw)}
    val lat = if ((lhs.fullDelay + nodelat).toInt != (lhs.fullDelay.toInt + nodelat.toInt)) s"Some($nodelat + 1.0)" else s"Some($nodelat)"
    rhs match {
      case FixMul(x,y) => emit(src"$lhs.r := ($x.mul($y, $lat, $backpressure, Truncate, Wrapping)).r")
      case UnbMul(x,y) => emit(src"$lhs.r := ($x.mul($y, $lat, $backpressure, Unbiased, Wrapping)).r")
      case SatMul(x,y) => emit(src"$lhs.r := ($x.mul($y, $lat, $backpressure, Truncate, Saturating)).r")
      case UnbSatMul(x,y) => emit(src"$lhs.r := ($x.mul($y, $lat, $backpressure, Unbiased, Saturating)).r")
      case FixDiv(x,y) => emit(src"$lhs.r := ($x.div($y, $lat, $backpressure, Truncate, Wrapping)).r")
      case UnbDiv(x,y) => emit(src"$lhs.r := ($x.div($y, $lat, $backpressure, Unbiased, Wrapping)).r")
      case SatDiv(x,y) => emit(src"$lhs.r := ($x.div($y, $lat, $backpressure, Truncate, Saturating)).r")
      case UnbSatDiv(x,y) => emit(src"$lhs.r := ($x.div($y, $lat, $backpressure, Unbiased, Saturating)).r")
      case FixMod(x,y) => emit(src"$lhs.r := ($x.mod($y, $lat, $backpressure)).r")
      case FixRecip(x) => emit(src"$lhs.r := (${lhs}_one.div($x, $lat, $backpressure)).r")
      case FixSqrt(x) => emit(src"$lhs.r := Math.sqrt($x, $lat, $backpressure).r")
      case FixSin(x) => emit(src"$lhs.r := Math.sin($x, $lat, $backpressure).r")
      case FixCos(x) => emit(src"$lhs.r := Math.cos($x, $lat, $backpressure).r")
      case FixAtan(x) => emit(src"$lhs.r := Math.tan($x, $lat, $backpressure).r")
      case FixSinh(x) => emit(src"$lhs.r := Math.sin($x, $lat, $backpressure).r")
      case FixCosh(x) => emit(src"$lhs.r := Math.cos($x, $lat, $backpressure).r")
      case FixRecipSqrt(x) => emit(src"$lhs.r := (${lhs}_one.div(Math.sqrt($x, ${s"""latencyOption("FixSqrt", Some(bitWidth(lhs.tp)))"""}, $backpressure), $lat, $backpressure)).r")
      case FixFMA(x,y,z) => emit(src"$lhs.r := Math.fma($x,$y,$z,$lat, $backpressure).toFixed($lhs).r")
      case FltFMA(x,y,z) => emit(src"$lhs.r := Math.fma($x,$y,$z,$lat, $backpressure).r")
      case FltSqrt(x) => emit(src"$lhs.r := Math.fsqrt($x, $lat, $backpressure).r")
      case FltAdd(x,y) => emit(src"$lhs.r := Math.fadd($x, $y, $lat, $backpressure).r")
      case FltSub(x,y) => emit(src"$lhs.r := Math.fsub($x, $y, $lat, $backpressure).r")
      case FltMul(x,y) => emit(src"$lhs.r := Math.fmul($x, $y, $lat, $backpressure).r")
      case FltDiv(x,y) => emit(src"$lhs.r := Math.fdiv($x, $y, $lat, $backpressure).r")
      case FixLst(x,y) => emit(src"$lhs.r := Math.lt($x, $y, $lat, $backpressure).r")
      case FixLeq(x,y) => emit(src"$lhs.r := Math.lte($x, $y, $lat, $backpressure).r")
      case FixNeq(x,y) => emit(src"$lhs.r := Math.neq($x, $y, $lat, $backpressure).r")
      case FixEql(x,y) => emit(src"$lhs.r := Math.eql($x, $y, $lat, $backpressure).r")
      case FltLst(x,y) => emit(src"$lhs.r := Math.flt($x, $y, $lat, $backpressure).r")
      case FltLeq(x,y) => emit(src"$lhs.r := Math.flte($x, $y, $lat, $backpressure).r")
      case FltNeq(x,y) => emit(src"$lhs.r := Math.fneq($x, $y, $lat, $backpressure).r")
      case FltEql(x,y) => emit(src"$lhs.r := Math.feql($x, $y, $lat, $backpressure).r")
      case FltRecip(x) => emit(src"$lhs.r := Math.frec($x, $lat, $backpressure).r")
      case FixInv(x)   => emit(src"$lhs.r := Math.inv($x,$lat, $backpressure).r")
      case FixNeg(x)   => emit(src"$lhs.r := Math.neg($x,$lat, $backpressure).r")
      case FixAdd(x,y) => emit(src"$lhs.r := Math.add($x,$y,$lat, $backpressure, Truncate, Wrapping).r")
      case FixSub(x,y) => emit(src"$lhs.r := Math.sub($x,$y,$lat, $backpressure, Truncate, Wrapping).r")
      case FixAnd(x,y)  => emit(src"$lhs.r := Math.and($x,$y,$lat, $backpressure).r")
      case FixOr(x,y)   => emit(src"$lhs.r := Math.or($x,$y,$lat, $backpressure).r")
      case FixXor(x,y)  => emit(src"$lhs.r := Math.xor($x,$y,$lat, $backpressure).r")
      case SatAdd(x,y) => emit(src"$lhs.r := Math.add($x, $y,$lat, $backpressure, Truncate, Saturating).r")
      case SatSub(x,y) => emit(src"$lhs.r := Math.sub($x, $y,$lat, $backpressure, Truncate, Saturating).r")
      case FixToFix(x, fmt) => emit(src"$lhs.r := Math.fix2fix(${x}, ${fmt.sign}, ${fmt.ibits}, ${fmt.fbits}, $lat, $backpressure, Truncate, Wrapping).r")
      case FixToFixSat(x, fmt) => emit(src"$lhs.r := Math.fix2fix(${x}, ${fmt.sign}, ${fmt.ibits}, ${fmt.fbits}, $lat, $backpressure, Truncate, Saturating).r")
      case FixToFixUnb(x, fmt) => emit(src"$lhs.r := Math.fix2fix(${x}, ${fmt.sign}, ${fmt.ibits}, ${fmt.fbits}, $lat, $backpressure, Unbiased, Wrapping).r")
      case FixToFixUnbSat(x, fmt) => emit(src"$lhs.r := Math.fix2fix(${x}, ${fmt.sign}, ${fmt.ibits}, ${fmt.fbits}, $lat, $backpressure, Unbiased, Saturating).r")
      case FixSLA(x,y) => 
        val shift = DLTrace(y).getOrElse(throw new Exception("Cannot shift by non-constant amount in accel")).replaceAll("\\.FP.*|\\.U.*|\\.S.*|L","")
        emit(src"$lhs.r := Math.arith_left_shift($x, $shift, $lat, $backpressure).r")
      case FixSRA(x,y) => 
        val shift = DLTrace(y).getOrElse(throw new Exception("Cannot shift by non-constant amount in accel")).replaceAll("\\.FP.*|\\.U.*|\\.S.*|L","")
        emit(src"$lhs.r := Math.arith_right_shift($x, $shift, $lat, $backpressure).r")
      case FixSRU(x,y) => 
        val shift = DLTrace(y).getOrElse(throw new Exception("Cannot shift by non-constant amount in accel")).replaceAll("\\.FP.*|\\.U.*|\\.S.*|L","")
        emit(src"$lhs.r := Math.logic_right_shift($x, $shift, $lat, $backpressure).r")

      case FixToFlt(x, fmt) => 
        val FixPtType(s,d,f) = x.tp
        val FltPtType(m,e) = lhs.tp
        emit(src"$lhs.r := Math.fix2flt($x,$m,$e,$lat,$backpressure).r")
      case FltToFix(x, fmt) => 
        val FixPtType(s,d,f) = lhs.tp
        val FltPtType(m,e) = x.tp
        emit(src"$lhs.r := Math.flt2fix($x, $s,$d,$f,$lat,$backpressure, Truncate, Wrapping).r")
      case FltToFlt(x, fmt) => 
        val FltPtType(m,e) = lhs.tp
        emit(src"$lhs.r := Math.flt2flt($x, $m, $e, $lat, $backpressure).r")

    }
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case _ if lhs.isBroadcastAddr => // Do nothing
    case FixInv(x)   => MathDL(lhs, rhs, latencyOption("FixInv", Some(bitWidth(lhs.tp))))
    case FixNeg(x)   => MathDL(lhs, rhs, latencyOption("FixNeg", Some(bitWidth(lhs.tp))))
    case FixAdd(x,y) => MathDL(lhs, rhs, latencyOption("FixAdd", Some(bitWidth(lhs.tp))))
    case FixSub(x,y) => MathDL(lhs, rhs, latencyOption("FixSub", Some(bitWidth(lhs.tp))))
    case FixAnd(x,y)  => MathDL(lhs, rhs, latencyOption("FixAnd", Some(bitWidth(lhs.tp))))
    case FixOr(x,y)   => MathDL(lhs, rhs, latencyOption("FixOr", Some(bitWidth(lhs.tp))))
    case FixXor(x,y)  => MathDL(lhs, rhs, latencyOption("FixXor", Some(bitWidth(lhs.tp))))
    case FixPow(x,y)  => throw new Exception(s"FixPow($x, $y) should have transformed to either a multiply tree (constant exp) or reduce structure (variable exp)")
    case VecApply(vector, i) => emit(createWire(quote(lhs),remap(lhs.tp))); emit(src"$lhs := $vector.apply($i)")

    case FixLst(x,y) => MathDL(lhs, rhs, latencyOption("FixLst", Some(bitWidth(lhs.tp))))
    case FixLeq(x,y) => MathDL(lhs, rhs, latencyOption("FixLeq", Some(bitWidth(lhs.tp))))
    case FixNeq(x,y) => MathDL(lhs, rhs, latencyOption("FixNeq", Some(bitWidth(lhs.tp))))
    case FixEql(x,y) => MathDL(lhs, rhs, latencyOption("FixEql", Some(bitWidth(lhs.tp))))
    case FltLst(x,y) => MathDL(lhs, rhs, latencyOption("FltLst", Some(bitWidth(lhs.tp))))
    case FltLeq(x,y) => MathDL(lhs, rhs, latencyOption("FltLeq", Some(bitWidth(lhs.tp))))
    case FltNeq(x,y) => MathDL(lhs, rhs, latencyOption("FltNeq", Some(bitWidth(lhs.tp))))
    case FltEql(x,y) => MathDL(lhs, rhs, latencyOption("FltEql", Some(bitWidth(lhs.tp))))
    case UnbMul(x,y) => MathDL(lhs, rhs, latencyOption("UnbMul", Some(bitWidth(lhs.tp)))) 
    case UnbDiv(x,y) => MathDL(lhs, rhs, latencyOption("UnbDiv", Some(bitWidth(lhs.tp)))) 
    case SatMul(x,y) => MathDL(lhs, rhs, latencyOption("SatMul", Some(bitWidth(lhs.tp)))) 
    case SatDiv(x,y) => MathDL(lhs, rhs, latencyOption("SatDiv", Some(bitWidth(lhs.tp)))) 
    case UnbSatMul(x,y) => MathDL(lhs, rhs, latencyOption("SatMul", Some(bitWidth(lhs.tp)))) 
    case UnbSatDiv(x,y) => MathDL(lhs, rhs, latencyOption("SatDiv", Some(bitWidth(lhs.tp)))) 
    case FixMul(x,y) => MathDL(lhs, rhs, latencyOption("FixMul", Some(bitWidth(lhs.tp))))
    case FixDiv(x,y) => MathDL(lhs, rhs, latencyOption("FixDiv", Some(bitWidth(lhs.tp))))
    case FixRecipSqrt(a) => 
      emit(createWire(src"${lhs}_one", src"${lhs.tp}"))
      emit(src"${lhs}_one.r := 1.FP(${lhs}_one.s, ${lhs}_one.d, ${lhs}_one.f).r")
      MathDL(lhs, rhs, latencyOption("FixDiv", Some(bitWidth(lhs.tp)))) 
    case FixRecip(y) => 
      emit(createWire(src"${lhs}_one", src"${lhs.tp}"))
      emit(src"${lhs}_one.r := 1.FP(${lhs}_one.s, ${lhs}_one.d, ${lhs}_one.f).r")
      MathDL(lhs, rhs, latencyOption("FixDiv", Some(bitWidth(lhs.tp)))) 
    case FixMod(x,y) => MathDL(lhs, rhs, latencyOption("FixMod", Some(bitWidth(lhs.tp)))) 
    case FixFMA(x,y,z) => MathDL(lhs, rhs, latencyOption("FixFMA", Some(bitWidth(lhs.tp)))) 
      

    case SatAdd(x,y) => MathDL(lhs, rhs, latencyOption("FixAdd", Some(bitWidth(lhs.tp))))
    case SatSub(x,y) => MathDL(lhs, rhs, latencyOption("FixSub", Some(bitWidth(lhs.tp))))
    case FixSLA(x,y) => MathDL(lhs, rhs, latencyOption("FixSLA", Some(bitWidth(lhs.tp))))
    case FixSRA(x,y) => MathDL(lhs, rhs, latencyOption("FixSLA", Some(bitWidth(lhs.tp))))
    case FixSRU(x,y) => MathDL(lhs, rhs, latencyOption("FixSLA", Some(bitWidth(lhs.tp))))
    case BitRandom(None) if lhs.parent.s.isDefined => emit(src"val $lhs = Math.fixrand(${scala.math.random*scala.math.pow(2, bitWidth(lhs.tp))}.toInt, ${bitWidth(lhs.tp)}, datapathEn) === 1.U")
    case FixRandom(None) if lhs.parent.s.isDefined => emit(createWire(quote(lhs),remap(lhs.tp)));emit(src"$lhs.r := Math.fixrand(${scala.math.random*scala.math.pow(2, bitWidth(lhs.tp))}.toInt, ${bitWidth(lhs.tp)}, datapathEn).r")
    case FixRandom(x) =>
      val FixPtType(s,d,f) = lhs.tp
      emit(createWire(quote(lhs),remap(lhs.tp)))
      val seed = (scala.math.random*1000).toInt
      val size = x match{
        case Some(Const(xx)) => s"$xx"
        case Some(_) => s"$x"
        case None => "4096"
      }
      emit(s"val ${quote(lhs)}_bitsize = fringe.utils.log2Up($size) max 1")
      emit(src"val ${lhs}_rng = Module(new PRNG($seed))")
      val en = if (lhs.parent.s.isDefined) src"datapathEn" else "true.B"
      emit(src"${lhs}_rng.io.en := $en")
      emit(src"${lhs}.r := ${lhs}_rng.io.output(${lhs}_bitsize,0)")
    case FltRandom(None) if lhs.parent.s.isDefined => 
      val FltPtType(m,e) = lhs.tp
      emit(createWire(quote(lhs),remap(lhs.tp)))
      emit(src"$lhs.r := Math.frand(${scala.math.random*scala.math.pow(2, bitWidth(lhs.tp))}.toInt, $m, $e, datapathEn).r")
    case FltRandom(x) => throw new Exception(s"Can only generate random float with no bounds right now!")

    case FixAbs(x) =>
      emit(createWire(quote(lhs),remap(lhs.tp)))
      emit(src"$lhs.r := Mux($x < 0.U, -$x, $x).r")

    case FixSqrt(x) => MathDL(lhs, rhs, latencyOption("FixSqrt", Some(bitWidth(lhs.tp))))
    case FixSin(x) => MathDL(lhs, rhs, latencyOption("FixSin", Some(bitWidth(lhs.tp))))
    case FixCos(x) => MathDL(lhs, rhs, latencyOption("FixCos", Some(bitWidth(lhs.tp))))
    case FixAtan(x) => MathDL(lhs, rhs, latencyOption("FixAtan", Some(bitWidth(lhs.tp))))
    case FixSinh(x) => MathDL(lhs, rhs, latencyOption("FixSinh", Some(bitWidth(lhs.tp))))
    case FixCosh(x) => MathDL(lhs, rhs, latencyOption("FixCosh", Some(bitWidth(lhs.tp))))
    case FltFMA(x,y,z) => MathDL(lhs, rhs, latencyOption("FltFMA", Some(bitWidth(lhs.tp)))) 

    case FltNeg(x) =>
      emit(createWire(quote(lhs),remap(lhs.tp)))
      emit(src"$lhs.r := (-$x).r")

    case FltAdd(x,y) => MathDL(lhs, rhs, latencyOption("FltAdd", Some(bitWidth(lhs.tp))))
    case FltSub(x,y) => MathDL(lhs, rhs, latencyOption("FltSub", Some(bitWidth(lhs.tp))))
    case FltMul(x,y) => MathDL(lhs, rhs, latencyOption("FltMul", Some(bitWidth(lhs.tp))))
    case FltDiv(x,y) => MathDL(lhs, rhs, latencyOption("FltDiv", Some(bitWidth(lhs.tp))))
    case FltMax(x,y) => 
      emit(createWire(quote(lhs),remap(lhs.tp)))
      emit(src"$lhs.r := Mux($x > $y, ${x}.r, ${y}.r)")

    case FltMin(x,y) => 
      emit(createWire(quote(lhs),remap(lhs.tp)))
      emit(src"$lhs.r := Mux($x < $y, ${x}.r, ${y}.r)")

    case FltAbs(x) => 
      emit(createWire(quote(lhs),remap(lhs.tp)))
      emit(src"$lhs.r := chisel3.util.Cat(false.B, ${x}(${x}.getWidth-1,0))")

    case FltPow(x,exp) => throw new Exception(s"FltPow($x, $exp) should have transformed to either a multiply tree (constant exp) or reduce structure (variable exp)")

    case FltSqrt(x) => MathDL(lhs, rhs, latencyOption("FltSqrt", Some(bitWidth(lhs.tp))))

    // case FltPow(x,y) => if (emitEn) throw new Exception("Pow not implemented in hardware yet!")
    // case FixFloor(x) => emit(src"val $lhs = floor($x)")
    // case FixCeil(x) => emit(src"val $lhs = ceil($x)")

    // case FltSin(x)  => throw new spatial.TrigInAccelException(lhs)
    // case FltCos(x)  => throw new spatial.TrigInAccelException(lhs)
    // case FltTan(x)  => throw new spatial.TrigInAccelException(lhs)
    // case FltSinh(x) => throw new spatial.TrigInAccelException(lhs)
    // case FltCosh(x) => throw new spatial.TrigInAccelException(lhs)
    // case FltTanh(x) => emit(src"val $lhs = tanh($x)")
    // case FltSigmoid(x) => emit(src"val $lhs = sigmoid($x)")
    // case FltAsin(x) => throw new spatial.TrigInAccelException(lhs)
    // case FltAcos(x) => throw new spatial.TrigInAccelException(lhs)
    // case FltAtan(x) => throw new spatial.TrigInAccelException(lhs)

    case OneHotMux(sels, opts) => 
      emit(createWire(quote(lhs),remap(lhs.tp)))
      emit(src"$lhs.r := Mux1H(List($sels), List(${opts.map{x => src"$x.r"}}))")

    case PriorityMux(sels, opts) => 
      emit(createWire(quote(lhs),remap(lhs.tp)))
      emit(src"$lhs.r := PriorityMux(List($sels), List(${opts.map{x => src"$x.r"}}))")

    case Mux(sel, a, b) => 
      emit(createWire(quote(lhs),remap(lhs.tp)))
      emit(src"$lhs.r := Mux(($sel), $a.r, $b.r)")

    case FixMin(a, b) => emit(createWire(quote(lhs),remap(lhs.tp)));emit(src"$lhs.r := Mux(($a < $b), $a, $b).r")
    case FixMax(a, b) => emit(createWire(quote(lhs),remap(lhs.tp)));emit(src"$lhs.r := Mux(($a > $b), $a, $b).r")
    case FixToFix(a, fmt) => MathDL(lhs, rhs, latencyOption("FixToFix", Some(bitWidth(lhs.tp))))
    case FixToFixSat(a, fmt) => MathDL(lhs, rhs, latencyOption("FixToFixSat", Some(bitWidth(lhs.tp))))
    case FixToFixUnb(a, fmt) => MathDL(lhs, rhs, latencyOption("FixToFixUnb", Some(bitWidth(lhs.tp))))
    case FixToFixUnbSat(a, fmt) => MathDL(lhs, rhs, latencyOption("FixToFixUnbSat", Some(bitWidth(lhs.tp))))
    case FltToFlt(a, fmt) => MathDL(lhs, rhs, latencyOption("FltToFlt", Some(bitWidth(lhs.tp))))
    case FixToFlt(a, fmt) => MathDL(lhs, rhs, latencyOption("FixToFlt", Some(bitWidth(lhs.tp))))
    case FltToFix(a, fmt) => MathDL(lhs, rhs, latencyOption("FltToFix", Some(bitWidth(lhs.tp))))
    case FltRecip(x) => MathDL(lhs, rhs, latencyOption("FltRecip", Some(bitWidth(lhs.tp)))) 
    
    case And(a, b) => emit(createWire(quote(lhs),remap(lhs.tp)));emit(src"$lhs := $a & $b")
    case Not(a) => emit(createWire(quote(lhs),remap(lhs.tp)));emit(src"$lhs := ~$a")
    case Or(a, b) => emit(createWire(quote(lhs),remap(lhs.tp)));emit(src"$lhs := $a | $b")
    case Xor(a, b) => emit(createWire(quote(lhs),remap(lhs.tp)));emit(src"$lhs := $a ^ $b")
    case Xnor(a, b) => emit(createWire(quote(lhs),remap(lhs.tp)));emit(src"$lhs := ~($a ^ $b)")

    case FixFloor(a) => emit(createWire(quote(lhs),remap(lhs.tp)));emit(src"$lhs.r := Cat($a.raw_dec, 0.U(${fracBits(a)}.W))")
    case FixCeil(a) => emit(createWire(quote(lhs),remap(lhs.tp)));emit(src"$lhs.r := Mux($a.raw_frac === 0.U, $a.r, Cat($a.raw_dec + 1.U, 0.U(${fracBits(a)}.W)))")
    // case FltFloor(a) => emit(createWire(quote(lhs),remap(lhs.tp)));emit(src"$lhs.r := Cat($a.raw_dec, 0.U(${fracBits(a)}.W))")
    // case FltCeil(a) => emit(createWire(quote(lhs),remap(lhs.tp)));emit(src"$lhs.r := Mux($a.raw_frac === 0.U, $a.r, Cat($a.raw_dec + 1.U, 0.U(${fracBits(a)}.W)))")
    case DataAsBits(data) => 
      emit(createWire(quote(lhs),remap(lhs.tp)));emit(src"$lhs.zipWithIndex.foreach{case (dab, i) => dab := $data(i)}")
    case BitsAsData(data, fmt) => 
      emit(createWire(quote(lhs),remap(lhs.tp)));emit(src"$lhs.r := chisel3.util.Cat($data.reverse)")
    // case FltInvSqrt(x) => x.tp match {
    //   case DoubleType() => throw new Exception("DoubleType not supported for FltInvSqrt") 
    //   case HalfType() =>  emit(src"val $lhs = frsqrt($x)")
    //   case FloatType()  => emit(src"val $lhs = frsqrt($x)")
    // }

	  case _ => super.gen(lhs, rhs)
  }


}