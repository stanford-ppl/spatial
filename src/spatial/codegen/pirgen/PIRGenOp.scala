package spatial.codegen.pirgen

import argon._
import argon.node._
import spatial.lang._
import spatial.node._
import emul.FixedPoint

trait PIRGenOp extends PIRCodegen {

  def genOp(lhs:Sym[_], rhs:Op[_]) = {
    val op = rhs.getClass.getSimpleName
    state(lhs)(src"OpDef(op=$op, inputs=${rhs.productIterator.toList})")
  }

}

