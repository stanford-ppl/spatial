	component ghrd_10as066n2_emif_hps is
		port (
			global_reset_n : in    std_logic                       := 'X';             -- reset_n
			hps_to_emif    : in    std_logic_vector(4095 downto 0) := (others => 'X'); -- hps_to_emif
			emif_to_hps    : out   std_logic_vector(4095 downto 0);                    -- emif_to_hps
			hps_to_emif_gp : in    std_logic_vector(1 downto 0)    := (others => 'X'); -- gp_to_emif
			emif_to_hps_gp : out   std_logic_vector(0 downto 0);                       -- emif_to_gp
			mem_ck         : out   std_logic_vector(0 downto 0);                       -- mem_ck
			mem_ck_n       : out   std_logic_vector(0 downto 0);                       -- mem_ck_n
			mem_a          : out   std_logic_vector(16 downto 0);                      -- mem_a
			mem_act_n      : out   std_logic_vector(0 downto 0);                       -- mem_act_n
			mem_ba         : out   std_logic_vector(1 downto 0);                       -- mem_ba
			mem_bg         : out   std_logic_vector(0 downto 0);                       -- mem_bg
			mem_cke        : out   std_logic_vector(0 downto 0);                       -- mem_cke
			mem_cs_n       : out   std_logic_vector(0 downto 0);                       -- mem_cs_n
			mem_odt        : out   std_logic_vector(0 downto 0);                       -- mem_odt
			mem_reset_n    : out   std_logic_vector(0 downto 0);                       -- mem_reset_n
			mem_par        : out   std_logic_vector(0 downto 0);                       -- mem_par
			mem_alert_n    : in    std_logic_vector(0 downto 0)    := (others => 'X'); -- mem_alert_n
			mem_dqs        : inout std_logic_vector(3 downto 0)    := (others => 'X'); -- mem_dqs
			mem_dqs_n      : inout std_logic_vector(3 downto 0)    := (others => 'X'); -- mem_dqs_n
			mem_dq         : inout std_logic_vector(31 downto 0)   := (others => 'X'); -- mem_dq
			mem_dbi_n      : inout std_logic_vector(3 downto 0)    := (others => 'X'); -- mem_dbi_n
			oct_rzqin      : in    std_logic                       := 'X';             -- oct_rzqin
			pll_ref_clk    : in    std_logic                       := 'X'              -- clk
		);
	end component ghrd_10as066n2_emif_hps;

	u0 : component ghrd_10as066n2_emif_hps
		port map (
			global_reset_n => CONNECTED_TO_global_reset_n, -- global_reset_reset_sink.reset_n
			hps_to_emif    => CONNECTED_TO_hps_to_emif,    --    hps_emif_conduit_end.hps_to_emif
			emif_to_hps    => CONNECTED_TO_emif_to_hps,    --                        .emif_to_hps
			hps_to_emif_gp => CONNECTED_TO_hps_to_emif_gp, --                        .gp_to_emif
			emif_to_hps_gp => CONNECTED_TO_emif_to_hps_gp, --                        .emif_to_gp
			mem_ck         => CONNECTED_TO_mem_ck,         --         mem_conduit_end.mem_ck
			mem_ck_n       => CONNECTED_TO_mem_ck_n,       --                        .mem_ck_n
			mem_a          => CONNECTED_TO_mem_a,          --                        .mem_a
			mem_act_n      => CONNECTED_TO_mem_act_n,      --                        .mem_act_n
			mem_ba         => CONNECTED_TO_mem_ba,         --                        .mem_ba
			mem_bg         => CONNECTED_TO_mem_bg,         --                        .mem_bg
			mem_cke        => CONNECTED_TO_mem_cke,        --                        .mem_cke
			mem_cs_n       => CONNECTED_TO_mem_cs_n,       --                        .mem_cs_n
			mem_odt        => CONNECTED_TO_mem_odt,        --                        .mem_odt
			mem_reset_n    => CONNECTED_TO_mem_reset_n,    --                        .mem_reset_n
			mem_par        => CONNECTED_TO_mem_par,        --                        .mem_par
			mem_alert_n    => CONNECTED_TO_mem_alert_n,    --                        .mem_alert_n
			mem_dqs        => CONNECTED_TO_mem_dqs,        --                        .mem_dqs
			mem_dqs_n      => CONNECTED_TO_mem_dqs_n,      --                        .mem_dqs_n
			mem_dq         => CONNECTED_TO_mem_dq,         --                        .mem_dq
			mem_dbi_n      => CONNECTED_TO_mem_dbi_n,      --                        .mem_dbi_n
			oct_rzqin      => CONNECTED_TO_oct_rzqin,      --         oct_conduit_end.oct_rzqin
			pll_ref_clk    => CONNECTED_TO_pll_ref_clk     --  pll_ref_clk_clock_sink.clk
		);

