package spatial.codegen.pirgen

import argon._
import argon.node._
import spatial.lang._

import emul.Bool

trait PIRGenBit extends PIRGenBits {

  override protected def remap(tp: Type[_]): String = tp match {
    case _:Bit => "Bool"
    case _ => super.remap(tp)
  }

  override protected def quoteConst(tp: Type[_], c: Any): String = (tp, c) match {
    case (_:Bit, c:Bool) => s"Bool(${c.value},${c.valid})"
    case _ => super.quoteConst(tp,c)
  }

  override def invalid(tp: Type[_]): String = tp match {
    case _:Bit => "Bool(false,false)"
    case _ => super.invalid(tp)
  }


  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case Not(x)    => emit(src"val $lhs = !$x")
    case And(x,y)  => emit(src"val $lhs = $x && $y")
    case Or(x,y)   => emit(src"val $lhs = $x || $y")
    case Xor(x,y)  => emit(src"val $lhs = $x !== $y")
    case Xnor(x,y) => emit(src"val $lhs = $x === $y")

    case BitRandom(None)      => emit(src"val $lhs = Bool(java.util.concurrent.ThreadLocalRandom.current().nextBoolean())")
    case BitRandom(Some(max)) => emit(src"val $lhs = Bool(java.util.concurrent.ThreadLocalRandom.current().nextBoolean() && $max)")
    case TextToBit(x)         => emit(src"val $lhs = Bool.from($x)")
    case BitToText(x)         => emit(src"val $lhs = $x.toString")
    case _ => super.gen(lhs, rhs)
  }
}
