package require -exact qsys 16.0
package require -exact altera_terp 1.0


set_module_property DESCRIPTION "Freeze conduit interface signals between Non-static(PR) Region & Static Region during Partial Reconfiguration"
set_module_property NAME conduit_bridge
set_module_property VERSION __ACDS_VERSION_SHORT__
set_module_property INTERNAL false
set_module_property OPAQUE_ADDRESS_MAP true
set_module_property DISPLAY_NAME "Freeze Conduit Bridge"
set_module_property GROUP "Generic Component"
set_module_property INSTANTIATE_IN_SYSTEM_MODULE true
set_module_property EDITABLE true
set_module_property REPORT_TO_TALKBACK false
set_module_property ALLOW_GREYBOX_GENERATION false
set_module_property REPORT_HIERARCHY false
set_module_property ELABORATION_CALLBACK elaborate
set_module_property VALIDATION_CALLBACK validate

add_parameter NUM_INTERFACES INTEGER 1
set_parameter_property NUM_INTERFACES DISPLAY_NAME "Number of Conduit Interfaces"
set_parameter_property NUM_INTERFACES UNITS None
set_parameter_property NUM_INTERFACES AFFECTS_ELABORATION true
set_parameter_property NUM_INTERFACES HDL_PARAMETER false
set_parameter_property NUM_INTERFACES ALLOWED_RANGES "1:16"
set_parameter_property NUM_INTERFACES DESCRIPTION "Determines the numbers of conduit interfaces available in one single PR/Static Region"

add_parameter NUM_PORTS INTEGER 1
set_parameter_property NUM_PORTS DISPLAY_NAME "Number of Port Signals"
set_parameter_property NUM_PORTS UNITS None
set_parameter_property NUM_PORTS AFFECTS_ELABORATION true
set_parameter_property NUM_PORTS HDL_PARAMETER false
set_parameter_property NUM_PORTS ALLOWED_RANGES "1:64"
set_parameter_property NUM_PORTS DESCRIPTION "Determines the numbers of port signals from all conduit interface available in one single PR/Static Region"

add_parameter NUM_READ_LATENCY INTEGER 0
set_parameter_property NUM_READ_LATENCY DISPLAY_NAME "Fixed Ready Latency"
set_parameter_property NUM_READ_LATENCY UNITS None
set_parameter_property NUM_READ_LATENCY AFFECTS_ELABORATION true
set_parameter_property NUM_READ_LATENCY HDL_PARAMETER false
set_parameter_property NUM_READ_LATENCY ALLOWED_RANGES "0:10"
set_parameter_property NUM_READ_LATENCY DESCRIPTION "Determines the numbers of ready lantecy, this is applicable only when AVST interface is defined"

add_parameter NUM_DATABITSPERSYMBOL INTEGER 8
set_parameter_property NUM_DATABITSPERSYMBOL DISPLAY_NAME "Data Bits Per Symbol"
set_parameter_property NUM_DATABITSPERSYMBOL UNITS None
set_parameter_property NUM_DATABITSPERSYMBOL AFFECTS_ELABORATION true
set_parameter_property NUM_DATABITSPERSYMBOL HDL_PARAMETER false
set_parameter_property NUM_DATABITSPERSYMBOL DESCRIPTION "Determines the numbers of data bits per symbol, this is applicable only when AVST interface is defined"

add_display_item "" "Port Signal Definition " GROUP ""

add_display_item "Port Signal Definition" "PortSignalDefinitionText" text "Define every port signals available in each conduit/AVST/interrupt interface at the PR region, type in exact same port signal name defined from each interface as well as its port width and freeze assertion values required during the Partial Reconfiguration process"

add_display_item "Port Signal Definition" "Update Port SignalTable" "action" "update_port_signals"

add_display_item "Port Signal Definition" "Port Signal Table" GROUP "table"

add_parameter MODULE_LIST STRING_LIST ""
set_parameter_property MODULE_LIST DISPLAY_NAME "Interface Name"
set_parameter_property MODULE_LIST DISPLAY_HINT "table"
set_parameter_property MODULE_LIST AFFECTS_ELABORATION true
set_parameter_property MODULE_LIST DERIVED false
set_parameter_property MODULE_LIST HDL_PARAMETER false
set_parameter_property MODULE_LIST GROUP "Port Signal Table"
set_parameter_property MODULE_LIST DISPLAY_HINT "WIDTH:150"
set_parameter_property MODULE_LIST DESCRIPTION "Interface name observed from Qsys, for example: the rx_stream is one of conduit interface from Display Port IP with a few number of ports exported, the tx/rx_cal_busy is one of conduit interface from Transceiver Native PHY with only one port exported"

add_parameter PORT_LIST STRING_LIST ""
set_parameter_property PORT_LIST DISPLAY_NAME "Port Name"
set_parameter_property PORT_LIST DISPLAY_HINT "table"
set_parameter_property PORT_LIST AFFECTS_ELABORATION true
set_parameter_property PORT_LIST HDL_PARAMETER false
set_parameter_property PORT_LIST GROUP "Port Signal Table"
set_parameter_property PORT_LIST DISPLAY_HINT "WIDTH:150"
set_parameter_property PORT_LIST DESCRIPTION "Specific name of the port from each interface"

add_parameter PORT_DIRECTION_LIST STRING_LIST ""
set_parameter_property PORT_DIRECTION_LIST DISPLAY_NAME "Port Direction"
set_parameter_property PORT_DIRECTION_LIST DISPLAY_HINT "table"
set_parameter_property PORT_DIRECTION_LIST AFFECTS_ELABORATION true
set_parameter_property PORT_DIRECTION_LIST HDL_PARAMETER false
set_parameter_property PORT_DIRECTION_LIST ALLOWED_RANGES { "output:O" "input:I" }
set_parameter_property PORT_DIRECTION_LIST GROUP "Port Signal Table"
set_parameter_property PORT_DIRECTION_LIST DISPLAY_HINT "WIDTH:90"
set_parameter_property PORT_DIRECTION_LIST DESCRIPTION "Signal direction for the specific port accordingly to the specification of each interface, for example: the tx/rx_cal_busy is one of conduit interface output from Transceiver Native PHY but is conduit interface output to the Transceiver Reset Controller"

add_parameter PORT_WIDTH_LIST INTEGER_LIST ""
set_parameter_property PORT_WIDTH_LIST DISPLAY_NAME "Port Width"
set_parameter_property PORT_WIDTH_LIST DISPLAY_HINT "table"
set_parameter_property PORT_WIDTH_LIST AFFECTS_ELABORATION true
set_parameter_property PORT_WIDTH_LIST HDL_PARAMETER false
set_parameter_property PORT_WIDTH_LIST GROUP "Port Signal Table"
set_parameter_property PORT_WIDTH_LIST DISPLAY_HINT "WIDTH:70"
set_parameter_property PORT_WIDTH_LIST DESCRIPTION "Port data width to the specific port accordingly to definition from each interface"

add_parameter PORT_FREEZE_ASSERTION_LIST STRING_LIST ""
set_parameter_property PORT_FREEZE_ASSERTION_LIST DISPLAY_NAME "Freeze Assertion"
set_parameter_property PORT_FREEZE_ASSERTION_LIST DISPLAY_HINT "table"
set_parameter_property PORT_FREEZE_ASSERTION_LIST AFFECTS_ELABORATION true
set_parameter_property PORT_FREEZE_ASSERTION_LIST HDL_PARAMETER false
set_parameter_property PORT_FREEZE_ASSERTION_LIST ALLOWED_RANGES { "0:Active Low" "1:Active High" }
set_parameter_property PORT_FREEZE_ASSERTION_LIST GROUP "Port Signal Table"
set_parameter_property PORT_FREEZE_ASSERTION_LIST DISPLAY_HINT "WIDTH:110"
set_parameter_property PORT_FREEZE_ASSERTION_LIST DESCRIPTION "Port freeze assertion values for the specific port during Partial Reconfiguration process accordingly to the specification of each conduit interface"

add_parameter PORT_INTERFACE_TYPE_LIST STRING_LIST ""
set_parameter_property PORT_INTERFACE_TYPE_LIST DISPLAY_NAME "Interface Type"
set_parameter_property PORT_INTERFACE_TYPE_LIST DISPLAY_HINT "table"
set_parameter_property PORT_INTERFACE_TYPE_LIST AFFECTS_ELABORATION true
set_parameter_property PORT_INTERFACE_TYPE_LIST HDL_PARAMETER false
set_parameter_property PORT_INTERFACE_TYPE_LIST ALLOWED_RANGES { "conduit:0" "avalon_streaming:1" "interrupt:2"}
set_parameter_property PORT_INTERFACE_TYPE_LIST GROUP "Port Signal Table"
set_parameter_property PORT_INTERFACE_TYPE_LIST DISPLAY_HINT "WIDTH:130"
set_parameter_property PORT_INTERFACE_TYPE_LIST DESCRIPTION "The interface type of each port defined, for example: it can be a conduit, avalon streaming or interrupt type"

add_parameter PORT_INTERFACE_DIRECTION_LIST STRING_LIST ""
set_parameter_property PORT_INTERFACE_DIRECTION_LIST DISPLAY_NAME "Interface Direction"
set_parameter_property PORT_INTERFACE_DIRECTION_LIST DISPLAY_HINT "table"
set_parameter_property PORT_INTERFACE_DIRECTION_LIST AFFECTS_ELABORATION true
set_parameter_property PORT_INTERFACE_DIRECTION_LIST HDL_PARAMETER false
set_parameter_property PORT_INTERFACE_DIRECTION_LIST ALLOWED_RANGES { "end:0" "start:1"}
set_parameter_property PORT_INTERFACE_DIRECTION_LIST GROUP "Port Signal Table"
set_parameter_property PORT_INTERFACE_DIRECTION_LIST DISPLAY_HINT "WIDTH:120"
set_parameter_property PORT_INTERFACE_DIRECTION_LIST DESCRIPTION "The interface direction based on the specific type defined, set end for conduit and interrupt type. For avalon streaming, set start if the PR region has a Sink interface and end for if PR region has a Source interface"

add_parameter PORT_STREAMING_INTERFACE_TYPE_LIST STRING_LIST ""
set_parameter_property PORT_STREAMING_INTERFACE_TYPE_LIST DISPLAY_NAME "AVST Port Type"
set_parameter_property PORT_STREAMING_INTERFACE_TYPE_LIST DISPLAY_HINT "table"
set_parameter_property PORT_STREAMING_INTERFACE_TYPE_LIST AFFECTS_ELABORATION true
set_parameter_property PORT_STREAMING_INTERFACE_TYPE_LIST HDL_PARAMETER false
set_parameter_property PORT_STREAMING_INTERFACE_TYPE_LIST ALLOWED_RANGES { "none:0" "ready:1" "valid:2" "startofpacket:3" "endofpacket:4" "data:5" "empty:6"}
set_parameter_property PORT_STREAMING_INTERFACE_TYPE_LIST GROUP "Port Signal Table"
set_parameter_property PORT_STREAMING_INTERFACE_TYPE_LIST DISPLAY_HINT "WIDTH:120"
set_parameter_property PORT_STREAMING_INTERFACE_TYPE_LIST DESCRIPTION "The port type of Standard Avalon Streaming interface such as ready, valid, endofpacket, startofpacket, data. For conduit & interrupt interface, set it to none"

add_parameter REALTIME_MODULE_LIST STRING_LIST ""
set_parameter_property REALTIME_MODULE_LIST AFFECTS_ELABORATION true
set_parameter_property REALTIME_MODULE_LIST derived true
set_parameter_property REALTIME_MODULE_LIST visible false

add_parameter REALTIME_PORT_LIST STRING_LIST ""
set_parameter_property REALTIME_PORT_LIST AFFECTS_ELABORATION true
set_parameter_property REALTIME_PORT_LIST derived true
set_parameter_property REALTIME_PORT_LIST visible false

add_parameter REALTIME_PORT_DIRECTION_LIST STRING_LIST ""
set_parameter_property REALTIME_PORT_DIRECTION_LIST AFFECTS_ELABORATION true
set_parameter_property REALTIME_PORT_DIRECTION_LIST derived true
set_parameter_property REALTIME_PORT_DIRECTION_LIST visible false

add_parameter REALTIME_PORT_ORIGIN_WIDTH_LIST STRING_LIST ""
set_parameter_property REALTIME_PORT_ORIGIN_WIDTH_LIST AFFECTS_ELABORATION true
set_parameter_property REALTIME_PORT_ORIGIN_WIDTH_LIST derived true
set_parameter_property REALTIME_PORT_ORIGIN_WIDTH_LIST visible false

add_parameter REALTIME_PORT_FREEZE_ASSERTIONL_LIST STRING_LIST ""
set_parameter_property REALTIME_PORT_FREEZE_ASSERTIONL_LIST AFFECTS_ELABORATION true
set_parameter_property REALTIME_PORT_FREEZE_ASSERTIONL_LIST derived true
set_parameter_property REALTIME_PORT_FREEZE_ASSERTIONL_LIST visible false

add_parameter REALTIME_PORT_INTERFACE_TYPE_LIST STRING_LIST ""
set_parameter_property REALTIME_PORT_INTERFACE_TYPE_LIST AFFECTS_ELABORATION true
set_parameter_property REALTIME_PORT_INTERFACE_TYPE_LIST derived true
set_parameter_property REALTIME_PORT_INTERFACE_TYPE_LIST visible false

add_parameter REALTIME_PORT_INTERFACE_DIRECTION_LIST STRING_LIST ""
set_parameter_property REALTIME_PORT_INTERFACE_DIRECTION_LIST AFFECTS_ELABORATION true
set_parameter_property REALTIME_PORT_INTERFACE_DIRECTION_LIST derived true
set_parameter_property REALTIME_PORT_INTERFACE_DIRECTION_LIST visible false

add_parameter REALTIME_PORT_STREAMING_INTERFACE_TYPE_LIST STRING_LIST ""
set_parameter_property REALTIME_PORT_STREAMING_INTERFACE_TYPE_LIST AFFECTS_ELABORATION true
set_parameter_property REALTIME_PORT_STREAMING_INTERFACE_TYPE_LIST derived true
set_parameter_property REALTIME_PORT_STREAMING_INTERFACE_TYPE_LIST visible false


# connection point clock
# 
add_interface clock clock end
set_interface_property clock clockRate 0
set_interface_property clock ENABLED true
set_interface_property clock EXPORT_OF ""
set_interface_property clock PORT_NAME_MAP ""
set_interface_property clock CMSIS_SVD_VARIABLES ""
set_interface_property clock SVD_ADDRESS_GROUP ""

add_interface_port clock clk clk Input 1
# 
# connection point reset
# 
add_interface reset reset end
set_interface_property reset associatedClock clock
set_interface_property reset synchronousEdges DEASSERT
set_interface_property reset ENABLED true
set_interface_property reset EXPORT_OF ""
set_interface_property reset PORT_NAME_MAP ""
set_interface_property reset CMSIS_SVD_VARIABLES ""
set_interface_property reset SVD_ADDRESS_GROUP ""

add_interface_port reset reset_n reset_n Input 1

add_interface freeze_conduit conduit end
set_interface_property freeze_conduit associatedClock ""
set_interface_property freeze_conduit associatedReset ""
set_interface_property freeze_conduit ENABLED true
set_interface_property freeze_conduit EXPORT_OF ""
set_interface_property freeze_conduit PORT_NAME_MAP ""
set_interface_property freeze_conduit CMSIS_SVD_VARIABLES ""
set_interface_property freeze_conduit SVD_ADDRESS_GROUP ""

add_interface_port freeze_conduit freeze  freeze  Input  1
add_interface_port freeze_conduit illegal_request illegal_request  Output 1

proc update_port_signals {} {
    set_parameter MODULE_LIST [get_parameter REALTIME_MODULE_LIST]
    set_parameter PORT_LIST [get_parameter REALTIME_PORT_LIST]
    set_parameter PORT_DIRECTION_LIST [get_parameter REALTIME_PORT_DIRECTION_LIST]
    set_parameter PORT_WIDTH_LIST [get_parameter REALTIME_PORT_WIDTH_LIST]
    set_parameter PORT_FREEZE_ASSERTION_LIST [get_parameter REALTIME_PORT_FREEZE_ASSERTIONL_LIST]
    set_parameter PORT_INTERFACE_TYPE_LIST [get_parameter REALTIME_PORT_INTERFACE_TYPE_LIST]	
    set_parameter PORT_INTERFACE_DIRECTION_LIST [get_parameter REALTIME_PORT_INTERFACE_DIRECTION_LIST]	
    set_parameter PORT_STREAMING_INTERFACE_TYPE_LIST [get_parameter REALTIME_PORT_STREAMING_INTERFACE_TYPE_LIST]
	
}

# +-----------------------------------
# | Validation callback
# +-----------------------------------
proc validate {} {
    
	set user_module_list [get_parameter MODULE_LIST]
    set user_port_list [get_parameter PORT_LIST]
    set user_port_direction_list [get_parameter PORT_DIRECTION_LIST]
    set user_port_width_list [get_parameter PORT_WIDTH_LIST]
    set user_port_freeze_assertion_list [get_parameter PORT_FREEZE_ASSERTION_LIST]
    set user_port_interface_type_list [get_parameter PORT_INTERFACE_TYPE_LIST]	
    set user_port_interface_direction_list [get_parameter PORT_INTERFACE_DIRECTION_LIST]
    set user_port_streaming_interface_type_list [get_parameter PORT_STREAMING_INTERFACE_TYPE_LIST]	
	
    set NUM_INTERFACES [ get_parameter_value NUM_INTERFACES ]
    set NUM_PORTS [ get_parameter_value NUM_PORTS ]
  
  if { $NUM_INTERFACES > 16} {
    send_message error "$NUM_INTERFACES > NUM_INTERFACES" 
  }

  if { $NUM_PORTS > 64} {
    send_message error "$NUM_PORTS > NUM_PORTS" 
  }
  send_message info "Ensure that the Number of Conduit Interfaces & Number of Port Signals defined in the Parameters Tab match with the one defined in the Port Signal Table under the Port Signal Definition"
  send_message info "Conduit and interrupt interface can share one single conduit bridge component but not with the AVST interface. The AVST interface will need to have another separate conduit bridge component instantiated"
  send_message info "For Interface Direction, refer to the Avalon Interface Specifications for more details. For AVST and interrupt interface, set start for Source interface/Interrupt Receiver and end for Sink interface/Interrupt Sender based on the PR region"
  send_message info "For AVST Port Type, set it based on the port type of Standard Avalon Streaming interface at the PR region; while conduit & interrupt interface, set it to none. If data is defined in the AVST Port Type, the Port Width value must be in multiple of 8"
  
}
# | 
# +-----------------------------------

# +-----------------------------------
# | Elaboration callback
# +-----------------------------------
proc elaborate {} {
	set NUM_INTERFACES [get_parameter_value NUM_INTERFACES]
	set NUM_PORTS [get_parameter_value NUM_PORTS]
	set NUM_DATABITSPERSYMBOL [get_parameter_value NUM_DATABITSPERSYMBOL]
	
	set user_module_list [get_parameter MODULE_LIST]
    set user_port_list [get_parameter PORT_LIST]
    set user_port_direction_list [get_parameter PORT_DIRECTION_LIST]
    set user_port_width_list [get_parameter PORT_WIDTH_LIST]
    set user_port_freeze_assertion_list [get_parameter PORT_FREEZE_ASSERTION_LIST]	
    set user_port_interface_type_list [get_parameter PORT_INTERFACE_TYPE_LIST]	
    set user_port_interface_direction_list [get_parameter PORT_INTERFACE_DIRECTION_LIST]
    set user_port_streaming_interface_type_list [get_parameter PORT_STREAMING_INTERFACE_TYPE_LIST]		
	
	set repeat_count 0 
	set filtered_module_list ""
	set filtered_interface_type_list ""	
	set filtered_interface_direction_list ""		
    for {set i 0} { ${i} < [llength $user_module_list] } { incr i } {
	#filter off repeated interface name defined from list
	set prev_module [lindex $user_module_list [expr $i-1]]
	set current_module [lindex $user_module_list $i]
		#only begin compare from second module name
		if {$i != 0} {
			if [string match $current_module $prev_module] {
			set repeat_count 1
			} else {
			set repeat_count 0
			}
		}
		if {$repeat_count == 0} {
		#create a conduit interface for each different interface defined
		lappend filtered_module_list $current_module
		lappend filtered_interface_type_list [lindex $user_port_interface_type_list $i]
		lappend filtered_interface_direction_list [lindex $user_port_interface_direction_list $i]
		}
	}
	

    for {set j 0} { ${j} < $NUM_INTERFACES } { incr j } {
	if [string match [lindex $filtered_interface_type_list $j] "conduit"] {
	set interface_in "[lindex $filtered_module_list ${j}]_to_pr"
	add_interface $interface_in [lindex $filtered_interface_type_list $j] end clock
	set interface_out "[lindex $filtered_module_list ${j}]_to_sr"
	add_interface $interface_out [lindex $filtered_interface_type_list $j] end clock
	} else {
		if [string match [lindex $filtered_interface_direction_list $j] "start"] {
		set interface_in "[lindex $filtered_module_list ${j}]_to_pr"
		add_interface $interface_in [lindex $filtered_interface_type_list $j] start clock
		set interface_out "[lindex $filtered_module_list ${j}]_to_sr"
		add_interface $interface_out [lindex $filtered_interface_type_list $j] end clock
		if [string match [lindex $filtered_interface_type_list $j] "avalon_streaming"] {
		set_interface_property $interface_in dataBitsPerSymbol $NUM_DATABITSPERSYMBOL
		set_interface_property $interface_out dataBitsPerSymbol $NUM_DATABITSPERSYMBOL
		}
		} 		
		if [string match [lindex $filtered_interface_direction_list $j] "end"]  {
		set interface_in "[lindex $filtered_module_list ${j}]_to_pr"
		add_interface $interface_in [lindex $filtered_interface_type_list $j] end clock
		set interface_out "[lindex $filtered_module_list ${j}]_to_sr"
		add_interface $interface_out [lindex $filtered_interface_type_list $j] start clock
		if [string match [lindex $filtered_interface_type_list $j] "avalon_streaming"] {
		set_interface_property $interface_in dataBitsPerSymbol $NUM_DATABITSPERSYMBOL
		set_interface_property $interface_out dataBitsPerSymbol $NUM_DATABITSPERSYMBOL
		}
		}
	}
	}		
	
		foreach conduit_module $user_module_list conduit_port $user_port_list conduit_port_direction $user_port_direction_list conduit_port_width $user_port_width_list interface_type $user_port_interface_type_list port_streaming_interface_type $user_port_streaming_interface_type_list {
		set conduit_port_definition ""
		lappend conduit_port_definition $conduit_module $conduit_port $conduit_port_direction $conduit_port_width $interface_type $port_streaming_interface_type
		
		set conduit_to_pr_no [lsearch -regexp $filtered_module_list [lindex $conduit_port_definition 0]]
		set conduit_to_sr_no [lsearch -regexp $filtered_module_list [lindex $conduit_port_definition 0]]
		
		set conduit_to_pr "[lindex $filtered_module_list ${conduit_to_pr_no}]_to_pr"
		set conduit_to_sr "[lindex $filtered_module_list ${conduit_to_sr_no}]_to_sr"
		
		#to assign each port to its respective conduit pr/sr interface accordingly
		set conduit_port_name [lindex $conduit_port_definition 1]
		if [string match [lindex $conduit_port_definition 2] "input"] {
		#input port to PR region, output port from Static region
				if [string match [lindex $conduit_port_definition 4] "avalon_streaming"] {
					#add ready latency to avoid adaptation from Qsys
					set_interface_property $conduit_to_pr readyLatency 1		
					set_interface_property $conduit_to_sr readyLatency 1
					if [string match [lindex $conduit_port_definition 5] "ready"] {			
					add_interface_port $conduit_to_pr "conduit_bridge_to_pr_${conduit_port_name}" ready input [lindex $conduit_port_definition 3]
					add_interface_port $conduit_to_sr "conduit_bridge_to_sr_${conduit_port_name}" ready output [lindex $conduit_port_definition 3]
					} elseif [string match [lindex $conduit_port_definition 5] "valid"] {
					add_interface_port $conduit_to_pr "conduit_bridge_to_pr_${conduit_port_name}" valid input [lindex $conduit_port_definition 3]
					add_interface_port $conduit_to_sr "conduit_bridge_to_sr_${conduit_port_name}" valid output [lindex $conduit_port_definition 3]
					} elseif [string match [lindex $conduit_port_definition 5] "startofpacket"] {
					add_interface_port $conduit_to_pr "conduit_bridge_to_pr_${conduit_port_name}" startofpacket input [lindex $conduit_port_definition 3]
					add_interface_port $conduit_to_sr "conduit_bridge_to_sr_${conduit_port_name}" startofpacket output [lindex $conduit_port_definition 3]
					} elseif [string match [lindex $conduit_port_definition 5] "endofpacket"] {
					add_interface_port $conduit_to_pr "conduit_bridge_to_pr_${conduit_port_name}" endofpacket input [lindex $conduit_port_definition 3]
					add_interface_port $conduit_to_sr "conduit_bridge_to_sr_${conduit_port_name}" endofpacket output [lindex $conduit_port_definition 3]
					} elseif [string match [lindex $conduit_port_definition 5] "empty"] {
					add_interface_port $conduit_to_pr "conduit_bridge_to_pr_${conduit_port_name}" empty input [lindex $conduit_port_definition 3]
					add_interface_port $conduit_to_sr "conduit_bridge_to_sr_${conduit_port_name}" empty output [lindex $conduit_port_definition 3]
					} else {
					add_interface_port $conduit_to_pr "conduit_bridge_to_pr_${conduit_port_name}" data input [lindex $conduit_port_definition 3]
					add_interface_port $conduit_to_sr "conduit_bridge_to_sr_${conduit_port_name}" data output [lindex $conduit_port_definition 3]
					}
				} elseif [string match [lindex $conduit_port_definition 4] "interrupt"] {
				add_interface_port $conduit_to_pr "conduit_bridge_to_pr_${conduit_port_name}" irq input [lindex $conduit_port_definition 3]
				add_interface_port $conduit_to_sr "conduit_bridge_to_sr_${conduit_port_name}" irq output [lindex $conduit_port_definition 3]
				} else {
				add_interface_port $conduit_to_pr "conduit_bridge_to_pr_${conduit_port_name}" [lindex $conduit_port_definition 1] input [lindex $conduit_port_definition 3]
				add_interface_port $conduit_to_sr "conduit_bridge_to_sr_${conduit_port_name}" [lindex $conduit_port_definition 1] output [lindex $conduit_port_definition 3]
				}				
		} else {
		#output port from PR region, input port to Static region
				if [string match [lindex $conduit_port_definition 4] "avalon_streaming"] {
					#add ready latency to avoid adaptation from Qsys
					set_interface_property $conduit_to_pr readyLatency 1		
					set_interface_property $conduit_to_sr readyLatency 1
					if [string match [lindex $conduit_port_definition 5] "ready"] {
					add_interface_port $conduit_to_pr "conduit_bridge_to_pr_${conduit_port_name}" ready output [lindex $conduit_port_definition 3]
					add_interface_port $conduit_to_sr "conduit_bridge_to_sr_${conduit_port_name}" ready input [lindex $conduit_port_definition 3]
					} elseif [string match [lindex $conduit_port_definition 5] "valid"] {
					add_interface_port $conduit_to_pr "conduit_bridge_to_pr_${conduit_port_name}" valid output [lindex $conduit_port_definition 3]
					add_interface_port $conduit_to_sr "conduit_bridge_to_sr_${conduit_port_name}" valid input [lindex $conduit_port_definition 3]
					} elseif [string match [lindex $conduit_port_definition 5] "startofpacket"] {
					add_interface_port $conduit_to_pr "conduit_bridge_to_pr_${conduit_port_name}" startofpacket output [lindex $conduit_port_definition 3]
					add_interface_port $conduit_to_sr "conduit_bridge_to_sr_${conduit_port_name}" startofpacket input [lindex $conduit_port_definition 3]
					} elseif [string match [lindex $conduit_port_definition 5] "endofpacket"] {
					add_interface_port $conduit_to_pr "conduit_bridge_to_pr_${conduit_port_name}" endofpacket output [lindex $conduit_port_definition 3]
					add_interface_port $conduit_to_sr "conduit_bridge_to_sr_${conduit_port_name}" endofpacket input [lindex $conduit_port_definition 3]
					}  elseif [string match [lindex $conduit_port_definition 5] "empty"] {
					add_interface_port $conduit_to_pr "conduit_bridge_to_pr_${conduit_port_name}" empty output [lindex $conduit_port_definition 3]
					add_interface_port $conduit_to_sr "conduit_bridge_to_sr_${conduit_port_name}" empty input [lindex $conduit_port_definition 3]
					} else {
					add_interface_port $conduit_to_pr "conduit_bridge_to_pr_${conduit_port_name}" data output [lindex $conduit_port_definition 3]
					add_interface_port $conduit_to_sr "conduit_bridge_to_sr_${conduit_port_name}" data input [lindex $conduit_port_definition 3]
					}
				} elseif [string match [lindex $conduit_port_definition 4] "interrupt"] {
					add_interface_port $conduit_to_pr "conduit_bridge_to_pr_${conduit_port_name}" irq output [lindex $conduit_port_definition 3]
					add_interface_port $conduit_to_sr "conduit_bridge_to_sr_${conduit_port_name}" irq input [lindex $conduit_port_definition 3]
				} else {
					add_interface_port $conduit_to_pr "conduit_bridge_to_pr_${conduit_port_name}" [lindex $conduit_port_definition 1] output [lindex $conduit_port_definition 3]
					add_interface_port $conduit_to_sr "conduit_bridge_to_sr_${conduit_port_name}" [lindex $conduit_port_definition 1] input [lindex $conduit_port_definition 3]
				}
		}

		}
}

add_fileset SYNTH   QUARTUS_SYNTH generate
# +-----------------------------------
# | Generation callback
# +-----------------------------------
proc generate {OUTPUT_NAME} {

    set template_file "freeze_conduit_bridge.sv.terp"

    set template    [ read [ open $template_file r ] ]
    # set params(OUTPUT_NAME) freeze_conduit_bridge
    set params(OUTPUT_NAME)  ${OUTPUT_NAME}
    set NUM_INTERFACES [ get_parameter_value NUM_INTERFACES ]
    set params(NUM_INTERFACES) $NUM_INTERFACES
    set NUM_PORTS [ get_parameter_value NUM_PORTS ]
    set params(NUM_PORTS) $NUM_PORTS
	set CONDUIT_SIGNALS_LIST [ get_parameter_value PORT_LIST ]
    set params(CONDUIT_SIGNALS_LIST) $CONDUIT_SIGNALS_LIST
	set CONDUIT_SIGNALS_DIRECTION_LIST [ get_parameter_value PORT_DIRECTION_LIST ]
    set params(CONDUIT_SIGNALS_DIRECTION_LIST) $CONDUIT_SIGNALS_DIRECTION_LIST
	set CONDUIT_SIGNALS_WIDTH_LIST [ get_parameter_value PORT_WIDTH_LIST ]
    set params(CONDUIT_SIGNALS_WIDTH_LIST) $CONDUIT_SIGNALS_WIDTH_LIST
	set CONDUIT_SIGNALS_ASSERTION_LIST [ get_parameter_value PORT_FREEZE_ASSERTION_LIST ]
    set params(CONDUIT_SIGNALS_ASSERTION_LIST) $CONDUIT_SIGNALS_ASSERTION_LIST
	set SIGNAL_INTERFACE_TYPE_LIST [ get_parameter_value PORT_INTERFACE_TYPE_LIST ]
    set params(SIGNAL_INTERFACE_TYPE_LIST) $SIGNAL_INTERFACE_TYPE_LIST
	set SIGNAL_INTERFACE_DIRECTION_LIST [ get_parameter_value PORT_INTERFACE_DIRECTION_LIST ]
    set params(SIGNAL_INTERFACE_DIRECTION_LIST) $SIGNAL_INTERFACE_DIRECTION_LIST
	set STREAMING_INTERFACE_SIGNAL_TYPE_LIST [ get_parameter_value PORT_STREAMING_INTERFACE_TYPE_LIST ]
    set params(STREAMING_INTERFACE_SIGNAL_TYPE_LIST) $STREAMING_INTERFACE_SIGNAL_TYPE_LIST
    set result          [ altera_terp $template params ]

    add_fileset_file ${OUTPUT_NAME}.sv SYSTEM_VERILOG TEXT $result
}

## Add documentation links for user guide and/or release notes
