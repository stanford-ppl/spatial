# 100MHz board input clock, 133.3333MHz for EMIF refclk
create_clock -name EMIF_REFCLOCK -period 7.5 [get_ports a10_emif_pll_ref_clk_clock_clk]

derive_pll_clocks -create_base_clocks

# UART
set_false_path -from * -to [get_ports {uart_tx}]

##ASMI2
create_generated_clock -name {DCLK} -source [get_nets {soc_inst|qspi_pll|qspi_pll|altera_iopll_i|twentynm_pll|outclk[0]}] -master_clock {soc_inst|qspi_pll|qspi_pll|outclk0}  
# ncs
set tSLCH 4                       
set tCHSL 4  
# asdata
set tCLQV 6
set tCLQX 1  

set tDVCH 2     
set tCHDX 3        

set tBRD_CK_max 1.0  
set tBRD_CK_min 0.5
set tBRD_DT_max 1.0
set tBRD_DT_min 0.5

set_output_delay -clock DCLK -max [expr $tSLCH + $tBRD_DT_max - $tBRD_CK_min] [get_ports NCS*]
set_output_delay -clock DCLK -min [expr -$tCHSL + $tBRD_DT_min - $tBRD_CK_max] [get_ports NCS*]

set_input_delay -clock DCLK -clock_fall -max [expr $tCLQV + $tBRD_CK_max + $tBRD_DT_max] [get_ports AS_DATA*]
set_input_delay -clock DCLK -clock_fall -min [expr $tCLQX + $tBRD_CK_min + $tBRD_DT_min] [get_ports AS_DATA*]

set_output_delay -clock DCLK -max [expr $tDVCH + $tBRD_DT_max - $tBRD_CK_min] [get_ports AS_DATA*]
set_output_delay -clock DCLK -min [expr -$tCHDX + $tBRD_DT_min - $tBRD_CK_max] [get_ports AS_DATA*]
