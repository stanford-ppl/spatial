// See LICENSE for license details.
package fringe.templates.axi4

import fringe.utils.GenericParameterizedBundle
import chisel3._
import chisel3.util.{Cat, Irrevocable}

abstract class AXI4BundleBase(params: AXI4BundleParameters) extends GenericParameterizedBundle(params)

abstract class AXI4BundleA(params: AXI4BundleParameters) extends AXI4BundleBase(params) {
  val id     = UInt(params.idBits.W)
  val addr   = UInt(params.addrBits.W)
  val len    = UInt(params.lenBits.W)  // number of beats - 1
  val size   = UInt(params.sizeBits.W) // bytes in beat = 2^size
  val burst  = UInt(params.burstBits.W)
  val lock   = UInt(params.lockBits.W)
  val cache  = UInt(params.cacheBits.W)
  val prot   = UInt(params.protBits.W)
  val qos    = UInt(params.qosBits.W)  // 0=no QoS, bigger = higher priority
  // val region = UInt(width = 4) // optional

  // Number of bytes-1 in this operation
  def bytes1(x: Int = 0): Bits = {
    val maxShift = 1 << params.sizeBits
    val tail = ((BigInt(1) << maxShift) - 1).U
    (Cat(len, tail) << size) >> maxShift
  }
}

// A non-standard bundle that can be both AR and AW
class AXI4BundleARW(params: AXI4BundleParameters) extends AXI4BundleA(params) {
  val wen = Bool()
}

class AXI4BundleAW(params: AXI4BundleParameters) extends AXI4BundleA(params)
class AXI4BundleAR(params: AXI4BundleParameters) extends AXI4BundleA(params)

class AXI4BundleW(params: AXI4BundleParameters) extends AXI4BundleBase(params) {
  // id ... removed in AXI4
  val data = UInt(params.dataBits.W)
  val strb = UInt((params.dataBits/8).W)
  val last = Bool()
}

class AXI4BundleR(params: AXI4BundleParameters) extends AXI4BundleBase(params) {
  val id   = UInt(params.idBits.W)
  val data = UInt(params.dataBits.W)
  val resp = UInt(params.respBits.W)
  val last = Bool()
}

class AXI4BundleB(params: AXI4BundleParameters) extends AXI4BundleBase(params) {
  val id   = UInt(params.idBits.W)
  val resp = UInt(params.respBits.W)
}

class AXI4Bundle(params: AXI4BundleParameters) extends AXI4BundleBase(params) {
  val aw = Irrevocable(new AXI4BundleAW(params))
  val w  = Irrevocable(new AXI4BundleW (params))
  val b  = Flipped(Irrevocable(new AXI4BundleB (params)))
  val ar = Irrevocable(new AXI4BundleAR(params))
  val r  = Flipped(Irrevocable(new AXI4BundleR (params)))
}
object AXI4Bundle {
  def apply(params: AXI4BundleParameters) = new AXI4Bundle(params)
}

/** Inlined AXI4 interface definition, same as 'AXI4Bundle'. Inlining helps Vivado
  * to auto-detect AXI4 and hence enables using block connection automation features
  */
class AXI4Inlined(params: AXI4BundleParameters) extends AXI4BundleBase(params) {
  // aw
  val AWID     = Output(UInt(params.idBits.W))
  val AWUSER   = Output(UInt(params.addrBits.W))
  val AWADDR   = Output(UInt(params.addrBits.W))
  val AWLEN    = Output(UInt(params.lenBits.W))  // number of beats - 1
  val AWSIZE   = Output(UInt(params.sizeBits.W)) // bytes in beat = 2^size
  val AWBURST  = Output(UInt(params.burstBits.W))
  val AWLOCK   = Output(UInt(params.lockBits.W))
  val AWCACHE  = Output(UInt(params.cacheBits.W))
  val AWPROT   = Output(UInt(params.protBits.W))
  val AWQOS    = Output(UInt(params.qosBits.W))  // 0=no QoS, bigger = higher priority
  val AWVALID  = Output(Bool())
  val AWREADY  = Input(Bool())

  // ar
  val ARID     = Output(UInt(params.idBits.W))
  val ARUSER   = Output(UInt(params.addrBits.W))
  val ARADDR   = Output(UInt(params.addrBits.W))
  val ARLEN    = Output(UInt(params.lenBits.W))  // number of beats - 1
  val ARSIZE   = Output(UInt(params.sizeBits.W)) // bytes in beat = 2^size
  val ARBURST  = Output(UInt(params.burstBits.W))
  val ARLOCK   = Output(UInt(params.lockBits.W))
  val ARCACHE  = Output(UInt(params.cacheBits.W))
  val ARPROT   = Output(UInt(params.protBits.W))
  val ARQOS    = Output(UInt(params.qosBits.W))  // 0=no QoS, bigger = higher priority
  val ARVALID  = Output(Bool())
  val ARREADY  = Input(Bool())


  // w
  val WDATA = Output(UInt(params.dataBits.W))
  val WSTRB = Output(UInt((params.dataBits/8).W))
  val WLAST = Output(Bool())
  val WVALID  = Output(Bool())
  val WREADY  = Input(Bool())

  // r: Input
  val RID   = Input(UInt(params.idBits.W))
  val RUSER = Input(UInt(params.addrBits.W))
  val RDATA = Input(UInt(params.dataBits.W))
  val RRESP = Input(UInt(params.respBits.W))
  val RLAST = Input(Bool())
  val RVALID  = Input(Bool())
  val RREADY  = Output(Bool())

  // b: Input
  val BID   = Input(UInt(params.idBits.W))
  val BUSER = Input(UInt(params.addrBits.W))
  val BRESP = Input(UInt(params.respBits.W))
  val BVALID  = Input(Bool())
  val BREADY  = Output(Bool())
}

class AXI4Lite(params: AXI4BundleParameters) extends AXI4BundleBase(params) {
  // aw
  val AWADDR   = Output(UInt(params.addrBits.W))
  val AWPROT   = Output(UInt(params.protBits.W))
  val AWVALID  = Output(Bool())
  val AWREADY  = Input(Bool())

  // ar
  val ARADDR   = Output(UInt(params.addrBits.W))
  val ARPROT   = Output(UInt(params.protBits.W))
  val ARVALID  = Output(Bool())
  val ARREADY  = Input(Bool())

  // w
  val WDATA   = Output(UInt(params.dataBits.W))
  val WSTRB   = Output(UInt((params.dataBits/8).W))
  val WVALID  = Output(Bool())
  val WREADY  = Input(Bool())

  // r: Input
  val RDATA   = Input(UInt(params.dataBits.W))
  val RRESP   = Input(UInt(params.respBits.W))
  val RVALID  = Input(Bool())
  val RREADY  = Output(Bool())

  // b: Input
  val BRESP   = Input(UInt(params.respBits.W))
  val BVALID  = Input(Bool())
  val BREADY  = Output(Bool())
}

// AXI Bus Prober
class AXI4Probe(params: AXI4BundleParameters) extends AXI4BundleBase(params) {
  // aw
  val AWID     = Input(UInt(params.idBits.W))
  val AWUSER   = Input(UInt(params.addrBits.W))
  val AWADDR   = Input(UInt(params.addrBits.W))
  val AWLEN    = Input(UInt(params.lenBits.W))  // number of beats - 1
  val AWSIZE   = Input(UInt(params.sizeBits.W)) // bytes in beat = 2^size
  val AWBURST  = Input(UInt(params.burstBits.W))
  val AWLOCK   = Input(UInt(params.lockBits.W))
  val AWCACHE  = Input(UInt(params.cacheBits.W))
  val AWPROT   = Input(UInt(params.protBits.W))
  val AWQOS    = Input(UInt(params.qosBits.W))  // 0=no QoS, bigger = higher priority
  val AWVALID  = Input(Bool())
  val AWREADY  = Input(Bool())

  // ar
  val ARID     = Input(UInt(params.idBits.W))
  val ARUSER   = Input(UInt(params.addrBits.W))
  val ARADDR   = Input(UInt(params.addrBits.W))
  val ARLEN    = Input(UInt(params.lenBits.W))  // number of beats - 1
  val ARSIZE   = Input(UInt(params.sizeBits.W)) // bytes in beat = 2^size
  val ARBURST  = Input(UInt(params.burstBits.W))
  val ARLOCK   = Input(UInt(params.lockBits.W))
  val ARCACHE  = Input(UInt(params.cacheBits.W))
  val ARPROT   = Input(UInt(params.protBits.W))
  val ARQOS    = Input(UInt(params.qosBits.W))  // 0=no QoS, bigger = higher priority
  val ARVALID  = Input(Bool())
  val ARREADY  = Input(Bool())


  // w
  val WDATA   = Input(UInt(params.dataBits.W))
  val WSTRB   = Input(UInt(64.W))
  val WLAST   = Input(Bool())
  val WVALID  = Input(Bool())
  val WREADY  = Input(Bool())

  // r: Input
  val RID     = Input(UInt(params.idBits.W))
  val RUSER   = Input(UInt(params.addrBits.W))
  val RDATA   = Input(UInt(params.dataBits.W))
  val RRESP   = Input(UInt(params.respBits.W))
  val RLAST   = Input(Bool())
  val RVALID  = Input(Bool())
  val RREADY  = Input(Bool())

  // b: Input
  val BID     = Input(UInt(params.idBits.W))
  val BUSER   = Input(UInt(params.addrBits.W))
  val BRESP   = Input(UInt(params.respBits.W))
  val BVALID  = Input(Bool())
  val BREADY  = Input(Bool())
}


// Avalon Slave interface
class AvalonSlave(params: AXI4BundleParameters) extends AXI4BundleBase(params) {
  val readdata = Output(UInt(params.dataBits.W ))
  val address = Input(UInt(params.addrBits.W))
  val chipselect = Input(Bool())
//  val reset_n = Input(Bool())
  val write = Input(Bool())
  val read = Input(Bool())
  val writedata = Input(UInt(params.dataBits.W))
}

class AvalonMaster(params: AXI4BundleParameters) extends AXI4BundleBase(params) {
  // datawidth = 512, byteenable = 512 / 8 = 64. Right now just hard code it...
  val address = Output(UInt(params.addrBits.W)) // 32
  val byteenable = Output(UInt(64.W)) // I actually need it for wstrb
//  val debugaccess = Output(1.U)
  val read = Output(Bool())
  val readdata = Input(UInt(params.dataBits.W))
  val readdatavalid = Input(Bool())
//  val response = Input(2.U) // This signal is used for checking data integrity. Dont' really need it.
  val write = Output(Bool())
  val writedata = Output(UInt(params.dataBits.W))
  val writeresponsevalid = Input(Bool())
//  val lock = Output(1.U) // Don't need this signal since we don't arbitrate
  val waitrequest = Input(Bool())
  val burstcount = Output(UInt(params.lenBits.W))
//  val beginbursttransfer = Output(Bool()) // deprecated by Intel
  val response = Input(UInt(2.W)) // Do not connect this signal as I don't really use it.
}

// Avalon streaming interface
class AvalonStream(params: AXI4BundleParameters) extends AXI4BundleBase(params) {
  // TODO: need to parameterize these bits
  // Video Stream Inputs
  val stream_in_data            = Input(UInt(24.W))
  val stream_in_startofpacket   = Input(Bool())
  val stream_in_endofpacket     = Input(Bool())
  val stream_in_empty           = Input(UInt(2.W))
  val stream_in_valid           = Input(Bool())
  val stream_out_ready          = Input(Bool())

  // Video Stream Outputs
  val stream_in_ready           = Output(Bool())
  val stream_out_data           = Output(UInt(16.W))
  val stream_out_startofpacket  = Output(Bool())
  val stream_out_endofpacket    = Output(Bool())
  val stream_out_empty          = Output(UInt(1.W))
  val stream_out_valid          = Output(Bool())
}
