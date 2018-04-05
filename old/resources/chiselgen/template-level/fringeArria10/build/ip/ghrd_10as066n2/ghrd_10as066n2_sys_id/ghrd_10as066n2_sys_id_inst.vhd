	component ghrd_10as066n2_sys_id is
		port (
			clock    : in  std_logic                     := 'X'; -- clk
			readdata : out std_logic_vector(31 downto 0);        -- readdata
			address  : in  std_logic                     := 'X'; -- address
			reset_n  : in  std_logic                     := 'X'  -- reset_n
		);
	end component ghrd_10as066n2_sys_id;

	u0 : component ghrd_10as066n2_sys_id
		port map (
			clock    => CONNECTED_TO_clock,    --           clk.clk
			readdata => CONNECTED_TO_readdata, -- control_slave.readdata
			address  => CONNECTED_TO_address,  --              .address
			reset_n  => CONNECTED_TO_reset_n   --         reset.reset_n
		);

