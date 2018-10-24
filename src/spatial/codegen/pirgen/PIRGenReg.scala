package spatial.codegen.pirgen

import argon._
import spatial.lang._
import spatial.node._
import spatial.metadata.memory._

trait PIRGenReg extends PIRCodegen {

  override def emitAccelHeader = {
    super.emitAccelHeader
    emit("""
    def argIn() = {
      within(argFringe, hostInCtrl) {
        val mem = Reg()
        MemWrite().mem(mem)
        mem
      }
    }
    def argOut() = {
      within(argFringe, hostOutCtrl) {
        val mem = Reg()
        MemRead().mem(mem)
        mem
      }
    }
""")
  }

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@RegNew(init)    =>
      stateMem(lhs, "Reg()", Some(List(init)))

    case op@ArgInNew(init)  =>
      stateMem(lhs, "argIn()", tp=Some("Reg"), inits=Some(List(init)))

    case op@HostIONew(init)  =>
      stateMem(lhs, "argIn()", tp=Some("Reg"), inits=Some(List(init)))

    case op@ArgOutNew(init) =>
      stateMem(lhs, "argOut()", tp=Some("Reg"), inits=Some(List(init)))

    case RegReset(reg, ens) =>
      stateStruct(lhs, reg)(name => src"RegReset(reg=${Lhs(reg,name)}, ens=$ens)")

    case RegRead(reg)       => 
      stateRead(lhs, reg, None, None, Seq(Set.empty))

    case RegWrite(reg,v,ens) => 
      stateWrite(lhs, reg, None, None, Seq(v), Seq(ens))

    case RegAccumOp(reg,in,ens,op,first) =>
      genOp(lhs, op=Some(s"RegAccumOp_$op"),inputs=Some(Seq(in, first, ens)))

    case RegAccumFMA(reg,m0,m1,ens,first) =>
      genOp(lhs, inputs=Some(Seq(m0,m1,first,ens)))

    case _ => super.genAccel(lhs, rhs)
  }

  override protected def genHost(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@ArgInNew(init)  =>
      super.genHost(lhs, rhs)
      genInAccel(lhs, rhs)

    case op@ArgOutNew(init) =>
      super.genHost(lhs, rhs)
      genInAccel(lhs, rhs)

    case _ => super.genHost(lhs, rhs)
  }

}
