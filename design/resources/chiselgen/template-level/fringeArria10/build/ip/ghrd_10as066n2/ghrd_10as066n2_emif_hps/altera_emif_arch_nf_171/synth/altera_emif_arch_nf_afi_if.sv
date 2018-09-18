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
// This module is responsible for exposing the AFI interface through which
// a soft controller interacts with the memory interface PHY inside the tile.
// 
///////////////////////////////////////////////////////////////////////////////

`define _get_pin_count(_loc) ( _loc[ 9 : 0 ] )
`define _get_pin_index(_loc, _port_i) ( _loc[ (_port_i + 1) * 10 +: 10 ] )

`define _get_tile(_loc, _port_i) (  `_get_pin_index(_loc, _port_i) / (PINS_PER_LANE * LANES_PER_TILE) )
`define _get_lane(_loc, _port_i) ( (`_get_pin_index(_loc, _port_i) / PINS_PER_LANE) % LANES_PER_TILE ) 
`define _get_pin(_loc, _port_i)  (  `_get_pin_index(_loc, _port_i) % PINS_PER_LANE )

`define _get_lane_usage(_tile_i, _lane_i) ( LANES_USAGE[(_tile_i * LANES_PER_TILE + _lane_i) * 3 +: 3] )

`define _core2l_afi(_loc, _port_i, _phase_i) core2l_data\
   [`_get_tile(_loc, _port_i)]\
   [`_get_lane(_loc, _port_i)]\
   [(`_get_pin(_loc, _port_i) * 8) + _phase_i]

`define _core2l_oe(_loc, _port_i, _phase_i) core2l_oe\
   [`_get_tile(_loc, _port_i)]\
   [`_get_lane(_loc, _port_i)]\
   [(`_get_pin(_loc, _port_i) * 4) + _phase_i]
   
`define _l2core_afi(_loc, _port_i, _phase_i) l2core_data\
   [`_get_tile(_loc, _port_i)]\
   [`_get_lane(_loc, _port_i)]\
   [(`_get_pin(_loc, _port_i) * 8) + _phase_i]
   
`define _unused_core2l_afi(_pin_i) core2l_data\
   [_pin_i / (PINS_PER_LANE * LANES_PER_TILE)]\
   [(_pin_i / PINS_PER_LANE) % LANES_PER_TILE]\
   [((_pin_i % PINS_PER_LANE) * 8) +: 8]
   
`define _unused_core2l_oe(_pin_i) core2l_oe\
   [_pin_i / (PINS_PER_LANE * LANES_PER_TILE)]\
   [(_pin_i / PINS_PER_LANE) % LANES_PER_TILE]\
   [((_pin_i % PINS_PER_LANE) * 4) +: 4]   

`define _connect_out(_loc, _mem_port_width, _afi_port_width, _afi_port, _afi_oe_port_width, _afi_oe_port) \
   for (port_i = 0; port_i < _mem_port_width; ++port_i) begin : oport \
      for (phase_i = 0; phase_i < 8; ++phase_i) begin : data_phase \
         if (phase_i < _afi_port_width / _mem_port_width) begin \
            assign `_core2l_afi(_loc, port_i, phase_i) = _afi_port[_mem_port_width * phase_i + port_i]; \
         end else begin \
            assign `_core2l_afi(_loc, port_i, phase_i) = 1'b0; \
         end \
      end \
      for (phase_i = 0; phase_i < 4; ++phase_i) begin : oe_phase \
         if (phase_i < SDR_RATIO) begin \
            assign `_core2l_oe(_loc, port_i, phase_i) = _afi_oe_port[(port_i / (_mem_port_width / (_afi_oe_port_width / SDR_RATIO))) * SDR_RATIO + phase_i]; \
         end else begin \
            assign `_core2l_oe(_loc, port_i, phase_i) = 1'b0; \
         end \
      end \
   end
   
`define _connect_out_with_regs(_loc, _mem_port_width, _afi_port_width, _afi_port, _afi_oe_port_width, _afi_oe_port) \
   logic [_afi_port_width-1:0] sr_o; \
   altera_emif_arch_nf_regs # ( \
      .REGISTER     (REGISTER_AFI), \
      .WIDTH        (_afi_port_width) \
   ) afi_regs_o ( \
      .clk      (afi_clk), \
      .reset_n  (1'b1), \
      .data_in  (_afi_port), \
      .data_out (sr_o) \
   ); \
   `_connect_out(_loc, _mem_port_width, _afi_port_width, sr_o, _afi_oe_port_width, _afi_oe_port)
   
`define _connect_in(_loc, _mem_port_width, _afi_port_width, _afi_port) \
   for (port_i = 0; port_i < _mem_port_width; ++port_i) begin : iport \
      for (phase_i = 0; phase_i < _afi_port_width / _mem_port_width; ++phase_i) begin : data_phase \
         assign _afi_port[_mem_port_width * phase_i + port_i] = `_l2core_afi(_loc, port_i, phase_i); \
      end \
   end   

`define _connect_in_with_regs(_loc, _mem_port_width, _afi_port_width, _afi_port) \
   logic [_afi_port_width-1:0] sr_i; \
   `_connect_in(_loc, _mem_port_width, _afi_port_width, sr_i) \
   altera_emif_arch_nf_regs # ( \
      .REGISTER     (REGISTER_AFI), \
      .WIDTH        (_afi_port_width) \
   ) afi_regs_i ( \
      .clk      (afi_clk), \
      .reset_n  (1'b1), \
      .data_in  (sr_i), \
      .data_out (_afi_port) \
   );
   
module altera_emif_arch_nf_afi_if #(

   parameter MEM_TTL_DATA_WIDTH                      = 0,
   parameter MEM_TTL_NUM_OF_READ_GROUPS              = 0,
   parameter MEM_TTL_NUM_OF_WRITE_GROUPS             = 0,
   parameter REGISTER_AFI                            = 0,
   parameter PORT_AFI_ADDR_WIDTH                     = 1,
   parameter PORT_AFI_BA_WIDTH                       = 1,
   parameter PORT_AFI_BG_WIDTH                       = 1,
   parameter PORT_AFI_C_WIDTH                        = 1,
   parameter PORT_AFI_CKE_WIDTH                      = 1,
   parameter PORT_AFI_CS_N_WIDTH                     = 1,
   parameter PORT_AFI_RM_WIDTH                       = 1,
   parameter PORT_AFI_ODT_WIDTH                      = 1,
   parameter PORT_AFI_RAS_N_WIDTH                    = 1,
   parameter PORT_AFI_CAS_N_WIDTH                    = 1,
   parameter PORT_AFI_WE_N_WIDTH                     = 1,
   parameter PORT_AFI_RST_N_WIDTH                    = 1,
   parameter PORT_AFI_ACT_N_WIDTH                    = 1,
   parameter PORT_AFI_PAR_WIDTH                      = 1,
   parameter PORT_AFI_CA_WIDTH                       = 1,
   parameter PORT_AFI_REF_N_WIDTH                    = 1,
   parameter PORT_AFI_WPS_N_WIDTH                    = 1,
   parameter PORT_AFI_RPS_N_WIDTH                    = 1,
   parameter PORT_AFI_DOFF_N_WIDTH                   = 1,
   parameter PORT_AFI_LD_N_WIDTH                     = 1,
   parameter PORT_AFI_RW_N_WIDTH                     = 1,
   parameter PORT_AFI_LBK0_N_WIDTH                   = 1,
   parameter PORT_AFI_LBK1_N_WIDTH                   = 1,
   parameter PORT_AFI_CFG_N_WIDTH                    = 1,
   parameter PORT_AFI_AP_WIDTH                       = 1,
   parameter PORT_AFI_AINV_WIDTH                     = 1,
   parameter PORT_AFI_DM_WIDTH                       = 1,
   parameter PORT_AFI_DM_N_WIDTH                     = 1,
   parameter PORT_AFI_BWS_N_WIDTH                    = 1,
   parameter PORT_AFI_RDATA_DBI_N_WIDTH              = 1,
   parameter PORT_AFI_WDATA_DBI_N_WIDTH              = 1,
   parameter PORT_AFI_RDATA_DINV_WIDTH               = 1,
   parameter PORT_AFI_WDATA_DINV_WIDTH               = 1,
   parameter PORT_AFI_DQS_BURST_WIDTH                = 1,
   parameter PORT_AFI_WDATA_VALID_WIDTH              = 1,
   parameter PORT_AFI_WDATA_WIDTH                    = 1,
   parameter PORT_AFI_RDATA_EN_FULL_WIDTH            = 1,
   parameter PORT_AFI_RDATA_WIDTH                    = 1,
   parameter PORT_AFI_RDATA_VALID_WIDTH              = 1,
   parameter PORT_AFI_RRANK_WIDTH                    = 1,
   parameter PORT_AFI_WRANK_WIDTH                    = 1,
   parameter PORT_AFI_ALERT_N_WIDTH                  = 1,
   parameter PORT_AFI_PE_N_WIDTH                     = 1,
   
   // Definition of port widths for "mem" interface (auto-generated)
   //AUTOGEN_BEGIN: Definition of memory port widths   
   parameter PORT_MEM_CK_WIDTH                       = 1,
   parameter PORT_MEM_CK_N_WIDTH                     = 1,
   parameter PORT_MEM_DK_WIDTH                       = 1,
   parameter PORT_MEM_DK_N_WIDTH                     = 1,
   parameter PORT_MEM_DKA_WIDTH                      = 1,
   parameter PORT_MEM_DKA_N_WIDTH                    = 1,
   parameter PORT_MEM_DKB_WIDTH                      = 1,
   parameter PORT_MEM_DKB_N_WIDTH                    = 1,
   parameter PORT_MEM_K_WIDTH                        = 1,
   parameter PORT_MEM_K_N_WIDTH                      = 1,
   parameter PORT_MEM_A_WIDTH                        = 1,
   parameter PORT_MEM_BA_WIDTH                       = 1,
   parameter PORT_MEM_BG_WIDTH                       = 1,
   parameter PORT_MEM_C_WIDTH                        = 1,
   parameter PORT_MEM_CKE_WIDTH                      = 1,
   parameter PORT_MEM_CS_N_WIDTH                     = 1,
   parameter PORT_MEM_RM_WIDTH                       = 1,
   parameter PORT_MEM_ODT_WIDTH                      = 1,
   parameter PORT_MEM_RAS_N_WIDTH                    = 1,
   parameter PORT_MEM_CAS_N_WIDTH                    = 1,
   parameter PORT_MEM_WE_N_WIDTH                     = 1,
   parameter PORT_MEM_RESET_N_WIDTH                  = 1,
   parameter PORT_MEM_ACT_N_WIDTH                    = 1,
   parameter PORT_MEM_PAR_WIDTH                      = 1,
   parameter PORT_MEM_CA_WIDTH                       = 1,
   parameter PORT_MEM_REF_N_WIDTH                    = 1,
   parameter PORT_MEM_WPS_N_WIDTH                    = 1,
   parameter PORT_MEM_RPS_N_WIDTH                    = 1,
   parameter PORT_MEM_DOFF_N_WIDTH                   = 1,
   parameter PORT_MEM_LDA_N_WIDTH                    = 1,
   parameter PORT_MEM_LDB_N_WIDTH                    = 1,
   parameter PORT_MEM_RWA_N_WIDTH                    = 1,
   parameter PORT_MEM_RWB_N_WIDTH                    = 1,
   parameter PORT_MEM_LBK0_N_WIDTH                   = 1,
   parameter PORT_MEM_LBK1_N_WIDTH                   = 1,
   parameter PORT_MEM_CFG_N_WIDTH                    = 1,
   parameter PORT_MEM_AP_WIDTH                       = 1,
   parameter PORT_MEM_AINV_WIDTH                     = 1,
   parameter PORT_MEM_DM_WIDTH                       = 1,
   parameter PORT_MEM_BWS_N_WIDTH                    = 1,
   parameter PORT_MEM_D_WIDTH                        = 1,
   parameter PORT_MEM_DQ_WIDTH                       = 1,
   parameter PORT_MEM_DBI_N_WIDTH                    = 1,
   parameter PORT_MEM_DQA_WIDTH                      = 1,
   parameter PORT_MEM_DQB_WIDTH                      = 1,
   parameter PORT_MEM_DINVA_WIDTH                    = 1,
   parameter PORT_MEM_DINVB_WIDTH                    = 1,
   parameter PORT_MEM_Q_WIDTH                        = 1,
   parameter PORT_MEM_DQS_WIDTH                      = 1,
   parameter PORT_MEM_DQS_N_WIDTH                    = 1,
   parameter PORT_MEM_QK_WIDTH                       = 1,
   parameter PORT_MEM_QK_N_WIDTH                     = 1,
   parameter PORT_MEM_QKA_WIDTH                      = 1,
   parameter PORT_MEM_QKA_N_WIDTH                    = 1,
   parameter PORT_MEM_QKB_WIDTH                      = 1,
   parameter PORT_MEM_QKB_N_WIDTH                    = 1,
   parameter PORT_MEM_CQ_WIDTH                       = 1,
   parameter PORT_MEM_CQ_N_WIDTH                     = 1,
   parameter PORT_MEM_ALERT_N_WIDTH                  = 1,
   parameter PORT_MEM_PE_N_WIDTH                     = 1,
   
   parameter PORT_MEM_CK_PINLOC                      = 10'b0000000000,
   parameter PORT_MEM_CK_N_PINLOC                    = 10'b0000000000,
   parameter PORT_MEM_DK_PINLOC                      = 10'b0000000000,
   parameter PORT_MEM_DK_N_PINLOC                    = 10'b0000000000,
   parameter PORT_MEM_DKA_PINLOC                     = 10'b0000000000,
   parameter PORT_MEM_DKA_N_PINLOC                   = 10'b0000000000,
   parameter PORT_MEM_DKB_PINLOC                     = 10'b0000000000,
   parameter PORT_MEM_DKB_N_PINLOC                   = 10'b0000000000,
   parameter PORT_MEM_K_PINLOC                       = 10'b0000000000,
   parameter PORT_MEM_K_N_PINLOC                     = 10'b0000000000,
   parameter PORT_MEM_A_PINLOC                       = 10'b0000000000,
   parameter PORT_MEM_BA_PINLOC                      = 10'b0000000000,
   parameter PORT_MEM_BG_PINLOC                      = 10'b0000000000,
   parameter PORT_MEM_C_PINLOC                       = 10'b0000000000,
   parameter PORT_MEM_CKE_PINLOC                     = 10'b0000000000,
   parameter PORT_MEM_CS_N_PINLOC                    = 10'b0000000000,
   parameter PORT_MEM_RM_PINLOC                      = 10'b0000000000,
   parameter PORT_MEM_ODT_PINLOC                     = 10'b0000000000,
   parameter PORT_MEM_RAS_N_PINLOC                   = 10'b0000000000,
   parameter PORT_MEM_CAS_N_PINLOC                   = 10'b0000000000,
   parameter PORT_MEM_WE_N_PINLOC                    = 10'b0000000000,
   parameter PORT_MEM_RESET_N_PINLOC                 = 10'b0000000000,
   parameter PORT_MEM_ACT_N_PINLOC                   = 10'b0000000000,
   parameter PORT_MEM_PAR_PINLOC                     = 10'b0000000000,
   parameter PORT_MEM_CA_PINLOC                      = 10'b0000000000,
   parameter PORT_MEM_REF_N_PINLOC                   = 10'b0000000000,
   parameter PORT_MEM_WPS_N_PINLOC                   = 10'b0000000000,
   parameter PORT_MEM_RPS_N_PINLOC                   = 10'b0000000000,
   parameter PORT_MEM_DOFF_N_PINLOC                  = 10'b0000000000,
   parameter PORT_MEM_LDA_N_PINLOC                   = 10'b0000000000,
   parameter PORT_MEM_LDB_N_PINLOC                   = 10'b0000000000,
   parameter PORT_MEM_RWA_N_PINLOC                   = 10'b0000000000,
   parameter PORT_MEM_RWB_N_PINLOC                   = 10'b0000000000,
   parameter PORT_MEM_LBK0_N_PINLOC                  = 10'b0000000000,
   parameter PORT_MEM_LBK1_N_PINLOC                  = 10'b0000000000,
   parameter PORT_MEM_CFG_N_PINLOC                   = 10'b0000000000,
   parameter PORT_MEM_AP_PINLOC                      = 10'b0000000000,
   parameter PORT_MEM_AINV_PINLOC                    = 10'b0000000000,
   parameter PORT_MEM_DM_PINLOC                      = 10'b0000000000,
   parameter PORT_MEM_BWS_N_PINLOC                   = 10'b0000000000,
   parameter PORT_MEM_D_PINLOC                       = 10'b0000000000,
   parameter PORT_MEM_DQ_PINLOC                      = 10'b0000000000,
   parameter PORT_MEM_DBI_N_PINLOC                   = 10'b0000000000,
   parameter PORT_MEM_DQA_PINLOC                     = 10'b0000000000,
   parameter PORT_MEM_DQB_PINLOC                     = 10'b0000000000,
   parameter PORT_MEM_DINVA_PINLOC                   = 10'b0000000000,
   parameter PORT_MEM_DINVB_PINLOC                   = 10'b0000000000,
   parameter PORT_MEM_Q_PINLOC                       = 10'b0000000000,
   parameter PORT_MEM_DQS_PINLOC                     = 10'b0000000000,
   parameter PORT_MEM_DQS_N_PINLOC                   = 10'b0000000000,
   parameter PORT_MEM_QK_PINLOC                      = 10'b0000000000,
   parameter PORT_MEM_QK_N_PINLOC                    = 10'b0000000000,
   parameter PORT_MEM_QKA_PINLOC                     = 10'b0000000000,
   parameter PORT_MEM_QKA_N_PINLOC                   = 10'b0000000000,
   parameter PORT_MEM_QKB_PINLOC                     = 10'b0000000000,
   parameter PORT_MEM_QKB_N_PINLOC                   = 10'b0000000000,
   parameter PORT_MEM_CQ_PINLOC                      = 10'b0000000000,
   parameter PORT_MEM_CQ_N_PINLOC                    = 10'b0000000000,
   parameter PORT_MEM_ALERT_N_PINLOC                 = 10'b0000000000,
   parameter PORT_MEM_PE_N_PINLOC                    = 10'b0000000000,
   
   parameter PINS_PER_LANE                           = 1,
   parameter LANES_PER_TILE                          = 1,
   parameter NUM_OF_RTL_TILES                        = 1,
   parameter LANES_USAGE                             = 1'b0,
   parameter PRI_RDATA_TILE_INDEX                    = -1,
   parameter PRI_RDATA_LANE_INDEX                    = -1,
   parameter PRI_WDATA_TILE_INDEX                    = -1,
   parameter PRI_WDATA_LANE_INDEX                    = -1,
   parameter SEC_RDATA_TILE_INDEX                    = -1,
   parameter SEC_RDATA_LANE_INDEX                    = -1,
   parameter SEC_WDATA_TILE_INDEX                    = -1,
   parameter SEC_WDATA_LANE_INDEX                    = -1,
   
   // Parameter indicating the core-2-lane connection of a pin is actually driven
   parameter PINS_C2L_DRIVEN                         = 1'b0,
   
   // Parameter indicating if the OE is inverted or not
   parameter PINS_INVERT_OE                          = 1'b0,
   
   parameter MEM_DATA_MASK_EN                        = 1,
   parameter PHY_HMC_CLK_RATIO                       = 1
   
) (
   input  logic                                               afi_clk,
   input  logic                                               afi_reset_n,

   input  logic [PORT_AFI_ADDR_WIDTH-1:0]                     afi_addr,
   input  logic [PORT_AFI_BA_WIDTH-1:0]                       afi_ba,
   input  logic [PORT_AFI_BG_WIDTH-1:0]                       afi_bg,
   input  logic [PORT_AFI_C_WIDTH-1:0]                        afi_c,
   input  logic [PORT_AFI_CKE_WIDTH-1:0]                      afi_cke,
   input  logic [PORT_AFI_CS_N_WIDTH-1:0]                     afi_cs_n,
   input  logic [PORT_AFI_RM_WIDTH-1:0]                       afi_rm,
   input  logic [PORT_AFI_ODT_WIDTH-1:0]                      afi_odt,
   input  logic [PORT_AFI_RAS_N_WIDTH-1:0]                    afi_ras_n,
   input  logic [PORT_AFI_CAS_N_WIDTH-1:0]                    afi_cas_n,
   input  logic [PORT_AFI_WE_N_WIDTH-1:0]                     afi_we_n,
   input  logic [PORT_AFI_RST_N_WIDTH-1:0]                    afi_rst_n,
   input  logic [PORT_AFI_ACT_N_WIDTH-1:0]                    afi_act_n,
   input  logic [PORT_AFI_PAR_WIDTH-1:0]                      afi_par,
   input  logic [PORT_AFI_CA_WIDTH-1:0]                       afi_ca,
   input  logic [PORT_AFI_REF_N_WIDTH-1:0]                    afi_ref_n,
   input  logic [PORT_AFI_WPS_N_WIDTH-1:0]                    afi_wps_n,
   input  logic [PORT_AFI_RPS_N_WIDTH-1:0]                    afi_rps_n,
   input  logic [PORT_AFI_DOFF_N_WIDTH-1:0]                   afi_doff_n,
   input  logic [PORT_AFI_LD_N_WIDTH-1:0]                     afi_ld_n,
   input  logic [PORT_AFI_RW_N_WIDTH-1:0]                     afi_rw_n,
   input  logic [PORT_AFI_LBK0_N_WIDTH-1:0]                   afi_lbk0_n,
   input  logic [PORT_AFI_LBK1_N_WIDTH-1:0]                   afi_lbk1_n,
   input  logic [PORT_AFI_CFG_N_WIDTH-1:0]                    afi_cfg_n,
   input  logic [PORT_AFI_AP_WIDTH-1:0]                       afi_ap,
   input  logic [PORT_AFI_AINV_WIDTH-1:0]                     afi_ainv,
   input  logic [PORT_AFI_DM_WIDTH-1:0]                       afi_dm,
   input  logic [PORT_AFI_DM_N_WIDTH-1:0]                     afi_dm_n,
   input  logic [PORT_AFI_BWS_N_WIDTH-1:0]                    afi_bws_n,
   output logic [PORT_AFI_RDATA_DBI_N_WIDTH-1:0]              afi_rdata_dbi_n,
   input  logic [PORT_AFI_WDATA_DBI_N_WIDTH-1:0]              afi_wdata_dbi_n,
   output logic [PORT_AFI_RDATA_DINV_WIDTH-1:0]               afi_rdata_dinv,
   input  logic [PORT_AFI_WDATA_DINV_WIDTH-1:0]               afi_wdata_dinv,
   input  logic [PORT_AFI_DQS_BURST_WIDTH-1:0]                afi_dqs_burst,
   input  logic [PORT_AFI_WDATA_VALID_WIDTH-1:0]              afi_wdata_valid,
   input  logic [PORT_AFI_WDATA_WIDTH-1:0]                    afi_wdata,
   input  logic [PORT_AFI_RDATA_EN_FULL_WIDTH-1:0]            afi_rdata_en_full,
   output logic [PORT_AFI_RDATA_WIDTH-1:0]                    afi_rdata,
   output logic [PORT_AFI_RDATA_VALID_WIDTH-1:0]              afi_rdata_valid,
   input  logic [PORT_AFI_RRANK_WIDTH-1:0]                    afi_rrank,
   input  logic [PORT_AFI_WRANK_WIDTH-1:0]                    afi_wrank,
   output logic [PORT_AFI_ALERT_N_WIDTH-1:0]                  afi_alert_n,
   output logic [PORT_AFI_PE_N_WIDTH-1:0]                     afi_pe_n,

   output logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][PINS_PER_LANE * 8 - 1:0]      core2l_data,
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][PINS_PER_LANE * 8 - 1:0]      l2core_data,
   output logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][PINS_PER_LANE * 4 - 1:0]      core2l_oe,
   
   output logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][3:0]                          core2l_rdata_en_full,
   output logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][15:0]                         core2l_mrnk_read,
   output logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][15:0]                         core2l_mrnk_write,
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][3:0]                          l2core_rdata_valid
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
   
   localparam SDR_RATIO = PHY_HMC_CLK_RATIO;
   localparam DDR_RATIO = SDR_RATIO * 2;
   
   localparam NUM_OF_LOGICAL_RANKS = PORT_AFI_RRANK_WIDTH / SDR_RATIO;
   
   logic [PORT_AFI_RRANK_WIDTH-1:0]  afi_rrank_r;
   logic [PORT_AFI_WRANK_WIDTH-1:0]  afi_wrank_r;
   logic [15:0]                      afi_rrank_r_padded;
   logic [15:0]                      afi_wrank_r_padded;
   
   (* altera_attribute = {"-name MAX_FANOUT 1; -name ADV_NETLIST_OPT_ALLOWED ALWAYS_ALLOW"}*)
   altera_emif_arch_nf_regs # (
      .REGISTER (REGISTER_AFI),
      .WIDTH    (PORT_AFI_RRANK_WIDTH)
   ) afi_rrank_regs (
      .clk      (afi_clk),
      .reset_n  (afi_reset_n),
      .data_in  (afi_rrank),
      .data_out (afi_rrank_r)
   );
   
   (* altera_attribute = {"-name MAX_FANOUT 1; -name ADV_NETLIST_OPT_ALLOWED ALWAYS_ALLOW"}*)
   altera_emif_arch_nf_regs # (
      .REGISTER (REGISTER_AFI),
      .WIDTH    (PORT_AFI_WRANK_WIDTH)
   ) afi_wrank_regs (
      .clk      (afi_clk),
      .reset_n  (afi_reset_n),
      .data_in  (afi_wrank),
      .data_out (afi_wrank_r)
   );
   
   generate
      genvar r;
      genvar t;
   
      for (t = 0; t < 4; ++t) begin: timeslot
         for (r = 0; r < 4; ++r) begin : rank
            if (t >= SDR_RATIO || r >= NUM_OF_LOGICAL_RANKS) begin
               assign afi_rrank_r_padded[t * 4 + r] = 1'b0;
               assign afi_wrank_r_padded[t * 4 + r] = 1'b0;
            end else begin
               assign afi_rrank_r_padded[t * 4 + r] = afi_rrank_r[t * NUM_OF_LOGICAL_RANKS + r];
               assign afi_wrank_r_padded[t * 4 + r] = afi_wrank_r[t * NUM_OF_LOGICAL_RANKS + r];
            end
         end
      end
   endgenerate
   
   assign core2l_mrnk_read  = {(NUM_OF_RTL_TILES * LANES_PER_TILE){afi_rrank_r_padded}};
   assign core2l_mrnk_write = {(NUM_OF_RTL_TILES * LANES_PER_TILE){afi_wrank_r_padded}};
   
   assign afi_alert_n = '0;
   assign afi_pe_n = '0;
   
   generate
      genvar port_i;
      genvar phase_i;
      genvar tile_i;
      genvar lane_i;
      genvar pin_i;
      genvar i;
      
      ////////////////////////////////////////////////////////////////////////////
      // Connection for read control signals afi_rdata_en_full and afi_rdata_valid
      ////////////////////////////////////////////////////////////////////////////
      
      // Register and duplicate the afi_rdata_en_full signal for timing closure
      logic [PORT_AFI_RDATA_EN_FULL_WIDTH-1:0] afi_rdata_en_full_r;
      
      (* altera_attribute = {"-name MAX_FANOUT 1; -name ADV_NETLIST_OPT_ALLOWED ALWAYS_ALLOW"}*)
      altera_emif_arch_nf_regs # (
         .REGISTER (REGISTER_AFI),
         .WIDTH    (PORT_AFI_RDATA_EN_FULL_WIDTH)
      ) afi_rdata_en_full_regs (
         .clk      (afi_clk),
         .reset_n  (afi_reset_n),
         .data_in  (afi_rdata_en_full),
         .data_out (afi_rdata_en_full_r)
      );      
      
      if (`_get_pin_count(PORT_MEM_DQA_PINLOC) != 0 && `_get_pin_count(PORT_MEM_DQB_PINLOC) != 0) begin : dual_port
      
         // External memory has dual data ports (i.e. DQA and DQB, as in QDR-IV)
         // Split afi_rdata_en_full based on which port the signal belongs to.
         // This special code path relies on the location of QKA/QKB pins to identify
         // the read lanes for each data port.
         logic [3:0] afi_rdata_en_full_r_padded_a;
         logic [3:0] afi_rdata_en_full_r_padded_b;
         
         if (SDR_RATIO < 4) begin
            assign afi_rdata_en_full_r_padded_a = {'0, afi_rdata_en_full_r[SDR_RATIO-1:0]};
            assign afi_rdata_en_full_r_padded_b = {'0, afi_rdata_en_full_r[PORT_AFI_RDATA_EN_FULL_WIDTH-1:SDR_RATIO]};
         end else begin
            assign afi_rdata_en_full_r_padded_a = afi_rdata_en_full_r[SDR_RATIO-1:0];
            assign afi_rdata_en_full_r_padded_b = afi_rdata_en_full_r[PORT_AFI_RDATA_EN_FULL_WIDTH-1:SDR_RATIO];
         end
         
         // afi_rdata_en_full for port A
         for (port_i = 0; port_i < PORT_MEM_QKA_WIDTH; ++port_i) begin : port_a
            assign core2l_rdata_en_full[`_get_tile(PORT_MEM_QKA_PINLOC, port_i)][`_get_lane(PORT_MEM_QKA_PINLOC, port_i)] = afi_rdata_en_full_r_padded_a;
            if (MEM_TTL_DATA_WIDTH / MEM_TTL_NUM_OF_READ_GROUPS == 18) begin
               assign core2l_rdata_en_full[`_get_tile(PORT_MEM_QKA_PINLOC, port_i)][`_get_lane(PORT_MEM_QKA_PINLOC, port_i)-1] = afi_rdata_en_full_r_padded_a;
            end
         end

         // afi_rdata_en_full for port B
         for (port_i = 0; port_i < PORT_MEM_QKB_WIDTH; ++port_i) begin : port_b
            assign core2l_rdata_en_full[`_get_tile(PORT_MEM_QKB_PINLOC, port_i)][`_get_lane(PORT_MEM_QKB_PINLOC, port_i)] = afi_rdata_en_full_r_padded_b;
            if (MEM_TTL_DATA_WIDTH / MEM_TTL_NUM_OF_READ_GROUPS == 18) begin
               assign core2l_rdata_en_full[`_get_tile(PORT_MEM_QKB_PINLOC, port_i)][`_get_lane(PORT_MEM_QKB_PINLOC, port_i)-1] = afi_rdata_en_full_r_padded_b;
            end
         end
         
         // Tie off for non-read-data-lanes to avoid synthesis warnings
         for (tile_i = 0; tile_i < NUM_OF_RTL_TILES; ++tile_i) begin : tile
            for (lane_i = 0; lane_i < LANES_PER_TILE; ++lane_i) begin : lane
               if (`_get_lane_usage(tile_i, lane_i) != LANE_USAGE_RDATA && `_get_lane_usage(tile_i, lane_i) != LANE_USAGE_WRDATA)
                  assign core2l_rdata_en_full[tile_i][lane_i] = '0;
            end
         end
         
         // Connection for afi_rdata_valid
         logic [PORT_AFI_RDATA_VALID_WIDTH/2-1:0] afi_rdata_valid_a;
         logic [PORT_AFI_RDATA_VALID_WIDTH/2-1:0] afi_rdata_valid_b;
         
         assign afi_rdata_valid_a = l2core_rdata_valid[`_get_tile(PORT_MEM_QKA_PINLOC, 0)][`_get_lane(PORT_MEM_QKA_PINLOC, 0)][PORT_AFI_RDATA_VALID_WIDTH/2-1:0];
         assign afi_rdata_valid_b = l2core_rdata_valid[`_get_tile(PORT_MEM_QKB_PINLOC, 0)][`_get_lane(PORT_MEM_QKB_PINLOC, 0)][PORT_AFI_RDATA_VALID_WIDTH/2-1:0];
         
         altera_emif_arch_nf_regs # (
            .REGISTER (REGISTER_AFI),
            .WIDTH    (PORT_AFI_RDATA_VALID_WIDTH)
         ) afi_rdata_valid_regs (
            .clk      (afi_clk),
            .reset_n  (afi_reset_n),
            .data_in  ({afi_rdata_valid_b, afi_rdata_valid_a}),
            .data_out (afi_rdata_valid)
         );               
      
      end else begin : single_port
      
         // External memory has single port.
         // This general code path works for non QDR-IV protocols.
         logic [3:0] afi_rdata_en_full_r_padded;
         
         if (PORT_AFI_RDATA_EN_FULL_WIDTH < 4) 
            assign afi_rdata_en_full_r_padded = {'0, afi_rdata_en_full_r};
         else
            assign afi_rdata_en_full_r_padded = afi_rdata_en_full_r;
         
         assign core2l_rdata_en_full = {(NUM_OF_RTL_TILES * LANES_PER_TILE){afi_rdata_en_full_r_padded}};
         
         // Connection for afi_rdata_valid
         altera_emif_arch_nf_regs # (
            .REGISTER (REGISTER_AFI),
            .WIDTH    (PORT_AFI_RDATA_VALID_WIDTH)
         ) afi_rdata_valid_regs (
            .clk      (afi_clk),
            .reset_n  (afi_reset_n),
            .data_in  (l2core_rdata_valid[PRI_RDATA_TILE_INDEX][PRI_RDATA_LANE_INDEX][PORT_AFI_RDATA_VALID_WIDTH-1:0]),
            .data_out (afi_rdata_valid)
         );      
      end
      
      // Generate constant OE signal for output-only ports
      localparam OE_ON_WIDTH = SDR_RATIO;
      logic [OE_ON_WIDTH-1:0] oe_on;
      assign oe_on = '1;
            
      ////////////////////////////////////////////////////////////////////////////
      // Connection for AFI signals that go to output-only pins
      ////////////////////////////////////////////////////////////////////////////
      if (`_get_pin_count(PORT_MEM_A_PINLOC) != 0) begin : mem_a
         `_connect_out_with_regs(PORT_MEM_A_PINLOC, PORT_MEM_A_WIDTH, PORT_AFI_ADDR_WIDTH, afi_addr, OE_ON_WIDTH, oe_on)
      end
      
      if (`_get_pin_count(PORT_MEM_BA_PINLOC) != 0) begin : mem_ba
         `_connect_out_with_regs(PORT_MEM_BA_PINLOC, PORT_MEM_BA_WIDTH, PORT_AFI_BA_WIDTH, afi_ba, OE_ON_WIDTH, oe_on)
      end
      
      if (`_get_pin_count(PORT_MEM_BG_PINLOC) != 0) begin : mem_bg
         `_connect_out_with_regs(PORT_MEM_BG_PINLOC, PORT_MEM_BG_WIDTH, PORT_AFI_BG_WIDTH, afi_bg, OE_ON_WIDTH, oe_on)
      end
      
      if (`_get_pin_count(PORT_MEM_C_PINLOC) != 0) begin : mem_c
         `_connect_out_with_regs(PORT_MEM_C_PINLOC, PORT_MEM_C_WIDTH, PORT_AFI_C_WIDTH, afi_c, OE_ON_WIDTH, oe_on)
      end      

      if (`_get_pin_count(PORT_MEM_CKE_PINLOC) != 0) begin : mem_cke
         `_connect_out_with_regs(PORT_MEM_CKE_PINLOC, PORT_MEM_CKE_WIDTH, PORT_AFI_CKE_WIDTH, afi_cke, OE_ON_WIDTH, oe_on)
      end      

      if (`_get_pin_count(PORT_MEM_CS_N_PINLOC) != 0) begin : mem_cs_n
         `_connect_out_with_regs(PORT_MEM_CS_N_PINLOC, PORT_MEM_CS_N_WIDTH, PORT_AFI_CS_N_WIDTH, afi_cs_n, OE_ON_WIDTH, oe_on)
      end

      if (`_get_pin_count(PORT_MEM_RM_PINLOC) != 0) begin : mem_rm
         `_connect_out_with_regs(PORT_MEM_RM_PINLOC, PORT_MEM_RM_WIDTH, PORT_AFI_RM_WIDTH, afi_rm, OE_ON_WIDTH, oe_on)
      end      
      
      if (`_get_pin_count(PORT_MEM_ODT_PINLOC) != 0) begin : mem_odt
         `_connect_out_with_regs(PORT_MEM_ODT_PINLOC, PORT_MEM_ODT_WIDTH, PORT_AFI_ODT_WIDTH, afi_odt, OE_ON_WIDTH, oe_on)
      end
      
      if (`_get_pin_count(PORT_MEM_RAS_N_PINLOC) != 0) begin : mem_ras_n
         `_connect_out_with_regs(PORT_MEM_RAS_N_PINLOC, PORT_MEM_RAS_N_WIDTH, PORT_AFI_RAS_N_WIDTH, afi_ras_n, OE_ON_WIDTH, oe_on)
      end

      if (`_get_pin_count(PORT_MEM_CAS_N_PINLOC) != 0) begin : mem_cas_n
         `_connect_out_with_regs(PORT_MEM_CAS_N_PINLOC, PORT_MEM_CAS_N_WIDTH, PORT_AFI_CAS_N_WIDTH, afi_cas_n, OE_ON_WIDTH, oe_on)
      end

      if (`_get_pin_count(PORT_MEM_WE_N_PINLOC) != 0) begin : mem_we_n
         `_connect_out_with_regs(PORT_MEM_WE_N_PINLOC, PORT_MEM_WE_N_WIDTH, PORT_AFI_WE_N_WIDTH, afi_we_n, OE_ON_WIDTH, oe_on)
      end

      if (`_get_pin_count(PORT_MEM_RESET_N_PINLOC) != 0) begin : mem_reset_n
         `_connect_out_with_regs(PORT_MEM_RESET_N_PINLOC, PORT_MEM_RESET_N_WIDTH, PORT_AFI_RST_N_WIDTH, afi_rst_n, OE_ON_WIDTH, oe_on)
      end
      
      if (`_get_pin_count(PORT_MEM_ACT_N_PINLOC) != 0) begin : mem_act_n
         `_connect_out_with_regs(PORT_MEM_ACT_N_PINLOC, PORT_MEM_ACT_N_WIDTH, PORT_AFI_ACT_N_WIDTH, afi_act_n, OE_ON_WIDTH, oe_on)
      end

      if (`_get_pin_count(PORT_MEM_PAR_PINLOC) != 0) begin : mem_par
         `_connect_out_with_regs(PORT_MEM_PAR_PINLOC, PORT_MEM_PAR_WIDTH, PORT_AFI_PAR_WIDTH, afi_par, OE_ON_WIDTH, oe_on)
      end

      if (`_get_pin_count(PORT_MEM_CA_PINLOC) != 0) begin : mem_ca
         `_connect_out_with_regs(PORT_MEM_CA_PINLOC, PORT_MEM_CA_WIDTH, PORT_AFI_CA_WIDTH, afi_ca, OE_ON_WIDTH, oe_on)
      end
      
      if (`_get_pin_count(PORT_MEM_REF_N_PINLOC) != 0) begin : mem_ref_n
         `_connect_out_with_regs(PORT_MEM_REF_N_PINLOC, PORT_MEM_REF_N_WIDTH, PORT_AFI_REF_N_WIDTH, afi_ref_n, OE_ON_WIDTH, oe_on)
      end
      
      if (`_get_pin_count(PORT_MEM_WPS_N_PINLOC) != 0) begin : mem_wps_n
         `_connect_out_with_regs(PORT_MEM_WPS_N_PINLOC, PORT_MEM_WPS_N_WIDTH, PORT_AFI_WPS_N_WIDTH, afi_wps_n, OE_ON_WIDTH, oe_on)
      end

      if (`_get_pin_count(PORT_MEM_RPS_N_PINLOC) != 0) begin : mem_rps_n
         `_connect_out_with_regs(PORT_MEM_RPS_N_PINLOC, PORT_MEM_RPS_N_WIDTH, PORT_AFI_RPS_N_WIDTH, afi_rps_n, OE_ON_WIDTH, oe_on)
      end

      if (`_get_pin_count(PORT_MEM_DOFF_N_PINLOC) != 0) begin : mem_doff_n
         `_connect_out_with_regs(PORT_MEM_DOFF_N_PINLOC, PORT_MEM_DOFF_N_WIDTH, PORT_AFI_DOFF_N_WIDTH, afi_doff_n, OE_ON_WIDTH, oe_on)
      end

      if (`_get_pin_count(PORT_MEM_LDA_N_PINLOC) != 0 && `_get_pin_count(PORT_MEM_LDB_N_PINLOC) != 0) begin : mem_ldab_n
         logic [PORT_AFI_LD_N_WIDTH/2-1:0] afi_lda_n, afi_ldb_n;
         assign afi_lda_n = afi_ld_n[0 +: PORT_AFI_LD_N_WIDTH / 2];
         assign afi_ldb_n = afi_ld_n[PORT_AFI_LD_N_WIDTH / 2 +: PORT_AFI_LD_N_WIDTH / 2];
         
         if (`_get_pin_count(PORT_MEM_LDA_N_PINLOC) != 0) begin : a
            `_connect_out_with_regs(PORT_MEM_LDA_N_PINLOC, PORT_MEM_LDA_N_WIDTH, (PORT_AFI_LD_N_WIDTH / 2), afi_lda_n, OE_ON_WIDTH, oe_on)
         end
         if (`_get_pin_count(PORT_MEM_LDB_N_PINLOC) != 0) begin : b
            `_connect_out_with_regs(PORT_MEM_LDB_N_PINLOC, PORT_MEM_LDB_N_WIDTH, (PORT_AFI_LD_N_WIDTH / 2), afi_ldb_n, OE_ON_WIDTH, oe_on)
         end
      end
      
      if (`_get_pin_count(PORT_MEM_RWA_N_PINLOC) != 0 && `_get_pin_count(PORT_MEM_RWB_N_PINLOC) != 0) begin : mem_rwab_n
         logic [PORT_AFI_RW_N_WIDTH/2-1:0] afi_rwa_n, afi_rwb_n;
         
         assign afi_rwa_n = afi_rw_n[0 +: PORT_AFI_RW_N_WIDTH / 2];
         assign afi_rwb_n = afi_rw_n[PORT_AFI_RW_N_WIDTH / 2 +: PORT_AFI_RW_N_WIDTH / 2];
         
         if (`_get_pin_count(PORT_MEM_RWA_N_PINLOC) != 0) begin : a
            `_connect_out_with_regs(PORT_MEM_RWA_N_PINLOC, PORT_MEM_RWA_N_WIDTH, (PORT_AFI_RW_N_WIDTH / 2), afi_rwa_n, OE_ON_WIDTH, oe_on)
         end 
         if (`_get_pin_count(PORT_MEM_RWB_N_PINLOC) != 0) begin : b
            `_connect_out_with_regs(PORT_MEM_RWB_N_PINLOC, PORT_MEM_RWB_N_WIDTH, (PORT_AFI_RW_N_WIDTH / 2), afi_rwb_n, OE_ON_WIDTH, oe_on)
         end
      end
      
      if (`_get_pin_count(PORT_MEM_LBK0_N_PINLOC) != 0) begin : mem_lbk0_n
         `_connect_out_with_regs(PORT_MEM_LBK0_N_PINLOC, PORT_MEM_LBK0_N_WIDTH, PORT_AFI_LBK0_N_WIDTH, afi_lbk0_n, OE_ON_WIDTH, oe_on)
      end

      if (`_get_pin_count(PORT_MEM_LBK1_N_PINLOC) != 0) begin : mem_lbk1_n
         `_connect_out_with_regs(PORT_MEM_LBK1_N_PINLOC, PORT_MEM_LBK1_N_WIDTH, PORT_AFI_LBK1_N_WIDTH, afi_lbk1_n, OE_ON_WIDTH, oe_on)
      end

      if (`_get_pin_count(PORT_MEM_CFG_N_PINLOC) != 0) begin : mem_cfg_n
         `_connect_out_with_regs(PORT_MEM_CFG_N_PINLOC, PORT_MEM_CFG_N_WIDTH, PORT_AFI_CFG_N_WIDTH, afi_cfg_n, OE_ON_WIDTH, oe_on)
      end

      if (`_get_pin_count(PORT_MEM_AP_PINLOC) != 0) begin : mem_ap
         `_connect_out_with_regs(PORT_MEM_AP_PINLOC, PORT_MEM_AP_WIDTH, PORT_AFI_AP_WIDTH, afi_ap, OE_ON_WIDTH, oe_on)
      end

      if (`_get_pin_count(PORT_MEM_AINV_PINLOC) != 0) begin : mem_ainv
         `_connect_out_with_regs(PORT_MEM_AINV_PINLOC, PORT_MEM_AINV_WIDTH, PORT_AFI_AINV_WIDTH, afi_ainv, OE_ON_WIDTH, oe_on)
      end
      
      if (`_get_pin_count(PORT_MEM_DM_PINLOC) != 0) begin : mem_dm
         `_connect_out_with_regs(PORT_MEM_DM_PINLOC, PORT_MEM_DM_WIDTH, PORT_AFI_DM_WIDTH, ~afi_dm, OE_ON_WIDTH, oe_on)
      end      

      if (`_get_pin_count(PORT_MEM_BWS_N_PINLOC) != 0) begin : mem_bws_n
         `_connect_out_with_regs(PORT_MEM_BWS_N_PINLOC, PORT_MEM_BWS_N_WIDTH, PORT_AFI_BWS_N_WIDTH, ~afi_bws_n, OE_ON_WIDTH, oe_on)
      end      
      
      if (`_get_pin_count(PORT_MEM_D_PINLOC) != 0) begin : mem_d
         `_connect_out_with_regs(PORT_MEM_D_PINLOC, PORT_MEM_D_WIDTH, PORT_AFI_WDATA_WIDTH, afi_wdata, OE_ON_WIDTH, oe_on)
      end
      
      ////////////////////////////////////////////////////////////////////////////
      // Connection for AFI signals that go to input-only pins
      ////////////////////////////////////////////////////////////////////////////
      if (`_get_pin_count(PORT_MEM_Q_PINLOC) != 0) begin : mem_q
         `_connect_in_with_regs(PORT_MEM_Q_PINLOC, PORT_MEM_Q_WIDTH, PORT_AFI_RDATA_WIDTH, afi_rdata)
         
         logic [PORT_MEM_Q_WIDTH-1:0] zeros;
         assign zeros = '0;
         // Switching OE on read pins as we switch OE_INVERT in the IP
         `_connect_out(PORT_MEM_Q_PINLOC, PORT_MEM_Q_WIDTH, PORT_MEM_Q_WIDTH, zeros, OE_ON_WIDTH, oe_on)
      end
      
      if (`_get_pin_count(PORT_MEM_ALERT_N_PINLOC) != 0) begin : mem_alert_n
         logic [PORT_MEM_ALERT_N_WIDTH-1:0] zeros;
         assign zeros = '0;
         `_connect_out(PORT_MEM_ALERT_N_PINLOC, PORT_MEM_ALERT_N_WIDTH, PORT_MEM_ALERT_N_WIDTH, zeros, OE_ON_WIDTH, oe_on)
      end
      
      if (`_get_pin_count(PORT_MEM_PE_N_PINLOC) != 0) begin : mem_pe_n
         logic [PORT_MEM_PE_N_WIDTH-1:0] zeros;
         assign zeros = '0;

         logic [OE_ON_WIDTH-1:0] oe_off;
         assign oe_off = '0;
         `_connect_out(PORT_MEM_PE_N_PINLOC, PORT_MEM_PE_N_WIDTH, PORT_MEM_PE_N_WIDTH, zeros, OE_ON_WIDTH, oe_off)
      end
      
      ////////////////////////////////////////////////////////////////////////////
      // Connection for AFI signals that go to bidirectional pins
      ////////////////////////////////////////////////////////////////////////////
      if (`_get_pin_count(PORT_MEM_DQ_PINLOC) != 0 || `_get_pin_count(PORT_MEM_DBI_N_PINLOC) != 0) begin : mem_sp_bidir_data
      
         // Replicate per-interface afi_wdata_valid to be per-group to help timing closure
         localparam PORT_AFI_WDATA_VALID_ALL_GRPS_WIDTH = PORT_AFI_WDATA_VALID_WIDTH * MEM_TTL_NUM_OF_WRITE_GROUPS;
         logic [PORT_AFI_WDATA_VALID_ALL_GRPS_WIDTH-1:0] afi_wdata_valid_all_grps_r;
         
         for (i = 0; i < MEM_TTL_NUM_OF_WRITE_GROUPS; ++i) begin : wgrp
            (* altera_attribute = {"-name MAX_FANOUT 1; -name ADV_NETLIST_OPT_ALLOWED ALWAYS_ALLOW"}*)
            altera_emif_arch_nf_regs # (
               .REGISTER (REGISTER_AFI),
               .WIDTH    (PORT_AFI_WDATA_VALID_WIDTH)
            ) afi_wdata_valid_regs (
               .clk      (afi_clk),
               .reset_n  (1'b1),
               .data_in  (afi_wdata_valid),
               .data_out (afi_wdata_valid_all_grps_r[i * PORT_AFI_WDATA_VALID_WIDTH +: PORT_AFI_WDATA_VALID_WIDTH])
            );      
         end
      
         if (`_get_pin_count(PORT_MEM_DQ_PINLOC) != 0) begin : mem_dq
            `_connect_out_with_regs(PORT_MEM_DQ_PINLOC, PORT_MEM_DQ_WIDTH, PORT_AFI_WDATA_WIDTH, afi_wdata, PORT_AFI_WDATA_VALID_ALL_GRPS_WIDTH, afi_wdata_valid_all_grps_r)
            `_connect_in_with_regs(PORT_MEM_DQ_PINLOC, PORT_MEM_DQ_WIDTH, PORT_AFI_RDATA_WIDTH, afi_rdata)
         end
         
         if (`_get_pin_count(PORT_MEM_DBI_N_PINLOC) != 0) begin : mem_dbi_n
            if (MEM_DATA_MASK_EN) begin : dm
               `_connect_out_with_regs(PORT_MEM_DBI_N_PINLOC, PORT_MEM_DBI_N_WIDTH, PORT_AFI_DM_N_WIDTH, afi_dm_n, PORT_AFI_WDATA_VALID_ALL_GRPS_WIDTH, afi_wdata_valid_all_grps_r)
            end else begin : wdbi
               logic [PORT_MEM_DBI_N_WIDTH-1:0] zeros;
               assign zeros = '0;
               `_connect_out(PORT_MEM_DBI_N_PINLOC, PORT_MEM_DBI_N_WIDTH, PORT_MEM_DBI_N_WIDTH, zeros, PORT_AFI_WDATA_VALID_ALL_GRPS_WIDTH, afi_wdata_valid_all_grps_r)
            end
         end
      end
      
      assign afi_rdata_dbi_n = '1;
      
      if ((`_get_pin_count(PORT_MEM_DQA_PINLOC) != 0 && `_get_pin_count(PORT_MEM_DQB_PINLOC) != 0) || (`_get_pin_count(PORT_MEM_DINVA_PINLOC) != 0 && `_get_pin_count(PORT_MEM_DINVB_PINLOC) != 0)) begin : mem_dp_bidir_data
      
         localparam MEM_NUM_OF_WRITE_GROUPS_PER_PORT = MEM_TTL_NUM_OF_WRITE_GROUPS / 2;
         localparam PORT_AFI_WDATA_VALID_PER_PORT_WIDTH = PORT_AFI_WDATA_VALID_WIDTH / 2;
         localparam PORT_AFI_WDATA_VALID_PER_PORT_ALL_GRPS_WIDTH = PORT_AFI_WDATA_VALID_PER_PORT_WIDTH * MEM_NUM_OF_WRITE_GROUPS_PER_PORT;

         logic [PORT_AFI_WDATA_VALID_PER_PORT_WIDTH-1:0] afi_wdata_valid_a, afi_wdata_valid_b;
         assign afi_wdata_valid_a = afi_wdata_valid[0 +: PORT_AFI_WDATA_VALID_PER_PORT_WIDTH];
         assign afi_wdata_valid_b = afi_wdata_valid[PORT_AFI_WDATA_VALID_PER_PORT_WIDTH +: PORT_AFI_WDATA_VALID_PER_PORT_WIDTH];
      
         // Replicate per-interface afi_wdata_valid to be per-group to help timing closure
         logic [PORT_AFI_WDATA_VALID_PER_PORT_ALL_GRPS_WIDTH-1:0] afi_wdata_valid_a_all_grps_r, afi_wdata_valid_b_all_grps_r;

         for (i = 0; i < MEM_NUM_OF_WRITE_GROUPS_PER_PORT; ++i) begin : wgrp
            (* altera_attribute = {"-name MAX_FANOUT 1; -name ADV_NETLIST_OPT_ALLOWED ALWAYS_ALLOW"}*)
            altera_emif_arch_nf_regs # (
               .REGISTER (REGISTER_AFI),
               .WIDTH    (PORT_AFI_WDATA_VALID_PER_PORT_WIDTH)
            ) afi_wdata_valid_a_regs (
               .clk      (afi_clk),
               .reset_n  (1'b1),
               .data_in  (afi_wdata_valid_a),
               .data_out (afi_wdata_valid_a_all_grps_r[i * PORT_AFI_WDATA_VALID_PER_PORT_WIDTH +: PORT_AFI_WDATA_VALID_PER_PORT_WIDTH])
            );      
            
            (* altera_attribute = {"-name MAX_FANOUT 1; -name ADV_NETLIST_OPT_ALLOWED ALWAYS_ALLOW"}*)
            altera_emif_arch_nf_regs # (
               .REGISTER (REGISTER_AFI),
               .WIDTH    (PORT_AFI_WDATA_VALID_PER_PORT_WIDTH)
            ) afi_wdata_valid_b_regs (
               .clk      (afi_clk),
               .reset_n  (1'b1),
               .data_in  (afi_wdata_valid_b),
               .data_out (afi_wdata_valid_b_all_grps_r[i * PORT_AFI_WDATA_VALID_PER_PORT_WIDTH +: PORT_AFI_WDATA_VALID_PER_PORT_WIDTH])
            );      
         end

         if (`_get_pin_count(PORT_MEM_DQA_PINLOC) != 0 && `_get_pin_count(PORT_MEM_DQB_PINLOC) != 0) begin : mem_dqab
         
            logic [PORT_AFI_RDATA_WIDTH/2-1:0]       afi_rdata_a      , afi_rdata_b;
            logic [PORT_AFI_WDATA_WIDTH/2-1:0]       afi_wdata_a      , afi_wdata_b;
            
            assign afi_rdata[0 +: PORT_AFI_RDATA_WIDTH / 2] = afi_rdata_a;
            assign afi_wdata_a = afi_wdata[0 +: PORT_AFI_WDATA_WIDTH / 2];
            
            assign afi_rdata[PORT_AFI_RDATA_WIDTH / 2 +: PORT_AFI_RDATA_WIDTH / 2] = afi_rdata_b;
            assign afi_wdata_b = afi_wdata[PORT_AFI_RDATA_WIDTH / 2 +: PORT_AFI_WDATA_WIDTH / 2];

            if (`_get_pin_count(PORT_MEM_DQA_PINLOC) != 0) begin : a
               `_connect_out_with_regs(PORT_MEM_DQA_PINLOC, PORT_MEM_DQA_WIDTH, (PORT_AFI_WDATA_WIDTH / 2), afi_wdata_a, PORT_AFI_WDATA_VALID_PER_PORT_ALL_GRPS_WIDTH, afi_wdata_valid_a_all_grps_r)
               `_connect_in_with_regs(PORT_MEM_DQA_PINLOC, PORT_MEM_DQA_WIDTH, (PORT_AFI_RDATA_WIDTH / 2), afi_rdata_a)
            end
            
            if (`_get_pin_count(PORT_MEM_DQB_PINLOC) != 0) begin : b
               `_connect_out_with_regs(PORT_MEM_DQB_PINLOC, PORT_MEM_DQB_WIDTH, (PORT_AFI_WDATA_WIDTH / 2), afi_wdata_b, PORT_AFI_WDATA_VALID_PER_PORT_ALL_GRPS_WIDTH, afi_wdata_valid_b_all_grps_r)
               `_connect_in_with_regs(PORT_MEM_DQB_PINLOC, PORT_MEM_DQB_WIDTH, (PORT_AFI_RDATA_WIDTH / 2), afi_rdata_b)
            end
         end
               
         if (`_get_pin_count(PORT_MEM_DINVA_PINLOC) != 0 && `_get_pin_count(PORT_MEM_DINVB_PINLOC) != 0) begin : mem_dinvab

            logic [PORT_AFI_RDATA_DINV_WIDTH/2-1:0]  afi_rdata_dinv_a , afi_rdata_dinv_b;
            logic [PORT_AFI_WDATA_DINV_WIDTH/2-1:0]  afi_wdata_dinv_a , afi_wdata_dinv_b;
            
            assign afi_rdata_dinv[0 +: PORT_AFI_RDATA_DINV_WIDTH / 2] = afi_rdata_dinv_a;
            assign afi_wdata_dinv_a = afi_wdata_dinv[0 +: PORT_AFI_RDATA_DINV_WIDTH / 2];
            
            assign afi_rdata_dinv[PORT_AFI_RDATA_DINV_WIDTH / 2 +: PORT_AFI_RDATA_DINV_WIDTH / 2] = afi_rdata_dinv_b;
            assign afi_wdata_dinv_b = afi_wdata_dinv[PORT_AFI_RDATA_DINV_WIDTH / 2 +: PORT_AFI_RDATA_DINV_WIDTH / 2];
            
            if (`_get_pin_count(PORT_MEM_DINVA_PINLOC) != 0) begin : a
               `_connect_out_with_regs(PORT_MEM_DINVA_PINLOC, PORT_MEM_DINVA_WIDTH, (PORT_AFI_WDATA_DINV_WIDTH / 2), afi_wdata_dinv_a, PORT_AFI_WDATA_VALID_PER_PORT_ALL_GRPS_WIDTH, afi_wdata_valid_a_all_grps_r)
               `_connect_in_with_regs(PORT_MEM_DINVA_PINLOC, PORT_MEM_DINVA_WIDTH, (PORT_AFI_RDATA_DINV_WIDTH / 2), afi_rdata_dinv_a)
            end
            
            if (`_get_pin_count(PORT_MEM_DINVA_PINLOC) != 0) begin : b
               `_connect_out_with_regs(PORT_MEM_DINVB_PINLOC, PORT_MEM_DINVB_WIDTH, (PORT_AFI_WDATA_DINV_WIDTH / 2), afi_wdata_dinv_b, PORT_AFI_WDATA_VALID_PER_PORT_ALL_GRPS_WIDTH, afi_wdata_valid_b_all_grps_r)
               `_connect_in_with_regs(PORT_MEM_DINVB_PINLOC, PORT_MEM_DINVB_WIDTH, (PORT_AFI_RDATA_DINV_WIDTH / 2), afi_rdata_dinv_b)
            end
         end else begin : no_mem_dinvab
            assign afi_rdata_dinv = '0;
         end
      end else begin : no_mem_dp_bidir_data
         assign afi_rdata_dinv = '0;
      end
      
      ////////////////////////////////////////////////////////////////////////////
      // Connection for AFI signals that go to bidir strobe pins
      ////////////////////////////////////////////////////////////////////////////
      if (`_get_pin_count(PORT_MEM_DQS_PINLOC) != 0 && `_get_pin_count(PORT_MEM_DQS_N_PINLOC) != 0) begin : mem_dqs_pair
         logic [(PORT_MEM_DQS_WIDTH * DDR_RATIO)-1:0] disable_dqs;
         assign disable_dqs = '0; 
         
         logic [PORT_AFI_DQS_BURST_WIDTH-1:0] afi_dqs_burst_r;
         
         (* altera_attribute = {"-name MAX_FANOUT 1; -name ADV_NETLIST_OPT_ALLOWED ALWAYS_ALLOW"}*) 
         altera_emif_arch_nf_regs # (
            .REGISTER (REGISTER_AFI),
            .WIDTH    (PORT_AFI_DQS_BURST_WIDTH)
         ) afi_dqs_burst_regs (
            .clk      (afi_clk),
            .reset_n  (1'b1),
            .data_in  (afi_dqs_burst),
            .data_out (afi_dqs_burst_r)
         );
      
         if (`_get_pin_count(PORT_MEM_DQS_PINLOC) != 0) begin : p
            `_connect_out_with_regs(PORT_MEM_DQS_PINLOC, PORT_MEM_DQS_WIDTH, (PORT_MEM_DQS_WIDTH * DDR_RATIO), disable_dqs, PORT_AFI_DQS_BURST_WIDTH, afi_dqs_burst_r)
         end      

         if (`_get_pin_count(PORT_MEM_DQS_N_PINLOC) != 0) begin : n
            `_connect_out_with_regs(PORT_MEM_DQS_N_PINLOC, PORT_MEM_DQS_N_WIDTH, (PORT_MEM_DQS_N_WIDTH * DDR_RATIO), disable_dqs, PORT_AFI_DQS_BURST_WIDTH, afi_dqs_burst_r)
         end
      end
      
      ////////////////////////////////////////////////////////////////////////////
      // Connection for AFI signals that go to output-only clock pins
      ////////////////////////////////////////////////////////////////////////////

      if (`_get_pin_count(PORT_MEM_CK_PINLOC) != 0 && `_get_pin_count(PORT_MEM_CK_N_PINLOC) != 0) begin : mem_ck_pair
         logic [(PORT_MEM_CK_WIDTH * DDR_RATIO)-1:0] disable_ck;
         assign disable_ck = '0; 
         
         if (`_get_pin_count(PORT_MEM_CK_PINLOC) != 0) begin : p
            `_connect_out_with_regs(PORT_MEM_CK_PINLOC, PORT_MEM_CK_WIDTH, (PORT_MEM_CK_WIDTH * DDR_RATIO), disable_ck, OE_ON_WIDTH, oe_on)
         end      

         if (`_get_pin_count(PORT_MEM_CK_N_PINLOC) != 0) begin : n
            `_connect_out_with_regs(PORT_MEM_CK_N_PINLOC, PORT_MEM_CK_N_WIDTH, (PORT_MEM_CK_N_WIDTH * DDR_RATIO), disable_ck, OE_ON_WIDTH, oe_on)
         end
      end
      
      if (`_get_pin_count(PORT_MEM_DK_PINLOC) != 0 && `_get_pin_count(PORT_MEM_DK_N_PINLOC) != 0) begin : mem_dk_pair
         logic [(PORT_MEM_DK_WIDTH * DDR_RATIO)-1:0] disable_dk;
         assign disable_dk = '0; 
      
         if (`_get_pin_count(PORT_MEM_DK_PINLOC) != 0) begin : p
            `_connect_out_with_regs(PORT_MEM_DK_PINLOC, PORT_MEM_DK_WIDTH, (PORT_MEM_DK_WIDTH * DDR_RATIO), disable_dk, OE_ON_WIDTH, oe_on)
         end      

         if (`_get_pin_count(PORT_MEM_DK_N_PINLOC) != 0) begin : n
            `_connect_out_with_regs(PORT_MEM_DK_N_PINLOC, PORT_MEM_DK_N_WIDTH, (PORT_MEM_DK_N_WIDTH * DDR_RATIO), disable_dk, OE_ON_WIDTH, oe_on)
         end
      end
      
      if (`_get_pin_count(PORT_MEM_DKA_PINLOC) != 0 && `_get_pin_count(PORT_MEM_DKA_N_PINLOC) != 0) begin : mem_dka_pair
         logic [(PORT_MEM_DKA_WIDTH * DDR_RATIO)-1:0] disable_dka;
         assign disable_dka = '0; 
      
         if (`_get_pin_count(PORT_MEM_DKA_PINLOC) != 0) begin : p
            `_connect_out_with_regs(PORT_MEM_DKA_PINLOC, PORT_MEM_DKA_WIDTH, (PORT_MEM_DKA_WIDTH * DDR_RATIO), disable_dka, OE_ON_WIDTH, oe_on)
         end      

         if (`_get_pin_count(PORT_MEM_DKA_N_PINLOC) != 0) begin : n
            `_connect_out_with_regs(PORT_MEM_DKA_N_PINLOC, PORT_MEM_DKA_N_WIDTH, (PORT_MEM_DKA_N_WIDTH * DDR_RATIO), disable_dka, OE_ON_WIDTH, oe_on)
         end
      end

      if (`_get_pin_count(PORT_MEM_DKB_PINLOC) != 0 && `_get_pin_count(PORT_MEM_DKB_N_PINLOC) != 0) begin : mem_dkb_pair
         logic [(PORT_MEM_DKB_WIDTH * DDR_RATIO)-1:0] disable_dkb;
         assign disable_dkb = '0; 
      
         if (`_get_pin_count(PORT_MEM_DKB_PINLOC) != 0) begin : p
            `_connect_out_with_regs(PORT_MEM_DKB_PINLOC, PORT_MEM_DKB_WIDTH, (PORT_MEM_DKB_WIDTH * DDR_RATIO), disable_dkb, OE_ON_WIDTH, oe_on)
         end      

         if (`_get_pin_count(PORT_MEM_DKB_N_PINLOC) != 0) begin : n
            `_connect_out_with_regs(PORT_MEM_DKB_N_PINLOC, PORT_MEM_DKB_N_WIDTH, (PORT_MEM_DKB_N_WIDTH * DDR_RATIO), disable_dkb, OE_ON_WIDTH, oe_on)
         end
      end
      
      if (`_get_pin_count(PORT_MEM_K_PINLOC) != 0 && `_get_pin_count(PORT_MEM_K_N_PINLOC) != 0) begin : mem_k_pair
         logic [(PORT_MEM_K_WIDTH * DDR_RATIO)-1:0] disable_k;
         assign disable_k = '0; 
      
         if (`_get_pin_count(PORT_MEM_K_PINLOC) != 0) begin : p
            `_connect_out_with_regs(PORT_MEM_K_PINLOC, PORT_MEM_K_WIDTH, (PORT_MEM_K_WIDTH * DDR_RATIO), disable_k, OE_ON_WIDTH, oe_on)
         end      

         if (`_get_pin_count(PORT_MEM_K_N_PINLOC) != 0) begin : n
            `_connect_out_with_regs(PORT_MEM_K_N_PINLOC, PORT_MEM_K_N_WIDTH, (PORT_MEM_K_N_WIDTH * DDR_RATIO), disable_k, OE_ON_WIDTH, oe_on)
         end
      end
      
      ////////////////////////////////////////////////////////////////////////////
      // Tie off core2l_data for unused connections
      //////////////////////////////////////////////////////////////////////////////
      for (pin_i = 0; pin_i < (NUM_OF_RTL_TILES * LANES_PER_TILE * PINS_PER_LANE); ++pin_i)
      begin : non_c2l_pin
         if (PINS_C2L_DRIVEN[pin_i] == 1'b0) begin
            assign `_unused_core2l_afi(pin_i) = '0;
            if (PINS_INVERT_OE[pin_i] == 1'b0) begin
               assign `_unused_core2l_oe(pin_i) = '1;
            end else begin    
               assign `_unused_core2l_oe(pin_i) = '0;
            end   
         end
         
         
      end		      
   endgenerate
endmodule
