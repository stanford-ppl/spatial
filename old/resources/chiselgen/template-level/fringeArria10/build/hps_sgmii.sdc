# Clock Group
create_clock -name PCS_REFLCLK -period 8.000 [get_ports {pcs_clk_125}]
derive_pll_clocks -create_base_clocks
derive_clock_uncertainty

set_clock_groups -asynchronous -group [get_clocks {MAIN_CLOCK}] -group [get_clocks {PCS_REFLCLK}] -group [get_clocks {hps_emac2_gtx_clk}] -group [get_clocks {hps_emac1_gtx_clk}] -group [get_clocks {soc_inst|sgmii_*|tx_clkout}]
create_clock -name emac1_fpga_mdc -period 400.000 [get_keepers {*~emac1_gmii_mdc_o_1.reg}]
create_clock -name emac2_fpga_mdc -period 400.000 [get_keepers {*~emac2_gmii_mdc_o_1.reg}]
set_output_delay -clock { emac1_fpga_mdc } 30 [get_ports {emac1_fpga_mdio}]
set_input_delay  -clock { emac1_fpga_mdc } 30 [get_ports {emac1_fpga_mdio}]
set_output_delay -clock { emac2_fpga_mdc } 30 [get_ports {emac2_fpga_mdio}]
set_input_delay  -clock { emac2_fpga_mdc } 30 [get_ports {emac2_fpga_mdio}]
set_false_path -from * -to [ get_ports sgmii1_phy_reset_n ]
set_false_path -from * -to [ get_ports sgmii2_phy_reset_n ]
set_false_path -from * -to [ get_ports emac1_fpga_mdc ]
set_false_path -from * -to [ get_ports emac2_fpga_mdc ]
set_false_path -from [get_ports {sgmii1_phy_irq_n}] -to *
set_false_path -from [get_ports {sgmii2_phy_irq_n}] -to *
