package fringe.targets.verilator

import chisel3._
import fringe.TopInterface
import fringe.globals._

class FringelessInterface extends TopInterface {
  // Host scalar interface
  raddr = Input(UInt(ADDR_WIDTH.W))
  wen   = Input(Bool())
  waddr = Input(UInt(ADDR_WIDTH.W))
  wdata = Input(Bits(64.W))
  rdata = Output(Bits(64.W))

}
