	component pr_region_default_onchip_memory2_0 is
		port (
			clk        : in  std_logic                     := 'X';             -- clk
			reset      : in  std_logic                     := 'X';             -- reset
			reset_req  : in  std_logic                     := 'X';             -- reset_req
			address    : in  std_logic_vector(6 downto 0)  := (others => 'X'); -- address
			clken      : in  std_logic                     := 'X';             -- clken
			chipselect : in  std_logic                     := 'X';             -- chipselect
			write      : in  std_logic                     := 'X';             -- write
			readdata   : out std_logic_vector(31 downto 0);                    -- readdata
			writedata  : in  std_logic_vector(31 downto 0) := (others => 'X'); -- writedata
			byteenable : in  std_logic_vector(3 downto 0)  := (others => 'X')  -- byteenable
		);
	end component pr_region_default_onchip_memory2_0;

	u0 : component pr_region_default_onchip_memory2_0
		port map (
			clk        => CONNECTED_TO_clk,        --   clk1.clk
			reset      => CONNECTED_TO_reset,      -- reset1.reset
			reset_req  => CONNECTED_TO_reset_req,  --       .reset_req
			address    => CONNECTED_TO_address,    --     s1.address
			clken      => CONNECTED_TO_clken,      --       .clken
			chipselect => CONNECTED_TO_chipselect, --       .chipselect
			write      => CONNECTED_TO_write,      --       .write
			readdata   => CONNECTED_TO_readdata,   --       .readdata
			writedata  => CONNECTED_TO_writedata,  --       .writedata
			byteenable => CONNECTED_TO_byteenable  --       .byteenable
		);

