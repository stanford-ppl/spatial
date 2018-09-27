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


`timescale 1 ps / 1 ps

module altera_oct(
	rzqin,
	calibration_request,
	clock,
	reset,
	calibration_shift_busy,
	calibration_busy,
	s2pload_ena,
	s2pload_rdy,
	oct_0_series_termination_control,
	oct_0_parallel_termination_control,
	oct_1_series_termination_control,
	oct_1_parallel_termination_control,
	oct_2_series_termination_control,
	oct_2_parallel_termination_control,
	oct_3_series_termination_control,
	oct_3_parallel_termination_control,
	oct_4_series_termination_control,
	oct_4_parallel_termination_control,
	oct_5_series_termination_control,
	oct_5_parallel_termination_control,
	oct_6_series_termination_control,
	oct_6_parallel_termination_control,
	oct_7_series_termination_control,
	oct_7_parallel_termination_control,
	oct_8_series_termination_control,
	oct_8_parallel_termination_control,
	oct_9_series_termination_control,
	oct_9_parallel_termination_control,
	oct_10_series_termination_control,
	oct_10_parallel_termination_control,
	oct_11_series_termination_control,
	oct_11_parallel_termination_control
);

parameter OCT_CAL_NUM = 1;
parameter OCT_USER_MODE = "A_OCT_USER_OCT_OFF";
parameter OCT_CKBUF_MODE = "false";
parameter OCT_S2P_HANDSHAKE = "false";

parameter OCT_CAL_MODE_DER_0 = "A_OCT_CAL_MODE_SINGLE";
parameter OCT_CAL_MODE_DER_1 = "A_OCT_CAL_MODE_SINGLE";
parameter OCT_CAL_MODE_DER_2 = "A_OCT_CAL_MODE_SINGLE";
parameter OCT_CAL_MODE_DER_3 = "A_OCT_CAL_MODE_SINGLE";
parameter OCT_CAL_MODE_DER_4 = "A_OCT_CAL_MODE_SINGLE";
parameter OCT_CAL_MODE_DER_5 = "A_OCT_CAL_MODE_SINGLE";
parameter OCT_CAL_MODE_DER_6 = "A_OCT_CAL_MODE_SINGLE";
parameter OCT_CAL_MODE_DER_7 = "A_OCT_CAL_MODE_SINGLE";
parameter OCT_CAL_MODE_DER_8 = "A_OCT_CAL_MODE_SINGLE";
parameter OCT_CAL_MODE_DER_9 = "A_OCT_CAL_MODE_SINGLE";
parameter OCT_CAL_MODE_DER_10 = "A_OCT_CAL_MODE_SINGLE";
parameter OCT_CAL_MODE_DER_11 = "A_OCT_CAL_MODE_SINGLE";

localparam string OCT_CAL_MODE_S[0:11] ='{ OCT_CAL_MODE_DER_0,
				     	OCT_CAL_MODE_DER_1	,
				     	OCT_CAL_MODE_DER_2	,
				     	OCT_CAL_MODE_DER_3	,
				     	OCT_CAL_MODE_DER_4	,
				     	OCT_CAL_MODE_DER_5	,
				     	OCT_CAL_MODE_DER_6	,
				     	OCT_CAL_MODE_DER_7	,
				     	OCT_CAL_MODE_DER_8	,
				     	OCT_CAL_MODE_DER_9	,
				     	OCT_CAL_MODE_DER_10	,
				     	OCT_CAL_MODE_DER_11	};

input [OCT_CAL_NUM-1:0] rzqin;
input [OCT_CAL_NUM-1:0] calibration_request;
input clock;
input reset;
input s2pload_ena;

output [OCT_CAL_NUM-1:0] calibration_shift_busy;
output [OCT_CAL_NUM-1:0] calibration_busy;

output [15:0] oct_0_series_termination_control;
output [15:0] oct_0_parallel_termination_control;
output [15:0] oct_1_series_termination_control;
output [15:0] oct_1_parallel_termination_control;
output [15:0] oct_2_series_termination_control;
output [15:0] oct_2_parallel_termination_control;
output [15:0] oct_3_series_termination_control;
output [15:0] oct_3_parallel_termination_control;
output [15:0] oct_4_series_termination_control;
output [15:0] oct_4_parallel_termination_control;
output [15:0] oct_5_series_termination_control;
output [15:0] oct_5_parallel_termination_control;
output [15:0] oct_6_series_termination_control;
output [15:0] oct_6_parallel_termination_control;
output [15:0] oct_7_series_termination_control;
output [15:0] oct_7_parallel_termination_control;
output [15:0] oct_8_series_termination_control;
output [15:0] oct_8_parallel_termination_control;
output [15:0] oct_9_series_termination_control;
output [15:0] oct_9_parallel_termination_control;
output [15:0] oct_10_series_termination_control;
output [15:0] oct_10_parallel_termination_control;
output [15:0] oct_11_series_termination_control;
output [15:0] oct_11_parallel_termination_control;
output s2pload_rdy;

wire [191 : 0] series_termination_control;
wire [191 : 0] parallel_termination_control;

wire [OCT_CAL_NUM-1:0] enserusr;
wire nclrusr;
wire clkenusr;
wire clkusr;
wire [OCT_CAL_NUM-1:0] ser_data;
wire [OCT_CAL_NUM-1:0] s2pload_w;

assign oct_0_series_termination_control = series_termination_control[15:0];
assign oct_0_parallel_termination_control = parallel_termination_control[15:0];
assign oct_1_series_termination_control = series_termination_control[31:16];
assign oct_1_parallel_termination_control = parallel_termination_control[31:16];
assign oct_2_series_termination_control = series_termination_control[47:32];
assign oct_2_parallel_termination_control = parallel_termination_control[47:32];
assign oct_3_series_termination_control = series_termination_control[63:48];
assign oct_3_parallel_termination_control = parallel_termination_control[63:48];
assign oct_4_series_termination_control = series_termination_control[79:64];
assign oct_4_parallel_termination_control = parallel_termination_control[79:64];
assign oct_5_series_termination_control = series_termination_control[95:80];
assign oct_5_parallel_termination_control = parallel_termination_control[95:80];
assign oct_6_series_termination_control = series_termination_control[111:96];
assign oct_6_parallel_termination_control = parallel_termination_control[111:96];
assign oct_7_series_termination_control = series_termination_control[127:112];
assign oct_7_parallel_termination_control = parallel_termination_control[127:112];
assign oct_8_series_termination_control = series_termination_control[143:128];
assign oct_8_parallel_termination_control = parallel_termination_control[143:128];
assign oct_9_series_termination_control = series_termination_control[159:144];
assign oct_9_parallel_termination_control = parallel_termination_control[159:144];
assign oct_10_series_termination_control = series_termination_control[175:160];
assign oct_10_parallel_termination_control = parallel_termination_control[175:160];
assign oct_11_series_termination_control = series_termination_control[191:176];
assign oct_11_parallel_termination_control = parallel_termination_control[191:176];


genvar i;
generate
begin : gpio_one_bit
	for(i = 0 ; i < OCT_CAL_NUM ; i = i + 1) begin : oct_i_loop
		
		localparam OCT_CAL_MODE = (OCT_CAL_MODE_S[i] == "A_OCT_CAL_MODE_SINGLE") ? "A_OCT_CAL_MODE_SINGLE" : 
					  (OCT_CAL_MODE_S[i] == "A_OCT_CAL_MODE_DOUBLE") ? "A_OCT_CAL_MODE_DOUBLE" : 
					  (OCT_CAL_MODE_S[i] == "A_OCT_CAL_MODE_AUTO")   ? "A_OCT_CAL_MODE_AUTO"   :
					  "A_OCT_CAL_MODE_POD"; 

		twentynm_termination #(
			.a_oct_cal_mode(OCT_CAL_MODE),
			.a_oct_user_oct(OCT_USER_MODE)
		) sd1a_i ( 
			.rzqin(rzqin[i]),
			.enserusr(enserusr[i]),
			.nclrusr(nclrusr),
			.clkenusr(clkenusr),
			.clkusr(clkusr),			
			.serdataout(ser_data[i])
		);
		
		twentynm_termination_logic   sd2a_i
		( 
			.parallelterminationcontrol(parallel_termination_control[15+16*i:i*16]),
			.s2pload(s2pload_w[i]),
			.serdata(ser_data[i]),
			.seriesterminationcontrol(series_termination_control[15+16*i:i*16])
		);

	end

   if (OCT_CAL_NUM < 12)
   begin : gen_term_ctrl_tieoff
      assign series_termination_control[191:(OCT_CAL_NUM*16)] = '0;
      assign parallel_termination_control[191:(OCT_CAL_NUM*16)] = '0;
   end
end
endgenerate

generate
if (OCT_USER_MODE == "A_OCT_USER_OCT_ON") begin
	altera_oct_um_fsm #(
		.OCT_CKBUF_MODE(OCT_CKBUF_MODE),
		.OCT_CAL_NUM(OCT_CAL_NUM),
		.OCT_S2P_HANDSHAKE(OCT_S2P_HANDSHAKE)
	) altera_oct_um_fsm_i (
		.calibration_request(calibration_request),
		.clock(clock),
		.reset(reset),
		.calibration_shift_busy(calibration_shift_busy),
		.calibration_busy(calibration_busy),
		.s2pload_rdy(s2pload_rdy),
		.s2pload_ena(s2pload_ena),
		.enserusr(enserusr),
		.nclrusr(nclrusr),
		.clkenusr(clkenusr),
		.clkusr(clkusr),
		.s2pload(s2pload_w)
);

end else begin
	assign calibration_shift_busy = {OCT_CAL_NUM{1'b0}};
	assign calibration_busy = {OCT_CAL_NUM{1'b0}};
	assign s2pload_rdy = 1'b1;
end
endgenerate


endmodule

