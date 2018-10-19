package spatial.codegen.pirgen

import argon._
import spatial.lang._
import spatial.metadata.memory._
import spatial.node._

import utils.implicits.collections._

trait PIRGenLIFO extends PIRCodegen {

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@LIFONew(size)    => 
      stateMem(lhs, "LIFO", None)
    //case LIFOIsEmpty(lifo,_) => 
    //case LIFOIsFull(lifo,_)  => 
    //case LIFOIsAlmostEmpty(lifo,_) =>
    //case LIFOIsAlmostFull(lifo,_) =>
    //case op@LIFOPeek(lifo,_) => 
    //case LIFONumel(lifo,_) => 

    case op@LIFOBankedPop(lifo, ens) =>
      stateRead(lhs, lifo, None, None, ens)

    case LIFOBankedPush(lifo, data, ens) =>
      stateWrite(lhs, lifo, None, None, data, ens)

    case _ => super.genAccel(lhs, rhs)
  }
}
