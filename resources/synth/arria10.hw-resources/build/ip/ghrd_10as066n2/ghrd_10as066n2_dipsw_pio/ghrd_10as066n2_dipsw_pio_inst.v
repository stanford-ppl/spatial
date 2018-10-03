	ghrd_10as066n2_dipsw_pio u0 (
		.clk        (_connected_to_clk_),        //   input,   width = 1,                 clk.clk
		.in_port    (_connected_to_in_port_),    //   input,   width = 4, external_connection.export
		.irq        (_connected_to_irq_),        //  output,   width = 1,                 irq.irq
		.reset_n    (_connected_to_reset_n_),    //   input,   width = 1,               reset.reset_n
		.address    (_connected_to_address_),    //   input,   width = 2,                  s1.address
		.write_n    (_connected_to_write_n_),    //   input,   width = 1,                    .write_n
		.writedata  (_connected_to_writedata_),  //   input,  width = 32,                    .writedata
		.chipselect (_connected_to_chipselect_), //   input,   width = 1,                    .chipselect
		.readdata   (_connected_to_readdata_)    //  output,  width = 32,                    .readdata
	);

