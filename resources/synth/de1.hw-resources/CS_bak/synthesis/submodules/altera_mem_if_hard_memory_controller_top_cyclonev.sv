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

(* altera_attribute = "-name FITTER_ADJUST_HC_SHORT_PATH_GUARDBAND 100" *)
module altera_mem_if_hard_memory_controller_top_cyclonev (
    afi_clk,
    afi_half_clk,
    ctl_clk,
    mp_cmd_clk_0,
    mp_cmd_clk_1,
    mp_cmd_clk_2,
    mp_cmd_clk_3,
    mp_cmd_clk_4,
    mp_cmd_clk_5,
    mp_cmd_reset_n_0,
    mp_cmd_reset_n_1,
    mp_cmd_reset_n_2,
    mp_cmd_reset_n_3,
    mp_cmd_reset_n_4,
    mp_cmd_reset_n_5,
    mp_rfifo_clk_0,
    mp_rfifo_clk_1,
    mp_rfifo_clk_2,
    mp_rfifo_clk_3,
    mp_rfifo_reset_n_0,
    mp_rfifo_reset_n_1,
    mp_rfifo_reset_n_2,
    mp_rfifo_reset_n_3,
    mp_wfifo_clk_0,
    mp_wfifo_clk_1,
    mp_wfifo_clk_2,
    mp_wfifo_clk_3,
    mp_wfifo_reset_n_0,
    mp_wfifo_reset_n_1,
    mp_wfifo_reset_n_2,
    mp_wfifo_reset_n_3,
    csr_clk,
    csr_reset_n,
    afi_reset_n,
    ctl_reset_n,
    avl_ready_0,
    avl_write_req_0,
    avl_read_req_0,
    avl_addr_0,
    avl_be_0,
    avl_wdata_0,
    avl_size_0,
    avl_burstbegin_0,
    avl_rdata_0,
    avl_rdata_valid_0,
    avl_ready_1,
    avl_write_req_1,
    avl_read_req_1,
    avl_addr_1,
    avl_be_1,
    avl_wdata_1,
    avl_size_1,
    avl_burstbegin_1,
    avl_rdata_1,
    avl_rdata_valid_1,
    avl_ready_2,
    avl_write_req_2,
    avl_read_req_2,
    avl_addr_2,
    avl_be_2,
    avl_wdata_2,
    avl_size_2,
    avl_burstbegin_2,
    avl_rdata_2,
    avl_rdata_valid_2,
    avl_ready_3,
    avl_write_req_3,
    avl_read_req_3,
    avl_addr_3,
    avl_be_3,
    avl_wdata_3,
    avl_size_3,
    avl_burstbegin_3,
    avl_rdata_3,
    avl_rdata_valid_3,
    avl_ready_4,
    avl_write_req_4,
    avl_read_req_4,
    avl_addr_4,
    avl_be_4,
    avl_wdata_4,
    avl_size_4,
    avl_burstbegin_4,
    avl_rdata_4,
    avl_rdata_valid_4,
    avl_ready_5,
    avl_write_req_5,
    avl_read_req_5,
    avl_addr_5,
    avl_be_5,
    avl_wdata_5,
    avl_size_5,
    avl_burstbegin_5,
    avl_rdata_5,
    avl_rdata_valid_5,
    afi_rst_n,
    afi_cs_n,
    afi_cke,
    afi_odt,
    afi_addr,
    afi_ba,
    afi_ras_n,
    afi_cas_n,
    afi_we_n,
    afi_dqs_burst,
    afi_wdata_valid,
    afi_wdata,
    afi_dm,
    afi_wlat,
    afi_rdata_en,
    afi_rdata_en_full,
    afi_rdata,
    afi_rdata_valid,
    afi_rlat,
    afi_cal_success,
    afi_mem_clk_disable,
    afi_ctl_refresh_done,
    afi_seq_busy,
    afi_ctl_long_idle,
    afi_cal_fail,
    afi_cal_req,
    afi_init_req,
    cfg_dramconfig,
    cfg_caswrlat,
    cfg_addlat,
    cfg_tcl,
    cfg_trfc,
    cfg_trefi,
    cfg_twr,
    cfg_tmrd,
    cfg_coladdrwidth,
    cfg_rowaddrwidth,
    cfg_bankaddrwidth,
    cfg_csaddrwidth,
    cfg_interfacewidth,
    cfg_devicewidth,
    local_refresh_ack,
    local_powerdn_ack,
    local_self_rfsh_ack,
    local_deep_powerdn_ack,
    local_refresh_req,
    local_refresh_chip,
    local_self_rfsh_req,
    local_self_rfsh_chip,
    local_deep_powerdn_req,
    local_deep_powerdn_chip,
    local_multicast,
    local_priority,
    local_init_done,
    local_cal_success,
    local_cal_fail,
    csr_read_req,
    csr_write_req,
    csr_addr,
    csr_wdata,
    csr_rdata,
    csr_be,
    csr_rdata_valid,
    csr_waitrequest,
    bonding_out_1,
    bonding_in_1,
    bonding_out_2,
    bonding_in_2,
    bonding_out_3,
    bonding_in_3,
    io_intaficalfail,
    ctl_init_req,
    local_sts_ctl_empty,
    io_intaficalsuccess
);

//////////////////////////////////////////////////////////////////////////////
// BEGIN PARAMETER SECTION

// Existing SIP parameters
parameter   AVL_SIZE_WIDTH                                                      = 0;
parameter   AVL_ADDR_WIDTH                                                      = 0;
parameter   AVL_DATA_WIDTH                                                      = 0;
parameter   MEM_IF_CLK_PAIR_COUNT                                               = 0;
parameter   MEM_IF_CS_WIDTH                                                     = 0;
parameter   MEM_IF_DQS_WIDTH                                                    = 0;
parameter   MEM_IF_CHIP_BITS                                                    = 0;
parameter   AFI_ADDR_WIDTH                                                      = 0;
parameter   AFI_BANKADDR_WIDTH                                                  = 0;
parameter   AFI_CONTROL_WIDTH                                                   = 0;
parameter   AFI_CS_WIDTH                                                        = 0;
parameter   AFI_ODT_WIDTH                                                       = 0;
parameter   AFI_DM_WIDTH                                                        = 0;
parameter   AFI_DQ_WIDTH                                                        = 0;
parameter   AFI_WRITE_DQS_WIDTH                                                 = 0;
parameter   AFI_RATE_RATIO                                                      = 0;
parameter   AFI_WLAT_WIDTH                                                      = 0;
parameter   AFI_RLAT_WIDTH                                                      = 0;
parameter   CSR_BE_WIDTH                                                        = 0;
parameter   CSR_ADDR_WIDTH                                                      = 0;
parameter   CSR_DATA_WIDTH                                                      = 0;

// New parameters for HMC
parameter   AVL_DATA_WIDTH_PORT_0                                               = 0;
parameter   AVL_DATA_WIDTH_PORT_1                                               = 0;
parameter   AVL_DATA_WIDTH_PORT_2                                               = 0;
parameter   AVL_DATA_WIDTH_PORT_3                                               = 0;
parameter   AVL_DATA_WIDTH_PORT_4                                               = 0;
parameter   AVL_DATA_WIDTH_PORT_5                                               = 0;
parameter   AVL_ADDR_WIDTH_PORT_0                                               = 0;
parameter   AVL_ADDR_WIDTH_PORT_1                                               = 0;
parameter   AVL_ADDR_WIDTH_PORT_2                                               = 0;
parameter   AVL_ADDR_WIDTH_PORT_3                                               = 0;
parameter   AVL_ADDR_WIDTH_PORT_4                                               = 0;
parameter   AVL_ADDR_WIDTH_PORT_5                                               = 0;
parameter   AVL_NUM_SYMBOLS_PORT_0                                              = 0;
parameter   AVL_NUM_SYMBOLS_PORT_1                                              = 0;
parameter   AVL_NUM_SYMBOLS_PORT_2                                              = 0;
parameter   AVL_NUM_SYMBOLS_PORT_3                                              = 0;
parameter   AVL_NUM_SYMBOLS_PORT_4                                              = 0;
parameter   AVL_NUM_SYMBOLS_PORT_5                                              = 0;
parameter   LSB_WFIFO_PORT_0                                                    = 5;
parameter   MSB_WFIFO_PORT_0                                                    = 5;
parameter   LSB_RFIFO_PORT_0                                                    = 5;
parameter   MSB_RFIFO_PORT_0                                                    = 5;
parameter   LSB_WFIFO_PORT_1                                                    = 5;
parameter   MSB_WFIFO_PORT_1                                                    = 5;
parameter   LSB_RFIFO_PORT_1                                                    = 5;
parameter   MSB_RFIFO_PORT_1                                                    = 5;
parameter   LSB_WFIFO_PORT_2                                                    = 5;
parameter   MSB_WFIFO_PORT_2                                                    = 5;
parameter   LSB_RFIFO_PORT_2                                                    = 5;
parameter   MSB_RFIFO_PORT_2                                                    = 5;
parameter   LSB_WFIFO_PORT_3                                                    = 5;
parameter   MSB_WFIFO_PORT_3                                                    = 5;
parameter   LSB_RFIFO_PORT_3                                                    = 5;
parameter   MSB_RFIFO_PORT_3                                                    = 5;
parameter   LSB_WFIFO_PORT_4                                                    = 5;
parameter   MSB_WFIFO_PORT_4                                                    = 5;
parameter   LSB_RFIFO_PORT_4                                                    = 5;
parameter   MSB_RFIFO_PORT_4                                                    = 5;
parameter   LSB_WFIFO_PORT_5                                                    = 5;
parameter   MSB_WFIFO_PORT_5                                                    = 5;
parameter   LSB_RFIFO_PORT_5                                                    = 5;
parameter   MSB_RFIFO_PORT_5                                                    = 5;
parameter   HARD_PHY                                                            = 0;

// Atom defparam
// Those that mark with // SYTH & SIM is used to force MMR signals in simulation
// Those that mark with // SYTH ONLY is only used for Quartus sythesis
parameter   ENUM_ATTR_COUNTER_ONE_RESET                                         = "DISABLED";                                                           //SYTH & SIM
parameter   ENUM_ATTR_COUNTER_ZERO_RESET                                        = "DISABLED";                                                           //SYTH & SIM
parameter   ENUM_ATTR_STATIC_CONFIG_VALID                                       = "DISABLED";                                                           //SYTH & SIM
parameter   ENUM_AUTO_PCH_ENABLE_0                                              = "DISABLED";                                                           //SYTH & SIM
parameter   ENUM_AUTO_PCH_ENABLE_1                                              = "DISABLED";                                                           //SYTH & SIM
parameter   ENUM_AUTO_PCH_ENABLE_2                                              = "DISABLED";                                                           //SYTH & SIM
parameter   ENUM_AUTO_PCH_ENABLE_3                                              = "DISABLED";                                                           //SYTH & SIM
parameter   ENUM_AUTO_PCH_ENABLE_4                                              = "DISABLED";                                                           //SYTH & SIM
parameter   ENUM_AUTO_PCH_ENABLE_5                                              = "DISABLED";                                                           //SYTH & SIM
parameter   ENUM_CAL_REQ                                                        = "DISABLED";                                                           //SYTH & SIM
parameter   ENUM_CFG_BURST_LENGTH                                               = "BL_8";                                                               //SYTH & SIM
parameter   ENUM_CFG_INTERFACE_WIDTH                                            = "DWIDTH_32";                                                          //SYTH & SIM
parameter   ENUM_CFG_SELF_RFSH_EXIT_CYCLES                                      = "SELF_RFSH_EXIT_CYCLES_512";                                          //SYTH & SIM
parameter   ENUM_CFG_STARVE_LIMIT                                               = "STARVE_LIMIT_32";                                                    //SYTH & SIM
parameter   ENUM_CFG_TYPE                                                       = "DDR3";                                                               //SYTH & SIM
parameter   ENUM_CLOCK_OFF_0                                                    = "DISABLED";                                                           //SIM ONLY
parameter   ENUM_CLOCK_OFF_1                                                    = "DISABLED";                                                           //SIM ONLY
parameter   ENUM_CLOCK_OFF_2                                                    = "DISABLED";                                                           //SIM ONLY
parameter   ENUM_CLOCK_OFF_3                                                    = "DISABLED";                                                           //SIM ONLY
parameter   ENUM_CLOCK_OFF_4                                                    = "DISABLED";                                                           //SIM ONLY
parameter   ENUM_CLOCK_OFF_5                                                    = "DISABLED";                                                           //SIM ONLY
parameter   ENUM_CLR_INTR                                                       = "NO_CLR_INTR";                                                        //SIM ONLY
parameter   ENUM_CMD_PORT_IN_USE_0                                              = "FALSE";                                                              //SYTH ONLY
parameter   ENUM_CMD_PORT_IN_USE_1                                              = "FALSE";                                                              //SYTH ONLY
parameter   ENUM_CMD_PORT_IN_USE_2                                              = "FALSE";                                                              //SYTH ONLY
parameter   ENUM_CMD_PORT_IN_USE_3                                              = "FALSE";                                                              //SYTH ONLY
parameter   ENUM_CMD_PORT_IN_USE_4                                              = "FALSE";                                                              //SYTH ONLY
parameter   ENUM_CMD_PORT_IN_USE_5                                              = "FALSE";                                                              //SYTH ONLY
parameter   ENUM_CPORT0_RDY_ALMOST_FULL                                         = "NOT_FULL";                                                           //SYTH & SIM
parameter   ENUM_CPORT0_RFIFO_MAP                                               = "FIFO_0";                                                             //SYTH & SIM
parameter   ENUM_CPORT0_TYPE                                                    = "DISABLE";                                                            //SYTH & SIM
parameter   ENUM_CPORT0_WFIFO_MAP                                               = "FIFO_0";                                                             //SYTH & SIM
parameter   ENUM_CPORT1_RDY_ALMOST_FULL                                         = "NOT_FULL";                                                           //SYTH & SIM
parameter   ENUM_CPORT1_RFIFO_MAP                                               = "FIFO_0";                                                             //SYTH & SIM
parameter   ENUM_CPORT1_TYPE                                                    = "DISABLE";                                                            //SYTH & SIM
parameter   ENUM_CPORT1_WFIFO_MAP                                               = "FIFO_0";                                                             //SYTH & SIM
parameter   ENUM_CPORT2_RDY_ALMOST_FULL                                         = "NOT_FULL";                                                           //SYTH & SIM
parameter   ENUM_CPORT2_RFIFO_MAP                                               = "FIFO_0";                                                             //SYTH & SIM
parameter   ENUM_CPORT2_TYPE                                                    = "DISABLE";                                                            //SYTH & SIM
parameter   ENUM_CPORT2_WFIFO_MAP                                               = "FIFO_0";                                                             //SYTH & SIM
parameter   ENUM_CPORT3_RDY_ALMOST_FULL                                         = "NOT_FULL";                                                           //SYTH & SIM
parameter   ENUM_CPORT3_RFIFO_MAP                                               = "FIFO_0";                                                             //SYTH & SIM
parameter   ENUM_CPORT3_TYPE                                                    = "DISABLE";                                                            //SYTH & SIM
parameter   ENUM_CPORT3_WFIFO_MAP                                               = "FIFO_0";                                                             //SYTH & SIM
parameter   ENUM_CPORT4_RDY_ALMOST_FULL                                         = "NOT_FULL";                                                           //SYTH & SIM
parameter   ENUM_CPORT4_RFIFO_MAP                                               = "FIFO_0";                                                             //SYTH & SIM
parameter   ENUM_CPORT4_TYPE                                                    = "DISABLE";                                                            //SYTH & SIM
parameter   ENUM_CPORT4_WFIFO_MAP                                               = "FIFO_0";                                                             //SYTH & SIM
parameter   ENUM_CPORT5_RDY_ALMOST_FULL                                         = "NOT_FULL";                                                           //SYTH & SIM
parameter   ENUM_CPORT5_RFIFO_MAP                                               = "FIFO_0";                                                             //SYTH & SIM
parameter   ENUM_CPORT5_TYPE                                                    = "DISABLE";                                                            //SYTH & SIM
parameter   ENUM_CPORT5_WFIFO_MAP                                               = "FIFO_0";                                                             //SYTH & SIM
parameter   ENUM_CTL_ADDR_ORDER                                                 = "CHIP_BANK_ROW_COL";                                                  //SYTH & SIM
parameter   ENUM_CTL_ECC_ENABLED                                                = "CTL_ECC_DISABLED";                                                   //SYTH & SIM
parameter   ENUM_CTL_ECC_RMW_ENABLED                                            = "CTL_ECC_RMW_DISABLED";                                               //SYTH & SIM
parameter   ENUM_CTL_REGDIMM_ENABLED                                            = "REGDIMM_DISABLED";           	                                //SIM ONLY
parameter   ENUM_CTL_USR_REFRESH                                                = "CTL_USR_REFRESH_DISABLED";                                           //SYTH & SIM
parameter   ENUM_CTRL_WIDTH                                                     = "DATA_WIDTH_64_BIT";                                                  //SYTH & SIM
parameter   ENUM_DELAY_BONDING                                                  = "BONDING_LATENCY_0";                                                  //SYTH & SIM
parameter   ENUM_DFX_BYPASS_ENABLE                                              = "DFX_BYPASS_DISABLED";                                                //SYTH & SIM
parameter   ENUM_DISABLE_MERGING                                                = "MERGING_ENABLED";                                                    //SIM ONLY
parameter   ENUM_ECC_DQ_WIDTH                                                   = "ECC_DQ_WIDTH_0";                                                     //SYTH ONLY
parameter   ENUM_ENABLE_ATPG                                                    = "DISABLED";                                                           //SYTH & SIM
parameter   ENUM_ENABLE_BONDING_0                                               = "DISABLED";                                                           //SYTH & SIM
parameter   ENUM_ENABLE_BONDING_1                                               = "DISABLED";                                                           //SYTH & SIM
parameter   ENUM_ENABLE_BONDING_2                                               = "DISABLED";                                                           //SYTH & SIM
parameter   ENUM_ENABLE_BONDING_3                                               = "DISABLED";                                                           //SYTH & SIM
parameter   ENUM_ENABLE_BONDING_4                                               = "DISABLED";                                                           //SYTH & SIM
parameter   ENUM_ENABLE_BONDING_5                                               = "DISABLED";                                                           //SYTH & SIM
parameter   ENUM_ENABLE_BONDING_WRAPBACK                                        = "DISABLED";                                                           //SYTH & SIM
parameter   ENUM_ENABLE_DQS_TRACKING                                            = "DISABLED";                                                           //SYTH & SIM
parameter   ENUM_ENABLE_ECC_CODE_OVERWRITES                                     = "DISABLED";                                                           //SYTH & SIM
parameter   ENUM_ENABLE_FAST_EXIT_PPD                                           = "DISABLED";                                                           //SYTH ONLY
parameter   ENUM_ENABLE_INTR                                                    = "DISABLED";                                                           //SYTH & SIM
parameter   ENUM_ENABLE_NO_DM                                                   = "DISABLED";                                                           //SYTH & SIM
parameter   ENUM_ENABLE_PIPELINEGLOBAL                                          = "DISABLED";                                                           //SYTH & SIM
parameter   ENUM_GANGED_ARF                                                     = "DISABLED";                                                           //SIM ONLY
parameter   ENUM_GEN_DBE                                                        = "GEN_DBE_DISABLED";     	                                                //SIM ONLY
parameter   ENUM_GEN_SBE                                                        = "GEN_SBE_DISABLED";            	                                        //SIM ONLY
parameter   ENUM_INC_SYNC                                                       = "FIFO_SET_2";                                                         //SYTH & SIM
parameter   ENUM_LOCAL_IF_CS_WIDTH                                              = "ADDR_WIDTH_2";                                                       //SYTH & SIM
parameter   ENUM_MASK_CORR_DROPPED_INTR                                         = "DISABLED";                                                           //SYTH & SIM
parameter   ENUM_MASK_DBE_INTR                                                  = "DISABLED";                                                           //SYTH & SIM
parameter   ENUM_MASK_SBE_INTR                                                  = "DISABLED";                                                           //SYTH & SIM
parameter   ENUM_MEM_IF_AL                                                      = "AL_0";                                                               //SYTH & SIM
parameter   ENUM_MEM_IF_BANKADDR_WIDTH                                          = "ADDR_WIDTH_3";                                                       //SYTH & SIM
parameter   ENUM_MEM_IF_BURSTLENGTH                                             = "MEM_IF_BURSTLENGTH_8";                                               //SYTH ONLY
parameter   ENUM_MEM_IF_COLADDR_WIDTH                                           = "ADDR_WIDTH_12";                                                      //SYTH & SIM
parameter   ENUM_MEM_IF_CS_PER_RANK                                             = "MEM_IF_CS_PER_RANK_1";                                               //SYTH ONLY
parameter   ENUM_MEM_IF_CS_WIDTH                                                = "MEM_IF_CS_WIDTH_1";                                                  //SYTH ONLY
parameter   ENUM_MEM_IF_DQ_PER_CHIP                                             = "MEM_IF_DQ_PER_CHIP_8";                                               //SYTH ONLY
parameter   ENUM_MEM_IF_DQS_WIDTH                                               = "DQS_WIDTH_4";                                                        //SYTH & SIM
parameter   ENUM_MEM_IF_DWIDTH                                                  = "MEM_IF_DWIDTH_32";                                                   //SYTH ONLY
parameter   ENUM_MEM_IF_MEMTYPE                                                 = "DDR3_SDRAM";                                                         //SYTH ONLY
parameter   ENUM_MEM_IF_ROWADDR_WIDTH                                           = "ADDR_WIDTH_16";                                                      //SYTH & SIM
parameter   ENUM_MEM_IF_SPEEDBIN                                                = "DDR3_1066_6_6_6";                                                    //SYTH ONLY
parameter   ENUM_MEM_IF_TCCD                                                    = "TCCD_4";                                                             //SYTH & SIM
parameter   ENUM_MEM_IF_TCL                                                     = "TCL_6";                                                              //SYTH & SIM
parameter   ENUM_MEM_IF_TCWL                                                    = "TCWL_5";                                                             //SYTH & SIM
parameter   ENUM_MEM_IF_TFAW                                                    = "TFAW_16";                                                            //SYTH & SIM
parameter   ENUM_MEM_IF_TMRD                                                    = "TMRD_4";                                                             //SYTH & SIM
parameter   ENUM_MEM_IF_TRAS                                                    = "TRAS_16";                                                            //SYTH & SIM
parameter   ENUM_MEM_IF_TRC                                                     = "TRC_22";                                                             //SYTH & SIM
parameter   ENUM_MEM_IF_TRCD                                                    = "TRCD_6";                                                             //SYTH & SIM
parameter   ENUM_MEM_IF_TRP                                                     = "TRP_6";                                                              //SYTH & SIM
parameter   ENUM_MEM_IF_TRRD                                                    = "TRRD_4";                                                             //SYTH & SIM
parameter   ENUM_MEM_IF_TRTP                                                    = "TRTP_4";                                                             //SYTH & SIM
parameter   ENUM_MEM_IF_TWR                                                     = "TWR_6";                                                              //SYTH & SIM
parameter   ENUM_MEM_IF_TWTR                                                    = "TWTR_4";                                                             //SYTH & SIM
parameter   ENUM_MMR_CFG_MEM_BL                                                 = "MP_BL_8";                                                            //SYTH & SIM
parameter   ENUM_OUTPUT_REGD                                                    = "DISABLED";                                                           //SYTH & SIM
parameter   ENUM_PDN_EXIT_CYCLES                                                = "SLOW_EXIT";                                                          //SYTH & SIM
parameter   ENUM_PORT0_WIDTH                                                    = "PORT_64_BIT";                                                        //SYTH & SIM
parameter   ENUM_PORT1_WIDTH                                                    = "PORT_64_BIT";                                                        //SYTH & SIM
parameter   ENUM_PORT2_WIDTH                                                    = "PORT_64_BIT";                                                        //SYTH & SIM
parameter   ENUM_PORT3_WIDTH                                                    = "PORT_64_BIT";                                                        //SYTH & SIM
parameter   ENUM_PORT4_WIDTH                                                    = "PORT_64_BIT";                                                        //SYTH & SIM
parameter   ENUM_PORT5_WIDTH                                                    = "PORT_64_BIT";                                                        //SYTH & SIM
parameter   ENUM_PRIORITY_0_0                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_0_1                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_0_2                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_0_3                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_0_4                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_0_5                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_1_0                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_1_1                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_1_2                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_1_3                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_1_4                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_1_5                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_2_0                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_2_1                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_2_2                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_2_3                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_2_4                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_2_5                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_3_0                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_3_1                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_3_2                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_3_3                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_3_4                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_3_5                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_4_0                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_4_1                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_4_2                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_4_3                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_4_4                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_4_5                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_5_0                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_5_1                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_5_2                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_5_3                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_5_4                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_5_5                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_6_0                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_6_1                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_6_2                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_6_3                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_6_4                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_6_5                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_7_0                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_7_1                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_7_2                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_7_3                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_7_4                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_PRIORITY_7_5                                                   = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_RCFG_STATIC_WEIGHT_0                                           = "WEIGHT_0";                                                           //SYTH & SIM
parameter   ENUM_RCFG_STATIC_WEIGHT_1                                           = "WEIGHT_0";                                                           //SYTH & SIM
parameter   ENUM_RCFG_STATIC_WEIGHT_2                                           = "WEIGHT_0";                                                           //SYTH & SIM
parameter   ENUM_RCFG_STATIC_WEIGHT_3                                           = "WEIGHT_0";                                                           //SYTH & SIM
parameter   ENUM_RCFG_STATIC_WEIGHT_4                                           = "WEIGHT_0";                                                           //SYTH & SIM
parameter   ENUM_RCFG_STATIC_WEIGHT_5                                           = "WEIGHT_0";                                                           //SYTH & SIM
parameter   ENUM_RCFG_USER_PRIORITY_0                                           = "PRIORITY_0";                                                         //SYTH & SIM
parameter   ENUM_RCFG_USER_PRIORITY_1                                           = "PRIORITY_0";                                                         //SYTH & SIM
parameter   ENUM_RCFG_USER_PRIORITY_2                                           = "PRIORITY_0";                                                         //SYTH & SIM
parameter   ENUM_RCFG_USER_PRIORITY_3                                           = "PRIORITY_0";                                                         //SYTH & SIM
parameter   ENUM_RCFG_USER_PRIORITY_4                                           = "PRIORITY_0";                                                         //SYTH & SIM
parameter   ENUM_RCFG_USER_PRIORITY_5                                           = "PRIORITY_0";                                                         //SYTH & SIM
parameter   ENUM_RD_DWIDTH_0                                                    = "DWIDTH_0";                                                           //SYTH ONLY
parameter   ENUM_RD_DWIDTH_1                                                    = "DWIDTH_0";                                                           //SYTH ONLY
parameter   ENUM_RD_DWIDTH_2                                                    = "DWIDTH_0";                                                           //SYTH ONLY
parameter   ENUM_RD_DWIDTH_3                                                    = "DWIDTH_0";                                                           //SYTH ONLY
parameter   ENUM_RD_DWIDTH_4                                                    = "DWIDTH_0";                                                           //SYTH ONLY
parameter   ENUM_RD_DWIDTH_5                                                    = "DWIDTH_0";                                                           //SYTH ONLY
parameter   ENUM_RD_FIFO_IN_USE_0                                               = "FALSE";                                                              //SYTH ONLY
parameter   ENUM_RD_FIFO_IN_USE_1                                               = "FALSE";                                                              //SYTH ONLY
parameter   ENUM_RD_FIFO_IN_USE_2                                               = "FALSE";                                                              //SYTH ONLY
parameter   ENUM_RD_FIFO_IN_USE_3                                               = "FALSE";                                                              //SYTH ONLY
parameter   ENUM_RD_PORT_INFO_0                                                 = "USE_NO";                                                             //SYTH ONLY
parameter   ENUM_RD_PORT_INFO_1                                                 = "USE_NO";                                                             //SYTH ONLY
parameter   ENUM_RD_PORT_INFO_2                                                 = "USE_NO";                                                             //SYTH ONLY
parameter   ENUM_RD_PORT_INFO_3                                                 = "USE_NO";                                                             //SYTH ONLY
parameter   ENUM_RD_PORT_INFO_4                                                 = "USE_NO";                                                             //SYTH ONLY
parameter   ENUM_RD_PORT_INFO_5                                                 = "USE_NO";                                                             //SYTH ONLY
parameter   ENUM_READ_ODT_CHIP                                                  = "ODT_DISABLED";                                                       //SYTH & SIM
parameter   ENUM_REORDER_DATA                                                   = "DATA_REORDERING";                                                    //SYTH & SIM
parameter   ENUM_RFIFO0_CPORT_MAP                                               = "CMD_PORT_0";                                                         //SYTH & SIM
parameter   ENUM_RFIFO1_CPORT_MAP                                               = "CMD_PORT_0";                                                         //SYTH & SIM
parameter   ENUM_RFIFO2_CPORT_MAP                                               = "CMD_PORT_0";                                                         //SYTH & SIM
parameter   ENUM_RFIFO3_CPORT_MAP                                               = "CMD_PORT_0";                                                         //SYTH & SIM
parameter   ENUM_SINGLE_READY_0                                                 = "CONCATENATE_RDY";                                                    //SYTH & SIM
parameter   ENUM_SINGLE_READY_1                                                 = "CONCATENATE_RDY";                                                    //SYTH & SIM
parameter   ENUM_SINGLE_READY_2                                                 = "CONCATENATE_RDY";                                                    //SYTH & SIM
parameter   ENUM_SINGLE_READY_3                                                 = "CONCATENATE_RDY";                                                    //SYTH & SIM
parameter   ENUM_STATIC_WEIGHT_0                                                = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_STATIC_WEIGHT_1                                                = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_STATIC_WEIGHT_2                                                = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_STATIC_WEIGHT_3                                                = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_STATIC_WEIGHT_4                                                = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_STATIC_WEIGHT_5                                                = "WEIGHT_0";                                                           //SYTH ONLY
parameter   ENUM_SYNC_MODE_0                                                    = "ASYNCHRONOUS";                                                       //SYTH & SIM
parameter   ENUM_SYNC_MODE_1                                                    = "ASYNCHRONOUS";                                                       //SYTH & SIM
parameter   ENUM_SYNC_MODE_2                                                    = "ASYNCHRONOUS";                                                       //SYTH & SIM
parameter   ENUM_SYNC_MODE_3                                                    = "ASYNCHRONOUS";                                                       //SYTH & SIM
parameter   ENUM_SYNC_MODE_4                                                    = "ASYNCHRONOUS";                                                       //SYTH & SIM
parameter   ENUM_SYNC_MODE_5                                                    = "ASYNCHRONOUS";                                                       //SYTH & SIM
parameter   ENUM_TEST_MODE                                                      = "NORMAL_MODE";                                                        //SYTH & SIM
parameter   ENUM_THLD_JAR1_0                                                    = "THRESHOLD_32";                                                       //SYTH & SIM
parameter   ENUM_THLD_JAR1_1                                                    = "THRESHOLD_32";                                                       //SYTH & SIM
parameter   ENUM_THLD_JAR1_2                                                    = "THRESHOLD_32";                                                       //SYTH & SIM
parameter   ENUM_THLD_JAR1_3                                                    = "THRESHOLD_32";                                                       //SYTH & SIM
parameter   ENUM_THLD_JAR1_4                                                    = "THRESHOLD_32";                                                       //SYTH & SIM
parameter   ENUM_THLD_JAR1_5                                                    = "THRESHOLD_32";                                                       //SYTH & SIM
parameter   ENUM_THLD_JAR2_0                                                    = "THRESHOLD_16";                                                       //SYTH & SIM
parameter   ENUM_THLD_JAR2_1                                                    = "THRESHOLD_16";                                                       //SYTH & SIM
parameter   ENUM_THLD_JAR2_2                                                    = "THRESHOLD_16";                                                       //SYTH & SIM
parameter   ENUM_THLD_JAR2_3                                                    = "THRESHOLD_16";                                                       //SYTH & SIM
parameter   ENUM_THLD_JAR2_4                                                    = "THRESHOLD_16";                                                       //SYTH & SIM
parameter   ENUM_THLD_JAR2_5                                                    = "THRESHOLD_16";                                                       //SYTH & SIM
parameter   ENUM_USE_ALMOST_EMPTY_0                                             = "EMPTY";                                                              //SYTH & SIM
parameter   ENUM_USE_ALMOST_EMPTY_1                                             = "EMPTY";                                                              //SYTH & SIM
parameter   ENUM_USE_ALMOST_EMPTY_2                                             = "EMPTY";                                                              //SYTH & SIM
parameter   ENUM_USE_ALMOST_EMPTY_3                                             = "EMPTY";                                                              //SYTH & SIM
parameter   ENUM_USER_ECC_EN                                                    = "DISABLE";                                                            //SYTH & SIM
parameter   ENUM_USER_PRIORITY_0                                                = "PRIORITY_0";                                                         //SYTH ONLY
parameter   ENUM_USER_PRIORITY_1                                                = "PRIORITY_0";                                                         //SYTH ONLY
parameter   ENUM_USER_PRIORITY_2                                                = "PRIORITY_0";                                                         //SYTH ONLY
parameter   ENUM_USER_PRIORITY_3                                                = "PRIORITY_0";                                                         //SYTH ONLY
parameter   ENUM_USER_PRIORITY_4                                                = "PRIORITY_0";                                                         //SYTH ONLY
parameter   ENUM_USER_PRIORITY_5                                                = "PRIORITY_0";                                                         //SYTH ONLY
parameter   ENUM_WFIFO0_CPORT_MAP                                               = "CMD_PORT_0";                                                         //SYTH & SIM
parameter   ENUM_WFIFO0_RDY_ALMOST_FULL                                         = "NOT_FULL";                                                           //SYTH & SIM
parameter   ENUM_WFIFO1_CPORT_MAP                                               = "CMD_PORT_0";                                                         //SYTH & SIM
parameter   ENUM_WFIFO1_RDY_ALMOST_FULL                                         = "NOT_FULL";                                                           //SYTH & SIM
parameter   ENUM_WFIFO2_CPORT_MAP                                               = "CMD_PORT_0";                                                         //SYTH & SIM
parameter   ENUM_WFIFO2_RDY_ALMOST_FULL                                         = "NOT_FULL";                                                           //SYTH & SIM
parameter   ENUM_WFIFO3_CPORT_MAP                                               = "CMD_PORT_0";                                                         //SYTH & SIM
parameter   ENUM_WFIFO3_RDY_ALMOST_FULL                                         = "NOT_FULL";                                                           //SYTH & SIM
parameter   ENUM_WR_DWIDTH_0                                                    = "DWIDTH_0";                                                           //SYTH ONLY
parameter   ENUM_WR_DWIDTH_1                                                    = "DWIDTH_0";                                                           //SYTH ONLY
parameter   ENUM_WR_DWIDTH_2                                                    = "DWIDTH_0";                                                           //SYTH ONLY
parameter   ENUM_WR_DWIDTH_3                                                    = "DWIDTH_0";                                                           //SYTH ONLY
parameter   ENUM_WR_DWIDTH_4                                                    = "DWIDTH_0";                                                           //SYTH ONLY
parameter   ENUM_WR_DWIDTH_5                                                    = "DWIDTH_0";                                                           //SYTH ONLY
parameter   ENUM_WR_FIFO_IN_USE_0                                               = "FALSE";                                                              //SYTH ONLY
parameter   ENUM_WR_FIFO_IN_USE_1                                               = "FALSE";                                                              //SYTH ONLY
parameter   ENUM_WR_FIFO_IN_USE_2                                               = "FALSE";                                                              //SYTH ONLY
parameter   ENUM_WR_FIFO_IN_USE_3                                               = "FALSE";                                                              //SYTH ONLY
parameter   ENUM_WR_PORT_INFO_0                                                 = "USE_NO";                                                             //SYTH ONLY
parameter   ENUM_WR_PORT_INFO_1                                                 = "USE_NO";                                                             //SYTH ONLY
parameter   ENUM_WR_PORT_INFO_2                                                 = "USE_NO";                                                             //SYTH ONLY
parameter   ENUM_WR_PORT_INFO_3                                                 = "USE_NO";                                                             //SYTH ONLY
parameter   ENUM_WR_PORT_INFO_4                                                 = "USE_NO";                                                             //SYTH ONLY
parameter   ENUM_WR_PORT_INFO_5                                                 = "USE_NO";                                                             //SYTH ONLY
parameter   ENUM_WRITE_ODT_CHIP                                                 = "ODT_DISABLED";                                                       //SYTH & SIM
parameter   ENUM_ENABLE_BURST_INTERRUPT                                         = "DISABLED";                                                           //SYTH & SIM
parameter   ENUM_ENABLE_BURST_TERMINATE                                         = "DISABLED";                                                           //SYTH & SIM
parameter   INTG_POWER_SAVING_EXIT_CYCLES                                       = 5;                                                                    //SYTH & SIM
parameter   INTG_MEM_CLK_ENTRY_CYCLES                                           = 10;                                                                   //SYTH & SIM
parameter   INTG_PRIORITY_REMAP                                                 = 0;                                                                    //SYTH & SIM
parameter   INTG_MEM_AUTO_PD_CYCLES                                             = 0;                                                                    //SYTH & SIM
parameter   INTG_CYC_TO_RLD_JARS_0                                              = 128;                                                                  //SYTH & SIM
parameter   INTG_CYC_TO_RLD_JARS_1                                              = 128;                                                                  //SYTH & SIM
parameter   INTG_CYC_TO_RLD_JARS_2                                              = 128;                                                                  //SYTH & SIM
parameter   INTG_CYC_TO_RLD_JARS_3                                              = 128;                                                                  //SYTH & SIM
parameter   INTG_CYC_TO_RLD_JARS_4                                              = 128;                                                                  //SYTH & SIM
parameter   INTG_CYC_TO_RLD_JARS_5                                              = 128;                                                                  //SYTH & SIM
parameter   INTG_EXTRA_CTL_CLK_ACT_TO_ACT                                       = 0;                                                                    //SYTH & SIM
parameter   INTG_EXTRA_CTL_CLK_ACT_TO_ACT_DIFF_BANK                             = 0;                                                                    //SYTH & SIM
parameter   INTG_EXTRA_CTL_CLK_ACT_TO_PCH                                       = 0;                                                                    //SYTH & SIM
parameter   INTG_EXTRA_CTL_CLK_ACT_TO_RDWR                                      = 0;                                                                    //SYTH & SIM
parameter   INTG_EXTRA_CTL_CLK_ARF_PERIOD                                       = 0;                                                                    //SYTH & SIM
parameter   INTG_EXTRA_CTL_CLK_ARF_TO_VALID                                     = 0;                                                                    //SYTH & SIM
parameter   INTG_EXTRA_CTL_CLK_FOUR_ACT_TO_ACT                                  = 0;                                                                    //SYTH & SIM
parameter   INTG_EXTRA_CTL_CLK_PCH_ALL_TO_VALID                                 = 0;                                                                    //SYTH & SIM
parameter   INTG_EXTRA_CTL_CLK_PCH_TO_VALID                                     = 0;                                                                    //SYTH & SIM
parameter   INTG_EXTRA_CTL_CLK_PDN_PERIOD                                       = 0;                                                                    //SYTH & SIM
parameter   INTG_EXTRA_CTL_CLK_PDN_TO_VALID                                     = 0;                                                                    //SYTH & SIM
parameter   INTG_EXTRA_CTL_CLK_RD_AP_TO_VALID                                   = 0;                                                                    //SYTH & SIM
parameter   INTG_EXTRA_CTL_CLK_RD_TO_PCH                                        = 0;                                                                    //SYTH & SIM
parameter   INTG_EXTRA_CTL_CLK_RD_TO_RD                                         = 0;                                                                    //SYTH & SIM
parameter   INTG_EXTRA_CTL_CLK_RD_TO_RD_DIFF_CHIP                               = 0;                                                                    //SYTH & SIM
parameter   INTG_EXTRA_CTL_CLK_RD_TO_WR                                         = 0;                                                                    //SYTH & SIM
parameter   INTG_EXTRA_CTL_CLK_RD_TO_WR_BC                                      = 0;                                                                    //SYTH & SIM
parameter   INTG_EXTRA_CTL_CLK_RD_TO_WR_DIFF_CHIP                               = 0;                                                                    //SYTH & SIM
parameter   INTG_EXTRA_CTL_CLK_SRF_TO_VALID                                     = 0;                                                                    //SYTH & SIM
parameter   INTG_EXTRA_CTL_CLK_SRF_TO_ZQ_CAL                                    = 0;                                                                    //SYTH & SIM
parameter   INTG_EXTRA_CTL_CLK_WR_AP_TO_VALID                                   = 0;                                                                    //SYTH & SIM
parameter   INTG_EXTRA_CTL_CLK_WR_TO_PCH                                        = 0;                                                                    //SYTH & SIM
parameter   INTG_EXTRA_CTL_CLK_WR_TO_RD                                         = 0;                                                                    //SYTH & SIM
parameter   INTG_EXTRA_CTL_CLK_WR_TO_RD_BC                                      = 0;                                                                    //SYTH & SIM
parameter   INTG_EXTRA_CTL_CLK_WR_TO_RD_DIFF_CHIP                               = 0;                                                                    //SYTH & SIM
parameter   INTG_EXTRA_CTL_CLK_WR_TO_WR                                         = 0;                                                                    //SYTH & SIM
parameter   INTG_EXTRA_CTL_CLK_WR_TO_WR_DIFF_CHIP                               = 0;                                                                    //SYTH & SIM
parameter   INTG_MEM_IF_TREFI                                                   = 3120;                                                                 //SYTH & SIM
parameter   INTG_MEM_IF_TRFC                                                    = 34;                                                                   //SYTH & SIM
parameter   INTG_RCFG_SUM_WT_PRIORITY_0                                         = 0;                                                                    //SYTH & SIM
parameter   INTG_RCFG_SUM_WT_PRIORITY_1                                         = 0;                                                                    //SYTH & SIM
parameter   INTG_RCFG_SUM_WT_PRIORITY_2                                         = 0;                                                                    //SYTH & SIM
parameter   INTG_RCFG_SUM_WT_PRIORITY_3                                         = 0;                                                                    //SYTH & SIM
parameter   INTG_RCFG_SUM_WT_PRIORITY_4                                         = 0;                                                                    //SYTH & SIM
parameter   INTG_RCFG_SUM_WT_PRIORITY_5                                         = 0;                                                                    //SYTH & SIM
parameter   INTG_RCFG_SUM_WT_PRIORITY_6                                         = 0;                                                                    //SYTH & SIM
parameter   INTG_RCFG_SUM_WT_PRIORITY_7                                         = 0;                                                                    //SYTH & SIM
parameter   INTG_SUM_WT_PRIORITY_0                                              = 0;                                                                    //SYTH ONLY
parameter   INTG_SUM_WT_PRIORITY_1                                              = 0;                                                                    //SYTH ONLY
parameter   INTG_SUM_WT_PRIORITY_2                                              = 0;                                                                    //SYTH ONLY
parameter   INTG_SUM_WT_PRIORITY_3                                              = 0;                                                                    //SYTH ONLY
parameter   INTG_SUM_WT_PRIORITY_4                                              = 0;                                                                    //SYTH ONLY
parameter   INTG_SUM_WT_PRIORITY_5                                              = 0;                                                                    //SYTH ONLY
parameter   INTG_SUM_WT_PRIORITY_6                                              = 0;                                                                    //SYTH ONLY
parameter   INTG_SUM_WT_PRIORITY_7                                              = 0;                                                                    //SYTH ONLY
parameter   VECT_ATTR_COUNTER_ONE_MASK                                          = 64'b0000000000000000000000000000000000000000000000000000000000000000; //SYTH & SIM
parameter   VECT_ATTR_COUNTER_ONE_MATCH                                         = 64'b0000000000000000000000000000000000000000000000000000000000000000; //SYTH & SIM
parameter   VECT_ATTR_COUNTER_ZERO_MASK                                         = 64'b0000000000000000000000000000000000000000000000000000000000000000; //SYTH & SIM
parameter   VECT_ATTR_COUNTER_ZERO_MATCH                                        = 64'b0000000000000000000000000000000000000000000000000000000000000000; //SYTH & SIM
parameter   VECT_ATTR_DEBUG_SELECT_BYTE                                         = 32'b00000000000000000000000000000000;                                 //SYTH & SIM

// END PARAMETER SECTION
//////////////////////////////////////////////////////////////////////////////

//////////////////////////////////////////////////////////////////////////////
// START LOCALPARAM

// WIRE
localparam  CFG_CFG_AVALON_DATA_BYTES                                           = 'd1;
localparam  CFG_CFG_AVALON_ADDR_WIDTH                                           = 'd10;
localparam  MAX_CMD_PT_NUM                                                      = 6;
localparam  MAX_FIFO_NUM                                                        = 4;
localparam  RD_FIFO_WIDTH                                                       = 80;
localparam  WR_FIFO_WIDTH                                                       = 90;
localparam  MAX_PORT_BL                                                         = 255;
localparam  MAX_PORT_CMD_WIDTH                                                  = 2;
localparam  MAX_PORT_PRI_WIDTH                                                  = 0;
localparam  MAX_PORT_ADDR_WIDTH                                                 = 32;
localparam  MAX_PORT_BL_WIDTH                                                   = 8;
localparam  MAX_PORT_TID_WIDTH                                                  = 0;
localparam  MAX_PORT_CMDE_WIDTH                                                 = 0;
localparam  CMD_FIFO_DWIDTH                                                     = (MAX_PORT_CMDE_WIDTH + MAX_PORT_TID_WIDTH + MAX_PORT_BL_WIDTH + MAX_PORT_ADDR_WIDTH + MAX_PORT_PRI_WIDTH + MAX_PORT_CMD_WIDTH);
localparam  CFG_MEM_IF_CHIP                                                     = 1;
localparam  CFG_PORT_WIDTH_INTERFACE_WIDTH                                      = 8;
localparam  CFG_PORT_WIDTH_DEVICE_WIDTH                                         = 8;
localparam  CFG_PORT_WIDTH_COL_ADDR_WIDTH                                       = 8;
localparam  CFG_PORT_WIDTH_ROW_ADDR_WIDTH                                       = 8;
localparam  CFG_PORT_WIDTH_BANK_ADDR_WIDTH                                      = 8;
localparam  CFG_PORT_WIDTH_CS_ADDR_WIDTH                                        = 8;
localparam  CFG_PORT_WIDTH_CAS_WR_LAT                                           = 8;
localparam  CFG_PORT_WIDTH_ADD_LAT                                              = 8;
localparam  CFG_PORT_WIDTH_TCL                                                  = 8;
localparam  CFG_PORT_WIDTH_TRFC                                                 = 8;
localparam  CFG_PORT_WIDTH_TREFI                                                = 16;
localparam  CFG_PORT_WIDTH_TWR                                                  = 8;
localparam  CFG_PORT_WIDTH_TMRD                                                 = 8;

localparam  HARDIP_AFI_ADDR_WIDTH                                               = 20;
localparam  HARDIP_AFI_BANKADDR_WIDTH                                           = 3 ;
localparam  HARDIP_AFI_CONTROL_WIDTH                                            = 1 ;
localparam  HARDIP_AFI_CS_WIDTH                                                 = 2 ;
localparam  HARDIP_AFI_ODT_WIDTH                                                = 2 ;
localparam  HARDIP_AFI_DM_WIDTH                                                 = 10;
localparam  HARDIP_AFI_DQ_WIDTH                                                 = 80;
localparam  HARDIP_AFI_WRITE_DQS_WIDTH                                          = 5 ;
// HARDIP_AFI_RATE_RATIO doesn't make sense as a normal rate ratio,
// but it does make the correct size of the afi_rdata_en and afi_rdata_en_full signals
localparam  HARDIP_AFI_RATE_RATIO                                               = 5 ;
localparam  HARDIP_AFI_WLAT_WIDTH                                               = 4 ;
localparam  HARDIP_AFI_RLAT_WIDTH                                               = 5 ;
localparam  HARDIP_TRACKING_WIDTH                                               = 2 ;

localparam  INT_AFI_ADDR_WIDTH                                                  = (HARD_PHY == 1) ? HARDIP_AFI_ADDR_WIDTH      : AFI_ADDR_WIDTH     ;
localparam  INT_AFI_BANKADDR_WIDTH                                              = (HARD_PHY == 1) ? HARDIP_AFI_BANKADDR_WIDTH  : AFI_BANKADDR_WIDTH ;
localparam  INT_AFI_CONTROL_WIDTH                                               = (HARD_PHY == 1) ? HARDIP_AFI_CONTROL_WIDTH   : AFI_CONTROL_WIDTH  ;
localparam  INT_AFI_CS_WIDTH                                                    = (HARD_PHY == 1) ? HARDIP_AFI_CS_WIDTH        : AFI_CS_WIDTH       ;
localparam  INT_AFI_ODT_WIDTH                                                   = (HARD_PHY == 1) ? HARDIP_AFI_ODT_WIDTH       : AFI_ODT_WIDTH       ;
localparam  INT_AFI_DM_WIDTH                                                    = (HARD_PHY == 1) ? HARDIP_AFI_DM_WIDTH        : AFI_DM_WIDTH       ;
localparam  INT_AFI_DQ_WIDTH                                                    = (HARD_PHY == 1) ? HARDIP_AFI_DQ_WIDTH        : AFI_DQ_WIDTH       ;
localparam  INT_AFI_WRITE_DQS_WIDTH                                             = (HARD_PHY == 1) ? HARDIP_AFI_WRITE_DQS_WIDTH : AFI_WRITE_DQS_WIDTH;
localparam  INT_AFI_RATE_RATIO                                                  = (HARD_PHY == 1) ? HARDIP_AFI_RATE_RATIO      : AFI_RATE_RATIO     ;
localparam  INT_AFI_WLAT_WIDTH                                                  = (HARD_PHY == 1) ? HARDIP_AFI_WLAT_WIDTH      : AFI_WLAT_WIDTH     ;
localparam  INT_AFI_RLAT_WIDTH                                                  = (HARD_PHY == 1) ? HARDIP_AFI_RLAT_WIDTH      : AFI_RLAT_WIDTH     ;

localparam  ZERO_PAD_WIDTH_BE_32                                                = (ENUM_USER_ECC_EN == "ENABLE" ) ?   2 :  6;
localparam  ZERO_PAD_WIDTH_BE_64                                                = (ENUM_USER_ECC_EN == "ENABLE" ) ?   0 :  2;
localparam  ZERO_PAD_WIDTH_DT_32                                                = (ENUM_USER_ECC_EN == "ENABLE" ) ?  32 : 48;
localparam  ZERO_PAD_WIDTH_DT_64                                                = (ENUM_USER_ECC_EN == "ENABLE" ) ?   0 : 16;
localparam  BE_WIDTH_FIFO0_32                                                   = (ENUM_USER_ECC_EN == "ENABLE" ) ?   6 :  4;
localparam  BE_WIDTH_FIFO0_64                                                   = (ENUM_USER_ECC_EN == "ENABLE" ) ?  10 :  8;
localparam  BE_WIDTH_FIFO1_64                                                   = (ENUM_USER_ECC_EN == "ENABLE" ) ?  20 : 16;
localparam  BE_WIDTH_FIFO2_64                                                   = (ENUM_USER_ECC_EN == "ENABLE" ) ?  30 : 24;
localparam  BE_WIDTH_FIFO3_64                                                   = (ENUM_USER_ECC_EN == "ENABLE" ) ?  40 : 32;
localparam  DATA_WIDTH_FIFO0_32                                                 = (ENUM_USER_ECC_EN == "ENABLE" ) ?  48 : 32;
localparam  DATA_WIDTH_FIFO0_64                                                 = (ENUM_USER_ECC_EN == "ENABLE" ) ?  80 : 64;
localparam  DATA_WIDTH_FIFO1_64                                                 = (ENUM_USER_ECC_EN == "ENABLE" ) ? 160 :128;
localparam  DATA_WIDTH_FIFO2_64                                                 = (ENUM_USER_ECC_EN == "ENABLE" ) ? 240 :192;
localparam  DATA_WIDTH_FIFO3_64                                                 = (ENUM_USER_ECC_EN == "ENABLE" ) ? 320 :256;

// END LOCALPARAM
////////////////////////////////////////////////////////////////////////////////

// END LOCALPARAM
//////////////////////////////////////////////////////////////////////////////

//////////////////////////////////////////////////////////////////////////////
// BEGIN PORT SECTION

// Clock and reset interface
input                                                                           afi_clk;
input                                                                           afi_half_clk;
input                                                                           afi_reset_n;
input                                                                           ctl_clk;
input                                                                           ctl_reset_n;
input                                                                           mp_cmd_clk_0;
input                                                                           mp_cmd_clk_1;
input                                                                           mp_cmd_clk_2;
input                                                                           mp_cmd_clk_3;
input                                                                           mp_cmd_clk_4;
input                                                                           mp_cmd_clk_5;
input                                                                           mp_cmd_reset_n_0;
input                                                                           mp_cmd_reset_n_1;
input                                                                           mp_cmd_reset_n_2;
input                                                                           mp_cmd_reset_n_3;
input                                                                           mp_cmd_reset_n_4;
input                                                                           mp_cmd_reset_n_5;
input                                                                           mp_rfifo_clk_0;
input                                                                           mp_rfifo_clk_1;
input                                                                           mp_rfifo_clk_2;
input                                                                           mp_rfifo_clk_3;
input                                                                           mp_rfifo_reset_n_0;
input                                                                           mp_rfifo_reset_n_1;
input                                                                           mp_rfifo_reset_n_2;
input                                                                           mp_rfifo_reset_n_3;
input                                                                           mp_wfifo_clk_0;
input                                                                           mp_wfifo_clk_1;
input                                                                           mp_wfifo_clk_2;
input                                                                           mp_wfifo_clk_3;
input                                                                           mp_wfifo_reset_n_0;
input                                                                           mp_wfifo_reset_n_1;
input                                                                           mp_wfifo_reset_n_2;
input                                                                           mp_wfifo_reset_n_3;
input                                                                           csr_clk;
input                                                                           csr_reset_n;            

// Avalon data slave interface
output                                                                          avl_ready_0;
input                                                                           avl_write_req_0;
input                                                                           avl_read_req_0;
input   [AVL_ADDR_WIDTH_PORT_0              - 1 : 0]                            avl_addr_0;
input   [AVL_NUM_SYMBOLS_PORT_0             - 1 : 0]                            avl_be_0;
input   [AVL_DATA_WIDTH_PORT_0              - 1 : 0]                            avl_wdata_0;
input   [AVL_SIZE_WIDTH                     - 1 : 0]                            avl_size_0;
input                                                                           avl_burstbegin_0;
output  [AVL_DATA_WIDTH_PORT_0              - 1 : 0]                            avl_rdata_0;
output                                                                          avl_rdata_valid_0;
output                                                                          avl_ready_1;
input                                                                           avl_write_req_1;
input                                                                           avl_read_req_1;
input   [AVL_ADDR_WIDTH_PORT_1              - 1 : 0]                            avl_addr_1;
input   [AVL_NUM_SYMBOLS_PORT_1             - 1 : 0]                            avl_be_1;
input   [AVL_DATA_WIDTH_PORT_1              - 1 : 0]                            avl_wdata_1;
input   [AVL_SIZE_WIDTH                     - 1 : 0]                            avl_size_1;
input                                                                           avl_burstbegin_1;
output  [AVL_DATA_WIDTH_PORT_1              - 1 : 0]                            avl_rdata_1;
output                                                                          avl_rdata_valid_1;
output                                                                          avl_ready_2;
input                                                                           avl_write_req_2;
input                                                                           avl_read_req_2;
input   [AVL_ADDR_WIDTH_PORT_2              - 1 : 0]                            avl_addr_2;
input   [AVL_NUM_SYMBOLS_PORT_2             - 1 : 0]                            avl_be_2;
input   [AVL_DATA_WIDTH_PORT_2              - 1 : 0]                            avl_wdata_2;
input   [AVL_SIZE_WIDTH                     - 1 : 0]                            avl_size_2;
input                                                                           avl_burstbegin_2;
output  [AVL_DATA_WIDTH_PORT_2              - 1 : 0]                            avl_rdata_2;
output                                                                          avl_rdata_valid_2;
output                                                                          avl_ready_3;
input                                                                           avl_write_req_3;
input                                                                           avl_read_req_3;
input   [AVL_ADDR_WIDTH_PORT_3              - 1 : 0]                            avl_addr_3;
input   [AVL_NUM_SYMBOLS_PORT_3             - 1 : 0]                            avl_be_3;
input   [AVL_DATA_WIDTH_PORT_3              - 1 : 0]                            avl_wdata_3;
input   [AVL_SIZE_WIDTH                     - 1 : 0]                            avl_size_3;
input                                                                           avl_burstbegin_3;
output  [AVL_DATA_WIDTH_PORT_3              - 1 : 0]                            avl_rdata_3;
output                                                                          avl_rdata_valid_3;
output                                                                          avl_ready_4;
input                                                                           avl_write_req_4;
input                                                                           avl_read_req_4;
input   [AVL_ADDR_WIDTH_PORT_4              - 1 : 0]                            avl_addr_4;
input   [AVL_NUM_SYMBOLS_PORT_4             - 1 : 0]                            avl_be_4;
input   [AVL_DATA_WIDTH_PORT_4              - 1 : 0]                            avl_wdata_4;
input   [AVL_SIZE_WIDTH                     - 1 : 0]                            avl_size_4;
input                                                                           avl_burstbegin_4;
output  [AVL_DATA_WIDTH_PORT_4              - 1 : 0]                            avl_rdata_4;
output                                                                          avl_rdata_valid_4;
output                                                                          avl_ready_5;
input                                                                           avl_write_req_5;
input                                                                           avl_read_req_5;
input   [AVL_ADDR_WIDTH_PORT_5              - 1 : 0]                            avl_addr_5;
input   [AVL_NUM_SYMBOLS_PORT_5             - 1 : 0]                            avl_be_5;
input   [AVL_DATA_WIDTH_PORT_5              - 1 : 0]                            avl_wdata_5;
input   [AVL_SIZE_WIDTH                     - 1 : 0]                            avl_size_5;
input                                                                           avl_burstbegin_5;
output  [AVL_DATA_WIDTH_PORT_5              - 1 : 0]                            avl_rdata_5;
output                                                                          avl_rdata_valid_5;

// AFI signals
output  [INT_AFI_CS_WIDTH                   - 1 : 0]                            afi_cs_n;
output  [INT_AFI_CS_WIDTH                   - 1 : 0]                            afi_cke;
output  [INT_AFI_ODT_WIDTH                  - 1 : 0]                            afi_odt;
output  [INT_AFI_ADDR_WIDTH                 - 1 : 0]                            afi_addr;
output  [INT_AFI_BANKADDR_WIDTH             - 1 : 0]                            afi_ba;
output  [INT_AFI_CONTROL_WIDTH              - 1 : 0]                            afi_ras_n;
output  [INT_AFI_CONTROL_WIDTH              - 1 : 0]                            afi_cas_n;
output  [INT_AFI_CONTROL_WIDTH              - 1 : 0]                            afi_we_n;
output  [INT_AFI_CONTROL_WIDTH              - 1 : 0]                            afi_rst_n;
output  [INT_AFI_WRITE_DQS_WIDTH            - 1 : 0]                            afi_dqs_burst;
output  [INT_AFI_WRITE_DQS_WIDTH            - 1 : 0]                            afi_wdata_valid;
output  [INT_AFI_DQ_WIDTH                   - 1 : 0]                            afi_wdata;
output  [INT_AFI_DM_WIDTH                   - 1 : 0]                            afi_dm;
input   [INT_AFI_WLAT_WIDTH                 - 1 : 0]                            afi_wlat;
output  [INT_AFI_RATE_RATIO                 - 1 : 0]                            afi_rdata_en;
output  [INT_AFI_RATE_RATIO                 - 1 : 0]                            afi_rdata_en_full;
input   [INT_AFI_DQ_WIDTH                   - 1 : 0]                            afi_rdata;
input   [1                                  - 1 : 0]                            afi_rdata_valid;
input   [INT_AFI_RLAT_WIDTH                 - 1 : 0]                            afi_rlat;
input                                                                           afi_cal_success;
input                                                                           afi_cal_fail;
output                                                                          afi_cal_req;
output                                                                          afi_init_req;

output  [MEM_IF_CLK_PAIR_COUNT              - 1 : 0]                            afi_mem_clk_disable;

// disable unused AFI signals

wire    [(MEM_IF_DQS_WIDTH*MEM_IF_CS_WIDTH) - 1 : 0]                            afi_cal_byte_lane_sel_n;
output  [MEM_IF_CS_WIDTH                    - 1 : 0]                            afi_ctl_refresh_done;
input   [MEM_IF_CS_WIDTH                    - 1 : 0]                            afi_seq_busy;
output  [MEM_IF_CS_WIDTH                    - 1 : 0]                            afi_ctl_long_idle;

// Sideband signals
output                                                                          local_refresh_ack;
output                                                                          local_powerdn_ack;
output                                                                          local_self_rfsh_ack;
output                                                                          local_deep_powerdn_ack;
input                                                                           local_refresh_req;
input   [MEM_IF_CS_WIDTH                    - 1 : 0]                            local_refresh_chip;

//Ahmed: do we need this?
//input                                                                           local_powerdn_req;
input                                                                           local_self_rfsh_req;
input   [MEM_IF_CS_WIDTH                    - 1 : 0]                            local_self_rfsh_chip;
input                                                                           local_deep_powerdn_req;
input   [MEM_IF_CS_WIDTH                    - 1 : 0]                            local_deep_powerdn_chip;
input                                                                           local_multicast;
input                                                                           local_priority;

output                                                                          local_init_done;
output                                                                          local_cal_success;
output                                                                          local_cal_fail;

// Csr & ecc signals
input                                                                           csr_read_req;
input                                                                           csr_write_req;
input   [CSR_ADDR_WIDTH                     - 1 : 0]                            csr_addr;
input   [CSR_DATA_WIDTH                     - 1 : 0]                            csr_wdata;
output  [CSR_DATA_WIDTH                     - 1 : 0]                            csr_rdata;
input   [CSR_BE_WIDTH                       - 1 : 0]                            csr_be;
output                                                                          csr_rdata_valid;
output                                                                          csr_waitrequest;

// Cfg signal to Phy
output [23:0]                                                                   cfg_dramconfig;
output [CFG_PORT_WIDTH_CAS_WR_LAT           - 1 : 0]                            cfg_caswrlat;
output [CFG_PORT_WIDTH_ADD_LAT              - 1 : 0]                            cfg_addlat;
output [CFG_PORT_WIDTH_TCL                  - 1 : 0]                            cfg_tcl;
output [CFG_PORT_WIDTH_TRFC                 - 1 : 0]                            cfg_trfc;
output [CFG_PORT_WIDTH_TREFI                - 1 : 0]                            cfg_trefi;
output [CFG_PORT_WIDTH_TWR                  - 1 : 0]                            cfg_twr;
output [CFG_PORT_WIDTH_TMRD                 - 1 : 0]                            cfg_tmrd;
output [CFG_PORT_WIDTH_COL_ADDR_WIDTH       - 1 : 0]                            cfg_coladdrwidth;
output [CFG_PORT_WIDTH_ROW_ADDR_WIDTH       - 1 : 0]                            cfg_rowaddrwidth;
output [CFG_PORT_WIDTH_BANK_ADDR_WIDTH      - 1 : 0]                            cfg_bankaddrwidth;
output [CFG_PORT_WIDTH_CS_ADDR_WIDTH        - 1 : 0]                            cfg_csaddrwidth;
output [CFG_PORT_WIDTH_INTERFACE_WIDTH      - 1 : 0]                            cfg_interfacewidth;
output [CFG_PORT_WIDTH_DEVICE_WIDTH         - 1 : 0]                            cfg_devicewidth;
output										ctl_init_req;

// Bonding signals
output [MAX_FIFO_NUM                        - 1 : 0]                            bonding_out_1;
input  [MAX_FIFO_NUM                        - 1 : 0]                            bonding_in_1;
output [MAX_CMD_PT_NUM                      - 1 : 0]                            bonding_out_2;
input  [MAX_CMD_PT_NUM                      - 1 : 0]                            bonding_in_2;
output [MAX_CMD_PT_NUM                      - 1 : 0]                            bonding_out_3;
input  [MAX_CMD_PT_NUM                      - 1 : 0]                            bonding_in_3;

// IO_INT interface from HPHY
input                                                                           io_intaficalfail;
input                                                                           io_intaficalsuccess;

// Connect to user logic
output										local_sts_ctl_empty;

// END PORT SECTION
//////////////////////////////////////////////////////////////////////////////

wire                                                                            i_avst_cmd_reset_n_0;
wire                                                                            i_avst_cmd_reset_n_1;
wire                                                                            i_avst_cmd_reset_n_2;
wire                                                                            i_avst_cmd_reset_n_3;
wire                                                                            i_avst_cmd_reset_n_4;
wire                                                                            i_avst_cmd_reset_n_5;
wire [CMD_FIFO_DWIDTH                            -1:0]                          i_avst_cmd_data_0;
wire [CMD_FIFO_DWIDTH                            -1:0]                          i_avst_cmd_data_1;
wire [CMD_FIFO_DWIDTH                            -1:0]                          i_avst_cmd_data_2;
wire [CMD_FIFO_DWIDTH                            -1:0]                          i_avst_cmd_data_3;
wire [CMD_FIFO_DWIDTH                            -1:0]                          i_avst_cmd_data_4;
wire [CMD_FIFO_DWIDTH                            -1:0]                          i_avst_cmd_data_5;
wire                                                                            o_a_mm_ready_0;
wire                                                                            o_a_mm_ready_1;
wire                                                                            o_a_mm_ready_2;
wire                                                                            o_a_mm_ready_3;
wire                                                                            o_a_mm_ready_4;
wire                                                                            o_a_mm_ready_5;
wire                                                                            i_avst_wrack_ready_0;
wire                                                                            i_avst_wrack_ready_1;
wire                                                                            i_avst_wrack_ready_2;
wire                                                                            i_avst_wrack_ready_3;
wire                                                                            i_avst_wrack_ready_4;
wire                                                                            i_avst_wrack_ready_5;
wire                                                                            o_wrack_avst_valid_0;
wire                                                                            o_wrack_avst_valid_1;
wire                                                                            o_wrack_avst_valid_2;
wire                                                                            o_wrack_avst_valid_3;
wire                                                                            o_wrack_avst_valid_4;
wire                                                                            o_wrack_avst_valid_5;
wire                                                                            o_wrack_avst_data_0;
wire                                                                            o_wrack_avst_data_1;
wire                                                                            o_wrack_avst_data_2;
wire                                                                            o_wrack_avst_data_3;
wire                                                                            o_wrack_avst_data_4;
wire                                                                            o_wrack_avst_data_5;
wire                                                                            i_avst_rd_clk_0;
wire                                                                            i_avst_rd_clk_1;
wire                                                                            i_avst_rd_clk_2;
wire                                                                            i_avst_rd_clk_3;
wire                                                                            i_avst_rd_reset_n_0;
wire                                                                            i_avst_rd_reset_n_1;
wire                                                                            i_avst_rd_reset_n_2;
wire                                                                            i_avst_rd_reset_n_3;
wire                                                                            o_rd_avst_valid_0;
wire                                                                            o_rd_avst_valid_1;
wire                                                                            o_rd_avst_valid_2;
wire                                                                            o_rd_avst_valid_3;
wire [RD_FIFO_WIDTH                              -1:0]                          o_rd_avst_data_0;
wire [RD_FIFO_WIDTH                              -1:0]                          o_rd_avst_data_1;
wire [RD_FIFO_WIDTH                              -1:0]                          o_rd_avst_data_2;
wire [RD_FIFO_WIDTH                              -1:0]                          o_rd_avst_data_3;
wire                                                                            i_avst_rd_ready_0;
wire                                                                            i_avst_rd_ready_1;
wire                                                                            i_avst_rd_ready_2;
wire                                                                            i_avst_rd_ready_3;
wire                                                                            i_avst_wr_clk_0;
wire                                                                            i_avst_wr_clk_1;
wire                                                                            i_avst_wr_clk_2;
wire                                                                            i_avst_wr_clk_3;
wire                                                                            i_avst_wr_reset_n_0;
wire                                                                            i_avst_wr_reset_n_1;
wire                                                                            i_avst_wr_reset_n_2;
wire                                                                            i_avst_wr_reset_n_3;
wire [WR_FIFO_WIDTH                              -1:0]                          i_avst_wr_data_0;
wire [WR_FIFO_WIDTH                              -1:0]                          i_avst_wr_data_1;
wire [WR_FIFO_WIDTH                              -1:0]                          i_avst_wr_data_2;
wire [WR_FIFO_WIDTH                              -1:0]                          i_avst_wr_data_3;
wire [MAX_FIFO_NUM                               -1:0]                          bonding_out_1;
wire [MAX_FIFO_NUM                               -1:0]                          bonding_in_1;
wire [MAX_CMD_PT_NUM                             -1:0]                          bonding_out_2;
wire [MAX_CMD_PT_NUM                             -1:0]                          bonding_in_2;
wire [MAX_CMD_PT_NUM                             -1:0]                          bonding_out_3;
wire [MAX_CMD_PT_NUM                             -1:0]                          bonding_in_3;
wire                                                                            local_refresh_req;
wire [2                                          -1:0]                          local_refresh_chip_wire;
wire                                                                            local_deep_powerdn_req;
wire [2                                          -1:0]                          local_deep_powerdn_chip_wire;
wire                                                                            local_self_rfsh_req;
wire [2                                          -1:0]                          local_self_rfsh_chip_wire;
wire                                                                            local_refresh_ack;
wire                                                                            local_deep_powerdn_ack;
wire                                                                            local_powerdn_ack;
wire                                                                            local_self_rfsh_ack;
wire                                                                            local_init_done;
wire										local_sts_ctl_empty;
wire										ctl_init_req;
wire                                                                            mmr_clk;
wire                                                                            mmr_reset_n;
wire                                                                            mmr_read_req;
wire                                                                            mmr_write_req;
wire [2                                          -1:0]                          mmr_burst_count;
wire                                                                            mmr_burst_begin;
wire [CFG_CFG_AVALON_ADDR_WIDTH                  -1:0]                          mmr_addr;
wire [CFG_CFG_AVALON_DATA_BYTES*8                -1:0]                          mmr_wdata;
wire [CFG_CFG_AVALON_DATA_BYTES                  -1:0]                          mmr_be;
wire [CFG_CFG_AVALON_DATA_BYTES*8                -1:0]                          mmr_rdata;
wire                                                                            mmr_rdata_valid;
wire                                                                            mmr_waitrequest;
wire                                                                            sc_clk;
wire                                                                            sc_reset_n;
wire                                                                            sc_read_req;
wire                                                                            sc_write_req;
wire [2                                          -1:0]                          sc_burst_count;
wire                                                                            sc_burst_begin;
wire [CFG_CFG_AVALON_ADDR_WIDTH                  -1:0]                          sc_addr;
wire [CFG_CFG_AVALON_DATA_BYTES*8                -1:0]                          sc_wdata;
wire [CFG_CFG_AVALON_DATA_BYTES                  -1:0]                          sc_be;
wire [CFG_CFG_AVALON_DATA_BYTES*8                -1:0]                          sc_rdata;
wire                                                                            sc_rdata_valid;
wire                                                                            sc_waitrequest;
wire [24                                         -1:0]                          cfg_dramconfig;
wire [CFG_PORT_WIDTH_CAS_WR_LAT                  -1:0]                          cfg_caswrlat;
wire [CFG_PORT_WIDTH_ADD_LAT                     -1:0]                          cfg_addlat;
wire [CFG_PORT_WIDTH_TCL                         -1:0]                          cfg_tcl;
wire [CFG_PORT_WIDTH_TRFC                        -1:0]                          cfg_trfc;
wire [CFG_PORT_WIDTH_TREFI                       -1:0]                          cfg_trefi;
wire [CFG_PORT_WIDTH_TWR                         -1:0]                          cfg_twr;
wire [CFG_PORT_WIDTH_TMRD                        -1:0]                          cfg_tmrd;
wire [CFG_PORT_WIDTH_COL_ADDR_WIDTH              -1:0]                          cfg_coladdrwidth;
wire [CFG_PORT_WIDTH_ROW_ADDR_WIDTH              -1:0]                          cfg_rowaddrwidth;
wire [CFG_PORT_WIDTH_BANK_ADDR_WIDTH             -1:0]                          cfg_bankaddrwidth;
wire [CFG_PORT_WIDTH_CS_ADDR_WIDTH               -1:0]                          cfg_csaddrwidth;
wire [CFG_PORT_WIDTH_INTERFACE_WIDTH             -1:0]                          cfg_interfacewidth;
wire [CFG_PORT_WIDTH_DEVICE_WIDTH                -1:0]                          cfg_devicewidth;
wire                                                                            csrdin;
wire                                                                            csrdout;
wire                                                                            csrclk;
wire                                                                            csren;
wire                                                                            scanenable;
wire                                                                            afi_clk;
wire                                                                            afi_reset_n;
wire                                                                            ctl_reset_n;
wire [HARDIP_TRACKING_WIDTH                      -1:0]                          afi_seq_busy_int;

wire [24                                         -1:0]                          cfg_dramconfig_wire;
wire [4                                          -1:0]                          cfg_caswrlat_wire;
wire [5                                          -1:0]                          cfg_addlat_wire;
wire [5                                          -1:0]                          cfg_tcl_wire;
wire [8                                          -1:0]                          cfg_trfc_wire;
wire [16                                         -1:0]                          cfg_trefi_wire;
wire [4                                          -1:0]                          cfg_twr_wire;
wire [4                                          -1:0]                          cfg_tmrd_wire;
wire [5                                          -1:0]                          cfg_coladdrwidth_wire;
wire [5                                          -1:0]                          cfg_rowaddrwidth_wire;
wire [3                                          -1:0]                          cfg_bankaddrwidth_wire;
wire [3                                          -1:0]                          cfg_csaddrwidth_wire;
wire [8                                          -1:0]                          cfg_interfacewidth_wire;
wire [4                                          -1:0]                          cfg_devicewidth_wire;



//USED WITHIN THE MAPPING

wire [256                                        -1:0]                          data_width[5:0];
wire [32                                         -1:0]                          lsb_wfifo[5:0];
wire [32                                         -1:0]                          msb_wfifo[5:0];
wire [32                                         -1:0]                          lsb_rfifo[5:0];

wire [320                                        -1:0]                          avl_wdata_g[5:0];
wire [40                                         -1:0]                          avl_be_g[5:0];
reg  [WR_FIFO_WIDTH                              -1:0]                          i_avst_wr_data_g [3:0];

reg                                                                             avl_rdata_valid_g[5:0];
reg  [320                                        -1:0]                          avl_rdata_g[5:0];

reg [INT_AFI_DM_WIDTH                            -1:0]                          afi_dm;
reg [INT_AFI_DQ_WIDTH                            -1:0]                          afi_wdata;
reg [INT_AFI_DQ_WIDTH                            -1:0]                          afi_rdata;

reg [HARDIP_AFI_DM_WIDTH                         -1:0]                          afi_dm_int;
reg [HARDIP_AFI_DQ_WIDTH                         -1:0]                          afi_wdata_int;
reg [HARDIP_AFI_DQ_WIDTH                         -1:0]                          afi_rdata_int;


//------------------------------------------------------------------------------
// CFG Interface Assignments
//------------------------------------------------------------------------------

assign cfg_dramconfig =  cfg_dramconfig_wire;
assign cfg_caswrlat = cfg_caswrlat_wire;
assign cfg_addlat = cfg_addlat_wire;
assign cfg_tcl = cfg_tcl_wire;
assign cfg_trfc = cfg_trfc_wire;
assign cfg_trefi = cfg_trefi_wire;
assign cfg_twr = cfg_twr_wire;
assign cfg_tmrd = cfg_tmrd_wire;
assign cfg_coladdrwidth = cfg_coladdrwidth_wire;
assign cfg_rowaddrwidth = cfg_rowaddrwidth_wire;
assign cfg_bankaddrwidth = cfg_bankaddrwidth_wire;
assign cfg_csaddrwidth = cfg_csaddrwidth_wire;
assign cfg_interfacewidth = cfg_interfacewidth_wire;
assign cfg_devicewidth = cfg_devicewidth_wire;

//------------------------------------------------------------------------------
// Sideband
//------------------------------------------------------------------------------
assign local_refresh_chip_wire = {{2-MEM_IF_CS_WIDTH{local_refresh_chip}}, local_refresh_chip};
assign local_self_rfsh_chip_wire = {{2-MEM_IF_CS_WIDTH{local_self_rfsh_chip}}, local_self_rfsh_chip};
assign local_deep_powerdn_chip_wire = {{2-MEM_IF_CS_WIDTH{local_deep_powerdn_chip}}, local_deep_powerdn_chip};

////////////////////////////////////////////////////////////////////////////////
// START HIP TO SIP MAPING

//------------------------------------------------------------------------------
// Passing all ports parameters into same variable
//------------------------------------------------------------------------------

assign data_width[0] = AVL_DATA_WIDTH_PORT_0;
assign data_width[1] = AVL_DATA_WIDTH_PORT_1;
assign data_width[2] = AVL_DATA_WIDTH_PORT_2;
assign data_width[3] = AVL_DATA_WIDTH_PORT_3;
assign data_width[4] = AVL_DATA_WIDTH_PORT_4;
assign data_width[5] = AVL_DATA_WIDTH_PORT_5;

assign lsb_wfifo[0] = LSB_WFIFO_PORT_0;
assign lsb_wfifo[1] = LSB_WFIFO_PORT_1;
assign lsb_wfifo[2] = LSB_WFIFO_PORT_2;
assign lsb_wfifo[3] = LSB_WFIFO_PORT_3;
assign lsb_wfifo[4] = LSB_WFIFO_PORT_4;
assign lsb_wfifo[5] = LSB_WFIFO_PORT_5;

assign msb_wfifo[0] = MSB_WFIFO_PORT_0;
assign msb_wfifo[1] = MSB_WFIFO_PORT_1;
assign msb_wfifo[2] = MSB_WFIFO_PORT_2;
assign msb_wfifo[3] = MSB_WFIFO_PORT_3;
assign msb_wfifo[4] = MSB_WFIFO_PORT_4;
assign msb_wfifo[5] = MSB_WFIFO_PORT_5;

assign lsb_rfifo[0] = LSB_RFIFO_PORT_0;
assign lsb_rfifo[1] = LSB_RFIFO_PORT_1;
assign lsb_rfifo[2] = LSB_RFIFO_PORT_2;
assign lsb_rfifo[3] = LSB_RFIFO_PORT_3;
assign lsb_rfifo[4] = LSB_RFIFO_PORT_4;
assign lsb_rfifo[5] = LSB_RFIFO_PORT_5;

//------------------------------------------------------------------------------
// Command path
//------------------------------------------------------------------------------

assign avl_ready_0 = o_a_mm_ready_0 ;
assign avl_ready_1 = o_a_mm_ready_1 ;
assign avl_ready_2 = o_a_mm_ready_2 ;
assign avl_ready_3 = o_a_mm_ready_3 ;
assign avl_ready_4 = o_a_mm_ready_4 ;
assign avl_ready_5 = o_a_mm_ready_5 ;

assign i_avst_cmd_data_0 = {{42-AVL_SIZE_WIDTH-34{1'b0}},avl_size_0,{34-AVL_ADDR_WIDTH_PORT_0-2{1'b0}},avl_addr_0,avl_write_req_0,avl_read_req_0};
assign i_avst_cmd_data_1 = {{42-AVL_SIZE_WIDTH-34{1'b0}},avl_size_1,{34-AVL_ADDR_WIDTH_PORT_1-2{1'b0}},avl_addr_1,avl_write_req_1,avl_read_req_1};
assign i_avst_cmd_data_2 = {{42-AVL_SIZE_WIDTH-34{1'b0}},avl_size_2,{34-AVL_ADDR_WIDTH_PORT_2-2{1'b0}},avl_addr_2,avl_write_req_2,avl_read_req_2};
assign i_avst_cmd_data_3 = {{42-AVL_SIZE_WIDTH-34{1'b0}},avl_size_3,{34-AVL_ADDR_WIDTH_PORT_3-2{1'b0}},avl_addr_3,avl_write_req_3,avl_read_req_3};
assign i_avst_cmd_data_4 = {{42-AVL_SIZE_WIDTH-34{1'b0}},avl_size_4,{34-AVL_ADDR_WIDTH_PORT_4-2{1'b0}},avl_addr_4,avl_write_req_4,avl_read_req_4};
assign i_avst_cmd_data_5 = {{42-AVL_SIZE_WIDTH-34{1'b0}},avl_size_5,{34-AVL_ADDR_WIDTH_PORT_5-2{1'b0}},avl_addr_5,avl_write_req_5,avl_read_req_5};

//------------------------------------------------------------------------------
// Write data path
//------------------------------------------------------------------------------

assign i_avst_wr_data_0 = i_avst_wr_data_g[0];
assign i_avst_wr_data_1 = i_avst_wr_data_g[1];
assign i_avst_wr_data_2 = i_avst_wr_data_g[2];
assign i_avst_wr_data_3 = i_avst_wr_data_g[3];

assign avl_wdata_g[0] = avl_wdata_0;
assign avl_wdata_g[1] = avl_wdata_1;
assign avl_wdata_g[2] = avl_wdata_2;
assign avl_wdata_g[3] = avl_wdata_3;
assign avl_wdata_g[4] = avl_wdata_4;
assign avl_wdata_g[5] = avl_wdata_5;

assign avl_be_g[0] = avl_be_0;
assign avl_be_g[1] = avl_be_1;
assign avl_be_g[2] = avl_be_2;
assign avl_be_g[3] = avl_be_3;
assign avl_be_g[4] = avl_be_4;
assign avl_be_g[5] = avl_be_5;

//------------------------------------------------------------------------------
// Read data path
//------------------------------------------------------------------------------

assign avl_rdata_valid_0 = avl_rdata_valid_g[0];
assign avl_rdata_valid_1 = avl_rdata_valid_g[1];
assign avl_rdata_valid_2 = avl_rdata_valid_g[2];
assign avl_rdata_valid_3 = avl_rdata_valid_g[3];
assign avl_rdata_valid_4 = avl_rdata_valid_g[4];
assign avl_rdata_valid_5 = avl_rdata_valid_g[5];

assign avl_rdata_0 = avl_rdata_g[0];
assign avl_rdata_1 = avl_rdata_g[1];
assign avl_rdata_2 = avl_rdata_g[2];
assign avl_rdata_3 = avl_rdata_g[3];
assign avl_rdata_4 = avl_rdata_g[4];
assign avl_rdata_5 = avl_rdata_g[5];

reg [6-1:0] multi_fact_cmd0[5:0];
reg [6-1:0] multi_fact_cmd1[5:0];
reg [6-1:0] multi_fact_cmd2[5:0];
reg [6-1:0] multi_fact_cmd3[5:0];
reg [6-1:0] multi_fact_cmd4[5:0];
reg [6-1:0] multi_fact_cmd5[5:0];

integer total_used_fifo,port_i,fifo_i,idx_0,idx_1,idx_2,idx_3,idx_4,idx_5,idx_6;
always_comb
begin

    for (port_i = 0; port_i < 6; port_i = port_i + 1'b1)
    begin : port_loop

        //----------------------------------------------------------------------
        // Read data path
        //----------------------------------------------------------------------

        if (lsb_rfifo[port_i] == 0)
             avl_rdata_valid_g[port_i] = o_rd_avst_valid_0;
        else if (lsb_rfifo[port_i] == 1)
             avl_rdata_valid_g[port_i] = o_rd_avst_valid_1;
        else if (lsb_rfifo[port_i] == 2)
             avl_rdata_valid_g[port_i] = o_rd_avst_valid_2;
        else if (lsb_rfifo[port_i] == 3)
             avl_rdata_valid_g[port_i] = o_rd_avst_valid_3;
        else
             avl_rdata_valid_g[port_i] = 0;

        if ((data_width[port_i] == 32) || (data_width[port_i] == 48))
            if (lsb_rfifo[port_i] == 0)
                avl_rdata_g[port_i] = {{(320 - DATA_WIDTH_FIFO0_32){1'b0}},o_rd_avst_data_0[DATA_WIDTH_FIFO0_32 - 1 : 0]};
            else if (lsb_rfifo[port_i] == 1)
                avl_rdata_g[port_i] = {{(320 - DATA_WIDTH_FIFO0_32){1'b0}},o_rd_avst_data_1[DATA_WIDTH_FIFO0_32 - 1 : 0]};
            else if (lsb_rfifo[port_i] == 2)
                avl_rdata_g[port_i] = {{(320 - DATA_WIDTH_FIFO0_32){1'b0}},o_rd_avst_data_2[DATA_WIDTH_FIFO0_32 - 1 : 0]};
            else if (lsb_rfifo[port_i] == 3)
                avl_rdata_g[port_i] = {{(320 - DATA_WIDTH_FIFO0_32){1'b0}},o_rd_avst_data_3[DATA_WIDTH_FIFO0_32 - 1 : 0]};
            else
                avl_rdata_g[port_i] = 320'd0;
        else if ((data_width[port_i] == 64) || (data_width[port_i] == 80))
            if (lsb_rfifo[port_i] == 0)
                avl_rdata_g[port_i] = {{(320 - DATA_WIDTH_FIFO0_64){1'b0}},o_rd_avst_data_0[DATA_WIDTH_FIFO0_64 - 1 : 0]};
            else if (lsb_rfifo[port_i] == 1)
                avl_rdata_g[port_i] = {{(320 - DATA_WIDTH_FIFO0_64){1'b0}},o_rd_avst_data_1[DATA_WIDTH_FIFO0_64 - 1 : 0]};
            else if (lsb_rfifo[port_i] == 2)
                avl_rdata_g[port_i] = {{(320 - DATA_WIDTH_FIFO0_64){1'b0}},o_rd_avst_data_2[DATA_WIDTH_FIFO0_64 - 1 : 0]};
            else if (lsb_rfifo[port_i] == 3)
                avl_rdata_g[port_i] = {{(320 - DATA_WIDTH_FIFO0_64){1'b0}},o_rd_avst_data_3[DATA_WIDTH_FIFO0_64 - 1 : 0]};
            else
                avl_rdata_g[port_i] = 320'd0;
        else if ((data_width[port_i] == 128) || (data_width[port_i] == 160))
            if (lsb_rfifo[port_i] == 0)
                avl_rdata_g[port_i] = {{(320 - DATA_WIDTH_FIFO0_64 * 2){1'b0}},o_rd_avst_data_1[DATA_WIDTH_FIFO0_64 - 1 : 0],o_rd_avst_data_0[DATA_WIDTH_FIFO0_64 - 1 : 0]};
            else if (lsb_rfifo[port_i] == 2)
                avl_rdata_g[port_i] = {{(320 - DATA_WIDTH_FIFO0_64 * 2){1'b0}},o_rd_avst_data_3[DATA_WIDTH_FIFO0_64 - 1 : 0],o_rd_avst_data_2[DATA_WIDTH_FIFO0_64 - 1 : 0]};
            else
                avl_rdata_g[port_i] = 320'd0;
        else if ((data_width[port_i] == 256) || (data_width[port_i] == 320))
                avl_rdata_g[port_i] = {{(320 - DATA_WIDTH_FIFO0_64 * 4){1'b0}},o_rd_avst_data_3[DATA_WIDTH_FIFO0_64 - 1 : 0],o_rd_avst_data_2[DATA_WIDTH_FIFO0_64 - 1 : 0],o_rd_avst_data_1[DATA_WIDTH_FIFO0_64 - 1 : 0],o_rd_avst_data_0[DATA_WIDTH_FIFO0_64 - 1 : 0]};
        else
            avl_rdata_g[port_i] = 320'd0;
    end

    for (fifo_i = 0; fifo_i < 4; fifo_i = fifo_i + 1)
    begin : fifo_loop

        //----------------------------------------------------------------------
        // Write data path
        //----------------------------------------------------------------------

        if ((lsb_wfifo[0] <= fifo_i) && (msb_wfifo[0] >= fifo_i))
            if ((data_width[0] == 32) || (data_width[0] == 48))
                i_avst_wr_data_g[fifo_i] = {{ZERO_PAD_WIDTH_BE_32{1'b0}},avl_be_g[0][BE_WIDTH_FIFO0_32 - 1 : 0],{ZERO_PAD_WIDTH_DT_32{1'b0}},avl_wdata_g[0][DATA_WIDTH_FIFO0_32 - 1 : 0]};
            else if (fifo_i - lsb_wfifo[0] == 0)
                i_avst_wr_data_g[fifo_i] = {{ZERO_PAD_WIDTH_BE_64{1'b0}},avl_be_g[0][BE_WIDTH_FIFO0_64 - 1 : 0],{ZERO_PAD_WIDTH_DT_64{1'b0}},avl_wdata_g[0][DATA_WIDTH_FIFO0_64 - 1 : 0]};
            else if (fifo_i - lsb_wfifo[0] == 1)
                i_avst_wr_data_g[fifo_i] = {{ZERO_PAD_WIDTH_BE_64{1'b0}},avl_be_g[0][BE_WIDTH_FIFO1_64 - 1 : BE_WIDTH_FIFO0_64],{ZERO_PAD_WIDTH_DT_64{1'b0}},avl_wdata_g[0][DATA_WIDTH_FIFO1_64 - 1 : DATA_WIDTH_FIFO0_64]};
            else if (fifo_i - lsb_wfifo[0] == 2)
                i_avst_wr_data_g[fifo_i] = {{ZERO_PAD_WIDTH_BE_64{1'b0}},avl_be_g[0][BE_WIDTH_FIFO2_64 - 1 : BE_WIDTH_FIFO1_64],{ZERO_PAD_WIDTH_DT_64{1'b0}},avl_wdata_g[0][DATA_WIDTH_FIFO2_64 - 1 : DATA_WIDTH_FIFO1_64]};
            else if (fifo_i - lsb_wfifo[0] == 3)
                i_avst_wr_data_g[fifo_i] = {{ZERO_PAD_WIDTH_BE_64{1'b0}},avl_be_g[0][BE_WIDTH_FIFO3_64 - 1 : BE_WIDTH_FIFO2_64],{ZERO_PAD_WIDTH_DT_64{1'b0}},avl_wdata_g[0][DATA_WIDTH_FIFO3_64 - 1 : DATA_WIDTH_FIFO2_64]};
            else
                i_avst_wr_data_g[fifo_i] = 90'd0;
        else if ((lsb_wfifo[1] <= fifo_i) && (msb_wfifo[1] >= fifo_i))
            if ((data_width[1] == 32) || (data_width[1] == 48))
                i_avst_wr_data_g[fifo_i] = {{ZERO_PAD_WIDTH_BE_32{1'b0}},avl_be_g[1][BE_WIDTH_FIFO0_32 - 1 : 0],{ZERO_PAD_WIDTH_DT_32{1'b0}},avl_wdata_g[1][DATA_WIDTH_FIFO0_32 - 1 : 0]};
            else if (fifo_i - lsb_wfifo[1] == 0)
                i_avst_wr_data_g[fifo_i] = {{ZERO_PAD_WIDTH_BE_64{1'b0}},avl_be_g[1][BE_WIDTH_FIFO0_64 - 1 : 0],{ZERO_PAD_WIDTH_DT_64{1'b0}},avl_wdata_g[1][DATA_WIDTH_FIFO0_64 - 1 : 0]};
            else if (fifo_i - lsb_wfifo[1] == 1)
                i_avst_wr_data_g[fifo_i] = {{ZERO_PAD_WIDTH_BE_64{1'b0}},avl_be_g[1][BE_WIDTH_FIFO1_64 - 1 : BE_WIDTH_FIFO0_64],{ZERO_PAD_WIDTH_DT_64{1'b0}},avl_wdata_g[1][DATA_WIDTH_FIFO1_64 - 1 : DATA_WIDTH_FIFO0_64]};
            else if (fifo_i - lsb_wfifo[1] == 2)
                i_avst_wr_data_g[fifo_i] = {{ZERO_PAD_WIDTH_BE_64{1'b0}},avl_be_g[1][BE_WIDTH_FIFO2_64 - 1 : BE_WIDTH_FIFO1_64],{ZERO_PAD_WIDTH_DT_64{1'b0}},avl_wdata_g[1][DATA_WIDTH_FIFO2_64 - 1 : DATA_WIDTH_FIFO1_64]};
            else if (fifo_i - lsb_wfifo[1] == 3)
                i_avst_wr_data_g[fifo_i] = {{ZERO_PAD_WIDTH_BE_64{1'b0}},avl_be_g[1][BE_WIDTH_FIFO3_64 - 1 : BE_WIDTH_FIFO2_64],{ZERO_PAD_WIDTH_DT_64{1'b0}},avl_wdata_g[1][DATA_WIDTH_FIFO3_64 - 1 : DATA_WIDTH_FIFO2_64]};
            else
                i_avst_wr_data_g[fifo_i] = 90'd0;
        else if ((lsb_wfifo[2] <= fifo_i) && (msb_wfifo[2] >= fifo_i))
            if ((data_width[2] == 32) || (data_width[2] == 48))
                i_avst_wr_data_g[fifo_i] = {{ZERO_PAD_WIDTH_BE_32{1'b0}},avl_be_g[2][BE_WIDTH_FIFO0_32 - 1 : 0],{ZERO_PAD_WIDTH_DT_32{1'b0}},avl_wdata_g[2][DATA_WIDTH_FIFO0_32 - 1 : 0]};
            else if (fifo_i - lsb_wfifo[2] == 0)
                i_avst_wr_data_g[fifo_i] = {{ZERO_PAD_WIDTH_BE_64{1'b0}},avl_be_g[2][BE_WIDTH_FIFO0_64 - 1 : 0],{ZERO_PAD_WIDTH_DT_64{1'b0}},avl_wdata_g[2][DATA_WIDTH_FIFO0_64 - 1 : 0]};
            else if (fifo_i - lsb_wfifo[2] == 1)
                i_avst_wr_data_g[fifo_i] = {{ZERO_PAD_WIDTH_BE_64{1'b0}},avl_be_g[2][BE_WIDTH_FIFO1_64 - 1 : BE_WIDTH_FIFO0_64],{ZERO_PAD_WIDTH_DT_64{1'b0}},avl_wdata_g[2][DATA_WIDTH_FIFO1_64 - 1 : DATA_WIDTH_FIFO0_64]};
            else if (fifo_i - lsb_wfifo[2] == 2)
                i_avst_wr_data_g[fifo_i] = {{ZERO_PAD_WIDTH_BE_64{1'b0}},avl_be_g[2][BE_WIDTH_FIFO2_64 - 1 : BE_WIDTH_FIFO1_64],{ZERO_PAD_WIDTH_DT_64{1'b0}},avl_wdata_g[2][DATA_WIDTH_FIFO2_64 - 1 : DATA_WIDTH_FIFO1_64]};
            else if (fifo_i - lsb_wfifo[2] == 3)
                i_avst_wr_data_g[fifo_i] = {{ZERO_PAD_WIDTH_BE_64{1'b0}},avl_be_g[2][BE_WIDTH_FIFO3_64 - 1 : BE_WIDTH_FIFO2_64],{ZERO_PAD_WIDTH_DT_64{1'b0}},avl_wdata_g[2][DATA_WIDTH_FIFO3_64 - 1 : DATA_WIDTH_FIFO2_64]};
            else
                i_avst_wr_data_g[fifo_i] = 90'd0;
        else if ((lsb_wfifo[3] <= fifo_i) && (msb_wfifo[3] >= fifo_i))
            if ((data_width[3] == 32) || (data_width[3] == 48))
                i_avst_wr_data_g[fifo_i] = {{ZERO_PAD_WIDTH_BE_32{1'b0}},avl_be_g[3][BE_WIDTH_FIFO0_32 - 1 : 0],{ZERO_PAD_WIDTH_DT_32{1'b0}},avl_wdata_g[3][DATA_WIDTH_FIFO0_32 - 1 : 0]};
            else if (fifo_i - lsb_wfifo[3] == 0)
                i_avst_wr_data_g[fifo_i] = {{ZERO_PAD_WIDTH_BE_64{1'b0}},avl_be_g[3][BE_WIDTH_FIFO0_64 - 1 : 0],{ZERO_PAD_WIDTH_DT_64{1'b0}},avl_wdata_g[3][DATA_WIDTH_FIFO0_64 - 1 : 0]};
            else if (fifo_i - lsb_wfifo[3] == 1)
                i_avst_wr_data_g[fifo_i] = {{ZERO_PAD_WIDTH_BE_64{1'b0}},avl_be_g[3][BE_WIDTH_FIFO1_64 - 1 : BE_WIDTH_FIFO0_64],{ZERO_PAD_WIDTH_DT_64{1'b0}},avl_wdata_g[3][DATA_WIDTH_FIFO1_64 - 1 : DATA_WIDTH_FIFO0_64]};
            else if (fifo_i - lsb_wfifo[3] == 2)
                i_avst_wr_data_g[fifo_i] = {{ZERO_PAD_WIDTH_BE_64{1'b0}},avl_be_g[3][BE_WIDTH_FIFO2_64 - 1 : BE_WIDTH_FIFO1_64],{ZERO_PAD_WIDTH_DT_64{1'b0}},avl_wdata_g[3][DATA_WIDTH_FIFO2_64 - 1 : DATA_WIDTH_FIFO1_64]};
            else if (fifo_i - lsb_wfifo[3] == 3)
                i_avst_wr_data_g[fifo_i] = {{ZERO_PAD_WIDTH_BE_64{1'b0}},avl_be_g[3][BE_WIDTH_FIFO3_64 - 1 : BE_WIDTH_FIFO2_64],{ZERO_PAD_WIDTH_DT_64{1'b0}},avl_wdata_g[3][DATA_WIDTH_FIFO3_64 - 1 : DATA_WIDTH_FIFO2_64]};
            else
                i_avst_wr_data_g[fifo_i] = 90'd0;
        else if ((lsb_wfifo[4] <= fifo_i) && (msb_wfifo[4] >= fifo_i))
            if ((data_width[4] == 32) || (data_width[4] == 48))
                i_avst_wr_data_g[fifo_i] = {{ZERO_PAD_WIDTH_BE_32{1'b0}},avl_be_g[4][BE_WIDTH_FIFO0_32 - 1 : 0],{ZERO_PAD_WIDTH_DT_32{1'b0}},avl_wdata_g[4][DATA_WIDTH_FIFO0_32 - 1 : 0]};
            else if (fifo_i - lsb_wfifo[4] == 0)
                i_avst_wr_data_g[fifo_i] = {{ZERO_PAD_WIDTH_BE_64{1'b0}},avl_be_g[4][BE_WIDTH_FIFO0_64 - 1 : 0],{ZERO_PAD_WIDTH_DT_64{1'b0}},avl_wdata_g[4][DATA_WIDTH_FIFO0_64 - 1 : 0]};
            else if (fifo_i - lsb_wfifo[4] == 1)
                i_avst_wr_data_g[fifo_i] = {{ZERO_PAD_WIDTH_BE_64{1'b0}},avl_be_g[4][BE_WIDTH_FIFO1_64 - 1 : BE_WIDTH_FIFO0_64],{ZERO_PAD_WIDTH_DT_64{1'b0}},avl_wdata_g[4][DATA_WIDTH_FIFO1_64 - 1 : DATA_WIDTH_FIFO0_64]};
            else if (fifo_i - lsb_wfifo[4] == 2)
                i_avst_wr_data_g[fifo_i] = {{ZERO_PAD_WIDTH_BE_64{1'b0}},avl_be_g[4][BE_WIDTH_FIFO2_64 - 1 : BE_WIDTH_FIFO1_64],{ZERO_PAD_WIDTH_DT_64{1'b0}},avl_wdata_g[4][DATA_WIDTH_FIFO2_64 - 1 : DATA_WIDTH_FIFO1_64]};
            else if (fifo_i - lsb_wfifo[4] == 3)
                i_avst_wr_data_g[fifo_i] = {{ZERO_PAD_WIDTH_BE_64{1'b0}},avl_be_g[4][BE_WIDTH_FIFO3_64 - 1 : BE_WIDTH_FIFO2_64],{ZERO_PAD_WIDTH_DT_64{1'b0}},avl_wdata_g[4][DATA_WIDTH_FIFO3_64 - 1 : DATA_WIDTH_FIFO2_64]};
            else
                i_avst_wr_data_g[fifo_i] = 90'd0;
        else if ((lsb_wfifo[5] <= fifo_i) && (msb_wfifo[5] >= fifo_i))
            if ((data_width[5] == 32) || (data_width[5] == 48))
                i_avst_wr_data_g[fifo_i] = {{ZERO_PAD_WIDTH_BE_32{1'b0}},avl_be_g[5][BE_WIDTH_FIFO0_32 - 1 : 0],{ZERO_PAD_WIDTH_DT_32{1'b0}},avl_wdata_g[5][DATA_WIDTH_FIFO0_32 - 1 : 0]};
            else if (fifo_i - lsb_wfifo[5] == 0)
                i_avst_wr_data_g[fifo_i] = {{ZERO_PAD_WIDTH_BE_64{1'b0}},avl_be_g[5][BE_WIDTH_FIFO0_64 - 1 : 0],{ZERO_PAD_WIDTH_DT_64{1'b0}},avl_wdata_g[5][DATA_WIDTH_FIFO0_64 - 1 : 0]};
            else if (fifo_i - lsb_wfifo[5] == 1)
                i_avst_wr_data_g[fifo_i] = {{ZERO_PAD_WIDTH_BE_64{1'b0}},avl_be_g[5][BE_WIDTH_FIFO1_64 - 1 : BE_WIDTH_FIFO0_64],{ZERO_PAD_WIDTH_DT_64{1'b0}},avl_wdata_g[5][DATA_WIDTH_FIFO1_64 - 1 : DATA_WIDTH_FIFO0_64]};
            else if (fifo_i - lsb_wfifo[5] == 2)
                i_avst_wr_data_g[fifo_i] = {{ZERO_PAD_WIDTH_BE_64{1'b0}},avl_be_g[5][BE_WIDTH_FIFO2_64 - 1 : BE_WIDTH_FIFO1_64],{ZERO_PAD_WIDTH_DT_64{1'b0}},avl_wdata_g[5][DATA_WIDTH_FIFO2_64 - 1 : DATA_WIDTH_FIFO1_64]};
            else if (fifo_i - lsb_wfifo[5] == 3)
                i_avst_wr_data_g[fifo_i] = {{ZERO_PAD_WIDTH_BE_64{1'b0}},avl_be_g[5][BE_WIDTH_FIFO3_64 - 1 : BE_WIDTH_FIFO2_64],{ZERO_PAD_WIDTH_DT_64{1'b0}},avl_wdata_g[5][DATA_WIDTH_FIFO3_64 - 1 : DATA_WIDTH_FIFO2_64]};
            else
                i_avst_wr_data_g[fifo_i] = 90'd0;
        else
            i_avst_wr_data_g[fifo_i] = 90'd0;
    end

        //----------------------------------------------------------------------
        // Afi
        //----------------------------------------------------------------------

    if (HARD_PHY == 1)
    begin
        afi_rdata_int = afi_rdata;
        afi_wdata = afi_wdata_int;
        afi_dm = afi_dm_int;
    end
    else
    begin
        for (idx_4 = 0; idx_4 < HARDIP_AFI_DQ_WIDTH; idx_4 = idx_4 + 1'b1)
        begin : afi_rdata_hip_to_sip
            if (idx_4 < INT_AFI_DQ_WIDTH/2)
                afi_rdata_int[idx_4] = afi_rdata[idx_4];
            else if ((idx_4 > (HARDIP_AFI_DQ_WIDTH / 2 - 1)) && (idx_4 < (HARDIP_AFI_DQ_WIDTH / 2 + INT_AFI_DQ_WIDTH / 2)))
                afi_rdata_int[idx_4] = afi_rdata[idx_4 - ((HARDIP_AFI_DQ_WIDTH /2) - (INT_AFI_DQ_WIDTH / 2))];
            else
                afi_rdata_int[idx_4] = 1'b0;
        end

        for (idx_5 = 0; idx_5 < INT_AFI_DQ_WIDTH; idx_5 = idx_5 + 1'b1)
        begin : afi_wdata_hip_to_sip
            if (idx_5 < (INT_AFI_DQ_WIDTH / 2))
                afi_wdata[idx_5] = afi_wdata_int[idx_5];
            else
                afi_wdata[idx_5] = afi_wdata_int[idx_5 + ((HARDIP_AFI_DQ_WIDTH / 2) - (INT_AFI_DQ_WIDTH / 2))];
        end

        for (idx_6 = 0; idx_6 < INT_AFI_DM_WIDTH; idx_6 = idx_6 + 1'b1)
        begin : afi_dm_hip_to_sip
            if (idx_6 < (INT_AFI_DM_WIDTH / 2))
                afi_dm[idx_6] = afi_dm_int[idx_6];
            else
                afi_dm[idx_6] = afi_dm_int[idx_6 + ((HARDIP_AFI_DM_WIDTH / 2) - (INT_AFI_DM_WIDTH / 2))];
        end
    end
end

// END HIP TO SIP MAPING
////////////////////////////////////////////////////////////////////////////////

////////////////////////////////////////////////////////////////////////////////
// START OTHERS ASSIGNMENT

assign local_cal_success = io_intaficalsuccess;
assign local_cal_fail = io_intaficalfail;

assign afi_init_req = '0;
assign afi_seq_busy_int = {HARDIP_TRACKING_WIDTH{afi_seq_busy[0]}};

// END OTHERS ASSIGNMENT
////////////////////////////////////////////////////////////////////////////////

////////////////////////////////////////////////////////////////////////////////
// START ATOM INSTANTIATION

// Those that mark with //HARD NET is harden wire between controller and phy.
// ASM / Fitter will fail if they're not connected properly

cyclonev_hmc hmc_inst (
    .portclk0                       ( mp_cmd_clk_0                      ), //SOFT NET
    .portclk1                       ( mp_cmd_clk_1                      ), //SOFT NET
    .portclk2                       ( mp_cmd_clk_2                      ), //SOFT NET
    .portclk3                       ( mp_cmd_clk_3                      ), //SOFT NET
    .portclk4                       ( mp_cmd_clk_4                      ), //SOFT NET
    .portclk5                       ( mp_cmd_clk_5                      ), //SOFT NET
    .iavstcmdresetn0                ( mp_cmd_reset_n_0                  ), //SOFT NET
    .iavstcmdresetn1                ( mp_cmd_reset_n_1                  ), //SOFT NET
    .iavstcmdresetn2                ( mp_cmd_reset_n_2                  ), //SOFT NET
    .iavstcmdresetn3                ( mp_cmd_reset_n_3                  ), //SOFT NET
    .iavstcmdresetn4                ( mp_cmd_reset_n_4                  ), //SOFT NET
    .iavstcmdresetn5                ( mp_cmd_reset_n_5                  ), //SOFT NET
    .iavstcmddata0                  ( i_avst_cmd_data_0                 ), //SOFT NET
    .iavstcmddata1                  ( i_avst_cmd_data_1                 ), //SOFT NET
    .iavstcmddata2                  ( i_avst_cmd_data_2                 ), //SOFT NET
    .iavstcmddata3                  ( i_avst_cmd_data_3                 ), //SOFT NET
    .iavstcmddata4                  ( i_avst_cmd_data_4                 ), //SOFT NET
    .iavstcmddata5                  ( i_avst_cmd_data_5                 ), //SOFT NET
    .oammready0                     ( o_a_mm_ready_0                    ), //SOFT NET
    .oammready1                     ( o_a_mm_ready_1                    ), //SOFT NET
    .oammready2                     ( o_a_mm_ready_2                    ), //SOFT NET
    .oammready3                     ( o_a_mm_ready_3                    ), //SOFT NET
    .oammready4                     ( o_a_mm_ready_4                    ), //SOFT NET
    .oammready5                     ( o_a_mm_ready_5                    ), //SOFT NET
    .iavstwrackready0               ( 1'b1                              ), //INTERNAL USE
    .iavstwrackready1               ( 1'b1                              ), //INTERNAL USE
    .iavstwrackready2               ( 1'b1                              ), //INTERNAL USE
    .iavstwrackready3               ( 1'b1                              ), //INTERNAL USE
    .iavstwrackready4               ( 1'b1                              ), //INTERNAL USE
    .iavstwrackready5               ( 1'b1                              ), //INTERNAL USE
    .owrackavstvalid0               ( o_wrack_avst_valid_0              ), //SOFT NET
    .owrackavstvalid1               ( o_wrack_avst_valid_1              ), //SOFT NET
    .owrackavstvalid2               ( o_wrack_avst_valid_2              ), //SOFT NET
    .owrackavstvalid3               ( o_wrack_avst_valid_3              ), //SOFT NET
    .owrackavstvalid4               ( o_wrack_avst_valid_4              ), //SOFT NET
    .owrackavstvalid5               ( o_wrack_avst_valid_5              ), //SOFT NET
    .owrackavstdata0                ( o_wrack_avst_data_0               ), //SOFT NET
    .owrackavstdata1                ( o_wrack_avst_data_1               ), //SOFT NET
    .owrackavstdata2                ( o_wrack_avst_data_2               ), //SOFT NET
    .owrackavstdata3                ( o_wrack_avst_data_3               ), //SOFT NET
    .owrackavstdata4                ( o_wrack_avst_data_4               ), //SOFT NET
    .owrackavstdata5                ( o_wrack_avst_data_5               ), //SOFT NET
    .iavstrdclk0                    ( mp_rfifo_clk_0                    ), //SOFT NET
    .iavstrdclk1                    ( mp_rfifo_clk_1                    ), //SOFT NET
    .iavstrdclk2                    ( mp_rfifo_clk_2                    ), //SOFT NET
    .iavstrdclk3                    ( mp_rfifo_clk_3                    ), //SOFT NET
    .iavstrdresetn0                 ( mp_rfifo_reset_n_0                ), //SOFT NET
    .iavstrdresetn1                 ( mp_rfifo_reset_n_1                ), //SOFT NET
    .iavstrdresetn2                 ( mp_rfifo_reset_n_2                ), //SOFT NET
    .iavstrdresetn3                 ( mp_rfifo_reset_n_3                ), //SOFT NET
    .ordavstvalid0                  ( o_rd_avst_valid_0                 ), //SOFT NET
    .ordavstvalid1                  ( o_rd_avst_valid_1                 ), //SOFT NET
    .ordavstvalid2                  ( o_rd_avst_valid_2                 ), //SOFT NET
    .ordavstvalid3                  ( o_rd_avst_valid_3                 ), //SOFT NET
    .ordavstdata0                   ( o_rd_avst_data_0                  ), //SOFT NET
    .ordavstdata1                   ( o_rd_avst_data_1                  ), //SOFT NET
    .ordavstdata2                   ( o_rd_avst_data_2                  ), //SOFT NET
    .ordavstdata3                   ( o_rd_avst_data_3                  ), //SOFT NET
    .iavstrdready0                  ( 1'b1                              ), //INTERNAL USE
    .iavstrdready1                  ( 1'b1                              ), //INTERNAL USE
    .iavstrdready2                  ( 1'b1                              ), //INTERNAL USE
    .iavstrdready3                  ( 1'b1                              ), //INTERNAL USE
    .iavstwrclk0                    ( mp_wfifo_clk_0                    ), //SOFT NET
    .iavstwrclk1                    ( mp_wfifo_clk_1                    ), //SOFT NET
    .iavstwrclk2                    ( mp_wfifo_clk_2                    ), //SOFT NET
    .iavstwrclk3                    ( mp_wfifo_clk_3                    ), //SOFT NET
    .iavstwrresetn0                 ( mp_wfifo_reset_n_0                ), //SOFT NET
    .iavstwrresetn1                 ( mp_wfifo_reset_n_1                ), //SOFT NET
    .iavstwrresetn2                 ( mp_wfifo_reset_n_2                ), //SOFT NET
    .iavstwrresetn3                 ( mp_wfifo_reset_n_3                ), //SOFT NET
    .iavstwrdata0                   ( i_avst_wr_data_0                  ), //SOFT NET
    .iavstwrdata1                   ( i_avst_wr_data_1                  ), //SOFT NET
    .iavstwrdata2                   ( i_avst_wr_data_2                  ), //SOFT NET
    .iavstwrdata3                   ( i_avst_wr_data_3                  ), //SOFT NET
    .bondingout1                    ( bonding_out_1                     ), //SOFT NET
    .bondingin1                     ( bonding_in_1                      ), //SOFT NET
    .bondingout2                    ( bonding_out_2                     ), //SOFT NET
    .bondingin2                     ( bonding_in_2                      ), //SOFT NET
    .bondingout3                    ( bonding_out_3                     ), //SOFT NET
    .bondingin3                     ( bonding_in_3                      ), //SOFT NET
    .localrefreshreq                ( local_refresh_req                 ), //SOFT NET
    .localrefreshchip               ( local_refresh_chip_wire           ), //SOFT NET
    .localdeeppowerdnreq            ( local_deep_powerdn_req            ), //SOFT NET
    .localdeeppowerdnchip           ( local_deep_powerdn_chip_wire      ), //SOFT NET
    .localselfrfshreq               ( local_self_rfsh_req               ), //SOFT NET
    .localselfrfshchip              ( local_self_rfsh_chip_wire         ), //SOFT NET
    .localrefreshack                ( local_refresh_ack                 ), //SOFT NET
    .localdeeppowerdnack            ( local_deep_powerdn_ack            ), //SOFT NET
    .localpowerdownack              ( local_powerdn_ack                 ), //SOFT NET
    .localselfrfshack               ( local_self_rfsh_ack               ), //SOFT NET
    .localstsctlempty	 	    ( local_sts_ctl_empty		), 
    .ctlinitreq		  	    ( ctl_init_req			),
    .localinitdone                  ( local_init_done                   ), //SOFT NET
    .afirstn                        ( afi_rst_n                         ), //HARD NET
    .afiba                          ( afi_ba                            ), //HARD NET
    .afiaddr                        ( afi_addr                          ), //HARD NET
    .aficke                         ( afi_cke                           ), //HARD NET
    .aficsn                         ( afi_cs_n                          ), //HARD NET
    .afirasn                        ( afi_ras_n                         ), //HARD NET
    .aficasn                        ( afi_cas_n                         ), //HARD NET
    .afiwen                         ( afi_we_n                          ), //HARD NET
    .afiodt                         ( afi_odt                           ), //HARD NET
    .afiwlat                        ( afi_wlat                          ), //HARD NET
    .afidqsburst                    ( afi_dqs_burst                     ), //HARD NET
    .afidm                          ( afi_dm_int                        ), //HARD NET
    .afiwdata                       ( afi_wdata_int                     ), //HARD NET
    .afiwdatavalid                  ( afi_wdata_valid                   ), //HARD NET
    .afirdataen                     ( afi_rdata_en                      ), //HARD NET
    .afirdataenfull                 ( afi_rdata_en_full                 ), //HARD NET
    .afirdata                       ( afi_rdata_int                     ), //HARD NET
    .afirdatavalid                  ( afi_rdata_valid                   ), //HARD NET
    .ctlcalsuccess                  ( afi_cal_success                   ), //HARD NET
    .ctlcalfail                     ( afi_cal_fail                      ), //HARD NET
    .ctlcalreq                      ( afi_cal_req                       ), //SOFT NET
    .ctlcalbytelaneseln             ( afi_cal_byte_lane_sel_n           ), //SOFT NET
    .ctlmemclkdisable               ( afi_mem_clk_disable               ), //HARD NET
    .afictlrefreshdone              ( afi_ctl_refresh_done              ), //SOFT NET
    .afiseqbusy                     ( afi_seq_busy_int                  ), //SOFT NET
    .afictllongidle                 ( afi_ctl_long_idle                 ), //SOFT NET
    .mmrclk                         ( csr_clk                           ), //SOFT NET
    .mmrresetn                      ( csr_reset_n                       ), //SOFT NET
    .mmrreadreq                     ( csr_read_req                      ), //SOFT NET
    .mmrwritereq                    ( csr_write_req                     ), //SOFT NET
    .mmrburstcount                  ( 2'b01                             ), //SOFT NET
    .mmrburstbegin                  ( 1'b1                              ), //SOFT NET
    .mmraddr                        ( csr_addr                          ), //SOFT NET
    .mmrwdata                       ( csr_wdata                         ), //SOFT NET
    .mmrbe                          ( csr_be                            ), //SOFT NET
    .mmrrdata                       ( csr_rdata                         ), //SOFT NET
    .mmrrdatavalid                  ( csr_rdata_valid                   ), //SOFT NET
    .mmrwaitrequest                 ( csr_waitrequest                   ), //SOFT NET
    .scclk                          ( 1'b0                              ), //INTERNAL USE
    .scresetn                       ( 1'b1                              ), //INTERNAL USE
    .screadreq                      ( 1'b0                              ), //INTERNAL USE
    .scwritereq                     ( 1'b0                              ), //INTERNAL USE
    .scburstcount                   ( 2'b0                              ), //INTERNAL USE
    .scburstbegin                   ( 1'b0                              ), //INTERNAL USE
    .scaddr                         ( 10'b0000000000                    ), //INTERNAL USE
    .scwdata                        ( 8'b0                              ), //INTERNAL USE
    .scbe                           ( 1'b0                              ), //INTERNAL USE
    .scrdata                        (                                   ), //INTERNAL USE
    .scrdatavalid                   (                                   ), //INTERNAL USE
    .scwaitrequest                  (                                   ), //INTERNAL USE
    .dramconfig                     ( cfg_dramconfig_wire               ), //SOFT NET
    .cfgcaswrlat                    ( cfg_caswrlat_wire                 ), //SOFT NET
    .cfgaddlat                      ( cfg_addlat_wire                   ), //SOFT NET
    .cfgtcl                         ( cfg_tcl_wire                      ), //SOFT NET
    .cfgtrfc                        ( cfg_trfc_wire                     ), //SOFT NET
    .cfgtrefi                       ( cfg_trefi_wire                    ), //SOFT NET
    .cfgtwr                         ( cfg_twr_wire                      ), //SOFT NET
    .cfgtmrd                        ( cfg_tmrd_wire                     ), //SOFT NET
    .cfgcoladdrwidth                ( cfg_coladdrwidth_wire             ), //SOFT NET
    .cfgrowaddrwidth                ( cfg_rowaddrwidth_wire             ), //SOFT NET
    .cfgbankaddrwidth               ( cfg_bankaddrwidth_wire            ), //SOFT NET
    .cfgcsaddrwidth                 ( cfg_csaddrwidth_wire              ), //SOFT NET
    .cfginterfacewidth              ( cfg_interfacewidth_wire           ), //SOFT NET
    .cfgdevicewidth                 ( cfg_devicewidth_wire              ), //SOFT NET
    .scanenable                     ( 1'b0                              ), //INTERNAL USE
    .ctlclk                         ( ctl_clk                           ), //HARD NET
    .ctlresetn                      ( ctl_reset_n                       )  //HARD NET
);

// Those that mark with // SYTH & SIM is used to force MMR signals in simulation
// Those that mark with // SYTH ONLY is only used for Quartus sythesis
defparam hmc_inst.attr_counter_one_mask                                  = VECT_ATTR_COUNTER_ONE_MASK;                       //SYTH & SIM
defparam hmc_inst.attr_counter_one_match                                 = VECT_ATTR_COUNTER_ONE_MATCH;                      //SYTH & SIM
defparam hmc_inst.attr_counter_one_reset                                 = ENUM_ATTR_COUNTER_ONE_RESET;                      //SYTH & SIM
defparam hmc_inst.attr_counter_zero_mask                                 = VECT_ATTR_COUNTER_ZERO_MASK;                      //SYTH & SIM
defparam hmc_inst.attr_counter_zero_match                                = VECT_ATTR_COUNTER_ZERO_MATCH;                     //SYTH & SIM
defparam hmc_inst.attr_counter_zero_reset                                = ENUM_ATTR_COUNTER_ZERO_RESET;                     //SYTH & SIM
defparam hmc_inst.attr_debug_select_byte                                 = VECT_ATTR_DEBUG_SELECT_BYTE;                      //SYTH & SIM
defparam hmc_inst.attr_static_config_valid                               = ENUM_ATTR_STATIC_CONFIG_VALID;                    //SYTH & SIM
defparam hmc_inst.auto_pch_enable_0                                      = ENUM_AUTO_PCH_ENABLE_0;                           //SYTH & SIM
defparam hmc_inst.auto_pch_enable_1                                      = ENUM_AUTO_PCH_ENABLE_1;                           //SYTH & SIM
defparam hmc_inst.auto_pch_enable_2                                      = ENUM_AUTO_PCH_ENABLE_2;                           //SYTH & SIM
defparam hmc_inst.auto_pch_enable_3                                      = ENUM_AUTO_PCH_ENABLE_3;                           //SYTH & SIM
defparam hmc_inst.auto_pch_enable_4                                      = ENUM_AUTO_PCH_ENABLE_4;                           //SYTH & SIM
defparam hmc_inst.auto_pch_enable_5                                      = ENUM_AUTO_PCH_ENABLE_5;                           //SYTH & SIM
defparam hmc_inst.cal_req                                                = ENUM_CAL_REQ;                                     //SYTH & SIM
defparam hmc_inst.cfg_burst_length                                       = ENUM_CFG_BURST_LENGTH;                            //SYTH & SIM
defparam hmc_inst.cfg_interface_width                                    = ENUM_CFG_INTERFACE_WIDTH;                         //SYTH & SIM
defparam hmc_inst.cfg_self_rfsh_exit_cycles                              = ENUM_CFG_SELF_RFSH_EXIT_CYCLES;                   //SYTH & SIM
defparam hmc_inst.cfg_starve_limit                                       = ENUM_CFG_STARVE_LIMIT;                            //SYTH & SIM
defparam hmc_inst.cfg_type                                               = ENUM_CFG_TYPE;                                    //SYTH & SIM
defparam hmc_inst.clock_off_0                                            = ENUM_CLOCK_OFF_0;                                 //SIM ONLY
defparam hmc_inst.clock_off_1                                            = ENUM_CLOCK_OFF_1;                                 //SIM ONLY
defparam hmc_inst.clock_off_2                                            = ENUM_CLOCK_OFF_2;                                 //SIM ONLY
defparam hmc_inst.clock_off_3                                            = ENUM_CLOCK_OFF_3;                                 //SIM ONLY
defparam hmc_inst.clock_off_4                                            = ENUM_CLOCK_OFF_4;                                 //SIM ONLY
defparam hmc_inst.clock_off_5                                            = ENUM_CLOCK_OFF_5;                                 //SIM ONLY
defparam hmc_inst.clr_intr                                               = ENUM_CLR_INTR;                                    //SIM ONLY
defparam hmc_inst.cmd_port_in_use_0                                      = ENUM_CMD_PORT_IN_USE_0;                           //SYTH ONLY
defparam hmc_inst.cmd_port_in_use_1                                      = ENUM_CMD_PORT_IN_USE_1;                           //SYTH ONLY
defparam hmc_inst.cmd_port_in_use_2                                      = ENUM_CMD_PORT_IN_USE_2;                           //SYTH ONLY
defparam hmc_inst.cmd_port_in_use_3                                      = ENUM_CMD_PORT_IN_USE_3;                           //SYTH ONLY
defparam hmc_inst.cmd_port_in_use_4                                      = ENUM_CMD_PORT_IN_USE_4;                           //SYTH ONLY
defparam hmc_inst.cmd_port_in_use_5                                      = ENUM_CMD_PORT_IN_USE_5;                           //SYTH ONLY
defparam hmc_inst.cport0_rdy_almost_full                                 = ENUM_CPORT0_RDY_ALMOST_FULL;                      //SYTH & SIM
defparam hmc_inst.cport0_rfifo_map                                       = ENUM_CPORT0_RFIFO_MAP;                            //SYTH & SIM
defparam hmc_inst.cport0_type                                            = ENUM_CPORT0_TYPE;                                 //SYTH & SIM
defparam hmc_inst.cport0_wfifo_map                                       = ENUM_CPORT0_WFIFO_MAP;                            //SYTH & SIM
defparam hmc_inst.cport1_rdy_almost_full                                 = ENUM_CPORT1_RDY_ALMOST_FULL;                      //SYTH & SIM
defparam hmc_inst.cport1_rfifo_map                                       = ENUM_CPORT1_RFIFO_MAP;                            //SYTH & SIM
defparam hmc_inst.cport1_type                                            = ENUM_CPORT1_TYPE;                                 //SYTH & SIM
defparam hmc_inst.cport1_wfifo_map                                       = ENUM_CPORT1_WFIFO_MAP;                            //SYTH & SIM
defparam hmc_inst.cport2_rdy_almost_full                                 = ENUM_CPORT2_RDY_ALMOST_FULL;                      //SYTH & SIM
defparam hmc_inst.cport2_rfifo_map                                       = ENUM_CPORT2_RFIFO_MAP;                            //SYTH & SIM
defparam hmc_inst.cport2_type                                            = ENUM_CPORT2_TYPE;                                 //SYTH & SIM
defparam hmc_inst.cport2_wfifo_map                                       = ENUM_CPORT2_WFIFO_MAP;                            //SYTH & SIM
defparam hmc_inst.cport3_rdy_almost_full                                 = ENUM_CPORT3_RDY_ALMOST_FULL;                      //SYTH & SIM
defparam hmc_inst.cport3_rfifo_map                                       = ENUM_CPORT3_RFIFO_MAP;                            //SYTH & SIM
defparam hmc_inst.cport3_type                                            = ENUM_CPORT3_TYPE;                                 //SYTH & SIM
defparam hmc_inst.cport3_wfifo_map                                       = ENUM_CPORT3_WFIFO_MAP;                            //SYTH & SIM
defparam hmc_inst.cport4_rdy_almost_full                                 = ENUM_CPORT4_RDY_ALMOST_FULL;                      //SYTH & SIM
defparam hmc_inst.cport4_rfifo_map                                       = ENUM_CPORT4_RFIFO_MAP;                            //SYTH & SIM
defparam hmc_inst.cport4_type                                            = ENUM_CPORT4_TYPE;                                 //SYTH & SIM
defparam hmc_inst.cport4_wfifo_map                                       = ENUM_CPORT4_WFIFO_MAP;                            //SYTH & SIM
defparam hmc_inst.cport5_rdy_almost_full                                 = ENUM_CPORT5_RDY_ALMOST_FULL;                      //SYTH & SIM
defparam hmc_inst.cport5_rfifo_map                                       = ENUM_CPORT5_RFIFO_MAP;                            //SYTH & SIM
defparam hmc_inst.cport5_type                                            = ENUM_CPORT5_TYPE;                                 //SYTH & SIM
defparam hmc_inst.cport5_wfifo_map                                       = ENUM_CPORT5_WFIFO_MAP;                            //SYTH & SIM
defparam hmc_inst.ctl_addr_order                                         = ENUM_CTL_ADDR_ORDER;                              //SYTH & SIM
defparam hmc_inst.ctl_ecc_enabled                                        = ENUM_CTL_ECC_ENABLED;                             //SYTH & SIM
defparam hmc_inst.ctl_ecc_rmw_enabled                                    = ENUM_CTL_ECC_RMW_ENABLED;                         //SYTH & SIM
defparam hmc_inst.ctl_regdimm_enabled                                    = ENUM_CTL_REGDIMM_ENABLED;                         //SIM ONLY
defparam hmc_inst.ctl_usr_refresh                                        = ENUM_CTL_USR_REFRESH;                             //SYTH & SIM
defparam hmc_inst.ctrl_width                                             = ENUM_CTRL_WIDTH;                                  //SYTH & SIM
defparam hmc_inst.cyc_to_rld_jars_0                                      = INTG_CYC_TO_RLD_JARS_0;                           //SYTH & SIM
defparam hmc_inst.cyc_to_rld_jars_1                                      = INTG_CYC_TO_RLD_JARS_1;                           //SYTH & SIM
defparam hmc_inst.cyc_to_rld_jars_2                                      = INTG_CYC_TO_RLD_JARS_2;                           //SYTH & SIM
defparam hmc_inst.cyc_to_rld_jars_3                                      = INTG_CYC_TO_RLD_JARS_3;                           //SYTH & SIM
defparam hmc_inst.cyc_to_rld_jars_4                                      = INTG_CYC_TO_RLD_JARS_4;                           //SYTH & SIM
defparam hmc_inst.cyc_to_rld_jars_5                                      = INTG_CYC_TO_RLD_JARS_5;                           //SYTH & SIM
defparam hmc_inst.power_saving_exit_cycles				 				 = INTG_POWER_SAVING_EXIT_CYCLES;		     		//SYTH & SIM
defparam hmc_inst.mem_clk_entry_cycles					 				 = INTG_MEM_CLK_ENTRY_CYCLES;			     		//SYTH & SIM
defparam hmc_inst.priority_remap					 					 = INTG_PRIORITY_REMAP;				     			//SIM ONLY
defparam hmc_inst.enable_burst_interrupt				 				 = ENUM_ENABLE_BURST_INTERRUPT;			     		//SYTH & SIM
defparam hmc_inst.enable_burst_terminate				 				 = ENUM_ENABLE_BURST_TERMINATE;   		     		//SYTH & SIM
defparam hmc_inst.delay_bonding                                          = ENUM_DELAY_BONDING;                               //SYTH & SIM
defparam hmc_inst.dfx_bypass_enable                                      = ENUM_DFX_BYPASS_ENABLE;                           //SYTH & SIM
defparam hmc_inst.disable_merging                                        = ENUM_DISABLE_MERGING;                             //SIM ONLY
defparam hmc_inst.ecc_dq_width                                           = ENUM_ECC_DQ_WIDTH;                                //SYTH ONLY
defparam hmc_inst.enable_atpg                                            = ENUM_ENABLE_ATPG;                                 //SYTH & SIM
defparam hmc_inst.enable_bonding_0                                       = ENUM_ENABLE_BONDING_0;                            //SYTH & SIM
defparam hmc_inst.enable_bonding_1                                       = ENUM_ENABLE_BONDING_1;                            //SYTH & SIM
defparam hmc_inst.enable_bonding_2                                       = ENUM_ENABLE_BONDING_2;                            //SYTH & SIM
defparam hmc_inst.enable_bonding_3                                       = ENUM_ENABLE_BONDING_3;                            //SYTH & SIM
defparam hmc_inst.enable_bonding_4                                       = ENUM_ENABLE_BONDING_4;                            //SYTH & SIM
defparam hmc_inst.enable_bonding_5                                       = ENUM_ENABLE_BONDING_5;                            //SYTH & SIM
defparam hmc_inst.enable_bonding_wrapback                                = ENUM_ENABLE_BONDING_WRAPBACK;                     //SYTH & SIM
defparam hmc_inst.enable_dqs_tracking                                    = ENUM_ENABLE_DQS_TRACKING;                         //SYTH & SIM
defparam hmc_inst.enable_ecc_code_overwrites                             = ENUM_ENABLE_ECC_CODE_OVERWRITES;                  //SYTH & SIM
defparam hmc_inst.enable_fast_exit_ppd                                   = ENUM_ENABLE_FAST_EXIT_PPD;                        //SYTH ONLY
defparam hmc_inst.enable_intr                                            = ENUM_ENABLE_INTR;                                 //SYTH & SIM
defparam hmc_inst.enable_no_dm                                           = ENUM_ENABLE_NO_DM;                                //SYTH & SIM
defparam hmc_inst.enable_pipelineglobal                                  = ENUM_ENABLE_PIPELINEGLOBAL;                       //SYTH & SIM
defparam hmc_inst.extra_ctl_clk_act_to_act                               = INTG_EXTRA_CTL_CLK_ACT_TO_ACT;                    //SYTH & SIM
defparam hmc_inst.extra_ctl_clk_act_to_act_diff_bank                     = INTG_EXTRA_CTL_CLK_ACT_TO_ACT_DIFF_BANK;          //SYTH & SIM
defparam hmc_inst.extra_ctl_clk_act_to_pch                               = INTG_EXTRA_CTL_CLK_ACT_TO_PCH;                    //SYTH & SIM
defparam hmc_inst.extra_ctl_clk_act_to_rdwr                              = INTG_EXTRA_CTL_CLK_ACT_TO_RDWR;                   //SYTH & SIM
defparam hmc_inst.extra_ctl_clk_arf_period                               = INTG_EXTRA_CTL_CLK_ARF_PERIOD;                    //SYTH & SIM
defparam hmc_inst.extra_ctl_clk_arf_to_valid                             = INTG_EXTRA_CTL_CLK_ARF_TO_VALID;                  //SYTH & SIM
defparam hmc_inst.extra_ctl_clk_four_act_to_act                          = INTG_EXTRA_CTL_CLK_FOUR_ACT_TO_ACT;               //SYTH & SIM
defparam hmc_inst.extra_ctl_clk_pch_all_to_valid                         = INTG_EXTRA_CTL_CLK_PCH_ALL_TO_VALID;              //SYTH & SIM
defparam hmc_inst.extra_ctl_clk_pch_to_valid                             = INTG_EXTRA_CTL_CLK_PCH_TO_VALID;                  //SYTH & SIM
defparam hmc_inst.extra_ctl_clk_pdn_period                               = INTG_EXTRA_CTL_CLK_PDN_PERIOD;                    //SYTH & SIM
defparam hmc_inst.extra_ctl_clk_pdn_to_valid                             = INTG_EXTRA_CTL_CLK_PDN_TO_VALID;                  //SYTH & SIM
defparam hmc_inst.extra_ctl_clk_rd_ap_to_valid                           = INTG_EXTRA_CTL_CLK_RD_AP_TO_VALID;                //SYTH & SIM
defparam hmc_inst.extra_ctl_clk_rd_to_pch                                = INTG_EXTRA_CTL_CLK_RD_TO_PCH;                     //SYTH & SIM
defparam hmc_inst.extra_ctl_clk_rd_to_rd                                 = INTG_EXTRA_CTL_CLK_RD_TO_RD;                      //SYTH & SIM
defparam hmc_inst.extra_ctl_clk_rd_to_rd_diff_chip                       = INTG_EXTRA_CTL_CLK_RD_TO_RD_DIFF_CHIP;            //SYTH & SIM
defparam hmc_inst.extra_ctl_clk_rd_to_wr                                 = INTG_EXTRA_CTL_CLK_RD_TO_WR;                      //SYTH & SIM
defparam hmc_inst.extra_ctl_clk_rd_to_wr_bc                              = INTG_EXTRA_CTL_CLK_RD_TO_WR_BC;                   //SYTH & SIM
defparam hmc_inst.extra_ctl_clk_rd_to_wr_diff_chip                       = INTG_EXTRA_CTL_CLK_RD_TO_WR_DIFF_CHIP;            //SYTH & SIM
defparam hmc_inst.extra_ctl_clk_srf_to_valid                             = INTG_EXTRA_CTL_CLK_SRF_TO_VALID;                  //SYTH & SIM
defparam hmc_inst.extra_ctl_clk_srf_to_zq_cal                            = INTG_EXTRA_CTL_CLK_SRF_TO_ZQ_CAL;                 //SYTH & SIM
defparam hmc_inst.extra_ctl_clk_wr_ap_to_valid                           = INTG_EXTRA_CTL_CLK_WR_AP_TO_VALID;                //SYTH & SIM
defparam hmc_inst.extra_ctl_clk_wr_to_pch                                = INTG_EXTRA_CTL_CLK_WR_TO_PCH;                     //SYTH & SIM
defparam hmc_inst.extra_ctl_clk_wr_to_rd                                 = INTG_EXTRA_CTL_CLK_WR_TO_RD;                      //SYTH & SIM
defparam hmc_inst.extra_ctl_clk_wr_to_rd_bc                              = INTG_EXTRA_CTL_CLK_WR_TO_RD_BC;                   //SYTH & SIM
defparam hmc_inst.extra_ctl_clk_wr_to_rd_diff_chip                       = INTG_EXTRA_CTL_CLK_WR_TO_RD_DIFF_CHIP;            //SYTH & SIM
defparam hmc_inst.extra_ctl_clk_wr_to_wr                                 = INTG_EXTRA_CTL_CLK_WR_TO_WR;                      //SYTH & SIM
defparam hmc_inst.extra_ctl_clk_wr_to_wr_diff_chip                       = INTG_EXTRA_CTL_CLK_WR_TO_WR_DIFF_CHIP;            //SYTH & SIM
defparam hmc_inst.ganged_arf                                             = ENUM_GANGED_ARF;                                  //SIM ONLY
defparam hmc_inst.gen_dbe                                                = ENUM_GEN_DBE;                                     //SIM ONLY
defparam hmc_inst.gen_sbe                                                = ENUM_GEN_SBE;                                     //SIM ONLY
defparam hmc_inst.inc_sync                                               = ENUM_INC_SYNC;                                    //SYTH & SIM
defparam hmc_inst.local_if_cs_width                                      = ENUM_LOCAL_IF_CS_WIDTH;                           //SYTH & SIM
defparam hmc_inst.mask_corr_dropped_intr                                 = ENUM_MASK_CORR_DROPPED_INTR;                      //SYTH & SIM
defparam hmc_inst.mask_dbe_intr                                          = ENUM_MASK_DBE_INTR;                               //SYTH & SIM
defparam hmc_inst.mask_sbe_intr                                          = ENUM_MASK_SBE_INTR;                               //SYTH & SIM
defparam hmc_inst.mem_auto_pd_cycles                                     = INTG_MEM_AUTO_PD_CYCLES;                          //SYTH & SIM
defparam hmc_inst.mem_if_al                                              = ENUM_MEM_IF_AL;                                   //SYTH & SIM
defparam hmc_inst.mem_if_bankaddr_width                                  = ENUM_MEM_IF_BANKADDR_WIDTH;                       //SYTH & SIM
defparam hmc_inst.mem_if_burstlength                                     = ENUM_MEM_IF_BURSTLENGTH;                          //SYTH ONLY
defparam hmc_inst.mem_if_coladdr_width                                   = ENUM_MEM_IF_COLADDR_WIDTH;                        //SYTH & SIM
defparam hmc_inst.mem_if_cs_per_rank                                     = ENUM_MEM_IF_CS_PER_RANK;                          //SYTH ONLY
defparam hmc_inst.mem_if_cs_width                                        = ENUM_MEM_IF_CS_WIDTH;                             //SYTH ONLY
defparam hmc_inst.mem_if_dq_per_chip                                     = ENUM_MEM_IF_DQ_PER_CHIP;                          //SYTH ONLY
defparam hmc_inst.mem_if_dqs_width                                       = ENUM_MEM_IF_DQS_WIDTH;                            //SYTH & SIM
defparam hmc_inst.mem_if_dwidth                                          = ENUM_MEM_IF_DWIDTH;                               //SYTH ONLY
defparam hmc_inst.mem_if_memtype                                         = ENUM_MEM_IF_MEMTYPE;                              //SYTH ONLY
defparam hmc_inst.mem_if_rowaddr_width                                   = ENUM_MEM_IF_ROWADDR_WIDTH;                        //SYTH & SIM
defparam hmc_inst.mem_if_speedbin                                        = ENUM_MEM_IF_SPEEDBIN;                             //SYTH ONLY
defparam hmc_inst.mem_if_tccd                                            = ENUM_MEM_IF_TCCD;                                 //SYTH & SIM
defparam hmc_inst.mem_if_tcl                                             = ENUM_MEM_IF_TCL;                                  //SYTH & SIM
defparam hmc_inst.mem_if_tcwl                                            = ENUM_MEM_IF_TCWL;                                 //SYTH & SIM
defparam hmc_inst.mem_if_tfaw                                            = ENUM_MEM_IF_TFAW;                                 //SYTH & SIM
defparam hmc_inst.mem_if_tmrd                                            = ENUM_MEM_IF_TMRD;                                 //SYTH & SIM
defparam hmc_inst.mem_if_tras                                            = ENUM_MEM_IF_TRAS;                                 //SYTH & SIM
defparam hmc_inst.mem_if_trc                                             = ENUM_MEM_IF_TRC;                                  //SYTH & SIM
defparam hmc_inst.mem_if_trcd                                            = ENUM_MEM_IF_TRCD;                                 //SYTH & SIM
defparam hmc_inst.mem_if_trefi                                           = INTG_MEM_IF_TREFI;                                //SYTH & SIM
defparam hmc_inst.mem_if_trfc                                            = INTG_MEM_IF_TRFC;                                 //SYTH & SIM
defparam hmc_inst.mem_if_trp                                             = ENUM_MEM_IF_TRP;                                  //SYTH & SIM
defparam hmc_inst.mem_if_trrd                                            = ENUM_MEM_IF_TRRD;                                 //SYTH & SIM
defparam hmc_inst.mem_if_trtp                                            = ENUM_MEM_IF_TRTP;                                 //SYTH & SIM
defparam hmc_inst.mem_if_twr                                             = ENUM_MEM_IF_TWR;                                  //SYTH & SIM
defparam hmc_inst.mem_if_twtr                                            = ENUM_MEM_IF_TWTR;                                 //SYTH & SIM
defparam hmc_inst.mmr_cfg_mem_bl                                         = ENUM_MMR_CFG_MEM_BL;                              //SYTH & SIM
defparam hmc_inst.output_regd                                            = ENUM_OUTPUT_REGD;                                 //SYTH & SIM
defparam hmc_inst.pdn_exit_cycles                                        = ENUM_PDN_EXIT_CYCLES;                             //SYTH & SIM
defparam hmc_inst.port0_width                                            = ENUM_PORT0_WIDTH;                                 //SYTH & SIM
defparam hmc_inst.port1_width                                            = ENUM_PORT1_WIDTH;                                 //SYTH & SIM
defparam hmc_inst.port2_width                                            = ENUM_PORT2_WIDTH;                                 //SYTH & SIM
defparam hmc_inst.port3_width                                            = ENUM_PORT3_WIDTH;                                 //SYTH & SIM
defparam hmc_inst.port4_width                                            = ENUM_PORT4_WIDTH;                                 //SYTH & SIM
defparam hmc_inst.port5_width                                            = ENUM_PORT5_WIDTH;                                 //SYTH & SIM
defparam hmc_inst.priority_0_0                                           = ENUM_PRIORITY_0_0;                                //SYTH ONLY
defparam hmc_inst.priority_0_1                                           = ENUM_PRIORITY_0_1;                                //SYTH ONLY
defparam hmc_inst.priority_0_2                                           = ENUM_PRIORITY_0_2;                                //SYTH ONLY
defparam hmc_inst.priority_0_3                                           = ENUM_PRIORITY_0_3;                                //SYTH ONLY
defparam hmc_inst.priority_0_4                                           = ENUM_PRIORITY_0_4;                                //SYTH ONLY
defparam hmc_inst.priority_0_5                                           = ENUM_PRIORITY_0_5;                                //SYTH ONLY
defparam hmc_inst.priority_1_0                                           = ENUM_PRIORITY_1_0;                                //SYTH ONLY
defparam hmc_inst.priority_1_1                                           = ENUM_PRIORITY_1_1;                                //SYTH ONLY
defparam hmc_inst.priority_1_2                                           = ENUM_PRIORITY_1_2;                                //SYTH ONLY
defparam hmc_inst.priority_1_3                                           = ENUM_PRIORITY_1_3;                                //SYTH ONLY
defparam hmc_inst.priority_1_4                                           = ENUM_PRIORITY_1_4;                                //SYTH ONLY
defparam hmc_inst.priority_1_5                                           = ENUM_PRIORITY_1_5;                                //SYTH ONLY
defparam hmc_inst.priority_2_0                                           = ENUM_PRIORITY_2_0;                                //SYTH ONLY
defparam hmc_inst.priority_2_1                                           = ENUM_PRIORITY_2_1;                                //SYTH ONLY
defparam hmc_inst.priority_2_2                                           = ENUM_PRIORITY_2_2;                                //SYTH ONLY
defparam hmc_inst.priority_2_3                                           = ENUM_PRIORITY_2_3;                                //SYTH ONLY
defparam hmc_inst.priority_2_4                                           = ENUM_PRIORITY_2_4;                                //SYTH ONLY
defparam hmc_inst.priority_2_5                                           = ENUM_PRIORITY_2_5;                                //SYTH ONLY
defparam hmc_inst.priority_3_0                                           = ENUM_PRIORITY_3_0;                                //SYTH ONLY
defparam hmc_inst.priority_3_1                                           = ENUM_PRIORITY_3_1;                                //SYTH ONLY
defparam hmc_inst.priority_3_2                                           = ENUM_PRIORITY_3_2;                                //SYTH ONLY
defparam hmc_inst.priority_3_3                                           = ENUM_PRIORITY_3_3;                                //SYTH ONLY
defparam hmc_inst.priority_3_4                                           = ENUM_PRIORITY_3_4;                                //SYTH ONLY
defparam hmc_inst.priority_3_5                                           = ENUM_PRIORITY_3_5;                                //SYTH ONLY
defparam hmc_inst.priority_4_0                                           = ENUM_PRIORITY_4_0;                                //SYTH ONLY
defparam hmc_inst.priority_4_1                                           = ENUM_PRIORITY_4_1;                                //SYTH ONLY
defparam hmc_inst.priority_4_2                                           = ENUM_PRIORITY_4_2;                                //SYTH ONLY
defparam hmc_inst.priority_4_3                                           = ENUM_PRIORITY_4_3;                                //SYTH ONLY
defparam hmc_inst.priority_4_4                                           = ENUM_PRIORITY_4_4;                                //SYTH ONLY
defparam hmc_inst.priority_4_5                                           = ENUM_PRIORITY_4_5;                                //SYTH ONLY
defparam hmc_inst.priority_5_0                                           = ENUM_PRIORITY_5_0;                                //SYTH ONLY
defparam hmc_inst.priority_5_1                                           = ENUM_PRIORITY_5_1;                                //SYTH ONLY
defparam hmc_inst.priority_5_2                                           = ENUM_PRIORITY_5_2;                                //SYTH ONLY
defparam hmc_inst.priority_5_3                                           = ENUM_PRIORITY_5_3;                                //SYTH ONLY
defparam hmc_inst.priority_5_4                                           = ENUM_PRIORITY_5_4;                                //SYTH ONLY
defparam hmc_inst.priority_5_5                                           = ENUM_PRIORITY_5_5;                                //SYTH ONLY
defparam hmc_inst.priority_6_0                                           = ENUM_PRIORITY_6_0;                                //SYTH ONLY
defparam hmc_inst.priority_6_1                                           = ENUM_PRIORITY_6_1;                                //SYTH ONLY
defparam hmc_inst.priority_6_2                                           = ENUM_PRIORITY_6_2;                                //SYTH ONLY
defparam hmc_inst.priority_6_3                                           = ENUM_PRIORITY_6_3;                                //SYTH ONLY
defparam hmc_inst.priority_6_4                                           = ENUM_PRIORITY_6_4;                                //SYTH ONLY
defparam hmc_inst.priority_6_5                                           = ENUM_PRIORITY_6_5;                                //SYTH ONLY
defparam hmc_inst.priority_7_0                                           = ENUM_PRIORITY_7_0;                                //SYTH ONLY
defparam hmc_inst.priority_7_1                                           = ENUM_PRIORITY_7_1;                                //SYTH ONLY
defparam hmc_inst.priority_7_2                                           = ENUM_PRIORITY_7_2;                                //SYTH ONLY
defparam hmc_inst.priority_7_3                                           = ENUM_PRIORITY_7_3;                                //SYTH ONLY
defparam hmc_inst.priority_7_4                                           = ENUM_PRIORITY_7_4;                                //SYTH ONLY
defparam hmc_inst.priority_7_5                                           = ENUM_PRIORITY_7_5;                                //SYTH ONLY
defparam hmc_inst.rcfg_static_weight_0                                   = ENUM_RCFG_STATIC_WEIGHT_0;                        //SYTH & SIM
defparam hmc_inst.rcfg_static_weight_1                                   = ENUM_RCFG_STATIC_WEIGHT_1;                        //SYTH & SIM
defparam hmc_inst.rcfg_static_weight_2                                   = ENUM_RCFG_STATIC_WEIGHT_2;                        //SYTH & SIM
defparam hmc_inst.rcfg_static_weight_3                                   = ENUM_RCFG_STATIC_WEIGHT_3;                        //SYTH & SIM
defparam hmc_inst.rcfg_static_weight_4                                   = ENUM_RCFG_STATIC_WEIGHT_4;                        //SYTH & SIM
defparam hmc_inst.rcfg_static_weight_5                                   = ENUM_RCFG_STATIC_WEIGHT_5;                        //SYTH & SIM
defparam hmc_inst.rcfg_sum_wt_priority_0                                 = INTG_RCFG_SUM_WT_PRIORITY_0;                      //SYTH & SIM
defparam hmc_inst.rcfg_sum_wt_priority_1                                 = INTG_RCFG_SUM_WT_PRIORITY_1;                      //SYTH & SIM
defparam hmc_inst.rcfg_sum_wt_priority_2                                 = INTG_RCFG_SUM_WT_PRIORITY_2;                      //SYTH & SIM
defparam hmc_inst.rcfg_sum_wt_priority_3                                 = INTG_RCFG_SUM_WT_PRIORITY_3;                      //SYTH & SIM
defparam hmc_inst.rcfg_sum_wt_priority_4                                 = INTG_RCFG_SUM_WT_PRIORITY_4;                      //SYTH & SIM
defparam hmc_inst.rcfg_sum_wt_priority_5                                 = INTG_RCFG_SUM_WT_PRIORITY_5;                      //SYTH & SIM
defparam hmc_inst.rcfg_sum_wt_priority_6                                 = INTG_RCFG_SUM_WT_PRIORITY_6;                      //SYTH & SIM
defparam hmc_inst.rcfg_sum_wt_priority_7                                 = INTG_RCFG_SUM_WT_PRIORITY_7;                      //SYTH & SIM
defparam hmc_inst.rcfg_user_priority_0                                   = ENUM_RCFG_USER_PRIORITY_0;                        //SYTH & SIM
defparam hmc_inst.rcfg_user_priority_1                                   = ENUM_RCFG_USER_PRIORITY_1;                        //SYTH & SIM
defparam hmc_inst.rcfg_user_priority_2                                   = ENUM_RCFG_USER_PRIORITY_2;                        //SYTH & SIM
defparam hmc_inst.rcfg_user_priority_3                                   = ENUM_RCFG_USER_PRIORITY_3;                        //SYTH & SIM
defparam hmc_inst.rcfg_user_priority_4                                   = ENUM_RCFG_USER_PRIORITY_4;                        //SYTH & SIM
defparam hmc_inst.rcfg_user_priority_5                                   = ENUM_RCFG_USER_PRIORITY_5;                        //SYTH & SIM
defparam hmc_inst.rd_dwidth_0                                            = ENUM_RD_DWIDTH_0;                                 //SYTH ONLY
defparam hmc_inst.rd_dwidth_1                                            = ENUM_RD_DWIDTH_1;                                 //SYTH ONLY
defparam hmc_inst.rd_dwidth_2                                            = ENUM_RD_DWIDTH_2;                                 //SYTH ONLY
defparam hmc_inst.rd_dwidth_3                                            = ENUM_RD_DWIDTH_3;                                 //SYTH ONLY
defparam hmc_inst.rd_dwidth_4                                            = ENUM_RD_DWIDTH_4;                                 //SYTH ONLY
defparam hmc_inst.rd_dwidth_5                                            = ENUM_RD_DWIDTH_5;                                 //SYTH ONLY
defparam hmc_inst.rd_fifo_in_use_0                                       = ENUM_RD_FIFO_IN_USE_0;                            //SYTH ONLY
defparam hmc_inst.rd_fifo_in_use_1                                       = ENUM_RD_FIFO_IN_USE_1;                            //SYTH ONLY
defparam hmc_inst.rd_fifo_in_use_2                                       = ENUM_RD_FIFO_IN_USE_2;                            //SYTH ONLY
defparam hmc_inst.rd_fifo_in_use_3                                       = ENUM_RD_FIFO_IN_USE_3;                            //SYTH ONLY
defparam hmc_inst.rd_port_info_0                                         = ENUM_RD_PORT_INFO_0;                              //SYTH ONLY
defparam hmc_inst.rd_port_info_1                                         = ENUM_RD_PORT_INFO_1;                              //SYTH ONLY
defparam hmc_inst.rd_port_info_2                                         = ENUM_RD_PORT_INFO_2;                              //SYTH ONLY
defparam hmc_inst.rd_port_info_3                                         = ENUM_RD_PORT_INFO_3;                              //SYTH ONLY
defparam hmc_inst.rd_port_info_4                                         = ENUM_RD_PORT_INFO_4;                              //SYTH ONLY
defparam hmc_inst.rd_port_info_5                                         = ENUM_RD_PORT_INFO_5;                              //SYTH ONLY
defparam hmc_inst.read_odt_chip                                          = ENUM_READ_ODT_CHIP;                               //SYTH & SIM
defparam hmc_inst.reorder_data                                           = ENUM_REORDER_DATA;                                //SYTH & SIM
defparam hmc_inst.rfifo0_cport_map                                       = ENUM_RFIFO0_CPORT_MAP;                            //SYTH & SIM
defparam hmc_inst.rfifo1_cport_map                                       = ENUM_RFIFO1_CPORT_MAP;                            //SYTH & SIM
defparam hmc_inst.rfifo2_cport_map                                       = ENUM_RFIFO2_CPORT_MAP;                            //SYTH & SIM
defparam hmc_inst.rfifo3_cport_map                                       = ENUM_RFIFO3_CPORT_MAP;                            //SYTH & SIM
defparam hmc_inst.single_ready_0                                         = ENUM_SINGLE_READY_0;                              //SYTH & SIM
defparam hmc_inst.single_ready_1                                         = ENUM_SINGLE_READY_1;                              //SYTH & SIM
defparam hmc_inst.single_ready_2                                         = ENUM_SINGLE_READY_2;                              //SYTH & SIM
defparam hmc_inst.single_ready_3                                         = ENUM_SINGLE_READY_3;                              //SYTH & SIM
defparam hmc_inst.static_weight_0                                        = ENUM_STATIC_WEIGHT_0;                             //SYTH ONLY
defparam hmc_inst.static_weight_1                                        = ENUM_STATIC_WEIGHT_1;                             //SYTH ONLY
defparam hmc_inst.static_weight_2                                        = ENUM_STATIC_WEIGHT_2;                             //SYTH ONLY
defparam hmc_inst.static_weight_3                                        = ENUM_STATIC_WEIGHT_3;                             //SYTH ONLY
defparam hmc_inst.static_weight_4                                        = ENUM_STATIC_WEIGHT_4;                             //SYTH ONLY
defparam hmc_inst.static_weight_5                                        = ENUM_STATIC_WEIGHT_5;                             //SYTH ONLY
defparam hmc_inst.sum_wt_priority_0                                      = INTG_SUM_WT_PRIORITY_0;                           //SYTH ONLY
defparam hmc_inst.sum_wt_priority_1                                      = INTG_SUM_WT_PRIORITY_1;                           //SYTH ONLY
defparam hmc_inst.sum_wt_priority_2                                      = INTG_SUM_WT_PRIORITY_2;                           //SYTH ONLY
defparam hmc_inst.sum_wt_priority_3                                      = INTG_SUM_WT_PRIORITY_3;                           //SYTH ONLY
defparam hmc_inst.sum_wt_priority_4                                      = INTG_SUM_WT_PRIORITY_4;                           //SYTH ONLY
defparam hmc_inst.sum_wt_priority_5                                      = INTG_SUM_WT_PRIORITY_5;                           //SYTH ONLY
defparam hmc_inst.sum_wt_priority_6                                      = INTG_SUM_WT_PRIORITY_6;                           //SYTH ONLY
defparam hmc_inst.sum_wt_priority_7                                      = INTG_SUM_WT_PRIORITY_7;                           //SYTH ONLY
defparam hmc_inst.sync_mode_0                                            = ENUM_SYNC_MODE_0;                                 //SYTH & SIM
defparam hmc_inst.sync_mode_1                                            = ENUM_SYNC_MODE_1;                                 //SYTH & SIM
defparam hmc_inst.sync_mode_2                                            = ENUM_SYNC_MODE_2;                                 //SYTH & SIM
defparam hmc_inst.sync_mode_3                                            = ENUM_SYNC_MODE_3;                                 //SYTH & SIM
defparam hmc_inst.sync_mode_4                                            = ENUM_SYNC_MODE_4;                                 //SYTH & SIM
defparam hmc_inst.sync_mode_5                                            = ENUM_SYNC_MODE_5;                                 //SYTH & SIM
defparam hmc_inst.test_mode                                              = ENUM_TEST_MODE;                                   //SYTH & SIM
defparam hmc_inst.thld_jar1_0                                            = ENUM_THLD_JAR1_0;                                 //SYTH & SIM
defparam hmc_inst.thld_jar1_1                                            = ENUM_THLD_JAR1_1;                                 //SYTH & SIM
defparam hmc_inst.thld_jar1_2                                            = ENUM_THLD_JAR1_2;                                 //SYTH & SIM
defparam hmc_inst.thld_jar1_3                                            = ENUM_THLD_JAR1_3;                                 //SYTH & SIM
defparam hmc_inst.thld_jar1_4                                            = ENUM_THLD_JAR1_4;                                 //SYTH & SIM
defparam hmc_inst.thld_jar1_5                                            = ENUM_THLD_JAR1_5;                                 //SYTH & SIM
defparam hmc_inst.thld_jar2_0                                            = ENUM_THLD_JAR2_0;                                 //SYTH & SIM
defparam hmc_inst.thld_jar2_1                                            = ENUM_THLD_JAR2_1;                                 //SYTH & SIM
defparam hmc_inst.thld_jar2_2                                            = ENUM_THLD_JAR2_2;                                 //SYTH & SIM
defparam hmc_inst.thld_jar2_3                                            = ENUM_THLD_JAR2_3;                                 //SYTH & SIM
defparam hmc_inst.thld_jar2_4                                            = ENUM_THLD_JAR2_4;                                 //SYTH & SIM
defparam hmc_inst.thld_jar2_5                                            = ENUM_THLD_JAR2_5;                                 //SYTH & SIM
defparam hmc_inst.use_almost_empty_0                                     = ENUM_USE_ALMOST_EMPTY_0;                          //SYTH & SIM
defparam hmc_inst.use_almost_empty_1                                     = ENUM_USE_ALMOST_EMPTY_1;                          //SYTH & SIM
defparam hmc_inst.use_almost_empty_2                                     = ENUM_USE_ALMOST_EMPTY_2;                          //SYTH & SIM
defparam hmc_inst.use_almost_empty_3                                     = ENUM_USE_ALMOST_EMPTY_3;                          //SYTH & SIM
defparam hmc_inst.user_ecc_en                                            = ENUM_USER_ECC_EN;                                 //SYTH & SIM
defparam hmc_inst.user_priority_0                                        = ENUM_USER_PRIORITY_0;                             //SYTH ONLY
defparam hmc_inst.user_priority_1                                        = ENUM_USER_PRIORITY_1;                             //SYTH ONLY
defparam hmc_inst.user_priority_2                                        = ENUM_USER_PRIORITY_2;                             //SYTH ONLY
defparam hmc_inst.user_priority_3                                        = ENUM_USER_PRIORITY_3;                             //SYTH ONLY
defparam hmc_inst.user_priority_4                                        = ENUM_USER_PRIORITY_4;                             //SYTH ONLY
defparam hmc_inst.user_priority_5                                        = ENUM_USER_PRIORITY_5;                             //SYTH ONLY
defparam hmc_inst.wfifo0_cport_map                                       = ENUM_WFIFO0_CPORT_MAP;                            //SYTH & SIM
defparam hmc_inst.wfifo0_rdy_almost_full                                 = ENUM_WFIFO0_RDY_ALMOST_FULL;                      //SYTH & SIM
defparam hmc_inst.wfifo1_cport_map                                       = ENUM_WFIFO1_CPORT_MAP;                            //SYTH & SIM
defparam hmc_inst.wfifo1_rdy_almost_full                                 = ENUM_WFIFO1_RDY_ALMOST_FULL;                      //SYTH & SIM
defparam hmc_inst.wfifo2_cport_map                                       = ENUM_WFIFO2_CPORT_MAP;                            //SYTH & SIM
defparam hmc_inst.wfifo2_rdy_almost_full                                 = ENUM_WFIFO2_RDY_ALMOST_FULL;                      //SYTH & SIM
defparam hmc_inst.wfifo3_cport_map                                       = ENUM_WFIFO3_CPORT_MAP;                            //SYTH & SIM
defparam hmc_inst.wfifo3_rdy_almost_full                                 = ENUM_WFIFO3_RDY_ALMOST_FULL;                      //SYTH & SIM
defparam hmc_inst.wr_dwidth_0                                            = ENUM_WR_DWIDTH_0;                                 //SYTH ONLY
defparam hmc_inst.wr_dwidth_1                                            = ENUM_WR_DWIDTH_1;                                 //SYTH ONLY
defparam hmc_inst.wr_dwidth_2                                            = ENUM_WR_DWIDTH_2;                                 //SYTH ONLY
defparam hmc_inst.wr_dwidth_3                                            = ENUM_WR_DWIDTH_3;                                 //SYTH ONLY
defparam hmc_inst.wr_dwidth_4                                            = ENUM_WR_DWIDTH_4;                                 //SYTH ONLY
defparam hmc_inst.wr_dwidth_5                                            = ENUM_WR_DWIDTH_5;                                 //SYTH ONLY
defparam hmc_inst.wr_fifo_in_use_0                                       = ENUM_WR_FIFO_IN_USE_0;                            //SYTH ONLY
defparam hmc_inst.wr_fifo_in_use_1                                       = ENUM_WR_FIFO_IN_USE_1;                            //SYTH ONLY
defparam hmc_inst.wr_fifo_in_use_2                                       = ENUM_WR_FIFO_IN_USE_2;                            //SYTH ONLY
defparam hmc_inst.wr_fifo_in_use_3                                       = ENUM_WR_FIFO_IN_USE_3;                            //SYTH ONLY
defparam hmc_inst.wr_port_info_0                                         = ENUM_WR_PORT_INFO_0;                              //SYTH ONLY
defparam hmc_inst.wr_port_info_1                                         = ENUM_WR_PORT_INFO_1;                              //SYTH ONLY
defparam hmc_inst.wr_port_info_2                                         = ENUM_WR_PORT_INFO_2;                              //SYTH ONLY
defparam hmc_inst.wr_port_info_3                                         = ENUM_WR_PORT_INFO_3;                              //SYTH ONLY
defparam hmc_inst.wr_port_info_4                                         = ENUM_WR_PORT_INFO_4;                              //SYTH ONLY
defparam hmc_inst.wr_port_info_5                                         = ENUM_WR_PORT_INFO_5;                              //SYTH ONLY
defparam hmc_inst.write_odt_chip                                         = ENUM_WRITE_ODT_CHIP;                              //SYTH & SIM

// END ATOM INSTANTIATION
////////////////////////////////////////////////////////////////////////////////

////////////////////////////////////////////////////////////////////////////////
// START LOCAL FUNCTIONS

// END LOCAL FUNCTIONS
////////////////////////////////////////////////////////////////////////////////

endmodule
