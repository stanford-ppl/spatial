`ifdef RANDOMIZE_GARBAGE_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_INVALID_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_REG_INIT
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_MEM_INIT
`define RANDOMIZE
`endif

module Streaminner(
  input         clock,
  input         reset,
  input         io_input_enable,
  input         io_input_ctr_done,
  input         io_input_forever,
  input         io_input_rst,
  input         io_input_hasStreamIns,
  input  [31:0] io_input_nextState,
  input  [31:0] io_input_initState,
  input         io_input_doneCondition,
  output        io_output_done,
  output        io_output_ctr_en,
  output        io_output_ctr_inc,
  output        io_output_rst_en,
  output [31:0] io_output_state
);
  reg  _T_32;
  reg [31:0] _GEN_4;
  wire  _T_35;
  wire  _T_36;
  wire  _T_40;
  reg  _GEN_0;
  reg [31:0] _GEN_5;
  reg  _GEN_1;
  reg [31:0] _GEN_6;
  reg  _GEN_2;
  reg [31:0] _GEN_7;
  reg [31:0] _GEN_3;
  reg [31:0] _GEN_8;
  assign io_output_done = _T_40;
  assign io_output_ctr_en = _GEN_0;
  assign io_output_ctr_inc = _GEN_1;
  assign io_output_rst_en = _GEN_2;
  assign io_output_state = _GEN_3;
  assign _T_35 = io_input_hasStreamIns ? 1'h1 : io_input_enable;
  assign _T_36 = io_input_ctr_done & _T_35;
  assign _T_40 = io_input_forever ? 1'h0 : _T_36;
`ifdef RANDOMIZE
  integer initvar;
  initial begin
    `ifndef verilator
      #0.002 begin end
    `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_4 = {1{$random}};
  _T_32 = _GEN_4[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_5 = {1{$random}};
  _GEN_0 = _GEN_5[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_6 = {1{$random}};
  _GEN_1 = _GEN_6[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_7 = {1{$random}};
  _GEN_2 = _GEN_7[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_8 = {1{$random}};
  _GEN_3 = _GEN_8[31:0];
  `endif
  end
`endif
  always @(posedge clock) begin
    if (reset) begin
      _T_32 <= 1'h0;
    end
  end
endmodule
module SRFF_sp(
  input   clock,
  input   reset,
  input   io_input_set,
  input   io_input_reset,
  input   io_input_asyn_reset,
  output  io_output_data
);
  reg  _T_14;
  reg [31:0] _GEN_0;
  wire  _T_18;
  wire  _T_19;
  wire  _T_20;
  wire  _T_22;
  assign io_output_data = _T_22;
  assign _T_18 = io_input_reset ? 1'h0 : _T_14;
  assign _T_19 = io_input_set ? 1'h1 : _T_18;
  assign _T_20 = io_input_asyn_reset ? 1'h0 : _T_19;
  assign _T_22 = io_input_asyn_reset ? 1'h0 : _T_14;
`ifdef RANDOMIZE
  integer initvar;
  initial begin
    `ifndef verilator
      #0.002 begin end
    `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_0 = {1{$random}};
  _T_14 = _GEN_0[0:0];
  `endif
  end
`endif
  always @(posedge clock) begin
    if (reset) begin
      _T_14 <= 1'h0;
    end else begin
      if (io_input_asyn_reset) begin
        _T_14 <= 1'h0;
      end else begin
        if (io_input_set) begin
          _T_14 <= 1'h1;
        end else begin
          if (io_input_reset) begin
            _T_14 <= 1'h0;
          end
        end
      end
    end
  end
endmodule
module AccelTop(
  input         clock,
  input         reset,
  input         io_enable,
  output        io_done,
  input  [23:0] io_stream_in_data,
  input         io_stream_in_startofpacket,
  input         io_stream_in_endofpacket,
  input  [1:0]  io_stream_in_empty,
  input         io_stream_in_valid,
  input         io_stream_out_ready,
  output        io_stream_in_ready,
  output [15:0] io_stream_out_data,
  output        io_stream_out_startofpacket,
  output        io_stream_out_endofpacket,
  output        io_stream_out_empty,
  output        io_stream_out_valid,
  output [31:0] io_led_stream_out_data,
  input  [31:0] io_switch_stream_in_data
);
  wire  x264_ready;
  wire  x264_valid;
  wire  x265_ready;
  wire  x265_valid;
  wire  x266_ready;
  wire  x266_valid;
  wire  RootController_ctr_en;
  wire  RootController_datapath_en;
  wire [15:0] x274_tuple;
  wire [14:0] x282_tuple;
  wire [15:0] x266_data;
  wire [15:0] converted_data;
  wire  _T_80;
  wire  _T_81;
  wire  _T_83;
  wire  _T_84;
  wire [15:0] _GEN_1;
  wire  _GEN_2;
  wire  _GEN_3;
  wire [1:0] _GEN_4;
  wire  _T_86;
  wire  _T_87;
  wire  _T_88;
  wire  _T_89;
  wire  RootController_en;
  wire  RootController_sm_clock;
  wire  RootController_sm_reset;
  wire  RootController_sm_io_input_enable;
  wire  RootController_sm_io_input_ctr_done;
  wire  RootController_sm_io_input_forever;
  wire  RootController_sm_io_input_rst;
  wire  RootController_sm_io_input_hasStreamIns;
  wire [31:0] RootController_sm_io_input_nextState;
  wire [31:0] RootController_sm_io_input_initState;
  wire  RootController_sm_io_input_doneCondition;
  wire  RootController_sm_io_output_done;
  wire  RootController_sm_io_output_ctr_en;
  wire  RootController_sm_io_output_ctr_inc;
  wire  RootController_sm_io_output_rst_en;
  wire [31:0] RootController_sm_io_output_state;
  wire  _T_90;
  wire  _T_91;
  reg  _T_96;
  reg [31:0] _GEN_20;
  wire  done_latch_clock;
  wire  done_latch_reset;
  wire  done_latch_io_input_set;
  wire  done_latch_io_input_reset;
  wire  done_latch_io_input_asyn_reset;
  wire  done_latch_io_output_data;
  wire  _T_107;
  wire  x270;
  wire [4:0] x271_apply;
  wire [5:0] x272_apply;
  wire [4:0] x273_apply;
  wire [10:0] _T_116;
  wire [15:0] _T_117;
  wire [7:0] x275_number;
  wire  x275_debug_overflow;
  wire [7:0] x276_number;
  wire  x276_debug_overflow;
  wire [8:0] _T_131_number;
  wire  _T_131_debug_overflow;
  wire [8:0] _T_137_number;
  wire  _T_137_debug_overflow;
  wire [8:0] _T_143_number;
  wire  _T_143_debug_overflow;
  wire  _T_149;
  wire  _T_151;
  wire [8:0] _T_153;
  wire [8:0] _T_158;
  wire  _T_162;
  wire  _T_164;
  wire [8:0] _T_166;
  wire [8:0] _T_171;
  wire [9:0] _T_172;
  wire [8:0] _T_173;
  wire [7:0] x277_sumx275_x276_number;
  wire  x277_sumx275_x276_debug_overflow;
  wire  _T_182;
  wire  _T_184;
  wire [7:0] _T_186;
  wire  _T_189;
  wire [7:0] _T_190;
  wire [7:0] x278_number;
  wire  x278_debug_overflow;
  wire [8:0] _T_199_number;
  wire  _T_199_debug_overflow;
  wire [8:0] _T_205_number;
  wire  _T_205_debug_overflow;
  wire [8:0] _T_211_number;
  wire  _T_211_debug_overflow;
  wire  _T_217;
  wire  _T_219;
  wire [8:0] _T_221;
  wire [8:0] _T_226;
  wire  _T_230;
  wire  _T_232;
  wire [8:0] _T_234;
  wire [8:0] _T_239;
  wire [9:0] _T_240;
  wire [8:0] _T_241;
  wire [7:0] x279_sumx277_x278_number;
  wire  x279_sumx277_x278_debug_overflow;
  wire  _T_250;
  wire  _T_252;
  wire [7:0] _T_254;
  wire  _T_257;
  wire [7:0] _T_258;
  wire [31:0] _T_263_number;
  wire  _T_263_debug_overflow;
  wire [31:0] _GEN_5;
  wire [7:0] _T_266;
  wire [31:0] x280_number;
  wire  x280_debug_overflow;
  wire [4:0] x281_number;
  wire  x281_debug_overflow;
  wire [9:0] _T_280;
  wire [14:0] _T_281;
  wire [15:0] x283;
  reg [31:0] _GEN_0;
  reg [31:0] _GEN_21;
  reg [31:0] _GEN_6;
  reg [31:0] _GEN_22;
  reg  _GEN_7;
  reg [31:0] _GEN_23;
  reg  _GEN_8;
  reg [31:0] _GEN_24;
  reg  _GEN_9;
  reg [31:0] _GEN_25;
  reg  _GEN_10;
  reg [31:0] _GEN_26;
  reg  _GEN_11;
  reg [31:0] _GEN_27;
  reg  _GEN_12;
  reg [31:0] _GEN_28;
  reg  _GEN_13;
  reg [31:0] _GEN_29;
  reg  _GEN_14;
  reg [31:0] _GEN_30;
  reg  _GEN_15;
  reg [31:0] _GEN_31;
  reg  _GEN_16;
  reg [31:0] _GEN_32;
  reg  _GEN_17;
  reg [31:0] _GEN_33;
  reg  _GEN_18;
  reg [31:0] _GEN_34;
  reg  _GEN_19;
  reg [31:0] _GEN_35;
  Streaminner RootController_sm (
    .clock(RootController_sm_clock),
    .reset(RootController_sm_reset),
    .io_input_enable(RootController_sm_io_input_enable),
    .io_input_ctr_done(RootController_sm_io_input_ctr_done),
    .io_input_forever(RootController_sm_io_input_forever),
    .io_input_rst(RootController_sm_io_input_rst),
    .io_input_hasStreamIns(RootController_sm_io_input_hasStreamIns),
    .io_input_nextState(RootController_sm_io_input_nextState),
    .io_input_initState(RootController_sm_io_input_initState),
    .io_input_doneCondition(RootController_sm_io_input_doneCondition),
    .io_output_done(RootController_sm_io_output_done),
    .io_output_ctr_en(RootController_sm_io_output_ctr_en),
    .io_output_ctr_inc(RootController_sm_io_output_ctr_inc),
    .io_output_rst_en(RootController_sm_io_output_rst_en),
    .io_output_state(RootController_sm_io_output_state)
  );
  SRFF_sp done_latch (
    .clock(done_latch_clock),
    .reset(done_latch_reset),
    .io_input_set(done_latch_io_input_set),
    .io_input_reset(done_latch_io_input_reset),
    .io_input_asyn_reset(done_latch_io_input_asyn_reset),
    .io_output_data(done_latch_io_output_data)
  );
  assign io_done = done_latch_io_output_data;
  assign io_stream_in_ready = x265_ready;
  assign io_stream_out_data = _GEN_1;
  assign io_stream_out_startofpacket = _GEN_2;
  assign io_stream_out_endofpacket = _GEN_3;
  assign io_stream_out_empty = _GEN_4[0];
  assign io_stream_out_valid = x266_valid;
  assign io_led_stream_out_data = {{31'd0}, io_stream_in_ready};
  assign x264_ready = _T_107;
  assign x264_valid = 1'h1;
  assign x265_ready = _T_107;
  assign x265_valid = io_stream_in_valid;
  assign x266_ready = io_stream_out_ready;
  assign x266_valid = _T_107;
  assign RootController_ctr_en = RootController_sm_io_output_done;
  assign RootController_datapath_en = _T_91;
  assign x274_tuple = _T_117;
  assign x282_tuple = _T_281;
  assign x266_data = x283;
  assign converted_data = x266_data;
  assign _T_80 = ~ io_stream_out_valid;
  assign _T_81 = io_stream_out_ready | _T_80;
  assign _T_83 = reset == 1'h0;
  assign _T_84 = _T_83 & _T_81;
  assign _GEN_1 = _T_84 ? converted_data : 16'h0;
  assign _GEN_2 = _T_84 ? io_stream_in_startofpacket : 1'h0;
  assign _GEN_3 = _T_84 ? io_stream_in_endofpacket : 1'h0;
  assign _GEN_4 = _T_84 ? io_stream_in_empty : 2'h0;
  assign _T_86 = io_done == 1'h0;
  assign _T_87 = io_enable & _T_86;
  assign _T_88 = _T_87 & x266_ready;
  assign _T_89 = _T_88 & x265_valid;
  assign RootController_en = _T_89 & x264_valid;
  assign RootController_sm_clock = clock;
  assign RootController_sm_reset = reset;
  assign RootController_sm_io_input_enable = RootController_en;
  assign RootController_sm_io_input_ctr_done = _T_96;
  assign RootController_sm_io_input_forever = 1'h1;
  assign RootController_sm_io_input_rst = reset;
  assign RootController_sm_io_input_hasStreamIns = 1'h1;
  assign RootController_sm_io_input_nextState = _GEN_0;
  assign RootController_sm_io_input_initState = _GEN_6;
  assign RootController_sm_io_input_doneCondition = _GEN_7;
  assign _T_90 = ~ RootController_ctr_en;
  assign _T_91 = RootController_en & _T_90;
  assign done_latch_clock = clock;
  assign done_latch_reset = reset;
  assign done_latch_io_input_set = RootController_ctr_en;
  assign done_latch_io_input_reset = reset;
  assign done_latch_io_input_asyn_reset = reset;
  assign _T_107 = RootController_en & RootController_datapath_en;
  assign x270 = 32'h4 < io_switch_stream_in_data;
  assign x271_apply = io_stream_in_data[23:19];
  assign x272_apply = io_stream_in_data[15:10];
  assign x273_apply = io_stream_in_data[7:3];
  assign _T_116 = {x271_apply,x272_apply};
  assign _T_117 = {_T_116,x273_apply};
  assign x275_number = {{3'd0}, x271_apply};
  assign x275_debug_overflow = _GEN_8;
  assign x276_number = {{2'd0}, x272_apply};
  assign x276_debug_overflow = _GEN_9;
  assign _T_131_number = _T_173;
  assign _T_131_debug_overflow = _GEN_10;
  assign _T_137_number = _T_153;
  assign _T_137_debug_overflow = _GEN_11;
  assign _T_143_number = _T_166;
  assign _T_143_debug_overflow = _GEN_12;
  assign _T_149 = 1'h0;
  assign _T_151 = _T_149;
  assign _T_153 = _T_158;
  assign _T_158 = {1'h0,x275_number};
  assign _T_162 = 1'h0;
  assign _T_164 = _T_162;
  assign _T_166 = _T_171;
  assign _T_171 = {1'h0,x276_number};
  assign _T_172 = _T_137_number + _T_143_number;
  assign _T_173 = _T_172[8:0];
  assign x277_sumx275_x276_number = _T_186;
  assign x277_sumx275_x276_debug_overflow = _T_189;
  assign _T_182 = 1'h0;
  assign _T_184 = _T_182;
  assign _T_186 = _T_190;
  assign _T_189 = _T_131_number[8];
  assign _T_190 = _T_131_number[7:0];
  assign x278_number = {{3'd0}, x273_apply};
  assign x278_debug_overflow = _GEN_13;
  assign _T_199_number = _T_241;
  assign _T_199_debug_overflow = _GEN_14;
  assign _T_205_number = _T_221;
  assign _T_205_debug_overflow = _GEN_15;
  assign _T_211_number = _T_234;
  assign _T_211_debug_overflow = _GEN_16;
  assign _T_217 = 1'h0;
  assign _T_219 = _T_217;
  assign _T_221 = _T_226;
  assign _T_226 = {1'h0,x277_sumx275_x276_number};
  assign _T_230 = 1'h0;
  assign _T_232 = _T_230;
  assign _T_234 = _T_239;
  assign _T_239 = {1'h0,x278_number};
  assign _T_240 = _T_205_number + _T_211_number;
  assign _T_241 = _T_240[8:0];
  assign x279_sumx277_x278_number = _T_254;
  assign x279_sumx277_x278_debug_overflow = _T_257;
  assign _T_250 = 1'h0;
  assign _T_252 = _T_250;
  assign _T_254 = _T_258;
  assign _T_257 = _T_199_number[8];
  assign _T_258 = _T_199_number[7:0];
  assign _T_263_number = 32'h3;
  assign _T_263_debug_overflow = _GEN_17;
  assign _GEN_5 = {{24'd0}, x279_sumx277_x278_number};
  assign _T_266 = _GEN_5 / _T_263_number;
  assign x280_number = {{24'd0}, _T_266};
  assign x280_debug_overflow = _GEN_18;
  assign x281_number = x280_number[4:0];
  assign x281_debug_overflow = _GEN_19;
  assign _T_280 = {x281_number,x281_number};
  assign _T_281 = {_T_280,x281_number};
  assign x283 = x270 ? x274_tuple : {{1'd0}, x282_tuple};
`ifdef RANDOMIZE
  integer initvar;
  initial begin
    `ifndef verilator
      #0.002 begin end
    `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_20 = {1{$random}};
  _T_96 = _GEN_20[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_21 = {1{$random}};
  _GEN_0 = _GEN_21[31:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_22 = {1{$random}};
  _GEN_6 = _GEN_22[31:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_23 = {1{$random}};
  _GEN_7 = _GEN_23[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_24 = {1{$random}};
  _GEN_8 = _GEN_24[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_25 = {1{$random}};
  _GEN_9 = _GEN_25[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_26 = {1{$random}};
  _GEN_10 = _GEN_26[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_27 = {1{$random}};
  _GEN_11 = _GEN_27[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_28 = {1{$random}};
  _GEN_12 = _GEN_28[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_29 = {1{$random}};
  _GEN_13 = _GEN_29[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_30 = {1{$random}};
  _GEN_14 = _GEN_30[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_31 = {1{$random}};
  _GEN_15 = _GEN_31[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_32 = {1{$random}};
  _GEN_16 = _GEN_32[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_33 = {1{$random}};
  _GEN_17 = _GEN_33[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_34 = {1{$random}};
  _GEN_18 = _GEN_34[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_35 = {1{$random}};
  _GEN_19 = _GEN_35[0:0];
  `endif
  end
`endif
  always @(posedge clock) begin
    if (reset) begin
      _T_96 <= 1'h0;
    end else begin
      _T_96 <= _T_91;
    end
  end
endmodule
module FF(
  input         clock,
  input         reset,
  input  [63:0] io_in,
  input  [63:0] io_init,
  output [63:0] io_out,
  input         io_enable
);
  wire [63:0] d;
  reg [63:0] ff;
  reg [63:0] _GEN_0;
  wire  _T_13;
  wire [63:0] _GEN_1;
  assign io_out = ff;
  assign d = _GEN_1;
  assign _T_13 = io_enable == 1'h0;
  assign _GEN_1 = _T_13 ? ff : io_in;
`ifdef RANDOMIZE
  integer initvar;
  initial begin
    `ifndef verilator
      #0.002 begin end
    `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_0 = {2{$random}};
  ff = _GEN_0[63:0];
  `endif
  end
`endif
  always @(posedge clock) begin
    if (reset) begin
      ff <= io_init;
    end else begin
      ff <= d;
    end
  end
endmodule
module MuxN(
  input         clock,
  input         reset,
  input  [63:0] io_ins_0,
  input  [63:0] io_ins_1,
  input         io_sel,
  output [63:0] io_out
);
  wire [63:0] _GEN_0;
  wire [63:0] _GEN_1;
  assign io_out = _GEN_0;
  assign _GEN_0 = _GEN_1;
  assign _GEN_1 = io_sel ? io_ins_1 : io_ins_0;
endmodule
module RegFile(
  input         clock,
  input         reset,
  input         io_raddr,
  input         io_wen,
  input         io_waddr,
  input  [63:0] io_wdata,
  output [63:0] io_rdata,
  output [63:0] io_argIns_0,
  output [63:0] io_argIns_1,
  output        io_argOuts_0_ready,
  input         io_argOuts_0_valid,
  input  [63:0] io_argOuts_0_bits
);
  wire  regs_0_clock;
  wire  regs_0_reset;
  wire [63:0] regs_0_io_in;
  wire [63:0] regs_0_io_init;
  wire [63:0] regs_0_io_out;
  wire  regs_0_io_enable;
  wire  _T_50;
  wire  _T_51;
  wire  regs_1_clock;
  wire  regs_1_reset;
  wire [63:0] regs_1_io_in;
  wire [63:0] regs_1_io_init;
  wire [63:0] regs_1_io_out;
  wire  regs_1_io_enable;
  wire  _T_55;
  wire  _T_59;
  wire [63:0] _T_63;
  wire  rport_clock;
  wire  rport_reset;
  wire [63:0] rport_io_ins_0;
  wire [63:0] rport_io_ins_1;
  wire  rport_io_sel;
  wire [63:0] rport_io_out;
  wire [63:0] regOuts_0;
  wire [63:0] regOuts_1;
  wire [63:0] _T_73_0;
  wire [63:0] _T_73_1;
  reg  _GEN_0;
  reg [31:0] _GEN_1;
  FF regs_0 (
    .clock(regs_0_clock),
    .reset(regs_0_reset),
    .io_in(regs_0_io_in),
    .io_init(regs_0_io_init),
    .io_out(regs_0_io_out),
    .io_enable(regs_0_io_enable)
  );
  FF regs_1 (
    .clock(regs_1_clock),
    .reset(regs_1_reset),
    .io_in(regs_1_io_in),
    .io_init(regs_1_io_init),
    .io_out(regs_1_io_out),
    .io_enable(regs_1_io_enable)
  );
  MuxN rport (
    .clock(rport_clock),
    .reset(rport_reset),
    .io_ins_0(rport_io_ins_0),
    .io_ins_1(rport_io_ins_1),
    .io_sel(rport_io_sel),
    .io_out(rport_io_out)
  );
  assign io_rdata = rport_io_out;
  assign io_argIns_0 = _T_73_0;
  assign io_argIns_1 = _T_73_1;
  assign io_argOuts_0_ready = _GEN_0;
  assign regs_0_clock = clock;
  assign regs_0_reset = reset;
  assign regs_0_io_in = io_wdata;
  assign regs_0_io_init = 64'h0;
  assign regs_0_io_enable = _T_51;
  assign _T_50 = io_waddr == 1'h0;
  assign _T_51 = io_wen & _T_50;
  assign regs_1_clock = clock;
  assign regs_1_reset = reset;
  assign regs_1_io_in = _T_63;
  assign regs_1_io_init = 64'h0;
  assign regs_1_io_enable = _T_59;
  assign _T_55 = io_wen & io_waddr;
  assign _T_59 = _T_55 ? _T_55 : io_argOuts_0_valid;
  assign _T_63 = _T_55 ? io_wdata : io_argOuts_0_bits;
  assign rport_clock = clock;
  assign rport_reset = reset;
  assign rport_io_ins_0 = regOuts_0;
  assign rport_io_ins_1 = regOuts_1;
  assign rport_io_sel = io_raddr;
  assign regOuts_0 = regs_0_io_out;
  assign regOuts_1 = regs_1_io_out;
  assign _T_73_0 = regOuts_0;
  assign _T_73_1 = regOuts_1;
`ifdef RANDOMIZE
  integer initvar;
  initial begin
    `ifndef verilator
      #0.002 begin end
    `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_1 = {1{$random}};
  _GEN_0 = _GEN_1[0:0];
  `endif
  end
`endif
endmodule
module FF_2(
  input   clock,
  input   reset,
  input   io_in,
  input   io_init,
  output  io_out,
  input   io_enable
);
  wire  d;
  reg  ff;
  reg [31:0] _GEN_0;
  wire  _T_13;
  wire  _GEN_1;
  assign io_out = ff;
  assign d = _GEN_1;
  assign _T_13 = io_enable == 1'h0;
  assign _GEN_1 = _T_13 ? ff : io_in;
`ifdef RANDOMIZE
  integer initvar;
  initial begin
    `ifndef verilator
      #0.002 begin end
    `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_0 = {1{$random}};
  ff = _GEN_0[0:0];
  `endif
  end
`endif
  always @(posedge clock) begin
    if (reset) begin
      ff <= io_init;
    end else begin
      ff <= d;
    end
  end
endmodule
module Depulser(
  input   clock,
  input   reset,
  input   io_in,
  input   io_rst,
  output  io_out
);
  wire  r_clock;
  wire  r_reset;
  wire  r_io_in;
  wire  r_io_init;
  wire  r_io_out;
  wire  r_io_enable;
  wire  _T_9;
  wire  _T_11;
  FF_2 r (
    .clock(r_clock),
    .reset(r_reset),
    .io_in(r_io_in),
    .io_init(r_io_init),
    .io_out(r_io_out),
    .io_enable(r_io_enable)
  );
  assign io_out = r_io_out;
  assign r_clock = clock;
  assign r_reset = reset;
  assign r_io_in = _T_9;
  assign r_io_init = 1'h0;
  assign r_io_enable = _T_11;
  assign _T_9 = io_rst ? 1'h0 : io_in;
  assign _T_11 = io_in | io_rst;
endmodule
module FIFOArbiter(
  input         clock,
  input         reset,
  output [63:0] io_deq_0,
  output [63:0] io_deq_1,
  output [63:0] io_deq_2,
  output [63:0] io_deq_3,
  output [63:0] io_deq_4,
  output [63:0] io_deq_5,
  output [63:0] io_deq_6,
  output [63:0] io_deq_7,
  output [63:0] io_deq_8,
  output [63:0] io_deq_9,
  output [63:0] io_deq_10,
  output [63:0] io_deq_11,
  output [63:0] io_deq_12,
  output [63:0] io_deq_13,
  output [63:0] io_deq_14,
  output [63:0] io_deq_15,
  input         io_deqVld,
  output        io_empty,
  output        io_forceTag_ready,
  input         io_forceTag_valid,
  input         io_forceTag_bits,
  output        io_tag,
  input         io_config_chainWrite,
  input         io_config_chainRead
);
  wire  tagFF_clock;
  wire  tagFF_reset;
  wire  tagFF_io_in;
  wire  tagFF_io_init;
  wire  tagFF_io_out;
  wire  tagFF_io_enable;
  wire [63:0] _T_162_0;
  wire [63:0] _T_162_1;
  wire [63:0] _T_162_2;
  wire [63:0] _T_162_3;
  wire [63:0] _T_162_4;
  wire [63:0] _T_162_5;
  wire [63:0] _T_162_6;
  wire [63:0] _T_162_7;
  wire [63:0] _T_162_8;
  wire [63:0] _T_162_9;
  wire [63:0] _T_162_10;
  wire [63:0] _T_162_11;
  wire [63:0] _T_162_12;
  wire [63:0] _T_162_13;
  wire [63:0] _T_162_14;
  wire [63:0] _T_162_15;
  reg  _GEN_0;
  reg [31:0] _GEN_3;
  reg  _GEN_1;
  reg [31:0] _GEN_4;
  reg  _GEN_2;
  reg [31:0] _GEN_5;
  FF_2 tagFF (
    .clock(tagFF_clock),
    .reset(tagFF_reset),
    .io_in(tagFF_io_in),
    .io_init(tagFF_io_init),
    .io_out(tagFF_io_out),
    .io_enable(tagFF_io_enable)
  );
  assign io_deq_0 = _T_162_0;
  assign io_deq_1 = _T_162_1;
  assign io_deq_2 = _T_162_2;
  assign io_deq_3 = _T_162_3;
  assign io_deq_4 = _T_162_4;
  assign io_deq_5 = _T_162_5;
  assign io_deq_6 = _T_162_6;
  assign io_deq_7 = _T_162_7;
  assign io_deq_8 = _T_162_8;
  assign io_deq_9 = _T_162_9;
  assign io_deq_10 = _T_162_10;
  assign io_deq_11 = _T_162_11;
  assign io_deq_12 = _T_162_12;
  assign io_deq_13 = _T_162_13;
  assign io_deq_14 = _T_162_14;
  assign io_deq_15 = _T_162_15;
  assign io_empty = 1'h1;
  assign io_forceTag_ready = _GEN_0;
  assign io_tag = 1'h0;
  assign tagFF_clock = clock;
  assign tagFF_reset = reset;
  assign tagFF_io_in = _GEN_1;
  assign tagFF_io_init = 1'h0;
  assign tagFF_io_enable = _GEN_2;
  assign _T_162_0 = 64'h0;
  assign _T_162_1 = 64'h0;
  assign _T_162_2 = 64'h0;
  assign _T_162_3 = 64'h0;
  assign _T_162_4 = 64'h0;
  assign _T_162_5 = 64'h0;
  assign _T_162_6 = 64'h0;
  assign _T_162_7 = 64'h0;
  assign _T_162_8 = 64'h0;
  assign _T_162_9 = 64'h0;
  assign _T_162_10 = 64'h0;
  assign _T_162_11 = 64'h0;
  assign _T_162_12 = 64'h0;
  assign _T_162_13 = 64'h0;
  assign _T_162_14 = 64'h0;
  assign _T_162_15 = 64'h0;
`ifdef RANDOMIZE
  integer initvar;
  initial begin
    `ifndef verilator
      #0.002 begin end
    `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_3 = {1{$random}};
  _GEN_0 = _GEN_3[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_4 = {1{$random}};
  _GEN_1 = _GEN_4[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_5 = {1{$random}};
  _GEN_2 = _GEN_5[0:0];
  `endif
  end
`endif
endmodule
module FIFOArbiter_1(
  input   clock,
  input   reset,
  output  io_deq_0,
  output  io_deq_1,
  output  io_deq_2,
  output  io_deq_3,
  output  io_deq_4,
  output  io_deq_5,
  output  io_deq_6,
  output  io_deq_7,
  output  io_deq_8,
  output  io_deq_9,
  output  io_deq_10,
  output  io_deq_11,
  output  io_deq_12,
  output  io_deq_13,
  output  io_deq_14,
  output  io_deq_15,
  input   io_deqVld,
  output  io_empty,
  output  io_forceTag_ready,
  input   io_forceTag_valid,
  input   io_forceTag_bits,
  output  io_tag,
  input   io_config_chainWrite,
  input   io_config_chainRead
);
  wire  tagFF_clock;
  wire  tagFF_reset;
  wire  tagFF_io_in;
  wire  tagFF_io_init;
  wire  tagFF_io_out;
  wire  tagFF_io_enable;
  wire  _T_162_0;
  wire  _T_162_1;
  wire  _T_162_2;
  wire  _T_162_3;
  wire  _T_162_4;
  wire  _T_162_5;
  wire  _T_162_6;
  wire  _T_162_7;
  wire  _T_162_8;
  wire  _T_162_9;
  wire  _T_162_10;
  wire  _T_162_11;
  wire  _T_162_12;
  wire  _T_162_13;
  wire  _T_162_14;
  wire  _T_162_15;
  reg  _GEN_0;
  reg [31:0] _GEN_3;
  reg  _GEN_1;
  reg [31:0] _GEN_4;
  reg  _GEN_2;
  reg [31:0] _GEN_5;
  FF_2 tagFF (
    .clock(tagFF_clock),
    .reset(tagFF_reset),
    .io_in(tagFF_io_in),
    .io_init(tagFF_io_init),
    .io_out(tagFF_io_out),
    .io_enable(tagFF_io_enable)
  );
  assign io_deq_0 = _T_162_0;
  assign io_deq_1 = _T_162_1;
  assign io_deq_2 = _T_162_2;
  assign io_deq_3 = _T_162_3;
  assign io_deq_4 = _T_162_4;
  assign io_deq_5 = _T_162_5;
  assign io_deq_6 = _T_162_6;
  assign io_deq_7 = _T_162_7;
  assign io_deq_8 = _T_162_8;
  assign io_deq_9 = _T_162_9;
  assign io_deq_10 = _T_162_10;
  assign io_deq_11 = _T_162_11;
  assign io_deq_12 = _T_162_12;
  assign io_deq_13 = _T_162_13;
  assign io_deq_14 = _T_162_14;
  assign io_deq_15 = _T_162_15;
  assign io_empty = 1'h1;
  assign io_forceTag_ready = _GEN_0;
  assign io_tag = 1'h0;
  assign tagFF_clock = clock;
  assign tagFF_reset = reset;
  assign tagFF_io_in = _GEN_1;
  assign tagFF_io_init = 1'h0;
  assign tagFF_io_enable = _GEN_2;
  assign _T_162_0 = 1'h0;
  assign _T_162_1 = 1'h0;
  assign _T_162_2 = 1'h0;
  assign _T_162_3 = 1'h0;
  assign _T_162_4 = 1'h0;
  assign _T_162_5 = 1'h0;
  assign _T_162_6 = 1'h0;
  assign _T_162_7 = 1'h0;
  assign _T_162_8 = 1'h0;
  assign _T_162_9 = 1'h0;
  assign _T_162_10 = 1'h0;
  assign _T_162_11 = 1'h0;
  assign _T_162_12 = 1'h0;
  assign _T_162_13 = 1'h0;
  assign _T_162_14 = 1'h0;
  assign _T_162_15 = 1'h0;
`ifdef RANDOMIZE
  integer initvar;
  initial begin
    `ifndef verilator
      #0.002 begin end
    `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_3 = {1{$random}};
  _GEN_0 = _GEN_3[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_4 = {1{$random}};
  _GEN_1 = _GEN_4[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_5 = {1{$random}};
  _GEN_2 = _GEN_5[0:0];
  `endif
  end
`endif
endmodule
module FIFOArbiter_2(
  input         clock,
  input         reset,
  output [31:0] io_deq_0,
  output [31:0] io_deq_1,
  output [31:0] io_deq_2,
  output [31:0] io_deq_3,
  output [31:0] io_deq_4,
  output [31:0] io_deq_5,
  output [31:0] io_deq_6,
  output [31:0] io_deq_7,
  output [31:0] io_deq_8,
  output [31:0] io_deq_9,
  output [31:0] io_deq_10,
  output [31:0] io_deq_11,
  output [31:0] io_deq_12,
  output [31:0] io_deq_13,
  output [31:0] io_deq_14,
  output [31:0] io_deq_15,
  input         io_deqVld,
  output        io_empty,
  output        io_forceTag_ready,
  input         io_forceTag_valid,
  input         io_forceTag_bits,
  output        io_tag,
  input         io_config_chainWrite,
  input         io_config_chainRead
);
  wire  tagFF_clock;
  wire  tagFF_reset;
  wire  tagFF_io_in;
  wire  tagFF_io_init;
  wire  tagFF_io_out;
  wire  tagFF_io_enable;
  wire [31:0] _T_162_0;
  wire [31:0] _T_162_1;
  wire [31:0] _T_162_2;
  wire [31:0] _T_162_3;
  wire [31:0] _T_162_4;
  wire [31:0] _T_162_5;
  wire [31:0] _T_162_6;
  wire [31:0] _T_162_7;
  wire [31:0] _T_162_8;
  wire [31:0] _T_162_9;
  wire [31:0] _T_162_10;
  wire [31:0] _T_162_11;
  wire [31:0] _T_162_12;
  wire [31:0] _T_162_13;
  wire [31:0] _T_162_14;
  wire [31:0] _T_162_15;
  reg  _GEN_0;
  reg [31:0] _GEN_3;
  reg  _GEN_1;
  reg [31:0] _GEN_4;
  reg  _GEN_2;
  reg [31:0] _GEN_5;
  FF_2 tagFF (
    .clock(tagFF_clock),
    .reset(tagFF_reset),
    .io_in(tagFF_io_in),
    .io_init(tagFF_io_init),
    .io_out(tagFF_io_out),
    .io_enable(tagFF_io_enable)
  );
  assign io_deq_0 = _T_162_0;
  assign io_deq_1 = _T_162_1;
  assign io_deq_2 = _T_162_2;
  assign io_deq_3 = _T_162_3;
  assign io_deq_4 = _T_162_4;
  assign io_deq_5 = _T_162_5;
  assign io_deq_6 = _T_162_6;
  assign io_deq_7 = _T_162_7;
  assign io_deq_8 = _T_162_8;
  assign io_deq_9 = _T_162_9;
  assign io_deq_10 = _T_162_10;
  assign io_deq_11 = _T_162_11;
  assign io_deq_12 = _T_162_12;
  assign io_deq_13 = _T_162_13;
  assign io_deq_14 = _T_162_14;
  assign io_deq_15 = _T_162_15;
  assign io_empty = 1'h1;
  assign io_forceTag_ready = _GEN_0;
  assign io_tag = 1'h0;
  assign tagFF_clock = clock;
  assign tagFF_reset = reset;
  assign tagFF_io_in = _GEN_1;
  assign tagFF_io_init = 1'h0;
  assign tagFF_io_enable = _GEN_2;
  assign _T_162_0 = 32'h0;
  assign _T_162_1 = 32'h0;
  assign _T_162_2 = 32'h0;
  assign _T_162_3 = 32'h0;
  assign _T_162_4 = 32'h0;
  assign _T_162_5 = 32'h0;
  assign _T_162_6 = 32'h0;
  assign _T_162_7 = 32'h0;
  assign _T_162_8 = 32'h0;
  assign _T_162_9 = 32'h0;
  assign _T_162_10 = 32'h0;
  assign _T_162_11 = 32'h0;
  assign _T_162_12 = 32'h0;
  assign _T_162_13 = 32'h0;
  assign _T_162_14 = 32'h0;
  assign _T_162_15 = 32'h0;
`ifdef RANDOMIZE
  integer initvar;
  initial begin
    `ifndef verilator
      #0.002 begin end
    `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_3 = {1{$random}};
  _GEN_0 = _GEN_3[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_4 = {1{$random}};
  _GEN_1 = _GEN_4[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_5 = {1{$random}};
  _GEN_2 = _GEN_5[0:0];
  `endif
  end
`endif
endmodule
module FIFOArbiterWidthConvert(
  input         clock,
  input         reset,
  output [31:0] io_deq_0,
  output [31:0] io_deq_1,
  output [31:0] io_deq_2,
  output [31:0] io_deq_3,
  output [31:0] io_deq_4,
  output [31:0] io_deq_5,
  output [31:0] io_deq_6,
  output [31:0] io_deq_7,
  output [31:0] io_deq_8,
  output [31:0] io_deq_9,
  output [31:0] io_deq_10,
  output [31:0] io_deq_11,
  output [31:0] io_deq_12,
  output [31:0] io_deq_13,
  output [31:0] io_deq_14,
  output [31:0] io_deq_15,
  input         io_deqVld,
  output        io_empty,
  output        io_forceTag_ready,
  input         io_forceTag_valid,
  input         io_forceTag_bits,
  output        io_tag
);
  wire  tagFF_clock;
  wire  tagFF_reset;
  wire  tagFF_io_in;
  wire  tagFF_io_init;
  wire  tagFF_io_out;
  wire  tagFF_io_enable;
  wire [31:0] _T_79_0;
  wire [31:0] _T_79_1;
  wire [31:0] _T_79_2;
  wire [31:0] _T_79_3;
  wire [31:0] _T_79_4;
  wire [31:0] _T_79_5;
  wire [31:0] _T_79_6;
  wire [31:0] _T_79_7;
  wire [31:0] _T_79_8;
  wire [31:0] _T_79_9;
  wire [31:0] _T_79_10;
  wire [31:0] _T_79_11;
  wire [31:0] _T_79_12;
  wire [31:0] _T_79_13;
  wire [31:0] _T_79_14;
  wire [31:0] _T_79_15;
  reg  _GEN_0;
  reg [31:0] _GEN_3;
  reg  _GEN_1;
  reg [31:0] _GEN_4;
  reg  _GEN_2;
  reg [31:0] _GEN_5;
  FF_2 tagFF (
    .clock(tagFF_clock),
    .reset(tagFF_reset),
    .io_in(tagFF_io_in),
    .io_init(tagFF_io_init),
    .io_out(tagFF_io_out),
    .io_enable(tagFF_io_enable)
  );
  assign io_deq_0 = _T_79_0;
  assign io_deq_1 = _T_79_1;
  assign io_deq_2 = _T_79_2;
  assign io_deq_3 = _T_79_3;
  assign io_deq_4 = _T_79_4;
  assign io_deq_5 = _T_79_5;
  assign io_deq_6 = _T_79_6;
  assign io_deq_7 = _T_79_7;
  assign io_deq_8 = _T_79_8;
  assign io_deq_9 = _T_79_9;
  assign io_deq_10 = _T_79_10;
  assign io_deq_11 = _T_79_11;
  assign io_deq_12 = _T_79_12;
  assign io_deq_13 = _T_79_13;
  assign io_deq_14 = _T_79_14;
  assign io_deq_15 = _T_79_15;
  assign io_empty = 1'h1;
  assign io_forceTag_ready = _GEN_0;
  assign io_tag = 1'h0;
  assign tagFF_clock = clock;
  assign tagFF_reset = reset;
  assign tagFF_io_in = _GEN_1;
  assign tagFF_io_init = 1'h0;
  assign tagFF_io_enable = _GEN_2;
  assign _T_79_0 = 32'h0;
  assign _T_79_1 = 32'h0;
  assign _T_79_2 = 32'h0;
  assign _T_79_3 = 32'h0;
  assign _T_79_4 = 32'h0;
  assign _T_79_5 = 32'h0;
  assign _T_79_6 = 32'h0;
  assign _T_79_7 = 32'h0;
  assign _T_79_8 = 32'h0;
  assign _T_79_9 = 32'h0;
  assign _T_79_10 = 32'h0;
  assign _T_79_11 = 32'h0;
  assign _T_79_12 = 32'h0;
  assign _T_79_13 = 32'h0;
  assign _T_79_14 = 32'h0;
  assign _T_79_15 = 32'h0;
`ifdef RANDOMIZE
  integer initvar;
  initial begin
    `ifndef verilator
      #0.002 begin end
    `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_3 = {1{$random}};
  _GEN_0 = _GEN_3[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_4 = {1{$random}};
  _GEN_1 = _GEN_4[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_5 = {1{$random}};
  _GEN_2 = _GEN_5[0:0];
  `endif
  end
`endif
endmodule
module FF_8(
  input         clock,
  input         reset,
  input  [31:0] io_in,
  input  [31:0] io_init,
  output [31:0] io_out,
  input         io_enable
);
  wire [31:0] d;
  reg [31:0] ff;
  reg [31:0] _GEN_0;
  wire  _T_13;
  wire [31:0] _GEN_1;
  assign io_out = ff;
  assign d = _GEN_1;
  assign _T_13 = io_enable == 1'h0;
  assign _GEN_1 = _T_13 ? ff : io_in;
`ifdef RANDOMIZE
  integer initvar;
  initial begin
    `ifndef verilator
      #0.002 begin end
    `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_0 = {1{$random}};
  ff = _GEN_0[31:0];
  `endif
  end
`endif
  always @(posedge clock) begin
    if (reset) begin
      ff <= io_init;
    end else begin
      ff <= d;
    end
  end
endmodule
module Counter(
  input         clock,
  input         reset,
  input  [31:0] io_max,
  input  [31:0] io_stride,
  output [31:0] io_out,
  output [31:0] io_next,
  input         io_reset,
  input         io_enable,
  input         io_saturate,
  output        io_done
);
  wire  reg$_clock;
  wire  reg$_reset;
  wire [31:0] reg$_io_in;
  wire [31:0] reg$_io_init;
  wire [31:0] reg$_io_out;
  wire  reg$_io_enable;
  wire  _T_18;
  wire [32:0] count;
  wire [32:0] _GEN_2;
  wire [33:0] _T_20;
  wire [32:0] newval;
  wire [32:0] _GEN_3;
  wire  isMax;
  wire [32:0] _T_21;
  wire [32:0] next;
  wire  _T_23;
  wire [32:0] _GEN_1;
  wire  _T_24;
  FF_8 reg$ (
    .clock(reg$_clock),
    .reset(reg$_reset),
    .io_in(reg$_io_in),
    .io_init(reg$_io_init),
    .io_out(reg$_io_out),
    .io_enable(reg$_io_enable)
  );
  assign io_out = count[31:0];
  assign io_next = next[31:0];
  assign io_done = _T_24;
  assign reg$_clock = clock;
  assign reg$_reset = reset;
  assign reg$_io_in = _GEN_1[31:0];
  assign reg$_io_init = 32'h0;
  assign reg$_io_enable = _T_18;
  assign _T_18 = io_reset | io_enable;
  assign count = {1'h0,reg$_io_out};
  assign _GEN_2 = {{1'd0}, io_stride};
  assign _T_20 = count + _GEN_2;
  assign newval = _T_20[32:0];
  assign _GEN_3 = {{1'd0}, io_max};
  assign isMax = newval >= _GEN_3;
  assign _T_21 = io_saturate ? count : 33'h0;
  assign next = isMax ? _T_21 : newval;
  assign _T_23 = io_reset == 1'h0;
  assign _GEN_1 = _T_23 ? next : 33'h0;
  assign _T_24 = io_enable & isMax;
endmodule
module FF_9(
  input         clock,
  input         reset,
  input  [10:0] io_in,
  input  [10:0] io_init,
  output [10:0] io_out,
  input         io_enable
);
  wire [10:0] d;
  reg [10:0] ff;
  reg [31:0] _GEN_0;
  wire  _T_13;
  wire [10:0] _GEN_1;
  assign io_out = ff;
  assign d = _GEN_1;
  assign _T_13 = io_enable == 1'h0;
  assign _GEN_1 = _T_13 ? ff : io_in;
`ifdef RANDOMIZE
  integer initvar;
  initial begin
    `ifndef verilator
      #0.002 begin end
    `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_0 = {1{$random}};
  ff = _GEN_0[10:0];
  `endif
  end
`endif
  always @(posedge clock) begin
    if (reset) begin
      ff <= io_init;
    end else begin
      ff <= d;
    end
  end
endmodule
module Counter_1(
  input         clock,
  input         reset,
  input  [10:0] io_max,
  input  [10:0] io_stride,
  output [10:0] io_out,
  output [10:0] io_next,
  input         io_reset,
  input         io_enable,
  input         io_saturate,
  output        io_done
);
  wire  reg$_clock;
  wire  reg$_reset;
  wire [10:0] reg$_io_in;
  wire [10:0] reg$_io_init;
  wire [10:0] reg$_io_out;
  wire  reg$_io_enable;
  wire  _T_18;
  wire [11:0] count;
  wire [11:0] _GEN_2;
  wire [12:0] _T_20;
  wire [11:0] newval;
  wire [11:0] _GEN_3;
  wire  isMax;
  wire [11:0] _T_21;
  wire [11:0] next;
  wire  _T_23;
  wire [11:0] _GEN_1;
  wire  _T_24;
  FF_9 reg$ (
    .clock(reg$_clock),
    .reset(reg$_reset),
    .io_in(reg$_io_in),
    .io_init(reg$_io_init),
    .io_out(reg$_io_out),
    .io_enable(reg$_io_enable)
  );
  assign io_out = count[10:0];
  assign io_next = next[10:0];
  assign io_done = _T_24;
  assign reg$_clock = clock;
  assign reg$_reset = reset;
  assign reg$_io_in = _GEN_1[10:0];
  assign reg$_io_init = 11'h0;
  assign reg$_io_enable = _T_18;
  assign _T_18 = io_reset | io_enable;
  assign count = {1'h0,reg$_io_out};
  assign _GEN_2 = {{1'd0}, io_stride};
  assign _T_20 = count + _GEN_2;
  assign newval = _T_20[11:0];
  assign _GEN_3 = {{1'd0}, io_max};
  assign isMax = newval >= _GEN_3;
  assign _T_21 = io_saturate ? count : 12'h0;
  assign next = isMax ? _T_21 : newval;
  assign _T_23 = io_reset == 1'h0;
  assign _GEN_1 = _T_23 ? next : 12'h0;
  assign _T_24 = io_enable & isMax;
endmodule
module MAGCore(
  input         clock,
  input         reset,
  input         io_dram_cmd_ready,
  output        io_dram_cmd_valid,
  output [63:0] io_dram_cmd_bits_addr,
  output        io_dram_cmd_bits_isWr,
  output [31:0] io_dram_cmd_bits_tag,
  output [31:0] io_dram_cmd_bits_streamId,
  output [31:0] io_dram_cmd_bits_wdata_0,
  output [31:0] io_dram_cmd_bits_wdata_1,
  output [31:0] io_dram_cmd_bits_wdata_2,
  output [31:0] io_dram_cmd_bits_wdata_3,
  output [31:0] io_dram_cmd_bits_wdata_4,
  output [31:0] io_dram_cmd_bits_wdata_5,
  output [31:0] io_dram_cmd_bits_wdata_6,
  output [31:0] io_dram_cmd_bits_wdata_7,
  output [31:0] io_dram_cmd_bits_wdata_8,
  output [31:0] io_dram_cmd_bits_wdata_9,
  output [31:0] io_dram_cmd_bits_wdata_10,
  output [31:0] io_dram_cmd_bits_wdata_11,
  output [31:0] io_dram_cmd_bits_wdata_12,
  output [31:0] io_dram_cmd_bits_wdata_13,
  output [31:0] io_dram_cmd_bits_wdata_14,
  output [31:0] io_dram_cmd_bits_wdata_15,
  output        io_dram_resp_ready,
  input         io_dram_resp_valid,
  input  [31:0] io_dram_resp_bits_rdata_0,
  input  [31:0] io_dram_resp_bits_rdata_1,
  input  [31:0] io_dram_resp_bits_rdata_2,
  input  [31:0] io_dram_resp_bits_rdata_3,
  input  [31:0] io_dram_resp_bits_rdata_4,
  input  [31:0] io_dram_resp_bits_rdata_5,
  input  [31:0] io_dram_resp_bits_rdata_6,
  input  [31:0] io_dram_resp_bits_rdata_7,
  input  [31:0] io_dram_resp_bits_rdata_8,
  input  [31:0] io_dram_resp_bits_rdata_9,
  input  [31:0] io_dram_resp_bits_rdata_10,
  input  [31:0] io_dram_resp_bits_rdata_11,
  input  [31:0] io_dram_resp_bits_rdata_12,
  input  [31:0] io_dram_resp_bits_rdata_13,
  input  [31:0] io_dram_resp_bits_rdata_14,
  input  [31:0] io_dram_resp_bits_rdata_15,
  input  [31:0] io_dram_resp_bits_tag,
  input  [31:0] io_dram_resp_bits_streamId,
  input         io_config_scatterGather
);
  wire  addrFifo_clock;
  wire  addrFifo_reset;
  wire [63:0] addrFifo_io_deq_0;
  wire [63:0] addrFifo_io_deq_1;
  wire [63:0] addrFifo_io_deq_2;
  wire [63:0] addrFifo_io_deq_3;
  wire [63:0] addrFifo_io_deq_4;
  wire [63:0] addrFifo_io_deq_5;
  wire [63:0] addrFifo_io_deq_6;
  wire [63:0] addrFifo_io_deq_7;
  wire [63:0] addrFifo_io_deq_8;
  wire [63:0] addrFifo_io_deq_9;
  wire [63:0] addrFifo_io_deq_10;
  wire [63:0] addrFifo_io_deq_11;
  wire [63:0] addrFifo_io_deq_12;
  wire [63:0] addrFifo_io_deq_13;
  wire [63:0] addrFifo_io_deq_14;
  wire [63:0] addrFifo_io_deq_15;
  wire  addrFifo_io_deqVld;
  wire  addrFifo_io_empty;
  wire  addrFifo_io_forceTag_ready;
  wire  addrFifo_io_forceTag_valid;
  wire  addrFifo_io_forceTag_bits;
  wire  addrFifo_io_tag;
  wire  addrFifo_io_config_chainWrite;
  wire  addrFifo_io_config_chainRead;
  wire  addrFifoConfig_chainWrite;
  wire  addrFifoConfig_chainRead;
  wire  _T_84;
  wire [57:0] burstAddrs_0;
  wire  isWrFifo_clock;
  wire  isWrFifo_reset;
  wire  isWrFifo_io_deq_0;
  wire  isWrFifo_io_deq_1;
  wire  isWrFifo_io_deq_2;
  wire  isWrFifo_io_deq_3;
  wire  isWrFifo_io_deq_4;
  wire  isWrFifo_io_deq_5;
  wire  isWrFifo_io_deq_6;
  wire  isWrFifo_io_deq_7;
  wire  isWrFifo_io_deq_8;
  wire  isWrFifo_io_deq_9;
  wire  isWrFifo_io_deq_10;
  wire  isWrFifo_io_deq_11;
  wire  isWrFifo_io_deq_12;
  wire  isWrFifo_io_deq_13;
  wire  isWrFifo_io_deq_14;
  wire  isWrFifo_io_deq_15;
  wire  isWrFifo_io_deqVld;
  wire  isWrFifo_io_empty;
  wire  isWrFifo_io_forceTag_ready;
  wire  isWrFifo_io_forceTag_valid;
  wire  isWrFifo_io_forceTag_bits;
  wire  isWrFifo_io_tag;
  wire  isWrFifo_io_config_chainWrite;
  wire  isWrFifo_io_config_chainRead;
  wire  isWrFifoConfig_chainWrite;
  wire  isWrFifoConfig_chainRead;
  wire  sizeFifo_clock;
  wire  sizeFifo_reset;
  wire [31:0] sizeFifo_io_deq_0;
  wire [31:0] sizeFifo_io_deq_1;
  wire [31:0] sizeFifo_io_deq_2;
  wire [31:0] sizeFifo_io_deq_3;
  wire [31:0] sizeFifo_io_deq_4;
  wire [31:0] sizeFifo_io_deq_5;
  wire [31:0] sizeFifo_io_deq_6;
  wire [31:0] sizeFifo_io_deq_7;
  wire [31:0] sizeFifo_io_deq_8;
  wire [31:0] sizeFifo_io_deq_9;
  wire [31:0] sizeFifo_io_deq_10;
  wire [31:0] sizeFifo_io_deq_11;
  wire [31:0] sizeFifo_io_deq_12;
  wire [31:0] sizeFifo_io_deq_13;
  wire [31:0] sizeFifo_io_deq_14;
  wire [31:0] sizeFifo_io_deq_15;
  wire  sizeFifo_io_deqVld;
  wire  sizeFifo_io_empty;
  wire  sizeFifo_io_forceTag_ready;
  wire  sizeFifo_io_forceTag_valid;
  wire  sizeFifo_io_forceTag_bits;
  wire  sizeFifo_io_tag;
  wire  sizeFifo_io_config_chainWrite;
  wire  sizeFifo_io_config_chainRead;
  wire  sizeFifoConfig_chainWrite;
  wire  sizeFifoConfig_chainRead;
  wire [25:0] _T_102;
  wire [5:0] _T_103;
  wire  _T_105;
  wire [25:0] _GEN_0;
  wire [26:0] _T_106;
  wire [25:0] sizeInBursts;
  wire  wdataFifo_clock;
  wire  wdataFifo_reset;
  wire [31:0] wdataFifo_io_deq_0;
  wire [31:0] wdataFifo_io_deq_1;
  wire [31:0] wdataFifo_io_deq_2;
  wire [31:0] wdataFifo_io_deq_3;
  wire [31:0] wdataFifo_io_deq_4;
  wire [31:0] wdataFifo_io_deq_5;
  wire [31:0] wdataFifo_io_deq_6;
  wire [31:0] wdataFifo_io_deq_7;
  wire [31:0] wdataFifo_io_deq_8;
  wire [31:0] wdataFifo_io_deq_9;
  wire [31:0] wdataFifo_io_deq_10;
  wire [31:0] wdataFifo_io_deq_11;
  wire [31:0] wdataFifo_io_deq_12;
  wire [31:0] wdataFifo_io_deq_13;
  wire [31:0] wdataFifo_io_deq_14;
  wire [31:0] wdataFifo_io_deq_15;
  wire  wdataFifo_io_deqVld;
  wire  wdataFifo_io_empty;
  wire  wdataFifo_io_forceTag_ready;
  wire  wdataFifo_io_forceTag_valid;
  wire  wdataFifo_io_forceTag_bits;
  wire  wdataFifo_io_tag;
  wire  wrPhase_clock;
  wire  wrPhase_reset;
  wire  wrPhase_io_input_set;
  wire  wrPhase_io_input_reset;
  wire  wrPhase_io_input_asyn_reset;
  wire  wrPhase_io_output_data;
  wire  _T_107;
  wire  _T_108;
  wire  _T_109;
  wire  _T_110;
  wire  _T_111;
  wire  _T_112;
  wire  _T_114;
  wire  burstVld;
  wire  issued;
  wire  issuedFF_clock;
  wire  issuedFF_reset;
  wire  issuedFF_io_in;
  wire  issuedFF_io_init;
  wire  issuedFF_io_out;
  wire  issuedFF_io_enable;
  wire  _T_117;
  wire  _T_118;
  wire  _T_119;
  wire [1:0] _T_123;
  wire [1:0] _T_124;
  wire  _T_125;
  wire  burstCounter_clock;
  wire  burstCounter_reset;
  wire [31:0] burstCounter_io_max;
  wire [31:0] burstCounter_io_stride;
  wire [31:0] burstCounter_io_out;
  wire [31:0] burstCounter_io_next;
  wire  burstCounter_io_reset;
  wire  burstCounter_io_enable;
  wire  burstCounter_io_saturate;
  wire  burstCounter_io_done;
  wire [25:0] _T_129;
  wire  _T_132;
  wire  _T_133;
  wire  _T_134;
  wire  _T_135;
  wire  _T_136;
  wire  burstTagCounter_clock;
  wire  burstTagCounter_reset;
  wire [10:0] burstTagCounter_io_max;
  wire [10:0] burstTagCounter_io_stride;
  wire [10:0] burstTagCounter_io_out;
  wire [10:0] burstTagCounter_io_next;
  wire  burstTagCounter_io_reset;
  wire  burstTagCounter_io_enable;
  wire  burstTagCounter_io_saturate;
  wire  burstTagCounter_io_done;
  wire  _T_147;
  wire  _T_148;
  wire  _T_150;
  wire  tagOut_streamTag;
  wire [30:0] tagOut_burstTag;
  wire [57:0] _T_156;
  wire [57:0] _GEN_1;
  wire [58:0] _T_157;
  wire [57:0] _T_158;
  wire [63:0] _T_160;
  wire [31:0] _T_161;
  wire  _T_163;
  reg  _T_166;
  reg [31:0] _GEN_7;
  wire  _T_173;
  wire [31:0] issuedTag;
  reg  _GEN_2;
  reg [31:0] _GEN_8;
  reg  _GEN_3;
  reg [31:0] _GEN_9;
  reg  _GEN_4;
  reg [31:0] _GEN_10;
  reg  _GEN_5;
  reg [31:0] _GEN_11;
  reg  _GEN_6;
  reg [31:0] _GEN_12;
  FIFOArbiter addrFifo (
    .clock(addrFifo_clock),
    .reset(addrFifo_reset),
    .io_deq_0(addrFifo_io_deq_0),
    .io_deq_1(addrFifo_io_deq_1),
    .io_deq_2(addrFifo_io_deq_2),
    .io_deq_3(addrFifo_io_deq_3),
    .io_deq_4(addrFifo_io_deq_4),
    .io_deq_5(addrFifo_io_deq_5),
    .io_deq_6(addrFifo_io_deq_6),
    .io_deq_7(addrFifo_io_deq_7),
    .io_deq_8(addrFifo_io_deq_8),
    .io_deq_9(addrFifo_io_deq_9),
    .io_deq_10(addrFifo_io_deq_10),
    .io_deq_11(addrFifo_io_deq_11),
    .io_deq_12(addrFifo_io_deq_12),
    .io_deq_13(addrFifo_io_deq_13),
    .io_deq_14(addrFifo_io_deq_14),
    .io_deq_15(addrFifo_io_deq_15),
    .io_deqVld(addrFifo_io_deqVld),
    .io_empty(addrFifo_io_empty),
    .io_forceTag_ready(addrFifo_io_forceTag_ready),
    .io_forceTag_valid(addrFifo_io_forceTag_valid),
    .io_forceTag_bits(addrFifo_io_forceTag_bits),
    .io_tag(addrFifo_io_tag),
    .io_config_chainWrite(addrFifo_io_config_chainWrite),
    .io_config_chainRead(addrFifo_io_config_chainRead)
  );
  FIFOArbiter_1 isWrFifo (
    .clock(isWrFifo_clock),
    .reset(isWrFifo_reset),
    .io_deq_0(isWrFifo_io_deq_0),
    .io_deq_1(isWrFifo_io_deq_1),
    .io_deq_2(isWrFifo_io_deq_2),
    .io_deq_3(isWrFifo_io_deq_3),
    .io_deq_4(isWrFifo_io_deq_4),
    .io_deq_5(isWrFifo_io_deq_5),
    .io_deq_6(isWrFifo_io_deq_6),
    .io_deq_7(isWrFifo_io_deq_7),
    .io_deq_8(isWrFifo_io_deq_8),
    .io_deq_9(isWrFifo_io_deq_9),
    .io_deq_10(isWrFifo_io_deq_10),
    .io_deq_11(isWrFifo_io_deq_11),
    .io_deq_12(isWrFifo_io_deq_12),
    .io_deq_13(isWrFifo_io_deq_13),
    .io_deq_14(isWrFifo_io_deq_14),
    .io_deq_15(isWrFifo_io_deq_15),
    .io_deqVld(isWrFifo_io_deqVld),
    .io_empty(isWrFifo_io_empty),
    .io_forceTag_ready(isWrFifo_io_forceTag_ready),
    .io_forceTag_valid(isWrFifo_io_forceTag_valid),
    .io_forceTag_bits(isWrFifo_io_forceTag_bits),
    .io_tag(isWrFifo_io_tag),
    .io_config_chainWrite(isWrFifo_io_config_chainWrite),
    .io_config_chainRead(isWrFifo_io_config_chainRead)
  );
  FIFOArbiter_2 sizeFifo (
    .clock(sizeFifo_clock),
    .reset(sizeFifo_reset),
    .io_deq_0(sizeFifo_io_deq_0),
    .io_deq_1(sizeFifo_io_deq_1),
    .io_deq_2(sizeFifo_io_deq_2),
    .io_deq_3(sizeFifo_io_deq_3),
    .io_deq_4(sizeFifo_io_deq_4),
    .io_deq_5(sizeFifo_io_deq_5),
    .io_deq_6(sizeFifo_io_deq_6),
    .io_deq_7(sizeFifo_io_deq_7),
    .io_deq_8(sizeFifo_io_deq_8),
    .io_deq_9(sizeFifo_io_deq_9),
    .io_deq_10(sizeFifo_io_deq_10),
    .io_deq_11(sizeFifo_io_deq_11),
    .io_deq_12(sizeFifo_io_deq_12),
    .io_deq_13(sizeFifo_io_deq_13),
    .io_deq_14(sizeFifo_io_deq_14),
    .io_deq_15(sizeFifo_io_deq_15),
    .io_deqVld(sizeFifo_io_deqVld),
    .io_empty(sizeFifo_io_empty),
    .io_forceTag_ready(sizeFifo_io_forceTag_ready),
    .io_forceTag_valid(sizeFifo_io_forceTag_valid),
    .io_forceTag_bits(sizeFifo_io_forceTag_bits),
    .io_tag(sizeFifo_io_tag),
    .io_config_chainWrite(sizeFifo_io_config_chainWrite),
    .io_config_chainRead(sizeFifo_io_config_chainRead)
  );
  FIFOArbiterWidthConvert wdataFifo (
    .clock(wdataFifo_clock),
    .reset(wdataFifo_reset),
    .io_deq_0(wdataFifo_io_deq_0),
    .io_deq_1(wdataFifo_io_deq_1),
    .io_deq_2(wdataFifo_io_deq_2),
    .io_deq_3(wdataFifo_io_deq_3),
    .io_deq_4(wdataFifo_io_deq_4),
    .io_deq_5(wdataFifo_io_deq_5),
    .io_deq_6(wdataFifo_io_deq_6),
    .io_deq_7(wdataFifo_io_deq_7),
    .io_deq_8(wdataFifo_io_deq_8),
    .io_deq_9(wdataFifo_io_deq_9),
    .io_deq_10(wdataFifo_io_deq_10),
    .io_deq_11(wdataFifo_io_deq_11),
    .io_deq_12(wdataFifo_io_deq_12),
    .io_deq_13(wdataFifo_io_deq_13),
    .io_deq_14(wdataFifo_io_deq_14),
    .io_deq_15(wdataFifo_io_deq_15),
    .io_deqVld(wdataFifo_io_deqVld),
    .io_empty(wdataFifo_io_empty),
    .io_forceTag_ready(wdataFifo_io_forceTag_ready),
    .io_forceTag_valid(wdataFifo_io_forceTag_valid),
    .io_forceTag_bits(wdataFifo_io_forceTag_bits),
    .io_tag(wdataFifo_io_tag)
  );
  SRFF_sp wrPhase (
    .clock(wrPhase_clock),
    .reset(wrPhase_reset),
    .io_input_set(wrPhase_io_input_set),
    .io_input_reset(wrPhase_io_input_reset),
    .io_input_asyn_reset(wrPhase_io_input_asyn_reset),
    .io_output_data(wrPhase_io_output_data)
  );
  FF_2 issuedFF (
    .clock(issuedFF_clock),
    .reset(issuedFF_reset),
    .io_in(issuedFF_io_in),
    .io_init(issuedFF_io_init),
    .io_out(issuedFF_io_out),
    .io_enable(issuedFF_io_enable)
  );
  Counter burstCounter (
    .clock(burstCounter_clock),
    .reset(burstCounter_reset),
    .io_max(burstCounter_io_max),
    .io_stride(burstCounter_io_stride),
    .io_out(burstCounter_io_out),
    .io_next(burstCounter_io_next),
    .io_reset(burstCounter_io_reset),
    .io_enable(burstCounter_io_enable),
    .io_saturate(burstCounter_io_saturate),
    .io_done(burstCounter_io_done)
  );
  Counter_1 burstTagCounter (
    .clock(burstTagCounter_clock),
    .reset(burstTagCounter_reset),
    .io_max(burstTagCounter_io_max),
    .io_stride(burstTagCounter_io_stride),
    .io_out(burstTagCounter_io_out),
    .io_next(burstTagCounter_io_next),
    .io_reset(burstTagCounter_io_reset),
    .io_enable(burstTagCounter_io_enable),
    .io_saturate(burstTagCounter_io_saturate),
    .io_done(burstTagCounter_io_done)
  );
  assign io_dram_cmd_valid = _T_173;
  assign io_dram_cmd_bits_addr = _T_160;
  assign io_dram_cmd_bits_isWr = isWrFifo_io_deq_0;
  assign io_dram_cmd_bits_tag = _T_161;
  assign io_dram_cmd_bits_streamId = {{31'd0}, tagOut_streamTag};
  assign io_dram_cmd_bits_wdata_0 = wdataFifo_io_deq_0;
  assign io_dram_cmd_bits_wdata_1 = wdataFifo_io_deq_1;
  assign io_dram_cmd_bits_wdata_2 = wdataFifo_io_deq_2;
  assign io_dram_cmd_bits_wdata_3 = wdataFifo_io_deq_3;
  assign io_dram_cmd_bits_wdata_4 = wdataFifo_io_deq_4;
  assign io_dram_cmd_bits_wdata_5 = wdataFifo_io_deq_5;
  assign io_dram_cmd_bits_wdata_6 = wdataFifo_io_deq_6;
  assign io_dram_cmd_bits_wdata_7 = wdataFifo_io_deq_7;
  assign io_dram_cmd_bits_wdata_8 = wdataFifo_io_deq_8;
  assign io_dram_cmd_bits_wdata_9 = wdataFifo_io_deq_9;
  assign io_dram_cmd_bits_wdata_10 = wdataFifo_io_deq_10;
  assign io_dram_cmd_bits_wdata_11 = wdataFifo_io_deq_11;
  assign io_dram_cmd_bits_wdata_12 = wdataFifo_io_deq_12;
  assign io_dram_cmd_bits_wdata_13 = wdataFifo_io_deq_13;
  assign io_dram_cmd_bits_wdata_14 = wdataFifo_io_deq_14;
  assign io_dram_cmd_bits_wdata_15 = wdataFifo_io_deq_15;
  assign io_dram_resp_ready = 1'h1;
  assign addrFifo_clock = clock;
  assign addrFifo_reset = reset;
  assign addrFifo_io_deqVld = burstCounter_io_done;
  assign addrFifo_io_forceTag_valid = 1'h0;
  assign addrFifo_io_forceTag_bits = _GEN_2;
  assign addrFifo_io_config_chainWrite = addrFifoConfig_chainWrite;
  assign addrFifo_io_config_chainRead = addrFifoConfig_chainRead;
  assign addrFifoConfig_chainWrite = _T_84;
  assign addrFifoConfig_chainRead = 1'h1;
  assign _T_84 = ~ io_config_scatterGather;
  assign burstAddrs_0 = addrFifo_io_deq_0[63:6];
  assign isWrFifo_clock = clock;
  assign isWrFifo_reset = reset;
  assign isWrFifo_io_deqVld = burstCounter_io_done;
  assign isWrFifo_io_forceTag_valid = 1'h0;
  assign isWrFifo_io_forceTag_bits = _GEN_3;
  assign isWrFifo_io_config_chainWrite = isWrFifoConfig_chainWrite;
  assign isWrFifo_io_config_chainRead = isWrFifoConfig_chainRead;
  assign isWrFifoConfig_chainWrite = 1'h1;
  assign isWrFifoConfig_chainRead = 1'h1;
  assign sizeFifo_clock = clock;
  assign sizeFifo_reset = reset;
  assign sizeFifo_io_deqVld = burstCounter_io_done;
  assign sizeFifo_io_forceTag_valid = 1'h0;
  assign sizeFifo_io_forceTag_bits = _GEN_4;
  assign sizeFifo_io_config_chainWrite = sizeFifoConfig_chainWrite;
  assign sizeFifo_io_config_chainRead = sizeFifoConfig_chainRead;
  assign sizeFifoConfig_chainWrite = 1'h1;
  assign sizeFifoConfig_chainRead = 1'h1;
  assign _T_102 = sizeFifo_io_deq_0[31:6];
  assign _T_103 = sizeFifo_io_deq_0[5:0];
  assign _T_105 = _T_103 != 6'h0;
  assign _GEN_0 = {{25'd0}, _T_105};
  assign _T_106 = _T_102 + _GEN_0;
  assign sizeInBursts = _T_106[25:0];
  assign wdataFifo_clock = clock;
  assign wdataFifo_reset = reset;
  assign wdataFifo_io_deqVld = _T_150;
  assign wdataFifo_io_forceTag_valid = 1'h1;
  assign wdataFifo_io_forceTag_bits = _T_125;
  assign wrPhase_clock = clock;
  assign wrPhase_reset = reset;
  assign wrPhase_io_input_set = _T_163;
  assign wrPhase_io_input_reset = _T_166;
  assign wrPhase_io_input_asyn_reset = _GEN_5;
  assign _T_107 = ~ sizeFifo_io_empty;
  assign _T_108 = ~ isWrFifo_io_empty;
  assign _T_109 = isWrFifo_io_deq_0;
  assign _T_110 = _T_108 & _T_109;
  assign _T_111 = wrPhase_io_output_data | _T_110;
  assign _T_112 = ~ wdataFifo_io_empty;
  assign _T_114 = _T_111 ? _T_112 : 1'h1;
  assign burstVld = _T_107 & _T_114;
  assign issued = 1'h0;
  assign issuedFF_clock = clock;
  assign issuedFF_reset = reset;
  assign issuedFF_io_in = _T_119;
  assign issuedFF_io_init = 1'h0;
  assign issuedFF_io_enable = 1'h1;
  assign _T_117 = ~ io_dram_resp_valid;
  assign _T_118 = burstVld & io_dram_cmd_ready;
  assign _T_119 = issued ? _T_117 : _T_118;
  assign _T_123 = addrFifo_io_tag - 1'h0;
  assign _T_124 = $unsigned(_T_123);
  assign _T_125 = _T_124[0:0];
  assign burstCounter_clock = clock;
  assign burstCounter_reset = reset;
  assign burstCounter_io_max = {{6'd0}, _T_129};
  assign burstCounter_io_stride = 32'h1;
  assign burstCounter_io_reset = 1'h0;
  assign burstCounter_io_enable = _T_136;
  assign burstCounter_io_saturate = 1'h0;
  assign _T_129 = io_config_scatterGather ? 26'h1 : sizeInBursts;
  assign _T_132 = ~ addrFifo_io_empty;
  assign _T_133 = io_config_scatterGather ? _T_132 : burstVld;
  assign _T_134 = _T_133 & io_dram_cmd_ready;
  assign _T_135 = ~ issued;
  assign _T_136 = _T_134 & _T_135;
  assign burstTagCounter_clock = clock;
  assign burstTagCounter_reset = reset;
  assign burstTagCounter_io_max = 11'h400;
  assign burstTagCounter_io_stride = 11'h1;
  assign burstTagCounter_io_reset = 1'h0;
  assign burstTagCounter_io_enable = _T_136;
  assign burstTagCounter_io_saturate = _GEN_6;
  assign _T_147 = burstVld & isWrFifo_io_deq_0;
  assign _T_148 = _T_147 & io_dram_cmd_ready;
  assign _T_150 = _T_148 & _T_135;
  assign tagOut_streamTag = addrFifo_io_tag;
  assign tagOut_burstTag = _T_156[30:0];
  assign _T_156 = io_config_scatterGather ? burstAddrs_0 : {{47'd0}, burstTagCounter_io_out};
  assign _GEN_1 = {{26'd0}, burstCounter_io_out};
  assign _T_157 = burstAddrs_0 + _GEN_1;
  assign _T_158 = _T_157[57:0];
  assign _T_160 = {_T_158,6'h0};
  assign _T_161 = {tagOut_streamTag,tagOut_burstTag};
  assign _T_163 = _T_108 & isWrFifo_io_deq_0;
  assign _T_173 = burstVld & _T_135;
  assign issuedTag = 32'h0;
`ifdef RANDOMIZE
  integer initvar;
  initial begin
    `ifndef verilator
      #0.002 begin end
    `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_7 = {1{$random}};
  _T_166 = _GEN_7[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_8 = {1{$random}};
  _GEN_2 = _GEN_8[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_9 = {1{$random}};
  _GEN_3 = _GEN_9[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_10 = {1{$random}};
  _GEN_4 = _GEN_10[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_11 = {1{$random}};
  _GEN_5 = _GEN_11[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_12 = {1{$random}};
  _GEN_6 = _GEN_12[0:0];
  `endif
  end
`endif
  always @(posedge clock) begin
    if (reset) begin
      _T_166 <= 1'h0;
    end else begin
      _T_166 <= burstVld;
    end
  end
endmodule
module Fringe(
  input         clock,
  input         reset,
  input         io_raddr,
  input         io_wen,
  input         io_waddr,
  input  [63:0] io_wdata,
  output [63:0] io_rdata,
  output        io_enable,
  input         io_done,
  input         io_dram_cmd_ready,
  output        io_dram_cmd_valid,
  output [63:0] io_dram_cmd_bits_addr,
  output        io_dram_cmd_bits_isWr,
  output [31:0] io_dram_cmd_bits_tag,
  output [31:0] io_dram_cmd_bits_streamId,
  output [31:0] io_dram_cmd_bits_wdata_0,
  output [31:0] io_dram_cmd_bits_wdata_1,
  output [31:0] io_dram_cmd_bits_wdata_2,
  output [31:0] io_dram_cmd_bits_wdata_3,
  output [31:0] io_dram_cmd_bits_wdata_4,
  output [31:0] io_dram_cmd_bits_wdata_5,
  output [31:0] io_dram_cmd_bits_wdata_6,
  output [31:0] io_dram_cmd_bits_wdata_7,
  output [31:0] io_dram_cmd_bits_wdata_8,
  output [31:0] io_dram_cmd_bits_wdata_9,
  output [31:0] io_dram_cmd_bits_wdata_10,
  output [31:0] io_dram_cmd_bits_wdata_11,
  output [31:0] io_dram_cmd_bits_wdata_12,
  output [31:0] io_dram_cmd_bits_wdata_13,
  output [31:0] io_dram_cmd_bits_wdata_14,
  output [31:0] io_dram_cmd_bits_wdata_15,
  output        io_dram_resp_ready,
  input         io_dram_resp_valid,
  input  [31:0] io_dram_resp_bits_rdata_0,
  input  [31:0] io_dram_resp_bits_rdata_1,
  input  [31:0] io_dram_resp_bits_rdata_2,
  input  [31:0] io_dram_resp_bits_rdata_3,
  input  [31:0] io_dram_resp_bits_rdata_4,
  input  [31:0] io_dram_resp_bits_rdata_5,
  input  [31:0] io_dram_resp_bits_rdata_6,
  input  [31:0] io_dram_resp_bits_rdata_7,
  input  [31:0] io_dram_resp_bits_rdata_8,
  input  [31:0] io_dram_resp_bits_rdata_9,
  input  [31:0] io_dram_resp_bits_rdata_10,
  input  [31:0] io_dram_resp_bits_rdata_11,
  input  [31:0] io_dram_resp_bits_rdata_12,
  input  [31:0] io_dram_resp_bits_rdata_13,
  input  [31:0] io_dram_resp_bits_rdata_14,
  input  [31:0] io_dram_resp_bits_rdata_15,
  input  [31:0] io_dram_resp_bits_tag,
  input  [31:0] io_dram_resp_bits_streamId,
  input         io_genericStreamOutTop_ready,
  output        io_genericStreamOutTop_valid,
  output [31:0] io_genericStreamOutTop_bits_data,
  output [31:0] io_genericStreamOutTop_bits_tag,
  output        io_genericStreamOutTop_bits_last,
  output        io_genericStreamInTop_ready,
  input         io_genericStreamInTop_valid,
  input  [31:0] io_genericStreamInTop_bits_data,
  input  [31:0] io_genericStreamInTop_bits_tag,
  input         io_genericStreamInTop_bits_last
);
  wire  regs_clock;
  wire  regs_reset;
  wire  regs_io_raddr;
  wire  regs_io_wen;
  wire  regs_io_waddr;
  wire [63:0] regs_io_wdata;
  wire [63:0] regs_io_rdata;
  wire [63:0] regs_io_argIns_0;
  wire [63:0] regs_io_argIns_1;
  wire  regs_io_argOuts_0_ready;
  wire  regs_io_argOuts_0_valid;
  wire [63:0] regs_io_argOuts_0_bits;
  wire  _T_171;
  wire  _T_172;
  wire  _T_173;
  wire  _T_174;
  wire  depulser_clock;
  wire  depulser_reset;
  wire  depulser_io_in;
  wire  depulser_io_rst;
  wire  depulser_io_out;
  wire [63:0] _T_175;
  wire  status_ready;
  wire  status_valid;
  wire [63:0] status_bits;
  wire [63:0] _GEN_0;
  wire [63:0] _T_190;
  wire  mag_clock;
  wire  mag_reset;
  wire  mag_io_dram_cmd_ready;
  wire  mag_io_dram_cmd_valid;
  wire [63:0] mag_io_dram_cmd_bits_addr;
  wire  mag_io_dram_cmd_bits_isWr;
  wire [31:0] mag_io_dram_cmd_bits_tag;
  wire [31:0] mag_io_dram_cmd_bits_streamId;
  wire [31:0] mag_io_dram_cmd_bits_wdata_0;
  wire [31:0] mag_io_dram_cmd_bits_wdata_1;
  wire [31:0] mag_io_dram_cmd_bits_wdata_2;
  wire [31:0] mag_io_dram_cmd_bits_wdata_3;
  wire [31:0] mag_io_dram_cmd_bits_wdata_4;
  wire [31:0] mag_io_dram_cmd_bits_wdata_5;
  wire [31:0] mag_io_dram_cmd_bits_wdata_6;
  wire [31:0] mag_io_dram_cmd_bits_wdata_7;
  wire [31:0] mag_io_dram_cmd_bits_wdata_8;
  wire [31:0] mag_io_dram_cmd_bits_wdata_9;
  wire [31:0] mag_io_dram_cmd_bits_wdata_10;
  wire [31:0] mag_io_dram_cmd_bits_wdata_11;
  wire [31:0] mag_io_dram_cmd_bits_wdata_12;
  wire [31:0] mag_io_dram_cmd_bits_wdata_13;
  wire [31:0] mag_io_dram_cmd_bits_wdata_14;
  wire [31:0] mag_io_dram_cmd_bits_wdata_15;
  wire  mag_io_dram_resp_ready;
  wire  mag_io_dram_resp_valid;
  wire [31:0] mag_io_dram_resp_bits_rdata_0;
  wire [31:0] mag_io_dram_resp_bits_rdata_1;
  wire [31:0] mag_io_dram_resp_bits_rdata_2;
  wire [31:0] mag_io_dram_resp_bits_rdata_3;
  wire [31:0] mag_io_dram_resp_bits_rdata_4;
  wire [31:0] mag_io_dram_resp_bits_rdata_5;
  wire [31:0] mag_io_dram_resp_bits_rdata_6;
  wire [31:0] mag_io_dram_resp_bits_rdata_7;
  wire [31:0] mag_io_dram_resp_bits_rdata_8;
  wire [31:0] mag_io_dram_resp_bits_rdata_9;
  wire [31:0] mag_io_dram_resp_bits_rdata_10;
  wire [31:0] mag_io_dram_resp_bits_rdata_11;
  wire [31:0] mag_io_dram_resp_bits_rdata_12;
  wire [31:0] mag_io_dram_resp_bits_rdata_13;
  wire [31:0] mag_io_dram_resp_bits_rdata_14;
  wire [31:0] mag_io_dram_resp_bits_rdata_15;
  wire [31:0] mag_io_dram_resp_bits_tag;
  wire [31:0] mag_io_dram_resp_bits_streamId;
  wire  mag_io_config_scatterGather;
  wire  magConfig_scatterGather;
  reg  _GEN_1;
  reg [31:0] _GEN_7;
  reg [31:0] _GEN_2;
  reg [31:0] _GEN_8;
  reg [31:0] _GEN_3;
  reg [31:0] _GEN_9;
  reg  _GEN_4;
  reg [31:0] _GEN_10;
  reg  _GEN_5;
  reg [31:0] _GEN_11;
  reg  _GEN_6;
  reg [31:0] _GEN_12;
  RegFile regs (
    .clock(regs_clock),
    .reset(regs_reset),
    .io_raddr(regs_io_raddr),
    .io_wen(regs_io_wen),
    .io_waddr(regs_io_waddr),
    .io_wdata(regs_io_wdata),
    .io_rdata(regs_io_rdata),
    .io_argIns_0(regs_io_argIns_0),
    .io_argIns_1(regs_io_argIns_1),
    .io_argOuts_0_ready(regs_io_argOuts_0_ready),
    .io_argOuts_0_valid(regs_io_argOuts_0_valid),
    .io_argOuts_0_bits(regs_io_argOuts_0_bits)
  );
  Depulser depulser (
    .clock(depulser_clock),
    .reset(depulser_reset),
    .io_in(depulser_io_in),
    .io_rst(depulser_io_rst),
    .io_out(depulser_io_out)
  );
  MAGCore mag (
    .clock(mag_clock),
    .reset(mag_reset),
    .io_dram_cmd_ready(mag_io_dram_cmd_ready),
    .io_dram_cmd_valid(mag_io_dram_cmd_valid),
    .io_dram_cmd_bits_addr(mag_io_dram_cmd_bits_addr),
    .io_dram_cmd_bits_isWr(mag_io_dram_cmd_bits_isWr),
    .io_dram_cmd_bits_tag(mag_io_dram_cmd_bits_tag),
    .io_dram_cmd_bits_streamId(mag_io_dram_cmd_bits_streamId),
    .io_dram_cmd_bits_wdata_0(mag_io_dram_cmd_bits_wdata_0),
    .io_dram_cmd_bits_wdata_1(mag_io_dram_cmd_bits_wdata_1),
    .io_dram_cmd_bits_wdata_2(mag_io_dram_cmd_bits_wdata_2),
    .io_dram_cmd_bits_wdata_3(mag_io_dram_cmd_bits_wdata_3),
    .io_dram_cmd_bits_wdata_4(mag_io_dram_cmd_bits_wdata_4),
    .io_dram_cmd_bits_wdata_5(mag_io_dram_cmd_bits_wdata_5),
    .io_dram_cmd_bits_wdata_6(mag_io_dram_cmd_bits_wdata_6),
    .io_dram_cmd_bits_wdata_7(mag_io_dram_cmd_bits_wdata_7),
    .io_dram_cmd_bits_wdata_8(mag_io_dram_cmd_bits_wdata_8),
    .io_dram_cmd_bits_wdata_9(mag_io_dram_cmd_bits_wdata_9),
    .io_dram_cmd_bits_wdata_10(mag_io_dram_cmd_bits_wdata_10),
    .io_dram_cmd_bits_wdata_11(mag_io_dram_cmd_bits_wdata_11),
    .io_dram_cmd_bits_wdata_12(mag_io_dram_cmd_bits_wdata_12),
    .io_dram_cmd_bits_wdata_13(mag_io_dram_cmd_bits_wdata_13),
    .io_dram_cmd_bits_wdata_14(mag_io_dram_cmd_bits_wdata_14),
    .io_dram_cmd_bits_wdata_15(mag_io_dram_cmd_bits_wdata_15),
    .io_dram_resp_ready(mag_io_dram_resp_ready),
    .io_dram_resp_valid(mag_io_dram_resp_valid),
    .io_dram_resp_bits_rdata_0(mag_io_dram_resp_bits_rdata_0),
    .io_dram_resp_bits_rdata_1(mag_io_dram_resp_bits_rdata_1),
    .io_dram_resp_bits_rdata_2(mag_io_dram_resp_bits_rdata_2),
    .io_dram_resp_bits_rdata_3(mag_io_dram_resp_bits_rdata_3),
    .io_dram_resp_bits_rdata_4(mag_io_dram_resp_bits_rdata_4),
    .io_dram_resp_bits_rdata_5(mag_io_dram_resp_bits_rdata_5),
    .io_dram_resp_bits_rdata_6(mag_io_dram_resp_bits_rdata_6),
    .io_dram_resp_bits_rdata_7(mag_io_dram_resp_bits_rdata_7),
    .io_dram_resp_bits_rdata_8(mag_io_dram_resp_bits_rdata_8),
    .io_dram_resp_bits_rdata_9(mag_io_dram_resp_bits_rdata_9),
    .io_dram_resp_bits_rdata_10(mag_io_dram_resp_bits_rdata_10),
    .io_dram_resp_bits_rdata_11(mag_io_dram_resp_bits_rdata_11),
    .io_dram_resp_bits_rdata_12(mag_io_dram_resp_bits_rdata_12),
    .io_dram_resp_bits_rdata_13(mag_io_dram_resp_bits_rdata_13),
    .io_dram_resp_bits_rdata_14(mag_io_dram_resp_bits_rdata_14),
    .io_dram_resp_bits_rdata_15(mag_io_dram_resp_bits_rdata_15),
    .io_dram_resp_bits_tag(mag_io_dram_resp_bits_tag),
    .io_dram_resp_bits_streamId(mag_io_dram_resp_bits_streamId),
    .io_config_scatterGather(mag_io_config_scatterGather)
  );
  assign io_rdata = regs_io_rdata;
  assign io_enable = _T_174;
  assign io_dram_cmd_valid = mag_io_dram_cmd_valid;
  assign io_dram_cmd_bits_addr = mag_io_dram_cmd_bits_addr;
  assign io_dram_cmd_bits_isWr = mag_io_dram_cmd_bits_isWr;
  assign io_dram_cmd_bits_tag = mag_io_dram_cmd_bits_tag;
  assign io_dram_cmd_bits_streamId = mag_io_dram_cmd_bits_streamId;
  assign io_dram_cmd_bits_wdata_0 = mag_io_dram_cmd_bits_wdata_0;
  assign io_dram_cmd_bits_wdata_1 = mag_io_dram_cmd_bits_wdata_1;
  assign io_dram_cmd_bits_wdata_2 = mag_io_dram_cmd_bits_wdata_2;
  assign io_dram_cmd_bits_wdata_3 = mag_io_dram_cmd_bits_wdata_3;
  assign io_dram_cmd_bits_wdata_4 = mag_io_dram_cmd_bits_wdata_4;
  assign io_dram_cmd_bits_wdata_5 = mag_io_dram_cmd_bits_wdata_5;
  assign io_dram_cmd_bits_wdata_6 = mag_io_dram_cmd_bits_wdata_6;
  assign io_dram_cmd_bits_wdata_7 = mag_io_dram_cmd_bits_wdata_7;
  assign io_dram_cmd_bits_wdata_8 = mag_io_dram_cmd_bits_wdata_8;
  assign io_dram_cmd_bits_wdata_9 = mag_io_dram_cmd_bits_wdata_9;
  assign io_dram_cmd_bits_wdata_10 = mag_io_dram_cmd_bits_wdata_10;
  assign io_dram_cmd_bits_wdata_11 = mag_io_dram_cmd_bits_wdata_11;
  assign io_dram_cmd_bits_wdata_12 = mag_io_dram_cmd_bits_wdata_12;
  assign io_dram_cmd_bits_wdata_13 = mag_io_dram_cmd_bits_wdata_13;
  assign io_dram_cmd_bits_wdata_14 = mag_io_dram_cmd_bits_wdata_14;
  assign io_dram_cmd_bits_wdata_15 = mag_io_dram_cmd_bits_wdata_15;
  assign io_dram_resp_ready = mag_io_dram_resp_ready;
  assign io_genericStreamOutTop_valid = _GEN_1;
  assign io_genericStreamOutTop_bits_data = _GEN_2;
  assign io_genericStreamOutTop_bits_tag = _GEN_3;
  assign io_genericStreamOutTop_bits_last = _GEN_4;
  assign io_genericStreamInTop_ready = _GEN_5;
  assign regs_clock = clock;
  assign regs_reset = reset;
  assign regs_io_raddr = io_raddr;
  assign regs_io_wen = io_wen;
  assign regs_io_waddr = io_waddr;
  assign regs_io_wdata = io_wdata;
  assign regs_io_argOuts_0_valid = status_valid;
  assign regs_io_argOuts_0_bits = status_bits;
  assign _T_171 = regs_io_argIns_0[0];
  assign _T_172 = regs_io_argIns_1[0];
  assign _T_173 = ~ _T_172;
  assign _T_174 = _T_171 & _T_173;
  assign depulser_clock = clock;
  assign depulser_reset = reset;
  assign depulser_io_in = io_done;
  assign depulser_io_rst = _T_175[0];
  assign _T_175 = ~ regs_io_argIns_0;
  assign status_ready = _GEN_6;
  assign status_valid = depulser_io_out;
  assign status_bits = _T_190;
  assign _GEN_0 = {{63'd0}, depulser_io_out};
  assign _T_190 = regs_io_argIns_0 & _GEN_0;
  assign mag_clock = clock;
  assign mag_reset = reset;
  assign mag_io_dram_cmd_ready = io_dram_cmd_ready;
  assign mag_io_dram_resp_valid = io_dram_resp_valid;
  assign mag_io_dram_resp_bits_rdata_0 = io_dram_resp_bits_rdata_0;
  assign mag_io_dram_resp_bits_rdata_1 = io_dram_resp_bits_rdata_1;
  assign mag_io_dram_resp_bits_rdata_2 = io_dram_resp_bits_rdata_2;
  assign mag_io_dram_resp_bits_rdata_3 = io_dram_resp_bits_rdata_3;
  assign mag_io_dram_resp_bits_rdata_4 = io_dram_resp_bits_rdata_4;
  assign mag_io_dram_resp_bits_rdata_5 = io_dram_resp_bits_rdata_5;
  assign mag_io_dram_resp_bits_rdata_6 = io_dram_resp_bits_rdata_6;
  assign mag_io_dram_resp_bits_rdata_7 = io_dram_resp_bits_rdata_7;
  assign mag_io_dram_resp_bits_rdata_8 = io_dram_resp_bits_rdata_8;
  assign mag_io_dram_resp_bits_rdata_9 = io_dram_resp_bits_rdata_9;
  assign mag_io_dram_resp_bits_rdata_10 = io_dram_resp_bits_rdata_10;
  assign mag_io_dram_resp_bits_rdata_11 = io_dram_resp_bits_rdata_11;
  assign mag_io_dram_resp_bits_rdata_12 = io_dram_resp_bits_rdata_12;
  assign mag_io_dram_resp_bits_rdata_13 = io_dram_resp_bits_rdata_13;
  assign mag_io_dram_resp_bits_rdata_14 = io_dram_resp_bits_rdata_14;
  assign mag_io_dram_resp_bits_rdata_15 = io_dram_resp_bits_rdata_15;
  assign mag_io_dram_resp_bits_tag = io_dram_resp_bits_tag;
  assign mag_io_dram_resp_bits_streamId = io_dram_resp_bits_streamId;
  assign mag_io_config_scatterGather = magConfig_scatterGather;
  assign magConfig_scatterGather = 1'h0;
`ifdef RANDOMIZE
  integer initvar;
  initial begin
    `ifndef verilator
      #0.002 begin end
    `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_7 = {1{$random}};
  _GEN_1 = _GEN_7[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_8 = {1{$random}};
  _GEN_2 = _GEN_8[31:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_9 = {1{$random}};
  _GEN_3 = _GEN_9[31:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_10 = {1{$random}};
  _GEN_4 = _GEN_10[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_11 = {1{$random}};
  _GEN_5 = _GEN_11[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_12 = {1{$random}};
  _GEN_6 = _GEN_12[0:0];
  `endif
  end
`endif
endmodule
module FringeDE1SoC(
  input         clock,
  input         reset,
  output [31:0] io_S_AVALON_readdata,
  input  [15:0] io_S_AVALON_address,
  input         io_S_AVALON_chipselect,
  input         io_S_AVALON_write_n,
  input  [31:0] io_S_AVALON_writedata,
  output        io_enable,
  input         io_done
);
  wire  fringeCommon_clock;
  wire  fringeCommon_reset;
  wire  fringeCommon_io_raddr;
  wire  fringeCommon_io_wen;
  wire  fringeCommon_io_waddr;
  wire [63:0] fringeCommon_io_wdata;
  wire [63:0] fringeCommon_io_rdata;
  wire  fringeCommon_io_enable;
  wire  fringeCommon_io_done;
  wire  fringeCommon_io_dram_cmd_ready;
  wire  fringeCommon_io_dram_cmd_valid;
  wire [63:0] fringeCommon_io_dram_cmd_bits_addr;
  wire  fringeCommon_io_dram_cmd_bits_isWr;
  wire [31:0] fringeCommon_io_dram_cmd_bits_tag;
  wire [31:0] fringeCommon_io_dram_cmd_bits_streamId;
  wire [31:0] fringeCommon_io_dram_cmd_bits_wdata_0;
  wire [31:0] fringeCommon_io_dram_cmd_bits_wdata_1;
  wire [31:0] fringeCommon_io_dram_cmd_bits_wdata_2;
  wire [31:0] fringeCommon_io_dram_cmd_bits_wdata_3;
  wire [31:0] fringeCommon_io_dram_cmd_bits_wdata_4;
  wire [31:0] fringeCommon_io_dram_cmd_bits_wdata_5;
  wire [31:0] fringeCommon_io_dram_cmd_bits_wdata_6;
  wire [31:0] fringeCommon_io_dram_cmd_bits_wdata_7;
  wire [31:0] fringeCommon_io_dram_cmd_bits_wdata_8;
  wire [31:0] fringeCommon_io_dram_cmd_bits_wdata_9;
  wire [31:0] fringeCommon_io_dram_cmd_bits_wdata_10;
  wire [31:0] fringeCommon_io_dram_cmd_bits_wdata_11;
  wire [31:0] fringeCommon_io_dram_cmd_bits_wdata_12;
  wire [31:0] fringeCommon_io_dram_cmd_bits_wdata_13;
  wire [31:0] fringeCommon_io_dram_cmd_bits_wdata_14;
  wire [31:0] fringeCommon_io_dram_cmd_bits_wdata_15;
  wire  fringeCommon_io_dram_resp_ready;
  wire  fringeCommon_io_dram_resp_valid;
  wire [31:0] fringeCommon_io_dram_resp_bits_rdata_0;
  wire [31:0] fringeCommon_io_dram_resp_bits_rdata_1;
  wire [31:0] fringeCommon_io_dram_resp_bits_rdata_2;
  wire [31:0] fringeCommon_io_dram_resp_bits_rdata_3;
  wire [31:0] fringeCommon_io_dram_resp_bits_rdata_4;
  wire [31:0] fringeCommon_io_dram_resp_bits_rdata_5;
  wire [31:0] fringeCommon_io_dram_resp_bits_rdata_6;
  wire [31:0] fringeCommon_io_dram_resp_bits_rdata_7;
  wire [31:0] fringeCommon_io_dram_resp_bits_rdata_8;
  wire [31:0] fringeCommon_io_dram_resp_bits_rdata_9;
  wire [31:0] fringeCommon_io_dram_resp_bits_rdata_10;
  wire [31:0] fringeCommon_io_dram_resp_bits_rdata_11;
  wire [31:0] fringeCommon_io_dram_resp_bits_rdata_12;
  wire [31:0] fringeCommon_io_dram_resp_bits_rdata_13;
  wire [31:0] fringeCommon_io_dram_resp_bits_rdata_14;
  wire [31:0] fringeCommon_io_dram_resp_bits_rdata_15;
  wire [31:0] fringeCommon_io_dram_resp_bits_tag;
  wire [31:0] fringeCommon_io_dram_resp_bits_streamId;
  wire  fringeCommon_io_genericStreamOutTop_ready;
  wire  fringeCommon_io_genericStreamOutTop_valid;
  wire [31:0] fringeCommon_io_genericStreamOutTop_bits_data;
  wire [31:0] fringeCommon_io_genericStreamOutTop_bits_tag;
  wire  fringeCommon_io_genericStreamOutTop_bits_last;
  wire  fringeCommon_io_genericStreamInTop_ready;
  wire  fringeCommon_io_genericStreamInTop_valid;
  wire [31:0] fringeCommon_io_genericStreamInTop_bits_data;
  wire [31:0] fringeCommon_io_genericStreamInTop_bits_tag;
  wire  fringeCommon_io_genericStreamInTop_bits_last;
  wire  _T_46;
  wire  _T_47;
  reg  _GEN_0;
  reg [31:0] _GEN_25;
  reg  _GEN_1;
  reg [31:0] _GEN_26;
  reg [31:0] _GEN_2;
  reg [31:0] _GEN_27;
  reg [31:0] _GEN_3;
  reg [31:0] _GEN_28;
  reg [31:0] _GEN_4;
  reg [31:0] _GEN_29;
  reg [31:0] _GEN_5;
  reg [31:0] _GEN_30;
  reg [31:0] _GEN_6;
  reg [31:0] _GEN_31;
  reg [31:0] _GEN_7;
  reg [31:0] _GEN_32;
  reg [31:0] _GEN_8;
  reg [31:0] _GEN_33;
  reg [31:0] _GEN_9;
  reg [31:0] _GEN_34;
  reg [31:0] _GEN_10;
  reg [31:0] _GEN_35;
  reg [31:0] _GEN_11;
  reg [31:0] _GEN_36;
  reg [31:0] _GEN_12;
  reg [31:0] _GEN_37;
  reg [31:0] _GEN_13;
  reg [31:0] _GEN_38;
  reg [31:0] _GEN_14;
  reg [31:0] _GEN_39;
  reg [31:0] _GEN_15;
  reg [31:0] _GEN_40;
  reg [31:0] _GEN_16;
  reg [31:0] _GEN_41;
  reg [31:0] _GEN_17;
  reg [31:0] _GEN_42;
  reg [31:0] _GEN_18;
  reg [31:0] _GEN_43;
  reg [31:0] _GEN_19;
  reg [31:0] _GEN_44;
  reg  _GEN_20;
  reg [31:0] _GEN_45;
  reg  _GEN_21;
  reg [31:0] _GEN_46;
  reg [31:0] _GEN_22;
  reg [31:0] _GEN_47;
  reg [31:0] _GEN_23;
  reg [31:0] _GEN_48;
  reg  _GEN_24;
  reg [31:0] _GEN_49;
  Fringe fringeCommon (
    .clock(fringeCommon_clock),
    .reset(fringeCommon_reset),
    .io_raddr(fringeCommon_io_raddr),
    .io_wen(fringeCommon_io_wen),
    .io_waddr(fringeCommon_io_waddr),
    .io_wdata(fringeCommon_io_wdata),
    .io_rdata(fringeCommon_io_rdata),
    .io_enable(fringeCommon_io_enable),
    .io_done(fringeCommon_io_done),
    .io_dram_cmd_ready(fringeCommon_io_dram_cmd_ready),
    .io_dram_cmd_valid(fringeCommon_io_dram_cmd_valid),
    .io_dram_cmd_bits_addr(fringeCommon_io_dram_cmd_bits_addr),
    .io_dram_cmd_bits_isWr(fringeCommon_io_dram_cmd_bits_isWr),
    .io_dram_cmd_bits_tag(fringeCommon_io_dram_cmd_bits_tag),
    .io_dram_cmd_bits_streamId(fringeCommon_io_dram_cmd_bits_streamId),
    .io_dram_cmd_bits_wdata_0(fringeCommon_io_dram_cmd_bits_wdata_0),
    .io_dram_cmd_bits_wdata_1(fringeCommon_io_dram_cmd_bits_wdata_1),
    .io_dram_cmd_bits_wdata_2(fringeCommon_io_dram_cmd_bits_wdata_2),
    .io_dram_cmd_bits_wdata_3(fringeCommon_io_dram_cmd_bits_wdata_3),
    .io_dram_cmd_bits_wdata_4(fringeCommon_io_dram_cmd_bits_wdata_4),
    .io_dram_cmd_bits_wdata_5(fringeCommon_io_dram_cmd_bits_wdata_5),
    .io_dram_cmd_bits_wdata_6(fringeCommon_io_dram_cmd_bits_wdata_6),
    .io_dram_cmd_bits_wdata_7(fringeCommon_io_dram_cmd_bits_wdata_7),
    .io_dram_cmd_bits_wdata_8(fringeCommon_io_dram_cmd_bits_wdata_8),
    .io_dram_cmd_bits_wdata_9(fringeCommon_io_dram_cmd_bits_wdata_9),
    .io_dram_cmd_bits_wdata_10(fringeCommon_io_dram_cmd_bits_wdata_10),
    .io_dram_cmd_bits_wdata_11(fringeCommon_io_dram_cmd_bits_wdata_11),
    .io_dram_cmd_bits_wdata_12(fringeCommon_io_dram_cmd_bits_wdata_12),
    .io_dram_cmd_bits_wdata_13(fringeCommon_io_dram_cmd_bits_wdata_13),
    .io_dram_cmd_bits_wdata_14(fringeCommon_io_dram_cmd_bits_wdata_14),
    .io_dram_cmd_bits_wdata_15(fringeCommon_io_dram_cmd_bits_wdata_15),
    .io_dram_resp_ready(fringeCommon_io_dram_resp_ready),
    .io_dram_resp_valid(fringeCommon_io_dram_resp_valid),
    .io_dram_resp_bits_rdata_0(fringeCommon_io_dram_resp_bits_rdata_0),
    .io_dram_resp_bits_rdata_1(fringeCommon_io_dram_resp_bits_rdata_1),
    .io_dram_resp_bits_rdata_2(fringeCommon_io_dram_resp_bits_rdata_2),
    .io_dram_resp_bits_rdata_3(fringeCommon_io_dram_resp_bits_rdata_3),
    .io_dram_resp_bits_rdata_4(fringeCommon_io_dram_resp_bits_rdata_4),
    .io_dram_resp_bits_rdata_5(fringeCommon_io_dram_resp_bits_rdata_5),
    .io_dram_resp_bits_rdata_6(fringeCommon_io_dram_resp_bits_rdata_6),
    .io_dram_resp_bits_rdata_7(fringeCommon_io_dram_resp_bits_rdata_7),
    .io_dram_resp_bits_rdata_8(fringeCommon_io_dram_resp_bits_rdata_8),
    .io_dram_resp_bits_rdata_9(fringeCommon_io_dram_resp_bits_rdata_9),
    .io_dram_resp_bits_rdata_10(fringeCommon_io_dram_resp_bits_rdata_10),
    .io_dram_resp_bits_rdata_11(fringeCommon_io_dram_resp_bits_rdata_11),
    .io_dram_resp_bits_rdata_12(fringeCommon_io_dram_resp_bits_rdata_12),
    .io_dram_resp_bits_rdata_13(fringeCommon_io_dram_resp_bits_rdata_13),
    .io_dram_resp_bits_rdata_14(fringeCommon_io_dram_resp_bits_rdata_14),
    .io_dram_resp_bits_rdata_15(fringeCommon_io_dram_resp_bits_rdata_15),
    .io_dram_resp_bits_tag(fringeCommon_io_dram_resp_bits_tag),
    .io_dram_resp_bits_streamId(fringeCommon_io_dram_resp_bits_streamId),
    .io_genericStreamOutTop_ready(fringeCommon_io_genericStreamOutTop_ready),
    .io_genericStreamOutTop_valid(fringeCommon_io_genericStreamOutTop_valid),
    .io_genericStreamOutTop_bits_data(fringeCommon_io_genericStreamOutTop_bits_data),
    .io_genericStreamOutTop_bits_tag(fringeCommon_io_genericStreamOutTop_bits_tag),
    .io_genericStreamOutTop_bits_last(fringeCommon_io_genericStreamOutTop_bits_last),
    .io_genericStreamInTop_ready(fringeCommon_io_genericStreamInTop_ready),
    .io_genericStreamInTop_valid(fringeCommon_io_genericStreamInTop_valid),
    .io_genericStreamInTop_bits_data(fringeCommon_io_genericStreamInTop_bits_data),
    .io_genericStreamInTop_bits_tag(fringeCommon_io_genericStreamInTop_bits_tag),
    .io_genericStreamInTop_bits_last(fringeCommon_io_genericStreamInTop_bits_last)
  );
  assign io_S_AVALON_readdata = fringeCommon_io_rdata[31:0];
  assign io_enable = fringeCommon_io_enable;
  assign fringeCommon_clock = clock;
  assign fringeCommon_reset = reset;
  assign fringeCommon_io_raddr = io_S_AVALON_address[0];
  assign fringeCommon_io_wen = _T_47;
  assign fringeCommon_io_waddr = io_S_AVALON_address[0];
  assign fringeCommon_io_wdata = {{32'd0}, io_S_AVALON_writedata};
  assign fringeCommon_io_done = io_done;
  assign fringeCommon_io_dram_cmd_ready = _GEN_0;
  assign fringeCommon_io_dram_resp_valid = _GEN_1;
  assign fringeCommon_io_dram_resp_bits_rdata_0 = _GEN_2;
  assign fringeCommon_io_dram_resp_bits_rdata_1 = _GEN_3;
  assign fringeCommon_io_dram_resp_bits_rdata_2 = _GEN_4;
  assign fringeCommon_io_dram_resp_bits_rdata_3 = _GEN_5;
  assign fringeCommon_io_dram_resp_bits_rdata_4 = _GEN_6;
  assign fringeCommon_io_dram_resp_bits_rdata_5 = _GEN_7;
  assign fringeCommon_io_dram_resp_bits_rdata_6 = _GEN_8;
  assign fringeCommon_io_dram_resp_bits_rdata_7 = _GEN_9;
  assign fringeCommon_io_dram_resp_bits_rdata_8 = _GEN_10;
  assign fringeCommon_io_dram_resp_bits_rdata_9 = _GEN_11;
  assign fringeCommon_io_dram_resp_bits_rdata_10 = _GEN_12;
  assign fringeCommon_io_dram_resp_bits_rdata_11 = _GEN_13;
  assign fringeCommon_io_dram_resp_bits_rdata_12 = _GEN_14;
  assign fringeCommon_io_dram_resp_bits_rdata_13 = _GEN_15;
  assign fringeCommon_io_dram_resp_bits_rdata_14 = _GEN_16;
  assign fringeCommon_io_dram_resp_bits_rdata_15 = _GEN_17;
  assign fringeCommon_io_dram_resp_bits_tag = _GEN_18;
  assign fringeCommon_io_dram_resp_bits_streamId = _GEN_19;
  assign fringeCommon_io_genericStreamOutTop_ready = _GEN_20;
  assign fringeCommon_io_genericStreamInTop_valid = _GEN_21;
  assign fringeCommon_io_genericStreamInTop_bits_data = _GEN_22;
  assign fringeCommon_io_genericStreamInTop_bits_tag = _GEN_23;
  assign fringeCommon_io_genericStreamInTop_bits_last = _GEN_24;
  assign _T_46 = ~ io_S_AVALON_write_n;
  assign _T_47 = _T_46 & io_S_AVALON_chipselect;
`ifdef RANDOMIZE
  integer initvar;
  initial begin
    `ifndef verilator
      #0.002 begin end
    `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_25 = {1{$random}};
  _GEN_0 = _GEN_25[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_26 = {1{$random}};
  _GEN_1 = _GEN_26[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_27 = {1{$random}};
  _GEN_2 = _GEN_27[31:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_28 = {1{$random}};
  _GEN_3 = _GEN_28[31:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_29 = {1{$random}};
  _GEN_4 = _GEN_29[31:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_30 = {1{$random}};
  _GEN_5 = _GEN_30[31:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_31 = {1{$random}};
  _GEN_6 = _GEN_31[31:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_32 = {1{$random}};
  _GEN_7 = _GEN_32[31:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_33 = {1{$random}};
  _GEN_8 = _GEN_33[31:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_34 = {1{$random}};
  _GEN_9 = _GEN_34[31:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_35 = {1{$random}};
  _GEN_10 = _GEN_35[31:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_36 = {1{$random}};
  _GEN_11 = _GEN_36[31:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_37 = {1{$random}};
  _GEN_12 = _GEN_37[31:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_38 = {1{$random}};
  _GEN_13 = _GEN_38[31:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_39 = {1{$random}};
  _GEN_14 = _GEN_39[31:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_40 = {1{$random}};
  _GEN_15 = _GEN_40[31:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_41 = {1{$random}};
  _GEN_16 = _GEN_41[31:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_42 = {1{$random}};
  _GEN_17 = _GEN_42[31:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_43 = {1{$random}};
  _GEN_18 = _GEN_43[31:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_44 = {1{$random}};
  _GEN_19 = _GEN_44[31:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_45 = {1{$random}};
  _GEN_20 = _GEN_45[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_46 = {1{$random}};
  _GEN_21 = _GEN_46[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_47 = {1{$random}};
  _GEN_22 = _GEN_47[31:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_48 = {1{$random}};
  _GEN_23 = _GEN_48[31:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_49 = {1{$random}};
  _GEN_24 = _GEN_49[0:0];
  `endif
  end
`endif
endmodule
module Top(
  input         clock,
  input         reset,
  input         io_raddr,
  input         io_wen,
  input         io_waddr,
  input         io_wdata,
  output        io_rdata,
  output [31:0] io_S_AVALON_readdata,
  input  [15:0] io_S_AVALON_address,
  input         io_S_AVALON_chipselect,
  input         io_S_AVALON_write_n,
  input  [31:0] io_S_AVALON_writedata,
  input  [23:0] io_S_STREAM_stream_in_data,
  input         io_S_STREAM_stream_in_startofpacket,
  input         io_S_STREAM_stream_in_endofpacket,
  input  [1:0]  io_S_STREAM_stream_in_empty,
  input         io_S_STREAM_stream_in_valid,
  input         io_S_STREAM_stream_out_ready,
  output        io_S_STREAM_stream_in_ready,
  output [15:0] io_S_STREAM_stream_out_data,
  output        io_S_STREAM_stream_out_startofpacket,
  output        io_S_STREAM_stream_out_endofpacket,
  output        io_S_STREAM_stream_out_empty,
  output        io_S_STREAM_stream_out_valid,
  output [3:0]  io_LEDR_STREAM_address,
  output        io_LEDR_STREAM_chipselect,
  output [31:0] io_LEDR_STREAM_writedata,
  output        io_LEDR_STREAM_write_n,
  output [31:0] io_SWITCHES_STREAM_address,
  input  [31:0] io_SWITCHES_STREAM_readdata,
  output        io_SWITCHES_STREAM_read
);
  wire  _T_65;
  wire  _T_70;
  wire  _T_77;
  wire  accel_clock;
  wire  accel_reset;
  wire  accel_io_enable;
  wire  accel_io_done;
  wire [23:0] accel_io_stream_in_data;
  wire  accel_io_stream_in_startofpacket;
  wire  accel_io_stream_in_endofpacket;
  wire [1:0] accel_io_stream_in_empty;
  wire  accel_io_stream_in_valid;
  wire  accel_io_stream_out_ready;
  wire  accel_io_stream_in_ready;
  wire [15:0] accel_io_stream_out_data;
  wire  accel_io_stream_out_startofpacket;
  wire  accel_io_stream_out_endofpacket;
  wire  accel_io_stream_out_empty;
  wire  accel_io_stream_out_valid;
  wire [31:0] accel_io_led_stream_out_data;
  wire [31:0] accel_io_switch_stream_in_data;
  wire  FringeDE1SoC_clock;
  wire  FringeDE1SoC_reset;
  wire [31:0] FringeDE1SoC_io_S_AVALON_readdata;
  wire [15:0] FringeDE1SoC_io_S_AVALON_address;
  wire  FringeDE1SoC_io_S_AVALON_chipselect;
  wire  FringeDE1SoC_io_S_AVALON_write_n;
  wire [31:0] FringeDE1SoC_io_S_AVALON_writedata;
  wire  FringeDE1SoC_io_enable;
  wire  FringeDE1SoC_io_done;
  reg  _GEN_0;
  reg [31:0] _GEN_4;
  reg  _GEN_1;
  reg [31:0] _GEN_5;
  reg  _GEN_2;
  reg [31:0] _GEN_6;
  reg  _GEN_3;
  reg [31:0] _GEN_7;
  AccelTop accel (
    .clock(accel_clock),
    .reset(accel_reset),
    .io_enable(accel_io_enable),
    .io_done(accel_io_done),
    .io_stream_in_data(accel_io_stream_in_data),
    .io_stream_in_startofpacket(accel_io_stream_in_startofpacket),
    .io_stream_in_endofpacket(accel_io_stream_in_endofpacket),
    .io_stream_in_empty(accel_io_stream_in_empty),
    .io_stream_in_valid(accel_io_stream_in_valid),
    .io_stream_out_ready(accel_io_stream_out_ready),
    .io_stream_in_ready(accel_io_stream_in_ready),
    .io_stream_out_data(accel_io_stream_out_data),
    .io_stream_out_startofpacket(accel_io_stream_out_startofpacket),
    .io_stream_out_endofpacket(accel_io_stream_out_endofpacket),
    .io_stream_out_empty(accel_io_stream_out_empty),
    .io_stream_out_valid(accel_io_stream_out_valid),
    .io_led_stream_out_data(accel_io_led_stream_out_data),
    .io_switch_stream_in_data(accel_io_switch_stream_in_data)
  );
  FringeDE1SoC FringeDE1SoC (
    .clock(FringeDE1SoC_clock),
    .reset(FringeDE1SoC_reset),
    .io_S_AVALON_readdata(FringeDE1SoC_io_S_AVALON_readdata),
    .io_S_AVALON_address(FringeDE1SoC_io_S_AVALON_address),
    .io_S_AVALON_chipselect(FringeDE1SoC_io_S_AVALON_chipselect),
    .io_S_AVALON_write_n(FringeDE1SoC_io_S_AVALON_write_n),
    .io_S_AVALON_writedata(FringeDE1SoC_io_S_AVALON_writedata),
    .io_enable(FringeDE1SoC_io_enable),
    .io_done(FringeDE1SoC_io_done)
  );
  assign io_rdata = _GEN_0;
  assign io_S_AVALON_readdata = FringeDE1SoC_io_S_AVALON_readdata;
  assign io_S_STREAM_stream_in_ready = accel_io_stream_in_ready;
  assign io_S_STREAM_stream_out_data = accel_io_stream_out_data;
  assign io_S_STREAM_stream_out_startofpacket = accel_io_stream_out_startofpacket;
  assign io_S_STREAM_stream_out_endofpacket = accel_io_stream_out_endofpacket;
  assign io_S_STREAM_stream_out_empty = accel_io_stream_out_empty;
  assign io_S_STREAM_stream_out_valid = accel_io_stream_out_valid;
  assign io_LEDR_STREAM_address = 4'h0;
  assign io_LEDR_STREAM_chipselect = 1'h1;
  assign io_LEDR_STREAM_writedata = accel_io_led_stream_out_data;
  assign io_LEDR_STREAM_write_n = 1'h0;
  assign io_SWITCHES_STREAM_address = 32'h0;
  assign io_SWITCHES_STREAM_read = 1'h0;
  assign _T_65 = _GEN_1;
  assign _T_70 = _GEN_2;
  assign _T_77 = _GEN_3;
  assign accel_clock = clock;
  assign accel_reset = reset;
  assign accel_io_enable = FringeDE1SoC_io_enable;
  assign accel_io_stream_in_data = io_S_STREAM_stream_in_data;
  assign accel_io_stream_in_startofpacket = io_S_STREAM_stream_in_startofpacket;
  assign accel_io_stream_in_endofpacket = io_S_STREAM_stream_in_endofpacket;
  assign accel_io_stream_in_empty = io_S_STREAM_stream_in_empty;
  assign accel_io_stream_in_valid = io_S_STREAM_stream_in_valid;
  assign accel_io_stream_out_ready = io_S_STREAM_stream_out_ready;
  assign accel_io_switch_stream_in_data = io_SWITCHES_STREAM_readdata;
  assign FringeDE1SoC_clock = clock;
  assign FringeDE1SoC_reset = reset;
  assign FringeDE1SoC_io_S_AVALON_address = io_S_AVALON_address;
  assign FringeDE1SoC_io_S_AVALON_chipselect = io_S_AVALON_chipselect;
  assign FringeDE1SoC_io_S_AVALON_write_n = io_S_AVALON_write_n;
  assign FringeDE1SoC_io_S_AVALON_writedata = io_S_AVALON_writedata;
  assign FringeDE1SoC_io_done = accel_io_done;
`ifdef RANDOMIZE
  integer initvar;
  initial begin
    `ifndef verilator
      #0.002 begin end
    `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_4 = {1{$random}};
  _GEN_0 = _GEN_4[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_5 = {1{$random}};
  _GEN_1 = _GEN_5[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_6 = {1{$random}};
  _GEN_2 = _GEN_6[0:0];
  `endif
  `ifdef RANDOMIZE_REG_INIT
  _GEN_7 = {1{$random}};
  _GEN_3 = _GEN_7[0:0];
  `endif
  end
`endif
endmodule
