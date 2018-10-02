package fringe.targets.verilator

import chisel3._
import fringe.globals._
import fringe.{DRAMStream, globals, TopInterface}
import fringe.templates.axi4.AXI4BundleParameters

class VerilatorInterface extends TopInterface {
  // Host scalar interface
  raddr = Input(UInt(ADDR_WIDTH.W))
  wen   = Input(Bool())
  waddr = Input(UInt(ADDR_WIDTH.W))
  wdata = Input(Bits(64.W))
  rdata = Output(Bits(64.W))

  // DRAM interface - currently only one stream
  val dram = Vec(NUM_CHANNELS, new DRAMStream(DATA_WIDTH, EXTERNAL_V)) // Bus is 64 elements of 8 bits
  val axiParams = new AXI4BundleParameters(DATA_WIDTH, 512, 32)

  // Input streams
  //  val genericStreamIn = StreamIn(StreamParInfo(32,1))
  //  val genericStreamOut = StreamOut(StreamParInfo(32,1))

  // Debug signals
  //  val dbg = new DebugSignals
}
