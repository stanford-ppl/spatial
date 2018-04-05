# (C) 2001-2017 Intel Corporation. All rights reserved.
# Your use of Intel Corporation's design tools, logic functions and other 
# software and tools, and its AMPP partner logic functions, and any output 
# files from any of the foregoing (including device programming or simulation 
# files), and any associated documentation or information are expressly subject 
# to the terms and conditions of the Intel Program License Subscription 
# Agreement, Intel FPGA IP License Agreement, or other applicable 
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
# This file contains the routines to generate the early external memory
# interface timing report at the before the start of the compile flow.
#
# These routines are only meant to be used in this specific context.
# Trying to using them in a different context can have unexpected
# results.
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
#############################################################

# Determin if only doing IO analysis
set ::io_only_analysis 1

#############################################################
# Initialize the environment / Error Checking
#############################################################

if { ![info exists quartus(nameofexecutable)] || $quartus(nameofexecutable) != "quartus_sta" } {
   post_message -type error "This script must be run from quartus_sta"
   return 1
}

# Check the project
if { ! [ is_project_open ] } {
   if { [ llength $quartus(args) ] > 0 } {
      set project_name [lindex $quartus(args) 0]
      project_open -revision [ get_current_revision $project_name ] $project_name
   } else {
      post_message -type error "Missing project_name argument"
      return 1
   }
}


# Load the timing netlist if required
if { ! [timing_netlist_exist] } {
   # In IO only flow, check to see if we could even create a timing nelist
   # First try to see if we could even create a
   catch {create_timing_netlist} create_timing_netlist_out
   set create_timing_netlist_error [regexp "ERROR" $create_timing_netlist_out]
      
   # If create timing netlist cannot run, then the IO flow is a valid flow
   if {$create_timing_netlist_error == 1} {
      create_emif_netlist -revision $::quartus(project)
      sta_create_empty_report   
   } else {
      delete_timing_netlist
      post_message -type error "Early EMIF IO timing estimate cannot be run once the Fitter has been run"
      return 1   
   }

} else {
   post_message -type error "Early EMIF IO timing estimate cannot be run once the Fitter has been run"
   return 1
}

# Load the reports
load_package report
catch {load_report} load_report_out
set report_not_loaded [regexp "ERROR" $load_report_out]
if {$report_not_loaded == 1} {
   create_report_database -type cmp
   create_report_database -type taw
}

#############################################################
# Some useful functions
#############################################################
set script_dir [file dirname [info script]]
source "$script_dir/ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_ip_parameters.tcl"
source "$script_dir/ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_parameters.tcl"
source "$script_dir/ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_pin_map.tcl"
source "$script_dir/ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_report_timing_core.tcl"


if [ info exists ddr_db ] {
   unset ddr_db
}
ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_initialize_ddr_db ddr_db var


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

   #################################################################################
   # Find some design values and parameters that will used during the timing analysis
   # that do not change accross the operating conditions

   set fname ""
   set fbasename ""
   if {[llength $instances] <= 1} {
      set fbasename "${::GLOBAL_ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_corename}"
   } else {
      set fbasename "${::GLOBAL_ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_corename}_${inst_id}"
   }
   
   #################################################################################
   # Now loop the timing analysis over the various operating conditions
   set summary [list]

   set opcname "All conditions"
   set opcname [string trim $opcname]
   
   #######################################
   # Read Analysis

   ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_perform_read_capture_analysis $opcname $inst pins var summary
   
   #######################################
   # Write Analysis

   ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_perform_write_launch_analysis $opcname $inst pins var summary

   #######################################
   # Address/command Analyses

   ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_perform_ac_analysis  $opcname $inst pins var summary

   #######################################
   # DQS Gating Analysis

   ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_perform_dqs_gating_analysis   $opcname $inst pins var summary

   #######################################
   # Write Levelling Analysis 
   
   ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_perform_write_levelling_analysis $opcname $inst pins var summary

   #######################################
   # PHY Analyses
   
   ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_perform_core_analysis $opcname $inst pins var summary

   #######################################
   # Print out the Summary Panel for this instance   

   set summary [lsort -command ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_sort_proc $summary]

   set f -1
   set fname "${fbasename}_io_summary.csv"

   set f [open $fname w]

   puts $f "Core: ${::GLOBAL_ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_corename} - Instance: $inst"
   puts $f "Path, Setup Margin, Hold Margin"

   
   post_message -type info "Core: ${::GLOBAL_ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_corename} - Instance: $inst"
   post_message -type info "                                                         setup  hold"
   set panel_name "$inst"
   set root_folder_name [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_current_timequest_report_folder]
   
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
         set su [ ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_round_3dp $su]
      }
      if {$hold != "--"} {
         set hold [ ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_round_3dp $hold]
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
   
   incr inst_id
}
# end foreach inst


if {$report_not_loaded == 1} {
   save_report_database
   sta_reload_sta_report
}

delete_emiftcl
