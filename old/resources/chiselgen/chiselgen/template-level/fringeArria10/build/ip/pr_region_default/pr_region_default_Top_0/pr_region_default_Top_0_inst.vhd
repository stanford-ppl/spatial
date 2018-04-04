	component pr_region_default_Top_0 is
		port (
			clock                  : in  std_logic                      := 'X';             -- clk
			io_M_AXI_0_AWID        : out std_logic_vector(5 downto 0);                      -- awid
			io_M_AXI_0_AWUSER      : out std_logic_vector(31 downto 0);                     -- awuser
			io_M_AXI_0_AWADDR      : out std_logic_vector(31 downto 0);                     -- awaddr
			io_M_AXI_0_AWLEN       : out std_logic_vector(7 downto 0);                      -- awlen
			io_M_AXI_0_AWSIZE      : out std_logic_vector(2 downto 0);                      -- awsize
			io_M_AXI_0_AWBURST     : out std_logic_vector(1 downto 0);                      -- awburst
			io_M_AXI_0_AWLOCK      : out std_logic;                                         -- awlock
			io_M_AXI_0_AWCACHE     : out std_logic_vector(3 downto 0);                      -- awcache
			io_M_AXI_0_AWPROT      : out std_logic_vector(2 downto 0);                      -- awprot
			io_M_AXI_0_AWQOS       : out std_logic_vector(3 downto 0);                      -- awqos
			io_M_AXI_0_AWVALID     : out std_logic;                                         -- awvalid
			io_M_AXI_0_AWREADY     : in  std_logic                      := 'X';             -- awready
			io_M_AXI_0_ARID        : out std_logic_vector(5 downto 0);                      -- arid
			io_M_AXI_0_ARUSER      : out std_logic_vector(31 downto 0);                     -- aruser
			io_M_AXI_0_ARADDR      : out std_logic_vector(31 downto 0);                     -- araddr
			io_M_AXI_0_ARLEN       : out std_logic_vector(7 downto 0);                      -- arlen
			io_M_AXI_0_ARSIZE      : out std_logic_vector(2 downto 0);                      -- arsize
			io_M_AXI_0_ARBURST     : out std_logic_vector(1 downto 0);                      -- arburst
			io_M_AXI_0_ARLOCK      : out std_logic;                                         -- arlock
			io_M_AXI_0_ARCACHE     : out std_logic_vector(3 downto 0);                      -- arcache
			io_M_AXI_0_ARPROT      : out std_logic_vector(2 downto 0);                      -- arprot
			io_M_AXI_0_ARQOS       : out std_logic_vector(3 downto 0);                      -- arqos
			io_M_AXI_0_ARVALID     : out std_logic;                                         -- arvalid
			io_M_AXI_0_ARREADY     : in  std_logic                      := 'X';             -- arready
			io_M_AXI_0_WDATA       : out std_logic_vector(511 downto 0);                    -- wdata
			io_M_AXI_0_WSTRB       : out std_logic_vector(63 downto 0);                     -- wstrb
			io_M_AXI_0_WLAST       : out std_logic;                                         -- wlast
			io_M_AXI_0_WVALID      : out std_logic;                                         -- wvalid
			io_M_AXI_0_WREADY      : in  std_logic                      := 'X';             -- wready
			io_M_AXI_0_RID         : in  std_logic_vector(5 downto 0)   := (others => 'X'); -- rid
			io_M_AXI_0_RUSER       : in  std_logic_vector(31 downto 0)  := (others => 'X'); -- ruser
			io_M_AXI_0_RDATA       : in  std_logic_vector(511 downto 0) := (others => 'X'); -- rdata
			io_M_AXI_0_RRESP       : in  std_logic_vector(1 downto 0)   := (others => 'X'); -- rresp
			io_M_AXI_0_RLAST       : in  std_logic                      := 'X';             -- rlast
			io_M_AXI_0_RVALID      : in  std_logic                      := 'X';             -- rvalid
			io_M_AXI_0_RREADY      : out std_logic;                                         -- rready
			io_M_AXI_0_BID         : in  std_logic_vector(5 downto 0)   := (others => 'X'); -- bid
			io_M_AXI_0_BUSER       : in  std_logic_vector(31 downto 0)  := (others => 'X'); -- buser
			io_M_AXI_0_BRESP       : in  std_logic_vector(1 downto 0)   := (others => 'X'); -- bresp
			io_M_AXI_0_BVALID      : in  std_logic                      := 'X';             -- bvalid
			io_M_AXI_0_BREADY      : out std_logic;                                         -- bready
			io_S_AVALON_address    : in  std_logic_vector(6 downto 0)   := (others => 'X'); -- address
			io_S_AVALON_readdata   : out std_logic_vector(31 downto 0);                     -- readdata
			io_S_AVALON_chipselect : in  std_logic                      := 'X';             -- chipselect
			io_S_AVALON_write      : in  std_logic                      := 'X';             -- write
			io_S_AVALON_read       : in  std_logic                      := 'X';             -- read
			io_S_AVALON_writedata  : in  std_logic_vector(31 downto 0)  := (others => 'X'); -- writedata
			reset                  : in  std_logic                      := 'X'              -- reset
		);
	end component pr_region_default_Top_0;

	u0 : component pr_region_default_Top_0
		port map (
			clock                  => CONNECTED_TO_clock,                  --       clock.clk
			io_M_AXI_0_AWID        => CONNECTED_TO_io_M_AXI_0_AWID,        --  io_M_AXI_0.awid
			io_M_AXI_0_AWUSER      => CONNECTED_TO_io_M_AXI_0_AWUSER,      --            .awuser
			io_M_AXI_0_AWADDR      => CONNECTED_TO_io_M_AXI_0_AWADDR,      --            .awaddr
			io_M_AXI_0_AWLEN       => CONNECTED_TO_io_M_AXI_0_AWLEN,       --            .awlen
			io_M_AXI_0_AWSIZE      => CONNECTED_TO_io_M_AXI_0_AWSIZE,      --            .awsize
			io_M_AXI_0_AWBURST     => CONNECTED_TO_io_M_AXI_0_AWBURST,     --            .awburst
			io_M_AXI_0_AWLOCK      => CONNECTED_TO_io_M_AXI_0_AWLOCK,      --            .awlock
			io_M_AXI_0_AWCACHE     => CONNECTED_TO_io_M_AXI_0_AWCACHE,     --            .awcache
			io_M_AXI_0_AWPROT      => CONNECTED_TO_io_M_AXI_0_AWPROT,      --            .awprot
			io_M_AXI_0_AWQOS       => CONNECTED_TO_io_M_AXI_0_AWQOS,       --            .awqos
			io_M_AXI_0_AWVALID     => CONNECTED_TO_io_M_AXI_0_AWVALID,     --            .awvalid
			io_M_AXI_0_AWREADY     => CONNECTED_TO_io_M_AXI_0_AWREADY,     --            .awready
			io_M_AXI_0_ARID        => CONNECTED_TO_io_M_AXI_0_ARID,        --            .arid
			io_M_AXI_0_ARUSER      => CONNECTED_TO_io_M_AXI_0_ARUSER,      --            .aruser
			io_M_AXI_0_ARADDR      => CONNECTED_TO_io_M_AXI_0_ARADDR,      --            .araddr
			io_M_AXI_0_ARLEN       => CONNECTED_TO_io_M_AXI_0_ARLEN,       --            .arlen
			io_M_AXI_0_ARSIZE      => CONNECTED_TO_io_M_AXI_0_ARSIZE,      --            .arsize
			io_M_AXI_0_ARBURST     => CONNECTED_TO_io_M_AXI_0_ARBURST,     --            .arburst
			io_M_AXI_0_ARLOCK      => CONNECTED_TO_io_M_AXI_0_ARLOCK,      --            .arlock
			io_M_AXI_0_ARCACHE     => CONNECTED_TO_io_M_AXI_0_ARCACHE,     --            .arcache
			io_M_AXI_0_ARPROT      => CONNECTED_TO_io_M_AXI_0_ARPROT,      --            .arprot
			io_M_AXI_0_ARQOS       => CONNECTED_TO_io_M_AXI_0_ARQOS,       --            .arqos
			io_M_AXI_0_ARVALID     => CONNECTED_TO_io_M_AXI_0_ARVALID,     --            .arvalid
			io_M_AXI_0_ARREADY     => CONNECTED_TO_io_M_AXI_0_ARREADY,     --            .arready
			io_M_AXI_0_WDATA       => CONNECTED_TO_io_M_AXI_0_WDATA,       --            .wdata
			io_M_AXI_0_WSTRB       => CONNECTED_TO_io_M_AXI_0_WSTRB,       --            .wstrb
			io_M_AXI_0_WLAST       => CONNECTED_TO_io_M_AXI_0_WLAST,       --            .wlast
			io_M_AXI_0_WVALID      => CONNECTED_TO_io_M_AXI_0_WVALID,      --            .wvalid
			io_M_AXI_0_WREADY      => CONNECTED_TO_io_M_AXI_0_WREADY,      --            .wready
			io_M_AXI_0_RID         => CONNECTED_TO_io_M_AXI_0_RID,         --            .rid
			io_M_AXI_0_RUSER       => CONNECTED_TO_io_M_AXI_0_RUSER,       --            .ruser
			io_M_AXI_0_RDATA       => CONNECTED_TO_io_M_AXI_0_RDATA,       --            .rdata
			io_M_AXI_0_RRESP       => CONNECTED_TO_io_M_AXI_0_RRESP,       --            .rresp
			io_M_AXI_0_RLAST       => CONNECTED_TO_io_M_AXI_0_RLAST,       --            .rlast
			io_M_AXI_0_RVALID      => CONNECTED_TO_io_M_AXI_0_RVALID,      --            .rvalid
			io_M_AXI_0_RREADY      => CONNECTED_TO_io_M_AXI_0_RREADY,      --            .rready
			io_M_AXI_0_BID         => CONNECTED_TO_io_M_AXI_0_BID,         --            .bid
			io_M_AXI_0_BUSER       => CONNECTED_TO_io_M_AXI_0_BUSER,       --            .buser
			io_M_AXI_0_BRESP       => CONNECTED_TO_io_M_AXI_0_BRESP,       --            .bresp
			io_M_AXI_0_BVALID      => CONNECTED_TO_io_M_AXI_0_BVALID,      --            .bvalid
			io_M_AXI_0_BREADY      => CONNECTED_TO_io_M_AXI_0_BREADY,      --            .bready
			io_S_AVALON_address    => CONNECTED_TO_io_S_AVALON_address,    -- io_S_AVALON.address
			io_S_AVALON_readdata   => CONNECTED_TO_io_S_AVALON_readdata,   --            .readdata
			io_S_AVALON_chipselect => CONNECTED_TO_io_S_AVALON_chipselect, --            .chipselect
			io_S_AVALON_write      => CONNECTED_TO_io_S_AVALON_write,      --            .write
			io_S_AVALON_read       => CONNECTED_TO_io_S_AVALON_read,       --            .read
			io_S_AVALON_writedata  => CONNECTED_TO_io_S_AVALON_writedata,  --            .writedata
			reset                  => CONNECTED_TO_reset                   --       reset.reset
		);

