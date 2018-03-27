package spatial.codegen.scalagen

import argon._
import spatial.lang._
import spatial.data._
import spatial.node._
import spatial.util._

import utils.implicits.collections._

trait ScalaGenLIFO extends ScalaGenMemories {

  override protected def remap(tp: Type[_]): String = tp match {
    case tp: LIFO[_] => src"scala.collection.mutable.Stack[${tp.A}]"
    case _ => super.remap(tp)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@LIFONew(size)    => emitMemObject(lhs){ emit(src"object $lhs extends scala.collection.mutable.Stack[${op.A}]") }
    case LIFOIsEmpty(lifo,_) => emit(src"val $lhs = $lifo.isEmpty")
    case LIFOIsFull(lifo,_)  => emit(src"val $lhs = $lifo.size >= ${sizeOf(lifo)} ")
    case LIFOIsAlmostEmpty(lifo,_) =>
      val rPar = readWidths(lifo).maxOrElse(1)
      emit(src"val $lhs = $lifo.size === $rPar")
    case LIFOIsAlmostFull(lifo,_) =>
      val wPar = writeWidths(lifo).maxOrElse(1)
      emit(src"val $lhs = $lifo.size === ${sizeOf(lifo)} - $wPar")

    case op@LIFOPeek(lifo,_) => emit(src"val $lhs = if ($lifo.nonEmpty) $lifo.head else ${invalid(op.A)}")
    case LIFONumel(lifo,_) => emit(src"val $lhs = $lifo.size")

    case op@LIFOBankedPop(lifo, ens) =>
      open(src"val $lhs = {")
      ens.zipWithIndex.foreach{case (en,i) =>
        emit(src"val a$i = if ($en && $lifo.nonEmpty) $lifo.pop() else ${invalid(op.A)}")
      }
      emit(src"Array[${op.A}](" + ens.indices.map{i => src"a$i"}.mkString(", ") + ")")
      close("}")

    case LIFOBankedPush(lifo, data, ens) =>
      open(src"val $lhs = {")
      ens.zipWithIndex.foreach{case (en,i) =>
        emit(src"if ($en) $lifo.push(${data(i)})")
      }
      close("}")

    case _ => super.gen(lhs, rhs)
  }
}
