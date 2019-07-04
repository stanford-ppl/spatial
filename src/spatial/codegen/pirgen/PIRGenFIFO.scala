package spatial.codegen.pirgen

import argon._
import spatial.metadata.memory._
import spatial.lang._
import spatial.node._

import utils.implicits.collections._

trait PIRGenFIFO extends PIRCodegen {

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@FIFONew(size)    => 
      stateMem(lhs, "FIFO()")
    //case FIFOIsEmpty(fifo,_) => 
    //case FIFOIsFull(fifo,_)  => 

    //case FIFOIsAlmostEmpty(fifo,_) =>
      //val rPar = fifo.readWidths.maxOrElse(1)
      //emit(src"val $lhs = $fifo.size <= $rPar")

    //case FIFOIsAlmostFull(fifo,_) =>
      //val wPar = fifo.writeWidths.maxOrElse(1)
      //emit(src"val $lhs = $fifo.size === ${fifo.stagedSize} - $wPar")

    //case op@FIFOPeek(fifo,_) => emit(src"val $lhs = if ($fifo.nonEmpty) $fifo.head else ${invalid(op.A)}")
    //case FIFONumel(fifo,_)   => emit(src"val $lhs = $fifo.size")

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
