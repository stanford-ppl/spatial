	component ghrd_10as066n2_dipsw_pio is
		port (
			clk        : in  std_logic                     := 'X';             -- clk
			in_port    : in  std_logic_vector(3 downto 0)  := (others => 'X'); -- export
			irq        : out std_logic;                                        -- irq
			reset_n    : in  std_logic                     := 'X';             -- reset_n
			address    : in  std_logic_vector(1 downto 0)  := (others => 'X'); -- address
			write_n    : in  std_logic                     := 'X';             -- write_n
			writedata  : in  std_logic_vector(31 downto 0) := (others => 'X'); -- writedata
			chipselect : in  std_logic                     := 'X';             -- chipselect
			readdata   : out std_logic_vector(31 downto 0)                     -- readdata
		);
	end component ghrd_10as066n2_dipsw_pio;

	u0 : component ghrd_10as066n2_dipsw_pio
		port map (
			clk        => CONNECTED_TO_clk,        --                 clk.clk
			in_port    => CONNECTED_TO_in_port,    -- external_connection.export
			irq        => CONNECTED_TO_irq,        --                 irq.irq
			reset_n    => CONNECTED_TO_reset_n,    --               reset.reset_n
			address    => CONNECTED_TO_address,    --                  s1.address
			write_n    => CONNECTED_TO_write_n,    --                    .write_n
			writedata  => CONNECTED_TO_writedata,  --                    .writedata
			chipselect => CONNECTED_TO_chipselect, --                    .chipselect
			readdata   => CONNECTED_TO_readdata    --                    .readdata
		);

