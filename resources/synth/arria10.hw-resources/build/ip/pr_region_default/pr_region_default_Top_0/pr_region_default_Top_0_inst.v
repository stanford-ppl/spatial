	pr_region_default_SpatialIP_0 u0 (
		.clock                  (_connected_to_clock_),                  //   input,    width = 1,       clock.clk
		.io_M_AXI_0_AWID        (_connected_to_io_M_AXI_0_AWID_),        //  output,    width = 6,  io_M_AXI_0.awid
		.io_M_AXI_0_AWUSER      (_connected_to_io_M_AXI_0_AWUSER_),      //  output,   width = 32,            .awuser
		.io_M_AXI_0_AWADDR      (_connected_to_io_M_AXI_0_AWADDR_),      //  output,   width = 32,            .awaddr
		.io_M_AXI_0_AWLEN       (_connected_to_io_M_AXI_0_AWLEN_),       //  output,    width = 8,            .awlen
		.io_M_AXI_0_AWSIZE      (_connected_to_io_M_AXI_0_AWSIZE_),      //  output,    width = 3,            .awsize
		.io_M_AXI_0_AWBURST     (_connected_to_io_M_AXI_0_AWBURST_),     //  output,    width = 2,            .awburst
		.io_M_AXI_0_AWLOCK      (_connected_to_io_M_AXI_0_AWLOCK_),      //  output,    width = 1,            .awlock
		.io_M_AXI_0_AWCACHE     (_connected_to_io_M_AXI_0_AWCACHE_),     //  output,    width = 4,            .awcache
		.io_M_AXI_0_AWPROT      (_connected_to_io_M_AXI_0_AWPROT_),      //  output,    width = 3,            .awprot
		.io_M_AXI_0_AWQOS       (_connected_to_io_M_AXI_0_AWQOS_),       //  output,    width = 4,            .awqos
		.io_M_AXI_0_AWVALID     (_connected_to_io_M_AXI_0_AWVALID_),     //  output,    width = 1,            .awvalid
		.io_M_AXI_0_AWREADY     (_connected_to_io_M_AXI_0_AWREADY_),     //   input,    width = 1,            .awready
		.io_M_AXI_0_ARID        (_connected_to_io_M_AXI_0_ARID_),        //  output,    width = 6,            .arid
		.io_M_AXI_0_ARUSER      (_connected_to_io_M_AXI_0_ARUSER_),      //  output,   width = 32,            .aruser
		.io_M_AXI_0_ARADDR      (_connected_to_io_M_AXI_0_ARADDR_),      //  output,   width = 32,            .araddr
		.io_M_AXI_0_ARLEN       (_connected_to_io_M_AXI_0_ARLEN_),       //  output,    width = 8,            .arlen
		.io_M_AXI_0_ARSIZE      (_connected_to_io_M_AXI_0_ARSIZE_),      //  output,    width = 3,            .arsize
		.io_M_AXI_0_ARBURST     (_connected_to_io_M_AXI_0_ARBURST_),     //  output,    width = 2,            .arburst
		.io_M_AXI_0_ARLOCK      (_connected_to_io_M_AXI_0_ARLOCK_),      //  output,    width = 1,            .arlock
		.io_M_AXI_0_ARCACHE     (_connected_to_io_M_AXI_0_ARCACHE_),     //  output,    width = 4,            .arcache
		.io_M_AXI_0_ARPROT      (_connected_to_io_M_AXI_0_ARPROT_),      //  output,    width = 3,            .arprot
		.io_M_AXI_0_ARQOS       (_connected_to_io_M_AXI_0_ARQOS_),       //  output,    width = 4,            .arqos
		.io_M_AXI_0_ARVALID     (_connected_to_io_M_AXI_0_ARVALID_),     //  output,    width = 1,            .arvalid
		.io_M_AXI_0_ARREADY     (_connected_to_io_M_AXI_0_ARREADY_),     //   input,    width = 1,            .arready
		.io_M_AXI_0_WDATA       (_connected_to_io_M_AXI_0_WDATA_),       //  output,  width = 512,            .wdata
		.io_M_AXI_0_WSTRB       (_connected_to_io_M_AXI_0_WSTRB_),       //  output,   width = 64,            .wstrb
		.io_M_AXI_0_WLAST       (_connected_to_io_M_AXI_0_WLAST_),       //  output,    width = 1,            .wlast
		.io_M_AXI_0_WVALID      (_connected_to_io_M_AXI_0_WVALID_),      //  output,    width = 1,            .wvalid
		.io_M_AXI_0_WREADY      (_connected_to_io_M_AXI_0_WREADY_),      //   input,    width = 1,            .wready
		.io_M_AXI_0_RID         (_connected_to_io_M_AXI_0_RID_),         //   input,    width = 6,            .rid
		.io_M_AXI_0_RUSER       (_connected_to_io_M_AXI_0_RUSER_),       //   input,   width = 32,            .ruser
		.io_M_AXI_0_RDATA       (_connected_to_io_M_AXI_0_RDATA_),       //   input,  width = 512,            .rdata
		.io_M_AXI_0_RRESP       (_connected_to_io_M_AXI_0_RRESP_),       //   input,    width = 2,            .rresp
		.io_M_AXI_0_RLAST       (_connected_to_io_M_AXI_0_RLAST_),       //   input,    width = 1,            .rlast
		.io_M_AXI_0_RVALID      (_connected_to_io_M_AXI_0_RVALID_),      //   input,    width = 1,            .rvalid
		.io_M_AXI_0_RREADY      (_connected_to_io_M_AXI_0_RREADY_),      //  output,    width = 1,            .rready
		.io_M_AXI_0_BID         (_connected_to_io_M_AXI_0_BID_),         //   input,    width = 6,            .bid
		.io_M_AXI_0_BUSER       (_connected_to_io_M_AXI_0_BUSER_),       //   input,   width = 32,            .buser
		.io_M_AXI_0_BRESP       (_connected_to_io_M_AXI_0_BRESP_),       //   input,    width = 2,            .bresp
		.io_M_AXI_0_BVALID      (_connected_to_io_M_AXI_0_BVALID_),      //   input,    width = 1,            .bvalid
		.io_M_AXI_0_BREADY      (_connected_to_io_M_AXI_0_BREADY_),      //  output,    width = 1,            .bready
		.io_S_AVALON_address    (_connected_to_io_S_AVALON_address_),    //   input,    width = 7, io_S_AVALON.address
		.io_S_AVALON_readdata   (_connected_to_io_S_AVALON_readdata_),   //  output,   width = 32,            .readdata
		.io_S_AVALON_chipselect (_connected_to_io_S_AVALON_chipselect_), //   input,    width = 1,            .chipselect
		.io_S_AVALON_write      (_connected_to_io_S_AVALON_write_),      //   input,    width = 1,            .write
		.io_S_AVALON_read       (_connected_to_io_S_AVALON_read_),       //   input,    width = 1,            .read
		.io_S_AVALON_writedata  (_connected_to_io_S_AVALON_writedata_),  //   input,   width = 32,            .writedata
		.reset                  (_connected_to_reset_)                   //   input,    width = 1,       reset.reset
	);

