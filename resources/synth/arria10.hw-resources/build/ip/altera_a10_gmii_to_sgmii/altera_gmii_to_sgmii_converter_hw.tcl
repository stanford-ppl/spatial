# +-----------------------------------
# | 
# | altera_gmii_to_sgmii_converter 
# | "Altera GMII to SGMII Converter"
# | 
# +-----------------------------------

# +-----------------------------------
# | request TCL package
# |
package require -exact qsys 14.0
# |
# +-----------------------------------

# +-----------------------------------
# | module altera_gmii_to_sgmii_converter
# |
set_module_property NAME altera_gmii_to_sgmii_converter
set_module_property DISPLAY_NAME "Altera GMII To SGMII Converter"
set_module_property VERSION 2.0
set_module_property INTERNAL false
set_module_property OPAQUE_ADDRESS_MAP true
set_module_property DISPLAY_NAME "Altera GMII to SGMII Converter"
set_module_property GROUP "Example Designs/Component"
set_module_property AUTHOR "Altera Corporation"
set_module_property INSTANTIATE_IN_SYSTEM_MODULE true
set_module_property EDITABLE true
set_module_property REPORT_TO_TALKBACK false
set_module_property VALIDATION_CALLBACK do_validate
set_module_property COMPOSITION_CALLBACK do_compose
# |
# +-----------------------------------

# +-----------------------------------
# | Parameters
# |
add_parameter                  DETECTED_FAMILY               STRING              ""
set_parameter_property         DETECTED_FAMILY               SYSTEM_INFO         {DEVICE_FAMILY}
set_parameter_property         DETECTED_FAMILY               DISPLAY_NAME        "System Device Family"
set_parameter_property         DETECTED_FAMILY               DESCRIPTION         "Family name of the currently selected device."
set_parameter_property         DETECTED_FAMILY               HDL_PARAMETER       false
set_parameter_property         DETECTED_FAMILY               VISIBLE             true
# |
# +-----------------------------------

# +-----------------------------------
# | Device Tree Assignments
# |
set_module_assignment embeddedsw.dts.vendor "altr"
set_module_assignment embeddedsw.dts.name "gmii-to-sgmii"
set_module_assignment embeddedsw.dts.group "phy"
# |
# +-----------------------------------

# +-----------------------------------
# | Validate Callback Procedure
# | 
proc do_validate {} {

  if { [get_parameter_value DETECTED_FAMILY] == "Arria 10" } {
    send_message info "EMAC link status is automatically connected for GMII to SGMII converter component"
  } elseif { [get_parameter_value DETECTED_FAMILY] == "Cyclone V" || [get_parameter_value DETECTED_FAMILY] == "Arria V" } {
    send_message info "Link status of EMAC is required by GMII to SGMII converter component via CSR write"
  } else {
    send_message warning "GMII to SGMII converter component is designed for Arria 10 SoC, Arria V SoC and Cyclone V SoC EMAC extention to FPGA interface. This component may not functional in other architecture"
  }
  
}
# |
# +-----------------------------------

# +-----------------------------------
# | Compose Callback Procedure
# | 
proc do_compose { } {

  set DETECTED_FAMILY [get_parameter_value DETECTED_FAMILY]
  
# Interface declaration
  add_interface clock_in clock input
  add_interface reset_in reset input
 
  add_interface emac_gtx_clk clock input
  add_interface emac_tx_clk_in clock output
  add_interface emac_rx_clk_in clock output
  add_interface emac_tx_reset reset input
  add_interface emac_rx_reset reset intput
 
  add_interface hps_emac_mdio conduit output
  add_interface hps_emac_ptp conduit output
  add_interface emac conduit output

  add_interface tse_pcs_ref_clk_clock_connection clock input
  
  add_interface tse_sgmii_status_connection conduit output
  add_interface tse_status_led_connection conduit output
  add_interface tse_serdes_control_connection conduit output
  add_interface tse_serial_connection conduit output
  
  if {$DETECTED_FAMILY == "Cyclone V" || $DETECTED_FAMILY == "Arria V"} {
    add_interface hps_emac_interface_splitter_avalon_slave avalon slave
    #set_interface_property hps_emac_interface_splitter_avalon_slave associatedClock clock_in
    #set_interface_property hps_emac_interface_splitter_avalon_slave associatedReset reset_in
  }

  add_interface gmii_to_sgmii_adapter_avalon_slave avalon slave
  #set_interface_property gmii_to_sgmii_adapter_avalon_slave associatedClock clock_in
  #set_interface_property gmii_to_sgmii_adapter_avalon_slave associatedReset reset_in

  add_interface eth_tse_control_port avalon slave
  #set_interface_property eth_tse_control_port associatedClock clock_in
  #set_interface_property eth_tse_control_port associatedReset reset_in



  add_instance clk_bridge altera_clock_bridge
	
  add_instance rst_bridge altera_reset_bridge
 
  add_instance hps_emac_interface_splitter_0 altera_hps_emac_interface_splitter 

  add_instance gmii_to_sgmii_adapter_0 altera_gmii_to_sgmii_adapter 
 
  add_instance eth_tse_0 altera_eth_tse 
  set_instance_parameter_value eth_tse_0 {core_variation} {PCS_ONLY}
  set_instance_parameter_value eth_tse_0 {ifGMII} {MII_GMII}
  set_instance_parameter_value eth_tse_0 {enable_use_internal_fifo} {1}
  set_instance_parameter_value eth_tse_0 {enable_ecc} {0}
  set_instance_parameter_value eth_tse_0 {max_channels} {1}
  set_instance_parameter_value eth_tse_0 {use_misc_ports} {1}
  set_instance_parameter_value eth_tse_0 {transceiver_type} {GXB}
  set_instance_parameter_value eth_tse_0 {enable_hd_logic} {1}
  set_instance_parameter_value eth_tse_0 {enable_gmii_loopback} {0}
  set_instance_parameter_value eth_tse_0 {enable_sup_addr} {0}
  set_instance_parameter_value eth_tse_0 {stat_cnt_ena} {1}
  set_instance_parameter_value eth_tse_0 {ext_stat_cnt_ena} {0}
  set_instance_parameter_value eth_tse_0 {ena_hash} {0}
  set_instance_parameter_value eth_tse_0 {enable_shift16} {1}
  set_instance_parameter_value eth_tse_0 {enable_mac_flow_ctrl} {0}
  set_instance_parameter_value eth_tse_0 {enable_mac_vlan} {0}
  set_instance_parameter_value eth_tse_0 {enable_magic_detect} {1}
  set_instance_parameter_value eth_tse_0 {useMDIO} {0}
  set_instance_parameter_value eth_tse_0 {mdio_clk_div} {40}
  set_instance_parameter_value eth_tse_0 {enable_ena} {32}
  set_instance_parameter_value eth_tse_0 {eg_addr} {11}
  set_instance_parameter_value eth_tse_0 {ing_addr} {11}
  set_instance_parameter_value eth_tse_0 {phy_identifier} {305419896}
  set_instance_parameter_value eth_tse_0 {enable_sgmii} {1}
  set_instance_parameter_value eth_tse_0 {export_pwrdn} {0}
  set_instance_parameter_value eth_tse_0 {enable_alt_reconfig} {0}
  set_instance_parameter_value eth_tse_0 {starting_channel_number} {0}
  set_instance_parameter_value eth_tse_0 {phyip_pll_type} {CMU}
  set_instance_parameter_value eth_tse_0 {phyip_pll_base_data_rate} {1250 Mbps}
  set_instance_parameter_value eth_tse_0 {phyip_en_synce_support} {0}
  set_instance_parameter_value eth_tse_0 {phyip_pma_bonding_mode} {x1}
  set_instance_parameter_value eth_tse_0 {nf_phyip_rcfg_enable} {0}
  set_instance_parameter_value eth_tse_0 {enable_timestamping} {0}
  set_instance_parameter_value eth_tse_0 {enable_ptp_1step} {0}
  set_instance_parameter_value eth_tse_0 {tstamp_fp_width} {4}
	
  # connections 

  if {$DETECTED_FAMILY == "Cyclone V" || $DETECTED_FAMILY == "Arria V"} {
    add_connection clk_bridge.out_clk hps_emac_interface_splitter_0.peri_clock
    add_connection rst_bridge.out_reset hps_emac_interface_splitter_0.peri_reset
  }
	
  add_connection clk_bridge.out_clk rst_bridge.clk

  add_connection clk_bridge.out_clk gmii_to_sgmii_adapter_0.peri_clock

  add_connection clk_bridge.out_clk eth_tse_0.control_port_clock_connection
  
  add_connection rst_bridge.out_reset gmii_to_sgmii_adapter_0.peri_reset
  add_connection rst_bridge.out_reset eth_tse_0.reset_connection
	
  add_connection hps_emac_interface_splitter_0.hps_gmii gmii_to_sgmii_adapter_0.hps_gmii
	
  add_connection eth_tse_0.pcs_transmit_clock_connection gmii_to_sgmii_adapter_0.pcs_transmit_clock
  add_connection eth_tse_0.pcs_receive_clock_connection gmii_to_sgmii_adapter_0.pcs_receive_clock

  add_connection gmii_to_sgmii_adapter_0.pcs_clock_enable eth_tse_0.clock_enable_connection
  add_connection gmii_to_sgmii_adapter_0.pcs_gmii eth_tse_0.gmii_connection
  add_connection gmii_to_sgmii_adapter_0.pcs_mii eth_tse_0.mii_connection
  add_connection gmii_to_sgmii_adapter_0.pcs_transmit_reset eth_tse_0.pcs_transmit_reset_connection
  add_connection gmii_to_sgmii_adapter_0.pcs_receive_reset eth_tse_0.pcs_receive_reset_connection
	
  # interfaces export
	
  if {$DETECTED_FAMILY == "Cyclone V" || $DETECTED_FAMILY == "Arria V"} {
	set_interface_property hps_emac_interface_splitter_avalon_slave export_of hps_emac_interface_splitter_0.avalon_slave
  }

  if {$DETECTED_FAMILY == "Arria 10"} {
    add_interface tse_tx_serial_clk hssi_serial_clock sink
    set_interface_property tse_tx_serial_clk EXPORT_OF eth_tse_0.tx_serial_clk
    add_interface tse_rx_cdr_refclk clock sink
    set_interface_property tse_rx_cdr_refclk EXPORT_OF eth_tse_0.rx_cdr_refclk

    add_interface tse_tx_analogreset conduit end
    set_interface_property tse_tx_analogreset EXPORT_OF eth_tse_0.tx_analogreset
    add_interface tse_tx_digitalreset conduit end
    set_interface_property tse_tx_digitalreset EXPORT_OF eth_tse_0.tx_digitalreset
    add_interface tse_tx_cal_busy conduit end
    set_interface_property tse_tx_cal_busy EXPORT_OF eth_tse_0.tx_cal_busy

    add_interface tse_rx_analogreset conduit end
    set_interface_property tse_rx_analogreset EXPORT_OF eth_tse_0.rx_analogreset
    add_interface tse_rx_digitalreset conduit end
    set_interface_property tse_rx_digitalreset EXPORT_OF eth_tse_0.rx_digitalreset
    add_interface tse_rx_cal_busy conduit end
    set_interface_property tse_rx_cal_busy EXPORT_OF eth_tse_0.rx_cal_busy
    add_interface tse_rx_is_lockedtodata conduit end
    set_interface_property tse_rx_is_lockedtodata EXPORT_OF eth_tse_0.rx_is_lockedtodata

    ### terminating unneccesary ports at top level
    add_interface tse_rx_set_locktodata conduit end
    set_interface_property tse_rx_set_locktodata EXPORT_OF eth_tse_0.rx_set_locktodata
    #set_interface_property tse_rx_set_locktodata ENABLED false
    add_interface tse_rx_set_locktoref conduit end
    set_interface_property tse_rx_set_locktoref EXPORT_OF eth_tse_0.rx_set_locktoref
    #set_interface_property tse_rx_set_locktoref ENABLED false
    add_interface tse_rx_is_lockedtoref conduit end
    set_interface_property tse_rx_is_lockedtoref EXPORT_OF eth_tse_0.rx_is_lockedtoref
    #set_interface_property tse_rx_is_lockedtoref ENABLED false
  }	

  set_interface_property clock_in export_of clk_bridge.in_clk
  set_interface_property reset_in export_of rst_bridge.in_reset
  set_interface_property tse_pcs_ref_clk_clock_connection export_of eth_tse_0.pcs_ref_clk_clock_connection
	
  set_interface_property gmii_to_sgmii_adapter_avalon_slave export_of gmii_to_sgmii_adapter_0.avalon_slave
  set_interface_property eth_tse_control_port export_of eth_tse_0.control_port

  set_interface_property hps_emac_mdio export_of hps_emac_interface_splitter_0.mdio
  set_interface_property hps_emac_ptp export_of hps_emac_interface_splitter_0.ptp
  set_interface_property emac export_of hps_emac_interface_splitter_0.emac

  set_interface_property tse_sgmii_status_connection export_of eth_tse_0.sgmii_status_connection
  set_interface_property tse_status_led_connection export_of eth_tse_0.status_led_connection
  set_interface_property tse_serdes_control_connection export_of eth_tse_0.serdes_control_connection
  set_interface_property tse_serial_connection export_of eth_tse_0.serial_connection

  set_interface_property emac_gtx_clk export_of hps_emac_interface_splitter_0.emac_gtx_clk
  set_interface_property emac_tx_clk_in export_of hps_emac_interface_splitter_0.emac_tx_clk_in
  set_interface_property emac_rx_clk_in export_of hps_emac_interface_splitter_0.emac_rx_clk_in
  set_interface_property emac_tx_reset export_of hps_emac_interface_splitter_0.emac_tx_reset
  set_interface_property emac_rx_reset export_of hps_emac_interface_splitter_0.emac_rx_reset
	
}
# |
# +-----------------------------------
