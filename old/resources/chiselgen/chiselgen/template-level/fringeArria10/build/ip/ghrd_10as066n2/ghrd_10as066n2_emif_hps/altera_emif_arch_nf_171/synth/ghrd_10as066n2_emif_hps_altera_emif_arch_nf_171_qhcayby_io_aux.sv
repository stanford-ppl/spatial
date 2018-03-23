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



module ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_io_aux #(
   // Device parameters
   parameter SILICON_REV                             = "",
   parameter IS_HPS                                  = 0,
   parameter SEQ_CODE_HEX_FILENAME                   = "",

   // Synthesis Parameters
   parameter SEQ_SYNTH_OSC_FREQ_MHZ                  = 800,
   parameter SEQ_SYNTH_PARAMS_HEX_FILENAME           = "",
   parameter SEQ_SYNTH_CPU_CLK_DIVIDE                = 0,
   parameter SEQ_SYNTH_CAL_CLK_DIVIDE                = 0,

   // Simulation Parameters
   parameter SEQ_SIM_OSC_FREQ_MHZ                    = 800,
   parameter SEQ_SIM_PARAMS_HEX_FILENAME             = "",
   parameter SEQ_SIM_CPU_CLK_DIVIDE                  = 0,
   parameter SEQ_SIM_CAL_CLK_DIVIDE                  = 0,

   // Debug Parameters
   parameter DIAG_SYNTH_FOR_SIM                      = 0,
   parameter DIAG_ECLIPSE_DEBUG                      = 0,
   parameter DIAG_EXPORT_VJI                         = 0,
   parameter DIAG_INTERFACE_ID                       = 0,
   parameter DIAG_VERBOSE_IOAUX                      = 0,

   // Port widths for core debug access
   parameter PORT_CAL_DEBUG_ADDRESS_WIDTH            = 1,
   parameter PORT_CAL_DEBUG_BYTEEN_WIDTH             = 1,
   parameter PORT_CAL_DEBUG_RDATA_WIDTH              = 1,
   parameter PORT_CAL_DEBUG_WDATA_WIDTH              = 1,
   parameter PORT_CAL_MASTER_ADDRESS_WIDTH           = 1,
   parameter PORT_CAL_MASTER_BYTEEN_WIDTH            = 1,
   parameter PORT_CAL_MASTER_RDATA_WIDTH             = 1,
   parameter PORT_CAL_MASTER_WDATA_WIDTH             = 1,
   parameter PORT_DFT_NF_IOAUX_PIO_IN_WIDTH          = 1,
   parameter PORT_DFT_NF_IOAUX_PIO_OUT_WIDTH         = 1
) (
   input  logic        global_reset_n_int,
   output logic        cal_bus_clk,
   output logic        cal_bus_avl_read,
   output logic        cal_bus_avl_write,
   output logic [19:0] cal_bus_avl_address,
   input  logic [31:0] cal_bus_avl_read_data,
   output logic [31:0] cal_bus_avl_write_data,

   // Toolkit/On-Chip Debug Access
   input  logic [PORT_CAL_DEBUG_ADDRESS_WIDTH-1:0] cal_debug_addr,
   input  logic [PORT_CAL_DEBUG_BYTEEN_WIDTH-1:0]  cal_debug_byteenable,
   input  logic                                    cal_debug_clk,
   input  logic                                    cal_debug_read,
   input  logic                                    cal_debug_reset_n,
   input  logic                                    cal_debug_write,
   input  logic [PORT_CAL_DEBUG_WDATA_WIDTH-1:0]   cal_debug_write_data,
   output logic [PORT_CAL_DEBUG_RDATA_WIDTH-1:0]   cal_debug_read_data,
   output logic                                    cal_debug_read_data_valid,
   output logic                                    cal_debug_waitrequest,
   
   input  logic                                    cal_slave_clk_in,
   input  logic                                    cal_slave_reset_n_in,
   

   // Avalon Master to core
   output  logic [PORT_CAL_MASTER_ADDRESS_WIDTH-1:0]   cal_master_addr,
   output  logic [PORT_CAL_MASTER_BYTEEN_WIDTH-1:0]    cal_master_byteenable,
   output  logic                                       cal_master_burstcount,
   output  logic                                       cal_master_debugaccess,
   output  logic                                       cal_master_read,
   output  logic                                       cal_master_write,
   output  logic [PORT_CAL_MASTER_WDATA_WIDTH-1:0]     cal_master_write_data,
   input   logic [PORT_CAL_MASTER_RDATA_WIDTH-1:0]     cal_master_read_data,
   input   logic                                       cal_master_read_data_valid,
   input   logic                                       cal_master_waitrequest,

   // Toolkit/On-Chip Debug connection to next interface in column
   output logic [PORT_CAL_DEBUG_ADDRESS_WIDTH-1:0] cal_debug_out_addr,
   output logic [PORT_CAL_DEBUG_BYTEEN_WIDTH-1:0]  cal_debug_out_byteenable,
   output logic                                    cal_debug_out_clk,
   output logic                                    cal_debug_out_read,
   output logic                                    cal_debug_out_reset_n,
   output logic                                    cal_debug_out_write,
   output logic [PORT_CAL_DEBUG_WDATA_WIDTH-1:0]   cal_debug_out_write_data,
   input  logic [PORT_CAL_DEBUG_RDATA_WIDTH-1:0]   cal_debug_out_read_data,
   input  logic                                    cal_debug_out_read_data_valid,
   input  logic                                    cal_debug_out_waitrequest,

   // Internal test and debug
   input  logic [PORT_DFT_NF_IOAUX_PIO_IN_WIDTH-1:0]          ioaux_pio_in,
   output logic [PORT_DFT_NF_IOAUX_PIO_OUT_WIDTH-1:0]         ioaux_pio_out
);
   timeunit 1ns;
   timeprecision 1ps;

   // Derive localparam values
   // The following is evaluated for simulation
   // synthesis translate_off
   localparam SEQ_PARAMS_HEX_FILENAME  = SEQ_SIM_PARAMS_HEX_FILENAME;
   localparam SEQ_CPU_CLK_DIVIDE       = SEQ_SIM_CPU_CLK_DIVIDE;
   localparam SEQ_CAL_CLK_DIVIDE       = SEQ_SIM_CAL_CLK_DIVIDE;
   localparam SEQ_OSC_FREQ_MHZ         = SEQ_SIM_OSC_FREQ_MHZ;
   // synthesis translate_on

   // The following is evaluated for synthesis.
   // Typically we synthesize full-calibration behavior for hardware,
   // except when DIAG_SYNTH_FOR_SIM is set, which allows flows such
   // as post-fit simulation to adopt RTL simulation behavior.
   // synthesis read_comments_as_HDL on
   // localparam SEQ_PARAMS_HEX_FILENAME  = DIAG_SYNTH_FOR_SIM ? SEQ_SIM_PARAMS_HEX_FILENAME : SEQ_SYNTH_PARAMS_HEX_FILENAME;
   // localparam SEQ_CPU_CLK_DIVIDE       = DIAG_SYNTH_FOR_SIM ? SEQ_SIM_CPU_CLK_DIVIDE      : SEQ_SYNTH_CPU_CLK_DIVIDE;
   // localparam SEQ_CAL_CLK_DIVIDE       = DIAG_SYNTH_FOR_SIM ? SEQ_SIM_CAL_CLK_DIVIDE      : SEQ_SYNTH_CAL_CLK_DIVIDE;
   // localparam SEQ_OSC_FREQ_MHZ         = DIAG_SYNTH_FOR_SIM ? SEQ_SIM_OSC_FREQ_MHZ        : SEQ_SYNTH_OSC_FREQ_MHZ;
   // synthesis read_comments_as_HDL off

   wire             w_core_clk;
   wire             w_debug_clk;
   wire    [ 3: 0]  w_debug_select;
   wire             w_mcu_en;
   wire             w_mode;
   wire    [31: 0]  w_uc_read_data;
   wire             w_usrmode;
   wire    [21: 0]  w_debug_out;
   wire    [ 8: 0]  w_soft_nios_ctl_sig_bidir_out;
   wire    [19: 0]  w_uc_address;
   wire             w_uc_av_bus_clk;
   wire             w_uc_read;
   wire             w_uc_write;
   wire    [31: 0]  w_uc_write_data;

   assign cal_bus_clk                     = w_uc_av_bus_clk;
   assign cal_bus_avl_read                = w_uc_read;
   assign cal_bus_avl_write               = w_uc_write;
   assign cal_bus_avl_write_data[31: 0]   = w_uc_write_data[31: 0];
   assign cal_bus_avl_address[19: 0]      = w_uc_address[19: 0];
   assign w_uc_read_data[31: 0]           = cal_bus_avl_read_data[31: 0];
   assign w_core_clk                      = 1'b0;

   assign cal_master_debugaccess        = 1'b0;
   twentynm_io_aux # (
      .silicon_rev(SILICON_REV),
      .sys_clk_source("int_osc_clk"),
      .config_hps(IS_HPS ? "true" : "false"),
      .config_io_aux_bypass("false"),
      .config_power_down("false"),
      .config_ram(38'h306420c0),
      .config_spare(8'h00),
      .nios_break_vector_word_addr(16'h8200),
      .nios_exception_vector_word_addr(16'h0008),
      .nios_reset_vector_word_addr(16'h0000),
      .simulation_osc_freq_mhz(SEQ_OSC_FREQ_MHZ),
      .sys_clk_div(SEQ_CPU_CLK_DIVIDE),
      .cal_clk_div(SEQ_CAL_CLK_DIVIDE),
      .nios_code_hex_file(SEQ_CODE_HEX_FILENAME),
      .parameter_table_hex_file (SEQ_PARAMS_HEX_FILENAME),
      .interface_id(DIAG_INTERFACE_ID),
      .verbose_ioaux(DIAG_VERBOSE_IOAUX ? "true" : "false")
   ) io_aux (
      .core_clk(w_core_clk),
      .core_usr_reset_n(global_reset_n_int),
      .debug_clk(w_debug_clk),
      .debug_select(w_debug_select),
      .mcu_en(w_mcu_en),
      .mode(w_mode),
      .soft_nios_addr(cal_debug_addr),
   // synthesis translate_off
   //This is needed to allow simulation of core logic driving the cal bus.
      .soft_nios_burstcount(1'b1),
   // synthesis translate_on
      .soft_nios_byteenable(cal_debug_byteenable),
      .soft_nios_clk(cal_debug_clk),
      .soft_nios_read(cal_debug_read),
      .soft_nios_reset_n(cal_debug_reset_n),
      .soft_nios_write(cal_debug_write),
      .soft_nios_write_data(cal_debug_write_data),
      .soft_ram_clk(cal_slave_clk_in),
      .soft_ram_reset_n(cal_slave_reset_n_in),
      .soft_ram_read_data(cal_master_read_data),
      .soft_ram_rdata_valid(cal_master_read_data_valid),
      .soft_ram_waitrequest(cal_master_waitrequest),
      .uc_read_data(w_uc_read_data),
      .usrmode(w_usrmode),
      .vji_cdr_to_the_hard_nios(),
      .vji_ir_in_to_the_hard_nios(),
      .vji_rti_to_the_hard_nios(),
      .vji_sdr_to_the_hard_nios(),
      .vji_tck_to_the_hard_nios(),
      .vji_tdi_to_the_hard_nios(),
      .vji_udr_to_the_hard_nios(),
      .vji_uir_to_the_hard_nios(),
      .debug_out(w_debug_out),
      .soft_nios_read_data(cal_debug_read_data),
      .soft_nios_read_data_valid(cal_debug_read_data_valid),
      .soft_nios_waitrequest(cal_debug_waitrequest),
      .soft_ram_addr(cal_master_addr),
      .soft_ram_burstcount(cal_master_burstcount),
      .soft_ram_byteenable(cal_master_byteenable),
      .soft_ram_debugaccess(),
      .soft_ram_read(cal_master_read),
      .soft_ram_rst_n(),
      .soft_ram_write(cal_master_write),
      .soft_ram_write_data(cal_master_write_data),
      .uc_address(w_uc_address),
      .uc_av_bus_clk(w_uc_av_bus_clk),
      .uc_read(w_uc_read),
      .uc_write(w_uc_write),
      .uc_write_data(w_uc_write_data),
      .vji_ir_out_from_the_hard_nios(),
      .vji_tdo_from_the_hard_nios(),
      .soft_nios_out_addr(cal_debug_out_addr),
      .soft_nios_out_burstcount(),
      .soft_nios_out_byteenable(cal_debug_out_byteenable),
      .soft_nios_out_clk(cal_debug_out_clk),
      .soft_nios_out_read(cal_debug_out_read),
      .soft_nios_out_reset_n(cal_debug_out_reset_n),
      .soft_nios_out_write(cal_debug_out_write),
      .soft_nios_out_write_data(cal_debug_out_write_data),
      .soft_nios_out_read_data(cal_debug_out_read_data),
      .soft_nios_out_read_data_valid(cal_debug_out_read_data_valid),
      .soft_nios_out_waitrequest(cal_debug_out_waitrequest),
      .pio_in(ioaux_pio_in),
      .pio_out(ioaux_pio_out)
   );

   // Debug print
   // synthesis translate_off
   string debug_str = "";
   logic [31:0] chars;
   always @ (posedge w_uc_av_bus_clk) begin
      if (w_uc_address == 20'h1_0000 && w_uc_write) begin
         chars = w_uc_write_data;

         while (chars[7:0] != 8'b0) begin
            debug_str = {debug_str, string'(chars[7:0])};
            chars = chars >> 8;
         end

         if ((w_uc_write_data & 32'hff) == 32'b0 ||
             (w_uc_write_data & 32'hff00) == 32'b0 ||
             (w_uc_write_data & 32'hff0000) == 32'b0 ||
             (w_uc_write_data & 32'hff000000) == 32'b0 ) begin
               $display("%s", debug_str);
               debug_str = "";
         end
      end
   end
   // synthesis translate_on

endmodule

