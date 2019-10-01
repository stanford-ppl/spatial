package fringe

import chisel3._
import chisel3.util._
import fringe.templates.euresys.{CXPStream}

abstract class AccelInterface extends Bundle {
  val done:  Bool                     // Design done
  val reset: Bool                     // Design reset
  val enable: Bool                    // Design enable
  val argIns: Vec[UInt]               // Input: Vec of 64b UInts for input arguments
  val argOuts: Vec[ArgOut] // Vec of 64b UInts for output arguments

  val memStreams: AppStreams      // TODO: Flipped: ???
  val heap: Vec[HeapIO]
}

class CustomAccelInterface(
  val io_w: Int, 
  val io_v: Int, 
  val io_loadStreamInfo: List[StreamParInfo], 
  val io_storeStreamInfo: List[StreamParInfo], 
  val io_gatherStreamInfo: List[StreamParInfo], 
  val io_scatterStreamInfo: List[StreamParInfo], 
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
  
  // HEAP IO
  val heap = Flipped(Vec(io_numAllocators, new HeapIO()))
  
  // Scalar IO
  val argIns = Input(Vec(io_numArgIns, UInt(64.W)))
  val argOuts = Vec(io_numArgOuts, new ArgOut())
  
  override def cloneType = (new CustomAccelInterface(io_w, io_v, io_loadStreamInfo, io_storeStreamInfo, io_gatherStreamInfo, io_scatterStreamInfo, io_numAllocators, io_numArgIns, io_numArgOuts)).asInstanceOf[this.type] // See chisel3 bug 358
}

class CXPAccelInterface(
  override val io_w: Int, 
  override val io_v: Int, 
  override val io_loadStreamInfo: List[StreamParInfo], 
  override val io_storeStreamInfo: List[StreamParInfo], 
  override val io_gatherStreamInfo: List[StreamParInfo], 
  override val io_scatterStreamInfo: List[StreamParInfo], 
  override val io_numAllocators: Int, 
  override val io_numArgIns: Int, 
  override val io_numArgOuts: Int, 
) extends CustomAccelInterface(io_w, io_v, io_loadStreamInfo, io_storeStreamInfo, io_gatherStreamInfo, io_scatterStreamInfo, io_numAllocators, io_numArgIns, io_numArgOuts){
  // Pixel Stream
  val AXIS_IN = Flipped(Decoupled(new CXPStream()))
  val AXIS_OUT = Decoupled(new CXPStream())
  
  override def cloneType = (new CXPAccelInterface(io_w, io_v, io_loadStreamInfo, io_storeStreamInfo, io_gatherStreamInfo, io_scatterStreamInfo, io_numAllocators, io_numArgIns, io_numArgOuts)).asInstanceOf[this.type] // See chisel3 bug 358
}

class BlackBoxStreamInterface( // Accel Interface for apps that have an input/output BlackBoxStream, used for experimental purposes mostly
  override val io_w: Int,
  override val io_v: Int,
  override val io_loadStreamInfo: List[StreamParInfo],
  override val io_storeStreamInfo: List[StreamParInfo],
  override val io_gatherStreamInfo: List[StreamParInfo],
  override val io_scatterStreamInfo: List[StreamParInfo],
  override val io_numAllocators: Int,
  override val io_numArgIns: Int,
  override val io_numArgOuts: Int,
  val io_stream_width: Int
) extends CustomAccelInterface(io_w, io_v, io_loadStreamInfo, io_storeStreamInfo, io_gatherStreamInfo, io_scatterStreamInfo, io_numAllocators, io_numArgIns, io_numArgOuts){
  // Generic Stream
  val STREAM_IN = Flipped(Decoupled(UInt(io_stream_width.W)))
  val STREAM_OUT = Decoupled(UInt(io_stream_width.W))

  override def cloneType = (new BlackBoxStreamInterface(io_w, io_v, io_loadStreamInfo, io_storeStreamInfo, io_gatherStreamInfo, io_scatterStreamInfo, io_numAllocators, io_numArgIns, io_numArgOuts, io_stream_width)).asInstanceOf[this.type] // See chisel3 bug 358
}


abstract class AbstractAccelTop extends Module {
  val io: AccelInterface
}
