######################################################################
# Automatically inserts ILA instances in a batch flow, and calls "implement_debug_core".   Can also be used in a GUI flow
# This should ONLY be invoked after synthesis, and before opt_design.   If opt_design is called first, marked nets may be missing and not found
# Warning: Currently will skip a net if it has no obvious clock domain on the driver.  Nets connected to input buffers will be dropped unless "mark_debug_clock" is attached to the net.
# Nets attached to VIO cores have the "mark_debug" attribute, and will be filtered out unless the "mark_debug_valid" attribute is attached.
# Supports the following additional attributes beyond "mark_debug"
# attribute mark_debug_valid of X : signal is "true";   -- Marks a net for ILA capture, even if net is also attached to a VIO core
# attribute mark_debug_clock of X : signal is "inst1_bufg/clock";  -- Specifies clock net to use for capturing this net.  May create a new ILA core for that clock domain
# attribute mark_debug_depth of X : signal is "4096";              -- overrides default depth for this ILA core. valid values: 1024, 2048, ... 132072.   Last attribute that is scanned will win.
# attribute mark_debug_adv_trigger of X : signal is "true";        -- specifies that advanced trigger capability will be added to ILA core
# Engineer:  J. McCluskey
proc insert_ila { depth } {
    ##################################################################
    # sequence through debug nets and organize them by clock in the
    # clock_list array. Also create max and min array for bus indices
    set dbgs [get_nets -hierarchical -filter {MARK_DEBUG}]
    if {[llength $dbgs] == 0} {
        puts "No nets have the MARK_DEBUG attribute.  No ILA cores created"
        return
    } else {
    # process list of nets to find and reject nets that are attached to VIO cores.  This has a side effect that VIO nets can't be monitored with an ILA
    # This can be overridden by using the attribute "mark_debug_valid" = "true" on a net like this.
    set net_list {}
        foreach net $dbgs {
            if { [get_property -quiet MARK_DEBUG_VALID $net] != "true" } { 
                set pin_list [get_pins -of_objects [get_nets -segments $net]]
                set not_vio_net 1
                foreach pin $pin_list {
                    if { [get_property IS_DEBUG_CORE [get_cells -of_object $pin]] == 1 } {
                        # It seems this net is attached to a debug core (i.e. VIO core) already, so we should skip adding it to the netlist
                        set not_vio_net 0
                        break
                        }
                    }
                if { $not_vio_net == 1 } { lappend net_list $net; }
            } else { 
                lappend net_list $net
            }
        }
    }
    # check again to see if we have any nets left now
    if {[llength $net_list] == 0} {
        puts "All nets with MARK_DEBUG are already connected to VIO cores.  No ILA cores created"
        return
    }
    # Now that the netlist has been filtered,  determine bus names and clock domains
    foreach d $net_list {
        # name is root name of a bus, index is the bit index in the
        # bus
        set name [regsub {\[[[:digit:]]+\]$} $d {}]
        set index [regsub {^.*\[([[:digit:]]+)\]$} $d {\1}]
        if {[string is integer -strict $index]} {
            if {![info exists max($name)]} {
                set max($name) $index
                set min($name) $index
            } elseif {$index > $max($name)} {
                set max($name) $index
            } elseif {$index < $min($name)} {
                set min($name) $index
            }
        } else {
            set max($name) -1
        }
    }

create_debug_core u_ila_0 ila

set_property C_DATA_DEPTH $depth [get_debug_cores u_ila_0]
set_property C_TRIGIN_EN false [get_debug_cores u_ila_0]
set_property C_TRIGOUT_EN false [get_debug_cores u_ila_0]
set_property C_INPUT_PIPE_STAGES 2 [get_debug_cores u_ila_0]
set_property C_EN_STRG_QUAL true [get_debug_cores u_ila_0 ]
set_property C_ADV_TRIGGER true [get_debug_cores u_ila_0 ]
set_property ALL_PROBE_SAME_MU true [get_debug_cores u_ila_0 ]
set_property ALL_PROBE_SAME_MU_CNT 4 [get_debug_cores u_ila_0 ]

set_property port_width 1 [get_debug_ports u_ila_0/clk]
connect_debug_port u_ila_0/clk [get_nets [list design_1_i/processing_system7_0/inst/FCLK_CLK0 ]]

#        ##################################################################
#        # add probes
        set nprobes 0
	foreach n [array names max] {
            set nets {}
            if {$max($n) < 0} {
                lappend nets [get_nets $n]
            } else {
                # n is a bus name
                for {set i $min($n)} {$i <= $max($n)} {incr i} {
                    lappend nets [get_nets $n[$i]]
                }
            }
            set prb probe$nprobes
            if {$nprobes > 0} {
                create_debug_port u_ila_0 probe
            }
            set_property port_width [llength $nets] [get_debug_ports u_ila_0/$prb]
            connect_debug_port u_ila_0/$prb $nets
            incr nprobes
        }
    }
 
    set project_found [get_projects -quiet] 
    if { $project_found != "New Project" } {
        puts "Saving constraints now in project [current_project -quiet]"
        save_constraints_as debug_constraints.xdc
    }    
    ##################################################################
    implement_debug_core
    ##################################################################
    # write out probe info file
    write_debug_probes -force debug_nets.ltx
}

