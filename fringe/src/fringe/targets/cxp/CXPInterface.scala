package fringe.targets.cxp

import chisel3._
import chisel3.util.Decoupled
import fringe.globals._
import fringe._
import fringe.TopInterface
import fringe.templates.axi4.{AXI4BundleParameters, AXI4Inlined, AXI4Lite, AXI4Probe}
import fringe.templates.axi4._
import fringe.templates.euresys.{CXPStream}

class CXPInterface extends TopInterface {
  val axiParams = new AXI4BundleParameters(32, 256, 32)

  val M_AXI = Vec(NUM_CHANNELS, new AXI4Inlined(axiParams))

  // val METADATA_IN = Input(new Metadata_rec())
  // val METADATA_OUT = Output(new Metadata_rec())
  // val CUSTOMLOGIC_EVENT = Output(Bool())
  // val CUSTOMLOGIC_EVENT_ARG0 = Output(UInt(32.W))
  // val CUSTOMLOGIC_EVENT_ARG1 = Output(UInt(32.W))
  val CUSTOMLOGIC_CTRL_ADDR = Input(UInt(16.W))
  val CUSTOMLOGIC_CTRL_DATA_IN_CE = Input(Bool())
  val CUSTOMLOGIC_CTRL_DATA_IN = Input(UInt(32.W))
  val CUSTOMLOGIC_CTRL_DATA_OUT = Output(UInt(32.W))

  val PIPELINECLEAR = Input(Bool())
  val AXIS_IN = Flipped(Decoupled(new CXPStream()))
  val AXIS_OUT = Decoupled(new CXPStream())
}
