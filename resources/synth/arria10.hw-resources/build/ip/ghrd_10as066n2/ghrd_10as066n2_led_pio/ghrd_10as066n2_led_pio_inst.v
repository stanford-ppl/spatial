	ghrd_10as066n2_led_pio u0 (
		.clk        (_connected_to_clk_),        //   input,   width = 1,                 clk.clk
		.in_port    (_connected_to_in_port_),    //   input,   width = 4, external_connection.in_port
		.out_port   (_connected_to_out_port_),   //  output,   width = 4,                    .out_port
		.reset_n    (_connected_to_reset_n_),    //   input,   width = 1,               reset.reset_n
		.address    (_connected_to_address_),    //   input,   width = 2,                  s1.address
		.write_n    (_connected_to_write_n_),    //   input,   width = 1,                    .write_n
		.writedata  (_connected_to_writedata_),  //   input,  width = 32,                    .writedata
		.chipselect (_connected_to_chipselect_), //   input,   width = 1,                    .chipselect
		.readdata   (_connected_to_readdata_)    //  output,  width = 32,                    .readdata
	);

