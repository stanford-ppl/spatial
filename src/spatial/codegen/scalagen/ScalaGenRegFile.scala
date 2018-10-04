package spatial.codegen.scalagen

import argon._
import spatial.lang._
import spatial.node._

trait ScalaGenRegFile extends ScalaGenMemories {

  override protected def remap(tp: Type[_]): String = tp match {
    case tp: RegFile[_,_] => src"Array[${tp.A}]"
    case _ => super.remap(tp)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@RegFileNew(_, inits) => emitBankedInitMem(lhs, inits, op.A)
    case RegFileReset(rf, en)    => emit(src"val $lhs = if (${and(en)}) $rf.reset()")
    case RegFileShiftIn(rf,data,addr,en,axis) =>
      val ctx = s""""${lhs.ctx}""""
      emit(src"val $lhs = if (${and(en)}) $rf.shiftIn($ctx, Seq($addr), $axis, $data)")

    case RegFileShiftInVector(rf,data,addr,en,axis) =>
      val ctx = s""""${lhs.ctx}""""
      emit(src"val $lhs = if (${and(en)}) $rf.shiftInVec($ctx, Seq($addr), $axis, $data)")

    case op@RegFileVectorRead(rf,addr,ens)       => emitVectorLoad(lhs,rf,addr,ens)(op.A)
    case op@RegFileVectorWrite(rf,data,addr,ens) => emitVectorStore(lhs,rf,data,addr,ens)(op.A)

    case RegFileBankedShiftIn(rf,data,addr,en,axis) =>
      val ctx = s""""${lhs.ctx}""""
      (data,addr,en).zipped.foreach{(d,a,e) => 
        emit(src"val $lhs = if (${and(e)}) $rf.shiftIn($ctx, Seq($a), $axis, $d)")
      }

    case _ => super.gen(lhs, rhs)
  }

}
