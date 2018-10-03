module pr_region_default_onchip_memory2_0 (
		input  wire        clk,        //   clk1.clk
		input  wire        reset,      // reset1.reset
		input  wire        reset_req,  //       .reset_req
		input  wire [6:0]  address,    //     s1.address
		input  wire        clken,      //       .clken
		input  wire        chipselect, //       .chipselect
		input  wire        write,      //       .write
		output wire [31:0] readdata,   //       .readdata
		input  wire [31:0] writedata,  //       .writedata
		input  wire [3:0]  byteenable  //       .byteenable
	);
endmodule

