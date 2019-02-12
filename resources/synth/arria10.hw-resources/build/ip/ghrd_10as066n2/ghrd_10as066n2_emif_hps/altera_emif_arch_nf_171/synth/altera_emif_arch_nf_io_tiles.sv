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



///////////////////////////////////////////////////////////////////////////////
// This module instantiates one or more x48 I/O tiles (along with
// the necessary x12 I/O lanes) that are required to build as single EMIF.
//
///////////////////////////////////////////////////////////////////////////////
 
// Simple max/min
`define _get_max(_i, _j)                                 ( (_i) > (_j) ? (_i) : (_j) )
`define _get_min(_i, _j)                                 ( (_i) < (_j) ? (_i) : (_j) )
   
// Index to signal buses used to implement a daisy chain of 
// (L0->L1->T0->L2->L3)->(L0->L1->T1->L2->L3)->...
`define _get_chain_index_for_tile(_tile_i)               ( _tile_i * (LANES_PER_TILE + 1) + 2 )

`define _get_chain_index_for_lane(_tile_i, _lane_i)      ( (_lane_i < 2) ? (_tile_i * (LANES_PER_TILE + 1) + _lane_i) : ( \
                                                                           (_tile_i * (LANES_PER_TILE + 1) + _lane_i + 1 )) )

// Index to signal buses used to implement a daisy chain of
// (L0->L1->L2->L3)->(L0->L1->L2->L3)->...
`define _get_broadcast_chain_index(_tile_i, _lane_i)     ( _tile_i * LANES_PER_TILE + _lane_i )

`define _get_lane_usage(_tile_i, _lane_i)                ( LANES_USAGE[(_tile_i * LANES_PER_TILE + _lane_i) * 3 +: 3] )

`define _get_pin_oct_mode_raw(_tile_i, _lane_i, _pin_i)  ( PINS_OCT_MODE[(_tile_i * LANES_PER_TILE * PINS_PER_LANE + _lane_i * PINS_PER_LANE + _pin_i)] )

`define _get_pin_ddr_raw(_tile_i, _lane_i, _pin_i)       ( PINS_RATE[_tile_i * LANES_PER_TILE * PINS_PER_LANE + _lane_i * PINS_PER_LANE + _pin_i] )
`define _get_pin_ddr_str(_tile_i, _lane_i, _pin_i)       ( `_get_pin_ddr_raw(_tile_i, _lane_i, _pin_i) == PIN_RATE_DDR ? "mode_ddr" : "mode_sdr" )
                                                         
`define _get_pin_usage(_tile_i, _lane_i, _pin_i)         ( PINS_USAGE[_tile_i * LANES_PER_TILE * PINS_PER_LANE + _lane_i * PINS_PER_LANE + _pin_i] )
                                                         
`define _get_pin_wdb_raw(_tile_i, _lane_i, _pin_i)       ( PINS_WDB[(_tile_i * LANES_PER_TILE * PINS_PER_LANE + _lane_i * PINS_PER_LANE + _pin_i) * 3 +: 3] )
`define _get_pin_wdb_str(_tile_i, _lane_i, _pin_i)       ( `_get_pin_wdb_raw(_tile_i, _lane_i, _pin_i) == PIN_WDB_AC_CORE       ? "ac_core"       : ( \
                                                           `_get_pin_wdb_raw(_tile_i, _lane_i, _pin_i) == PIN_WDB_AC_HMC        ? "ac_hmc"        : ( \
                                                           `_get_pin_wdb_raw(_tile_i, _lane_i, _pin_i) == PIN_WDB_DQS_WDB_MODE  ? "dqs_wdb_mode"  : ( \
                                                           `_get_pin_wdb_raw(_tile_i, _lane_i, _pin_i) == PIN_WDB_DQS_MODE      ? "dqs_mode"      : ( \
                                                           `_get_pin_wdb_raw(_tile_i, _lane_i, _pin_i) == PIN_WDB_DM_WDB_MODE   ? "dm_wdb_mode"   : ( \
                                                           `_get_pin_wdb_raw(_tile_i, _lane_i, _pin_i) == PIN_WDB_DM_MODE       ? "dm_mode"       : ( \
                                                           `_get_pin_wdb_raw(_tile_i, _lane_i, _pin_i) == PIN_WDB_DQ_WDB_MODE   ? "dq_wdb_mode"   : ( \
                                                                                                                                  "dq_mode"         ))))))))

`define _get_pin_db_in_bypass(_tile_i, _lane_i, _pin_i)  ( PINS_DB_IN_BYPASS[_tile_i * LANES_PER_TILE * PINS_PER_LANE + _lane_i * PINS_PER_LANE + _pin_i] ? "true" : "false" )
`define _get_pin_db_out_bypass(_tile_i, _lane_i, _pin_i) ( PINS_DB_OUT_BYPASS[_tile_i * LANES_PER_TILE * PINS_PER_LANE + _lane_i * PINS_PER_LANE + _pin_i] ? "true" : "false" )
`define _get_pin_db_oe_bypass(_tile_i, _lane_i, _pin_i)  ( PINS_DB_OE_BYPASS[_tile_i * LANES_PER_TILE * PINS_PER_LANE + _lane_i * PINS_PER_LANE + _pin_i] ? "true" : "false" )
                                                                                                                                  
`define _get_pin_invert_wr(_tile_i, _lane_i, _pin_i)     ( PINS_INVERT_WR[_tile_i * LANES_PER_TILE * PINS_PER_LANE + _lane_i * PINS_PER_LANE + _pin_i] ? "true" : "false" )
`define _get_pin_invert_oe(_tile_i, _lane_i, _pin_i)     ( PINS_INVERT_OE[_tile_i * LANES_PER_TILE * PINS_PER_LANE + _lane_i * PINS_PER_LANE + _pin_i] ? "true" : "false" )

`define _get_pin_ac_hmc_data_override_ena(_tile_i, _lane_i, _pin_i) ( PINS_AC_HMC_DATA_OVERRIDE_ENA[_tile_i * LANES_PER_TILE * PINS_PER_LANE + _lane_i * PINS_PER_LANE + _pin_i] ? "true" : "false" )

`define _get_pin_oct_mode_str(_tile_i, _lane_i, _pin_i)   ( `_get_pin_oct_mode_raw(_tile_i, _lane_i, _pin_i) == PIN_OCT_STATIC_OFF  ? "static_off" : ( \
                                                            `_get_pin_oct_mode_raw(_tile_i, _lane_i, _pin_i) == PIN_OCT_DYNAMIC     ? "dynamic" : ( \
                                                                                                                                      "dynamic" )))
                                                                                                                                      
`define _get_pin_gpio_or_ddr(_tile_i, _lane_i, _pin_i)     ( PINS_GPIO_MODE[_tile_i * LANES_PER_TILE * PINS_PER_LANE + _lane_i * PINS_PER_LANE + _pin_i] ? "gpio" : "ddr" )

`define _get_pin_data_in_mode_raw(_tile_i, _lane_i, _pin_i) ( PINS_DATA_IN_MODE[(_tile_i * LANES_PER_TILE * PINS_PER_LANE + _lane_i * PINS_PER_LANE + _pin_i) * 3 +: 3] )

`define _get_pin_data_in_mode_str(_tile_i, _lane_i, _pin_i) ( `_get_pin_data_in_mode_raw(_tile_i, _lane_i, _pin_i) == PIN_DATA_IN_MODE_DISABLED         ? "disabled"                    : ( \
                                                              `_get_pin_data_in_mode_raw(_tile_i, _lane_i, _pin_i) == PIN_DATA_IN_MODE_SSTL_IN          ? "sstl_in"                     : ( \
                                                              `_get_pin_data_in_mode_raw(_tile_i, _lane_i, _pin_i) == PIN_DATA_IN_MODE_LOOPBACK_IN      ? "loopback_in"                 : ( \
                                                              `_get_pin_data_in_mode_raw(_tile_i, _lane_i, _pin_i) == PIN_DATA_IN_MODE_XOR_LOOPBACK_IN  ? "xor_loopback_in"             : ( \
                                                              `_get_pin_data_in_mode_raw(_tile_i, _lane_i, _pin_i) == PIN_DATA_IN_MODE_DIFF_IN          ? "differential_in"             : ( \
                                                              `_get_pin_data_in_mode_raw(_tile_i, _lane_i, _pin_i) == PIN_DATA_IN_MODE_DIFF_IN_AVL_OUT  ? "differential_in_avl_out"     : ( \
                                                              `_get_pin_data_in_mode_raw(_tile_i, _lane_i, _pin_i) == PIN_DATA_IN_MODE_DIFF_IN_X12_OUT  ? "differential_in_x12_out"     : ( \
                                                                                                                                                          "differential_in_avl_x12_out"   ))))))))

//  Given the tile and lane index of a lane, returns the index of the AC tile controlling
//  this lane. For non-ping-pong, return value is always PRI_AC_TILE_INDEX. 
//  For ping-pong, return SEC_AC_TILE_INDEX for all tiles below tile at SEC_AC_TILE_INDEX,
//  and for lane 2 and 3 of tile SEC_AC_TILE_INDEX; return PRI_AC_TILE_INDEX otherwise.
//  This assumption must be consistent with the logical pin placement strategy in hwtcl.
`define _get_ac_tile_index(_tile_i, _lane_i)             ( (PHY_PING_PONG_EN && (_tile_i < SEC_AC_TILE_INDEX || (_tile_i == SEC_AC_TILE_INDEX && _lane_i < 2))) ? SEC_AC_TILE_INDEX : PRI_AC_TILE_INDEX )

// _get_dbc_pipe_lat returns: current_distance_from_ac_tile
//      which is the same as: abs(_tile_i - AC_TILE_INDEX)
`define _get_dbc_pipe_lat(_tile_i, _lane_i)              ( (_tile_i > `_get_ac_tile_index(_tile_i, _lane_i)) ? (_tile_i - `_get_ac_tile_index(_tile_i, _lane_i)) : \
                                                                                                               (`_get_ac_tile_index(_tile_i, _lane_i) - _tile_i) )

// _get_db_ptr_pipe_depth returns: max_distance_from_ac_tile - current_distance_from_ac_tile
//           which is the same as: max(NUM_OF_RTL_TILES - PRI_AC_TILE_INDEX - 1, PRI_AC_TILE_INDEX) - abs(_tile_i - PRI_AC_TILE_INDEX)
// In PingPong PHY case, the same calculation method applies but as if all data lanes are
// controlled by primary AC tile, except that the lanes actually controlled by secondary AC tile
// have the value added by 1.
`define _get_max_distance_from_ac_tile                   ( `_get_max( (NUM_OF_RTL_TILES - PRI_AC_TILE_INDEX - 1), PRI_AC_TILE_INDEX ) )

// Note that the maximum db_seq_rd_en_full_pipeline value supported by h/w is 4. In Ping-Pong PHY, 
// applying the normal formulas can result in a value of 5 for the secondary lanes whenever the max
// distance from ac tile is 3. When this happens, we normalize by reducing both db_seq_rd_en_full_pipeline
// and pipeline_depth by 1. This is always safe to do because by construction, a ping-pong interface
// must have more tiles below the primary HMC tile than above, and so nominal pipeline_depth is always
// bigger than 1 (i.e. won't become negative after the subtraction). The simplest way to implement this
// is by capping max_distance_from_ac_tile to 2.
`define _get_max_distance_from_ac_tile_capped            ( PHY_PING_PONG_EN ? `_get_min(2, `_get_max_distance_from_ac_tile) : `_get_max_distance_from_ac_tile ) 

`define _get_curr_distance_from_ac_tile(_tile_i)         ( (_tile_i > PRI_AC_TILE_INDEX) ? (_tile_i - PRI_AC_TILE_INDEX) : (PRI_AC_TILE_INDEX - _tile_i) )
`define _get_db_ptr_pipe_depth_pri(_tile_i)              ( `_get_max_distance_from_ac_tile_capped - `_get_curr_distance_from_ac_tile(_tile_i) )
`define _get_db_ptr_pipe_depth_sec(_tile_i)              ( `_get_db_ptr_pipe_depth_pri(_tile_i) + 1)
`define _get_db_ptr_pipe_depth(_tile_i, _lane_i)         ( `_get_ac_tile_index(_tile_i, _lane_i) == PRI_AC_TILE_INDEX ? `_get_db_ptr_pipe_depth_pri(_tile_i) : `_get_db_ptr_pipe_depth_sec(_tile_i))

// _get_db_seq_rd_en_full_pipeline: for HMC: max_distance_from_ac_tile + 1
//                                  for SMC: db_ptr_pipe_depth + 1
// In PingPong PHY case, the same calculation method applies but as if all data lanes are
// controlled by primary AC tile, except that the lanes actually controlled by secondary AC tile
// have the value added by 1.
`define _get_db_seq_rd_en_full_pipeline_pri(_tile_i, _lane_i) ( (NUM_OF_HMC_PORTS > 0) ? (`_get_max_distance_from_ac_tile_capped + 1) : (`_get_db_ptr_pipe_depth(_tile_i, _lane_i) + 1) )
`define _get_db_seq_rd_en_full_pipeline_sec(_tile_i, _lane_i) ( `_get_db_seq_rd_en_full_pipeline_pri(_tile_i, _lane_i) + 1 )
`define _get_db_seq_rd_en_full_pipeline(_tile_i, _lane_i)     ( `_get_ac_tile_index(_tile_i, _lane_i) == PRI_AC_TILE_INDEX ? `_get_db_seq_rd_en_full_pipeline_pri(_tile_i, _lane_i) : `_get_db_seq_rd_en_full_pipeline_sec(_tile_i, _lane_i))

`define _get_db_data_alignment_mode                      ( (NUM_OF_HMC_PORTS > 0) ? "align_ena" : "align_disable" )
                                                                                                                                  
`define _get_lane_mode_rate_in                           ( PHY_HMC_CLK_RATIO == 4 ? "in_rate_1_4" : ( \
                                                           PHY_HMC_CLK_RATIO == 2 ? "in_rate_1_2" : ( \
                                                                                    "in_rate_full" )))
                                                         
`define _get_lane_mode_rate_out                          ( PLL_VCO_TO_MEM_CLK_FREQ_RATIO == 8 ? "out_rate_1_8" : ( \
                                                           PLL_VCO_TO_MEM_CLK_FREQ_RATIO == 4 ? "out_rate_1_4" : ( \
                                                           PLL_VCO_TO_MEM_CLK_FREQ_RATIO == 2 ? "out_rate_1_2" : ( \
                                                                                                "out_rate_full" ))))
                                                         
`define _get_hmc_ctrl_mem_type                           ( PROTOCOL_ENUM == "PROTOCOL_DDR3"   ? "ddr3"       : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_DDR4"   ? "ddr4"       : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_RLD3"   ? "rldram_iii" : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_LPDDR3" ? "lpddr3"     : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_QDR2"   ? "rldram_iii"       : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_RLD2"   ? "rldram_iii"       : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_QDR4"   ? "rldram_iii"       : ( \
                                                                                                ""             ))))))))

`define _get_hmc_or_core                                 ( NUM_OF_HMC_PORTS == 0 ? "core" : "hmc" )
                                                         
`define _get_hmc_cmd_rate                                ( PHY_HMC_CLK_RATIO == 4 ? "quarter_rate"      : "half_rate" )
`define _get_dbc0_cmd_rate                               ( PHY_HMC_CLK_RATIO == 4 ? "quarter_rate_dbc0" : "half_rate_dbc0" )
`define _get_dbc1_cmd_rate                               ( PHY_HMC_CLK_RATIO == 4 ? "quarter_rate_dbc1" : "half_rate_dbc1" )
`define _get_dbc2_cmd_rate                               ( PHY_HMC_CLK_RATIO == 4 ? "quarter_rate_dbc2" : "half_rate_dbc2" )
`define _get_dbc3_cmd_rate                               ( PHY_HMC_CLK_RATIO == 4 ? "quarter_rate_dbc3" : "half_rate_dbc3" )
                                                         
`define _get_hmc_protocol                                ( HMC_AVL_PROTOCOL_ENUM == "CTRL_AVL_PROTOCOL_MM" ? "amm_in"   : "ast_in" )
`define _get_dbc0_protocol                               ( HMC_AVL_PROTOCOL_ENUM == "CTRL_AVL_PROTOCOL_MM" ? "amm_dbc0" : "ast_dbc0" )
`define _get_dbc1_protocol                               ( HMC_AVL_PROTOCOL_ENUM == "CTRL_AVL_PROTOCOL_MM" ? "amm_dbc1" : "ast_dbc1" )
`define _get_dbc2_protocol                               ( HMC_AVL_PROTOCOL_ENUM == "CTRL_AVL_PROTOCOL_MM" ? "amm_dbc2" : "ast_dbc2" )
`define _get_dbc3_protocol                               ( HMC_AVL_PROTOCOL_ENUM == "CTRL_AVL_PROTOCOL_MM" ? "amm_dbc3" : "ast_dbc3" )
                                                         
`define _get_hmc_burst_length                            ( MEM_BURST_LENGTH == 2 ? "bl_2_ctrl"   : ( \
                                                           MEM_BURST_LENGTH == 4 ? "bl_4_ctrl"   : ( \
                                                           MEM_BURST_LENGTH == 8 ? "bl_8_ctrl"   : ( \
                                                                                   ""              ))))
                                                         
`define _get_dbc0_burst_length                           ( MEM_BURST_LENGTH == 2 ? "bl_2_dbc0"   : ( \
                                                           MEM_BURST_LENGTH == 4 ? "bl_4_dbc0"   : ( \
                                                           MEM_BURST_LENGTH == 8 ? "bl_8_dbc0"   : ( \
                                                                                   ""              ))))
                                                         
`define _get_dbc1_burst_length                           ( MEM_BURST_LENGTH == 2 ? "bl_2_dbc1"   : ( \
                                                           MEM_BURST_LENGTH == 4 ? "bl_4_dbc1"   : ( \
                                                           MEM_BURST_LENGTH == 8 ? "bl_8_dbc1"   : ( \
                                                                                   ""              ))))
                                                         
`define _get_dbc2_burst_length                           ( MEM_BURST_LENGTH == 2 ? "bl_2_dbc2"   : ( \
                                                           MEM_BURST_LENGTH == 4 ? "bl_4_dbc2"   : ( \
                                                           MEM_BURST_LENGTH == 8 ? "bl_8_dbc2"   : ( \
                                                                                   ""              ))))
                                                         
`define _get_dbc3_burst_length                           ( MEM_BURST_LENGTH == 2 ? "bl_2_dbc3"   : ( \
                                                           MEM_BURST_LENGTH == 4 ? "bl_4_dbc3"   : ( \
                                                           MEM_BURST_LENGTH == 8 ? "bl_8_dbc3"   : ( \
                                                                                   ""              ))))

`define _get_dqs_lgc_burst_length                        ( PROTOCOL_ENUM == "PROTOCOL_RLD3" ? "burst_length_2" : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_RLD2" ? "burst_length_2" : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_QDR2" ? "burst_length_2" : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_QDR4" ? "burst_length_2" : ( \
                                                           MEM_BURST_LENGTH == 2 ? "burst_length_2"   : ( \
                                                           MEM_BURST_LENGTH == 4 ? "burst_length_4"   : ( \
                                                           MEM_BURST_LENGTH == 8 ? "burst_length_8"   : ( \
                                                                                   ""                   ))))))))
                                                                                   
`define _get_pa_exponent(_clk_ratio)                     ( (_clk_ratio * PLL_VCO_TO_MEM_CLK_FREQ_RATIO) == 1   ? 3'b000 : ( \
                                                           (_clk_ratio * PLL_VCO_TO_MEM_CLK_FREQ_RATIO) == 2   ? 3'b001 : ( \
                                                           (_clk_ratio * PLL_VCO_TO_MEM_CLK_FREQ_RATIO) == 4   ? 3'b010 : ( \
                                                           (_clk_ratio * PLL_VCO_TO_MEM_CLK_FREQ_RATIO) == 8   ? 3'b011 : ( \
                                                           (_clk_ratio * PLL_VCO_TO_MEM_CLK_FREQ_RATIO) == 16  ? 3'b100 : ( \
                                                           (_clk_ratio * PLL_VCO_TO_MEM_CLK_FREQ_RATIO) == 32  ? 3'b101 : ( \
                                                           (_clk_ratio * PLL_VCO_TO_MEM_CLK_FREQ_RATIO) == 64  ? 3'b110 : ( \
                                                           (_clk_ratio * PLL_VCO_TO_MEM_CLK_FREQ_RATIO) == 128 ? 3'b111 : ( \
                                                                                                                 3'b000 )))))))))
                                                                                                                 
// CPA output 0 - in HMC mode, matches emif_usr_clk; in non-HMC mode, matches afi_half_clk
`define _get_cpa_0_clk_ratio                             ( NUM_OF_HMC_PORTS > 0 ? USER_CLK_RATIO : (USER_CLK_RATIO * 2) )
`define _get_pa_exponent_0                               ( (`_get_pa_exponent(`_get_cpa_0_clk_ratio)) )

// CPA output 1 - always matches the C2P/P2C rate
`define _get_cpa_1_clk_ratio                             ( C2P_P2C_CLK_RATIO )
`define _get_pa_exponent_1                               ( (`_get_pa_exponent(`_get_cpa_1_clk_ratio)) )

// CPA output 0 - clock divider on PHY clock feedback. 
//                Enable divide-by-2 whenever the core clock needs to run at half the speed of the feedback clock
`define _get_pa_feedback_divider_p0                      ( (`_get_cpa_0_clk_ratio == C2P_P2C_CLK_RATIO * 2) ? "div_by_2_p0" : "div_by_1_p0" )

// CPA output 0 - clock divider on core clock feedback. 
//                Enable divide-by-2 whenever the core clock needs to run at 2x the speed of the feedback clock
`define _get_pa_feedback_divider_c0                      ( (`_get_cpa_0_clk_ratio * 2 == C2P_P2C_CLK_RATIO) ? "div_by_2_c0" : "div_by_1_c0" )

`define _get_dqsin(_tile_i, _lane_i)                     ( (`_get_lane_usage(_tile_i, _lane_i) != LANE_USAGE_RDATA && `_get_lane_usage(_tile_i, _lane_i) != LANE_USAGE_WDATA && `_get_lane_usage(_tile_i, _lane_i) != LANE_USAGE_WRDATA) ? 2'b0 : ( \
                                                           DQS_BUS_MODE_ENUM == "DQS_BUS_MODE_X4"       ? t2l_dqsbus_x4[_lane_i]  : ( \
                                                           DQS_BUS_MODE_ENUM == "DQS_BUS_MODE_X8_X9"    ? t2l_dqsbus_x8[_lane_i]  : ( \
                                                           DQS_BUS_MODE_ENUM == "DQS_BUS_MODE_X16_X18"  ? t2l_dqsbus_x18[_lane_i] : ( \
                                                           DQS_BUS_MODE_ENUM == "DQS_BUS_MODE_X32_X36"  ? t2l_dqsbus_x36[_lane_i] : ( \
                                                                                                                                    2'b0 ))))))

`define _get_pin_dqs_x4_mode_0                          ( (DQS_BUS_MODE_ENUM != "DQS_BUS_MODE_X4") ? "dqs_x4_not_used" : "dqs_x4_a" )
`define _get_pin_dqs_x4_mode_1                          ( (DQS_BUS_MODE_ENUM != "DQS_BUS_MODE_X4") ? "dqs_x4_not_used" : "dqs_x4_a" )
`define _get_pin_dqs_x4_mode_2                          ( (DQS_BUS_MODE_ENUM != "DQS_BUS_MODE_X4") ? "dqs_x4_not_used" : "dqs_x4_a" )
`define _get_pin_dqs_x4_mode_3                          ( (DQS_BUS_MODE_ENUM != "DQS_BUS_MODE_X4") ? "dqs_x4_not_used" : "dqs_x4_a" )
`define _get_pin_dqs_x4_mode_4                          ( (DQS_BUS_MODE_ENUM != "DQS_BUS_MODE_X4") ? "dqs_x4_not_used" : "dqs_x4_a" )
`define _get_pin_dqs_x4_mode_5                          ( (DQS_BUS_MODE_ENUM != "DQS_BUS_MODE_X4") ? "dqs_x4_not_used" : "dqs_x4_a" )
`define _get_pin_dqs_x4_mode_6                          ( (DQS_BUS_MODE_ENUM != "DQS_BUS_MODE_X4") ? "dqs_x4_not_used" : "dqs_x4_b" )
`define _get_pin_dqs_x4_mode_7                          ( (DQS_BUS_MODE_ENUM != "DQS_BUS_MODE_X4") ? "dqs_x4_not_used" : "dqs_x4_b" )
`define _get_pin_dqs_x4_mode_8                          ( (DQS_BUS_MODE_ENUM != "DQS_BUS_MODE_X4") ? "dqs_x4_not_used" : "dqs_x4_a" )
`define _get_pin_dqs_x4_mode_9                          ( (DQS_BUS_MODE_ENUM != "DQS_BUS_MODE_X4") ? "dqs_x4_not_used" : "dqs_x4_a" )
`define _get_pin_dqs_x4_mode_10                         ( (DQS_BUS_MODE_ENUM != "DQS_BUS_MODE_X4") ? "dqs_x4_not_used" : "dqs_x4_b" )
`define _get_pin_dqs_x4_mode_11                         ( (DQS_BUS_MODE_ENUM != "DQS_BUS_MODE_X4") ? "dqs_x4_not_used" : "dqs_x4_b" )

// DBC Mux Scheme (non-ping-pong):
//
// Tiles above      :   switch0 = don't-care   dbc*_sel = switch1 (lower mux)
//                      switch1 = from lower
//
// AC Tile          :   switch0 = local        dbc*_sel = switch0 (upper mux)
//                      switch1 = local  
//
// Tiles below      :   switch0 = from upper   dbc*_sel = switch0 (upper mux)
//                      switch1 = don't-care
//
`define _get_ctrl2dbc_switch_0_non_pp(_tile_i)             ( (_tile_i == PRI_AC_TILE_INDEX) ? "local_tile_dbc0" : ( \
                                                             (_tile_i <= PRI_AC_TILE_INDEX) ? "upper_tile_dbc0" : ( \
                                                                                              "lower_tile_dbc0" )))

`define _get_ctrl2dbc_switch_1_non_pp(_tile_i)             ( (_tile_i == PRI_AC_TILE_INDEX) ? "local_tile_dbc1" : ( \
                                                             (_tile_i >  PRI_AC_TILE_INDEX) ? "lower_tile_dbc1" : ( \
                                                                                              "upper_tile_dbc1" )))

`define _get_ctrl2dbc_sel_0_non_pp(_tile_i)                ( (_tile_i <= PRI_AC_TILE_INDEX) ? "upper_mux_dbc0" : "lower_mux_dbc0" )
`define _get_ctrl2dbc_sel_1_non_pp(_tile_i)                ( (_tile_i <= PRI_AC_TILE_INDEX) ? "upper_mux_dbc1" : "lower_mux_dbc1" )
`define _get_ctrl2dbc_sel_2_non_pp(_tile_i)                ( (_tile_i <= PRI_AC_TILE_INDEX) ? "upper_mux_dbc2" : "lower_mux_dbc2" )
`define _get_ctrl2dbc_sel_3_non_pp(_tile_i)                ( (_tile_i <= PRI_AC_TILE_INDEX) ? "upper_mux_dbc3" : "lower_mux_dbc3" )

// DBC Mux Scheme (ping-pong):
//
// Tiles above      :   switch0 = don't-care   dbc*_sel = switch1 (lower mux)
//                      switch1 = from lower
//
// Primary AC Tile  :   switch0 = local        dbc*_sel = switch1 (lower mux)
//                      switch1 = local  
//
// Secondary AC Tile:   switch0 = local        dbc2_sel, dbc3_sel = switch0 (upper mux)
//                      switch1 = from upper   dbc0_sel, dbc1_sel = switch1 (lower mux)
//
// Tiles below      :   switch0 = from upper   dbc*_sel = switch0 (upper mux)
//                      switch1 = don't-care
//
`define _get_ctrl2dbc_switch_0_pp(_tile_i)               ( (_tile_i == PRI_AC_TILE_INDEX) ? "local_tile_dbc0" : ( \
                                                           (_tile_i == SEC_AC_TILE_INDEX) ? "local_tile_dbc0" : ( \
                                                           (_tile_i <  SEC_AC_TILE_INDEX) ? "upper_tile_dbc0" : ( \
                                                                                            "lower_tile_dbc0" ))))

`define _get_ctrl2dbc_switch_1_pp(_tile_i)               ( (_tile_i == PRI_AC_TILE_INDEX) ? "local_tile_dbc1" : ( \
                                                           (_tile_i == SEC_AC_TILE_INDEX) ? "upper_tile_dbc1" : ( \
                                                           (_tile_i >  PRI_AC_TILE_INDEX) ? "lower_tile_dbc1" : ( \
                                                                                            "upper_tile_dbc1" ))))

`define _get_ctrl2dbc_sel_0_pp(_tile_i)                  ( (_tile_i >= PRI_AC_TILE_INDEX) ? "lower_mux_dbc0" : ((_tile_i < SEC_AC_TILE_INDEX) ? "upper_mux_dbc0" : (`_get_ac_tile_index(_tile_i, 0) == PRI_AC_TILE_INDEX ? "lower_mux_dbc0" : "upper_mux_dbc0")) )
`define _get_ctrl2dbc_sel_1_pp(_tile_i)                  ( (_tile_i >= PRI_AC_TILE_INDEX) ? "lower_mux_dbc1" : ((_tile_i < SEC_AC_TILE_INDEX) ? "upper_mux_dbc1" : (`_get_ac_tile_index(_tile_i, 1) == PRI_AC_TILE_INDEX ? "lower_mux_dbc1" : "upper_mux_dbc1")) )
`define _get_ctrl2dbc_sel_2_pp(_tile_i)                  ( (_tile_i >= PRI_AC_TILE_INDEX) ? "lower_mux_dbc2" : ((_tile_i < SEC_AC_TILE_INDEX) ? "upper_mux_dbc2" : (`_get_ac_tile_index(_tile_i, 2) == PRI_AC_TILE_INDEX ? "lower_mux_dbc2" : "upper_mux_dbc2")) )
`define _get_ctrl2dbc_sel_3_pp(_tile_i)                  ( (_tile_i >= PRI_AC_TILE_INDEX) ? "lower_mux_dbc3" : ((_tile_i < SEC_AC_TILE_INDEX) ? "upper_mux_dbc3" : (`_get_ac_tile_index(_tile_i, 3) == PRI_AC_TILE_INDEX ? "lower_mux_dbc3" : "upper_mux_dbc3")) )

// DBC Mux Scheme (ping-pong and non-ping-pong)
`define _get_ctrl2dbc_switch_0(_tile_i)                  ( PHY_PING_PONG_EN ? `_get_ctrl2dbc_switch_0_pp(_tile_i) : `_get_ctrl2dbc_switch_0_non_pp(_tile_i) )
`define _get_ctrl2dbc_switch_1(_tile_i)                  ( PHY_PING_PONG_EN ? `_get_ctrl2dbc_switch_1_pp(_tile_i) : `_get_ctrl2dbc_switch_1_non_pp(_tile_i) )
`define _get_ctrl2dbc_sel_0(_tile_i)                     ( PHY_PING_PONG_EN ? `_get_ctrl2dbc_sel_0_pp(_tile_i)    : `_get_ctrl2dbc_sel_0_non_pp(_tile_i) )
`define _get_ctrl2dbc_sel_1(_tile_i)                     ( PHY_PING_PONG_EN ? `_get_ctrl2dbc_sel_1_pp(_tile_i)    : `_get_ctrl2dbc_sel_1_non_pp(_tile_i) )
`define _get_ctrl2dbc_sel_2(_tile_i)                     ( PHY_PING_PONG_EN ? `_get_ctrl2dbc_sel_2_pp(_tile_i)    : `_get_ctrl2dbc_sel_2_non_pp(_tile_i) )
`define _get_ctrl2dbc_sel_3(_tile_i)                     ( PHY_PING_PONG_EN ? `_get_ctrl2dbc_sel_3_pp(_tile_i)    : `_get_ctrl2dbc_sel_3_non_pp(_tile_i) )

// Select which DBC to use as shadow.
// For the primary HMC tile or non-Ping-Pong HMC tile, pick "dbc1_to_local" as it's guaranteed to be used by the interface (as an A/C lane).
//    The exception is for HPS mode - HPS is only connected to lane 3 of the HMC tile for the 
//    various Avalon control signals, therefore we must denote lane 3 as shadow. 
//    (note that HPS doesn't support Ping-Pong so we only need to handle non-Ping-Pong case)
// For the secondary HMC tile, which one we pick depends on which data lane in the tile belongs to the secondary interface.
`define _get_hmc_dbc2ctrl_sel_non_pp(_tile_i)            ( PRI_HMC_DBC_SHADOW_LANE_INDEX == 0 ? "dbc0_to_local" : ( \
                                                           PRI_HMC_DBC_SHADOW_LANE_INDEX == 1 ? "dbc1_to_local" : ( \
                                                           PRI_HMC_DBC_SHADOW_LANE_INDEX == 2 ? "dbc2_to_local" : ( \
                                                                                                "dbc3_to_local" ))))

`define _get_hmc_dbc2ctrl_sel_pp(_tile_i)                ( (_tile_i != SEC_AC_TILE_INDEX) ? `_get_hmc_dbc2ctrl_sel_non_pp(_tile_i) : ( \
                                                           (`_get_ac_tile_index(SEC_AC_TILE_INDEX, 0) == SEC_AC_TILE_INDEX) ? "dbc0_to_local" : ( \
                                                           (`_get_ac_tile_index(SEC_AC_TILE_INDEX, 1) == SEC_AC_TILE_INDEX) ? "dbc1_to_local" : ( \
                                                           (`_get_ac_tile_index(SEC_AC_TILE_INDEX, 2) == SEC_AC_TILE_INDEX) ? "dbc2_to_local" : ( \
                                                                                                                              "dbc3_to_local" )))))
`define _get_hmc_dbc2ctrl_sel(_tile_i)                   ( PHY_PING_PONG_EN ? `_get_hmc_dbc2ctrl_sel_pp(_tile_i) : `_get_hmc_dbc2ctrl_sel_non_pp(_tile_i) )

// ac_hmc is hard connectivity between HMC and A/C lanes
// The Fitter uses ac_hmc as a special connection to locate the A/C tile and lanes, regardless of whether HMC is used.
// Normally, we only connect these to lanes that are used as A/C, regardless of HMC or SMC.
// For HPS non-ECC, we must ensure even the unused lane 3 is connected so that the lane isn't swept away by Fitter
`define _get_ac_hmc(_tile_i, _lane_i)                    ( (`_get_lane_usage(_tile_i, _lane_i) == LANE_USAGE_AC_HMC || \
                                                            `_get_lane_usage(_tile_i, _lane_i) == LANE_USAGE_AC_CORE || \
                                                            (`_get_lane_usage(_tile_i, _lane_i) == LANE_USAGE_UNUSED && IS_HPS && _tile_i == PRI_AC_TILE_INDEX)) ? \
                                                            t2l_ac_hmc[lane_i] : 96'b0 )

// core2dbc_wr_data_vld needs to fanout to every data lane and also the A/C lane denoted as shadow by _get_hmc_dbc2ctrl_sel
`define _get_core2dbc_wr_data_vld_of_hmc(_tile_i, _lane_i)    ( (`_get_ac_tile_index(_tile_i, _lane_i) == PRI_AC_TILE_INDEX ? core2l_wr_data_vld_ast_0 : core2l_wr_data_vld_ast_1 ) )
`define _get_core2dbc_wr_data_vld(_tile_i, _lane_i)           ( ((`_get_lane_usage(_tile_i, _lane_i) == LANE_USAGE_WRDATA) || \
                                                                 (_lane_i == 0 && `_get_lane_usage(_tile_i, 0) == LANE_USAGE_AC_HMC && `_get_hmc_dbc2ctrl_sel(_tile_i) == "dbc0_to_local") || \
                                                                 (_lane_i == 1 && `_get_lane_usage(_tile_i, 1) == LANE_USAGE_AC_HMC && `_get_hmc_dbc2ctrl_sel(_tile_i) == "dbc1_to_local") || \
                                                                 (_lane_i == 2 && `_get_lane_usage(_tile_i, 2) == LANE_USAGE_AC_HMC && `_get_hmc_dbc2ctrl_sel(_tile_i) == "dbc2_to_local") || \
                                                                 (_lane_i == 3 && `_get_lane_usage(_tile_i, 3) == LANE_USAGE_AC_HMC && `_get_hmc_dbc2ctrl_sel(_tile_i) == "dbc3_to_local")) ? \
                                                                  `_get_core2dbc_wr_data_vld_of_hmc(_tile_i, _lane_i) : 1'b0 )

// core2dbc_rd_data_rdy needs to fanout to every data lane and also the lane denoted as shadow by _get_hmc_dbc2ctrl_sel
`define _get_core2dbc_rd_data_rdy_of_hmc(_tile_i, _lane_i)    ( (`_get_ac_tile_index(_tile_i, _lane_i) == PRI_AC_TILE_INDEX ? core2l_rd_data_rdy_ast_0 : core2l_rd_data_rdy_ast_1 ) )
`define _get_core2dbc_rd_data_rdy(_tile_i, _lane_i)           ( ((`_get_lane_usage(_tile_i, _lane_i) == LANE_USAGE_WRDATA) || \
                                                                 (_lane_i == 0 && `_get_lane_usage(_tile_i, 0) == LANE_USAGE_AC_HMC && `_get_hmc_dbc2ctrl_sel(_tile_i) == "dbc0_to_local") || \
                                                                 (_lane_i == 1 && `_get_lane_usage(_tile_i, 1) == LANE_USAGE_AC_HMC && `_get_hmc_dbc2ctrl_sel(_tile_i) == "dbc1_to_local") || \
                                                                 (_lane_i == 2 && `_get_lane_usage(_tile_i, 2) == LANE_USAGE_AC_HMC && `_get_hmc_dbc2ctrl_sel(_tile_i) == "dbc2_to_local") || \
                                                                 (_lane_i == 3 && `_get_lane_usage(_tile_i, 3) == LANE_USAGE_AC_HMC && `_get_hmc_dbc2ctrl_sel(_tile_i) == "dbc3_to_local")) ? \
                                                                  `_get_core2dbc_rd_data_rdy_of_hmc(_tile_i, _lane_i) : 1'b1 )

// core2dbc_wr_ecc_info needs to fanout to every data lane and also the lane denoted as shadow by _get_hmc_dbc2ctrl_sel
`define _get_core2dbc_wr_ecc_info_of_hmc(_tile_i, _lane_i)    ( (`_get_ac_tile_index(_tile_i, _lane_i) == PRI_AC_TILE_INDEX ? core2l_wr_ecc_info_0 : core2l_wr_ecc_info_1 ) )
`define _get_core2dbc_wr_ecc_info(_tile_i, _lane_i)           ( ((`_get_lane_usage(_tile_i, _lane_i) == LANE_USAGE_WRDATA) || \
                                                                 (_lane_i == 0 && `_get_lane_usage(_tile_i, 0) == LANE_USAGE_AC_HMC && `_get_hmc_dbc2ctrl_sel(_tile_i) == "dbc0_to_local") || \
                                                                 (_lane_i == 1 && `_get_lane_usage(_tile_i, 1) == LANE_USAGE_AC_HMC && `_get_hmc_dbc2ctrl_sel(_tile_i) == "dbc1_to_local") || \
                                                                 (_lane_i == 2 && `_get_lane_usage(_tile_i, 2) == LANE_USAGE_AC_HMC && `_get_hmc_dbc2ctrl_sel(_tile_i) == "dbc2_to_local") || \
                                                                 (_lane_i == 3 && `_get_lane_usage(_tile_i, 3) == LANE_USAGE_AC_HMC && `_get_hmc_dbc2ctrl_sel(_tile_i) == "dbc3_to_local")) ? \
                                                                  `_get_core2dbc_wr_ecc_info_of_hmc(_tile_i, _lane_i) : 13'b0 )

`define _get_center_tid(_tile_i)                         ( CENTER_TIDS[_tile_i * 9 +: 9] )
`define _get_hmc_tid(_tile_i)                            ( HMC_TIDS[_tile_i * 9 +: 9] )
`define _get_lane_tid(_tile_i, _lane_i)                  ( LANE_TIDS[(_tile_i * LANES_PER_TILE + _lane_i) * 9 +: 9] )

`define _get_preamble_track_dqs_enable_mode              ( PROTOCOL_ENUM == "PROTOCOL_DDR4"   ? "preamble_track_dqs_enable" : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_DDR3"   ? "preamble_track_dqs_enable" : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_LPDDR3" ? "preamble_track_dqs_enable" : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_RLD3"   ? "preamble_track_toggler" : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_QDR2"   ? "preamble_track_toggler" : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_RLD2"   ? "preamble_track_toggler" : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_QDR4"   ? "preamble_track_toggler" : ( \
                                                                                                "" ))))))))

`define _get_pst_preamble_mode                           ( PROTOCOL_ENUM == "PROTOCOL_DDR4"   ? ((DQS_BUS_MODE_ENUM == "DQS_BUS_MODE_X4") ? "ddr3_preamble" : "ddr4_preamble") : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_DDR3"   ? "ddr3_preamble" : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_LPDDR3" ? "ddr3_preamble" : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_RLD3"   ? "ddr3_preamble" : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_QDR2"   ? "ddr3_preamble" : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_RLD2"   ? "ddr3_preamble" : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_QDR4"   ? "ddr3_preamble" : ( \
                                                                                                "" ))))))))

`define _get_ddr4_search                                 "ddr3_search"
/*`define _get_ddr4_search                                 ( PROTOCOL_ENUM == "PROTOCOL_DDR4" ? "ddr4_search" : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_DDR3" ? "ddr3_search" : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_RLD3" ? "ddr3_search" : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_QDR2" ? "ddr3_search" : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_RLD2" ? "ddr3_search" : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_QDR4" ? "ddr3_search" : ( \
                                                                                              "" )))))))
*/

// Enable DQSB bus for QDR-II and for x4 mode
`define _get_dqs_b_en                                    ( (PROTOCOL_ENUM == "PROTOCOL_QDR2") || (DQS_BUS_MODE_ENUM == "DQS_BUS_MODE_X4") ? "true" : "false" )

`define _get_pst_en_shrink                               ( PROTOCOL_ENUM == "PROTOCOL_DDR4"   ? ((DQS_BUS_MODE_ENUM == "DQS_BUS_MODE_X4") ? "shrink_1_1" : "shrink_1_0") : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_DDR3"   ? "shrink_1_1" : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_LPDDR3" ? "shrink_1_1" : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_RLD3"   ? "shrink_0_1" : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_QDR2"   ? "shrink_0_1" : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_RLD2"   ? "shrink_0_1" : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_QDR4"   ? "shrink_0_1" : ( \
                                                                                              "" ))))))))

`define _get_pa_track_speed                              ( 5'h0c )

// synthesis translate_off
`define _get_dll_ctlsel                                  ( "ctl_dynamic" )
`define _get_dll_ctl_static                              ( 10'd0 )
// synthesis translate_on
// synthesis read_comments_as_HDL on
// `define _get_dll_ctlsel          ( DIAG_SYNTH_FOR_SIM ? "ctl_dynamic" : DLL_MODE          )
// `define _get_dll_ctl_static      ( DIAG_SYNTH_FOR_SIM ? 10'd0         : DLL_CODEWORD[9:0] )
// synthesis read_comments_as_HDL off   

// Enable the per-lane hard DBI circuitry. Only intended to be used by DDR4 data lanes.
`define _get_dbi_wr_en(_tile_i, _lane_i)                 ((`_get_lane_usage(_tile_i, _lane_i) == LANE_USAGE_WRDATA) ? DBI_WR_ENABLE : "false")
`define _get_dbi_rd_en(_tile_i, _lane_i)                 ((`_get_lane_usage(_tile_i, _lane_i) == LANE_USAGE_WRDATA) ? DBI_RD_ENABLE : "false")

// Enable the per-lane hard CRC circuitry. Only intended to be used by DDR4 data lanes.
`define _get_crc_en(_tile_i, _lane_i)                    ((`_get_lane_usage(_tile_i, _lane_i) == LANE_USAGE_WRDATA) ? CRC_EN : "crc_disable")

// The hard CRC circuitry needs to know how many bits to use for CRC calculation.
`define _get_crc_x4_or_x8_or_x9                          ( (PORT_MEM_DQ_WIDTH / PORT_MEM_DQS_WIDTH == 4) ? "x4_mode" : ( \
                                                           (DBI_WR_ENABLE == "true" || MEM_DATA_MASK_EN) ? "x9_mode" : ( \
                                                                                                           "x8_mode"   )) )

// Map CSR Bit Position tp each data pin
//In x4 mode only first 4 pins are required
`define _get_crc_pin_pos_0				                  1
`define _get_crc_pin_pos_1				                  2
`define _get_crc_pin_pos_2				                  3
`define _get_crc_pin_pos_3				                  6
`define _get_crc_pin_pos_4				                  ((`_get_crc_x4_or_x8_or_x9 != "x4_mode") ? 7 : 4)
`define _get_crc_pin_pos_5				                  ((`_get_crc_x4_or_x8_or_x9 != "x4_mode") ? 8 : 4)
`define _get_crc_pin_pos_6				                  ((`_get_crc_x4_or_x8_or_x9 != "x4_mode") ? 9 : 4)
`define _get_crc_pin_pos_7				                  ((`_get_crc_x4_or_x8_or_x9 != "x4_mode") ? 10 : 4)
`define _get_crc_pin_pos_8				                  ((`_get_crc_x4_or_x8_or_x9 == "x9_mode") ? 11 : 4)

// Select primary or secondary HMC config
// For non-ping-pong and primary HMC of ping-pong, select primary
// For secondary HMC of ping-pong, select secondary
// For everything else, select primary
`define _sel_hmc_val(_tile_i, _pri, _sec)             ( PHY_PING_PONG_EN ? (_tile_i <= SEC_AC_TILE_INDEX ? _sec : _pri) : _pri )

// Select primary/secondary/default HMC config
// For non-ping-pong and primary HMC of ping-pong, select primary
// For secondary HMC of ping-pong, select secondary
// For everything else, select default
`define _sel_hmc_def(_tile_i, _pri, _sec, _def)       ( PHY_PING_PONG_EN ? ((_tile_i == SEC_AC_TILE_INDEX) ? _sec : (_tile_i == PRI_AC_TILE_INDEX) ? _pri : _def) : _pri )

// Select primary or secondary HMC config, with lane dependence
// For non-ping-pong and primary HMC of ping-pong, select primary
// For secondary HMC of ping-pong, select primary or secondary based on lane affiliation 
`define _sel_hmc_lane(_tile_i, _lane_i, _pri, _sec)   ( (PHY_PING_PONG_EN && (_tile_i < SEC_AC_TILE_INDEX || (_tile_i == SEC_AC_TILE_INDEX && _lane_i < 2))) ? _sec : _pri )

module altera_emif_arch_nf_io_tiles #(
   parameter DIAG_SYNTH_FOR_SIM                      = 0,
   parameter DIAG_CPA_OUT_1_EN                       = 0,
   parameter DIAG_FAST_SIM                           = 0,
   parameter IS_HPS                                  = 0,
   parameter SILICON_REV                             = "",
   parameter PROTOCOL_ENUM                           = "",
   parameter PHY_PING_PONG_EN                        = 0,
   parameter DQS_BUS_MODE_ENUM                       = "",
   parameter USER_CLK_RATIO                          = 1,
   parameter PHY_HMC_CLK_RATIO                       = 1,
   parameter C2P_P2C_CLK_RATIO                       = 1,
   parameter PLL_VCO_TO_MEM_CLK_FREQ_RATIO           = 1,
   parameter PLL_VCO_FREQ_MHZ_INT                    = 0,
   parameter MEM_BURST_LENGTH                        = 0,
   parameter MEM_DATA_MASK_EN                        = 1,
   parameter PINS_PER_LANE                           = 1,
   parameter LANES_PER_TILE                          = 1,
   parameter PINS_IN_RTL_TILES                       = 1,
   parameter LANES_IN_RTL_TILES                      = 1,
   parameter NUM_OF_RTL_TILES                        = 1,
   parameter AC_PIN_MAP_SCHEME                       = "",
   parameter PRI_AC_TILE_INDEX                       = -1,
   parameter SEC_AC_TILE_INDEX                       = -1,
   parameter PRI_HMC_DBC_SHADOW_LANE_INDEX           = -1,
   parameter NUM_OF_HMC_PORTS                        = 1,
   parameter HMC_AVL_PROTOCOL_ENUM                   = "",
   parameter HMC_CTRL_DIMM_TYPE                      = "",
   parameter           PRI_HMC_CFG_ENABLE_ECC                      = "",
   parameter           PRI_HMC_CFG_REORDER_DATA                    = "",
   parameter           PRI_HMC_CFG_REORDER_READ                    = "",
   parameter           PRI_HMC_CFG_REORDER_RDATA                   = "",
   parameter [  5:  0] PRI_HMC_CFG_STARVE_LIMIT                    = 0,
   parameter           PRI_HMC_CFG_DQS_TRACKING_EN                 = "",
   parameter           PRI_HMC_CFG_ARBITER_TYPE                    = "",
   parameter           PRI_HMC_CFG_OPEN_PAGE_EN                    = "",
   parameter           PRI_HMC_CFG_GEAR_DOWN_EN                    = "",
   parameter           PRI_HMC_CFG_RLD3_MULTIBANK_MODE             = "",
   parameter           PRI_HMC_CFG_PING_PONG_MODE                  = "",
   parameter [  1:  0] PRI_HMC_CFG_SLOT_ROTATE_EN                  = 0,
   parameter [  1:  0] PRI_HMC_CFG_SLOT_OFFSET                     = 0,
   parameter [  3:  0] PRI_HMC_CFG_COL_CMD_SLOT                    = 0,
   parameter [  3:  0] PRI_HMC_CFG_ROW_CMD_SLOT                    = 0,
   parameter           PRI_HMC_CFG_ENABLE_RC                       = "",
   parameter [ 15:  0] PRI_HMC_CFG_CS_TO_CHIP_MAPPING              = 0,
   parameter [  6:  0] PRI_HMC_CFG_RB_RESERVED_ENTRY               = 0,
   parameter [  6:  0] PRI_HMC_CFG_WB_RESERVED_ENTRY               = 0,
   parameter [  6:  0] PRI_HMC_CFG_TCL                             = 0,
   parameter [  5:  0] PRI_HMC_CFG_POWER_SAVING_EXIT_CYC           = 0,
   parameter [  5:  0] PRI_HMC_CFG_MEM_CLK_DISABLE_ENTRY_CYC       = 0,
   parameter [ 15:  0] PRI_HMC_CFG_WRITE_ODT_CHIP                  = 0,
   parameter [ 15:  0] PRI_HMC_CFG_READ_ODT_CHIP                   = 0,
   parameter [  5:  0] PRI_HMC_CFG_WR_ODT_ON                       = 0,
   parameter [  5:  0] PRI_HMC_CFG_RD_ODT_ON                       = 0,
   parameter [  5:  0] PRI_HMC_CFG_WR_ODT_PERIOD                   = 0,
   parameter [  5:  0] PRI_HMC_CFG_RD_ODT_PERIOD                   = 0,
   parameter [ 15:  0] PRI_HMC_CFG_RLD3_REFRESH_SEQ0               = 0,
   parameter [ 15:  0] PRI_HMC_CFG_RLD3_REFRESH_SEQ1               = 0,
   parameter [ 15:  0] PRI_HMC_CFG_RLD3_REFRESH_SEQ2               = 0,
   parameter [ 15:  0] PRI_HMC_CFG_RLD3_REFRESH_SEQ3               = 0,
   parameter           PRI_HMC_CFG_SRF_ZQCAL_DISABLE               = "",
   parameter           PRI_HMC_CFG_MPS_ZQCAL_DISABLE               = "",
   parameter           PRI_HMC_CFG_MPS_DQSTRK_DISABLE              = "",
   parameter           PRI_HMC_CFG_SHORT_DQSTRK_CTRL_EN            = "",
   parameter           PRI_HMC_CFG_PERIOD_DQSTRK_CTRL_EN           = "",
   parameter [ 15:  0] PRI_HMC_CFG_PERIOD_DQSTRK_INTERVAL          = 0,
   parameter [  7:  0] PRI_HMC_CFG_DQSTRK_TO_VALID_LAST            = 0,
   parameter [  7:  0] PRI_HMC_CFG_DQSTRK_TO_VALID                 = 0,
   parameter [  6:  0] PRI_HMC_CFG_RFSH_WARN_THRESHOLD             = 0,
   parameter           PRI_HMC_CFG_SB_CG_DISABLE                   = "",
   parameter           PRI_HMC_CFG_USER_RFSH_EN                    = "",
   parameter           PRI_HMC_CFG_SRF_AUTOEXIT_EN                 = "",
   parameter           PRI_HMC_CFG_SRF_ENTRY_EXIT_BLOCK            = "",
   parameter [ 19:  0] PRI_HMC_CFG_SB_DDR4_MR3                     = 0,
   parameter [ 19:  0] PRI_HMC_CFG_SB_DDR4_MR4                     = 0,
   parameter [ 15:  0] PRI_HMC_CFG_SB_DDR4_MR5                     = 0,
   parameter [  0:  0] PRI_HMC_CFG_DDR4_MPS_ADDR_MIRROR            = 0,
   parameter           PRI_HMC_CFG_MEM_IF_COLADDR_WIDTH            = "",
   parameter           PRI_HMC_CFG_MEM_IF_ROWADDR_WIDTH            = "",
   parameter           PRI_HMC_CFG_MEM_IF_BANKADDR_WIDTH           = "",
   parameter           PRI_HMC_CFG_MEM_IF_BGADDR_WIDTH             = "",
   parameter           PRI_HMC_CFG_LOCAL_IF_CS_WIDTH               = "",
   parameter           PRI_HMC_CFG_ADDR_ORDER                      = "",
   parameter [  5:  0] PRI_HMC_CFG_ACT_TO_RDWR                     = 0,
   parameter [  5:  0] PRI_HMC_CFG_ACT_TO_PCH                      = 0,
   parameter [  5:  0] PRI_HMC_CFG_ACT_TO_ACT                      = 0,
   parameter [  5:  0] PRI_HMC_CFG_ACT_TO_ACT_DIFF_BANK            = 0,
   parameter [  5:  0] PRI_HMC_CFG_ACT_TO_ACT_DIFF_BG              = 0,
   parameter [  5:  0] PRI_HMC_CFG_RD_TO_RD                        = 0,
   parameter [  5:  0] PRI_HMC_CFG_RD_TO_RD_DIFF_CHIP              = 0,
   parameter [  5:  0] PRI_HMC_CFG_RD_TO_RD_DIFF_BG                = 0,
   parameter [  5:  0] PRI_HMC_CFG_RD_TO_WR                        = 0,
   parameter [  5:  0] PRI_HMC_CFG_RD_TO_WR_DIFF_CHIP              = 0,
   parameter [  5:  0] PRI_HMC_CFG_RD_TO_WR_DIFF_BG                = 0,
   parameter [  5:  0] PRI_HMC_CFG_RD_TO_PCH                       = 0,
   parameter [  5:  0] PRI_HMC_CFG_RD_AP_TO_VALID                  = 0,
   parameter [  5:  0] PRI_HMC_CFG_WR_TO_WR                        = 0,
   parameter [  5:  0] PRI_HMC_CFG_WR_TO_WR_DIFF_CHIP              = 0,
   parameter [  5:  0] PRI_HMC_CFG_WR_TO_WR_DIFF_BG                = 0,
   parameter [  5:  0] PRI_HMC_CFG_WR_TO_RD                        = 0,
   parameter [  5:  0] PRI_HMC_CFG_WR_TO_RD_DIFF_CHIP              = 0,
   parameter [  5:  0] PRI_HMC_CFG_WR_TO_RD_DIFF_BG                = 0,
   parameter [  5:  0] PRI_HMC_CFG_WR_TO_PCH                       = 0,
   parameter [  5:  0] PRI_HMC_CFG_WR_AP_TO_VALID                  = 0,
   parameter [  5:  0] PRI_HMC_CFG_PCH_TO_VALID                    = 0,
   parameter [  5:  0] PRI_HMC_CFG_PCH_ALL_TO_VALID                = 0,
   parameter [  7:  0] PRI_HMC_CFG_ARF_TO_VALID                    = 0,
   parameter [  5:  0] PRI_HMC_CFG_PDN_TO_VALID                    = 0,
   parameter [  9:  0] PRI_HMC_CFG_SRF_TO_VALID                    = 0,
   parameter [  9:  0] PRI_HMC_CFG_SRF_TO_ZQ_CAL                   = 0,
   parameter [ 12:  0] PRI_HMC_CFG_ARF_PERIOD                      = 0,
   parameter [ 15:  0] PRI_HMC_CFG_PDN_PERIOD                      = 0,
   parameter [  8:  0] PRI_HMC_CFG_ZQCL_TO_VALID                   = 0,
   parameter [  6:  0] PRI_HMC_CFG_ZQCS_TO_VALID                   = 0,
   parameter [  3:  0] PRI_HMC_CFG_MRS_TO_VALID                    = 0,
   parameter [  9:  0] PRI_HMC_CFG_MPS_TO_VALID                    = 0,
   parameter [  3:  0] PRI_HMC_CFG_MRR_TO_VALID                    = 0,
   parameter [  4:  0] PRI_HMC_CFG_MPR_TO_VALID                    = 0,
   parameter [  3:  0] PRI_HMC_CFG_MPS_EXIT_CS_TO_CKE              = 0,
   parameter [  3:  0] PRI_HMC_CFG_MPS_EXIT_CKE_TO_CS              = 0,
   parameter [  2:  0] PRI_HMC_CFG_RLD3_MULTIBANK_REF_DELAY        = 0,
   parameter [  7:  0] PRI_HMC_CFG_MMR_CMD_TO_VALID                = 0,
   parameter [  7:  0] PRI_HMC_CFG_4_ACT_TO_ACT                    = 0,
   parameter [  7:  0] PRI_HMC_CFG_16_ACT_TO_ACT                   = 0,
   
   parameter           SEC_HMC_CFG_ENABLE_ECC                      = "",
   parameter           SEC_HMC_CFG_REORDER_DATA                    = "",
   parameter           SEC_HMC_CFG_REORDER_READ                    = "",
   parameter           SEC_HMC_CFG_REORDER_RDATA                   = "",
   parameter [  5:  0] SEC_HMC_CFG_STARVE_LIMIT                    = 0,
   parameter           SEC_HMC_CFG_DQS_TRACKING_EN                 = "",
   parameter           SEC_HMC_CFG_ARBITER_TYPE                    = "",
   parameter           SEC_HMC_CFG_OPEN_PAGE_EN                    = "",
   parameter           SEC_HMC_CFG_GEAR_DOWN_EN                    = "",
   parameter           SEC_HMC_CFG_RLD3_MULTIBANK_MODE             = "",
   parameter           SEC_HMC_CFG_PING_PONG_MODE                  = "",
   parameter [  1:  0] SEC_HMC_CFG_SLOT_ROTATE_EN                  = 0,
   parameter [  1:  0] SEC_HMC_CFG_SLOT_OFFSET                     = 0,
   parameter [  3:  0] SEC_HMC_CFG_COL_CMD_SLOT                    = 0,
   parameter [  3:  0] SEC_HMC_CFG_ROW_CMD_SLOT                    = 0,
   parameter           SEC_HMC_CFG_ENABLE_RC                       = "",
   parameter [ 15:  0] SEC_HMC_CFG_CS_TO_CHIP_MAPPING              = 0,
   parameter [  6:  0] SEC_HMC_CFG_RB_RESERVED_ENTRY               = 0,
   parameter [  6:  0] SEC_HMC_CFG_WB_RESERVED_ENTRY               = 0,
   parameter [  6:  0] SEC_HMC_CFG_TCL                             = 0,
   parameter [  5:  0] SEC_HMC_CFG_POWER_SAVING_EXIT_CYC           = 0,
   parameter [  5:  0] SEC_HMC_CFG_MEM_CLK_DISABLE_ENTRY_CYC       = 0,
   parameter [ 15:  0] SEC_HMC_CFG_WRITE_ODT_CHIP                  = 0,
   parameter [ 15:  0] SEC_HMC_CFG_READ_ODT_CHIP                   = 0,
   parameter [  5:  0] SEC_HMC_CFG_WR_ODT_ON                       = 0,
   parameter [  5:  0] SEC_HMC_CFG_RD_ODT_ON                       = 0,
   parameter [  5:  0] SEC_HMC_CFG_WR_ODT_PERIOD                   = 0,
   parameter [  5:  0] SEC_HMC_CFG_RD_ODT_PERIOD                   = 0,
   parameter [ 15:  0] SEC_HMC_CFG_RLD3_REFRESH_SEQ0               = 0,
   parameter [ 15:  0] SEC_HMC_CFG_RLD3_REFRESH_SEQ1               = 0,
   parameter [ 15:  0] SEC_HMC_CFG_RLD3_REFRESH_SEQ2               = 0,
   parameter [ 15:  0] SEC_HMC_CFG_RLD3_REFRESH_SEQ3               = 0,
   parameter           SEC_HMC_CFG_SRF_ZQCAL_DISABLE               = "",
   parameter           SEC_HMC_CFG_MPS_ZQCAL_DISABLE               = "",
   parameter           SEC_HMC_CFG_MPS_DQSTRK_DISABLE              = "",
   parameter           SEC_HMC_CFG_SHORT_DQSTRK_CTRL_EN            = "",
   parameter           SEC_HMC_CFG_PERIOD_DQSTRK_CTRL_EN           = "",
   parameter [ 15:  0] SEC_HMC_CFG_PERIOD_DQSTRK_INTERVAL          = 0,
   parameter [  7:  0] SEC_HMC_CFG_DQSTRK_TO_VALID_LAST            = 0,
   parameter [  7:  0] SEC_HMC_CFG_DQSTRK_TO_VALID                 = 0,
   parameter [  6:  0] SEC_HMC_CFG_RFSH_WARN_THRESHOLD             = 0,
   parameter           SEC_HMC_CFG_SB_CG_DISABLE                   = "",
   parameter           SEC_HMC_CFG_USER_RFSH_EN                    = "",
   parameter           SEC_HMC_CFG_SRF_AUTOEXIT_EN                 = "",
   parameter           SEC_HMC_CFG_SRF_ENTRY_EXIT_BLOCK            = "",
   parameter [ 19:  0] SEC_HMC_CFG_SB_DDR4_MR3                     = 0,
   parameter [ 19:  0] SEC_HMC_CFG_SB_DDR4_MR4                     = 0,
   parameter [ 15:  0] SEC_HMC_CFG_SB_DDR4_MR5                     = 0,
   parameter [  0:  0] SEC_HMC_CFG_DDR4_MPS_ADDR_MIRROR            = 0,
   parameter           SEC_HMC_CFG_MEM_IF_COLADDR_WIDTH            = "",
   parameter           SEC_HMC_CFG_MEM_IF_ROWADDR_WIDTH            = "",
   parameter           SEC_HMC_CFG_MEM_IF_BANKADDR_WIDTH           = "",
   parameter           SEC_HMC_CFG_MEM_IF_BGADDR_WIDTH             = "",
   parameter           SEC_HMC_CFG_LOCAL_IF_CS_WIDTH               = "",
   parameter           SEC_HMC_CFG_ADDR_ORDER                      = "",
   parameter [  5:  0] SEC_HMC_CFG_ACT_TO_RDWR                     = 0,
   parameter [  5:  0] SEC_HMC_CFG_ACT_TO_PCH                      = 0,
   parameter [  5:  0] SEC_HMC_CFG_ACT_TO_ACT                      = 0,
   parameter [  5:  0] SEC_HMC_CFG_ACT_TO_ACT_DIFF_BANK            = 0,
   parameter [  5:  0] SEC_HMC_CFG_ACT_TO_ACT_DIFF_BG              = 0,
   parameter [  5:  0] SEC_HMC_CFG_RD_TO_RD                        = 0,
   parameter [  5:  0] SEC_HMC_CFG_RD_TO_RD_DIFF_CHIP              = 0,
   parameter [  5:  0] SEC_HMC_CFG_RD_TO_RD_DIFF_BG                = 0,
   parameter [  5:  0] SEC_HMC_CFG_RD_TO_WR                        = 0,
   parameter [  5:  0] SEC_HMC_CFG_RD_TO_WR_DIFF_CHIP              = 0,
   parameter [  5:  0] SEC_HMC_CFG_RD_TO_WR_DIFF_BG                = 0,
   parameter [  5:  0] SEC_HMC_CFG_RD_TO_PCH                       = 0,
   parameter [  5:  0] SEC_HMC_CFG_RD_AP_TO_VALID                  = 0,
   parameter [  5:  0] SEC_HMC_CFG_WR_TO_WR                        = 0,
   parameter [  5:  0] SEC_HMC_CFG_WR_TO_WR_DIFF_CHIP              = 0,
   parameter [  5:  0] SEC_HMC_CFG_WR_TO_WR_DIFF_BG                = 0,
   parameter [  5:  0] SEC_HMC_CFG_WR_TO_RD                        = 0,
   parameter [  5:  0] SEC_HMC_CFG_WR_TO_RD_DIFF_CHIP              = 0,
   parameter [  5:  0] SEC_HMC_CFG_WR_TO_RD_DIFF_BG                = 0,
   parameter [  5:  0] SEC_HMC_CFG_WR_TO_PCH                       = 0,
   parameter [  5:  0] SEC_HMC_CFG_WR_AP_TO_VALID                  = 0,
   parameter [  5:  0] SEC_HMC_CFG_PCH_TO_VALID                    = 0,
   parameter [  5:  0] SEC_HMC_CFG_PCH_ALL_TO_VALID                = 0,
   parameter [  7:  0] SEC_HMC_CFG_ARF_TO_VALID                    = 0,
   parameter [  5:  0] SEC_HMC_CFG_PDN_TO_VALID                    = 0,
   parameter [  9:  0] SEC_HMC_CFG_SRF_TO_VALID                    = 0,
   parameter [  9:  0] SEC_HMC_CFG_SRF_TO_ZQ_CAL                   = 0,
   parameter [ 12:  0] SEC_HMC_CFG_ARF_PERIOD                      = 0,
   parameter [ 15:  0] SEC_HMC_CFG_PDN_PERIOD                      = 0,
   parameter [  8:  0] SEC_HMC_CFG_ZQCL_TO_VALID                   = 0,
   parameter [  6:  0] SEC_HMC_CFG_ZQCS_TO_VALID                   = 0,
   parameter [  3:  0] SEC_HMC_CFG_MRS_TO_VALID                    = 0,
   parameter [  9:  0] SEC_HMC_CFG_MPS_TO_VALID                    = 0,
   parameter [  3:  0] SEC_HMC_CFG_MRR_TO_VALID                    = 0,
   parameter [  4:  0] SEC_HMC_CFG_MPR_TO_VALID                    = 0,
   parameter [  3:  0] SEC_HMC_CFG_MPS_EXIT_CS_TO_CKE              = 0,
   parameter [  3:  0] SEC_HMC_CFG_MPS_EXIT_CKE_TO_CS              = 0,
   parameter [  2:  0] SEC_HMC_CFG_RLD3_MULTIBANK_REF_DELAY        = 0,
   parameter [  7:  0] SEC_HMC_CFG_MMR_CMD_TO_VALID                = 0,
   parameter [  7:  0] SEC_HMC_CFG_4_ACT_TO_ACT                    = 0,
   parameter [  7:  0] SEC_HMC_CFG_16_ACT_TO_ACT                   = 0,   
   parameter LANES_USAGE                             = 1'b0,
   parameter PINS_USAGE                              = 1'b0,
   parameter PINS_RATE                               = 1'b0,
   parameter PINS_WDB                                = 1'b0,
   parameter PINS_DB_IN_BYPASS                       = 1'b0,
   parameter PINS_DB_OUT_BYPASS                      = 1'b0,
   parameter PINS_DB_OE_BYPASS                       = 1'b0,
   parameter PINS_INVERT_WR                          = 1'b0,
   parameter PINS_INVERT_OE                          = 1'b0,
   parameter PINS_AC_HMC_DATA_OVERRIDE_ENA           = 1'b0,
   parameter PINS_DATA_IN_MODE                       = 1'b0,
   parameter PINS_OCT_MODE                           = 1'b0,
   parameter PINS_GPIO_MODE                          = 1'b0,
   parameter CENTER_TIDS                             = 1'b0,
   parameter HMC_TIDS                                = 1'b0,
   parameter LANE_TIDS                               = 1'b0,
   parameter PREAMBLE_MODE                           = "",
   parameter DBI_WR_ENABLE                           = "",
   parameter DBI_RD_ENABLE                           = "",
   parameter CRC_EN                                  = "",
   parameter SWAP_DQS_A_B                            = "",
   parameter DQS_PACK_MODE                           = "",
   parameter OCT_SIZE                                = "",
   parameter [6:0] DBC_WB_RESERVED_ENTRY             = 4,
   parameter DLL_MODE                                = "",
   parameter DLL_CODEWORD                            = 0,
   parameter PORT_MEM_DQ_WIDTH                       = 1,
   parameter PORT_MEM_DQS_WIDTH                      = 1,
   parameter PORT_DFT_NF_PA_DPRIO_REG_ADDR_WIDTH     = 1,
   parameter PORT_DFT_NF_PA_DPRIO_WRITEDATA_WIDTH    = 1,
   parameter PORT_DFT_NF_PA_DPRIO_READDATA_WIDTH     = 1
) (
   // Reset
   input  logic                                                                                  global_reset_n_int,                //__ACDS_USER_COMMNET__ Async reset signal from user
   output logic                                                                                  phy_reset_n_nonabphy,              // Async reset signal from reset circuitry in the tile

   // Signals for various signals from PLL                                                       
   input  logic                                                                                  pll_locked,                        // Indicates PLL lock status
   input  logic                                                                                  pll_dll_clk,                       // PLL -> DLL output clock
   input  logic [7:0]                                                                            phy_clk_phs,                       // FR PHY clock signals (8 phases, 45-deg apart)
   input  logic [1:0]                                                                            phy_clk,                           // {phy_clk[1], phy_clk[0]}
   input  logic                                                                                  phy_fb_clk_to_tile,                // PHY feedback clock (to tile)
   output logic                                                                                  phy_fb_clk_to_pll_nonabphy,        // PHY feedback clock (to PLL)
   
   // Core clock signals from/to the Clock Phase Alignment (CPA) block
   output logic [1:0]                                                                            core_clks_from_cpa_pri_nonabphy,   // Core clock signals from the CPA of primary interface
   output logic [1:0]                                                                            core_clks_locked_cpa_pri_nonabphy, // Core clock locked signals from the CPA of primary interface
   input  logic [1:0]                                                                            core_clks_fb_to_cpa_pri,           // Core clock feedback signals to the CPA of primary interface
   output logic [1:0]                                                                            core_clks_from_cpa_sec_nonabphy,   // Core clock signals from the CPA of secondary interface (ping-pong only)
   output logic [1:0]                                                                            core_clks_locked_cpa_sec_nonabphy, // Core clock locked signals from the CPA of secondary interface (ping-pong only)
   input  logic [1:0]                                                                            core_clks_fb_to_cpa_sec,           // Core clock feedback signals to the CPA of secondary interface (ping-pong only)

   // Avalon interfaces between core and HMC
   input  logic [59:0]                                                                           core2ctl_avl_0,
   input  logic [59:0]                                                                           core2ctl_avl_1,
   input  logic                                                                                  core2ctl_avl_rd_data_ready_0,
   input  logic                                                                                  core2ctl_avl_rd_data_ready_1,
   output logic                                                                                  ctl2core_avl_cmd_ready_0_nonabphy,
   output logic                                                                                  ctl2core_avl_cmd_ready_1_nonabphy,
   output logic [12:0]                                                                           ctl2core_avl_rdata_id_0_nonabphy,
   output logic [12:0]                                                                           ctl2core_avl_rdata_id_1_nonabphy,
   input  logic                                                                                  core2l_wr_data_vld_ast_0,
   input  logic                                                                                  core2l_wr_data_vld_ast_1,
   input  logic                                                                                  core2l_rd_data_rdy_ast_0,
   input  logic                                                                                  core2l_rd_data_rdy_ast_1,

   // Avalon interfaces between core and lanes
   output logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0]                                       l2core_rd_data_vld_avl0_nonabphy,
   output logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0]                                       l2core_wr_data_rdy_ast_nonabphy,
   
   // ECC signals between core and lanes
   input  logic [12:0]                                                                           core2l_wr_ecc_info_0,
   input  logic [12:0]                                                                           core2l_wr_ecc_info_1,
   output logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][11:0]                                 l2core_wb_pointer_for_ecc_nonabphy,
      
   // Signals between core and data lanes
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][PINS_PER_LANE * 8 - 1:0]              core2l_data,
   output logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][PINS_PER_LANE * 8 - 1:0]              l2core_data_nonabphy,
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][PINS_PER_LANE * 4 - 1:0]              core2l_oe,
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][3:0]                                  core2l_rdata_en_full,
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][15:0]                                 core2l_mrnk_read,
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][15:0]                                 core2l_mrnk_write,  
   output logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][3:0]                                  l2core_rdata_valid_nonabphy,
   output logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][5:0]                                  l2core_afi_rlat_nonabphy,
   output logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][5:0]                                  l2core_afi_wlat_nonabphy,

   // AFI signals between tile and core
   input  [16:0]                                                                                 c2t_afi,
   output [25:0]                                                                                 t2c_afi_nonabphy,   
   
   // Side-band signals between core and HMC
   input  logic [41:0]                                                                           core2ctl_sideband_0,
   output logic [13:0]                                                                           ctl2core_sideband_0_nonabphy,
   input  logic [41:0]                                                                           core2ctl_sideband_1,
   output logic [13:0]                                                                           ctl2core_sideband_1_nonabphy,

   // MMR signals between core and HMC
   output logic [33:0]                                                                           ctl2core_mmr_0_nonabphy,
   input  logic [50:0]                                                                           core2ctl_mmr_0,
   output logic [33:0]                                                                           ctl2core_mmr_1_nonabphy,
   input  logic [50:0]                                                                           core2ctl_mmr_1,

   // Signals between I/O buffers and lanes/tiles
   output logic [PINS_IN_RTL_TILES-1:0]                                                          l2b_data_nonabphy,  // lane-to-buffer data
   output logic [PINS_IN_RTL_TILES-1:0]                                                          l2b_oe_nonabphy,    // lane-to-buffer output-enable
   output logic [PINS_IN_RTL_TILES-1:0]                                                          l2b_dtc_nonabphy,   // lane-to-buffer dynamic-termination-control
   input  logic [PINS_IN_RTL_TILES-1:0]                                                          b2l_data,           // buffer-to-lane data
   input  logic [LANES_IN_RTL_TILES-1:0]                                                         b2t_dqs,            // buffer-to-tile DQS
   input  logic [LANES_IN_RTL_TILES-1:0]                                                         b2t_dqsb,           // buffer-to-tile DQSb

   // Avalon-MM bus for the calibration commands between io_aux and tiles
   input  logic                                                                                  cal_bus_clk,
   input  logic                                                                                  cal_bus_avl_read,
   input  logic                                                                                  cal_bus_avl_write,
   input  logic [19:0]                                                                           cal_bus_avl_address,
   output logic [31:0]                                                                           cal_bus_avl_read_data,
   input  logic [31:0]                                                                           cal_bus_avl_write_data,
   
   // Ports for internal test and debug
   input  logic                                                                                  pa_dprio_clk,
   input  logic                                                                                  pa_dprio_read,
   input  logic [PORT_DFT_NF_PA_DPRIO_REG_ADDR_WIDTH-1:0]                                        pa_dprio_reg_addr,
   input  logic                                                                                  pa_dprio_rst_n,
   input  logic                                                                                  pa_dprio_write,
   input  logic [PORT_DFT_NF_PA_DPRIO_WRITEDATA_WIDTH-1:0]                                       pa_dprio_writedata,
   output logic                                                                                  pa_dprio_block_select_nonabphy,
   output logic [PORT_DFT_NF_PA_DPRIO_READDATA_WIDTH-1:0]                                        pa_dprio_readdata_nonabphy
);
   timeunit 1ns;
   timeprecision 1ps;

   // Enum that defines whether a lane is used or not, and in what mode.
   // This enum type is used to encode the LANES_USAGE_MODE parameter
   // passed into the io_tiles module.
   typedef enum bit [2:0] {
      LANE_USAGE_UNUSED  = 3'b000,
      LANE_USAGE_AC_HMC  = 3'b001,
      LANE_USAGE_AC_CORE = 3'b010,
      LANE_USAGE_RDATA   = 3'b011,
      LANE_USAGE_WDATA   = 3'b100,
      LANE_USAGE_WRDATA  = 3'b101
   } LANE_USAGE;

   // Enum that defines whether a pin is used by EMIF
   // This enum type is used to encode the PINS_USAGE parameter
   // passed into the io_tiles module.
   typedef enum bit [0:0] {
      PIN_USAGE_UNUSED   = 1'b0,
      PIN_USAGE_USED     = 1'b1
   } PIN_USAGE;

   // Enum that defines whether an EMIF pin operates at SDR or DDR.
   // This enum type is used to encode the PINS_RATE parameter
   // passed into the io_tiles module.
   typedef enum bit [0:0] {
      PIN_RATE_DDR       = 1'b0,
      PIN_RATE_SDR       = 1'b1
   } PIN_RATE;
   
   // Enum that defines the direction of an EMIF pin.
   typedef enum bit [0:0] {
      PIN_OCT_STATIC_OFF = 1'b0,
      PIN_OCT_DYNAMIC    = 1'b1
   } PIN_OCT_MODE;

   // Enum that defines the write data buffer mode of an EMIF pin.
   // This enum type is used to encode the PINS_WDB parameter
   // passed into the io_tiles module.
   typedef enum bit [2:0] {
      PIN_WDB_AC_CORE      = 3'b000,
      PIN_WDB_AC_HMC       = 3'b001,
      PIN_WDB_DQS_WDB_MODE = 3'b010,
      PIN_WDB_DQS_MODE     = 3'b011,
      PIN_WDB_DM_WDB_MODE  = 3'b100,
      PIN_WDB_DM_MODE      = 3'b101,
      PIN_WDB_DQ_WDB_MODE  = 3'b110,
      PIN_WDB_DQ_MODE      = 3'b111
   } PIN_WDB;

   // Enum that defines the pin data in mode of an EMIF pin.
   // This enum type is used to encode the PINS_DATA_IN_MODE parameter
   // passed into the io_tiles module.
   typedef enum bit [2:0] {
      PIN_DATA_IN_MODE_DISABLED             = 3'b000,
      PIN_DATA_IN_MODE_SSTL_IN              = 3'b001,
      PIN_DATA_IN_MODE_LOOPBACK_IN          = 3'b010,
      PIN_DATA_IN_MODE_XOR_LOOPBACK_IN      = 3'b011,
      PIN_DATA_IN_MODE_DIFF_IN              = 3'b100,
      PIN_DATA_IN_MODE_DIFF_IN_AVL_OUT      = 3'b101,
      PIN_DATA_IN_MODE_DIFF_IN_X12_OUT      = 3'b110,
      PIN_DATA_IN_MODE_DIFF_IN_AVL_X12_OUT  = 3'b111
   } PIN_DATA_IN_MODE;
   
   // Is HMC rate converter or dual-port feature turned on?
   // This can be inferred from the clock rates at core/periphery boundary and in HMC.
   localparam USE_HMC_RC_OR_DP = (C2P_P2C_CLK_RATIO == PHY_HMC_CLK_RATIO) ? 0 : 1;

   // The VCO frequency is used to derive filter code of interpolators
   // This is capped at 650MHz intentionally for slower interfaces to ensure
   // timing closure of a hard path.
   localparam PLL_VCO_FREQ_MHZ_INT_CAPPED = PLL_VCO_FREQ_MHZ_INT < 650 ? 650 : PLL_VCO_FREQ_MHZ_INT;
   
   localparam USE_FAST_INTERPOLATOR_SIM = (PLL_VCO_FREQ_MHZ_INT < 600) ? 0 : DIAG_FAST_SIM;
      
   // Reset Signals
   // Only element at tile index PRI_AC_TILE_INDEX, corresponding to the addr/cmd tile, is used
   logic [NUM_OF_RTL_TILES-1:0] t2c_seq2core_reset_n;
   assign phy_reset_n_nonabphy = t2c_seq2core_reset_n[PRI_AC_TILE_INDEX];
   
   // The phase alignment blocks have synchronization signals between them
   logic [(NUM_OF_RTL_TILES * (LANES_PER_TILE + 1)):0] pa_sync_data_up_chain;
   logic [(NUM_OF_RTL_TILES * (LANES_PER_TILE + 1)):0] pa_sync_data_dn_chain;
   logic [(NUM_OF_RTL_TILES * (LANES_PER_TILE + 1)):0] pa_sync_clk_up_chain;
   logic [(NUM_OF_RTL_TILES * (LANES_PER_TILE + 1)):0] pa_sync_clk_dn_chain;
   assign pa_sync_data_dn_chain[NUM_OF_RTL_TILES * (LANES_PER_TILE + 1)] = 1'b1;
   assign pa_sync_clk_dn_chain [NUM_OF_RTL_TILES * (LANES_PER_TILE + 1)] = 1'b1;
   assign pa_sync_data_up_chain[0] = 1'b1;
   assign pa_sync_clk_up_chain [0] = 1'b1;
      
   // The Avalon command bus signal daisy-chains one tile to another 
   // from bottom-to-top starting from the I/O aux.
   logic [(NUM_OF_RTL_TILES * (LANES_PER_TILE + 1)):0][54:0] cal_bus_avl_up_chain;
   assign cal_bus_avl_up_chain[0][19:0]  = cal_bus_avl_address;
   assign cal_bus_avl_up_chain[0][51:20] = cal_bus_avl_write_data;
   assign cal_bus_avl_up_chain[0][52]    = cal_bus_avl_write;
   assign cal_bus_avl_up_chain[0][53]    = cal_bus_avl_read;
   assign cal_bus_avl_up_chain[0][54]    = cal_bus_clk;
      
   // The Avalon read data signal daisy-chains one tile to another
   // from top-to-bottom ending at the I/O aux.
   logic [(NUM_OF_RTL_TILES * (LANES_PER_TILE + 1)):0][31:0] cal_bus_avl_read_data_dn_chain;
   assign cal_bus_avl_read_data = cal_bus_avl_read_data_dn_chain[0];
   assign cal_bus_avl_read_data_dn_chain[NUM_OF_RTL_TILES * (LANES_PER_TILE + 1)] = 32'b0;
   
   // Broadcast signals that daisy-chain all lanes in upward and downward directions.
   logic [(NUM_OF_RTL_TILES * LANES_PER_TILE):0] broadcast_up_chain;
   logic [(NUM_OF_RTL_TILES * LANES_PER_TILE):0] broadcast_dn_chain;
   assign broadcast_dn_chain[NUM_OF_RTL_TILES * LANES_PER_TILE] = 1'b1;
   assign broadcast_up_chain[0] = 1'b1;
   
   // HMC-to-DBC signals going from tiles to lanes and between tiles
   logic [NUM_OF_RTL_TILES:0][50:0] all_tiles_ctl2dbc0_dn_chain;
   logic [NUM_OF_RTL_TILES:0][50:0] all_tiles_ctl2dbc1_up_chain;
   assign all_tiles_ctl2dbc0_dn_chain[NUM_OF_RTL_TILES] = {51{1'b1}};
   assign all_tiles_ctl2dbc1_up_chain[0] = {51{1'b1}};
   
   // Ping-Pong signals going up the column
   logic [NUM_OF_RTL_TILES:0][47:0] all_tiles_ping_pong_up_chain;
   assign all_tiles_ping_pong_up_chain[0] = {48{1'b1}};
   
   // PHY clock signals going from tiles to lanes
   logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][7:0] all_tiles_t2l_phy_clk_phs;
   logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][1:0] all_tiles_t2l_phy_clk;   

   // DLL clock from tile_ctrl to lanes
   logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0] all_tiles_dll_clk_out;

   // Outputs from the CPA inside each tile
   // In the following arrays, only elements at tile index PRI_AC_TILE_INDEX, corresponding to the addr/cmd tile, are used
   // In ping-pong configuration, the CPA inside the primary HMC tile is used, hence no need to account for secondary tile
   logic [NUM_OF_RTL_TILES-1:0][1:0] all_tiles_core_clks_out;
   logic [NUM_OF_RTL_TILES-1:0][1:0] all_tiles_core_clks_fb_in;
   logic [NUM_OF_RTL_TILES-1:0][1:0] all_tiles_core_clks_locked;
      
   assign core_clks_from_cpa_pri_nonabphy = all_tiles_core_clks_out[PRI_AC_TILE_INDEX];
   assign core_clks_locked_cpa_pri_nonabphy = all_tiles_core_clks_locked[PRI_AC_TILE_INDEX];
   assign all_tiles_core_clks_fb_in[PRI_AC_TILE_INDEX] = core_clks_fb_to_cpa_pri;

   assign core_clks_from_cpa_sec_nonabphy = PHY_PING_PONG_EN ? all_tiles_core_clks_out[SEC_AC_TILE_INDEX] : '0;
   assign core_clks_locked_cpa_sec_nonabphy = PHY_PING_PONG_EN ? all_tiles_core_clks_locked[SEC_AC_TILE_INDEX] : '0;
   generate
      if (PHY_PING_PONG_EN) begin
         assign all_tiles_core_clks_fb_in[SEC_AC_TILE_INDEX] = core_clks_fb_to_cpa_sec;
      end
   endgenerate

   // Outputs from PHY clock tree back to PLL
   // Physically, this connection needs to happen in every tile but
   // in RTL we only make this connection for the A/C tile (since we
   // only have one logical PLL)
   logic [NUM_OF_RTL_TILES-1:0] all_tiles_phy_fb_clk_to_pll;
   assign phy_fb_clk_to_pll_nonabphy = all_tiles_phy_fb_clk_to_pll[PRI_AC_TILE_INDEX];

   // Avalon signals between HMC and core
   // In the following arrays, only elements at tile index *_AC_TILE_INDEX, corresponding to the addr/cmd tile, are used
   logic [NUM_OF_RTL_TILES-1:0]       all_tiles_ctl2core_avl_cmd_ready;
   logic [NUM_OF_RTL_TILES-1:0][12:0] all_tiles_ctl2core_avl_rdata_id;

   assign ctl2core_avl_cmd_ready_0_nonabphy = all_tiles_ctl2core_avl_cmd_ready[PRI_AC_TILE_INDEX];
   assign ctl2core_avl_rdata_id_0_nonabphy  = all_tiles_ctl2core_avl_rdata_id[PRI_AC_TILE_INDEX];
   
   assign ctl2core_avl_cmd_ready_1_nonabphy = all_tiles_ctl2core_avl_cmd_ready[SEC_AC_TILE_INDEX];
   assign ctl2core_avl_rdata_id_1_nonabphy  = all_tiles_ctl2core_avl_rdata_id[SEC_AC_TILE_INDEX];
   
   // AFI signals between tile and core
   // In the following arrays, only elements at tile index PRI_AC_TILE_INDEX, corresponding to the addr/cmd tile, are used
   // Ping-Pong PHY doesn't support AFI interface so there's no need to account for SEC_AC_TILE_INDEX
   logic [NUM_OF_RTL_TILES-1:0][16:0] all_tiles_c2t_afi;
   logic [NUM_OF_RTL_TILES-1:0][25:0] all_tiles_t2c_afi;
   
   assign all_tiles_c2t_afi[PRI_AC_TILE_INDEX] = c2t_afi;
   assign t2c_afi_nonabphy = all_tiles_t2c_afi[PRI_AC_TILE_INDEX];

   // Sideband signals between HMC and core
   // In the following arrays, only elements at tile index *_AC_TILE_INDEX, corresponding to the addr/cmd tile, are used
   logic [NUM_OF_RTL_TILES-1:0][13:0] all_tiles_ctl2core_sideband;

   assign ctl2core_sideband_0_nonabphy = all_tiles_ctl2core_sideband[PRI_AC_TILE_INDEX];
   assign ctl2core_sideband_1_nonabphy = all_tiles_ctl2core_sideband[SEC_AC_TILE_INDEX];

   // MMR signals between HMC and core
   // In the following arrays, only elements at tile index *_AC_TILE_INDEX, corresponding to the addr/cmd tile, are used
   logic [NUM_OF_RTL_TILES-1:0][33:0] all_tiles_ctl2core_mmr;

   assign ctl2core_mmr_0_nonabphy = all_tiles_ctl2core_mmr[PRI_AC_TILE_INDEX];
   assign ctl2core_mmr_1_nonabphy = all_tiles_ctl2core_mmr[SEC_AC_TILE_INDEX];
   
   // CPA DPRIO signals (for internal debug)
   // In the following arrays, only elements at tile index PRI_AC_TILE_INDEX, corresponding to the addr/cmd tile, are used
   logic [NUM_OF_RTL_TILES-1:0]                                          all_tiles_pa_dprio_block_select;
   logic [NUM_OF_RTL_TILES-1:0][PORT_DFT_NF_PA_DPRIO_READDATA_WIDTH-1:0] all_tiles_pa_dprio_readdata;
   
   assign pa_dprio_readdata_nonabphy = all_tiles_pa_dprio_readdata[PRI_AC_TILE_INDEX];
   assign pa_dprio_block_select_nonabphy = all_tiles_pa_dprio_block_select[PRI_AC_TILE_INDEX];
   
   // DLL reset signal 
   // Comes from the core solely when not using the HPS
   logic [2:0] core2dll;
   generate
   if (IS_HPS) begin : core2dll_hps
      assign core2dll = 3'b000;
   end else begin : core2dll_non_hps
      assign core2dll = {global_reset_n_int, 1'b0, 1'b0};
   end
   endgenerate
   
   ////////////////////////////////////////////////////////////////////////////
   // Generate tiles and lanes. 
   ////////////////////////////////////////////////////////////////////////////
   generate
      genvar tile_i, lane_i;
      for (tile_i = 0; tile_i < NUM_OF_RTL_TILES; ++tile_i)
      begin: tile_gen

         // DQS bus from tile to lanes
         logic [1:0]       t2l_dqsbus_x4 [LANES_PER_TILE-1:0]; 
         logic [1:0]       t2l_dqsbus_x8 [LANES_PER_TILE-1:0];
         logic [1:0]       t2l_dqsbus_x18 [LANES_PER_TILE-1:0];
         logic [1:0]       t2l_dqsbus_x36 [LANES_PER_TILE-1:0];

         // HMC AFI signals going to lanes.
         logic [3:0][95:0] t2l_ac_hmc;

         // HMC to Data buffer control blocks in the lanes
         logic [16:0]      t2l_cfg_dbc [LANES_PER_TILE-1:0];

         // Data buffer control blocks in the lanes to HMC
         logic [22:0]      l2t_dbc2ctl [LANES_PER_TILE-1:0];

         (* altera_attribute = "-name MAX_WIRES_FOR_CORE_PERIPHERY_TRANSFER  1" *)
         twentynm_tile_ctrl # (
            .silicon_rev                      (SILICON_REV),
            .hps_ctrl_en                      (IS_HPS ? "true" : "false"),
            .pa_filter_code                   (PLL_VCO_FREQ_MHZ_INT_CAPPED),
            .pa_phase_offset_0                (12'b0),                                             // Output clock phase degree = phase_offset / 128 * 360
            .pa_phase_offset_1                (12'b0),                                             // Output clock phase degree = phase_offset / 128 * 360
            .pa_exponent_0                    (`_get_pa_exponent_0),                               // Output clock freq = VCO Freq / ( 1.mantissa * 2^exponent)
            .pa_exponent_1                    (`_get_pa_exponent_1),                               // Output clock freq = VCO Freq / ( 1.mantissa * 2^exponent)
            .pa_mantissa_0                    (5'b0),                                              // Output clock freq = VCO Freq / ( 1.mantissa * 2^exponent)
            .pa_mantissa_1                    (5'b0),                                              // Output clock freq = VCO Freq / ( 1.mantissa * 2^exponent)
            .pa_feedback_divider_c0           (`_get_pa_feedback_divider_c0),                      // Core clock 0 divider (either 1 or 2)
            .pa_feedback_divider_c1           ("div_by_1_c1"),                                     // Core clock 1 divider (always 1)
            .pa_feedback_divider_p0           (`_get_pa_feedback_divider_p0),                      // PHY clock 0 divider (either 1 or 2)
            .pa_feedback_divider_p1           ("div_by_1_p1"),                                     // PHY clock 1 divider (always 1)
            .pa_feedback_mux_sel_0            ("fb2_p_clk_0"),                                     // Use phy_clk[2] as feedback
            .pa_feedback_mux_sel_1            (DIAG_CPA_OUT_1_EN ? "fb0_p_clk_1" : "fb2_p_clk_1"), // Use phy_clk[2] as feedback, unless in dual-CPA characterization mode
            .pa_freq_track_speed              (4'hd),
            .pa_track_speed                   (`_get_pa_track_speed),                                         
            .pa_sync_control                  ("no_sync"),
            .pa_sync_latency                  (4'b0000),                                       
            .hmc_ck_inv                       ("disable"),
            .hmc_cfg_wdata_driver_sel         ("core_w"),
            .hmc_cfg_prbs_ctrl_sel            ("hmc"),
            .hmc_cfg_mmr_driver_sel           ("core_m"),
            .hmc_cfg_loopback_en              ("disable"),
            .hmc_cfg_cmd_driver_sel           ("core_c"),
            .hmc_cfg_dbg_mode                 ("function"),
            .hmc_cfg_dbg_ctrl                 (32'b00000000000000000000000000000000),
            .hmc_cfg_bist_cmd0_u              (32'b00000000000000000000000000000000),
            .hmc_cfg_bist_cmd0_l              (32'b00000000000000000000000000000000),
            .hmc_cfg_bist_cmd1_u              (32'b00000000000000000000000000000000),
            .hmc_cfg_bist_cmd1_l              (32'b00000000000000000000000000000000),
            .hmc_cfg_dbg_out_sel              (16'b0000000000000000),
            .hmc_ctrl_mem_type                (`_get_hmc_ctrl_mem_type),
            .hmc_ctrl_dimm_type               (HMC_CTRL_DIMM_TYPE),
            .hmc_ctrl_ac_pos                  (AC_PIN_MAP_SCHEME),
            .hmc_ctrl_burst_length            (`_get_hmc_burst_length),
            .hmc_dbc0_burst_length            (`_get_dbc0_burst_length),
            .hmc_dbc1_burst_length            (`_get_dbc1_burst_length),
            .hmc_dbc2_burst_length            (`_get_dbc2_burst_length),
            .hmc_dbc3_burst_length            (`_get_dbc3_burst_length),
            .hmc_ctrl_enable_dm               (MEM_DATA_MASK_EN ? "enable" : "disable"),
            .hmc_dbc0_enable_dm               (MEM_DATA_MASK_EN ? "enable" : "disable"),
            .hmc_dbc1_enable_dm               (MEM_DATA_MASK_EN ? "enable" : "disable"),
            .hmc_dbc2_enable_dm               (MEM_DATA_MASK_EN ? "enable" : "disable"),
            .hmc_dbc3_enable_dm               (MEM_DATA_MASK_EN ? "enable" : "disable"),
            .hmc_clkgating_en                 ("disable"),                                     // Gate the clock going into hard controller to save power, if hard controller isn't used
            .hmc_ctrl_output_regd             ("disable"),                                     // Engineering option to turn on register stage to help internal timing. Currently not needed.
            .hmc_dbc0_output_regd             ("disable"),                                     // Engineering option to turn on register stage to help internal timing. Currently not needed.
            .hmc_dbc1_output_regd             ("disable"),                                     // Engineering option to turn on register stage to help internal timing. Currently not needed.
            .hmc_dbc2_output_regd             ("disable"),                                     // Engineering option to turn on register stage to help internal timing. Currently not needed.
            .hmc_dbc3_output_regd             ("disable"),                                     // Engineering option to turn on register stage to help internal timing. Currently not needed.
            .hmc_ctrl2dbc_switch0             (`_get_ctrl2dbc_switch_0(tile_i)),
            .hmc_ctrl2dbc_switch1             (`_get_ctrl2dbc_switch_1(tile_i)),
            .hmc_dbc0_ctrl_sel                (`_get_ctrl2dbc_sel_0(tile_i)),
            .hmc_dbc1_ctrl_sel                (`_get_ctrl2dbc_sel_1(tile_i)),
            .hmc_dbc2_ctrl_sel                (`_get_ctrl2dbc_sel_2(tile_i)),
            .hmc_dbc3_ctrl_sel                (`_get_ctrl2dbc_sel_3(tile_i)),
            .hmc_dbc2ctrl_sel                 (`_get_hmc_dbc2ctrl_sel(tile_i)),  
            .hmc_dbc0_pipe_lat                (3'(`_get_dbc_pipe_lat(tile_i, 0))),
            .hmc_dbc1_pipe_lat                (3'(`_get_dbc_pipe_lat(tile_i, 1))),
            .hmc_dbc2_pipe_lat                (3'(`_get_dbc_pipe_lat(tile_i, 2))),
            .hmc_dbc3_pipe_lat                (3'(`_get_dbc_pipe_lat(tile_i, 3))),
            .hmc_ctrl_cmd_rate                (`_get_hmc_cmd_rate),
            .hmc_dbc0_cmd_rate                (`_get_dbc0_cmd_rate),
            .hmc_dbc1_cmd_rate                (`_get_dbc1_cmd_rate),
            .hmc_dbc2_cmd_rate                (`_get_dbc2_cmd_rate),
            .hmc_dbc3_cmd_rate                (`_get_dbc3_cmd_rate),
            .hmc_ctrl_in_protocol             (`_get_hmc_protocol),
            .hmc_dbc0_in_protocol             (`_get_dbc0_protocol),
            .hmc_dbc1_in_protocol             (`_get_dbc1_protocol),
            .hmc_dbc2_in_protocol             (`_get_dbc2_protocol),
            .hmc_dbc3_in_protocol             (`_get_dbc3_protocol),
            .hmc_ctrl_dualport_en             ("disable"),                           // No dual-port mode support
            .hmc_dbc0_dualport_en             ("disable"),                           // No dual-port mode support
            .hmc_dbc1_dualport_en             ("disable"),                           // No dual-port mode support
            .hmc_dbc2_dualport_en             ("disable"),                           // No dual-port mode support
            .hmc_dbc3_dualport_en             ("disable"),                           // No dual-port mode support
            .hmc_tile_id                      (tile_i[4:0]),                         // HMC ID (0 for T0, 1 for T1, etc) - actual value set by Fitter based on placement
            .physeq_tile_id                   (`_get_center_tid(tile_i)),            // io_center tile ID - actual value is set by fitter based on placement 
            .physeq_bc_id_ena                 ("bc_enable"),                         // Enable broadcast mechanism
            .physeq_avl_ena                   ("avl_enable"),                        // Enable Avalon interface
            .physeq_hmc_or_core               (`_get_hmc_or_core),                   // Is HMC used?
            .physeq_trk_mgr_mrnk_mode         ("one_rank"),
            .physeq_trk_mgr_read_monitor_ena  ("disable"),                           // Must be disabled to avoid an issue with tracking manager (ICD)
            .physeq_hmc_id                    (`_get_hmc_tid(tile_i)),               // HMC tile ID - actual value is set by fitter based on placement
            .physeq_reset_auto_release        ("avl"),                               // Reset sequencer controlled via Avalon by Nios
            .physeq_rwlat_mode                ("avl_vlu"),                           // wlat/rlat set dynamically via Avalon by Nios (instead of through CSR)
            .physeq_afi_rlat_vlu              (6'b000000),                           // Unused - wlat set dynamically via Avalon by Nios
            .physeq_afi_wlat_vlu              (6'b000000),                           // Unused - rlat set dynamically via Avalon by Nios
            .hmc_second_clk_src               (USE_HMC_RC_OR_DP ? "clk1" : "clk0"),  // Use clk1 in rate-converter or dual-port mode, and clk0 otherwise
            .physeq_seq_feature               (21'b000000000000000000000),
            .hmc_ctrl_enable_ecc              (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ENABLE_ECC                , SEC_HMC_CFG_ENABLE_ECC                    )),  // Enable ECC
            .hmc_dbc0_enable_ecc              (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ENABLE_ECC                , SEC_HMC_CFG_ENABLE_ECC                    )),  // Enable ECC
            .hmc_dbc1_enable_ecc              (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ENABLE_ECC                , SEC_HMC_CFG_ENABLE_ECC                    )),  // Enable ECC
            .hmc_dbc2_enable_ecc              (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ENABLE_ECC                , SEC_HMC_CFG_ENABLE_ECC                    )),  // Enable ECC
            .hmc_dbc3_enable_ecc              (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ENABLE_ECC                , SEC_HMC_CFG_ENABLE_ECC                    )),  // Enable ECC
            .hmc_reorder_data                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_REORDER_DATA              , SEC_HMC_CFG_REORDER_DATA                  )),  // Enable command reodering
            .hmc_reorder_read                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_REORDER_READ              , SEC_HMC_CFG_REORDER_READ                  )),  // Enable read command reordering if command reordering is enabled
            .hmc_ctrl_reorder_rdata           (`_sel_hmc_val(tile_i, PRI_HMC_CFG_REORDER_RDATA             , SEC_HMC_CFG_REORDER_RDATA                 )),  // Enable in-order read data return when read command reordering is enabled
            .hmc_dbc0_reorder_rdata           (`_sel_hmc_val(tile_i, PRI_HMC_CFG_REORDER_RDATA             , SEC_HMC_CFG_REORDER_RDATA                 )),  // Enable in-order read data return when read command reordering is enabled
            .hmc_dbc1_reorder_rdata           (`_sel_hmc_val(tile_i, PRI_HMC_CFG_REORDER_RDATA             , SEC_HMC_CFG_REORDER_RDATA                 )),  // Enable in-order read data return when read command reordering is enabled
            .hmc_dbc2_reorder_rdata           (`_sel_hmc_val(tile_i, PRI_HMC_CFG_REORDER_RDATA             , SEC_HMC_CFG_REORDER_RDATA                 )),  // Enable in-order read data return when read command reordering is enabled
            .hmc_dbc3_reorder_rdata           (`_sel_hmc_val(tile_i, PRI_HMC_CFG_REORDER_RDATA             , SEC_HMC_CFG_REORDER_RDATA                 )),  // Enable in-order read data return when read command reordering is enabled
            .hmc_starve_limit                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_STARVE_LIMIT              , SEC_HMC_CFG_STARVE_LIMIT                  )),  // When command reordering is enabled, specifies the number of commands that can be served before a starved command is starved.
            .hmc_enable_dqs_tracking          (`_sel_hmc_val(tile_i, PRI_HMC_CFG_DQS_TRACKING_EN           , SEC_HMC_CFG_DQS_TRACKING_EN               )),  // Enable DQS tracking
            .hmc_arbiter_type                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ARBITER_TYPE              , SEC_HMC_CFG_ARBITER_TYPE                  )),  // Arbiter Type
            .hmc_open_page_en                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_OPEN_PAGE_EN              , SEC_HMC_CFG_OPEN_PAGE_EN                  )),  // Unused
            .hmc_geardn_en                    (`_sel_hmc_val(tile_i, PRI_HMC_CFG_GEAR_DOWN_EN              , SEC_HMC_CFG_GEAR_DOWN_EN                  )),  // Gear-down (DDR4)
            .hmc_rld3_multibank_mode          (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RLD3_MULTIBANK_MODE       , SEC_HMC_CFG_RLD3_MULTIBANK_MODE           )),  // RLD3 multi-bank mode
            .hmc_cfg_pinpong_mode             (`_sel_hmc_def(tile_i, PRI_HMC_CFG_PING_PONG_MODE            , SEC_HMC_CFG_PING_PONG_MODE ,"pingpong_off")),  // Ping-Pong PHY mode
            .hmc_ctrl_slot_rotate_en          (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SLOT_ROTATE_EN            , SEC_HMC_CFG_SLOT_ROTATE_EN                )),  // Command slot rotation
            .hmc_dbc0_slot_rotate_en          (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SLOT_ROTATE_EN            , SEC_HMC_CFG_SLOT_ROTATE_EN                )),  // Command slot rotation
            .hmc_dbc1_slot_rotate_en          (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SLOT_ROTATE_EN            , SEC_HMC_CFG_SLOT_ROTATE_EN                )),  // Command slot rotation
            .hmc_dbc2_slot_rotate_en          (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SLOT_ROTATE_EN            , SEC_HMC_CFG_SLOT_ROTATE_EN                )),  // Command slot rotation
            .hmc_dbc3_slot_rotate_en          (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SLOT_ROTATE_EN            , SEC_HMC_CFG_SLOT_ROTATE_EN                )),  // Command slot rotation
            .hmc_ctrl_slot_offset             (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SLOT_OFFSET               , SEC_HMC_CFG_SLOT_OFFSET                   )),  // Command slot offset
            .hmc_dbc0_slot_offset             (`_sel_hmc_lane(tile_i, 0, PRI_HMC_CFG_SLOT_OFFSET           , SEC_HMC_CFG_SLOT_OFFSET                   )),  // Command slot offset
            .hmc_dbc1_slot_offset             (`_sel_hmc_lane(tile_i, 1, PRI_HMC_CFG_SLOT_OFFSET           , SEC_HMC_CFG_SLOT_OFFSET                   )),  // Command slot offset
            .hmc_dbc2_slot_offset             (`_sel_hmc_lane(tile_i, 2, PRI_HMC_CFG_SLOT_OFFSET           , SEC_HMC_CFG_SLOT_OFFSET                   )),  // Command slot offset
            .hmc_dbc3_slot_offset             (`_sel_hmc_lane(tile_i, 3, PRI_HMC_CFG_SLOT_OFFSET           , SEC_HMC_CFG_SLOT_OFFSET                   )),  // Command slot offset
            .hmc_col_cmd_slot                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_COL_CMD_SLOT              , SEC_HMC_CFG_COL_CMD_SLOT                  )),  // Command slot for column commands
            .hmc_row_cmd_slot                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ROW_CMD_SLOT              , SEC_HMC_CFG_ROW_CMD_SLOT                  )),  // Command slot for row commands
            .hmc_ctrl_rc_en                   (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ENABLE_RC                 , SEC_HMC_CFG_ENABLE_RC                     )),  // Enable rate-conversion feature
            .hmc_dbc0_rc_en                   (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ENABLE_RC                 , SEC_HMC_CFG_ENABLE_RC                     )),  // Enable rate-conversion feature
            .hmc_dbc1_rc_en                   (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ENABLE_RC                 , SEC_HMC_CFG_ENABLE_RC                     )),  // Enable rate-conversion feature
            .hmc_dbc2_rc_en                   (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ENABLE_RC                 , SEC_HMC_CFG_ENABLE_RC                     )),  // Enable rate-conversion feature
            .hmc_dbc3_rc_en                   (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ENABLE_RC                 , SEC_HMC_CFG_ENABLE_RC                     )),  // Enable rate-conversion feature
            .hmc_cs_chip                      (`_sel_hmc_val(tile_i, PRI_HMC_CFG_CS_TO_CHIP_MAPPING        , SEC_HMC_CFG_CS_TO_CHIP_MAPPING            )),  // Chip select mapping scheme
            .hmc_rb_reserved_entry            (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RB_RESERVED_ENTRY         , SEC_HMC_CFG_RB_RESERVED_ENTRY             )),  // Number of entries reserved in read buffer before almost full is asserted. Should be set to 4 + 2 * user_pipe_stages
            .hmc_wb_reserved_entry            (`_sel_hmc_val(tile_i, PRI_HMC_CFG_WB_RESERVED_ENTRY         , SEC_HMC_CFG_WB_RESERVED_ENTRY             )),  // Number of entries reserved in write buffer before almost full is asserted. Should be set to 4 + 2 * user_pipe_stages
            .hmc_tcl                          (`_sel_hmc_val(tile_i, PRI_HMC_CFG_TCL                       , SEC_HMC_CFG_TCL                           )),  // Memory CAS latency
            .hmc_power_saving_exit_cycles     (`_sel_hmc_val(tile_i, PRI_HMC_CFG_POWER_SAVING_EXIT_CYC     , SEC_HMC_CFG_POWER_SAVING_EXIT_CYC         )),  // The minimum number of cycles to stay in a low power state. This applies to both power down and self-refresh and should be set to the greater of tPD and tCKESR
            .hmc_mem_clk_disable_entry_cycles (`_sel_hmc_val(tile_i, PRI_HMC_CFG_MEM_CLK_DISABLE_ENTRY_CYC , SEC_HMC_CFG_MEM_CLK_DISABLE_ENTRY_CYC     )),  // Set to a the number of clocks after the execution of an self-refresh to stop the clock.  This register is generally set based on PHY design latency and should generally not be changed
            .hmc_write_odt_chip               (`_sel_hmc_val(tile_i, PRI_HMC_CFG_WRITE_ODT_CHIP            , SEC_HMC_CFG_WRITE_ODT_CHIP                )),  // ODT scheme setting for write command
            .hmc_read_odt_chip                (`_sel_hmc_val(tile_i, PRI_HMC_CFG_READ_ODT_CHIP             , SEC_HMC_CFG_READ_ODT_CHIP                 )),  // ODT scheme setting for read command
            .hmc_wr_odt_on                    (`_sel_hmc_val(tile_i, PRI_HMC_CFG_WR_ODT_ON                 , SEC_HMC_CFG_WR_ODT_ON                     )),  // Indicates number of memory clock cycle gap between write command and ODT signal rising edge
            .hmc_rd_odt_on                    (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RD_ODT_ON                 , SEC_HMC_CFG_RD_ODT_ON                     )),  // Indicates number of memory clock cycle gap between read command and ODT signal rising edge
            .hmc_wr_odt_period                (`_sel_hmc_val(tile_i, PRI_HMC_CFG_WR_ODT_PERIOD             , SEC_HMC_CFG_WR_ODT_PERIOD                 )),  // Indicates number of memory clock cycle write ODT signal should stay asserted after rising edge
            .hmc_rd_odt_period                (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RD_ODT_PERIOD             , SEC_HMC_CFG_RD_ODT_PERIOD                 )),  // Indicates number of memory clock cycle read ODT signal should stay asserted after rising edge
            .hmc_rld3_refresh_seq0            (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RLD3_REFRESH_SEQ0         , SEC_HMC_CFG_RLD3_REFRESH_SEQ0             )),  // Banks to refresh for RLD3 in sequence 0. Must not be more than 4 banks
            .hmc_rld3_refresh_seq1            (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RLD3_REFRESH_SEQ1         , SEC_HMC_CFG_RLD3_REFRESH_SEQ1             )),  // Banks to refresh for RLD3 in sequence 1. Must not be more than 4 banks
            .hmc_rld3_refresh_seq2            (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RLD3_REFRESH_SEQ2         , SEC_HMC_CFG_RLD3_REFRESH_SEQ2             )),  // Banks to refresh for RLD3 in sequence 2. Must not be more than 4 banks
            .hmc_rld3_refresh_seq3            (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RLD3_REFRESH_SEQ3         , SEC_HMC_CFG_RLD3_REFRESH_SEQ3             )),  // Banks to refresh for RLD3 in sequence 3. Must not be more than 4 banks
            .hmc_srf_zqcal_disable            (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SRF_ZQCAL_DISABLE         , SEC_HMC_CFG_SRF_ZQCAL_DISABLE             )),  // Setting to disable ZQ Calibration after self refresh
            .hmc_mps_zqcal_disable            (`_sel_hmc_val(tile_i, PRI_HMC_CFG_MPS_ZQCAL_DISABLE         , SEC_HMC_CFG_MPS_ZQCAL_DISABLE             )),  // Setting to disable ZQ Calibration after Maximum Power Saving exit
            .hmc_short_dqstrk_ctrl_en         (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SHORT_DQSTRK_CTRL_EN      , SEC_HMC_CFG_SHORT_DQSTRK_CTRL_EN          )),  
            .hmc_period_dqstrk_ctrl_en        (`_sel_hmc_val(tile_i, PRI_HMC_CFG_PERIOD_DQSTRK_CTRL_EN     , SEC_HMC_CFG_PERIOD_DQSTRK_CTRL_EN         )),  
            .hmc_period_dqstrk_interval       (`_sel_hmc_val(tile_i, PRI_HMC_CFG_PERIOD_DQSTRK_INTERVAL    , SEC_HMC_CFG_PERIOD_DQSTRK_INTERVAL        )),  
            .hmc_dqstrk_to_valid_last         (`_sel_hmc_val(tile_i, PRI_HMC_CFG_DQSTRK_TO_VALID_LAST      , SEC_HMC_CFG_DQSTRK_TO_VALID_LAST          )),  
            .hmc_dqstrk_to_valid              (`_sel_hmc_val(tile_i, PRI_HMC_CFG_DQSTRK_TO_VALID           , SEC_HMC_CFG_DQSTRK_TO_VALID               )),  
            .hmc_rfsh_warn_threshold          (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RFSH_WARN_THRESHOLD       , SEC_HMC_CFG_RFSH_WARN_THRESHOLD           )),  
            .hmc_mps_dqstrk_disable           (`_sel_hmc_val(tile_i, PRI_HMC_CFG_MPS_DQSTRK_DISABLE        , SEC_HMC_CFG_MPS_DQSTRK_DISABLE            )),  // Setting to disable DQS Tracking after Maximum Power Saving exit
            .hmc_sb_cg_disable                (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SB_CG_DISABLE             , SEC_HMC_CFG_SB_CG_DISABLE                 )),  // Setting to disable mem_ck gating during self refresh and deep power down
            .hmc_user_rfsh_en                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_USER_RFSH_EN              , SEC_HMC_CFG_USER_RFSH_EN                  )),  // Setting to enable user refresh 
            .hmc_srf_autoexit_en              (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SRF_AUTOEXIT_EN           , SEC_HMC_CFG_SRF_AUTOEXIT_EN               )),  // Setting to enable controller to exit Self Refresh when new command is detected
            .hmc_srf_entry_exit_block         (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SRF_ENTRY_EXIT_BLOCK      , SEC_HMC_CFG_SRF_ENTRY_EXIT_BLOCK          )),  // Blocking arbiter from issuing commands
            .hmc_sb_ddr4_mr3                  (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SB_DDR4_MR3               , SEC_HMC_CFG_SB_DDR4_MR3                   )),  // DDR4 MR3
            .hmc_sb_ddr4_mr4                  (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SB_DDR4_MR4               , SEC_HMC_CFG_SB_DDR4_MR4                   )),  // DDR4 MR4
            .hmc_sb_ddr4_mr5                  (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SB_DDR4_MR5               , SEC_HMC_CFG_SB_DDR4_MR5                   )),  // DDR4 MR5
            .hmc_ddr4_mps_addr_mirror         (`_sel_hmc_val(tile_i, PRI_HMC_CFG_DDR4_MPS_ADDR_MIRROR      , SEC_HMC_CFG_DDR4_MPS_ADDR_MIRROR          )),  // DDR4 MPS Address Mirror
            .hmc_mem_if_coladdr_width         (`_sel_hmc_val(tile_i, PRI_HMC_CFG_MEM_IF_COLADDR_WIDTH      , SEC_HMC_CFG_MEM_IF_COLADDR_WIDTH          )),  // Column address width
            .hmc_mem_if_rowaddr_width         (`_sel_hmc_val(tile_i, PRI_HMC_CFG_MEM_IF_ROWADDR_WIDTH      , SEC_HMC_CFG_MEM_IF_ROWADDR_WIDTH          )),  // Row address width
            .hmc_mem_if_bankaddr_width        (`_sel_hmc_val(tile_i, PRI_HMC_CFG_MEM_IF_BANKADDR_WIDTH     , SEC_HMC_CFG_MEM_IF_BANKADDR_WIDTH         )),  // Bank address width
            .hmc_mem_if_bgaddr_width          (`_sel_hmc_val(tile_i, PRI_HMC_CFG_MEM_IF_BGADDR_WIDTH       , SEC_HMC_CFG_MEM_IF_BGADDR_WIDTH           )),  // Bank group address width
            .hmc_local_if_cs_width            (`_sel_hmc_val(tile_i, PRI_HMC_CFG_LOCAL_IF_CS_WIDTH         , SEC_HMC_CFG_LOCAL_IF_CS_WIDTH             )),  // Address width in bits required to access every CS in interface
            .hmc_addr_order                   (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ADDR_ORDER                , SEC_HMC_CFG_ADDR_ORDER                    )),  // Mapping of Avalon address to physical address of the memory device
            .hmc_act_to_rdwr                  (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ACT_TO_RDWR               , SEC_HMC_CFG_ACT_TO_RDWR                   )),  // Activate to Read/write command timing (e.g. tRCD)
            .hmc_act_to_pch                   (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ACT_TO_PCH                , SEC_HMC_CFG_ACT_TO_PCH                    )),  // Active to precharge (e.g. tRAS)
            .hmc_act_to_act                   (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ACT_TO_ACT                , SEC_HMC_CFG_ACT_TO_ACT                    )),  // Active to activate timing on same bank (e.g. tRC)
            .hmc_act_to_act_diff_bank         (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ACT_TO_ACT_DIFF_BANK      , SEC_HMC_CFG_ACT_TO_ACT_DIFF_BANK          )),  // Active to activate timing on different banks, for DDR4 same bank group (e.g. tRRD)
            .hmc_act_to_act_diff_bg           (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ACT_TO_ACT_DIFF_BG        , SEC_HMC_CFG_ACT_TO_ACT_DIFF_BG            )),  // Active to activate timing on different bank groups, DDR4 only
            .hmc_rd_to_rd                     (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RD_TO_RD                  , SEC_HMC_CFG_RD_TO_RD                      )),  // Read to read command timing on same bank (e.g. tCCD)
            .hmc_rd_to_rd_diff_chip           (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RD_TO_RD_DIFF_CHIP        , SEC_HMC_CFG_RD_TO_RD_DIFF_CHIP            )),  // Read to read command timing on different chips
            .hmc_rd_to_rd_diff_bg             (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RD_TO_RD_DIFF_BG          , SEC_HMC_CFG_RD_TO_RD_DIFF_BG              )),  // Read to read command timing on different chips
            .hmc_rd_to_wr                     (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RD_TO_WR                  , SEC_HMC_CFG_RD_TO_WR                      )),  // Read to write command timing on same bank
            .hmc_rd_to_wr_diff_chip           (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RD_TO_WR_DIFF_CHIP        , SEC_HMC_CFG_RD_TO_WR_DIFF_CHIP            )),  // Read to write command timing on different chips
            .hmc_rd_to_wr_diff_bg             (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RD_TO_WR_DIFF_BG          , SEC_HMC_CFG_RD_TO_WR_DIFF_BG              )),  // Read to write command timing on different bank groups
            .hmc_rd_to_pch                    (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RD_TO_PCH                 , SEC_HMC_CFG_RD_TO_PCH                     )),  // Read to precharge command timing (e.g. tRTP)
            .hmc_rd_ap_to_valid               (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RD_AP_TO_VALID            , SEC_HMC_CFG_RD_AP_TO_VALID                )),  // Read command with autoprecharge to data valid timing
            .hmc_wr_to_wr                     (`_sel_hmc_val(tile_i, PRI_HMC_CFG_WR_TO_WR                  , SEC_HMC_CFG_WR_TO_WR                      )),  // Write to write command timing on same bank. (e.g. tCCD)
            .hmc_wr_to_wr_diff_chip           (`_sel_hmc_val(tile_i, PRI_HMC_CFG_WR_TO_WR_DIFF_CHIP        , SEC_HMC_CFG_WR_TO_WR_DIFF_CHIP            )),  // Write to write command timing on different chips.
            .hmc_wr_to_wr_diff_bg             (`_sel_hmc_val(tile_i, PRI_HMC_CFG_WR_TO_WR_DIFF_BG          , SEC_HMC_CFG_WR_TO_WR_DIFF_BG              )),  // Write to write command timing on different bank groups.
            .hmc_wr_to_rd                     (`_sel_hmc_val(tile_i, PRI_HMC_CFG_WR_TO_RD                  , SEC_HMC_CFG_WR_TO_RD                      )),  // Write to read command timing. (e.g. tWTR)
            .hmc_wr_to_rd_diff_chip           (`_sel_hmc_val(tile_i, PRI_HMC_CFG_WR_TO_RD_DIFF_CHIP        , SEC_HMC_CFG_WR_TO_RD_DIFF_CHIP            )),  // Write to read command timing on different chips.
            .hmc_wr_to_rd_diff_bg             (`_sel_hmc_val(tile_i, PRI_HMC_CFG_WR_TO_RD_DIFF_BG          , SEC_HMC_CFG_WR_TO_RD_DIFF_BG              )),  // Write to read command timing on different bank groups
            .hmc_wr_to_pch                    (`_sel_hmc_val(tile_i, PRI_HMC_CFG_WR_TO_PCH                 , SEC_HMC_CFG_WR_TO_PCH                     )),  // Write to precharge command timing. (e.g. tWR)
            .hmc_wr_ap_to_valid               (`_sel_hmc_val(tile_i, PRI_HMC_CFG_WR_AP_TO_VALID            , SEC_HMC_CFG_WR_AP_TO_VALID                )),  // Write with autoprecharge to valid command timing.
            .hmc_pch_to_valid                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_PCH_TO_VALID              , SEC_HMC_CFG_PCH_TO_VALID                  )),  // Precharge to valid command timing. (e.g. tRP)
            .hmc_pch_all_to_valid             (`_sel_hmc_val(tile_i, PRI_HMC_CFG_PCH_ALL_TO_VALID          , SEC_HMC_CFG_PCH_ALL_TO_VALID              )),  // Precharge all to banks being ready for bank activation command.
            .hmc_arf_to_valid                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ARF_TO_VALID              , SEC_HMC_CFG_ARF_TO_VALID                  )),  // Auto Refresh to valid DRAM command window.
            .hmc_pdn_to_valid                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_PDN_TO_VALID              , SEC_HMC_CFG_PDN_TO_VALID                  )),  // Power down to valid bank command window.
            .hmc_srf_to_valid                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SRF_TO_VALID              , SEC_HMC_CFG_SRF_TO_VALID                  )),  // Self-refresh to valid bank command window. (e.g. tRFC)
            .hmc_srf_to_zq_cal                (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SRF_TO_ZQ_CAL             , SEC_HMC_CFG_SRF_TO_ZQ_CAL                 )),  // Self refresh to ZQ calibration window
            .hmc_arf_period                   (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ARF_PERIOD                , SEC_HMC_CFG_ARF_PERIOD                    )),  // Auto-refresh period (e.g. tREFI)
            .hmc_pdn_period                   (`_sel_hmc_val(tile_i, PRI_HMC_CFG_PDN_PERIOD                , SEC_HMC_CFG_PDN_PERIOD                    )),  // Number of controller cycles before automatic power down.
            .hmc_zqcl_to_valid                (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ZQCL_TO_VALID             , SEC_HMC_CFG_ZQCL_TO_VALID                 )),  // Long ZQ calibration to valid
            .hmc_zqcs_to_valid                (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ZQCS_TO_VALID             , SEC_HMC_CFG_ZQCS_TO_VALID                 )),  // Short ZQ calibration to valid
            .hmc_mrs_to_valid                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_MRS_TO_VALID              , SEC_HMC_CFG_MRS_TO_VALID                  )),  // Mode Register Setting to valid (e.g. tMRD)
            .hmc_mps_to_valid                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_MPS_TO_VALID              , SEC_HMC_CFG_MPS_TO_VALID                  )),  // Max Power Saving to Valid
            .hmc_mrr_to_valid                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_MRR_TO_VALID              , SEC_HMC_CFG_MRR_TO_VALID                  )),  // Mode Register Read to Valid
            .hmc_mpr_to_valid                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_MPR_TO_VALID              , SEC_HMC_CFG_MPR_TO_VALID                  )),  // Multi Purpose Register Read to Valid
            .hmc_mps_exit_cs_to_cke           (`_sel_hmc_val(tile_i, PRI_HMC_CFG_MPS_EXIT_CS_TO_CKE        , SEC_HMC_CFG_MPS_EXIT_CS_TO_CKE            )),  // Max Power Saving CS to CKE
            .hmc_mps_exit_cke_to_cs           (`_sel_hmc_val(tile_i, PRI_HMC_CFG_MPS_EXIT_CKE_TO_CS        , SEC_HMC_CFG_MPS_EXIT_CKE_TO_CS            )),  // Max Power Saving CKE to CS
            .hmc_rld3_multibank_ref_delay     (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RLD3_MULTIBANK_REF_DELAY  , SEC_HMC_CFG_RLD3_MULTIBANK_REF_DELAY      )),  // RLD3 Multibank Refresh Delay
            .hmc_mmr_cmd_to_valid             (`_sel_hmc_val(tile_i, PRI_HMC_CFG_MMR_CMD_TO_VALID          , SEC_HMC_CFG_MMR_CMD_TO_VALID              )),  // MMR cmd to valid delay
            .hmc_4_act_to_act                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_4_ACT_TO_ACT              , SEC_HMC_CFG_4_ACT_TO_ACT                  )),  // The four-activate window timing parameter. (e.g. tFAW)
            .hmc_16_act_to_act                (`_sel_hmc_val(tile_i, PRI_HMC_CFG_16_ACT_TO_ACT             , SEC_HMC_CFG_16_ACT_TO_ACT                 )),  // The 16-activate window timing parameter (RLD3) (e.g. tSAW)
            .mode                             ("tile_ddr")
            
         ) tile_ctrl_inst (

            // Reset
            .global_reset_n                   (global_reset_n_int),

            // PLL -> Tiles
            .pll_locked_in                    (pll_locked),
            .pll_vco_in                       (phy_clk_phs),                        // FR clocks routed on PHY clock tree
            .phy_clk_in                       (phy_clk),                            // PHY clock tree inputs

            // Clock Phase Alignment
            .pa_core_clk_in                   (all_tiles_core_clks_fb_in[tile_i]),  // Input to CPA through feedback path
            .pa_core_clk_out                  (all_tiles_core_clks_out[tile_i]),    // Output from CPA to core clock networks
            .pa_locked                        (all_tiles_core_clks_locked[tile_i]), // Lock signal from CPA to core
            .pa_reset_n                       (IS_HPS ? 1'b1 : global_reset_n_int), // Connected to global reset from core in non-HPS mode
            .pa_core_in                       (12'b000000000000),                   // Control code word
            .pa_fbclk_in                      (phy_fb_clk_to_tile),                 // PLL signal going into PHY feedback clock
            .pa_sync_data_bot_in              (pa_sync_data_up_chain[`_get_chain_index_for_tile(tile_i)]),
            .pa_sync_data_top_out             (pa_sync_data_up_chain[`_get_chain_index_for_tile(tile_i) + 1]),
            .pa_sync_data_top_in              (pa_sync_data_dn_chain[`_get_chain_index_for_tile(tile_i) + 1]),
            .pa_sync_data_bot_out             (pa_sync_data_dn_chain[`_get_chain_index_for_tile(tile_i)]),
            .pa_sync_clk_bot_in               (pa_sync_clk_up_chain [`_get_chain_index_for_tile(tile_i)]),
            .pa_sync_clk_top_out              (pa_sync_clk_up_chain [`_get_chain_index_for_tile(tile_i) + 1]),
            .pa_sync_clk_top_in               (pa_sync_clk_dn_chain [`_get_chain_index_for_tile(tile_i) + 1]), 
            .pa_sync_clk_bot_out              (pa_sync_clk_dn_chain [`_get_chain_index_for_tile(tile_i)]),
            .pa_dprio_rst_n                   ((tile_i == PRI_AC_TILE_INDEX ? pa_dprio_rst_n : 1'b0)),
            .pa_dprio_clk                     ((tile_i == PRI_AC_TILE_INDEX ? pa_dprio_clk : 1'b0)),
            .pa_dprio_read                    ((tile_i == PRI_AC_TILE_INDEX ? pa_dprio_read : 1'b0)),
            .pa_dprio_reg_addr                ((tile_i == PRI_AC_TILE_INDEX ? pa_dprio_reg_addr : 9'b0)),
            .pa_dprio_write                   ((tile_i == PRI_AC_TILE_INDEX ? pa_dprio_write : 1'b0)),
            .pa_dprio_writedata               ((tile_i == PRI_AC_TILE_INDEX ? pa_dprio_writedata : 8'b0)),
            .pa_dprio_block_select            (all_tiles_pa_dprio_block_select[tile_i]),
            .pa_dprio_readdata                (all_tiles_pa_dprio_readdata[tile_i]),
            
            // PHY clock signals going from tiles to lanes
            .phy_clk_out0                     ({all_tiles_t2l_phy_clk[tile_i][0], all_tiles_t2l_phy_clk_phs[tile_i][0]}), // PHY clocks to lane 0
            .phy_clk_out1                     ({all_tiles_t2l_phy_clk[tile_i][1], all_tiles_t2l_phy_clk_phs[tile_i][1]}), // PHY clocks to lane 1
            .phy_clk_out2                     ({all_tiles_t2l_phy_clk[tile_i][2], all_tiles_t2l_phy_clk_phs[tile_i][2]}), // PHY clocks to lane 2
            .phy_clk_out3                     ({all_tiles_t2l_phy_clk[tile_i][3], all_tiles_t2l_phy_clk_phs[tile_i][3]}), // PHY clocks to lane 3
            .phy_fbclk_out                    (all_tiles_phy_fb_clk_to_pll[tile_i]),                                      // PHY clock signal going into the M counter of PLL to complete the feedback loop

            // DLL Interface
            .dll_clk_in                       (pll_dll_clk),                       // PLL clock feeding to DLL
            .dll_clk_out0                     (all_tiles_dll_clk_out[tile_i][0]),  // DLL clock to lane 0
            .dll_clk_out1                     (all_tiles_dll_clk_out[tile_i][1]),  // DLL clock to lane 1
            .dll_clk_out2                     (all_tiles_dll_clk_out[tile_i][2]),  // DLL clock to lane 2
            .dll_clk_out3                     (all_tiles_dll_clk_out[tile_i][3]),  // DLL clock to lane 3
            
            // Calibration bus between Nios and sequencer (a.k.a slow Avalon-MM bus)
            .cal_avl_in                       (cal_bus_avl_up_chain          [`_get_chain_index_for_tile(tile_i)]),
            .cal_avl_out                      (cal_bus_avl_up_chain          [`_get_chain_index_for_tile(tile_i) + 1]),
            .cal_avl_rdata_in                 (cal_bus_avl_read_data_dn_chain[`_get_chain_index_for_tile(tile_i) + 1]),
            .cal_avl_rdata_out                (cal_bus_avl_read_data_dn_chain[`_get_chain_index_for_tile(tile_i)]),

            .core2ctl_avl0                    (`_sel_hmc_def(tile_i, core2ctl_avl_0, core2ctl_avl_1, 60'b0)),
            .core2ctl_avl1                    (60'b0),
            .core2ctl_avl_rd_data_ready       (`_sel_hmc_def(tile_i, core2ctl_avl_rd_data_ready_0, core2ctl_avl_rd_data_ready_1, 1'b0)),
            .ctl2core_avl_cmd_ready           (all_tiles_ctl2core_avl_cmd_ready[tile_i]),
            .ctl2core_avl_rdata_id            (all_tiles_ctl2core_avl_rdata_id[tile_i]),

            .core2ctl_sideband                (`_sel_hmc_def(tile_i, core2ctl_sideband_0, core2ctl_sideband_1, 42'b0)),
            .ctl2core_sideband                (all_tiles_ctl2core_sideband[tile_i]),

            // Interface between HMC and lanes
            .afi_cmd_bus                      (t2l_ac_hmc),

            // DQS buses
            // There are 8 x4 DQS buses per tile, with two pairs of input DQS per lane.
            .dqs_in_x4_a_0                    (DQS_BUS_MODE_ENUM == "DQS_BUS_MODE_X4" ? b2t_dqs[(tile_i * LANES_PER_TILE) + 0]  : 1'b0),
            .dqs_in_x4_a_1                    (DQS_BUS_MODE_ENUM == "DQS_BUS_MODE_X4" ? b2t_dqs[(tile_i * LANES_PER_TILE) + 1]  : 1'b0),
            .dqs_in_x4_a_2                    (DQS_BUS_MODE_ENUM == "DQS_BUS_MODE_X4" ? b2t_dqs[(tile_i * LANES_PER_TILE) + 2]  : 1'b0),
            .dqs_in_x4_a_3                    (DQS_BUS_MODE_ENUM == "DQS_BUS_MODE_X4" ? b2t_dqs[(tile_i * LANES_PER_TILE) + 3]  : 1'b0),
            .dqs_in_x4_b_0                    (DQS_BUS_MODE_ENUM == "DQS_BUS_MODE_X4" ? b2t_dqsb[(tile_i * LANES_PER_TILE) + 0] : 1'b0),
            .dqs_in_x4_b_1                    (DQS_BUS_MODE_ENUM == "DQS_BUS_MODE_X4" ? b2t_dqsb[(tile_i * LANES_PER_TILE) + 1] : 1'b0),
            .dqs_in_x4_b_2                    (DQS_BUS_MODE_ENUM == "DQS_BUS_MODE_X4" ? b2t_dqsb[(tile_i * LANES_PER_TILE) + 2] : 1'b0),
            .dqs_in_x4_b_3                    (DQS_BUS_MODE_ENUM == "DQS_BUS_MODE_X4" ? b2t_dqsb[(tile_i * LANES_PER_TILE) + 3] : 1'b0),
            .dqs_out_x4_a_lane0               (t2l_dqsbus_x4[0][0]),
            .dqs_out_x4_b_lane0               (t2l_dqsbus_x4[0][1]),
            .dqs_out_x4_a_lane1               (t2l_dqsbus_x4[1][0]),
            .dqs_out_x4_b_lane1               (t2l_dqsbus_x4[1][1]),
            .dqs_out_x4_a_lane2               (t2l_dqsbus_x4[2][0]),
            .dqs_out_x4_b_lane2               (t2l_dqsbus_x4[2][1]),
            .dqs_out_x4_a_lane3               (t2l_dqsbus_x4[3][0]),
            .dqs_out_x4_b_lane3               (t2l_dqsbus_x4[3][1]),
            
            // There are 4 x8/x9 DQS buses per tile, with one pair of input DQS per lane.
            .dqs_in_x8_0                      (DQS_BUS_MODE_ENUM == "DQS_BUS_MODE_X8_X9" ? {b2t_dqsb[(tile_i * LANES_PER_TILE) + 0], b2t_dqs[(tile_i * LANES_PER_TILE) + 0]} : 2'b0),
            .dqs_in_x8_1                      (DQS_BUS_MODE_ENUM == "DQS_BUS_MODE_X8_X9" ? {b2t_dqsb[(tile_i * LANES_PER_TILE) + 1], b2t_dqs[(tile_i * LANES_PER_TILE) + 1]} : 2'b0),
            .dqs_in_x8_2                      (DQS_BUS_MODE_ENUM == "DQS_BUS_MODE_X8_X9" ? {b2t_dqsb[(tile_i * LANES_PER_TILE) + 2], b2t_dqs[(tile_i * LANES_PER_TILE) + 2]} : 2'b0),
            .dqs_in_x8_3                      (DQS_BUS_MODE_ENUM == "DQS_BUS_MODE_X8_X9" ? {b2t_dqsb[(tile_i * LANES_PER_TILE) + 3], b2t_dqs[(tile_i * LANES_PER_TILE) + 3]} : 2'b0),
            .dqs_out_x8_lane0                 (t2l_dqsbus_x8[0]),
            .dqs_out_x8_lane1                 (t2l_dqsbus_x8[1]),
            .dqs_out_x8_lane2                 (t2l_dqsbus_x8[2]),
            .dqs_out_x8_lane3                 (t2l_dqsbus_x8[3]),

            // There are 2 x16/x18 DQS buses per tile, and the input DQS must originate from lane 1 and 3
            .dqs_in_x18_0                     (DQS_BUS_MODE_ENUM == "DQS_BUS_MODE_X16_X18" ? {b2t_dqsb[(tile_i * LANES_PER_TILE) + 1], b2t_dqs[(tile_i * LANES_PER_TILE) + 1]} : 2'b0),
            .dqs_in_x18_1                     (DQS_BUS_MODE_ENUM == "DQS_BUS_MODE_X16_X18" ? {b2t_dqsb[(tile_i * LANES_PER_TILE) + 3], b2t_dqs[(tile_i * LANES_PER_TILE) + 3]} : 2'b0),
            .dqs_out_x18_lane0                (t2l_dqsbus_x18[0]),
            .dqs_out_x18_lane1                (t2l_dqsbus_x18[1]),
            .dqs_out_x18_lane2                (t2l_dqsbus_x18[2]),
            .dqs_out_x18_lane3                (t2l_dqsbus_x18[3]),

            // There is 1 x32/x36 DQS bus per tile, and the input DQS must originate from lane 1
            .dqs_in_x36                       (DQS_BUS_MODE_ENUM == "DQS_BUS_MODE_X32_X36" ? {b2t_dqsb[(tile_i * LANES_PER_TILE) + 1], b2t_dqs[(tile_i * LANES_PER_TILE) + 1]} : 2'b0),
            .dqs_out_x36_lane0                (t2l_dqsbus_x36[0]),
            .dqs_out_x36_lane1                (t2l_dqsbus_x36[1]),
            .dqs_out_x36_lane2                (t2l_dqsbus_x36[2]),
            .dqs_out_x36_lane3                (t2l_dqsbus_x36[3]),
            
            // Data buffer control signals
            .ctl2dbc0                         (all_tiles_ctl2dbc0_dn_chain[tile_i]),
            .ctl2dbc1                         (all_tiles_ctl2dbc1_up_chain[tile_i + 1]),
            .ctl2dbc_in_up                    (all_tiles_ctl2dbc0_dn_chain[tile_i + 1]),
            .ctl2dbc_in_down                  (all_tiles_ctl2dbc1_up_chain[tile_i]),
            .dbc2ctl0                         (l2t_dbc2ctl[0]),
            .dbc2ctl1                         (l2t_dbc2ctl[1]),
            .dbc2ctl2                         (l2t_dbc2ctl[2]),
            .dbc2ctl3                         (l2t_dbc2ctl[3]),
            .cfg_dbc0                         (t2l_cfg_dbc[0]),
            .cfg_dbc1                         (t2l_cfg_dbc[1]),
            .cfg_dbc2                         (t2l_cfg_dbc[2]),
            .cfg_dbc3                         (t2l_cfg_dbc[3]),
            .dbc2core_wr_data_rdy0            (l2core_wr_data_rdy_ast_nonabphy[tile_i][0]),
            .dbc2core_wr_data_rdy1            (l2core_wr_data_rdy_ast_nonabphy[tile_i][1]),
            .dbc2core_wr_data_rdy2            (l2core_wr_data_rdy_ast_nonabphy[tile_i][2]),
            .dbc2core_wr_data_rdy3            (l2core_wr_data_rdy_ast_nonabphy[tile_i][3]),
                        
            // Ping-Pong PHY related signals
            .ping_pong_in                     (all_tiles_ping_pong_up_chain[tile_i]),
            .ping_pong_out                    (all_tiles_ping_pong_up_chain[tile_i + 1]),

            // MMR-related signals
            .mmr_in                           (`_sel_hmc_def(tile_i, core2ctl_mmr_0, core2ctl_mmr_1, 51'b0)),
            .mmr_out                          (all_tiles_ctl2core_mmr[tile_i]),

            // Miscellaneous signals
            .afi_core2ctl                     (all_tiles_c2t_afi[tile_i]),
            .afi_ctl2core                     (all_tiles_t2c_afi[tile_i]),
            .seq2core_reset_n                 (t2c_seq2core_reset_n[tile_i]),
            .ctl_mem_clk_disable              (),
            .afi_lane0_to_ctl                 (16'b0),  
            .afi_lane1_to_ctl                 (16'b0),  
            .afi_lane2_to_ctl                 (16'b0),  
            .afi_lane3_to_ctl                 (16'b0),  
            .rdata_en_full_core               (4'b0),   
            .mrnk_read_core                   (16'b0),  
            .test_dbg_out                     ()        
         );

         for (lane_i = 0; lane_i < LANES_PER_TILE; ++lane_i)
         begin: lane_gen

            (* altera_attribute = "-name MAX_WIRES_FOR_CORE_PERIPHERY_TRANSFER  2; -name MAX_WIRES_FOR_PERIPHERY_CORE_TRANSFER  1" *)
            twentynm_io_12_lane #(
               .silicon_rev                              (SILICON_REV),
               .fast_interpolator_sim                    (USE_FAST_INTERPOLATOR_SIM),
               .hps_ctrl_en                              (IS_HPS ? "true" : "false"),
               .phy_clk_phs_freq                         (PLL_VCO_FREQ_MHZ_INT_CAPPED),
               .mode_rate_in                             (`_get_lane_mode_rate_in),
               .mode_rate_out                            (`_get_lane_mode_rate_out),
               .pipe_latency                             (8'b00000000),               // Don't-care - always set by calibration software
               .dqs_enable_delay                         (6'b000000),                 // Don't-care - always set by calibration software
               .rd_valid_delay                           (7'b0000000),                // Don't-care - always set by calibration software
               .phy_clk_sel                              (0),                         // Always use phy_clk[0] 
               .pin_0_initial_out                        ("initial_out_z"),
               .pin_0_output_phase                       (13'b0000000000000),
               .pin_0_mode_ddr                           (`_get_pin_ddr_str         (tile_i, lane_i, 0)),
               .pin_0_oct_mode                           (`_get_pin_oct_mode_str    (tile_i, lane_i, 0)),
               .pin_0_data_in_mode                       (`_get_pin_data_in_mode_str(tile_i, lane_i, 0)),
               .pin_0_dqs_x4_mode                        (`_get_pin_dqs_x4_mode_0),
               .pin_0_gpio_or_ddr                        (`_get_pin_gpio_or_ddr     (tile_i, lane_i, 0)),
               .pin_1_initial_out                        ("initial_out_z"),
               .pin_1_output_phase                       (13'b0000000000000),
               .pin_1_mode_ddr                           (`_get_pin_ddr_str         (tile_i, lane_i, 1)),
               .pin_1_oct_mode                           (`_get_pin_oct_mode_str    (tile_i, lane_i, 1)),
               .pin_1_data_in_mode                       (`_get_pin_data_in_mode_str(tile_i, lane_i, 1)),
               .pin_1_dqs_x4_mode                        (`_get_pin_dqs_x4_mode_1),
               .pin_1_gpio_or_ddr                        (`_get_pin_gpio_or_ddr     (tile_i, lane_i, 1)),
               .pin_2_initial_out                        ("initial_out_z"),
               .pin_2_output_phase                       (13'b0000000000000),
               .pin_2_mode_ddr                           (`_get_pin_ddr_str         (tile_i, lane_i, 2)),
               .pin_2_oct_mode                           (`_get_pin_oct_mode_str    (tile_i, lane_i, 2)),
               .pin_2_data_in_mode                       (`_get_pin_data_in_mode_str(tile_i, lane_i, 2)),
               .pin_2_dqs_x4_mode                        (`_get_pin_dqs_x4_mode_2),
               .pin_2_gpio_or_ddr                        (`_get_pin_gpio_or_ddr     (tile_i, lane_i, 2)),
               .pin_3_initial_out                        ("initial_out_z"),
               .pin_3_output_phase                       (13'b0000000000000),
               .pin_3_mode_ddr                           (`_get_pin_ddr_str         (tile_i, lane_i, 3)),
               .pin_3_oct_mode                           (`_get_pin_oct_mode_str    (tile_i, lane_i, 3)),
               .pin_3_data_in_mode                       (`_get_pin_data_in_mode_str(tile_i, lane_i, 3)),
               .pin_3_dqs_x4_mode                        (`_get_pin_dqs_x4_mode_3),
               .pin_3_gpio_or_ddr                        (`_get_pin_gpio_or_ddr     (tile_i, lane_i, 3)),
               .pin_4_initial_out                        ("initial_out_z"),
               .pin_4_output_phase                       (13'b0000000000000),
               .pin_4_mode_ddr                           (`_get_pin_ddr_str         (tile_i, lane_i, 4)),
               .pin_4_oct_mode                           (`_get_pin_oct_mode_str    (tile_i, lane_i, 4)),
               .pin_4_data_in_mode                       (`_get_pin_data_in_mode_str(tile_i, lane_i, 4)),
               .pin_4_dqs_x4_mode                        (`_get_pin_dqs_x4_mode_4),
               .pin_4_gpio_or_ddr                        (`_get_pin_gpio_or_ddr     (tile_i, lane_i, 4)),
               .pin_5_initial_out                        ("initial_out_z"),
               .pin_5_output_phase                       (13'b0000000000000),
               .pin_5_mode_ddr                           (`_get_pin_ddr_str         (tile_i, lane_i, 5)),
               .pin_5_oct_mode                           (`_get_pin_oct_mode_str    (tile_i, lane_i, 5)),
               .pin_5_data_in_mode                       (`_get_pin_data_in_mode_str(tile_i, lane_i, 5)),
               .pin_5_dqs_x4_mode                        (`_get_pin_dqs_x4_mode_5),
               .pin_5_gpio_or_ddr                        (`_get_pin_gpio_or_ddr     (tile_i, lane_i, 5)),
               .pin_6_initial_out                        ("initial_out_z"),
               .pin_6_output_phase                       (13'b0000000000000),
               .pin_6_mode_ddr                           (`_get_pin_ddr_str         (tile_i, lane_i, 6)),
               .pin_6_oct_mode                           (`_get_pin_oct_mode_str    (tile_i, lane_i, 6)),
               .pin_6_data_in_mode                       (`_get_pin_data_in_mode_str(tile_i, lane_i, 6)),
               .pin_6_dqs_x4_mode                        (`_get_pin_dqs_x4_mode_6),
               .pin_6_gpio_or_ddr                        (`_get_pin_gpio_or_ddr     (tile_i, lane_i, 6)),
               .pin_7_initial_out                        ("initial_out_z"),
               .pin_7_output_phase                       (13'b0000000000000),
               .pin_7_mode_ddr                           (`_get_pin_ddr_str         (tile_i, lane_i, 7)),
               .pin_7_oct_mode                           (`_get_pin_oct_mode_str    (tile_i, lane_i, 7)),
               .pin_7_data_in_mode                       (`_get_pin_data_in_mode_str(tile_i, lane_i, 7)),
               .pin_7_dqs_x4_mode                        (`_get_pin_dqs_x4_mode_7),
               .pin_7_gpio_or_ddr                        (`_get_pin_gpio_or_ddr     (tile_i, lane_i, 7)),
               .pin_8_initial_out                        ("initial_out_z"),
               .pin_8_output_phase                       (13'b0000000000000),
               .pin_8_mode_ddr                           (`_get_pin_ddr_str         (tile_i, lane_i, 8)),
               .pin_8_oct_mode                           (`_get_pin_oct_mode_str    (tile_i, lane_i, 8)),
               .pin_8_data_in_mode                       (`_get_pin_data_in_mode_str(tile_i, lane_i, 8)),
               .pin_8_dqs_x4_mode                        (`_get_pin_dqs_x4_mode_8),
               .pin_8_gpio_or_ddr                        (`_get_pin_gpio_or_ddr     (tile_i, lane_i, 8)),
               .pin_9_initial_out                        ("initial_out_z"),
               .pin_9_output_phase                       (13'b0000000000000),
               .pin_9_mode_ddr                           (`_get_pin_ddr_str         (tile_i, lane_i, 9)),
               .pin_9_oct_mode                           (`_get_pin_oct_mode_str    (tile_i, lane_i, 9)),
               .pin_9_data_in_mode                       (`_get_pin_data_in_mode_str(tile_i, lane_i, 9)),
               .pin_9_dqs_x4_mode                        (`_get_pin_dqs_x4_mode_9),
               .pin_9_gpio_or_ddr                        (`_get_pin_gpio_or_ddr     (tile_i, lane_i, 9)),
               .pin_10_initial_out                       ("initial_out_z"),
               .pin_10_output_phase                      (13'b0000000000000),
               .pin_10_mode_ddr                          (`_get_pin_ddr_str         (tile_i, lane_i, 10)),
               .pin_10_oct_mode                          (`_get_pin_oct_mode_str    (tile_i, lane_i, 10)),
               .pin_10_data_in_mode                      (`_get_pin_data_in_mode_str(tile_i, lane_i, 10)),
               .pin_10_dqs_x4_mode                       (`_get_pin_dqs_x4_mode_10),
               .pin_10_gpio_or_ddr                       (`_get_pin_gpio_or_ddr     (tile_i, lane_i, 10)),
               .pin_11_initial_out                       ("initial_out_z"),
               .pin_11_output_phase                      (13'b0000000000000),
               .pin_11_mode_ddr                          (`_get_pin_ddr_str         (tile_i, lane_i, 11)),
               .pin_11_oct_mode                          (`_get_pin_oct_mode_str    (tile_i, lane_i, 11)),
               .pin_11_data_in_mode                      (`_get_pin_data_in_mode_str(tile_i, lane_i, 11)),
               .pin_11_dqs_x4_mode                       (`_get_pin_dqs_x4_mode_11),
               .pin_11_gpio_or_ddr                       (`_get_pin_gpio_or_ddr     (tile_i, lane_i, 11)),
               .avl_base_addr                            (`_get_lane_tid(tile_i, lane_i)),
               .avl_ena                                  ("true"),
               .db_hmc_or_core                           (`_get_hmc_or_core),
               .db_dbi_sel                               (11),   
               .db_dbi_wr_en                             (`_get_dbi_wr_en(tile_i, lane_i)),
               .db_dbi_rd_en                             (`_get_dbi_rd_en(tile_i, lane_i)),
               .db_crc_dq0                               (`_get_crc_pin_pos_0),   
               .db_crc_dq1                               (`_get_crc_pin_pos_1),   
               .db_crc_dq2                               (`_get_crc_pin_pos_2),   
               .db_crc_dq3                               (`_get_crc_pin_pos_3),   
               .db_crc_dq4                               (`_get_crc_pin_pos_4),   
               .db_crc_dq5                               (`_get_crc_pin_pos_5),   
               .db_crc_dq6                               (`_get_crc_pin_pos_6),   
               .db_crc_dq7                               (`_get_crc_pin_pos_7),   
               .db_crc_dq8                               (`_get_crc_pin_pos_8),   
               .db_crc_x4_or_x8_or_x9                    (`_get_crc_x4_or_x8_or_x9),
               .db_crc_en                                (`_get_crc_en(tile_i, lane_i)),
               .db_rwlat_mode                            ("avl_vlu"),                                         // wlat/rlat set dynamically via Avalon by Nios (instead of through CSR)
               .db_afi_wlat_vlu                          (6'b000000),                                         // Unused - wlat set dynamically via Avalon by Nios
               .db_afi_rlat_vlu                          (6'b000000),                                         // Unused - rlat set dynamically via Avalon by Nios
               .db_ptr_pipeline_depth                    (`_get_db_ptr_pipe_depth(tile_i, lane_i)),           // Additional latency to compensate for distance from HMC
               .db_seq_rd_en_full_pipeline               (`_get_db_seq_rd_en_full_pipeline(tile_i, lane_i)),  // Additional latency to compensate for distance from sequencer
               .db_preamble_mode                         (PREAMBLE_MODE),
               .db_reset_auto_release                    ("avl_release"),                         // Reset sequencer controlled via Avalon by Nios
               .db_data_alignment_mode                   (`_get_db_data_alignment_mode),          // Data alignment mode (enabled IFF HMC)
               .db_db2core_registered                    ("true"),
               .db_core_or_hmc2db_registered             ("false"),
               .dbc_core_clk_sel                         (USE_HMC_RC_OR_DP ? 1 : 0),              // Use phy_clk1 if HMC dual-port or rate-converter is used, use phy_clk0 otherwise
               .dbc_wb_reserved_entry                    (DBC_WB_RESERVED_ENTRY),
               .db_pin_0_ac_hmc_data_override_ena        (`_get_pin_ac_hmc_data_override_ena (tile_i, lane_i, 0)),
               .db_pin_0_mode                            (`_get_pin_wdb_str                  (tile_i, lane_i, 0)),
               .db_pin_0_in_bypass                       (`_get_pin_db_in_bypass             (tile_i, lane_i, 0)),
               .db_pin_0_out_bypass                      (`_get_pin_db_out_bypass            (tile_i, lane_i, 0)),
               .db_pin_0_oe_bypass                       (`_get_pin_db_oe_bypass             (tile_i, lane_i, 0)),
               .db_pin_0_oe_invert                       (`_get_pin_invert_oe                (tile_i, lane_i, 0)),
               .db_pin_0_wr_invert                       (`_get_pin_invert_wr                (tile_i, lane_i, 0)),
               .db_pin_1_ac_hmc_data_override_ena        (`_get_pin_ac_hmc_data_override_ena (tile_i, lane_i, 1)),
               .db_pin_1_mode                            (`_get_pin_wdb_str                  (tile_i, lane_i, 1)),
               .db_pin_1_in_bypass                       (`_get_pin_db_in_bypass             (tile_i, lane_i, 1)),
               .db_pin_1_out_bypass                      (`_get_pin_db_out_bypass            (tile_i, lane_i, 1)),
               .db_pin_1_oe_bypass                       (`_get_pin_db_oe_bypass             (tile_i, lane_i, 1)),
               .db_pin_1_oe_invert                       (`_get_pin_invert_oe                (tile_i, lane_i, 1)),
               .db_pin_1_wr_invert                       (`_get_pin_invert_wr                (tile_i, lane_i, 1)),
               .db_pin_2_ac_hmc_data_override_ena        (`_get_pin_ac_hmc_data_override_ena (tile_i, lane_i, 2)),
               .db_pin_2_mode                            (`_get_pin_wdb_str                  (tile_i, lane_i, 2)),
               .db_pin_2_in_bypass                       (`_get_pin_db_in_bypass             (tile_i, lane_i, 2)),
               .db_pin_2_out_bypass                      (`_get_pin_db_out_bypass            (tile_i, lane_i, 2)),
               .db_pin_2_oe_bypass                       (`_get_pin_db_oe_bypass             (tile_i, lane_i, 2)),
               .db_pin_2_oe_invert                       (`_get_pin_invert_oe                (tile_i, lane_i, 2)),
               .db_pin_2_wr_invert                       (`_get_pin_invert_wr                (tile_i, lane_i, 2)),
               .db_pin_3_ac_hmc_data_override_ena        (`_get_pin_ac_hmc_data_override_ena (tile_i, lane_i, 3)),
               .db_pin_3_mode                            (`_get_pin_wdb_str                  (tile_i, lane_i, 3)),
               .db_pin_3_in_bypass                       (`_get_pin_db_in_bypass             (tile_i, lane_i, 3)),
               .db_pin_3_out_bypass                      (`_get_pin_db_out_bypass            (tile_i, lane_i, 3)),
               .db_pin_3_oe_bypass                       (`_get_pin_db_oe_bypass             (tile_i, lane_i, 3)),
               .db_pin_3_oe_invert                       (`_get_pin_invert_oe                (tile_i, lane_i, 3)),
               .db_pin_3_wr_invert                       (`_get_pin_invert_wr                (tile_i, lane_i, 3)),
               .db_pin_4_ac_hmc_data_override_ena        (`_get_pin_ac_hmc_data_override_ena (tile_i, lane_i, 4)),
               .db_pin_4_mode                            (`_get_pin_wdb_str                  (tile_i, lane_i, 4)),
               .db_pin_4_in_bypass                       (`_get_pin_db_in_bypass             (tile_i, lane_i, 4)),
               .db_pin_4_out_bypass                      (`_get_pin_db_out_bypass            (tile_i, lane_i, 4)),
               .db_pin_4_oe_bypass                       (`_get_pin_db_oe_bypass             (tile_i, lane_i, 4)),
               .db_pin_4_oe_invert                       (`_get_pin_invert_oe                (tile_i, lane_i, 4)),
               .db_pin_4_wr_invert                       (`_get_pin_invert_wr                (tile_i, lane_i, 4)),
               .db_pin_5_ac_hmc_data_override_ena        (`_get_pin_ac_hmc_data_override_ena (tile_i, lane_i, 5)),
               .db_pin_5_mode                            (`_get_pin_wdb_str                  (tile_i, lane_i, 5)),
               .db_pin_5_in_bypass                       (`_get_pin_db_in_bypass             (tile_i, lane_i, 5)),
               .db_pin_5_out_bypass                      (`_get_pin_db_out_bypass            (tile_i, lane_i, 5)),
               .db_pin_5_oe_bypass                       (`_get_pin_db_oe_bypass             (tile_i, lane_i, 5)),
               .db_pin_5_oe_invert                       (`_get_pin_invert_oe                (tile_i, lane_i, 5)),
               .db_pin_5_wr_invert                       (`_get_pin_invert_wr                (tile_i, lane_i, 5)),
               .db_pin_6_ac_hmc_data_override_ena        (`_get_pin_ac_hmc_data_override_ena (tile_i, lane_i, 6)),
               .db_pin_6_mode                            (`_get_pin_wdb_str                  (tile_i, lane_i, 6)),
               .db_pin_6_in_bypass                       (`_get_pin_db_in_bypass             (tile_i, lane_i, 6)),
               .db_pin_6_out_bypass                      (`_get_pin_db_out_bypass            (tile_i, lane_i, 6)),
               .db_pin_6_oe_bypass                       (`_get_pin_db_oe_bypass             (tile_i, lane_i, 6)),
               .db_pin_6_oe_invert                       (`_get_pin_invert_oe                (tile_i, lane_i, 6)),
               .db_pin_6_wr_invert                       (`_get_pin_invert_wr                (tile_i, lane_i, 6)),
               .db_pin_7_ac_hmc_data_override_ena        (`_get_pin_ac_hmc_data_override_ena (tile_i, lane_i, 7)),
               .db_pin_7_mode                            (`_get_pin_wdb_str                  (tile_i, lane_i, 7)),
               .db_pin_7_in_bypass                       (`_get_pin_db_in_bypass             (tile_i, lane_i, 7)),
               .db_pin_7_out_bypass                      (`_get_pin_db_out_bypass            (tile_i, lane_i, 7)),
               .db_pin_7_oe_bypass                       (`_get_pin_db_oe_bypass             (tile_i, lane_i, 7)),
               .db_pin_7_oe_invert                       (`_get_pin_invert_oe                (tile_i, lane_i, 7)),
               .db_pin_7_wr_invert                       (`_get_pin_invert_wr                (tile_i, lane_i, 7)),
               .db_pin_8_ac_hmc_data_override_ena        (`_get_pin_ac_hmc_data_override_ena (tile_i, lane_i, 8)),
               .db_pin_8_mode                            (`_get_pin_wdb_str                  (tile_i, lane_i, 8)),
               .db_pin_8_in_bypass                       (`_get_pin_db_in_bypass             (tile_i, lane_i, 8)),
               .db_pin_8_out_bypass                      (`_get_pin_db_out_bypass            (tile_i, lane_i, 8)),
               .db_pin_8_oe_bypass                       (`_get_pin_db_oe_bypass             (tile_i, lane_i, 8)),
               .db_pin_8_oe_invert                       (`_get_pin_invert_oe                (tile_i, lane_i, 8)),
               .db_pin_8_wr_invert                       (`_get_pin_invert_wr                (tile_i, lane_i, 8)),
               .db_pin_9_ac_hmc_data_override_ena        (`_get_pin_ac_hmc_data_override_ena (tile_i, lane_i, 9)),
               .db_pin_9_mode                            (`_get_pin_wdb_str                  (tile_i, lane_i, 9)),
               .db_pin_9_in_bypass                       (`_get_pin_db_in_bypass             (tile_i, lane_i, 9)),
               .db_pin_9_out_bypass                      (`_get_pin_db_out_bypass            (tile_i, lane_i, 9)),
               .db_pin_9_oe_bypass                       (`_get_pin_db_oe_bypass             (tile_i, lane_i, 9)),
               .db_pin_9_oe_invert                       (`_get_pin_invert_oe                (tile_i, lane_i, 9)),
               .db_pin_9_wr_invert                       (`_get_pin_invert_wr                (tile_i, lane_i, 9)),
               .db_pin_10_ac_hmc_data_override_ena       (`_get_pin_ac_hmc_data_override_ena (tile_i, lane_i, 10)),
               .db_pin_10_mode                           (`_get_pin_wdb_str                  (tile_i, lane_i, 10)),
               .db_pin_10_in_bypass                      (`_get_pin_db_in_bypass             (tile_i, lane_i, 10)),
               .db_pin_10_out_bypass                     (`_get_pin_db_out_bypass            (tile_i, lane_i, 10)),
               .db_pin_10_oe_bypass                      (`_get_pin_db_oe_bypass             (tile_i, lane_i, 10)),
               .db_pin_10_oe_invert                      (`_get_pin_invert_oe                (tile_i, lane_i, 10)),
               .db_pin_10_wr_invert                      (`_get_pin_invert_wr                (tile_i, lane_i, 10)),
               .db_pin_11_ac_hmc_data_override_ena       (`_get_pin_ac_hmc_data_override_ena (tile_i, lane_i, 11)),
               .db_pin_11_mode                           (`_get_pin_wdb_str                  (tile_i, lane_i, 11)),
               .db_pin_11_in_bypass                      (`_get_pin_db_in_bypass             (tile_i, lane_i, 11)),
               .db_pin_11_out_bypass                     (`_get_pin_db_out_bypass            (tile_i, lane_i, 11)),
               .db_pin_11_oe_bypass                      (`_get_pin_db_oe_bypass             (tile_i, lane_i, 11)),
               .db_pin_11_oe_invert                      (`_get_pin_invert_oe                (tile_i, lane_i, 11)),
               .db_pin_11_wr_invert                      (`_get_pin_invert_wr                (tile_i, lane_i, 11)),
               .dll_rst_en                               (IS_HPS ? "dll_rst_dis" : "dll_rst_en"),
               .dll_en                                   ("dll_en"),
               .dll_core_updnen                          ("core_updn_dis"),
               .dll_ctlsel                               (`_get_dll_ctlsel),
               .dll_ctl_static                           (`_get_dll_ctl_static),
               .dqs_lgc_dqs_b_en                         (`_get_dqs_b_en),                       // Must be enabled for complimentary (non differential) read clock
               .dqs_lgc_dqs_a_interp_en                  ("false"),                              // This enables read capture using an internal clock - never used by EMIF
               .dqs_lgc_dqs_b_interp_en                  ("false"),                              // This enables read capture using an internal clock - never used by EMIF
               .dqs_lgc_swap_dqs_a_b                     (SWAP_DQS_A_B),                         // This is used by QDR2 which may have fractional (1.5 or 2.5 cyc) read latencies
               .dqs_lgc_pvt_input_delay_a                (10'b00000_00000),                      // Phase shift to center read clock/strobe signal in read window (DQS-A bus). Overriden by Nios during calibration.
               .dqs_lgc_pvt_input_delay_b                (10'b00000_00000),                      // Phase shift to center read clock/strobe signal in read window (DQS-B bus). Overriden by Nios during calibration.
               .dqs_lgc_enable_toggler                   (`_get_preamble_track_dqs_enable_mode), // Tracking Mode
               .dqs_lgc_phase_shift_b                    (13'b00000_0000_0000),                  // Delay to read clock/strobe gating signal. Overriden by Nios during calibration.
               .dqs_lgc_phase_shift_a                    (13'b00000_0000_0000),                  // Delay to read clock/strobe gating signal. Overriden by Nios during calibration.
               .dqs_lgc_pack_mode                        (DQS_PACK_MODE),
               .oct_size                                 (OCT_SIZE),
               .dqs_lgc_pst_preamble_mode                (`_get_pst_preamble_mode),
               .dqs_lgc_pst_en_shrink                    (`_get_pst_en_shrink),
               .dqs_lgc_broadcast_enable                 ("disable_broadcast"),
               .dqs_lgc_burst_length                     (`_get_dqs_lgc_burst_length),
               .dqs_lgc_ddr4_search                      (`_get_ddr4_search),
               .dqs_lgc_count_threshold                  (7'b0011000),
               .pingpong_primary                         (`_sel_hmc_lane(tile_i, lane_i, "true", "false")),
               .pingpong_secondary                       (`_sel_hmc_lane(tile_i, lane_i, "false", "true"))
               
            ) lane_inst (
            
               // PLL/DLL/PVT interface
               .pll_locked                               (pll_locked),
               .dll_ref_clk                              (all_tiles_dll_clk_out[tile_i][lane_i]),
               .core_dll                                 (core2dll),    
               .dll_core                                 (),    
               .ioereg_locked                            (),

               // Clocks
               .phy_clk                                  (all_tiles_t2l_phy_clk[tile_i][lane_i]),                  
               .phy_clk_phs                              (all_tiles_t2l_phy_clk_phs[tile_i][lane_i]),
               
               // Clock Phase Alignment
               .sync_data_bot_in                         (pa_sync_data_up_chain[`_get_chain_index_for_lane(tile_i, lane_i)]),
               .sync_data_top_out                        (pa_sync_data_up_chain[`_get_chain_index_for_lane(tile_i, lane_i) + 1]),
               .sync_data_top_in                         (pa_sync_data_dn_chain[`_get_chain_index_for_lane(tile_i, lane_i) + 1]),
               .sync_data_bot_out                        (pa_sync_data_dn_chain[`_get_chain_index_for_lane(tile_i, lane_i)]),
               .sync_clk_bot_in                          (pa_sync_clk_up_chain [`_get_chain_index_for_lane(tile_i, lane_i)]),
               .sync_clk_top_out                         (pa_sync_clk_up_chain [`_get_chain_index_for_lane(tile_i, lane_i) + 1]),
               .sync_clk_top_in                          (pa_sync_clk_dn_chain [`_get_chain_index_for_lane(tile_i, lane_i) + 1]),
               .sync_clk_bot_out                         (pa_sync_clk_dn_chain [`_get_chain_index_for_lane(tile_i, lane_i)]),

               // DQS bus from tile. Connections are only made for the data lanes (as captured by the macro)
               .dqs_in                                   (`_get_dqsin(tile_i, lane_i)),

               // Interface to I/O buffers
               .oct_enable                               (l2b_dtc_nonabphy [tile_i * PINS_PER_LANE * LANES_PER_TILE + lane_i * PINS_PER_LANE +: PINS_PER_LANE]),
               .data_oe                                  (l2b_oe_nonabphy  [tile_i * PINS_PER_LANE * LANES_PER_TILE + lane_i * PINS_PER_LANE +: PINS_PER_LANE]),
               .data_out                                 (l2b_data_nonabphy[tile_i * PINS_PER_LANE * LANES_PER_TILE + lane_i * PINS_PER_LANE +: PINS_PER_LANE]),
               .data_in                                  (b2l_data[tile_i * PINS_PER_LANE * LANES_PER_TILE + lane_i * PINS_PER_LANE +: PINS_PER_LANE]),

               // Interface to core
               .data_from_core                           (core2l_data[tile_i][lane_i]),
               .data_to_core                             (l2core_data_nonabphy[tile_i][lane_i]),
               // core2l_oe is inverted before feeding into the lane because
               // oe_invert is always set to true as required by HMC and sequencer
               .oe_from_core                             (~core2l_oe[tile_i][lane_i]),  
               .rdata_en_full_core                       ((`_get_lane_usage(tile_i, lane_i) == LANE_USAGE_RDATA || `_get_lane_usage(tile_i, lane_i) == LANE_USAGE_WRDATA) ? core2l_rdata_en_full[tile_i][lane_i] : 4'b0),
               .mrnk_read_core                           ((`_get_lane_usage(tile_i, lane_i) == LANE_USAGE_RDATA || `_get_lane_usage(tile_i, lane_i) == LANE_USAGE_WRDATA) ? core2l_mrnk_read[tile_i][lane_i]     : 16'b0),
               .mrnk_write_core                          ((`_get_lane_usage(tile_i, lane_i) == LANE_USAGE_WDATA || `_get_lane_usage(tile_i, lane_i) == LANE_USAGE_WRDATA) ? core2l_mrnk_write[tile_i][lane_i]    : 16'b0),
               .rdata_valid_core                         (l2core_rdata_valid_nonabphy[tile_i][lane_i]),
               .afi_wlat_core                            (l2core_afi_wlat_nonabphy[tile_i][lane_i]),
               .afi_rlat_core                            (l2core_afi_rlat_nonabphy[tile_i][lane_i]),

               // Data Buffer Interface to Core
               .dbc2core_rd_data_vld0                    (l2core_rd_data_vld_avl0_nonabphy[tile_i][lane_i]),
               .dbc2core_rd_data_vld1                    (),
               .core2dbc_wr_data_vld0                    (`_get_core2dbc_wr_data_vld(tile_i, lane_i)),
               .core2dbc_wr_data_vld1                    (1'b0),
               .dbc2core_wr_data_rdy                     (l2core_wr_data_rdy_ast_nonabphy [tile_i][lane_i]),
               .core2dbc_rd_data_rdy                     (`_get_core2dbc_rd_data_rdy(tile_i, lane_i)),
               .dbc2core_wb_pointer                      (l2core_wb_pointer_for_ecc_nonabphy[tile_i][lane_i]),
               .core2dbc_wr_ecc_info                     (`_get_core2dbc_wr_ecc_info(tile_i, lane_i)),
               .dbc2core_rd_type                         (),
               
               // Calibration bus between Nios and sequencer (a.k.a slow Avalon-MM bus)
               .reset_n                                  (global_reset_n_int),
               .cal_avl_in                               (cal_bus_avl_up_chain          [`_get_chain_index_for_lane(tile_i, lane_i)]),
               .cal_avl_out                              (cal_bus_avl_up_chain          [`_get_chain_index_for_lane(tile_i, lane_i) + 1]),
               .cal_avl_readdata_in                      (cal_bus_avl_read_data_dn_chain[`_get_chain_index_for_lane(tile_i, lane_i) + 1]),
               .cal_avl_readdata_out                     (cal_bus_avl_read_data_dn_chain[`_get_chain_index_for_lane(tile_i, lane_i)]),
               
               // HMC interface
               .ac_hmc                                   (`_get_ac_hmc(tile_i, lane_i)),
               .ctl2dbc0                                 (all_tiles_ctl2dbc0_dn_chain[tile_i]),
               .ctl2dbc1                                 (all_tiles_ctl2dbc1_up_chain[tile_i + 1]),
               .dbc2ctl                                  (l2t_dbc2ctl[lane_i]),
               .cfg_dbc                                  (t2l_cfg_dbc[lane_i]),
               
               // Broadcast signals
               .broadcast_in_bot                         (broadcast_up_chain[`_get_broadcast_chain_index(tile_i, lane_i)]),
               .broadcast_out_top                        (broadcast_up_chain[`_get_broadcast_chain_index(tile_i, lane_i) + 1]),              
               .broadcast_in_top                         (broadcast_dn_chain[`_get_broadcast_chain_index(tile_i, lane_i) + 1]),
               .broadcast_out_bot                        (broadcast_dn_chain[`_get_broadcast_chain_index(tile_i, lane_i)]),
               
               // Unused signals
               .dft_phy_clk                              ()
            );
         end
      end
   endgenerate
endmodule

