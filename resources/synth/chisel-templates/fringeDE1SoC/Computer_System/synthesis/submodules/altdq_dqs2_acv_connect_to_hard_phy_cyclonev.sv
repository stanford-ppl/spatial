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


// altera message_off 10034 10036 10030 10858

`timescale 1 ps / 1 ps

(* altera_attribute = "-name MESSAGE_DISABLE 12010; -name MESSAGE_DISABLE 12161" *)
module altdq_dqs2_acv_connect_to_hard_phy_cyclonev (
	dll_delayctrl_in,
	dll_offsetdelay_in,
	capture_strobe_in,
	capture_strobe_n_in,
	capture_strobe_ena,
	capture_strobe_out,
	
	output_strobe_ena,
	output_strobe_out,
	output_strobe_n_out,
	oct_ena_in,
	strobe_io,
	strobe_n_io,
	
	core_clock_in,
	fr_clock_in,
	hr_clock_in,
	dr_clock_in,
	strobe_ena_hr_clock_in,
	write_strobe_clock_in,
	write_strobe,
	reset_n_core_clock_in,
	parallelterminationcontrol_in,
	seriesterminationcontrol_in,

	read_data_in,
	write_data_out,
	read_write_data_io,
		
	write_oe_in,
	read_data_out,
	write_data_in,
	extra_write_data_in,
	extra_write_data_out,
	capture_strobe_tracking,
	
	lfifo_rdata_en,
	lfifo_rdata_en_full,
	lfifo_rd_latency,
	lfifo_reset_n,
	lfifo_rdata_valid,
	vfifo_qvld,
	vfifo_inc_wr_ptr,
	vfifo_reset_n,
	rfifo_reset_n,

	config_data_in,
	config_dqs_ena,
	config_io_ena,
	config_extra_io_ena,
	config_dqs_io_ena,
	config_update,
	config_clock_in

);
	
parameter PIN_WIDTH = 8;
parameter PIN_TYPE = "bidir";

parameter USE_INPUT_PHASE_ALIGNMENT = "false";
parameter USE_OUTPUT_PHASE_ALIGNMENT = "false";
parameter USE_HALF_RATE_INPUT = "false";
parameter USE_HALF_RATE_OUTPUT = "false";

parameter DIFFERENTIAL_CAPTURE_STROBE = "false";
parameter SEPARATE_CAPTURE_STROBE = "false";

parameter INPUT_FREQ = 0.0;
parameter INPUT_FREQ_PS = "0 ps";
parameter DELAY_CHAIN_BUFFER_MODE = "high";
parameter DQS_PHASE_SETTING = 3;
parameter DQS_PHASE_SHIFT = 9000;
localparam DQS_DELAYCHAIN_BYPASS = (DQS_PHASE_SHIFT == 0) ? "true" : "false";
parameter DQS_ENABLE_PHASE_SETTING = 2;
parameter USE_DYNAMIC_CONFIG = "true";
parameter INVERT_CAPTURE_STROBE = "false";
parameter SWAP_CAPTURE_STROBE_POLARITY = "false";
parameter USE_TERMINATION_CONTROL = "false";
parameter USE_OCT_ENA_IN_FOR_OCT = "false";
parameter USE_DQS_ENABLE = "false";
parameter USE_IO_CONFIG = "false";
parameter USE_DQS_CONFIG = "false";

parameter USE_OFFSET_CTRL = "false";
parameter HR_DDIO_OUT_HAS_THREE_REGS = "true";

parameter USE_OUTPUT_STROBE = "true";
parameter DIFFERENTIAL_OUTPUT_STROBE = "false";
parameter USE_OUTPUT_STROBE_RESET = "true";
parameter USE_BIDIR_STROBE = "false";
parameter REVERSE_READ_WORDS = "false";
parameter NATURAL_ALIGNMENT = "false";

parameter EXTRA_OUTPUT_WIDTH = 0;
parameter PREAMBLE_TYPE = "none";
parameter USE_DATA_OE_FOR_OCT = "false";
parameter DQS_ENABLE_WIDTH = 1;
parameter EMIF_UNALIGNED_PREAMBLE_SUPPORT = "false";
parameter EMIF_BYPASS_OCT_DDIO = "false";

parameter USE_2X_FF = "false";
parameter USE_DQS_TRACKING = "false";

parameter SEPERATE_LDC_FOR_WRITE_STROBE = "false";

localparam rate_mult_in = (USE_HALF_RATE_INPUT == "true") ? 4 : 2;
localparam rate_mult_out = (USE_HALF_RATE_OUTPUT == "true") ? 4 : 2;
localparam fpga_width_in = PIN_WIDTH * rate_mult_in;
localparam fpga_width_out = PIN_WIDTH * rate_mult_out;
localparam extra_fpga_width_out = EXTRA_OUTPUT_WIDTH * rate_mult_out;
localparam OS_ENA_WIDTH =  rate_mult_out / 2;
localparam WRITE_OE_WIDTH = PIN_WIDTH * rate_mult_out / 2;
parameter  DQS_ENABLE_PHASECTRL = "true"; 

parameter DYNAMIC_MODE = "dynamic";

parameter OCT_SERIES_TERM_CONTROL_WIDTH   = 16; 
parameter OCT_PARALLEL_TERM_CONTROL_WIDTH = 16; 
parameter DLL_WIDTH = 6;
parameter REGULAR_WRITE_BUS_ORDERING = "true";

parameter ALTERA_ALTDQ_DQS2_FAST_SIM_MODEL = 0;

parameter USE_HARD_FIFOS = "false";
parameter CALIBRATION_SUPPORT = "false";
parameter USE_DQSIN_FOR_VFIFO_READ = "false";
parameter HHP_HPS = "false";

localparam READ_FIFO_MODE = (USE_HALF_RATE_OUTPUT == "true") ? "hrate_mode" : "frate_mode";

localparam DATA_RATE = 1;


localparam DELAY_CHAIN_WIDTH = 5;

localparam OUTPUT_ALIGNMENT_DELAY = "two_cycle";

input [DLL_WIDTH-1:0] dll_delayctrl_in;
input [DLL_WIDTH-1:0] dll_offsetdelay_in;

input core_clock_in;
input fr_clock_in;
input hr_clock_in;
input strobe_ena_hr_clock_in;
input write_strobe_clock_in;
input [3:0] write_strobe;

input [PIN_WIDTH-1:0] read_data_in;
output [PIN_WIDTH-1:0] write_data_out;
inout [PIN_WIDTH-1:0] read_write_data_io;

input capture_strobe_in;
input capture_strobe_n_in;
input [DQS_ENABLE_WIDTH-1:0] capture_strobe_ena;

input reset_n_core_clock_in;
parameter USE_LDC_AS_LOW_SKEW_CLOCK = "false";
parameter DLL_USE_2X_CLK = "false";

parameter DQS_ENABLE_AFTER_T7 = "true";

input [OS_ENA_WIDTH-1:0] output_strobe_ena;
output output_strobe_out;
output output_strobe_n_out;
input [1:0] oct_ena_in;
inout strobe_io;
inout strobe_n_io;

output [fpga_width_out-1:0] read_data_out;
input [fpga_width_out-1:0] write_data_in;

input [WRITE_OE_WIDTH-1:0] write_oe_in;
output capture_strobe_out;

input [extra_fpga_width_out-1:0] extra_write_data_in;
output [EXTRA_OUTPUT_WIDTH-1:0] extra_write_data_out;

output capture_strobe_tracking;

parameter LFIFO_OCT_EN_MASK = 4294967295;
input [(rate_mult_out / 2)-1:0] lfifo_rdata_en;
input [(rate_mult_out / 2)-1:0] lfifo_rdata_en_full;
localparam LFIFO_RD_LATENCY_WIDTH = 5;
input [LFIFO_RD_LATENCY_WIDTH-1:0] lfifo_rd_latency;
input lfifo_reset_n;
output lfifo_rdata_valid;
input [(rate_mult_out / 2)-1:0] vfifo_qvld;
input [(rate_mult_out / 2)-1:0] vfifo_inc_wr_ptr;
input vfifo_reset_n;
input rfifo_reset_n;

input dr_clock_in;

input	[OCT_PARALLEL_TERM_CONTROL_WIDTH-1:0] parallelterminationcontrol_in;
input	[OCT_SERIES_TERM_CONTROL_WIDTH-1:0] seriesterminationcontrol_in;

input config_data_in;
input config_update;
input config_dqs_ena;
input [PIN_WIDTH-1:0] config_io_ena;
input [EXTRA_OUTPUT_WIDTH-1:0] config_extra_io_ena;
input config_dqs_io_ena;
input config_clock_in;

wire [DLL_WIDTH-1:0] dll_delay_value;
assign dll_delay_value = dll_delayctrl_in;

wire dqsbusout;
wire dqsnbusout;

wire capture_strobe;


wire [1:0] inputclkdelaysetting;
wire [1:0] inputclkndelaysetting;
wire [4:0] dqs_outputdelaysetting;
wire [4:0] dqs_outputenabledelaysetting;

wire [DELAY_CHAIN_WIDTH-1:0] dqs_outputdelaysetting_dlc;
wire [DELAY_CHAIN_WIDTH-1:0] dqs_outputenabledelaysetting_dlc;



`ifndef FAMILY_HAS_NO_DYNCONF
generate
if (USE_DYNAMIC_CONFIG =="true" && (USE_OUTPUT_STROBE == "true" || PIN_TYPE =="input" || PIN_TYPE == "bidir"))
begin

	cyclonev_io_config dqs_io_config_1 (
			.outputhalfratebypass(),
      .readfiforeadclockselect(),
      .readfifomode(),
      .padtoinputregisterdelaysetting(),
  		.datain(config_data_in),  
			.clk(config_clock_in),
			.ena(config_dqs_io_ena),
			.update(config_update),  
			.outputregdelaysetting(dqs_outputdelaysetting),
			.outputenabledelaysetting(dqs_outputenabledelaysetting),
			.dataout()
		);

	assign dqs_outputdelaysetting_dlc = dqs_outputdelaysetting;
	assign dqs_outputenabledelaysetting_dlc = dqs_outputenabledelaysetting;

	/*
	cyclonev_io_config dqsn_io_config_1 (
		    .datain(config_data_in),          
			.clk(config_clock_in),
			.ena(config_dqs_io_ena),
			.update(config_update),       
			.outputregdelaysetting(dqsn_outputdelaysetting),
			.outputenabledelaysetting(dqsn_outputenabledelaysetting),
			.padtoinputregisterdelaysetting(dqsn_inputdelaysetting),

			.dataout()
		);
	*/
end
endgenerate
`endif

wire [1:0] oct_ena;
wire fr_term;

wire [1:0] oct_source;

generate
	if (USE_DATA_OE_FOR_OCT == "true")
	begin
		assign oct_source = ~write_oe_in;
	end
	else
	begin
	  	assign oct_source = ~output_strobe_ena;
	end
endgenerate

generate
	if (USE_HARD_FIFOS == "true")
	begin
		if (USE_HALF_RATE_OUTPUT == "true")
		begin
			if (USE_OCT_ENA_IN_FOR_OCT == "true")
			begin
				assign oct_ena = oct_ena_in;
			end
			else
			begin
				assign oct_ena = oct_source;
			end
				
		end
		else
		begin
			if (USE_OCT_ENA_IN_FOR_OCT == "true")
			begin
				assign fr_term = oct_ena_in[0];
			end
			else
			begin
				assign fr_term = oct_source[0];
			end
		end
	end
	else
	begin
		if (USE_HALF_RATE_OUTPUT == "true")
		begin : oct_ena_hr_gen
			if (USE_OCT_ENA_IN_FOR_OCT == "true")
			begin
				assign oct_ena = oct_ena_in;
			end
			else
			begin
				reg oct_ena_hr_reg;
				always @(posedge hr_clock_in)
					oct_ena_hr_reg <= oct_source[1];
				assign oct_ena[1] = ~oct_source[1];
				assign oct_ena[0] = ~(oct_ena_hr_reg | oct_source[1]);
			end
		end
		else
		begin : oct_ena_fr_gen
			if (USE_OCT_ENA_IN_FOR_OCT == "true")
			begin
				assign fr_term = oct_ena_in[0];
			end
			else
			begin
				reg oct_ena_fr_reg;
				initial
					oct_ena_fr_reg = 0;
				always @(posedge hr_clock_in)
					oct_ena_fr_reg <= oct_source[0];
				assign fr_term = ~(oct_source[0] | oct_ena_fr_reg);
			end
		end
	end
endgenerate



localparam PINS_PER_DQS_CONFIG = 6;
localparam NUM_STROBES = (DIFFERENTIAL_CAPTURE_STROBE == "true" || SEPARATE_CAPTURE_STROBE == "true") ? 2 : 1;
localparam DQS_CONFIGS = (PIN_WIDTH + EXTRA_OUTPUT_WIDTH + NUM_STROBES) / PINS_PER_DQS_CONFIG;

wire dividerphasesetting [DQS_CONFIGS:0];
wire dqoutputphaseinvert [DQS_CONFIGS:0];
wire [1:0] dqoutputphasesetting [DQS_CONFIGS:0];
wire [4:0] dqsbusoutdelaysetting [DQS_CONFIGS:0];	
wire [1:0] dqsoutputphasesetting [DQS_CONFIGS:0];
wire [1:0] resyncinputphasesetting [DQS_CONFIGS:0];
wire [4:0] dqsenabledelaysetting [DQS_CONFIGS:0];
wire [4:0] dqsdisabledelaysetting [DQS_CONFIGS:0];
wire dqshalfratebypass [DQS_CONFIGS:0];
wire [1:0] dqsinputphasesetting [DQS_CONFIGS:0];
wire [1:0] dqsenablectrlphasesetting [DQS_CONFIGS:0];
wire dqoutputpowerdown [DQS_CONFIGS:0];
wire dqsoutputpowerdown [DQS_CONFIGS:0];
wire resyncinputpowerdown [DQS_CONFIGS:0];
wire dqsenablectrlpowerdown [DQS_CONFIGS:0]; 

wire [1:0] dq2xoutputphasesetting [DQS_CONFIGS:0];
wire dq2xoutputphaseinvert [DQS_CONFIGS:0];
wire [1:0] dqs2xoutputphasesetting [DQS_CONFIGS:0];
wire dqs2xoutputphaseinvert [DQS_CONFIGS:0];

wire dqsbusoutfinedelaysetting [DQS_CONFIGS:0];
wire dqsenablectrlphaseinvert [DQS_CONFIGS:0];
wire dqsenablefinedelaysetting [DQS_CONFIGS:0];
wire dqsoutputphaseinvert [DQS_CONFIGS:0];
wire enadataoutbypass [DQS_CONFIGS:0];
wire enadqsenablephasetransferreg [DQS_CONFIGS:0];

wire enainputcycledelaysetting [DQS_CONFIGS:0];
wire enainputphasetransferreg [DQS_CONFIGS:0];

wire enaoctphasetransferreg [DQS_CONFIGS:0];
wire enaoutputphasetransferreg [DQS_CONFIGS:0];
wire enadqsphasetransferreg [DQS_CONFIGS:0];
wire [2:0] enadqscycledelaysetting [DQS_CONFIGS:0];
wire [2:0] enaoctcycledelaysetting [DQS_CONFIGS:0];
wire [2:0] enaoutputcycledelaysetting [DQS_CONFIGS:0];
wire [4:0] octdelaysetting1 [DQS_CONFIGS:0];

wire resyncinputphaseinvert [DQS_CONFIGS:0];

wire [DELAY_CHAIN_WIDTH-1:0] dqsbusoutdelaysetting_dlc[DQS_CONFIGS:0];
wire [DELAY_CHAIN_WIDTH-1:0] dqsenabledelaysetting_dlc[DQS_CONFIGS:0];
wire [DELAY_CHAIN_WIDTH-1:0] dqsdisabledelaysetting_dlc[DQS_CONFIGS:0];
wire [DELAY_CHAIN_WIDTH-1:0] octdelaysetting1_dlc[DQS_CONFIGS:0];

`ifndef FAMILY_HAS_NO_DYNCONF
generate
if (USE_DYNAMIC_CONFIG == "true")
begin
	genvar c_num; 
	for (c_num = 0; c_num <= DQS_CONFIGS; c_num = c_num + 1)
	begin :dqs_config_gen
			
		cyclonev_dqs_config dqs_config_inst
		( 
		.clk(config_clock_in),
		.datain(config_data_in),
		.dataout(),
		.ena(config_dqs_ena),
		.update(config_update),

		.postamblephasesetting(dqsenablectrlphasesetting[c_num]),
		.postamblephaseinvert(dqsenablectrlphaseinvert[c_num]),
		
		.dqsenablegatingdelaysetting(dqsenabledelaysetting[c_num]),
		.dqsenableungatingdelaysetting(dqsdisabledelaysetting[c_num]),
		.dqshalfratebypass(dqshalfratebypass[c_num]),

		.enadqsenablephasetransferreg(enadqsenablephasetransferreg[c_num]), 
		.octdelaysetting(octdelaysetting1[c_num]),
		.dqsbusoutdelaysetting(dqsbusoutdelaysetting[c_num]) 
		);
		assign dqsbusoutdelaysetting_dlc[c_num] = dqsbusoutdelaysetting[c_num];
		assign dqsenabledelaysetting_dlc[c_num] = dqsenabledelaysetting[c_num];
		assign dqsdisabledelaysetting_dlc[c_num] = dqsdisabledelaysetting[c_num];
		assign octdelaysetting1_dlc[c_num] = octdelaysetting1[c_num];
	end
end
endgenerate
`endif

generate
if (USE_BIDIR_STROBE == "true")
begin
	assign output_strobe_out = 1'b0;
	assign output_strobe_n_out = 1'b1;	
end
else
begin
	assign strobe_io = 1'b0;
	assign strobe_n_io = 1'b1;	
end
endgenerate

wire dq_dr_clock;
wire dqs_dr_clock;
wire dq_shifted_clock;
wire dqs_shifted_clock;
wire write_strobe_clock;
wire hr_seq_clock;

wire ena_clock;
wire ena_zero_phase_clock;


wire phy_clk_dqs_2x;
wire phy_clk_dqs;
wire phy_clk_dq;
wire phy_clk_hr;

generate
if (HHP_HPS == "true") begin
	assign phy_clk_hr = hr_clock_in;
	assign phy_clk_dq = fr_clock_in;
	assign phy_clk_dqs = write_strobe_clock_in;
	assign phy_clk_dqs_2x = dr_clock_in;
end else begin
	cyclonev_phy_clkbuf phy_clkbuf (
		.inclk ({hr_clock_in, fr_clock_in, write_strobe_clock_in, dr_clock_in}),
		.outclk ({phy_clk_hr, phy_clk_dq, phy_clk_dqs, phy_clk_dqs_2x})
	);
end
endgenerate

wire [3:0] leveled_dqs_clocks;
wire [3:0] leveled_dq_clocks; 
wire [3:0] leveled_hr_clocks; 

cyclonev_leveling_delay_chain leveling_delay_chain_dqs (
	.clkin (phy_clk_dqs),
	.delayctrlin (dll_delay_value),
	.clkout(leveled_dqs_clocks)
);
defparam leveling_delay_chain_dqs.physical_clock_source = "DQS";

cyclonev_clk_phase_select clk_phase_select_dqs (
  .phasectrlin(),
  .phaseinvertctrl(),
  .dqsin(),
	.clkin (leveled_dqs_clocks[0]),
	.clkout (dqs_shifted_clock)
);
defparam clk_phase_select_dqs.physical_clock_source = "DQS";
defparam clk_phase_select_dqs.use_phasectrlin = "false";
defparam clk_phase_select_dqs.phase_setting = 0;

generate
	if (SEPERATE_LDC_FOR_WRITE_STROBE == "true") begin
		
		wire extra_phy_clk_dqs_2x;
		wire extra_phy_clk_dqs;
		wire extra_phy_clk_dq;
		wire extra_phy_clk_hr;
		cyclonev_phy_clkbuf phy_clkbuf (
			.inclk ({hr_clock_in, fr_clock_in, write_strobe_clock_in, dr_clock_in}),
			.outclk ({extra_phy_clk_hr, extra_phy_clk_dq, extra_phy_clk_dqs, extra_phy_clk_dqs_2x})
		);
		wire [3:0] extra_leveled_dqs_clocks;
		cyclonev_leveling_delay_chain leveling_delay_chain_dqs (
			.clkin (extra_phy_clk_dqs),
			.delayctrlin (dll_delay_value),
			.clkout(extra_leveled_dqs_clocks)
		);
		defparam leveling_delay_chain_dqs.physical_clock_source = "DQS";
		
		cyclonev_clk_phase_select clk_phase_select_dqs (
      .phasectrlin(),
      .phaseinvertctrl(),
      .dqsin(),
			.clkin (extra_leveled_dqs_clocks[0]),
			.clkout (write_strobe_clock)
		);
		defparam clk_phase_select_dqs.physical_clock_source = "DQS";
		defparam clk_phase_select_dqs.use_phasectrlin = "false";
		defparam clk_phase_select_dqs.phase_setting = 0;
	end else begin
		assign write_strobe_clock = dqs_shifted_clock;
	end
endgenerate

cyclonev_clk_phase_select clk_phase_select_pst_0p (
    .phasectrlin(),
    .phaseinvertctrl(),
    .dqsin(dqsbusout),
    .clkin (leveled_dqs_clocks[0]),
    .clkout (ena_zero_phase_clock)
);
defparam clk_phase_select_pst_0p.physical_clock_source = "PST_0P";
defparam clk_phase_select_pst_0p.use_phasectrlin = "false";
defparam clk_phase_select_pst_0p.phase_setting = 0;
defparam clk_phase_select_pst_0p.use_dqs_input = USE_DQSIN_FOR_VFIFO_READ;

generate
	if (USE_DYNAMIC_CONFIG == "true")
	begin
		cyclonev_clk_phase_select clk_phase_select_pst (
		    .clkin (leveled_dqs_clocks),
		    .phasectrlin(dqsenablectrlphasesetting[0]),
		    .phaseinvertctrl(dqsenablectrlphaseinvert[0]),
		    .clkout (ena_clock),
		    .dqsin()																		
		);
		defparam clk_phase_select_pst.physical_clock_source = "PST";
		defparam clk_phase_select_pst.invert_phase = "dynamic";
	end
	else
	begin
		cyclonev_clk_phase_select clk_phase_select_pst (
		    .clkin (leveled_dqs_clocks),
		    .phasectrlin(),
		    .phaseinvertctrl(),
		    .clkout (ena_clock),
		    .dqsin()																		
		);
		defparam clk_phase_select_pst.physical_clock_source = "PST";
		defparam clk_phase_select_pst.use_phasectrlin = "false";
	end
endgenerate

cyclonev_leveling_delay_chain leveling_delay_chain_dq (
    .clkin (phy_clk_dq),
    .delayctrlin (),
    .clkout(leveled_dq_clocks)
);
defparam leveling_delay_chain_dq.physical_clock_source = "DQ";

cyclonev_clk_phase_select clk_phase_select_dq (
    .phasectrlin(),
    .phaseinvertctrl(),
    .dqsin(),
    .clkin (leveled_dq_clocks[0]),
    .clkout (dq_shifted_clock)
);
defparam clk_phase_select_dq.physical_clock_source = "DQ";
defparam clk_phase_select_dq.use_phasectrlin = "false";
defparam clk_phase_select_dq.phase_setting = 0;

cyclonev_leveling_delay_chain leveling_delay_chain_hr (
    .clkin (phy_clk_hr),
    .delayctrlin (),
    .clkout(leveled_hr_clocks)
);
defparam leveling_delay_chain_hr.physical_clock_source = "HR";

cyclonev_clk_phase_select clk_phase_select_hr (
    .phasectrlin(),
    .phaseinvertctrl(),
    .dqsin(),
    .clkin (leveled_hr_clocks[0]),
    .clkout (hr_seq_clock)
);
defparam clk_phase_select_hr.physical_clock_source = "HR";
defparam clk_phase_select_hr.use_phasectrlin = "false";
defparam clk_phase_select_hr.phase_setting = 0;

generate
if (USE_2X_FF == "true")
begin
	wire [3:0] leveled_dqs_2x_clocks;

	cyclonev_leveling_delay_chain leveling_delay_chain_dqs_2x (
		.clkin (phy_clk_dqs_2x),
		.delayctrlin (),
		.clkout(leveled_dqs_2x_clocks)
	);
	defparam leveling_delay_chain_dqs_2x.physical_clock_source = "DQS_2X";
	
	cyclonev_clk_phase_select clk_phase_select_dqs_2x (
    .phasectrlin(),
    .phaseinvertctrl(),
    .dqsin(),
		.clkin (leveled_dqs_2x_clocks[0]),
		.clkout (dqs_dr_clock)
	);
	defparam clk_phase_select_dqs_2x.physical_clock_source = "DQS_2X";
	defparam clk_phase_select_dqs_2x.use_phasectrlin = "false";
	defparam clk_phase_select_dqs_2x.phase_setting = 0;

	assign dq_dr_clock = dqs_dr_clock;
end
endgenerate

generate
wire vfifo_capture_strobe_ena;
wire lfifo_rden;
wire lfifo_oct;
if (USE_HARD_FIFOS == "true" && (PIN_TYPE == "input" || PIN_TYPE == "bidir"))
begin
	wire vfifo_qvld_fr;
	wire vfifo_inc_wr_ptr_fr;
	wire lfifo_rdata_en_fr;
	wire lfifo_rdata_en_full_fr;
	if (USE_HALF_RATE_OUTPUT == "true")
	begin
		cyclonev_ddio_out
		#(
			.half_rate_mode("true"),
			.use_new_clocking_model("true"),
			.async_mode("none")
		) hr_to_fr_vfifo_qvld (		
			.datainhi(vfifo_qvld[0]),
			.datainlo(vfifo_qvld[1]),
			.dataout(vfifo_qvld_fr),
			.clkhi (hr_seq_clock),
			.clklo (hr_seq_clock),
			.hrbypass(dqshalfratebypass[0]),
			.muxsel (hr_seq_clock),
      .clk(),
      .ena(1'b1),
      .areset(),
      .sreset(),
      .dfflo(),
      .dffhi(),
      .devpor(),
      .devclrn()
		);
		
		cyclonev_ddio_out
		#(
			.half_rate_mode("true"),
			.use_new_clocking_model("true"),
			.async_mode("none")
		) hr_to_fr_vfifo_inc_wr_ptr (		
			.datainhi(vfifo_inc_wr_ptr[0]),
			.datainlo(vfifo_inc_wr_ptr[1]),
			.dataout(vfifo_inc_wr_ptr_fr),
			.clkhi (hr_seq_clock),
			.clklo (hr_seq_clock),
			.hrbypass(dqshalfratebypass[0]),
			.muxsel (hr_seq_clock),
      .clk(),
      .ena(1'b1),
      .areset(),
      .sreset(),
      .dfflo(),
      .dffhi(),
      .devpor(),
      .devclrn()
		);
		cyclonev_ddio_out
		#(
			.half_rate_mode("true"),
			.use_new_clocking_model("true"),
			.async_mode("none")
		) hr_to_fr_lfifo_rdata_en (		
			.datainhi(lfifo_rdata_en[0]),
			.datainlo(lfifo_rdata_en[1]),
			.dataout(lfifo_rdata_en_fr),
			.clkhi (hr_seq_clock),
			.clklo (hr_seq_clock),
			.hrbypass(dqshalfratebypass[0]),
			.muxsel (hr_seq_clock),
      .clk(),
      .ena(1'b1),
      .areset(),
      .sreset(),
      .dfflo(),
      .dffhi(),
      .devpor(),
      .devclrn()
		);
		
		cyclonev_ddio_out
		#(
			.half_rate_mode("true"),
			.use_new_clocking_model("true"),
			.async_mode("none")
		) hr_to_fr_lfifo_rdata_en_full (		
			.datainhi(lfifo_rdata_en_full[0]),
			.datainlo(lfifo_rdata_en_full[1]),
			.dataout(lfifo_rdata_en_full_fr),
			.clkhi (hr_seq_clock),
			.clklo (hr_seq_clock),
			.hrbypass(dqshalfratebypass[0]),
			.muxsel (hr_seq_clock),
      .clk(),
      .ena(1'b1),
      .areset(),
      .sreset(),
      .dfflo(),
      .dffhi(),
      .devpor(),
      .devclrn()
		);
	end
	else
	begin
		assign vfifo_qvld_fr = vfifo_qvld[0];
		assign vfifo_inc_wr_ptr_fr = vfifo_inc_wr_ptr[0];
		assign lfifo_rdata_en_fr = lfifo_rdata_en[0];
		assign lfifo_rdata_en_full_fr = lfifo_rdata_en_full[0];
	end
	
	cyclonev_vfifo
	vfifo (
		.rstn (vfifo_reset_n),
		.qvldin (vfifo_qvld_fr),
		.qvldreg (vfifo_capture_strobe_ena),
		.incwrptr (vfifo_inc_wr_ptr_fr),
		.wrclk (dqs_shifted_clock),
		.rdclk (ena_zero_phase_clock)
	);

	wire [LFIFO_RD_LATENCY_WIDTH-1:0] lfifo_rd_latency_full_rate;

	assign lfifo_rd_latency_full_rate = lfifo_rd_latency << (DATA_RATE-1);

	cyclonev_lfifo
	#(
		.oct_lfifo_enable(LFIFO_OCT_EN_MASK)
	) lfifo (
		.rdataen (lfifo_rdata_en_fr),
		.rdataenfull (lfifo_rdata_en_full_fr),
		.rdlatency (lfifo_rd_latency_full_rate),
		.rstn (lfifo_reset_n),
		.clk (dqs_shifted_clock),
		.rden (lfifo_rden),
		.rdatavalid(lfifo_rdata_valid),
		.octlfifo(lfifo_oct)
	);
end
else
begin
	assign vfifo_capture_strobe_ena = 1'b0;
	assign lfifo_rden = 1'b0;
	assign lfifo_rdata_valid = 1'b0;
	assign lfifo_oct = 1'b0;
end
endgenerate

wire delayed_oct;
generate
	wire fr_os_oct;
	wire aligned_os_oct;

	if (USE_HALF_RATE_OUTPUT == "true")
	begin
		if (EMIF_BYPASS_OCT_DDIO == "true")
		begin
			assign fr_os_oct = oct_ena[0];
		end
		else
		begin
			cyclonev_ddio_out
			#(
				.half_rate_mode("true"),
				.use_new_clocking_model("true"),
				.async_mode("none")
			) hr_to_fr_os_oct (		
				.datainhi(oct_ena[0]),
				.datainlo(oct_ena[1]),
				.dataout(fr_os_oct),
				.clkhi (hr_seq_clock),
				.clklo (hr_seq_clock),
				.hrbypass(dqshalfratebypass[0]),
				.muxsel (hr_seq_clock),
			.clk(),
			.ena(1'b1),
			.areset(),
			.sreset(),
			.dfflo(),
			.dffhi(),
			.devpor(),
			.devclrn()
			);
		end
	end
	else
	begin
		assign fr_os_oct = fr_term;		
	end

	if (USE_HARD_FIFOS == "true")
	begin
		if (EMIF_BYPASS_OCT_DDIO == "true")
		begin
			assign aligned_os_oct = fr_os_oct;
		end
		else
		begin
			cyclonev_ddio_oe # (
			.disable_second_level_register("true")
			) os_oct_ddio_oe (
				.clk (dqs_shifted_clock),
				.oe (fr_os_oct),
				.octreadcontrol (lfifo_oct),
				.dataout (aligned_os_oct),
				.ena (1'b1),
				.areset (),
				.sreset (),
				.dfflo (),
				.dffhi (),
				.devpor (),
				.devclrn ()
			);
		end
	end
	else
	begin
		reg oct_oe_reg;	
		always @(posedge dqs_shifted_clock) oct_oe_reg <= fr_os_oct;
		assign aligned_os_oct = oct_oe_reg;
	end
	
	wire predelayed_os_oct;
	if (USE_2X_FF == "true")
	begin
		reg dd_os_oct;
		always @(posedge dqs_dr_clock)
		begin
			dd_os_oct <= aligned_os_oct;
		end
		assign predelayed_os_oct = dd_os_oct;
	end
	else
	begin
		assign predelayed_os_oct = aligned_os_oct;
	end	
	
	if (USE_DYNAMIC_CONFIG == "true")
	begin
		if (EMIF_BYPASS_OCT_DDIO == "true")
		begin
			assign delayed_oct = predelayed_os_oct;
		end
		else
		begin
			cyclonev_delay_chain # (
				.sim_intrinsic_rising_delay(0),
				.sim_intrinsic_falling_delay(0)
			) oct_delay (
				.datain             (predelayed_os_oct),
				.delayctrlin        (octdelaysetting1_dlc[0]),
				.dataout            (delayed_oct)
			);
		end
	end
	else
	begin
		assign delayed_oct = predelayed_os_oct;
	end
endgenerate


generate 
if (PIN_TYPE == "input" || PIN_TYPE == "bidir")
begin

	assign capture_strobe = dqsbusout;
	wire dqsin;
	
	wire capture_strobe_ibuf_i;
	wire capture_strobe_ibuf_ibar;

	if (USE_BIDIR_STROBE == "true")
	begin
		if (SWAP_CAPTURE_STROBE_POLARITY == "true") begin
			assign capture_strobe_ibuf_i = strobe_n_io;
			assign capture_strobe_ibuf_ibar = strobe_io;
		end else begin
			assign capture_strobe_ibuf_i = strobe_io;
			assign capture_strobe_ibuf_ibar = strobe_n_io;
		end
	end
	else
	begin
		if (SWAP_CAPTURE_STROBE_POLARITY == "true") begin
			assign capture_strobe_ibuf_i = capture_strobe_n_in;
			assign capture_strobe_ibuf_ibar = capture_strobe_in;
		end else begin
			assign capture_strobe_ibuf_i = capture_strobe_in;
			assign capture_strobe_ibuf_ibar = capture_strobe_n_in;
		end		
	end	
	
	if (DIFFERENTIAL_CAPTURE_STROBE == "true")
	begin
		cyclonev_io_ibuf
		#(
			.differential_mode(DIFFERENTIAL_CAPTURE_STROBE),
			.bus_hold("false")
		) strobe_in (					      
			.i(capture_strobe_ibuf_i),
			.ibar(capture_strobe_ibuf_ibar),
			.o(dqsin),
			.dynamicterminationcontrol(1'b0)
		);
	end
	else
	begin
		cyclonev_io_ibuf
		#(
			.bus_hold("false")
		) strobe_in (					      
			.i(capture_strobe_ibuf_i),
			.o(dqsin),
			.ibar(),
			.dynamicterminationcontrol(1'b0)
		);
	end

	
	wire capture_strobe_ena_fr;
	if (USE_HARD_FIFOS == "true")
	begin
		assign capture_strobe_ena_fr = vfifo_capture_strobe_ena;
	end
	else
	begin
		if (DQS_ENABLE_WIDTH > 1)
		begin
				cyclonev_ddio_out
				#(
					.half_rate_mode("true"),
					.use_new_clocking_model("true"),
					.async_mode("none")
				) hr_to_fr_ena (					      
						.datainhi(capture_strobe_ena[0]),
						.datainlo(capture_strobe_ena[1]),
						.dataout(capture_strobe_ena_fr),
						.clkhi (strobe_ena_hr_clock_in),
						.clklo (strobe_ena_hr_clock_in),
						.hrbypass(dqshalfratebypass[0]),
						.muxsel (strobe_ena_hr_clock_in)
				);				
		end	
		else
		begin
			assign capture_strobe_ena_fr = capture_strobe_ena;
		end
	end



	wire dqs_enable_shifted;
	wire dqs_shifted;
	wire dqs_enable_int;
	wire dqs_disable_int;

	if (USE_BIDIR_STROBE == "true")
	begin
		wire dqs_pre_delayed;

		assign dqs_pre_delayed = capture_strobe_ena_fr;
		if (USE_DYNAMIC_CONFIG == "true")
		begin
			cyclonev_dqs_enable_ctrl
			#(
				.add_phase_transfer_reg(DYNAMIC_MODE),
				.delay_dqs_enable("one_and_half_cycle")
			) dqs_enable_ctrl (
				.dqsenablein (dqs_pre_delayed),
				.zerophaseclk (ena_zero_phase_clock),
				.levelingclk (ena_clock),
				.dqsenableout (dqs_enable_shifted),
				.enaphasetransferreg(enadqsenablephasetransferreg[0]),
			  .rstn(),
	      .dffin()
			);
		end
		else
		begin
			cyclonev_dqs_enable_ctrl
			#(
				.delay_dqs_enable("one_and_half_cycle")
			) dqs_enable_ctrl (
				.dqsenablein (dqs_pre_delayed),
				.zerophaseclk (ena_zero_phase_clock),
				.levelingclk (ena_clock),
				.dqsenableout (dqs_enable_shifted),
			  .rstn(),
	      .dffin()
			);
		end

		cyclonev_dqs_delay_chain
		#(
			.dqs_period(INPUT_FREQ_PS),
			.dqs_phase_shift(DQS_PHASE_SHIFT),
			.dqs_delay_chain_bypass(DQS_DELAYCHAIN_BYPASS)
		)
		dqs_delay_chain (
			.dqsin (dqsin),
			.delayctrlin (dll_delay_value),
			.dqsenable (dqs_enable_int),
			.dqsdisablen (dqs_disable_int),
			.dqsbusout (dqs_shifted),
			.dqsupdateen(),
      .testin(),
      .dffin()
		);
	end
	else
	begin
		if (USE_DYNAMIC_CONFIG == "true")
		begin
			cyclonev_dqs_delay_chain
			#(
				.dqs_period(INPUT_FREQ_PS),
				.dqs_phase_shift(DQS_PHASE_SHIFT),
				.dqs_delay_chain_bypass(DQS_DELAYCHAIN_BYPASS)
			) dqs_delay_chain (
				.dqsin (dqsin),
				.delayctrlin (dll_delay_value),
				.dqsbusout (dqs_shifted),
			  .dqsenable (),
			  .dqsdisablen (),
			  .dqsupdateen(),
        .testin(),
        .dffin()
			);	
		end
		else
		begin
			cyclonev_dqs_delay_chain
			#(
				.dqs_period(INPUT_FREQ_PS),
				.dqs_phase_shift(DQS_PHASE_SHIFT),
				.dqs_delay_chain_bypass(DQS_DELAYCHAIN_BYPASS)
			) dqs_delay_chain (
				.dqsin (dqsin),
				.delayctrlin (dll_delay_value),
				.dqsbusout (dqs_shifted),
			  .dqsenable (),
			  .dqsdisablen (),
			  .dqsupdateen(),
        .testin(),
        .dffin()
			);	
	
		end
	end

	if (USE_DYNAMIC_CONFIG == "true")
	begin
		cyclonev_delay_chain
		#(.sim_intrinsic_rising_delay(0), .sim_intrinsic_falling_delay(0))
		dqs_in_delay_1(
			.datain             (dqs_shifted),
			.delayctrlin        (dqsbusoutdelaysetting_dlc[0]),
			.dataout            (dqsbusout)
		);

		cyclonev_delay_chain
		#(.sim_intrinsic_rising_delay(0), .sim_intrinsic_falling_delay(0))
		dqs_ena_delay_1(
			.datain             (dqs_enable_shifted),
			.delayctrlin        (dqsenabledelaysetting_dlc[0]),
			.dataout            (dqs_enable_int)
		);
	
		cyclonev_delay_chain
		#(.sim_intrinsic_rising_delay(0), .sim_intrinsic_falling_delay(0))
		dqs_dis_delay_1(
			.datain             (dqs_enable_shifted),
			.delayctrlin        (dqsdisabledelaysetting_dlc[0]),
			.dataout            (dqs_disable_int)
		);
	end
	else
	begin
		assign dqsbusout = dqs_shifted;
		assign dqs_enable_int = dqs_enable_shifted;
		assign dqs_disable_int = dqs_enable_shifted;
	end

	if (USE_DQS_TRACKING == "true")
	begin
		reg dqs_ff;
		always @(negedge dqs_enable_int)
			dqs_ff <= dqsin;

		assign capture_strobe_tracking = dqs_ff;
	end

	if (SEPARATE_CAPTURE_STROBE == "true")
	begin
	
		wire dqsnin;
		
		cyclonev_io_ibuf
		#(
			.bus_hold("false")
		) strobe_n_in (
			.i(capture_strobe_ibuf_ibar),
			.o(dqsnin)
		);

		wire dqsn_enable_int;
		wire dqsn_disable_int;
		wire dqsn_enable_shifted;
		wire dqsn_shifted;

		if (USE_BIDIR_STROBE == "true")
		begin
			if (USE_DYNAMIC_CONFIG == "true")
			begin
				cyclonev_dqs_enable_ctrl
				#(
					.add_phase_transfer_reg(DYNAMIC_MODE),
					.delay_dqs_enable("one_and_half_cycle")
				) dqs_enable_n_ctrl (
					.dqsenablein (capture_strobe_ena_fr),
					.zerophaseclk (ena_zero_phase_clock),
					.levelingclk (ena_clock),
					.dqsenableout (dqsn_enable_shifted),
					.enaphasetransferreg(enadqsenablephasetransferreg[0])
				);
			end
			else
			begin
				cyclonev_dqs_enable_ctrl
				#(
					.add_phase_transfer_reg("false"),
					.delay_dqs_enable("one_and_half_cycle")
				) dqs_enable_n_ctrl (
					.dqsenablein (capture_strobe_ena_fr),
					.zerophaseclk (ena_zero_phase_clock),
					.levelingclk (ena_clock),
					.dqsenableout (dqsn_enable_shifted)
				);
			end

			cyclonev_dqs_delay_chain
			#(
				.dqs_period(INPUT_FREQ_PS),
				.dqs_phase_shift(DQS_PHASE_SHIFT),
				.dqs_delay_chain_bypass(DQS_DELAYCHAIN_BYPASS)
			) dqs_n_delay_chain (
				.dqsin (dqsnin),
				.delayctrlin (dll_delay_value),
				.dqsenable (dqsn_enable_int),
				.dqsdisablen (dqsn_disable_int),
				.dqsbusout (dqsn_shifted)
			);

		end
		else
		begin
			cyclonev_dqs_delay_chain
			#(
				.dqs_period(INPUT_FREQ_PS),
				.dqs_phase_shift(DQS_PHASE_SHIFT),
				.dqs_delay_chain_bypass(DQS_DELAYCHAIN_BYPASS)
			) dqs_n_delay_chain (
				.dqsin (dqsnin),
				.delayctrlin (dll_delay_value),
				.dqsbusout (dqsn_shifted)
			);
		end
		
		if (USE_DYNAMIC_CONFIG == "true")
		begin	
			cyclonev_delay_chain
			#(.sim_intrinsic_rising_delay(0), .sim_intrinsic_falling_delay(0))
			dqs_n_delay_1(
				.datain             (dqsn_shifted),
				.delayctrlin        (dqsbusoutdelaysetting_dlc[0]),
				.dataout            (dqsnbusout)
			);

			cyclonev_delay_chain
			#(.sim_intrinsic_rising_delay(0), .sim_intrinsic_falling_delay(0))
			dqs_n_ena_delay_1(
				.datain             (dqsn_enable_shifted),
				.delayctrlin        (dqsenabledelaysetting_dlc[0]),
				.dataout            (dqsn_enable_int)
			);

			cyclonev_delay_chain
			#(.sim_intrinsic_rising_delay(0), .sim_intrinsic_falling_delay(0))
			dqs_n_dis_delay_1(
				.datain             (dqsn_enable_shifted),
				.delayctrlin        (dqsdisabledelaysetting_dlc[0]),
				.dataout            (dqsn_disable_int)
			);
		
		end
		else
		begin
			assign dqsnbusout = dqsn_shifted;
			assign dqsn_enable_int = dqsn_enable_shifted;
			assign dqsn_disable_int = dqsn_enable_shifted;
		end
	end
end
endgenerate

generate
if (USE_OUTPUT_STROBE == "true")
begin
	wire os;
	wire os_bar;
	wire os_dtc;
	wire os_dtc_bar;
	wire os_delayed1;
	wire os_delayed2;

	wire fr_os_oe;
	wire aligned_os_oe;
	wire aligned_strobe;
	
	wire fr_os_hi;
	wire fr_os_lo;
	
	if (USE_HALF_RATE_OUTPUT == "true")
	begin
		if (USE_BIDIR_STROBE == "true")
		begin		
			wire clk_gate_hi;
			wire clk_gate_lo;
			
			if (PREAMBLE_TYPE == "low")
			begin
				if (EMIF_UNALIGNED_PREAMBLE_SUPPORT != "true")
				begin
					assign clk_gate_hi = output_strobe_ena[0];
					assign clk_gate_lo = output_strobe_ena[0];
				end 
				else
				begin 
					reg [1:0] os_ena_reg;
					reg [1:0] os_ena_preamble;

					always @(posedge core_clock_in) 
					begin
						os_ena_reg[1:0] <= output_strobe_ena[1:0];
					end

					always @(*)
					begin
						case ({os_ena_reg[0], os_ena_reg[1],
							   output_strobe_ena[0], output_strobe_ena[1]}) 
							4'b00_00: os_ena_preamble[1:0] <= 2'b00;
							4'b00_01: os_ena_preamble[1:0] <= 2'b00; 
							4'b00_10: os_ena_preamble[1:0] <= 2'b00; 
							4'b00_11: os_ena_preamble[1:0] <= 2'b01; 

							4'b01_00: os_ena_preamble[1:0] <= 2'b00;
							4'b01_01: os_ena_preamble[1:0] <= 2'b00; 
							4'b01_10: os_ena_preamble[1:0] <= 2'b10;
							4'b01_11: os_ena_preamble[1:0] <= 2'b11;

							4'b10_00: os_ena_preamble[1:0] <= 2'b00;
							4'b10_01: os_ena_preamble[1:0] <= 2'b00; 
							4'b10_10: os_ena_preamble[1:0] <= 2'b00; 
							4'b10_11: os_ena_preamble[1:0] <= 2'b01; 

							4'b11_00: os_ena_preamble[1:0] <= 2'b00;
							4'b11_01: os_ena_preamble[1:0] <= 2'b00; 
							4'b11_10: os_ena_preamble[1:0] <= 2'b10;
							4'b11_11: os_ena_preamble[1:0] <= 2'b11;

							default:  os_ena_preamble[1:0] <= 2'b00;
						endcase
					end

					assign clk_gate_hi = os_ena_preamble[1];
					assign clk_gate_lo = os_ena_preamble[0];
				end 
			end
			else
			begin
				assign clk_gate_hi = 1'b1;
				assign clk_gate_lo = 1'b1;
			end 
			cyclonev_ddio_out
			#(
				.half_rate_mode("true"),
				.use_new_clocking_model("true"),
				.async_mode("none")	
			) hr_to_fr_os_hi (
					.hrbypass(1'b1),
					.datainhi(write_strobe[0]),
					.datainlo(write_strobe[2]),
					.dataout(fr_os_hi),
					.clkhi (hr_seq_clock),
					.clklo (hr_seq_clock),
					.muxsel (hr_seq_clock),
					.clk(),
          .ena(1'b1),
          .areset(),
          .sreset(),
          .dfflo(),
          .dffhi(),
          .devpor(),
          .devclrn()
			);			

			cyclonev_ddio_out
			#(
					.half_rate_mode("true"),
					.use_new_clocking_model("true"),
					.async_mode("none")
			) hr_to_fr_os_lo (	
					.hrbypass(1'b1),
					.datainhi(write_strobe[1]),
					.datainlo(write_strobe[3]),
					.dataout(fr_os_lo),
					.clkhi (hr_seq_clock),
					.clklo (hr_seq_clock),
					.muxsel (hr_seq_clock),
					.clk(),
          .ena(1'b1),
          .areset(),
          .sreset(),
          .dfflo(),
          .dffhi(),
          .devpor(),
          .devclrn()
			);			

			cyclonev_ddio_out
			#(
					.half_rate_mode("true"),
					.use_new_clocking_model("true"),
					.async_mode("none")
			) hr_to_fr_os_oe (								  
					.hrbypass(1'b1),
					.datainhi(~output_strobe_ena [0]),
					.datainlo(~output_strobe_ena [1]),
					.dataout(fr_os_oe),
					.clkhi (hr_seq_clock),
					.clklo (hr_seq_clock),
					.muxsel (hr_seq_clock),
					.clk(),
          .ena(1'b1),
          .areset(),
          .sreset(),
          .dfflo(),
          .dffhi(),
          .devpor(),
          .devclrn()
			);

		end
		else 
		begin
			wire gnd_lut /* synthesis keep = 1*/;
			assign gnd_lut = 1'b0;
			assign fr_os_lo = gnd_lut;
			
			assign fr_os_oe = 1'b0;		
			
			if (USE_OUTPUT_STROBE_RESET == "true") begin
				reg clk_h /* synthesis dont_merge */;
				always @(posedge write_strobe_clock_in or negedge reset_n_core_clock_in)
				begin
					if (~reset_n_core_clock_in)
						clk_h <= 1'b0;
					else
						clk_h <= 1'b1;
				end			
				assign fr_os_hi = clk_h;
			end else begin
				assign fr_os_hi = 1'b1;
			end
		end
	end
	else
	begin
		if (USE_BIDIR_STROBE == "true")
		begin
			assign fr_os_oe = ~output_strobe_ena[0];

			wire gnd_lut /* synthesis keep = 1*/;
			assign gnd_lut = 1'b0;
			assign fr_os_lo = gnd_lut;

			if (PREAMBLE_TYPE == "low")
			begin
				reg os_ena_reg1;
				initial
					os_ena_reg1 = 0;
				always @(posedge core_clock_in)
					os_ena_reg1 <= output_strobe_ena[0];
	
				assign fr_os_hi = os_ena_reg1 & output_strobe_ena[0];
			end
			else
			begin
			
				wire vcc_lut /* synthesis keep = 1*/;
				assign vcc_lut = 1'b1;
				assign fr_os_hi = vcc_lut;
			end
		end
		else
		begin
			wire gnd_lut /* synthesis keep = 1*/;
			assign gnd_lut = 1'b0;
			assign fr_os_lo = gnd_lut;
			
			assign fr_os_oe = 1'b0;			

			if (USE_OUTPUT_STROBE_RESET == "true") begin
				reg clk_h /* synthesis dont_merge */;
				always @(posedge write_strobe_clock_in or negedge reset_n_core_clock_in)
				begin
					if (~reset_n_core_clock_in)
						clk_h <= 1'b0;
					else
						clk_h <= 1'b1;
				end			
				assign fr_os_hi = clk_h;
			end else begin
				assign fr_os_hi = 1'b1;
			end
		end		
	end

	if (USE_OUTPUT_PHASE_ALIGNMENT == "true")
	begin
	end
	else
	begin

		/*
		cyclonev_ddio_oe
		os_oe_ddio_oe (
			.clk (write_strobe_clock_in),
			.oe (fr_os_oe),
			.dataout (aligned_os_oe)
		);
		*/
		reg os_oe_reg;
		always @(posedge write_strobe_clock) os_oe_reg <= fr_os_oe;
		assign aligned_os_oe = os_oe_reg;
		
		cyclonev_ddio_out
		#(
				.half_rate_mode("false"),
				.use_new_clocking_model("true"),
				.async_mode("none")
		) phase_align_os (
			.datainhi(fr_os_hi),
			.datainlo(fr_os_lo),
			.dataout(aligned_strobe),
			.clkhi (write_strobe_clock),
			.clklo (write_strobe_clock),
			.muxsel (write_strobe_clock),
		  .clk(),
      .ena(1'b1),
      .areset(),
      .sreset(),
      .dfflo(),
      .dffhi(),
      .devpor(),
      .devclrn(),
			.hrbypass()
		);
	end
	
	wire delayed_os_oe;
	wire predelayed_os;
	wire predelayed_os_oe;
	
	if (USE_2X_FF == "true")
	begin
		reg dd_os;
		reg dd_os_oe;
		always @(posedge dqs_dr_clock)
		begin
			dd_os <= aligned_strobe;
			dd_os_oe <= aligned_os_oe;
		end
		assign predelayed_os = dd_os;
		assign predelayed_os_oe = dd_os_oe;
	end
	else
	begin
		assign predelayed_os = aligned_strobe;
		assign predelayed_os_oe = aligned_os_oe;
	end
	
	if (USE_DYNAMIC_CONFIG == "true")
	begin
`ifndef FAMILY_HAS_NO_DYNCONF
		wire delayed_os_oe_1;

		cyclonev_delay_chain
		#(.sim_intrinsic_rising_delay(0), .sim_intrinsic_falling_delay(0))
		dqs_out_delay_1(
			.datain             (predelayed_os),
			.delayctrlin        (dqs_outputdelaysetting_dlc),
			.dataout            (os_delayed1)
		);

		assign os_delayed2 = os_delayed1;

		cyclonev_delay_chain
		#(.sim_intrinsic_rising_delay(0), .sim_intrinsic_falling_delay(0))
		oe_delay_1(
			.datain             (predelayed_os_oe),
			.delayctrlin        (dqs_outputenabledelaysetting_dlc),
			.dataout            (delayed_os_oe_1)
		);

			assign delayed_os_oe = delayed_os_oe_1;
`endif
	end
	else
	begin
		assign os_delayed2 = aligned_strobe;
		assign delayed_os_oe = aligned_os_oe;
	end

	wire diff_oe;
	wire diff_oe_bar;
	wire diff_dtc;
	wire diff_dtc_bar;

	if (DIFFERENTIAL_OUTPUT_STROBE=="true")
	begin
		if (USE_BIDIR_STROBE == "true")
		begin
			cyclonev_pseudo_diff_out   pseudo_diffa_0
			( 
				.oein(delayed_os_oe),
				.dtcin(delayed_oct),
				.oeout(diff_oe),
				.oebout(diff_oe_bar),
				.dtc(diff_dtc),
				.dtcbar(diff_dtc_bar),
				.i(os_delayed2),
				.o(os),
				.obar(os_bar)
			);		
		
			cyclonev_io_obuf
			#(
			  	.sim_dynamic_termination_control_is_connected("true"),
				.bus_hold("false"),
				.open_drain_output("false")
			) obuf_os_bar_0
			( 
				.i(os_bar),
				.o(strobe_n_io),
				.obar(),
				.oe(~diff_oe_bar),
				.parallelterminationcontrol	(parallelterminationcontrol_in),
				.dynamicterminationcontrol	(diff_dtc_bar),
				.seriesterminationcontrol	(seriesterminationcontrol_in),
				.devoe()
			);
		end
		else
		begin
			cyclonev_pseudo_diff_out   pseudo_diffa_0
			( 
				.oein(1'b0),
				.oeout(diff_oe),
				.oebout(diff_oe_bar),
				.i(os_delayed2),
				.o(os),
				.obar(os_bar),
				.dtcin(),
				.dtc(),
				.dtcbar()
			);
		
			cyclonev_io_obuf
			#(
				.bus_hold("false"),
				.open_drain_output("false")
			) obuf_os_bar_0
			( 
				.i(os_bar),
				.o(output_strobe_n_out),
				.obar(),
				.oe(~diff_oe_bar),
				.dynamicterminationcontrol(1'b0),
				.seriesterminationcontrol(),
				.parallelterminationcontrol(),
				.devoe()
			);
		end
	end
	else
	begin
		assign os = os_delayed2;
		assign diff_dtc = delayed_oct;
		assign diff_oe = delayed_os_oe;
	end


	if (USE_BIDIR_STROBE == "true")
	begin
		cyclonev_io_obuf
		#(
		  	.sim_dynamic_termination_control_is_connected("true"),
			.bus_hold("false"),
			.open_drain_output("false")
		) obuf_os_0
			( 
			.i(os),
			.o(strobe_io),
			.obar(),
			.oe(~diff_oe),
			.parallelterminationcontrol	(parallelterminationcontrol_in),
			.dynamicterminationcontrol	(diff_dtc),
			.seriesterminationcontrol	(seriesterminationcontrol_in),
			.devoe()
		);
	end
	else
	begin
		cyclonev_io_obuf
		#(
			.bus_hold("false"),
			.open_drain_output("false")
		) obuf_os_0			  
			( 
			.i(os),
			.o(output_strobe_out),
			.obar(),
			.oe(~diff_oe),
			.parallelterminationcontrol	(),		
			.dynamicterminationcontrol	(1'b0),
			.seriesterminationcontrol	(),
			.devoe()
			);
	
	end	
end
endgenerate


wire [PIN_WIDTH-1:0] aligned_oe ;
wire [PIN_WIDTH-1:0] aligned_data;
wire [PIN_WIDTH-1:0] ddr_data;
wire [PIN_WIDTH-1:0] dq_outputhalfratebypass;
wire [PIN_WIDTH*2-1:0] rfifo_clock_select;
wire [PIN_WIDTH*3-1:0] rfifo_mode;


generate
	if (PIN_TYPE == "output" || PIN_TYPE == "bidir")
	begin
		genvar opin_num;
		for (opin_num = 0; opin_num < PIN_WIDTH; opin_num = opin_num + 1)
		begin :output_path_gen
			wire fr_data_hi;
			wire fr_data_lo;
			wire fr_oe;
			
			if (USE_HALF_RATE_OUTPUT == "true")
			begin
				wire hr_data_t0;
				wire hr_data_t1;
				wire hr_data_t2;
				wire hr_data_t3;
				wire write_oe_hi;
				wire write_oe_lo;

				if (NATURAL_ALIGNMENT == "true")
				begin
					assign hr_data_t0 = write_data_in [opin_num * rate_mult_out];
		  		assign hr_data_t1 = write_data_in [opin_num * rate_mult_out + 1];
		  		assign hr_data_t2 = write_data_in [opin_num * rate_mult_out + 2];
					assign hr_data_t3 = write_data_in [opin_num * rate_mult_out + 3];
					assign write_oe_hi = write_oe_in [2*opin_num + 0];
					assign write_oe_lo = write_oe_in [2*opin_num + 1];
				end
				else if (REGULAR_WRITE_BUS_ORDERING == "true")
			  	begin
					assign hr_data_t0 = write_data_in [opin_num + 0*PIN_WIDTH];
		  		assign hr_data_t1 = write_data_in [opin_num + 1*PIN_WIDTH];
		  		assign hr_data_t2 = write_data_in [opin_num + 2*PIN_WIDTH];
					assign hr_data_t3 = write_data_in [opin_num + 3*PIN_WIDTH];
					assign write_oe_hi = write_oe_in [opin_num + 0];
					assign write_oe_lo = write_oe_in [opin_num + PIN_WIDTH];
				end
				else
			  	begin
					assign hr_data_t0 = write_data_in [opin_num + 1*PIN_WIDTH];
					assign hr_data_t1 = write_data_in [opin_num + 0*PIN_WIDTH];
					assign hr_data_t2 = write_data_in [opin_num + 3*PIN_WIDTH];
					assign hr_data_t3 = write_data_in [opin_num + 2*PIN_WIDTH];
					assign write_oe_hi = write_oe_in [opin_num + 0];
					assign write_oe_lo = write_oe_in [opin_num + PIN_WIDTH];
				end
						
				cyclonev_ddio_out 
				#(
					.half_rate_mode("true"),
					.use_new_clocking_model("true"),
					.async_mode("none")
				) hr_to_fr_hi (			  
					.datainhi(hr_data_t0),
					.datainlo(hr_data_t2),
					.dataout(fr_data_hi),
					.clkhi (hr_seq_clock),
					.clklo (hr_seq_clock),
					.hrbypass(dq_outputhalfratebypass[opin_num]),
					.muxsel (hr_seq_clock),
					.clk(),
          .ena(1'b1),
          .areset(),
          .sreset(),
          .dfflo(),
          .dffhi(),
          .devpor(),
          .devclrn()
				);				
				
				cyclonev_ddio_out
				#(
					.half_rate_mode("true"),
					.use_new_clocking_model("true"),
					.async_mode("none")
				) hr_to_fr_lo (			  
					.datainhi(hr_data_t1),
					.datainlo(hr_data_t3),
					.dataout(fr_data_lo),
					.clkhi (hr_seq_clock),
					.clklo (hr_seq_clock),
					.hrbypass(dq_outputhalfratebypass[opin_num]),
					.muxsel (hr_seq_clock),
					.clk(),
          .ena(1'b1),
          .areset(),
          .sreset(),
          .dfflo(),
          .dffhi(),
          .devpor(),
          .devclrn()
				);

				cyclonev_ddio_out
				#(
				.half_rate_mode("true"),
				.use_new_clocking_model("true")
				) hr_to_fr_oe (
					.datainhi(~write_oe_hi),
					.datainlo(~write_oe_lo),
					.dataout(fr_oe),
					.clkhi (hr_seq_clock),
					.clklo (hr_seq_clock),
					.hrbypass(dq_outputhalfratebypass[opin_num]),
					.muxsel (hr_seq_clock),
					.clk(),
          .ena(1'b1),
          .areset(),
          .sreset(),
          .dfflo(),
          .dffhi(),
          .devpor(),
          .devclrn()
				);												
			end
			else
			begin
				if (NATURAL_ALIGNMENT == "true")
				begin
					assign fr_data_lo = write_data_in [opin_num * rate_mult_out + 1];
					assign fr_data_hi = write_data_in [opin_num * rate_mult_out];
				end
				else
				begin
					assign fr_data_lo = write_data_in [opin_num+PIN_WIDTH];
					assign fr_data_hi = write_data_in [opin_num];
				end
				assign fr_oe = ~write_oe_in [opin_num];
			end
			
			if (USE_OUTPUT_PHASE_ALIGNMENT == "true")
			begin
			end
			else
			begin
				cyclonev_ddio_out
				#(
					.async_mode("none"),
					.half_rate_mode("false"),
					.sync_mode("none"),
					.use_new_clocking_model("true")
				) ddio_out (
					.datainhi(fr_data_hi),
					.datainlo(fr_data_lo),
					.dataout(aligned_data[opin_num]),
					.clkhi (dq_shifted_clock),
					.clklo (dq_shifted_clock),
					.muxsel (dq_shifted_clock),
					.clk(),
          .ena(1'b1),
          .areset(),
          .sreset(),
          .dfflo(),
          .dffhi(),
          .devpor(),
          .devclrn(),
					.hrbypass()	
				);


				/*
				cyclonev_ddio_oe
				dq_oe_ddio_oe (
					.clk (fr_clock_in),
					.oe (fr_oe),
					.dataout (aligned_oe [opin_num])
				);
				*/
				reg oe_reg /* synthesis dont_merge altera_attribute="FAST_OUTPUT_ENABLE_REGISTER=on" */;
				always @(posedge dq_shifted_clock) oe_reg <= fr_oe;
				assign aligned_oe[opin_num] = oe_reg;
			end
		end
	end
endgenerate


generate
if (PIN_TYPE == "input" || PIN_TYPE == "bidir")
begin
	genvar ipin_num;
	for (ipin_num = 0; ipin_num < PIN_WIDTH; ipin_num = ipin_num + 1)
	begin :input_path_gen

		wire [1:0] sdr_data;
		wire [1:0] aligned_input;
		wire dqsbusout_to_ddio_in;
		wire dqsnbusout_to_ddio_in;
		
		if (INVERT_CAPTURE_STROBE == "true") begin
			assign dqsbusout_to_ddio_in = ~dqsbusout;
			if (SEPARATE_CAPTURE_STROBE == "true") begin
				assign dqsnbusout_to_ddio_in = ~dqsnbusout;
			end
		end else begin
			assign dqsbusout_to_ddio_in = dqsbusout;
			if (SEPARATE_CAPTURE_STROBE == "true") begin
				assign dqsnbusout_to_ddio_in = dqsnbusout;
			end
		end
		
		if (SEPARATE_CAPTURE_STROBE == "true") begin
			cyclonev_ddio_in
			#(
				.use_clkn("true"),
				.async_mode("none"),
				.sync_mode("none")
			) capture_reg(
				.datain(ddr_data[ipin_num]),
				.clk (dqsbusout_to_ddio_in),
				.clkn (dqsnbusout_to_ddio_in),
				.regouthi(sdr_data[1]),
				.regoutlo(sdr_data[0]),
        .ena(1'b1),
        .areset(),
        .sreset(),
        .dfflo(),
        .devpor(),
        .devclrn()
			);
		end else begin
			cyclonev_ddio_in
			#(
				.use_clkn("false"),
				.async_mode("none"),
				.sync_mode("none")
			)  capture_reg(
				.datain(ddr_data[ipin_num]),
				.clk (dqsbusout_to_ddio_in),
				.regouthi(sdr_data[1]),
				.regoutlo(sdr_data[0]),
				.clkn(),
        .ena(1'b1),
        .areset(),
        .sreset(),
        .dfflo(),
        .devpor(),
        .devclrn()
			);
		end
		
		if (USE_INPUT_PHASE_ALIGNMENT == "true") 
		begin
		end
		else
		begin
			assign aligned_input = sdr_data;
		end
		
		wire [3:0] read_fifo_out;
		if (USE_HARD_FIFOS == "true")
		begin
			wire wren;
			if (USE_BIDIR_STROBE == "true")
			begin
				assign wren = 1'b1;
			end
			else
			begin
				assign wren = vfifo_capture_strobe_ena;
			end
			
			wire rfifo_rd_clk;
			if(USE_DYNAMIC_CONFIG == "true") begin
				cyclonev_read_fifo_read_clock_select
				read_fifo_clk_sel
				(
					.clkin({hr_seq_clock, dqs_shifted_clock, 1'b0}),
					.clksel(rfifo_clock_select[(ipin_num+1)*2-1:ipin_num*2]),
					.clkout (rfifo_rd_clk)
				);
			end
			else begin
				if (USE_HALF_RATE_OUTPUT == "true") begin
					assign rfifo_rd_clk = hr_seq_clock;
				end
				else begin
					assign rfifo_rd_clk = dqs_shifted_clock;
				end
			end

			wire writeclk;

			// in skip-cal mode, the read_fifo writeclk cannot be x otherwise we pick up an 
			// extra edge at the beginning and there is no calibration to reset things after the first edge
			// synthesis translate_off
			assign writeclk = (dqsbusout_to_ddio_in === 1'b0) ? 1'b0 : 1'b1;
			// synthesis translate_on
			// synthesis read_comments_as_HDL on
			// assign writeclk = dqsbusout_to_ddio_in;
			// synthesis read_comments_as_HDL off
			
			localparam READ_FIFO_MODE = (USE_HALF_RATE_OUTPUT == "true") ? "hrate_mode" : "frate_mode";
			
			cyclonev_ir_fifo_userdes
			read_fifo
			(
				.rstn (rfifo_reset_n),
				.dinfiforx (aligned_input),
				.writeclk (writeclk),
				.writeenable (wren),
				.dout (read_fifo_out),
				.readclk (rfifo_rd_clk),
				.dynfifomode(rfifo_mode[(ipin_num+1)*3-1:ipin_num*3]),
				.readenable(lfifo_rden),
			  .tstclk(),
        .regscanovrd(),
        .bslipin(),
        .txin(),
        .loaden(),
        .bslipctl(),
        .regscan(),
        .scanin(),
        .lvdsmodeen(),
        .lvdstxsel(),
        .txout(),
        .rxout(),
        .bslipout(),
        .bslipmax(),
        .scanout(),
        .observableout(),
        .observablefout1(),
        .observablefout2(),
        .observablefout3(),
        .observablefout4(),
        .observablewaddrcnt(),
        .observableraddrcnt()
			);
			defparam read_fifo.a_use_dynamic_fifo_mode = USE_DYNAMIC_CONFIG;
			defparam read_fifo.a_rb_fifo_mode = READ_FIFO_MODE;
			defparam read_fifo.a_sim_wclk_pre_delay = 10;
			defparam read_fifo.a_sim_readenable_pre_delay = 10;
		end
		else
		begin
			assign read_fifo_out = aligned_input;
		end
		
		if (REVERSE_READ_WORDS == "true")
		begin
			if (USE_HALF_RATE_OUTPUT == "true")
			begin
				assign read_data_out [ipin_num] = read_fifo_out [3];
				assign read_data_out [PIN_WIDTH +ipin_num] = read_fifo_out [2];
				assign read_data_out [PIN_WIDTH*2 +ipin_num] = read_fifo_out [1];
				assign read_data_out [PIN_WIDTH*3 +ipin_num] = read_fifo_out [0];
			end
			else
			begin
				assign read_data_out [ipin_num] = read_fifo_out [1];
				assign read_data_out [PIN_WIDTH +ipin_num] = read_fifo_out [0];
			end
		end
		else if (NATURAL_ALIGNMENT == "true")
		begin
			assign read_data_out [ipin_num*rate_mult_out] = read_fifo_out [0];
			assign read_data_out [ipin_num*rate_mult_out + 1] = read_fifo_out [1];
			if (USE_HALF_RATE_OUTPUT == "true")
			begin
				assign read_data_out [ipin_num*rate_mult_out + 2] = read_fifo_out [2];
				assign read_data_out [ipin_num*rate_mult_out + 3] = read_fifo_out [3];
			end
		end
		else
		begin
			assign read_data_out [ipin_num] = read_fifo_out [0];
			assign read_data_out [PIN_WIDTH +ipin_num] = read_fifo_out [1];
			if (USE_HALF_RATE_OUTPUT == "true")
			begin
				assign read_data_out [PIN_WIDTH*2 +ipin_num] = read_fifo_out [2];
				assign read_data_out [PIN_WIDTH*3 +ipin_num] = read_fifo_out [3];
			end
		end
	end
end
endgenerate

generate
	genvar pin_num;
	for (pin_num = 0; pin_num < PIN_WIDTH; pin_num = pin_num + 1)
	begin :pad_gen
		if (PIN_TYPE == "bidir")
		begin
			assign write_data_out [pin_num] = 1'b0;
		end
		else
		begin
			assign read_write_data_io [pin_num] = 1'b0;
		end
	
	
		wire delayed_data_in;
		wire delayed_data_out;
		wire delayed_oe;
		wire [4:0] dq_outputdelaysetting;
		wire [4:0] dq_outputenabledelaysetting;
		wire [4:0] dq_inputdelaysetting;
		
		wire [DELAY_CHAIN_WIDTH-1:0] dq_outputdelaysetting_dlc;
		wire [DELAY_CHAIN_WIDTH-1:0] dq_outputenabledelaysetting_dlc;
		wire [DELAY_CHAIN_WIDTH-1:0] dq_inputdelaysetting_dlc;
		
		
		if (USE_DYNAMIC_CONFIG == "true")
		begin
`ifndef FAMILY_HAS_NO_DYNCONF		
		cyclonev_io_config config_1 (
		    .datain(config_data_in),          
		    .clk(config_clock_in),
		    .ena(config_io_ena[pin_num]),
		    .update(config_update),       

		    .outputregdelaysetting(dq_outputdelaysetting), 
		    .outputenabledelaysetting(dq_outputenabledelaysetting),
		    .outputhalfratebypass(dq_outputhalfratebypass[pin_num]),
		    .readfiforeadclockselect(rfifo_clock_select[(pin_num+1)*2-1:pin_num*2]),
		    .readfifomode(rfifo_mode[(pin_num+1)*3-1:pin_num*3]),
		    
		    .padtoinputregisterdelaysetting(dq_inputdelaysetting),
		    .dataout()
		);
`endif		
		assign dq_outputdelaysetting_dlc = dq_outputdelaysetting;
		assign dq_outputenabledelaysetting_dlc = dq_outputenabledelaysetting;
		assign dq_inputdelaysetting_dlc = dq_inputdelaysetting;
		end
	
		if (PIN_TYPE == "input" || PIN_TYPE == "bidir")
		begin
			wire raw_input;
			if (USE_DYNAMIC_CONFIG == "true")
			begin
`ifndef FAMILY_HAS_NO_DYNCONF			
				cyclonev_delay_chain in_delay_1(
					.datain             (raw_input),
					.delayctrlin        (dq_inputdelaysetting_dlc),
					.dataout            (ddr_data[pin_num])
				);
`endif				
			end
			else
			begin
				assign ddr_data[pin_num] = raw_input;
			end	
			
			if (PIN_TYPE == "bidir")
			begin
				cyclonev_io_ibuf data_in (
					.i(read_write_data_io[pin_num]),
					.o(raw_input),
					.ibar(),
					.dynamicterminationcontrol(1'b0)
				);
			end
			else
			begin
				cyclonev_io_ibuf data_in (
					.i(read_data_in[pin_num]),
					.o(raw_input),
					.ibar(),
					.dynamicterminationcontrol(1'b0)
				);
			end
		end
		
		if (PIN_TYPE == "output" || PIN_TYPE == "bidir")
		begin
		
			wire predelayed_data;
			wire predelayed_oe;
	
			if (USE_2X_FF == "true")
			begin
				reg dd_data;
				reg dd_oe;
				always @(posedge dq_dr_clock)
				begin
					dd_data <= aligned_data[pin_num];
					dd_oe <= aligned_oe[pin_num];
				end
				assign predelayed_data = dd_data;
				assign predelayed_oe = dd_oe;
			end
			else
			begin
				assign predelayed_data = aligned_data[pin_num];
				assign predelayed_oe = aligned_oe[pin_num];
			end

			if (USE_DYNAMIC_CONFIG == "true")
			begin
`ifndef FAMILY_HAS_NO_DYNCONF	
				wire delayed_data_1;
				wire delayed_oe_1;
						
				cyclonev_delay_chain
				#(.sim_intrinsic_rising_delay(0), .sim_intrinsic_falling_delay(0))	
				out_delay_1(
					.datain             (predelayed_data),
					.delayctrlin        (dq_outputdelaysetting_dlc),
					.dataout            (delayed_data_1)
				);

				assign delayed_data_out = delayed_data_1;

				cyclonev_delay_chain
				#(.sim_intrinsic_rising_delay(0), .sim_intrinsic_falling_delay(0))
				oe_delay_1(
					.datain             (predelayed_oe),
					.delayctrlin        (dq_outputenabledelaysetting_dlc),
					.dataout            (delayed_oe_1)
				);

				assign delayed_oe = delayed_oe_1;

`endif	
			end
			else
			begin
				assign delayed_data_out = predelayed_data;
				assign delayed_oe = predelayed_oe;	
			end
		
			if (PIN_TYPE == "output")
			begin
				cyclonev_io_obuf data_out (
					.i (delayed_data_out),
					.o (write_data_out [pin_num]),
					.oe (~delayed_oe),
					.parallelterminationcontrol	(parallelterminationcontrol_in),		
					.seriesterminationcontrol	(seriesterminationcontrol_in),
          .obar(),
          .devoe(),
          .dynamicterminationcontrol(1'b0)																	 
				);
			end
			else if (PIN_TYPE == "bidir")
			begin
				cyclonev_io_obuf
			#(
			  	.sim_dynamic_termination_control_is_connected("true")
			  ) data_out (
					.oe (~delayed_oe),
					.i (delayed_data_out),
					.o (read_write_data_io [pin_num]),
					.parallelterminationcontrol	(parallelterminationcontrol_in),
					.dynamicterminationcontrol	(delayed_oct),
					.seriesterminationcontrol	(seriesterminationcontrol_in),
          .obar(),
          .devoe()				
				);
				
				/* synthesis translate_off */
				
				assert property (@(posedge fr_clock_in or negedge fr_clock_in) (~delayed_oe === 1'b1) |-> delayed_oct === 1'b0) 
					else $display(1, "OE enabled but dynamic OCT ctrl is not in write mode");

`ifndef BOARD_DELAY_MODEL
				assert property (@(posedge capture_strobe or negedge capture_strobe) (~delayed_oe === 1'b0 && read_write_data_io[pin_num] !== 1'bz) |-> delayed_oct === 1'b1) 
				else $display(1, "Read data comes back but dynamic OCT ctrl is not in read mode");
`endif

				/* synthesis translate_on */					
			end
		end
	end
endgenerate

generate
	genvar epin_num;
	for (epin_num = 0; epin_num < EXTRA_OUTPUT_WIDTH; epin_num = epin_num + 1)
	begin :extra_output_pad_gen
		wire fr_data_hi;
		wire fr_data_lo;
		wire aligned_data;
		wire extra_outputhalfratebypass;

		if (USE_HALF_RATE_OUTPUT == "true")
		begin
			wire hr_data_t0;
			wire hr_data_t1;
			wire hr_data_t2;
			wire hr_data_t3;
			
			if (NATURAL_ALIGNMENT == "true")
			begin
				assign hr_data_t0 = extra_write_data_in [epin_num * rate_mult_out];
				assign hr_data_t1 = extra_write_data_in [epin_num * rate_mult_out + 1];
				assign hr_data_t2 = extra_write_data_in [epin_num * rate_mult_out + 2];
				assign hr_data_t3 = extra_write_data_in [epin_num * rate_mult_out + 3];
			end
			else if (REGULAR_WRITE_BUS_ORDERING == "true")
		  	begin
				assign hr_data_t0 = extra_write_data_in [epin_num + 0*EXTRA_OUTPUT_WIDTH];
				assign hr_data_t1 = extra_write_data_in [epin_num + 1*EXTRA_OUTPUT_WIDTH];
				assign hr_data_t2 = extra_write_data_in [epin_num + 2*EXTRA_OUTPUT_WIDTH];
				assign hr_data_t3 = extra_write_data_in [epin_num + 3*EXTRA_OUTPUT_WIDTH];
			end
			else
		  	begin
				assign hr_data_t0 = extra_write_data_in [epin_num + 2*EXTRA_OUTPUT_WIDTH];
				assign hr_data_t1 = extra_write_data_in [epin_num + 0*EXTRA_OUTPUT_WIDTH];
				assign hr_data_t2 = extra_write_data_in [epin_num + 3*EXTRA_OUTPUT_WIDTH];
				assign hr_data_t3 = extra_write_data_in [epin_num + 1*EXTRA_OUTPUT_WIDTH];
			end
		
			cyclonev_ddio_out
			#(
				.half_rate_mode("true"),
				.use_new_clocking_model("true"),
				.async_mode("none")
			) hr_to_fr_hi (							    
				.datainhi(hr_data_t0),
				.datainlo(hr_data_t2),
				.dataout(fr_data_hi),
				.clkhi (hr_seq_clock),
				.clklo (hr_seq_clock),
				.hrbypass(extra_outputhalfratebypass),
				.muxsel (hr_seq_clock),
				.clk(),
        .ena(1'b1),
        .areset(),
        .sreset(),
        .dfflo(),
        .dffhi(),
        .devpor(),
        .devclrn()																		
			);
		
			cyclonev_ddio_out
			#(
				.half_rate_mode("true"),
				.use_new_clocking_model("true"),
				.async_mode("none")
			) hr_to_fr_lo (							    
				.datainhi(hr_data_t1),
				.datainlo(hr_data_t3),
				.dataout(fr_data_lo),
				.clkhi (hr_seq_clock),
				.clklo (hr_seq_clock),
				.hrbypass(extra_outputhalfratebypass),
				.muxsel (hr_seq_clock),
  			.clk(),
        .ena(1'b1),
        .areset(),
        .sreset(),
        .dfflo(),
        .dffhi(),
        .devpor(),
        .devclrn()																		
			);
		end
		else
		begin
			if (NATURAL_ALIGNMENT == "true")
			begin
				assign fr_data_lo = extra_write_data_in [epin_num * rate_mult_out + 1];
				assign fr_data_hi = extra_write_data_in [epin_num * rate_mult_out];
			end
			else
			begin
				assign fr_data_lo = extra_write_data_in [epin_num+EXTRA_OUTPUT_WIDTH];
				assign fr_data_hi = extra_write_data_in [epin_num];
			end
		end
		
		if (USE_OUTPUT_PHASE_ALIGNMENT == "true")
		begin
		end
		else
		begin

			cyclonev_ddio_out
			#(
				.async_mode("none"),
				.half_rate_mode("false"),
				.sync_mode("none"),
				.use_new_clocking_model("true")
			) ddio_out (
				.datainhi(fr_data_hi),
				.datainlo(fr_data_lo),
				.dataout(aligned_data),
				.clkhi (dq_shifted_clock),
				.clklo (dq_shifted_clock),
				.muxsel (dq_shifted_clock),
  			.clk(),
        .ena(1'b1),
        .areset(),
        .sreset(),
        .dfflo(),
        .dffhi(),
        .devpor(),
        .devclrn(),
				.hrbypass()
			);
		end
		
		wire delayed_data_out;
		wire [4:0] dq_outputdelaysetting1;
		wire [4:0] dq_inputdelaysetting;
		wire [DELAY_CHAIN_WIDTH-1:0] dq_outputdelaysetting1_dlc;
		wire [DELAY_CHAIN_WIDTH-1:0] dq_inputdelaysetting_dlc;
	
		if (USE_DYNAMIC_CONFIG == "true")
		begin	
`ifndef FAMILY_HAS_NO_DYNCONF		
			cyclonev_io_config config_1 (
				.datain(config_data_in),          
				.clk(config_clock_in),
				.ena(config_extra_io_ena[epin_num]),
				.update(config_update),       

				.outputregdelaysetting(dq_outputdelaysetting1),
				.outputhalfratebypass(extra_outputhalfratebypass),
				.padtoinputregisterdelaysetting(dq_inputdelaysetting),
				.dataout(),
				.readfiforeadclockselect(),
        .readfifomode(),
        .outputenabledelaysetting()

			);
		
			assign dq_outputdelaysetting1_dlc = dq_outputdelaysetting1;
			assign dq_inputdelaysetting_dlc = dq_inputdelaysetting;

			wire delayed_data_1;
			
			cyclonev_delay_chain
			#(.sim_intrinsic_rising_delay(0), .sim_intrinsic_falling_delay(0))
			out_delay_1(						      
				.datain             (aligned_data),
				.delayctrlin        (dq_outputdelaysetting1_dlc),
				.dataout            (delayed_data_1)
			);
			assign delayed_data_out = delayed_data_1;
`endif
		end
		else
		begin
			assign delayed_data_out = aligned_data;
		end
		cyclonev_io_obuf obuf_1 (
			.i (delayed_data_out),
			.o (extra_write_data_out[epin_num]),
			.parallelterminationcontrol(parallelterminationcontrol_in),
			.dynamicterminationcontrol(1'b0),
			.seriesterminationcontrol(seriesterminationcontrol_in),
			.oe (1'b1),
			.obar(),
			.devoe()
		);
	end
endgenerate
endmodule
