// (C) 2001-2016 Intel Corporation. All rights reserved.
// Your use of Intel Corporation's design tools, logic functions and other 
// software and tools, and its AMPP partner logic functions, and any output 
// files any of the foregoing (including device programming or simulation 
// files), and any associated documentation or information are expressly subject 
// to the terms and conditions of the Intel Program License Subscription 
// Agreement, Intel MegaCore Function License Agreement, or other applicable 
// license agreement, including, without limitation, that your use is for the 
// sole purpose of programming logic devices manufactured by Intel and sold by 
// Intel or its authorized distributors.  Please refer to the applicable 
// agreement for further details.


// ******************************************************************************************************************************** 
// This file instantiates the OCT block.
// ******************************************************************************************************************************** 

`timescale 1 ps / 1 ps

(* altera_attribute = "-name IP_TOOL_NAME altera_mem_if_oct; -name IP_TOOL_VERSION 16.1; -name FITTER_ADJUST_HC_SHORT_PATH_GUARDBAND 100; -name ALLOW_SYNCH_CTRL_USAGE OFF; -name AUTO_CLOCK_ENABLE_RECOGNITION OFF; -name AUTO_SHIFT_REGISTER_RECOGNITION OFF" *)


module altera_mem_if_oct_cyclonev (
	oct_rzqin,
	parallelterminationcontrol,
	seriesterminationcontrol
);


parameter OCT_TERM_CONTROL_WIDTH = 0;


// These should be connected to reference resistance pins on the board, via OCT control block if instantiated by user
input oct_rzqin;

// for OCT master, termination control signals will be available to top level
output [OCT_TERM_CONTROL_WIDTH-1:0] parallelterminationcontrol;
output [OCT_TERM_CONTROL_WIDTH-1:0] seriesterminationcontrol;





	`ifndef ALTERA_RESERVED_QIS
	// synopsys translate_off
	`endif
	tri0  oct_rzqin;
	`ifndef ALTERA_RESERVED_QIS
	// synopsys translate_on
	`endif

	wire  [0:0]   wire_sd1a_serdataout;

	cyclonev_termination   sd1a_0
	( 
	.clkusrdftout(),
	.compoutrdn(),
	.compoutrup(),
	.enserout(),
	.rzqin(oct_rzqin),
	.scanout(),
	.serdataout(wire_sd1a_serdataout[0:0]),
	.serdatatocore()
	`ifndef FORMAL_VERIFICATION
	// synopsys translate_off
	`endif
	,
	.clkenusr(1'b0),
	.clkusr(1'b0),
	.enserusr(1'b0),
	.nclrusr(1'b0),
	.otherenser({10{1'b0}}),
	.scanclk(1'b0),
	.scanen(1'b0),
	.scanin(1'b0),
	.serdatafromcore(1'b0),
	.serdatain(1'b0)
	`ifndef FORMAL_VERIFICATION
	// synopsys translate_on
	`endif
	);

	cyclonev_termination_logic   sd2a_0
	( 
	.parallelterminationcontrol(parallelterminationcontrol),
	.serdata(wire_sd1a_serdataout),
	.seriesterminationcontrol(seriesterminationcontrol)
	`ifndef FORMAL_VERIFICATION
	// synopsys translate_off
	`endif
	,
	.enser(1'b0),
	.s2pload(1'b0),
	.scanclk(1'b0),
	.scanenable(1'b0)
	`ifndef FORMAL_VERIFICATION
	// synopsys translate_on
	`endif
	// synopsys translate_off

	// synopsys translate_on
	);

endmodule

