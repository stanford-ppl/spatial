package fringe

import chisel3._
import chisel3.util.DecoupledIO

abstract class AccelInterface extends Bundle {
  val done:  Bool                     // Design done
  val reset: Bool                     // Design reset
  val enable: Bool                    // Design enable
  val argIns: Vec[UInt]               // Input: Vec of 64b UInts for input arguments
  val argOuts: Vec[ArgOut] // Vec of 64b UInts for output arguments

  val memStreams: AppStreams      // TODO: Flipped: ???
  val heap: Vec[HeapIO]
}

abstract class AbstractAccelTop extends Module {
  val io: AccelInterface
}
