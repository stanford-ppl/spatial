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


module altera_emif_arch_nf_buf_udir_se_o #(
   parameter OCT_CONTROL_WIDTH = 1,
   parameter CALIBRATED_OCT = 1
) (
   input  logic i,
   output logic o,
   input  logic [OCT_CONTROL_WIDTH-1:0] oct_stc,
   input  logic [OCT_CONTROL_WIDTH-1:0] oct_ptc
);
   timeunit 1ns;
   timeprecision 1ps;

   generate
      if (CALIBRATED_OCT) 
      begin : cal_oct
         twentynm_io_obuf obuf (
            .i(i),
            .o(o),
            .seriesterminationcontrol(oct_stc),
            .parallelterminationcontrol(oct_ptc),
            .obar(),
            .oe(),
            .dynamicterminationcontrol(),
            .devoe()
            );    
      end else 
      begin : no_oct
         twentynm_io_obuf obuf (
            .i(i),
            .o(o),
            .seriesterminationcontrol(),
            .parallelterminationcontrol(),
            .obar(),
            .oe(),
            .dynamicterminationcontrol(),
            .devoe()
            );    
      end
   endgenerate
endmodule


