package spatial.codegen.pirgen

import argon._
import spatial.lang._
import spatial.node._
import spatial.metadata.memory._

trait PIRGenSRAM extends PIRCodegen {

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op: SRAMNew[_,_] => 
      val accum = if (lhs.isMemReduceAccum) s".isMemReduceAccum(true)" else ""
      stateMem(lhs, src"SRAM()$accum")
    case op@SRAMBankedRead(sram,bank,ofs,ens)       => 
      stateAccess(lhs, sram, ens) {
        src"BankedRead()" +
        src".bank(${assertOne(bank)})" + 
        src".offset(${assertOne(ofs)})"
      }
    case op@SRAMBankedWrite(sram,data,bank,ofs,ens) => 
      stateAccess(lhs, sram, ens, data=Some(data)) {
        src"BankedWrite()" +
        src".bank(${assertOne(bank)})" + 
        src".offset(${assertOne(ofs)})"
      }
    case _ => super.genAccel(lhs, rhs)
  }
}
