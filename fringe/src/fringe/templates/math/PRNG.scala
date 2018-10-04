// See LICENSE.txt for license details.
package fringe.templates.math

import chisel3._

class PRNG(val seed: Int, val bitWidth:Int = 32) extends Module {

  val io = IO( new Bundle {
    val en = Input(Bool())
    val output = Output(UInt(bitWidth.W))
  })

  // Single Cycle Version
  val reg = RegInit(seed.U(bitWidth.W))
  val shifted = ((reg ^ (reg << 1)) ^ ((reg ^ (reg << 1)) >> 3)) ^ (((reg ^ (reg << 1)) ^ ((reg ^ (reg << 1)) >> 3)) << 10)
  reg := Mux(io.en, shifted, reg)

  // // 3 Cycle Version
  // val reg = RegInit(seed.U(bitWidth.W))
  // val cnt = RegInit(0.U(2.W))
  // cnt := Mux(io.en, Mux(cnt === 2.U, 0.U, cnt + 1.U), cnt)
  // val shift_options = List((0.U -> {reg << 1}), (1.U -> {reg >> 3}), (2.U -> {reg << 10}))
  // reg := Mux(io.en, reg ^ chisel3.util.MuxLookup(cnt, 0.U, shift_options), reg)

  io.output := reg
}
