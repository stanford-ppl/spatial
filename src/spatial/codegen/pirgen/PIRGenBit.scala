package spatial.codegen.pirgen

import argon._
import argon.node._
import spatial.lang._

import emul.Bool

trait PIRGenBit extends PIRCodegen {

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case Not(x)    => genOp(lhs, rhs)
    case And(x,y)  => emit(src"val $lhs = $x && $y")
    case Or(x,y)   => emit(src"val $lhs = $x || $y")
    case Xor(x,y)  => emit(src"val $lhs = $x !== $y")
    case Xnor(x,y) => emit(src"val $lhs = $x === $y")

    case BitRandom(None)      => emit(src"val $lhs = Bool(java.util.concurrent.ThreadLocalRandom.current().nextBoolean())")
    case BitRandom(Some(max)) => emit(src"val $lhs = Bool(java.util.concurrent.ThreadLocalRandom.current().nextBoolean() && $max)")
    case TextToBit(x)         => emit(src"val $lhs = Bool.from($x)")
    case BitToText(x)         => emit(src"val $lhs = $x.toString")
    case _ => super.genAccel(lhs, rhs)
  }
}
