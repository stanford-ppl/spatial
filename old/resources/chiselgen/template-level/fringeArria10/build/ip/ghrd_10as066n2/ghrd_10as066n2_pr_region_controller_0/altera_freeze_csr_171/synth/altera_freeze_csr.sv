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

module altera_freeze_csr #(
	parameter NUM_INTF_BRIDGE = 1
)(
	input 	wire                         clk,
	input 	wire                         reset_n,
	                                                   			
	// ports to access csr                                   			
	input 	wire                         avl_csr_write,
	input 	wire                         avl_csr_read,
	input 	wire  [ 1:0]                 avl_csr_addr,
	input 	wire  [31:0]                 avl_csr_wrdata,
	output 	reg   [31:0]                 avl_csr_rddata,
	                                     
	input   wire                         freeze_status,
	output  reg                          freeze_req,
	output  reg                          unfreeze_req,
	input   wire                         unfreeze_status,
	output  reg                          reset_req,
	input   wire  [NUM_INTF_BRIDGE-1:0]  illegal_req,
	
	output  reg                          irq
);

localparam CSR_VERSION = 32'hAD000003;

wire access_csr_status, access_csr_ctrl, access_csr_illegal_req, access_csr_reg_version;
wire read_csr_status, read_csr_ctrl, read_csr_illegal_req, read_csr_reg_version, write_csr_ctrl, write_csr_illegal_req;
wire set_freeze_req, clear_freeze_req, set_unfreeze_req, clear_unfreeze_req;
wire [31:0] csr_rddata_combi;
reg [2:0] csr_ctrl;
reg [1:0] csr_status;
reg [31:0] csr_reg_version;
reg [NUM_INTF_BRIDGE-1:0] csr_illegal_req;
reg unfreeze_status_reg, freeze_status_reg;

assign access_csr_status     = (avl_csr_addr == 2'b00);
assign access_csr_ctrl       = (avl_csr_addr == 2'b01);
assign access_csr_illegal_req= (avl_csr_addr == 2'b10);
assign access_csr_reg_version= (avl_csr_addr == 2'b11);
                             
assign read_csr_status       = avl_csr_read & access_csr_status;
assign read_csr_ctrl         = avl_csr_read & access_csr_ctrl;
assign read_csr_illegal_req  = avl_csr_read & access_csr_illegal_req;
assign read_csr_reg_version  = avl_csr_read & access_csr_reg_version;
assign write_csr_ctrl        = avl_csr_write & access_csr_ctrl;
assign write_csr_illegal_req = avl_csr_write & access_csr_illegal_req;
                             
assign freeze_req            = csr_ctrl[0];
assign reset_req             = csr_ctrl[1];
assign unfreeze_req          = csr_ctrl[2];

assign irq = |csr_illegal_req;

assign set_freeze_req        = write_csr_ctrl && avl_csr_wrdata[0];
assign clear_freeze_req      = (write_csr_ctrl && ~avl_csr_wrdata[0]) || (freeze_status && ~freeze_status_reg);    // edge detector for freeze_status
assign set_unfreeze_req      = write_csr_ctrl && avl_csr_wrdata[2];
assign clear_unfreeze_req    = (write_csr_ctrl && ~avl_csr_wrdata[2]) || (unfreeze_status && ~unfreeze_status_reg);    // edge detector for unfreeze_status

always_ff @(posedge clk) begin
    if (!reset_n) begin
        freeze_status_reg      <= 1'b0;
        unfreeze_status_reg    <= 1'b0;
        avl_csr_rddata         <= {32{1'b0}};
    end
    else begin
        freeze_status_reg      <= freeze_status;
        unfreeze_status_reg    <= unfreeze_status;
        avl_csr_rddata         <= csr_rddata_combi;
    end
end

// write to freeze_req
always_ff @(posedge clk) begin
    if (~reset_n) begin
        csr_ctrl[0]    <= 1'b0;
    end
    else begin
        if (clear_freeze_req) begin
            csr_ctrl[0]    <= 1'b0;
        end
        if (set_freeze_req && ~set_unfreeze_req) begin
            csr_ctrl[0]    <= 1'b1;
        end
    end
end

// write to reset_req
always_ff @(posedge clk) begin
    if (~reset_n) begin
        csr_ctrl[1]    <= 1'b0;
    end
    else begin
        if (write_csr_ctrl) begin
            csr_ctrl[1]    <= avl_csr_wrdata[1];
        end
    end
end

// write to unfreeze_req
always_ff @(posedge clk) begin
    if (~reset_n) begin
        csr_ctrl[2]    <= 1'b0;
    end
    else begin
        if (clear_unfreeze_req) begin
            csr_ctrl[2]    <= 1'b0;
        end
        if (set_unfreeze_req && ~set_freeze_req) begin
            csr_ctrl[2]    <= 1'b1;
        end
    end
end

// write to status register
always_ff @(posedge clk) begin
    if (~reset_n) begin
        csr_status    <= {2{1'b0}};
    end
    else begin
        csr_status[0]    <= freeze_status;
        csr_status[1]    <= unfreeze_status;
    end
end

// write to version register
always_ff @(posedge clk) begin
    if (~reset_n) begin
        csr_reg_version    <= {32{1'b0}};
    end
    else begin
        csr_reg_version    <= CSR_VERSION;    // this is for the software driver to identify the version number of the CSR register
    end
end

// write to illegal request register
genvar i;
generate for (i=0; i<NUM_INTF_BRIDGE; i++) begin : illegal_req_blk
    always_ff @(posedge clk) begin
        if (~reset_n) begin
            csr_illegal_req[i]    <= {1{1'b0}};
        end
        else if (illegal_req[i]) begin
            csr_illegal_req[i]    <= {1{1'b1}};
        end
		else if (write_csr_illegal_req && avl_csr_wrdata[i]) begin // write one to clear
            csr_illegal_req[i]    <= {1{1'b0}};
        end
    end
end
endgenerate

// read from status/control register
assign csr_rddata_combi = read_csr_status ? {{30{1'b0}}, csr_status} : 
                            read_csr_ctrl ? {{29{1'b0}}, csr_ctrl} : 
                                read_csr_illegal_req ? {{32-NUM_INTF_BRIDGE{1'b0}}, csr_illegal_req} :
                                    read_csr_reg_version ? csr_reg_version : {32{1'b0}};

endmodule
