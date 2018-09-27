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
// Top-level wrapper of 20nm hardened EMIF component.
//
///////////////////////////////////////////////////////////////////////////////
module ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_top #(

   // Interface properties
   parameter PROTOCOL_ENUM                           = "",
   parameter MEM_FORMAT_ENUM                         = "",
   parameter PHY_CONFIG_ENUM                         = "",
   parameter PHY_PING_PONG_EN                        = 0,
   parameter PHY_CORE_CLKS_SHARING_ENUM              = "",
   parameter PHY_HPS_ENABLE_EARLY_RELEASE            = 0,
   parameter IS_HPS                                  = 0,
   parameter IS_VID                                  = 0,
   parameter PHY_TARGET_IS_ES                        = 0,
   parameter PHY_TARGET_IS_ES2                       = 0,
   parameter PHY_TARGET_IS_PRODUCTION                = 1,
   parameter SILICON_REV                             = "",
   parameter PHY_HAS_DCC                             = 0,
   parameter PLL_NUM_OF_EXTRA_CLKS                   = 0,
   parameter USER_CLK_RATIO                          = 1,
   parameter PHY_HMC_CLK_RATIO                       = 1,
   parameter C2P_P2C_CLK_RATIO                       = 1,
   parameter DQS_BUS_MODE_ENUM                       = "",
   parameter MEM_BURST_LENGTH                        = 0,
   parameter MEM_DATA_MASK_EN                        = 1,
   parameter MEM_TTL_DATA_WIDTH                      = 0,
   parameter MEM_TTL_NUM_OF_READ_GROUPS              = 0,
   parameter MEM_TTL_NUM_OF_WRITE_GROUPS             = 0,

   // Core logic related properties
   parameter REGISTER_AFI                            = 0,

   // OCT-related properties
   parameter PHY_CALIBRATED_OCT                      = 1,
   parameter PHY_AC_CALIBRATED_OCT                   = 1,
   parameter PHY_CK_CALIBRATED_OCT                   = 1,
   parameter PHY_DATA_CALIBRATED_OCT                 = 1,
   parameter PHY_USERMODE_OCT                        = 1,
   parameter PHY_PERIODIC_OCT_RECAL                  = 1,

   // Debug parameters
   parameter DIAG_SIM_REGTEST_MODE                   = 0,
   parameter DIAG_SYNTH_FOR_SIM                      = 0,
   parameter DIAG_FAST_SIM                           = 0,
   parameter DIAG_VERBOSE_IOAUX                      = 0,
   parameter DIAG_INTERFACE_ID                       = 0,
   parameter DIAG_CPA_OUT_1_EN                       = 0,
   parameter DIAG_USE_CPA_LOCK                       = 1,
   parameter DIAG_ECLIPSE_DEBUG                      = 0,
   parameter DIAG_EXPORT_VJI                         = 0,
   parameter DIAG_USE_ABSTRACT_PHY                   = 0,
   parameter DIAG_ABSTRACT_PHY_WLAT                  = 3,
   parameter DIAG_ABSTRACT_PHY_RLAT                  = 8,
   parameter ABPHY_WRITE_PROTOCOL                    = 1,

   // Calibration algorithm and parameter table
   parameter SEQ_CODE_HEX_FILENAME                   = "",

   parameter SEQ_SYNTH_OSC_FREQ_MHZ                  = 0,
   parameter SEQ_SYNTH_PARAMS_HEX_FILENAME           = "",
   parameter SEQ_SYNTH_CPU_CLK_DIVIDE                = 0,
   parameter SEQ_SYNTH_CAL_CLK_DIVIDE                = 0,

   parameter SEQ_SIM_OSC_FREQ_MHZ                    = 0,
   parameter SEQ_SIM_PARAMS_HEX_FILENAME             = "",
   parameter SEQ_SIM_CPU_CLK_DIVIDE                  = 0,
   parameter SEQ_SIM_CAL_CLK_DIVIDE                  = 0,

   // Family traits
   parameter LANES_PER_TILE                          = 1,
   parameter PINS_PER_LANE                           = 1,
   parameter OCT_CONTROL_WIDTH                       = 1,

   // PLL parameters
   parameter PLL_VCO_FREQ_MHZ_INT                    = 0,
   parameter PLL_REF_CLK_FREQ_PS                     = 0,
   parameter PLL_VCO_TO_MEM_CLK_FREQ_RATIO           = 1,
   parameter PLL_PHY_CLK_VCO_PHASE                   = 0,
   parameter PLL_SIM_VCO_FREQ_PS                     = 0,
   parameter PLL_SIM_PHYCLK_0_FREQ_PS                = 0,
   parameter PLL_SIM_PHYCLK_1_FREQ_PS                = 0,
   parameter PLL_SIM_PHYCLK_FB_FREQ_PS               = 0,
   parameter PLL_SIM_PHY_CLK_VCO_PHASE_PS            = 0,
   parameter PLL_SIM_CAL_SLAVE_CLK_FREQ_PS           = 0,
   parameter PLL_SIM_CAL_MASTER_CLK_FREQ_PS          = 0,
   parameter PLL_REF_CLK_FREQ_PS_STR                 = "",
   parameter PLL_VCO_FREQ_PS_STR                     = "",
   parameter PLL_M_CNT_HIGH                          = 0,
   parameter PLL_M_CNT_LOW                           = 0,
   parameter PLL_N_CNT_HIGH                          = 0,
   parameter PLL_N_CNT_LOW                           = 0,
   parameter PLL_M_CNT_BYPASS_EN                     = "",
   parameter PLL_N_CNT_BYPASS_EN                     = "",
   parameter PLL_M_CNT_EVEN_DUTY_EN                  = "",
   parameter PLL_N_CNT_EVEN_DUTY_EN                  = "",
   parameter PLL_FBCLK_MUX_1                         = "",
   parameter PLL_FBCLK_MUX_2                         = "",
   parameter PLL_M_CNT_IN_SRC                        = "",
   parameter PLL_CP_SETTING                          = "",
   parameter PLL_BW_CTRL                             = "",
   parameter PLL_BW_SEL                              = "",
   parameter PLL_C_CNT_HIGH_0                        = 0,
   parameter PLL_C_CNT_LOW_0                         = 0,
   parameter PLL_C_CNT_PRST_0                        = 0,
   parameter PLL_C_CNT_PH_MUX_PRST_0                 = 0,
   parameter PLL_C_CNT_BYPASS_EN_0                   = "",
   parameter PLL_C_CNT_EVEN_DUTY_EN_0                = "",
   parameter PLL_C_CNT_HIGH_1                        = 0,
   parameter PLL_C_CNT_LOW_1                         = 0,
   parameter PLL_C_CNT_PRST_1                        = 0,
   parameter PLL_C_CNT_PH_MUX_PRST_1                 = 0,
   parameter PLL_C_CNT_BYPASS_EN_1                   = "",
   parameter PLL_C_CNT_EVEN_DUTY_EN_1                = "",
   parameter PLL_C_CNT_HIGH_2                        = 0,
   parameter PLL_C_CNT_LOW_2                         = 0,
   parameter PLL_C_CNT_PRST_2                        = 0,
   parameter PLL_C_CNT_PH_MUX_PRST_2                 = 0,
   parameter PLL_C_CNT_BYPASS_EN_2                   = "",
   parameter PLL_C_CNT_EVEN_DUTY_EN_2                = "",
   parameter PLL_C_CNT_HIGH_3                        = 0,
   parameter PLL_C_CNT_LOW_3                         = 0,
   parameter PLL_C_CNT_PRST_3                        = 0,
   parameter PLL_C_CNT_PH_MUX_PRST_3                 = 0,
   parameter PLL_C_CNT_BYPASS_EN_3                   = "",
   parameter PLL_C_CNT_EVEN_DUTY_EN_3                = "",
   parameter PLL_C_CNT_HIGH_4                        = 0,
   parameter PLL_C_CNT_LOW_4                         = 0,
   parameter PLL_C_CNT_PRST_4                        = 0,
   parameter PLL_C_CNT_PH_MUX_PRST_4                 = 0,
   parameter PLL_C_CNT_BYPASS_EN_4                   = "",
   parameter PLL_C_CNT_EVEN_DUTY_EN_4                = "",
   parameter PLL_C_CNT_HIGH_5                        = 0,
   parameter PLL_C_CNT_LOW_5                         = 0,
   parameter PLL_C_CNT_PRST_5                        = 0,
   parameter PLL_C_CNT_PH_MUX_PRST_5                 = 0,
   parameter PLL_C_CNT_BYPASS_EN_5                   = "",
   parameter PLL_C_CNT_EVEN_DUTY_EN_5                = "",
   parameter PLL_C_CNT_HIGH_6                        = 0,
   parameter PLL_C_CNT_LOW_6                         = 0,
   parameter PLL_C_CNT_PRST_6                        = 0,
   parameter PLL_C_CNT_PH_MUX_PRST_6                 = 0,
   parameter PLL_C_CNT_BYPASS_EN_6                   = "",
   parameter PLL_C_CNT_EVEN_DUTY_EN_6                = "",
   parameter PLL_C_CNT_HIGH_7                        = 0,
   parameter PLL_C_CNT_LOW_7                         = 0,
   parameter PLL_C_CNT_PRST_7                        = 0,
   parameter PLL_C_CNT_PH_MUX_PRST_7                 = 0,
   parameter PLL_C_CNT_BYPASS_EN_7                   = "",
   parameter PLL_C_CNT_EVEN_DUTY_EN_7                = "",
   parameter PLL_C_CNT_HIGH_8                        = 0,
   parameter PLL_C_CNT_LOW_8                         = 0,
   parameter PLL_C_CNT_PRST_8                        = 0,
   parameter PLL_C_CNT_PH_MUX_PRST_8                 = 0,
   parameter PLL_C_CNT_BYPASS_EN_8                   = "",
   parameter PLL_C_CNT_EVEN_DUTY_EN_8                = "",
   parameter PLL_C_CNT_FREQ_PS_STR_0                 = "",
   parameter PLL_C_CNT_PHASE_PS_STR_0                = "",
   parameter PLL_C_CNT_DUTY_CYCLE_0                  = 0,
   parameter PLL_C_CNT_FREQ_PS_STR_1                 = "",
   parameter PLL_C_CNT_PHASE_PS_STR_1                = "",
   parameter PLL_C_CNT_DUTY_CYCLE_1                  = 0,
   parameter PLL_C_CNT_FREQ_PS_STR_2                 = "",
   parameter PLL_C_CNT_PHASE_PS_STR_2                = "",
   parameter PLL_C_CNT_DUTY_CYCLE_2                  = 0,
   parameter PLL_C_CNT_FREQ_PS_STR_3                 = "",
   parameter PLL_C_CNT_PHASE_PS_STR_3                = "",
   parameter PLL_C_CNT_DUTY_CYCLE_3                  = 0,
   parameter PLL_C_CNT_FREQ_PS_STR_4                 = "",
   parameter PLL_C_CNT_PHASE_PS_STR_4                = "",
   parameter PLL_C_CNT_DUTY_CYCLE_4                  = 0,
   parameter PLL_C_CNT_FREQ_PS_STR_5                 = "",
   parameter PLL_C_CNT_PHASE_PS_STR_5                = "",
   parameter PLL_C_CNT_DUTY_CYCLE_5                  = 0,
   parameter PLL_C_CNT_FREQ_PS_STR_6                 = "",
   parameter PLL_C_CNT_PHASE_PS_STR_6                = "",
   parameter PLL_C_CNT_DUTY_CYCLE_6                  = 0,
   parameter PLL_C_CNT_FREQ_PS_STR_7                 = "",
   parameter PLL_C_CNT_PHASE_PS_STR_7                = "",
   parameter PLL_C_CNT_DUTY_CYCLE_7                  = 0,
   parameter PLL_C_CNT_FREQ_PS_STR_8                 = "",
   parameter PLL_C_CNT_PHASE_PS_STR_8                = "",
   parameter PLL_C_CNT_DUTY_CYCLE_8                  = 0,
   parameter PLL_C_CNT_OUT_EN_0                      = "",
   parameter PLL_C_CNT_OUT_EN_1                      = "",
   parameter PLL_C_CNT_OUT_EN_2                      = "",
   parameter PLL_C_CNT_OUT_EN_3                      = "",
   parameter PLL_C_CNT_OUT_EN_4                      = "",
   parameter PLL_C_CNT_OUT_EN_5                      = "",
   parameter PLL_C_CNT_OUT_EN_6                      = "",
   parameter PLL_C_CNT_OUT_EN_7                      = "",
   parameter PLL_C_CNT_OUT_EN_8                      = "",

   // Parameters describing HMC configuration
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

   parameter PREAMBLE_MODE                           = "",
   parameter DBI_WR_ENABLE                           = "",
   parameter DBI_RD_ENABLE                           = "",
   parameter CRC_EN                                  = "",
   parameter SWAP_DQS_A_B                            = "",
   parameter DQS_PACK_MODE                           = "",
   parameter OCT_SIZE                                = 1,
   parameter [6:0] DBC_WB_RESERVED_ENTRY             = 4,
   parameter DLL_MODE                                = "",
   parameter DLL_CODEWORD                            = 0,

   // Parameters describing logical tile/lane/pin allocation in the RTL
   parameter NUM_OF_RTL_TILES                        = 1,
   parameter AC_PIN_MAP_SCHEME                       = "",
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

   // Definition of port widhts for "clks_sharing_master_out" interface
   parameter PORT_CLKS_SHARING_MASTER_OUT_WIDTH      = 1,

   // Definition of port widhts for "clks_sharing_slave_in" interface
   parameter PORT_CLKS_SHARING_SLAVE_IN_WIDTH        = 1,

   // Definition of port widths for "mem" interface
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

   // Definition of port widths for "afi" interface
   //AUTOGEN_BEGIN: Definition of afi port widths
   parameter PORT_AFI_RLAT_WIDTH                     = 1,
   parameter PORT_AFI_WLAT_WIDTH                     = 1,
   parameter PORT_AFI_SEQ_BUSY_WIDTH                 = 1,
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

   // Definition of port widths for "ctrl_ast_cmd" interface
   parameter PORT_CTRL_AST_CMD_DATA_WIDTH            = 1,

   // Definition of port widths for "ctrl_ast_wr" interface
   parameter PORT_CTRL_AST_WR_DATA_WIDTH             = 1,

   // Definition of port widths for "ctrl_ast_rd" interface
   parameter PORT_CTRL_AST_RD_DATA_WIDTH             = 1,

   // Definition of port widths for "ctrl_amm" interface
   parameter PORT_CTRL_AMM_RDATA_WIDTH               = 1,
   parameter PORT_CTRL_AMM_ADDRESS_WIDTH             = 1,
   parameter PORT_CTRL_AMM_WDATA_WIDTH               = 1,
   parameter PORT_CTRL_AMM_BCOUNT_WIDTH              = 1,
   parameter PORT_CTRL_AMM_BYTEEN_WIDTH              = 1,

   // Definition of port widths for "ctrl_user_refresh" interface
   parameter PORT_CTRL_USER_REFRESH_REQ_WIDTH        = 1,
   parameter PORT_CTRL_USER_REFRESH_BANK_WIDTH       = 1,

   // Definition of port widths for "ctrl_self_refresh" interface
   parameter PORT_CTRL_SELF_REFRESH_REQ_WIDTH        = 1,

   // Definition of port widths for "ctrl_ecc" interface
   parameter PORT_CTRL_ECC_WRITE_INFO_WIDTH          = 1,
   parameter PORT_CTRL_ECC_READ_INFO_WIDTH           = 1,
   parameter PORT_CTRL_ECC_CMD_INFO_WIDTH            = 1,
   parameter PORT_CTRL_ECC_WB_POINTER_WIDTH          = 1,
   parameter PORT_CTRL_ECC_RDATA_ID_WIDTH            = 1,

   // Definition of port widths for "ctrl_mmr" interface
   parameter PORT_CTRL_MMR_SLAVE_ADDRESS_WIDTH       = 1,
   parameter PORT_CTRL_MMR_SLAVE_RDATA_WIDTH         = 1,
   parameter PORT_CTRL_MMR_SLAVE_WDATA_WIDTH         = 1,
   parameter PORT_CTRL_MMR_SLAVE_BCOUNT_WIDTH        = 1,

   // Definition of port widths for "hps_emif" interface
   parameter PORT_HPS_EMIF_H2E_WIDTH                 = 1,
   parameter PORT_HPS_EMIF_E2H_WIDTH                 = 1,
   parameter PORT_HPS_EMIF_H2E_GP_WIDTH              = 2,
   parameter PORT_HPS_EMIF_E2H_GP_WIDTH              = 1,

   // Definition of port widths for "cal_debug" interface
   parameter PORT_CAL_DEBUG_ADDRESS_WIDTH            = 1,
   parameter PORT_CAL_DEBUG_BYTEEN_WIDTH             = 1,
   parameter PORT_CAL_DEBUG_RDATA_WIDTH              = 1,
   parameter PORT_CAL_DEBUG_WDATA_WIDTH              = 1,

   // Definition of port widths for "cal_debug_out" interface
   parameter PORT_CAL_DEBUG_OUT_ADDRESS_WIDTH        = 1,
   parameter PORT_CAL_DEBUG_OUT_BYTEEN_WIDTH         = 1,
   parameter PORT_CAL_DEBUG_OUT_RDATA_WIDTH          = 1,
   parameter PORT_CAL_DEBUG_OUT_WDATA_WIDTH          = 1,

   // Definition of port widths for "cal_master" interface
   parameter PORT_CAL_MASTER_ADDRESS_WIDTH           = 1,
   parameter PORT_CAL_MASTER_BYTEEN_WIDTH            = 1,
   parameter PORT_CAL_MASTER_RDATA_WIDTH             = 1,
   parameter PORT_CAL_MASTER_WDATA_WIDTH             = 1,

   // Definition of port widths for "dft_nf" interface
   parameter PORT_DFT_NF_IOAUX_PIO_IN_WIDTH          = 1,
   parameter PORT_DFT_NF_IOAUX_PIO_OUT_WIDTH         = 1,
   parameter PORT_DFT_NF_PA_DPRIO_REG_ADDR_WIDTH     = 1,
   parameter PORT_DFT_NF_PA_DPRIO_WRITEDATA_WIDTH    = 1,
   parameter PORT_DFT_NF_PA_DPRIO_READDATA_WIDTH     = 1,
   parameter PORT_DFT_NF_PLL_CNTSEL_WIDTH            = 1,
   parameter PORT_DFT_NF_PLL_NUM_SHIFT_WIDTH         = 1,
   parameter PORT_DFT_NF_CORE_CLK_BUF_OUT_WIDTH      = 1,
   parameter PORT_DFT_NF_CORE_CLK_LOCKED_WIDTH       = 1,

   // Definition of port widths for "vji" interface
   parameter PORT_VJI_IR_IN_WIDTH                    = 1,
   parameter PORT_VJI_IR_OUT_WIDTH                   = 1,

   parameter LANES_USAGE_AUTOGEN_WCNT                = 0,
   parameter LANES_USAGE_3                           = 1'b0,
   parameter LANES_USAGE_2                           = 1'b0,
   parameter LANES_USAGE_1                           = 1'b0,
   parameter LANES_USAGE_0                           = 1'b0,
   parameter PINS_USAGE_AUTOGEN_WCNT                 = 0,
   parameter PINS_USAGE_12                           = 1'b0,
   parameter PINS_USAGE_11                           = 1'b0,
   parameter PINS_USAGE_10                           = 1'b0,
   parameter PINS_USAGE_9                            = 1'b0,
   parameter PINS_USAGE_8                            = 1'b0,
   parameter PINS_USAGE_7                            = 1'b0,
   parameter PINS_USAGE_6                            = 1'b0,
   parameter PINS_USAGE_5                            = 1'b0,
   parameter PINS_USAGE_4                            = 1'b0,
   parameter PINS_USAGE_3                            = 1'b0,
   parameter PINS_USAGE_2                            = 1'b0,
   parameter PINS_USAGE_1                            = 1'b0,
   parameter PINS_USAGE_0                            = 1'b0,
   parameter PINS_RATE_AUTOGEN_WCNT                  = 0,
   parameter PINS_RATE_12                            = 1'b0,
   parameter PINS_RATE_11                            = 1'b0,
   parameter PINS_RATE_10                            = 1'b0,
   parameter PINS_RATE_9                             = 1'b0,
   parameter PINS_RATE_8                             = 1'b0,
   parameter PINS_RATE_7                             = 1'b0,
   parameter PINS_RATE_6                             = 1'b0,
   parameter PINS_RATE_5                             = 1'b0,
   parameter PINS_RATE_4                             = 1'b0,
   parameter PINS_RATE_3                             = 1'b0,
   parameter PINS_RATE_2                             = 1'b0,
   parameter PINS_RATE_1                             = 1'b0,
   parameter PINS_RATE_0                             = 1'b0,
   parameter PINS_WDB_AUTOGEN_WCNT                   = 0,
   parameter PINS_WDB_38                             = 1'b0,
   parameter PINS_WDB_37                             = 1'b0,
   parameter PINS_WDB_36                             = 1'b0,
   parameter PINS_WDB_35                             = 1'b0,
   parameter PINS_WDB_34                             = 1'b0,
   parameter PINS_WDB_33                             = 1'b0,
   parameter PINS_WDB_32                             = 1'b0,
   parameter PINS_WDB_31                             = 1'b0,
   parameter PINS_WDB_30                             = 1'b0,
   parameter PINS_WDB_29                             = 1'b0,
   parameter PINS_WDB_28                             = 1'b0,
   parameter PINS_WDB_27                             = 1'b0,
   parameter PINS_WDB_26                             = 1'b0,
   parameter PINS_WDB_25                             = 1'b0,
   parameter PINS_WDB_24                             = 1'b0,
   parameter PINS_WDB_23                             = 1'b0,
   parameter PINS_WDB_22                             = 1'b0,
   parameter PINS_WDB_21                             = 1'b0,
   parameter PINS_WDB_20                             = 1'b0,
   parameter PINS_WDB_19                             = 1'b0,
   parameter PINS_WDB_18                             = 1'b0,
   parameter PINS_WDB_17                             = 1'b0,
   parameter PINS_WDB_16                             = 1'b0,
   parameter PINS_WDB_15                             = 1'b0,
   parameter PINS_WDB_14                             = 1'b0,
   parameter PINS_WDB_13                             = 1'b0,
   parameter PINS_WDB_12                             = 1'b0,
   parameter PINS_WDB_11                             = 1'b0,
   parameter PINS_WDB_10                             = 1'b0,
   parameter PINS_WDB_9                              = 1'b0,
   parameter PINS_WDB_8                              = 1'b0,
   parameter PINS_WDB_7                              = 1'b0,
   parameter PINS_WDB_6                              = 1'b0,
   parameter PINS_WDB_5                              = 1'b0,
   parameter PINS_WDB_4                              = 1'b0,
   parameter PINS_WDB_3                              = 1'b0,
   parameter PINS_WDB_2                              = 1'b0,
   parameter PINS_WDB_1                              = 1'b0,
   parameter PINS_WDB_0                              = 1'b0,
   parameter PINS_DATA_IN_MODE_AUTOGEN_WCNT          = 0,
   parameter PINS_DATA_IN_MODE_38                    = 1'b0,
   parameter PINS_DATA_IN_MODE_37                    = 1'b0,
   parameter PINS_DATA_IN_MODE_36                    = 1'b0,
   parameter PINS_DATA_IN_MODE_35                    = 1'b0,
   parameter PINS_DATA_IN_MODE_34                    = 1'b0,
   parameter PINS_DATA_IN_MODE_33                    = 1'b0,
   parameter PINS_DATA_IN_MODE_32                    = 1'b0,
   parameter PINS_DATA_IN_MODE_31                    = 1'b0,
   parameter PINS_DATA_IN_MODE_30                    = 1'b0,
   parameter PINS_DATA_IN_MODE_29                    = 1'b0,
   parameter PINS_DATA_IN_MODE_28                    = 1'b0,
   parameter PINS_DATA_IN_MODE_27                    = 1'b0,
   parameter PINS_DATA_IN_MODE_26                    = 1'b0,
   parameter PINS_DATA_IN_MODE_25                    = 1'b0,
   parameter PINS_DATA_IN_MODE_24                    = 1'b0,
   parameter PINS_DATA_IN_MODE_23                    = 1'b0,
   parameter PINS_DATA_IN_MODE_22                    = 1'b0,
   parameter PINS_DATA_IN_MODE_21                    = 1'b0,
   parameter PINS_DATA_IN_MODE_20                    = 1'b0,
   parameter PINS_DATA_IN_MODE_19                    = 1'b0,
   parameter PINS_DATA_IN_MODE_18                    = 1'b0,
   parameter PINS_DATA_IN_MODE_17                    = 1'b0,
   parameter PINS_DATA_IN_MODE_16                    = 1'b0,
   parameter PINS_DATA_IN_MODE_15                    = 1'b0,
   parameter PINS_DATA_IN_MODE_14                    = 1'b0,
   parameter PINS_DATA_IN_MODE_13                    = 1'b0,
   parameter PINS_DATA_IN_MODE_12                    = 1'b0,
   parameter PINS_DATA_IN_MODE_11                    = 1'b0,
   parameter PINS_DATA_IN_MODE_10                    = 1'b0,
   parameter PINS_DATA_IN_MODE_9                     = 1'b0,
   parameter PINS_DATA_IN_MODE_8                     = 1'b0,
   parameter PINS_DATA_IN_MODE_7                     = 1'b0,
   parameter PINS_DATA_IN_MODE_6                     = 1'b0,
   parameter PINS_DATA_IN_MODE_5                     = 1'b0,
   parameter PINS_DATA_IN_MODE_4                     = 1'b0,
   parameter PINS_DATA_IN_MODE_3                     = 1'b0,
   parameter PINS_DATA_IN_MODE_2                     = 1'b0,
   parameter PINS_DATA_IN_MODE_1                     = 1'b0,
   parameter PINS_DATA_IN_MODE_0                     = 1'b0,
   parameter PINS_C2L_DRIVEN_AUTOGEN_WCNT            = 0,
   parameter PINS_C2L_DRIVEN_12                      = 1'b0,
   parameter PINS_C2L_DRIVEN_11                      = 1'b0,
   parameter PINS_C2L_DRIVEN_10                      = 1'b0,
   parameter PINS_C2L_DRIVEN_9                       = 1'b0,
   parameter PINS_C2L_DRIVEN_8                       = 1'b0,
   parameter PINS_C2L_DRIVEN_7                       = 1'b0,
   parameter PINS_C2L_DRIVEN_6                       = 1'b0,
   parameter PINS_C2L_DRIVEN_5                       = 1'b0,
   parameter PINS_C2L_DRIVEN_4                       = 1'b0,
   parameter PINS_C2L_DRIVEN_3                       = 1'b0,
   parameter PINS_C2L_DRIVEN_2                       = 1'b0,
   parameter PINS_C2L_DRIVEN_1                       = 1'b0,
   parameter PINS_C2L_DRIVEN_0                       = 1'b0,
   parameter PINS_DB_IN_BYPASS_AUTOGEN_WCNT          = 0,
   parameter PINS_DB_IN_BYPASS_12                    = 1'b0,
   parameter PINS_DB_IN_BYPASS_11                    = 1'b0,
   parameter PINS_DB_IN_BYPASS_10                    = 1'b0,
   parameter PINS_DB_IN_BYPASS_9                     = 1'b0,
   parameter PINS_DB_IN_BYPASS_8                     = 1'b0,
   parameter PINS_DB_IN_BYPASS_7                     = 1'b0,
   parameter PINS_DB_IN_BYPASS_6                     = 1'b0,
   parameter PINS_DB_IN_BYPASS_5                     = 1'b0,
   parameter PINS_DB_IN_BYPASS_4                     = 1'b0,
   parameter PINS_DB_IN_BYPASS_3                     = 1'b0,
   parameter PINS_DB_IN_BYPASS_2                     = 1'b0,
   parameter PINS_DB_IN_BYPASS_1                     = 1'b0,
   parameter PINS_DB_IN_BYPASS_0                     = 1'b0,
   parameter PINS_DB_OUT_BYPASS_AUTOGEN_WCNT         = 0,
   parameter PINS_DB_OUT_BYPASS_12                   = 1'b0,
   parameter PINS_DB_OUT_BYPASS_11                   = 1'b0,
   parameter PINS_DB_OUT_BYPASS_10                   = 1'b0,
   parameter PINS_DB_OUT_BYPASS_9                    = 1'b0,
   parameter PINS_DB_OUT_BYPASS_8                    = 1'b0,
   parameter PINS_DB_OUT_BYPASS_7                    = 1'b0,
   parameter PINS_DB_OUT_BYPASS_6                    = 1'b0,
   parameter PINS_DB_OUT_BYPASS_5                    = 1'b0,
   parameter PINS_DB_OUT_BYPASS_4                    = 1'b0,
   parameter PINS_DB_OUT_BYPASS_3                    = 1'b0,
   parameter PINS_DB_OUT_BYPASS_2                    = 1'b0,
   parameter PINS_DB_OUT_BYPASS_1                    = 1'b0,
   parameter PINS_DB_OUT_BYPASS_0                    = 1'b0,
   parameter PINS_DB_OE_BYPASS_AUTOGEN_WCNT          = 0,
   parameter PINS_DB_OE_BYPASS_12                    = 1'b0,
   parameter PINS_DB_OE_BYPASS_11                    = 1'b0,
   parameter PINS_DB_OE_BYPASS_10                    = 1'b0,
   parameter PINS_DB_OE_BYPASS_9                     = 1'b0,
   parameter PINS_DB_OE_BYPASS_8                     = 1'b0,
   parameter PINS_DB_OE_BYPASS_7                     = 1'b0,
   parameter PINS_DB_OE_BYPASS_6                     = 1'b0,
   parameter PINS_DB_OE_BYPASS_5                     = 1'b0,
   parameter PINS_DB_OE_BYPASS_4                     = 1'b0,
   parameter PINS_DB_OE_BYPASS_3                     = 1'b0,
   parameter PINS_DB_OE_BYPASS_2                     = 1'b0,
   parameter PINS_DB_OE_BYPASS_1                     = 1'b0,
   parameter PINS_DB_OE_BYPASS_0                     = 1'b0,
   parameter PINS_INVERT_WR_AUTOGEN_WCNT             = 0,
   parameter PINS_INVERT_WR_12                       = 1'b0,
   parameter PINS_INVERT_WR_11                       = 1'b0,
   parameter PINS_INVERT_WR_10                       = 1'b0,
   parameter PINS_INVERT_WR_9                        = 1'b0,
   parameter PINS_INVERT_WR_8                        = 1'b0,
   parameter PINS_INVERT_WR_7                        = 1'b0,
   parameter PINS_INVERT_WR_6                        = 1'b0,
   parameter PINS_INVERT_WR_5                        = 1'b0,
   parameter PINS_INVERT_WR_4                        = 1'b0,
   parameter PINS_INVERT_WR_3                        = 1'b0,
   parameter PINS_INVERT_WR_2                        = 1'b0,
   parameter PINS_INVERT_WR_1                        = 1'b0,
   parameter PINS_INVERT_WR_0                        = 1'b0,
   parameter PINS_INVERT_OE_AUTOGEN_WCNT             = 0,
   parameter PINS_INVERT_OE_12                       = 1'b0,
   parameter PINS_INVERT_OE_11                       = 1'b0,
   parameter PINS_INVERT_OE_10                       = 1'b0,
   parameter PINS_INVERT_OE_9                        = 1'b0,
   parameter PINS_INVERT_OE_8                        = 1'b0,
   parameter PINS_INVERT_OE_7                        = 1'b0,
   parameter PINS_INVERT_OE_6                        = 1'b0,
   parameter PINS_INVERT_OE_5                        = 1'b0,
   parameter PINS_INVERT_OE_4                        = 1'b0,
   parameter PINS_INVERT_OE_3                        = 1'b0,
   parameter PINS_INVERT_OE_2                        = 1'b0,
   parameter PINS_INVERT_OE_1                        = 1'b0,
   parameter PINS_INVERT_OE_0                        = 1'b0,
   parameter PINS_AC_HMC_DATA_OVERRIDE_ENA_AUTOGEN_WCNT= 0,
   parameter PINS_AC_HMC_DATA_OVERRIDE_ENA_12        = 1'b0,
   parameter PINS_AC_HMC_DATA_OVERRIDE_ENA_11        = 1'b0,
   parameter PINS_AC_HMC_DATA_OVERRIDE_ENA_10        = 1'b0,
   parameter PINS_AC_HMC_DATA_OVERRIDE_ENA_9         = 1'b0,
   parameter PINS_AC_HMC_DATA_OVERRIDE_ENA_8         = 1'b0,
   parameter PINS_AC_HMC_DATA_OVERRIDE_ENA_7         = 1'b0,
   parameter PINS_AC_HMC_DATA_OVERRIDE_ENA_6         = 1'b0,
   parameter PINS_AC_HMC_DATA_OVERRIDE_ENA_5         = 1'b0,
   parameter PINS_AC_HMC_DATA_OVERRIDE_ENA_4         = 1'b0,
   parameter PINS_AC_HMC_DATA_OVERRIDE_ENA_3         = 1'b0,
   parameter PINS_AC_HMC_DATA_OVERRIDE_ENA_2         = 1'b0,
   parameter PINS_AC_HMC_DATA_OVERRIDE_ENA_1         = 1'b0,
   parameter PINS_AC_HMC_DATA_OVERRIDE_ENA_0         = 1'b0,
   parameter PINS_OCT_MODE_AUTOGEN_WCNT              = 0,
   parameter PINS_OCT_MODE_12                        = 1'b0,
   parameter PINS_OCT_MODE_11                        = 1'b0,
   parameter PINS_OCT_MODE_10                        = 1'b0,
   parameter PINS_OCT_MODE_9                         = 1'b0,
   parameter PINS_OCT_MODE_8                         = 1'b0,
   parameter PINS_OCT_MODE_7                         = 1'b0,
   parameter PINS_OCT_MODE_6                         = 1'b0,
   parameter PINS_OCT_MODE_5                         = 1'b0,
   parameter PINS_OCT_MODE_4                         = 1'b0,
   parameter PINS_OCT_MODE_3                         = 1'b0,
   parameter PINS_OCT_MODE_2                         = 1'b0,
   parameter PINS_OCT_MODE_1                         = 1'b0,
   parameter PINS_OCT_MODE_0                         = 1'b0,
   parameter PINS_GPIO_MODE_AUTOGEN_WCNT             = 0,
   parameter PINS_GPIO_MODE_12                       = 1'b0,
   parameter PINS_GPIO_MODE_11                       = 1'b0,
   parameter PINS_GPIO_MODE_10                       = 1'b0,
   parameter PINS_GPIO_MODE_9                        = 1'b0,
   parameter PINS_GPIO_MODE_8                        = 1'b0,
   parameter PINS_GPIO_MODE_7                        = 1'b0,
   parameter PINS_GPIO_MODE_6                        = 1'b0,
   parameter PINS_GPIO_MODE_5                        = 1'b0,
   parameter PINS_GPIO_MODE_4                        = 1'b0,
   parameter PINS_GPIO_MODE_3                        = 1'b0,
   parameter PINS_GPIO_MODE_2                        = 1'b0,
   parameter PINS_GPIO_MODE_1                        = 1'b0,
   parameter PINS_GPIO_MODE_0                        = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_AUTOGEN_WCNT     = 0,
   parameter UNUSED_MEM_PINS_PINLOC_128              = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_127              = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_126              = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_125              = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_124              = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_123              = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_122              = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_121              = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_120              = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_119              = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_118              = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_117              = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_116              = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_115              = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_114              = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_113              = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_112              = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_111              = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_110              = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_109              = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_108              = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_107              = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_106              = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_105              = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_104              = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_103              = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_102              = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_101              = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_100              = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_99               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_98               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_97               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_96               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_95               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_94               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_93               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_92               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_91               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_90               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_89               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_88               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_87               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_86               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_85               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_84               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_83               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_82               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_81               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_80               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_79               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_78               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_77               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_76               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_75               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_74               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_73               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_72               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_71               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_70               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_69               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_68               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_67               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_66               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_65               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_64               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_63               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_62               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_61               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_60               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_59               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_58               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_57               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_56               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_55               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_54               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_53               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_52               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_51               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_50               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_49               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_48               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_47               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_46               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_45               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_44               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_43               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_42               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_41               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_40               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_39               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_38               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_37               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_36               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_35               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_34               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_33               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_32               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_31               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_30               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_29               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_28               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_27               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_26               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_25               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_24               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_23               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_22               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_21               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_20               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_19               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_18               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_17               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_16               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_15               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_14               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_13               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_12               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_11               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_10               = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_9                = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_8                = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_7                = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_6                = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_5                = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_4                = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_3                = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_2                = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_1                = 1'b0,
   parameter UNUSED_MEM_PINS_PINLOC_0                = 1'b0,
   parameter UNUSED_DQS_BUSES_LANELOC_AUTOGEN_WCNT   = 0,
   parameter UNUSED_DQS_BUSES_LANELOC_10             = 1'b0,
   parameter UNUSED_DQS_BUSES_LANELOC_9              = 1'b0,
   parameter UNUSED_DQS_BUSES_LANELOC_8              = 1'b0,
   parameter UNUSED_DQS_BUSES_LANELOC_7              = 1'b0,
   parameter UNUSED_DQS_BUSES_LANELOC_6              = 1'b0,
   parameter UNUSED_DQS_BUSES_LANELOC_5              = 1'b0,
   parameter UNUSED_DQS_BUSES_LANELOC_4              = 1'b0,
   parameter UNUSED_DQS_BUSES_LANELOC_3              = 1'b0,
   parameter UNUSED_DQS_BUSES_LANELOC_2              = 1'b0,
   parameter UNUSED_DQS_BUSES_LANELOC_1              = 1'b0,
   parameter UNUSED_DQS_BUSES_LANELOC_0              = 1'b0,
   parameter CENTER_TIDS_AUTOGEN_WCNT                = 0,
   parameter CENTER_TIDS_2                           = 1'b0,
   parameter CENTER_TIDS_1                           = 1'b0,
   parameter CENTER_TIDS_0                           = 1'b0,
   parameter HMC_TIDS_AUTOGEN_WCNT                   = 0,
   parameter HMC_TIDS_2                              = 1'b0,
   parameter HMC_TIDS_1                              = 1'b0,
   parameter HMC_TIDS_0                              = 1'b0,
   parameter LANE_TIDS_AUTOGEN_WCNT                  = 0,
   parameter LANE_TIDS_9                             = 1'b0,
   parameter LANE_TIDS_8                             = 1'b0,
   parameter LANE_TIDS_7                             = 1'b0,
   parameter LANE_TIDS_6                             = 1'b0,
   parameter LANE_TIDS_5                             = 1'b0,
   parameter LANE_TIDS_4                             = 1'b0,
   parameter LANE_TIDS_3                             = 1'b0,
   parameter LANE_TIDS_2                             = 1'b0,
   parameter LANE_TIDS_1                             = 1'b0,
   parameter LANE_TIDS_0                             = 1'b0,
   parameter PORT_MEM_CK_PINLOC_AUTOGEN_WCNT         = 0,
   parameter PORT_MEM_CK_PINLOC_5                    = 1'b0,
   parameter PORT_MEM_CK_PINLOC_4                    = 1'b0,
   parameter PORT_MEM_CK_PINLOC_3                    = 1'b0,
   parameter PORT_MEM_CK_PINLOC_2                    = 1'b0,
   parameter PORT_MEM_CK_PINLOC_1                    = 1'b0,
   parameter PORT_MEM_CK_PINLOC_0                    = 1'b0,
   parameter PORT_MEM_CK_N_PINLOC_AUTOGEN_WCNT       = 0,
   parameter PORT_MEM_CK_N_PINLOC_5                  = 1'b0,
   parameter PORT_MEM_CK_N_PINLOC_4                  = 1'b0,
   parameter PORT_MEM_CK_N_PINLOC_3                  = 1'b0,
   parameter PORT_MEM_CK_N_PINLOC_2                  = 1'b0,
   parameter PORT_MEM_CK_N_PINLOC_1                  = 1'b0,
   parameter PORT_MEM_CK_N_PINLOC_0                  = 1'b0,
   parameter PORT_MEM_DK_PINLOC_AUTOGEN_WCNT         = 0,
   parameter PORT_MEM_DK_PINLOC_5                    = 1'b0,
   parameter PORT_MEM_DK_PINLOC_4                    = 1'b0,
   parameter PORT_MEM_DK_PINLOC_3                    = 1'b0,
   parameter PORT_MEM_DK_PINLOC_2                    = 1'b0,
   parameter PORT_MEM_DK_PINLOC_1                    = 1'b0,
   parameter PORT_MEM_DK_PINLOC_0                    = 1'b0,
   parameter PORT_MEM_DK_N_PINLOC_AUTOGEN_WCNT       = 0,
   parameter PORT_MEM_DK_N_PINLOC_5                  = 1'b0,
   parameter PORT_MEM_DK_N_PINLOC_4                  = 1'b0,
   parameter PORT_MEM_DK_N_PINLOC_3                  = 1'b0,
   parameter PORT_MEM_DK_N_PINLOC_2                  = 1'b0,
   parameter PORT_MEM_DK_N_PINLOC_1                  = 1'b0,
   parameter PORT_MEM_DK_N_PINLOC_0                  = 1'b0,
   parameter PORT_MEM_DKA_PINLOC_AUTOGEN_WCNT        = 0,
   parameter PORT_MEM_DKA_PINLOC_5                   = 1'b0,
   parameter PORT_MEM_DKA_PINLOC_4                   = 1'b0,
   parameter PORT_MEM_DKA_PINLOC_3                   = 1'b0,
   parameter PORT_MEM_DKA_PINLOC_2                   = 1'b0,
   parameter PORT_MEM_DKA_PINLOC_1                   = 1'b0,
   parameter PORT_MEM_DKA_PINLOC_0                   = 1'b0,
   parameter PORT_MEM_DKA_N_PINLOC_AUTOGEN_WCNT      = 0,
   parameter PORT_MEM_DKA_N_PINLOC_5                 = 1'b0,
   parameter PORT_MEM_DKA_N_PINLOC_4                 = 1'b0,
   parameter PORT_MEM_DKA_N_PINLOC_3                 = 1'b0,
   parameter PORT_MEM_DKA_N_PINLOC_2                 = 1'b0,
   parameter PORT_MEM_DKA_N_PINLOC_1                 = 1'b0,
   parameter PORT_MEM_DKA_N_PINLOC_0                 = 1'b0,
   parameter PORT_MEM_DKB_PINLOC_AUTOGEN_WCNT        = 0,
   parameter PORT_MEM_DKB_PINLOC_5                   = 1'b0,
   parameter PORT_MEM_DKB_PINLOC_4                   = 1'b0,
   parameter PORT_MEM_DKB_PINLOC_3                   = 1'b0,
   parameter PORT_MEM_DKB_PINLOC_2                   = 1'b0,
   parameter PORT_MEM_DKB_PINLOC_1                   = 1'b0,
   parameter PORT_MEM_DKB_PINLOC_0                   = 1'b0,
   parameter PORT_MEM_DKB_N_PINLOC_AUTOGEN_WCNT      = 0,
   parameter PORT_MEM_DKB_N_PINLOC_5                 = 1'b0,
   parameter PORT_MEM_DKB_N_PINLOC_4                 = 1'b0,
   parameter PORT_MEM_DKB_N_PINLOC_3                 = 1'b0,
   parameter PORT_MEM_DKB_N_PINLOC_2                 = 1'b0,
   parameter PORT_MEM_DKB_N_PINLOC_1                 = 1'b0,
   parameter PORT_MEM_DKB_N_PINLOC_0                 = 1'b0,
   parameter PORT_MEM_K_PINLOC_AUTOGEN_WCNT          = 0,
   parameter PORT_MEM_K_PINLOC_5                     = 1'b0,
   parameter PORT_MEM_K_PINLOC_4                     = 1'b0,
   parameter PORT_MEM_K_PINLOC_3                     = 1'b0,
   parameter PORT_MEM_K_PINLOC_2                     = 1'b0,
   parameter PORT_MEM_K_PINLOC_1                     = 1'b0,
   parameter PORT_MEM_K_PINLOC_0                     = 1'b0,
   parameter PORT_MEM_K_N_PINLOC_AUTOGEN_WCNT        = 0,
   parameter PORT_MEM_K_N_PINLOC_5                   = 1'b0,
   parameter PORT_MEM_K_N_PINLOC_4                   = 1'b0,
   parameter PORT_MEM_K_N_PINLOC_3                   = 1'b0,
   parameter PORT_MEM_K_N_PINLOC_2                   = 1'b0,
   parameter PORT_MEM_K_N_PINLOC_1                   = 1'b0,
   parameter PORT_MEM_K_N_PINLOC_0                   = 1'b0,
   parameter PORT_MEM_A_PINLOC_AUTOGEN_WCNT          = 0,
   parameter PORT_MEM_A_PINLOC_16                    = 1'b0,
   parameter PORT_MEM_A_PINLOC_15                    = 1'b0,
   parameter PORT_MEM_A_PINLOC_14                    = 1'b0,
   parameter PORT_MEM_A_PINLOC_13                    = 1'b0,
   parameter PORT_MEM_A_PINLOC_12                    = 1'b0,
   parameter PORT_MEM_A_PINLOC_11                    = 1'b0,
   parameter PORT_MEM_A_PINLOC_10                    = 1'b0,
   parameter PORT_MEM_A_PINLOC_9                     = 1'b0,
   parameter PORT_MEM_A_PINLOC_8                     = 1'b0,
   parameter PORT_MEM_A_PINLOC_7                     = 1'b0,
   parameter PORT_MEM_A_PINLOC_6                     = 1'b0,
   parameter PORT_MEM_A_PINLOC_5                     = 1'b0,
   parameter PORT_MEM_A_PINLOC_4                     = 1'b0,
   parameter PORT_MEM_A_PINLOC_3                     = 1'b0,
   parameter PORT_MEM_A_PINLOC_2                     = 1'b0,
   parameter PORT_MEM_A_PINLOC_1                     = 1'b0,
   parameter PORT_MEM_A_PINLOC_0                     = 1'b0,
   parameter PORT_MEM_BA_PINLOC_AUTOGEN_WCNT         = 0,
   parameter PORT_MEM_BA_PINLOC_5                    = 1'b0,
   parameter PORT_MEM_BA_PINLOC_4                    = 1'b0,
   parameter PORT_MEM_BA_PINLOC_3                    = 1'b0,
   parameter PORT_MEM_BA_PINLOC_2                    = 1'b0,
   parameter PORT_MEM_BA_PINLOC_1                    = 1'b0,
   parameter PORT_MEM_BA_PINLOC_0                    = 1'b0,
   parameter PORT_MEM_BG_PINLOC_AUTOGEN_WCNT         = 0,
   parameter PORT_MEM_BG_PINLOC_5                    = 1'b0,
   parameter PORT_MEM_BG_PINLOC_4                    = 1'b0,
   parameter PORT_MEM_BG_PINLOC_3                    = 1'b0,
   parameter PORT_MEM_BG_PINLOC_2                    = 1'b0,
   parameter PORT_MEM_BG_PINLOC_1                    = 1'b0,
   parameter PORT_MEM_BG_PINLOC_0                    = 1'b0,
   parameter PORT_MEM_C_PINLOC_AUTOGEN_WCNT          = 0,
   parameter PORT_MEM_C_PINLOC_5                     = 1'b0,
   parameter PORT_MEM_C_PINLOC_4                     = 1'b0,
   parameter PORT_MEM_C_PINLOC_3                     = 1'b0,
   parameter PORT_MEM_C_PINLOC_2                     = 1'b0,
   parameter PORT_MEM_C_PINLOC_1                     = 1'b0,
   parameter PORT_MEM_C_PINLOC_0                     = 1'b0,
   parameter PORT_MEM_CKE_PINLOC_AUTOGEN_WCNT        = 0,
   parameter PORT_MEM_CKE_PINLOC_5                   = 1'b0,
   parameter PORT_MEM_CKE_PINLOC_4                   = 1'b0,
   parameter PORT_MEM_CKE_PINLOC_3                   = 1'b0,
   parameter PORT_MEM_CKE_PINLOC_2                   = 1'b0,
   parameter PORT_MEM_CKE_PINLOC_1                   = 1'b0,
   parameter PORT_MEM_CKE_PINLOC_0                   = 1'b0,
   parameter PORT_MEM_CS_N_PINLOC_AUTOGEN_WCNT       = 0,
   parameter PORT_MEM_CS_N_PINLOC_5                  = 1'b0,
   parameter PORT_MEM_CS_N_PINLOC_4                  = 1'b0,
   parameter PORT_MEM_CS_N_PINLOC_3                  = 1'b0,
   parameter PORT_MEM_CS_N_PINLOC_2                  = 1'b0,
   parameter PORT_MEM_CS_N_PINLOC_1                  = 1'b0,
   parameter PORT_MEM_CS_N_PINLOC_0                  = 1'b0,
   parameter PORT_MEM_RM_PINLOC_AUTOGEN_WCNT         = 0,
   parameter PORT_MEM_RM_PINLOC_5                    = 1'b0,
   parameter PORT_MEM_RM_PINLOC_4                    = 1'b0,
   parameter PORT_MEM_RM_PINLOC_3                    = 1'b0,
   parameter PORT_MEM_RM_PINLOC_2                    = 1'b0,
   parameter PORT_MEM_RM_PINLOC_1                    = 1'b0,
   parameter PORT_MEM_RM_PINLOC_0                    = 1'b0,
   parameter PORT_MEM_ODT_PINLOC_AUTOGEN_WCNT        = 0,
   parameter PORT_MEM_ODT_PINLOC_5                   = 1'b0,
   parameter PORT_MEM_ODT_PINLOC_4                   = 1'b0,
   parameter PORT_MEM_ODT_PINLOC_3                   = 1'b0,
   parameter PORT_MEM_ODT_PINLOC_2                   = 1'b0,
   parameter PORT_MEM_ODT_PINLOC_1                   = 1'b0,
   parameter PORT_MEM_ODT_PINLOC_0                   = 1'b0,
   parameter PORT_MEM_RAS_N_PINLOC_AUTOGEN_WCNT      = 0,
   parameter PORT_MEM_RAS_N_PINLOC_1                 = 1'b0,
   parameter PORT_MEM_RAS_N_PINLOC_0                 = 1'b0,
   parameter PORT_MEM_CAS_N_PINLOC_AUTOGEN_WCNT      = 0,
   parameter PORT_MEM_CAS_N_PINLOC_1                 = 1'b0,
   parameter PORT_MEM_CAS_N_PINLOC_0                 = 1'b0,
   parameter PORT_MEM_WE_N_PINLOC_AUTOGEN_WCNT       = 0,
   parameter PORT_MEM_WE_N_PINLOC_1                  = 1'b0,
   parameter PORT_MEM_WE_N_PINLOC_0                  = 1'b0,
   parameter PORT_MEM_RESET_N_PINLOC_AUTOGEN_WCNT    = 0,
   parameter PORT_MEM_RESET_N_PINLOC_1               = 1'b0,
   parameter PORT_MEM_RESET_N_PINLOC_0               = 1'b0,
   parameter PORT_MEM_ACT_N_PINLOC_AUTOGEN_WCNT      = 0,
   parameter PORT_MEM_ACT_N_PINLOC_1                 = 1'b0,
   parameter PORT_MEM_ACT_N_PINLOC_0                 = 1'b0,
   parameter PORT_MEM_PAR_PINLOC_AUTOGEN_WCNT        = 0,
   parameter PORT_MEM_PAR_PINLOC_1                   = 1'b0,
   parameter PORT_MEM_PAR_PINLOC_0                   = 1'b0,
   parameter PORT_MEM_CA_PINLOC_AUTOGEN_WCNT         = 0,
   parameter PORT_MEM_CA_PINLOC_16                   = 1'b0,
   parameter PORT_MEM_CA_PINLOC_15                   = 1'b0,
   parameter PORT_MEM_CA_PINLOC_14                   = 1'b0,
   parameter PORT_MEM_CA_PINLOC_13                   = 1'b0,
   parameter PORT_MEM_CA_PINLOC_12                   = 1'b0,
   parameter PORT_MEM_CA_PINLOC_11                   = 1'b0,
   parameter PORT_MEM_CA_PINLOC_10                   = 1'b0,
   parameter PORT_MEM_CA_PINLOC_9                    = 1'b0,
   parameter PORT_MEM_CA_PINLOC_8                    = 1'b0,
   parameter PORT_MEM_CA_PINLOC_7                    = 1'b0,
   parameter PORT_MEM_CA_PINLOC_6                    = 1'b0,
   parameter PORT_MEM_CA_PINLOC_5                    = 1'b0,
   parameter PORT_MEM_CA_PINLOC_4                    = 1'b0,
   parameter PORT_MEM_CA_PINLOC_3                    = 1'b0,
   parameter PORT_MEM_CA_PINLOC_2                    = 1'b0,
   parameter PORT_MEM_CA_PINLOC_1                    = 1'b0,
   parameter PORT_MEM_CA_PINLOC_0                    = 1'b0,
   parameter PORT_MEM_REF_N_PINLOC_AUTOGEN_WCNT      = 0,
   parameter PORT_MEM_REF_N_PINLOC_0                 = 1'b0,
   parameter PORT_MEM_WPS_N_PINLOC_AUTOGEN_WCNT      = 0,
   parameter PORT_MEM_WPS_N_PINLOC_0                 = 1'b0,
   parameter PORT_MEM_RPS_N_PINLOC_AUTOGEN_WCNT      = 0,
   parameter PORT_MEM_RPS_N_PINLOC_0                 = 1'b0,
   parameter PORT_MEM_DOFF_N_PINLOC_AUTOGEN_WCNT     = 0,
   parameter PORT_MEM_DOFF_N_PINLOC_0                = 1'b0,
   parameter PORT_MEM_LDA_N_PINLOC_AUTOGEN_WCNT      = 0,
   parameter PORT_MEM_LDA_N_PINLOC_0                 = 1'b0,
   parameter PORT_MEM_LDB_N_PINLOC_AUTOGEN_WCNT      = 0,
   parameter PORT_MEM_LDB_N_PINLOC_0                 = 1'b0,
   parameter PORT_MEM_RWA_N_PINLOC_AUTOGEN_WCNT      = 0,
   parameter PORT_MEM_RWA_N_PINLOC_0                 = 1'b0,
   parameter PORT_MEM_RWB_N_PINLOC_AUTOGEN_WCNT      = 0,
   parameter PORT_MEM_RWB_N_PINLOC_0                 = 1'b0,
   parameter PORT_MEM_LBK0_N_PINLOC_AUTOGEN_WCNT     = 0,
   parameter PORT_MEM_LBK0_N_PINLOC_0                = 1'b0,
   parameter PORT_MEM_LBK1_N_PINLOC_AUTOGEN_WCNT     = 0,
   parameter PORT_MEM_LBK1_N_PINLOC_0                = 1'b0,
   parameter PORT_MEM_CFG_N_PINLOC_AUTOGEN_WCNT      = 0,
   parameter PORT_MEM_CFG_N_PINLOC_0                 = 1'b0,
   parameter PORT_MEM_AP_PINLOC_AUTOGEN_WCNT         = 0,
   parameter PORT_MEM_AP_PINLOC_0                    = 1'b0,
   parameter PORT_MEM_AINV_PINLOC_AUTOGEN_WCNT       = 0,
   parameter PORT_MEM_AINV_PINLOC_0                  = 1'b0,
   parameter PORT_MEM_DM_PINLOC_AUTOGEN_WCNT         = 0,
   parameter PORT_MEM_DM_PINLOC_12                   = 1'b0,
   parameter PORT_MEM_DM_PINLOC_11                   = 1'b0,
   parameter PORT_MEM_DM_PINLOC_10                   = 1'b0,
   parameter PORT_MEM_DM_PINLOC_9                    = 1'b0,
   parameter PORT_MEM_DM_PINLOC_8                    = 1'b0,
   parameter PORT_MEM_DM_PINLOC_7                    = 1'b0,
   parameter PORT_MEM_DM_PINLOC_6                    = 1'b0,
   parameter PORT_MEM_DM_PINLOC_5                    = 1'b0,
   parameter PORT_MEM_DM_PINLOC_4                    = 1'b0,
   parameter PORT_MEM_DM_PINLOC_3                    = 1'b0,
   parameter PORT_MEM_DM_PINLOC_2                    = 1'b0,
   parameter PORT_MEM_DM_PINLOC_1                    = 1'b0,
   parameter PORT_MEM_DM_PINLOC_0                    = 1'b0,
   parameter PORT_MEM_BWS_N_PINLOC_AUTOGEN_WCNT      = 0,
   parameter PORT_MEM_BWS_N_PINLOC_2                 = 1'b0,
   parameter PORT_MEM_BWS_N_PINLOC_1                 = 1'b0,
   parameter PORT_MEM_BWS_N_PINLOC_0                 = 1'b0,
   parameter PORT_MEM_D_PINLOC_AUTOGEN_WCNT          = 0,
   parameter PORT_MEM_D_PINLOC_48                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_47                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_46                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_45                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_44                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_43                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_42                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_41                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_40                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_39                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_38                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_37                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_36                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_35                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_34                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_33                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_32                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_31                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_30                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_29                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_28                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_27                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_26                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_25                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_24                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_23                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_22                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_21                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_20                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_19                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_18                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_17                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_16                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_15                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_14                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_13                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_12                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_11                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_10                    = 1'b0,
   parameter PORT_MEM_D_PINLOC_9                     = 1'b0,
   parameter PORT_MEM_D_PINLOC_8                     = 1'b0,
   parameter PORT_MEM_D_PINLOC_7                     = 1'b0,
   parameter PORT_MEM_D_PINLOC_6                     = 1'b0,
   parameter PORT_MEM_D_PINLOC_5                     = 1'b0,
   parameter PORT_MEM_D_PINLOC_4                     = 1'b0,
   parameter PORT_MEM_D_PINLOC_3                     = 1'b0,
   parameter PORT_MEM_D_PINLOC_2                     = 1'b0,
   parameter PORT_MEM_D_PINLOC_1                     = 1'b0,
   parameter PORT_MEM_D_PINLOC_0                     = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_AUTOGEN_WCNT         = 0,
   parameter PORT_MEM_DQ_PINLOC_48                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_47                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_46                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_45                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_44                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_43                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_42                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_41                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_40                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_39                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_38                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_37                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_36                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_35                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_34                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_33                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_32                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_31                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_30                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_29                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_28                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_27                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_26                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_25                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_24                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_23                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_22                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_21                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_20                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_19                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_18                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_17                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_16                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_15                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_14                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_13                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_12                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_11                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_10                   = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_9                    = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_8                    = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_7                    = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_6                    = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_5                    = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_4                    = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_3                    = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_2                    = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_1                    = 1'b0,
   parameter PORT_MEM_DQ_PINLOC_0                    = 1'b0,
   parameter PORT_MEM_DBI_N_PINLOC_AUTOGEN_WCNT      = 0,
   parameter PORT_MEM_DBI_N_PINLOC_6                 = 1'b0,
   parameter PORT_MEM_DBI_N_PINLOC_5                 = 1'b0,
   parameter PORT_MEM_DBI_N_PINLOC_4                 = 1'b0,
   parameter PORT_MEM_DBI_N_PINLOC_3                 = 1'b0,
   parameter PORT_MEM_DBI_N_PINLOC_2                 = 1'b0,
   parameter PORT_MEM_DBI_N_PINLOC_1                 = 1'b0,
   parameter PORT_MEM_DBI_N_PINLOC_0                 = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_AUTOGEN_WCNT        = 0,
   parameter PORT_MEM_DQA_PINLOC_48                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_47                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_46                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_45                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_44                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_43                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_42                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_41                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_40                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_39                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_38                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_37                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_36                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_35                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_34                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_33                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_32                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_31                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_30                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_29                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_28                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_27                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_26                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_25                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_24                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_23                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_22                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_21                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_20                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_19                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_18                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_17                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_16                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_15                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_14                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_13                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_12                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_11                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_10                  = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_9                   = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_8                   = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_7                   = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_6                   = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_5                   = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_4                   = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_3                   = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_2                   = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_1                   = 1'b0,
   parameter PORT_MEM_DQA_PINLOC_0                   = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_AUTOGEN_WCNT        = 0,
   parameter PORT_MEM_DQB_PINLOC_48                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_47                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_46                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_45                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_44                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_43                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_42                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_41                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_40                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_39                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_38                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_37                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_36                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_35                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_34                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_33                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_32                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_31                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_30                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_29                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_28                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_27                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_26                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_25                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_24                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_23                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_22                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_21                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_20                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_19                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_18                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_17                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_16                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_15                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_14                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_13                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_12                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_11                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_10                  = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_9                   = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_8                   = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_7                   = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_6                   = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_5                   = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_4                   = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_3                   = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_2                   = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_1                   = 1'b0,
   parameter PORT_MEM_DQB_PINLOC_0                   = 1'b0,
   parameter PORT_MEM_DINVA_PINLOC_AUTOGEN_WCNT      = 0,
   parameter PORT_MEM_DINVA_PINLOC_2                 = 1'b0,
   parameter PORT_MEM_DINVA_PINLOC_1                 = 1'b0,
   parameter PORT_MEM_DINVA_PINLOC_0                 = 1'b0,
   parameter PORT_MEM_DINVB_PINLOC_AUTOGEN_WCNT      = 0,
   parameter PORT_MEM_DINVB_PINLOC_2                 = 1'b0,
   parameter PORT_MEM_DINVB_PINLOC_1                 = 1'b0,
   parameter PORT_MEM_DINVB_PINLOC_0                 = 1'b0,
   parameter PORT_MEM_Q_PINLOC_AUTOGEN_WCNT          = 0,
   parameter PORT_MEM_Q_PINLOC_48                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_47                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_46                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_45                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_44                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_43                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_42                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_41                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_40                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_39                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_38                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_37                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_36                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_35                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_34                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_33                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_32                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_31                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_30                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_29                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_28                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_27                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_26                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_25                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_24                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_23                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_22                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_21                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_20                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_19                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_18                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_17                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_16                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_15                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_14                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_13                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_12                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_11                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_10                    = 1'b0,
   parameter PORT_MEM_Q_PINLOC_9                     = 1'b0,
   parameter PORT_MEM_Q_PINLOC_8                     = 1'b0,
   parameter PORT_MEM_Q_PINLOC_7                     = 1'b0,
   parameter PORT_MEM_Q_PINLOC_6                     = 1'b0,
   parameter PORT_MEM_Q_PINLOC_5                     = 1'b0,
   parameter PORT_MEM_Q_PINLOC_4                     = 1'b0,
   parameter PORT_MEM_Q_PINLOC_3                     = 1'b0,
   parameter PORT_MEM_Q_PINLOC_2                     = 1'b0,
   parameter PORT_MEM_Q_PINLOC_1                     = 1'b0,
   parameter PORT_MEM_Q_PINLOC_0                     = 1'b0,
   parameter PORT_MEM_DQS_PINLOC_AUTOGEN_WCNT        = 0,
   parameter PORT_MEM_DQS_PINLOC_12                  = 1'b0,
   parameter PORT_MEM_DQS_PINLOC_11                  = 1'b0,
   parameter PORT_MEM_DQS_PINLOC_10                  = 1'b0,
   parameter PORT_MEM_DQS_PINLOC_9                   = 1'b0,
   parameter PORT_MEM_DQS_PINLOC_8                   = 1'b0,
   parameter PORT_MEM_DQS_PINLOC_7                   = 1'b0,
   parameter PORT_MEM_DQS_PINLOC_6                   = 1'b0,
   parameter PORT_MEM_DQS_PINLOC_5                   = 1'b0,
   parameter PORT_MEM_DQS_PINLOC_4                   = 1'b0,
   parameter PORT_MEM_DQS_PINLOC_3                   = 1'b0,
   parameter PORT_MEM_DQS_PINLOC_2                   = 1'b0,
   parameter PORT_MEM_DQS_PINLOC_1                   = 1'b0,
   parameter PORT_MEM_DQS_PINLOC_0                   = 1'b0,
   parameter PORT_MEM_DQS_N_PINLOC_AUTOGEN_WCNT      = 0,
   parameter PORT_MEM_DQS_N_PINLOC_12                = 1'b0,
   parameter PORT_MEM_DQS_N_PINLOC_11                = 1'b0,
   parameter PORT_MEM_DQS_N_PINLOC_10                = 1'b0,
   parameter PORT_MEM_DQS_N_PINLOC_9                 = 1'b0,
   parameter PORT_MEM_DQS_N_PINLOC_8                 = 1'b0,
   parameter PORT_MEM_DQS_N_PINLOC_7                 = 1'b0,
   parameter PORT_MEM_DQS_N_PINLOC_6                 = 1'b0,
   parameter PORT_MEM_DQS_N_PINLOC_5                 = 1'b0,
   parameter PORT_MEM_DQS_N_PINLOC_4                 = 1'b0,
   parameter PORT_MEM_DQS_N_PINLOC_3                 = 1'b0,
   parameter PORT_MEM_DQS_N_PINLOC_2                 = 1'b0,
   parameter PORT_MEM_DQS_N_PINLOC_1                 = 1'b0,
   parameter PORT_MEM_DQS_N_PINLOC_0                 = 1'b0,
   parameter PORT_MEM_QK_PINLOC_AUTOGEN_WCNT         = 0,
   parameter PORT_MEM_QK_PINLOC_5                    = 1'b0,
   parameter PORT_MEM_QK_PINLOC_4                    = 1'b0,
   parameter PORT_MEM_QK_PINLOC_3                    = 1'b0,
   parameter PORT_MEM_QK_PINLOC_2                    = 1'b0,
   parameter PORT_MEM_QK_PINLOC_1                    = 1'b0,
   parameter PORT_MEM_QK_PINLOC_0                    = 1'b0,
   parameter PORT_MEM_QK_N_PINLOC_AUTOGEN_WCNT       = 0,
   parameter PORT_MEM_QK_N_PINLOC_5                  = 1'b0,
   parameter PORT_MEM_QK_N_PINLOC_4                  = 1'b0,
   parameter PORT_MEM_QK_N_PINLOC_3                  = 1'b0,
   parameter PORT_MEM_QK_N_PINLOC_2                  = 1'b0,
   parameter PORT_MEM_QK_N_PINLOC_1                  = 1'b0,
   parameter PORT_MEM_QK_N_PINLOC_0                  = 1'b0,
   parameter PORT_MEM_QKA_PINLOC_AUTOGEN_WCNT        = 0,
   parameter PORT_MEM_QKA_PINLOC_5                   = 1'b0,
   parameter PORT_MEM_QKA_PINLOC_4                   = 1'b0,
   parameter PORT_MEM_QKA_PINLOC_3                   = 1'b0,
   parameter PORT_MEM_QKA_PINLOC_2                   = 1'b0,
   parameter PORT_MEM_QKA_PINLOC_1                   = 1'b0,
   parameter PORT_MEM_QKA_PINLOC_0                   = 1'b0,
   parameter PORT_MEM_QKA_N_PINLOC_AUTOGEN_WCNT      = 0,
   parameter PORT_MEM_QKA_N_PINLOC_5                 = 1'b0,
   parameter PORT_MEM_QKA_N_PINLOC_4                 = 1'b0,
   parameter PORT_MEM_QKA_N_PINLOC_3                 = 1'b0,
   parameter PORT_MEM_QKA_N_PINLOC_2                 = 1'b0,
   parameter PORT_MEM_QKA_N_PINLOC_1                 = 1'b0,
   parameter PORT_MEM_QKA_N_PINLOC_0                 = 1'b0,
   parameter PORT_MEM_QKB_PINLOC_AUTOGEN_WCNT        = 0,
   parameter PORT_MEM_QKB_PINLOC_5                   = 1'b0,
   parameter PORT_MEM_QKB_PINLOC_4                   = 1'b0,
   parameter PORT_MEM_QKB_PINLOC_3                   = 1'b0,
   parameter PORT_MEM_QKB_PINLOC_2                   = 1'b0,
   parameter PORT_MEM_QKB_PINLOC_1                   = 1'b0,
   parameter PORT_MEM_QKB_PINLOC_0                   = 1'b0,
   parameter PORT_MEM_QKB_N_PINLOC_AUTOGEN_WCNT      = 0,
   parameter PORT_MEM_QKB_N_PINLOC_5                 = 1'b0,
   parameter PORT_MEM_QKB_N_PINLOC_4                 = 1'b0,
   parameter PORT_MEM_QKB_N_PINLOC_3                 = 1'b0,
   parameter PORT_MEM_QKB_N_PINLOC_2                 = 1'b0,
   parameter PORT_MEM_QKB_N_PINLOC_1                 = 1'b0,
   parameter PORT_MEM_QKB_N_PINLOC_0                 = 1'b0,
   parameter PORT_MEM_CQ_PINLOC_AUTOGEN_WCNT         = 0,
   parameter PORT_MEM_CQ_PINLOC_1                    = 1'b0,
   parameter PORT_MEM_CQ_PINLOC_0                    = 1'b0,
   parameter PORT_MEM_CQ_N_PINLOC_AUTOGEN_WCNT       = 0,
   parameter PORT_MEM_CQ_N_PINLOC_1                  = 1'b0,
   parameter PORT_MEM_CQ_N_PINLOC_0                  = 1'b0,
   parameter PORT_MEM_ALERT_N_PINLOC_AUTOGEN_WCNT    = 0,
   parameter PORT_MEM_ALERT_N_PINLOC_1               = 1'b0,
   parameter PORT_MEM_ALERT_N_PINLOC_0               = 1'b0,
   parameter PORT_MEM_PE_N_PINLOC_AUTOGEN_WCNT       = 0,
   parameter PORT_MEM_PE_N_PINLOC_1                  = 1'b0,
   parameter PORT_MEM_PE_N_PINLOC_0                  = 1'b0
) (
   // Reset
   input  logic                                               global_reset_n,

   // PLL signals
   input  logic                                               pll_ref_clk,
   output logic                                               pll_locked,
   output logic                                               pll_extra_clk_0,
   output logic                                               pll_extra_clk_1,
   output logic                                               pll_extra_clk_2,
   output logic                                               pll_extra_clk_3,

   // OCT signals
   input  logic                                               oct_rzqin,

   // Status signals
   output logic                                               local_cal_success,
   output logic                                               local_cal_fail,

   // VID cal done signal
   input  logic                                               vid_cal_done_persist,

   // User reset signal going to core (for PHY + hard controller interfaces)
   output logic                                               emif_usr_reset_n,
   output logic                                               emif_usr_reset_n_sec,

   // User clock going to core (for PHY + hard controller interfaces)
   output logic                                               emif_usr_clk,
   output logic                                               emif_usr_clk_sec,

   // A clock that runs at half the frequency of emif_usr_clk going to core
   output logic                                               emif_usr_half_clk,
   output logic                                               emif_usr_half_clk_sec,

   // AFI reset going to core
   output logic                                               afi_reset_n,

   // AFI clock going to core
   output logic                                               afi_clk,

   // A clock that runs at half the frequency of afi_clk going to core
   output logic                                               afi_half_clk,

   // Signals required to share core clocking resources between across
   // compatible interfaces. An interface can be configured as a "master"
   // which exports the core clocks, or a "slave" which imports the
   // core clocks from a master interface.
   input  logic [PORT_CLKS_SHARING_SLAVE_IN_WIDTH-1:0]        clks_sharing_slave_in,
   output logic [PORT_CLKS_SHARING_MASTER_OUT_WIDTH-1:0]      clks_sharing_master_out,

   // Ports for "mem" interface
   //AUTOGEN_BEGIN: Definition of memory ports
   output logic [PORT_MEM_CK_WIDTH-1:0]                       mem_ck,
   output logic [PORT_MEM_CK_N_WIDTH-1:0]                     mem_ck_n,
   output logic [PORT_MEM_DK_WIDTH-1:0]                       mem_dk,
   output logic [PORT_MEM_DK_N_WIDTH-1:0]                     mem_dk_n,
   output logic [PORT_MEM_DKA_WIDTH-1:0]                      mem_dka,
   output logic [PORT_MEM_DKA_N_WIDTH-1:0]                    mem_dka_n,
   output logic [PORT_MEM_DKB_WIDTH-1:0]                      mem_dkb,
   output logic [PORT_MEM_DKB_N_WIDTH-1:0]                    mem_dkb_n,
   output logic [PORT_MEM_K_WIDTH-1:0]                        mem_k,
   output logic [PORT_MEM_K_N_WIDTH-1:0]                      mem_k_n,
   output logic [PORT_MEM_A_WIDTH-1:0]                        mem_a,
   output logic [PORT_MEM_BA_WIDTH-1:0]                       mem_ba,
   output logic [PORT_MEM_BG_WIDTH-1:0]                       mem_bg,
   output logic [PORT_MEM_C_WIDTH-1:0]                        mem_c,
   output logic [PORT_MEM_CKE_WIDTH-1:0]                      mem_cke,
   output logic [PORT_MEM_CS_N_WIDTH-1:0]                     mem_cs_n,
   output logic [PORT_MEM_RM_WIDTH-1:0]                       mem_rm,
   output logic [PORT_MEM_ODT_WIDTH-1:0]                      mem_odt,
   output logic [PORT_MEM_RAS_N_WIDTH-1:0]                    mem_ras_n,
   output logic [PORT_MEM_CAS_N_WIDTH-1:0]                    mem_cas_n,
   output logic [PORT_MEM_WE_N_WIDTH-1:0]                     mem_we_n,
   output logic [PORT_MEM_RESET_N_WIDTH-1:0]                  mem_reset_n,
   output logic [PORT_MEM_ACT_N_WIDTH-1:0]                    mem_act_n,
   output logic [PORT_MEM_PAR_WIDTH-1:0]                      mem_par,
   output logic [PORT_MEM_CA_WIDTH-1:0]                       mem_ca,
   output logic [PORT_MEM_REF_N_WIDTH-1:0]                    mem_ref_n,
   output logic [PORT_MEM_WPS_N_WIDTH-1:0]                    mem_wps_n,
   output logic [PORT_MEM_RPS_N_WIDTH-1:0]                    mem_rps_n,
   output logic [PORT_MEM_DOFF_N_WIDTH-1:0]                   mem_doff_n,
   output logic [PORT_MEM_LDA_N_WIDTH-1:0]                    mem_lda_n,
   output logic [PORT_MEM_LDB_N_WIDTH-1:0]                    mem_ldb_n,
   output logic [PORT_MEM_RWA_N_WIDTH-1:0]                    mem_rwa_n,
   output logic [PORT_MEM_RWB_N_WIDTH-1:0]                    mem_rwb_n,
   output logic [PORT_MEM_LBK0_N_WIDTH-1:0]                   mem_lbk0_n,
   output logic [PORT_MEM_LBK1_N_WIDTH-1:0]                   mem_lbk1_n,
   output logic [PORT_MEM_CFG_N_WIDTH-1:0]                    mem_cfg_n,
   output logic [PORT_MEM_AP_WIDTH-1:0]                       mem_ap,
   output logic [PORT_MEM_AINV_WIDTH-1:0]                     mem_ainv,
   output logic [PORT_MEM_DM_WIDTH-1:0]                       mem_dm,
   output logic [PORT_MEM_BWS_N_WIDTH-1:0]                    mem_bws_n,
   output logic [PORT_MEM_D_WIDTH-1:0]                        mem_d,
   inout  tri   [PORT_MEM_DQ_WIDTH-1:0]                       mem_dq,
   inout  tri   [PORT_MEM_DBI_N_WIDTH-1:0]                    mem_dbi_n,
   inout  tri   [PORT_MEM_DQA_WIDTH-1:0]                      mem_dqa,
   inout  tri   [PORT_MEM_DQB_WIDTH-1:0]                      mem_dqb,
   inout  tri   [PORT_MEM_DINVA_WIDTH-1:0]                    mem_dinva,
   inout  tri   [PORT_MEM_DINVB_WIDTH-1:0]                    mem_dinvb,
   input  logic [PORT_MEM_Q_WIDTH-1:0]                        mem_q,
   inout  tri   [PORT_MEM_DQS_WIDTH-1:0]                      mem_dqs,
   inout  tri   [PORT_MEM_DQS_N_WIDTH-1:0]                    mem_dqs_n,
   input  logic [PORT_MEM_QK_WIDTH-1:0]                       mem_qk,
   input  logic [PORT_MEM_QK_N_WIDTH-1:0]                     mem_qk_n,
   input  logic [PORT_MEM_QKA_WIDTH-1:0]                      mem_qka,
   input  logic [PORT_MEM_QKA_N_WIDTH-1:0]                    mem_qka_n,
   input  logic [PORT_MEM_QKB_WIDTH-1:0]                      mem_qkb,
   input  logic [PORT_MEM_QKB_N_WIDTH-1:0]                    mem_qkb_n,
   input  logic [PORT_MEM_CQ_WIDTH-1:0]                       mem_cq,
   input  logic [PORT_MEM_CQ_N_WIDTH-1:0]                     mem_cq_n,
   input  logic [PORT_MEM_ALERT_N_WIDTH-1:0]                  mem_alert_n,
   input  logic [PORT_MEM_PE_N_WIDTH-1:0]                     mem_pe_n,

   // Ports for "afi" interface
   //AUTOGEN_BEGIN: Definition of afi ports
   output logic                                               afi_cal_success,
   output logic                                               afi_cal_fail,
   input  logic                                               afi_cal_req,
   output logic [PORT_AFI_RLAT_WIDTH-1:0]                     afi_rlat,
   output logic [PORT_AFI_WLAT_WIDTH-1:0]                     afi_wlat,
   output logic [PORT_AFI_SEQ_BUSY_WIDTH-1:0]                 afi_seq_busy,
   input  logic                                               afi_ctl_refresh_done,
   input  logic                                               afi_ctl_long_idle,
   input  logic                                               afi_mps_req,
   output logic                                               afi_mps_ack,
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

   // Ports for "ctrl_ast_cmd" interfaces
   output logic                                               ast_cmd_ready_0,
   input  logic                                               ast_cmd_valid_0,
   input  logic [PORT_CTRL_AST_CMD_DATA_WIDTH-1:0]            ast_cmd_data_0,

   output logic                                               ast_cmd_ready_1,
   input  logic                                               ast_cmd_valid_1,
   input  logic [PORT_CTRL_AST_CMD_DATA_WIDTH-1:0]            ast_cmd_data_1,

   // Ports for "ctrl_ast_wr" interfaces
   output logic                                               ast_wr_ready_0,
   input  logic                                               ast_wr_valid_0,
   input  logic [PORT_CTRL_AST_WR_DATA_WIDTH-1:0]             ast_wr_data_0,

   output logic                                               ast_wr_ready_1,
   input  logic                                               ast_wr_valid_1,
   input  logic [PORT_CTRL_AST_WR_DATA_WIDTH-1:0]             ast_wr_data_1,

   // Ports for "ctrl_ast_rd" interfaces
   input  logic                                               ast_rd_ready_0,
   output logic                                               ast_rd_valid_0,
   output logic [PORT_CTRL_AST_RD_DATA_WIDTH-1:0]             ast_rd_data_0,

   input  logic                                               ast_rd_ready_1,
   output logic                                               ast_rd_valid_1,
   output logic [PORT_CTRL_AST_RD_DATA_WIDTH-1:0]             ast_rd_data_1,

   // Ports for "ctrl_amm" interfaces
   input  logic                                               amm_write_0,
   input  logic                                               amm_read_0,
   output logic                                               amm_ready_0,
   output logic [PORT_CTRL_AMM_RDATA_WIDTH-1:0]               amm_readdata_0,
   input  logic [PORT_CTRL_AMM_ADDRESS_WIDTH-1:0]             amm_address_0,
   input  logic [PORT_CTRL_AMM_WDATA_WIDTH-1:0]               amm_writedata_0,
   input  logic [PORT_CTRL_AMM_BCOUNT_WIDTH-1:0]              amm_burstcount_0,
   input  logic [PORT_CTRL_AMM_BYTEEN_WIDTH-1:0]              amm_byteenable_0,
   input  logic                                               amm_beginbursttransfer_0,
   output logic                                               amm_readdatavalid_0,

   input  logic                                               amm_write_1,
   input  logic                                               amm_read_1,
   output logic                                               amm_ready_1,
   output logic [PORT_CTRL_AMM_RDATA_WIDTH-1:0]               amm_readdata_1,
   input  logic [PORT_CTRL_AMM_ADDRESS_WIDTH-1:0]             amm_address_1,
   input  logic [PORT_CTRL_AMM_WDATA_WIDTH-1:0]               amm_writedata_1,
   input  logic [PORT_CTRL_AMM_BCOUNT_WIDTH-1:0]              amm_burstcount_1,
   input  logic [PORT_CTRL_AMM_BYTEEN_WIDTH-1:0]              amm_byteenable_1,
   input  logic                                               amm_beginbursttransfer_1,
   output logic                                               amm_readdatavalid_1,

   // Ports for "ctrl_user_priority" interface
   input  logic                                               ctrl_user_priority_hi_0,
   input  logic                                               ctrl_user_priority_hi_1,

   // Ports for "ctrl_auto_precharge" interface
   input  logic                                               ctrl_auto_precharge_req_0,
   input  logic                                               ctrl_auto_precharge_req_1,

   // Ports for "ctrl_user_refresh" interface
   input  logic [PORT_CTRL_USER_REFRESH_REQ_WIDTH-1:0]        ctrl_user_refresh_req,
   input  logic [PORT_CTRL_USER_REFRESH_BANK_WIDTH-1:0]       ctrl_user_refresh_bank,
   output logic                                               ctrl_user_refresh_ack,

   // Ports for "ctrl_self_refresh" interface
   input  logic [PORT_CTRL_SELF_REFRESH_REQ_WIDTH-1:0]        ctrl_self_refresh_req,
   output logic                                               ctrl_self_refresh_ack,

   // Ports for "ctrl_will_refresh" interface
   output logic                                               ctrl_will_refresh,

   // Ports for "ctrl_deep_power_down" interface
   input  logic                                               ctrl_deep_power_down_req,
   output logic                                               ctrl_deep_power_down_ack,

   // Ports for "ctrl_power_down" interface
   output logic                                               ctrl_power_down_ack,

   // Ports for "ctrl_zq_cal" interface
   input  logic                                               ctrl_zq_cal_long_req,
   input  logic                                               ctrl_zq_cal_short_req,
   output logic                                               ctrl_zq_cal_ack,

   // Ports for "ctrl_ecc" interface
   input  logic [PORT_CTRL_ECC_WRITE_INFO_WIDTH-1:0]          ctrl_ecc_write_info_0,
   output logic [PORT_CTRL_ECC_RDATA_ID_WIDTH-1:0]            ctrl_ecc_rdata_id_0,
   output logic [PORT_CTRL_ECC_WB_POINTER_WIDTH-1:0]          ctrl_ecc_wr_pointer_info_0,
   output logic [PORT_CTRL_ECC_READ_INFO_WIDTH-1:0]           ctrl_ecc_read_info_0,
   output logic [PORT_CTRL_ECC_CMD_INFO_WIDTH-1:0]            ctrl_ecc_cmd_info_0,
   output logic                                               ctrl_ecc_idle_0,

   // Ports for "ctrl_ecc" interface
   input  logic [PORT_CTRL_ECC_WRITE_INFO_WIDTH-1:0]          ctrl_ecc_write_info_1,
   output logic [PORT_CTRL_ECC_RDATA_ID_WIDTH-1:0]            ctrl_ecc_rdata_id_1,
   output logic [PORT_CTRL_ECC_WB_POINTER_WIDTH-1:0]          ctrl_ecc_wr_pointer_info_1,
   output logic [PORT_CTRL_ECC_READ_INFO_WIDTH-1:0]           ctrl_ecc_read_info_1,
   output logic [PORT_CTRL_ECC_CMD_INFO_WIDTH-1:0]            ctrl_ecc_cmd_info_1,
   output logic                                               ctrl_ecc_idle_1,

   // Ports for "ctrl_mmr" interface
   output logic                                               mmr_slave_waitrequest_0,
   input  logic                                               mmr_slave_read_0,
   input  logic                                               mmr_slave_write_0,
   input  logic [PORT_CTRL_MMR_SLAVE_ADDRESS_WIDTH-1:0]       mmr_slave_address_0,
   output logic [PORT_CTRL_MMR_SLAVE_RDATA_WIDTH-1:0]         mmr_slave_readdata_0,
   input  logic [PORT_CTRL_MMR_SLAVE_WDATA_WIDTH-1:0]         mmr_slave_writedata_0,
   input  logic [PORT_CTRL_MMR_SLAVE_BCOUNT_WIDTH-1:0]        mmr_slave_burstcount_0,
   input  logic                                               mmr_slave_beginbursttransfer_0,
   output logic                                               mmr_slave_readdatavalid_0,

   // Ports for "ctrl_mmr" interface
   output logic                                               mmr_slave_waitrequest_1,
   input  logic                                               mmr_slave_read_1,
   input  logic                                               mmr_slave_write_1,
   input  logic [PORT_CTRL_MMR_SLAVE_ADDRESS_WIDTH-1:0]       mmr_slave_address_1,
   output logic [PORT_CTRL_MMR_SLAVE_RDATA_WIDTH-1:0]         mmr_slave_readdata_1,
   input  logic [PORT_CTRL_MMR_SLAVE_WDATA_WIDTH-1:0]         mmr_slave_writedata_1,
   input  logic [PORT_CTRL_MMR_SLAVE_BCOUNT_WIDTH-1:0]        mmr_slave_burstcount_1,
   input  logic                                               mmr_slave_beginbursttransfer_1,
   output logic                                               mmr_slave_readdatavalid_1,

   // Ports for the HPS<->EMIF conduit
   input  logic [PORT_HPS_EMIF_H2E_WIDTH-1:0]                 hps_to_emif,
   output logic [PORT_HPS_EMIF_E2H_WIDTH-1:0]                 emif_to_hps,
   input  logic [PORT_HPS_EMIF_H2E_GP_WIDTH-1:0]              hps_to_emif_gp,
   output logic [PORT_HPS_EMIF_E2H_GP_WIDTH-1:0]              emif_to_hps_gp,

   // Output/input clock/reset intended for core slave logic that interacts with the sequencer CPU
   output logic                                               cal_slave_clk,
   output logic                                               cal_slave_reset_n,
   input  logic                                               cal_slave_clk_in,
   input  logic                                               cal_slave_reset_n_in,

   // Output clock/reset intended for core master logic that interacts with the sequencer CPU
   output logic                                               cal_master_clk,
   output logic                                               cal_master_reset_n,

   // Input clock/reset intended for core logic connected to the Avalon slave port of the sequencer CPU.
   // The "out" clock/reset is intended for daisy-chaining logic from multiple interfaces.
   input  logic                                               cal_debug_clk,
   input  logic                                               cal_debug_reset_n,
   output logic                                               cal_debug_out_clk,
   output logic                                               cal_debug_out_reset_n,

   // Ports for "cal_debug" interface
   input  logic [PORT_CAL_DEBUG_ADDRESS_WIDTH-1:0]            cal_debug_addr,
   input  logic [PORT_CAL_DEBUG_BYTEEN_WIDTH-1:0]             cal_debug_byteenable,
   input  logic                                               cal_debug_read,
   input  logic                                               cal_debug_write,
   input  logic [PORT_CAL_DEBUG_WDATA_WIDTH-1:0]              cal_debug_write_data,
   output logic [PORT_CAL_DEBUG_RDATA_WIDTH-1:0]              cal_debug_read_data,
   output logic                                               cal_debug_read_data_valid,
   output logic                                               cal_debug_waitrequest,

   // Ports for "cal_debug_out" interface
   output logic [PORT_CAL_DEBUG_OUT_ADDRESS_WIDTH-1:0]        cal_debug_out_addr,
   output logic [PORT_CAL_DEBUG_OUT_BYTEEN_WIDTH-1:0]         cal_debug_out_byteenable,
   output logic                                               cal_debug_out_read,
   output logic                                               cal_debug_out_write,
   output logic [PORT_CAL_DEBUG_OUT_WDATA_WIDTH-1:0]          cal_debug_out_write_data,
   input  logic [PORT_CAL_DEBUG_OUT_RDATA_WIDTH-1:0]          cal_debug_out_read_data,
   input  logic                                               cal_debug_out_read_data_valid,
   input  logic                                               cal_debug_out_waitrequest,

   // Ports for "cal_master" interface
   output logic [PORT_CAL_MASTER_ADDRESS_WIDTH-1:0]           cal_master_addr,
   output logic [PORT_CAL_MASTER_BYTEEN_WIDTH-1:0]            cal_master_byteenable,
   output logic                                               cal_master_burstcount,
   output logic                                               cal_master_debugaccess,
   output logic                                               cal_master_read,
   output logic                                               cal_master_write,
   output logic [PORT_CAL_MASTER_WDATA_WIDTH-1:0]             cal_master_write_data,
   input  logic [PORT_CAL_MASTER_RDATA_WIDTH-1:0]             cal_master_read_data,
   input  logic                                               cal_master_read_data_valid,
   input  logic                                               cal_master_waitrequest,

   // Ports for internal test and debug
   input  logic [PORT_DFT_NF_IOAUX_PIO_IN_WIDTH-1:0]          ioaux_pio_in,
   output logic [PORT_DFT_NF_IOAUX_PIO_OUT_WIDTH-1:0]         ioaux_pio_out,
   input  logic                                               pa_dprio_clk,
   input  logic                                               pa_dprio_read,
   input  logic [PORT_DFT_NF_PA_DPRIO_REG_ADDR_WIDTH-1:0]     pa_dprio_reg_addr,
   input  logic                                               pa_dprio_rst_n,
   input  logic                                               pa_dprio_write,
   input  logic [PORT_DFT_NF_PA_DPRIO_WRITEDATA_WIDTH-1:0]    pa_dprio_writedata,
   output logic                                               pa_dprio_block_select,
   output logic [PORT_DFT_NF_PA_DPRIO_READDATA_WIDTH-1:0]     pa_dprio_readdata,
   input  logic                                               pll_phase_en,
   input  logic                                               pll_up_dn,
   input  logic [PORT_DFT_NF_PLL_CNTSEL_WIDTH-1:0]            pll_cnt_sel,
   input  logic [PORT_DFT_NF_PLL_NUM_SHIFT_WIDTH-1:0]         pll_num_phase_shifts,
   output logic                                               pll_phase_done,
   output logic [PORT_DFT_NF_CORE_CLK_BUF_OUT_WIDTH-1:0]      dft_core_clk_buf_out,
   output logic [PORT_DFT_NF_CORE_CLK_LOCKED_WIDTH-1:0]       dft_core_clk_locked
);
   timeunit 1ns;
   timeprecision 1ps;

   // Below is used to override the user selection for ABSTRACT PHY for synthesis
   // synthesis read_comments_as_HDL on
   // `define DISABLE_ABSTRACT_PHY_FOR_SYNTH TRUE
   // synthesis read_comments_as_HDL off

   `ifdef DISABLE_ABSTRACT_PHY_FOR_SYNTH
     localparam DIAG_USE_ABSTRACT_PHY_AFT_SYNTH_OVRD  = 0;
   `else
     localparam DIAG_USE_ABSTRACT_PHY_AFT_SYNTH_OVRD  = DIAG_USE_ABSTRACT_PHY;
   `endif

   // Assertions
   initial begin
      assert(LANES_USAGE_AUTOGEN_WCNT                 == 4) else $fatal("LANES_USAGE_AUTOGEN_WCNT != 4 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PINS_USAGE_AUTOGEN_WCNT                  == 13) else $fatal("PINS_USAGE_AUTOGEN_WCNT != 13 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PINS_RATE_AUTOGEN_WCNT                   == 13) else $fatal("PINS_RATE_AUTOGEN_WCNT != 13 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PINS_WDB_AUTOGEN_WCNT                    == 39) else $fatal("PINS_WDB_AUTOGEN_WCNT != 39 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PINS_DATA_IN_MODE_AUTOGEN_WCNT           == 39) else $fatal("PINS_DATA_IN_MODE_AUTOGEN_WCNT != 39 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PINS_C2L_DRIVEN_AUTOGEN_WCNT             == 13) else $fatal("PINS_C2L_DRIVEN_AUTOGEN_WCNT != 13 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PINS_DB_IN_BYPASS_AUTOGEN_WCNT           == 13) else $fatal("PINS_DB_IN_BYPASS_AUTOGEN_WCNT != 13 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PINS_DB_OUT_BYPASS_AUTOGEN_WCNT          == 13) else $fatal("PINS_DB_OUT_BYPASS_AUTOGEN_WCNT != 13 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PINS_DB_OE_BYPASS_AUTOGEN_WCNT           == 13) else $fatal("PINS_DB_OE_BYPASS_AUTOGEN_WCNT != 13 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PINS_INVERT_WR_AUTOGEN_WCNT              == 13) else $fatal("PINS_INVERT_WR_AUTOGEN_WCNT != 13 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PINS_INVERT_OE_AUTOGEN_WCNT              == 13) else $fatal("PINS_INVERT_OE_AUTOGEN_WCNT != 13 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PINS_AC_HMC_DATA_OVERRIDE_ENA_AUTOGEN_WCNT == 13) else $fatal("PINS_AC_HMC_DATA_OVERRIDE_ENA_AUTOGEN_WCNT != 13 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PINS_OCT_MODE_AUTOGEN_WCNT               == 13) else $fatal("PINS_OCT_MODE_AUTOGEN_WCNT != 13 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PINS_GPIO_MODE_AUTOGEN_WCNT              == 13) else $fatal("PINS_GPIO_MODE_AUTOGEN_WCNT != 13 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(UNUSED_MEM_PINS_PINLOC_AUTOGEN_WCNT      == 129) else $fatal("UNUSED_MEM_PINS_PINLOC_AUTOGEN_WCNT != 129 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(UNUSED_DQS_BUSES_LANELOC_AUTOGEN_WCNT    == 11) else $fatal("UNUSED_DQS_BUSES_LANELOC_AUTOGEN_WCNT != 11 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(CENTER_TIDS_AUTOGEN_WCNT                 == 3) else $fatal("CENTER_TIDS_AUTOGEN_WCNT != 3 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(HMC_TIDS_AUTOGEN_WCNT                    == 3) else $fatal("HMC_TIDS_AUTOGEN_WCNT != 3 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(LANE_TIDS_AUTOGEN_WCNT                   == 10) else $fatal("LANE_TIDS_AUTOGEN_WCNT != 10 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_CK_PINLOC_AUTOGEN_WCNT          == 6) else $fatal("PORT_MEM_CK_PINLOC_AUTOGEN_WCNT != 6 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_CK_N_PINLOC_AUTOGEN_WCNT        == 6) else $fatal("PORT_MEM_CK_N_PINLOC_AUTOGEN_WCNT != 6 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_DK_PINLOC_AUTOGEN_WCNT          == 6) else $fatal("PORT_MEM_DK_PINLOC_AUTOGEN_WCNT != 6 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_DK_N_PINLOC_AUTOGEN_WCNT        == 6) else $fatal("PORT_MEM_DK_N_PINLOC_AUTOGEN_WCNT != 6 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_DKA_PINLOC_AUTOGEN_WCNT         == 6) else $fatal("PORT_MEM_DKA_PINLOC_AUTOGEN_WCNT != 6 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_DKA_N_PINLOC_AUTOGEN_WCNT       == 6) else $fatal("PORT_MEM_DKA_N_PINLOC_AUTOGEN_WCNT != 6 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_DKB_PINLOC_AUTOGEN_WCNT         == 6) else $fatal("PORT_MEM_DKB_PINLOC_AUTOGEN_WCNT != 6 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_DKB_N_PINLOC_AUTOGEN_WCNT       == 6) else $fatal("PORT_MEM_DKB_N_PINLOC_AUTOGEN_WCNT != 6 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_K_PINLOC_AUTOGEN_WCNT           == 6) else $fatal("PORT_MEM_K_PINLOC_AUTOGEN_WCNT != 6 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_K_N_PINLOC_AUTOGEN_WCNT         == 6) else $fatal("PORT_MEM_K_N_PINLOC_AUTOGEN_WCNT != 6 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_A_PINLOC_AUTOGEN_WCNT           == 17) else $fatal("PORT_MEM_A_PINLOC_AUTOGEN_WCNT != 17 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_BA_PINLOC_AUTOGEN_WCNT          == 6) else $fatal("PORT_MEM_BA_PINLOC_AUTOGEN_WCNT != 6 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_BG_PINLOC_AUTOGEN_WCNT          == 6) else $fatal("PORT_MEM_BG_PINLOC_AUTOGEN_WCNT != 6 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_C_PINLOC_AUTOGEN_WCNT           == 6) else $fatal("PORT_MEM_C_PINLOC_AUTOGEN_WCNT != 6 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_CKE_PINLOC_AUTOGEN_WCNT         == 6) else $fatal("PORT_MEM_CKE_PINLOC_AUTOGEN_WCNT != 6 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_CS_N_PINLOC_AUTOGEN_WCNT        == 6) else $fatal("PORT_MEM_CS_N_PINLOC_AUTOGEN_WCNT != 6 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_RM_PINLOC_AUTOGEN_WCNT          == 6) else $fatal("PORT_MEM_RM_PINLOC_AUTOGEN_WCNT != 6 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_ODT_PINLOC_AUTOGEN_WCNT         == 6) else $fatal("PORT_MEM_ODT_PINLOC_AUTOGEN_WCNT != 6 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_RAS_N_PINLOC_AUTOGEN_WCNT       == 2) else $fatal("PORT_MEM_RAS_N_PINLOC_AUTOGEN_WCNT != 2 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_CAS_N_PINLOC_AUTOGEN_WCNT       == 2) else $fatal("PORT_MEM_CAS_N_PINLOC_AUTOGEN_WCNT != 2 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_WE_N_PINLOC_AUTOGEN_WCNT        == 2) else $fatal("PORT_MEM_WE_N_PINLOC_AUTOGEN_WCNT != 2 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_RESET_N_PINLOC_AUTOGEN_WCNT     == 2) else $fatal("PORT_MEM_RESET_N_PINLOC_AUTOGEN_WCNT != 2 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_ACT_N_PINLOC_AUTOGEN_WCNT       == 2) else $fatal("PORT_MEM_ACT_N_PINLOC_AUTOGEN_WCNT != 2 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_PAR_PINLOC_AUTOGEN_WCNT         == 2) else $fatal("PORT_MEM_PAR_PINLOC_AUTOGEN_WCNT != 2 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_CA_PINLOC_AUTOGEN_WCNT          == 17) else $fatal("PORT_MEM_CA_PINLOC_AUTOGEN_WCNT != 17 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_REF_N_PINLOC_AUTOGEN_WCNT       == 1) else $fatal("PORT_MEM_REF_N_PINLOC_AUTOGEN_WCNT != 1 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_WPS_N_PINLOC_AUTOGEN_WCNT       == 1) else $fatal("PORT_MEM_WPS_N_PINLOC_AUTOGEN_WCNT != 1 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_RPS_N_PINLOC_AUTOGEN_WCNT       == 1) else $fatal("PORT_MEM_RPS_N_PINLOC_AUTOGEN_WCNT != 1 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_DOFF_N_PINLOC_AUTOGEN_WCNT      == 1) else $fatal("PORT_MEM_DOFF_N_PINLOC_AUTOGEN_WCNT != 1 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_LDA_N_PINLOC_AUTOGEN_WCNT       == 1) else $fatal("PORT_MEM_LDA_N_PINLOC_AUTOGEN_WCNT != 1 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_LDB_N_PINLOC_AUTOGEN_WCNT       == 1) else $fatal("PORT_MEM_LDB_N_PINLOC_AUTOGEN_WCNT != 1 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_RWA_N_PINLOC_AUTOGEN_WCNT       == 1) else $fatal("PORT_MEM_RWA_N_PINLOC_AUTOGEN_WCNT != 1 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_RWB_N_PINLOC_AUTOGEN_WCNT       == 1) else $fatal("PORT_MEM_RWB_N_PINLOC_AUTOGEN_WCNT != 1 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_LBK0_N_PINLOC_AUTOGEN_WCNT      == 1) else $fatal("PORT_MEM_LBK0_N_PINLOC_AUTOGEN_WCNT != 1 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_LBK1_N_PINLOC_AUTOGEN_WCNT      == 1) else $fatal("PORT_MEM_LBK1_N_PINLOC_AUTOGEN_WCNT != 1 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_CFG_N_PINLOC_AUTOGEN_WCNT       == 1) else $fatal("PORT_MEM_CFG_N_PINLOC_AUTOGEN_WCNT != 1 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_AP_PINLOC_AUTOGEN_WCNT          == 1) else $fatal("PORT_MEM_AP_PINLOC_AUTOGEN_WCNT != 1 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_AINV_PINLOC_AUTOGEN_WCNT        == 1) else $fatal("PORT_MEM_AINV_PINLOC_AUTOGEN_WCNT != 1 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_DM_PINLOC_AUTOGEN_WCNT          == 13) else $fatal("PORT_MEM_DM_PINLOC_AUTOGEN_WCNT != 13 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_BWS_N_PINLOC_AUTOGEN_WCNT       == 3) else $fatal("PORT_MEM_BWS_N_PINLOC_AUTOGEN_WCNT != 3 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_D_PINLOC_AUTOGEN_WCNT           == 49) else $fatal("PORT_MEM_D_PINLOC_AUTOGEN_WCNT != 49 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_DQ_PINLOC_AUTOGEN_WCNT          == 49) else $fatal("PORT_MEM_DQ_PINLOC_AUTOGEN_WCNT != 49 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_DBI_N_PINLOC_AUTOGEN_WCNT       == 7) else $fatal("PORT_MEM_DBI_N_PINLOC_AUTOGEN_WCNT != 7 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_DQA_PINLOC_AUTOGEN_WCNT         == 49) else $fatal("PORT_MEM_DQA_PINLOC_AUTOGEN_WCNT != 49 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_DQB_PINLOC_AUTOGEN_WCNT         == 49) else $fatal("PORT_MEM_DQB_PINLOC_AUTOGEN_WCNT != 49 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_DINVA_PINLOC_AUTOGEN_WCNT       == 3) else $fatal("PORT_MEM_DINVA_PINLOC_AUTOGEN_WCNT != 3 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_DINVB_PINLOC_AUTOGEN_WCNT       == 3) else $fatal("PORT_MEM_DINVB_PINLOC_AUTOGEN_WCNT != 3 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_Q_PINLOC_AUTOGEN_WCNT           == 49) else $fatal("PORT_MEM_Q_PINLOC_AUTOGEN_WCNT != 49 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_DQS_PINLOC_AUTOGEN_WCNT         == 13) else $fatal("PORT_MEM_DQS_PINLOC_AUTOGEN_WCNT != 13 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_DQS_N_PINLOC_AUTOGEN_WCNT       == 13) else $fatal("PORT_MEM_DQS_N_PINLOC_AUTOGEN_WCNT != 13 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_QK_PINLOC_AUTOGEN_WCNT          == 6) else $fatal("PORT_MEM_QK_PINLOC_AUTOGEN_WCNT != 6 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_QK_N_PINLOC_AUTOGEN_WCNT        == 6) else $fatal("PORT_MEM_QK_N_PINLOC_AUTOGEN_WCNT != 6 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_QKA_PINLOC_AUTOGEN_WCNT         == 6) else $fatal("PORT_MEM_QKA_PINLOC_AUTOGEN_WCNT != 6 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_QKA_N_PINLOC_AUTOGEN_WCNT       == 6) else $fatal("PORT_MEM_QKA_N_PINLOC_AUTOGEN_WCNT != 6 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_QKB_PINLOC_AUTOGEN_WCNT         == 6) else $fatal("PORT_MEM_QKB_PINLOC_AUTOGEN_WCNT != 6 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_QKB_N_PINLOC_AUTOGEN_WCNT       == 6) else $fatal("PORT_MEM_QKB_N_PINLOC_AUTOGEN_WCNT != 6 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_CQ_PINLOC_AUTOGEN_WCNT          == 2) else $fatal("PORT_MEM_CQ_PINLOC_AUTOGEN_WCNT != 2 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_CQ_N_PINLOC_AUTOGEN_WCNT        == 2) else $fatal("PORT_MEM_CQ_N_PINLOC_AUTOGEN_WCNT != 2 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_ALERT_N_PINLOC_AUTOGEN_WCNT     == 2) else $fatal("PORT_MEM_ALERT_N_PINLOC_AUTOGEN_WCNT != 2 - Parameter definitions in RTL and Tcl generation code are out of sync!");
      assert(PORT_MEM_PE_N_PINLOC_AUTOGEN_WCNT        == 2) else $fatal("PORT_MEM_PE_N_PINLOC_AUTOGEN_WCNT != 2 - Parameter definitions in RTL and Tcl generation code are out of sync!");
   end

   // Derive localparam values
   //AUTOGEN_BEGIN: Derive bit-vector parameters
   localparam LANES_USAGE                   = {LANES_USAGE_3[29:0],LANES_USAGE_2[29:0],LANES_USAGE_1[29:0],LANES_USAGE_0[29:0]};
   localparam PINS_USAGE                    = {PINS_USAGE_12[29:0],PINS_USAGE_11[29:0],PINS_USAGE_10[29:0],PINS_USAGE_9[29:0],PINS_USAGE_8[29:0],PINS_USAGE_7[29:0],PINS_USAGE_6[29:0],PINS_USAGE_5[29:0],PINS_USAGE_4[29:0],PINS_USAGE_3[29:0],PINS_USAGE_2[29:0],PINS_USAGE_1[29:0],PINS_USAGE_0[29:0]};
   localparam PINS_RATE                     = {PINS_RATE_12[29:0],PINS_RATE_11[29:0],PINS_RATE_10[29:0],PINS_RATE_9[29:0],PINS_RATE_8[29:0],PINS_RATE_7[29:0],PINS_RATE_6[29:0],PINS_RATE_5[29:0],PINS_RATE_4[29:0],PINS_RATE_3[29:0],PINS_RATE_2[29:0],PINS_RATE_1[29:0],PINS_RATE_0[29:0]};
   localparam PINS_WDB                      = {PINS_WDB_38[29:0],PINS_WDB_37[29:0],PINS_WDB_36[29:0],PINS_WDB_35[29:0],PINS_WDB_34[29:0],PINS_WDB_33[29:0],PINS_WDB_32[29:0],PINS_WDB_31[29:0],PINS_WDB_30[29:0],PINS_WDB_29[29:0],PINS_WDB_28[29:0],PINS_WDB_27[29:0],PINS_WDB_26[29:0],PINS_WDB_25[29:0],PINS_WDB_24[29:0],PINS_WDB_23[29:0],PINS_WDB_22[29:0],PINS_WDB_21[29:0],PINS_WDB_20[29:0],PINS_WDB_19[29:0],PINS_WDB_18[29:0],PINS_WDB_17[29:0],PINS_WDB_16[29:0],PINS_WDB_15[29:0],PINS_WDB_14[29:0],PINS_WDB_13[29:0],PINS_WDB_12[29:0],PINS_WDB_11[29:0],PINS_WDB_10[29:0],PINS_WDB_9[29:0],PINS_WDB_8[29:0],PINS_WDB_7[29:0],PINS_WDB_6[29:0],PINS_WDB_5[29:0],PINS_WDB_4[29:0],PINS_WDB_3[29:0],PINS_WDB_2[29:0],PINS_WDB_1[29:0],PINS_WDB_0[29:0]};
   localparam PINS_DATA_IN_MODE             = {PINS_DATA_IN_MODE_38[29:0],PINS_DATA_IN_MODE_37[29:0],PINS_DATA_IN_MODE_36[29:0],PINS_DATA_IN_MODE_35[29:0],PINS_DATA_IN_MODE_34[29:0],PINS_DATA_IN_MODE_33[29:0],PINS_DATA_IN_MODE_32[29:0],PINS_DATA_IN_MODE_31[29:0],PINS_DATA_IN_MODE_30[29:0],PINS_DATA_IN_MODE_29[29:0],PINS_DATA_IN_MODE_28[29:0],PINS_DATA_IN_MODE_27[29:0],PINS_DATA_IN_MODE_26[29:0],PINS_DATA_IN_MODE_25[29:0],PINS_DATA_IN_MODE_24[29:0],PINS_DATA_IN_MODE_23[29:0],PINS_DATA_IN_MODE_22[29:0],PINS_DATA_IN_MODE_21[29:0],PINS_DATA_IN_MODE_20[29:0],PINS_DATA_IN_MODE_19[29:0],PINS_DATA_IN_MODE_18[29:0],PINS_DATA_IN_MODE_17[29:0],PINS_DATA_IN_MODE_16[29:0],PINS_DATA_IN_MODE_15[29:0],PINS_DATA_IN_MODE_14[29:0],PINS_DATA_IN_MODE_13[29:0],PINS_DATA_IN_MODE_12[29:0],PINS_DATA_IN_MODE_11[29:0],PINS_DATA_IN_MODE_10[29:0],PINS_DATA_IN_MODE_9[29:0],PINS_DATA_IN_MODE_8[29:0],PINS_DATA_IN_MODE_7[29:0],PINS_DATA_IN_MODE_6[29:0],PINS_DATA_IN_MODE_5[29:0],PINS_DATA_IN_MODE_4[29:0],PINS_DATA_IN_MODE_3[29:0],PINS_DATA_IN_MODE_2[29:0],PINS_DATA_IN_MODE_1[29:0],PINS_DATA_IN_MODE_0[29:0]};
   localparam PINS_C2L_DRIVEN               = {PINS_C2L_DRIVEN_12[29:0],PINS_C2L_DRIVEN_11[29:0],PINS_C2L_DRIVEN_10[29:0],PINS_C2L_DRIVEN_9[29:0],PINS_C2L_DRIVEN_8[29:0],PINS_C2L_DRIVEN_7[29:0],PINS_C2L_DRIVEN_6[29:0],PINS_C2L_DRIVEN_5[29:0],PINS_C2L_DRIVEN_4[29:0],PINS_C2L_DRIVEN_3[29:0],PINS_C2L_DRIVEN_2[29:0],PINS_C2L_DRIVEN_1[29:0],PINS_C2L_DRIVEN_0[29:0]};
   localparam PINS_DB_IN_BYPASS             = {PINS_DB_IN_BYPASS_12[29:0],PINS_DB_IN_BYPASS_11[29:0],PINS_DB_IN_BYPASS_10[29:0],PINS_DB_IN_BYPASS_9[29:0],PINS_DB_IN_BYPASS_8[29:0],PINS_DB_IN_BYPASS_7[29:0],PINS_DB_IN_BYPASS_6[29:0],PINS_DB_IN_BYPASS_5[29:0],PINS_DB_IN_BYPASS_4[29:0],PINS_DB_IN_BYPASS_3[29:0],PINS_DB_IN_BYPASS_2[29:0],PINS_DB_IN_BYPASS_1[29:0],PINS_DB_IN_BYPASS_0[29:0]};
   localparam PINS_DB_OUT_BYPASS            = {PINS_DB_OUT_BYPASS_12[29:0],PINS_DB_OUT_BYPASS_11[29:0],PINS_DB_OUT_BYPASS_10[29:0],PINS_DB_OUT_BYPASS_9[29:0],PINS_DB_OUT_BYPASS_8[29:0],PINS_DB_OUT_BYPASS_7[29:0],PINS_DB_OUT_BYPASS_6[29:0],PINS_DB_OUT_BYPASS_5[29:0],PINS_DB_OUT_BYPASS_4[29:0],PINS_DB_OUT_BYPASS_3[29:0],PINS_DB_OUT_BYPASS_2[29:0],PINS_DB_OUT_BYPASS_1[29:0],PINS_DB_OUT_BYPASS_0[29:0]};
   localparam PINS_DB_OE_BYPASS             = {PINS_DB_OE_BYPASS_12[29:0],PINS_DB_OE_BYPASS_11[29:0],PINS_DB_OE_BYPASS_10[29:0],PINS_DB_OE_BYPASS_9[29:0],PINS_DB_OE_BYPASS_8[29:0],PINS_DB_OE_BYPASS_7[29:0],PINS_DB_OE_BYPASS_6[29:0],PINS_DB_OE_BYPASS_5[29:0],PINS_DB_OE_BYPASS_4[29:0],PINS_DB_OE_BYPASS_3[29:0],PINS_DB_OE_BYPASS_2[29:0],PINS_DB_OE_BYPASS_1[29:0],PINS_DB_OE_BYPASS_0[29:0]};
   localparam PINS_INVERT_WR                = {PINS_INVERT_WR_12[29:0],PINS_INVERT_WR_11[29:0],PINS_INVERT_WR_10[29:0],PINS_INVERT_WR_9[29:0],PINS_INVERT_WR_8[29:0],PINS_INVERT_WR_7[29:0],PINS_INVERT_WR_6[29:0],PINS_INVERT_WR_5[29:0],PINS_INVERT_WR_4[29:0],PINS_INVERT_WR_3[29:0],PINS_INVERT_WR_2[29:0],PINS_INVERT_WR_1[29:0],PINS_INVERT_WR_0[29:0]};
   localparam PINS_INVERT_OE                = {PINS_INVERT_OE_12[29:0],PINS_INVERT_OE_11[29:0],PINS_INVERT_OE_10[29:0],PINS_INVERT_OE_9[29:0],PINS_INVERT_OE_8[29:0],PINS_INVERT_OE_7[29:0],PINS_INVERT_OE_6[29:0],PINS_INVERT_OE_5[29:0],PINS_INVERT_OE_4[29:0],PINS_INVERT_OE_3[29:0],PINS_INVERT_OE_2[29:0],PINS_INVERT_OE_1[29:0],PINS_INVERT_OE_0[29:0]};
   localparam PINS_AC_HMC_DATA_OVERRIDE_ENA = {PINS_AC_HMC_DATA_OVERRIDE_ENA_12[29:0],PINS_AC_HMC_DATA_OVERRIDE_ENA_11[29:0],PINS_AC_HMC_DATA_OVERRIDE_ENA_10[29:0],PINS_AC_HMC_DATA_OVERRIDE_ENA_9[29:0],PINS_AC_HMC_DATA_OVERRIDE_ENA_8[29:0],PINS_AC_HMC_DATA_OVERRIDE_ENA_7[29:0],PINS_AC_HMC_DATA_OVERRIDE_ENA_6[29:0],PINS_AC_HMC_DATA_OVERRIDE_ENA_5[29:0],PINS_AC_HMC_DATA_OVERRIDE_ENA_4[29:0],PINS_AC_HMC_DATA_OVERRIDE_ENA_3[29:0],PINS_AC_HMC_DATA_OVERRIDE_ENA_2[29:0],PINS_AC_HMC_DATA_OVERRIDE_ENA_1[29:0],PINS_AC_HMC_DATA_OVERRIDE_ENA_0[29:0]};
   localparam PINS_OCT_MODE                 = {PINS_OCT_MODE_12[29:0],PINS_OCT_MODE_11[29:0],PINS_OCT_MODE_10[29:0],PINS_OCT_MODE_9[29:0],PINS_OCT_MODE_8[29:0],PINS_OCT_MODE_7[29:0],PINS_OCT_MODE_6[29:0],PINS_OCT_MODE_5[29:0],PINS_OCT_MODE_4[29:0],PINS_OCT_MODE_3[29:0],PINS_OCT_MODE_2[29:0],PINS_OCT_MODE_1[29:0],PINS_OCT_MODE_0[29:0]};
   localparam PINS_GPIO_MODE                = {PINS_GPIO_MODE_12[29:0],PINS_GPIO_MODE_11[29:0],PINS_GPIO_MODE_10[29:0],PINS_GPIO_MODE_9[29:0],PINS_GPIO_MODE_8[29:0],PINS_GPIO_MODE_7[29:0],PINS_GPIO_MODE_6[29:0],PINS_GPIO_MODE_5[29:0],PINS_GPIO_MODE_4[29:0],PINS_GPIO_MODE_3[29:0],PINS_GPIO_MODE_2[29:0],PINS_GPIO_MODE_1[29:0],PINS_GPIO_MODE_0[29:0]};
   localparam UNUSED_MEM_PINS_PINLOC        = {UNUSED_MEM_PINS_PINLOC_128[29:0],UNUSED_MEM_PINS_PINLOC_127[29:0],UNUSED_MEM_PINS_PINLOC_126[29:0],UNUSED_MEM_PINS_PINLOC_125[29:0],UNUSED_MEM_PINS_PINLOC_124[29:0],UNUSED_MEM_PINS_PINLOC_123[29:0],UNUSED_MEM_PINS_PINLOC_122[29:0],UNUSED_MEM_PINS_PINLOC_121[29:0],UNUSED_MEM_PINS_PINLOC_120[29:0],UNUSED_MEM_PINS_PINLOC_119[29:0],UNUSED_MEM_PINS_PINLOC_118[29:0],UNUSED_MEM_PINS_PINLOC_117[29:0],UNUSED_MEM_PINS_PINLOC_116[29:0],UNUSED_MEM_PINS_PINLOC_115[29:0],UNUSED_MEM_PINS_PINLOC_114[29:0],UNUSED_MEM_PINS_PINLOC_113[29:0],UNUSED_MEM_PINS_PINLOC_112[29:0],UNUSED_MEM_PINS_PINLOC_111[29:0],UNUSED_MEM_PINS_PINLOC_110[29:0],UNUSED_MEM_PINS_PINLOC_109[29:0],UNUSED_MEM_PINS_PINLOC_108[29:0],UNUSED_MEM_PINS_PINLOC_107[29:0],UNUSED_MEM_PINS_PINLOC_106[29:0],UNUSED_MEM_PINS_PINLOC_105[29:0],UNUSED_MEM_PINS_PINLOC_104[29:0],UNUSED_MEM_PINS_PINLOC_103[29:0],UNUSED_MEM_PINS_PINLOC_102[29:0],UNUSED_MEM_PINS_PINLOC_101[29:0],UNUSED_MEM_PINS_PINLOC_100[29:0],UNUSED_MEM_PINS_PINLOC_99[29:0],UNUSED_MEM_PINS_PINLOC_98[29:0],UNUSED_MEM_PINS_PINLOC_97[29:0],UNUSED_MEM_PINS_PINLOC_96[29:0],UNUSED_MEM_PINS_PINLOC_95[29:0],UNUSED_MEM_PINS_PINLOC_94[29:0],UNUSED_MEM_PINS_PINLOC_93[29:0],UNUSED_MEM_PINS_PINLOC_92[29:0],UNUSED_MEM_PINS_PINLOC_91[29:0],UNUSED_MEM_PINS_PINLOC_90[29:0],UNUSED_MEM_PINS_PINLOC_89[29:0],UNUSED_MEM_PINS_PINLOC_88[29:0],UNUSED_MEM_PINS_PINLOC_87[29:0],UNUSED_MEM_PINS_PINLOC_86[29:0],UNUSED_MEM_PINS_PINLOC_85[29:0],UNUSED_MEM_PINS_PINLOC_84[29:0],UNUSED_MEM_PINS_PINLOC_83[29:0],UNUSED_MEM_PINS_PINLOC_82[29:0],UNUSED_MEM_PINS_PINLOC_81[29:0],UNUSED_MEM_PINS_PINLOC_80[29:0],UNUSED_MEM_PINS_PINLOC_79[29:0],UNUSED_MEM_PINS_PINLOC_78[29:0],UNUSED_MEM_PINS_PINLOC_77[29:0],UNUSED_MEM_PINS_PINLOC_76[29:0],UNUSED_MEM_PINS_PINLOC_75[29:0],UNUSED_MEM_PINS_PINLOC_74[29:0],UNUSED_MEM_PINS_PINLOC_73[29:0],UNUSED_MEM_PINS_PINLOC_72[29:0],UNUSED_MEM_PINS_PINLOC_71[29:0],UNUSED_MEM_PINS_PINLOC_70[29:0],UNUSED_MEM_PINS_PINLOC_69[29:0],UNUSED_MEM_PINS_PINLOC_68[29:0],UNUSED_MEM_PINS_PINLOC_67[29:0],UNUSED_MEM_PINS_PINLOC_66[29:0],UNUSED_MEM_PINS_PINLOC_65[29:0],UNUSED_MEM_PINS_PINLOC_64[29:0],UNUSED_MEM_PINS_PINLOC_63[29:0],UNUSED_MEM_PINS_PINLOC_62[29:0],UNUSED_MEM_PINS_PINLOC_61[29:0],UNUSED_MEM_PINS_PINLOC_60[29:0],UNUSED_MEM_PINS_PINLOC_59[29:0],UNUSED_MEM_PINS_PINLOC_58[29:0],UNUSED_MEM_PINS_PINLOC_57[29:0],UNUSED_MEM_PINS_PINLOC_56[29:0],UNUSED_MEM_PINS_PINLOC_55[29:0],UNUSED_MEM_PINS_PINLOC_54[29:0],UNUSED_MEM_PINS_PINLOC_53[29:0],UNUSED_MEM_PINS_PINLOC_52[29:0],UNUSED_MEM_PINS_PINLOC_51[29:0],UNUSED_MEM_PINS_PINLOC_50[29:0],UNUSED_MEM_PINS_PINLOC_49[29:0],UNUSED_MEM_PINS_PINLOC_48[29:0],UNUSED_MEM_PINS_PINLOC_47[29:0],UNUSED_MEM_PINS_PINLOC_46[29:0],UNUSED_MEM_PINS_PINLOC_45[29:0],UNUSED_MEM_PINS_PINLOC_44[29:0],UNUSED_MEM_PINS_PINLOC_43[29:0],UNUSED_MEM_PINS_PINLOC_42[29:0],UNUSED_MEM_PINS_PINLOC_41[29:0],UNUSED_MEM_PINS_PINLOC_40[29:0],UNUSED_MEM_PINS_PINLOC_39[29:0],UNUSED_MEM_PINS_PINLOC_38[29:0],UNUSED_MEM_PINS_PINLOC_37[29:0],UNUSED_MEM_PINS_PINLOC_36[29:0],UNUSED_MEM_PINS_PINLOC_35[29:0],UNUSED_MEM_PINS_PINLOC_34[29:0],UNUSED_MEM_PINS_PINLOC_33[29:0],UNUSED_MEM_PINS_PINLOC_32[29:0],UNUSED_MEM_PINS_PINLOC_31[29:0],UNUSED_MEM_PINS_PINLOC_30[29:0],UNUSED_MEM_PINS_PINLOC_29[29:0],UNUSED_MEM_PINS_PINLOC_28[29:0],UNUSED_MEM_PINS_PINLOC_27[29:0],UNUSED_MEM_PINS_PINLOC_26[29:0],UNUSED_MEM_PINS_PINLOC_25[29:0],UNUSED_MEM_PINS_PINLOC_24[29:0],UNUSED_MEM_PINS_PINLOC_23[29:0],UNUSED_MEM_PINS_PINLOC_22[29:0],UNUSED_MEM_PINS_PINLOC_21[29:0],UNUSED_MEM_PINS_PINLOC_20[29:0],UNUSED_MEM_PINS_PINLOC_19[29:0],UNUSED_MEM_PINS_PINLOC_18[29:0],UNUSED_MEM_PINS_PINLOC_17[29:0],UNUSED_MEM_PINS_PINLOC_16[29:0],UNUSED_MEM_PINS_PINLOC_15[29:0],UNUSED_MEM_PINS_PINLOC_14[29:0],UNUSED_MEM_PINS_PINLOC_13[29:0],UNUSED_MEM_PINS_PINLOC_12[29:0],UNUSED_MEM_PINS_PINLOC_11[29:0],UNUSED_MEM_PINS_PINLOC_10[29:0],UNUSED_MEM_PINS_PINLOC_9[29:0],UNUSED_MEM_PINS_PINLOC_8[29:0],UNUSED_MEM_PINS_PINLOC_7[29:0],UNUSED_MEM_PINS_PINLOC_6[29:0],UNUSED_MEM_PINS_PINLOC_5[29:0],UNUSED_MEM_PINS_PINLOC_4[29:0],UNUSED_MEM_PINS_PINLOC_3[29:0],UNUSED_MEM_PINS_PINLOC_2[29:0],UNUSED_MEM_PINS_PINLOC_1[29:0],UNUSED_MEM_PINS_PINLOC_0[29:0]};
   localparam UNUSED_DQS_BUSES_LANELOC      = {UNUSED_DQS_BUSES_LANELOC_10[29:0],UNUSED_DQS_BUSES_LANELOC_9[29:0],UNUSED_DQS_BUSES_LANELOC_8[29:0],UNUSED_DQS_BUSES_LANELOC_7[29:0],UNUSED_DQS_BUSES_LANELOC_6[29:0],UNUSED_DQS_BUSES_LANELOC_5[29:0],UNUSED_DQS_BUSES_LANELOC_4[29:0],UNUSED_DQS_BUSES_LANELOC_3[29:0],UNUSED_DQS_BUSES_LANELOC_2[29:0],UNUSED_DQS_BUSES_LANELOC_1[29:0],UNUSED_DQS_BUSES_LANELOC_0[29:0]};
   localparam CENTER_TIDS                   = {CENTER_TIDS_2[29:0],CENTER_TIDS_1[29:0],CENTER_TIDS_0[29:0]};
   localparam HMC_TIDS                      = {HMC_TIDS_2[29:0],HMC_TIDS_1[29:0],HMC_TIDS_0[29:0]};
   localparam LANE_TIDS                     = {LANE_TIDS_9[29:0],LANE_TIDS_8[29:0],LANE_TIDS_7[29:0],LANE_TIDS_6[29:0],LANE_TIDS_5[29:0],LANE_TIDS_4[29:0],LANE_TIDS_3[29:0],LANE_TIDS_2[29:0],LANE_TIDS_1[29:0],LANE_TIDS_0[29:0]};
   localparam PORT_MEM_CK_PINLOC            = {PORT_MEM_CK_PINLOC_5[29:0],PORT_MEM_CK_PINLOC_4[29:0],PORT_MEM_CK_PINLOC_3[29:0],PORT_MEM_CK_PINLOC_2[29:0],PORT_MEM_CK_PINLOC_1[29:0],PORT_MEM_CK_PINLOC_0[29:0]};
   localparam PORT_MEM_CK_N_PINLOC          = {PORT_MEM_CK_N_PINLOC_5[29:0],PORT_MEM_CK_N_PINLOC_4[29:0],PORT_MEM_CK_N_PINLOC_3[29:0],PORT_MEM_CK_N_PINLOC_2[29:0],PORT_MEM_CK_N_PINLOC_1[29:0],PORT_MEM_CK_N_PINLOC_0[29:0]};
   localparam PORT_MEM_DK_PINLOC            = {PORT_MEM_DK_PINLOC_5[29:0],PORT_MEM_DK_PINLOC_4[29:0],PORT_MEM_DK_PINLOC_3[29:0],PORT_MEM_DK_PINLOC_2[29:0],PORT_MEM_DK_PINLOC_1[29:0],PORT_MEM_DK_PINLOC_0[29:0]};
   localparam PORT_MEM_DK_N_PINLOC          = {PORT_MEM_DK_N_PINLOC_5[29:0],PORT_MEM_DK_N_PINLOC_4[29:0],PORT_MEM_DK_N_PINLOC_3[29:0],PORT_MEM_DK_N_PINLOC_2[29:0],PORT_MEM_DK_N_PINLOC_1[29:0],PORT_MEM_DK_N_PINLOC_0[29:0]};
   localparam PORT_MEM_DKA_PINLOC           = {PORT_MEM_DKA_PINLOC_5[29:0],PORT_MEM_DKA_PINLOC_4[29:0],PORT_MEM_DKA_PINLOC_3[29:0],PORT_MEM_DKA_PINLOC_2[29:0],PORT_MEM_DKA_PINLOC_1[29:0],PORT_MEM_DKA_PINLOC_0[29:0]};
   localparam PORT_MEM_DKA_N_PINLOC         = {PORT_MEM_DKA_N_PINLOC_5[29:0],PORT_MEM_DKA_N_PINLOC_4[29:0],PORT_MEM_DKA_N_PINLOC_3[29:0],PORT_MEM_DKA_N_PINLOC_2[29:0],PORT_MEM_DKA_N_PINLOC_1[29:0],PORT_MEM_DKA_N_PINLOC_0[29:0]};
   localparam PORT_MEM_DKB_PINLOC           = {PORT_MEM_DKB_PINLOC_5[29:0],PORT_MEM_DKB_PINLOC_4[29:0],PORT_MEM_DKB_PINLOC_3[29:0],PORT_MEM_DKB_PINLOC_2[29:0],PORT_MEM_DKB_PINLOC_1[29:0],PORT_MEM_DKB_PINLOC_0[29:0]};
   localparam PORT_MEM_DKB_N_PINLOC         = {PORT_MEM_DKB_N_PINLOC_5[29:0],PORT_MEM_DKB_N_PINLOC_4[29:0],PORT_MEM_DKB_N_PINLOC_3[29:0],PORT_MEM_DKB_N_PINLOC_2[29:0],PORT_MEM_DKB_N_PINLOC_1[29:0],PORT_MEM_DKB_N_PINLOC_0[29:0]};
   localparam PORT_MEM_K_PINLOC             = {PORT_MEM_K_PINLOC_5[29:0],PORT_MEM_K_PINLOC_4[29:0],PORT_MEM_K_PINLOC_3[29:0],PORT_MEM_K_PINLOC_2[29:0],PORT_MEM_K_PINLOC_1[29:0],PORT_MEM_K_PINLOC_0[29:0]};
   localparam PORT_MEM_K_N_PINLOC           = {PORT_MEM_K_N_PINLOC_5[29:0],PORT_MEM_K_N_PINLOC_4[29:0],PORT_MEM_K_N_PINLOC_3[29:0],PORT_MEM_K_N_PINLOC_2[29:0],PORT_MEM_K_N_PINLOC_1[29:0],PORT_MEM_K_N_PINLOC_0[29:0]};
   localparam PORT_MEM_A_PINLOC             = {PORT_MEM_A_PINLOC_16[29:0],PORT_MEM_A_PINLOC_15[29:0],PORT_MEM_A_PINLOC_14[29:0],PORT_MEM_A_PINLOC_13[29:0],PORT_MEM_A_PINLOC_12[29:0],PORT_MEM_A_PINLOC_11[29:0],PORT_MEM_A_PINLOC_10[29:0],PORT_MEM_A_PINLOC_9[29:0],PORT_MEM_A_PINLOC_8[29:0],PORT_MEM_A_PINLOC_7[29:0],PORT_MEM_A_PINLOC_6[29:0],PORT_MEM_A_PINLOC_5[29:0],PORT_MEM_A_PINLOC_4[29:0],PORT_MEM_A_PINLOC_3[29:0],PORT_MEM_A_PINLOC_2[29:0],PORT_MEM_A_PINLOC_1[29:0],PORT_MEM_A_PINLOC_0[29:0]};
   localparam PORT_MEM_BA_PINLOC            = {PORT_MEM_BA_PINLOC_5[29:0],PORT_MEM_BA_PINLOC_4[29:0],PORT_MEM_BA_PINLOC_3[29:0],PORT_MEM_BA_PINLOC_2[29:0],PORT_MEM_BA_PINLOC_1[29:0],PORT_MEM_BA_PINLOC_0[29:0]};
   localparam PORT_MEM_BG_PINLOC            = {PORT_MEM_BG_PINLOC_5[29:0],PORT_MEM_BG_PINLOC_4[29:0],PORT_MEM_BG_PINLOC_3[29:0],PORT_MEM_BG_PINLOC_2[29:0],PORT_MEM_BG_PINLOC_1[29:0],PORT_MEM_BG_PINLOC_0[29:0]};
   localparam PORT_MEM_C_PINLOC             = {PORT_MEM_C_PINLOC_5[29:0],PORT_MEM_C_PINLOC_4[29:0],PORT_MEM_C_PINLOC_3[29:0],PORT_MEM_C_PINLOC_2[29:0],PORT_MEM_C_PINLOC_1[29:0],PORT_MEM_C_PINLOC_0[29:0]};
   localparam PORT_MEM_CKE_PINLOC           = {PORT_MEM_CKE_PINLOC_5[29:0],PORT_MEM_CKE_PINLOC_4[29:0],PORT_MEM_CKE_PINLOC_3[29:0],PORT_MEM_CKE_PINLOC_2[29:0],PORT_MEM_CKE_PINLOC_1[29:0],PORT_MEM_CKE_PINLOC_0[29:0]};
   localparam PORT_MEM_CS_N_PINLOC          = {PORT_MEM_CS_N_PINLOC_5[29:0],PORT_MEM_CS_N_PINLOC_4[29:0],PORT_MEM_CS_N_PINLOC_3[29:0],PORT_MEM_CS_N_PINLOC_2[29:0],PORT_MEM_CS_N_PINLOC_1[29:0],PORT_MEM_CS_N_PINLOC_0[29:0]};
   localparam PORT_MEM_RM_PINLOC            = {PORT_MEM_RM_PINLOC_5[29:0],PORT_MEM_RM_PINLOC_4[29:0],PORT_MEM_RM_PINLOC_3[29:0],PORT_MEM_RM_PINLOC_2[29:0],PORT_MEM_RM_PINLOC_1[29:0],PORT_MEM_RM_PINLOC_0[29:0]};
   localparam PORT_MEM_ODT_PINLOC           = {PORT_MEM_ODT_PINLOC_5[29:0],PORT_MEM_ODT_PINLOC_4[29:0],PORT_MEM_ODT_PINLOC_3[29:0],PORT_MEM_ODT_PINLOC_2[29:0],PORT_MEM_ODT_PINLOC_1[29:0],PORT_MEM_ODT_PINLOC_0[29:0]};
   localparam PORT_MEM_RAS_N_PINLOC         = {PORT_MEM_RAS_N_PINLOC_1[29:0],PORT_MEM_RAS_N_PINLOC_0[29:0]};
   localparam PORT_MEM_CAS_N_PINLOC         = {PORT_MEM_CAS_N_PINLOC_1[29:0],PORT_MEM_CAS_N_PINLOC_0[29:0]};
   localparam PORT_MEM_WE_N_PINLOC          = {PORT_MEM_WE_N_PINLOC_1[29:0],PORT_MEM_WE_N_PINLOC_0[29:0]};
   localparam PORT_MEM_RESET_N_PINLOC       = {PORT_MEM_RESET_N_PINLOC_1[29:0],PORT_MEM_RESET_N_PINLOC_0[29:0]};
   localparam PORT_MEM_ACT_N_PINLOC         = {PORT_MEM_ACT_N_PINLOC_1[29:0],PORT_MEM_ACT_N_PINLOC_0[29:0]};
   localparam PORT_MEM_PAR_PINLOC           = {PORT_MEM_PAR_PINLOC_1[29:0],PORT_MEM_PAR_PINLOC_0[29:0]};
   localparam PORT_MEM_CA_PINLOC            = {PORT_MEM_CA_PINLOC_16[29:0],PORT_MEM_CA_PINLOC_15[29:0],PORT_MEM_CA_PINLOC_14[29:0],PORT_MEM_CA_PINLOC_13[29:0],PORT_MEM_CA_PINLOC_12[29:0],PORT_MEM_CA_PINLOC_11[29:0],PORT_MEM_CA_PINLOC_10[29:0],PORT_MEM_CA_PINLOC_9[29:0],PORT_MEM_CA_PINLOC_8[29:0],PORT_MEM_CA_PINLOC_7[29:0],PORT_MEM_CA_PINLOC_6[29:0],PORT_MEM_CA_PINLOC_5[29:0],PORT_MEM_CA_PINLOC_4[29:0],PORT_MEM_CA_PINLOC_3[29:0],PORT_MEM_CA_PINLOC_2[29:0],PORT_MEM_CA_PINLOC_1[29:0],PORT_MEM_CA_PINLOC_0[29:0]};
   localparam PORT_MEM_REF_N_PINLOC         = {PORT_MEM_REF_N_PINLOC_0[29:0]};
   localparam PORT_MEM_WPS_N_PINLOC         = {PORT_MEM_WPS_N_PINLOC_0[29:0]};
   localparam PORT_MEM_RPS_N_PINLOC         = {PORT_MEM_RPS_N_PINLOC_0[29:0]};
   localparam PORT_MEM_DOFF_N_PINLOC        = {PORT_MEM_DOFF_N_PINLOC_0[29:0]};
   localparam PORT_MEM_LDA_N_PINLOC         = {PORT_MEM_LDA_N_PINLOC_0[29:0]};
   localparam PORT_MEM_LDB_N_PINLOC         = {PORT_MEM_LDB_N_PINLOC_0[29:0]};
   localparam PORT_MEM_RWA_N_PINLOC         = {PORT_MEM_RWA_N_PINLOC_0[29:0]};
   localparam PORT_MEM_RWB_N_PINLOC         = {PORT_MEM_RWB_N_PINLOC_0[29:0]};
   localparam PORT_MEM_LBK0_N_PINLOC        = {PORT_MEM_LBK0_N_PINLOC_0[29:0]};
   localparam PORT_MEM_LBK1_N_PINLOC        = {PORT_MEM_LBK1_N_PINLOC_0[29:0]};
   localparam PORT_MEM_CFG_N_PINLOC         = {PORT_MEM_CFG_N_PINLOC_0[29:0]};
   localparam PORT_MEM_AP_PINLOC            = {PORT_MEM_AP_PINLOC_0[29:0]};
   localparam PORT_MEM_AINV_PINLOC          = {PORT_MEM_AINV_PINLOC_0[29:0]};
   localparam PORT_MEM_DM_PINLOC            = {PORT_MEM_DM_PINLOC_12[29:0],PORT_MEM_DM_PINLOC_11[29:0],PORT_MEM_DM_PINLOC_10[29:0],PORT_MEM_DM_PINLOC_9[29:0],PORT_MEM_DM_PINLOC_8[29:0],PORT_MEM_DM_PINLOC_7[29:0],PORT_MEM_DM_PINLOC_6[29:0],PORT_MEM_DM_PINLOC_5[29:0],PORT_MEM_DM_PINLOC_4[29:0],PORT_MEM_DM_PINLOC_3[29:0],PORT_MEM_DM_PINLOC_2[29:0],PORT_MEM_DM_PINLOC_1[29:0],PORT_MEM_DM_PINLOC_0[29:0]};
   localparam PORT_MEM_BWS_N_PINLOC         = {PORT_MEM_BWS_N_PINLOC_2[29:0],PORT_MEM_BWS_N_PINLOC_1[29:0],PORT_MEM_BWS_N_PINLOC_0[29:0]};
   localparam PORT_MEM_D_PINLOC             = {PORT_MEM_D_PINLOC_48[29:0],PORT_MEM_D_PINLOC_47[29:0],PORT_MEM_D_PINLOC_46[29:0],PORT_MEM_D_PINLOC_45[29:0],PORT_MEM_D_PINLOC_44[29:0],PORT_MEM_D_PINLOC_43[29:0],PORT_MEM_D_PINLOC_42[29:0],PORT_MEM_D_PINLOC_41[29:0],PORT_MEM_D_PINLOC_40[29:0],PORT_MEM_D_PINLOC_39[29:0],PORT_MEM_D_PINLOC_38[29:0],PORT_MEM_D_PINLOC_37[29:0],PORT_MEM_D_PINLOC_36[29:0],PORT_MEM_D_PINLOC_35[29:0],PORT_MEM_D_PINLOC_34[29:0],PORT_MEM_D_PINLOC_33[29:0],PORT_MEM_D_PINLOC_32[29:0],PORT_MEM_D_PINLOC_31[29:0],PORT_MEM_D_PINLOC_30[29:0],PORT_MEM_D_PINLOC_29[29:0],PORT_MEM_D_PINLOC_28[29:0],PORT_MEM_D_PINLOC_27[29:0],PORT_MEM_D_PINLOC_26[29:0],PORT_MEM_D_PINLOC_25[29:0],PORT_MEM_D_PINLOC_24[29:0],PORT_MEM_D_PINLOC_23[29:0],PORT_MEM_D_PINLOC_22[29:0],PORT_MEM_D_PINLOC_21[29:0],PORT_MEM_D_PINLOC_20[29:0],PORT_MEM_D_PINLOC_19[29:0],PORT_MEM_D_PINLOC_18[29:0],PORT_MEM_D_PINLOC_17[29:0],PORT_MEM_D_PINLOC_16[29:0],PORT_MEM_D_PINLOC_15[29:0],PORT_MEM_D_PINLOC_14[29:0],PORT_MEM_D_PINLOC_13[29:0],PORT_MEM_D_PINLOC_12[29:0],PORT_MEM_D_PINLOC_11[29:0],PORT_MEM_D_PINLOC_10[29:0],PORT_MEM_D_PINLOC_9[29:0],PORT_MEM_D_PINLOC_8[29:0],PORT_MEM_D_PINLOC_7[29:0],PORT_MEM_D_PINLOC_6[29:0],PORT_MEM_D_PINLOC_5[29:0],PORT_MEM_D_PINLOC_4[29:0],PORT_MEM_D_PINLOC_3[29:0],PORT_MEM_D_PINLOC_2[29:0],PORT_MEM_D_PINLOC_1[29:0],PORT_MEM_D_PINLOC_0[29:0]};
   localparam PORT_MEM_DQ_PINLOC            = {PORT_MEM_DQ_PINLOC_48[29:0],PORT_MEM_DQ_PINLOC_47[29:0],PORT_MEM_DQ_PINLOC_46[29:0],PORT_MEM_DQ_PINLOC_45[29:0],PORT_MEM_DQ_PINLOC_44[29:0],PORT_MEM_DQ_PINLOC_43[29:0],PORT_MEM_DQ_PINLOC_42[29:0],PORT_MEM_DQ_PINLOC_41[29:0],PORT_MEM_DQ_PINLOC_40[29:0],PORT_MEM_DQ_PINLOC_39[29:0],PORT_MEM_DQ_PINLOC_38[29:0],PORT_MEM_DQ_PINLOC_37[29:0],PORT_MEM_DQ_PINLOC_36[29:0],PORT_MEM_DQ_PINLOC_35[29:0],PORT_MEM_DQ_PINLOC_34[29:0],PORT_MEM_DQ_PINLOC_33[29:0],PORT_MEM_DQ_PINLOC_32[29:0],PORT_MEM_DQ_PINLOC_31[29:0],PORT_MEM_DQ_PINLOC_30[29:0],PORT_MEM_DQ_PINLOC_29[29:0],PORT_MEM_DQ_PINLOC_28[29:0],PORT_MEM_DQ_PINLOC_27[29:0],PORT_MEM_DQ_PINLOC_26[29:0],PORT_MEM_DQ_PINLOC_25[29:0],PORT_MEM_DQ_PINLOC_24[29:0],PORT_MEM_DQ_PINLOC_23[29:0],PORT_MEM_DQ_PINLOC_22[29:0],PORT_MEM_DQ_PINLOC_21[29:0],PORT_MEM_DQ_PINLOC_20[29:0],PORT_MEM_DQ_PINLOC_19[29:0],PORT_MEM_DQ_PINLOC_18[29:0],PORT_MEM_DQ_PINLOC_17[29:0],PORT_MEM_DQ_PINLOC_16[29:0],PORT_MEM_DQ_PINLOC_15[29:0],PORT_MEM_DQ_PINLOC_14[29:0],PORT_MEM_DQ_PINLOC_13[29:0],PORT_MEM_DQ_PINLOC_12[29:0],PORT_MEM_DQ_PINLOC_11[29:0],PORT_MEM_DQ_PINLOC_10[29:0],PORT_MEM_DQ_PINLOC_9[29:0],PORT_MEM_DQ_PINLOC_8[29:0],PORT_MEM_DQ_PINLOC_7[29:0],PORT_MEM_DQ_PINLOC_6[29:0],PORT_MEM_DQ_PINLOC_5[29:0],PORT_MEM_DQ_PINLOC_4[29:0],PORT_MEM_DQ_PINLOC_3[29:0],PORT_MEM_DQ_PINLOC_2[29:0],PORT_MEM_DQ_PINLOC_1[29:0],PORT_MEM_DQ_PINLOC_0[29:0]};
   localparam PORT_MEM_DBI_N_PINLOC         = {PORT_MEM_DBI_N_PINLOC_6[29:0],PORT_MEM_DBI_N_PINLOC_5[29:0],PORT_MEM_DBI_N_PINLOC_4[29:0],PORT_MEM_DBI_N_PINLOC_3[29:0],PORT_MEM_DBI_N_PINLOC_2[29:0],PORT_MEM_DBI_N_PINLOC_1[29:0],PORT_MEM_DBI_N_PINLOC_0[29:0]};
   localparam PORT_MEM_DQA_PINLOC           = {PORT_MEM_DQA_PINLOC_48[29:0],PORT_MEM_DQA_PINLOC_47[29:0],PORT_MEM_DQA_PINLOC_46[29:0],PORT_MEM_DQA_PINLOC_45[29:0],PORT_MEM_DQA_PINLOC_44[29:0],PORT_MEM_DQA_PINLOC_43[29:0],PORT_MEM_DQA_PINLOC_42[29:0],PORT_MEM_DQA_PINLOC_41[29:0],PORT_MEM_DQA_PINLOC_40[29:0],PORT_MEM_DQA_PINLOC_39[29:0],PORT_MEM_DQA_PINLOC_38[29:0],PORT_MEM_DQA_PINLOC_37[29:0],PORT_MEM_DQA_PINLOC_36[29:0],PORT_MEM_DQA_PINLOC_35[29:0],PORT_MEM_DQA_PINLOC_34[29:0],PORT_MEM_DQA_PINLOC_33[29:0],PORT_MEM_DQA_PINLOC_32[29:0],PORT_MEM_DQA_PINLOC_31[29:0],PORT_MEM_DQA_PINLOC_30[29:0],PORT_MEM_DQA_PINLOC_29[29:0],PORT_MEM_DQA_PINLOC_28[29:0],PORT_MEM_DQA_PINLOC_27[29:0],PORT_MEM_DQA_PINLOC_26[29:0],PORT_MEM_DQA_PINLOC_25[29:0],PORT_MEM_DQA_PINLOC_24[29:0],PORT_MEM_DQA_PINLOC_23[29:0],PORT_MEM_DQA_PINLOC_22[29:0],PORT_MEM_DQA_PINLOC_21[29:0],PORT_MEM_DQA_PINLOC_20[29:0],PORT_MEM_DQA_PINLOC_19[29:0],PORT_MEM_DQA_PINLOC_18[29:0],PORT_MEM_DQA_PINLOC_17[29:0],PORT_MEM_DQA_PINLOC_16[29:0],PORT_MEM_DQA_PINLOC_15[29:0],PORT_MEM_DQA_PINLOC_14[29:0],PORT_MEM_DQA_PINLOC_13[29:0],PORT_MEM_DQA_PINLOC_12[29:0],PORT_MEM_DQA_PINLOC_11[29:0],PORT_MEM_DQA_PINLOC_10[29:0],PORT_MEM_DQA_PINLOC_9[29:0],PORT_MEM_DQA_PINLOC_8[29:0],PORT_MEM_DQA_PINLOC_7[29:0],PORT_MEM_DQA_PINLOC_6[29:0],PORT_MEM_DQA_PINLOC_5[29:0],PORT_MEM_DQA_PINLOC_4[29:0],PORT_MEM_DQA_PINLOC_3[29:0],PORT_MEM_DQA_PINLOC_2[29:0],PORT_MEM_DQA_PINLOC_1[29:0],PORT_MEM_DQA_PINLOC_0[29:0]};
   localparam PORT_MEM_DQB_PINLOC           = {PORT_MEM_DQB_PINLOC_48[29:0],PORT_MEM_DQB_PINLOC_47[29:0],PORT_MEM_DQB_PINLOC_46[29:0],PORT_MEM_DQB_PINLOC_45[29:0],PORT_MEM_DQB_PINLOC_44[29:0],PORT_MEM_DQB_PINLOC_43[29:0],PORT_MEM_DQB_PINLOC_42[29:0],PORT_MEM_DQB_PINLOC_41[29:0],PORT_MEM_DQB_PINLOC_40[29:0],PORT_MEM_DQB_PINLOC_39[29:0],PORT_MEM_DQB_PINLOC_38[29:0],PORT_MEM_DQB_PINLOC_37[29:0],PORT_MEM_DQB_PINLOC_36[29:0],PORT_MEM_DQB_PINLOC_35[29:0],PORT_MEM_DQB_PINLOC_34[29:0],PORT_MEM_DQB_PINLOC_33[29:0],PORT_MEM_DQB_PINLOC_32[29:0],PORT_MEM_DQB_PINLOC_31[29:0],PORT_MEM_DQB_PINLOC_30[29:0],PORT_MEM_DQB_PINLOC_29[29:0],PORT_MEM_DQB_PINLOC_28[29:0],PORT_MEM_DQB_PINLOC_27[29:0],PORT_MEM_DQB_PINLOC_26[29:0],PORT_MEM_DQB_PINLOC_25[29:0],PORT_MEM_DQB_PINLOC_24[29:0],PORT_MEM_DQB_PINLOC_23[29:0],PORT_MEM_DQB_PINLOC_22[29:0],PORT_MEM_DQB_PINLOC_21[29:0],PORT_MEM_DQB_PINLOC_20[29:0],PORT_MEM_DQB_PINLOC_19[29:0],PORT_MEM_DQB_PINLOC_18[29:0],PORT_MEM_DQB_PINLOC_17[29:0],PORT_MEM_DQB_PINLOC_16[29:0],PORT_MEM_DQB_PINLOC_15[29:0],PORT_MEM_DQB_PINLOC_14[29:0],PORT_MEM_DQB_PINLOC_13[29:0],PORT_MEM_DQB_PINLOC_12[29:0],PORT_MEM_DQB_PINLOC_11[29:0],PORT_MEM_DQB_PINLOC_10[29:0],PORT_MEM_DQB_PINLOC_9[29:0],PORT_MEM_DQB_PINLOC_8[29:0],PORT_MEM_DQB_PINLOC_7[29:0],PORT_MEM_DQB_PINLOC_6[29:0],PORT_MEM_DQB_PINLOC_5[29:0],PORT_MEM_DQB_PINLOC_4[29:0],PORT_MEM_DQB_PINLOC_3[29:0],PORT_MEM_DQB_PINLOC_2[29:0],PORT_MEM_DQB_PINLOC_1[29:0],PORT_MEM_DQB_PINLOC_0[29:0]};
   localparam PORT_MEM_DINVA_PINLOC         = {PORT_MEM_DINVA_PINLOC_2[29:0],PORT_MEM_DINVA_PINLOC_1[29:0],PORT_MEM_DINVA_PINLOC_0[29:0]};
   localparam PORT_MEM_DINVB_PINLOC         = {PORT_MEM_DINVB_PINLOC_2[29:0],PORT_MEM_DINVB_PINLOC_1[29:0],PORT_MEM_DINVB_PINLOC_0[29:0]};
   localparam PORT_MEM_Q_PINLOC             = {PORT_MEM_Q_PINLOC_48[29:0],PORT_MEM_Q_PINLOC_47[29:0],PORT_MEM_Q_PINLOC_46[29:0],PORT_MEM_Q_PINLOC_45[29:0],PORT_MEM_Q_PINLOC_44[29:0],PORT_MEM_Q_PINLOC_43[29:0],PORT_MEM_Q_PINLOC_42[29:0],PORT_MEM_Q_PINLOC_41[29:0],PORT_MEM_Q_PINLOC_40[29:0],PORT_MEM_Q_PINLOC_39[29:0],PORT_MEM_Q_PINLOC_38[29:0],PORT_MEM_Q_PINLOC_37[29:0],PORT_MEM_Q_PINLOC_36[29:0],PORT_MEM_Q_PINLOC_35[29:0],PORT_MEM_Q_PINLOC_34[29:0],PORT_MEM_Q_PINLOC_33[29:0],PORT_MEM_Q_PINLOC_32[29:0],PORT_MEM_Q_PINLOC_31[29:0],PORT_MEM_Q_PINLOC_30[29:0],PORT_MEM_Q_PINLOC_29[29:0],PORT_MEM_Q_PINLOC_28[29:0],PORT_MEM_Q_PINLOC_27[29:0],PORT_MEM_Q_PINLOC_26[29:0],PORT_MEM_Q_PINLOC_25[29:0],PORT_MEM_Q_PINLOC_24[29:0],PORT_MEM_Q_PINLOC_23[29:0],PORT_MEM_Q_PINLOC_22[29:0],PORT_MEM_Q_PINLOC_21[29:0],PORT_MEM_Q_PINLOC_20[29:0],PORT_MEM_Q_PINLOC_19[29:0],PORT_MEM_Q_PINLOC_18[29:0],PORT_MEM_Q_PINLOC_17[29:0],PORT_MEM_Q_PINLOC_16[29:0],PORT_MEM_Q_PINLOC_15[29:0],PORT_MEM_Q_PINLOC_14[29:0],PORT_MEM_Q_PINLOC_13[29:0],PORT_MEM_Q_PINLOC_12[29:0],PORT_MEM_Q_PINLOC_11[29:0],PORT_MEM_Q_PINLOC_10[29:0],PORT_MEM_Q_PINLOC_9[29:0],PORT_MEM_Q_PINLOC_8[29:0],PORT_MEM_Q_PINLOC_7[29:0],PORT_MEM_Q_PINLOC_6[29:0],PORT_MEM_Q_PINLOC_5[29:0],PORT_MEM_Q_PINLOC_4[29:0],PORT_MEM_Q_PINLOC_3[29:0],PORT_MEM_Q_PINLOC_2[29:0],PORT_MEM_Q_PINLOC_1[29:0],PORT_MEM_Q_PINLOC_0[29:0]};
   localparam PORT_MEM_DQS_PINLOC           = {PORT_MEM_DQS_PINLOC_12[29:0],PORT_MEM_DQS_PINLOC_11[29:0],PORT_MEM_DQS_PINLOC_10[29:0],PORT_MEM_DQS_PINLOC_9[29:0],PORT_MEM_DQS_PINLOC_8[29:0],PORT_MEM_DQS_PINLOC_7[29:0],PORT_MEM_DQS_PINLOC_6[29:0],PORT_MEM_DQS_PINLOC_5[29:0],PORT_MEM_DQS_PINLOC_4[29:0],PORT_MEM_DQS_PINLOC_3[29:0],PORT_MEM_DQS_PINLOC_2[29:0],PORT_MEM_DQS_PINLOC_1[29:0],PORT_MEM_DQS_PINLOC_0[29:0]};
   localparam PORT_MEM_DQS_N_PINLOC         = {PORT_MEM_DQS_N_PINLOC_12[29:0],PORT_MEM_DQS_N_PINLOC_11[29:0],PORT_MEM_DQS_N_PINLOC_10[29:0],PORT_MEM_DQS_N_PINLOC_9[29:0],PORT_MEM_DQS_N_PINLOC_8[29:0],PORT_MEM_DQS_N_PINLOC_7[29:0],PORT_MEM_DQS_N_PINLOC_6[29:0],PORT_MEM_DQS_N_PINLOC_5[29:0],PORT_MEM_DQS_N_PINLOC_4[29:0],PORT_MEM_DQS_N_PINLOC_3[29:0],PORT_MEM_DQS_N_PINLOC_2[29:0],PORT_MEM_DQS_N_PINLOC_1[29:0],PORT_MEM_DQS_N_PINLOC_0[29:0]};
   localparam PORT_MEM_QK_PINLOC            = {PORT_MEM_QK_PINLOC_5[29:0],PORT_MEM_QK_PINLOC_4[29:0],PORT_MEM_QK_PINLOC_3[29:0],PORT_MEM_QK_PINLOC_2[29:0],PORT_MEM_QK_PINLOC_1[29:0],PORT_MEM_QK_PINLOC_0[29:0]};
   localparam PORT_MEM_QK_N_PINLOC          = {PORT_MEM_QK_N_PINLOC_5[29:0],PORT_MEM_QK_N_PINLOC_4[29:0],PORT_MEM_QK_N_PINLOC_3[29:0],PORT_MEM_QK_N_PINLOC_2[29:0],PORT_MEM_QK_N_PINLOC_1[29:0],PORT_MEM_QK_N_PINLOC_0[29:0]};
   localparam PORT_MEM_QKA_PINLOC           = {PORT_MEM_QKA_PINLOC_5[29:0],PORT_MEM_QKA_PINLOC_4[29:0],PORT_MEM_QKA_PINLOC_3[29:0],PORT_MEM_QKA_PINLOC_2[29:0],PORT_MEM_QKA_PINLOC_1[29:0],PORT_MEM_QKA_PINLOC_0[29:0]};
   localparam PORT_MEM_QKA_N_PINLOC         = {PORT_MEM_QKA_N_PINLOC_5[29:0],PORT_MEM_QKA_N_PINLOC_4[29:0],PORT_MEM_QKA_N_PINLOC_3[29:0],PORT_MEM_QKA_N_PINLOC_2[29:0],PORT_MEM_QKA_N_PINLOC_1[29:0],PORT_MEM_QKA_N_PINLOC_0[29:0]};
   localparam PORT_MEM_QKB_PINLOC           = {PORT_MEM_QKB_PINLOC_5[29:0],PORT_MEM_QKB_PINLOC_4[29:0],PORT_MEM_QKB_PINLOC_3[29:0],PORT_MEM_QKB_PINLOC_2[29:0],PORT_MEM_QKB_PINLOC_1[29:0],PORT_MEM_QKB_PINLOC_0[29:0]};
   localparam PORT_MEM_QKB_N_PINLOC         = {PORT_MEM_QKB_N_PINLOC_5[29:0],PORT_MEM_QKB_N_PINLOC_4[29:0],PORT_MEM_QKB_N_PINLOC_3[29:0],PORT_MEM_QKB_N_PINLOC_2[29:0],PORT_MEM_QKB_N_PINLOC_1[29:0],PORT_MEM_QKB_N_PINLOC_0[29:0]};
   localparam PORT_MEM_CQ_PINLOC            = {PORT_MEM_CQ_PINLOC_1[29:0],PORT_MEM_CQ_PINLOC_0[29:0]};
   localparam PORT_MEM_CQ_N_PINLOC          = {PORT_MEM_CQ_N_PINLOC_1[29:0],PORT_MEM_CQ_N_PINLOC_0[29:0]};
   localparam PORT_MEM_ALERT_N_PINLOC       = {PORT_MEM_ALERT_N_PINLOC_1[29:0],PORT_MEM_ALERT_N_PINLOC_0[29:0]};
   localparam PORT_MEM_PE_N_PINLOC          = {PORT_MEM_PE_N_PINLOC_1[29:0],PORT_MEM_PE_N_PINLOC_0[29:0]};

   localparam LANES_IN_RTL_TILES            = NUM_OF_RTL_TILES * LANES_PER_TILE;
   localparam PINS_IN_RTL_TILES             = NUM_OF_RTL_TILES * LANES_PER_TILE * PINS_PER_LANE;

   // Select which DBC to use as shadow for the primary HMC.
   // We always pick "dbc1_to_local" as it's guaranteed to be used by the interface (as an A/C lane).
   // The exception is for HPS mode - HPS is only connected to lane 3 of the HMC tile for the
   // various Avalon control signals, therefore we must denote lane 3 as shadow.
   localparam PRI_HMC_DBC_SHADOW_LANE_INDEX = IS_HPS ? 3 : 1;

   // The actual reset signal, selected from either the local signal or from master
   logic                                                                                    global_reset_n_int;

   // The actual PLL ref clock signal, selected from either the local signal or from master
   logic                                                                                    pll_ref_clk_int;

   // Reset Signals
   logic                                                                                    phy_reset_n;           // Reset signal from tile that is completely asynchronous

   // Signals for various clocks
   logic                                                                                    pll_dll_clk;           // PLL -> DLL output clock
   logic [7:0]                                                                              phy_clk_phs;           // FR PHY clock signals (8 phases, 45-deg apart)
   logic [1:0]                                                                              phy_clk;               // {phy_clk[1], phy_clk[0]}
   logic                                                                                    phy_fb_clk_to_tile;    // PHY feedback clock (to tile)
   logic                                                                                    phy_fb_clk_to_pll;     // PHY feedback clock (to PLL)
   logic [8:0]                                                                              pll_c_counters;        // PLL C counter outputs
   logic                                                                                    pll_extra_clk_diag_ok; // Internal test signal for PLL extra clocks

   // Core clock signals from/to the Clock Phase Alignment (CPA) block
   logic [1:0]                                                                              core_clks_from_cpa_pri;
   logic [1:0]                                                                              core_clks_locked_cpa_pri;
   logic [1:0]                                                                              core_clks_fb_to_cpa_pri;
   logic [1:0]                                                                              core_clks_from_cpa_sec;
   logic [1:0]                                                                              core_clks_locked_cpa_sec;
   logic [1:0]                                                                              core_clks_fb_to_cpa_sec;
   logic                                                                                    dcc_stable;

   // Avalon interfaces between core and HMC
   logic [59:0]                                                                             core2ctl_avl_0;
   logic [59:0]                                                                             core2ctl_avl_1;
   logic                                                                                    core2ctl_avl_rd_data_ready_0;
   logic                                                                                    core2ctl_avl_rd_data_ready_1;
   logic                                                                                    ctl2core_avl_cmd_ready_0;
   logic                                                                                    ctl2core_avl_cmd_ready_1;
   logic [12:0]                                                                             ctl2core_avl_rdata_id_0;
   logic [12:0]                                                                             ctl2core_avl_rdata_id_1;
   logic                                                                                    core2l_wr_data_vld_ast_0;
   logic                                                                                    core2l_wr_data_vld_ast_1;
   logic                                                                                    core2l_rd_data_rdy_ast_0;
   logic                                                                                    core2l_rd_data_rdy_ast_1;

   // Avalon interfaces between core and lanes
   logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0]                                         l2core_rd_data_vld_avl0;
   logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0]                                         l2core_wr_data_rdy_ast;

   // ECC signals between core and lanes
   logic [12:0]                                                                             core2l_wr_ecc_info_0;
   logic [12:0]                                                                             core2l_wr_ecc_info_1;
   logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][11:0]                                   l2core_wb_pointer_for_ecc;

   // Signals between core and data lanes
   logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][PINS_PER_LANE * 8 - 1:0]                core2l_data;
   logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][PINS_PER_LANE * 8 - 1:0]                l2core_data;
   logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][PINS_PER_LANE * 4 - 1:0]                core2l_oe;
   logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][3:0]                                    core2l_rdata_en_full;
   logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][15:0]                                   core2l_mrnk_read;
   logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][15:0]                                   core2l_mrnk_write;
   logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][3:0]                                    l2core_rdata_valid;
   logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][5:0]                                    l2core_afi_rlat;
   logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][5:0]                                    l2core_afi_wlat;

   // Wires for wire-luts
   logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][PINS_PER_LANE * 8 - 1:0]                wl1_l2core_data;
   logic [NUM_OF_RTL_TILES-1:0][LANES_PER_TILE-1:0][PINS_PER_LANE * 8 - 1:0]                wl2_l2core_data;

   // AFI signals between tile and core
   logic [16:0]                                                                             c2t_afi;
   logic [25:0]                                                                             t2c_afi;

   // Side-band signals between core and HMC
   logic [41:0]                                                                             core2ctl_sideband_0;
   logic [13:0]                                                                             ctl2core_sideband_0;
   logic [41:0]                                                                             core2ctl_sideband_1;
   logic [13:0]                                                                             ctl2core_sideband_1;

   // MMR signals between core and HMC
   logic [33:0]                                                                             ctl2core_mmr_0;
   logic [50:0]                                                                             core2ctl_mmr_0;
   logic [33:0]                                                                             ctl2core_mmr_1;
   logic [50:0]                                                                             core2ctl_mmr_1;

   // Signals for connecting OCT block to I/O buffers
   logic [OCT_CONTROL_WIDTH-1:0]                                                            oct_stc;         // serial-termination-control
   logic [OCT_CONTROL_WIDTH-1:0]                                                            oct_ptc;         // parallel-termination-control
   logic                                                                                    oct_cal_req;     // OCT manual calibration request
   logic                                                                                    oct_cal_rdy;     // OCT manual calibration ready
   logic                                                                                    oct_recal_req;   // OCT manual calibration request
   logic                                                                                    oct_s2pload_rdy; // OCT manual calibration load ready
   logic                                                                                    oct_s2pload_ena; // OCT manual calibration load stall

   // Signals for connecting emif signals between lanes/tiles and I/O buffers
   logic [PINS_IN_RTL_TILES-1:0]                                                            l2b_data;     // lane-to-buffer data
   logic [PINS_IN_RTL_TILES-1:0]                                                            l2b_oe;       // lane-to-buffer output-enable
   logic [PINS_IN_RTL_TILES-1:0]                                                            l2b_dtc;      // lane-to-buffer dynamic-termination-control
   logic [PINS_IN_RTL_TILES-1:0]                                                            b2l_data;     // buffer-to-lane data
   logic [LANES_IN_RTL_TILES-1:0]                                                           b2t_dqs;      // buffer-to-tile DQS
   logic [LANES_IN_RTL_TILES-1:0]                                                           b2t_dqsb;     // buffer-to-tile DQSb

   // Avalon-MM bus for the calibration commands between io_aux and tiles
   logic                                                                                    cal_bus_clk;
   logic                                                                                    cal_bus_avl_read;
   logic                                                                                    cal_bus_avl_write;
   logic [19:0]                                                                             cal_bus_avl_address;
   logic [31:0]                                                                             cal_bus_avl_read_data;
   logic [31:0]                                                                             cal_bus_avl_write_data;

   // Internal signal for cal_counter
   logic                                                                                    afi_cal_in_progress;

   assign local_cal_success = afi_cal_success & pll_extra_clk_diag_ok;
   assign local_cal_fail = afi_cal_fail;

   assign afi_mps_ack = 1'b0;

   wire runAbstractPhySim;
   wire global_reset_n_int_io_aux_in;
   wire cal_debug_reset_n_io_aux_in;
   wire cal_slave_reset_n_in_io_aux_in;

`ifdef ALTERA_EMIF_ENABLE_ISSP
   altsource_probe #(
      .sld_auto_instance_index ("YES"),
      .sld_instance_index      (0),
      .instance_id             ("CALP"),
      .probe_width             (1),
      .source_width            (0),
      .source_initial_value    ("0"),
      .enable_metastability    ("NO")
   ) cal_success (
      .probe  (local_cal_success)
   );

   altsource_probe #(
      .sld_auto_instance_index ("YES"),
      .sld_instance_index      (0),
      .instance_id             ("CALF"),
      .probe_width             (1),
      .source_width            (0),
      .source_initial_value    ("0"),
      .enable_metastability    ("NO")
   ) cal_fail (
      .probe  (local_cal_fail)
   );
`endif

   ////////////////////////////////////////////////////////////////////////////
   // PLL
   ////////////////////////////////////////////////////////////////////////////
   generate
   // synthesis translate_off
      if (DIAG_FAST_SIM) begin : gen_fast_sim
         altera_emif_arch_nf_pll_fast_sim # (
            .PLL_SIM_VCO_FREQ_PS                 (PLL_SIM_VCO_FREQ_PS),
            .PLL_SIM_PHYCLK_0_FREQ_PS            (PLL_SIM_PHYCLK_0_FREQ_PS),
            .PLL_SIM_PHYCLK_1_FREQ_PS            (PLL_SIM_PHYCLK_1_FREQ_PS),
            .PLL_SIM_PHYCLK_FB_FREQ_PS           (PLL_SIM_PHYCLK_FB_FREQ_PS),
            .PLL_SIM_PHY_CLK_VCO_PHASE_PS        (PLL_SIM_PHY_CLK_VCO_PHASE_PS),
            .PLL_SIM_CAL_SLAVE_CLK_FREQ_PS       (PLL_SIM_CAL_SLAVE_CLK_FREQ_PS),
            .PLL_SIM_CAL_MASTER_CLK_FREQ_PS      (PLL_SIM_CAL_MASTER_CLK_FREQ_PS),
            .PORT_DFT_NF_PLL_CNTSEL_WIDTH        (PORT_DFT_NF_PLL_CNTSEL_WIDTH),
            .PORT_DFT_NF_PLL_NUM_SHIFT_WIDTH     (PORT_DFT_NF_PLL_NUM_SHIFT_WIDTH)
         ) pll_inst (
            .*
         );
      end else begin : gen_normal
   // synthesis translate_on
         altera_emif_arch_nf_pll # (
            .PORT_DFT_NF_PLL_CNTSEL_WIDTH        (PORT_DFT_NF_PLL_CNTSEL_WIDTH),
            .PORT_DFT_NF_PLL_NUM_SHIFT_WIDTH     (PORT_DFT_NF_PLL_NUM_SHIFT_WIDTH),
            .PLL_REF_CLK_FREQ_PS_STR             (PLL_REF_CLK_FREQ_PS_STR),
            .PLL_VCO_FREQ_PS_STR                 (PLL_VCO_FREQ_PS_STR),
            .PLL_M_CNT_HIGH                      (PLL_M_CNT_HIGH),
            .PLL_M_CNT_LOW                       (PLL_M_CNT_LOW),
            .PLL_N_CNT_HIGH                      (PLL_N_CNT_HIGH),
            .PLL_N_CNT_LOW                       (PLL_N_CNT_LOW),
            .PLL_M_CNT_BYPASS_EN                 (PLL_M_CNT_BYPASS_EN),
            .PLL_N_CNT_BYPASS_EN                 (PLL_N_CNT_BYPASS_EN),
            .PLL_M_CNT_EVEN_DUTY_EN              (PLL_M_CNT_EVEN_DUTY_EN),
            .PLL_N_CNT_EVEN_DUTY_EN              (PLL_N_CNT_EVEN_DUTY_EN),
            .PLL_CP_SETTING                      (PLL_CP_SETTING),
            .PLL_BW_CTRL                         (PLL_BW_CTRL),
            .PLL_C_CNT_HIGH_0                    (PLL_C_CNT_HIGH_0),
            .PLL_C_CNT_LOW_0                     (PLL_C_CNT_LOW_0),
            .PLL_C_CNT_PRST_0                    (PLL_C_CNT_PRST_0),
            .PLL_C_CNT_PH_MUX_PRST_0             (PLL_C_CNT_PH_MUX_PRST_0),
            .PLL_C_CNT_BYPASS_EN_0               (PLL_C_CNT_BYPASS_EN_0),
            .PLL_C_CNT_EVEN_DUTY_EN_0            (PLL_C_CNT_EVEN_DUTY_EN_0),
            .PLL_C_CNT_HIGH_1                    (PLL_C_CNT_HIGH_1),
            .PLL_C_CNT_LOW_1                     (PLL_C_CNT_LOW_1),
            .PLL_C_CNT_PRST_1                    (PLL_C_CNT_PRST_1),
            .PLL_C_CNT_PH_MUX_PRST_1             (PLL_C_CNT_PH_MUX_PRST_1),
            .PLL_C_CNT_BYPASS_EN_1               (PLL_C_CNT_BYPASS_EN_1),
            .PLL_C_CNT_EVEN_DUTY_EN_1            (PLL_C_CNT_EVEN_DUTY_EN_1),
            .PLL_C_CNT_HIGH_2                    (PLL_C_CNT_HIGH_2),
            .PLL_C_CNT_LOW_2                     (PLL_C_CNT_LOW_2),
            .PLL_C_CNT_PRST_2                    (PLL_C_CNT_PRST_2),
            .PLL_C_CNT_PH_MUX_PRST_2             (PLL_C_CNT_PH_MUX_PRST_2),
            .PLL_C_CNT_BYPASS_EN_2               (PLL_C_CNT_BYPASS_EN_2),
            .PLL_C_CNT_EVEN_DUTY_EN_2            (PLL_C_CNT_EVEN_DUTY_EN_2),
            .PLL_C_CNT_HIGH_3                    (PLL_C_CNT_HIGH_3),
            .PLL_C_CNT_LOW_3                     (PLL_C_CNT_LOW_3),
            .PLL_C_CNT_PRST_3                    (PLL_C_CNT_PRST_3),
            .PLL_C_CNT_PH_MUX_PRST_3             (PLL_C_CNT_PH_MUX_PRST_3),
            .PLL_C_CNT_BYPASS_EN_3               (PLL_C_CNT_BYPASS_EN_3),
            .PLL_C_CNT_EVEN_DUTY_EN_3            (PLL_C_CNT_EVEN_DUTY_EN_3),
            .PLL_C_CNT_HIGH_4                    (PLL_C_CNT_HIGH_4),
            .PLL_C_CNT_LOW_4                     (PLL_C_CNT_LOW_4),
            .PLL_C_CNT_PRST_4                    (PLL_C_CNT_PRST_4),
            .PLL_C_CNT_PH_MUX_PRST_4             (PLL_C_CNT_PH_MUX_PRST_4),
            .PLL_C_CNT_BYPASS_EN_4               (PLL_C_CNT_BYPASS_EN_4),
            .PLL_C_CNT_EVEN_DUTY_EN_4            (PLL_C_CNT_EVEN_DUTY_EN_4),
            .PLL_C_CNT_HIGH_5                    (PLL_C_CNT_HIGH_5),
            .PLL_C_CNT_LOW_5                     (PLL_C_CNT_LOW_5),
            .PLL_C_CNT_PRST_5                    (PLL_C_CNT_PRST_5),
            .PLL_C_CNT_PH_MUX_PRST_5             (PLL_C_CNT_PH_MUX_PRST_5),
            .PLL_C_CNT_BYPASS_EN_5               (PLL_C_CNT_BYPASS_EN_5),
            .PLL_C_CNT_EVEN_DUTY_EN_5            (PLL_C_CNT_EVEN_DUTY_EN_5),
            .PLL_C_CNT_HIGH_6                    (PLL_C_CNT_HIGH_6),
            .PLL_C_CNT_LOW_6                     (PLL_C_CNT_LOW_6),
            .PLL_C_CNT_PRST_6                    (PLL_C_CNT_PRST_6),
            .PLL_C_CNT_PH_MUX_PRST_6             (PLL_C_CNT_PH_MUX_PRST_6),
            .PLL_C_CNT_BYPASS_EN_6               (PLL_C_CNT_BYPASS_EN_6),
            .PLL_C_CNT_EVEN_DUTY_EN_6            (PLL_C_CNT_EVEN_DUTY_EN_6),
            .PLL_C_CNT_HIGH_7                    (PLL_C_CNT_HIGH_7),
            .PLL_C_CNT_LOW_7                     (PLL_C_CNT_LOW_7),
            .PLL_C_CNT_PRST_7                    (PLL_C_CNT_PRST_7),
            .PLL_C_CNT_PH_MUX_PRST_7             (PLL_C_CNT_PH_MUX_PRST_7),
            .PLL_C_CNT_BYPASS_EN_7               (PLL_C_CNT_BYPASS_EN_7),
            .PLL_C_CNT_EVEN_DUTY_EN_7            (PLL_C_CNT_EVEN_DUTY_EN_7),
            .PLL_C_CNT_HIGH_8                    (PLL_C_CNT_HIGH_8),
            .PLL_C_CNT_LOW_8                     (PLL_C_CNT_LOW_8),
            .PLL_C_CNT_PRST_8                    (PLL_C_CNT_PRST_8),
            .PLL_C_CNT_PH_MUX_PRST_8             (PLL_C_CNT_PH_MUX_PRST_8),
            .PLL_C_CNT_BYPASS_EN_8               (PLL_C_CNT_BYPASS_EN_8),
            .PLL_C_CNT_EVEN_DUTY_EN_8            (PLL_C_CNT_EVEN_DUTY_EN_8),
            .PLL_C_CNT_FREQ_PS_STR_0             (PLL_C_CNT_FREQ_PS_STR_0),
            .PLL_C_CNT_PHASE_PS_STR_0            (PLL_C_CNT_PHASE_PS_STR_0),
            .PLL_C_CNT_DUTY_CYCLE_0              (PLL_C_CNT_DUTY_CYCLE_0),
            .PLL_C_CNT_FREQ_PS_STR_1             (PLL_C_CNT_FREQ_PS_STR_1),
            .PLL_C_CNT_PHASE_PS_STR_1            (PLL_C_CNT_PHASE_PS_STR_1),
            .PLL_C_CNT_DUTY_CYCLE_1              (PLL_C_CNT_DUTY_CYCLE_1),
            .PLL_C_CNT_FREQ_PS_STR_2             (PLL_C_CNT_FREQ_PS_STR_2),
            .PLL_C_CNT_PHASE_PS_STR_2            (PLL_C_CNT_PHASE_PS_STR_2),
            .PLL_C_CNT_DUTY_CYCLE_2              (PLL_C_CNT_DUTY_CYCLE_2),
            .PLL_C_CNT_FREQ_PS_STR_3             (PLL_C_CNT_FREQ_PS_STR_3),
            .PLL_C_CNT_PHASE_PS_STR_3            (PLL_C_CNT_PHASE_PS_STR_3),
            .PLL_C_CNT_DUTY_CYCLE_3              (PLL_C_CNT_DUTY_CYCLE_3),
            .PLL_C_CNT_FREQ_PS_STR_4             (PLL_C_CNT_FREQ_PS_STR_4),
            .PLL_C_CNT_PHASE_PS_STR_4            (PLL_C_CNT_PHASE_PS_STR_4),
            .PLL_C_CNT_DUTY_CYCLE_4              (PLL_C_CNT_DUTY_CYCLE_4),
            .PLL_C_CNT_FREQ_PS_STR_5             (PLL_C_CNT_FREQ_PS_STR_5),
            .PLL_C_CNT_PHASE_PS_STR_5            (PLL_C_CNT_PHASE_PS_STR_5),
            .PLL_C_CNT_DUTY_CYCLE_5              (PLL_C_CNT_DUTY_CYCLE_5),
            .PLL_C_CNT_FREQ_PS_STR_6             (PLL_C_CNT_FREQ_PS_STR_6),
            .PLL_C_CNT_PHASE_PS_STR_6            (PLL_C_CNT_PHASE_PS_STR_6),
            .PLL_C_CNT_DUTY_CYCLE_6              (PLL_C_CNT_DUTY_CYCLE_6),
            .PLL_C_CNT_FREQ_PS_STR_7             (PLL_C_CNT_FREQ_PS_STR_7),
            .PLL_C_CNT_PHASE_PS_STR_7            (PLL_C_CNT_PHASE_PS_STR_7),
            .PLL_C_CNT_DUTY_CYCLE_7              (PLL_C_CNT_DUTY_CYCLE_7),
            .PLL_C_CNT_FREQ_PS_STR_8             (PLL_C_CNT_FREQ_PS_STR_8),
            .PLL_C_CNT_PHASE_PS_STR_8            (PLL_C_CNT_PHASE_PS_STR_8),
            .PLL_C_CNT_DUTY_CYCLE_8              (PLL_C_CNT_DUTY_CYCLE_8),
            .PLL_C_CNT_OUT_EN_0                  (PLL_C_CNT_OUT_EN_0),
            .PLL_C_CNT_OUT_EN_1                  (PLL_C_CNT_OUT_EN_1),
            .PLL_C_CNT_OUT_EN_2                  (PLL_C_CNT_OUT_EN_2),
            .PLL_C_CNT_OUT_EN_3                  (PLL_C_CNT_OUT_EN_3),
            .PLL_C_CNT_OUT_EN_4                  (PLL_C_CNT_OUT_EN_4),
            .PLL_C_CNT_OUT_EN_5                  (PLL_C_CNT_OUT_EN_5),
            .PLL_C_CNT_OUT_EN_6                  (PLL_C_CNT_OUT_EN_6),
            .PLL_C_CNT_OUT_EN_7                  (PLL_C_CNT_OUT_EN_7),
            .PLL_C_CNT_OUT_EN_8                  (PLL_C_CNT_OUT_EN_8),
            .PLL_FBCLK_MUX_1                     (PLL_FBCLK_MUX_1),
            .PLL_FBCLK_MUX_2                     (PLL_FBCLK_MUX_2),
            .PLL_M_CNT_IN_SRC                    (PLL_M_CNT_IN_SRC),
            .PLL_BW_SEL                          (PLL_BW_SEL)
         ) pll_inst (
            .*
         );
   // synthesis translate_off
      end
   // synthesis translate_on
   endgenerate

   altera_emif_arch_nf_pll_extra_clks # (
      .PLL_NUM_OF_EXTRA_CLKS               (PLL_NUM_OF_EXTRA_CLKS),
      .DIAG_SIM_REGTEST_MODE               (DIAG_SIM_REGTEST_MODE)
   ) pll_extra_clks_inst (
      .*
   );

   ////////////////////////////////////////////////////////////////////////////
   // OCT Block
   ////////////////////////////////////////////////////////////////////////////
   altera_emif_arch_nf_oct # (
      .OCT_CONTROL_WIDTH                   (OCT_CONTROL_WIDTH),
      .PLL_REF_CLK_FREQ_PS                 (PLL_REF_CLK_FREQ_PS),
      .PHY_CALIBRATED_OCT                  (PHY_CALIBRATED_OCT),
      .PHY_USERMODE_OCT                    (PHY_USERMODE_OCT),
      .PHY_PERIODIC_OCT_RECAL              (PHY_PERIODIC_OCT_RECAL),
      .PHY_CONFIG_ENUM                     (PHY_CONFIG_ENUM),
      .IS_HPS                              (IS_HPS)
   ) oct_inst (
      .*
   );

   ////////////////////////////////////////////////////////////////////////////
   // Output clock and reset signals
   ////////////////////////////////////////////////////////////////////////////
   generate
   if (IS_HPS) begin : hps
      altera_emif_arch_nf_hps_clks_rsts # (
         .IS_VID                              (IS_VID),
         .PORT_CLKS_SHARING_MASTER_OUT_WIDTH  (PORT_CLKS_SHARING_MASTER_OUT_WIDTH),
         .PORT_CLKS_SHARING_SLAVE_IN_WIDTH    (PORT_CLKS_SHARING_SLAVE_IN_WIDTH),
         .PORT_DFT_NF_CORE_CLK_BUF_OUT_WIDTH  (PORT_DFT_NF_CORE_CLK_BUF_OUT_WIDTH),
         .PORT_DFT_NF_CORE_CLK_LOCKED_WIDTH   (PORT_DFT_NF_CORE_CLK_LOCKED_WIDTH),
         .PORT_HPS_EMIF_H2E_GP_WIDTH          (PORT_HPS_EMIF_H2E_GP_WIDTH),
         .PHY_USERMODE_OCT                    (PHY_USERMODE_OCT),
         .PHY_HPS_ENABLE_EARLY_RELEASE        (PHY_HPS_ENABLE_EARLY_RELEASE)
      ) hps_clks_rsts_inst (
         .*
      );
   end else begin : non_hps
      altera_emif_arch_nf_core_clks_rsts # (
         .PHY_CONFIG_ENUM                     (PHY_CONFIG_ENUM),
         .PHY_CORE_CLKS_SHARING_ENUM          (PHY_CORE_CLKS_SHARING_ENUM),
         .IS_VID                              (IS_VID),
         .PHY_PING_PONG_EN                    (PHY_PING_PONG_EN),
         .USER_CLK_RATIO                      (USER_CLK_RATIO),
         .C2P_P2C_CLK_RATIO                   (C2P_P2C_CLK_RATIO),
         .PORT_CLKS_SHARING_MASTER_OUT_WIDTH  (PORT_CLKS_SHARING_MASTER_OUT_WIDTH),
         .PORT_CLKS_SHARING_SLAVE_IN_WIDTH    (PORT_CLKS_SHARING_SLAVE_IN_WIDTH),
         .DIAG_CPA_OUT_1_EN                   (DIAG_CPA_OUT_1_EN),
         .DIAG_USE_CPA_LOCK                   (DIAG_USE_CPA_LOCK),
         .DIAG_SYNTH_FOR_SIM                  (DIAG_SYNTH_FOR_SIM),
         .PORT_DFT_NF_CORE_CLK_BUF_OUT_WIDTH  (PORT_DFT_NF_CORE_CLK_BUF_OUT_WIDTH),
         .PORT_DFT_NF_CORE_CLK_LOCKED_WIDTH   (PORT_DFT_NF_CORE_CLK_LOCKED_WIDTH)
      ) core_clks_rsts_inst (
         .*
      );
   end
   endgenerate

   ////////////////////////////////////////////////////////////////////////////
   // I/O Buffers
   ////////////////////////////////////////////////////////////////////////////
   altera_emif_arch_nf_bufs # (
      .PROTOCOL_ENUM                       (PROTOCOL_ENUM),
      .MEM_FORMAT_ENUM                     (MEM_FORMAT_ENUM),
      .PINS_PER_LANE                       (PINS_PER_LANE),
      .PINS_IN_RTL_TILES                   (PINS_IN_RTL_TILES),
      .LANES_IN_RTL_TILES                  (LANES_IN_RTL_TILES),
      .OCT_CONTROL_WIDTH                   (OCT_CONTROL_WIDTH),
      .DQS_BUS_MODE_ENUM                   (DQS_BUS_MODE_ENUM),
      .UNUSED_MEM_PINS_PINLOC              (UNUSED_MEM_PINS_PINLOC),
      .UNUSED_DQS_BUSES_LANELOC            (UNUSED_DQS_BUSES_LANELOC),

      // Assignment of port widths for "mem" interface
      //AUTOGEN_BEGIN: Assignment of memory port widths
      .PORT_MEM_CK_WIDTH                   (PORT_MEM_CK_WIDTH),
      .PORT_MEM_CK_N_WIDTH                 (PORT_MEM_CK_N_WIDTH),
      .PORT_MEM_DK_WIDTH                   (PORT_MEM_DK_WIDTH),
      .PORT_MEM_DK_N_WIDTH                 (PORT_MEM_DK_N_WIDTH),
      .PORT_MEM_DKA_WIDTH                  (PORT_MEM_DKA_WIDTH),
      .PORT_MEM_DKA_N_WIDTH                (PORT_MEM_DKA_N_WIDTH),
      .PORT_MEM_DKB_WIDTH                  (PORT_MEM_DKB_WIDTH),
      .PORT_MEM_DKB_N_WIDTH                (PORT_MEM_DKB_N_WIDTH),
      .PORT_MEM_K_WIDTH                    (PORT_MEM_K_WIDTH),
      .PORT_MEM_K_N_WIDTH                  (PORT_MEM_K_N_WIDTH),
      .PORT_MEM_A_WIDTH                    (PORT_MEM_A_WIDTH),
      .PORT_MEM_BA_WIDTH                   (PORT_MEM_BA_WIDTH),
      .PORT_MEM_BG_WIDTH                   (PORT_MEM_BG_WIDTH),
      .PORT_MEM_C_WIDTH                    (PORT_MEM_C_WIDTH),
      .PORT_MEM_CKE_WIDTH                  (PORT_MEM_CKE_WIDTH),
      .PORT_MEM_CS_N_WIDTH                 (PORT_MEM_CS_N_WIDTH),
      .PORT_MEM_RM_WIDTH                   (PORT_MEM_RM_WIDTH),
      .PORT_MEM_ODT_WIDTH                  (PORT_MEM_ODT_WIDTH),
      .PORT_MEM_RAS_N_WIDTH                (PORT_MEM_RAS_N_WIDTH),
      .PORT_MEM_CAS_N_WIDTH                (PORT_MEM_CAS_N_WIDTH),
      .PORT_MEM_WE_N_WIDTH                 (PORT_MEM_WE_N_WIDTH),
      .PORT_MEM_RESET_N_WIDTH              (PORT_MEM_RESET_N_WIDTH),
      .PORT_MEM_ACT_N_WIDTH                (PORT_MEM_ACT_N_WIDTH),
      .PORT_MEM_PAR_WIDTH                  (PORT_MEM_PAR_WIDTH),
      .PORT_MEM_CA_WIDTH                   (PORT_MEM_CA_WIDTH),
      .PORT_MEM_REF_N_WIDTH                (PORT_MEM_REF_N_WIDTH),
      .PORT_MEM_WPS_N_WIDTH                (PORT_MEM_WPS_N_WIDTH),
      .PORT_MEM_RPS_N_WIDTH                (PORT_MEM_RPS_N_WIDTH),
      .PORT_MEM_DOFF_N_WIDTH               (PORT_MEM_DOFF_N_WIDTH),
      .PORT_MEM_LDA_N_WIDTH                (PORT_MEM_LDA_N_WIDTH),
      .PORT_MEM_LDB_N_WIDTH                (PORT_MEM_LDB_N_WIDTH),
      .PORT_MEM_RWA_N_WIDTH                (PORT_MEM_RWA_N_WIDTH),
      .PORT_MEM_RWB_N_WIDTH                (PORT_MEM_RWB_N_WIDTH),
      .PORT_MEM_LBK0_N_WIDTH               (PORT_MEM_LBK0_N_WIDTH),
      .PORT_MEM_LBK1_N_WIDTH               (PORT_MEM_LBK1_N_WIDTH),
      .PORT_MEM_CFG_N_WIDTH                (PORT_MEM_CFG_N_WIDTH),
      .PORT_MEM_AP_WIDTH                   (PORT_MEM_AP_WIDTH),
      .PORT_MEM_AINV_WIDTH                 (PORT_MEM_AINV_WIDTH),
      .PORT_MEM_DM_WIDTH                   (PORT_MEM_DM_WIDTH),
      .PORT_MEM_BWS_N_WIDTH                (PORT_MEM_BWS_N_WIDTH),
      .PORT_MEM_D_WIDTH                    (PORT_MEM_D_WIDTH),
      .PORT_MEM_DQ_WIDTH                   (PORT_MEM_DQ_WIDTH),
      .PORT_MEM_DBI_N_WIDTH                (PORT_MEM_DBI_N_WIDTH),
      .PORT_MEM_DQA_WIDTH                  (PORT_MEM_DQA_WIDTH),
      .PORT_MEM_DQB_WIDTH                  (PORT_MEM_DQB_WIDTH),
      .PORT_MEM_DINVA_WIDTH                (PORT_MEM_DINVA_WIDTH),
      .PORT_MEM_DINVB_WIDTH                (PORT_MEM_DINVB_WIDTH),
      .PORT_MEM_Q_WIDTH                    (PORT_MEM_Q_WIDTH),
      .PORT_MEM_DQS_WIDTH                  (PORT_MEM_DQS_WIDTH),
      .PORT_MEM_DQS_N_WIDTH                (PORT_MEM_DQS_N_WIDTH),
      .PORT_MEM_QK_WIDTH                   (PORT_MEM_QK_WIDTH),
      .PORT_MEM_QK_N_WIDTH                 (PORT_MEM_QK_N_WIDTH),
      .PORT_MEM_QKA_WIDTH                  (PORT_MEM_QKA_WIDTH),
      .PORT_MEM_QKA_N_WIDTH                (PORT_MEM_QKA_N_WIDTH),
      .PORT_MEM_QKB_WIDTH                  (PORT_MEM_QKB_WIDTH),
      .PORT_MEM_QKB_N_WIDTH                (PORT_MEM_QKB_N_WIDTH),
      .PORT_MEM_CQ_WIDTH                   (PORT_MEM_CQ_WIDTH),
      .PORT_MEM_CQ_N_WIDTH                 (PORT_MEM_CQ_N_WIDTH),
      .PORT_MEM_ALERT_N_WIDTH              (PORT_MEM_ALERT_N_WIDTH),
      .PORT_MEM_PE_N_WIDTH                 (PORT_MEM_PE_N_WIDTH),

      // Assignment of parameters describing logical pin allocation
      //AUTOGEN_BEGIN: Assignment of memory port pinlocs
      .PORT_MEM_CK_PINLOC                  (PORT_MEM_CK_PINLOC),
      .PORT_MEM_CK_N_PINLOC                (PORT_MEM_CK_N_PINLOC),
      .PORT_MEM_DK_PINLOC                  (PORT_MEM_DK_PINLOC),
      .PORT_MEM_DK_N_PINLOC                (PORT_MEM_DK_N_PINLOC),
      .PORT_MEM_DKA_PINLOC                 (PORT_MEM_DKA_PINLOC),
      .PORT_MEM_DKA_N_PINLOC               (PORT_MEM_DKA_N_PINLOC),
      .PORT_MEM_DKB_PINLOC                 (PORT_MEM_DKB_PINLOC),
      .PORT_MEM_DKB_N_PINLOC               (PORT_MEM_DKB_N_PINLOC),
      .PORT_MEM_K_PINLOC                   (PORT_MEM_K_PINLOC),
      .PORT_MEM_K_N_PINLOC                 (PORT_MEM_K_N_PINLOC),
      .PORT_MEM_A_PINLOC                   (PORT_MEM_A_PINLOC),
      .PORT_MEM_BA_PINLOC                  (PORT_MEM_BA_PINLOC),
      .PORT_MEM_BG_PINLOC                  (PORT_MEM_BG_PINLOC),
      .PORT_MEM_C_PINLOC                   (PORT_MEM_C_PINLOC),
      .PORT_MEM_CKE_PINLOC                 (PORT_MEM_CKE_PINLOC),
      .PORT_MEM_CS_N_PINLOC                (PORT_MEM_CS_N_PINLOC),
      .PORT_MEM_RM_PINLOC                  (PORT_MEM_RM_PINLOC),
      .PORT_MEM_ODT_PINLOC                 (PORT_MEM_ODT_PINLOC),
      .PORT_MEM_RAS_N_PINLOC               (PORT_MEM_RAS_N_PINLOC),
      .PORT_MEM_CAS_N_PINLOC               (PORT_MEM_CAS_N_PINLOC),
      .PORT_MEM_WE_N_PINLOC                (PORT_MEM_WE_N_PINLOC),
      .PORT_MEM_RESET_N_PINLOC             (PORT_MEM_RESET_N_PINLOC),
      .PORT_MEM_ACT_N_PINLOC               (PORT_MEM_ACT_N_PINLOC),
      .PORT_MEM_PAR_PINLOC                 (PORT_MEM_PAR_PINLOC),
      .PORT_MEM_CA_PINLOC                  (PORT_MEM_CA_PINLOC),
      .PORT_MEM_REF_N_PINLOC               (PORT_MEM_REF_N_PINLOC),
      .PORT_MEM_WPS_N_PINLOC               (PORT_MEM_WPS_N_PINLOC),
      .PORT_MEM_RPS_N_PINLOC               (PORT_MEM_RPS_N_PINLOC),
      .PORT_MEM_DOFF_N_PINLOC              (PORT_MEM_DOFF_N_PINLOC),
      .PORT_MEM_LDA_N_PINLOC               (PORT_MEM_LDA_N_PINLOC),
      .PORT_MEM_LDB_N_PINLOC               (PORT_MEM_LDB_N_PINLOC),
      .PORT_MEM_RWA_N_PINLOC               (PORT_MEM_RWA_N_PINLOC),
      .PORT_MEM_RWB_N_PINLOC               (PORT_MEM_RWB_N_PINLOC),
      .PORT_MEM_LBK0_N_PINLOC              (PORT_MEM_LBK0_N_PINLOC),
      .PORT_MEM_LBK1_N_PINLOC              (PORT_MEM_LBK1_N_PINLOC),
      .PORT_MEM_CFG_N_PINLOC               (PORT_MEM_CFG_N_PINLOC),
      .PORT_MEM_AP_PINLOC                  (PORT_MEM_AP_PINLOC),
      .PORT_MEM_AINV_PINLOC                (PORT_MEM_AINV_PINLOC),
      .PORT_MEM_DM_PINLOC                  (PORT_MEM_DM_PINLOC),
      .PORT_MEM_BWS_N_PINLOC               (PORT_MEM_BWS_N_PINLOC),
      .PORT_MEM_D_PINLOC                   (PORT_MEM_D_PINLOC),
      .PORT_MEM_DQ_PINLOC                  (PORT_MEM_DQ_PINLOC),
      .PORT_MEM_DBI_N_PINLOC               (PORT_MEM_DBI_N_PINLOC),
      .PORT_MEM_DQA_PINLOC                 (PORT_MEM_DQA_PINLOC),
      .PORT_MEM_DQB_PINLOC                 (PORT_MEM_DQB_PINLOC),
      .PORT_MEM_DINVA_PINLOC               (PORT_MEM_DINVA_PINLOC),
      .PORT_MEM_DINVB_PINLOC               (PORT_MEM_DINVB_PINLOC),
      .PORT_MEM_Q_PINLOC                   (PORT_MEM_Q_PINLOC),
      .PORT_MEM_DQS_PINLOC                 (PORT_MEM_DQS_PINLOC),
      .PORT_MEM_DQS_N_PINLOC               (PORT_MEM_DQS_N_PINLOC),
      .PORT_MEM_QK_PINLOC                  (PORT_MEM_QK_PINLOC),
      .PORT_MEM_QK_N_PINLOC                (PORT_MEM_QK_N_PINLOC),
      .PORT_MEM_QKA_PINLOC                 (PORT_MEM_QKA_PINLOC),
      .PORT_MEM_QKA_N_PINLOC               (PORT_MEM_QKA_N_PINLOC),
      .PORT_MEM_QKB_PINLOC                 (PORT_MEM_QKB_PINLOC),
      .PORT_MEM_QKB_N_PINLOC               (PORT_MEM_QKB_N_PINLOC),
      .PORT_MEM_CQ_PINLOC                  (PORT_MEM_CQ_PINLOC),
      .PORT_MEM_CQ_N_PINLOC                (PORT_MEM_CQ_N_PINLOC),
      .PORT_MEM_ALERT_N_PINLOC             (PORT_MEM_ALERT_N_PINLOC),
      .PORT_MEM_PE_N_PINLOC                (PORT_MEM_PE_N_PINLOC),

      .PHY_CALIBRATED_OCT                  (PHY_CALIBRATED_OCT),
      .PHY_AC_CALIBRATED_OCT               (PHY_AC_CALIBRATED_OCT),
      .PHY_CK_CALIBRATED_OCT               (PHY_CK_CALIBRATED_OCT),
      .PHY_DATA_CALIBRATED_OCT             (PHY_DATA_CALIBRATED_OCT)
   ) bufs_inst (
      .*
   );

   ////////////////////////////////////////////////////////////////////////////
   // I/O Aux
   ////////////////////////////////////////////////////////////////////////////
   ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_io_aux # (
      .SILICON_REV                         (SILICON_REV),
      .IS_HPS                              (IS_HPS),
      .SEQ_CODE_HEX_FILENAME               (SEQ_CODE_HEX_FILENAME),
      .SEQ_SYNTH_OSC_FREQ_MHZ              (SEQ_SYNTH_OSC_FREQ_MHZ),
      .SEQ_SYNTH_PARAMS_HEX_FILENAME       (SEQ_SYNTH_PARAMS_HEX_FILENAME),
      .SEQ_SYNTH_CPU_CLK_DIVIDE            (SEQ_SYNTH_CPU_CLK_DIVIDE),
      .SEQ_SYNTH_CAL_CLK_DIVIDE            (SEQ_SYNTH_CAL_CLK_DIVIDE),
      .SEQ_SIM_OSC_FREQ_MHZ                (SEQ_SIM_OSC_FREQ_MHZ),
      .SEQ_SIM_PARAMS_HEX_FILENAME         (SEQ_SIM_PARAMS_HEX_FILENAME),
      .SEQ_SIM_CPU_CLK_DIVIDE              (SEQ_SIM_CPU_CLK_DIVIDE),
      .SEQ_SIM_CAL_CLK_DIVIDE              (SEQ_SIM_CAL_CLK_DIVIDE),
      .DIAG_SYNTH_FOR_SIM                  (DIAG_SYNTH_FOR_SIM),
      .DIAG_VERBOSE_IOAUX                  (DIAG_VERBOSE_IOAUX),
      .DIAG_ECLIPSE_DEBUG                  (DIAG_ECLIPSE_DEBUG),
      .DIAG_EXPORT_VJI                     (DIAG_EXPORT_VJI),
      .DIAG_INTERFACE_ID                   (DIAG_INTERFACE_ID),
      .PORT_CAL_DEBUG_ADDRESS_WIDTH        (PORT_CAL_DEBUG_ADDRESS_WIDTH),
      .PORT_CAL_DEBUG_BYTEEN_WIDTH         (PORT_CAL_DEBUG_BYTEEN_WIDTH),
      .PORT_CAL_DEBUG_RDATA_WIDTH          (PORT_CAL_DEBUG_RDATA_WIDTH),
      .PORT_CAL_DEBUG_WDATA_WIDTH          (PORT_CAL_DEBUG_WDATA_WIDTH),
      .PORT_CAL_MASTER_ADDRESS_WIDTH       (PORT_CAL_MASTER_ADDRESS_WIDTH),
      .PORT_CAL_MASTER_BYTEEN_WIDTH        (PORT_CAL_MASTER_BYTEEN_WIDTH),
      .PORT_CAL_MASTER_RDATA_WIDTH         (PORT_CAL_MASTER_RDATA_WIDTH),
      .PORT_CAL_MASTER_WDATA_WIDTH         (PORT_CAL_MASTER_WDATA_WIDTH),
      .PORT_DFT_NF_IOAUX_PIO_IN_WIDTH      (PORT_DFT_NF_IOAUX_PIO_IN_WIDTH),
      .PORT_DFT_NF_IOAUX_PIO_OUT_WIDTH     (PORT_DFT_NF_IOAUX_PIO_OUT_WIDTH)
   ) io_aux_inst (
      .global_reset_n_int                   (global_reset_n_int_io_aux_in),
      .cal_debug_reset_n                    (cal_debug_reset_n_io_aux_in),
      .cal_slave_reset_n_in                 (cal_slave_reset_n_in_io_aux_in),
      .*
   );

   ////////////////////////////////////////////////////////////////////////////
   // Tiles and Lanes
   ////////////////////////////////////////////////////////////////////////////
   altera_emif_arch_nf_io_tiles_wrap # (
      .DIAG_VERBOSE_IOAUX                  (DIAG_VERBOSE_IOAUX),
      .DIAG_SYNTH_FOR_SIM                   (DIAG_SYNTH_FOR_SIM),
      .DIAG_CPA_OUT_1_EN                    (DIAG_CPA_OUT_1_EN),
      .DIAG_FAST_SIM                        (DIAG_FAST_SIM),
      .IS_HPS                               (IS_HPS),
      .SILICON_REV                          (SILICON_REV),
      .PROTOCOL_ENUM                        (PROTOCOL_ENUM),
      .PHY_PING_PONG_EN                     (PHY_PING_PONG_EN),
      .DQS_BUS_MODE_ENUM                    (DQS_BUS_MODE_ENUM),
      .USER_CLK_RATIO                       (USER_CLK_RATIO),
      .PHY_HMC_CLK_RATIO                    (PHY_HMC_CLK_RATIO),
      .C2P_P2C_CLK_RATIO                    (C2P_P2C_CLK_RATIO),
      .PLL_VCO_FREQ_MHZ_INT                 (PLL_VCO_FREQ_MHZ_INT),
      .PLL_VCO_TO_MEM_CLK_FREQ_RATIO        (PLL_VCO_TO_MEM_CLK_FREQ_RATIO),
      .MEM_BURST_LENGTH                     (MEM_BURST_LENGTH),
      .MEM_DATA_MASK_EN                     (MEM_DATA_MASK_EN),
      .NUM_OF_HMC_PORTS                     (NUM_OF_HMC_PORTS),
      .HMC_AVL_PROTOCOL_ENUM                (HMC_AVL_PROTOCOL_ENUM),
      .HMC_CTRL_DIMM_TYPE                   (HMC_CTRL_DIMM_TYPE),
      .PRI_HMC_CFG_ENABLE_ECC               (PRI_HMC_CFG_ENABLE_ECC),
      .PRI_HMC_CFG_REORDER_DATA             (PRI_HMC_CFG_REORDER_DATA),
      .PRI_HMC_CFG_REORDER_READ             (PRI_HMC_CFG_REORDER_READ),
      .PRI_HMC_CFG_REORDER_RDATA            (PRI_HMC_CFG_REORDER_RDATA),
      .PRI_HMC_CFG_STARVE_LIMIT             (PRI_HMC_CFG_STARVE_LIMIT),
      .PRI_HMC_CFG_DQS_TRACKING_EN          (PRI_HMC_CFG_DQS_TRACKING_EN),
      .PRI_HMC_CFG_ARBITER_TYPE             (PRI_HMC_CFG_ARBITER_TYPE),
      .PRI_HMC_CFG_OPEN_PAGE_EN             (PRI_HMC_CFG_OPEN_PAGE_EN),
      .PRI_HMC_CFG_GEAR_DOWN_EN             (PRI_HMC_CFG_GEAR_DOWN_EN),
      .PRI_HMC_CFG_RLD3_MULTIBANK_MODE      (PRI_HMC_CFG_RLD3_MULTIBANK_MODE),
      .PRI_HMC_CFG_PING_PONG_MODE           (PRI_HMC_CFG_PING_PONG_MODE),
      .PRI_HMC_CFG_SLOT_ROTATE_EN           (PRI_HMC_CFG_SLOT_ROTATE_EN),
      .PRI_HMC_CFG_SLOT_OFFSET              (PRI_HMC_CFG_SLOT_OFFSET),
      .PRI_HMC_CFG_COL_CMD_SLOT             (PRI_HMC_CFG_COL_CMD_SLOT),
      .PRI_HMC_CFG_ROW_CMD_SLOT             (PRI_HMC_CFG_ROW_CMD_SLOT),
      .PRI_HMC_CFG_ENABLE_RC                (PRI_HMC_CFG_ENABLE_RC),
      .PRI_HMC_CFG_CS_TO_CHIP_MAPPING       (PRI_HMC_CFG_CS_TO_CHIP_MAPPING),
      .PRI_HMC_CFG_RB_RESERVED_ENTRY        (PRI_HMC_CFG_RB_RESERVED_ENTRY),
      .PRI_HMC_CFG_WB_RESERVED_ENTRY        (PRI_HMC_CFG_WB_RESERVED_ENTRY),
      .PRI_HMC_CFG_TCL                      (PRI_HMC_CFG_TCL),
      .PRI_HMC_CFG_POWER_SAVING_EXIT_CYC    (PRI_HMC_CFG_POWER_SAVING_EXIT_CYC),
      .PRI_HMC_CFG_MEM_CLK_DISABLE_ENTRY_CYC(PRI_HMC_CFG_MEM_CLK_DISABLE_ENTRY_CYC),
      .PRI_HMC_CFG_WRITE_ODT_CHIP           (PRI_HMC_CFG_WRITE_ODT_CHIP),
      .PRI_HMC_CFG_READ_ODT_CHIP            (PRI_HMC_CFG_READ_ODT_CHIP),
      .PRI_HMC_CFG_WR_ODT_ON                (PRI_HMC_CFG_WR_ODT_ON),
      .PRI_HMC_CFG_RD_ODT_ON                (PRI_HMC_CFG_RD_ODT_ON),
      .PRI_HMC_CFG_WR_ODT_PERIOD            (PRI_HMC_CFG_WR_ODT_PERIOD),
      .PRI_HMC_CFG_RD_ODT_PERIOD            (PRI_HMC_CFG_RD_ODT_PERIOD),
      .PRI_HMC_CFG_RLD3_REFRESH_SEQ0        (PRI_HMC_CFG_RLD3_REFRESH_SEQ0),
      .PRI_HMC_CFG_RLD3_REFRESH_SEQ1        (PRI_HMC_CFG_RLD3_REFRESH_SEQ1),
      .PRI_HMC_CFG_RLD3_REFRESH_SEQ2        (PRI_HMC_CFG_RLD3_REFRESH_SEQ2),
      .PRI_HMC_CFG_RLD3_REFRESH_SEQ3        (PRI_HMC_CFG_RLD3_REFRESH_SEQ3),
      .PRI_HMC_CFG_SRF_ZQCAL_DISABLE        (PRI_HMC_CFG_SRF_ZQCAL_DISABLE),
      .PRI_HMC_CFG_MPS_ZQCAL_DISABLE        (PRI_HMC_CFG_MPS_ZQCAL_DISABLE),
      .PRI_HMC_CFG_MPS_DQSTRK_DISABLE       (PRI_HMC_CFG_MPS_DQSTRK_DISABLE),
      .PRI_HMC_CFG_SHORT_DQSTRK_CTRL_EN     (PRI_HMC_CFG_SHORT_DQSTRK_CTRL_EN),
      .PRI_HMC_CFG_PERIOD_DQSTRK_CTRL_EN    (PRI_HMC_CFG_PERIOD_DQSTRK_CTRL_EN),
      .PRI_HMC_CFG_PERIOD_DQSTRK_INTERVAL   (PRI_HMC_CFG_PERIOD_DQSTRK_INTERVAL),
      .PRI_HMC_CFG_DQSTRK_TO_VALID_LAST     (PRI_HMC_CFG_DQSTRK_TO_VALID_LAST),
      .PRI_HMC_CFG_DQSTRK_TO_VALID          (PRI_HMC_CFG_DQSTRK_TO_VALID),
      .PRI_HMC_CFG_RFSH_WARN_THRESHOLD      (PRI_HMC_CFG_RFSH_WARN_THRESHOLD),
      .PRI_HMC_CFG_SB_CG_DISABLE            (PRI_HMC_CFG_SB_CG_DISABLE),
      .PRI_HMC_CFG_USER_RFSH_EN             (PRI_HMC_CFG_USER_RFSH_EN),
      .PRI_HMC_CFG_SRF_AUTOEXIT_EN          (PRI_HMC_CFG_SRF_AUTOEXIT_EN),
      .PRI_HMC_CFG_SRF_ENTRY_EXIT_BLOCK     (PRI_HMC_CFG_SRF_ENTRY_EXIT_BLOCK),
      .PRI_HMC_CFG_SB_DDR4_MR3              (PRI_HMC_CFG_SB_DDR4_MR3),
      .PRI_HMC_CFG_SB_DDR4_MR4              (PRI_HMC_CFG_SB_DDR4_MR4),
      .PRI_HMC_CFG_SB_DDR4_MR5              (PRI_HMC_CFG_SB_DDR4_MR5),
      .PRI_HMC_CFG_DDR4_MPS_ADDR_MIRROR     (PRI_HMC_CFG_DDR4_MPS_ADDR_MIRROR),
      .PRI_HMC_CFG_MEM_IF_COLADDR_WIDTH     (PRI_HMC_CFG_MEM_IF_COLADDR_WIDTH),
      .PRI_HMC_CFG_MEM_IF_ROWADDR_WIDTH     (PRI_HMC_CFG_MEM_IF_ROWADDR_WIDTH),
      .PRI_HMC_CFG_MEM_IF_BANKADDR_WIDTH    (PRI_HMC_CFG_MEM_IF_BANKADDR_WIDTH),
      .PRI_HMC_CFG_MEM_IF_BGADDR_WIDTH      (PRI_HMC_CFG_MEM_IF_BGADDR_WIDTH),
      .PRI_HMC_CFG_LOCAL_IF_CS_WIDTH        (PRI_HMC_CFG_LOCAL_IF_CS_WIDTH),
      .PRI_HMC_CFG_ADDR_ORDER               (PRI_HMC_CFG_ADDR_ORDER),
      .PRI_HMC_CFG_ACT_TO_RDWR              (PRI_HMC_CFG_ACT_TO_RDWR),
      .PRI_HMC_CFG_ACT_TO_PCH               (PRI_HMC_CFG_ACT_TO_PCH),
      .PRI_HMC_CFG_ACT_TO_ACT               (PRI_HMC_CFG_ACT_TO_ACT),
      .PRI_HMC_CFG_ACT_TO_ACT_DIFF_BANK     (PRI_HMC_CFG_ACT_TO_ACT_DIFF_BANK),
      .PRI_HMC_CFG_ACT_TO_ACT_DIFF_BG       (PRI_HMC_CFG_ACT_TO_ACT_DIFF_BG),
      .PRI_HMC_CFG_RD_TO_RD                 (PRI_HMC_CFG_RD_TO_RD),
      .PRI_HMC_CFG_RD_TO_RD_DIFF_CHIP       (PRI_HMC_CFG_RD_TO_RD_DIFF_CHIP),
      .PRI_HMC_CFG_RD_TO_RD_DIFF_BG         (PRI_HMC_CFG_RD_TO_RD_DIFF_BG),
      .PRI_HMC_CFG_RD_TO_WR                 (PRI_HMC_CFG_RD_TO_WR),
      .PRI_HMC_CFG_RD_TO_WR_DIFF_CHIP       (PRI_HMC_CFG_RD_TO_WR_DIFF_CHIP),
      .PRI_HMC_CFG_RD_TO_WR_DIFF_BG         (PRI_HMC_CFG_RD_TO_WR_DIFF_BG),
      .PRI_HMC_CFG_RD_TO_PCH                (PRI_HMC_CFG_RD_TO_PCH),
      .PRI_HMC_CFG_RD_AP_TO_VALID           (PRI_HMC_CFG_RD_AP_TO_VALID),
      .PRI_HMC_CFG_WR_TO_WR                 (PRI_HMC_CFG_WR_TO_WR),
      .PRI_HMC_CFG_WR_TO_WR_DIFF_CHIP       (PRI_HMC_CFG_WR_TO_WR_DIFF_CHIP),
      .PRI_HMC_CFG_WR_TO_WR_DIFF_BG         (PRI_HMC_CFG_WR_TO_WR_DIFF_BG),
      .PRI_HMC_CFG_WR_TO_RD                 (PRI_HMC_CFG_WR_TO_RD),
      .PRI_HMC_CFG_WR_TO_RD_DIFF_CHIP       (PRI_HMC_CFG_WR_TO_RD_DIFF_CHIP),
      .PRI_HMC_CFG_WR_TO_RD_DIFF_BG         (PRI_HMC_CFG_WR_TO_RD_DIFF_BG),
      .PRI_HMC_CFG_WR_TO_PCH                (PRI_HMC_CFG_WR_TO_PCH),
      .PRI_HMC_CFG_WR_AP_TO_VALID           (PRI_HMC_CFG_WR_AP_TO_VALID),
      .PRI_HMC_CFG_PCH_TO_VALID             (PRI_HMC_CFG_PCH_TO_VALID),
      .PRI_HMC_CFG_PCH_ALL_TO_VALID         (PRI_HMC_CFG_PCH_ALL_TO_VALID),
      .PRI_HMC_CFG_ARF_TO_VALID             (PRI_HMC_CFG_ARF_TO_VALID),
      .PRI_HMC_CFG_PDN_TO_VALID             (PRI_HMC_CFG_PDN_TO_VALID),
      .PRI_HMC_CFG_SRF_TO_VALID             (PRI_HMC_CFG_SRF_TO_VALID),
      .PRI_HMC_CFG_SRF_TO_ZQ_CAL            (PRI_HMC_CFG_SRF_TO_ZQ_CAL),
      .PRI_HMC_CFG_ARF_PERIOD               (PRI_HMC_CFG_ARF_PERIOD),
      .PRI_HMC_CFG_PDN_PERIOD               (PRI_HMC_CFG_PDN_PERIOD),
      .PRI_HMC_CFG_ZQCL_TO_VALID            (PRI_HMC_CFG_ZQCL_TO_VALID),
      .PRI_HMC_CFG_ZQCS_TO_VALID            (PRI_HMC_CFG_ZQCS_TO_VALID),
      .PRI_HMC_CFG_MRS_TO_VALID             (PRI_HMC_CFG_MRS_TO_VALID),
      .PRI_HMC_CFG_MPS_TO_VALID             (PRI_HMC_CFG_MPS_TO_VALID),
      .PRI_HMC_CFG_MRR_TO_VALID             (PRI_HMC_CFG_MRR_TO_VALID),
      .PRI_HMC_CFG_MPR_TO_VALID             (PRI_HMC_CFG_MPR_TO_VALID),
      .PRI_HMC_CFG_MPS_EXIT_CS_TO_CKE       (PRI_HMC_CFG_MPS_EXIT_CS_TO_CKE),
      .PRI_HMC_CFG_MPS_EXIT_CKE_TO_CS       (PRI_HMC_CFG_MPS_EXIT_CKE_TO_CS),
      .PRI_HMC_CFG_RLD3_MULTIBANK_REF_DELAY (PRI_HMC_CFG_RLD3_MULTIBANK_REF_DELAY),
      .PRI_HMC_CFG_MMR_CMD_TO_VALID         (PRI_HMC_CFG_MMR_CMD_TO_VALID),
      .PRI_HMC_CFG_4_ACT_TO_ACT             (PRI_HMC_CFG_4_ACT_TO_ACT),
      .PRI_HMC_CFG_16_ACT_TO_ACT            (PRI_HMC_CFG_16_ACT_TO_ACT),

      .SEC_HMC_CFG_ENABLE_ECC               (SEC_HMC_CFG_ENABLE_ECC),
      .SEC_HMC_CFG_REORDER_DATA             (SEC_HMC_CFG_REORDER_DATA),
      .SEC_HMC_CFG_REORDER_READ             (SEC_HMC_CFG_REORDER_READ),
      .SEC_HMC_CFG_REORDER_RDATA            (SEC_HMC_CFG_REORDER_RDATA),
      .SEC_HMC_CFG_STARVE_LIMIT             (SEC_HMC_CFG_STARVE_LIMIT),
      .SEC_HMC_CFG_DQS_TRACKING_EN          (SEC_HMC_CFG_DQS_TRACKING_EN),
      .SEC_HMC_CFG_ARBITER_TYPE             (SEC_HMC_CFG_ARBITER_TYPE),
      .SEC_HMC_CFG_OPEN_PAGE_EN             (SEC_HMC_CFG_OPEN_PAGE_EN),
      .SEC_HMC_CFG_GEAR_DOWN_EN             (SEC_HMC_CFG_GEAR_DOWN_EN),
      .SEC_HMC_CFG_RLD3_MULTIBANK_MODE      (SEC_HMC_CFG_RLD3_MULTIBANK_MODE),
      .SEC_HMC_CFG_PING_PONG_MODE           (SEC_HMC_CFG_PING_PONG_MODE),
      .SEC_HMC_CFG_SLOT_ROTATE_EN           (SEC_HMC_CFG_SLOT_ROTATE_EN),
      .SEC_HMC_CFG_SLOT_OFFSET              (SEC_HMC_CFG_SLOT_OFFSET),
      .SEC_HMC_CFG_COL_CMD_SLOT             (SEC_HMC_CFG_COL_CMD_SLOT),
      .SEC_HMC_CFG_ROW_CMD_SLOT             (SEC_HMC_CFG_ROW_CMD_SLOT),
      .SEC_HMC_CFG_ENABLE_RC                (SEC_HMC_CFG_ENABLE_RC),
      .SEC_HMC_CFG_CS_TO_CHIP_MAPPING       (SEC_HMC_CFG_CS_TO_CHIP_MAPPING),
      .SEC_HMC_CFG_RB_RESERVED_ENTRY        (SEC_HMC_CFG_RB_RESERVED_ENTRY),
      .SEC_HMC_CFG_WB_RESERVED_ENTRY        (SEC_HMC_CFG_WB_RESERVED_ENTRY),
      .SEC_HMC_CFG_TCL                      (SEC_HMC_CFG_TCL),
      .SEC_HMC_CFG_POWER_SAVING_EXIT_CYC    (SEC_HMC_CFG_POWER_SAVING_EXIT_CYC),
      .SEC_HMC_CFG_MEM_CLK_DISABLE_ENTRY_CYC(SEC_HMC_CFG_MEM_CLK_DISABLE_ENTRY_CYC),
      .SEC_HMC_CFG_WRITE_ODT_CHIP           (SEC_HMC_CFG_WRITE_ODT_CHIP),
      .SEC_HMC_CFG_READ_ODT_CHIP            (SEC_HMC_CFG_READ_ODT_CHIP),
      .SEC_HMC_CFG_WR_ODT_ON                (SEC_HMC_CFG_WR_ODT_ON),
      .SEC_HMC_CFG_RD_ODT_ON                (SEC_HMC_CFG_RD_ODT_ON),
      .SEC_HMC_CFG_WR_ODT_PERIOD            (SEC_HMC_CFG_WR_ODT_PERIOD),
      .SEC_HMC_CFG_RD_ODT_PERIOD            (SEC_HMC_CFG_RD_ODT_PERIOD),
      .SEC_HMC_CFG_RLD3_REFRESH_SEQ0        (SEC_HMC_CFG_RLD3_REFRESH_SEQ0),
      .SEC_HMC_CFG_RLD3_REFRESH_SEQ1        (SEC_HMC_CFG_RLD3_REFRESH_SEQ1),
      .SEC_HMC_CFG_RLD3_REFRESH_SEQ2        (SEC_HMC_CFG_RLD3_REFRESH_SEQ2),
      .SEC_HMC_CFG_RLD3_REFRESH_SEQ3        (SEC_HMC_CFG_RLD3_REFRESH_SEQ3),
      .SEC_HMC_CFG_SRF_ZQCAL_DISABLE        (SEC_HMC_CFG_SRF_ZQCAL_DISABLE),
      .SEC_HMC_CFG_MPS_ZQCAL_DISABLE        (SEC_HMC_CFG_MPS_ZQCAL_DISABLE),
      .SEC_HMC_CFG_MPS_DQSTRK_DISABLE       (SEC_HMC_CFG_MPS_DQSTRK_DISABLE),
      .SEC_HMC_CFG_SHORT_DQSTRK_CTRL_EN     (SEC_HMC_CFG_SHORT_DQSTRK_CTRL_EN),
      .SEC_HMC_CFG_PERIOD_DQSTRK_CTRL_EN    (SEC_HMC_CFG_PERIOD_DQSTRK_CTRL_EN),
      .SEC_HMC_CFG_PERIOD_DQSTRK_INTERVAL   (SEC_HMC_CFG_PERIOD_DQSTRK_INTERVAL),
      .SEC_HMC_CFG_DQSTRK_TO_VALID_LAST     (SEC_HMC_CFG_DQSTRK_TO_VALID_LAST),
      .SEC_HMC_CFG_DQSTRK_TO_VALID          (SEC_HMC_CFG_DQSTRK_TO_VALID),
      .SEC_HMC_CFG_RFSH_WARN_THRESHOLD      (SEC_HMC_CFG_RFSH_WARN_THRESHOLD),
      .SEC_HMC_CFG_SB_CG_DISABLE            (SEC_HMC_CFG_SB_CG_DISABLE),
      .SEC_HMC_CFG_USER_RFSH_EN             (SEC_HMC_CFG_USER_RFSH_EN),
      .SEC_HMC_CFG_SRF_AUTOEXIT_EN          (SEC_HMC_CFG_SRF_AUTOEXIT_EN),
      .SEC_HMC_CFG_SRF_ENTRY_EXIT_BLOCK     (SEC_HMC_CFG_SRF_ENTRY_EXIT_BLOCK),
      .SEC_HMC_CFG_SB_DDR4_MR3              (SEC_HMC_CFG_SB_DDR4_MR3),
      .SEC_HMC_CFG_SB_DDR4_MR4              (SEC_HMC_CFG_SB_DDR4_MR4),
      .SEC_HMC_CFG_SB_DDR4_MR5              (SEC_HMC_CFG_SB_DDR4_MR5),
      .SEC_HMC_CFG_DDR4_MPS_ADDR_MIRROR     (SEC_HMC_CFG_DDR4_MPS_ADDR_MIRROR),
      .SEC_HMC_CFG_MEM_IF_COLADDR_WIDTH     (SEC_HMC_CFG_MEM_IF_COLADDR_WIDTH),
      .SEC_HMC_CFG_MEM_IF_ROWADDR_WIDTH     (SEC_HMC_CFG_MEM_IF_ROWADDR_WIDTH),
      .SEC_HMC_CFG_MEM_IF_BANKADDR_WIDTH    (SEC_HMC_CFG_MEM_IF_BANKADDR_WIDTH),
      .SEC_HMC_CFG_MEM_IF_BGADDR_WIDTH      (SEC_HMC_CFG_MEM_IF_BGADDR_WIDTH),
      .SEC_HMC_CFG_LOCAL_IF_CS_WIDTH        (SEC_HMC_CFG_LOCAL_IF_CS_WIDTH),
      .SEC_HMC_CFG_ADDR_ORDER               (SEC_HMC_CFG_ADDR_ORDER),
      .SEC_HMC_CFG_ACT_TO_RDWR              (SEC_HMC_CFG_ACT_TO_RDWR),
      .SEC_HMC_CFG_ACT_TO_PCH               (SEC_HMC_CFG_ACT_TO_PCH),
      .SEC_HMC_CFG_ACT_TO_ACT               (SEC_HMC_CFG_ACT_TO_ACT),
      .SEC_HMC_CFG_ACT_TO_ACT_DIFF_BANK     (SEC_HMC_CFG_ACT_TO_ACT_DIFF_BANK),
      .SEC_HMC_CFG_ACT_TO_ACT_DIFF_BG       (SEC_HMC_CFG_ACT_TO_ACT_DIFF_BG),
      .SEC_HMC_CFG_RD_TO_RD                 (SEC_HMC_CFG_RD_TO_RD),
      .SEC_HMC_CFG_RD_TO_RD_DIFF_CHIP       (SEC_HMC_CFG_RD_TO_RD_DIFF_CHIP),
      .SEC_HMC_CFG_RD_TO_RD_DIFF_BG         (SEC_HMC_CFG_RD_TO_RD_DIFF_BG),
      .SEC_HMC_CFG_RD_TO_WR                 (SEC_HMC_CFG_RD_TO_WR),
      .SEC_HMC_CFG_RD_TO_WR_DIFF_CHIP       (SEC_HMC_CFG_RD_TO_WR_DIFF_CHIP),
      .SEC_HMC_CFG_RD_TO_WR_DIFF_BG         (SEC_HMC_CFG_RD_TO_WR_DIFF_BG),
      .SEC_HMC_CFG_RD_TO_PCH                (SEC_HMC_CFG_RD_TO_PCH),
      .SEC_HMC_CFG_RD_AP_TO_VALID           (SEC_HMC_CFG_RD_AP_TO_VALID),
      .SEC_HMC_CFG_WR_TO_WR                 (SEC_HMC_CFG_WR_TO_WR),
      .SEC_HMC_CFG_WR_TO_WR_DIFF_CHIP       (SEC_HMC_CFG_WR_TO_WR_DIFF_CHIP),
      .SEC_HMC_CFG_WR_TO_WR_DIFF_BG         (SEC_HMC_CFG_WR_TO_WR_DIFF_BG),
      .SEC_HMC_CFG_WR_TO_RD                 (SEC_HMC_CFG_WR_TO_RD),
      .SEC_HMC_CFG_WR_TO_RD_DIFF_CHIP       (SEC_HMC_CFG_WR_TO_RD_DIFF_CHIP),
      .SEC_HMC_CFG_WR_TO_RD_DIFF_BG         (SEC_HMC_CFG_WR_TO_RD_DIFF_BG),
      .SEC_HMC_CFG_WR_TO_PCH                (SEC_HMC_CFG_WR_TO_PCH),
      .SEC_HMC_CFG_WR_AP_TO_VALID           (SEC_HMC_CFG_WR_AP_TO_VALID),
      .SEC_HMC_CFG_PCH_TO_VALID             (SEC_HMC_CFG_PCH_TO_VALID),
      .SEC_HMC_CFG_PCH_ALL_TO_VALID         (SEC_HMC_CFG_PCH_ALL_TO_VALID),
      .SEC_HMC_CFG_ARF_TO_VALID             (SEC_HMC_CFG_ARF_TO_VALID),
      .SEC_HMC_CFG_PDN_TO_VALID             (SEC_HMC_CFG_PDN_TO_VALID),
      .SEC_HMC_CFG_SRF_TO_VALID             (SEC_HMC_CFG_SRF_TO_VALID),
      .SEC_HMC_CFG_SRF_TO_ZQ_CAL            (SEC_HMC_CFG_SRF_TO_ZQ_CAL),
      .SEC_HMC_CFG_ARF_PERIOD               (SEC_HMC_CFG_ARF_PERIOD),
      .SEC_HMC_CFG_PDN_PERIOD               (SEC_HMC_CFG_PDN_PERIOD),
      .SEC_HMC_CFG_ZQCL_TO_VALID            (SEC_HMC_CFG_ZQCL_TO_VALID),
      .SEC_HMC_CFG_ZQCS_TO_VALID            (SEC_HMC_CFG_ZQCS_TO_VALID),
      .SEC_HMC_CFG_MRS_TO_VALID             (SEC_HMC_CFG_MRS_TO_VALID),
      .SEC_HMC_CFG_MPS_TO_VALID             (SEC_HMC_CFG_MPS_TO_VALID),
      .SEC_HMC_CFG_MRR_TO_VALID             (SEC_HMC_CFG_MRR_TO_VALID),
      .SEC_HMC_CFG_MPR_TO_VALID             (SEC_HMC_CFG_MPR_TO_VALID),
      .SEC_HMC_CFG_MPS_EXIT_CS_TO_CKE       (SEC_HMC_CFG_MPS_EXIT_CS_TO_CKE),
      .SEC_HMC_CFG_MPS_EXIT_CKE_TO_CS       (SEC_HMC_CFG_MPS_EXIT_CKE_TO_CS),
      .SEC_HMC_CFG_RLD3_MULTIBANK_REF_DELAY (SEC_HMC_CFG_RLD3_MULTIBANK_REF_DELAY),
      .SEC_HMC_CFG_MMR_CMD_TO_VALID         (SEC_HMC_CFG_MMR_CMD_TO_VALID),
      .SEC_HMC_CFG_4_ACT_TO_ACT             (SEC_HMC_CFG_4_ACT_TO_ACT),
      .SEC_HMC_CFG_16_ACT_TO_ACT            (SEC_HMC_CFG_16_ACT_TO_ACT),
      .PINS_PER_LANE                        (PINS_PER_LANE),
      .LANES_PER_TILE                       (LANES_PER_TILE),
      .PINS_IN_RTL_TILES                    (PINS_IN_RTL_TILES),
      .LANES_IN_RTL_TILES                   (LANES_IN_RTL_TILES),
      .NUM_OF_RTL_TILES                     (NUM_OF_RTL_TILES),
      .AC_PIN_MAP_SCHEME                    (AC_PIN_MAP_SCHEME),
      .PRI_AC_TILE_INDEX                    (PRI_AC_TILE_INDEX),
      .SEC_AC_TILE_INDEX                    (SEC_AC_TILE_INDEX),
      .PRI_HMC_DBC_SHADOW_LANE_INDEX        (PRI_HMC_DBC_SHADOW_LANE_INDEX),
      .LANES_USAGE                          (LANES_USAGE),
      .PINS_USAGE                           (PINS_USAGE),
      .PINS_RATE                            (PINS_RATE),
      .PINS_WDB                             (PINS_WDB),
      .PINS_DB_IN_BYPASS                    (PINS_DB_IN_BYPASS),
      .PINS_DB_OUT_BYPASS                   (PINS_DB_OUT_BYPASS),
      .PINS_DB_OE_BYPASS                    (PINS_DB_OE_BYPASS),
      .PINS_INVERT_WR                       (PINS_INVERT_WR),
      .PINS_INVERT_OE                       (PINS_INVERT_OE),
      .PINS_AC_HMC_DATA_OVERRIDE_ENA        (PINS_AC_HMC_DATA_OVERRIDE_ENA),
      .PINS_DATA_IN_MODE                    (PINS_DATA_IN_MODE),
      .PINS_OCT_MODE                        (PINS_OCT_MODE),
      .PINS_GPIO_MODE                       (PINS_GPIO_MODE),
      .CENTER_TIDS                          (CENTER_TIDS),
      .HMC_TIDS                             (HMC_TIDS),
      .LANE_TIDS                            (LANE_TIDS),
      .PREAMBLE_MODE                        (PREAMBLE_MODE),
      .DBI_WR_ENABLE                        (DBI_WR_ENABLE),
      .DBI_RD_ENABLE                        (DBI_RD_ENABLE),
      .CRC_EN                               (CRC_EN),
      .SWAP_DQS_A_B                         (SWAP_DQS_A_B),
      .DQS_PACK_MODE                        (DQS_PACK_MODE),
      .OCT_SIZE                             (OCT_SIZE),
      .DBC_WB_RESERVED_ENTRY                (DBC_WB_RESERVED_ENTRY),
      .DLL_MODE                             (DLL_MODE),
      .DLL_CODEWORD                         (DLL_CODEWORD),
      .PORT_MEM_DQS_WIDTH                   (PORT_MEM_DQS_WIDTH),
      .PORT_MEM_DQ_WIDTH                    (PORT_MEM_DQ_WIDTH),
      .PORT_DFT_NF_PA_DPRIO_REG_ADDR_WIDTH  (PORT_DFT_NF_PA_DPRIO_REG_ADDR_WIDTH),
      .PORT_DFT_NF_PA_DPRIO_WRITEDATA_WIDTH (PORT_DFT_NF_PA_DPRIO_WRITEDATA_WIDTH),
      .PORT_DFT_NF_PA_DPRIO_READDATA_WIDTH  (PORT_DFT_NF_PA_DPRIO_READDATA_WIDTH),
      .PORT_MEM_A_PINLOC                    (PORT_MEM_A_PINLOC),
      .PORT_MEM_BA_PINLOC                   (PORT_MEM_BA_PINLOC),
      .PORT_MEM_BG_PINLOC                   (PORT_MEM_BG_PINLOC),
      .PORT_MEM_CS_N_PINLOC                 (PORT_MEM_CS_N_PINLOC),
      .PORT_MEM_ACT_N_PINLOC                (PORT_MEM_ACT_N_PINLOC),
      .PORT_MEM_DQ_PINLOC                   (PORT_MEM_DQ_PINLOC),
      .PORT_MEM_DM_PINLOC                   (PORT_MEM_DM_PINLOC),
      .PORT_MEM_DBI_N_PINLOC                (PORT_MEM_DBI_N_PINLOC),
      .PORT_MEM_RAS_N_PINLOC                (PORT_MEM_RAS_N_PINLOC),
      .PORT_MEM_CAS_N_PINLOC                (PORT_MEM_CAS_N_PINLOC),
      .PORT_MEM_WE_N_PINLOC                 (PORT_MEM_WE_N_PINLOC),
      .PORT_MEM_REF_N_PINLOC                (PORT_MEM_REF_N_PINLOC),
      .PORT_MEM_WPS_N_PINLOC                (PORT_MEM_WPS_N_PINLOC),
      .PORT_MEM_RPS_N_PINLOC                (PORT_MEM_RPS_N_PINLOC),
      .PORT_MEM_BWS_N_PINLOC                (PORT_MEM_BWS_N_PINLOC),
      .PORT_MEM_DQA_PINLOC                  (PORT_MEM_DQA_PINLOC),
      .PORT_MEM_DQB_PINLOC                  (PORT_MEM_DQB_PINLOC),
      .PORT_MEM_Q_PINLOC                    (PORT_MEM_Q_PINLOC),
      .PORT_MEM_D_PINLOC                    (PORT_MEM_D_PINLOC),
      .PORT_MEM_RWA_N_PINLOC                (PORT_MEM_RWA_N_PINLOC),
      .PORT_MEM_RWB_N_PINLOC                (PORT_MEM_RWB_N_PINLOC),
      .PORT_MEM_QKA_PINLOC                  (PORT_MEM_QKA_PINLOC),
      .PORT_MEM_QKB_PINLOC                  (PORT_MEM_QKB_PINLOC),
      .PORT_MEM_LDA_N_PINLOC                (PORT_MEM_LDA_N_PINLOC),
      .PORT_MEM_LDB_N_PINLOC                (PORT_MEM_LDB_N_PINLOC),
      .PORT_MEM_CK_PINLOC                   (PORT_MEM_CK_PINLOC),
      .PORT_MEM_DINVA_PINLOC                (PORT_MEM_DINVA_PINLOC),
      .PORT_MEM_DINVB_PINLOC                (PORT_MEM_DINVB_PINLOC),
      .PORT_MEM_AINV_PINLOC                 (PORT_MEM_AINV_PINLOC),
      .PORT_MEM_DM_WIDTH                    (PORT_MEM_DM_WIDTH),
      .PORT_MEM_A_WIDTH                     (PORT_MEM_A_WIDTH),
      .PORT_MEM_BA_WIDTH                    (PORT_MEM_BA_WIDTH),
      .PORT_MEM_BG_WIDTH                    (PORT_MEM_BG_WIDTH),
      .PORT_MEM_CS_N_WIDTH                  (PORT_MEM_CS_N_WIDTH),
      .PORT_MEM_ACT_N_WIDTH                 (PORT_MEM_ACT_N_WIDTH),
      .PORT_MEM_DBI_N_WIDTH                 (PORT_MEM_DBI_N_WIDTH),
      .PORT_MEM_RAS_N_WIDTH                 (PORT_MEM_RAS_N_WIDTH),
      .PORT_MEM_CAS_N_WIDTH                 (PORT_MEM_CAS_N_WIDTH),
      .PORT_MEM_WE_N_WIDTH                  (PORT_MEM_WE_N_WIDTH),
      .PORT_MEM_REF_N_WIDTH                 (PORT_MEM_REF_N_WIDTH),
      .PORT_MEM_WPS_N_WIDTH                 (PORT_MEM_WPS_N_WIDTH),
      .PORT_MEM_RPS_N_WIDTH                 (PORT_MEM_RPS_N_WIDTH),
      .PORT_MEM_BWS_N_WIDTH                 (PORT_MEM_BWS_N_WIDTH),
      .PORT_MEM_DQA_WIDTH                   (PORT_MEM_DQA_WIDTH),
      .PORT_MEM_DQB_WIDTH                   (PORT_MEM_DQB_WIDTH),
      .PORT_MEM_Q_WIDTH                     (PORT_MEM_Q_WIDTH),
      .PORT_MEM_D_WIDTH                     (PORT_MEM_D_WIDTH),
      .PORT_MEM_RWA_N_WIDTH                 (PORT_MEM_RWA_N_WIDTH),
      .PORT_MEM_RWB_N_WIDTH                 (PORT_MEM_RWB_N_WIDTH),
      .PORT_MEM_QKA_WIDTH                   (PORT_MEM_QKA_WIDTH),
      .PORT_MEM_QKB_WIDTH                   (PORT_MEM_QKB_WIDTH),
      .PORT_MEM_LDA_N_WIDTH                 (PORT_MEM_LDA_N_WIDTH),
      .PORT_MEM_LDB_N_WIDTH                 (PORT_MEM_LDB_N_WIDTH),
      .PORT_MEM_CK_WIDTH                    (PORT_MEM_CK_WIDTH),
      .PORT_MEM_DINVA_WIDTH                 (PORT_MEM_DINVA_WIDTH),
      .PORT_MEM_DINVB_WIDTH                 (PORT_MEM_DINVB_WIDTH),
      .PORT_MEM_AINV_WIDTH                  (PORT_MEM_AINV_WIDTH),
      .DIAG_USE_ABSTRACT_PHY                (DIAG_USE_ABSTRACT_PHY_AFT_SYNTH_OVRD),
      .DIAG_ABSTRACT_PHY_WLAT               (DIAG_ABSTRACT_PHY_WLAT),
      .DIAG_ABSTRACT_PHY_RLAT               (DIAG_ABSTRACT_PHY_RLAT),
      .ABPHY_WRITE_PROTOCOL                 (ABPHY_WRITE_PROTOCOL)
   ) io_tiles_wrap_inst (
      .l2core_data                                        (l2core_data),
      .runAbstractPhySim                                  (runAbstractPhySim),
      .*
   );

   generate
   if (DIAG_USE_ABSTRACT_PHY_AFT_SYNTH_OVRD == 0)
   begin : nonabphy_connections
      assign global_reset_n_int_io_aux_in   = global_reset_n_int;
      assign cal_debug_reset_n_io_aux_in    = cal_debug_reset_n;
      assign cal_slave_reset_n_in_io_aux_in = cal_slave_reset_n_in;
   end
   else begin : abphy_connections
      assign global_reset_n_int_io_aux_in   = runAbstractPhySim==0 ? global_reset_n_int   : 'b0;
      assign cal_debug_reset_n_io_aux_in    = runAbstractPhySim==0 ? cal_debug_reset_n    : 'b0;
      assign cal_slave_reset_n_in_io_aux_in = runAbstractPhySim==0 ? cal_slave_reset_n_in : 'b0;
   end
   endgenerate

   ////////////////////////////////////////////////////////////////////////////
   // Expose sequencer interface
   ////////////////////////////////////////////////////////////////////////////
   altera_emif_arch_nf_seq_if # (
      .PHY_CONFIG_ENUM                     (PHY_CONFIG_ENUM),
      .USER_CLK_RATIO                      (USER_CLK_RATIO),
      .REGISTER_AFI                        (REGISTER_AFI),
      .PORT_AFI_RLAT_WIDTH                 (PORT_AFI_RLAT_WIDTH),
      .PORT_AFI_WLAT_WIDTH                 (PORT_AFI_WLAT_WIDTH),
      .PORT_AFI_SEQ_BUSY_WIDTH             (PORT_AFI_SEQ_BUSY_WIDTH),
      .PORT_HPS_EMIF_E2H_GP_WIDTH          (PORT_HPS_EMIF_E2H_GP_WIDTH),
      .PORT_HPS_EMIF_H2E_GP_WIDTH          (PORT_HPS_EMIF_H2E_GP_WIDTH),
      .PHY_USERMODE_OCT                    (PHY_USERMODE_OCT),
      .PHY_PERIODIC_OCT_RECAL              (PHY_PERIODIC_OCT_RECAL),
      .PHY_HAS_DCC                         (PHY_HAS_DCC),
      .IS_HPS                              (IS_HPS)
   ) seq_if_inst (
      .*
   );

   ////////////////////////////////////////////////////////////////////////////
   // Expose HMC signals from io_tiles as proper Avalon signals
   ////////////////////////////////////////////////////////////////////////////
   altera_emif_arch_nf_hmc_avl_if # (
      .NUM_OF_HMC_PORTS                     (NUM_OF_HMC_PORTS),
      .HMC_AVL_PROTOCOL_ENUM                (HMC_AVL_PROTOCOL_ENUM),
      .LANES_PER_TILE                       (LANES_PER_TILE),
      .NUM_OF_RTL_TILES                     (NUM_OF_RTL_TILES),
      .PRI_AC_TILE_INDEX                    (PRI_AC_TILE_INDEX),
      .PRI_RDATA_TILE_INDEX                 (PRI_RDATA_TILE_INDEX),
      .PRI_RDATA_LANE_INDEX                 (PRI_RDATA_LANE_INDEX),
      .PRI_WDATA_TILE_INDEX                 (PRI_WDATA_TILE_INDEX),
      .PRI_WDATA_LANE_INDEX                 (PRI_WDATA_LANE_INDEX),
      .SEC_AC_TILE_INDEX                    (SEC_AC_TILE_INDEX),
      .SEC_RDATA_TILE_INDEX                 (SEC_RDATA_TILE_INDEX),
      .SEC_RDATA_LANE_INDEX                 (SEC_RDATA_LANE_INDEX),
      .SEC_WDATA_TILE_INDEX                 (SEC_WDATA_TILE_INDEX),
      .SEC_WDATA_LANE_INDEX                 (SEC_WDATA_LANE_INDEX),
      .PRI_HMC_DBC_SHADOW_LANE_INDEX        (PRI_HMC_DBC_SHADOW_LANE_INDEX),
      .PORT_CTRL_AST_CMD_DATA_WIDTH         (PORT_CTRL_AST_CMD_DATA_WIDTH),
      .PORT_CTRL_AMM_ADDRESS_WIDTH          (PORT_CTRL_AMM_ADDRESS_WIDTH),
      .PORT_CTRL_AMM_BCOUNT_WIDTH           (PORT_CTRL_AMM_BCOUNT_WIDTH)
   ) hmc_avl_if_inst (
      .*
   );

   ////////////////////////////////////////////////////////////////////////////
   // Expose HMC sideband interfaces
   ////////////////////////////////////////////////////////////////////////////
   altera_emif_arch_nf_hmc_sideband_if # (
      .PHY_PING_PONG_EN                     (PHY_PING_PONG_EN),
      .LANES_PER_TILE                       (LANES_PER_TILE),
      .NUM_OF_RTL_TILES                     (NUM_OF_RTL_TILES),
      .PRI_AC_TILE_INDEX                    (PRI_AC_TILE_INDEX),
      .SEC_AC_TILE_INDEX                    (SEC_AC_TILE_INDEX),
      .PRI_RDATA_TILE_INDEX                 (PRI_RDATA_TILE_INDEX),
      .PRI_RDATA_LANE_INDEX                 (PRI_RDATA_LANE_INDEX),
      .PRI_WDATA_TILE_INDEX                 (PRI_WDATA_TILE_INDEX),
      .PRI_WDATA_LANE_INDEX                 (PRI_WDATA_LANE_INDEX),
      .SEC_RDATA_TILE_INDEX                 (SEC_RDATA_TILE_INDEX),
      .SEC_RDATA_LANE_INDEX                 (SEC_RDATA_LANE_INDEX),
      .SEC_WDATA_TILE_INDEX                 (SEC_WDATA_TILE_INDEX),
      .SEC_WDATA_LANE_INDEX                 (SEC_WDATA_LANE_INDEX),
      .PRI_HMC_DBC_SHADOW_LANE_INDEX        (PRI_HMC_DBC_SHADOW_LANE_INDEX),
      .PRI_HMC_CFG_ENABLE_ECC               (PRI_HMC_CFG_ENABLE_ECC),
      .SEC_HMC_CFG_ENABLE_ECC               (SEC_HMC_CFG_ENABLE_ECC),
      .PORT_CTRL_USER_REFRESH_REQ_WIDTH     (PORT_CTRL_USER_REFRESH_REQ_WIDTH),
      .PORT_CTRL_USER_REFRESH_BANK_WIDTH    (PORT_CTRL_USER_REFRESH_BANK_WIDTH),
      .PORT_CTRL_SELF_REFRESH_REQ_WIDTH     (PORT_CTRL_SELF_REFRESH_REQ_WIDTH),
      .PORT_CTRL_ECC_WRITE_INFO_WIDTH       (PORT_CTRL_ECC_WRITE_INFO_WIDTH),
      .PORT_CTRL_ECC_READ_INFO_WIDTH        (PORT_CTRL_ECC_READ_INFO_WIDTH),
      .PORT_CTRL_ECC_CMD_INFO_WIDTH         (PORT_CTRL_ECC_CMD_INFO_WIDTH),
      .PORT_CTRL_ECC_WB_POINTER_WIDTH       (PORT_CTRL_ECC_WB_POINTER_WIDTH),
      .PORT_CTRL_ECC_RDATA_ID_WIDTH         (PORT_CTRL_ECC_RDATA_ID_WIDTH)
   ) hmc_sideband_if_inst (
      .*
   );

   ////////////////////////////////////////////////////////////////////////////
   // Expose HMC MMR interface
   ////////////////////////////////////////////////////////////////////////////
   altera_emif_arch_nf_hmc_mmr_if # (
      .PORT_CTRL_MMR_SLAVE_ADDRESS_WIDTH          (PORT_CTRL_MMR_SLAVE_ADDRESS_WIDTH),
      .PORT_CTRL_MMR_SLAVE_RDATA_WIDTH            (PORT_CTRL_MMR_SLAVE_RDATA_WIDTH),
      .PORT_CTRL_MMR_SLAVE_WDATA_WIDTH            (PORT_CTRL_MMR_SLAVE_WDATA_WIDTH),
      .PORT_CTRL_MMR_SLAVE_BCOUNT_WIDTH           (PORT_CTRL_MMR_SLAVE_BCOUNT_WIDTH)
   ) hmc_mmr_if_inst (
      .*
   );

   ////////////////////////////////////////////////////////////////////////////
   // Rewire and expose data signals
   ////////////////////////////////////////////////////////////////////////////
   generate
   if (NUM_OF_HMC_PORTS == 0)
   begin : afi
      altera_emif_arch_nf_afi_if # (
         .MEM_TTL_DATA_WIDTH                  (MEM_TTL_DATA_WIDTH),
         .MEM_TTL_NUM_OF_READ_GROUPS          (MEM_TTL_NUM_OF_READ_GROUPS),
         .MEM_TTL_NUM_OF_WRITE_GROUPS         (MEM_TTL_NUM_OF_WRITE_GROUPS),
         .REGISTER_AFI                        (REGISTER_AFI),

         .PORT_AFI_ADDR_WIDTH                 (PORT_AFI_ADDR_WIDTH),
         .PORT_AFI_BA_WIDTH                   (PORT_AFI_BA_WIDTH),
         .PORT_AFI_BG_WIDTH                   (PORT_AFI_BG_WIDTH),
         .PORT_AFI_C_WIDTH                    (PORT_AFI_C_WIDTH),
         .PORT_AFI_CKE_WIDTH                  (PORT_AFI_CKE_WIDTH),
         .PORT_AFI_CS_N_WIDTH                 (PORT_AFI_CS_N_WIDTH),
         .PORT_AFI_RM_WIDTH                   (PORT_AFI_RM_WIDTH),
         .PORT_AFI_ODT_WIDTH                  (PORT_AFI_ODT_WIDTH),
         .PORT_AFI_RAS_N_WIDTH                (PORT_AFI_RAS_N_WIDTH),
         .PORT_AFI_CAS_N_WIDTH                (PORT_AFI_CAS_N_WIDTH),
         .PORT_AFI_WE_N_WIDTH                 (PORT_AFI_WE_N_WIDTH),
         .PORT_AFI_RST_N_WIDTH                (PORT_AFI_RST_N_WIDTH),
         .PORT_AFI_ACT_N_WIDTH                (PORT_AFI_ACT_N_WIDTH),
         .PORT_AFI_PAR_WIDTH                  (PORT_AFI_PAR_WIDTH),
         .PORT_AFI_CA_WIDTH                   (PORT_AFI_CA_WIDTH),
         .PORT_AFI_REF_N_WIDTH                (PORT_AFI_REF_N_WIDTH),
         .PORT_AFI_WPS_N_WIDTH                (PORT_AFI_WPS_N_WIDTH),
         .PORT_AFI_RPS_N_WIDTH                (PORT_AFI_RPS_N_WIDTH),
         .PORT_AFI_DOFF_N_WIDTH               (PORT_AFI_DOFF_N_WIDTH),
         .PORT_AFI_LD_N_WIDTH                 (PORT_AFI_LD_N_WIDTH),
         .PORT_AFI_RW_N_WIDTH                 (PORT_AFI_RW_N_WIDTH),
         .PORT_AFI_LBK0_N_WIDTH               (PORT_AFI_LBK0_N_WIDTH),
         .PORT_AFI_LBK1_N_WIDTH               (PORT_AFI_LBK1_N_WIDTH),
         .PORT_AFI_CFG_N_WIDTH                (PORT_AFI_CFG_N_WIDTH),
         .PORT_AFI_AP_WIDTH                   (PORT_AFI_AP_WIDTH),
         .PORT_AFI_AINV_WIDTH                 (PORT_AFI_AINV_WIDTH),
         .PORT_AFI_DM_WIDTH                   (PORT_AFI_DM_WIDTH),
         .PORT_AFI_DM_N_WIDTH                 (PORT_AFI_DM_N_WIDTH),
         .PORT_AFI_BWS_N_WIDTH                (PORT_AFI_BWS_N_WIDTH),
         .PORT_AFI_RDATA_DBI_N_WIDTH          (PORT_AFI_RDATA_DBI_N_WIDTH),
         .PORT_AFI_WDATA_DBI_N_WIDTH          (PORT_AFI_WDATA_DBI_N_WIDTH),
         .PORT_AFI_RDATA_DINV_WIDTH           (PORT_AFI_RDATA_DINV_WIDTH),
         .PORT_AFI_WDATA_DINV_WIDTH           (PORT_AFI_WDATA_DINV_WIDTH),
         .PORT_AFI_DQS_BURST_WIDTH            (PORT_AFI_DQS_BURST_WIDTH),
         .PORT_AFI_WDATA_VALID_WIDTH          (PORT_AFI_WDATA_VALID_WIDTH),
         .PORT_AFI_WDATA_WIDTH                (PORT_AFI_WDATA_WIDTH),
         .PORT_AFI_RDATA_EN_FULL_WIDTH        (PORT_AFI_RDATA_EN_FULL_WIDTH),
         .PORT_AFI_RDATA_WIDTH                (PORT_AFI_RDATA_WIDTH),
         .PORT_AFI_RDATA_VALID_WIDTH          (PORT_AFI_RDATA_VALID_WIDTH),
         .PORT_AFI_RRANK_WIDTH                (PORT_AFI_RRANK_WIDTH),
         .PORT_AFI_WRANK_WIDTH                (PORT_AFI_WRANK_WIDTH),
         .PORT_AFI_ALERT_N_WIDTH              (PORT_AFI_ALERT_N_WIDTH),
         .PORT_AFI_PE_N_WIDTH                 (PORT_AFI_PE_N_WIDTH),
         .PORT_MEM_CK_WIDTH                   (PORT_MEM_CK_WIDTH),
         .PORT_MEM_CK_N_WIDTH                 (PORT_MEM_CK_N_WIDTH),
         .PORT_MEM_DK_WIDTH                   (PORT_MEM_DK_WIDTH),
         .PORT_MEM_DK_N_WIDTH                 (PORT_MEM_DK_N_WIDTH),
         .PORT_MEM_DKA_WIDTH                  (PORT_MEM_DKA_WIDTH),
         .PORT_MEM_DKA_N_WIDTH                (PORT_MEM_DKA_N_WIDTH),
         .PORT_MEM_DKB_WIDTH                  (PORT_MEM_DKB_WIDTH),
         .PORT_MEM_DKB_N_WIDTH                (PORT_MEM_DKB_N_WIDTH),
         .PORT_MEM_K_WIDTH                    (PORT_MEM_K_WIDTH),
         .PORT_MEM_K_N_WIDTH                  (PORT_MEM_K_N_WIDTH),
         .PORT_MEM_A_WIDTH                    (PORT_MEM_A_WIDTH),
         .PORT_MEM_BA_WIDTH                   (PORT_MEM_BA_WIDTH),
         .PORT_MEM_BG_WIDTH                   (PORT_MEM_BG_WIDTH),
         .PORT_MEM_C_WIDTH                    (PORT_MEM_C_WIDTH),
         .PORT_MEM_CKE_WIDTH                  (PORT_MEM_CKE_WIDTH),
         .PORT_MEM_CS_N_WIDTH                 (PORT_MEM_CS_N_WIDTH),
         .PORT_MEM_RM_WIDTH                   (PORT_MEM_RM_WIDTH),
         .PORT_MEM_ODT_WIDTH                  (PORT_MEM_ODT_WIDTH),
         .PORT_MEM_RAS_N_WIDTH                (PORT_MEM_RAS_N_WIDTH),
         .PORT_MEM_CAS_N_WIDTH                (PORT_MEM_CAS_N_WIDTH),
         .PORT_MEM_WE_N_WIDTH                 (PORT_MEM_WE_N_WIDTH),
         .PORT_MEM_RESET_N_WIDTH              (PORT_MEM_RESET_N_WIDTH),
         .PORT_MEM_ACT_N_WIDTH                (PORT_MEM_ACT_N_WIDTH),
         .PORT_MEM_PAR_WIDTH                  (PORT_MEM_PAR_WIDTH),
         .PORT_MEM_CA_WIDTH                   (PORT_MEM_CA_WIDTH),
         .PORT_MEM_REF_N_WIDTH                (PORT_MEM_REF_N_WIDTH),
         .PORT_MEM_WPS_N_WIDTH                (PORT_MEM_WPS_N_WIDTH),
         .PORT_MEM_RPS_N_WIDTH                (PORT_MEM_RPS_N_WIDTH),
         .PORT_MEM_DOFF_N_WIDTH               (PORT_MEM_DOFF_N_WIDTH),
         .PORT_MEM_LDA_N_WIDTH                (PORT_MEM_LDA_N_WIDTH),
         .PORT_MEM_LDB_N_WIDTH                (PORT_MEM_LDB_N_WIDTH),
         .PORT_MEM_RWA_N_WIDTH                (PORT_MEM_RWA_N_WIDTH),
         .PORT_MEM_RWB_N_WIDTH                (PORT_MEM_RWB_N_WIDTH),
         .PORT_MEM_LBK0_N_WIDTH               (PORT_MEM_LBK0_N_WIDTH),
         .PORT_MEM_LBK1_N_WIDTH               (PORT_MEM_LBK1_N_WIDTH),
         .PORT_MEM_CFG_N_WIDTH                (PORT_MEM_CFG_N_WIDTH),
         .PORT_MEM_AP_WIDTH                   (PORT_MEM_AP_WIDTH),
         .PORT_MEM_AINV_WIDTH                 (PORT_MEM_AINV_WIDTH),
         .PORT_MEM_DM_WIDTH                   (PORT_MEM_DM_WIDTH),
         .PORT_MEM_BWS_N_WIDTH                (PORT_MEM_BWS_N_WIDTH),
         .PORT_MEM_D_WIDTH                    (PORT_MEM_D_WIDTH),
         .PORT_MEM_DQ_WIDTH                   (PORT_MEM_DQ_WIDTH),
         .PORT_MEM_DBI_N_WIDTH                (PORT_MEM_DBI_N_WIDTH),
         .PORT_MEM_DQA_WIDTH                  (PORT_MEM_DQA_WIDTH),
         .PORT_MEM_DQB_WIDTH                  (PORT_MEM_DQB_WIDTH),
         .PORT_MEM_DINVA_WIDTH                (PORT_MEM_DINVA_WIDTH),
         .PORT_MEM_DINVB_WIDTH                (PORT_MEM_DINVB_WIDTH),
         .PORT_MEM_Q_WIDTH                    (PORT_MEM_Q_WIDTH),
         .PORT_MEM_DQS_WIDTH                  (PORT_MEM_DQS_WIDTH),
         .PORT_MEM_DQS_N_WIDTH                (PORT_MEM_DQS_N_WIDTH),
         .PORT_MEM_QK_WIDTH                   (PORT_MEM_QK_WIDTH),
         .PORT_MEM_QK_N_WIDTH                 (PORT_MEM_QK_N_WIDTH),
         .PORT_MEM_QKA_WIDTH                  (PORT_MEM_QKA_WIDTH),
         .PORT_MEM_QKA_N_WIDTH                (PORT_MEM_QKA_N_WIDTH),
         .PORT_MEM_QKB_WIDTH                  (PORT_MEM_QKB_WIDTH),
         .PORT_MEM_QKB_N_WIDTH                (PORT_MEM_QKB_N_WIDTH),
         .PORT_MEM_CQ_WIDTH                   (PORT_MEM_CQ_WIDTH),
         .PORT_MEM_CQ_N_WIDTH                 (PORT_MEM_CQ_N_WIDTH),
         .PORT_MEM_ALERT_N_WIDTH              (PORT_MEM_ALERT_N_WIDTH),
         .PORT_MEM_PE_N_WIDTH                 (PORT_MEM_PE_N_WIDTH),
         .PORT_MEM_CK_PINLOC                  (PORT_MEM_CK_PINLOC),
         .PORT_MEM_CK_N_PINLOC                (PORT_MEM_CK_N_PINLOC),
         .PORT_MEM_DK_PINLOC                  (PORT_MEM_DK_PINLOC),
         .PORT_MEM_DK_N_PINLOC                (PORT_MEM_DK_N_PINLOC),
         .PORT_MEM_DKA_PINLOC                 (PORT_MEM_DKA_PINLOC),
         .PORT_MEM_DKA_N_PINLOC               (PORT_MEM_DKA_N_PINLOC),
         .PORT_MEM_DKB_PINLOC                 (PORT_MEM_DKB_PINLOC),
         .PORT_MEM_DKB_N_PINLOC               (PORT_MEM_DKB_N_PINLOC),
         .PORT_MEM_K_PINLOC                   (PORT_MEM_K_PINLOC),
         .PORT_MEM_K_N_PINLOC                 (PORT_MEM_K_N_PINLOC),
         .PORT_MEM_A_PINLOC                   (PORT_MEM_A_PINLOC),
         .PORT_MEM_BA_PINLOC                  (PORT_MEM_BA_PINLOC),
         .PORT_MEM_BG_PINLOC                  (PORT_MEM_BG_PINLOC),
         .PORT_MEM_C_PINLOC                   (PORT_MEM_C_PINLOC),
         .PORT_MEM_CKE_PINLOC                 (PORT_MEM_CKE_PINLOC),
         .PORT_MEM_CS_N_PINLOC                (PORT_MEM_CS_N_PINLOC),
         .PORT_MEM_RM_PINLOC                  (PORT_MEM_RM_PINLOC),
         .PORT_MEM_ODT_PINLOC                 (PORT_MEM_ODT_PINLOC),
         .PORT_MEM_RAS_N_PINLOC               (PORT_MEM_RAS_N_PINLOC),
         .PORT_MEM_CAS_N_PINLOC               (PORT_MEM_CAS_N_PINLOC),
         .PORT_MEM_WE_N_PINLOC                (PORT_MEM_WE_N_PINLOC),
         .PORT_MEM_RESET_N_PINLOC             (PORT_MEM_RESET_N_PINLOC),
         .PORT_MEM_ACT_N_PINLOC               (PORT_MEM_ACT_N_PINLOC),
         .PORT_MEM_PAR_PINLOC                 (PORT_MEM_PAR_PINLOC),
         .PORT_MEM_CA_PINLOC                  (PORT_MEM_CA_PINLOC),
         .PORT_MEM_REF_N_PINLOC               (PORT_MEM_REF_N_PINLOC),
         .PORT_MEM_WPS_N_PINLOC               (PORT_MEM_WPS_N_PINLOC),
         .PORT_MEM_RPS_N_PINLOC               (PORT_MEM_RPS_N_PINLOC),
         .PORT_MEM_DOFF_N_PINLOC              (PORT_MEM_DOFF_N_PINLOC),
         .PORT_MEM_LDA_N_PINLOC               (PORT_MEM_LDA_N_PINLOC),
         .PORT_MEM_LDB_N_PINLOC               (PORT_MEM_LDB_N_PINLOC),
         .PORT_MEM_RWA_N_PINLOC               (PORT_MEM_RWA_N_PINLOC),
         .PORT_MEM_RWB_N_PINLOC               (PORT_MEM_RWB_N_PINLOC),
         .PORT_MEM_LBK0_N_PINLOC              (PORT_MEM_LBK0_N_PINLOC),
         .PORT_MEM_LBK1_N_PINLOC              (PORT_MEM_LBK1_N_PINLOC),
         .PORT_MEM_CFG_N_PINLOC               (PORT_MEM_CFG_N_PINLOC),
         .PORT_MEM_AP_PINLOC                  (PORT_MEM_AP_PINLOC),
         .PORT_MEM_AINV_PINLOC                (PORT_MEM_AINV_PINLOC),
         .PORT_MEM_DM_PINLOC                  (PORT_MEM_DM_PINLOC),
         .PORT_MEM_BWS_N_PINLOC               (PORT_MEM_BWS_N_PINLOC),
         .PORT_MEM_D_PINLOC                   (PORT_MEM_D_PINLOC),
         .PORT_MEM_DQ_PINLOC                  (PORT_MEM_DQ_PINLOC),
         .PORT_MEM_DBI_N_PINLOC               (PORT_MEM_DBI_N_PINLOC),
         .PORT_MEM_DQA_PINLOC                 (PORT_MEM_DQA_PINLOC),
         .PORT_MEM_DQB_PINLOC                 (PORT_MEM_DQB_PINLOC),
         .PORT_MEM_DINVA_PINLOC               (PORT_MEM_DINVA_PINLOC),
         .PORT_MEM_DINVB_PINLOC               (PORT_MEM_DINVB_PINLOC),
         .PORT_MEM_Q_PINLOC                   (PORT_MEM_Q_PINLOC),
         .PORT_MEM_DQS_PINLOC                 (PORT_MEM_DQS_PINLOC),
         .PORT_MEM_DQS_N_PINLOC               (PORT_MEM_DQS_N_PINLOC),
         .PORT_MEM_QK_PINLOC                  (PORT_MEM_QK_PINLOC),
         .PORT_MEM_QK_N_PINLOC                (PORT_MEM_QK_N_PINLOC),
         .PORT_MEM_QKA_PINLOC                 (PORT_MEM_QKA_PINLOC),
         .PORT_MEM_QKA_N_PINLOC               (PORT_MEM_QKA_N_PINLOC),
         .PORT_MEM_QKB_PINLOC                 (PORT_MEM_QKB_PINLOC),
         .PORT_MEM_QKB_N_PINLOC               (PORT_MEM_QKB_N_PINLOC),
         .PORT_MEM_CQ_PINLOC                  (PORT_MEM_CQ_PINLOC),
         .PORT_MEM_CQ_N_PINLOC                (PORT_MEM_CQ_N_PINLOC),
         .PORT_MEM_ALERT_N_PINLOC             (PORT_MEM_ALERT_N_PINLOC),
         .PORT_MEM_PE_N_PINLOC                (PORT_MEM_PE_N_PINLOC),
         .PINS_PER_LANE                       (PINS_PER_LANE),
         .LANES_PER_TILE                      (LANES_PER_TILE),
         .NUM_OF_RTL_TILES                    (NUM_OF_RTL_TILES),
         .LANES_USAGE                         (LANES_USAGE),
         .PRI_RDATA_TILE_INDEX                (PRI_RDATA_TILE_INDEX),
         .PRI_RDATA_LANE_INDEX                (PRI_RDATA_LANE_INDEX),
         .PRI_WDATA_TILE_INDEX                (PRI_WDATA_TILE_INDEX),
         .PRI_WDATA_LANE_INDEX                (PRI_WDATA_LANE_INDEX),
         .SEC_RDATA_TILE_INDEX                (SEC_RDATA_TILE_INDEX),
         .SEC_RDATA_LANE_INDEX                (SEC_RDATA_LANE_INDEX),
         .SEC_WDATA_TILE_INDEX                (SEC_WDATA_TILE_INDEX),
         .SEC_WDATA_LANE_INDEX                (SEC_WDATA_LANE_INDEX),
         .PINS_C2L_DRIVEN                     (PINS_C2L_DRIVEN),
         .PINS_INVERT_OE                      (PINS_INVERT_OE),
         .MEM_DATA_MASK_EN                    (MEM_DATA_MASK_EN),
         .PHY_HMC_CLK_RATIO                   (PHY_HMC_CLK_RATIO)
      ) if_inst (
         .*
      );

      assign amm_readdata_0 = '0;
      assign amm_readdata_1 = '0;
      assign ast_rd_data_0  = '0;
      assign ast_rd_data_1  = '0;

   end else
   begin : hmc
      if (HMC_AVL_PROTOCOL_ENUM == "CTRL_AVL_PROTOCOL_MM")
      begin : amm

         altera_emif_arch_nf_hmc_amm_data_if # (
            .PINS_PER_LANE                        (PINS_PER_LANE),
            .LANES_PER_TILE                       (LANES_PER_TILE),
            .NUM_OF_RTL_TILES                     (NUM_OF_RTL_TILES),
            .NUM_OF_HMC_PORTS                     (NUM_OF_HMC_PORTS),
            .PORT_CTRL_AMM_RDATA_WIDTH            (PORT_CTRL_AMM_RDATA_WIDTH),
            .PORT_CTRL_AMM_WDATA_WIDTH            (PORT_CTRL_AMM_WDATA_WIDTH),
            .PORT_CTRL_AMM_BYTEEN_WIDTH           (PORT_CTRL_AMM_BYTEEN_WIDTH),
            .PORT_MEM_D_PINLOC                    (PORT_MEM_D_PINLOC),
            .PORT_MEM_DQ_PINLOC                   (PORT_MEM_DQ_PINLOC),
            .PORT_MEM_Q_PINLOC                    (PORT_MEM_Q_PINLOC),
            .PORT_MEM_DM_PINLOC                   (PORT_MEM_DM_PINLOC),
            .PORT_MEM_DBI_N_PINLOC                (PORT_MEM_DBI_N_PINLOC),
            .PORT_MEM_BWS_N_PINLOC                (PORT_MEM_BWS_N_PINLOC),
            .PINS_C2L_DRIVEN                      (PINS_C2L_DRIVEN)
         ) data_if_inst (
            .*
         );

         assign ast_rd_data_0        = '0;
         assign ast_rd_data_1        = '0;
      end else
      begin : hmc_ast
         altera_emif_arch_nf_hmc_ast_data_if # (
            .PINS_PER_LANE                        (PINS_PER_LANE),
            .LANES_PER_TILE                       (LANES_PER_TILE),
            .NUM_OF_RTL_TILES                     (NUM_OF_RTL_TILES),
            .NUM_OF_HMC_PORTS                     (NUM_OF_HMC_PORTS),
            .PORT_CTRL_AST_WR_DATA_WIDTH          (PORT_CTRL_AST_WR_DATA_WIDTH),
            .PORT_CTRL_AST_RD_DATA_WIDTH          (PORT_CTRL_AST_RD_DATA_WIDTH),
            .PORT_MEM_D_PINLOC                    (PORT_MEM_D_PINLOC),
            .PORT_MEM_DQ_PINLOC                   (PORT_MEM_DQ_PINLOC),
            .PORT_MEM_Q_PINLOC                    (PORT_MEM_Q_PINLOC),
            .PORT_MEM_DM_PINLOC                   (PORT_MEM_DM_PINLOC),
            .PORT_MEM_DBI_N_PINLOC                (PORT_MEM_DBI_N_PINLOC),
            .PORT_MEM_BWS_N_PINLOC                (PORT_MEM_BWS_N_PINLOC),
            .PINS_C2L_DRIVEN                      (PINS_C2L_DRIVEN)
         ) data_if_inst (
            .*
         );

         assign amm_readdata_0 = '0;
         assign amm_readdata_1 = '0;
      end

      assign afi_rdata_dbi_n      = '0;
      assign afi_rdata_dinv       = '0;
      assign afi_rdata            = '0;
      assign afi_rdata_valid      = '0;
      assign afi_alert_n          = '0;
      assign afi_pe_n             = '0;
      assign core2l_rdata_en_full = '0;
      assign core2l_mrnk_read     = '0;
      assign core2l_mrnk_write    = '0;
   end
   endgenerate

   altera_emif_arch_nf_cal_counter # (
      .IS_HPS (IS_HPS)
   ) cal_counter_inst (
      .*
   );

   assign emif_to_hps          = '0;

endmodule

