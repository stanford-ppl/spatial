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

module altera_freeze_control #(
	parameter NUM_INTF_BRIDGE = 1
)(     
    input wire                        clk,
    input wire                        reset_n,

    // conduit handshake from PR region
    output reg                        start_req,
    input  wire                       start_ack,
    output reg                        region_reset,
    output reg                        stop_req,
    input  wire                       stop_ack,
    
    // conduit interface from freeze_csr block or user
    input  wire                       unfreeze_req,
    input  wire                       freeze_req,
    output reg                        freeze_status,
    input  wire                       reset_req,
    output reg                        unfreeze_status,
    output reg  [NUM_INTF_BRIDGE-1:0] illegal_req,					// how to derive this bit?
    
    // conduit freeze to freeze_bridge block
    output reg                        freeze,
    input  wire [NUM_INTF_BRIDGE-1:0] illegal_request			    // this should be multibit bus!!!!
);

`ifdef ALTERA_FREEZE_ASSERTION_ON
    `include "altera_freeze_control_checker.svh"
`endif

localparam [2:0]    IDLE                    = 3'b000;
localparam [2:0]    STOP_CURRENT_PERSONA    = 3'b001;
localparam [2:0]    START_FREEZE            = 3'b010;
localparam [2:0]    START_NEW_PERSONA       = 3'b011;

reg [2:0]  fsm_state, fsm_state_nxt;
reg freeze_combi, stop_req_combi, start_req_combi, freeze_status_combi, unfreeze_status_combi;
reg [NUM_INTF_BRIDGE-1:0] illegal_request_s1, illegal_request_s2;
reg start_ack_s1, start_ack_s2, stop_ack_s1, stop_ack_s2;

assign illegal_req       = illegal_request_s2;
assign region_reset      = (reset_req | ~reset_n);  //Input pin for applying the “soft reset” to the new PR persona.

// ------------------------- reset syncronizer -----------------------------
genvar i;
generate for (i=0; i<NUM_INTF_BRIDGE; i++) begin : reset_sync_blk
    always_ff @(posedge clk) begin
        if (!reset_n) begin
			illegal_request_s1[i] <= 1'b0;
			illegal_request_s2[i] <= 1'b0;
        end
        else begin
			illegal_request_s1[i] <= illegal_request[i];
			illegal_request_s2[i] <= illegal_request_s1[i];
    	end
    end
end
endgenerate

always_ff @(posedge clk) begin
    if (!reset_n) begin
        start_ack_s1      <= 1'b0;
		start_ack_s2      <= 1'b0;
		stop_ack_s1       <= 1'b0;
		stop_ack_s2       <= 1'b0;
    end
    else begin
		start_ack_s1      <= start_ack;
		start_ack_s2      <= start_ack_s1;
		stop_ack_s1       <= stop_ack;
		stop_ack_s2       <= stop_ack_s1;
	end
end

//-------------------------------------------------------------------------------------------

always_ff @(posedge clk) begin
    if (!reset_n)
        fsm_state <= IDLE;
    else
        fsm_state <= fsm_state_nxt;
end

always_ff @(posedge clk) begin
    if (!reset_n) begin
        freeze            <= 1'b0;
        stop_req          <= 1'b0;
        start_req         <= 1'b0;
        freeze_status     <= 1'b0;
        unfreeze_status   <= 1'b1;
    end
    else begin
        freeze            <= freeze_combi;
        stop_req          <= stop_req_combi;
        start_req         <= start_req_combi;
        freeze_status     <= freeze_status_combi;
        unfreeze_status   <= unfreeze_status_combi;
    end
end

always_comb begin
    case (fsm_state)
        IDLE: begin
            if (freeze_req)
                fsm_state_nxt = STOP_CURRENT_PERSONA;
            else
                fsm_state_nxt = IDLE;
        end
        
        STOP_CURRENT_PERSONA:  begin
            if (stop_ack_s2) 
                fsm_state_nxt = START_FREEZE;
            else if (~freeze_req)
                fsm_state_nxt = IDLE;
            else
                fsm_state_nxt = STOP_CURRENT_PERSONA;
        end
        
        START_FREEZE:  begin
            if (unfreeze_req) 
                fsm_state_nxt = START_NEW_PERSONA;
            else
                fsm_state_nxt = START_FREEZE;
        end
        
        START_NEW_PERSONA: begin
            if (start_ack_s2 || ~unfreeze_req) 
                fsm_state_nxt = IDLE;
            else 
                fsm_state_nxt = START_NEW_PERSONA;
        end
		
        default: begin
            fsm_state_nxt = IDLE;
        end
    endcase
end

always_comb begin
    freeze_combi                     = freeze;
    stop_req_combi                   = stop_req;
    start_req_combi                  = start_req;
    freeze_status_combi              = freeze_status;
    unfreeze_status_combi            = unfreeze_status;
                                     
    case (fsm_state_nxt)             
        IDLE: begin                  
            freeze_combi             = 1'b0;
            stop_req_combi           = 1'b0;
            start_req_combi          = 1'b0;
            freeze_status_combi      = 1'b0;
            unfreeze_status_combi    = 1'b1;
        end
        
        STOP_CURRENT_PERSONA:  begin
            unfreeze_status_combi    = 1'b0;
            stop_req_combi           = 1'b1;
        end

        START_FREEZE: begin
		    stop_req_combi           = 1'b0;
			freeze_combi             = 1'b1;
            freeze_status_combi      = 1'b1;
        end
                          
        START_NEW_PERSONA: begin    
            freeze_status_combi      = 1'b0;
            freeze_combi             = 1'b0;
            start_req_combi          = 1'b1;
        end                          
		
        default: begin
            freeze_combi             = 1'b0;
            stop_req_combi           = 1'b0;
            start_req_combi          = 1'b0;
            freeze_status_combi      = 1'b0;
            unfreeze_status_combi    = 1'b0;
        end
    endcase
end

    
endmodule
