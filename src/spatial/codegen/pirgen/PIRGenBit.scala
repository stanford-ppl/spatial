package spatial.codegen.pirgen

import argon._
import spatial.lang._
import spatial.node._

import emul.Bool

trait PIRGenBit extends PIRGenBits {

  //override protected def remap(tp: Type[_]): String = tp match {
    //case _:Bit => "Bool"
    //case _ => super.remap(tp)
  //}

  //override protected def quoteConst(tp: Type[_], c: Any): String = (tp, c) match {
    //case (_:Bit, c:Bool) => s"Bool(${c.value},${c.valid})"
    //case _ => super.quoteConst(tp,c)
  //}

  //override def invalid(tp: Type[_]): String = tp match {
    //case _:Bit => "Bool(false,false)"
    //case _ => super.invalid(tp)
  //}

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    //case Not(x)    => 
    //case And(x,y)  => 
    //case Or(x,y)   => 
    //case Xor(x,y)  => 
    //case Xnor(x,y) => 

    //case BitRandom(None)      => 
    //case BitRandom(Some(max)) => 
    case TextToBit(x)         => emitDummy(lhs, rhs)
    case BitToText(x)         => emitDummy(lhs, rhs)
    case _ => super.gen(lhs, rhs)
  }
}
