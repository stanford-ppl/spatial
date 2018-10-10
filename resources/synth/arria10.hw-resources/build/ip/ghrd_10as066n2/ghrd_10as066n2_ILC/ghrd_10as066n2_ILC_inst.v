	ghrd_10as066n2_ILC u0 (
		.avmm_addr   (_connected_to_avmm_addr_),   //   input,   width = 6, avalon_slave.address
		.avmm_wrdata (_connected_to_avmm_wrdata_), //   input,  width = 32,             .writedata
		.avmm_write  (_connected_to_avmm_write_),  //   input,   width = 1,             .write
		.avmm_read   (_connected_to_avmm_read_),   //   input,   width = 1,             .read
		.avmm_rddata (_connected_to_avmm_rddata_), //  output,  width = 32,             .readdata
		.clk         (_connected_to_clk_),         //   input,   width = 1,          clk.clk
		.irq         (_connected_to_irq_),         //   input,   width = 2,          irq.irq
		.reset_n     (_connected_to_reset_n_)      //   input,   width = 1,      reset_n.reset_n
	);

