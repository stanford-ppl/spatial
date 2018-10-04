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


module twentynm_io_12_lane_abphy (
	input  [1:0] phy_clk,
	input  [7:0] phy_clk_phs,
	input        reset_n,
	input        pll_locked,
	input        dll_ref_clk,
	output [5:0] ioereg_locked,

	input  [47:0] oe_from_core,
	input  [95:0] data_from_core,
	output [95:0] data_to_core,
	input  [15:0] mrnk_read_core,
	input  [15:0] mrnk_write_core,
	input   [3:0] rdata_en_full_core,
	output  [3:0] rdata_valid_core,

	input         core2dbc_rd_data_rdy,
	input         core2dbc_wr_data_vld0,
	input         core2dbc_wr_data_vld1,
	input  [12:0] core2dbc_wr_ecc_info,
	output        dbc2core_rd_data_vld0,
	output        dbc2core_rd_data_vld1,
	output	      dbc2core_rd_type,
	output [11:0] dbc2core_wb_pointer,
	output	      dbc2core_wr_data_rdy,

	input  [95:0] ac_hmc,
	output  [5:0] afi_rlat_core,
	output  [5:0] afi_wlat_core,
	input  [16:0] cfg_dbc,
	input  [50:0] ctl2dbc0,
	input  [50:0] ctl2dbc1,
	output [22:0] dbc2ctl,

	input  [54:0] cal_avl_in,
	output [31:0] cal_avl_readdata_out,
	output [54:0] cal_avl_out,
	input  [31:0] cal_avl_readdata_in,

	input [1:0] dqs_in,
	input	    broadcast_in_bot,
	input	    broadcast_in_top,
	output	    broadcast_out_bot,
	output	    broadcast_out_top,

	input  [11:0] data_in,
	output [11:0] data_out,
	output [11:0] data_oe,
	output [11:0] oct_enable,

	input   [2:0] core_dll,
	output [12:0] dll_core,

	input	sync_clk_bot_in,
	output	sync_clk_bot_out,
	input	sync_clk_top_in,
	output	sync_clk_top_out,
	input	sync_data_bot_in,
	output	sync_data_bot_out,
	input	sync_data_top_in,
	output	sync_data_top_out,

	output [1:0]	dft_phy_clk
);

  timeunit 1ps;
  timeprecision 1ps;


parameter phy_clk_phs_freq = 1000;
parameter mode_rate_in     = "in_rate_1_4";
parameter mode_rate_out    = "out_rate_full";
parameter [8-1:0] pipe_latency     = 8'd0; 
parameter [7-1:0] rd_valid_delay   = 7'd0; 
parameter [6-1:0] dqs_enable_delay = 6'd0; 
parameter phy_clk_sel      = 0;
parameter dqs_lgc_dqs_b_en         = "false";

parameter pin_0_initial_out   = "initial_out_z";
parameter pin_0_mode_ddr      = "mode_ddr";
parameter [12-1:0] pin_0_output_phase  = 12'd0; 
parameter pin_0_oct_mode      = "static_off";
parameter pin_0_data_in_mode  = "disabled";
parameter pin_1_initial_out   = "initial_out_z";
parameter pin_1_mode_ddr      = "mode_ddr";
parameter [12-1:0] pin_1_output_phase  = 12'd0; 
parameter pin_1_oct_mode      = "static_off";
parameter pin_1_data_in_mode  = "disabled";
parameter pin_2_initial_out   = "initial_out_z";
parameter pin_2_mode_ddr      = "mode_ddr";
parameter [12-1:0] pin_2_output_phase  = 12'd0; 
parameter pin_2_oct_mode      = "static_off";
parameter pin_2_data_in_mode  = "disabled";
parameter pin_3_initial_out   = "initial_out_z";
parameter pin_3_mode_ddr      = "mode_ddr";
parameter [12-1:0] pin_3_output_phase  = 12'd0; 
parameter pin_3_oct_mode      = "static_off";
parameter pin_3_data_in_mode  = "disabled";
parameter pin_4_initial_out   = "initial_out_z";
parameter pin_4_mode_ddr      = "mode_ddr";
parameter [12-1:0] pin_4_output_phase  = 12'd0; 
parameter pin_4_oct_mode      = "static_off";
parameter pin_4_data_in_mode  = "disabled";
parameter pin_5_initial_out   = "initial_out_z";
parameter pin_5_mode_ddr      = "mode_ddr";
parameter [12-1:0] pin_5_output_phase  = 12'd0; 
parameter pin_5_oct_mode      = "static_off";
parameter pin_5_data_in_mode  = "disabled";
parameter pin_6_initial_out   = "initial_out_z";
parameter pin_6_mode_ddr      = "mode_ddr";
parameter [12-1:0] pin_6_output_phase  = 12'd0; 
parameter pin_6_oct_mode      = "static_off";
parameter pin_6_data_in_mode  = "disabled";
parameter pin_7_initial_out   = "initial_out_z";
parameter pin_7_mode_ddr      = "mode_ddr";
parameter [12-1:0] pin_7_output_phase  = 12'd0; 
parameter pin_7_oct_mode      = "static_off";
parameter pin_7_data_in_mode  = "disabled";
parameter pin_8_initial_out   = "initial_out_z";
parameter pin_8_mode_ddr      = "mode_ddr";
parameter [12-1:0] pin_8_output_phase  = 12'd0; 
parameter pin_8_oct_mode      = "static_off";
parameter pin_8_data_in_mode  = "disabled";
parameter pin_9_initial_out   = "initial_out_z";
parameter pin_9_mode_ddr      = "mode_ddr";
parameter [12-1:0] pin_9_output_phase  = 12'd0; 
parameter pin_9_oct_mode      = "static_off";
parameter pin_9_data_in_mode  = "disabled";
parameter pin_10_initial_out   = "initial_out_z";
parameter pin_10_mode_ddr      = "mode_ddr";
parameter [12-1:0] pin_10_output_phase  = 12'd0; 
parameter pin_10_oct_mode      = "static_off";
parameter pin_10_data_in_mode  = "disabled";
parameter pin_11_initial_out   = "initial_out_z";
parameter pin_11_mode_ddr      = "mode_ddr";
parameter [12-1:0] pin_11_output_phase  = 12'd0; 
parameter pin_11_oct_mode      = "static_off";
parameter pin_11_data_in_mode  = "disabled";

parameter [9-1:0] avl_base_addr = 9'h1FF; 
parameter avl_ena       = "false";

parameter db_hmc_or_core                = "core";
parameter db_dbi_sel                    = 0;
parameter db_dbi_wr_en                  = "false";
parameter db_dbi_rd_en                  = "false";
parameter db_crc_dq0                    = 0;
parameter db_crc_dq1                    = 0;
parameter db_crc_dq2                    = 0;
parameter db_crc_dq3                    = 0;
parameter db_crc_dq4                    = 0;
parameter db_crc_dq5                    = 0;
parameter db_crc_dq6                    = 0;
parameter db_crc_dq7                    = 0;
parameter db_crc_dq8                    = 0;
parameter db_crc_x4_or_x8_or_x9         = "x8_mode";
parameter db_crc_en                     = "crc_disable";
parameter db_rwlat_mode                 = "csr_vlu";
parameter db_afi_wlat_vlu               = 0; 
parameter db_afi_rlat_vlu               = 0; 
parameter db_ptr_pipeline_depth         = 0;
parameter db_preamble_mode              = "preamble_one_cycle";
parameter db_reset_auto_release         = "auto_release";
parameter db_data_alignment_mode        = "align_disable";
parameter db_db2core_registered         = "false";
parameter db_core_or_hmc2db_registered  = "false";
parameter dbc_core_clk_sel              = 0;
parameter db_seq_rd_en_full_pipeline    = 0;
parameter [6:0] dbc_wb_reserved_entry   = 7'h04;

parameter db_pin_0_ac_hmc_data_override_ena = "false";
parameter db_pin_0_in_bypass                = "true";
parameter db_pin_0_mode                     = "dq_mode";
parameter db_pin_0_oe_bypass                = "true";
parameter db_pin_0_oe_invert                = "false";
parameter db_pin_0_out_bypass               = "true";
parameter db_pin_0_wr_invert                = "false";
parameter db_pin_1_ac_hmc_data_override_ena = "false";
parameter db_pin_1_in_bypass                = "true";
parameter db_pin_1_mode                     = "dq_mode";
parameter db_pin_1_oe_bypass                = "true";
parameter db_pin_1_oe_invert                = "false";
parameter db_pin_1_out_bypass               = "true";
parameter db_pin_1_wr_invert                = "false";
parameter db_pin_2_ac_hmc_data_override_ena = "false";
parameter db_pin_2_in_bypass                = "true";
parameter db_pin_2_mode                     = "dq_mode";
parameter db_pin_2_oe_bypass                = "true";
parameter db_pin_2_oe_invert                = "false";
parameter db_pin_2_out_bypass               = "true";
parameter db_pin_2_wr_invert                = "false";
parameter db_pin_3_ac_hmc_data_override_ena = "false";
parameter db_pin_3_in_bypass                = "true";
parameter db_pin_3_mode                     = "dq_mode";
parameter db_pin_3_oe_bypass                = "true";
parameter db_pin_3_oe_invert                = "false";
parameter db_pin_3_out_bypass               = "true";
parameter db_pin_3_wr_invert                = "false";
parameter db_pin_4_ac_hmc_data_override_ena = "false";
parameter db_pin_4_in_bypass                = "true";
parameter db_pin_4_mode                     = "dq_mode";
parameter db_pin_4_oe_bypass                = "true";
parameter db_pin_4_oe_invert                = "false";
parameter db_pin_4_out_bypass               = "true";
parameter db_pin_4_wr_invert                = "false";
parameter db_pin_5_ac_hmc_data_override_ena = "false";
parameter db_pin_5_in_bypass                = "true";
parameter db_pin_5_mode                     = "dq_mode";
parameter db_pin_5_oe_bypass                = "true";
parameter db_pin_5_oe_invert                = "false";
parameter db_pin_5_out_bypass               = "true";
parameter db_pin_5_wr_invert                = "false";
parameter db_pin_6_ac_hmc_data_override_ena = "false";
parameter db_pin_6_in_bypass                = "true";
parameter db_pin_6_mode                     = "dq_mode";
parameter db_pin_6_oe_bypass                = "true";
parameter db_pin_6_oe_invert                = "false";
parameter db_pin_6_out_bypass               = "true";
parameter db_pin_6_wr_invert                = "false";
parameter db_pin_7_ac_hmc_data_override_ena = "false";
parameter db_pin_7_in_bypass                = "true";
parameter db_pin_7_mode                     = "dq_mode";
parameter db_pin_7_oe_bypass                = "true";
parameter db_pin_7_oe_invert                = "false";
parameter db_pin_7_out_bypass               = "true";
parameter db_pin_7_wr_invert                = "false";
parameter db_pin_8_ac_hmc_data_override_ena = "false";
parameter db_pin_8_in_bypass                = "true";
parameter db_pin_8_mode                     = "dq_mode";
parameter db_pin_8_oe_bypass                = "true";
parameter db_pin_8_oe_invert                = "false";
parameter db_pin_8_out_bypass               = "true";
parameter db_pin_8_wr_invert                = "false";
parameter db_pin_9_ac_hmc_data_override_ena = "false";
parameter db_pin_9_in_bypass                = "true";
parameter db_pin_9_mode                     = "dq_mode";
parameter db_pin_9_oe_bypass                = "true";
parameter db_pin_9_oe_invert                = "false";
parameter db_pin_9_out_bypass               = "true";
parameter db_pin_9_wr_invert                = "false";
parameter db_pin_10_ac_hmc_data_override_ena = "false";
parameter db_pin_10_in_bypass                = "true";
parameter db_pin_10_mode                     = "dq_mode";
parameter db_pin_10_oe_bypass                = "true";
parameter db_pin_10_oe_invert                = "false";
parameter db_pin_10_out_bypass               = "true";
parameter db_pin_10_wr_invert                = "false";
parameter db_pin_11_ac_hmc_data_override_ena = "false";
parameter db_pin_11_in_bypass                = "true";
parameter db_pin_11_mode                     = "dq_mode";
parameter db_pin_11_oe_bypass                = "true";
parameter db_pin_11_oe_invert                = "false";
parameter db_pin_11_out_bypass               = "true";
parameter db_pin_11_wr_invert                = "false";

parameter dll_rst_en      = "dll_rst_dis";
parameter dll_en          = "dll_dis";
parameter dll_core_updnen = "core_updn_dis";
parameter dll_ctlsel      = "ctl_dynamic";
parameter [10-1:0] dll_ctl_static = 10'd0; 

parameter dqs_lgc_swap_dqs_a_b      = "false";
parameter dqs_lgc_dqs_a_interp_en   = "false";
parameter dqs_lgc_dqs_b_interp_en   = "false";
parameter [10-1:0] dqs_lgc_pvt_input_delay_a = 10'd0; 
parameter [10-1:0] dqs_lgc_pvt_input_delay_b = 10'd0; 
parameter dqs_lgc_enable_toggler    = "preamble_track_dqs_enable";
parameter [12-1:0] dqs_lgc_phase_shift_b     = 12'd0; 
parameter [12-1:0] dqs_lgc_phase_shift_a     = 12'd0; 
parameter dqs_lgc_pack_mode         = "packed";
parameter dqs_lgc_pst_preamble_mode = "ddr3_preamble";
parameter dqs_lgc_pst_en_shrink     = "shrink_1_0";
parameter dqs_lgc_broadcast_enable  = "disable_broadcast";
parameter dqs_lgc_burst_length      = "burst_length_2";
parameter dqs_lgc_ddr4_search       = "ddr3_search";
parameter [7-1:0] dqs_lgc_count_threshold   = 7'd0; 

parameter hps_ctrl_en = "false";
parameter silicon_rev = "20nm5es";
parameter pingpong_primary = "false";
parameter pingpong_secondary = "false";

parameter pin_0_dqs_x4_mode  = (db_crc_x4_or_x8_or_x9 == "x4_mode") ? "dqs_x4_a" : "dqs_x4_not_used";
parameter pin_1_dqs_x4_mode  = (db_crc_x4_or_x8_or_x9 == "x4_mode") ? "dqs_x4_a" : "dqs_x4_not_used";
parameter pin_2_dqs_x4_mode  = (db_crc_x4_or_x8_or_x9 == "x4_mode") ? "dqs_x4_a" : "dqs_x4_not_used";
parameter pin_3_dqs_x4_mode  = (db_crc_x4_or_x8_or_x9 == "x4_mode") ? "dqs_x4_a" : "dqs_x4_not_used";
parameter pin_4_dqs_x4_mode  = (db_crc_x4_or_x8_or_x9 == "x4_mode") ? "dqs_x4_a" : "dqs_x4_not_used";
parameter pin_5_dqs_x4_mode  = (db_crc_x4_or_x8_or_x9 == "x4_mode") ? "dqs_x4_a" : "dqs_x4_not_used";
parameter pin_6_dqs_x4_mode  = (db_crc_x4_or_x8_or_x9 == "x4_mode") ? "dqs_x4_b" : "dqs_x4_not_used";
parameter pin_7_dqs_x4_mode  = (db_crc_x4_or_x8_or_x9 == "x4_mode") ? "dqs_x4_b" : "dqs_x4_not_used";
parameter pin_8_dqs_x4_mode  = (db_crc_x4_or_x8_or_x9 == "x4_mode") ? "dqs_x4_a" : "dqs_x4_not_used";
parameter pin_9_dqs_x4_mode  = (db_crc_x4_or_x8_or_x9 == "x4_mode") ? "dqs_x4_a" : "dqs_x4_not_used";
parameter pin_10_dqs_x4_mode = (db_crc_x4_or_x8_or_x9 == "x4_mode") ? "dqs_x4_b" : "dqs_x4_not_used";
parameter pin_11_dqs_x4_mode = (db_crc_x4_or_x8_or_x9 == "x4_mode") ? "dqs_x4_b" : "dqs_x4_not_used";

parameter fast_interpolator_sim = 0;

wire debug_dqs_gated_a;
wire debug_dqs_enable_a;
wire debug_dqs_gated_b;
wire debug_dqs_enable_b;
wire [11:0] debug_dq_delayed;

twentynm_io_12_lane_encrypted_abphy inst (
	.phy_clk       (phy_clk       ),
	.phy_clk_phs   (phy_clk_phs   ),
	.reset_n       (reset_n       ),
	.pll_locked    (pll_locked    ),
	.dll_ref_clk   (dll_ref_clk   ),
	.ioereg_locked (ioereg_locked ),

	.oe_from_core       (oe_from_core       ),
	.data_from_core     (data_from_core     ),
	.data_to_core       (data_to_core       ),
	.mrnk_read_core     (mrnk_read_core     ),
	.mrnk_write_core    (mrnk_write_core    ),
	.rdata_en_full_core (rdata_en_full_core ),
	.rdata_valid_core   (rdata_valid_core   ),

	.core2dbc_rd_data_rdy  (core2dbc_rd_data_rdy  ),
	.core2dbc_wr_data_vld0 (core2dbc_wr_data_vld0 ),
	.core2dbc_wr_data_vld1 (core2dbc_wr_data_vld1 ),
	.core2dbc_wr_ecc_info  (core2dbc_wr_ecc_info  ),
	.dbc2core_rd_data_vld0 (dbc2core_rd_data_vld0 ),
	.dbc2core_rd_data_vld1 (dbc2core_rd_data_vld1 ),
	.dbc2core_rd_type      (dbc2core_rd_type      ),
	.dbc2core_wb_pointer   (dbc2core_wb_pointer   ),
	.dbc2core_wr_data_rdy  (dbc2core_wr_data_rdy  ),

	.ac_hmc        (ac_hmc        ),
	.afi_rlat_core (afi_rlat_core ),
	.afi_wlat_core (afi_wlat_core ),
	.cfg_dbc       (cfg_dbc       ),
	.ctl2dbc0      (ctl2dbc0      ),
	.ctl2dbc1      (ctl2dbc1      ),
	.dbc2ctl       (dbc2ctl       ),

	.cal_avl_in           (cal_avl_in           ),
	.cal_avl_readdata_out (cal_avl_readdata_out ),
	.cal_avl_out          (cal_avl_out          ),
	.cal_avl_readdata_in  (cal_avl_readdata_in  ),

	.dqs_in            (dqs_in            ),
	.broadcast_in_bot  (broadcast_in_bot  ),
	.broadcast_in_top  (broadcast_in_top  ),
	.broadcast_out_bot (broadcast_out_bot ),
	.broadcast_out_top (broadcast_out_top ),

	.data_in    (data_in    ),
	.data_out   (data_out   ),
	.data_oe    (data_oe    ),
	.oct_enable (oct_enable ),

	.core_dll (core_dll ),
	.dll_core (dll_core ),

	.sync_clk_bot_in   (sync_clk_bot_in  ),
	.sync_clk_bot_out  (sync_clk_bot_out ),
	.sync_clk_top_in   (sync_clk_top_in  ),
	.sync_clk_top_out  (sync_clk_top_out ),
	.sync_data_bot_in  (sync_data_bot_in ),
	.sync_data_bot_out (sync_data_bot_out),
	.sync_data_top_in  (sync_data_top_in ),
	.sync_data_top_out (sync_data_top_out),

	.debug_dqs_gated_a  (debug_dqs_gated_a  ),
	.debug_dqs_enable_a (debug_dqs_enable_a ),
	.debug_dqs_gated_b  (debug_dqs_gated_b  ),
	.debug_dqs_enable_b (debug_dqs_enable_b ),
	.debug_dq_delayed   (debug_dq_delayed   ),

	.dft_phy_clk (dft_phy_clk)
);

defparam inst.phy_clk_phs_freq                       = phy_clk_phs_freq                     ;
defparam inst.mode_rate_in                           = mode_rate_in                         ;
defparam inst.mode_rate_out                          = mode_rate_out                        ;
defparam inst.pipe_latency                           = pipe_latency                         ; 
defparam inst.rd_valid_delay                         = rd_valid_delay                       ; 
defparam inst.dqs_enable_delay                       = dqs_enable_delay                     ; 
defparam inst.phy_clk_sel                            = phy_clk_sel                          ;
defparam inst.dqs_lgc_dqs_b_en                       = dqs_lgc_dqs_b_en                     ;
defparam inst.pin_0_initial_out                      = pin_0_initial_out                    ;
defparam inst.pin_0_mode_ddr                         = pin_0_mode_ddr                       ;
defparam inst.pin_0_output_phase                     = pin_0_output_phase                   ; 
defparam inst.pin_0_oct_mode                         = pin_0_oct_mode                       ;
defparam inst.pin_0_data_in_mode                     = pin_0_data_in_mode                   ;
defparam inst.pin_1_initial_out                      = pin_1_initial_out                    ;
defparam inst.pin_1_mode_ddr                         = pin_1_mode_ddr                       ;
defparam inst.pin_1_output_phase                     = pin_1_output_phase                   ; 
defparam inst.pin_1_oct_mode                         = pin_1_oct_mode                       ;
defparam inst.pin_1_data_in_mode                     = pin_1_data_in_mode                   ;
defparam inst.pin_2_initial_out                      = pin_2_initial_out                    ;
defparam inst.pin_2_mode_ddr                         = pin_2_mode_ddr                       ;
defparam inst.pin_2_output_phase                     = pin_2_output_phase                   ; 
defparam inst.pin_2_oct_mode                         = pin_2_oct_mode                       ;
defparam inst.pin_2_data_in_mode                     = pin_2_data_in_mode                   ;
defparam inst.pin_3_initial_out                      = pin_3_initial_out                    ;
defparam inst.pin_3_mode_ddr                         = pin_3_mode_ddr                       ;
defparam inst.pin_3_output_phase                     = pin_3_output_phase                   ; 
defparam inst.pin_3_oct_mode                         = pin_3_oct_mode                       ;
defparam inst.pin_3_data_in_mode                     = pin_3_data_in_mode                   ;
defparam inst.pin_4_initial_out                      = pin_4_initial_out                    ;
defparam inst.pin_4_mode_ddr                         = pin_4_mode_ddr                       ;
defparam inst.pin_4_output_phase                     = pin_4_output_phase                   ; 
defparam inst.pin_4_oct_mode                         = pin_4_oct_mode                       ;
defparam inst.pin_4_data_in_mode                     = pin_4_data_in_mode                   ;
defparam inst.pin_5_initial_out                      = pin_5_initial_out                    ;
defparam inst.pin_5_mode_ddr                         = pin_5_mode_ddr                       ;
defparam inst.pin_5_output_phase                     = pin_5_output_phase                   ; 
defparam inst.pin_5_oct_mode                         = pin_5_oct_mode                       ;
defparam inst.pin_5_data_in_mode                     = pin_5_data_in_mode                   ;
defparam inst.pin_6_initial_out                      = pin_6_initial_out                    ;
defparam inst.pin_6_mode_ddr                         = pin_6_mode_ddr                       ;
defparam inst.pin_6_output_phase                     = pin_6_output_phase                   ; 
defparam inst.pin_6_oct_mode                         = pin_6_oct_mode                       ;
defparam inst.pin_6_data_in_mode                     = pin_6_data_in_mode                   ;
defparam inst.pin_7_initial_out                      = pin_7_initial_out                    ;
defparam inst.pin_7_mode_ddr                         = pin_7_mode_ddr                       ;
defparam inst.pin_7_output_phase                     = pin_7_output_phase                   ; 
defparam inst.pin_7_oct_mode                         = pin_7_oct_mode                       ;
defparam inst.pin_7_data_in_mode                     = pin_7_data_in_mode                   ;
defparam inst.pin_8_initial_out                      = pin_8_initial_out                    ;
defparam inst.pin_8_mode_ddr                         = pin_8_mode_ddr                       ;
defparam inst.pin_8_output_phase                     = pin_8_output_phase                   ; 
defparam inst.pin_8_oct_mode                         = pin_8_oct_mode                       ;
defparam inst.pin_8_data_in_mode                     = pin_8_data_in_mode                   ;
defparam inst.pin_9_initial_out                      = pin_9_initial_out                    ;
defparam inst.pin_9_mode_ddr                         = pin_9_mode_ddr                       ;
defparam inst.pin_9_output_phase                     = pin_9_output_phase                   ; 
defparam inst.pin_9_oct_mode                         = pin_9_oct_mode                       ;
defparam inst.pin_9_data_in_mode                     = pin_9_data_in_mode                   ;
defparam inst.pin_10_initial_out                     = pin_10_initial_out                   ;
defparam inst.pin_10_mode_ddr                        = pin_10_mode_ddr                      ;
defparam inst.pin_10_output_phase                    = pin_10_output_phase                  ; 
defparam inst.pin_10_oct_mode                        = pin_10_oct_mode                      ;
defparam inst.pin_10_data_in_mode                    = pin_10_data_in_mode                  ;
defparam inst.pin_11_initial_out                     = pin_11_initial_out                   ;
defparam inst.pin_11_mode_ddr                        = pin_11_mode_ddr                      ;
defparam inst.pin_11_output_phase                    = pin_11_output_phase                  ; 
defparam inst.pin_11_oct_mode                        = pin_11_oct_mode                      ;
defparam inst.pin_11_data_in_mode                    = pin_11_data_in_mode                  ;
defparam inst.avl_base_addr                          = avl_base_addr                        ; 
defparam inst.avl_ena                                = avl_ena                              ;
defparam inst.db_hmc_or_core                         = db_hmc_or_core                       ;
defparam inst.db_dbi_sel                             = db_dbi_sel                           ;
defparam inst.db_dbi_wr_en                           = db_dbi_wr_en                         ;
defparam inst.db_dbi_rd_en                           = db_dbi_rd_en                         ;
defparam inst.db_crc_dq0                             = db_crc_dq0                           ;
defparam inst.db_crc_dq1                             = db_crc_dq1                           ;
defparam inst.db_crc_dq2                             = db_crc_dq2                           ;
defparam inst.db_crc_dq3                             = db_crc_dq3                           ;
defparam inst.db_crc_dq4                             = db_crc_dq4                           ;
defparam inst.db_crc_dq5                             = db_crc_dq5                           ;
defparam inst.db_crc_dq6                             = db_crc_dq6                           ;
defparam inst.db_crc_dq7                             = db_crc_dq7                           ;
defparam inst.db_crc_dq8                             = db_crc_dq8                           ;
defparam inst.db_crc_x4_or_x8_or_x9                  = db_crc_x4_or_x8_or_x9                ;
defparam inst.db_crc_en                              = db_crc_en                            ;
defparam inst.db_rwlat_mode                          = db_rwlat_mode                        ;
defparam inst.db_afi_wlat_vlu                        = db_afi_wlat_vlu                      ; 
defparam inst.db_afi_rlat_vlu                        = db_afi_rlat_vlu                      ; 
defparam inst.db_ptr_pipeline_depth                  = db_ptr_pipeline_depth                ;
defparam inst.db_preamble_mode                       = db_preamble_mode                     ;
defparam inst.db_reset_auto_release                  = db_reset_auto_release                ;
defparam inst.db_data_alignment_mode                 = db_data_alignment_mode               ;
defparam inst.db_db2core_registered                  = db_db2core_registered                ;
defparam inst.db_core_or_hmc2db_registered           = db_core_or_hmc2db_registered         ;
defparam inst.dbc_core_clk_sel                       = dbc_core_clk_sel                     ;
defparam inst.db_seq_rd_en_full_pipeline             = db_seq_rd_en_full_pipeline           ;
defparam inst.dbc_wb_reserved_entry                  = dbc_wb_reserved_entry                ;
defparam inst.db_pin_0_ac_hmc_data_override_ena      = db_pin_0_ac_hmc_data_override_ena    ;
defparam inst.db_pin_0_in_bypass                     = db_pin_0_in_bypass                   ;
defparam inst.db_pin_0_mode                          = db_pin_0_mode                        ;
defparam inst.db_pin_0_oe_bypass                     = db_pin_0_oe_bypass                   ;
defparam inst.db_pin_0_oe_invert                     = db_pin_0_oe_invert                   ;
defparam inst.db_pin_0_out_bypass                    = db_pin_0_out_bypass                  ;
defparam inst.db_pin_0_wr_invert                     = db_pin_0_wr_invert                   ;
defparam inst.db_pin_1_ac_hmc_data_override_ena      = db_pin_1_ac_hmc_data_override_ena    ;
defparam inst.db_pin_1_in_bypass                     = db_pin_1_in_bypass                   ;
defparam inst.db_pin_1_mode                          = db_pin_1_mode                        ;
defparam inst.db_pin_1_oe_bypass                     = db_pin_1_oe_bypass                   ;
defparam inst.db_pin_1_oe_invert                     = db_pin_1_oe_invert                   ;
defparam inst.db_pin_1_out_bypass                    = db_pin_1_out_bypass                  ;
defparam inst.db_pin_1_wr_invert                     = db_pin_1_wr_invert                   ;
defparam inst.db_pin_2_ac_hmc_data_override_ena      = db_pin_2_ac_hmc_data_override_ena    ;
defparam inst.db_pin_2_in_bypass                     = db_pin_2_in_bypass                   ;
defparam inst.db_pin_2_mode                          = db_pin_2_mode                        ;
defparam inst.db_pin_2_oe_bypass                     = db_pin_2_oe_bypass                   ;
defparam inst.db_pin_2_oe_invert                     = db_pin_2_oe_invert                   ;
defparam inst.db_pin_2_out_bypass                    = db_pin_2_out_bypass                  ;
defparam inst.db_pin_2_wr_invert                     = db_pin_2_wr_invert                   ;
defparam inst.db_pin_3_ac_hmc_data_override_ena      = db_pin_3_ac_hmc_data_override_ena    ;
defparam inst.db_pin_3_in_bypass                     = db_pin_3_in_bypass                   ;
defparam inst.db_pin_3_mode                          = db_pin_3_mode                        ;
defparam inst.db_pin_3_oe_bypass                     = db_pin_3_oe_bypass                   ;
defparam inst.db_pin_3_oe_invert                     = db_pin_3_oe_invert                   ;
defparam inst.db_pin_3_out_bypass                    = db_pin_3_out_bypass                  ;
defparam inst.db_pin_3_wr_invert                     = db_pin_3_wr_invert                   ;
defparam inst.db_pin_4_ac_hmc_data_override_ena      = db_pin_4_ac_hmc_data_override_ena    ;
defparam inst.db_pin_4_in_bypass                     = db_pin_4_in_bypass                   ;
defparam inst.db_pin_4_mode                          = db_pin_4_mode                        ;
defparam inst.db_pin_4_oe_bypass                     = db_pin_4_oe_bypass                   ;
defparam inst.db_pin_4_oe_invert                     = db_pin_4_oe_invert                   ;
defparam inst.db_pin_4_out_bypass                    = db_pin_4_out_bypass                  ;
defparam inst.db_pin_4_wr_invert                     = db_pin_4_wr_invert                   ;
defparam inst.db_pin_5_ac_hmc_data_override_ena      = db_pin_5_ac_hmc_data_override_ena    ;
defparam inst.db_pin_5_in_bypass                     = db_pin_5_in_bypass                   ;
defparam inst.db_pin_5_mode                          = db_pin_5_mode                        ;
defparam inst.db_pin_5_oe_bypass                     = db_pin_5_oe_bypass                   ;
defparam inst.db_pin_5_oe_invert                     = db_pin_5_oe_invert                   ;
defparam inst.db_pin_5_out_bypass                    = db_pin_5_out_bypass                  ;
defparam inst.db_pin_5_wr_invert                     = db_pin_5_wr_invert                   ;
defparam inst.db_pin_6_ac_hmc_data_override_ena      = db_pin_6_ac_hmc_data_override_ena    ;
defparam inst.db_pin_6_in_bypass                     = db_pin_6_in_bypass                   ;
defparam inst.db_pin_6_mode                          = db_pin_6_mode                        ;
defparam inst.db_pin_6_oe_bypass                     = db_pin_6_oe_bypass                   ;
defparam inst.db_pin_6_oe_invert                     = db_pin_6_oe_invert                   ;
defparam inst.db_pin_6_out_bypass                    = db_pin_6_out_bypass                  ;
defparam inst.db_pin_6_wr_invert                     = db_pin_6_wr_invert                   ;
defparam inst.db_pin_7_ac_hmc_data_override_ena      = db_pin_7_ac_hmc_data_override_ena    ;
defparam inst.db_pin_7_in_bypass                     = db_pin_7_in_bypass                   ;
defparam inst.db_pin_7_mode                          = db_pin_7_mode                        ;
defparam inst.db_pin_7_oe_bypass                     = db_pin_7_oe_bypass                   ;
defparam inst.db_pin_7_oe_invert                     = db_pin_7_oe_invert                   ;
defparam inst.db_pin_7_out_bypass                    = db_pin_7_out_bypass                  ;
defparam inst.db_pin_7_wr_invert                     = db_pin_7_wr_invert                   ;
defparam inst.db_pin_8_ac_hmc_data_override_ena      = db_pin_8_ac_hmc_data_override_ena    ;
defparam inst.db_pin_8_in_bypass                     = db_pin_8_in_bypass                   ;
defparam inst.db_pin_8_mode                          = db_pin_8_mode                        ;
defparam inst.db_pin_8_oe_bypass                     = db_pin_8_oe_bypass                   ;
defparam inst.db_pin_8_oe_invert                     = db_pin_8_oe_invert                   ;
defparam inst.db_pin_8_out_bypass                    = db_pin_8_out_bypass                  ;
defparam inst.db_pin_8_wr_invert                     = db_pin_8_wr_invert                   ;
defparam inst.db_pin_9_ac_hmc_data_override_ena      = db_pin_9_ac_hmc_data_override_ena    ;
defparam inst.db_pin_9_in_bypass                     = db_pin_9_in_bypass                   ;
defparam inst.db_pin_9_mode                          = db_pin_9_mode                        ;
defparam inst.db_pin_9_oe_bypass                     = db_pin_9_oe_bypass                   ;
defparam inst.db_pin_9_oe_invert                     = db_pin_9_oe_invert                   ;
defparam inst.db_pin_9_out_bypass                    = db_pin_9_out_bypass                  ;
defparam inst.db_pin_9_wr_invert                     = db_pin_9_wr_invert                   ;
defparam inst.db_pin_10_ac_hmc_data_override_ena     = db_pin_10_ac_hmc_data_override_ena   ;
defparam inst.db_pin_10_in_bypass                    = db_pin_10_in_bypass                  ;
defparam inst.db_pin_10_mode                         = db_pin_10_mode                       ;
defparam inst.db_pin_10_oe_bypass                    = db_pin_10_oe_bypass                  ;
defparam inst.db_pin_10_oe_invert                    = db_pin_10_oe_invert                  ;
defparam inst.db_pin_10_out_bypass                   = db_pin_10_out_bypass                 ;
defparam inst.db_pin_10_wr_invert                    = db_pin_10_wr_invert                  ;
defparam inst.db_pin_11_ac_hmc_data_override_ena     = db_pin_11_ac_hmc_data_override_ena   ;
defparam inst.db_pin_11_in_bypass                    = db_pin_11_in_bypass                  ;
defparam inst.db_pin_11_mode                         = db_pin_11_mode                       ;
defparam inst.db_pin_11_oe_bypass                    = db_pin_11_oe_bypass                  ;
defparam inst.db_pin_11_oe_invert                    = db_pin_11_oe_invert                  ;
defparam inst.db_pin_11_out_bypass                   = db_pin_11_out_bypass                 ;
defparam inst.db_pin_11_wr_invert                    = db_pin_11_wr_invert                  ;
defparam inst.dll_rst_en                             = dll_rst_en                           ;
defparam inst.dll_en                                 = dll_en                               ;
defparam inst.dll_core_updnen                        = dll_core_updnen                      ;
defparam inst.dll_ctlsel                             = dll_ctlsel                           ;
defparam inst.dll_ctl_static                         = dll_ctl_static                       ; 
defparam inst.dqs_lgc_swap_dqs_a_b                   = dqs_lgc_swap_dqs_a_b                 ;
defparam inst.dqs_lgc_dqs_a_interp_en                = dqs_lgc_dqs_a_interp_en              ;
defparam inst.dqs_lgc_dqs_b_interp_en                = dqs_lgc_dqs_b_interp_en              ;
defparam inst.dqs_lgc_pvt_input_delay_a              = dqs_lgc_pvt_input_delay_a            ; 
defparam inst.dqs_lgc_pvt_input_delay_b              = dqs_lgc_pvt_input_delay_b            ; 
defparam inst.dqs_lgc_enable_toggler                 = dqs_lgc_enable_toggler               ;
defparam inst.dqs_lgc_phase_shift_b                  = dqs_lgc_phase_shift_b                ; 
defparam inst.dqs_lgc_phase_shift_a                  = dqs_lgc_phase_shift_a                ; 
defparam inst.dqs_lgc_pack_mode                      = dqs_lgc_pack_mode                    ;
defparam inst.dqs_lgc_pst_preamble_mode              = dqs_lgc_pst_preamble_mode            ;
defparam inst.dqs_lgc_pst_en_shrink                  = dqs_lgc_pst_en_shrink                ;
defparam inst.dqs_lgc_broadcast_enable               = dqs_lgc_broadcast_enable             ;
defparam inst.dqs_lgc_burst_length                   = dqs_lgc_burst_length                 ;
defparam inst.dqs_lgc_ddr4_search                    = dqs_lgc_ddr4_search                  ;
defparam inst.dqs_lgc_count_threshold                = dqs_lgc_count_threshold              ; 
defparam inst.hps_ctrl_en                            = hps_ctrl_en                          ;
defparam inst.silicon_rev                            = silicon_rev                          ;
defparam inst.pingpong_primary                       = pingpong_primary                     ;
defparam inst.pingpong_secondary                     = pingpong_secondary                   ;
defparam inst.pin_0_dqs_x4_mode = pin_0_dqs_x4_mode;
defparam inst.pin_1_dqs_x4_mode = pin_1_dqs_x4_mode;
defparam inst.pin_2_dqs_x4_mode = pin_2_dqs_x4_mode;
defparam inst.pin_3_dqs_x4_mode = pin_3_dqs_x4_mode;
defparam inst.pin_4_dqs_x4_mode = pin_4_dqs_x4_mode;
defparam inst.pin_5_dqs_x4_mode = pin_5_dqs_x4_mode;
defparam inst.pin_6_dqs_x4_mode = pin_6_dqs_x4_mode;
defparam inst.pin_7_dqs_x4_mode = pin_7_dqs_x4_mode;
defparam inst.pin_8_dqs_x4_mode = pin_8_dqs_x4_mode;
defparam inst.pin_9_dqs_x4_mode = pin_9_dqs_x4_mode;
defparam inst.pin_10_dqs_x4_mode = pin_10_dqs_x4_mode;
defparam inst.pin_11_dqs_x4_mode = pin_11_dqs_x4_mode;
defparam inst.fast_interpolator_sim                  = fast_interpolator_sim                ;

endmodule 

