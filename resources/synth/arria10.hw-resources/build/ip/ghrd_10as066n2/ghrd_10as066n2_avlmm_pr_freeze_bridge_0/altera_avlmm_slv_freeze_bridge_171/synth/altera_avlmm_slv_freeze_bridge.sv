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

module altera_avlmm_slv_freeze_bridge #(
    parameter ENABLE_TRAFFIC_TRACKING        = 0,
    parameter ENABLE_FREEZE_FROM_PR_REGION   = 0,
    parameter USE_BURSTCOUNT                 = 1,
    parameter USE_READ_DATA_VALID            = 1,
    parameter USE_READ_WAIT_TIME             = 0,
    parameter USE_WRITE_WAIT_TIME            = 0,
    parameter USE_WRRESPONSEVALID            = 0,
    parameter ENABLED_BACKPRESSURE_BRIDGE    = 0,
    parameter SLV_BRIDGE_READ_WAIT_TIME      = 1,
    parameter SLV_BRIDGE_WRITE_WAIT_TIME     = 1,
    parameter SLV_BRIDGE_FIX_READ_LATENCY    = 0,
    parameter SLV_BRIDGE_BYTEEN_WIDTH        = 4,
    parameter SLV_BRIDGE_ADDR_WIDTH          = 32,
    parameter SLV_BRIDGE_WRDATA_WIDTH        = 32,
    parameter SLV_BRIDGE_RDDATA_WIDTH        = 32,
    parameter SLV_BRIDGE_BURSTCOUNT_WIDTH    = 7,
    parameter SLV_BRIDGE_FIX_RDLATENCY_WIDTH = 1,
    parameter SLV_BRIDGE_RWT_WIDTH           = 1,
    parameter SLV_BRIDGE_WWT_WIDTH           = 1,
    parameter SLV_BRIDGE_MAX_RDTRANS_WIDTH   = 3,
    parameter SLV_BRIDGE_MAX_WRTRANS_WIDTH   = 3
)(                                                
	input                                              clk,
	input                                              reset_n,
	                                                   
	// avalon mm slave interface to static region                           			
	input  wire                                        slv_bridge_to_sr_write,
	input  wire                                        slv_bridge_to_sr_read,
	input  wire    [SLV_BRIDGE_ADDR_WIDTH-1       :0]  slv_bridge_to_sr_addr,
	input  wire    [SLV_BRIDGE_WRDATA_WIDTH-1     :0]  slv_bridge_to_sr_wrdata,
	input  wire    [SLV_BRIDGE_BYTEEN_WIDTH-1     :0]  slv_bridge_to_sr_byteenable,
	input  wire    [SLV_BRIDGE_BURSTCOUNT_WIDTH-1 :0]  slv_bridge_to_sr_burstcount,
	input  wire                                        slv_bridge_to_sr_beginbursttransfer,
	input  wire                                        slv_bridge_to_sr_debugaccess,
	output reg 	   [SLV_BRIDGE_RDDATA_WIDTH-1     :0]  slv_bridge_to_sr_rddata,
	output reg                                         slv_bridge_to_sr_rddata_valid,
	output reg                                         slv_bridge_to_sr_waitrequest,
	output reg     [1                        :0]       slv_bridge_to_sr_response,
	input  wire                                        slv_bridge_to_sr_lock,
	output reg                                         slv_bridge_to_sr_writeresponsevalid,
	                                                   
	// avalon mm master interface to PR region         
	output reg                                         slv_bridge_to_pr_write,
	output reg                                         slv_bridge_to_pr_read,
	output reg      [SLV_BRIDGE_ADDR_WIDTH-1       :0] slv_bridge_to_pr_addr,
	output reg      [SLV_BRIDGE_WRDATA_WIDTH-1     :0] slv_bridge_to_pr_wrdata,
	output reg      [SLV_BRIDGE_BYTEEN_WIDTH-1     :0] slv_bridge_to_pr_byteenable,
	output reg      [SLV_BRIDGE_BURSTCOUNT_WIDTH-1 :0] slv_bridge_to_pr_burstcount,
	output reg                                         slv_bridge_to_pr_beginbursttransfer,
	output reg                                         slv_bridge_to_pr_debugaccess,
	input  wire     [SLV_BRIDGE_RDDATA_WIDTH-1     :0] slv_bridge_to_pr_rddata,
	input  wire                                        slv_bridge_to_pr_rddata_valid,
	input  wire                                        slv_bridge_to_pr_waitrequest,
	input  wire     [1                        :0]      slv_bridge_to_pr_response,
	output reg                                         slv_bridge_to_pr_lock,
	input  wire                                        slv_bridge_to_pr_writeresponsevalid,
	                                                   
	// conduit freeze                                  
	input  wire                                        freeze,
	input  wire                                        pr_freeze,
	output reg                                         illegal_request
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
    assign slv_bridge_to_sr_rddata             = slv_bridge_to_pr_rddata;
    assign slv_bridge_to_sr_response           = slv_bridge_to_pr_response; 
    assign slv_bridge_to_pr_addr               = slv_bridge_to_sr_addr;               
    assign slv_bridge_to_pr_wrdata             = slv_bridge_to_sr_wrdata;
    assign slv_bridge_to_pr_byteenable         = slv_bridge_to_sr_byteenable;
    assign slv_bridge_to_pr_burstcount         = slv_bridge_to_sr_burstcount;
    // ---------------------------------------------------------------------------------
    assign slv_bridge_to_pr_read               = (~freeze_s2) ? slv_bridge_to_sr_read : 1'b0; 
    assign slv_bridge_to_pr_write              = (~freeze_s2) ? slv_bridge_to_sr_write : 1'b0;
    assign slv_bridge_to_pr_beginbursttransfer = (~freeze_s2) ? slv_bridge_to_sr_beginbursttransfer : 1'b0;
    assign slv_bridge_to_pr_debugaccess        = (~freeze_s2) ? slv_bridge_to_sr_debugaccess : 1'b0;
    assign slv_bridge_to_pr_lock               = (~freeze_s2) ? slv_bridge_to_sr_lock : 1'b0;
    assign slv_bridge_to_sr_rddata_valid       = (~freeze_s2) ? slv_bridge_to_pr_rddata_valid : 1'b0;
    assign slv_bridge_to_sr_writeresponsevalid = (~freeze_s2) ? slv_bridge_to_pr_writeresponsevalid : 1'b0;
    assign slv_bridge_to_sr_waitrequest        = (~freeze_s2) ? slv_bridge_to_pr_waitrequest : 1'b0;
end
else begin : gen12
    reg [2:0] state, next_state;
    reg illegal_request_combi;
    reg [SLV_BRIDGE_MAX_RDTRANS_WIDTH-1:0] rdcounter;
    reg [SLV_BRIDGE_MAX_WRTRANS_WIDTH-1:0] wrresponse_counter;
    reg [SLV_BRIDGE_RWT_WIDTH-1:0] readwaittime_counter;
    reg [SLV_BRIDGE_WWT_WIDTH-1:0] writewaittime_counter;
    reg [SLV_BRIDGE_BURSTCOUNT_WIDTH-1:0] wrcounter;  
    
    wire inc_rdcounter, dec_rdcounter;
    wire set_wrcounter, dec_wrcounter;
    wire [SLV_BRIDGE_BURSTCOUNT_WIDTH-1:0] burstcount_value;
	
    localparam [2:0] IDLE                = 3'b000;
    localparam [2:0] FREEZE_INTF         = 3'b001;
    localparam [2:0] SEND_RDERROR        = 3'b010;
    localparam [2:0] SEND_WRERROR        = 3'b011;
    localparam READ_LATENCY              = SLV_BRIDGE_FIX_READ_LATENCY;
    localparam READ_WAIT_TIME            = USE_READ_WAIT_TIME ? SLV_BRIDGE_READ_WAIT_TIME : 0;
    localparam WRITE_WAIT_TIME           = USE_WRITE_WAIT_TIME ? SLV_BRIDGE_WRITE_WAIT_TIME : 0;
    localparam USE_WAIT_TIME             = (USE_WRITE_WAIT_TIME || USE_READ_WAIT_TIME) ? 1 : 0;
	
    // -------------------- signals other than control signals are just pass thru -----
    assign slv_bridge_to_pr_addr               = slv_bridge_to_sr_addr;               
    assign slv_bridge_to_pr_wrdata             = slv_bridge_to_sr_wrdata;
    assign slv_bridge_to_pr_byteenable         = slv_bridge_to_sr_byteenable;
    assign slv_bridge_to_pr_burstcount         = slv_bridge_to_sr_burstcount;
    // ---------------------------------------------------------------------------------
    assign slv_bridge_to_pr_read               = (~freeze_s2) ? slv_bridge_to_sr_read : 1'b0; 
    assign slv_bridge_to_pr_write              = (~freeze_s2) ? slv_bridge_to_sr_write : 1'b0;
    assign slv_bridge_to_pr_beginbursttransfer = (~freeze_s2) ? slv_bridge_to_sr_beginbursttransfer : 1'b0;
    
    if (SLV_BRIDGE_RDDATA_WIDTH > 32) begin : gen0
       localparam ERROR_RDDATA                    = {{SLV_BRIDGE_RDDATA_WIDTH-32{1'b0}}, 32'hDEADBEEF};
       assign slv_bridge_to_sr_rddata             = (~freeze_s2) ? slv_bridge_to_pr_rddata : ERROR_RDDATA;
    end
    else begin
       localparam ERROR_RDDATA                    = 32'hDEADBEEF;
       assign slv_bridge_to_sr_rddata             = (~freeze_s2) ? slv_bridge_to_pr_rddata : ERROR_RDDATA[SLV_BRIDGE_RDDATA_WIDTH-1:0];
    end
    
    assign slv_bridge_to_pr_debugaccess        = (~freeze_s2) ? slv_bridge_to_sr_debugaccess : 1'b0;
    assign slv_bridge_to_pr_lock               = (~freeze_s2) ? slv_bridge_to_sr_lock : 1'b0;
    assign slv_bridge_to_sr_response           = (~freeze_s2) ? slv_bridge_to_pr_response : 2'b10; 
    assign slv_bridge_to_sr_rddata_valid       = (~freeze_s2) ? slv_bridge_to_pr_rddata_valid : 
    	                                             (next_state == SEND_RDERROR) ? 1'b1 : 
    											             1'b0;
    if (USE_WRRESPONSEVALID) begin : gen2
        assign slv_bridge_to_sr_writeresponsevalid = (~freeze_s2) ? slv_bridge_to_pr_writeresponsevalid : 
	                                                     (next_state == SEND_WRERROR) ? 1'b1 : 
											                 1'b0;
    end
    else begin
        assign slv_bridge_to_sr_writeresponsevalid = 1'b0;
    end

    if (ENABLED_BACKPRESSURE_BRIDGE) begin : gen5
        assign slv_bridge_to_sr_waitrequest        = (~freeze_s2) ? slv_bridge_to_pr_waitrequest :
								                         1'b1;
    end
    else begin // this is to backpressure incoming write/read request during responses so that MPRT is not violated. 
		if (USE_WRRESPONSEVALID) begin : gen7
            assign slv_bridge_to_sr_waitrequest        = (~freeze_s2) ? slv_bridge_to_pr_waitrequest :
                                                             ((next_state == SEND_RDERROR) || (next_state == SEND_WRERROR)) ? 1'b1 :    
                                                                 1'b0;
		end
		else begin
			assign slv_bridge_to_sr_waitrequest        = (~freeze_s2) ? slv_bridge_to_pr_waitrequest :
                                                             (next_state == SEND_RDERROR) ? 1'b1 :    
                 				                                 1'b0;
		end
    end
															 
    `ifdef ALTERA_FREEZE_ASSERTION_ON
        `include "altera_avlmm_slv_freeze_bridge_checker.svh"
    `endif	
    
    if (USE_WAIT_TIME) begin : gen1
    wire set_writewaittime_counter, reset_writewaittime_counter;
    wire set_readwaittime_counter, reset_readwaittime_counter;
    
        assign dec_wrcounter                    = ((wrcounter > 0) && slv_bridge_to_sr_write && (writewaittime_counter == WRITE_WAIT_TIME));
        assign set_wrcounter                    = ((|wrcounter == 0) && slv_bridge_to_sr_write && (writewaittime_counter == WRITE_WAIT_TIME));
        assign set_writewaittime_counter        = (slv_bridge_to_sr_write && (|writewaittime_counter == 0));
        assign reset_writewaittime_counter      = (writewaittime_counter == WRITE_WAIT_TIME);
    	assign inc_rdcounter                    = (slv_bridge_to_sr_read && (readwaittime_counter == READ_WAIT_TIME));
        assign set_readwaittime_counter         = (slv_bridge_to_sr_read && (|readwaittime_counter == 0));
        assign reset_readwaittime_counter       = (readwaittime_counter == READ_WAIT_TIME);
    	
    	always_ff @(posedge clk) begin
            if (!reset_n) begin
                readwaittime_counter <= {(SLV_BRIDGE_RWT_WIDTH){1'b0}};
            end
            else begin
                if (reset_readwaittime_counter) begin
                    readwaittime_counter <= {(SLV_BRIDGE_RWT_WIDTH){1'b0}};
                end
                else if (set_readwaittime_counter) begin
                    readwaittime_counter <= {{(SLV_BRIDGE_RWT_WIDTH-1){1'b0}}, 1'b1}; 
                end
                else if ((readwaittime_counter < READ_WAIT_TIME) && (readwaittime_counter > 0)) begin
                    readwaittime_counter <= readwaittime_counter + {{(SLV_BRIDGE_RWT_WIDTH-1){1'b0}}, 1'b1};
                end
            end
        end
        
        always_ff @(posedge clk) begin
            if (!reset_n) begin
                writewaittime_counter <= {(SLV_BRIDGE_WWT_WIDTH){1'b0}};
            end
            else begin
                if (reset_writewaittime_counter) begin
                    writewaittime_counter <= {(SLV_BRIDGE_WWT_WIDTH){1'b0}};
                end
                else if (set_writewaittime_counter) begin
                    writewaittime_counter <= {{(SLV_BRIDGE_WWT_WIDTH-1){1'b0}}, 1'b1};
                end
                else if ((writewaittime_counter < WRITE_WAIT_TIME) && (writewaittime_counter > 0)) begin
                    writewaittime_counter <= writewaittime_counter + {{(SLV_BRIDGE_WWT_WIDTH-1){1'b0}}, 1'b1};
                end
            end
        end
    end
    else begin
        assign dec_wrcounter                    = ((wrcounter > 0) && slv_bridge_to_sr_write && ~slv_bridge_to_sr_waitrequest);
        assign set_wrcounter                    = ((|wrcounter == 0) && slv_bridge_to_sr_write && ~slv_bridge_to_sr_waitrequest);
    	assign inc_rdcounter                    = (slv_bridge_to_sr_read && ~slv_bridge_to_sr_waitrequest);
    end
    
    if (USE_READ_DATA_VALID) begin : gen3
        assign dec_rdcounter                    = (slv_bridge_to_sr_rddata_valid && (rdcounter > 0));
    end  // when readdatavalid is not used, bursting is not allow
    else begin
    wire set_fixlatency_rdcounter, reset_fixlatency_rdcounter;
    reg [SLV_BRIDGE_FIX_RDLATENCY_WIDTH-1:0] fixlatency_rdcounter;
    
    	if (USE_WAIT_TIME) begin : gen6
    		assign set_fixlatency_rdcounter         = (slv_bridge_to_sr_read && (readwaittime_counter == READ_WAIT_TIME) && (|fixlatency_rdcounter == 0));
    	end
    	else begin
    		assign set_fixlatency_rdcounter         = (slv_bridge_to_sr_read && ~slv_bridge_to_sr_waitrequest && (|fixlatency_rdcounter == 0));
    	end
    	
        assign reset_fixlatency_rdcounter       = (fixlatency_rdcounter == READ_LATENCY);
        assign dec_rdcounter                    = ((|fixlatency_rdcounter == 0) && (rdcounter > 0));
    	
    	always_ff @(posedge clk) begin
            if (!reset_n) begin
                fixlatency_rdcounter <= {(SLV_BRIDGE_FIX_RDLATENCY_WIDTH){1'b0}};
            end
            else begin
                if (reset_fixlatency_rdcounter) begin
                    fixlatency_rdcounter <= {(SLV_BRIDGE_FIX_RDLATENCY_WIDTH){1'b0}};
                end
                else if (set_fixlatency_rdcounter) begin
                    fixlatency_rdcounter <= {{(SLV_BRIDGE_FIX_RDLATENCY_WIDTH-1){1'b0}}, 1'b1};
                end
                else if ((fixlatency_rdcounter < READ_LATENCY) && (fixlatency_rdcounter > 0)) begin
                    fixlatency_rdcounter <= fixlatency_rdcounter + {{(SLV_BRIDGE_FIX_RDLATENCY_WIDTH-1){1'b0}}, 1'b1};
                end
            end
        end	
    end
    
    if (USE_BURSTCOUNT) begin : gen4
        assign burstcount_value                 = slv_bridge_to_sr_burstcount;
    end
    else begin
        assign burstcount_value                 = 1;
    end
    
    if (USE_WRRESPONSEVALID) begin : gen8
       always_ff @(posedge clk) begin
            if (!reset_n) begin
                wrresponse_counter <= {(SLV_BRIDGE_MAX_WRTRANS_WIDTH){1'b0}};
            end
            else begin
                if (set_wrcounter && ~slv_bridge_to_sr_writeresponsevalid) begin
                    wrresponse_counter <= wrresponse_counter + {{(SLV_BRIDGE_MAX_WRTRANS_WIDTH-1){1'b0}}, 1'b1};
                end
                if (slv_bridge_to_sr_writeresponsevalid && ~set_wrcounter) begin
                    wrresponse_counter <= wrresponse_counter - {{(SLV_BRIDGE_MAX_WRTRANS_WIDTH-1){1'b0}}, 1'b1};
                end
            end
        end
    end
    else begin
        assign wrresponse_counter = {(SLV_BRIDGE_MAX_WRTRANS_WIDTH){1'b0}};
    end
    
    always_ff @(posedge clk) begin
        if (!reset_n) begin
            rdcounter <= {(SLV_BRIDGE_MAX_RDTRANS_WIDTH){1'b0}};
        end
        else begin
            if (inc_rdcounter && ~dec_rdcounter) begin
                rdcounter <= rdcounter + burstcount_value;
            end
            if (dec_rdcounter && ~inc_rdcounter) begin
                rdcounter <= rdcounter - {{(SLV_BRIDGE_MAX_RDTRANS_WIDTH-1){1'b0}}, 1'b1};
            end
    		if (dec_rdcounter && inc_rdcounter) begin
                rdcounter <= rdcounter + burstcount_value - {{(SLV_BRIDGE_MAX_RDTRANS_WIDTH-1){1'b0}}, 1'b1};
            end
        end
    end
    
    always_ff @(posedge clk) begin
        if (!reset_n) begin
            wrcounter <= {(SLV_BRIDGE_BURSTCOUNT_WIDTH){1'b0}};
        end
        else begin
            if (set_wrcounter && ~dec_wrcounter) begin
                wrcounter <= burstcount_value - {{(SLV_BRIDGE_BURSTCOUNT_WIDTH-1){1'b0}}, 1'b1};
            end
            if (dec_wrcounter && ~set_wrcounter) begin
                wrcounter <= wrcounter - {{(SLV_BRIDGE_BURSTCOUNT_WIDTH-1){1'b0}}, 1'b1};
            end
        end
    end
    
    always_ff @(posedge clk) begin
        if (!reset_n)
            state <= IDLE;
        else
            state <= next_state;
    end
    
    always_comb begin
        case (state)
            IDLE: begin
                if (freeze_s2) begin
                    next_state = FREEZE_INTF;
                end
                else begin
                    next_state = IDLE;
                end
            end
    		
            FREEZE_INTF: begin
                if (~freeze_s2) begin
                    next_state = IDLE;
                end
                else if (rdcounter > 0) begin
                    next_state = SEND_RDERROR;
                end
                else if (((wrresponse_counter > 0) && (wrcounter == 0)) || ((wrresponse_counter == 0) && (wrcounter > 0))) begin
                    next_state = SEND_WRERROR;
                end
                else begin
                    next_state = FREEZE_INTF;
                end
            end
            
            SEND_RDERROR: begin
                if (rdcounter > 0) begin
                	next_state = SEND_RDERROR;
                end
                else if (((wrresponse_counter > 0) && (wrcounter == 0)) || ((wrresponse_counter == 0) && (wrcounter > 0))) begin
                	next_state = SEND_WRERROR;
                end
                else begin
                	next_state = FREEZE_INTF;
                end
            end
            
            SEND_WRERROR: begin
                if (rdcounter > 0) begin
                	next_state = SEND_RDERROR;
                end
                else if (((wrresponse_counter > 0) && (wrcounter == 0)) || ((wrresponse_counter == 0) && (wrcounter > 0))) begin
                	next_state = SEND_WRERROR;
                end
                else begin
                	next_state = FREEZE_INTF;
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
    
            FREEZE_INTF: begin
                illegal_request_combi                     = 1'b0;
            end
            
            SEND_RDERROR: begin
                illegal_request_combi                     = 1'b1;
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
