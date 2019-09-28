package spatial.codegen.pirgen

import argon._
import spatial.lang._
import spatial.node._

trait PIRGenRegFile extends PIRCodegen {

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@RegFileNew(_, inits) => 
      stateMem(lhs, "RegFile()", inits.map { _.map { case Const(c) => c } })

    //case RegFileReset(rf, en)    => 
      //emit(src"val $lhs = if ($en) $rf.reset()")

    //case RegFileShiftIn(rf,data,addr,en,axis) =>
      //val ctx = s""""${lhs.ctx}""""
      //emit(src"val $lhs = if (${and(en)}) $rf.shiftIn($ctx, Seq($addr), $axis, $data)")

    //case RegFileShiftInVector(rf,data,addr,en,axis) =>
      //val ctx = s""""${lhs.ctx}""""
      //emit(src"val $lhs = if (${and(en)}) $rf.shiftInVec($ctx, Seq($addr), $axis, $data)")

    case op@RegFileVectorRead(rf,addr,ens)       => 
      stateAccess(lhs, rf, ens) {
        src"BankedRead()" +
        src".bank(${assertOne(addr)})" + 
        src".offset(${assertOne(addr.map(_.map { _ => "Const(0)" }))})"
      }

    case op@RegFileVectorWrite(rf,data,addr,ens) => 
      stateAccess(lhs, rf, ens, data=Some(data)) {
        src"BankedWrite()" +
        src".bank(${assertOne(addr)})" + 
        src".offset(${addr.map(_.map { _ => "Const(0)" })})"
      }

    //case RegFileBankedShiftIn(rf,data,addr,en,axis) =>
      //val ctx = s""""${lhs.ctx}""""
      //(data,addr,en).zipped.foreach{(d,a,e) => 
        //emit(src"val $lhs = if (${and(e)}) $rf.shiftIn($ctx, Seq($a), $axis, $d)")
      //}

    case _ => super.genAccel(lhs, rhs)
  }

}
