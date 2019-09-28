package spatial.codegen.pirgen

import argon._
import spatial.lang._
import spatial.metadata.memory._
import spatial.node._

import utils.implicits.collections._

trait PIRGenLIFO extends PIRCodegen {

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@LIFONew(size)    => 
      stateMem(lhs, "LIFO()")
    //case LIFOIsEmpty(lifo,_) => 
    //case LIFOIsFull(lifo,_)  => 
    //case LIFOIsAlmostEmpty(lifo,_) =>
    //case LIFOIsAlmostFull(lifo,_) =>
    //case op@LIFOPeek(lifo,_) => 
    //case LIFONumel(lifo,_) => 

    case op@LIFOBankedPop(lifo, ens) =>
      stateAccess(lhs, lifo, ens) {
        src"MemRead()"
      }

    case LIFOBankedPush(lifo, data, ens) =>
      stateAccess(lhs, lifo, ens, data=Some(data)) {
        src"MemWrite()"
      }

    case _ => super.genAccel(lhs, rhs)
  }
}
