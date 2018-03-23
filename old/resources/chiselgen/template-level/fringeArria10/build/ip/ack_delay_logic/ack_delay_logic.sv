module ack_delay_logic (
  input  wire [31:0]   delay_ack_pio,    
  input  wire          ack_in,  
  output wire		   ack_delay_out, 
  input  wire          clk,
  input  wire          reset  
);

  wire 		  ack_loopback_delay_ver;
  reg [1:0]   state, next_state;
  reg [31:0]  ack_delay_counter;
  reg 		  start_ack_count;
  reg 		  en_ack_loopback;
  reg 		  reset_ack_count;
  
localparam [1:0] IDLE 				   = 2'b00;
localparam [1:0] ACK_DELAY_COUNT  	   = 2'b01;
localparam [1:0] ENABLE_ACK_LOOPBACK   = 2'b10;
localparam [1:0] RESET_ACK_DELAY_COUNT = 2'b11;

always @(posedge clk or posedge reset) begin
    if (reset) begin
        ack_delay_counter <= 32'd0;
    end
    else begin
        if (start_ack_count) begin
            ack_delay_counter <= ack_delay_counter + 32'd1;
        end
		if (reset_ack_count) begin
            ack_delay_counter <= 32'd0;
        end
    end
end

always @(posedge clk or posedge reset) begin
    if (reset) begin
        state <= IDLE;
    end else begin
        state <= next_state;
	 end
end
 
always @* begin
	case (state)
		IDLE: begin
			if (ack_in) begin
				if (delay_ack_pio == 0) begin
				next_state = ENABLE_ACK_LOOPBACK;
				end else begin
				next_state = ACK_DELAY_COUNT;
				end
			end
			else begin
			next_state = IDLE;
			end
		end
	
		ACK_DELAY_COUNT: begin
			if (ack_delay_counter == delay_ack_pio) begin
			next_state = ENABLE_ACK_LOOPBACK;
			end
			else begin
			next_state = ACK_DELAY_COUNT;
			end
		end
		
		ENABLE_ACK_LOOPBACK: begin
			if (~ack_in) begin
			next_state = RESET_ACK_DELAY_COUNT;
			end 
			else begin
			next_state = ENABLE_ACK_LOOPBACK;
			end
		end
		
		RESET_ACK_DELAY_COUNT: begin
			next_state = IDLE;
		end

		default: begin
			next_state = 2'bxx;
		end
	endcase
end 
 
always @(next_state) begin
  if (next_state == ACK_DELAY_COUNT) begin
	start_ack_count <= 1'b1;
  end else 
	start_ack_count <= 1'b0;		
end 

always @(next_state) begin
  if (next_state == ENABLE_ACK_LOOPBACK) begin
	 en_ack_loopback <= 1'b1;
  end else 
	 en_ack_loopback <= 1'b0;
end 

always @(next_state) begin
  if (next_state == RESET_ACK_DELAY_COUNT || next_state == IDLE) begin
	reset_ack_count <= 1'b1;
  end else 
	reset_ack_count <= 1'b0;		
end 

assign ack_delay_out = (en_ack_loopback) ? ack_in : 1'b0;

endmodule


