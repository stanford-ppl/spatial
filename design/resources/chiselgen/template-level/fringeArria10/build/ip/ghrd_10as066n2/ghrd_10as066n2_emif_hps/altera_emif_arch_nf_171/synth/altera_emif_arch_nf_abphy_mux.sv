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
// abstract phy mux
//
///////////////////////////////////////////////////////////////////////////////
module altera_emif_arch_nf_abphy_mux #(
   parameter DIAG_USE_ABSTRACT_PHY                   = 0,
   parameter LANES_PER_TILE                          = 1,
   parameter NUM_OF_RTL_TILES                        = 1,
   parameter PINS_PER_LANE                           = 1,
   parameter PINS_IN_RTL_TILES                       = 1,
   parameter PORT_DFT_NF_PA_DPRIO_READDATA_WIDTH     = 1,
   parameter PORT_DFT_NF_PA_DPRIO_REG_ADDR_WIDTH     = 1,
   parameter PORT_DFT_NF_PA_DPRIO_WRITEDATA_WIDTH    = 1,
   parameter LANES_IN_RTL_TILES                      = 1
) (
   output logic                                                                                  phy_reset_n,         // Async reset signal from reset circuitry in the tile
   output logic                                                                                  phy_fb_clk_to_pll,   // PHY feedback clock (to PLL)
   output logic [1:0]                                                                            core_clks_from_cpa_pri,   // Core clock signals from the CPA of primary interface
   output logic [1:0]                                                                            core_clks_locked_cpa_pri, // Core clock locked signals from the CPA of primary interface
   output logic [1:0]                                                                            core_clks_from_cpa_sec,   // Core clock signals from the CPA of secondary interface (ping-pong only)
   output logic [1:0]                                                                            core_clks_locked_cpa_sec, // Core clock locked signals from the CPA of secondary interface (ping-pong only)
   output logic                                                                                  ctl2core_avl_cmd_ready_0,
   output logic                                                                                  ctl2core_avl_cmd_ready_1,
   output logic [12:0]                                                                           ctl2core_avl_rdata_id_0,
   output logic [12:0]                                                                           ctl2core_avl_rdata_id_1,
   output logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0]                                       l2core_rd_data_vld_avl0,
   output logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0]                                       l2core_wr_data_rdy_ast,
   output logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][11:0]                                 l2core_wb_pointer_for_ecc,
   output logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][PINS_PER_LANE * 8 - 1:0]              l2core_data,
   output logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][3:0]                                  l2core_rdata_valid,
   output logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][5:0]                                  l2core_afi_rlat,
   output logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][5:0]                                  l2core_afi_wlat,
   output logic [25:0]                                                                           t2c_afi,   
   output logic [13:0]                                                                           ctl2core_sideband_0,
   output logic [13:0]                                                                           ctl2core_sideband_1,
   output logic [33:0]                                                                           ctl2core_mmr_0,
   output logic [33:0]                                                                           ctl2core_mmr_1,
   output logic [PINS_IN_RTL_TILES-1:0]                                                          l2b_data,         // lane-to-buffer data
   output logic [PINS_IN_RTL_TILES-1:0]                                                          l2b_oe,           // lane-to-buffer output-enable
   output logic [PINS_IN_RTL_TILES-1:0]                                                          l2b_dtc,          // lane-to-buffer dynamic-termination-control
   output logic                                                                                  pa_dprio_block_select,
   output logic [PORT_DFT_NF_PA_DPRIO_READDATA_WIDTH-1:0]                                        pa_dprio_readdata,

   output logic                                                                                  global_reset_n_int_iotile_in,
   output logic                                                                                  pll_locked_iotile_in,
   output logic                                                                                  pll_dll_clk_iotile_in,
   output logic [7:0]                                                                            phy_clk_phs_iotile_in,
   output logic [1:0]                                                                            phy_clk_iotile_in,
   output logic                                                                                  phy_fb_clk_to_tile_iotile_in,
   output logic [1:0]                                                                            core_clks_fb_to_cpa_pri_iotile_in,
   output logic [1:0]                                                                            core_clks_fb_to_cpa_sec_iotile_in,
   output logic [59:0]                                                                           core2ctl_avl_0_iotile_in,
   output logic [59:0]                                                                           core2ctl_avl_1_iotile_in,
   output logic                                                                                  core2ctl_avl_rd_data_ready_0_iotile_in,
   output logic                                                                                  core2ctl_avl_rd_data_ready_1_iotile_in,
   output logic                                                                                  core2l_wr_data_vld_ast_0_iotile_in,
   output logic                                                                                  core2l_wr_data_vld_ast_1_iotile_in,
   output logic                                                                                  core2l_rd_data_rdy_ast_0_iotile_in,
   output logic                                                                                  core2l_rd_data_rdy_ast_1_iotile_in,
   output logic [12:0]                                                                           core2l_wr_ecc_info_0_iotile_in,
   output logic [12:0]                                                                           core2l_wr_ecc_info_1_iotile_in,
   output logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][PINS_PER_LANE * 8 - 1:0]              core2l_data_iotile_in,
   output logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][PINS_PER_LANE * 4 - 1:0]              core2l_oe_iotile_in,
   output logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][3:0]                                  core2l_rdata_en_full_iotile_in,
   output logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][15:0]                                 core2l_mrnk_read_iotile_in,
   output logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][15:0]                                 core2l_mrnk_write_iotile_in,
   output logic [16:0]                                                                           c2t_afi_iotile_in,
   output logic [41:0]                                                                           core2ctl_sideband_0_iotile_in,
   output logic [41:0]                                                                           core2ctl_sideband_1_iotile_in,
   output logic [50:0]                                                                           core2ctl_mmr_0_iotile_in,
   output logic [50:0]                                                                           core2ctl_mmr_1_iotile_in,
   output logic [PINS_IN_RTL_TILES-1:0]                                                          b2l_data_iotile_in,
   output logic [LANES_IN_RTL_TILES-1:0]                                                         b2t_dqs_iotile_in,
   output logic [LANES_IN_RTL_TILES-1:0]                                                         b2t_dqsb_iotile_in,
   output logic                                                                                  cal_bus_clk_iotile_in,
   output logic                                                                                  cal_bus_avl_read_iotile_in,
   output logic                                                                                  cal_bus_avl_write_iotile_in,
   output logic [19:0]                                                                           cal_bus_avl_address_iotile_in,
   output logic [31:0]                                                                           cal_bus_avl_write_data_iotile_in,
   output logic                                                                                  pa_dprio_clk_iotile_in,
   output logic                                                                                  pa_dprio_read_iotile_in,
   output logic [PORT_DFT_NF_PA_DPRIO_REG_ADDR_WIDTH-1:0]                                        pa_dprio_reg_addr_iotile_in,
   output logic                                                                                  pa_dprio_rst_n_iotile_in,
   output logic                                                                                  pa_dprio_write_iotile_in,
   output logic [PORT_DFT_NF_PA_DPRIO_WRITEDATA_WIDTH-1:0]                                       pa_dprio_writedata_iotile_in,

   input  logic                                                                                  phy_reset_n_abphy,         
   input  logic                                                                                  phy_fb_clk_to_pll_abphy,   
   input  logic [1:0]                                                                            core_clks_from_cpa_pri_abphy,   // Core clock signals from the CPA of primary interface
   input  logic [1:0]                                                                            core_clks_locked_cpa_pri_abphy, // Core clock locked signals from the CPA of primary interface
   input  logic [1:0]                                                                            core_clks_from_cpa_sec_abphy,   // Core clock signals from the CPA of secondary interface (ping-pong only)
   input  logic [1:0]                                                                            core_clks_locked_cpa_sec_abphy, // Core clock locked signals from the CPA of secondary interface (ping-pong only)
   input  logic                                                                                  ctl2core_avl_cmd_ready_0_abphy,
   input  logic                                                                                  ctl2core_avl_cmd_ready_1_abphy,
   input  logic [12:0]                                                                           ctl2core_avl_rdata_id_0_abphy,
   input  logic [12:0]                                                                           ctl2core_avl_rdata_id_1_abphy,
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0]                                       l2core_rd_data_vld_avl0_abphy,
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0]                                       l2core_wr_data_rdy_ast_abphy,
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][11:0]                                 l2core_wb_pointer_for_ecc_abphy,
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][PINS_PER_LANE * 8 - 1:0]              l2core_data_abphy,
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][3:0]                                  l2core_rdata_valid_abphy,
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][5:0]                                  l2core_afi_rlat_abphy,
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][5:0]                                  l2core_afi_wlat_abphy,
   input  logic [25:0]                                                                           t2c_afi_abphy,   
   input  logic [13:0]                                                                           ctl2core_sideband_0_abphy,
   input  logic [13:0]                                                                           ctl2core_sideband_1_abphy,
   input  logic [33:0]                                                                           ctl2core_mmr_0_abphy,
   input  logic [33:0]                                                                           ctl2core_mmr_1_abphy,
   input  logic [PINS_IN_RTL_TILES-1:0]                                                          l2b_data_abphy,         
   input  logic [PINS_IN_RTL_TILES-1:0]                                                          l2b_oe_abphy,           
   input  logic [PINS_IN_RTL_TILES-1:0]                                                          l2b_dtc_abphy,          
   input  logic                                                                                  pa_dprio_block_select_abphy,
   input  logic [PORT_DFT_NF_PA_DPRIO_READDATA_WIDTH-1:0]                                        pa_dprio_readdata_abphy,

   input  logic                                                                                  phy_reset_n_nonabphy,         // Async reset signal from reset circuitry in the tile
   input  logic                                                                                  phy_fb_clk_to_pll_nonabphy,   // PHY feedback clock (to PLL)
   input  logic [1:0]                                                                            core_clks_from_cpa_pri_nonabphy,   // Core clock signals from the CPA of primary interface
   input  logic [1:0]                                                                            core_clks_locked_cpa_pri_nonabphy, // Core clock locked signals from the CPA of primary interface
   input  logic [1:0]                                                                            core_clks_from_cpa_sec_nonabphy,   // Core clock signals from the CPA of secondary interface (ping-pong only)
   input  logic [1:0]                                                                            core_clks_locked_cpa_sec_nonabphy, // Core clock locked signals from the CPA of secondary interface (ping-pong only)
   input  logic                                                                                  ctl2core_avl_cmd_ready_0_nonabphy,
   input  logic                                                                                  ctl2core_avl_cmd_ready_1_nonabphy,
   input  logic [12:0]                                                                           ctl2core_avl_rdata_id_0_nonabphy,
   input  logic [12:0]                                                                           ctl2core_avl_rdata_id_1_nonabphy,
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0]                                       l2core_rd_data_vld_avl0_nonabphy,
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0]                                       l2core_wr_data_rdy_ast_nonabphy,
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][11:0]                                 l2core_wb_pointer_for_ecc_nonabphy,
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][PINS_PER_LANE * 8 - 1:0]              l2core_data_nonabphy,
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][3:0]                                  l2core_rdata_valid_nonabphy,
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][5:0]                                  l2core_afi_rlat_nonabphy,
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][5:0]                                  l2core_afi_wlat_nonabphy,
   input  logic [25:0]                                                                           t2c_afi_nonabphy,   
   input  logic [13:0]                                                                           ctl2core_sideband_0_nonabphy,
   input  logic [13:0]                                                                           ctl2core_sideband_1_nonabphy,
   input  logic [33:0]                                                                           ctl2core_mmr_0_nonabphy,
   input  logic [33:0]                                                                           ctl2core_mmr_1_nonabphy,
   input  logic [PINS_IN_RTL_TILES-1:0]                                                          l2b_data_nonabphy,         // lane-to-buffer data
   input  logic [PINS_IN_RTL_TILES-1:0]                                                          l2b_oe_nonabphy,           // lane-to-buffer output-enable
   input  logic [PINS_IN_RTL_TILES-1:0]                                                          l2b_dtc_nonabphy,          // lane-to-buffer dynamic-termination-control
   input  logic                                                                                  pa_dprio_block_select_nonabphy,
   input  logic [PORT_DFT_NF_PA_DPRIO_READDATA_WIDTH-1:0]                                        pa_dprio_readdata_nonabphy,

   input  logic                                                                                  global_reset_n_int,                
   input  logic                                                                                  pll_locked,                        
   input  logic                                                                                  pll_dll_clk,                       
   input  logic [7:0]                                                                            phy_clk_phs,                       
   input  logic [1:0]                                                                            phy_clk,                           
   input  logic                                                                                  phy_fb_clk_to_tile,                
   input  logic [1:0]                                                                            core_clks_fb_to_cpa_pri,           
   input  logic [1:0]                                                                            core_clks_fb_to_cpa_sec,           
   input  logic [59:0]                                                                           core2ctl_avl_0,
   input  logic [59:0]                                                                           core2ctl_avl_1,
   input  logic                                                                                  core2ctl_avl_rd_data_ready_0,
   input  logic                                                                                  core2ctl_avl_rd_data_ready_1,
   input  logic                                                                                  core2l_wr_data_vld_ast_0,
   input  logic                                                                                  core2l_wr_data_vld_ast_1,
   input  logic                                                                                  core2l_rd_data_rdy_ast_0,
   input  logic                                                                                  core2l_rd_data_rdy_ast_1,
   input  logic [12:0]                                                                           core2l_wr_ecc_info_0,
   input  logic [12:0]                                                                           core2l_wr_ecc_info_1,
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][PINS_PER_LANE * 8 - 1:0]              core2l_data,
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][PINS_PER_LANE * 4 - 1:0]              core2l_oe,
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][3:0]                                  core2l_rdata_en_full,
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][15:0]                                 core2l_mrnk_read,
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][15:0]                                 core2l_mrnk_write,  
   input  [16:0]                                                                                 c2t_afi,
   input  logic [41:0]                                                                           core2ctl_sideband_0,
   input  logic [41:0]                                                                           core2ctl_sideband_1,
   input  logic [50:0]                                                                           core2ctl_mmr_0,
   input  logic [50:0]                                                                           core2ctl_mmr_1,
   input  logic [PINS_IN_RTL_TILES-1:0]                                                          b2l_data,           
   input  logic [LANES_IN_RTL_TILES-1:0]                                                         b2t_dqs,            
   input  logic [LANES_IN_RTL_TILES-1:0]                                                         b2t_dqsb,           
   input  logic                                                                                  cal_bus_clk,
   input  logic                                                                                  cal_bus_avl_read,
   input  logic                                                                                  cal_bus_avl_write,
   input  logic [19:0]                                                                           cal_bus_avl_address,
   input  logic [31:0]                                                                           cal_bus_avl_write_data,
   input  logic                                                                                  pa_dprio_clk,
   input  logic                                                                                  pa_dprio_read,
   input  logic [PORT_DFT_NF_PA_DPRIO_REG_ADDR_WIDTH-1:0]                                        pa_dprio_reg_addr,
   input  logic                                                                                  pa_dprio_rst_n,
   input  logic                                                                                  pa_dprio_write,
   input  logic [PORT_DFT_NF_PA_DPRIO_WRITEDATA_WIDTH-1:0]                                       pa_dprio_writedata,

   input  logic                                                                                  runAbstractPhySim
);
   timeunit 1ns;
   timeprecision 1ps;
   
   generate
     if (DIAG_USE_ABSTRACT_PHY == 0) 
     begin : nonabphy_connections

           assign phy_reset_n                                        = phy_reset_n_nonabphy;
           assign ctl2core_avl_rdata_id_1                            = ctl2core_avl_rdata_id_1_nonabphy;
           assign ctl2core_avl_rdata_id_0                            = ctl2core_avl_rdata_id_0_nonabphy;
           assign ctl2core_mmr_0                                     = ctl2core_mmr_0_nonabphy;
           assign l2core_afi_wlat                                    = l2core_afi_wlat_nonabphy;
           assign l2core_data                                        = l2core_data_nonabphy;
           assign l2b_dtc                                            = l2b_dtc_nonabphy;
           assign ctl2core_mmr_1                                     = ctl2core_mmr_1_nonabphy;
           assign l2b_data                                           = l2b_data_nonabphy;
           assign l2core_rd_data_vld_avl0                            = l2core_rd_data_vld_avl0_nonabphy;
           assign ctl2core_avl_cmd_ready_0                           = ctl2core_avl_cmd_ready_0_nonabphy;
           assign phy_fb_clk_to_pll                                  = phy_fb_clk_to_pll_nonabphy;
           assign l2b_oe                                             = l2b_oe_nonabphy;
           assign ctl2core_sideband_0                                = ctl2core_sideband_0_nonabphy;
           assign l2core_wb_pointer_for_ecc                          = l2core_wb_pointer_for_ecc_nonabphy;
           assign t2c_afi                                            = t2c_afi_nonabphy;
           assign pa_dprio_block_select                              = pa_dprio_block_select_nonabphy;
           assign ctl2core_sideband_1                                = ctl2core_sideband_1_nonabphy;
           assign core_clks_locked_cpa_pri                           = core_clks_locked_cpa_pri_nonabphy;
           assign core_clks_locked_cpa_sec                           = core_clks_locked_cpa_sec_nonabphy;
           assign core_clks_from_cpa_pri                             = core_clks_from_cpa_pri_nonabphy;
           assign core_clks_from_cpa_sec                             = core_clks_from_cpa_sec_nonabphy;
           assign l2core_rdata_valid                                 = l2core_rdata_valid_nonabphy;
           assign ctl2core_avl_cmd_ready_1                           = ctl2core_avl_cmd_ready_1_nonabphy;
           assign l2core_afi_rlat                                    = l2core_afi_rlat_nonabphy;
           assign l2core_wr_data_rdy_ast                             = l2core_wr_data_rdy_ast_nonabphy;
           assign pa_dprio_readdata                                  = pa_dprio_readdata_nonabphy;

           assign global_reset_n_int_iotile_in            = global_reset_n_int;
           assign pll_locked_iotile_in                    = pll_locked;
           assign pll_dll_clk_iotile_in                   = pll_dll_clk;
           assign phy_clk_phs_iotile_in                   = phy_clk_phs;
           assign phy_clk_iotile_in                       = phy_clk;
           assign phy_fb_clk_to_tile_iotile_in            = phy_fb_clk_to_tile;
           assign core_clks_fb_to_cpa_pri_iotile_in       = core_clks_fb_to_cpa_pri;
           assign core_clks_fb_to_cpa_sec_iotile_in       = core_clks_fb_to_cpa_sec;
           assign core2ctl_avl_0_iotile_in                = core2ctl_avl_0;
           assign core2ctl_avl_1_iotile_in                = core2ctl_avl_1;
           assign core2ctl_avl_rd_data_ready_0_iotile_in  = core2ctl_avl_rd_data_ready_0;
           assign core2ctl_avl_rd_data_ready_1_iotile_in  = core2ctl_avl_rd_data_ready_1;
           assign core2l_wr_data_vld_ast_0_iotile_in      = core2l_wr_data_vld_ast_0;
           assign core2l_wr_data_vld_ast_1_iotile_in      = core2l_wr_data_vld_ast_1;
           assign core2l_rd_data_rdy_ast_0_iotile_in      = core2l_rd_data_rdy_ast_0;
           assign core2l_rd_data_rdy_ast_1_iotile_in      = core2l_rd_data_rdy_ast_1;
           assign core2l_wr_ecc_info_0_iotile_in          = core2l_wr_ecc_info_0;
           assign core2l_wr_ecc_info_1_iotile_in          = core2l_wr_ecc_info_1;
           assign core2l_data_iotile_in                   = core2l_data;
           assign core2l_oe_iotile_in                     = core2l_oe;
           assign core2l_rdata_en_full_iotile_in          = core2l_rdata_en_full;
           assign core2l_mrnk_read_iotile_in              = core2l_mrnk_read;
           assign core2l_mrnk_write_iotile_in             = core2l_mrnk_write;
           assign c2t_afi_iotile_in                       = c2t_afi;
           assign core2ctl_sideband_0_iotile_in           = core2ctl_sideband_0;
           assign core2ctl_sideband_1_iotile_in           = core2ctl_sideband_1;
           assign core2ctl_mmr_0_iotile_in                = core2ctl_mmr_0;
           assign core2ctl_mmr_1_iotile_in                = core2ctl_mmr_1;
           assign b2l_data_iotile_in                      = b2l_data;
           assign b2t_dqs_iotile_in                       = b2t_dqs;
           assign b2t_dqsb_iotile_in                      = b2t_dqsb;
           assign cal_bus_clk_iotile_in                   = cal_bus_clk;
           assign cal_bus_avl_read_iotile_in              = cal_bus_avl_read;
           assign cal_bus_avl_write_iotile_in             = cal_bus_avl_write;
           assign cal_bus_avl_address_iotile_in           = cal_bus_avl_address;
           assign cal_bus_avl_write_data_iotile_in        = cal_bus_avl_write_data;
           assign pa_dprio_clk_iotile_in                  = pa_dprio_clk;
           assign pa_dprio_read_iotile_in                 = pa_dprio_read;
           assign pa_dprio_reg_addr_iotile_in             = pa_dprio_reg_addr;
           assign pa_dprio_rst_n_iotile_in                = pa_dprio_rst_n;
           assign pa_dprio_write_iotile_in                = pa_dprio_write;
           assign pa_dprio_writedata_iotile_in            = pa_dprio_writedata;

     end
     else begin : abphy_connections
       always @ ( * ) begin                                                      
         if ( runAbstractPhySim==0 ) begin
           phy_reset_n                                        = phy_reset_n_nonabphy;
           ctl2core_avl_rdata_id_1                            = ctl2core_avl_rdata_id_1_nonabphy;
           ctl2core_avl_rdata_id_0                            = ctl2core_avl_rdata_id_0_nonabphy;
           ctl2core_mmr_0                                     = ctl2core_mmr_0_nonabphy;
           l2core_afi_wlat                                    = l2core_afi_wlat_nonabphy;
           l2core_data                                        = l2core_data_nonabphy;
           l2b_dtc                                            = l2b_dtc_nonabphy;
           ctl2core_mmr_1                                     = ctl2core_mmr_1_nonabphy;
           l2b_data                                           = l2b_data_nonabphy;
           l2core_rd_data_vld_avl0                            = l2core_rd_data_vld_avl0_nonabphy;
           ctl2core_avl_cmd_ready_0                           = ctl2core_avl_cmd_ready_0_nonabphy;
           phy_fb_clk_to_pll                                  = phy_fb_clk_to_pll_nonabphy;
           l2b_oe                                             = l2b_oe_nonabphy;
           ctl2core_sideband_0                                = ctl2core_sideband_0_nonabphy;
           l2core_wb_pointer_for_ecc                          = l2core_wb_pointer_for_ecc_nonabphy;
           t2c_afi                                            = t2c_afi_nonabphy;
           pa_dprio_block_select                              = pa_dprio_block_select_nonabphy;
           ctl2core_sideband_1                                = ctl2core_sideband_1_nonabphy;
           core_clks_locked_cpa_pri                           = core_clks_locked_cpa_pri_nonabphy;
           core_clks_locked_cpa_sec                           = core_clks_locked_cpa_sec_nonabphy;
           core_clks_from_cpa_pri                             = core_clks_from_cpa_pri_nonabphy;
           core_clks_from_cpa_sec                             = core_clks_from_cpa_sec_nonabphy;
           l2core_rdata_valid                                 = l2core_rdata_valid_nonabphy;
           ctl2core_avl_cmd_ready_1                           = ctl2core_avl_cmd_ready_1_nonabphy;
           l2core_afi_rlat                                    = l2core_afi_rlat_nonabphy;
           l2core_wr_data_rdy_ast                             = l2core_wr_data_rdy_ast_nonabphy;
           pa_dprio_readdata                                  = pa_dprio_readdata_nonabphy;
           
           global_reset_n_int_iotile_in            = global_reset_n_int;
           pll_locked_iotile_in                    = pll_locked;
           pll_dll_clk_iotile_in                   = pll_dll_clk;
           phy_clk_phs_iotile_in                   = phy_clk_phs;
           phy_clk_iotile_in                       = phy_clk;
           phy_fb_clk_to_tile_iotile_in            = phy_fb_clk_to_tile;
           core_clks_fb_to_cpa_pri_iotile_in       = core_clks_fb_to_cpa_pri;
           core_clks_fb_to_cpa_sec_iotile_in       = core_clks_fb_to_cpa_sec;
           core2ctl_avl_0_iotile_in                = core2ctl_avl_0;
           core2ctl_avl_1_iotile_in                = core2ctl_avl_1;
           core2ctl_avl_rd_data_ready_0_iotile_in  = core2ctl_avl_rd_data_ready_0;
           core2ctl_avl_rd_data_ready_1_iotile_in  = core2ctl_avl_rd_data_ready_1;
           core2l_wr_data_vld_ast_0_iotile_in      = core2l_wr_data_vld_ast_0;
           core2l_wr_data_vld_ast_1_iotile_in      = core2l_wr_data_vld_ast_1;
           core2l_rd_data_rdy_ast_0_iotile_in      = core2l_rd_data_rdy_ast_0;
           core2l_rd_data_rdy_ast_1_iotile_in      = core2l_rd_data_rdy_ast_1;
           core2l_wr_ecc_info_0_iotile_in          = core2l_wr_ecc_info_0;
           core2l_wr_ecc_info_1_iotile_in          = core2l_wr_ecc_info_1;
           core2l_data_iotile_in                   = core2l_data;
           core2l_oe_iotile_in                     = core2l_oe;
           core2l_rdata_en_full_iotile_in          = core2l_rdata_en_full;
           core2l_mrnk_read_iotile_in              = core2l_mrnk_read;
           core2l_mrnk_write_iotile_in             = core2l_mrnk_write;
           c2t_afi_iotile_in                       = c2t_afi;
           core2ctl_sideband_0_iotile_in           = core2ctl_sideband_0;
           core2ctl_sideband_1_iotile_in           = core2ctl_sideband_1;
           core2ctl_mmr_0_iotile_in                = core2ctl_mmr_0;
           core2ctl_mmr_1_iotile_in                = core2ctl_mmr_1;
           b2l_data_iotile_in                      = b2l_data;
           b2t_dqs_iotile_in                       = b2t_dqs;
           b2t_dqsb_iotile_in                      = b2t_dqsb;
           cal_bus_clk_iotile_in                   = cal_bus_clk;
           cal_bus_avl_read_iotile_in              = cal_bus_avl_read;
           cal_bus_avl_write_iotile_in             = cal_bus_avl_write;
           cal_bus_avl_address_iotile_in           = cal_bus_avl_address;
           cal_bus_avl_write_data_iotile_in        = cal_bus_avl_write_data;
           pa_dprio_clk_iotile_in                  = pa_dprio_clk;
           pa_dprio_read_iotile_in                 = pa_dprio_read;
           pa_dprio_reg_addr_iotile_in             = pa_dprio_reg_addr;
           pa_dprio_rst_n_iotile_in                = pa_dprio_rst_n;
           pa_dprio_write_iotile_in                = pa_dprio_write;
           pa_dprio_writedata_iotile_in            = pa_dprio_writedata;
         end
         else begin
           phy_reset_n                                        = phy_reset_n_abphy;
           ctl2core_avl_rdata_id_1                            = ctl2core_avl_rdata_id_1_abphy;
           ctl2core_avl_rdata_id_0                            = ctl2core_avl_rdata_id_0_abphy;
           ctl2core_mmr_0                                     = ctl2core_mmr_0_abphy;
           l2core_afi_wlat                                    = l2core_afi_wlat_abphy;
           l2core_data                                        = l2core_data_abphy;
           l2b_dtc                                            = l2b_dtc_abphy;
           ctl2core_mmr_1                                     = ctl2core_mmr_1_abphy;
           l2b_data                                           = l2b_data_abphy;
           l2core_rd_data_vld_avl0                            = l2core_rd_data_vld_avl0_abphy;
           ctl2core_avl_cmd_ready_0                           = ctl2core_avl_cmd_ready_0_abphy;
           phy_fb_clk_to_pll                                  = phy_fb_clk_to_pll_abphy;
           l2b_oe                                             = l2b_oe_abphy;
           ctl2core_sideband_0                                = ctl2core_sideband_0_abphy;
           l2core_wb_pointer_for_ecc                          = l2core_wb_pointer_for_ecc_abphy;
           t2c_afi                                            = t2c_afi_abphy;
           pa_dprio_block_select                              = pa_dprio_block_select_abphy;
           ctl2core_sideband_1                                = ctl2core_sideband_1_abphy;
           core_clks_locked_cpa_pri                           = core_clks_locked_cpa_pri_abphy;
           core_clks_locked_cpa_sec                           = core_clks_locked_cpa_sec_abphy;
           core_clks_from_cpa_pri                             = core_clks_from_cpa_pri_abphy;
           core_clks_from_cpa_sec                             = core_clks_from_cpa_sec_abphy;
           l2core_rdata_valid                                 = l2core_rdata_valid_abphy;
           ctl2core_avl_cmd_ready_1                           = ctl2core_avl_cmd_ready_1_abphy;
           l2core_afi_rlat                                    = l2core_afi_rlat_abphy;
           l2core_wr_data_rdy_ast                             = l2core_wr_data_rdy_ast_abphy;
           pa_dprio_readdata                                  = pa_dprio_readdata_abphy;
           
           global_reset_n_int_iotile_in            = 'd0;
           pll_locked_iotile_in                    = 'd0;
           pll_dll_clk_iotile_in                   = 'd0;
           phy_clk_phs_iotile_in                   = 'd0;
           phy_clk_iotile_in                       = 'd0;
           phy_fb_clk_to_tile_iotile_in            = 'd0;
           core_clks_fb_to_cpa_pri_iotile_in       = 'd0;
           core_clks_fb_to_cpa_sec_iotile_in       = 'd0;
           core2ctl_avl_0_iotile_in                = 'd0;
           core2ctl_avl_1_iotile_in                = 'd0;
           core2ctl_avl_rd_data_ready_0_iotile_in  = 'd0;
           core2ctl_avl_rd_data_ready_1_iotile_in  = 'd0;
           core2l_wr_data_vld_ast_0_iotile_in      = 'd0;
           core2l_wr_data_vld_ast_1_iotile_in      = 'd0;
           core2l_rd_data_rdy_ast_0_iotile_in      = 'd0;
           core2l_rd_data_rdy_ast_1_iotile_in      = 'd0;
           core2l_wr_ecc_info_0_iotile_in          = 'd0;
           core2l_wr_ecc_info_1_iotile_in          = 'd0;
           core2l_data_iotile_in                   = 'd0;
           core2l_oe_iotile_in                     = 'd0;
           core2l_rdata_en_full_iotile_in          = 'd0;
           core2l_mrnk_read_iotile_in              = 'd0;
           core2l_mrnk_write_iotile_in             = 'd0;
           c2t_afi_iotile_in                       = 'd0;
           core2ctl_sideband_0_iotile_in           = 'd0;
           core2ctl_sideband_1_iotile_in           = 'd0;
           core2ctl_mmr_0_iotile_in                = 'd0;
           core2ctl_mmr_1_iotile_in                = 'd0;
           b2l_data_iotile_in                      = 'd0;
           b2t_dqs_iotile_in                       = 'd0;
           b2t_dqsb_iotile_in                      = 'd0;
           cal_bus_clk_iotile_in                   = 'd0;
           cal_bus_avl_read_iotile_in              = 'd0;
           cal_bus_avl_write_iotile_in             = 'd0;
           cal_bus_avl_address_iotile_in           = 'd0;
           cal_bus_avl_write_data_iotile_in        = 'd0;
           pa_dprio_clk_iotile_in                  = 'd0;
           pa_dprio_read_iotile_in                 = 'd0;
           pa_dprio_reg_addr_iotile_in             = 'd0;
           pa_dprio_rst_n_iotile_in                = 'd0;
           pa_dprio_write_iotile_in                = 'd0;
           pa_dprio_writedata_iotile_in            = 'd0;
         end
       end
     end
   endgenerate
   
endmodule
