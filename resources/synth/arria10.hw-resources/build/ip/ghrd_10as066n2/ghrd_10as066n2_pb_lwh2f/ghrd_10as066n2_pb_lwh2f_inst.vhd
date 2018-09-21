	component ghrd_10as066n2_pb_lwh2f is
		generic (
			DATA_WIDTH        : integer := 32;
			SYMBOL_WIDTH      : integer := 8;
			HDL_ADDR_WIDTH    : integer := 10;
			BURSTCOUNT_WIDTH  : integer := 1;
			PIPELINE_COMMAND  : integer := 1;
			PIPELINE_RESPONSE : integer := 1
		);
		port (
			clk              : in  std_logic                                     := 'X';             -- clk
			m0_waitrequest   : in  std_logic                                     := 'X';             -- waitrequest
			m0_readdata      : in  std_logic_vector(DATA_WIDTH-1 downto 0)       := (others => 'X'); -- readdata
			m0_readdatavalid : in  std_logic                                     := 'X';             -- readdatavalid
			m0_burstcount    : out std_logic_vector(BURSTCOUNT_WIDTH-1 downto 0);                    -- burstcount
			m0_writedata     : out std_logic_vector(DATA_WIDTH-1 downto 0);                          -- writedata
			m0_address       : out std_logic_vector(HDL_ADDR_WIDTH-1 downto 0);                      -- address
			m0_write         : out std_logic;                                                        -- write
			m0_read          : out std_logic;                                                        -- read
			m0_byteenable    : out std_logic_vector(3 downto 0);                                     -- byteenable
			m0_debugaccess   : out std_logic;                                                        -- debugaccess
			reset            : in  std_logic                                     := 'X';             -- reset
			s0_waitrequest   : out std_logic;                                                        -- waitrequest
			s0_readdata      : out std_logic_vector(DATA_WIDTH-1 downto 0);                          -- readdata
			s0_readdatavalid : out std_logic;                                                        -- readdatavalid
			s0_burstcount    : in  std_logic_vector(BURSTCOUNT_WIDTH-1 downto 0) := (others => 'X'); -- burstcount
			s0_writedata     : in  std_logic_vector(DATA_WIDTH-1 downto 0)       := (others => 'X'); -- writedata
			s0_address       : in  std_logic_vector(HDL_ADDR_WIDTH-1 downto 0)   := (others => 'X'); -- address
			s0_write         : in  std_logic                                     := 'X';             -- write
			s0_read          : in  std_logic                                     := 'X';             -- read
			s0_byteenable    : in  std_logic_vector(3 downto 0)                  := (others => 'X'); -- byteenable
			s0_debugaccess   : in  std_logic                                     := 'X'              -- debugaccess
		);
	end component ghrd_10as066n2_pb_lwh2f;

	u0 : component ghrd_10as066n2_pb_lwh2f
		generic map (
			DATA_WIDTH        => INTEGER_VALUE_FOR_DATA_WIDTH,
			SYMBOL_WIDTH      => INTEGER_VALUE_FOR_SYMBOL_WIDTH,
			HDL_ADDR_WIDTH    => INTEGER_VALUE_FOR_HDL_ADDR_WIDTH,
			BURSTCOUNT_WIDTH  => INTEGER_VALUE_FOR_BURSTCOUNT_WIDTH,
			PIPELINE_COMMAND  => INTEGER_VALUE_FOR_PIPELINE_COMMAND,
			PIPELINE_RESPONSE => INTEGER_VALUE_FOR_PIPELINE_RESPONSE
		)
		port map (
			clk              => CONNECTED_TO_clk,              --   clk.clk
			m0_waitrequest   => CONNECTED_TO_m0_waitrequest,   --    m0.waitrequest
			m0_readdata      => CONNECTED_TO_m0_readdata,      --      .readdata
			m0_readdatavalid => CONNECTED_TO_m0_readdatavalid, --      .readdatavalid
			m0_burstcount    => CONNECTED_TO_m0_burstcount,    --      .burstcount
			m0_writedata     => CONNECTED_TO_m0_writedata,     --      .writedata
			m0_address       => CONNECTED_TO_m0_address,       --      .address
			m0_write         => CONNECTED_TO_m0_write,         --      .write
			m0_read          => CONNECTED_TO_m0_read,          --      .read
			m0_byteenable    => CONNECTED_TO_m0_byteenable,    --      .byteenable
			m0_debugaccess   => CONNECTED_TO_m0_debugaccess,   --      .debugaccess
			reset            => CONNECTED_TO_reset,            -- reset.reset
			s0_waitrequest   => CONNECTED_TO_s0_waitrequest,   --    s0.waitrequest
			s0_readdata      => CONNECTED_TO_s0_readdata,      --      .readdata
			s0_readdatavalid => CONNECTED_TO_s0_readdatavalid, --      .readdatavalid
			s0_burstcount    => CONNECTED_TO_s0_burstcount,    --      .burstcount
			s0_writedata     => CONNECTED_TO_s0_writedata,     --      .writedata
			s0_address       => CONNECTED_TO_s0_address,       --      .address
			s0_write         => CONNECTED_TO_s0_write,         --      .write
			s0_read          => CONNECTED_TO_s0_read,          --      .read
			s0_byteenable    => CONNECTED_TO_s0_byteenable,    --      .byteenable
			s0_debugaccess   => CONNECTED_TO_s0_debugaccess    --      .debugaccess
		);

