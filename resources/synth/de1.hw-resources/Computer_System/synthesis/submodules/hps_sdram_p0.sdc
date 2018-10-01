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
# This file contains the timing constraints for the UniPHY memory
# interface.
#    * The timing parameters used by this file are assigned
#      in the hps_sdram_p0_timing.tcl script.
#    * The helper routines are defined in hps_sdram_p0_pin_map.tcl
#
# NOTE
# ----

set script_dir [file dirname [info script]]

source "$script_dir/hps_sdram_p0_parameters.tcl"
source "$script_dir/hps_sdram_p0_timing.tcl"
source "$script_dir/hps_sdram_p0_pin_map.tcl"

load_package ddr_timing_model

set synthesis_flow 0
set sta_flow 0
set fit_flow 0
if { $::TimeQuestInfo(nameofexecutable) == "quartus_map" } {
	set synthesis_flow 1
} elseif { $::TimeQuestInfo(nameofexecutable) == "quartus_sta" } {
	set sta_flow 1
} elseif { $::TimeQuestInfo(nameofexecutable) == "quartus_fit" } {
	set fit_flow 1
}

####################
#                  #
# GENERAL SETTINGS #
#                  #
####################

# This is a global setting and will apply to the whole design.
# This setting is required for the memory interface to be
# properly constrained.
derive_clock_uncertainty

# Debug switch. Change to 1 to get more run-time debug information
set debug 0

# All timing requirements will be represented in nanoseconds with up to 3 decimal places of precision
set_time_format -unit ns -decimal_places 3

# Determine if entity names are on
set entity_names_on [ hps_sdram_p0_are_entity_names_on ]

##################
#                #
# QUERIED TIMING #
#                #
##################

set io_standard "DIFFERENTIAL 1.5-V SSTL CLASS I"

# This is the peak-to-peak jitter on the whole read capture path
set DQSpathjitter [expr [get_micro_node_delay -micro DQDQS_JITTER -parameters [list IO] -in_fitter]/1000.0]

# This is the proportion of the DQ-DQS read capture path jitter that applies to setup
set DQSpathjitter_setup_prop [expr [get_micro_node_delay -micro DQDQS_JITTER_DIVISION -parameters [list IO] -in_fitter]/100.0]

# This is the peak-to-peak jitter, of which half is considered to be tJITper
set tJITper [expr [get_micro_node_delay -micro MEM_CK_PERIOD_JITTER -parameters [list IO PHY_SHORT] -in_fitter -period $t(CK)]/2000.0 + $SSN(pullin_o)]

##################
#                #
# DERIVED TIMING #
#                #
##################

# These parameters are used to make constraints more readeable

# Half of memory clock cycle
set half_period [ hps_sdram_p0_round_3dp [ expr $t(CK) / 2.0 ] ]

# Half of reference clock
set ref_half_period [ hps_sdram_p0_round_3dp [ expr $t(refCK) / 2.0 ] ]

# Minimum delay on data output pins
set t(wru_output_min_delay_external) [expr $t(DH) + $board(intra_DQS_group_skew) + $ISI(DQ)/2 + $ISI(DQS)/2 - $board(DQ_DQS_skew)]
set t(wru_output_min_delay_internal) [expr $t(WL_DCD) + $t(WL_JITTER)*(1.0-$t(WL_JITTER_DIVISION)) + $SSN(rel_pullin_o)]
set data_output_min_delay [ hps_sdram_p0_round_3dp [ expr - $t(wru_output_min_delay_external) - $t(wru_output_min_delay_internal)]]

# Maximum delay on data output pins
set t(wru_output_max_delay_external) [expr $t(DS) + $board(intra_DQS_group_skew) + $ISI(DQ)/2 + $ISI(DQS)/2 + $board(DQ_DQS_skew)]
set t(wru_output_max_delay_internal) [expr $t(WL_DCD) + $t(WL_JITTER)*$t(WL_JITTER_DIVISION) + $SSN(rel_pushout_o)]
set data_output_max_delay [ hps_sdram_p0_round_3dp [ expr $t(wru_output_max_delay_external) + $t(wru_output_max_delay_internal)]]

# Maximum delay on data input pins
set t(rdu_input_max_delay_external) [expr $t(DQSQ) + $board(intra_DQS_group_skew) + $board(DQ_DQS_skew) + $ISI(READ_DQ)/2 + $ISI(READ_DQS)/2]
set t(rdu_input_max_delay_internal) [expr $DQSpathjitter*$DQSpathjitter_setup_prop + $SSN(rel_pushout_i)]
set data_input_max_delay [ hps_sdram_p0_round_3dp [ expr $t(rdu_input_max_delay_external) + $t(rdu_input_max_delay_internal) ]]

# Minimum delay on data input pins
set t(rdu_input_min_delay_external) [expr $board(intra_DQS_group_skew) - $board(DQ_DQS_skew) + $ISI(READ_DQ)/2 + $ISI(READ_DQS)/2]
set t(rdu_input_min_delay_internal) [expr $t(DCD) + $DQSpathjitter*(1.0-$DQSpathjitter_setup_prop) + $SSN(rel_pullin_i)]
set data_input_min_delay [ hps_sdram_p0_round_3dp [ expr - $t(rdu_input_min_delay_external) - $t(rdu_input_min_delay_internal) ]]

# Minimum delay on address and command paths
set ac_min_delay [ hps_sdram_p0_round_3dp [ expr - $t(IH) -$fpga(tPLL_JITTER) - $fpga(tPLL_PSERR) - $board(intra_addr_ctrl_skew) + $board(addresscmd_CK_skew) - $ISI(addresscmd_hold) ]]

# Maximum delay on address and command paths
set ac_max_delay [ hps_sdram_p0_round_3dp [ expr $t(IS) +$fpga(tPLL_JITTER) + $fpga(tPLL_PSERR) + $board(intra_addr_ctrl_skew) + $board(addresscmd_CK_skew) + $ISI(addresscmd_setup) ]]

if { $debug } {
	post_message -type info "SDC: Computed Parameters:"
	post_message -type info "SDC: --------------------"
	post_message -type info "SDC: half_period: $half_period"
	post_message -type info "SDC: data_output_min_delay: $data_output_min_delay"
	post_message -type info "SDC: data_output_max_delay: $data_output_max_delay"
	post_message -type info "SDC: data_input_min_delay: $data_input_min_delay"
	post_message -type info "SDC: data_input_max_delay: $data_input_max_delay"
	post_message -type info "SDC: ac_min_delay: $ac_min_delay"
	post_message -type info "SDC: ac_max_delay: $ac_max_delay"
	post_message -type info "SDC: Using Timing Models: Micro"
}

# This is the main call to the netlist traversal routines
# that will automatically find all pins and registers required
# to apply timing constraints.
# During the fitter, the routines will be called only once
# and cached data will be used in all subsequent calls.
if { ! [ info exists hps_sdram_p0_sdc_cache ] } {
	set hps_sdram_p0_sdc_cache 1
	hps_sdram_p0_initialize_ddr_db hps_sdram_p0_ddr_db
} else {
	if { $debug } {
		post_message -type info "SDC: reusing cached DDR DB"
	}
}

# If multiple instances of this core are present in the
# design they will all be constrained through the
# following loop
set instances [ array names hps_sdram_p0_ddr_db ]
foreach { inst } $instances {
	if { [ info exists pins ] } {
		# Clean-up stale content
		unset pins
	}
	array set pins $hps_sdram_p0_ddr_db($inst)

	set prefix $inst
	if { $entity_names_on } {
		set prefix [ string map "| |*:" $inst ]
		set prefix "*:$prefix"
	}

	#####################################################
	#                                                   #
	# Transfer the pin names to more readable variables #
	#                                                   #
	#####################################################

	set dqs_pins $pins(dqs_pins)
	set dqsn_pins $pins(dqsn_pins)
	set q_groups [ list ]
	foreach { q_group } $pins(q_groups) {
		set q_group $q_group
		lappend q_groups $q_group
	}
	set all_dq_pins [ join [ join $q_groups ] ]

	set ck_pins $pins(ck_pins)
	set ckn_pins $pins(ckn_pins)
	set add_pins $pins(add_pins)
	set ba_pins $pins(ba_pins)
	set cmd_pins $pins(cmd_pins)
	set reset_pins $pins(reset_pins)
	set ac_pins [ concat $add_pins $ba_pins $cmd_pins ]
	set dm_pins $pins(dm_pins)
	set all_dq_dm_pins [ concat $all_dq_pins $dm_pins ]

	set pll_ref_clock "pll_ref_clock"
	set pll_dq_write_clock $pins(pll_dq_write_clock)
	set pll_ck_clock $pins(pll_ck_clock)
	set pll_write_clock $pins(pll_write_clock)
	set pll_driver_core_clock $pins(pll_driver_core_clock)

	set dqs_in_clocks $pins(dqs_in_clocks)
	set dqs_out_clocks $pins(dqs_out_clocks)
	set dqsn_out_clocks $pins(dqsn_out_clocks)

	set afi_reset_reg $pins(afi_reset_reg)
	set sync_reg $pins(sync_reg)
	set read_capture_ddio $pins(read_capture_ddio)
	set fifo_wraddress_reg $pins(fifo_wraddress_reg)
	set fifo_rdaddress_reg $pins(fifo_rdaddress_reg)
	set fifo_wrdata_reg $pins(fifo_wrdata_reg)
	set fifo_rddata_reg $pins(fifo_rddata_reg)

	##################
	#                #
	# QUERIED TIMING #
	#                #
	##################

	# Phase Jitter on DQS paths. This parameter is queried at run time
	set fpga(tDQS_PHASE_JITTER) [ expr [ get_integer_node_delay -integer $::GLOBAL_hps_sdram_p0_dqs_delay_chain_length -parameters {IO MAX HIGH} -src DQS_PHASE_JITTER -in_fitter ] / 1000.0 ]

	# Phase Error on DQS paths. This parameter is queried at run time
	set fpga(tDQS_PSERR) [ expr [ get_integer_node_delay -integer $::GLOBAL_hps_sdram_p0_dqs_delay_chain_length -parameters {IO MAX HIGH} -src DQS_PSERR -in_fitter ] / 1000.0 ]

	# Correct input min/max delay for queried parameters
	set t(rdu_input_min_delay_external) [expr $t(rdu_input_min_delay_external) + ($t(CK)/2.0 - $t(QH_time))]
	set t(rdu_input_min_delay_internal) [expr $t(rdu_input_min_delay_internal) + $fpga(tDQS_PSERR) + $tJITper]
	set t(rdu_input_max_delay_external) [expr $t(rdu_input_max_delay_external)]
	set t(rdu_input_max_delay_internal) [expr $t(rdu_input_max_delay_internal) + $fpga(tDQS_PSERR)]

	set final_data_input_max_delay [ hps_sdram_p0_round_3dp [ expr $data_input_max_delay + $fpga(tDQS_PSERR) ]]
	set final_data_input_min_delay [ hps_sdram_p0_round_3dp [ expr $data_input_min_delay - $t(CK) / 2.0 + $t(QH_time) - $fpga(tDQS_PSERR) - $tJITper]]

	if { $debug } {
		post_message -type info "SDC: Jitter Parameters"
		post_message -type info "SDC: -----------------"
		post_message -type info "SDC:    DQS Phase: $::GLOBAL_hps_sdram_p0_dqs_delay_chain_length"
		post_message -type info "SDC:    fpga(tDQS_PHASE_JITTER): $fpga(tDQS_PHASE_JITTER)"
		post_message -type info "SDC:    fpga(tDQS_PSERR): $fpga(tDQS_PSERR)"
		post_message -type info "SDC:    t(QH_time): $t(QH_time)"
		post_message -type info "SDC:"
		post_message -type info "SDC: Derived Parameters:"
		post_message -type info "SDC: -----------------"
		post_message -type info "SDC:    Corrected data_input_max_delay: $final_data_input_max_delay"
		post_message -type info "SDC:    Corrected data_input_min_delay: $final_data_input_min_delay"
		post_message -type info "SDC: -----------------"
	}

	# ----------------------- #
	# -                     - #
	# --- REFERENCE CLOCK --- #
	# -                     - #
	# ----------------------- #

	# This is the reference clock used by the PLL to derive any other clock in the core
	if { [get_collection_size [get_clocks -nowarn $pll_ref_clock]] > 0 } { remove_clock $pll_ref_clock }

	# ------------------ #
	# -                - #
	# --- PLL CLOCKS --- #
	# -                - #
	# ------------------ #
	
	


	# DQ write clock
	set local_pll_dq_write_clk [ hps_sdram_p0_get_or_add_clock_vseries_from_virtual_refclk \
		-target $pll_dq_write_clock \
		-suffix "dq_write_clk" \
		-period $t(CK) \
		-phase $::GLOBAL_hps_sdram_p0_pll_phase(PLL_WRITE_CLK) ]
	
	# DQS write clock
	set local_pll_write_clk [ hps_sdram_p0_get_or_add_clock_vseries_from_virtual_refclk \
		-target $pll_write_clock \
		-suffix "write_clk" \
		-period $t(CK) \
		-phase $::GLOBAL_hps_sdram_p0_pll_phase(PLL_MEM_CLK) ]


	




	# Pulse-generator used by DQS tracking
	set local_sampling_clock "${inst}|hps_sdram_p0_sampling_clock"

	if {[get_collection_size [get_registers -nowarn $pins(dqs_enable_regs_pins)]] > 0} {
		create_generated_clock \
			-add \
			-name $local_sampling_clock \
			-source $pll_write_clock \
			-multiply_by 1 \
			-divide_by 1 \
			-phase 0 \
			$pins(dqs_enable_regs_pins)
	}

	# If this is the example design, then we need to find the PLL output which is used in the core by the driver and MPFE ports.
	# The node name is known; check to see if it exists (implying the example design) before creating the clock.
	if {[string compare -nocase $pll_driver_core_clock "_UNDEFINED_PIN_"] != 0} {
		set local_pll_driver_core_clk [ hps_sdram_p0_get_or_add_clock_vseries \
			-target $pll_driver_core_clock \
			-suffix "driver_core_clk" \
			-source $pll_ref_clock \
			-multiply_by 1 \
			-divide_by 1 \
			-phase 0 ]	
	}


	# -------------------- #
	# -                  - #
	# --- SYSTEM CLOCK --- #
	# -                  - #
	# -------------------- #

	# This is the CK clock
	foreach { ck_pin } $ck_pins {
		create_generated_clock -multiply_by 1 -source $pll_write_clock -master_clock "$local_pll_write_clk" $ck_pin -name $ck_pin
		set_clock_uncertainty -to [ get_clocks $ck_pin ] $t(WL_JITTER)
	}

	# This is the CK#clock
	foreach { ckn_pin } $ckn_pins {
		create_generated_clock -multiply_by 1 -invert -source $pll_write_clock -master_clock "$local_pll_write_clk" $ckn_pin -name $ckn_pin
		set_clock_uncertainty -to [ get_clocks $ckn_pin ] $t(WL_JITTER)
	}
	
	# ------------------- #
	# -                 - #
	# --- READ CLOCKS --- #
	# -                 - #
	# ------------------- #

	foreach dqs_in_clock_struct $dqs_in_clocks {
		array set dqs_in_clock $dqs_in_clock_struct
		# This is the DQS clock for Read Capture analysis (micro model)
		create_clock -period $t(CK) -waveform [ list 0 $half_period ] $dqs_in_clock(dqs_pin) -name $dqs_in_clock(dqs_pin)_IN -add

		# Clock Uncertainty is accounted for by the ...pathjitter parameters
		set_clock_uncertainty -from [ get_clocks $dqs_in_clock(dqs_pin)_IN ] 0
	}

	# -------------------- #
	# -                  - #
	# --- WRITE CLOCKS --- #
	# -                  - #
	# -------------------- #

	# This is the DQS clock for Data Write analysis (micro model)
	foreach dqs_out_clock_struct $dqs_out_clocks {
		array set dqs_out_clock $dqs_out_clock_struct
		create_generated_clock -multiply_by 1 -master_clock [get_clocks $local_pll_write_clk] -source $pll_write_clock $dqs_out_clock(dst) -name $dqs_out_clock(dst)_OUT -add

		# Clock Uncertainty is accounted for by the ...pathjitter parameters
		set_clock_uncertainty -to [ get_clocks $dqs_out_clock(dst)_OUT ] 0
	}

	# This is the DQS#clock for Data Write analysis (micro model)
	foreach dqsn_out_clock_struct $dqsn_out_clocks {
		array set dqsn_out_clock $dqsn_out_clock_struct
		create_generated_clock -multiply_by 1 -master_clock [get_clocks $local_pll_write_clk] -source $pll_write_clock $dqsn_out_clock(dst) -name $dqsn_out_clock(dst)_OUT -add

		# Clock Uncertainty is accounted for by the ...pathjitter parameters
		set_clock_uncertainty -to [ get_clocks $dqsn_out_clock(dst)_OUT ] 0
	}

	##################
	#                #
	# READ DATA PATH #
	#                #
	##################

	foreach { dqs_pin } $dqs_pins { dq_pins } $q_groups {
		foreach { dq_pin } $dq_pins {
			if {[get_collection_size [get_registers -nowarn $read_capture_ddio]] > 0} {
				set_max_delay -from [get_ports $dq_pin] -to $read_capture_ddio 0
				set_min_delay -from [get_ports $dq_pin] -to $read_capture_ddio [expr 0-$half_period]
			}

			# Specifies the maximum delay difference between the DQ pin and the DQS pin:
			set_input_delay -max $final_data_input_max_delay -clock [get_clocks ${dqs_pin}_IN ] [get_ports $dq_pin] -add_delay

			# Specifies the minimum delay difference between the DQ pin and the DQS pin:
			set_input_delay -min $final_data_input_min_delay -clock [get_clocks ${dqs_pin}_IN ] [get_ports $dq_pin] -add_delay
		}
	}

	###################
	#                 #
	# WRITE DATA PATH #
	#                 #
	###################

	foreach { dqs_pin } $dqs_pins { dq_pins } $q_groups {
		foreach { dq_pin } $dq_pins {
			# Specifies the minimum delay difference between the DQS pin and the DQ pins:
			set_output_delay -min $data_output_min_delay -clock [get_clocks ${dqs_pin}_OUT ] [get_ports $dq_pin] -add_delay

			# Specifies the maximum delay difference between the DQS pin and the DQ pins:
			set_output_delay -max $data_output_max_delay -clock [get_clocks ${dqs_pin}_OUT ] [get_ports $dq_pin] -add_delay
		}
	}

	foreach { dqsn_pin } $dqsn_pins { dq_pins } $q_groups {
		foreach { dq_pin } $dq_pins {
			# Specifies the minimum delay difference between the DQS#pin and the DQ pins:
			set_output_delay -min $data_output_min_delay -clock [get_clocks ${dqsn_pin}_OUT ] [get_ports $dq_pin] -add_delay

			# Specifies the maximum delay difference between the DQS#pin and the DQ pins:
			set_output_delay -max $data_output_max_delay -clock [get_clocks ${dqsn_pin}_OUT ] [get_ports $dq_pin] -add_delay
		}
	}

	foreach dqs_out_clock_struct $dqs_out_clocks {
		array set dqs_out_clock $dqs_out_clock_struct

		if { [string length $dqs_out_clock(dm_pin)] > 0 } {
			# Specifies the minimum delay difference between the DQS and the DM pins:
			set_output_delay -min $data_output_min_delay -clock [get_clocks $dqs_out_clock(dst)_OUT ] [get_ports $dqs_out_clock(dm_pin)] -add_delay

			# Specifies the maximum delay difference between the DQS and the DM pins:
			set_output_delay -max $data_output_max_delay -clock [get_clocks $dqs_out_clock(dst)_OUT ] [get_ports $dqs_out_clock(dm_pin)] -add_delay
		}
	}

	foreach dqsn_out_clock_struct $dqsn_out_clocks {
		array set dqsn_out_clock $dqsn_out_clock_struct

		if { [string length $dqsn_out_clock(dm_pin)] > 0 } {
			# Specifies the minimum delay difference between the DQS and the DM pins:
			set_output_delay -min $data_output_min_delay -clock [get_clocks $dqsn_out_clock(dst)_OUT ] [get_ports $dqsn_out_clock(dm_pin)] -add_delay

			# Specifies the maximum delay difference between the DQS and the DM pins:
			set_output_delay -max $data_output_max_delay -clock [get_clocks $dqsn_out_clock(dst)_OUT ] [get_ports $dqsn_out_clock(dm_pin)] -add_delay
		}
	}

	##################
	#                #
	# DQS vs CK PATH #
	#                #
	##################

	foreach { ck_pin } $ck_pins { 
		set_output_delay -add_delay -clock [get_clocks $ck_pin] -max [hps_sdram_p0_round_3dp [expr $t(CK) - $t(DQSS)*$t(CK) - $board(minCK_DQS_skew) ]] $dqs_pins
		set_output_delay -add_delay -clock [get_clocks $ck_pin] -min [hps_sdram_p0_round_3dp [expr $t(DQSS)*$t(CK) - $board(maxCK_DQS_skew) ]] $dqs_pins
		set_false_path -to [get_clocks $ck_pin] -fall_from [get_clocks $local_pll_write_clk ] 
	}

	############
	#          #
	# A/C PATH #
	#          #
	############

	foreach { ck_pin } $ck_pins {
		# ac_pins can contain input ports such as mem_err_out_n
		# Loop through each ac pin to make sure we only apply set_output_delay to output ports
		foreach { ac_pin } $ac_pins {
			set ac_port [ get_ports $ac_pin ]
			if {[get_collection_size $ac_port] > 0} {
				if [ get_port_info -is_output_port $ac_port ] {
					# Specifies the minimum delay difference between the DQS pin and the address/control pins:
					set_output_delay -min [hps_sdram_p0_round_3dp [expr {$ac_min_delay + $t(CK)/2}]] -clock [get_clocks $ck_pin] $ac_port -add_delay

					# Specifies the maximum delay difference between the DQS pin and the address/control pins:
					set_output_delay -max [hps_sdram_p0_round_3dp [expr {$ac_max_delay + $t(CK)/2}]] -clock [get_clocks $ck_pin] $ac_port -add_delay
				}
			}
		}
	}

	# Only the rising edge-launched control data needs to be timing analyzed in full rate
	set_false_path -fall_from [ get_clocks ${local_pll_write_clk} ] -to [ get_ports $ac_pins ]

	##########################
	#                        #
	# MULTICYCLE CONSTRAINTS #
	#                        #
	##########################


	# If powerdown feature is enabled, multicycle path from core logic to the CK generator. 
	# The PHY must be idle several cycles before entering and after exiting powerdown mode.
	if { [get_collection_size [get_registers -nowarn ${prefix}|*p0|*umemphy|*uio_pads|*uaddr_cmd_pads|*clock_gen[*].umem_ck_pad|*]] > 0 } {
		set_multicycle_path -to [get_registers ${prefix}|*p0|*umemphy|*uio_pads|*uaddr_cmd_pads|*clock_gen[*].umem_ck_pad|*] -end -setup 4
		set_multicycle_path -to [get_registers ${prefix}|*p0|*umemphy|*uio_pads|*uaddr_cmd_pads|*clock_gen[*].umem_ck_pad|*] -end -hold 4
	}

	




	set read_fifo_read_dff ${prefix}|*p0|*altdq_dqs2_inst|*read_fifo~OUTPUT_DFF_*
	set read_fifo_write_address_dff ${prefix}|*p0|*altdq_dqs2_inst|*read_fifo~WRITE_ADDRESS_DFF
	set read_fifo_read_address_dff ${prefix}|*p0|*altdq_dqs2_inst|*read_fifo~READ_ADDRESS_DFF
	set lfifo_in_read_en_dff ${prefix}|*p0|*lfifo~LFIFO_IN_READ_EN_DFF
	set lfifo_in_read_en_full_dff ${prefix}|*p0|*lfifo~LFIFO_IN_READ_EN_FULL_DFF
	set lfifo_dff_reg ${prefix}|*p0|*lfifo~LFIFO_OUT_OCT_LFIFO_DFF
	set lfifo_out_rden_dff ${prefix}|*p0|*lfifo~LFIFO_OUT_RDEN_DFF
	set lfifo_out_rdata_valid_dff ${prefix}|*p0|*lfifo~LFIFO_OUT_RDATA_VALID_DFF
	set os_oct_ddio_oe_reg ${prefix}|*p0|*os_oct_ddio_oe~DFF
	set lfifo_rd_latency_dff ${prefix}|*p0|*lfifo~RD_LATENCY_DFF*
	set vfifo_qvld_in_dff ${prefix}|*p0|*altdq_dqs2_inst|vfifo~QVLD_IN_DFF
	set vfifo_inc_wr_ptr_dff ${prefix}|*p0|*vfifo~INC_WR_PTR_DFF
	set phase_align_dff ${prefix}|*p0|*altdq_dqs2_inst|phase_align_os~DFF*
	set os_oe_reg ${prefix}|*p0|*os_oe_reg
	set phase_align_dff ${prefix}|*p0|*phase_align_os~DFF*
	set hphy_ff ${prefix}|*p0|*umemphy|hphy_inst~FF_*
	set hmc_ff ${prefix}|*c0|hmc_inst~FF_*
	set phy_read_latency_counter $hphy_ff
	set read_fifo_reset $hphy_ff
	set phy_reset_mem_stable $hphy_ff

	set after_u2b 0
	if {[get_collection_size [get_registers -nowarn $read_fifo_write_address_dff]] > 0} {
		set after_u2b 1
	}

	if {$after_u2b} {

		set_multicycle_path -from $hphy_ff -to $lfifo_in_read_en_full_dff -end -setup 2
		set_multicycle_path -from $hphy_ff -to $lfifo_in_read_en_full_dff -end -hold 1

		set_multicycle_path -from $read_fifo_reset -to $read_fifo_read_address_dff -end -setup 2
		set_multicycle_path -from $read_fifo_reset -to $read_fifo_read_address_dff -end -hold 1


                set_false_path -from $hmc_ff -to ${prefix}|*p0|*umemphy|*uio_pads|*uaddr_cmd_pads|*ddio_out*
		set_false_path -from $hphy_ff -to $lfifo_in_read_en_dff
		set_false_path -from $hmc_ff -to $lfifo_in_read_en_dff
		set_false_path -from $hphy_ff -to $vfifo_inc_wr_ptr_dff
		set_false_path -from $hmc_ff -to $vfifo_qvld_in_dff
		set_false_path -from $lfifo_out_rdata_valid_dff -to $hphy_ff
		set_false_path -from $phy_reset_mem_stable -to $vfifo_qvld_in_dff
		set_false_path -from $phy_read_latency_counter -to $lfifo_rd_latency_dff
		set_false_path -from $hphy_ff -to ${prefix}|*p0|*umemphy|*uio_pads|*uaddr_cmd_pads|*ddio_out*
		set_false_path -from $hphy_ff -to ${prefix}|*p0|*umemphy|*altdq_dqs2_inst|*output_path_gen[*].ddio_out*
		set_false_path -from $hphy_ff -to ${prefix}|*p0|*umemphy|*altdq_dqs2_inst|extra_output_pad_gen[*].ddio_out*
		set_false_path -from $hphy_ff -to $hphy_ff
		set_false_path -from $hmc_ff -to $hphy_ff
		set_false_path -from $hphy_ff -to $hmc_ff
		set_false_path -from $hphy_ff -to $phase_align_dff
		set_false_path -from ${prefix}|*s0|* -to [get_clocks $local_pll_write_clk]
		set_false_path -from [get_clocks $local_pll_write_clk] -to ${prefix}|*s0|*hphy_bridge_s0_translator|av_readdata_pre[*]

		set_false_path -from $read_fifo_read_dff -to $hphy_ff

	}

	#  Sampling register to AVL clock is multicycled because of topology of the PHY
	if {[get_collection_size [get_registers -nowarn $pins(dqs_enable_regs_pins)]] > 0} {
		set_multicycle_path -from [get_clocks $local_sampling_clock] -setup 2
		set_multicycle_path -from [get_clocks $local_sampling_clock] -hold 2
	}


	##########################
	#                        #
	# FALSE PATH CONSTRAINTS #
	#                        #
	##########################

	# Cut paths for memory clocks / async resets to avoid unconstrained warnings
	foreach { pin } [concat $dqsn_pins $ck_pins $ckn_pins $reset_pins] {
		set_false_path -to [get_ports $pin]
	}

	if { ! $synthesis_flow } {
		foreach dqs_in_clock_struct $dqs_in_clocks dqsn_out_clock_struct $dqsn_out_clocks {
			array set dqs_in_clock $dqs_in_clock_struct
			array set dqsn_out_clock $dqsn_out_clock_struct

			set_clock_groups -physically_exclusive	-group "$dqs_in_clock(dqs_pin)_IN" -group "$dqs_in_clock(dqs_pin)_OUT $dqsn_out_clock(dst)_OUT"


		}
	}

	foreach dqs_out_clock_struct $dqs_out_clocks {
		array set dqs_out_clock $dqs_out_clock_struct
		set_false_path -from $read_fifo_reset -to [ get_clocks $dqs_out_clock(dst)_OUT ]
	}

	# The paths between DQS_ENA_CLK and DQS_IN are calibrated, so they must not be analyzed
	set_false_path -from [get_clocks $local_pll_write_clk] -to [get_clocks {*_IN}]



	# The following registers serve as anchors for the pin_map.tcl
	# script and are not used by the IP during memory operation

	# Cut internal calibrated paths
	set dqs_delay_chain_pst_dff ${prefix}|*p0|*altdq_dqs2_inst|dqs_delay_chain~POSTAMBLE_DFF
	if {$after_u2b} {
		set_false_path -from ${prefix}|*p0|*altdq_dqs2_inst|dqs_enable_ctrl~* -to $dqs_delay_chain_pst_dff
	}

	#  Cut path to sampling register, calibrated by the PHY
	if {[get_collection_size [get_registers -nowarn $pins(dqs_enable_regs_pins)]] > 0} {
		set_false_path -to [get_clocks $local_sampling_clock]
	}

	# ------------------------------ #
	# -                            - #
	# --- FITTER OVERCONSTRAINTS --- #
	# -                            - #
	# ------------------------------ #
	if {$fit_flow} {
	


	}
	
	# -------------------------------- #
	# -                              - #
	# --- TIMING MODEL ADJUSTMENTS --- #
	# -                              - #
	# -------------------------------- #

}

if {(($::quartus(nameofexecutable) ne "quartus_fit") && ($::quartus(nameofexecutable) ne "quartus_map"))} {
	set dqs_clocks [hps_sdram_p0_get_all_instances_dqs_pins hps_sdram_p0_ddr_db]
	# Leave clocks active when in debug mode
	if {[llength $dqs_clocks] > 0 && !$debug} {
		post_sdc_message info "Setting DQS clocks as inactive; use Report DDR to timing analyze DQS clocks"
		set_active_clocks [remove_from_collection [get_active_clocks] [get_clocks $dqs_clocks]]
	}
}

######################
#                    #
# REPORT DDR COMMAND #
#                    #
######################

add_ddr_report_command "source [list [file join [file dirname [info script]] ${::GLOBAL_hps_sdram_p0_corename}_report_timing.tcl]]"

