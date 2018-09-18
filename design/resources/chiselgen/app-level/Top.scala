package top

import chisel3._
import chisel3.util._
import fringe._
import accel._
import axi4._
import templates._
import templates.Utils.{log2Up, getFF}


// import AccelTop
abstract class TopInterface extends Bundle {
  // Host scalar interface
  var raddr = Input(UInt(1.W))
  var wen  = Input(Bool())
  var waddr = Input(UInt(1.W))
  var wdata = Input(Bits(1.W))
  var rdata = Output(Bits(1.W))
  val is_enabled = Output(Bool())

}

case class TopParams(
  val addrWidth: Int,
  val dataWidth: Int,
  val v: Int,
  val numArgIns: Int,
  val numArgOuts: Int,
  val numArgIOs: Int,
  val numChannels: Int,
  val numArgInstrs: Int,
  val argOutLoopbacksMap: scala.collection.immutable.Map[Int,Int],
  val loadStreamInfo: List[StreamParInfo],
  val storeStreamInfo: List[StreamParInfo],
  val streamInsInfo: List[StreamParInfo],
  val streamOutsInfo: List[StreamParInfo],
  target: String
)

class VerilatorInterface(p: TopParams) extends TopInterface {
  // Host scalar interface
  raddr = Input(UInt(p.addrWidth.W))
  wen  = Input(Bool())
  waddr = Input(UInt(p.addrWidth.W))
  wdata = Input(Bits(64.W))
  rdata = Output(Bits(64.W))

  // DRAM interface - currently only one stream
  val dram = Vec(p.numChannels, new DRAMStream(p.dataWidth, p.v)) // Bus is 64 elements of 8 bits
  val axiParams = new AXI4BundleParameters(p.dataWidth, 512, 32)

  // Input streams
//  val genericStreamIn = StreamIn(StreamParInfo(32,1))
//  val genericStreamOut = StreamOut(StreamParInfo(32,1))

  // Debug signals
//  val dbg = new DebugSignals
}

class ZynqInterface(p: TopParams) extends TopInterface {
  val axiLiteParams = new AXI4BundleParameters(p.addrWidth, p.dataWidth, 1)
  val axiParams = new AXI4BundleParameters(p.addrWidth, 512, 32)

  val S_AXI = Flipped(new AXI4Lite(axiLiteParams))
  val M_AXI = Vec(p.numChannels, new AXI4Inlined(axiParams))

  // AXI debugging loopbacks
  val TOP_AXI = new AXI4Probe(axiLiteParams)
  val DWIDTH_AXI = new AXI4Probe(axiLiteParams)
  val PROTOCOL_AXI = new AXI4Probe(axiLiteParams)
  val CLOCKCONVERT_AXI = new AXI4Probe(axiLiteParams)
}

class Arria10Interface(p: TopParams) extends TopInterface {
  // To fit the sysid interface, we only want to have 7 bits for 0x0000 ~ 0x01ff
  val axiLiteParams = new AXI4BundleParameters(7, p.dataWidth, 1)
  // TODO: This group of params is for memory
  val axiParams = new AXI4BundleParameters(p.dataWidth, 512, 6)
  val S_AVALON = new AvalonSlave(axiLiteParams) // scalars
  val M_AXI = Vec(p.numChannels, new AXI4Inlined(axiParams))
}


class AWSInterface(p: TopParams) extends TopInterface {
  val axiLiteParams = new AXI4BundleParameters(p.addrWidth, p.dataWidth, 1)
  val axiParams = new AXI4BundleParameters(p.addrWidth, 512, 16)

  val enable = Input(UInt(p.dataWidth.W))
  val done = Output(UInt(p.dataWidth.W))
  val scalarIns = Input(Vec(p.numArgIns, UInt(64.W)))
  val scalarOuts = Output(Vec(p.numArgOuts, UInt(64.W)))

//  val dbg = new DebugSignals

  // DRAM interface - currently only one stream
//  val dram = new DRAMStream(p.dataWidth, p.v)
  val M_AXI = Vec(p.numChannels, new AXI4Inlined(axiParams))
}

/**
 * Top: Top module including Fringe and Accel
 * @param w: Word width
 * @param numArgIns: Number of input scalar arguments
 * @param numArgOuts: Number of output scalar arguments
 */
class Top(
  val w: Int,
  val numArgIns: Int,
  val numArgOuts: Int,
  val numArgIOs: Int,
  val numArgInstrs: Int,
  val argOutLoopbacksMap: scala.collection.immutable.Map[Int,Int],
  val loadStreamInfo: List[StreamParInfo],
  val storeStreamInfo: List[StreamParInfo],
  val streamInsInfo: List[StreamParInfo],
  val streamOutsInfo: List[StreamParInfo],
  target: String = "") extends Module {

  // Ensure that there is at least one argIn and argOut; this ensures that
  // the argIn/argOut interfaces do no become undirected null vectors
  val totalArgIns = math.max(1, numArgIns)
  val totalArgOuts = math.max(1, numArgOuts)
  val totalArgOutLoobacks = math.max(1, argOutLoopbacksMap.toList.length)
  val totalRegs = totalArgIns + totalArgOuts + 2  // (command, status registers)

  //val addrWidth = if (target == "zcu") 40 else 32
  val addrWidth = target match {
    case "zcu"             => 40
    case "aws" | "aws-sim" => 64
    case "VCU1525"         => 64
    case _                 => 32
  }
  val dataWidth = if (target == "zcu") 64 else w
  val v = if (target == "vcs" || target == "asic") 64 else 16
  val totalLoadStreamInfo = loadStreamInfo ++ (if (loadStreamInfo.size == 0) List(StreamParInfo(w, v, 0, false)) else List[StreamParInfo]())
  val totalStoreStreamInfo = storeStreamInfo ++ (if (storeStreamInfo.size == 0) List(StreamParInfo(w, v, 0, false)) else List[StreamParInfo]())

  val numChannels = target match {
    case "zynq" | "zcu"     => 1
    case "aws" | "aws-sim"  => 4
    case "VCU1525"          => 4
    case _                  => 1
  }

  val topParams = TopParams(addrWidth, dataWidth, v, totalArgIns, totalArgOuts, numArgIOs, numChannels, numArgInstrs, argOutLoopbacksMap, totalLoadStreamInfo, totalStoreStreamInfo, streamInsInfo, streamOutsInfo, target)

  FringeGlobals.target = target
  FringeGlobals.enableDebugRegs = false

  val io = target match {
    case "verilator"  => IO(new VerilatorInterface(topParams))
    case "vcs"        => IO(new VerilatorInterface(topParams))
    case "xsim"       => IO(new VerilatorInterface(topParams))
    case "aws"        => IO(new AWSInterface(topParams))
    case "aws-sim"    => IO(new AWSInterface(topParams))
    case "VCU1525"    => IO(new AWSInterface(topParams))
    case "zynq"       => IO(new ZynqInterface(topParams))
    case "zcu"        => IO(new ZynqInterface(topParams))
    case "arria10"    => IO(new Arria10Interface(topParams))
    case "asic"       => IO(new VerilatorInterface(topParams))
    case _ => throw new Exception(s"Unknown target '$target'")
  }

  // Accel
  val accel = Module(new AccelTop(w, totalArgIns, totalArgOuts, numArgIOs, numArgInstrs, argOutLoopbacksMap, totalLoadStreamInfo, totalStoreStreamInfo, streamInsInfo, streamOutsInfo))

  target match {
    case "verilator" | "vcs" | "xsim" | "asic" =>
      // Simulation Fringe
      val blockingDRAMIssue = false
      val topIO = io.asInstanceOf[VerilatorInterface]
      val fringe = Module(new Fringe(w, totalArgIns, totalArgOuts, numArgIOs, numChannels, numArgInstrs, argOutLoopbacksMap, totalLoadStreamInfo, totalStoreStreamInfo, streamInsInfo, streamOutsInfo, blockingDRAMIssue, topIO.axiParams))

      // Fringe <-> Host connections
      fringe.io.raddr := topIO.raddr
      fringe.io.wen   := topIO.wen
      fringe.io.waddr := topIO.waddr
      fringe.io.wdata := topIO.wdata
      topIO.rdata := fringe.io.rdata

      // Fringe <-> DRAM connections
      topIO.dram <> fringe.io.dram

      if (accel.io.argIns.length > 0) {
        accel.io.argIns := fringe.io.argIns
      }

      if (accel.io.argOutLoopbacks.length > 0) {
        accel.io.argOutLoopbacks := fringe.io.argOutLoopbacks
      }

      if (accel.io.argOuts.length > 0) {
        fringe.io.argOuts.zip(accel.io.argOuts) foreach { case (fringeArgOut, accelArgOut) =>
            fringeArgOut.bits := accelArgOut.bits
            fringeArgOut.valid := accelArgOut.valid
        }
      }
      fringe.io.memStreams <> accel.io.memStreams
      accel.io.enable := fringe.io.enable
      fringe.io.done := accel.io.done
      accel.io.reset := fringe.io.reset


    case "arria10" =>
      val blockingDRAMIssue = false
      val topIO = io.asInstanceOf[Arria10Interface]
      val fringe = Module(new FringeArria10(w, totalArgIns, totalArgOuts,
                                            numArgIOs, numChannels, numArgInstrs, argOutLoopbacksMap,
                                            totalLoadStreamInfo, totalStoreStreamInfo,
                                            streamInsInfo, streamOutsInfo, blockingDRAMIssue,
                                            topIO.axiLiteParams, topIO.axiParams))
      // Fringe <-> Host Connections
      fringe.io.S_AVALON <> topIO.S_AVALON

      // Fringe <-> DRAM Connections
      topIO.M_AXI <> fringe.io.M_AXI

      // TODO: add memstream connections here
      if (accel.io.argIns.length > 0) {
        accel.io.argIns := fringe.io.argIns
      }

      if (accel.io.argOutLoopbacks.length > 0) {
        accel.io.argOutLoopbacks := fringe.io.argOutLoopbacks
      }

      if (accel.io.argOuts.length > 0) {
        fringe.io.argOuts.zip(accel.io.argOuts) foreach { case (fringeArgOut, accelArgOut) =>
          fringeArgOut.bits := accelArgOut.bits
          fringeArgOut.valid := accelArgOut.valid
        }
      }

      // memStream connections
      fringe.io.externalEnable := false.B
      fringe.io.memStreams <> accel.io.memStreams

      accel.io.enable := fringe.io.enable
      fringe.io.done := accel.io.done
      accel.reset := reset.toBool | fringe.io.reset

    case "zynq" | "zcu" =>
      // Zynq Fringe
      val topIO = io.asInstanceOf[ZynqInterface]

      val blockingDRAMIssue = false // Allow only one in-flight request, block until response comes back
      val fringe = Module(new FringeZynq(w, totalArgIns, totalArgOuts, numArgIOs, numChannels, numArgInstrs, argOutLoopbacksMap, totalLoadStreamInfo, totalStoreStreamInfo, streamInsInfo, streamOutsInfo, blockingDRAMIssue, topIO.axiLiteParams, topIO.axiParams))

      // Fringe <-> Host connections
      fringe.io.S_AXI <> topIO.S_AXI

      // Fringe <-> DRAM connections
      topIO.M_AXI <> fringe.io.M_AXI

      topIO.TOP_AXI <> fringe.io.TOP_AXI
      topIO.DWIDTH_AXI <> fringe.io.DWIDTH_AXI
      topIO.PROTOCOL_AXI <> fringe.io.PROTOCOL_AXI
      topIO.CLOCKCONVERT_AXI <> fringe.io.CLOCKCONVERT_AXI

      accel.io.argIns := fringe.io.argIns
      accel.io.argOutLoopbacks := fringe.io.argOutLoopbacks
      fringe.io.argOuts.zip(accel.io.argOuts) foreach { case (fringeArgOut, accelArgOut) =>
          fringeArgOut.bits := accelArgOut.bits
          fringeArgOut.valid := accelArgOut.valid
      }
      // accel.io.argIOIns := fringe.io.argIOIns
      // fringe.io.argIOOuts.zip(accel.io.argIOOuts) foreach { case (fringeArgOut, accelArgOut) =>
      //     fringeArgOut.bits := accelArgOut.bits
      //     fringeArgOut.valid := 1.U
      // }
      fringe.io.externalEnable := false.B
      fringe.io.memStreams <> accel.io.memStreams
      accel.io.enable := fringe.io.enable
      fringe.io.done := accel.io.done
      fringe.reset := ~reset.toBool
      accel.reset := fringe.io.reset
      // accel.reset := ~reset.toBool
      io.is_enabled := ~accel.io.enable

    case "aws" | "aws-sim" | "VCU1525" =>
      val topIO = io.asInstanceOf[AWSInterface]
      val blockingDRAMIssue = false  // Allow only one in-flight request, block until response comes back
//      val fringe = Module(new Fringe(w, totalArgIns, totalArgOuts, numArgIOs, totalLoadStreamInfo, totalStoreStreamInfo, streamInsInfo, streamOutsInfo, blockingDRAMIssue))
      // FIXME: can we redo the Fringe such that the Instrumentation Counters instantiation in one place ? 
      // force ArgOuts and ArgInstr to zero (
      val fringe = Module(new FringeZynq(w, totalArgIns, 1 /* totalArgOuts*/, numArgIOs , numChannels, 0/*numArgInstrs*/, argOutLoopbacksMap, totalLoadStreamInfo, totalStoreStreamInfo, streamInsInfo, streamOutsInfo, blockingDRAMIssue, topIO.axiLiteParams, topIO.axiParams))

      // Fringe <-> DRAM connections
//      topIO.dram <> fringe.io.dram
      topIO.M_AXI <> fringe.io.M_AXI
      fringe.io.memStreams <> accel.io.memStreams

      // Accel: Scalar and control connections
      accel.io.argIns := topIO.scalarIns


      // topIO.scalarOuts.zip(accel.io.argOuts) foreach { case (ioOut, accelOut) => ioOut := getFF(accelOut.bits, accelOut.valid) }
      topIO.scalarOuts.zipWithIndex.foreach { case (ioOut, i) => 
	if ( i < (numArgOuts - numArgInstrs)) {
	  ioOut := getFF(accel.io.argOuts(i).bits, accel.io.argOuts(i).valid)
        } else {
	  val ic = Module(new InstrumentationCounter()) 
	  ioOut := getFF(ic.io.count, 1.U)
	  ic.io.enable := accel.io.argOuts(i).valid
        }
      }
      accel.io.enable := topIO.enable
      topIO.done := accel.io.done
      accel.io.reset := fringe.io.reset

      fringe.io.externalEnable := topIO.enable
//      topIO.dbg <> fringe.io.dbg

    case _ =>
      throw new Exception(s"Unknown target '$target'")
  }

  // TBD: Accel <-> Fringe Memory connections
//  for (i <- 0 until numMemoryStreams) {
//    fringe.io.memStreams(i).cmd.bits := accel.io.memStreams(i).cmd.bits
//    fringe.io.memStreams(i).cmd.valid := accel.io.memStreams(i).cmd.valid
//    fringe.io.memStreams(i).wdata.bits := accel.io.memStreams(i).wdata.bits
//    fringe.io.memStreams(i).wdata.valid := accel.io.memStreams(i).wdata.valid
//    accel.io.memStreams(i).rdata.bits := fringe.io.memStreams(i).rdata.bits
//    accel.io.memStreams(i).rdata.valid := fringe.io.memStreams(i).rdata.valid
//  }

}
