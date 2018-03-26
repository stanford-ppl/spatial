package spatial.codegen.scalagen

import argon._
import spatial.lang._
import spatial.data._
import spatial.node._
import spatial.util._

trait ScalaGenLIFO extends ScalaGenMemories {

  override protected def remap(tp: Type[_]): String = tp match {
    case tp: LIFO[_] => src"scala.collection.mutable.Stack[${tp.A}]"
    case _ => super.remap(tp)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@LIFONew(size)    => emitMem(lhs, src"$lhs = new scala.collection.mutable.Stack[${op.A}] // size: $size")
    case LIFOIsEmpty(filo,_) => emit(src"val $lhs = $filo.isEmpty")
    case LIFOIsFull(filo)    => emit(src"val $lhs = $filo.size >= ${stagedSizeOf(filo)} ")
    case LIFOIsAlmostEmpty(filo,_) =>
      // Find rPar
      val rPar = readersOf(filo).map{ r => r.node match {
        case Def(ParLIFOPop(_,ens)) => ens.length
        case _ => 1
      }}.head
      emit(src"val $lhs = $filo.size === $rPar") 
    case LIFOIsAlmostFull(filo,_)  =>
      // Find wPar
      val wPar = writersOf(filo).map{ r => r.node match {
        case Def(ParLIFOPush(_,ens,_)) => ens.length
        case _ => 1
      }}.head
      emit(src"val $lhs = $filo.size === ${stagedSizeOf(filo)} - $wPar")

    case op@LIFOPeek(fifo,_) => emit(src"val $lhs = if (${fifo}.nonEmpty) ${fifo}.head else ${invalid(op.mT)}")
    case LIFONumel(filo,_) => emit(src"val $lhs = $filo.size")

    case op@ParLIFOPop(filo, ens) =>
      open(src"val $lhs = {")
        ens.zipWithIndex.foreach{case (en,i) =>
          emit(src"val a$i = if ($en && $filo.nonEmpty) $filo.pop() else ${invalid(op.mT)}")
        }
        emit(src"Array[${op.mT}](" + ens.indices.map{i => src"a$i"}.mkString(", ") + ")")
      close("}")

    case ParLIFOPush(filo, data, ens) =>
      open(src"val $lhs = {")
        ens.zipWithIndex.foreach{case (en,i) =>
          emit(src"if ($en) $filo.push(${data(i)})")
        }
      close("}")

    case _ => super.gen(lhs, rhs)
  }
}
