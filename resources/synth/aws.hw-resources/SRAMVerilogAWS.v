module SRAMVerilogAWS
#(
    parameter WORDS = 1024,
    parameter AWIDTH = 10,
    parameter DWIDTH = 32)
(
    input clk,
    input [AWIDTH-1:0] raddr,
    input [AWIDTH-1:0] waddr,
    input raddrEn,
    input waddrEn,
    input wen,
    input [DWIDTH-1:0] wdata,
    input backpressure,
    output reg [DWIDTH-1:0] rdata
);

    reg [DWIDTH-1:0] mem [0:WORDS-1];

    always @(posedge clk) begin
      if (wen) mem[waddr] <= wdata;
      if (backpressure) rdata <= mem[raddr];
    end

endmodule

module SRAMVerilogAWS_U
#(
    parameter WORDS = 1024,
    parameter AWIDTH = 10,
    parameter DWIDTH = 32)
(
    input clk,
    input [AWIDTH-1:0] raddr,
    input [AWIDTH-1:0] waddr,
    input raddrEn,
    input waddrEn,
    input wen,
    input [DWIDTH-1:0] wdata,
    input backpressure,
    output reg [DWIDTH-1:0] rdata
);

    //                        reg [DWIDTH-1:0] mem [0:WORDS-1];
    (* ram_style = "ultra" *) reg [DWIDTH-1:0] mem [0:WORDS-1];

    always @(posedge clk) begin
      if (wen) mem[waddr] <= wdata;
      if (backpressure) rdata <= mem[raddr];
    end

endmodule

