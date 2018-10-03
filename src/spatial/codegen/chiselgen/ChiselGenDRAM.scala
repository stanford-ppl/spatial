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
    case DRAMStaticNew(_,_) =>
      hostDrams += (lhs -> hostDrams.size)

    case DRAMDynNew() =>
      val reqCount = lhs.consumers.collect {
        case w@Op(_: DRAMAlloc[_,_] | _: DRAMDealloc[_,_]) => w
      }.size
      emitGlobalModule(src"""val $lhs = Module(new DRAMAllocator($reqCount))""")
      val id = accelDrams.size
      emitt(src"io.heap.req($id) := $lhs.io.heapReq")
      emitt(src"$lhs.io.heapResp := io.heap.resp($id)")
      accelDrams += (lhs -> id)

    case DRAMAlloc(dram, dims) =>
      dram match {
        case _@Op(DRAMDynNew()) =>
          val id = requesters.size
          val parent = lhs.parent
          val invEnable = src"""${DL(src"${swap(parent, DatapathEn)} & ${swap(parent, IIDone)}", lhs.fullDelay, true)}"""
          emitt(src"${dram}.io.appReq($id).valid := $invEnable")
          emitt(src"${dram}.io.appReq($id).bits.allocDealloc := true.B")
          emitt(src"${dram}.io.appReq($id).bits.sizeAddr := ${dims}.r")
          requesters += (lhs -> id)
        case _ =>
      }

    case DRAMDealloc(dram) =>
      dram match {
        case _@Op(DRAMDynNew()) =>
          val id = requesters.size
          val parent = lhs.parent
          val invEnable = src"""${DL(src"${swap(parent, DatapathEn)} & ${swap(parent, IIDone)}", lhs.fullDelay, true)}"""
          emitt(src"${dram}.io.appReq($id).valid := $invEnable")
          emitt(src"${dram}.io.appReq($id).bits.allocDealloc := false.B")
          requesters += (lhs -> id)
        case _ =>
      }

    case DRAMAddress(dram) =>
      dram match {
        case _@Op(DRAMDynNew()) =>
          emit(src"val $lhs = ${dram}.io.addr")
        case _@Op(DRAMStaticNew(_,_)) =>
          val id = argHandle(dram)
          emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})")
          emit(src"""$lhs.r := io.argIns(api.${id}_ptr)""")
        case _ =>
      }

    case _ => super.gen(lhs, rhs)
  }

  override def emitFooter(): Unit = {
  	inAccel{
      inGenn(out, "IOModule", ext) {
        emit("// Heap")
        emit(src"val io_numAllocators = scala.math.max(1, ${accelDrams.size})")
      }

      inGen(out, "Instantiator.scala") {
        emit(src"// Heap")
        emit(src"val numAllocators = ${accelDrams.size}")
      }
    }
    super.emitFooter()
  }
}
