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



`timescale 1 ps / 1 ps

(* altera_attribute = "-name IP_TOOL_NAME altera_mem_if_ddr3_hard_phy_core; -name IP_TOOL_VERSION 16.1; -name FITTER_ADJUST_HC_SHORT_PATH_GUARDBAND 100" *)
module hps_sdram_p0 (
    global_reset_n,
    soft_reset_n,
	csr_soft_reset_req,
    parallelterminationcontrol,
    seriesterminationcontrol,
	pll_mem_clk,
	pll_write_clk,
	pll_write_clk_pre_phy_clk,
	pll_addr_cmd_clk,
	pll_avl_clk,
	pll_config_clk,
	pll_mem_phy_clk,
	afi_phy_clk,
	pll_avl_phy_clk,
	pll_locked,
	dll_pll_locked,
	dll_delayctrl,
	dll_clk,
	ctl_reset_n,
	afi_reset_n,
	afi_reset_export_n,
	afi_clk,
	afi_half_clk,
	afi_addr,
	afi_ba,
	afi_cke,
	afi_cs_n,
	afi_ras_n,
	afi_we_n,
	afi_cas_n,
	afi_rst_n,
	afi_odt,
	afi_mem_clk_disable,
	afi_dqs_burst,
	afi_wdata,
	afi_wdata_valid,
	afi_dm,
	afi_rdata,
	afi_rdata_en,
	afi_rdata_en_full,
	afi_rdata_valid,
	afi_cal_success,
	afi_cal_fail,
	afi_wlat,
	afi_rlat,
	avl_read,
	avl_write,
	avl_address,
	avl_writedata,
	avl_waitrequest,
	avl_readdata,
	cfg_addlat,               
	cfg_bankaddrwidth,
	cfg_caswrlat,
	cfg_coladdrwidth,
	cfg_csaddrwidth,
	cfg_devicewidth,
	cfg_dramconfig,
	cfg_interfacewidth,
	cfg_rowaddrwidth,
	cfg_tcl,
	cfg_tmrd,
	cfg_trefi,
	cfg_trfc,
	cfg_twr,
	io_intaddrdout,
	io_intbadout,
	io_intcasndout,
	io_intckdout,
	io_intckedout,
	io_intckndout,
	io_intcsndout,
	io_intdmdout,
	io_intdqdin,
	io_intdqdout,
	io_intdqoe,
	io_intdqsbdout,
	io_intdqsboe,
	io_intdqsdout,
	io_intdqslogicdqsena,
	io_intdqslogicfiforeset,
	io_intdqslogicincrdataen,
	io_intdqslogicincwrptr,
	io_intdqslogicoct,
	io_intdqslogicrdatavalid,
	io_intdqslogicreadlatency,
	io_intdqsoe,
	io_intodtdout,
	io_intrasndout,
	io_intresetndout,
	io_intwendout,
	io_intafirlat,
	io_intafiwlat,
	io_intaficalfail,
	io_intaficalsuccess,
	mem_a,
	mem_ba,
	mem_ck,
	mem_ck_n,
	mem_cke,
	mem_cs_n,
	mem_dm,
	mem_ras_n,
	mem_cas_n,
	mem_we_n,
	mem_dq,
	mem_dqs,
	mem_dqs_n,
	mem_reset_n,
	mem_odt,
	avl_clk,
	scc_clk,
	avl_reset_n,
	scc_reset_n,
	scc_data,
	scc_dqs_ena,
	scc_dqs_io_ena,
	scc_dq_ena,
	scc_dm_ena,
	scc_upd,
	capture_strobe_tracking,
	phy_clk,
	ctl_clk,
	phy_reset_n
);


// ******************************************************************************************************************************** 
// BEGIN PARAMETER SECTION
// All parameters default to "" will have their values passed in from higher level wrapper with the controller and driver. 
parameter DEVICE_FAMILY = "Cyclone V";
parameter IS_HHP_HPS = "true";

// choose between abstract (fast) and regular model
`ifndef ALTERA_ALT_MEM_IF_PHY_FAST_SIM_MODEL
  `define ALTERA_ALT_MEM_IF_PHY_FAST_SIM_MODEL 0
`endif

parameter ALTERA_ALT_MEM_IF_PHY_FAST_SIM_MODEL = `ALTERA_ALT_MEM_IF_PHY_FAST_SIM_MODEL;

localparam FAST_SIM_MODEL = ALTERA_ALT_MEM_IF_PHY_FAST_SIM_MODEL;


// On-chip termination
parameter OCT_TERM_CONTROL_WIDTH   = 16;

// PHY-Memory Interface
// Memory device specific parameters, they are set according to the memory spec.
parameter MEM_IF_ADDR_WIDTH			= 15;
parameter MEM_IF_BANKADDR_WIDTH     = 3;
parameter MEM_IF_CK_WIDTH			= 1;
parameter MEM_IF_CLK_EN_WIDTH		= 1;
parameter MEM_IF_CS_WIDTH			= 1;
parameter MEM_IF_DM_WIDTH         	= 4;
parameter MEM_IF_CONTROL_WIDTH    	= 1; 
parameter MEM_IF_DQ_WIDTH         	= 32;
parameter MEM_IF_DQS_WIDTH         	= 4;
parameter MEM_IF_READ_DQS_WIDTH    	= 4;
parameter MEM_IF_WRITE_DQS_WIDTH   	= 4;
parameter MEM_IF_ODT_WIDTH         	= 1;


// DLL Interface
parameter DLL_DELAY_CTRL_WIDTH	= 7;

parameter SCC_DATA_WIDTH            = 1;
	
// Read Datapath parameters, the values should not be changed unless the intention is to change the architecture.
// Read valid prediction FIFO
parameter READ_VALID_FIFO_SIZE             = 16;

// Data resynchronization FIFO
parameter READ_FIFO_SIZE                   = 8;

parameter MR1_ODS								= 1;
parameter MR1_RTT								= 1;
parameter MR2_RTT_WR							= 1;


// The DLL offset control width
parameter DLL_OFFSET_CTRL_WIDTH = 6;

parameter CALIB_REG_WIDTH = 8;


parameter TB_PROTOCOL       = "DDR3";
parameter TB_MEM_CLK_FREQ   = "400.0";
parameter TB_RATE           = "FULL";
parameter TB_MEM_DQ_WIDTH   = "32";
parameter TB_MEM_DQS_WIDTH  = "4";
parameter TB_PLL_DLL_MASTER = "true";

parameter FAST_SIM_CALIBRATION = "false";


parameter AC_ROM_INIT_FILE_NAME = "hps_AC_ROM.hex";
parameter INST_ROM_INIT_FILE_NAME = "hps_inst_ROM.hex";

localparam SIM_FILESET = ("false" == "true");


// END PARAMETER SECTION
// ******************************************************************************************************************************** 


// ******************************************************************************************************************************** 
// BEGIN PORT SECTION


// When the PHY is selected to be a PLL/DLL SLAVE, the PLL and DLL are instantied at the top level of the example design
input	pll_mem_clk;	
input	pll_write_clk;
input	pll_write_clk_pre_phy_clk;
input	pll_addr_cmd_clk;
input	pll_avl_clk;
input	pll_config_clk;
input	pll_locked;
input pll_mem_phy_clk;
input afi_phy_clk;
input pll_avl_phy_clk;




input	[DLL_DELAY_CTRL_WIDTH-1:0]  dll_delayctrl;
output  dll_pll_locked;
output  dll_clk;



// Reset Interface, AFI 2.0
input   global_reset_n;		// Resets (active-low) the whole system (all PHY logic + PLL)
input	soft_reset_n;		// Resets (active-low) PHY logic only, PLL is NOT reset
output	afi_reset_n;		// Asynchronously asserted and synchronously de-asserted on afi_clk domain
output	afi_reset_export_n;		// Asynchronously asserted and synchronously de-asserted on afi_clk domain
							// should be used to reset system level afi_clk domain logic
output	ctl_reset_n;		// Asynchronously asserted and synchronously de-asserted on ctl_clk domain
                            // should be used by hard controller only
input csr_soft_reset_req;  // Reset request (active_high) being driven by external debug master

// OCT termination control signals
input [OCT_TERM_CONTROL_WIDTH-1:0] parallelterminationcontrol;
input [OCT_TERM_CONTROL_WIDTH-1:0] seriesterminationcontrol;


// PHY-Controller Interface, AFI 2.0
// Control Interface
input  [19:0]  afi_addr;		// address
input   [2:0]  afi_ba;			// bank
input   [1:0]  afi_cke;		// clock enable
input   [1:0]  afi_cs_n;		// chip select
input   [0:0]  afi_ras_n;
input   [0:0]  afi_we_n;
input   [0:0]  afi_cas_n;
input   [1:0]  afi_odt;
input   [0:0]  afi_rst_n;
input   [0:0]  afi_mem_clk_disable;


// Write data interface
input   [4:0]  afi_dqs_burst;
input  [79:0]  afi_wdata;			// write data
input	[4:0]  afi_wdata_valid;	// write data valid, used to maintain write latency required by protocol spec
input   [9:0]  afi_dm;				// write data mask

// Read data interface
output [79:0]  afi_rdata;			// read data				
input   [4:0]  afi_rdata_en;		// read enable, used to maintain the read latency calibrated by PHY
input   [4:0]  afi_rdata_en_full;	// read enable full burst, used to create DQS enable
output  [0:0]  afi_rdata_valid;	// read data valid

// Status interface
output  afi_cal_success;	// calibration success
output  afi_cal_fail;		// calibration failure

output  [3:0]  afi_wlat;
output  [4:0]  afi_rlat;


// Avalon interface to the sequencer
input   [15:0]  avl_address;
input           avl_read;
output  [31:0]  avl_readdata;
output          avl_waitrequest;
input           avl_write;
input   [31:0]  avl_writedata;


// Configuration interface to the memory controller
input    [7:0]  cfg_addlat;
input    [7:0]  cfg_bankaddrwidth;
input    [7:0]  cfg_caswrlat;
input    [7:0]  cfg_coladdrwidth;
input    [7:0]  cfg_csaddrwidth;
input    [7:0]  cfg_devicewidth;
input   [23:0]  cfg_dramconfig;
input    [7:0]  cfg_interfacewidth;
input    [7:0]  cfg_rowaddrwidth;
input    [7:0]  cfg_tcl;
input    [7:0]  cfg_tmrd;
input   [15:0]  cfg_trefi;
input    [7:0]  cfg_trfc;
input    [7:0]  cfg_twr;


//  IO/bypass interface to the core (or soft controller)
input   [63:0]  io_intaddrdout;
input   [11:0]  io_intbadout;
input    [3:0]  io_intcasndout;
input    [3:0]  io_intckdout;
input    [7:0]  io_intckedout;
input    [3:0]  io_intckndout;
input    [7:0]  io_intcsndout;
input   [19:0]  io_intdmdout;
output [179:0]  io_intdqdin;
input  [179:0]  io_intdqdout;
input   [89:0]  io_intdqoe;
input   [19:0]  io_intdqsbdout;
input    [9:0]  io_intdqsboe;
input   [19:0]  io_intdqsdout;
input    [9:0]  io_intdqslogicdqsena;
input    [4:0]  io_intdqslogicfiforeset;
input    [9:0]  io_intdqslogicincrdataen;
input    [9:0]  io_intdqslogicincwrptr;
input    [9:0]  io_intdqslogicoct;
output   [4:0]  io_intdqslogicrdatavalid;
input   [24:0]  io_intdqslogicreadlatency;
input    [9:0]  io_intdqsoe;
input    [7:0]  io_intodtdout;
input    [3:0]  io_intrasndout;
input    [3:0]  io_intresetndout;
input    [3:0]  io_intwendout;
output   [4:0]  io_intafirlat;
output   [3:0]  io_intafiwlat;
output          io_intaficalfail;  
output          io_intaficalsuccess;


// PHY-Memory Interface

output  [MEM_IF_ADDR_WIDTH-1:0]       mem_a;        // address
output  [MEM_IF_BANKADDR_WIDTH-1:0]   mem_ba;       // bank
output  [MEM_IF_CK_WIDTH-1:0]         mem_ck;       // differential address and command clock
output  [MEM_IF_CK_WIDTH-1:0]         mem_ck_n;
output  [MEM_IF_CLK_EN_WIDTH-1:0]     mem_cke;      // clock enable
output  [MEM_IF_CS_WIDTH-1:0]         mem_cs_n;     // chip select
output  [MEM_IF_DM_WIDTH-1:0]         mem_dm;       // data mask
output  [MEM_IF_CONTROL_WIDTH-1:0]    mem_ras_n;		
output  [MEM_IF_CONTROL_WIDTH-1:0]    mem_cas_n;		
output  [MEM_IF_CONTROL_WIDTH-1:0]    mem_we_n;		
inout	[MEM_IF_DQ_WIDTH-1:0]         mem_dq;       // bidirectional data bus
inout	[MEM_IF_DQS_WIDTH-1:0]        mem_dqs;      // bidirectional data strobe
inout	[MEM_IF_DQS_WIDTH-1:0]        mem_dqs_n;    // differential bidirectional data strobe
output  [MEM_IF_ODT_WIDTH-1:0]        mem_odt;
output	                              mem_reset_n;


// PLL Interface
input	afi_clk;
input	afi_half_clk;

wire	pll_dqs_ena_clk;



output  avl_clk;
output  scc_clk;
output  avl_reset_n;
output  scc_reset_n;

input           [SCC_DATA_WIDTH-1:0]  scc_data;
input    [MEM_IF_READ_DQS_WIDTH-1:0]  scc_dqs_ena;
input    [MEM_IF_READ_DQS_WIDTH-1:0]  scc_dqs_io_ena;
input          [MEM_IF_DQ_WIDTH-1:0]  scc_dq_ena;
input          [MEM_IF_DM_WIDTH-1:0]  scc_dm_ena;
input                          [0:0]  scc_upd;
output   [MEM_IF_READ_DQS_WIDTH-1:0]  capture_strobe_tracking;

output  phy_clk;
output	ctl_clk;
output  phy_reset_n;


// END PORT SECTION


initial $display("Using %0s core emif simulation models", FAST_SIM_MODEL ? "Fast" : "Regular");




assign avl_clk = pll_avl_clk;
assign scc_clk = pll_config_clk;



assign pll_dqs_ena_clk = pll_write_clk;

hps_sdram_p0_acv_hard_memphy #(
	.DEVICE_FAMILY(DEVICE_FAMILY),
	.IS_HHP_HPS(IS_HHP_HPS),
	.OCT_SERIES_TERM_CONTROL_WIDTH(OCT_TERM_CONTROL_WIDTH),
	.OCT_PARALLEL_TERM_CONTROL_WIDTH(OCT_TERM_CONTROL_WIDTH),
	.MEM_ADDRESS_WIDTH(MEM_IF_ADDR_WIDTH),
	.MEM_BANK_WIDTH(MEM_IF_BANKADDR_WIDTH),
	.MEM_CLK_EN_WIDTH(MEM_IF_CLK_EN_WIDTH),
	.MEM_CK_WIDTH(MEM_IF_CK_WIDTH),
	.MEM_ODT_WIDTH(MEM_IF_ODT_WIDTH),
	.MEM_DQS_WIDTH(MEM_IF_DQS_WIDTH),
	.MEM_IF_CS_WIDTH(MEM_IF_CS_WIDTH),
	.MEM_DM_WIDTH(MEM_IF_DM_WIDTH),
	.MEM_CONTROL_WIDTH(MEM_IF_CONTROL_WIDTH),
	.MEM_DQ_WIDTH(MEM_IF_DQ_WIDTH),
	.MEM_READ_DQS_WIDTH(MEM_IF_READ_DQS_WIDTH),
	.MEM_WRITE_DQS_WIDTH(MEM_IF_WRITE_DQS_WIDTH),
	.DLL_DELAY_CTRL_WIDTH(DLL_DELAY_CTRL_WIDTH),
	.MR1_ODS(MR1_ODS),
	.MR1_RTT(MR1_RTT),
	.MR2_RTT_WR(MR2_RTT_WR),
	.CALIB_REG_WIDTH(CALIB_REG_WIDTH),
	.TB_PROTOCOL(TB_PROTOCOL),
	.TB_MEM_CLK_FREQ(TB_MEM_CLK_FREQ),
	.TB_RATE(TB_RATE),
	.TB_MEM_DQ_WIDTH(TB_MEM_DQ_WIDTH),
	.TB_MEM_DQS_WIDTH(TB_MEM_DQS_WIDTH),
	.TB_PLL_DLL_MASTER(TB_PLL_DLL_MASTER),
	.FAST_SIM_MODEL(FAST_SIM_MODEL),
	.FAST_SIM_CALIBRATION(FAST_SIM_CALIBRATION),
	.AC_ROM_INIT_FILE_NAME(AC_ROM_INIT_FILE_NAME),
	.INST_ROM_INIT_FILE_NAME(INST_ROM_INIT_FILE_NAME)
) umemphy (
	.global_reset_n(global_reset_n),
	.soft_reset_n(soft_reset_n & ~csr_soft_reset_req),
	.ctl_reset_n(ctl_reset_n),
	.ctl_reset_export_n(afi_reset_export_n),
    .afi_reset_n(afi_reset_n),
	.pll_locked(pll_locked),
	.oct_ctl_rt_value(parallelterminationcontrol),
	.oct_ctl_rs_value(seriesterminationcontrol),
	.afi_addr(afi_addr),
	.afi_ba(afi_ba),
	.afi_cke(afi_cke),
	.afi_cs_n(afi_cs_n),
	.afi_ras_n(afi_ras_n),
	.afi_we_n(afi_we_n),
	.afi_cas_n(afi_cas_n),
	.afi_rst_n(afi_rst_n),
	.afi_odt(afi_odt),
	.afi_mem_clk_disable(afi_mem_clk_disable),
	.afi_dqs_burst(afi_dqs_burst),
	.afi_wdata(afi_wdata),
	.afi_wdata_valid(afi_wdata_valid),
	.afi_dm(afi_dm),
	.afi_rdata(afi_rdata),
	.afi_rdata_en(afi_rdata_en),
	.afi_rdata_en_full(afi_rdata_en_full),
	.afi_rdata_valid(afi_rdata_valid),
	.afi_wlat(afi_wlat),
	.afi_rlat(afi_rlat),
	.afi_cal_success(afi_cal_success),
	.afi_cal_fail(afi_cal_fail),
	.avl_read(avl_read),
	.avl_write(avl_write),
	.avl_address(avl_address),
	.avl_writedata(avl_writedata),
	.avl_waitrequest(avl_waitrequest),
	.avl_readdata(avl_readdata),
	.cfg_addlat(cfg_addlat),
	.cfg_bankaddrwidth(cfg_bankaddrwidth),
	.cfg_caswrlat(cfg_caswrlat),
	.cfg_coladdrwidth(cfg_coladdrwidth),
	.cfg_csaddrwidth(cfg_csaddrwidth),
	.cfg_devicewidth(cfg_devicewidth),
	.cfg_dramconfig(cfg_dramconfig),
	.cfg_interfacewidth(cfg_interfacewidth),
	.cfg_rowaddrwidth(cfg_rowaddrwidth),
	.cfg_tcl(cfg_tcl),
	.cfg_tmrd(cfg_tmrd),
	.cfg_trefi(cfg_trefi),
	.cfg_trfc(cfg_trfc),
	.cfg_twr(cfg_twr),
	.io_intaddrdout(io_intaddrdout),
	.io_intbadout(io_intbadout),
	.io_intcasndout(io_intcasndout),
	.io_intckdout(io_intckdout),
	.io_intckedout(io_intckedout),
	.io_intckndout(io_intckndout),
	.io_intcsndout(io_intcsndout),
	.io_intdmdout(io_intdmdout),
	.io_intdqdin(io_intdqdin),
	.io_intdqdout(io_intdqdout),
	.io_intdqoe(io_intdqoe),
	.io_intdqsbdout(io_intdqsbdout),
	.io_intdqsboe(io_intdqsboe),
	.io_intdqsdout(io_intdqsdout),
	.io_intdqslogicdqsena(io_intdqslogicdqsena),
	.io_intdqslogicfiforeset(io_intdqslogicfiforeset),
	.io_intdqslogicincrdataen(io_intdqslogicincrdataen),
	.io_intdqslogicincwrptr(io_intdqslogicincwrptr),
	.io_intdqslogicoct(io_intdqslogicoct),
	.io_intdqslogicrdatavalid(io_intdqslogicrdatavalid),
	.io_intdqslogicreadlatency(io_intdqslogicreadlatency),
	.io_intdqsoe(io_intdqsoe),
	.io_intodtdout(io_intodtdout),
	.io_intrasndout(io_intrasndout),
	.io_intresetndout(io_intresetndout),
	.io_intwendout(io_intwendout),
	.io_intafirlat(io_intafirlat),
	.io_intafiwlat(io_intafiwlat),
	.io_intaficalfail(io_intaficalfail),
	.io_intaficalsuccess(io_intaficalsuccess),
	.mem_a(mem_a),
	.mem_ba(mem_ba),
	.mem_ck(mem_ck),
	.mem_ck_n(mem_ck_n),
	.mem_cke(mem_cke),
	.mem_cs_n(mem_cs_n),
	.mem_dm(mem_dm),
	.mem_ras_n(mem_ras_n),
	.mem_cas_n(mem_cas_n),
	.mem_we_n(mem_we_n),
	.mem_reset_n(mem_reset_n),
	.mem_dq(mem_dq),
	.mem_dqs(mem_dqs),
	.mem_dqs_n(mem_dqs_n),
	.mem_odt(mem_odt),
	.pll_afi_clk(afi_clk),
	.pll_mem_clk(pll_mem_clk),
	.pll_mem_phy_clk(pll_mem_phy_clk),
	.pll_afi_phy_clk(afi_phy_clk),
	.pll_avl_phy_clk(pll_avl_phy_clk),
	.pll_write_clk(pll_write_clk),
	.pll_write_clk_pre_phy_clk(pll_write_clk_pre_phy_clk),
	.pll_addr_cmd_clk(pll_addr_cmd_clk),
	.pll_afi_half_clk(afi_half_clk),
	.pll_dqs_ena_clk(pll_dqs_ena_clk),
	.seq_clk(afi_clk), 
	.reset_n_avl_clk(avl_reset_n),
	.reset_n_scc_clk(scc_reset_n),
	.scc_data(scc_data),
	.scc_dqs_ena(scc_dqs_ena),
	.scc_dqs_io_ena(scc_dqs_io_ena),
	.scc_dq_ena(scc_dq_ena),
	.scc_dm_ena(scc_dm_ena),
	.scc_upd(scc_upd),
	.capture_strobe_tracking(capture_strobe_tracking),
	.phy_clk(phy_clk),
	.ctl_clk(ctl_clk),
	.phy_reset_n(phy_reset_n),
	.pll_avl_clk(pll_avl_clk),
	.pll_config_clk(pll_config_clk),
	.dll_clk(dll_clk),
	.dll_pll_locked(dll_pll_locked),
	.dll_phy_delayctrl(dll_delayctrl)
);


endmodule

