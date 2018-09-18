module Arria10_tb;
  reg clk;                    // in
  reg reset;                  // in
  reg [6:0] addr;             // in
  wire [31:0] readdata;       // out
  reg [31:0] writedata;      // in
  wire io_rdata;
  reg chipselect;             // in
  reg write;                  // in
  reg read;                   // in

  Top top0 (
            .clock (clk),
            .reset (reset),
            .io_raddr (clk),
            .io_wen (write),
            .io_waddr (clk),
            .io_rdata (io_rdata),
            .io_S_AVALON_readdata (readdata),
            .io_S_AVALON_address (addr),
            .io_S_AVALON_chipselect (chipselect),
            .io_S_AVALON_write (write),
            .io_S_AVALON_read (read),
            .io_S_AVALON_writedata (writedata)
            );

  initial
  begin
    $dumpfile("arria10_argInOuts.vcd");
    $dumpvars(0, top0);
    clk = 0;
    chipselect = 1;
    reset = 0;
    write = 0;
    addr = 0;
    writedata = 0;
    chipselect = 0;
    write = 0;
    read = 0;

    #10
    reset = 1;
    #10
    reset = 0;

    #10 // argIn
    addr = 10'h2;
    writedata = 32'h4;
    write = 1;

    #10
    write = 0;

    #20
    addr = 10'h0;
    writedata = 32'h1;
    write = 1;

    #10
    write = 0;

    #490

    // check results and status
    // argouts
    #10
    addr = 10'h3;
    read = 1;

    #30
    addr = 10'h1;

    $finish;
  end

  always
    #5 clk = !clk;

endmodule
