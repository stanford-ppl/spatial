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



///////////////////////////////////////////////////////////////////////////////
// This module handles the creation of a conditional register stage
// This module may be used to implement a synchronizer (with properly selected
// REGISTER value)
///////////////////////////////////////////////////////////////////////////////

// The following ensures that the register stage isn't synthesized into
// RAM-based shift-regs (especially if customer logic implements another follow-on
// pipeline stage). RAM-based shift-regs can degrade timing for C2P/P2C transfers.
(* altera_attribute = "-name AUTO_SHIFT_REGISTER_RECOGNITION OFF" *)

 module altera_emif_arch_nf_regs #(
   parameter REGISTER       = 0,
   parameter WIDTH          = 0
) (
   input  logic              clk,
   input  logic              reset_n,
   input  logic [WIDTH-1:0]  data_in,
   output logic [WIDTH-1:0]  data_out
) /* synthesis dont_merge */;
   timeunit 1ns;
   timeprecision 1ps;

   generate
      if (REGISTER == 0) begin
         assign data_out = data_in;

      end else begin
         logic [WIDTH-1:0] sr_out;
         always_ff @(posedge clk or negedge reset_n) begin
            if (~reset_n) begin
               sr_out <= '0;
            end else begin
               sr_out <= data_in;
            end
         end
         assign data_out = sr_out;
      end
   endgenerate
endmodule
