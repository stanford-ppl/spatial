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
    case InputArguments()       => 
    case ArgInNew(init)  => 
      argIns += (lhs -> argIns.toList.length)
    case ArgOutNew(init) => 
      enterAccel()
      emitGlobalWireMap(src"${swap(lhs, DataOptions)}", src"Wire(Vec(${scala.math.max(1,writersOf(lhs).size)}, UInt(64.W)))", forceful=true)
      emitGlobalWireMap(src"${swap(lhs, EnOptions)}", src"Wire(Vec(${scala.math.max(1,writersOf(lhs).size)}, Bool()))", forceful=true)
      argOuts += (lhs -> argOuts.toList.length)
      emitt(src"""io.argOuts(${argOuts(lhs)}).bits := chisel3.util.Mux1H(${swap(lhs, EnOptions)}, ${swap(lhs, DataOptions)}) // ${lhs.name.getOrElse("")}""", forceful=true)
      emitt(src"""io.argOuts(${argOuts(lhs)}).valid := ${swap(lhs, EnOptions)}.reduce{_|_}""", forceful=true)
      exitAccel()

    case GetReg(reg) =>
      argOutLoopbacks.getOrElseUpdate(argOuts(reg), argOutLoopbacks.toList.length)
      // emitGlobalWireMap(src"""${lhs}""",src"Wire(${newWire(reg.tp.typeArguments.head)})")
      emitt(src"""${lhs}.r := io.argOutLoopbacks(${argOutLoopbacks(argOuts(reg))})""")


    case RegRead(reg)  if (reg.isArgIn) => 
      emitGlobalWireMap(src"""${lhs}""",src"Wire(${lhs.tp})")
      emitGlobalWire(src"""${lhs}.r := io.argIns(${argIns(reg)})""")

    case RegRead(reg)  if (reg.isArgOut) => 
      argOutLoopbacks.getOrElseUpdate(argOuts(reg), argOutLoopbacks.toList.length)
      // emitGlobalWireMap(src"""${lhs}""",src"Wire(${newWire(reg.tp.typeArguments.head)})")
      emitt(src"""${lhs}.r := io.argOutLoopbacks(${argOutLoopbacks(argOuts(reg))})""")


    case RegWrite(reg, v, en) if (reg.isArgOut) => 
      val id = argOuts(reg)
      emitt(src"val ${lhs}_wId = getArgOutLane($id)")
      v.tp match {
        case FixPtType(s,d,f) =>
          if (s) {
            val pad = 64 - d - f
            if (pad > 0) {
              emitt(src"""${swap(reg, DataOptions)}(${lhs}_wId) := util.Cat(util.Fill($pad, ${v}.msb), ${v}.r)""")
            } else {
              emitt(src"""${swap(reg, DataOptions)}(${lhs}_wId) := ${v}.r""")
            }
          } else {
            emitt(src"""${swap(reg, DataOptions)}(${lhs}_wId) := ${v}.r""")
          }
        case _ =>
          emitt(src"""${swap(reg, DataOptions)}(${lhs}_wId) := ${v}.r""")
      }
      val enStr = if (en.isEmpty) "true.B" else en.map(quote).mkString(" & ")
      emitt(src"""${swap(reg, EnOptions)}(${lhs}_wId) := ${enStr} & ${DL(swap(controllerStack.head, DatapathEn), src"${if (en.isEmpty) 0 else enableRetimeMatch(en.head, lhs)}.toInt")}""")

    case DRAMNew(dims, _) => 
      drams += (lhs -> drams.toList.length)

    case GetDRAMAddress(dram) =>
      val id = argHandle(dram)
      emit(src"""val $lhs = io.argIns(api.${id}_ptr)""")

    case FringeDenseLoad(dram,cmdStream,dataStream) =>
      // // Find stages that pushes to cmdstream and dataStream
      // var cmdStage: Option[Sym[_]] = None
      // childrenOf(parentOf(lhs).get).map{c =>
      //   if (childrenOf(c).length > 0) {
      //     childrenOf(c).map{ cc => 
      //       pushesTo(cc).distinct.map{ pt => pt.memory match {
      //         case fifo @ Def(StreamOutNew(bus)) => 
      //           if (s"$bus".contains("BurstCmdBus")) cmdStage = Some(cc)
      //         case _ => 
      //       }}                      
      //     }
      //   } else {
      //     pushesTo(c).distinct.map{ pt => pt.memory match {
      //       case fifo @ Def(StreamOutNew(bus)) => 
      //         if (s"$bus".contains("BurstCmdBus")) cmdStage = Some(c)
      //       case _ => 
      //     }}          
      //   }
      // }

      appPropertyStats += HasTileLoad
      if (isAligned(cmdStream)) appPropertyStats += HasAlignedLoad
      else appPropertyStats += HasUnalignedLoad
      // Get parallelization of datastream
      // emit(src"// This transfer belongs in channel ${transferChannel(parentOf(lhs).get)}")
      val par = readersOf(dataStream).head match { case Op(e@StreamInBankedRead(strm, ens)) => ens.length }

      val id = loadsList.length
      // loadParMapping = loadParMapping :+ s"""StreamParInfo(${bitWidth(dram.tp.typeArguments.head)}, ${par}, ${transferChannel(parentOf(lhs).get)}, false)"""
      loadParMapping = loadParMapping :+ s"""StreamParInfo(${bitWidth(dram.tp.typeArgs.head)}, ${par}, 0, false)"""
      loadsList = loadsList :+ dram

      // controllerStack.push(cmdStage.get) // Push so that DLI does the right thing
      // Connect the IO interface signals to their streams
      emit(src"${swap(cmdStream, Ready)} := io.memStreams.loads($id).cmd.ready // Not sure why the cmdStream ready used to be delayed")
      val (addrMSB, addrLSB)  = getField(cmdStream.tp.typeArgs.head, "offset")
      val (sizeMSB, sizeLSB)  = getField(cmdStream.tp.typeArgs.head, "size")
      val (isLdMSB, isLdLSB)  = getField(cmdStream.tp.typeArgs.head, "isLoad")
      emit(src"io.memStreams.loads($id).cmd.bits.addr := ${cmdStream}(0)($addrMSB,$addrLSB)")
      emit(src"io.memStreams.loads($id).cmd.bits.size := ${cmdStream}(0)($sizeMSB,$sizeLSB)")
      emit(src"io.memStreams.loads($id).cmd.valid :=  ${swap(cmdStream, Valid)}")
      emit(src"io.memStreams.loads($id).cmd.bits.isWr := ~${cmdStream}(0)($isLdMSB,$isLdLSB)")
      emit(src"io.memStreams.loads($id).cmd.bits.isSparse := 0.U")
      // controllerStack.pop()
      
      // Connect the streams to their IO interface signals
      emit(src"io.memStreams.loads($id).rdata.ready := ${swap(dataStream, Ready)}")
      emit(src"""${dataStream}.zip(io.memStreams.loads($id).rdata.bits).foreach{case (a,b) => a.r := ${DL("b", src"${symDelay(readersOf(dataStream).head)}.toInt")}}""")
      emit(src"""${swap(dataStream, NowValid)} := io.memStreams.loads($id).rdata.valid""")
      emit(src"""${swap(dataStream, Valid)} := ${DL(swap(dataStream, NowValid), src"${symDelay(readersOf(dataStream).head)}.toInt", true)}""")


    // case FringeSparseLoad(dram,addrStream,dataStream) =>
    //   appPropertyStats += HasGather
    //   // Get parallelization of datastream
    //   emit(src"// This transfer belongs in channel ${transferChannel(parentOf(lhs).get)}")
    //   val par = readersOf(dataStream).head.node match {
    //     case Def(e@ParStreamRead(strm, ens)) => ens.length
    //     case _ => 1
    //   }
    //   assert(par == 1, s"Unsupported par '$par' for sparse loads! Must be 1 currently")

    //   val id = loadsList.length
    //   // loadParMapping = loadParMapping :+ s"""StreamParInfo(if (FringeGlobals.target == "zcu") 32 else ${bitWidth(dram.tp.typeArguments.head)}, ${par}, ${transferChannel(parentOf(lhs).get)}, true)"""
    //   loadParMapping = loadParMapping :+ s"""StreamParInfo(${bitWidth(dram.tp.typeArguments.head)}, ${par}, ${transferChannel(parentOf(lhs).get)}, true)"""
    //   loadsList = loadsList :+ dram
    //   val turnstiling_stage = getLastChild(parentOf(lhs).get)
    //   emitGlobalWire(src"""val ${turnstiling_stage}_enq = io.memStreams.loads(${id}).rdata.valid""")

    //   emit(src"""${dataStream}.zip(io.memStreams.loads($id).rdata.bits).foreach{case (a,b) => a.r := ${DL("b", src"${symDelay(readersOf(dataStream).head.node)}.toInt")}}""")
    //   emit(src"""${swap(dataStream, NowValid)} := io.memStreams.loads($id).rdata.valid""")
    //   emit(src"""${swap(dataStream, Valid)} := ${DL(swap(dataStream, NowValid), src"${symDelay(readersOf(dataStream).head.node)}.toInt", true)}""")
    //   // emit(src"${swap(addrStream, Ready)} := ${DL(src"io.memStreams.loads($id).cmd.ready", src"${symDelay(writersOf(addrStream).head.node)}.toInt", true)}")
    //   emit(src"${swap(addrStream, Ready)} := ${DL(src"io.memStreams.loads($id).cmd.ready", src"0", true)} // Not sure why the cmdStream ready used to be delayed")
    //   emit(src"io.memStreams.loads($id).rdata.ready := ${swap(dataStream, Ready)}")
    //   emit(src"io.memStreams.loads($id).cmd.bits.addr := ${addrStream}(0).r // TODO: Is sparse addr stream always a vec?")
    //   emit(src"io.memStreams.loads($id).cmd.bits.size := 1.U")
    //   emit(src"io.memStreams.loads($id).cmd.valid :=  ${swap(addrStream, Valid)}")
    //   emit(src"io.memStreams.loads($id).cmd.bits.isWr := false.B")
    //   emit(src"io.memStreams.loads($id).cmd.bits.isSparse := 1.U")

    case FringeDenseStore(dram,cmdStream,dataStream,ackStream) =>
      appPropertyStats += HasTileStore

      // // Find stages that pushes to cmdstream and dataStream
      // var cmdStage: Option[Sym[_]] = None
      // var dataStage: Option[Sym[_]] = None
      // childrenOf(parentOf(lhs).get).map{c =>
      //   if (childrenOf(c).length > 0) {
      //     childrenOf(c).map{ cc => 
      //       pushesTo(cc).distinct.map{ pt => pt.memory match {
      //         case fifo @ Def(StreamOutNew(bus)) => 
      //           if (s"$bus".contains("BurstFullDataBus")) dataStage = Some(cc)
      //           if (s"$bus".contains("BurstCmdBus")) cmdStage = Some(cc)
      //         case _ => 
      //       }}                      
      //     }
      //   } else {
      //     pushesTo(c).distinct.map{ pt => pt.memory match {
      //       case fifo @ Def(StreamOutNew(bus)) => 
      //         if (s"$bus".contains("BurstFullDataBus")) dataStage = Some(c)
      //         if (s"$bus".contains("BurstCmdBus")) cmdStage = Some(c)
      //       case _ => 
      //     }}          
      //   }
      // }

      if (isAligned(cmdStream)) appPropertyStats += HasAlignedStore
      else appPropertyStats += HasUnalignedStore

      // Get parallelization of datastream
      // emit(src"// This transfer belongs in channel ${transferChannel(parentOf(lhs).get)}")
      val par = writersOf(dataStream).head match { case Op(e@StreamOutBankedWrite(_, _, ens)) => ens.length }

      val id = storesList.length
      // storeParMapping = storeParMapping :+ s"""StreamParInfo(if (FringeGlobals.target == "zcu") 32 else ${bitWidth(dram.tp.typeArguments.head)}, ${par}, ${transferChannel(parentOf(lhs).get)}, false)"""
      // storeParMapping = storeParMapping :+ s"""StreamParInfo(${bitWidth(dram.tp.typeArgs.head)}, ${par}, ${transferChannel(parentOf(lhs).get)}, false)"""
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
      emit(src"""${swap(ackStream, Valid)} := ${DL(swap(ackStream, NowValid), src"${symDelay(readersOf(ackStream).head)}.toInt", true)}""")
      emit(src"""io.memStreams.stores($id).wresp.ready := ${swap(ackStream, Ready)}""")

    // case FringeSparseStore(dram,cmdStream,ackStream) =>
    //   appPropertyStats += HasScatter
    //   // Get parallelization of datastream
    //   emit(src"// This transfer belongs in channel ${transferChannel(parentOf(lhs).get)}")
    //   val par = writersOf(cmdStream).head.node match {
    //     case Def(e@ParStreamWrite(_, _, ens)) => ens.length
    //     case _ => 1
    //   }
    //   Predef.assert(par == 1, s"Unsupported par '$par', only par=1 currently supported")

    //   val id = storesList.length
    //   // storeParMapping = storeParMapping :+ s"""StreamParInfo({if (FringeGlobals.target == "zcu") 32 else ${bitWidth(dram.tp.typeArguments.head)}}, ${par}, ${transferChannel(parentOf(lhs).get)}, true)"""
    //   storeParMapping = storeParMapping :+ s"""StreamParInfo(${bitWidth(dram.tp.typeArguments.head)}, ${par}, ${transferChannel(parentOf(lhs).get)}, true)"""
    //   storesList = storesList :+ dram

    //   val (addrMSB, addrLSB) = tupCoordinates(cmdStream.tp.typeArguments.head, "_2")
    //   val (dataMSB, dataLSB) = tupCoordinates(cmdStream.tp.typeArguments.head, "_1")
    //   emit(src"io.memStreams.stores($id).wdata.bits.zip(${cmdStream}).foreach{case (wport, wdata) => wport := wdata($dataMSB, $dataLSB)}")
    //   emit(src"io.memStreams.stores($id).wdata.valid := ${swap(cmdStream, Valid)}")
    //   emit(src"io.memStreams.stores($id).cmd.bits.addr := ${cmdStream}(0)($addrMSB, $addrLSB) // TODO: Is this always a vec of size 1?")
    //   emit(src"io.memStreams.stores($id).cmd.bits.size := 1.U")
    //   emit(src"io.memStreams.stores($id).cmd.valid :=  ${swap(cmdStream, Valid)}")
    //   emit(src"io.memStreams.stores($id).cmd.bits.isWr := 1.U")
    //   emit(src"io.memStreams.stores($id).cmd.bits.isSparse := 1.U")
    //   emit(src"${swap(cmdStream, Ready)} := ${DL(src"io.memStreams.stores($id).wdata.ready", src"${symDelay(writersOf(cmdStream).head.node)}.toInt", true)}")
    //   emit(src"""${swap(ackStream, NowValid)} := io.memStreams.stores($id).wresp.valid""")
    //   emit(src"""${swap(ackStream, Valid)} := ${DL(swap(ackStream, NowValid), src"${symDelay(readersOf(ackStream).head.node)}.toInt", true)}""")
    //   emit(src"""io.memStreams.stores($id).wresp.ready := ${swap(ackStream, Ready)}""")

    case _ => super.gen(lhs, rhs)
  }

  override def emitFooter(): Unit = {

    enterAccel()
    val intersect = loadsList.distinct.intersect(storesList.distinct)

    val num_unusedDrams = drams.toList.length - loadsList.distinct.length - storesList.distinct.length + intersect.length

    inGen(out, "Instantiator.scala") {
      emit("")
      emit("// Scalars")
      emit(s"val numArgIns_reg = ${argIns.toList.length}")
      emit(s"val numArgOuts_reg = ${argOuts.toList.length}")
      emit(s"val numArgIOs_reg = ${argIOs.toList.length}")
      argIns.zipWithIndex.foreach { case(p,i) => emit(s"""//${quote(p._1)} = argIns($i) ( ${p._1.name.getOrElse("")} )""") }
      argOuts.zipWithIndex.foreach { case(p,i) => emit(s"""//${quote(p._1)} = argOuts($i) ( ${p._1.name.getOrElse("")} )""") }
      argIOs.zipWithIndex.foreach { case(p,i) => emit(s"""//${quote(p._1)} = argIOs($i) ( ${p._1.name.getOrElse("")} )""") }
      emit(s"val io_argOutLoopbacksMap: scala.collection.immutable.Map[Int,Int] = ${argOutLoopbacks}")
      emit("")
      emit(s"// Memory streams")
      emit(src"""val loadStreamInfo = List(${loadParMapping.map(_.replace("FringeGlobals.",""))}) """)
      emit(src"""val storeStreamInfo = List(${storeParMapping.map(_.replace("FringeGlobals.",""))}) """)
      emit(src"""val numArgIns_mem = ${loadsList.distinct.length} /*from loads*/ + ${storesList.distinct.length} /*from stores*/ - ${intersect.length} /*from bidirectional ${intersect}*/ + ${num_unusedDrams} /* from unused DRAMs */""")
      emit(src"""// $loadsList $storesList)""")
    }

    inGenn(out, "IOModule", ext) {
      emit("// Scalars")
      emit(s"val io_numArgIns_reg = ${argIns.toList.length}")
      emit(s"val io_numArgOuts_reg = ${argOuts.toList.length}")
      emit(s"val io_numArgIOs_reg = ${argIOs.toList.length}")
      emit(s"val io_argOutLoopbacksMap: scala.collection.immutable.Map[Int,Int] = ${argOutLoopbacks}")
      emit("// Memory Streams")
      emit(src"""val io_loadStreamInfo = List($loadParMapping) """)
      emit(src"""val io_storeStreamInfo = List($storeParMapping) """)
      emit(src"val io_numArgIns_mem = ${loadsList.distinct.length} /*from loads*/ + ${storesList.distinct.length} /*from stores*/ - ${intersect.length} /*from bidirectional ${intersect}*/ + ${num_unusedDrams} /* from unused DRAMs */")
    }

    inGen(out, "ArgAPI.scala") {
      emit("package accel")
      open("object api {")
      emit("\n// ArgIns")
      argIns.foreach{case (a, id) => emit(src"val ${argHandle(a)}_arg = $id")}
      emit("\n// ArgIOs")
      argIOs.foreach{case (a, id) => emit(src"val ${argHandle(a)}_arg = ${id+argIns.toList.length}")}
      emit("\n// ArgOuts")
      argOuts.foreach{case (a, id) => emit(src"val ${argHandle(a)}_arg = $id")}
      emit("\n// DRAM Ptrs:")
      drams.foreach {case (d, id) => emit(src"val ${argHandle(d)}_ptr = ${id+argIns.toList.length+argIOs.toList.length}")}
      close("}")
    }
    exitAccel()
    super.emitFooter()
  }

}
