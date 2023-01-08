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


// synthesis translate_off
`timescale 1ns / 1ps
// synthesis translate_on

module interrupt_latency_counter #(
parameter INTR_TYPE = 1'b1,
parameter CLOCK_RATE = 32'b1,
parameter IRQ_PORT_CNT = 6'd32
)(
   irq,
   reset_n,
   clk,
   avmm_write,
   avmm_read,
   avmm_addr,				
   avmm_wrdata,
   avmm_rddata
);
 
 input   wire  [IRQ_PORT_CNT-1:0] irq;
 input   wire  reset_n;
 input   wire  clk;
 input   wire  avmm_write;
 input	wire  avmm_read;
 input	wire    [5:0]	avmm_addr; 		
 input	wire	[31:0]  avmm_wrdata;
 output  reg	[31:0]  avmm_rddata;


//Local parameters
localparam ADDRW =32;

// two dimensional arrays to support multiple IRQ port instantiation 
reg [IRQ_PORT_CNT-1:0] irq_d;
reg [31:0] data_store_reg [0:IRQ_PORT_CNT-1];
reg [31:0] count_reg [0:IRQ_PORT_CNT-1];

// Interrupt Latency Counter CSR
reg        global_enable_reg;
reg        avmm_read_reg;
reg        avmm_write_reg;
reg [31:0] pulse_irq_counter_stop;
reg [IRQ_PORT_CNT-1:0] readdata_valid_reg;


// ILC wires
wire                    global_enable_comb;
wire                    irq_type_comb;
wire                    ilc_ctrl_access;
wire                    frequency_access;
wire                    pulse_irq_counter_stop_access;
wire                    read_data_valid_access;
wire                    irq_active_access;
wire [5:0]              irq_num_comb;
wire [23:0]             revision_comb;
wire [31:0]             ilc_ctrl_and_comb;
wire [31:0]             frequency_and_comb;
wire [31:0]             pulse_irq_counter_stop_and_comb;
wire [31:0]             read_data_valid_and_comb;
wire [31:0]             latency_data_access;
wire [31:0]             frequency_comb;
wire [23:0]             ip_version_comb;
wire [31:0]             rddata_combi;
wire [31:0]             pulse_irq_counter_stop_comb;
wire [31:0]             read_data_valid_combi;
wire [31:0]             latency_data_or;
wire [31:0]             irq_active_comb;
wire [31:0]             irq_active;
wire [IRQ_PORT_CNT-1:0] c_start;
wire [IRQ_PORT_CNT-1:0] c_stop;
wire [IRQ_PORT_CNT-1:0] level_reset;
wire [IRQ_PORT_CNT-1:0] enable;
wire [IRQ_PORT_CNT-1:0] c_idle;
wire [IRQ_PORT_CNT-1:0] data_store;
wire [31:0] latency_data_and_comb [0:31];

// Essential IP information pass through parameters

assign ip_version_comb = 2'b10; // fixed bug
assign irq_type_comb   = INTR_TYPE;
assign irq_num_comb    = IRQ_PORT_CNT;
assign frequency_comb  = CLOCK_RATE;



// and-or mux addressing
assign  latency_data_access[0]  = (avmm_addr == 6'b000000);
assign  latency_data_access[1]  = (avmm_addr == 6'b000001);
assign  latency_data_access[2]  = (avmm_addr == 6'b000010);
assign  latency_data_access[3]  = (avmm_addr == 6'b000011); 
assign  latency_data_access[4]  = (avmm_addr == 6'b000100);
assign  latency_data_access[5]  = (avmm_addr == 6'b000101);
assign  latency_data_access[6]  = (avmm_addr == 6'b000110);
assign  latency_data_access[7]  = (avmm_addr == 6'b000111);
assign  latency_data_access[8]  = (avmm_addr == 6'b001000);
assign  latency_data_access[9]  = (avmm_addr == 6'b001001);
assign  latency_data_access[10] = (avmm_addr == 6'b001010);
assign  latency_data_access[11]  = (avmm_addr == 6'b001011);
assign  latency_data_access[12]  = (avmm_addr == 6'b001100);
assign  latency_data_access[13]  = (avmm_addr == 6'b001101);
assign  latency_data_access[14]  = (avmm_addr == 6'b001110);
assign  latency_data_access[15]  = (avmm_addr == 6'b001111);
assign  latency_data_access[16]  = (avmm_addr == 6'b010000);
assign  latency_data_access[17]  = (avmm_addr == 6'b010001); 
assign  latency_data_access[18]  = (avmm_addr == 6'b010010);
assign  latency_data_access[19]  = (avmm_addr == 6'b010011);
assign  latency_data_access[20]  = (avmm_addr == 6'b010100);
assign  latency_data_access[21]  = (avmm_addr == 6'b010101);
assign  latency_data_access[22]  = (avmm_addr == 6'b010110);
assign  latency_data_access[23]  = (avmm_addr == 6'b010111);
assign  latency_data_access[24]  = (avmm_addr == 6'b011000);
assign  latency_data_access[25]  = (avmm_addr == 6'b011001);
assign  latency_data_access[26]  = (avmm_addr == 6'b011010);
assign  latency_data_access[27]  = (avmm_addr == 6'b011011);
assign  latency_data_access[28]  = (avmm_addr == 6'b011100);
assign  latency_data_access[29]  = (avmm_addr == 6'b011101);
assign  latency_data_access[30]  = (avmm_addr == 6'b011110);
assign  latency_data_access[31]  = (avmm_addr == 6'b011111);
assign ilc_ctrl_access               = (avmm_addr == 6'b100000);
assign frequency_access              = (avmm_addr == 6'b100001);
assign pulse_irq_counter_stop_access = (avmm_addr == 6'b100010);
assign read_data_valid_access        = (avmm_addr == 6'b100011);
assign irq_active_access             = (avmm_addr == 6'b100100); // new feature

assign rddata_combi = avmm_read? latency_data_or : {(ADDRW){1'b0}};

assign latency_data_or =  latency_data_and_comb[0] |
                          latency_data_and_comb[1] |
                          latency_data_and_comb[2] |
                          latency_data_and_comb[3] |
                          latency_data_and_comb[4] |
                          latency_data_and_comb[5] |
                          latency_data_and_comb[6] |
                          latency_data_and_comb[7] |
                          latency_data_and_comb[8] |
                          latency_data_and_comb[9] |
                          latency_data_and_comb[10] |
                          latency_data_and_comb[11] |
                          latency_data_and_comb[12] |
                          latency_data_and_comb[13] |
                          latency_data_and_comb[14] |
                          latency_data_and_comb[15] |
                          latency_data_and_comb[16] |
                          latency_data_and_comb[17] |
                          latency_data_and_comb[18] |
                          latency_data_and_comb[19] |
                          latency_data_and_comb[20] |
                          latency_data_and_comb[21] |
                          latency_data_and_comb[22] |
                          latency_data_and_comb[23] |
                          latency_data_and_comb[24] |
                          latency_data_and_comb[25] |
                          latency_data_and_comb[26] |
                          latency_data_and_comb[27] |
                          latency_data_and_comb[28] |
                          latency_data_and_comb[29] |
                          latency_data_and_comb[30] |
                          latency_data_and_comb[31] |
								  ilc_ctrl_and_comb         |
								  frequency_and_comb        |
								  pulse_irq_counter_stop_and_comb | 
								  read_data_valid_and_comb | irq_active_comb; // new feature

genvar a;
generate
   for(a=0;a<IRQ_PORT_CNT;a=a+1) begin : latency_data_address_array
       assign latency_data_and_comb[a] =  data_store_reg[a] & {(ADDRW){latency_data_access[a]}};
   end
	for(a=IRQ_PORT_CNT;a<32;a=a+1) begin : unused_latency_data_address
       assign latency_data_and_comb[a] =  32'b0 & {(ADDRW){latency_data_access[a]}};
   end
endgenerate

assign ilc_ctrl_and_comb               = {ip_version_comb,irq_num_comb,irq_type_comb,global_enable_reg} & {(ADDRW){ilc_ctrl_access}};
assign frequency_and_comb              =  frequency_comb  & {(ADDRW){frequency_access}};
assign pulse_irq_counter_stop_and_comb =  pulse_irq_counter_stop  & {(ADDRW){pulse_irq_counter_stop_access}};
assign read_data_valid_and_comb        =  read_data_valid_combi   & {(ADDRW){read_data_valid_access}};								  
assign irq_active_comb                 =  irq_active & {(ADDRW){irq_active_access}};	// new feature	  			

always @(posedge clk or negedge reset_n) 
  begin
     if (~reset_n) begin
        avmm_rddata <= {(ADDRW){1'b0}};
      end
      else begin
        avmm_rddata <= rddata_combi;	 
      end
  end 

// Avalon-MM write address decoding logic
assign global_enable_comb	          =  ((avmm_addr == 6'b100000) && avmm_write) ? avmm_wrdata[0] : global_enable_reg;
assign pulse_irq_counter_stop_comb	 =	 ((avmm_addr == 6'b100010) && avmm_write) ? avmm_wrdata  : pulse_irq_counter_stop;


always @(posedge clk or negedge reset_n) 
begin
    if (~reset_n) begin

     global_enable_reg <= 1'b0;
	  pulse_irq_counter_stop <= {(ADDRW){1'b0}};
    end
    else begin

      global_enable_reg <= global_enable_comb;
	   pulse_irq_counter_stop <= pulse_irq_counter_stop_comb;
    end
end

// Pulse generation based on INTR_TYPE value
genvar s;	
generate 
for(s=0;s<IRQ_PORT_CNT;s=s+1) begin : irq_detector_cicuit
 
irq_detector #(
		.INTR_TYPE        (INTR_TYPE)
) irq_detector (
		.clk   (clk),                        
		.reset_n (reset_n), 
		.irq   (irq[s]),        
	   .counter_start(c_start[s]),
	   .counter_stop(c_stop[s])
);
end

endgenerate

genvar v;
generate 
    for (v=0;v<IRQ_PORT_CNT;v=v+1) begin : data_valid_signal
	       always @ (posedge clk, negedge reset_n)
			 begin
			    if (~reset_n) begin
				      readdata_valid_reg[v] <= 1'b0;
				 end
			 else if (data_store[v] == 1'b1) begin
				      readdata_valid_reg[v] <= 1'b1;	
		    end	
          else if (avmm_read && (avmm_addr == v)) begin
			         readdata_valid_reg[v] <= 1'b0;
			 end	
		 end
	 end
endgenerate

assign read_data_valid_combi = {{(32-IRQ_PORT_CNT){1'b0}}, readdata_valid_reg};

genvar m;
generate 
for(m=0;m<IRQ_PORT_CNT;m=m+1) begin : state_machine

state_machine_counter #(
		.INTR_TYPE        (INTR_TYPE)
) state_machine_counter (
     .clk(clk),
	  .reset_n(reset_n),
	  .pulse_irq_counter_stop(pulse_irq_counter_stop[m]),
	  .global_enable_reg(global_enable_reg),
	  .counter_start(c_start[m]),
	  .counter_stop(c_stop[m]),
	  .enable(enable[m]),
	  .c_idle(c_idle[m]),
	  .level_reset(level_reset[m]),
	  .data_store(data_store[m])
);

end
endgenerate


// Latency up-counter 
genvar l;
generate
for(l=0;l<IRQ_PORT_CNT;l=l+1) begin : up_counter
  always @ (posedge clk, negedge level_reset[l])
      begin

	     if (level_reset[l] == 1'b0) begin
           count_reg[l] <=  32'd0;
        end
		  else begin
		     if (enable[l] == 1'b1) begin
			    count_reg[l] <=  count_reg[l] + 1;
		     end
		  end
			
      end 
 end
 endgenerate
	 
// Data storage register
genvar d;
generate
for(d=0;d<IRQ_PORT_CNT;d=d+1) begin : data_store_register

   always @ (posedge clk, negedge reset_n)
	   begin
	       if (reset_n == 1'b0) begin
                data_store_reg[d] <=  32'd0;
		    end
		    else begin 
		     if (data_store[d] == 1'b1) begin
	              data_store_reg[d] <= count_reg[d];
			  end     
       end
	 end
	 
 end
endgenerate 

//irq active logic
genvar i;

generate // new feature
if (INTR_TYPE == 1'b0) begin	

    for(i=0;i<IRQ_PORT_CNT;i=i+1) begin : irq_active_array_level
       assign irq_active[i] =  irq[i];
    end
	for(i=IRQ_PORT_CNT;i<32;i=i+1) begin : unused_irq_active_array_level
       assign irq_active[i] =  1'b0 ;
    end

end else begin

    for(i=0;i<IRQ_PORT_CNT;i=i+1) begin : irq_active_array_pulse
       assign irq_active[i] =  enable[i];
    end
	for(i=IRQ_PORT_CNT;i<32;i=i+1) begin : unused_irq_active_array_pulse
       assign irq_active[i] =  1'b0 ;
    end

end
endgenerate

endmodule
