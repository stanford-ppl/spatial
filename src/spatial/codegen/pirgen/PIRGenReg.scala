package spatial.codegen.pirgen

import argon._
import spatial.lang._
import spatial.node._
import spatial.metadata.memory._

trait PIRGenReg extends PIRCodegen with PIRGenMemories {

  override protected def genHost(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@ArgInNew(init)  =>
      stateStruct(lhs, lhs.asMem.A)(name => src"ArgIn(init=$init)")
      genAccel(lhs, rhs)
    case op@HostIONew(init)  =>
      stateStruct(lhs, lhs.asMem.A)(name => src"HostIO(init=$init)")
      genAccel(lhs, rhs)
    case op@ArgOutNew(init) =>
      stateStruct(lhs, lhs.asMem.A)(name => src"ArgOut(init=$init)")
      genAccel(lhs, rhs)
    case SetReg(reg, v)  => emit(src"val $lhs = $reg.set($v)")
    case GetReg(reg)     => emit(src"val $lhs = $reg.value")
    case rhs => super.genHost(lhs, rhs)
  }

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@RegNew(init)    =>
      stateStruct(lhs, lhs.asMem.A)(name => src"Reg(init=$init)")

    case op@ArgInNew(init)  =>
      stateStruct(lhs, lhs.asMem.A)(name => src"ArgIn(init=$init)")

    case op@HostIONew(init)  =>
      stateStruct(lhs, lhs.asMem.A)(name => src"HostIO(init=$init)")

    case op@ArgOutNew(init) =>
      stateStruct(lhs, lhs.asMem.A)(name => src"ArgOut(init=$init)")

    case RegReset(reg, ens) =>
      stateStruct(lhs, reg)(name => src"RegReset(reg=${Lhs(reg,name)}, ens=$ens)")

    case RegRead(reg)       => 
      stateRead(lhs, reg, None, None, Seq(Set.empty))

    case RegWrite(reg,v,ens) => 
      stateWrite(lhs, reg, None, None, Seq(v), Seq(ens))

    case RegAccumOp(reg,in,ens,op,first) =>
      state(lhs) {
        src"RegAccumOpDef(op=$op, in=$in, first=$first, ens=$ens)"
      }
    case RegAccumFMA(reg,m0,m1,ens,first) =>
      state(lhs) {
        src"RegAccumFMADef(m0=$m0, m1=$m1, first=$first, ens=$ens)"
      }

    case _ => super.genAccel(lhs, rhs)
  }

}
