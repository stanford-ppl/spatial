# Clock Group
# create_clock -name PCS_REFLCLK -period 8.000 [get_ports {pcs_clk_125}]
derive_pll_clocks -create_base_clocks
derive_clock_uncertainty
# set_clock_groups -asynchronous -group [get_clocks {MAIN_CLOCK}] -group [get_clocks {PCS_REFLCLK}] -group [get_clocks {mac1_fpga_mdc}] -group [get_clocks {mac0_fpga_mdc}]
set_max_skew -to [get_ports "mac0_fpga_mdc"] 2
set_max_skew -to [get_ports "mac1_fpga_mdc"] 2
set_max_skew -to [get_ports "mac0_fpga_mdio"] 2
set_max_skew -to [get_ports "mac1_fpga_mdio"] 2
set_false_path -from * -to [ get_ports sgmii0_phy_reset_n ]
set_false_path -from * -to [ get_ports sgmii1_phy_reset_n ]
set_false_path -from [get_ports {sgmii0_phy_irq_n}] -to *
set_false_path -from [get_ports {sgmii1_phy_irq_n}] -to *
