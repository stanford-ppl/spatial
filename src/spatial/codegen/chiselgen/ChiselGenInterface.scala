package spatial.codegen.chiselgen

import argon._
import spatial.lang._
import spatial.node._

import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.retiming._

trait ChiselGenInterface extends ChiselGenCommon {

  var loadsList = List[Sym[_]]()
  var storesList = List[Sym[_]]()
  var loadParMapping = List[String]()
  var storeParMapping = List[String]()

  var gathersList = List[Sym[_]]()
  var scattersList = List[Sym[_]]()
  var gatherParMapping = List[String]()
  var scatterParMapping = List[String]()

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case InputArguments() =>
    case ArgInNew(init)  => 
      argIns += (lhs -> argIns.toList.length)
    case HostIONew(init)  => 
      inAccel{
        inGen(out, "ArgInterface.scala") {
          forceEmit(src"object ${lhs} {")
          forceEmit(src"  val data_options = Wire(Vec(${scala.math.max(1,lhs.writers.size)}, UInt(64.W)))")
          forceEmit(src"  val en_options = Wire(Vec(${scala.math.max(1,lhs.writers.size)}, Bool()))")
          forceEmit(src"}")
        }
        argIOs += (lhs -> argIOs.toList.length)
        forceEmit(src"""top.io.argOuts(${argIOs(lhs)}).bits := chisel3.util.Mux1H(${lhs}.en_options, ${lhs}.data_options)""")
        forceEmit(src"""top.io.argOuts(${argIOs(lhs)}).valid := ${lhs}.en_options.reduce{_|_}""")
      }
    case ArgOutNew(init) => 
      inAccel{
        inGen(out, "ArgInterface.scala") {
          forceEmit(src"object ${lhs} {")
          forceEmit(src"  val data_options = Wire(Vec(${scala.math.max(1,lhs.writers.size)}, UInt(64.W)))")
          forceEmit(src"  val en_options = Wire(Vec(${scala.math.max(1,lhs.writers.size)}, Bool()))")
          forceEmit(src"}")
        }
        argOuts += (lhs -> argOuts.toList.length)
        forceEmit(src"""top.io.argOuts(top.io_numArgIOs_reg + ${argOuts(lhs)}).bits := chisel3.util.Mux1H(${lhs}.en_options, ${lhs}.data_options)""")
        forceEmit(src"""top.io.argOuts(top.io_numArgIOs_reg + ${argOuts(lhs)}).valid := ${lhs}.en_options.reduce{_|_}""")
      }

    // case GetReg(reg) if (reg.isArgOut) =>
    //   argOutLoopbacks.getOrElseUpdate(argOuts(reg), argOutLoopbacks.toList.length)
    //   // emitGlobalWireMap(src"""${lhs}""",src"Wire(${newWire(reg.tp.typeArgs.head)})")
    //   emit(src"""${lhs}.r := io.argOutLoopbacks(${argOutLoopbacks(argOuts(reg))})""")

    case GetReg(reg) if reg.isHostIO =>
      emit(src"""val ${lhs} = Wire(${lhs.tp})""")
      val id = argHandle(reg)
      emit(src"""${lhs}.r := top.io.argIns(api.${id}_arg)""")

    case RegRead(reg)  if reg.isArgIn =>
      emit(src"""val ${lhs} = Wire(${lhs.tp})""")
      val id = argHandle(reg)
      emit(src"""${lhs}.r := top.io.argIns(api.${id}_arg)""")

    case RegRead(reg)  if reg.isHostIO =>
      emit(src"""val ${lhs} = Wire(${lhs.tp})""")
      val id = argHandle(reg)
      emit(src"""${lhs}.r := top.io.argIns(api.${id}_arg)""")

    case RegRead(reg)  if reg.isArgOut =>
      argOutLoopbacks.getOrElseUpdate(argOuts(reg), argOutLoopbacks.toList.length)
      emit(src"""val ${lhs} = Wire(${reg.tp.typeArgs.head})""")
      emit(src"""${lhs}.r := top.io.argOutLoopbacks(${argOutLoopbacks(argOuts(reg))})""")


    case RegWrite(reg, v, en) if reg.isHostIO =>
      val id = lhs.port.muxPort
      emit(src"val $id = $id")
      v.tp match {
        case FixPtType(s,d,f) =>
          if (s) {
            val pad = 64 - d - f
            if (pad > 0) {
              emit(src"""${reg}.data_options($id) := util.Cat(util.Fill($pad, ${v}.msb), ${v}.r)""")
            } else {
              emit(src"""${reg}.data_options($id) := ${v}.r""")
            }
          } else {
            emit(src"""${reg}.data_options($id) := ${v}.r""")
          }
        case _ =>
          emit(src"""${reg}.data_options($id) := ${v}.r""")
      }
      val enStr = if (en.isEmpty) "true.B" else en.map(quote).mkString(" & ")
      emit(src"""${reg}.en_options($id) := ${enStr} & ${DL(src"${controllerStack.head}.datapathEn & ${controllerStack.head}.iiDone", lhs.fullDelay)}""")

    case RegWrite(reg, v, en) if reg.isArgOut =>
      val id = lhs.port.muxPort
      emit(src"val $id = $id")
      val padded = v.tp match {
        case FixPtType(s,d,f) if s && (64 > d + f) =>
          src"util.Cat(util.Fill(${64 - d - f}, $v.msb), $v.r)"
        case _ => src"$v.r"
      }
      emit(src"""${reg}.data_options($id) := $padded""")

      val enStr = if (en.isEmpty) "true.B" else en.map(quote).mkString(" & ")
      emit(src"""${reg}.en_options($id) := ${enStr} & ${DL(src"${controllerStack.head}.datapathEn & ${controllerStack.head}.iiDone", lhs.fullDelay)}""")

    case FringeDenseLoad(dram,cmdStream,dataStream) =>
      appPropertyStats += HasTileLoad
      if (cmdStream.isAligned) appPropertyStats += HasAlignedLoad
      else appPropertyStats += HasUnalignedLoad
      val par = dataStream.readers.head match { case Op(e@StreamInBankedRead(strm, ens)) => ens.length }

      val id = loadsList.length
      loadParMapping = loadParMapping :+ s"""StreamParInfo(${bitWidth(dram.tp.typeArgs.head)}, ${par}, 0)"""
      loadsList = loadsList :+ dram

      emit(src"${cmdStream}.ready := top.io.memStreams.loads($id).cmd.ready // Not sure why the cmdStream ready used to be delayed")
      val (addrMSB, addrLSB)  = getField(cmdStream.tp.typeArgs.head, "offset")
      val (sizeMSB, sizeLSB)  = getField(cmdStream.tp.typeArgs.head, "size")
      val (isLdMSB, isLdLSB)  = getField(cmdStream.tp.typeArgs.head, "isLoad")
      emit(src"top.io.memStreams.loads($id).cmd.bits.addr := ${cmdStream}.m(0)($addrMSB,$addrLSB)")
      emit(src"top.io.memStreams.loads($id).cmd.bits.size := ${cmdStream}.m(0)($sizeMSB,$sizeLSB)")
      emit(src"top.io.memStreams.loads($id).cmd.valid :=  ${cmdStream}.valid & ${cmdStream}.ready")

      // Connect the streams to their IO interface signals
      emit(src"top.io.memStreams.loads($id).rdata.ready := ${dataStream}.ready")
      emit(src"""${dataStream}.m.zip(top.io.memStreams.loads($id).rdata.bits).foreach{case (a,b) => a.r := ${DL("b", src"${dataStream.readers.head.fullDelay}.toInt")}}""")
      emit(src"""${dataStream}.now_valid := top.io.memStreams.loads($id).rdata.valid""")
      emit(src"""${dataStream}.valid := ${DL(src"${dataStream}.now_valid", src"${dataStream.readers.head.fullDelay}.toInt", true)}""")


    case FringeSparseLoad(dram,cmdStream,dataStream) =>
      appPropertyStats += HasGather
      val par = dataStream.readers.head match { case Op(e@StreamInBankedRead(strm, ens)) => ens.length }

      val id = gathersList.length
      gatherParMapping = gatherParMapping :+ s"""StreamParInfo(${bitWidth(dram.tp.typeArgs.head)}, ${par}, 0)"""
      gathersList = gathersList :+ dram

      emit(src"${cmdStream}.ready := top.io.memStreams.gathers($id).cmd.ready // Not sure why the cmdStream ready used to be delayed")
      emit(src"top.io.memStreams.gathers($id).cmd.bits.addr.zip(${cmdStream}.m).foreach{case (a,b) => a := b.r}")
      emit(src"top.io.memStreams.gathers($id).cmd.valid :=  ${cmdStream}.valid & ${cmdStream}.ready")

      // Connect the streams to their IO interface signals
      emit(src"top.io.memStreams.gathers($id).rdata.ready := ${dataStream}.ready")
      emit(src"""${dataStream}.m.zip(top.io.memStreams.gathers($id).rdata.bits).foreach{case (a,b) => a.r := ${DL("b", src"${dataStream.readers.head.fullDelay}.toInt")}}""")
      emit(src"""${dataStream}.now_valid := top.io.memStreams.gathers($id).rdata.valid""")
      emit(src"""${dataStream}.valid := ${DL(src"${dataStream}.now_valid", src"${dataStream.readers.head.fullDelay}.toInt", true)}""")

    case FringeDenseStore(dram,cmdStream,dataStream,ackStream) =>
      appPropertyStats += HasTileStore
      if (cmdStream.isAligned) appPropertyStats += HasAlignedStore
      else appPropertyStats += HasUnalignedStore

      // Get parallelization of datastream
      val par = dataStream.writers.head match { case Op(e@StreamOutBankedWrite(_, _, ens)) => ens.length }

      val id = storesList.length
      storeParMapping = storeParMapping :+ s"""StreamParInfo(${bitWidth(dram.tp.typeArgs.head)}, ${par}, 0)"""
      storesList = storesList :+ dram

      // Connect streams to their IO interface signals
      emit(src"""${dataStream}.ready := top.io.memStreams.stores($id).wdata.ready""")

      // Connect IO interface signals to their streams
      val (dataMSB, dataLSB) = getField(dataStream.tp.typeArgs.head, "_1")
      val (strbMSB, strbLSB) = getField(dataStream.tp.typeArgs.head, "_2")
      val (addrMSB, addrLSB)  = getField(cmdStream.tp.typeArgs.head, "offset")
      val (sizeMSB, sizeLSB)  = getField(cmdStream.tp.typeArgs.head, "size")
      val (isLdMSB, isLdLSB)  = getField(cmdStream.tp.typeArgs.head, "isLoad")

      emit(src"""top.io.memStreams.stores($id).wdata.bits.zip(${dataStream}.m).foreach{case (wport, wdata) => wport := wdata($dataMSB,$dataLSB) }""")
      emit(src"""top.io.memStreams.stores($id).wstrb.bits := ${dataStream}.m.map{ _.apply($strbMSB,$strbLSB) }.reduce(Cat(_,_)) """)
      emit(src"""top.io.memStreams.stores($id).wdata.valid := ${dataStream}.valid """)

      emit(src"top.io.memStreams.stores($id).cmd.bits.addr := ${cmdStream}.m(0)($addrMSB,$addrLSB)")
      emit(src"top.io.memStreams.stores($id).cmd.bits.size := ${cmdStream}.m(0)($sizeMSB,$sizeLSB)")
      emit(src"top.io.memStreams.stores($id).cmd.valid :=  ${cmdStream}.valid & ${cmdStream}.ready")

      emit(src"${cmdStream}.ready := top.io.memStreams.stores($id).cmd.ready")
      emit(src"""${ackStream}.now_valid := top.io.memStreams.stores($id).wresp.valid""")
      emit(src"""${ackStream}.valid := ${DL(src"${ackStream}.now_valid", src"${ackStream.readers.head.fullDelay}.toInt", true)}""")
      emit(src"""top.io.memStreams.stores($id).wresp.ready := ${ackStream}.ready""")

    case FringeSparseStore(dram,cmdStream,ackStream) =>
      appPropertyStats += HasScatter
      // Get parallelization of datastream
      val par = cmdStream.writers.head match { case Op(e@StreamOutBankedWrite(_, _, ens)) => ens.length }

      val id = scattersList.length
      scatterParMapping = scatterParMapping :+ s"""StreamParInfo(${bitWidth(dram.tp.typeArgs.head)}, ${par}, 0)"""
      scattersList = scattersList :+ dram

      // Connect IO interface signals to their streams
      val (dataMSB, dataLSB)  = getField(cmdStream.tp.typeArgs.head, "_1")
      val (addrMSB, addrLSB)  = getField(cmdStream.tp.typeArgs.head, "_2")

      emit(src"top.io.memStreams.scatters($id).wdata.bits.zip(${cmdStream}.m).foreach{case (wport, wdata) => wport := wdata($dataMSB, $dataLSB)}")
      emit(src"top.io.memStreams.scatters($id).wdata.valid := ${cmdStream}.valid")
      emit(src"top.io.memStreams.scatters($id).cmd.bits.addr.zip(${cmdStream}.m).foreach{case (a,b) => a := b($addrMSB, $addrLSB)}")
      emit(src"top.io.memStreams.scatters($id).cmd.valid :=  ${cmdStream}.valid & ${cmdStream}.ready")
      emit(src"${cmdStream}.ready := top.io.memStreams.scatters($id).cmd.ready & top.io.memStreams.scatters($id).wdata.ready")
      emit(src"""${ackStream}.now_valid := top.io.memStreams.scatters($id).wresp.valid""")
      emit(src"""${ackStream}.valid := ${DL(src"${ackStream}.now_valid", src"${ackStream.readers.head.fullDelay}.toInt", true)}""")
      emit(src"""top.io.memStreams.scatters($id).wresp.ready := ${ackStream}.ready""")

    case _ => super.gen(lhs, rhs)
  }

  override def emitPostMain(): Unit = {
    inGen(out, "Instantiator.scala") {
      emit ("")
      emit ("// Scalars")
      emit (s"val numArgIns_reg = ${argIns.toList.length}")
      emit (s"val numArgOuts_reg = ${argOuts.toList.length}")
      emit (s"val numArgIOs_reg = ${argIOs.toList.length}")
      argIns.zipWithIndex.foreach { case(p,i) => emit(s"""//${quote(p._1)} = argIns($i) ( ${p._1.name.getOrElse("")} )""") }
      argOuts.zipWithIndex.foreach { case(p,i) => emit(s"""//${quote(p._1)} = argOuts($i) ( ${p._1.name.getOrElse("")} )""") }
      argIOs.zipWithIndex.foreach { case(p,i) => emit(s"""//${quote(p._1)} = argIOs($i) ( ${p._1.name.getOrElse("")} )""") }
      emit (s"val io_argOutLoopbacksMap: scala.collection.immutable.Map[Int,Int] = ${argOutLoopbacks}")
      emit ("")
      emit (s"// Memory streams")
      emit (src"""val loadStreamInfo = List(${loadParMapping.map(_.replace("FringeGlobals.",""))}) """)
      emit (src"""val storeStreamInfo = List(${storeParMapping.map(_.replace("FringeGlobals.",""))}) """)
      emit (src"""val gatherStreamInfo = List(${gatherParMapping.map(_.replace("FringeGlobals.",""))}) """)
      emit (src"""val scatterStreamInfo = List(${scatterParMapping.map(_.replace("FringeGlobals.",""))}) """)
      emit (src"""val numArgIns_mem = ${hostDrams.toList.length}""")
      emit (src"""// $loadsList $storesList $gathersList $scattersList)""")
    }

    inGen(out, s"IOModule.$ext") {
      emit ("// Scalars")
      emit (s"val io_numArgIns_reg = ${argIns.toList.length}")
      emit (s"val io_numArgOuts_reg = ${argOuts.toList.length}")
      emit (s"val io_numArgIOs_reg = ${argIOs.toList.length}")
      emit (s"val io_argOutLoopbacksMap: scala.collection.immutable.Map[Int,Int] = ${argOutLoopbacks}")
      emit ("// Memory Streams")
      emit (src"""val io_loadStreamInfo = List($loadParMapping) """)
      emit (src"""val io_storeStreamInfo = List($storeParMapping) """)
      emit (src"""val io_gatherStreamInfo = List($gatherParMapping) """)
      emit (src"""val io_scatterStreamInfo = List($scatterParMapping) """)
      emit (src"val io_numArgIns_mem = ${hostDrams.toList.length}")
      emit (src"val outArgMuxMap: scala.collection.mutable.Map[Int, Int] = scala.collection.mutable.Map[Int,Int]()")

    }

    inGen(out, "ArgAPI.scala") {
      emit("package accel")
      open("object api {")
      emit("\n// ArgIns")
      argIns.foreach{case (a, id) => emit(src"val ${argHandle(a)}_arg = $id")}
      emit("\n// DRAM Ptrs:")
      hostDrams.foreach {case (d, id) => emit(src"val ${argHandle(d)}_ptr = ${id+argIns.toList.length}")}
      emit("\n// ArgIOs")
      argIOs.foreach{case (a, id) => emit(src"val ${argHandle(a)}_arg = ${id+argIns.toList.length+hostDrams.toList.length}")}
      emit("\n// ArgOuts")
      argOuts.foreach{case (a, id) => emit(src"val ${argHandle(a)}_arg = ${id+argIns.toList.length+hostDrams.toList.length+argIOs.toList.length}")}
      emit("\n// Instrumentation Counters")
      instrumentCounters.zipWithIndex.foreach{case ((s,_), i) => 
        emit(src"val ${quote(s).toUpperCase}_cycles_arg = ${argIOs.toList.length + argOuts.toList.length + 2*i}")
        emit(src"val ${quote(s).toUpperCase}_iters_arg = ${argIOs.toList.length + argOuts.toList.length + 2*i + 1}")
      }
      earlyExits.foreach{x => 
        emit(src"val ${quote(x).toUpperCase}_exit_arg = ${argOuts.toList.length + argIOs.toList.length + instrumentCounters.toList.length}")
      }
      close("}")
    }
    super.emitPostMain()
  }

}
