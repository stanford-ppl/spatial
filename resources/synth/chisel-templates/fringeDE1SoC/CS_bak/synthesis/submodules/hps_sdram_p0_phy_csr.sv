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


// ******************************************************************************************************************************** 
// File name: phy_csr.v
// This file instantiates the PHY configuration and status register port
// ******************************************************************************************************************************** 


// PHY CSR ports. The CSR port operates where all reads respond in
// the next clock cycle and only ever take 1 clock cycle. The writes are cached
// and are processed in the following clock cycle to when they are initially
// captured.

// altera message_off 10036

//synopsys translate_off
`timescale 1 ps / 1 ps
//synopsys translate_on

module hps_sdram_p0_phy_csr(
	clk,
	reset_n,

	csr_addr,
	csr_be,
	csr_write_req,
	csr_wdata,
	csr_read_req,
	csr_rdata,
	csr_rdata_valid,
	csr_waitrequest,
	
	pll_locked,
	afi_cal_success,
	afi_cal_fail,
	seq_fom_in,
	seq_fom_out,
	cal_init_failing_stage,
	cal_init_failing_substage,
	cal_init_failing_group
	
);

localparam RESET_REQUEST_DELAY = 4;

localparam CSR_IP_VERSION_NUMBER = 161;

parameter CSR_ADDR_WIDTH       = 8;
parameter CSR_DATA_WIDTH       = 32;
parameter CSR_BE_WIDTH         = 4;

parameter MEM_READ_DQS_WIDTH = 64;

parameter MR1_RTT = 0;
parameter MR1_ODS = 0;
parameter MR2_RTT_WR = 0;

input clk;

input reset_n;

input [CSR_ADDR_WIDTH - 1       : 0] csr_addr;
input [CSR_BE_WIDTH - 1 : 0] csr_be;
input csr_write_req;
input [CSR_DATA_WIDTH - 1       : 0] csr_wdata;
input csr_read_req;
output [CSR_DATA_WIDTH - 1 : 0] csr_rdata;
output csr_rdata_valid;
output csr_waitrequest;

input pll_locked;
input afi_cal_success;
input afi_cal_fail;
input [7:0] seq_fom_in;
input [7:0] seq_fom_out;
input [7:0] cal_init_failing_stage;
input [7:0] cal_init_failing_substage;
input [7:0] cal_init_failing_group;



reg int_write_req;
reg int_read_req;
reg [CSR_ADDR_WIDTH-1:0] int_addr;
reg [CSR_BE_WIDTH - 1 : 0] int_be;
reg [CSR_DATA_WIDTH - 1 : 0] int_rdata;
reg int_rdata_valid;
reg int_waitrequest;
reg [CSR_DATA_WIDTH - 1       : 0] int_wdata;

reg [31:0] csr_register_0001;
reg [31:0] csr_register_0002;
reg [31:0] csr_register_0004;
reg [31:0] csr_register_0005;
reg [31:0] csr_register_0006;
reg [31:0] csr_register_0007;
reg [31:0] csr_register_0008;



always @ (posedge clk) begin
	csr_register_0001 <= 0;
	csr_register_0001 <= 32'hdeadbeef;
	
	csr_register_0002 <= 0;
	csr_register_0002 <= {CSR_IP_VERSION_NUMBER[15:0],16'h4};
	
	csr_register_0004 <= 0;
	csr_register_0004[24] <= afi_cal_success;
	csr_register_0004[25] <= afi_cal_fail;
	csr_register_0004[26] <= pll_locked;
	
	csr_register_0005 <= 0;
	csr_register_0005[7:0] <= seq_fom_in;
	csr_register_0005[23:16] <= seq_fom_out;

	csr_register_0006 <= 0;
	csr_register_0006[7:0] <= cal_init_failing_stage;
	csr_register_0006[15:8] <= cal_init_failing_substage;
	csr_register_0006[23:16] <= cal_init_failing_group;
	
	csr_register_0007 <= 0;

	csr_register_0008 <= 0;
	csr_register_0008[2:0] <= MR1_RTT[2:0] & 3'b111;
	csr_register_0008[6:5] <= MR1_ODS[1:0] & 2'b11;
	csr_register_0008[10:9] <= MR2_RTT_WR[1:0] & 2'b11;

end
		
always @ (posedge clk or negedge reset_n) begin
	if (!reset_n) begin
		int_write_req <= 0;
		int_read_req <= 0;
		int_addr <= 0;
		int_wdata <= 0;
		int_be <= 0;
	end
	else begin

		int_addr  <= csr_addr;
		int_wdata <= csr_wdata;

		int_be    <= csr_be;
		
		if (csr_write_req)
			int_write_req <= 1'b1;
		else
			int_write_req <= 1'b0;
		
		if (csr_read_req)
			int_read_req <= 1'b1;
		else
			int_read_req <= 1'b0;
	end
end

always @ (posedge clk or negedge reset_n) begin
	if (!reset_n) begin
		int_rdata       <= 0;
		int_rdata_valid <= 0;
		int_waitrequest <= 1;
	end
	else begin
		int_waitrequest <= 1'b0;

		if (int_read_req) 
			case (int_addr)
				'h1 :
					int_rdata <= csr_register_0001;
				'h2 :
					int_rdata <= csr_register_0002;
				'h4 :
					int_rdata <= csr_register_0004;
				'h5 :
					int_rdata <= csr_register_0005;
				'h6 :
					int_rdata <= csr_register_0006;
				'h7 :
					int_rdata <= csr_register_0007;
				'h8 :
					int_rdata <= csr_register_0008;
				default :
					int_rdata <= 0;
			endcase
		
		if (int_read_req)
			int_rdata_valid <= 1'b1;
		else
			int_rdata_valid <= 1'b0;
	end
end


always @ (posedge clk or negedge reset_n) begin
	if (!reset_n) begin
		



	end
	else begin
	
		if (int_write_req) begin
		end
	end
end

`ifndef SYNTH_FOR_SIM
hps_sdram_p0_iss_probe pll_probe (
	.probe_input(pll_locked)
);
`endif

assign csr_waitrequest = int_waitrequest;
assign csr_rdata = int_rdata;
assign csr_rdata_valid = int_rdata_valid;


endmodule
