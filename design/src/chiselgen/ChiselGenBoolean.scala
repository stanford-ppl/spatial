package spatial.codegen.chiselgen

import argon.core._
import argon.nodes._

trait ChiselGenBoolean extends ChiselCodegen {

  override protected def remap(tp: Type[_]): String = tp match {
    case BooleanType => "Boolean"
    case _ => super.remap(tp)
  }

  override protected def quoteConst(c: Const[_]): String = c match {
    case Const(c: Boolean) => c.toString + ".B"
    case _ => super.quoteConst(c)
  }

  override protected def emitNode(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case Not(x)       => alphaconv_register(src"$lhs"); emitGlobalWireMap(src"$lhs", src"Wire(Bool())");emit(src"$lhs := !$x")
    case And(x,y)     => alphaconv_register(src"$lhs"); emitGlobalWireMap(src"$lhs", src"Wire(Bool())");emit(src"$lhs := $x && $y")
    case Or(x,y)      => alphaconv_register(src"$lhs"); emitGlobalWireMap(src"$lhs", src"Wire(Bool())");emit(src"$lhs := $x || $y")
    case XOr(x,y)     => alphaconv_register(src"$lhs"); emitGlobalWireMap(src"$lhs", src"Wire(Bool())");emit(src"$lhs := $x =/= $y")
    case XNor(x,y)    => alphaconv_register(src"$lhs"); emitGlobalWireMap(src"$lhs", src"Wire(Bool())");emit(src"$lhs := $x === $y")
    case RandomBoolean(x) => emit(src"val $lhs = java.util.concurrent.ThreadLocalRandom.current().nextBoolean()")
    case StringToBoolean(x) => emit(src"val $lhs = $x.toBoolean")
    case _ => super.emitNode(lhs, rhs)
  }
}
