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





 
`define _get_max(_i, _j)                                 ( (_i) > (_j) ? (_i) : (_j) )
`define _get_min(_i, _j)                                 ( (_i) < (_j) ? (_i) : (_j) )
   
`define _get_chain_index_for_tile(_tile_i)               ( _tile_i * (LANES_PER_TILE + 1) + 2 )

`define _get_chain_index_for_lane(_tile_i, _lane_i)      ( (_lane_i < 2) ? (_tile_i * (LANES_PER_TILE + 1) + _lane_i) : ( \
                                                                           (_tile_i * (LANES_PER_TILE + 1) + _lane_i + 1 )) )

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

`define _get_ac_tile_index(_tile_i, _lane_i)             ( (PHY_PING_PONG_EN && (_tile_i < SEC_AC_TILE_INDEX || (_tile_i == SEC_AC_TILE_INDEX && _lane_i < 2))) ? SEC_AC_TILE_INDEX : PRI_AC_TILE_INDEX )

`define _get_dbc_pipe_lat(_tile_i, _lane_i)              ( (_tile_i > `_get_ac_tile_index(_tile_i, _lane_i)) ? (_tile_i - `_get_ac_tile_index(_tile_i, _lane_i)) : \
                                                                                                               (`_get_ac_tile_index(_tile_i, _lane_i) - _tile_i) )

`define _get_max_distance_from_ac_tile                   ( `_get_max( (NUM_OF_RTL_TILES - PRI_AC_TILE_INDEX - 1), PRI_AC_TILE_INDEX ) )

`define _get_max_distance_from_ac_tile_capped            ( PHY_PING_PONG_EN ? `_get_min(2, `_get_max_distance_from_ac_tile) : `_get_max_distance_from_ac_tile ) 

`define _get_curr_distance_from_ac_tile(_tile_i)         ( (_tile_i > PRI_AC_TILE_INDEX) ? (_tile_i - PRI_AC_TILE_INDEX) : (PRI_AC_TILE_INDEX - _tile_i) )
`define _get_db_ptr_pipe_depth_pri(_tile_i)              ( `_get_max_distance_from_ac_tile_capped - `_get_curr_distance_from_ac_tile(_tile_i) )
`define _get_db_ptr_pipe_depth_sec(_tile_i)              ( `_get_db_ptr_pipe_depth_pri(_tile_i) + 1)
`define _get_db_ptr_pipe_depth(_tile_i, _lane_i)         ( `_get_ac_tile_index(_tile_i, _lane_i) == PRI_AC_TILE_INDEX ? `_get_db_ptr_pipe_depth_pri(_tile_i) : `_get_db_ptr_pipe_depth_sec(_tile_i))

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
                                                                                                                 
`define _get_cpa_0_clk_ratio                             ( NUM_OF_HMC_PORTS > 0 ? USER_CLK_RATIO : (USER_CLK_RATIO * 2) )
`define _get_pa_exponent_0                               ( (`_get_pa_exponent(`_get_cpa_0_clk_ratio)) )

`define _get_cpa_1_clk_ratio                             ( C2P_P2C_CLK_RATIO )
`define _get_pa_exponent_1                               ( (`_get_pa_exponent(`_get_cpa_1_clk_ratio)) )

`define _get_pa_feedback_divider_p0                      ( (`_get_cpa_0_clk_ratio == C2P_P2C_CLK_RATIO * 2) ? "div_by_2_p0" : "div_by_1_p0" )

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

`define _get_ctrl2dbc_switch_0(_tile_i)                  ( PHY_PING_PONG_EN ? `_get_ctrl2dbc_switch_0_pp(_tile_i) : `_get_ctrl2dbc_switch_0_non_pp(_tile_i) )
`define _get_ctrl2dbc_switch_1(_tile_i)                  ( PHY_PING_PONG_EN ? `_get_ctrl2dbc_switch_1_pp(_tile_i) : `_get_ctrl2dbc_switch_1_non_pp(_tile_i) )
`define _get_ctrl2dbc_sel_0(_tile_i)                     ( PHY_PING_PONG_EN ? `_get_ctrl2dbc_sel_0_pp(_tile_i)    : `_get_ctrl2dbc_sel_0_non_pp(_tile_i) )
`define _get_ctrl2dbc_sel_1(_tile_i)                     ( PHY_PING_PONG_EN ? `_get_ctrl2dbc_sel_1_pp(_tile_i)    : `_get_ctrl2dbc_sel_1_non_pp(_tile_i) )
`define _get_ctrl2dbc_sel_2(_tile_i)                     ( PHY_PING_PONG_EN ? `_get_ctrl2dbc_sel_2_pp(_tile_i)    : `_get_ctrl2dbc_sel_2_non_pp(_tile_i) )
`define _get_ctrl2dbc_sel_3(_tile_i)                     ( PHY_PING_PONG_EN ? `_get_ctrl2dbc_sel_3_pp(_tile_i)    : `_get_ctrl2dbc_sel_3_non_pp(_tile_i) )

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

`define _get_ac_hmc(_tile_i, _lane_i)                    ( (`_get_lane_usage(_tile_i, _lane_i) == LANE_USAGE_AC_HMC || \
                                                            `_get_lane_usage(_tile_i, _lane_i) == LANE_USAGE_AC_CORE || \
                                                            (`_get_lane_usage(_tile_i, _lane_i) == LANE_USAGE_UNUSED && IS_HPS && _tile_i == PRI_AC_TILE_INDEX)) ? \
                                                            t2l_ac_hmc[lane_i] : 96'b0 )

`define _get_core2dbc_wr_data_vld_of_hmc(_tile_i, _lane_i)    ( (`_get_ac_tile_index(_tile_i, _lane_i) == PRI_AC_TILE_INDEX ? core2l_wr_data_vld_ast_0 : core2l_wr_data_vld_ast_1 ) )
`define _get_core2dbc_wr_data_vld(_tile_i, _lane_i)           ( ((`_get_lane_usage(_tile_i, _lane_i) == LANE_USAGE_WRDATA) || \
                                                                 (_lane_i == 0 && `_get_lane_usage(_tile_i, 0) == LANE_USAGE_AC_HMC && `_get_hmc_dbc2ctrl_sel(_tile_i) == "dbc0_to_local") || \
                                                                 (_lane_i == 1 && `_get_lane_usage(_tile_i, 1) == LANE_USAGE_AC_HMC && `_get_hmc_dbc2ctrl_sel(_tile_i) == "dbc1_to_local") || \
                                                                 (_lane_i == 2 && `_get_lane_usage(_tile_i, 2) == LANE_USAGE_AC_HMC && `_get_hmc_dbc2ctrl_sel(_tile_i) == "dbc2_to_local") || \
                                                                 (_lane_i == 3 && `_get_lane_usage(_tile_i, 3) == LANE_USAGE_AC_HMC && `_get_hmc_dbc2ctrl_sel(_tile_i) == "dbc3_to_local")) ? \
                                                                  `_get_core2dbc_wr_data_vld_of_hmc(_tile_i, _lane_i) : 1'b0 )

`define _get_core2dbc_rd_data_rdy_of_hmc(_tile_i, _lane_i)    ( (`_get_ac_tile_index(_tile_i, _lane_i) == PRI_AC_TILE_INDEX ? core2l_rd_data_rdy_ast_0 : core2l_rd_data_rdy_ast_1 ) )
`define _get_core2dbc_rd_data_rdy(_tile_i, _lane_i)           ( ((`_get_lane_usage(_tile_i, _lane_i) == LANE_USAGE_WRDATA) || \
                                                                 (_lane_i == 0 && `_get_lane_usage(_tile_i, 0) == LANE_USAGE_AC_HMC && `_get_hmc_dbc2ctrl_sel(_tile_i) == "dbc0_to_local") || \
                                                                 (_lane_i == 1 && `_get_lane_usage(_tile_i, 1) == LANE_USAGE_AC_HMC && `_get_hmc_dbc2ctrl_sel(_tile_i) == "dbc1_to_local") || \
                                                                 (_lane_i == 2 && `_get_lane_usage(_tile_i, 2) == LANE_USAGE_AC_HMC && `_get_hmc_dbc2ctrl_sel(_tile_i) == "dbc2_to_local") || \
                                                                 (_lane_i == 3 && `_get_lane_usage(_tile_i, 3) == LANE_USAGE_AC_HMC && `_get_hmc_dbc2ctrl_sel(_tile_i) == "dbc3_to_local")) ? \
                                                                  `_get_core2dbc_rd_data_rdy_of_hmc(_tile_i, _lane_i) : 1'b1 )

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

`define _get_dqs_b_en                                    ( (PROTOCOL_ENUM == "PROTOCOL_QDR2") || (DQS_BUS_MODE_ENUM == "DQS_BUS_MODE_X4") ? "true" : "false" )

`define _get_pst_en_shrink                               ( PROTOCOL_ENUM == "PROTOCOL_DDR4"   ? ((DQS_BUS_MODE_ENUM == "DQS_BUS_MODE_X4") ? "shrink_1_1" : "shrink_1_0") : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_DDR3"   ? "shrink_1_1" : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_LPDDR3" ? "shrink_1_1" : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_RLD3"   ? "shrink_0_1" : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_QDR2"   ? "shrink_0_1" : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_RLD2"   ? "shrink_0_1" : ( \
                                                           PROTOCOL_ENUM == "PROTOCOL_QDR4"   ? "shrink_0_1" : ( \
                                                                                              "" ))))))))


`define _get_dbi_wr_en(_tile_i, _lane_i)                 ((`_get_lane_usage(_tile_i, _lane_i) == LANE_USAGE_WRDATA) ? DBI_WR_ENABLE : "false")
`define _get_dbi_rd_en(_tile_i, _lane_i)                 ((`_get_lane_usage(_tile_i, _lane_i) == LANE_USAGE_WRDATA) ? DBI_RD_ENABLE : "false")

`define _get_crc_en(_tile_i, _lane_i)                    ((`_get_lane_usage(_tile_i, _lane_i) == LANE_USAGE_WRDATA) ? CRC_EN : "crc_disable")

`define _get_crc_x4_or_x8_or_x9                          ( (PORT_MEM_DQ_WIDTH / PORT_MEM_DQS_WIDTH == 4) ? "x4_mode" : ( \
                                                           (DBI_WR_ENABLE == "true" || MEM_DATA_MASK_EN) ? "x9_mode" : ( \
                                                                                                           "x8_mode"   )) )

`define _get_crc_pin_pos_0				                  1
`define _get_crc_pin_pos_1				                  2
`define _get_crc_pin_pos_2				                  3
`define _get_crc_pin_pos_3				                  6
`define _get_crc_pin_pos_4				                  ((`_get_crc_x4_or_x8_or_x9 != "x4_mode") ? 7 : 4)
`define _get_crc_pin_pos_5				                  ((`_get_crc_x4_or_x8_or_x9 != "x4_mode") ? 8 : 4)
`define _get_crc_pin_pos_6				                  ((`_get_crc_x4_or_x8_or_x9 != "x4_mode") ? 9 : 4)
`define _get_crc_pin_pos_7				                  ((`_get_crc_x4_or_x8_or_x9 != "x4_mode") ? 10 : 4)
`define _get_crc_pin_pos_8				                  ((`_get_crc_x4_or_x8_or_x9 == "x9_mode") ? 11 : 4)

`define _sel_hmc_val(_tile_i, _pri, _sec)             ( PHY_PING_PONG_EN ? (_tile_i <= SEC_AC_TILE_INDEX ? _sec : _pri) : _pri )

`define _sel_hmc_def(_tile_i, _pri, _sec, _def)       ( PHY_PING_PONG_EN ? ((_tile_i == SEC_AC_TILE_INDEX) ? _sec : (_tile_i == PRI_AC_TILE_INDEX) ? _pri : _def) : _pri )

`define _sel_hmc_lane(_tile_i, _lane_i, _pri, _sec)   ( (PHY_PING_PONG_EN && (_tile_i < SEC_AC_TILE_INDEX || (_tile_i == SEC_AC_TILE_INDEX && _lane_i < 2))) ? _sec : _pri )

module altera_emif_arch_nf_io_tiles_abphy #(
   parameter DIAG_SYNTH_FOR_SIM                      = 0,
   parameter DIAG_VERBOSE_IOAUX                      = 0,
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
   parameter OCT_SIZE	                             = "",
   parameter [6:0] DBC_WB_RESERVED_ENTRY             = 4,
   parameter DLL_MODE                                = "",
   parameter DLL_CODEWORD                            = 0,
   parameter PORT_MEM_DQ_WIDTH                       = 1,
   parameter PORT_MEM_DQS_WIDTH                      = 1,
   parameter PORT_DFT_NF_PA_DPRIO_REG_ADDR_WIDTH     = 1,
   parameter PORT_DFT_NF_PA_DPRIO_WRITEDATA_WIDTH    = 1,
   parameter PORT_DFT_NF_PA_DPRIO_READDATA_WIDTH     = 1,
   parameter PORT_MEM_A_PINLOC                       = 0,
   parameter PORT_MEM_BA_PINLOC                      = 0,
   parameter PORT_MEM_BG_PINLOC                      = 0,
   parameter PORT_MEM_CS_N_PINLOC                    = 0,
   parameter PORT_MEM_ACT_N_PINLOC                   = 0,
   parameter PORT_MEM_DQ_PINLOC                      = 0,
   parameter PORT_MEM_DM_PINLOC                      = 0,
   parameter PORT_MEM_DBI_N_PINLOC                   = 0, 
   parameter PORT_MEM_RAS_N_PINLOC                   = 0,
   parameter PORT_MEM_CAS_N_PINLOC                   = 0,
   parameter PORT_MEM_WE_N_PINLOC                    = 0,
   parameter PORT_MEM_REF_N_PINLOC                   = 0,
   
   parameter PORT_MEM_WPS_N_PINLOC                   = 0,
   parameter PORT_MEM_RPS_N_PINLOC                   = 0,
   parameter PORT_MEM_BWS_N_PINLOC                   = 0,
   parameter PORT_MEM_DQA_PINLOC                     = 0,
   parameter PORT_MEM_DQB_PINLOC                     = 0,
   parameter PORT_MEM_Q_PINLOC                       = 0,
   parameter PORT_MEM_D_PINLOC                       = 0,
   parameter PORT_MEM_RWA_N_PINLOC                   = 0,
   parameter PORT_MEM_RWB_N_PINLOC                   = 0,
   parameter PORT_MEM_QKA_PINLOC                     = 0,
   parameter PORT_MEM_QKB_PINLOC                     = 0,
   parameter PORT_MEM_LDA_N_PINLOC                   = 0,
   parameter PORT_MEM_LDB_N_PINLOC                   = 0,
   parameter PORT_MEM_CK_PINLOC                      = 0,
   parameter PORT_MEM_DINVA_PINLOC                   = 0,
   parameter PORT_MEM_DINVB_PINLOC                   = 0,
   parameter PORT_MEM_AINV_PINLOC                    = 0,
   
   parameter PORT_MEM_A_WIDTH                        = 0,
   parameter PORT_MEM_BA_WIDTH                       = 0,
   parameter PORT_MEM_BG_WIDTH                       = 0,
   parameter PORT_MEM_CS_N_WIDTH                     = 0,
   parameter PORT_MEM_ACT_N_WIDTH                    = 0,
   parameter PORT_MEM_DBI_N_WIDTH                    = 0,
   parameter PORT_MEM_RAS_N_WIDTH                    = 0,
   parameter PORT_MEM_CAS_N_WIDTH                    = 0,
   parameter PORT_MEM_WE_N_WIDTH                     = 0,
   parameter PORT_MEM_DM_WIDTH                       = 0,
   parameter PORT_MEM_REF_N_WIDTH                    = 0,
   parameter PORT_MEM_WPS_N_WIDTH                    = 0,
   parameter PORT_MEM_RPS_N_WIDTH                    = 0,
   parameter PORT_MEM_BWS_N_WIDTH                    = 0,
   parameter PORT_MEM_DQA_WIDTH                      = 0,
   parameter PORT_MEM_DQB_WIDTH                      = 0,
   parameter PORT_MEM_Q_WIDTH                        = 0,
   parameter PORT_MEM_D_WIDTH                        = 0,
   parameter PORT_MEM_RWA_N_WIDTH                    = 0,
   parameter PORT_MEM_RWB_N_WIDTH                    = 0,
   parameter PORT_MEM_QKA_WIDTH                      = 0,
   parameter PORT_MEM_QKB_WIDTH                      = 0,
   parameter PORT_MEM_LDA_N_WIDTH                    = 0,
   parameter PORT_MEM_LDB_N_WIDTH                    = 0,
   parameter PORT_MEM_CK_WIDTH                       = 0,
   parameter PORT_MEM_DINVA_WIDTH                    = 0,
   parameter PORT_MEM_DINVB_WIDTH                    = 0,
   parameter PORT_MEM_AINV_WIDTH                     = 0,
   parameter DIAG_USE_ABSTRACT_PHY                   = 0,
   parameter DIAG_ABSTRACT_PHY_WLAT                  = 0,
   parameter DIAG_ABSTRACT_PHY_RLAT                  = 0,
   parameter ABPHY_WRITE_PROTOCOL                    = 1
   ) (
   input  logic                                                                                  global_reset_n_int,  
   output logic                                                                                  phy_reset_n_abphy,         

   input  logic                                                                                  pll_locked,          
   input  logic                                                                                  pll_dll_clk,         
   input  logic [7:0]                                                                            phy_clk_phs,         
   input  logic [1:0]                                                                            phy_clk,             
   input  logic                                                                                  phy_fb_clk_to_tile,  
   output logic                                                                                  phy_fb_clk_to_pll_abphy,   
   
   output logic [1:0]                                                                            core_clks_from_cpa_pri_abphy,   // Core clock signals from the CPA of primary interface
   output logic [1:0]                                                                            core_clks_locked_cpa_pri_abphy, // Core clock locked signals from the CPA of primary interface
   input  logic [1:0]                                                                            core_clks_fb_to_cpa_pri,  // Core clock feedback signals to the CPA of primary interface
   output logic [1:0]                                                                            core_clks_from_cpa_sec_abphy,   // Core clock signals from the CPA of secondary interface (ping-pong only)
   output logic [1:0]                                                                            core_clks_locked_cpa_sec_abphy, // Core clock locked signals from the CPA of secondary interface (ping-pong only)
   input  logic [1:0]                                                                            core_clks_fb_to_cpa_sec,  // Core clock feedback signals to the CPA of secondary interface (ping-pong only)

   input  logic [59:0]                                                                           core2ctl_avl_0,
   input  logic [59:0]                                                                           core2ctl_avl_1,
   input  logic                                                                                  core2ctl_avl_rd_data_ready_0,
   input  logic                                                                                  core2ctl_avl_rd_data_ready_1,
   output logic                                                                                  ctl2core_avl_cmd_ready_0_abphy,
   output logic                                                                                  ctl2core_avl_cmd_ready_1_abphy,
   output logic [12:0]                                                                           ctl2core_avl_rdata_id_0_abphy,
   output logic [12:0]                                                                           ctl2core_avl_rdata_id_1_abphy,
   input  logic                                                                                  core2l_wr_data_vld_ast_0,
   input  logic                                                                                  core2l_wr_data_vld_ast_1,
   input  logic                                                                                  core2l_rd_data_rdy_ast_0,
   input  logic                                                                                  core2l_rd_data_rdy_ast_1,

   output logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0]                                       l2core_rd_data_vld_avl0_abphy,
   output logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0]                                       l2core_wr_data_rdy_ast_abphy,
   
   input  logic [12:0]                                                                           core2l_wr_ecc_info_0,
   input  logic [12:0]                                                                           core2l_wr_ecc_info_1,
   output logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][11:0]                                 l2core_wb_pointer_for_ecc_abphy,
      
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][PINS_PER_LANE * 8 - 1:0]              core2l_data,
   output logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][PINS_PER_LANE * 8 - 1:0]              l2core_data_abphy,
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][PINS_PER_LANE * 4 - 1:0]              core2l_oe,
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][3:0]                                  core2l_rdata_en_full,
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][15:0]                                 core2l_mrnk_read,
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][15:0]                                 core2l_mrnk_write,  
   output logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][3:0]                                  l2core_rdata_valid_abphy,
   output logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][5:0]                                  l2core_afi_rlat_abphy,
   output logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][5:0]                                  l2core_afi_wlat_abphy,

   input  [16:0]                                                                                 c2t_afi,
   output [25:0]                                                                                 t2c_afi_abphy,   
   
   input  logic [41:0]                                                                           core2ctl_sideband_0,
   output logic [13:0]                                                                           ctl2core_sideband_0_abphy,
   input  logic [41:0]                                                                           core2ctl_sideband_1,
   output logic [13:0]                                                                           ctl2core_sideband_1_abphy,

   output logic [33:0]                                                                           ctl2core_mmr_0_abphy,
   input  logic [50:0]                                                                           core2ctl_mmr_0,
   output logic [33:0]                                                                           ctl2core_mmr_1_abphy,
   input  logic [50:0]                                                                           core2ctl_mmr_1,

   output logic [PINS_IN_RTL_TILES-1:0]                                                          l2b_data_abphy,         
   output logic [PINS_IN_RTL_TILES-1:0]                                                          l2b_oe_abphy,           
   output logic [PINS_IN_RTL_TILES-1:0]                                                          l2b_dtc_abphy,          
   input  logic [PINS_IN_RTL_TILES-1:0]                                                          b2l_data,         
   input  logic [LANES_IN_RTL_TILES-1:0]                                                         b2t_dqs,          
   input  logic [LANES_IN_RTL_TILES-1:0]                                                         b2t_dqsb,         

   // Avalon-MM bus for the calibration commands between io_aux and tiles
   input  logic                                                                                  cal_bus_clk,
   input  logic                                                                                  cal_bus_avl_write,
   input  logic [19:0]                                                                           cal_bus_avl_address,
   input  logic [31:0]                                                                           cal_bus_avl_write_data,

   input  logic                                                                                  pa_dprio_clk,
   input  logic                                                                                  pa_dprio_read,
   input  logic [PORT_DFT_NF_PA_DPRIO_REG_ADDR_WIDTH-1:0]                                        pa_dprio_reg_addr,
   input  logic                                                                                  pa_dprio_rst_n,
   input  logic                                                                                  pa_dprio_write,
   input  logic [PORT_DFT_NF_PA_DPRIO_WRITEDATA_WIDTH-1:0]                                       pa_dprio_writedata,
   output logic                                                                                  pa_dprio_block_select_abphy,
   output logic [PORT_DFT_NF_PA_DPRIO_READDATA_WIDTH-1:0]                                        pa_dprio_readdata_abphy,
   
   input logic                                                                                   afi_cal_success,
   output logic                                                                                  runAbstractPhySim
);
   timeunit 1ns;
   timeprecision 1ps;

   typedef enum bit [2:0] {
      LANE_USAGE_UNUSED  = 3'b000,
      LANE_USAGE_AC_HMC  = 3'b001,
      LANE_USAGE_AC_CORE = 3'b010,
      LANE_USAGE_RDATA   = 3'b011,
      LANE_USAGE_WDATA   = 3'b100,
      LANE_USAGE_WRDATA  = 3'b101
   } LANE_USAGE;

   typedef enum bit [0:0] {
      PIN_USAGE_UNUSED   = 1'b0,
      PIN_USAGE_USED     = 1'b1
   } PIN_USAGE;

   typedef enum bit [0:0] {
      PIN_RATE_DDR       = 1'b0,
      PIN_RATE_SDR       = 1'b1
   } PIN_RATE;
   
   typedef enum bit [0:0] {
      PIN_OCT_STATIC_OFF = 1'b0,
      PIN_OCT_DYNAMIC    = 1'b1
   } PIN_OCT_MODE;

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
   
   localparam USE_HMC_RC_OR_DP = (C2P_P2C_CLK_RATIO == PHY_HMC_CLK_RATIO) ? 0 : 1;
      
   logic [NUM_OF_RTL_TILES-1:0] t2c_seq2core_reset_n;
   assign phy_reset_n_abphy = t2c_seq2core_reset_n[PRI_AC_TILE_INDEX];
   
   logic [(NUM_OF_RTL_TILES * (LANES_PER_TILE + 1)):0] pa_sync_data_up_chain;
   logic [(NUM_OF_RTL_TILES * (LANES_PER_TILE + 1)):0] pa_sync_data_dn_chain;
   logic [(NUM_OF_RTL_TILES * (LANES_PER_TILE + 1)):0] pa_sync_clk_up_chain;
   logic [(NUM_OF_RTL_TILES * (LANES_PER_TILE + 1)):0] pa_sync_clk_dn_chain;
   assign pa_sync_data_dn_chain[NUM_OF_RTL_TILES * (LANES_PER_TILE + 1)] = 1'b1;
   assign pa_sync_clk_dn_chain [NUM_OF_RTL_TILES * (LANES_PER_TILE + 1)] = 1'b1;
   assign pa_sync_data_up_chain[0] = 1'b1;
   assign pa_sync_clk_up_chain [0] = 1'b1;
      
   wire                                                                                    cal_bus_clk_force;
   wire                                                                                    cal_bus_avl_read;           
   wire   [31:0]                                                                           cal_bus_avl_read_data;
   wire                                                                                    cal_bus_avl_write_force;
   wire   [19:0]                                                                           cal_bus_avl_address_force;
   wire   [31:0]                                                                           cal_bus_avl_write_data_force;

   assign cal_bus_avl_read = 'd0;
   assign cal_bus_avl_read_data = 'd0;

   logic [(NUM_OF_RTL_TILES * (LANES_PER_TILE + 1)):0][54:0] cal_bus_avl_up_chain;
   assign cal_bus_avl_up_chain[0][19:0]  = cal_bus_avl_address_force;
   assign cal_bus_avl_up_chain[0][51:20] = cal_bus_avl_write_data_force;
   assign cal_bus_avl_up_chain[0][52]    = cal_bus_avl_write_force;
   assign cal_bus_avl_up_chain[0][53]    = cal_bus_avl_read;
   assign cal_bus_avl_up_chain[0][54]    = cal_bus_clk_force;
      
   logic [(NUM_OF_RTL_TILES * (LANES_PER_TILE + 1)):0][31:0] cal_bus_avl_read_data_dn_chain;
   assign cal_bus_avl_read_data_dn_chain[NUM_OF_RTL_TILES * (LANES_PER_TILE + 1)] = 32'b0;
   
   logic [(NUM_OF_RTL_TILES * LANES_PER_TILE):0] broadcast_up_chain;
   logic [(NUM_OF_RTL_TILES * LANES_PER_TILE):0] broadcast_dn_chain;
   assign broadcast_dn_chain[NUM_OF_RTL_TILES * LANES_PER_TILE] = 1'b1;
   assign broadcast_up_chain[0] = 1'b1;
   
   logic [NUM_OF_RTL_TILES:0][50:0] all_tiles_ctl2dbc0_dn_chain;
   logic [NUM_OF_RTL_TILES:0][50:0] all_tiles_ctl2dbc1_up_chain;
   assign all_tiles_ctl2dbc0_dn_chain[NUM_OF_RTL_TILES] = {51{1'b1}};
   assign all_tiles_ctl2dbc1_up_chain[0] = {51{1'b1}};
   
   logic [NUM_OF_RTL_TILES:0][47:0] all_tiles_ping_pong_up_chain;
   assign all_tiles_ping_pong_up_chain[0] = {48{1'b1}};
   
   logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][7:0] all_tiles_t2l_phy_clk_phs;
   logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][1:0] all_tiles_t2l_phy_clk;   

   logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0] all_tiles_dll_clk_out;

   logic [NUM_OF_RTL_TILES-1:0][1:0] all_tiles_core_clks_out;
   logic [NUM_OF_RTL_TILES-1:0][1:0] all_tiles_core_clks_fb_in;
   logic [NUM_OF_RTL_TILES-1:0][1:0] all_tiles_core_clks_locked;
      
   assign core_clks_from_cpa_pri_abphy = all_tiles_core_clks_out[PRI_AC_TILE_INDEX];
   assign core_clks_locked_cpa_pri_abphy = all_tiles_core_clks_locked[PRI_AC_TILE_INDEX];
   assign all_tiles_core_clks_fb_in[PRI_AC_TILE_INDEX] = core_clks_fb_to_cpa_pri;

   assign core_clks_from_cpa_sec_abphy = PHY_PING_PONG_EN ? all_tiles_core_clks_out[SEC_AC_TILE_INDEX] : '0;
   assign core_clks_locked_cpa_sec_abphy = PHY_PING_PONG_EN ? all_tiles_core_clks_locked[SEC_AC_TILE_INDEX] : '0;
   generate
      if (PHY_PING_PONG_EN) begin
         assign all_tiles_core_clks_fb_in[SEC_AC_TILE_INDEX] = core_clks_fb_to_cpa_sec;
      end
   endgenerate
   
   logic [NUM_OF_RTL_TILES-1:0] all_tiles_phy_fb_clk_to_pll;
   assign phy_fb_clk_to_pll_abphy = all_tiles_phy_fb_clk_to_pll[PRI_AC_TILE_INDEX];

   logic [NUM_OF_RTL_TILES-1:0]       all_tiles_ctl2core_avl_cmd_ready;
   logic [NUM_OF_RTL_TILES-1:0][12:0] all_tiles_ctl2core_avl_rdata_id;

   assign ctl2core_avl_cmd_ready_0_abphy = all_tiles_ctl2core_avl_cmd_ready[PRI_AC_TILE_INDEX];
   assign ctl2core_avl_rdata_id_0_abphy  = all_tiles_ctl2core_avl_rdata_id[PRI_AC_TILE_INDEX];
   
   assign ctl2core_avl_cmd_ready_1_abphy = all_tiles_ctl2core_avl_cmd_ready[SEC_AC_TILE_INDEX];
   assign ctl2core_avl_rdata_id_1_abphy  = all_tiles_ctl2core_avl_rdata_id[SEC_AC_TILE_INDEX];
   
   logic [NUM_OF_RTL_TILES-1:0][16:0] all_tiles_c2t_afi;
   logic [NUM_OF_RTL_TILES-1:0][25:0] all_tiles_t2c_afi;
   
   assign all_tiles_c2t_afi[PRI_AC_TILE_INDEX] = c2t_afi;
   assign t2c_afi_abphy = all_tiles_t2c_afi[PRI_AC_TILE_INDEX];

   logic [NUM_OF_RTL_TILES-1:0][13:0] all_tiles_ctl2core_sideband;

   assign ctl2core_sideband_0_abphy = all_tiles_ctl2core_sideband[PRI_AC_TILE_INDEX];
   assign ctl2core_sideband_1_abphy = all_tiles_ctl2core_sideband[SEC_AC_TILE_INDEX];

   logic [NUM_OF_RTL_TILES-1:0][33:0] all_tiles_ctl2core_mmr;

   assign ctl2core_mmr_0_abphy = all_tiles_ctl2core_mmr[PRI_AC_TILE_INDEX];
   assign ctl2core_mmr_1_abphy = all_tiles_ctl2core_mmr[SEC_AC_TILE_INDEX];
   
   logic [NUM_OF_RTL_TILES-1:0]                                          all_tiles_pa_dprio_block_select;
   logic [NUM_OF_RTL_TILES-1:0][PORT_DFT_NF_PA_DPRIO_READDATA_WIDTH-1:0] all_tiles_pa_dprio_readdata;
   
   assign pa_dprio_readdata_abphy = all_tiles_pa_dprio_readdata[PRI_AC_TILE_INDEX];
   assign pa_dprio_block_select_abphy = all_tiles_pa_dprio_block_select[PRI_AC_TILE_INDEX];

   wire      [96*NUM_OF_RTL_TILES*LANES_PER_TILE-1:0]                          ac_hmc_par;        
   wire      [96*NUM_OF_RTL_TILES*LANES_PER_TILE-1:0]                          dq_data_to_mem;       
   wire      [96*NUM_OF_RTL_TILES*LANES_PER_TILE-1:0]                          dq_data_from_mem;     
   wire      [3:0]                                                             rdata_valid_local [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0];
   wire      [48*NUM_OF_RTL_TILES*LANES_PER_TILE-1:0]                          dq_oe;

   integer                                                                     add_2 [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0];

`define _abphy_get_pin_index(_loc, _port_i) ( _loc[ (_port_i + 1) * 10 +: 10 ] )
`define _abphy_get_tile(_loc, _port_i) (  `_abphy_get_pin_index(_loc, _port_i) / (PINS_PER_LANE * LANES_PER_TILE) )
`define _abphy_get_lane(_loc, _port_i) ( (`_abphy_get_pin_index(_loc, _port_i) / PINS_PER_LANE) % LANES_PER_TILE ) 
                
   initial begin
     runAbstractPhySim  = 1;
   end
                
   // synthesis translate_off
   integer fileID,fileMentorID,r;
   string sim_loc,force_file,force_file_mentor,line_in,sub_line_in;
   reg afi_cal_success_delay;
   initial begin
     afi_cal_success_delay = 0;
     @ ( posedge afi_cal_success );
     repeat (20) @ ( posedge cal_bus_clk );
     afi_cal_success_delay = 1;
   end
   
   
   integer min_wlat,wlat,wlat_offset,rlat;
   initial begin
     @ (posedge global_reset_n_int);
     if ( global_reset_n_int !==1'b1 )
       @ (posedge global_reset_n_int);
     min_wlat              = 2;

     if ( NUM_OF_HMC_PORTS>0 ) begin
       if ( SEC_AC_TILE_INDEX>-1 ) begin
         if ( ((NUM_OF_RTL_TILES+1)-SEC_AC_TILE_INDEX)>min_wlat ) begin
           min_wlat       = (NUM_OF_RTL_TILES+1)-SEC_AC_TILE_INDEX;
         end
         if ( (SEC_AC_TILE_INDEX+1)>min_wlat ) begin
           min_wlat       = SEC_AC_TILE_INDEX+1;
         end
       end
       if ( ((NUM_OF_RTL_TILES+1)-PRI_AC_TILE_INDEX)>min_wlat ) begin
         min_wlat         = (NUM_OF_RTL_TILES+1)-PRI_AC_TILE_INDEX;
       end
       if ( (PRI_AC_TILE_INDEX+1)>min_wlat ) begin
         min_wlat         = PRI_AC_TILE_INDEX+1;
       end
     end
     if ( DIAG_VERBOSE_IOAUX!=0 ) $display("min wlat=%d",min_wlat);
     if ( DIAG_ABSTRACT_PHY_WLAT<min_wlat ) begin
       wlat             = min_wlat;
     end
     else begin
       wlat             = DIAG_ABSTRACT_PHY_WLAT;;
     end
     rlat               = DIAG_ABSTRACT_PHY_RLAT;;
     if ( rlat<(wlat+6) ) 
       rlat             = wlat+6;

     if ( DIAG_VERBOSE_IOAUX!=0 ) $display("post min wlat=%d rlat=%d",wlat,rlat);
   end   
         
   generate
      genvar tile_i, lane_i;
      for (tile_i = 0; tile_i < NUM_OF_RTL_TILES; ++tile_i) begin: tile_gen

         logic [1:0]       t2l_dqsbus_x4 [LANES_PER_TILE-1:0]; 
         logic [1:0]       t2l_dqsbus_x8 [LANES_PER_TILE-1:0];
         logic [1:0]       t2l_dqsbus_x18 [LANES_PER_TILE-1:0];
         logic [1:0]       t2l_dqsbus_x36 [LANES_PER_TILE-1:0];

         logic [3:0][95:0] t2l_ac_hmc;

         logic [16:0]      t2l_cfg_dbc [LANES_PER_TILE-1:0];

         logic [22:0]      l2t_dbc2ctl [LANES_PER_TILE-1:0];


         twentynm_tile_ctrl # (
            .silicon_rev                      (SILICON_REV),
            .hps_ctrl_en                      (IS_HPS ? "true" : "false"),
            .pa_filter_code                   (PLL_VCO_FREQ_MHZ_INT),
            .pa_phase_offset_0                (12'b0),                                             
            .pa_phase_offset_1                (12'b0),                                             
            .pa_exponent_0                    (`_get_pa_exponent_0),                               
            .pa_exponent_1                    (`_get_pa_exponent_1),                               
            .pa_mantissa_0                    (5'b0),                                              
            .pa_mantissa_1                    (5'b0),                                              
            .pa_feedback_divider_c0           (`_get_pa_feedback_divider_c0),                      
            .pa_feedback_divider_c1           ("div_by_1_c1"),                                     
            .pa_feedback_divider_p0           (`_get_pa_feedback_divider_p0),                      
            .pa_feedback_divider_p1           ("div_by_1_p1"),                                     
            .pa_feedback_mux_sel_0            ("fb2_p_clk_0"),                                     
            .pa_feedback_mux_sel_1            (DIAG_CPA_OUT_1_EN ? "fb0_p_clk_1" : "fb2_p_clk_1"), 
            .pa_freq_track_speed              (4'hd),
            .pa_track_speed                   (5'h18),                                         
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
            .hmc_clkgating_en                 ("disable"),                                     
            .hmc_ctrl_output_regd             ("disable"),                                     
            .hmc_dbc0_output_regd             ("disable"),                                     
            .hmc_dbc1_output_regd             ("disable"),                                     
            .hmc_dbc2_output_regd             ("disable"),                                     
            .hmc_dbc3_output_regd             ("disable"),                                     
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
            .hmc_ctrl_dualport_en             ("disable"),                           
            .hmc_dbc0_dualport_en             ("disable"),                           
            .hmc_dbc1_dualport_en             ("disable"),                           
            .hmc_dbc2_dualport_en             ("disable"),                           
            .hmc_dbc3_dualport_en             ("disable"),                           
            .hmc_tile_id                      (tile_i[4:0]),                         
            .physeq_tile_id                   (`_get_center_tid(tile_i)),            
            .physeq_bc_id_ena                 ("bc_enable"),                         
            .physeq_avl_ena                   ("avl_enable"),                        
            .physeq_hmc_or_core               (`_get_hmc_or_core),                   
            .physeq_trk_mgr_mrnk_mode         ("one_rank"),
            .physeq_trk_mgr_read_monitor_ena  ("disable"),                           
            .physeq_hmc_id                    (`_get_hmc_tid(tile_i)),               
            .physeq_reset_auto_release        ("avl"),                               
            .physeq_rwlat_mode                ("avl_vlu"),                           
            .physeq_afi_rlat_vlu              (6'b000000),                           
            .physeq_afi_wlat_vlu              (6'b000000),                           
            .hmc_second_clk_src               (USE_HMC_RC_OR_DP ? "clk1" : "clk0"),  
            .physeq_seq_feature               (21'b000000000000000000000),
            .hmc_ctrl_enable_ecc              (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ENABLE_ECC                , SEC_HMC_CFG_ENABLE_ECC                    )),  
            .hmc_dbc0_enable_ecc              (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ENABLE_ECC                , SEC_HMC_CFG_ENABLE_ECC                    )),  
            .hmc_dbc1_enable_ecc              (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ENABLE_ECC                , SEC_HMC_CFG_ENABLE_ECC                    )),  
            .hmc_dbc2_enable_ecc              (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ENABLE_ECC                , SEC_HMC_CFG_ENABLE_ECC                    )),  
            .hmc_dbc3_enable_ecc              (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ENABLE_ECC                , SEC_HMC_CFG_ENABLE_ECC                    )),  
            .hmc_reorder_data                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_REORDER_DATA              , SEC_HMC_CFG_REORDER_DATA                  )),  
            .hmc_reorder_read                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_REORDER_READ              , SEC_HMC_CFG_REORDER_READ                  )),  
            .hmc_ctrl_reorder_rdata           (`_sel_hmc_val(tile_i, PRI_HMC_CFG_REORDER_RDATA             , SEC_HMC_CFG_REORDER_RDATA                 )),  
            .hmc_dbc0_reorder_rdata           (`_sel_hmc_val(tile_i, PRI_HMC_CFG_REORDER_RDATA             , SEC_HMC_CFG_REORDER_RDATA                 )),  
            .hmc_dbc1_reorder_rdata           (`_sel_hmc_val(tile_i, PRI_HMC_CFG_REORDER_RDATA             , SEC_HMC_CFG_REORDER_RDATA                 )),  
            .hmc_dbc2_reorder_rdata           (`_sel_hmc_val(tile_i, PRI_HMC_CFG_REORDER_RDATA             , SEC_HMC_CFG_REORDER_RDATA                 )),  
            .hmc_dbc3_reorder_rdata           (`_sel_hmc_val(tile_i, PRI_HMC_CFG_REORDER_RDATA             , SEC_HMC_CFG_REORDER_RDATA                 )),  
            .hmc_starve_limit                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_STARVE_LIMIT              , SEC_HMC_CFG_STARVE_LIMIT                  )),  
            .hmc_enable_dqs_tracking          (`_sel_hmc_val(tile_i, PRI_HMC_CFG_DQS_TRACKING_EN           , SEC_HMC_CFG_DQS_TRACKING_EN               )),  
            .hmc_arbiter_type                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ARBITER_TYPE              , SEC_HMC_CFG_ARBITER_TYPE                  )),  
            .hmc_open_page_en                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_OPEN_PAGE_EN              , SEC_HMC_CFG_OPEN_PAGE_EN                  )),  
            .hmc_geardn_en                    (`_sel_hmc_val(tile_i, PRI_HMC_CFG_GEAR_DOWN_EN              , SEC_HMC_CFG_GEAR_DOWN_EN                  )),  
            .hmc_rld3_multibank_mode          (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RLD3_MULTIBANK_MODE       , SEC_HMC_CFG_RLD3_MULTIBANK_MODE           )),  
            .hmc_cfg_pinpong_mode             (`_sel_hmc_def(tile_i, PRI_HMC_CFG_PING_PONG_MODE            , SEC_HMC_CFG_PING_PONG_MODE ,"pingpong_off")),  
            .hmc_ctrl_slot_rotate_en          (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SLOT_ROTATE_EN            , SEC_HMC_CFG_SLOT_ROTATE_EN                )),  
            .hmc_dbc0_slot_rotate_en          (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SLOT_ROTATE_EN            , SEC_HMC_CFG_SLOT_ROTATE_EN                )),  
            .hmc_dbc1_slot_rotate_en          (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SLOT_ROTATE_EN            , SEC_HMC_CFG_SLOT_ROTATE_EN                )),  
            .hmc_dbc2_slot_rotate_en          (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SLOT_ROTATE_EN            , SEC_HMC_CFG_SLOT_ROTATE_EN                )),  
            .hmc_dbc3_slot_rotate_en          (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SLOT_ROTATE_EN            , SEC_HMC_CFG_SLOT_ROTATE_EN                )),  
            .hmc_ctrl_slot_offset             (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SLOT_OFFSET               , SEC_HMC_CFG_SLOT_OFFSET                   )),  
            .hmc_dbc0_slot_offset             (`_sel_hmc_lane(tile_i, 0, PRI_HMC_CFG_SLOT_OFFSET           , SEC_HMC_CFG_SLOT_OFFSET                   )),  
            .hmc_dbc1_slot_offset             (`_sel_hmc_lane(tile_i, 1, PRI_HMC_CFG_SLOT_OFFSET           , SEC_HMC_CFG_SLOT_OFFSET                   )),  
            .hmc_dbc2_slot_offset             (`_sel_hmc_lane(tile_i, 2, PRI_HMC_CFG_SLOT_OFFSET           , SEC_HMC_CFG_SLOT_OFFSET                   )),  
            .hmc_dbc3_slot_offset             (`_sel_hmc_lane(tile_i, 3, PRI_HMC_CFG_SLOT_OFFSET           , SEC_HMC_CFG_SLOT_OFFSET                   )),  
            .hmc_col_cmd_slot                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_COL_CMD_SLOT              , SEC_HMC_CFG_COL_CMD_SLOT                  )),  
            .hmc_row_cmd_slot                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ROW_CMD_SLOT              , SEC_HMC_CFG_ROW_CMD_SLOT                  )),  
            .hmc_ctrl_rc_en                   (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ENABLE_RC                 , SEC_HMC_CFG_ENABLE_RC                     )),  
            .hmc_dbc0_rc_en                   (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ENABLE_RC                 , SEC_HMC_CFG_ENABLE_RC                     )),  
            .hmc_dbc1_rc_en                   (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ENABLE_RC                 , SEC_HMC_CFG_ENABLE_RC                     )),  
            .hmc_dbc2_rc_en                   (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ENABLE_RC                 , SEC_HMC_CFG_ENABLE_RC                     )),  
            .hmc_dbc3_rc_en                   (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ENABLE_RC                 , SEC_HMC_CFG_ENABLE_RC                     )),  
            .hmc_cs_chip                      (`_sel_hmc_val(tile_i, PRI_HMC_CFG_CS_TO_CHIP_MAPPING        , SEC_HMC_CFG_CS_TO_CHIP_MAPPING            )),  
            .hmc_rb_reserved_entry            (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RB_RESERVED_ENTRY         , SEC_HMC_CFG_RB_RESERVED_ENTRY             )),  
            .hmc_wb_reserved_entry            (`_sel_hmc_val(tile_i, PRI_HMC_CFG_WB_RESERVED_ENTRY         , SEC_HMC_CFG_WB_RESERVED_ENTRY             )),  
            .hmc_tcl                          (`_sel_hmc_val(tile_i, PRI_HMC_CFG_TCL                       , SEC_HMC_CFG_TCL                           )),  
            .hmc_power_saving_exit_cycles     (`_sel_hmc_val(tile_i, PRI_HMC_CFG_POWER_SAVING_EXIT_CYC     , SEC_HMC_CFG_POWER_SAVING_EXIT_CYC         )),  
            .hmc_mem_clk_disable_entry_cycles (`_sel_hmc_val(tile_i, PRI_HMC_CFG_MEM_CLK_DISABLE_ENTRY_CYC , SEC_HMC_CFG_MEM_CLK_DISABLE_ENTRY_CYC     )),  
            .hmc_write_odt_chip               (`_sel_hmc_val(tile_i, PRI_HMC_CFG_WRITE_ODT_CHIP            , SEC_HMC_CFG_WRITE_ODT_CHIP                )),  
            .hmc_read_odt_chip                (`_sel_hmc_val(tile_i, PRI_HMC_CFG_READ_ODT_CHIP             , SEC_HMC_CFG_READ_ODT_CHIP                 )),  
            .hmc_wr_odt_on                    (`_sel_hmc_val(tile_i, PRI_HMC_CFG_WR_ODT_ON                 , SEC_HMC_CFG_WR_ODT_ON                     )),  
            .hmc_rd_odt_on                    (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RD_ODT_ON                 , SEC_HMC_CFG_RD_ODT_ON                     )),  
            .hmc_wr_odt_period                (`_sel_hmc_val(tile_i, PRI_HMC_CFG_WR_ODT_PERIOD             , SEC_HMC_CFG_WR_ODT_PERIOD                 )),  
            .hmc_rd_odt_period                (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RD_ODT_PERIOD             , SEC_HMC_CFG_RD_ODT_PERIOD                 )),  
            .hmc_rld3_refresh_seq0            (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RLD3_REFRESH_SEQ0         , SEC_HMC_CFG_RLD3_REFRESH_SEQ0             )),  
            .hmc_rld3_refresh_seq1            (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RLD3_REFRESH_SEQ1         , SEC_HMC_CFG_RLD3_REFRESH_SEQ1             )),  
            .hmc_rld3_refresh_seq2            (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RLD3_REFRESH_SEQ2         , SEC_HMC_CFG_RLD3_REFRESH_SEQ2             )),  
            .hmc_rld3_refresh_seq3            (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RLD3_REFRESH_SEQ3         , SEC_HMC_CFG_RLD3_REFRESH_SEQ3             )),  
            .hmc_srf_zqcal_disable            (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SRF_ZQCAL_DISABLE         , SEC_HMC_CFG_SRF_ZQCAL_DISABLE             )),  
            .hmc_mps_zqcal_disable            (`_sel_hmc_val(tile_i, PRI_HMC_CFG_MPS_ZQCAL_DISABLE         , SEC_HMC_CFG_MPS_ZQCAL_DISABLE             )),  
            .hmc_short_dqstrk_ctrl_en         (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SHORT_DQSTRK_CTRL_EN      , SEC_HMC_CFG_SHORT_DQSTRK_CTRL_EN          )),  
            .hmc_period_dqstrk_ctrl_en        (`_sel_hmc_val(tile_i, PRI_HMC_CFG_PERIOD_DQSTRK_CTRL_EN     , SEC_HMC_CFG_PERIOD_DQSTRK_CTRL_EN         )),  
            .hmc_period_dqstrk_interval       (`_sel_hmc_val(tile_i, PRI_HMC_CFG_PERIOD_DQSTRK_INTERVAL    , SEC_HMC_CFG_PERIOD_DQSTRK_INTERVAL        )),  
            .hmc_dqstrk_to_valid_last         (`_sel_hmc_val(tile_i, PRI_HMC_CFG_DQSTRK_TO_VALID_LAST      , SEC_HMC_CFG_DQSTRK_TO_VALID_LAST          )),  
            .hmc_dqstrk_to_valid              (`_sel_hmc_val(tile_i, PRI_HMC_CFG_DQSTRK_TO_VALID           , SEC_HMC_CFG_DQSTRK_TO_VALID               )),  
            .hmc_rfsh_warn_threshold          (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RFSH_WARN_THRESHOLD       , SEC_HMC_CFG_RFSH_WARN_THRESHOLD           )),  
            .hmc_mps_dqstrk_disable           (`_sel_hmc_val(tile_i, PRI_HMC_CFG_MPS_DQSTRK_DISABLE        , SEC_HMC_CFG_MPS_DQSTRK_DISABLE            )),  
            .hmc_sb_cg_disable                (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SB_CG_DISABLE             , SEC_HMC_CFG_SB_CG_DISABLE                 )),  
            .hmc_user_rfsh_en                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_USER_RFSH_EN              , SEC_HMC_CFG_USER_RFSH_EN                  )),  
            .hmc_srf_autoexit_en              (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SRF_AUTOEXIT_EN           , SEC_HMC_CFG_SRF_AUTOEXIT_EN               )),  
            .hmc_srf_entry_exit_block         (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SRF_ENTRY_EXIT_BLOCK      , SEC_HMC_CFG_SRF_ENTRY_EXIT_BLOCK          )),  
            .hmc_sb_ddr4_mr3                  (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SB_DDR4_MR3               , SEC_HMC_CFG_SB_DDR4_MR3                   )),  
            .hmc_sb_ddr4_mr4                  (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SB_DDR4_MR4               , SEC_HMC_CFG_SB_DDR4_MR4                   )),  
            .hmc_sb_ddr4_mr5                  (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SB_DDR4_MR5               , SEC_HMC_CFG_SB_DDR4_MR5                   )),  
            .hmc_ddr4_mps_addr_mirror         (`_sel_hmc_val(tile_i, PRI_HMC_CFG_DDR4_MPS_ADDR_MIRROR      , SEC_HMC_CFG_DDR4_MPS_ADDR_MIRROR          )),  
            .hmc_mem_if_coladdr_width         (`_sel_hmc_val(tile_i, PRI_HMC_CFG_MEM_IF_COLADDR_WIDTH      , SEC_HMC_CFG_MEM_IF_COLADDR_WIDTH          )),  
            .hmc_mem_if_rowaddr_width         (`_sel_hmc_val(tile_i, PRI_HMC_CFG_MEM_IF_ROWADDR_WIDTH      , SEC_HMC_CFG_MEM_IF_ROWADDR_WIDTH          )),  
            .hmc_mem_if_bankaddr_width        (`_sel_hmc_val(tile_i, PRI_HMC_CFG_MEM_IF_BANKADDR_WIDTH     , SEC_HMC_CFG_MEM_IF_BANKADDR_WIDTH         )),  
            .hmc_mem_if_bgaddr_width          (`_sel_hmc_val(tile_i, PRI_HMC_CFG_MEM_IF_BGADDR_WIDTH       , SEC_HMC_CFG_MEM_IF_BGADDR_WIDTH           )),  
            .hmc_local_if_cs_width            (`_sel_hmc_val(tile_i, PRI_HMC_CFG_LOCAL_IF_CS_WIDTH         , SEC_HMC_CFG_LOCAL_IF_CS_WIDTH             )),  
            .hmc_addr_order                   (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ADDR_ORDER                , SEC_HMC_CFG_ADDR_ORDER                    )),  
            .hmc_act_to_rdwr                  (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ACT_TO_RDWR               , SEC_HMC_CFG_ACT_TO_RDWR                   )),  
            .hmc_act_to_pch                   (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ACT_TO_PCH                , SEC_HMC_CFG_ACT_TO_PCH                    )),  
            .hmc_act_to_act                   (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ACT_TO_ACT                , SEC_HMC_CFG_ACT_TO_ACT                    )),  
            .hmc_act_to_act_diff_bank         (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ACT_TO_ACT_DIFF_BANK      , SEC_HMC_CFG_ACT_TO_ACT_DIFF_BANK          )),  
            .hmc_act_to_act_diff_bg           (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ACT_TO_ACT_DIFF_BG        , SEC_HMC_CFG_ACT_TO_ACT_DIFF_BG            )),  
            .hmc_rd_to_rd                     (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RD_TO_RD                  , SEC_HMC_CFG_RD_TO_RD                      )),  
            .hmc_rd_to_rd_diff_chip           (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RD_TO_RD_DIFF_CHIP        , SEC_HMC_CFG_RD_TO_RD_DIFF_CHIP            )),  
            .hmc_rd_to_rd_diff_bg             (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RD_TO_RD_DIFF_BG          , SEC_HMC_CFG_RD_TO_RD_DIFF_BG              )),  
            .hmc_rd_to_wr                     (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RD_TO_WR                  , SEC_HMC_CFG_RD_TO_WR                      )),  
            .hmc_rd_to_wr_diff_chip           (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RD_TO_WR_DIFF_CHIP        , SEC_HMC_CFG_RD_TO_WR_DIFF_CHIP            )),  
            .hmc_rd_to_wr_diff_bg             (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RD_TO_WR_DIFF_BG          , SEC_HMC_CFG_RD_TO_WR_DIFF_BG              )),  
            .hmc_rd_to_pch                    (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RD_TO_PCH                 , SEC_HMC_CFG_RD_TO_PCH                     )),  
            .hmc_rd_ap_to_valid               (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RD_AP_TO_VALID            , SEC_HMC_CFG_RD_AP_TO_VALID                )),  
            .hmc_wr_to_wr                     (`_sel_hmc_val(tile_i, PRI_HMC_CFG_WR_TO_WR                  , SEC_HMC_CFG_WR_TO_WR                      )),  
            .hmc_wr_to_wr_diff_chip           (`_sel_hmc_val(tile_i, PRI_HMC_CFG_WR_TO_WR_DIFF_CHIP        , SEC_HMC_CFG_WR_TO_WR_DIFF_CHIP            )),  
            .hmc_wr_to_wr_diff_bg             (`_sel_hmc_val(tile_i, PRI_HMC_CFG_WR_TO_WR_DIFF_BG          , SEC_HMC_CFG_WR_TO_WR_DIFF_BG              )),  
            .hmc_wr_to_rd                     (`_sel_hmc_val(tile_i, PRI_HMC_CFG_WR_TO_RD                  , SEC_HMC_CFG_WR_TO_RD                      )),  
            .hmc_wr_to_rd_diff_chip           (`_sel_hmc_val(tile_i, PRI_HMC_CFG_WR_TO_RD_DIFF_CHIP        , SEC_HMC_CFG_WR_TO_RD_DIFF_CHIP            )),  
            .hmc_wr_to_rd_diff_bg             (`_sel_hmc_val(tile_i, PRI_HMC_CFG_WR_TO_RD_DIFF_BG          , SEC_HMC_CFG_WR_TO_RD_DIFF_BG              )),  
            .hmc_wr_to_pch                    (`_sel_hmc_val(tile_i, PRI_HMC_CFG_WR_TO_PCH                 , SEC_HMC_CFG_WR_TO_PCH                     )),  
            .hmc_wr_ap_to_valid               (`_sel_hmc_val(tile_i, PRI_HMC_CFG_WR_AP_TO_VALID            , SEC_HMC_CFG_WR_AP_TO_VALID                )),  
            .hmc_pch_to_valid                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_PCH_TO_VALID              , SEC_HMC_CFG_PCH_TO_VALID                  )),  
            .hmc_pch_all_to_valid             (`_sel_hmc_val(tile_i, PRI_HMC_CFG_PCH_ALL_TO_VALID          , SEC_HMC_CFG_PCH_ALL_TO_VALID              )),  
            .hmc_arf_to_valid                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ARF_TO_VALID              , SEC_HMC_CFG_ARF_TO_VALID                  )),  
            .hmc_pdn_to_valid                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_PDN_TO_VALID              , SEC_HMC_CFG_PDN_TO_VALID                  )),  
            .hmc_srf_to_valid                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SRF_TO_VALID              , SEC_HMC_CFG_SRF_TO_VALID                  )),  
            .hmc_srf_to_zq_cal                (`_sel_hmc_val(tile_i, PRI_HMC_CFG_SRF_TO_ZQ_CAL             , SEC_HMC_CFG_SRF_TO_ZQ_CAL                 )),  
            .hmc_arf_period                   (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ARF_PERIOD                , SEC_HMC_CFG_ARF_PERIOD                    )),  
            .hmc_pdn_period                   (`_sel_hmc_val(tile_i, PRI_HMC_CFG_PDN_PERIOD                , SEC_HMC_CFG_PDN_PERIOD                    )),  
            .hmc_zqcl_to_valid                (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ZQCL_TO_VALID             , SEC_HMC_CFG_ZQCL_TO_VALID                 )),  
            .hmc_zqcs_to_valid                (`_sel_hmc_val(tile_i, PRI_HMC_CFG_ZQCS_TO_VALID             , SEC_HMC_CFG_ZQCS_TO_VALID                 )),  
            .hmc_mrs_to_valid                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_MRS_TO_VALID              , SEC_HMC_CFG_MRS_TO_VALID                  )),  
            .hmc_mps_to_valid                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_MPS_TO_VALID              , SEC_HMC_CFG_MPS_TO_VALID                  )),  
            .hmc_mrr_to_valid                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_MRR_TO_VALID              , SEC_HMC_CFG_MRR_TO_VALID                  )),  
            .hmc_mpr_to_valid                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_MPR_TO_VALID              , SEC_HMC_CFG_MPR_TO_VALID                  )),  
            .hmc_mps_exit_cs_to_cke           (`_sel_hmc_val(tile_i, PRI_HMC_CFG_MPS_EXIT_CS_TO_CKE        , SEC_HMC_CFG_MPS_EXIT_CS_TO_CKE            )),  
            .hmc_mps_exit_cke_to_cs           (`_sel_hmc_val(tile_i, PRI_HMC_CFG_MPS_EXIT_CKE_TO_CS        , SEC_HMC_CFG_MPS_EXIT_CKE_TO_CS            )),  
            .hmc_rld3_multibank_ref_delay     (`_sel_hmc_val(tile_i, PRI_HMC_CFG_RLD3_MULTIBANK_REF_DELAY  , SEC_HMC_CFG_RLD3_MULTIBANK_REF_DELAY      )),  
            .hmc_mmr_cmd_to_valid             (`_sel_hmc_val(tile_i, PRI_HMC_CFG_MMR_CMD_TO_VALID          , SEC_HMC_CFG_MMR_CMD_TO_VALID              )),  
            .hmc_4_act_to_act                 (`_sel_hmc_val(tile_i, PRI_HMC_CFG_4_ACT_TO_ACT              , SEC_HMC_CFG_4_ACT_TO_ACT                  )),  
            .hmc_16_act_to_act                (`_sel_hmc_val(tile_i, PRI_HMC_CFG_16_ACT_TO_ACT             , SEC_HMC_CFG_16_ACT_TO_ACT                 )),  
            .mode                             ("tile_ddr")
            
         ) tile_ctrl_inst (

            .global_reset_n                   (global_reset_n_int),

            .pll_locked_in                    (pll_locked),
            .pll_vco_in                       (phy_clk_phs),                        
            .phy_clk_in                       (phy_clk),                            

            .pa_core_clk_in                   (all_tiles_core_clks_fb_in[tile_i]),  
            .pa_core_clk_out                  (all_tiles_core_clks_out[tile_i]),    
            .pa_locked                        (all_tiles_core_clks_locked[tile_i]), 
            .pa_reset_n                       (global_reset_n_int),                 
            .pa_core_in                       (12'b000000000000),                   
            .pa_fbclk_in                      (phy_fb_clk_to_tile),                 
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
            
            .phy_clk_out0                     ({all_tiles_t2l_phy_clk[tile_i][0], all_tiles_t2l_phy_clk_phs[tile_i][0]}), 
            .phy_clk_out1                     ({all_tiles_t2l_phy_clk[tile_i][1], all_tiles_t2l_phy_clk_phs[tile_i][1]}), 
            .phy_clk_out2                     ({all_tiles_t2l_phy_clk[tile_i][2], all_tiles_t2l_phy_clk_phs[tile_i][2]}), 
            .phy_clk_out3                     ({all_tiles_t2l_phy_clk[tile_i][3], all_tiles_t2l_phy_clk_phs[tile_i][3]}), 
            .phy_fbclk_out                    (all_tiles_phy_fb_clk_to_pll[tile_i]),                                      

            .dll_clk_in                       (pll_dll_clk),                       
            .dll_clk_out0                     (all_tiles_dll_clk_out[tile_i][0]),  
            .dll_clk_out1                     (all_tiles_dll_clk_out[tile_i][1]),  
            .dll_clk_out2                     (all_tiles_dll_clk_out[tile_i][2]),  
            .dll_clk_out3                     (all_tiles_dll_clk_out[tile_i][3]),  
            
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

            .afi_cmd_bus                      (t2l_ac_hmc),

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
            
            .dqs_in_x8_0                      (DQS_BUS_MODE_ENUM == "DQS_BUS_MODE_X8_X9" ? {b2t_dqsb[(tile_i * LANES_PER_TILE) + 0], b2t_dqs[(tile_i * LANES_PER_TILE) + 0]} : 2'b0),
            .dqs_in_x8_1                      (DQS_BUS_MODE_ENUM == "DQS_BUS_MODE_X8_X9" ? {b2t_dqsb[(tile_i * LANES_PER_TILE) + 1], b2t_dqs[(tile_i * LANES_PER_TILE) + 1]} : 2'b0),
            .dqs_in_x8_2                      (DQS_BUS_MODE_ENUM == "DQS_BUS_MODE_X8_X9" ? {b2t_dqsb[(tile_i * LANES_PER_TILE) + 2], b2t_dqs[(tile_i * LANES_PER_TILE) + 2]} : 2'b0),
            .dqs_in_x8_3                      (DQS_BUS_MODE_ENUM == "DQS_BUS_MODE_X8_X9" ? {b2t_dqsb[(tile_i * LANES_PER_TILE) + 3], b2t_dqs[(tile_i * LANES_PER_TILE) + 3]} : 2'b0),
            .dqs_out_x8_lane0                 (t2l_dqsbus_x8[0]),
            .dqs_out_x8_lane1                 (t2l_dqsbus_x8[1]),
            .dqs_out_x8_lane2                 (t2l_dqsbus_x8[2]),
            .dqs_out_x8_lane3                 (t2l_dqsbus_x8[3]),

            .dqs_in_x18_0                     (DQS_BUS_MODE_ENUM == "DQS_BUS_MODE_X16_X18" ? {b2t_dqsb[(tile_i * LANES_PER_TILE) + 1], b2t_dqs[(tile_i * LANES_PER_TILE) + 1]} : 2'b0),
            .dqs_in_x18_1                     (DQS_BUS_MODE_ENUM == "DQS_BUS_MODE_X16_X18" ? {b2t_dqsb[(tile_i * LANES_PER_TILE) + 3], b2t_dqs[(tile_i * LANES_PER_TILE) + 3]} : 2'b0),
            .dqs_out_x18_lane0                (t2l_dqsbus_x18[0]),
            .dqs_out_x18_lane1                (t2l_dqsbus_x18[1]),
            .dqs_out_x18_lane2                (t2l_dqsbus_x18[2]),
            .dqs_out_x18_lane3                (t2l_dqsbus_x18[3]),

            .dqs_in_x36                       (DQS_BUS_MODE_ENUM == "DQS_BUS_MODE_X32_X36" ? {b2t_dqsb[(tile_i * LANES_PER_TILE) + 1], b2t_dqs[(tile_i * LANES_PER_TILE) + 1]} : 2'b0),
            .dqs_out_x36_lane0                (t2l_dqsbus_x36[0]),
            .dqs_out_x36_lane1                (t2l_dqsbus_x36[1]),
            .dqs_out_x36_lane2                (t2l_dqsbus_x36[2]),
            .dqs_out_x36_lane3                (t2l_dqsbus_x36[3]),
            
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
            .dbc2core_wr_data_rdy0            (l2core_wr_data_rdy_ast_abphy[tile_i][0]),
            .dbc2core_wr_data_rdy1            (l2core_wr_data_rdy_ast_abphy[tile_i][1]),
            .dbc2core_wr_data_rdy2            (l2core_wr_data_rdy_ast_abphy[tile_i][2]),
            .dbc2core_wr_data_rdy3            (l2core_wr_data_rdy_ast_abphy[tile_i][3]),
                        
            .ping_pong_in                     (all_tiles_ping_pong_up_chain[tile_i]),
            .ping_pong_out                    (all_tiles_ping_pong_up_chain[tile_i + 1]),

            .mmr_in                           (`_sel_hmc_def(tile_i, core2ctl_mmr_0, core2ctl_mmr_1, 51'b0)),
            .mmr_out                          (all_tiles_ctl2core_mmr[tile_i]),

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
   
         if ( SILICON_REV=="20nm5es" ) begin
           initial begin
             @ (posedge global_reset_n_int);
             if ( global_reset_n_int !==1'b1 )
               @ (posedge global_reset_n_int);
             #100;
             if ( DIAG_VERBOSE_IOAUX!=0 ) $display("wlat=%d rtl",wlat);
             if ( DIAG_VERBOSE_IOAUX!=0 ) $display("rlat=%d rtl",rlat);
             force tile_gen[tile_i].tile_ctrl_inst.inst.genblk2.xio_tile_ctrl.xio_hmc.io_phy_sequencer_inst.avl_tile_inst.cmd_phy_rst_n=1;
             force tile_gen[tile_i].tile_ctrl_inst.inst.genblk2.xio_tile_ctrl.xio_hmc.io_phy_sequencer_inst.avl_tile_inst.cmd_ctl_rst_n=1;
             force tile_gen[tile_i].tile_ctrl_inst.inst.genblk2.xio_tile_ctrl.xio_hmc.io_phy_sequencer_inst.avl_tile_inst.cmd_rst_n=1;
             force tile_gen[tile_i].tile_ctrl_inst.inst.genblk2.xio_tile_ctrl.xio_hmc.io_phy_sequencer_inst.io_phy_manager_inst.afi_seq2core='h40;
             if ( PHY_PING_PONG_EN==1 && tile_i==SEC_AC_TILE_INDEX ) begin
               force tile_gen[tile_i].tile_ctrl_inst.inst.genblk2.xio_tile_ctrl.xio_hmc.io_phy_sequencer_inst.io_phy_manager_inst.phy_afi_wlat=wlat+2;
               force tile_gen[tile_i].tile_ctrl_inst.inst.genblk2.xio_tile_ctrl.xio_hmc.io_phy_sequencer_inst.io_phy_manager_inst.phy_afi_rlat=rlat+2;
             end
             else begin
               force tile_gen[tile_i].tile_ctrl_inst.inst.genblk2.xio_tile_ctrl.xio_hmc.io_phy_sequencer_inst.io_phy_manager_inst.phy_afi_wlat=wlat;
               force tile_gen[tile_i].tile_ctrl_inst.inst.genblk2.xio_tile_ctrl.xio_hmc.io_phy_sequencer_inst.io_phy_manager_inst.phy_afi_rlat=rlat;
             end
             #100;
             force tile_gen[tile_i].tile_ctrl_inst.inst.genblk2.xio_tile_ctrl.xio_hmc.io_phy_sequencer_inst.io_phy_manager_inst.phy_cal_success=1;
           end
         end
         else begin
           initial begin
             @ (posedge global_reset_n_int);
             if ( global_reset_n_int !==1'b1 )
               @ (posedge global_reset_n_int);
             #100;
             if ( DIAG_VERBOSE_IOAUX!=0 ) $display("wlat=%d rtl",wlat);
             if ( DIAG_VERBOSE_IOAUX!=0 ) $display("rlat=%d rtl",rlat);
             force tile_gen[tile_i].tile_ctrl_inst.inst.genblk1.xio_tile_ctrl.xio_hmc.io_phy_sequencer_inst.avl_tile_inst.cmd_phy_rst_n=1;
             force tile_gen[tile_i].tile_ctrl_inst.inst.genblk1.xio_tile_ctrl.xio_hmc.io_phy_sequencer_inst.avl_tile_inst.cmd_ctl_rst_n=1;
             force tile_gen[tile_i].tile_ctrl_inst.inst.genblk1.xio_tile_ctrl.xio_hmc.io_phy_sequencer_inst.avl_tile_inst.cmd_rst_n=1;
             force tile_gen[tile_i].tile_ctrl_inst.inst.genblk1.xio_tile_ctrl.xio_hmc.io_phy_sequencer_inst.io_phy_manager_inst.afi_seq2core='h40;
             if ( PHY_PING_PONG_EN==1 && tile_i==SEC_AC_TILE_INDEX ) begin
               force tile_gen[tile_i].tile_ctrl_inst.inst.genblk1.xio_tile_ctrl.xio_hmc.io_phy_sequencer_inst.io_phy_manager_inst.phy_afi_wlat=wlat+2;
               force tile_gen[tile_i].tile_ctrl_inst.inst.genblk1.xio_tile_ctrl.xio_hmc.io_phy_sequencer_inst.io_phy_manager_inst.phy_afi_rlat=rlat+2;
             end
             else begin
               force tile_gen[tile_i].tile_ctrl_inst.inst.genblk1.xio_tile_ctrl.xio_hmc.io_phy_sequencer_inst.io_phy_manager_inst.phy_afi_wlat=wlat;
               force tile_gen[tile_i].tile_ctrl_inst.inst.genblk1.xio_tile_ctrl.xio_hmc.io_phy_sequencer_inst.io_phy_manager_inst.phy_afi_rlat=rlat;
             end
             #100;
             force tile_gen[tile_i].tile_ctrl_inst.inst.genblk1.xio_tile_ctrl.xio_hmc.io_phy_sequencer_inst.io_phy_manager_inst.phy_cal_success=1;
           end
         end
            
         for (lane_i = 0; lane_i < LANES_PER_TILE; ++lane_i)
         begin: lane_gen

            assign ac_hmc_par[lane_i*96+tile_i*4*96+95:lane_i*96+tile_i*4*96] = `_get_ac_hmc(tile_i, lane_i);
            assign dq_data_to_mem[lane_i*96+tile_i*4*96+95:lane_i*96+tile_i*4*96]    =  
                          tile_gen[tile_i].lane_gen[lane_i].lane_inst.inst.genblk1.xio_io_12_lane.u_io_12_lane_bcm.i0.data_to_ioreg;
            assign dq_oe[lane_i*48+tile_i*4*48+47:lane_i*48+tile_i*4*48]      =  
                          tile_gen[tile_i].lane_gen[lane_i].lane_inst.inst.genblk1.xio_io_12_lane.u_io_12_lane_bcm.i0.oeb_to_ioreg;
            assign tile_gen[tile_i].lane_gen[lane_i].lane_inst.inst.genblk1.xio_io_12_lane.u_io_12_lane_bcm.i0.data_from_ioreg_abphy = 
                          dq_data_from_mem[lane_i*96+tile_i*4*96+95:lane_i*96+tile_i*4*96];
            assign tile_gen[tile_i].lane_gen[lane_i].lane_inst.inst.genblk1.xio_io_12_lane.u_io_12_lane_bcm.i0.rdata_valid_local=
                          rdata_valid_local[tile_i][lane_i];

            assign tile_gen[tile_i].lane_gen[lane_i].lane_inst.inst.genblk1.xio_io_12_lane.u_io_12_lane_bcm.i0.afi_cal_success = afi_cal_success_delay;
            

            integer i,j;
            initial begin
              add_2[tile_i][lane_i]                = 0;
              if ( PHY_PING_PONG_EN==1 ) begin
                for ( i=(PORT_MEM_DQ_WIDTH/2); i<PORT_MEM_DQ_WIDTH; i++ ) begin
                  if ( tile_i==`_abphy_get_tile(PORT_MEM_DQ_PINLOC, i) && 
                              lane_i==`_abphy_get_lane(PORT_MEM_DQ_PINLOC, i) ) begin
                    add_2[tile_i][lane_i]          = 2;
                  end
                end
              end
              
              @ (posedge global_reset_n_int);
              if ( global_reset_n_int !==1'b1 )
                @ (posedge global_reset_n_int);
              #100;
               force tile_gen[tile_i].lane_gen[lane_i].lane_inst.inst.genblk1.xio_io_12_lane.u_io_12_lane_bcm.i0.data_buffer.avl_lane_inst.cmd_phy_rst_n=1;;
               force tile_gen[tile_i].lane_gen[lane_i].lane_inst.inst.genblk1.xio_io_12_lane.u_io_12_lane_bcm.i0.data_buffer.avl_lane_inst.cmd_dbc_rst_n=1;;
               force tile_gen[tile_i].lane_gen[lane_i].lane_inst.inst.genblk1.xio_io_12_lane.u_io_12_lane_bcm.i0.data_buffer.avl_lane_inst.cmd_rst_n=1;
               force tile_gen[tile_i].lane_gen[lane_i].lane_inst.inst.genblk1.xio_io_12_lane.u_io_12_lane_bcm.i0.data_buffer.afi_rlat[5:0]=rlat+add_2[tile_i][lane_i];
               force tile_gen[tile_i].lane_gen[lane_i].lane_inst.inst.genblk1.xio_io_12_lane.u_io_12_lane_bcm.i0.data_buffer.afi_wlat[5:0]=wlat+add_2[tile_i][lane_i];
            end

            
               twentynm_io_12_lane_abphy #(
                  .silicon_rev                              (SILICON_REV),
                  .fast_interpolator_sim                    (DIAG_FAST_SIM),
                  .hps_ctrl_en                              (IS_HPS ? "true" : "false"),
                  .phy_clk_phs_freq                         (PLL_VCO_FREQ_MHZ_INT),
                  .mode_rate_in                             (`_get_lane_mode_rate_in),
                  .mode_rate_out                            (`_get_lane_mode_rate_out),
                  .pipe_latency                             (8'b00000000),               
                  .dqs_enable_delay                         (6'b000000),                 
                  .rd_valid_delay                           (7'b0000000),                
                  .phy_clk_sel                              (0),                         
                  .pin_0_initial_out                        ("initial_out_z"),
                  .pin_0_output_phase                       (13'b0000000000000),
                  .pin_0_mode_ddr                           (`_get_pin_ddr_str         (tile_i, lane_i, 0)),
                  .pin_0_oct_mode                           (`_get_pin_oct_mode_str    (tile_i, lane_i, 0)),
                  .pin_0_data_in_mode                       (`_get_pin_data_in_mode_str(tile_i, lane_i, 0)),
                  .pin_0_dqs_x4_mode                        (`_get_pin_dqs_x4_mode_0),
                  .pin_1_initial_out                        ("initial_out_z"),
                  .pin_1_output_phase                       (13'b0000000000000),
                  .pin_1_mode_ddr                           (`_get_pin_ddr_str         (tile_i, lane_i, 1)),
                  .pin_1_oct_mode                           (`_get_pin_oct_mode_str    (tile_i, lane_i, 1)),
                  .pin_1_data_in_mode                       (`_get_pin_data_in_mode_str(tile_i, lane_i, 1)),
                  .pin_1_dqs_x4_mode                        (`_get_pin_dqs_x4_mode_1),
                  .pin_2_initial_out                        ("initial_out_z"),
                  .pin_2_output_phase                       (13'b0000000000000),
                  .pin_2_mode_ddr                           (`_get_pin_ddr_str         (tile_i, lane_i, 2)),
                  .pin_2_oct_mode                           (`_get_pin_oct_mode_str    (tile_i, lane_i, 2)),
                  .pin_2_data_in_mode                       (`_get_pin_data_in_mode_str(tile_i, lane_i, 2)),
                  .pin_2_dqs_x4_mode                        (`_get_pin_dqs_x4_mode_2),
                  .pin_3_initial_out                        ("initial_out_z"),
                  .pin_3_output_phase                       (13'b0000000000000),
                  .pin_3_mode_ddr                           (`_get_pin_ddr_str         (tile_i, lane_i, 3)),
                  .pin_3_oct_mode                           (`_get_pin_oct_mode_str    (tile_i, lane_i, 3)),
                  .pin_3_data_in_mode                       (`_get_pin_data_in_mode_str(tile_i, lane_i, 3)),
                  .pin_3_dqs_x4_mode                        (`_get_pin_dqs_x4_mode_3),
                  .pin_4_initial_out                        ("initial_out_z"),
                  .pin_4_output_phase                       (13'b0000000000000),
                  .pin_4_mode_ddr                           (`_get_pin_ddr_str         (tile_i, lane_i, 4)),
                  .pin_4_oct_mode                           (`_get_pin_oct_mode_str    (tile_i, lane_i, 4)),
                  .pin_4_data_in_mode                       (`_get_pin_data_in_mode_str(tile_i, lane_i, 4)),
                  .pin_4_dqs_x4_mode                        (`_get_pin_dqs_x4_mode_4),
                  .pin_5_initial_out                        ("initial_out_z"),
                  .pin_5_output_phase                       (13'b0000000000000),
                  .pin_5_mode_ddr                           (`_get_pin_ddr_str         (tile_i, lane_i, 5)),
                  .pin_5_oct_mode                           (`_get_pin_oct_mode_str    (tile_i, lane_i, 5)),
                  .pin_5_data_in_mode                       (`_get_pin_data_in_mode_str(tile_i, lane_i, 5)),
                  .pin_5_dqs_x4_mode                        (`_get_pin_dqs_x4_mode_5),
                  .pin_6_initial_out                        ("initial_out_z"),
                  .pin_6_output_phase                       (13'b0000000000000),
                  .pin_6_mode_ddr                           (`_get_pin_ddr_str         (tile_i, lane_i, 6)),
                  .pin_6_oct_mode                           (`_get_pin_oct_mode_str    (tile_i, lane_i, 6)),
                  .pin_6_data_in_mode                       (`_get_pin_data_in_mode_str(tile_i, lane_i, 6)),
                  .pin_6_dqs_x4_mode                        (`_get_pin_dqs_x4_mode_6),
                  .pin_7_initial_out                        ("initial_out_z"),
                  .pin_7_output_phase                       (13'b0000000000000),
                  .pin_7_mode_ddr                           (`_get_pin_ddr_str         (tile_i, lane_i, 7)),
                  .pin_7_oct_mode                           (`_get_pin_oct_mode_str    (tile_i, lane_i, 7)),
                  .pin_7_data_in_mode                       (`_get_pin_data_in_mode_str(tile_i, lane_i, 7)),
                  .pin_7_dqs_x4_mode                        (`_get_pin_dqs_x4_mode_7),
                  .pin_8_initial_out                        ("initial_out_z"),
                  .pin_8_output_phase                       (13'b0000000000000),
                  .pin_8_mode_ddr                           (`_get_pin_ddr_str         (tile_i, lane_i, 8)),
                  .pin_8_oct_mode                           (`_get_pin_oct_mode_str    (tile_i, lane_i, 8)),
                  .pin_8_data_in_mode                       (`_get_pin_data_in_mode_str(tile_i, lane_i, 8)),
                  .pin_8_dqs_x4_mode                        (`_get_pin_dqs_x4_mode_8),
                  .pin_9_initial_out                        ("initial_out_z"),
                  .pin_9_output_phase                       (13'b0000000000000),
                  .pin_9_mode_ddr                           (`_get_pin_ddr_str         (tile_i, lane_i, 9)),
                  .pin_9_oct_mode                           (`_get_pin_oct_mode_str    (tile_i, lane_i, 9)),
                  .pin_9_data_in_mode                       (`_get_pin_data_in_mode_str(tile_i, lane_i, 9)),
                  .pin_9_dqs_x4_mode                        (`_get_pin_dqs_x4_mode_9),
                  .pin_10_initial_out                       ("initial_out_z"),
                  .pin_10_output_phase                      (13'b0000000000000),
                  .pin_10_mode_ddr                          (`_get_pin_ddr_str         (tile_i, lane_i, 10)),
                  .pin_10_oct_mode                          (`_get_pin_oct_mode_str    (tile_i, lane_i, 10)),
                  .pin_10_data_in_mode                      (`_get_pin_data_in_mode_str(tile_i, lane_i, 10)),
                  .pin_10_dqs_x4_mode                       (`_get_pin_dqs_x4_mode_10),
                  .pin_11_initial_out                       ("initial_out_z"),
                  .pin_11_output_phase                      (13'b0000000000000),
                  .pin_11_mode_ddr                          (`_get_pin_ddr_str         (tile_i, lane_i, 11)),
                  .pin_11_oct_mode                          (`_get_pin_oct_mode_str    (tile_i, lane_i, 11)),
                  .pin_11_data_in_mode                      (`_get_pin_data_in_mode_str(tile_i, lane_i, 11)),
                  .pin_11_dqs_x4_mode                       (`_get_pin_dqs_x4_mode_11),
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
                  .db_rwlat_mode                            ("avl_vlu"),                                         
                  .db_afi_wlat_vlu                          (6'b000000),                                         
                  .db_afi_rlat_vlu                          (6'b000000),                                         
                  .db_ptr_pipeline_depth                    (`_get_db_ptr_pipe_depth(tile_i, lane_i)),           
                  .db_seq_rd_en_full_pipeline               (`_get_db_seq_rd_en_full_pipeline(tile_i, lane_i)),  
                  .db_preamble_mode                         (PREAMBLE_MODE),
                  .db_reset_auto_release                    ("avl_release"),                         
                  .db_data_alignment_mode                   (`_get_db_data_alignment_mode),          
                  .db_db2core_registered                    ("true"),
                  .db_core_or_hmc2db_registered             ("false"),
                  .dbc_core_clk_sel                         (USE_HMC_RC_OR_DP ? 1 : 0),              
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
                  .dll_rst_en                               ("dll_rst_dis"),
                  .dll_en                                   ("dll_en"),
                  .dll_core_updnen                          ("core_updn_dis"),
                  .dll_ctlsel                               (DLL_MODE),
                  .dll_ctl_static                           (DLL_CODEWORD),
                  .dqs_lgc_dqs_b_en                         (`_get_dqs_b_en),                       
                  .dqs_lgc_dqs_a_interp_en                  ("false"),                              
                  .dqs_lgc_dqs_b_interp_en                  ("false"),                              
                  .dqs_lgc_swap_dqs_a_b                     (SWAP_DQS_A_B),                         
                  .dqs_lgc_pvt_input_delay_a                (10'b00000_00000),                      
                  .dqs_lgc_pvt_input_delay_b                (10'b00000_00000),                      
                  .dqs_lgc_enable_toggler                   (`_get_preamble_track_dqs_enable_mode), 
                  .dqs_lgc_phase_shift_b                    (13'b00000_0000_0000),                  
                  .dqs_lgc_phase_shift_a                    (13'b00000_0000_0000),                  
                  .dqs_lgc_pack_mode                        (DQS_PACK_MODE),
                  .dqs_lgc_pst_preamble_mode                (`_get_pst_preamble_mode),
                  .dqs_lgc_pst_en_shrink                    (`_get_pst_en_shrink),
                  .dqs_lgc_broadcast_enable                 ("disable_broadcast"),
                  .dqs_lgc_burst_length                     (`_get_dqs_lgc_burst_length),
                  .dqs_lgc_ddr4_search                      (`_get_ddr4_search),
                  .dqs_lgc_count_threshold                  (7'b0011000),
                  .pingpong_primary                         (`_sel_hmc_lane(tile_i, lane_i, "true", "false")),
                  .pingpong_secondary                       (`_sel_hmc_lane(tile_i, lane_i, "false", "true"))
                  
               ) lane_inst (
               
                  .pll_locked                               (pll_locked),
                  .dll_ref_clk                              (all_tiles_dll_clk_out[tile_i][lane_i]),
                  .core_dll                                 (),    
                  .dll_core                                 (),    
                  .ioereg_locked                            (),

                  .phy_clk                                  (all_tiles_t2l_phy_clk[tile_i][lane_i]),                  
                  .phy_clk_phs                              (all_tiles_t2l_phy_clk_phs[tile_i][lane_i]),
                  
                  .sync_data_bot_in                         (pa_sync_data_up_chain[`_get_chain_index_for_lane(tile_i, lane_i)]),
                  .sync_data_top_out                        (pa_sync_data_up_chain[`_get_chain_index_for_lane(tile_i, lane_i) + 1]),
                  .sync_data_top_in                         (pa_sync_data_dn_chain[`_get_chain_index_for_lane(tile_i, lane_i) + 1]),
                  .sync_data_bot_out                        (pa_sync_data_dn_chain[`_get_chain_index_for_lane(tile_i, lane_i)]),
                  .sync_clk_bot_in                          (pa_sync_clk_up_chain [`_get_chain_index_for_lane(tile_i, lane_i)]),
                  .sync_clk_top_out                         (pa_sync_clk_up_chain [`_get_chain_index_for_lane(tile_i, lane_i) + 1]),
                  .sync_clk_top_in                          (pa_sync_clk_dn_chain [`_get_chain_index_for_lane(tile_i, lane_i) + 1]),
                  .sync_clk_bot_out                         (pa_sync_clk_dn_chain [`_get_chain_index_for_lane(tile_i, lane_i)]),

                  .dqs_in                                   (`_get_dqsin(tile_i, lane_i)),

                  .oct_enable                               (l2b_dtc_abphy [tile_i * PINS_PER_LANE * LANES_PER_TILE + lane_i * PINS_PER_LANE +: PINS_PER_LANE]),
                  .data_oe                                  (l2b_oe_abphy  [tile_i * PINS_PER_LANE * LANES_PER_TILE + lane_i * PINS_PER_LANE +: PINS_PER_LANE]),
                  .data_out                                 (l2b_data_abphy[tile_i * PINS_PER_LANE * LANES_PER_TILE + lane_i * PINS_PER_LANE +: PINS_PER_LANE]),
                  .data_in                                  (b2l_data[tile_i * PINS_PER_LANE * LANES_PER_TILE + lane_i * PINS_PER_LANE +: PINS_PER_LANE]),

                  .data_from_core                           (core2l_data[tile_i][lane_i]),
                  .data_to_core                             (l2core_data_abphy[tile_i][lane_i]),
                  .oe_from_core                             (~core2l_oe[tile_i][lane_i]),  
                  .rdata_en_full_core                       ((`_get_lane_usage(tile_i, lane_i) == LANE_USAGE_RDATA || `_get_lane_usage(tile_i, lane_i) == LANE_USAGE_WRDATA) ? core2l_rdata_en_full[tile_i][lane_i] : 4'b0),
                  .mrnk_read_core                           ((`_get_lane_usage(tile_i, lane_i) == LANE_USAGE_RDATA || `_get_lane_usage(tile_i, lane_i) == LANE_USAGE_WRDATA) ? core2l_mrnk_read[tile_i][lane_i]     : 16'b0),
                  .mrnk_write_core                          ((`_get_lane_usage(tile_i, lane_i) == LANE_USAGE_WDATA || `_get_lane_usage(tile_i, lane_i) == LANE_USAGE_WRDATA) ? core2l_mrnk_write[tile_i][lane_i]    : 16'b0),
                  .rdata_valid_core                         (l2core_rdata_valid_abphy[tile_i][lane_i]),
                  .afi_wlat_core                            (l2core_afi_wlat_abphy[tile_i][lane_i]),
                  .afi_rlat_core                            (l2core_afi_rlat_abphy[tile_i][lane_i]),

                  .dbc2core_rd_data_vld0                    (l2core_rd_data_vld_avl0_abphy[tile_i][lane_i]),
                  .dbc2core_rd_data_vld1                    (),
                  .core2dbc_wr_data_vld0                    (`_get_core2dbc_wr_data_vld(tile_i, lane_i)),
                  .core2dbc_wr_data_vld1                    (1'b0),
                  .dbc2core_wr_data_rdy                     (l2core_wr_data_rdy_ast_abphy [tile_i][lane_i]),
                  .core2dbc_rd_data_rdy                     (`_get_core2dbc_rd_data_rdy(tile_i, lane_i)),
                  .dbc2core_wb_pointer                      (l2core_wb_pointer_for_ecc_abphy[tile_i][lane_i]),
                  .core2dbc_wr_ecc_info                     (`_get_core2dbc_wr_ecc_info(tile_i, lane_i)),
                  .dbc2core_rd_type                         (),
                  
                  .reset_n                                  (global_reset_n_int),
                  .cal_avl_in                               (cal_bus_avl_up_chain          [`_get_chain_index_for_lane(tile_i, lane_i)]),
                  .cal_avl_out                              (cal_bus_avl_up_chain          [`_get_chain_index_for_lane(tile_i, lane_i) + 1]),
                  .cal_avl_readdata_in                      (cal_bus_avl_read_data_dn_chain[`_get_chain_index_for_lane(tile_i, lane_i) + 1]),
                  .cal_avl_readdata_out                     (cal_bus_avl_read_data_dn_chain[`_get_chain_index_for_lane(tile_i, lane_i)]),
                  
                  .ac_hmc                                   (`_get_ac_hmc(tile_i, lane_i)),
                  .ctl2dbc0                                 (all_tiles_ctl2dbc0_dn_chain[tile_i]),
                  .ctl2dbc1                                 (all_tiles_ctl2dbc1_up_chain[tile_i + 1]),
                  .dbc2ctl                                  (l2t_dbc2ctl[lane_i]),
                  .cfg_dbc                                  (t2l_cfg_dbc[lane_i]),
                  
                  .broadcast_in_bot                         (broadcast_up_chain[`_get_broadcast_chain_index(tile_i, lane_i)]),
                  .broadcast_out_top                        (broadcast_up_chain[`_get_broadcast_chain_index(tile_i, lane_i) + 1]),              
                  .broadcast_in_top                         (broadcast_dn_chain[`_get_broadcast_chain_index(tile_i, lane_i) + 1]),
                  .broadcast_out_bot                        (broadcast_dn_chain[`_get_broadcast_chain_index(tile_i, lane_i)]),
                  
                  .dft_phy_clk                              ()
               );       
         end
      end
   endgenerate
   
   
   mem_array_abphy #(
     .DIAG_VERBOSE_IOAUX                        (DIAG_VERBOSE_IOAUX),
     .NUM_OF_RTL_TILES                          (NUM_OF_RTL_TILES),
     .LANES_PER_TILE                            (LANES_PER_TILE),
     .USER_CLK_RATIO                            (USER_CLK_RATIO),
     .PINS_RATE                                 (PINS_RATE),
     .MEM_DATA_MASK_EN                          (MEM_DATA_MASK_EN),
     .PHY_HMC_CLK_RATIO                         (PHY_HMC_CLK_RATIO),
     .NUM_OF_HMC_PORTS                          (NUM_OF_HMC_PORTS),
     .PORT_MEM_A_PINLOC                         (PORT_MEM_A_PINLOC),
     .PORT_MEM_BA_PINLOC                        (PORT_MEM_BA_PINLOC),
     .PORT_MEM_BG_PINLOC                        (PORT_MEM_BG_PINLOC),
     .PORT_MEM_CS_N_PINLOC                      (PORT_MEM_CS_N_PINLOC),
     .PORT_MEM_ACT_N_PINLOC                     (PORT_MEM_ACT_N_PINLOC),
     .PORT_MEM_DQ_PINLOC                        (PORT_MEM_DQ_PINLOC),
     .PORT_MEM_DBI_N_PINLOC                     (PORT_MEM_DBI_N_PINLOC),
     .PORT_MEM_RAS_N_PINLOC                     (PORT_MEM_RAS_N_PINLOC),
     .PORT_MEM_CAS_N_PINLOC                     (PORT_MEM_CAS_N_PINLOC),
     .PORT_MEM_WE_N_PINLOC                      (PORT_MEM_WE_N_PINLOC),
     .PORT_MEM_DQ_WIDTH                         (PORT_MEM_DQ_WIDTH),
     .PORT_MEM_DM_WIDTH                         (PORT_MEM_DM_WIDTH),
     .PORT_MEM_DM_PINLOC                        (PORT_MEM_DM_PINLOC),
     .PORT_MEM_REF_N_PINLOC                     (PORT_MEM_REF_N_PINLOC),
     .PORT_MEM_WPS_N_PINLOC                     (PORT_MEM_WPS_N_PINLOC),
     .PORT_MEM_RPS_N_PINLOC                     (PORT_MEM_RPS_N_PINLOC),
     .PORT_MEM_BWS_N_PINLOC                     (PORT_MEM_BWS_N_PINLOC),
     .PORT_MEM_DQA_PINLOC                       (PORT_MEM_DQA_PINLOC),
     .PORT_MEM_DQB_PINLOC                       (PORT_MEM_DQB_PINLOC),
     .PORT_MEM_Q_PINLOC                         (PORT_MEM_Q_PINLOC),
     .PORT_MEM_D_PINLOC                         (PORT_MEM_D_PINLOC),
     .PORT_MEM_RWA_N_PINLOC                     (PORT_MEM_RWA_N_PINLOC),
     .PORT_MEM_RWB_N_PINLOC                     (PORT_MEM_RWB_N_PINLOC),
     .PORT_MEM_QKA_PINLOC                       (PORT_MEM_QKA_PINLOC),
     .PORT_MEM_QKB_PINLOC                       (PORT_MEM_QKB_PINLOC),
     .PORT_MEM_LDA_N_PINLOC                     (PORT_MEM_LDA_N_PINLOC),
     .PORT_MEM_LDB_N_PINLOC                     (PORT_MEM_LDB_N_PINLOC),
     .PORT_MEM_CK_PINLOC                        (PORT_MEM_CK_PINLOC),
     .PORT_MEM_DINVA_PINLOC                     (PORT_MEM_DINVA_PINLOC),
     .PORT_MEM_DINVB_PINLOC                     (PORT_MEM_DINVB_PINLOC),
     .PORT_MEM_AINV_PINLOC                      (PORT_MEM_AINV_PINLOC),
     .PORT_MEM_A_WIDTH                          (PORT_MEM_A_WIDTH),
     .PORT_MEM_BA_WIDTH                         (PORT_MEM_BA_WIDTH),
     .PORT_MEM_BG_WIDTH                         (PORT_MEM_BG_WIDTH),
     .PORT_MEM_CS_N_WIDTH                       (PORT_MEM_CS_N_WIDTH),
     .PORT_MEM_ACT_N_WIDTH                      (PORT_MEM_ACT_N_WIDTH),
     .PORT_MEM_DBI_N_WIDTH                      (PORT_MEM_DBI_N_WIDTH),
     .PORT_MEM_RAS_N_WIDTH                      (PORT_MEM_RAS_N_WIDTH),
     .PORT_MEM_CAS_N_WIDTH                      (PORT_MEM_CAS_N_WIDTH),
     .PORT_MEM_WE_N_WIDTH                       (PORT_MEM_WE_N_WIDTH),
     .PORT_MEM_REF_N_WIDTH                      (PORT_MEM_REF_N_WIDTH),
     .PORT_MEM_WPS_N_WIDTH                      (PORT_MEM_WPS_N_WIDTH),
     .PORT_MEM_RPS_N_WIDTH                      (PORT_MEM_RPS_N_WIDTH),
     .PORT_MEM_BWS_N_WIDTH                      (PORT_MEM_BWS_N_WIDTH),
     .PORT_MEM_DQA_WIDTH                        (PORT_MEM_DQA_WIDTH),
     .PORT_MEM_DQB_WIDTH                        (PORT_MEM_DQB_WIDTH),
     .PORT_MEM_Q_WIDTH                          (PORT_MEM_Q_WIDTH),
     .PORT_MEM_D_WIDTH                          (PORT_MEM_D_WIDTH),
     .PORT_MEM_RWA_N_WIDTH                      (PORT_MEM_RWA_N_WIDTH),
     .PORT_MEM_RWB_N_WIDTH                      (PORT_MEM_RWB_N_WIDTH),
     .PORT_MEM_QKA_WIDTH                        (PORT_MEM_QKA_WIDTH),
     .PORT_MEM_QKB_WIDTH                        (PORT_MEM_QKB_WIDTH),
     .PORT_MEM_LDA_N_WIDTH                      (PORT_MEM_LDA_N_WIDTH),
     .PORT_MEM_LDB_N_WIDTH                      (PORT_MEM_LDB_N_WIDTH),
     .PORT_MEM_CK_WIDTH                         (PORT_MEM_CK_WIDTH),
     .PORT_MEM_DINVA_WIDTH                      (PORT_MEM_DINVA_WIDTH),
     .PORT_MEM_DINVB_WIDTH                      (PORT_MEM_DINVB_WIDTH),
     .PORT_MEM_AINV_WIDTH                       (PORT_MEM_AINV_WIDTH),
     .PHY_PING_PONG_EN                          (PHY_PING_PONG_EN),
     .PROTOCOL_ENUM                             (PROTOCOL_ENUM),
     .DBI_WR_ENABLE                             (DBI_WR_ENABLE),
     .DBI_RD_ENABLE                             (DBI_RD_ENABLE),
     .PRI_HMC_CFG_MEM_IF_COLADDR_WIDTH          (PRI_HMC_CFG_MEM_IF_COLADDR_WIDTH), 
     .PRI_HMC_CFG_MEM_IF_ROWADDR_WIDTH          (PRI_HMC_CFG_MEM_IF_ROWADDR_WIDTH), 
     .SEC_HMC_CFG_MEM_IF_COLADDR_WIDTH          (SEC_HMC_CFG_MEM_IF_COLADDR_WIDTH), 
     .SEC_HMC_CFG_MEM_IF_ROWADDR_WIDTH          (SEC_HMC_CFG_MEM_IF_ROWADDR_WIDTH),
     .MEM_BURST_LENGTH                          (MEM_BURST_LENGTH),
     .ABPHY_WRITE_PROTOCOL                      (ABPHY_WRITE_PROTOCOL)
   ) mem_array_abphy_inst (
     .phy_clk                                   (all_tiles_t2l_phy_clk[0][0][0]),
     .reset_n                                   (global_reset_n_int),
     .select_ac_hmc                             (),
     .afi_rlat                                  (l2core_afi_rlat_abphy),
     .afi_wlat                                  (l2core_afi_wlat_abphy),
     .ac_hmc                                    (ac_hmc_par),
     .dq_data_to_mem                            (dq_data_to_mem),
     .dq_oe                                     (dq_oe),
     .rdata_valid_local                         (rdata_valid_local),
     .afi_cal_success                           (afi_cal_success),
     .dq_data_from_mem                          (dq_data_from_mem),
     .runAbstractPhySim                         (runAbstractPhySim)
     
   );
   
   assign cal_bus_clk_force=0;
   assign cal_bus_avl_write_force=0;
   assign cal_bus_avl_address_force=0;
   assign cal_bus_avl_write_data_force=0;
   
   
   initial begin
   end
   // synthesis translate_on
   

endmodule

