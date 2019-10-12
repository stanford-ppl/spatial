--------------------------------------------------------------------------------
-- Project: CustomLogic
--------------------------------------------------------------------------------
--  Module: CustomLogic
--    File: CustomLogic.vhd
--    Date: 2017-12-15
--     Rev: 0.1
--  Author: PP
--------------------------------------------------------------------------------
-- CustomLogic wrapper for the user design
--------------------------------------------------------------------------------
-- 0.1, 2017-12-15, PP, Initial release
--------------------------------------------------------------------------------


library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;
use work.CustomLogicPkg.all;


entity CustomLogic is
	generic (
		DATA_WIDTH 				: natural := 256
	);
    port (
        -- Clock/Reset/Ctrl
		clk250					: in  std_logic;	-- Clock 250 MHz
		srst250					: in  std_logic; 	-- Synchronous Reset (PCIe reset)
		PipelineClear			: in  std_logic;	-- Asserted after a DSStopAcquisition
        -- AXI Stream Input Interface
		axis_tvalid_in			: in  std_logic;
		axis_tready_in			: out std_logic;
		axis_tdata_in			: in  std_logic_vector(DATA_WIDTH - 1 downto 0);
		axis_tuser_in			: in  std_logic_vector(  3 downto 0);
        -- Image Header and Metadata
		Metadata_in				: in  Metadata_rec;
        -- AXI Stream Output Interface
		axis_tvalid_out			: out std_logic;
		axis_tready_out			: in  std_logic;
		axis_tdata_out			: out std_logic_vector(DATA_WIDTH - 1 downto 0);
		axis_tuser_out			: out std_logic_vector(  3 downto 0);
        -- Image Header and Metadata
		Metadata_out			: out Metadata_rec;
        -- Memento Interface (CustomLogic Event)
		CustomLogic_event		: out std_logic;
		CustomLogic_event_arg0	: out std_logic_vector( 31 downto 0);
		CustomLogic_event_arg1	: out std_logic_vector( 31 downto 0);
		-- Control Interface
		CustomLogic_ctrl_addr		: in  std_logic_vector( 15 downto 0);
		CustomLogic_ctrl_data_in_ce	: in  std_logic;
		CustomLogic_ctrl_data_in	: in  std_logic_vector( 31 downto 0);
		CustomLogic_ctrl_data_out	: out std_logic_vector( 31 downto 0);
        -- DDR4 Interface
		m_axi_awaddr 			: out std_logic_vector( 31 downto 0);
		m_axi_awlen 			: out std_logic_vector(  7 downto 0);
		m_axi_awsize 			: out std_logic_vector(  2 downto 0);
		m_axi_awburst 			: out std_logic_vector(  1 downto 0);
		m_axi_awlock 			: out std_logic;
		m_axi_awcache 			: out std_logic_vector(  3 downto 0);
		m_axi_awprot 			: out std_logic_vector(  2 downto 0);
		m_axi_awqos 			: out std_logic_vector(  3 downto 0);
		m_axi_awvalid 			: out std_logic;
		m_axi_awready 			: in  std_logic;
		m_axi_wdata 			: out std_logic_vector(DATA_WIDTH   - 1 downto 0);
		m_axi_wstrb 			: out std_logic_vector(DATA_WIDTH/8 - 1 downto 0);
		m_axi_wlast 			: out std_logic;
		m_axi_wvalid 			: out std_logic;
		m_axi_wready 			: in  std_logic;
		m_axi_bresp 			: in  std_logic_vector(  1 downto 0);
		m_axi_bvalid 			: in  std_logic;
		m_axi_bready 			: out std_logic;
		m_axi_araddr 			: out std_logic_vector( 31 downto 0);
		m_axi_arlen 			: out std_logic_vector(  7 downto 0);
		m_axi_arsize 			: out std_logic_vector(  2 downto 0);
		m_axi_arburst 			: out std_logic_vector(  1 downto 0);
		m_axi_arlock 			: out std_logic;
		m_axi_arcache 			: out std_logic_vector(  3 downto 0);
		m_axi_arprot 			: out std_logic_vector(  2 downto 0);
		m_axi_arqos 			: out std_logic_vector(  3 downto 0);
		m_axi_arvalid 			: out std_logic;
		m_axi_arready 			: in  std_logic;
		m_axi_rdata 			: in  std_logic_vector(DATA_WIDTH - 1 downto 0);
		m_axi_rresp 			: in  std_logic_vector(  1 downto 0);
		m_axi_rlast 			: in  std_logic;
		m_axi_rvalid 			: in  std_logic;
		m_axi_rready 			: out std_logic
    );
end entity CustomLogic;

architecture behav of CustomLogic is

	----------------------------------------------------------------------------
	-- Constants
	----------------------------------------------------------------------------
	

	----------------------------------------------------------------------------
	-- Types
	----------------------------------------------------------------------------


	----------------------------------------------------------------------------
	-- Functions
	----------------------------------------------------------------------------


	----------------------------------------------------------------------------
	-- Components
	----------------------------------------------------------------------------

    component SpatialIP
      port (
        clock           : in std_logic;
        reset           : in std_logic;
        io_PIPELINECLEAR                 : in  std_logic;  -- Asserted after a DSStopAcquisition
	 	io_AXIS_IN_TVALID				: in std_logic;
	 	io_AXIS_IN_TREADY				: out std_logic;
	 	io_AXIS_IN_TDATA					: in std_logic_vector(255 downto 0);
	 	io_AXIS_IN_TSTRB					: in std_logic_vector(31 downto 0);
	 	io_AXIS_IN_TKEEP					: in std_logic_vector(31 downto 0);
	 	io_AXIS_IN_TLAST					: in std_logic;
	 	io_AXIS_IN_TUSER					: in std_logic_vector(31 downto 0);
	 	io_AXIS_IN_TID					: in std_logic_vector(3 downto 0);
	 	io_AXIS_IN_TDEST					: in std_logic_vector(3 downto 0);
	 	io_AXIS_OUT_TVALID				: out std_logic;
	 	io_AXIS_OUT_TREADY				: in std_logic;
	 	io_AXIS_OUT_TDATA				: out std_logic_vector(255 downto 0);
	 	io_AXIS_OUT_TSTRB				: out std_logic_vector(31 downto 0);
	 	io_AXIS_OUT_TKEEP				: out std_logic_vector(31 downto 0);
	 	io_AXIS_OUT_TLAST				: out std_logic;
	 	io_AXIS_OUT_TID					: out std_logic_vector(3 downto 0);
	 	io_AXIS_OUT_TDEST				: out std_logic_vector(3 downto 0);
	 	io_AXIS_OUT_TUSER				: out std_logic_vector(31 downto 0);
        --    -- Image Header and Metadata
        --io_METADATA_IN                   : in  Metadata_rec;
        --    -- Image Header and Metadata
        --io_METADATA_OUT                  : out Metadata_rec;
        --    -- Memento Interface (CustomLogic Event)
        --io_CUSTOMLOGIC_EVENT             : out std_logic;
        --io_CUSTOMLOGIC_EVENT_ARG0        : out std_logic_vector( 31 downto 0);
        --io_CUSTOMLOGIC_EVENT_ARG1        : out std_logic_vector( 31 downto 0);
        ---- Control Interface
        io_CUSTOMLOGIC_CTRL_ADDR         : in  std_logic_vector( 15 downto 0);
        io_CUSTOMLOGIC_CTRL_DATA_IN_CE   : in  std_logic;
        io_CUSTOMLOGIC_CTRL_DATA_IN      : in  std_logic_vector( 31 downto 0);
        io_CUSTOMLOGIC_CTRL_DATA_OUT     : out std_logic_vector( 31 downto 0);
        -- DRAM --
        io_M_AXI_0_AWID           : out std_logic_vector(31 downto 0) := (others => '-');
        io_M_AXI_0_AWUSER         : out std_logic_vector(31 downto 0) := (others => '-');
        io_M_AXI_0_AWADDR         : out std_logic_vector(31 downto 0);
        io_M_AXI_0_AWLEN          : out std_logic_vector(7 downto 0);
        io_M_AXI_0_AWSIZE         : out std_logic_vector(2 downto 0);
        io_M_AXI_0_AWBURST        : out std_logic_vector(1 downto 0);
        io_M_AXI_0_AWLOCK         : out std_logic;
        io_M_AXI_0_AWCACHE        : out std_logic_vector(3 downto 0);
        io_M_AXI_0_AWPROT         : out std_logic_vector(2 downto 0);
        io_M_AXI_0_AWQOS          : out std_logic_vector(3 downto 0);
        io_M_AXI_0_AWVALID        : out std_logic;
        io_M_AXI_0_AWREADY        : in std_logic;
        io_M_AXI_0_ARID           : out std_logic_vector(31 downto 0) := (others => '-');
        io_M_AXI_0_ARUSER         : out std_logic_vector(31 downto 0) := (others => '-');
        io_M_AXI_0_ARADDR         : out std_logic_vector(31 downto 0);
        io_M_AXI_0_ARLEN          : out std_logic_vector(7 downto 0);
        io_M_AXI_0_ARSIZE         : out std_logic_vector(2 downto 0);
        io_M_AXI_0_ARBURST        : out std_logic_vector(1 downto 0);
        io_M_AXI_0_ARLOCK         : out std_logic;
        io_M_AXI_0_ARCACHE        : out std_logic_vector(3 downto 0);
        io_M_AXI_0_ARPROT         : out std_logic_vector(2 downto 0);
        io_M_AXI_0_ARQOS          : out std_logic_vector(3 downto 0);
        io_M_AXI_0_ARVALID        : out std_logic;
        io_M_AXI_0_ARREADY        : in std_logic;
        io_M_AXI_0_WDATA          : out std_logic_vector(DATA_WIDTH-1 downto 0);
        io_M_AXI_0_WSTRB          : out std_logic_vector(DATA_WIDTH/8 - 1 downto 0);
        io_M_AXI_0_WLAST          : out std_logic;
        io_M_AXI_0_WVALID         : out std_logic;
        io_M_AXI_0_WREADY         : in std_logic;
        io_M_AXI_0_RID            : in std_logic_vector(31 downto 0) := (others => '-');
        io_M_AXI_0_RUSER          : in std_logic_vector(31 downto 0) := (others => '-');
        io_M_AXI_0_RDATA          : in std_logic_vector(DATA_WIDTH-1 downto 0);
        io_M_AXI_0_RRESP          : in std_logic_vector(1 downto 0);
        io_M_AXI_0_RLAST          : in std_logic;
        io_M_AXI_0_RVALID         : in std_logic;
        io_M_AXI_0_RREADY         : out std_logic;
        io_M_AXI_0_BID            : in std_logic_vector(31 downto 0) := (others => '-');
        io_M_AXI_0_BUSER          : in std_logic_vector(31 downto 0) := (others => '-');
        io_M_AXI_0_BRESP          : in std_logic_vector(1 downto 0);
        io_M_AXI_0_BVALID         : in std_logic;
        io_M_AXI_0_BREADY         : out std_logic);
  	end component;

	----------------------------------------------------------------------------
	-- Signals
	----------------------------------------------------------------------------
	-- Control Registers
	signal Frame2Line_bypass	    : std_logic;
	signal MemTrafficGen_en		    : std_logic;
	signal MementoEvent_en		    : std_logic;
	signal MementoEvent_arg0	    : std_logic_vector(31 downto 0);
	signal PixelLut_bypass		    : std_logic;
	signal PixelLut_coef_start	    : std_logic;
	signal PixelLut_coef		    : std_logic_vector( 7 downto 0);
	signal PixelLut_coef_vld	    : std_logic;
	signal PixelLut_coef_done	    : std_logic;
	signal PixelThreshold_bypass    : std_logic;
	signal PixelThreshold_level		: std_logic_vector( 7 downto 0);
	
	-- Pixel LUT
	signal PixelLut_tvalid		    : std_logic;
	signal PixelLut_tready		    : std_logic;
	signal PixelLut_tdata		    : std_logic_vector(DATA_WIDTH - 1 downto 0);
	signal PixelLut_tuser		    : std_logic_vector( 3 downto 0);
	signal PixelLut_metadata	    : Metadata_rec;
	
	-- HLS Pixel Threshold
	signal HlsPixTh_tvalid		    : std_logic;
	signal HlsPixTh_tready		    : std_logic;
	signal HlsPixTh_tdata		    : std_logic_vector(DATA_WIDTH - 1 downto 0);
	signal HlsPixTh_tuser		    : std_logic_vector( 3 downto 0);
	signal HlsPixTh_metadata	    : Metadata_rec;
	
	-- Memento Events
	signal Wraparound_pls		    : std_logic;
	signal Wraparound_cnt		    : std_logic_vector(31 downto 0);


	----------------------------------------------------------------------------
	-- Debug
	----------------------------------------------------------------------------
	-- attribute mark_debug : string;
	-- attribute mark_debug of srst250          : signal is "true";
	-- attribute mark_debug of PipelineClear    : signal is "true";


begin

   U_SpatialIP :  SpatialIP
      port map (
         -- AXI-Lite Interface
         clock         => clk250,
         reset         => srst250,
         io_PIPELINECLEAR => PipelineClear,
         --io_METADATA_IN => Metadata_in,
         --io_METADATA_OUT => Metadata_out,
         --io_CUSTOMLOGIC_EVENT => CustomLogic_event,
         --io_CUSTOMLOGIC_EVENT_ARG0 => CustomLogic_event_arg0,
         --io_CUSTOMLOGIC_EVENT_ARG1 => CustomLogic_event_arg1,
         io_CUSTOMLOGIC_CTRL_ADDR => CustomLogic_ctrl_addr,
         io_CUSTOMLOGIC_CTRL_DATA_IN_CE => CustomLogic_ctrl_data_in_ce,
         io_CUSTOMLOGIC_CTRL_DATA_IN => CustomLogic_ctrl_data_in,
         io_CUSTOMLOGIC_CTRL_DATA_OUT => CustomLogic_ctrl_data_out,
         io_AXIS_IN_TVALID    => axis_tvalid_in,
		 io_AXIS_IN_TREADY    => axis_tready_in,
		 io_AXIS_IN_TDATA    => axis_tdata_in,
		 io_AXIS_IN_TSTRB    => open,
		 io_AXIS_IN_TKEEP    => open,
		 io_AXIS_IN_TUSER    => open,
		 io_AXIS_IN_TLAST    => open,
		 io_AXIS_IN_TID    => axis_tuser_in,
		 io_AXIS_IN_TDEST    => open,
		 io_AXIS_OUT_TVALID    => axis_tvalid_out,
		 io_AXIS_OUT_TREADY    => axis_tready_out,
		 io_AXIS_OUT_TDATA    => axis_tdata_out,
		 io_AXIS_OUT_TSTRB    => open,
		 io_AXIS_OUT_TKEEP    => open,
		 io_AXIS_OUT_TLAST    => open,
		 io_AXIS_OUT_TID    => axis_tuser_out,
		 io_AXIS_OUT_TDEST    => open,
		 io_AXIS_OUT_TUSER    => open,

         -- no connection
         io_M_AXI_0_AWID => open,
         io_M_AXI_0_AWUSER => open,
         io_M_AXI_0_ARID => open,
         io_M_AXI_0_ARUSER => open,
         io_M_AXI_0_BID => open,
         io_M_AXI_0_BUSER => open,
         io_M_AXI_0_RID => open,
         io_M_AXI_0_RUSER => open,

         io_M_AXI_0_AWADDR => m_axi_awaddr, 
         io_M_AXI_0_AWLEN => m_axi_awlen, 
         io_M_AXI_0_AWSIZE => m_axi_awsize, 
         io_M_AXI_0_AWBURST => m_axi_awburst, 
         io_M_AXI_0_AWLOCK => m_axi_awlock, 
         io_M_AXI_0_AWCACHE => m_axi_awcache, 
         io_M_AXI_0_AWPROT => m_axi_awprot, 
         io_M_AXI_0_AWQOS => m_axi_awqos, 
         io_M_AXI_0_AWVALID => m_axi_awvalid, 
         io_M_AXI_0_AWREADY => m_axi_awready, 
         io_M_AXI_0_ARADDR => m_axi_araddr, 
         io_M_AXI_0_ARLEN => m_axi_arlen, 
         io_M_AXI_0_ARSIZE => m_axi_arsize, 
         io_M_AXI_0_ARBURST => m_axi_arburst, 
         io_M_AXI_0_ARLOCK => m_axi_arlock, 
         io_M_AXI_0_ARCACHE => m_axi_arcache, 
         io_M_AXI_0_ARPROT => m_axi_arprot, 
         io_M_AXI_0_ARQOS => m_axi_arqos, 
         io_M_AXI_0_ARVALID => m_axi_arvalid, 
         io_M_AXI_0_ARREADY => m_axi_arready, 
         io_M_AXI_0_WDATA => m_axi_wdata, 
         io_M_AXI_0_WSTRB => m_axi_wstrb, 
         io_M_AXI_0_WLAST => m_axi_wlast, 
         io_M_AXI_0_WVALID => m_axi_wvalid, 
         io_M_AXI_0_WREADY => m_axi_wready, 
         io_M_AXI_0_RDATA => m_axi_rdata, 
         io_M_AXI_0_RRESP => m_axi_rresp, 
         io_M_AXI_0_RLAST => m_axi_rlast, 
         io_M_AXI_0_RVALID => m_axi_rvalid, 
         io_M_AXI_0_RREADY => m_axi_rready, 
         io_M_AXI_0_BRESP => m_axi_bresp, 
         io_M_AXI_0_BVALID => m_axi_bvalid, 
         io_M_AXI_0_BREADY => m_axi_bready);


	---- Control Registers (Example)
	--iControlRegs : entity work.control_registers
	--	port map (
	--		clk							=> clk250,
	--		srst						=> srst250,
	--		CustomLogic_ctrl_addr		=> CustomLogic_ctrl_addr,
	--		CustomLogic_ctrl_data_in_ce	=> CustomLogic_ctrl_data_in_ce,
	--		CustomLogic_ctrl_data_in	=> CustomLogic_ctrl_data_in,
	--		CustomLogic_ctrl_data_out	=> CustomLogic_ctrl_data_out,
	--		Frame2Line_bypass			=> Frame2Line_bypass,
	--		MemTrafficGen_en			=> MemTrafficGen_en,
	--		MementoEvent_en				=> MementoEvent_en,
	--		MementoEvent_arg0			=> MementoEvent_arg0,
	--		PixelLut_bypass				=> PixelLut_bypass,
	--		PixelLut_coef_start			=> PixelLut_coef_start,
	--		PixelLut_coef_vld			=> PixelLut_coef_vld,
	--		PixelLut_coef				=> PixelLut_coef,
	--		PixelLut_coef_done			=> PixelLut_coef_done,
	--		PixelThreshold_bypass		=> PixelThreshold_bypass,
	--		PixelThreshold_level		=> PixelThreshold_level
	--	);
	
	---- Pixel Lookup Table 8-bit (Example)
	--iPixelLut : entity work.pix_lut8b
	--	generic map (
	--		DATA_WIDTH 				=> DATA_WIDTH
	--	)
	--	port map (
	--		PipelineClear			=> PipelineClear,
	--		PixelLut_bypass			=> PixelLut_bypass,
	--		PixelLut_coef_start		=> PixelLut_coef_start,
	--		PixelLut_coef_vld		=> PixelLut_coef_vld,
	--		PixelLut_coef			=> PixelLut_coef,
	--		PixelLut_coef_done		=> PixelLut_coef_done,
	--		axis_aclk	 			=> clk250,
	--		axis_areset 			=> srst250,
	--		axis_tvalid_in			=> axis_tvalid_in,
	--		axis_tready_in			=> axis_tready_in,
	--		axis_tdata_in			=> axis_tdata_in,
	--		axis_tuser_in			=> axis_tuser_in,
	--		Metadata_in				=> Metadata_in,
	--		axis_tvalid_out			=> PixelLut_tvalid,
	--		axis_tready_out			=> PixelLut_tready,
	--		axis_tdata_out			=> PixelLut_tdata,
	--		axis_tuser_out			=> PixelLut_tuser,
	--		Metadata_out			=> PixelLut_metadata
	--	);
		
	---- HLS Pixel Threshold (Example)
	--iHlsPixTh : entity work.pix_threshold_wrp
	--	generic map (
	--		DATA_WIDTH 				=> DATA_WIDTH
	--	)
	--	port map (
	--		PipelineClear			=> PipelineClear,
	--		HlsThreshold_bypass		=> PixelThreshold_bypass,
	--		HlsThreshold_level		=> PixelThreshold_level,
	--		axis_aclk	 			=> clk250,
	--		axis_areset 			=> srst250,
	--		axis_tvalid_in			=> PixelLut_tvalid,
	--		axis_tready_in			=> PixelLut_tready,
	--		axis_tdata_in			=> PixelLut_tdata,
	--		axis_tuser_in			=> PixelLut_tuser,
	--		Metadata_in				=> PixelLut_metadata,
	--		axis_tvalid_out			=> HlsPixTh_tvalid,
	--		axis_tready_out			=> HlsPixTh_tready,
	--		axis_tdata_out			=> HlsPixTh_tdata,
	--		axis_tuser_out			=> HlsPixTh_tuser,
	--		Metadata_out			=> HlsPixTh_metadata
	--	);
	
	---- Image Processing Module (Example)
	--iFrame2Line : entity work.frame_to_line
	--	generic map (
	--		DATA_WIDTH 				=> DATA_WIDTH
	--	)
	--	port map (
	--		PipelineClear			=> PipelineClear,
	--		Frame2Line_bypass		=> Frame2Line_bypass,
	--		axis_aclk	 			=> clk250,
	--		axis_areset 			=> srst250,
	--		axis_tvalid_in			=> HlsPixTh_tvalid,
	--		axis_tready_in			=> HlsPixTh_tready,
	--		axis_tdata_in			=> HlsPixTh_tdata,
	--		axis_tuser_in			=> HlsPixTh_tuser,
	--		Metadata_in				=> HlsPixTh_metadata,
	--		axis_tvalid_out			=> axis_tvalid_out,
	--		axis_tready_out			=> axis_tready_out,
	--		axis_tdata_out			=> axis_tdata_out,
	--		axis_tuser_out			=> axis_tuser_out,
	--		Metadata_out			=> Metadata_out
	--	);
		
	-- Read/Write On-Board Memory (Example)
	iMemTrafficGen : entity work.mem_traffic_gen
		generic map (
			DATA_WIDTH 				=> DATA_WIDTH
		)
		port map (
			MemTrafficGen_en	=> MemTrafficGen_en,
			Wraparound_pls		=> Wraparound_pls,
			Wraparound_cnt		=> Wraparound_cnt,
			m_axi_aclk			=> clk250,
			m_axi_areset		=> srst250,
			m_axi_awaddr 		=> m_axi_awaddr,
			m_axi_awlen 		=> m_axi_awlen,
			m_axi_awsize 		=> m_axi_awsize,
			m_axi_awburst 		=> m_axi_awburst,
			m_axi_awlock 		=> m_axi_awlock,
			m_axi_awcache 		=> m_axi_awcache,
			m_axi_awprot 		=> m_axi_awprot,
			m_axi_awqos 		=> m_axi_awqos,
			m_axi_awvalid 		=> m_axi_awvalid,
			m_axi_awready 		=> m_axi_awready,
			m_axi_wdata 		=> m_axi_wdata,
			m_axi_wstrb 		=> m_axi_wstrb,
			m_axi_wlast 		=> m_axi_wlast,
			m_axi_wvalid 		=> m_axi_wvalid,
			m_axi_wready 		=> m_axi_wready,
			m_axi_bresp 		=> m_axi_bresp,
			m_axi_bvalid 		=> m_axi_bvalid,
			m_axi_bready 		=> m_axi_bready,
			m_axi_araddr 		=> m_axi_araddr,
			m_axi_arlen 		=> m_axi_arlen,
			m_axi_arsize 		=> m_axi_arsize,
			m_axi_arburst 		=> m_axi_arburst,
			m_axi_arlock 		=> m_axi_arlock,
			m_axi_arcache 		=> m_axi_arcache,
			m_axi_arprot 		=> m_axi_arprot,
			m_axi_arqos 		=> m_axi_arqos,
			m_axi_arvalid 		=> m_axi_arvalid,
			m_axi_arready 		=> m_axi_arready,
			m_axi_rdata 		=> m_axi_rdata,
			m_axi_rresp 		=> m_axi_rresp,
			m_axi_rlast 		=> m_axi_rlast,
			m_axi_rvalid 		=> m_axi_rvalid,
			m_axi_rready 		=> m_axi_rready
		);
	
	-- Generate CustomLogic events on Memento
	CustomLogic_event 		<= MementoEvent_en or Wraparound_pls;
	CustomLogic_event_arg0	<= MementoEvent_arg0;
	CustomLogic_event_arg1	<= Wraparound_cnt;
	
end behav;
