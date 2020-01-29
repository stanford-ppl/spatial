package spatial.codegen.pirgen

import argon._
import spatial.lang._
import spatial.node._
import spatial.metadata.memory._

trait PIRGenLock extends PIRCodegen {

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case LockNew(depth) => 
      state(lhs)(
        src"""Lock()"""
      )
    case LockOnKeys(lock, keys) =>
      state(lhs) {
        src"""LockOnKeys().key(${assertOne(keys)}).lock(${lock})"""
      }
    case op: LockSRAMNew[_,_] => 
      stateMem(lhs, "LockMem(false)")
    case op: LockDRAMHostNew[_,_] => 
      stateMem(lhs, src"LockMem(true)")
    case op@LockSRAMBankedRead(sram,bank,ofs,lock,ens)       => 
      stateAccess(lhs, sram, ens) {
        src"LockRead()" +
        src".addr(${assertOne(bank)})" + 
        src".lock(${lock.map { lock => assertOne(lock) }})"
      }
    case op@LockSRAMBankedWrite(sram,data,bank,ofs,lock,ens) => 
      stateAccess(lhs, sram, ens, data=Some(data)) {
        src"LockWrite()" +
        src".addr(${assertOne(bank)})" + 
        src".lock(${lock.map { lock => assertOne(lock) }})"
      }
    case op@LockDRAMBankedRead(dram,bank,ofs,lock,ens)       => 
      stateAccess(lhs, dram, ens) {
        src"LockRead()" +
        src".addr(${assertOne(ofs)})" + 
        src".lock(${lock.map { lock => assertOne(lock) }})"
      }
    case op@LockDRAMBankedWrite(dram,data,bank,ofs,lock,ens) => 
      stateAccess(lhs, dram, ens, data=Some(data)) {
        src"LockWrite()" +
        src".addr(${assertOne(ofs)})" + 
        src".lock(${lock.map { lock => assertOne(lock) }})"
      }
    case _ => super.genAccel(lhs, rhs)
  }
}

