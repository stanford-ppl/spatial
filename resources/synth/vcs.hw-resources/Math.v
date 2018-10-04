module SqrtSimBBox
#(
    parameter DWIDTH = 32)
(
   input  clk,
   output rdy,
   input  reset,
   input [DWIDTH-1:0] x,
   output [DWIDTH-1:0] y
);

   // acc holds the accumulated result, and acc2 is the accumulated
   // square of the accumulated result.
   reg [DWIDTH/2-1:0] acc;
   reg [DWIDTH-1:0] acc2;

   // Keep track of which bit I'm working on.
   reg [4:0]  bitl;
   wire [DWIDTH/2-1:0] bit1 = 1 << bitl;
   wire [DWIDTH-1:0] bit2 = 1 << (bitl << 1);

   assign y = {{DWIDTH{1'b0}}, acc};

   // The output is ready when the bitl counter underflows.
   wire rdy = bitl[4];

   // guess holds the potential next values for acc, and guess2 holds
   // the square of that guess. The guess2 calculation is a little bit
   // subtle. The idea is that:
   //
   //      guess2 = (acc + bit1) * (acc + bit1)
   //             = (acc * acc) + 2*acc*bit1 + bit1*bit1
   //             = acc2 + 2*acc*bit1 + bit2
   //             = acc2 + 2 * (acc<<bitl) + bit1
   //
   // This works out using shifts because bit1 and bit2 are known to
   // have only a single bit in them.
   wire [15:0] guess  = acc | bit1;
   wire [31:0] guess2 = acc2 + bit2 + ((acc << bitl) << 1);

   task clear;
      begin
     acc = 0;
     acc2 = 0;
     bitl = 15;
      end
   endtask

   initial clear;

   always @(reset or posedge clk)
      if (reset)
       clear;
      else begin
     if (guess2 <= x) begin
        acc  <= guess;
        acc2 <= guess2;
     end
     bitl <= bitl - 1;
      end

endmodule

module Log2BBox
#(
    parameter DWIDTH = 32)
(
 input  clk,
 output rdy,
 input  reset,
 input  [DWIDTH-1:0] x,
 output reg [DWIDTH-1:0] y
);
  always @(posedge clk) begin
    y <= $clog2(x);
  end
endmodule

