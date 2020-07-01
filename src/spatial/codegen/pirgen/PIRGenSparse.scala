package spatial.codegen.pirgen

import argon._
import spatial.lang._
import spatial.node._
import spatial.metadata.memory._
import spatial.metadata.control._

trait PIRGenSparse extends PIRCodegen {

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@SparseSRAMNew(dims)       => 
      stateMem(lhs, src"""SparseMem("SRAM", false)""")

    case op@SparseParSRAMNew(dims, par)       => 
      stateMem(lhs, src"""SparseMem("ParSRAM", $par)""")

    case op@SparseDRAMNew(dims, par)       => 
      stateMem(lhs, src"""SparseMem("ParDRAM", $par).alias.update(${lhs.alias})""")

    case op@SparseSRAMBankedRead(sram,bank,ofs,barriers,ens)       => 
      stateAccess(lhs, sram, ens) {
        src"SparseRead()" +
        src".addr(${assertOne(bank)})" +
        src".barriers($barriers)"
      }
    case op@SparseSRAMBankedWrite(sram,data,bank,ofs,barriers,ens) => 
      stateAccess(lhs, sram, ens, data=Some(data)) {
        src"SparseWrite()" +
        src".addr(${assertOne(bank)})" +
        src".barriers($barriers)"
      }
    case op@SparseSRAMBankedRMW(sram,data,bank,ofs,opcode,order,barriers,remoteAddr,ens) => 
      stateAccess(lhs, sram, ens) {
        src"""SparseRMW("$opcode","$order",$remoteAddr)""" +
        src".addr(${assertOne(bank)})" + 
        src".input(${assertOne(data)})" +
        src".barriers($barriers)"
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
    case op@BarrierNew(init) => 
      state(lhs)(src"""Barrier(stackTop[Ctrl].get.asInstanceOf[ControlTree],$init).srcCtx("${lhs.ctx}").name("${lhs.name}")""")
    case op@BarrierPush(barrier) =>
      state(lhs, tp=Some("(Barrier, Boolean)"))(src"($barrier,true)")
    case op@BarrierPop(barrier) =>
      state(lhs, tp=Some("(Barrier, Boolean)"))(src"($barrier,false)")
    case _ => super.genAccel(lhs, rhs)
  }
}
