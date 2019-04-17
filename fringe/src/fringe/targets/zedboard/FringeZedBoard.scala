package fringe.targets.zedboard

import fringe.targets.zynq._
import chisel3._
import chisel3.util.Decoupled
import fringe._
import fringe.globals._
import fringe.templates.axi4._

/** Top module for Zynq FPGA shell
  * @param blockingDRAMIssue TODO: What is this?
  * @param axiLiteParams TODO: What is this?
  * @param axiParams TODO: What is this?
  */
class FringeZedBoard(
  override val blockingDRAMIssue: Boolean,
  override val axiLiteParams:     AXI4BundleParameters,
  override val axiParams:         AXI4BundleParameters
) extends FringeZynq(blockingDRAMIssue, axiLiteParams, axiParams) {}
