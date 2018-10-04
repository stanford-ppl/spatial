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


#############################################################
# Write Timing Analysis
#############################################################
proc hps_sdram_p0_perform_flexible_write_launch_timing_analysis {opcs opcname inst family scale_factors_name interface_type max_package_skew dll_length period pin_array_name timing_parameters_array_name summary_name MP_name IP_name board_name} {

	###############################################################################
	# This timing analysis covers the write timing constraints.  It includes support 
	# for uncalibrated and calibrated write paths.  The analysis starts by running a 
	# conventional timing analysis for the write paths and then adds support for 
	# topologies and IP options which are unique to source-synchronous data transfers.  
	# The support for further topologies includes common clock paths in DDR3 as well as 
	# correlation between D and K.  The support for further IP includes support for 
	# write-deskew calibration.
	# 
	# During write deskew calibration, the IP will adjust delay chain settings along 
	# each signal path to reduce the skew between D pins and to centre align the K 
	# clock within the DVW.  This operation has the benefit of increasing margin on the 
	# setup and hold, as well as removing some of the unknown process variation on each 
	# signal path.  This timing analysis emulates the IP process by deskewing each pin as 
	# well as accounting for the elimination of the unknown process variation.  Once the 
	# deskew emulation is complete, the analysis further considers the effect of changing 
	# the delay chain settings to the operation of the device after calibration: these 
	# effects include changes in voltage and temperature which may affect the optimality 
	# of the deskew process.
	# 
	# The timing analysis creates a write summary report indicating how the timing analysis 
	# was performed starting with a typical timing analysis before calibration.
	###############################################################################

	#######################################
	# Need access to global variables
	upvar 1 $summary_name summary
	upvar 1 $timing_parameters_array_name t
	upvar 1 $pin_array_name pins
	upvar 1 $MP_name MP
	upvar 1 $IP_name IP
	upvar 1 $board_name board
	upvar 1 $scale_factors_name scale_factors

	set eol_reduction_factor $IP(eol_reduction_factor_write)
	set num_failing_path $IP(num_report_paths)

	set debug 0
	set result 1
	
	#################################
	# Find the clock output of the PLL
	set msg_list [ list ]
	set dqs_pll_clock_id [hps_sdram_p0_get_output_clock_id $pins(dqs_pins) "DQS output" msg_list]
	if {$dqs_pll_clock_id == -1} {
		foreach {msg_type msg} $msg_list {
			post_message -type $msg_type "$msg"
		}
		post_message -type warning "Failed to find PLL clock for pins [join $pins(dqs_pins)]"
		set result 0
	} else {
		set dqsclksource [get_node_info -name $dqs_pll_clock_id]
	}

	set msg_list [ list ]
	set dq_pll_clock_id [hps_sdram_p0_get_output_clock_id [ join [ join $pins(q_groups) ]] "DQ output" msg_list]
	if {$dq_pll_clock_id == -1} {
		foreach {msg_type msg} $msg_list {
			post_message -type $msg_type "$msg"
		}
		post_message -type warning "Failed to find PLL clock for pins [ join [ join $pins(q_groups) ]]"
		set result 0
	} else {
		set dqclksource [get_node_info -name $dq_pll_clock_id]
	}

	foreach q_group $pins(q_groups) {
		set q_group $q_group
		lappend q_groups $q_group
	}
	set all_dq_pins [ join [ join $q_groups ] ]
	set dm_pins $pins(dm_pins)
	set all_dq_dm_pins [ concat $all_dq_pins $dm_pins ]	
	
	if {$IP(write_deskew_mode) == "dynamic"} {
		set panel_name_setup  "Before Calibration \u0028Negative slacks are OK\u0029||$inst Write \u0028Before Calibration\u0029 (setup)"
		set panel_name_hold   "Before Calibration \u0028Negative slacks are OK\u0029||$inst Write \u0028Before Calibration\u0029 (hold)"
	} else {
		set panel_name_setup  "Before Spatial Pessimism Removal \u0028Negative slacks are OK\u0029||$inst Write (setup)"
		set panel_name_hold   "Before Spatial Pessimism Removal \u0028Negative slacks are OK\u0029||$inst Write (hold)"
	}	
	
	#####################################################################
	# Default Write Analysis
	set before_calibration_reporting [get_ini_var -name "qsta_enable_before_calibration_ddr_reporting"]
	if {![string equal -nocase $before_calibration_reporting off]}  {
		set res_0 [report_timing -detail full_path -to [get_ports $all_dq_dm_pins] \
			-npaths $num_failing_path -panel_name $panel_name_setup -setup -disable_panel_color -quiet]
		set res_1 [report_timing -detail full_path -to [get_ports $all_dq_dm_pins] \
			-npaths $num_failing_path -panel_name $panel_name_hold -hold -disable_panel_color -quiet]
	}

	# Perform the default timing analysis to get required and arrival times
	set paths_setup [get_timing_paths -to [get_ports $all_dq_dm_pins] -npaths 400 -setup -nworst 1]
	set paths_hold  [get_timing_paths -to [get_ports $all_dq_dm_pins] -npaths 400 -hold  -nworst 1]

	#####################################
	# Find Memory Calibration Improvement 
	#####################################
	
	set mp_setup_slack 0
	set mp_hold_slack  0
	if {($IP(write_deskew_mode) == "dynamic") && ($IP(mp_calibration) == 1) && ($IP(num_ranks) == 1)} {
		# Reduce the effect of tDS on the setup slack
		set mp_setup_slack [expr $MP(DS)*$t(DS)]
		
		# Reduce the effect of tDH on the hold slack
		set mp_hold_slack  [expr $MP(DH)*$t(DH)]
	}
	set pll_ccpp 0

	########################################
	# Go over each pin and compute its slack
	# Then include any effects that are unique
	# to source synchronous designs including
	# common clocks, signal correlation, and
	# IP calibration options to compute the
	# total slack of the instance
	
	set setup_slack 1000000000
	set hold_slack  1000000000
	set default_setup_slack 1000000000
	set default_hold_slack  1000000000	

	set max_write_deskew_setup [expr $IP(write_deskew_range_setup)*$IP(quantization_T9)]
	set max_write_deskew_hold  [expr $IP(write_deskew_range_hold)*$IP(quantization_T9)]

	if {($result == 1)} {

		# Go over each DQS pin
		set group_number -1
		foreach dqpins $pins(q_groups) {
		
			set group_number [expr $group_number + 1]
			
			set dqspin [lindex $pins(dqs_pins) $group_number]
			set dqsnpin [lindex $pins(dqsn_pins) $group_number]
			set dmpins [lindex $pins(dm_pins) $group_number]
			set dqdmpins $dqpins
			if {[llength $dmpins] > 0} {
				lappend dqdmpins $dmpins
			}

			# Find DQS clock node before the periphery 
			set msg_list [list]
			set leveling_delay_chain_name [hps_sdram_p0_traverse_to_leveling_delay_chain $dqspin msg_list]

			set dqs_periphery_node ${leveling_delay_chain_name}|clkin
			
			set cps_name [hps_sdram_p0_traverse_to_clock_phase_select $dqspin msg_list]
			set dqs_clk_phase_select_node ${cps_name}|clkout
			# Find paths from PLL to DQS clock periphery node
			set DQSpaths_max [get_path -rise_from $dqsclksource -rise_to $dqs_clk_phase_select_node -nworst 1]
			set DQSpaths_min [get_path -rise_from $dqsclksource -rise_to $dqs_clk_phase_select_node -nworst 1 -min_path]
			set DQSmin_of_max [hps_sdram_p0_min_in_collection $DQSpaths_max "arrival_time"]
			set DQSmax_of_min [hps_sdram_p0_max_in_collection $DQSpaths_min "arrival_time"]
			set DQSmax_of_max [hps_sdram_p0_max_in_collection $DQSpaths_max "arrival_time"]
			set DQSmin_of_min [hps_sdram_p0_min_in_collection $DQSpaths_min "arrival_time"]
		
			#############################################
			# Find extra DQS pessimism due to correlation (both spatial correlation and aging correlation)
			#############################################
			
			# Find paths from DQS clock periphery node to beginning of output buffer
			set output_buffer_node ${inst}|p0|umemphy|uio_pads|dq_ddio[${group_number}].ubidir_dq_dqs|altdq_dqs2_inst|*obuf*_0|i
			set DQSperiphery_min [get_path -rise_from $dqs_clk_phase_select_node -rise_to $dqspin -min_path -nworst 1]
			set DQSperiphery_max [get_path -rise_from $dqs_clk_phase_select_node -rise_to $dqspin -nworst 1]
			set DQSperiphery_min_delay [hps_sdram_p0_min_in_collection $DQSperiphery_min "arrival_time"]
			set DQSperiphery_max_delay [hps_sdram_p0_max_in_collection $DQSperiphery_max "arrival_time"]
			set aiot_delay [hps_sdram_p0_round_3dp [expr [hps_sdram_p0_get_rise_aiot_delay $dqspin] * 1e9]]
			set DQSperiphery_min_delay [expr $DQSperiphery_min_delay - $aiot_delay]
			set DQSperiphery_max_delay [expr $DQSperiphery_max_delay - $aiot_delay]
			set DQSpath_pessimism  [expr $DQSperiphery_min_delay*($scale_factors(emif) + $scale_factors(eol) - $scale_factors(eol)/$eol_reduction_factor)]

			# Go over each DQ pin in group
			set dq_index 0
			set dm_index 0
			
			foreach dqpin $dqdmpins {
			
				if {[lsearch -exact $dmpins $dqpin] >= 0} {
					set isdmpin 1
					regexp {\d+} $dqpin dm_pin_index
				} else {
					set isdmpin 0
					regexp {\d+} $dqpin dq_pin_index
				}
				
				# Perform the default timing analysis to get required and arrival times
				set pin_setup_slack [hps_sdram_p0_min_in_collection_to_name $paths_setup "slack" $dqpin]
				set pin_hold_slack  [hps_sdram_p0_min_in_collection_to_name $paths_hold "slack" $dqpin]

				set default_setup_slack [min $default_setup_slack $pin_setup_slack]
				set default_hold_slack  [min $default_hold_slack  $pin_hold_slack]		

				if { $debug } {
					puts "$group_number $dqspin $dqpin $pin_setup_slack $pin_hold_slack"	
				}
				
				###############################
				# Extra common clock pessimism removal (from PLL) that is not caught by STA
				###############################
				
				# Find the DQ clock node before the periphery
				set msg_list [list]
				set leveling_delay_chain_name [hps_sdram_p0_traverse_to_leveling_delay_chain $dqpin msg_list]
				
				set dq_periphery_node ${leveling_delay_chain_name}|clkin

				set cps_name [hps_sdram_p0_traverse_to_clock_phase_select $dqpin msg_list]
				set dq_clk_phase_select_node ${cps_name}|clkout

				# Find paths from PLL to DQ clock periphery node
				set DQpaths_max [get_path -rise_from $dqclksource -rise_to $dq_clk_phase_select_node -nworst 1]
				set DQpaths_min [get_path -rise_from $dqclksource -rise_to $dq_clk_phase_select_node -nworst 1 -min_path]
				set DQmin_of_max [hps_sdram_p0_min_in_collection $DQpaths_max "arrival_time"]
				set DQmax_of_min [hps_sdram_p0_max_in_collection $DQpaths_min "arrival_time"]
				set DQmax_of_max [hps_sdram_p0_max_in_collection $DQpaths_max "arrival_time"]
				set DQmin_of_min [hps_sdram_p0_min_in_collection $DQpaths_min "arrival_time"]
				if {[expr abs(($DQSmax_of_min - $DQSmin_of_max) - ($DQmax_of_min - $DQmin_of_max))] < 0.05} {
					set extra_ccpp_DQS [expr $DQSmin_of_max - $DQSmax_of_min]
					set extra_ccpp_DQ  [expr $DQmin_of_max  - $DQmax_of_min]
					set extra_ccpp [expr [min $extra_ccpp_DQS $extra_ccpp_DQ] + $pll_ccpp]
				} else {
					set extra_ccpp $pll_ccpp
				}
				
				# Add the extra ccpp to both setup and hold slacks
				set pin_setup_slack [expr $pin_setup_slack + $extra_ccpp]
				set pin_hold_slack [expr $pin_hold_slack + $extra_ccpp]
								
				
				########################################
				# Add the memory calibration improvement
				########################################
				
				set pin_setup_slack [expr $pin_setup_slack + $mp_setup_slack]
				set pin_hold_slack [expr $pin_hold_slack + $mp_hold_slack]
		
				############################################
				# Find extra DQ pessimism due to correlation
				# (both spatial correlation and aging correlation)
				############################################
				
				# Find the DQ clock node before the periphery
				if {$isdmpin == 1} {
					set output_buffer_node_dq ${inst}|p0|umemphy|uio_pads|dq_ddio[${group_number}].ubidir_dq_dqs|altdq_dqs2_inst|*extra_output_pad_gen[0].obuf_1|i
				} else {
					set output_buffer_node_dq ${inst}|p0|umemphy|uio_pads|dq_ddio[${group_number}].ubidir_dq_dqs|altdq_dqs2_inst|pad_gen[${dq_index}].data_out|i
				}
				
				set DQperiphery_min [get_path -rise_from $dq_clk_phase_select_node -rise_to $dqpin -min_path -nworst 1]
				set DQperiphery_max [get_path -rise_from $dq_clk_phase_select_node -rise_to $dqpin -nworst 1]

				set DQperiphery_min_delay [hps_sdram_p0_min_in_collection $DQperiphery_min "arrival_time"]
				set DQperiphery_max_delay [hps_sdram_p0_max_in_collection $DQperiphery_max "arrival_time"]
				set aiot_delay [hps_sdram_p0_round_3dp [expr [hps_sdram_p0_get_rise_aiot_delay $dqpin] * 1e9]]
				set DQperiphery_min_delay [expr $DQperiphery_min_delay - $aiot_delay]
				set DQperiphery_max_delay [expr $DQperiphery_max_delay - $aiot_delay]
				set DQpath_pessimism  [expr $DQperiphery_min_delay*($scale_factors(emif) + $scale_factors(eol) - $scale_factors(eol)/$eol_reduction_factor)]
				
				########################################
				# Merge current slacks with other slacks
				########################################

				# If write deskew is available, the setup and hold slacks for this pin will be equal
				#   and can also remove the extra DQS and DQ pessimism removal
				if {$IP(write_deskew_mode) == "dynamic"} {
				
					set extra_pessimism $IP(epw)*$DQperiphery_min_delay
				
					# Consider the maximum range of the deskew when deskewing
					set shift_setup_slack [expr ($pin_setup_slack + $pin_hold_slack)/2 - $pin_setup_slack]
					if {$shift_setup_slack >= $max_write_deskew_setup} {
						if { $debug } {
							puts "limited setup"
						}
						set pin_setup_slack [expr $pin_setup_slack + $max_write_deskew_setup + $extra_pessimism/2]
						set pin_hold_slack [expr $pin_hold_slack - $max_write_deskew_setup + $extra_pessimism/2]

						# Remember the largest shifts in either direction
						if {[info exist max_shift]} {
							if {$max_write_deskew_setup > $max_shift} {
								set max_shift $max_write_deskew_setup
							}
							if {$max_write_deskew_setup < $min_shift} {
								set min_shift $max_write_deskew_setup
							}
						} else {
							set max_shift $max_write_deskew_setup
							set min_shift $max_shift
						}

					} elseif {$shift_setup_slack <= -$max_write_deskew_hold} {
						if { $debug } {
							puts "limited hold"
						}
						set pin_setup_slack [expr $pin_setup_slack - $max_write_deskew_hold + $extra_pessimism/2]
						set pin_hold_slack [expr $pin_hold_slack + $max_write_deskew_hold + $extra_pessimism/2]

						# Remember the largest shifts in either direction
						if {[info exist max_shift]} {
							if {[expr 0 -$max_write_deskew_hold] > $max_shift} {
								set max_shift [expr 0 - $max_write_deskew_hold]
							}
							if {[expr 0 -$max_write_deskew_hold] < $min_shift} {
								set min_shift [expr 0 - $max_write_deskew_hold]
							}
						} else {
							set max_shift [expr 0 - $max_write_deskew_hold]
							set min_shift $max_shift
						}
					} else {
						# In this case we can also consider the DQS/DQpath pessimism since we can guarantee we have enough delay chain settings to align it
						set pin_setup_slack [expr $pin_setup_slack + $shift_setup_slack + $DQSpath_pessimism/2 + $DQpath_pessimism/2 + $extra_pessimism/2]
						set pin_hold_slack [expr $pin_hold_slack - $shift_setup_slack + $DQSpath_pessimism/2 + $DQpath_pessimism/2 + $extra_pessimism/2]

						# Remember the largest shifts in either direction
						if {[info exist max_shift]} {
							if {[expr $shift_setup_slack + $DQSpath_pessimism/2 + $DQpath_pessimism/2] > $max_shift} {
								set max_shift [expr $shift_setup_slack + $DQSpath_pessimism/2 + $DQpath_pessimism/2]
							}
							if {[expr $shift_setup_slack - $DQSpath_pessimism/2 - $DQpath_pessimism/2] < $min_shift} {
								set min_shift [expr $shift_setup_slack - $DQSpath_pessimism/2 - $DQpath_pessimism/2]
							}
						} else {
							set max_shift [expr $shift_setup_slack + $DQSpath_pessimism/2 + $DQpath_pessimism/2]
							set min_shift [expr $shift_setup_slack - $DQSpath_pessimism/2 - $DQpath_pessimism/2]
						}
					}
				} else {
					# For uncalibrated calls, there is some spatial correlation between DQ and DQS signals, so remove
					# some of the pessimism
					set total_DQ_DQS_pessimism [expr $DQSpath_pessimism + $DQpath_pessimism]
					set dqs_width [llength $dqpins]
					if {$dqs_width <= 9} {
						set pin_setup_slack [expr $pin_setup_slack + 0.35*$total_DQ_DQS_pessimism]
						set pin_hold_slack  [expr $pin_hold_slack  + 0.35*$total_DQ_DQS_pessimism]
					} 
				} 
				

				set setup_slack [min $setup_slack $pin_setup_slack]
				set hold_slack  [min $hold_slack $pin_hold_slack]
				
				if { $debug } {
					puts "                                $extra_ccpp $DQSpath_pessimism $DQpath_pessimism ($pin_setup_slack $pin_hold_slack $setup_slack $hold_slack)" 
				}

				if {$isdmpin == 0} {
					set dq_index [expr $dq_index + 1]
				} else {
					set dm_index [expr $dm_index + 1]
				}
			}
		}
	}

	###############################
	# Consider some post calibration effects on calibration
	#  and output the write summary report
	###############################
	set positive_fcolour [list "black" "blue" "blue"]
	set negative_fcolour [list "black" "red"  "red"]
	
	set wr_summary [list]
	
	if {$IP(write_deskew_mode) == "dynamic"} {
		lappend wr_summary [list "  Before Calibration Write" [hps_sdram_p0_format_3dp $default_setup_slack] [hps_sdram_p0_format_3dp $default_hold_slack]]
	} else {
		lappend wr_summary [list "  Standard Write" [hps_sdram_p0_format_3dp $default_setup_slack] [hps_sdram_p0_format_3dp $default_hold_slack]]
	}
	
	if {($IP(write_deskew_mode) == "dynamic") && ($IP(mp_calibration) == 1) && ($IP(num_ranks) == 1)} {
		lappend wr_summary [list "  Memory Calibration" [hps_sdram_p0_format_3dp $mp_setup_slack] [hps_sdram_p0_format_3dp $mp_hold_slack]]
	}		
	
	if {$IP(write_deskew_mode) == "dynamic"} {
		if { $debug } {
			puts "	$setup_slack $hold_slack"
		}
		
		#######################################
		# Find values for uncertainty table
		set t(wru_fpga_deskew_s) [expr $setup_slack - $default_setup_slack - $extra_ccpp - $mp_setup_slack]
		set t(wru_fpga_deskew_h) [expr $hold_slack  - $default_hold_slack  - $extra_ccpp - $mp_setup_slack]
		#######################################

		# Remove external delays (add slack) that are fixed by the dynamic deskew
		if { $IP(discrete_device) == 1 } {
			set t(WL_PSE) 0
		}
		[catch {get_float_table_node_delay -src {DELAYCHAIN_T9} -dst {VTVARIATION} -parameters [list IO $interface_type]} t9_vt_variation_percent]
		set extra_shift [expr $board(intra_DQS_group_skew) + [hps_sdram_p0_round_3dp [expr (1.0-$t9_vt_variation_percent)*$t(WL_PSE)]]]
		
		if {$extra_shift > [expr $max_write_deskew_setup - $max_shift]} {
			set setup_slack [expr $setup_slack + $max_write_deskew_setup - $max_shift]
		} else {
			set setup_slack [expr $setup_slack + $extra_shift]
		}
		if {$extra_shift > [expr $max_write_deskew_hold + $min_shift]} {
			set hold_slack [expr $hold_slack + $max_write_deskew_hold + $min_shift]
		} else {
			set hold_slack [expr $hold_slack + $extra_shift]
		}	

		if { $debug } {
			puts "	$setup_slack $hold_slack"
		}
		set deskew_setup [expr $setup_slack - $default_setup_slack -$mp_setup_slack]
		set deskew_hold  [expr $hold_slack - $default_hold_slack - $mp_hold_slack]
		lappend wr_summary [list "  Deskew Write and/or more clock pessimism removal" [hps_sdram_p0_format_3dp $deskew_setup] [hps_sdram_p0_format_3dp $deskew_hold]]
		
		#######################################
		# Find values for uncertainty table
		set t(wru_external_deskew_s) [expr $deskew_setup - $t(wru_fpga_deskew_s) + $mp_setup_slack - $extra_ccpp]
		set t(wru_external_deskew_h) [expr $deskew_hold  - $t(wru_fpga_deskew_h) + $mp_hold_slack  - $extra_ccpp]
		#######################################

		# Consider errors in the dynamic deskew
		set t9_quantization $IP(quantization_T9)
		set setup_slack [expr $setup_slack - $t9_quantization]
		set hold_slack  [expr $hold_slack - $t9_quantization]
		if { $debug } {
			puts "	$setup_slack $hold_slack"
		}
		lappend wr_summary [list "  Quantization error" [hps_sdram_p0_format_3dp [expr 0-$t9_quantization]] [hps_sdram_p0_format_3dp [expr 0-$t9_quantization]]]
		
		# Consider variation in the delay chains used during dynamic deksew
		#This works out to 0 since the DLL length is 8 for AV/CV
		set offset_from_90 0
		if {$IP(num_ranks) == 1} {
			set t9_variation [expr [min [expr $offset_from_90 + [max [expr $MP(DS)*$t(DS)] [expr $MP(DH)*$t(DH)]] + (2*$board(intra_DQS_group_skew) + $max_package_skew + $t(WL_PSE))] [max $max_write_deskew_setup $max_write_deskew_hold]]*2*$t9_vt_variation_percent]
		} else {
			set t9_variation [expr [min [expr $offset_from_90 + (2*$board(intra_DQS_group_skew) + $max_package_skew + $t(WL_PSE))] [max $max_write_deskew_setup $max_write_deskew_hold]]*2*$t9_vt_variation_percent]
		}
		set setup_slack [expr $setup_slack - $t9_variation]
		set hold_slack  [expr $hold_slack - $t9_variation]	
		if { $debug } {
			puts "	$setup_slack $hold_slack"
		}
		lappend wr_summary [list "  Calibration uncertainty" [hps_sdram_p0_format_3dp [expr 0-$t9_variation]] [hps_sdram_p0_format_3dp [expr 0-$t9_variation]]] 
		
		#######################################
		# Find values for uncertainty table
		set uncertainty_reporting [get_ini_var -name "qsta_enable_uncertainty_ddr_reporting"]
		if {[string equal -nocase $uncertainty_reporting on]} {
			set t(wru_calibration_uncertaintyerror_s) [expr 0 - $t9_variation - $t9_quantization]
			set t(wru_calibration_uncertaintyerror_h) [expr 0 - $t9_variation - $t9_quantization]
			set t(wru_fpga_uncertainty_s) [expr $t(CK)/4 - $default_setup_slack - $t(wru_output_max_delay_external) - $extra_ccpp]
			set t(wru_fpga_uncertainty_h) [expr $t(CK)/4 - $default_hold_slack  - $t(wru_output_min_delay_external) - $extra_ccpp]
			set t(wru_extl_uncertainty_s) [expr $t(wru_output_max_delay_external)]
			set t(wru_extl_uncertainty_h) [expr $t(wru_output_min_delay_external)]		
		}
		#######################################
		
	} else {
		set pessimism_setup [expr $setup_slack - $default_setup_slack - $mp_setup_slack]
		set pessimism_hold  [expr $hold_slack - $default_hold_slack - $mp_hold_slack]
		lappend wr_summary [list "  Spatial correlation pessimism removal" [hps_sdram_p0_format_3dp $pessimism_setup] [hps_sdram_p0_format_3dp $pessimism_hold]]

		#######################################
		# Find values for uncertainty table
		set uncertainty_reporting [get_ini_var -name "qsta_enable_uncertainty_ddr_reporting"]
		if {[string equal -nocase $uncertainty_reporting on]} {		
			set t(wru_fpga_deskew_s) 0
			set t(wru_fpga_deskew_h) 0
			set t(wru_external_deskew_s) 0
			set t(wru_external_deskew_h) 0
			set t(wru_calibration_uncertaintyerror_s) 0
			set t(wru_calibration_uncertaintyerror_h) 0
			set t(wru_fpga_uncertainty_s) [expr $t(CK)/4 - $default_setup_slack - $t(wru_output_max_delay_external) - $pessimism_setup]
			set t(wru_fpga_uncertainty_h) [expr $t(CK)/4 - $default_hold_slack  - $t(wru_output_min_delay_external) - $pessimism_hold]
			set t(wru_extl_uncertainty_s) [expr $t(wru_output_max_delay_external)]
			set t(wru_extl_uncertainty_h) [expr $t(wru_output_min_delay_external)]				
		}
		#######################################
	}	
	
	###############################
	# Consider Duty Cycle Calibration if enabled
	###############################

	if {($IP(write_dcc) == "dynamic")} {
		#First remove the Systematic DCD
		set setup_slack [expr $setup_slack + $t(WL_DCD)]
		set hold_slack  [expr $hold_slack + $t(WL_DCD)]
		if { $debug } {
			puts "	$setup_slack $hold_slack"
		}
		lappend wr_summary [list "  Duty cycle correction" $t(WL_DCD) $t(WL_DCD)]
		
		#Add errors in the DCC
		set DCC_quantization $IP(quantization_DCC)
		set setup_slack [expr $setup_slack - $DCC_quantization]
		set hold_slack  [expr $hold_slack - $DCC_quantization]
		if { $debug } {
			puts "	$setup_slack $hold_slack"
		}
		lappend wr_summary [list "  Duty cycle correction quantization error" [hps_sdram_p0_format_3dp [expr 0-$DCC_quantization]] [hps_sdram_p0_format_3dp [expr 0-$DCC_quantization]]]
		
		# Consider variation in the DCC 
		[catch {get_float_table_node_delay -src {DELAYCHAIN_DUTY_CYCLE} -dst {VTVARIATION} -parameters [list IO $interface_type]} dcc_vt_variation_percent]
		set dcc_variation [expr $t(WL_DCD)*2*$dcc_vt_variation_percent]
		set setup_slack [expr $setup_slack - $dcc_variation]
		set hold_slack  [expr $hold_slack - $dcc_variation]		
		if { $debug } {
			puts "	$setup_slack $hold_slack"
		}
		lappend wr_summary [list "  Duty cycle correction calibration uncertainity" [hps_sdram_p0_format_3dp [expr 0-$dcc_variation]] [hps_sdram_p0_format_3dp [expr 0-$dcc_variation]]]
	}
	
	#######################################
	#######################################
	# Create the write analysis panel	
	set panel_name "$inst Write"
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
	
	if {($setup_slack < 0) || ($hold_slack <0)} {
		set panel_id [create_report_panel -table $panel_name -color red]
	} else {
		set panel_id [create_report_panel -table $panel_name]
	}
	add_row_to_table -id $panel_id [list "Operation" "Setup Slack" "Hold Slack"] 		
	
	if {($IP(write_deskew_mode) == "dynamic")} {
		set fcolour [hps_sdram_p0_get_colours $setup_slack $hold_slack]
		add_row_to_table -id $panel_id [list "After Calibration Write" [hps_sdram_p0_format_3dp $setup_slack] [hps_sdram_p0_format_3dp $hold_slack]] -fcolor $fcolour
		lappend summary [list $opcname 0 "Write ($opcname)" $setup_slack $hold_slack]
	} else {
		set fcolour [hps_sdram_p0_get_colours $setup_slack $hold_slack] 
		add_row_to_table -id $panel_id [list "Write" [hps_sdram_p0_format_3dp $setup_slack] [hps_sdram_p0_format_3dp $hold_slack]] -fcolor $fcolour
		lappend summary [list $opcname 0 "Write ($opcname)" [hps_sdram_p0_format_3dp $setup_slack] [hps_sdram_p0_format_3dp $hold_slack]]
	}

	foreach summary_line $wr_summary {
		add_row_to_table -id $panel_id $summary_line -fcolors $positive_fcolour
	}
	
	#######################################
	# Create the Write uncertainty panel
	set uncertainty_reporting [get_ini_var -name "qsta_enable_uncertainty_ddr_reporting"]
	if {[string equal -nocase $uncertainty_reporting on]} {
		set panel_name "$inst Write Uncertainty"
		set root_folder_name [get_current_timequest_report_folder]

		if { ! [string match "${root_folder_name}*" $panel_name] } {
			set panel_name "${root_folder_name}||$panel_name"
		}

		# Delete any pre-existing summary panel
		set panel_id [get_report_panel_id $panel_name]
		if {$panel_id != -1} {
			delete_report_panel -id $panel_id
		}
		
		set panel_id [create_report_panel -table $panel_name]
		add_row_to_table -id $panel_id [list "Value" "Setup Side" "Hold Side"]
		add_row_to_table -id $panel_id [list "Uncertainty" "" ""]
		add_row_to_table -id $panel_id [list "  FPGA uncertainty" [hps_sdram_p0_format_3dp $t(wru_fpga_uncertainty_s)] [hps_sdram_p0_format_3dp $t(wru_fpga_uncertainty_h)]] 
		add_row_to_table -id $panel_id [list "  External uncertainty" [hps_sdram_p0_format_3dp $t(wru_extl_uncertainty_s)] [hps_sdram_p0_format_3dp $t(wru_extl_uncertainty_h)]] 
		add_row_to_table -id $panel_id [list "Deskew" "" ""]
		add_row_to_table -id $panel_id [list "  FPGA deskew" [hps_sdram_p0_format_3dp $t(wru_fpga_deskew_s)] [hps_sdram_p0_format_3dp $t(wru_fpga_deskew_h)]] 
		add_row_to_table -id $panel_id [list "  External deskew" [hps_sdram_p0_format_3dp $t(wru_external_deskew_s)] [hps_sdram_p0_format_3dp $t(wru_external_deskew_h)]] 
		add_row_to_table -id $panel_id [list "  Calibration uncertainty/error" [hps_sdram_p0_format_3dp $t(wru_calibration_uncertaintyerror_s)] [hps_sdram_p0_format_3dp $t(wru_calibration_uncertaintyerror_h)]] 
	}		
}


#############################################################
# Read Timing Analysis
#############################################################
proc hps_sdram_p0_perform_flexible_read_capture_timing_analysis {opcs opcname inst family scale_factors_name io_std interface_type max_package_skew dqs_phase period all_dq_pins pin_array_name timing_parameters_array_name summary_name MP_name IP_name board_name fpga_name} {

	################################################################################
	# This timing analysis covers the read timing constraints.  It includes support 
	# for uncalibrated and calibrated read paths.  The analysis starts by running a 
	# conventional timing analysis for the read paths and then adds support for 
	# topologies and IP options which are unique to source-synchronous data transfers.  
	# The support for further topologies includes correlation between DQ and DQS signals
	# The support for further IP includes support for read-deskew calibration.
	# 
	# During read deskew calibration, the IP will adjust delay chain settings along 
	# each signal path to reduce the skew between DQ pins and to centre align the DQ 
	# strobe within the DVW.  This operation has the benefit of increasing margin on the 
	# setup and hold, as well as removing some of the unknown process variation on each 
	# signal path.  This timing analysis emulates the IP process by deskewing each pin as 
	# well as accounting for the elimination of the unknown process variation.  Once the 
	# deskew emulation is complete, the analysis further considers the effect of changing 
	# the delay chain settings to the operation of the device after calibration: these 
	# effects include changes in voltage and temperature which may affect the optimality 
	# of the deskew process.
	# 
	# The timing analysis creates a read summary report indicating how the timing analysis 
	# was performed starting with a typical timing analysis before calibration.
	###############################################################################

	#######################################
	# Need access to global variables
	upvar 1 $summary_name summary
	upvar 1 $timing_parameters_array_name t
	upvar 1 $pin_array_name pins
	upvar 1 $MP_name MP
	upvar 1 $IP_name IP
	upvar 1 $board_name board
	upvar 1 $fpga_name fpga
	upvar 1 $scale_factors_name scale_factors
	
	set eol_reduction_factor $IP(eol_reduction_factor_read)
	set num_failing_path $IP(num_report_paths)

	# Debug switch. Change to 1 to get more run-time debug information
	set debug 0	
	set result 1

	foreach dqsclock $pins(dqs_pins) {
		lappend dqs_pins_in ${dqsclock}_IN
	}
	
	if {$IP(read_deskew_mode) == "dynamic"} {
		set panel_name_setup  "Before Calibration \u0028Negative slacks are OK\u0029||$inst Read Capture \u0028Before Calibration\u0029 (setup)"
		set panel_name_hold   "Before Calibration \u0028Negative slacks are OK\u0029||$inst Read Capture \u0028Before Calibration\u0029 (hold)"
	} else {
		set panel_name_setup  "Before Spatial Pessimism Removal \u0028Negative slacks are OK\u0029||$inst Read Capture (setup)"
		set panel_name_hold   "Before Spatial Pessimism Removal \u0028Negative slacks are OK\u0029||$inst Read Capture (hold)"
	}

	#####################################################################
	# Default Read Analysis
	set before_calibration_reporting [get_ini_var -name "qsta_enable_before_calibration_ddr_reporting"]
	if {![string equal -nocase $before_calibration_reporting off]} {
		set res_0 [report_timing -detail full_path -from [get_ports $all_dq_pins] \
			-to_clock [get_clocks $dqs_pins_in] -npaths $num_failing_path -panel_name $panel_name_setup -setup -disable_panel_color -quiet]
		set res_1 [report_timing -detail full_path -from [get_ports $all_dq_pins] \
			-to_clock [get_clocks $dqs_pins_in] -npaths $num_failing_path -panel_name $panel_name_hold -hold -disable_panel_color -quiet]
	}	

	set paths_setup [get_timing_paths -from [get_ports $all_dq_pins] -to_clock [get_clocks $dqs_pins_in] -npaths 400 -setup -nworst 1]
	set paths_hold  [get_timing_paths -from [get_ports $all_dq_pins] -to_clock [get_clocks $dqs_pins_in] -npaths 400 -hold  -nworst 1]

	#####################################
	# Find Memory Calibration Improvement
	#####################################
	
	set mp_setup_slack 0
	set mp_hold_slack  0
	if {($IP(read_deskew_mode) == "dynamic") && ($IP(mp_calibration) == 1) && ($IP(num_ranks) == 1)} {
		# Reduce the effect of tDQSQ on the setup slack
		set mp_setup_slack [expr $MP(DQSQ)*$t(DQSQ)]

		# Reduce the effect of tQH_time on the hold slack
		set mp_hold_slack  [expr $MP(QH_time)*(0.5*$period-$t(QH_time))]
	}

	########################################
	# Go over each pin and compute its slack
	# Then include any effects that are unique
	# to source synchronous designs including
	# common clocks, signal correlation, and
	# IP calibration options to compute the
	# total slack of the instance	

	set prefix [ string map "| |*:" $inst ]
	set prefix "*:$prefix"	
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
	# Phase Error on DQS paths. This parameter is queried at run time
	set fpga(tDQS_PSERR) [ expr [ get_integer_node_delay -integer $::GLOBAL_hps_sdram_p0_dqs_delay_chain_length -parameters {IO MAX HIGH} -src DQS_PSERR -in_fitter ] / 1000.0 ]

	set setup_slack 1000000000
	set hold_slack  1000000000
	set default_setup_slack 1000000000
	set default_hold_slack  1000000000	
		
	# Find quiet jitter values during calibration
	if {$family == "arria v"} {
		set quiet_clk_jitter_proportion 0.25
	} else {
		set quiet_clk_jitter_proportion 0.5
	}
	set quiet_setup_jitter [expr 0.8*$DQSpathjitter*$DQSpathjitter_setup_prop]
	set quiet_hold_jitter  [expr 0.8*$DQSpathjitter*(1-$DQSpathjitter_setup_prop) + $quiet_clk_jitter_proportion*$tJITper/2]		
	set max_read_deskew_setup [expr $IP(read_deskew_range_setup)*$IP(quantization_T1)]
	set max_read_deskew_hold  [expr $IP(read_deskew_range_hold)*$IP(quantization_T1)]
		
	if {($result == 1)} {

		# Go over each DQS pin
		set group_number -1
		foreach qpins $pins(q_groups) {
			
			set group_number [expr $group_number + 1]
			
			set dqspin [lindex $pins(dqs_pins) $group_number]
			set dqsnpin [lindex $pins(dqsn_pins) $group_number]
			
			#############################################
			# Find extra DQS pessimism due to correlation 
			# (both spatial correlation and aging correlation)
			#############################################

			# Find paths from output of the input buffer to the end of the DQS periphery
			set input_buffer_node "${inst}|p0|umemphy|uio_pads|dq_ddio[${group_number}].ubidir_dq_dqs|altdq_dqs2_inst|*strobe_in|o"
			set DQScapture_node [list "${prefix}*dq_ddio[${group_number}].ubidir_dq_dqs|*altdq_dqs2_inst|*input_path_gen[*].capture_reg~DFFLO" ]

			set DQSperiphery_min [get_path -rise_from $input_buffer_node -rise_to $DQScapture_node -min_path -nworst 1]
			set DQSperiphery_max [get_path -rise_from $input_buffer_node -rise_to $DQScapture_node -nworst 1]
			set DQSperiphery_min_delay [hps_sdram_p0_min_in_collection $DQSperiphery_min "arrival_time"]
			set DQSperiphery_max_delay [hps_sdram_p0_max_in_collection $DQSperiphery_max "arrival_time"]
			set DQSpath_pessimism  [expr ($DQSperiphery_min_delay - 90.0/360*$t(CK))*($scale_factors(emif) + $scale_factors(eol) - $scale_factors(eol)/$eol_reduction_factor)]
			
			# Go over each DQ pin in group
			set q_index 0
			foreach qpin $qpins {
				regexp {\d+} $qpin q_pin_index
			
				# Perform the default timing analysis to get required and arrival times
				set pin_setup_slack [hps_sdram_p0_min_in_collection_from_name $paths_setup "slack" $qpin]
				set pin_hold_slack  [hps_sdram_p0_min_in_collection_from_name $paths_hold "slack" $qpin]

				set default_setup_slack [min $default_setup_slack $pin_setup_slack]
				set default_hold_slack  [min $default_hold_slack  $pin_hold_slack]		

				if { $debug } {
					puts "READ: $group_number $dqspin $qpin $pin_setup_slack $pin_hold_slack (MP: $mp_setup_slack $mp_hold_slack)"
				}
			
				###############################
				# Add the memory calibration improvement
				###############################
				
				set pin_setup_slack [expr $pin_setup_slack + $mp_setup_slack]
				set pin_hold_slack [expr $pin_hold_slack + $mp_hold_slack]
				
				############################################
				# Find extra DQ pessimism due to correlation
				############################################
				
				# Find paths from output of the input buffer to the end of the DQ periphery
				set input_buffer_node_dq ${inst}|p0|umemphy|uio_pads|dq_ddio[${group_number}].ubidir_dq_dqs|altdq_dqs2_inst|*pad_gen[${q_index}].data_in|o
				set DQcapture_node [list "${prefix}*dq_ddio[${group_number}].ubidir_dq_dqs|*altdq_dqs2_inst|*input_path_gen[${q_index}].capture_reg~DFFLO" ]

				set DQperiphery_min [get_path -rise_from $input_buffer_node_dq -rise_to $DQScapture_node -min_path -nworst 1]
				set DQperiphery_max [get_path -rise_from $input_buffer_node_dq -rise_to $DQScapture_node -nworst 1]
				set DQperiphery_min_delay [hps_sdram_p0_min_in_collection $DQperiphery_min "arrival_time"]
				set DQperiphery_max_delay [hps_sdram_p0_max_in_collection $DQperiphery_max "arrival_time"]
				set DQpath_pessimism  [expr $DQperiphery_min_delay*($scale_factors(emif) + $scale_factors(eol) - $scale_factors(eol)/$eol_reduction_factor)]
				
				########################################
				# Merge current slacks with other slacks
				########################################

				# If read deskew is available, the setup and hold slacks for this pin will be equal
				#   and can also remove the extra DQS pessimism removal
				if {$IP(read_deskew_mode) == "dynamic"} {
				
					set extra_pessimism $IP(epr)*$DQperiphery_min_delay

					# Consider the maximum range of the deskew when deskewing
					set shift_setup_slack [expr (($pin_setup_slack + $quiet_setup_jitter) + ($pin_hold_slack + $quiet_hold_jitter))/2 - $pin_setup_slack - $quiet_setup_jitter]
					if {$shift_setup_slack >= $max_read_deskew_setup} {
						if { $debug } {
							puts "limited setup"
						}
						set pin_setup_slack [expr $pin_setup_slack + $max_read_deskew_setup + $extra_pessimism/2]
						set pin_hold_slack [expr $pin_hold_slack - $max_read_deskew_setup + $extra_pessimism/2]
						
						# Remember the largest shifts in either direction
						if {[info exist max_shift]} {
							if {$max_read_deskew_setup > $max_shift} {
								set max_shift $max_read_deskew_setup
							}
							if {$max_read_deskew_setup < $min_shift} {
								set min_shift $max_read_deskew_setup
							}
						} else {
							set max_shift $max_read_deskew_setup
							set min_shift $max_shift
						}
						
					} elseif {$shift_setup_slack <= -$max_read_deskew_hold} {
						if { $debug } {
							puts "limited hold"
						}
						set pin_setup_slack [expr $pin_setup_slack - $max_read_deskew_hold + $extra_pessimism/2]
						set pin_hold_slack [expr $pin_hold_slack + $max_read_deskew_hold + $extra_pessimism/2]
						
						# Remember the largest shifts in either direction
						if {[info exist max_shift]} {
							if {[expr 0 -$max_read_deskew_hold] > $max_shift} {
								set max_shift [expr 0 - $max_read_deskew_hold]
							}
							if {[expr 0 -$max_read_deskew_hold] < $min_shift} {
								set min_shift [expr 0 - $max_read_deskew_hold]
							}
						} else {
							set max_shift [expr 0 - $max_read_deskew_hold]
							set min_shift $max_shift
						}
					} else {
						# In this case we can also consider the DQS path pessimism since we can guarantee we have enough delay chain settings to align it
						set pin_setup_slack [expr $pin_setup_slack + $shift_setup_slack + $DQSpath_pessimism/2 + $DQpath_pessimism/2 + $extra_pessimism/2]
						set pin_hold_slack [expr $pin_hold_slack - $shift_setup_slack + $DQSpath_pessimism/2 + $DQpath_pessimism/2 + $extra_pessimism/2]
						
						# Remember the largest shifts in either direction
						if {[info exist max_shift]} {
							if {[expr $shift_setup_slack + $DQSpath_pessimism/2 + $DQpath_pessimism/2] > $max_shift} {
								set max_shift [expr $shift_setup_slack + $DQSpath_pessimism/2 + $DQpath_pessimism/2]
							}
							if {[expr $shift_setup_slack - $DQSpath_pessimism/2 - $DQpath_pessimism/2] < $min_shift} {
								set min_shift [expr $shift_setup_slack - $DQSpath_pessimism/2 - $DQpath_pessimism/2]
							}
						} else {
							set max_shift [expr $shift_setup_slack + $DQSpath_pessimism/2 + $DQpath_pessimism/2]
							set min_shift [expr $shift_setup_slack - $DQSpath_pessimism/2 - $DQpath_pessimism/2]
						}
					}
				} else {
					# For uncalibrated calls, there is some spatial correlation between DQ and DQS signals, so remove
					# some of the pessimism
					set total_DQ_DQS_pessimism [expr $DQSpath_pessimism + $DQpath_pessimism]
					set dqs_width [llength $qpins]
					if {$dqs_width <= 9} {
						set pin_setup_slack [expr $pin_setup_slack + 0.35*$total_DQ_DQS_pessimism]
						set pin_hold_slack  [expr $pin_hold_slack  + 0.35*$total_DQ_DQS_pessimism]
					} 
				}

				set setup_slack [min $setup_slack $pin_setup_slack]
				set hold_slack  [min $hold_slack $pin_hold_slack]
				
				if { $debug } {
					puts "READ:               $DQSpath_pessimism $DQpath_pessimism ($pin_setup_slack $pin_hold_slack $setup_slack $hold_slack)" 
				}
				set q_index [expr $q_index + 1]
			}
		}
	}
	
	########################################################
	# Consider some post calibration effects on calibration
	#  and output the read summary report
	########################################################
	
	set positive_fcolour [list "black" "blue" "blue"]
	set negative_fcolour [list "black" "red"  "red"]

	set rc_summary [list]	
	
	set fcolour [hps_sdram_p0_get_colours $default_setup_slack $default_hold_slack]
	if {$IP(read_deskew_mode) == "dynamic"} {
		lappend rc_summary [list "  Before Calibration Read Capture" [hps_sdram_p0_format_3dp $default_setup_slack] [hps_sdram_p0_format_3dp $default_hold_slack]]
	} else {
		lappend rc_summary [list "  Standard Read Capture" [hps_sdram_p0_format_3dp $default_setup_slack] [hps_sdram_p0_format_3dp $default_hold_slack]] 
	}
	
	if {($IP(read_deskew_mode) == "dynamic") && ($IP(mp_calibration) == 1) && ($IP(num_ranks) == 1)} {
		lappend rc_summary [list "  Memory Calibration" [hps_sdram_p0_format_3dp $mp_setup_slack] [hps_sdram_p0_format_3dp $mp_hold_slack]] 
	}
	
	if {$IP(read_deskew_mode) == "dynamic"} {
		if { $debug } {
			puts "	$setup_slack $hold_slack"
		}
		
		#######################################
		# Find values for uncertainty table
		set t(rdu_fpga_deskew_s) [expr $setup_slack - $default_setup_slack - $mp_setup_slack]
		set t(rdu_fpga_deskew_h) [expr $hold_slack  - $default_hold_slack  - $mp_hold_slack]
		#######################################

		# Remove external delays (add slack) that are fixed by the dynamic deskew
		[catch {get_float_table_node_delay -src {DELAYCHAIN_T1} -dst {VTVARIATION} -parameters [list IO $interface_type]} t1_vt_variation_percent]
		set extra_shift [expr $board(intra_DQS_group_skew) + [hps_sdram_p0_round_3dp [expr (1.0-$t1_vt_variation_percent)*$fpga(tDQS_PSERR)]]]
		
		if {$extra_shift > [expr $max_read_deskew_setup - $max_shift]} {
			set setup_slack [expr $setup_slack + $max_read_deskew_setup - $max_shift]
		} else {
			set setup_slack [expr $setup_slack + $extra_shift]
		}
		if {$extra_shift > [expr $max_read_deskew_hold + $min_shift]} {
			set hold_slack [expr $hold_slack + $max_read_deskew_hold + $min_shift]
		} else {
			set hold_slack [expr $hold_slack + $extra_shift]
		}	
		
		if { $debug } {
			puts "	$setup_slack $hold_slack"
		}
		set deskew_setup [expr $setup_slack - $default_setup_slack - $mp_setup_slack]
		set deskew_hold  [expr $hold_slack - $default_hold_slack - $mp_hold_slack]
		lappend rc_summary [list "  Deskew Read" [hps_sdram_p0_format_3dp $deskew_setup] [hps_sdram_p0_format_3dp $deskew_hold]]
		
		#######################################
		# Find values for uncertainty table
		set t(rdu_external_deskew_s) [expr $deskew_setup - $t(rdu_fpga_deskew_s) + $mp_setup_slack]
		set t(rdu_external_deskew_h) [expr $deskew_hold  - $t(rdu_fpga_deskew_h) + $mp_hold_slack]
		#######################################

		# Consider errors in the dynamic deskew
		set t1_quantization $IP(quantization_T1)
		set setup_slack [expr $setup_slack - $t1_quantization]
		set hold_slack  [expr $hold_slack - $t1_quantization]
		if { $debug } {
			puts "	$setup_slack $hold_slack"
		}
		lappend rc_summary [list "  Quantization error" [hps_sdram_p0_format_3dp [expr 0-$t1_quantization]] [hps_sdram_p0_format_3dp [expr 0-$t1_quantization]]]
		
		# Consider variation in the delay chains used during dynamic deksew
		set dqs_period [ hps_sdram_p0_get_dqs_period $pins(dqs_pins) ]
		set offset_from_90 [hps_sdram_p0_get_acv_read_offset $period $dqs_phase $dqs_period]
		if {$IP(num_ranks) == 1} {
			set t1_variation [expr [min [expr $offset_from_90 + [max [expr $MP(DQSQ)*$t(DQSQ)] [expr $MP(QH_time)*(0.5*$period - $t(QH_time))]] + 2*$board(intra_DQS_group_skew) + $max_package_skew + $fpga(tDQS_PSERR)] [max $max_read_deskew_setup $max_read_deskew_hold]]*2*$t1_vt_variation_percent*0.75]
		} else {
			set t1_variation [expr [min [expr $offset_from_90 + 2*$board(intra_DQS_group_skew) + $max_package_skew + $fpga(tDQS_PSERR)] [max $max_read_deskew_setup $max_read_deskew_hold]]*2*$t1_vt_variation_percent*0.75]
		}
		if {($dqs_period < 1.250) && ($family == "arria v")} {
			set speedgrade [string trim [string range [get_speedgrade_string] 0 0]]
			if {$speedgrade == 6} {
				set further_dqs_pserr 0.025
				set t1_variation [expr $t1_variation + $further_dqs_pserr]
			}
		}
		set setup_slack [expr $setup_slack - $t1_variation]
		set hold_slack  [expr $hold_slack - $t1_variation]	
		if { $debug } {
			puts "	$setup_slack $hold_slack"
		}
		lappend rc_summary [list "  Calibration uncertainty" [hps_sdram_p0_format_3dp [expr 0-$t1_variation]] [hps_sdram_p0_format_3dp [expr 0-$t1_variation]]]
		
		#######################################
		# Find values for uncertainty table
		set uncertainty_reporting [get_ini_var -name "qsta_enable_uncertainty_ddr_reporting"]
		if {[string equal -nocase $uncertainty_reporting on]} {
			set t(rdu_calibration_uncertaintyerror_s) [expr 0 - $t1_variation - $t1_quantization]
			set t(rdu_calibration_uncertaintyerror_h) [expr 0 - $t1_variation - $t1_quantization]
			set t(rdu_fpga_uncertainty_s) [expr $t(CK)/4 - $default_setup_slack - $t(rdu_input_max_delay_external)]
			set t(rdu_fpga_uncertainty_h) [expr $t(CK)/4 - $default_hold_slack  - $t(rdu_input_min_delay_external)]
			set t(rdu_extl_uncertainty_s) [expr $t(rdu_input_max_delay_external)]
			set t(rdu_extl_uncertainty_h) [expr $t(rdu_input_min_delay_external)]		
		}
		#######################################
		
	} else {
		set pessimism_setup [expr $setup_slack - $default_setup_slack - $mp_setup_slack]
		set pessimism_hold  [expr $hold_slack - $default_hold_slack - $mp_hold_slack]
		lappend rc_summary [list "  Spatial correlation pessimism removal" [hps_sdram_p0_format_3dp $pessimism_setup] [hps_sdram_p0_format_3dp $pessimism_hold]] 
		
		#######################################
		# Find values for uncertainty table
		set uncertainty_reporting [get_ini_var -name "qsta_enable_uncertainty_ddr_reporting"]
		if {[string equal -nocase $uncertainty_reporting on]} {
			set t(rdu_fpga_deskew_s) 0
			set t(rdu_fpga_deskew_h) 0
			set t(rdu_external_deskew_s) 0
			set t(rdu_external_deskew_h) 0
			set t(rdu_calibration_uncertaintyerror_s) 0
			set t(rdu_calibration_uncertaintyerror_h) 0
			set t(rdu_fpga_uncertainty_s) [expr $t(CK)/4 - $default_setup_slack - $t(rdu_input_max_delay_external) - $pessimism_setup]
			set t(rdu_fpga_uncertainty_h) [expr $t(CK)/4 - $default_hold_slack  - $t(rdu_input_min_delay_external) - $pessimism_hold]
			set t(rdu_extl_uncertainty_s) [expr $t(rdu_input_max_delay_external)]
			set t(rdu_extl_uncertainty_h) [expr $t(rdu_input_min_delay_external)]				
		}
		#######################################
	}
	
	#######################################
	# Create the read analysis panel	
	set panel_name "$inst Read Capture"
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
	
	if {($setup_slack < 0) || ($hold_slack <0)} {
		set panel_id [create_report_panel -table $panel_name -color red]
	} else {
		set panel_id [create_report_panel -table $panel_name]
	}	
	add_row_to_table -id $panel_id [list "Operation" "Setup Slack" "Hold Slack"]	
	
	if {$IP(read_deskew_mode) == "dynamic"} {
		set fcolour [hps_sdram_p0_get_colours $setup_slack $hold_slack] 
		add_row_to_table -id $panel_id [list "After Calibration Read Capture" [hps_sdram_p0_format_3dp $setup_slack] [hps_sdram_p0_format_3dp $hold_slack]] -fcolor $fcolour
		lappend summary [list $opcname 0 "Read Capture ($opcname)" $setup_slack $hold_slack]
	} else {
		set fcolour [hps_sdram_p0_get_colours $setup_slack $hold_slack] 
		add_row_to_table -id $panel_id [list "Read Capture" [hps_sdram_p0_format_3dp $setup_slack] [hps_sdram_p0_format_3dp $hold_slack]] -fcolor $fcolour
		lappend summary [list $opcname 0 "Read Capture ($opcname)" $setup_slack $hold_slack]  
	}	
	
	foreach summary_line $rc_summary {
		add_row_to_table -id $panel_id $summary_line -fcolors $positive_fcolour
	}	
	
	#######################################
	# Create the Read uncertainty panel
	set uncertainty_reporting [get_ini_var -name "qsta_enable_uncertainty_ddr_reporting"]
	if {[string equal -nocase $uncertainty_reporting on]} {
		set panel_name "$inst Read Capture Uncertainty"
		set root_folder_name [get_current_timequest_report_folder]

		if { ! [string match "${root_folder_name}*" $panel_name] } {
			set panel_name "${root_folder_name}||$panel_name"
		}

		# Delete any pre-existing summary panel
		set panel_id [get_report_panel_id $panel_name]
		if {$panel_id != -1} {
			delete_report_panel -id $panel_id
		}
		
		set panel_id [create_report_panel -table $panel_name]
		add_row_to_table -id $panel_id [list "Value" "Setup Side" "Hold Side"]
		add_row_to_table -id $panel_id [list "Uncertainty" "" ""]
		add_row_to_table -id $panel_id [list "  FPGA uncertainty" [hps_sdram_p0_format_3dp $t(rdu_fpga_uncertainty_s)] [hps_sdram_p0_format_3dp $t(rdu_fpga_uncertainty_h)]] 
		add_row_to_table -id $panel_id [list "  External uncertainty" [hps_sdram_p0_format_3dp $t(rdu_extl_uncertainty_s)] [hps_sdram_p0_format_3dp $t(rdu_extl_uncertainty_h)]] 
		add_row_to_table -id $panel_id [list "Deskew" "" ""]
		add_row_to_table -id $panel_id [list "  FPGA deskew" [hps_sdram_p0_format_3dp $t(rdu_fpga_deskew_s)] [hps_sdram_p0_format_3dp $t(rdu_fpga_deskew_h)]] 
		add_row_to_table -id $panel_id [list "  External deskew" [hps_sdram_p0_format_3dp $t(rdu_external_deskew_s)] [hps_sdram_p0_format_3dp $t(rdu_external_deskew_h)]] 
		add_row_to_table -id $panel_id [list "  Calibration uncertainty/error" [hps_sdram_p0_format_3dp $t(rdu_calibration_uncertaintyerror_s)] [hps_sdram_p0_format_3dp $t(rdu_calibration_uncertaintyerror_h)]] 
	}
}

#############################################################
# Other Timing Analysis
#############################################################

proc hps_sdram_p0_perform_phy_analyses {opcs opcname inst inst_controller pin_array_name timing_parameters_array_name summary_name IP_name} {

	###############################################################################
	# The PHY analysis concerns the timing requirements of the PHY which includes
	# soft registers in the FPGA core as well as some registers in the hard periphery
	# The read capture and write registers are not analyzed here, even though they 
	# are part of the PHY since they are timing analyzed separately. 
	###############################################################################

	#######################################
	# Need access to global variables
	upvar 1 $summary_name summary
	upvar 1 $timing_parameters_array_name t
	upvar 1 $pin_array_name pins
	upvar 1 $IP_name IP
	
	set num_failing_path $IP(num_report_paths)

	set entity_names_on [ hps_sdram_p0_are_entity_names_on ]

	set prefix [ string map "| |*:" $inst ]
	set prefix "*:$prefix"
	set prefix_controller [ string map "| |*:" $inst_controller ]
	set prefix_controller "*:$prefix_controller"

	if { ! $entity_names_on } {
		set core_regs [remove_from_collection [get_registers $inst|*] [get_registers $pins(read_capture_ddio)]]
	} else {
		set core_regs [remove_from_collection [get_registers $prefix|*] [get_registers $pins(read_capture_ddio)]]
	}

	# Core
	set res_0 [report_timing -detail full_path -to $core_regs -npaths $num_failing_path -panel_name "$inst Core (setup)" -setup]
	set res_1 [report_timing -detail full_path -to $core_regs -npaths $num_failing_path -panel_name "$inst Core (hold)" -hold]
	lappend summary [list $opcname 0 "Core ($opcname)" [lindex $res_0 1] [lindex $res_1 1] [lindex $res_0 0] [lindex $res_1 0]]

	# Core Recovery/Removal
	set res_0 [report_timing -detail full_path -to $core_regs -npaths $num_failing_path -panel_name "$inst Core Recovery/Removal (recovery)" -recovery]
	set res_1 [report_timing -detail full_path -to $core_regs -npaths $num_failing_path -panel_name "$inst Core Recovery/Removal (removal)" -removal]
	lappend summary [list $opcname 0 "Core Recovery/Removal ($opcname)" [lindex $res_0 1] [lindex $res_1 1] [lindex $res_0 0] [lindex $res_1 0]]
	

}

proc hps_sdram_p0_perform_ac_analyses {opcs opcname inst scale_factors_name pin_array_name timing_parameters_array_name summary_name IP_name} {

	###############################################################################
	# The adress/command analysis concerns the timing requirements of the pins (other
	# than the D/Q pins) which go to the memory device/DIMM.  These include address/command
	# pins, some of which are runing at Single-Data-Rate (SDR) and some which are 
	# running at Half-Data-Rate (HDR).  
	###############################################################################
	
	#######################################
	# Need access to global variables
	upvar 1 $summary_name summary
	upvar 1 $timing_parameters_array_name t
	upvar 1 $pin_array_name pins
	upvar 1 $IP_name IP
	upvar 1 $scale_factors_name scale_factors
	set eol_reduction_factor $IP(eol_reduction_factor_addr)

	set num_failing_path $IP(num_report_paths)

	set add_pins $pins(add_pins)
	set ba_pins $pins(ba_pins)
	set cmd_pins $pins(cmd_pins)
	set reset_pins $pins(reset_pins)
	set ac_pins [ concat $add_pins $ba_pins $cmd_pins ]

	set entity_names_on [ hps_sdram_p0_are_entity_names_on ]

	set prefix [ string map "| |*:" $inst ]
	set prefix "*:$prefix"
	
	set panel_name_setup  "Before Extra Common Clock Pessimism Removal \u0028Negative slacks are OK\u0029||$inst Address Command (setup)"
	set panel_name_hold   "Before Extra Common Clock Pessimism Removal \u0028Negative slacks are OK\u0029||$inst Address Command (hold)"

	set panel_name_dqs_setup  "Before Extra Common Clock Pessimism Removal \u0028Negative slacks are OK\u0029||$inst DQS vs CK (setup)"
	set panel_name_dqs_hold   "Before Extra Common Clock Pessimism Removal \u0028Negative slacks are OK\u0029||$inst DQS vs CK (hold)"

	######################################################################
	##Default Address Command Analysis
	set res_0 [report_timing -detail full_path -to $ac_pins \
		-npaths $num_failing_path -panel_name $panel_name_setup -setup -disable_panel_color -quiet]
	set res_1 [report_timing -detail full_path -to $ac_pins \
		-npaths $num_failing_path -panel_name $panel_name_hold -hold -disable_panel_color -quiet]
	set default_setup_slack [lindex $res_0 1]
	set default_hold_slack [lindex $res_1 1]
	
	# Perform the default A/C timing analysis to get required and arrival times
	set paths_setup [get_timing_paths -to [get_ports $ac_pins] -npaths 400 -setup -nworst 1]
	set paths_hold  [get_timing_paths -to [get_ports $ac_pins] -npaths 400 -hold  -nworst 1]
	
	######################################################################
	##Default DQS vs CK Analysis
	set dqs_res_0 [report_timing -detail full_path -to [get_ports $pins(dqs_pins)] -npaths $num_failing_path -panel_name  $panel_name_dqs_setup -setup -disable_panel_color -quiet]
	set dqs_res_1 [report_timing -detail full_path -to [get_ports $pins(dqs_pins)] -npaths $num_failing_path -panel_name $panel_name_dqs_hold -hold -disable_panel_color -quiet]
	set default_dqs_setup_slack [lindex $dqs_res_0 1]
	set default_dqs_hold_slack [lindex $dqs_res_1 1]

	# Perform the default DQS vs CK timing analysis to get required and arrival times
	set dqs_paths_setup [get_timing_paths -to [get_ports $pins(dqs_pins)] -npaths 400 -setup -nworst 1]
	set dqs_paths_hold  [get_timing_paths -to [get_ports $pins(dqs_pins)] -npaths 400 -hold  -nworst 1]
	
	######################################################################
	##Find portions of common clock pessimism removal
	
	# Some regular expression matching to create the right strings
	set pll_clock $pins(pll_write_clock)
	
	set ck_periphery_nodes [list]
	foreach ck_pin $pins(ck_pins) {
		set clk_clk_phase_select_node [hps_sdram_p0_traverse_to_clock_phase_select $ck_pin msg_list]
		set clk_periphery_node ${clk_clk_phase_select_node}|clkout
		lappend clk_periphery_nodes $clk_periphery_node
	}
	set clk_periphery_nodes [lsort -unique $clk_periphery_nodes]	
	set clk_max [hps_sdram_p0_max_in_collection [get_path -rise_from $pll_clock -rise_to $clk_periphery_nodes] "arrival_time"]
	if {$clk_max <= 0} {set clk_max -9999}
	set clk_min [hps_sdram_p0_max_in_collection [get_path -rise_from $pll_clock -rise_to $clk_periphery_nodes -min_path] "arrival_time"]
	if {$clk_min <= 0} {set clk_min 9999}
	
	set CKperiphery_min [get_path -rise_from $clk_periphery_nodes -rise_to $pins(ck_pins) -min_path -nworst 1]
	set CKperiphery_max [get_path -rise_from $clk_periphery_nodes -rise_to $pins(ck_pins) -nworst 1]
	set CKperiphery_min_delay [hps_sdram_p0_min_in_collection $CKperiphery_min "arrival_time"]
	set CKperiphery_max_delay [hps_sdram_p0_max_in_collection $CKperiphery_max "arrival_time"]
	set aiot_delay [hps_sdram_p0_round_3dp [expr [hps_sdram_p0_get_rise_aiot_delay [lindex $pins(ck_pins) 0]] * 1e9]]
	set CKperiphery_min_delay [expr $CKperiphery_min_delay - $aiot_delay]
	set CKperiphery_max_delay [expr $CKperiphery_max_delay - $aiot_delay]
	set CKpath_pessimism  [expr $CKperiphery_min_delay*($scale_factors(eol) - $scale_factors(eol)/$eol_reduction_factor) +  + ($CKperiphery_max_delay - $CKperiphery_min_delay)/2]
	
	######################################################################
	##Loop over AC pins, and perform analysis
	
	set pll_clock $pins(pll_write_clock)
	# The A/C CPS is set to inversion mode, so we require a RF path
	set rise_or_fall_to "fall_to"

	set setup_slack 1000000000
	set hold_slack  1000000000	
	foreach ac_pin $ac_pins {
			
		set pin_setup_slack [hps_sdram_p0_min_in_collection_to_name $paths_setup "slack" $ac_pin]
		set pin_hold_slack  [hps_sdram_p0_min_in_collection_to_name $paths_hold  "slack" $ac_pin]
		set current_ccpr    [hps_sdram_p0_min_in_collection_to_name $paths_hold  "ccpp" $ac_pin]
		
		set ac_clk_phase_select_node [hps_sdram_p0_traverse_to_clock_phase_select $ac_pin msg_list]
		set ac_periphery_node ${ac_clk_phase_select_node}|clkout 

		set ac_max  [hps_sdram_p0_max_in_collection [get_path -rise_from $pll_clock -${rise_or_fall_to} $ac_periphery_node] "arrival_time"]
		if {$ac_max <= 0} {set ac_max -9999}
		set ac_min  [hps_sdram_p0_max_in_collection [get_path -rise_from $pll_clock -${rise_or_fall_to} $ac_periphery_node -min_path] "arrival_time"]
		if {$ac_min <= 0} {set ac_min 9999}
		set extra_ccpr [hps_sdram_p0_round_3dp [expr ($clk_max - $clk_min + $ac_max - $ac_min)/2.0]]
		
		set pin_setup_slack [expr $pin_setup_slack + $extra_ccpr]
		set pin_hold_slack [expr $pin_hold_slack + $extra_ccpr]
		
		set ACperiphery_min [get_path -rise_from $ac_periphery_node -rise_to $ac_pin -min_path -nworst 1 ]
		set ACperiphery_max [get_path -rise_from $ac_periphery_node -rise_to $ac_pin  -nworst 1]
		set ACperiphery_min_delay [hps_sdram_p0_min_in_collection $ACperiphery_min "arrival_time"]
		set ACperiphery_max_delay [hps_sdram_p0_max_in_collection $ACperiphery_max "arrival_time"]
		set aiot_delay [hps_sdram_p0_round_3dp [expr [hps_sdram_p0_get_rise_aiot_delay [lindex $ac_pin 0]] * 1e9]]
		set ACperiphery_min_delay [expr $ACperiphery_min_delay - $aiot_delay]
		set ACperiphery_max_delay [expr $ACperiphery_max_delay - $aiot_delay]		
		set ACpath_pessimism  [expr $ACperiphery_min_delay*($scale_factors(eol) - $scale_factors(eol)/$eol_reduction_factor) + ($ACperiphery_max_delay - $ACperiphery_min_delay)/2]
		
		set pin_setup_slack [expr $pin_setup_slack + $ACpath_pessimism]
		set pin_hold_slack [expr $pin_hold_slack + $CKpath_pessimism]
	
		set setup_slack [min $setup_slack $pin_setup_slack]
		set hold_slack  [min $hold_slack $pin_hold_slack]		
		
	}
	
	########################################
	########################################
	##Create the a/c analysis panel	
	set ac_summary [list]
	lappend ac_summary [list "  Standard Address Command" [hps_sdram_p0_format_3dp $default_setup_slack] [hps_sdram_p0_format_3dp $default_hold_slack]]
	lappend ac_summary [list "  Extra common clock and spatial correlation pessimism removal" [hps_sdram_p0_format_3dp [expr $setup_slack - $default_setup_slack]] [hps_sdram_p0_format_3dp [expr $hold_slack - $default_hold_slack]]]

	set positive_fcolour [list "black" "blue" "blue"]
	set negative_fcolour [list "black" "red"  "red"]
	set panel_name "$inst Address Command"
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
	
	if {($setup_slack < 0) || ($hold_slack <0)} {
		set panel_id [create_report_panel -table $panel_name -color red]
	} else {
		set panel_id [create_report_panel -table $panel_name]
	}
	add_row_to_table -id $panel_id [list "Operation" "Setup Slack" "Hold Slack"] 		
	
	set fcolour [hps_sdram_p0_get_colours $setup_slack $hold_slack] 
	add_row_to_table -id $panel_id [list "Address Command" [hps_sdram_p0_format_3dp $setup_slack] [hps_sdram_p0_format_3dp $hold_slack]] -fcolor $fcolour
	lappend summary [list $opcname 0 "Address Command ($opcname)" [hps_sdram_p0_format_3dp $setup_slack] [hps_sdram_p0_format_3dp $hold_slack]]

	foreach summary_line $ac_summary {
		add_row_to_table -id $panel_id $summary_line -fcolors $positive_fcolour
	}

	######################################################################
	##Loop over DQS pins, and perform analysis
	
	set pll_clock $pins(pll_write_clock)

	set dqs_setup_slack 1000000000
	set dqs_hold_slack  1000000000	
	foreach dqs_pin $pins(dqs_pins) {
			
		set dqs_pin_setup_slack [hps_sdram_p0_min_in_collection_to_name $dqs_paths_setup "slack" $dqs_pin]
		set dqs_pin_hold_slack  [hps_sdram_p0_min_in_collection_to_name $dqs_paths_hold  "slack" $dqs_pin]
		
		set dqs_clk_phase_select_node [hps_sdram_p0_traverse_to_clock_phase_select $dqs_pin msg_list]
		set dqs_periphery_node ${dqs_clk_phase_select_node}|clkout 

		set dqs_max  [hps_sdram_p0_max_in_collection [get_path -rise_from $pll_clock -rise_to $dqs_periphery_node] "arrival_time"]
		if {$dqs_max <= 0} {set dqs_max -9999}
		set dqs_min  [hps_sdram_p0_max_in_collection [get_path -rise_from $pll_clock -rise_to $dqs_periphery_node -min_path] "arrival_time"]
		if {$dqs_min <= 0} {set dqs_min 9999}

		set extra_ccpr [hps_sdram_p0_round_3dp [expr ($clk_max - $clk_min + $dqs_max - $dqs_min)/2.0]]
		
		set dqs_pin_setup_slack [expr $dqs_pin_setup_slack + $extra_ccpr]
		set dqs_pin_hold_slack [expr $dqs_pin_hold_slack + $extra_ccpr]
		
		set dqs_setup_slack [min $dqs_setup_slack $dqs_pin_setup_slack]
		set dqs_hold_slack  [min $dqs_hold_slack $dqs_pin_hold_slack]		
	}
	
	########################################
	########################################
	##Create the DQS vs CK analysis panel	

	set dqs_summary [list]

	lappend dqs_summary [list "  Standard DQS vs CK" [hps_sdram_p0_format_3dp $default_dqs_setup_slack] [hps_sdram_p0_format_3dp $default_dqs_hold_slack]]
	lappend dqs_summary [list "  Extra common clock pessimism removal" [hps_sdram_p0_format_3dp [expr $dqs_setup_slack - $default_dqs_setup_slack]] [hps_sdram_p0_format_3dp [expr $dqs_hold_slack - $default_dqs_hold_slack]]]
	
	set dqs_panel_name "$inst DQS vs CK"

	if { ! [string match "${root_folder_name}*" $dqs_panel_name] } {
		set dqs_panel_name "${root_folder_name}||$dqs_panel_name"
	}

	# Delete any pre-existing summary panel
	set dqs_panel_id [get_report_panel_id $dqs_panel_name]
	if {$dqs_panel_id != -1} {
		delete_report_panel -id $dqs_panel_id
	}
	
	if {($dqs_setup_slack < 0) || ($dqs_hold_slack <0)} {
		set dqs_panel_id [create_report_panel -table $dqs_panel_name -color red]
	} else {
		set dqs_panel_id [create_report_panel -table $dqs_panel_name]
	}
	add_row_to_table -id $dqs_panel_id [list "Operation" "Setup Slack" "Hold Slack"] 		
	
	set fcolour [hps_sdram_p0_get_colours $dqs_setup_slack $dqs_hold_slack] 
	add_row_to_table -id $dqs_panel_id [list "DQS vs CK" [hps_sdram_p0_format_3dp $dqs_setup_slack] [hps_sdram_p0_format_3dp $dqs_hold_slack]] -fcolor $fcolour
	lappend summary [list $opcname 0 "DQS vs CK ($opcname)" [hps_sdram_p0_format_3dp $dqs_setup_slack] [hps_sdram_p0_format_3dp $dqs_hold_slack]]

	foreach summary_line $dqs_summary {
		add_row_to_table -id $dqs_panel_id $summary_line -fcolors $positive_fcolour
	}
}


#############################################################
# Bus Turnaround Time Analysis
#############################################################

proc hps_sdram_p0_perform_flexible_bus_turnaround_time_analysis {opcs opcname instname family period dll_length interface_type tJITper tJITdty tDCD pll_steps pin_array_name timing_parameters_array_name summary_name  MP_name IP_name SSN_name board_name ISI_parameters_name} {

	###############################################################################
	# The bus-turnaround time analysis concerns making sure there is no contention on
	# on the DQ bus when a read command is followed by a write command.  When a read
	# command is issued, some cycles later the memory takes control of the DQS bus and
	# starts sending back data to the controller.  If the controller issues a write 
	# command too early then the read command data may not have fully read and there 
	# may be contention on the bus.  This analysis determines how much margin there 
	# is on the switchover time and if the slack is negative, either the controller's 
	# bus turnaround time must be increased (which reduces effeciency), or the 
	# absolute delays on the board traces must be reduced.
	###############################################################################

	#######################################
	# Need access to global variables
	upvar 1 $summary_name summary
	upvar 1 $timing_parameters_array_name t
	upvar 1 $IP_name IP
	upvar 1 $pin_array_name pins
	upvar 1 $board_name board	
	upvar 1 $MP_name MP
	upvar 1 $SSN_name SSN
	upvar 1 $ISI_parameters_name ISI
	
	# Derived parameters
	if {$t(DWIDTH_RATIO) == 2} {
		set burst_length 4
	} elseif {$t(DWIDTH_RATIO) == 4} {
		set burst_length 8
	} else {
		set burst_length 16
	}

	######################################################################
	# Find the maximum delay of the CK issuing a read command followed by 
	# read data coming back
	
	# Maximum clock delay
	set ac_hold   [get_timing_paths -to $pins(add_pins) -hold -npaths 100]
	set max_dly [expr [hps_sdram_p0_max_in_collection $ac_hold "clock_skew"]]
	
	# SSO and Jitter pushout on clock
	set max_dly [expr $max_dly + $SSN(pushout_o) + $tJITper/2]
	
	# CK Board delay
	set max_dly [expr $max_dly + $board(abs_max_CK_delay)]
	
	# Read Latency and Burst Lenght
	set max_dly [expr $max_dly + ($t(RL) + $burst_length/2)*$t(CK)]
	
	# DQS Board delay
	set max_dly [expr $max_dly + $board(abs_max_DQS_delay)]
	
	# Time for DQS to go high impedance relative to the CK
	set max_dly [expr $max_dly + $t(DQSCK)] 
	
	# SSI pushout on DQS
	set max_dly [expr $max_dly + $SSN(pushout_i)]
	
	######################################################################
	# Find the minimum delay of the issuing a write command after read
	# command has been issued and the FPGA taking hold of the DQS trace
	
	# Because of the levelling operation we assume that the output delay of 
	# the clock is the same as the output delay of the write (other than 
	# board delays and transient delays
	set ac_setup   [get_timing_paths -to $pins(add_pins) -setup -npaths 100]
	set min_dly [expr [hps_sdram_p0_min_in_collection $ac_setup "clock_skew"]]

	# SSO pullin on write data
	set min_dly [expr $min_dly - $SSN(pullin_o)]
	
	# Jitter and other effects on write data
	set min_dly [expr $min_dly - $t(WL_DCD) - $t(WL_JITTER) - $t(WL_PSE)]
	
	# Quantization error on levelling
	set min_dly [expr $min_dly - $IP(quantization_WL)]
	
	# Difference in board delay
	set min_dly [expr $min_dly - $board(minCK_DQS_skew)]
	
	# Delay between the read command and write command 
	set num_clocks_read_to_write [expr $t(RL) - $t(WL) + $burst_length/2 + 2 + $t(rd_to_wr_turnaround_oct)]
	set min_dly [expr $min_dly + $num_clocks_read_to_write*$t(CK)]
	
	# Delay between write command and write data on the bus
	set min_dly [expr $min_dly + $t(WL)*$t(CK)]
	
	# Adjustment for when the DQS preamble is driven
	set min_dly [expr $min_dly - $t(CK)]
	
	# Adjustment for when the OCT is enabled (one cycle berfore DQS preamble) 
	set min_dly [expr $min_dly - $t(CK)]	

	set setup_slack [expr $min_dly - $max_dly]
	set hold_slack "--"

	lappend summary [list $opcname 0 "Bus Turnaround Time ($opcname)" $setup_slack $hold_slack]
	
}


proc hps_sdram_p0_perform_resync_timing_analysis {opcs opcname inst fbasename family scale_factors_name io_std interface_type period pin_array_name timing_parameters_array_name summary_name MP_name IP_name board_name fpga_name SSN_name} {

	###############################################################################
	# The resynchronization timing analysis concerns transferring read data that
	# has been captured with a DQS strobe to a clock domain under the control of
	# the UniPHY. A special FIFO is used to resynchronize the data which has a wide
	# tolerance to any changes in the arrival time of data from DQS groups
	###############################################################################

	#######################################
	# Need access to global variables
	upvar 1 $summary_name summary
	upvar 1 $timing_parameters_array_name t
	upvar 1 $pin_array_name pins
	upvar 1 $MP_name MP
	upvar 1 $IP_name IP
	upvar 1 $board_name board
	upvar 1 $fpga_name fpga
	upvar 1 $SSN_name SSN
	upvar 1 $scale_factors_name scale_factors
	
	set num_paths 5000

	set prefix [ string map "| |*:" $inst ]
	set prefix "*:$prefix"

	lappend summary [list $opcname 0 "Read Resync ($opcname)" 1.000 1.000]

	return
	#######################################
	# Node names
	set dqs_pins $pins(dqs_pins)
	set fifo ${prefix}*${fbasename}_flop_mem:read_buffering[*].read_subgroup[*].uread_fifo|data_stored[*][*]
	set reg_in_rd_clk_domain ${prefix}*${fbasename}_flop_mem:read_buffering[*].read_subgroup[*].uread_fifo|rd_data[*]
	set reg_wr_address ${prefix}*${fbasename}_read_datapath:uread_datapath|read_buffering[*].read_subgroup[*].wraddress[*]
	set reg_rd_address ${prefix}*${fbasename}_read_datapath:uread_datapath|read_buffering[*].read_subgroup[*].rdaddress[*]
	
	#######################################
	# Paths
	set max_DQS_to_fifo_paths  [get_path -from $dqs_pins -to $fifo -npaths $num_paths -nworst 1]
	set min_DQS_to_fifo_paths  [get_path -from $dqs_pins -to $fifo -npaths $num_paths -min_path  -nworst 1]
	
	set max_fifo_to_rd_clk_domain_paths [get_path -from $fifo -to $reg_in_rd_clk_domain -npaths $num_paths  -nworst 1]
	set min_fifo_to_rd_clk_domain_paths [get_path -from $fifo -to $reg_in_rd_clk_domain -npaths $num_paths -min_path  -nworst 1]
	
	set max_DQS_to_wr_address_paths [get_path -from $dqs_pins -to $reg_wr_address -npaths $num_paths -nworst 1]
	set min_DQS_to_wr_address_paths [get_path -from $dqs_pins -to $reg_wr_address -npaths $num_paths -min_path  -nworst 1]
	
	set max_rd_address_to_rd_data_paths [get_path -from $reg_rd_address -to $reg_in_rd_clk_domain -npaths $num_paths -nworst 1]
	set min_rd_address_to_rd_data_paths [get_path -from $reg_rd_address -to $reg_in_rd_clk_domain -npaths $num_paths -min_path -nworst 1]
	
	set max_dqs_common_to_fifo [hps_sdram_p0_max_in_collection [get_path -from $dqs_pins -to $fifo -nworst 1] "arrival_time"]
	
	#########################################
	# Limit to one endpoint/startpoint
	
	foreach_in_collection path $max_DQS_to_fifo_paths {
		set arrival_time [get_path_info $path -arrival_time]
		set startpoint [get_node_info -name [get_path_info $path -from]]
		if {[info exist max_DQS_to_fifo_paths_max($startpoint)]} {
			if {$arrival_time > $max_DQS_to_fifo_paths_max($startpoint)} {
				set max_DQS_to_fifo_paths_max($startpoint) $arrival_time
			}
		} else {
			set max_DQS_to_fifo_paths_max($startpoint) $arrival_time
		}
	}
	
	foreach_in_collection path $min_DQS_to_fifo_paths {
		set arrival_time [get_path_info $path -arrival_time]
		set startpoint [get_node_info -name [get_path_info $path -from]]
		if {[info exist min_DQS_to_fifo_paths_min($startpoint)]} {
			if {$arrival_time < $min_DQS_to_fifo_paths_min($startpoint)} {
				set min_DQS_to_fifo_paths_min($startpoint) $arrival_time
			}
		} else {
			set min_DQS_to_fifo_paths_min($startpoint) $arrival_time
		}
	}	

	
	foreach_in_collection path $max_fifo_to_rd_clk_domain_paths {
		set arrival_time [get_path_info $path -arrival_time]
		set endpoint [get_node_info -name [get_path_info $path -to]]
		if {[info exist max_fifo_to_rd_clk_domain_paths_max($endpoint)]} {
			if {$arrival_time > $max_fifo_to_rd_clk_domain_paths_max($endpoint)} {
				set max_fifo_to_rd_clk_domain_paths_max($endpoint) $arrival_time
			}
		} else {
			set max_fifo_to_rd_clk_domain_paths_max($endpoint) $arrival_time
		}
	}
	
	foreach_in_collection path $min_fifo_to_rd_clk_domain_paths {
		set arrival_time [get_path_info $path -arrival_time]
		set endpoint [get_node_info -name [get_path_info $path -to]]
		if {[info exist min_fifo_to_rd_clk_domain_paths_min($endpoint)]} {
			if {$arrival_time < $min_fifo_to_rd_clk_domain_paths_min($endpoint)} {
				set min_fifo_to_rd_clk_domain_paths_min($endpoint) $arrival_time
			}
		} else {
			set min_fifo_to_rd_clk_domain_paths_min($endpoint) $arrival_time
		}
	}
	
	foreach_in_collection path $max_rd_address_to_rd_data_paths {
		set arrival_time [get_path_info $path -arrival_time]
		set endpoint [get_node_info -name [get_path_info $path -to]]
		if {[info exist max_rd_address_to_rd_data_paths_max($endpoint)]} {
			if {$arrival_time > $max_rd_address_to_rd_data_paths_max($endpoint)} {
				set max_rd_address_to_rd_data_paths_max($endpoint) $arrival_time
			}
		} else {
			set max_rd_address_to_rd_data_paths_max($endpoint) $arrival_time
		}
	}
	
	foreach_in_collection path $min_rd_address_to_rd_data_paths {
		set arrival_time [get_path_info $path -arrival_time]
		set endpoint [get_node_info -name [get_path_info $path -to]]
		if {[info exist min_rd_address_to_rd_data_paths_min($endpoint)]} {
			if {$arrival_time < $min_rd_address_to_rd_data_paths_min($endpoint)} {
				set min_rd_address_to_rd_data_paths_min($endpoint) $arrival_time
			}
		} else {
			set min_rd_address_to_rd_data_paths_min($endpoint) $arrival_time
		}
	}		
	
	#######################################
	# TCO times
	set i 0
	set tco_fifo_min 0
	set tco_fifo_max 0
	foreach_in_collection register [get_keepers $fifo] {
		set tcotemp [get_register_info $register -tco]
		if {$i == 0} {
			set tco_fifo_min $tcotemp
			set tco_fifo_max $tcotemp
		} else {
			if {$tcotemp < $tco_fifo_min} {
				set tco_fifo_min $tcotemp
			} elseif {$tcotemp > $tco_fifo_max} {
				set tco_fifo_max $tcotemp
			}
		}
		incr i
	}
	set i 0
	set tco_wr_address_min 0
	set tco_wr_address_max 0
	foreach_in_collection register [get_keepers $reg_wr_address] {
		set tcotemp [get_register_info $register -tco]
		if {$i == 0} {
			set tco_wr_address_min $tcotemp
			set tco_wr_address_max $tcotemp
		} else {
			if {$tcotemp < $tco_wr_address_min} {
				set tco_wr_address_min $tcotemp
			} elseif {$tcotemp > $tco_wr_address_max} {
				set tco_wr_addressmax $tcotemp
			}
		}
		incr i
	}
	
	#######################################
	# Other parameters
	set entity_names_on [ hps_sdram_p0_are_entity_names_on ]	
	set fly_by_wire 1.6
	set min_latency 1
	set max_latency 2
	if { ! $entity_names_on } {
		set fifo_depth [get_collection_size [get_keepers $inst*read_buffering[0].read_subgroup[0].uread_fifo|data_stored[*][0]]]	
	} else {
		set fifo_depth [get_collection_size [get_keepers $prefix*read_buffering[0].read_subgroup[0].uread_fifo|data_stored[*][0]]]	
	}	
	if {($IP(mp_calibration) == 1) && ($IP(num_ranks) == 1)} {
		# Reduce the effect of tDQSCK
		set mp_DQSCK [expr $MP(DQSCK)*$t(DQSCK)]
	} else {
		set mp_DQSCK 0
	}
	set hf_DQS_variation [expr [get_micro_node_delay -micro MEM_CK_PERIOD_JITTER -parameters [list IO PHY_SHORT] -in_fitter -period $period]/1000.0*2/2]
	set hf_DQS_variation [expr $hf_DQS_variation + $SSN(pushout_o) + $SSN(pullin_o) + 2*$t(DQSCK) - 2*$mp_DQSCK + $SSN(pullin_i)]
	set hf_DQS_variation [expr $hf_DQS_variation + [get_float_table_node_delay -src {DELAYCHAIN_T9} -dst {VTVARIATION} -parameters [list IO $interface_type]]*$max_dqs_common_to_fifo/2]
	
	#######################################
	# Board parameters
	set board_skew [expr $board(inter_DQS_group_skew)/2.0]
	if {$IP(num_ranks) > 1} {
		set board_skew [expr $board_skew + $board(tpd_inter_DIMM)]
	}	

	#######################################
	# Body of Resync analysis
	# Go over each DQ pin

	set total_setup_slack 10000000
	set total_hold_slack  10000000
	
	set regs [get_keepers $reg_in_rd_clk_domain]

	foreach_in_collection reg $regs {

		set reg_name [get_register_info -name $reg]

		if {[info exists max_rd_address_to_rd_data_paths_max($reg_name)]==0} {
			# not all registers have arcs for the hard read fifo, depending upon full/half rate
			continue
		}
		
		regexp {read_buffering\[(\d+)\]\.read_subgroup} $reg_name match dqs_group_number

		set dqs_pin [lindex $pins(dqs_pins) $dqs_group_number]
      if {!([info exists max_DQS_to_fifo_paths_max($dqs_pin)] &&
		     [info exists min_DQS_to_fifo_paths_min($dqs_pin)] &&
		     [info exists max_fifo_to_rd_clk_domain_paths_max($reg_name_fifo_data_rd_clk_domain)] &&
		     [info exists min_fifo_to_rd_clk_domain_paths_min($reg_name_fifo_data_rd_clk_domain)] &&
		     [info exists max_rd_address_to_rd_data_paths_max($reg_name)] &&
		     [info exists min_rd_address_to_rd_data_paths_min($reg_name)])} {
         post_message -type error "Paths not found for resync analysis."
         return 1
      }
		set max_DQS_to_fifo $max_DQS_to_fifo_paths_max($dqs_pin)
		set min_DQS_to_fifo $min_DQS_to_fifo_paths_min($dqs_pin)
		set max_fifo_to_rd_clk_domain $max_fifo_to_rd_clk_domain_paths_max($reg_name)
		set min_fifo_to_rd_clk_domain $min_fifo_to_rd_clk_domain_paths_min($reg_name)
		set max_rd_address_to_rd_data $max_rd_address_to_rd_data_paths_max($reg_name)
		set min_rd_address_to_rd_data $min_rd_address_to_rd_data_paths_min($reg_name)


		###############
		# Setup analysis	
		set setup_arrival_time  [expr ($max_DQS_to_fifo - $min_DQS_to_fifo) + $tco_fifo_max + $max_fifo_to_rd_clk_domain]
		set setup_required_time [expr $min_latency*$period + $tco_wr_address_min + $min_rd_address_to_rd_data]
		set setup_slack [expr $setup_required_time - $setup_arrival_time - $board_skew]

		###############
		# Hold analysis
		set hold_arrival_time  [expr ($min_DQS_to_fifo - $max_DQS_to_fifo) + $tco_fifo_min + $min_fifo_to_rd_clk_domain + $fifo_depth*$period]
		set hold_required_time [expr $hf_DQS_variation + $max_rd_address_to_rd_data + $tco_wr_address_max + $max_latency*$period  + $fly_by_wire]	
		set hold_slack [expr -$hold_required_time + $hold_arrival_time - $board_skew]

		if {$setup_slack < $total_setup_slack} {
			set total_setup_slack $setup_slack
		}
		
		if {$hold_slack < $total_hold_slack} {
			set total_hold_slack $hold_slack
		}				
	}
	
	lappend summary [list $opcname 0 "Read Resync ($opcname)" $total_setup_slack $total_hold_slack]

}

#############################################################
# Postamble Timing Analysis
#############################################################

proc hps_sdram_p0_perform_flexible_postamble_timing_analysis {opcs opcname instname scale_factors_name family period dll_length interface_type tJITper tJITdty tDCD DQSpathjitter pin_array_name timing_parameters_array_name summary_name  MP_name IP_name SSN_name board_name ISI_parameters_name} {

	###############################################################################
	# The postamble analysis concerns gating the DQS signal within the device
	# in the allotated time before DQS goes tri-state, such that spurious data is not
	# registered if there is noise on DQS. Altera devices have dedicated register that
	# can be used to ground the DQS signal, and the timing analysis below considers
	# the timing margins for the clock that is used to reset that register.
	###############################################################################
	

	#######################################
	# Need access to global variables
	upvar 1 $summary_name summary
	upvar 1 $timing_parameters_array_name t
	upvar 1 $IP_name IP
	upvar 1 $pin_array_name pins
	upvar 1 $board_name board	
	upvar 1 $MP_name MP
	upvar 1 $SSN_name SSN
	upvar 1 $ISI_parameters_name ISI
	upvar 1 $scale_factors_name scale_factors		
	
	# Ideal setup and hold slacks is half the postamble time (full clock cycle)
	set setup_slack [expr 0.5*$t(CK)]
	set hold_slack  [expr 0.5*$t(CK)]
	
	###############################
	# Memory Parameters
	###############################
	
	# Remove the variation in the clock coming back from the memory
	set tDQSCK $t(DQSCK)
	if {($IP(mp_calibration) == 1) && ($IP(num_ranks) == 1)} {
		if {$IP(tracking_enabled) == 1} {
			set tDQSCK [expr (1.0-($MP(DQSCK)+$MP(DQSCK_T)))*$tDQSCK]
		} else {
			set tDQSCK [expr (1.0-$MP(DQSCK))*$tDQSCK]
		}
	} elseif {$IP(mp_calibration) == 1} {
		set tDQSCK [expr (1.0-$MP(DQSCK)/2.5)*$tDQSCK]
	}

	set setup_slack [expr $setup_slack - $tDQSCK]
	set hold_slack  [expr $hold_slack  - $tDQSCK]
	
	##############################
	# Jitter/SSN Effects
	##############################

	# Remove the jitter on the clock out to the memory, and the DQS enable clock 
	set setup_slack [expr $setup_slack - $tJITper/2 - $tJITper/2]
	set hold_slack  [expr $hold_slack  - $tJITper/2 - $tJITper/2]
	
	# Remove the jitter on the DQS path
	set setup_slack [expr $setup_slack - $DQSpathjitter/2]
	set hold_slack  [expr $hold_slack  - $DQSpathjitter/2]
	
	# Remove SSN effects
	if {$IP(tracking_enabled) == 1} {
		set total_ssn [expr $SSN(pushout_o) + $SSN(pushout_i) + $SSN(pullin_o) + $SSN(pullin_i)]
		set setup_slack [expr $setup_slack - $total_ssn/2]
		set hold_slack  [expr $hold_slack  - $total_ssn/2]	
	} else {
		set setup_slack [expr $setup_slack - $SSN(pushout_o) - $SSN(pushout_i)]
		set hold_slack  [expr $hold_slack  - $SSN(pullin_o)  - $SSN(pullin_i)]	
	}

	##############################
	# VT/Aging effects
	##############################
	
	set coarse_delay [expr $t(CK)/$dll_length]	
	set dqsenableextend_regs [list *dq_ddio[*].ubidir_dq_dqs|*|dqs_enable_ctrl~DFFEXTENDDQSENABLE *dq_ddio[*].ubidir_dq_dqs|*|dqs_enable_ctrl~DQSENABLEOUT_DFF]
	if {$family == "arria v"} {
		set hps_compensation [expr 0.250 - [hps_sdram_p0_max_in_collection [get_path -rise_to ${instname}|pll|pll|clk_out[0]] "arrival_time"]]
	} else {
		set hps_compensation [expr 0.350 - [hps_sdram_p0_max_in_collection [get_path -rise_to ${instname}|pll|pll|clk_out[0]] "arrival_time"]]
	}
	# Clock out to DQS path coming back into the FPGA
	set mem_clock_delay_max [hps_sdram_p0_max_in_collection [get_path -rise_from $pins(pll_write_clock) -rise_to $pins(ck_pins)] "arrival_time"]
	set mem_clock_delay_min [hps_sdram_p0_min_in_collection [get_path -rise_from $pins(pll_write_clock) -rise_to $pins(ck_pins) -min_path] "arrival_time"]

	set mem_clock_delay_max [expr $mem_clock_delay_max + $hps_compensation]

	set dqs_delay_max  [hps_sdram_p0_max_in_collection [get_path -rise_from $pins(dqs_pins) -fall_to *POSTAMBLE_DFF] "arrival_time"]
	set dqs_delay_min  [hps_sdram_p0_min_in_collection [get_path -rise_from $pins(dqs_pins) -fall_to *POSTAMBLE_DFF -min_path] "arrival_time"]	
	set ck_pin_buffer_delay [hps_sdram_p0_round_3dp [expr [hps_sdram_p0_get_min_aiot_delay [lindex $pins(ck_pins) 0]] * 1e9]]

	set return_path_delay_max [expr $mem_clock_delay_max + $dqs_delay_max - ($ck_pin_buffer_delay*1.55)]
	set return_path_delay_min [expr $mem_clock_delay_min + $dqs_delay_min - ($ck_pin_buffer_delay*1.55)]

	# DQS Enable clk to DQS Enable register
	set clock_delay_max [hps_sdram_p0_max_in_collection [get_path -rise_from $pins(pll_write_clock) -rise_to $dqsenableextend_regs] "arrival_time"]
	set clock_delay_min [hps_sdram_p0_min_in_collection [get_path -rise_from $pins(pll_write_clock) -rise_to $dqsenableextend_regs -min_path] "arrival_time"]
	set clock_delay_max [expr $clock_delay_max + $hps_compensation]
	set t11_delay_max [hps_sdram_p0_max_in_collection [get_path -rise_from $dqsenableextend_regs -fall_to *POSTAMBLE_DFF] "arrival_time"]
	set t11_delay_min [hps_sdram_p0_min_in_collection [get_path -rise_from $dqsenableextend_regs -fall_to *POSTAMBLE_DFF -min_path] "arrival_time"]

	set t11_setting_delay_max_base [expr [get_integer_node_delay -integer 0 -parameters {DQS_ENABLE RC_RISE RC_RISE} -src DELAYCHAIN_T11 -in_fitter]/1000.0]
	set t11_setting_delay_max   [expr [get_integer_node_delay -integer 15 -parameters {DQS_ENABLE RC_RISE RC_RISE} -src DELAYCHAIN_T11 -in_fitter]/1000.0]
	set t11_setting_delay_max [expr {$t11_setting_delay_max - $t11_setting_delay_max_base}]
	set t11_setting_delay_min_base   [expr [get_integer_node_delay -integer 0 -parameters {DQS_ENABLE MIN RC_RISE RC_RISE} -src DELAYCHAIN_T11 -in_fitter]/1000.0]
	set t11_setting_delay_min   [expr [get_integer_node_delay -integer 15 -parameters {DQS_ENABLE MIN RC_RISE RC_RISE} -src DELAYCHAIN_T11 -in_fitter]/1000.0]
	set t11_setting_delay_min [expr {$t11_setting_delay_min - $t11_setting_delay_min_base}]

	set dqs_enable_signal_delay_max [expr $clock_delay_max + $t11_delay_max + $t11_setting_delay_max]
	set dqs_enable_signal_delay_min [expr $clock_delay_min + $t11_delay_min + $t11_setting_delay_min]

	if {($mem_clock_delay_max == 0) || ($clock_delay_max == 0) || ($dqs_delay_max == 0)} {
		lappend summary [list $opcname 0 "Postamble ($opcname)" -1.999 -1.999]
		return
	}

	# Determine the worst-case difference between the two paths
	set t11_delay_difference 0
	set t11_delay_difference [max $t11_delay_difference [expr {abs($return_path_delay_min - $dqs_enable_signal_delay_min)}]]
	set t11_delay_difference [max $t11_delay_difference [expr {abs($return_path_delay_min - $dqs_enable_signal_delay_max)}]]
	set t11_delay_difference [max $t11_delay_difference [expr {abs($return_path_delay_max - $dqs_enable_signal_delay_min)}]]
	set t11_delay_difference [max $t11_delay_difference [expr {abs($return_path_delay_max - $dqs_enable_signal_delay_max)}]]
	if {$t11_delay_difference < 0} {
		# If the DQS Enable path is longer, then any T11 setting picked for centering will make the delay mismatch worse
		set t11_delay_difference [expr {abs($t11_delay_difference) + $coarse_delay}]
	}

	# Determine worst-case V or VT variation as a % of delay
	set lf_vt_drift_derating 0.75
	if {$IP(tracking_enabled) == 1} {
		# Just V variation
		set vt_variation_percent [expr 2*[get_float_table_node_delay -src {DELAYCHAIN_DUTY_CYCLE} -dst {VTVARIATION} -parameters [list IO $interface_type]]]
	} else {
		# Both V and T variation
		#TODO, get T variation
		set vt_variation_percent [expr 0.10 + 2*[get_float_table_node_delay -src {DELAYCHAIN_DUTY_CYCLE} -dst {VTVARIATION} -parameters [list IO $interface_type]]]
	}
	
	# Determine aging variation as a % of delay
	set age_variation_derating 0.7
	set age_variation_percent [expr $scale_factors(eol)/$IP(eol_reduction_factor_write)]
	
	# Determine total on-chip variation
	set delay_variation_percent [expr 2*$vt_variation_percent*$lf_vt_drift_derating + $age_variation_derating*$age_variation_percent]
	set delay_variation [expr $t11_delay_difference*$delay_variation_percent]
	
	set setup_slack [expr $setup_slack - $delay_variation/2]
	set hold_slack  [expr $hold_slack  - $delay_variation/2]
	
	# Voltage variation across matched clocks (this is not included above because the matched clock paths are subtracted out)
	#Below values for delay miscorrelation matched paths are obtained from the FLS
	set delay_miscorrelation_matched_paths_just_v 0.014
	set delay_miscorrelation_matched_paths 0.02
	set matched_path_voltage_variation [expr $clock_delay_max*$delay_miscorrelation_matched_paths_just_v*2]
	set setup_slack [expr $setup_slack - $matched_path_voltage_variation/2]
	set hold_slack  [expr $hold_slack  - $matched_path_voltage_variation/2]

	# DCD effects
	#1.25 is the period @ 800MHz
	set dcd_effect [expr 1.25*$tDCD*2]
	set setup_slack [expr $setup_slack - $dcd_effect/2]
	set hold_slack  [expr $hold_slack  - $dcd_effect/2]			
	
	##############################
	# PVT compensated tracking uncertainties
	##############################
	
	set dll_update_latency 9000.0
	set hf_rate_of_change 0.00001029
	set ldc_tracking_error_percent [min $vt_variation_percent [expr $dll_update_latency*$hf_rate_of_change]]
	set ldc_tracking_error [expr 2*$t(CK)*3/$dll_length*$ldc_tracking_error_percent]
	set ldc_absolute_error [expr 2*$IP(quantization_WL)*3+2*$t(CK)*0.375*$delay_miscorrelation_matched_paths]
	
	set setup_slack [expr $setup_slack - $ldc_tracking_error/2 - $ldc_absolute_error/2]
	set hold_slack  [expr $hold_slack  - $ldc_tracking_error/2 - $ldc_absolute_error/2]	


	##############################
	# Quantization error
	##############################
	
	set quantization_step [expr 2*$IP(quantization_T11)*1.5]
	set setup_slack [expr $setup_slack - $quantization_step/2]
	set hold_slack  [expr $hold_slack  - $quantization_step/2]	

	##############################
	# Tracking algorithm uncertainties
	##############################
	
	if {$IP(tracking_enabled) == 1} {
		set mistake_tracking_jitter [expr 0.025*(2*$tDQSCK + 2*$tJITper + $total_ssn + $delay_variation + $quantization_step + $ldc_tracking_error + $matched_path_voltage_variation)]
		set setup_slack [expr $setup_slack - $mistake_tracking_jitter/2]
		set hold_slack  [expr $hold_slack  - $mistake_tracking_jitter/2]		
	}

	##############################
	# Multirank effects
	##############################
	
	
	# Remove Multirank board skew effects - Calibration will calibrate to the average of multiple ranks
	if {$IP(num_ranks) > 1} {
		set setup_slack [expr $setup_slack - $board(tpd_inter_DIMM)]
		set hold_slack  [expr $hold_slack  - $board(tpd_inter_DIMM)]
	}	

	lappend summary [list $opcname 0 "Postamble ($opcname)" $setup_slack $hold_slack]
}

