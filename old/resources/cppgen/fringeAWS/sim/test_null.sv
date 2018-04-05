// Amazon FPGA Hardware Development Kit
//
// Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
//
// Licensed under the Amazon Software License (the "License"). You may not use
// this file except in compliance with the License. A copy of the License is
// located at
//
//    http://aws.amazon.com/asl/
//
// or in the "license" file accompanying this file. This file is distributed on
// an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, express or
// implied. See the License for the specific language governing permissions and
// limitations under the License.

module test_null();

   import tb_type_defines_pkg::*;
   
   initial begin
      int exit_code;
      
      tb.power_up();
      // tb.power_up(.clk_recipe_a(ClockRecipe::A1), 
                  // .clk_recipe_b(ClockRecipe::B0), 
                  // .clk_recipe_c(ClockRecipe::C0));
      
      // init ddr here
      // Note: can also refactor into c1->load() if not simulation-specific
      // (e.g. flr currently unsupported over PCIe, only in sim)
      
      tb.nsec_delay(1000);
      tb.poke_stat(.addr(8'h0c), .ddr_idx(0), .data(32'h0000_0000));
      tb.poke_stat(.addr(8'h0c), .ddr_idx(1), .data(32'h0000_0000));
      tb.poke_stat(.addr(8'h0c), .ddr_idx(2), .data(32'h0000_0000));

      // de-select the ATG hardware
      tb.poke_ocl(.addr(64'h130), .data(0));
      tb.poke_ocl(.addr(64'h230), .data(0));
      tb.poke_ocl(.addr(64'h330), .data(0));
      tb.poke_ocl(.addr(64'h430), .data(0));
      // allow memory to initialize
      tb.nsec_delay(25000);
      // issuing flr
      tb.issue_flr();

      tb.test_main(exit_code);
      
      #50ns;

      tb.power_down();
      
      $finish;
   end

endmodule // test_null
