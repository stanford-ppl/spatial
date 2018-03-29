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
// This module is responsible for exposing the Avalon interfaces through which
// soft logic interacts with the Hard Memory Controller inside the tile.
// The tile WYSIWYG blocks collapse the individual Avalon signals into big
// buses. This module re-wires the big buses into proper Avalon interfaces.
//
///////////////////////////////////////////////////////////////////////////////
module altera_emif_arch_nf_hmc_avl_if #(

   // Parameters describing HMC front-end ports
   parameter NUM_OF_HMC_PORTS                        = 1,
   parameter HMC_AVL_PROTOCOL_ENUM                   = "",
   
   // Parameters describing lanes/tiles
   parameter LANES_PER_TILE                          = 1,
   parameter NUM_OF_RTL_TILES                        = 1,
   parameter PRI_AC_TILE_INDEX                       = -1,
   parameter PRI_RDATA_TILE_INDEX                    = -1,
   parameter PRI_RDATA_LANE_INDEX                    = -1,
   parameter PRI_WDATA_TILE_INDEX                    = -1,
   parameter PRI_WDATA_LANE_INDEX                    = -1,
   parameter SEC_AC_TILE_INDEX                       = -1,
   parameter SEC_RDATA_TILE_INDEX                    = -1,
   parameter SEC_RDATA_LANE_INDEX                    = -1,
   parameter SEC_WDATA_TILE_INDEX                    = -1,
   parameter SEC_WDATA_LANE_INDEX                    = -1,
   parameter PRI_HMC_DBC_SHADOW_LANE_INDEX           = -1,

   // Definition of port widths for "ctrl_ast_cmd" interface (auto-generated)
   parameter PORT_CTRL_AST_CMD_DATA_WIDTH            = 1,

   // Definition of port widths for "ctrl_amm" interface (auto-generated)
   parameter PORT_CTRL_AMM_ADDRESS_WIDTH             = 1,
   parameter PORT_CTRL_AMM_BCOUNT_WIDTH              = 1

) (
   // Collapsed Avalon signals going into/out of tiles
   output logic [59:0]                                        core2ctl_avl_0,
   output                                                     core2ctl_avl_rd_data_ready_0,
   input  logic                                               ctl2core_avl_cmd_ready_0,
   output logic                                               core2l_wr_data_vld_ast_0,
   output logic                                               core2l_rd_data_rdy_ast_0,
   
   output logic [59:0]                                        core2ctl_avl_1,
   output                                                     core2ctl_avl_rd_data_ready_1,
   input  logic                                               ctl2core_avl_cmd_ready_1,
   output logic                                               core2l_wr_data_vld_ast_1,
   output logic                                               core2l_rd_data_rdy_ast_1,
      
   // Avalon interfaces between core and lanes
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0]    l2core_rd_data_vld_avl0,
   input  logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0]    l2core_wr_data_rdy_ast,
   
   // Ports for "ctrl_user_priority" interface
   input  logic                                               ctrl_user_priority_hi_0,
   input  logic                                               ctrl_user_priority_hi_1,
   
   // Controller auto-precharge request signals
   input  logic                                               ctrl_auto_precharge_req_0,
   input  logic                                               ctrl_auto_precharge_req_1,

   // Ports for "ctrl_ast_cmd" interfaces (auto-generated)
   output logic                                               ast_cmd_ready_0,
   input  logic                                               ast_cmd_valid_0,
   input  logic [PORT_CTRL_AST_CMD_DATA_WIDTH-1:0]            ast_cmd_data_0,

   output logic                                               ast_cmd_ready_1,
   input  logic                                               ast_cmd_valid_1,
   input  logic [PORT_CTRL_AST_CMD_DATA_WIDTH-1:0]            ast_cmd_data_1,

   // Ports for "ctrl_ast_wr" interfaces (auto-generated)
   output logic                                               ast_wr_ready_0,
   input  logic                                               ast_wr_valid_0,

   output logic                                               ast_wr_ready_1,
   input  logic                                               ast_wr_valid_1,

   // Ports for "ctrl_ast_rd" interfaces (auto-generated)
   input  logic                                               ast_rd_ready_0,
   output logic                                               ast_rd_valid_0,

   input  logic                                               ast_rd_ready_1,
   output logic                                               ast_rd_valid_1,

   // Ports for "ctrl_amm" interfaces (auto-generated)
   input  logic                                               amm_write_0,
   input  logic                                               amm_read_0,
   output logic                                               amm_ready_0,
   input  logic [PORT_CTRL_AMM_ADDRESS_WIDTH-1:0]             amm_address_0,
   input  logic [PORT_CTRL_AMM_BCOUNT_WIDTH-1:0]              amm_burstcount_0,
   input  logic                                               amm_beginbursttransfer_0,
   output logic                                               amm_readdatavalid_0,

   input  logic                                               amm_write_1,
   input  logic                                               amm_read_1,
   output logic                                               amm_ready_1,
   input  logic [PORT_CTRL_AMM_ADDRESS_WIDTH-1:0]             amm_address_1,
   input  logic [PORT_CTRL_AMM_BCOUNT_WIDTH-1:0]              amm_burstcount_1,
   input  logic                                               amm_beginbursttransfer_1,
   output logic                                               amm_readdatavalid_1
);
   timeunit 1ns;
   timeprecision 1ps;
   
      
   generate
   
      if (HMC_AVL_PROTOCOL_ENUM == "CTRL_AVL_PROTOCOL_MM") begin : amm
      
         logic [31:0] amm_address_padded_0;
         logic [31:0] amm_address_padded_1;
         logic [7:0]  amm_burstcount_padded_0;
         logic [7:0]  amm_burstcount_padded_1;
      
         if (PORT_CTRL_AMM_ADDRESS_WIDTH >= 32) begin
            assign amm_address_padded_0 = amm_address_0;
            assign amm_address_padded_1 = amm_address_1;
         end else begin
            assign amm_address_padded_0 = {'0, amm_address_0};
            assign amm_address_padded_1 = {'0, amm_address_1};
         end
         
         if (PORT_CTRL_AMM_BCOUNT_WIDTH >= 8) begin
            assign amm_burstcount_padded_0 = amm_burstcount_0;
            assign amm_burstcount_padded_1 = amm_burstcount_1;
         end else begin
            assign amm_burstcount_padded_0 = {'0, amm_burstcount_0};
            assign amm_burstcount_padded_1 = {'0, amm_burstcount_1};
         end

         // Port 0
         assign core2ctl_avl_0[0]            = amm_read_0; 
         assign core2ctl_avl_0[1]            = amm_write_0;
         assign core2ctl_avl_0[33:2]         = amm_address_padded_0;
         assign core2ctl_avl_0[41:34]        = amm_burstcount_padded_0;
         assign core2ctl_avl_0[42]           = ctrl_user_priority_hi_0;
         assign core2ctl_avl_0[43]           = ctrl_auto_precharge_req_0;
         assign core2ctl_avl_0[44]           = '0;  
         assign core2ctl_avl_0[57:45]        = '0;  
         assign core2ctl_avl_0[58]           = '0;  
         assign core2ctl_avl_0[59]           = '0;  
         assign amm_ready_0                  = ctl2core_avl_cmd_ready_0;
         
         assign amm_readdatavalid_0          = l2core_rd_data_vld_avl0[PRI_AC_TILE_INDEX][PRI_HMC_DBC_SHADOW_LANE_INDEX];
         
         assign ast_cmd_ready_0              = '0;
         assign ast_wr_ready_0               = '0;
         assign ast_rd_valid_0               = '0;
         assign core2ctl_avl_rd_data_ready_0 = '1;
         assign core2l_wr_data_vld_ast_0     = '0;
         assign core2l_rd_data_rdy_ast_0     = '1;
         
         // Port 1
         assign core2ctl_avl_1[0]            = amm_read_1; 
         assign core2ctl_avl_1[1]            = amm_write_1;
         assign core2ctl_avl_1[33:2]         = amm_address_padded_1;
         assign core2ctl_avl_1[41:34]        = amm_burstcount_padded_1;
         assign core2ctl_avl_1[42]           = ctrl_user_priority_hi_1;
         assign core2ctl_avl_1[43]           = ctrl_auto_precharge_req_1;
         assign core2ctl_avl_1[44]           = '0;  
         assign core2ctl_avl_1[57:45]        = '0;  
         assign core2ctl_avl_1[58]           = '0;  
         assign core2ctl_avl_1[59]           = '0;  
         assign amm_ready_1                  = ctl2core_avl_cmd_ready_1;
         assign amm_readdatavalid_1          = l2core_rd_data_vld_avl0[SEC_RDATA_TILE_INDEX][SEC_RDATA_LANE_INDEX];
         assign ast_cmd_ready_1              = '0;
         assign ast_wr_ready_1               = '0;
         assign ast_rd_valid_1               = '0;
         assign core2ctl_avl_rd_data_ready_1 = '1;         
         assign core2l_wr_data_vld_ast_1     = '0;
         assign core2l_rd_data_rdy_ast_1     = '1;
         
      end else if (HMC_AVL_PROTOCOL_ENUM == "CTRL_AVL_PROTOCOL_ST") begin : ast
      
         // Port 0
         assign core2ctl_avl_0[57:0]         = ast_cmd_data_0;
         assign core2ctl_avl_0[58]           = ast_cmd_valid_0;
         assign core2ctl_avl_0[59]           = ast_wr_valid_0;
         assign ast_cmd_ready_0              = ctl2core_avl_cmd_ready_0;
         
         assign ast_wr_ready_0               = l2core_wr_data_rdy_ast[PRI_AC_TILE_INDEX][PRI_HMC_DBC_SHADOW_LANE_INDEX];
         assign ast_rd_valid_0               = l2core_rd_data_vld_avl0[PRI_AC_TILE_INDEX][PRI_HMC_DBC_SHADOW_LANE_INDEX];
         
         assign core2ctl_avl_rd_data_ready_0 = ast_rd_ready_0;
         assign amm_ready_0                  = '0;
         assign amm_readdatavalid_0          = '0;
         assign core2l_wr_data_vld_ast_0     = ast_wr_valid_0;
         assign core2l_rd_data_rdy_ast_0     = ast_rd_ready_0;
         
         // Port 1
         assign core2ctl_avl_1[57:0]         = ast_cmd_data_1;
         assign core2ctl_avl_1[58]           = ast_cmd_valid_1;
         assign core2ctl_avl_1[59]           = ast_wr_valid_1;
         assign ast_cmd_ready_1              = ctl2core_avl_cmd_ready_1;
         assign ast_wr_ready_1               = l2core_wr_data_rdy_ast[SEC_WDATA_TILE_INDEX][SEC_WDATA_LANE_INDEX];
         assign ast_rd_valid_1               = l2core_rd_data_vld_avl0[SEC_RDATA_TILE_INDEX][SEC_RDATA_LANE_INDEX];
         assign core2ctl_avl_rd_data_ready_1 = ast_rd_ready_1;
         assign amm_ready_1                  = '0;
         assign amm_readdatavalid_1          = '0;         
         assign core2l_wr_data_vld_ast_1     = ast_wr_valid_1;
         assign core2l_rd_data_rdy_ast_1     = ast_rd_ready_1;
         
      end else begin : no_hmc
      
         // Port 0
         assign core2ctl_avl_0               = '0;
         assign ast_cmd_ready_0              = '0;
         assign ast_wr_ready_0               = '0;
         assign ast_rd_valid_0               = '0;
         assign core2ctl_avl_rd_data_ready_0 = '1;
         assign amm_ready_0                  = '0;
         assign amm_readdatavalid_0          = '0;
         assign core2l_wr_data_vld_ast_0     = '0;
         assign core2l_rd_data_rdy_ast_0     = '1;
         
         // Port 1
         assign core2ctl_avl_1               = '0;
         assign ast_cmd_ready_1              = '0;
         assign ast_wr_ready_1               = '0;
         assign ast_rd_valid_1               = '0;
         assign core2ctl_avl_rd_data_ready_1 = '1;
         assign amm_ready_1                  = '0;
         assign amm_readdatavalid_1          = '0;
         assign core2l_wr_data_vld_ast_1     = '0;
         assign core2l_rd_data_rdy_ast_1     = '1;
      end
   endgenerate  
endmodule

