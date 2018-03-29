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


####################################################################
#
# THIS IS AN AUTO-GENERATED FILE!
# -------------------------------
# If you modify this files, all your changes will be lost if you
# regenerate the core!
#
# FILE DESCRIPTION
# ----------------
# This file contains the routines to generate the UniPHY memory
# interface timing report at the end of the compile flow.
#
# These routines are only meant to be used in this specific context.
# Trying to using them in a different context can have unexpected
# results.
#############################################################
# This report_timing script performs the timing analysis for
# all memory interfaces in the design.  In particular, this
# script will loop over all memory interface cores and
# instances and will timing analyze a range of paths that
# are applicable for each instance.  These include the
# timing analysis for the read capture, write, PHY
# address/command, and resynchronization paths among others.
#
# In performing the above timing analysis, the script
# calls procedures that are found in a separate file (report_timing_core.tcl)
# that has all the details of the timing analysis, and this
# file only serves as the top-level timing analysis flow.
#
# To reduce data lookups in all the procuedures that perform
# the individual timing analysis, data that is needed for
# multiple procedures is lookup up in this file and passed
# to the various parameters.  These data include both values
# that are applicable over all operating conditions, and those
# that are applicable to only one operating condition.
#
# In addition to the data that is looked up, the script
# and the underlying procedures use various other data
# that are stored in TCL sets and include the following:
#
#   t(.)     : Holds the memory timing parameters
#   board(.) : Holds the board skews and propagation delays
#   SSN(.)   : Holds the SSN pushout and pullin delays
#   IP(.)    : Holds the configuration of the memory interface
#              that was generated
#   ISI(.)   : Holds any intersymbol interference when the
#              memory interface is generated in a multirank
#              topology
#   MP(.)    : Holds some process variation data for the memory
#              See below for more information
#   pins(.)  : Holds the pin names for the memory interface
#
#############################################################

set script_dir [file dirname [info script]]

#############################################################
# Memory Specification Process Variation Information
#############################################################

# The percentage of the JEDEC specification that is due
# to process variation 

set MP(DQSQ) 0.65
set MP(QH_time) 0.55
set MP(IS) 0.70
set MP(IH) 0.6
set MP(DS) 0.60
set MP(DH) 0.50
set MP(DSS) 0.60
set MP(DSH) 0.60
set MP(DQSS) 0.5
set MP(WLH) 0.60
set MP(WLS) 0.70
set MP(DQSCK) 0.5
set MP(DQSCK_T) 0.15

#############################################################
# Initialize the environment
#############################################################

global quartus
if { ![info exists quartus(nameofexecutable)] || $quartus(nameofexecutable) != "quartus_sta" } {
	post_message -type error "This script must be run from quartus_sta"
	return 1
}

if { ! [ is_project_open ] } {
	if { [ llength $quartus(args) ] > 0 } {
		set project_name [lindex $quartus(args) 0]
		project_open -revision [ get_current_revision $project_name ] $project_name
	} else {
		post_message -type error "Missing project_name argument"
		return 1
	}
}

#############################################################
# Some useful functions
#############################################################
source "$script_dir/hps_sdram_p0_timing.tcl"
source "$script_dir/hps_sdram_p0_pin_map.tcl"
source "$script_dir/hps_sdram_p0_report_timing_core.tcl"

set family [get_family_string]
set family [string tolower $family]
if {$family == "arria ii gx"} {
	set family "arria ii"
}
if {$family == "stratix iv gx"} {
	set family "stratix iv"
}
if {$family == "stratix v gx"} {
	set family "stratix v"
}
if {$family == "stratix v gt"} {
	set family "stratix v"
}
if {$family == "hardcopy iv gx"} {
	set family "hardcopy iv"
}


#############################################################
# Load the timing netlist
#############################################################

if { ! [ timing_netlist_exist ] } {
	create_timing_netlist
}

set opcs [ list "" ]

set signoff_mode $::quartus(ipc_mode)
if { [string match "*Analyzer GUI" [get_current_timequest_report_folder]]} {
	read_sdc
	update_timing_netlist
	set script_dir [file dirname [info script]]
	source "$script_dir/hps_sdram_p0_timing.tcl"
	source "$script_dir/hps_sdram_p0_pin_map.tcl"
	source "$script_dir/hps_sdram_p0_report_timing_core.tcl"
}

load_package atoms
read_atom_netlist

load_package report
load_report
if { ! [timing_netlist_exist] } {
	post_message -type error "Timing Netlist has not been created. Run the 'Update Timing Netlist' task first."
	return
}

package require ::quartus::ioo
package require ::quartus::sin
initialize_ioo

#############################################################
# This is the main timing analysis function
#   It performs the timing analysis over all of the
#   various Memory Interface instances and timing corners
#############################################################

set mem_if_memtype "ddr3"

if [ info exists ddr_db ] {
	unset ddr_db
}

###############################################
# This is the main call to the netlist traversal routines
# that will automatically find all pins and registers required
# to timing analyze the Core.
hps_sdram_p0_initialize_ddr_db ddr_db

set old_active_clocks [get_active_clocks]
set_active_clocks [all_clocks]

# If multiple instances of this core are present in the
# design they will all be analyzed through the
# following loop
set instances [ array names ddr_db ]
set inst_id 0
foreach inst $instances {
	if { [ info exists pins ] } {
		# Clean-up stale content
		unset pins
	}
	array set pins $ddr_db($inst)

	set inst_controller [regsub {p0$} $inst "c0"]

	####################################################
	#                                                  #
	# Transfer the pin names to a more readable scheme #
	#                                                  #
	####################################################
	set dqs_pins $pins(dqs_pins)
	set dqsn_pins $pins(dqsn_pins)
	set q_groups [ list ]
	foreach dq_group $pins(q_groups) {
		set dq_group $dq_group
		lappend q_groups $dq_group
	}
	set all_dq_pins [ join [ join $q_groups ] ]

	set ck_pins $pins(ck_pins)
	set ckn_pins $pins(ckn_pins)
	set add_pins $pins(add_pins)
	set ba_pins $pins(ba_pins)
	set cmd_pins $pins(cmd_pins)
	set ac_pins [ concat $add_pins $ba_pins $cmd_pins ]
	set dm_pins $pins(dm_pins)
	set all_dq_dm_pins [ concat $all_dq_pins $dm_pins ]


	#################################################################################
	# Find some design values and parameters that will used during the timing analysis
	# that do not change accross the operating conditions
	set period $t(CK)
	
	# Get the number of PLL steps
	set pll_steps "UNDEFINED"
	
	# Package skew
	[catch {get_max_package_skew} max_package_skew]
	if { ($max_package_skew == "") } {
		set max_package_skew 0
	} else {
		set max_package_skew [expr $max_package_skew / 1000.0]
	}
	
	# DLL length
	# Arria V DLL Length is always 8
	set dll_length 8

	# DQS_phase offset
	set dqs_phase [ hps_sdram_p0_get_dqs_phase $dqs_pins ]

	set fitter_run [hps_sdram_p0_get_io_interface_type [lindex [lindex $pins(q_groups) 0] 0]]
	if {$fitter_run == ""} {
		post_message -type critical_warning "Fitter (quartus_fit) failed or was not run. Run the Fitter (quartus_fit) successfully before running ReportDDR"
		continue
	}

	# Get the interface type (HPAD or VPAD)
	set interface_type [hps_sdram_p0_get_io_interface_type $all_dq_pins]

	# Treat the VHPAD interface as the same as a HPAD interface
	if {($interface_type == "VHPAD") || ($interface_type == "HYBRID")} {
		set interface_type "HPAD"
	}
	
	# Get the IO standard which helps us determine the Memory type
	set io_std [hps_sdram_p0_get_io_standard [lindex $dqs_pins 0]]

	if {$interface_type == "" || $interface_type == "UNKNOWN" || $io_std == "" || $io_std == "UNKNOWN"} {
		set result 0
	}

	# Get some of the FPGA jitter and DCD specs
	# When not specified all jitter values are peak-to-peak jitters in ns
	set tJITper [expr [get_micro_node_delay -micro MEM_CK_PERIOD_JITTER -parameters [list IO PHY_SHORT] -period $period]/1000.0]
	set tJITdty [expr [get_micro_node_delay -micro MEM_CK_DC_JITTER -parameters [list IO PHY_SHORT]]/1000.0]
	# DCD value that is looked up is in %, and thus needs to be divided by 100
	set tDCD [expr [get_micro_node_delay -micro MEM_CK_DCD -parameters [list IO PHY_SHORT]]/100.0]
	# This is the peak-to-peak jitter on the whole DQ-DQS read capture path
	set DQSpathjitter [expr [get_micro_node_delay -micro DQDQS_JITTER -parameters [list IO] -in_fitter]/1000.0]
	# This is the proportion of the DQ-DQS read capture path jitter that applies to setup (looed up value is in %, and thus needs to be divided by 100)
	set DQSpathjitter_setup_prop [expr [get_micro_node_delay -micro DQDQS_JITTER_DIVISION -parameters [list IO] -in_fitter]/100.0]
	
	set fname ""
	set fbasename ""
	if {[llength $instances] <= 1} {
		set fbasename "${::GLOBAL_hps_sdram_p0_corename}"
	} else {
		set fbasename "${::GLOBAL_hps_sdram_p0_corename}_${inst_id}"
	}
	
	set fname "${fbasename}_summary.csv"
	
	#################################################################################
	# Now loop the timing analysis over the various operating conditions
	set summary [list]
	foreach opc $opcs {
		if {$opc != "" } {
			set_operating_conditions $opc
			update_timing_netlist
		}
		set opcname [get_operating_conditions_info [get_operating_conditions] -display_name]
		set opcname [string trim $opcname]
		
		set model_corner [hps_sdram_p0_get_model_corner]
		initialize_sin -model [lindex $model_corner 0] -corner [lindex $model_corner 1]
		
		global assumptions_cache
		set in_gui [regexp "TimeQuest Timing Analyzer GUI" [get_current_timequest_report_folder]]
		if {!$in_gui && [array exists assumptions_cache] &&  [info exists assumptions_cache(${::GLOBAL_hps_sdram_p0_corename}-$inst)] } {
			set assumptions_valid $assumptions_cache(${::GLOBAL_hps_sdram_p0_corename}-$inst)
			if {!$assumptions_valid} {
				post_message -type critical_warning "Read Capture and Write timing analyses may not be valid due to violated timing model assumptions"
				post_message -type critical_warning "See violated timing model assumptions in previous timing analysis above"
			}
		} else {
			set assumptions_valid [hps_sdram_p0_verify_flexible_timing_assumptions $inst pins $mem_if_memtype]
			set assumptions_cache(${::GLOBAL_hps_sdram_p0_corename}-$inst) $assumptions_valid
		}	

		#######################################
		# Determine parameters and values that are valid only for this operating condition

		set total_max_scale_factor [get_float_table_node_delay -src {SCALE_FACTOR} -dst {TOTAL_SCALE_FACTOR} -parameters {IO}]
		set total_min_scale_factor [get_float_table_node_delay -src {SCALE_FACTOR} -dst {TOTAL_SCALE_FACTOR} -parameters {IO MIN}]
		set scale_factors(total) [expr $total_max_scale_factor - $total_min_scale_factor]

		set odv_max_scale_factor [get_float_table_node_delay -src {SCALE_FACTOR} -dst {ODV_SCALE_FACTOR} -parameters {IO}]
		set odv_min_scale_factor [get_float_table_node_delay -src {SCALE_FACTOR} -dst {ODV_SCALE_FACTOR} -parameters {IO MIN}]
		set scale_factors(odv) [expr $odv_max_scale_factor - $odv_min_scale_factor]

		set eol_max_scale_factor [get_float_table_node_delay -src {SCALE_FACTOR} -dst {EOL_SCALE_FACTOR} -parameters {IO}]
		set eol_min_scale_factor [get_float_table_node_delay -src {SCALE_FACTOR} -dst {EOL_SCALE_FACTOR} -parameters {IO MIN}]
		set scale_factors(eol) [expr $eol_max_scale_factor - $eol_min_scale_factor]

		set emif_max_scale_factor [get_float_table_node_delay -src {SCALE_FACTOR} -dst {MEM_INTERFACE_SCALE_FACTOR} -parameters {IO}]
		set emif_min_scale_factor [get_float_table_node_delay -src {SCALE_FACTOR} -dst {MEM_INTERFACE_SCALE_FACTOR} -parameters {IO MIN}]
		set scale_factors(emif) [expr $emif_max_scale_factor - $emif_min_scale_factor]

		#######################################
		# Write Analysis

		hps_sdram_p0_perform_flexible_write_launch_timing_analysis $opcs $opcname $inst $family scale_factors $interface_type $max_package_skew $dll_length $period pins t summary MP IP board

		#######################################
		# Read Analysis

		hps_sdram_p0_perform_flexible_read_capture_timing_analysis $opcs $opcname $inst $family scale_factors $io_std $interface_type $max_package_skew $dqs_phase $period $all_dq_pins pins t summary MP IP board fpga

		#######################################
		# PHY and Address/command Analyses

		hps_sdram_p0_perform_ac_analyses  $opcs $opcname $inst scale_factors pins t summary IP		
		hps_sdram_p0_perform_phy_analyses $opcs $opcname $inst $inst_controller pins t summary IP


		#######################################
		# Bus Turnaround Time Analysis
		hps_sdram_p0_perform_flexible_bus_turnaround_time_analysis $opcs $opcname $inst $family $period $dll_length $interface_type $tJITper $tJITdty $tDCD $pll_steps pins t summary MP IP SSN board ISI


		#######################################
		# Postamble analysis
		hps_sdram_p0_perform_flexible_postamble_timing_analysis $opcs $opcname $inst scale_factors $family $period $dll_length $interface_type $tJITper $tJITdty $tDCD $DQSpathjitter pins t summary MP IP SSN board ISI

	}

	#################################################
	# Now perform analysis of some of the calibrated paths that consider
	# Worst-case conditions	

	set opcname "All Conditions"

	#######################################
	# Print out the Summary Panel for this instance	

	set summary [lsort -command hps_sdram_p0_sort_proc $summary]

	set f -1
	if { [hps_sdram_p0_get_operating_conditions_number] == 0 } {
		set f [open $fname w]

		puts $f "Core: ${::GLOBAL_hps_sdram_p0_corename} - Instance: $inst"
		puts $f "Path, Setup Margin, Hold Margin"
	} else {
		set f [open $fname a]
	}

	


	post_message -type info "Core: ${::GLOBAL_hps_sdram_p0_corename} - Instance: $inst"
	post_message -type info "                                                         setup  hold"
	set panel_name "$inst"
	set root_folder_name [get_current_timequest_report_folder]
	if { ! [string match "${root_folder_name}*" $panel_name] } {
		set panel_name "${root_folder_name}||$panel_name"
	}
	# Create the root if it doesn't yet exist
	if {[get_report_panel_id $root_folder_name] == -1} {
		set panel_id [create_report_panel -folder $root_folder_name]
	}
	# Delete any pre-existing summary panel
	set panel_id [get_report_panel_id $panel_name]
	if {$panel_id != -1} {
		delete_report_panel -id $panel_id
	}

	# Create summary panel
	set total_failures 0
	set rows [list]
	lappend rows "add_row_to_table -id \$panel_id \[list \"Path\" \"Operating Condition\" \"Setup Slack\" \"Hold Slack\"\]"
	foreach summary_line $summary {
		foreach {corner order path su hold num_su num_hold} $summary_line { }
		if {($num_su == 0) || ([string trim $su] == "")} {
			set su "--"
		}
		if {($num_hold == 0) || ([string trim $hold] == "")} {
			set hold "--"
		}


		if { ($su != "--" && $su < 0) || ($hold != "--" && $hold < 0) } {
			incr total_failures
			set type warning
			set offset 50
		} else {
			set type info
			set offset 53
		}
		if {$su != "--"} {
			set su [ hps_sdram_p0_round_3dp $su]
		}
		if {$hold != "--"} {
			set hold [ hps_sdram_p0_round_3dp $hold]
		}
		post_message -type $type [format "%-${offset}s | %6s %6s" $path $su $hold]
		puts $f [format "\"%s\",%s,%s" $path $su $hold]
		set fg_colours [list black black]
		if { $su != "--" && $su < 0 } {
			lappend fg_colours red
		} else {
			lappend fg_colours black
		}

		if { $hold != "" && $hold < 0 } {
			lappend fg_colours red
		} else {
			lappend fg_colours black
		}
		lappend rows "add_row_to_table -id \$panel_id -fcolors \"$fg_colours\" \[list \"$path\" \"$corner\" \"$su\" \"$hold\"\]"
	}
	close $f
	if {$total_failures > 0} {
		post_message -type critical_warning "DDR Timing requirements not met"
		set panel_id [create_report_panel -table $panel_name -color red]
	} else {
		set panel_id [create_report_panel -table $panel_name]
	}
	foreach row $rows {
		eval $row
	}
	
	write_timing_report


	incr inst_id
}

set_active_clocks $old_active_clocks
uninitialize_sin
uninitialize_ioo
