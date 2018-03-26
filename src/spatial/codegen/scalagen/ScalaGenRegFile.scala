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

  private def shiftIn(lhs: Sym[_], rf: Sym[_], inds: Seq[Idx], d: Int, data: Sym[_], isVec: Boolean, en: Bit): Unit = {
    val len = if (isVec) lenOf(data) else 1
    val dims = stagedDimsOf(rf)
    val size = dims(d)
    val stride = (dims.drop(d+1).map(quote) :+ "1").mkString("*")

    open(src"val $lhs = if ($en) {")
      emit(src"val ofs = ${flattenAddress(dims,inds,None)}")
      emit(src"val stride = $stride")
      open(src"for (j <- $size-1 to 0 by - 1) {")
        if (isVec) emit(src"if (j < $len) $rf.update(ofs+($len-1-j)*stride, $data(j)) else $rf.update(ofs + j*stride, $rf.apply(ofs + (j - $len)*stride))")
        else       emit(src"if (j < $len) $rf.update(ofs+j*stride, $data) else $rf.update(ofs + j*stride, $rf.apply(ofs + (j - $len)*stride))")
      close("}")
    close("}")
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@RegFileNew(dims, inits) => 
      if (inits.isDefined) {
        val initString = src"List(${inits.get})"
        emit(src"val ${lhs}_values = $initString")
        emitMem(lhs, src"$lhs = Array.tabulate(${dims.map(quote).mkString("*")})(i => ${lhs}_values(i))")
      }
      else {
        emitMem(lhs, src"$lhs = Array.fill(${dims.map(quote).mkString("*")})(${invalid(op.A)})")
      }

    case RegFileReset(rf, en) => 
      val dims = stagedDimsOf(rf)
      val inits = rf match {
        case Def(RegFileNew(_, inits)) => inits
      }
      if (inits.isDefined) {
        val initString = src"List(${inits.get})"
        emit(src"val ${lhs}_values = ${initString} ")
        emit(src"val $lhs = if ($en) {for (${lhs}_addr <- 0 until ${dims.map(quote).mkString{"*"}} ) {$rf.update(${lhs}_addr, ${lhs}_values(${lhs}_addr))} }")
      } else {
        emit(src"val $lhs = if ($en) {for (${lhs}_addr <- 0 until ${dims.map(quote).mkString{"*"}} ) {$rf.update(${lhs}_addr, 0)} }")
      }

    case RegFileShiftIn(rf,i,d,data,en)    => shiftIn(lhs, rf, i, d, data, isVec = false, en)
    case ParRegFileShiftIn(rf,i,d,data,en) => shiftIn(lhs, rf, i, d, data, isVec = true, en)

    case op@ParRegFileStore(rf,inds,data,ens) =>
      val dims = stagedDimsOf(rf)
      open(src"val $lhs = {")
        ens.zipWithIndex.foreach{case (en,i) =>
          oobUpdate(op.mT,rf,lhs,inds(i)) { emit(src"if ($en) $rf.update(${flattenAddress(dims,inds(i),None)}, ${data(i)})") }
        }
      close("}")

    case op@ParRegFileLoad(rf,inds,ens) =>
      val dims = stagedDimsOf(rf)
      open(src"val $lhs = {")
        ens.zipWithIndex.foreach{case (en,i) =>
          open(src"val a$i = {")
            oobApply(op.mT,rf,lhs,inds(i)){ emit(src"if ($en) $rf.apply(${flattenAddress(dims,inds(i),None)}) else ${invalid(op.mT)}") }
          close("}")
        }
        emit(src"Array[${op.mT}](" + ens.indices.map{i => src"a$i"}.mkString(", ") + ")")
      close("}")


    case _ => super.gen(lhs, rhs)
  }

}
