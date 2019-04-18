package spatial.codegen.pirgen

import argon._
import spatial.util.spatialConfig
import spatial.lang._
import spatial.node._
import spatial.metadata.memory._

trait PIRGenDRAM extends PIRCodegen with PIRGenController {

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@DRAMHostNew(dims,zero) =>
      val dimStr = lhs.getConstDims.fold("") { dims =>
        s".dims($dims)"
      }
      state(lhs)(src"""DRAM("${lhs.name.getOrElse(src"$lhs")}")$dimStr.tp(${op.A})""")

    case DRAMAddress(dram) =>
      state(lhs, tp=Some("MemRead"))(src"""dramAddress($dram).name("${dram.toString}_addr").tp(${lhs.tp})""")

    case DRAMIsAlloc(dram) =>
      state(lhs)(src"Const(true).tp(Bool)") //HACK for now

    // Fringe templates expect byte-based addresses and sizes, while PIR gen expects word-based
    case e@FringeDenseLoad(dram,cmdStream,dataStream) =>
      state(lhs)(
        src"""FringeDenseLoad($dram)""" +
        src""".offset(MemRead().setMem(${Lhs(cmdStream,Some("offset"))}))""" + 
        src""".size(MemRead().setMem(${Lhs(cmdStream,Some("size"))}))""" +
        src""".data(MemWrite().setMem($dataStream).data)"""
      )

    case e@FringeDenseStore(dram,cmdStream,dataStream,ackStream) =>
      state(lhs)(
        src"""FringeDenseStore($dram)""" +
        src""".offset(MemRead().setMem(${Lhs(cmdStream,Some("offset"))}))""" + 
        src""".size(MemRead().setMem(${Lhs(cmdStream,Some("size"))}))""" +
        src""".data(MemRead().setMem(${Lhs(dataStream, Some("_1"))}))""" +
        src""".valid(MemRead().setMem(${Lhs(dataStream, Some("_2"))}))""" +
        src""".ack(MemWrite().setMem($ackStream).data)"""
      )

    case e@FringeSparseLoad(dram,addrStream,dataStream) =>
      state(lhs)(
        src"""FringeSparseLoad($dram)""" +
        src""".addr(MemRead().setMem($addrStream))""" + 
        src""".data(MemWrite().setMem($dataStream).data)"""
      )

    case e@FringeSparseStore(dram,cmdStream,ackStream) =>
      state(lhs)(
        src"""FringeSparseStore($dram)""" +
        src""".addr(MemRead().setMem(${Lhs(cmdStream,Some("addr"))}))""" + 
        src""".data(MemRead().setMem(${Lhs(cmdStream,Some("data"))}))""" +
        src""".ack(MemWrite().setMem($ackStream).data)"""
      )

    case MemDenseAlias(cond, mems, _) =>
      //open(src"val $lhs = {")
        //cond.zip(mems).zipWithIndex.foreach{case ((c,mem),idx) =>
          //if (idx == 0) emit(src"if ($c) $mem")
          //else          emit(src"else if ($c) $mem")
        //}
        //emit(src"else null.asInstanceOf[${lhs.tp}]")
      //close("}")

    case MemSparseAlias(cond, mems, _, _, _) =>
      //open(src"val $lhs = {")
      //cond.zip(mems).zipWithIndex.foreach{case ((c,mem),idx) =>
        //if (idx == 0) emit(src"if ($c) $mem")
        //else          emit(src"else if ($c) $mem")
      //}
      //emit(src"else null.asInstanceOf[${lhs.tp}]")
      //close("}")

    case _ => super.genAccel(lhs, rhs)
  }

}
