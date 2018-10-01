module vesa_tpg(
 clock,
 reset,

 dout_ready,
 dout_valid,
 dout_data,
 dout_sop,
 dout_eop,
 
 bpp        // 3'b000 = 18bpp, 3'b001 = 24bpp
);

parameter MAX_BPC = 8;

input clock;
input reset;

input dout_ready;
output dout_valid;
output [MAX_BPC*3-1:0] dout_data;
output dout_sop;
output dout_eop;
wire [1:0] dout_empty;

input [2:0] bpp;

assign dout_empty = 0;

parameter WIDTH=1920;
parameter HEIGHT=1080;
localparam CTRL_PKT_NUM=3;
localparam CTRL_PKT_HEADER=24'd15;
localparam DATA_PKT_HEADER=24'd0;


///////////////////////

reg [11:0] dot_cnt;
reg [11:0] line_cnt;


///////////////////////
// Catch ready signal
reg dout_ready_reg;
always @(posedge clock or posedge reset) begin
  if (reset) 
    dout_ready_reg <= 0;
  else
    dout_ready_reg <= dout_ready;
end

///////////////////////
// valid is always 1 but masked by dout_ready_reg
assign dout_valid = dout_ready_reg;


///////////////////////
// State Machine


//reg     out;
reg  [1:0]   pkt_state;
        
localparam STATE_CTRL_PKT_SOP = 0;
localparam STATE_CTRL_PKT_DAT = 1;
localparam STATE_DATA_PKT_SOP = 2;
localparam STATE_DATA_PKT_DAT = 3;



wire ctrl_pkt_sop = (pkt_state == STATE_CTRL_PKT_SOP ) ? 1 : 0 ;
wire ctrl_pkt_eop = ((pkt_state == STATE_CTRL_PKT_DAT) & (dot_cnt==(CTRL_PKT_NUM-1))  ) ? 1 : 0 ; 


wire data_pkt_sop = (pkt_state == STATE_DATA_PKT_SOP ) ? 1 : 0 ;
wire data_pkt_eop = ((pkt_state == STATE_DATA_PKT_DAT) & (dot_cnt==(WIDTH-1)) & (line_cnt==(HEIGHT-1))  ) ? 1 : 0 ;


always @ (posedge clock or posedge reset) begin
  if (reset) pkt_state <= STATE_CTRL_PKT_SOP; 
  else 
    case (pkt_state) // state transitions
        STATE_CTRL_PKT_SOP: if (dout_ready_reg) pkt_state <= STATE_CTRL_PKT_DAT;
        STATE_CTRL_PKT_DAT: if (dout_ready_reg & ctrl_pkt_eop) pkt_state <= STATE_DATA_PKT_SOP;
        STATE_DATA_PKT_SOP: if (dout_ready_reg) pkt_state <= STATE_DATA_PKT_DAT;
        STATE_DATA_PKT_DAT: if (dout_ready_reg & data_pkt_eop) pkt_state <= STATE_CTRL_PKT_SOP;
        default : pkt_state <= STATE_CTRL_PKT_DAT;
    endcase
end

///////////////////////


/////////////////////////
// sop and eop signals
assign dout_sop = (ctrl_pkt_sop | data_pkt_sop) & dout_ready_reg;
assign dout_eop = (ctrl_pkt_eop | data_pkt_eop) & dout_ready_reg;

always @(posedge clock or posedge reset) begin
  if (reset) begin
    dot_cnt <= 0;
  end
  else begin
    if (dout_ready_reg)
      if ((pkt_state == STATE_DATA_PKT_DAT) ) begin
        if ( dot_cnt < (WIDTH-1) )
          dot_cnt <= dot_cnt + 1;
        else
         dot_cnt <= 0;
      end
      else if ((pkt_state == STATE_CTRL_PKT_DAT) )begin // control packet
        if ( dot_cnt < (CTRL_PKT_NUM-1) )
          dot_cnt <= dot_cnt + 1;
        else
          dot_cnt <= 0;
      end
  end
end

always @(posedge clock or posedge reset) begin
  if (reset) begin
    line_cnt <= 0;
  end
  else begin
    if (dout_ready_reg ) begin
      if (pkt_state == STATE_DATA_PKT_DAT) begin
        if ( dot_cnt == (WIDTH-1) )  begin
          if ( line_cnt < (HEIGHT-1) )
            line_cnt <= line_cnt+1;
          else
            line_cnt <= 0;
        end
      end
      else
          line_cnt <= 0;
    end
  end
end


reg [MAX_BPC*3-1:0] ctrl_data;
wire [MAX_BPC-1:0] r_data;
wire [MAX_BPC-1:0] g_data;
wire [MAX_BPC-1:0] b_data;
wire [MAX_BPC*3-1:0] image_data={r_data, g_data, b_data};

///////////////////////
// Making Image Data

// Generate VESA ramp 6bpc
wire r_line_active_18bpp = line_cnt[7:6] == 2'b00;
wire g_line_active_18bpp = line_cnt[7:6] == 2'b01;
wire b_line_active_18bpp = line_cnt[7:6] == 2'b10;
wire w_line_active_18bpp = line_cnt[7:6] == 2'b11;

wire [MAX_BPC-1:0] r_data_18bpp = {{6{r_line_active_18bpp}} & dot_cnt[5:0] | {6{w_line_active_18bpp}} & dot_cnt[5:0], {MAX_BPC-6{1'b0}}};
wire [MAX_BPC-1:0] g_data_18bpp = {{6{g_line_active_18bpp}} & dot_cnt[5:0] | {6{w_line_active_18bpp}} & dot_cnt[5:0], {MAX_BPC-6{1'b0}}};
wire [MAX_BPC-1:0] b_data_18bpp = {{6{b_line_active_18bpp}} & dot_cnt[5:0] | {6{w_line_active_18bpp}} & dot_cnt[5:0], {MAX_BPC-6{1'b0}}};

// Generate VESA ramp 8bpc
wire r_line_active_24bpp = line_cnt[7:6] == 2'b00;
wire g_line_active_24bpp = line_cnt[7:6] == 2'b01;
wire b_line_active_24bpp = line_cnt[7:6] == 2'b10;
wire w_line_active_24bpp = line_cnt[7:6] == 2'b11;

wire [MAX_BPC-1:0] r_data_24bpp = {{8{r_line_active_24bpp}} & dot_cnt[7:0] | {8{w_line_active_24bpp}} & dot_cnt[7:0], {MAX_BPC-8{1'b0}}};
wire [MAX_BPC-1:0] g_data_24bpp = {{8{g_line_active_24bpp}} & dot_cnt[7:0] | {8{w_line_active_24bpp}} & dot_cnt[7:0], {MAX_BPC-8{1'b0}}};
wire [MAX_BPC-1:0] b_data_24bpp = {{8{b_line_active_24bpp}} & dot_cnt[7:0] | {8{w_line_active_24bpp}} & dot_cnt[7:0], {MAX_BPC-8{1'b0}}};

// Combiner
assign r_data = 16'd0 | {16{bpp == 3'b001}} & r_data_24bpp | {16{bpp == 3'b000}} & r_data_18bpp;
assign g_data = 16'd0 | {16{bpp == 3'b001}} & g_data_24bpp | {16{bpp == 3'b000}} & g_data_18bpp;
assign b_data = 16'd0 | {16{bpp == 3'b001}} & b_data_24bpp | {16{bpp == 3'b000}} & b_data_18bpp;

///////////////////////
// Making Final Output Data
reg [MAX_BPC*3-1:0] dout_data;
always @(pkt_state or ctrl_data or image_data ) begin
  case (pkt_state) 
     STATE_CTRL_PKT_SOP: dout_data = CTRL_PKT_HEADER;
     STATE_CTRL_PKT_DAT: dout_data = ctrl_data;
     STATE_DATA_PKT_SOP: dout_data = DATA_PKT_HEADER;
     default:            dout_data = image_data; 
  endcase
end

wire [15:0] w_width = WIDTH;
wire [15:0] w_height = HEIGHT;
always @(dot_cnt[3:0]) begin
  case (dot_cnt[3:0])
    4'd0 : ctrl_data = {{MAX_BPC-4{1'b0}}, w_width[ 7: 4],  {MAX_BPC-4{1'b0}},  w_width[11: 8], {MAX_BPC-4{1'b0}},  w_width[15:12]};
    4'd1 : ctrl_data = {{MAX_BPC-4{1'b0}}, w_height[11: 8], {MAX_BPC-4{1'b0}}, w_height[15:12], {MAX_BPC-4{1'b0}},  w_width[ 3: 0]};
    4'd2 : ctrl_data = {{MAX_BPC-4{1'b0}}, 4'b0011,         {MAX_BPC-4{1'b0}}, w_height[ 3: 0], {MAX_BPC-4{1'b0}},  w_height[ 7: 4]};
    default : ctrl_data = {MAX_BPC*3{1'bx}};
  endcase
end


endmodule

