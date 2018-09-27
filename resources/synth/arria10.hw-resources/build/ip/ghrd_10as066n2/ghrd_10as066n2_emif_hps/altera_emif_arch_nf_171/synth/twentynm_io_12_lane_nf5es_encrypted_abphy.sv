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


module twentynm_io_12_lane_nf5es_encrypted_abphy (
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

	output debug_dqs_gated_a, 
	output debug_dqs_enable_a,
	output debug_dqs_gated_b, 
	output debug_dqs_enable_b,
	output [11:0] debug_dq_delayed,

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
parameter [13-1:0] pin_0_output_phase  = 13'd0; 
parameter pin_0_oct_mode      = "static_off";
parameter pin_0_data_in_mode  = "disabled";
parameter pin_1_initial_out   = "initial_out_z";
parameter pin_1_mode_ddr      = "mode_ddr";
parameter [13-1:0] pin_1_output_phase  = 13'd0; 
parameter pin_1_oct_mode      = "static_off";
parameter pin_1_data_in_mode  = "disabled";
parameter pin_2_initial_out   = "initial_out_z";
parameter pin_2_mode_ddr      = "mode_ddr";
parameter [13-1:0] pin_2_output_phase  = 13'd0; 
parameter pin_2_oct_mode      = "static_off";
parameter pin_2_data_in_mode  = "disabled";
parameter pin_3_initial_out   = "initial_out_z";
parameter pin_3_mode_ddr      = "mode_ddr";
parameter [13-1:0] pin_3_output_phase  = 13'd0; 
parameter pin_3_oct_mode      = "static_off";
parameter pin_3_data_in_mode  = "disabled";
parameter pin_4_initial_out   = "initial_out_z";
parameter pin_4_mode_ddr      = "mode_ddr";
parameter [13-1:0] pin_4_output_phase  = 13'd0; 
parameter pin_4_oct_mode      = "static_off";
parameter pin_4_data_in_mode  = "disabled";
parameter pin_5_initial_out   = "initial_out_z";
parameter pin_5_mode_ddr      = "mode_ddr";
parameter [13-1:0] pin_5_output_phase  = 13'd0; 
parameter pin_5_oct_mode      = "static_off";
parameter pin_5_data_in_mode  = "disabled";
parameter pin_6_initial_out   = "initial_out_z";
parameter pin_6_mode_ddr      = "mode_ddr";
parameter [13-1:0] pin_6_output_phase  = 13'd0; 
parameter pin_6_oct_mode      = "static_off";
parameter pin_6_data_in_mode  = "disabled";
parameter pin_7_initial_out   = "initial_out_z";
parameter pin_7_mode_ddr      = "mode_ddr";
parameter [13-1:0] pin_7_output_phase  = 13'd0; 
parameter pin_7_oct_mode      = "static_off";
parameter pin_7_data_in_mode  = "disabled";
parameter pin_8_initial_out   = "initial_out_z";
parameter pin_8_mode_ddr      = "mode_ddr";
parameter [13-1:0] pin_8_output_phase  = 13'd0; 
parameter pin_8_oct_mode      = "static_off";
parameter pin_8_data_in_mode  = "disabled";
parameter pin_9_initial_out   = "initial_out_z";
parameter pin_9_mode_ddr      = "mode_ddr";
parameter [13-1:0] pin_9_output_phase  = 13'd0; 
parameter pin_9_oct_mode      = "static_off";
parameter pin_9_data_in_mode  = "disabled";
parameter pin_10_initial_out   = "initial_out_z";
parameter pin_10_mode_ddr      = "mode_ddr";
parameter [13-1:0] pin_10_output_phase  = 13'd0; 
parameter pin_10_oct_mode      = "static_off";
parameter pin_10_data_in_mode  = "disabled";
parameter pin_11_initial_out   = "initial_out_z";
parameter pin_11_mode_ddr      = "mode_ddr";
parameter [13-1:0] pin_11_output_phase  = 13'd0; 
parameter pin_11_oct_mode      = "static_off";
parameter pin_11_data_in_mode  = "disabled";

parameter [9-1:0] avl_base_addr = 9'h1FF; 
parameter avl_ena       = "false";

parameter db_hmc_or_core         = "core";
parameter db_dbi_sel             = 0;
parameter db_dbi_wr_en           = "false";
parameter db_dbi_rd_en           = "false";
parameter db_crc_dq0             = 0;
parameter db_crc_dq1             = 0;
parameter db_crc_dq2             = 0;
parameter db_crc_dq3             = 0;
parameter db_crc_dq4             = 0;
parameter db_crc_dq5             = 0;
parameter db_crc_dq6             = 0;
parameter db_crc_dq7             = 0;
parameter db_crc_dq8             = 0;
parameter db_crc_x4_or_x8_or_x9  = "x8_mode";
parameter db_crc_en              = "crc_disable";
parameter db_rwlat_mode          = "csr_vlu";
parameter db_afi_wlat_vlu        = 0; 
parameter db_afi_rlat_vlu        = 0; 
parameter db_ptr_pipeline_depth  = 0;
parameter db_preamble_mode       = "preamble_one_cycle";
parameter db_reset_auto_release  = "auto_release";
parameter db_data_alignment_mode = "align_disable";
parameter db_db2core_registered  = "false";
parameter db_core_or_hmc2db_registered  = "false";
parameter dbc_core_clk_sel        = 0;
parameter db_seq_rd_en_full_pipeline = 0;
parameter [6:0] dbc_wb_reserved_entry = 7'h04;

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

parameter pin_0_dqs_x4_mode  = "dqs_x4_not_used";
parameter pin_1_dqs_x4_mode  = "dqs_x4_not_used";
parameter pin_2_dqs_x4_mode  = "dqs_x4_not_used";
parameter pin_3_dqs_x4_mode  = "dqs_x4_not_used";
parameter pin_4_dqs_x4_mode  = "dqs_x4_not_used";
parameter pin_5_dqs_x4_mode  = "dqs_x4_not_used";
parameter pin_6_dqs_x4_mode  = "dqs_x4_not_used";
parameter pin_7_dqs_x4_mode  = "dqs_x4_not_used";
parameter pin_8_dqs_x4_mode  = "dqs_x4_not_used";
parameter pin_9_dqs_x4_mode  = "dqs_x4_not_used";
parameter pin_10_dqs_x4_mode = "dqs_x4_not_used";
parameter pin_11_dqs_x4_mode = "dqs_x4_not_used";

parameter fast_interpolator_sim = 0;

wire  [1:0] ctl2dbc_cs0;
wire        ctl2dbc_mask_entry0;
wire  [3:0] ctl2dbc_misc0;
wire  [7:0] ctl2dbc_mrnk_read0;
wire        ctl2dbc_nop0;
wire [11:0] ctl2dbc_rb_rdptr0;
wire  [1:0] ctl2dbc_rb_rdptr_vld0;
wire  [5:0] ctl2dbc_rb_wrptr0;
wire        ctl2dbc_rb_wrptr_vld0;
wire        ctl2dbc_rd_type0;
wire  [3:0] ctl2dbc_rdata_en_full0;
wire        ctl2dbc_seq_en0;
wire  [5:0] ctl2dbc_wb_rdptr0;
wire        ctl2dbc_wb_rdptr_vld0;
wire        ctl2dbc_wrdata_vld0;

wire  [1:0] ctl2dbc_cs1;
wire        ctl2dbc_mask_entry1;
wire  [3:0] ctl2dbc_misc1;
wire  [7:0] ctl2dbc_mrnk_read1;
wire        ctl2dbc_nop1;
wire [11:0] ctl2dbc_rb_rdptr1;
wire  [1:0] ctl2dbc_rb_rdptr_vld1;
wire  [5:0] ctl2dbc_rb_wrptr1;
wire        ctl2dbc_rb_wrptr_vld1;
wire        ctl2dbc_rd_type1;
wire  [3:0] ctl2dbc_rdata_en_full1;
wire        ctl2dbc_seq_en1;
wire  [5:0] ctl2dbc_wb_rdptr1;
wire        ctl2dbc_wb_rdptr_vld1;
wire        ctl2dbc_wrdata_vld1;

wire       dbc2ctl_all_rd_done;
wire [5:0] dbc2ctl_rb_retire_ptr;
wire       dbc2ctl_rb_retire_ptr_vld;
wire       dbc2ctl_rd_data_vld;
wire [5:0] dbc2ctl_wb_retire_ptr;
wire	   dbc2ctl_wb_retire_ptr_vld;
wire [5:0] dbc2db_wb_wrptr;
wire       dbc2db_wb_wrptr_vld;

wire [2:0] cfg_cmd_rate;
wire       cfg_dbc_ctrl_sel;
wire       cfg_dbc_dualport_en;
wire       cfg_dbc_in_protocol;
wire [2:0] cfg_dbc_pipe_lat;
wire       cfg_dbc_rc_en;
wire [1:0] cfg_dbc_slot_offset;
wire [1:0] cfg_dbc_slot_rotate_en;
wire       cfg_output_regd;
wire       cfg_reorder_rdata;
wire       cfg_rmw_en;

wire [19:0] avl_address_in;
wire [31:0] avl_writedata_in;
wire        avl_write_in;
wire        avl_read_in;
wire        avl_clk_in;
wire [31:0] avl_readdata_out;

wire [19:0] avl_address_out;
wire [31:0] avl_writedata_out;
wire        avl_write_out;
wire        avl_read_out;
wire        avl_clk_out;
wire [31:0] avl_readdata_in;

wire [23:0] dq_in;
wire [11:0] codin_n;
wire [11:0] codin_nb;
wire [11:0] codin_p;
wire [11:0] codin_pb;


assign  dq_in[23:0] = {  data_in[11],
                        ~data_in[11],
                         data_in[10],
                        ~data_in[10],
                         data_in[9],
                        ~data_in[9],
                         data_in[8],
                        ~data_in[8],
                         data_in[7],
                        ~data_in[7],
                         data_in[6],
                        ~data_in[6],
                         data_in[5],
                        ~data_in[5],
                         data_in[4],
                        ~data_in[4],
                         data_in[3],
                        ~data_in[3],
                         data_in[2],
                        ~data_in[2],
                         data_in[1],
                        ~data_in[1],
                         data_in[0],
                        ~data_in[0] };

assign data_out[0]  = ~codin_p[0];
assign data_out[1]  = ~codin_p[1];
assign data_out[2]  = ~codin_p[2];
assign data_out[3]  = ~codin_p[3];
assign data_out[4]  = ~codin_p[4];
assign data_out[5]  = ~codin_p[5];
assign data_out[6]  = ~codin_p[6];
assign data_out[7]  = ~codin_p[7];
assign data_out[8]  = ~codin_p[8];
assign data_out[9]  = ~codin_p[9];
assign data_out[10] = ~codin_p[10];
assign data_out[11] = ~codin_p[11];

assign data_oe[0]  = (codin_p[0]  == codin_n[0] );
assign data_oe[1]  = (codin_p[1]  == codin_n[1] );
assign data_oe[2]  = (codin_p[2]  == codin_n[2] );
assign data_oe[3]  = (codin_p[3]  == codin_n[3] );
assign data_oe[4]  = (codin_p[4]  == codin_n[4] );
assign data_oe[5]  = (codin_p[5]  == codin_n[5] );
assign data_oe[6]  = (codin_p[6]  == codin_n[6] );
assign data_oe[7]  = (codin_p[7]  == codin_n[7] );
assign data_oe[8]  = (codin_p[8]  == codin_n[8] );
assign data_oe[9]  = (codin_p[9]  == codin_n[9] );
assign data_oe[10] = (codin_p[10] == codin_n[10]);
assign data_oe[11] = (codin_p[11] == codin_n[11]);

assign ctl2dbc_wb_rdptr_vld0 = ctl2dbc0[0];
assign ctl2dbc_wb_rdptr0 = ctl2dbc0[6:1];
assign ctl2dbc_rb_wrptr_vld0 = ctl2dbc0[7];
assign ctl2dbc_rb_wrptr0 = ctl2dbc0[13:8];
assign ctl2dbc_rb_rdptr_vld0 = ctl2dbc0[15:14];
assign ctl2dbc_rb_rdptr0 = ctl2dbc0[27:16];
assign ctl2dbc_wrdata_vld0 = ctl2dbc0[28];
assign ctl2dbc_mask_entry0 = ctl2dbc0[29];
assign ctl2dbc_rdata_en_full0 = ctl2dbc0[33:30];
assign ctl2dbc_mrnk_read0 = ctl2dbc0[41:34];
assign ctl2dbc_seq_en0 = ctl2dbc0[42];
assign ctl2dbc_cs0 = ctl2dbc0[44:43];
assign ctl2dbc_misc0 = ctl2dbc0[48:45];
assign ctl2dbc_nop0 = ctl2dbc0[49];
assign ctl2dbc_rd_type0 = ctl2dbc0[50];

assign ctl2dbc_wb_rdptr_vld1 = ctl2dbc1[0];
assign ctl2dbc_wb_rdptr1 = ctl2dbc1[6:1];
assign ctl2dbc_rb_wrptr_vld1 = ctl2dbc1[7];
assign ctl2dbc_rb_wrptr1 = ctl2dbc1[13:8];
assign ctl2dbc_rb_rdptr_vld1 = ctl2dbc1[15:14];
assign ctl2dbc_rb_rdptr1 = ctl2dbc1[27:16];
assign ctl2dbc_wrdata_vld1 = ctl2dbc1[28];
assign ctl2dbc_mask_entry1 = ctl2dbc1[29];
assign ctl2dbc_rdata_en_full1 = ctl2dbc1[33:30];
assign ctl2dbc_mrnk_read1 = ctl2dbc1[41:34];
assign ctl2dbc_seq_en1 = ctl2dbc1[42];
assign ctl2dbc_cs1 = ctl2dbc1[44:43];
assign ctl2dbc_misc1 = ctl2dbc1[48:45];
assign ctl2dbc_nop1 = ctl2dbc1[49];
assign ctl2dbc_rd_type1 = ctl2dbc1[50];

assign dbc2ctl[0] = dbc2ctl_wb_retire_ptr_vld;
assign dbc2ctl[6:1] = dbc2ctl_wb_retire_ptr;
assign dbc2ctl[7] = dbc2ctl_rb_retire_ptr_vld;
assign dbc2ctl[13:8] = dbc2ctl_rb_retire_ptr;
assign dbc2ctl[14] = dbc2db_wb_wrptr_vld;
assign dbc2ctl[20:15] = dbc2db_wb_wrptr;
assign dbc2ctl[21] = dbc2ctl_rd_data_vld;
assign dbc2ctl[22] = dbc2ctl_all_rd_done;

assign cfg_reorder_rdata      = cfg_dbc[0];
assign cfg_rmw_en             = cfg_dbc[1]; 
assign cfg_output_regd        = cfg_dbc[2];
assign cfg_dbc_ctrl_sel       = cfg_dbc[3];
assign cfg_dbc_in_protocol    = cfg_dbc[4];
assign cfg_dbc_dualport_en    = cfg_dbc[5];
assign cfg_dbc_pipe_lat       = cfg_dbc[8:6];
assign cfg_cmd_rate           = cfg_dbc[11:9];
assign cfg_dbc_rc_en          = cfg_dbc[12];
assign cfg_dbc_slot_rotate_en = cfg_dbc[14:13];
assign cfg_dbc_slot_offset    = cfg_dbc[16:15];



reg cal_avl_rst_override;
reg cal_avl_tmp_clk;

initial begin
	cal_avl_rst_override = 1'b1;
	cal_avl_tmp_clk = 1'b0;
	#1;
	cal_avl_tmp_clk = 1'b1;
	#1;
	cal_avl_rst_override = 1'b0;
end

reg nfrzdrv;

initial begin
	nfrzdrv = 1'b1;
	#10
	nfrzdrv = 1'b0;
	#100 nfrzdrv = 1'b1;
end

assign avl_address_in   = cal_avl_rst_override ? 20'd0           : cal_avl_in[19:0]  ;
assign avl_writedata_in = cal_avl_rst_override ? 32'd0           : cal_avl_in[51:20] ;
assign avl_write_in     = cal_avl_rst_override ? 1'b0            : cal_avl_in[52]    ;
assign avl_read_in      = cal_avl_rst_override ? 1'b0            : cal_avl_in[53]    ;
assign avl_clk_in       = cal_avl_rst_override ? cal_avl_tmp_clk : cal_avl_in[54]    ;
assign cal_avl_readdata_out = avl_readdata_out;

assign cal_avl_out[19:0]  = cal_avl_rst_override ? 20'd0 : avl_address_out   ;
assign cal_avl_out[51:20] = cal_avl_rst_override ? 32'd0 : avl_writedata_out ;
assign cal_avl_out[52]    = cal_avl_rst_override ? 1'b0  : avl_write_out     ;
assign cal_avl_out[53]    = cal_avl_rst_override ? 1'b0  : avl_read_out      ;
assign cal_avl_out[54]    = cal_avl_rst_override ? 1'b0  : avl_clk_out       ;
assign avl_readdata_in = cal_avl_readdata_in;

assign debug_dqs_gated_a    = u_io_12_lane_bcm.i0.xio_dqs_lgc_top.dqs_out_a[0];
assign debug_dqs_enable_a   = u_io_12_lane_bcm.i0.xio_dqs_lgc_top.pstamble_reg_a.dqs_en_shrunk;
assign debug_dqs_gated_b    = u_io_12_lane_bcm.i0.xio_dqs_lgc_top.dqs_out_b[0];
assign debug_dqs_enable_b   = u_io_12_lane_bcm.i0.xio_dqs_lgc_top.pstamble_reg_b.dqs_en_shrunk;
assign debug_dq_delayed[0]  = u_io_12_lane_bcm.i0.ioereg_top_0_.ioereg_pnr_x2.dq_in_tree[1];
assign debug_dq_delayed[1]  = u_io_12_lane_bcm.i0.ioereg_top_0_.ioereg_pnr_x2.dq_in_tree[3];
assign debug_dq_delayed[2]  = u_io_12_lane_bcm.i0.ioereg_top_1_.ioereg_pnr_x2.dq_in_tree[1];
assign debug_dq_delayed[3]  = u_io_12_lane_bcm.i0.ioereg_top_1_.ioereg_pnr_x2.dq_in_tree[3];
assign debug_dq_delayed[4]  = u_io_12_lane_bcm.i0.ioereg_top_2_.ioereg_pnr_x2.dq_in_tree[1];
assign debug_dq_delayed[5]  = u_io_12_lane_bcm.i0.ioereg_top_2_.ioereg_pnr_x2.dq_in_tree[3];
assign debug_dq_delayed[6]  = u_io_12_lane_bcm.i0.ioereg_top_3_.ioereg_pnr_x2.dq_in_tree[1];
assign debug_dq_delayed[7]  = u_io_12_lane_bcm.i0.ioereg_top_3_.ioereg_pnr_x2.dq_in_tree[3];
assign debug_dq_delayed[8]  = u_io_12_lane_bcm.i0.ioereg_top_4_.ioereg_pnr_x2.dq_in_tree[1];
assign debug_dq_delayed[9]  = u_io_12_lane_bcm.i0.ioereg_top_4_.ioereg_pnr_x2.dq_in_tree[3];
assign debug_dq_delayed[10] = u_io_12_lane_bcm.i0.ioereg_top_5_.ioereg_pnr_x2.dq_in_tree[1];
assign debug_dq_delayed[11] = u_io_12_lane_bcm.i0.ioereg_top_5_.ioereg_pnr_x2.dq_in_tree[3];


io_12_lane_bcm__nf5es_abphy u_io_12_lane_bcm (
	.ac_hmc( ac_hmc ),	
	.afi_rlat_core( afi_rlat_core ),	
	.afi_wlat_core( afi_wlat_core ),	
	.atbi_0( ), 
	.atbi_1( ), 
	.atpg_en_n        ( 1'b1 ),			
	.avl_address_in( avl_address_in ),		
	.avl_address_out( avl_address_out ),		
	.avl_clk_in( avl_clk_in ),			
	.avl_clk_out( avl_clk_out ),			
	.avl_read_in( avl_read_in ),			
	.avl_read_out( avl_read_out ),			
	.avl_readdata_in( avl_readdata_in ),		
	.avl_readdata_out( avl_readdata_out ),		
	.avl_write_in( avl_write_in ),			
	.avl_write_out( avl_write_out ),		
	.avl_writedata_in( avl_writedata_in ),		
	.avl_writedata_out( avl_writedata_out ),	
	.bhniotri           ( 1'b0         ), 
	.broadcast_in_bot  ( broadcast_in_bot ), 	
	.broadcast_in_top  ( broadcast_in_top ), 	
	.broadcast_out_bot ( broadcast_out_bot ), 
	.broadcast_out_top ( broadcast_out_top ), 
	.cas_csrdin( 5'd0), 
	.cas_csrdout( ), 
	.cfg_cmd_rate( cfg_cmd_rate ),	
	.cfg_dbc_ctrl_sel( cfg_dbc_ctrl_sel ),	
	.cfg_dbc_dualport_en( cfg_dbc_dualport_en ),	
	.cfg_dbc_in_protocol( cfg_dbc_in_protocol ),	
	.cfg_dbc_pipe_lat( cfg_dbc_pipe_lat ),	
	.cfg_dbc_rc_en( cfg_dbc_rc_en ),  
	.cfg_dbc_slot_offset( cfg_dbc_slot_offset ),  
	.cfg_dbc_slot_rotate_en( cfg_dbc_slot_rotate_en ),  
	.cfg_output_regd( cfg_output_regd ),	
	.cfg_reorder_rdata( cfg_reorder_rdata ),	
	.cfg_rmw_en( cfg_rmw_en ),	
	.clk_pll( dll_ref_clk ),	
	.codin_n ( codin_n ),	
	.codin_nb( codin_nb ),	
	.codin_p ( codin_p ),	
	.codin_pb( codin_pb ),	
	.core2dbc_rd_data_rdy ( core2dbc_rd_data_rdy ),	
	.core2dbc_wr_data_vld0( core2dbc_wr_data_vld0 ),	
	.core2dbc_wr_data_vld1( core2dbc_wr_data_vld1 ),	
	.core2dbc_wr_ecc_info( core2dbc_wr_ecc_info ),  
	.core_dll( core_dll ),	
	.crnt_clk  (  ),	
	.ctl2dbc_cs0( ctl2dbc_cs0 ),  
	.ctl2dbc_cs1( ctl2dbc_cs1 ),  
	.ctl2dbc_mask_entry0( ctl2dbc_mask_entry0 ),	
	.ctl2dbc_mask_entry1( ctl2dbc_mask_entry1 ),	
	.ctl2dbc_misc0( ctl2dbc_misc0 ),  
	.ctl2dbc_misc1( ctl2dbc_misc1 ),  
	.ctl2dbc_mrnk_read0( ctl2dbc_mrnk_read0 ),	
	.ctl2dbc_mrnk_read1( ctl2dbc_mrnk_read1 ),	
	.ctl2dbc_nop0( ctl2dbc_nop0 ),  
	.ctl2dbc_nop1( ctl2dbc_nop1 ),  
	.ctl2dbc_rb_rdptr0( ctl2dbc_rb_rdptr0 ),	
	.ctl2dbc_rb_rdptr1( ctl2dbc_rb_rdptr1 ),	
	.ctl2dbc_rb_rdptr_vld0( ctl2dbc_rb_rdptr_vld0 ),	
	.ctl2dbc_rb_rdptr_vld1( ctl2dbc_rb_rdptr_vld1 ),	
	.ctl2dbc_rb_wrptr0( ctl2dbc_rb_wrptr0 ),	
	.ctl2dbc_rb_wrptr1( ctl2dbc_rb_wrptr1 ),	
	.ctl2dbc_rb_wrptr_vld0( ctl2dbc_rb_wrptr_vld0 ),	
	.ctl2dbc_rb_wrptr_vld1( ctl2dbc_rb_wrptr_vld1 ),	
	.ctl2dbc_rd_type0( ctl2dbc_rd_type0 ),  
	.ctl2dbc_rd_type1( ctl2dbc_rd_type1 ),  
	.ctl2dbc_rdata_en_full0( ctl2dbc_rdata_en_full0 ),	
	.ctl2dbc_rdata_en_full1( ctl2dbc_rdata_en_full1 ),	
	.ctl2dbc_seq_en0( ctl2dbc_seq_en0 ),	
	.ctl2dbc_seq_en1( ctl2dbc_seq_en1 ),	
	.ctl2dbc_wb_rdptr0( ctl2dbc_wb_rdptr0 ),	
	.ctl2dbc_wb_rdptr1( ctl2dbc_wb_rdptr1 ),	
	.ctl2dbc_wb_rdptr_vld0( ctl2dbc_wb_rdptr_vld0 ),	
	.ctl2dbc_wb_rdptr_vld1( ctl2dbc_wb_rdptr_vld1 ),	
	.ctl2dbc_wrdata_vld0( ctl2dbc_wrdata_vld0 ),	
	.ctl2dbc_wrdata_vld1( ctl2dbc_wrdata_vld1 ),	
	.data_from_core( data_from_core ),	
	.data_to_core( data_to_core ),	
	.dbc2core_rd_data_vld0( dbc2core_rd_data_vld0 ),	
	.dbc2core_rd_data_vld1( dbc2core_rd_data_vld1 ),	
	.dbc2core_rd_type( dbc2core_rd_type), 
	.dbc2core_wb_pointer( dbc2core_wb_pointer ),  
	.dbc2core_wr_data_rdy ( dbc2core_wr_data_rdy ),	
	.dbc2ctl_all_rd_done( dbc2ctl_all_rd_done), 
	.dbc2ctl_rb_retire_ptr( dbc2ctl_rb_retire_ptr ),	
	.dbc2ctl_rb_retire_ptr_vld( dbc2ctl_rb_retire_ptr_vld ),	
	.dbc2ctl_rd_data_vld( dbc2ctl_rd_data_vld ), 	
	.dbc2ctl_wb_retire_ptr( dbc2ctl_wb_retire_ptr ),	
	.dbc2ctl_wb_retire_ptr_vld( dbc2ctl_wb_retire_ptr_vld ),	
	.dbc2db_wb_wrptr( dbc2db_wb_wrptr ),	
	.dbc2db_wb_wrptr_vld( dbc2db_wb_wrptr_vld ),	
	.dft_core2db( 8'b0 ),  
	.dft_db2core(  ),  
	.dft_phy_clk(dft_phy_clk  ),  
	.dft_prbs_done(  ),		
	.dft_prbs_pass(  ),		
	.dll_core( dll_core ),	
	.dq_diff_in( dq_in ),	
	.dq_sstl_in( dq_in ),	
	.dqs_diff_in_0( {dqs_in[0],~dqs_in[0]} ),	
	.dqs_diff_in_1( {dqs_in[0],~dqs_in[0]} ),	
	.dqs_diff_in_2( {dqs_in[0],~dqs_in[0]} ),	
	.dqs_diff_in_3( {dqs_in[0],~dqs_in[0]} ),	
	.dqs_sstl_n_0( {dqs_in[1],~dqs_in[1]} ),	
	.dqs_sstl_n_1( {dqs_in[1],~dqs_in[1]} ),	
	.dqs_sstl_n_2( {dqs_in[1],~dqs_in[1]} ),	
	.dqs_sstl_n_3( {dqs_in[1],~dqs_in[1]} ),	
	.dqs_sstl_p_0( {dqs_in[0],~dqs_in[0]} ),	
	.dqs_sstl_p_1( {dqs_in[0],~dqs_in[0]} ),	
	.dqs_sstl_p_2( {dqs_in[0],~dqs_in[0]} ),	
	.dqs_sstl_p_3( {dqs_in[0],~dqs_in[0]} ),	
	.dzoutx( 6'd0 ),	
	.early_bhniotri     ( 1'b0         ), 
	.early_csren        ( 1'b0         ), 
	.early_enrnsl       ( 1'b0         ), 
	.early_frzreg       ( 1'b0         ), 
	.early_nfrzdrv      ( 1'b0         ), 
	.early_niotri       ( 1'b0         ), 
	.early_plniotri     ( 1'b0         ), 
	.early_usrmode      ( 1'b0         ), 
	.enrnsl             ( 1'b0         ), 
	.entest( 1'b0 ),	
	.fb_clkout( ),	
	.fr_in_clk ( 12'd0 ),	
	.fr_out_clk( 12'd0 ),	
	.frzreg             ( 1'b0         ), 
	.hps_to_core_ctrl_en(     ), 
	.hr_in_clk ( 12'd0 ),	
	.hr_out_clk( 12'd0 ),	
	.i50u_ref    ( 1'b0 ),	
	.ibp50u      ( 1'b0 ),	
	.ibp50u_cal( 1'b0 ), 
	.ioereg_locked( ioereg_locked), 
	.jtag_clk   ( 1'b0),	
	.jtag_highz ( 1'b0 ),	
	.jtag_mode  ( 1'b0 ),	
	.jtag_sdin  ( 1'b0 ),	
	.jtag_sdout ( ),	
	.jtag_shftdr( 1'b0 ),	
	.jtag_updtdr( 1'b0 ),	
	.lane_cal_done( ), 
	.local_bhniotri     (     ), 
	.local_enrnsl       (     ), 
	.local_frzreg       (     ), 
	.local_nfrzdrv      (     ), 
	.local_niotri       (     ), 
	.local_plniotri     (     ), 
	.local_usrmode      (     ), 
	.local_wkpullup     (     ), 
	.lvds_rx_clk_chnl0( ),	
	.lvds_rx_clk_chnl1( ),	
	.lvds_rx_clk_chnl2( ),	
	.lvds_rx_clk_chnl3( ),	
	.lvds_rx_clk_chnl4( ),	
	.lvds_rx_clk_chnl5( ),	
	.lvds_tx_clk_chnl0( ),	
	.lvds_tx_clk_chnl1( ),	
	.lvds_tx_clk_chnl2( ),	
	.lvds_tx_clk_chnl3( ),	
	.lvds_tx_clk_chnl4( ),	
	.lvds_tx_clk_chnl5( ),	
	.mrnk_read_core( mrnk_read_core ),	
	.mrnk_write_core( mrnk_write_core ),	
	.n_crnt_clk(  ),	
	.n_next_clk(  ),	
	.naclr     ( 12'd0 ),	
	.ncein     ( 12'd0 ),	
	.nceout    ( 12'd0 ),	
	.next_clk  (  ),	
	.nfrzdrv            ( nfrzdrv         ), 
	.niotri             ( 1'b1         ), 
	.nsclr     ( 12'd0 ),	
	.oct_enable( oct_enable ),	
	.oeb_from_core( ~oe_from_core ),	
	.osc_en_n( 1'b1 ), 
	.osc_enable_in( 1'b0 ), 
	.osc_mode_in( 1'b1 ), 
	.osc_rocount_to_core(), 
	.osc_sel_n( 1'b0 ),
	.phy_clk     ( {3'd0,phy_clk} ),	
	.phy_clk_phs ( phy_clk_phs ),	
	.pipeline_global_en_n( 1'b1 ),	
	.pll_clk   ( 1'b0 ),	
	.pll_locked( pll_locked ),	
	.plniotri           ( 1'b0         ), 
	.progctl( 12'd0 ),	
	.progoe ( 12'd0),	
	.progout( 12'd0 ),	
	.rdata_en_full_core( rdata_en_full_core ),	
	.rdata_valid_core( rdata_valid_core ),	
	.regulator_clk( 1'b0 ), 
	.reinit( 1'b0 ),	
	.reset_n     ( reset_n ),	
	.scan_shift_n( 1'b1 ),	
	.scanin( 1'b0 ),	
	.scanout( ),	
	.switch_dn( 6'd0 ),	
	.switch_up( 6'd0 ),	
	.sync_clk_bot_in( sync_clk_bot_in ),		
	.sync_clk_bot_out( sync_clk_bot_out ),	
	.sync_clk_top_in( sync_clk_top_in ),		
	.sync_clk_top_out( sync_clk_top_out ),	
	.sync_data_bot_in( sync_data_bot_in ),	
	.sync_data_bot_out( sync_data_bot_out ),	
	.sync_data_top_in( sync_data_top_in ),	
	.sync_data_top_out( sync_data_top_out ),	
	.test_avl_clk_in_en_n  ( 1'b0 ),      
	.test_clk(  ),	
	.test_clk_ph_buf_en_n( 1'b0 ),    
	.test_clk_pll_en_n( 1'b1 ),	
	.test_clr_n( 1'b1 ),			
	.test_datovr_en_n      ( 1'b0 ),          
	.test_db_csr_in( 1'b0 ), 
	.test_dbg_in( ), 	
	.test_dbg_out( 12'd0 ),  	
	.test_dqs_csr_in( 1'b0), 
	.test_dqs_enable_en_n  ( 1'b0 ),      
	.test_fr_clk_en_n ( 1'b1 ),		
	.test_hr_clk_en_n ( 1'b1 ),		
	.test_int_clk_en_n     ( 1'b0 ),         
	.test_interp_clk_en_n  ( 1'b0 ),      
	.test_ioereg2_csr_out( ), 
	.test_phy_clk_en_n( 1'b1 ), 	
	.test_phy_clk_lane_en_n( 1'b0 ),    
	.test_pst_clk_en_n( 1'b0), 
	.test_pst_dll_i( 1'b0), 
	.test_pst_dll_o( ), 
	.test_tdf_select_n( 1'b1 ),	
	.test_vref_csr_out( ), 
	.test_xor_clk( ),	
	.tpctl( 12'd0  ),	
	.tpdata( ),	
	.tpin ( 12'd0 ),	
	.up_ph( 6'd0 ),	
	.usrmode            ( 1'b0         ), 
	.vref_ext( 1'b1 ),	
	.vref_int(  ),	
	.weak_pullup_enable( ), 
	.wkpullup           ( 1'b0         ), 
	.x1024_osc_out( ), 
	.xor_vref(), 
	.xprio_clk( 1'b0), 
	.xprio_sync( 1'b0), 
	.xprio_xbus( 8'b0) 

  );

defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_0__a_ac_dqs_dm_dq = (db_pin_0_mode == "dq_mode" || db_pin_0_mode == "dq_wdb_mode") ? "dq_type" : ( (db_pin_0_mode == "dm_mode" || db_pin_0_mode == "dm_wdb_mode") ? "dm_type":  ( (db_pin_0_mode == "dqs_mode" || db_pin_0_mode == "dqs_wdb_mode") ? "dqs_type" : "ac_type" ) );
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_0__a_db_oe_bypass  = (db_pin_0_oe_bypass == "true") ? "db_oe_bypass" : "db_oe_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_0__a_db_out_bypass = (db_pin_0_out_bypass == "true") ? "db_out_bypass" : "db_out_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_0__a_oe_datapath_prgmnvrt = (db_pin_0_oe_invert == "true") ? "oe_datapath_invert" : "oe_datapath_non_invert";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_0__a_rb_sel_ac_hmc_ena = (db_pin_0_ac_hmc_data_override_ena == "true") ? "sel_ac_hmc_ena" : "sel_ac_hmc_disable";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_0__a_wr_datapath_prgmnvrt = (db_pin_0_wr_invert == "true") ? "wr_datapath_invert" : "wr_datapath_non_invert";

defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_1__a_ac_dqs_dm_dq = (db_pin_1_mode == "dq_mode" || db_pin_1_mode == "dq_wdb_mode") ? "dq_type" : ( (db_pin_1_mode == "dm_mode" || db_pin_1_mode == "dm_wdb_mode") ? "dm_type":  ( (db_pin_1_mode == "dqs_mode" || db_pin_1_mode == "dqs_wdb_mode") ? "dqs_type" : "ac_type" ) );
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_1__a_db_oe_bypass  = (db_pin_1_oe_bypass == "true") ? "db_oe_bypass" : "db_oe_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_1__a_db_out_bypass = (db_pin_1_out_bypass == "true") ? "db_out_bypass" : "db_out_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_1__a_oe_datapath_prgmnvrt = (db_pin_1_oe_invert == "true") ? "oe_datapath_invert" : "oe_datapath_non_invert";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_1__a_rb_sel_ac_hmc_ena = (db_pin_1_ac_hmc_data_override_ena == "true") ? "sel_ac_hmc_ena" : "sel_ac_hmc_disable";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_1__a_wr_datapath_prgmnvrt = (db_pin_1_wr_invert == "true") ? "wr_datapath_invert" : "wr_datapath_non_invert";

defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_2__a_ac_dqs_dm_dq = (db_pin_2_mode == "dq_mode" || db_pin_2_mode == "dq_wdb_mode") ? "dq_type" : ( (db_pin_2_mode == "dm_mode" || db_pin_2_mode == "dm_wdb_mode") ? "dm_type":  ( (db_pin_2_mode == "dqs_mode" || db_pin_2_mode == "dqs_wdb_mode") ? "dqs_type" : "ac_type" ) );
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_2__a_db_oe_bypass  = (db_pin_2_oe_bypass == "true") ? "db_oe_bypass" : "db_oe_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_2__a_db_out_bypass = (db_pin_2_out_bypass == "true") ? "db_out_bypass" : "db_out_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_2__a_oe_datapath_prgmnvrt = (db_pin_2_oe_invert == "true") ? "oe_datapath_invert" : "oe_datapath_non_invert";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_2__a_rb_sel_ac_hmc_ena = (db_pin_2_ac_hmc_data_override_ena == "true") ? "sel_ac_hmc_ena" : "sel_ac_hmc_disable";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_2__a_wr_datapath_prgmnvrt = (db_pin_2_wr_invert == "true") ? "wr_datapath_invert" : "wr_datapath_non_invert";

defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_3__a_ac_dqs_dm_dq = (db_pin_3_mode == "dq_mode" || db_pin_3_mode == "dq_wdb_mode") ? "dq_type" : ( (db_pin_3_mode == "dm_mode" || db_pin_3_mode == "dm_wdb_mode") ? "dm_type":  ( (db_pin_3_mode == "dqs_mode" || db_pin_3_mode == "dqs_wdb_mode") ? "dqs_type" : "ac_type" ) );
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_3__a_db_oe_bypass  = (db_pin_3_oe_bypass == "true") ? "db_oe_bypass" : "db_oe_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_3__a_db_out_bypass = (db_pin_3_out_bypass == "true") ? "db_out_bypass" : "db_out_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_3__a_oe_datapath_prgmnvrt = (db_pin_3_oe_invert == "true") ? "oe_datapath_invert" : "oe_datapath_non_invert";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_3__a_rb_sel_ac_hmc_ena = (db_pin_3_ac_hmc_data_override_ena == "true") ? "sel_ac_hmc_ena" : "sel_ac_hmc_disable";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_3__a_wr_datapath_prgmnvrt = (db_pin_3_wr_invert == "true") ? "wr_datapath_invert" : "wr_datapath_non_invert";

defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_4__a_ac_dqs_dm_dq = (db_pin_4_mode == "dq_mode" || db_pin_4_mode == "dq_wdb_mode") ? "dq_type" : ( (db_pin_4_mode == "dm_mode" || db_pin_4_mode == "dm_wdb_mode") ? "dm_type":  ( (db_pin_4_mode == "dqs_mode" || db_pin_4_mode == "dqs_wdb_mode") ? "dqs_type" : "ac_type" ) );
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_4__a_db_oe_bypass  = (db_pin_4_oe_bypass == "true") ? "db_oe_bypass" : "db_oe_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_4__a_db_out_bypass = (db_pin_4_out_bypass == "true") ? "db_out_bypass" : "db_out_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_4__a_oe_datapath_prgmnvrt = (db_pin_4_oe_invert == "true") ? "oe_datapath_invert" : "oe_datapath_non_invert";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_4__a_rb_sel_ac_hmc_ena = (db_pin_4_ac_hmc_data_override_ena == "true") ? "sel_ac_hmc_ena" : "sel_ac_hmc_disable";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_4__a_wr_datapath_prgmnvrt = (db_pin_4_wr_invert == "true") ? "wr_datapath_invert" : "wr_datapath_non_invert";

defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_5__a_ac_dqs_dm_dq = (db_pin_5_mode == "dq_mode" || db_pin_5_mode == "dq_wdb_mode") ? "dq_type" : ( (db_pin_5_mode == "dm_mode" || db_pin_5_mode == "dm_wdb_mode") ? "dm_type":  ( (db_pin_5_mode == "dqs_mode" || db_pin_5_mode == "dqs_wdb_mode") ? "dqs_type" : "ac_type" ) );
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_5__a_db_oe_bypass  = (db_pin_5_oe_bypass == "true") ? "db_oe_bypass" : "db_oe_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_5__a_db_out_bypass = (db_pin_5_out_bypass == "true") ? "db_out_bypass" : "db_out_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_5__a_oe_datapath_prgmnvrt = (db_pin_5_oe_invert == "true") ? "oe_datapath_invert" : "oe_datapath_non_invert";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_5__a_rb_sel_ac_hmc_ena = (db_pin_5_ac_hmc_data_override_ena == "true") ? "sel_ac_hmc_ena" : "sel_ac_hmc_disable";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_5__a_wr_datapath_prgmnvrt = (db_pin_5_wr_invert == "true") ? "wr_datapath_invert" : "wr_datapath_non_invert";

defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_6__a_ac_dqs_dm_dq = (db_pin_6_mode == "dq_mode" || db_pin_6_mode == "dq_wdb_mode") ? "dq_type" : ( (db_pin_6_mode == "dm_mode" || db_pin_6_mode == "dm_wdb_mode") ? "dm_type":  ( (db_pin_6_mode == "dqs_mode" || db_pin_6_mode == "dqs_wdb_mode") ? "dqs_type" : "ac_type" ) );
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_6__a_db_oe_bypass  = (db_pin_6_oe_bypass == "true") ? "db_oe_bypass" : "db_oe_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_6__a_db_out_bypass = (db_pin_6_out_bypass == "true") ? "db_out_bypass" : "db_out_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_6__a_oe_datapath_prgmnvrt = (db_pin_6_oe_invert == "true") ? "oe_datapath_invert" : "oe_datapath_non_invert";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_6__a_rb_sel_ac_hmc_ena = (db_pin_6_ac_hmc_data_override_ena == "true") ? "sel_ac_hmc_ena" : "sel_ac_hmc_disable";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_6__a_wr_datapath_prgmnvrt = (db_pin_6_wr_invert == "true") ? "wr_datapath_invert" : "wr_datapath_non_invert";

defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_7__a_ac_dqs_dm_dq = (db_pin_7_mode == "dq_mode" || db_pin_7_mode == "dq_wdb_mode") ? "dq_type" : ( (db_pin_7_mode == "dm_mode" || db_pin_7_mode == "dm_wdb_mode") ? "dm_type":  ( (db_pin_7_mode == "dqs_mode" || db_pin_7_mode == "dqs_wdb_mode") ? "dqs_type" : "ac_type" ) );
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_7__a_db_oe_bypass  = (db_pin_7_oe_bypass == "true") ? "db_oe_bypass" : "db_oe_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_7__a_db_out_bypass = (db_pin_7_out_bypass == "true") ? "db_out_bypass" : "db_out_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_7__a_oe_datapath_prgmnvrt = (db_pin_7_oe_invert == "true") ? "oe_datapath_invert" : "oe_datapath_non_invert";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_7__a_rb_sel_ac_hmc_ena = (db_pin_7_ac_hmc_data_override_ena == "true") ? "sel_ac_hmc_ena" : "sel_ac_hmc_disable";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_7__a_wr_datapath_prgmnvrt = (db_pin_7_wr_invert == "true") ? "wr_datapath_invert" : "wr_datapath_non_invert";

defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_8__a_ac_dqs_dm_dq = (db_pin_8_mode == "dq_mode" || db_pin_8_mode == "dq_wdb_mode") ? "dq_type" : ( (db_pin_8_mode == "dm_mode" || db_pin_8_mode == "dm_wdb_mode") ? "dm_type":  ( (db_pin_8_mode == "dqs_mode" || db_pin_8_mode == "dqs_wdb_mode") ? "dqs_type" : "ac_type" ) );
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_8__a_db_oe_bypass  = (db_pin_8_oe_bypass == "true") ? "db_oe_bypass" : "db_oe_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_8__a_db_out_bypass = (db_pin_8_out_bypass == "true") ? "db_out_bypass" : "db_out_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_8__a_oe_datapath_prgmnvrt = (db_pin_8_oe_invert == "true") ? "oe_datapath_invert" : "oe_datapath_non_invert";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_8__a_rb_sel_ac_hmc_ena = (db_pin_8_ac_hmc_data_override_ena == "true") ? "sel_ac_hmc_ena" : "sel_ac_hmc_disable";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_8__a_wr_datapath_prgmnvrt = (db_pin_8_wr_invert == "true") ? "wr_datapath_invert" : "wr_datapath_non_invert";

defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_9__a_ac_dqs_dm_dq = (db_pin_9_mode == "dq_mode" || db_pin_9_mode == "dq_wdb_mode") ? "dq_type" : ( (db_pin_9_mode == "dm_mode" || db_pin_9_mode == "dm_wdb_mode") ? "dm_type":  ( (db_pin_9_mode == "dqs_mode" || db_pin_9_mode == "dqs_wdb_mode") ? "dqs_type" : "ac_type" ) );
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_9__a_db_oe_bypass  = (db_pin_9_oe_bypass == "true") ? "db_oe_bypass" : "db_oe_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_9__a_db_out_bypass = (db_pin_9_out_bypass == "true") ? "db_out_bypass" : "db_out_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_9__a_oe_datapath_prgmnvrt = (db_pin_9_oe_invert == "true") ? "oe_datapath_invert" : "oe_datapath_non_invert";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_9__a_rb_sel_ac_hmc_ena = (db_pin_9_ac_hmc_data_override_ena == "true") ? "sel_ac_hmc_ena" : "sel_ac_hmc_disable";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_9__a_wr_datapath_prgmnvrt = (db_pin_9_wr_invert == "true") ? "wr_datapath_invert" : "wr_datapath_non_invert";

defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_10__a_ac_dqs_dm_dq = (db_pin_10_mode == "dq_mode" || db_pin_10_mode == "dq_wdb_mode") ? "dq_type" : ( (db_pin_10_mode == "dm_mode" || db_pin_10_mode == "dm_wdb_mode") ? "dm_type":  ( (db_pin_10_mode == "dqs_mode" || db_pin_10_mode == "dqs_wdb_mode") ? "dqs_type" : "ac_type" ) );
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_10__a_db_oe_bypass  = (db_pin_10_oe_bypass == "true") ? "db_oe_bypass" : "db_oe_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_10__a_db_out_bypass = (db_pin_10_out_bypass == "true") ? "db_out_bypass" : "db_out_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_10__a_oe_datapath_prgmnvrt = (db_pin_10_oe_invert == "true") ? "oe_datapath_invert" : "oe_datapath_non_invert";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_10__a_rb_sel_ac_hmc_ena = (db_pin_10_ac_hmc_data_override_ena == "true") ? "sel_ac_hmc_ena" : "sel_ac_hmc_disable";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_10__a_wr_datapath_prgmnvrt = (db_pin_10_wr_invert == "true") ? "wr_datapath_invert" : "wr_datapath_non_invert";

defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_11__a_ac_dqs_dm_dq = (db_pin_11_mode == "dq_mode" || db_pin_11_mode == "dq_wdb_mode") ? "dq_type" : ( (db_pin_11_mode == "dm_mode" || db_pin_11_mode == "dm_wdb_mode") ? "dm_type":  ( (db_pin_11_mode == "dqs_mode" || db_pin_11_mode == "dqs_wdb_mode") ? "dqs_type" : "ac_type" ) );
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_11__a_db_oe_bypass  = (db_pin_11_oe_bypass == "true") ? "db_oe_bypass" : "db_oe_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_11__a_db_out_bypass = (db_pin_11_out_bypass == "true") ? "db_out_bypass" : "db_out_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_11__a_oe_datapath_prgmnvrt = (db_pin_11_oe_invert == "true") ? "oe_datapath_invert" : "oe_datapath_non_invert";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_11__a_rb_sel_ac_hmc_ena = (db_pin_11_ac_hmc_data_override_ena == "true") ? "sel_ac_hmc_ena" : "sel_ac_hmc_disable";
defparam u_io_12_lane_bcm.data_buffer__data_buffer_out_if_inst__io_data_buffer_out_mux_inst_11__a_wr_datapath_prgmnvrt = (db_pin_11_wr_invert == "true") ? "wr_datapath_invert" : "wr_datapath_non_invert";


defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_0__a_db_in_bypass = (db_pin_0_in_bypass == "true") ? "db_in_bypass" : "db_in_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_0__a_dbc_sel = ((db_pin_0_mode == "ac_core") || (db_pin_0_mode == "dq_mode") || (db_pin_0_mode == "dm_mode") || (db_pin_0_mode == "dqs_mode")) ? "sel_core" : "sel_dbc";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_0__a_oe_datapath_mod = "oe_datapath_one_cycle";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_0__a_prbs = "not_sel_prbs";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_0__a_wdb_bypass  = (db_pin_0_mode == "dq_wdb_mode" || db_pin_0_mode == "dm_wdb_mode" || db_pin_0_mode == "dqs_wdb_mode" || db_pin_0_mode == "ac_hmc") ? "wdb_not_bypass" : "wdb_bypass";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_0__a_wr_datapath_mod = "wr_datapath_one_cycle";

defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_1__a_db_in_bypass = (db_pin_1_in_bypass == "true") ? "db_in_bypass" : "db_in_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_1__a_dbc_sel = ((db_pin_1_mode == "ac_core") || (db_pin_1_mode == "dq_mode") || (db_pin_1_mode == "dm_mode") || (db_pin_1_mode == "dqs_mode")) ? "sel_core" : "sel_dbc";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_1__a_oe_datapath_mod = "oe_datapath_one_cycle";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_1__a_prbs = "not_sel_prbs";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_1__a_wdb_bypass  = (db_pin_1_mode == "dq_wdb_mode" || db_pin_1_mode == "dm_wdb_mode" || db_pin_1_mode == "dqs_wdb_mode" || db_pin_1_mode == "ac_hmc") ? "wdb_not_bypass" : "wdb_bypass";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_1__a_wr_datapath_mod = "wr_datapath_one_cycle";

defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_2__a_db_in_bypass = (db_pin_2_in_bypass == "true") ? "db_in_bypass" : "db_in_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_2__a_dbc_sel = ((db_pin_2_mode == "ac_core") || (db_pin_2_mode == "dq_mode") || (db_pin_2_mode == "dm_mode") || (db_pin_2_mode == "dqs_mode")) ? "sel_core" : "sel_dbc";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_2__a_oe_datapath_mod = "oe_datapath_one_cycle";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_2__a_prbs = "not_sel_prbs";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_2__a_wdb_bypass  = (db_pin_2_mode == "dq_wdb_mode" || db_pin_2_mode == "dm_wdb_mode" || db_pin_2_mode == "dqs_wdb_mode" || db_pin_2_mode == "ac_hmc") ? "wdb_not_bypass" : "wdb_bypass";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_2__a_wr_datapath_mod = "wr_datapath_one_cycle";

defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_3__a_db_in_bypass = (db_pin_3_in_bypass == "true") ? "db_in_bypass" : "db_in_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_3__a_dbc_sel = ((db_pin_3_mode == "ac_core") || (db_pin_3_mode == "dq_mode") || (db_pin_3_mode == "dm_mode") || (db_pin_3_mode == "dqs_mode")) ? "sel_core" : "sel_dbc";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_3__a_oe_datapath_mod = "oe_datapath_one_cycle";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_3__a_prbs = "not_sel_prbs";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_3__a_wdb_bypass  = (db_pin_3_mode == "dq_wdb_mode" || db_pin_3_mode == "dm_wdb_mode" || db_pin_3_mode == "dqs_wdb_mode" || db_pin_3_mode == "ac_hmc") ? "wdb_not_bypass" : "wdb_bypass";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_3__a_wr_datapath_mod = "wr_datapath_one_cycle";

defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_4__a_db_in_bypass = (db_pin_4_in_bypass == "true") ? "db_in_bypass" : "db_in_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_4__a_dbc_sel = ((db_pin_4_mode == "ac_core") || (db_pin_4_mode == "dq_mode") || (db_pin_4_mode == "dm_mode") || (db_pin_4_mode == "dqs_mode")) ? "sel_core" : "sel_dbc";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_4__a_oe_datapath_mod = "oe_datapath_one_cycle";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_4__a_prbs = "not_sel_prbs";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_4__a_wdb_bypass  = (db_pin_4_mode == "dq_wdb_mode" || db_pin_4_mode == "dm_wdb_mode" || db_pin_4_mode == "dqs_wdb_mode" || db_pin_4_mode == "ac_hmc") ? "wdb_not_bypass" : "wdb_bypass";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_4__a_wr_datapath_mod = "wr_datapath_one_cycle";

defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_5__a_db_in_bypass = (db_pin_5_in_bypass == "true") ? "db_in_bypass" : "db_in_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_5__a_dbc_sel = ((db_pin_5_mode == "ac_core") || (db_pin_5_mode == "dq_mode") || (db_pin_5_mode == "dm_mode") || (db_pin_5_mode == "dqs_mode")) ? "sel_core" : "sel_dbc";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_5__a_oe_datapath_mod = "oe_datapath_one_cycle";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_5__a_prbs = "not_sel_prbs";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_5__a_wdb_bypass  = (db_pin_5_mode == "dq_wdb_mode" || db_pin_5_mode == "dm_wdb_mode" || db_pin_5_mode == "dqs_wdb_mode" || db_pin_5_mode == "ac_hmc") ? "wdb_not_bypass" : "wdb_bypass";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_5__a_wr_datapath_mod = "wr_datapath_one_cycle";

defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_6__a_db_in_bypass = (db_pin_6_in_bypass == "true") ? "db_in_bypass" : "db_in_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_6__a_dbc_sel = ((db_pin_6_mode == "ac_core") || (db_pin_6_mode == "dq_mode") || (db_pin_6_mode == "dm_mode") || (db_pin_6_mode == "dqs_mode")) ? "sel_core" : "sel_dbc";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_6__a_oe_datapath_mod = "oe_datapath_one_cycle";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_6__a_prbs = "not_sel_prbs";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_6__a_wdb_bypass  = (db_pin_6_mode == "dq_wdb_mode" || db_pin_6_mode == "dm_wdb_mode" || db_pin_6_mode == "dqs_wdb_mode" || db_pin_6_mode == "ac_hmc") ? "wdb_not_bypass" : "wdb_bypass";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_6__a_wr_datapath_mod = "wr_datapath_one_cycle";

defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_7__a_db_in_bypass = (db_pin_7_in_bypass == "true") ? "db_in_bypass" : "db_in_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_7__a_dbc_sel = ((db_pin_7_mode == "ac_core") || (db_pin_7_mode == "dq_mode") || (db_pin_7_mode == "dm_mode") || (db_pin_7_mode == "dqs_mode")) ? "sel_core" : "sel_dbc";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_7__a_oe_datapath_mod = "oe_datapath_one_cycle";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_7__a_prbs = "not_sel_prbs";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_7__a_wdb_bypass  = (db_pin_7_mode == "dq_wdb_mode" || db_pin_7_mode == "dm_wdb_mode" || db_pin_7_mode == "dqs_wdb_mode" || db_pin_7_mode == "ac_hmc") ? "wdb_not_bypass" : "wdb_bypass";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_7__a_wr_datapath_mod = "wr_datapath_one_cycle";

defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_8__a_db_in_bypass = (db_pin_8_in_bypass == "true") ? "db_in_bypass" : "db_in_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_8__a_dbc_sel = ((db_pin_8_mode == "ac_core") || (db_pin_8_mode == "dq_mode") || (db_pin_8_mode == "dm_mode") || (db_pin_8_mode == "dqs_mode")) ? "sel_core" : "sel_dbc";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_8__a_oe_datapath_mod = "oe_datapath_one_cycle";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_8__a_prbs = "not_sel_prbs";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_8__a_wdb_bypass  = (db_pin_8_mode == "dq_wdb_mode" || db_pin_8_mode == "dm_wdb_mode" || db_pin_8_mode == "dqs_wdb_mode" || db_pin_8_mode == "ac_hmc") ? "wdb_not_bypass" : "wdb_bypass";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_8__a_wr_datapath_mod = "wr_datapath_one_cycle";

defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_9__a_db_in_bypass = (db_pin_9_in_bypass == "true") ? "db_in_bypass" : "db_in_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_9__a_dbc_sel = ((db_pin_9_mode == "ac_core") || (db_pin_9_mode == "dq_mode") || (db_pin_9_mode == "dm_mode") || (db_pin_9_mode == "dqs_mode")) ? "sel_core" : "sel_dbc";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_9__a_oe_datapath_mod = "oe_datapath_one_cycle";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_9__a_prbs = "not_sel_prbs";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_9__a_wdb_bypass  = (db_pin_9_mode == "dq_wdb_mode" || db_pin_9_mode == "dm_wdb_mode" || db_pin_9_mode == "dqs_wdb_mode" || db_pin_9_mode == "ac_hmc") ? "wdb_not_bypass" : "wdb_bypass";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_9__a_wr_datapath_mod = "wr_datapath_one_cycle";

defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_10__a_db_in_bypass = (db_pin_10_in_bypass == "true") ? "db_in_bypass" : "db_in_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_10__a_dbc_sel = ((db_pin_10_mode == "ac_core") || (db_pin_10_mode == "dq_mode") || (db_pin_10_mode == "dm_mode") || (db_pin_10_mode == "dqs_mode")) ? "sel_core" : "sel_dbc";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_10__a_oe_datapath_mod = "oe_datapath_one_cycle";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_10__a_prbs = "not_sel_prbs";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_10__a_wdb_bypass  = (db_pin_10_mode == "dq_wdb_mode" || db_pin_10_mode == "dm_wdb_mode" || db_pin_10_mode == "dqs_wdb_mode" || db_pin_10_mode == "ac_hmc") ? "wdb_not_bypass" : "wdb_bypass";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_10__a_wr_datapath_mod = "wr_datapath_one_cycle";

defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_11__a_db_in_bypass = (db_pin_11_in_bypass == "true") ? "db_in_bypass" : "db_in_not_bypass";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_11__a_dbc_sel = ((db_pin_11_mode == "ac_core") || (db_pin_11_mode == "dq_mode") || (db_pin_11_mode == "dm_mode") || (db_pin_11_mode == "dqs_mode")) ? "sel_core" : "sel_dbc";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_11__a_oe_datapath_mod = "oe_datapath_one_cycle";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_11__a_prbs = "not_sel_prbs";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_11__a_wdb_bypass  = (db_pin_11_mode == "dq_wdb_mode" || db_pin_11_mode == "dm_wdb_mode" || db_pin_11_mode == "dqs_wdb_mode" || db_pin_11_mode == "ac_hmc") ? "wdb_not_bypass" : "wdb_bypass";
defparam u_io_12_lane_bcm.data_buffer__rdwr_buffer_inst_11__a_wr_datapath_mod = "wr_datapath_one_cycle";

defparam u_io_12_lane_bcm.data_buffer__a_rb_afi_wlat_vlu = db_afi_wlat_vlu;
defparam u_io_12_lane_bcm.data_buffer__a_rb_afi_rlat_vlu = db_afi_rlat_vlu;
defparam u_io_12_lane_bcm.data_buffer__a_rb_avl_ena = (avl_ena == "true") ? "avl_enable" : "avl_disable";
defparam u_io_12_lane_bcm.data_buffer__a_rb_bc_id_ena = (avl_ena == "true") ? "bc_enable" : "bc_disable";
defparam u_io_12_lane_bcm.data_buffer__a_rb_crc_dq0 = (db_crc_dq0 == 0)  ? "crc_dq0_pin0"  :
                                                      (db_crc_dq0 == 1)  ? "crc_dq0_pin1"  :
                                                      (db_crc_dq0 == 2)  ? "crc_dq0_pin2"  :
                                                      (db_crc_dq0 == 3)  ? "crc_dq0_pin3"  :
                                                      (db_crc_dq0 == 4)  ? "crc_dq0_pin4"  :
                                                      (db_crc_dq0 == 5)  ? "crc_dq0_pin5"  :
                                                      (db_crc_dq0 == 6)  ? "crc_dq0_pin6"  :
                                                      (db_crc_dq0 == 7)  ? "crc_dq0_pin7"  :
                                                      (db_crc_dq0 == 8)  ? "crc_dq0_pin8"  :
                                                      (db_crc_dq0 == 9)  ? "crc_dq0_pin9"  :
                                                      (db_crc_dq0 == 10) ? "crc_dq0_pin10" :
                                                                           "crc_dq0_pin11" ;
defparam u_io_12_lane_bcm.data_buffer__a_rb_crc_dq1 = (db_crc_dq1 == 0)  ? "crc_dq1_pin0"  :
                                                      (db_crc_dq1 == 1)  ? "crc_dq1_pin1"  :
                                                      (db_crc_dq1 == 2)  ? "crc_dq1_pin2"  :
                                                      (db_crc_dq1 == 3)  ? "crc_dq1_pin3"  :
                                                      (db_crc_dq1 == 4)  ? "crc_dq1_pin4"  :
                                                      (db_crc_dq1 == 5)  ? "crc_dq1_pin5"  :
                                                      (db_crc_dq1 == 6)  ? "crc_dq1_pin6"  :
                                                      (db_crc_dq1 == 7)  ? "crc_dq1_pin7"  :
                                                      (db_crc_dq1 == 8)  ? "crc_dq1_pin8"  :
                                                      (db_crc_dq1 == 9)  ? "crc_dq1_pin9"  :
                                                      (db_crc_dq1 == 10) ? "crc_dq1_pin10" :
                                                                           "crc_dq1_pin11" ;
defparam u_io_12_lane_bcm.data_buffer__a_rb_crc_dq2 = (db_crc_dq2 == 0)  ? "crc_dq2_pin0"  :
                                                      (db_crc_dq2 == 1)  ? "crc_dq2_pin1"  :
                                                      (db_crc_dq2 == 2)  ? "crc_dq2_pin2"  :
                                                      (db_crc_dq2 == 3)  ? "crc_dq2_pin3"  :
                                                      (db_crc_dq2 == 4)  ? "crc_dq2_pin4"  :
                                                      (db_crc_dq2 == 5)  ? "crc_dq2_pin5"  :
                                                      (db_crc_dq2 == 6)  ? "crc_dq2_pin6"  :
                                                      (db_crc_dq2 == 7)  ? "crc_dq2_pin7"  :
                                                      (db_crc_dq2 == 8)  ? "crc_dq2_pin8"  :
                                                      (db_crc_dq2 == 9)  ? "crc_dq2_pin9"  :
                                                      (db_crc_dq2 == 10) ? "crc_dq2_pin10" :
                                                                           "crc_dq2_pin11" ;
defparam u_io_12_lane_bcm.data_buffer__a_rb_crc_dq3 = (db_crc_dq3 == 0)  ? "crc_dq3_pin0"  :
                                                      (db_crc_dq3 == 1)  ? "crc_dq3_pin1"  :
                                                      (db_crc_dq3 == 2)  ? "crc_dq3_pin2"  :
                                                      (db_crc_dq3 == 3)  ? "crc_dq3_pin3"  :
                                                      (db_crc_dq3 == 4)  ? "crc_dq3_pin4"  :
                                                      (db_crc_dq3 == 5)  ? "crc_dq3_pin5"  :
                                                      (db_crc_dq3 == 6)  ? "crc_dq3_pin6"  :
                                                      (db_crc_dq3 == 7)  ? "crc_dq3_pin7"  :
                                                      (db_crc_dq3 == 8)  ? "crc_dq3_pin8"  :
                                                      (db_crc_dq3 == 9)  ? "crc_dq3_pin9"  :
                                                      (db_crc_dq3 == 10) ? "crc_dq3_pin10" :
                                                                           "crc_dq3_pin11" ;
defparam u_io_12_lane_bcm.data_buffer__a_rb_crc_dq4 = (db_crc_dq4 == 0)  ? "crc_dq4_pin0"  :
                                                      (db_crc_dq4 == 1)  ? "crc_dq4_pin1"  :
                                                      (db_crc_dq4 == 2)  ? "crc_dq4_pin2"  :
                                                      (db_crc_dq4 == 3)  ? "crc_dq4_pin3"  :
                                                      (db_crc_dq4 == 4)  ? "crc_dq4_pin4"  :
                                                      (db_crc_dq4 == 5)  ? "crc_dq4_pin5"  :
                                                      (db_crc_dq4 == 6)  ? "crc_dq4_pin6"  :
                                                      (db_crc_dq4 == 7)  ? "crc_dq4_pin7"  :
                                                      (db_crc_dq4 == 8)  ? "crc_dq4_pin8"  :
                                                      (db_crc_dq4 == 9)  ? "crc_dq4_pin9"  :
                                                      (db_crc_dq4 == 10) ? "crc_dq4_pin10" :
                                                                           "crc_dq4_pin11" ;
defparam u_io_12_lane_bcm.data_buffer__a_rb_crc_dq5 = (db_crc_dq5 == 0)  ? "crc_dq5_pin0"  :
                                                      (db_crc_dq5 == 1)  ? "crc_dq5_pin1"  :
                                                      (db_crc_dq5 == 2)  ? "crc_dq5_pin2"  :
                                                      (db_crc_dq5 == 3)  ? "crc_dq5_pin3"  :
                                                      (db_crc_dq5 == 4)  ? "crc_dq5_pin4"  :
                                                      (db_crc_dq5 == 5)  ? "crc_dq5_pin5"  :
                                                      (db_crc_dq5 == 6)  ? "crc_dq5_pin6"  :
                                                      (db_crc_dq5 == 7)  ? "crc_dq5_pin7"  :
                                                      (db_crc_dq5 == 8)  ? "crc_dq5_pin8"  :
                                                      (db_crc_dq5 == 9)  ? "crc_dq5_pin9"  :
                                                      (db_crc_dq5 == 10) ? "crc_dq5_pin10" :
                                                                           "crc_dq5_pin11" ;
defparam u_io_12_lane_bcm.data_buffer__a_rb_crc_dq6 = (db_crc_dq6 == 0)  ? "crc_dq6_pin0"  :
                                                      (db_crc_dq6 == 1)  ? "crc_dq6_pin1"  :
                                                      (db_crc_dq6 == 2)  ? "crc_dq6_pin2"  :
                                                      (db_crc_dq6 == 3)  ? "crc_dq6_pin3"  :
                                                      (db_crc_dq6 == 4)  ? "crc_dq6_pin4"  :
                                                      (db_crc_dq6 == 5)  ? "crc_dq6_pin5"  :
                                                      (db_crc_dq6 == 6)  ? "crc_dq6_pin6"  :
                                                      (db_crc_dq6 == 7)  ? "crc_dq6_pin7"  :
                                                      (db_crc_dq6 == 8)  ? "crc_dq6_pin8"  :
                                                      (db_crc_dq6 == 9)  ? "crc_dq6_pin9"  :
                                                      (db_crc_dq6 == 10) ? "crc_dq6_pin10" :
                                                                           "crc_dq6_pin11" ;
defparam u_io_12_lane_bcm.data_buffer__a_rb_crc_dq7 = (db_crc_dq7 == 0)  ? "crc_dq7_pin0"  :
                                                      (db_crc_dq7 == 1)  ? "crc_dq7_pin1"  :
                                                      (db_crc_dq7 == 2)  ? "crc_dq7_pin2"  :
                                                      (db_crc_dq7 == 3)  ? "crc_dq7_pin3"  :
                                                      (db_crc_dq7 == 4)  ? "crc_dq7_pin4"  :
                                                      (db_crc_dq7 == 5)  ? "crc_dq7_pin5"  :
                                                      (db_crc_dq7 == 6)  ? "crc_dq7_pin6"  :
                                                      (db_crc_dq7 == 7)  ? "crc_dq7_pin7"  :
                                                      (db_crc_dq7 == 8)  ? "crc_dq7_pin8"  :
                                                      (db_crc_dq7 == 9)  ? "crc_dq7_pin9"  :
                                                      (db_crc_dq7 == 10) ? "crc_dq7_pin10" :
                                                                           "crc_dq7_pin11" ;
defparam u_io_12_lane_bcm.data_buffer__a_rb_crc_dq8 = (db_crc_dq8 == 0)  ? "crc_dq8_pin0"  :
                                                      (db_crc_dq8 == 1)  ? "crc_dq8_pin1"  :
                                                      (db_crc_dq8 == 2)  ? "crc_dq8_pin2"  :
                                                      (db_crc_dq8 == 3)  ? "crc_dq8_pin3"  :
                                                      (db_crc_dq8 == 4)  ? "crc_dq8_pin4"  :
                                                      (db_crc_dq8 == 5)  ? "crc_dq8_pin5"  :
                                                      (db_crc_dq8 == 6)  ? "crc_dq8_pin6"  :
                                                      (db_crc_dq8 == 7)  ? "crc_dq8_pin7"  :
                                                      (db_crc_dq8 == 8)  ? "crc_dq8_pin8"  :
                                                      (db_crc_dq8 == 9)  ? "crc_dq8_pin9"  :
                                                      (db_crc_dq8 == 10) ? "crc_dq8_pin10" :
                                                                           "crc_dq8_pin11" ;
defparam u_io_12_lane_bcm.data_buffer__a_rb_crc_en  = db_crc_en;
defparam u_io_12_lane_bcm.data_buffer__a_rb_data_alignment_mode = db_data_alignment_mode;
defparam u_io_12_lane_bcm.data_buffer__a_rb_db2core_registered = (db_db2core_registered == "true") ? "registered" : "not_registered";
defparam u_io_12_lane_bcm.data_buffer__a_rb_mrnk_read_registered  = (db_core_or_hmc2db_registered == "true") ? "mrnk_read_registered" : "mrnk_read_bypass";
defparam u_io_12_lane_bcm.data_buffer__a_rb_mrnk_write_registered = (db_core_or_hmc2db_registered == "true") ? "mrnk_write_registered" : "mrnk_write_bypass";
defparam u_io_12_lane_bcm.data_buffer__a_rb_rdata_en_full_registered = (db_core_or_hmc2db_registered == "true") ? "rdata_en_full_registered" : "rdata_en_full_bypass";
defparam u_io_12_lane_bcm.data_buffer__a_rb_seq_rd_en_full_pipeline = db_seq_rd_en_full_pipeline;
defparam u_io_12_lane_bcm.data_buffer__a_rb_dbc_wb_reserved_entry = dbc_wb_reserved_entry;
defparam u_io_12_lane_bcm.data_buffer__a_rb_dbi_wr_en  = (db_dbi_wr_en == "true") ? "dbi_wr_enable" : "dbi_wr_disable";
defparam u_io_12_lane_bcm.data_buffer__a_rb_dbi_rd_en  = (db_dbi_rd_en == "true") ? "dbi_rd_enable" : "dbi_rd_disable";
defparam u_io_12_lane_bcm.data_buffer__a_rb_dbi_sel = (db_dbi_sel == 11) ? "dbi_dq11" :
                                                      (db_dbi_sel == 10) ? "dbi_dq10" :
                                                      (db_dbi_sel == 9 ) ? "dbi_dq9"  :
                                                      (db_dbi_sel == 8 ) ? "dbi_dq8"  :
                                                      (db_dbi_sel == 7 ) ? "dbi_dq7"  :
                                                      (db_dbi_sel == 6 ) ? "dbi_dq6"  :
                                                      (db_dbi_sel == 5 ) ? "dbi_dq5"  :
                                                      (db_dbi_sel == 4 ) ? "dbi_dq4"  :
                                                      (db_dbi_sel == 3 ) ? "dbi_dq3"  :
                                                      (db_dbi_sel == 2 ) ? "dbi_dq2"  :
                                                      (db_dbi_sel == 1 ) ? "dbi_dq1"  :
                                                                           "dbi_dq0"  ;
defparam u_io_12_lane_bcm. data_buffer__a_rb_dft_hmc_phy = "lbk_hmc_disable";
defparam u_io_12_lane_bcm. data_buffer__a_rb_dft_lbk_phy = "lbk_phy_disable";
defparam u_io_12_lane_bcm. data_buffer__a_rb_dft_mux_speed_in = "sel_speed_in0";
defparam u_io_12_lane_bcm. data_buffer__a_rb_dft_mux_speed_out = "sel_speed_out0";
defparam u_io_12_lane_bcm. data_buffer__a_rb_dft_prbs_mode = "oe_0_dq_0";
defparam u_io_12_lane_bcm.data_buffer__a_rb_hmc_or_core = db_hmc_or_core;
defparam u_io_12_lane_bcm.data_buffer__a_rb_phy_clk0_ena = "phy_clk0_ena";
defparam u_io_12_lane_bcm.data_buffer__a_rb_phy_clk1_ena = "phy_clk1_ena";
defparam u_io_12_lane_bcm.data_buffer__a_rb_preamble_mode = db_preamble_mode;
defparam u_io_12_lane_bcm.data_buffer__a_rb_ptr_pipeline = (db_ptr_pipeline_depth == 4) ? "ptr_four_cycle" :
                                                           (db_ptr_pipeline_depth == 3) ? "ptr_three_cycle" :
                                                           (db_ptr_pipeline_depth == 2) ? "ptr_two_cycle" :
                                                           (db_ptr_pipeline_depth == 1) ? "ptr_one_cycle" :
                                                                                          "ptr_bypass";
defparam u_io_12_lane_bcm.data_buffer__a_rb_qr_or_hr = (mode_rate_in == "in_rate_1_4") ? "qr" : "hr";
defparam u_io_12_lane_bcm.data_buffer__a_rb_reset_auto_release = db_reset_auto_release;
defparam u_io_12_lane_bcm.data_buffer__a_rb_rwlat_mode = db_rwlat_mode;
defparam u_io_12_lane_bcm.data_buffer__a_rb_sel_core_clk = (dbc_core_clk_sel == 0) ? "phy_clk0" : "phy_clk1";
defparam u_io_12_lane_bcm.data_buffer__a_rb_tile_id = avl_base_addr;
defparam u_io_12_lane_bcm.data_buffer__a_rb_x4_or_x8_or_x9 = db_crc_x4_or_x8_or_x9;
defparam u_io_12_lane_bcm.data_buffer__a_rb_burst_length_mode = (dqs_lgc_burst_length == "burst_length_2") ? "bl2" :
                                                                (dqs_lgc_burst_length == "burst_length_4") ? "bl4" :
                                                                                                             "bl8" ;

defparam u_io_12_lane_bcm.ioereg_top_0___ioereg_pnr_x2__a_ddr2_oeb = "ddr3_preamble";
defparam u_io_12_lane_bcm.ioereg_top_0___ioereg_pnr_x2__a_dpa_enable = "dpa_disabled";
defparam u_io_12_lane_bcm.ioereg_top_0___ioereg_pnr_x2__a_lock_speed = 4'hF;
defparam u_io_12_lane_bcm.ioereg_top_0___ioereg_pnr_x2__a_power_down = "power_on";
defparam u_io_12_lane_bcm.ioereg_top_0___ioereg_pnr_x2__a_power_down_0 = "power_on_0";
defparam u_io_12_lane_bcm.ioereg_top_0___ioereg_pnr_x2__a_power_down_1 = "power_on_1";
defparam u_io_12_lane_bcm.ioereg_top_0___ioereg_pnr_x2__a_power_down_2 = "power_on_2";
defparam u_io_12_lane_bcm.ioereg_top_0___ioereg_pnr_x2__a_sync_control = "sync_enabled";
defparam u_io_12_lane_bcm.ioereg_top_0___ioereg_pnr_x2__ioereg_pnr_0__a_dfx_mode     = "dfx_disabled";
defparam u_io_12_lane_bcm.ioereg_top_0___ioereg_pnr_x2__ioereg_pnr_0__a_dq_select    = (pin_0_data_in_mode == "sstl_in")                     ? "dq_sstl_in"                     :
                                                                                       (pin_0_data_in_mode == "loopback_in")                 ? "dq_loopback_in"                 :
                                                                                       (pin_0_data_in_mode == "xor_loopback_in")             ? "dq_xor_loopback_in"             :
                                                                                       (pin_0_data_in_mode == "differential_in")             ? "dq_differential_in"             :
                                                                                       (pin_0_data_in_mode == "differential_in_avl_out")     ? "dq_differential_in_avl_out"     :
                                                                                       (pin_0_data_in_mode == "differential_in_x12_out")     ? "dq_differential_in_x12_out"     :
                                                                                       (pin_0_data_in_mode == "differential_in_avl_x12_out") ? "dq_differential_in_avl_x12_out" :
                                                                                                                                               "dq_disabled"                    ;
defparam u_io_12_lane_bcm.ioereg_top_0___ioereg_pnr_x2__ioereg_pnr_0__a_dqs_select   = (pin_0_dqs_x4_mode == "dqs_x4_a") ? "dqs_sampler_a" :
                                                                                       (pin_0_dqs_x4_mode == "dqs_x4_b") ? "dqs_sampler_b" :
                                                                                       (dqs_lgc_dqs_b_en == "true") ? "dqs_sampler_b_a_rise" : "dqs_sampler_a";
defparam u_io_12_lane_bcm.ioereg_top_0___ioereg_pnr_x2__ioereg_pnr_0__a_dynoct       = (pin_0_oct_mode == "dynamic")   ? "oct_enabled" : "oct_disabled" ;
defparam u_io_12_lane_bcm.ioereg_top_0___ioereg_pnr_x2__ioereg_pnr_0__a_initial_out  = pin_0_initial_out;
defparam u_io_12_lane_bcm.ioereg_top_0___ioereg_pnr_x2__ioereg_pnr_0__a_mode_ddr     = pin_0_mode_ddr;
defparam u_io_12_lane_bcm.ioereg_top_0___ioereg_pnr_x2__ioereg_pnr_0__a_octrt        = (pin_0_oct_mode == "static_on") ? "static_oct_on" : "static_oct_off" ;
defparam u_io_12_lane_bcm.ioereg_top_0___ioereg_pnr_x2__ioereg_pnr_0__a_output_phase = pin_0_output_phase[11:0];
defparam u_io_12_lane_bcm.ioereg_top_0___ioereg_pnr_x2__ioereg_pnr_0__a_gpio_differential = pin_0_output_phase[12];

defparam u_io_12_lane_bcm.ioereg_top_0___ioereg_pnr_x2__ioereg_pnr_1__a_dfx_mode     = "dfx_disabled";
defparam u_io_12_lane_bcm.ioereg_top_0___ioereg_pnr_x2__ioereg_pnr_1__a_dq_select    = (pin_1_data_in_mode == "sstl_in")                     ? "dq_sstl_in"                     :
                                                                                       (pin_1_data_in_mode == "loopback_in")                 ? "dq_loopback_in"                 :
                                                                                       (pin_1_data_in_mode == "xor_loopback_in")             ? "dq_xor_loopback_in"             :
                                                                                       (pin_1_data_in_mode == "differential_in")             ? "dq_differential_in"             :
                                                                                       (pin_1_data_in_mode == "differential_in_avl_out")     ? "dq_differential_in_avl_out"     :
                                                                                       (pin_1_data_in_mode == "differential_in_x12_out")     ? "dq_differential_in_x12_out"     :
                                                                                       (pin_1_data_in_mode == "differential_in_avl_x12_out") ? "dq_differential_in_avl_x12_out" :
                                                                                                                                               "dq_disabled"                    ;
defparam u_io_12_lane_bcm.ioereg_top_0___ioereg_pnr_x2__ioereg_pnr_1__a_dqs_select   = (pin_1_dqs_x4_mode == "dqs_x4_a") ? "dqs_sampler_a" :
                                                                                       (pin_1_dqs_x4_mode == "dqs_x4_b") ? "dqs_sampler_b" :
                                                                                       (dqs_lgc_dqs_b_en == "true") ? "dqs_sampler_b_a_rise" : "dqs_sampler_a";
defparam u_io_12_lane_bcm.ioereg_top_0___ioereg_pnr_x2__ioereg_pnr_1__a_dynoct       = (pin_1_oct_mode == "dynamic")   ? "oct_enabled" : "oct_disabled" ;
defparam u_io_12_lane_bcm.ioereg_top_0___ioereg_pnr_x2__ioereg_pnr_1__a_initial_out  = pin_1_initial_out;
defparam u_io_12_lane_bcm.ioereg_top_0___ioereg_pnr_x2__ioereg_pnr_1__a_mode_ddr     = pin_1_mode_ddr;
defparam u_io_12_lane_bcm.ioereg_top_0___ioereg_pnr_x2__ioereg_pnr_1__a_octrt        = (pin_1_oct_mode == "static_on") ? "static_oct_on" : "static_oct_off" ;
defparam u_io_12_lane_bcm.ioereg_top_0___ioereg_pnr_x2__ioereg_pnr_1__a_output_phase = pin_1_output_phase[11:0];
defparam u_io_12_lane_bcm.ioereg_top_0___ioereg_pnr_x2__ioereg_pnr_1__a_gpio_differential = pin_1_output_phase[12];

defparam u_io_12_lane_bcm.ioereg_top_1___ioereg_pnr_x2__a_ddr2_oeb = "ddr3_preamble";
defparam u_io_12_lane_bcm.ioereg_top_1___ioereg_pnr_x2__a_dpa_enable = "dpa_disabled";
defparam u_io_12_lane_bcm.ioereg_top_1___ioereg_pnr_x2__a_lock_speed = 4'hF;
defparam u_io_12_lane_bcm.ioereg_top_1___ioereg_pnr_x2__a_power_down = "power_on";
defparam u_io_12_lane_bcm.ioereg_top_1___ioereg_pnr_x2__a_power_down_0 = "power_on_0";
defparam u_io_12_lane_bcm.ioereg_top_1___ioereg_pnr_x2__a_power_down_1 = "power_on_1";
defparam u_io_12_lane_bcm.ioereg_top_1___ioereg_pnr_x2__a_power_down_2 = "power_on_2";
defparam u_io_12_lane_bcm.ioereg_top_1___ioereg_pnr_x2__a_sync_control = "sync_enabled";
defparam u_io_12_lane_bcm.ioereg_top_1___ioereg_pnr_x2__ioereg_pnr_0__a_dfx_mode     = "dfx_disabled";
defparam u_io_12_lane_bcm.ioereg_top_1___ioereg_pnr_x2__ioereg_pnr_0__a_dq_select    = (pin_2_data_in_mode == "sstl_in")                     ? "dq_sstl_in"                     :
                                                                                       (pin_2_data_in_mode == "loopback_in")                 ? "dq_loopback_in"                 :
                                                                                       (pin_2_data_in_mode == "xor_loopback_in")             ? "dq_xor_loopback_in"             :
                                                                                       (pin_2_data_in_mode == "differential_in")             ? "dq_differential_in"             :
                                                                                       (pin_2_data_in_mode == "differential_in_avl_out")     ? "dq_differential_in_avl_out"     :
                                                                                       (pin_2_data_in_mode == "differential_in_x12_out")     ? "dq_differential_in_x12_out"     :
                                                                                       (pin_2_data_in_mode == "differential_in_avl_x12_out") ? "dq_differential_in_avl_x12_out" :
                                                                                                                                               "dq_disabled"                    ;
defparam u_io_12_lane_bcm.ioereg_top_1___ioereg_pnr_x2__ioereg_pnr_0__a_dqs_select   = (pin_2_dqs_x4_mode == "dqs_x4_a") ? "dqs_sampler_a" :
                                                                                       (pin_2_dqs_x4_mode == "dqs_x4_b") ? "dqs_sampler_b" :
                                                                                       (dqs_lgc_dqs_b_en == "true") ? "dqs_sampler_b_a_rise" : "dqs_sampler_a";
defparam u_io_12_lane_bcm.ioereg_top_1___ioereg_pnr_x2__ioereg_pnr_0__a_dynoct       = (pin_2_oct_mode == "dynamic")   ? "oct_enabled" : "oct_disabled" ;
defparam u_io_12_lane_bcm.ioereg_top_1___ioereg_pnr_x2__ioereg_pnr_0__a_initial_out  = pin_2_initial_out;
defparam u_io_12_lane_bcm.ioereg_top_1___ioereg_pnr_x2__ioereg_pnr_0__a_mode_ddr     = pin_2_mode_ddr;
defparam u_io_12_lane_bcm.ioereg_top_1___ioereg_pnr_x2__ioereg_pnr_0__a_octrt        = (pin_2_oct_mode == "static_on") ? "static_oct_on" : "static_oct_off" ;
defparam u_io_12_lane_bcm.ioereg_top_1___ioereg_pnr_x2__ioereg_pnr_0__a_output_phase = pin_2_output_phase[11:0];
defparam u_io_12_lane_bcm.ioereg_top_1___ioereg_pnr_x2__ioereg_pnr_0__a_gpio_differential = pin_2_output_phase[12];

defparam u_io_12_lane_bcm.ioereg_top_1___ioereg_pnr_x2__ioereg_pnr_1__a_dfx_mode     = "dfx_disabled";
defparam u_io_12_lane_bcm.ioereg_top_1___ioereg_pnr_x2__ioereg_pnr_1__a_dq_select    = (pin_3_data_in_mode == "sstl_in")                     ? "dq_sstl_in"                     :
                                                                                       (pin_3_data_in_mode == "loopback_in")                 ? "dq_loopback_in"                 :
                                                                                       (pin_3_data_in_mode == "xor_loopback_in")             ? "dq_xor_loopback_in"             :
                                                                                       (pin_3_data_in_mode == "differential_in")             ? "dq_differential_in"             :
                                                                                       (pin_3_data_in_mode == "differential_in_avl_out")     ? "dq_differential_in_avl_out"     :
                                                                                       (pin_3_data_in_mode == "differential_in_x12_out")     ? "dq_differential_in_x12_out"     :
                                                                                       (pin_3_data_in_mode == "differential_in_avl_x12_out") ? "dq_differential_in_avl_x12_out" :
                                                                                                                                               "dq_disabled"                    ;
defparam u_io_12_lane_bcm.ioereg_top_1___ioereg_pnr_x2__ioereg_pnr_1__a_dqs_select   = (pin_3_dqs_x4_mode == "dqs_x4_a") ? "dqs_sampler_a" :
                                                                                       (pin_3_dqs_x4_mode == "dqs_x4_b") ? "dqs_sampler_b" :
                                                                                       (dqs_lgc_dqs_b_en == "true") ? "dqs_sampler_b_a_rise" : "dqs_sampler_a";
defparam u_io_12_lane_bcm.ioereg_top_1___ioereg_pnr_x2__ioereg_pnr_1__a_dynoct       = (pin_3_oct_mode == "dynamic")   ? "oct_enabled" : "oct_disabled" ;
defparam u_io_12_lane_bcm.ioereg_top_1___ioereg_pnr_x2__ioereg_pnr_1__a_initial_out  = pin_3_initial_out;
defparam u_io_12_lane_bcm.ioereg_top_1___ioereg_pnr_x2__ioereg_pnr_1__a_mode_ddr     = pin_3_mode_ddr;
defparam u_io_12_lane_bcm.ioereg_top_1___ioereg_pnr_x2__ioereg_pnr_1__a_octrt        = (pin_3_oct_mode == "static_on") ? "static_oct_on" : "static_oct_off" ;
defparam u_io_12_lane_bcm.ioereg_top_1___ioereg_pnr_x2__ioereg_pnr_1__a_output_phase = pin_3_output_phase[11:0];
defparam u_io_12_lane_bcm.ioereg_top_1___ioereg_pnr_x2__ioereg_pnr_1__a_gpio_differential = pin_3_output_phase[12];

defparam u_io_12_lane_bcm.ioereg_top_2___ioereg_pnr_x2__a_ddr2_oeb = "ddr3_preamble";
defparam u_io_12_lane_bcm.ioereg_top_2___ioereg_pnr_x2__a_dpa_enable = "dpa_disabled";
defparam u_io_12_lane_bcm.ioereg_top_2___ioereg_pnr_x2__a_lock_speed = 4'hF;
defparam u_io_12_lane_bcm.ioereg_top_2___ioereg_pnr_x2__a_power_down = "power_on";
defparam u_io_12_lane_bcm.ioereg_top_2___ioereg_pnr_x2__a_power_down_0 = "power_on_0";
defparam u_io_12_lane_bcm.ioereg_top_2___ioereg_pnr_x2__a_power_down_1 = "power_on_1";
defparam u_io_12_lane_bcm.ioereg_top_2___ioereg_pnr_x2__a_power_down_2 = "power_on_2";
defparam u_io_12_lane_bcm.ioereg_top_2___ioereg_pnr_x2__a_sync_control = "sync_enabled";
defparam u_io_12_lane_bcm.ioereg_top_2___ioereg_pnr_x2__ioereg_pnr_0__a_dfx_mode     = "dfx_disabled";
defparam u_io_12_lane_bcm.ioereg_top_2___ioereg_pnr_x2__ioereg_pnr_0__a_dq_select    = (pin_4_data_in_mode == "sstl_in")                     ? "dq_sstl_in"                     :
                                                                                       (pin_4_data_in_mode == "loopback_in")                 ? "dq_loopback_in"                 :
                                                                                       (pin_4_data_in_mode == "xor_loopback_in")             ? "dq_xor_loopback_in"             :
                                                                                       (pin_4_data_in_mode == "differential_in")             ? "dq_differential_in"             :
                                                                                       (pin_4_data_in_mode == "differential_in_avl_out")     ? "dq_differential_in_avl_out"     :
                                                                                       (pin_4_data_in_mode == "differential_in_x12_out")     ? "dq_differential_in_x12_out"     :
                                                                                       (pin_4_data_in_mode == "differential_in_avl_x12_out") ? "dq_differential_in_avl_x12_out" :
                                                                                                                                               "dq_disabled"                    ;
defparam u_io_12_lane_bcm.ioereg_top_2___ioereg_pnr_x2__ioereg_pnr_0__a_dqs_select   = (pin_4_dqs_x4_mode == "dqs_x4_a") ? "dqs_sampler_a" :
                                                                                       (pin_4_dqs_x4_mode == "dqs_x4_b") ? "dqs_sampler_b" :
                                                                                       (dqs_lgc_dqs_b_en == "true") ? "dqs_sampler_b_a_rise" : "dqs_sampler_a";
defparam u_io_12_lane_bcm.ioereg_top_2___ioereg_pnr_x2__ioereg_pnr_0__a_dynoct       = (pin_4_oct_mode == "dynamic")   ? "oct_enabled" : "oct_disabled" ;
defparam u_io_12_lane_bcm.ioereg_top_2___ioereg_pnr_x2__ioereg_pnr_0__a_initial_out  = pin_4_initial_out;
defparam u_io_12_lane_bcm.ioereg_top_2___ioereg_pnr_x2__ioereg_pnr_0__a_mode_ddr     = pin_4_mode_ddr;
defparam u_io_12_lane_bcm.ioereg_top_2___ioereg_pnr_x2__ioereg_pnr_0__a_octrt        = (pin_4_oct_mode == "static_on") ? "static_oct_on" : "static_oct_off" ;
defparam u_io_12_lane_bcm.ioereg_top_2___ioereg_pnr_x2__ioereg_pnr_0__a_output_phase = pin_4_output_phase[11:0];
defparam u_io_12_lane_bcm.ioereg_top_2___ioereg_pnr_x2__ioereg_pnr_0__a_gpio_differential = pin_4_output_phase[12];

defparam u_io_12_lane_bcm.ioereg_top_2___ioereg_pnr_x2__ioereg_pnr_1__a_dfx_mode     = "dfx_disabled";
defparam u_io_12_lane_bcm.ioereg_top_2___ioereg_pnr_x2__ioereg_pnr_1__a_dq_select    = (pin_5_data_in_mode == "sstl_in")                     ? "dq_sstl_in"                     :
                                                                                       (pin_5_data_in_mode == "loopback_in")                 ? "dq_loopback_in"                 :
                                                                                       (pin_5_data_in_mode == "xor_loopback_in")             ? "dq_xor_loopback_in"             :
                                                                                       (pin_5_data_in_mode == "differential_in")             ? "dq_differential_in"             :
                                                                                       (pin_5_data_in_mode == "differential_in_avl_out")     ? "dq_differential_in_avl_out"     :
                                                                                       (pin_5_data_in_mode == "differential_in_x12_out")     ? "dq_differential_in_x12_out"     :
                                                                                       (pin_5_data_in_mode == "differential_in_avl_x12_out") ? "dq_differential_in_avl_x12_out" :
                                                                                                                                               "dq_disabled"                    ;
defparam u_io_12_lane_bcm.ioereg_top_2___ioereg_pnr_x2__ioereg_pnr_1__a_dqs_select   = (pin_5_dqs_x4_mode == "dqs_x4_a") ? "dqs_sampler_a" :
                                                                                       (pin_5_dqs_x4_mode == "dqs_x4_b") ? "dqs_sampler_b" :
                                                                                       (dqs_lgc_dqs_b_en == "true") ? "dqs_sampler_b_a_rise" : "dqs_sampler_a";
defparam u_io_12_lane_bcm.ioereg_top_2___ioereg_pnr_x2__ioereg_pnr_1__a_dynoct       = (pin_5_oct_mode == "dynamic")   ? "oct_enabled" : "oct_disabled" ;
defparam u_io_12_lane_bcm.ioereg_top_2___ioereg_pnr_x2__ioereg_pnr_1__a_initial_out  = pin_5_initial_out;
defparam u_io_12_lane_bcm.ioereg_top_2___ioereg_pnr_x2__ioereg_pnr_1__a_mode_ddr     = pin_5_mode_ddr;
defparam u_io_12_lane_bcm.ioereg_top_2___ioereg_pnr_x2__ioereg_pnr_1__a_octrt        = (pin_5_oct_mode == "static_on") ? "static_oct_on" : "static_oct_off" ;
defparam u_io_12_lane_bcm.ioereg_top_2___ioereg_pnr_x2__ioereg_pnr_1__a_output_phase = pin_5_output_phase[11:0];
defparam u_io_12_lane_bcm.ioereg_top_2___ioereg_pnr_x2__ioereg_pnr_1__a_gpio_differential = pin_5_output_phase[12];

defparam u_io_12_lane_bcm.ioereg_top_3___ioereg_pnr_x2__a_ddr2_oeb = "ddr3_preamble";
defparam u_io_12_lane_bcm.ioereg_top_3___ioereg_pnr_x2__a_dpa_enable = "dpa_disabled";
defparam u_io_12_lane_bcm.ioereg_top_3___ioereg_pnr_x2__a_lock_speed = 4'hF;
defparam u_io_12_lane_bcm.ioereg_top_3___ioereg_pnr_x2__a_power_down = "power_on";
defparam u_io_12_lane_bcm.ioereg_top_3___ioereg_pnr_x2__a_power_down_0 = "power_on_0";
defparam u_io_12_lane_bcm.ioereg_top_3___ioereg_pnr_x2__a_power_down_1 = "power_on_1";
defparam u_io_12_lane_bcm.ioereg_top_3___ioereg_pnr_x2__a_power_down_2 = "power_on_2";
defparam u_io_12_lane_bcm.ioereg_top_3___ioereg_pnr_x2__a_sync_control = "sync_enabled";
defparam u_io_12_lane_bcm.ioereg_top_3___ioereg_pnr_x2__ioereg_pnr_0__a_dfx_mode     = "dfx_disabled";
defparam u_io_12_lane_bcm.ioereg_top_3___ioereg_pnr_x2__ioereg_pnr_0__a_dq_select    = (pin_6_data_in_mode == "sstl_in")                     ? "dq_sstl_in"                     :
                                                                                       (pin_6_data_in_mode == "loopback_in")                 ? "dq_loopback_in"                 :
                                                                                       (pin_6_data_in_mode == "xor_loopback_in")             ? "dq_xor_loopback_in"             :
                                                                                       (pin_6_data_in_mode == "differential_in")             ? "dq_differential_in"             :
                                                                                       (pin_6_data_in_mode == "differential_in_avl_out")     ? "dq_differential_in_avl_out"     :
                                                                                       (pin_6_data_in_mode == "differential_in_x12_out")     ? "dq_differential_in_x12_out"     :
                                                                                       (pin_6_data_in_mode == "differential_in_avl_x12_out") ? "dq_differential_in_avl_x12_out" :
                                                                                                                                               "dq_disabled"                    ;
defparam u_io_12_lane_bcm.ioereg_top_3___ioereg_pnr_x2__ioereg_pnr_0__a_dqs_select   = (pin_6_dqs_x4_mode == "dqs_x4_a") ? "dqs_sampler_a" :
                                                                                       (pin_6_dqs_x4_mode == "dqs_x4_b") ? "dqs_sampler_b" :
                                                                                       (dqs_lgc_dqs_b_en == "true") ? "dqs_sampler_b_a_rise" : "dqs_sampler_a";
defparam u_io_12_lane_bcm.ioereg_top_3___ioereg_pnr_x2__ioereg_pnr_0__a_dynoct       = (pin_6_oct_mode == "dynamic")   ? "oct_enabled" : "oct_disabled" ;
defparam u_io_12_lane_bcm.ioereg_top_3___ioereg_pnr_x2__ioereg_pnr_0__a_initial_out  = pin_6_initial_out;
defparam u_io_12_lane_bcm.ioereg_top_3___ioereg_pnr_x2__ioereg_pnr_0__a_mode_ddr     = pin_6_mode_ddr;
defparam u_io_12_lane_bcm.ioereg_top_3___ioereg_pnr_x2__ioereg_pnr_0__a_octrt        = (pin_6_oct_mode == "static_on") ? "static_oct_on" : "static_oct_off" ;
defparam u_io_12_lane_bcm.ioereg_top_3___ioereg_pnr_x2__ioereg_pnr_0__a_output_phase = pin_6_output_phase[11:0];
defparam u_io_12_lane_bcm.ioereg_top_3___ioereg_pnr_x2__ioereg_pnr_0__a_gpio_differential = pin_6_output_phase[12];

defparam u_io_12_lane_bcm.ioereg_top_3___ioereg_pnr_x2__ioereg_pnr_1__a_dfx_mode     = "dfx_disabled";
defparam u_io_12_lane_bcm.ioereg_top_3___ioereg_pnr_x2__ioereg_pnr_1__a_dq_select    = (pin_7_data_in_mode == "sstl_in")                     ? "dq_sstl_in"                     :
                                                                                       (pin_7_data_in_mode == "loopback_in")                 ? "dq_loopback_in"                 :
                                                                                       (pin_7_data_in_mode == "xor_loopback_in")             ? "dq_xor_loopback_in"             :
                                                                                       (pin_7_data_in_mode == "differential_in")             ? "dq_differential_in"             :
                                                                                       (pin_7_data_in_mode == "differential_in_avl_out")     ? "dq_differential_in_avl_out"     :
                                                                                       (pin_7_data_in_mode == "differential_in_x12_out")     ? "dq_differential_in_x12_out"     :
                                                                                       (pin_7_data_in_mode == "differential_in_avl_x12_out") ? "dq_differential_in_avl_x12_out" :
                                                                                                                                               "dq_disabled"                    ;
defparam u_io_12_lane_bcm.ioereg_top_3___ioereg_pnr_x2__ioereg_pnr_1__a_dqs_select   = (pin_7_dqs_x4_mode == "dqs_x4_a") ? "dqs_sampler_a" :
                                                                                       (pin_7_dqs_x4_mode == "dqs_x4_b") ? "dqs_sampler_b" :
                                                                                       (dqs_lgc_dqs_b_en == "true") ? "dqs_sampler_b_a_rise" : "dqs_sampler_a";
defparam u_io_12_lane_bcm.ioereg_top_3___ioereg_pnr_x2__ioereg_pnr_1__a_dynoct       = (pin_7_oct_mode == "dynamic")   ? "oct_enabled" : "oct_disabled" ;
defparam u_io_12_lane_bcm.ioereg_top_3___ioereg_pnr_x2__ioereg_pnr_1__a_initial_out  = pin_7_initial_out;
defparam u_io_12_lane_bcm.ioereg_top_3___ioereg_pnr_x2__ioereg_pnr_1__a_mode_ddr     = pin_7_mode_ddr;
defparam u_io_12_lane_bcm.ioereg_top_3___ioereg_pnr_x2__ioereg_pnr_1__a_octrt        = (pin_7_oct_mode == "static_on") ? "static_oct_on" : "static_oct_off" ;
defparam u_io_12_lane_bcm.ioereg_top_3___ioereg_pnr_x2__ioereg_pnr_1__a_output_phase = pin_7_output_phase[11:0];
defparam u_io_12_lane_bcm.ioereg_top_3___ioereg_pnr_x2__ioereg_pnr_1__a_gpio_differential = pin_7_output_phase[12];

defparam u_io_12_lane_bcm.ioereg_top_4___ioereg_pnr_x2__a_ddr2_oeb = "ddr3_preamble";
defparam u_io_12_lane_bcm.ioereg_top_4___ioereg_pnr_x2__a_dpa_enable = "dpa_disabled";
defparam u_io_12_lane_bcm.ioereg_top_4___ioereg_pnr_x2__a_lock_speed = 4'hF;
defparam u_io_12_lane_bcm.ioereg_top_4___ioereg_pnr_x2__a_power_down = "power_on";
defparam u_io_12_lane_bcm.ioereg_top_4___ioereg_pnr_x2__a_power_down_0 = "power_on_0";
defparam u_io_12_lane_bcm.ioereg_top_4___ioereg_pnr_x2__a_power_down_1 = "power_on_1";
defparam u_io_12_lane_bcm.ioereg_top_4___ioereg_pnr_x2__a_power_down_2 = "power_on_2";
defparam u_io_12_lane_bcm.ioereg_top_4___ioereg_pnr_x2__a_sync_control = "sync_enabled";
defparam u_io_12_lane_bcm.ioereg_top_4___ioereg_pnr_x2__ioereg_pnr_0__a_dfx_mode     = "dfx_disabled";
defparam u_io_12_lane_bcm.ioereg_top_4___ioereg_pnr_x2__ioereg_pnr_0__a_dq_select    = (pin_8_data_in_mode == "sstl_in")                     ? "dq_sstl_in"                     :
                                                                                       (pin_8_data_in_mode == "loopback_in")                 ? "dq_loopback_in"                 :
                                                                                       (pin_8_data_in_mode == "xor_loopback_in")             ? "dq_xor_loopback_in"             :
                                                                                       (pin_8_data_in_mode == "differential_in")             ? "dq_differential_in"             :
                                                                                       (pin_8_data_in_mode == "differential_in_avl_out")     ? "dq_differential_in_avl_out"     :
                                                                                       (pin_8_data_in_mode == "differential_in_x12_out")     ? "dq_differential_in_x12_out"     :
                                                                                       (pin_8_data_in_mode == "differential_in_avl_x12_out") ? "dq_differential_in_avl_x12_out" :
                                                                                                                                               "dq_disabled"                    ;
defparam u_io_12_lane_bcm.ioereg_top_4___ioereg_pnr_x2__ioereg_pnr_0__a_dqs_select   = (pin_8_dqs_x4_mode == "dqs_x4_a") ? "dqs_sampler_a" :
                                                                                       (pin_8_dqs_x4_mode == "dqs_x4_b") ? "dqs_sampler_b" :
                                                                                       (dqs_lgc_dqs_b_en == "true") ? "dqs_sampler_b_a_rise" : "dqs_sampler_a";
defparam u_io_12_lane_bcm.ioereg_top_4___ioereg_pnr_x2__ioereg_pnr_0__a_dynoct       = (pin_8_oct_mode == "dynamic")   ? "oct_enabled" : "oct_disabled" ;
defparam u_io_12_lane_bcm.ioereg_top_4___ioereg_pnr_x2__ioereg_pnr_0__a_initial_out  = pin_8_initial_out;
defparam u_io_12_lane_bcm.ioereg_top_4___ioereg_pnr_x2__ioereg_pnr_0__a_mode_ddr     = pin_8_mode_ddr;
defparam u_io_12_lane_bcm.ioereg_top_4___ioereg_pnr_x2__ioereg_pnr_0__a_octrt        = (pin_8_oct_mode == "static_on") ? "static_oct_on" : "static_oct_off" ;
defparam u_io_12_lane_bcm.ioereg_top_4___ioereg_pnr_x2__ioereg_pnr_0__a_output_phase = pin_8_output_phase[11:0];
defparam u_io_12_lane_bcm.ioereg_top_4___ioereg_pnr_x2__ioereg_pnr_0__a_gpio_differential = pin_8_output_phase[12];

defparam u_io_12_lane_bcm.ioereg_top_4___ioereg_pnr_x2__ioereg_pnr_1__a_dfx_mode     = "dfx_disabled";
defparam u_io_12_lane_bcm.ioereg_top_4___ioereg_pnr_x2__ioereg_pnr_1__a_dq_select    = (pin_9_data_in_mode == "sstl_in")                     ? "dq_sstl_in"                     :
                                                                                       (pin_9_data_in_mode == "loopback_in")                 ? "dq_loopback_in"                 :
                                                                                       (pin_9_data_in_mode == "xor_loopback_in")             ? "dq_xor_loopback_in"             :
                                                                                       (pin_9_data_in_mode == "differential_in")             ? "dq_differential_in"             :
                                                                                       (pin_9_data_in_mode == "differential_in_avl_out")     ? "dq_differential_in_avl_out"     :
                                                                                       (pin_9_data_in_mode == "differential_in_x12_out")     ? "dq_differential_in_x12_out"     :
                                                                                       (pin_9_data_in_mode == "differential_in_avl_x12_out") ? "dq_differential_in_avl_x12_out" :
                                                                                                                                               "dq_disabled"                    ;
defparam u_io_12_lane_bcm.ioereg_top_4___ioereg_pnr_x2__ioereg_pnr_1__a_dqs_select   = (pin_9_dqs_x4_mode == "dqs_x4_a") ? "dqs_sampler_a" :
                                                                                       (pin_9_dqs_x4_mode == "dqs_x4_b") ? "dqs_sampler_b" :
                                                                                       (dqs_lgc_dqs_b_en == "true") ? "dqs_sampler_b_a_rise" : "dqs_sampler_a";
defparam u_io_12_lane_bcm.ioereg_top_4___ioereg_pnr_x2__ioereg_pnr_1__a_dynoct       = (pin_9_oct_mode == "dynamic")   ? "oct_enabled" : "oct_disabled" ;
defparam u_io_12_lane_bcm.ioereg_top_4___ioereg_pnr_x2__ioereg_pnr_1__a_initial_out  = pin_9_initial_out;
defparam u_io_12_lane_bcm.ioereg_top_4___ioereg_pnr_x2__ioereg_pnr_1__a_mode_ddr     = pin_9_mode_ddr;
defparam u_io_12_lane_bcm.ioereg_top_4___ioereg_pnr_x2__ioereg_pnr_1__a_octrt        = (pin_9_oct_mode == "static_on") ? "static_oct_on" : "static_oct_off" ;
defparam u_io_12_lane_bcm.ioereg_top_4___ioereg_pnr_x2__ioereg_pnr_1__a_output_phase = pin_9_output_phase[11:0];
defparam u_io_12_lane_bcm.ioereg_top_4___ioereg_pnr_x2__ioereg_pnr_1__a_gpio_differential = pin_9_output_phase[12];

defparam u_io_12_lane_bcm.ioereg_top_5___ioereg_pnr_x2__a_ddr2_oeb = "ddr3_preamble";
defparam u_io_12_lane_bcm.ioereg_top_5___ioereg_pnr_x2__a_dpa_enable = "dpa_disabled";
defparam u_io_12_lane_bcm.ioereg_top_5___ioereg_pnr_x2__a_lock_speed = 4'hF;
defparam u_io_12_lane_bcm.ioereg_top_5___ioereg_pnr_x2__a_power_down = "power_on";
defparam u_io_12_lane_bcm.ioereg_top_5___ioereg_pnr_x2__a_power_down_0 = "power_on_0";
defparam u_io_12_lane_bcm.ioereg_top_5___ioereg_pnr_x2__a_power_down_1 = "power_on_1";
defparam u_io_12_lane_bcm.ioereg_top_5___ioereg_pnr_x2__a_power_down_2 = "power_on_2";
defparam u_io_12_lane_bcm.ioereg_top_5___ioereg_pnr_x2__a_sync_control = "sync_enabled";
defparam u_io_12_lane_bcm.ioereg_top_5___ioereg_pnr_x2__ioereg_pnr_0__a_dfx_mode     = "dfx_disabled";
defparam u_io_12_lane_bcm.ioereg_top_5___ioereg_pnr_x2__ioereg_pnr_0__a_dq_select    = (pin_10_data_in_mode == "sstl_in")                     ? "dq_sstl_in"                     :
                                                                                       (pin_10_data_in_mode == "loopback_in")                 ? "dq_loopback_in"                 :
                                                                                       (pin_10_data_in_mode == "xor_loopback_in")             ? "dq_xor_loopback_in"             :
                                                                                       (pin_10_data_in_mode == "differential_in")             ? "dq_differential_in"             :
                                                                                       (pin_10_data_in_mode == "differential_in_avl_out")     ? "dq_differential_in_avl_out"     :
                                                                                       (pin_10_data_in_mode == "differential_in_x12_out")     ? "dq_differential_in_x12_out"     :
                                                                                       (pin_10_data_in_mode == "differential_in_avl_x12_out") ? "dq_differential_in_avl_x12_out" :
                                                                                                                                               "dq_disabled"                    ;
defparam u_io_12_lane_bcm.ioereg_top_5___ioereg_pnr_x2__ioereg_pnr_0__a_dqs_select   = (pin_10_dqs_x4_mode == "dqs_x4_a") ? "dqs_sampler_a" :
                                                                                       (pin_10_dqs_x4_mode == "dqs_x4_b") ? "dqs_sampler_b" :
                                                                                       (dqs_lgc_dqs_b_en == "true") ? "dqs_sampler_b_a_rise" : "dqs_sampler_a";
defparam u_io_12_lane_bcm.ioereg_top_5___ioereg_pnr_x2__ioereg_pnr_0__a_dynoct       = (pin_10_oct_mode == "dynamic")   ? "oct_enabled" : "oct_disabled" ;
defparam u_io_12_lane_bcm.ioereg_top_5___ioereg_pnr_x2__ioereg_pnr_0__a_initial_out  = pin_10_initial_out;
defparam u_io_12_lane_bcm.ioereg_top_5___ioereg_pnr_x2__ioereg_pnr_0__a_mode_ddr     = pin_10_mode_ddr;
defparam u_io_12_lane_bcm.ioereg_top_5___ioereg_pnr_x2__ioereg_pnr_0__a_octrt        = (pin_10_oct_mode == "static_on") ? "static_oct_on" : "static_oct_off" ;
defparam u_io_12_lane_bcm.ioereg_top_5___ioereg_pnr_x2__ioereg_pnr_0__a_output_phase = pin_10_output_phase[11:0];
defparam u_io_12_lane_bcm.ioereg_top_5___ioereg_pnr_x2__ioereg_pnr_0__a_gpio_differential = pin_10_output_phase[12];

defparam u_io_12_lane_bcm.ioereg_top_5___ioereg_pnr_x2__ioereg_pnr_1__a_dfx_mode     = "dfx_disabled";
defparam u_io_12_lane_bcm.ioereg_top_5___ioereg_pnr_x2__ioereg_pnr_1__a_dq_select    = (pin_11_data_in_mode == "sstl_in")                     ? "dq_sstl_in"                     :
                                                                                       (pin_11_data_in_mode == "loopback_in")                 ? "dq_loopback_in"                 :
                                                                                       (pin_11_data_in_mode == "xor_loopback_in")             ? "dq_xor_loopback_in"             :
                                                                                       (pin_11_data_in_mode == "differential_in")             ? "dq_differential_in"             :
                                                                                       (pin_11_data_in_mode == "differential_in_avl_out")     ? "dq_differential_in_avl_out"     :
                                                                                       (pin_11_data_in_mode == "differential_in_x12_out")     ? "dq_differential_in_x12_out"     :
                                                                                       (pin_11_data_in_mode == "differential_in_avl_x12_out") ? "dq_differential_in_avl_x12_out" :
                                                                                                                                               "dq_disabled"                    ;
defparam u_io_12_lane_bcm.ioereg_top_5___ioereg_pnr_x2__ioereg_pnr_1__a_dqs_select   = (pin_11_dqs_x4_mode == "dqs_x4_a") ? "dqs_sampler_a" :
                                                                                       (pin_11_dqs_x4_mode == "dqs_x4_b") ? "dqs_sampler_b" :
                                                                                       (dqs_lgc_dqs_b_en == "true") ? "dqs_sampler_b_a_rise" : "dqs_sampler_a";
defparam u_io_12_lane_bcm.ioereg_top_5___ioereg_pnr_x2__ioereg_pnr_1__a_dynoct       = (pin_11_oct_mode == "dynamic")   ? "oct_enabled" : "oct_disabled" ;
defparam u_io_12_lane_bcm.ioereg_top_5___ioereg_pnr_x2__ioereg_pnr_1__a_initial_out  = pin_11_initial_out;
defparam u_io_12_lane_bcm.ioereg_top_5___ioereg_pnr_x2__ioereg_pnr_1__a_mode_ddr     = pin_11_mode_ddr;
defparam u_io_12_lane_bcm.ioereg_top_5___ioereg_pnr_x2__ioereg_pnr_1__a_octrt        = (pin_11_oct_mode == "static_on") ? "static_oct_on" : "static_oct_off" ;
defparam u_io_12_lane_bcm.ioereg_top_5___ioereg_pnr_x2__ioereg_pnr_1__a_output_phase = pin_11_output_phase[11:0];
defparam u_io_12_lane_bcm.ioereg_top_5___ioereg_pnr_x2__ioereg_pnr_1__a_gpio_differential = pin_11_output_phase[12];

defparam u_io_12_lane_bcm.vref__a_vref_cal        = "a_vref_cal_disable";
defparam u_io_12_lane_bcm.vref__a_vref_offset     = "a_vref_offset_0";
defparam u_io_12_lane_bcm.vref__a_vref_offsetmode = "a_vref_offsetmode_add";
defparam u_io_12_lane_bcm.vref__a_vref_range      = "a_vref_range1";
defparam u_io_12_lane_bcm.vref__a_vref_sel        = "a_vref_sel_ext";
defparam u_io_12_lane_bcm.vref__a_vref_val        = "a_vref_cal_0";

defparam u_io_12_lane_bcm.xio_dll_top__xio_dll_pnr__a_rb_core_dn_prgmnvrt = "off";
defparam u_io_12_lane_bcm.xio_dll_top__xio_dll_pnr__a_rb_core_up_prgmnvrt = "off";
defparam u_io_12_lane_bcm.xio_dll_top__xio_dll_pnr__a_rb_core_updnen = dll_core_updnen;
defparam u_io_12_lane_bcm.xio_dll_top__xio_dll_pnr__a_rb_ctl_static = dll_ctl_static;
defparam u_io_12_lane_bcm.xio_dll_top__xio_dll_pnr__a_rb_ctlsel = dll_ctlsel;
defparam u_io_12_lane_bcm.xio_dll_top__xio_dll_pnr__a_rb_dftmuxsel0 = "pvt_gry_0";
defparam u_io_12_lane_bcm.xio_dll_top__xio_dll_pnr__a_rb_dftmuxsel1 = "pvt_gry_1";
defparam u_io_12_lane_bcm.xio_dll_top__xio_dll_pnr__a_rb_dftmuxsel2 = "pvt_gry_2";
defparam u_io_12_lane_bcm.xio_dll_top__xio_dll_pnr__a_rb_dftmuxsel4 = "pvt_gry_4";
defparam u_io_12_lane_bcm.xio_dll_top__xio_dll_pnr__a_rb_dftmuxsel5 = "pvt_gry_5";
defparam u_io_12_lane_bcm.xio_dll_top__xio_dll_pnr__a_rb_dftmuxsel6 = "pvt_gry_6";
defparam u_io_12_lane_bcm.xio_dll_top__xio_dll_pnr__a_rb_dftmuxsel7 = "pvt_gry_7";
defparam u_io_12_lane_bcm.xio_dll_top__xio_dll_pnr__a_rb_dftmuxsel8 = "pvt_gry_8";
defparam u_io_12_lane_bcm.xio_dll_top__xio_dll_pnr__a_rb_dftmuxsel9 = "pvt_gry_9";
defparam u_io_12_lane_bcm.xio_dll_top__xio_dll_pnr__a_rb_dll_en = dll_en;
defparam u_io_12_lane_bcm.xio_dll_top__xio_dll_pnr__a_rb_dll_rst_en = dll_rst_en;
defparam u_io_12_lane_bcm.xio_dll_top__xio_dll_pnr__a_rb_dly_pst = 10'h000;
defparam u_io_12_lane_bcm.xio_dll_top__xio_dll_pnr__a_rb_dly_pst_en = "dly_adj_dis";
defparam u_io_12_lane_bcm.xio_dll_top__xio_dll_pnr__a_rb_hps_ctrl_en = "hps_ctrl_dis";
defparam u_io_12_lane_bcm.xio_dll_top__xio_dll_pnr__a_rb_ndllrst_prgmnvrt = "off";
defparam u_io_12_lane_bcm.xio_dll_top__xio_dll_pnr__a_rb_new_dll = 3'b000;

defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_broadcast_enable = dqs_lgc_broadcast_enable;
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_burst_length = dqs_lgc_burst_length;
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_count_threshold = dqs_lgc_count_threshold;
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_ddr4_search = dqs_lgc_ddr4_search;
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_dqs_en = "dqs_gated";

localparam dqs_shrink_delay = (dqs_lgc_pst_en_shrink == "shrink_0_0") ? ((mode_rate_out == "out_rate_full") ? ((128 + 64) +    0)  :
                                                                         (mode_rate_out == "out_rate_1_2")  ? ((128 + 64) +    0)  : 
                                                                         (mode_rate_out == "out_rate_1_4")  ? ((128 + 64) +    0)  : 
                                                                                                              ((128 + 64) +    0)) : 
                              (dqs_lgc_pst_en_shrink == "shrink_0_1") ? ((mode_rate_out == "out_rate_full") ? ((128 + 64) +    0)  :
                                                                         (mode_rate_out == "out_rate_1_2")  ? ((128 + 64) +  128)  :
                                                                         (mode_rate_out == "out_rate_1_4")  ? ((128 + 64) +  256)  :
                                                                                                              ((128 + 64) +  512)) :
                              (dqs_lgc_pst_en_shrink == "shrink_1_0") ? ((mode_rate_out == "out_rate_full") ? ((128 + 64) +  128)  :
                                                                         (mode_rate_out == "out_rate_1_2")  ? ((128 + 64) +  256)  :
                                                                         (mode_rate_out == "out_rate_1_4")  ? ((128 + 64) +  512)  :
                                                                                                              ((128 + 64) + 1024)) :
                                                                        ((mode_rate_out == "out_rate_full") ? ((128 + 64) +  128)  :
                                                                         (mode_rate_out == "out_rate_1_2")  ? ((128 + 64) +  384)  :
                                                                         (mode_rate_out == "out_rate_1_4")  ? ((128 + 64) +  768)  :
                                                                                                              ((128 + 64) + 1536)) ;

localparam out_rate_fact = (mode_rate_out == "out_rate_full") ? 1 :
                           (mode_rate_out == "out_rate_1_2")  ? 2 :
                           (mode_rate_out == "out_rate_1_4")  ? 4 :
                                                                8 ;
localparam integer dqs_shrink_delay_full_osc_cycles  = (dqs_shrink_delay / 128) + 1; 
localparam integer dqs_shrink_delay_full_dram_cycles = (dqs_shrink_delay_full_osc_cycles / out_rate_fact) + (((dqs_shrink_delay_full_osc_cycles % out_rate_fact) > 0) ? 1 : 0); 
localparam dqs_shrink_delay_phase_adj = (dqs_shrink_delay_full_dram_cycles * out_rate_fact * 128) - dqs_shrink_delay;

localparam dqs_enable_gate_delay = 128 * (195 / ((1.0 / phy_clk_phs_freq) * (10**6)));
localparam output_gate_delay     = 128 * ( 56 / ((1.0 / phy_clk_phs_freq) * (10**6)));
localparam [12:0] net_gate_delays = output_gate_delay - dqs_enable_gate_delay;

defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_dqs_enable_delay = (mode_rate_out == "out_rate_full" ) ? ((mode_rate_in == "in_rate_1_4") ? dqs_enable_delay - (pipe_latency * 4)     - 6'd04             - 6'd01 - dqs_shrink_delay_full_dram_cycles   :
                                                                                                                    (mode_rate_in == "in_rate_1_2") ? dqs_enable_delay - (pipe_latency * 2)     - 6'd02             - 6'd01 - dqs_shrink_delay_full_dram_cycles   :
                                                                                                                                                      dqs_enable_delay - (pipe_latency * 1)     - 6'd01             - 6'd01 - dqs_shrink_delay_full_dram_cycles ) :
                                                                             (mode_rate_out == "out_rate_1_2" ) ?  ((mode_rate_in == "in_rate_1_4") ? dqs_enable_delay - (pipe_latency * 4)     - 6'd04             - 6'd01 - dqs_shrink_delay_full_dram_cycles   :
                                                                                                                    (mode_rate_in == "in_rate_1_2") ? dqs_enable_delay - (pipe_latency * 2)     - 6'd02             - 6'd01 - dqs_shrink_delay_full_dram_cycles   :
                                                                                                                                                      dqs_enable_delay - (pipe_latency * 1)     - 6'd01             - 6'd01 - dqs_shrink_delay_full_dram_cycles ) :
                                                                             (mode_rate_out == "out_rate_1_4" ) ?  ((mode_rate_in == "in_rate_1_4") ? dqs_enable_delay - (pipe_latency * 4)     - 6'd04             - 6'd01 - dqs_shrink_delay_full_dram_cycles   :
                                                                                                                    (mode_rate_in == "in_rate_1_2") ? dqs_enable_delay - (pipe_latency * 2)     - 6'd02             - 6'd01 - dqs_shrink_delay_full_dram_cycles   :
                                                                                                                                                      dqs_enable_delay - (pipe_latency * 1)     - 6'd01             - 6'd01 - dqs_shrink_delay_full_dram_cycles ) :
                                                                                                                   ((mode_rate_in == "in_rate_1_4") ? dqs_enable_delay - (pipe_latency * 4)     - 6'd04             - 6'd01 - dqs_shrink_delay_full_dram_cycles   :
                                                                                                                    (mode_rate_in == "in_rate_1_2") ? dqs_enable_delay - (pipe_latency * 2)     - 6'd02             - 6'd01 - dqs_shrink_delay_full_dram_cycles   :
                                                                                                                                                      dqs_enable_delay - (pipe_latency * 1)     - 6'd01             - 6'd01 - dqs_shrink_delay_full_dram_cycles ) ;

defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_phase_shift_a = dqs_lgc_phase_shift_a                + net_gate_delays      + dqs_shrink_delay_phase_adj;
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_phase_shift_b = dqs_lgc_phase_shift_b                + net_gate_delays      + dqs_shrink_delay_phase_adj;

defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_dqs_select_a = (dqs_lgc_dqs_a_interp_en == "true")  ? "a_dqs_interpolator" :
                                                                         (dqs_lgc_swap_dqs_a_b == "true")     ? "a_dqs_sstl_n_0"     :
                                                                         (db_crc_x4_or_x8_or_x9 == "x4_mode") ? "a_dqs_diff_in_1"    :
                                                                                                                "a_dqs_sstl_p_0"     ;

defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_dqs_select_b = (dqs_lgc_dqs_b_interp_en == "true")  ? "b_dqs_interpolator" :
                                                                         (dqs_lgc_swap_dqs_a_b == "true")     ? "b_dqs_sstl_p_0"     :
                                                                         (db_crc_x4_or_x8_or_x9 == "x4_mode") ? "b_dqs_diff_in_0"    :
                                                                                                                "b_dqs_sstl_n_0"     ;

defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_enable_toggler = dqs_lgc_enable_toggler;
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_filter_code = (phy_clk_phs_freq >= 500 && phy_clk_phs_freq < 600) ? "freq_08ghz" :
									(phy_clk_phs_freq >= 600 && phy_clk_phs_freq < 750) ? "freq_10ghz" :
									(phy_clk_phs_freq >= 750 && phy_clk_phs_freq < 950) ? "freq_12ghz" :
									"freq_16ghz";
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_kicker_size = 2'h1;
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_lock_edge = "preamble_lock_rising_edge";
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_mode_rate_in = mode_rate_in;
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_mode_rate_out = mode_rate_out;
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_non_pvt_dq_0_delay = 9'd0;
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_non_pvt_dq_1_delay = 9'd0;
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_non_pvt_dq_2_delay = 9'd0;
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_non_pvt_dq_3_delay = 9'd0;
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_non_pvt_dq_4_delay = 9'd0;
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_non_pvt_dq_5_delay = 9'd0;
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_non_pvt_dq_6_delay = 9'd0;
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_non_pvt_dq_7_delay = 9'd0;
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_non_pvt_dq_8_delay = 9'd0;
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_non_pvt_dq_9_delay = 9'd0;
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_non_pvt_dq_10_delay = 9'd0;
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_non_pvt_dq_11_delay = 9'd0;
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_non_pvt_dqs_delay = 10'd0;
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_oct_size = 3'h1;
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_pack_mode = dqs_lgc_pack_mode;
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_phy_clk_mode = (phy_clk_sel == 0) ? "phy_clk_0" : "phy_clk_1";
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_probe_sel = 4'h0;
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_pst_en_shrink = dqs_lgc_pst_en_shrink;
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_pst_preamble_mode = dqs_lgc_pst_preamble_mode;
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_pvt_input_delay_a = dqs_lgc_pvt_input_delay_a;
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_pvt_input_delay_b = dqs_lgc_pvt_input_delay_b;
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_rd_valid_delay = rd_valid_delay;
defparam u_io_12_lane_bcm.xio_dqs_lgc_top__dqs_lgc_pnr__a_track_speed = 4'hc;

defparam u_io_12_lane_bcm.xio_regulator__a_cr_atbsel0 = "cr_atbsel0_dis";
defparam u_io_12_lane_bcm.xio_regulator__a_cr_atbsel1 = "cr_atbsel1_dis";
defparam u_io_12_lane_bcm.xio_regulator__a_cr_atbsel2 = "cr_atbsel2_dis";
defparam u_io_12_lane_bcm.xio_regulator__a_cr_pd = "cr_pd_dis";



defparam u_io_12_lane_bcm.ioereg_top_0___gpio_wrapper_0__gpio_reg__xio_jtag__a_rb_gpio_or_ddr_sel = "jtag_ddr_sel";
defparam u_io_12_lane_bcm.ioereg_top_0___gpio_wrapper_1__gpio_reg__xio_jtag__a_rb_gpio_or_ddr_sel = "jtag_ddr_sel";
defparam u_io_12_lane_bcm.ioereg_top_1___gpio_wrapper_0__gpio_reg__xio_jtag__a_rb_gpio_or_ddr_sel = "jtag_ddr_sel";
defparam u_io_12_lane_bcm.ioereg_top_1___gpio_wrapper_1__gpio_reg__xio_jtag__a_rb_gpio_or_ddr_sel = "jtag_ddr_sel";
defparam u_io_12_lane_bcm.ioereg_top_2___gpio_wrapper_0__gpio_reg__xio_jtag__a_rb_gpio_or_ddr_sel = "jtag_ddr_sel";
defparam u_io_12_lane_bcm.ioereg_top_2___gpio_wrapper_1__gpio_reg__xio_jtag__a_rb_gpio_or_ddr_sel = "jtag_ddr_sel";
defparam u_io_12_lane_bcm.ioereg_top_3___gpio_wrapper_0__gpio_reg__xio_jtag__a_rb_gpio_or_ddr_sel = "jtag_ddr_sel";
defparam u_io_12_lane_bcm.ioereg_top_3___gpio_wrapper_1__gpio_reg__xio_jtag__a_rb_gpio_or_ddr_sel = "jtag_ddr_sel";
defparam u_io_12_lane_bcm.ioereg_top_4___gpio_wrapper_0__gpio_reg__xio_jtag__a_rb_gpio_or_ddr_sel = "jtag_ddr_sel";
defparam u_io_12_lane_bcm.ioereg_top_4___gpio_wrapper_1__gpio_reg__xio_jtag__a_rb_gpio_or_ddr_sel = "jtag_ddr_sel";
defparam u_io_12_lane_bcm.ioereg_top_5___gpio_wrapper_0__gpio_reg__xio_jtag__a_rb_gpio_or_ddr_sel = "jtag_ddr_sel";
defparam u_io_12_lane_bcm.ioereg_top_5___gpio_wrapper_1__gpio_reg__xio_jtag__a_rb_gpio_or_ddr_sel = "jtag_ddr_sel";

defparam u_io_12_lane_bcm.i0.ioereg_top_0_.interpolator_0.fast_interpolator_sim = fast_interpolator_sim;
defparam u_io_12_lane_bcm.i0.ioereg_top_0_.interpolator_1.fast_interpolator_sim = fast_interpolator_sim;
defparam u_io_12_lane_bcm.i0.ioereg_top_1_.interpolator_0.fast_interpolator_sim = fast_interpolator_sim;
defparam u_io_12_lane_bcm.i0.ioereg_top_1_.interpolator_1.fast_interpolator_sim = fast_interpolator_sim;
defparam u_io_12_lane_bcm.i0.ioereg_top_2_.interpolator_0.fast_interpolator_sim = fast_interpolator_sim;
defparam u_io_12_lane_bcm.i0.ioereg_top_2_.interpolator_1.fast_interpolator_sim = fast_interpolator_sim;
defparam u_io_12_lane_bcm.i0.ioereg_top_3_.interpolator_0.fast_interpolator_sim = fast_interpolator_sim;
defparam u_io_12_lane_bcm.i0.ioereg_top_3_.interpolator_1.fast_interpolator_sim = fast_interpolator_sim;
defparam u_io_12_lane_bcm.i0.ioereg_top_4_.interpolator_0.fast_interpolator_sim = fast_interpolator_sim;
defparam u_io_12_lane_bcm.i0.ioereg_top_4_.interpolator_1.fast_interpolator_sim = fast_interpolator_sim;
defparam u_io_12_lane_bcm.i0.ioereg_top_5_.interpolator_0.fast_interpolator_sim = fast_interpolator_sim;
defparam u_io_12_lane_bcm.i0.ioereg_top_5_.interpolator_1.fast_interpolator_sim = fast_interpolator_sim;
defparam u_io_12_lane_bcm.i0.xio_dqs_lgc_top.interpolator_a.fast_interpolator_sim = fast_interpolator_sim;
defparam u_io_12_lane_bcm.i0.xio_dqs_lgc_top.interpolator_b.fast_interpolator_sim = fast_interpolator_sim;

endmodule

