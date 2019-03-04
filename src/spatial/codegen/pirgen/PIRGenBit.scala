package spatial.codegen.pirgen

import argon._
import argon.node._
import spatial.lang._

import emul.Bool

trait PIRGenBit extends PIRCodegen {

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case Not(x)    => genOp(lhs)
    case And(x,y)  => genOp(lhs)
    case Or(x,y)   => genOp(lhs)
    case Xor(x,y)  => genOp(lhs)
    case Xnor(x,y) => genOp(lhs)

    case BitRandom(None)      => genOp(lhs)
      //emit(src"val $lhs = Bool(java.util.concurrent.ThreadLocalRandom.current().nextBoolean())")
    case BitRandom(Some(max)) => genOp(lhs)
      //emit(src"val $lhs = Bool(java.util.concurrent.ThreadLocalRandom.current().nextBoolean() && $max)")
    //case TextToBit(x)         =>  genOp(lhs)
    case BitToText(x)         => genOp(lhs)
    case _ => super.genAccel(lhs, rhs)
  }
}
