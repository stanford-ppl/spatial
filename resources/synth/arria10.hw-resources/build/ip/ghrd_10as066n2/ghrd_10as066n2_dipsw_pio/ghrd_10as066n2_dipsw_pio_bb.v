module ghrd_10as066n2_dipsw_pio (
		input  wire        clk,        //                 clk.clk
		input  wire [3:0]  in_port,    // external_connection.export
		output wire        irq,        //                 irq.irq
		input  wire        reset_n,    //               reset.reset_n
		input  wire [1:0]  address,    //                  s1.address
		input  wire        write_n,    //                    .write_n
		input  wire [31:0] writedata,  //                    .writedata
		input  wire        chipselect, //                    .chipselect
		output wire [31:0] readdata    //                    .readdata
	);
endmodule

