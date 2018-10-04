	component ghrd_10as066n2_avlmm_pr_freeze_bridge_0 is
		port (
			clock                               : in  std_logic                     := 'X';             -- clk
			freeze_conduit_freeze               : in  std_logic                     := 'X';             -- freeze
			freeze_conduit_illegal_request      : out std_logic;                                        -- illegal_request
			reset_n                             : in  std_logic                     := 'X';             -- reset_n
			slv_bridge_to_pr_read               : out std_logic;                                        -- read
			slv_bridge_to_pr_waitrequest        : in  std_logic                     := 'X';             -- waitrequest
			slv_bridge_to_pr_write              : out std_logic;                                        -- write
			slv_bridge_to_pr_address            : out std_logic_vector(9 downto 0);                     -- address
			slv_bridge_to_pr_byteenable         : out std_logic_vector(3 downto 0);                     -- byteenable
			slv_bridge_to_pr_writedata          : out std_logic_vector(31 downto 0);                    -- writedata
			slv_bridge_to_pr_readdata           : in  std_logic_vector(31 downto 0) := (others => 'X'); -- readdata
			slv_bridge_to_pr_burstcount         : out std_logic_vector(2 downto 0);                     -- burstcount
			slv_bridge_to_pr_readdatavalid      : in  std_logic                     := 'X';             -- readdatavalid
			slv_bridge_to_pr_beginbursttransfer : out std_logic;                                        -- beginbursttransfer
			slv_bridge_to_pr_debugaccess        : out std_logic;                                        -- debugaccess
			slv_bridge_to_pr_response           : in  std_logic_vector(1 downto 0)  := (others => 'X'); -- response
			slv_bridge_to_pr_lock               : out std_logic;                                        -- lock
			slv_bridge_to_pr_writeresponsevalid : in  std_logic                     := 'X';             -- writeresponsevalid
			slv_bridge_to_sr_read               : in  std_logic                     := 'X';             -- read
			slv_bridge_to_sr_waitrequest        : out std_logic;                                        -- waitrequest
			slv_bridge_to_sr_write              : in  std_logic                     := 'X';             -- write
			slv_bridge_to_sr_address            : in  std_logic_vector(9 downto 0)  := (others => 'X'); -- address
			slv_bridge_to_sr_byteenable         : in  std_logic_vector(3 downto 0)  := (others => 'X'); -- byteenable
			slv_bridge_to_sr_writedata          : in  std_logic_vector(31 downto 0) := (others => 'X'); -- writedata
			slv_bridge_to_sr_readdata           : out std_logic_vector(31 downto 0);                    -- readdata
			slv_bridge_to_sr_burstcount         : in  std_logic_vector(2 downto 0)  := (others => 'X'); -- burstcount
			slv_bridge_to_sr_readdatavalid      : out std_logic;                                        -- readdatavalid
			slv_bridge_to_sr_beginbursttransfer : in  std_logic                     := 'X';             -- beginbursttransfer
			slv_bridge_to_sr_debugaccess        : in  std_logic                     := 'X';             -- debugaccess
			slv_bridge_to_sr_response           : out std_logic_vector(1 downto 0);                     -- response
			slv_bridge_to_sr_lock               : in  std_logic                     := 'X';             -- lock
			slv_bridge_to_sr_writeresponsevalid : out std_logic                                         -- writeresponsevalid
		);
	end component ghrd_10as066n2_avlmm_pr_freeze_bridge_0;

	u0 : component ghrd_10as066n2_avlmm_pr_freeze_bridge_0
		port map (
			clock                               => CONNECTED_TO_clock,                               --            clock.clk
			freeze_conduit_freeze               => CONNECTED_TO_freeze_conduit_freeze,               --   freeze_conduit.freeze
			freeze_conduit_illegal_request      => CONNECTED_TO_freeze_conduit_illegal_request,      --                 .illegal_request
			reset_n                             => CONNECTED_TO_reset_n,                             --          reset_n.reset_n
			slv_bridge_to_pr_read               => CONNECTED_TO_slv_bridge_to_pr_read,               -- slv_bridge_to_pr.read
			slv_bridge_to_pr_waitrequest        => CONNECTED_TO_slv_bridge_to_pr_waitrequest,        --                 .waitrequest
			slv_bridge_to_pr_write              => CONNECTED_TO_slv_bridge_to_pr_write,              --                 .write
			slv_bridge_to_pr_address            => CONNECTED_TO_slv_bridge_to_pr_address,            --                 .address
			slv_bridge_to_pr_byteenable         => CONNECTED_TO_slv_bridge_to_pr_byteenable,         --                 .byteenable
			slv_bridge_to_pr_writedata          => CONNECTED_TO_slv_bridge_to_pr_writedata,          --                 .writedata
			slv_bridge_to_pr_readdata           => CONNECTED_TO_slv_bridge_to_pr_readdata,           --                 .readdata
			slv_bridge_to_pr_burstcount         => CONNECTED_TO_slv_bridge_to_pr_burstcount,         --                 .burstcount
			slv_bridge_to_pr_readdatavalid      => CONNECTED_TO_slv_bridge_to_pr_readdatavalid,      --                 .readdatavalid
			slv_bridge_to_pr_beginbursttransfer => CONNECTED_TO_slv_bridge_to_pr_beginbursttransfer, --                 .beginbursttransfer
			slv_bridge_to_pr_debugaccess        => CONNECTED_TO_slv_bridge_to_pr_debugaccess,        --                 .debugaccess
			slv_bridge_to_pr_response           => CONNECTED_TO_slv_bridge_to_pr_response,           --                 .response
			slv_bridge_to_pr_lock               => CONNECTED_TO_slv_bridge_to_pr_lock,               --                 .lock
			slv_bridge_to_pr_writeresponsevalid => CONNECTED_TO_slv_bridge_to_pr_writeresponsevalid, --                 .writeresponsevalid
			slv_bridge_to_sr_read               => CONNECTED_TO_slv_bridge_to_sr_read,               -- slv_bridge_to_sr.read
			slv_bridge_to_sr_waitrequest        => CONNECTED_TO_slv_bridge_to_sr_waitrequest,        --                 .waitrequest
			slv_bridge_to_sr_write              => CONNECTED_TO_slv_bridge_to_sr_write,              --                 .write
			slv_bridge_to_sr_address            => CONNECTED_TO_slv_bridge_to_sr_address,            --                 .address
			slv_bridge_to_sr_byteenable         => CONNECTED_TO_slv_bridge_to_sr_byteenable,         --                 .byteenable
			slv_bridge_to_sr_writedata          => CONNECTED_TO_slv_bridge_to_sr_writedata,          --                 .writedata
			slv_bridge_to_sr_readdata           => CONNECTED_TO_slv_bridge_to_sr_readdata,           --                 .readdata
			slv_bridge_to_sr_burstcount         => CONNECTED_TO_slv_bridge_to_sr_burstcount,         --                 .burstcount
			slv_bridge_to_sr_readdatavalid      => CONNECTED_TO_slv_bridge_to_sr_readdatavalid,      --                 .readdatavalid
			slv_bridge_to_sr_beginbursttransfer => CONNECTED_TO_slv_bridge_to_sr_beginbursttransfer, --                 .beginbursttransfer
			slv_bridge_to_sr_debugaccess        => CONNECTED_TO_slv_bridge_to_sr_debugaccess,        --                 .debugaccess
			slv_bridge_to_sr_response           => CONNECTED_TO_slv_bridge_to_sr_response,           --                 .response
			slv_bridge_to_sr_lock               => CONNECTED_TO_slv_bridge_to_sr_lock,               --                 .lock
			slv_bridge_to_sr_writeresponsevalid => CONNECTED_TO_slv_bridge_to_sr_writeresponsevalid  --                 .writeresponsevalid
		);

