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

module altera_oct_um_fsm(
	calibration_request,
	clock,
	reset,
	calibration_shift_busy,
	calibration_busy,
	enserusr,
	nclrusr,
	clkenusr,
	clkusr,
	s2pload,
	s2pload_ena,
	s2pload_rdy
);

parameter OCT_CAL_NUM = 1;
parameter OCT_CKBUF_MODE = "false";
parameter OCT_S2P_HANDSHAKE = "false";

input [OCT_CAL_NUM-1:0] calibration_request;
input clock;
input reset;
input s2pload_ena;

output reg [OCT_CAL_NUM-1:0] calibration_shift_busy;
output reg [OCT_CAL_NUM-1:0] calibration_busy;

output reg [OCT_CAL_NUM-1:0] enserusr;
output reg nclrusr;
output reg clkenusr;
output clkusr;
output reg [OCT_CAL_NUM-1:0] s2pload;
output reg s2pload_rdy;

typedef enum {
	OCT_IDLE,
	OCT_RESET,
	OCT_CALIBRATION,
	OCT_CALIBRATION_DONE,
	OCT_READY_FOR_SERIAL_SHIFT,
	OCT_SERIAL_SHIFT_SETUP,
	OCT_SERIAL_SHIFT,
	OCT_S2P_ASSERT,
	OCT_SERIAL_SHIFT_DONE,
	OCT_SERIAL_SHIFT_HOLD
} FSM_STATES;

FSM_STATES state;

reg [OCT_CAL_NUM-1:0] calibration_request_snapshot;
reg [11:0] calibration_counter;
reg [6:0] serial_shift_counter;
reg [3:0] oct_block_shifting;
reg [OCT_CAL_NUM-1:0] oct_block_shifting_decode;
reg [4:0] serial_shift_setup_counter;
reg [4:0] serial_shift_hold_counter;

localparam SERIAL_SHIFT_SETUP_CYCLES = 4'h4;
localparam SERIAL_SHIFT_HOLD_CYCLES = 4'h4;
localparam CALIBRATION_CYCLES = 12'h7D0; 
localparam SERIAL_SHIFT_CYCLES = 6'h20; 

wire calibration_done = (calibration_counter == CALIBRATION_CYCLES);
wire shifting_done = (serial_shift_counter == SERIAL_SHIFT_CYCLES);
wire oct_block_shifting_done = (oct_block_shifting == OCT_CAL_NUM);
wire serial_shift_setup_done = (serial_shift_setup_counter == SERIAL_SHIFT_SETUP_CYCLES);
wire serial_shift_hold_done_int = (serial_shift_hold_counter == SERIAL_SHIFT_HOLD_CYCLES);
wire serial_shift_hold_done = ((OCT_S2P_HANDSHAKE == "true") ? (serial_shift_hold_done_int & s2pload_ena) : serial_shift_hold_done_int);
wire clk;

wire [OCT_CAL_NUM-1:0] calibration_shift_busy_w;
wire [OCT_CAL_NUM-1:0] calibration_busy_w;
wire [OCT_CAL_NUM-1:0] enserusr_w;
wire nclrusr_w;
wire clkenusr_w;
wire [OCT_CAL_NUM-1:0] s2pload_w;


assign nclrusr_w = ~(state == OCT_RESET);
assign clkenusr_w = (
	(state == OCT_RESET) 		| 
	(state == OCT_CALIBRATION) 	| 
	(state == OCT_CALIBRATION_DONE)
);
assign calibration_busy_w = (state == OCT_CALIBRATION) ? calibration_request_snapshot : {OCT_CAL_NUM{1'b0}};
assign calibration_shift_busy_w = (
	state == OCT_CALIBRATION 		|| 
	state == OCT_CALIBRATION_DONE 		|| 
	state == OCT_READY_FOR_SERIAL_SHIFT 	||
	state == OCT_SERIAL_SHIFT_SETUP		|| 
	state == OCT_SERIAL_SHIFT 		||
	state == OCT_S2P_ASSERT			||
	state == OCT_SERIAL_SHIFT_DONE 		||
	state == OCT_SERIAL_SHIFT_HOLD) 	? calibration_request_snapshot : {OCT_CAL_NUM{1'b0}};
assign s2pload_w = (state == OCT_S2P_ASSERT) ? (calibration_request_snapshot & oct_block_shifting_decode) : {OCT_CAL_NUM{1'b0}};

integer i;
always @(*) begin
   oct_block_shifting_decode <= {OCT_CAL_NUM{1'b0}};

   for (i = 0; i < OCT_CAL_NUM; i = i + 1) begin
      oct_block_shifting_decode[i] <= ((oct_block_shifting == i) ? 1'b1 : 1'b0);
   end
end

always @(posedge clk or posedge reset) begin
	if(reset) begin
		state <= OCT_IDLE;
		calibration_request_snapshot <= {OCT_CAL_NUM{1'b0}};
		calibration_counter <= 12'h000; 
		oct_block_shifting <= 4'h0;
		serial_shift_counter <= 6'h00;
		serial_shift_setup_counter <= 4'h0;
		serial_shift_hold_counter <= 4'h0;
	end
	else begin
		case(state)
			OCT_IDLE: begin
				if(|calibration_request) begin
					state <= OCT_RESET;
					calibration_request_snapshot <= calibration_request;
				end
			end
			OCT_RESET: begin
				state <= OCT_CALIBRATION;
				calibration_counter <= 12'h000; 
				oct_block_shifting <= 4'h0;
				serial_shift_counter <= 6'h00;
			end
			OCT_CALIBRATION: begin
				if(calibration_done) begin
					state <= OCT_CALIBRATION_DONE;
				end
				else begin
					calibration_counter <= calibration_counter + 1'b1;
				end
			end
			OCT_CALIBRATION_DONE: begin
				state <= OCT_READY_FOR_SERIAL_SHIFT;
				oct_block_shifting <= 4'h0;
			end
			OCT_READY_FOR_SERIAL_SHIFT: begin
				state <= OCT_SERIAL_SHIFT_SETUP;
				serial_shift_counter <= 6'h00;
				serial_shift_setup_counter <= 4'h0;
			end
			OCT_SERIAL_SHIFT_SETUP:begin
				if(serial_shift_setup_done) begin
					state <= OCT_SERIAL_SHIFT;
				end
				else begin
					serial_shift_setup_counter <= serial_shift_setup_counter + 1'b1;
				end
			end
			OCT_SERIAL_SHIFT: begin
				if(shifting_done) begin
					state <= OCT_SERIAL_SHIFT_HOLD;
					serial_shift_hold_counter <= 4'h0;
				end
				else begin
					serial_shift_counter <= serial_shift_counter + 1'b1;
				end
			end
			OCT_SERIAL_SHIFT_HOLD: begin
				if(serial_shift_hold_done) begin
					state <= OCT_S2P_ASSERT; 
				end
				else if (serial_shift_hold_done_int == 1'b0) begin
					serial_shift_hold_counter <= serial_shift_hold_counter + 1'b1;
				end
			end
			OCT_S2P_ASSERT: begin
				state <= OCT_SERIAL_SHIFT_DONE;
				oct_block_shifting <= oct_block_shifting + 1'b1;
			end
			OCT_SERIAL_SHIFT_DONE: begin
				if(oct_block_shifting_done) begin
					state <= OCT_IDLE;
				end
				else begin
					state <= OCT_SERIAL_SHIFT_SETUP;
					serial_shift_counter <= 6'h00; 
					serial_shift_setup_counter <= 4'h0;
				end
			end
		endcase;
	end
end

always @(posedge clk) begin
   s2pload_rdy <= ((state == OCT_SERIAL_SHIFT_HOLD) ? 1'b1 : 1'b0);
end

assign enserusr_w = 
	(state == OCT_CALIBRATION) ? calibration_request_snapshot : 
		(state == OCT_SERIAL_SHIFT_SETUP	|| 
		state == OCT_SERIAL_SHIFT 		|| 
		state == OCT_SERIAL_SHIFT_HOLD 		||
		state == OCT_S2P_ASSERT			||
		state == OCT_SERIAL_SHIFT_DONE) 	?  (calibration_request_snapshot & oct_block_shifting_decode) :
							   {OCT_CAL_NUM{1'b0}};

wire clock_enable =
	state == OCT_IDLE 		|| 
	state == OCT_RESET 		|| 
	state == OCT_CALIBRATION 	|| 
	state == OCT_CALIBRATION_DONE 	|| 
	state == OCT_SERIAL_SHIFT;

generate

if (OCT_CKBUF_MODE == "true") begin
	reg cken_w;
	always @(negedge clock) begin
		cken_w <= clock_enable;
	end
	twentynm_clkena #(
		.clock_type("GLOBAL CLOCK"),
		.ena_register_mode("falling edge")
		) clkena (
			.inclk(clock),
			.ena(cken_w),
			.outclk(clkusr)
		);
		
		assign clk = clock;
		
		reg ff /* synthesis noprune */;
		always @(posedge clkusr) ff <= ~ff;
		
      always @(posedge clk)
      begin
         calibration_shift_busy <= calibration_shift_busy_w;
         calibration_busy       <= calibration_busy_w;
         enserusr               <= enserusr_w;
         nclrusr                <= nclrusr_w;
         clkenusr               <= clkenusr_w;
         s2pload                <= s2pload_w;
      end

end else begin
		
		reg clkusr_out;
		reg div_clk;
		
		always @(posedge clock or posedge reset) begin : CLKUSR_GEN
			if(reset) begin
				clkusr_out 	<= 1'b0;
			end
			else if(clock_enable) begin
				clkusr_out <= ~clkusr_out;
			end
		end
		
		always @(negedge clock or posedge reset) begin : FSM_CLK_GEN
			if(reset) begin
				div_clk		<= 1'b0;
			end
			else begin
				div_clk	   <= ~div_clk;
			end
		end
		
		assign clk = div_clk;
		assign clkusr = clkusr_out;

      always @(*)
      begin
         calibration_shift_busy <= calibration_shift_busy_w;
         calibration_busy       <= calibration_busy_w;
         enserusr               <= enserusr_w;
         nclrusr                <= nclrusr_w;
         clkenusr               <= clkenusr_w;
         s2pload                <= s2pload_w;
      end
		
end
endgenerate
	
endmodule

