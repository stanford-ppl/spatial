package fringe

import chisel3._
import chisel3.util._
import fringe.templates.axi4.{AXI4BundleParameters, AXI4Stream}
import fringe.templates.euresys.CXPStream
import fringe.utils.HVec

abstract class AccelInterface extends Bundle {
  val done:  Bool                     // Design done
  val reset: Bool                     // Design reset
  val enable: Bool                    // Design enable
  val argIns: Vec[UInt]               // Input: Vec of 64b UInts for input arguments
  val argOuts: Vec[ArgOut] // Vec of 64b UInts for output arguments

  val memStreams: AppStreams      // TODO: Flipped: ???
  val axiStreamsIn: HVec[AXI4Stream]
  val axiStreamsOut: HVec[AXI4Stream]
  val heap: Vec[HeapIO]
}

class CustomAccelInterface(
  val io_w: Int, 
  val io_v: Int, 
  val io_loadStreamInfo: List[StreamParInfo], 
  val io_storeStreamInfo: List[StreamParInfo], 
  val io_gatherStreamInfo: List[StreamParInfo], 
  val io_scatterStreamInfo: List[StreamParInfo],
  val io_axiStreamsIn: List[AXI4BundleParameters],
  val io_axiStreamsOut: List[AXI4BundleParameters],
  val io_numAllocators: Int,
  val io_numArgIns: Int, 
  val io_numArgOuts: Int, 
) extends AccelInterface{
  // Control IO
  val enable = Input(Bool())
  val done = Output(Bool())
  val reset = Input(Bool())
  
  // DRAM IO
  val memStreams = Flipped(new AppStreams(io_loadStreamInfo, io_storeStreamInfo, io_gatherStreamInfo, io_scatterStreamInfo))

  // AXISTREAM IO
  val axiStreamsIn = {
    HVec.tabulate(io_axiStreamsIn.size) { i => new AXI4Stream(io_axiStreamsIn(i)) }
  }
  val axiStreamsOut = {
    HVec.tabulate(io_axiStreamsOut.size) { i => Flipped(new AXI4Stream(io_axiStreamsOut(i))) }
  }

  // HEAP IO
  val heap = Flipped(Vec(io_numAllocators, new HeapIO()))
  
  // Scalar IO
  val argIns = Input(Vec(io_numArgIns, UInt(64.W)))
  val argOuts = Vec(io_numArgOuts, new ArgOut())
  
  override def cloneType = new CustomAccelInterface(io_w, io_v, io_loadStreamInfo, io_storeStreamInfo, io_gatherStreamInfo, io_scatterStreamInfo, io_axiStreamsIn, io_axiStreamsOut, io_numAllocators, io_numArgIns, io_numArgOuts).asInstanceOf[this.type] // See chisel3 bug 358
}


abstract class AbstractAccelUnit extends Module {
  val io: AccelInterface
}
