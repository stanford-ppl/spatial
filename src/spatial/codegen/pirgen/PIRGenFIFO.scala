package spatial.codegen.pirgen

import argon._
import spatial.metadata.memory._
import spatial.metadata.bounds.Expect
import spatial.lang._
import spatial.node._

import utils.implicits.collections._

trait PIRGenFIFO extends PIRCodegen {

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@FIFONew(Expect(size))    => 
      stateMem(lhs, "FIFO()", depth=Some(size.toInt))
    case FIFOIsEmpty(fifo,_) => error(s"Cannot check FIFO.isEmpty on Plasticine")
    case FIFOIsFull(fifo,_)  => error(s"Cannot check FIFO.isFull on Plasticine")

    case FIFOIsAlmostEmpty(fifo,_) => error(s"Cannot check FIFO.almostEmpty on Plasticine")

    case FIFOIsAlmostFull(fifo,_) => error(s"Cannot check FIFO.almostEmpty on Plasticine")

    case op@FIFOPeek(fifo,_) => error(s"Cannot perform FIFO.peek on Plasticine")
    case FIFONumel(fifo,_)   => error(s"Cannot perform FIFO.nueml on Plasticine")

    case op@FIFOBankedDeq(fifo, ens) =>
      stateAccess(lhs, fifo, ens) {
        src"MemRead()"
      }

    case FIFOBankedEnq(fifo, data, ens) =>
      stateAccess(lhs, fifo, ens, data=Some(data)) {
        src"MemWrite()"
      }

    case op@FIFORegNew(Const(init))    =>
      stateMem(lhs, "FIFO()", Some(init))

    case op@FIFORegDeq(fifo, ens) =>
      stateAccess(lhs, fifo, Seq(ens)) {
        src"MemRead()"
      }

    case FIFORegEnq(fifo, data, ens) =>
      stateAccess(lhs, fifo, Seq(ens), data=Some(Seq(data))) {
        src"MemWrite()"
      }

    case _ => super.genAccel(lhs, rhs)
  }
}
