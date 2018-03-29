	component ghrd_10as066n2_issp_0 is
		port (
			source_clk : in  std_logic                    := 'X'; -- clk
			source     : out std_logic_vector(2 downto 0)         -- source
		);
	end component ghrd_10as066n2_issp_0;

	u0 : component ghrd_10as066n2_issp_0
		port map (
			source_clk => CONNECTED_TO_source_clk, -- source_clk.clk
			source     => CONNECTED_TO_source      --    sources.source
		);

