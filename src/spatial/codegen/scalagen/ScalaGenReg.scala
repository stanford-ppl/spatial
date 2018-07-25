package spatial.codegen.scalagen

import argon._
import spatial.lang._
import spatial.node._

trait ScalaGenReg extends ScalaCodegen with ScalaGenMemories {

  override protected def remap(tp: Type[_]): String = tp match {
    case tp: Reg[_] => src"Array[${tp.A}]"
    case _ => super.remap(tp)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@RegNew(init)    =>
      emitMemObject(lhs){ emit(src"object $lhs extends Ptr[${op.A}](null.asInstanceOf[${op.A}])") }
      emit(src"$lhs.initMem($init)")

    case op@ArgInNew(init)  =>
      emitMemObject(lhs){ emit(src"object $lhs extends Ptr[${op.A}](null.asInstanceOf[${op.A}])") }
      emit(src"$lhs.initMem($init)")

    case op@HostIONew(init)  =>
      emitMemObject(lhs){ emit(src"object $lhs extends Ptr[${op.A}](null.asInstanceOf[${op.A}])") }
      emit(src"$lhs.initMem($init)")

    case op@ArgOutNew(init) =>
      emitMemObject(lhs){ emit(src"object $lhs extends Ptr[${op.A}](null.asInstanceOf[${op.A}])") }
      emit(src"$lhs.initMem($init)")

    case RegReset(reg, ens) =>
      val init = reg match {case Op(RegNew(i)) => i }
      emit(src"val $lhs = if (${and(ens)}) $reg.set($init)")

    case RegRead(reg)       => emit(src"val $lhs = $reg.value")
    case RegWrite(reg,v,en) => emit(src"val $lhs = if (${and(en)}) $reg.set($v)")

    case SetReg(reg, v)  => emit(src"val $lhs = $reg.set($v)")
    case GetReg(reg)     => emit(src"val $lhs = $reg.value")

    case RegAccumOp(reg,in,en,op,first) =>
      open(src"val $lhs = if (${and(en)}) {")
        val input = op match {
          case Accum.Add => src"$reg.value + $in"
          case Accum.Mul => src"$reg.value * $in"
          case Accum.Max => src"Number.max($reg.value, $in)"
          case Accum.Min => src"Number.min($reg.value, $in)"
        }
        emit(src"$reg.set((if ($first) $in else $input))")
      close("}")

    case RegAccumFMA(reg,m0,m1,en,first) =>
      open(src"val $lhs = if (${and(en)}) {")
        val input = src"$m0 * $m1 + $reg.value"
        emit(src"$reg.set((if ($first) $m0*$m1 else $input))")
      close("}")

    case _ => super.gen(lhs, rhs)
  }

}
