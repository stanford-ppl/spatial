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


#############################################################
# Read Timing Analysis
#############################################################
proc ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_perform_read_capture_analysis {opcname inst pin_array_name var_array_name summary_name} {

   set analysis_name "Read Capture"

   #######################################
   # Need access to global variables
   upvar 1 $summary_name global_summary
   upvar 1 $var_array_name var
   upvar 1 $pin_array_name pins

   # Debug switch. Change to 1 to get more run-time debug information
   set debug 0   
   set result 1

   ###############################
   # Write summary report
   ###############################

   set positive_fcolour [list "black" "blue"]
   set negative_fcolour [list "black" "red" ]

   set summary [list]
   
   set var(RD_UI) [expr $var(UI)/2]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter RD_UI]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(RD_UI)]]

   set var(RD_ISI) $var(RD_ISI)
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter RD_ISI]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(RD_ISI)]]

   set var(RD_SSI) $var(RD_SSI)
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter RD_SSI]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(RD_SSI)]]

   set var(RD_DQSQ) [expr $var(tDQSQ)]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter RD_DQSQ]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(RD_DQSQ)]]

   set var(RD_QH) [expr (1-$var(tQH)*2)*$var(tCK)/2]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter RD_QH]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(RD_QH)]]

   set var(RD_MPR) [expr ($var(WITH_MPR) == 1 ? -$var(RD_DQSQ)*$var(MPR_DQSQ)-$var(RD_QH)*$var(MPR_QH) : 0)]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter RD_MPR]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(RD_MPR)]]

   set var(RD_JITTER) $var(RD_JITTER)
   set var(RD_JITTER_sens) [expr ([expr (($var(UI))*1000.0-$var(EXTRACTED_PERIOD))*$var(RD_JITTER_SENS_TO_PERIOD)])/1000.0]
   if {$var(RD_JITTER_sens) > 0} {
      set var(RD_JITTER) [expr $var(RD_JITTER) + $var(RD_JITTER_sens)]
   }
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter RD_JITTER]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(RD_JITTER)]]

   set var(RD_DCD) $var(RD_DCD)
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter RD_DCD]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(RD_DCD)]]

   set var(RD_SH) $var(RD_SH)
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter RD_SH]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(RD_SH)]]

   set var(RD_EOL) $var(RD_EOL)
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter RD_EOL]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(RD_EOL)]]

   set var(RD_CAL_UNC) [expr $var(RD_CALIBRATION_LOSS_OTHER)+($var(IS_DLL_ON) == 1 ? 0 : $var(RD_TEMP_CAL_LOSS_WO_DLL))+($var(OCT_RECAL) == 1 ? $var(RD_TEMP_CAL_LOSS_OCT_RECAL) : $var(RD_TEMP_CAL_LOSS_WO_OCT_RECAL))+(([string compare $var(PROTOCOL) "DDR4"] == 0) ? ($var(RDBI) == 1 ? 0 : $var(RD_DBI_EFFECT)) : (([string compare $var(PROTOCOL) "QDRIV"] == 0) ? ($var(RDBI) == 1 ? 0 : $var(RD_DBI_EFFECT)) : 0))+($var(IS_COMPONENT) == 1 ? ($var(TERMINATION_LESS_THAN_120) == 1 ? $var(TERMINATION_LOSS_DEVICE_60) : $var(TERMINATION_LOSS_DEVICE_120)) : ($var(TERMINATION_LESS_THAN_120) == 1 ? $var(TERMINATION_LOSS_DIMM_60) : $var(TERMINATION_LOSS_DIMM_120)))]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter RD_CAL_UNC]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(RD_CAL_UNC)]]

   set var(RD_SK_EFFECT) [expr ($var(BD_PKG_SKEW)-$var(DEFAULT_BD_PKG_SKEW))*$var(BD_SK_SENS_RD)]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter RD_SK_EFFECT]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(RD_SK_EFFECT)]]

   set var(RD_MARGIN) [expr $var(RD_UI)-$var(RD_ISI)-$var(RD_SSI)-$var(RD_DQSQ)-$var(RD_QH)-$var(RD_MPR)-$var(RD_JITTER)-$var(RD_DCD)-$var(RD_SH)-$var(RD_EOL)-$var(RD_CAL_UNC)-$var(RD_SK_EFFECT)]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter RD_MARGIN]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(RD_MARGIN)]]
   
   #######################################
   #######################################
   # Create the read analysis panel   
   
   set setup_slack [expr $var(RD_MARGIN)/2]
   set hold_slack  [expr $var(RD_MARGIN)/2]

   set panel_name "$inst $analysis_name"
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
   
   if {($setup_slack < 0) || ($hold_slack <0)} {
      set panel_id [create_report_panel -table $panel_name -color red]
   } else {
      set panel_id [create_report_panel -table $panel_name]
   }
   add_row_to_table -id $panel_id [list "Operation" "Margin"]      
   
   foreach summary_line $summary {
      add_row_to_table -id $panel_id $summary_line -fcolors $positive_fcolour
   }
   
   lappend global_summary [list $opcname 0 "$analysis_name ($opcname)" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $setup_slack] [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $hold_slack]]
}


#############################################################
# Write Timing Analysis
#############################################################
proc ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_perform_write_launch_analysis {opcname inst pin_array_name var_array_name summary_name} {

   set analysis_name "Write"

   #######################################
   # Need access to global variables
   upvar 1 $summary_name global_summary
   upvar 1 $var_array_name var
   upvar 1 $pin_array_name pins

   # Debug switch. Change to 1 to get more run-time debug information
   set debug 0   
   set result 1

   ###############################
   # Write summary report
   ###############################

   set positive_fcolour [list "black" "blue"]
   set negative_fcolour [list "black" "red" ]

   set summary [list]
   
   set var(WR_UI) [expr $var(UI)/2]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter WR_UI]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(WR_UI)]]

   set var(WR_ISI) $var(WR_ISI)
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter WR_ISI]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(WR_ISI)]]

   set var(WR_SSO) $var(WR_SSO)
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter WR_SSO]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(WR_SSO)]]

   set var(WR_DS) [expr $var(tDS)]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter WR_DS]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(WR_DS)]]

   set var(WR_DH) [expr $var(tDH)]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter WR_DH]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(WR_DH)]]

   set var(WR_MPR) [expr ($var(WITH_MPR) == 1 ? -$var(WR_DS)*$var(MPR_DS) -$var(WR_DH)*$var(MPR_DH) : 0)]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter WR_MPR]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(WR_MPR)]]

   set var(WR_JITTER) $var(WR_JITTER)
   set var(WR_JITTER_sens) [expr ([expr (($var(UI))*1000.0-$var(EXTRACTED_PERIOD))*$var(WR_JITTER_SENS_TO_PERIOD)])/1000.0]
   if {$var(WR_JITTER_sens) > 0} {
      set var(WR_JITTER) [expr $var(WR_JITTER) + $var(WR_JITTER_sens)]
   }
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter WR_JITTER]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(WR_JITTER)]]

   set var(WR_DCD) $var(WR_DCD)
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter WR_DCD]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(WR_DCD)]]

   set var(WR_EOL) $var(WR_EOL)
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter WR_EOL]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(WR_EOL)]]

   set var(WR_CAL_UNC) [expr $var(WR_CALIBRATION_LOSS_OTHER)+($var(OCT_RECAL) == 1 ? $var(WR_TEMP_CAL_LOSS_OCT_RECAL) : $var(WR_TEMP_CAL_LOSS_WO_OCT_RECAL))]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter WR_CAL_UNC]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(WR_CAL_UNC)]]

   set var(WR_SK_EFFECT) [expr ($var(BD_PKG_SKEW)-$var(DEFAULT_BD_PKG_SKEW))*$var(BD_SK_SENS_WR)]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter WR_SK_EFFECT]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(WR_SK_EFFECT)]]

   set var(WR_MARGIN) [expr $var(WR_UI)-$var(WR_ISI)-$var(WR_SSO)-$var(WR_DS)-$var(WR_DH)-$var(WR_MPR)-$var(WR_JITTER)-$var(WR_DCD)-$var(WR_EOL)-$var(WR_CAL_UNC)-$var(WR_SK_EFFECT)]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter WR_MARGIN]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(WR_MARGIN)]]
   
   #######################################
   #######################################
   # Create the write analysis panel   
   set setup_slack [expr $var(WR_MARGIN)/2]
   set hold_slack  [expr $var(WR_MARGIN)/2]

   set panel_name "$inst $analysis_name"
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
   
   if {($setup_slack < 0) || ($hold_slack <0)} {
      set panel_id [create_report_panel -table $panel_name -color red]
   } else {
      set panel_id [create_report_panel -table $panel_name]
   }
   add_row_to_table -id $panel_id [list "Operation" "Margin"]       
   
   foreach summary_line $summary {
      add_row_to_table -id $panel_id $summary_line -fcolors $positive_fcolour
   }
   
   lappend global_summary [list $opcname 0 "$analysis_name ($opcname)" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $setup_slack] [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $hold_slack]]
}

#############################################################
# Address/Command Timing Analysis
#############################################################
proc ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_perform_ac_analysis {opcname inst pin_array_name var_array_name summary_name} {

   set analysis_name "Address/Command"


   #######################################
   # Need access to global variables
   upvar 1 $summary_name global_summary
   upvar 1 $var_array_name var
   upvar 1 $pin_array_name pins

   # Debug switch. Change to 1 to get more run-time debug information
   set debug 0   
   set result 1

   ###############################
   # AC summary report
   ###############################

   set positive_fcolour [list "black" "blue"]
   set negative_fcolour [list "black" "red" ]

   set summary [list]
   
   set var(AC_UI) [expr (([string compare $var(PROTOCOL) "QDRIV"] == 0) ? $var(UI)/2 : (([string compare $var(PROTOCOL) "LPDDR3"] == 0) ? $var(UI)/2 : $var(UI)))]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter AC_UI]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(AC_UI)]]

   set var(CA_ISI) $var(CA_ISI)
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter CA_ISI]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(CA_ISI)]]

   set var(CA_SSO) $var(CA_SSO)
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter CA_SSO]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(CA_SSO)]]

   set var(CA_IS) [expr $var(tIS)]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter CA_IS]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(CA_IS)]]

   set var(CA_IH) [expr $var(tIH)]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter CA_IH]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(CA_IH)]]

   set var(CA_MPR) [expr ($var(WITH_CA_CALIB) == 1 ? ($var(NUM_RANKS) == 1 ? ($var(WITH_MPR) == 1 ? -$var(CA_IS)*$var(MPR_IS) - $var(CA_IH)*$var(MPR_IH) : 0) : 0) : 0)]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter CA_MPR]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(CA_MPR)]]

   set var(CA_JITTER) $var(CA_JITTER)
   set var(CA_JITTER_sens) [expr ([expr (($var(UI))*1000.0-$var(EXTRACTED_PERIOD))*$var(CA_JITTER_SENS_TO_PERIOD)])/1000.0]
   if {$var(CA_JITTER_sens) > 0} {
      set var(CA_JITTER) [expr $var(CA_JITTER) + $var(CA_JITTER_sens)]
   }
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter CA_JITTER]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(CA_JITTER)]]

   set var(CA_DCD) $var(CA_DCD)
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter CA_DCD]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(CA_DCD)]]

   set var(CA_EOL) $var(CA_EOL)
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter CA_EOL]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(CA_EOL)]]

   set var(CA_CAL_UNC) [expr ($var(WITH_CA_CALIB) == 1 ? $var(CA_CALIBRATION_LOSS_OTHER)+($var(OCT_RECAL) == 1 ? $var(CA_TEMP_CAL_LOSS_OCT_RECAL_CA_CAL) : $var(CA_TEMP_CAL_LOSS_WO_OCT_RECAL_CA_CAL)) : 0)]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter CA_CAL_UNC]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(CA_CAL_UNC)]]

   set var(CA_PVT) [expr ($var(WITH_CA_CALIB) == 1 ? 0 : $var(CA_CALIBRATION_LOSS_OTHER)+($var(OCT_RECAL) == 1 ? $var(CA_TEMP_CAL_LOSS_OCT_RECAL_WO_CA_CAL) : $var(CA_TEMP_CAL_LOSS_WO_OCT_RECAL_WO_CA_CAL)))]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter CA_PVT]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(CA_PVT)]]

   set var(CA_SK_EFFECT) [expr ($var(WITH_CA_CALIB) == 1 ? ($var(CA_BD_PKG_SKEW)-$var(DEFAULT_BD_PKG_SKEW))*$var(BD_SK_SENS_CA) : ($var(CA_BD_PKG_SKEW)-$var(DEFAULT_BD_PKG_SKEW))*$var(BD_SK_SENS_CA_WO_CALIB)+$var(CA_BD_PKG_SKEW)+($var(CA_TO_CK_BD_PKG_SKEW) < 0 == 1 ?  -2*$var(CA_TO_CK_BD_PKG_SKEW) :  2*$var(CA_TO_CK_BD_PKG_SKEW)))]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter CA_SK_EFFECT]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(CA_SK_EFFECT)]]

   set var(CA_MARGIN) [expr $var(AC_UI)-$var(CA_ISI)-$var(CA_SSO)-$var(CA_IS)-$var(CA_IH)-$var(CA_MPR)-$var(CA_JITTER)-$var(CA_DCD)-$var(CA_EOL)-$var(CA_CAL_UNC)-$var(CA_PVT)-$var(CA_SK_EFFECT)]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter CA_MARGIN]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(CA_MARGIN)]]
   
   
   #######################################
   #######################################
   # Create the AC analysis panel   
   set setup_slack [expr $var(CA_MARGIN)/2]
   set hold_slack  [expr $var(CA_MARGIN)/2]

   set panel_name "$inst $analysis_name"
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
   
   if {($setup_slack < 0) || ($hold_slack <0)} {
      set panel_id [create_report_panel -table $panel_name -color red]
   } else {
      set panel_id [create_report_panel -table $panel_name]
   }
   add_row_to_table -id $panel_id [list "Operation" "Margin"]      
   
   foreach summary_line $summary {
      add_row_to_table -id $panel_id $summary_line -fcolors $positive_fcolour
   }
   
   lappend global_summary [list $opcname 0 "$analysis_name ($opcname)" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $setup_slack] [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $hold_slack]]
}

#############################################################
# DQS Gating Timing Analysis
#############################################################
proc ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_perform_dqs_gating_analysis  {opcname inst pin_array_name var_array_name summary_name} {

   set analysis_name "DQS Gating"

   #######################################
   # Need access to global variables
   upvar 1 $summary_name global_summary
   upvar 1 $var_array_name var
   upvar 1 $pin_array_name pins

   # Debug switch. Change to 1 to get more run-time debug information
   set debug 0   
   set result 1


   # Only perform for DDR protocols 
   if { !(([string compare $var(PROTOCOL) "DDR4"] == 0) || ([string compare $var(PROTOCOL) "DDR3"] == 0) || ([string compare $var(PROTOCOL) "LPDDR3"] == 0))} {
      return
   }
   
   ###############################
   # DQS Gating summary report
   ###############################

   set positive_fcolour [list "black" "blue"]
   set negative_fcolour [list "black" "red" ]

   set summary [list]
   
   set var(DQSG_UI) [expr (([string compare $var(PROTOCOL) "DDR4"] == 0) ? ($var(X4) == 1 ? $var(UI) : 2*$var(UI)) : ((([string compare $var(PROTOCOL) "DDR3"] == 0) ? $var(UI)*0.8 : (([string compare $var(PROTOCOL) "LPDDR3"] == 0) ? $var(UI)*0.8 : $var(UI)))))]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter DQSG_UI]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(DQSG_UI)]]

   set var(DQSG_ISI) $var(DQSG_ISI)
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter DQSG_ISI]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(DQSG_ISI)]]

   set var(DQSG_SSI) $var(DQSG_SSI)
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter DQSG_SSI]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(DQSG_SSI)]]

   set var(DQSG_DQSCK) [expr (([string compare $var(PROTOCOL) "LPDDR3"] == 0) ? $var(tDQSCK) : $var(tDQSCK)*2)]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter DQSG_DQSCK]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(DQSG_DQSCK)]]

   set var(DQSG_MPR) [expr ($var(WITH_MPR) == 1 ? -$var(DQSG_DQSCK)*$var(MPR_DQSCK) : 0)]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter DQSG_MPR]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(DQSG_MPR)]]

   set var(DQSG_JITTER) $var(DQSG_JITTER)
   set var(DQSG_JITTER_sens) [expr ([expr (($var(UI))*1000.0-$var(EXTRACTED_PERIOD))*$var(DQSG_JITTER_SENS_TO_PERIOD)])/1000.0]
   if {$var(DQSG_JITTER_sens) > 0} {
      set var(DQSG_JITTER) [expr $var(DQSG_JITTER) + $var(DQSG_JITTER_sens)]
   }
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter DQSG_JITTER]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(DQSG_JITTER)]]

   set var(DQSG_DCD) $var(DQSG_DCD)
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter DQSG_DCD]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(DQSG_DCD)]]

   set var(DQSG_EOL) $var(DQSG_EOL)
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter DQSG_EOL]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(DQSG_EOL)]]

   set var(DQSG_CAL_UNC) $var(DQSG_CAL_UNC)
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter DQSG_CAL_UNC]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(DQSG_CAL_UNC)]]

   set var(DQSG_TRK_UNC) $var(DQSG_TRK_UNC)
   set var(DQSG_TRK_UNC_sens) [expr ([expr (($var(UI))*1000.0-$var(EXTRACTED_PERIOD))*$var(DQSG_TRKUNC_SENS_TO_PERIOD)])/1000.0]
   if {$var(DQSG_TRK_UNC_sens) > 0} {
      set var(DQSG_TRK_UNC) [expr $var(DQSG_TRK_UNC) + $var(DQSG_TRK_UNC_sens)]
   }
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter DQSG_TRK_UNC]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(DQSG_TRK_UNC)]]

   set var(DQSG_SH) $var(DQSG_SH)
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter DQSG_SH]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(DQSG_SH)]]

   set var(DQSG_SK_EFFECT) [expr ($var(BD_PKG_SKEW)-$var(DEFAULT_BD_PKG_SKEW))*$var(BD_SK_SENS_DQSG)]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter DQSG_SK_EFFECT]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(DQSG_SK_EFFECT)]]

   set var(DQSG_MARGIN) [expr $var(DQSG_UI)-$var(DQSG_ISI)-$var(DQSG_SSI)-$var(DQSG_DQSCK)-$var(DQSG_MPR)-$var(DQSG_JITTER)-$var(DQSG_DCD)-$var(DQSG_EOL)-$var(DQSG_CAL_UNC)-$var(DQSG_TRK_UNC)-$var(DQSG_SH)-$var(DQSG_SK_EFFECT)]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter DQSG_MARGIN]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(DQSG_MARGIN)]]

   
   #######################################
   #######################################
   # Create the DQS Gating analysis panel   
   set setup_slack [expr $var(DQSG_MARGIN)/2]
   set hold_slack  [expr $var(DQSG_MARGIN)/2]

   set panel_name "$inst $analysis_name"
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
   
   if {($setup_slack < 0) || ($hold_slack <0)} {
      set panel_id [create_report_panel -table $panel_name -color red]
   } else {
      set panel_id [create_report_panel -table $panel_name]
   }
   add_row_to_table -id $panel_id [list "Operation" "Margin"]
   
   foreach summary_line $summary {
      add_row_to_table -id $panel_id $summary_line -fcolors $positive_fcolour
   }
   
   lappend global_summary [list $opcname 0 "$analysis_name ($opcname)" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $setup_slack] [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $hold_slack]]
}

#############################################################
# Write Levelling Analysis
#############################################################
proc ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_perform_write_levelling_analysis  {opcname inst pin_array_name var_array_name summary_name} {

   #######################################
   # Need access to global variables
   upvar 1 $summary_name global_summary
   upvar 1 $var_array_name var
   upvar 1 $pin_array_name pins

   # Debug switch. Change to 1 to get more run-time debug information
   set debug 0   
   set result 1

   # Do not perform for QDRII
   if {([string compare $var(PROTOCOL) "QDRII"] == 0)} {
      return
   }
   
   # Only perform for DDR protocols 
   if { ([string compare $var(PROTOCOL) "DDR4"] == 0) || ([string compare $var(PROTOCOL) "DDR3"] == 0) || ([string compare $var(PROTOCOL) "LPDDR3"] == 0)} {
      set analysis_name "Write Levelling"
   } else {
      set analysis_name "DK vs CK"
   }   

   ###############################
   # Write Levelling summary report
   ###############################

   set positive_fcolour [list "black" "blue"]
   set negative_fcolour [list "black" "red" ]

   set summary [list]
   
   set var(WL_UI) [expr $var(UI)]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter WL_UI]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(WL_UI)]]

   set var(WL_ISI) $var(WL_ISI)
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter WL_ISI]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(WL_ISI)]]

   set var(WL_SSO) $var(WL_SSO)
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter WL_SSO]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(WL_SSO)]]

   set var(WL_MEM) [expr $var(tCK)*(1-2*[min [expr $var(tDQSS)] [expr (1-$var(tDSS)-$var(tDSH))/2]])]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter WL_MEM]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(WL_MEM)]]

   set var(WL_MPR) [expr ($var(WITH_WL_CALIB) == 1 ? ($var(WITH_MPR) == 1 ? -$var(WL_MEM)*$var(MPR_DQSS) : 0) : 0)]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter WL_MPR]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(WL_MPR)]]

   set var(WL_SH) [expr ($var(WITH_WL_M_CALIB) == 1 ? (1-$var(MPR_WLS))*$var(tWLS) + (1-$var(MPR_WLH))*$var(tWLH) : 0)]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter WL_SH]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(WL_SH)]]

   set var(WL_JITTER) $var(WL_JITTER)
   set var(WL_JITTER_sens) [expr ([expr (($var(UI))*1000.0-$var(EXTRACTED_PERIOD))*$var(WL_JITTER_SENS_TO_PERIOD)])/1000.0]
   if {$var(WL_JITTER_sens) > 0} {
      set var(WL_JITTER) [expr $var(WL_JITTER) + $var(WL_JITTER_sens)]
   }
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter WL_JITTER]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(WL_JITTER)]]

   set var(WL_DCD) $var(WL_DCD)
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter WL_DCD]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(WL_DCD)]]

   set var(WL_EOL) $var(WL_EOL)
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter WL_EOL]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(WL_EOL)]]

   set var(WL_CAL_UNC) [expr ($var(WITH_WL_CALIB) == 1 ? $var(WL_CALIBRATION_LOSS_OTHER)+($var(OCT_RECAL) == 1 ? $var(WL_TEMP_CAL_LOSS_OCT_RECAL) : $var(WL_TEMP_CAL_LOSS_WO_OCT_RECAL)) : 0)]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter WL_CAL_UNC]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(WL_CAL_UNC)]]

   set var(WL_PVT) [expr ($var(WITH_WL_CALIB) == 1 ? 0 : $var(WL_CALIBRATION_LOSS_OTHER)+($var(OCT_RECAL) == 1 ? $var(WL_TEMP_CAL_LOSS_OCT_RECAL_WO_WL_CAL) : $var(WL_TEMP_CAL_LOSS_WO_OCT_RECAL_WO_WL_CAL)))]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter WL_PVT]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(WL_PVT)]]

   set var(WL_SK_EFFECT) [expr ($var(WITH_WL_CALIB) == 1 ? ($var(BD_PKG_SKEW)-$var(DEFAULT_BD_PKG_SKEW))*$var(BD_SK_SENS_WL) : ($var(BD_PKG_SKEW)-$var(DEFAULT_BD_PKG_SKEW))*$var(BD_SK_SENS_WL_WO_CALIB)+$var(DQS_TO_CK_BOARD_SKEW)+$var(DQS_BOARD_SKEW))]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter WL_SK_EFFECT]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(WL_SK_EFFECT)]]

   set var(WL_MARGIN) [expr $var(WL_UI)-$var(WL_ISI)-$var(WL_SSO)-$var(WL_MEM)-$var(WL_MPR)-$var(WL_SH)-$var(WL_JITTER)-$var(WL_DCD)-$var(WL_EOL)-$var(WL_CAL_UNC)-$var(WL_PVT)-$var(WL_SK_EFFECT)]
   lappend summary [list "   [emiftcl_get_parameter_user_string -parameter WL_MARGIN]" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $var(WL_MARGIN)]]


   #######################################
   #######################################
   # Create the Write Levelling analysis panel   
   set setup_slack [expr $var(WL_MARGIN)/2]
   set hold_slack  [expr $var(WL_MARGIN)/2]

   set panel_name "$inst $analysis_name"
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
   
   if {($setup_slack < 0) || ($hold_slack <0)} {
      set panel_id [create_report_panel -table $panel_name -color red]
   } else {
      set panel_id [create_report_panel -table $panel_name]
   }
   add_row_to_table -id $panel_id [list "Operation" "Margin"]      
   
   foreach summary_line $summary {
      add_row_to_table -id $panel_id $summary_line -fcolors $positive_fcolour
   }
   
   lappend global_summary [list $opcname 0 "$analysis_name ($opcname)" [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $setup_slack] [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_format_3dp $hold_slack]]
}

################################################################
# Helper function to add a report_timing-based analysis section
################################################################
proc ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_add_report_timing_analysis {opcname inst var_array_name summary_name title from_clks to_clks from_nodes to_nodes } {

   #######################################
   # Need access to global variables
   upvar 1 $summary_name global_summary
   upvar 1 $var_array_name var
   
   set num_failing_path 10
   
   set setup_margin    999.9
   set hold_margin     999.9
   set recovery_margin 999.9
   set removal_margin  999.9
      
   if {[get_collection_size [get_timing_paths -from_clock $from_clks -to_clock $to_clks -from $from_nodes -to $to_nodes -npaths 1 -setup]] > 0} {
      set res_0        [report_timing -detail full_path -from_clock $from_clks -to_clock $to_clks -from $from_nodes -to $to_nodes -npaths $num_failing_path -panel_name "$inst $title (setup)" -setup]
      set res_1        [report_timing -detail full_path -from_clock $from_clks -to_clock $to_clks -from $from_nodes -to $to_nodes -npaths $num_failing_path -panel_name "$inst $title (hold)" -hold]
      set setup_margin [lindex $res_0 1]
      set hold_margin  [lindex $res_1 1]
      
      if {$var(DIAG_TIMING_REGTEST_MODE)} {
         lappend global_summary [list $opcname 0 "$title ($opcname)" $setup_margin $hold_margin]
      }
   }
   
   if {[get_collection_size [get_timing_paths -from_clock $from_clks -to_clock $to_clks -from $from_nodes -to $to_nodes -npaths 1 -recovery]] > 0} {
      set res_0           [report_timing -detail full_path -from_clock $from_clks -to_clock $to_clks -from $from_nodes -to $to_nodes -npaths $num_failing_path -panel_name "$inst $title (recovery)" -recovery]
      set res_1           [report_timing -detail full_path -from_clock $from_clks -to_clock $to_clks -from $from_nodes -to $to_nodes -npaths $num_failing_path -panel_name "$inst $title (removal)" -removal]
      set recovery_margin [lindex $res_0 1]
      set removal_margin  [lindex $res_1 1]
      
      if {$var(DIAG_TIMING_REGTEST_MODE)} {
         lappend global_summary [list $opcname 0 "$title Recovery/Removal ($opcname)" $recovery_margin $removal_margin]
      }
   }
   
   return [list $setup_margin $hold_margin $recovery_margin $removal_margin]
}

#############################################################
# Other Core-Logic related Timing Analysis
#############################################################

proc ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_add_c2p_p2c_report_timing_analysis {opcname inst pin_array_name var_array_name summary_name title from_clks to_clks from_nodes to_nodes p2c} {

   #######################################
   # Need access to global variables
   upvar 1 $summary_name global_summary
   upvar 1 $var_array_name var
   upvar 1 $pin_array_name pins

   set num_failing_path 10

   set setup_margin    999.9
   set hold_margin     999.9
   set recovery_margin 999.9
   set removal_margin  999.9
   set debug 0

   set positive_fcolour [list "black" "blue" "blue"]
   set negative_fcolour [list "black" "red"  "red"]
   set summary [list]

   # Get the periphery clocks
   if {$p2c} {
      set phyclks $from_clks
   } else {
      set phyclks $to_clks
   }

   # Set panel names
   set panel_name_setup  "$inst $title (setup)"
   set panel_name_hold   "$inst $title (hold)"
   set panel_name_recovery  "$inst $title (recovery)"
   set panel_name_removal   "$inst $title (removal)"	  
   set disable_panel_color_flag ""
   set quiet_flag ""

   # Generate the default margins
   set res_0        [report_timing -detail full_path -from_clock $from_clks -to_clock $to_clks -from $from_nodes -to $to_nodes -npaths $num_failing_path -panel_name $panel_name_setup -setup $disable_panel_color_flag $quiet_flag]
   set res_1        [report_timing -detail full_path -from_clock $from_clks -to_clock $to_clks -from $from_nodes -to $to_nodes -npaths $num_failing_path -panel_name $panel_name_hold -hold $disable_panel_color_flag $quiet_flag]
   set recovery_removal_paths 0
   if {[get_collection_size [get_timing_paths -from_clock $from_clks -to_clock $to_clks -from $from_nodes -to $to_nodes -npaths 1 -recovery]] > 0} {
      set recovery_removal_paths 1
      set res_2        [report_timing -detail full_path -from_clock $from_clks -to_clock $to_clks -from $from_nodes -to $to_nodes -npaths $num_failing_path -panel_name $panel_name_recovery -recovery $disable_panel_color_flag $quiet_flag]
      set res_3        [report_timing -detail full_path -from_clock $from_clks -to_clock $to_clks -from $from_nodes -to $to_nodes -npaths $num_failing_path -panel_name $panel_name_removal  -removal  $disable_panel_color_flag $quiet_flag]
   }
   set setup_margin [lindex $res_0 1]
   set hold_margin  [lindex $res_1 1]
   if {$recovery_removal_paths == 1} {
     set recovery_margin [lindex $res_2 1]
	 set removal_margin  [lindex $res_3 1]
   }

   if {$var(DIAG_TIMING_REGTEST_MODE)} {
      lappend global_summary [list $opcname 0 "$title ($opcname)" $setup_margin $hold_margin]
      if {$recovery_removal_paths == 1} {
         lappend global_summary [list $opcname 0 "$title Recovery/Removal ($opcname)" $recovery_margin $hold_margin]
      }
   }

   return [list $setup_margin $hold_margin $recovery_margin $removal_margin]
}


proc ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_perform_core_analysis {opcname inst pin_array_name var_array_name summary_name} {

   #######################################
   # Need access to global variables
   upvar 1 $summary_name global_summary
   upvar 1 $var_array_name var
   upvar 1 $pin_array_name pins
   global ::io_only_analysis

   # Debug switch. Change to 1 to get more run-time debug information
   set debug 0   
   set result 1

   ###############################
   # PHY analysis
   ###############################
   
   set analysis_name "Core"

   if {$::io_only_analysis == 1} {
      set setup_slack "--"
      set hold_slack  "--"
      lappend global_summary [list $opcname 0 "$analysis_name ($opcname)" $setup_slack $hold_slack]
      post_message -type warning "Early EMIF IO timing estimate does not include core FPGA timing"
   } elseif {$var(IS_HPS)} {
      # No core timing analysis required by HPS interface
      set setup_slack "--"
      set hold_slack  "--"
      lappend global_summary [list $opcname 0 "$analysis_name ($opcname)" $setup_slack $hold_slack]
      lappend global_summary [list $opcname 0 "$analysis_name Recovery/Removal ($opcname)" $setup_slack $hold_slack]
   } else {
   
      set master_instname $pins(master_instname)
      set coreclkname [list ${master_instname}_core_usr_* ${master_instname}_core_afi_* ${master_instname}_core_dft_* ${master_instname}_ref_clock ${master_instname}_core_nios_clk ${inst}_oct_clock ${inst}_oct_gated_clock [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_get_clock_name_from_pin_name $pins(pll_ref_clock)]]
      set coreclks [get_clocks -nowarn $coreclkname]
      
      set phyclkname [list ${inst}_phy_*]
      set phyclks [get_clocks -nowarn $phyclkname]
   
      set emif_regs [get_registers $inst|*] 
      set rest_regs [remove_from_collection [all_registers] $emif_regs]
      
      set setup_margin    999.9
      set hold_margin     999.9
      set recovery_margin 999.9
      set removal_margin  999.9

      # Core/periphery transfers

      # Core-to-periphery
      set res [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_add_c2p_p2c_report_timing_analysis $opcname $inst $pin_array_name var global_summary "Core To Periphery" $coreclks $phyclks "*" $emif_regs 0]
      set setup_margin    [min $setup_margin    [lindex $res 0]]
      set hold_margin     [min $hold_margin     [lindex $res 1]]
      set recovery_margin [min $recovery_margin [lindex $res 2]]
      set removal_margin  [min $removal_margin  [lindex $res 3]]
      
      # Periphery-to-core
      set res [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_add_c2p_p2c_report_timing_analysis $opcname $inst $pin_array_name var global_summary "Periphery To Core" $phyclks $coreclks $emif_regs "*" 1]
      set setup_margin    [min $setup_margin    [lindex $res 0]]
      set hold_margin     [min $hold_margin     [lindex $res 1]]
      set recovery_margin [min $recovery_margin [lindex $res 2]]
      set removal_margin  [min $removal_margin  [lindex $res 3]]

      # Pure Core transfers

      set_active_clocks [remove_from_collection [all_clocks] $phyclks]

      # EMIF logic within FPGA core
      set res [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_add_report_timing_analysis $opcname $inst var global_summary "Within Core" $coreclks $coreclks $emif_regs $emif_regs]
      set setup_margin    [min $setup_margin    [lindex $res 0]]
      set hold_margin     [min $hold_margin     [lindex $res 1]]
      set recovery_margin [min $recovery_margin [lindex $res 2]]
      set removal_margin  [min $removal_margin  [lindex $res 3]]
      
      # Transfers between EMIF and user logic
      set res [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_add_report_timing_analysis $opcname $inst var global_summary "IP to User Logic" "*" "*" $emif_regs $rest_regs]
      set setup_margin    [min $setup_margin    [lindex $res 0]]
      set hold_margin     [min $hold_margin     [lindex $res 1]]
      set recovery_margin [min $recovery_margin [lindex $res 2]]
      set removal_margin  [min $removal_margin  [lindex $res 3]]
      
      # Transfers between user and EMIF logic
      set res [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_add_report_timing_analysis $opcname $inst var global_summary "User Logic to IP" "*" "*" $rest_regs $emif_regs]
      set setup_margin    [min $setup_margin    [lindex $res 0]]
      set hold_margin     [min $hold_margin     [lindex $res 1]]
      set recovery_margin [min $recovery_margin [lindex $res 2]]
      set removal_margin  [min $removal_margin  [lindex $res 3]]
      
      # Transfers within non-EMIF logic (not reported by default since they are irrelevant to EMIF IP)
      if {$var(DIAG_TIMING_REGTEST_MODE)} {
         set res [ghrd_10as066n2_emif_hps_altera_emif_arch_nf_171_qhcayby_add_report_timing_analysis $opcname $inst var global_summary "Within User Logic" $coreclks $coreclks $rest_regs $rest_regs]
         set setup_margin    [min $setup_margin    [lindex $res 0]]
         set hold_margin     [min $hold_margin     [lindex $res 1]]
         set recovery_margin [min $recovery_margin [lindex $res 2]]
         set removal_margin  [min $removal_margin  [lindex $res 3]]
      }

      set_active_clocks [all_clocks]

      lappend global_summary [list $opcname 0 "$analysis_name ($opcname)" $setup_margin $hold_margin]
      lappend global_summary [list $opcname 0 "$analysis_name Recovery/Removal ($opcname)" $recovery_margin $removal_margin]
   }
}


