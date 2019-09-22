package spatial.codegen.pirgen

import argon._
import spatial.metadata.memory._
import spatial.lang._
import spatial.node._
import spatial.metadata.access._
import spatial.metadata.control._


import utils.implicits.collections._

trait PIRGenLineBuffer extends PIRCodegen {

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@LineBufferNew(rows, cols, stride) => error(s"Plasticine doesn't support LineBuffer")
    case op@LineBufferBankedEnq(lb, data, row, ens) => error(s"Plasticine doesn't support LineBuffer")
    case op@LineBufferBankedRead(lb, bank, ofs, ens) => error(s"Plasticine doesn't support LineBuffer")

    case _ => super.genAccel(lhs, rhs)
  }


}

