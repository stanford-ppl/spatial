	ghrd_10as066n2_f2sdram0_m u0 (
		.clk_clk              (_connected_to_clk_clk_),              //   input,   width = 1,          clk.clk
		.clk_reset_reset      (_connected_to_clk_reset_reset_),      //   input,   width = 1,    clk_reset.reset
		.master_address       (_connected_to_master_address_),       //  output,  width = 32,       master.address
		.master_readdata      (_connected_to_master_readdata_),      //   input,  width = 32,             .readdata
		.master_read          (_connected_to_master_read_),          //  output,   width = 1,             .read
		.master_write         (_connected_to_master_write_),         //  output,   width = 1,             .write
		.master_writedata     (_connected_to_master_writedata_),     //  output,  width = 32,             .writedata
		.master_waitrequest   (_connected_to_master_waitrequest_),   //   input,   width = 1,             .waitrequest
		.master_readdatavalid (_connected_to_master_readdatavalid_), //   input,   width = 1,             .readdatavalid
		.master_byteenable    (_connected_to_master_byteenable_),    //  output,   width = 4,             .byteenable
		.master_reset_reset   (_connected_to_master_reset_reset_)    //  output,   width = 1, master_reset.reset
	);

