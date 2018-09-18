module SRAMVerilogSim
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
    output reg [DWIDTH-1:0] rdata
);

    reg [DWIDTH-1:0] mem [0:WORDS-1];

    always @(negedge clk) begin
      if (wen) begin
        mem[waddr] <= wdata;
      end
    end

    always @(posedge clk) begin
        rdata <= mem[raddr];
    end

endmodule
