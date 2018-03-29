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
// This file instantiates the PLL.
// ******************************************************************************************************************************** 

`timescale 1 ps / 1 ps

(* altera_attribute = "-name IP_TOOL_NAME altera_mem_if_hps_pll; -name IP_TOOL_VERSION 16.1; -name FITTER_ADJUST_HC_SHORT_PATH_GUARDBAND 100; -name ALLOW_SYNCH_CTRL_USAGE OFF; -name AUTO_CLOCK_ENABLE_RECOGNITION OFF; -name AUTO_SHIFT_REGISTER_RECOGNITION OFF" *)

// pll_mem_clk: full-rate clock, 0 degree phase shift, clock output to memory
// pll_write_clk: full-rate clock, -90 degree phase shift, clocks write data out to memory

module hps_sdram_pll (
	global_reset_n,
	pll_ref_clk,
	pll_mem_clk,
	pll_write_clk,
	pll_write_clk_pre_phy_clk,
	pll_addr_cmd_clk,
	pll_avl_clk,
	pll_config_clk,
	pll_locked,
	afi_clk,
	pll_mem_phy_clk,
	afi_phy_clk,
	pll_avl_phy_clk,
	afi_half_clk
);


// ******************************************************************************************************************************** 
// BEGIN PARAMETER SECTION
// All parameters default to "" will have their values passed in from higher level wrapper with the controller and driver. 
parameter DEVICE_FAMILY = "Cyclone V";

parameter IS_HHP_HPS = "true";


// Clock settings
parameter GENERIC_PLL = "true";
parameter REF_CLK_FREQ = "25.0 MHz";
parameter REF_CLK_PERIOD_PS = 40000;

parameter PLL_MEM_CLK_FREQ_STR = "400.0 MHz";
parameter PLL_WRITE_CLK_FREQ_STR = "400.0 MHz";
parameter PLL_DR_CLK_FREQ_STR = "";

parameter PLL_MEM_CLK_FREQ_SIM_STR = "2500 ps";
parameter PLL_WRITE_CLK_FREQ_SIM_STR = "2500 ps";
parameter PLL_DR_CLK_FREQ_SIM_STR = "0 ps";

parameter MEM_CLK_PHASE      = "0 ps";
parameter WRITE_CLK_PHASE    = "1875 ps";
parameter DR_CLK_PHASE       = "";


localparam SIM_FILESET = ("false" == "true");

localparam MEM_CLK_FREQ       = SIM_FILESET ? PLL_MEM_CLK_FREQ_SIM_STR : PLL_MEM_CLK_FREQ_STR;
localparam WRITE_CLK_FREQ     = SIM_FILESET ? PLL_WRITE_CLK_FREQ_SIM_STR : PLL_WRITE_CLK_FREQ_STR;
localparam DR_CLK_FREQ        = SIM_FILESET ? PLL_DR_CLK_FREQ_SIM_STR : PLL_DR_CLK_FREQ_STR;

// END PARAMETER SECTION
// ******************************************************************************************************************************** 


// ******************************************************************************************************************************** 
// BEGIN PORT SECTION

input   global_reset_n;		// Resets (active-low) the whole system (all PHY logic + PLL)
input	pll_ref_clk;		// PLL reference clock

output	pll_mem_clk;
output	pll_write_clk;
output	pll_write_clk_pre_phy_clk;
output	pll_addr_cmd_clk;
output	pll_avl_clk;
output	pll_config_clk;
output	pll_locked;

output afi_clk;
output pll_mem_phy_clk;
output afi_phy_clk;
output pll_avl_phy_clk;
output afi_half_clk;


// END PORT SECTION
// ******************************************************************************************************************************** 


generate
if (SIM_FILESET) begin
	wire fbout;
	
	generic_pll pll1 (
		.refclk({pll_ref_clk}),
		.rst(~global_reset_n),
		.fbclk(fbout),
		.outclk(pll_mem_clk),
		.fboutclk(fbout),
		.locked(pll_locked)
	);	
	defparam pll1.reference_clock_frequency = REF_CLK_FREQ,
		pll1.output_clock_frequency = MEM_CLK_FREQ,
		pll1.phase_shift = MEM_CLK_PHASE,
		pll1.duty_cycle = 50;

	generic_pll pll2 (
		.refclk({pll_ref_clk}),
		.rst(~global_reset_n),
		.fbclk(fbout),
		.outclk(pll_write_clk),
		.fboutclk(),
		.locked()
	);	
	defparam pll2.reference_clock_frequency = REF_CLK_FREQ,
		pll2.output_clock_frequency = WRITE_CLK_FREQ,
		pll2.phase_shift = WRITE_CLK_PHASE,
		pll2.duty_cycle = 50;


end else begin

	wire [4-1:0] clk_out;	


	if (DEVICE_FAMILY == "Arria V") begin
		arriav_hps_sdram_pll pll (
			.clk_out(clk_out)
		);
		defparam pll.reference_clock_frequency = REF_CLK_FREQ,
			pll.clk0_frequency    = MEM_CLK_FREQ,
			pll.clk0_phase_shift   = MEM_CLK_PHASE,
			pll.clk1_frequency     = WRITE_CLK_FREQ,
			pll.clk1_phase_shift   = WRITE_CLK_PHASE,
			pll.clk2_frequency     = DR_CLK_FREQ,
			pll.clk2_phase_shift   = DR_CLK_PHASE;
	end else if (DEVICE_FAMILY == "Cyclone V") begin
		cyclonev_hps_sdram_pll pll (
			.clk_out(clk_out)
		);
		defparam pll.reference_clock_frequency = REF_CLK_FREQ,
			pll.clk0_frequency    = MEM_CLK_FREQ,
			pll.clk0_phase_shift   = MEM_CLK_PHASE,
			pll.clk1_frequency     = WRITE_CLK_FREQ,
			pll.clk1_phase_shift   = WRITE_CLK_PHASE,
			pll.clk2_frequency     = DR_CLK_FREQ,
			pll.clk2_phase_shift   = DR_CLK_PHASE;
	end else begin
		unknown_family_hps_sdram_pll pll();
	end
	
	assign pll_mem_clk = clk_out[0];
	assign pll_write_clk = clk_out[1];
	assign pll_dr_clk = clk_out[2];
end
endgenerate

assign pll_addr_cmd_clk = pll_mem_clk;
assign pll_avl_clk = pll_mem_clk;
assign pll_config_clk = pll_mem_clk;
assign afi_clk = pll_mem_clk;
assign pll_mem_phy_clk = pll_mem_clk;
assign afi_phy_clk = pll_mem_clk;
assign pll_avl_phy_clk = pll_mem_clk;
assign afi_half_clk = pll_mem_clk;

assign pll_write_clk_pre_phy_clk = pll_write_clk;

endmodule

