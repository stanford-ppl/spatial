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



`timescale 1ps / 1ps

module altera_avlmm_mst_freeze_bridge #(
    parameter ENABLE_TRAFFIC_TRACKING        = 0,
    parameter ENABLE_FREEZE_FROM_PR_REGION   = 0,
    parameter USE_BURSTCOUNT                 = 1,
    parameter USE_READ_WAIT_TIME             = 0,
    parameter USE_WRITE_WAIT_TIME            = 0,
    parameter MST_BRIDGE_READ_WAIT_TIME      = 1,
    parameter MST_BRIDGE_WRITE_WAIT_TIME     = 1,
    parameter MST_BRIDGE_BYTEEN_WIDTH        = 4,
    parameter MST_BRIDGE_ADDR_WIDTH          = 32,
    parameter MST_BRIDGE_WRDATA_WIDTH        = 32,
    parameter MST_BRIDGE_RDDATA_WIDTH        = 32,
    parameter MST_BRIDGE_BURSTCOUNT_WIDTH    = 7,
    parameter MST_BRIDGE_RWT_WIDTH           = 1,
    parameter MST_BRIDGE_WWT_WIDTH           = 1
)(                                                
	input                                               clk,
	input                                               reset_n,
	                                                   
	// avalon mm master interface to static region                           			
	output reg                                          mst_bridge_to_sr_write,
	output reg                                          mst_bridge_to_sr_read,
	output reg       [MST_BRIDGE_ADDR_WIDTH-1       :0] mst_bridge_to_sr_addr,
	output reg       [MST_BRIDGE_WRDATA_WIDTH -1    :0] mst_bridge_to_sr_wrdata,
	output reg       [MST_BRIDGE_BYTEEN_WIDTH-1     :0] mst_bridge_to_sr_byteenable,
	output reg       [MST_BRIDGE_BURSTCOUNT_WIDTH-1 :0] mst_bridge_to_sr_burstcount,
	output reg                                          mst_bridge_to_sr_beginbursttransfer,
	output reg                                          mst_bridge_to_sr_debugaccess,
	input  wire      [MST_BRIDGE_RDDATA_WIDTH-1     :0] mst_bridge_to_sr_rddata,
	input  wire                                         mst_bridge_to_sr_rddata_valid,
	input  wire                                         mst_bridge_to_sr_waitrequest,
	input  wire      [1                        :0]      mst_bridge_to_sr_response,
	output reg                                          mst_bridge_to_sr_lock,
	input  wire                                         mst_bridge_to_sr_writeresponsevalid,
	                                                   
	// avalon mm slave interface to PR region         
	input 	wire                                        mst_bridge_to_pr_write,
	input 	wire                                        mst_bridge_to_pr_read,
	input 	wire     [MST_BRIDGE_ADDR_WIDTH-1       :0] mst_bridge_to_pr_addr,
	input 	wire     [MST_BRIDGE_WRDATA_WIDTH-1     :0] mst_bridge_to_pr_wrdata,
	input   wire     [MST_BRIDGE_BYTEEN_WIDTH-1     :0] mst_bridge_to_pr_byteenable,
	input   wire     [MST_BRIDGE_BURSTCOUNT_WIDTH-1 :0] mst_bridge_to_pr_burstcount,
	input   wire                                        mst_bridge_to_pr_beginbursttransfer,
	input   wire                                        mst_bridge_to_pr_debugaccess,
	output 	reg      [MST_BRIDGE_RDDATA_WIDTH-1     :0] mst_bridge_to_pr_rddata,
	output 	reg                                         mst_bridge_to_pr_rddata_valid,
	output 	reg                                         mst_bridge_to_pr_waitrequest,
	output  reg      [1                        :0]      mst_bridge_to_pr_response,
	input   wire                                        mst_bridge_to_pr_lock,
	output  reg                                         mst_bridge_to_pr_writeresponsevalid,
	                                                   
	// conduit freeze                                  
	input   wire                                        freeze,
	input   wire                                        pr_freeze,
	output  reg                                         illegal_request
);  

reg freeze_s1, freeze_s2;
wire interface_freeze;

generate if (ENABLE_FREEZE_FROM_PR_REGION) begin : gen10
    assign interface_freeze = freeze || pr_freeze;
end
else begin
    assign interface_freeze = freeze;
end
endgenerate

// ------------------------- reset syncronizer for freeze input -----------------------------
always_ff @(posedge clk) begin
    if (!reset_n) begin
        freeze_s1 <= 1'b0;
        freeze_s2 <= 1'b0;
    end
    else begin
        freeze_s1 <= interface_freeze;
        freeze_s2 <= freeze_s1;
    end
end

//------------------------------------------------------------------------------------------------

generate if (ENABLE_TRAFFIC_TRACKING == 0) begin : gen11
    // -------------------- signals other than control signals are just pass thru -----
    assign mst_bridge_to_pr_rddata             = mst_bridge_to_sr_rddata;
    assign mst_bridge_to_pr_response           = mst_bridge_to_sr_response;
	assign mst_bridge_to_sr_wrdata             = mst_bridge_to_pr_wrdata;
	assign mst_bridge_to_sr_addr               = mst_bridge_to_pr_addr;
    assign mst_bridge_to_sr_byteenable         = mst_bridge_to_pr_byteenable;
    assign mst_bridge_to_sr_burstcount         = mst_bridge_to_pr_burstcount;
    // --------------------------------------------------------------------------------- 
    assign mst_bridge_to_sr_debugaccess        = (~freeze_s2) ? mst_bridge_to_pr_debugaccess : 1'b0;
    assign mst_bridge_to_sr_lock               = (~freeze_s2) ? mst_bridge_to_pr_lock : 1'b0;
    assign mst_bridge_to_sr_beginbursttransfer = (~freeze_s2) ? mst_bridge_to_pr_beginbursttransfer : 1'b0;
	assign mst_bridge_to_sr_write              = (~freeze_s2) ? mst_bridge_to_pr_write : 1'b0;				 
    assign mst_bridge_to_sr_read               = (~freeze_s2) ? mst_bridge_to_pr_read :  1'b0;
    assign mst_bridge_to_pr_waitrequest        = (~freeze_s2) ? mst_bridge_to_sr_waitrequest : 1'b0;
    assign mst_bridge_to_pr_rddata_valid       = (~freeze_s2) ? mst_bridge_to_sr_rddata_valid : 1'b0;
    assign mst_bridge_to_pr_writeresponsevalid = (~freeze_s2) ? mst_bridge_to_sr_writeresponsevalid : 1'b0;													 
end
else begin
    `ifdef ALTERA_FREEZE_ASSERTION_ON
        `include "altera_avlmm_mst_freeze_bridge_checker.svh"
    `endif
	
    localparam [1:0] IDLE                = 2'b00;
    localparam [1:0] SEND_WRERROR	       = 2'b01;
    localparam READ_WAIT_TIME            = USE_READ_WAIT_TIME ? MST_BRIDGE_READ_WAIT_TIME : 0;
    localparam WRITE_WAIT_TIME           = USE_WRITE_WAIT_TIME ? MST_BRIDGE_WRITE_WAIT_TIME : 0;
    localparam USE_WAIT_TIME             = (USE_WRITE_WAIT_TIME || USE_READ_WAIT_TIME) ? 1 : 0;

    reg [1:0] state, next_state;
    reg [MST_BRIDGE_BURSTCOUNT_WIDTH-1:0] wrcounter; 
    reg illegal_request_combi, back_pressure_wr, back_pressure_rd;
    reg [MST_BRIDGE_ADDR_WIDTH-1:0] addr_buffer;
    reg [MST_BRIDGE_BYTEEN_WIDTH-1:0] byteenable_buffer;
    reg [MST_BRIDGE_BURSTCOUNT_WIDTH-1:0] burstcount_buffer;
    
    wire set_wrcounter, dec_wrcounter;
    wire [MST_BRIDGE_BURSTCOUNT_WIDTH-1:0] burstcount_value;	
	
    // -------------------- signals other than control signals are just pass thru -----
    assign mst_bridge_to_pr_rddata             = mst_bridge_to_sr_rddata;
    assign mst_bridge_to_pr_response           = mst_bridge_to_sr_response;
    // ---------------------------------------------------------------------------------
    assign mst_bridge_to_sr_addr               = (freeze_s2) ? addr_buffer :
                                                     mst_bridge_to_pr_addr;              
    assign mst_bridge_to_sr_byteenable         = (freeze_s2) ? byteenable_buffer :
                                                     mst_bridge_to_pr_byteenable;
    assign mst_bridge_to_sr_burstcount         = (freeze_s2) ? burstcount_buffer :
                                                     mst_bridge_to_pr_burstcount;
    												 
    assign mst_bridge_to_sr_debugaccess        = (~freeze_s2) ? mst_bridge_to_pr_debugaccess : 1'b0;
    assign mst_bridge_to_sr_lock               = (~freeze_s2) ? mst_bridge_to_pr_lock : 1'b0;
    assign mst_bridge_to_sr_beginbursttransfer = (~freeze_s2) ? mst_bridge_to_pr_beginbursttransfer : 1'b0;
    assign mst_bridge_to_pr_waitrequest        = (~freeze_s2) ? mst_bridge_to_sr_waitrequest : 1'b0;
    assign mst_bridge_to_pr_rddata_valid       = (~freeze_s2) ? mst_bridge_to_sr_rddata_valid : 1'b0;
    assign mst_bridge_to_pr_writeresponsevalid = (~freeze_s2) ? mst_bridge_to_sr_writeresponsevalid : 1'b0;
    
    if (MST_BRIDGE_WRDATA_WIDTH > 32) begin : gen0
       localparam ERROR_WRDATA                    = {{MST_BRIDGE_WRDATA_WIDTH-32{1'b0}}, 32'hDEADBEEF};
       assign mst_bridge_to_sr_wrdata             = (~freeze_s2) ? mst_bridge_to_pr_wrdata : ERROR_WRDATA;
    end
    else begin
       localparam ERROR_WRDATA                    = 32'hDEADBEEF;
       assign mst_bridge_to_sr_wrdata             = (~freeze_s2) ? mst_bridge_to_pr_wrdata : ERROR_WRDATA[MST_BRIDGE_WRDATA_WIDTH-1:0];
    end
    
    assign mst_bridge_to_sr_write              = (~freeze_s2) ? mst_bridge_to_pr_write :
                                                     (next_state == SEND_WRERROR) ? 1'b1 : 
                                                         1'b0;
    													 
    assign mst_bridge_to_sr_read               = (~freeze_s2) ? mst_bridge_to_pr_read : 
                                                     (back_pressure_rd) ? 1'b1 :
                                                         1'b0; 
    
    if (USE_WRITE_WAIT_TIME) begin : mst_gen1
    reg [MST_BRIDGE_WWT_WIDTH-1:0] writewaittime_counter;
    reg [MST_BRIDGE_RWT_WIDTH-1:0] readwaittime_counter;
    wire set_writewaittime_counter, reset_writewaittime_counter;
    wire set_readwaittime_counter, reset_readwaittime_counter;
    
        assign dec_wrcounter                    = ((wrcounter > 0) && (writewaittime_counter == WRITE_WAIT_TIME));
        assign set_wrcounter                    = ((|wrcounter == 0) && (writewaittime_counter == WRITE_WAIT_TIME));
        assign set_writewaittime_counter        = (mst_bridge_to_sr_write && (|writewaittime_counter == 0));
        assign reset_writewaittime_counter      = (writewaittime_counter == WRITE_WAIT_TIME);
        assign set_readwaittime_counter         = (mst_bridge_to_sr_read && (|readwaittime_counter == 0));
        assign reset_readwaittime_counter       = (readwaittime_counter == READ_WAIT_TIME);
    	
        always_ff @(posedge clk) begin
            if (!reset_n) begin
                readwaittime_counter <= {(MST_BRIDGE_RWT_WIDTH){1'b0}};
            end
            else begin
                if (reset_readwaittime_counter) begin
                    readwaittime_counter <= {(MST_BRIDGE_RWT_WIDTH){1'b0}};
                end
                else if (set_readwaittime_counter) begin
                    readwaittime_counter <= {{(MST_BRIDGE_RWT_WIDTH-1){1'b0}}, 1'b1};
                end
                else if ((readwaittime_counter < READ_WAIT_TIME) && (readwaittime_counter > 0)) begin
                    readwaittime_counter <= readwaittime_counter + {{(MST_BRIDGE_RWT_WIDTH-1){1'b0}}, 1'b1};
                end
            end
        end
    	
        always_ff @(posedge clk) begin
            if (!reset_n) begin
                writewaittime_counter <= {(MST_BRIDGE_WWT_WIDTH){1'b0}};
            end
            else begin
                if (reset_writewaittime_counter) begin
                    writewaittime_counter <= {(MST_BRIDGE_WWT_WIDTH){1'b0}};
                end
                else if (set_writewaittime_counter) begin
                    writewaittime_counter <= {{(MST_BRIDGE_WWT_WIDTH-1){1'b0}}, 1'b1};
                end
                else if ((writewaittime_counter < WRITE_WAIT_TIME) && (writewaittime_counter > 0)) begin
                    writewaittime_counter <= writewaittime_counter + {{(MST_BRIDGE_WWT_WIDTH-1){1'b0}}, 1'b1};
                end
            end
        end
    	
        always_ff @(posedge clk) begin
            if (!reset_n) begin
                back_pressure_wr <= 1'b0;
            end
            else if (~freeze_s2 && mst_bridge_to_sr_write && (writewaittime_counter == 0)) begin
                back_pressure_wr <= 1'b1;
            end
            else if (writewaittime_counter == WRITE_WAIT_TIME) begin
                back_pressure_wr <= 1'b0;
            end
        end
    	
        always_ff @(posedge clk) begin
            if (!reset_n) begin
                back_pressure_rd <= 1'b0;
            end
            else if (~freeze_s2 && mst_bridge_to_sr_read && (readwaittime_counter == 0)) begin
                back_pressure_rd <= 1'b1;
            end
            else if (readwaittime_counter == READ_WAIT_TIME) begin
                back_pressure_rd <= 1'b0;
            end
        end
    end
    else begin
        assign dec_wrcounter                    = ((wrcounter > 0) && mst_bridge_to_sr_write && ~mst_bridge_to_sr_waitrequest);
        assign set_wrcounter                    = ((|wrcounter == 0) && mst_bridge_to_sr_write && ~mst_bridge_to_sr_waitrequest);
    	
        always_ff @(posedge clk) begin
            if (!reset_n) begin
                back_pressure_wr <= 1'b0;
            end
            else if (~freeze_s2 && mst_bridge_to_sr_write && mst_bridge_to_sr_waitrequest) begin
                back_pressure_wr <= 1'b1;
            end
            else if (~mst_bridge_to_sr_waitrequest) begin
                back_pressure_wr <= 1'b0;
            end
        end
    	
        always_ff @(posedge clk) begin
            if (!reset_n) begin
                back_pressure_rd <= 1'b0;
            end
            else if (~freeze_s2 && mst_bridge_to_sr_read && mst_bridge_to_sr_waitrequest) begin
                back_pressure_rd <= 1'b1;
            end
            else if (~mst_bridge_to_sr_waitrequest) begin
                back_pressure_rd <= 1'b0;
            end
        end
    end
    
    if (USE_BURSTCOUNT) begin : mst_gen5
        assign burstcount_value   = mst_bridge_to_sr_burstcount;
    end
    else begin
        assign burstcount_value   = 1;
    end
    
    //-------------------------- buffer to store data during backpressure ----------------------------
    always_ff @(posedge clk) begin
        if (!reset_n) begin
            addr_buffer       <= {(MST_BRIDGE_ADDR_WIDTH){1'b0}};
            byteenable_buffer <= {(MST_BRIDGE_BYTEEN_WIDTH){1'b0}};
    		burstcount_buffer <= {(MST_BRIDGE_BURSTCOUNT_WIDTH){1'b0}};
        end
        else if (~freeze_s2 && (mst_bridge_to_pr_write || mst_bridge_to_pr_read)) begin
            addr_buffer       <= mst_bridge_to_pr_addr;
            byteenable_buffer <= mst_bridge_to_pr_byteenable;
    		burstcount_buffer <= mst_bridge_to_pr_burstcount;
        end
    end
    //------------------------------------------------------------------------------------------------
    
    always_ff @(posedge clk) begin
        if (!reset_n)
            state <= IDLE;
        else
            state <= next_state;
    end
    
    always_ff @(posedge clk) begin
        if (!reset_n) begin
            wrcounter <= {(MST_BRIDGE_BURSTCOUNT_WIDTH){1'b0}};
        end
        else begin
            if (set_wrcounter && ~dec_wrcounter) begin
                wrcounter <= burstcount_value - {{(MST_BRIDGE_BURSTCOUNT_WIDTH-1){1'b0}}, 1'b1};
            end
            if (dec_wrcounter && ~set_wrcounter) begin
                wrcounter <= wrcounter - {{(MST_BRIDGE_BURSTCOUNT_WIDTH-1){1'b0}}, 1'b1};
            end
        end
    end
    
    always_comb begin
        case (state)
            IDLE: begin
                if (freeze_s2 && (back_pressure_wr || wrcounter > 0)) begin
                    next_state = SEND_WRERROR;
                end
                else begin
                    next_state = IDLE;
                end
            end
    
            SEND_WRERROR: begin
                if (back_pressure_wr || wrcounter > 0) begin
                    next_state = SEND_WRERROR;
                end
                else begin
                    next_state = IDLE;
                end
            end
            
            default: begin
                next_state = IDLE;
            end
        endcase
    end
    
    always_ff @(posedge clk) begin
        if (!reset_n) begin
            illegal_request                     <= 1'b0;
        end
        else begin
            illegal_request                     <= illegal_request_combi;
        end
    end
    
    always_comb begin                         
        case (next_state)             
            IDLE: begin                  
                illegal_request_combi                     = 1'b0;
            end
    		
            SEND_WRERROR: begin
                illegal_request_combi                     = 1'b1;
            end
    
            default: begin
                illegal_request_combi                     = 1'b0;
            end
        endcase
    end
end
endgenerate


endmodule
