module ghrd_10as066n2_avlmm_pr_freeze_bridge_1 (
		input  wire        clock,                               //            clock.clk
		input  wire        freeze_conduit_freeze,               //   freeze_conduit.freeze
		output wire        freeze_conduit_illegal_request,      //                 .illegal_request
		input  wire        mst_bridge_to_pr_read,               // mst_bridge_to_pr.read
		output wire        mst_bridge_to_pr_waitrequest,        //                 .waitrequest
		input  wire        mst_bridge_to_pr_write,              //                 .write
		input  wire [31:0] mst_bridge_to_pr_address,            //                 .address
		input  wire [3:0]  mst_bridge_to_pr_byteenable,         //                 .byteenable
		input  wire [31:0] mst_bridge_to_pr_writedata,          //                 .writedata
		output wire [31:0] mst_bridge_to_pr_readdata,           //                 .readdata
		input  wire [2:0]  mst_bridge_to_pr_burstcount,         //                 .burstcount
		output wire        mst_bridge_to_pr_readdatavalid,      //                 .readdatavalid
		input  wire        mst_bridge_to_pr_beginbursttransfer, //                 .beginbursttransfer
		input  wire        mst_bridge_to_pr_debugaccess,        //                 .debugaccess
		output wire [1:0]  mst_bridge_to_pr_response,           //                 .response
		input  wire        mst_bridge_to_pr_lock,               //                 .lock
		output wire        mst_bridge_to_pr_writeresponsevalid, //                 .writeresponsevalid
		output wire        mst_bridge_to_sr_read,               // mst_bridge_to_sr.read
		input  wire        mst_bridge_to_sr_waitrequest,        //                 .waitrequest
		output wire        mst_bridge_to_sr_write,              //                 .write
		output wire [31:0] mst_bridge_to_sr_address,            //                 .address
		output wire [3:0]  mst_bridge_to_sr_byteenable,         //                 .byteenable
		output wire [31:0] mst_bridge_to_sr_writedata,          //                 .writedata
		input  wire [31:0] mst_bridge_to_sr_readdata,           //                 .readdata
		output wire [2:0]  mst_bridge_to_sr_burstcount,         //                 .burstcount
		input  wire        mst_bridge_to_sr_readdatavalid,      //                 .readdatavalid
		output wire        mst_bridge_to_sr_beginbursttransfer, //                 .beginbursttransfer
		output wire        mst_bridge_to_sr_debugaccess,        //                 .debugaccess
		input  wire [1:0]  mst_bridge_to_sr_response,           //                 .response
		output wire        mst_bridge_to_sr_lock,               //                 .lock
		input  wire        mst_bridge_to_sr_writeresponsevalid, //                 .writeresponsevalid
		input  wire        reset_n                              //          reset_n.reset_n
	);
endmodule

