package spatial.codegen.chiselgen

import argon.core._
import argon.nodes._
import spatial.metadata._

import scala.math._

trait ChiselGenFltPt extends ChiselCodegen {

  override protected def remap(tp: Type[_]): String = tp match {
    case HalfType() => "Half"
    case FloatType()  => "Float"
    case DoubleType() => "Double"
    case _ => super.remap(tp)
  }

  override protected def needsFPType(tp: Type[_]): Boolean = tp match {
      case HalfType() => true
      case FloatType() => true
      case DoubleType() => true
      case _ => super.needsFPType(tp)
  }

  override protected def quoteConst(c: Const[_]): String = (c.tp, c) match {
    // recFNFromFN(8, 24, Mux(faddCode === io.opcode, io.b, getFloatBits(1.0f).S))
    case (HalfType(), Const(cc)) => cc.toString + src".FlP(11, 5)"
    case (FloatType(), Const(cc)) => cc.toString + src".FlP(24, 8)"
    case (DoubleType(), Const(c)) => "DspReal(" + c.toString + ")"
    case _ => super.quoteConst(c)
  }

  override protected def emitNode(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case FltNeg(x)   => emit(src"val $lhs = -$x")
    case FltAdd(x,y) =>
      val regs = if (reduceCycleOf(lhs).nonEmpty) 0 else 12
      emit(src"val $lhs = Utils.fadd($x, $y, $regs)")

    case FltSub(x,y) => emit(src"val $lhs = $x - $y")
    case FltMul(x,y) =>
      // Quick fix: Emit floating point multiply wires in GlobalWires so that
      // Utils.fltaccum (which is in GlobalModules) does not fail Chisel compilation in the
      // common case of FltMul + fltaccum. The real fix would be to identify the signal
      // being accumulated and emit that as a GlobalWire, and remove this hack.
      emitGlobalWireMap(src"""$lhs""", src"""Wire(${newWire(lhs.tp)})""")
      emit(src"$lhs := $x *-* $y")
    case FltDiv(x,y) => emit(src"val $lhs = $x /-/ $y")
    case FltLt(x,y)  => emit(src"val $lhs = $x < $y")
    case FltLeq(x,y) => emit(src"val $lhs = $x <= $y")
    case FltNeq(x,y) => emit(src"val $lhs = $x != $y")
    case FltEql(x,y) => emit(src"val $lhs = $x === $y")
    case FltRandom(x) => lhs.tp match {
      case FloatType()  => 
          val seed = (random*1000).toInt
          emit(src"val $lhs = Utils.frand(${seed}, 24, 8)")        
      case HalfType()  => 
          val seed = (random*1000).toInt
          emit(src"val $lhs = Utils.frand(${seed}, 11, 5)")        
      case DoubleType() => emit(src"val $lhs = chisel.util.Random.nextDouble()")
    }
    case FltConvert(x) =>
      val FltPtType(m,e) = lhs.tp
      emit(src"val $lhs = $x.toFloat($m,$e)")

    case FltPtToFixPt(x) =>
      val FixPtType(s,i,f) = lhs.tp
      emit(src"val $lhs = $x.toFixed($s,$i,$f)")

    case StringToFltPt(x) => lhs.tp match {
      //case HalfType()  => emit(src"val $lhs = $x.toHalf)
      case DoubleType() => emit(src"val $lhs = $x.toDouble")
      case FloatType()  => emit(src"val $lhs = $x.toFloat")
    }
    case _ => super.emitNode(lhs, rhs)
  }

}
