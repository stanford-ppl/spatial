if { $argc != 1 } {
  puts $argc
  puts [llength $argv]
  foreach i $argv {puts $i}
  puts "The second arg is [lindex $argv 1]"; #indexes start at 0
  puts "Usage: settings.tcl <clockFreqMHz>"
  exit -1
}

set CLOCK_FREQ_MHZ [lindex $argv 0]
set CLOCK_FREQ_HZ  [expr $CLOCK_FREQ_MHZ * 1000000]

source settings.tcl
set ver [version -short]

# Create project to make processing system IP from bd
create_project bd_project ./bd_project -part $PART
set_property board_part $BOARD [current_project]

add_files -norecurse [glob *.v]
add_files -norecurse [glob *.sv]
update_compile_order -fileset sources_1
update_compile_order -fileset sim_1

## Create processing system using bd
create_bd_design "design_1"
update_compile_order -fileset sources_1

create_bd_cell -type module -reference SpatialIP SpatialIP_0
create_bd_cell -type ip -vlnv xilinx.com:ip:clk_wiz:6.0 clk_wiz_0
apply_bd_automation -rule xilinx.com:bd_rule:board -config { Board_Interface {reset ( FPGA Reset ) } Manual_Source {New External Port (ACTIVE_HIGH)}}  [get_bd_pins clk_wiz_0/reset]
apply_bd_automation -rule xilinx.com:bd_rule:board -config { Clk {New External Port (100 MHz)} Manual_Source {Auto}}  [get_bd_pins clk_wiz_0/clk_in1]
connect_bd_net [get_bd_pins clk_wiz_0/clk_out1] [get_bd_pins SpatialIP_0/clock]
connect_bd_net [get_bd_ports reset] [get_bd_pins SpatialIP_0/reset]


validate_bd_design
save_bd_design

source bigIP.tcl

#synth_design -mode out_of_context -top SpatialIP_v1_0
launch_runs synth_1 -jobs 4
wait_on_run synth_1
open_run -name implDesign synth_1
report_timing_summary -file ./synth_timing_summary.rpt
report_utilization -packthru -file ./synth_utilization.rpt
report_utilization -packthru -hierarchical -hierarchical_depth 20 -hierarchical_percentages -file ./synth_utilization_hierarchical.rpt
report_ram_utilization -detail -file ./synth_ram_utilization.rpt

