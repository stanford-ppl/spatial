# This script construct diffin qsys component for GHRD with Display Port enabled
# command to run: create the qsys file: qsys-script --script=./ip/diffin/construct_diffin.tcl to create the qsys file, to generate qsys library: qsys-generate --synthesis=VERILOG diffin.qsys --output-directory=./../diffin/

# source ./../../design_config.tcl
set DEVICE_FAMILY "Arria 10"
set FPGA_DEVICE 10AS066N3F40E2SG

if { ![ info exists devicefamily ] } {
  set devicefamily $DEVICE_FAMILY
} else {
  puts "-- Accepted parameter \$devicefamily = $devicefamily"
}
    
if { ![ info exists device ] } {
  set device $FPGA_DEVICE
} else {
  puts "-- Accepted parameter \$device = $device"
}

package require -exact qsys 14.1

create_system {diffin}

set_project_property DEVICE_FAMILY $devicefamily
set_project_property DEVICE $device

# Instances and instance parameters
# (disabled instances are intentionally culled)
add_instance diffin altera_gpio
set_instance_parameter_value diffin {PIN_TYPE_GUI} {Input}
set_instance_parameter_value diffin {SIZE} {1}
set_instance_parameter_value diffin {gui_enable_migratable_port_names} {1}
set_instance_parameter_value diffin {gui_diff_buff} {1}
set_instance_parameter_value diffin {gui_pseudo_diff} {0}
set_instance_parameter_value diffin {gui_bus_hold} {0}
set_instance_parameter_value diffin {gui_open_drain} {0}
set_instance_parameter_value diffin {gui_use_oe} {0}
set_instance_parameter_value diffin {gui_enable_termination_ports} {0}
set_instance_parameter_value diffin {gui_io_reg_mode} {none}
set_instance_parameter_value diffin {gui_sreset_mode} {None}
set_instance_parameter_value diffin {gui_areset_mode} {None}
set_instance_parameter_value diffin {gui_enable_cke} {0}
set_instance_parameter_value diffin {gui_hr_logic} {0}
set_instance_parameter_value diffin {gui_separate_io_clks} {0}
set_instance_parameter_value diffin {EXT_DRIVER_PARAM} {0}
set_instance_parameter_value diffin {GENERATE_SDC_FILE} {0}
set_instance_parameter_value diffin {IP_MIGRATE_PORT_MAP_FILE} {altiobuf_in_port_map.csv}

# exported interfaces
set_instance_property diffin AUTO_EXPORT {true}

# interconnect requirements
set_interconnect_requirement {$system} {qsys_mm.clockCrossingAdapter} {HANDSHAKE}
set_interconnect_requirement {$system} {qsys_mm.maxAdditionalLatency} {1}
set_interconnect_requirement {$system} {qsys_mm.enableEccProtection} {FALSE}
set_interconnect_requirement {$system} {qsys_mm.insertDefaultSlave} {FALSE}

save_system {diffin.qsys}
