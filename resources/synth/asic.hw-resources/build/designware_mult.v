module designware_mult (clock, reset, in0, in1, out);

parameter IN0_BIT_WIDTH = 16;
parameter IN1_BIT_WIDTH = 16;
parameter OUT_BIT_WIDTH = 16;
parameter SIGNED = 0;		//0 means unsigned, 1 means two's complement signed numbers
parameter NUM_STAGES = 2;	//number of pipeline stages, the latency of the multiply is num_stages - 1

input clock, reset;			//note reset is active low
  input [IN0_BIT_WIDTH-1:0] in0;
  input [IN1_BIT_WIDTH-1:0] in1;
  output [OUT_BIT_WIDTH-1:0] out;

//assumes asynchronous reset
DW_mult_pipe #(.a_width(IN0_BIT_WIDTH), .b_width(IN1_BIT_WIDTH), .num_stages(NUM_STAGES), .stall_mode(1'b0), .rst_mode(1)) mult_pipe_inst (.clk(clock), .rst_n(reset), .en(1'b1), .tc(SIGNED), .a(in0), .b(in1), .product(out));

endmodule
