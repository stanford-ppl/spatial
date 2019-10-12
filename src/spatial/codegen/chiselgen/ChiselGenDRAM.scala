package spatial.codegen.chiselgen

import argon._
import spatial.lang._
import spatial.node._
import spatial.metadata.access._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.retiming._
import spatial.metadata.types._
import spatial.util.spatialConfig

trait ChiselGenDRAM extends ChiselGenCommon {
  var requesters = scala.collection.mutable.HashMap[Sym[_], Int]()

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case DRAMHostNew(_,_) =>
      hostDrams += (lhs -> hostDrams.size)
      connectDRAMStreams(lhs)
      forceEmit(src"// scoped in dram is ${scoped.mkString(",")} ")
      forceEmit(src"val $lhs = Wire(new FixedPoint(true, 64, 0))")
      forceEmit(src"$lhs.r := accelUnit.io.argIns(api.${argHandle(lhs)}_ptr)")

    case FrameHostNew(size,_) =>

    case DRAMAccelNew(dim) =>

    case DRAMAlloc(dram, dims) =>
      dram match {
        case _@Op(DRAMAccelNew(_)) =>
          val id = requesters.size
          val parent = lhs.parent.s.get
          val invEnable = src"""${DL(src"$datapathEn & $iiDone", lhs.fullDelay, true)}"""
          val d = dims.map{ quote(_) + ".r" }.mkString(src"List[UInt](", ",", ")")
          emit(src"${dram}.connectAlloc($id, $d, $invEnable)")
          requesters += (lhs -> id)
        case _ =>
      }

    case DRAMIsAlloc(dram) =>
      dram match {
        case _@Op(DRAMAccelNew(_)) =>
          emit(src"val $lhs = $dram.output.isAlloc")
        case _@Op(DRAMHostNew(_,_)) =>
          emit(src"val $lhs = true.B")
        case _ =>
      }

    case DRAMDealloc(dram) =>
      dram match {
        case _@Op(DRAMAccelNew(_)) =>
          val id = requesters.size
          val parent = lhs.parent.s.get
          val invEnable = src"""${DL(src"$datapathEn & $iiDone", lhs.fullDelay, true)}"""
          emit(src"${dram}.connectDealloc($id, $invEnable)")
          requesters += (lhs -> id)
        case _ =>
      }

    case DRAMAddress(dram) =>
      dram match {
        case _@Op(DRAMAccelNew(_)) =>
          emit(src"val $lhs = ${dram}.output.addr")
        case _@Op(DRAMHostNew(_,_)) =>
          emit(src"val $lhs = $dram")
        case _ =>
      }

    case _ => super.gen(lhs, rhs)
  }

  override def emitPostMain(): Unit = {

    inGen(out, s"AccelWrapper.$ext") {
      emit("// Heap")
      emit(src"val io_numAllocators = scala.math.max(1, ${accelDrams.size})")
    }

    inGen(out, "Instantiator.scala") {
      emit(src"// Heap")
      emit(src"val numAllocators = ${accelDrams.size}")
    }
  
    super.emitPostMain()
  }
}
