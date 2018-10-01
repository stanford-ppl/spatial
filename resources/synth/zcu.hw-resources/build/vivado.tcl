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
set RST_FREQ [expr $CLOCK_FREQ_MHZ - 1]

source settings.tcl

## Create a second project to build the design
create_project project_1 ./project_1 -part $PART
set_property board_part $BOARD [current_project]

## Import Verilog generated from Chisel and static Verilog files
add_files -norecurse [glob *.v]
add_files -norecurse [glob *.sv]

## Import PS, reset, AXI protocol conversion and word width conversion IP
#import_ip -files [glob *.xci]

switch $TARGET {
  "ZCU102" {
    import_ip -files [list \
      ./bd_project/bd_project.srcs/sources_1/bd/design_1/ip/design_1_zynq_ultra_ps_e_0_0/design_1_zynq_ultra_ps_e_0_0.xci                  \
      ./bd_project/bd_project.srcs/sources_1/bd/design_1/ip/design_1_rst_ps8_0_${RST_FREQ}M_0/design_1_rst_ps8_0_${RST_FREQ}M_0.xci  \
      ./bd_project/bd_project.srcs/sources_1/bd/design_1/ip/design_1_auto_pc_0/design_1_auto_pc_0.xci                                            \
      ./bd_project/bd_project.srcs/sources_1/bd/design_1/ip/design_1_axi_dwidth_converter_0_0/design_1_axi_dwidth_converter_0_0.xci              \
      ./bd_project/bd_project.srcs/sources_1/bd/design_1/ip/design_1_axi_dwidth_converter_1_0/design_1_axi_dwidth_converter_1_0.xci              \
      ./bd_project/bd_project.srcs/sources_1/bd/design_1/ip/design_1_auto_ds_0/design_1_auto_ds_0.xci
    #  ./bd_project/bd_project.srcs/sources_1/bd/design_1/ip/design_1_axi_data_fifo_0_0/design_1_axi_data_fifo_0_0.xci
    ]
  }
  default {
    import_ip -files [list \
      ./bd_project/bd_project.srcs/sources_1/bd/design_1/ip/design_1_processing_system7_0_0/design_1_processing_system7_0_0.xci                  \
      ./bd_project/bd_project.srcs/sources_1/bd/design_1/ip/design_1_rst_ps7_0_${CLOCK_FREQ_MHZ}M_0/design_1_rst_ps7_0_${CLOCK_FREQ_MHZ}M_0.xci  \
      ./bd_project/bd_project.srcs/sources_1/bd/design_1/ip/design_1_proc_sys_reset_fclk1_0/design_1_proc_sys_reset_fclk1_0.xci                  \
      ./bd_project/bd_project.srcs/sources_1/bd/design_1/ip/design_1_auto_pc_0/design_1_auto_pc_0.xci                                            \
      ./bd_project/bd_project.srcs/sources_1/bd/design_1/ip/design_1_axi_register_slice_0_0/design_1_axi_register_slice_0_0.xci              \
      ./bd_project/bd_project.srcs/sources_1/bd/design_1/ip/design_1_axi_register_slice_1_0/design_1_axi_register_slice_1_0.xci              \
      ./bd_project/bd_project.srcs/sources_1/bd/design_1/ip/design_1_axi_register_slice_2_0/design_1_axi_register_slice_2_0.xci              \
      ./bd_project/bd_project.srcs/sources_1/bd/design_1/ip/design_1_axi_register_slice_3_0/design_1_axi_register_slice_3_0.xci              \
      ./bd_project/bd_project.srcs/sources_1/bd/design_1/ip/design_1_axi_dwidth_converter_0_0/design_1_axi_dwidth_converter_0_0.xci              \
      ./bd_project/bd_project.srcs/sources_1/bd/design_1/ip/design_1_axi_dwidth_converter_1_0/design_1_axi_dwidth_converter_1_0.xci              \
      ./bd_project/bd_project.srcs/sources_1/bd/design_1/ip/design_1_axi_dwidth_converter_2_0/design_1_axi_dwidth_converter_2_0.xci              \
      ./bd_project/bd_project.srcs/sources_1/bd/design_1/ip/design_1_axi_dwidth_converter_3_0/design_1_axi_dwidth_converter_3_0.xci              \
      ./bd_project/bd_project.srcs/sources_1/bd/design_1/ip/design_1_axi_protocol_converter_0_0/design_1_axi_protocol_converter_0_0.xci          \
      ./bd_project/bd_project.srcs/sources_1/bd/design_1/ip/design_1_axi_protocol_converter_1_0/design_1_axi_protocol_converter_1_0.xci          \
      ./bd_project/bd_project.srcs/sources_1/bd/design_1/ip/design_1_axi_protocol_converter_2_0/design_1_axi_protocol_converter_2_0.xci          \
      ./bd_project/bd_project.srcs/sources_1/bd/design_1/ip/design_1_axi_protocol_converter_3_0/design_1_axi_protocol_converter_3_0.xci          \
      ./bd_project/bd_project.srcs/sources_1/bd/design_1/ip/design_1_axi_clock_converter_0_0/design_1_axi_clock_converter_0_0.xci                \
      ./bd_project/bd_project.srcs/sources_1/bd/design_1/ip/design_1_axi_clock_converter_1_0/design_1_axi_clock_converter_1_0.xci                \
      ./bd_project/bd_project.srcs/sources_1/bd/design_1/ip/design_1_axi_clock_converter_2_0/design_1_axi_clock_converter_2_0.xci                \
      ./bd_project/bd_project.srcs/sources_1/bd/design_1/ip/design_1_axi_clock_converter_3_0/design_1_axi_clock_converter_3_0.xci
    #  ./bd_project/bd_project.srcs/sources_1/bd/design_1/ip/design_1_axi_data_fifo_0_0/design_1_axi_data_fifo_0_0.xci
    ]
  }
}

## Create application-specific IP
source bigIP.tcl

update_compile_order -fileset sources_1
set_property top design_1_wrapper [current_fileset]

#set_property STEPS.SYNTH_DESIGN.ARGS.RETIMING true [get_runs synth_1]
set_property STEPS.SYNTH_DESIGN.ARGS.KEEP_EQUIVALENT_REGISTERS true [get_runs synth_1]

launch_runs synth_1 -jobs 6
wait_on_run synth_1

open_run -name implDesign synth_1
report_timing_summary -file ./synth_timing_summary.rpt
report_utilization -packthru -file ./synth_utilization.rpt
report_utilization -packthru -hierarchical -hierarchical_depth 20 -hierarchical_percentages -file ./synth_utilization_hierarchical.rpt
report_ram_utilization -detail -file ./synth_ram_utilization.rpt

launch_runs impl_1 -jobs 6
wait_on_run impl_1
launch_runs impl_1 -to_step write_bitstream -jobs 6
wait_on_run impl_1

# Reports
open_run -name implDesign impl_1
report_timing_summary -file ./par_timing_summary.rpt
report_utilization -packthru -file  ./par_utilization.rpt
report_utilization -packthru -hierarchical -hierarchical_depth 20 -hierarchical_percentages -file  ./par_utilization_hierarchical.rpt
report_ram_utilization -detail -file ./par_ram_utilization.rpt
report_high_fanout_nets -ascending -timing -load_types -file ./par_high_fanout_nets.rpt

#Export bitstream
file copy -force ./project_1/project_1.runs/impl_1/design_1_wrapper.bit ./accel.bit