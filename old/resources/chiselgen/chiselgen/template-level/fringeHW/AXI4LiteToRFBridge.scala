package fringe

import chisel3._
import chisel3.util._
import axi4._

class AXI4LiteToRFBridgeVerilog(val addrWidth: Int, val dataWidth: Int) extends BlackBox {
  val idBits = 1 // AXI-Lite does not have ID field
  val p = new AXI4BundleParameters(addrWidth, dataWidth, idBits)

  val io = IO(new Bundle {
    val S_AXI = Flipped(new AXI4Lite(p))
    val S_AXI_ACLK = Input(Clock())
    val S_AXI_ARESETN = Input(Bool())
    val rf_raddr = Output(UInt(addrWidth.W))
    val rf_wen   = Output(Bool())
    val rf_waddr = Output(UInt(addrWidth.W))
    val rf_wdata = Output(Bits(dataWidth.W))
    val rf_rdata = Input(Bits(dataWidth.W))

  })
}


class AXI4LiteToRFBridge(val addrWidth: Int, val dataWidth: Int) extends Module {
  val idBits = 1 // AXI-Lite does not have ID field
  val p = new AXI4BundleParameters(addrWidth, dataWidth, idBits)

  val io = IO(new Bundle {
    val S_AXI = Flipped(new AXI4Lite(p))
    val raddr = Output(UInt(addrWidth.W))
    val wen   = Output(Bool())
    val waddr = Output(UInt(addrWidth.W))
    val wdata = Output(Bits(dataWidth.W))
    val rdata = Input(Bits(dataWidth.W))
  })

  val d = Module(new AXI4LiteToRFBridgeVerilog(addrWidth, dataWidth))

  d.io.S_AXI <> io.S_AXI
  d.io.S_AXI_ACLK := clock
  d.io.S_AXI_ARESETN := ~reset.toBool

  io.raddr := d.io.rf_raddr
  io.waddr := d.io.rf_waddr
  io.wdata := d.io.rf_wdata
  io.wen   := d.io.rf_wen
  d.io.rf_rdata := io.rdata
}



class AXI4LiteToRFBridgeZCUVerilog(val addrWidth: Int, val dataWidth: Int) extends BlackBox {
  val idBits = 1 // AXI-Lite does not have ID field
  val p = new AXI4BundleParameters(addrWidth, dataWidth, idBits)

  val io = IO(new Bundle {
    val S_AXI = Flipped(new AXI4Lite(p))
    val S_AXI_ACLK = Input(Clock())
    val S_AXI_ARESETN = Input(Bool())
    val rf_raddr = Output(UInt(addrWidth.W))
    val rf_wen   = Output(Bool())
    val rf_waddr = Output(UInt(addrWidth.W))
    val rf_wdata = Output(Bits(dataWidth.W))
    val rf_rdata = Input(Bits(dataWidth.W))

  })
}


class AXI4LiteToRFBridgeZCU(val addrWidth: Int, val dataWidth: Int) extends Module {
  val idBits = 1 // AXI-Lite does not have ID field
  val p = new AXI4BundleParameters(addrWidth, dataWidth, idBits)

  val io = IO(new Bundle {
    val S_AXI = Flipped(new AXI4Lite(p))
    val raddr = Output(UInt(addrWidth.W))
    val wen   = Output(Bool())
    val waddr = Output(UInt(addrWidth.W))
    val wdata = Output(Bits(dataWidth.W))
    val rdata = Input(Bits(dataWidth.W))
  })

  val d = Module(new AXI4LiteToRFBridgeZCUVerilog(addrWidth, dataWidth))

  d.io.S_AXI <> io.S_AXI
  d.io.S_AXI_ACLK := clock
  d.io.S_AXI_ARESETN := ~reset.toBool

  io.raddr := d.io.rf_raddr
  io.waddr := d.io.rf_waddr
  io.wdata := d.io.rf_wdata
  io.wen   := d.io.rf_wen
  d.io.rf_rdata := io.rdata
}




//class AXI4LiteToRFBridge(val addrWidth: Int, val dataWidth: Int) extends BlackBox {
//  val idBits = 1 // AXI-Lite does not have ID field
//  val p = new AXI4BundleParameters(addrWidth, dataWidth, idBits)
//
//  val io = IO(new Bundle {
//    // Slave: AXI4 lite
//    val s_in = Flipped(new AXI4Lite(p))
//
//    // Master: RF interface
//    val raddr = Output(UInt(addrWidth.W))
//    val wen  = Output(Bool())
//    val waddr = Output(UInt(addrWidth.W))
//    val wdata = Output(Bits(dataWidth.W))
//    val rdata = Input(Bits(dataWidth.W))
//
//  })
//
////	// AXI4LITE signals
////	val axi_awaddr    = Reg(UInt(addrWidth.W))
////  val axi_awready   = Reg(Bool())
////  val axi_wready    = Reg(Bool())
////  val axi_bresp     = Reg(UInt(2.W))
////  val axi_bvalid    = Reg(Bool())
////  val axi_araddr    = Reg(UInt(addrWidth.W))
////  val axi_arready   = Reg(Bool())
////  val axi_rdata     = Reg(UInt(dataWidth.W))
////  val axi_rresp     = Reg(UInt(2.W))
////  val axi_rvalid    = Reg(Bool())
////
////	io.s_in.awready	:= axi_awready;
////	io.s_in.wready	:= axi_wready;
////	io.s_in.bresp		:= axi_bresp;
////	io.s_in.bvalid	:= axi_bvalid;
////	io.s_in.arready	:= axi_arready;
////	io.s_in.rdata		:= axi_rdata;
////	io.s_in.rresp		:= axi_rresp;
////	io.s_in.rvalid	:= axi_rvalid;
////
////  // awready
////	when (reset) {
////		axi_awready := 0.U
////	}.otherwise {
////		when (~axi_awready && io.s_in.awvalid & io.s_in.wvalid) {
////			axi_awready := 1.U
////		}.otherwise {
////			axi_awready := 0.U
////		}
////  }
////
////  // awaddr
////	when (reset) {
////		axi_awaddr := 0.U
////	}.otherwise {
////		when (~axi_awready && io.s_in.awvalid && io.s_in.wvalid) {
////			axi_awaddr := io.s_in.awaddr
////		}
////  }
////
////  // wready
////	when (reset) {
////		axi_wready := 0.U
////	}.otherwise {
////		when (~axi_wready && io.s_in.wvalid && io.s_in.awvalid) {
////			axi_wready := 1.U
////		}.otherwise {
////      axi_wready := 0.U
////    }
////	}
////
////  io.wen := axi_wready && io.s_in.wvalid && axi_awready && io.s_in.awvalid;
////
////  // bresp, bvalid
////	when (reset) {
////		axi_bvalid  := 0.U;
////		axi_bresp   := 0.U;
////	}.otherwise {
////		when (axi_awready && io.s_in.awvalid && ~axi_bvalid && axi_wready && io.s_in.wvalid) {
////			axi_bvalid := 1.U
////			axi_bresp  := 0.U
////		}.elsewhen(io.s_in.bready && axi_bvalid) {
////			axi_bvalid := 0.U
////    }
////	}
////
////  // arready, araddr
////	when (reset) {
////		axi_arready := 0.U
////		axi_araddr := 0.U
////	}.otherwise {
////		when (~axi_arready && io.s_in.arvalid) {
////			axi_arready := 1.U
////			axi_araddr := io.s_in.araddr
////		}.otherwise {
////			axi_arready := 0.U
////		}
////	}
////
////  // rresp, rvalid
////	when (reset) {
////		axi_rvalid := 0.U
////		axi_rresp  := 0.U
////	}.otherwise {
////		when (axi_arready && io.s_in.arvalid && ~axi_rvalid) {
////			axi_rvalid := 1.U
////			axi_rresp  := 0.U
////		}.elsewhen (axi_rvalid && io.s_in.rready) {
////			axi_rvalid := 0.U
////		}
////	}
////
////	io.raddr := axi_araddr
////  io.waddr := axi_awaddr
////  io.wdata := io.s_in.wdata
////  axi_rdata := io.rdata
//}
