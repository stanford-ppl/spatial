if { $argc != 1 } {
  puts $argc
  puts [llength $argv]
  foreach i $argv {puts $i}
  puts "The second arg is [lindex $argv 1]"; #indexes start at 0
  puts "Usage: settings.tcl <clockFreqMHz>"
  exit -1
}

set CLOCK_FREQ_MHZ [lindex $argv 0]
set CLOCK_FREQ_HZ  [expr $CLOCK_FREQ_MHZ * 1000000]

source settings.tcl

# Create project to make processing system IP from bd
create_project bd_project ./bd_project -part $PART
set_property board_part $BOARD [current_project]

add_files -norecurse [glob *.v]
add_files -norecurse [glob *.sv]
update_compile_order -fileset sources_1
update_compile_order -fileset sim_1

## Create processing system using bd
create_bd_design "design_1"
update_compile_order -fileset sources_1
switch $TARGET {
  default {
    create_bd_cell -type ip -vlnv xilinx.com:ip:ddr4:2.2 ddr4_0

    
    create_bd_cell -type ip -vlnv xilinx.com:ip:processing_system7:5.5 processing_system7_0
    apply_bd_automation -rule xilinx.com:bd_rule:processing_system7 -config {make_external "FIXED_IO, DDR" apply_board_preset "1" Master "Disable" Slave "Disable" }  [get_bd_cells processing_system7_0]
    create_bd_cell -type module -reference SpatialIP SpatialIP_0
    set_property -dict [list CONFIG.PCW_FPGA0_PERIPHERAL_FREQMHZ $CLOCK_FREQ_MHZ] [get_bd_cells processing_system7_0]
    set_property -dict [list CONFIG.PCW_FPGA1_PERIPHERAL_FREQMHZ {250} CONFIG.PCW_EN_CLK1_PORT {1}] [get_bd_cells processing_system7_0]
    apply_bd_automation -rule xilinx.com:bd_rule:axi4 -config {Master "/processing_system7_0/M_AXI_GP0" Clk "/processing_system7_0/FCLK_CLK0 ($CLOCK_FREQ_MHZ MHz)" }  [get_bd_intf_pins SpatialIP_0/io_S_AXI]
    # Faster clock (200 MHz) for memory interface
    create_bd_cell -type ip -vlnv xilinx.com:ip:proc_sys_reset:5.0 proc_sys_reset_fclk1
    connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK1] [get_bd_pins proc_sys_reset_fclk1/slowest_sync_clk]
    connect_bd_net [get_bd_pins processing_system7_0/FCLK_RESET0_N] [get_bd_pins proc_sys_reset_fclk1/ext_reset_in]
    ### HP0 Begin {
      # Enable HP0, connect faster clock
      set_property -dict [list CONFIG.PCW_USE_S_AXI_HP0 {1}] [get_bd_cells processing_system7_0]
      ## CLOCK CROSSING HACK
      # connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK1] [get_bd_pins processing_system7_0/S_AXI_HP0_ACLK]
      connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK0] [get_bd_pins processing_system7_0/S_AXI_HP0_ACLK]
      # Create axi slice for timing
      create_bd_cell -type ip -vlnv xilinx.com:ip:axi_register_slice:2.1 axi_register_slice_0
      connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK0] [get_bd_pins axi_register_slice_0/aclk]
      connect_bd_net [get_bd_pins rst_ps7_0_${CLOCK_FREQ_MHZ}M/peripheral_aresetn] [get_bd_pins axi_register_slice_0/aresetn]
      set_property -dict [list CONFIG.REG_AW {9} CONFIG.REG_AR {9} CONFIG.REG_R {9} CONFIG.REG_W {9} CONFIG.REG_B {9}] [get_bd_cells axi_register_slice_0]
      # 512-to-64 data width converter
      create_bd_cell -type ip -vlnv xilinx.com:ip:axi_dwidth_converter:2.1 axi_dwidth_converter_0
      set_property -dict [list CONFIG.SI_ID_WIDTH.VALUE_SRC USER CONFIG.SI_DATA_WIDTH.VALUE_SRC USER CONFIG.MI_DATA_WIDTH.VALUE_SRC USER CONFIG.READ_WRITE_MODE.VALUE_SRC USER CONFIG.PROTOCOL.VALUE_SRC USER CONFIG.ADDR_WIDTH.VALUE_SRC USER] [get_bd_cells axi_dwidth_converter_0]
      set_property -dict [list CONFIG.SI_DATA_WIDTH {512} CONFIG.SI_ID_WIDTH {32} CONFIG.MI_DATA_WIDTH {64}] [get_bd_cells axi_dwidth_converter_0]
      connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK0] [get_bd_pins axi_dwidth_converter_0/s_axi_aclk]
      connect_bd_net [get_bd_pins rst_ps7_0_${CLOCK_FREQ_MHZ}M/peripheral_aresetn] [get_bd_pins axi_dwidth_converter_0/s_axi_aresetn]
      # AXI4 to AXI3 protocol converter
      create_bd_cell -type ip -vlnv xilinx.com:ip:axi_protocol_converter:2.1 axi_protocol_converter_0
      set_property -dict [list CONFIG.MI_PROTOCOL.VALUE_SRC USER CONFIG.ID_WIDTH.VALUE_SRC USER CONFIG.DATA_WIDTH.VALUE_SRC USER CONFIG.SI_PROTOCOL.VALUE_SRC USER] [get_bd_cells axi_protocol_converter_0]
      set_property -dict [list CONFIG.MI_PROTOCOL {AXI3} CONFIG.DATA_WIDTH {64} CONFIG.ID_WIDTH {6} CONFIG.TRANSLATION_MODE {2}] [get_bd_cells axi_protocol_converter_0]
      connect_bd_net [get_bd_pins rst_ps7_0_${CLOCK_FREQ_MHZ}M/peripheral_aresetn] [get_bd_pins axi_protocol_converter_0/aresetn]
      connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK0] [get_bd_pins axi_protocol_converter_0/aclk]
      # Clock converter from FCLKCLK0 <-> FCLKCLK1
      create_bd_cell -type ip -vlnv xilinx.com:ip:axi_clock_converter:2.1 axi_clock_converter_0
      set_property -dict [list CONFIG.ID_WIDTH.VALUE_SRC USER CONFIG.DATA_WIDTH.VALUE_SRC USER CONFIG.READ_WRITE_MODE.VALUE_SRC USER CONFIG.ADDR_WIDTH.VALUE_SRC USER CONFIG.PROTOCOL.VALUE_SRC USER] [get_bd_cells axi_clock_converter_0]
      set_property -dict [list CONFIG.PROTOCOL {AXI3} CONFIG.DATA_WIDTH {64} CONFIG.ID_WIDTH {6}] [get_bd_cells axi_clock_converter_0]
      connect_bd_net [get_bd_pins rst_ps7_0_${CLOCK_FREQ_MHZ}M/peripheral_aresetn] [get_bd_pins axi_clock_converter_0/s_axi_aresetn]
      connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK0] [get_bd_pins axi_clock_converter_0/s_axi_aclk]
      ## CLOCK CROSSING HACK
      # connect_bd_net [get_bd_pins proc_sys_reset_fclk1/peripheral_aresetn] [get_bd_pins axi_clock_converter_0/m_axi_aresetn]
      # connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK1] [get_bd_pins axi_clock_converter_0/m_axi_aclk]
      connect_bd_net [get_bd_pins proc_sys_reset_fclk1/peripheral_aresetn] [get_bd_pins axi_clock_converter_0/m_axi_aresetn]
      connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK0] [get_bd_pins axi_clock_converter_0/m_axi_aclk]

    ### } HP0 end

    # Loopback debuggers
    # top signals
    connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_ARVALID] [get_bd_pins SpatialIP_0/io_TOP_AXI_ARVALID]
    connect_bd_net [get_bd_pins axi_register_slice_0/s_axi_arvalid] [get_bd_pins SpatialIP_0/io_M_AXI_0_ARVALID]
    connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_ARREADY] [get_bd_pins SpatialIP_0/io_TOP_AXI_ARREADY]
    connect_bd_net [get_bd_pins axi_register_slice_0/s_axi_arready] [get_bd_pins SpatialIP_0/io_M_AXI_0_ARREADY]
    connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_ARADDR] [get_bd_pins SpatialIP_0/io_TOP_AXI_ARADDR]
    connect_bd_net [get_bd_pins axi_register_slice_0/s_axi_araddr] [get_bd_pins SpatialIP_0/io_M_AXI_0_ARADDR]
    connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_ARLEN] [get_bd_pins SpatialIP_0/io_TOP_AXI_ARLEN]
    connect_bd_net [get_bd_pins axi_register_slice_0/s_axi_arlen] [get_bd_pins SpatialIP_0/io_M_AXI_0_ARLEN]
    connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_ARSIZE] [get_bd_pins SpatialIP_0/io_TOP_AXI_ARSIZE]
    connect_bd_net [get_bd_pins axi_register_slice_0/s_axi_arsize] [get_bd_pins SpatialIP_0/io_M_AXI_0_ARSIZE]
    # connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_ARID] [get_bd_pins SpatialIP_0/io_TOP_AXI_ARID]
    # connect_bd_net [get_bd_pins axi_register_slice_0/s_axi_arid] [get_bd_pins SpatialIP_0/io_M_AXI_0_ARID]
    connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_ARBURST] [get_bd_pins SpatialIP_0/io_TOP_AXI_ARBURST]
    connect_bd_net [get_bd_pins axi_register_slice_0/s_axi_arburst] [get_bd_pins SpatialIP_0/io_M_AXI_0_ARBURST]
    connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_ARLOCK] [get_bd_pins SpatialIP_0/io_TOP_AXI_ARLOCK]
    connect_bd_net [get_bd_pins axi_register_slice_0/s_axi_arlock] [get_bd_pins SpatialIP_0/io_M_AXI_0_ARLOCK]

    connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_AWVALID] [get_bd_pins SpatialIP_0/io_TOP_AXI_AWVALID]
    connect_bd_net [get_bd_pins axi_register_slice_0/s_axi_awvalid] [get_bd_pins SpatialIP_0/io_M_AXI_0_AWVALID]
    connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_AWREADY] [get_bd_pins SpatialIP_0/io_TOP_AXI_AWREADY]
    connect_bd_net [get_bd_pins axi_register_slice_0/s_axi_awready] [get_bd_pins SpatialIP_0/io_M_AXI_0_AWREADY]
    connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_AWADDR] [get_bd_pins SpatialIP_0/io_TOP_AXI_AWADDR]
    connect_bd_net [get_bd_pins axi_register_slice_0/s_axi_awaddr] [get_bd_pins SpatialIP_0/io_M_AXI_0_AWADDR]
    connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_AWLEN] [get_bd_pins SpatialIP_0/io_TOP_AXI_AWLEN]
    connect_bd_net [get_bd_pins axi_register_slice_0/s_axi_awlen] [get_bd_pins SpatialIP_0/io_M_AXI_0_AWLEN]

    connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_RVALID] [get_bd_pins SpatialIP_0/io_TOP_AXI_RVALID]
    connect_bd_net [get_bd_pins axi_register_slice_0/s_axi_rvalid] [get_bd_pins SpatialIP_0/io_M_AXI_0_RVALID]
    connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_RREADY] [get_bd_pins SpatialIP_0/io_TOP_AXI_RREADY]
    connect_bd_net [get_bd_pins axi_register_slice_0/s_axi_rready] [get_bd_pins SpatialIP_0/io_M_AXI_0_RREADY]

    connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_WVALID] [get_bd_pins SpatialIP_0/io_TOP_AXI_WVALID]
    connect_bd_net [get_bd_pins axi_register_slice_0/s_axi_wvalid] [get_bd_pins SpatialIP_0/io_M_AXI_0_WVALID]
    connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_WREADY] [get_bd_pins SpatialIP_0/io_TOP_AXI_WREADY]
    connect_bd_net [get_bd_pins axi_register_slice_0/s_axi_wready] [get_bd_pins SpatialIP_0/io_M_AXI_0_WREADY]
    connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_WDATA] [get_bd_pins SpatialIP_0/io_TOP_AXI_WDATA]
    connect_bd_net [get_bd_pins axi_register_slice_0/s_axi_wdata] [get_bd_pins SpatialIP_0/io_M_AXI_0_WDATA]
    connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_WSTRB] [get_bd_pins SpatialIP_0/io_TOP_AXI_WSTRB]
    connect_bd_net [get_bd_pins axi_register_slice_0/s_axi_wstrb] [get_bd_pins SpatialIP_0/io_M_AXI_0_WSTRB]

    connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_BVALID] [get_bd_pins SpatialIP_0/io_TOP_AXI_BVALID]
    connect_bd_net [get_bd_pins axi_register_slice_0/s_axi_bvalid] [get_bd_pins SpatialIP_0/io_M_AXI_0_BVALID]
    connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_BREADY] [get_bd_pins SpatialIP_0/io_TOP_AXI_BREADY]
    connect_bd_net [get_bd_pins axi_register_slice_0/s_axi_bready] [get_bd_pins SpatialIP_0/io_M_AXI_0_BREADY]


    # dwidth signals
    connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_arvalid] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_ARVALID]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/s_axi_arvalid] [get_bd_pins axi_dwidth_converter_0/m_axi_arvalid]
    connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_arready] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_ARREADY]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/s_axi_arready] [get_bd_pins axi_dwidth_converter_0/m_axi_arready]
    connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_araddr] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_ARADDR]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/s_axi_araddr] [get_bd_pins axi_dwidth_converter_0/m_axi_araddr]
    connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_arlen] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_ARLEN]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/s_axi_arlen] [get_bd_pins axi_dwidth_converter_0/m_axi_arlen]
    connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_arsize] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_ARSIZE]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/s_axi_arsize] [get_bd_pins axi_dwidth_converter_0/m_axi_arsize]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_arid] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_ARID]
    # connect_bd_net [get_bd_pins axi_protocol_converter_0/s_axi_arid] [get_bd_pins axi_dwidth_converter_0/m_axi_arid]
    connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_arburst] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_ARBURST]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/s_axi_arburst] [get_bd_pins axi_dwidth_converter_0/m_axi_arburst]
    connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_arlock] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_ARLOCK]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/s_axi_arlock] [get_bd_pins axi_dwidth_converter_0/m_axi_arlock]


    connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_awvalid] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_AWVALID]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/s_axi_awvalid] [get_bd_pins axi_dwidth_converter_0/m_axi_awvalid]
    connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_awready] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_AWREADY]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/s_axi_awready] [get_bd_pins axi_dwidth_converter_0/m_axi_awready]
    connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_awaddr] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_AWADDR]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/s_axi_awaddr] [get_bd_pins axi_dwidth_converter_0/m_axi_awaddr]
    connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_awlen] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_AWLEN]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/s_axi_awlen] [get_bd_pins axi_dwidth_converter_0/m_axi_awlen]

    connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_rvalid] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_RVALID]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/s_axi_rvalid] [get_bd_pins axi_dwidth_converter_0/m_axi_rvalid]
    connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_rready] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_RREADY]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/s_axi_rready] [get_bd_pins axi_dwidth_converter_0/m_axi_rready]

    connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_wvalid] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_WVALID]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/s_axi_wvalid] [get_bd_pins axi_dwidth_converter_0/m_axi_wvalid]
    connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_wready] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_WREADY]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/s_axi_wready] [get_bd_pins axi_dwidth_converter_0/m_axi_wready]
    connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_wdata] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_WDATA]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/s_axi_wdata] [get_bd_pins axi_dwidth_converter_0/m_axi_wdata]
    connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_wstrb] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_WSTRB]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/s_axi_wstrb] [get_bd_pins axi_dwidth_converter_0/m_axi_wstrb]

    connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_bvalid] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_BVALID]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/s_axi_bvalid] [get_bd_pins axi_dwidth_converter_0/m_axi_bvalid]
    connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_bready] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_BREADY]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/s_axi_bready] [get_bd_pins axi_dwidth_converter_0/m_axi_bready]

    # protocol signals
    connect_bd_net [get_bd_pins axi_protocol_converter_0/m_axi_arvalid] [get_bd_pins SpatialIP_0/io_PROTOCOL_AXI_ARVALID]
    connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_ARVALID] [get_bd_pins axi_protocol_converter_0/m_axi_arvalid]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/m_axi_arready] [get_bd_pins SpatialIP_0/io_PROTOCOL_AXI_ARREADY]
    connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_ARREADY] [get_bd_pins axi_protocol_converter_0/m_axi_arready]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/m_axi_araddr] [get_bd_pins SpatialIP_0/io_PROTOCOL_AXI_ARADDR]
    connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_ARADDR] [get_bd_pins axi_protocol_converter_0/m_axi_araddr]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/m_axi_arlen] [get_bd_pins SpatialIP_0/io_PROTOCOL_AXI_ARLEN]
    connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_ARLEN] [get_bd_pins axi_protocol_converter_0/m_axi_arlen]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/m_axi_arsize] [get_bd_pins SpatialIP_0/io_PROTOCOL_AXI_ARSIZE]
    connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_ARSIZE] [get_bd_pins axi_protocol_converter_0/m_axi_arsize]
    # connect_bd_net [get_bd_pins axi_protocol_converter_0/m_axi_arid] [get_bd_pins SpatialIP_0/io_PROTOCOL_AXI_ARID]
    # connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_ARID] [get_bd_pins axi_protocol_converter_0/m_axi_arid]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/m_axi_arburst] [get_bd_pins SpatialIP_0/io_PROTOCOL_AXI_ARBURST]
    connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_ARBURST] [get_bd_pins axi_protocol_converter_0/m_axi_arburst]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/m_axi_arlock] [get_bd_pins SpatialIP_0/io_PROTOCOL_AXI_ARLOCK]
    connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_ARLOCK] [get_bd_pins axi_protocol_converter_0/m_axi_arlock]

    connect_bd_net [get_bd_pins axi_protocol_converter_0/m_axi_awvalid] [get_bd_pins SpatialIP_0/io_PROTOCOL_AXI_AWVALID]
    connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_AWVALID] [get_bd_pins axi_protocol_converter_0/m_axi_awvalid]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/m_axi_awready] [get_bd_pins SpatialIP_0/io_PROTOCOL_AXI_AWREADY]
    connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_AWREADY] [get_bd_pins axi_protocol_converter_0/m_axi_awready]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/m_axi_awaddr] [get_bd_pins SpatialIP_0/io_PROTOCOL_AXI_AWADDR]
    connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_AWADDR] [get_bd_pins axi_protocol_converter_0/m_axi_awaddr]

    connect_bd_net [get_bd_pins axi_protocol_converter_0/m_axi_rvalid] [get_bd_pins SpatialIP_0/io_PROTOCOL_AXI_RVALID]
    connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_RVALID] [get_bd_pins axi_protocol_converter_0/m_axi_rvalid]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/m_axi_rready] [get_bd_pins SpatialIP_0/io_PROTOCOL_AXI_RREADY]
    connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_RREADY] [get_bd_pins axi_protocol_converter_0/m_axi_rready]

    connect_bd_net [get_bd_pins axi_protocol_converter_0/m_axi_wvalid] [get_bd_pins SpatialIP_0/io_PROTOCOL_AXI_WVALID]
    connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_WVALID] [get_bd_pins axi_protocol_converter_0/m_axi_wvalid]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/m_axi_wready] [get_bd_pins SpatialIP_0/io_PROTOCOL_AXI_WREADY]
    connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_WREADY] [get_bd_pins axi_protocol_converter_0/m_axi_wready]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/m_axi_wdata] [get_bd_pins SpatialIP_0/io_PROTOCOL_AXI_WDATA]
    connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_WDATA] [get_bd_pins axi_protocol_converter_0/m_axi_wdata]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/m_axi_wstrb] [get_bd_pins SpatialIP_0/io_PROTOCOL_AXI_WSTRB]
    connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_WSTRB] [get_bd_pins axi_protocol_converter_0/m_axi_wstrb]

    connect_bd_net [get_bd_pins axi_protocol_converter_0/m_axi_bvalid] [get_bd_pins SpatialIP_0/io_PROTOCOL_AXI_BVALID]
    connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_BVALID] [get_bd_pins axi_protocol_converter_0/m_axi_bvalid]
    connect_bd_net [get_bd_pins axi_protocol_converter_0/m_axi_bready] [get_bd_pins SpatialIP_0/io_PROTOCOL_AXI_BREADY]
    connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_BREADY] [get_bd_pins axi_protocol_converter_0/m_axi_bready]

    # # clockconverter signals
    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_arvalid] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_ARVALID]
    # connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_ARVALID] [get_bd_pins axi_clock_converter_0/m_axi_arvalid]
    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_arready] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_ARREADY]
    # connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_ARREADY] [get_bd_pins axi_clock_converter_0/m_axi_arready]
    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_araddr] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_ARADDR]
    # connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_ARADDR] [get_bd_pins axi_clock_converter_0/m_axi_araddr]
    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_arlen] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_ARLEN]
    # connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_ARLEN] [get_bd_pins axi_clock_converter_0/m_axi_arlen]
    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_arsize] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_ARSIZE]
    # connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_ARSIZE] [get_bd_pins axi_clock_converter_0/m_axi_arsize]
    # # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_arid] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_ARID]
    # # connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_ARID] [get_bd_pins axi_clock_converter_0/m_axi_arid]
    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_arburst] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_ARBURST]
    # connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_ARBURST] [get_bd_pins axi_clock_converter_0/m_axi_arburst]
    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_arlock] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_ARLOCK]
    # connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_ARLOCK] [get_bd_pins axi_clock_converter_0/m_axi_arlock]

    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_awvalid] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_AWVALID]
    # connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_AWVALID] [get_bd_pins axi_clock_converter_0/m_axi_awvalid]
    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_awready] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_AWREADY]
    # connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_AWREADY] [get_bd_pins axi_clock_converter_0/m_axi_awready]
    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_awaddr] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_AWADDR]
    # connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_AWADDR] [get_bd_pins axi_clock_converter_0/m_axi_awaddr]

    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_rvalid] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_RVALID]
    # connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_RVALID] [get_bd_pins axi_clock_converter_0/m_axi_rvalid]
    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_rready] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_RREADY]
    # connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_RREADY] [get_bd_pins axi_clock_converter_0/m_axi_rready]

    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_wvalid] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_WVALID]
    # connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_WVALID] [get_bd_pins axi_clock_converter_0/m_axi_wvalid]
    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_wready] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_WREADY]
    # connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_WREADY] [get_bd_pins axi_clock_converter_0/m_axi_wready]

    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_bvalid] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_BVALID]
    # connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_BVALID] [get_bd_pins axi_clock_converter_0/m_axi_bvalid]
    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_bready] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_BREADY]
    # connect_bd_net [get_bd_pins processing_system7_0/S_AXI_HP0_BREADY] [get_bd_pins axi_clock_converter_0/m_axi_bready]

    ### FINISH UP HP0
      # Top -> axi slicer
      connect_bd_intf_net [get_bd_intf_pins SpatialIP_0/io_M_AXI_0] [get_bd_intf_pins axi_register_slice_0/S_AXI]
      # axi slicer -> data width converter
      connect_bd_intf_net [get_bd_intf_pins axi_register_slice_0/M_AXI] [get_bd_intf_pins axi_dwidth_converter_0/S_AXI]
      # data width converter -> protocol converter
      connect_bd_intf_net [get_bd_intf_pins axi_dwidth_converter_0/M_AXI] [get_bd_intf_pins axi_protocol_converter_0/S_AXI]

    ### HP1 Begin {
      # Enable HP1, connect faster clock
      set_property -dict [list CONFIG.PCW_USE_S_AXI_HP1 {1}] [get_bd_cells processing_system7_0]
      ## CLOCK CROSSING HACK
      # connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK1] [get_bd_pins processing_system7_0/S_AXI_HP1_ACLK]
      connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK0] [get_bd_pins processing_system7_0/S_AXI_HP1_ACLK]

      # Create axi slice for timing
      create_bd_cell -type ip -vlnv xilinx.com:ip:axi_register_slice:2.1 axi_register_slice_1
      connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK0] [get_bd_pins axi_register_slice_1/aclk]
      connect_bd_net [get_bd_pins rst_ps7_0_${CLOCK_FREQ_MHZ}M/peripheral_aresetn] [get_bd_pins axi_register_slice_1/aresetn]
    set_property -dict [list CONFIG.REG_AW {9} CONFIG.REG_AR {9} CONFIG.REG_R {9} CONFIG.REG_W {9} CONFIG.REG_B {9}] [get_bd_cells axi_register_slice_1]

      # 512-to-64 data width converter
      create_bd_cell -type ip -vlnv xilinx.com:ip:axi_dwidth_converter:2.1 axi_dwidth_converter_1
      set_property -dict [list CONFIG.SI_ID_WIDTH.VALUE_SRC USER CONFIG.SI_DATA_WIDTH.VALUE_SRC USER CONFIG.MI_DATA_WIDTH.VALUE_SRC USER CONFIG.READ_WRITE_MODE.VALUE_SRC USER CONFIG.PROTOCOL.VALUE_SRC USER CONFIG.ADDR_WIDTH.VALUE_SRC USER] [get_bd_cells axi_dwidth_converter_1]
      set_property -dict [list CONFIG.SI_DATA_WIDTH {512} CONFIG.SI_ID_WIDTH {32} CONFIG.MI_DATA_WIDTH {64}] [get_bd_cells axi_dwidth_converter_1]
      connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK0] [get_bd_pins axi_dwidth_converter_1/s_axi_aclk]
      connect_bd_net [get_bd_pins rst_ps7_0_${CLOCK_FREQ_MHZ}M/peripheral_aresetn] [get_bd_pins axi_dwidth_converter_1/s_axi_aresetn]

      # AXI4 to AXI3 protocol converter
      create_bd_cell -type ip -vlnv xilinx.com:ip:axi_protocol_converter:2.1 axi_protocol_converter_1
      set_property -dict [list CONFIG.MI_PROTOCOL.VALUE_SRC USER CONFIG.ID_WIDTH.VALUE_SRC USER CONFIG.DATA_WIDTH.VALUE_SRC USER CONFIG.SI_PROTOCOL.VALUE_SRC USER] [get_bd_cells axi_protocol_converter_1]
      set_property -dict [list CONFIG.MI_PROTOCOL {AXI3} CONFIG.DATA_WIDTH {64} CONFIG.ID_WIDTH {6} CONFIG.TRANSLATION_MODE {2}] [get_bd_cells axi_protocol_converter_1]
      connect_bd_net [get_bd_pins rst_ps7_0_${CLOCK_FREQ_MHZ}M/peripheral_aresetn] [get_bd_pins axi_protocol_converter_1/aresetn]
      connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK0] [get_bd_pins axi_protocol_converter_1/aclk]

      # Clock converter from FCLKCLK0 <-> FCLKCLK1
      create_bd_cell -type ip -vlnv xilinx.com:ip:axi_clock_converter:2.1 axi_clock_converter_1
      set_property -dict [list CONFIG.ID_WIDTH.VALUE_SRC USER CONFIG.DATA_WIDTH.VALUE_SRC USER CONFIG.READ_WRITE_MODE.VALUE_SRC USER CONFIG.ADDR_WIDTH.VALUE_SRC USER CONFIG.PROTOCOL.VALUE_SRC USER] [get_bd_cells axi_clock_converter_1]
      set_property -dict [list CONFIG.PROTOCOL {AXI3} CONFIG.DATA_WIDTH {64} CONFIG.ID_WIDTH {6}] [get_bd_cells axi_clock_converter_1]
      connect_bd_net [get_bd_pins rst_ps7_0_${CLOCK_FREQ_MHZ}M/peripheral_aresetn] [get_bd_pins axi_clock_converter_1/s_axi_aresetn]
      connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK0] [get_bd_pins axi_clock_converter_1/s_axi_aclk]
      ## CLOCK CROSSING HACK
      # connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK1] [get_bd_pins axi_clock_converter_1/m_axi_aclk]
      # connect_bd_net [get_bd_pins proc_sys_reset_fclk1/peripheral_aresetn] [get_bd_pins axi_clock_converter_1/m_axi_aresetn]
      connect_bd_net [get_bd_pins proc_sys_reset_fclk1/peripheral_aresetn] [get_bd_pins axi_clock_converter_1/m_axi_aresetn]
      connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK0] [get_bd_pins axi_clock_converter_1/m_axi_aclk]

      # Top -> axi slicer
      connect_bd_intf_net [get_bd_intf_pins SpatialIP_0/io_M_AXI_1] [get_bd_intf_pins axi_register_slice_1/S_AXI]
      # axi slicer -> data width converter
      connect_bd_intf_net [get_bd_intf_pins axi_register_slice_1/M_AXI] [get_bd_intf_pins axi_dwidth_converter_1/S_AXI]
      # data width converter -> protocol converter
      connect_bd_intf_net [get_bd_intf_pins axi_dwidth_converter_1/M_AXI] [get_bd_intf_pins axi_protocol_converter_1/S_AXI]

    ### } HP1 end

    ### HP2 Begin {
      # Enable HP2, connect faster clock
      set_property -dict [list CONFIG.PCW_USE_S_AXI_HP2 {1}] [get_bd_cells processing_system7_0]
      ## CLOCK CROSSING HACK
      # connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK1] [get_bd_pins processing_system7_0/S_AXI_HP2_ACLK]
      connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK0] [get_bd_pins processing_system7_0/S_AXI_HP2_ACLK]

      # Create axi slice for timing
      create_bd_cell -type ip -vlnv xilinx.com:ip:axi_register_slice:2.1 axi_register_slice_2
      connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK0] [get_bd_pins axi_register_slice_2/aclk]
      connect_bd_net [get_bd_pins rst_ps7_0_${CLOCK_FREQ_MHZ}M/peripheral_aresetn] [get_bd_pins axi_register_slice_2/aresetn]
      set_property -dict [list CONFIG.REG_AW {9} CONFIG.REG_AR {9} CONFIG.REG_R {9} CONFIG.REG_W {9} CONFIG.REG_B {9}] [get_bd_cells axi_register_slice_0]


      # 512-to-64 data width converter
      create_bd_cell -type ip -vlnv xilinx.com:ip:axi_dwidth_converter:2.1 axi_dwidth_converter_2
      set_property -dict [list CONFIG.SI_ID_WIDTH.VALUE_SRC USER CONFIG.SI_DATA_WIDTH.VALUE_SRC USER CONFIG.MI_DATA_WIDTH.VALUE_SRC USER CONFIG.READ_WRITE_MODE.VALUE_SRC USER CONFIG.PROTOCOL.VALUE_SRC USER CONFIG.ADDR_WIDTH.VALUE_SRC USER] [get_bd_cells axi_dwidth_converter_2]
      set_property -dict [list CONFIG.SI_DATA_WIDTH {512} CONFIG.SI_ID_WIDTH {32} CONFIG.MI_DATA_WIDTH {64}] [get_bd_cells axi_dwidth_converter_2]
      connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK0] [get_bd_pins axi_dwidth_converter_2/s_axi_aclk]
      connect_bd_net [get_bd_pins rst_ps7_0_${CLOCK_FREQ_MHZ}M/peripheral_aresetn] [get_bd_pins axi_dwidth_converter_2/s_axi_aresetn]

      # AXI4 to AXI3 protocol converter
      create_bd_cell -type ip -vlnv xilinx.com:ip:axi_protocol_converter:2.1 axi_protocol_converter_2
      set_property -dict [list CONFIG.MI_PROTOCOL.VALUE_SRC USER CONFIG.ID_WIDTH.VALUE_SRC USER CONFIG.DATA_WIDTH.VALUE_SRC USER CONFIG.SI_PROTOCOL.VALUE_SRC USER] [get_bd_cells axi_protocol_converter_2]
      set_property -dict [list CONFIG.MI_PROTOCOL {AXI3} CONFIG.DATA_WIDTH {64} CONFIG.ID_WIDTH {6} CONFIG.TRANSLATION_MODE {2}] [get_bd_cells axi_protocol_converter_2]
      connect_bd_net [get_bd_pins rst_ps7_0_${CLOCK_FREQ_MHZ}M/peripheral_aresetn] [get_bd_pins axi_protocol_converter_2/aresetn]
      connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK0] [get_bd_pins axi_protocol_converter_2/aclk]

      # Clock converter from FCLKCLK0 <-> FCLKCLK1
      create_bd_cell -type ip -vlnv xilinx.com:ip:axi_clock_converter:2.1 axi_clock_converter_2
      set_property -dict [list CONFIG.ID_WIDTH.VALUE_SRC USER CONFIG.DATA_WIDTH.VALUE_SRC USER CONFIG.READ_WRITE_MODE.VALUE_SRC USER CONFIG.ADDR_WIDTH.VALUE_SRC USER CONFIG.PROTOCOL.VALUE_SRC USER] [get_bd_cells axi_clock_converter_2]
      set_property -dict [list CONFIG.PROTOCOL {AXI3} CONFIG.DATA_WIDTH {64} CONFIG.ID_WIDTH {6}] [get_bd_cells axi_clock_converter_2]
      connect_bd_net [get_bd_pins rst_ps7_0_${CLOCK_FREQ_MHZ}M/peripheral_aresetn] [get_bd_pins axi_clock_converter_2/s_axi_aresetn]
      connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK0] [get_bd_pins axi_clock_converter_2/s_axi_aclk]
      ## CLOCK CROSSING HACK
      # connect_bd_net [get_bd_pins proc_sys_reset_fclk1/peripheral_aresetn] [get_bd_pins axi_clock_converter_2/m_axi_aresetn]
      # connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK1] [get_bd_pins axi_clock_converter_2/m_axi_aclk]
      connect_bd_net [get_bd_pins proc_sys_reset_fclk1/peripheral_aresetn] [get_bd_pins axi_clock_converter_2/m_axi_aresetn]
      connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK0] [get_bd_pins axi_clock_converter_2/m_axi_aclk]

      # Top -> axi slicer
      connect_bd_intf_net [get_bd_intf_pins SpatialIP_0/io_M_AXI_2] [get_bd_intf_pins axi_register_slice_2/S_AXI]
      # axi slicer -> data width converter
      connect_bd_intf_net [get_bd_intf_pins axi_register_slice_2/M_AXI] [get_bd_intf_pins axi_dwidth_converter_2/S_AXI]
      # data width converter -> protocol converter
      connect_bd_intf_net [get_bd_intf_pins axi_dwidth_converter_2/M_AXI] [get_bd_intf_pins axi_protocol_converter_2/S_AXI]
    ### } HP2 end

    ### HP3 Begin {
      # Enable HP3, connect faster clock
      set_property -dict [list CONFIG.PCW_USE_S_AXI_HP3 {1}] [get_bd_cells processing_system7_0]
      ## CLOCK CROSSING HACK
      # connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK1] [get_bd_pins processing_system7_0/S_AXI_HP3_ACLK]
      connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK0] [get_bd_pins processing_system7_0/S_AXI_HP3_ACLK]

      # Create axi slice for timing
      create_bd_cell -type ip -vlnv xilinx.com:ip:axi_register_slice:2.1 axi_register_slice_3
      connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK0] [get_bd_pins axi_register_slice_3/aclk]
      connect_bd_net [get_bd_pins rst_ps7_0_${CLOCK_FREQ_MHZ}M/peripheral_aresetn] [get_bd_pins axi_register_slice_3/aresetn]
      set_property -dict [list CONFIG.REG_AW {9} CONFIG.REG_AR {9} CONFIG.REG_R {9} CONFIG.REG_W {9} CONFIG.REG_B {9}] [get_bd_cells axi_register_slice_0]

      # 512-to-64 data width converter
      create_bd_cell -type ip -vlnv xilinx.com:ip:axi_dwidth_converter:2.1 axi_dwidth_converter_3
      set_property -dict [list CONFIG.SI_ID_WIDTH.VALUE_SRC USER CONFIG.SI_DATA_WIDTH.VALUE_SRC USER CONFIG.MI_DATA_WIDTH.VALUE_SRC USER CONFIG.READ_WRITE_MODE.VALUE_SRC USER CONFIG.PROTOCOL.VALUE_SRC USER CONFIG.ADDR_WIDTH.VALUE_SRC USER] [get_bd_cells axi_dwidth_converter_3]
      set_property -dict [list CONFIG.SI_DATA_WIDTH {512} CONFIG.SI_ID_WIDTH {32} CONFIG.MI_DATA_WIDTH {64}] [get_bd_cells axi_dwidth_converter_3]
      connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK0] [get_bd_pins axi_dwidth_converter_3/s_axi_aclk]
      connect_bd_net [get_bd_pins rst_ps7_0_${CLOCK_FREQ_MHZ}M/peripheral_aresetn] [get_bd_pins axi_dwidth_converter_3/s_axi_aresetn]

      # AXI4 to AXI3 protocol converter
      create_bd_cell -type ip -vlnv xilinx.com:ip:axi_protocol_converter:2.1 axi_protocol_converter_3
      set_property -dict [list CONFIG.MI_PROTOCOL.VALUE_SRC USER CONFIG.ID_WIDTH.VALUE_SRC USER CONFIG.DATA_WIDTH.VALUE_SRC USER CONFIG.SI_PROTOCOL.VALUE_SRC USER] [get_bd_cells axi_protocol_converter_3]
      set_property -dict [list CONFIG.MI_PROTOCOL {AXI3} CONFIG.DATA_WIDTH {64} CONFIG.ID_WIDTH {6} CONFIG.TRANSLATION_MODE {2}] [get_bd_cells axi_protocol_converter_3]
      connect_bd_net [get_bd_pins rst_ps7_0_${CLOCK_FREQ_MHZ}M/peripheral_aresetn] [get_bd_pins axi_protocol_converter_3/aresetn]
      connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK0] [get_bd_pins axi_protocol_converter_3/aclk]

      # Clock converter from FCLKCLK0 <-> FCLKCLK1
      create_bd_cell -type ip -vlnv xilinx.com:ip:axi_clock_converter:2.1 axi_clock_converter_3
      set_property -dict [list CONFIG.ID_WIDTH.VALUE_SRC USER CONFIG.DATA_WIDTH.VALUE_SRC USER CONFIG.READ_WRITE_MODE.VALUE_SRC USER CONFIG.ADDR_WIDTH.VALUE_SRC USER CONFIG.PROTOCOL.VALUE_SRC USER] [get_bd_cells axi_clock_converter_3]
      set_property -dict [list CONFIG.PROTOCOL {AXI3} CONFIG.DATA_WIDTH {64} CONFIG.ID_WIDTH {6}] [get_bd_cells axi_clock_converter_3]
      connect_bd_net [get_bd_pins rst_ps7_0_${CLOCK_FREQ_MHZ}M/peripheral_aresetn] [get_bd_pins axi_clock_converter_3/s_axi_aresetn]
      connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK0] [get_bd_pins axi_clock_converter_3/s_axi_aclk]
      ## CLOCK CROSSING HACK
      # connect_bd_net [get_bd_pins proc_sys_reset_fclk1/peripheral_aresetn] [get_bd_pins axi_clock_converter_3/m_axi_aresetn]
      # connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK1] [get_bd_pins axi_clock_converter_3/m_axi_aclk]
      connect_bd_net [get_bd_pins proc_sys_reset_fclk1/peripheral_aresetn] [get_bd_pins axi_clock_converter_3/m_axi_aresetn]
      connect_bd_net [get_bd_pins processing_system7_0/FCLK_CLK0] [get_bd_pins axi_clock_converter_3/m_axi_aclk]

      # Top -> axi slicer
      connect_bd_intf_net [get_bd_intf_pins SpatialIP_0/io_M_AXI_3] [get_bd_intf_pins axi_register_slice_3/S_AXI]
      # axi slicer -> data width converter
      connect_bd_intf_net [get_bd_intf_pins axi_register_slice_3/M_AXI] [get_bd_intf_pins axi_dwidth_converter_3/S_AXI]
      # data width converter -> protocol converter
      connect_bd_intf_net [get_bd_intf_pins axi_dwidth_converter_3/M_AXI] [get_bd_intf_pins axi_protocol_converter_3/S_AXI]
    ### } HP3 end

    ## CLOCK CROSSING HACK
    # # protocol converter -> Clock converter
    # connect_bd_intf_net [get_bd_intf_pins axi_protocol_converter_0/M_AXI] [get_bd_intf_pins axi_clock_converter_0/S_AXI]
    # connect_bd_intf_net [get_bd_intf_pins axi_protocol_converter_1/M_AXI] [get_bd_intf_pins axi_clock_converter_1/S_AXI]
    # connect_bd_intf_net [get_bd_intf_pins axi_protocol_converter_2/M_AXI] [get_bd_intf_pins axi_clock_converter_2/S_AXI]
    # connect_bd_intf_net [get_bd_intf_pins axi_protocol_converter_3/M_AXI] [get_bd_intf_pins axi_clock_converter_3/S_AXI]
    # # Clock converter -> PS
    # connect_bd_intf_net [get_bd_intf_pins axi_clock_converter_3/M_AXI] [get_bd_intf_pins processing_system7_0/S_AXI_HP3]
    # connect_bd_intf_net [get_bd_intf_pins axi_clock_converter_0/M_AXI] [get_bd_intf_pins processing_system7_0/S_AXI_HP0]
    # connect_bd_intf_net [get_bd_intf_pins axi_clock_converter_1/M_AXI] [get_bd_intf_pins processing_system7_0/S_AXI_HP1]
    # connect_bd_intf_net [get_bd_intf_pins axi_clock_converter_2/M_AXI] [get_bd_intf_pins processing_system7_0/S_AXI_HP2]

    connect_bd_intf_net [get_bd_intf_pins axi_protocol_converter_0/M_AXI] [get_bd_intf_pins processing_system7_0/S_AXI_HP0]
    connect_bd_intf_net [get_bd_intf_pins axi_protocol_converter_1/M_AXI] [get_bd_intf_pins processing_system7_0/S_AXI_HP1]
    connect_bd_intf_net [get_bd_intf_pins axi_protocol_converter_2/M_AXI] [get_bd_intf_pins processing_system7_0/S_AXI_HP2]
    connect_bd_intf_net [get_bd_intf_pins axi_protocol_converter_3/M_AXI] [get_bd_intf_pins processing_system7_0/S_AXI_HP3]
    # Address assignments
    assign_bd_address [get_bd_addr_segs processing_system7_0/S_AXI_HP0/HP0_DDR_LOWOCM] -target_address_space /SpatialIP_0/io_M_AXI_0
    assign_bd_address [get_bd_addr_segs processing_system7_0/S_AXI_HP1/HP1_DDR_LOWOCM] -target_address_space /SpatialIP_0/io_M_AXI_1
    assign_bd_address [get_bd_addr_segs processing_system7_0/S_AXI_HP2/HP2_DDR_LOWOCM] -target_address_space /SpatialIP_0/io_M_AXI_2
    assign_bd_address [get_bd_addr_segs processing_system7_0/S_AXI_HP3/HP3_DDR_LOWOCM] -target_address_space /SpatialIP_0/io_M_AXI_3


  }
}

validate_bd_design
save_bd_design

make_wrapper -files [get_files ./bd_project/bd_project.srcs/sources_1/bd/design_1/design_1.bd] -top
add_files -norecurse ./bd_project/bd_project.srcs/sources_1/bd/design_1/hdl/design_1_wrapper.v
update_compile_order -fileset sources_1

set_property top design_1_wrapper [current_fileset]
update_compile_order -fileset sources_1

# Copy required files here
if { [file exists ./bd_project/bd_project.srcs/sources_1/bd/design_1/hdl/design_1.v] == 1} {               
    file copy -force ./bd_project/bd_project.srcs/sources_1/bd/design_1/hdl/design_1.v ./design_1.v
} else {
    file copy -force ./bd_project/bd_project.srcs/sources_1/bd/design_1/sim/design_1.v ./design_1.v
}


file copy -force ./bd_project/bd_project.srcs/sources_1/bd/design_1/hdl/design_1.v ./design_1.v
file copy -force ./bd_project/bd_project.srcs/sources_1/bd/design_1/hdl/design_1_wrapper.v ./design_1_wrapper.v

close_project
