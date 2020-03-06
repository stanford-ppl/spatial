module SRAMVerilogDualRead
#(
    parameter WORDS = 1024,
    parameter AWIDTH = 10,
    parameter DWIDTH = 32)
(
    input clk,
    input [AWIDTH-1:0] raddr0,
    input [AWIDTH-1:0] raddr1,
    input [AWIDTH-1:0] waddr,
    input raddrEn0,
    input raddrEn1,
    input waddrEn,
    input wen,
    input backpressure0,
    input backpressure1,
    input [DWIDTH-1:0] wdata,
    output reg [DWIDTH-1:0] rdata0,
    output reg [DWIDTH-1:0] rdata1
);

    reg [DWIDTH-1:0] mem [0:WORDS-1];

    always @(posedge clk)
    begin
            if (wen)
            begin
                mem[waddr] <= wdata;
            end
            if (backpressure0) rdata0 <= mem[raddr0];
    end


    always @(posedge clk)
    begin
        if (backpressure1) rdata1 <= mem[raddr1];
    end
endmodule




