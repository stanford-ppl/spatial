package spatial.codegen.pirgen

import argon._
import spatial.lang._
import spatial.node._

trait PIRGenLUTs extends PIRCodegen {

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@FileLUTNew(_,filename) => 
      stateMem(lhs, "LUT()", inits=Some(quoteString(s"file:${filename}")))
    case op@LUTNew(_,elems) => 
      stateMem(lhs, "LUT()", inits=Some(elems.map { case Const(c) => c }))
    case op@LUTBankedRead(lut,bank,ofs,ens) => 
      stateAccess(lhs, lut, ens) {
        src"BankedRead()" +
        src".bank(${assertOne(bank)})" + 
        src".offset(${assertOne(ofs)})"
      }
    case _ => super.genAccel(lhs, rhs)
  }

}
