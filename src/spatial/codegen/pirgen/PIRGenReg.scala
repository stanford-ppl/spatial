package spatial.codegen.pirgen

import argon._
import spatial.lang._
import spatial.node._
import spatial.metadata.memory._

trait PIRGenReg extends PIRCodegen {

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@RegNew(Const(init))    =>
      stateMem(lhs, "Reg()", Some(init))

    case op@ArgInNew(Const(init))  =>
      stateMem(lhs, s"""argIn("${lhs.name.getOrElse(s"$lhs")}")""", tp=Some("Reg"), inits=Some(init))

    case op@HostIONew(Const(init))  =>
      stateMem(lhs, "argIn()", tp=Some("Reg"), inits=Some(init))

    case op@ArgOutNew(Const(init)) =>
      stateMem(lhs, "argOut()", tp=Some("Reg"), inits=Some(init))

    case RegReset(reg, ens) =>
      stateStruct(lhs, reg)(field => src"RegReset(reg=${Lhs(reg,field.map{_._1})}, ens=$ens)")

    case RegRead(reg)       => 
      stateRead(lhs, reg, None, None, Seq(Set.empty))

    case RegWrite(reg,v,ens) => 
      stateWrite(lhs, reg, None, None, Seq(v), Seq(ens))

    case RegAccumOp(reg,in,ens,op,first) =>
      state(lhs)(src"""RegAccumOp("$op").in($in).en($ens).first($first).tp(${lhs.tp})""")
      if (reg.readers.filterNot(_ == lhs).nonEmpty) { //HACK
        state(Lhs(lhs, Some("write")))(src"MemWrite().setMem($reg).en(${ens}).data($lhs).port(Some(0))")
      }

    case RegAccumFMA(reg,m0,m1,ens,first) =>
      genOp(Lhs(lhs,Some("mul")), op=Some("FixMul"),inputs=Some(Seq(m0, m1)))
      state(lhs)(src"""RegAccumOp("AccumAdd").in(${Lhs(lhs, Some("mul"))}).en($ens).first($first).tp(${lhs.tp})""")
      if (reg.readers.filterNot(_ == lhs).nonEmpty) { //HACK
        state(Lhs(lhs, Some("write")))(src"MemWrite().setMem($reg).en(${ens}).data($lhs).port(Some(0))")
      }

    case _ => super.genAccel(lhs, rhs)
  }

}
