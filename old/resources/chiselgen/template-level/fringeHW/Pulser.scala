package fringe

import chisel3._

/**
 * Pulser: Converts a rising edge to a 1-cycle pulse
 */
class Pulser() extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(1.W))
    val out = Output(UInt(1.W))
  })

  // val commandReg = Reg(Bits(1.W), io.in, 0.U)
  val commandReg = RegNext(io.in, 0.U)
  io.out := io.in & (commandReg ^ io.in)
}
