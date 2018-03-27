package spatial.codegen.scalagen

import argon._
import spatial.internal.spatialConfig
import spatial.lang._
import spatial.node._

trait ScalaGenDRAM extends ScalaGenMemories {

  override protected def remap(tp: Type[_]): String = tp match {
    case tp: DRAM[_,_] => src"Array[${tp.A}]"
    case _ => super.remap(tp)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@DRAMNew(dims,zero) =>
      emitMemObject(lhs){
        val name = s""""${lhs.fullname}""""
        emit(src"object $lhs extends Memory[${op.A}]($name, $zero)")
      }
      val elementsPerBurst = spatialConfig.target.burstSize / op.A.nbits
      val size = src"""${dims.map(quote).mkString("*")} + $elementsPerBurst"""
      emit(src"$lhs.initMem($size)")

    case GetDRAMAddress(dram) =>
      emit(src"val $lhs = 0")

    case op@SetMem(dram, data) =>
      open(src"val $lhs = {")
      open(src"for (i <- 0 until $data.length) {")
      oobUpdate(op.A,dram,lhs,Nil){ oobApply(op.A,data,lhs,Nil){ emit(src"$dram(i) = $data(i)") } }
      close("}")
      close("}")

    case op@GetMem(dram, data) =>
      open(src"val $lhs = {")
      open(src"for (i <- 0 until $data.length) {")
      oobUpdate(op.A,data,lhs,Nil){ oobApply(op.A,dram,lhs,Nil){ emit(src"$data(i) = $dram(i)") } }
      close("}")
      close("}")

    // Fringe templates expect byte-based addresses and sizes, while Scala gen expects word-based
    case e@FringeDenseLoad(dram,cmdStream,dataStream) =>
      val bytesPerWord = e.A.nbits / 8 + (if (e.A.nbits % 8 != 0) 1 else 0)
      open(src"val $lhs = $cmdStream.foreach{cmd => ")
        open(src"for (i <- cmd.offset until cmd.offset+cmd.size by $bytesPerWord) {")
          open(src"val data = {")
            oobApply(e.A, dram, lhs, Nil){ emit(src"$dram.apply(i / $bytesPerWord)") }
          close("}")
          emit(src"$dataStream.enqueue(data)")
        close("}")
      close("}")
      emit(src"$cmdStream.clear()")

    case e@FringeDenseStore(dram,cmdStream,dataStream,ackStream) =>
      val bytesPerWord = e.A.nbits / 8 + (if (e.A.nbits % 8 != 0) 1 else 0)
      open(src"val $lhs = $cmdStream.foreach{cmd => ")
        open(src"for (i <- cmd.offset until cmd.offset+cmd.size by $bytesPerWord) {")
          emit(src"val data = $dataStream.dequeue()")
          oobUpdate(e.A, dram, lhs, Nil){ emit(src"if (data._2) $dram(i / $bytesPerWord) = data._1") }
        close("}")
        emit(src"$ackStream.enqueue(true)")
      close("}")
      emit(src"$cmdStream.clear()")

    case e@FringeSparseLoad(dram,addrStream,dataStream) =>
      val bytesPerWord = e.A.nbits / 8 + (if (e.A.nbits % 8 != 0) 1 else 0)
      open(src"val $lhs = $addrStream.foreach{addr => ")
        open(src"val data = {")
          oobApply(e.A, dram, lhs, Nil){ emit(src"$dram(addr / $bytesPerWord)") }
        close("}")
        emit(src"$dataStream.enqueue(data)")
      close("}")
      emit(src"$addrStream.clear()")

    case e@FringeSparseStore(dram,cmdStream,ackStream) =>
      val bytesPerWord = e.A.nbits / 8 + (if (e.A.nbits % 8 != 0) 1 else 0)
      open(src"val $lhs = $cmdStream.foreach{cmd => ")
        oobUpdate(e.A, dram, lhs, Nil){ emit(src"$dram(cmd._2 / $bytesPerWord) = cmd._1 ") }
        emit(src"$ackStream.enqueue(true)")
      close("}")
      emit(src"$cmdStream.clear()")


    case _ => super.gen(lhs, rhs)
  }

}
