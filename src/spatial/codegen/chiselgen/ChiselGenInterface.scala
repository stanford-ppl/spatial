package spatial.codegen.chiselgen

import argon._
import argon.codegen.Codegen
import spatial.lang._
import spatial.node._
import spatial.internal.{spatialConfig => cfg}
import spatial.data._
import spatial.util._


trait ChiselGenInterface extends ChiselGenCommon {

  var loadsList = List[Sym[_]]()
  var storesList = List[Sym[_]]()
  var loadParMapping = List[String]()
  var storeParMapping = List[String]()

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case InputArguments() =>
    case ArgInNew(init)  => 
      argIns += (lhs -> argIns.toList.length)
    case HostIONew(init)  => 
      inAccel{
        emitGlobalWireMap(src"${swap(lhs, DataOptions)}", src"Wire(Vec(${scala.math.max(1,lhs.writers.size)}, UInt(64.W)))", forceful=true)
        emitGlobalWireMap(src"${swap(lhs, EnOptions)}", src"Wire(Vec(${scala.math.max(1,lhs.writers.size)}, Bool()))", forceful=true)
        argIOs += (lhs -> argIOs.toList.length)
        emitt(src"""io.argOuts(${argIOs(lhs)}).bits := chisel3.util.Mux1H(${swap(lhs, EnOptions)}, ${swap(lhs, DataOptions)}) // ${lhs.name.getOrElse("")}""", forceful=true)
        emitt(src"""io.argOuts(${argIOs(lhs)}).valid := ${swap(lhs, EnOptions)}.reduce{_|_}""", forceful=true)
      }
    case ArgOutNew(init) => 
      inAccel{
        emitGlobalWireMap(src"${swap(lhs, DataOptions)}", src"Wire(Vec(${scala.math.max(1,lhs.writers.size)}, UInt(64.W)))", forceful=true)
        emitGlobalWireMap(src"${swap(lhs, EnOptions)}", src"Wire(Vec(${scala.math.max(1,lhs.writers.size)}, Bool()))", forceful=true)
        argOuts += (lhs -> argOuts.toList.length)
        emitt(src"""io.argOuts(io_numArgIOs_reg + ${argOuts(lhs)}).bits := chisel3.util.Mux1H(${swap(lhs, EnOptions)}, ${swap(lhs, DataOptions)}) // ${lhs.name.getOrElse("")}""", forceful=true)
        emitt(src"""io.argOuts(io_numArgIOs_reg + ${argOuts(lhs)}).valid := ${swap(lhs, EnOptions)}.reduce{_|_}""", forceful=true)
      }

    // case GetReg(reg) if (reg.isArgOut) =>
    //   argOutLoopbacks.getOrElseUpdate(argOuts(reg), argOutLoopbacks.toList.length)
    //   // emitGlobalWireMap(src"""${lhs}""",src"Wire(${newWire(reg.tp.typeArgs.head)})")
    //   emitt(src"""${lhs}.r := io.argOutLoopbacks(${argOutLoopbacks(argOuts(reg))})""")

    case GetReg(reg) if reg.isHostIO =>
      emitGlobalWireMap(src"""${lhs}""",src"Wire(${lhs.tp})")
      val id = argHandle(reg)
      emitGlobalWire(src"""${lhs}.r := io.argIns(api.${id}_arg)""")

    case RegRead(reg)  if reg.isArgIn =>
      emitGlobalWireMap(src"""${lhs}""",src"Wire(${lhs.tp})")
      val id = argHandle(reg)
      emitGlobalWire(src"""${lhs}.r := io.argIns(api.${id}_arg)""")

    case RegRead(reg)  if reg.isHostIO =>
      emitGlobalWireMap(src"""${lhs}""",src"Wire(${lhs.tp})")
      val id = argHandle(reg)
      emitGlobalWire(src"""${lhs}.r := io.argIns(api.${id}_arg)""")

    case RegRead(reg)  if reg.isArgOut =>
      argOutLoopbacks.getOrElseUpdate(argOuts(reg), argOutLoopbacks.toList.length)
      emitGlobalWireMap(src"""${lhs}""",src"Wire(${reg.tp.typeArgs.head})")
      emitt(src"""${lhs}.r := io.argOutLoopbacks(${argOutLoopbacks(argOuts(reg))})""")


    case RegWrite(reg, v, en) if reg.isHostIO =>
      val id = lhs.ports(0).values.head.muxPort
      emitt(src"val $id = $id")
      v.tp match {
        case FixPtType(s,d,f) =>
          if (s) {
            val pad = 64 - d - f
            if (pad > 0) {
              emitt(src"""${swap(reg, DataOptions)}($id) := util.Cat(util.Fill($pad, ${v}.msb), ${v}.r)""")
            } else {
              emitt(src"""${swap(reg, DataOptions)}($id) := ${v}.r""")
            }
          } else {
            emitt(src"""${swap(reg, DataOptions)}($id) := ${v}.r""")
          }
        case _ =>
          emitt(src"""${swap(reg, DataOptions)}($id) := ${v}.r""")
      }
      val enStr = if (en.isEmpty) "true.B" else en.map(quote).mkString(" & ")
      emitt(src"""${swap(reg, EnOptions)}($id) := ${enStr} & ${DL(src"${swap(controllerStack.head, DatapathEn)} & ${swap(controllerStack.head, IIDone)}", lhs.fullDelay)}""")

    case RegWrite(reg, v, en) if reg.isArgOut =>
      val id = lhs.ports(0).values.head.muxPort
      emitt(src"val $id = $id")
      val padded = v.tp match {
        case FixPtType(s,d,f) if s && (64 > d + f) =>
          src"util.Cat(util.Fill(${64 - d - f}, $v.msb), $v.r)"
        case _ => src"$v.r"
      }
      emitt(src"""${swap(reg, DataOptions)}($id) := $padded""")

      val enStr = if (en.isEmpty) "true.B" else en.map(quote).mkString(" & ")
      emitt(src"""${swap(reg, EnOptions)}($id) := ${enStr} & ${DL(src"${swap(controllerStack.head, DatapathEn)} & ${swap(controllerStack.head, IIDone)}", lhs.fullDelay)}""")

    case DRAMNew(dims, _) => 
      drams += (lhs -> drams.toList.length)

    case GetDRAMAddress(dram) =>
      val id = argHandle(dram)
      emit(src"""val $lhs = io.argIns(api.${id}_ptr)""")

    case FringeDenseLoad(dram,cmdStream,dataStream) =>
      appPropertyStats += HasTileLoad
      if (cmdStream.isAligned) appPropertyStats += HasAlignedLoad
      else appPropertyStats += HasUnalignedLoad
      val par = dataStream.readers.head match { case Op(e@StreamInBankedRead(strm, ens)) => ens.length }

      val id = loadsList.length
      loadParMapping = loadParMapping :+ s"""StreamParInfo(${bitWidth(dram.tp.typeArgs.head)}, ${par}, 0, false)"""
      loadsList = loadsList :+ dram

      emit(src"${swap(cmdStream, Ready)} := io.memStreams.loads($id).cmd.ready // Not sure why the cmdStream ready used to be delayed")
      val (addrMSB, addrLSB)  = getField(cmdStream.tp.typeArgs.head, "offset")
      val (sizeMSB, sizeLSB)  = getField(cmdStream.tp.typeArgs.head, "size")
      val (isLdMSB, isLdLSB)  = getField(cmdStream.tp.typeArgs.head, "isLoad")
      emit(src"io.memStreams.loads($id).cmd.bits.addr := ${cmdStream}(0)($addrMSB,$addrLSB)")
      emit(src"io.memStreams.loads($id).cmd.bits.size := ${cmdStream}(0)($sizeMSB,$sizeLSB)")
      emit(src"io.memStreams.loads($id).cmd.valid :=  ${swap(cmdStream, Valid)}")
      emit(src"io.memStreams.loads($id).cmd.bits.isWr := ~${cmdStream}(0)($isLdMSB,$isLdLSB)")
      emit(src"io.memStreams.loads($id).cmd.bits.isSparse := 0.U")

      // Connect the streams to their IO interface signals
      emit(src"io.memStreams.loads($id).rdata.ready := ${swap(dataStream, Ready)}")
      emit(src"""${dataStream}.zip(io.memStreams.loads($id).rdata.bits).foreach{case (a,b) => a.r := ${DL("b", src"${dataStream.readers.head.fullDelay}.toInt")}}""")
      emit(src"""${swap(dataStream, NowValid)} := io.memStreams.loads($id).rdata.valid""")
      emit(src"""${swap(dataStream, Valid)} := ${DL(swap(dataStream, NowValid), src"${dataStream.readers.head.fullDelay}.toInt", true)}""")


    case FringeSparseLoad(dram,cmdStream,dataStream) =>
      appPropertyStats += HasGather
      val par = dataStream.readers.head match { case Op(e@StreamInBankedRead(strm, ens)) => ens.length }
      assert(par == 1, s"Unsupported par '$par' for sparse loads! Must be 1 currently")

      val id = loadsList.length
      // loadParMapping = loadParMapping :+ s"""StreamParInfo(if (FringeGlobals.target == "zcu") 32 else ${bitWidth(dram.tp.typeArgs.head)}, ${par}, ${transferChannel(parentOf(lhs).get)}, true)"""
      loadParMapping = loadParMapping :+ s"""StreamParInfo(${bitWidth(dram.tp.typeArgs.head)}, ${par}, 0, true)"""
      loadsList = loadsList :+ dram

      emit(src"${swap(cmdStream, Ready)} := io.memStreams.loads($id).cmd.ready // Not sure why the cmdStream ready used to be delayed")
      emit(src"io.memStreams.loads($id).cmd.bits.addr := ${cmdStream}(0).r")
      emit(src"io.memStreams.loads($id).cmd.bits.size := 1.U")
      emit(src"io.memStreams.loads($id).cmd.valid :=  ${swap(cmdStream, Valid)}")
      emit(src"io.memStreams.loads($id).cmd.bits.isWr := false.B")
      emit(src"io.memStreams.loads($id).cmd.bits.isSparse := 1.U")

      // Connect the streams to their IO interface signals
      emit(src"io.memStreams.loads($id).rdata.ready := ${swap(dataStream, Ready)}")
      emit(src"""${dataStream}.zip(io.memStreams.loads($id).rdata.bits).foreach{case (a,b) => a.r := ${DL("b", src"${dataStream.readers.head.fullDelay}.toInt")}}""")
      emit(src"""${swap(dataStream, NowValid)} := io.memStreams.loads($id).rdata.valid""")
      emit(src"""${swap(dataStream, Valid)} := ${DL(swap(dataStream, NowValid), src"${dataStream.readers.head.fullDelay}.toInt", true)}""")

    case FringeDenseStore(dram,cmdStream,dataStream,ackStream) =>
      appPropertyStats += HasTileStore
      if (cmdStream.isAligned) appPropertyStats += HasAlignedStore
      else appPropertyStats += HasUnalignedStore

      // Get parallelization of datastream
      val par = dataStream.writers.head match { case Op(e@StreamOutBankedWrite(_, _, ens)) => ens.length }

      val id = storesList.length
      storeParMapping = storeParMapping :+ s"""StreamParInfo(${bitWidth(dram.tp.typeArgs.head)}, ${par}, 0, false)"""
      storesList = storesList :+ dram

      // Connect streams to their IO interface signals
      emit(src"""${swap(dataStream, Ready)} := io.memStreams.stores($id).wdata.ready""")

      // Connect IO interface signals to their streams
      val (dataMSB, dataLSB) = getField(dataStream.tp.typeArgs.head, "_1")
      val (strbMSB, strbLSB) = getField(dataStream.tp.typeArgs.head, "_2")
      val (addrMSB, addrLSB)  = getField(cmdStream.tp.typeArgs.head, "offset")
      val (sizeMSB, sizeLSB)  = getField(cmdStream.tp.typeArgs.head, "size")
      val (isLdMSB, isLdLSB)  = getField(cmdStream.tp.typeArgs.head, "isLoad")

      emit(src"""io.memStreams.stores($id).wdata.bits.zip(${dataStream}).foreach{case (wport, wdata) => wport := wdata($dataMSB,$dataLSB) }""")
      emit(src"""io.memStreams.stores($id).wstrb.bits := ${dataStream}.map{ _.apply($strbMSB,$strbLSB) }.reduce(Cat(_,_)) """)
      emit(src"""io.memStreams.stores($id).wdata.valid := ${swap(dataStream, Valid)} """)

      emit(src"io.memStreams.stores($id).cmd.bits.addr := ${cmdStream}(0)($addrMSB,$addrLSB)")
      emit(src"io.memStreams.stores($id).cmd.bits.size := ${cmdStream}(0)($sizeMSB,$sizeLSB)")
      emit(src"io.memStreams.stores($id).cmd.valid :=  ${swap(cmdStream, Valid)}")
      emit(src"io.memStreams.stores($id).cmd.bits.isWr := ~${cmdStream}(0)($isLdMSB,$isLdLSB)")
      emit(src"io.memStreams.stores($id).cmd.bits.isSparse := 0.U")

      emit(src"${swap(cmdStream, Ready)} := io.memStreams.stores($id).cmd.ready")
      emit(src"""${swap(ackStream, NowValid)} := io.memStreams.stores($id).wresp.valid""")
      emit(src"""${swap(ackStream, Valid)} := ${DL(swap(ackStream, NowValid), src"${ackStream.readers.head.fullDelay}.toInt", true)}""")
      emit(src"""io.memStreams.stores($id).wresp.ready := ${swap(ackStream, Ready)}""")

    case FringeSparseStore(dram,cmdStream,ackStream) =>
      appPropertyStats += HasScatter
      // Get parallelization of datastream
      // emit(src"// This transfer belongs in channel ${transferChannel(parentOf(lhs).get)}")
      val par = 1//dataStream.writers.head match { case Op(e@StreamOutBankedWrite(_, _, ens)) => ens.length }

      Predef.assert(par == 1, s"Unsupported par '$par', only par=1 currently supported")

      val id = storesList.length
      // storeParMapping = storeParMapping :+ s"""StreamParInfo({if (FringeGlobals.target == "zcu") 32 else ${bitWidth(dram.tp.typeArgs.head)}}, ${par}, ${transferChannel(parentOf(lhs).get)}, true)"""
      storeParMapping = storeParMapping :+ s"""StreamParInfo(${bitWidth(dram.tp.typeArgs.head)}, ${par}, 0, true)"""
      storesList = storesList :+ dram

      // Connect IO interface signals to their streams
      val (dataMSB, dataLSB)  = getField(cmdStream.tp.typeArgs.head, "_1")
      val (addrMSB, addrLSB)  = getField(cmdStream.tp.typeArgs.head, "_2")

      emit(src"io.memStreams.stores($id).wdata.bits.zip(${cmdStream}).foreach{case (wport, wdata) => wport := wdata($dataMSB, $dataLSB)}")
      emit(src"io.memStreams.stores($id).wdata.valid := ${swap(cmdStream, Valid)}")
      emit(src"io.memStreams.stores($id).cmd.bits.addr := ${cmdStream}(0)($addrMSB, $addrLSB) // TODO: Is this always a vec of size 1?")
      emit(src"io.memStreams.stores($id).cmd.bits.size := 1.U")
      emit(src"io.memStreams.stores($id).cmd.valid :=  ${swap(cmdStream, Valid)}")
      emit(src"io.memStreams.stores($id).cmd.bits.isWr := 1.U")
      emit(src"io.memStreams.stores($id).cmd.bits.isSparse := 1.U")
      emit(src"${swap(cmdStream, Ready)} := io.memStreams.stores($id).cmd.ready")
      emit(src"""${swap(ackStream, NowValid)} := io.memStreams.stores($id).wresp.valid""")
      emit(src"""${swap(ackStream, Valid)} := ${DL(swap(ackStream, NowValid), src"${ackStream.readers.head.fullDelay}.toInt", true)}""")
      emit(src"""io.memStreams.stores($id).wresp.ready := ${swap(ackStream, Ready)}""")

    case _ => super.gen(lhs, rhs)
  }

  override def emitFooter(): Unit = {

    inAccel{
      val intersect = loadsList.distinct.intersect(storesList.distinct)

      val num_unusedDrams = drams.toList.length - loadsList.distinct.length - storesList.distinct.length + intersect.length

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
        emit (src"""val numArgIns_mem = ${loadsList.distinct.length} /*from loads*/ + ${storesList.distinct.length} /*from stores*/ - ${intersect.length} /*from bidirectional ${intersect}*/ + ${num_unusedDrams} /* from unused DRAMs */""")
        emit (src"""// $loadsList $storesList)""")
      }

      inGenn(out, "IOModule", ext) {
        emit ("// Scalars")
        emit (s"val io_numArgIns_reg = ${argIns.toList.length}")
        emit (s"val io_numArgOuts_reg = ${argOuts.toList.length}")
        emit (s"val io_numArgIOs_reg = ${argIOs.toList.length}")
        emit (s"val io_argOutLoopbacksMap: scala.collection.immutable.Map[Int,Int] = ${argOutLoopbacks}")
        emit ("// Memory Streams")
        emit (src"""val io_loadStreamInfo = List($loadParMapping) """)
        emit (src"""val io_storeStreamInfo = List($storeParMapping) """)
        emit (src"val io_numArgIns_mem = ${loadsList.distinct.length} /*from loads*/ + ${storesList.distinct.length} /*from stores*/ - ${intersect.length} /*from bidirectional ${intersect}*/ + ${num_unusedDrams} /* from unused DRAMs */")
        emit (src"val outArgMuxMap: scala.collection.mutable.Map[Int, Int] = scala.collection.mutable.Map[Int,Int]()")

      }

      inGen(out, "ArgAPI.scala") {
        emit("package accel")
        open("object api {")
        emit("\n// ArgIns")
        argIns.foreach{case (a, id) => emit(src"val ${argHandle(a)}_arg = $id")}
        emit("\n// DRAM Ptrs:")
        drams.foreach {case (d, id) => emit(src"val ${argHandle(d)}_ptr = ${id+argIns.toList.length}")}
        emit("\n// ArgIOs")
        argIOs.foreach{case (a, id) => emit(src"val ${argHandle(a)}_arg = ${id+argIns.toList.length+drams.toList.length}")}
        emit("\n// ArgOuts")
        argOuts.foreach{case (a, id) => emit(src"val ${argHandle(a)}_arg = ${id+argIns.toList.length+drams.toList.length+argIOs.toList.length}")}
        close("}")
      }
    }
    super.emitFooter()
  }

}
