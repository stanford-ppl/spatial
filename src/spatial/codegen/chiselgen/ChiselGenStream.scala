package spatial.codegen.chiselgen

import argon._
import spatial.lang._
import spatial.node._
import spatial.metadata.access._
import spatial.metadata.memory._
import spatial.metadata.control._
import spatial.metadata.retiming._

import scala.collection.mutable

trait ChiselGenStream extends ChiselGenCommon {
  var axiStreamIns = mutable.HashMap[Sym[_], (Int,Int)]()
  var axiStreamOuts = mutable.HashMap[Sym[_], (Int,Int)]()

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case StreamInNew(bus) =>
      bus match {
        case AxiStream256Bus => forceEmit(src"val $lhs = accelUnit.io.axiStreamsIn(${axiStreamIns.size})"); axiStreamIns += (lhs -> (axiStreamIns.size, 256))
        case AxiStream512Bus => forceEmit(src"val $lhs = accelUnit.io.axiStreamsIn(${axiStreamIns.size})"); axiStreamIns += (lhs -> (axiStreamIns.size, 512))
        case _ =>
      }

    case StreamOutNew(bus) =>
      bus match {
        case AxiStream256Bus => forceEmit(src"val $lhs = accelUnit.io.axiStreamsOut(${axiStreamOuts.size})"); axiStreamOuts += (lhs -> (axiStreamOuts.size, 256))
        case AxiStream512Bus => forceEmit(src"val $lhs = accelUnit.io.axiStreamsOut(${axiStreamOuts.size})"); axiStreamOuts += (lhs -> (axiStreamOuts.size, 512))
        case _ =>
      }

    case StreamOutBankedWrite(stream, data, ens) =>
      val muxPort = lhs.port.muxPort
      val base = stream.writers.filter(_.port.muxPort < muxPort).map(_.accessWidth).sum
      val parent = lhs.parent.s.get
      val sfx = if (parent.isBranch) "_obj" else ""
      val maskingLogic = src"$backpressure" 
      ens.zipWithIndex.foreach{case(e,i) =>
        val en = if (e.isEmpty) "true.B" else src"${e.toList.map(quote).mkString("&")}"
        emit(src"""${stream}.valid := ${DL(src"$datapathEn & $iiDone", src"${lhs.fullDelay}.toInt", true)} & $en & $maskingLogic""")
      }
      val Op(StreamOutNew(bus)) = stream
      bus match {
        case BurstCmdBus => 
          val (addrMSB, addrLSB)  = getField(stream.tp.typeArgs.head, "offset")
          val (sizeMSB, sizeLSB)  = getField(stream.tp.typeArgs.head, "size")
          emit(src"$stream.bits.addr := $data($addrMSB,$addrLSB)")
          emit(src"$stream.bits.size := $data($sizeMSB,$sizeLSB)")

        case _: BurstFullDataBus[_] => 
          val (dataMSB, dataLSB) = getField(stream.tp.typeArgs.head, "_1")
          val (strbMSB, strbLSB) = getField(stream.tp.typeArgs.head, "_2")

          if (ens.size == 1) {
            emit(src"$stream.bits.wdata(0) := $data($dataMSB,$dataLSB)")
            emit(src"$stream.bits.wstrb := $data($strbMSB,$strbLSB)")            
          } else {
            data.zipWithIndex.foreach{case (d,i) => 
              emit(src"$stream.bits.wdata($i) := $d($dataMSB,$dataLSB)")
            }
              
            emit(src"$stream.bits.wstrb := List($data).map{_($strbMSB,$strbLSB)}.reduce(Cat(_,_))")
          }

        case GatherAddrBus => 
          data.zipWithIndex.foreach{case (d,i) => 
            emit(src"$stream.bits.addr($i) := $d.r")
          }
          
        
        case _: ScatterCmdBus[_] => 
          val (dataMSB, dataLSB)  = getField(stream.tp.typeArgs.head, "_1")
          val (addrMSB, addrLSB)  = getField(stream.tp.typeArgs.head, "_2")
          data.zipWithIndex.foreach{case (d,i) => 
            emit(src"$stream.bits.addr.addr($i) := $d($addrMSB, $addrLSB)")
            emit(src"$stream.bits.wdata($i) := $d($dataMSB, $dataLSB)")
          }

        case AxiStream256Bus if data.head.tp.isInstanceOf[AxiStream256] =>
          emit(src"$stream.TDATA.r := ${data.head}.TDATA.r")
          emit(src"$stream.TSTRB.r := ${data.head}.TSTRB.r")
          emit(src"$stream.TKEEP.r := ${data.head}.TKEEP.r")
          emit(src"$stream.TID.r := ${data.head}.TID.r")
          emit(src"$stream.TDEST.r := ${data.head}.TDEST.r")
          emit(src"$stream.TLAST := ${data.head}.TLAST")
          emit(src"$stream.TUSER := ${data.head}.TUSER")
        case AxiStream256Bus => // If Stream was not declared as AxiStream type, assume user only cares about the tdata
          emit(src"$stream.TDATA.r := ${data.head}.r")
          emit(src"$stream.TSTRB.r := ~0.U(32.W)")
          emit(src"$stream.TKEEP.r := ~0.U(32.W)")
          emit(src"$stream.TID.r := 0.U")
          emit(src"$stream.TDEST.r := 0.U")
          emit(src"$stream.TLAST := 0.U")
          emit(src"$stream.TUSER.r := 4.U")
        case AxiStream512Bus if data.head.tp.isInstanceOf[AxiStream512] =>
          emit(src"$stream.TDATA.r := ${data.head}.r(511,0)")
          emit(src"$stream.TSTRB.r := ${data.head}.r(575,512)")
          emit(src"$stream.TKEEP.r := ${data.head}.r(639,576)")
          emit(src"$stream.TID.r := ${data.head}.r(648,641)")
          emit(src"$stream.TDEST.r := ${data.head}.r(657,649)")
          emit(src"$stream.TLAST := ${data.head}.r(640)")
          emit(src"$stream.TUSER := ${data.head}.r(720,658)")
        case AxiStream512Bus => // If Stream was not declared as AxiStream type, assume user only cares about the tdata
          emit(src"$stream.TDATA.r := ${data.head}.r")
          emit(src"$stream.TSTRB.r := ~0.U(64.W)")
          emit(src"$stream.TKEEP.r := ~0.U(64.W)")
          emit(src"$stream.TID.r := 0.U")
          emit(src"$stream.TDEST.r := 0.U")
          emit(src"$stream.TLAST := 0.U")
          emit(src"$stream.TUSER.r := 4.U")
        case _ =>
          data.zipWithIndex.foreach{case(d,i) =>
            emit(src"""${stream}.bits := ${d}.r""")
          }
      }


    case StreamInBankedRead(strm, ens) =>
      val muxPort = lhs.port.muxPort
      val base = strm.readers.filter(_.port.muxPort < muxPort).map(_.accessWidth).sum
      val parent = lhs.parent.s.get
      val sfx = if (parent.isBranch) "_obj" else ""
      emit(createWire(quote(lhs),remap(lhs.tp)))
      emit(src"""${strm}.ready := ${and(ens.flatten.toSet)} & ($datapathEn) """)
      val Op(StreamInNew(bus)) = strm
      bus match {
        case _: BurstDataBus[_] => emit(src"""(0 until ${ens.length}).map{ i => ${lhs}(i).r := ${strm}.bits.rdata(i).r }""")
        case _: GatherDataBus[_] => emit(src"(0 until ${ens.length}).map{ i => ${lhs}(i).r := ${strm}.bits(i).r }")
        case AxiStream256Bus if lhs.tp.isInstanceOf[AxiStream256] =>
          emit(src"(0 until ${ens.length}).map{ i => ${lhs}(i).TDATA.r := ${strm}.TDATA.r }")
          emit(src"(0 until ${ens.length}).map{ i => ${lhs}(i).TSTRB.r := ${strm}.TSTRB.r }")
          emit(src"(0 until ${ens.length}).map{ i => ${lhs}(i).TKEEP.r := ${strm}.TKEEP.r }")
          emit(src"(0 until ${ens.length}).map{ i => ${lhs}(i).TID.r := ${strm}.TID.r }")
          emit(src"(0 until ${ens.length}).map{ i => ${lhs}(i).TDEST.r := ${strm}.TDEST.r }")
          emit(src"(0 until ${ens.length}).map{ i => ${lhs}(i).TLAST := ${strm}.TLAST }")
          emit(src"(0 until ${ens.length}).map{ i => ${lhs}(i).TUSER.r := ${strm}.TUSER.r }")
        case AxiStream256Bus => // If Stream was not declared as AxiStream type, assume user only cares about the tdata
          emit(src"(0 until ${ens.length}).map{ i => ${lhs}(i).r := ${strm}.TDATA.r }")
        case AxiStream512Bus if lhs.tp.isInstanceOf[AxiStream512] =>
          emit(src"(0 until ${ens.length}).map{ i => ${lhs}(i).TDATA.r := ${strm}.TDATA.r }")
          emit(src"(0 until ${ens.length}).map{ i => ${lhs}(i).TSTRB.r := ${strm}.TSTRB.r }")
          emit(src"(0 until ${ens.length}).map{ i => ${lhs}(i).TKEEP.r := ${strm}.TKEEP.r }")
          emit(src"(0 until ${ens.length}).map{ i => ${lhs}(i).TID.r := ${strm}.TID.r }")
          emit(src"(0 until ${ens.length}).map{ i => ${lhs}(i).TDEST.r := ${strm}.TDEST.r }")
          emit(src"(0 until ${ens.length}).map{ i => ${lhs}(i).TLAST := ${strm}.TLAST }")
          emit(src"(0 until ${ens.length}).map{ i => ${lhs}(i).TUSER.r := ${strm}.TUSER.r }")
        case AxiStream512Bus => // If Stream was not declared as AxiStream type, assume user only cares about the tdata
          emit(src"(0 until ${ens.length}).map{ i => ${lhs}(i).r := ${strm}.TDATA.r }")



        // case ScatterAckBus => emit(src"""(0 until ${ens.length}).map{ i => ${lhs}(i) := ${strm}.bits }""")
        // case BurstAckBus => emit(src"""(0 until ${ens.length}).map{ i => ${lhs}(i) := ${strm}.bits }""")
        case _ => emit(src"""(0 until ${ens.length}).map{ i => ${lhs}(i) := ${strm}.bits }""")

      }
      

    case _ => super.gen(lhs, rhs)
  }

  override def emitPostMain(): Unit = {
    val insList = axiStreamIns.map{case (s, (_,w)) => s"AXI4StreamParameters($w, 8, 32)" }.mkString(",")
    val outsList = axiStreamOuts.map{case (s, (_,w)) => s"AXI4StreamParameters($w, 8, 32)" }.mkString(",")

    inGen(out, s"AccelWrapper.$ext") {
      emit(src"// Non-memory Streams")
      emit(s"""val io_axiStreamInsInfo = List(${insList})""")
      emit(s"""val io_axiStreamOutsInfo = List(${outsList})""")
    }

    inGen(out, "Instantiator.scala") {
      emit(src"// Non-memory Streams")
      emit(s"""val axiStreamInsInfo = List(${insList})""")
      emit(s"""val axiStreamOutsInfo = List(${outsList})""")
    }
    super.emitPostMain()
  }
}
