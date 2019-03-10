package fringe.templates.axi4

import chisel3._
import chisel3.util.Cat
import fringe.globals
import fringe.{DRAMStream}

class MAGToAvalonBridge(val p: AXI4BundleParameters) extends Module {
  Predef.assert(
    p.dataBits == 512,
    s"ERROR: Unsupported data width ${p.dataBits} in MAGToAvalonBridge"
  )

  val io = IO(new Bundle {
    val in = Flipped(
      new DRAMStream(globals.DATA_WIDTH, globals.WORDS_PER_STREAM)
    ) // hardcoding stuff here
    val M_AVALON = new AvalonMaster(p)
  })

  io <> DontCare

  // Outputs
  val size = io.in.cmd.bits.size // 512
  io.M_AVALON.burstcount := size - 1.U
  io.M_AVALON.address := io.in.cmd.bits.addr
  io.M_AVALON.byteenable := io.in.wdata.bits.wstrb.reverse.reduce[UInt] {
    Cat(_, _)
  }
  io.M_AVALON.read := !io.in.cmd.bits.isWr
  io.M_AVALON.write := io.in.cmd.bits.isWr
  io.M_AVALON.writedata := io.in.wdata.bits.wdata.reverse.reduce { Cat(_, _) }

  // Inputs
  val rdataAsVec = VecInit(
    List
      .tabulate(globals.EXTERNAL_V) { i =>
        io.M_AVALON.readdata(
          (globals.EXTERNAL_W * globals.EXTERNAL_V) - 1 - i * globals.EXTERNAL_W,
          (globals.EXTERNAL_W * globals.EXTERNAL_V) - 1 - i * globals.EXTERNAL_W - (globals.EXTERNAL_W - 1)
        )
      }
      .reverse
  )
  io.in.rresp.bits.rdata := rdataAsVec
  io.in.cmd.ready := !io.M_AVALON.waitrequest
  io.in.rresp.valid := io.M_AVALON.readdatavalid
  io.in.wresp.valid := io.M_AVALON.writerespondvalid
  io.in.wdata.ready := !io.M_AVALON.waitrequest
}
