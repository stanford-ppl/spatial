module SRAMVerilogAWS_URAM #(
    //----------------------------------------------------------------------
    parameter   WORDS       = 1024,
    parameter   AWIDTH      =  10, 
    parameter   DWIDTH      =  32  // Data  Width in bits
    //----------------------------------------------------------------------
) (
    input clk,
    input [AWIDTH-1:0] raddr,
    input [AWIDTH-1:0] waddr,
    input raddrEn,
    input waddrEn,
    input wen,
    input [DWIDTH-1:0] wdata,
    input flow,
    output [DWIDTH-1:0] rdata
);

    //----------------------------------------------------------------------
    // Localparams
    //----------------------------------------------------------------------
    localparam SRAM_WIDTH = 64;

    // Check if data width is byte multiple, if not use single column mode
    localparam NUM_COL        = (DWIDTH%8 == 0) && (AWIDTH > 1) && (DWIDTH < SRAM_WIDTH) ? SRAM_WIDTH/DWIDTH : 1,
               SRAM_LOG_DEPTH = (DWIDTH%8 == 0) ?  (WORDS + NUM_COL-1)/NUM_COL : WORDS,
               SRAM_LOG_WIDTH = (DWIDTH%8 == 0) ? DWIDTH*NUM_COL    : DWIDTH,
               COL_SEL        = (DWIDTH%8 == 0) ? $clog2(NUM_COL)   : 0;


    //----------------------------------------------------------------------
    // Variables
    //----------------------------------------------------------------------
    // iterator variable
    integer i;
`ifdef VCS
    bit memprint; // enable write logging
`endif

    // Core Memory  
    (* ram_style = "ultra" *) reg [SRAM_LOG_WIDTH-1:0] mem [SRAM_LOG_DEPTH-1:0];

    // Read data register
    reg [AWIDTH-1:0]         raddr_reg;
    reg [SRAM_LOG_WIDTH-1:0] rdata_reg;
    
    // Enable vector
    reg [NUM_COL-1:0] weA = {NUM_COL{1'b1}};
    

    //----------------------------------------------------------------------
    // Initialization of memory array (SIM ONLY)
    //----------------------------------------------------------------------
    initial begin
      for (i=0; i<SRAM_LOG_DEPTH; i=i+1) begin
        mem[i] = 0;
      end
`ifdef VCS
      if ($test$plusargs("MEMPRINT") )
        memprint = 1;
`endif
      raddr_reg = 0;
      rdata_reg = 0;
    end

    //----------------------------------------------------------------------
    // Combinatorial Logic
    //----------------------------------------------------------------------
    // Generate byte enable vector
    generate
      if (COL_SEL == 0) begin
        // Single word column, enable is always set
        always @*
          weA = 1'b1;
      end else begin
        // Convert address bits used to select word column to one-hot select vector
        always @* begin
          weA = {NUM_COL{1'b0}};
          weA[waddr[COL_SEL-1:0]] = 1'b1;
        end
      end
    endgenerate

    // Read word mux
    generate
      if (COL_SEL == 0) begin
        assign rdata = rdata_reg; 
      end else begin
        assign rdata = rdata_reg[raddr_reg[COL_SEL-1:0]*DWIDTH +: DWIDTH]; 
      end
    endgenerate

    //----------------------------------------------------------------------
    // Sequential Logic
    //----------------------------------------------------------------------

    // Memory array, writes
    // SRAM row selected based on upper address bits, column selected by iterator
    integer waddr_gen;
    integer waddr_print;
    integer raddr_gen;
    generate
      genvar col;
      for(col=0;col<NUM_COL;col=col+1) begin
        always @ (posedge clk) begin
          if(wen) begin
            if(weA[col]) begin
              if (AWIDTH == 0) begin
                waddr_gen = 0;
                waddr_print = 0;
              end 
              else begin
                waddr_gen = waddr >> COL_SEL;
                waddr_print = waddr;
              end
              mem[waddr_gen][col*DWIDTH +: DWIDTH] <= wdata;
`ifdef VCS
              if (memprint) $display($time," %m: addr 0x%h 0x%h",waddr_print, wdata);
`endif
            end
          end
        end
      end
    endgenerate

    // Memory array, reads
    always @(posedge clk) begin
      if (flow & raddrEn) begin
        raddr_reg <= raddr;
        if (AWIDTH == 0) 
          raddr_gen = 0;
        else 
          raddr_gen = raddr >> COL_SEL;
        rdata_reg <= mem[raddr_gen];
      end
    end
endmodule
