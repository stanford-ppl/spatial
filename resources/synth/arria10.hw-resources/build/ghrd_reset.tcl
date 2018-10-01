# to run this script using quartus_stp
#   quartus_stp -t ghrd_reset.tcl --cable-name <> --device-index <> --cold-reset

package require cmdline

# Command line processing
set parameters {
    {-cable-name.arg   "" "Name of JTAG cable connected to system to reset. Defaults to value of $BOARD_CABLE."}
    {-device-index.arg "" "Device index of the FPGA in JTAG chain. Defaults to value of $BOARD_DEVICE_INDEX."}
    {-cold-reset          "Cold reset the system. This is the default operation."}
    {-warm-reset          "Warm reset the system."}
    {-debug-reset         "Reset the system's debug core."}
}
array set opts [cmdline::getoptions argv $parameters]

if {[string length $opts(-cable-name)] == 0} {
    if {[info exists ::env(BOARD_CABLE)]} {
        array set opts [list -cable-name $::env(BOARD_CABLE)]
    } else {
        puts stderr "Unable to determine board cable to use for reset. Do you have a board resource, or have you specified --cable-name?"
        exit 1
    }
}

if {[string length $opts(-device-index)] == 0} {
    if {[info exists ::env(BOARD_DEVICE_INDEX)]} {
        array set opts [list -device-index $::env(BOARD_DEVICE_INDEX)]
    } else {
        puts stderr "Unable to determine FPGA device index to use for reset. Do you have a board resource, or have you specified --device-index?"
        exit 1
    }
}

if {!$opts(-debug-reset) && !$opts(-warm-reset)} {
    # If no operation specified, default to cold reset.
    array set opts {-cold-reset 1}
}

# Helper functions
proc get_device_name { cable_name device_index } {
    foreach device_name [get_device_names -hardware_name $cable_name] {
        if { [string match "@$device_index:*" $device_name] } {
            return $device_name
        }
    }
}

proc get_reset_source_instance { cable_name device_name } {
    foreach instance [get_insystem_source_probe_instance_info -hardware_name $cable_name -device_name $device_name] {
        if { [string match "RST" [lindex $instance 3]] } {
            return [lindex $instance 0]
        }
    }
}

proc source_write_sequence { cable_name device_name source_instance data_sequence } {
    start_insystem_source_probe -hardware_name $cable_name -device_name $device_name

    foreach value $data_sequence {
        write_source_data -instance_index $source_instance -value $value -value_in_hex
    }

    end_insystem_source_probe
}

# Look up additional parameters
set device_name [get_device_name $opts(-cable-name) $opts(-device-index)]
set source_instance [get_reset_source_instance $opts(-cable-name) $device_name]
if {![info exists device_name] && ![info exists source_instance]} {
    puts stderr "Unable to find device or source index to drive reset. Is the FPGA configured with a design that allows HPS reset?"
    exit 1
}

# Finally, do the reset(s)
if {$opts(-debug-reset)} {
    source_write_sequence $opts(-cable-name) $device_name $source_instance {0x0 0x4}
}

if {$opts(-warm-reset)} {
    source_write_sequence $opts(-cable-name) $device_name $source_instance {0x0 0x2}
}

if {$opts(-cold-reset)} {
    source_write_sequence $opts(-cable-name) $device_name $source_instance {0x0 0x1}
}

exit 0

