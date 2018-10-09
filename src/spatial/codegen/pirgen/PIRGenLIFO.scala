package spatial.codegen.pirgen

import argon._
import spatial.lang._
import spatial.metadata.memory._
import spatial.node._

import utils.implicits.collections._

trait PIRGenLIFO extends PIRGenMemories {

  override protected def remap(tp: Type[_]): String = tp match {
    case tp: LIFO[_] => src"scala.collection.mutable.Stack[${tp.A}]"
    case _ => super.remap(tp)
  }

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@LIFONew(size)    => emitMemObject(lhs){ emit(src"object $lhs extends scala.collection.mutable.Stack[${op.A}]") }
    case LIFOIsEmpty(lifo,_) => emit(src"val $lhs = $lifo.isEmpty")
    case LIFOIsFull(lifo,_)  => emit(src"val $lhs = $lifo.size >= ${lifo.stagedSize} ")
    case LIFOIsAlmostEmpty(lifo,_) =>
      val rPar = lifo.readWidths.maxOrElse(1)
      emit(src"val $lhs = $lifo.size === $rPar")
    case LIFOIsAlmostFull(lifo,_) =>
      val wPar = lifo.writeWidths.maxOrElse(1)
      emit(src"val $lhs = $lifo.size === ${lifo.stagedSize} - $wPar")

    case op@LIFOPeek(lifo,_) => emit(src"val $lhs = if ($lifo.nonEmpty) $lifo.head else ${invalid(op.A)}")
    case LIFONumel(lifo,_) => emit(src"val $lhs = $lifo.size")

    case op@LIFOBankedPop(lifo, ens) =>
      open(src"val $lhs = {")
      ens.zipWithIndex.foreach{case (en,i) =>
        emit(src"val a$i = if (${and(en)} && $lifo.nonEmpty) $lifo.pop() else ${invalid(op.A)}")
      }
      emit(src"Array[${op.A}](" + ens.indices.map{i => src"a$i"}.mkString(", ") + ")")
      close("}")

    case LIFOBankedPush(lifo, data, ens) =>
      open(src"val $lhs = {")
      ens.zipWithIndex.foreach{case (en,i) =>
        emit(src"if (${and(en)}) $lifo.push(${data(i)})")
      }
      close("}")

    case _ => super.genAccel(lhs, rhs)
  }
}
