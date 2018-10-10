package spatial.codegen.pirgen

import argon._
import spatial.lang._
import spatial.node._
import spatial.metadata.memory._

trait PIRGenStream extends PIRCodegen {

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@StreamInNew(bus)  =>
      stateMem(lhs, "StreamIn", None)

    case op@StreamOutNew(bus) =>
      stateMem(lhs, "StreamOut", None)

    case op@StreamInBankedRead(strm, ens) =>
      stateRead(lhs, strm, None, None, ens)

    case StreamOutBankedWrite(strm, data, ens) =>
      stateWrite(lhs, strm, None, None, data, ens)

    case _ => super.genAccel(lhs, rhs)
  }

}
