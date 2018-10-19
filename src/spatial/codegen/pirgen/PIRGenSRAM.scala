package spatial.codegen.pirgen

import argon._
import spatial.lang._
import spatial.node._
import spatial.metadata.memory._

trait PIRGenSRAM extends PIRCodegen {

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op: SRAMNew[_,_] => 
      stateMem(lhs, "SRAM", None)
    case op@SRAMBankedRead(sram,bank,ofs,ens)       => 
      stateRead(lhs, sram, Some(bank), Some(ofs), ens)
    case op@SRAMBankedWrite(sram,data,bank,ofs,ens) => 
      stateWrite(lhs, sram, Some(bank), Some(ofs), data, ens)
    case _ => super.genAccel(lhs, rhs)
  }
}
