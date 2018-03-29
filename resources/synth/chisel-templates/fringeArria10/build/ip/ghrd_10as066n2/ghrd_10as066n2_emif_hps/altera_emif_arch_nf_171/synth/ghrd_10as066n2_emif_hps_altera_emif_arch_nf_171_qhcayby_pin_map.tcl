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


set script_dir [file dirname [info script]]
source "$script_dir/ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_utils.tcl"

load_package sdc_ext

proc ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_ddr_pins { instname allpins var_array_name} {
   # We need to make a local copy of the allpins associative array
   upvar allpins pins
   upvar 1 $var_array_name var
   set debug 0

   # with the hyper-retimer, the tdb netlist may contain the full set of routing nodes, so we need
   # to search deeper
   set var(pll_inclock_search_depth) 30
   set var(pll_outclock_search_depth) 20
   set var(pll_vcoclock_search_depth) 5

   # ########################################
   #  1.0 find all of the PLL output clocks

   if {[is_fitter_in_qhd_mode]} {
      set c0_periph_clock_name "phy_clk\[1\]"
      set c1_periph_clock_name "phy_clk\[0\]"
      set vco_clock_name "phy_clk_phs\[0\]"
   } else {
      set c0_periph_clock_name "pll_lvds_clk\[0\]"
      set c1_periph_clock_name "pll_loaden\[0\]"
      set vco_clock_name "pll_vcoph\[0\]"
   }

   #  C0 output in the periphery
   set pins(pll_c0_periph_clock) [list]
   set pins(pll_c0_periph_clock_id) [get_nets -nowarn [list ${instname}|arch|arch_inst|pll_inst|${c0_periph_clock_name}  ${instname}|arch|arch_inst|pll_inst|pll_inst*LVDS_CLK0]]
   foreach_in_collection c $pins(pll_c0_periph_clock_id) {
      lappend pins(pll_c0_periph_clock) [regsub -all {\\} [get_node_info -name $c] {\\\\}]
   }
   set pins(pll_c0_periph_clock) [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_sort_duplicate_names $pins(pll_c0_periph_clock)]

   #  C1 output in the periphery
   set pins(pll_c1_periph_clock) [list]
   set pins(pll_c1_periph_clock_id) [get_nets -nowarn [list ${instname}|arch|arch_inst|pll_inst|${c1_periph_clock_name}  ${instname}|arch|arch_inst|pll_inst|pll_inst*LOADEN0]]
   foreach_in_collection c $pins(pll_c1_periph_clock_id) {
      lappend pins(pll_c1_periph_clock) [regsub -all {\\} [get_node_info -name $c] {\\\\}]
   }
   set pins(pll_c1_periph_clock) [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_sort_duplicate_names $pins(pll_c1_periph_clock)]

   #  VCO clock (used for the system clock)
   set pins(vco_clock) [list]
   set pins(vco_clock_id) [get_nets -nowarn [list ${instname}|arch|arch_inst|pll_inst|${vco_clock_name}  ${instname}|arch|arch_inst|pll_inst|pll_inst*VCOPH0]]
   foreach_in_collection c $pins(vco_clock_id) {
      lappend pins(vco_clock) [regsub -all {\\} [get_node_info -name $c] {\\\\}]
   }
   set pins(vco_clock) [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_sort_duplicate_names $pins(vco_clock)]
   set pins(pll_vco_clock) $pins(vco_clock)
   set pins(pll_phy_clock) $pins(pll_c1_periph_clock)
   set pins(pll_phy_clock_l) $pins(pll_c0_periph_clock)

   if {$debug == 1} {
     puts "VCO:           $pins(pll_vco_clock)"
     puts "PHY:           $pins(pll_phy_clock)"
     puts "PHY_L:         $pins(pll_phy_clock_l)"
     puts ""
   }

   #########################################
   # 2.0  Find the actual master core clock
   #      As it could come from another interface
   #      In master/slave configurations
   #
   # Skip this if we're in HPS mode as core clocks don't exist
   
   set pins(master_vco_clock) ""
   set pins(master_vco_clock_sec) ""
   set pins(master_core_usr_clock) ""
   set pins(master_core_usr_half_clock) ""
   set pins(master_core_usr_clock_sec) ""
   set pins(master_core_usr_half_clock_sec) ""
   set pins(master_core_afi_half_clock) ""
   set pins(master_core_afi_clock) ""
   set pins(master_core_dft_cpa_1_clock) ""
   set pins(master_cal_master_clk) ""
   set pins(master_cal_slave_clk) ""
   
   if {$var(IS_HPS)} {
      set pins(master_instname) $instname

   } else {
      set msg_list [ list ]
      
      set num_of_cpa_blocks [expr {$var(PHY_PING_PONG_EN) ? 2 : 1}]
      
      for {set cpa_idx 0} {$cpa_idx < $num_of_cpa_blocks} {incr cpa_idx} {
      
         if {$cpa_idx == 0} {
            set sync_reset_reg ${instname}|arch|arch_inst|non_hps.core_clks_rsts_inst|reset_sync_pri_sdc_anchor
         } else {
            set sync_reset_reg ${instname}|arch|arch_inst|non_hps.core_clks_rsts_inst|pp.reset_sync_sec_sdc_anchor
         }
      
         set core_reset_sync_clock "_UNDEFINED_PIN_"
         set core_reset_sync_clock_id [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_output_clock_id $sync_reset_reg "Usr clock" msg_list var]
         if {$core_reset_sync_clock_id == -1} {
            foreach {msg_type msg} $msg_list {
               post_message -type $msg_type "ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_pin_map.tcl: $msg"
            }
            post_message -type error "ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_pin_map.tcl: Failed to find clock source for register $sync_reset_reg"
            
            if {$var(PHY_CORE_CLKS_SHARING_ENUM) == "CORE_CLKS_SHARING_SLAVE"} {
               post_message -type error "ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_pin_map.tcl: This is a clock sharing SLAVE interface. Please ensure that the clks_sharing_master_out port of the master is connected to the clks_sharing_master_in port of the slave(s)."
               if {$cpa_idx > 0} {
                  post_message -type error "ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_pin_map.tcl: This clock sharing slave interface uses a Ping-Pong PHY and has extra clock/reset requirements. Please ensure that the master interface is also a ping-pong interface. A ping-pong interface can act as clock sharing master for both ping-pong and non-ping-pong interfaces."
               }
            } else {
               post_message -type error "ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_pin_map.tcl: Please ensure that the register has not been removed or optimized away."
            }
         } else {
            set core_reset_sync_clock [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_pll_clock_name $core_reset_sync_clock_id]
         }
      
         if {[regexp {(^.*)\|arch\|arch_inst\|io_tiles_wrap_inst\|io_tiles_inst\|tile_gen\[([0-9])\].tile_ctrl_inst(.*)\|pa_core_clk_out\[[0-9]\]$} $core_reset_sync_clock matched pins(master_instname) tilegen_num tile_instnum] == 1} {
            if {$var(PHY_CONFIG_ENUM) == "CONFIG_PHY_AND_HARD_CTRL"} {
               if {$var(USER_CLK_RATIO) == 2 && $var(C2P_P2C_CLK_RATIO) == 4} {
                  if {$cpa_idx == 0} {
                     set pins(master_core_usr_clock)          "$pins(master_instname)|arch|arch_inst|io_tiles_wrap_inst|io_tiles_inst|tile_gen\[${tilegen_num}\].tile_ctrl_inst${tile_instnum}|pa_core_clk_out\[0\]"
                     set pins(master_core_usr_half_clock)     "$pins(master_instname)|arch|arch_inst|io_tiles_wrap_inst|io_tiles_inst|tile_gen\[${tilegen_num}\].tile_ctrl_inst${tile_instnum}|pa_core_clk_out\[1\]"
                  } else {
                     set pins(master_core_usr_clock_sec)      "$pins(master_instname)|arch|arch_inst|io_tiles_wrap_inst|io_tiles_inst|tile_gen\[${tilegen_num}\].tile_ctrl_inst${tile_instnum}|pa_core_clk_out\[0\]"
                     set pins(master_core_usr_half_clock_sec) "$pins(master_instname)|arch|arch_inst|io_tiles_wrap_inst|io_tiles_inst|tile_gen\[${tilegen_num}\].tile_ctrl_inst${tile_instnum}|pa_core_clk_out\[1\]"
                  }
               } else {
                  if {$cpa_idx == 0} {
                     set pins(master_core_usr_clock)          "$pins(master_instname)|arch|arch_inst|io_tiles_wrap_inst|io_tiles_inst|tile_gen\[${tilegen_num}\].tile_ctrl_inst${tile_instnum}|pa_core_clk_out\[0\]"
                  } else {
                     set pins(master_core_usr_clock_sec)      "$pins(master_instname)|arch|arch_inst|io_tiles_wrap_inst|io_tiles_inst|tile_gen\[${tilegen_num}\].tile_ctrl_inst${tile_instnum}|pa_core_clk_out\[0\]"
                  }
               }
               set pins(master_core_dft_cpa_1_clock)   [expr {$var(DIAG_CPA_OUT_1_EN) ? "$pins(master_instname)|arch|arch_inst|io_tiles_wrap_inst|io_tiles_inst|tile_gen\[${tilegen_num}\].tile_ctrl_inst${tile_instnum}|pa_core_clk_out\[1\]" : ""}]

            } else {
               set pins(master_core_afi_half_clock)        "$pins(master_instname)|arch|arch_inst|io_tiles_wrap_inst|io_tiles_inst|tile_gen\[${tilegen_num}\].tile_ctrl_inst${tile_instnum}|pa_core_clk_out\[0\]"
               set pins(master_core_afi_clock)             "$pins(master_instname)|arch|arch_inst|io_tiles_wrap_inst|io_tiles_inst|tile_gen\[${tilegen_num}\].tile_ctrl_inst${tile_instnum}|pa_core_clk_out\[1\]"
            }

            if { $::TimeQuestInfo(nameofexecutable) == "quartus_map" || $::TimeQuestInfo(nameofexecutable) == "quartus_syn"} {
               set vco_clock_name "_UNDEFINED_PIN_"
            } else {
               set vco_clock_id [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_vco_clk_id $core_reset_sync_clock_id var]
               set vco_clock_name [get_net_info -name [get_pin_info -net $vco_clock_id]]
            }
            if {$cpa_idx == 0} {
               set pins(master_vco_clock) $vco_clock_name
            } else {
               set pins(master_vco_clock_sec) $vco_clock_name  
            }

         } else {
            post_message -type error "ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_pin_map.tcl: Failed to find CPA outputs."
         }
      }

      if {!$var(DIAG_USE_CPA_LOCK)} {
         set pins(counter_lock_reg) $pins(master_instname)|arch|arch_inst|non_hps.core_clks_rsts_inst|counter_lock
      }

      set pll_master_user_clock_base [string range $pins(master_vco_clock) 0 [string last "|" $pins(master_vco_clock)] ]pll_inst|outclk
      
      set var(pll_c3_cnt) [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_calculate_counter_value $var(PLL_C_CNT_HIGH_3) $var(PLL_C_CNT_LOW_3) $var(PLL_C_CNT_BYPASS_EN_3)]
      set pins(master_cal_slave_clk) "$pll_master_user_clock_base\[3\]"
      
      set var(pll_c4_cnt) [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_calculate_counter_value $var(PLL_C_CNT_HIGH_4) $var(PLL_C_CNT_LOW_4) $var(PLL_C_CNT_BYPASS_EN_4)]
      set pins(master_cal_master_clk) "$pll_master_user_clock_base\[4\]"
   }

   if {$var(PHY_USERMODE_OCT)} {
      set pins(usermode_oct_clock) "${instname}|arch|arch_inst|oct_inst|cal_oct.manual_oct_cal.r_clkdiv"
      set pins(usermode_oct_gated_clock) "${instname}|arch|arch_inst|oct_inst|cal_oct.manual_oct_cal.oct_inst|altera_oct_um_fsm_i|clkena|outclk"
   } else {
      set pins(usermode_oct_clock) ""
      set pins(usermode_oct_gated_clock) ""
   }

   if {$debug == 1} {
     puts "Master VCO                       : $pins(master_vco_clock)"
     puts "Master Core USR                  : $pins(master_core_usr_clock)"
     puts "Master Core USR Half             : $pins(master_core_usr_half_clock)"
     puts "Master Core AFI                  : $pins(master_core_afi_clock)"
     puts "Master Core AFI Half             : $pins(master_core_afi_half_clock)"
     puts "Master VCO (SECONDARY)           : $pins(master_vco_clock_sec)"
     puts "Master Core USR (SECONDARY)      : $pins(master_core_usr_clock_sec)"
     puts "Master Core USR Half (SECONDARY) : $pins(master_core_usr_half_clock_sec)"
     puts ""
   }


   # ########################################
   #  2.5 Find the reference clock input of the PLL

   set pins(pll_cascade_in_id) [get_pins -compatibility_mode $pins(master_instname)|arch|arch_inst|pll_inst|pll_inst|pll_cascade_in]
   set pll_ref_clock_id [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_input_clk_id $pins(pll_cascade_in_id) var]
   if {$pll_ref_clock_id == -1} {
      post_message -type critical_warning "ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_pin_map.tcl: Failed to find PLL reference clock"
   } else {
      set pll_ref_clock [get_node_info -name $pll_ref_clock_id]
   }
   set pins(pll_ref_clock) $pll_ref_clock

   if {$debug == 1} {
     puts "REF:     $pins(pll_ref_clock)"
     puts ""
   }

   #########################################
   # 3.0  find the FPGA pins

   # The hierarchy paths to all the pins are stored in the *_ip_parameters.tcl
   # file which is a generated file. Pins are divided into the following
   # protocol-agnostic categories. For each pin category, we need to
   # fully-resolve the hierarchy path patterns and store the results into
   # the "pins" arrays.
   set pin_categories [list ac_clk \
                            ac_clk_n \
                            ac_sync \
                            ac_async \
                            rclk \
                            rclk_n \
                            wclk \
                            wclk_n \
                            rdata \
                            wdata \
                            dm \
                            dbi ]

   set patterns [ list ]
   foreach pin_category $pin_categories {
      set pins($pin_category) [list]

      foreach pattern $var(PATTERNS_[string toupper $pin_category]) {
         set pattern "${instname}|$pattern"
         lappend patterns $pin_category $pattern
      }
   }

   foreach {pin_type pattern} $patterns {
      if {[string match "*|o" $pattern]} {
         set local_pins [ ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_names_in_collection [ get_fanouts $pattern ] ]
      } else {
         set local_pins [ ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_names_in_collection [ get_fanins $pattern ] ]
      }

      if {[llength $local_pins] == 0} {
         post_message -type critical_warning "Could not find pin of type $pin_type from pattern $pattern"
      } else {
         foreach pin [lsort -unique $local_pins] {
            lappend pins($pin_type) $pin
         }
      }
   }

   #########################################
   # 4.0  setup extra PLL clocks parameters

   # User can use remaining PLL clocks from EMIF GUI and this is to
   # setup the parameters for those clocks such as multiply_by
   # and divide_by

   if {$var(PLL_NUM_OF_EXTRA_CLKS) > 0} {

      set pll_master_user_clock_base [string range $pins(master_vco_clock) 0 [string last "|" $pins(master_vco_clock)] ]pll_inst|outclk

      set var(pll_num_of_reserved_cnts) 5

      for {set i 0} {$i < $var(PLL_NUM_OF_EXTRA_CLKS)} {incr i} {
         set i_cnt_num [expr $i + $var(pll_num_of_reserved_cnts)]
         set var(pll_c${i_cnt_num}_cnt) [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_calculate_counter_value $var(PLL_C_CNT_HIGH_${i_cnt_num}) $var(PLL_C_CNT_LOW_${i_cnt_num}) $var(PLL_C_CNT_BYPASS_EN_${i_cnt_num})]
         set pins(pll_extra_clk_${i}) "$pll_master_user_clock_base\[$i_cnt_num\]"
      }
   }
}

proc ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_initialize_ddr_db { ddr_db_par var_array_name} {
   upvar $ddr_db_par local_ddr_db
   upvar 1 $var_array_name var

   global ::GLOBAL_ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_corename
   global ::io_only_analysis

   post_sdc_message info "Initializing DDR database for CORE $::GLOBAL_ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_corename"
   set instance_list [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_core_instance_list $::GLOBAL_ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_corename]

   foreach instname $instance_list {

      if {$::io_only_analysis == 0}  {
         post_sdc_message info "Finding port-to-pin mapping for CORE: $::GLOBAL_ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_corename INSTANCE: $instname"
         ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_ddr_pins $instname allpins var
         ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_verify_ddr_pins allpins var
      }

      set local_ddr_db($instname) [ array get allpins ]
   }
}

proc ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_verify_ddr_pins { pins_par var_array_name} {

   upvar 1 $var_array_name var
   upvar $pins_par pins

   if { [ llength $pins(pll_phy_clock) ] != [ llength $pins(pll_vco_clock) ] } {
      post_message -type critical_warning "Found different amounts of the phy_clocks compared to the vco_clocks"
   }
   if {!$var(IS_HPS)} {
      if {$var(PHY_CONFIG_ENUM) == "CONFIG_PHY_AND_HARD_CTRL"} {
         if { [ llength $pins(master_core_usr_clock) ] != 1 } {
            post_message -type critical_warning "Found [ llength $pins(master_core_usr_clock) ] of master_core_usr_clock when there should be 1"
         }

         if {$var(USER_CLK_RATIO) == 2 && $var(C2P_P2C_CLK_RATIO) == 4} {
            if { [ llength $pins(master_core_usr_half_clock) ] != 1 } {
               post_message -type critical_warning "Found [ llength $pins(master_core_usr_half_clock) ] of master_core_usr_half_clock when there should be 1"
            }
         }
         
         if {$var(PHY_PING_PONG_EN)} {
            if { [ llength $pins(master_core_usr_clock_sec) ] != 1 } {
               post_message -type critical_warning "Found [ llength $pins(master_core_usr_clock_sec) ] of master_core_usr_clock_sec when there should be 1"
            }

            if {$var(USER_CLK_RATIO) == 2 && $var(C2P_P2C_CLK_RATIO) == 4} {
               if { [ llength $pins(master_core_usr_half_clock_sec) ] != 1 } {
                  post_message -type critical_warning "Found [ llength $pins(master_core_usr_half_clock_sec) ] of master_core_usr_half_clock_sec when there should be 1"
               }
            }
         }
      } else {
         if { [ llength $pins(master_core_afi_clock) ] != 1 } {
            post_message -type critical_warning "Found [ llength $pins(master_core_afi_clock) ] of master_core_afi_clock when there should be 1"
         }
         if { [ llength $pins(master_core_afi_half_clock) ] != 1 } {
            post_message -type critical_warning "Found [ llength $pins(master_core_afi_half_clock) ] of master_core_afi_half_clock when there should be 1"
         }
      }
   }
}

proc ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_all_instances_dqs_pins { ddr_db_par } {
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
   }

   return $dqs_pins
}

proc ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_calculate_counter_value { cnt_hi cnt_lo cnt_bypass } {
   if {$cnt_bypass} {
      set result 1
   } else {
      set result [expr {$cnt_hi + $cnt_lo}]
   }
   return $result
}

proc ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_input_clk_id { pll_inclk_id var_array_name} {
   upvar 1 $var_array_name var

   array set results_array [list]

   ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_traverse_fanin_up_to_depth $pll_inclk_id ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_is_node_type_pin clock results_array $var(pll_inclock_search_depth)
   if {[array size results_array] == 1} {
      set pin_id [lindex [array names results_array] 0]
      set result $pin_id
   } else {
      post_message -type critical_warning "Could not find PLL clock for [get_node_info -name $pll_inclk_id]"
      set result -1
   }

   return $result
}

proc ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_output_clock_id { pin_list pin_type msg_list_name var_array_name} {
   upvar 1 $msg_list_name msg_list
   upvar 1 $var_array_name var
   set output_clock_id -1

   set output_id_list [list]
   set pin_collection [get_keepers -no_duplicates $pin_list]
   if {[get_collection_size $pin_collection] == [llength $pin_list]} {
      foreach_in_collection id $pin_collection {
         lappend output_id_list $id
      }
   } elseif {[get_collection_size $pin_collection] == 0} {
      lappend msg_list "warning" "Could not find any $pin_type pins"
   } else {
      lappend msg_list "warning" "Could not find all $pin_type pins"
   }
   ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_pll_clock $output_id_list $pin_type output_clock_id $var(pll_outclock_search_depth)
   return $output_clock_id
}

proc ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_pll_clock { dest_id_list node_type clock_id_name search_depth} {
   if {$clock_id_name != ""} {
      upvar 1 $clock_id_name clock_id
   }
   set clock_id -1

   array set clk_array [list]
   foreach node_id $dest_id_list {
      ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_traverse_fanin_up_to_depth $node_id ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_is_node_type_pll_clk clock clk_array $search_depth
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

proc ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_vco_clk_id { wf_clock_id var_array_name} {
   upvar 1 $var_array_name var

   array set results_array [list]

   ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_traverse_fanin_up_to_depth $wf_clock_id ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_is_node_type_vco clock results_array $var(pll_vcoclock_search_depth)
   if {[array size results_array] == 1} {
      set pin_id [lindex [array names results_array] 0]
      set result $pin_id
   } else {
      post_message -type critical_warning "Could not find VCO clock for [get_node_info -name $wf_clock_id]"
      set result -1
   }

   return $result
}

proc ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_is_node_type_pll_clk { node_id } {
   set cell_id [get_node_info -cell $node_id]

   if {$cell_id == ""} {
      set result 0
   } else {
      set atom_type [get_cell_info -atom_type $cell_id]
      if {$atom_type == "IOPLL"} {
         set node_name [get_node_info -name $node_id]

         if  {[regexp {pll_inst~.*OUTCLK[0-9]$} $node_name]} {
            set result 1
         } else {
            set result 0
         }
      } elseif {$atom_type == "TILE_CTRL"} {
         set node_name [get_node_info -name $node_id]

         if {[regexp {tile_ctrl_inst.*\|pa_core_clk_out\[[0-9]\]$} $node_name]} {
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

proc ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_is_node_type_vco { node_id } {
   set cell_id [get_node_info -cell $node_id]

   if {$cell_id == ""} {
      set result 0
   } else {
      set atom_type [get_cell_info -atom_type $cell_id]
      if {$atom_type == "IOPLL"} {
         set node_name [get_node_info -name $node_id]

         if {[regexp {pll_inst.*\|.*vcoph\[0\]$} $node_name]} {
            set result 1
         } elseif {[regexp {pll_inst.*VCOPH0$} $node_name]} {
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

proc ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_does_ref_clk_exist { ref_clk_name } {

   set ref_clock_found 0
   foreach_in_collection iclk [get_clocks -nowarn] {
      set clk_targets [get_clock_info -target $iclk]
      foreach_in_collection itgt $clk_targets {
         set node_name [get_node_info -name $itgt]
         if {[string compare $node_name $ref_clk_name] == 0} {
            set ref_clock_found 1
            break
         }
      }
      if {$ref_clock_found == 1} {
         break;
      }
   }

   return $ref_clock_found
}

proc ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_p2c_c2p_clock_uncertainty { instname var_array_name } {

   set success 1
   set error_message ""
   set clock_uncertainty 0
   set debug 0

   package require ::quartus::atoms
   upvar 1 $var_array_name var

   catch {read_atom_netlist} read_atom_netlist_out
   set read_atom_netlist_error [regexp "ERROR" $read_atom_netlist_out]

   if {$read_atom_netlist_error == 0} {
      if {[ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_are_entity_names_on]} {
         regsub -all {\|} $instname "|*:" instname
      }
      regsub -all {\\} $instname {\\\\} instname
      regsub -all {\[} $instname "\\\[" instname
      regsub -all {\]} $instname "\\\]" instname

      # Find the IOPLLs
      if {$success == 1} {
         if {[ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_are_entity_names_on]} {
            set pll_atoms [get_atom_nodes -matching *${instname}|*:arch|*:arch_inst|*:pll_inst|* -type IOPLL]
         } else {
            set pll_atoms [get_atom_nodes -matching *${instname}|arch|arch_inst|pll_inst|* -type IOPLL]
         }
         set num_pll_inst [get_collection_size $pll_atoms]

         if {$num_pll_inst == 0} {
            set success 0
            post_message -type critical_warning "The auto-constraining script was not able to detect any PLLs in the < $instname > memory interface."
         }
      }

      # Get atom parameters
      if {$success == 1} {

         set mcnt_list [list]
         set bw_list   [list]
         set cp_setting_list [list]
         set vco_period_list [list]

         foreach_in_collection pll_atom $pll_atoms {

            # M-counter value
            if {[get_atom_node_info -node $pll_atom -key  BOOL_IOPLL_M_CNT_BYPASS_EN] == 1} {
               set mcnt 1
            } else {
               set mcnt [expr [get_atom_node_info -node $pll_atom -key INT_IOPLL_M_CNT_HI_DIV] + [get_atom_node_info -node $pll_atom -key INT_IOPLL_M_CNT_LO_DIV]]
            }
            lappend mcnt_list $mcnt

            # BW
            set bw [get_atom_node_info -node $pll_atom -key  ENUM_PLL_BANDWIDTH_SELECT]
            if {[string compare -nocase $bw "AUTO"] == 0} {
               set bw "LOW"
            }
            lappend bw_list $bw

            # CP current setting
            set cp_setting [get_atom_node_info -node $pll_atom -key ENUM_IOPLL_PLL_CP_CURRENT]
            lappend cp_setting_list $cp_setting

            # VCO frequency setting
            set vco_period [get_atom_node_info -node $pll_atom -key TIME_VCO_FREQUENCY]
            lappend vco_period_list $vco_period
         }

         # Make sure all IOPLL parameters are the same
         for {set i [expr [llength $mcnt_list] - 1]} {$i > 0} {set i [expr $i - 1]} {
            if {[lindex $mcnt_list $i] != [lindex $mcnt_list [expr $i - 1]]} {
               set success 0
               post_message -type critical_warning "The auto-constraining script found multiple PLLs in the < $instname > memory interface with different parameters."
            }
         }
         for {set i [expr [llength $bw_list] - 1]} {$i > 0} {set i [expr $i - 1]} {
            set bw_a [lindex $bw_list $i]
            set bw_b [lindex $bw_list [expr $i - 1]]
            if {[string compare -nocase $bw_a $bw_b] != 0} {
               set success 0
               post_message -type critical_warning "The auto-constraining script found multiple PLLs in the < $instname > memory interface with different parameters."
            }
         }
         for {set i [expr [llength $cp_setting_list] - 1]} {$i > 0} {set i [expr $i - 1]} {
            set cp_a [lindex $cp_setting_list $i]
            set cp_b [lindex $cp_setting_list [expr $i - 1]]
            if {[string compare -nocase $cp_a $cp_b] != 0} {
               set success 0
               post_message -type critical_warning "The auto-constraining script found multiple PLLs in the < $instname > memory interface with different parameters."
            }
         }
         for {set i [expr [llength $vco_period_list] - 1]} {$i > 0} {set i [expr $i - 1]} {
            set vco_a [lindex $vco_period_list $i]
            set vco_b [lindex $vco_period_list [expr $i - 1]]
            if {[string compare -nocase $vco_a $vco_b] != 0} {
               set success 0
               post_message -type critical_warning "The auto-constraining script found multiple PLLs in the < $instname > memory interface with different parameters."
            }
         }
      }

      # Calculate clock uncertainty
      if {$success == 1} {

         set mcnt [lindex $mcnt_list 0]
         set bw   [string toupper [lindex $bw_list 0]]
         set cp_setting [lindex $cp_setting_list 0]
         set cp_current [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_cp_current_from_setting $cp_setting]
         set vco_period [lindex $vco_period_list 0]
         if {[regexp {([0-9]+) ps} $vco_period matched vco_period] == 1} {
         } else {
            post_message -type critical_warning "The auto-constraining script was not able to read the netlist."
            set success 0
         }

         if {$debug} {
            puts "MCNT : $mcnt"
            puts "BW   : $bw"
            puts "CP   : $cp_setting ($cp_current)"
            puts "VCO  : $vco_period"
         }

         # For VCO frequencies of > 600MHz (period < 1667ps) use the normal way of getting PLL clk uncertaint parameters
         if {$vco_period <= 1667} {
            set HFR  [get_clock_uncertainty_data NOM NOM NOM PLL OFFSET${mcnt} HFR $bw]
            set LFD  [get_clock_uncertainty_data NOM NOM NOM PLL OFFSET${mcnt} LFD $bw]
            set SPE  [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_spe_from_cp_current $cp_current]

         } elseif {
           ($vco_period > 3572) ||                                       
           ([string compare -nocase $bw "HIGH"] != 0) ||                 
           ($mcnt < 2) ||                                                
           (($mcnt > 8) && ($vco_period > 2500))                         
           } {

            post_message -type critical_warning "The PLL parameters are out of range."
            set HFR  1000e-12
            set LFD  1000e-12
            set SPE  1000e-12
            set success 0

         } else {
            set clk_uncertainty_params [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_alternate_clock_uncertainty_parameters $mcnt $bw $cp_current $vco_period]
            set HFR  [lindex $clk_uncertainty_params 0]
            set LFD  [lindex $clk_uncertainty_params 1]
            set SPE  [lindex $clk_uncertainty_params 2]
         }

         if {$success == 1} {
            set clock_uncertainty_sqrt  [expr sqrt(($LFD/2)*($LFD/2) + ($LFD/2)*($LFD/2))]
            set clock_uncertainty [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_round_3dp [expr ($clock_uncertainty_sqrt + $SPE)*1e9]]

            if {$debug} {
               puts "HFR  : $HFR"
               puts "LFD  : $LFD"
               puts "SPE  : $SPE"
               puts "TOTAL: $clock_uncertainty"
            }
         }
      }

   } else {
      set success 0
      post_message -type critical_warning "The auto-constraining script was not able to read the netlist."
   }

   # Output warning in the case that clock uncertainty can't be determined
   if {$success == 0} {
      post_message -type critical_warning "Verify the following:"
      post_message -type critical_warning " The core < $instname > is instantiated within another component (wrapper)"
      post_message -type critical_warning " The core is not the top-level of the project"
      post_message -type critical_warning " The memory interface pins are exported to the top-level of the project"
      post_message -type critical_warning " The core  < $instname > RTL has not been modified manually"
   }

   return $clock_uncertainty
}


proc ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_cp_current_from_setting { cp_setting } {

   set cp_current 0

   if {[string compare -nocase $cp_setting "PLL_CP_SETTING8"] == 0} {
      set cp_current 10
   } elseif {[string compare -nocase $cp_setting "PLL_CP_SETTING10"] == 0} {
      set cp_current 20
   } elseif {[string compare -nocase $cp_setting "PLL_CP_SETTING12"] == 0} {
      set cp_current 40
   } elseif {[string compare -nocase $cp_setting "PLL_CP_SETTING14"] == 0} {
      set cp_current 15
   } elseif {[string compare -nocase $cp_setting "PLL_CP_SETTING15"] == 0} {
      set cp_current 30
   } elseif {[string compare -nocase $cp_setting "PLL_CP_SETTING21"] == 0} {
      set cp_current 25
   } elseif {[string compare -nocase $cp_setting "PLL_CP_SETTING28"] == 0} {
      set cp_current 35
   } elseif {[string compare -nocase $cp_setting "PLL_CP_SETTING35"] == 0} {
      set cp_current 45
   } else {
      set cp_current 0
   }
}

proc ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_spe_from_cp_current { cp_current } {

   set spe 147.0e-12

   if {$cp_current <= 15} {
      set spe 147.0e-12
   } elseif {$cp_current <= 20} {
      set spe 117.0e-12
   } elseif {$cp_current <= 25} {
      set spe 87.0e-12
   } elseif {$cp_current <= 30} {
      set spe 75.0e-12
   } elseif {$cp_current <= 35} {
      set spe 60.0e-12
   } elseif {$cp_current <= 40} {
      set spe 55.0e-12
   } elseif {$cp_current <= 45} {
      set spe 49.0e-12
   } else {
      set spe 49.0e-12
   }
}

proc ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_odv_reduction_factor {master_pll_location inst phyclk_name} {

   set odv_reduction_factor 0.0
   set debug 0

   set phy_clocks [get_clocks $phyclk_name]
   foreach_in_collection c $phy_clocks { set phy_clock_targets [get_clock_info -targets $c] }
   foreach_in_collection p $phy_clock_targets { set pll_cell [get_node_info -cell $p] }
   set pll_location [get_cell_info -location $pll_cell]

   if {[regexp {IOPLL_X([0-9]+)_Y([0-9]+)_N([0-9]+)$} $master_pll_location matched master_x master_y master_n] == 1} {
      if {[regexp {IOPLL_X([0-9]+)_Y([0-9]+)_N([0-9]+)$} $pll_location matched x y n] == 1} {

      if {$master_x == $x} {

         if {$master_y == $y} {
            set odv_reduction_factor 0.0
         } else {
            set y_distance [expr abs($master_y - $y)]
            set y_distance_tiles [expr $y_distance / 27]

            set odv_reduction_factor 0
            if {$odv_reduction_factor < 0} {
               set odv_reduction_factor 0.0
            }
         }

         if {$debug == 1} {
            puts "$master_pll_location $pll_location $y_distance $y_distance_tiles $odv_reduction_factor"
         }

      } else {
         set odv_reduction_factor 0.0
       }
     } else {
        set odv_reduction_factor 0.0
     }
   } else {
      set odv_reduction_factor 0.0
   }

   return $odv_reduction_factor
}

proc ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_a10_iopll_workaround_present {} {
   set oscs [get_pins -nowarn -compatibility_mode *ALTERA_INSERTED_OSCILLATOR_FOR_IOPLL\|clkout]
   set num_oscs 0
   foreach_in_collection o $oscs {
      incr num_oscs
   }
   if {$num_oscs > 0} {
      return true
   } else {
      return false
   }
}

proc ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_periphery_clock_uncertainty { results_array_name var_array_name } {
   upvar 1 $results_array_name results
   upvar 1 $var_array_name var

   set speed_temp_grade [get_speedgrade_name]

   set c2p_setup  [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_round_3dp [expr [get_clock_uncertainty_data 900MV $speed_temp_grade EMIF SETUP C2P]*1e9 + ($var(PHY_REF_CLK_JITTER_PS) - 10.0)/1000.0]]
   set c2p_hold   [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_round_3dp [expr [get_clock_uncertainty_data 900MV $speed_temp_grade EMIF HOLD C2P]*1e9  + ($var(PHY_REF_CLK_JITTER_PS) - 10.0)/1000.0]]
   set p2c_setup  [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_round_3dp [expr [get_clock_uncertainty_data 900MV $speed_temp_grade EMIF SETUP P2C]*1e9 + ($var(PHY_REF_CLK_JITTER_PS) - 10.0)/1000.0]]
   set p2c_hold   [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_round_3dp [expr [get_clock_uncertainty_data 900MV $speed_temp_grade EMIF HOLD P2C]*1e9  + ($var(PHY_REF_CLK_JITTER_PS) - 10.0)/1000.0]]
   
   if {[ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_a10_iopll_workaround_present]} {
      set c2p_setup   [expr $c2p_setup + 0.004]
      set c2p_hold    [expr $c2p_hold  + 0.004]
      set p2c_setup   [expr $p2c_setup + 0.004]
      set p2c_hold    [expr $p2c_hold  + 0.004]
   }

   set results [list $c2p_setup $c2p_hold $p2c_setup $p2c_hold]
}

proc ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_core_clock_uncertainty { results_array_name var_array_name } {
   upvar 1 $results_array_name results
   upvar 1 $var_array_name var

   set c2c_same_setup  0
   set c2c_same_hold   0
   set c2c_diff_setup  0
   set c2c_diff_hold   0

   set results [list $c2c_same_setup $c2c_same_hold $c2c_diff_setup $c2c_diff_hold]
}

proc ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_core_overconstraints { results_array_name var_array_name } {
   upvar 1 $results_array_name results
   upvar 1 $var_array_name var

   set speed_temp_grade [get_speedgrade_name]

   set results [list $var(C2C_SAME_CLK_SETUP_OC_NS) $var(C2C_SAME_CLK_HOLD_OC_NS) $var(C2C_DIFF_CLK_SETUP_OC_NS) $var(C2C_DIFF_CLK_HOLD_OC_NS)]
}

proc ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_periphery_overconstraints { results_st_array_name results_mt_array_name var_array_name } {
   upvar 1 $results_st_array_name results_st
   upvar 1 $results_mt_array_name results_mt
   upvar 1 $var_array_name var

   set c2p_p2c_frequency [expr $var(PHY_MEM_CLK_FREQ_MHZ)/$var(C2P_P2C_CLK_RATIO)]

   set results_st [list $var(C2P_SETUP_OC_NS) $var(C2P_HOLD_OC_NS) $var(P2C_SETUP_OC_NS) $var(P2C_HOLD_OC_NS)]
   set results_mt [list [expr $var(C2P_SETUP_OC_NS) + 0.000] [expr $var(C2P_HOLD_OC_NS) + 0.000] [expr $var(P2C_SETUP_OC_NS) + 0.000] [expr $var(P2C_HOLD_OC_NS) + 0.000]]

}


proc ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_sort_duplicate_names { names_array } {

   set main_name ""
   set duplicate_names [list]

   # Find the main name as opposed to all the duplicate names
   foreach { name } $names_array {
      if  {[regexp {Duplicate} $name]} {
         lappend duplicate_names $name
      } else {
         if {$main_name == ""} {
            set main_name $name
         } else {
            post_message -type error "More than one main tile name ($main_name and $name).  Please verify the connectivity of these pins."
         }
      }
   }

   # Now sort the duplicate names
   set duplicate_names [lsort -decreasing $duplicate_names]

   # Prepend the main name and then return
   set result [join [linsert $duplicate_names 0 $main_name]]

   return $result
}


proc ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_alternate_clock_uncertainty_parameters { mcnt bw cp_current vco_period } {

    set success 1

    if {($vco_period <= 1667) || ($vco_period > 3572) || ([string compare -nocase $bw "HIGH"] != 0) || ($mcnt < 2)} {
    
        post_message -type error "PLL parameters are inappropriate for this function to be called."
        set success 0
    
    # For VCO frequencies between 400MHz and 600MHz
    } elseif {$vco_period <= 2500} {

        if {$mcnt == 2} {
            set HFR 171e-12
            set LFD 213e-12
        } elseif {$mcnt == 3} {
            set HFR 159e-12
            set LFD 206e-12
        } elseif {$mcnt == 4} {
            set HFR 158e-12
            set LFD 137e-12
        } elseif {$mcnt == 5} {
            set HFR 175e-12
            set LFD 240e-12
        } elseif {$mcnt == 6} {
            set HFR 183e-12
            set LFD 219e-12
        } elseif {$mcnt == 7} {
            set HFR 190e-12
            set LFD 232e-12
        } elseif {$mcnt == 8} {
            set HFR 158e-12
            set LFD 174e-12
        } elseif {$mcnt == 9} {
            set HFR 202e-12
            set LFD 225e-12
        } elseif {$mcnt <= 15} {
            set HFR 236e-12
            set LFD 264e-12
        } else {
            post_message -type critical_warning "PLL parameters are inappropriate for this function to be called."
            set success 0
        }

    # For VCO frequencies between 280MHz and 400MHz
    } else {
        if {$mcnt == 2} {
            set HFR 120e-12
            set LFD 133e-12
        } elseif {$mcnt == 3} {
            set HFR 207e-12
            set LFD 257e-12
        } elseif {$mcnt == 4} {
            set HFR 139e-12
            set LFD 127e-12
        } elseif {$mcnt == 5} {
            set HFR 234e-12
            set LFD 304e-12
        } elseif {$mcnt == 6} {
            set HFR 248e-12
            set LFD 276e-12
        } elseif {$mcnt == 7} {
            set HFR 259e-12
            set LFD 293e-12
        } elseif {$mcnt == 8} {
            set HFR 287e-12
            set LFD 241e-12
        } else {
            post_message -type critical_warning "PLL parameters are inappropriate for this function to be called."
            set success 0
        }
    }

    set SPE  [expr [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_spe_from_cp_current $cp_current] + 25e-12]

    if {$success == 0} {
        set HFR  1000e-12
        set LFD  1000e-12
        set SPE  1000e-12
    }
    
    set return_value [list $HFR $LFD $SPE]
    return $return_value
}
