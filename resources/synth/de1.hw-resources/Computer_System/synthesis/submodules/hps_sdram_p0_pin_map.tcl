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
# This file contains the traversal routines that are used by both
# hps_sdram_p0_pin_assignments.tcl and hps_sdram_p0.sdc scripts. 
#
# These routines are only meant to support these two scripts. 
# Trying to using them in a different context can have unexpected 
# results.

set script_dir [file dirname [info script]]

source [file join $script_dir hps_sdram_p0_parameters.tcl]
load_package sdc_ext

proc hps_sdram_p0_find_all_pins { mystring } {
	set allpins [get_pins -compatibility_mode $mystring ]

	foreach_in_collection pin $allpins {
		set pinname [ get_pin_info -name $pin ]

		puts "$pinname"
	}
}


proc hps_sdram_p0_index_in_collection { col j } {
	set i 0
	foreach_in_collection path $col {
		if {$i == $j} {
			return $path
		}
		set i [expr $i + 1]
	}
	return ""
}


proc hps_sdram_p0_get_clock_to_pin_name_mapping {} {
	set result [list]
	set clocks_collection [get_clocks]
	foreach_in_collection clock $clocks_collection { 
		set clock_name [get_clock_info -name $clock] 
		set clock_target [get_clock_info -targets $clock]
		set first_index [hps_sdram_p0_index_in_collection $clock_target 0]
		set catch_exception [catch {get_pin_info -name $first_index} pin_name]
		if {$catch_exception == 0} {
			lappend result [list $clock_name $pin_name]
		}
	}
	return $result
}              


proc hps_sdram_p0_get_clock_name_from_pin_name { pin_name } {
	set table [hps_sdram_p0_get_clock_to_pin_name_mapping]
	foreach entry $table {
		if {[string compare [lindex [lindex [split $entry] 1] 0] $pin_name] == 0} {
			return [lindex $entry 0]
		}
	}
	return ""
}


proc hps_sdram_p0_get_clock_name_from_pin_name_vseries {pin_name suffix} {
	set name [hps_sdram_p0_get_clock_name_from_pin_name $pin_name]
	if {[string compare -nocase $name ""] == 0} {
		set pll_clock $pin_name
		regsub {[^\|]+$} $pll_clock {} pll_clock
		set pll_clock "${pll_clock}pll_${suffix}"
	} else {
		set pll_clock $name
	}
	return $pll_clock
}


proc hps_sdram_p0_get_clock_name_from_pin_name_pre_vseries {pin_name suffix} {
	set name [hps_sdram_p0_get_clock_name_from_pin_name $pin_name]
	if {[string compare -nocase $name ""] == 0} {
		set pll_clock $pin_name
		regsub {upll_memphy\|auto_generated\|pll1\|clk\[[0-9]+\]$} $pll_clock "pll" pll_clock
		set pll_clock "${pll_clock}_${suffix}"
	} else {
		set pll_clock $name
	}
	return $pll_clock
}

proc hps_sdram_p0_get_or_add_clock_vseries_from_virtual_refclk {args} {
	array set opts { /
		-suffix "" /
		-target "" /
		-period "" /
		-phase 0 }

	array set opts $args
	
	set clock_name [hps_sdram_p0_get_clock_name_from_pin_name $opts(-target)]

	
	if {[string compare -nocase $clock_name ""] == 0} {
		set clock_name $opts(-target)
		set suffix $opts(-suffix)
		
		regsub {~PLL_OUTPUT_COUNTER\|divclk$} $clock_name "" clock_name
		regsub {_phy$} $clock_name "" clock_name
		regsub {[0-9]+$} $clock_name "" clock_name
		set clock_name "${clock_name}_${suffix}"
		set re [expr $opts(-period) * $opts(-phase)/360]
		set fe [expr $opts(-period) * $opts(-phase)/360 + $opts(-period)/2]
		
		create_clock \
			-name $clock_name \
			-period $opts(-period) \
			-waveform [ list $re $fe ] \
			$opts(-target)
	}
	
	return $clock_name
}

proc hps_sdram_p0_get_or_add_clock_vseries {args} {
	array set opts { /
		-suffix "" /
		-target "" /
		-source "" /
		-multiply_by 1 /
		-divide_by 1 /
		-phase 0 }

	array set opts $args
	set target $opts(-target)

	set clock_name [hps_sdram_p0_get_clock_name_from_pin_name $opts(-target)]
	
	if {[string compare -nocase $clock_name ""] == 0} {
		set clock_name $opts(-target)
		set suffix $opts(-suffix)
		
		regsub {~PLL_OUTPUT_COUNTER\|divclk$} $clock_name "" clock_name
		regsub {_phy$} $clock_name "" clock_name
		regsub {[0-9]+$} $clock_name "" clock_name
		regsub -all {\\} $clock_name "" clock_name
		set clock_name "${clock_name}_${suffix}"
		set source_name "\{$opts(-source)\}"

		create_generated_clock \
			-name ${clock_name} \
			-source ${source_name} \
			-multiply_by $opts(-multiply_by) \
			-divide_by $opts(-divide_by) \
			-phase $opts(-phase) \
			$target
	}
	
	return $clock_name
}

proc hps_sdram_p0_get_or_add_clock_pre_vseries {args} {
	array set opts { /
		-suffix "" /
		-target "" /
		-source "" /
		-multiply_by 1 /
		-divide_by 1 /
		-phase 0 }

	array set opts $args
	
	set clock_name [hps_sdram_p0_get_clock_name_from_pin_name $opts(-target)]
	
	if {[string compare -nocase $clock_name ""] == 0} {
		set clock_name $opts(-target)
		set suffix $opts(-suffix)
		
		regsub {upll_memphy\|auto_generated\|pll1\|clk\[[0-9]+\]$} $clock_name "pll" clock_name
		set clock_name "${clock_name}_${suffix}"
		
		create_generated_clock \
			-name $clock_name \
			-source $opts(-source) \
			-multiply_by $opts(-multiply_by) \
			-divide_by $opts(-divide_by) \
			-phase $opts(-phase) \
			$opts(-target)
	}
	
	return $clock_name
}


proc hps_sdram_p0_get_source_clock_pin_name {node_name} {

	set nodename ""
	set nodes [get_nodes $node_name]
	hps_sdram_p0_traverse_fanin_up_to_depth [hps_sdram_p0_index_in_collection $nodes 0] hps_sdram_p0_is_node_type_pll_clk clock results_array 10
	if {[array size results_array] == 1} {
		set pin_id [lindex [array names results_array] 0]
		if {[string compare -nocase $pin_id ""] != 0} {
			set nodename [get_node_info -name $pin_id]
		}
	}
	return $nodename
}


proc hps_sdram_p0_find_all_keepers { mystring } {
	set allkeepers [get_keepers $mystring ]

	foreach_in_collection keeper $allkeepers {
		set keepername [ get_node_info -name $keeper ]

		puts "$keepername"
	}
}

proc hps_sdram_p0_round_3dp { x } {
	return [expr { round($x * 1000) / 1000.0  } ]
}

proc hps_sdram_p0_get_timequest_name {hier_name} {
	set sta_name ""
	for {set inst_start [string first ":" $hier_name]} {$inst_start != -1} {} {
		incr inst_start
		set inst_end [string first "|" $hier_name $inst_start]
		if {$inst_end == -1} {
			append sta_name [string range $hier_name $inst_start end]
			set inst_start -1
		} else {
			append sta_name [string range $hier_name $inst_start $inst_end]
			set inst_start [string first ":" $hier_name $inst_end]
		}
	}
	return $sta_name
}

proc hps_sdram_p0_are_entity_names_on { } {
	set entity_names_on 1


	return [set_project_mode -is_show_entity]	
}

proc hps_sdram_p0_get_core_instance_list {corename} {
	set full_instance_list [hps_sdram_p0_get_core_full_instance_list $corename]
	set instance_list [list]

	foreach inst $full_instance_list {
		set sta_name [hps_sdram_p0_get_timequest_name $inst]
		if {[lsearch $instance_list [escape_brackets $sta_name]] == -1} {
			lappend instance_list $sta_name
		}
	}
	return $instance_list
}

proc hps_sdram_p0_get_core_full_instance_list {corename} {
	set allkeepers [get_keepers * ]

	set_project_mode -always_show_entity_name on

	set instance_list [list]

	set inst_regexp {(^.*}
	append inst_regexp {:[A-Za-z0-9\.\\_\[\]\-\$():]+)\|}
	append inst_regexp ${corename}
	append inst_regexp {:[A-Za-z0-9\.\\_\[\]\-\$():]+\|}
	append inst_regexp "${corename}_acv_hard_memphy"
        append inst_regexp {:umemphy}

	foreach_in_collection keeper $allkeepers {
		set name [ get_node_info -name $keeper ]

		if {[regexp -- $inst_regexp $name -> hier_name] == 1} {
			if {[lsearch $instance_list [escape_brackets $hier_name]] == -1} {
				lappend instance_list $hier_name
			}
		}
	}

	set_project_mode -always_show_entity_name qsf

	if {[ llength $instance_list ] == 0} {
		post_message -type error "The auto-constraining script was not able to detect any instance for core < $corename >"
		post_message -type error "Verify the following:"
		post_message -type error " The core < $corename > is instantiated within another component (wrapper)"
		post_message -type error " The core is not the top-level of the project"
		post_message -type error " The memory interface pins are exported to the top-level of the project"
		post_message -type error "Alternatively, if you are no longer instantiating core < $corename >,"
		post_message -type error " clean up any stale SDC_FILE references from the QSF/QIP files."
	}

	return $instance_list
}


proc hps_sdram_p0_traverse_fanin_up_to_depth { node_id match_command edge_type results_array_name depth} {
	upvar 1 $results_array_name results

	if {$depth < 0} {
		error "Internal error: Bad timing netlist search depth"
	}
	set fanin_edges [get_node_info -${edge_type}_edges $node_id]
	set number_of_fanin_edges [llength $fanin_edges]
	for {set i 0} {$i != $number_of_fanin_edges} {incr i} {
		set fanin_edge [lindex $fanin_edges $i]
		set fanin_id [get_edge_info -src $fanin_edge]
		if {$match_command == "" || [eval $match_command $fanin_id] != 0} {
			set results($fanin_id) 1
		} elseif {$depth == 0} {
		} else {
			hps_sdram_p0_traverse_fanin_up_to_depth $fanin_id $match_command $edge_type results [expr {$depth - 1}]
		}
	}
}
proc hps_sdram_p0_is_node_type_pll_inclk { node_id } {
	set cell_id [get_node_info -cell $node_id]
	
	if {$cell_id == ""} {
		set result 0
	} else {
		set atom_type [get_cell_info -atom_type $cell_id]
		if {$atom_type == "FRACTIONAL_PLL"} {
			set node_name [get_node_info -name $node_id]
			set fanin_edges [get_node_info -clock_edges $node_id]
			if {([string match "*|refclkin" $node_name] || [string match "*|refclkin\\\[0\\\]" $node_name]) && [llength $fanin_edges] > 0} {
				set result 1
			} else {
				set result 0
			}
		} elseif {$atom_type == "HPS_SDRAM_PLL"} {
			set node_name [get_node_info -name $node_id]
			set fanin_edges [get_node_info -clock_edges $node_id]
			if {[string match "*|ref_clk" $node_name] && [llength $fanin_edges] > 0} {
				set result 1
			} else {
				set result 0
			}
		} elseif {$atom_type == "PLL"} {
			set node_name [get_node_info -name $node_id]
			set fanin_edges [get_node_info -clock_edges $node_id]
			if {([string match "*|refclk" $node_name] || [string match "*|refclk\\\[0\\\]" $node_name]) && [llength $fanin_edges] > 0} {
				set result 1
			} else {
				set result 0
			}
		} else {
			set result 0
		}
	}
	return $result
}

proc hps_sdram_p0_is_node_type_pin { node_id } {
	set node_type [get_node_info -type $node_id]
	if {$node_type == "port"} {
		set result 1
	} else {
		set result 0
	}
	return $result
}

proc hps_sdram_p0_get_input_clk_id { pll_output_node_id } {
	if {[hps_sdram_p0_is_node_type_pll_clk $pll_output_node_id]} {
		array set results_array [list]
		hps_sdram_p0_traverse_fanin_up_to_depth $pll_output_node_id hps_sdram_p0_is_node_type_pll_inclk clock results_array 2
		if {[array size results_array] == 1} {
			# Found PLL inclk, now find the input pin
			set pll_inclk_id [lindex [array names results_array] 0]
			set result [get_node_info -name $pll_inclk_id]
		} else {
			post_message -type critical_warning "Could not find PLL clock for [get_node_info -name $pll_output_node_id]"
			set result -1
		}
	} else {
		error "Internal error: hps_sdram_p0_get_input_clk_id only works on PLL output clocks"
	}
	return $result
}

proc hps_sdram_p0_is_node_type_pll_clk { node_id } {
	set cell_id [get_node_info -cell $node_id]
	
	if {$cell_id == ""} {
		set result 0
	} else {
		set atom_type [get_cell_info -atom_type $cell_id]
		if {$atom_type == "PLL_OUTPUT_COUNTER"} {
			set node_name [get_node_info -name $node_id]
			if {[string match "*|pll*~PLL_OUTPUT_COUNTER*|divclk" $node_name]} {
				set result 1
			} else {
				set result 0
			}
		} elseif {$atom_type == "HPS_SDRAM_PLL"} {
			set node_name [get_node_info -name $node_id]
			if {[string match "*|*pll|*_clk" $node_name]} {
				set result 1
			} else {
				set result 0
			}
		} elseif {$atom_type == "PLL"} {
			set node_name [get_node_info -name $node_id]
			if {[string match "*|pll*|divclk" $node_name]} {
				set result 1
			} else {
				set result 0
			}
		} else {
			set result 0
		}
	}
	return $result
}

proc hps_sdram_p0_get_pll_clock { dest_id_list node_type clock_id_name search_depth} {
	if {$clock_id_name != ""} {
		upvar 1 $clock_id_name clock_id
	}
	set clock_id -1

	array set clk_array [list]
	foreach node_id $dest_id_list {
		hps_sdram_p0_traverse_fanin_up_to_depth $node_id hps_sdram_p0_is_node_type_pll_clk clock clk_array $search_depth
	}
	if {[array size clk_array] == 1} {
		set clock_id [lindex [array names clk_array] 0]
		set clk [get_node_info -name $clock_id]
	} elseif {[array size clk_array] > 1} {
		puts "Found more than 1 clock driving the $node_type"
		set clk ""
	} else {
		set clk ""
	}

	return $clk
}

proc hps_sdram_p0_get_pll_clock_name { clock_id } {
	set clock_name [get_node_info -name $clock_id]

	return $clock_name
}

proc hps_sdram_p0_get_pll_clock_name_for_acf { clock_id pll_output_wire_name } {
	set clock_name [get_node_info -name $clock_id]
	regexp {(.*)\|pll\d+\~PLL_OUTPUT_COUNTER} $clock_name matched clock_name
	regexp {(.*)\|pll\d+_phy\~PLL_OUTPUT_COUNTER} $clock_name matched clock_name
	set clock_name "$clock_name|$pll_output_wire_name"
	return $clock_name
}

proc hps_sdram_p0_get_output_clock_id { ddio_output_pin_list pin_type msg_list_name {max_search_depth 20} } {
	upvar 1 $msg_list_name msg_list
	set output_clock_id -1
	
	set output_id_list [list]
	set pin_collection [get_keepers -no_duplicates $ddio_output_pin_list]
	if {[get_collection_size $pin_collection] == [llength $ddio_output_pin_list]} {
		foreach_in_collection id $pin_collection {
			lappend output_id_list $id
		}
	} elseif {[get_collection_size $pin_collection] == 0} {
		lappend msg_list "warning" "Could not find any $pin_type pins"
	} else {
		lappend msg_list "warning" "Could not find all $pin_type pins"
	}
	hps_sdram_p0_get_pll_clock $output_id_list $pin_type output_clock_id $max_search_depth
	return $output_clock_id
}

proc hps_sdram_p0_get_output_clock_id2 { ddio_output_pin_list pin_type msg_list_name {max_search_depth 20} } {
	upvar 1 $msg_list_name msg_list
	set output_clock_id -1
	
	set output_id_list [list]
	set pin_collection [get_pins -no_duplicates $ddio_output_pin_list]
	if {[get_collection_size $pin_collection] == [llength $ddio_output_pin_list]} {
		foreach_in_collection id $pin_collection {
			lappend output_id_list $id
		}
	} elseif {[get_collection_size $pin_collection] == 0} {
		lappend msg_list "warning" "Could not find any $pin_type pins"
	} else {
		lappend msg_list "warning" "Could not find all $pin_type pins"
	}
	hps_sdram_p0_get_pll_clock $output_id_list $pin_type output_clock_id $max_search_depth
	return $output_clock_id
}

proc hps_sdram_p0_is_node_type_clkbuf { node_id } {
	set cell_id [get_node_info -cell $node_id]
	if {$cell_id == ""} {
		set result 0
	} else {
		set atom_type [get_cell_info -atom_type $cell_id]
		if {$atom_type == "CLKBUF" || $atom_type == "PHY_CLKBUF"} {
			set result 1
		} else {
			set result 0
		}
	}
	return $result
}

proc hps_sdram_p0_get_clkbuf_clock { dest_id_list node_type clock_id_name search_depth} {
	if {$clock_id_name != ""} {
		upvar 1 $clock_id_name clock_id
	}
	set clock_id -1

	array set clk_array [list]
	foreach node_id $dest_id_list {
		hps_sdram_p0_traverse_fanin_up_to_depth $node_id hps_sdram_p0_is_node_type_clkbuf clock clk_array $search_depth
	}
	if {[array size clk_array] == 1} {
		set clock_id [lindex [array names clk_array] 0]
		set clk [get_node_info -name $clock_id]
	} elseif {[array size clk_array] > 1} {
		set clk ""
	} else {
		set clk ""
	}

	return $clk
}

proc hps_sdram_p0_get_output_clock_clkbuf_id { ddio_output_pin_list pin_type msg_list_name {max_search_depth 20} } {
	upvar 1 $msg_list_name msg_list
	set output_clock_id -1
	
	set output_id_list [list]
	set pin_collection [get_keepers -no_duplicates $ddio_output_pin_list]
	if {[get_collection_size $pin_collection] == [llength $ddio_output_pin_list]} {
		foreach_in_collection id $pin_collection {
			lappend output_id_list $id
		}
	} elseif {[get_collection_size $pin_collection] == 0} {
		lappend msg_list "warning" "Could not find any $pin_type pins"
	} else {
		lappend msg_list "warning" "Could not find all $pin_type pins"
	}
	hps_sdram_p0_get_clkbuf_clock $output_id_list $pin_type output_clock_id $max_search_depth
	return $output_clock_id
}


proc hps_sdram_p0_is_node_type_clk_phase_select { node_id } {
	set cell_id [get_node_info -cell $node_id]
	if {$cell_id == ""} {
		set result 0
	} else {
		set atom_type [get_cell_info -atom_type $cell_id]
		if {$atom_type == "CLK_PHASE_SELECT"} {
			set result 1
		} else {
			set result 0
		}
	}
	return $result
}

proc hps_sdram_p0_get_clk_phase_select_clock { dest_id_list node_type clock_id_name search_depth} {
	if {$clock_id_name != ""} {
		upvar 1 $clock_id_name clock_id
	}
	set clock_id -1

	array set clk_array [list]
	foreach node_id $dest_id_list {
		hps_sdram_p0_traverse_fanin_up_to_depth $node_id hps_sdram_p0_is_node_type_clk_phase_select clock clk_array $search_depth
	}
	if {[array size clk_array] == 1} {
		set clock_id [lindex [array names clk_array] 0]
		set clk [get_node_info -name $clock_id]
	} elseif {[array size clk_array] > 1} {
		set clk ""
	} else {
		set clk ""
	}

	return $clk
}

proc hps_sdram_p0_get_output_clock_clk_phase_select_id { ddio_output_pin_list pin_type msg_list_name {max_search_depth 20} } {
	upvar 1 $msg_list_name msg_list
	set output_clock_id -1
	
	set output_id_list [list]
	set pin_collection [get_keepers -no_duplicates $ddio_output_pin_list]
	if {[get_collection_size $pin_collection] == [llength $ddio_output_pin_list]} {
		foreach_in_collection id $pin_collection {
			lappend output_id_list $id
		}
	} elseif {[get_collection_size $pin_collection] == 0} {
		lappend msg_list "warning" "Could not find any $pin_type pins"
	} else {
		lappend msg_list "warning" "Could not find all $pin_type pins"
	}
	hps_sdram_p0_get_clk_phase_select_clock $output_id_list $pin_type output_clock_id $max_search_depth
	return $output_clock_id
}

proc post_sdc_message {msg_type msg} {
	if { $::TimeQuestInfo(nameofexecutable) != "quartus_fit"} {
		post_message -type $msg_type $msg
	}
}

proc hps_sdram_p0_get_names_in_collection { col } {
	set res [list]
	foreach_in_collection node $col {
		lappend res [ get_node_info -name $node ]
	}
	return $res
}

proc hps_sdram_p0_static_map_expand_list { FH listname pinname } {
	upvar $listname local_list

	puts $FH ""
	puts $FH "   # $pinname"
	puts $FH "   set pins($pinname) \[ list \]"
	foreach pin $local_list($pinname) {
		puts $FH "   lappend pins($pinname) $pin"
	}
}

proc hps_sdram_p0_static_map_expand_list_of_list { FH listname pinname } {
	upvar $listname local_list

	puts $FH ""
	puts $FH "   # $pinname"
	puts $FH "   set pins($pinname) \[ list \]"
	set count_groups 0
	foreach sublist $local_list($pinname) {
		puts $FH ""
		puts $FH "   # GROUP - ${count_groups}"
		puts $FH "   set group_${count_groups} \[ list \]"
		foreach pin $sublist {
			puts $FH "   lappend group_${count_groups} $pin"
		}
		puts $FH ""
		puts $FH "   lappend pins($pinname) \$group_${count_groups}"

		incr count_groups
	}
}

proc hps_sdram_p0_static_map_expand_string { FH stringname pinname } {
	upvar $stringname local_string

	puts $FH ""
	puts $FH "   # $pinname"
	puts $FH "   set pins($pinname) $local_string($pinname)"
}

proc hps_sdram_p0_format_3dp { x } {
	return [format %.3f $x]
}

proc hps_sdram_p0_get_colours { x y } {

	set fcolour [list "black"]
	if {$x < 0} {
		lappend fcolour "red"
	} else {
		lappend fcolour "blue"
	}
	if {$y < 0} {
		lappend fcolour "red"
	} else {
		lappend fcolour "blue"
	}
	
	return $fcolour
}

proc min { a b } {
	if { $a == "" } { 
		return $b
	} elseif { $a < $b } {
		return $a
	} else {
		return $b
	}
}

proc max { a b } {
	if { $a == "" } { 
		return $b
	} elseif { $a > $b } {
		return $a
	} else {
		return $b
	}
}

proc hps_sdram_p0_max_in_collection { col attribute } {
	set i 0
	set max 0
	foreach_in_collection path $col {
		if {$i == 0} {
			set max [get_path_info $path -${attribute}]
		} else {
			set temp [get_path_info $path -${attribute}]
			if {$temp > $max} {
				set max $temp
			} 
		}
		set i [expr $i + 1]
	}
	return $max
}

proc hps_sdram_p0_min_in_collection { col attribute } {
	set i 0
	set min 0
	foreach_in_collection path $col {
		if {$i == 0} {
			set min [get_path_info $path -${attribute}]
		} else {
			set temp [get_path_info $path -${attribute}]
			if {$temp < $min} {
				set min $temp
			} 
		}
		set i [expr $i + 1]
	}
	return $min
}

proc hps_sdram_p0_min_in_collection_to_name { col attribute name } {
	set i 0
	set min 0
	foreach_in_collection path $col {
		if {[get_node_info -name [get_path_info $path -to]] == $name} {
			if {$i == 0} {
				set min [get_path_info $path -${attribute}]
			} else {
				set temp [get_path_info $path -${attribute}]
				if {$temp < $min} {
					set min $temp
				} 
			}
			set i [expr $i + 1]		
		}
	}
	return $min
}

proc hps_sdram_p0_min_in_collection_from_name { col attribute name } {
	set i 0
	set min 0
	foreach_in_collection path $col {
		if {[get_node_info -name [get_path_info $path -from]] == $name} {
			if {$i == 0} {
				set min [get_path_info $path -${attribute}]
			} else {
				set temp [get_path_info $path -${attribute}]
				if {$temp < $min} {
					set min $temp
				} 
			}
			set i [expr $i + 1]			
		}
	}
	return $min
}

proc hps_sdram_p0_max_in_collection_to_name { col attribute name } {
	set i 0
	set max 0
	foreach_in_collection path $col {
		if {[get_node_info -name [get_path_info $path -to]] == $name} {
			if {$i == 0} {
				set max [get_path_info $path -${attribute}]
			} else {
				set temp [get_path_info $path -${attribute}]
				if {$temp > $max} {
					set max $temp
				} 
			}
			set i [expr $i + 1]					
		}
	}
	return $max
}

proc hps_sdram_p0_max_in_collection_from_name { col attribute name } {
	set i 0
	set max 0
	foreach_in_collection path $col {
		if {[get_node_info -name [get_path_info $path -from]] == $name} {
			if {$i == 0} {
				set max [get_path_info $path -${attribute}]
			} else {
				set temp [get_path_info $path -${attribute}]
				if {$temp > $max} {
					set max $temp
				} 
			}
			set i [expr $i + 1]
		}
	}
	return $max
}


proc hps_sdram_p0_min_in_collection_to_name2 { col attribute name } {
	set i 0
	set min 0
	foreach_in_collection path $col {
		if {[regexp $name [get_node_info -name [get_path_info $path -to]]]} {
			if {$i == 0} {
				set min [get_path_info $path -${attribute}]
			} else {
				set temp [get_path_info $path -${attribute}]
				if {$temp < $min} {
					set min $temp
				} 
			}
			set i [expr $i + 1]		
		}
	}
	return $min
}

proc hps_sdram_p0_min_in_collection_from_name2 { col attribute name } {
	set i 0
	set min 0
	foreach_in_collection path $col {
		if {[regexp $name [get_node_info -name [get_path_info $path -from]]]} {
			if {$i == 0} {
				set min [get_path_info $path -${attribute}]
			} else {
				set temp [get_path_info $path -${attribute}]
				if {$temp < $min} {
					set min $temp
				} 
			}
			set i [expr $i + 1]
		}
	}
	return $min
}

proc hps_sdram_p0_max_in_collection_to_name2 { col attribute name } {
	set i 0
	set max 0
	foreach_in_collection path $col {
		if {[regexp $name [get_node_info -name [get_path_info $path -to]]]} {
			if {$i == 0} {
				set max [get_path_info $path -${attribute}]
			} else {
				set temp [get_path_info $path -${attribute}]
				if {$temp > $max} {
					set max $temp
				} 
			}
			set i [expr $i + 1]				
		}
	}
	return $max
}

proc hps_sdram_p0_max_in_collection_from_name2 { col attribute name } {
	set i 0
	set max 0
	foreach_in_collection path $col {
		if {[regexp $name [get_node_info -name [get_path_info $path -from]]]} {
			if {$i == 0} {
				set max [get_path_info $path -${attribute}]
			} else {
				set temp [get_path_info $path -${attribute}]
				if {$temp > $max} {
					set max $temp
				} 
			}
			set i [expr $i + 1]
		}
	}
	return $max
}


proc hps_sdram_p0_get_max_clock_path_delay_through_clock_node {from through to} {
	set init 0
	set max_delay 0
	set paths [get_path -rise_from $through -rise_to $to]
	foreach_in_collection path1 $paths {
		set delay [get_path_info $path1 -arrival_time]
		set clock_node [get_node_info -name [get_path_info $path1 -from]]
				
		set paths2 [get_path -rise_from $from -rise_to $clock_node]
		foreach_in_collection path2 $paths2 {
			set total_delay [expr $delay + [get_path_info $path2 -arrival_time]]
			if {$init == 0 || $total_delay > $max_delay} {
				set init 1
				set max_delay $total_delay
			}
		}
	}
	return $max_delay
}

proc hps_sdram_p0_get_min_clock_path_delay_through_clock_node {from through to} {
	set init 0
	set min_delay 0
	set paths [get_path -rise_from $through -rise_to $to -min_path]
	foreach_in_collection path1 $paths {
		set delay [get_path_info $path1 -arrival_time]
		set clock_node [get_node_info -name [get_path_info $path1 -from]]

		set paths2 [get_path -rise_from $from -rise_to $clock_node -min_path]
		foreach_in_collection path2 $paths2 {
			set total_delay [expr $delay + [get_path_info $path2 -arrival_time]]
			if {$init == 0 || $total_delay < $min_delay} {
				set init 1
				set min_delay $total_delay
			}
		}
	}
	return $min_delay
}

proc hps_sdram_p0_get_model_corner {} {

	set operating_conditions [get_operating_conditions]
	set return_value [list]
	if {[regexp {^([0-9])_H([0-9])_([a-z]+)_([a-z0-9_\-]+)} $operating_conditions matched speedgrade transceiver model corner]} {

	} elseif {[regexp {^([A-Z0-9]+)_([a-z]+)_([a-z0-9_\-]+)} $operating_conditions matched speedgrade model corner]} {

	}
	regsub {\-} $corner "n" corner
	set return_value [list $model $corner]
	return $return_value
}

proc hps_sdram_p0_get_min_aiot_delay {pinname} {

	set atom_id [get_atom_node_by_name -name $pinname]
	set sin_pin [create_pin_object -atom $atom_id]
	set results [get_simulation_results -pin $sin_pin -aiot]
	
	set rise 0
	set fall 0
	foreach { key value } $results {
		if {$key == "Absolute Rise Delay to Far-end"} {
			set rise $value
		} elseif {$key == "Absolute Fall Delay to Far-end"} {
			set fall $value
		}
	}
	return [min $rise $fall]
}

proc hps_sdram_p0_get_rise_aiot_delay {pinname} {

	set atom_id [get_atom_node_by_name -name $pinname]
	set sin_pin [create_pin_object -atom $atom_id]
	set results [get_simulation_results -pin $sin_pin -aiot]
	
	set rise 0
	foreach { key value } $results {
            if {$key == "Absolute Rise Delay to Far-end"} {
               set rise $value
            }
	}
	return $rise
}

proc hps_sdram_p0_get_fall_aiot_delay {pinname} {

	set atom_id [get_atom_node_by_name -name $pinname]
	set sin_pin [create_pin_object -atom $atom_id]
	set results [get_simulation_results -pin $sin_pin -aiot]
	
	set fall 0
	foreach { key value } $results {
            if {$key == "Absolute Fall Delay to Far-end"} {
               set fall $value
            }
	}
	return $fall
}


proc hps_sdram_p0_get_aiot_attr {pinname attr} {

	set atom_id [get_atom_node_by_name -name $pinname]
	set sin_pin [create_pin_object -atom $atom_id]
	set results [get_simulation_results -pin $sin_pin -aiot]
	
	set value 0
	foreach { key value } $results {
		if {$key == $attr} {
			return $value
		} 
	}
	return $value
}

proc hps_sdram_p0_get_pll_phase_shift {output_counter_name} {
	load_package atoms
	read_atom_netlist
	set phase_shift ""
	
	# Remove possible "|divclk" at the end of the name
	regsub {\|divclk$} $output_counter_name "" output_counter_name

	# Get all PLL output counters
	set pll_output_counter_atoms [get_atom_nodes -type PLL_OUTPUT_COUNTER]
	
	# Go through the output counters and find the one that matches the above and return the phase
	foreach_in_collection atom $pll_output_counter_atoms { 
		set name [get_atom_node_info -key name -node $atom] 
		regsub {^[^\:]+\:} $name "" name
		regsub -all {\|[^\:]+\:} $name "|" name
		
		# If the name matches return the phase shift
		if {$name == $output_counter_name} {
			set phase_shift [get_atom_node_info -key TIME_PHASE_SHIFT -node $atom]
			regsub { ps} $phase_shift "" phase_shift
			break
		}
	}
	return $phase_shift
}

# ----------------------------------------------------------------
#
proc hps_sdram_p0_get_io_standard {target_pin} {
#
# Description: Gets the I/O standard of the given memory interface pin
#              This function assumes the fitter has already completed and the
#              compiler report has been loaded.
#
# ----------------------------------------------------------------
	# Look through the pin report
	set io_std [hps_sdram_p0_get_fitter_report_pin_info $target_pin "I/O Standard" -1]
	if {$io_std == ""} {
		return "UNKNOWN"
	}
	set result ""
	switch -exact -- $io_std {
		"SSTL-2 Class I" {set result "SSTL_2_I"}
		"Differential 2.5-V SSTL Class I" {set result "DIFF_SSTL_2_I"}
		"SSTL-2 Class II" {set result "SSTL_2_II"}
		"Differential 2.5-V SSTL Class II" {set result "DIFF_SSTL_2_II"}
		"SSTL-18 Class I" {set result "SSTL_18_I"}
		"Differential 1.8-V SSTL Class I" {set result "DIFF_SSTL_18_I"}
		"SSTL-18 Class II" {set result "SSTL_18_II"}
		"Differential 1.8-V SSTL Class II" {set result "DIFF_SSTL_18_II"}
		"SSTL-15 Class I" {set result "SSTL_15_I"}
		"Differential 1.5-V SSTL Class I" {set result "DIFF_SSTL_15_I"}
		"SSTL-15 Class II" {set result "SSTL_15_II"}
		"Differential 1.5-V SSTL Class II" {set result "DIFF_SSTL_15_II"}
		"1.8-V HSTL Class I" {set result "HSTL_18_I"}
		"Differential 1.8-V HSTL Class I" {set result "DIFF_HSTL_18_I"}
		"1.8-V HSTL Class II" {set result "HSTL_18_II"}
		"Differential 1.8-V HSTL Class II" {set result "DIFF_HSTL_18_II"}
		"1.5-V HSTL Class I" {set result "HSTL_I"}
		"Differential 1.5-V HSTL Class I" {set result "DIFF_HSTL"}
		"1.5-V HSTL Class II" {set result "HSTL_II"}
		"Differential 1.5-V HSTL Class II" {set result "DIFF_HSTL_II"}
		"1.2-V HSTL Class I" {set result "SSTL_125"}
		"Differential 1.2-V HSTL Class I" {set result "DIFF_SSTL_125"}
		"1.2-V HSTL Class II" {set result "SSTL_125"}
		"Differential 1.2-V HSTL Class II" {set result "DIFF_SSTL_125"}
		"SSTL-15" {set result "SSTL_15"}
		"Differential 1.5-V SSTL" {set result "DIFF_SSTL_15"}
		"SSTL-135" {set result "SSTL_135"}
		"Differential 1.35-V SSTL" {set result "DIFF_SSTL_135"}
		"SSTL-125" {set result "SSTL_125"}
		"Differential 1.25-V SSTL" {set result "DIFF_SSTL_125"}
		"SSTL-12" {set result "DIFF_SSTL_125"}
		"Differential 1.2-V HSUL" {set result "DIFF_HSUL_12"}
		default {
			post_message -type error "Found unsupported Memory I/O standard $io_std on pin $target_pin"
			set result "UNKNOWN"
		}
	}
	return $result
}

# Routine to find the termination pins
proc hps_sdram_p0_get_rzq_pins { instname all_rzq_pins } {
	upvar $all_rzq_pins rzqpins
	load_package atoms
	read_atom_netlist
	set rzq_pins [ list ]
	set entity_names_on [ hps_sdram_p0_are_entity_names_on ]
	
	# Get all termination atoms, to which rzqpin should be attached
	set_project_mode -always_show_entity_name off
	set instance ${instname}*
	set atoms [get_atom_nodes -type TERMINATION -matching [escape_brackets $instance] ]
	post_message -type info "Number of Termination Atoms are [get_collection_size $atoms]"
	foreach_in_collection term_atom $atoms { 
		set rzq_pin ""
		set atom $term_atom
		set term_atom_name [get_atom_node_info -key name -node $term_atom] 
		post_message -type info "Found Termination Atom $term_atom_name"
		set type [get_atom_node_info -key type -node $term_atom] 
		
		# Check until you traverse to an IO_PAD for the RZQ Pin
		while { ![regexp IO_PAD $type ] } { 
			set name [get_atom_node_info -key name -node $atom] 
			set iterms [get_atom_iports -node $atom]
			set iterm_size [llength $iterms]
			# Check for Multiple Inputs
			if { $iterm_size > 1 } {
				post_message -type error " Multiple inputs to a node:$name attached to a  Termination_Atom:$term_atom_name "
				break
			
			}
			
			foreach iterm $iterms { 
				set fanin	[get_atom_port_info -node $atom -type iport -port_id $iterm -key fanin]
				set atom [lindex $fanin 0]
				set type [get_atom_node_info -key type -node $atom]
				set rzq_pin [get_atom_node_info -key name -node $atom]
			}		
		}
			
		lappend rzq_pins [ join $rzq_pin ]
	}

	set_project_mode -always_show_entity_name qsf
	set rzqpins $rzq_pins
}


proc hps_sdram_p0_get_acv_read_offset { period dqs_phase dqs_period } {

	set offset [expr abs(90/360.0*$period - $dqs_phase/360.0*$dqs_period)]
	if {$offset != 0} {
		set part_period [expr $dqs_phase/360.0*$dqs_period - 0.469/2.0 - 0.12]
		set offset [max 0.120 $part_period] 
	}

	return $offset
}
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


proc hps_sdram_p0_sort_proc {a b} {
	set idxs [list 1 2 0]
	foreach i $idxs {
		set ai [lindex $a $i]
		set bi [lindex $b $i]
		if {$ai > $bi} {
			return 1
		} elseif { $ai < $bi } {
			return -1
		}
	}
	return 0
}

proc hps_sdram_p0_traverse_atom_path {atom_id atom_oport_id path} {
	# Return list of {atom oterm_id} pairs by tracing the atom netlist starting from the given atom_id through the given path
	# Path consists of list of {atom_type fanin|fanout|end <port_type> <-optional>}
	set result [list]
	if {[llength $path] > 0} {
		set path_point [lindex $path 0]
		set atom_type [lindex $path_point 0]
		set next_direction [lindex $path_point 1]
		set port_type [lindex $path_point 2]
		set atom_optional [lindex $path_point 3]
		if {[get_atom_node_info -key type -node $atom_id] == $atom_type} {
			if {$next_direction == "end"} {
				if {[get_atom_port_info -key type -node $atom_id -port_id $atom_oport_id -type oport] == $port_type} {
					lappend result [list $atom_id $atom_oport_id]
				}
			} elseif {$next_direction == "atom"} {
				lappend result [list $atom_id]
			} elseif {$next_direction == "fanin"} {
				set atom_iport [get_atom_iport_by_type -node $atom_id -type $port_type]
				if {$atom_iport != -1} {
					set iport_fanin [get_atom_port_info -key fanin -node $atom_id -port_id $atom_iport -type iport]
					set source_atom [lindex $iport_fanin 0]
					set source_oterm [lindex $iport_fanin 1]
					set result [hps_sdram_p0_traverse_atom_path $source_atom $source_oterm [lrange $path 1 end]]
				} elseif {$atom_optional == "-optional"} {
					set result [hps_sdram_p0_traverse_atom_path $atom_id $atom_oport_id [lrange $path 1 end]]
				}
			} elseif {$next_direction == "fanout"} {
				set atom_oport [get_atom_oport_by_type -node $atom_id -type $port_type]
				if {$atom_oport != -1} {
					set oport_fanout [get_atom_port_info -key fanout -node $atom_id -port_id $atom_oport -type oport]
					foreach dest $oport_fanout {
						set dest_atom [lindex $dest 0]
						set dest_iterm [lindex $dest 1]
						set fanout_result_list [hps_sdram_p0_traverse_atom_path $dest_atom -1 [lrange $path 1 end]]
						foreach fanout_result $fanout_result_list {
							if {[lsearch $result $fanout_result] == -1} {
								lappend result $fanout_result
							}
						}
					}
				}
			} else {
				error "Unexpected path"
			}
		} elseif {$atom_optional == "-optional"} {
			set result [hps_sdram_p0_traverse_atom_path $atom_id $atom_oport_id [lrange $path 1 end]]
		}
	}
	return $result
}

# Get the fitter name of the PLL output driving the given pin
proc hps_sdram_p0_traverse_to_ddio_out_pll_clock {pin msg_list_name} {
	upvar 1 $msg_list_name msg_list
	set result ""
	if {$pin != ""} {
		set pin_id [get_atom_node_by_name -name $pin]
		set pin_to_pll_path [list {IO_PAD fanin PADIN} {IO_OBUF fanin I} {PSEUDO_DIFF_OUT fanin I -optional} {DELAY_CHAIN fanin DATAIN -optional} {DELAY_CHAIN fanin DATAIN -optional} {DDIO_OUT fanin CLKHI -optional} {OUTPUT_PHASE_ALIGNMENT fanin CLK -optional} {CLKBUF fanin INCLK -optional} {PLL end CLK}]
		set pll_id_list [hps_sdram_p0_traverse_atom_path $pin_id -1 $pin_to_pll_path]
		if {[llength $pll_id_list] == 1} {
			set atom_oterm_pair [lindex $pll_id_list 0]
			set result [get_atom_port_info -key name -node [lindex $atom_oterm_pair 0] -port_id [lindex $atom_oterm_pair 1] -type oport]
		} else {
			lappend msg_list "Error: PLL clock not found for $pin"
		}
	}
	return $result
}

proc hps_sdram_p0_traverse_to_leveling_delay_chain {pin msg_list_name} {
	upvar 1 $msg_list_name msg_list
	set result ""
	if {$pin != ""} {
		set pin_id [get_atom_node_by_name -name $pin]
		set pin_to_leveling_path [list {IO_PAD fanin PADIN} {IO_OBUF fanin I} {PSEUDO_DIFF_OUT fanin I -optional} {DELAY_CHAIN fanin DATAIN -optional} {DELAY_CHAIN fanin DATAIN -optional} {DDIO_OUT fanin CLK -optional} {FF fanin CLK -optional} {DDIO_OUT fanin CLKHI -optional} {CLK_PHASE_SELECT fanin CLKIN -optional} {LEVELING_DELAY_CHAIN end CLKOUT} ]
		set leveling_id_list [hps_sdram_p0_traverse_atom_path $pin_id -1 $pin_to_leveling_path]
		if {[llength $leveling_id_list] == 1} {
			set atom_oterm_pair [lindex $leveling_id_list 0]
			set result [get_atom_node_info -key name -node [lindex $atom_oterm_pair 0]]
		} else {
			lappend msg_list "Error: Leveling delay chain not found for $pin"
		}
	}

	regsub {^[^\:]+\:} $result "" result
	regsub -all {\|[^\:]+\:} $result "|" result

	return $result
}

proc hps_sdram_p0_traverse_to_clock_phase_select {pin msg_list_name} {
	upvar 1 $msg_list_name msg_list
	set result ""
	if {$pin != ""} {
		set pin_id [get_atom_node_by_name -name $pin]
		set pin_to_cps_path [list {IO_PAD fanin PADIN} {IO_OBUF fanin I} {PSEUDO_DIFF_OUT fanin I -optional} {DELAY_CHAIN fanin DATAIN -optional} {DELAY_CHAIN fanin DATAIN -optional} {DDIO_OUT fanin CLK -optional} {FF fanin CLK -optional} {DDIO_OUT fanin CLKHI -optional} {CLK_PHASE_SELECT end CLKOUT}  ]
		set cps_id_list [hps_sdram_p0_traverse_atom_path $pin_id -1 $pin_to_cps_path]
		if {[llength $cps_id_list] == 1} {
			set atom_oterm_pair [lindex $cps_id_list 0]
			set result [get_atom_node_info -key name -node [lindex $atom_oterm_pair 0]]
		} else {
			lappend msg_list "Error: Clock phase select not found for $pin"
		}
	}

	regsub {^[^\:]+\:} $result "" result
	regsub -all {\|[^\:]+\:} $result "|" result

	return $result
}

proc hps_sdram_p0_traverse_to_clkbuf {pin msg_list_name} {
	upvar 1 $msg_list_name msg_list
	set result ""
	if {$pin != ""} {
		set pin_id [get_atom_node_by_name -name $pin]
		set pin_to_clkbuf_path [list {IO_PAD fanin PADIN} {IO_OBUF fanin I} {PSEUDO_DIFF_OUT fanin I -optional} {DELAY_CHAIN fanin DATAIN -optional} {DELAY_CHAIN fanin DATAIN -optional} {DDIO_OUT fanin CLK -optional} {FF fanin CLK -optional} {DDIO_OUT fanin CLKHI -optional} {CLKBUF end OUTCLK}  ]
		set clkbuf_id_list [hps_sdram_p0_traverse_atom_path $pin_id -1 $pin_to_clkbuf_path]
		if {[llength $clkbuf_id_list] == 1} {
			set atom_oterm_pair [lindex $clkbuf_id_list 0]
			set result [get_atom_node_info -key name -node [lindex $atom_oterm_pair 0]]
		} else {
			lappend msg_list "Error: Clock buffer not found for $pin"
		}
	}

	regsub {^[^\:]+\:} $result "" result
	regsub -all {\|[^\:]+\:} $result "|" result

	return $result
}

proc hps_sdram_p0_traverse_to_dll {dqs_pin msg_list_name} {
	upvar 1 $msg_list_name msg_list
	set dqs_pin_id [get_atom_node_by_name -name $dqs_pin]
	set dqs_to_dll_path [list {IO_PAD fanout PADOUT} {IO_IBUF fanout O} {DQS_DELAY_CHAIN fanin DELAYCTRLIN} {DLL end DELAYCTRLOUT}]
	set dll_id_list [hps_sdram_p0_traverse_atom_path $dqs_pin_id -1 $dqs_to_dll_path]
	set result ""
	if {[llength $dll_id_list] == 1} {
		set dll_atom_oterm_pair [lindex $dll_id_list 0]
		set result [get_atom_node_info -key name -node [lindex $dll_atom_oterm_pair 0]]
	} elseif {[llength $dll_id_list] > 1} {
		lappend msg_list "Error: Found more than 1 DLL"
	} else {
		lappend msg_list "Error: DLL not found"
	}
	return $result
}

proc hps_sdram_p0_check_hybrid_interface { inst pins_array_name mem_if_memtype } {
	upvar $pins_array_name pins

	foreach q_group $pins(q_groups) {
		set q_group $q_group
		lappend q_groups $q_group
	}
	set all_dq_pins [ join [ join $q_groups ] ]
	set dm_pins $pins(dm_pins)

	set all_dq_dm_pins [ concat $all_dq_pins $dm_pins ]
	foreach dq_dm_pin $all_dq_dm_pins {
		set io_type [hps_sdram_p0_get_fitter_report_pin_io_type_info $dq_dm_pin]
		if {[string compare -nocase "Column I/O" $io_type] == 0} {
			set io_types("column") 1
		} elseif {[string compare -nocase "Row I/O" $io_type] == 0} {
			set io_types("row") 1
		} else {
			post_message -type warning "Could not determine IO type for pin $dq_dm_pin"
		}
	}

	if {[llength [array names io_types]] == 0} {
		post_message -type warning "Could not determine if memory interface $inst is implemented in hybrid mode. Assuming memory interface is implemented in non-hybrid mode"
		return 0
	} elseif {[llength [array names io_types]] == 1} {
		return 0
	} elseif {[llength [array names io_types]] == 2} {
		return 1
	} else {
		post_message -type error "Internal Error: Found IO types [array names io_types]"
		qexit -error
	}

}

proc hps_sdram_p0_verify_flexible_timing_assumptions { inst pins_array_name mem_if_memtype } {
	return 1
}

proc hps_sdram_p0_verify_high_performance_timing_assumptions { inst pins_array_name mem_if_memtype } {
	upvar $pins_array_name pins

	set num_errors 0
	load_package verify_ddr
	set ck_ckn_pairs [list]
	set failed_assumptions [list]
	if {[llength $pins(ck_pins)] > 0 && [llength $pins(ck_pins)] == [llength $pins(ckn_pins)]} {
		for {set ck_index 0} {$ck_index != [llength $pins(ck_pins)]} {incr ck_index} {
			lappend ck_ckn_pairs [list [lindex $pins(ck_pins) $ck_index] [lindex $pins(ckn_pins) $ck_index]]
		}
	} else {
		incr num_errors
		lappend failed_assumptions "Error: Could not locate same number of CK pins as CK# pins"
	}

	set read_pins_list [list]
	set write_pins_list [list]
	set read_clock_pairs [list]
	set write_clock_pairs [list]
	foreach { dqs } $pins(dqs_pins) { dqsn } $pins(dqsn_pins) { dq_list } $pins(q_groups) {
		lappend read_pins_list [list $dqs $dq_list]
		lappend read_clock_pairs [list $dqs $dqsn]
	}

	foreach { dqs } $pins(dqs_pins) { dqsn } $pins(dqsn_pins) { dm_list } $pins(dm_pins) { dq_list } $pins(q_groups) {
		lappend write_pins_list [list $dqs [concat $dq_list $dm_list]]
		lappend write_clock_pairs [list $dqs $dqsn]
	}

	set all_write_dqs_list $pins(dqs_pins)
	set all_d_list $pins(all_dq_pins)
	if {[llength $pins(q_groups)] == 0} {
		incr num_errors
		lappend failed_assumptions "Error: Could not locate DQS pins"
	}

	if {$num_errors == 0} {
		set msg_list [list]
		set dll_name [hps_sdram_p0_traverse_to_dll $dqs msg_list]
		set clk_to_write_d [hps_sdram_p0_traverse_to_ddio_out_pll_clock [lindex $all_d_list 0] msg_list]
		set clk_to_write_clock [hps_sdram_p0_traverse_to_ddio_out_pll_clock [lindex $all_write_dqs_list 0] msg_list]
		set clk_to_ck_ckn [hps_sdram_p0_traverse_to_ddio_out_pll_clock [lindex $pins(ck_pins) 0] msg_list]
		foreach msg $msg_list {
			set verify_assumptions_exception 1
			incr num_errors
			lappend failed_assumptions $msg
		}
		if {$num_errors == 0} {
			set verify_assumptions_exception 0
			set verify_assumptions_result {0}
			set verify_assumptions_exception [catch {verify_assumptions -uniphy -memory_type $mem_if_memtype \
				-read_pins_list $read_pins_list -write_pins_list $write_pins_list -ck_ckn_pairs $ck_ckn_pairs \
				-clk_to_write_d $clk_to_write_d -clk_to_write_clock $clk_to_write_clock -clk_to_ck_ckn $clk_to_ck_ckn \
				-dll $dll_name -read_clock_pairs $read_clock_pairs -write_clock_pairs $write_clock_pairs} verify_assumptions_result]
			if {$verify_assumptions_exception == 0} {
				incr num_errors [lindex $verify_assumptions_result 0]
				set failed_assumptions [concat $failed_assumptions [lrange $verify_assumptions_result 1 end]]
			}
		}
		if {$verify_assumptions_exception != 0} {
			lappend failed_assumptions "Error: MACRO timing assumptions could not be verified"
			incr num_errors
		}
	}

	if {$num_errors != 0} {
		for {set i 0} {$i != [llength $failed_assumptions]} {incr i} {
			set raw_msg [lindex $failed_assumptions $i]
			if {[regexp {^\W*(Info|Extra Info|Warning|Critical Warning|Error): (.*)$} $raw_msg -- msg_type msg]} {
				regsub " " $msg_type _ msg_type
				if {$msg_type == "Error"} {
					set msg_type "critical_warning"
				}
				post_message -type $msg_type $msg
			} else {
				post_message -type info $raw_msg
			}
		}
		post_message -type critical_warning "Read Capture and Write timing analyses may not be valid due to violated timing model assumptions"
	}

	return [expr $num_errors == 0]
}

# Return a tuple of the tCCS value for a given device
proc hps_sdram_p0_get_tccs { mem_if_memtype dqs_list period args} {
	global TimeQuestInfo
	array set options [list "-write_deskew" "none" "-dll_length" 0 "-config_period" 0 "-ddr3_discrete" 0]
	foreach {option value} $args {
		if {[array names options -exact "$option"] != ""} {
			set options($option) $value
		} else {
			error "ERROR: Unknown get_tccs option $option (with value $value; args are $args)"
		}
	}

	if {$mem_if_memtype == "ddr2"} {
		set options(-write_deskew) "none"
	}
	
	set speedgrade [ get_speedgrade_string ]
	if { ($mem_if_memtype == "ddr3") && ($speedgrade != "2") && ($options(-write_deskew) == "dynamic") } {
		set options(-write_deskew) "static"
	}
	
	if { ($mem_if_memtype == "ddr3") && ($options(-write_deskew) == "none") } {
		set options(-write_deskew) "static"
	}
	
	if { ($mem_if_memtype == "ddr3") && ($speedgrade == "4") && ($options(-ddr3_discrete) == 0)} {
		set options(-ddr3_discrete) 1
	}	

	set interface_type [hps_sdram_p0_get_io_interface_type $dqs_list]
	# The tCCS for a VHPAD interface is the same as a HPAD interface
	if {$interface_type == "VHPAD"} {
		set interface_type "HPAD"
	}
	set io_std [hps_sdram_p0_get_io_standard [lindex $dqs_list 0]]
  	set result [list 0 0]
	if {$interface_type != "" && $interface_type != "UNKNOWN" && $io_std != "" && $io_std != "UNKNOWN"} {
		package require ::quartus::ddr_timing_model

		set tccs_params [list IO $interface_type]
		if {($mem_if_memtype == "ddr3") && ($options(-ddr3_discrete) == 1)} {
			lappend tccs_params NONLEVELED
		} elseif {($mem_if_memtype == "ddr3") && ($options(-write_deskew) == "static")} {
			if {$options(-dll_length) == 12} {
				set options(-dll_length) 10
			}
			if {$options(-dll_length) != 0} {
				lappend tccs_params STATIC_DESKEW_$options(-dll_length)
			} else {
				# No DLL length dependency
				lappend tccs_params STATIC_DESKEW
			}
		} elseif {($mem_if_memtype == "ddr3") && ($options(-write_deskew) == "dynamic")} {
			lappend tccs_params DYNAMIC_DESKEW
		}
		if {$options(-ddr3_discrete) == 0 && $options(-write_deskew) != "none"} {
			set mode [hps_sdram_p0_get_deskew_freq_range $tccs_params $period]
			if {$mode == [list]} {
				post_message -type critical_warning "Memory interface with period $period and write $options(-write_deskew) deskew does not fall in a supported frequency range"
			} elseif {[lindex $mode 0] != [list]} {
				lappend tccs_params [lindex $mode 0]
				puts $tccs_params
			}
		}
		if {[catch {get_io_standard_node_delay -dst TCCS_LEAD -io_standard $io_std -parameters $tccs_params} tccs_lead] != 0 || $tccs_lead == "" || $tccs_lead == 0 || \
				[catch {get_io_standard_node_delay -dst TCCS_LAG -io_standard $io_std -parameters $tccs_params} tccs_lag] != 0 || $tccs_lag == "" || $tccs_lag == 0 } {
			set family $TimeQuestInfo(family)
			error "Missing $family timing model for tCCS of $io_std $tccs_params"
		} else {
			return [list $tccs_lead $tccs_lag]
		}
	}
}

# For static deskew, get the frequency range of the given configuration
# Return triplet {mode min_freq max_freq}
proc hps_sdram_p0_get_deskew_freq_range {timing_params period} {
	set mode [list]
	# freq_range list should be sorted from low to high
	if {[lindex $timing_params 2] == "STATIC_DESKEW_8" || [lindex $timing_params 2] == "STATIC_DESKEW_10"}  {
		# These modes have more than 2 freq ranges
		set range_list [list LOW HIGH]
	} else {
		# Just 1 freq range
		set range_list [list [list]]
	}
	set freq_mode [list]
	foreach freq_range $range_list {
		if {[catch {get_micro_node_delay -micro MIN -parameters [concat $timing_params $freq_range]} min_freq] != 0 || $min_freq == "" ||
			[catch {get_micro_node_delay -micro MAX -parameters [concat $timing_params $freq_range]} max_freq] != 0 || $max_freq == ""} {
			# Invalid mode
		} else {
			set max_freq_period [expr 1000.0 / $min_freq]
			set min_freq_period [expr 1000.0 / $max_freq]
			lappend freq_mode [list $freq_range $min_freq $max_freq]
			if {$period >= $min_freq_period && $period <= $max_freq_period} {
				set mode [lindex $freq_mode end]
				break
			}
		}
	}
	if {$mode == [list] && $freq_mode != [list]} {
		if {$period < $min_freq_period} {
			# Fastest mode
			set mode [lindex $freq_mode end]
		} else {
			# Slowest mode
			set mode [lindex $freq_mode 0]
		}
	}
	return $mode
}




# Return a tuple of setup,hold time for read capture
proc hps_sdram_p0_get_tsw { mem_if_memtype dqs_list period args} {
	global TimeQuestInfo
	array set options [list "-read_deskew" "none" "-dll_length" 0 "-config_period" 0 "-ddr3_discrete" 0]
	foreach {option value} $args {
		if {[array names options -exact "$option"] != ""} {
			set options($option) $value
		} else {
			error "ERROR: Unknown get_tsw option $option (with value $value; args are $args)"
		}
	}

	set interface_type [hps_sdram_p0_get_io_interface_type $dqs_list]
	if {$interface_type == "VHPAD"} {
		set interface_type "HPAD"
	}
	set io_std [hps_sdram_p0_get_io_standard [lindex $dqs_list 0]]

	if {$interface_type != "" && $interface_type != "UNKNOWN" && $io_std != "" && $io_std != "UNKNOWN"} {
		package require ::quartus::ddr_timing_model
		set family $TimeQuestInfo(family)
		set tsw_params [list IO $interface_type]
		if {$options(-ddr3_discrete) == 1} {
			lappend tsw_params NONLEVELED
		} elseif {$options(-read_deskew) == "static"} {
			if {$options(-dll_length) != 0} {
				lappend tsw_params STATIC_DESKEW_$options(-dll_length)
			} else {
				# No DLL length dependency
				lappend tsw_params STATIC_DESKEW
			}
		} elseif {$options(-read_deskew) == "dynamic"} {
			lappend tsw_params DYNAMIC_DESKEW
		}

		if {[catch {get_io_standard_node_delay -dst TSU -io_standard $io_std -parameters $tsw_params} tsw_setup] != 0 || $tsw_setup == "" || $tsw_setup == 0 || \
				[catch {get_io_standard_node_delay -dst TH -io_standard $io_std -parameters $tsw_params} tsw_hold] != 0 || $tsw_hold == "" || $tsw_hold == 0 } {
			error "Missing $family timing model for tSW of $io_std $tsw_params"
		} else {
			# Derate tSW for DDR2 on VPAD in CIII Q240 parts
			# The tSW for HPADs and for other interface types on C8 devices
			# have a large guardband, so derating for them is not required
			if {[get_part_info -package -pin_count $TimeQuestInfo(part)] == "PQFP 240"} {
				if {[catch {get_io_standard_node_delay -dst TSU -io_standard $io_std -parameters [list IO $interface_type Q240_DERATING]} tsw_setup_derating] != 0 || $tsw_setup_derating == 0 || \
						[catch {get_io_standard_node_delay -dst TH -io_standard $io_std -parameters [list IO $interface_type Q240_DERATING]} tsw_hold_derating] != 0 || $tsw_hold_derating == 0} {
					set f "$io_std/$interface_type/$family"
					switch -glob $f {
						"SSTL_18*/VPAD/Cyclone III"  {
							set tsw_setup_derating 50
							set tsw_hold_derating 135
						}
						"SSTL_18*/VPAD/Cyclone IV E"  {
							set tsw_setup_derating 50
							set tsw_hold_derating 135
						}						
						default {
							set tsw_setup_derating 0
							set tsw_hold_derating 0
						}
					}
				}
				incr tsw_setup $tsw_setup_derating
				incr tsw_hold $tsw_hold_derating
			}
			return [list $tsw_setup $tsw_hold]
		}
	}
}

# ----------------------------------------------------------------
#
proc hps_sdram_p0_get_fitter_report_pin_info_from_report {target_pin info_type pin_report_id} {
#
# Description: Gets the report field for the given pin in the given report
#
# ----------------------------------------------------------------
	set pin_name_column [hps_sdram_p0_get_report_column $pin_report_id "Name"]
	set info_column [hps_sdram_p0_get_report_column $pin_report_id $info_type]
	set result ""

	if {$pin_name_column == 0 && 0} {
		set row_index [get_report_panel_row_index -id $pin_report_id $target_pin]
		if {$row_index != -1} {
			set row [get_report_panel_row -id $pin_report_id -row $row_index]
			set result [lindex $row $info_column]
		}
	} else {
		set report_rows [get_number_of_rows -id $pin_report_id]
		for {set row_index 1} {$row_index < $report_rows && $result == ""} {incr row_index} {
			set row [get_report_panel_row -id $pin_report_id -row $row_index]
			set pin [lindex $row $pin_name_column]
			if {$pin == $target_pin} {
				set result [lindex $row $info_column]
			}
		}
	}
	return $result
}

# ----------------------------------------------------------------
#
proc hps_sdram_p0_get_fitter_report_pin_info {target_pin info_type preferred_report_id {found_report_id_name ""}} {
#
# Description: Gets the report field for the given pin by searching through the
#              input, output and bidir pin reports
#
# ----------------------------------------------------------------
	if {$found_report_id_name != ""} {
		upvar 1 $found_report_id_name found_report_id
	}
	set found_report_id -1
	set result ""
	if {$preferred_report_id == -1} {
		set pin_report_list [list "Fitter||Resource Section||Bidir Pins" "Fitter||Resource Section||Input Pins" "Fitter||Resource Section||Output Pins"]
		for {set pin_report_index 0} {$pin_report_index != [llength $pin_report_list] && $result == ""} {incr pin_report_index} {
			set pin_report_id [get_report_panel_id [lindex $pin_report_list $pin_report_index]]
			if {$pin_report_id != -1} {
				set result [hps_sdram_p0_get_fitter_report_pin_info_from_report $target_pin $info_type $pin_report_id]
				if {$result != ""} {
					set found_report_id $pin_report_id
				}
			} else {
				post_message -type error "hps_sdram_p0_pin_map.tcl: Failed to find fitter report. If report timing is run after an ECO, the user must set_global_assignment -name ECO_REGENERATE_REPORT ON in hps_sdram_p0.qsf and in hps_sdram_p0_pin_assignment.tcl files and rerun ECO and STA"
			}
		}
	} else {
		set result [hps_sdram_p0_get_fitter_report_pin_info_from_report $target_pin $info_type $preferred_report_id]
		if {$result != ""} {
			set found_report_id $preferred_report_id
		}
	}
	return $result
}
# ----------------------------------------------------------------
#
proc hps_sdram_p0_get_fitter_report_pin_io_type_info {target_pin} {
#
# Description: Gets the type of IO, either column or row for
# a given pin. If none found then "" is returned.
#
# ----------------------------------------------------------------
	set result ""
	set pin_report_id [get_report_panel_id "Fitter||Resource Section||All Package Pins"]
	if {$pin_report_id != -1} {
		set pin_name_column [hps_sdram_p0_get_report_column $pin_report_id "Pin Name/Usage"]
		set info_column [hps_sdram_p0_get_report_column $pin_report_id "I/O Type"]
		if {$pin_name_column == 0 && 0} {
			set row_index [get_report_panel_row_index -id $pin_report_id $target_pin]
			if {$row_index != -1} {
				set row [get_report_panel_row -id $pin_report_id -row $row_index]
				set result [lindex $row $info_column]
			}
		} else {
			set report_rows [get_number_of_rows -id $pin_report_id]
			for {set row_index 1} {$row_index < $report_rows && $result == ""} {incr row_index} {
				set row [get_report_panel_row -id $pin_report_id -row $row_index]
				set pin [lindex $row $pin_name_column]
				if {$pin == $target_pin} {
					set result [lindex $row $info_column]
				}
			}
		}
	} else {
		set pin_report_id [get_report_panel_id "Fitter||Resource Section||DQS Summary"]
		if {$pin_report_id != -1} {
		
			set report_rows [get_number_of_rows -id $pin_report_id]
			set pin_name_column [hps_sdram_p0_get_report_column $pin_report_id "Name"]
			set info_column [hps_sdram_p0_get_report_column $pin_report_id "I/O Edge"]
			
			for {set row_index 1} {$row_index < $report_rows && $result == ""} {incr row_index} {
				set row [get_report_panel_row -id $pin_report_id -row $row_index]
				set pin [lindex $row $pin_name_column]
				regsub -all {[ \r\t\n]+} $pin "" pin_no_whitespace
				if {$pin_no_whitespace == $target_pin} {
					set result [lindex $row $info_column]
				}
			}
			
			if {($result == "Bottom") || ($result == "Top")} {
				set result "Column I/O"
			} elseif {($result == "Left") || ($result == "Right")} {
				set result "Row I/O"
			}
		}
	}

	return $result
}
# ----------------------------------------------------------------
#
proc hps_sdram_p0_get_io_interface_type {pin_list} {
#
# Description: Gets the type of pin that the given pins are placed on
#              either (HPAD, VPAD, HYBRID, "", or UNKNOWN).
#              "" is returned if pin_list is empty
#              UNKNOWN is returned if an error was encountered
#              This function assumes the fitter has already completed and the
#              compiler report has been loaded.
#
# ----------------------------------------------------------------
	set preferred_report_id -1
	set interface_type ""
	foreach target_pin $pin_list {
		set io_bank [hps_sdram_p0_get_fitter_report_pin_info $target_pin "I/O Bank" $preferred_report_id preferred_report_id]
		if {[regexp -- {^([0-9]+)[A-Z]*} $io_bank -> io_bank_number]} {
			if {$io_bank_number == 1 || $io_bank_number == 2 || $io_bank_number == 5 || $io_bank_number == 6} {
				# Row I/O
				if {$interface_type == ""} {
					set interface_type "HPAD"
				} elseif {$interface_type == "VIO"} {
					set interface_type "HYBRID"
				}
			} elseif {$io_bank_number == 3 || $io_bank_number == 4 || $io_bank_number == 7 || $io_bank_number == 8} {
				if {$interface_type == ""} {
					set interface_type "VPAD"
				} elseif {$interface_type == "HIO"} {
					set interface_type "HYBRID"
				}
			} else {
				post_message -type critical_warning "Unknown I/O bank $io_bank for pin $target_pin"
				# Assume worst case performance (mixed HIO/VIO interface)
				set interface_type "HYBRID"
			}
		}
	}
	return $interface_type
}


# ----------------------------------------------------------------
#
proc hps_sdram_p0_get_report_column { report_id str} {
#
# Description: Gets the report column index with the given header string
#
# ----------------------------------------------------------------
	set target_col [get_report_panel_column_index -id $report_id $str]
	if {$target_col == -1} {
		error "Cannot find $str column"
	}
	return $target_col
}

proc hps_sdram_p0_traverse_to_dll_id {dqs_pin msg_list_name} {
	upvar 1 $msg_list_name msg_list
	set dqs_pin_id [get_atom_node_by_name -name $dqs_pin]
	set dqs_to_dll_path [list {IO_PAD fanout PADOUT} {IO_IBUF fanout O} {DQS_DELAY_CHAIN fanin DELAYCTRLIN} {DLL end DELAYCTRLOUT}]
	set dll_id_list [hps_sdram_p0_traverse_atom_path $dqs_pin_id -1 $dqs_to_dll_path]
	set dll_id -1
	if {[llength $dll_id_list] == 1} {
		set dll_atom_oterm_pair [lindex $dll_id_list 0]
		set dll_id [lindex $dll_atom_oterm_pair 0]
	} elseif {[llength $dll_id_list] > 1} {
		lappend msg_list "Error: Found more than 1 DLL"
	} else {
		lappend msg_list "Error: DLL not found"
	}
	return $dll_id
}

proc hps_sdram_p0_traverse_to_dqs_delaychain_id {dqs_pin msg_list_name} {
	upvar 1 $msg_list_name msg_list
	set dqs_pin_id [get_atom_node_by_name -name $dqs_pin]
	set dqs_to_delaychain_path [list {IO_PAD fanout PADOUT} {IO_IBUF fanout O} {DQS_DELAY_CHAIN atom}]
	set delaychain_id_list [hps_sdram_p0_traverse_atom_path $dqs_pin_id -1 $dqs_to_delaychain_path]
	set delaychain_id -1
	if {[llength $delaychain_id_list] == 1} {
		set delaychain_atom_oterm_pair [lindex $delaychain_id_list 0]
		set delaychain_id [lindex $delaychain_atom_oterm_pair 0]
	} elseif {[llength $delaychain_id_list] > 1} {
		lappend msg_list "Error: Found more than 1 DQS delaychain"
	} else {
		lappend msg_list "Error: DQS delaychain not found"
	}
	return $delaychain_id
}

proc hps_sdram_p0_get_dqs_phase { dqs_pins } {
	set dqs_phase -100
	set dqs0 [lindex $dqs_pins 0]
	if {$dqs0 != ""} {
		set dqs_delay_chain_id [hps_sdram_p0_traverse_to_dqs_delaychain_id $dqs0 msg_list]
		if {$dqs_delay_chain_id != -1} {
			set dqs_phase [get_atom_node_info -key INT_DQS_PHASE_SHIFT -node $dqs_delay_chain_id]
		}
	}

	set dqs_phase [expr {$dqs_phase / 100}]

	if {$dqs_phase < 0} {
		set dqs_phase 90
		post_message -type critical_warning "Unable to determine DQS delay chain phase shift.  Assuming default setting of $dqs_phase"
	}

	return $dqs_phase
}

proc hps_sdram_p0_get_dqs_period { dqs_pins } {
	set dqs_period -100
	set dqs0 [lindex $dqs_pins 0]
	if {$dqs0 != ""} {
		set dll_id [hps_sdram_p0_traverse_to_dll_id $dqs0 msg_list]
		if {$dll_id != -1} {
			set dqs_period_str [get_atom_node_info -key TIME_INPUT_FREQUENCY -node $dll_id]
			if {[regexp {(.*) ps} $dqs_period_str matched dqs_period_ps] == 1} {
				set dqs_period [expr $dqs_period_ps/1000.0]
			} elseif {[regexp {(.*) ps} $dqs_period_str matched dqs_period_ns] == 1} {
				set dqs_period $dqs_period_ns
			}
			
		}
	}

	if {$dqs_period < 0} {
		set dqs_period 0
		post_message -type critical_warning "Unable to determine DQS delay chain period.  Assuming default setting of $dqs_period"
	}

	return $dqs_period
}

proc hps_sdram_p0_get_operating_conditions_number {} {
	set cur_operating_condition [get_operating_conditions]
	set counter 0
	foreach_in_collection op [get_available_operating_conditions] {
		if {[string compare $cur_operating_condition $op] == 0} {
			return $counter
		}
		incr counter
	}
	return $counter
}

proc hps_sdram_p0_get_rzq_pins { instname all_rzq_pins } {
	upvar $all_rzq_pins rzqpins
	load_package atoms
	read_atom_netlist
	set rzq_pins [ list ]
	set entity_names_on [ hps_sdram_p0_are_entity_names_on ]
	
	# Get all termination atoms, to which rzqpin should be attached
	set_project_mode -always_show_entity_name off
	set instance ${instname}*
	set atoms [get_atom_nodes -type TERMINATION -matching [escape_brackets $instance] ]
	post_message -type info "Number of Termination Atoms: [get_collection_size $atoms]"
	foreach_in_collection term_atom $atoms { 
		set rzq_pin ""
		set atom $term_atom
		set term_atom_name [get_atom_node_info -key name -node $term_atom] 
		post_message -type info "Found Termination Atom $term_atom_name"
		set type [get_atom_node_info -key type -node $term_atom] 
		
		# Check until you traverse to an IO_PAD for the RZQ Pin
		while { ![regexp IO_PAD $type ] } { 
			set name [get_atom_node_info -key name -node $atom] 
			set iterms [get_atom_iports -node $atom]
			set iterm_size [llength $iterms]
			# Check for Multiple Inputs
			if { $iterm_size > 1 } {
				post_message -type error " Multiple inputs to a node:$name attached to a  Termination_Atom:$term_atom_name "
				break
			
			}
			
			foreach iterm $iterms { 
				set fanin	[get_atom_port_info -node $atom -type iport -port_id $iterm -key fanin]
				set atom [lindex $fanin 0]
				set type [get_atom_node_info -key type -node $atom]
				set rzq_pin [get_atom_node_info -key name -node $atom]
			}		
		}
			
		lappend rzq_pins [ join $rzq_pin ]
	}

	set_project_mode -always_show_entity_name qsf
	set rzqpins $rzq_pins
}

proc hps_sdram_p0_get_ddr_pins { instname allpins } {
	# We need to make a local copy of the allpins associative array
	upvar allpins pins
	global ::GLOBAL_hps_sdram_p0_dqs_group_size
	global ::GLOBAL_hps_sdram_p0_number_of_dqs_groups

	set synthesis_flow 0
	set sta_flow 0
	if { $::TimeQuestInfo(nameofexecutable) == "quartus_map" } {
		set synthesis_flow 1
	} elseif { $::TimeQuestInfo(nameofexecutable) == "quartus_sta" } {
		set sta_flow 1
	}
	
	set dqs_inst "altdq_dqs2_inst|"
	set dqs_pins [ list ]
	set dqsn_pins [ list ]
	set q_groups [ list ]
	set dqs_in_clocks [ list ]
	set dqs_out_clocks [ list ]
	set dqsn_out_clocks [ list ]
	
	for { set i 0 } { $i < $::GLOBAL_hps_sdram_p0_number_of_dqs_groups } { incr i } {
	
		set dqs_string ${instname}|p0|umemphy|uio_pads|dq_ddio\[$i\].ubidir_dq_dqs|${dqs_inst}obuf_os_0|o
		set dqs_local_pins [ hps_sdram_p0_get_names_in_collection [ get_fanouts $dqs_string ] ]
		set dqsn_string ${instname}|p0|umemphy|uio_pads|dq_ddio\[$i\].ubidir_dq_dqs|${dqs_inst}obuf_os_bar_0|o
		set dqsn_local_pins [ hps_sdram_p0_get_names_in_collection [ get_fanouts $dqsn_string ] ]

		set dm_string ${instname}|p0|umemphy|uio_pads|dq_ddio\[$i\].ubidir_dq_dqs|${dqs_inst}extra_output_pad_gen\[0\].obuf_1|o
		set dm_local_pins [ hps_sdram_p0_get_names_in_collection [ get_fanouts $dm_string ] ]

		set dqs_in_clock(dqs_pin) [ lindex $dqs_local_pins 0 ]
		set dqs_in_clock(dqs_shifted_pin) "${instname}|p0|umemphy|uio_pads|dq_ddio[$i].ubidir_dq_dqs|${dqs_inst}dqs_delay_chain|dqsbusout"
		set dqs_in_clock(div_name) "${instname}|div_clock_$i"
		set dqs_in_clock(div_pin) "${instname}|p0|umemphy|uread_datapath|read_capture_clk_div2[$i]"

		lappend dqs_in_clocks [ array get dqs_in_clock ]

		set dqs_out_clock(dst) [ lindex $dqs_local_pins 0 ]
		set dqs_out_clock(src) $dqs_string
		set dqs_out_clock(dm_pin) [ lindex $dm_local_pins 0 ]
		set dqsn_out_clock(dst) [ lindex $dqsn_local_pins 0 ]
		set dqsn_out_clock(src) $dqsn_string
		set dqsn_out_clock(dm_pin) [ lindex $dm_local_pins 0 ]
		lappend dqs_out_clocks [ array get dqs_out_clock ]
		lappend dqsn_out_clocks [ array get dqsn_out_clock ]

		set q_group [ list ]
		for { set j 0 } { $j < $::GLOBAL_hps_sdram_p0_dqs_group_size } { incr j } { 
			set index [ expr $i * $::GLOBAL_hps_sdram_p0_dqs_group_size + $j ]
			set q_string ${instname}|p0|umemphy|uio_pads|dq_ddio\[$i\].ubidir_dq_dqs|${dqs_inst}pad_gen\[${j}\].data_out|o
			set tmp_q_pins [ hps_sdram_p0_get_names_in_collection [ get_fanouts $q_string ] ]
			
			lappend q_group $tmp_q_pins
		}

		if { [llength $dqs_local_pins] != 1} { post_sdc_message critical_warning "Could not find DQS pin number $i" } 
		if { [llength $dqsn_local_pins] != 1} { post_sdc_message critical_warning "Could not find DQSn pin number $i" } 
		if { [llength $q_group] != $::GLOBAL_hps_sdram_p0_dqs_group_size} { 
			post_sdc_message critical_warning "Could not find correct number of D pins for K pin $i. \
				Found [llength $q_group] pins. Expecting ${::GLOBAL_hps_sdram_p0_dqs_group_size}." 
		}

		lappend dqs_pins [ join $dqs_local_pins ]
		lappend dqsn_pins [ join $dqsn_local_pins ]
		lappend dm_pins [ join $dm_local_pins ]
		lappend q_groups [ join $q_group ]
	}

	set pins(dqs_pins) $dqs_pins
	set pins(dqsn_pins) $dqsn_pins
	set pins(dm_pins) $dm_pins
	set pins(q_groups) $q_groups
	set pins(all_dq_pins) [ join [ join $q_groups ] ]
	set pins(dqs_in_clocks) $dqs_in_clocks
	set pins(dqs_out_clocks) $dqs_out_clocks
	set pins(dqsn_out_clocks) $dqsn_out_clocks

	set pins(all_dq_dm_pins) [ concat $pins(all_dq_pins) $pins(dm_pins) ]

	# Other Outputs

	set pins(ck_pins) [ list ]
	set pins(ckn_pins) [ list ]
	set pins(add_pins) [ list ]
	set pins(ba_pins) [ list ]
	set pins(cmd_pins) [ list ]
	set pins(reset_pins) [ list ]

	set patterns [ list ]
	lappend patterns ck_pins ${instname}|p0|umemphy|uio_pads|uaddr_cmd_pads|clock_gen[*].uclk_generator|pseudo_diffa_0|o
	lappend patterns ckn_pins ${instname}|p0|umemphy|uio_pads|uaddr_cmd_pads|clock_gen[*].uclk_generator|pseudo_diffa_0|obar

	set addr_cmd_postfix "acblock[*].ddio_out|dataout"
	lappend patterns reset_pins ${instname}|p0|umemphy|uio_pads|uaddr_cmd_pads|ureset_n_pad|${addr_cmd_postfix}

	lappend patterns add_pins ${instname}|p0|umemphy|uio_pads|uaddr_cmd_pads|uaddress_pad|${addr_cmd_postfix}
	lappend patterns ba_pins ${instname}|p0|umemphy|uio_pads|uaddr_cmd_pads|ubank_pad|${addr_cmd_postfix}
	lappend patterns cmd_pins ${instname}|p0|umemphy|uio_pads|uaddr_cmd_pads|ucmd_pad|${addr_cmd_postfix}

	foreach {pin_type pattern} $patterns {
		set local_pins [ hps_sdram_p0_get_names_in_collection [ get_fanouts $pattern ] ]
		if {[llength $local_pins] == 0} {
			post_message -type critical_warning "Could not find pin of type $pin_type from pattern $pattern"
		} else {
			foreach pin [lsort -unique $local_pins] {
				lappend pins($pin_type) $pin
			}
		}
	}
	

	set pins(ac_pins) [ concat $pins(add_pins) $pins(ba_pins) $pins(cmd_pins) $pins(reset_pins)]
	set pins(ac_wo_reset_pins) [ concat $pins(add_pins) $pins(ba_pins) $pins(cmd_pins)]

	set pins(afi_ck_pins) ${instname}|p0|umemphy|afi_clk_reg
	set pins(dqs_enable_regs_pins) ${instname}|p0|umemphy|uio_pads|dq_ddio[*].ubidir_dq_dqs|altdq_dqs2_inst|dqs_enable_ctrl~DQSENABLEOUT_DFF
	set inst_driver ""
	set pins(driver_core_ck_pins) ""
	if {[regexp -nocase {if[0-9]$} $instname] == 1} {
		set inst_driver [regsub {if[0-9]$} $instname "d0"]
	}
	if {[string compare -nocase $inst_driver ""] != 0} {
		set pins(driver_core_ck_pins) ${inst_driver}|traffic_generator_0|pnf_per_bit_persist[0]
	}

	#############
	# PLL STUFF #
	#############

	set pll_ck_clock "_UNDEFINED_PIN_"
	set pll_dq_write_clock "_UNDEFINED_PIN_"
	set pll_write_clock "_UNDEFINED_PIN_"
	set pll_ref_clock "_UNDEFINED_PIN_"
	set pll_ref_clock_input_buffer "_UNDEFINED_PIN_"
	set pll_driver_core_clock "_UNDEFINED_PIN_"

	set msg_list [ list ]

	# CLOCK OUTPUT PLL
	set pll_ck_clock_id [hps_sdram_p0_get_output_clock_id $pins(ck_pins) "CK Output" msg_list]
	if {$pll_ck_clock_id == -1} {
		foreach {msg_type msg} $msg_list {
			post_message -type $msg_type "hps_sdram_p0_pin_map.tcl: $msg"
		}
		post_message -type critical_warning "hps_sdram_p0_pin_map.tcl: Failed to find PLL clock for pins [join $pins(ck_pins)]"
	} else {
		set pll_ck_clock [hps_sdram_p0_get_pll_clock_name $pll_ck_clock_id]
	}
	set pins(pll_ck_clock) $pll_ck_clock


	# DRIVER CORE CLOCK PLL
	if {[string compare -nocase $pins(driver_core_ck_pins) ""] != 0 && [get_collection_size [get_registers -nowarn $pins(driver_core_ck_pins)]] > 0} {
		set pll_driver_core_clock_id [hps_sdram_p0_get_output_clock_id $pins(driver_core_ck_pins) "DRIVER CORE CK" msg_list]
		if {$pll_driver_core_clock_id == -1} {
			foreach {msg_type msg} $msg_list {
				post_message -type $msg_type "hps_sdram_p0_pin_map.tcl: $msg"
			}
			post_message -type critical_warning "hps_sdram_p0_pin_map.tcl: Failed to find PLL clock for pins [join $pins(driver_core_ck_pins)]"
		} else {
			set pll_driver_core_clock [hps_sdram_p0_get_pll_clock_name $pll_driver_core_clock_id]
		}
	}
	set pins(pll_driver_core_clock) $pll_driver_core_clock



	# DQ PLL
	set pll_dq_write_clock_id [hps_sdram_p0_get_output_clock_id [ join [ join $pins(q_groups) ]] "Write DQ CK" msg_list 20]
	if {$pll_dq_write_clock_id == -1} {
		foreach {msg_type msg} $msg_list {
			post_message -type $msg_type "hps_sdram_p0_pin_map.tcl: $msg"
		}
		post_message -type critical_warning "hps_sdram_p0_pin_map.tcl: Failed to find PLL clock for pins [ join [ join $pins(q_groups) ]]"
	} else {
		set pll_dq_write_clock [hps_sdram_p0_get_pll_clock_name $pll_dq_write_clock_id]
	}
	set pins(pll_dq_write_clock) $pll_dq_write_clock
	
	# DQS PLL
	set pll_write_clock_id [hps_sdram_p0_get_output_clock_id $pins(dqs_pins) "Write CK" msg_list 20]
	if {$pll_write_clock_id == -1} {
		foreach {msg_type msg} $msg_list {
			post_message -type $msg_type "hps_sdram_p0_pin_map.tcl: $msg"
		}
		post_message -type critical_warning "hps_sdram_p0_pin_map.tcl: Failed to find PLL clock for pins $pins(dqs_pins)"
	} else {
		set pll_write_clock [hps_sdram_p0_get_pll_clock_name $pll_write_clock_id]
	}
	set pins(pll_write_clock) $pll_write_clock	







	set entity_names_on [ hps_sdram_p0_are_entity_names_on ]

	# Instance name prefix
	
	set prefix [ string map "| |*:" $instname ]
	set prefix "*:$prefix"

	#####################
	# READ CAPTURE DDIO #
	#####################

	# Pending ALTDQ_DQS fix
	# Half rate: separate read and write ALTDQ_DQS
	# Full rate: bidirectional ALTDQ_DQS

	set read_capture_ddio_prefix [expr { $entity_names_on ? \
		"$prefix|*:p0|*:umemphy|*:uio_pads|*:dq_ddio\[*\].ubidir_dq_dqs|*:" : \
		"$instname|p0|umemphy|uio_pads|dq_ddio\[*\].ubidir_dq_dqs|" }]

	set read_capture_ddio [list "${read_capture_ddio_prefix}${dqs_inst}*input_path_gen\[*\].capture_reg~DFFLO" \
								"${read_capture_ddio_prefix}${dqs_inst}*input_path_gen\[*\].aligned_input\[*\]"]
	set pins(read_capture_ddio) $read_capture_ddio

	###################
	# RESET REGISTERS #
	###################

	# the output of this flop feeds the asynchronous clear pin of the reset registers and should be false pathed
    # since the deassertion of the reset is synchronous with the use of a reset pipeline
    # normal timing analysis will take care that
	set afi_reset_reg $prefix|*:p0|*:umemphy|*:ureset|*:ureset_afi_clk|reset_reg[3]
	if { ! $entity_names_on } {
		set afi_reset_reg $instname|p0|umemphy|ureset|ureset_afi_clk|reset_reg[3]
	}
	set pins(afi_reset_reg) $afi_reset_reg


    # first flop of a synchronzier
    # sequencer issues multiple resets during calibration, reset is synced over from AFI to read capture clock domain
	set sync_reg $prefix|*:p0|*:umemphy|*:uread_datapath|read_buffering[*].seq_read_fifo_reset_sync
	if { ! $entity_names_on } {
		set sync_reg $instname|p0|umemphy|uread_datapath|read_buffering[*].seq_read_fifo_reset_sync
	}
	set pins(sync_reg) $sync_reg


	###############################
	# DATA RESYNCHRONIZATION FIFO #
	###############################

	set fifo_wraddress_reg $prefix|*:p0|*:umemphy|*:uread_datapath|read_buffering[*].read_subgroup[*].wraddress[*]
	if { ! $entity_names_on } {
		set fifo_wraddress_reg $instname|p0|umemphy|uread_datapath|read_buffering[*].read_subgroup[*].wraddress[*]
	}
	set pins(fifo_wraddress_reg) $fifo_wraddress_reg
	
	set fifo_rdaddress_reg $prefix|*:p0|*:umemphy|*:uread_datapath|read_buffering[*].read_subgroup[*].rdaddress[*]
	if { ! $entity_names_on } {
		set fifo_rdaddress_reg $instname|p0|umemphy|uread_datapath|read_buffering[*].read_subgroup[*].rdaddress[*]
	}
	set pins(fifo_rdaddress_reg) $fifo_rdaddress_reg		

	set fifo_wrdata_reg $prefix|*:p0|*:umemphy|*:uio_pads|*:dq_ddio[*].ubidir_dq_dqs|*:altdq_dqs2_inst|input_path_gen[*].read_fifo|*INPUT_DFF*
	if { ! $entity_names_on } {
		set fifo_wrdata_reg $instname|p0|umemphy|uio_pads|dq_ddio[*].ubidir_dq_dqs|altdq_dqs2_inst|input_path_gen[*].read_fifo|*INPUT_DFF*
	}
	set pins(fifo_wrdata_reg) $fifo_wrdata_reg

	set fifo_rddata_reg $prefix|*:p0|*:umemphy|*:uio_pads|*:dq_ddio[*].ubidir_dq_dqs|*:altdq_dqs2_inst|input_path_gen[*].read_fifo|dout[*]
	if { ! $entity_names_on } {
		set fifo_rddata_reg $instname|p0|umemphy|uio_pads|dq_ddio[*].ubidir_dq_dqs|altdq_dqs2_inst|input_path_gen[*].read_fifo|dout[*]
	}
	set pins(fifo_rddata_reg) $fifo_rddata_reg

}

proc hps_sdram_p0_initialize_ddr_db { ddr_db_par } {
	upvar $ddr_db_par local_ddr_db

	global ::GLOBAL_hps_sdram_p0_corename

	post_sdc_message info "Initializing DDR database for CORE $::GLOBAL_hps_sdram_p0_corename"
	set instance_list [hps_sdram_p0_get_core_instance_list $::GLOBAL_hps_sdram_p0_corename]
	# set local_ddr_db(instance_list) $instance_list

	foreach instname $instance_list {
		post_sdc_message info "Finding port-to-pin mapping for CORE: $::GLOBAL_hps_sdram_p0_corename INSTANCE: $instname"

		hps_sdram_p0_get_ddr_pins $instname allpins

		hps_sdram_p0_verify_ddr_pins allpins

		set local_ddr_db($instname) [ array get allpins ]
	}
}

proc hps_sdram_p0_verify_ddr_pins { pins_par } {
	upvar $pins_par pins

	# Verify Q groups
	set current_q_group_size -1
	foreach q_group $pins(q_groups) {
		set group_size [ llength $q_group ]
		if { $group_size == 0 } {
			post_message -type critical_warning "Q group of size 0"
		}
		if { $current_q_group_size == -1 } {
			set current_q_group_size $group_size
		} else {
			if { $current_q_group_size != $group_size } {
				post_message -type critical_warning "Inconsistent Q group size across groups"
			}
		}
	}

	# Verify DM pins
	set counted_dm_pins [ llength $pins(dm_pins) ]
	if { $::GLOBAL_hps_sdram_p0_number_of_dm_pins != $counted_dm_pins } {
		post_message -type critical_warning "Unexpected number of detected DM pins: $counted_dm_pins"
		post_message -type critical_warning "   expected: $::GLOBAL_hps_sdram_p0_number_of_dm_pins"
	}
	# Verify Address/Command/BA pins
	if { [ llength $pins(add_pins) ] == 0 } {
		post_message -type critical_warning "Address pins of size 0"
	}
	if { [ llength $pins(cmd_pins) ] == 0 } {
		post_message -type critical_warning "Command pins of size 0"
	}
	if { [ llength $pins(ba_pins) ] == 0 } {
		post_message -type critical_warning "BA pins of size 0"
	}
	if { [ llength $pins(reset_pins) ] == 0 } {
		post_message -type critical_warning "Reset pins of size 0"
	}	
}

proc hps_sdram_p0_get_all_instances_div_names { ddr_db_par } {
	upvar $ddr_db_par local_ddr_db

	set div_names [ list ]
	set instnames [ array names local_ddr_db ]
	foreach instance $instnames {
		array set pins $local_ddr_db($instance)

		foreach { dqs_in_clock_struct } $pins(dqs_in_clocks) {
			array set dqs_in_clock $dqs_in_clock_struct
			lappend div_names $dqs_in_clock(div_name)
		}
	}

	return $div_names
}

proc hps_sdram_p0_get_all_instances_dqs_pins { ddr_db_par } {
	upvar $ddr_db_par local_ddr_db

	set dqs_pins [ list ]
	set instnames [ array names local_ddr_db ]
	foreach instance $instnames {
		array set pins $local_ddr_db($instance)

		foreach { dqs_pin } $pins(dqs_pins) {
			lappend dqs_pins ${dqs_pin}_IN
			lappend dqs_pins ${dqs_pin}_OUT
		}
		foreach { dqsn_pin } $pins(dqsn_pins) {
			lappend dqs_pins ${dqsn_pin}_OUT
		}
		foreach { ck_pin } $pins(ck_pins) {
			lappend dqs_pins $ck_pin
		}
		foreach { ckn_pin } $pins(ckn_pins) {
			lappend dqs_pins $ckn_pin
		}
	}

	return $dqs_pins
}

proc hps_sdram_p0_dump_all_pins { ddr_db_par } {
	upvar $ddr_db_par local_ddr_db

	set instnames [ array names local_ddr_db ]

	set filename "${::GLOBAL_hps_sdram_p0_corename}_all_pins.txt"
	if [ catch { open $filename w 0777 } FH ] {
		post_message -type error "Can't open file < $filename > for writing"
	}

	post_message -type info "Dumping reference pin-map file: $filename"

	set script_name [ info script ]
	puts $FH "# PIN MAP for core < $::GLOBAL_hps_sdram_p0_corename >"
	puts $FH "#"
	puts $FH "# Generated by ${::GLOBAL_hps_sdram_p0_corename}_pin_assignments.tcl"
	puts $FH "#"
	puts $FH "# This file is for reference only and is not used by Quartus Prime"
	puts $FH "#"
	puts $FH ""

	foreach instance $instnames {
		array set pins $local_ddr_db($instance)

		puts $FH "INSTANCE: $instance"
		puts $FH "DQS: $pins(dqs_pins)"
		puts $FH "DQSn: $pins(dqsn_pins)"
		puts $FH "DQ: $pins(q_groups)"

		puts $FH "DM $pins(dm_pins)"

		puts $FH "CK: $pins(ck_pins)"
		puts $FH "CKn: $pins(ckn_pins)"

		puts $FH "ADD: $pins(add_pins)"
		puts $FH "CMD: $pins(cmd_pins)"
		puts $FH "RESET: $pins(reset_pins)"
		puts $FH "BA: $pins(ba_pins)"

		puts $FH "PLL CK: $pins(pll_ck_clock)"
		puts $FH "PLL DQ WRITE: $pins(pll_dq_write_clock)"
		puts $FH "PLL WRITE: $pins(pll_write_clock)"
		puts $FH "PLL DRIVER CORE: $pins(pll_driver_core_clock)"

		set i 0
		foreach dqs_in_clock_struct $pins(dqs_in_clocks) {
			array set dqs_in_clock $dqs_in_clock_struct
			puts $FH "DQS_IN_CLOCK DQS_PIN ($i): $dqs_in_clock(dqs_pin)"
			puts $FH "DQS_IN_CLOCK DQS_SHIFTED_PIN ($i): $dqs_in_clock(dqs_shifted_pin)"
			puts $FH "DQS_IN_CLOCK DIV_NAME ($i): $dqs_in_clock(div_name)"
			puts $FH "DQS_IN_CLOCK DIV_PIN ($i): $dqs_in_clock(div_pin)"

			incr i
		}

		set i 0
		foreach dqs_out_clock_struct $pins(dqs_out_clocks) {
			array set dqs_out_clock $dqs_out_clock_struct
			puts $FH "DQS_OUT_CLOCK SRC ($i): $dqs_out_clock(src)"
			puts $FH "DQS_OUT_CLOCK DST ($i): $dqs_out_clock(dst)"
			puts $FH "DQS_OUT_CLOCK DM ($i): $dqs_out_clock(dm_pin)"

			incr i
		}

		set i 0
		foreach dqsn_out_clock_struct $pins(dqsn_out_clocks) {
			array set dqsn_out_clock $dqsn_out_clock_struct
			puts $FH "DQSN_OUT_CLOCK SRC ($i): $dqsn_out_clock(src)"
			puts $FH "DQSN_OUT_CLOCK DST ($i): $dqsn_out_clock(dst)"
			puts $FH "DQSN_OUT_CLOCK DM ($i): $dqsn_out_clock(dm_pin)"

			incr i
		}

		puts $FH "READ CAPTURE DDIO: $pins(read_capture_ddio)"
		puts $FH "AFI RESET REGISTERS: $pins(afi_reset_reg)"
		puts $FH "SYNCHRONIZERS: $pins(sync_reg)"
		puts $FH "SYNCHRONIZATION FIFO WRITE ADDRESS REGISTERS: $pins(fifo_wraddress_reg)"
		puts $FH "SYNCHRONIZATION FIFO WRITE REGISTERS: $pins(fifo_wrdata_reg)"
		puts $FH "SYNCHRONIZATION FIFO READ REGISTERS: $pins(fifo_rddata_reg)"

		puts $FH ""
		puts $FH "#"
		puts $FH "# END OF INSTANCE: $instance"
		puts $FH ""
	}

	close $FH
}
proc hps_sdram_p0_dump_static_pin_map { ddr_db_par filename } {
	upvar $ddr_db_par local_ddr_db

	set instnames [ array names local_ddr_db ]

	if [ catch { open $filename w 0777 } FH ] {
		post_message -type error "Can't open file < $filename > for writing"
	}

	post_message -type info "Dumping static pin-map file: $filename"

	puts $FH "# AUTO-GENERATED static pin map for core < $::GLOBAL_hps_sdram_p0_corename >"
	puts $FH ""
	puts $FH "proc ${::GLOBAL_hps_sdram_p0_corename}_initialize_static_ddr_db { ddr_db_par } {"
	puts $FH "   upvar \$ddr_db_par local_ddr_db"
	puts $FH ""

	foreach instname $instnames {
		array set pins $local_ddr_db($instname)

		puts $FH "   # Pin Mapping for instance: $instname"

		hps_sdram_p0_static_map_expand_list $FH pins dqs_pins
		hps_sdram_p0_static_map_expand_list $FH pins dqsn_pins

		hps_sdram_p0_static_map_expand_list_of_list $FH pins q_groups

		puts $FH ""
		puts $FH "   set pins(all_dq_pins) \[ join \[ join \$pins(q_groups) \] \]"

		hps_sdram_p0_static_map_expand_list $FH pins dm_pins

		hps_sdram_p0_static_map_expand_list $FH pins ck_pins
		hps_sdram_p0_static_map_expand_list $FH pins ckn_pins

		hps_sdram_p0_static_map_expand_list $FH pins add_pins
		hps_sdram_p0_static_map_expand_list $FH pins cmd_pins
		hps_sdram_p0_static_map_expand_list $FH pins reset_pins
		hps_sdram_p0_static_map_expand_list $FH pins ba_pins

		puts $FH ""
		puts $FH "   set pins(ac_pins) \[ concat \$pins(add_pins) \$pins(ba_pins) \$pins(cmd_pins) \$pins(reset_pins)\]"

		hps_sdram_p0_static_map_expand_string $FH pins pll_ck_clock
		hps_sdram_p0_static_map_expand_string $FH pins pll_dq_write_clock
		hps_sdram_p0_static_map_expand_string $FH pins pll_write_clock
		hps_sdram_p0_static_map_expand_string $FH pins pll_driver_core_clock

		puts $FH ""
		puts $FH "   set dqs_in_clocks \[ list \]"
		set i 0
		foreach dqs_in_clock_struct $pins(dqs_in_clocks) {
			array set dqs_in_clock $dqs_in_clock_struct
			puts $FH "   # DIV Clock ($i)"
			puts $FH "   set dqs_in_clock(dqs_pin) $dqs_in_clock(dqs_pin)"
			puts $FH "   set dqs_in_clock(dqs_shifted_pin) $dqs_in_clock(dqs_shifted_pin)"
			puts $FH "   set dqs_in_clock(div_name) $dqs_in_clock(div_name)"
			puts $FH "   set dqs_in_clock(div_pin) $dqs_in_clock(div_pin)"

			puts $FH "   lappend dqs_in_clocks \[ array get dqs_in_clock \]"

			incr i
		}
		puts $FH "   set pins(dqs_in_clocks) \$dqs_in_clocks"


		puts $FH ""
		puts $FH "   set dqs_out_clocks \[ list \]"
		set i 0
		foreach dqs_out_clock_struct $pins(dqs_out_clocks) {
			array set dqs_out_clock $dqs_out_clock_struct
			puts $FH "   # DQS OUT Clock ($i)"
			puts $FH "   set dqs_out_clock(src) $dqs_out_clock(src)"
			puts $FH "   set dqs_out_clock(dst) $dqs_out_clock(dst)"
			puts $FH "   set dqs_out_clock(dm_pin) $dqs_out_clock(dm_pin)"
			puts $FH "   lappend dqs_out_clocks \[ array get dqs_out_clock \]"

			incr i
		}
		puts $FH "   set pins(dqs_out_clocks) \$dqs_out_clocks"

		puts $FH ""
		puts $FH "   set dqsn_out_clocks \[ list \]"
		set i 0
		foreach dqsn_out_clock_struct $pins(dqsn_out_clocks) {
			array set dqsn_out_clock $dqsn_out_clock_struct
			puts $FH "   # DQSN OUT Clock ($i)"
			puts $FH "   set dqsn_out_clock(src) $dqsn_out_clock(src)"
			puts $FH "   set dqsn_out_clock(dst) $dqsn_out_clock(dst)"
			puts $FH "   set dqsn_out_clock(dm_pin) $dqsn_out_clock(dm_pin)"
			puts $FH "   lappend dqsn_out_clocks \[ array get dqsn_out_clock \]"

			incr i
		}
		puts $FH "   set pins(dqsn_out_clocks) \$dqsn_out_clocks"

		hps_sdram_p0_static_map_expand_string $FH pins read_capture_ddio
		hps_sdram_p0_static_map_expand_string $FH pins afi_reset_reg
		hps_sdram_p0_static_map_expand_string $FH pins sync_reg
		hps_sdram_p0_static_map_expand_string $FH pins fifo_wraddress_reg 
		hps_sdram_p0_static_map_expand_string $FH pins fifo_wrdata_reg 
		hps_sdram_p0_static_map_expand_string $FH pins fifo_rddata_reg

		puts $FH ""
		puts $FH "   set local_ddr_db($instname) \[ array get pins \]"
	}

	puts $FH "}"

	close $FH
}
