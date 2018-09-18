module RetimeShiftRegister
#(
    parameter WIDTH = 1,
    parameter STAGES = 1)
(
    input clock,
    input reset,
    input [WIDTH-1:0] in,
    output reg [WIDTH-1:0] out
);
  integer i;
  reg [WIDTH-1:0] sr[STAGES]; // Create 'STAGES' number of register, each 'WIDTH' bits wide

   /* synopsys dc_tcl_script_begin
    set_ungroup [current_design] true
    set_flatten true -effort high -phase true -design [current_design]
    set_dont_retime [current_design] false
    set_optimize_registers true -design [current_design]
    */
  always @(posedge clock) begin
    if (reset) begin
      for(i=0; i<STAGES; i=i+1) begin
        sr[i] <= {WIDTH{1'b0}};
      end
    end else begin
      sr[0] <= in;
      for(i=1; i<STAGES; i=i+1) begin
        sr[i] <= sr[i-1];
      end
    end
  end

  always @(*) begin
    out <= sr[STAGES-1];
  end
endmodule

module RetimeShiftRegisterBool
#(
    parameter WIDTH = 1,
    parameter STAGES = 1)
(
    input clock,
    input reset,
    input  in,
    output reg out
);
  integer i;
  reg sr[STAGES]; // Create 'STAGES' number of register, each 'WIDTH' bits wide

   /* synopsys dc_tcl_script_begin
    set_ungroup [current_design] true
    set_flatten true -effort high -phase true -design [current_design]
    set_dont_retime [current_design] false
    set_optimize_registers true -design [current_design]
    */
  always @(posedge clock) begin
    if (reset) begin
      for(i=0; i<STAGES; i=i+1) begin
        sr[i] <= {WIDTH{1'b0}};
      end
    end else begin
      sr[0] <= in;
      for(i=1; i<STAGES; i=i+1) begin
        sr[i] <= sr[i-1];
      end
    end
  end

  always @(*) begin
    out <= sr[STAGES-1];
  end
endmodule

