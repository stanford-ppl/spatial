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


## Create a second project to build the design
open_project ./project_1/project_1.xpr -part $PART

open_run -name synthDesign synth_1
report_timing_summary -file ./REPORT_synth_timing_summary.rpt
report_utilization -packthru -file ./REPORT_synth_utilization.rpt
report_utilization -packthru -hierarchical -hierarchical_depth 20 -hierarchical_percentages -file ./REPORT_synth_utilization_hierarchical.rpt
report_ram_utilization -detail -file ./REPORT_synth_ram_utilization.rpt

# Reports
open_run -name implDesign impl_1
report_timing_summary -file ./REPORT_par_timing_summary.rpt
report_utilization -packthru -file  ./REPORT_par_utilization.rpt
report_utilization -packthru -hierarchical -hierarchical_depth 20 -hierarchical_percentages -file  ./REPORT_par_utilization_hierarchical.rpt
report_ram_utilization -detail -file ./REPORT_par_ram_utilization.rpt

close_project
