package spatial.codegen.pirgen

import argon._
import spatial.lang._
import spatial.node._

trait PIRGenLUTs extends PIRCodegen {

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@LUTNew(_,elems) => 
      stateMem(lhs, "LUT", Some(elems))
    case op@LUTBankedRead(lut,bank,ofs,ens) => 
      stateRead(lhs, lut, Some(bank), Some(ofs), ens)
    case _ => super.genAccel(lhs, rhs)
  }

}
