package spatial.codegen.pirgen

import argon._
import spatial.lang._
import spatial.node._
import spatial.metadata.memory._

trait PIRGenSparse extends PIRCodegen {

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@SparseSRAMNew(dims)       => 
      stateMem(lhs, "SparseMem(false)")

    case op@SparseSRAMBankedTokenRead(sram,bank,ofs,tokens,ens)       => 
      stateAccess(lhs, sram, ens) {
        src"SparseRead()" +
        src".addr(${assertOne(bank)})"
        //src".lock(${lock.map { lock => assertOne(lock) }})"
      }
    case op@SparseSRAMBankedRead(sram,bank,ofs,ens)       => 
      stateAccess(lhs, sram, ens) {
        src"SparseRead()" +
        src".addr(${assertOne(bank)})"
        //src".lock(${lock.map { lock => assertOne(lock) }})"
      }
    case op@SparseSRAMBankedTokenWrite(sram,data,bank,ofs,ens) => 
      stateAccess(lhs, sram, ens, data=Some(data)) {
        src"SparseWrite()" +
        src".addr(${assertOne(bank)})"
        //src".lock(${lock.map { lock => assertOne(lock) }})"
      }
    case op@SparseSRAMBankedWrite(sram,data,bank,ofs,ens) => 
      stateAccess(lhs, sram, ens, data=Some(data)) {
        src"SparseWrite()" +
        src".addr(${assertOne(bank)})" 
        //src".lock(${lock.map { lock => assertOne(lock) }})"
      }
    case op@SparseSRAMBankedRMW(sram,data,bank,ofs,tokens,opcode,order,ens) => 
      stateAccess(lhs, sram, ens) {
        src"""SparseRMW("$opcode","$order")""" +
        src".addr(${assertOne(bank)})" + 
        src".input(${assertOne(data)})" 
        //src".lock(${lock.map { lock => assertOne(lock) }})"
      }
    //case op@LockDRAMBankedRead(dram,bank,ofs,lock,ens)       => 
      //stateAccess(lhs, dram, ens) {
        //src"LockRead()" +
        //src".addr(${assertOne(ofs)})" + 
        ////src".lock(${lock.map { lock => assertOne(lock) }})"
      //}
    //case op@LockDRAMBankedWrite(dram,data,bank,ofs,lock,ens) => 
      //stateAccess(lhs, dram, ens, data=Some(data)) {
        //src"LockWrite()" +
        //src".addr(${assertOne(ofs)})" + 
        ////src".lock(${lock.map { lock => assertOne(lock) }})"
      //}
    case _ => super.genAccel(lhs, rhs)
  }
}


