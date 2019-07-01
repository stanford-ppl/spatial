module test;
  import "DPI" function void sim_init();
  import "DPI" context function int tick();
  import "DPI" function int sendDRAMRequest(longint addr, longint rawAddr, int size, int tag, int isWr);
  import "DPI" function int sendWdataStrb(int dramCmdValid, int wdata0, int wdata1, int wdata2, int wdata3, int wdata4, int wdata5, int wdata6, int wdata7, int wdata8, int wdata9, int wdata10, int wdata11, int wdata12, int wdata13, int wdata14, int wdata15, int wdata16, int wdata17, int wdata18, int wdata19, int wdata20, int wdata21, int wdata22, int wdata23, int wdata24, int wdata25, int wdata26, int wdata27, int wdata28, int wdata29, int wdata30, int wdata31, int wdata32, int wdata33, int wdata34, int wdata35, int wdata36, int wdata37, int wdata38, int wdata39, int wdata40, int wdata41, int wdata42, int wdata43, int wdata44, int wdata45, int wdata46, int wdata47, int wdata48, int wdata49, int wdata50, int wdata51, int wdata52, int wdata53, int wdata54, int wdata55, int wdata56, int wdata57, int wdata58, int wdata59, int wdata60, int wdata61, int wdata62, int wdata63, int strb0, int strb1, int strb2, int strb3, int strb4, int strb5, int strb6, int strb7, int strb8, int strb9, int strb10, int strb11, int strb12, int strb13, int strb14, int strb15, int strb16, int strb17, int strb18, int strb19, int strb20, int strb21, int strb22, int strb23, int strb24, int strb25, int strb26, int strb27, int strb28, int strb29, int strb30, int strb31, int strb32, int strb33, int strb34, int strb35, int strb36, int strb37, int strb38, int strb39, int strb40, int strb41, int strb42, int strb43, int strb44, int strb45, int strb46, int strb47, int strb48, int strb49, int strb50, int strb51, int strb52, int strb53, int strb54, int strb55, int strb56, int strb57, int strb58, int strb59, int strb60, int strb61, int strb62, int strb63);
  import "DPI" function void serviceWRequest();
  import "DPI" function void popDRAMReadQ();
  import "DPI" function void popDRAMWriteQ();
  import "DPI" function void readOutputStream(int data, int tag, int last);

  // Export functionality to C layer
  export "DPI" function start;
  export "DPI" function rst;
  export "DPI" function writeReg;
  export "DPI" function readRegRaddr;
  export "DPI" function readRegRdataHi32;
  export "DPI" function readRegRdataLo32;
  export "DPI" function pokeDRAMReadResponse;
  export "DPI" function pokeDRAMWriteResponse;
  export "DPI" function getDRAMReadRespReady;
  export "DPI" function getDRAMWriteRespReady;
  export "DPI" function getCycles;
  export "DPI" function writeStream;
  export "DPI" function startVPD;
  export "DPI" function startVCD;
  export "DPI" function stopVPD;
  export "DPI" function stopVCD;
  export "DPI" function terminateSim;

  reg clock = 1;
  reg reset = 1;
  reg finish = 0;

  longint numCycles = 0;

  function void start();
    reset = 0;
    numCycles = 0;
  endfunction

  function void rst();
    reset = 1;
    numCycles = 0;
  endfunction

  always #`CLOCK_PERIOD clock = ~clock;

  /**
  * Variables that control waveform generation
  * vpdon: Generates VPD file
  * vcdon: Generates VCD file
  * Use the "[start|stop][VPD|VCD] functions from sim.cpp to control these variables"
  */
  reg vpdon = 0;
  reg vcdon = 1;
  reg [1023:0] vcdfile = 0;
  reg [1023:0] vpdfile = 0;

  reg [31:0] io_raddr;
  reg io_wen;
  reg [31:0] io_waddr;
  reg [63:0] io_wdata;
  wire [63:0] io_rdata;
  reg io_dram_0_cmd_ready;
  wire  io_dram_0_cmd_valid;
  wire [63:0] io_dram_0_cmd_bits_addr;
  wire [31:0] io_dram_0_cmd_bits_size;
  wire [63:0] io_dram_0_cmd_bits_rawAddr;
  wire  io_dram_0_cmd_bits_isWr;
  wire  io_dram_0_wdata_valid;
  wire  io_dram_0_wdata_bits_wlast;
  reg io_dram_0_wdata_ready;
  wire [31:0] io_dram_0_cmd_bits_tag;
  wire [7:0] io_dram_0_wdata_bits_wdata_0;
  wire [7:0] io_dram_0_wdata_bits_wdata_1;
  wire [7:0] io_dram_0_wdata_bits_wdata_2;
  wire [7:0] io_dram_0_wdata_bits_wdata_3;
  wire [7:0] io_dram_0_wdata_bits_wdata_4;
  wire [7:0] io_dram_0_wdata_bits_wdata_5;
  wire [7:0] io_dram_0_wdata_bits_wdata_6;
  wire [7:0] io_dram_0_wdata_bits_wdata_7;
  wire [7:0] io_dram_0_wdata_bits_wdata_8;
  wire [7:0] io_dram_0_wdata_bits_wdata_9;
  wire [7:0] io_dram_0_wdata_bits_wdata_10;
  wire [7:0] io_dram_0_wdata_bits_wdata_11;
  wire [7:0] io_dram_0_wdata_bits_wdata_12;
  wire [7:0] io_dram_0_wdata_bits_wdata_13;
  wire [7:0] io_dram_0_wdata_bits_wdata_14;
  wire [7:0] io_dram_0_wdata_bits_wdata_15;
  wire [7:0] io_dram_0_wdata_bits_wdata_16;
  wire [7:0] io_dram_0_wdata_bits_wdata_17;
  wire [7:0] io_dram_0_wdata_bits_wdata_18;
  wire [7:0] io_dram_0_wdata_bits_wdata_19;
  wire [7:0] io_dram_0_wdata_bits_wdata_20;
  wire [7:0] io_dram_0_wdata_bits_wdata_21;
  wire [7:0] io_dram_0_wdata_bits_wdata_22;
  wire [7:0] io_dram_0_wdata_bits_wdata_23;
  wire [7:0] io_dram_0_wdata_bits_wdata_24;
  wire [7:0] io_dram_0_wdata_bits_wdata_25;
  wire [7:0] io_dram_0_wdata_bits_wdata_26;
  wire [7:0] io_dram_0_wdata_bits_wdata_27;
  wire [7:0] io_dram_0_wdata_bits_wdata_28;
  wire [7:0] io_dram_0_wdata_bits_wdata_29;
  wire [7:0] io_dram_0_wdata_bits_wdata_30;
  wire [7:0] io_dram_0_wdata_bits_wdata_31;
  wire [7:0] io_dram_0_wdata_bits_wdata_32;
  wire [7:0] io_dram_0_wdata_bits_wdata_33;
  wire [7:0] io_dram_0_wdata_bits_wdata_34;
  wire [7:0] io_dram_0_wdata_bits_wdata_35;
  wire [7:0] io_dram_0_wdata_bits_wdata_36;
  wire [7:0] io_dram_0_wdata_bits_wdata_37;
  wire [7:0] io_dram_0_wdata_bits_wdata_38;
  wire [7:0] io_dram_0_wdata_bits_wdata_39;
  wire [7:0] io_dram_0_wdata_bits_wdata_40;
  wire [7:0] io_dram_0_wdata_bits_wdata_41;
  wire [7:0] io_dram_0_wdata_bits_wdata_42;
  wire [7:0] io_dram_0_wdata_bits_wdata_43;
  wire [7:0] io_dram_0_wdata_bits_wdata_44;
  wire [7:0] io_dram_0_wdata_bits_wdata_45;
  wire [7:0] io_dram_0_wdata_bits_wdata_46;
  wire [7:0] io_dram_0_wdata_bits_wdata_47;
  wire [7:0] io_dram_0_wdata_bits_wdata_48;
  wire [7:0] io_dram_0_wdata_bits_wdata_49;
  wire [7:0] io_dram_0_wdata_bits_wdata_50;
  wire [7:0] io_dram_0_wdata_bits_wdata_51;
  wire [7:0] io_dram_0_wdata_bits_wdata_52;
  wire [7:0] io_dram_0_wdata_bits_wdata_53;
  wire [7:0] io_dram_0_wdata_bits_wdata_54;
  wire [7:0] io_dram_0_wdata_bits_wdata_55;
  wire [7:0] io_dram_0_wdata_bits_wdata_56;
  wire [7:0] io_dram_0_wdata_bits_wdata_57;
  wire [7:0] io_dram_0_wdata_bits_wdata_58;
  wire [7:0] io_dram_0_wdata_bits_wdata_59;
  wire [7:0] io_dram_0_wdata_bits_wdata_60;
  wire [7:0] io_dram_0_wdata_bits_wdata_61;
  wire [7:0] io_dram_0_wdata_bits_wdata_62;
  wire [7:0] io_dram_0_wdata_bits_wdata_63;
  wire io_dram_0_wdata_bits_wstrb_0;
  wire io_dram_0_wdata_bits_wstrb_1;
  wire io_dram_0_wdata_bits_wstrb_2;
  wire io_dram_0_wdata_bits_wstrb_3;
  wire io_dram_0_wdata_bits_wstrb_4;
  wire io_dram_0_wdata_bits_wstrb_5;
  wire io_dram_0_wdata_bits_wstrb_6;
  wire io_dram_0_wdata_bits_wstrb_7;
  wire io_dram_0_wdata_bits_wstrb_8;
  wire io_dram_0_wdata_bits_wstrb_9;
  wire io_dram_0_wdata_bits_wstrb_10;
  wire io_dram_0_wdata_bits_wstrb_11;
  wire io_dram_0_wdata_bits_wstrb_12;
  wire io_dram_0_wdata_bits_wstrb_13;
  wire io_dram_0_wdata_bits_wstrb_14;
  wire io_dram_0_wdata_bits_wstrb_15;
  wire io_dram_0_wdata_bits_wstrb_16;
  wire io_dram_0_wdata_bits_wstrb_17;
  wire io_dram_0_wdata_bits_wstrb_18;
  wire io_dram_0_wdata_bits_wstrb_19;
  wire io_dram_0_wdata_bits_wstrb_20;
  wire io_dram_0_wdata_bits_wstrb_21;
  wire io_dram_0_wdata_bits_wstrb_22;
  wire io_dram_0_wdata_bits_wstrb_23;
  wire io_dram_0_wdata_bits_wstrb_24;
  wire io_dram_0_wdata_bits_wstrb_25;
  wire io_dram_0_wdata_bits_wstrb_26;
  wire io_dram_0_wdata_bits_wstrb_27;
  wire io_dram_0_wdata_bits_wstrb_28;
  wire io_dram_0_wdata_bits_wstrb_29;
  wire io_dram_0_wdata_bits_wstrb_30;
  wire io_dram_0_wdata_bits_wstrb_31;
  wire io_dram_0_wdata_bits_wstrb_32;
  wire io_dram_0_wdata_bits_wstrb_33;
  wire io_dram_0_wdata_bits_wstrb_34;
  wire io_dram_0_wdata_bits_wstrb_35;
  wire io_dram_0_wdata_bits_wstrb_36;
  wire io_dram_0_wdata_bits_wstrb_37;
  wire io_dram_0_wdata_bits_wstrb_38;
  wire io_dram_0_wdata_bits_wstrb_39;
  wire io_dram_0_wdata_bits_wstrb_40;
  wire io_dram_0_wdata_bits_wstrb_41;
  wire io_dram_0_wdata_bits_wstrb_42;
  wire io_dram_0_wdata_bits_wstrb_43;
  wire io_dram_0_wdata_bits_wstrb_44;
  wire io_dram_0_wdata_bits_wstrb_45;
  wire io_dram_0_wdata_bits_wstrb_46;
  wire io_dram_0_wdata_bits_wstrb_47;
  wire io_dram_0_wdata_bits_wstrb_48;
  wire io_dram_0_wdata_bits_wstrb_49;
  wire io_dram_0_wdata_bits_wstrb_50;
  wire io_dram_0_wdata_bits_wstrb_51;
  wire io_dram_0_wdata_bits_wstrb_52;
  wire io_dram_0_wdata_bits_wstrb_53;
  wire io_dram_0_wdata_bits_wstrb_54;
  wire io_dram_0_wdata_bits_wstrb_55;
  wire io_dram_0_wdata_bits_wstrb_56;
  wire io_dram_0_wdata_bits_wstrb_57;
  wire io_dram_0_wdata_bits_wstrb_58;
  wire io_dram_0_wdata_bits_wstrb_59;
  wire io_dram_0_wdata_bits_wstrb_60;
  wire io_dram_0_wdata_bits_wstrb_61;
  wire io_dram_0_wdata_bits_wstrb_62;
  wire io_dram_0_wdata_bits_wstrb_63;


  wire        io_dram_0_rresp_ready;
  reg         io_dram_0_rresp_valid;
  reg [7:0] io_dram_0_rresp_bits_rdata_0;
  reg [7:0] io_dram_0_rresp_bits_rdata_1;
  reg [7:0] io_dram_0_rresp_bits_rdata_2;
  reg [7:0] io_dram_0_rresp_bits_rdata_3;
  reg [7:0] io_dram_0_rresp_bits_rdata_4;
  reg [7:0] io_dram_0_rresp_bits_rdata_5;
  reg [7:0] io_dram_0_rresp_bits_rdata_6;
  reg [7:0] io_dram_0_rresp_bits_rdata_7;
  reg [7:0] io_dram_0_rresp_bits_rdata_8;
  reg [7:0] io_dram_0_rresp_bits_rdata_9;
  reg [7:0] io_dram_0_rresp_bits_rdata_10;
  reg [7:0] io_dram_0_rresp_bits_rdata_11;
  reg [7:0] io_dram_0_rresp_bits_rdata_12;
  reg [7:0] io_dram_0_rresp_bits_rdata_13;
  reg [7:0] io_dram_0_rresp_bits_rdata_14;
  reg [7:0] io_dram_0_rresp_bits_rdata_15;
  reg [7:0] io_dram_0_rresp_bits_rdata_16;
  reg [7:0] io_dram_0_rresp_bits_rdata_17;
  reg [7:0] io_dram_0_rresp_bits_rdata_18;
  reg [7:0] io_dram_0_rresp_bits_rdata_19;
  reg [7:0] io_dram_0_rresp_bits_rdata_20;
  reg [7:0] io_dram_0_rresp_bits_rdata_21;
  reg [7:0] io_dram_0_rresp_bits_rdata_22;
  reg [7:0] io_dram_0_rresp_bits_rdata_23;
  reg [7:0] io_dram_0_rresp_bits_rdata_24;
  reg [7:0] io_dram_0_rresp_bits_rdata_25;
  reg [7:0] io_dram_0_rresp_bits_rdata_26;
  reg [7:0] io_dram_0_rresp_bits_rdata_27;
  reg [7:0] io_dram_0_rresp_bits_rdata_28;
  reg [7:0] io_dram_0_rresp_bits_rdata_29;
  reg [7:0] io_dram_0_rresp_bits_rdata_30;
  reg [7:0] io_dram_0_rresp_bits_rdata_31;
  reg [7:0] io_dram_0_rresp_bits_rdata_32;
  reg [7:0] io_dram_0_rresp_bits_rdata_33;
  reg [7:0] io_dram_0_rresp_bits_rdata_34;
  reg [7:0] io_dram_0_rresp_bits_rdata_35;
  reg [7:0] io_dram_0_rresp_bits_rdata_36;
  reg [7:0] io_dram_0_rresp_bits_rdata_37;
  reg [7:0] io_dram_0_rresp_bits_rdata_38;
  reg [7:0] io_dram_0_rresp_bits_rdata_39;
  reg [7:0] io_dram_0_rresp_bits_rdata_40;
  reg [7:0] io_dram_0_rresp_bits_rdata_41;
  reg [7:0] io_dram_0_rresp_bits_rdata_42;
  reg [7:0] io_dram_0_rresp_bits_rdata_43;
  reg [7:0] io_dram_0_rresp_bits_rdata_44;
  reg [7:0] io_dram_0_rresp_bits_rdata_45;
  reg [7:0] io_dram_0_rresp_bits_rdata_46;
  reg [7:0] io_dram_0_rresp_bits_rdata_47;
  reg [7:0] io_dram_0_rresp_bits_rdata_48;
  reg [7:0] io_dram_0_rresp_bits_rdata_49;
  reg [7:0] io_dram_0_rresp_bits_rdata_50;
  reg [7:0] io_dram_0_rresp_bits_rdata_51;
  reg [7:0] io_dram_0_rresp_bits_rdata_52;
  reg [7:0] io_dram_0_rresp_bits_rdata_53;
  reg [7:0] io_dram_0_rresp_bits_rdata_54;
  reg [7:0] io_dram_0_rresp_bits_rdata_55;
  reg [7:0] io_dram_0_rresp_bits_rdata_56;
  reg [7:0] io_dram_0_rresp_bits_rdata_57;
  reg [7:0] io_dram_0_rresp_bits_rdata_58;
  reg [7:0] io_dram_0_rresp_bits_rdata_59;
  reg [7:0] io_dram_0_rresp_bits_rdata_60;
  reg [7:0] io_dram_0_rresp_bits_rdata_61;
  reg [7:0] io_dram_0_rresp_bits_rdata_62;
  reg [7:0] io_dram_0_rresp_bits_rdata_63;
  reg [31:0] io_dram_0_rresp_bits_tag;
  wire        io_dram_0_wresp_ready;
  reg         io_dram_0_wresp_valid;
  reg  [31:0] io_dram_0_wresp_bits_tag;

//  wire        io_genericStreamIn_ready;
//  reg         io_genericStreamIn_valid;
//  reg  [31:0] io_genericStreamIn_bits_data;
//  reg  [31:0] io_genericStreamIn_bits_tag = 0;
//
//  reg         io_genericStreamIn_bits_last;
//  reg         io_genericStreamOut_ready;
//  wire        io_genericStreamOut_valid;
//  wire [31:0] io_genericStreamOut_bits_data;
//  wire [31:0] io_genericStreamOut_bits_tag;
//  wire        io_genericStreamOut_bits_last;

  /*** DUT instantiation ***/
  Top Top(
    .clock(clock),
    .reset(reset),
    .io_raddr(io_raddr),
    .io_wen(io_wen),
    .io_waddr(io_waddr),
    .io_wdata(io_wdata),
    .io_rdata(io_rdata),
    .io_dram_0_cmd_ready(io_dram_0_cmd_ready),
    .io_dram_0_cmd_valid(io_dram_0_cmd_valid),
    .io_dram_0_cmd_bits_addr(io_dram_0_cmd_bits_addr),
    .io_dram_0_cmd_bits_rawAddr(io_dram_0_cmd_bits_rawAddr),
    .io_dram_0_cmd_bits_size(io_dram_0_cmd_bits_size),
    .io_dram_0_cmd_bits_isWr(io_dram_0_cmd_bits_isWr),
    .io_dram_0_cmd_bits_tag(io_dram_0_cmd_bits_tag),
    .io_dram_0_wdata_bits_wlast(io_dram_0_wdata_bits_wlast),
    .io_dram_0_wdata_bits_wdata_0(io_dram_0_wdata_bits_wdata_0),
    .io_dram_0_wdata_bits_wdata_1(io_dram_0_wdata_bits_wdata_1),
    .io_dram_0_wdata_bits_wdata_2(io_dram_0_wdata_bits_wdata_2),
    .io_dram_0_wdata_bits_wdata_3(io_dram_0_wdata_bits_wdata_3),
    .io_dram_0_wdata_bits_wdata_4(io_dram_0_wdata_bits_wdata_4),
    .io_dram_0_wdata_bits_wdata_5(io_dram_0_wdata_bits_wdata_5),
    .io_dram_0_wdata_bits_wdata_6(io_dram_0_wdata_bits_wdata_6),
    .io_dram_0_wdata_bits_wdata_7(io_dram_0_wdata_bits_wdata_7),
    .io_dram_0_wdata_bits_wdata_8(io_dram_0_wdata_bits_wdata_8),
    .io_dram_0_wdata_bits_wdata_9(io_dram_0_wdata_bits_wdata_9),
    .io_dram_0_wdata_bits_wdata_10(io_dram_0_wdata_bits_wdata_10),
    .io_dram_0_wdata_bits_wdata_11(io_dram_0_wdata_bits_wdata_11),
    .io_dram_0_wdata_bits_wdata_12(io_dram_0_wdata_bits_wdata_12),
    .io_dram_0_wdata_bits_wdata_13(io_dram_0_wdata_bits_wdata_13),
    .io_dram_0_wdata_bits_wdata_14(io_dram_0_wdata_bits_wdata_14),
    .io_dram_0_wdata_bits_wdata_15(io_dram_0_wdata_bits_wdata_15),
    .io_dram_0_wdata_bits_wdata_16(io_dram_0_wdata_bits_wdata_16),
    .io_dram_0_wdata_bits_wdata_17(io_dram_0_wdata_bits_wdata_17),
    .io_dram_0_wdata_bits_wdata_18(io_dram_0_wdata_bits_wdata_18),
    .io_dram_0_wdata_bits_wdata_19(io_dram_0_wdata_bits_wdata_19),
    .io_dram_0_wdata_bits_wdata_20(io_dram_0_wdata_bits_wdata_20),
    .io_dram_0_wdata_bits_wdata_21(io_dram_0_wdata_bits_wdata_21),
    .io_dram_0_wdata_bits_wdata_22(io_dram_0_wdata_bits_wdata_22),
    .io_dram_0_wdata_bits_wdata_23(io_dram_0_wdata_bits_wdata_23),
    .io_dram_0_wdata_bits_wdata_24(io_dram_0_wdata_bits_wdata_24),
    .io_dram_0_wdata_bits_wdata_25(io_dram_0_wdata_bits_wdata_25),
    .io_dram_0_wdata_bits_wdata_26(io_dram_0_wdata_bits_wdata_26),
    .io_dram_0_wdata_bits_wdata_27(io_dram_0_wdata_bits_wdata_27),
    .io_dram_0_wdata_bits_wdata_28(io_dram_0_wdata_bits_wdata_28),
    .io_dram_0_wdata_bits_wdata_29(io_dram_0_wdata_bits_wdata_29),
    .io_dram_0_wdata_bits_wdata_30(io_dram_0_wdata_bits_wdata_30),
    .io_dram_0_wdata_bits_wdata_31(io_dram_0_wdata_bits_wdata_31),
    .io_dram_0_wdata_bits_wdata_32(io_dram_0_wdata_bits_wdata_32),
    .io_dram_0_wdata_bits_wdata_33(io_dram_0_wdata_bits_wdata_33),
    .io_dram_0_wdata_bits_wdata_34(io_dram_0_wdata_bits_wdata_34),
    .io_dram_0_wdata_bits_wdata_35(io_dram_0_wdata_bits_wdata_35),
    .io_dram_0_wdata_bits_wdata_36(io_dram_0_wdata_bits_wdata_36),
    .io_dram_0_wdata_bits_wdata_37(io_dram_0_wdata_bits_wdata_37),
    .io_dram_0_wdata_bits_wdata_38(io_dram_0_wdata_bits_wdata_38),
    .io_dram_0_wdata_bits_wdata_39(io_dram_0_wdata_bits_wdata_39),
    .io_dram_0_wdata_bits_wdata_40(io_dram_0_wdata_bits_wdata_40),
    .io_dram_0_wdata_bits_wdata_41(io_dram_0_wdata_bits_wdata_41),
    .io_dram_0_wdata_bits_wdata_42(io_dram_0_wdata_bits_wdata_42),
    .io_dram_0_wdata_bits_wdata_43(io_dram_0_wdata_bits_wdata_43),
    .io_dram_0_wdata_bits_wdata_44(io_dram_0_wdata_bits_wdata_44),
    .io_dram_0_wdata_bits_wdata_45(io_dram_0_wdata_bits_wdata_45),
    .io_dram_0_wdata_bits_wdata_46(io_dram_0_wdata_bits_wdata_46),
    .io_dram_0_wdata_bits_wdata_47(io_dram_0_wdata_bits_wdata_47),
    .io_dram_0_wdata_bits_wdata_48(io_dram_0_wdata_bits_wdata_48),
    .io_dram_0_wdata_bits_wdata_49(io_dram_0_wdata_bits_wdata_49),
    .io_dram_0_wdata_bits_wdata_50(io_dram_0_wdata_bits_wdata_50),
    .io_dram_0_wdata_bits_wdata_51(io_dram_0_wdata_bits_wdata_51),
    .io_dram_0_wdata_bits_wdata_52(io_dram_0_wdata_bits_wdata_52),
    .io_dram_0_wdata_bits_wdata_53(io_dram_0_wdata_bits_wdata_53),
    .io_dram_0_wdata_bits_wdata_54(io_dram_0_wdata_bits_wdata_54),
    .io_dram_0_wdata_bits_wdata_55(io_dram_0_wdata_bits_wdata_55),
    .io_dram_0_wdata_bits_wdata_56(io_dram_0_wdata_bits_wdata_56),
    .io_dram_0_wdata_bits_wdata_57(io_dram_0_wdata_bits_wdata_57),
    .io_dram_0_wdata_bits_wdata_58(io_dram_0_wdata_bits_wdata_58),
    .io_dram_0_wdata_bits_wdata_59(io_dram_0_wdata_bits_wdata_59),
    .io_dram_0_wdata_bits_wdata_60(io_dram_0_wdata_bits_wdata_60),
    .io_dram_0_wdata_bits_wdata_61(io_dram_0_wdata_bits_wdata_61),
    .io_dram_0_wdata_bits_wdata_62(io_dram_0_wdata_bits_wdata_62),
    .io_dram_0_wdata_bits_wdata_63(io_dram_0_wdata_bits_wdata_63),
    .io_dram_0_wdata_bits_wstrb_0(io_dram_0_wdata_bits_wstrb_0),
    .io_dram_0_wdata_bits_wstrb_1(io_dram_0_wdata_bits_wstrb_1),
    .io_dram_0_wdata_bits_wstrb_2(io_dram_0_wdata_bits_wstrb_2),
    .io_dram_0_wdata_bits_wstrb_3(io_dram_0_wdata_bits_wstrb_3),
    .io_dram_0_wdata_bits_wstrb_4(io_dram_0_wdata_bits_wstrb_4),
    .io_dram_0_wdata_bits_wstrb_5(io_dram_0_wdata_bits_wstrb_5),
    .io_dram_0_wdata_bits_wstrb_6(io_dram_0_wdata_bits_wstrb_6),
    .io_dram_0_wdata_bits_wstrb_7(io_dram_0_wdata_bits_wstrb_7),
    .io_dram_0_wdata_bits_wstrb_8(io_dram_0_wdata_bits_wstrb_8),
    .io_dram_0_wdata_bits_wstrb_9(io_dram_0_wdata_bits_wstrb_9),
    .io_dram_0_wdata_bits_wstrb_10(io_dram_0_wdata_bits_wstrb_10),
    .io_dram_0_wdata_bits_wstrb_11(io_dram_0_wdata_bits_wstrb_11),
    .io_dram_0_wdata_bits_wstrb_12(io_dram_0_wdata_bits_wstrb_12),
    .io_dram_0_wdata_bits_wstrb_13(io_dram_0_wdata_bits_wstrb_13),
    .io_dram_0_wdata_bits_wstrb_14(io_dram_0_wdata_bits_wstrb_14),
    .io_dram_0_wdata_bits_wstrb_15(io_dram_0_wdata_bits_wstrb_15),
    .io_dram_0_wdata_bits_wstrb_16(io_dram_0_wdata_bits_wstrb_16),
    .io_dram_0_wdata_bits_wstrb_17(io_dram_0_wdata_bits_wstrb_17),
    .io_dram_0_wdata_bits_wstrb_18(io_dram_0_wdata_bits_wstrb_18),
    .io_dram_0_wdata_bits_wstrb_19(io_dram_0_wdata_bits_wstrb_19),
    .io_dram_0_wdata_bits_wstrb_20(io_dram_0_wdata_bits_wstrb_20),
    .io_dram_0_wdata_bits_wstrb_21(io_dram_0_wdata_bits_wstrb_21),
    .io_dram_0_wdata_bits_wstrb_22(io_dram_0_wdata_bits_wstrb_22),
    .io_dram_0_wdata_bits_wstrb_23(io_dram_0_wdata_bits_wstrb_23),
    .io_dram_0_wdata_bits_wstrb_24(io_dram_0_wdata_bits_wstrb_24),
    .io_dram_0_wdata_bits_wstrb_25(io_dram_0_wdata_bits_wstrb_25),
    .io_dram_0_wdata_bits_wstrb_26(io_dram_0_wdata_bits_wstrb_26),
    .io_dram_0_wdata_bits_wstrb_27(io_dram_0_wdata_bits_wstrb_27),
    .io_dram_0_wdata_bits_wstrb_28(io_dram_0_wdata_bits_wstrb_28),
    .io_dram_0_wdata_bits_wstrb_29(io_dram_0_wdata_bits_wstrb_29),
    .io_dram_0_wdata_bits_wstrb_30(io_dram_0_wdata_bits_wstrb_30),
    .io_dram_0_wdata_bits_wstrb_31(io_dram_0_wdata_bits_wstrb_31),
    .io_dram_0_wdata_bits_wstrb_32(io_dram_0_wdata_bits_wstrb_32),
    .io_dram_0_wdata_bits_wstrb_33(io_dram_0_wdata_bits_wstrb_33),
    .io_dram_0_wdata_bits_wstrb_34(io_dram_0_wdata_bits_wstrb_34),
    .io_dram_0_wdata_bits_wstrb_35(io_dram_0_wdata_bits_wstrb_35),
    .io_dram_0_wdata_bits_wstrb_36(io_dram_0_wdata_bits_wstrb_36),
    .io_dram_0_wdata_bits_wstrb_37(io_dram_0_wdata_bits_wstrb_37),
    .io_dram_0_wdata_bits_wstrb_38(io_dram_0_wdata_bits_wstrb_38),
    .io_dram_0_wdata_bits_wstrb_39(io_dram_0_wdata_bits_wstrb_39),
    .io_dram_0_wdata_bits_wstrb_40(io_dram_0_wdata_bits_wstrb_40),
    .io_dram_0_wdata_bits_wstrb_41(io_dram_0_wdata_bits_wstrb_41),
    .io_dram_0_wdata_bits_wstrb_42(io_dram_0_wdata_bits_wstrb_42),
    .io_dram_0_wdata_bits_wstrb_43(io_dram_0_wdata_bits_wstrb_43),
    .io_dram_0_wdata_bits_wstrb_44(io_dram_0_wdata_bits_wstrb_44),
    .io_dram_0_wdata_bits_wstrb_45(io_dram_0_wdata_bits_wstrb_45),
    .io_dram_0_wdata_bits_wstrb_46(io_dram_0_wdata_bits_wstrb_46),
    .io_dram_0_wdata_bits_wstrb_47(io_dram_0_wdata_bits_wstrb_47),
    .io_dram_0_wdata_bits_wstrb_48(io_dram_0_wdata_bits_wstrb_48),
    .io_dram_0_wdata_bits_wstrb_49(io_dram_0_wdata_bits_wstrb_49),
    .io_dram_0_wdata_bits_wstrb_50(io_dram_0_wdata_bits_wstrb_50),
    .io_dram_0_wdata_bits_wstrb_51(io_dram_0_wdata_bits_wstrb_51),
    .io_dram_0_wdata_bits_wstrb_52(io_dram_0_wdata_bits_wstrb_52),
    .io_dram_0_wdata_bits_wstrb_53(io_dram_0_wdata_bits_wstrb_53),
    .io_dram_0_wdata_bits_wstrb_54(io_dram_0_wdata_bits_wstrb_54),
    .io_dram_0_wdata_bits_wstrb_55(io_dram_0_wdata_bits_wstrb_55),
    .io_dram_0_wdata_bits_wstrb_56(io_dram_0_wdata_bits_wstrb_56),
    .io_dram_0_wdata_bits_wstrb_57(io_dram_0_wdata_bits_wstrb_57),
    .io_dram_0_wdata_bits_wstrb_58(io_dram_0_wdata_bits_wstrb_58),
    .io_dram_0_wdata_bits_wstrb_59(io_dram_0_wdata_bits_wstrb_59),
    .io_dram_0_wdata_bits_wstrb_60(io_dram_0_wdata_bits_wstrb_60),
    .io_dram_0_wdata_bits_wstrb_61(io_dram_0_wdata_bits_wstrb_61),
    .io_dram_0_wdata_bits_wstrb_62(io_dram_0_wdata_bits_wstrb_62),
    .io_dram_0_wdata_bits_wstrb_63(io_dram_0_wdata_bits_wstrb_63),
    .io_dram_0_wdata_ready(io_dram_0_wdata_ready),
    .io_dram_0_wdata_valid(io_dram_0_wdata_valid),

    .io_dram_0_rresp_ready(io_dram_0_rresp_ready),
    .io_dram_0_rresp_valid(io_dram_0_rresp_valid),
    .io_dram_0_rresp_bits_rdata_0(io_dram_0_rresp_bits_rdata_0),
    .io_dram_0_rresp_bits_rdata_1(io_dram_0_rresp_bits_rdata_1),
    .io_dram_0_rresp_bits_rdata_2(io_dram_0_rresp_bits_rdata_2),
    .io_dram_0_rresp_bits_rdata_3(io_dram_0_rresp_bits_rdata_3),
    .io_dram_0_rresp_bits_rdata_4(io_dram_0_rresp_bits_rdata_4),
    .io_dram_0_rresp_bits_rdata_5(io_dram_0_rresp_bits_rdata_5),
    .io_dram_0_rresp_bits_rdata_6(io_dram_0_rresp_bits_rdata_6),
    .io_dram_0_rresp_bits_rdata_7(io_dram_0_rresp_bits_rdata_7),
    .io_dram_0_rresp_bits_rdata_8(io_dram_0_rresp_bits_rdata_8),
    .io_dram_0_rresp_bits_rdata_9(io_dram_0_rresp_bits_rdata_9),
    .io_dram_0_rresp_bits_rdata_10(io_dram_0_rresp_bits_rdata_10),
    .io_dram_0_rresp_bits_rdata_11(io_dram_0_rresp_bits_rdata_11),
    .io_dram_0_rresp_bits_rdata_12(io_dram_0_rresp_bits_rdata_12),
    .io_dram_0_rresp_bits_rdata_13(io_dram_0_rresp_bits_rdata_13),
    .io_dram_0_rresp_bits_rdata_14(io_dram_0_rresp_bits_rdata_14),
    .io_dram_0_rresp_bits_rdata_15(io_dram_0_rresp_bits_rdata_15),
    .io_dram_0_rresp_bits_rdata_16(io_dram_0_rresp_bits_rdata_16),
    .io_dram_0_rresp_bits_rdata_17(io_dram_0_rresp_bits_rdata_17),
    .io_dram_0_rresp_bits_rdata_18(io_dram_0_rresp_bits_rdata_18),
    .io_dram_0_rresp_bits_rdata_19(io_dram_0_rresp_bits_rdata_19),
    .io_dram_0_rresp_bits_rdata_20(io_dram_0_rresp_bits_rdata_20),
    .io_dram_0_rresp_bits_rdata_21(io_dram_0_rresp_bits_rdata_21),
    .io_dram_0_rresp_bits_rdata_22(io_dram_0_rresp_bits_rdata_22),
    .io_dram_0_rresp_bits_rdata_23(io_dram_0_rresp_bits_rdata_23),
    .io_dram_0_rresp_bits_rdata_24(io_dram_0_rresp_bits_rdata_24),
    .io_dram_0_rresp_bits_rdata_25(io_dram_0_rresp_bits_rdata_25),
    .io_dram_0_rresp_bits_rdata_26(io_dram_0_rresp_bits_rdata_26),
    .io_dram_0_rresp_bits_rdata_27(io_dram_0_rresp_bits_rdata_27),
    .io_dram_0_rresp_bits_rdata_28(io_dram_0_rresp_bits_rdata_28),
    .io_dram_0_rresp_bits_rdata_29(io_dram_0_rresp_bits_rdata_29),
    .io_dram_0_rresp_bits_rdata_30(io_dram_0_rresp_bits_rdata_30),
    .io_dram_0_rresp_bits_rdata_31(io_dram_0_rresp_bits_rdata_31),
    .io_dram_0_rresp_bits_rdata_32(io_dram_0_rresp_bits_rdata_32),
    .io_dram_0_rresp_bits_rdata_33(io_dram_0_rresp_bits_rdata_33),
    .io_dram_0_rresp_bits_rdata_34(io_dram_0_rresp_bits_rdata_34),
    .io_dram_0_rresp_bits_rdata_35(io_dram_0_rresp_bits_rdata_35),
    .io_dram_0_rresp_bits_rdata_36(io_dram_0_rresp_bits_rdata_36),
    .io_dram_0_rresp_bits_rdata_37(io_dram_0_rresp_bits_rdata_37),
    .io_dram_0_rresp_bits_rdata_38(io_dram_0_rresp_bits_rdata_38),
    .io_dram_0_rresp_bits_rdata_39(io_dram_0_rresp_bits_rdata_39),
    .io_dram_0_rresp_bits_rdata_40(io_dram_0_rresp_bits_rdata_40),
    .io_dram_0_rresp_bits_rdata_41(io_dram_0_rresp_bits_rdata_41),
    .io_dram_0_rresp_bits_rdata_42(io_dram_0_rresp_bits_rdata_42),
    .io_dram_0_rresp_bits_rdata_43(io_dram_0_rresp_bits_rdata_43),
    .io_dram_0_rresp_bits_rdata_44(io_dram_0_rresp_bits_rdata_44),
    .io_dram_0_rresp_bits_rdata_45(io_dram_0_rresp_bits_rdata_45),
    .io_dram_0_rresp_bits_rdata_46(io_dram_0_rresp_bits_rdata_46),
    .io_dram_0_rresp_bits_rdata_47(io_dram_0_rresp_bits_rdata_47),
    .io_dram_0_rresp_bits_rdata_48(io_dram_0_rresp_bits_rdata_48),
    .io_dram_0_rresp_bits_rdata_49(io_dram_0_rresp_bits_rdata_49),
    .io_dram_0_rresp_bits_rdata_50(io_dram_0_rresp_bits_rdata_50),
    .io_dram_0_rresp_bits_rdata_51(io_dram_0_rresp_bits_rdata_51),
    .io_dram_0_rresp_bits_rdata_52(io_dram_0_rresp_bits_rdata_52),
    .io_dram_0_rresp_bits_rdata_53(io_dram_0_rresp_bits_rdata_53),
    .io_dram_0_rresp_bits_rdata_54(io_dram_0_rresp_bits_rdata_54),
    .io_dram_0_rresp_bits_rdata_55(io_dram_0_rresp_bits_rdata_55),
    .io_dram_0_rresp_bits_rdata_56(io_dram_0_rresp_bits_rdata_56),
    .io_dram_0_rresp_bits_rdata_57(io_dram_0_rresp_bits_rdata_57),
    .io_dram_0_rresp_bits_rdata_58(io_dram_0_rresp_bits_rdata_58),
    .io_dram_0_rresp_bits_rdata_59(io_dram_0_rresp_bits_rdata_59),
    .io_dram_0_rresp_bits_rdata_60(io_dram_0_rresp_bits_rdata_60),
    .io_dram_0_rresp_bits_rdata_61(io_dram_0_rresp_bits_rdata_61),
    .io_dram_0_rresp_bits_rdata_62(io_dram_0_rresp_bits_rdata_62),
    .io_dram_0_rresp_bits_rdata_63(io_dram_0_rresp_bits_rdata_63),
    .io_dram_0_rresp_bits_tag(io_dram_0_rresp_bits_tag),
    .io_dram_0_wresp_ready(io_dram_0_wresp_ready),
    .io_dram_0_wresp_valid(io_dram_0_wresp_valid),
    .io_dram_0_wresp_bits_tag(io_dram_0_wresp_bits_tag)
//    .io_genericStreamIn_ready(io_genericStreamIn_ready),
//    .io_genericStreamIn_valid(io_genericStreamIn_valid),
//    .io_genericStreamIn_bits_data(io_genericStreamIn_bits_data),
//    .io_genericStreamIn_bits_tag(io_genericStreamIn_bits_tag),
//    .io_genericStreamIn_bits_last(io_genericStreamIn_bits_last),
//    .io_genericStreamOut_ready(io_genericStreamOut_ready),
//    .io_genericStreamOut_valid(io_genericStreamOut_valid),
//    .io_genericStreamOut_bits_data(io_genericStreamOut_bits_data),
//    .io_genericStreamOut_bits_tag(io_genericStreamOut_bits_tag),
//    .io_genericStreamOut_bits_last(io_genericStreamOut_bits_last)
);

  function void startVPD();
    vpdon = 1;
  endfunction

  function void startVCD();
    vcdon = 1;
  endfunction

  function void stopVPD();
    vpdon = 0;
  endfunction

  function void stopVCD();
    vcdon = 1;
  endfunction


  function void readRegRaddr(input int r);
    io_raddr = r;
  endfunction

  function void readRegRdataHi32(output bit [31:0] rdatahi);
    rdatahi = io_rdata[63:32];
  endfunction

  function void readRegRdataLo32(output bit [31:0] rdatalo);
    rdatalo = io_rdata[31:0];
  endfunction

  function void writeReg(input int r, longint wdata);
    io_waddr = r;
    io_wdata = wdata;
    io_wen = 1;
  endfunction

  function void getDRAMReadRespReady(output bit [31:0] respReady);
    respReady = io_dram_0_rresp_ready;
  endfunction

  function void getDRAMWriteRespReady(output bit [31:0] respReady);
    respReady = io_dram_0_wresp_ready;
  endfunction

  function void pokeDRAMReadResponse(
    input int tag,
    input int rdata0,
    input int rdata1,
    input int rdata2,
    input int rdata3,
    input int rdata4,
    input int rdata5,
    input int rdata6,
    input int rdata7,
    input int rdata8,
    input int rdata9,
    input int rdata10,
    input int rdata11,
    input int rdata12,
    input int rdata13,
    input int rdata14,
    input int rdata15,
    input int rdata16,
    input int rdata17,
    input int rdata18,
    input int rdata19,
    input int rdata20,
    input int rdata21,
    input int rdata22,
    input int rdata23,
    input int rdata24,
    input int rdata25,
    input int rdata26,
    input int rdata27,
    input int rdata28,
    input int rdata29,
    input int rdata30,
    input int rdata31,
    input int rdata32,
    input int rdata33,
    input int rdata34,
    input int rdata35,
    input int rdata36,
    input int rdata37,
    input int rdata38,
    input int rdata39,
    input int rdata40,
    input int rdata41,
    input int rdata42,
    input int rdata43,
    input int rdata44,
    input int rdata45,
    input int rdata46,
    input int rdata47,
    input int rdata48,
    input int rdata49,
    input int rdata50,
    input int rdata51,
    input int rdata52,
    input int rdata53,
    input int rdata54,
    input int rdata55,
    input int rdata56,
    input int rdata57,
    input int rdata58,
    input int rdata59,
    input int rdata60,
    input int rdata61,
    input int rdata62,
    input int rdata63
  );
    io_dram_0_rresp_valid = 1;
    io_dram_0_rresp_bits_tag = tag;
    io_dram_0_rresp_bits_rdata_0 = rdata0;
    io_dram_0_rresp_bits_rdata_1 = rdata1;
    io_dram_0_rresp_bits_rdata_2 = rdata2;
    io_dram_0_rresp_bits_rdata_3 = rdata3;
    io_dram_0_rresp_bits_rdata_4 = rdata4;
    io_dram_0_rresp_bits_rdata_5 = rdata5;
    io_dram_0_rresp_bits_rdata_6 = rdata6;
    io_dram_0_rresp_bits_rdata_7 = rdata7;
    io_dram_0_rresp_bits_rdata_8 = rdata8;
    io_dram_0_rresp_bits_rdata_9 = rdata9;
    io_dram_0_rresp_bits_rdata_10 = rdata10;
    io_dram_0_rresp_bits_rdata_11 = rdata11;
    io_dram_0_rresp_bits_rdata_12 = rdata12;
    io_dram_0_rresp_bits_rdata_13 = rdata13;
    io_dram_0_rresp_bits_rdata_14 = rdata14;
    io_dram_0_rresp_bits_rdata_15 = rdata15;
    io_dram_0_rresp_bits_rdata_16 = rdata16;
    io_dram_0_rresp_bits_rdata_17 = rdata17;
    io_dram_0_rresp_bits_rdata_18 = rdata18;
    io_dram_0_rresp_bits_rdata_19 = rdata19;
    io_dram_0_rresp_bits_rdata_20 = rdata20;
    io_dram_0_rresp_bits_rdata_21 = rdata21;
    io_dram_0_rresp_bits_rdata_22 = rdata22;
    io_dram_0_rresp_bits_rdata_23 = rdata23;
    io_dram_0_rresp_bits_rdata_24 = rdata24;
    io_dram_0_rresp_bits_rdata_25 = rdata25;
    io_dram_0_rresp_bits_rdata_26 = rdata26;
    io_dram_0_rresp_bits_rdata_27 = rdata27;
    io_dram_0_rresp_bits_rdata_28 = rdata28;
    io_dram_0_rresp_bits_rdata_29 = rdata29;
    io_dram_0_rresp_bits_rdata_30 = rdata30;
    io_dram_0_rresp_bits_rdata_31 = rdata31;
    io_dram_0_rresp_bits_rdata_32 = rdata32;
    io_dram_0_rresp_bits_rdata_33 = rdata33;
    io_dram_0_rresp_bits_rdata_34 = rdata34;
    io_dram_0_rresp_bits_rdata_35 = rdata35;
    io_dram_0_rresp_bits_rdata_36 = rdata36;
    io_dram_0_rresp_bits_rdata_37 = rdata37;
    io_dram_0_rresp_bits_rdata_38 = rdata38;
    io_dram_0_rresp_bits_rdata_39 = rdata39;
    io_dram_0_rresp_bits_rdata_40 = rdata40;
    io_dram_0_rresp_bits_rdata_41 = rdata41;
    io_dram_0_rresp_bits_rdata_42 = rdata42;
    io_dram_0_rresp_bits_rdata_43 = rdata43;
    io_dram_0_rresp_bits_rdata_44 = rdata44;
    io_dram_0_rresp_bits_rdata_45 = rdata45;
    io_dram_0_rresp_bits_rdata_46 = rdata46;
    io_dram_0_rresp_bits_rdata_47 = rdata47;
    io_dram_0_rresp_bits_rdata_48 = rdata48;
    io_dram_0_rresp_bits_rdata_49 = rdata49;
    io_dram_0_rresp_bits_rdata_50 = rdata50;
    io_dram_0_rresp_bits_rdata_51 = rdata51;
    io_dram_0_rresp_bits_rdata_52 = rdata52;
    io_dram_0_rresp_bits_rdata_53 = rdata53;
    io_dram_0_rresp_bits_rdata_54 = rdata54;
    io_dram_0_rresp_bits_rdata_55 = rdata55;
    io_dram_0_rresp_bits_rdata_56 = rdata56;
    io_dram_0_rresp_bits_rdata_57 = rdata57;
    io_dram_0_rresp_bits_rdata_58 = rdata58;
    io_dram_0_rresp_bits_rdata_59 = rdata59;
    io_dram_0_rresp_bits_rdata_60 = rdata60;
    io_dram_0_rresp_bits_rdata_61 = rdata61;
    io_dram_0_rresp_bits_rdata_62 = rdata62;
    io_dram_0_rresp_bits_rdata_63 = rdata63;

  endfunction

  function void pokeDRAMWriteResponse(
    input int tag
  );
    io_dram_0_wresp_valid = 1;
    io_dram_0_wresp_bits_tag = tag;
  endfunction


  function void writeStream(
    input int data,
    input int tag,
    input int last
  );
//    io_genericStreamIn_valid = 1;
//    io_genericStreamIn_bits_data = data;
//    io_genericStreamIn_bits_tag = tag;
//    io_genericStreamIn_bits_last = last;
  endfunction

  // 1. If io_dram_0_cmd_valid, then send send DRAM request to CPP layer
  function void post_update_callbacks();
    if (io_dram_0_cmd_valid & ~reset) begin
      io_dram_0_cmd_ready <= 1;
    end else begin
      io_dram_0_cmd_ready <= 0;
    end

    // Lower the ready signal for a cycle after wlast goes high
    if (io_dram_0_wdata_valid & ~reset) begin
      io_dram_0_wdata_ready <= 1;
    end else begin
      io_dram_0_wdata_ready <= 0;
    end

//    if (io_genericStreamOut_valid & ~reset) begin
//      readOutputStream(
//        io_genericStreamOut_bits_data,
//        io_genericStreamOut_bits_tag,
//       io_genericStreamOut_bits_last
//      );
//    end

  endfunction

  /**
  * Sample and handle DRAM CMD/WDATA signals
  * NOTE: Order of invocation is important: Handle command before wdata
  * This is because the DRAM simulation logic expects that write commands
  * are always seen BEFORE write data. Handling command before data preserves
  * this order. Flipping the order screws up simulation when both command
  * and wdata are issued in the same cycle.
  */
  function void pre_update_callbacks();
    if (io_dram_0_cmd_valid & io_dram_0_cmd_ready) begin
      sendDRAMRequest(
        io_dram_0_cmd_bits_addr,
        io_dram_0_cmd_bits_rawAddr,
        io_dram_0_cmd_bits_size,
        io_dram_0_cmd_bits_tag,
        io_dram_0_cmd_bits_isWr
      );
    end

    if (io_dram_0_wdata_valid & io_dram_0_wdata_ready) begin
      sendWdataStrb(
        io_dram_0_cmd_valid,
        io_dram_0_wdata_bits_wdata_0,
        io_dram_0_wdata_bits_wdata_1,
        io_dram_0_wdata_bits_wdata_2,
        io_dram_0_wdata_bits_wdata_3,
        io_dram_0_wdata_bits_wdata_4,
        io_dram_0_wdata_bits_wdata_5,
        io_dram_0_wdata_bits_wdata_6,
        io_dram_0_wdata_bits_wdata_7,
        io_dram_0_wdata_bits_wdata_8,
        io_dram_0_wdata_bits_wdata_9,
        io_dram_0_wdata_bits_wdata_10,
        io_dram_0_wdata_bits_wdata_11,
        io_dram_0_wdata_bits_wdata_12,
        io_dram_0_wdata_bits_wdata_13,
        io_dram_0_wdata_bits_wdata_14,
        io_dram_0_wdata_bits_wdata_15,
        io_dram_0_wdata_bits_wdata_16,
        io_dram_0_wdata_bits_wdata_17,
        io_dram_0_wdata_bits_wdata_18,
        io_dram_0_wdata_bits_wdata_19,
        io_dram_0_wdata_bits_wdata_20,
        io_dram_0_wdata_bits_wdata_21,
        io_dram_0_wdata_bits_wdata_22,
        io_dram_0_wdata_bits_wdata_23,
        io_dram_0_wdata_bits_wdata_24,
        io_dram_0_wdata_bits_wdata_25,
        io_dram_0_wdata_bits_wdata_26,
        io_dram_0_wdata_bits_wdata_27,
        io_dram_0_wdata_bits_wdata_28,
        io_dram_0_wdata_bits_wdata_29,
        io_dram_0_wdata_bits_wdata_30,
        io_dram_0_wdata_bits_wdata_31,
        io_dram_0_wdata_bits_wdata_32,
        io_dram_0_wdata_bits_wdata_33,
        io_dram_0_wdata_bits_wdata_34,
        io_dram_0_wdata_bits_wdata_35,
        io_dram_0_wdata_bits_wdata_36,
        io_dram_0_wdata_bits_wdata_37,
        io_dram_0_wdata_bits_wdata_38,
        io_dram_0_wdata_bits_wdata_39,
        io_dram_0_wdata_bits_wdata_40,
        io_dram_0_wdata_bits_wdata_41,
        io_dram_0_wdata_bits_wdata_42,
        io_dram_0_wdata_bits_wdata_43,
        io_dram_0_wdata_bits_wdata_44,
        io_dram_0_wdata_bits_wdata_45,
        io_dram_0_wdata_bits_wdata_46,
        io_dram_0_wdata_bits_wdata_47,
        io_dram_0_wdata_bits_wdata_48,
        io_dram_0_wdata_bits_wdata_49,
        io_dram_0_wdata_bits_wdata_50,
        io_dram_0_wdata_bits_wdata_51,
        io_dram_0_wdata_bits_wdata_52,
        io_dram_0_wdata_bits_wdata_53,
        io_dram_0_wdata_bits_wdata_54,
        io_dram_0_wdata_bits_wdata_55,
        io_dram_0_wdata_bits_wdata_56,
        io_dram_0_wdata_bits_wdata_57,
        io_dram_0_wdata_bits_wdata_58,
        io_dram_0_wdata_bits_wdata_59,
        io_dram_0_wdata_bits_wdata_60,
        io_dram_0_wdata_bits_wdata_61,
        io_dram_0_wdata_bits_wdata_62,
        io_dram_0_wdata_bits_wdata_63,
        io_dram_0_wdata_bits_wstrb_0,
        io_dram_0_wdata_bits_wstrb_1,
        io_dram_0_wdata_bits_wstrb_2,
        io_dram_0_wdata_bits_wstrb_3,
        io_dram_0_wdata_bits_wstrb_4,
        io_dram_0_wdata_bits_wstrb_5,
        io_dram_0_wdata_bits_wstrb_6,
        io_dram_0_wdata_bits_wstrb_7,
        io_dram_0_wdata_bits_wstrb_8,
        io_dram_0_wdata_bits_wstrb_9,
        io_dram_0_wdata_bits_wstrb_10,
        io_dram_0_wdata_bits_wstrb_11,
        io_dram_0_wdata_bits_wstrb_12,
        io_dram_0_wdata_bits_wstrb_13,
        io_dram_0_wdata_bits_wstrb_14,
        io_dram_0_wdata_bits_wstrb_15,
        io_dram_0_wdata_bits_wstrb_16,
        io_dram_0_wdata_bits_wstrb_17,
        io_dram_0_wdata_bits_wstrb_18,
        io_dram_0_wdata_bits_wstrb_19,
        io_dram_0_wdata_bits_wstrb_20,
        io_dram_0_wdata_bits_wstrb_21,
        io_dram_0_wdata_bits_wstrb_22,
        io_dram_0_wdata_bits_wstrb_23,
        io_dram_0_wdata_bits_wstrb_24,
        io_dram_0_wdata_bits_wstrb_25,
        io_dram_0_wdata_bits_wstrb_26,
        io_dram_0_wdata_bits_wstrb_27,
        io_dram_0_wdata_bits_wstrb_28,
        io_dram_0_wdata_bits_wstrb_29,
        io_dram_0_wdata_bits_wstrb_30,
        io_dram_0_wdata_bits_wstrb_31,
        io_dram_0_wdata_bits_wstrb_32,
        io_dram_0_wdata_bits_wstrb_33,
        io_dram_0_wdata_bits_wstrb_34,
        io_dram_0_wdata_bits_wstrb_35,
        io_dram_0_wdata_bits_wstrb_36,
        io_dram_0_wdata_bits_wstrb_37,
        io_dram_0_wdata_bits_wstrb_38,
        io_dram_0_wdata_bits_wstrb_39,
        io_dram_0_wdata_bits_wstrb_40,
        io_dram_0_wdata_bits_wstrb_41,
        io_dram_0_wdata_bits_wstrb_42,
        io_dram_0_wdata_bits_wstrb_43,
        io_dram_0_wdata_bits_wstrb_44,
        io_dram_0_wdata_bits_wstrb_45,
        io_dram_0_wdata_bits_wstrb_46,
        io_dram_0_wdata_bits_wstrb_47,
        io_dram_0_wdata_bits_wstrb_48,
        io_dram_0_wdata_bits_wstrb_49,
        io_dram_0_wdata_bits_wstrb_50,
        io_dram_0_wdata_bits_wstrb_51,
        io_dram_0_wdata_bits_wstrb_52,
        io_dram_0_wdata_bits_wstrb_53,
        io_dram_0_wdata_bits_wstrb_54,
        io_dram_0_wdata_bits_wstrb_55,
        io_dram_0_wdata_bits_wstrb_56,
        io_dram_0_wdata_bits_wstrb_57,
        io_dram_0_wdata_bits_wstrb_58,
        io_dram_0_wdata_bits_wstrb_59,
        io_dram_0_wdata_bits_wstrb_60,
        io_dram_0_wdata_bits_wstrb_61,
        io_dram_0_wdata_bits_wstrb_62,
        io_dram_0_wdata_bits_wstrb_63
      );
    end

    serviceWRequest();


    // Update internal response queues
    if (io_dram_0_rresp_valid & io_dram_0_rresp_ready) begin
      popDRAMReadQ();
    end

    if (io_dram_0_wresp_valid & io_dram_0_wresp_ready) begin
      popDRAMWriteQ();
    end

  endfunction

  initial begin

    sim_init();

    /*** VCD & VPD dump ***/
    if (vpdon) begin
      $vcdplusfile("Top.vpd");
      $vcdpluson (0, Top);
      $vcdplusmemon ();
    end

    if (vcdon) begin
      $dumpfile("Top.vcd");
      $dumpvars(0, Top);
    end

    io_dram_0_cmd_ready = 0;
    io_dram_0_wdata_ready = 0;
    io_dram_0_rresp_valid = 0;
    io_dram_0_wresp_valid = 0;
  end


  function void getCycles(output longint cycles);
    cycles = numCycles;
  endfunction

  function void terminateSim();
    if (vpdon) begin
      $vcdplusflush;
    end
    if (vcdon) begin
      $dumpflush;
    end
    $finish;
  endfunction

  always @(posedge clock) begin

    numCycles = numCycles + 1;

    pre_update_callbacks();

    io_wen = 0;
    io_dram_0_rresp_valid = 0;
    io_dram_0_wresp_valid = 0;
//    io_dram_0_cmd_ready = 0;
//    io_genericStreamIn_valid = 0;
//    io_genericStreamOut_ready = 1;

    if (tick()) begin
      if (vpdon) begin
        $vcdplusflush;
      end
      if (vcdon) begin
        $dumpflush;
      end
      $finish;
    end


    post_update_callbacks();

    if (vpdon) begin
      $vcdplusflush;
    end
    if (vcdon) begin
      $dumpflush;
    end
  end

endmodule
