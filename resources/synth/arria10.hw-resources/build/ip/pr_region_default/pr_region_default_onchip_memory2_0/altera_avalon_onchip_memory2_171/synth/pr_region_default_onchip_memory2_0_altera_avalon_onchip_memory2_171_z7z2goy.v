//Legal Notice: (C)2017 Altera Corporation. All rights reserved.  Your
//use of Altera Corporation's design tools, logic functions and other
//software and tools, and its AMPP partner logic functions, and any
//output files any of the foregoing (including device programming or
//simulation files), and any associated documentation or information are
//expressly subject to the terms and conditions of the Altera Program
//License Subscription Agreement or other applicable license agreement,
//including, without limitation, that your use is for the sole purpose
//of programming logic devices manufactured by Altera and sold by Altera
//or its authorized distributors.  Please refer to the applicable
//agreement for further details.

// synthesis translate_off
`timescale 1ns / 1ps
// synthesis translate_on

// turn off superfluous verilog processor warnings 
// altera message_level Level1 
// altera message_off 10034 10035 10036 10037 10230 10240 10030 13469 16735 16788 

module pr_region_default_onchip_memory2_0_altera_avalon_onchip_memory2_171_z7z2goy (
                                                                                     // inputs:
                                                                                      address,
                                                                                      byteenable,
                                                                                      chipselect,
                                                                                      clk,
                                                                                      clken,
                                                                                      freeze,
                                                                                      reset,
                                                                                      reset_req,
                                                                                      write,
                                                                                      writedata,

                                                                                     // outputs:
                                                                                      readdata
                                                                                   )
;

  output  [ 31: 0] readdata;
  input   [  6: 0] address;
  input   [  3: 0] byteenable;
  input            chipselect;
  input            clk;
  input            clken;
  input            freeze;
  input            reset;
  input            reset_req;
  input            write;
  input   [ 31: 0] writedata;


wire             clocken0;
wire    [ 31: 0] readdata;
wire             wren;
  assign wren = chipselect & write;
  assign clocken0 = clken & ~reset_req;
  altsyncram the_altsyncram
    (
      .address_a (address),
      .byteena_a (byteenable),
      .clock0 (clk),
      .clocken0 (clocken0),
      .data_a (writedata),
      .q_a (readdata),
      .wren_a (wren)
    );

  defparam the_altsyncram.byte_size = 8,
           the_altsyncram.init_file = "UNUSED",
           the_altsyncram.lpm_type = "altsyncram",
           the_altsyncram.maximum_depth = 128,
           the_altsyncram.numwords_a = 128,
           the_altsyncram.operation_mode = "SINGLE_PORT",
           the_altsyncram.outdata_reg_a = "UNREGISTERED",
           the_altsyncram.ram_block_type = "AUTO",
           the_altsyncram.read_during_write_mode_mixed_ports = "DONT_CARE",
           the_altsyncram.read_during_write_mode_port_a = "DONT_CARE",
           the_altsyncram.width_a = 32,
           the_altsyncram.width_byteena_a = 4,
           the_altsyncram.widthad_a = 7;

  //s1, which is an e_avalon_slave
  //s2, which is an e_avalon_slave

endmodule

