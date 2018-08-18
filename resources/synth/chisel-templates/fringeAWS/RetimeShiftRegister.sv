module RetimeShiftRegister
#(
    parameter WIDTH = 1,
    parameter STAGES = 1)
(
    input clock,
    input reset,
    input [WIDTH-1:0] init,
    input [WIDTH-1:0] in,
    input flow,
    output logic [WIDTH-1:0] out
);

  reg [WIDTH-1:0] sr[STAGES]; // Create 'STAGES' number of register, each 'WIDTH' bits wide

   /* synopsys dc_tcl_script_begin
    set_ungroup [current_design] true
    set_flatten true -effort high -phase true -design [current_design]
    set_dont_retime [current_design] false
    set_optimize_registers true -design [current_design]
    */
  always @(posedge clock) begin
    if (reset) begin
      for(int i=0; i<STAGES; i++) begin
        sr[i] <= init;
      end
    end else begin
      if (flow) begin
        sr[0] <= in;
        for(int i=1; i<STAGES; i++) begin
          sr[i] <= sr[i-1];
        end
      end
    end
  end

  always @(*) begin
    out <= sr[STAGES-1];
  end
endmodule

