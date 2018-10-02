	component ghrd_10as066n2_avlmm_pr_freeze_bridge_1 is
		port (
			clock                               : in  std_logic                     := 'X';             -- clk
			freeze_conduit_freeze               : in  std_logic                     := 'X';             -- freeze
			freeze_conduit_illegal_request      : out std_logic;                                        -- illegal_request
			mst_bridge_to_pr_read               : in  std_logic                     := 'X';             -- read
			mst_bridge_to_pr_waitrequest        : out std_logic;                                        -- waitrequest
			mst_bridge_to_pr_write              : in  std_logic                     := 'X';             -- write
			mst_bridge_to_pr_address            : in  std_logic_vector(31 downto 0) := (others => 'X'); -- address
			mst_bridge_to_pr_byteenable         : in  std_logic_vector(3 downto 0)  := (others => 'X'); -- byteenable
			mst_bridge_to_pr_writedata          : in  std_logic_vector(31 downto 0) := (others => 'X'); -- writedata
			mst_bridge_to_pr_readdata           : out std_logic_vector(31 downto 0);                    -- readdata
			mst_bridge_to_pr_burstcount         : in  std_logic_vector(2 downto 0)  := (others => 'X'); -- burstcount
			mst_bridge_to_pr_readdatavalid      : out std_logic;                                        -- readdatavalid
			mst_bridge_to_pr_beginbursttransfer : in  std_logic                     := 'X';             -- beginbursttransfer
			mst_bridge_to_pr_debugaccess        : in  std_logic                     := 'X';             -- debugaccess
			mst_bridge_to_pr_response           : out std_logic_vector(1 downto 0);                     -- response
			mst_bridge_to_pr_lock               : in  std_logic                     := 'X';             -- lock
			mst_bridge_to_pr_writeresponsevalid : out std_logic;                                        -- writeresponsevalid
			mst_bridge_to_sr_read               : out std_logic;                                        -- read
			mst_bridge_to_sr_waitrequest        : in  std_logic                     := 'X';             -- waitrequest
			mst_bridge_to_sr_write              : out std_logic;                                        -- write
			mst_bridge_to_sr_address            : out std_logic_vector(31 downto 0);                    -- address
			mst_bridge_to_sr_byteenable         : out std_logic_vector(3 downto 0);                     -- byteenable
			mst_bridge_to_sr_writedata          : out std_logic_vector(31 downto 0);                    -- writedata
			mst_bridge_to_sr_readdata           : in  std_logic_vector(31 downto 0) := (others => 'X'); -- readdata
			mst_bridge_to_sr_burstcount         : out std_logic_vector(2 downto 0);                     -- burstcount
			mst_bridge_to_sr_readdatavalid      : in  std_logic                     := 'X';             -- readdatavalid
			mst_bridge_to_sr_beginbursttransfer : out std_logic;                                        -- beginbursttransfer
			mst_bridge_to_sr_debugaccess        : out std_logic;                                        -- debugaccess
			mst_bridge_to_sr_response           : in  std_logic_vector(1 downto 0)  := (others => 'X'); -- response
			mst_bridge_to_sr_lock               : out std_logic;                                        -- lock
			mst_bridge_to_sr_writeresponsevalid : in  std_logic                     := 'X';             -- writeresponsevalid
			reset_n                             : in  std_logic                     := 'X'              -- reset_n
		);
	end component ghrd_10as066n2_avlmm_pr_freeze_bridge_1;

	u0 : component ghrd_10as066n2_avlmm_pr_freeze_bridge_1
		port map (
			clock                               => CONNECTED_TO_clock,                               --            clock.clk
			freeze_conduit_freeze               => CONNECTED_TO_freeze_conduit_freeze,               --   freeze_conduit.freeze
			freeze_conduit_illegal_request      => CONNECTED_TO_freeze_conduit_illegal_request,      --                 .illegal_request
			mst_bridge_to_pr_read               => CONNECTED_TO_mst_bridge_to_pr_read,               -- mst_bridge_to_pr.read
			mst_bridge_to_pr_waitrequest        => CONNECTED_TO_mst_bridge_to_pr_waitrequest,        --                 .waitrequest
			mst_bridge_to_pr_write              => CONNECTED_TO_mst_bridge_to_pr_write,              --                 .write
			mst_bridge_to_pr_address            => CONNECTED_TO_mst_bridge_to_pr_address,            --                 .address
			mst_bridge_to_pr_byteenable         => CONNECTED_TO_mst_bridge_to_pr_byteenable,         --                 .byteenable
			mst_bridge_to_pr_writedata          => CONNECTED_TO_mst_bridge_to_pr_writedata,          --                 .writedata
			mst_bridge_to_pr_readdata           => CONNECTED_TO_mst_bridge_to_pr_readdata,           --                 .readdata
			mst_bridge_to_pr_burstcount         => CONNECTED_TO_mst_bridge_to_pr_burstcount,         --                 .burstcount
			mst_bridge_to_pr_readdatavalid      => CONNECTED_TO_mst_bridge_to_pr_readdatavalid,      --                 .readdatavalid
			mst_bridge_to_pr_beginbursttransfer => CONNECTED_TO_mst_bridge_to_pr_beginbursttransfer, --                 .beginbursttransfer
			mst_bridge_to_pr_debugaccess        => CONNECTED_TO_mst_bridge_to_pr_debugaccess,        --                 .debugaccess
			mst_bridge_to_pr_response           => CONNECTED_TO_mst_bridge_to_pr_response,           --                 .response
			mst_bridge_to_pr_lock               => CONNECTED_TO_mst_bridge_to_pr_lock,               --                 .lock
			mst_bridge_to_pr_writeresponsevalid => CONNECTED_TO_mst_bridge_to_pr_writeresponsevalid, --                 .writeresponsevalid
			mst_bridge_to_sr_read               => CONNECTED_TO_mst_bridge_to_sr_read,               -- mst_bridge_to_sr.read
			mst_bridge_to_sr_waitrequest        => CONNECTED_TO_mst_bridge_to_sr_waitrequest,        --                 .waitrequest
			mst_bridge_to_sr_write              => CONNECTED_TO_mst_bridge_to_sr_write,              --                 .write
			mst_bridge_to_sr_address            => CONNECTED_TO_mst_bridge_to_sr_address,            --                 .address
			mst_bridge_to_sr_byteenable         => CONNECTED_TO_mst_bridge_to_sr_byteenable,         --                 .byteenable
			mst_bridge_to_sr_writedata          => CONNECTED_TO_mst_bridge_to_sr_writedata,          --                 .writedata
			mst_bridge_to_sr_readdata           => CONNECTED_TO_mst_bridge_to_sr_readdata,           --                 .readdata
			mst_bridge_to_sr_burstcount         => CONNECTED_TO_mst_bridge_to_sr_burstcount,         --                 .burstcount
			mst_bridge_to_sr_readdatavalid      => CONNECTED_TO_mst_bridge_to_sr_readdatavalid,      --                 .readdatavalid
			mst_bridge_to_sr_beginbursttransfer => CONNECTED_TO_mst_bridge_to_sr_beginbursttransfer, --                 .beginbursttransfer
			mst_bridge_to_sr_debugaccess        => CONNECTED_TO_mst_bridge_to_sr_debugaccess,        --                 .debugaccess
			mst_bridge_to_sr_response           => CONNECTED_TO_mst_bridge_to_sr_response,           --                 .response
			mst_bridge_to_sr_lock               => CONNECTED_TO_mst_bridge_to_sr_lock,               --                 .lock
			mst_bridge_to_sr_writeresponsevalid => CONNECTED_TO_mst_bridge_to_sr_writeresponsevalid, --                 .writeresponsevalid
			reset_n                             => CONNECTED_TO_reset_n                              --          reset_n.reset_n
		);

