package fringe.targets.de1soc

import chisel3._
import fringe.globals._
import fringe.TopInterface
import fringe.templates.axi4.{AXI4BundleParameters, AvalonSlave, AvalonStream}

class DE1SoCInterface extends TopInterface {
  private val axiLiteParams = new AXI4BundleParameters(16, DATA_WIDTH, 1)
  val axiParams = new AXI4BundleParameters(DATA_WIDTH, 512, 6)

  val S_AVALON = new AvalonSlave(axiLiteParams)
  val S_STREAM = new AvalonStream(axiLiteParams)

  // For led output data
  val LEDR_STREAM_address = Output(UInt(4.W))
  val LEDR_STREAM_chipselect = Output(Wire(Bool()))
  val LEDR_STREAM_writedata = Output(UInt(32.W))
  val LEDR_STREAM_write_n = Output(Wire(Bool()))

  // For switch input data
  val SWITCHES_STREAM_address = Output(UInt(32.W))
  val SWITCHES_STREAM_readdata = Input(UInt(32.W))
  // This read control signal is not needed by the switches interface
  // By default this is a read_n signal
  // TODO: fix the naming of read control signal
  val SWITCHES_STREAM_read = Output(Wire(Bool()))

  // For BufferedOut data that goes to the pixel buffer
  val BUFFOUT_waitrequest  = Input(UInt(1.W))
  val BUFFOUT_address      = Output(UInt(32.W))
  val BUFFOUT_write        = Output(UInt(1.W))
  val BUFFOUT_writedata    = Output(UInt(16.W))

  // For GPI1 interface that reads from the JP1
  val GPI1_STREAMIN_chipselect  = Output(UInt(1.W))
  val GPI1_STREAMIN_address     = Output(UInt(4.W))
  val GPI1_STREAMIN_readdata    = Input(UInt(32.W))
  val GPI1_STREAMIN_read        = Output(UInt(1.W))

  // For GPI1 interface that writes to the JP1
  val GPO1_STREAMOUT_chipselect = Output(UInt(1.W))
  val GPO1_STREAMOUT_address    = Output(UInt(4.W))
  val GPO1_STREAMOUT_writedata  = Output(UInt(32.W))
  val GPO1_STREAMOUT_writen     = Output(UInt(1.W))

  // For GPI2 interface that reads from the JP2
  val GPI2_STREAMIN_chipselect  = Output(UInt(1.W))
  val GPI2_STREAMIN_address     = Output(UInt(4.W))
  val GPI2_STREAMIN_readdata    = Input(UInt(32.W))
  val GPI2_STREAMIN_read        = Output(UInt(1.W))

  // For GPI2 interface that writes to the JP2
  val GPO2_STREAMOUT_chipselect = Output(UInt(1.W))
  val GPO2_STREAMOUT_address    = Output(UInt(4.W))
  val GPO2_STREAMOUT_writedata  = Output(UInt(32.W))
  val GPO2_STREAMOUT_writen     = Output(UInt(1.W))
}
