package spatial.codegen.chiselgen

import argon._
import spatial.lang._
import spatial.node._

import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.retiming._

trait ChiselGenInterface extends ChiselGenCommon {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case InputArguments() =>
    case ArgInNew(init)  => 
      argIns += (lhs -> argIns.toList.length)
      val id = argHandle(lhs)
      forceEmit(src"val $lhs = top.io.argIns(api.${id}_arg)")
    case HostIONew(init)  => 
      argIOs += (lhs -> argIOs.toList.length)
      forceEmit(src"val $lhs = top.io.argOuts(${argIOs(lhs)})")
    case ArgOutNew(init) => 
      argOuts += (lhs -> argOuts.toList.length)
      forceEmit(src"val $lhs = top.io.argOuts(top.io_numArgIOs_reg + ${argOuts(lhs)})")

    // case GetReg(reg) if (reg.isArgOut) =>
    //   argOutLoopbacks.getOrElseUpdate(argOuts(reg), argOutLoopbacks.toList.length)
    //   // emitGlobalWireMap(src"""${lhs}""",src"Wire(${newWire(reg.tp.typeArgs.head)})")
    //   emit(src"""${lhs}.r := io.argOutLoopbacks(${argOutLoopbacks(argOuts(reg))})""")

    case GetReg(reg) if reg.isHostIO =>
      emit(src"""val ${lhs} = Wire(${lhs.tp})""")
      val id = argHandle(reg)
      emit(src"""${lhs}.r := $reg.bits.r""")

    case RegRead(reg)  if reg.isArgIn =>
      emit(src"""val ${lhs} = Wire(${lhs.tp})""")
      val id = argHandle(reg)
      emit(src"""${lhs}.r := $reg.r""")

    case RegRead(reg)  if reg.isHostIO =>
      emit(src"""val ${lhs} = Wire(${lhs.tp})""")
      val id = argHandle(reg)
      emit(src"""${lhs}.r := $reg.echo.r""")

    case RegRead(reg)  if reg.isArgOut =>
      argOutLoopbacks.getOrElseUpdate(argOuts(reg), argOutLoopbacks.toList.length)
      emit(src"""val ${lhs} = Wire(${reg.tp.typeArgs.head})""")
      emit(src"""${lhs}.r := $reg.echo.r""")


    case RegWrite(reg, v, en) if reg.isHostIO =>
      val isBroadcast = lhs.port.broadcast.exists(_>0)
      if (!isBroadcast) {
        val id = lhs.port.muxPort
        emit(src"val $id = $id")
        v.tp match {
          case FixPtType(s,d,f) =>
            if (s) {
              val pad = 64 - d - f
              if (pad > 0) {
                emit(src"""${reg}.port.bits.r := util.Cat(util.Fill($pad, ${v}.msb), ${v}.r)""")
              } else {
                emit(src"""${reg}.port.bits.r := ${v}.r""")
              }
            } else {
              emit(src"""${reg}.port.bits.r := ${v}.r""")
            }
          case _ =>
            emit(src"""${reg}.port.bits.r := ${v}.r""")
        }
        val enStr = if (en.isEmpty) "true.B" else en.map(quote).mkString(" & ")
        emit(src"""${reg}.port.valid := ${enStr} & ${DL(src"datapathEn & iiDone", lhs.fullDelay)}""")
      }

    case RegWrite(reg, v, en) if reg.isArgOut =>
      val isBroadcast = lhs.port.broadcast.exists(_>0)
      if (!isBroadcast) {
        val id = lhs.port.muxPort
        emit(src"val $id = $id")
        val padded = v.tp match {
          case FixPtType(s,d,f) if s && (64 > d + f) =>
            src"util.Cat(util.Fill(${64 - d - f}, $v.msb), $v.r)"
          case _ => src"$v.r"
        }
        emit(src"""${reg}.port.bits := $padded""")
        val enStr = if (en.isEmpty) "true.B" else en.map(quote).mkString(" & ")
        emit(src"""${reg}.port.valid := ${enStr} & ${DL(src"datapathEn & iiDone", lhs.fullDelay)}""")
      }

    case FringeDenseLoad(dram,cmdStream,dataStream) =>
      appPropertyStats += HasTileLoad
      if (cmdStream.isAligned) appPropertyStats += HasAlignedLoad
      else appPropertyStats += HasUnalignedLoad

    case FringeSparseLoad(dram,cmdStream,dataStream) =>
      appPropertyStats += HasGather

    case FringeDenseStore(dram,cmdStream,dataStream,ackStream) =>
      appPropertyStats += HasTileStore
      if (cmdStream.isAligned) appPropertyStats += HasAlignedStore
      else appPropertyStats += HasUnalignedStore

    case FringeSparseStore(dram,cmdStream,ackStream) =>
      appPropertyStats += HasScatter

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
      emit (src"""val loadStreamInfo = List(${loadStreams.toList.sortBy(_._2._2).map(_._2._1).map(_.replace("FringeGlobals.",""))}) """)
      emit (src"""val storeStreamInfo = List(${storeStreams.toList.sortBy(_._2._2).map(_._2._1).map(_.replace("FringeGlobals.",""))}) """)
      emit (src"""val gatherStreamInfo = List(${gatherStreams.toList.sortBy(_._2._2).map(_._2._1).map(_.replace("FringeGlobals.",""))}) """)
      emit (src"""val scatterStreamInfo = List(${scatterStreams.toList.sortBy(_._2._2).map(_._2._1).map(_.replace("FringeGlobals.",""))}) """)
      emit (src"""val numArgIns_mem = ${hostDrams.toList.length}""")
    }

    inGen(out, s"IOModule.$ext") {
      emit ("// Scalars")
      emit (s"val io_numArgIns_reg = ${argIns.toList.length}")
      emit (s"val io_numArgOuts_reg = ${argOuts.toList.length}")
      emit (s"val io_numArgIOs_reg = ${argIOs.toList.length}")
      emit (s"val io_argOutLoopbacksMap: scala.collection.immutable.Map[Int,Int] = ${argOutLoopbacks}")
      emit ("// Memory Streams")
      emit (src"""val io_loadStreamInfo = List(${loadStreams.toList.sortBy(_._2._2).map(_._2._1)}) """)
      emit (src"""val io_storeStreamInfo = List(${storeStreams.toList.sortBy(_._2._2).map(_._2._1)}) """)
      emit (src"""val io_gatherStreamInfo = List(${gatherStreams.toList.sortBy(_._2._2).map(_._2._1)}) """)
      emit (src"""val io_scatterStreamInfo = List(${scatterStreams.toList.sortBy(_._2._2).map(_._2._1)}) """)
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
      instrumentCounters.foreach{case (s,_) => 
        val base = instrumentCounterIndex(s)
        emit(src"val ${quote(s).toUpperCase}_cycles_arg = ${argIOs.toList.length + argOuts.toList.length + base}")
        emit(src"val ${quote(s).toUpperCase}_iters_arg = ${argIOs.toList.length + argOuts.toList.length + base + 1}")
        if (hasBackPressure(s.toCtrl) || hasForwardPressure(s.toCtrl)) {
          emit(src"val ${quote(s).toUpperCase}_stalled_arg = ${argIOs.toList.length + argOuts.toList.length + base + 2}")
          emit(src"val ${quote(s).toUpperCase}_idle_arg = ${argIOs.toList.length + argOuts.toList.length + base + 3}")
        }
      }
      earlyExits.foreach{x => 
        emit(src"val ${quote(x).toUpperCase}_exit_arg = ${argOuts.toList.length + argIOs.toList.length + instrumentCounterArgs()}")
      }
      close("}")
    }
    super.emitPostMain()
  }

}
