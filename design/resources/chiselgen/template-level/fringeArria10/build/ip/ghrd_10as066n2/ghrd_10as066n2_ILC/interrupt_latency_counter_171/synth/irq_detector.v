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


// synthesis translate_off
`timescale 1ns / 1ps
// synthesis translate_on

module irq_detector #(
parameter INTR_TYPE = 1'b1
)(
   clk,
	reset_n,
	irq,
	counter_start,
	counter_stop
);

input   wire  clk;
input   wire  irq;
input   wire  reset_n;
output  wire  counter_start;
output  wire  counter_stop;

reg irq_d;

always @ (posedge clk or negedge reset_n)
   if (~reset_n) begin
        irq_d <= 1'b0;
   end
   else begin
	     irq_d <= irq;
   end
	
generate
if (INTR_TYPE == 1'b0) begin	
	
 assign counter_stop = ~irq & irq_d;
 assign counter_start = irq & ~irq_d;
 
end else begin

  assign counter_start = irq & ~irq_d;
  assign counter_stop = 1'b0;
  
end
endgenerate

endmodule
