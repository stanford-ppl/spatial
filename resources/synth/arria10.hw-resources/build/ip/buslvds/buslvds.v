// Copyright (C) 1991-2011 Altera Corporation
// Your use of Altera Corporation's design tools, logic functions 
// and other software and tools, and its AMPP partner logic 
// functions, and any output files from any of the foregoing 
// (including device programming or simulation files), and any 
// associated documentation or information are expressly subject 
// to the terms and conditions of the Altera Program License 
// Subscription Agreement, Altera MegaCore Function License 
// Agreement, or other applicable license agreement, including, 
// without limitation, that your use is for the sole purpose of 
// programming logic devices manufactured by Altera and sold by 
// Altera or its authorized distributors.  Please refer to the 
// applicable agreement for further details.

// PROGRAM		"Quartus II 64-Bit"
// VERSION		"Version 10.1 Build 197 01/19/2011 Service Pack 1 SJ Full Version"
// CREATED		"Wed Nov 28 11:17:40 2012"

module buslvds(
	doutp,
	oe,
	din,
	p,
	n
);


input wire	doutp;
input wire	oe;
output wire	din;
inout wire	p;
inout wire	n;
wire oebout;
wire oeout;

wire	[0:0] SYNTHESIZED_WIRE_0;
wire	SYNTHESIZED_WIRE_1;
wire	SYNTHESIZED_WIRE_2;
wire	SYNTHESIZED_WIRE_3;





pdo	b2v_inst(
	.i(doutp),
	.oein(oe),
	.o(SYNTHESIZED_WIRE_2),
	.obar(SYNTHESIZED_WIRE_3),
	.oebout(oebout),
	.oeout(oeout));

assign	din = SYNTHESIZED_WIRE_0 & SYNTHESIZED_WIRE_1;

assign	SYNTHESIZED_WIRE_1 =  ~oe;


twentynm_io_obuf	b2v_inst3(
	.i(SYNTHESIZED_WIRE_2),
	.oe(oeout),
	.o(p),
	.obar());


twentynm_io_obuf	b2v_inst4(
	.i(SYNTHESIZED_WIRE_3),
	.oe(oebout),
	.o(n),
	.obar());


diffin	b2v_inst5(
	.datain(p),
	.datain_b(n),
	.dataout(SYNTHESIZED_WIRE_0));


endmodule
