	ghrd_10as066n2_emif_hps u0 (
		.global_reset_n (_connected_to_global_reset_n_), //   input,     width = 1, global_reset_reset_sink.reset_n
		.hps_to_emif    (_connected_to_hps_to_emif_),    //   input,  width = 4096,    hps_emif_conduit_end.hps_to_emif
		.emif_to_hps    (_connected_to_emif_to_hps_),    //  output,  width = 4096,                        .emif_to_hps
		.hps_to_emif_gp (_connected_to_hps_to_emif_gp_), //   input,     width = 2,                        .gp_to_emif
		.emif_to_hps_gp (_connected_to_emif_to_hps_gp_), //  output,     width = 1,                        .emif_to_gp
		.mem_ck         (_connected_to_mem_ck_),         //  output,     width = 1,         mem_conduit_end.mem_ck
		.mem_ck_n       (_connected_to_mem_ck_n_),       //  output,     width = 1,                        .mem_ck_n
		.mem_a          (_connected_to_mem_a_),          //  output,    width = 17,                        .mem_a
		.mem_act_n      (_connected_to_mem_act_n_),      //  output,     width = 1,                        .mem_act_n
		.mem_ba         (_connected_to_mem_ba_),         //  output,     width = 2,                        .mem_ba
		.mem_bg         (_connected_to_mem_bg_),         //  output,     width = 1,                        .mem_bg
		.mem_cke        (_connected_to_mem_cke_),        //  output,     width = 1,                        .mem_cke
		.mem_cs_n       (_connected_to_mem_cs_n_),       //  output,     width = 1,                        .mem_cs_n
		.mem_odt        (_connected_to_mem_odt_),        //  output,     width = 1,                        .mem_odt
		.mem_reset_n    (_connected_to_mem_reset_n_),    //  output,     width = 1,                        .mem_reset_n
		.mem_par        (_connected_to_mem_par_),        //  output,     width = 1,                        .mem_par
		.mem_alert_n    (_connected_to_mem_alert_n_),    //   input,     width = 1,                        .mem_alert_n
		.mem_dqs        (_connected_to_mem_dqs_),        //   inout,     width = 4,                        .mem_dqs
		.mem_dqs_n      (_connected_to_mem_dqs_n_),      //   inout,     width = 4,                        .mem_dqs_n
		.mem_dq         (_connected_to_mem_dq_),         //   inout,    width = 32,                        .mem_dq
		.mem_dbi_n      (_connected_to_mem_dbi_n_),      //   inout,     width = 4,                        .mem_dbi_n
		.oct_rzqin      (_connected_to_oct_rzqin_),      //   input,     width = 1,         oct_conduit_end.oct_rzqin
		.pll_ref_clk    (_connected_to_pll_ref_clk_)     //   input,     width = 1,  pll_ref_clk_clock_sink.clk
	);

