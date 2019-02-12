//Legal Notice: (C)2018 Altera Corporation. All rights reserved.  Your
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

module ghrd_10as066n2_led_pio_altera_avalon_pio_171_yz2rppa (
                                                              // inputs:
                                                               address,
                                                               chipselect,
                                                               clk,
                                                               in_port,
                                                               reset_n,
                                                               write_n,
                                                               writedata,

                                                              // outputs:
                                                               out_port,
                                                               readdata
                                                            )
;

  output  [  3: 0] out_port;
  output  [ 31: 0] readdata;
  input   [  1: 0] address;
  input            chipselect;
  input            clk;
  input   [  3: 0] in_port;
  input            reset_n;
  input            write_n;
  input   [ 31: 0] writedata;


wire             clk_en;
wire    [  3: 0] data_in;
reg     [  3: 0] data_out;
wire    [  3: 0] out_port;
wire    [  3: 0] read_mux_out;
reg     [ 31: 0] readdata;
  assign clk_en = 1;
  //s1, which is an e_avalon_slave
  assign read_mux_out = {4 {(address == 0)}} & data_in;
  always @(posedge clk or negedge reset_n)
    begin
      if (reset_n == 0)
          readdata <= 0;
      else if (clk_en)
          readdata <= {32'b0 | read_mux_out};
    end


  always @(posedge clk or negedge reset_n)
    begin
      if (reset_n == 0)
          data_out <= 0;
      else if (chipselect && ~write_n && (address == 0))
          data_out <= writedata[3 : 0];
    end


  assign out_port = data_out;
  assign data_in = in_port;

endmodule

