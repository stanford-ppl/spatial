package fringe.targets.de1soc

import fringe.{AbstractAccelUnit, BigIP, SpatialIPInterface}
import fringe.targets.{BigIPSim, DeviceTarget}

class DE1SoC extends DeviceTarget {
  def makeBigIP: BigIP = new BigIPSim
  override def addFringeAndCreateIP(reset: Reset, accel: AbstractAccelUnit): SpatialIPInterface = {
    val io = IO(new DE1SoCInterface)

    throw new Exception(s"Top Interface is unimplemented for DE1SoC")
    // //   // DE1SoC Fringe
    // //   val blockingDRAMIssue = false
    // //   val topIO = io.asInstanceOf[DE1SoCInterface]
    // //   val fringe = Module(new FringeDE1SoC(w, totalArgIns, totalArgOuts, numArgIOs, numChannels, numArgInstrs, argOutLoopbacksMap, totalLoadStreamInfo, totalStoreStreamInfo, streamInsInfo, streamOutsInfo, blockingDRAMIssue, topIO.axiParams))

    // //   // Fringe <-> Host connections
    // //   fringe.io.S_AVALON <> topIO.S_AVALON
    // //   // Accel <-> Stream
    // //   accel.io.stream_in_data                 := topIO.S_STREAM.stream_in_data
    // //   accel.io.stream_in_startofpacket        := topIO.S_STREAM.stream_in_startofpacket
    // //   accel.io.stream_in_endofpacket          := topIO.S_STREAM.stream_in_endofpacket
    // //   accel.io.stream_in_empty                := topIO.S_STREAM.stream_in_empty
    // //   accel.io.stream_in_valid                := topIO.S_STREAM.stream_in_valid
    // //   accel.io.stream_out_ready               := topIO.S_STREAM.stream_out_ready

    // //   // Video Stream Outputs
    // //   topIO.S_STREAM.stream_in_ready          := accel.io.stream_in_ready
    // //   topIO.S_STREAM.stream_out_data          := accel.io.stream_out_data
    // //   topIO.S_STREAM.stream_out_startofpacket := accel.io.stream_out_startofpacket
    // //   topIO.S_STREAM.stream_out_endofpacket   := accel.io.stream_out_endofpacket
    // //   topIO.S_STREAM.stream_out_empty         := accel.io.stream_out_empty
    // //   topIO.S_STREAM.stream_out_valid         := accel.io.stream_out_valid

    // //   // LED Stream Outputs
    // //   topIO.LEDR_STREAM_writedata             := accel.io.led_stream_out_data
    // //   topIO.LEDR_STREAM_chipselect            := 1.U
    // //   topIO.LEDR_STREAM_write_n               := 0.U
    // //   topIO.LEDR_STREAM_address               := 0.U

    // //   // Switch Stream Outputs
    // //   topIO.SWITCHES_STREAM_address           := 0.U
    // //   accel.io.switch_stream_in_data          := topIO.SWITCHES_STREAM_readdata
    // //   topIO.SWITCHES_STREAM_read              := 0.U

    // //   // BufferedOut Outputs
    // //   accel.io.buffout_waitrequest            := topIO.BUFFOUT_waitrequest
    // //   topIO.BUFFOUT_address                   := accel.io.buffout_address
    // //   topIO.BUFFOUT_write                     := accel.io.buffout_write
    // //   topIO.BUFFOUT_writedata                 := accel.io.buffout_writedata

    // //   // GPI1 StreamIn
    // //   topIO.GPI1_STREAMIN_chipselect          := 1.U
    // //   topIO.GPI1_STREAMIN_address             := 0.U
    // //   topIO.GPI1_STREAMIN_read                := 1.U
    // //   accel.io.gpi1_streamin_readdata         := topIO.GPI1_STREAMIN_readdata

    // //   // GPO1 StreamOut
    // //   topIO.GPO1_STREAMOUT_chipselect         := 1.U
    // //   topIO.GPO1_STREAMOUT_address            := 0.U
    // //   topIO.GPO1_STREAMOUT_writen             := 0.U
    // //   topIO.GPO1_STREAMOUT_writedata          := accel.io.gpo1_streamout_writedata

    // //   // GPI2 StreamIn
    // //   topIO.GPI2_STREAMIN_chipselect          := 1.U
    // //   topIO.GPI2_STREAMIN_address             := 0.U
    // //   topIO.GPI2_STREAMIN_read                := 1.U
    // //   accel.io.gpi2_streamin_readdata         := topIO.GPI2_STREAMIN_readdata

    // //   // GPO2 StreamOut
    // //   topIO.GPO2_STREAMOUT_chipselect         := 1.U
    // //   topIO.GPO2_STREAMOUT_address            := 0.U
    // //   topIO.GPO2_STREAMOUT_writen             := 0.U
    // //   topIO.GPO2_STREAMOUT_writedata          := accel.io.gpo2_streamout_writedata

    // //   if (accel.io.argIns.length > 0) {
    // //     accel.io.argIns := fringe.io.argIns
    // //   }

    // //   if (accel.io.argOuts.length > 0) {
    // //     fringe.io.argOuts.zip(accel.io.argOuts) foreach { case (fringeArgOut, accelArgOut) =>
    // //         fringeArgOut.bits := accelArgOut.bits
    // //         fringeArgOut.valid := accelArgOut.valid
    // //     }
    // //   }

    // //   accel.io.enable := fringe.io.enable
    // //   fringe.io.done := accel.io.done
    // //   // Top reset is connected to a rst controller on DE1SoC, which converts active low to active high
    // //   accel.reset := reset.toBool | fringe.io.reset

    // io
  }
}