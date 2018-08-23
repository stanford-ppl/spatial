package spatial.codegen.pirgen

import argon._
import spatial.metadata.memory._
import spatial.lang._
import spatial.node._

import utils.implicits.collections._

trait PIRGenFIFO extends PIRGenMemories {

  override protected def remap(tp: Type[_]): String = tp match {
    case tp: FIFO[_] => src"scala.collection.mutable.Queue[${tp.A}]"
    case _ => super.remap(tp)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@FIFONew(size)    => emitMemObject(lhs){ emit(src"object $lhs extends scala.collection.mutable.Queue[${op.A}]") }
    case FIFOIsEmpty(fifo,_) => emit(src"val $lhs = $fifo.isEmpty")
    case FIFOIsFull(fifo,_)  => emit(src"val $lhs = $fifo.size >= ${fifo.stagedSize} ")

    case FIFOIsAlmostEmpty(fifo,_) =>
      val rPar = fifo.readWidths.maxOrElse(1)
      emit(src"val $lhs = $fifo.size <= $rPar")

    case FIFOIsAlmostFull(fifo,_) =>
      val wPar = fifo.writeWidths.maxOrElse(1)
      emit(src"val $lhs = $fifo.size === ${fifo.stagedSize} - $wPar")

    case op@FIFOPeek(fifo,_) => emit(src"val $lhs = if ($fifo.nonEmpty) $fifo.head else ${invalid(op.A)}")
    case FIFONumel(fifo,_)   => emit(src"val $lhs = $fifo.size")

    case op@FIFOBankedDeq(fifo, ens) =>
      open(src"val $lhs = {")
      ens.zipWithIndex.foreach{case (en,i) =>
        emit(src"val a$i = if (${and(en)} && $fifo.nonEmpty) $fifo.dequeue() else ${invalid(op.A)}")
      }
      emit(src"Array[${op.A}](" + ens.indices.map{i => src"a$i"}.mkString(", ") + ")")
      close("}")

    case FIFOBankedEnq(fifo, data, ens) =>
      open(src"val $lhs = {")
      ens.zipWithIndex.foreach{case (en,i) => emit(src"if (${and(en)}) $fifo.enqueue(${data(i)})") }
      close("}")

    case _ => super.gen(lhs, rhs)
  }
}
