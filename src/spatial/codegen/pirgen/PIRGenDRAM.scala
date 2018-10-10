package spatial.codegen.pirgen

import argon._
import spatial.util.spatialConfig
import spatial.lang._
import spatial.node._

trait PIRGenDRAM extends PIRCodegen {

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@DRAMNew(dims,zero) =>
      state(lhs)(s"""DRAM()""")

    case GetDRAMAddress(dram) =>
      state(lhs)(src"DRAMAddress($dram)")

    // Fringe templates expect byte-based addresses and sizes, while PIR gen expects word-based
    case e@FringeDenseLoad(dram,cmdStream,dataStream) =>
      state(lhs)(
        src"""FringeDenseLoad($dram, offset=$cmdStream("offset"), size=$cmdStream("size"), data=$dataStream)"""
      )

    case e@FringeDenseStore(dram,cmdStream,dataStream,ackStream) =>
      state(lhs)(
        src"""FringeDenseLoad($dram, offset=$cmdStream("offset"), size=$cmdStream("size"), data=$dataStream, ack=$ackStream)"""
      )

    case e@FringeSparseLoad(dram,addrStream,dataStream) =>
      state(lhs)(
        src"""FringeSparseLoad($dram, addr=$addrStream, data=$dataStream)"""
      )

    case e@FringeSparseStore(dram,cmdStream,ackStream) =>
      state(lhs)(
        src"""FringeSparseStore($dram, data=$cmdStream("data"), addr=$cmdStream("addr"), ack=$ackStream)"""
      )

    case MemDenseAlias(cond, mems, _) =>
      //open(src"val $lhs = {")
        //cond.zip(mems).zipWithIndex.foreach{case ((c,mem),idx) =>
          //if (idx == 0) emit(src"if ($c) $mem")
          //else          emit(src"else if ($c) $mem")
        //}
        //emit(src"else null.asInstanceOf[${lhs.tp}]")
      //close("}")

    case MemSparseAlias(cond, mems, _, _) =>
      //open(src"val $lhs = {")
      //cond.zip(mems).zipWithIndex.foreach{case ((c,mem),idx) =>
        //if (idx == 0) emit(src"if ($c) $mem")
        //else          emit(src"else if ($c) $mem")
      //}
      //emit(src"else null.asInstanceOf[${lhs.tp}]")
      //close("}")

    case _ => super.genAccel(lhs, rhs)
  }

  override protected def genHost(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@DRAMNew(dims,zero) =>
      genAccel(lhs, rhs)

    case op@SetMem(dram, data) =>
      //open(src"val $lhs = {")
      //open(src"for (i <- 0 until $data.length) {")
      //oobUpdate(op.A,dram,lhs,Nil){ oobApply(op.A,data,lhs,Nil){ emit(src"$dram(i) = $data(i)") } }
      //close("}")
      //close("}")

    case op@GetMem(dram, data) =>
      //open(src"val $lhs = {")
      //open(src"for (i <- 0 until $data.length) {")
      //oobUpdate(op.A,data,lhs,Nil){ oobApply(op.A,dram,lhs,Nil){ emit(src"$data(i) = $dram(i)") } }
      //close("}")
      //close("}")

    // Fringe templates expect byte-based addresses and sizes, while PIR gen expects word-based

    case _ => super.genHost(lhs, rhs)
  }

}
