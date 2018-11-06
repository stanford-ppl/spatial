package spatial.codegen.pirgen

import argon._
import spatial.lang._
import spatial.node._
import spatial.metadata.memory._

trait PIRGenReg extends PIRCodegen {

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@RegNew(init)    =>
      stateMem(lhs, "Reg()", Some(List(init)))

    case op@ArgInNew(init)  =>
      stateMem(lhs, s"""argIn("${lhs.name.get}")""", tp=Some("Reg"), inits=Some(List(init)))

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
      state(lhs)(s"""RegAccumOp("$op").in($in).en($ens).first($first)""")

    case RegAccumFMA(reg,m0,m1,ens,first) =>
      genOp(Lhs(lhs,Some("mul")), op=Some(s"FixMul"),inputs=Some(Seq(m0, m1)))
      state(lhs)(src"""RegAccumOp("AccumAdd").in(${Lhs(lhs, Some("mul"))}).en($ens).first($first)""")

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
