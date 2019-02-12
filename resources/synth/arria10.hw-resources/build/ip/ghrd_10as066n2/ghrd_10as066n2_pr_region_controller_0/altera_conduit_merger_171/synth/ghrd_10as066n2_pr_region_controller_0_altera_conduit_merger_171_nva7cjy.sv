// (C) 2001-2017 Intel Corporation. All rights reserved.
// Your use of Intel Corporation's design tools, logic functions and other 
// software and tools, and its AMPP partner logic functions, and any output 
// files from any of the foregoing (including device programming or simulation 
// files), and any associated documentation or information are expressly subject 
// to the terms and conditions of the Intel Program License Subscription 
// Agreement, Intel FPGA IP License Agreement, or other applicable 
// license agreement, including, without limitation, that your use is for the 
// sole purpose of programming logic devices manufactured by Intel and sold by 
// Intel or its authorized distributors.  Please refer to the applicable 
// agreement for further details.


// (C) 2001-2016 Intel Corporation. All rights reserved.
// Your use of Intel Corporation's design tools, logic functions and other 
// software and tools, and its AMPP partner logic functions, and any output 
// files any of the foregoing (including device programming or simulation 
// files), and any associated documentation or information are expressly subject 
// to the terms and conditions of the Intel Program License Subscription 
// Agreement, Intel MegaCore Function License Agreement, or other applicable 
// license agreement, including, without limitation, that your use is for the 
// sole purpose of programming logic devices manufactured by Intel and sold by 
// Intel or its authorized distributors.  Please refer to the applicable 
// agreement for further details.

`timescale 1 ps / 1 ps

module ghrd_10as066n2_pr_region_controller_0_altera_conduit_merger_171_nva7cjy #(
    parameter NUM_INTF_BRIDGE   = 1
) (
    input  illegal_request0,
	output freeze0,
    input  illegal_request1,
	output freeze1,
	output pr_freeze0,
    input                        freeze_in,
    output [NUM_INTF_BRIDGE-1:0] illegal_request_out
);
	
	assign illegal_request_out = {
        illegal_request1,
        illegal_request0 
    };
	
    assign freeze1 = freeze_in;
    assign freeze0 = freeze_in;

    assign pr_freeze0 = freeze_in;

endmodule

