	component ghrd_10as066n2_pr_region_controller_0 is
		port (
			avl_csr_read                   : in  std_logic                     := 'X';             -- read
			avl_csr_write                  : in  std_logic                     := 'X';             -- write
			avl_csr_address                : in  std_logic_vector(1 downto 0)  := (others => 'X'); -- address
			avl_csr_writedata              : in  std_logic_vector(31 downto 0) := (others => 'X'); -- writedata
			avl_csr_readdata               : out std_logic_vector(31 downto 0);                    -- readdata
			bridge_freeze0_freeze          : out std_logic;                                        -- freeze
			bridge_freeze0_illegal_request : in  std_logic                     := 'X';             -- illegal_request
			bridge_freeze1_freeze          : out std_logic;                                        -- freeze
			bridge_freeze1_illegal_request : in  std_logic                     := 'X';             -- illegal_request
			clock_clk                      : in  std_logic                     := 'X';             -- clk
			pr_handshake_start_req         : out std_logic;                                        -- start_req
			pr_handshake_start_ack         : in  std_logic                     := 'X';             -- start_ack
			pr_handshake_stop_req          : out std_logic;                                        -- stop_req
			pr_handshake_stop_ack          : in  std_logic                     := 'X';             -- stop_ack
			reset_reset                    : in  std_logic                     := 'X';             -- reset
			reset_source_reset             : out std_logic                                         -- reset
		);
	end component ghrd_10as066n2_pr_region_controller_0;

	u0 : component ghrd_10as066n2_pr_region_controller_0
		port map (
			avl_csr_read                   => CONNECTED_TO_avl_csr_read,                   --        avl_csr.read
			avl_csr_write                  => CONNECTED_TO_avl_csr_write,                  --               .write
			avl_csr_address                => CONNECTED_TO_avl_csr_address,                --               .address
			avl_csr_writedata              => CONNECTED_TO_avl_csr_writedata,              --               .writedata
			avl_csr_readdata               => CONNECTED_TO_avl_csr_readdata,               --               .readdata
			bridge_freeze0_freeze          => CONNECTED_TO_bridge_freeze0_freeze,          -- bridge_freeze0.freeze
			bridge_freeze0_illegal_request => CONNECTED_TO_bridge_freeze0_illegal_request, --               .illegal_request
			bridge_freeze1_freeze          => CONNECTED_TO_bridge_freeze1_freeze,          -- bridge_freeze1.freeze
			bridge_freeze1_illegal_request => CONNECTED_TO_bridge_freeze1_illegal_request, --               .illegal_request
			clock_clk                      => CONNECTED_TO_clock_clk,                      --          clock.clk
			pr_handshake_start_req         => CONNECTED_TO_pr_handshake_start_req,         --   pr_handshake.start_req
			pr_handshake_start_ack         => CONNECTED_TO_pr_handshake_start_ack,         --               .start_ack
			pr_handshake_stop_req          => CONNECTED_TO_pr_handshake_stop_req,          --               .stop_req
			pr_handshake_stop_ack          => CONNECTED_TO_pr_handshake_stop_ack,          --               .stop_ack
			reset_reset                    => CONNECTED_TO_reset_reset,                    --          reset.reset
			reset_source_reset             => CONNECTED_TO_reset_source_reset              --   reset_source.reset
		);

