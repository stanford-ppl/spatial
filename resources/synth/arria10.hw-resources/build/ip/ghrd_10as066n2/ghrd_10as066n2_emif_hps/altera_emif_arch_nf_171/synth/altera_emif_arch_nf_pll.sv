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



////////////////////////////////////////////////////////////////////////////////////////////////////////////
//  EMIF IOPLL instantiation for 20nm families
//
//  The following table describes the usage of IOPLL by EMIF.
//
//  PLL Counter    Fanouts                          Usage
//  =====================================================================================
//  VCO Outputs    vcoph[7:0] -> phy_clk_phs[7:0]   FR clocks, 8 phases (45-deg apart)
//                 vcoph[0] -> DLL                  FR clock to DLL
//  C-counter 0    lvds_clk[0] -> phy_clk[1]        Secondary PHY clock tree (C2P/P2C rate)
//  C-counter 1    loaden[0] -> phy_clk[0]          Primary PHY clock tree (PHY/HMC rate)
//  C-counter 2    phy_clk[2]                       Feedback PHY clock tree (slowest phy clock in system)
//
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////
module altera_emif_arch_nf_pll #(
   parameter PORT_DFT_NF_PLL_CNTSEL_WIDTH            = 1,
   parameter PORT_DFT_NF_PLL_NUM_SHIFT_WIDTH         = 1,

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
   parameter PLL_C_CNT_OUT_EN_8                      = ""
) (
   input  logic                                               global_reset_n_int,    
   input  logic                                               pll_ref_clk_int,       
   output logic                                               pll_locked,            
   output logic                                               pll_dll_clk,           
   output logic [7:0]                                         phy_clk_phs,           
   output logic [1:0]                                         phy_clk,               
   output logic                                               phy_fb_clk_to_tile,    
   input  logic                                               phy_fb_clk_to_pll,     
   output logic [8:0]                                         pll_c_counters,        
   input  logic                                               pll_phase_en,          
   input  logic                                               pll_up_dn,             
   input  logic [PORT_DFT_NF_PLL_CNTSEL_WIDTH-1:0]            pll_cnt_sel,           
   input  logic [PORT_DFT_NF_PLL_NUM_SHIFT_WIDTH-1:0]         pll_num_phase_shifts,  
   output logic                                               pll_phase_done         
);
   timeunit 1ns;
   timeprecision 1ps;

   logic [7:0]        pll_vcoph;              
   logic [1:0]        pll_loaden;             
   logic [1:0]        pll_lvds_clk;           

   logic              pll_dprio_clk;
   logic              pll_dprio_rst_n;
   logic [8:0]        pll_dprio_address;
   logic              pll_dprio_read;
   logic [7:0]        pll_dprio_readdata;
   logic              pll_dprio_write;
   logic [7:0]        pll_dprio_writedata;
   logic              pll_reset_n;

   assign phy_clk_phs  = pll_vcoph;

   assign phy_clk[0]   = pll_loaden[0];       // C-cnt 1 drives phy_clk 0 through a delay chain (swapping is intentional)
   assign phy_clk[1]   = pll_lvds_clk[0];     // C-cnt 0 drives phy_clk 1 through a delay chain (swapping is intentional)

`ifdef ALTERA_A10_IOPLL_BOOTSTRAP
   localparam PHY_IOPLL_WORKAROUND_LOCAL = 1;
`else
   localparam PHY_IOPLL_WORKAROUND_LOCAL = 0;
`endif

`ifdef ALTERA_EMIF_ENABLE_ISSP
   altsource_probe #(
		.sld_auto_instance_index ("YES"),
		.sld_instance_index      (0),
		.instance_id             ("PLLL"),
		.probe_width             (1),
		.source_width            (0),
		.source_initial_value    ("0"),
		.enable_metastability    ("NO")
	) pll_lock_issp (
		.probe  (pll_locked)
	);

`endif

   twentynm_iopll # (

      ////////////////////////////////////
      //  VCO and Ref clock
      //  fVCO = fRefClk * M * CCnt2 / N
      ////////////////////////////////////
      .reference_clock_frequency                  (PLL_REF_CLK_FREQ_PS_STR),
      .vco_frequency                              (PLL_VCO_FREQ_PS_STR),

      .pll_vco_ph0_en                             ("true"),                        // vcoph[0] is required to drive phy_clk_phs[0]
      .pll_vco_ph1_en                             ("true"),                        // vcoph[1] is required to drive phy_clk_phs[1]
      .pll_vco_ph2_en                             ("true"),                        // vcoph[2] is required to drive phy_clk_phs[2]
      .pll_vco_ph3_en                             ("true"),                        // vcoph[3] is required to drive phy_clk_phs[3]
      .pll_vco_ph4_en                             ("true"),                        // vcoph[4] is required to drive phy_clk_phs[4]
      .pll_vco_ph5_en                             ("true"),                        // vcoph[5] is required to drive phy_clk_phs[5]
      .pll_vco_ph6_en                             ("true"),                        // vcoph[6] is required to drive phy_clk_phs[6]
      .pll_vco_ph7_en                             ("true"),                        // vcoph[7] is required to drive phy_clk_phs[7]

      ////////////////////////////////////
      //  Special clock selects
      ////////////////////////////////////
      .pll_dll_src                                ("pll_dll_src_ph0"),             // Use vcoph[0] as DLL input
      .pll_phyfb_mux                              ("lvds_tx_fclk"),                // PHY clock feedback path selector

      ////////////////////////////////////
      //  M Counter
      ////////////////////////////////////
      .pll_m_counter_bypass_en                    (PLL_M_CNT_BYPASS_EN),
      .pll_m_counter_even_duty_en                 (PLL_M_CNT_EVEN_DUTY_EN),
      .pll_m_counter_high                         (PLL_M_CNT_HIGH),
      .pll_m_counter_low                          (PLL_M_CNT_LOW),
      .pll_m_counter_ph_mux_prst                  (0),
      .pll_m_counter_prst                         (1),
      .pll_m_counter_coarse_dly                   ("0 ps"),
      .pll_m_counter_fine_dly                     ("0 ps"),
      .pll_m_counter_in_src                       (PLL_M_CNT_IN_SRC),              // Take VCO clock as input to M Counter

      ////////////////////////////////////
      //  N Counter (bypassed)
      ////////////////////////////////////
      .pll_n_counter_bypass_en                    (PLL_N_CNT_BYPASS_EN),
      .pll_n_counter_odd_div_duty_en              (PLL_N_CNT_EVEN_DUTY_EN),
      .pll_n_counter_high                         (PLL_N_CNT_HIGH),
      .pll_n_counter_low                          (PLL_N_CNT_LOW),
      .pll_n_counter_coarse_dly                   ("0 ps"),
      .pll_n_counter_fine_dly                     ("0 ps"),

      ////////////////////////////////////
      //  C Counter 0 (phy_clk[1])
      ////////////////////////////////////
      .pll_c0_out_en                              (PLL_C_CNT_OUT_EN_0),                      // C-counter driving phy_clk[1]
      .output_clock_frequency_0                   (PLL_C_CNT_FREQ_PS_STR_0),
      .phase_shift_0                              (PLL_C_CNT_PHASE_PS_STR_0),
      .duty_cycle_0                               (PLL_C_CNT_DUTY_CYCLE_0),
      .pll_c0_extclk_dllout_en                    ("true"),
      .pll_c_counter_0_bypass_en                  (PLL_C_CNT_BYPASS_EN_0),
      .pll_c_counter_0_even_duty_en               (PLL_C_CNT_EVEN_DUTY_EN_0),
      .pll_c_counter_0_high                       (PLL_C_CNT_HIGH_0),
      .pll_c_counter_0_low                        (PLL_C_CNT_LOW_0),
      .pll_c_counter_0_ph_mux_prst                (PLL_C_CNT_PH_MUX_PRST_0),
      .pll_c_counter_0_prst                       (PLL_C_CNT_PRST_0),
      .pll_c_counter_0_coarse_dly                 ("0 ps"),
      .pll_c_counter_0_fine_dly                   ("0 ps"),
      .pll_c_counter_0_in_src                     ("c_m_cnt_in_src_ph_mux_clk"),

      ////////////////////////////////////
      //  C Counter 1 (phy_clk[0])
      ////////////////////////////////////
      .pll_c1_out_en                              (PLL_C_CNT_OUT_EN_1),                      // C-counter driving phy_clk[0]
      .output_clock_frequency_1                   (PLL_C_CNT_FREQ_PS_STR_1),
      .phase_shift_1                              (PLL_C_CNT_PHASE_PS_STR_1),
      .duty_cycle_1                               (PLL_C_CNT_DUTY_CYCLE_1),
      .pll_c1_extclk_dllout_en                    ("true"),
      .pll_c_counter_1_bypass_en                  (PLL_C_CNT_BYPASS_EN_1),
      .pll_c_counter_1_even_duty_en               (PLL_C_CNT_EVEN_DUTY_EN_1),
      .pll_c_counter_1_high                       (PLL_C_CNT_HIGH_1),
      .pll_c_counter_1_low                        (PLL_C_CNT_LOW_1),
      .pll_c_counter_1_ph_mux_prst                (PLL_C_CNT_PH_MUX_PRST_1),
      .pll_c_counter_1_prst                       (PLL_C_CNT_PRST_1),
      .pll_c_counter_1_coarse_dly                 ("0 ps"),
      .pll_c_counter_1_fine_dly                   ("0 ps"),
      .pll_c_counter_1_in_src                     ("c_m_cnt_in_src_ph_mux_clk"),

      ////////////////////////////////////
      //  C Counter 2 (phy_clk[2])
      ////////////////////////////////////
      .pll_c2_out_en                              (PLL_C_CNT_OUT_EN_2),                      // C-counter driving phy_clk[2]
      .output_clock_frequency_2                   (PLL_C_CNT_FREQ_PS_STR_2),
      .phase_shift_2                              (PLL_C_CNT_PHASE_PS_STR_2),
      .duty_cycle_2                               (PLL_C_CNT_DUTY_CYCLE_2),
      .pll_c2_extclk_dllout_en                    ("true"),
      .pll_c_counter_2_bypass_en                  (PLL_C_CNT_BYPASS_EN_2),
      .pll_c_counter_2_even_duty_en               (PLL_C_CNT_EVEN_DUTY_EN_2),
      .pll_c_counter_2_high                       (PLL_C_CNT_HIGH_2),
      .pll_c_counter_2_low                        (PLL_C_CNT_LOW_2),
      .pll_c_counter_2_ph_mux_prst                (PLL_C_CNT_PH_MUX_PRST_2),
      .pll_c_counter_2_prst                       (PLL_C_CNT_PRST_2),
      .pll_c_counter_2_coarse_dly                 ("0 ps"),
      .pll_c_counter_2_fine_dly                   ("0 ps"),
      .pll_c_counter_2_in_src                     ("c_m_cnt_in_src_ph_mux_clk"),

      ////////////////////////////////////
      //  C Counter 3 (unused)
      ////////////////////////////////////
      .pll_c3_out_en                              (PLL_C_CNT_OUT_EN_3),                         // C-counter driving cal_slave_clk
      .output_clock_frequency_3                   (PLL_C_CNT_FREQ_PS_STR_3),
      .phase_shift_3                              (PLL_C_CNT_PHASE_PS_STR_3),
      .duty_cycle_3                               (PLL_C_CNT_DUTY_CYCLE_3),
      .pll_c_counter_3_bypass_en                  (PLL_C_CNT_BYPASS_EN_3),
      .pll_c_counter_3_even_duty_en               (PLL_C_CNT_EVEN_DUTY_EN_3),
      .pll_c_counter_3_high                       (PLL_C_CNT_HIGH_3),
      .pll_c_counter_3_low                        (PLL_C_CNT_LOW_3),
      .pll_c_counter_3_ph_mux_prst                (PLL_C_CNT_PH_MUX_PRST_3),
      .pll_c_counter_3_prst                       (PLL_C_CNT_PRST_3),
      .pll_c_counter_3_coarse_dly                 ("0 ps"),
      .pll_c_counter_3_fine_dly                   ("0 ps"),
      .pll_c_counter_3_in_src                     ("c_m_cnt_in_src_ph_mux_clk"),

      ////////////////////////////////////
      //  C Counter 4 (unused)
      ////////////////////////////////////
      .pll_c4_out_en                              (PLL_C_CNT_OUT_EN_4),                         // C-counter driving cal_master_clk
      .output_clock_frequency_4                   (PLL_C_CNT_FREQ_PS_STR_4),
      .phase_shift_4                              (PLL_C_CNT_PHASE_PS_STR_4),
      .duty_cycle_4                               (PLL_C_CNT_DUTY_CYCLE_4),
      .pll_c_counter_4_bypass_en                  (PLL_C_CNT_BYPASS_EN_4),
      .pll_c_counter_4_even_duty_en               (PLL_C_CNT_EVEN_DUTY_EN_4),
      .pll_c_counter_4_high                       (PLL_C_CNT_HIGH_4),
      .pll_c_counter_4_low                        (PLL_C_CNT_LOW_4),
      .pll_c_counter_4_ph_mux_prst                (PLL_C_CNT_PH_MUX_PRST_4),
      .pll_c_counter_4_prst                       (PLL_C_CNT_PRST_4),
      .pll_c_counter_4_coarse_dly                 ("0 ps"),
      .pll_c_counter_4_fine_dly                   ("0 ps"),
      .pll_c_counter_4_in_src                     ("c_m_cnt_in_src_ph_mux_clk"),

      ////////////////////////////////////
      //  C Counter 5 (unused)
      ////////////////////////////////////
      .pll_c5_out_en                              (PLL_C_CNT_OUT_EN_5),                         // Not used by EMIF
      .output_clock_frequency_5                   (PLL_C_CNT_FREQ_PS_STR_5),                    // Don't care (unused c-counter)
      .phase_shift_5                              (PLL_C_CNT_PHASE_PS_STR_5),                   // Don't care (unused c-counter)
      .duty_cycle_5                               (PLL_C_CNT_DUTY_CYCLE_5),                     // Don't care (unused c-counter)
      .pll_c_counter_5_bypass_en                  (PLL_C_CNT_BYPASS_EN_5),                      // Don't care (unused c-counter)
      .pll_c_counter_5_even_duty_en               (PLL_C_CNT_EVEN_DUTY_EN_5),                   // Don't care (unused c-counter)
      .pll_c_counter_5_high                       (PLL_C_CNT_HIGH_5),                           // Don't care (unused c-counter)
      .pll_c_counter_5_low                        (PLL_C_CNT_LOW_5),                            // Don't care (unused c-counter)
      .pll_c_counter_5_ph_mux_prst                (PLL_C_CNT_PH_MUX_PRST_5),                    // Don't care (unused c-counter)
      .pll_c_counter_5_prst                       (PLL_C_CNT_PRST_5),                           // Don't care (unused c-counter)
      .pll_c_counter_5_coarse_dly                 ("0 ps"),                                     // Don't care (unused c-counter)
      .pll_c_counter_5_fine_dly                   ("0 ps"),                                     // Don't care (unused c-counter)
      .pll_c_counter_5_in_src                     ("c_m_cnt_in_src_ph_mux_clk"),                // Don't care (unused c-counter)

      ////////////////////////////////////
      //  C Counter 6 (unused)
      ////////////////////////////////////
      .pll_c6_out_en                              (PLL_C_CNT_OUT_EN_6),                         // Not used by EMIF
      .output_clock_frequency_6                   (PLL_C_CNT_FREQ_PS_STR_6),                    // Don't care (unused c-counter)
      .phase_shift_6                              (PLL_C_CNT_PHASE_PS_STR_6),                   // Don't care (unused c-counter)
      .duty_cycle_6                               (PLL_C_CNT_DUTY_CYCLE_6),                     // Don't care (unused c-counter)
      .pll_c_counter_6_bypass_en                  (PLL_C_CNT_BYPASS_EN_6),                      // Don't care (unused c-counter)
      .pll_c_counter_6_even_duty_en               (PLL_C_CNT_EVEN_DUTY_EN_6),                   // Don't care (unused c-counter)
      .pll_c_counter_6_high                       (PLL_C_CNT_HIGH_6),                           // Don't care (unused c-counter)
      .pll_c_counter_6_low                        (PLL_C_CNT_LOW_6),                            // Don't care (unused c-counter)
      .pll_c_counter_6_ph_mux_prst                (PLL_C_CNT_PH_MUX_PRST_6),                    // Don't care (unused c-counter)
      .pll_c_counter_6_prst                       (PLL_C_CNT_PRST_6),                           // Don't care (unused c-counter)
      .pll_c_counter_6_coarse_dly                 ("0 ps"),                                     // Don't care (unused c-counter)
      .pll_c_counter_6_fine_dly                   ("0 ps"),                                     // Don't care (unused c-counter)
      .pll_c_counter_6_in_src                     ("c_m_cnt_in_src_ph_mux_clk"),                // Don't care (unused c-counter)

      ////////////////////////////////////
      //  C Counter 7 (unused)
      ////////////////////////////////////
      .pll_c7_out_en                              (PLL_C_CNT_OUT_EN_7),                         // Not used by EMIF
      .output_clock_frequency_7                   (PLL_C_CNT_FREQ_PS_STR_7),                    // Don't care (unused c-counter)
      .phase_shift_7                              (PLL_C_CNT_PHASE_PS_STR_7),                   // Don't care (unused c-counter)
      .duty_cycle_7                               (PLL_C_CNT_DUTY_CYCLE_7),                     // Don't care (unused c-counter)
      .pll_c_counter_7_bypass_en                  (PLL_C_CNT_BYPASS_EN_7),                      // Don't care (unused c-counter)
      .pll_c_counter_7_even_duty_en               (PLL_C_CNT_EVEN_DUTY_EN_7),                   // Don't care (unused c-counter)
      .pll_c_counter_7_high                       (PLL_C_CNT_HIGH_7),                           // Don't care (unused c-counter)
      .pll_c_counter_7_low                        (PLL_C_CNT_LOW_7),                            // Don't care (unused c-counter)
      .pll_c_counter_7_ph_mux_prst                (PLL_C_CNT_PH_MUX_PRST_7),                    // Don't care (unused c-counter)
      .pll_c_counter_7_prst                       (PLL_C_CNT_PRST_7),                           // Don't care (unused c-counter)
      .pll_c_counter_7_coarse_dly                 ("0 ps"),                                     // Don't care (unused c-counter)
      .pll_c_counter_7_fine_dly                   ("0 ps"),                                     // Don't care (unused c-counter)
      .pll_c_counter_7_in_src                     ("c_m_cnt_in_src_ph_mux_clk"),                // Don't care (unused c-counter)

      ////////////////////////////////////
      //  C Counter 8 (unused)
      ////////////////////////////////////
      .pll_c8_out_en                              (PLL_C_CNT_OUT_EN_8),                         // Not used by EMIF
      .output_clock_frequency_8                   (PLL_C_CNT_FREQ_PS_STR_8),                    // Don't care (unused c-counter)
      .phase_shift_8                              (PLL_C_CNT_PHASE_PS_STR_8),                   // Don't care (unused c-counter)
      .duty_cycle_8                               (PLL_C_CNT_DUTY_CYCLE_8),                     // Don't care (unused c-counter)
      .pll_c_counter_8_bypass_en                  (PLL_C_CNT_BYPASS_EN_8),                      // Don't care (unused c-counter)
      .pll_c_counter_8_even_duty_en               (PLL_C_CNT_EVEN_DUTY_EN_8),                   // Don't care (unused c-counter)
      .pll_c_counter_8_high                       (PLL_C_CNT_HIGH_8),                           // Don't care (unused c-counter)
      .pll_c_counter_8_low                        (PLL_C_CNT_LOW_8),                            // Don't care (unused c-counter)
      .pll_c_counter_8_ph_mux_prst                (PLL_C_CNT_PH_MUX_PRST_8),                    // Don't care (unused c-counter)
      .pll_c_counter_8_prst                       (PLL_C_CNT_PRST_8),                           // Don't care (unused c-counter)
      .pll_c_counter_8_coarse_dly                 ("0 ps"),                                     // Don't care (unused c-counter)
      .pll_c_counter_8_fine_dly                   ("0 ps"),                                     // Don't care (unused c-counter)
      .pll_c_counter_8_in_src                     ("c_m_cnt_in_src_ph_mux_clk"),                // Don't care (unused c-counter)

      ////////////////////////////////////
      //  Misc Delay Chains
      ////////////////////////////////////
      .pll_ref_buf_dly                            ("0 ps"),
      .pll_cmp_buf_dly                            ("0 ps"),

      .pll_dly_0_enable                           ("true"),                        // Controls whether delay chain on phyclk[0] is enabled, must be true for phyclk to toggle
      .pll_dly_1_enable                           ("true"),                        // Controls whether delay chain on phyclk[1] is enabled, must be true for phyclk to toggle
      .pll_dly_2_enable                           ("true"),                        // Controls whether delay chain on phyclk[2] is enabled
      .pll_dly_3_enable                           ("true"),                        // Controls whether delay chain on phyclk[3] is enabled

      .pll_coarse_dly_0                           ("0 ps"),                        // Fine delay chain to skew phyclk[0]
      .pll_coarse_dly_1                           ("0 ps"),                        // Fine delay chain to skew phyclk[1]
      .pll_coarse_dly_2                           ("0 ps"),                        // Fine delay chain to skew phyclk[2]
      .pll_coarse_dly_3                           ("0 ps"),                        // Fine delay chain to skew phyclk[3]

      .pll_fine_dly_0                             ("0 ps"),                        // Fine delay chain to skew phyclk[0]
      .pll_fine_dly_1                             ("0 ps"),                        // Fine delay chain to skew phyclk[1]
      .pll_fine_dly_2                             ("0 ps"),                        // Fine delay chain to skew phyclk[2]
      .pll_fine_dly_3                             ("0 ps"),                        // Fine delay chain to skew phyclk[3]

      ////////////////////////////////////
      //  Misc PLL Modes and Features
      ////////////////////////////////////
      .pll_enable                                 ("true"),                        // Enable PLL
      .pll_powerdown_mode                         ("false"),                       // PLL power down mode
      .is_cascaded_pll                            ("false"),                       // EMIF assumes non-cascaded PLL for optimal jitter

      .compensation_mode                          ("emif"),                        // EMIF doesn't need PLL compensation. Alignment of core clocks and PHY clocks is handled by CPA
      .pll_fbclk_mux_1                            (PLL_FBCLK_MUX_1),               // Setting required by DIRECT compensation
      .pll_fbclk_mux_2                            (PLL_FBCLK_MUX_2),               // Setting required by DIRECT compensation

      .pll_extclk_0_enable                        ("false"),                       // EMIF PLL does not need to drive output clock pin
      .pll_extclk_1_enable                        ("false"),                       // EMIF PLL does not need to drive output clock pin

      .pll_clkin_0_src                            ("pll_clkin_0_src_refclkin"),    //
      .pll_clkin_1_src                            ("pll_clkin_1_src_refclkin"),    //
      .pll_sw_refclk_src                          ("pll_sw_refclk_src_clk_0"),     //
      .pll_auto_clk_sw_en                         ("false"),                       // EMIF PLL does not use the automatic clock switch-over feature
      .pll_clk_loss_sw_en                         ("false"),                       // EMIF PLL does not use the automatic clock switch-over feature
      .pll_manu_clk_sw_en                         ("false"),                       // EMIF PLL does not use the automatic clock switch-over feature
      .pll_ctrl_override_setting                  ("true"),
      .pll_cp_compensation                        ("false"),

      .bw_sel                                     (PLL_BW_SEL),                    // Bandwidth select
      .pll_bwctrl                                 (PLL_BW_CTRL),                   // Bandwidth control
      .pll_cp_current_setting                     (PLL_CP_SETTING),                // Charge pump setting
      .pll_unlock_fltr_cfg                        (2),

      .pll_dprio_broadcast_en                     ("false"),
      .pll_dprio_cvp_inter_sel                    ("false"),
      .pll_dprio_force_inter_sel                  ("false"),
      .pll_dprio_power_iso_en                     ("false")

   ) pll_inst (

      .refclk                                     (4'b0000),
      .rst_n                                      (pll_reset_n),
      .loaden                                     (pll_loaden),
      .lvds_clk                                   (pll_lvds_clk),
      .vcoph                                      (pll_vcoph),
      .fblvds_in                                  (phy_fb_clk_to_pll),
      .fblvds_out                                 (phy_fb_clk_to_tile),
      .dll_output                                 (pll_dll_clk),
      .lock                                       (pll_locked),
      .outclk                                     (pll_c_counters),
      .fbclk_in                                   (1'b0),
      .fbclk_out                                  (),
      .zdb_in                                     (1'b0),
      .phase_done                                 (pll_phase_done),
      .pll_cascade_in                             (pll_ref_clk_int),
      .pll_cascade_out                            (),
      .extclk_output                              (),
      .core_refclk                                (1'b0),
      .dps_rst_n                                  (1'b1),
      .mdio_dis                                   (1'b0),
      .pfden                                      (1'b1),
      .phase_en                                   (pll_phase_en),
      .pma_csr_test_dis                           (1'b1),
      .up_dn                                      (pll_up_dn),
      .extswitch                                  (1'b0),
      .clken                                      (2'b00),                            // Don't care (extclk)
      .cnt_sel                                    (pll_cnt_sel),
      .num_phase_shifts                           (pll_num_phase_shifts),
      .clk0_bad                                   (),
      .clk1_bad                                   (),
      .clksel                                     (),
      .csr_clk                                    (1'b1),
      .csr_en                                     (1'b1),
      .csr_in                                     (1'b1),
      .csr_out                                    (),
      .dprio_clk                                  (pll_dprio_clk),
      .dprio_rst_n                                (pll_dprio_rst_n),
      .dprio_address                              (pll_dprio_address),
      .scan_mode_n                                (1'b1),
      .scan_shift_n                               (1'b1),
      .write                                      (pll_dprio_write),
      .read                                       (pll_dprio_read),
      .readdata                                   (pll_dprio_readdata),
      .writedata                                  (pll_dprio_writedata),
      .extclk_dft                                 (),
      .block_select                               (),
      .lf_reset                                   (),
      .pipeline_global_en_n                       (),
      .pll_pd                                     (),
      .vcop_en                                    (),
      .user_mode                                  (1'b1)
   );

   generate
      if (PHY_IOPLL_WORKAROUND_LOCAL != 1)
      begin  : gen_pll_dprio_tieoff
         assign pll_dprio_clk            = 1'b0;
         assign pll_dprio_rst_n          = 1'b1;
         assign pll_dprio_address[8:0]   = 9'b0_0000_0000;
         assign pll_dprio_read           = 1'b0;
         assign pll_dprio_write          = 1'b0;
         assign pll_dprio_writedata[7:0] = 8'b0000_0000;
         assign pll_reset_n              = global_reset_n_int;
      end
      else
      begin : gen_pll_dprio
         iopll_bootstrap
         #(
            .PLL_CTR_RESYNC(1),
            .PLL_AUTO_RESET_ON_LOSS_OF_LOCK(0)
          )
         inst_iopll_bootstrap
         (
            .u_dprio_clk(1'b0),
            .u_dprio_rst_n(1'b1),
            .u_dprio_address({9{1'b0}}),
            .u_dprio_read(1'b0),
            .u_dprio_write(1'b0),
            .u_dprio_writedata({8{1'b0}}),
            .u_rst_n(global_reset_n_int),
            .pll_locked(pll_locked),
            .pll_dprio_readdata(pll_dprio_readdata),

            .pll_dprio_clk(pll_dprio_clk),
            .pll_dprio_rst_n(pll_dprio_rst_n),
            .pll_dprio_address(pll_dprio_address),
            .pll_dprio_read(pll_dprio_read),
            .pll_dprio_write(pll_dprio_write),
            .pll_dprio_writedata(pll_dprio_writedata),
            .pll_rst_n(pll_reset_n),
            .u_locked()
         );
      end
   endgenerate

endmodule

