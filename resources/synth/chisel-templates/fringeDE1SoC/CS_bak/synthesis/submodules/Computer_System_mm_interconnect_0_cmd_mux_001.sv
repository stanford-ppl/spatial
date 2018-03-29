// (C) 2001-2016 Intel Corporation. All rights reserved.
// Your use of Intel Corporation's design tools, logic functions and other 
// software and tools, and its AMPP partner logic functions, and any output 
// files any of the foregoing (including device programming or simulation 
// files), and any associated documentation or information are expressly subject 
// to the terms and conditions of the Intel Program License Subscription 
// Agreement, Intel MegaCore Function License Agreement, or other applicable 
// license agreement, including, without limitation, that your use is for the 
// sole purpose of programming logic devices manufactured by Intel and sold by 
// Intel or its authorized distributors.  Please refer to the applicable 
// agreement for further details.


// (C) 2001-2014 Altera Corporation. All rights reserved.
// Your use of Altera Corporation's design tools, logic functions and other 
// software and tools, and its AMPP partner logic functions, and any output 
// files any of the foregoing (including device programming or simulation 
// files), and any associated documentation or information are expressly subject 
// to the terms and conditions of the Altera Program License Subscription 
// Agreement, Altera MegaCore Function License Agreement, or other applicable 
// license agreement, including, without limitation, that your use is for the 
// sole purpose of programming logic devices manufactured by Altera and sold by 
// Altera or its authorized distributors.  Please refer to the applicable 
// agreement for further details.


// $Id: //acds/rel/16.1/ip/merlin/altera_merlin_multiplexer/altera_merlin_multiplexer.sv.terp#1 $
// $Revision: #1 $
// $Date: 2016/08/07 $
// $Author: swbranch $

// ------------------------------------------
// Merlin Multiplexer
// ------------------------------------------

`timescale 1 ns / 1 ns


// ------------------------------------------
// Generation parameters:
//   output_name:         Computer_System_mm_interconnect_0_cmd_mux_001
//   NUM_INPUTS:          4
//   ARBITRATION_SHARES:  1 1 1 1
//   ARBITRATION_SCHEME   "round-robin"
//   PIPELINE_ARB:        1
//   PKT_TRANS_LOCK:      54 (arbitration locking enabled)
//   ST_DATA_W:           111
//   ST_CHANNEL_W:        5
// ------------------------------------------

module Computer_System_mm_interconnect_0_cmd_mux_001
(
    // ----------------------
    // Sinks
    // ----------------------
    input                       sink0_valid,
    input [111-1   : 0]  sink0_data,
    input [5-1: 0]  sink0_channel,
    input                       sink0_startofpacket,
    input                       sink0_endofpacket,
    output                      sink0_ready,

    input                       sink1_valid,
    input [111-1   : 0]  sink1_data,
    input [5-1: 0]  sink1_channel,
    input                       sink1_startofpacket,
    input                       sink1_endofpacket,
    output                      sink1_ready,

    input                       sink2_valid,
    input [111-1   : 0]  sink2_data,
    input [5-1: 0]  sink2_channel,
    input                       sink2_startofpacket,
    input                       sink2_endofpacket,
    output                      sink2_ready,

    input                       sink3_valid,
    input [111-1   : 0]  sink3_data,
    input [5-1: 0]  sink3_channel,
    input                       sink3_startofpacket,
    input                       sink3_endofpacket,
    output                      sink3_ready,


    // ----------------------
    // Source
    // ----------------------
    output                      src_valid,
    output [111-1    : 0] src_data,
    output [5-1 : 0] src_channel,
    output                      src_startofpacket,
    output                      src_endofpacket,
    input                       src_ready,

    // ----------------------
    // Clock & Reset
    // ----------------------
    input clk,
    input reset
);
    localparam PAYLOAD_W        = 111 + 5 + 2;
    localparam NUM_INPUTS       = 4;
    localparam SHARE_COUNTER_W  = 1;
    localparam PIPELINE_ARB     = 1;
    localparam ST_DATA_W        = 111;
    localparam ST_CHANNEL_W     = 5;
    localparam PKT_TRANS_LOCK   = 54;

    // ------------------------------------------
    // Signals
    // ------------------------------------------
    wire [NUM_INPUTS - 1 : 0]      request;
    wire [NUM_INPUTS - 1 : 0]      valid;
    wire [NUM_INPUTS - 1 : 0]      grant;
    wire [NUM_INPUTS - 1 : 0]      next_grant;
    reg [NUM_INPUTS - 1 : 0]       saved_grant;
    reg [PAYLOAD_W - 1 : 0]        src_payload;
    wire                           last_cycle;
    reg                            packet_in_progress;
    reg                            update_grant;

    wire [PAYLOAD_W - 1 : 0] sink0_payload;
    wire [PAYLOAD_W - 1 : 0] sink1_payload;
    wire [PAYLOAD_W - 1 : 0] sink2_payload;
    wire [PAYLOAD_W - 1 : 0] sink3_payload;

    assign valid[0] = sink0_valid;
    assign valid[1] = sink1_valid;
    assign valid[2] = sink2_valid;
    assign valid[3] = sink3_valid;

    wire [NUM_INPUTS - 1 : 0] eop;
    assign eop[0] = sink0_endofpacket;
    assign eop[1] = sink1_endofpacket;
    assign eop[2] = sink2_endofpacket;
    assign eop[3] = sink3_endofpacket;

    // ------------------------------------------
    // ------------------------------------------
    // Grant Logic & Updates
    // ------------------------------------------
    // ------------------------------------------
    reg [NUM_INPUTS - 1 : 0] lock;
    always @* begin
      lock[0] = sink0_data[54];
      lock[1] = sink1_data[54];
      lock[2] = sink2_data[54];
      lock[3] = sink3_data[54];
    end
    reg [NUM_INPUTS - 1 : 0] locked = '0;
    always @(posedge clk or posedge reset) begin
      if (reset) begin
        locked <= '0;
      end
      else begin
        locked <= next_grant & lock;
      end
    end

    assign last_cycle = src_valid & src_ready & src_endofpacket & ~(|(lock & grant));

    // ------------------------------------------
    // We're working on a packet at any time valid is high, except
    // when this is the endofpacket.
    // ------------------------------------------
    always @(posedge clk or posedge reset) begin
      if (reset) begin
        packet_in_progress <= 1'b0;
      end
      else begin
        if (last_cycle)
          packet_in_progress <= 1'b0; 
        else if (src_valid)
          packet_in_progress <= 1'b1;
      end
    end


    // ------------------------------------------
    // Shares
    //
    // Special case: all-equal shares _should_ be optimized into assigning a
    // constant to next_grant_share.
    // Special case: all-1's shares _should_ result in the share counter
    // being optimized away.
    // ------------------------------------------
    // Input  |  arb shares  |  counter load value
    // 0      |      1       |  0
    // 1      |      1       |  0
    // 2      |      1       |  0
    // 3      |      1       |  0
     wire [SHARE_COUNTER_W - 1 : 0] share_0 = 1'd0;
     wire [SHARE_COUNTER_W - 1 : 0] share_1 = 1'd0;
     wire [SHARE_COUNTER_W - 1 : 0] share_2 = 1'd0;
     wire [SHARE_COUNTER_W - 1 : 0] share_3 = 1'd0;

    // ------------------------------------------
    // Choose the share value corresponding to the grant.
    // ------------------------------------------
    reg [SHARE_COUNTER_W - 1 : 0] next_grant_share;
    always @* begin
      next_grant_share =
    share_0 & { SHARE_COUNTER_W {next_grant[0]} } |
    share_1 & { SHARE_COUNTER_W {next_grant[1]} } |
    share_2 & { SHARE_COUNTER_W {next_grant[2]} } |
    share_3 & { SHARE_COUNTER_W {next_grant[3]} };
    end

    // ------------------------------------------
    // Flag to indicate first packet of an arb sequence.
    // ------------------------------------------

    // ------------------------------------------
    // Compute the next share-count value.
    // ------------------------------------------
    reg [SHARE_COUNTER_W - 1 : 0] p1_share_count;
    reg [SHARE_COUNTER_W - 1 : 0] share_count;
    reg share_count_zero_flag;

    always @* begin
        // Update the counter, but don't decrement below 0.
      p1_share_count = share_count_zero_flag ? '0 : share_count - 1'b1;
     end

    // ------------------------------------------
    // Update the share counter and share-counter=zero flag.
    // ------------------------------------------
    always @(posedge clk or posedge reset) begin
      if (reset) begin
        share_count <= '0;
        share_count_zero_flag <= 1'b1;
      end
      else begin
        if (update_grant) begin
          share_count <= next_grant_share;
          share_count_zero_flag <= (next_grant_share == '0);
        end
        else if (last_cycle) begin
          share_count <= p1_share_count;
          share_count_zero_flag <= (p1_share_count == '0);
        end
      end
    end


    always @* begin
      update_grant = 0;

        // ------------------------------------------
        // The pipeline delays grant by one cycle, so
        // we have to calculate the update_grant signal
        // one cycle ahead of time.
        //
        // Possible optimization: omit the first clause
        //    "if (!packet_in_progress & ~src_valid) ..."
        //   cost: one idle cycle at the the beginning of each 
        //     grant cycle.
        //   benefit: save a small amount of logic.
        // ------------------------------------------
    if (!packet_in_progress & !src_valid)
      update_grant = 1;
    if (last_cycle && share_count_zero_flag)
      update_grant = 1;
    end

    wire save_grant;
    assign save_grant = update_grant;
    assign grant = saved_grant;

    always @(posedge clk, posedge reset) begin
      if (reset)
        saved_grant <= '0;
      else if (save_grant)
        saved_grant <= next_grant;
    end

    // ------------------------------------------
    // ------------------------------------------
    // Arbitrator
    // ------------------------------------------
    // ------------------------------------------

    // ------------------------------------------
    // Create a request vector that stays high during
    // the packet for unpipelined arbitration.
    //
    // The pipelined arbitration scheme does not require
    // request to be held high during the packet.
    // ------------------------------------------
    reg [NUM_INPUTS - 1 : 0] prev_request;
    always @(posedge clk, posedge reset) begin
      if (reset)
        prev_request <= '0;
      else
        prev_request <= request & ~(valid & eop);
    end

    assign request = (PIPELINE_ARB == 1) ? valid | locked :
    prev_request | valid | locked;

    wire [NUM_INPUTS - 1 : 0] next_grant_from_arb;
                               
    altera_merlin_arbitrator
    #(
    .NUM_REQUESTERS(NUM_INPUTS),
    .SCHEME ("round-robin"),
    .PIPELINE (1)
    ) arb (
    .clk (clk),
    .reset (reset),
    .request (request),
    .grant (next_grant_from_arb),
    .save_top_priority (src_valid),
    .increment_top_priority (update_grant)
    );

   assign next_grant = next_grant_from_arb;
                         
    // ------------------------------------------
    // ------------------------------------------
    // Mux
    //
    // Implemented as a sum of products.
    // ------------------------------------------
    // ------------------------------------------

    assign sink0_ready = src_ready && grant[0];
    assign sink1_ready = src_ready && grant[1];
    assign sink2_ready = src_ready && grant[2];
    assign sink3_ready = src_ready && grant[3];

    assign src_valid = |(grant & valid);

    always @* begin
      src_payload =
      sink0_payload & {PAYLOAD_W {grant[0]} } |
      sink1_payload & {PAYLOAD_W {grant[1]} } |
      sink2_payload & {PAYLOAD_W {grant[2]} } |
      sink3_payload & {PAYLOAD_W {grant[3]} };
    end

    // ------------------------------------------
    // Mux Payload Mapping
    // ------------------------------------------

    assign sink0_payload = {sink0_channel,sink0_data,
    sink0_startofpacket,sink0_endofpacket};
    assign sink1_payload = {sink1_channel,sink1_data,
    sink1_startofpacket,sink1_endofpacket};
    assign sink2_payload = {sink2_channel,sink2_data,
    sink2_startofpacket,sink2_endofpacket};
    assign sink3_payload = {sink3_channel,sink3_data,
    sink3_startofpacket,sink3_endofpacket};

    assign {src_channel,src_data,src_startofpacket,src_endofpacket} = src_payload;
endmodule


