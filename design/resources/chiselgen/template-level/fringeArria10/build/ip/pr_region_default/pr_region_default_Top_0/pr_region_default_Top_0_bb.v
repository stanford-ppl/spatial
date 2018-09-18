module pr_region_default_Top_0 (
		input  wire         clock,                  //       clock.clk
		output wire [5:0]   io_M_AXI_0_AWID,        //  io_M_AXI_0.awid
		output wire [31:0]  io_M_AXI_0_AWUSER,      //            .awuser
		output wire [31:0]  io_M_AXI_0_AWADDR,      //            .awaddr
		output wire [7:0]   io_M_AXI_0_AWLEN,       //            .awlen
		output wire [2:0]   io_M_AXI_0_AWSIZE,      //            .awsize
		output wire [1:0]   io_M_AXI_0_AWBURST,     //            .awburst
		output wire         io_M_AXI_0_AWLOCK,      //            .awlock
		output wire [3:0]   io_M_AXI_0_AWCACHE,     //            .awcache
		output wire [2:0]   io_M_AXI_0_AWPROT,      //            .awprot
		output wire [3:0]   io_M_AXI_0_AWQOS,       //            .awqos
		output wire         io_M_AXI_0_AWVALID,     //            .awvalid
		input  wire         io_M_AXI_0_AWREADY,     //            .awready
		output wire [5:0]   io_M_AXI_0_ARID,        //            .arid
		output wire [31:0]  io_M_AXI_0_ARUSER,      //            .aruser
		output wire [31:0]  io_M_AXI_0_ARADDR,      //            .araddr
		output wire [7:0]   io_M_AXI_0_ARLEN,       //            .arlen
		output wire [2:0]   io_M_AXI_0_ARSIZE,      //            .arsize
		output wire [1:0]   io_M_AXI_0_ARBURST,     //            .arburst
		output wire         io_M_AXI_0_ARLOCK,      //            .arlock
		output wire [3:0]   io_M_AXI_0_ARCACHE,     //            .arcache
		output wire [2:0]   io_M_AXI_0_ARPROT,      //            .arprot
		output wire [3:0]   io_M_AXI_0_ARQOS,       //            .arqos
		output wire         io_M_AXI_0_ARVALID,     //            .arvalid
		input  wire         io_M_AXI_0_ARREADY,     //            .arready
		output wire [511:0] io_M_AXI_0_WDATA,       //            .wdata
		output wire [63:0]  io_M_AXI_0_WSTRB,       //            .wstrb
		output wire         io_M_AXI_0_WLAST,       //            .wlast
		output wire         io_M_AXI_0_WVALID,      //            .wvalid
		input  wire         io_M_AXI_0_WREADY,      //            .wready
		input  wire [5:0]   io_M_AXI_0_RID,         //            .rid
		input  wire [31:0]  io_M_AXI_0_RUSER,       //            .ruser
		input  wire [511:0] io_M_AXI_0_RDATA,       //            .rdata
		input  wire [1:0]   io_M_AXI_0_RRESP,       //            .rresp
		input  wire         io_M_AXI_0_RLAST,       //            .rlast
		input  wire         io_M_AXI_0_RVALID,      //            .rvalid
		output wire         io_M_AXI_0_RREADY,      //            .rready
		input  wire [5:0]   io_M_AXI_0_BID,         //            .bid
		input  wire [31:0]  io_M_AXI_0_BUSER,       //            .buser
		input  wire [1:0]   io_M_AXI_0_BRESP,       //            .bresp
		input  wire         io_M_AXI_0_BVALID,      //            .bvalid
		output wire         io_M_AXI_0_BREADY,      //            .bready
		input  wire [6:0]   io_S_AVALON_address,    // io_S_AVALON.address
		output wire [31:0]  io_S_AVALON_readdata,   //            .readdata
		input  wire         io_S_AVALON_chipselect, //            .chipselect
		input  wire         io_S_AVALON_write,      //            .write
		input  wire         io_S_AVALON_read,       //            .read
		input  wire [31:0]  io_S_AVALON_writedata,  //            .writedata
		input  wire         reset                   //       reset.reset
	);
endmodule

