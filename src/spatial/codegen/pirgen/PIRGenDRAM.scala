package spatial.codegen.pirgen

import argon._
import spatial.util.spatialConfig
import spatial.lang._
import spatial.node._

trait PIRGenDRAM extends PIRCodegen with PIRGenController {

  override def emitAccelHeader = {
    super.emitAccelHeader
    emit("""
    def dramAddress(dram:DRAM) = {
      val mem = Reg()
      within(argFringe, hostInCtrl) {
        MemWrite().setMem(mem).data(hostWrite) // DRAMDef
      }
      MemRead().setMem(mem)
    }
""")
  }

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@DRAMHostNew(dims,zero) =>
      state(lhs)(s"""DRAM()""")

    case DRAMAddress(dram) =>
      state(lhs, tp=Some("Reg"))(src"""dramAddress($dram).name("${dram.toString}_addr")""")

    case DRAMIsAlloc(dram) =>
      state(lhs)(src"Const(true)") //HACK for now

    // Fringe templates expect byte-based addresses and sizes, while PIR gen expects word-based
    case e@FringeDenseLoad(dram,cmdStream,dataStream) =>
      emitController(Lhs(lhs, Some("ctrl")), ctrler=Some("DramController()"), schedule=Some("Streaming")) {
        state(lhs)(
          src"""FringeDenseLoad($dram)""" +
          src""".offset(MemRead().setMem(${Lhs(cmdStream,Some("offset"))}))""" + 
          src""".size(MemRead().setMem(${Lhs(cmdStream,Some("size"))}))""" +
          src""".data(MemWrite().setMem($dataStream).data)"""
        )
      }

    case e@FringeDenseStore(dram,cmdStream,dataStream,ackStream) =>
      emitController(Lhs(lhs, Some("ctrl")), ctrler=Some("DramController()"), schedule=Some("Streaming")) {
        state(lhs)(
          src"""FringeDenseStore($dram)""" +
          src""".offset(MemRead().setMem(${Lhs(cmdStream,Some("offset"))}))""" + 
          src""".size(MemRead().setMem(${Lhs(cmdStream,Some("size"))}))""" +
          src""".data(MemRead().setMem(${Lhs(dataStream, Some("_1"))}))""" +
          src""".valid(MemRead().setMem(${Lhs(dataStream, Some("_2"))}))""" +
          src""".ack(MemWrite().setMem($ackStream).data)"""
        )
      }

    case e@FringeSparseLoad(dram,addrStream,dataStream) =>
      emitController(Lhs(lhs, Some("ctrl")), ctrler=Some("DramController()"), schedule=Some("Streaming")) {
        state(lhs)(
          src"""FringeSparseLoad($dram)""" +
          src""".addr(MemRead().setMem($addrStream))""" + 
          src""".data(MemWrite().setMem($dataStream).data)"""
        )
      }

    case e@FringeSparseStore(dram,cmdStream,ackStream) =>
      emitController(Lhs(lhs, Some("ctrl")), ctrler=Some("DramController()"), schedule=Some("Streaming")) {
        state(lhs)(
          src"""FringeDenseStore($dram)""" +
          src""".addr(MemRead().setMem(${Lhs(cmdStream,Some("addr"))}))""" + 
          src""".data(MemRead().setMem(${Lhs(cmdStream,Some("data"))}))""" +
          src""".ack(MemWrite().setMem($ackStream).data)"""
        )
      }

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
    case op@DRAMHostNew(dims,zero) =>
      super.genHost(lhs, rhs)
      genInAccel(lhs, rhs)

    //case op@SetMem(dram, data) =>
      //open(src"val $lhs = {")
      //open(src"for (i <- 0 until $data.length) {")
      //oobUpdate(op.A,dram,lhs,Nil){ oobApply(op.A,data,lhs,Nil){ emit(src"$dram(i) = $data(i)") } }
      //close("}")
      //close("}")

    //case op@GetMem(dram, data) =>
      //open(src"val $lhs = {")
      //open(src"for (i <- 0 until $data.length) {")
      //oobUpdate(op.A,data,lhs,Nil){ oobApply(op.A,dram,lhs,Nil){ emit(src"$data(i) = $dram(i)") } }
      //close("}")
      //close("}")

    // Fringe templates expect byte-based addresses and sizes, while PIR gen expects word-based

    case _ => super.genHost(lhs, rhs)
  }

}
