module ghrd_10as066n2_ocm_0 (
		input  wire        clk,        //   clk1.clk
		input  wire        reset,      // reset1.reset
		input  wire        reset_req,  //       .reset_req
		input  wire [17:0] address,    //     s1.address
		input  wire        clken,      //       .clken
		input  wire        chipselect, //       .chipselect
		input  wire        write,      //       .write
		output wire [7:0]  readdata,   //       .readdata
		input  wire [7:0]  writedata   //       .writedata
	);
endmodule

