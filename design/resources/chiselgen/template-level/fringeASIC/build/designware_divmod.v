module designware_divmod(clock, reset, dividend, divisor, quot_out, rem_out);

parameter DIVIDEND_BIT_WIDTH = 16;
parameter DIVISOR_BIT_WIDTH = 16;
parameter SIGNED = 0;		            // 0 means unsigned, 1 means two's complement signed numbers
parameter IS_DIV = 1;		            // 0 means mod, 1 means div
parameter NUM_STAGES = 2;	          // number of pipeline stages, the latency of the multiply is num_stages - 1

input clock, reset;			//note reset is active low
  input [DIVIDEND_BIT_WIDTH-1:0] dividend;
  input [DIVISOR_BIT_WIDTH-1:0] divisor;
  output [DIVIDEND_BIT_WIDTH-1:0] quot_out, rem_out;

wire divide_by_0; //left floating intentionally (assumes we don't need this signal)

//computes dividend / divisor, assumes asynchronous reset
DW_div_pipe #(
  .a_width(DIVIDEND_BIT_WIDTH),
  .b_width(DIVISOR_BIT_WIDTH),
  .tc_mode(SIGNED),
  .rem_mode(IS_DIV),
  .num_stages(NUM_STAGES),
  .stall_mode(1'b0),
  .rst_mode(1)) div_pipe_inst (
    .clk(clock),
    .rst_n(reset),
    .en(1'b1),
    .a(dividend),
    .b(divisor),
    .quotient(quot_out),
    .remainder(rem_out),
    .divide_by_0(divide_by_0)
  );

endmodule
