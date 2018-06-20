package spatial.codegen.scalagen

import argon._
import spatial.lang._
import spatial.node._
import spatial.util._

trait ScalaGenRegFile extends ScalaGenMemories {

  override protected def remap(tp: Type[_]): String = tp match {
    case tp: RegFile[_,_] => src"Array[${tp.A}]"
    case _ => super.remap(tp)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@RegFileNew(_, inits) => emitBankedInitMem(lhs, inits, op.A)
    case RegFileReset(rf, en)    => emit(src"val $lhs = if ($en) $rf.reset()")
    case RegFileShiftIn(rf,data,addr,en,axis) =>
      val ctx = s""""${lhs.ctx}""""
      emit(src"val $lhs = if (${and(en)}) $rf.shiftIn($ctx, Seq($addr), $axis, $data)")

    case RegFileBankedShiftIn(rf,data,addr,en,axis) =>
      val ctx = s""""${lhs.ctx}""""
      (data,addr,en).zipped.foreach{(d,a,e) => 
        emit(src"val $lhs = if (${and(e)}) $rf.shiftIn($ctx, Seq($a), $axis, $d)")
      }

//    case RegFileVectorShiftIn(rf,data,addr,en,axis,len) =>
//      val ctx = s""""${lhs.ctx}""""
//      emit(src"val $lhs = if ($en) $rf.shiftInVec($ctx, Seq($addr), $axis, $data)")

    case op@RegFileBankedRead(rf,bank,ofs,ens)       => emitBankedLoad(lhs,rf,bank,ofs,ens)(op.A)
    case op@RegFileBankedWrite(rf,data,bank,ofs,ens) => emitBankedStore(lhs,rf,data,bank,ofs,ens)(op.A)
    case _ => super.gen(lhs, rhs)
  }

}
