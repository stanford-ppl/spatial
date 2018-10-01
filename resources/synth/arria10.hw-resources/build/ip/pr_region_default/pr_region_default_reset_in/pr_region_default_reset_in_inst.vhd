	component pr_region_default_reset_in is
		port (
			clk       : in  std_logic := 'X'; -- clk
			in_reset  : in  std_logic := 'X'; -- reset
			out_reset : out std_logic         -- reset
		);
	end component pr_region_default_reset_in;

	u0 : component pr_region_default_reset_in
		port map (
			clk       => CONNECTED_TO_clk,       --       clk.clk
			in_reset  => CONNECTED_TO_in_reset,  --  in_reset.reset
			out_reset => CONNECTED_TO_out_reset  -- out_reset.reset
		);

