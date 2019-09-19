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
      stateRead(lhs, fifo, None, None, ens)

    case FIFOBankedEnq(fifo, data, ens) =>
      stateWrite(lhs, fifo, None, None, data, ens)

    case op@FIFORegNew(Const(init))    =>
      stateMem(lhs, "FIFO()", Some(init))

    case op@FIFORegDeq(fifo, ens) =>
      stateRead(lhs, fifo, None, None, Seq(ens))

    case FIFORegEnq(fifo, data, ens) =>
      stateWrite(lhs, fifo, None, None, Seq(data), Seq(ens))

    case _ => super.genAccel(lhs, rhs)
  }
}
