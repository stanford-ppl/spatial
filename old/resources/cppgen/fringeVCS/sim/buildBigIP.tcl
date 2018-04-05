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

create_project bigIPSimulation ./bigIPSimulation -part xcvu9p-flgb2104-2-i

source bigIP.tcl

export_ip_user_files -of_objects [get_ips] -no_script -force
export_simulation -simulator vcs -directory ./export_sim -of_objects [get_ips]

close_project
