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


(* altera_attribute = "-name AUTO_SHIFT_REGISTER_RECOGNITION OFF" *)

module altera_emif_arch_nf_oct #(
   parameter OCT_CONTROL_WIDTH = 1,
   parameter PLL_REF_CLK_FREQ_PS = 0,
   parameter PHY_CALIBRATED_OCT = 1,
   parameter PHY_USERMODE_OCT = 1,
   parameter PHY_PERIODIC_OCT_RECAL = 1,
   parameter PHY_CONFIG_ENUM = "CONFIG_PHY_AND_HARD_CTRL",
   parameter IS_HPS = 0
) (
   input  logic                          afi_clk,
   input  logic                          afi_reset_n,
   input  logic                          emif_usr_clk,
   input  logic                          emif_usr_reset_n,
   input  logic                          oct_cal_req,
   output logic                          oct_cal_rdy,
   output logic                          oct_recal_req,
   input  logic                          oct_s2pload_ena,
   output logic                          oct_s2pload_rdy,
   input  logic                          pll_ref_clk_int,
   input  logic                          phy_reset_n,

   input  logic                          oct_rzqin,  
   output logic [OCT_CONTROL_WIDTH-1:0]  oct_stc,    
   output logic [OCT_CONTROL_WIDTH-1:0]  oct_ptc     
);
   timeunit 1ns;
   timeprecision 1ps;

   typedef enum
   {
      ST_OCTFSM_RESET,           
      ST_OCTFSM_WAIT_IDLE,       
      ST_OCTFSM_WAIT_REQ_HI,     
      ST_OCTFSM_WAIT_BUSY_HI,    
      ST_OCTFSM_WAIT_BUSY_LO,    
      ST_OCTFSM_WAIT_REQ_LO,     
      ST_OCTFSM_DONE             
   } ST_OCTFSM;

   localparam OCT_CAL_MODE = "A_OCT_CAL_MODE_AUTO";
   localparam OCT_USER_OCT = (PHY_USERMODE_OCT == 1) ? "A_OCT_USER_OCT_ON" : "A_OCT_USER_OCT_OFF";
   localparam OCT_CLK_DIV_CNT_SHIFT = 4; 
   localparam OCT_S2P_HANDSHAKE = (PHY_PERIODIC_OCT_RECAL == 1) ? "true" : "false";

   localparam OCT_REFCLK_FREQ_MHZ = 1000000 / PLL_REF_CLK_FREQ_PS;
   localparam OCT_RECAL_INTERVAL_MS = 500;
   localparam OCT_RECAL_TIMER_PRESET = (OCT_RECAL_INTERVAL_MS * OCT_REFCLK_FREQ_MHZ * 1000 / (2 ** OCT_CLK_DIV_CNT_SHIFT)) - 1;
   localparam OCT_RECAL_TIMER_WIDTH = 32;

   generate
   if (PHY_CALIBRATED_OCT) begin : cal_oct

      if (PHY_USERMODE_OCT) begin : manual_oct_cal
         wire           w_clk;
         wire           w_oct_cal_request;
         wire           w_oct_clock;
         wire           w_oct_reset;
         wire           w_oct_cal_shift_busy;
         wire           w_oct_cal_busy;
         wire           w_oct_s2pload_ena;
         wire           w_oct_s2pload_rdy;
         wire  [15:0]   w_oct_0_ser_term_ctrl;
         wire  [15:0]   w_oct_0_par_term_ctrl;
         wire  [15:0]   w_oct_1_ser_term_ctrl;
         wire  [15:0]   w_oct_1_par_term_ctrl;
         wire  [15:0]   w_oct_2_ser_term_ctrl;
         wire  [15:0]   w_oct_2_par_term_ctrl;
         wire  [15:0]   w_oct_3_ser_term_ctrl;
         wire  [15:0]   w_oct_3_par_term_ctrl;
         wire  [15:0]   w_oct_4_ser_term_ctrl;
         wire  [15:0]   w_oct_4_par_term_ctrl;
         wire  [15:0]   w_oct_5_ser_term_ctrl;
         wire  [15:0]   w_oct_5_par_term_ctrl;
         wire  [15:0]   w_oct_6_ser_term_ctrl;
         wire  [15:0]   w_oct_6_par_term_ctrl;
         wire  [15:0]   w_oct_7_ser_term_ctrl;
         wire  [15:0]   w_oct_7_par_term_ctrl;
         wire  [15:0]   w_oct_8_ser_term_ctrl;
         wire  [15:0]   w_oct_8_par_term_ctrl;
         wire  [15:0]   w_oct_9_ser_term_ctrl;
         wire  [15:0]   w_oct_9_par_term_ctrl;
         wire  [15:0]   w_oct_10_ser_term_ctrl;
         wire  [15:0]   w_oct_10_par_term_ctrl;
         wire  [15:0]   w_oct_11_ser_term_ctrl;
         wire  [15:0]   w_oct_11_par_term_ctrl;

         (* altera_attribute = {"-name GLOBAL_SIGNAL OFF"}*) logic [2:0]    r_cal_req_metasync /* synthesis dont_merge syn_noprune syn_preserve = 1 */;
         (* altera_attribute = {"-name GLOBAL_SIGNAL OFF"}*) logic [2:0]    r_cal_rdy_metasync /* synthesis dont_merge syn_noprune syn_preserve = 1 */;
         (* altera_attribute = {"-name GLOBAL_SIGNAL OFF"}*) logic [2:0]    r_cal_rst_metasync /* synthesis dont_merge syn_noprune syn_preserve = 1 */;

         logic          r_cal_busy;

         ST_OCTFSM      r_octfsm_cs;
         ST_OCTFSM      c_octfsm_ns;
         logic          r_octfsm_rdy;
         logic          c_octfsm_rdy;
         logic          r_octfsm_req;
         logic          c_octfsm_req;

         reg  [7:0]     r_clkdiv_ctr;
         (* altera_attribute = {"-name GLOBAL_SIGNAL OFF"}*) reg r_clkdiv /* synthesis dont_merge syn_noprune syn_preserve = 1 */;

         altera_oct #(
            .OCT_CAL_NUM(1),
            .OCT_USER_MODE(OCT_USER_OCT),
            .OCT_S2P_HANDSHAKE(OCT_S2P_HANDSHAKE),
            .OCT_CAL_MODE_DER_0(OCT_CAL_MODE),
            .OCT_CKBUF_MODE("true"),
            .OCT_CAL_MODE_DER_1("A_OCT_CAL_MODE_SINGLE"),
            .OCT_CAL_MODE_DER_2("A_OCT_CAL_MODE_SINGLE"),
            .OCT_CAL_MODE_DER_3("A_OCT_CAL_MODE_SINGLE"),
            .OCT_CAL_MODE_DER_4("A_OCT_CAL_MODE_SINGLE"),
            .OCT_CAL_MODE_DER_5("A_OCT_CAL_MODE_SINGLE"),
            .OCT_CAL_MODE_DER_6("A_OCT_CAL_MODE_SINGLE"),
            .OCT_CAL_MODE_DER_7("A_OCT_CAL_MODE_SINGLE"),
            .OCT_CAL_MODE_DER_8("A_OCT_CAL_MODE_SINGLE"),
            .OCT_CAL_MODE_DER_9("A_OCT_CAL_MODE_SINGLE"),
            .OCT_CAL_MODE_DER_10("A_OCT_CAL_MODE_SINGLE"),
            .OCT_CAL_MODE_DER_11("A_OCT_CAL_MODE_SINGLE")
         ) oct_inst (
            .rzqin(oct_rzqin),
            .calibration_request(w_oct_cal_request),
            .clock(w_oct_clock),
            .reset(w_oct_reset),
            .calibration_shift_busy(w_oct_cal_shift_busy),
            .calibration_busy(w_oct_cal_busy),
            .s2pload_ena(w_oct_s2pload_ena),
            .s2pload_rdy(w_oct_s2pload_rdy),
            .oct_0_series_termination_control(w_oct_0_ser_term_ctrl),
            .oct_0_parallel_termination_control(w_oct_0_par_term_ctrl),
            .oct_1_series_termination_control(w_oct_1_ser_term_ctrl),
            .oct_1_parallel_termination_control(w_oct_1_par_term_ctrl),
            .oct_2_series_termination_control(w_oct_2_ser_term_ctrl),
            .oct_2_parallel_termination_control(w_oct_1_par_term_ctrl),
            .oct_3_series_termination_control(w_oct_3_ser_term_ctrl),
            .oct_3_parallel_termination_control(w_oct_1_par_term_ctrl),
            .oct_4_series_termination_control(w_oct_4_ser_term_ctrl),
            .oct_4_parallel_termination_control(w_oct_1_par_term_ctrl),
            .oct_5_series_termination_control(w_oct_5_ser_term_ctrl),
            .oct_5_parallel_termination_control(w_oct_1_par_term_ctrl),
            .oct_6_series_termination_control(w_oct_6_ser_term_ctrl),
            .oct_6_parallel_termination_control(w_oct_1_par_term_ctrl),
            .oct_7_series_termination_control(w_oct_7_ser_term_ctrl),
            .oct_7_parallel_termination_control(w_oct_1_par_term_ctrl),
            .oct_8_series_termination_control(w_oct_8_ser_term_ctrl),
            .oct_8_parallel_termination_control(w_oct_1_par_term_ctrl),
            .oct_9_series_termination_control(w_oct_9_ser_term_ctrl),
            .oct_9_parallel_termination_control(w_oct_1_par_term_ctrl),
            .oct_10_series_termination_control(w_oct_10_ser_term_ctrl),
            .oct_10_parallel_termination_control(w_oct_1_par_term_ctrl),
            .oct_11_series_termination_control(w_oct_11_ser_term_ctrl),
            .oct_11_parallel_termination_control(w_oct_1_par_term_ctrl)
         );

      initial
      begin
         r_clkdiv_ctr[7:0] <= 8'b0000_0000;
         r_clkdiv <= 1'b0;
      end
      always @(posedge pll_ref_clk_int)
      begin
         if (r_clkdiv_ctr[7:0] == ((1<<OCT_CLK_DIV_CNT_SHIFT)-1))
         begin
            r_clkdiv_ctr[7:0] <= {8{1'b0}};
         end
         else
         begin
            r_clkdiv_ctr[7:0] <= r_clkdiv_ctr[7:0] + 8'h01;
         end

         r_clkdiv <= r_clkdiv_ctr[OCT_CLK_DIV_CNT_SHIFT-1];
      end

      always_ff @(posedge w_oct_clock)
      begin
         r_cal_req_metasync[2:0] <= {r_cal_req_metasync[1:0], oct_cal_req};
      end

      always_ff @(posedge w_oct_clock)
      begin
         r_cal_busy <= w_oct_cal_shift_busy;
      end

      always_ff @(posedge w_clk)
      begin
         r_cal_rdy_metasync[2:0] <= {r_cal_rdy_metasync[1:0], r_octfsm_rdy};
      end

      always_ff @(posedge w_oct_clock)
      begin
         r_cal_rst_metasync[2:0] <= {r_cal_rst_metasync[1:0], phy_reset_n};
      end

      always_ff @(posedge w_oct_clock)
      begin
         if (w_oct_reset == 1'b1)
         begin
            r_octfsm_cs <= ST_OCTFSM_RESET;
            r_octfsm_rdy <= 1'b0;
            r_octfsm_req <= 1'b0;
         end
         else
         begin
            r_octfsm_cs <= c_octfsm_ns;
            r_octfsm_rdy <= c_octfsm_rdy;
            r_octfsm_req <= c_octfsm_req;
         end
      end
      always_comb
      begin
         c_octfsm_rdy <= 1'b0;

         case (r_octfsm_cs)
            ST_OCTFSM_RESET:
            begin
               c_octfsm_ns <= ST_OCTFSM_WAIT_IDLE;
               c_octfsm_rdy <= 1'b0;
               c_octfsm_req <= 1'b0;
            end

            ST_OCTFSM_WAIT_IDLE:
            begin
               c_octfsm_ns <= ((r_cal_busy == 1'b0) ? ST_OCTFSM_WAIT_REQ_HI : ST_OCTFSM_WAIT_IDLE);
               c_octfsm_rdy <= ((r_cal_busy == 1'b0) ? 1'b1 : 1'b0);
               c_octfsm_req <= 1'b0;
            end

            ST_OCTFSM_WAIT_REQ_HI:
            begin
               c_octfsm_ns <= ((r_cal_req_metasync[2] == 1'b1) ? ST_OCTFSM_WAIT_BUSY_HI : ST_OCTFSM_WAIT_REQ_HI);
               c_octfsm_rdy <= ((r_cal_req_metasync[2] == 1'b1) ? 1'b0 : 1'b1);
               c_octfsm_req <= 1'b0;
            end

            ST_OCTFSM_WAIT_BUSY_HI:
            begin
               c_octfsm_ns <= ((r_cal_busy == 1'b1) ? ST_OCTFSM_WAIT_BUSY_LO : ST_OCTFSM_WAIT_BUSY_HI);
               c_octfsm_rdy <= 1'b0;
               c_octfsm_req <= 1'b1;
            end

            ST_OCTFSM_WAIT_BUSY_LO:
            begin
               c_octfsm_ns <= ((r_cal_busy == 1'b0) ? ST_OCTFSM_WAIT_REQ_LO : ST_OCTFSM_WAIT_BUSY_LO);
               c_octfsm_rdy <= 1'b0;
               c_octfsm_req <= 1'b0;
            end

            ST_OCTFSM_WAIT_REQ_LO:
            begin
               c_octfsm_ns <= ((r_cal_req_metasync[2] == 1'b0) ? ST_OCTFSM_DONE : ST_OCTFSM_WAIT_REQ_LO);
               c_octfsm_rdy <= 1'b0;
               c_octfsm_req <= 1'b0;
            end

            ST_OCTFSM_DONE:
            begin
               c_octfsm_ns <= ST_OCTFSM_RESET;
               c_octfsm_rdy <= 1'b0;
               c_octfsm_req <= 1'b0;
            end

            default:
            begin
               c_octfsm_ns <= ST_OCTFSM_RESET;
               c_octfsm_rdy <= 1'b0;
               c_octfsm_req <= 1'b0;
            end

         endcase
      end

      assign w_oct_cal_request = r_octfsm_req;
      assign w_oct_clock       = r_clkdiv;
      assign w_oct_reset       =~r_cal_rst_metasync[2];

      if (IS_HPS == 1) begin : hps
         assign w_clk = w_oct_clock;
      end else if (PHY_CONFIG_ENUM == "CONFIG_PHY_AND_HARD_CTRL") begin : hmc
         assign w_clk = emif_usr_clk;
      end else begin : non_hmc
         assign w_clk = afi_clk;
      end
   
      assign oct_stc         = w_oct_0_ser_term_ctrl[15:0];
      assign oct_ptc         = w_oct_0_par_term_ctrl[15:0];
      assign oct_cal_rdy     = r_cal_rdy_metasync[2];


      if (PHY_PERIODIC_OCT_RECAL == 1) begin : gen_oct_recal_s2p_handshake
         (* altera_attribute = {"-name GLOBAL_SIGNAL OFF"}*) logic [2:0]    r_s2pload_ena_metasync  /* synthesis dont_merge syn_noprune syn_preserve = 1 */;
         (* altera_attribute = {"-name GLOBAL_SIGNAL OFF"}*) logic [2:0]    r_s2pload_rdy_metasync  /* synthesis dont_merge syn_noprune syn_preserve = 1 */;

         always_ff @(posedge w_oct_clock)
         begin
            r_s2pload_ena_metasync[2:0] <= {r_s2pload_ena_metasync[1:0], oct_s2pload_ena};
         end

         always_ff @(posedge w_clk)
         begin
            r_s2pload_rdy_metasync[2:0] <= {r_s2pload_rdy_metasync[1:0], w_oct_s2pload_rdy};
         end

         assign oct_s2pload_rdy = r_s2pload_rdy_metasync[2];
         assign w_oct_s2pload_ena = r_s2pload_ena_metasync[2];

      end else begin : gen_no_oct_recal_s2p_handshake
         assign oct_s2pload_rdy = 1'b1;         
         assign w_oct_s2pload_ena = 1'b1;
      end

      if (PHY_PERIODIC_OCT_RECAL == 1) begin : gen_oct_recal_timer
         logic [(OCT_RECAL_TIMER_WIDTH-1):0] r_oct_recal_timer;
         logic                               r_oct_recal_req;
         (* altera_attribute = {"-name GLOBAL_SIGNAL OFF"}*) logic [2:0]    r_oct_recal_req_metasync /* synthesis dont_merge syn_noprune syn_preserve = 1 */;
         
         always_ff @(posedge w_oct_clock)
         begin
            if (w_oct_reset == 1'b1)
            begin
               r_oct_recal_timer[(OCT_RECAL_TIMER_WIDTH-1)]   <= 1'b0;
               r_oct_recal_timer[(OCT_RECAL_TIMER_WIDTH-2):0] <= OCT_RECAL_TIMER_PRESET[(OCT_RECAL_TIMER_WIDTH-2):0];
               r_oct_recal_req                                <= 1'b0;
            end
            else
            begin
               if (r_oct_recal_timer[(OCT_RECAL_TIMER_WIDTH-1)] == 1'b1)
               begin
                  r_oct_recal_timer[(OCT_RECAL_TIMER_WIDTH-1)]   <= 1'b0;
                  r_oct_recal_timer[(OCT_RECAL_TIMER_WIDTH-2):0] <= OCT_RECAL_TIMER_PRESET[(OCT_RECAL_TIMER_WIDTH-2):0];
                  r_oct_recal_req                                <= ~r_oct_recal_req;
               end
               else
               begin
                  r_oct_recal_timer[(OCT_RECAL_TIMER_WIDTH-1):0] <= r_oct_recal_timer[(OCT_RECAL_TIMER_WIDTH-1):0]
                                                                  - {{(OCT_RECAL_TIMER_WIDTH-1){1'b0}}, 1'b1};
               end
            end
         end

         always_ff @(posedge w_clk)
         begin
            r_oct_recal_req_metasync[2:0] <= {r_oct_recal_req_metasync[1:0], r_oct_recal_req};
         end

         assign oct_recal_req = r_oct_recal_req_metasync[2];
      end else begin : gen_no_oct_cal_timer
         assign oct_recal_req = 1'b0;
      end
    

      end else begin : powerup_oct_cal
         logic oct_ser_data;
         logic oct_nclrusr;
         logic oct_enserusr;
         logic oct_clkenusr;
         logic oct_clkusr;
         logic oct_s2pload;
 
         assign oct_nclrusr  = 1'b0;
         assign oct_clkenusr = 1'b0;
         assign oct_clkusr   = 1'b0;
         assign oct_s2pload  = 1'b0;
         assign oct_enserusr = 1'b0;
         
         twentynm_termination #(
            .a_oct_cal_mode(OCT_CAL_MODE),
            .a_oct_user_oct(OCT_USER_OCT)
         )
         termination_inst (
            .rzqin(oct_rzqin),
            .enserusr(oct_enserusr),
            .serdataout(oct_ser_data),
            .nclrusr(oct_nclrusr),
            .clkenusr(oct_clkenusr),
            .clkusr(oct_clkusr)
         );

         twentynm_termination_logic termination_logic_inst (
            .s2pload(oct_s2pload),
            .serdata(oct_ser_data),
            .seriesterminationcontrol(oct_stc),
            .parallelterminationcontrol(oct_ptc)
         );
         
         
         assign oct_cal_rdy = 1'b0;
         assign oct_s2pload_rdy = 1'b1;
         assign oct_recal_req = 1'b0;
      end


   end else begin : non_cal_oct
      assign oct_cal_rdy = 1'b0;
      assign oct_s2pload_rdy = 1'b1;
      assign oct_recal_req = 1'b0;
      assign oct_stc = '0;
      assign oct_ptc = '0;
   end
   endgenerate
   
endmodule


