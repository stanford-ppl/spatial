package fringe.templates.axi4

import chisel3._
import chisel3.util.Cat
import fringe.globals
import fringe.targets.DeviceTarget
import fringe.{DRAMCommandTag, DRAMStream}

class MAGToAXI4Bridge(val p: AXI4BundleParameters, val tagWidth: Int) extends Module {
  Predef.assert(p.dataBits == 512, s"ERROR: Unsupported data width ${p.dataBits} in MAGToAXI4Bridge")

  val io = IO(new Bundle {
    val in = Flipped(new DRAMStream(globals.DATA_WIDTH, globals.WORDS_PER_STREAM))  // hardcoding stuff here
    val M_AXI = new AXI4Inlined(p)
  })

  val numPipelinedLevels = globals.magPipelineDepth

  val size = io.in.cmd.bits.size
  // AR
  val id = io.in.cmd.bits.tag.asUInt()
  io.M_AXI.ARID     := id // Used to be shift registered
  io.M_AXI.ARADDR   := io.in.cmd.bits.addr // Used to be shift registered
  io.M_AXI.ARLEN    := size - 1.U // Used to be shift registered
  io.M_AXI.ARSIZE   := 6.U  // 110, for 64-byte burst size
  io.M_AXI.ARBURST  := 1.U  // INCR mode
  io.M_AXI.ARLOCK   := 0.U
  if (globals.target.isInstanceOf[fringe.targets.zcu.ZCU]) io.M_AXI.ARCACHE  := 15.U
  else io.M_AXI.ARCACHE  := 3.U  
  io.M_AXI.ARPROT   := 0.U  // Xilinx recommended value
  io.M_AXI.ARQOS    := 0.U
  io.M_AXI.ARVALID  := io.in.cmd.valid & !io.in.cmd.bits.isWr // Used to be shift registered
  io.in.cmd.ready   := Mux(io.in.cmd.bits.isWr, io.M_AXI.AWREADY, io.M_AXI.ARREADY) // Used to be shift registered

  // AW
  io.M_AXI.AWID     := id // Used to be shift registered
  io.M_AXI.AWADDR   := io.in.cmd.bits.addr // Used to be shift registered
  io.M_AXI.AWLEN    := size - 1.U // Used to be shift registered
  io.M_AXI.AWSIZE   := 6.U  // 110, for 64-byte burst size
  io.M_AXI.AWBURST  := 1.U  // INCR mode
  io.M_AXI.AWLOCK   := 0.U
  if (globals.target.isInstanceOf[fringe.targets.zcu.ZCU]) io.M_AXI.AWCACHE  := 15.U
  else io.M_AXI.AWCACHE  := 3.U  // Xilinx recommended value
  io.M_AXI.AWPROT   := 0.U  // Xilinx recommended value
  io.M_AXI.AWQOS    := 0.U
  io.M_AXI.AWVALID  := io.in.cmd.valid & io.in.cmd.bits.isWr // Used to be shift registered

  // W
  io.M_AXI.WDATA    := io.in.wdata.bits.wdata.reverse.reduce{ Cat(_,_) } // Used to be shift registered
  io.M_AXI.WSTRB    := io.in.wdata.bits.wstrb.reverse.reduce[UInt]{ Cat(_,_) }
  io.M_AXI.WLAST    := io.in.wdata.bits.wlast // Used to be shift registered
  io.M_AXI.WVALID   := io.in.wdata.valid // Used to be shift registered
  io.in.wdata.ready := io.M_AXI.WREADY // Used to be shift registered

  // R
  val rdataAsVec = Vec(List.tabulate(globals.EXTERNAL_V) { i =>
      io.M_AXI.RDATA((globals.EXTERNAL_W*globals.EXTERNAL_V) - 1 - i*globals.EXTERNAL_W, (globals.EXTERNAL_W*globals.EXTERNAL_V) - 1 - i*globals.EXTERNAL_W - (globals.EXTERNAL_W-1))
  }.reverse)
  io.in.rresp.bits.rdata := rdataAsVec // Used to be shift registered

  io.M_AXI.RREADY := io.in.rresp.ready // Used to be shift registered

  // B
  io.M_AXI.BREADY := io.in.wresp.ready // Used to be shift registered

  // MAG Response channel is currently shared between reads and writes
  io.in.rresp.valid := io.M_AXI.RVALID // Used to be shift registered
  io.in.wresp.valid := io.M_AXI.BVALID // Used to be shift registered

  io.in.rresp.bits.tag := io.M_AXI.RID.asTypeOf(new DRAMCommandTag)
  io.in.wresp.bits.tag := io.M_AXI.BID.asTypeOf(new DRAMCommandTag)
}
