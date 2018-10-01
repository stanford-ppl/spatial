package fringe

import chisel3._
import chisel3.util.DecoupledIO

abstract class AccelInterface extends Bundle {
  val done:  Bool                     // Design done
  val reset: Bool                     // Design reset
  val enable: Bool                    // Design enable
  val argIns: Vec[UInt]               // Input: Vec of 64b UInts for input arguments
  val argOuts: Vec[DecoupledIO[UInt]] // Vec of 64b UInts for output arguments
  val argOutLoopbacks: Vec[UInt]      // TODO: Input: Vec of 64b UInts for ???

  val memStreams: AppStreams      // TODO: Flipped: ???
}

abstract class AbstractAccelTop extends Module {
  val io: AccelInterface
}
