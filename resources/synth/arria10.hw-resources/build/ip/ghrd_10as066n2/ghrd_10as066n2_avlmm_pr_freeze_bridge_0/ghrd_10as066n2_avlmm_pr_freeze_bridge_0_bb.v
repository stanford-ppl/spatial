module ghrd_10as066n2_avlmm_pr_freeze_bridge_0 (
		input  wire        clock,                               //            clock.clk
		input  wire        freeze_conduit_freeze,               //   freeze_conduit.freeze
		output wire        freeze_conduit_illegal_request,      //                 .illegal_request
		input  wire        reset_n,                             //          reset_n.reset_n
		output wire        slv_bridge_to_pr_read,               // slv_bridge_to_pr.read
		input  wire        slv_bridge_to_pr_waitrequest,        //                 .waitrequest
		output wire        slv_bridge_to_pr_write,              //                 .write
		output wire [9:0]  slv_bridge_to_pr_address,            //                 .address
		output wire [3:0]  slv_bridge_to_pr_byteenable,         //                 .byteenable
		output wire [31:0] slv_bridge_to_pr_writedata,          //                 .writedata
		input  wire [31:0] slv_bridge_to_pr_readdata,           //                 .readdata
		output wire [2:0]  slv_bridge_to_pr_burstcount,         //                 .burstcount
		input  wire        slv_bridge_to_pr_readdatavalid,      //                 .readdatavalid
		output wire        slv_bridge_to_pr_beginbursttransfer, //                 .beginbursttransfer
		output wire        slv_bridge_to_pr_debugaccess,        //                 .debugaccess
		input  wire [1:0]  slv_bridge_to_pr_response,           //                 .response
		output wire        slv_bridge_to_pr_lock,               //                 .lock
		input  wire        slv_bridge_to_pr_writeresponsevalid, //                 .writeresponsevalid
		input  wire        slv_bridge_to_sr_read,               // slv_bridge_to_sr.read
		output wire        slv_bridge_to_sr_waitrequest,        //                 .waitrequest
		input  wire        slv_bridge_to_sr_write,              //                 .write
		input  wire [9:0]  slv_bridge_to_sr_address,            //                 .address
		input  wire [3:0]  slv_bridge_to_sr_byteenable,         //                 .byteenable
		input  wire [31:0] slv_bridge_to_sr_writedata,          //                 .writedata
		output wire [31:0] slv_bridge_to_sr_readdata,           //                 .readdata
		input  wire [2:0]  slv_bridge_to_sr_burstcount,         //                 .burstcount
		output wire        slv_bridge_to_sr_readdatavalid,      //                 .readdatavalid
		input  wire        slv_bridge_to_sr_beginbursttransfer, //                 .beginbursttransfer
		input  wire        slv_bridge_to_sr_debugaccess,        //                 .debugaccess
		output wire [1:0]  slv_bridge_to_sr_response,           //                 .response
		input  wire        slv_bridge_to_sr_lock,               //                 .lock
		output wire        slv_bridge_to_sr_writeresponsevalid  //                 .writeresponsevalid
	);
endmodule

