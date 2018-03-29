# (C) 2001-2016 Intel Corporation. All rights reserved.
# Your use of Intel Corporation's design tools, logic functions and other 
# software and tools, and its AMPP partner logic functions, and any output 
# files any of the foregoing (including device programming or simulation 
# files), and any associated documentation or information are expressly subject 
# to the terms and conditions of the Intel Program License Subscription 
# Agreement, Intel MegaCore Function License Agreement, or other applicable 
# license agreement, including, without limitation, that your use is for the 
# sole purpose of programming logic devices manufactured by Intel and sold by 
# Intel or its authorized distributors.  Please refer to the applicable 
# agreement for further details.


#####################################################################
#
# THIS IS AN AUTO-GENERATED FILE!
# -------------------------------
# If you modify this files, all your changes will be lost if you
# regenerate the core!
#
# FILE DESCRIPTION
# ----------------
# This file contains a simple script to automatically apply
# IO standards and other IO assignments for the UniPHY memory
# interface pins that connect to the memory device. The pins
# are automatically detected using the routines defined in
# the hps_sdram_p0_pin_map.tcl script.
# All the memory interface parameters are defined in the
# hps_sdram_p0_parameters.tcl script


set available_options {
	{ c.arg "#_ignore_#" "Option to specify the revision name" }
}
package require cmdline

set script_dir [file dirname [info script]]

global ::GLOBAL_hps_sdram_p0_corename
global ::GLOBAL_hps_sdram_p0_io_standard
global ::GLOBAL_hps_sdram_p0_io_standard_differential
global ::GLOBAL_hps_sdram_p0_dqs_group_size
global ::GLOBAL_hps_sdram_p0_number_of_dqs_groups
global ::GLOBAL_hps_sdram_p0_uniphy_temp_ver_code

#################
#               #
# SETUP SECTION #
#               #
#################

global options
set argument_list $quartus(args)
set argv0 "quartus_sta -t [info script]"
set usage "\[<options>\] <project_name>:"
	
if [catch {array set options [cmdline::getoptions argument_list $::available_options]} result] {
	if {[llength $argument_list] > 0 } {
		post_message -type error "Illegal Options"
		post_message -type error  [::cmdline::usage $::available_options $usage]
		qexit -error
	} else {
		post_message -type info  "Usage:"
		post_message -type info  [::cmdline::usage $::available_options $usage]
		qexit -success
	}
}
if {$options(c) != "#_ignore_#"} {
	if [string compare [file extension $options(c)] ""] {
		set options(c) [file rootname $options(c)]
	}
}

if {[llength $argument_list] == 1 } {
	set options(project_name) [lindex $argument_list 0]

	if [string compare [file extension $options(project_name)] ""] {
		set project_name [file rootname $options(project_name)]
	}

	set project_name [file normalize $options(project_name)]

} elseif { [llength $argument_list] == 2 } {
	set options(project_name) [lindex $argument_list 0]
	set options(rev)          [lindex $argument_list 1]

	if [string compare [file extension $options(project_name)] ""] {
		set project_name [file rootname $options(project_name)]
	}
	if [string compare [file extension $options(c)] ""] {
		set revision_name [file rootname $options(c)]
	}

	set project_name [file normalize $options(project_name)]
	set revision_name [file normalize $options(rev)]

} elseif { [ is_project_open ] } {
	set project_name $::quartus(project)
	set options(rev) $::quartus(settings)

} else {
	post_message -type error "Project name is missing"
	post_message -type info [::cmdline::usage $::available_options $usage]
	post_message -type info "For more details, use \"quartus_sta --help\""
	qexit -error
}


 # If this script is called from outside quartus_sta/map, it will re-launch itself in quartus_sta
if { ![info exists quartus(nameofexecutable)] || ($quartus(nameofexecutable) != "quartus_sta" && $quartus(nameofexecutable) != "quartus_map") } {
	post_message -type info "Restarting in quartus_sta..."

	set cmd quartus_sta
	if { [info exists quartus(binpath)] } {
		set cmd [file join $quartus(binpath) $cmd]
	}

	if { [ is_project_open ] } {
		set project_name [ get_current_revision ]
	} elseif { ! [ string compare $project_name "" ] } {
		post_message -type error "Missing project_name argument"

		return 1
	}

	set output [ exec $cmd -t [ info script ] $project_name ]

	foreach line [split $output \n] {
		set type info
		set matched_line [ regexp {^\W*(Info|Extra Info|Warning|Critical Warning|Error): (.*)$} $line x type msg ]
		regsub " " $type _ type

		if { $matched_line } {
			post_message -type $type $msg
		} else {
			puts "$line"
		}
	}

	return 0
}

if { ! [ is_project_open ] } {
	if { ! [ string compare $project_name "" ] } {
		post_message -type error "Missing project_name argument"

		return 1
	}

	if {$options(c) == "#_ignore_#"} {
		project_open $project_name
	} else {
		project_open $project_name -revision $options(c)
	}

}

source "$script_dir/hps_sdram_p0_parameters.tcl"
source "$script_dir/hps_sdram_p0_pin_map.tcl"

set family_name [string tolower [regsub -all " +" [get_global_assignment -name FAMILY] ""]]


##############################
# Clean up stale assignments #
##############################
post_message -type info "Cleaning up stale assignments..."

set asgn_types [ list IO_STANDARD INPUT_TERMINATION OUTPUT_TERMINATION CURRENT_STRENGTH_NEW DQ_GROUP TERMINATION_CONTROL_BLOCK ]
foreach asgn_type $asgn_types {
	remove_all_instance_assignments -tag __$::GLOBAL_hps_sdram_p0_corename -name $asgn_type
}

if { ! [ timing_netlist_exist ] } {
	create_timing_netlist -post_map
}

#######################
#                     #
# ASSIGNMENTS SECTION #
#                     #
#######################

# This is the main call to the netlist traversal routines
# that will automatically find all pins and registers required
# to apply pin settings.
hps_sdram_p0_initialize_ddr_db ddr_db

# If multiple instances of this core are present in the
# design they will all be constrained through the
# following loop

set instances [ array names ddr_db ]
foreach inst $instances {
	if { [ info exists pins ] } {
		# Clean-up stale content
		unset pins
	}
	array set pins $ddr_db($inst)


	hps_sdram_p0_get_rzq_pins $inst all_rzq_pins
	# Set rzq pin I/O standard
	foreach rzq_pin $all_rzq_pins {
				if { ! [ string compare $::GLOBAL_hps_sdram_p0_io_standard "SSTL-135" ] } {
					set_instance_assignment -name IO_STANDARD "$::GLOBAL_hps_sdram_p0_io_standard" -to $rzq_pin -tag __$::GLOBAL_hps_sdram_p0_corename
				} else {
					set_instance_assignment -name IO_STANDARD "$::GLOBAL_hps_sdram_p0_io_standard CLASS I" -to $rzq_pin -tag __$::GLOBAL_hps_sdram_p0_corename
				}
	}
	
  # 1.35V DDR3L pin assignments
  if { ! [ string compare $::GLOBAL_hps_sdram_p0_io_standard "SSTL-135" ] } {
    foreach dq_pin $pins(all_dq_pins) {
      set_instance_assignment -name IO_STANDARD "$::GLOBAL_hps_sdram_p0_io_standard" -to $dq_pin -tag __$::GLOBAL_hps_sdram_p0_corename
      set_instance_assignment -name INPUT_TERMINATION "PARALLEL 40 OHM WITH CALIBRATION" -to $dq_pin -tag __$::GLOBAL_hps_sdram_p0_corename
      set_instance_assignment -name OUTPUT_TERMINATION "SERIES 40 OHM WITH CALIBRATION" -to $dq_pin -tag __$::GLOBAL_hps_sdram_p0_corename
    }

    foreach dqs_pin [ concat $pins(dqs_pins) $pins(dqsn_pins) ] {
      set_instance_assignment -name IO_STANDARD "DIFFERENTIAL $::GLOBAL_hps_sdram_p0_io_standard_differential" -to $dqs_pin -tag __$::GLOBAL_hps_sdram_p0_corename
      set_instance_assignment -name INPUT_TERMINATION "PARALLEL 40 OHM WITH CALIBRATION" -to $dqs_pin -tag __$::GLOBAL_hps_sdram_p0_corename
      set_instance_assignment -name OUTPUT_TERMINATION "SERIES 40 OHM WITH CALIBRATION" -to $dqs_pin -tag __$::GLOBAL_hps_sdram_p0_corename
    }

    foreach ck_pin [ concat $pins(ck_pins) $pins(ckn_pins) ] {
      set_instance_assignment -name IO_STANDARD "DIFFERENTIAL $::GLOBAL_hps_sdram_p0_io_standard_differential" -to $ck_pin -tag __$::GLOBAL_hps_sdram_p0_corename
      set_instance_assignment -name OUTPUT_TERMINATION "SERIES 40 OHM WITHOUT CALIBRATION" -to $ck_pin -tag __$::GLOBAL_hps_sdram_p0_corename

      set_instance_assignment -name D5_DELAY 2 -to $ck_pin -tag __$::GLOBAL_hps_sdram_p0_corename
    }

    foreach ac_pin $pins(ac_wo_reset_pins) {
      set_instance_assignment -name IO_STANDARD "$::GLOBAL_hps_sdram_p0_io_standard" -to $ac_pin -tag __$::GLOBAL_hps_sdram_p0_corename
      set_instance_assignment -name OUTPUT_TERMINATION "SERIES 40 OHM WITHOUT CALIBRATION" -to $ac_pin -tag __$::GLOBAL_hps_sdram_p0_corename
    }
    
    foreach reset_pin $pins(reset_pins) {
      set_instance_assignment -name IO_STANDARD "$::GLOBAL_hps_sdram_p0_io_standard" -to $reset_pin -tag __$::GLOBAL_hps_sdram_p0_corename
      set_instance_assignment -name BOARD_MODEL_FAR_PULLUP_R OPEN -to $reset_pin -tag __$::GLOBAL_hps_sdram_p0_corename
      set_instance_assignment -name BOARD_MODEL_NEAR_PULLUP_R OPEN -to $reset_pin -tag __$::GLOBAL_hps_sdram_p0_corename
      set_instance_assignment -name BOARD_MODEL_FAR_PULLDOWN_R OPEN -to $reset_pin -tag __$::GLOBAL_hps_sdram_p0_corename
      set_instance_assignment -name BOARD_MODEL_NEAR_PULLDOWN_R OPEN -to $reset_pin -tag __$::GLOBAL_hps_sdram_p0_corename
      set_instance_assignment -name OUTPUT_TERMINATION "SERIES 40 OHM WITHOUT CALIBRATION" -to $reset_pin -tag __$::GLOBAL_hps_sdram_p0_corename
    } 

    foreach dm_pin $pins(dm_pins) {
      set_instance_assignment -name IO_STANDARD "$::GLOBAL_hps_sdram_p0_io_standard" -to $dm_pin -tag __$::GLOBAL_hps_sdram_p0_corename
      set_instance_assignment -name OUTPUT_TERMINATION "SERIES 40 OHM WITH CALIBRATION" -to $dm_pin -tag __$::GLOBAL_hps_sdram_p0_corename
    }
  } else {
    # 1.5V DDR3 pin assignments

    foreach dq_pin $pins(all_dq_pins) {
      set_instance_assignment -name IO_STANDARD "$::GLOBAL_hps_sdram_p0_io_standard CLASS I" -to $dq_pin -tag __$::GLOBAL_hps_sdram_p0_corename
      set_instance_assignment -name INPUT_TERMINATION "PARALLEL 50 OHM WITH CALIBRATION" -to $dq_pin -tag __$::GLOBAL_hps_sdram_p0_corename
      set_instance_assignment -name OUTPUT_TERMINATION "SERIES 50 OHM WITH CALIBRATION" -to $dq_pin -tag __$::GLOBAL_hps_sdram_p0_corename
    }

    foreach dqs_pin [ concat $pins(dqs_pins) $pins(dqsn_pins) ] {
      set_instance_assignment -name IO_STANDARD "DIFFERENTIAL $::GLOBAL_hps_sdram_p0_io_standard_differential CLASS I" -to $dqs_pin -tag __$::GLOBAL_hps_sdram_p0_corename
      set_instance_assignment -name INPUT_TERMINATION "PARALLEL 50 OHM WITH CALIBRATION" -to $dqs_pin -tag __$::GLOBAL_hps_sdram_p0_corename
      set_instance_assignment -name OUTPUT_TERMINATION "SERIES 50 OHM WITH CALIBRATION" -to $dqs_pin -tag __$::GLOBAL_hps_sdram_p0_corename
    }

    foreach ck_pin [ concat $pins(ck_pins) $pins(ckn_pins) ] {
      set_instance_assignment -name IO_STANDARD "DIFFERENTIAL $::GLOBAL_hps_sdram_p0_io_standard_differential CLASS I" -to $ck_pin -tag __$::GLOBAL_hps_sdram_p0_corename
      set_instance_assignment -name OUTPUT_TERMINATION "SERIES 50 OHM WITHOUT CALIBRATION" -to $ck_pin -tag __$::GLOBAL_hps_sdram_p0_corename

      set_instance_assignment -name D5_DELAY 2 -to $ck_pin -tag __$::GLOBAL_hps_sdram_p0_corename
    }

    foreach ac_pin $pins(ac_wo_reset_pins) {
      set_instance_assignment -name IO_STANDARD "$::GLOBAL_hps_sdram_p0_io_standard CLASS I" -to $ac_pin -tag __$::GLOBAL_hps_sdram_p0_corename
      set_instance_assignment -name CURRENT_STRENGTH_NEW "MAXIMUM CURRENT" -to $ac_pin -tag __$::GLOBAL_hps_sdram_p0_corename
    }
    
    foreach reset_pin $pins(reset_pins) {
      set_instance_assignment -name IO_STANDARD "$::GLOBAL_hps_sdram_p0_io_standard CLASS I" -to $reset_pin -tag __$::GLOBAL_hps_sdram_p0_corename
      set_instance_assignment -name CURRENT_STRENGTH_NEW "MAXIMUM CURRENT" -to $reset_pin -tag __$::GLOBAL_hps_sdram_p0_corename
    }
    

    foreach dm_pin $pins(dm_pins) {
      set_instance_assignment -name IO_STANDARD "$::GLOBAL_hps_sdram_p0_io_standard CLASS I" -to $dm_pin -tag __$::GLOBAL_hps_sdram_p0_corename
      set_instance_assignment -name OUTPUT_TERMINATION "SERIES 50 OHM WITH CALIBRATION" -to $dm_pin -tag __$::GLOBAL_hps_sdram_p0_corename
    }
  }


	set ::GLOBAL_hps_sdram_p0_dqs_group_size_constraint $::GLOBAL_hps_sdram_p0_dqs_group_size
	if { $::GLOBAL_hps_sdram_p0_dqs_group_size == 8 } {
		set ::GLOBAL_hps_sdram_p0_dqs_group_size_constraint 9
	}
	
	set delay_chain_config FLEXIBLE_TIMING

	# Disable package skew compensation for data pins in timing analysis
	foreach dq_pin $pins(all_dq_pins) {
		set_instance_assignment -name PACKAGE_SKEW_COMPENSATION OFF -to $dq_pin -tag __$::GLOBAL_hps_sdram_p0_corename
	}
	foreach dm_pin $pins(dm_pins) {
		set_instance_assignment -name PACKAGE_SKEW_COMPENSATION OFF -to $dm_pin -tag __$::GLOBAL_hps_sdram_p0_corename
	}
	foreach dqs_pin [ concat $pins(dqs_pins) $pins(dqsn_pins) ] {
		set_instance_assignment -name PACKAGE_SKEW_COMPENSATION OFF -to $dqs_pin -tag __$::GLOBAL_hps_sdram_p0_corename
	}

	# Disable package skew compensation for address/command pins in timing analysis
	foreach ac_pin $pins(ac_pins) {
		set_instance_assignment -name PACKAGE_SKEW_COMPENSATION OFF -to $ac_pin -tag __$::GLOBAL_hps_sdram_p0_corename
	}
	foreach ck_pin [ concat $pins(ck_pins) $pins(ckn_pins) ] {
		set_instance_assignment -name PACKAGE_SKEW_COMPENSATION OFF -to $ck_pin -tag __$::GLOBAL_hps_sdram_p0_corename
	}


	set_instance_assignment -name GLOBAL_SIGNAL OFF -to "${inst}|p0|umemphy|ureset|phy_reset_mem_stable_n" -tag __$::GLOBAL_hps_sdram_p0_corename
	set_instance_assignment -name GLOBAL_SIGNAL OFF -to "${inst}|p0|umemphy|ureset|phy_reset_n" -tag __$::GLOBAL_hps_sdram_p0_corename
	
	for {set i 0} {$i < $::GLOBAL_hps_sdram_p0_number_of_dqs_groups} {incr i 1} {
		set_instance_assignment -name GLOBAL_SIGNAL OFF -to "${inst}|p0|umemphy|uio_pads|dq_ddio[$i].read_capture_clk_buffer" -tag __$::GLOBAL_hps_sdram_p0_corename
		set_instance_assignment -name GLOBAL_SIGNAL OFF -to "${inst}|p0|umemphy|uread_datapath|reset_n_fifo_write_side[$i]" -tag __$::GLOBAL_hps_sdram_p0_corename
		set_instance_assignment -name GLOBAL_SIGNAL OFF -to "${inst}|p0|umemphy|uread_datapath|reset_n_fifo_wraddress[$i]" -tag __$::GLOBAL_hps_sdram_p0_corename
	}

	set_instance_assignment -name ENABLE_BENEFICIAL_SKEW_OPTIMIZATION_FOR_NON_GLOBAL_CLOCKS ON -to $inst -tag __$::GLOBAL_hps_sdram_p0_corename
	
	# Use direct compensation mode to minimize jitter
	set_instance_assignment -name PLL_COMPENSATION_MODE DIRECT -to "${inst}|pll0|fbout" -tag __$::GLOBAL_hps_sdram_p0_corename
}

hps_sdram_p0_dump_all_pins ddr_db

if { [ llength $quartus(args) ] > 1 } {
	set param [lindex $quartus(args) 1]

	if { [ string match -dump_static_pin_map $param ] } {
		set filename "${::GLOBAL_hps_sdram_p0_corename}_static_pin_map.tcl"

		hps_sdram_p0_dump_static_pin_map ddr_db $filename
	}
}

set_global_assignment -name USE_DLL_FREQUENCY_FOR_DQS_DELAY_CHAIN ON
set_global_assignment -name UNIPHY_SEQUENCER_DQS_CONFIG_ENABLE ON
set_global_assignment -name OPTIMIZE_MULTI_CORNER_TIMING ON

set_global_assignment -name ECO_REGENERATE_REPORT ON
